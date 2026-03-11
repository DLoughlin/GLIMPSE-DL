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
* 
*/
package gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;

import glimpseBuilder.XMLModifier;
import glimpseElement.ComponentRow;
import glimpseElement.ComponentLibraryTable;
import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseUtil.CSVToXMLMain;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Pane for creating a scenario in the GLIMPSE Scenario Builder.
 * <p>
 * This class provides the user interface and logic for constructing new scenarios
 * in the GLIMPSE Scenario Builder application. It allows users to specify scenario
 * names, select and order scenario components, configure scenario options, and
 * generate scenario configuration files. The pane includes controls for moving
 * components up and down, entering scenario metadata, and setting advanced options
 * such as debug output, processor usage, and files to save. The scenario creation
 * process includes validation, file management, and XML configuration generation.
 * </p>
 * <p>
 * <b>Main Features:</b>
 * <ul>
 *   <li>Scenario name entry and validation (no special characters allowed)</li>
 *   <li>Component selection and ordering (move up/down)</li>
 *   <li>Scenario configuration dialog for metadata and advanced options</li>
 *   <li>Support for CSV, XML, and list-based scenario components</li>
 *   <li>Automatic creation and population of scenario configuration XML files</li>
 *   <li>Database size checking and warning</li>
 *   <li>Debug and parallelism options</li>
 *   <li>File management for scenario folders and logs</li>
 * </ul>
 * </p>
 * <p>
 * <b>Usage:</b> Instantiate this pane and add it to a JavaFX stage. The pane will
 * handle all user interactions for scenario creation.
 * </p>
 * <p>
 * <b>Thread Safety:</b> All UI operations must be performed on the JavaFX Application Thread.
 * </p>
 */
class PaneCreateScenario extends ScenarioBuilder {
    // UI Constants
    private static final String LABEL_NAME = "Name:";
    private static final String LABEL_COMPONENTS = "Components: ";
    private static final String LABEL_CREATE_SCENARIO = "Create Scenario";
    private static final String TOOLTIP_SCENARIO_NAME = "Enter name of scenario being constructed";
    private static final String BUTTON_CREATE = "Create";
    private static final String BUTTON_ICON_CREATE = "create";
    private static final String BUTTON_MOVE_UP = "Move selected item up in list";
    private static final String BUTTON_MOVE_DOWN = "Move selected item down in list";
    private static final String BUTTON_ICON_UP = "move_up";
    private static final String BUTTON_ICON_DOWN = "move_down";
    private static final String BUTTON_ICON_ADD = "add2";
    private static final String WARNING_INVALID_NAME = "Please specify a name for the scenario. The name should not include any of these special characters: [! @#$%&*()+=|<>?{}[]~]\\//";
    private static final String DIALOG_TITLE_CREATE = "Creating Scenario";
    private static final int DIALOG_HEIGHT = 550;
    // Make the Creating Scenario dialog about 50% wider than before
    private static final int DIALOG_WIDTH = 500;
    private static final String META_DATA_HEADER = "##################### Scenario Meta Data #####################";
    private static final String META_DATA_SEPARATOR = "###############################################################";
    private static final String COMPONENTS_HEADER = "Components:";
    private static final String SCENARIO_OVERWRITE_PROMPT = "Overwrite scenario ";
    private static final String DATABASE_SIZE_WARNING = "Database size is dangerously high. See User's Manual for instructions. Continue?";
    private static final String ERROR_CREATE_DIR = "Difficulty creating directory for xml code.";
    private static final String ERROR_CSV_TO_XML = "Error converting %s using CSV->XML. Please check formatting.";
    private static final String ERROR_HEADER_NOT_FOUND = "Could not find header in header file";
    private static final String ERROR_UNSUPPORTED_COMPONENT = "Only CSV-type and XML-list-type scenario components are currently supported.";
    private static final String ERROR_PROCESS_COMPONENT = "Unable to process scenario component ";

    // ComboBox options
    //private static final String[] DEFAULT_STOP_YEARS = {"2020", "2025", "2030", "2035", "2040", "2045", "2050", "2055", "2060", "2065", "2070", "2075", "2080", "2085", "2090", "2095", "2100"};
    //private static final String[] DEFAULT_DEBUG_REGIONS_USA = {"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "USA", "Canada", "EU-15", "Europe_Non_EU", "European Free Trade Association", "Japan", "Australia_NZ", "Central Asia", "Russia", "China", "Middle East", "Africa_Eastern", "Africa_Northern", "Africa_Southern", "Africa_Western", "South Africa", "Brazil", "Central America and Caribbean", "Mexico", "South America_Northern", "South America_Southern", "Argentina", "Colombia", "Indonesia", "Pakistan", "South Asia", "Southeast Asia", "Taiwan", "Europe_Eastern", "EU-12", "South Korea", "India"};
    //private static final String[] DEFAULT_DEBUG_REGIONS_GLOBAL = {"USA", "Canada", "EU-15", "Europe_Non_EU", "European Free Trade Association", "Japan", "Australia_NZ", "Central Asia", "Russia", "China", "Middle East", "Africa_Eastern", "Africa_Northern", "Africa_Southern", "Africa_Western", "South Africa", "Brazil", "Central America and Caribbean", "Mexico", "South America_Northern", "South America_Southern", "Argentina", "Colombia", "Indonesia", "Pakistan", "South Asia", "Southeast Asia", "Taiwan", "Europe_Eastern", "EU-12", "South Korea", "India"};

    private VBox vBox;
    private TextField textFieldScenarioName;

    // Local header label is no longer needed; ScenarioBuilder owns the title row.
    // private Label labelScenarioName;

    private Label labelName;
    private HBox nameRow;
    private HBox buttonRow;

    /**
     * Constructs the scenario creation pane and initializes all UI components.
     * Sets up the scenario name field, component table, and action buttons.
     *
     * @param stage The JavaFX stage to which this pane is bound.
     */
    PaneCreateScenario(Stage stage) {
        vBox = new VBox(1);
        textFieldScenarioName = utils.createTextField(2.5 * styles.getBigButtonWidth());
        textFieldScenarioName.setPromptText("Enter new scenario name...");
        textFieldScenarioName.setTooltip(new Tooltip(TOOLTIP_SCENARIO_NAME));

        labelName = utils.createLabel(LABEL_NAME);
        labelName.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        nameRow = new HBox(4);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.getChildren().addAll(labelName, textFieldScenarioName);
        textFieldScenarioName.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textFieldScenarioName, javafx.scene.layout.Priority.ALWAYS);

    	// Add a small visual buffer before the table.
    	VBox.setMargin(nameRow, new Insets(0, 0, 4, 0));

        buttonRow = new HBox();
        // Use centralized top padding 5px
        buttonRow.setPadding(styles.getTopPadding5());
        buttonRow.setAlignment(Pos.CENTER);

        setupButtons();
        buttonRow.getChildren().addAll(Client.buttonCreateScenarioConfigFile, utils.getSeparator(Orientation.VERTICAL, 2, false), Client.buttonMoveComponentUp, utils.getSeparator(Orientation.VERTICAL, 2, false), Client.buttonMoveComponentDown);

        // Set up main layout and bind width to stage
        vBox.getChildren().addAll(nameRow, ComponentLibraryTable.getTableCreateScenario(), buttonRow);
        vBox.prefWidthProperty().bind(stage.widthProperty().multiply(2.5 / 7.0));
    }

    /**
     * Sets up the main action buttons (Create, Move Up, Move Down) and their event handlers.
     * Handles enabling/disabling buttons based on selection and scenario name validity.
     */
    private void setupButtons() {
        Client.buttonMoveComponentUp = utils.createButton(null, styles.getSmallButtonWidth(), BUTTON_MOVE_UP, BUTTON_ICON_UP);
        Client.buttonMoveComponentDown = utils.createButton(null, styles.getSmallButtonWidth(), BUTTON_MOVE_DOWN, BUTTON_ICON_DOWN);
        Client.buttonCreateScenarioConfigFile = utils.createButton(BUTTON_CREATE, styles.getBigButtonWidth(), BUTTON_CREATE, BUTTON_ICON_CREATE);

        Client.buttonCreateScenarioConfigFile.setDisable(true);
        Client.buttonMoveComponentUp.setDisable(true);
        Client.buttonMoveComponentDown.setDisable(true);

        Client.buttonMoveComponentUp.setOnAction(e -> moveComponent(-1));
        Client.buttonMoveComponentDown.setOnAction(e -> moveComponent(1));
        Client.buttonCreateScenarioConfigFile.setOnAction(e -> {
            processScenarioComponentList(Client.getPrimaryStage(), false);
            Client.buttonRefreshScenarioStatus.fire();
        });
        // Set up event handlers for button actions and table interactions
        ComponentLibraryTable.getTableCreateScenario().setOnMouseClicked(e -> setArrowAndButtonStatus());
        textFieldScenarioName.setOnKeyPressed(e -> setArrowAndButtonStatus());
    }

    /**
     * Moves the selected component up or down in the list.
     * Swaps the selected row with the adjacent row in the specified direction.
     *
     * @param direction -1 for up, 1 for down
     */
    private void moveComponent(int direction) {
        // Get the table and its items
        TableView<ComponentRow> table = ComponentLibraryTable.getTableCreateScenario();
        ObservableList<ComponentRow> allFiles = table.getItems();
        ObservableList<ComponentRow> selectedFiles = table.getSelectionModel().getSelectedItems();

        // Only proceed if exactly one item is selected
        if (selectedFiles.size() == 1) {
            int n = table.getSelectionModel().getSelectedIndex();
            int newIndex = n + direction;

            // Check bounds for moving
            if (newIndex >= 0 && newIndex < allFiles.size()) {
                ComponentRow fileA = allFiles.get(n);
                ComponentRow fileB = allFiles.get(newIndex);

                // Swap the selected item with the one above/below
                allFiles.set(newIndex, fileA);
                allFiles.set(n, fileB);
                table.setItems(allFiles);
            }
        }
    }

    /**
     * Sets the scenario name in the text field.
     *
     * @param scenarioName The scenario name to set in the text field.
     */
    public void setScenarioName(String scenarioName) {
        if (textFieldScenarioName != null && scenarioName != null) {
            textFieldScenarioName.setText(scenarioName);
        }
    }

    /**
     * Processes the scenario component list and creates the scenario configuration.
     * Validates the scenario name, copies the component list, and calls processScenario.
     *
     * @param stage The JavaFX stage.
     * @param b Whether to execute the scenario after creation.
     */
    public void processScenarioComponentList(Stage stage, boolean b) {
        // Get and sanitize scenario name
        String scenName = textFieldScenarioName.getText().replace("/", "-").replace("\\", "-").replace(" ", "_");
        boolean fixName = false;
        if (utils.hasSpecialCharacter(scenName)) fixName = true;

        // Validate scenario name
        if ((scenName.length() < 1) || (fixName)) {
            utils.warningMessage(WARNING_INVALID_NAME);
        } else {
            // Copy component list
            ObservableList<ComponentRow> copy1 = FXCollections.observableArrayList();
            ObservableList<ComponentRow> copy2 = FXCollections.observableArrayList();
            for (ComponentRow i : ComponentLibraryTable.getListOfFilesCreateScenario()) {
                copy1.add(i);
                copy2.add(i);
            }
            try {
                // Process scenario
                processScenario(scenName, copy1, copy2, scenName, scenName, b);
            } catch (Exception e1) {
                e1.printStackTrace();
                utils.exitOnException();
            }
        }
    }

    /**
     * Processes the files in the table to create a scenario configuration and supporting files.
     * Handles file type detection, CSV-to-XML conversion, XML configuration updates, and file management.
     *
     * @param scenName Scenario name (used for folder and file naming)
     * @param list List of component rows (primary)
     * @param list1 Duplicate list of component rows (for backup or additional processing)
     * @param runName Run name (used in configuration)
     * @param scenarioName Scenario name (used in configuration)
     * @param execute Whether to execute after creation
     * @throws IOException if file operations fail
     */
    @SuppressWarnings("static-access")
    protected void processScenario(String scenName, ObservableList<ComponentRow> list, ObservableList<ComponentRow> list1,
                                   String runName, String scenarioName, boolean execute) throws IOException {
        String message = "";
        boolean overwritingScenario = checkInList(scenName, ScenarioTable.tableScenariosLibrary);
 
        // Check if scenario already exists and prompt for overwrite
        if (overwritingScenario) {
            String s = SCENARIO_OVERWRITE_PROMPT + scenName + "?";
            boolean overwrite = utils.confirmAction(s);
            if (!overwrite) {
                return;
            }
        }
 
        // Create scenario dialog and collect metadata
        message = createScenarioDialog(scenarioName);
        if (message == null) return;
 
        // Delete old log and XML files if scenario is being overwritten
        if (overwritingScenario) {
            clearScenarioRunResultFields(scenName);
            String mainLogFile = vars.getScenarioDir() + File.separator + scenName + File.separator + "main_log.txt";
            files.deleteFile(mainLogFile);
        }

        // Add component list to message
        if (list.size() > 0) {
            StringBuilder listOfComponents = new StringBuilder();
            for (ComponentRow f : list) {
                listOfComponents.append(f.getFileName()).append(vars.getEol());
            }
            message = message + vars.getEol() + COMPONENTS_HEADER + vars.getEol() + listOfComponents + vars.getEol();
        }
        message += META_DATA_SEPARATOR + vars.getEol();
        String newDescription = "<!--" + vars.getEol() + message + vars.getEol() + "-->";

        // Prepare log file and scenario directory
        String mainLogFile = vars.getScenarioDir() + File.separator + scenarioName + File.separator + "main_log.txt";
        File file = new File(mainLogFile);
        if (file.exists()) {
            files.deleteFiles(vars.getScenarioDir(), ".txt");
            files.deleteFiles(vars.getScenarioDir(), ".xml");
        }
        String workingDir = vars.getScenarioDir() + File.separator + scenarioName;
        File dir = new File(workingDir);
        try {
            if (dir.exists()) {
                dir.delete();
            }
            new File(workingDir).mkdir();
        } catch (Exception e) {
            utils.warningMessage(ERROR_CREATE_DIR);
            System.out.println("error:" + e);
            utils.exitOnException();
        }

        // Copy template config and insert metadata
        String templateConfigFileAddress = vars.getConfigurationTemplateFilename();
        String savedConfigFileAddress = workingDir + File.separator + "configuration_" + scenarioName + ".xml";
        files.copyFile(templateConfigFileAddress, savedConfigFileAddress);
        utils.insertLinesIntoFile(savedConfigFileAddress, newDescription, 2);
        Document xmlDoc = XMLModifier.openXmlDocument(savedConfigFileAddress);
        Date now = null;
        Path gcamexepath = Paths.get(vars.getgCamExecutableDir());

        // Process each component in the list
        for (ComponentRow f : list) {
            String fileType = getFileType(f.getAddress(), "@type");

            // Handle CSV and table-based components
            if ((fileType.equals("preset")) || (fileType.equals("techbound")) || (fileType.equals("techparam")) || (fileType.equals("INPUT_TABLE"))) {
                String xmlFileAddress = workingDir + File.separator + f.getFileName().substring(0, f.getFileName().lastIndexOf('.')) + ".xml";
                System.out.println("---" + vars.getEol() + "Creating new xml file:\n  " + xmlFileAddress);
                Path xmlPath = Paths.get(workingDir);
                Path relativePath = gcamexepath.relativize(xmlPath);
                String xmlFileAddressForConfig = relativePath.toString() + File.separator + f.getFileName().substring(0, f.getFileName().lastIndexOf('.')) + ".xml";

                if (fileType.equals("INPUT_TABLE")) {
                    try {
                        // Convert CSV to XML using header file
                        String[] s = { f.getAddress(), vars.getXmlHeaderFilename(), xmlFileAddress };
                        s = utils.getRidOfTrailingCommasInStringArray(s);
                        System.out.println("csv to xml conversion commencing:");
                        System.out.println("    csv file: " + f.getAddress());
                        System.out.println("    header file: " + vars.getXmlHeaderFilename());
                        System.out.println("    xml file: " + xmlFileAddress);
                        String header = utils.getRidOfTrailingCommasInString(files.getLineXFromFile(f.getAddress(), 3, "#").trim());
                        System.out.println("header specified in csv file: " + header);
                        String header1 = header + ",";
                        String header2 = header + " ";
                        int headerInFile = files.countLinesWithTextInFile(new File(vars.getXmlHeaderFilename()), header, "#");
                        if (headerInFile == 0) headerInFile = files.countLinesWithTextInFile(new File(vars.getXmlHeaderFilename()), header1, "#");
                        if (headerInFile == 0) headerInFile = files.countLinesWithTextInFile(new File(vars.getXmlHeaderFilename()), header2, "#");
                        if (headerInFile > 0) {
                            CSVToXMLMain.main(s);
                        } else {
                        	String msg = ERROR_HEADER_NOT_FOUND+vars.getEol()+"Header: " + header + vars.getEol() + "File: " + f.getFileName();
                            utils.warningMessage(msg);
                            System.out.println("======================"+vars.getEol()+msg+vars.getEol()+"======================");
                        }
                    } catch (Exception e) {
                        utils.warningMessage(String.format(ERROR_CSV_TO_XML, f.getFileName()));
                        System.out.println("Error converting CSV->XML: " + e);
                        System.out.println("Attempting to continue, but conversion unsuccessful.");
                    }
                } else {
                    // Only CSV-type and XML-list-type scenario components are supported
                    utils.warningMessage(ERROR_UNSUPPORTED_COMPONENT);
                    System.out.println(ERROR_UNSUPPORTED_COMPONENT);
                }
                // Add new XML file to configuration
                XMLModifier.addElement(xmlDoc, "ScenarioComponents", "Value", f.getFileName(), xmlFileAddressForConfig);
            } else if ((fileType.equals("xmllist")) || (fileType.equals("list"))) {
                // Handle list of XML files
                System.out.println("adding files from list...");
                ArrayList<String> fileList = files.loadFileListFromFile(f.getAddress(), "@type");
                int num = 0;
                for (String temp : fileList) {
                    num++;
                    String filename = temp.replace('\\', '/');
                    String relativePathname = files.getRelativePath(gcamexepath.toString(), filename);
                    String identifier = f.getFileName();
                    if (fileList.size() > 1) identifier += "-" + num;
                    XMLModifier.addElement(xmlDoc, "ScenarioComponents", "Value", identifier, relativePathname);
                }
            } else if (fileType.equals("xml")) {
                // Handle direct XML component
                String filename = vars.getScenarioComponentsDir() + File.separator + f.getFileName().replace('\\', '/');;
                String relativePathname = files.getRelativePath(gcamexepath.toString(), filename);
                XMLModifier.addElement(xmlDoc, "ScenarioComponents", "Value", f.getFileName(), relativePathname);
            } else {
                // Unsupported component type
                utils.warningMessage(ERROR_PROCESS_COMPONENT + f.getFileName());
            }
        }

        // Update scenario name and stop year in configuration
        XMLModifier.updateElementValue(xmlDoc, "Strings", "Value", "scenarioName", scenarioName);
        if (vars.getStopYear() != null) XMLModifier.updateElementValue(xmlDoc, "Ints", "Value", "stop-year", vars.getStopYear());

        // Set parallelism option if not using all processors
        boolean useAllProcessors = vars.getUseAllAvailableProcessors();
        if (!useAllProcessors) {
            XMLModifier.updateElementValue(xmlDoc, "Ints", "Value", "max-parallelism", "1");
        }

        // Set debug region and debug file options
        if (vars.getDebugRegion() != null)
            XMLModifier.updateElementValue(xmlDoc, "Strings", "Value", "debug-region", vars.getDebugRegion());
        if (vars.getDebugCreate() != null)
            XMLModifier.updateAttributeValue(xmlDoc, "Files", "Value", "xmlDebugFileName", "write-output", vars.getDebugCreate());
        if (vars.getDebugRename() != null)
            XMLModifier.updateAttributeValue(xmlDoc, "Files", "Value", "xmlDebugFileName", "append-scenario-name", vars.getDebugRename());

        // Set solver and database paths in configuration
        if (vars.getgCamSolver() != null) {
            try {
                // Set solver path as relative to GCAM executable directory
                File solverFile = new File(vars.getgCamSolver());
                Path solverPath = Paths.get(solverFile.getPath());
                XMLModifier.updateElementValue(xmlDoc, "ScenarioComponents", "Value", "solver", gcamexepath.relativize(solverPath).toString().replace('\\', '/'));
            } catch (Exception e) {
                // Fallback to full path if relative path fails
                System.out.println("Could not set solver path in config file. Using full path.");
                System.out.println("  error: " + e);
                XMLModifier.updateElementValue(xmlDoc, "ScenarioComponents", "Value", "solver", vars.getgCamSolver().replace('\\', '/'));
            }
        }
        if (vars.getgCamOutputDatabase() != null) {
            try {
                // Set database path as relative to GCAM executable directory
                File databaseDir = new File(vars.getgCamOutputDatabase());
                Path databasePath = Paths.get(databaseDir.getPath());
                XMLModifier.updateElementValue(xmlDoc, "Files", "Value", "xmldb-location", gcamexepath.relativize(databasePath).toString().replace('\\', '/'));
            } catch (Exception e) {
                // Fallback to full path if relative path fails
                System.out.println("Could not set relative database path in config file. Using full path.");
                System.out.println("  error: " + e);
                XMLModifier.updateElementValue(xmlDoc, "Files", "Value", "xmldb-location", vars.getgCamOutputDatabase().replace('\\', '/'));
            }
        }

        // Write the updated XML document to file
        XMLModifier.writeXmlDocument(xmlDoc, savedConfigFileAddress);
    }

    /**
     * Checks if a scenario name exists in the scenario table.
     * Iterates through the table rows and compares scenario names.
     *
     * @param name Scenario name to check
     * @param table Scenario table to search
     * @return true if found, false otherwise
     */
    public boolean checkInList(String name, TableView<ScenarioRow> table) {
        ObservableList<ScenarioRow> list = table.getItems();
        for (ScenarioRow row : list) {
            String str = row.getScenarioName();
            if (str.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the scenario name text field for direct access or testing.
     *
     * @return The scenario name text field.
     */
    public TextField getTextFieldScenarioName() {
        return textFieldScenarioName;
    }

    /**
     * Gets the VBox containing the UI for this pane.
     *
     * @return The VBox containing all UI elements.
     */
    public VBox getvBox() {
        return vBox;
    }

    /**
     * Creates the scenario dialog and returns the scenario meta data as a string.
     * Presents a dialog for the user to enter scenario metadata, select options, and add comments.
     * Handles database size warnings, debug and processor options, and files to save.
     *
     * @param scenName Scenario name to display in the dialog
     * @return Scenario meta data string, or null if cancelled
     */
    public String createScenarioDialog(String scenName) {
    	
    	// Determine default years and debug regions based on allowable policy years and GCAM version
    	String[] default_years = null;// = DEFAULT_STOP_YEARS;
    	String[] default_debug_regions = null;// = vars.isGcamUSA() ? DEFAULT_DEBUG_REGIONS_USA : DEFAULT_DEBUG_REGIONS_GLOBAL;
    	
    	//TODO: make this code work with subregions for regions other than USA
    	if (vars.getAllowablePolicyYears() != null && vars.getAllowablePolicyYears().size() > 0) {
			default_years = utils.createStringArrayFromListOfIntegers(vars.getAllowablePolicyYears());
		}
    	List<String> tempList = new ArrayList<>();
    	
    	if (vars.isGcamUSA()) {
    		if (vars.getSubRegionList() != null && vars.getSubRegionList().size() > 0) {
				tempList.addAll(vars.getSubRegionList());
			} 
    	}
    	if (vars.getRegionList() != null && vars.getRegionList().size() > 0) {
    		tempList.addAll(vars.getRegionList());
    	}
    	if (tempList.size() > 0) {
    		default_debug_regions = tempList.toArray(new String[0]);
		} 
    	
        // Create labels and controls for dialog
        Label scenarioNameLabel = new Label("Scenario name:");
        Label scenarioName = new Label(scenName);
        Label stopYearLabel = new Label("Final model year:");
        ComboBox<String> stopYearComboBox = new ComboBox<>();
        stopYearComboBox.getItems().addAll(default_years);
        stopYearComboBox.getSelectionModel().select(utils.getYearForPeriod(Integer.parseInt(vars.getStopPeriod())));
        stopYearComboBox.setDisable(false);
        stopYearComboBox.setOnAction(e -> vars.setStopPeriod(utils.getPeriodForYear(stopYearComboBox.getSelectionModel().getSelectedItem())));

        // Database info
        Label databaseNameLabel = new Label("Database:");
        String databaseName = vars.getgCamOutputDatabase();
        File databaseFolder = new File(databaseName);
        Path databasePath = databaseFolder.toPath();
        long databaseSize = files.getDirectorySize(databasePath) / 1000000000;
        String databaseSizeStr = " (" + databaseSize + " GB)";
        String databaseNameShort = databaseName.substring(databaseName.lastIndexOf(File.separator) + 1);
        Label databaseNameAndSize = new Label(databaseNameShort + databaseSizeStr);

        // Warn if database is too large
        if (databaseSize >= vars.getMaxDatabaseSizeGB()) {
            boolean b = utils.confirmAction(DATABASE_SIZE_WARNING);
            if (!b) return null;
        }

        // Debug and processor options
        CheckBox createDebugCheckBox = new CheckBox("Create debug file?");
        boolean isChecked = false;
        String strIsChecked = vars.getDebugCreate().toLowerCase();
        if (strIsChecked.equals("true") || strIsChecked.equals("yes") || strIsChecked.equals("1")) isChecked = true;
        createDebugCheckBox.setSelected(isChecked);

        ComboBox<String> debugRegionComboBox = new ComboBox<>();
        //if (vars.isGcamUSA()) {
            debugRegionComboBox.getItems().addAll(default_debug_regions);
        //} else {
        //    debugRegionComboBox.getItems().addAll(DEFAULT_DEBUG_REGIONS_GLOBAL);
        //}
        debugRegionComboBox.getSelectionModel().select(vars.getDebugRegion());
        debugRegionComboBox.setDisable(false);
        debugRegionComboBox.setOnAction(e -> vars.setDebugRegion(debugRegionComboBox.getSelectionModel().getSelectedItem()));

        CheckBox useAllAvailableProcessors = new CheckBox("Use all available processors?");
        boolean b = vars.getUseAllAvailableProcessors();
        if (b) isChecked = true;
        useAllAvailableProcessors.setSelected(isChecked);

        // Files to save options
        Label filesToSaveLabel = new Label("Save files in scenario folder: (global setting)");
        CheckBox saveMainLogCheckBox = new CheckBox("Main log");
        saveMainLogCheckBox.setSelected(true);
        saveMainLogCheckBox.setDisable(true);
        CheckBox saveCalibrationLogCheckBox = new CheckBox("Calibration log");
        saveCalibrationLogCheckBox.setSelected(false);
        saveCalibrationLogCheckBox.setDisable(false);
        CheckBox saveSolverLogCheckBox = new CheckBox("Solver log");
        saveSolverLogCheckBox.setSelected(false);
        saveSolverLogCheckBox.setDisable(false);
        CheckBox saveDebugFileCheckBox = new CheckBox("Debug file");
        saveDebugFileCheckBox.setSelected(false);
        saveDebugFileCheckBox.setDisable(false);
        String filesToSave = vars.getFilesToSave().toLowerCase();
        if (filesToSave.contains("debug")) saveDebugFileCheckBox.setSelected(true);
        if (filesToSave.contains("solver")) saveSolverLogCheckBox.setSelected(true);
        if (filesToSave.contains("calibration")) saveCalibrationLogCheckBox.setSelected(true);

        // Comments area
        Label commentLabel = new Label("Comments:");
        TextArea textArea = new TextArea();
        textArea.setEditable(true);
        // Allow the text area to grow with its container instead of using a fixed preferred size
        textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        textArea.setMinHeight(0);
        GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        // Layout grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        // Use centralized horizontal padding for grids
        grid.setPadding(styles.getHorizontalPadding10());
        grid.add(scenarioNameLabel, 0, 0);
        grid.add(scenarioName, 1, 0);
        grid.add(databaseNameLabel, 0, 1);
        grid.add(databaseNameAndSize, 1, 1);
        grid.add(stopYearLabel, 0, 2);
        grid.add(stopYearComboBox, 1, 2);
        grid.add(createDebugCheckBox, 0, 3);
        grid.add(debugRegionComboBox, 1, 3);
        grid.add(useAllAvailableProcessors, 0, 4, 2, 1);
        grid.add(filesToSaveLabel, 0, 5, 2, 1);
        grid.add(saveMainLogCheckBox, 0, 6);
        grid.add(saveDebugFileCheckBox, 1, 6);
        grid.add(saveCalibrationLogCheckBox, 0, 7);
        grid.add(saveSolverLogCheckBox, 1, 7);
        grid.add(commentLabel, 0, 8, 2, 1);
        grid.add(textArea, 0, 9, 2, 1);

        // Dialog stage and scene
        Stage stage = createDialogStage(DIALOG_TITLE_CREATE, DIALOG_WIDTH, DIALOG_HEIGHT);
        Scene scene = new Scene(new Group());

        // Ensure dialog uses the same modern theme as the main app
        applyModernTheme(scene);

        // Use centralized dialog button creation to match New Scenario Component Creator look-and-feel
        // OK and Cancel buttons
        Button okButton = createDialogButton("OK");
        Button cancelButton = createDialogButton("Cancel");
        okButton.setOnAction(e -> {
            String isSelected = "false";
            if (createDebugCheckBox.isSelected()) isSelected = "true";
            vars.setDebugCreate(isSelected);
            isSelected = "false";
            if (useAllAvailableProcessors.isSelected()) isSelected = "true";
            vars.setUseAllAvailableProcessors(isSelected);
            vars.setFilesToSave(adjustFilesToSave(saveCalibrationLogCheckBox.isSelected(), saveSolverLogCheckBox.isSelected(), saveDebugFileCheckBox.isSelected()));
            stage.close();
        });
        cancelButton.setOnAction(e -> {
            utils.clearTextArea(textArea);
            stage.close();
        });

        // Layout root and button box
        VBox root = new VBox();
        // Use centralized small padding for dialog roots
        root.setPadding(styles.getSmallPadding());
        root.setSpacing(5);
        root.setAlignment(Pos.TOP_LEFT);
        String text = "";
        textArea.setText(text);
        VBox.setVgrow(grid, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        HBox buttonBox = new HBox();
        // Use centralized button/small padding
        buttonBox.setPadding(styles.getSmallPadding());
        buttonBox.setSpacing(5);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(okButton, cancelButton);
        root.getChildren().addAll(grid, buttonBox);
        scene.setRoot(root);
        stage.setScene(scene);
        stage.showAndWait();

        // Prepare meta data string
        if (textArea.getText() == null) {
            text = null;
        } else {
            text = META_DATA_HEADER + vars.getEol();
            text += "Scenario name: " + scenarioName.getText() + vars.getEol();
            text += "Database: " + databaseNameShort + vars.getEol();
            text += "Debug region: " + debugRegionComboBox.getSelectionModel().getSelectedItem() + vars.getEol();
            text += "Stop year:" + stopYearComboBox.getSelectionModel().getSelectedItem() + vars.getEol();
            text += "Comments:" + vars.getEol();
            text += textArea.getText() + vars.getEol();
        }
        if (text != null) text = text.replaceAll(vars.getEol() + "" + vars.getEol(), vars.getEol());
        return text;
    }

    /**
     * Adjusts the files to save string based on user selections in the dialog.
     * Adds or removes calibration, solver, and debug log files as needed.
     *
     * @param saveCalibLog Save calibration log
     * @param saveSolverLog Save solver log
     * @param saveDebugFile Save debug file
     * @return Updated files to save string
     */
    private String adjustFilesToSave(boolean saveCalibLog, boolean saveSolverLog, boolean saveDebugFile) {
        List<String> filesToSave = utils.createArrayListFromString(vars.getFilesToSave(), ";");
        String foundCalib = null;
        String foundSolver = null;
        String foundDebug = null;
        for (String filename : filesToSave) {
            String filenamelc = filename.toLowerCase();
            if (filenamelc.contains("debug")) foundDebug = filename;
            if (filenamelc.contains("calib")) foundCalib = filename;
            if (filenamelc.contains("solver")) foundSolver = filename;
        }
        if ((!saveCalibLog) && (foundCalib != null)) filesToSave.remove(foundCalib);
        if ((!saveSolverLog) && (foundSolver != null)) filesToSave.remove(foundSolver);
        if ((!saveDebugFile) && (foundDebug != null)) filesToSave.remove(foundDebug);
        if ((saveCalibLog) && (foundCalib == null)) filesToSave.add(vars.getgCamExecutableDir() + File.separator + "logs" + File.separator + "calibration_log.txt");
        if ((saveSolverLog) && (foundSolver == null)) filesToSave.add(vars.getgCamExecutableDir() + File.separator + "logs" + File.separator + "solver_log.csv");
        if ((saveDebugFile) && (foundDebug == null)) filesToSave.add(vars.getgCamExecutableDir() + File.separator + "debug.xml");
        return utils.createStringFromArrayList(filesToSave, ";");
    }

    /**
     * Clears the run-result fields for a scenario by resetting the matching table row.
     * This is used when a scenario is recreated to clear stale run-result data.
     *
     * @param scenarioName The name of the scenario whose run-result fields should be cleared.
     */
    private void clearScenarioRunResultFields(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return;
        }
        if (ScenarioTable.listOfScenarioRuns == null) {
            return;
        }
        ScenarioLibraryViewStateHelper viewStateHelper = new ScenarioLibraryViewStateHelper(utils);
        viewStateHelper.preserveSelectionAndRefresh(() -> {
            for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                if (row == null || !scenarioName.equals(row.getScenarioName())) {
                    continue;
                }
                row.setCompletedDate("");
                row.setStatus("");
                row.setUnsolvedMarkets("");
                row.setRuntime("");
                break;
            }
        });
    }
}
