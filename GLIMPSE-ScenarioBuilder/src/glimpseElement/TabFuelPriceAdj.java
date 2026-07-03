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
 * TabFuelPriceAdj implements the UI and logic for creating and editing fuel
 * price adjustment scenario components in the GLIMPSE Scenario Builder.
 *
 * Responsibilities:
 * - Build and manage the JavaFX controls used to define a fuel price
 *   adjustment policy (fuels, regions, adjustment values, names, units).
 * - Validate the user input before saving.
 * - Generate the scenario component file content (metadata + table rows).
 *
 * This class focuses on UI wiring and content generation only; it does not
 * perform file I/O itself. It implements Runnable so save operations can be
 * dispatched off the FX application thread if needed (UI updates must use
 * Platform.runLater).
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
    // HBox grouping the auto-name and unique-name checkboxes
    private final HBox hBoxAutoUnique = new HBox(8);

    /**
     * Cached list of fuel names (extracted from technology info) used to
     * populate the fuel selection control.
     */
    private ArrayList<String> fuelList = new ArrayList<>();

    /**
     * Construct a TabFuelPriceAdj and initialize controls and layout.
     *
     * @param title  tab title shown to the user
     * @param stageX primary JavaFX Stage (used for dialogs/ownership)
     */
    public TabFuelPriceAdj(String title, Stage stageX) {
        super.setupEventHandlers();
        setupUIControls(title, stageX);
        setComponentWidths();
        setupUILayout();
    }

    /**
     * Initialize UI controls, load options from shared variables/utilities,
     * and attach event listeners. This method only mutates UI state; it does
     * not perform file operations.
     *
     * @param title  tab title
     * @param stageX JavaFX Stage used for any dialogs
     */
    private void setupUIControls(String title, Stage stageX) {
        setupLeftColumn();
        setupCenterColumn();
        setupRightColumn();

        // Expand the root of the region tree (if available)
        TreeItem<String> ti = paneForCountryStateTree != null && paneForCountryStateTree.getTree() != null
                ? paneForCountryStateTree.getTree().getRoot()
                : null;
        if (ti != null)
            ti.setExpanded(true);
        this.setText(title);
        if (styles != null)
            this.setStyle(styles.getFontStyle());

        // Default control states: auto/unique naming enabled and name fields
        // disabled when auto naming is active.
        if (checkBoxUseAutoNames != null)
            checkBoxUseAutoNames.setSelected(true);
        if (checkBoxUseUniqueNames != null)
            checkBoxUseUniqueNames.setSelected(true);
        if (textFieldPolicyName != null)
            textFieldPolicyName.setDisable(true);
        if (textFieldMarketName != null)
            textFieldMarketName.setDisable(true);
        if (comboBoxConvertFrom != null) {
            comboBoxConvertFrom.getSelectionModel().selectFirst();
        }

        // Populate fuel options from technology info
        String[][] tech_list = vars.getTechInfo();
        extractInfoFromTechList(tech_list);
        if (checkComboBoxFuel != null && fuelList != null) {
            resetCheckComboBoxItems(checkComboBoxFuel, fuelList);
            configureCheckComboBoxSelectionTitle(checkComboBoxFuel, "Select One or More", "Selected");
        }

        // Populate modification type choices
        if (comboBoxModificationType != null) {
            setModificationTypeOptions(MODIFICATION_TYPE_OPTIONS);
        }

        setPolicyAndMarketNames();

        // Update generated names whenever the fuel selection changes
        checkComboBoxFuel.getCheckModel().getCheckedItems()
                .addListener((javafx.collections.ListChangeListener<String>) change -> {
                    setPolicyAndMarketNames();
        });
    }

    /**
     * Build the left column of the tab, arranging labels and input controls.
     * The HBox groups the auto/unique name checkboxes for a compact layout.
     */
    private void setupLeftColumn() {
        gridPaneLeft.getChildren().clear();
        hBoxAutoUnique.getChildren().clear();
        hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
        gridPaneLeft.addColumn(0, createLabel("Specification:"),labelFuel, new Label(), labelUnits, new Label(), new Separator(),
        createLabel("Names:"), labelPolicyName, labelMarketName, new Label(), new Separator(),
        createLabel("Populate:"), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount,
        labelGrowth, labelConvertFrom);
        gridPaneLeft.addColumn(1, createLabel(""),checkComboBoxFuel, new Label(), labelUnitsValue, new Label(),
        new Separator(), hBoxAutoUnique, textFieldPolicyName, textFieldMarketName, new Label(),
        new Separator(), new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear,
        textFieldInitialAmount, textFieldGrowth, comboBoxConvertFrom);
        gridPaneLeft.setAlignment(Pos.TOP_LEFT);
        gridPaneLeft.setVgap(3.);
        // gridPaneLeft styling left to style manager elsewhere
        scrollPaneLeft.setContent(gridPaneLeft);
    }

    /**
     * Set consistent min/max/preferred widths for commonly used controls in the tab
     * to improve layout stability across platforms.
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
        comboBoxConvertFrom.setPrefWidth(pref_wid);
    }

    /**
     * Extract fuel (energy-carrier) names from the provided technology info
     * array. Filters technologies whose category equals "Energy-Carrier"
     * and de-duplicates the resulting list.
     *
     * @param tech_list 2D array of technology metadata (as provided by vars)
     */
    private void extractInfoFromTechList(String[][] tech_list) {
        for (int row = 0; row < tech_list.length; row++) {
            String str_cat = tech_list[row][7].trim();
            String str_tech = tech_list[row][2].trim();
            // Keep only energy carrier technologies as candidate fuels
            if (str_cat.equals("Energy-Carrier")) {
                fuelList.add(str_tech);
            }
        }
        // Remove duplicate names while preserving ordering
        fuelList = utils.getUniqueItemsFromStringArrayList(fuelList);
    }

    /**
     * Auto-generate policy and market names based on the currently selected
     * fuel(s) and region(s). Applies simple normalization (lowercase,
     * underscores) and a fallback when multiple items are selected.
     *
     * Naming convention: fuelPrc-<fuel>-<region>
     */
    protected void setPolicyAndMarketNames() {
        if (checkBoxUseAutoNames != null && checkBoxUseAutoNames.isSelected()) {
            String policy_type = "fuelPrc";
            String fuel = "----";
            String state = "--";
            try {
                // Build fuel part: single fuel -> normalized name, multiple -> "Misc"
                if (checkComboBoxFuel.getCheckModel().getCheckedItems() != null) {
                    fuel = "Misc";
                    if (checkComboBoxFuel.getCheckModel().getCheckedItems().size() == 1) {
                        fuel = checkComboBoxFuel.getCheckModel().getCheckedItems().get(0).toLowerCase().replaceAll(" ", "_");
                    }
                }
                // Build region part: collapse selection to a short string, use "Reg" for longer aggregated names
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
                        state = "Reg"; // use generic region label when selection is large
                }
                // Compose and normalize the name, then write to the text fields
                String name = policy_type + "-" + fuel + "-" + state;
                name = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("--", "_").replaceAll("_-_", "_").replaceAll("---", "");
                if (textFieldMarketName != null)
                    textFieldMarketName.setText(name + "_Mkt");
                if (textFieldPolicyName != null)
                    textFieldPolicyName.setText(name);
            } catch (Exception e) {
                // Non-fatal: if auto-naming fails, the user can enter names manually
                System.out.println("Cannot auto-name market. Continuing.");
            }
        }
    }

    /**
     * Runnable entry point used to invoke a save operation off the FX thread.
     * This method delegates to saveScenarioComponent().
     */
    @Override
    public void run() {
        saveScenarioComponent();
    }

    /**
     * Save the scenario component using the tree attached to the country/state
     * pane. This method is a convenient wrapper that delegates to the
     * saveScenarioComponent(TreeView<String>) implementation.
     */
    @Override
    public void saveScenarioComponent() {
        saveScenarioComponent(paneForCountryStateTree.getTree());
    }

    /**
     * Build and validate the scenario component content for the supplied region
     * tree. Performs QA checks, constructs unique identifiers (optional), and
     * aggregates adjustment rows for each selected fuel/technology and region.
     * The resulting content is written to the fileContent field.
     *
     * NOTE: This method constructs the CSV content but does not write it to
     * disk; that responsibility is handled elsewhere.
     *
     * @param tree TreeView of selected regions
     */
    private void saveScenarioComponent(TreeView<String> tree) {
        if (!qaInputs()) {
            // Validation failed: abort save
            return;
        } else {
            String[][] tech_list = vars.getTechInfo();
            String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
            listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
            utils.returnAppendedString(listOfSelectedLeaves);

            filenameSuggestion = "";

                  String ID = resolveUniqueSuffix(checkBoxUseUniqueNames.isSelected(), this.textFieldMarketName.getText());

            filenameSuggestion = textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
            String policyName = textFieldPolicyName.getText() + ID;
            String marketName = textFieldMarketName.getText() + ID;

            StringBuilder fileContentBuilder = new StringBuilder();
            fileContentBuilder.append(getMetaDataContent(tree, marketName, policyName));
            boolean firstitem = true;

            List<String> selectedFuels = checkComboBoxFuel.getCheckModel().getCheckedItems();

            // For each selected fuel, find corresponding tech entries and generate rows
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
     * Create the metadata block that is prepended to the generated component
     * content. Metadata includes selected fuels, units, names, regions and the
     * table rows used to construct the component.
     *
     * @param tree   the region tree used for region selection
     * @param market market name to include (if null, the current UI value is used)
     * @param policy policy name to include (if null, the current UI value is used)
     * @return formatted metadata string (ends with EOL)
     */
    public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
        StringBuilder rtnStr = new StringBuilder();
        rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        String fuel = fuelList != null ? utils.getStringFromList(checkComboBoxFuel.getCheckModel().getCheckedItems(), ";") : "";
        rtnStr.append("#Fuel: ").append(fuel).append(vars.getEol());
        rtnStr.append("#Units: ").append(labelUnitsValue.getText()).append(vars.getEol());
        if (policy == null) policy = textFieldPolicyName.getText();
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
     * Load a saved scenario component represented as a list of lines and update
     * the UI controls to reflect the loaded values. Only metadata lines (those
     * starting with '#') that contain a ':' separator are parsed here.
     *
     * @param content lines read from a saved component file
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            int pos = line.indexOf(":");
            if (line.startsWith("#") && (pos > -1)) {
                String param = line.substring(1, pos).trim().toLowerCase();
                String value = line.substring(pos + 1).trim();
                // Map recognized metadata keys back into the UI
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
                }
                if (param.equals("market name") && textFieldMarketName != null) {
                    textFieldMarketName.setText(value);
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
     * Parse a single table-data string of the form "year,value" and add it
     * to the component details data structure. This method tolerates extra
     * columns but uses only the first two values as year and value.
     *
     * @param value comma-separated data string
     */
    private void parseAndAddTableData(String value) {
        String[] s = utils.splitString(value, ",");
        if (s.length >= 2 && paneForComponentDetails != null) {
            this.paneForComponentDetails.addTableRow(s[0], s[1]);
        }
    }

    /**
     * Check that the years present in the data table intersect with the
     * allowable policy years. Returns true if at least one data year is
     * allowable; false otherwise.
     *
     * @return true when at least one table year is valid for policies
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
     * Perform QA on current UI inputs. Returns true if all required fields are
     * present and valid. On failure, display a user-facing message summarizing
     * the problems.
     *
     * @return true when QA passes
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
                error_count++;
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
     * Convenience method to set widths on an array of controls. Useful for
     * applying uniform sizing in layout code.
     *
     * @param controls array of controls to resize
     * @param min      minimum width
     * @param max      maximum width
     * @param pref     preferred width
     */
    private void setWidths(Control[] controls, double min, double max, double pref) {
        for (Control c : controls) {
            c.setMinWidth(min);
            c.setMaxWidth(max);
            c.setPrefWidth(pref);
        }
    }

}