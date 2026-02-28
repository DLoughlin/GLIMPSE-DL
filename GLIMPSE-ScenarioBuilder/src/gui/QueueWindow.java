/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package gui;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Run Queue window. Displays session queue/history in a searchable table.
 *
 * Data source is provided by {@link PaneScenarioLibrary} (runsQueuedList and runsCompletedList).
 */
public class QueueWindow {

    private static Stage stage;
    private static TableView<QueueRow> table;
    private static ObservableList<QueueRow> masterData = FXCollections.observableArrayList();
    private static Label statusLabel;
    private static TextField filterField;
    private static ChoiceBox<String> scopeChoice;

    /** Provider for reloading queue data on demand/interval. */
    private static Supplier<QueueData> dataSupplier;

    /** Auto-refresh timeline (20s) - only active while the window is showing. */
    private static Timeline autoRefreshTimeline;

    /** Auto-refresh is enabled by default and persists while window instance lives. */
    private static boolean autoRefreshEnabled = true;

    private static CheckBox autoRefreshCheckBox;

    /** Last size used for this window in the current session. */
    private static double lastWidth = 730;
    private static double lastHeight = 650;

    /**
     * Show (or re-focus) the Run Queue window.
     *
     * @param owner optional owner stage
     * @param queuedLines session queued list
     * @param completedLines session completed list
     */
    public static void show(Stage owner, List<String> queuedLines, List<String> completedLines) {
        show(owner, () -> new QueueData(queuedLines, completedLines));
    }

    /**
     * Show (or re-focus) the Run Queue window.
     *
     * @param owner optional owner stage
     * @param supplier supplies the latest queue data when refresh/auto-refresh runs
     */
    public static void show(Stage owner, Supplier<QueueData> supplier) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, supplier));
            return;
        }

        dataSupplier = supplier;

        if (stage == null) {
            stage = new Stage();
            stage.initModality(Modality.NONE);
            if (owner != null) {
                try {
                    stage.initOwner(owner);
                } catch (Exception ignored) {}
            }
            stage.setTitle("Run Queue");

            table = new TableView<>();
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.getStyleClass().add("queue-table");

            TableColumn<QueueRow, String> colScenario = new TableColumn<>("Scenario");
            colScenario.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().scenario));

            TableColumn<QueueRow, String> colStatus = new TableColumn<>("Status");
            colStatus.setPrefWidth(140);
            colStatus.setMinWidth(140);
            colStatus.setMaxWidth(180);
            colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().status));

            TableColumn<QueueRow, String> colSessionBucket = new TableColumn<>("Bucket");
            colSessionBucket.setPrefWidth(110);
            colSessionBucket.setMinWidth(110);
            colSessionBucket.setMaxWidth(140);
            colSessionBucket.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().bucket));

            TableColumn<QueueRow, String> colLine = new TableColumn<>("Details");
            colLine.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().rawLine));

            installStatusCellFactory(colStatus);

            table.getColumns().addAll(colScenario, colStatus, colSessionBucket, colLine);

            table.setRowFactory(tv -> new TableRow<QueueRow>() {
                @Override
                protected void updateItem(QueueRow item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("queue-row-queued", "queue-row-running", "queue-row-success", "queue-row-issues", "queue-row-unknown");
                    if (empty || item == null) {
                        return;
                    }
                    switch (item.status) {
                    case "Queued":
                        getStyleClass().add("queue-row-queued");
                        break;
                    case "Running":
                        getStyleClass().add("queue-row-running");
                        break;
                    case "Success":
                        getStyleClass().add("queue-row-success");
                        break;
                    case "Issues":
                        getStyleClass().add("queue-row-issues");
                        break;
                    default:
                        getStyleClass().add("queue-row-unknown");
                        break;
                    }
                }
            });

            statusLabel = new Label();
            statusLabel.setPadding(new Insets(6, 10, 6, 10));

            filterField = new TextField();
            filterField.setPromptText("Filter (scenario or details)...");
            filterField.setPrefColumnCount(20);
            filterField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

            scopeChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                    "All",
                    "Queued",
                    "Completed",
                    "Issues",
                    "Running"
            ));
            scopeChoice.getSelectionModel().select(0);
            scopeChoice.setTooltip(new Tooltip("Quick filter by status/bucket"));
            scopeChoice.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> applyFilter());

            Button refreshBtn = new Button("Refresh");
            refreshBtn.setTooltip(new Tooltip("Refresh from this session's queue/history"));
            refreshBtn.setOnAction(e -> refreshFromSupplier());

            Button copyBtn = new Button("Copy");
            copyBtn.setTooltip(new Tooltip("Copy selected rows (or all if none selected) to clipboard"));
            copyBtn.setOnAction(e -> copyToClipboard());

            Button saveAsBtn = new Button("Save As...");
            saveAsBtn.setTooltip(new Tooltip("Save the queue table as a CSV file"));
            saveAsBtn.setOnAction(e -> saveTableAsCsv(stage));

            Button cancelQueuedBtn = new Button("Cancel queued");
            cancelQueuedBtn.setTooltip(new Tooltip("Cancel queued GCAM jobs (keeps current run)"));
            cancelQueuedBtn.setOnAction(e -> cancelQueuedJobs());

            autoRefreshCheckBox = new CheckBox("Auto-refresh (20s)");
            autoRefreshCheckBox.setTooltip(new Tooltip("When enabled, this window refreshes automatically every 20 seconds"));
            autoRefreshCheckBox.setSelected(autoRefreshEnabled);
            autoRefreshCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
                autoRefreshEnabled = Boolean.TRUE.equals(newV);
                updateAutoRefreshState();
            });

            HBox controls = new HBox(10,
                    new Label("Show:"), scopeChoice,
                    new Label("Filter:"), filterField,
                    refreshBtn, copyBtn, saveAsBtn, cancelQueuedBtn,
                    autoRefreshCheckBox);
            controls.setPadding(new Insets(6, 10, 6, 10));

            BorderPane root = new BorderPane();
            root.setTop(new VBox2(controls, statusLabel));
            root.setCenter(table);

            // Slightly narrower default size (about 2/3 of the original width).
            Scene scene = new Scene(root, lastWidth, lastHeight);
            try {
                java.net.URL cssUrl = QueueWindow.class.getResource("/resources/modern.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception ignored) {}

            stage.setScene(scene);

            // Remember last size within this session.
            stage.widthProperty().addListener((obs, oldV, newV) -> {
                try {
                    double w = newV == null ? -1 : newV.doubleValue();
                    if (w > 200) {
                        lastWidth = w;
                    }
                } catch (Exception ignored) {}
            });
            stage.heightProperty().addListener((obs, oldV, newV) -> {
                try {
                    double h = newV == null ? -1 : newV.doubleValue();
                    if (h > 200) {
                        lastHeight = h;
                    }
                } catch (Exception ignored) {}
            });

            // Auto-refresh: start/stop based on visibility to avoid leaks.
            stage.setOnShown(e -> updateAutoRefreshState());
            stage.setOnHidden(e -> stopAutoRefresh());

            stage.setOnCloseRequest(e -> {
                // keep instance for re-open/refresh
                e.consume();
                stage.hide();
            });
        }

        refreshFromSupplier();

        // Restore last size (best-effort) when re-opening.
        try {
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
        } catch (Exception ignored) {}

        stage.show();
        stage.toFront();
    }

    private static void updateAutoRefreshState() {
        try {
            if (stage == null) {
                return;
            }
            boolean showing = stage.isShowing();
            if (!showing) {
                stopAutoRefresh();
                updateStatusLabelAutoRefreshDecoration();
                return;
            }
            if (autoRefreshEnabled) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
            updateStatusLabelAutoRefreshDecoration();
        } catch (Exception ignored) {}
    }

    private static void updateStatusLabelAutoRefreshDecoration() {
        try {
            if (statusLabel == null) {
                return;
            }

            final String suffix = " (Auto-refresh off)";
            String t = statusLabel.getText();
            if (t == null) {
                t = "";
            }

            // Ensure suffix is present only when disabled.
            if (!autoRefreshEnabled) {
                if (!t.endsWith(suffix)) {
                    statusLabel.setText(t + suffix);
                }
                // Muted text color (keeps CSS theme intact otherwise)
                statusLabel.setStyle("-fx-text-fill: #6b7280;");
            } else {
                if (t.endsWith(suffix)) {
                    statusLabel.setText(t.substring(0, t.length() - suffix.length()));
                }
                statusLabel.setStyle("");
            }
        } catch (Exception ignored) {}
    }

    private static void refreshFromSupplier() {
        try {
            Supplier<QueueData> s = dataSupplier;
            if (s == null) {
                return;
            }
            QueueData data = s.get();
            if (data == null) {
                refreshData(null, null);
                return;
            }
            refreshData(data.queuedLines, data.completedLines);
        } catch (Exception ignored) {}
    }

    private static void startAutoRefresh() {
        try {
            if (!autoRefreshEnabled) {
                return;
            }
            if (autoRefreshTimeline == null) {
                autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> refreshFromSupplier()));
                autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            }
            autoRefreshTimeline.play();
        } catch (Exception ignored) {}
    }

    private static void stopAutoRefresh() {
        try {
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Refresh the underlying table rows.
     */
    public static void refreshData(List<String> queuedLines, List<String> completedLines) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> refreshData(queuedLines, completedLines));
            return;
        }
        if (table == null) {
            return;
        }

        masterData.clear();

        // Best-effort: infer the currently running scenario from the active GCAM main_log.txt.
        // This fixes cases where runsQueuedList only contains scenario names (no "(Running)" marker).
        final String runningScenarioName = safeLower(getRunningScenarioNameBestEffort());

        if (queuedLines != null) {
            for (String line : queuedLines) {
                if (line == null || line.trim().isEmpty()) continue;
                QueueRow row = QueueRow.from("Queued", line);
                if (!runningScenarioName.isEmpty() && safeLower(row.scenario).equals(runningScenarioName)) {
                    row = row.withStatus("Running");
                }
                masterData.add(row);
            }
        }
        if (completedLines != null) {
            for (String line : completedLines) {
                if (line == null || line.trim().isEmpty()) continue;
                masterData.add(QueueRow.from("Completed", line));
            }
        }

        applyFilter();

        int queuedCount = 0;
        int completedCount = 0;
        int issuesCount = 0;
        int runningCount = 0;
        for (QueueRow r : masterData) {
            if ("Queued".equals(r.bucket)) queuedCount++;
            if ("Completed".equals(r.bucket)) completedCount++;
            if ("Issues".equals(r.status)) issuesCount++;
            if ("Running".equals(r.status)) runningCount++;
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        statusLabel.setText("Queued: " + queuedCount + "   Completed: " + completedCount + "   Issues: " + issuesCount + "   Running: " + runningCount + "   (Last updated: " + ts + ")");
        updateStatusLabelAutoRefreshDecoration();
    }

    /**
     * Best-effort running scenario detection, without requiring a PaneScenarioLibrary instance.
     * Reads the first "Configuration file:" line in <gcamExeDir>/logs/main_log.txt and returns its parent folder name.
     */
    private static String getRunningScenarioNameBestEffort() {
        try {
            // vars is a singleton in this codebase; if not available, fall back gracefully.
            String exeDir;
            try {
                exeDir = glimpseUtil.GLIMPSEVariables.getInstance().getgCamExecutableDir();
            } catch (Throwable t) {
                exeDir = null;
            }
            if (exeDir == null || exeDir.trim().isEmpty()) {
                return "";
            }

            File mainLog = new File(exeDir + File.separator + "logs" + File.separator + "main_log.txt");
            if (!mainLog.exists()) {
                return "";
            }

            try (Scanner sc = new Scanner(mainLog)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if (line == null) continue;
                    line = line.trim();
                    if (line.startsWith("Configuration file:")) {
                        String p = line.substring(line.indexOf(':') + 1).trim();
                        File f = new File(p);
                        if (f.exists() && f.getParentFile() != null) {
                            return f.getParentFile().getName();
                        }
                        return "";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static void applyFilter() {
        if (table == null) return;

        String txt = filterField == null ? "" : safeLower(filterField.getText());
        String scope = scopeChoice == null ? "All" : scopeChoice.getSelectionModel().getSelectedItem();

        ObservableList<QueueRow> filtered = FXCollections.observableArrayList();
        for (QueueRow r : masterData) {
            if (r == null) continue;

            if (scope != null && !"All".equals(scope)) {
                if ("Queued".equals(scope) && !"Queued".equals(r.bucket)) continue;
                if ("Completed".equals(scope) && !"Completed".equals(r.bucket)) continue;
                if ("Issues".equals(scope) && !"Issues".equals(r.status)) continue;
                if ("Running".equals(scope) && !"Running".equals(r.status)) continue;
            }

            if (txt != null && !txt.isEmpty()) {
                String hay = safeLower(r.scenario) + "\n" + safeLower(r.rawLine);
                if (!hay.contains(txt)) {
                    continue;
                }
            }
            filtered.add(r);
        }

        table.setItems(filtered);
    }

    private static void copyToClipboard() {
        try {
            List<QueueRow> rows;
            if (table.getSelectionModel().getSelectedItems() != null && !table.getSelectionModel().getSelectedItems().isEmpty()) {
                rows = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            } else {
                rows = new ArrayList<>(table.getItems());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Scenario\tStatus\tBucket\tDetails\n");
            for (QueueRow r : rows) {
                if (r == null) continue;
                sb.append(nullToEmpty(r.scenario)).append('\t')
                  .append(nullToEmpty(r.status)).append('\t')
                  .append(nullToEmpty(r.bucket)).append('\t')
                  .append(nullToEmpty(r.rawLine))
                  .append('\n');
            }

            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(sb.toString());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception ignored) {}
    }

    private static void saveTableAsCsv(Stage owner) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Run Queue as CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
            chooser.setInitialFileName("run_queue_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

            File outFile = chooser.showSaveDialog(owner);
            if (outFile == null) {
                return;
            }

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(outFile), java.nio.charset.StandardCharsets.UTF_8))) {
                pw.println("Scenario,Status,Bucket,Details");
                for (QueueRow r : table.getItems()) {
                    if (r == null) continue;
                    pw.print(csv(r.scenario));
                    pw.print(',');
                    pw.print(csv(r.status));
                    pw.print(',');
                    pw.print(csv(r.bucket));
                    pw.print(',');
                    pw.print(csv(r.rawLine));
                    pw.println();
                }
            }
        } catch (Exception ignored) {}
    }

    private static void cancelQueuedJobs() {
        try {
            if (Client.gCAMExecutionThread == null) {
                Alert a = new Alert(AlertType.INFORMATION);
                a.setTitle("Cancel queued");
                a.setHeaderText(null);
                a.setContentText("No execution thread is active.");
                a.showAndWait();
                return;
            }

            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Cancel queued runs");
            alert.setHeaderText("Cancel queued GCAM runs?");
            alert.setContentText("This cancels queued runs but keeps the current run (if any) going. This can't be undone.");

            ButtonType cancelQueuedBtn = new ButtonType("Cancel queued", ButtonData.OK_DONE);
            ButtonType keepBtn = new ButtonType("Keep", ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(cancelQueuedBtn, keepBtn);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent() || result.get() != cancelQueuedBtn) {
                return;
            }

            int nCancelled = Client.gCAMExecutionThread.cancelQueuedJobsKeepRunningCurrent();

            try {
                ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                        ConsoleManager.MessageKind.GLIMPSE_INFO,
                        "Cancelled queued GCAM jobs: " + nCancelled);
            } catch (Exception ignored) {}

            // We can't directly clear PaneScenarioLibrary lists here safely. They will refresh next time they open.
            // But we can show a small acknowledgement.
            Alert done = new Alert(AlertType.INFORMATION);
            done.setTitle("Cancel queued");
            done.setHeaderText(null);
            done.setContentText("Cancelled queued jobs: " + nCancelled + "\n\nTip: press Refresh status to update scenario table statuses.");
            done.showAndWait();
        } catch (Exception ignored) {}
    }

    private static void installStatusCellFactory(TableColumn<QueueRow, String> colStatus) {
        colStatus.setCellFactory(tc -> new TableCell<QueueRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);

                getStyleClass().removeAll("queue-cell-status", "queue-status-queued", "queue-status-running", "queue-status-success", "queue-status-issues", "queue-status-unknown");
                if (empty) {
                    return;
                }

                getStyleClass().add("queue-cell-status");
                String st = item == null ? "" : item;
                if ("Queued".equals(st)) getStyleClass().add("queue-status-queued");
                else if ("Running".equals(st)) getStyleClass().add("queue-status-running");
                else if ("Success".equals(st)) getStyleClass().add("queue-status-success");
                else if ("Issues".equals(st)) getStyleClass().add("queue-status-issues");
                else getStyleClass().add("queue-status-unknown");
            }
        });
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        boolean needsQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',' || c == '"' || c == '\n' || c == '\r' || c == '\t') {
                needsQuotes = true;
                break;
            }
        }
        if (!needsQuotes) {
            return s;
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private static class QueueRow {
        final String scenario;
        final String status;
        final String bucket;
        final String rawLine;

        private QueueRow(String scenario, String status, String bucket, String rawLine) {
            this.scenario = scenario;
            this.status = status;
            this.bucket = bucket;
            this.rawLine = rawLine;
        }

        QueueRow withStatus(String newStatus) {
            return new QueueRow(this.scenario, newStatus, this.bucket, this.rawLine);
        }

        static QueueRow from(String bucket, String line) {
            String scenario = guessScenarioName(line);
            String status = guessStatus(bucket, line);
            return new QueueRow(scenario, status, bucket, line);
        }

        private static String guessScenarioName(String line) {
            if (line == null) return "";
            String trimmed = line.trim();
            if (trimmed.isEmpty()) return "";

            // Session queue lines are often just the scenario name.
            // In that case, use the full line (minus any "(Running)" suffix) as the scenario.
            if (!trimmed.contains("\\") && !trimmed.contains("/") && !trimmed.toLowerCase().contains("configuration_") && !trimmed.contains(":") ) {
                return trimmed.replace("(Running)", "").replace("(running)", "").trim();
            }

            // Most lines contain ...\<scenario>\... or .../<scenario>/...
            try {
                String s = line.replace('/', '\\');
                int idx = s.lastIndexOf("configuration_");
                if (idx >= 0) {
                    // configuration_<name>.xml
                    int start = idx + "configuration_".length();
                    int end = s.indexOf(".xml", start);
                    if (end > start) {
                        String name = s.substring(start, end);
                        // Sometimes configuration_<name>_archive.xml
                        name = name.replace("_archive", "");
                        return name;
                    }
                }

                // fallback: pick folder name between separators if present
                String sep = "\\";
                String[] parts = s.split("\\\\");
                for (int i = 0; i < parts.length; i++) {
                    if ("configuration".equalsIgnoreCase(parts[i])) {
                        // unlikely
                        continue;
                    }
                }
                // last non-empty token
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (parts[i] != null && !parts[i].trim().isEmpty()) {
                        String p = parts[i].trim();
                        if (p.endsWith(".xml")) {
                            p = p.substring(0, p.length() - 4);
                        }
                        return p;
                    }
                }
            } catch (Exception ignored) {}
            return line;
        }

        private static String guessStatus(String bucket, String line) {
            if (bucket == null) bucket = "";
            String ll = line == null ? "" : line.toLowerCase();

            if ("Queued".equals(bucket)) {
                // Broader detection; some code paths append " (Running)" and some may not.
                if (ll.contains("(running)") || ll.contains(" running") || ll.contains("running)") || ll.contains("status=running")) {
                    return "Running";
                }
                return "Queued";
            }

            // Completed bucket: attempt to infer success/issues from line text.
            if (ll.contains("dnf") || ll.contains("error") || ll.contains("failed") || ll.contains("issue")) {
                return "Issues";
            }
            if (ll.contains("unsolved")) {
                return "Issues";
            }
            return "Success";
        }
    }

    /**
     * Minimal helper VBox so we don't pull in extra files; avoids project import/style churn.
     */
    private static class VBox2 extends javafx.scene.layout.VBox {
        VBox2(javafx.scene.Node... children) {
            super(0, children);
        }
    }

    /**
     * Simple carrier for queue data (session queued/completed lists).
     */
    public static class QueueData {
        public final List<String> queuedLines;
        public final List<String> completedLines;

        public QueueData(List<String> queuedLines, List<String> completedLines) {
            this.queuedLines = queuedLines;
            this.completedLines = completedLines;
        }
    }
}
