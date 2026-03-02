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
import java.time.Duration;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;

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

    private static Stage stage;

    private static ScrollPane glimpseStdoutScroll;
    // GLIMPSE stderr UI was removed (stderr is routed to GLIMPSE stdout).
    // private static ScrollPane glimpseStderrScroll;
    private static ScrollPane gcamStdoutScroll;
    private static ScrollPane modelInterfaceScroll;

    private static TextFlow glimpseStdoutFlow;
    // private static TextFlow glimpseStderrFlow;
    private static TextFlow gcamStdoutFlow;
    private static TextFlow modelInterfaceFlow;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ConsoleManager() {}

    static void show() {
        Platform.runLater(() -> {
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
        Platform.runLater(() -> {
            ensureModelCreated();
            if (stage == null) {
                createStage();
            }

            TextFlow flow;
            ScrollPane scroll;
            switch (source) {
            case GLIMPSE_STDERR:
                // GLIMPSE stderr tab was removed; route to stdout.
                flow = glimpseStdoutFlow;
                scroll = glimpseStdoutScroll;
                break;
            case GLIMPSE_STDOUT:
                flow = glimpseStdoutFlow;
                scroll = glimpseStdoutScroll;
                break;
            case MODEL_INTERFACE:
                flow = modelInterfaceFlow;
                scroll = modelInterfaceScroll;
                break;
            case GCAM_STDERR:
                // GCAM stderr tab was removed; route to stdout.
                flow = gcamStdoutFlow;
                scroll = gcamStdoutScroll;
                break;
            case GCAM_STDOUT:
            default:
                flow = gcamStdoutFlow;
                scroll = gcamStdoutScroll;
                break;
            }
            if (flow == null) {
                return;
            }

            String out = line;
            if (!out.endsWith("\n")) {
                out = out + System.lineSeparator();
            }

            Text t = new Text(out);
            t.setFill(colorFor(kind));
            flow.getChildren().add(t);

            autoScrollToBottom(scroll);
        });
    }

    static void appendHeader(StreamSource source, String header) {
        String msg = "[" + TS.format(LocalDateTime.now()) + "] " + header;
        appendLine(source, MessageKind.GLIMPSE_INFO, msg);
    }

    private static MessageKind defaultKindFor(StreamSource source) {
        if (source == null) {
            return MessageKind.GLIMPSE_INFO;
        }
        switch (source) {
        case GCAM_STDOUT:
            return MessageKind.MODEL_STDOUT;
        case GCAM_STDERR:
            return MessageKind.STDERR;
        case GLIMPSE_STDERR:
            return MessageKind.STDERR;
        case MODEL_INTERFACE:
        case GLIMPSE_STDOUT:
        default:
            return MessageKind.GLIMPSE_INFO;
        }
    }

    private static Color colorFor(MessageKind kind) {
        if (kind == null) {
            return Color.BLACK;
        }
        switch (kind) {
        case STDERR:
            return Color.FIREBRICK;
        case GLIMPSE_INFO:
            // Dark-ish blue for GLIMPSE-originated messages.
            return Color.DARKBLUE;
        case MODEL_STDOUT:
        default:
            return Color.BLACK;
        }
    }

    private static void autoScrollToBottom(ScrollPane scroll) {
        if (scroll == null) {
            return;
        }
        // Defer until after layout so scroll bounds update.
        Platform.runLater(() -> {
            try {
                scroll.setVvalue(1.0);
            } catch (Exception ignored) {}
        });
    }

    /** Ensure TextFlow models exist even if the Stage hasn't been created yet. Must be called on FX thread. */
    private static void ensureModelCreated() {
        if (glimpseStdoutFlow == null) {
            glimpseStdoutFlow = createConsoleTextFlow();
        }
        if (gcamStdoutFlow == null) {
            gcamStdoutFlow = createConsoleTextFlow();
        }
        if (modelInterfaceFlow == null) {
            modelInterfaceFlow = createConsoleTextFlow();
        }
    }

    private static void createStage() {
        // Ensure model exists before wiring it into the view.
        ensureModelCreated();

        stage = new Stage();
        stage.setTitle("GLIMPSE Console");

        glimpseStdoutScroll = createConsoleScrollPane(glimpseStdoutFlow);
        gcamStdoutScroll = createConsoleScrollPane(gcamStdoutFlow);
        modelInterfaceScroll = createConsoleScrollPane(modelInterfaceFlow);

        TabPane tabPane = new TabPane();
        Tab t0 = new Tab("GLIMPSE stdout", glimpseStdoutScroll);
        t0.setClosable(false);
        Tab t1 = new Tab("GCAM stdout", gcamStdoutScroll);
        t1.setClosable(false);
        Tab t2 = new Tab("ModelInterface stdout", modelInterfaceScroll);
        t2.setClosable(false);
        tabPane.getTabs().addAll(t0, t1, t2);

        Button clearActive = new Button("Clear");
        clearActive.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Object content = selected.getContent();
                TextFlow flow = extractFlow(content);
                if (flow != null) {
                    flow.getChildren().clear();
                }
            }
        });

        Button saveAs = new Button("Save As...");
        saveAs.setOnAction(e -> saveSelectedTabToFile(tabPane));

        Button zipAll = new Button("Zip");
        zipAll.setOnAction(e -> zipAllTabsToFile(tabPane));

        BorderPane root = new BorderPane();
        root.setTop(new ToolBar(clearActive, saveAs, zipAll));
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 900, 520);
        // Apply the shared app theme for consistent styling with the main window.
        ScenarioBuilder.applyModernTheme(scene);
        stage.setScene(scene);
    }

    private static TextFlow createConsoleTextFlow() {
        TextFlow tf = new TextFlow();
        // Keep text readable and wrapping like a console.
        tf.setLineSpacing(0.0);

        // 4px internal padding so text doesn't touch the edges.
        tf.setPadding(new Insets(4, 4, 4, 4));

        // Ensure padding area stays white.
        try {
            tf.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        } catch (Exception ignored) {}

        return tf;
    }

    private static ScrollPane createConsoleScrollPane(TextFlow flow) {
        ScrollPane sp = new ScrollPane(flow);
        // Allow content to size naturally so scrollbars appear as needed.
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        sp.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        // Keep viewport background white (matches padding/background).
        try {
            sp.setStyle("-fx-background: white; -fx-background-color: white;");
        } catch (Exception ignored) {}

        return sp;
    }

    private static TextFlow extractFlow(Object tabContent) {
        if (tabContent instanceof ScrollPane) {
            Object c = ((ScrollPane) tabContent).getContent();
            if (c instanceof TextFlow) {
                return (TextFlow) c;
            }
        }
        return null;
    }

    private static void saveSelectedTabToFile(TabPane tabPane) {
        Tab selected = (tabPane == null) ? null : tabPane.getSelectionModel().getSelectedItem();
        TextFlow flow = (selected == null) ? null : extractFlow(selected.getContent());
        if (selected == null || flow == null) {
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
            return; // user cancelled
        }

        // Ensure .txt extension if user omitted it.
        if (!outFile.getName().toLowerCase().endsWith(".txt")) {
            outFile = new File(outFile.getParentFile(), outFile.getName() + ".txt");
        }

        try {
            Files.write(outFile.toPath(), getFlowText(flow).getBytes(StandardCharsets.UTF_8));
            appendHeader(StreamSource.GLIMPSE_STDOUT, "Saved '" + tabName + "' to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Save As", "Failed to write file:", ex.getMessage());
            appendHeader(StreamSource.GLIMPSE_STDERR, "Save As failed: " + ex);
        }
    }

    private static String getFlowText(TextFlow flow) {
        if (flow == null || flow.getChildren() == null || flow.getChildren().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(Math.max(256, flow.getChildren().size() * 32));
        flow.getChildren().forEach(n -> {
            if (n instanceof Text) {
                sb.append(((Text) n).getText());
            }
        });
        return sb.toString();
    }

    private static void zipAllTabsToFile(TabPane tabPane) {
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

        String suggested = "all-logs-" + LocalDate.now().toString() + ".zip"; // YYYY-MM-DD
        chooser.setInitialFileName(suggested);

        File outFile = chooser.showSaveDialog(stage);
        if (outFile == null) {
            return; // user cancelled
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

                    String entryName = entryBase + ".txt";
                    entryName = ensureUniqueEntryName(entryName, usedEntryNames);

                    String text = "";
                    if (t.getContent() instanceof TextFlow) {
                        text = getFlowText((TextFlow) t.getContent());
                    }

                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                    zos.write(bytes);
                    zos.closeEntry();
                }
            }

            appendHeader(StreamSource.GLIMPSE_STDOUT, "Zipped all console logs to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Zip Logs", "Failed to write zip file:", ex.getMessage());
            appendHeader(StreamSource.GLIMPSE_STDERR, "Zip Logs failed: " + ex);
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

    /** Default directory for console exports: <GLIMPSE>/GLIMPSE-Data/logs (if available). */
    private static File getDefaultLogsDirectory() {
        try {
            String glimpseDir = GLIMPSEVariables.getInstance().getGlimpseDir();
            if (glimpseDir == null || glimpseDir.trim().isEmpty()) {
                return null;
            }
            return new File(glimpseDir, "GLIMPSE-Data" + File.separator + "logs");
        } catch (Throwable t) {
            // Be resilient if vars aren't initialized yet.
            return null;
        }
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null) {
            return "console.txt";
        }
        // Windows-illegal: < > : " / \ | ? * plus control chars.
        String s = raw.replaceAll("[\\\\/:*?\"<>|]", "_");
        s = s.replaceAll("[\\p{Cntrl}]", "");
        s = s.trim();
        if (s.isEmpty()) {
            s = "console";
        }
        // Keep it reasonably short for Windows path limits.
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
            // Last resort: avoid crashing if JavaFX alerts aren't available.
            System.err.println(title + ": " + (content == null ? "" : content));
        }
    }

    /**
     * Buffered console flushing for high-volume output (notably GCAM stdout).
     *
     * Why this exists:
     * - Scheduling Platform.runLater per line can backlog the FX queue and make output appear to "update late".
     * - We batch lines off-thread and flush them onto the FX thread periodically.
     */
    private static final class BufferedAppender {
        private final StreamSource source;
        private final ConcurrentLinkedQueue<BufferedItem> queue = new ConcurrentLinkedQueue<>();
        private volatile int approxCharsQueued = 0;

        private BufferedAppender(StreamSource source) {
            this.source = source;
        }

        void enqueue(MessageKind kind, String line) {
            if (line == null) {
                return;
            }
            // Queue the raw line; newline is added on flush to keep consistent behaviour.
            queue.add(new BufferedItem(kind, line));
            approxCharsQueued += Math.min(4096, line.length() + 1);
        }

        void drainToUi(int maxItems) {
            // Must be called on FX thread.
            if (maxItems <= 0) {
                maxItems = Integer.MAX_VALUE;
            }

            ensureModelCreated();
            if (stage == null) {
                createStage();
            }

            TextFlow flow;
            ScrollPane scroll;
            switch (source) {
            case GLIMPSE_STDERR:
            case GLIMPSE_STDOUT:
                flow = glimpseStdoutFlow;
                scroll = glimpseStdoutScroll;
                break;
            case MODEL_INTERFACE:
                flow = modelInterfaceFlow;
                scroll = modelInterfaceScroll;
                break;
            case GCAM_STDERR:
            case GCAM_STDOUT:
            default:
                flow = gcamStdoutFlow;
                scroll = gcamStdoutScroll;
                break;
            }

            if (flow == null) {
                // Drain anyway to keep memory bounded.
                int drained = 0;
                BufferedItem it;
                while (drained < maxItems && (it = queue.poll()) != null) {
                    drained++;
                }
                approxCharsQueued = 0;
                return;
            }

            // Batch queued lines by MessageKind so we create far fewer JavaFX Text nodes.
            int drained = 0;
            BufferedItem it;
            StringBuilder sbStdout = null;
            StringBuilder sbInfo = null;
            StringBuilder sbErr = null;

            while (drained < maxItems && (it = queue.poll()) != null) {
                drained++;

                String out = it.line;
                if (!out.endsWith("\n")) {
                    out = out + System.lineSeparator();
                }

                switch (it.kind) {
                case STDERR:
                    if (sbErr == null) sbErr = new StringBuilder(Math.min(4096, out.length() * 4));
                    sbErr.append(out);
                    break;
                case GLIMPSE_INFO:
                    if (sbInfo == null) sbInfo = new StringBuilder(Math.min(4096, out.length() * 4));
                    sbInfo.append(out);
                    break;
                case MODEL_STDOUT:
                default:
                    if (sbStdout == null) sbStdout = new StringBuilder(Math.min(4096, out.length() * 4));
                    sbStdout.append(out);
                    break;
                }
            }

            // Append in a stable order so output looks consistent.
            if (sbStdout != null && sbStdout.length() > 0) {
                Text t = new Text(sbStdout.toString());
                t.setFill(colorFor(MessageKind.MODEL_STDOUT));
                flow.getChildren().add(t);
            }
            if (sbInfo != null && sbInfo.length() > 0) {
                Text t = new Text(sbInfo.toString());
                t.setFill(colorFor(MessageKind.GLIMPSE_INFO));
                flow.getChildren().add(t);
            }
            if (sbErr != null && sbErr.length() > 0) {
                Text t = new Text(sbErr.toString());
                t.setFill(colorFor(MessageKind.STDERR));
                flow.getChildren().add(t);
            }

            // Reset the approximate size occasionally. This is not exact but good enough for throttling.
            if (queue.isEmpty()) {
                approxCharsQueued = 0;
            }

            autoScrollToBottom(scroll);
        }

        void clearPending() {
            while (queue.poll() != null) {
                // drain
            }
            approxCharsQueued = 0;
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

    // Config: keep these conservative so UI remains snappy.
    // Lower flush interval so GCAM output feels closer to real time.
    private static final long BUFFER_FLUSH_MILLIS = 20;
    // Keep per-pulse work bounded.
    private static final int BUFFER_FLUSH_MAX_ITEMS_PER_PULSE = 800;
    private static final int BUFFER_FORCE_FLUSH_CHAR_THRESHOLD = 64 * 1024;

    private static final ScheduledExecutorService BUFFER_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ConsoleManager-BufferedFlush");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<StreamSource, BufferedAppender> BUFFERED = new ConcurrentHashMap<>();
    private static volatile ScheduledFuture<?> flushTask;

    private static BufferedAppender buffered(StreamSource source) {
        StreamSource effective = source;
        if (effective == null) {
            effective = StreamSource.GLIMPSE_STDOUT;
        }
        // Route deprecated stderr sources to their stdout tab, consistent with appendLine().
        if (effective == StreamSource.GLIMPSE_STDERR) {
            effective = StreamSource.GLIMPSE_STDOUT;
        } else if (effective == StreamSource.GCAM_STDERR) {
            effective = StreamSource.GCAM_STDOUT;
        }
        return BUFFERED.computeIfAbsent(effective, BufferedAppender::new);
    }

    private static synchronized void ensureFlushTaskScheduled() {
        if (flushTask != null && !flushTask.isDone()) {
            return;
        }
        flushTask = BUFFER_SCHEDULER.scheduleAtFixedRate(() -> {
            // Only schedule a UI flush if there is something queued.
            boolean anyQueued = false;
            for (BufferedAppender a : BUFFERED.values()) {
                if (a != null && !a.queue.isEmpty()) {
                    anyQueued = true;
                    break;
                }
            }
            if (!anyQueued) {
                return;
            }
            Platform.runLater(() -> flushBufferedToUi(false));
        }, BUFFER_FLUSH_MILLIS, BUFFER_FLUSH_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Buffered append intended for very chatty streams.
     *
     * This does not call Platform.runLater per line. Instead it batches and flushes to the UI periodically.
     */
    static void appendLineBuffered(StreamSource source, MessageKind kind, String line) {
        if (line == null) {
            return;
        }
        BufferedAppender app = buffered(source);
        app.enqueue(kind, line);
        ensureFlushTaskScheduled();

        // GCAM emits steady progress lines every few seconds; make them appear promptly.
        // This is best-effort: the periodic flush still exists, but this reduces "big batch" updates
        // when the FX thread is otherwise idle.
        if (source == StreamSource.GCAM_STDOUT) {
            Platform.runLater(() -> flushBufferedToUi(false));
            return;
        }

        // Safety valve: if we get a huge burst, trigger a sooner UI flush.
        if (app.getApproxCharsQueued() >= BUFFER_FORCE_FLUSH_CHAR_THRESHOLD) {
            // Best-effort: schedule a one-off flush soon. If FX is busy, periodic flush will still catch up.
            Platform.runLater(() -> flushBufferedToUi(false));
        }
    }

    /** Forces any buffered text to be appended to the UI now (best-effort). */
    static void flushBuffered() {
        Platform.runLater(() -> flushBufferedToUi(true));
    }

    /** Must be called on FX thread. */
    private static void flushBufferedToUi(boolean drainAll) {
        ensureModelCreated();
        int maxItems = drainAll ? Integer.MAX_VALUE : BUFFER_FLUSH_MAX_ITEMS_PER_PULSE;
        for (BufferedAppender a : BUFFERED.values()) {
            if (a == null) {
                continue;
            }
            // If not draining all, split pulses across different streams fairly.
            a.drainToUi(maxItems);
        }
    }

    /** Clears the text for the given console stream (best-effort). */
    static void clear(StreamSource source) {
        // Flush pending buffered content first so it doesn't reappear right after clear().
        try {
            BufferedAppender app = buffered(source);
            app.clearPending();
        } catch (Exception ignored) {}

        Platform.runLater(() -> {
            // Create model buffers even if the window hasn't been opened yet.
            ensureModelCreated();

            TextFlow flow = null;
            switch (source) {
            case GLIMPSE_STDERR:
                flow = glimpseStdoutFlow;
                break;
            case GLIMPSE_STDOUT:
                flow = glimpseStdoutFlow;
                break;
            case MODEL_INTERFACE:
                flow = modelInterfaceFlow;
                break;
            case GCAM_STDERR:
                flow = gcamStdoutFlow;
                break;
            case GCAM_STDOUT:
            default:
                flow = gcamStdoutFlow;
                break;
            }
            if (flow != null) {
                flow.getChildren().clear();
            }
        });
    }
}
