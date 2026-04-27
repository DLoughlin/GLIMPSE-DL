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
package glimpseElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.CheckComboBox;
import glimpseBuilder.CsvFileWriter;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * TabTechParam is the JavaFX tab used in the GLIMPSE Scenario Builder to create
 * and edit technology parameter policy components.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Present UI controls for selecting technology categories and specific technologies.</li>
 *   <li>Allow users to choose a parameter to modify (and a sub-parameter/emission when applicable).</li>
 *   <li>Support input/output unit specification and region selection.</li>
 *   <li>Enable year/value pair data entry for policy definitions.</li>
 *   <li>Perform validation (QA) checks on user inputs before saving.</li>
 *   <li>Serialize configurations to CSV format and generate metadata for scenario components.</li>
 * </ul>
 * </p>
 *
 * <p><b>UI Structure:</b>
 * <ul>
 *   <li>Left column: Category, filter, technology selection, parameter choices, input/output units, and populate controls.</li>
 *   <li>Center column: Region tree selection and modification type options.</li>
 *   <li>Right column: Year/value data entry table.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b> Not thread-safe; use only on the JavaFX Application Thread.</p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Extends PolicyTab and implements Runnable for background thread execution.</li>
 *   <li>Handles special cases for emission parameters and levelized non-energy cost conversions.</li>
 * </ul>
 * </p>
 */
public class TabTechParam extends PolicyTab implements Runnable {
    // === Constants for UI Texts and Options ===
    // UI text constants for labels, options, and warnings
    private static final String SELECT_ONE = "Select One";
    private static final String SELECT_ONE_OR_MORE = "Select One or More";
    private static final String ALL = "All";
    private static final String[] PARAM_OPTIONS = {
            SELECT_ONE , "Shareweight", "Subsector Shareweight", "Nested-Subsector Shareweight", "Levelized Non-Energy Cost",
            "Capacity Factor", "Fixed Output", "Lifetime", "Halflife"
    };
    private static final String[] EMISSION_OPTIONS = {
            SELECT_ONE, "NOx", "SO2", "PM10", "PM2.5", "CO", "NH3", "NMVOC", "BC", "OC"
    };
    private static final String WARNING_UNITS_MISMATCH = "Warning - Units do not match!";
    private static final String UNIT_UNITLESS = "unitless";
    private static final String UNIT_YEARS = "years";
    private static final String UNIT_UNITLESS_CAPACITY = "Unitless";
    private static final String LABEL_VALUES = "Values: ";
    private static final String LABEL_SPECIFICATION = "Specification:";
    private static final String LABEL_POPULATE = "Populate:";
    private static final String LABEL_CATEGORY = "Category:";
    private static final String LABEL_FILTER = "Filter:";
    private static final String LABEL_TECHS = "Tech(s): ";
    private static final String LABEL_PARAMETER = "Parameter: ";
    private static final String LABEL_PARAMETER2 = "Parameter 2: ";
    private static final String LABEL_INPUT = "Input: ";
    private static final String LABEL_OUTPUT = "Output: ";
    private static final String LABEL_UNITS = "Units: ";
    private static final String LABEL_MANUAL_YEAR_VALUE_PAIRS = "Year-Value Pairs:";
    

    // === Layout and UI Components ===
    // Layout containers for tab columns and panes

    // === UI Controls ===
    // JavaFX controls for user input and display. These are initialized via
    // helper methods in PolicyTab (inherited) and here to keep the UI fields
    // strongly-typed and available across methods.
    private final Label labelCategory = createLabel(LABEL_CATEGORY, LABEL_WIDTH);
    private final ComboBox<String> comboBoxCategory = createComboBoxString(PREF_WIDTH);
    private final Label labelFilter = createLabel(LABEL_FILTER, LABEL_WIDTH);
    private final TextField textFieldFilter = createTextField(PREF_WIDTH);
    private final Label labelCheckComboBoxTech = createLabel(LABEL_TECHS, LABEL_WIDTH);
    private final CheckComboBox<String> checkComboBoxTech = utils.createCheckComboBox(PREF_WIDTH);
    private final Label labelComboBoxParam = createLabel(LABEL_PARAMETER, LABEL_WIDTH);
    private final ComboBox<String> comboBoxParam = createComboBoxString(PREF_WIDTH);
    private final Label labelComboBoxParam2 = createLabel(LABEL_PARAMETER2, LABEL_WIDTH);
    private final ComboBox<String> comboBoxParam2 = createComboBoxString(PREF_WIDTH);
    private final Label labelTextFieldInput = createLabel(LABEL_INPUT, LABEL_WIDTH);
    private final Label labelTextFieldInput2 = createLabel("");
    private final Label labelTextFieldOutput = createLabel(LABEL_OUTPUT, LABEL_WIDTH);
    private final Label labelTextFieldOutput2 = createLabel("", LABEL_WIDTH);
    private final Label labelTextFieldUnits = createLabel(LABEL_UNITS, LABEL_WIDTH);
    private final Label labelTextFieldUnits2 = createLabel("", LABEL_WIDTH);
    private final Label labelValue = createLabel(LABEL_VALUES);
    private final Label labelManualYearValuePairs = createLabel(LABEL_MANUAL_YEAR_VALUE_PAIRS, LABEL_WIDTH);

    // === Data ===
    // Technology info array loaded from GLIMPSEVariables. Structure is assumed
    // to be rows of String arrays describing sector, subsector, tech, inputs,
    // outputs and categories (see GLIMPSEVariables.getTechInfo()).
    private String[][] techInfo = null;

    /**
     * Construct a TabTechParam instance and initialize the UI controls.
     * Sets up the table, event handlers, and populates category controls from
     * technology metadata. The tab is ready for user interaction after construction.
     *
     * @param title  The title shown on the tab
     * @param stageX The JavaFX Stage (passed to parent classes)
     */
    public TabTechParam(String title, Stage stageX) {
        // sets tab title and style
        this.setText(title);
        this.setStyle(styles.getFontStyle());

        setupUIControls(); // Initialize UI controls and default options
        setComponentWidths();
        setupUIComponents();
        setupUILayout();   // arrange the left/center/right panes
        setupEventHandlers(); // register event handlers
        techInfo = vars.getTechInfo(); // load technology metadata
        setupComboBoxCategory(); // populate Category combo box

        //checkComboBoxTech.setDisable(true);
    }

    /**
     * Initialize control lists, default selections, visibility states, and option values.
     * Disables tech filtering and parameter 2 controls until a category is selected.
     * Keeps the UI in a known default state before any data is loaded.
     */
    private void setupUIControls() {
        checkComboBoxTech.getItems().clear();
        checkComboBoxTech.getItems().add(SELECT_ONE_OR_MORE);
        checkComboBoxTech.getCheckModel().check(0);
        checkComboBoxTech.setDisable(true);
        textFieldFilter.setDisable(true);

        // comboBoxConvertFrom defaults
        comboBoxConvertFrom.getSelectionModel().selectFirst();
        comboBoxConvertFrom.setVisible(false);
        labelConvertFrom.setVisible(false);

        // Parameter 2 is hidden unless required by the chosen parameter
        labelComboBoxParam2.setVisible(false);
        comboBoxParam2.setVisible(false);

        comboBoxParam.getItems().addAll(PARAM_OPTIONS);
        comboBoxParam.getSelectionModel().selectFirst();
        comboBoxParam.setDisable(false);

        comboBoxParam2.getItems().addAll(SELECT_ONE);
        comboBoxParam2.getSelectionModel().selectFirst();
        comboBoxParam2.setDisable(true);

        comboBoxModificationType.getItems().addAll(MODIFICATION_TYPE_OPTIONS);
        comboBoxModificationType.getSelectionModel().selectFirst();

    }

    /**
     * Set consistent min/max/preferred widths for UI components in this tab.
     */
    private void setComponentWidths() {
        // Set widths for specific labels
        Label[] labels = { labelTextFieldInput, labelTextFieldInput2, labelTextFieldOutput, labelTextFieldOutput2 };
        for (Label label : labels) {
            if (label != null) {
                label.setMaxWidth(MAX_WIDTH);
                label.setMinWidth(MIN_WIDTH);
                label.setPrefWidth(LABEL_WIDTH);
            }
        }
        ComboBox<?>[] comboBoxes = { comboBoxModificationType, comboBoxParam, comboBoxParam2, comboBoxCategory, comboBoxConvertFrom };
        for (ComboBox<?> comboBox : comboBoxes) {
            if (comboBox != null) {
                comboBox.setMaxWidth(MAX_WIDTH);
                comboBox.setMinWidth(MIN_WIDTH);
                comboBox.setPrefWidth(PREF_WIDTH);
            }
        }
        // Set widths for other controls used for data entry
        checkComboBoxTech.setMaxWidth(MAX_WIDTH);
        checkComboBoxTech.setMinWidth(MIN_WIDTH);
        checkComboBoxTech.setPrefWidth(PREF_WIDTH);
        textFieldStartYear.setMaxWidth(MAX_WIDTH);
        textFieldEndYear.setMaxWidth(MAX_WIDTH);
        textFieldInitialAmount.setMaxWidth(MAX_WIDTH);
        textFieldGrowth.setMaxWidth(MAX_WIDTH);
        textFieldPeriodLength.setMaxWidth(MAX_WIDTH);
        textFieldFilter.setMaxWidth(MAX_WIDTH);
        textFieldStartYear.setMinWidth(MIN_WIDTH);
        textFieldEndYear.setMinWidth(MIN_WIDTH);
        textFieldInitialAmount.setMinWidth(MIN_WIDTH);
        textFieldGrowth.setMinWidth(MIN_WIDTH);
        textFieldPeriodLength.setMinWidth(MIN_WIDTH);
        textFieldFilter.setMinWidth(MIN_WIDTH);
        textFieldStartYear.setPrefWidth(PREF_WIDTH);
        textFieldEndYear.setPrefWidth(PREF_WIDTH);
        textFieldInitialAmount.setPrefWidth(PREF_WIDTH);
        textFieldGrowth.setPrefWidth(PREF_WIDTH);
        textFieldPeriodLength.setPrefWidth(PREF_WIDTH);
        textFieldFilter.setPrefWidth(PREF_WIDTH);
    }
    
    /**
     * Configure left/center/right UI columns.
     */
    public void setupUIComponents() {
        setupLeftColumn();
        setupCenterColumn();
        setupRightColumn();    
    }

    /**
     * Configure and populate the left column grid pane with controls for policy specification,
     * including category selection, technology filter, parameter selectors, unit display,
     * and populate/data entry controls. The grid is placed inside scrollPaneLeft for scrolling.
     */
    private void setupLeftColumn() {

        // Prompt text helps users know they can filter the list of techs
        textFieldFilter.setPromptText("Filter techs");
        
        gridPaneLeft.getChildren().clear();
        gridPaneLeft.add(utils.createLabel(LABEL_SPECIFICATION), 0, 0, 2, 1);
        gridPaneLeft.addColumn(0, labelCategory, labelFilter, labelCheckComboBoxTech, labelComboBoxParam, labelComboBoxParam2,
                labelTextFieldInput, labelTextFieldOutput, labelTextFieldUnits, new Separator(),
                utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount,
                labelGrowth, labelConvertFrom);
        gridPaneLeft.addColumn(1,  comboBoxCategory,textFieldFilter, checkComboBoxTech, comboBoxParam, comboBoxParam2,
                labelTextFieldInput2, labelTextFieldOutput2, labelTextFieldUnits2, new Separator(), new Label(),
                comboBoxModificationType, textFieldStartYear, textFieldEndYear, textFieldInitialAmount,
                textFieldGrowth, comboBoxConvertFrom);
        gridPaneLeft.setAlignment(Pos.TOP_LEFT);
        gridPaneLeft.setVgap(3.);
        // Use scroll pane to contain the grid content for consistent layout
         scrollPaneLeft.setContent(gridPaneLeft);
    }

    /**
     * Small helper to attach an action handler to a button. Keeps code
     * consistent and centralizes registration logic.
     *
     * @param button target button
     * @param handler handler to execute on action
     */
    protected void registerButtonEvent(Button button, javafx.event.EventHandler<ActionEvent> handler) {
        button.setOnAction(handler);
    }
    /**
     * Register an action handler for a ComboBox.
     */
    private void registerComboBoxEvent(ComboBox<String> comboBox, javafx.event.EventHandler<ActionEvent> handler) {
        comboBox.setOnAction(handler);
    }
    /**
     * Register an action handler for a TextField.
     */
    private void registerTextFieldEvent(TextField textField, javafx.event.EventHandler<ActionEvent> handler) {
        textField.setOnAction(handler);
    }

    /**
     * Configure event handlers for user interactions in the tab. This includes
     * filter updates, category/parameter selection and reacting to changes in
     * the checked technologies list.
     */
    protected void setupEventHandlers() {

        super.setupEventHandlers();
        
        // Update tech list when filter text is changed (press Enter)
        registerTextFieldEvent(textFieldFilter, e -> { updateCheckComboTechs(); });
        
        // Double-clicking the tech label toggles selection when enabled
        labelCheckComboBoxTech.setOnMouseClicked(e -> {
            if (!checkComboBoxTech.isDisabled()) {
                boolean isFirstItemChecked = checkComboBoxTech.getCheckModel().isChecked(0);
                if (e.getClickCount() == 2) {
                    if (isFirstItemChecked) {
                        checkComboBoxTech.getCheckModel().clearChecks();
                    } else {
                        checkComboBoxTech.getCheckModel().checkAll();
                    }
                }
            }

        });
        // Category selection controls enabling/disabling of tech selection and filter
        registerComboBoxEvent(comboBoxCategory, e -> {
            String selectedItem = comboBoxCategory.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;
            if (selectedItem.equals(SELECT_ONE)) {
                // Reset tech selection UI when no category chosen
                checkComboBoxTech.getCheckModel().clearChecks();
                checkComboBoxTech.getItems().clear();
                checkComboBoxTech.getItems().add(SELECT_ONE_OR_MORE);
                checkComboBoxTech.getCheckModel().check(0);
                checkComboBoxTech.setDisable(true);
                labelTextFieldUnits2.setText("");
                textFieldFilter.setText("");
                textFieldFilter.setDisable(true);
            } else {
                // Populate and enable tech list when a category is selected
                updateCheckComboTechs();
                checkComboBoxTech.setDisable(false);
                textFieldFilter.setDisable(false);
            }
            setUnitsLabel();
        });
        // Parameter selection may reveal a secondary parameter (e.g., emissions)
        registerComboBoxEvent(comboBoxParam, e -> {
            comboBoxParam2.getSelectionModel().selectFirst();
            comboBoxParam2.setDisable(true);
            comboBoxParam2.setVisible(false);
            try {
                String selectedItem = comboBoxParam.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.contains("Emis")) {
                    comboBoxParam2.getItems().clear();
                    for (String option : EMISSION_OPTIONS) {
                        comboBoxParam2.getItems().add(option);
                    }
                    comboBoxParam2.getSelectionModel().select(0);
                    comboBoxParam2.setDisable(false);
                    comboBoxParam2.setVisible(true);
                } else {
                    comboBoxParam2.getSelectionModel().select(0);
                    comboBoxParam2.setDisable(true);
                }
                if (selectedItem != null && selectedItem.equals("Levelized Non-Energy Cost")) {
                    labelConvertFrom.setVisible(true);
                    comboBoxConvertFrom.setVisible(true);
                }
                setUnitsLabel();
            } catch (Exception ex) {
                // guard against unexpected nulls or index errors - do not propagate
            }
        });

        // When the set of checked technologies changes, update displayed IO units
        checkComboBoxTech.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> Platform.runLater(() -> {
            updateInputOutputUnits();
            setUnitsLabel();
        }));
    }

    /**
     * Update the displayed input/output unit labels based on currently checked
     * technologies in the CheckComboBox.
     *
     * If multiple selected technologies have differing units the label will be
     * set to "various" for that direction (input or output).
     */
    private void updateInputOutputUnits() {
        ObservableList<String> checkedItems = checkComboBoxTech.getCheckModel().getCheckedItems();
        String input = "";
        String output = "";
        if (checkedItems == null || checkedItems.isEmpty()) {
            labelTextFieldInput2.setText("");
            labelTextFieldOutput2.setText("");
            return;
        }
        for (String line : checkedItems) {
            String[] words = utils.splitString(line.trim(), ":");
            if (words.length >= 3) {
                String sector = words[0].trim();
                String subsector = words[1].trim();
                String tech = words[2].trim();
                for (String[] techRow : techInfo) {
                    if (sector.equals(techRow[0]) && subsector.equals(techRow[1]) && tech.equals(techRow[2])) {
                        String thisInput = techRow[3] + "(" + techRow[4] + ")";
                        String thisOutput = techRow[5] + "(" + techRow[6] + ")";
                        if (input.isEmpty()) {
                            input = thisInput;
                        } else if (!input.equals("various") && !input.equals(thisInput)) {
                            input = "various";
                        }
                        if (output.isEmpty()) {
                            output = thisOutput;
                        } else if (!output.equals("various") && !output.equals(thisOutput)) {
                            output = "various";
                        }
                    }
                }
            }
        }
        labelTextFieldInput2.setText(input);
        labelTextFieldOutput2.setText(output);
    }

    /**
     * Create a new TextField with a specific preferred width.
     *
     * @param prefWidth preferred width in pixels
     * @return configured TextField
     */
    private TextField createTextField(double prefWidth) {
        TextField textField = new TextField();
        textField.setPrefWidth(prefWidth);
        return textField;
    }


    /**
     * Populate the category combo box from the technology info array. The
     * category is expected to be at index 7 for each tech row. Duplicate
     * categories are removed.
     */
    private void setupComboBoxCategory() {
        comboBoxCategory.getItems().clear();
        comboBoxCategory.getItems().addAll(SELECT_ONE, ALL);
         comboBoxCategory.getSelectionModel().selectFirst();
         try {
            if (this.techInfo == null) return;
             ArrayList<String> categoryList = new ArrayList<>();
 
            for (String[] tech : this.techInfo) {
                 if (tech == null || tech.length == 0) continue;
                 String text = tech[7] != null ? tech[7].trim() : "";
                 boolean match = false;
                 for (String cat : categoryList) {
                     if (text.equals(cat)) {
                         match = true;
                         break;
                     }
                 }
                 if (!match) {
                         categoryList.add(text);
                 }
             }
             categoryList = utils.getUniqueItemsFromStringArrayList(categoryList);
             for (String cat : categoryList) {
                 if (cat != null) comboBoxCategory.getItems().add(cat.trim());
             }

         } catch (NullPointerException e) {
             utils.warningMessage("Problem reading tech list: Null value encountered.");
             System.out.println("NullPointerException reading tech list from " + vars.getTchBndListFilename() + ":");
             System.out.println("  ---> " + e);
         } catch (Exception e) {
             utils.warningMessage("Problem reading tech list.");
             System.out.println("Error reading tech list from " + vars.getTchBndListFilename() + ":");
             System.out.println("  ---> " + e);
         }
     }
     

    /**
     * Refresh the technologies shown in the CheckComboBox according to the
     * selected category and any filter text. Avoids duplicate entries and keeps
     * the display stable.
     */
    private void updateCheckComboTechs() {
            String cat = comboBoxCategory.getValue();
            if (cat == null) return;
            if (this.techInfo == null) return;
            boolean isAllCat = cat.equals(ALL);
            try {
                if (!checkComboBoxTech.getItems().isEmpty()) {
                    checkComboBoxTech.getCheckModel().clearChecks();
                    checkComboBoxTech.getItems().clear();
                }
                if (cat != null) {
                    String lastLine = "";
                    String filterText = textFieldFilter.getText() != null ? textFieldFilter.getText().trim() : "";
                    for (String[] techRow : this.techInfo) {
                        if (techRow == null || techRow.length < 3) continue;
                        String line = (techRow[0] != null ? techRow[0].trim() : "") + " : " + (techRow[1] != null ? techRow[1] : "") + " : " + (techRow[2] != null ? techRow[2] : "");
                        if (filterText.isEmpty() || line.contains(filterText)) {
                            if (techRow.length >= 7 && techRow[6] != null) line += " : " + techRow[6];
                            if (!line.equals(lastLine)) {
                                lastLine = line;
                                if (isAllCat || techRow[7].equals(cat)) {
                                    checkComboBoxTech.getItems().add(line);
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {
                utils.warningMessage("Problem reading tech list: Null value encountered.");
                System.out.println("NullPointerException reading tech list from " + vars.getTchBndListFilename() + ":");
                System.out.println("  ---> " + e);
            } catch (Exception e) {
                utils.warningMessage("Problem reading tech list.");
                System.out.println("Error reading tech list from " + vars.getTchBndListFilename() + ":");
                System.out.println("  ---> " + e);
            }
    }
    

    /**
     * Build the metadata header and body for the scenario component file. The
     * returned string includes category, technologies, parameter and all selected
     * regions as well as the data table rows.
     *
     * @param tree TreeView of regions (used to extract selected leaves)
     * @return string containing metadata and EOL-terminated lines
     */
    public String getMetaDataContent(TreeView<String> tree) {
        StringBuilder rtnStr = new StringBuilder();
        rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        rtnStr.append("#Category: ").append(comboBoxCategory.getValue()).append(vars.getEol());
        ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();
        String techs = utils.getStringFromList(techList, ";");
        rtnStr.append("#Technologies: ").append(techs).append(vars.getEol());
        rtnStr.append("#Parameter: ").append(comboBoxParam.getValue()).append(vars.getEol());
        appendTransportConversionMetadata(rtnStr);
        String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
        listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
        String states = utils.returnAppendedString(listOfSelectedLeaves);
        rtnStr.append("#Regions: ").append(states).append(vars.getEol());
        ArrayList<String> tableContent = this.paneForComponentDetails.getDataYrValsArrayList();
        for (String row : tableContent) {
            rtnStr.append("#Table data:").append(row).append(vars.getEol());
        }
        rtnStr.append("#################################################").append(vars.getEol());
        return rtnStr.toString();
    }

    /**
     * Load saved scenario component content (metadata lines) into the UI.
     * Recognizes category, technologies, parameter, regions and table data.
     *
     * @param content list of lines to parse
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        ArrayList<String> transportWarnings = new ArrayList<>();
        for (String line : content) {
            String transportWarning = getTransportConversionMetadataMismatchWarning(line);
            if (transportWarning != null) {
                transportWarnings = utils.addToArrayListIfUnique(transportWarnings, transportWarning);
            }

            int pos = line.indexOf(":");
            if (line.startsWith("#") && (pos > -1)) {
                String param = line.substring(1, pos).trim().toLowerCase();
                String value = line.substring(pos + 1).trim();
                if (param.equals("category")) {
                    comboBoxCategory.setValue(value);
                    comboBoxCategory.fireEvent(new ActionEvent());
                }
                if (param.equals("technologies")) {
                    // mark each technology in the checked list
                    String[] set = utils.splitString(value, ";");
                    for (String item : set) {
                        checkComboBoxTech.getCheckModel().check(item.trim());
                        checkComboBoxTech.fireEvent(new ActionEvent());
                    }
                }
                if (param.equals("parameter")) {
                    comboBoxParam.setValue(value);
                    comboBoxParam.fireEvent(new ActionEvent());
                }
                if (param.equals("regions")) {
                    String[] regions = utils.splitString(value, ",");
                    this.paneForCountryStateTree.selectNodes(regions);
                }
                if (param.equals("table data")) {
                    String[] s = utils.splitString(value, ",");
                    if (s.length >= 2) {
                        this.paneForComponentDetails.addTableRow(s[0], s[1]);
                    }
                }
            }
        }
        updateInputOutputUnits();
        this.paneForComponentDetails.updateTable();

        showTransportConversionMetadataWarnings(transportWarnings);
    }


    /**
     * For a given technology prefix, set the input/output labels using the
     * matching row in the techInfo array. This method is used when constructing
     * UI values from a single technology selection.
     *
     * @param techInfo two-dimensional array of tech metadata
     * @param row unused parameter kept for API compatibility
     * @param prefix array of prefix values to match (sector, subsector, tech)
     */
    public void addIOToTextFields(String[][] techInfo, int row, String[] prefix) {
        for (String[] tech : techInfo) {
            if (doesPrefixMatch(tech, prefix)) {
                String text = tech[3];
                if (tech.length > 3)
                    text += " (" + tech[4] + ")";
                labelTextFieldInput2.setText(text);
                if (tech.length > 4) {
                    text = tech[5];
                    if (tech.length > 5)
                        text += " (" + tech[6] + ")";
                    labelTextFieldOutput2.setText(text);
                } else {
                    labelTextFieldOutput2.setText("");
                }
            }
        }
    }

    /**
     * Add unique values from techInfo at the given index to the provided
     * ComboBox, for rows matching the provided prefix.
     */
    public void addNonDuplicatesToComboBox(ComboBox<String> comboBox, String[][] techInfo, int row, String[] prefix) {
        for (String[] tech : techInfo) {
            if (doesPrefixMatch(tech, prefix)) {
                if (!comboBox.getItems().contains(tech[row])) {
                    comboBox.getItems().add(tech[row]);
                }
            }
        }
    }

    /**
     * Add unique values from techInfo at the given index to the provided
     * CheckComboBox, for rows matching the provided prefix.
     */
    public void addNonDuplicatesToCheckComboBox(CheckComboBox<String> checkComboBox, String[][] techInfo, int row, String[] prefix) {
        for (String[] tech : techInfo) {
            if (doesPrefixMatch(tech, prefix)) {
                if (!checkComboBox.getItems().contains(tech[row])) {
                    checkComboBox.getItems().add(tech[row]);
                }
            }
        }
    }

    /**
     * Determine whether the item array begins with the given prefix array.
     * Returns true when prefix is null (matches all) and false for mismatches.
     *
     * @param item full item array
     * @param prefix prefix array to compare against (may be null)
     * @return true if prefix matches the start of item
     */
    public boolean doesPrefixMatch(String[] item, String[] prefix) {
        if (prefix == null) return true;
        if (item == null) return false;
        if (item.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (!item[i].equals(prefix[i])) return false;
        }
        return true;
    }

    /**
     * Determine a representative unit string for the currently checked
     * technologies. If different selected items have different units,
     * "No match" is returned. If nothing is selected an empty string is
     * returned.
     *
     * @return unit string, or "No match"/empty as described
     */
    public String getUnits() {
        ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();
        String unit = "";
        for (String line : techList) {
            try {
                String item = line.substring(line.lastIndexOf(":") + 1).trim();
                if (unit.isEmpty()) {
                    unit = item;
                } else if (!unit.equals(item)) {
                    unit = "No match";
                }
            } catch (Exception e) {
                // ignore malformed lines and continue
            }
        }
        if (unit.equals(SELECT_ONE_OR_MORE)) unit = "";
        return unit;
    }

    /**
     * Update the displayed units label according to the selected parameter
     * and the units derived from selected technologies. Handles special cases
     * for particular parameters (e.g., Capacity Factor, Lifetime).
     */
    public void setUnitsLabel() {
        String s = getUnits();
        String s2 = ""; 
        String label = "";
        String selectedParam = this.comboBoxParam.getSelectionModel().getSelectedItem();
        if (this.checkComboBoxTech.getCheckModel().getCheckedIndices().size() > 0) {
            switch (selectedParam) {
                case "Levelized Non-Energy Cost":
                    if (s.equals("No match")) {
                        label = WARNING_UNITS_MISMATCH;
                    } else if (s.equals("million pass-km")) {
                        String trnDollarUnits = vars.getUseTrn1990DollarConversions() ? "1990$" : "1975$s";
                        label = trnDollarUnits + " per veh-km";
                    } else if (s.equals("million ton-km")) {
                        String trnDollarUnits = vars.getUseTrn1990DollarConversions() ? "1990$" : "1975$s";
                        label = trnDollarUnits + " per GJ";
                    } else {
                        s2 = "GJ";
                        if (s.equals("petalumen-hours")) s2 = "megalumen-hours";
                        if (s.equals("million km3")) s2 = "million m3";
                        if (s.equals("billion cycles")) s2 = "cycle";
                        if (s.equals("Mt")) s2 = "kg";
                        if (s.equals("km^3")) s2 = "m^3";
                        label = "1975$s per " + s2;
                    }
                    break;
                case "Capacity Factor":
                    label = UNIT_UNITLESS_CAPACITY;
                    break;
                case "Fixed Output":
                    s2 = this.labelTextFieldOutput2.getText();
                    label = utils.getParentheticString(s2);
                    break;
                case "Lifetime":
                case "Halflife":
                    label = UNIT_YEARS;
                    break;
                default:
                    label = UNIT_UNITLESS;
            }
        }
        labelTextFieldUnits2.setText(label);
    }

    /**
     * Runnable implementation: save scenario component when run() invoked.
     */
    @Override
    public void run() {
        saveScenarioComponent();
    }

    /**
     * Save scenario component using the selected regions from the UI tree.
     */
    @Override
    public void saveScenarioComponent() {
        saveScenarioComponent(paneForCountryStateTree.getTree());
    }

    /**
     * Save scenario component to internal fields (fileContent/filenameSuggestion).
     * Performs QA checks and prepares CSV content.
     *
     * @param tree TreeView of regions used to build region fields
     */
    private void saveScenarioComponent(TreeView<String> tree) {
        if (!qaInputs()) {
            try {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {}
            return;
        } else {
            CsvFileWriter cfw = CsvFileWriter.getInstance();
            ArrayList<String> dataList = loadDataFromGUI(tree);
            ArrayList<String> colList = files.getStringArrayFromFile(vars.getCsvColumnFilename(), "#");
            ArrayList<String> csvContent = cfw.createCsvContent(colList, dataList);
            fileContent = getMetaDataContent(tree);
            fileContent += utils.createStringFromArrayList(csvContent);

            filenameSuggestion = getFilenameSuggestion();
        }
    }


    /**
     * Build a filename suggestion using selected technologies, parameter and
     * regions. Produces a sanitized filename safe for most filesystems.
     *
     * @return suggested filename with .csv extension
     */
    public String getFilenameSuggestion() {
        String name="";
        
        String tech="Multi";
        
        if (this.checkComboBoxTech.getCheckModel().getCheckedItems().size()==1) {
            tech = this.checkComboBoxTech.getCheckModel().getCheckedItems().get(0);
        }
        
        String param = this.comboBoxParam.getValue();

        String state = "Reg";
        String[] selectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
        if (selectedLeaves.length > 0) {
            selectedLeaves = utils.removeUSADuplicate(selectedLeaves);
            String stateStr = utils.returnAppendedString(selectedLeaves).replace(",", "");
            state = stateStr.length() < 9 ? stateStr : "Reg";
        }
        
        name = ("tchPrm" + "_" + tech + "_" + param + "_" + state).replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("___", "__").replaceAll("__", "_") + ".csv";
        return name;        
    }
    
    /**
     * Gather data from the GUI controls and return a list of type;sector;...
     * strings suitable for CSV creation. Each checked technology produces one
     * data line containing years/values and metadata fields.
     *
     * @param tree tree of selected regions
     * @return list of formatted data strings
     */
    private ArrayList<String> loadDataFromGUI(TreeView<String> tree) {
        ArrayList<String> dataList = new ArrayList<>();
        ObservableList<String> checkedItems = checkComboBoxTech.getCheckModel().getCheckedItems();
        for (String line : checkedItems) {
            String[] words = utils.splitString(line, ":");
            String sector = "sector:" + words[0].trim();
            String subsector = "subsector:" + words[1].trim();
            String tech = "technology:" + words[2].trim();
            String region = "region:" + getSelectedLeaves(tree);
            String input = "input:" + labelTextFieldInput2.getText().trim();
            String output = "output:" + labelTextFieldOutput2.getText().trim();
            String param = "param:" + this.comboBoxParam.getValue();
            String param2 = "param2:" + this.comboBoxParam2.getValue();
            ObservableList<DataPoint> data = this.paneForComponentDetails.table.getItems();
            StringBuilder year = new StringBuilder("year:");
            StringBuilder value = new StringBuilder("value:");
            for (int i = 0; i < data.size(); i++) {
                if (i != 0) {
                    year.append(",");
                    value.append(",");
                }
                year.append(data.get(i).getYear());
                value.append(data.get(i).getValue());
            }
            String group = "all";
            if (sector.indexOf("trn") >= 0) group = "trn";
            if (sector.indexOf("generation") >= 0) group = "egu";
            String type = "type:" + group + File.separator + this.comboBoxParam.getValue();
            String dataStr = type + ";" + sector + ";" + subsector + ";" + tech + ";" + region + ";" + input + ";"
                    + output + ";" + param + ";" + param2 + ";" + year + ";" + value;
            dataList.add(dataStr);
        }
        return dataList;
    }

    /**
     * Return a comma-separated list of selected leaf regions, handling a GCAM-
     * USA special-case where "USA" may be represented differently.
     *
     * @param tree TreeView of regions
     * @return comma-separated selected regions string
     */
    private String getSelectedLeaves(TreeView<String> tree) {
        String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
        String states = utils.returnAppendedString(listOfSelectedLeaves);
        if ((states.contains("USA")) && (vars.isGcamUSA())) {
            states = states.replace(",USA", "");
        }
        return states;
    }

    /**
     * Validate that the table data contains at least one row with a year that
     * is present in the allowable policy years configured in GLIMPSE.
     *
     * @return true when a valid year is found, false otherwise
     */
    private boolean validateTableDataYears() {
        List<Integer> listOfAllowableYears = vars.getAllowablePolicyYears();
        ObservableList<DataPoint> data = paneForComponentDetails != null ? this.paneForComponentDetails.table.getItems() : null;
        if (data == null) return false;
        for (DataPoint dp : data) {
            Integer year = Integer.parseInt(dp.getYear().trim());
            if (listOfAllowableYears.contains(year)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Run a series of QA checks on the current UI state. Checks include: at
     * least one region selected, at least one table entry, checked technologies,
     * and required parameter selections. Displays warnings/dialogs where
     * problems are found.
     *
     * @return true if all QA checks pass
     */
    protected boolean qaInputs() {
        TreeView<String> tree = paneForCountryStateTree.getTree();
        int errorCount = 0;
        StringBuilder message = new StringBuilder();
        try {
            if (utils.getAllSelectedRegions(tree).length < 1) {
                message.append("Must select at least one region from tree").append(vars.getEol());
                errorCount++;
            }
            if (paneForComponentDetails == null || paneForComponentDetails.table.getItems().size() == 0) {
                message.append("Data table must have at least one entry").append(vars.getEol());
                errorCount++;
            } else {
                boolean match = validateTableDataYears();
                if (!match) {
                    message.append("Years specified in table must match allowable policy years (").append(vars.getAllowablePolicyYears()).append(")").append(vars.getEol());
                    errorCount++;
                }
            }            
            if (checkComboBoxTech != null && ((checkComboBoxTech.getCheckModel().getCheckedItems().size() == 0) )) {
                message.append("Tech checkComboBox must have at least one selection").append(vars.getEol());
                errorCount++;
            }
            if (comboBoxParam.getSelectionModel().getSelectedItem().equals(SELECT_ONE)) {
                message.append("Parameter comboBox must have a selection").append(vars.getEol());
                errorCount++;
            }
            if (comboBoxParam2.isVisible()) {
                if (comboBoxParam2.getSelectionModel().getSelectedItem().equals(SELECT_ONE)) {
                    message.append("Parameter2 comboBox must have a selection").append(vars.getEol());
                    errorCount++;
                }
            }
        } catch (Exception e1) {
            errorCount++;
            message.append("Error in QA of entries").append(vars.getEol());
            message.append("  ---> ").append(e1).append(vars.getEol());
            System.out.println(message.toString());
        }
        if (errorCount > 0) {
            if (errorCount == 1) {
                utils.warningMessage(message.toString());
            } else if (errorCount > 1) {
                utils.displayString(message.toString(), "Parsing Errors");
            }
        }
        return errorCount == 0;
    }
}
