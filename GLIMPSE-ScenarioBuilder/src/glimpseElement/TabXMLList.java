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
package glimpseElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import glimpseUtil.FileChooserPlus;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TabXMLList provides the user interface and logic for managing lists of XML files
 * in the GLIMPSE Scenario Builder. This tab allows users to add, remove, and reorder
 * XML scenario components, and view or edit their details.
 *
 * <p>
 * <b>Usage:</b> Instantiated as a tab in the scenario builder, extending {@link PolicyTab}.
 * </p>
 *
 * <p>
 * <b>Thread Safety:</b> Not thread-safe; use only on the JavaFX Application Thread.
 * </p>
 *
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Display and manage a list of XML files used as scenario components.</li>
 *   <li>Allow users to add, remove, clear, and reorder XML files in the list.</li>
 *   <li>Provide file chooser dialogs for selecting XML files from the file system.</li>
 *   <li>Load and save the XML file list to and from scenario files.</li>
 *   <li>Integrate with the broader scenario builder UI and data model.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>UI Elements:</b>
 * <ul>
 *   <li>TableView for displaying the list of XML files.</li>
 *   <li>Buttons for Add, Delete, Clear, Move Up, and Move Down actions.</li>
 *   <li>Label for section header.</li>
 *   <li>Custom pane for displaying and editing XML file details.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Integration:</b>
 * <ul>
 *   <li>Works with {@link ComponentLibraryTable} for file list management.</li>
 *   <li>Uses {@link PaneForComponentDetails} for displaying file details.</li>
 *   <li>Relies on utility classes for UI styling and file path handling.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Example:</b>
 * <pre>
 * TabXMLList xmlListTab = new TabXMLList("XML List", stage, tableComponents);
 * scenarioTabPane.getTabs().add(xmlListTab);
 * </pre>
 * </p>
 */
public class TabXMLList extends PolicyTab {
    // === Constants ===
    private static final String LABEL_XML_FILES = "XML Files: ";
    private static final String BUTTON_ADD_TEXT = "Add";
    private static final String BUTTON_DELETE_TEXT = "Delete";
    private static final String BUTTON_CLEAR_TEXT = "Clear";
    private static final String BUTTON_MOVE_UP_TEXT = "Up";
    private static final String BUTTON_MOVE_DOWN_TEXT = "Down";
    private static final String XML_LIST_FILENAME = "xml_list.txt";
    private static final String XML_LIST_TYPE = "@type=xmllist";
    private static final String XML_FILE_EXTENSION = "*.xml";
    private static final String XML_FILE_DESCRIPTION = "XML files (*.xml)";
    private static final String FILECHOOSER_TITLE = "Select xml files";
    private static final String COLUMN_XML_FILENAME = "XML Filename";
    private static final String COLUMN_FORMAT = "-fx-alignment: CENTER-LEFT; -fx-padding: 5 20 5 5;";

    // === UI Components ===
    // Note: the table field was unused; removed to keep the class focused on the details pane.
     private final VBox paneIncludeXMLList = new VBox();
     private final VBox vBoxCenter = new VBox();
     private final HBox hBoxHeaderCenter = new HBox();
     private final Label labelValue = utils.createLabel(LABEL_XML_FILES);
     private final Button buttonAdd = utils.createButton(BUTTON_ADD_TEXT, styles.getBigButtonWidth(), null);
     private final Button buttonDelete = utils.createButton(BUTTON_DELETE_TEXT, styles.getBigButtonWidth(), null);
     private final Button buttonClear = utils.createButton(BUTTON_CLEAR_TEXT, styles.getBigButtonWidth(), null);
     private final Button buttonMoveUp = utils.createButton(BUTTON_MOVE_UP_TEXT, styles.getBigButtonWidth(), null);
     private final Button buttonMoveDown = utils.createButton(BUTTON_MOVE_DOWN_TEXT, styles.getBigButtonWidth(), null);
     private final PaneForComponentDetails paneForXMLList = new PaneForComponentDetails();

    /**
     * Constructs a new TabXMLList instance and initializes the UI components for the XML list tab.
     * Sets up event handlers and populates controls with available data.
     *
     * @param title The title of the tab
     * @param stageX The JavaFX stage used for file dialogs
     * @param tableComponents The table of scenario components (not used directly here)
     */
    public TabXMLList(String title, Stage stageX, TableView<ComponentRow> tableComponents) {
        // Set tab title and style
        this.setText(title);
        this.setStyle(styles.getFontStyle());

        setupUIControls();
        setComponentWidths();
        setupUILayout();
        setupButtonActions(stageX);
    }

    /**
     * Initializes UI controls, including the XML list pane and header buttons.
     */
    private void setupUIControls() {
        setupPaneForXMLList();
        setupHeaderButtons();
    }

    /**
     * Sets preferred, min, and max widths for UI components such as buttons.
     * Adjusts widths to maintain consistent UI appearance.
     */
    private void setComponentWidths() {
        buttonAdd.setPrefWidth(styles.getBigButtonWidth());
        buttonDelete.setPrefWidth(styles.getBigButtonWidth());
        buttonClear.setPrefWidth(styles.getBigButtonWidth());
        buttonMoveUp.setPrefWidth(styles.getBigButtonWidth());
        buttonMoveDown.setPrefWidth(styles.getBigButtonWidth());
        // Add more width settings for other controls if needed
    }

    /**
     * Sets up the overall layout of the tab, including the main pane and center VBox.
     * Assembles the UI hierarchy for display in the tab.
     */
    public void setupUILayout() {
        setupVBoxCenter();
        setupMainPane();
        VBox tabLayout = new VBox();
        // Apply centralized default padding, spacing and background style so the tab
        // visually matches other PolicyTab-based tabs
        tabLayout.setPadding(styles.getDefaultPadding());
        tabLayout.setSpacing(6.0);
        //tabLayout.setStyle(styles.getStyle2());

        // Ensure the main pane uses the standard style and padding
        //paneIncludeXMLList.setStyle(styles.getStyle2());
        paneIncludeXMLList.setPadding(styles.getDefaultPadding());
        // Allow the main pane to grow vertically to fill available space
        VBox.setVgrow(paneIncludeXMLList, Priority.ALWAYS);
        paneIncludeXMLList.setMaxHeight(Double.MAX_VALUE);

        // Ensure center VBox uses standard padding so header/buttons align with other tabs
        //vBoxCenter.setStyle(styles.getStyle2());
        vBoxCenter.setPadding(styles.getDefaultPadding());

        // Allow the center VBox to grow and expand vertically
        VBox.setVgrow(vBoxCenter, Priority.ALWAYS);
        vBoxCenter.setMaxHeight(Double.MAX_VALUE);

        tabLayout.getChildren().addAll(paneIncludeXMLList);
        this.setContent(tabLayout);
    }

    /**
     * Configures the pane for displaying and editing the XML file list.
     * Sets column names, disables add-item button, and applies column formatting.
     */
    private void setupPaneForXMLList() {
        paneForXMLList.setColumnNames(null, COLUMN_XML_FILENAME);
        paneForXMLList.setAddItemVisible(false);
        paneForXMLList.setColumnFormatting(COLUMN_FORMAT, COLUMN_FORMAT);
    }

    /**
     * Sets up the header buttons (Add, Move Up, Move Down, Delete, Clear) and their layout.
     * Arranges buttons in a horizontal box with spacing and style.
     */
    private void setupHeaderButtons() {
        hBoxHeaderCenter.getChildren().addAll(buttonAdd, buttonMoveUp, buttonMoveDown, buttonDelete, buttonClear);
        hBoxHeaderCenter.setSpacing(2.0);
        //hBoxHeaderCenter.setStyle(styles.getStyle3());
    }

    /**
     * Sets up the center VBox layout, including the label, header buttons, and XML list pane.
     * Applies style and fill width properties.
     */
    private void setupVBoxCenter() {
        vBoxCenter.getChildren().addAll(labelValue, hBoxHeaderCenter, paneForXMLList);
        // Apply default padding/background so the center matches other tabs
        //vBoxCenter.setStyle(styles.getStyle2());
        vBoxCenter.setPadding(styles.getDefaultPadding());
        vBoxCenter.setFillWidth(true);
        // Allow the details pane to expand vertically within the center VBox
        VBox.setVgrow(paneForXMLList, Priority.ALWAYS);
        paneForXMLList.setMaxHeight(Double.MAX_VALUE);
    }

    /**
     * Adds the center VBox to the main pane for the tab.
     * This is the main container for the tab's content.
     */
    private void setupMainPane() {
        paneIncludeXMLList.getChildren().addAll(vBoxCenter);
        // Ensure the center VBox can grow inside the main pane
        VBox.setVgrow(vBoxCenter, Priority.ALWAYS);
    }

    /**
     * Sets up button actions for the tab, including event handlers for Add, Delete, Clear, Move Up, and Move Down.
     * Handles file selection, error handling, and updates to the XML list.
     *
     * @param stageX The JavaFX stage used for file chooser dialogs
     */
    private void setupButtonActions(Stage stageX) {
        // Clear the XML list when Clear button is pressed
        buttonClear.setOnAction(e -> Platform.runLater(() -> paneForXMLList.clearTable()));
        // Add XML files to the list when Add button is pressed
        buttonAdd.setOnAction(e -> Platform.runLater(() -> {
            // Prefer xmlLibrary; fall back to GCAM executable dir if not available
            File initialDir = new File(vars.getXmlLibrary());
            if (initialDir.exists() && initialDir.isDirectory()) {
            } else {
                utils.warningMessage("Could not find xmlLibrary.");
                System.out.println("Could not find xmlLibrary " + vars.getXmlLibrary() + ". Defaulting to " + vars.getgCamExecutableDir());
                File fallbackDir = new File(vars.getgCamExecutableDir());
                if (fallbackDir.exists() && fallbackDir.isDirectory()) {
                    initialDir = fallbackDir;
                }
            }

            // Set file extension filter for XML files
            javafx.stage.FileChooser.ExtensionFilter filter = new javafx.stage.FileChooser.ExtensionFilter(XML_FILE_DESCRIPTION, XML_FILE_EXTENSION);

            // Show file chooser dialog for multiple file selection
            List<File> filesSelected = FileChooserPlus.showOpenMultipleDialog(stageX, FILECHOOSER_TITLE, initialDir, filter);
            if (filesSelected != null && !filesSelected.isEmpty()) {
                // If the table only contains a placeholder blank row, replace it on first add.
                ArrayList<String> currentValues = paneForXMLList.getValues();
                if (currentValues != null && currentValues.size() == 1) {
                    String onlyValue = currentValues.get(0);
                    if (onlyValue == null || onlyValue.trim().isEmpty()) {
                        paneForXMLList.clearTable();
                    }
                }

                for (File file : filesSelected) {
                    if (file == null) continue;
                    String path = file.toString().trim();
                    if (path.isEmpty()) continue;
                    // Convert absolute path to relative path for storage
                    String relPath = files.getRelativePath(vars.getgCamExecutableDir(), path);
                    if (relPath != null) {
                        paneForXMLList.addItem(relPath);
                    }
                }
            }
        }));
        // Delete selected XML files from the list
        buttonDelete.setOnAction(e -> Platform.runLater(() -> paneForXMLList.deleteItemsFromTable()));
        // Move selected XML file up in the list
        buttonMoveUp.setOnAction(e -> Platform.runLater(() -> paneForXMLList.moveItemUpInTable()));
        // Move selected XML file down in the list
        buttonMoveDown.setOnAction(e -> Platform.runLater(() -> paneForXMLList.moveItemDownInTable()));
    }

    /**
     * Loads content into the tab from the provided list of strings.
     * Each string represents an XML file path. Lines starting with '@' are ignored.
     *
     * @param content List of content lines to load (typically file paths)
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        if (content == null) return;
        int i = 0;
        for (String line : content) {
            if (line != null && !line.startsWith("@")) {
                String str = String.valueOf(i);
                i++;
                // Add each XML file path to the details pane
                paneForXMLList.addItem(str, line);
            }
        }
        // Refresh the component table if available
        if (ComponentLibraryTable.getTableComponents() != null) {
            ComponentLibraryTable.getTableComponents().refresh();
        }
    }

    /**
     * Loads XML list info from a file.
     *
     * Loads a list of XML file paths from the specified file, using the provided type string.
     * The loaded file list is then passed to {@link #loadContent(ArrayList)} to populate the tab.
     *
     * @param filename The path to the file containing the XML list
     * @param typeString The type string used to identify the XML list section in the file
     */
    public void loadInfoFromFile(String filename, String typeString) {
        if (filename == null || typeString == null) return;
        // Load file list from file using utility method
        ArrayList<String> fileList = files.loadFileListFromFile(filename, typeString);
        if (fileList != null) {
            loadContent(fileList);
        }
    }

    /**
     * Runs the save scenario component logic.
     *
     * This method triggers saving the current scenario component, including generating
     * the filename suggestion and file content for the XML list.
     * Typically called when the user saves the scenario or switches tabs.
     */
    public void run() {
        saveScenarioComponent();
    }

    /**
     * Saves the scenario component by generating the filename suggestion and file content.
     *
     * The XML file list is serialized to a string, with each file path on a new line,
     * and a type header at the top. The result is stored in the fileContent field.
     * This method is called by the scenario builder when saving the scenario.
     */
    @Override
    public void saveScenarioComponent() {
        filenameSuggestion = XML_LIST_FILENAME;
        StringBuilder sb = new StringBuilder(XML_LIST_TYPE).append(vars.getEol());
        ArrayList<String> fileList = paneForXMLList.getValues();
        if (fileList != null) {
            for (String file : fileList) {
                if (file != null) {
                    sb.append(file).append(vars.getEol());
                }
            }
        }
        fileContent = sb.toString();
    }
}
