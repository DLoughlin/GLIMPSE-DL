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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
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
        GLIMPSE_STDERR,
        GCAM_STDOUT,
        GCAM_STDERR,
        MODEL_INTERFACE
    }

    private static Stage stage;
    private static TextArea glimpseStdoutTextArea;
    private static TextArea glimpseStderrTextArea;
    private static TextArea gcamStdoutTextArea;
    private static TextArea gcamStderrTextArea;
    private static TextArea modelInterfaceTextArea;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ConsoleManager() {}

    static void show() {
        if (stage == null) {
            createStage();
        }
        stage.show();
        stage.toFront();
    }

    static void appendLine(StreamSource source, String line) {
        if (line == null) {
            return;
        }
        Platform.runLater(() -> {
            if (stage == null) {
                createStage();
            }
            TextArea ta;
            switch (source) {
            case GLIMPSE_STDOUT:
                ta = glimpseStdoutTextArea;
                break;
            case GLIMPSE_STDERR:
                ta = glimpseStderrTextArea;
                break;
            case GCAM_STDERR:
                ta = gcamStderrTextArea;
                break;
            case MODEL_INTERFACE:
                ta = modelInterfaceTextArea;
                break;
            case GCAM_STDOUT:
            default:
                ta = gcamStdoutTextArea;
                break;
            }
            if (ta == null) {
                return;
            }
            ta.appendText(line);
            if (!line.endsWith("\n")) {
                ta.appendText(System.lineSeparator());
            }
        });
    }

    static void appendHeader(StreamSource source, String header) {
        String msg = "[" + TS.format(LocalDateTime.now()) + "] " + header;
        appendLine(source, msg);
    }

    private static void createStage() {
        stage = new Stage();
        stage.setTitle("GLIMPSE Console");

        glimpseStdoutTextArea = createConsoleTextArea();
        glimpseStderrTextArea = createConsoleTextArea();
        gcamStdoutTextArea = createConsoleTextArea();
        gcamStderrTextArea = createConsoleTextArea();
        modelInterfaceTextArea = createConsoleTextArea();

        TabPane tabPane = new TabPane();
        Tab t0 = new Tab("GLIMPSE stdout", glimpseStdoutTextArea);
        t0.setClosable(false);
        Tab t0b = new Tab("GLIMPSE stderr", glimpseStderrTextArea);
        t0b.setClosable(false);
        Tab t1 = new Tab("GCAM stdout", gcamStdoutTextArea);
        t1.setClosable(false);
        Tab t1b = new Tab("GCAM stderr", gcamStderrTextArea);
        t1b.setClosable(false);
        Tab t2 = new Tab("ModelInterface stdout", modelInterfaceTextArea);
        t2.setClosable(false);
        tabPane.getTabs().addAll(t0, t0b, t1, t1b, t2);

        Button clearActive = new Button("Clear");
        clearActive.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getContent() instanceof TextArea) {
                ((TextArea) selected.getContent()).clear();
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
        stage.setScene(scene);
    }

    private static void saveSelectedTabToFile(TabPane tabPane) {
        Tab selected = (tabPane == null) ? null : tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getContent() instanceof TextArea)) {
            showAlert(Alert.AlertType.INFORMATION, "Save As", null, "No console tab is selected.");
            return;
        }

        TextArea ta = (TextArea) selected.getContent();
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
            Files.write(outFile.toPath(), ta.getText().getBytes(StandardCharsets.UTF_8));
            appendHeader(StreamSource.GLIMPSE_STDOUT, "Saved '" + tabName + "' to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Save As", "Failed to write file:", ex.getMessage());
            appendHeader(StreamSource.GLIMPSE_STDERR, "Save As failed: " + ex);
        }
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
                    if (t.getContent() instanceof TextArea) {
                        text = ((TextArea) t.getContent()).getText();
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

    private static TextArea createConsoleTextArea() {
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(false);
        return ta;
    }
}
