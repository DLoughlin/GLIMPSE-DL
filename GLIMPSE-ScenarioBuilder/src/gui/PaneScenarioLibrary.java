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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseUtil.FileChooserPlus;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.ProcessResult;
import glimpseUtil.ProcessRunner;
import glimpseUtil.WindowsRuntimePreflight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

/** Manages the scenario library pane, including scenario actions, GCAM runs, and status refresh. */
public class PaneScenarioLibrary extends ScenarioBuilder {
    private static final Duration LIVE_STATUS_REFRESH_INTERVAL = Duration.ofSeconds(5);
    private static final String STOPPED_LOG_MARKER = "GLIMPSE scenario status: Stopped";
    private static final String LIVE_STDOUT_ERROR_PREFIX = "ERROR";
    private static final java.util.regex.Pattern LIVE_UNSOLVED_PERIOD_ERROR_PATTERN = java.util.regex.Pattern.compile(
            "did\\s+not\\s+solve\\s+periods?\\s*[:=]?\\s*([0-9]{1,3}(?:\\s*(?:,|and|&)\\s*[0-9]{1,3})*)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern LIVE_UNSOLVED_PERIOD_NUMBER_PATTERN = java.util.regex.Pattern.compile("\\d{1,3}");
    private static final String[] EXE_LOG_ARTIFACT_FILENAMES = {
            "main_log.txt",
            "main_error.txt",
            "gcam_stdout.txt"
    };

    private static final String[] GCAM_STDOUT_SUCCESS_MARKERS = {
            "Model exiting successfully.",
            "Exiting successfully.",
            "Model run completed.",
            "Finished printing output."
    };

    private static final String LOADING_SCENARIOS_MESSAGE = "Loading scenario status...";
    private static final String NO_SCENARIOS_MESSAGE = "No scenarios found.";
    private static final String READY_MESSAGE = "Ready";
    private static final String ERROR_LOADING_SCENARIOS_MESSAGE = "Problem loading scenario status.";
    private static final String LIVE_RUNTIME_PREFIX = "> ";
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
 
    // --- Selection state ---
    private static final class ScenarioLibrarySelectionState {
        final int selectedCount;
        final boolean hasSelection;
        final boolean hasSingleSelection;
        final boolean hasTwoSelections;
        final boolean hasActiveRun;

        ScenarioLibrarySelectionState(int selectedCount, boolean hasActiveRun) {
            this.selectedCount = Math.max(0, selectedCount);
            this.hasSelection = this.selectedCount > 0;
            this.hasSingleSelection = this.selectedCount == 1;
            this.hasTwoSelections = this.selectedCount == 2;
            this.hasActiveRun = hasActiveRun;
        }

        static ScenarioLibrarySelectionState capture() {
            int selectionCount = 0;
            try {
                if (ScenarioTable.tableScenariosLibrary != null
                        && ScenarioTable.tableScenariosLibrary.getSelectionModel() != null
                        && ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems() != null) {
                    selectionCount = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems().size();
                }
            } catch (Exception ignored) {}
            boolean activeRun = false;
            try {
                activeRun = Client.paneScenarioLibrary != null
                        && Client.paneScenarioLibrary.getRunController().hasActiveRun();
            } catch (Exception ignored) {}
            return new ScenarioLibrarySelectionState(selectionCount, activeRun);
        }
    }

    // --- Dependencies and state ---
    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final ScenarioFileActionService scenarioFileActionService = new ScenarioFileActionService(vars, files, utils);
    private final ScenarioStatusService scenarioStatusService = new ScenarioStatusService(vars, files, utils);
    private final ScenarioLibraryRunPreparationHelper runPreparationHelper = new ScenarioLibraryRunPreparationHelper(vars, files, utils);
    private final GcamRunController runController = new GcamRunController();
    private final GcamPromptMonitor gcamPromptMonitor = new GcamPromptMonitor(runController, files, this::maybePromptForDatabaseRelease);
    private final ScenarioLibraryViewStateHelper viewStateHelper = new ScenarioLibraryViewStateHelper(utils);

    private Timeline liveStatusRefreshTimeline;
    private final AtomicBoolean liveStatusRefreshInProgress = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, LinkedHashSet<String>> liveStdoutErrorPeriodsByScenario = new ConcurrentHashMap<>();
    private long startupTime = 0;
    private final HBox scenarioLibraryHBox = new HBox(1);
    private final AtomicBoolean scenarioRefreshInProgress = new AtomicBoolean(false);
    private ScenarioLibraryViewStateHelper.RefreshViewState pendingRefreshViewState = ScenarioLibraryViewStateHelper.RefreshViewState.empty();
    private final ConcurrentHashMap<String, Boolean> liveSuccessMarkedByScenario = new ConcurrentHashMap<>();

    // --- Constructors ---
    PaneScenarioLibrary(Stage stage) {
        scenarioLibraryHBox.setSpacing(10);
        wireScenarioSelectionButtonRefresh();
        createScenarioLibraryButtons();
        ensureLiveStatusRefreshTimeline();

        ScenarioTable.tableScenariosLibrary.setMaxWidth(Double.MAX_VALUE);
        ScenarioTable.tableScenariosLibrary.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(ScenarioTable.tableScenariosLibrary, javafx.scene.layout.Priority.ALWAYS);
        scenarioLibraryHBox.setFillHeight(true);
        scenarioLibraryHBox.getChildren().addAll(ScenarioTable.tableScenariosLibrary);
        if (ScenarioTable.tableScenariosLibrary != null) {
            ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(LOADING_SCENARIOS_MESSAGE));
        }
        refreshScenarioActionButtons();
        if (startupTime == 0) {
            startupTime = (new Date()).getTime();
        }
        System.out.println("time now=" + (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).format(startupTime));
    }

    PaneScenarioLibrary() {}

    // --- UI setup ---
    private void createScenarioLibraryButtons() {
        createScenarioLibraryButtonInstances();
        configureInitialScenarioLibraryButtonState();
        bindScenarioLibraryButtonHandlers();
        configureScenarioLibraryButtonLayout();
        refreshScenarioActionButtons();
    }

    private void createScenarioLibraryButtonInstances() {
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
    }

    private void configureInitialScenarioLibraryButtonState() {
        ScenarioLibrarySelectionState selectionState = ScenarioLibrarySelectionState.capture();
        Client.buttonRunScenario.setDisable(!selectionState.hasSelection);
        Client.buttonStopScenario.setDisable(!selectionState.hasActiveRun);
        Client.buttonBrowseScenarioFolder.setDisable(!selectionState.hasSelection);
        Client.buttonImportScenario.setDisable(false);
        Client.buttonArchiveScenario.setDisable(!selectionState.hasSelection);
        Client.buttonDeleteScenario.setDisable(!selectionState.hasSelection);
        Client.buttonResultsForSelected.setDisable(!selectionState.hasSingleSelection);
        Client.buttonViewConfig.setDisable(!selectionState.hasSelection);
        Client.buttonDiffFiles.setDisable(!selectionState.hasTwoSelections);
        Client.buttonViewLog.setDisable(!selectionState.hasSingleSelection);
        Client.buttonViewExeErrors.setDisable(false);
        Client.buttonViewErrors.setDisable(!selectionState.hasSingleSelection);
        Client.buttonViewExeLog.setDisable(false);
        Client.buttonReport.setDisable(false);
    }

    private void bindScenarioLibraryButtonHandlers() {
        Client.buttonRefreshScenarioStatus.setOnAction(e -> {
            refreshScenarioStatusAsync(true);
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
    }

    private void configureScenarioLibraryButtonLayout() {
        Client.buttonResults.setAlignment(Pos.CENTER);
        Client.buttonResultsForSelected.setAlignment(Pos.CENTER);
        Client.buttonRunScenario.setAlignment(Pos.CENTER);
        Client.buttonStopScenario.setAlignment(Pos.CENTER);
        Client.buttonDeleteScenario.setAlignment(Pos.CENTER);

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

    private void wireScenarioSelectionButtonRefresh() {
        if (ScenarioTable.tableScenariosLibrary == null || ScenarioTable.tableScenariosLibrary.getSelectionModel() == null) {
            return;
        }
        ScenarioTable.tableScenariosLibrary.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                refreshScenarioActionButtons());
        ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<ScenarioRow>) change -> refreshScenarioActionButtons());
    }

    private void refreshScenarioActionButtons() {
        if (Platform.isFxApplicationThread()) {
            applyScenarioActionButtonState();
            return;
        }
        Platform.runLater(this::applyScenarioActionButtonState);
    }

    private void applyScenarioActionButtonState() {
        try {
            setArrowAndButtonStatus();
            applyScenarioLibrarySelectionState(ScenarioLibrarySelectionState.capture());
        } catch (Exception ignored) {}
    }

    private void applyScenarioLibrarySelectionState(ScenarioLibrarySelectionState selectionState) {
        if (selectionState == null) {
            return;
        }
        try {
            if (Client.buttonRunScenario != null) {
                Client.buttonRunScenario.setDisable(!selectionState.hasSelection);
            }
            if (Client.buttonBrowseScenarioFolder != null) {
                Client.buttonBrowseScenarioFolder.setDisable(!selectionState.hasSelection);
            }
            if (Client.buttonArchiveScenario != null) {
                Client.buttonArchiveScenario.setDisable(!selectionState.hasSelection);
            }
            if (Client.buttonDeleteScenario != null) {
                Client.buttonDeleteScenario.setDisable(!selectionState.hasSelection);
            }
            if (Client.buttonResultsForSelected != null) {
                Client.buttonResultsForSelected.setDisable(!selectionState.hasSingleSelection);
            }
            if (Client.buttonDiffFiles != null) {
                Client.buttonDiffFiles.setDisable(!selectionState.hasTwoSelections);
            }
            if (Client.buttonViewLog != null) {
                Client.buttonViewLog.setDisable(!selectionState.hasSingleSelection);
            }
            if (Client.buttonViewErrors != null) {
                Client.buttonViewErrors.setDisable(!selectionState.hasSingleSelection);
            }
            if (Client.buttonStopScenario != null) {
                Client.buttonStopScenario.setDisable(!selectionState.hasActiveRun);
            }
            if (Client.buttonImportScenario != null) {
                Client.buttonImportScenario.setDisable(false);
            }
            if (Client.buttonViewConfig != null) {
                Client.buttonViewConfig.setDisable(!selectionState.hasSelection);
            }
            if (Client.buttonViewExeErrors != null) {
                Client.buttonViewExeErrors.setDisable(false);
            }
            if (Client.buttonViewExeLog != null) {
                Client.buttonViewExeLog.setDisable(false);
            }
            if (Client.buttonReport != null) {
                Client.buttonReport.setDisable(false);
            }
        } catch (Exception ignored) {}
    }

    // --- UI actions ---
    private void handleArchiveScenario() {
        if (!utils.confirmArchiveScenario()) {
            return;
        }
        scenarioFileActionService.archiveScenarios(ScenarioSelection.capture());
    }

    private void handleDeleteScenario() {
        if (!utils.confirmDelete()) {
            return;
        }
        ScenarioSelection selection = ScenarioSelection.capture();
        dequeueScenariosAndClearStatus(FXCollections.observableArrayList(selection.getRows()));
        clearDeletedScenarioRunState(selection);
        try {
            List<ScenarioRow> deletedRows = scenarioFileActionService.deleteScenarios(selection);
            ScenarioTable.removeFromListOfRunFiles(FXCollections.observableArrayList(deletedRows));
        } catch (Exception e) {
            utils.warningMessage("Problem deleting scenario(s)");
            System.out.println("error: " + e);
            utils.exitOnException();
        }
    }

    private void handleResults() {
        if (!hasModelInterfaceLocationConfigured()) {
            utils.warningMessage("Please specify modelInterfaceDir in options file.");
            return;
        }
        try {
            runModelInterface();
        } catch (Exception e) {
            e.printStackTrace();
            utils.exitOnException();
        }
    }

    private void handleResultsForSelected() {
        if (!hasModelInterfaceLocationConfigured()) {
            utils.warningMessage("Please specify modelInterfaceDir in options file.");
            return;
        }
        ObservableList<ScenarioRow> selectedFiles = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
        if (selectedFiles.size() != 1) {
            return;
        }
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

    private void handleBrowseScenarioFolder() {
        scenarioFileActionService.openScenarioFolders(ScenarioSelection.capture());
    }

    private void handleImportScenario() {
        File newConfigFile = FileChooserPlus.showOpenDialog(null, "Select scenario configuration file", new File(vars.getgCamExecutableDir()), FileChooserPlus.createExtensionFilter(XML_FILE_FILTER_LABEL, XML_FILE_FILTER_EXT));
        if (newConfigFile == null) {
            return;
        }
        String str = files.searchForTextInFileS(newConfigFile, "scenarioName", "<!--");
        String scenarioName = utils.getStringBetweenCharSequences(str, ">", "</");
        String workingScenarioLog = ScenarioLibraryPathHelper.glimpseRunsFile(vars.getGlimpseLogDir());
        File workingScenariosFile = new File(workingScenarioLog);
        boolean doesScenarioExist = files.searchForTextAtStartOfLinesInFile(workingScenariosFile, scenarioName + ",", "#");
        String confirmMsg = doesScenarioExist ? "Overwrite existing scenario " + scenarioName + "?" : "Import " + scenarioName + " into GLIMPSE?";
        if (!utils.confirmAction(confirmMsg)) {
            return;
        }

        Client.beginScenarioOperationProgress();
        try {
            ScenarioFileActionService.ImportResult importResult = scenarioFileActionService.importScenarioConfig(newConfigFile);
            if (!importResult.wasImported()) {
                return;
            }
            if (doesScenarioExist) {
                clearImportedScenarioRunResultFields(scenarioName);
            }
            ScenarioRow[] newRun = { importResult.getScenarioRow() };
            ScenarioTable.addToListOfRunFiles(newRun);
        } finally {
            Client.endScenarioOperationProgress();
        }
    }

    private void handleViewConfig() {
        scenarioFileActionService.openScenarioConfigs(ScenarioSelection.capture());
    }

    private void handleViewLog() {
        scenarioFileActionService.openScenarioLogs(ScenarioSelection.capture());
    }

    private void handleViewExeLog() {
        scenarioFileActionService.openExeLog();
    }

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
            DiffWindow.show(Client.primaryStage, file1, file2, rows);
        } catch (Exception e) {
            utils.warningMessage("Problem generating diff: " + e.getMessage());
        }
    }

    private void handleShowRunQueue() {
        List<String> queuedRuns = runController.getQueuedRuns();
        List<String> completedRuns = runController.getCompletedRuns();
        String runningScenarioName = runController.getCurrentScenarioName();
        if (queuedRuns.isEmpty() && completedRuns.isEmpty()
                && (runningScenarioName == null || runningScenarioName.trim().isEmpty())) {
            utils.warningMessage("No queued runs this session.");
            return;
        }

        try {
            QueueWindow.show(Client.primaryStage, () -> new QueueWindow.QueueData(
                    runningScenarioName,
                    new ArrayList<>(queuedRuns),
                    new ArrayList<>(completedRuns)));
        } catch (Exception e) {
            ArrayList<String> txtArray = ScenarioLibraryReportHelper.createSimpleQueueReport(
                    runningScenarioName,
                    queuedRuns,
                    completedRuns);
            utils.displayArrayList(txtArray, "Run Queue");
        }
    }

    private void runModelInterface() {
        String database = vars.getgCamOutputDatabase();
        runModelInterfaceWhich(database);
    }

    private void runModelInterfaceWhich(String databasePath) {
        final String modelInterfaceDirStr = vars.getModelInterfaceDir();
        final File modelInterfaceDir = (modelInterfaceDirStr == null) ? null : new File(modelInterfaceDirStr);
        final String jarName = vars.getModelInterfaceJar();

        ArrayList<String> problems = new ArrayList<>();
        if (modelInterfaceDir == null || modelInterfaceDirStr == null || modelInterfaceDirStr.trim().isEmpty()) {
            problems.add("Set the ModelInterface directory in the options file.");
        } else if (!modelInterfaceDir.isDirectory()) {
            problems.add("The ModelInterface directory was not found: " + modelInterfaceDir.getAbsolutePath());
        }

        File jarFile = null;
        if (jarName == null || jarName.trim().isEmpty()) {
            problems.add("Set the ModelInterface jar file name in the options file.");
        } else if (modelInterfaceDir != null) {
            jarFile = new File(modelInterfaceDir, jarName);
            if (!jarFile.isFile()) {
                problems.add("The ModelInterface jar file was not found: " + jarFile.getAbsolutePath());
            }
        }

        String resolvedDatabasePath = databasePath == null ? "" : databasePath.trim();
        if (resolvedDatabasePath.isEmpty()) {
            problems.add("Set the output database path before opening ModelInterface.");
        }

        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Query file", vars.getQueryFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Units conversion file", vars.getUnitConversionsFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Preset region list file", vars.getPresetRegionListFilename());
        ScenarioLibraryModelInterfaceMiniHelper.validateOptionalFile(problems, "Favorite queries file", vars.getFavoriteQueryFilename());

        File mapsDir = null;
        if (modelInterfaceDir != null) {
            File candidateMapsDir = new File(modelInterfaceDir, "map_resources");
            if (candidateMapsDir.isDirectory()) {
                mapsDir = candidateMapsDir;
            }
        }

        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("ModelInterface could not be started because some required paths or files are missing or invalid.")
              .append(vars.getEol())
              .append(vars.getEol())
              .append("Please review the following:")
              .append(vars.getEol())
              .append(vars.getEol());
            for (String p : problems) {
                sb.append(" - ").append(p).append(vars.getEol());
            }
            utils.showInformationDialog("Configuration needed", "ModelInterface setup is incomplete.", sb.toString());
            System.out.println("Unable to start ModelInterface. " + sb.toString());
            return;
        }

        final ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(jarFile.getAbsolutePath());
        args.add("-o");
        args.add(resolvedDatabasePath);

        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-q", vars.getQueryFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-u", vars.getUnitConversionsFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-p", vars.getPresetRegionListFilename());
        ScenarioLibraryModelInterfaceMiniHelper.appendArgIfPresent(args, "-f", vars.getFavoriteQueryFilename());
        if (mapsDir != null) {
            args.add("-m");
            args.add(mapsDir.getAbsolutePath());
        }

        System.out.println("Starting " + jarName + " using database " + resolvedDatabasePath);
        System.out.println(">>   cmd args: " + args);
        System.out.println(">>   working dir: " + modelInterfaceDir.getAbsolutePath());

        ConsoleManager.appendHeader(ConsoleManager.StreamSource.MODEL_INTERFACE, "Starting ModelInterface");
        ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE, "cmd args: " + args);
        ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE, "working dir: " + modelInterfaceDir.getAbsolutePath());
        if (mapsDir == null) {
            ConsoleManager.appendLine(ConsoleManager.StreamSource.MODEL_INTERFACE,
                    "No ModelInterface map resources folder was found; launching without mapping support.");
        }

        try {
            Client.modelInterfaceExecutionThread.submitCommandWithDirectory(args, modelInterfaceDir.getAbsolutePath());
        } catch (Exception e) {
            utils.warningMessage("Problem starting up ModelInterface. See console for details.");
            System.out.println("Error in trying to start up ModelInterface:");
            System.out.println(e);
        }
    }

    // --- Run control ---
    private GcamRunController getRunController() {
        return runController;
    }

    private boolean hasActiveGcamRun() {
        return runController.hasActiveRun();
    }

    private void ensureLiveStatusRefreshTimeline() {
        if (liveStatusRefreshTimeline != null) {
            return;
        }
        liveStatusRefreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(LIVE_STATUS_REFRESH_INTERVAL.getSeconds()), e -> {
            try {
                if (!hasActiveGcamRun()) {
                    stopLiveStatusRefresh();
                    return;
                }
                // Guard against a previous tick's background work still running.
                if (!liveStatusRefreshInProgress.compareAndSet(false, true)) {
                    return;
                }
                Thread refreshThread = new Thread(() -> {
                    try {
                        updateRunStatus();
                    } catch (Exception ex) {
                        System.out.println("Problem during live status refresh: " + ex);
                    } finally {
                        Platform.runLater(() -> liveStatusRefreshInProgress.set(false));
                    }
                }, "live-status-refresh");
                refreshThread.setDaemon(true);
                refreshThread.start();
            } catch (Exception ignored) {}
        }));
        liveStatusRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
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

    /** Prepares queued runs by confirming reuse, clearing prior logs, optionally switching to archive config, and then launches GCAM. */
    private void runGcamOnSelected() throws IOException {
        if (!WindowsRuntimePreflight.ensureMsvcRuntimeAvailableOrWarn(utils, "GCAM run")) {
            System.out.println("GCAM launch blocked: missing Microsoft Visual C++ runtime.");
            return;
        }
        ScenarioSelection selection = ScenarioSelection.capture();
        ScenarioLibraryRunPreparationHelper.RunPreparationResult preparationResult = runPreparationHelper.prepareSelectedRuns(
                selection,
                new ScenarioLibraryRunPreparationHelper.RunPreparationCallbacks() {
                    @Override
                    public void clearScenarioRunStatusFields(String scenarioName) {
                        PaneScenarioLibrary.this.clearScenarioRunStatusFields(scenarioName);
                    }

                    @Override
                    public void markQueued(String scenarioName) {
                        runController.addQueuedRun(scenarioName);
                    }
                });
        if (!preparationResult.hasLaunchableRuns()) {
            return;
        }
        runGcamModel(preparationResult.getConfigFiles());
    }

    private void runGcamModel(String[] configFiles) {
        if (configFiles == null || configFiles.length == 0) {
            return;
        }
        if (Client.gCAMExecutionThread == null) {
            utils.warningMessage("GCAM execution thread is not available.");
            return;
        }
        File workingDir = new File(vars.getgCamExecutableDir());
        String executable = files.getResolvedPath(vars.getgCamExecutableDir(), vars.getgCamExecutable());
        for (String configFile : configFiles) {
            if (configFile == null || configFile.trim().isEmpty()) {
                continue;
            }
            String scenarioName = scenarioNameFromConfigPath(configFile);
            if (scenarioName.isEmpty()) {
                continue;
            }
            List<String> command = new ArrayList<>();
            command.add(executable);
            command.add("-C");
            command.add(configFile);
            runController.beginRun(
                    Client.gCAMExecutionThread,
                    new GcamRunController.RunRequest(scenarioName, command, workingDir),
                    this::handleGcamProcessLine,
                    new GcamRunController.RunLifecycleListener() {
                        @Override
                        public void onRunStarted(String startedScenarioName) {
                            Platform.runLater(() -> {
                                startLiveStatusRefresh();
                                refreshScenarioActionButtons();
                            });
                        }

                        @Override
                        public void onRunFinished(String finishedScenarioName, ProcessResult result) {
                            PaneScenarioLibrary.this.finalizeScenarioRunArtifacts(finishedScenarioName);
                            if (result != null && (result.getExitCode() != 0 || result.isTimedOut())) {
                                if (finishedScenarioName != null
                                        && finishedScenarioName.equals(runController.getStopRequestedScenarioName())) {
                                    moveExeMainLogToScenarioFolder(finishedScenarioName);
                                    persistStoppedStatusMarker(finishedScenarioName);
                                    markScenarioStopped(finishedScenarioName);
                                } else {
                                    markScenarioDnF(finishedScenarioName);
                                }
                            }
                            Platform.runLater(() -> {
                                try {
                                    updateRunStatus();
                                } catch (Exception ignored) {}
                                refreshScenarioActionButtons();
                                if (!runController.hasActiveRun()) {
                                    stopLiveStatusRefresh();
                                }
                            });
                        }
                    });
        }
        startLiveStatusRefresh();
        refreshScenarioActionButtons();
    }

    /** Stops the current GCAM run. "Stop All" also clears queued scenarios while leaving existing output untouched. */
    private void stopCurrentGcamRun() {
        if (!runController.hasActiveRun()) {
            return;
        }

        ScenarioLibraryStopHelper.StopMode stopMode = ScenarioLibraryStopHelper.promptForStopMode();
        if (stopMode == ScenarioLibraryStopHelper.StopMode.CONTINUE) {
            return;
        }

        boolean stopAll = stopMode == ScenarioLibraryStopHelper.StopMode.STOP_ALL;
        if (stopAll) {
            try {
                int nCancelled = 0;
                if (Client.gCAMExecutionThread != null) {
                    nCancelled = Client.gCAMExecutionThread.cancelQueuedJobsKeepRunningCurrent();
                }

                runController.clearQueuedRuns();
                for (ScenarioRow sr : ScenarioTable.listOfScenarioRuns) {
                    if (sr != null && "In queue".equals(sr.getStatus())) {
                        sr.setStatus("");
                    }
                }
                handleSelectionPreservingTableRefresh(null);

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

        try {
            ProcessRunner.StopResult stopResult = runController.stopCurrentRun();
            if (stopResult != null) {
                try {
                    ConsoleManager.appendLine(ConsoleManager.StreamSource.GCAM_STDOUT,
                            ConsoleManager.MessageKind.GLIMPSE_INFO,
                            "Stop signal sent (" + stopResult.getSummary() + ")");
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

    private String scenarioNameFromConfigPath(String configFile) {
        if (configFile == null || configFile.trim().isEmpty()) {
            return "";
        }
        try {
            File config = new File(configFile);
            String scenarioDir = new File(vars.getScenarioDir()).getAbsolutePath();
            File parent = config.getParentFile();
            if (parent != null && scenarioDir.equals(parent.getParentFile() == null ? "" : parent.getParentFile().getAbsolutePath())) {
                return parent.getName();
            }
            String line = files.searchForTextInFileS(config, "scenarioName", "<!--");
            String parsed = utils.getStringBetweenCharSequences(line, ">", "</");
            return parsed == null ? "" : parsed.trim();
        } catch (Exception ignored) {}
        return "";
    }

    private void handleGcamProcessLine(String scenarioName, String line, boolean stderr) {
        if (line == null) {
            return;
        }
        try {
            ConsoleManager.appendLineBuffered(
                    ConsoleManager.StreamSource.GCAM_STDOUT,
                    stderr ? ConsoleManager.MessageKind.STDERR : ConsoleManager.MessageKind.MODEL_STDOUT,
                    line);
        } catch (Exception ignored) {}
        if (!stderr) {
            try {
                if (isLiveStdoutErrorLine(line)) {
                    recordLiveStdoutError(scenarioName, line);
                }
            } catch (Exception ignored) {}
            try {
                gcamPromptMonitor.handlePotentialInteractivePrompt(line);
            } catch (Exception ignored) {}
            try {
                maybeMarkLiveGcamSuccess(line);
            } catch (Exception ignored) {}
        }
    }

    private void maybePromptForDatabaseRelease(String promptLine) {
        if (!looksLikeDatabaseSavePrompt(promptLine)) {
            return;
        }
        String normalizedPrompt = normalizeDatabasePromptText(promptLine);
        if (normalizedPrompt.isEmpty()) {
            return;
        }
        if (runController.isPromptDialogActive()) {
            return;
        }
        if (runController.isDatabasePromptAwaitingReset() && normalizedPrompt.equals(runController.getLastHandledDatabasePrompt())) {
            return;
        }

        ProcessRunner.RunningProcess rp = runController.getCurrentRun();
        if (rp == null || rp.getProcess() == null || !rp.getProcess().isAlive()) {
            return;
        }

        runController.setPromptDialogActive(true);
        runController.setActivePromptLine(promptLine);
        runController.setLastHandledDatabasePrompt(normalizedPrompt);
        runController.setDatabasePromptAwaitingReset(true);

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

                ProcessRunner.RunningProcess live = runController.getCurrentRun();
                if (live == null || live.getProcess() == null || !live.getProcess().isAlive()) {
                    runController.setPromptDialogActive(false);
                    runController.setActivePromptLine(null);
                    return;
                }

                Alert alert = new Alert(AlertType.CONFIRMATION);
                glimpseUtil.UtilsDialogs.initDialogOwner(alert);
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

                ProcessRunner.RunningProcess afterDialog = runController.getCurrentRun();
                if (confirmed) {
                    boolean sent = false;
                    try {
                        if (afterDialog != null && afterDialog.getProcess() != null && afterDialog.getProcess().isAlive()) {
                            sent = afterDialog.sendLine();
                        }
                    } catch (Exception ignored) {}

                    gcamPromptMonitor.clearRecentPromptBuffer();
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
                runController.setPromptDialogActive(false);
                runController.setActivePromptLine(null);
                try {
                    updateRunStatus();
                } catch (Exception ignored) {}
            }
        });
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
                String scenarioName = runController.getCurrentScenarioName();
                if (scenarioName == null || scenarioName.trim().isEmpty()) {
                    return;
                }
                String normalizedScenarioName = scenarioName.trim();
                if (liveSuccessMarkedByScenario.putIfAbsent(normalizedScenarioName, Boolean.TRUE) != null) {
                    return;
                }
                clearLiveStdoutError(normalizedScenarioName);
                Platform.runLater(() -> {
                    try {
                        updateScenarioRowInPlace(normalizedScenarioName, row -> {
                            if (sameText(row.getStatus(), "Success")) {
                                return false;
                            }
                            row.setStatus("Success");
                            return true;
                        });
                    } catch (Exception ignored) {}
                });
                return;
            }
        }
    }

    // --- Refresh and status ---
    public void updateRunStatus() {
        ScenarioStatusRefreshResult refreshResult = scenarioStatusService.refresh(buildScenarioStatusRefreshRequest());
        reconcileQueuedRunTracking(refreshResult);
        updateStatusBarComputerStats(refreshResult.getRunningScenario());
        Platform.runLater(() -> applyScenarioStatusSnapshots(refreshResult.getSnapshots(), refreshResult.isNoScenarios()));
    }

    private void updateStatusBarComputerStats(String runningScenario) {
        String statsText = "";
        try {
            statsText = utils.getComputerStatString();
        } catch (Exception ignored) {}

        String safeStatsText = statsText == null ? "" : statsText.trim();
        if (safeStatsText.isEmpty()) {
            safeStatsText = READY_MESSAGE;
        }
        Client.setStartupStatus(safeStatsText, -1, false);
    }

    private void applyScenarioStatusSnapshots(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios) {
        viewStateHelper.applySnapshots(
                snapshots,
                noScenarios,
                pendingRefreshViewState,
                NO_SCENARIOS_MESSAGE,
                READY_MESSAGE);
        applyLiveStdoutErrorPeriods();
        applyLiveRuntimeForActiveScenario();
        pendingRefreshViewState = ScenarioLibraryViewStateHelper.RefreshViewState.empty();
        refreshScenarioActionButtons();
    }

    private void applyLiveStdoutErrorPeriods() {
        if (liveStdoutErrorPeriodsByScenario.isEmpty()) {
            return;
        }
        GcamRunController.ExecutionState executionState = runController.snapshot();
        for (java.util.Map.Entry<String, LinkedHashSet<String>> entry : liveStdoutErrorPeriodsByScenario.entrySet()) {
            String scenarioName = entry.getKey();
            String periodsText = formatLiveErrorPeriods(entry.getValue());
            if (scenarioName == null || scenarioName.trim().isEmpty() || periodsText.isEmpty()) {
                continue;
            }
            if (!executionState.isScenarioActivelyRunning(scenarioName)) {
                continue;
            }
            updateScenarioRowInPlace(scenarioName, row -> {
                if (sameText(row.getUnsolvedMarkets(), periodsText)) {
                    return false;
                }
                row.setUnsolvedMarkets(periodsText);
                return true;
            });
        }
    }

    private boolean isLiveStdoutErrorLine(String line) {
        if (line == null) {
            return false;
        }
        return line.trim().startsWith(LIVE_STDOUT_ERROR_PREFIX);
    }

    private LinkedHashSet<String> extractLiveErrorPeriods(String line) {
        LinkedHashSet<String> periods = new LinkedHashSet<>();
        if (line == null) {
            return periods;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || !trimmed.toLowerCase(Locale.ENGLISH).contains("did not solve period")) {
            return periods;
        }
        java.util.regex.Matcher matcher = LIVE_UNSOLVED_PERIOD_ERROR_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return periods;
        }
        String rawPeriods = matcher.group(1);
        if (rawPeriods == null || rawPeriods.trim().isEmpty()) {
            return periods;
        }
        java.util.regex.Matcher numberMatcher = LIVE_UNSOLVED_PERIOD_NUMBER_PATTERN.matcher(rawPeriods);
        while (numberMatcher.find()) {
            String period = numberMatcher.group() == null ? "" : numberMatcher.group().trim();
            if (!period.isEmpty()) {
                periods.add(period);
            }
        }
        return periods;
    }

    private String formatLiveErrorPeriods(java.util.Set<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return "";
        }
        return String.join(",", periods);
    }

    private void recordLiveStdoutError(String scenarioName, String line) {
        String normalizedScenarioName = scenarioName == null ? "" : scenarioName.trim();
        if (normalizedScenarioName.isEmpty()) {
            return;
        }
        LinkedHashSet<String> periods = extractLiveErrorPeriods(line);
        if (periods.isEmpty()) {
            return;
        }
        LinkedHashSet<String> updatedPeriods = liveStdoutErrorPeriodsByScenario.compute(normalizedScenarioName, (key, existing) -> {
            LinkedHashSet<String> mergedPeriods = existing == null ? new LinkedHashSet<>() : new LinkedHashSet<>(existing);
            mergedPeriods.addAll(periods);
            return mergedPeriods;
        });
        String periodsText = formatLiveErrorPeriods(updatedPeriods);
        if (periodsText.isEmpty()) {
            return;
        }
        updateScenarioRowInPlace(normalizedScenarioName, row -> {
            if (sameText(row.getUnsolvedMarkets(), periodsText)) {
                return false;
            }
            row.setUnsolvedMarkets(periodsText);
            return true;
        });
    }

    private void clearLiveStdoutError(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        liveStdoutErrorPeriodsByScenario.remove(scenarioName.trim());
    }

    private void applyLiveRuntimeForActiveScenario() {
        GcamRunController.ExecutionState executionState = runController.snapshot();
        String scenarioName = executionState.currentScenarioName;
        long runStartTimeMillis = executionState.currentRunStartTimeMillis;
        if (!executionState.isScenarioActivelyRunning(scenarioName) || runStartTimeMillis <= 0L) {
            return;
        }
        String liveRuntime = formatLiveRuntime(runStartTimeMillis, System.currentTimeMillis());
        if (liveRuntime.isEmpty()) {
            return;
        }
        updateScenarioRowInPlace(scenarioName, row -> {
            String currentRuntime = row.getRuntime();
            if (sameText(currentRuntime, liveRuntime)) {
                return false;
            }
            row.setRuntime(liveRuntime);
            return true;
        });
    }

    public void clearAndRefreshScenarioTable() {
        refreshScenarioStatusAsync(true);
    }

    /** Captures table view state before refresh and restores it after refreshed snapshots are applied. */
    public void refreshScenarioStatusAsync(boolean userInitiated) {
        if (!scenarioRefreshInProgress.compareAndSet(false, true)) {
            return;
        }
        pendingRefreshViewState = capturePendingRefreshViewState();
        Platform.runLater(() -> {
            if (ScenarioTable.tableScenariosLibrary != null) {
                ScenarioTable.tableScenariosLibrary.setPlaceholder(utils.createLabel(LOADING_SCENARIOS_MESSAGE));
            }
            refreshScenarioActionButtons();
        });
        //System.out.println(refreshMessage);
        Thread refreshThread = new Thread(() -> {
            try {
                updateRunStatus();
                Platform.runLater(() -> {
                    try {
                        if (!userInitiated) {
                            Client.markInitialScenarioLoadComplete();
                        }
                        refreshScenarioActionButtons();
                    } finally {
                        scenarioRefreshInProgress.set(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    System.out.println("Problem updating scenario table: " + ex);
                    System.out.println(ERROR_LOADING_SCENARIOS_MESSAGE);
                    pendingRefreshViewState = ScenarioLibraryViewStateHelper.RefreshViewState.empty();
                    if (!userInitiated) {
                        Client.markInitialScenarioLoadComplete();
                    }
                    refreshScenarioActionButtons();
                    scenarioRefreshInProgress.set(false);
                });
            }
        }, "scenario-status-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private ScenarioLibraryViewStateHelper.RefreshViewState capturePendingRefreshViewState() {
        if (Platform.isFxApplicationThread()) {
            return viewStateHelper.capture();
        }
        CountDownLatch latch = new CountDownLatch(1);
        final ScenarioLibraryViewStateHelper.RefreshViewState[] capturedState =
                { ScenarioLibraryViewStateHelper.RefreshViewState.empty() };
        Platform.runLater(() -> {
            try {
                capturedState[0] = viewStateHelper.capture();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ScenarioLibraryViewStateHelper.RefreshViewState.empty();
        }
        return capturedState[0] == null ? ScenarioLibraryViewStateHelper.RefreshViewState.empty() : capturedState[0];
    }

    private ScenarioStatusService.RefreshRequest buildScenarioStatusRefreshRequest() {
        GcamRunController.ExecutionState executionState = runController.snapshot();
        return new ScenarioStatusService.RefreshRequest(
                executionState.queuedScenarioNames,
                startupTime,
                executionState.currentScenarioName,
                runController.getStopRequestedScenarioName(),
                executionState.promptDialogActive,
                executionState.databasePromptAwaitingReset,
                executionState.isScenarioActivelyRunning(executionState.currentScenarioName));
    }

    private void reconcileQueuedRunTracking(ScenarioStatusRefreshResult refreshResult) {
        if (refreshResult == null) {
            return;
        }
        runController.replaceQueuedRuns(refreshResult.getQueuedRuns());
    }

    private void markScenarioStoppedDnF(String scenarioName) {
        markScenarioStopped(scenarioName);
    }

    private void markScenarioStopped(String scenarioName) {
        clearLiveStdoutError(scenarioName);
        updateScenarioTerminalStatus(scenarioName, ScenarioStatusService.STATUS_STOPPED);
    }

    private void markScenarioDnF(String scenarioName) {
        clearLiveStdoutError(scenarioName);
        updateScenarioTerminalStatus(scenarioName, ScenarioStatusService.STATUS_DNF);
    }

    private void updateScenarioTerminalStatus(String scenarioName, String statusText) {
        if (scenarioName == null || scenarioName.trim().isEmpty() || statusText == null || statusText.trim().isEmpty()) {
            return;
        }
        if (ScenarioStatusService.STATUS_STOPPED.equals(statusText)) {
            runController.markScenarioStopped(scenarioName);
        }
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
                    row.setStatus(statusText);
                    break;
                }
            } catch (Exception ignored) {}
        });
    }

    private void persistStoppedStatusMarker(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        try {
            Path mainLogPath = ScenarioLibraryPathHelper.scenarioMainLogPath(vars.getScenarioDir(), scenarioName);
            if (mainLogPath == null) {
                return;
            }
            Files.createDirectories(mainLogPath.getParent());
            String existing = Files.exists(mainLogPath)
                    ? new String(Files.readAllBytes(mainLogPath), StandardCharsets.UTF_8)
                    : "";
            if (existing.contains(STOPPED_LOG_MARKER)) {
                return;
            }
            StringBuilder markerText = new StringBuilder();
            if (!existing.isEmpty() && !existing.endsWith(System.lineSeparator())) {
                markerText.append(System.lineSeparator());
            }
            markerText.append(STOPPED_LOG_MARKER).append(System.lineSeparator());
            Files.write(
                    mainLogPath,
                    markerText.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ex) {
            System.out.println("Problem writing stopped marker for scenario " + scenarioName + ": " + ex);
        }
    }

    private void moveExeMainLogToScenarioFolder(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        try {
            Path exeMainLogPath = ScenarioLibraryPathHelper.exeMainLogPath(vars.getgCamExecutableDir());
            if (exeMainLogPath == null || !Files.exists(exeMainLogPath)) {
                return;
            }
            Path scenarioMainLogPath = ScenarioLibraryPathHelper.scenarioMainLogPath(vars.getScenarioDir(), scenarioName);
            if (scenarioMainLogPath == null) {
                return;
            }
            Path scenarioDirPath = scenarioMainLogPath.getParent();
            if (scenarioDirPath != null) {
                Files.createDirectories(scenarioDirPath);
            }
            Files.move(exeMainLogPath, scenarioMainLogPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            System.out.println("Problem moving executable main_log.txt for stopped scenario " + scenarioName + ": " + ex);
        }
    }

    private void finalizeScenarioRunArtifacts(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        try {
            Path scenarioDirPath = Paths.get(vars.getScenarioDir(), scenarioName);
            Files.createDirectories(scenarioDirPath);
            Path exeLogsDir = Paths.get(vars.getgCamExecutableDir(), "logs");
            for (String artifactName : EXE_LOG_ARTIFACT_FILENAMES) {
                if (artifactName == null || artifactName.trim().isEmpty()) {
                    continue;
                }
                Path sourcePath = exeLogsDir.resolve(artifactName);
                if (!Files.exists(sourcePath)) {
                    continue;
                }
                Path targetPath = scenarioDirPath.resolve(artifactName);
                try {
                    Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception copyEx) {
                    System.out.println("Problem copying run artifact " + artifactName + " for scenario " + scenarioName + ": " + copyEx);
                }
            }
        } catch (Exception ex) {
            System.out.println("Problem finalizing run artifacts for scenario " + scenarioName + ": " + ex);
        }
    }

    // --- Reports ---
    private void generateRunReport() {
        ArrayList<String> txtArray = ScenarioLibraryReportHelper.createScenarioExecutionReport(
                files,
                vars.getScenarioDir(),
                ScenarioTable.listOfScenarioRuns,
                captureSelectedScenarioNames(),
                runController.getQueuedRuns(),
                runController.getCompletedRuns(),
                getStopRequestedScenarioName());
        if (txtArray.size() <= 5) {
            utils.warningMessage("No scenario report available.");
            return;
        }
        utils.displayArrayList(txtArray, "Scenario Execution Report");
    }

    private void generateExeErrorReport() {
        ScenarioLibraryReportHelper.ErrorTextReport report = ScenarioLibraryReportHelper.createExecutableErrorTextReport(
                files,
                ScenarioLibraryPathHelper.exeMainLogPath(vars.getgCamExecutableDir()).toFile());
        if (!report.hasVisibleContent("All lines") || report.buildText("All lines").trim().isEmpty()) {
            utils.warningMessage("No executable error report available.");
            return;
        }
        utils.showTextErrorReport(report, 910, 600);
    }

    private void generateErrorReport() {
        ScenarioSelection selection = ScenarioSelection.capture();
        if (scenarioFileActionService.warnIfAnyScenarioMainLogMissing(selection)) {
            return;
        }
        ScenarioLibraryReportHelper.ErrorTextReport report = ScenarioLibraryReportHelper.createScenarioErrorTextReport(
                files,
                vars.getScenarioDir(),
                selection.getRows());
        if (!report.hasVisibleContent("All lines") || report.buildText("All lines").trim().isEmpty()) {
            utils.warningMessage("No scenario error report available.");
            return;
        }
        utils.showTextErrorReport(report, 910, 600);
    }

    // --- Helpers ---
    private static boolean looksLikeDatabaseSavePrompt(String line) {
        return ScenarioLibraryPromptHelper.looksLikeDatabaseSavePrompt(line);
    }

    private static String normalizeDatabasePromptText(String text) {
        return ScenarioLibraryPromptHelper.normalizeDatabasePromptText(text);
    }

    private List<String> captureSelectedScenarioNames() {
        return new ArrayList<>(ScenarioSelection.capture().getScenarioNames());
    }

    private boolean hasModelInterfaceLocationConfigured() {
        String modelInterfaceDir = vars.getModelInterfaceDir();
        return modelInterfaceDir != null && !modelInterfaceDir.trim().isEmpty();
    }

    private String getStopRequestedScenarioName() {
        return runController.getStopRequestedScenarioName();
    }

    private void clearDeletedScenarioRunState(ScenarioSelection selection) {
        clearScenarioTransientRunState(selection, ScenarioRunStateClearMode.DELETE);
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
            runController.removeQueuedRun(scenName);
            if ("In queue".equals(row.getStatus())) {
                row.setStatus("");
            }
        }
    }

    void clearScenarioRunStatusFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.PREPARE_RUN);
    }

    void clearScenarioRunResultFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.RECREATE_OVERWRITE);
    }

    private void clearImportedScenarioRunResultFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.IMPORT_OVERWRITE);
    }

    private void clearScenarioTransientRunState(ScenarioSelection selection, ScenarioRunStateClearMode mode) {
        if (selection == null) {
            return;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            clearScenarioTransientRunState(scenarioName, mode);
        }
    }

    private void clearScenarioTransientRunState(String scenarioName, ScenarioRunStateClearMode mode) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        ScenarioRunStateClearMode effectiveMode = mode == null ? ScenarioRunStateClearMode.IMPORT_OVERWRITE : mode;
        runController.clearStoppedScenario(scenarioName);
        clearLiveStdoutError(scenarioName);
        liveSuccessMarkedByScenario.remove(scenarioName.trim());
        if (ScenarioRunStateClearMode.DELETE.equals(effectiveMode)) {
            return;
        }
        Platform.runLater(() -> {
            try {
                for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                    if (row == null || !scenarioName.equals(row.getScenarioName())) {
                        continue;
                    }
                    row.setCompletedDate("");
                    row.setRuntime("");
                    row.setUnsolvedMarkets("");
                    row.setStatus(effectiveMode.statusText);
                    break;
                }
            } catch (Exception ignored) {
            }
        });
    }

    public HBox gethBox() {
        return scenarioLibraryHBox;
    }

    private void handleSelectionPreservingTableRefresh(Runnable updateAction) {
        if (Platform.isFxApplicationThread()) {
            viewStateHelper.preserveSelectionAndRefresh(updateAction);
            refreshScenarioActionButtons();
            return;
        }
        Platform.runLater(() -> {
            viewStateHelper.preserveSelectionAndRefresh(updateAction);
            refreshScenarioActionButtons();
        });
    }

    private static boolean sameText(String left, String right) {
        String normalizedLeft = left == null ? "" : left;
        String normalizedRight = right == null ? "" : right;
        return normalizedLeft.equals(normalizedRight);
    }

    private void updateScenarioRowInPlace(String scenarioName, java.util.function.Predicate<ScenarioRow> rowUpdate) {
        if (scenarioName == null || scenarioName.trim().isEmpty() || rowUpdate == null) {
            return;
        }
        Runnable applyUpdate = () -> {
            try {
                for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                    if (row != null && scenarioName.equals(row.getScenarioName())) {
                        rowUpdate.test(row);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        };
        if (Platform.isFxApplicationThread()) {
            applyUpdate.run();
            return;
        }
        Platform.runLater(applyUpdate);
    }

    private static String formatLiveRuntime(long runStartTimeMillis, long currentTimeMillis) {
        if (runStartTimeMillis <= 0L || currentTimeMillis <= runStartTimeMillis) {
            return "";
        }
        long elapsedMillis = currentTimeMillis - runStartTimeMillis;
        long totalSeconds = Math.max(0L, elapsedMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder runtime = new StringBuilder(LIVE_RUNTIME_PREFIX);
        if (hours > 0L) {
            runtime.append(hours).append(" hr ");
        }
        if (hours > 0L || minutes > 0L) {
            runtime.append(minutes).append(" min ");
        }
        runtime.append(seconds).append(" sec");
        return runtime.toString().trim();
    }

    private enum ScenarioRunStateClearMode {
        PREPARE_RUN("Updating..."),
        IMPORT_OVERWRITE(""),
        RECREATE_OVERWRITE(""),
        DELETE(null);

        final String statusText;

        ScenarioRunStateClearMode(String statusText) {
            this.statusText = statusText;
        }
    }
}
