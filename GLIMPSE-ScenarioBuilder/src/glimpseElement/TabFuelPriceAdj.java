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

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.CheckComboBox;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * TabFuelPriceAdj provides the user interface and logic for creating and editing
 * fuel price adjustment policies in the GLIMPSE Scenario Builder.
 * <p>
 * <b>Main Features:</b>
 * <ul>
 *   <li>Specify, edit, and save fuel price adjustment policies.</li>
 *   <li>Select fuels, regions, and adjustment types.</li>
 *   <li>Auto-generate policy and market names based on selections.</li>
 *   <li>Validate user input and provide feedback for missing/invalid data.</li>
 *   <li>Generate scenario component files for downstream processing.</li>
 *   <li>Load existing policy files and populate the UI.</li>
 * </ul>
 *
 * <b>Usage:</b>
 * <ul>
 *   <li>Instantiate with a tab title and JavaFX stage.</li>
 *   <li>Interact with the UI to select fuels, regions, and adjustment parameters.</li>
 *   <li>Use Populate, Delete, and Clear buttons to manage adjustment values.</li>
 *   <li>Save the scenario component to generate the output file.</li>
 * </ul>
 *
 * <b>Threading:</b> Implements Runnable for background save operations. UI updates must be wrapped in Platform.runLater.
 *
 * <b>Dependencies:</b> Requires JavaFX, ControlsFX, and GLIMPSE utility classes.
 */
public class TabFuelPriceAdj extends PolicyTab implements Runnable {
    // === UI constants ===
    private static final String LABEL_FUEL = "Fuel: ";
    private static final String LABEL_UNITS = "Units: ";
    private static final String LABEL_UNITS_VALUE = "1975$s per GJ";

    // === UI components ===
    private final Label labelFuel = createLabel(LABEL_FUEL, LABEL_WIDTH);
    private final CheckComboBox<String> checkComboBoxFuel = createCheckComboBox();
    private final Label labelUnits = createLabel(LABEL_UNITS, LABEL_WIDTH);
    private final Label labelUnitsValue = createLabel(LABEL_UNITS_VALUE, 225.);
    // HBox for Auto and Unique checkboxes
    private final HBox hBoxAutoUnique = new HBox(8);

    /**
     * List of available fuel types for selection, populated from technology info.
     */
    private ArrayList<String> fuelList = new ArrayList<>();

    /**
     * Constructs a new TabFuelPriceAdj instance and initializes the UI components
     * for the Fuel Price Adjustment tab. Sets up event handlers and populates
     * controls with available data.
     *
     * @param title  The title of the tab
     * @param stageX The JavaFX stage
     */
    public TabFuelPriceAdj(String title, Stage stageX) {
    	super.setupEventHandlers();
    	setupUIControls(title, stageX);
        setComponentWidths();
        setupUILayout();
    }

    /**
     * Sets up the UI controls and populates them with available data.
     *
     * @param title  The title of the tab
     * @param stageX The JavaFX stage
     */
    private void setupUIControls(String title, Stage stageX) {
        setupLeftColumn();
        setupCenterColumn();
        setupRightColumn();

        // Set up region tree and tab title
        TreeItem<String> ti = paneForCountryStateTree != null && paneForCountryStateTree.getTree() != null
                ? paneForCountryStateTree.getTree().getRoot()
                : null;
        if (ti != null)
            ti.setExpanded(true);
        this.setText(title);
        if (styles != null)
            this.setStyle(styles.getFontStyle());

        // Set up initial state of check box and text fields
        if (checkBoxUseAutoNames != null)
            checkBoxUseAutoNames.setSelected(true);
        if (checkBoxUseUniqueNames != null)
            checkBoxUseUniqueNames.setSelected(true);
        if (textFieldPolicyName != null)
            textFieldPolicyName.setDisable(true);
        if (textFieldMarketName != null)
            textFieldMarketName.setDisable(true);
        if (comboBoxConvertFrom != null) {
    		//comboBoxConvertFrom.getItems().clear();
            //comboBoxConvertFrom.getItems().addAll(CONVERT_FROM_OPTIONS);
            comboBoxConvertFrom.getSelectionModel().selectFirst();
        }

        // Populate fuel and modification type options
        String[][] tech_list = vars.getTechInfo();
        extractInfoFromTechList(tech_list);
        if (checkComboBoxFuel != null && fuelList != null)
            checkComboBoxFuel.getItems().addAll(fuelList);

        if (comboBoxModificationType != null) {
            comboBoxModificationType.getItems().addAll(MODIFICATION_TYPE_OPTIONS);
            comboBoxModificationType.getSelectionModel().selectFirst();
        }
        
        setPolicyAndMarketNames();

        // Event handlers for UI controls
        checkComboBoxFuel.getCheckModel().getCheckedItems()
                .addListener((javafx.collections.ListChangeListener<String>) change -> {
                    setPolicyAndMarketNames();
        });
    }

    /**
     * Configures the left column of the tab UI, including labels and controls.
     */
    private void setupLeftColumn() {
        gridPaneLeft.getChildren().clear();
        hBoxAutoUnique.getChildren().clear();
        hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
        gridPaneLeft.addColumn(0, createLabel("Specification:"),labelFuel, new Label(), labelUnits, new Label(), new Separator(),
        createLabel("Names:"), labelPolicyName, labelMarketName, new Label(), new Separator(),
        createLabel("Populate:"), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount,
        labelGrowth, labelConvertFrom);
        gridPaneLeft.addColumn(1, createLabel("Select one or more:"),checkComboBoxFuel, new Label(), labelUnitsValue, new Label(),
        new Separator(), hBoxAutoUnique, textFieldPolicyName, textFieldMarketName, new Label(),
        new Separator(), new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear,
        textFieldInitialAmount, textFieldGrowth, comboBoxConvertFrom);
        gridPaneLeft.setAlignment(Pos.TOP_LEFT);
        gridPaneLeft.setVgap(3.);
        gridPaneLeft.setStyle(styles.getStyle2());
        scrollPaneLeft.setContent(gridPaneLeft);
    }

    /**
     * Sets min, max, and preferred widths for controls in the tab for consistent UI layout.
     */
    private void setComponentWidths() {
        double max_wid = this.MAX_WIDTH, min_wid = this.MIN_WIDTH, pref_wid = this.PREF_WIDTH;
        checkComboBoxFuel.setMaxWidth(max_wid);
        checkComboBoxFuel.setMinWidth(min_wid);
        checkComboBoxFuel.setPrefWidth(pref_wid);
        textFieldStartYear.setMaxWidth(max_wid);
        textFieldStartYear.setMinWidth(min_wid);
        textFieldStartYear.setPrefWidth(pref_wid);
        textFieldEndYear.setMaxWidth(max_wid);
        textFieldEndYear.setMinWidth(min_wid);
        textFieldEndYear.setPrefWidth(pref_wid);
        textFieldInitialAmount.setMaxWidth(max_wid);
        textFieldInitialAmount.setMinWidth(min_wid);
        textFieldInitialAmount.setPrefWidth(pref_wid);
        textFieldGrowth.setMaxWidth(max_wid);
        textFieldGrowth.setMinWidth(min_wid);
        textFieldGrowth.setPrefWidth(pref_wid);
        textFieldPeriodLength.setMaxWidth(max_wid);
        textFieldPeriodLength.setMinWidth(min_wid);
        textFieldPeriodLength.setPrefWidth(pref_wid);
        textFieldPolicyName.setMaxWidth(max_wid);
        textFieldPolicyName.setMinWidth(min_wid);
        textFieldPolicyName.setPrefWidth(pref_wid);
        textFieldMarketName.setMaxWidth(max_wid);
        textFieldMarketName.setMinWidth(min_wid);
        textFieldMarketName.setPrefWidth(pref_wid);
        comboBoxConvertFrom.setMaxWidth(max_wid);
        comboBoxConvertFrom.setMinWidth(min_wid);
        comboBoxConvertFrom.setPrefWidth(min_wid);
    }

    /**
     * Extracts fuel technology names from the technology list and populates fuelList.
     * Only technologies categorized as "Energy-Carrier" are included.
     *
     * @param tech_list 2D array of technology information
     */
    private void extractInfoFromTechList(String[][] tech_list) {
        for (int row = 0; row < tech_list.length; row++) {
            String str_cat = tech_list[row][7].trim();
            String str_tech = tech_list[row][2].trim();
            if (str_cat.equals("Energy-Carrier")) {
                fuelList.add(str_tech);
            }
        }
        // Remove duplicates
        fuelList = utils.getUniqueItemsFromStringArrayList(fuelList);
    }

    /**
     * Sets the policy and market names automatically based on selected fuels and regions.
     * If auto-naming is enabled, updates the text fields accordingly.
     * Naming convention: fuelPrc-<fuel>-<region>
     */
    protected void setPolicyAndMarketNames() {
        if (checkBoxUseAutoNames != null && checkBoxUseAutoNames.isSelected()) {
            String policy_type = "fuelPrc";
            String fuel = "----";
            String state = "--";
            try {
                // Determine selected fuel(s)
                if (checkComboBoxFuel.getCheckModel().getCheckedItems() != null) {
                    fuel = "Misc";
                    if (checkComboBoxFuel.getCheckModel().getCheckedItems().size() == 1) {
                        fuel = checkComboBoxFuel.getCheckModel().getCheckedItems().get(0).toLowerCase().replaceAll(" ", "_");
                    }
                }
                // Determine selected region(s)
                String[] listOfSelectedLeaves = (paneForCountryStateTree != null
                        && paneForCountryStateTree.getTree() != null)
                                ? utils.getAllSelectedRegions(paneForCountryStateTree.getTree())
                                : new String[0];
                if (listOfSelectedLeaves.length > 0) {
                    listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
                    String state_str = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
                    if (state_str.length() < 9)
                        state = state_str;
                    else
                        state = "Reg";
                }
                // Set names
                String name = policy_type + "-" + fuel + "-" + state;
                name = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("--", "_").replaceAll("_-_", "_").replaceAll("---", "");
                if (textFieldMarketName != null)
                    textFieldMarketName.setText(name + "_Mkt");
                if (textFieldPolicyName != null)
                    textFieldPolicyName.setText(name);
            } catch (Exception e) {
                System.out.println("Cannot auto-name market. Continuing.");
            }
        }
    }

    /**
     * Runnable implementation. Triggers saving of the scenario component.
     * Intended to be called in a separate thread; UI updates must be wrapped in Platform.runLater.
     */
    @Override
    public void run() {
        saveScenarioComponent();
    }

    /**
     * Saves the scenario component using the current UI state and selected regions.
     * Delegates to saveScenarioComponent(TreeView<String> tree).
     */
    @Override
    public void saveScenarioComponent() {
        saveScenarioComponent(paneForCountryStateTree.getTree());
    }

    /**
     * Saves the scenario component for the specified tree of regions. Performs QA
     * checks, generates unique IDs, and builds the output file content including
     * metadata and adjustment values for each selected fuel and region.
     *
     * @param tree The TreeView of regions
     */
    private void saveScenarioComponent(TreeView<String> tree) {
        if (!qaInputs()) {
            // If QA fails, terminate the current thread
            Thread.currentThread().destroy();
        } else {
            String[][] tech_list = vars.getTechInfo();
            String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
            listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
            utils.returnAppendedString(listOfSelectedLeaves);

            filenameSuggestion = "";

            String ID = null;
            if (checkBoxUseUniqueNames.isSelected()) { 
            	ID = utils.getUniqueString();
            } else {
            	ID="";
            }

            filenameSuggestion = textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
            String policyName = textFieldPolicyName.getText() + ID;
            String marketName = textFieldMarketName.getText() + ID;

            StringBuilder fileContentBuilder = new StringBuilder();
            fileContentBuilder.append(getMetaDataContent(tree, marketName, policyName));
            boolean firstitem = true;

            List<String> selectedFuels = checkComboBoxFuel.getCheckModel().getCheckedItems();

            // For each selected fuel, find matching technologies and build adjustment rows
            for (String fuel : selectedFuels) {
                for (int t = 0; t < tech_list.length; t++) {
                    String cat = tech_list[t][7].trim();
                    if (cat.equals("Energy-Carrier")) {
                        String tech = tech_list[t][2].trim();
                        if (tech.contains(fuel)) {
                            String sector = tech_list[t][0].trim();
                            String subsector = tech_list[t][1].trim();

                            if (!firstitem) {
                                fileContentBuilder.append(vars.getEol());
                            }
                            firstitem = false;

                            String header = "GLIMPSEFuelPriceAdj";

                            fileContentBuilder.append("INPUT_TABLE").append(vars.getEol()).append("Variable ID")
                                    .append(vars.getEol()).append(header).append(vars.getEol()).append(vars.getEol())
                                    .append("region,supplysector,subsector,technology,param,year,adjustment")
                                    .append(vars.getEol());

                            for (String state : listOfSelectedLeaves) {
                                ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();
                                for (String dataStr : data) {
                                    String[] splitData = utils.splitString(dataStr, ",");
                                    String year = splitData[0];
                                    String val = splitData[1];
                                    fileContentBuilder.append(state).append(",").append(sector).append(",")
                                            .append(subsector).append(",").append(tech).append(",").append(year)
                                            .append(",regional price adjustment,").append(val).append(vars.getEol());
                                }
                            }
                        }
                    }
                }
            }

            fileContent = fileContentBuilder.toString();
        }
    }

    /**
     * Generates the metadata content string for the scenario component, including
     * selected fuels, units, policy/market names, and table data.
     *
     * @param tree   The TreeView of regions
     * @param market The market name
     * @param policy The policy name
     * @return Metadata content string
     */
    public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
    	StringBuilder rtnStr = new StringBuilder();
    	rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        String fuel = fuelList != null ? utils.getStringFromList(checkComboBoxFuel.getCheckModel().getCheckedItems(), ";") : "";
        rtnStr.append("#Fuel: ").append(fuel).append(vars.getEol());
        rtnStr.append("#Units: ").append(labelUnitsValue.getText()).append(vars.getEol());
        if (policy == null) market = textFieldPolicyName.getText();
        rtnStr.append("#Policy name: ").append(policy).append(vars.getEol());
        if (market == null) market = textFieldMarketName.getText();
        rtnStr.append("#Market name: ").append(market).append(vars.getEol());
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
     * Loads content from a list of strings (typically from a file) and populates
     * the UI fields accordingly. Parses metadata and table data, and updates UI controls.
     *
     * @param content The list of content lines to load
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            int pos = line.indexOf(":");
            if (line.startsWith("#") && (pos > -1)) {
                String param = line.substring(1, pos).trim().toLowerCase();
                String value = line.substring(pos + 1).trim();
                // Populate UI fields based on metadata
                if (param.equals("fuel") && checkComboBoxFuel != null) {
                    String[] set = utils.splitString(value, ";");
                    for (int j = 0; j < set.length; j++) {
                        String item = set[j].trim();
                        checkComboBoxFuel.getCheckModel().check(item);
                    }
                }
                if (param.equals("units") && labelUnitsValue != null) {
                    labelUnitsValue.setText(value);
                }
                if (param.equals("policy name") && textFieldPolicyName != null) {
                    textFieldPolicyName.setText(value);
                    textFieldPolicyName.fireEvent(new ActionEvent());
                }
                if (param.equals("market name") && textFieldMarketName != null) {
                    textFieldMarketName.setText(value);
                    textFieldMarketName.fireEvent(new ActionEvent());
                }
                if (param.equals("regions") && paneForCountryStateTree != null) {
                    String[] regions = utils.splitString(value, ",");
                    this.paneForCountryStateTree.selectNodes(regions);
                }
                if (param.equals("table data") && paneForComponentDetails != null) {
                    parseAndAddTableData(value);
                }
            }
        }
        if (paneForComponentDetails != null)
            this.paneForComponentDetails.updateTable();
    }

    /**
     * Helper method to parse table data from a string and add to the component details.
     * Expects a comma-separated string with year and value.
     *
     * @param value The string containing year and value, comma-separated
     */
    private void parseAndAddTableData(String value) {
        String[] s = utils.splitString(value, ",");
        if (s.length >= 2 && paneForComponentDetails != null) {
            this.paneForComponentDetails.data.add(new DataPoint(s[0], s[1]));
        }
    }

    /**
     * Validates table data years against allowable policy years.
     * Returns true if at least one year matches allowable years, false otherwise.
     */
    private boolean validateTableDataYears() {
        List<Integer> listOfAllowableYears = vars.getAllowablePolicyYears();
        ObservableList<DataPoint> data = paneForComponentDetails != null ? this.paneForComponentDetails.table.getItems()
                : null;
        if (data == null)
            return false;
        for (DataPoint dp : data) {
            Integer year = Integer.parseInt(dp.getYear().trim());
            if (listOfAllowableYears.contains(year)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs QA checks on the current UI state to ensure all required inputs are valid.
     * Displays warnings or error messages as needed. Checks for region selection,
     * table data, valid years, fuel selection, and policy/market names.
     *
     * @return true if all inputs are valid, false otherwise
     */
    protected boolean qaInputs() {
        TreeView<String> tree = paneForCountryStateTree != null ? paneForCountryStateTree.getTree() : null;
        int error_count = 0;
        String message = "";
        try {
            if (tree == null || utils.getAllSelectedRegions(tree).length < 1) {
                message += "Must select at least one region from tree" + vars.getEol();
                error_count++;
            }
            if (paneForComponentDetails == null || paneForComponentDetails.table.getItems().size() == 0) {
                message += "Data table must have at least one entry" + vars.getEol();
                error_count++;
            } else {
                boolean match = validateTableDataYears();
                if (!match) {
                    message += "Years specified in table must match allowable policy years ("
                            + vars.getAllowablePolicyYears() + ")" + vars.getEol();
                    error_count++;
                }
            }
            if (checkComboBoxFuel != null && (checkComboBoxFuel.getCheckModel().getCheckedItems().size() == 0)) {
                message += "Fuel checkComboBox must have at least one selection" + vars.getEol();
                error_count++;
            }
            if (textFieldPolicyName == null || textFieldPolicyName.getText().equals("")) {
                message += "A policy name must be provided" + vars.getEol();
                error_count++;
            }
            if (textFieldMarketName == null || textFieldMarketName.getText().equals("")) {
                message += "A market name must be provided" + vars.getEol();
            }
        } catch (Exception e1) {
            System.out.println("error " + e1);
            error_count++;
            message += "Error in QA of entries" + vars.getEol();
        }
        if (error_count > 0) {
            try {
                if (error_count == 1)
                    utils.warningMessage(message);
                else if (error_count > 1)
                    utils.displayString(message, "Parsing Errors");
            } catch (Exception e1) {
                System.out.println(message);
                throw (e1);
            }
        }
        return error_count == 0;
    }

    /**
     * Helper method to set min, max, and preferred widths for multiple Controls.
     *
     * @param controls Array of controls to set widths for
     * @param min      Minimum width
     * @param max      Maximum width
     * @param pref     Preferred width
     */
    private void setWidths(Control[] controls, double min, double max, double pref) {
        for (Control c : controls) {
            c.setMinWidth(min);
            c.setMaxWidth(max);
            c.setPrefWidth(pref);
        }
    }

    /**
     * Helper method to add items to a ComboBox<String>.
     *
     * @param comboBox the ComboBox to add items to
     * @param items    the array of items to add
     */
    private void addItemsToComboBox(ComboBox<String> comboBox, String[] items) {
        comboBox.getItems().addAll(items);
    }

    /**
     * Helper method to add items to a CheckComboBox<String>.
     *
     * @param checkComboBox the CheckComboBox to add items to
     * @param items         the array of items to add
     */
    private void addItemsToCheckComboBox(CheckComboBox<String> checkComboBox, String[] items) {
        checkComboBox.getItems().addAll(items);
    }

    /**
     * Registers an event handler for a ComboBox's ActionEvent.
     *
     * @param comboBox the ComboBox to register the event for
     * @param handler  the event handler
     */
    private void registerComboBoxEvent(ComboBox<String> comboBox, javafx.event.EventHandler<ActionEvent> handler) {
        comboBox.setOnAction(handler);
    }

    /**
     * Registers an event handler for a CheckBox's ActionEvent.
     *
     * @param checkBox the CheckBox to register the event for
     * @param handler  the event handler
     */
    private void registerCheckBoxEvent(CheckBox checkBox, javafx.event.EventHandler<ActionEvent> handler) {
        checkBox.setOnAction(handler);
    }

    /**
     * Registers an event handler for a Button's ActionEvent.
     *
     * @param button  the Button to register the event for
     * @param handler the event handler
     */
    private void registerButtonEvent(Button button, javafx.event.EventHandler<ActionEvent> handler) {
        button.setOnAction(handler);
    }

}
