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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Manages the Scenario Library pane (bottom panel), including run orchestration, status refresh,
 * reporting, and file actions for scenarios.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Creates and manages scenario action buttons (run/stop/log/config/results/diff/import/delete).</li>
 *   <li>Coordinates queued GCAM runs through {@link GcamRunController} and execution threads.</li>
 *   <li>Refreshes scenario table status from logs/files using {@link ScenarioStatusService}.</li>
 *   <li>Preserves selection/focus across refreshes via {@link ScenarioLibraryViewStateHelper}.</li>
 *   <li>Streams run output into {@link ConsoleManager} and reacts to interactive GCAM prompts.</li>
 *   <li>Provides error and execution reports using {@link ScenarioLibraryReportHelper}.</li>
 * </ul>
 *
 * <b>Usage:</b> Constructed by {@link ScenarioBuilder} during UI build. Call
 * {@link #refreshScenarioStatusAsync(boolean)} to update table content and
 * {@link #requestDefaultCreatedSortAndScrollToTopOnNextRefresh()} after adding/importing scenarios.
 * <p>
 * <b>Thread Safety:</b> UI updates are marshaled to JavaFX. Log parsing and refresh work run on
 * background daemon threads.
 * <p>
 * <b>Integration:</b>
 * <ul>
 *   <li>Coordinates scenario file actions through {@link ScenarioFileActionService}.</li>
 *   <li>Computes row status via {@link ScenarioStatusService} and applies view-state restoration.</li>
 *   <li>Uses {@link ConsoleManager} and {@link GcamPromptMonitor} for live run feedback and prompts.</li>
 * </ul>
 */
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
    private static final String STARTUP_FAILURE_PREFIX = "GLIMPSE failed to start GCAM";
    private static final String WINDOWS_POLICY_BLOCK_ERROR_CODE = "createprocess error=4551";
    private static final String WINDOWS_POLICY_BLOCK_TEXT = "application control policy has blocked this file";

    private static final String LOADING_SCENARIOS_MESSAGE = "Loading scenario status...";
    private static final String NO_SCENARIOS_MESSAGE = "No scenarios found.";
    private static final String READY_MESSAGE = "Ready";
    private static final String ERROR_LOADING_SCENARIOS_MESSAGE = "Problem loading scenario status.";
    private static final String LIVE_RUNTIME_PREFIX = "> ";
    private static final String DIFF_LABEL = "Diff";
    private static final String DIFF_TOOLTIP = "Diff: Compare first two selected configurations";
    private static final String REFRESH_LABEL = "Refresh";
    private static final String REFRESH_TOOLTIP = "Refresh: Update scenario run status";
    private static final String CONSOLE_LABEL = "Console";
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
    /** Captured selection snapshot used to derive button enable/disable state. */
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
    private final ConcurrentHashMap<String, Boolean> windowsPolicyBlockPromptShownByScenario = new ConcurrentHashMap<>();
    /** Maps scenario name to the command submitted to the executor (for error reporting). */
    private final ConcurrentHashMap<String, String> submittedCommandByScenario = new ConcurrentHashMap<>();
    private final AtomicBoolean resetToDefaultCreatedSortAndScroll = new AtomicBoolean(true);
    /** True after a native/JVM linkage failure while reading resource stats; prevents repeated throws each refresh tick. */
    private final AtomicBoolean resourceStatsUnavailable = new AtomicBoolean(false);

    // --- Constructors ---
    /**
     * Creates and initializes the Scenario Library pane and action controls.
     *
     * @param stage owning stage (currently retained for compatibility with existing construction flow)
     */
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
        requestDefaultCreatedSortAndScrollToTopOnNextRefresh();
        refreshScenarioActionButtons();
        if (startupTime == 0) {
            startupTime = (new Date()).getTime();
        }
        System.out.println("time now=" + (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).format(startupTime));
    }

    /**
     * Zero-arg constructor retained for compatibility with tests and legacy call sites.
     */
    PaneScenarioLibrary() {}

    // --- UI setup ---
    /**
     * Orchestrates the creation of all scenario library action buttons and their configuration.
     */
    private void createScenarioLibraryButtons() {
        createScenarioLibraryButtonInstances();
        configureInitialScenarioLibraryButtonState();
        bindScenarioLibraryButtonHandlers();
        configureScenarioLibraryButtonLayout();
        refreshScenarioActionButtons();
    }

    /**
     * Creates instances of all scenario library buttons and stores them in static Client fields.
     */
    private void createScenarioLibraryButtonInstances() {
        Client.buttonDiffFiles = createTimedScenarioButton("buttonDiffFiles", DIFF_LABEL, styles.getBigButtonWidth(), DIFF_TOOLTIP, "compare");
        Client.buttonRefreshScenarioStatus = createTimedScenarioButton("buttonRefreshScenarioStatus", REFRESH_LABEL, styles.getBigButtonWidth(), REFRESH_TOOLTIP, "refresh1");
        Client.buttonConsole = createTimedScenarioButton("buttonConsole", CONSOLE_LABEL, styles.getBigButtonWidth(), CONSOLE_TOOLTIP, "console");
        Client.buttonResults = createTimedScenarioButton("buttonResults", RESULTS_LABEL, styles.getBigButtonWidth(), RESULTS_TOOLTIP, "results");
        Client.buttonResultsForSelected = createTimedScenarioButton("buttonResultsForSelected", RESULTS_SELECTED_LABEL, styles.getBigButtonWidth(), RESULTS_SELECTED_TOOLTIP, "results-selected");
        Client.buttonRunScenario = createTimedScenarioButton("buttonRunScenario", PLAY_LABEL, styles.getBigButtonWidth(), PLAY_TOOLTIP, "play");
        Client.buttonStopScenario = createTimedScenarioButton("buttonStopScenario", "Stop", styles.getBigButtonWidth(), "Stop the currently running GCAM scenario", "stop");
        Client.buttonDeleteScenario = createTimedScenarioButton("buttonDeleteScenario", DELETE_LABEL, styles.getBigButtonWidth(), DELETE_TOOLTIP, "delete1");
        Client.buttonViewConfig = createTimedScenarioButton("buttonViewConfig", CONFIG_LABEL, styles.getBigButtonWidth(), CONFIG_TOOLTIP, "edit1");
        Client.buttonViewLog = createTimedScenarioButton("buttonViewLog", LOG_LABEL, styles.getBigButtonWidth(), LOG_TOOLTIP, "log-selected");
        Client.buttonViewExeErrors = createTimedScenarioButton("buttonViewExeErrors", EXE_ERRORS_LABEL, styles.getBigButtonWidth(), EXE_ERRORS_TOOLTIP, "errors");
        Client.buttonViewErrors = createTimedScenarioButton("buttonViewErrors", ERRORS_LABEL, styles.getBigButtonWidth(), ERRORS_TOOLTIP, "errors-selected");
        Client.buttonViewExeLog = createTimedScenarioButton("buttonViewExeLog", EXE_LOG_LABEL, styles.getBigButtonWidth(), EXE_LOG_TOOLTIP, "log");
        Client.buttonBrowseScenarioFolder = createTimedScenarioButton("buttonBrowseScenarioFolder", BROWSE_LABEL, styles.getBigButtonWidth(), BROWSE_TOOLTIP, "open_folder1");
        Client.buttonImportScenario = createTimedScenarioButton("buttonImportScenario", IMPORT_LABEL, styles.getBigButtonWidth(), IMPORT_TOOLTIP, "add");
        Client.buttonShowRunQueue = createTimedScenarioButton("buttonShowRunQueue", QUEUE_LABEL, styles.getBigButtonWidth(), QUEUE_TOOLTIP, "queue");
        Client.buttonArchiveScenario = createTimedScenarioButton("buttonArchiveScenario", ARCHIVE_LABEL, styles.getBigButtonWidth(), ARCHIVE_TOOLTIP, null);
        Client.buttonReport = createTimedScenarioButton("buttonReport", REPORT_LABEL, styles.getBigButtonWidth(), REPORT_TOOLTIP, null);
    }

    /**
     * Sets initial disabled/enabled state for all scenario library buttons based on current selection.
     */
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

    /**
     * Attaches action event handlers to all scenario library buttons.
     */
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
                String diagnostics = buildRunFailureDiagnostics(ex);
                String fullMessage = "Problem running GCAM." + vars.getEol() + vars.getEol() + diagnostics;
                utils.warningMessage(fullMessage);
                System.out.println("Error trying to run GCAM.");
                System.out.println("Full diagnostics:");
                System.out.println(diagnostics);
                System.out.println("Exception: " + ex);
                ex.printStackTrace();
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

    /**
     * Configures the layout properties and visibility of all scenario library buttons.
     */
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

    /**
     * Wires the table selection model to refresh button states whenever selection changes.
     */
    private void wireScenarioSelectionButtonRefresh() {
        if (ScenarioTable.tableScenariosLibrary == null || ScenarioTable.tableScenariosLibrary.getSelectionModel() == null) {
            return;
        }
        ScenarioTable.tableScenariosLibrary.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                refreshScenarioActionButtons());
        ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<ScenarioRow>) change -> refreshScenarioActionButtons());
    }

    /**
     * Refreshes scenario action buttons by capturing selection state and applying button enable/disable logic.
     * Marshals the update to the JavaFX application thread if needed.
     */
    private void refreshScenarioActionButtons() {
        if (Platform.isFxApplicationThread()) {
            applyScenarioActionButtonState();
            return;
        }
        Platform.runLater(this::applyScenarioActionButtonState);
    }

    /**
     * Applies arrow status and button enable/disable logic based on current selection state.
     */
    private void applyScenarioActionButtonState() {
        try {
            setArrowAndButtonStatus();
            applyScenarioLibrarySelectionState(ScenarioLibrarySelectionState.capture());
        } catch (Exception ignored) {}
    }

    /**
     * Applies enable/disable state to individual scenario action buttons based on selection state.
     *
     * @param selectionState the current selection state snapshot
     */
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
    /**
     * Handles the Archive Scenario button action after user confirmation.
     */
    private void handleArchiveScenario() {
        if (!utils.confirmArchiveScenario()) {
            return;
        }
        scenarioFileActionService.archiveScenarios(ScenarioSelection.capture());
    }

    /**
     * Handles the Delete Scenario button action after user confirmation.
     * Dequeues scenarios, clears run state, and delegates file deletion to the service.
     */
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

    /**
     * Handles the Results button action to open ModelInterface with all available output databases.
     */
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

    /**
     * Handles the Results-Selected button action to open ModelInterface with the selected scenario's database.
     */
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
        String workingScenarioLog = ScenarioLibraryPathHelper.glimpseRunsFile(vars.getGlimpseLogDir());
        File workingScenariosFile = new File(workingScenarioLog);
        boolean doesScenarioExist = files.searchForTextAtStartOfLinesInFile(workingScenariosFile, scenName + ",", "#");
        String confirmMsg = doesScenarioExist ? "Overwrite existing scenario " + scenName + "?" : "Import " + scenName + " into GLIMPSE?";
        if (!utils.confirmAction(confirmMsg)) {
            return;
        }

        Client.beginScenarioOperationProgress();
        try {
            ScenarioFileActionService.ImportResult importResult = scenarioFileActionService.importScenarioConfig(configFile);
            if (!importResult.wasImported()) {
                return;
            }
            if (doesScenarioExist) {
                clearImportedScenarioRunResultFields(scenName);
            }
            ScenarioRow[] newRun = { importResult.getScenarioRow() };
            ScenarioTable.addToListOfRunFiles(newRun);
            requestDefaultCreatedSortAndScrollToTopOnNextRefresh();
        } finally {
            Client.endScenarioOperationProgress();
        }
    }

    /**
     * Handles the Browse Scenario Folder button action to open selected scenario directories.
     */
    private void handleBrowseScenarioFolder() {
        scenarioFileActionService.openScenarioFolders(ScenarioSelection.capture());
    }

    /**
     * Handles the Import Scenario button action to import an existing configuration file as a new scenario.
     * Prompts user for overwrite if scenario already exists.
     */
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
            requestDefaultCreatedSortAndScrollToTopOnNextRefresh();
        } finally {
            Client.endScenarioOperationProgress();
        }
    }

    /**
     * Handles the Config button action to open the configuration file of the selected scenario.
     */
    private void handleViewConfig() {
        scenarioFileActionService.openScenarioConfigs(ScenarioSelection.capture());
    }

    /**
     * Handles the Log button action to view the main_log.txt file of the selected scenario.
     */
    private void handleViewLog() {
        scenarioFileActionService.openScenarioLogs(ScenarioSelection.capture());
    }

    /**
     * Handles the ExeLog button action to view the main_log.txt file in the GCAM executable log folder.
     */
    private void handleViewExeLog() {
        scenarioFileActionService.openExeLog();
    }

    /**
     * Handles the Diff button action to compare configuration files of the two selected scenarios side-by-side.
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
            DiffWindow.show(Client.primaryStage, file1, file2, rows);
        } catch (Exception e) {
            utils.warningMessage("Problem generating diff: " + e.getMessage());
        }
    }

    /**
     * Handles the Queue button action to display the list of queued, running, and completed scenarios.
     */
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

    /**
     * Launches ModelInterface with the configured GCAM output database.
     */
    private void runModelInterface() {
        String database = vars.getgCamOutputDatabase();
        runModelInterfaceWhich(database);
    }

    /**
     * Launches ModelInterface with a specific output database path.
     * Validates configuration and command-line arguments before launch.
     *
     * @param databasePath the absolute path to the output database
     */
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
        // TODO: Re-enable font size pass-through once JavaFX (ScenarioBuilder) and Swing (ModelInterface)
        //       font-size scaling can be reconciled (Windows HiDPI / toolkit mismatch).
        // ScenarioLibraryModelInterfaceMiniHelper.appendModelInterfaceFontSizeArgIfValid(args, vars.getPreferredFontSize());

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

        showModelInterfaceLaunchingToast(5000);

        try {
            Future<?> modelInterfaceFuture = Client.modelInterfaceExecutionThread.submitCommandWithDirectory(
                    args,
                    modelInterfaceDir.getAbsolutePath());
            monitorModelInterfaceCompletion(modelInterfaceFuture);
        } catch (Exception e) {
            utils.warningMessage("Problem starting up ModelInterface. See console for details.");
            System.out.println("Error in trying to start up ModelInterface:");
            System.out.println(e);
        }
    }

    /**
     * Monitors ModelInterface process completion and triggers a database size refresh on exit.
     *
     * @param modelInterfaceFuture the future representing the running ModelInterface process
     */
    private void monitorModelInterfaceCompletion(Future<?> modelInterfaceFuture) {
        if (modelInterfaceFuture == null) {
            return;
        }
        Thread monitorThread = new Thread(() -> {
            try {
                modelInterfaceFuture.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
                // Process completion (success/failure) is enough for refresh trigger.
            }
            Client.requestDatabaseSizeRefresh(false);
        }, "model-interface-completion-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    // --- Run control ---
    /**
     * Returns the GCAM run controller managing current and queued scenario executions.
     *
     * @return the active run controller
     */
    private GcamRunController getRunController() {
        return runController;
    }

    /**
     * Checks whether a GCAM scenario is currently running.
     *
     * @return true if GCAM is actively running a scenario, false otherwise
     */
    private boolean hasActiveGcamRun() {
        return runController.hasActiveRun();
    }

    /**
     * Creates and configures the live status refresh timeline if not already initialized.
     * The timeline periodically updates scenario status while GCAM is running.
     */
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
                    } catch (Throwable ex) {
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

    /**
     * Starts the live status refresh timeline when a GCAM run begins.
     */
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

    /**
     * Stops the live status refresh timeline when GCAM run completes or is stopped.
     */
    private void stopLiveStatusRefresh() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::stopLiveStatusRefresh);
            return;
        }
        if (liveStatusRefreshTimeline != null) {
            liveStatusRefreshTimeline.stop();
        }
    }

    /**
     * Prepares queued runs by confirming reuse, clearing prior logs, optionally switching to archive config, and then launches GCAM.
     */
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

    /**
     * Executes GCAM with the provided configuration files, queuing runs and monitoring for completion.
     *
     * @param configFiles array of configuration file paths to process
     */
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
            
            // Store the command for error reporting
            String commandStr = String.join(" ", command);
            submittedCommandByScenario.put(scenarioName, commandStr);
            
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
                                    maybePromptWindowsPolicyBlockOnStartupFailure(finishedScenarioName, result);
                                    reportRunFailureDetails(finishedScenarioName, result);
                                    markScenarioDnF(finishedScenarioName);
                                }
                            }
                            // Clean up the stored command
                            submittedCommandByScenario.remove(finishedScenarioName);
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

    /**
     * Shows user a dialog when Windows policy blocks GCAM startup for a scenario.
     * Only shows once per scenario per session via deduplication.
     *
     * @param scenarioName the scenario name where the startup was blocked
     * @param result process result containing error details
     */
    private void maybePromptWindowsPolicyBlockOnStartupFailure(String scenarioName, ProcessResult result) {
        if (!isWindowsPolicyBlockStartupFailure(result)) {
            return;
        }
        String normalizedScenarioName = scenarioName == null ? "" : scenarioName.trim();
        if (normalizedScenarioName.isEmpty()) {
            normalizedScenarioName = "(unknown scenario)";
        }
        if (windowsPolicyBlockPromptShownByScenario.putIfAbsent(normalizedScenarioName, Boolean.TRUE) != null) {
            return;
        }
        final String scenarioLabel = normalizedScenarioName;
        Platform.runLater(() -> {
            try {
                StringBuilder details = new StringBuilder();
                details.append("Windows blocked GCAM from launching for scenario '")
                        .append(scenarioLabel)
                        .append("'.")
                        .append(vars.getEol())
                        .append(vars.getEol())
                        .append("Detected message:")
                        .append(vars.getEol())
                        .append(" - CreateProcess error=4551 (Application Control policy block)")
                        .append(vars.getEol())
                        .append(vars.getEol())
                        .append("Suggested next steps:")
                        .append(vars.getEol())
                        .append(" 1) Ask IT/security to allow this GCAM executable path or signer.")
                        .append(vars.getEol())
                        .append(" 2) Run GCAM from a trusted/approved install location.")
                        .append(vars.getEol())
                        .append(" 3) Check policy logs (WDAC/AppLocker/Defender) for the block record.")
                        .append(vars.getEol())
                        .append(vars.getEol())
                        .append("The full startup error is available in the GCAM Console tab.");
                utils.showInformationDialog(
                        "GCAM blocked by Windows policy",
                        "GCAM launch was blocked by Application Control.",
                        details.toString());
            } catch (Exception ignored) {}
        });
    }

    /**
     * Detects whether a process failure was caused by Windows Application Control policy blocking GCAM.
     *
     * @param result the process result to analyze
     * @return true if the failure was due to Windows policy block, false otherwise
     */
    private boolean isWindowsPolicyBlockStartupFailure(ProcessResult result) {
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase(Locale.ENGLISH).startsWith("windows") || result == null) {
            return false;
        }
        String stderr = result.getStderr();
        if (stderr == null || stderr.trim().isEmpty()) {
            return false;
        }
        String normalized = stderr.toLowerCase(Locale.ENGLISH);
        if (!normalized.contains(STARTUP_FAILURE_PREFIX.toLowerCase(Locale.ENGLISH))) {
            return false;
        }
        return normalized.contains(WINDOWS_POLICY_BLOCK_ERROR_CODE)
                || normalized.contains(WINDOWS_POLICY_BLOCK_TEXT);
    }

    /**
     * Reports GCAM run failure details to the console, including the submitted command and exit code.
     * This helps users understand why GCAM failed to start or complete.
     *
     * @param scenarioName the scenario name that failed
     * @param result the process result containing exit code and error details
     */
    private void reportRunFailureDetails(String scenarioName, ProcessResult result) {
        try {
            if (result == null) {
                return;
            }
            
            StringBuilder failureReport = new StringBuilder();
            failureReport.append("=== GCAM RUN FAILURE DIAGNOSTICS ===").append(vars.getEol());
            failureReport.append("Scenario: ").append(scenarioName).append(vars.getEol());
            
            // Add the submitted command if available
            String submittedCommand = submittedCommandByScenario.get(scenarioName);
            if (submittedCommand != null && !submittedCommand.trim().isEmpty()) {
                failureReport.append("Command submitted to executor: ").append(vars.getEol());
                failureReport.append("  ").append(submittedCommand).append(vars.getEol());
            }
            
            // Add exit code information
            if (result.isTimedOut()) {
                failureReport.append("Status: Process timed out after ").append(result.getDurationMillis()).append(" ms").append(vars.getEol());
            } else {
                int exitCode = result.getExitCode();
                failureReport.append("Exit Code: ").append(exitCode).append(vars.getEol());
                
                // Add Windows-specific exit code interpretation
                String windowsInterpretation = interpretWindowsExitCode(exitCode);
                if (!windowsInterpretation.isEmpty()) {
                    failureReport.append("Windows Exit Code Interpretation: ").append(windowsInterpretation).append(vars.getEol());
                }
            }
            
            // Add stdout if available
            String stdout = result.getStdout();
            if (stdout != null && !stdout.trim().isEmpty()) {
                failureReport.append("Process stdout:").append(vars.getEol());
                String[] stdoutLines = stdout.split("\n");
                for (String line : stdoutLines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        failureReport.append("  ").append(trimmedLine).append(vars.getEol());
                    }
                }
            }
            
            // Add stderr if available
            String stderr = result.getStderr();
            if (stderr != null && !stderr.trim().isEmpty()) {
                failureReport.append("Process stderr:").append(vars.getEol());
                String[] errorLines = stderr.split("\n");
                for (String errorLine : errorLines) {
                    String trimmedLine = errorLine.trim();
                    if (!trimmedLine.isEmpty()) {
                        failureReport.append("  ").append(trimmedLine).append(vars.getEol());
                    }
                }
            }
            
            failureReport.append("=== END DIAGNOSTICS ===").append(vars.getEol());
            
            String report = failureReport.toString();
            
            // Log to console - try multiple times with different approaches
            try {
                ConsoleManager.appendLine(
                        ConsoleManager.StreamSource.GCAM_STDOUT,
                        ConsoleManager.MessageKind.STDERR,
                        report);
            } catch (Exception e) {
                System.err.println("Failed to append to ConsoleManager: " + e.getMessage());
            }
            
            // Always print to stderr for visibility
            System.err.println(report);
            
            // Try to append to a log file as well for persistence
            try {
                Path logDir = Paths.get(vars.getGlimpseLogDir(), "failures");
                if (!Files.exists(logDir)) {
                    Files.createDirectories(logDir);
                }
                Path logFile = logDir.resolve("gcam_failures.log");
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                String logEntry = "[" + timestamp + "] " + report + vars.getEol();
                Files.write(logFile, logEntry.getBytes(StandardCharsets.UTF_8), 
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                System.err.println("Could not write failure log: " + e.getMessage());
            }
        } catch (Exception ignored) {
            // Silently ignore any errors in failure reporting
            System.err.println("Error in reportRunFailureDetails: " + ignored.getMessage());
            ignored.printStackTrace();
        }
    }

    /**
     * Interprets common Windows process exit codes to provide helpful error messages.
     * Negative exit codes are typically Windows NT NTSTATUS error codes.
     *
     * @param exitCode the process exit code
     * @return human-readable interpretation of the exit code, or empty string if unknown
     */
    private String interpretWindowsExitCode(int exitCode) {
        switch (exitCode) {
            case -1073741515: // 0xC0000139 - STATUS_ENTRYPOINT_NOT_FOUND
                return "Missing DLL export or entrypoint not found - possibly a missing runtime library or dependency";
            case -1073741819: // 0xC0000005 - STATUS_ACCESS_VIOLATION
                return "Access violation - possibly a memory corruption or incompatible executable";
            case -1073741571: // 0xC0000263 - STATUS_DLL_NOT_FOUND
                return "Cannot find DLL - a required runtime library or dependency is missing";
            case -1073741670: // 0xC00000FE - STATUS_STACK_OVERFLOW
                return "Stack overflow - the program used too much stack memory";
            case -1: // Generic failure from GLIMPSE
                return "GLIMPSE failed to start the process (see above for details)";
            default:
                return "";
        }
    }

    /**
     * Stops the current GCAM run. "Stop All" also clears queued scenarios while leaving existing output untouched.
     */
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

    /**
     * Extracts the scenario name from a configuration file path.
     *
     * @param configFile the absolute path to the configuration file
     * @return the scenario name, or an empty string if not found
     */
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

    /**
     * Processes a single line of output from the running GCAM process.
     * Handles error parsing, interactive prompts, and success markers.
     *
     * @param scenarioName the running scenario name
     * @param line the output line
     * @param stderr true if line is from stderr, false if from stdout
     */
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

    /**
     * Prompts user when GCAM is blocked waiting for the output database to be released by ModelInterface.
     *
     * @param promptLine the prompt line from GCAM stdout
     */
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

    /**
     * Updates scenario status to "Success" when a GCAM stdout success marker is detected.
     * Clears any previously recorded live unsolved market errors for the scenario.
     *
     * @param line the stdout line to check for success markers
     */
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
    /**
     * Recomputes scenario run status for all rows and applies refreshed snapshots to the table.
     */
    public void updateRunStatus() {
        ScenarioStatusRefreshResult refreshResult = scenarioStatusService.refresh(buildScenarioStatusRefreshRequest());
        reconcileQueuedRunTracking(refreshResult);
        updateStatusBarComputerStats(refreshResult.getRunningScenario());
        Platform.runLater(() -> applyScenarioStatusSnapshots(refreshResult.getSnapshots(), refreshResult.isNoScenarios()));
    }

    /**
     * Updates the status bar with current computer resource statistics (CPU, memory, disk).
     *
     * @param runningScenario the name of the currently running scenario, if any
     */
    private void updateStatusBarComputerStats(String runningScenario) {
        refreshStatusBarComputerStats();
    }

    /**
     * Recomputes and applies the resource-stats status bar text.
     * Package-visible so {@link Client} can trigger a re-render after an async
     * database-size calculation finishes without performing a full scenario refresh.
     */
    void refreshStatusBarComputerStats() {
        if (resourceStatsUnavailable.get()) {
            Client.setStartupStatus(READY_MESSAGE, -1, false);
            return;
        }

        String statsText = "";
        try {
            statsText = utils.getComputerStatString();
        } catch (LinkageError linkageError) {
            resourceStatsUnavailable.set(true);
            System.out.println("Resource stats disabled after linkage failure: " + linkageError);
        } catch (Throwable ignored) {}

        String safeStatsText = statsText == null ? "" : statsText.trim();
        if (safeStatsText.isEmpty()) {
            safeStatsText = READY_MESSAGE;
        }
        Client.setStartupStatus(safeStatsText, -1, false);
    }

    /**
     * Finds the "Created" date column in the scenario library table.
     *
     * @param table the scenario table
     * @return the Created column, or null if not found
     */
    private TableColumn<ScenarioRow, ?> findCreatedColumn(TableView<ScenarioRow> table) {
        if (table == null || table.getColumns() == null) {
            return null;
        }
        for (TableColumn<ScenarioRow, ?> column : table.getColumns()) {
            if (column != null && "Created".equals(column.getText())) {
                return column;
            }
        }
        return null;
    }

    /**
     * Applies refreshed scenario status snapshots to the table and triggers view state restoration.
     *
     * @param snapshots list of updated scenario status snapshots
     * @param noScenarios true if no scenarios exist, false otherwise
     */
    private void applyScenarioStatusSnapshots(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios) {
        boolean refreshDbSizeAfterStatusTransition = hasDbSizeTriggerStatusTransition(snapshots);
        viewStateHelper.applySnapshots(
                snapshots,
                noScenarios,
                pendingRefreshViewState,
                NO_SCENARIOS_MESSAGE,
                READY_MESSAGE);
        applyLiveStdoutErrorPeriods();
        applyLiveRuntimeForActiveScenario();
        pendingRefreshViewState = ScenarioLibraryViewStateHelper.RefreshViewState.empty();
        applyDefaultCreatedSortAndScrollToTopIfRequested();
        refreshScenarioActionButtons();
        if (refreshDbSizeAfterStatusTransition) {
            // Strategic trigger: refresh DB-size cache when scenario outcomes move to
            // terminal states likely to change output-database footprint.
            Client.requestDatabaseSizeRefresh(false);
        }
    }

    /**
     * Requests that the next status refresh snaps to the default "newly-created scenario"
     * view by selecting and scrolling to the newest row (top of table).
     */
    public void requestDefaultCreatedSortAndScrollToTopOnNextRefresh() {
        resetToDefaultCreatedSortAndScroll.set(true);
    }

    /**
     * Applies the default created sort and scroll position if a refresh was requested.
     * Sorts by Created column (descending, newest first), selects the first row,
     * and scrolls to it (top of table where newest scenarios appear).
     */
    private void applyDefaultCreatedSortAndScrollToTopIfRequested() {
        if (!resetToDefaultCreatedSortAndScroll.compareAndSet(true, false)) {
            return;
        }
        if (ScenarioTable.tableScenariosLibrary == null || ScenarioTable.tableScenariosLibrary.getSelectionModel() == null) {
            return;
        }
        int rowCount = ScenarioTable.tableScenariosLibrary.getItems() == null
                ? 0
                : ScenarioTable.tableScenariosLibrary.getItems().size();
        if (rowCount <= 0) {
            return;
        }

        // Apply descending sort on the "Created" column to show newest scenarios first
        TableColumn<ScenarioRow, ?> createdColumn = findCreatedColumn(ScenarioTable.tableScenariosLibrary);
        if (createdColumn != null) {
            createdColumn.setSortType(TableColumn.SortType.DESCENDING);
            ScenarioTable.tableScenariosLibrary.getSortOrder().setAll(createdColumn);
            ScenarioTable.tableScenariosLibrary.sort();
        }

        int firstIndex = 0;
        ScenarioTable.tableScenariosLibrary.getSelectionModel().clearSelection();
        ScenarioTable.tableScenariosLibrary.getSelectionModel().select(firstIndex);
        if (ScenarioTable.tableScenariosLibrary.getFocusModel() != null) {
            ScenarioTable.tableScenariosLibrary.getFocusModel().focus(firstIndex);
        }
        ScenarioTable.tableScenariosLibrary.scrollTo(firstIndex);
    }

    /**
     * Checks if any scenario status changed to a database-size-trigger state
     * (Success, Unsolved Mkts, DNF, etc.).
     *
     * @param snapshots the list of scenario status snapshots
     * @return true if any scenario transitioned to a DB-size-trigger status, false otherwise
     */
    private boolean hasDbSizeTriggerStatusTransition(List<ScenarioStatusSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return false;
        }
        Map<String, String> priorStatusByScenario = new HashMap<>();
        for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
            if (row == null) {
                continue;
            }
            String scenarioName = row.getScenarioName() == null ? "" : row.getScenarioName().trim();
            if (scenarioName.isEmpty()) {
                continue;
            }
            priorStatusByScenario.put(scenarioName, normalizeStatusForCompare(row.getStatus()));
        }

        for (ScenarioStatusSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            String scenarioName = snapshot.scenarioName == null ? "" : snapshot.scenarioName.trim();
            if (scenarioName.isEmpty()) {
                continue;
            }
            String nextStatus = normalizeStatusForCompare(snapshot.status);
            if (!isDbSizeTriggerStatus(nextStatus)) {
                continue;
            }
            String priorStatus = priorStatusByScenario.getOrDefault(scenarioName, "");
            if (!nextStatus.equals(priorStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a status text represents a terminal state that may change database footprint.
     *
     * @param statusText the status text to check
     * @return true if status is a database-size-trigger status, false otherwise
     */
    private boolean isDbSizeTriggerStatus(String statusText) {
        if (statusText == null || statusText.trim().isEmpty()) {
            return false;
        }
        String normalized = statusText.trim().toLowerCase(Locale.ENGLISH);
        return normalized.equals("success")
                || normalized.equals("unsolved mkts")
                || normalized.contains("unsolved")
                || normalized.contains("problemmkts")
                || normalized.contains("problem mkts");
    }

    /**
     * Normalizes status text for case-insensitive comparison.
     *
     * @param statusText the status text to normalize
     * @return lowercase trimmed status text, or empty string if null
     */
    private String normalizeStatusForCompare(String statusText) {
        return statusText == null ? "" : statusText.trim().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Applies live unsolved market error periods to the table for currently running scenarios.
     */
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

    /**
     * Checks whether a stdout line is a live error line (starts with ERROR prefix).
     *
     * @param line the line to check
     * @return true if line is a live error, false otherwise
     */
    private boolean isLiveStdoutErrorLine(String line) {
        if (line == null) {
            return false;
        }
        return line.trim().startsWith(LIVE_STDOUT_ERROR_PREFIX);
    }

    /**
     * Extracts unsolved market period numbers from a live error line using regex pattern matching.
     *
     * @param line the error line to parse
     * @return a LinkedHashSet of period numbers found in the line
     */
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

    /**
     * Formats a set of period numbers into a comma-separated string.
     *
     * @param periods the set of period numbers to format
     * @return formatted period string or empty string if no periods
     */
    private String formatLiveErrorPeriods(java.util.Set<String> periods) {
        if (periods == null || periods.isEmpty()) {
            return "";
        }
        return String.join(",", periods);
    }

    /**
     * Records unsolved market periods from a live GCAM stdout error line for a specific scenario.
     *
     * @param scenarioName the scenario name
     * @param line the error line containing period information
     */
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

    /**
     * Clears recorded unsolved market periods for a specific scenario.
     *
     * @param scenarioName the scenario to clear error records for
     */
    private void clearLiveStdoutError(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        liveStdoutErrorPeriodsByScenario.remove(scenarioName.trim());
    }

    /**
     * Applies live runtime updates to the currently running scenario's table row.
     */
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

    /**
     * Triggers an asynchronous scenario status refresh.
     */
    public void clearAndRefreshScenarioTable() {
        refreshScenarioStatusAsync(true);
    }

    /**
     * Captures table view state before refresh and restores it after refreshed snapshots are applied.
     *
     * @param userInitiated true if refresh was initiated by user, false for automatic refresh
     */
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
                        if (userInitiated) {
                            // Non-startup refresh completion is a key moment to update resource stats.
                            Client.requestDatabaseSizeRefresh(false);
                        }
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

    /**
     * Captures the current view state (selection, sort order, scroll position) before a refresh.
     *
     * @return the captured refresh view state
     */
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

    /**
     * Builds a status refresh request with current execution state and queued scenarios.
     *
     * @return a new refresh request with current run state
     */
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

    /**
     * Reconciles the internal run queue tracking with the refresh result.
     *
     * @param refreshResult the status refresh result containing queue updates
     */
    private void reconcileQueuedRunTracking(ScenarioStatusRefreshResult refreshResult) {
        if (refreshResult == null) {
            return;
        }
        runController.replaceQueuedRuns(refreshResult.getQueuedRuns());
    }

    /**
     * Marks a scenario as stopped (for manual stop) and updates the table row.
     *
     * @param scenarioName the scenario to mark as stopped
     */
    private void markScenarioStoppedDnF(String scenarioName) {
        markScenarioStopped(scenarioName);
    }

    /**
     * Marks a scenario as stopped and updates its terminal status.
     *
     * @param scenarioName the scenario to mark as stopped
     */
    private void markScenarioStopped(String scenarioName) {
        clearLiveStdoutError(scenarioName);
        updateScenarioTerminalStatus(scenarioName, ScenarioStatusService.STATUS_STOPPED);
    }

    /**
     * Marks a scenario as Did Not Finish (DNF) with appropriate error status.
     *
     * @param scenarioName the scenario to mark as DNF
     */
    private void markScenarioDnF(String scenarioName) {
        clearLiveStdoutError(scenarioName);
        updateScenarioTerminalStatus(scenarioName, ScenarioStatusService.STATUS_DNF);
    }

    /**
     * Updates a scenario's terminal status (e.g., "Success", "DNF", "Stopped") and completion date.
     *
     * @param scenarioName the scenario to update
     * @param statusText the new status text
     */
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

    /**
     * Persists a "stopped" status marker to the scenario's main_log.txt file.
     *
     * @param scenarioName the scenario to mark as stopped in logs
     */
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

    /**
     * Moves the executable's main_log.txt to the scenario's folder after a manual stop.
     *
     * @param scenarioName the scenario folder to move the log to
     */
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

    /**
     * Copies run artifact files (logs, errors, output) from the executable folder to the scenario folder.
     *
     * @param scenarioName the scenario folder to copy artifacts to
     */
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
    /**
     * Generates and displays a comprehensive scenario execution report.
     */
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

    /**
     * Generates and displays errors found in the GCAM executable log folder.
     */
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

    /**
     * Generates and displays errors found in the selected scenario's main_log.txt file.
     */
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
    /**
     * Checks if a text line looks like a GCAM database save prompt.
     *
     * @param line the line to check
     * @return true if line looks like a database save prompt, false otherwise
     */
    private static boolean looksLikeDatabaseSavePrompt(String line) {
        return ScenarioLibraryPromptHelper.looksLikeDatabaseSavePrompt(line);
    }

    /**
     * Normalizes a database prompt text by removing common formatting characters.
     *
     * @param text the prompt text to normalize
     * @return the normalized prompt text
     */
    private static String normalizeDatabasePromptText(String text) {
        return ScenarioLibraryPromptHelper.normalizeDatabasePromptText(text);
    }

    /**
     * Captures the names of all currently selected scenarios.
     *
     * @return a list of selected scenario names
     */
    private List<String> captureSelectedScenarioNames() {
        return new ArrayList<>(ScenarioSelection.capture().getScenarioNames());
    }

    /**
     * Checks if the ModelInterface location has been configured in the options file.
     *
     * @return true if ModelInterface directory is configured, false otherwise
     */
    private boolean hasModelInterfaceLocationConfigured() {
        String modelInterfaceDir = vars.getModelInterfaceDir();
        return modelInterfaceDir != null && !modelInterfaceDir.trim().isEmpty();
    }

    /**
     * Returns the name of the scenario that was requested to be stopped.
     *
     * @return the scenario name, or null if no stop was requested
     */
    private String getStopRequestedScenarioName() {
        return runController.getStopRequestedScenarioName();
    }

    /**
     * Clears transient run state for scenarios that were deleted.
     *
     * @param selection the scenarios being deleted
     */
    private void clearDeletedScenarioRunState(ScenarioSelection selection) {
        clearScenarioTransientRunState(selection, ScenarioRunStateClearMode.DELETE);
    }

    /**
     * Dequeues scenarios from the run queue and clears their "In queue" status.
     *
     * @param scenariosToDequeue the scenarios to dequeue
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
            runController.removeQueuedRun(scenName);
            if ("In queue".equals(row.getStatus())) {
                row.setStatus("");
            }
        }
    }

    /**
     * Clears volatile run-status fields for a scenario before queueing a new run.
     *
     * @param scenarioName scenario to reset
     */
    void clearScenarioRunStatusFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.PREPARE_RUN);
    }

    /**
     * Clears terminal run-result fields when recreating an existing scenario.
     *
     * @param scenarioName scenario to reset
     */
    void clearScenarioRunResultFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.RECREATE_OVERWRITE);
    }

    /**
     * Clears terminal run-result fields when importing/overwriting an existing scenario.
     *
     * @param scenarioName scenario to reset
     */
    private void clearImportedScenarioRunResultFields(String scenarioName) {
        clearScenarioTransientRunState(scenarioName, ScenarioRunStateClearMode.IMPORT_OVERWRITE);
    }

    /**
     * Clears transient run state for all scenarios in the selection.
     *
     * @param selection the scenarios to clear
     * @param mode the type of clear operation (prepare run, delete, etc.)
     */
    private void clearScenarioTransientRunState(ScenarioSelection selection, ScenarioRunStateClearMode mode) {
        if (selection == null) {
            return;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            clearScenarioTransientRunState(scenarioName, mode);
        }
    }

    /**
     * Clears transient run state for a specific scenario.
     *
     * @param scenarioName scenario to clear
     * @param mode the type of clear operation (prepare run, delete, etc.)
     */
    private void clearScenarioTransientRunState(String scenarioName, ScenarioRunStateClearMode mode) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        ScenarioRunStateClearMode effectiveMode = mode == null ? ScenarioRunStateClearMode.IMPORT_OVERWRITE : mode;
        runController.clearStoppedScenario(scenarioName);
        clearLiveStdoutError(scenarioName);
        liveSuccessMarkedByScenario.remove(scenarioName.trim());
        windowsPolicyBlockPromptShownByScenario.remove(scenarioName.trim());
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

    /**
     * Returns the root HBox containing the Scenario Library table.
     *
     * @return pane root container
     */
    public HBox gethBox() {
        return scenarioLibraryHBox;
    }

    /**
     * Handles a table refresh while preserving the current selection and applying an optional update action.
     *
     * @param updateAction optional action to run during the refresh
     */
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

    /**
     * Checks if two text strings are equal after null-safe comparison.
     *
     * @param left first string (may be null)
     * @param right second string (may be null)
     * @return true if both strings are equal or both are null, false otherwise
     */
    private static boolean sameText(String left, String right) {
        String normalizedLeft = left == null ? "" : left;
        String normalizedRight = right == null ? "" : right;
        return normalizedLeft.equals(normalizedRight);
    }

    /**
     * Updates a scenario row in-place using a provided predicate update function.
     * Marshals the update to the JavaFX thread if needed.
     *
     * @param scenarioName the scenario to find and update
     * @param rowUpdate predicate that updates the row and returns true if changed
     */
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

    /**
     * Formats elapsed time between start and current time into a human-readable runtime string.
     *
     * @param runStartTimeMillis the start time in milliseconds
     * @param currentTimeMillis the current time in milliseconds
     * @return formatted runtime string (e.g., "> 2 hr 30 min 45 sec") or empty string if invalid
     */
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

    /**
     * Shows a lightweight Swing "Starting ModelInterface..." toast and auto-dismisses it
     * after {@code dismissAfterMillis} milliseconds. Uses the same {@code JWindow} pattern
     * as the ScenarioBuilder startup splash so it appears immediately on the Swing EDT,
     * before the ModelInterface JVM has had time to paint its own window.
     *
     * @param dismissAfterMillis milliseconds before the window auto-closes (e.g. 5000)
     */
    private static void showModelInterfaceLaunchingToast(long dismissAfterMillis) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.JWindow window = new javax.swing.JWindow();
                window.setAlwaysOnTop(true);

                javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
                content.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

                javax.swing.JLabel heading = new javax.swing.JLabel("ModelInterface");
                heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
                javax.swing.JLabel msg = new javax.swing.JLabel("Starting ModelInterface, please wait...");
                javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
                bar.setIndeterminate(true);
                bar.setStringPainted(false);

                javax.swing.JPanel textPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 4));
                textPanel.add(heading);
                textPanel.add(msg);
                content.add(textPanel, java.awt.BorderLayout.CENTER);
                content.add(bar, java.awt.BorderLayout.SOUTH);

                window.setContentPane(content);
                window.setSize(430, 110);
                window.setLocationRelativeTo(null);
                window.setVisible(true);

                final long safeDelay = Math.max(1000, dismissAfterMillis);
                Thread dismissThread = new Thread(() -> {
                    try {
                        Thread.sleep(safeDelay);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            window.setVisible(false);
                            window.dispose();
                        } catch (Throwable ignored) {}
                    });
                }, "mi-launch-toast");
                dismissThread.setDaemon(true);
                dismissThread.start();
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Logs a button build step to the startup checkpoint system.
     *
     * @param label the description of the build step
     */
    private static void logButtonBuildStep(String label) {
        Client.logStartupBuildCheckpoint("PaneScenarioLibrary: " + label);
    }

    /**
     * Creates a scenario library action button with timing telemetry.
     *
     * @param label the button ID label
     * @param text the button display text
     * @param width the button width
     * @param tooltip the tooltip text
     * @param iconKey the icon key from the styles system
     * @return the configured button
     */
    private javafx.scene.control.Button createTimedScenarioButton(String label, String text, double width, String tooltip, String iconKey) {
        logButtonBuildStep("createScenarioLibraryButtonInstances: " + label + " start");
        javafx.scene.control.Button button = utils.createButton(text, (int) width, tooltip, iconKey);
        logButtonBuildStep("createScenarioLibraryButtonInstances: " + label + " complete");
        return button;
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
    /**
     * Builds detailed diagnostic information when a GCAM run fails.
     * Checks for common issues like missing config files, GCAM executable, and invalid paths.
     *
     * @param ex the exception that occurred during run initialization
     * @return formatted diagnostic string suitable for display to the user
     */
    private String buildRunFailureDiagnostics(Exception ex) {
        StringBuilder diagnostics = new StringBuilder();
        
        // Add exception message
        diagnostics.append("Error details:").append(vars.getEol());
        if (ex != null && ex.getMessage() != null) {
            diagnostics.append("  ").append(ex.getMessage()).append(vars.getEol());
        }
        diagnostics.append(vars.getEol());
        
        // Check GCAM executable
        diagnostics.append("Configuration checks:").append(vars.getEol());
        try {
            String gcamExeDir = vars.getgCamExecutableDir();
            if (gcamExeDir == null || gcamExeDir.trim().isEmpty()) {
                diagnostics.append("  ✗ GCAM executable directory not configured").append(vars.getEol());
            } else {
                File exeDir = new File(gcamExeDir);
                if (!exeDir.exists()) {
                    diagnostics.append("  ✗ GCAM executable directory does not exist:").append(vars.getEol());
                    diagnostics.append("    ").append(gcamExeDir).append(vars.getEol());
                } else {
                    diagnostics.append("  ✓ GCAM directory found: ").append(gcamExeDir).append(vars.getEol());
                }
                
                String gcamExe = vars.getgCamExecutable();
                if (gcamExe != null && !gcamExe.trim().isEmpty()) {
                    String fullExePath = files.getResolvedPath(gcamExeDir, gcamExe);
                    File exeFile = new File(fullExePath);
                    if (!exeFile.exists()) {
                        diagnostics.append("  ✗ GCAM executable not found:").append(vars.getEol());
                        diagnostics.append("    ").append(fullExePath).append(vars.getEol());
                    } else {
                        diagnostics.append("  ✓ GCAM executable found").append(vars.getEol());
                    }
                } else {
                    diagnostics.append("  ✗ GCAM executable name not configured").append(vars.getEol());
                }
            }
        } catch (Exception e) {
            diagnostics.append("  ? Could not check GCAM executable: ").append(e.getMessage()).append(vars.getEol());
        }
        
        // Check scenario directory
        try {
            String scenarioDir = vars.getScenarioDir();
            if (scenarioDir == null || scenarioDir.trim().isEmpty()) {
                diagnostics.append("  ✗ Scenario directory not configured").append(vars.getEol());
            } else {
                File scenDir = new File(scenarioDir);
                if (!scenDir.exists()) {
                    diagnostics.append("  ✗ Scenario directory does not exist:").append(vars.getEol());
                    diagnostics.append("    ").append(scenarioDir).append(vars.getEol());
                } else {
                    diagnostics.append("  ✓ Scenario directory found").append(vars.getEol());
                }
            }
        } catch (Exception e) {
            diagnostics.append("  ? Could not check scenario directory: ").append(e.getMessage()).append(vars.getEol());
        }
        
        // Check selected scenarios and their config files
        try {
            ScenarioSelection selection = ScenarioSelection.capture();
            int selectedCount = selection.getRows().size();
            diagnostics.append(vars.getEol()).append("Selected scenarios (").append(selectedCount).append("):").append(vars.getEol());
            
            for (ScenarioRow row : selection.getRows()) {
                String scenarioName = ScenarioSelection.normalizeScenarioName(row);
                if (scenarioName.isEmpty()) {
                    diagnostics.append("  ✗ Could not get scenario name from selected row").append(vars.getEol());
                } else {
                    String configFile = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
                    File configFileObj = new File(configFile);
                    if (!configFileObj.exists()) {
                        diagnostics.append("  ✗ Config missing for '").append(scenarioName).append("':").append(vars.getEol());
                        diagnostics.append("    ").append(configFile).append(vars.getEol());
                    } else {
                        diagnostics.append("  ✓ Config found for '").append(scenarioName).append("'").append(vars.getEol());
                    }
                }
            }
            
            if (selectedCount == 0) {
                diagnostics.append("  ! No scenarios selected").append(vars.getEol());
            }
        } catch (Exception e) {
            diagnostics.append("  ? Could not check selected scenarios: ").append(e.getMessage()).append(vars.getEol());
        }
        
        // Check GCAM execution thread
        diagnostics.append(vars.getEol()).append("Runtime state:").append(vars.getEol());
        if (Client.gCAMExecutionThread == null) {
            diagnostics.append("  ✗ GCAM execution thread not initialized").append(vars.getEol());
        } else {
            diagnostics.append("  ✓ GCAM execution thread available").append(vars.getEol());
        }
        
        // Add suggestion
        diagnostics.append(vars.getEol()).append("Suggested actions:").append(vars.getEol());
        diagnostics.append("  1. Check that all GCAM paths are configured correctly").append(vars.getEol());
        diagnostics.append("  2. Verify that at least one scenario is selected").append(vars.getEol());
        diagnostics.append("  3. Verify that scenario configuration files exist").append(vars.getEol());
        diagnostics.append("  4. Check the console output for additional error details").append(vars.getEol());
        
        return diagnostics.toString();
    }
}
