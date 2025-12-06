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
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

//import ModelInterface.InterfaceMain;
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
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
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

    // Constants for UI labels and tooltips
    private static final String DIFF_LABEL = "Diff";
    private static final String DIFF_TOOLTIP = "Diff: Compare first two selected configurations";
    private static final String REFRESH_LABEL = "Refresh";
    private static final String REFRESH_TOOLTIP = "Refresh: Update scenario completion status";
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
    private long startupTime = 0;
    private final HBox scenarioLibraryHBox = new HBox(1);

    /**
     * Constructs the scenario library pane, sets up UI controls, event handlers, and initializes the scenario table.
     * Binds table size to the main application stage and triggers initial status update.
     *
     * @param stage the main application stage for binding UI components
     */
    PaneScenarioLibrary(Stage stage) {
        scenarioLibraryHBox.setStyle(styles.getFontStyle()); // Set font style for the HBox
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
        Client.buttonRefreshScenarioStatus = utils.createButton(REFRESH_LABEL, styles.getBigButtonWidth(), REFRESH_TOOLTIP, "refresh");
        Client.buttonResults = utils.createButton(RESULTS_LABEL, styles.getBigButtonWidth(), RESULTS_TOOLTIP, "results");
        Client.buttonResultsForSelected = utils.createButton(RESULTS_SELECTED_LABEL, styles.getBigButtonWidth(), RESULTS_SELECTED_TOOLTIP, "results-selected");
        Client.buttonRunScenario = utils.createButton(PLAY_LABEL, styles.getBigButtonWidth(), PLAY_TOOLTIP, "run");
        Client.buttonDeleteScenario = utils.createButton(DELETE_LABEL, styles.getBigButtonWidth(), DELETE_TOOLTIP, "delete");
        Client.buttonViewConfig = utils.createButton(CONFIG_LABEL, styles.getBigButtonWidth(), CONFIG_TOOLTIP, "edit");
        Client.buttonViewLog = utils.createButton(LOG_LABEL, styles.getBigButtonWidth(), LOG_TOOLTIP, "log2");
        Client.buttonViewExeErrors = utils.createButton(EXE_ERRORS_LABEL, styles.getBigButtonWidth(), EXE_ERRORS_TOOLTIP, "exe-errors");
        Client.buttonViewErrors = utils.createButton(ERRORS_LABEL, styles.getBigButtonWidth(), ERRORS_TOOLTIP, "errors");
        Client.buttonViewExeLog = utils.createButton(EXE_LOG_LABEL, styles.getBigButtonWidth(), EXE_LOG_TOOLTIP, "exe-log");
        Client.buttonBrowseScenarioFolder = utils.createButton(BROWSE_LABEL, styles.getBigButtonWidth(), BROWSE_TOOLTIP, "open_folder");
        Client.buttonImportScenario = utils.createButton(IMPORT_LABEL, styles.getBigButtonWidth(), IMPORT_TOOLTIP, "import");
        Client.buttonShowRunQueue = utils.createButton(QUEUE_LABEL, styles.getBigButtonWidth(), QUEUE_TOOLTIP, "queue");
        Client.buttonArchiveScenario = utils.createButton(ARCHIVE_LABEL, styles.getBigButtonWidth(), ARCHIVE_TOOLTIP, "archive");
        Client.buttonReport = utils.createButton(REPORT_LABEL, styles.getBigButtonWidth(), REPORT_TOOLTIP, "report");

        // Set initial button status
        Client.buttonRunScenario.setDisable(true); // Disabled until a scenario is selected
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
        Client.buttonReport.setVisible(true);
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
        if (vars.getModelInterfaceDir().isEmpty()) {
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
        if (vars.getModelInterfaceDir().isEmpty()) {
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
        if (selectedFiles.size() == 2) {
            String sName1 = selectedFiles.get(0).getScenarioName();
            String sName2 = selectedFiles.get(1).getScenarioName();
            String file1 = vars.getScenarioDir() + File.separator + sName1 + File.separator + "configuration_" + sName1 + ".xml";
            String file2 = vars.getScenarioDir() + File.separator + sName2 + File.separator + "configuration_" + sName2 + ".xml";
            utils.diffTwoFiles(file1, file2);
        }
    }

    /**
     * Displays the current run queue in a popup window.
     * Shows scenarios added to the queue and completed runs for the session.
     */
    private void handleShowRunQueue() {
        ArrayList<String> txtArray = createSimpleQueueRpt();
        utils.displayArrayList(txtArray, "Run Queue");
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
                        if ((line.equals(configName)) || (line.equals(scenarioName))) {
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
                        if (lastDate < startupTime) {
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
                            if ((line.equals(configName)) || (line.equals(scenarioName))) {
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
                        s.setStatus(status);
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
                    sr.setStatus(status);
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
                files.deleteFile(mainLogFile);
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
                        if (utils.selectYesOrNoDialog(s))
                            configFiles[idx] = archiveConfigFilename;
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
        ArrayList<String> cmdList = new ArrayList<String>();
        for (String scenarioConfigFile : scenarioConfigFiles) {
            if (scenarioConfigFile != null) {
                final String dir = scenarioConfigFile.substring(0, scenarioConfigFile.lastIndexOf(File.separator)).replaceAll("/", File.separator);
                System.out.println("config: " + scenarioConfigFile);
                this.runsQueuedList.add(scenarioConfigFile);
                Client.gCAMExecutionThread.executeCallableCmd(new Callable<String>() {
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
                });
                boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
                String cmdStr = isWindows
                    ? "cmd.exe /C start ./" + vars.getgCamExecutable() + " " + vars.getgCamExecutableArgs() + " " + scenarioConfigFile
                    : "xterm -e " + vars.getgCamExecutableDir() + File.separator + vars.getgCamExecutable() + " " + vars.getgCamExecutableArgs() + " " + scenarioConfigFile ;
                Future f = Client.gCAMExecutionThread.submitCommandWithDirectory(cmdStr, vars.getgCamExecutableDir());
                 
                Client.gCAMExecutionThread.executeCallableCmd(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
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
                                    int count = 0;
                                    try {
                                        while ((!f.isDone()) && (count < 10000)) {
                                            Thread.sleep(10000);
                                            count++;
                                        }
                                        f.cancel(true);
                                        files.moveFile(source, destination);
                                    } catch (Exception e1) {
                                        System.out.println("Problem moving file " + fileToSave);
                                        System.out.println("Exception " + e1);
                                    }
                                    File destf = new File(destinationStr);
                                    if (!destf.exists()) {
                                        System.out.println("Problem moving file " + fileToSave);
                                    }
                                    if (file.exists())
                                        files.deleteFile(file);
                                    updateRunStatus();
                                } else {
                                    System.out.println("Unable to save " + fileToSave);
                                }
                            }
                        }
                        return "moving specified files to scenario folder";
                    }
                });
            }
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
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        String shell = isWindows ? "cmd.exe /C start" : "xterm -e";//"/bin/sh -c";
        String[] cmd = new String[1];
        String command = shell +" java -jar ./"
                + vars.getModelInterfaceJar() + " -o "
                + database_name;
        //specifying query file
        String temp = vars.getQueryFilename();
        if ((temp != null) && (temp != ""))
            command += " -q " + temp;
        //specifying unit conversions file
        temp = vars.getUnitConversionsFilename();
        if ((temp != null) && (temp != ""))
            command += " -u " + temp;
        //specifying preset region list file
        temp = vars.getPresetRegionListFilename();
        if ((temp != null) && (temp != ""))
            command += " -p " + temp;
        //specifying favorite query file
        temp = vars.getFavoriteQueryFilename();
        if ((temp != null) && (temp != ""))
            command += " -f " + temp;
        //
        temp = vars.getModelInterfaceDir() + File.separator + "map_resources";
        if ((temp != null) && (temp != ""))
            command += " -m " + temp;

        System.out
                .println("Starting " + vars.getModelInterfaceJar() + " using database " + vars.getgCamOutputDatabase());
        System.out.println(">>   cmd:" + command);
        try {
            Client.modelInterfaceExecutionThread.submitCommandWithDirectory(command,vars.getModelInterfaceDir());
        } catch (Exception e) {
            utils.warningMessage("Problem starting up ModelInterface.");
            System.out.println("Error in trying to start up ModelInterface:");
            System.out.println(e);
        }
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
                String scenarioMainLog = vars.getgCamExecutableDir() + File.separator + "logs" + File.separator + "main_log.txt";
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

}
