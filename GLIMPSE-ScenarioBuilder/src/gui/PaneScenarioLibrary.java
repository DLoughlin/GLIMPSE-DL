/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 * 
 * EXPORT CONTROL
 * User agrees that the Software will not be shipped, transferred or
 * exported into any country or used in any manner prohibited by the
 * United States Export Administration Act or any other applicable
 * export laws, restrictions or regulations (collectively the "Export Laws").
 * Export of the Software may require some form of license or other
 * authority from the U.S. Government, and failure to obtain such
 * export control license may result in criminal liability under
 * U.S. laws. In addition, if the Software is identified as export controlled
 * items under the Export Laws, User represents and warrants that User
 * is not a citizen, or otherwise located within, an embargoed nation
 * (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
 *     and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 *
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
*/
package gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import glimpseUtil.CommandLineTokenizer;
import glimpseUtil.ProcessResult;
import glimpseUtil.ProcessRunner;
import glimpseUtil.FileTailer;
import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseUtil.FileChooserPlus;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.WindowsRuntimePreflight;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * PaneScenarioLibrary manages the lower pane of the GLIMPSE Scenario Builder application,
 * displaying historical run records and providing scenario-related actions. It is responsible for:
 * <ul>
 *   <li>Displaying a table of all scenario runs, including their status, creation, and completion dates.</li>
 *   <li>Providing UI controls for scenario management, such as running, archiving, deleting, importing, and viewing scenarios.</li>
 *   <li>Handling user interactions for scenario operations, including confirmation dialogs and file operations.</li>
 *   <li>Updating scenario status based on log files and execution results.</li>
 *   <li>Generating reports on scenario execution, errors, and queue status.</li>
 *   <li>Integrating with external tools such as ModelInterface and file diff utilities.</li>
 * </ul>
 * <p>
 * This class is central to the workflow of scenario management, providing both the UI and the logic
 * for all scenario-related actions in the application. It interacts with the file system, background
 * execution threads, and various utility classes to ensure robust scenario handling.
 * </p>
 *
 * <b>Key Features:</b>
 * <ul>
 *   <li>Scenario queue management and reporting</li>
 *   <li>Scenario archiving and restoration</li>
 *   <li>Scenario import/export and configuration editing</li>
 *   <li>Log and error report generation</li>
 *   <li>Integration with ModelInterface for results viewing</li>
 *   <li>Support for diffing scenario configuration files</li>
 * </ul>
 *
 * <b>Usage:</b>
 * <pre>
 *     PaneScenarioLibrary pane = new PaneScenarioLibrary(stage);
 *     HBox scenarioPane = pane.gethBox();
 *     // Add scenarioPane to your application's layout
 * </pre>
 *
 * <b>Dependencies:</b>
 * <ul>
 *   <li>glimpseElement.ScenarioRow, ScenarioTable</li>
 *   <li>glimpseUtil.FileChooserPlus, GLIMPSEFiles, GLIMPSEStyles, GLIMPSEUtils, GLIMPSEVariables</li>
 *   <li>JavaFX (Platform, ObservableList, HBox, Stage, etc.)</li>
 * </ul>
 *
 * <b>Updated:</b> 2025-09-29
 * <b>Author:</b> US EPA, GLIMPSE Contributors
 *
 * <b>Class Overview:</b>
 * <ul>
 *   <li>Handles scenario table UI and all scenario-related actions.</li>
 *   <li>Provides methods for scenario queueing, archiving, deletion, import/export, and reporting.</li>
 *   <li>Integrates with ModelInterface and file system utilities.</li>
 *   <li>Updates scenario status and logs system/computer status.</li>
 * </ul>
 */
public class PaneScenarioLibrary extends ScenarioBuilder {
    // --- Live tail of exe/logs/main_log.txt (optional) ---
    // If true, the GCAM stdout console will also show lines tailed from exeDir/logs/main_log.txt while GCAM runs.
    // This helps on Windows when native stdout is block-buffered.
    private static final boolean TAIL_GCAM_MAIN_LOG_TO_CONSOLE = false;
    private static final String TAILED_LOG_PREFIX = "[main_log] ";
    private static final int GCAM_PROMPT_BUFFER_MAX_CHARS = 512;
    private static final Duration LIVE_STATUS_REFRESH_INTERVAL = Duration.ofSeconds(3);

    private static final String GCAM_STATUS_BLOCKED = "Blocked";
    private static final String GCAM_STATUS_WRITING = "Writing";
    private static final String[] GCAM_STDOUT_SUCCESS_MARKERS = {
            "Model exiting successfully.",
            "Exiting successfully.",
            "Model run completed.",
            "Finished printing output."
    };

    private static final String LOADING_SCENARIOS_MESSAGE = "Loading scenario status...";
    private static final String NO_SCENARIOS_MESSAGE = "No scenarios found.";
    private static final String READY_MESSAGE = "Ready";

    private static boolean isTailedLogLine(String line) {
        return line != null && line.startsWith(TAILED_LOG_PREFIX);
    }

    private void ensureLiveStatusRefreshTimeline() {
        if (liveStatusRefreshTimeline != null) {
            return;
        }
        liveStatusRefreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(LIVE_STATUS_REFRESH_INTERVAL.getSeconds()), e -> {
            try {
                if (hasActiveGcamRun()) {
                    updateRunStatus();
                    ScenarioTable.tableScenariosLibrary.refresh();
                } else {
                    stopLiveStatusRefresh();
                }
            } catch (Exception ignored) {}
        }));
        liveStatusRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private boolean hasActiveGcamRun() {
        try {
            ProcessRunner.RunningProcess rp = currentGcamRun;
            if (rp != null && rp.getProcess() != null && rp.getProcess().isAlive()) {
                return true;
            }
        } catch (Exception ignored) {}
        Future<ProcessResult> future = currentGcamFuture;
        return future != null && !future.isDone();
    }

    private void startLiveStatusRefresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::startLiveStatusRefresh);
            return;
        }
        ensureLiveStatusRefreshTimeline();
        if (liveStatusRefreshTimeline != null) {
            liveStatusRefreshTimeline.play();
        }
    }

    private void stopLiveStatusRefresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::stopLiveStatusRefresh);
            return;
        }
        if (liveStatusRefreshTimeline != null) {
            liveStatusRefreshTimeline.stop();
        }
    }

    private static boolean looksLikeDatabaseSavePrompt(String line) {
        return ScenarioLibraryPromptHelper.looksLikeDatabaseSavePrompt(line);
    }

    private static String normalizeDatabasePromptText(String text) {
        return ScenarioLibraryPromptHelper.normalizeDatabasePromptText(text);
    }

    // --- Added: Stop GCAM run method ---
    /**
     * Stops the currently running GCAM process (if any). This is best-effort and may leave partial outputs.
     */
    private void stopCurrentGcamRun() {
        // If nothing is running, do nothing.
        if (currentGcamRun == null && currentGcamFuture == null) {
            return;
        }

        // Confirm stop.
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Stop GCAM run");
        alert.setHeaderText("Stop GCAM?");

        Label msg = new Label(
                "Stopping GCAM may leave partial output files in the scenario folder.\n"
              + "\n"
              + "Choose:\n"
              + " - Stop: stop the currently running scenario only\n"
              + " - Stop All: stop the running scenario and cancel queued scenarios\n"
              + " - Continue: keep running\n");
        msg.setWrapText(true);

        VBox content = new VBox(10);
        content.getChildren().addAll(msg);
        alert.getDialogPane().setContent(content);

        ButtonType stopBtn = new ButtonType("Stop", ButtonBar.ButtonData.OK_DONE);
        ButtonType stopAllBtn = new ButtonType("Stop All", ButtonBar.ButtonData.OTHER);
        ButtonType continueBtn = new ButtonType("Continue", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(stopBtn, stopAllBtn, continueBtn);

        // Make Continue the default button (safer than defaulting to Stop).
        try {
            javafx.scene.control.Button continueButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(continueBtn);
            if (continueButton != null) {
                continueButton.setDefaultButton(true);
            }
        } catch (Exception ignored) {}

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() == continueBtn) {
            return;
        }

        boolean stopAll = result.get() == stopAllBtn;

        // Only Stop All should cancel queued futures.
        if (stopAll) {
            try {
                int nCancelled = 0;
                if (Client.gCAMExecutionThread != null) {
                    nCancelled = Client.gCAMExecutionThread.cancelQueuedJobsKeepRunningCurrent();
                }

                runsQueuedList.clear();
                for (ScenarioRow sr : ScenarioTable.listOfScenarioRuns) {
                    if (sr != null && "In queue".equals(sr.getStatus())) {
                        sr.setStatus("");
                    }
                }
                ScenarioTable.tableScenariosLibrary.refresh();

                try {
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                            ConsoleManager.MessageKind.GLIMPSE_INFO,
                            "Cancelled queued GCAM jobs: " + nCancelled);
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }

        try {
            ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "Stop requested");
            ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                    ConsoleManager.MessageKind.GLIMPSE_INFO,
                    stopAll ? "Attempting to stop GCAM (and cancel queued runs)..." : "Attempting to stop the running GCAM process...");
        } catch (Exception ignored) {}

        // IMPORTANT: Don't cancel the Future for the normal Stop case.
        // The Future represents the whole GCAM run chain (and cancelling can interrupt the executor worker
        // and/or cause queued tasks to be skipped). We only cancel queued jobs on Stop All.

        try {
            ProcessRunner.RunningProcess rp = currentGcamRun;
            if (rp != null) {
                lastStoppedGcamScenarioName = currentGcamScenarioName;
                lastGcamStopResult = rp.stop();
                try {
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                            ConsoleManager.MessageKind.GLIMPSE_INFO,
                            "Stop signal sent (" + lastGcamStopResult.getSummary() + ")");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        Platform.runLater(() -> {
            try {
                if (Client.buttonStopScenario != null) {
                    Client.buttonStopScenario.setDisable(true);
                }
            } catch (Exception ignored) {}
        });
    }

    // Constants for UI labels and tooltips
    private static final String DIFF_LABEL = "Diff";
    private static final String DIFF_TOOLTIP = "Diff: Compare first two selected configurations";
    private static final String REFRESH_LABEL = "Refresh";
    private static final String REFRESH_TOOLTIP = "Refresh: Update scenario run status";
    private static final String CONSOLE_TOOLTIP = "Console: View GCAM and ModelInterface output";
    private static final String RESULTS_LABEL = "Results";
    private static final String RESULTS_TOOLTIP = "Results: Open the ModelInterface to view results";
    private static final String RESULTS_SELECTED_LABEL = "Results (selected)";
    private static final String RESULTS_SELECTED_TOOLTIP = "Results-Selected: Open the ModelInterface to view results for selected scenario";
    private static final String PLAY_LABEL = "Play";
    private static final String PLAY_TOOLTIP = "Play: Add the selected scenarios to execution queue";
    private static final String DELETE_LABEL = "Delete";
    private static final String DELETE_TOOLTIP = "Delete: Move the selected scenarios to trash";
    private static final String CONFIG_LABEL = "Config";
    private static final String CONFIG_TOOLTIP = "Open: Open configuration file for selected scenario";
    private static final String LOG_LABEL = "Log";
    private static final String LOG_TOOLTIP = "Main_Log-Selected: View main_log.txt in selected scenario folder";
    private static final String EXE_ERRORS_LABEL = "ExeError";
    private static final String EXE_ERRORS_TOOLTIP = "Errors: View errors in main_log.txt file in exe/log folder";
    private static final String ERRORS_LABEL = "Errors";
    private static final String ERRORS_TOOLTIP = "Errors-Selected: View errors in selected scenario main_log.txt file";
    private static final String EXE_LOG_LABEL = "ExeLog";
    private static final String EXE_LOG_TOOLTIP = "Main_Log: View main_log.txt in the ee/log folder";
    private static final String BROWSE_LABEL = "Browse";
    private static final String BROWSE_TOOLTIP = "Browse: Open the folder of the selected scenarios";
    private static final String IMPORT_LABEL = "Import";
    private static final String IMPORT_TOOLTIP = "Import: Import an existing configuration file to create new scenario";
    private static final String QUEUE_LABEL = "Queue";
    private static final String QUEUE_TOOLTIP = "Queue: List scenarios added to queue this session";
    private static final String ARCHIVE_LABEL = "Archive";
    private static final String ARCHIVE_TOOLTIP = "Archive: Archive the selected scenarios";
    private static final String REPORT_LABEL = "Report";
    private static final String REPORT_TOOLTIP = "Report: Generate scenario execution report";

    private static final String XML_FILE_FILTER_LABEL = "XML files (*.xml)";
    private static final String XML_FILE_FILTER_EXT = "xml";

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

    private final ArrayList<String> runsQueuedList = new ArrayList<>();
    private final ArrayList<String> runsCompletedList = new ArrayList<>();

    /** Tracks the currently running GCAM process so the UI can stop it. */
    private volatile ProcessRunner.RunningProcess currentGcamRun;
    private volatile Future<ProcessResult> currentGcamFuture;
    private volatile boolean gcamPromptDialogActive;
    private volatile String activeGcamPromptLine;
    private volatile String lastHandledDatabasePrompt;
    private volatile boolean databasePromptAwaitingReset;
    private final StringBuilder recentGcamPromptBuffer = new StringBuilder();

    /**
     * Captures the last stop() attempt result so we can report a summary when the run actually ends.
     * (Stop is asynchronous from the point of view of the Future/get flow.)
     */
    private volatile ProcessRunner.StopResult lastGcamStopResult;
    private volatile String lastStoppedGcamScenarioName;

    /** Tracks the scenario name currently being executed by GCAM (best-effort). */
    private volatile String currentGcamScenarioName;
    private Timeline liveStatusRefreshTimeline;

    private long startupTime = 0;
    private final HBox scenarioLibraryHBox = new HBox(1);
    private final AtomicBoolean scenarioRefreshInProgress = new AtomicBoolean(false);

    /**
     * Constructs the scenario library pane, sets up UI controls, event handlers, and initializes the scenario table.
     * Binds table size to the main application stage and triggers initial status update.
     *
     * @param stage the main application stage for binding UI components
     */
    PaneScenarioLibrary(Stage stage) {
        // Removed inline style to allow CSS styling
        // scenarioLibraryHBox.setStyle(styles.getFontStyle()); // Set font style for the HBox
        scenarioLibraryHBox.setSpacing(10); // Set spacing between elements
        ScenarioTable.tableScenariosLibrary.setOnMouseClicked(e -> setArrowAndButtonStatus()); // Update button status on table click
        createScenarioLibraryButtons(); // Create and configure all scenario library buttons
        ensureLiveStatusRefreshTimeline();

        ScenarioTable.tableScenariosLibrary.prefWidthProperty().bind(stage.widthProperty().multiply(1.0)); // Bind table width to stage
        ScenarioTable.tableScenariosLibrary.prefHeightProperty().bind(stage.heightProperty().multiply(0.7)); // Bind table height to stage
        scenarioLibraryHBox.getChildren().addAll(ScenarioTable.tableScenariosLibrary); // Add table to HBox
        if (ScenarioTable.tableScenariosLibrary != null) {
            ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(LOADING_SCENARIOS_MESSAGE));
        }
        if (startupTime == 0) startupTime = (new Date()).getTime(); // Record startup time
        System.out.println("time now=" + (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).format(startupTime));
        refreshScenarioStatusAsync(false); // Initial update of scenario run status
    }

    /**
     * Default constructor for PaneScenarioLibrary. Used for testing or non-UI instantiation.
     */
    PaneScenarioLibrary() {}

    /**
     * Sets up scenario library buttons, their tooltips, icons, and event handlers.
     * Initializes button states and visibility based on application logic.
     *
     * <p>Buttons include: Diff, Refresh, Results, Play, Delete, Config, Log, ExeError, Errors, ExeLog, Browse, Import, Queue, Archive, Report.</p>
     */
    private void createScenarioLibraryButtons() {
        // Creating buttons on the bottom pane
        Client.buttonDiffFiles = utils.createButton(DIFF_LABEL, styles.getBigButtonWidth(), DIFF_TOOLTIP, "compare");
        Client.buttonRefreshScenarioStatus = utils.createButton(REFRESH_LABEL, styles.getBigButtonWidth(), REFRESH_TOOLTIP, "refresh1");
        Client.buttonConsole = utils.createButton(null, styles.getBigButtonWidth(), CONSOLE_TOOLTIP, "console");
        Client.buttonResults = utils.createButton(RESULTS_LABEL, styles.getBigButtonWidth(), RESULTS_TOOLTIP, "results");
        Client.buttonResultsForSelected = utils.createButton(RESULTS_SELECTED_LABEL, styles.getBigButtonWidth(), RESULTS_SELECTED_TOOLTIP, "results-selected");
        Client.buttonRunScenario = utils.createButton(PLAY_LABEL, styles.getBigButtonWidth(), PLAY_TOOLTIP, "play");
        Client.buttonStopScenario = utils.createButton("Stop", styles.getBigButtonWidth(), "Stop the currently running GCAM scenario", "stop");

        Client.buttonDeleteScenario = utils.createButton(DELETE_LABEL, styles.getBigButtonWidth(), DELETE_TOOLTIP, "delete1");
        Client.buttonViewConfig = utils.createButton(CONFIG_LABEL, styles.getBigButtonWidth(), CONFIG_TOOLTIP, "edit1");
        Client.buttonViewLog = utils.createButton(LOG_LABEL, styles.getBigButtonWidth(), LOG_TOOLTIP, "log-selected");
        Client.buttonViewExeErrors = utils.createButton(EXE_ERRORS_LABEL, styles.getBigButtonWidth(), EXE_ERRORS_TOOLTIP, "errors");
        Client.buttonViewErrors = utils.createButton(ERRORS_LABEL, styles.getBigButtonWidth(), ERRORS_TOOLTIP, "errors-selected");
        Client.buttonViewExeLog = utils.createButton(EXE_LOG_LABEL, styles.getBigButtonWidth(), EXE_LOG_TOOLTIP, "log");
        Client.buttonBrowseScenarioFolder = utils.createButton(BROWSE_LABEL, styles.getBigButtonWidth(), BROWSE_TOOLTIP, "open_folder1");
        Client.buttonImportScenario = utils.createButton(IMPORT_LABEL, styles.getBigButtonWidth(), IMPORT_TOOLTIP, "import");
        Client.buttonShowRunQueue = utils.createButton(QUEUE_LABEL, styles.getBigButtonWidth(), QUEUE_TOOLTIP, "queue");
        Client.buttonArchiveScenario = utils.createButton(ARCHIVE_LABEL, styles.getBigButtonWidth(), ARCHIVE_TOOLTIP, "archive");
        Client.buttonReport = utils.createButton(REPORT_LABEL, styles.getBigButtonWidth(), REPORT_TOOLTIP, "report");

        // Set initial button status
        Client.buttonRunScenario.setDisable(true); // Disabled until a scenario is selected
        Client.buttonStopScenario.setDisable(true); // Enabled only while a run is active
        Client.buttonBrowseScenarioFolder.setDisable(true);
        Client.buttonImportScenario.setDisable(false);
        Client.buttonArchiveScenario.setDisable(true);
        Client.buttonDeleteScenario.setDisable(true);
        Client.buttonResultsForSelected.setDisable(true);
        Client.buttonViewConfig.setDisable(true);
        Client.buttonDiffFiles.setDisable(true);
        Client.buttonViewLog.setDisable(true);
        Client.buttonViewExeErrors.setDisable(false);
        Client.buttonViewErrors.setDisable(true);
        Client.buttonViewExeLog.setDisable(false);
        Client.buttonReport.setDisable(false);

        // Event handlers for each button
        Client.buttonRefreshScenarioStatus.setOnAction(e -> {
            refreshScenarioStatusAsync(true);
            ScenarioTable.tableScenariosLibrary.refresh();
        });
        Client.buttonConsole.setOnAction(e -> ConsoleManager.show());
        Client.buttonReport.setOnAction(e -> generateRunReport());
        Client.buttonRunScenario.setOnAction(e -> {
            try {
                runGcamOnSelected();
            } catch (Exception ex) {
                utils.warningMessage("Problem running GCAM.");
                System.out.println("Error trying to run GCAM.");
                System.out.println("Error: " + ex);
                utils.exitOnException();
            }
            refreshScenarioStatusAsync(true);
        });
        Client.buttonStopScenario.setOnAction(e -> stopCurrentGcamRun());

        Client.buttonArchiveScenario.setOnAction(e -> handleArchiveScenario());
        Client.buttonDeleteScenario.setOnAction(e -> handleDeleteScenario());
        Client.buttonResults.setOnAction(e -> handleResults());
        Client.buttonResultsForSelected.setOnAction(e -> handleResultsForSelected());
        Client.buttonBrowseScenarioFolder.setOnAction(e -> handleBrowseScenarioFolder());
        Client.buttonImportScenario.setOnAction(e -> handleImportScenario());
        Client.buttonViewConfig.setOnAction(e -> handleViewConfig());
        Client.buttonViewLog.setOnAction(e -> handleViewLog());
        Client.buttonViewExeErrors.setOnAction(e -> generateExeErrorReport());
        Client.buttonViewErrors.setOnAction(e -> generateErrorReport());
        Client.buttonViewExeLog.setOnAction(e -> handleViewExeLog());
        Client.buttonDiffFiles.setOnAction(e -> handleDiffFiles());
        Client.buttonShowRunQueue.setOnAction(e -> handleShowRunQueue());

        // Alignment for key buttons
        Client.buttonResults.setAlignment(Pos.CENTER);
        Client.buttonResultsForSelected.setAlignment(Pos.CENTER);
        Client.buttonRunScenario.setAlignment(Pos.CENTER);
        Client.buttonStopScenario.setAlignment(Pos.CENTER);
        Client.buttonDeleteScenario.setAlignment(Pos.CENTER);

        // Initial button visibility
        Client.buttonRunScenario.setVisible(true);
        Client.buttonStopScenario.setVisible(true);
        Client.buttonBrowseScenarioFolder.setVisible(true);
        Client.buttonImportScenario.setVisible(true);
        Client.buttonArchiveScenario.setVisible(false);
        Client.buttonDeleteScenario.setVisible(true);
        Client.buttonViewConfig.setVisible(true);
        Client.buttonDiffFiles.setVisible(true);
        Client.buttonViewLog.setVisible(true);
        Client.buttonViewExeLog.setVisible(true);
        Client.buttonReport.setVisible(false);
    }

    // --- UI Event Handlers ---
    /**
     * Handles archiving of selected scenarios. Moves configuration and related files to an archive folder.
     * Prompts user if archive already exists. Updates scenario configuration to reference archived files.
     *
     * <p>For each selected scenario, creates an archive subfolder, copies referenced files, updates the configuration,
     * and zips the archive. If an archive already exists, prompts the user for overwrite.</p>
     */
    private void handleArchiveScenario() {
        if (!utils.confirmArchiveScenario()) return;
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        for (ScenarioRow row : selectedFiles) {
            String scenName = row.getScenarioName();
            String workingDir = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenName);
            String exeDir = vars.getgCamExecutableDir();
            String configFilename = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenName);
            String archiveConfigFilename = ScenarioLibraryPathHelper.scenarioArchiveConfigFile(vars.getScenarioDir(), scenName);
            archiveScenario(exeDir, workingDir, archiveConfigFilename, configFilename, scenName);
        }
    }

    /**
     * Handles deletion of selected scenarios. Moves scenario folders to trash.
     * Prompts user for confirmation. Removes scenarios from the scenario table.
     */
    private void handleDeleteScenario() {
        if (!utils.confirmDelete()) return;
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        // Clear queue state for any selected scenarios that were queued.
        dequeueScenariosAndClearStatus(selectedFiles);
        for (ScenarioRow row : selectedFiles) {
            String scenName = row.getScenarioName();
            String xmlDir = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenName);
            String trashDirFolder = ScenarioLibraryPathHelper.scenarioDir(vars.getTrashDir(), scenName);
            File trashDir = new File(trashDirFolder);
            if (trashDir.exists())
                try {
                    files.deleteDirectoryStream(trashDir.toPath());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            if (!trashDir.exists()) trashDir.mkdirs();
            try {
                Files.move(Paths.get(xmlDir), Paths.get(trashDirFolder), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                utils.warningMessage("Problem deleting scenario(s)");
                System.out.println("error: " + e);
                utils.exitOnException();
            }
        }
        ScenarioTable.removeFromListOfRunFiles(selectedFiles);
    }

    /**
     * Handles opening ModelInterface for all scenarios. Warns if executable directory is not set.
     * Launches ModelInterface in a background thread.
     */
    private void handleResults() {
        if (vars.getModelInterfaceDir() == null || vars.getModelInterfaceDir().trim().isEmpty()) {
            utils.warningMessage("Please specify modelInterfaceDir in options file.");
        } else {
            try {
                runModelInterface();
            } catch (Exception e) {
                e.printStackTrace();
                utils.exitOnException();
            }
        }
    }

    /**
     * Handles opening ModelInterface for a selected scenario. Warns if executable directory is not set.
     * Launches ModelInterface for the selected scenario's output database.
     */
    private void handleResultsForSelected() {
        if (vars.getModelInterfaceDir() == null || vars.getModelInterfaceDir().trim().isEmpty()) {
            utils.warningMessage("Please specify modelInterfaceDir in options file.");
        } else {
            ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
            if (selectedFiles.size() == 1) {
                String scenName = selectedFiles.get(0).getScenarioName();
                String configFilename = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenName);
                File configFile = new File(configFilename);
                String databaseLine = files.searchForTextInFileS(configFile, "xmldb-location", "#");
                String databaseName = utils.getStringBetweenCharSequences(databaseLine, ">", "</");
                String updatedName = files.getResolvedPath(vars.getgCamExecutableDir(), databaseName);
                try {
                    runModelInterfaceWhich(updatedName);
                } catch (Exception e) {
                    e.printStackTrace();
                    utils.exitOnException();
                }
            }
        }
    }

    /**
     * Opens the file explorer for the selected scenario folders.
     * Uses the system's file explorer to show the scenario directory.
     */
    private void handleBrowseScenarioFolder() {
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        for (ScenarioRow row : selectedFiles) {
            String scenName = row.getScenarioName();
            String xmlDir = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenName);
            files.openFileExplorer(xmlDir);
        }
    }

    /**
     * Handles importing a scenario configuration file. Prompts for overwrite if scenario exists.
     * Adds the imported scenario to the scenario table and creates its folder.
     */
    private void handleImportScenario() {
        File newConfigFile = FileChooserPlus.showOpenDialog(null, "Select scenario configuration file", new File(vars.getgCamExecutableDir()), FileChooserPlus.createExtensionFilter(XML_FILE_FILTER_LABEL, XML_FILE_FILTER_EXT));
        if (newConfigFile != null) {
            String str = files.searchForTextInFileS(newConfigFile, "scenarioName", "<!--");
            String scenarioName = utils.getStringBetweenCharSequences(str, ">", "</");
            String workingScenarioLog = ScenarioLibraryPathHelper.glimpseRunsFile(vars.getGlimpseLogDir());
            File workingScenariosFile = new File(workingScenarioLog);
            boolean doesScenarioExist = files.searchForTextAtStartOfLinesInFile(workingScenariosFile, scenarioName + ",", "#");
            String confirmMsg = doesScenarioExist ? "Overwrite existing scenario " + scenarioName + "?" : "Import " + scenarioName + " into GLIMPSE?";
            if (!utils.confirmAction(confirmMsg)) return;
            String newScenFolderName = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName);
            File newScenFolder = new File(newScenFolderName);
            newScenFolder.mkdir();
            String newScenFilename = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
            files.copyFile(newConfigFile.getAbsolutePath(), newScenFilename);
            ScenarioRow sr = new ScenarioRow(scenarioName);
            sr.setComponents("Externally-created scenario");
            sr.setCreatedDate(new Date());
            sr.setStatus("No");
            ScenarioRow[] newRun = { sr };
            ScenarioTable.addToListOfRunFiles(newRun);
        }
    }

    /**
     * Opens the configuration file for the selected scenarios in a text editor.
     * Uses the system's default text editor.
     */
    private void handleViewConfig() {
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        for (ScenarioRow row : selectedFiles) {
            String scenName = row.getScenarioName();
            String xmlFile = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenName);
            files.showFileInTextEditor(xmlFile);
        }
    }

    /**
     * Opens the main log file for the selected scenarios in a text editor.
     * Uses the system's default text editor.
     */
    private void handleViewLog() {
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        for (ScenarioRow row : selectedFiles) {
            String scenName = row.getScenarioName();
            String txtFile = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenName);
            files.showFileInTextEditor(txtFile);
        }
    }

    /**
     * Opens the main log file in the executable logs directory in a text editor.
     * Uses the system's default text editor.
     */
    private void handleViewExeLog() {
        String filename = ScenarioLibraryPathHelper.exeMainLogFile(vars.getgCamExecutableDir());
        files.showFileInTextEditor(filename);
    }

    /**
     * Compares the configuration files of two selected scenarios using a diff tool.
     * Only works if exactly two scenarios are selected.
     */
    private void handleDiffFiles() {
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        if (selectedFiles.size() != 2) {
            utils.warningMessage("Diff requires exactly two selected scenarios.");
            return;
        }

        String sName1 = selectedFiles.get(0).getScenarioName();
        String sName2 = selectedFiles.get(1).getScenarioName();
        String file1 = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), sName1);
        String file2 = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), sName2);

        try {
            List<DiffLineRow> rows = utils.generateSideBySideDiffRows(file1, file2);
            DiffWindow.show(file1, file2, rows);
        } catch (Exception e) {
            utils.warningMessage("Problem generating diff: " + e.getMessage());
        }
    }

    /**
     * Displays the current run queue in a popup window.
     * Shows scenarios added to the queue and completed runs for the session.
     */
    private void handleShowRunQueue() {
        // Big-win UI: show a dedicated, searchable table window.
        if ((runsQueuedList == null || runsQueuedList.isEmpty()) && (runsCompletedList == null || runsCompletedList.isEmpty())) {
            utils.warningMessage("No queued runs this session.");
            return;
        }

        try {
            // Provide a live supplier so the Refresh button and auto-refresh can re-pull the latest lists.
            QueueWindow.show(Client.primaryStage, () -> new QueueWindow.QueueData(
                    new ArrayList<>(runsQueuedList),
                    new ArrayList<>(runsCompletedList)));
        } catch (Exception e) {
            // Fallback: old text window
            ArrayList<String> txtArray = createSimpleQueueRpt();
            utils.displayArrayList(txtArray, "Run Queue");
        }
    }

    /**
     * Returns the HBox containing the scenario library table and controls.
     *
     * @return the HBox containing the scenario library UI
     */
    public HBox gethBox() {
        return scenarioLibraryHBox;
    }

    /**
     * Returns a simple report of the run queue and completed runs.
     *
     * @return ArrayList of report lines for the run queue
     */
    protected ArrayList<String> createSimpleQueueRpt() {
        return ScenarioLibraryReportHelper.createSimpleQueueReport(runsQueuedList, runsCompletedList);
    }

    private void dequeueScenariosAndClearStatus(ObservableList<ScenarioRow> scenariosToDequeue) {
        if (scenariosToDequeue == null || scenariosToDequeue.isEmpty()) {
            return;
        }
        for (ScenarioRow row : scenariosToDequeue) {
            if (row == null) {
                continue;
            }
            String scenName = row.getScenarioName();
            if (scenName == null) {
                continue;
            }
            while (runsQueuedList.remove(scenName)) {
                // keep removing duplicates
            }
            if ("In queue".equals(row.getStatus())) {
                row.setStatus("");
            }
        }
        ScenarioTable.tableScenariosLibrary.refresh();
    }

    private String getComponentsFromConfig(File file) {
        return ScenarioLibraryReportHelper.getComponentsFromConfig(file);
    }

    private void clearScenarioRunStatusFields(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            try {
                for (ScenarioRow s : ScenarioTable.listOfScenarioRuns) {
                    if (s != null && scenarioName.equals(s.getScenarioName())) {
                        s.setStatus("Updating...");
                        s.setRuntime("");
                        s.setUnsolvedMarkets("");
                        s.setCompletedDate("");
                        break;
                    }
                }
                ScenarioTable.tableScenariosLibrary.refresh();
            } catch (Exception ignored) {
            }
        });
    }

    private void copyExecutableMainLogToScenarioFolderWithRetry(String scenarioName, int maxAttempts, long retrySleepMillis) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        int attempts = Math.max(1, maxAttempts);
        long sleepMillis = Math.max(0L, retrySleepMillis);
        Exception lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Path src = ScenarioLibraryPathHelper.exeMainLogPath(vars.getgCamExecutableDir());
                if (!Files.exists(src)) {
                    return;
                }
                Path destDir = Paths.get(ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName));
                if (!Files.exists(destDir)) {
                    return;
                }
                Path dest = ScenarioLibraryPathHelper.scenarioMainLogPath(vars.getScenarioDir(), scenarioName);
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (Exception e) {
                lastError = e;
                if (attempt < attempts && sleepMillis > 0L) {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.out.println("Interrupted while retrying exe main_log.txt copy for '" + scenarioName + "'.");
                        return;
                    }
                }
            }
        }
        if (lastError != null) {
            System.out.println("Problem copying exe main_log.txt to scenario folder for '" + scenarioName + "': " + lastError);
        }
    }

    private void runGcamOnSelected() throws IOException {
        if (!WindowsRuntimePreflight.ensureMsvcRuntimeAvailableOrWarn(utils, "GCAM run")) {
            System.out.println("GCAM launch blocked: missing Microsoft Visual C++ runtime.");
            return;
        }
        ObservableList<ScenarioRow> selectedScenarioRows = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        String[] configFiles = new String[selectedScenarioRows.size()];
        int idx = 0;
        for (ScenarioRow mfr : selectedScenarioRows) {
            mfr.setCreatedDate(new Date());
            String scenName = mfr.getScenarioName();
            String mainLogFile = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenName);
            boolean b = true;
            if (files.doesFileExist(mainLogFile)) {
                String s = "main_log.txt exists for " + scenName + ". Run anyway?";
                b = utils.selectYesOrNoDialog(s);
            }
            if (b) {
                clearScenarioRunStatusFields(scenName);
                files.deleteFile(mainLogFile);
                try {
                    files.deleteFile(ScenarioLibraryPathHelper.scenarioStdoutFile(vars.getScenarioDir(), scenName));
                    files.deleteFile(ScenarioLibraryPathHelper.scenarioErrorFile(vars.getScenarioDir(), scenName));
                } catch (Exception ignored) {}

                configFiles[idx] = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenName);
                mfr.setStatus("In queue");
            } else {
                configFiles[idx] = null;
            }
            try {
                String archiveConfigFilename = configFiles[idx] != null
                        ? ScenarioLibraryPathHelper.scenarioArchiveConfigFile(vars.getScenarioDir(), scenName)
                        : null;
                if (archiveConfigFilename != null) {
                    File archiveConfigFile = new File(archiveConfigFilename);
                    if (archiveConfigFile.exists()) {
                        String s = "Run " + scenName + " from archive?";
                        if (utils.selectYesOrNoDialog(s)) {
                            configFiles[idx] = archiveConfigFilename;
                            mfr.setCreatedDate(new Date(archiveConfigFile.lastModified()));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Problem checking on existence of archive. Attempting to continue from non-archived files.");
            }
            idx++;
        }
        runGcamModel(configFiles);
    }

    private void runGcamModel(String[] scenarioConfigFiles) throws IOException {
        if (!WindowsRuntimePreflight.ensureMsvcRuntimeAvailableOrWarn(utils, "GCAM run")) {
            System.out.println("GCAM launch blocked: missing Microsoft Visual C++ runtime.");
            return;
        }
        System.out.println("Running scenarios in GCAM...");
        for (String scenarioConfigFile : scenarioConfigFiles) {
            if (scenarioConfigFile == null) {
                continue;
            }

            final String dir = scenarioConfigFile.substring(0, scenarioConfigFile.lastIndexOf(File.separator)).replaceAll("/", File.separator);
            System.out.println("config: " + scenarioConfigFile);

            final String queuedScenarioName = new File(dir).getName();
            this.runsQueuedList.add(queuedScenarioName);

            Client.gCAMExecutionThread.executeCallableCmd(ExecutionThread.namedCallable(
                    "GCAM pre-clean: scenario=" + queuedScenarioName + ", config=" + new File(scenarioConfigFile).getName(),
                     new Callable<String>() {
                @Override
                public String call() throws Exception {
                    System.out.println("Cleaning out folder.");
                    String[] filesToDelete = vars.getFilesToSave().replaceAll("/", File.separator).split(";");
                    for (String fileToDelete : filesToDelete) {
                        String file = dir + File.separator + fileToDelete.substring(fileToDelete.lastIndexOf(File.separator) + 1);
                        System.out.println(" Deleting " + file);
                        File f = new File(file);
                        if (f.exists()) {
                            try {
                                Path pathOfFileToDelete = Paths.get(file);
                                Files.delete(pathOfFileToDelete);
                            } catch (Exception e1) {
                                utils.warningMessage("Error deleting " + file);
                                System.out.println("Error deleting " + file + ":" + e1);
                            }
                        }
                    }
                    return "txt and log files deleted from scenario folder";
                }
            }));

            Future<ProcessResult> gcamFuture = Client.gCAMExecutionThread.submitCallable(ExecutionThread.namedCallable(
                    "GCAM run: scenario=" + queuedScenarioName + ", config=" + new File(scenarioConfigFile).getName(),
                     new Callable<ProcessResult>() {
                @Override
                public ProcessResult call() throws Exception {
                    lastGcamStopResult = null;
                    lastStoppedGcamScenarioName = null;

                    boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
                    System.out.println("OS detected as Windows: " + isWindows);

                    ArrayList<String> cmd = new ArrayList<>();
                    String exeDir = vars.getgCamExecutableDir();
                    String exeName = vars.getgCamExecutable();
                    String exePath = exeDir + File.separator + exeName;
                    cmd.add(exePath);

                    String args = vars.getgCamExecutableArgs();
                    if (args != null && args.trim().length() > 0) {
                        for (String a : CommandLineTokenizer.tokenize(args)) {
                            if (a != null && a.length() > 0) {
                                cmd.add(a);
                            }
                        }
                    }
                    cmd.add(scenarioConfigFile);

                    System.out.println("======" + System.lineSeparator() + "Command to run (direct): " + cmd);
                    ConsoleManager.clear(ConsoleManager.StreamSource.GCAM_STDOUT);
                    ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "Starting GCAM");
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "cmd: " + cmd);
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "exe: " + new File(exePath).getAbsolutePath());
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "working dir: " + new File(exeDir).getAbsolutePath());

                    FileTailer.TailHandle tailHandle = null;
                    if (TAIL_GCAM_MAIN_LOG_TO_CONSOLE) {
                        try {
                            final Path mainLog = Paths.get(exeDir, "logs", "main_log.txt");
                            try {
                                ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                        ConsoleManager.MessageKind.GLIMPSE_INFO,
                                        "Tailing: " + mainLog.toAbsolutePath());
                            } catch (Exception ignored) {}

                            tailHandle = FileTailer.start(
                                    mainLog,
                                    java.nio.charset.StandardCharsets.UTF_8,
                                    java.time.Duration.ofMillis(200),
                                    java.time.Duration.ofSeconds(30),
                                    line -> {
                                        if (line == null || line.trim().isEmpty()) {
                                            return;
                                        }
                                        if (isTailedLogLine(line)) {
                                            return;
                                        }
                                        ConsoleManager.appendLineBuffered(ConsoleManager.StreamSource.GCAM_STDOUT,
                                                ConsoleManager.MessageKind.MODEL_STDOUT,
                                                TAILED_LOG_PREFIX + line);
                                    }
                            );
                        } catch (Exception e) {
                            try {
                                ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                        ConsoleManager.MessageKind.GLIMPSE_INFO,
                                        "Tailer disabled (error starting tail): " + e);
                            } catch (Exception ignored) {}
                            tailHandle = null;
                        }
                    }

                    try {
                        ProcessRunner.RunningProcess rp = ProcessRunner.start(
                                cmd,
                                new File(exeDir),
                                null,
                                line -> {
                                    handlePotentialInteractivePrompt(line);
                                    maybeMarkLiveGcamSuccess(line);
                                    if (isTailedLogLine(line)) {
                                        return;
                                    }
                                    ConsoleManager.appendLineBuffered(ConsoleManager.StreamSource.GCAM_STDOUT,
                                            ConsoleManager.MessageKind.MODEL_STDOUT, line);
                                },
                                line -> {
                                    handlePotentialInteractivePrompt(line);
                                    maybeMarkLiveGcamSuccess(line);
                                    ConsoleManager.appendLineBuffered(ConsoleManager.StreamSource.GCAM_STDOUT,
                                            ConsoleManager.MessageKind.STDERR, line);
                                }
                        );

                        currentGcamRun = rp;
                        currentGcamScenarioName = queuedScenarioName;
                        lastStoppedGcamScenarioName = null;
                        clearScenarioRunStatusFields(queuedScenarioName);

                        Platform.runLater(() -> {
                            try {
                                if (Client.buttonStopScenario != null) {
                                    Client.buttonStopScenario.setDisable(false);
                                }
                            } catch (Exception ignored) {}
                        });

                        return rp.waitForResult(null);
                    } finally {
                        try {
                            if (tailHandle != null && !tailHandle.isStopRequested()) {
                                tailHandle.stop();
                                tailHandle.join(java.time.Duration.ofSeconds(2));
                            }
                        } catch (Exception ignored) {}

                        try {
                            ConsoleManager.flushBuffered();
                        } catch (Exception ignored) {}
                        currentGcamRun = null;
                        currentGcamScenarioName = null;
                        Platform.runLater(() -> {
                            try {
                                if (Client.buttonStopScenario != null) {
                                    Client.buttonStopScenario.setDisable(true);
                                }
                            } catch (Exception ignored) {}
                        });
                    }
                }
            }));

            currentGcamFuture = gcamFuture;

            Client.gCAMExecutionThread.executeCallableCmd(ExecutionThread.namedCallable(
                    "GCAM post-process: scenario=" + queuedScenarioName + ", config=" + new File(scenarioConfigFile).getName(),
                     new Callable<String>() {
                 @Override
                 public String call() throws Exception {
                    ProcessResult result;
                    boolean wasCancelled = false;
                    try {
                        result = gcamFuture.get();
                    } catch (java.util.concurrent.CancellationException ce) {
                        wasCancelled = true;
                        result = new ProcessResult(-1, "", "GCAM run was canceled by user.", false, 0);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        wasCancelled = true;
                        result = new ProcessResult(-1, "", "GCAM run was interrupted.", false, 0);
                    } catch (Exception e) {
                        result = new ProcessResult(-1, "", "Exception waiting for GCAM: " + e, false, 0);
                    } finally {
                        if (currentGcamFuture == gcamFuture) {
                            currentGcamFuture = null;
                        }
                    }

                    boolean stopRequested = false;
                    try {
                        ProcessRunner.StopResult sr = lastGcamStopResult;
                        if (sr != null) {
                            stopRequested = true;
                            ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "Stop result");
                            ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                    ConsoleManager.MessageKind.GLIMPSE_INFO,
                                    sr.getSummary());
                        }
                        ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "GCAM finished");
                        ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                ConsoleManager.MessageKind.GLIMPSE_INFO,
                                "exitCode=" + result.getExitCode() + ", cancelled=" + wasCancelled + ", success=" + result.isSuccess()
                                        + (stopRequested ? ", stopRequested=true" : ""));
                    } catch (Exception ignored) {}

                    String scenName = new File(dir).getName();
                    copyExecutableMainLogToScenarioFolderWithRetry(scenName, stopRequested ? 5 : 3, 400L);
                    if (stopRequested || wasCancelled) {
                        markScenarioStoppedDnF(scenName);
                    }

                    String gcamStdoutFile = ScenarioLibraryPathHelper.scenarioStdoutFile(vars.getScenarioDir(), scenName);
                    String mainErrFile = ScenarioLibraryPathHelper.scenarioErrorFile(vars.getScenarioDir(), scenName);

                    try {
                        files.saveFile(
                                "GCAM exitCode=" + result.getExitCode() + System.lineSeparator() + result.getStdout(),
                                gcamStdoutFile);
                        if (result.getStderr() != null && result.getStderr().trim().length() > 0) {
                            files.saveFile(
                                    "GCAM exitCode=" + result.getExitCode() + System.lineSeparator() + result.getStderr(),
                                    mainErrFile);
                        }
                    } catch (Exception e) {
                        System.out.println("Problem writing GCAM stdout/stderr logs: " + e);
                    }

                    System.out.println("GCAM finished with exitCode=" + result.getExitCode() + System.lineSeparator() + "=========");

                    boolean shouldMoveOutputs = result.isSuccess() || stopRequested || wasCancelled;

                    if (!shouldMoveOutputs) {
                        System.out.println("GCAM run failed (or timed out); skipping move of output files.");
                        updateRunStatus();
                        return "GCAM run failed";
                    }

                    if (stopRequested || wasCancelled) {
                        System.out.println("GCAM was stopped by user; attempting to move any generated output files.");
                    }

                    System.out.println("Moving results to scenario folder.");
                    if ((vars.getFilesToSave() != null) && (vars.getFilesToSave().length() > 0)) {
                        String[] filesToSave = vars.getFilesToSave().replaceAll("/", File.separator).split(";");
                        for (String fileToSave : filesToSave) {
                            File file = new File(fileToSave);
                            if (file.exists()) {
                                Path source = Paths.get(fileToSave);
                                String destinationStr = dir + File.separator + fileToSave.substring(fileToSave.lastIndexOf(File.separator) + 1);
                                Path destination = Paths.get(destinationStr);
                                System.out.println(" Moving " + fileToSave + " to " + destination);
                                try {
                                    files.moveFile(source, destination);
                                } catch (Exception e1) {
                                    System.out.println("Problem moving file " + fileToSave);
                                    System.out.println("Exception " + e1);
                                }
                                File destf = new File(destinationStr);
                                if (!destf.exists()) {
                                    System.out.println("Problem moving file " + fileToSave);
                                }
                                if (file.exists()) {
                                    files.deleteFile(file);
                                }
                            } else {
                                System.out.println("Unable to save " + fileToSave);
                            }
                        }
                    }
                    updateRunStatus();
                    return "moving specified files to scenario folder";
                }
            }));
        }
    }

    private void runModelInterface() throws IOException {
        String database = vars.getgCamOutputDatabase();
        runModelInterfaceWhich(database);
    }

    private void runModelInterfaceWhich(String database_name) throws IOException {
        final String modelInterfaceDirStr = vars.getModelInterfaceDir();
        final File modelInterfaceDir = (modelInterfaceDirStr == null) ? null : new File(modelInterfaceDirStr);
        final String jarName = vars.getModelInterfaceJar();

        ArrayList<String> problems = new ArrayList<>();
        if (modelInterfaceDir == null || modelInterfaceDirStr == null || modelInterfaceDirStr.trim().isEmpty()) {
            problems.add("ModelInterface directory is not set.");
        } else if (!modelInterfaceDir.isDirectory()) {
            problems.add("ModelInterface directory does not exist: " + modelInterfaceDir.getAbsolutePath());
        }

        File jarFile = null;
        if (jarName == null || jarName.trim().isEmpty()) {
            problems.add("ModelInterface jar file name is not set.");
        } else if (modelInterfaceDir != null) {
            jarFile = new File(modelInterfaceDir, jarName);
            if (!jarFile.isFile()) {
                problems.add("ModelInterface jar not found: " + jarFile.getAbsolutePath());
            }
        }

        if (database_name == null || database_name.trim().isEmpty()) {
            problems.add("Output database path is not set.");
        } else {
            File db = new File(database_name);
            if (!db.exists()) {
                boolean created = promptCreateDatabaseNotice(db);
                if (!created) {
                    problems.add("Database not found: " + db.getAbsolutePath());
                }
            }
        }

        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Query file", vars.getQueryFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Units conversion file", vars.getUnitConversionsFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Preset region list file", vars.getPresetRegionListFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Favorite queries file", vars.getFavoriteQueryFilename());

        File mapsDir = null;
        if (modelInterfaceDir != null) {
            mapsDir = new File(modelInterfaceDir, "map_resources");
            if (!mapsDir.isDirectory()) {
                problems.add("Map resources directory not found: " + mapsDir.getAbsolutePath());
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Please fix:\n\n");
            for (String p : problems) {
                sb.append(" - ").append(p).append("\n");
            }
            showNotice("Unable to start ModelInterface.", sb.toString());
            System.out.println("Unable to start ModelInterface. " + sb.toString());
            return;
        }

        final ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(jarFile.getAbsolutePath());
        args.add("-o");
        args.add(database_name);

        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-q", vars.getQueryFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-u", vars.getUnitConversionsFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-p", vars.getPresetRegionListFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-f", vars.getFavoriteQueryFilename());
        if (mapsDir != null) {
            args.add("-m");
            args.add(mapsDir.getAbsolutePath());
        }

        System.out.println("Starting " + jarName + " using database " + database_name);
        System.out.println(">>   cmd args: " + args);
        System.out.println(">>   working dir: " + modelInterfaceDir.getAbsolutePath());

        ConsoleManager.appendHeader(ConsoleManager.StreamSource.MODEL_INTERFACE, "Starting ModelInterface");
        ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE, "cmd args: " + args);
        ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE, "working dir: " + modelInterfaceDir.getAbsolutePath());

        try {
            Client.modelInterfaceExecutionThread.submitCommandWithDirectory(args, modelInterfaceDir.getAbsolutePath());
        } catch (Exception e) {
            utils.warningMessage("Problem starting up ModelInterface. See console for details.");
            System.out.println("Error in trying to start up ModelInterface:");
            e.printStackTrace();
            ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE, "ERROR: " + e);
        }
    }

    private void showNotice(String header, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean promptCreateDatabaseNotice(File dbFile) {
        if (dbFile == null) {
            return false;
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText("Output database not found.");
        alert.setContentText("Create and initialize a new database at:\n" + dbFile.getAbsolutePath());

        ButtonType createBtn = ScenarioLibraryModelInterfaceMiniHelper.createOkButton("Create");
        ButtonType okBtn = ScenarioLibraryModelInterfaceMiniHelper.createCancelCloseButton("OK");
        alert.getButtonTypes().setAll(createBtn, okBtn);

        ScenarioLibraryModelInterfaceMiniHelper.setDefaultButton(alert.getDialogPane(), okBtn);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() != createBtn) {
            return false;
        }

        return initializeDatabaseWithModelInterface(dbFile);
    }

    private boolean initializeDatabaseWithModelInterface(File dbFile) {
        if (dbFile == null) {
            return false;
        }

        String modelInterfaceDirStr = vars.getModelInterfaceDir();
        String jarName = vars.getModelInterfaceJar();
        if (modelInterfaceDirStr == null || modelInterfaceDirStr.trim().isEmpty()) {
            showNotice("Unable to initialize database.", "ModelInterface directory is not set.");
            return false;
        }
        if (jarName == null || jarName.trim().isEmpty()) {
            showNotice("Unable to initialize database.", "ModelInterface jar file name is not set.");
            return false;
        }

        File modelInterfaceDir = new File(modelInterfaceDirStr);
        File jarFile = new File(modelInterfaceDir, jarName);
        if (!jarFile.isFile()) {
            showNotice("Unable to initialize database.", "ModelInterface jar not found:\n" + jarFile.getAbsolutePath());
            return false;
        }

        try {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            ArrayList<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.add("-jar");
            cmd.add(jarFile.getAbsolutePath());
            cmd.add("-o");
            cmd.add(dbFile.getAbsolutePath());
            cmd.add("--init-db");

            Dialog<Void> progress = new Dialog<>();
            progress.setTitle("Notice");
            progress.setHeaderText("Initializing database...");
            Label msg = new Label("Creating and initializing database. Please wait...");
            ProgressIndicator indicator = new ProgressIndicator();
            VBox content = new VBox(10, msg, indicator);
            content.setAlignment(Pos.CENTER_LEFT);
            progress.getDialogPane().setContent(content);
            progress.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            Button cancelBtn = (Button) progress.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null) {
                cancelBtn.setDisable(true);
            }

            Task<ProcessResult> task = new Task<ProcessResult>() {
                @Override
                protected ProcessResult call() throws Exception {
                    return ProcessRunner.run(cmd, modelInterfaceDir, null, Duration.ofMinutes(2));
                }
            };
            task.setOnSucceeded(e -> progress.close());
            task.setOnFailed(e -> progress.close());
            task.setOnCancelled(e -> progress.close());

            Thread initThread = new Thread(task, "ModelInterface-InitDB");
            initThread.setDaemon(true);
            initThread.start();

            progress.showAndWait();

            ProcessResult result = task.get();
            if (result.isSuccess() && dbFile.exists()) {
                return true;
            }

            String detail = "exitCode=" + result.getExitCode();
            if (result.isTimedOut()) {
                detail += ", timedOut=true";
            }
            if (result.getStderr() != null && !result.getStderr().trim().isEmpty()) {
                detail += "\n\n" + result.getStderr().trim();
            } else if (result.getStdout() != null && !result.getStdout().trim().isEmpty()) {
                detail += "\n\n" + result.getStdout().trim();
            }
            showNotice("Database initialization failed.", detail);
            return false;
        } catch (Exception e) {
            showNotice("Database initialization failed.", String.valueOf(e));
            return false;
        }
    }

    /**
     * Updates the run status for all scenarios and refreshes the table.
     * Reads log files and updates scenario status, runtime, and unsolved markets.
     * Also updates the UI with computer stats and logs status changes.
     */
    public void updateRunStatus() {
        final String currentMainLogName = ScenarioLibraryPathHelper.exeMainLogFile(vars.getgCamExecutableDir());
        final File currentMainLogFile = new File(currentMainLogName);
        final String runningScenario = utils.getRunningScenario(currentMainLogFile);
        final String stopRequestedScenario = getStopRequestedScenarioName();
        final DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);
        final ArrayList<ScenarioStatusSnapshot> snapshots = new ArrayList<>();
        final boolean[] noScenarios = new boolean[] { false };

        updateStatusBarComputerStats(runningScenario);

        try {
            File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
            if (scenarioFolders == null) {
                noScenarios[0] = true;
            } else {
                for (File scenarioFolder : scenarioFolders) {
                    ScenarioStatusSnapshot snapshot = buildScenarioStatusSnapshot(
                            scenarioFolder, currentMainLogFile, runningScenario, stopRequestedScenario, format2);
                    if (snapshot != null) {
                        snapshots.add(snapshot);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Problem updating scenario table: " + ex);
            Platform.runLater(() -> {
                if (ScenarioTable.tableScenariosLibrary != null) {
                    ScenarioTable.tableScenariosLibrary.refresh();
                }
            });
            return;
        }

        Platform.runLater(() -> applyScenarioStatusSnapshots(snapshots, noScenarios[0]));
    }

    private void updateStatusBarComputerStats(String runningScenario) {
        Platform.runLater(() -> {
            String computerStats = utils.getComputerStatString().trim();
            if (computerStats.endsWith("!!!")) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();
                String time = formatter.format(date);
                String glimpseLogFilename = vars.getGlimpseLogDir() + File.separator + "glimpse_log.txt";
                String logText = runningScenario + ":" + time + ":" + computerStats + vars.getEol();
                files.appendTextToFile(logText, glimpseLogFilename);
            }
            String statusStyle = computerStats.endsWith("!") ? "-fx-text-fill: red" : "-fx-text-fill: black";
            Client.setDeferredStatusBarText(computerStats, statusStyle);
            if (!Client.isStartupBusy() && utils.sb != null) {
                utils.sb.setText(computerStats);
                utils.sb.setStyle(statusStyle);
            }
        });
    }

    private ScenarioStatusSnapshot buildScenarioStatusSnapshot(File scenarioFolder, File currentMainLogFile,
            String runningScenario, String stopRequestedScenario, DateFormat format2) {
        ArrayList<String> searchArray = new ArrayList<>();
        searchArray.add("Model run completed.");
        searchArray.add("Data Readin, Model Run & Write Time:");
        searchArray.add("The following model periods did not solve:");

        Long createdDate = 0L;
        Long completedDate = 0L;
        String scenarioName = scenarioFolder.getName();
        String configName = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
        File configFile = new File(configName);
        if (!configFile.exists()) {
            return null;
        }

        String components = getComponentsFromConfig(configFile);
        String mainLogName = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
        File mainLogFile = new File(mainLogName);
        boolean mainLogExists = mainLogFile.exists();
        String status = "";
        String runtime = "";
        String unsolved = "";
        createdDate = configFile.lastModified();

        if (mainLogExists) {
            completedDate = mainLogFile.lastModified();
            searchArray = files.getMatchingTextArrayInFile(mainLogName, searchArray);
            if (!searchArray.get(0).isEmpty()) {
                status = "Success";
            } else {
                status = "DNF";
                String runningStatus = utils.getScenarioStatusFromMainLog(mainLogFile);
                if (runningStatus.contains(",ERR")) {
                    String errorStr = runningStatus.substring(runningStatus.indexOf(",") + 4);
                    unsolved = errorStr;
                }
            }
            for (int i = 0; i < runsQueuedList.size(); i++) {
                String line = runsQueuedList.get(i);
                if (line.equals(scenarioName)) {
                    status = "In queue";
                    if (mainLogExists) {
                        runsCompletedList.add(line);
                        runsQueuedList.remove(i);
                    }
                    break;
                }
            }
        }

        if (!"Success".equals(status) && hasStdoutSuccessMarker(scenarioFolder)) {
            status = "Success";
        }
        if (!searchArray.get(1).isEmpty()) {
            try {
                runtime = searchArray.get(1).split(":")[1].trim();
            } catch (Exception e) {
                runtime = "";
            }
            runtime = runtime.replace("seconds.", "").trim();
            try {
                int totalSecs = (int) Math.round(Float.parseFloat(runtime));
                int hours = (totalSecs - totalSecs % 3600) / 3600;
                int minutes = (totalSecs % 3600 - totalSecs % 3600 % 60) / 60;
                runtime = hours + " hr " + minutes + " min ";
            } catch (Exception e) {
                runtime += "";
            }
        }
        if (!searchArray.get(2).isEmpty()) {
            try {
                unsolved = searchArray.get(2).split(":")[1].trim();
                status = "Unsolved mkts";
            } catch (Exception e) {
                unsolved = "";
            }
        }

        String createdDateStr = createdDate != 0L ? format2.format(createdDate) : "";
        String completedDateStr = completedDate != 0L ? format2.format(completedDate) : "";
        if ((!status.equals("Success")) && (!status.equals("Unsolved mkts")) && (!status.equals("DNF"))) {
            if (scenarioName.equals(runningScenario)) {
                status = "Running";
                long lastDate = currentMainLogFile.lastModified();
                boolean isQueued = runsQueuedList.contains(scenarioName);
                long graceMs = 30_000L;
                if (!isQueued && (startupTime > 0) && (System.currentTimeMillis() - startupTime > graceMs)
                        && lastDate < startupTime) {
                    status = "Lost handle";
                } else {
                    String runningStatus = utils.getScenarioStatusFromMainLog(currentMainLogFile);
                    String explicitRunState = getExplicitRunStateLabel(scenarioName, currentMainLogFile, runningStatus);
                    if (!explicitRunState.isEmpty()) {
                        status = explicitRunState;
                    } else if (runningStatus.contains(",ERR")) {
                        String temp = runningStatus.substring(0, runningStatus.indexOf(","));
                        status = status + "(" + temp + ")";
                        String errorStr = runningStatus.substring(runningStatus.indexOf(",") + 4);
                        unsolved = errorStr;
                    } else {
                        String temp = runningStatus;
                        if (!temp.isEmpty()) {
                            status = status + "(" + temp + ")";
                        }
                    }
                }
            } else {
                for (String line : runsQueuedList) {
                    if (line.equals(scenarioName)) {
                        status = "In queue";
                        break;
                    }
                }
            }
        }
        if ((status.isEmpty() || status.startsWith("Running") || GCAM_STATUS_BLOCKED.equals(status)
                || GCAM_STATUS_WRITING.equals(status))
                && scenarioName.equals(stopRequestedScenario) && !isScenarioActivelyRunning(scenarioName)) {
            status = "DNF";
        }

        return new ScenarioStatusSnapshot(scenarioName, components, createdDateStr, completedDateStr, status, runtime, unsolved);
    }

    private void applyScenarioStatusSnapshots(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios) {
        if (ScenarioTable.tableScenariosLibrary == null) {
            return;
        }
        ScenarioTable.tableScenariosLibrary.refresh();
        if (noScenarios) {
            ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(NO_SCENARIOS_MESSAGE));
            return;
        }
        for (ScenarioStatusSnapshot snapshot : snapshots) {
            boolean match = false;
            for (ScenarioRow s : ScenarioTable.listOfScenarioRuns) {
                if (s.getScenarioName().equals(snapshot.scenarioName)) {
                    match = true;
                    if (!s.getStatus().equals("In queue") || !snapshot.status.isEmpty()) {
                        s.setStatus(snapshot.status);
                    }
                    s.setCreatedDate(snapshot.createdDate);
                    s.setCompletedDate(snapshot.completedDate);
                    s.setComponents(snapshot.components);
                    s.setRuntime(snapshot.runtime);
                    s.setUnsolvedMarkets(snapshot.unsolved);
                }
            }
            if (!match) {
                ScenarioRow sr = new ScenarioRow(snapshot.scenarioName);
                sr.setComponents(snapshot.components);
                sr.setCreatedDate(snapshot.createdDate);
                sr.setCompletedDate(snapshot.completedDate);
                if (!"In queue".equals(sr.getStatus()) || !snapshot.status.isEmpty()) {
                    sr.setStatus(snapshot.status);
                }
                sr.setRuntime(snapshot.runtime);
                sr.setUnsolvedMarkets(snapshot.unsolved);
                ScenarioTable.listOfScenarioRuns.add(sr);
            }
        }
        if (ScenarioTable.listOfScenarioRuns.isEmpty()) {
            ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(NO_SCENARIOS_MESSAGE));
        }
        ScenarioTable.tableScenariosLibrary.refresh();
    }

    // --- Added: Explicit run-state handling ---
    private String getExplicitRunStateLabel(String scenarioName, File currentMainLogFile, String runningStatus) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return "";
        }
        if (scenarioName.equals(getStopRequestedScenarioName()) && !isScenarioActivelyRunning(scenarioName)) {
            return "DNF";
        }
        if (currentGcamScenarioName == null || !scenarioName.equals(currentGcamScenarioName)) {
            return "";
        }
        if (gcamPromptDialogActive || databasePromptAwaitingReset) {
            return GCAM_STATUS_BLOCKED;
        }
        if (isWritingResultsPhase(currentMainLogFile, runningStatus)) {
            return GCAM_STATUS_WRITING;
        }
        return "";
    }

    private String getStopRequestedScenarioName() {
        String currentScenario = currentGcamScenarioName;
        ProcessRunner.RunningProcess rp = currentGcamRun;
        if (currentScenario != null && rp != null && rp.isStopRequested()) {
            return currentScenario;
        }
        return lastStoppedGcamScenarioName;
    }

    private boolean isScenarioActivelyRunning(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return false;
        }
        ProcessRunner.RunningProcess rp = currentGcamRun;
        return scenarioName.equals(currentGcamScenarioName)
                && rp != null
                && rp.getProcess() != null
                && rp.getProcess().isAlive();
    }

    private void markScenarioStoppedDnF(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        lastStoppedGcamScenarioName = scenarioName;
        Platform.runLater(() -> {
            try {
                for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                    if (row == null || !scenarioName.equals(row.getScenarioName())) {
                        continue;
                    }
                    if (row.getCompletedDate() == null || row.getCompletedDate().trim().isEmpty()) {
                        DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);
                        row.setCompletedDate(format2.format(new Date()));
                    }
                    row.setStatus("DNF");
                    break;
                }
                ScenarioTable.tableScenariosLibrary.refresh();
            } catch (Exception ignored) {}
        });
    }

    private boolean isWritingResultsPhase(File currentMainLogFile, String runningStatus) {
        if (runningStatus != null && runningStatus.contains(",ERR")) {
            return false;
        }
        if (containsWritingPhrase(runningStatus)) {
            return true;
        }
        if (currentMainLogFile == null || !currentMainLogFile.exists()) {
            return false;
        }
        try {
            ArrayList<String> lines = files.getStringArrayFromFile(currentMainLogFile.getAbsolutePath(), "#");
            int start = Math.max(0, lines.size() - 40);
            for (int i = lines.size() - 1; i >= start; i--) {
                String line = lines.get(i);
                if (line == null) {
                    continue;
                }
                String normalized = normalizeDatabasePromptText(line);
                if (normalized.isEmpty()) {
                    continue;
                }
                if (normalized.contains("model run completed")) {
                    return false;
                }
                if (containsWritingPhrase(normalized)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean containsWritingPhrase(String text) {
        return ScenarioLibraryPromptHelper.containsWritingPhrase(text);
    }

    private void maybePromptForDatabaseRelease(String promptLine) {
        if (!looksLikeDatabaseSavePrompt(promptLine)) {
            return;
        }
        String normalizedPrompt = normalizeDatabasePromptText(promptLine);
        if (normalizedPrompt.isEmpty()) {
            return;
        }
        if (gcamPromptDialogActive) {
            return;
        }
        if (databasePromptAwaitingReset && normalizedPrompt.equals(lastHandledDatabasePrompt)) {
            return;
        }

        ProcessRunner.RunningProcess rp = currentGcamRun;
        if (rp == null || rp.getProcess() == null || !rp.getProcess().isAlive()) {
            return;
        }

        gcamPromptDialogActive = true;
        activeGcamPromptLine = promptLine;
        lastHandledDatabasePrompt = normalizedPrompt;
        databasePromptAwaitingReset = true;

        try {
            ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                    ConsoleManager.MessageKind.GLIMPSE_INFO,
                    "GCAM is waiting for ModelInterface/database access to be released.");
        } catch (Exception ignored) {}

        Platform.runLater(() -> {
            try {
                try {
                    updateRunStatus();
                } catch (Exception ignored) {}

                ProcessRunner.RunningProcess live = currentGcamRun;
                if (live == null || live.getProcess() == null || !live.getProcess().isAlive()) {
                    gcamPromptDialogActive = false;
                    activeGcamPromptLine = null;
                    return;
                }

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("GCAM waiting for database");
                alert.setHeaderText("Close ModelInterface to continue GCAM");

                Label msg = new Label(
                        "GCAM is ready to save results, but the output database is still in use.\n\n"
                      + "Please close ModelInterface, then press OK to let GCAM continue saving results.\n"
                      + "Press Cancel to stop the running GCAM scenario instead.");
                msg.setWrapText(true);
                msg.setMaxWidth(420);

                VBox content = new VBox(10);
                content.setFillWidth(true);
                content.setMaxWidth(440);
                content.getChildren().addAll(msg);
                alert.getDialogPane().setContent(content);
                alert.getDialogPane().setPrefWidth(480);
                alert.getDialogPane().setMaxWidth(480);

                ButtonType okBtn = ScenarioLibraryModelInterfaceMiniHelper.createOkButton("OK");
                ButtonType cancelBtn = ScenarioLibraryModelInterfaceMiniHelper.createCancelCloseButton("Cancel");
                alert.getButtonTypes().setAll(okBtn, cancelBtn);

                ScenarioLibraryModelInterfaceMiniHelper.setDefaultButton(alert.getDialogPane(), okBtn);

                Optional<ButtonType> result = alert.showAndWait();
                boolean confirmed = result.isPresent() && result.get() == okBtn;

                ProcessRunner.RunningProcess afterDialog = currentGcamRun;
                if (confirmed) {
                    boolean sent = false;
                    try {
                        if (afterDialog != null && afterDialog.getProcess() != null && afterDialog.getProcess().isAlive()) {
                            sent = afterDialog.sendLine();
                        }
                    } catch (Exception ignored) {}

                    clearRecentGcamPromptBuffer();
                    try {
                        updateRunStatus();
                    } catch (Exception ignored) {}

                    try {
                        ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                ConsoleManager.MessageKind.GLIMPSE_INFO,
                                sent ? "User confirmed database save prompt; sent Enter to GCAM."
                                        : "User confirmed database save prompt, but GLIMPSE could not send Enter to GCAM.");
                    } catch (Exception ignored) {}
                } else {
                    try {
                        ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                ConsoleManager.MessageKind.GLIMPSE_INFO,
                                "User canceled the GCAM database save prompt; stopping GCAM.");
                    } catch (Exception ignored) {}
                    stopCurrentGcamRun();
                }
            } finally {
                gcamPromptDialogActive = false;
                activeGcamPromptLine = null;
                try {
                    updateRunStatus();
                } catch (Exception ignored) {}
            }
        });
    }

    private void clearRecentGcamPromptBuffer() {
        synchronized (recentGcamPromptBuffer) {
            recentGcamPromptBuffer.setLength(0);
        }
    }

    private String appendAndGetRecentPromptWindow(String line) {
        if (line == null) {
            return "";
        }
        synchronized (recentGcamPromptBuffer) {
            if (recentGcamPromptBuffer.length() > 0) {
                recentGcamPromptBuffer.append(' ');
            }
            recentGcamPromptBuffer.append(line.trim());
            if (recentGcamPromptBuffer.length() > GCAM_PROMPT_BUFFER_MAX_CHARS) {
                recentGcamPromptBuffer.delete(0, recentGcamPromptBuffer.length() - GCAM_PROMPT_BUFFER_MAX_CHARS);
            }
            return recentGcamPromptBuffer.toString();
        }
    }

    private void handlePotentialInteractivePrompt(String line) {
        try {
            String promptWindow = appendAndGetRecentPromptWindow(line);
            String normalizedPromptWindow = normalizeDatabasePromptText(promptWindow);
            if (!looksLikeDatabaseSavePrompt(normalizedPromptWindow)) {
                if (databasePromptAwaitingReset) {
                    databasePromptAwaitingReset = false;
                    lastHandledDatabasePrompt = null;
                }
                return;
            }
            maybePromptForDatabaseRelease(promptWindow);
        } catch (Exception ignored) {}
    }

    private void maybeMarkLiveGcamSuccess(String line) {
        if (line == null) {
            return;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        for (String marker : GCAM_STDOUT_SUCCESS_MARKERS) {
            if (trimmed.contains(marker)) {
                String scenarioName = currentGcamScenarioName;
                if (scenarioName == null || scenarioName.trim().isEmpty()) {
                    return;
                }
                Platform.runLater(() -> {
                    try {
                        for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                            if (row != null && scenarioName.equals(row.getScenarioName())) {
                                row.setStatus("Success");
                                break;
                            }
                        }
                        ScenarioTable.tableScenariosLibrary.refresh();
                    } catch (Exception ignored) {}
                });
                return;
            }
        }
    }

    private boolean hasStdoutSuccessMarker(File scenarioFolder) {
        if (scenarioFolder == null) {
            return false;
        }
        try {
            File stdoutFile = new File(scenarioFolder, "gcam_stdout.txt");
            if (!stdoutFile.exists()) {
                return false;
            }
            ArrayList<String> lines = files.getStringArrayFromFile(stdoutFile.getAbsolutePath(), "#");
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line == null) {
                    continue;
                }
                for (String marker : GCAM_STDOUT_SUCCESS_MARKERS) {
                    if (line.contains(marker)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void archiveScenario(String exeDir, String workingDir, String archiveConfigFilename, String configFilename, String scenName) {
        ArrayList<String> config_content = files.getStringArrayFromFile(configFilename, "#");
        ArrayList<String> new_config_content = new ArrayList<String>();
        boolean inScenarioComponents = false;
        String archiveFoldername = workingDir + File.separator + "archive";
        File archiveFolder = new File(archiveFoldername);
        if (archiveFolder.exists()) {
            String msg = "Archive already exists. Replace?";
            if (!utils.selectYesOrNoDialog(msg)) {
                return;
            } else {
                for (File file : archiveFolder.listFiles()) {
                    if (!file.isDirectory())
                        file.delete();
                }
            }
        }
        for (String line : config_content) {
            if (line.indexOf("<ScenarioComponents>") >= 0) {
                inScenarioComponents = true;
            }
            if (line.indexOf("</ScenarioComponents>") >= 0) {
                inScenarioComponents = false;
            }
            if (inScenarioComponents) {
                if (line.indexOf("Value") >= 0) {
                    int start_index = line.indexOf('>') + 1;
                    int end_index = line.lastIndexOf('<');
                    String orig_path = line.substring(start_index, end_index);
                    Path origPath = Paths.get(orig_path);
                    Path exePath = Paths.get(exeDir);
                    Path sourcePath = exePath.resolve(origPath).normalize();
                    String destFilename = workingDir + File.separator + "archive" + File.separator + sourcePath.getFileName();
                    File destFile = new File(workingDir + File.separator + "archive" + File.separator + sourcePath.getFileName());
                    line = line.replace(orig_path, destFilename);
                    if (destFile.exists()) {
                        String msg = "Multiple files named " + sourcePath.getFileName() + ". Keeping last.";
                        utils.warningMessage(msg);
                        destFile.delete();
                    }
                    destFile.getParentFile().mkdir();
                    Path destPath = Paths.get(destFile.toString());
                    try {
                        Files.copy(sourcePath, destPath);
                    } catch (IOException e) {
                        System.out.println("Error during archiving:");
                        e.printStackTrace();
                    }
                }
            }
            new_config_content.add(line);
        }
        files.saveFile(new_config_content, archiveConfigFilename);
        String destFilename = archiveFolder + File.separator + "configuration_" + scenName + "_archive.xml";
        files.saveFile(new_config_content, destFilename);
        String zipFolder = workingDir + File.separator + "archive";
        File zipDir = new File(zipFolder);
        String zipFilename = workingDir + File.separator + "archive" + utils.getCurrentTimeStamp() + ".zip";
        File zipFile = new File(zipFilename);
        if (zipFile.exists())
            files.deleteDirectory(zipFile);
        files.zipDirectory(zipDir, zipFilename);
        System.out.println("Done archiving.");
    }

    private void generateExeErrorReport() {
        ArrayList<String> report = new ArrayList<String>();
        ObservableList<ScenarioRow> selectedScenarioRows = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        try {
            for (ScenarioRow row : selectedScenarioRows) {
                String scenarioName = "";
                String scenarioMainLog = vars.getgCamOutputDatabase() + File.separator + "logs" + File.separator + "main_log.txt";
                File mainlogfile = new File(scenarioMainLog);
                if (mainlogfile.exists()) {
                    ArrayList error_lines = utils.generateErrorReport(scenarioMainLog, scenarioName);
                    report.addAll(error_lines);
                }
            }
        } catch (Exception e) {
            System.out.println("error developing error log:" + e);
        }
        ArrayList<String> tableData = utils.buildErrorReportTable(report);
        utils.showPopupTableOfErrorReport("Error Report", tableData, 910, 600);
    }

    private void generateErrorReport() {
        ArrayList<String> report = new ArrayList<String>();
        ObservableList<ScenarioRow> selectedScenarioRows = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        try {
            for (ScenarioRow row : selectedScenarioRows) {
                String scenarioName = "" + row.getScenName();
                String scenarioMainLog = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
                File mainlogfile = new File(scenarioMainLog);
                if (mainlogfile.exists()) {
                    ArrayList error_lines = utils.generateErrorReport(scenarioMainLog, scenarioName);
                    report.addAll(error_lines);
                }
            }
        } catch (Exception e) {
            System.out.println("error developing error log:" + e);
        }
        ArrayList<String> tableData = utils.buildErrorReportTable(report);
        utils.showPopupTableOfErrorReport("Error Report", tableData, 910, 600);
    }

    private void generateRunReport() {
        ArrayList<String> report = new ArrayList<String>();
        File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
        String header = "scenario,created,run,status,version,#warn,#err,unsolved,errors,completed?,solution(sec),total(sec),components";
        report.add(header);
        if (scenarioFolders == null) {
            return;
        }
        for (File scenarioFolder : scenarioFolders) {
            if (scenarioFolder == null) {
                continue;
            }
            String scenarioName = scenarioFolder.getName();
            String configPath = scenarioFolder.getPath() + File.separator + "configuration_" + scenarioName + ".xml";
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                continue;
            }
            File mainLog = new File(scenarioFolder, "main_log.txt");
            if (!mainLog.exists()) {
                continue;
            }

            ArrayList<String> errorLines = utils.generateErrorReport(mainLog.getAbsolutePath(), scenarioName);
            int numErrors = errorLines == null ? 0 : errorLines.size();
            int numWarnings = countLogMatches(mainLog, "WARNING");
            String whenCreated = new Date(configFile.lastModified()).toString();
            String whenRun = new Date(mainLog.lastModified()).toString();
            String modelVersion = extractModelVersion(mainLog);
            String notSolved = extractUnsolvedMarkets(mainLog);
            boolean isCompleted = files.searchForTextInFileS(mainLog, "Model run completed.", "#") != null;
            String solutionTime = extractLastLogValue(mainLog, "Solution time:");
            String totalTime = extractLastLogValue(mainLog, "Data Readin, Model Run & Write Time:");
            String components = getComponentsFromConfig(configFile);
            String status = isCompleted ? "Success" : "DNF";
            if (!notSolved.isEmpty()) {
                status = "Unsolved mkts";
            }

            String row = scenarioName + "," + whenCreated + "," + whenRun + "," + status + "," + modelVersion + ", "
                    + numWarnings + "," + numErrors + "," + notSolved + ", "
                    + (errorLines == null ? "" : errorLines.toString().replaceAll(",", ";")) + ", "
                    + isCompleted + "," + solutionTime + "," + totalTime + "," + components;
            report.add(row);
        }
        TextArea window = new TextArea();
        window.setMaxWidth(1500);
        window.setMinWidth(1500);
        window.setMaxHeight(700);
        window.setMinHeight(700);
        window.setWrapText(false);
        window.setEditable(false);
        StringBuilder outputString = new StringBuilder();
        for (String line : report) {
            outputString.append(line).append(vars.getEol());
        }
        window.setText(outputString.toString());
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Scenario Report");
        dialog.getDialogPane().setContent(window);
        ButtonType exportBtn = new ButtonType("Export CSV", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(exportBtn, closeBtn);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == exportBtn) {
            File selected = FileChooserPlus.showSaveDialog(null, "Save report as CSV", new File(vars.getScenarioDir()), "scenario-report.csv",
                    FileChooserPlus.createExtensionFilter("CSV files (*.csv)", "csv"));
            if (selected != null) {
                files.saveFile(report, selected.getAbsolutePath());
            }
        }
    }

    private int countLogMatches(File mainLog, String token) {
        return ScenarioLibraryReportHelper.countLogMatches(files, mainLog, token);
    }

    private String extractModelVersion(File mainLog) {
        return ScenarioLibraryReportHelper.extractModelVersion(files, mainLog);
    }

    private String extractUnsolvedMarkets(File mainLog) {
        return ScenarioLibraryReportHelper.extractUnsolvedMarkets(files, mainLog);
    }

    private String extractLastLogValue(File mainLog, String prefix) {
        return ScenarioLibraryReportHelper.extractLastLogValue(files, mainLog, prefix);
    }

    /**
     * Backward-compatible helper that clears the scenario table and rebuilds it
     * from the scenario directory using the current refresh logic.
     */
    public void clearAndRefreshScenarioTable() {
        refreshScenarioStatusAsync(true);
    }

    public void refreshScenarioStatusAsync(boolean userInitiated) {
        if (!scenarioRefreshInProgress.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            if (ScenarioTable.tableScenariosLibrary != null) {
                ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(LOADING_SCENARIOS_MESSAGE));
            }
            ScenarioTable.clear();
        });
        Client.setStartupStatus(userInitiated ? "Refreshing scenario status..." : "Loading scenario status...", -1, !userInitiated);
        Thread refreshThread = new Thread(() -> {
            try {
                updateRunStatus();
                Platform.runLater(() -> {
                    try {
                        if (ScenarioTable.tableScenariosLibrary != null) {
                            ScenarioTable.tableScenariosLibrary.refresh();
                        }
                        Client.setStartupStatus(userInitiated ? "Scenario status refreshed." : READY_MESSAGE, -1, false);
                    } finally {
                        scenarioRefreshInProgress.set(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    System.out.println("Problem updating scenario table: " + ex);
                    Client.setStartupStatus("Problem loading scenario status.", -1, false);
                    scenarioRefreshInProgress.set(false);
                });
            }
        }, "scenario-status-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private static final class ScenarioStatusSnapshot {
        final String scenarioName;
        final String components;
        final String createdDate;
        final String completedDate;
        final String status;
        final String runtime;
        final String unsolved;

        ScenarioStatusSnapshot(String scenarioName, String components, String createdDate, String completedDate,
                String status, String runtime, String unsolved) {
            this.scenarioName = scenarioName;
            this.components = components;
            this.createdDate = createdDate;
            this.completedDate = completedDate;
            this.status = status;
            this.runtime = runtime;
            this.unsolved = unsolved;
        }
    }
}

