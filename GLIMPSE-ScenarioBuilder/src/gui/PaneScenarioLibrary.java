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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    private static boolean isTailedLogLine(String line) {
        return line != null && line.startsWith(TAILED_LOG_PREFIX);
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
                lastGcamStopResult = rp.stop();
                try {
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                            ConsoleManager.MessageKind.GLIMPSE_INFO,
                            "Stop signal sent (" + lastGcamStopResult.getSummary() + ")");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Best-effort: copy exe/logs/main_log.txt to the current scenario folder on termination.
        try {
            copyExecutableMainLogToScenarioFolder(currentGcamScenarioName);
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

    /**
     * Captures the last stop() attempt result so we can report a summary when the run actually ends.
     * (Stop is asynchronous from the point of view of the Future/get flow.)
     */
    private volatile ProcessRunner.StopResult lastGcamStopResult;

    /** Tracks the scenario name currently being executed by GCAM (best-effort). */
    private volatile String currentGcamScenarioName;

    private long startupTime = 0;
    private final HBox scenarioLibraryHBox = new HBox(1);

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

        ScenarioTable.tableScenariosLibrary.prefWidthProperty().bind(stage.widthProperty().multiply(1.0)); // Bind table width to stage
        ScenarioTable.tableScenariosLibrary.prefHeightProperty().bind(stage.heightProperty().multiply(0.7)); // Bind table height to stage
        scenarioLibraryHBox.getChildren().addAll(ScenarioTable.tableScenariosLibrary); // Add table to HBox
        if (startupTime == 0) startupTime = (new Date()).getTime(); // Record startup time
        System.out.println("time now=" + (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).format(startupTime));
        updateRunStatus(); // Initial update of scenario run status
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
            updateRunStatus();
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
            updateRunStatus();
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
        Client.buttonDeleteScenario.setAlignment(Pos.CENTER);

        // Initial button visibility
        Client.buttonRunScenario.setVisible(true);
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
            String workingDir = vars.getScenarioDir() + File.separator + scenName;
            String exeDir = vars.getgCamExecutableDir();
            String configFilename = workingDir + File.separator + "configuration_" + scenName + ".xml";
            String archiveConfigFilename = workingDir + File.separator + "configuration_" + scenName + "_archive.xml";
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
            String xmlDir = vars.getScenarioDir() + File.separator + scenName;
            String trashDirFolder = vars.getTrashDir() + File.separator + scenName;
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
                String configFilename = vars.getScenarioDir() + File.separator + scenName + File.separator + "configuration_" + scenName + ".xml";
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
            String xmlDir = vars.getScenarioDir() + File.separator + scenName;
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
            String workingScenarioLog = vars.getGlimpseLogDir() + File.separator + "Runs.txt";
            File workingScenariosFile = new File(workingScenarioLog);
            boolean doesScenarioExist = files.searchForTextAtStartOfLinesInFile(workingScenariosFile, scenarioName + ",", "#");
            String confirmMsg = doesScenarioExist ? "Overwrite existing scenario " + scenarioName + "?" : "Import " + scenarioName + " into GLIMPSE?";
            if (!utils.confirmAction(confirmMsg)) return;
            String newScenFolderName = vars.getScenarioDir() + File.separator + scenarioName;
            File newScenFolder = new File(newScenFolderName);
            newScenFolder.mkdir();
            String newScenFilename = newScenFolder + File.separator + "configuration_" + scenarioName + ".xml";
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
            String xmlFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "configuration_" + scenName + ".xml";
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
            String txtFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "main_log.txt";
            files.showFileInTextEditor(txtFile);
        }
    }

    /**
     * Opens the main log file in the executable logs directory in a text editor.
     * Uses the system's default text editor.
     */
    private void handleViewExeLog() {
        String filename = vars.getgCamExecutableDir() + File.separator + "logs" + File.separator + "main_log.txt";
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
        String file1 = vars.getScenarioDir() + File.separator + sName1 + File.separator + "configuration_" + sName1 + ".xml";
        String file2 = vars.getScenarioDir() + File.separator + sName2 + File.separator + "configuration_" + sName2 + ".xml";

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
        ArrayList<String> rtnArray = new ArrayList<>();
        rtnArray.add("Note: Includes only runs added to the queue since the start of this session.");
        if (!runsQueuedList.isEmpty()) {
            rtnArray.add("---");
            rtnArray.add("In queue:");
            rtnArray.addAll(runsQueuedList);
        }
        if (!runsCompletedList.isEmpty()) {
            rtnArray.add("---");
            rtnArray.add("Completed:");
            rtnArray.addAll(runsCompletedList);
        }
        return rtnArray;
    }

    /**
     * Returns a detailed report of the run queue, completed, and not completed runs.
     *
     * @param runQueue the run queue
     * @return ArrayList of report lines with completion status
     */
    protected ArrayList<String> createFancyQueueRpt(ArrayList<String> runQueue) {
        ArrayList<String> rtnArray = new ArrayList<>();
        rtnArray.add("Note: Includes only runs added to the queue since the start of this session.");
        ArrayList<String> completedArray = new ArrayList<>();
        completedArray.add("===");
        completedArray.add("Completed successfully:");
        ArrayList<String> issuesArray = new ArrayList<>();
        issuesArray.add("---");
        issuesArray.add("Not completed successfully (w/Issues):");
        ArrayList<String> notCompletedArray = new ArrayList<>();
        notCompletedArray.add("---");
        notCompletedArray.add("Running or still in queue:");
        ObservableList<ScenarioRow> allRuns = ScenarioTable.tableScenariosLibrary.getItems();
        for (ScenarioRow scenRow : allRuns) {
            String scenName = scenRow.getScenarioName();
            String searchText = File.separator + scenName + File.separator;
            String isComplete = scenRow.getStatus();
            String runDate = String.valueOf(scenRow.getCreatedDate());
            boolean match = false;
            for (String runInQueue : runQueue) {
                if (runInQueue.contains(searchText)) {
                    match = true;
                }
                if (match) {
                    if ((isComplete.equals("Success")) || (isComplete.equals("Unsolved mkts"))) {
                        completedArray.add(runInQueue);
                    } else if (isComplete.isEmpty()) {
                        if (runDate != null && !runDate.isEmpty()) {
                            notCompletedArray.add(runInQueue);
                        }
                    } else if (isComplete.equals("DNF")) {
                        issuesArray.add(runInQueue);
                    } else if (isComplete.equals("Running")) {
                        runInQueue += " (Running)";
                        notCompletedArray.add(runInQueue);
                    }
                    break;
                }
            }
        }
        rtnArray.addAll(completedArray);
        rtnArray.addAll(issuesArray);
        rtnArray.addAll(notCompletedArray);
        return rtnArray;
    }

    /**
     * Removes a scenario from the scenario library by name.
     *
     * @param nameToDelete the scenario name to remove
     */
    void deleteItemFromScenarioLibrary(String nameToDelete) {
        ObservableList<ScenarioRow> allScenariosList = ScenarioTable.tableScenariosLibrary.getItems();
        ObservableList<ScenarioRow> deleteScenariosList = FXCollections.observableArrayList();
        for (ScenarioRow mfr : allScenariosList) {
            if (mfr.getScenarioName().equals(nameToDelete)) {
                deleteScenariosList.add(mfr);
            }
        }
        ScenarioTable.removeFromListOfRunFiles(deleteScenariosList);
    }

    
    
    /**
     * Updates the run status for all scenarios and refreshes the table.
     * Reads log files and updates scenario status, runtime, and unsolved markets.
     * Also updates the UI with computer stats and logs status changes.
     */
    public void updateRunStatus() {
        String currentMainLogName = vars.getgCamExecutableDir() + File.separator + "logs" + File.separator + "main_log.txt";
        File currentMainLogFile = new File(currentMainLogName);
        String runningScenario = utils.getRunningScenario(currentMainLogFile);
        ScenarioTable.tableScenariosLibrary.refresh();
        String address = vars.getGlimpseLogDir() + File.separator + "Runs.txt";
        DateFormat format = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);
        ArrayList<String> searchArray = new ArrayList<>();
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
            utils.sb.setText(computerStats);
            if (computerStats.endsWith("!")) {
                utils.sb.setStyle("-fx-text-fill: red");
            } else {
                utils.sb.setStyle("-fx-text-fill: black");
            }
        });
        try {
            File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
            if (scenarioFolders == null) return;
            for (File scenarioFolder : scenarioFolders) {
                searchArray.clear();
                searchArray.add("Model run completed.");
                searchArray.add("Data Readin, Model Run & Write Time:");
                searchArray.add("The following model periods did not solve:");
                Long createdDate = 0L;
                Long completedDate = 0L;
                String scenarioName = scenarioFolder.getName();
                String configName = scenarioFolder + File.separator + "configuration_" + scenarioName + ".xml";
                File configFile = new File(configName);
                if (!configFile.exists()) continue;
                String components = getComponentsFromConfig(configFile); // Get scenario components from config
                String mainLogName = scenarioFolder + File.separator + "main_log.txt";
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
                        // Only consider "Lost handle" for a truly running scenario, and avoid transient
                        // misclassification immediately after startup / when runs are merely queued.
                        boolean isQueued = runsQueuedList.contains(scenarioName);
                        long graceMs = 30_000L; // allow some time for main_log.txt to begin updating
                        if (!isQueued && (startupTime > 0) && (System.currentTimeMillis() - startupTime > graceMs)
                                && lastDate < startupTime) {
                            status = "Lost handle";
                        } else {
                            String runningStatus = utils.getScenarioStatusFromMainLog(currentMainLogFile);
                            if (runningStatus.contains(",ERR")) {
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
                boolean match = false;
                for (ScenarioRow s : ScenarioTable.listOfScenarioRuns) {
                    if (s.getScenarioName().equals(scenarioName)) {
                        match = true;
                        if (!s.getStatus().equals("In queue") || !status.isEmpty()) {
                            s.setStatus(status);
                        }
                        s.setCreatedDate(createdDateStr);
                        s.setCompletedDate(completedDateStr);
                        s.setComponents(components);
                        s.setRuntime(runtime);
                        s.setUnsolvedMarkets(unsolved);
                    }
                }
                if (!match) {
                    ScenarioRow sr = new ScenarioRow(scenarioName);
                    sr.setComponents(components);
                    sr.setCreatedDate(createdDateStr);
                    sr.setCompletedDate(completedDateStr);
                    if (!"In queue".equals(sr.getStatus()) || !status.isEmpty()) {
                        sr.setStatus(status);
                    }
                    sr.setRuntime(runtime);
                    sr.setUnsolvedMarkets(unsolved);
                    ScenarioTable.listOfScenarioRuns.add(sr);
                }
            }
            ScenarioTable.tableScenariosLibrary.refresh();
        } catch (Exception ex) {
            System.out.println("Problem updating scenario table: " + ex);
        }
    }

    /**
     * Removes scenarios from the in-memory queue and clears their "In queue" status in the UI.
     * This affects only the queue for this session.
     */
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
            // Remove any matching queue entries.
            while (runsQueuedList.remove(scenName)) {
                // keep removing duplicates
            }
            // Clear UI status if it was showing queued.
            if ("In queue".equals(row.getStatus())) {
                row.setStatus("");
            }
        }
        ScenarioTable.tableScenariosLibrary.refresh();
    }

    /**
     * Reads scenario components from a configuration file.
     *
     * @param file the configuration file
     * @return the components string, or a default if not found
     */
    private String getComponentsFromConfig(File file) {
        String rtnStr = "";
        try (Scanner fileScanner = new Scanner(file)) {
            boolean startRecording = false;
            boolean stopRecording = false;
            boolean hasMetaData = false;
            int count = 0;
            while (fileScanner.hasNext() && !stopRecording) {
                String line = fileScanner.nextLine().trim();
                if (line.equals("##################### Scenario Meta Data #####################"))
                    hasMetaData = true;
                if (line.equals("###############################################################"))
                    stopRecording = true;
                if (startRecording && (line.length() > 0) && !stopRecording) {
                    if (count == 0) {
                        count++;
                        rtnStr += line;
                    } else {
                        rtnStr += " ; " + line;
                    }
                }
                if (line.equals("Components:"))
                    startRecording = true;
                if (line.equals("<Files>"))
                    stopRecording = true;
            }
            if (!hasMetaData) {
                rtnStr = "Externally-created scenario";
            }
        } catch (Exception e) {
            System.out.println("Problem reading components from " + file.getName() + ": " + e);
        }
        return rtnStr;
    }

    /**
     * Runs GCAM for the selected scenarios. Handles user confirmation and archive logic.
     * Uses enhanced for-loop for iterating over selected scenarios.
     *
     * @throws IOException if file operations fail
     */
    private void runGcamOnSelected() throws IOException {
        ObservableList<ScenarioRow> selectedScenarioRows = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        String[] configFiles = new String[selectedScenarioRows.size()];
        int idx = 0;
        for (ScenarioRow mfr : selectedScenarioRows) {
            mfr.setCreatedDate(new Date());
            String scenName = mfr.getScenarioName();
            String mainLogFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "main_log.txt";
            boolean b = true;
            if (files.doesFileExist(mainLogFile)) {
                String s = "main_log.txt exists for " + scenName + ". Run anyway?";
                b = utils.selectYesOrNoDialog(s);
            }
            if (b) {
                // Clear UI status immediately upon queueing so stale results from previous runs disappear right away.
                clearScenarioRunStatusFields(scenName);

                // Remove logs from a previous run so the Scenario Library won't show stale status while queued/running.
                files.deleteFile(mainLogFile);
                try {
                    files.deleteFile(vars.getScenarioDir() + File.separator + scenName + File.separator + "gcam_stdout.txt");
                    files.deleteFile(vars.getScenarioDir() + File.separator + scenName + File.separator + "main_error.txt");
                } catch (Exception ignored) {}

                configFiles[idx] = vars.getScenarioDir() + File.separator + scenName + File.separator + "configuration" + "_" + scenName + ".xml";
                mfr.setStatus("In queue");
            } else {
                configFiles[idx] = null;
            }
            try {
                String archiveConfigFilename = configFiles[idx] != null ? configFiles[idx].replace(".xml", "_archive.xml") : null;
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

    /**
     * Runs GCAM for the provided scenario configuration files. Handles cleaning, execution, and moving results.
     * Uses background threads for file operations and process execution.
     *
     * @param scenarioConfigFiles Array of scenario configuration file paths
     * @throws IOException if file operations fail
     */
    private void runGcamModel(String[] scenarioConfigFiles) throws IOException {
        System.out.println("Running scenarios in GCAM...");
        for (String scenarioConfigFile : scenarioConfigFiles) {
            if (scenarioConfigFile == null) {
                continue;
            }

            final String dir = scenarioConfigFile.substring(0, scenarioConfigFile.lastIndexOf(File.separator)).replaceAll("/", File.separator);
            System.out.println("config: " + scenarioConfigFile);

            // Track queue entries by scenario name (not config path) so dequeue/status logic stays consistent.
            final String queuedScenarioName = new File(dir).getName();
            this.runsQueuedList.add(queuedScenarioName);

            // 1) Clean out prior outputs from the scenario folder (async).
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

            // 2) Run GCAM (capture a handle so we can stop it).
            Future<ProcessResult> gcamFuture = Client.gCAMExecutionThread.submitCallable(ExecutionThread.namedCallable(
                    "GCAM run: scenario=" + queuedScenarioName + ", config=" + new File(scenarioConfigFile).getName(),
                     new Callable<ProcessResult>() {
                @Override
                public ProcessResult call() throws Exception {
                    // Reset stop-result for this run.
                    lastGcamStopResult = null;

                    boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
                    System.out.println("OS detected as Windows: " + isWindows);

                    ArrayList<String> cmd = new ArrayList<>();
                    String exeDir = vars.getgCamExecutableDir();
                    String exeName = vars.getgCamExecutable();

                    // NOTE: On Windows we previously launched GCAM via `cmd.exe /c .\\<exe>`.
                    // That makes the Java Process handle refer to cmd.exe, not GCAM itself, so stop()
                    // may not terminate the actual model immediately. Launch GCAM directly instead.
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

                    System.out.println("Command to run (direct): " + cmd);

                    // Auto-clear the GCAM console output at the start of each new run (best-effort).
                    ConsoleManager.clear(ConsoleManager.StreamSource.GCAM_STDOUT);

                    ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "Starting GCAM");
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "cmd: " + cmd);
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "exe: " + new File(exePath).getAbsolutePath());
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT, "working dir: " + new File(exeDir).getAbsolutePath());

                    // Optional: tail GCAM's exeDir/logs/main_log.txt into the console.
                    // This helps when GCAM stdout is pipe-buffered (common on Windows).
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
                                        // Avoid duplicating what GCAM already emits on stdout if it's logging the same content.
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
                            // Best-effort.
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
                                    // If for some reason the tailed prefix shows up on stdout, drop it to avoid duplicated-looking output.
                                    if (isTailedLogLine(line)) {
                                        return;
                                    }
                                    ConsoleManager.appendLineBuffered(ConsoleManager.StreamSource.GCAM_STDOUT,
                                            ConsoleManager.MessageKind.MODEL_STDOUT, line);
                                },
                                line -> ConsoleManager.appendLineBuffered(ConsoleManager.StreamSource.GCAM_STDOUT,
                                        ConsoleManager.MessageKind.STDERR, line)
                        );

                        currentGcamRun = rp;
                        currentGcamScenarioName = queuedScenarioName;

                        // When the process handle has been acquired, the run has truly started.
                        // Clear any stale status fields from a previous run *now* (not at queue time).
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
                        // Stop tailer first so we don't keep emitting after the process ends.
                        try {
                            if (tailHandle != null && !tailHandle.isStopRequested()) {
                                tailHandle.stop();
                                tailHandle.join(java.time.Duration.ofSeconds(2));
                            }
                        } catch (Exception ignored) {}

                        // Force a final flush so the last burst is visible immediately at completion.
                        try {
                            ConsoleManager.flushBuffered();
                        } catch (Exception ignored) {}
                         // Clear handle when finished.
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

            // Remember the future so stop can cancel the wait.
            currentGcamFuture = gcamFuture;

            // 3) After GCAM completes, write logs and move requested outputs based on exit code.
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
                        // Clear the future if it’s the current one.
                        if (currentGcamFuture == gcamFuture) {
                            currentGcamFuture = null;
                        }
                    }

                    // Report termination status (if a stop was requested).
                    try {
                        ProcessRunner.StopResult sr = lastGcamStopResult;
                        if (sr != null) {
                            ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "Stop result");
                            ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                    ConsoleManager.MessageKind.GLIMPSE_INFO,
                                    sr.getSummary());
                        }
                        // Always report final exit code so user can tell if it stopped or failed naturally.
                        ConsoleManager.appendHeader(ConsoleManager.StreamSource.GCAM_STDOUT, "GCAM finished");
                        ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                                ConsoleManager.MessageKind.GLIMPSE_INFO,
                                "exitCode=" + result.getExitCode() + ", cancelled=" + wasCancelled + ", success=" + result.isSuccess());
                    } catch (Exception ignored) {}

                    String scenName = new File(dir).getName();

                    // Always capture the executable's main_log.txt (exeDir/logs/main_log.txt) into the scenario folder.
                    // This is the authoritative GCAM log and is what the Scenario Library expects as main_log.txt.
                    copyExecutableMainLogToScenarioFolder(scenName);

                    // Save captured process stdout/stderr to separate files for debugging.
                    String gcamStdoutFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "gcam_stdout.txt";
                    String mainErrFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "main_error.txt";

                    try {
                        // Do NOT overwrite scenario/main_log.txt here; it should contain the copied exe/logs/main_log.txt.
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

                    System.out.println("GCAM finished with exitCode=" + result.getExitCode());

                    if (wasCancelled) {
                        System.out.println("GCAM run canceled by user; skipping move of output files.");
                        updateRunStatus();
                        return "GCAM run canceled";
                    }

                    if (!result.isSuccess()) {
                        System.out.println("GCAM run failed (or timed out); skipping move of output files.");
                        updateRunStatus();
                        return "GCAM run failed";
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

    /**
     * Runs the ModelInterface Java application with the current output database and optional arguments.
     * Handles both Windows and Unix-like systems. Launches ModelInterface in a background thread.
     *
     * @throws IOException if process execution fails
     */
    private void runModelInterface() throws IOException {
    	String database = vars.getgCamOutputDatabase();
    	runModelInterfaceWhich(database);
    }

    /**
     * Runs the ModelInterface Java application for a specific database.
     * Handles both Windows and Unix-like systems. Launches ModelInterface in a background thread.
     *
     * @param database_name Path to the database file
     * @throws IOException if process execution fails
     */
    private void runModelInterfaceWhich(String database_name) throws IOException {
        final String modelInterfaceDirStr = vars.getModelInterfaceDir();
        final File modelInterfaceDir = (modelInterfaceDirStr == null) ? null : new File(modelInterfaceDirStr);
        final String jarName = vars.getModelInterfaceJar();

        // Preflight checks (fail fast with actionable messages)
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
                problems.add("Database not found: " + db.getAbsolutePath());
            }
        }

        // Optional files (only validate if provided)
        validateOptionalFile(problems, "Query file", vars.getQueryFilename());
        validateOptionalFile(problems, "Units conversion file", vars.getUnitConversionsFilename());
        validateOptionalFile(problems, "Preset region list file", vars.getPresetRegionListFilename());
        validateOptionalFile(problems, "Favorite queries file", vars.getFavoriteQueryFilename());

        File mapsDir = null;
        if (modelInterfaceDir != null) {
            mapsDir = new File(modelInterfaceDir, "map_resources");
            if (!mapsDir.isDirectory()) {
                problems.add("Map resources directory not found: " + mapsDir.getAbsolutePath());
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to start ModelInterface. Please fix:\n\n");
            for (String p : problems) {
                sb.append(" - ").append(p).append("\n");
            }
            utils.warningMessage(sb.toString());
            System.out.println(sb.toString());
            return;
        }

        // Build args list to avoid OS-specific quoting issues.
        final ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(jarFile.getAbsolutePath());
        args.add("-o");
        args.add(database_name);

        appendArgIfPresent(args, "-q", vars.getQueryFilename());
        appendArgIfPresent(args, "-u", vars.getUnitConversionsFilename());
        appendArgIfPresent(args, "-p", vars.getPresetRegionListFilename());
        appendArgIfPresent(args, "-f", vars.getFavoriteQueryFilename());
        // Always pass maps folder if present and valid.
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

    private void validateOptionalFile(ArrayList<String> problems, String label, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }
        File f = new File(filename);
        if (!f.exists()) {
            problems.add(label + " not found: " + f.getAbsolutePath());
        }
    }

    private void appendArgIfPresent(ArrayList<String> args, String flag, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        args.add(flag);
        args.add(trimmed);
    }

    /**
     * Archives scenario files by copying them to an archive folder and zipping the result.
     * Prompts user if archive already exists. Updates configuration file paths to point to archived files.
     *
     * @param exeDir Path to the GCAM executable directory
     * @param workingDir Path to the scenario working directory
     * @param archiveConfigFilename Path to the archive configuration file
     * @param configFilename Path to the scenario configuration file
     * @param scenName Scenario name
     */
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

    /**
     * Generates and displays an error report for the selected scenarios using the executable log.
     * Aggregates error lines and displays them in a popup window.
     */
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
        if (report.size() == 0) {
            report.add("No errors reported.");
        }
        utils.displayArrayList(report, "Error Report", false);
    }

    /**
     * Generates and displays an error report for the selected scenarios using the scenario log.
     * Aggregates error lines and displays them in a popup window.
     */
    private void generateErrorReport() {
        ArrayList<String> report = new ArrayList<String>();
        ObservableList<ScenarioRow> selectedScenarioRows = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        try {
            for (ScenarioRow row : selectedScenarioRows) {
                String scenarioName = "" + row.getScenName();
                String scenarioMainLog = vars.getScenarioDir() + File.separator + scenarioName + File.separator + "main_log.txt";
                File mainlogfile = new File(scenarioMainLog);
                if (mainlogfile.exists()) {
                    ArrayList error_lines = utils.generateErrorReport(scenarioMainLog, scenarioName);
                    report.addAll(error_lines);
                }
            }
        } catch (Exception e) {
            System.out.println("error developing error log:" + e);
        }
        if (report.size() == 0) {
            report.add("No errors reported.");
        }
        utils.displayArrayList(report, "Error Report", false);
    }

    /**
     * Generates and displays a run report for all scenarios, including warnings, errors, and timing information.
     * Saves the report as a CSV file and displays it in a popup table.
     */
    private void generateRunReport() {
        ArrayList<String> report = new ArrayList<String>();
        String scenario_name = null;
        String when_created = null;
        String when_run = null;
        String model_version = null;
        String config_file = null;
        String config_path = null;
        int num_warnings = 0;
        int num_errors = 0;
        String not_solved = null;
        boolean is_completed = false;
        String solution_time = null;
        String total_time = null;
        String components = "";
        ArrayList<String> error_lines = null;
        File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
        ArrayList<File> mainLogFiles = new ArrayList<File>();
        for (File scenarioFolder : scenarioFolders) {
            String mainLogFilename = scenarioFolder.getPath() + File.separator + "main_log.txt";
            File logFile = new File(mainLogFilename);
            if (logFile.exists()) {
                mainLogFiles.add(logFile);
            }
        }
        String str = "scenario,created,run,version,#warn,#err,unsolved,errors,completed?,solution(sec),total(sec),components";
        report.add(str);
        for (File main_log : mainLogFiles) {
            String folder_name = main_log.getParent();
            String scenario_pathname = main_log.getParent();
            scenario_name = scenario_pathname.substring(scenario_pathname.lastIndexOf(File.separator) + 1);
            config_file = files.searchForTextInFileS(main_log, "Configuration file:", "#").replace("Configuration file:", "").trim();
            String temp = config_file;
            when_created = files.getLastModifiedInfoForFile(temp);
            when_run = files.getLastModifiedInfoForFile(main_log.toString());
            model_version = files.searchForTextInFileS(main_log, "Running GCAM model", "#").replace("Running GCAM model", "").trim();
            num_warnings = files.countLinesWithTextInFile(main_log, "Warning", "#");
            num_errors = files.countLinesWithTextInFile(main_log, "ERROR", "#");
            not_solved = files.searchForTextInFileS(main_log, "The following model periods did not solve:", "#").replace("The following model periods did not solve:", "").trim().replace(",", ";");
            is_completed = files.searchForTextInFile(main_log, "Model run completed.", "#");
            solution_time = files.searchForTextInFileS(main_log, "Full Scenario", "#").replace("Full Scenario", "").replace(" seconds.", "").trim();
            total_time = files.searchForTextInFileS(main_log, "Data Readin, Model Run & Write Time:", "#").replace("Data Readin, Model Run & Write Time:", "").replace(" seconds.", "").trim();
            components = getComponentsFromTable(scenario_name);
            error_lines = files.getStringArrayWithPrefix(main_log.getPath(), "ERROR");
            String error_rpt = utils.processErrors(error_lines, 0.01);
            String s = ",";
            str = scenario_name + s + when_created + s + when_run + s + model_version + s + num_warnings + s
                    + num_errors + s + not_solved + s + error_rpt + s + is_completed + s + solution_time + s
                    + total_time + s + components;
            report.add(str);
            if (not_solved.trim() != "")
                System.out.println(str);
        }
        String report_file = vars.getGlimpseLogDir() + File.separator + "scenario_report.csv";
        files.saveFile(report, report_file);
        utils.showPopupTableOfCSVData("Scenario Run Report", report, 910, 600);
    }

    /**
     * Retrieves the scenario components string from the scenario table for a given scenario name.
     *
     * @param scenName the scenario name
     * @return the components string
     */
    private String getComponentsFromTable(String scenName) {
        String str = "";
        for (ScenarioRow sr : ScenarioTable.listOfScenarioRuns) {
            String sname = sr.getScenarioName();
            if (sname.equals(scenName)) {
                str = sr.getComponents();
            }
        }
        return str;
    }

    /**
     * Clears the contents of ScenarioTable and updates the run status.
     */
    public void clearAndRefreshScenarioTable() {
        ScenarioTable.listOfScenarioRuns.clear();
        ScenarioTable.tableScenariosLibrary.getItems().clear();
        updateRunStatus();
    }

    /**
     * Copies the current GCAM executable main_log.txt (exeDir/logs/main_log.txt) into the scenario folder.
     * This is best-effort and should never throw.
     */
    private void copyExecutableMainLogToScenarioFolder(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        try {
            Path src = Paths.get(vars.getgCamExecutableDir(), "logs", "main_log.txt");
            if (!Files.exists(src)) {
                return;
            }
            Path destDir = Paths.get(vars.getScenarioDir(), scenarioName);
            if (!Files.exists(destDir)) {
                return;
            }
            Path dest = destDir.resolve("main_log.txt");
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.out.println("Problem copying exe main_log.txt to scenario folder for '" + scenarioName + "': " + e);
        }
    }

    /**
     * Clears UI status fields for a scenario run row.
     *
     * This is used when a scenario actually starts running so stale results from a
     * previous run (Success/DNF/Unsolved, runtime, etc.) don't linger while the new
     * run is starting up.
     */
    private void clearScenarioRunStatusFields(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            try {
                for (ScenarioRow s : ScenarioTable.listOfScenarioRuns) {
                    if (s != null && scenarioName.equals(s.getScenarioName())) {
                        s.setStatus("");
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
}
