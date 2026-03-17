/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package gui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Small tabbed console window used to view stdout/stderr from external processes.
 *
 * Notes:
 * - All UI updates are marshalled onto the JavaFX Application Thread.
 * - This is a lightweight in-app viewer; logs are still written to files elsewhere.
 */
final class ConsoleManager {

    enum StreamSource {
        GLIMPSE_STDOUT,
        /**
         * Deprecated: GLIMPSE stderr is routed to {@link #GLIMPSE_STDOUT}.
         * Kept for backward compatibility.
         */
        @Deprecated
        GLIMPSE_STDERR,
        GCAM_STDOUT,
        /**
         * Deprecated: GCAM currently doesn't produce meaningful stderr in this app.
         * Kept for backward compatibility; it will be routed to {@link #GCAM_STDOUT}.
         */
        @Deprecated
        GCAM_STDERR,
        MODEL_INTERFACE
    }

    /**
     * Visual styling for a line written to a console tab.
     */
    enum MessageKind {
        /** Text that originated from the external model (GCAM stdout). */
        MODEL_STDOUT,
        /** Text that originated from GLIMPSE itself (status/info). */
        GLIMPSE_INFO,
        /** Any stderr (GLIMPSE or external process). */
        STDERR
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long BUFFER_FLUSH_MILLIS = 20;
    private static final int BUFFER_FLUSH_MAX_ITEMS_PER_PULSE = 800;
    private static final int BUFFER_FORCE_FLUSH_CHAR_THRESHOLD = 64 * 1024;
    private static final int MAX_VISIBLE_CHARS_PER_CONSOLE = 400_000;
    private static final int TRIM_TO_VISIBLE_CHARS = 300_000;
    private static final boolean REDUCED_LIVE_GCAM_OUTPUT = true;
    private static final String GCAM_COMPLETION_MARKER = "Model run completed.";
    private static final String[] GCAM_STDOUT_PREFIX_FILTERS = {
            "Config",
            "Parsi",
            "XML ",
            "Starting new",
            "Period",
            "Error"
    };

    private static final String BASE_CONSOLE_STYLE =
            "-fx-control-inner-background: white;"
          + "-fx-font-family: 'Consolas', 'Courier New', monospace';"
          + "-fx-highlight-fill: derive(-fx-focus-color, 60%);"
          + "-fx-highlight-text-fill: -fx-text-inner-color;";
    private static final String STYLE_INFO = BASE_CONSOLE_STYLE + "-fx-text-fill: darkblue;";
    private static final String STYLE_STDERR = BASE_CONSOLE_STYLE + "-fx-text-fill: firebrick;";
    private static final String STYLE_STDOUT = BASE_CONSOLE_STYLE + "-fx-text-fill: black;";

    private static Stage stage;
    private static TabPane tabPane;
    private static TextArea glimpseStdoutArea;
    private static TextArea gcamStdoutArea;
    private static TextArea modelInterfaceArea;

    private static final ScheduledExecutorService BUFFER_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ConsoleManager-BufferedFlush");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentHashMap<StreamSource, BufferedAppender> BUFFERED = new ConcurrentHashMap<>();
    private static volatile ScheduledFuture<?> flushTask;
    private static final AtomicBoolean uiFlushScheduled = new AtomicBoolean(false);

    private ConsoleManager() {}

    static void show() {
        Platform.runLater(() -> {
            ensureModelCreated();
            if (stage == null) {
                createStage();
            }
            stage.show();
            stage.toFront();
        });
    }

    static void appendLine(StreamSource source, String line) {
        appendLine(source, defaultKindFor(source), line);
    }

    static void appendLine(StreamSource source, MessageKind kind, String line) {
        if (line == null) {
            return;
        }
        Platform.runLater(() -> appendLineToUi(effectiveSource(source), kind, line));
    }

    static void appendHeader(StreamSource source, String header) {
        String msg = "[" + TS.format(LocalDateTime.now()) + "] " + header;
        appendLine(source, MessageKind.GLIMPSE_INFO, msg);
    }

    static void appendLineBuffered(StreamSource source, MessageKind kind, String line) {
        if (line == null) {
            return;
        }
        BufferedAppender app = buffered(source);
        app.enqueue(kind, line);
        ensureFlushTaskScheduled();

        if (app.getApproxCharsQueued() >= BUFFER_FORCE_FLUSH_CHAR_THRESHOLD) {
            requestUiFlush(false);
        }
    }

    static void flushBuffered() {
        requestUiFlush(true);
    }

    static void clear(StreamSource source) {
        try {
            buffered(source).clearPending();
        } catch (Exception ignored) {}

        Platform.runLater(() -> {
            ensureModelCreated();
            TextArea area = areaFor(source);
            if (area != null) {
                area.clear();
                applyAreaStyle(area, MessageKind.MODEL_STDOUT);
            }
        });
    }

    private static void appendLineToUi(StreamSource source, MessageKind kind, String line) {
        ensureModelCreated();
        if (stage == null) {
            createStage();
        }

        TextArea area = areaFor(source);
        if (area == null) {
            return;
        }

        String normalized = normalizeConsoleLine(line);
        if (normalized.isEmpty() && isGlimpseSource(source)) {
            return;
        }

        area.appendText(normalized + System.lineSeparator());
        applyAreaStyle(area, kind);
        trimVisibleConsoleText(area);
        autoScrollToBottom(area);
    }

    private static void appendChunkToUi(StreamSource source, MessageKind kind, String text, boolean scrollToBottom) {
        ensureModelCreated();
        if (stage == null) {
            createStage();
        }

        TextArea area = areaFor(source);
        if (area == null || text == null || text.isEmpty()) {
            return;
        }

        area.appendText(text);
        applyAreaStyle(area, kind);
        trimVisibleConsoleText(area);
        if (scrollToBottom) {
            autoScrollToBottom(area);
        }
    }

    private static void ensureModelCreated() {
        if (glimpseStdoutArea == null) {
            glimpseStdoutArea = createConsoleTextArea();
        }
        if (gcamStdoutArea == null) {
            gcamStdoutArea = createConsoleTextArea();
        }
        if (modelInterfaceArea == null) {
            modelInterfaceArea = createConsoleTextArea();
        }
    }

    private static TextArea createConsoleTextArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setFocusTraversable(true);
        area.setContextMenu(createConsoleContextMenu(area));
        applyAreaStyle(area, MessageKind.MODEL_STDOUT);
        area.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                area.copy();
                event.consume();
            }
        });
        return area;
    }

    private static ContextMenu createConsoleContextMenu(TextArea area) {
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(e -> area.copy());

        MenuItem selectAll = new MenuItem("Select All");
        selectAll.setOnAction(e -> area.selectAll());

        MenuItem clear = new MenuItem("Clear");
        clear.setOnAction(e -> clearArea(area));

        ContextMenu menu = new ContextMenu(copy, selectAll, clear);
        menu.setOnShowing(e -> {
            copy.setDisable(area.getSelectedText() == null || area.getSelectedText().isEmpty());
            selectAll.setDisable(area.getText() == null || area.getText().isEmpty());
            clear.setDisable(area.getText() == null || area.getText().isEmpty());
        });
        return menu;
    }

    private static void createStage() {
        ensureModelCreated();

        stage = new Stage();
        stage.setTitle("GLIMPSE Console");

        tabPane = new TabPane();
        tabPane.getTabs().add(createTab("GLIMPSE", glimpseStdoutArea));
        tabPane.getTabs().add(createTab("GCAM", gcamStdoutArea));
        tabPane.getTabs().add(createTab("ModelInterface", modelInterfaceArea));

        Button clearActive = new Button("Clear");
        clearActive.setOnAction(e -> {
            TextArea area = selectedArea();
            if (area != null) {
                clearArea(area);
            }
        });

        Button saveAs = new Button("Save As...");
        saveAs.setOnAction(e -> saveSelectedTabToFile());

        Button zipAll = new Button("Zip");
        zipAll.setOnAction(e -> zipAllTabsToFile());

        BorderPane root = new BorderPane();
        root.setTop(new ToolBar(clearActive, saveAs, zipAll));
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 700, 525);
        ScenarioBuilder.applyModernTheme(scene);
        stage.setScene(scene);
    }

    private static Tab createTab(String title, TextArea area) {
        Tab tab = new Tab(title, area);
        tab.setClosable(false);
        return tab;
    }

    private static TextArea selectedArea() {
        Tab selected = (tabPane == null) ? null : tabPane.getSelectionModel().getSelectedItem();
        return selected == null ? null : areaFromTab(selected);
    }

    private static TextArea areaFromTab(Tab tab) {
        if (tab == null) {
            return null;
        }
        Object content = tab.getContent();
        return content instanceof TextArea ? (TextArea) content : null;
    }

    private static void saveSelectedTabToFile() {
        Tab selected = (tabPane == null) ? null : tabPane.getSelectionModel().getSelectedItem();
        TextArea area = selectedArea();
        if (selected == null || area == null) {
            showAlert(Alert.AlertType.INFORMATION, "Save As", null, "No console tab is selected.");
            return;
        }

        String tabName = selected.getText();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + tabName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));

        File initialDir = getDefaultLogsDirectory();
        if (initialDir != null && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }

        String suggested = sanitizeFilename(tabName);
        if (!suggested.toLowerCase().endsWith(".txt")) {
            suggested = suggested + ".txt";
        }
        chooser.setInitialFileName(suggested);

        File outFile = chooser.showSaveDialog(stage);
        if (outFile == null) {
            return;
        }
        if (!outFile.getName().toLowerCase().endsWith(".txt")) {
            outFile = new File(outFile.getParentFile(), outFile.getName() + ".txt");
        }

        try {
            Files.write(outFile.toPath(), area.getText().getBytes(StandardCharsets.UTF_8));
            appendHeader(StreamSource.GLIMPSE_STDOUT, "Saved '" + tabName + "' to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Save As", "Failed to write file:", ex.getMessage());
            appendHeader(StreamSource.GLIMPSE_STDERR, "Save As failed: " + ex);
        }
    }

    private static void zipAllTabsToFile() {
        if (tabPane == null || tabPane.getTabs() == null || tabPane.getTabs().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Zip Logs", null, "There are no console tabs to zip.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zip all console logs");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip archives (*.zip)", "*.zip"));

        File initialDir = getDefaultLogsDirectory();
        if (initialDir != null && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }

        chooser.setInitialFileName("all-logs-" + LocalDate.now().toString() + ".zip");

        File outFile = chooser.showSaveDialog(stage);
        if (outFile == null) {
            return;
        }
        if (!outFile.getName().toLowerCase().endsWith(".zip")) {
            outFile = new File(outFile.getParentFile(), outFile.getName() + ".zip");
        }

        try {
            if (outFile.getParentFile() != null) {
                outFile.getParentFile().mkdirs();
            }

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {
                zos.setLevel(9);
                Set<String> usedEntryNames = new HashSet<>();

                for (Tab t : tabPane.getTabs()) {
                    if (t == null) {
                        continue;
                    }
                    String tabName = t.getText();
                    String entryBase = sanitizeFilename(tabName);
                    if (entryBase.toLowerCase().endsWith(".txt")) {
                        entryBase = entryBase.substring(0, entryBase.length() - 4);
                    }
                    if (entryBase.isEmpty()) {
                        entryBase = "console";
                    }

                    String entryName = ensureUniqueEntryName(entryBase + ".txt", usedEntryNames);
                    String text = "";
                    TextArea area = areaFromTab(t);
                    if (area != null) {
                        text = area.getText();
                    }

                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    zos.write(text.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            appendHeader(StreamSource.GLIMPSE_STDOUT, "Zipped all console logs to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Zip Logs", "Failed to write zip file:", ex.getMessage());
            appendHeader(StreamSource.GLIMPSE_STDERR, "Zip Logs failed: " + ex);
        }
    }

    private static MessageKind defaultKindFor(StreamSource source) {
        if (source == null) {
            return MessageKind.GLIMPSE_INFO;
        }
        switch (source) {
        case GCAM_STDOUT:
            return MessageKind.MODEL_STDOUT;
        case GCAM_STDERR:
        case GLIMPSE_STDERR:
            return MessageKind.STDERR;
        case MODEL_INTERFACE:
        case GLIMPSE_STDOUT:
        default:
            return MessageKind.GLIMPSE_INFO;
        }
    }

    private static void applyAreaStyle(TextArea area, MessageKind kind) {
        if (area == null) {
            return;
        }
        switch (kind == null ? MessageKind.MODEL_STDOUT : kind) {
        case STDERR:
            area.setStyle(STYLE_STDERR);
            break;
        case GLIMPSE_INFO:
            area.setStyle(STYLE_INFO);
            break;
        case MODEL_STDOUT:
        default:
            area.setStyle(STYLE_STDOUT);
            break;
        }
    }

    private static void autoScrollToBottom(TextArea area) {
        if (area == null) {
            return;
        }
        try {
            area.positionCaret(area.getLength());
            area.setScrollTop(Double.MAX_VALUE);
        } catch (Exception ignored) {}
    }

    private static void clearArea(TextArea area) {
        if (area == null) {
            return;
        }
        area.clear();
        applyAreaStyle(area, MessageKind.MODEL_STDOUT);
    }

    private static StreamSource effectiveSource(StreamSource source) {
        if (source == null) {
            return StreamSource.GLIMPSE_STDOUT;
        }
        if (source == StreamSource.GLIMPSE_STDERR) {
            return StreamSource.GLIMPSE_STDOUT;
        }
        if (source == StreamSource.GCAM_STDERR) {
            return StreamSource.GCAM_STDOUT;
        }
        return source;
    }

    private static TextArea areaFor(StreamSource source) {
        switch (effectiveSource(source)) {
        case MODEL_INTERFACE:
            return modelInterfaceArea;
        case GCAM_STDOUT:
            return gcamStdoutArea;
        case GLIMPSE_STDOUT:
        default:
            return glimpseStdoutArea;
        }
    }

    private static BufferedAppender buffered(StreamSource source) {
        return BUFFERED.computeIfAbsent(effectiveSource(source), BufferedAppender::new);
    }

    private static synchronized void ensureFlushTaskScheduled() {
        if (flushTask != null && !flushTask.isDone()) {
            return;
        }
        flushTask = BUFFER_SCHEDULER.scheduleAtFixedRate(() -> {
            boolean anyQueued = false;
            for (BufferedAppender a : BUFFERED.values()) {
                if (a != null && !a.queue.isEmpty()) {
                    anyQueued = true;
                    break;
                }
            }
            if (anyQueued) {
                requestUiFlush(false);
            }
        }, BUFFER_FLUSH_MILLIS, BUFFER_FLUSH_MILLIS, TimeUnit.MILLISECONDS);
    }

    private static void requestUiFlush(boolean drainAll) {
        if (!uiFlushScheduled.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            try {
                flushBufferedToUi(drainAll);
            } finally {
                uiFlushScheduled.set(false);
                if (!drainAll && hasQueuedBufferedOutput()) {
                    requestUiFlush(false);
                }
            }
        });
    }

    private static boolean hasQueuedBufferedOutput() {
        for (BufferedAppender a : BUFFERED.values()) {
            if (a != null && !a.queue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void flushBufferedToUi(boolean drainAll) {
        ensureModelCreated();
        int maxItems = drainAll ? Integer.MAX_VALUE : BUFFER_FLUSH_MAX_ITEMS_PER_PULSE;
        for (BufferedAppender a : BUFFERED.values()) {
            if (a != null) {
                a.drainToUi(maxItems);
            }
        }
    }

    private static String ensureUniqueEntryName(String entryName, Set<String> usedEntryNames) {
        if (usedEntryNames.add(entryName)) {
            return entryName;
        }
        String base = entryName;
        String ext = "";
        int dot = entryName.lastIndexOf('.');
        if (dot > 0 && dot < entryName.length() - 1) {
            base = entryName.substring(0, dot);
            ext = entryName.substring(dot);
        }
        int i = 2;
        while (true) {
            String candidate = base + "-" + i + ext;
            if (usedEntryNames.add(candidate)) {
                return candidate;
            }
            i++;
        }
    }

    private static File getDefaultLogsDirectory() {
        try {
            String glimpseDir = GLIMPSEVariables.getInstance().getGlimpseDir();
            if (glimpseDir == null || glimpseDir.trim().isEmpty()) {
                return null;
            }
            return new File(glimpseDir, "GLIMPSE-Data" + File.separator + "logs");
        } catch (Throwable t) {
            return null;
        }
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null) {
            return "console.txt";
        }
        String s = raw.replaceAll("[\\\\/:*?\"<>|]", "_");
        s = s.replaceAll("[\\p{Cntrl}]", "");
        s = s.trim();
        if (s.isEmpty()) {
            s = "console";
        }
        if (s.length() > 80) {
            s = s.substring(0, 80).trim();
        }
        return s;
    }

    private static void showAlert(Alert.AlertType type, String title, String header, String content) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.initOwner(stage);
            alert.showAndWait();
        } catch (Throwable t) {
            System.err.println(title + ": " + (content == null ? "" : content));
        }
    }

    private static String normalizeConsoleLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        int end = line.length();
        while (end > 0) {
            char c = line.charAt(end - 1);
            if (c == '\n' || c == '\r') {
                end--;
            } else {
                break;
            }
        }
        return (end == line.length()) ? line : line.substring(0, end);
    }

    private static boolean isGlimpseSource(StreamSource source) {
        StreamSource effective = effectiveSource(source);
        return effective == StreamSource.GLIMPSE_STDOUT;
    }

    private static void trimVisibleConsoleText(TextArea area) {
        if (area == null) {
            return;
        }
        try {
            String text = area.getText();
            if (text == null || text.length() <= MAX_VISIBLE_CHARS_PER_CONSOLE) {
                return;
            }
            int targetStart = Math.max(0, text.length() - TRIM_TO_VISIBLE_CHARS);
            int newline = text.indexOf('\n', targetStart);
            int trimStart = newline >= 0 ? newline + 1 : targetStart;
            String trimmed = text.substring(trimStart);
            area.setText(trimmed);
        } catch (Exception ignored) {}
    }

    private static final class BufferedAppender {
        private final StreamSource source;
        private final ConcurrentLinkedQueue<BufferedItem> queue = new ConcurrentLinkedQueue<>();
        private volatile int approxCharsQueued = 0;
        private boolean includeAllFutureGcamStdout = false;

        private BufferedAppender(StreamSource source) {
            this.source = source;
        }

        void enqueue(MessageKind kind, String line) {
            queue.add(new BufferedItem(kind, line));
            approxCharsQueued += Math.min(4096, line == null ? 1 : line.length() + 1);
        }

        void drainToUi(int maxItems) {
            if (maxItems <= 0) {
                maxItems = Integer.MAX_VALUE;
            }
            int drained = 0;
            BufferedItem it;
            MessageKind chunkKind = null;
            StringBuilder chunk = new StringBuilder();
            while (drained < maxItems && (it = queue.poll()) != null) {
                drained++;
                String normalized = normalizeConsoleLine(it.line);
                if (normalized.isEmpty() && isGlimpseSource(source)) {
                    continue;
                }
                if (shouldReduceLiveOutput(it, normalized)) {
                    continue;
                }
                if (chunkKind != null && chunkKind != it.kind && chunk.length() > 0) {
                    appendChunkToUi(source, chunkKind, chunk.toString(), false);
                    chunk.setLength(0);
                }
                chunkKind = it.kind;
                chunk.append(normalized).append(System.lineSeparator());
            }
            if (chunk.length() > 0) {
                appendChunkToUi(source, chunkKind, chunk.toString(), true);
            }
            if (queue.isEmpty()) {
                approxCharsQueued = 0;
            }
        }

        private boolean shouldReduceLiveOutput(BufferedItem item, String normalizedLine) {
            if (!REDUCED_LIVE_GCAM_OUTPUT || source != StreamSource.GCAM_STDOUT) {
                return false;
            }
            if (item == null || item.kind != MessageKind.MODEL_STDOUT) {
                return false;
            }
            return !shouldKeepGcamStdoutLine(normalizedLine);
        }

        private boolean shouldKeepGcamStdoutLine(String normalizedLine) {
            if (normalizedLine == null) {
                return false;
            }
            String trimmed = normalizedLine.trim();
            if (trimmed.isEmpty()) {
                return false;
            }
            if (includeAllFutureGcamStdout || trimmed.contains(GCAM_COMPLETION_MARKER)) {
                includeAllFutureGcamStdout = true;
                return true;
            }
            for (String prefix : GCAM_STDOUT_PREFIX_FILTERS) {
                if (trimmed.startsWith(prefix)) {
                    return true;
                }
            }
            return trimmed.contains("iterations");
        }

        void clearPending() {
            while (queue.poll() != null) {
                // drain
            }
            approxCharsQueued = 0;
            includeAllFutureGcamStdout = false;
        }

        int getApproxCharsQueued() {
            return approxCharsQueued;
        }
    }

    private static final class BufferedItem {
        private final MessageKind kind;
        private final String line;

        private BufferedItem(MessageKind kind, String line) {
            this.kind = (kind == null) ? MessageKind.GLIMPSE_INFO : kind;
            this.line = (line == null) ? "" : line;
        }
    }
}