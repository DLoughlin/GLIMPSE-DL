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
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * TabCafeStd is a JavaFX tab for creating and editing CAFE (Corporate Average Fuel Economy) standard policies
 * within the GLIMPSE Scenario Builder. This class provides a user interface for specifying policy parameters,
 * selecting regions, technologies, and units, and managing policy data tables. It supports both manual and
 * automatic naming of policies and markets, and handles the export of scenario component data in a format
 * compatible with GLIMPSE.
 *
 * <b>Main Features:</b>
 * <ul>
 *   <li>UI controls for subsector, technology, units, and modification type selection</li>
 *   <li>Automatic and manual naming of policy and market</li>
 *   <li>Region selection via a tree view</li>
 *   <li>Data table for year-value pairs and policy details</li>
 *   <li>Validation (QA) of user input and table data</li>
 *   <li>Export of scenario component metadata and policy tables</li>
 *   <li>Support for loading and saving policy configurations</li>
 * </ul>
 *
 * <b>Thread Safety:</b> This class is not thread-safe. All UI updates must be performed on the JavaFX Application Thread.
 *
 * <b>Usage:</b> Instantiate this class as a tab in the scenario builder UI. The user interacts with the controls to
 * define a CAFE standard policy, and can save or load configurations as needed.
 *
 * <b>Key Methods:</b>
 * <ul>
 *   <li>{@link #setupUIComponents()} - Initializes UI components and layout</li>
 *   <li>{@link #setupEventHandlers()} - Sets up event handlers for user interaction</li>
 *   <li>{@link #setPolicyAndMarketNames()} - Automatically generates policy and market names</li>
 *   <li>{@link #saveScenarioComponent()} - Saves the scenario component and exports data</li>
 *   <li>{@link #getMetaDataContent(TreeView, String, String)} - Generates metadata for export</li>
 *   <li>{@link #loadContent(ArrayList)} - Loads configuration from file</li>
 *   <li>{@link #qaInputs()} - Validates user input and table data</li>
 * </ul>
 *
 * <b>Class Structure:</b>
 * <ul>
 *   <li>UI component setup and layout methods</li>
 *   <li>Event handler setup for user interaction</li>
 *   <li>Methods for auto/manual naming, validation, and export</li>
 *   <li>Helper methods for data processing and QA</li>
 * </ul>
 */
public class TabCafeStd extends PolicyTab implements Runnable {
    // === Constants for UI labels and options ===
    private static final String LABEL_SPECIFICATION = "Specification:";
    private static final String LABEL_NAMES = "Names:";
    private static final String LABEL_SUBSECTOR = "Subsector:";
    private static final String LABEL_POPULATE = "Populate:";
    private static final String LABEL_TECHS = "Tech(s): ";
    private static final String LABEL_UNITS = "Units:";
    private static final String LABEL_APPLICATION_MODE = "Apply to:";
    private static final String MODE_FLEET_AVERAGE = "All Stock";
    private static final String MODE_NEW_SALES = "Sales";
    private static final String[] APPLICATION_MODE_OPTIONS = {MODE_FLEET_AVERAGE, MODE_NEW_SALES};
    private static final String[] SUBSECTOR_OPTIONS = {"Select One", "Car", "Large Car and Truck", "Light Truck", "Medium Truck", "Heavy Truck"};
    private static final String[] TECH_OPTIONS = {"BEV", "FCEV", "Hybrid Liquids", "Liquids", "NG"};
    private static final String[] UNITS_OPTIONS = {"Select One", "MPG", "MJ/vkt"};
    private static final String[] MOD_TYPE_OPTIONS = {
            "Initial and Final", "Initial w/% Growth/yr",
            "Initial w/% Growth/pd", "Initial w/Delta/yr", "Initial w/Delta/pd"
    };
    private static final String HEADER_PART1 = "GLIMPSECAFETargets";
    private static final String HEADER_PART2 = "GLIMPSEPFStdActivate";
    private static final String INPUT_TABLE = "INPUT_TABLE";
    private static final String VARIABLE_ID = "Variable ID";
    private static final String NO_MATCH = "No match";
    private static final String SELECT_ONE = "Select One";
    private static final String REG = "Reg";
    private static final String MARKET_SUFFIX = "_Mkt";
    private static final String METADATA_APPLICATION_MODE = "#Application mode: ";

    // === UI Components ===
    //private final GridPane gridPanePresetModification = new GridPane();
    //private final GridPane gridPaneLeft = new GridPane();
    private final Label labelComboBoxSubsector = utils.createLabel(LABEL_SUBSECTOR,LABEL_WIDTH);
    private final ComboBox<String> comboBoxSubsector = utils.createComboBoxString(PREF_WIDTH);
    private final Label labelCheckComboBoxTech = utils.createLabel(LABEL_TECHS, LABEL_WIDTH);
    private final CheckComboBox<String> checkComboBoxTech = utils.createCheckComboBox(PREF_WIDTH);
    private final Label labelWhichUnits = utils.createLabel(LABEL_UNITS, LABEL_WIDTH);
    private final ComboBox<String> comboBoxWhichUnits = utils.createComboBoxString(PREF_WIDTH);
    private final Label labelApplicationMode = utils.createLabel(LABEL_APPLICATION_MODE, LABEL_WIDTH);
    private final ComboBox<String> comboBoxApplicationMode = utils.createComboBoxString(PREF_WIDTH);
    // HBox for Auto and Unique checkboxes
    private final HBox hBoxAutoUnique = new HBox(8); // spacing of 8
    
    /**
     * Constructs a new TabCafeStd instance and initializes the UI components for the CAFE Standard tab.
     * Sets up event handlers and populates controls with available data.
     *
     * @param title The title of the tab
     * @param stageX The JavaFX stage
     */
    public TabCafeStd(String title, Stage stageX) {
        // Set tab title and style
        this.setText(title);
        this.setStyle(styles.getFontStyle());

        // Set up initial state of check box and policy and market textfields
        checkBoxUseAutoNames.setSelected(true);
        checkBoxUseUniqueNames.setSelected(true);
        textFieldPolicyName.setDisable(true);
        textFieldMarketName.setDisable(true);

        // Setup UI controls and layout
        setupUIControls(); // Populate combo boxes and set initial selections
        setupUIComponents(); // Add controls to layout
        setupUILayout(); // Arrange panes and layout
        setComponentWidths(); // Set preferred/min/max widths
        setupEventHandlers(); // Set up listeners and handlers
        setPolicyAndMarketNames(); // Auto-generate names
        setUnitsLabel(); // Set units label based on tech selection
        
        // Update policy and market names when region tree changes
        paneForCountryStateTree.getTree().addEventHandler(ActionEvent.ACTION, e -> {
            setPolicyAndMarketNames();
        });
    }

    /**
     * Sets up all UI components by delegating to column setup methods.
     * Calls setupLeftColumn, setupCenterColumn, and setupRightColumn.
     */
    public void setupUIComponents() {
        setupLeftColumn(); // Left column: labels and controls
        setupCenterColumn(); // Center column: details table
        setupRightColumn(); // Right column: region tree
    }
    
    /**
     * Sets up the UI controls (combo boxes, check combo boxes, etc.).
     * Populates combo boxes and sets initial selections.
     * Delegates to setupLeftColumn, setupCenterColumn, setupRightColumn.
     */
    private void setupUIControls() {
        comboBoxSubsector.getItems().addAll(SUBSECTOR_OPTIONS);
        comboBoxSubsector.getSelectionModel().select(SELECT_ONE);
        checkComboBoxTech.getItems().addAll(TECH_OPTIONS);
        checkComboBoxTech.getCheckModel().checkAll(); // Default: all techs selected
        checkComboBoxTech.setDisable(true); // Disabled until subsector selected
        comboBoxWhichUnits.getItems().addAll(UNITS_OPTIONS);
        comboBoxWhichUnits.getSelectionModel().select("MPG");
        comboBoxWhichUnits.setDisable(true); // Disabled until tech selected
        comboBoxApplicationMode.getItems().addAll(APPLICATION_MODE_OPTIONS);
        comboBoxApplicationMode.getSelectionModel().select(MODE_FLEET_AVERAGE);
        comboBoxModificationType.getItems().addAll(MOD_TYPE_OPTIONS);
        comboBoxModificationType.getSelectionModel().selectFirst();
    }

    /**
     * Sets up the left column of the UI, adding labels and controls to the grid pane.
     * Arranges labels and controls for specification, subsector, technologies, units, and other parameters.
     */
    private void setupLeftColumn() {
        gridPaneLeft.add(utils.createLabel(LABEL_SPECIFICATION), 0, 0, 2, 1);
        // Add checkboxes to HBox for auto/unique naming
        hBoxAutoUnique.getChildren().clear();
        hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
        // Add labels and controls to grid
        gridPaneLeft.addColumn(0, labelComboBoxSubsector, labelCheckComboBoxTech,  
                labelWhichUnits, labelApplicationMode, new Separator(), utils.createLabel(LABEL_NAMES), labelPolicyName, labelMarketName,
                new Label(), new Separator(), utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear,
                labelEndYear, labelInitialAmount, labelGrowth);
        gridPaneLeft.addColumn(1, comboBoxSubsector, checkComboBoxTech,  
                comboBoxWhichUnits, comboBoxApplicationMode, new Separator(), hBoxAutoUnique, textFieldPolicyName,
                textFieldMarketName, new Label(), new Separator(), new Label(), comboBoxModificationType,
                textFieldStartYear, textFieldEndYear, textFieldInitialAmount, textFieldGrowth);
        gridPaneLeft.setAlignment(Pos.TOP_LEFT);
        gridPaneLeft.setVgap(3.);
        //gridPaneLeft.setStyle(styles.getStyle2());
        scrollPaneLeft.setContent(gridPaneLeft);
    }

    /**
     * Sets preferred, min, and max widths for UI components.
     * Applies sizing to combo boxes and text fields for consistent layout.
     */
    private void setComponentWidths() {
        ComboBox<?>[] comboBoxes = {comboBoxSubsector, comboBoxWhichUnits, comboBoxApplicationMode, comboBoxModificationType};
        TextField[] textFields = {textFieldStartYear, textFieldEndYear, textFieldInitialAmount, textFieldGrowth, textFieldPeriodLength, textFieldPolicyName, textFieldMarketName};
        for (ComboBox<?> cb : comboBoxes) {
            cb.setMaxWidth(MAX_WIDTH);
            cb.setMinWidth(MIN_WIDTH);
            cb.setPrefWidth(PREF_WIDTH);
        }
        checkComboBoxTech.setMaxWidth(MAX_WIDTH);
        checkComboBoxTech.setMinWidth(MIN_WIDTH);
        checkComboBoxTech.setPrefWidth(PREF_WIDTH);
        for (TextField tf : textFields) {
            tf.setMaxWidth(MAX_WIDTH);
            tf.setMinWidth(MIN_WIDTH);
            tf.setPrefWidth(PREF_WIDTH);
        }
    }

    /**
     * Sets up event handlers for UI components.
     * Handles user interactions such as combo box changes and button clicks.
     * Includes double-click toggling for technology selection and subsector-based enabling/disabling.
     */
    protected void setupEventHandlers() {
    	super.setupEventHandlers();
        // Double-click on tech label toggles all tech selections
        labelCheckComboBoxTech.setOnMouseClicked(e -> {
            if (!checkComboBoxTech.isDisabled()) {
                boolean isFirstItemChecked = checkComboBoxTech.getCheckModel().isChecked(0);
                if (e.getClickCount() == 2) {
                    if (isFirstItemChecked) {
                        checkComboBoxTech.getCheckModel().clearChecks(); // Uncheck all
                    } else {
                        checkComboBoxTech.getCheckModel().checkAll(); // Check all
                    }
                }
            }
        });
        // Enable/disable tech selection based on subsector
        comboBoxSubsector.setOnAction(e -> {
            if (comboBoxSubsector.getSelectionModel().getSelectedIndex() > 0) {
                checkComboBoxTech.setDisable(false); // Enable tech selection
            } else {
                checkComboBoxTech.setDisable(true); // Disable tech selection
            }
            setPolicyAndMarketNames(); // Update names when subsector changes
        });
        comboBoxApplicationMode.setOnAction(e -> setPolicyAndMarketNames());
    }

    private boolean isNewSalesMode() {
        String mode = comboBoxApplicationMode.getSelectionModel().getSelectedItem();
        return MODE_NEW_SALES.equals(mode);
    }

    private String getPolicyKeyForTargetYear(String basePolicyName, String targetYear, boolean newSalesMode) {
        // Keep policy keys target-year scoped so each target can activate independently.
        return targetYear + "_" + basePolicyName;
    }

    private String getMarketKeyForTargetYear(String baseMarketName, String policyKey, String targetYear, boolean newSalesMode) {
        if (!newSalesMode) {
            // Fleet Average follows legacy activate-table market naming based on policy key.
            return policyKey + "Mkt";
        }
        return targetYear + "_" + baseMarketName;
    }

    private boolean shouldApplyTargetToModelYear(boolean newSalesMode, int modelYear, int targetYear) {
        if (newSalesMode) {
            return modelYear == targetYear;
        }
        return modelYear >= vars.getCalibrationYear() && modelYear <= targetYear;
    }

    private Double safeParseDouble(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(trimmed);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer safeParseInt(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets the policy and market names automatically based on selected subsector and regions.
     * If auto-naming is enabled, updates the text fields accordingly.
     * Uses region, sector, and technology selections to build unique names.
     * Runs on the JavaFX Application Thread.
     */
    protected void setPolicyAndMarketNames() {
        Platform.runLater(() -> {
            if (checkBoxUseAutoNames.isSelected()) {
                String policyType = "mpgTgt";

                String sector = "--";
                String state = "--";
                try {
                    String s = comboBoxSubsector.getValue();
                    if (s != null && !s.equals(SELECT_ONE)) {
                        s = s.replace(" ", "_");
                        s = utils.capitalizeOnlyFirstLetterOfString(s);
                        sector = s;
                    }
                    String[] listOfSelectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
                    if (listOfSelectedLeaves.length > 0) {
                        listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
                        String stateStr = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
                        if (stateStr.length() < 9) {
                            state = stateStr;
                        } else {
                            state = REG;
                        }
                    }
                    String name = policyType + "_" + sector + "_" + state ;
                    // Clean up name string
                    name = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("--", "_").replaceAll("_-_", "_").replaceAll("---", "");
                    textFieldMarketName.setText(name + MARKET_SUFFIX);
                    textFieldPolicyName.setText(name);
                } catch (Exception e) {
                    System.out.println("Cannot auto-name market. Continuing.");
                }
            }
        });
    }

    /**
     * Runnable implementation. Triggers saving of the scenario component.
     * Calls saveScenarioComponent() on the JavaFX Application Thread.
     */
    @Override
    public void run() {
        Platform.runLater(() -> saveScenarioComponent());
    }

    /**
     * Saves the scenario component using the current UI state and selected regions.
     * Calls the overloaded saveScenarioComponent(TreeView) method.
     */
    @Override
    public void saveScenarioComponent() {
        saveScenarioComponent(paneForCountryStateTree.getTree());
    }

    /**
     * Saves the scenario component for the specified tree of regions.
     * Performs QA checks, generates unique IDs, and builds file content for export.
     *
     * @param tree The TreeView of regions
     */
    private void saveScenarioComponent(TreeView<String> tree) {
        if (!qaInputs()) {
            return;
        }

        String ID;
        if (checkBoxUseUniqueNames.isSelected()) {
            ID = utils.getUniqueString();
        } else {
            ID = "";
        }

        String policyName = textFieldPolicyName.getText() + ID;
        String marketName = textFieldMarketName.getText() + ID;
        boolean newSalesMode = isNewSalesMode();
        filenameSuggestion = textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
        fileContent = getMetaDataContent(tree, marketName, policyName);

        StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
            .append(VARIABLE_ID).append(vars.getEol())
            .append(HEADER_PART1).append(vars.getEol()).append(vars.getEol())
            .append("region,sector,subsector,tech,year,input,coefficient,policy,output-ratio,pMultiplier").append(vars.getEol());

        StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
            .append(VARIABLE_ID).append(vars.getEol())
            .append(HEADER_PART2).append(vars.getEol()).append(vars.getEol())
            .append("region,policy,market,type,year,constrained").append(vars.getEol());

        String[] listOfSelectedLeaves = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
        ArrayList<String> dataArrayList = paneForComponentDetails.getDataYrValsArrayList();

        final double gj_per_gal = 0.1203; // Gasoline LHV (MJ/gal) for CAFE-like comparability.
        final double mj_per_gal = 120.3;
        final double km_per_mile = 1.61;
        final double mj_per_ej = 1e12;
        final double km_per_bln_km = 1e9;
        final double km_per_mln_km = 1e6;
        final double veh_per_bln_veh = 1e9;
        final double veh_per_mln_veh = 1e6;
        final double km_per_thous_km = 1e3;
        final double mj_per_MMBTU = 1055.1;
        final double gj_per_MMBTU = 1.055;

        int skippedInvalidRows = 0;
        int writtenConstraintRows = 0;

        for (String region : listOfSelectedLeaves) {
            String subsector = comboBoxSubsector.getValue();
            String sector = (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
                ? "trn_freight_road" : "trn_pass_road_LDV_4W";

            for (String data : dataArrayList) {
                String[] split = utils.splitString(data.replaceAll(" ", "").trim(), ",");
                if (split == null || split.length < 2) {
                    skippedInvalidRows++;
                    continue;
                }

                String targetYearStr = split[0];
                Integer targetYearParsed = safeParseInt(targetYearStr);
                Double targetValueParsed = safeParseDouble(split[1]);
                if (targetYearParsed == null || targetValueParsed == null) {
                    skippedInvalidRows++;
                    continue;
                }
                int targetYear = targetYearParsed;
                double targetMilesPerGal = targetValueParsed;

                if (targetMilesPerGal <= 0.0 || !Double.isFinite(targetMilesPerGal)) {
                    skippedInvalidRows++;
                    continue;
                }

                String io = getPolicyKeyForTargetYear(policyName, targetYearStr, newSalesMode);
                String iom = getMarketKeyForTargetYear(marketName, io, targetYearStr, newSalesMode);
                boolean activationWritten = false;
                boolean hasValidConstraintRow = false;

                for (Integer modelYear : vars.getAllowablePolicyYears()) { 
                    if (!shouldApplyTargetToModelYear(newSalesMode, modelYear, targetYear)) {
                        continue;
                    }

                    String modelYearStr = Integer.toString(modelYear);
                    ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

                    for (int techIndex = 0; techIndex < techList.size(); techIndex++) {
                        String tech = techList.get(techIndex);
                        String loadStr = utils.getTrnVehInfo("load", region, sector, subsector, tech, modelYearStr);
                        Double loadParsed = safeParseDouble(loadStr);
                        double load = (loadParsed != null) ? loadParsed : 0.0;

                        // Intensity-based CAFE formulation: use UCD transport intensity as the exported coefficient value.
                        String coefStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
                        Double coefParsed = safeParseDouble(coefStr);
                        double coef = (coefParsed != null) ? coefParsed : 5000.0;

                        if (!Double.isFinite(load) || load <= 0.0 || !Double.isFinite(coef) || coef <= 0.0) {
                            skippedInvalidRows++;
                            continue;
                        }

                        double outputRatio;
                        double pMultiplier;
                        boolean useMMBTUConversions = vars.getUseTrnMMBTUConversions();

                        if (useMMBTUConversions) {
                            // legacy GCAM-USA 8.2
                            //outputRatio = load / ( targetMilesPerGal * coef * km_per_mile / mj_per_gal * mj_per_ej / km_per_mln_km );
                        	//outputRatio = load / ( targetMilesPerGal * coef * km_per_mile / mj_per_gal * mj_per_MMBTU / km_per_mln_km * 1e12);
                            //pMultiplier = 1.0;
                            outputRatio = (1.0 / targetMilesPerGal * coef / km_per_mile * mj_per_gal / mj_per_MMBTU / km_per_mln_km / 1e12);
                            pMultiplier = 1;
                        } else {
                            // GCAM 8.5+
                            outputRatio = 1.0 / ( targetMilesPerGal * km_per_mile / mj_per_gal * mj_per_ej / km_per_bln_km );
                            pMultiplier = 1.0;
                        }

                        if (!Double.isFinite(outputRatio) || outputRatio <= 0.0 || !Double.isFinite(pMultiplier) || pMultiplier <= 0.0) {
                            skippedInvalidRows++;
                            continue;
                        }

                        contentP1.append(region).append(",").append(sector).append(",").append(subsector).append(",")
                            .append(tech).append(",").append(modelYearStr).append(",").append(io).append(",")
                            .append(coef).append(",").append(io).append(",").append(outputRatio).append(",")
                            .append(pMultiplier).append(vars.getEol());
                        hasValidConstraintRow = true;
                        writtenConstraintRows++;

                        if (!activationWritten && hasValidConstraintRow && techIndex == 0) {
                            contentP2.append(region).append(",").append(io).append(",").append(iom).append(",RES,")
                                .append(targetYearStr).append(",1").append(vars.getEol());
                            activationWritten = true;
                        }
                    }
                }
            }
        }

        if (writtenConstraintRows == 0) {
            utils.warningMessage("No valid CAFE constraint rows were generated.\n"
                    + "Please verify target table values and transport metadata (load/intensity) for selected years.");
            return;
        }

        if (skippedInvalidRows > 0) {
            System.out.println("Skipped invalid CAFE rows: " + skippedInvalidRows);
        }

        fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();
        System.out.println("Done");
    }


    /**
     * Generates the metadata content string for the scenario component, including selected subsector, technologies, units, policy/market names, and table data.
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
        rtnStr.append("#Subsector: ").append(comboBoxSubsector.getValue()).append(vars.getEol());
        ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();
        String techs = utils.getStringFromList(techList, ";");
        rtnStr.append("#Technologies: ").append(techs).append(vars.getEol());
        rtnStr.append("#Units: ").append(comboBoxWhichUnits.getValue()).append(vars.getEol());
        rtnStr.append(METADATA_APPLICATION_MODE).append(comboBoxApplicationMode.getValue()).append(vars.getEol());
        rtnStr.append("#Policy name: ").append(policy).append(vars.getEol());
        rtnStr.append("#Market name: ").append(market).append(vars.getEol());
        appendTransportConversionMetadata(rtnStr);
        String[] listOfSelectedLeaves = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
        String states = utils.returnAppendedString(listOfSelectedLeaves);
        rtnStr.append("#Regions: ").append(states).append(vars.getEol());
        ArrayList<String> tableContent = paneForComponentDetails.getDataYrValsArrayList();
        for (String row : tableContent) {
            rtnStr.append("#Table data:").append(row).append(vars.getEol());
        }
        rtnStr.append("#################################################").append(vars.getEol());
        return rtnStr.toString();
    }

    /**
     * Loads content from a list of strings (typically from a file) and populates the UI fields accordingly.
     * Parses each line and updates the corresponding UI control.
     *
     * @param content The list of content lines to load
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        if (content == null) {
            return;
        }
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
                switch (param) {
                    case "subsector":
                        comboBoxSubsector.setValue(value);
                        comboBoxSubsector.fireEvent(new ActionEvent());
                        break;
                    case "technologies":
                        checkComboBoxTech.getCheckModel().clearChecks();
                        String[] set = utils.splitString(value, ";");
                        for (String item : set) {
                            checkComboBoxTech.getCheckModel().check(item.trim());
                            checkComboBoxTech.fireEvent(new ActionEvent());
                        }
                        break;
                    case "units":
                        comboBoxWhichUnits.setValue(value);
                        comboBoxWhichUnits.fireEvent(new ActionEvent());
                        break;
                    case "application mode":
                        if (APPLICATION_MODE_OPTIONS[0].equals(value) || APPLICATION_MODE_OPTIONS[1].equals(value)) {
                            comboBoxApplicationMode.setValue(value);
                            comboBoxApplicationMode.fireEvent(new ActionEvent());
                        }
                        break;
                    case "policy name":
                        textFieldPolicyName.setText(value);
                        textFieldPolicyName.fireEvent(new ActionEvent());
                        break;
                    case "market name":
                        textFieldMarketName.setText(value);
                        textFieldMarketName.fireEvent(new ActionEvent());
                        break;
                    case "regions":
                        String[] regions = utils.splitString(value, ",");
                        paneForCountryStateTree.selectNodes(regions);
                        break;
                    case "table data":
                        String[] s = utils.splitString(value, ",");
                        if (s.length >= 2) {
                            paneForComponentDetails.addTableRow(s[0], s[1]);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        paneForComponentDetails.updateTable();
        setPolicyAndMarketNames();
        setUnitsLabel();

        showTransportConversionMetadataWarnings(transportWarnings);
    }

    /**
     * Helper method to validate table data years against allowable policy years.
     * Checks if at least one year in the table matches allowable years.
     *
     * @return true if at least one year matches allowable years, false otherwise
     */
    private boolean validateTableDataYears() {
        List<Integer> listOfAllowableYears = vars.getAllowablePolicyYears();
        ObservableList<DataPoint> data = paneForComponentDetails != null ? this.paneForComponentDetails.table.getItems() : null;
        if (data == null) return false;
        for (DataPoint dp : data) {
            try {
                Integer year = Integer.parseInt(dp.getYear().trim());
                if (listOfAllowableYears.contains(year)) {
                    return true;
                }
            } catch (NumberFormatException nfe) {
                // ignore invalid year entries
            }
        }
        return false;
    }
    
    /**
     * Performs QA checks on the current UI state to ensure all required inputs are valid.
     * Displays warnings or error messages as needed.
     *
     * @return true if all inputs are valid, false otherwise
     */
    protected boolean qaInputs() {
        TreeView<String> tree = paneForCountryStateTree.getTree();
        int errorCount = 0;
        StringBuilder message = new StringBuilder();
        try {
            // Check for at least one selected region
            if (utils.getAllSelectedRegions(tree).length < 1) {
                message.append("Must select at least one region from tree").append(vars.getEol());
                errorCount++;
            }
            // Check for at least one data entry
            if (paneForComponentDetails.table.getItems().isEmpty()) {
                message.append("Data table must have at least one entry").append(vars.getEol());
                errorCount++;
            } else {
                boolean match = validateTableDataYears();
                if (!match) {
                    message.append("Years specified in table must match allowable policy years").append(vars.getEol());
                    errorCount++;
                }
            }
            // Check subsector selection
            String selectedSubsector = comboBoxSubsector.getSelectionModel().getSelectedItem();
            if (selectedSubsector == null || selectedSubsector.equals(SELECT_ONE)) {
                message.append("Sector comboBox must have a selection").append(vars.getEol());
                errorCount++;
            }
            // Check tech selection
            if (checkComboBoxTech == null || checkComboBoxTech.getCheckModel().getCheckedItems().isEmpty()) {
                message.append("Tech checkComboBox must have at least one selection").append(vars.getEol());
                errorCount++;
            }
            // Check units selection
            String selectedUnits = comboBoxWhichUnits.getSelectionModel().getSelectedItem();
            if (selectedUnits == null || selectedUnits.equals(SELECT_ONE)) {
                message.append("Treatment comboBox must have a selection").append(vars.getEol());
                errorCount++;
            }
            // Check market and policy name fields
            if (textFieldMarketName.getText() == null || textFieldMarketName.getText().isEmpty()) {
                message.append("A market name must be provided").append(vars.getEol());
                errorCount++;
            }
            if (textFieldPolicyName.getText() == null || textFieldPolicyName.getText().isEmpty()) {
                message.append("A policy name must be provided").append(vars.getEol());
                errorCount++;
            }
        } catch (Exception e1) {
            errorCount++;
            message.append("Error in QA of entries").append(vars.getEol());
        }
        // Display warnings or errors if any
        if (errorCount > 0) {
            if (errorCount == 1) {
                utils.warningMessage(message.toString());
            } else if (errorCount > 1) {
                utils.displayString(message.toString(), "Parsing Errors");
            }
        }
        return errorCount == 0;
    }

    /**
     * Sets the units label based on the selected technologies.
     * If units are inconsistent, sets a warning label.
     * Optionally updates a UI label if needed.
     */
    public void setUnitsLabel() {
        String s = getUnits();
        if (NO_MATCH.equals(s)) {
        } else {
        }
        // Optionally update a UI label here if needed
    }

    /**
     * Returns the units string for the selected technologies in the tech combo box.
     * If units are inconsistent, returns "No match".
     *
     * @return The units string, or "No match" if units are inconsistent
     */
    public String getUnits() {
        ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();
        String unit = "";
        for (String line : techList) {
            String item = "";
            try {
                // Extract unit from tech string (after last colon)
                item = line.substring(line.lastIndexOf(":") + 1).trim();
                if (unit.isEmpty()) {
                    unit = item;
                } else if (!unit.equals(item)) {
                    unit = NO_MATCH;
                }
            } catch (Exception e) {
                item = "";
            }
        }
        if (unit.trim().equals("Select One or More")) unit = "";
        return unit;
    }
}
