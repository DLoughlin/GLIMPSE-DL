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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    //private static final String HEADER_PART1 = "GLIMPSECAFETargets";
    private static final String HEADER_PART1 =
    	    "GLIMPSECAFETargets-update";//, "
//    	    + "world/+{name;nocreate=1}region, "
//    	    + "region/+{name;nocreate=1}supplysector, "
//    	    + "supplysector/+{name;nocreate=1}tranSubsector, "
//    	    + "tranSubsector/+{name;nocreate=1}tranTechnology, "
//    	    + "tranTechnology/+{year}period, "
//    	    + "period/+{name}input-tax, "
//    	    + "input-tax/+current-coef, "
//    	    + "scenario, scenario/world";
    private static final String HEADER_PART2 =
    	    "GLIMPSECAFETargetsPolicy-update";//, "
//    	    + "world/+{name;nocreate=1}region, "
//    	    + "region/+{name}policy-portfolio-standard, "
//    	    + "policy-portfolio-standard/+market, "
//    	    + "policy-portfolio-standard/+policyType, "
//    	    + "policy-portfolio-standard/+{year}constraint, "
//    	    + "constraint, "
//    	    + "scenario, scenario/world";

	private static final String HEADER_PART1_TAX =
		    "GLIMPSECAFETargetsTax-update";
	private static final String HEADER_PART1_SUBSIDY =
		    "GLIMPSECAFETargetsSubsidy-update";
	
	private static final String HEADER_TAX_1SIDE_PART1 = "GLIMPSECAFETargetsTax-1side";
	private static final String HEADER_TAX_1SIDE_PART2 = "GLIMPSECAFETargetsPolicyTax-1side";
	private static final String HEADER_TAXSUBSIDY_2SIDE_PART1 = "GLIMPSECAFETargetsTax-2side";
	private static final String HEADER_TAXSUBSIDY_2SIDE_PART2 = "GLIMPSECAFETargetsSubsidy-2side";
	private static final String HEADER_TAXSUBSIDY_2SIDE_PART3 = "GLIMPSECAFETargetsPolicyTax-2side";
	
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
        saveScenarioComponentAlt7(paneForCountryStateTree.getTree());
    }

private void saveScenarioComponentAlt1(TreeView<String> tree) {
    if (!qaInputs()) {
        return;
    }

    final String idSuffix = checkBoxUseUniqueNames.isSelected()
        ? utils.getUniqueString()
        : "";

    final String basePolicyName = textFieldPolicyName.getText();
    final String baseMarketName = textFieldMarketName.getText();

    final String policyName = basePolicyName + idSuffix;
    final String marketName = baseMarketName + idSuffix;
    final boolean newSalesMode = isNewSalesMode();

    // ---------------------------------------------------------------------
    // This implementation generates a one-sided tax only.
    //
    // Assumptions:
    // - intensity values from trn_veh_info_8.5.csv are on a vehicle-distance
    //   basis and can be compared directly to a target intensity derived from
    //   MPGe:
    //
    //       targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe)
    //
    // - tax is only applied to technologies worse than the target:
    //
    //       rawGap = techIntensityVehicle - targetIntensityVehicle
    //
    // - the written tax coefficient is load-adjusted:
    //
    //       taxCoefficient = rawGap / load
    //
    // CSV part 1 row shape:
    //   region,sector,subsector,tech,year,input-tax-name,current-coef
    //
    // CSV part 2 row shape:
    //   region,policy-name,market-name,policy-type,year,constraint
    //
    // IMPORTANT:
    // HEADER_PART1 and HEADER_PART2 must match these row widths.
    // ---------------------------------------------------------------------

    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
    fileContent = getMetaDataContent(tree, marketName, policyName);

    // ---------------------------------------------------------------------
    // Part 1: technology tax rows
    // ---------------------------------------------------------------------
    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_TAX_1SIDE_PART1).append(vars.getEol()).append(vars.getEol())
        .append("region,sector,subsector,tech,year,input-tax-name,current-coef")
        .append(vars.getEol());

    // ---------------------------------------------------------------------
    // Part 2: policy rows
    // ---------------------------------------------------------------------
    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_TAX_1SIDE_PART2).append(vars.getEol()).append(vars.getEol())
        .append("region,policy-name,market-name,policy-type,year,constraint")
        .append(vars.getEol());

    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

    final double gjPerGal = 0.1203;
    final double kmPerMile = 1.61;

    int skippedInvalidRows = 0;
    int writtenConstraintRows = 0;

    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
    Map<String, Integer> taxedTechsPerYear = new LinkedHashMap<>();

    System.out.println("======================================================");
    System.out.println("Generating MPG Target Scenario Component (one-sided tax + load adjustment)");
    System.out.println("Policy name base: " + basePolicyName);
    System.out.println("Market name base: " + baseMarketName);
    System.out.println("Policy name final: " + policyName);
    System.out.println("Market name final: " + marketName);
    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
    System.out.println("New sales mode: " + newSalesMode);
    System.out.println("Intensity basis assumption: vehicle-distance based");
    System.out.println("Tax coefficient adjustment: divide rawGap by load");
    System.out.println("======================================================");

    for (String region : selectedRegions) {
        final String subsector = comboBoxSubsector.getValue();
        final String sector =
            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
                ? "trn_freight_road"
                : "trn_pass_road_LDV_4W";

        for (String tableRow : targetTableRows) {
            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
            if (split == null || split.length < 2) {
                skippedInvalidRows++;
                continue;
            }

            final String targetYearStr = split[0];
            final Integer targetYearParsed = safeParseInt(targetYearStr);
            final Double targetValueParsed = safeParseDouble(split[1]);

            if (targetYearParsed == null || targetValueParsed == null) {
                skippedInvalidRows++;
                continue;
            }

            final int targetYear = targetYearParsed;
            final double targetMPGe = targetValueParsed;

            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
                skippedInvalidRows++;
                continue;
            }

            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);

            boolean taxActivationWritten = false;

            final double targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe);

            System.out.println();
            System.out.println("------------------------------------------------------");
            System.out.println("Region: " + region);
            System.out.println("Subsector: " + subsector);
            System.out.println("Target year: " + targetYear);
            System.out.println("Target MPGe: " + targetMPGe);
            System.out.println("Tax policy: " + taxPolicyKey);
            System.out.println("Tax market: " + taxMarketKey);
            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
            System.out.println("------------------------------------------------------");

            for (Integer modelYear : vars.getAllowablePolicyYears()) {
                final boolean applyThisTarget;
                if (newSalesMode) {
                    applyThisTarget = (modelYear == targetYear);
                } else {
                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
                }

                System.out.println(
                    "Target-year applicability => " +
                    "targetYear=" + targetYear +
                    ", modelYear=" + modelYear +
                    ", newSalesMode=" + newSalesMode +
                    ", apply=" + applyThisTarget
                );

                if (!applyThisTarget) {
                    continue;
                }

                final String modelYearStr = Integer.toString(modelYear);
                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

                for (String tech : techList) {
                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
                    final String loadStr = utils.getTrnVehInfo("load", region, sector, subsector, tech, modelYearStr);

                    if (intensityStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing intensity metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
                    final double techIntensityVehicle = (rawIntensityParsed != null) ? rawIntensityParsed : Double.NaN;

                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid intensity:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", intensity=" + techIntensityVehicle);
                        continue;
                    }

                    if (loadStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing load metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double loadParsed = safeParseDouble(loadStr);
                    final double load = (loadParsed != null) ? loadParsed : Double.NaN;

                    if (!Double.isFinite(load) || load <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid load:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", load=" + load);
                        continue;
                    }

                    final double rawGap = techIntensityVehicle - targetIntensityVehicle;

                    final double techBackConvertedMPGeVehicle =
                        gjPerGal / (kmPerMile * techIntensityVehicle);

                    final double targetBackConvertedMPGeVehicle =
                        gjPerGal / (kmPerMile * targetIntensityVehicle);

                    System.out.println(
                        "Tech debug => " +
                        "region=" + region +
                        ", subsector=" + subsector +
                        ", tech=" + tech +
                        ", targetYear=" + targetYearStr +
                        ", modelYear=" + modelYearStr +
                        ", load=" + load +
                        ", techIntensityVehicle=" + techIntensityVehicle +
                        ", targetIntensityVehicle=" + targetIntensityVehicle +
                        ", rawGap(tech-target)=" + rawGap +
                        ", loadAdjustedTaxGap=" + (rawGap / load) +
                        ", techBackConvertedMPGeVehicle=" + techBackConvertedMPGeVehicle +
                        ", targetBackConvertedMPGeVehicle=" + targetBackConvertedMPGeVehicle
                    );

                    if (rawGap > 0.0) {
                        final double taxCoefficient = rawGap / load;

                        contentP1.append(region).append(",")
                            .append(sector).append(",")
                            .append(subsector).append(",")
                            .append(tech).append(",")
                            .append(modelYearStr).append(",")
                            .append(taxPolicyKey).append(",")
                            .append(taxCoefficient)
                            .append(vars.getEol());

                        writtenConstraintRows++;
                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);
                        taxedTechsPerYear.put(targetYearStr, taxedTechsPerYear.getOrDefault(targetYearStr, 0) + 1);

                        if (!taxActivationWritten) {
                            contentP2.append(region).append(",")
                                .append(taxPolicyKey).append(",")
                                .append(taxMarketKey).append(",")
                                .append("tax").append(",")
                                .append(targetYearStr).append(",")
                                .append("1")
                                .append(vars.getEol());

                            taxActivationWritten = true;
                        }
                    } else {
                        System.out.println("No tax row written; tech is at or better than target:"
                            + " tech=" + tech
                            + ", modelYear=" + modelYearStr);
                    }
                }
            }
        }
    }

    if (writtenConstraintRows == 0) {
        utils.warningMessage(
            "No valid one-sided tax rows were generated.\n" +
            "Please verify target values and transport intensity/load metadata."
        );
        return;
    }

    if (skippedInvalidRows > 0) {
        System.out.println("Skipped invalid one-sided tax rows: " + skippedInvalidRows);
    }

    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

    System.out.println("======================================================");
    System.out.println("Finished generating MPG Target Scenario Component (one-sided tax + load adjustment)");
    System.out.println("Written constraint rows: " + writtenConstraintRows);
    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

    System.out.println("--- Rows per target year ---");
    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("--- Rows per model year ---");
    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("--- Taxed technologies per target year ---");
    for (Map.Entry<String, Integer> e : taxedTechsPerYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", taxedTechCount=" + e.getValue());
    }

    System.out.println("======================================================");
}

private void saveScenarioComponentAlt2(TreeView<String> tree) {
    if (!qaInputs()) {
        return;
    }

    final String idSuffix = checkBoxUseUniqueNames.isSelected()
        ? utils.getUniqueString()
        : "";

    final String basePolicyName = textFieldPolicyName.getText();
    final String baseMarketName = textFieldMarketName.getText();

    final String policyName = basePolicyName + idSuffix;
    final String marketName = baseMarketName + idSuffix;
    final boolean newSalesMode = isNewSalesMode();

    // ---------------------------------------------------------------------
    // This implementation generates a two-sided tax/subsidy structure.
    //
    // Assumptions:
    // - intensity values from trn_veh_info_8.5.csv are on a vehicle-distance
    //   basis and can be compared directly to a target intensity derived from
    //   MPGe:
    //
    //       targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe)
    //
    // - the comparison to determine whether a technology is above or below
    //   the target is done on the vehicle basis:
    //
    //       rawGap = techIntensityVehicle - targetIntensityVehicle
    //
    // - the written tax/subsidy coefficient is load-adjusted:
    //
    //       adjustedCoefficient = abs(rawGap) / load
    //
    // CSV part 1a row shape (tax rows):
    //   region,sector,subsector,tech,year,input-tax-name,current-coef
    //
    // CSV part 1b row shape (subsidy rows):
    //   region,sector,subsector,tech,year,input-subsidy-name,current-coef
    //
    // CSV part 2 row shape:
    //   region,policy-name,market-name,policy-type,year,constraint
    //
    // IMPORTANT:
    // HEADER_PART1_TAX, HEADER_PART1_SUBSIDY, and HEADER_PART2 must match
    // these row widths.
    // ---------------------------------------------------------------------

    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
    fileContent = getMetaDataContent(tree, marketName, policyName);

    // ---------------------------------------------------------------------
    // Part 1a: technology tax rows
    // ---------------------------------------------------------------------
    StringBuilder contentTax = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_TAXSUBSIDY_2SIDE_PART1).append(vars.getEol()).append(vars.getEol())
        .append("region,sector,subsector,tech,year,input-tax-name,current-coef")
        .append(vars.getEol());

    // ---------------------------------------------------------------------
    // Part 1b: technology subsidy rows
    // ---------------------------------------------------------------------
    StringBuilder contentSubsidy = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_TAXSUBSIDY_2SIDE_PART2).append(vars.getEol()).append(vars.getEol())
        .append("region,sector,subsector,tech,year,input-subsidy-name,current-coef")
        .append(vars.getEol());

    // ---------------------------------------------------------------------
    // Part 2: policy rows
    // ---------------------------------------------------------------------
    StringBuilder contentPolicy = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_TAXSUBSIDY_2SIDE_PART3).append(vars.getEol()).append(vars.getEol())
        .append("region,policy-name,market-name,policy-type,year,constraint")
        .append(vars.getEol());

    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

    final double gjPerGal = 0.1203;
    final double kmPerMile = 1.61;

    int skippedInvalidRows = 0;
    int writtenTaxRows = 0;
    int writtenSubsidyRows = 0;

    Map<String, Integer> taxRowsPerTargetYear = new LinkedHashMap<>();
    Map<String, Integer> subsidyRowsPerTargetYear = new LinkedHashMap<>();
    Map<String, Integer> totalRowsPerModelYear = new LinkedHashMap<>();

    System.out.println("======================================================");
    System.out.println("Generating MPG Target Scenario Component (two-sided tax/subsidy + load adjustment)");
    System.out.println("Policy name base: " + basePolicyName);
    System.out.println("Market name base: " + baseMarketName);
    System.out.println("Policy name final: " + policyName);
    System.out.println("Market name final: " + marketName);
    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
    System.out.println("New sales mode: " + newSalesMode);
    System.out.println("Intensity basis assumption: vehicle-distance based");
    System.out.println("Coefficient adjustment: divide abs(rawGap) by load");
    System.out.println("======================================================");

    for (String region : selectedRegions) {
        final String subsector = comboBoxSubsector.getValue();
        final String sector =
            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
                ? "trn_freight_road"
                : "trn_pass_road_LDV_4W";

        for (String tableRow : targetTableRows) {
            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
            if (split == null || split.length < 2) {
                skippedInvalidRows++;
                continue;
            }

            final String targetYearStr = split[0];
            final Integer targetYearParsed = safeParseInt(targetYearStr);
            final Double targetValueParsed = safeParseDouble(split[1]);

            if (targetYearParsed == null || targetValueParsed == null) {
                skippedInvalidRows++;
                continue;
            }

            final int targetYear = targetYearParsed;
            final double targetMPGe = targetValueParsed;

            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
                skippedInvalidRows++;
                continue;
            }

            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);

            final String subsidyPolicyKey = getPolicyKeyForTargetYear(policyName + "_subsidy", targetYearStr, newSalesMode);
            final String subsidyMarketKey = getMarketKeyForTargetYear(marketName + "_subsidy", subsidyPolicyKey, targetYearStr, newSalesMode);

            boolean taxActivationWritten = false;
            boolean subsidyActivationWritten = false;

            final double targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe);

            System.out.println();
            System.out.println("------------------------------------------------------");
            System.out.println("Region: " + region);
            System.out.println("Subsector: " + subsector);
            System.out.println("Target year: " + targetYear);
            System.out.println("Target MPGe: " + targetMPGe);
            System.out.println("Tax policy: " + taxPolicyKey);
            System.out.println("Tax market: " + taxMarketKey);
            System.out.println("Subsidy policy: " + subsidyPolicyKey);
            System.out.println("Subsidy market: " + subsidyMarketKey);
            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
            System.out.println("------------------------------------------------------");

            for (Integer modelYear : vars.getAllowablePolicyYears()) {
                final boolean applyThisTarget;
                if (newSalesMode) {
                    applyThisTarget = (modelYear == targetYear);
                } else {
                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
                }

                System.out.println(
                    "Target-year applicability => " +
                    "targetYear=" + targetYear +
                    ", modelYear=" + modelYear +
                    ", newSalesMode=" + newSalesMode +
                    ", apply=" + applyThisTarget
                );

                if (!applyThisTarget) {
                    continue;
                }

                final String modelYearStr = Integer.toString(modelYear);
                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

                for (String tech : techList) {
                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
                    final String loadStr = utils.getTrnVehInfo("load", region, sector, subsector, tech, modelYearStr);

                    if (intensityStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing intensity metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
                    final double techIntensityVehicle = (rawIntensityParsed != null) ? rawIntensityParsed : Double.NaN;

                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid intensity:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", intensity=" + techIntensityVehicle);
                        continue;
                    }

                    if (loadStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing load metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double loadParsed = safeParseDouble(loadStr);
                    final double load = (loadParsed != null) ? loadParsed : Double.NaN;

                    if (!Double.isFinite(load) || load <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid load:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", load=" + load);
                        continue;
                    }

                    final double rawGap = techIntensityVehicle - targetIntensityVehicle;
                    final double adjustedGap = Math.abs(rawGap) / load;

                    final double techBackConvertedMPGeVehicle =
                        gjPerGal / (kmPerMile * techIntensityVehicle);

                    final double targetBackConvertedMPGeVehicle =
                        gjPerGal / (kmPerMile * targetIntensityVehicle);

                    System.out.println(
                        "Tech debug => " +
                        "region=" + region +
                        ", subsector=" + subsector +
                        ", tech=" + tech +
                        ", targetYear=" + targetYearStr +
                        ", modelYear=" + modelYearStr +
                        ", load=" + load +
                        ", techIntensityVehicle=" + techIntensityVehicle +
                        ", targetIntensityVehicle=" + targetIntensityVehicle +
                        ", rawGap(tech-target)=" + rawGap +
                        ", loadAdjustedAbsGap=" + adjustedGap +
                        ", techBackConvertedMPGeVehicle=" + techBackConvertedMPGeVehicle +
                        ", targetBackConvertedMPGeVehicle=" + targetBackConvertedMPGeVehicle
                    );

                    if (rawGap > 0.0) {
                        contentTax.append(region).append(",")
                            .append(sector).append(",")
                            .append(subsector).append(",")
                            .append(tech).append(",")
                            .append(modelYearStr).append(",")
                            .append(taxPolicyKey).append(",")
                            .append(adjustedGap)
                            .append(vars.getEol());

                        writtenTaxRows++;
                        taxRowsPerTargetYear.put(targetYearStr, taxRowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
                        totalRowsPerModelYear.put(modelYearStr, totalRowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

                        if (!taxActivationWritten) {
                            contentPolicy.append(region).append(",")
                                .append(taxPolicyKey).append(",")
                                .append(taxMarketKey).append(",")
                                .append("tax").append(",")
                                .append(targetYearStr).append(",")
                                .append("1")
                                .append(vars.getEol());
                            taxActivationWritten = true;
                        }
                    } else if (rawGap < 0.0) {
                        contentSubsidy.append(region).append(",")
                            .append(sector).append(",")
                            .append(subsector).append(",")
                            .append(tech).append(",")
                            .append(modelYearStr).append(",")
                            .append(subsidyPolicyKey).append(",")
                            .append(adjustedGap)
                            .append(vars.getEol());

                        writtenSubsidyRows++;
                        subsidyRowsPerTargetYear.put(targetYearStr, subsidyRowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
                        totalRowsPerModelYear.put(modelYearStr, totalRowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

                        if (!subsidyActivationWritten) {
                            contentPolicy.append(region).append(",")
                                .append(subsidyPolicyKey).append(",")
                                .append(subsidyMarketKey).append(",")
                                .append("subsidy").append(",")
                                .append(targetYearStr).append(",")
                                .append("1")
                                .append(vars.getEol());
                            subsidyActivationWritten = true;
                        }
                    } else {
                        System.out.println("No row written; tech is exactly at target:"
                            + " tech=" + tech
                            + ", modelYear=" + modelYearStr);
                    }
                }
            }
        }
    }

    if (writtenTaxRows == 0 && writtenSubsidyRows == 0) {
        utils.warningMessage(
            "No valid two-sided tax/subsidy rows were generated.\n" +
            "Please verify target values and transport intensity/load metadata."
        );
        return;
    }

    if (skippedInvalidRows > 0) {
        System.out.println("Skipped invalid two-sided rows: " + skippedInvalidRows);
    }

    fileContent += contentTax.toString()
        + vars.getEol()
        + contentSubsidy.toString()
        + vars.getEol()
        + contentPolicy.toString();

    System.out.println("======================================================");
    System.out.println("Finished generating MPG Target Scenario Component (two-sided tax/subsidy + load adjustment)");
    System.out.println("Written tax rows: " + writtenTaxRows);
    System.out.println("Written subsidy rows: " + writtenSubsidyRows);
    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

    System.out.println("--- Tax rows per target year ---");
    for (Map.Entry<String, Integer> e : taxRowsPerTargetYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", taxRows=" + e.getValue());
    }

    System.out.println("--- Subsidy rows per target year ---");
    for (Map.Entry<String, Integer> e : subsidyRowsPerTargetYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", subsidyRows=" + e.getValue());
    }

    System.out.println("--- Total rows per model year ---");
    for (Map.Entry<String, Integer> e : totalRowsPerModelYear.entrySet()) {
        System.out.println("modelYear=" + e.getKey() + ", totalRows=" + e.getValue());
    }

    System.out.println("======================================================");
}
    
	private void saveScenarioComponentOld(TreeView<String> tree) {
		
		if (!qaInputs()) {
			Thread.currentThread().destroy();
		} else {

			//// setting up policy name and suggested file name
			
			
			String ID = utils.getUniqueString();
			String policy_name = this.textFieldPolicyName.getText() + ID;
			String market_name = this.textFieldMarketName.getText() + ID;
			filenameSuggestion = this.textFieldPolicyName.getText().replaceAll("/", "-").replaceAll(" ", "_") + ".csv";

			//clearing info to save to file
			fileContent = this.getMetaDataContent(tree, market_name, policy_name);
			String content_p1="";
			String content_p2="";

			//// -----------getting selected regions info from GUI
			String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
			listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
			String states = utils.returnAppendedString(listOfSelectedLeaves);

			//// -----------getting constraint data from GUI
			
			// getting values for constraint
			ArrayList<String> dataArrayList = this.paneForComponentDetails.getDataYrValsArrayList();
			String[] year_list = new String[dataArrayList.size()];
			String[] value_list = new String[dataArrayList.size()];
			double[] valuef_list = new double[dataArrayList.size()];

			// setting up dates for iteration
			
			
			//// ------------ setting up headers
			String header_part1 = "GLIMPSECAFETargets";
			String header_part2 = "GLIMPSEPFStdActivate";
			
			//header 1:
			content_p1+="INPUT_TABLE" + vars.getEol();
			content_p1+="Variable ID" + vars.getEol();
			content_p1+=header_part1 + vars.getEol() + vars.getEol();
			content_p1+="region,sector,subsector,tech,year,input,coefficient,policy,output-ratio,pMultiplier" + vars.getEol();

			//header 2:
			content_p2+="INPUT_TABLE" + vars.getEol();
			content_p2+="Variable ID" + vars.getEol();
			content_p2+=header_part2 + vars.getEol() + vars.getEol();
			content_p2+="region,policy,market,type,year,constrained" + vars.getEol();			
			

			/////----- Constructing data components			
			
			//loop over regions
			for (int r = 0; r < listOfSelectedLeaves.length; r++) {
				String region = listOfSelectedLeaves[r];
			    
				//for each region, sector/subsector, get list of techs
				String subsector=comboBoxSubsector.getValue(); 
				String sector="";
				if ((subsector.equals("Light Truck"))||(subsector.equals("Medium Truck"))||(subsector.equals("Heavy Truck"))) {
					sector="trn_freight_road";
				} else {
					sector="trn_pass_road_LDV_4W";
				}

				
				for (int i = 0; i < dataArrayList.size(); i++) {
					String str = dataArrayList.get(i).replaceAll(" ", "").trim();
					year_list[i] = utils.splitString(str, ",")[0];
					value_list[i] = utils.splitString(str, ",")[1];
					valuef_list[i] = Double.parseDouble(value_list[i]);
					
				    String yr=year_list[i];
				    double val=valuef_list[i];
					
					ObservableList<String> tech_list=this.checkComboBoxTech.getCheckModel().getCheckedItems();
					
					for (int t=0;t<tech_list.size();t++) {
						String tech=tech_list.get(t);
						
					    String load_str=utils.getTrnVehInfo("load", region, sector, subsector, tech, yr);
					    if (load_str==null) {
					    	System.out.println("why null?");
					    }
					    double load=Double.parseDouble(load_str);
					
					    String coef_str=utils.getTrnVehInfo("intensity", region, sector, subsector, tech, yr);
					    if (coef_str==null) {
					    	//hack since NG vehicles are not in the coef list
					    	coef_str="5000";
					    	load=0.0;
					    }
					    double coef=Double.parseDouble(coef_str);
			
					    String io=yr+"_"+policy_name;
					    String iom=io+"Mkt";
					    
					    String outputratio=Double.toString((float)(1.0/val/1.61*131.76/1e6));
					    String pMultiplier=Double.toString((float)(load*1e9)); 
					    
					    content_p1+=region+","+sector+","+subsector+","+tech+","+yr+","+io+","+coef+","+io+","+outputratio+","+pMultiplier+ vars.getEol();
					    if (t==0) content_p2+=region+","+io+","+iom+",RES,"+yr+",1"+ vars.getEol();
					}
				}

				fileContent+=content_p1+ vars.getEol();
				fileContent+=content_p2;

			System.out.println("Done");
			}}

	}
    
	private void saveScenarioComponentAlt3(TreeView<String> tree) {
	    if (!qaInputs()) {
	        return;
	    }

	    final String idSuffix = checkBoxUseUniqueNames.isSelected()
	        ? utils.getUniqueString()
	        : "";

	    final String basePolicyName = textFieldPolicyName.getText();
	    final String baseMarketName = textFieldMarketName.getText();

	    final String policyName = basePolicyName + idSuffix;
	    final String marketName = baseMarketName + idSuffix;
	    final boolean newSalesMode = isNewSalesMode();

	    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
	    fileContent = getMetaDataContent(tree, marketName, policyName);

	    // ---------------------------------------------------------------------
	    // PART 1:
	    // Technology rows. These now use input-tax or input-subsidy rather than
	    // minicam-energy-input + res-secondary-output.
	    //
	    // PART 2:
	    // Policy activation rows. We create separate tax and subsidy markets.
	    // ---------------------------------------------------------------------
	    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_PART1).append(vars.getEol()).append(vars.getEol())
	        .append("region,sector,subsector,tech,year,input-type,input,coefficient,market-name")
	        .append(vars.getEol());

	    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_PART2).append(vars.getEol()).append(vars.getEol())
	        .append("region,policy,market,type,year,constrained")
	        .append(vars.getEol());

	    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
	    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

	    final double gj_per_gal = 0.1203; // GJ / gallon gasoline equivalent
	    final double km_per_mile = 1.61;
	    final double km_per_bln_km = 1e9;

	    int skippedInvalidRows = 0;
	    int writtenConstraintRows = 0;

	    // Debug bookkeeping
	    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();

	    System.out.println("======================================================");
	    System.out.println("Generating MPG Target Scenario Component (tax/subsidy feebate style)");
	    System.out.println("Policy name base: " + basePolicyName);
	    System.out.println("Market name base: " + baseMarketName);
	    System.out.println("Policy name final: " + policyName);
	    System.out.println("Market name final: " + marketName);
	    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
	    System.out.println("New sales mode: " + newSalesMode);
	    System.out.println("======================================================");

	    for (String region : selectedRegions) {
	        final String subsector = comboBoxSubsector.getValue();
	        final String sector =
	            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
	                ? "trn_freight_road"
	                : "trn_pass_road_LDV_4W";

	        for (String tableRow : targetTableRows) {
	            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
	            if (split == null || split.length < 2) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String targetYearStr = split[0];
	            final Integer targetYearParsed = safeParseInt(targetYearStr);
	            final Double targetValueParsed = safeParseDouble(split[1]);

	            if (targetYearParsed == null || targetValueParsed == null) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final int targetYear = targetYearParsed;
	            final double targetMilesPerGal = targetValueParsed;

	            if (targetMilesPerGal <= 0.0 || !Double.isFinite(targetMilesPerGal)) {
	                skippedInvalidRows++;
	                continue;
	            }

	            // Convert MPG target to intensity basis used for comparison.
	            final double targetIntensity =
	                (1.0 / targetMilesPerGal) / km_per_mile * gj_per_gal * km_per_bln_km / 1000.0;

	            // Create separate policy/market names for tax and subsidy.
	            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
	            final String subPolicyKey = getPolicyKeyForTargetYear(policyName + "_subsidy", targetYearStr, newSalesMode);

	            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);
	            final String subMarketKey = getMarketKeyForTargetYear(marketName + "_subsidy", subPolicyKey, targetYearStr, newSalesMode);

	            boolean taxActivationWritten = false;
	            boolean subActivationWritten = false;

	            System.out.println();
	            System.out.println("------------------------------------------------------");
	            System.out.println("Region: " + region);
	            System.out.println("Subsector: " + subsector);
	            System.out.println("Target year: " + targetYear);
	            System.out.println("Target MPG/MPGe: " + targetMilesPerGal);
	            System.out.println("Target intensity: " + targetIntensity);
	            System.out.println("Tax policy: " + taxPolicyKey);
	            System.out.println("Tax market: " + taxMarketKey);
	            System.out.println("Subsidy policy: " + subPolicyKey);
	            System.out.println("Subsidy market: " + subMarketKey);
	            System.out.println("------------------------------------------------------");

	            for (Integer modelYear : vars.getAllowablePolicyYears()) {
	                final boolean applyThisTarget;
	                if (newSalesMode) {
	                    applyThisTarget = (modelYear == targetYear);
	                } else {
	                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
	                }

	                System.out.println(
	                    "Target-year applicability => " +
	                    "targetYear=" + targetYear +
	                    ", modelYear=" + modelYear +
	                    ", newSalesMode=" + newSalesMode +
	                    ", apply=" + applyThisTarget
	                );

	                if (!applyThisTarget) {
	                    continue;
	                }

	                final String modelYearStr = Integer.toString(modelYear);
	                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

	                for (int techIndex = 0; techIndex < techList.size(); techIndex++) {
	                    final String tech = techList.get(techIndex);

	                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
	                    if (intensityStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing intensity metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
	                    final double rawTechIntensityGJPerMillionVkt = (rawIntensityParsed != null) ? rawIntensityParsed : 0.0;

	                    if (!Double.isFinite(rawTechIntensityGJPerMillionVkt) || rawTechIntensityGJPerMillionVkt <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid intensity:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", rawIntensity=" + rawTechIntensityGJPerMillionVkt);
	                        continue;
	                    }

	                    // Convert tech intensity to same exported basis as target.
	                    final double techIntensity = rawTechIntensityGJPerMillionVkt * 1000.0;

	                    // Positive => tech is worse than target => tax
	                    // Negative => tech is better than target => subsidy
	                    final double intensityGap = techIntensity - targetIntensity;

	                    final double techBackConvertedMPG =
	                        (techIntensity > 0.0)
	                            ? (gj_per_gal * km_per_bln_km / 1000.0) / (techIntensity * km_per_mile)
	                            : Double.NaN;

	                    final double targetBackConvertedMPG =
	                        (targetIntensity > 0.0)
	                            ? (gj_per_gal * km_per_bln_km / 1000.0) / (targetIntensity * km_per_mile)
	                            : Double.NaN;

	                    System.out.println(
	                        "Tech debug => " +
	                        "region=" + region +
	                        ", subsector=" + subsector +
	                        ", tech=" + tech +
	                        ", targetYear=" + targetYearStr +
	                        ", modelYear=" + modelYearStr +
	                        ", inputMPG=" + targetMilesPerGal +
	                        ", rawIntensity(GJ/million-vkt)=" + rawTechIntensityGJPerMillionVkt +
	                        ", techIntensity(exported)=" + techIntensity +
	                        ", targetIntensity=" + targetIntensity +
	                        ", intensityGap(tech-target)=" + intensityGap +
	                        ", techBackConvertedMPG=" + techBackConvertedMPG +
	                        ", targetBackConvertedMPG=" + targetBackConvertedMPG
	                    );

	                    // Tax side: tech worse than target.
	                    if (intensityGap > 0.0) {
	                        contentP1.append(region).append(",")
	                            .append(sector).append(",")
	                            .append(subsector).append(",")
	                            .append(tech).append(",")
	                            .append(modelYearStr).append(",")
	                            .append("input-tax").append(",")
	                            .append(taxPolicyKey).append(",")
	                            .append(intensityGap).append(",")
	                            .append(region)
	                            .append(vars.getEol());

	                        writtenConstraintRows++;
	                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

	                        if (!taxActivationWritten) {
	                            contentP2.append(region).append(",")
	                                .append(taxPolicyKey).append(",")
	                                .append(taxMarketKey).append(",tax,")
	                                .append(targetYearStr).append(",1")
	                                .append(vars.getEol());
	                            taxActivationWritten = true;
	                        }
	                    }
	                    // Subsidy side: tech better than target.
	                    else if (intensityGap < 0.0) {
	                        final double subsidyGap = -intensityGap;

	                        contentP1.append(region).append(",")
	                            .append(sector).append(",")
	                            .append(subsector).append(",")
	                            .append(tech).append(",")
	                            .append(modelYearStr).append(",")
	                            .append("input-subsidy").append(",")
	                            .append(subPolicyKey).append(",")
	                            .append(subsidyGap).append(",")
	                            .append(region)
	                            .append(vars.getEol());

	                        writtenConstraintRows++;
	                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

	                        if (!subActivationWritten) {
	                            contentP2.append(region).append(",")
	                                .append(subPolicyKey).append(",")
	                                .append(subMarketKey).append(",subsidy,")
	                                .append(targetYearStr).append(",1")
	                                .append(vars.getEol());
	                            subActivationWritten = true;
	                        }
	                    }
	                    else {
	                        System.out.println("Tech exactly at target; no tax/subsidy row written for tech=" + tech
	                            + ", modelYear=" + modelYearStr);
	                    }
	                }
	            }
	        }
	    }

	    if (writtenConstraintRows == 0) {
	        utils.warningMessage(
	            "No valid feebate constraint rows were generated.\n" +
	            "Please verify target table values and transport intensity metadata for selected years."
	        );
	        return;
	    }

	    if (skippedInvalidRows > 0) {
	        System.out.println("Skipped invalid feebate rows: " + skippedInvalidRows);
	    }

	    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

	    System.out.println("======================================================");
	    System.out.println("Finished generating MPG target scenario component (tax/subsidy feebate style)");
	    System.out.println("Written constraint rows: " + writtenConstraintRows);
	    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

	    System.out.println("--- Rows per target year ---");
	    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Rows per model year ---");
	    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
	        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
	    }
	    System.out.println("======================================================");
	}

	private void saveScenarioComponentAlt4(TreeView<String> tree) {
	    if (!qaInputs()) {
	        return;
	    }

	    final String idSuffix = checkBoxUseUniqueNames.isSelected()
	        ? utils.getUniqueString()
	        : "";

	    final String basePolicyName = textFieldPolicyName.getText();
	    final String baseMarketName = textFieldMarketName.getText();

	    final String policyName = basePolicyName + idSuffix;
	    final String marketName = baseMarketName + idSuffix;
	    final boolean newSalesMode = isNewSalesMode();

	    // ---------------------------------------------------------------------
	    // This implementation generates a one-sided tax only.
	    //
	    // Assumptions:
	    // - intensity values from trn_veh_info_8.5.csv are on a vehicle-distance
	    //   basis
	    // - if the user-specified MPGe target is intended to reflect passenger/
	    //   service performance, then load should be incorporated when converting
	    //   the target into a vehicle-basis intensity:
	    //
	    //       targetIntensityVehicleAdjusted =
	    //           (gjPerGal * load) / (kmPerMile * targetMPGe)
	    //
	    // - tax is applied only to technologies worse than the adjusted target:
	    //
	    //       taxGap = techIntensityVehicle - targetIntensityVehicleAdjusted
	    //
	    // - no additional division by load is applied after the comparison
	    //
	    // CSV part 1 row shape:
	    //   region,sector,subsector,tech,year,input-tax-name,current-coef
	    //
	    // CSV part 2 row shape:
	    //   region,policy-name,market-name,policy-type,year,constraint
	    //
	    // IMPORTANT:
	    // HEADER_TAX_1SIDE_PART1 and HEADER_TAX_1SIDE_PART2 must match
	    // these row widths.
	    // ---------------------------------------------------------------------

	    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
	    fileContent = getMetaDataContent(tree, marketName, policyName);

	    // ---------------------------------------------------------------------
	    // Part 1: technology tax rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART1).append(vars.getEol()).append(vars.getEol())
	        .append("region,sector,subsector,tech,year,input-tax-name,current-coef")
	        .append(vars.getEol());

	    // ---------------------------------------------------------------------
	    // Part 2: policy rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART2).append(vars.getEol()).append(vars.getEol())
	        .append("region,policy-name,market-name,policy-type,year,constraint")
	        .append(vars.getEol());

	    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
	    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

	    final double gjPerGal = 0.1203;
	    final double kmPerMile = 1.61;

	    int skippedInvalidRows = 0;
	    int writtenConstraintRows = 0;

	    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
	    Map<String, Integer> taxedTechsPerYear = new LinkedHashMap<>();

	    System.out.println("======================================================");
	    System.out.println("Generating MPG Target Scenario Component (one-sided tax + load in target conversion)");
	    System.out.println("Policy name base: " + basePolicyName);
	    System.out.println("Market name base: " + baseMarketName);
	    System.out.println("Policy name final: " + policyName);
	    System.out.println("Market name final: " + marketName);
	    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
	    System.out.println("New sales mode: " + newSalesMode);
	    System.out.println("Intensity basis assumption: vehicle-distance based");
	    System.out.println("Target conversion adjustment: multiply gjPerGal by load before MPGe conversion");
	    System.out.println("======================================================");

	    for (String region : selectedRegions) {
	        final String subsector = comboBoxSubsector.getValue();
	        final String sector =
	            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
	                ? "trn_freight_road"
	                : "trn_pass_road_LDV_4W";

	        for (String tableRow : targetTableRows) {
	            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
	            if (split == null || split.length < 2) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String targetYearStr = split[0];
	            final Integer targetYearParsed = safeParseInt(targetYearStr);
	            final Double targetValueParsed = safeParseDouble(split[1]);

	            if (targetYearParsed == null || targetValueParsed == null) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final int targetYear = targetYearParsed;
	            final double targetMPGe = targetValueParsed;

	            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
	            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);

	            boolean taxActivationWritten = false;

	            System.out.println();
	            System.out.println("------------------------------------------------------");
	            System.out.println("Region: " + region);
	            System.out.println("Subsector: " + subsector);
	            System.out.println("Target year: " + targetYear);
	            System.out.println("Target MPGe: " + targetMPGe);
	            System.out.println("Tax policy: " + taxPolicyKey);
	            System.out.println("Tax market: " + taxMarketKey);
	            System.out.println("------------------------------------------------------");

	            for (Integer modelYear : vars.getAllowablePolicyYears()) {
	                final boolean applyThisTarget;
	                if (newSalesMode) {
	                    applyThisTarget = (modelYear == targetYear);
	                } else {
	                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
	                }

	                System.out.println(
	                    "Target-year applicability => " +
	                    "targetYear=" + targetYear +
	                    ", modelYear=" + modelYear +
	                    ", newSalesMode=" + newSalesMode +
	                    ", apply=" + applyThisTarget
	                );

	                if (!applyThisTarget) {
	                    continue;
	                }

	                final String modelYearStr = Integer.toString(modelYear);
	                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

	                for (String tech : techList) {
	                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
	                    final String loadStr = utils.getTrnVehInfo("load", region, sector, subsector, tech, modelYearStr);

	                    if (intensityStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing intensity metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
	                    final double techIntensityVehicle = (rawIntensityParsed != null) ? rawIntensityParsed : Double.NaN;

	                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid intensity:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", intensity=" + techIntensityVehicle);
	                        continue;
	                    }

	                    if (loadStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing load metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double loadParsed = safeParseDouble(loadStr);
	                    final double load = (loadParsed != null) ? loadParsed : Double.NaN;

	                    if (!Double.isFinite(load) || load <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid load:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", load=" + load);
	                        continue;
	                    }

	                    final double targetIntensityVehicleAdjusted =
	                        (gjPerGal * load) / (kmPerMile * targetMPGe);

	                    final double taxGap = techIntensityVehicle - targetIntensityVehicleAdjusted;

	                    final double techBackConvertedMPGeVehicle =
	                        gjPerGal / (kmPerMile * techIntensityVehicle);

	                    final double targetBackConvertedMPGeVehicleNoLoad =
	                        gjPerGal / (kmPerMile * (gjPerGal / (kmPerMile * targetMPGe)));

	                    final double targetBackConvertedMPGeVehicleAdjusted =
	                        gjPerGal / (kmPerMile * targetIntensityVehicleAdjusted);

	                    System.out.println(
	                        "Tech debug => " +
	                        "region=" + region +
	                        ", subsector=" + subsector +
	                        ", tech=" + tech +
	                        ", targetYear=" + targetYearStr +
	                        ", modelYear=" + modelYearStr +
	                        ", load=" + load +
	                        ", techIntensityVehicle=" + techIntensityVehicle +
	                        ", targetIntensityVehicleAdjusted=" + targetIntensityVehicleAdjusted +
	                        ", taxGap(tech-targetAdjusted)=" + taxGap +
	                        ", techBackConvertedMPGeVehicle=" + techBackConvertedMPGeVehicle +
	                        ", targetBackConvertedMPGeVehicleNoLoad=" + targetBackConvertedMPGeVehicleNoLoad +
	                        ", targetBackConvertedMPGeVehicleAdjusted=" + targetBackConvertedMPGeVehicleAdjusted
	                    );

	                    if (taxGap > 0.0) {
	                        contentP1.append(region).append(",")
	                            .append(sector).append(",")
	                            .append(subsector).append(",")
	                            .append(tech).append(",")
	                            .append(modelYearStr).append(",")
	                            .append(taxPolicyKey).append(",")
	                            .append(taxGap)
	                            .append(vars.getEol());

	                        writtenConstraintRows++;
	                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);
	                        taxedTechsPerYear.put(targetYearStr, taxedTechsPerYear.getOrDefault(targetYearStr, 0) + 1);

	                        if (!taxActivationWritten) {
	                            contentP2.append(region).append(",")
	                                .append(taxPolicyKey).append(",")
	                                .append(taxMarketKey).append(",")
	                                .append("tax").append(",")
	                                .append(targetYearStr).append(",")
	                                .append("1")
	                                .append(vars.getEol());

	                            taxActivationWritten = true;
	                        }
	                    } else {
	                        System.out.println("No tax row written; tech is at or better than adjusted target:"
	                            + " tech=" + tech
	                            + ", modelYear=" + modelYearStr);
	                    }
	                }
	            }
	        }
	    }

	    if (writtenConstraintRows == 0) {
	        utils.warningMessage(
	            "No valid one-sided tax rows were generated.\n" +
	            "Please verify target values and transport intensity/load metadata."
	        );
	        return;
	    }

	    if (skippedInvalidRows > 0) {
	        System.out.println("Skipped invalid one-sided tax rows: " + skippedInvalidRows);
	    }

	    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

	    System.out.println("======================================================");
	    System.out.println("Finished generating MPG Target Scenario Component (one-sided tax + load in target conversion)");
	    System.out.println("Written constraint rows: " + writtenConstraintRows);
	    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

	    System.out.println("--- Rows per target year ---");
	    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Rows per model year ---");
	    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
	        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Taxed technologies per target year ---");
	    for (Map.Entry<String, Integer> e : taxedTechsPerYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", taxedTechCount=" + e.getValue());
	    }

	    System.out.println("======================================================");
	}	

	private void saveScenarioComponentAlt5(TreeView<String> tree) {
	    if (!qaInputs()) {
	        return;
	    }

	    final String idSuffix = checkBoxUseUniqueNames.isSelected()
	        ? utils.getUniqueString()
	        : "";

	    final String basePolicyName = textFieldPolicyName.getText();
	    final String baseMarketName = textFieldMarketName.getText();

	    final String policyName = basePolicyName + idSuffix;
	    final String marketName = baseMarketName + idSuffix;
	    final boolean newSalesMode = isNewSalesMode();

	    // ---------------------------------------------------------------------
	    // This implementation generates a one-sided tax only.
	    //
	    // Assumptions:
	    // - intensity values from trn_veh_info_8.5.csv are on a vehicle-distance
	    //   basis
	    // - the target MPGe is converted directly to a vehicle-basis intensity:
	    //
	    //       targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe)
	    //
	    // - tax is applied only to technologies worse than the target:
	    //
	    //       rawGap = techIntensityVehicle - targetIntensityVehicle
	    //
	    // - the written tax coefficient is scaled by a tunable multiplier:
	    //
	    //       taxCoefficient = TAX_SCALAR * rawGap
	    //
	    // CSV part 1 row shape:
	    //   region,sector,subsector,tech,year,input-tax-name,current-coef
	    //
	    // CSV part 2 row shape:
	    //   region,policy-name,market-name,policy-type,year,constraint
	    //
	    // IMPORTANT:
	    // HEADER_TAX_1SIDE_PART1 and HEADER_TAX_1SIDE_PART2 must match
	    // these row widths.
	    // ---------------------------------------------------------------------

	    final double TAX_SCALAR = 0.10;

	    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
	    fileContent = getMetaDataContent(tree, marketName, policyName);

	    // ---------------------------------------------------------------------
	    // Part 1: technology tax rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART1).append(vars.getEol()).append(vars.getEol())
	        .append("region,sector,subsector,tech,year,input-tax-name,current-coef")
	        .append(vars.getEol());

	    // ---------------------------------------------------------------------
	    // Part 2: policy rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART2).append(vars.getEol()).append(vars.getEol())
	        .append("region,policy-name,market-name,policy-type,year,constraint")
	        .append(vars.getEol());

	    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
	    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

	    final double gjPerGal = 0.1203;
	    final double kmPerMile = 1.61;

	    int skippedInvalidRows = 0;
	    int writtenConstraintRows = 0;

	    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
	    Map<String, Integer> taxedTechsPerYear = new LinkedHashMap<>();

	    System.out.println("======================================================");
	    System.out.println("Generating MPG Target Scenario Component (one-sided tax + scalar)");
	    System.out.println("Policy name base: " + basePolicyName);
	    System.out.println("Market name base: " + baseMarketName);
	    System.out.println("Policy name final: " + policyName);
	    System.out.println("Market name final: " + marketName);
	    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
	    System.out.println("New sales mode: " + newSalesMode);
	    System.out.println("Intensity basis assumption: vehicle-distance based");
	    System.out.println("Tax coefficient scaling: TAX_SCALAR = " + TAX_SCALAR);
	    System.out.println("======================================================");

	    for (String region : selectedRegions) {
	        final String subsector = comboBoxSubsector.getValue();
	        final String sector =
	            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
	                ? "trn_freight_road"
	                : "trn_pass_road_LDV_4W";

	        for (String tableRow : targetTableRows) {
	            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
	            if (split == null || split.length < 2) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String targetYearStr = split[0];
	            final Integer targetYearParsed = safeParseInt(targetYearStr);
	            final Double targetValueParsed = safeParseDouble(split[1]);

	            if (targetYearParsed == null || targetValueParsed == null) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final int targetYear = targetYearParsed;
	            final double targetMPGe = targetValueParsed;

	            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
	            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);

	            boolean taxActivationWritten = false;

	            final double targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe);

	            System.out.println();
	            System.out.println("------------------------------------------------------");
	            System.out.println("Region: " + region);
	            System.out.println("Subsector: " + subsector);
	            System.out.println("Target year: " + targetYear);
	            System.out.println("Target MPGe: " + targetMPGe);
	            System.out.println("Tax policy: " + taxPolicyKey);
	            System.out.println("Tax market: " + taxMarketKey);
	            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
	            System.out.println("------------------------------------------------------");

	            for (Integer modelYear : vars.getAllowablePolicyYears()) {
	                final boolean applyThisTarget;
	                if (newSalesMode) {
	                    applyThisTarget = (modelYear == targetYear);
	                } else {
	                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
	                }

	                System.out.println(
	                    "Target-year applicability => " +
	                    "targetYear=" + targetYear +
	                    ", modelYear=" + modelYear +
	                    ", newSalesMode=" + newSalesMode +
	                    ", apply=" + applyThisTarget
	                );

	                if (!applyThisTarget) {
	                    continue;
	                }

	                final String modelYearStr = Integer.toString(modelYear);
	                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

	                for (String tech : techList) {
	                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);

	                    if (intensityStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing intensity metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
	                    final double techIntensityVehicle = (rawIntensityParsed != null) ? rawIntensityParsed : Double.NaN;

	                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid intensity:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", intensity=" + techIntensityVehicle);
	                        continue;
	                    }

	                    final double rawGap = techIntensityVehicle - targetIntensityVehicle;
	                    final double taxCoefficient = TAX_SCALAR * rawGap;

	                    final double techBackConvertedMPGeVehicle =
	                        gjPerGal / (kmPerMile * techIntensityVehicle);

	                    final double targetBackConvertedMPGeVehicle =
	                        gjPerGal / (kmPerMile * targetIntensityVehicle);

	                    System.out.println(
	                        "Tech debug => " +
	                        "region=" + region +
	                        ", subsector=" + subsector +
	                        ", tech=" + tech +
	                        ", targetYear=" + targetYearStr +
	                        ", modelYear=" + modelYearStr +
	                        ", techIntensityVehicle=" + techIntensityVehicle +
	                        ", targetIntensityVehicle=" + targetIntensityVehicle +
	                        ", rawGap(tech-target)=" + rawGap +
	                        ", taxCoefficient=" + taxCoefficient +
	                        ", techBackConvertedMPGeVehicle=" + techBackConvertedMPGeVehicle +
	                        ", targetBackConvertedMPGeVehicle=" + targetBackConvertedMPGeVehicle
	                    );

	                    if (rawGap > 0.0) {
	                        contentP1.append(region).append(",")
	                            .append(sector).append(",")
	                            .append(subsector).append(",")
	                            .append(tech).append(",")
	                            .append(modelYearStr).append(",")
	                            .append(taxPolicyKey).append(",")
	                            .append(taxCoefficient)
	                            .append(vars.getEol());

	                        writtenConstraintRows++;
	                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);
	                        taxedTechsPerYear.put(targetYearStr, taxedTechsPerYear.getOrDefault(targetYearStr, 0) + 1);

	                        if (!taxActivationWritten) {
	                            contentP2.append(region).append(",")
	                                .append(taxPolicyKey).append(",")
	                                .append(taxMarketKey).append(",")
	                                .append("tax").append(",")
	                                .append(targetYearStr).append(",")
	                                .append("1")
	                                .append(vars.getEol());

	                            taxActivationWritten = true;
	                        }
	                    } else {
	                        System.out.println("No tax row written; tech is at or better than target:"
	                            + " tech=" + tech
	                            + ", modelYear=" + modelYearStr);
	                    }
	                }
	            }
	        }
	    }

	    if (writtenConstraintRows == 0) {
	        utils.warningMessage(
	            "No valid one-sided tax rows were generated.\n" +
	            "Please verify target values and transport intensity metadata."
	        );
	        return;
	    }

	    if (skippedInvalidRows > 0) {
	        System.out.println("Skipped invalid one-sided tax rows: " + skippedInvalidRows);
	    }

	    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

	    System.out.println("======================================================");
	    System.out.println("Finished generating MPG Target Scenario Component (one-sided tax + scalar)");
	    System.out.println("Written constraint rows: " + writtenConstraintRows);
	    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

	    System.out.println("--- Rows per target year ---");
	    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Rows per model year ---");
	    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
	        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Taxed technologies per target year ---");
	    for (Map.Entry<String, Integer> e : taxedTechsPerYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", taxedTechCount=" + e.getValue());
	    }

	    System.out.println("======================================================");
	}

	private void saveScenarioComponentAlt6(TreeView<String> tree) {
	    if (!qaInputs()) {
	        return;
	    }

	    final String idSuffix = checkBoxUseUniqueNames.isSelected()
	        ? utils.getUniqueString()
	        : "";

	    final String basePolicyName = textFieldPolicyName.getText();
	    final String baseMarketName = textFieldMarketName.getText();

	    final String policyName = basePolicyName + idSuffix;
	    final String marketName = baseMarketName + idSuffix;
	    final boolean newSalesMode = isNewSalesMode();

	    // ---------------------------------------------------------------------
	    // This implementation generates a one-sided tax only.
	    //
	    // Assumptions:
	    // - intensity values from trn_veh_info_8.5.csv are on a vehicle-distance
	    //   basis
	    // - the target MPGe is converted directly to a vehicle-basis intensity:
	    //
	    //       targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe)
	    //
	    // - tax is applied only to technologies worse than the target:
	    //
	    //       rawGap = techIntensityVehicle - targetIntensityVehicle
	    //
	    // - the written tax coefficient is scaled by a tunable multiplier:
	    //
	    //       taxCoefficient = TAX_SCALAR * rawGap
	    //
	    // CSV part 1 row shape:
	    //   region,sector,subsector,tech,year,input-tax-name,current-coef
	    //
	    // CSV part 2 row shape:
	    //   region,policy-name,market-name,policy-type,year,constraint
	    //
	    // IMPORTANT:
	    // HEADER_TAX_1SIDE_PART1 and HEADER_TAX_1SIDE_PART2 must match
	    // these row widths.
	    // ---------------------------------------------------------------------

	    final double TAX_SCALAR = 1e-4;

	    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
	    fileContent = getMetaDataContent(tree, marketName, policyName);

	    // ---------------------------------------------------------------------
	    // Part 1: technology tax rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART1).append(vars.getEol()).append(vars.getEol())
	        .append("region,sector,subsector,tech,year,input-tax-name,current-coef")
	        .append(vars.getEol());

	    // ---------------------------------------------------------------------
	    // Part 2: policy rows
	    // ---------------------------------------------------------------------
	    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_TAX_1SIDE_PART2).append(vars.getEol()).append(vars.getEol())
	        .append("region,policy-name,market-name,policy-type,year,constraint")
	        .append(vars.getEol());

	    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
	    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

	    final double gjPerGal = 0.1203;
	    final double kmPerMile = 1.61;

	    int skippedInvalidRows = 0;
	    int writtenConstraintRows = 0;

	    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
	    Map<String, Integer> taxedTechsPerYear = new LinkedHashMap<>();

	    System.out.println("======================================================");
	    System.out.println("Generating MPG Target Scenario Component (one-sided tax + scalar)");
	    System.out.println("Policy name base: " + basePolicyName);
	    System.out.println("Market name base: " + baseMarketName);
	    System.out.println("Policy name final: " + policyName);
	    System.out.println("Market name final: " + marketName);
	    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
	    System.out.println("New sales mode: " + newSalesMode);
	    System.out.println("Intensity basis assumption: vehicle-distance based");
	    System.out.println("Tax coefficient scaling: TAX_SCALAR = " + TAX_SCALAR);
	    System.out.println("======================================================");

	    for (String region : selectedRegions) {
	        final String subsector = comboBoxSubsector.getValue();
	        final String sector =
	            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
	                ? "trn_freight_road"
	                : "trn_pass_road_LDV_4W";

	        for (String tableRow : targetTableRows) {
	            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
	            if (split == null || split.length < 2) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String targetYearStr = split[0];
	            final Integer targetYearParsed = safeParseInt(targetYearStr);
	            final Double targetValueParsed = safeParseDouble(split[1]);

	            if (targetYearParsed == null || targetValueParsed == null) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final int targetYear = targetYearParsed;
	            final double targetMPGe = targetValueParsed;

	            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String taxPolicyKey = getPolicyKeyForTargetYear(policyName + "_tax", targetYearStr, newSalesMode);
	            final String taxMarketKey = getMarketKeyForTargetYear(marketName + "_tax", taxPolicyKey, targetYearStr, newSalesMode);

	            boolean taxActivationWritten = false;

	            final double targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe);

	            System.out.println();
	            System.out.println("------------------------------------------------------");
	            System.out.println("Region: " + region);
	            System.out.println("Subsector: " + subsector);
	            System.out.println("Target year: " + targetYear);
	            System.out.println("Target MPGe: " + targetMPGe);
	            System.out.println("Tax policy: " + taxPolicyKey);
	            System.out.println("Tax market: " + taxMarketKey);
	            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
	            System.out.println("------------------------------------------------------");

	            for (Integer modelYear : vars.getAllowablePolicyYears()) {
	                final boolean applyThisTarget;
	                if (newSalesMode) {
	                    applyThisTarget = (modelYear == targetYear);
	                } else {
	                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
	                }

	                System.out.println(
	                    "Target-year applicability => " +
	                    "targetYear=" + targetYear +
	                    ", modelYear=" + modelYear +
	                    ", newSalesMode=" + newSalesMode +
	                    ", apply=" + applyThisTarget
	                );

	                if (!applyThisTarget) {
	                    continue;
	                }

	                final String modelYearStr = Integer.toString(modelYear);
	                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

	                for (String tech : techList) {
	                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);

	                    if (intensityStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing intensity metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double rawIntensityParsed = safeParseDouble(intensityStr);
	                    final double techIntensityVehicle = (rawIntensityParsed != null) ? rawIntensityParsed : Double.NaN;

	                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid intensity:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", intensity=" + techIntensityVehicle);
	                        continue;
	                    }

	                    final double rawGap = techIntensityVehicle - targetIntensityVehicle;
	                    final double taxCoefficient = TAX_SCALAR * rawGap;

	                    final double techBackConvertedMPGeVehicle =
	                        gjPerGal / (kmPerMile * techIntensityVehicle);

	                    final double targetBackConvertedMPGeVehicle =
	                        gjPerGal / (kmPerMile * targetIntensityVehicle);

	                    System.out.println(
	                        "Tech debug => " +
	                        "region=" + region +
	                        ", subsector=" + subsector +
	                        ", tech=" + tech +
	                        ", targetYear=" + targetYearStr +
	                        ", modelYear=" + modelYearStr +
	                        ", techIntensityVehicle=" + techIntensityVehicle +
	                        ", targetIntensityVehicle=" + targetIntensityVehicle +
	                        ", rawGap(tech-target)=" + rawGap +
	                        ", taxCoefficient=" + taxCoefficient +
	                        ", techBackConvertedMPGeVehicle=" + techBackConvertedMPGeVehicle +
	                        ", targetBackConvertedMPGeVehicle=" + targetBackConvertedMPGeVehicle
	                    );

	                    if (rawGap > 0.0) {
	                        contentP1.append(region).append(",")
	                            .append(sector).append(",")
	                            .append(subsector).append(",")
	                            .append(tech).append(",")
	                            .append(modelYearStr).append(",")
	                            .append(taxPolicyKey).append(",")
	                            .append(taxCoefficient)
	                            .append(vars.getEol());

	                        writtenConstraintRows++;
	                        rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                        rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);
	                        taxedTechsPerYear.put(targetYearStr, taxedTechsPerYear.getOrDefault(targetYearStr, 0) + 1);

	                        if (!taxActivationWritten) {
	                            contentP2.append(region).append(",")
	                                .append(taxPolicyKey).append(",")
	                                .append(taxMarketKey).append(",")
	                                .append("tax").append(",")
	                                .append(targetYearStr).append(",")
	                                .append("1")
	                                .append(vars.getEol());

	                            taxActivationWritten = true;
	                        }
	                    } else {
	                        System.out.println("No tax row written; tech is at or better than target:"
	                            + " tech=" + tech
	                            + ", modelYear=" + modelYearStr);
	                    }
	                }
	            }
	        }
	    }

	    if (writtenConstraintRows == 0) {
	        utils.warningMessage(
	            "No valid one-sided tax rows were generated.\n" +
	            "Please verify target values and transport intensity metadata."
	        );
	        return;
	    }

	    if (skippedInvalidRows > 0) {
	        System.out.println("Skipped invalid one-sided tax rows: " + skippedInvalidRows);
	    }

	    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

	    System.out.println("======================================================");
	    System.out.println("Finished generating MPG Target Scenario Component (one-sided tax + scalar)");
	    System.out.println("Written constraint rows: " + writtenConstraintRows);
	    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

	    System.out.println("--- Rows per target year ---");
	    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Rows per model year ---");
	    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
	        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Taxed technologies per target year ---");
	    for (Map.Entry<String, Integer> e : taxedTechsPerYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", taxedTechCount=" + e.getValue());
	    }

	    System.out.println("======================================================");
	}

private void saveScenarioComponentAlt7(TreeView<String> tree) {
    if (!qaInputs()) {
        return;
    }

    // ---------------------------------------------------------------------
    // Name setup
    // ---------------------------------------------------------------------
    final String idSuffix = checkBoxUseUniqueNames.isSelected()
        ? utils.getUniqueString()
        : "";

    final String basePolicyName = textFieldPolicyName.getText();
    final String baseMarketName = textFieldMarketName.getText();

    final String policyName = basePolicyName + idSuffix;
    final String marketName = baseMarketName + idSuffix;
    final boolean newSalesMode = isNewSalesMode();

    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
    fileContent = getMetaDataContent(tree, marketName, policyName);

    // ---------------------------------------------------------------------
    // Output tables
    //
    // RES-style implementation:
    // - minicam-energy-input current-coef
    // - res-secondary-output output-ratio
    // - policy-portfolio-standard type = RES
    // ---------------------------------------------------------------------
    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_PART1).append(vars.getEol()).append(vars.getEol())
        .append("region,sector,subsector,tech,year,input,current-coef,policy,output-ratio,pMultiplier,price-unit-conversion")
        .append(vars.getEol());

    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_PART2).append(vars.getEol()).append(vars.getEol())
        .append("region,policy,market,type,year,constrained")
        .append(vars.getEol());

    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

    // ---------------------------------------------------------------------
    // Physical conversion constants
    // ---------------------------------------------------------------------
    final double gjPerGal = 0.1203;   // GJ / gallon gasoline equivalent
    final double kmPerMile = 1.61;

    // ---------------------------------------------------------------------
    // Export scaling
    // ---------------------------------------------------------------------
    final double EXPORT_SCALE = 1.0;

    int skippedInvalidRows = 0;
    int writtenConstraintRows = 0;

    // ---------------------------------------------------------------------
    // Debug bookkeeping
    // ---------------------------------------------------------------------
    Map<String, Integer> rowsPerTechModelYear = new LinkedHashMap<>();
    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
    Map<String, Set<String>> policiesPerTechModelYear = new LinkedHashMap<>();

    System.out.println("======================================================");
    System.out.println("Generating MPG Target Scenario Component (revised RES)");
    System.out.println("Policy name base: " + basePolicyName);
    System.out.println("Market name base: " + baseMarketName);
    System.out.println("Policy name final: " + policyName);
    System.out.println("Market name final: " + marketName);
    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
    System.out.println("New sales mode: " + newSalesMode);
    System.out.println("Intensity basis assumption: vehicle-distance based");
    System.out.println("EXPORT_SCALE: " + EXPORT_SCALE);
    System.out.println("pMultiplier fixed at 1.0");
    System.out.println("priceUnitConversion fixed at 1.0");
    System.out.println("Policy market rule: marketKey = policyKey");
    System.out.println("======================================================");

    for (String region : selectedRegions) {
        final String subsector = comboBoxSubsector.getValue();
        final String sector =
            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
                ? "trn_freight_road"
                : "trn_pass_road_LDV_4W";

        for (String tableRow : targetTableRows) {
            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
            if (split == null || split.length < 2) {
                skippedInvalidRows++;
                continue;
            }

            final String targetYearStr = split[0];
            final Integer targetYearParsed = safeParseInt(targetYearStr);
            final Double targetValueParsed = safeParseDouble(split[1]);

            if (targetYearParsed == null || targetValueParsed == null) {
                skippedInvalidRows++;
                continue;
            }

            final int targetYear = targetYearParsed;
            final double targetMPGe = targetValueParsed;

            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
                skippedInvalidRows++;
                continue;
            }

            final String policyKey = getPolicyKeyForTargetYear(policyName, targetYearStr, newSalesMode);

            // -----------------------------------------------------------------
            // IMPORTANT:
            // Use the policy name itself as the market name for this RES test.
            // This is intended to avoid missing-market errors in the
            // policy-portfolio-standard.
            // -----------------------------------------------------------------
            final String marketKey = policyKey;

            boolean activationWritten = false;

            // -----------------------------------------------------------------
            // Convert MPGe target to vehicle-basis intensity:
            //
            // intensity = (GJ/gal) / (km/mile * miles/gal)
            // -----------------------------------------------------------------
            final double targetIntensityVehicle =
                gjPerGal / (kmPerMile * targetMPGe);

            final double exportedOutputRatio = targetIntensityVehicle * EXPORT_SCALE;
            final double pMultiplier = 1.0;
            final double priceUnitConversion = 1.0;

            System.out.println();
            System.out.println("------------------------------------------------------");
            System.out.println("Region: " + region);
            System.out.println("Subsector: " + subsector);
            System.out.println("Target year: " + targetYear);
            System.out.println("Target MPGe: " + targetMPGe);
            System.out.println("Policy key: " + policyKey);
            System.out.println("Market key: " + marketKey);
            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
            System.out.println("Exported output-ratio: " + exportedOutputRatio);
            System.out.println("------------------------------------------------------");

            for (Integer modelYear : vars.getAllowablePolicyYears()) {
                final boolean applyThisTarget;
                if (newSalesMode) {
                    applyThisTarget = (modelYear == targetYear);
                } else {
                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
                }

                System.out.println(
                    "Target-year applicability => " +
                    "targetYear=" + targetYear +
                    ", modelYear=" + modelYear +
                    ", newSalesMode=" + newSalesMode +
                    ", apply=" + applyThisTarget
                );

                if (!applyThisTarget) {
                    continue;
                }

                final String modelYearStr = Integer.toString(modelYear);
                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

                for (String tech : techList) {
                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);

                    if (intensityStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing intensity metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double intensityParsed = safeParseDouble(intensityStr);
                    final double techIntensityVehicle = (intensityParsed != null) ? intensityParsed : Double.NaN;

                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid intensity:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", intensity=" + techIntensityVehicle);
                        continue;
                    }

                    final double exportedCurrentCoef = techIntensityVehicle * EXPORT_SCALE;
                    final double intensityGap = exportedCurrentCoef - exportedOutputRatio;

                    final double techBackConvertedMPGe =
                        gjPerGal / (kmPerMile * techIntensityVehicle);

                    final double targetBackConvertedMPGe =
                        gjPerGal / (kmPerMile * targetIntensityVehicle);

                    final String rowKey = region + "|" + sector + "|" + subsector + "|" + tech + "|" + modelYearStr;
                    rowsPerTechModelYear.put(rowKey, rowsPerTechModelYear.getOrDefault(rowKey, 0) + 1);
                    policiesPerTechModelYear.computeIfAbsent(rowKey, k -> new LinkedHashSet<>()).add(policyKey);

                    if (rowsPerTechModelYear.get(rowKey) > 1) {
                        System.out.println("WARNING: Multiple policy rows written for same tech/modelYear:"
                            + " key=" + rowKey
                            + ", count=" + rowsPerTechModelYear.get(rowKey)
                            + ", policies=" + policiesPerTechModelYear.get(rowKey));
                    }

                    rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
                    rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

                    System.out.println(
                        "Tech debug => " +
                        "region=" + region +
                        ", subsector=" + subsector +
                        ", tech=" + tech +
                        ", targetYear=" + targetYearStr +
                        ", modelYear=" + modelYearStr +
                        ", rawIntensity(vehicle basis)=" + techIntensityVehicle +
                        ", exportedCurrentCoef=" + exportedCurrentCoef +
                        ", exportedOutputRatio=" + exportedOutputRatio +
                        ", pMultiplier=" + pMultiplier +
                        ", priceUnitConversion=" + priceUnitConversion +
                        ", intensityGap(current-output)=" + intensityGap +
                        ", techBackConvertedMPGe=" + techBackConvertedMPGe +
                        ", targetBackConvertedMPGe=" + targetBackConvertedMPGe
                    );

                    // ---------------------------------------------------------
                    // Write technology-level RES row
                    // ---------------------------------------------------------
                    contentP1.append(region).append(",")
                        .append(sector).append(",")
                        .append(subsector).append(",")
                        .append(tech).append(",")
                        .append(modelYearStr).append(",")
                        .append(policyKey).append(",")
                        .append(exportedCurrentCoef).append(",")
                        .append(policyKey).append(",")
                        .append(exportedOutputRatio).append(",")
                        .append(pMultiplier).append(",")
                        .append(priceUnitConversion)
                        .append(vars.getEol());

                    writtenConstraintRows++;

                    // ---------------------------------------------------------
                    // Write one policy activation row after the first valid
                    // technology row for this target year
                    // ---------------------------------------------------------
                    if (!activationWritten) {
                        contentP2.append(region).append(",")
                            .append(policyKey).append(",")
                            .append(marketKey).append(",")
                            .append("RES").append(",")
                            .append(targetYearStr).append(",")
                            .append("1")
                            .append(vars.getEol());
                        activationWritten = true;
                    }
                }
            }
        }
    }

    if (writtenConstraintRows == 0) {
        utils.warningMessage(
            "No valid CAFE RES rows were generated.\n" +
            "Please verify target table values and transport intensity metadata for selected years."
        );
        return;
    }

    if (skippedInvalidRows > 0) {
        System.out.println("Skipped invalid CAFE RES rows: " + skippedInvalidRows);
    }

    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

    // ---------------------------------------------------------------------
    // Debug summary
    // ---------------------------------------------------------------------
    System.out.println("======================================================");
    System.out.println("Finished generating MPG target scenario component (revised RES)");
    System.out.println("Written constraint rows: " + writtenConstraintRows);
    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

    System.out.println("--- Rows per target year ---");
    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("--- Rows per model year ---");
    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("--- Duplicate policy attachment summary ---");
    for (Map.Entry<String, Set<String>> e : policiesPerTechModelYear.entrySet()) {
        if (e.getValue().size() > 1) {
            System.out.println("WARNING: multiple policies attached to same tech/modelYear:"
                + " key=" + e.getKey()
                + ", policies=" + e.getValue());
        }
    }

    System.out.println("======================================================");
}

private void saveScenarioComponentAlt8(TreeView<String> tree) {
    if (!qaInputs()) {
        return;
    }

    final String idSuffix = checkBoxUseUniqueNames.isSelected()
        ? utils.getUniqueString()
        : "";

    final String basePolicyName = textFieldPolicyName.getText();
    final String baseMarketName = textFieldMarketName.getText();

    final String policyNameBase = basePolicyName + idSuffix;
    final String marketName = baseMarketName + idSuffix;
    final boolean newSalesMode = isNewSalesMode();

    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
    fileContent = getMetaDataContent(tree, marketName, policyNameBase);

    // ---------------------------------------------------------------------
    // IMPORTANT:
    // Part 1 uses minicam-energy-input/current-coef and res-secondary-output.
    // Part 2 should match the repo's PortfolioStdConstraint-style layout.
    // ---------------------------------------------------------------------
    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_PART1).append(vars.getEol()).append(vars.getEol())
        .append("region,sector,subsector,tech,year,input,current-coef,policy,output-ratio,pMultiplier,price-unit-conversion")
        .append(vars.getEol());

    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
        .append(VARIABLE_ID).append(vars.getEol())
        .append(HEADER_PART2).append(vars.getEol()).append(vars.getEol())
        .append("region,policy,market,type,year,constraint")
        .append(vars.getEol());

    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

    final double gjPerGal = 0.1203;
    final double kmPerMile = 1.61;
    final double EXPORT_SCALE = 1.0;

    int skippedInvalidRows = 0;
    int writtenTechRows = 0;
    int writtenPolicyRows = 0;

    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();

    System.out.println("======================================================");
    System.out.println("Generating MPG Target Scenario Component (RES, conservative wiring)");
    System.out.println("Policy name base: " + basePolicyName);
    System.out.println("Policy name final base: " + policyNameBase);
    System.out.println("Market name final: " + marketName);
    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
    System.out.println("New sales mode: " + newSalesMode);
    System.out.println("EXPORT_SCALE: " + EXPORT_SCALE);
    System.out.println("======================================================");

    for (String region : selectedRegions) {
        final String subsector = comboBoxSubsector.getValue();
        final String sector =
            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
                ? "trn_freight_road"
                : "trn_pass_road_LDV_4W";

        for (String tableRow : targetTableRows) {
            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
            if (split == null || split.length < 2) {
                skippedInvalidRows++;
                continue;
            }

            final String targetYearStr = split[0];
            final Integer targetYearParsed = safeParseInt(targetYearStr);
            final Double targetValueParsed = safeParseDouble(split[1]);

            if (targetYearParsed == null || targetValueParsed == null) {
                skippedInvalidRows++;
                continue;
            }

            final int targetYear = targetYearParsed;
            final double targetMPGe = targetValueParsed;

            if (targetMPGe <= 0.0 || !Double.isFinite(targetMPGe)) {
                skippedInvalidRows++;
                continue;
            }

            // Keep policy names year-specific, but keep market name stable.
            final String policyKey = getPolicyKeyForTargetYear(policyNameBase, targetYearStr, newSalesMode);
            final String marketKey = marketName;

            boolean activationWritten = false;

            final double targetIntensityVehicle = gjPerGal / (kmPerMile * targetMPGe);
            final double exportedOutputRatio = targetIntensityVehicle * EXPORT_SCALE;
            final double pMultiplier = 1.0;
            final double priceUnitConversion = 1.0;

            System.out.println();
            System.out.println("------------------------------------------------------");
            System.out.println("Region: " + region);
            System.out.println("Subsector: " + subsector);
            System.out.println("Target year: " + targetYear);
            System.out.println("Target MPGe: " + targetMPGe);
            System.out.println("Policy key: " + policyKey);
            System.out.println("Market key: " + marketKey);
            System.out.println("Target intensity (vehicle basis): " + targetIntensityVehicle);
            System.out.println("------------------------------------------------------");

            for (Integer modelYear : vars.getAllowablePolicyYears()) {
                final boolean applyThisTarget;
                if (newSalesMode) {
                    applyThisTarget = (modelYear == targetYear);
                } else {
                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
                }

                if (!applyThisTarget) {
                    continue;
                }

                final String modelYearStr = Integer.toString(modelYear);
                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

                for (String tech : techList) {
                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);

                    if (intensityStr == null) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to missing intensity metadata:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr);
                        continue;
                    }

                    final Double intensityParsed = safeParseDouble(intensityStr);
                    final double techIntensityVehicle = (intensityParsed != null) ? intensityParsed : Double.NaN;

                    if (!Double.isFinite(techIntensityVehicle) || techIntensityVehicle <= 0.0) {
                        skippedInvalidRows++;
                        System.out.println("Skipping row due to invalid intensity:"
                            + " region=" + region
                            + ", sector=" + sector
                            + ", subsector=" + subsector
                            + ", tech=" + tech
                            + ", year=" + modelYearStr
                            + ", intensity=" + techIntensityVehicle);
                        continue;
                    }

                    final double exportedCurrentCoef = techIntensityVehicle * EXPORT_SCALE;
                    final double techBackConvertedMPGe = gjPerGal / (kmPerMile * techIntensityVehicle);

                    System.out.println(
                        "Tech debug => " +
                        "region=" + region +
                        ", subsector=" + subsector +
                        ", tech=" + tech +
                        ", targetYear=" + targetYearStr +
                        ", modelYear=" + modelYearStr +
                        ", techIntensityVehicle=" + techIntensityVehicle +
                        ", targetIntensityVehicle=" + targetIntensityVehicle +
                        ", exportedCurrentCoef=" + exportedCurrentCoef +
                        ", exportedOutputRatio=" + exportedOutputRatio +
                        ", techBackConvertedMPGe=" + techBackConvertedMPGe +
                        ", targetBackConvertedMPGe=" + targetMPGe
                    );

                    contentP1.append(region).append(",")
                        .append(sector).append(",")
                        .append(subsector).append(",")
                        .append(tech).append(",")
                        .append(modelYearStr).append(",")
                        .append(policyKey).append(",")
                        .append(exportedCurrentCoef).append(",")
                        .append(policyKey).append(",")
                        .append(exportedOutputRatio).append(",")
                        .append(pMultiplier).append(",")
                        .append(priceUnitConversion)
                        .append(vars.getEol());

                    writtenTechRows++;
                    rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
                    rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

                    if (!activationWritten) {
                        contentP2.append(region).append(",")
                            .append(policyKey).append(",")
                            .append(marketKey).append(",")
                            .append("RES").append(",")
                            .append(targetYearStr).append(",")
                            .append("1")
                            .append(vars.getEol());
                        activationWritten = true;
                        writtenPolicyRows++;
                    }
                }
            }
        }
    }

    if (writtenTechRows == 0) {
        utils.warningMessage(
            "No valid CAFE RES rows were generated.\n" +
            "Please verify target table values and transport intensity metadata."
        );
        return;
    }

    if (skippedInvalidRows > 0) {
        System.out.println("Skipped invalid CAFE RES rows: " + skippedInvalidRows);
    }

    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

    System.out.println("======================================================");
    System.out.println("Finished generating MPG target scenario component (RES, conservative wiring)");
    System.out.println("Written tech rows: " + writtenTechRows);
    System.out.println("Written policy rows: " + writtenPolicyRows);
    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

    System.out.println("--- Rows per target year ---");
    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("--- Rows per model year ---");
    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
    }

    System.out.println("======================================================");
}
	private void saveScenarioComponent(TreeView<String> tree) {
	    if (!qaInputs()) {
	        return;
	    }

	    // ---------------------------------------------------------------------
	    // Name setup
	    // ---------------------------------------------------------------------
	    final String idSuffix = checkBoxUseUniqueNames.isSelected()
	        ? utils.getUniqueString()
	        : "";

	    final String basePolicyName = textFieldPolicyName.getText();
	    final String baseMarketName = textFieldMarketName.getText();

	    final String policyName = basePolicyName + idSuffix;
	    final String marketName = baseMarketName + idSuffix;
	    final boolean newSalesMode = isNewSalesMode();

	    filenameSuggestion = basePolicyName.replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
	    fileContent = getMetaDataContent(tree, marketName, policyName);

	    // ---------------------------------------------------------------------
	    // Output tables
	    // NOTE: using current-coef for minicam-energy-input.
	    // ---------------------------------------------------------------------
	    StringBuilder contentP1 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_PART1).append(vars.getEol()).append(vars.getEol())
	        .append("region,sector,subsector,tech,year,input,current-coef,policy,output-ratio,pMultiplier,price-unit-conversion")
	        .append(vars.getEol());

	    StringBuilder contentP2 = new StringBuilder(INPUT_TABLE).append(vars.getEol())
	        .append(VARIABLE_ID).append(vars.getEol())
	        .append(HEADER_PART2).append(vars.getEol()).append(vars.getEol())
	        .append("region,policy,market,type,year,constrained")
	        .append(vars.getEol());

	    final String[] selectedRegions = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
	    final ArrayList<String> targetTableRows = paneForComponentDetails.getDataYrValsArrayList();

	    // ---------------------------------------------------------------------
	    // Conversion constants
	    // ---------------------------------------------------------------------
	    final double gj_per_gal = 0.1203; // GJ / gallon gasoline equivalent
	    final double km_per_mile = 1.61;
	    final double km_per_bln_km = 1e9;
	    final double km_per_mln_km = 1e6;

	    int skippedInvalidRows = 0;
	    int writtenConstraintRows = 0;

	    // false => GCAM-USA 8.5+ pathway
	    boolean useMMBTUConversions = vars.getUseTrnMMBTUConversions();
	    useMMBTUConversions = false; // forced false for testing

	    // ---------------------------------------------------------------------
	    // Debug bookkeeping
	    // ---------------------------------------------------------------------
	    Map<String, Integer> rowsPerTechModelYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerTargetYear = new LinkedHashMap<>();
	    Map<String, Integer> rowsPerModelYear = new LinkedHashMap<>();
	    Map<String, Set<String>> policiesPerTechModelYear = new LinkedHashMap<>();

	    System.out.println("======================================================");
	    System.out.println("Generating MPG Target Scenario Component");
	    System.out.println("Policy name base: " + basePolicyName);
	    System.out.println("Market name base: " + baseMarketName);
	    System.out.println("Policy name final: " + policyName);
	    System.out.println("Market name final: " + marketName);
	    System.out.println("Use unique names: " + checkBoxUseUniqueNames.isSelected());
	    System.out.println("New sales mode: " + newSalesMode);
	    System.out.println("Using pre-8.5 MMBTU conversions: " + useMMBTUConversions);
	    System.out.println("======================================================");

	    for (String region : selectedRegions) {
	        final String subsector = comboBoxSubsector.getValue();
	        final String sector =
	            (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck"))
	                ? "trn_freight_road"
	                : "trn_pass_road_LDV_4W";

	        for (String tableRow : targetTableRows) {
	            String[] split = utils.splitString(tableRow.replaceAll(" ", "").trim(), ",");
	            if (split == null || split.length < 2) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String targetYearStr = split[0];
	            final Integer targetYearParsed = safeParseInt(targetYearStr);
	            final Double targetValueParsed = safeParseDouble(split[1]);

	            if (targetYearParsed == null || targetValueParsed == null) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final int targetYear = targetYearParsed;
	            final double targetMilesPerGal = targetValueParsed;

	            if (targetMilesPerGal <= 0.0 || !Double.isFinite(targetMilesPerGal)) {
	                skippedInvalidRows++;
	                continue;
	            }

	            final String policyKey = getPolicyKeyForTargetYear(policyName, targetYearStr, newSalesMode);
	            final String marketKey = getMarketKeyForTargetYear(marketName, policyKey, targetYearStr, newSalesMode);

	            boolean activationWritten = false;
	            boolean hasValidConstraintRow = false;

	            // -----------------------------------------------------------------
	            // Convert MPG target -> energy intensity target.
	            //
	            // Forward conversion:
	            //   targetIntensity = (1 / MPG) / km_per_mile * gj_per_gal * distanceScale
	            //
	            // For 8.5+, we assume comparison on a common vehicle-distance basis.
	            // -----------------------------------------------------------------
	            final double targetIntensityGJPerBillionVkt =
	                (1.0 / targetMilesPerGal) / km_per_mile * gj_per_gal * km_per_bln_km / 1000.0;

	            System.out.println();
	            System.out.println("------------------------------------------------------");
	            System.out.println("Region: " + region);
	            System.out.println("Subsector: " + subsector);
	            System.out.println("Target year: " + targetYear);
	            System.out.println("Target MPG/MPGe: " + targetMilesPerGal);
	            System.out.println("Policy key: " + policyKey);
	            System.out.println("Market key: " + marketKey);
	            System.out.println("Target intensity (GJ/billion-vkt, exported basis): " + targetIntensityGJPerBillionVkt);
	            System.out.println("------------------------------------------------------");

	            for (Integer modelYear : vars.getAllowablePolicyYears()) {

	                // -------------------------------------------------------------
	                // IMPORTANT:
	                // In sales mode, targets should generally apply only to the
	                // corresponding model year, not be propagated forward.
	                //
	                // If you later decide to allow forward propagation, make that
	                // explicit and add separate logging.
	                // -------------------------------------------------------------
	                final boolean applyThisTarget;
	                if (newSalesMode) {
	                    applyThisTarget = (modelYear == targetYear);
	                } else {
	                    applyThisTarget = shouldApplyTargetToModelYear(false, modelYear, targetYear);
	                }

	                System.out.println(
	                    "Target-year applicability => " +
	                    "targetYear=" + targetYear +
	                    ", modelYear=" + modelYear +
	                    ", newSalesMode=" + newSalesMode +
	                    ", apply=" + applyThisTarget
	                );

	                if (!applyThisTarget) {
	                    continue;
	                }

	                final String modelYearStr = Integer.toString(modelYear);
	                final ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();

	                for (int techIndex = 0; techIndex < techList.size(); techIndex++) {
	                    final String tech = techList.get(techIndex);

	                    final String loadStr = utils.getTrnVehInfo("load", region, sector, subsector, tech, modelYearStr);
	                    final String intensityStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);

	                    if (loadStr == null || intensityStr == null) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to missing load/intensity metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr);
	                        continue;
	                    }

	                    final Double loadParsed = safeParseDouble(loadStr);
	                    final Double rawIntensityParsed = safeParseDouble(intensityStr);

	                    final double load = (loadParsed != null) ? loadParsed : 0.0;
	                    final double rawTechIntensityGJPerMillionVkt = (rawIntensityParsed != null) ? rawIntensityParsed : 0.0;

	                    if (!Double.isFinite(load) || load <= 0.0 ||
	                        !Double.isFinite(rawTechIntensityGJPerMillionVkt) || rawTechIntensityGJPerMillionVkt <= 0.0) {
	                        skippedInvalidRows++;
	                        System.out.println("Skipping row due to invalid parsed metadata:"
	                            + " region=" + region
	                            + ", sector=" + sector
	                            + ", subsector=" + subsector
	                            + ", tech=" + tech
	                            + ", year=" + modelYearStr
	                            + ", load=" + load
	                            + ", rawIntensity=" + rawTechIntensityGJPerMillionVkt);
	                        continue;
	                    }

	                    double exportedCurrentCoef;
	                    double exportedOutputRatio;
	                    double pMultiplier;
	                    double priceUnitConversion = 1.0;

	                    if (useMMBTUConversions) {
	                        // -----------------------------------------------------
	                        // Pre-8.5 legacy branch
	                        // -----------------------------------------------------
	                        final double targetIntensityLegacy =
	                            (1.0 / targetMilesPerGal) / km_per_mile * gj_per_gal * km_per_mln_km / priceUnitConversion;

	                        exportedCurrentCoef = rawTechIntensityGJPerMillionVkt;
	                        exportedOutputRatio = targetIntensityLegacy;
	                        pMultiplier = 1e9 * priceUnitConversion;
	                    } else {
	                        // -----------------------------------------------------
	                        // GCAM-USA 8.5+ branch
	                        //
	                        // raw intensity from metadata is assumed to be:
	                        //   GJ / million-vkt
	                        //
	                        // export current-coef on a comparable basis.
	                        // -----------------------------------------------------
	                        exportedCurrentCoef = rawTechIntensityGJPerMillionVkt * 1000.0;
	                        exportedOutputRatio = targetIntensityGJPerBillionVkt;
	                        pMultiplier = 1.0;
	                    }

	                    // ---------------------------------------------------------
	                    // Back-conversion diagnostics
	                    // ---------------------------------------------------------
	                    final double intensityGap = exportedCurrentCoef - exportedOutputRatio;

	                    final double techBackConvertedMPG =
	                        (exportedCurrentCoef > 0.0)
	                            ? (gj_per_gal * km_per_bln_km / 1000.0) / (exportedCurrentCoef * km_per_mile)
	                            : Double.NaN;

	                    final double targetBackConvertedMPG =
	                        (exportedOutputRatio > 0.0)
	                            ? (gj_per_gal * km_per_bln_km / 1000.0) / (exportedOutputRatio * km_per_mile)
	                            : Double.NaN;

	                    // ---------------------------------------------------------
	                    // Duplicate detection:
	                    // key by region / sector / subsector / tech / modelYear
	                    // ---------------------------------------------------------
	                    final String rowKey = region + "|" + sector + "|" + subsector + "|" + tech + "|" + modelYearStr;
	                    rowsPerTechModelYear.put(rowKey, rowsPerTechModelYear.getOrDefault(rowKey, 0) + 1);

	                    policiesPerTechModelYear.computeIfAbsent(rowKey, k -> new LinkedHashSet<>()).add(policyKey);

	                    if (rowsPerTechModelYear.get(rowKey) > 1) {
	                        System.out.println("WARNING: Multiple policy rows written for same tech/modelYear:"
	                            + " key=" + rowKey
	                            + ", count=" + rowsPerTechModelYear.get(rowKey)
	                            + ", policies=" + policiesPerTechModelYear.get(rowKey));
	                    }

	                    rowsPerTargetYear.put(targetYearStr, rowsPerTargetYear.getOrDefault(targetYearStr, 0) + 1);
	                    rowsPerModelYear.put(modelYearStr, rowsPerModelYear.getOrDefault(modelYearStr, 0) + 1);

	                    System.out.println(
	                        "Tech debug => " +
	                        "region=" + region +
	                        ", subsector=" + subsector +
	                        ", tech=" + tech +
	                        ", targetYear=" + targetYearStr +
	                        ", modelYear=" + modelYearStr +
	                        ", inputMPG=" + targetMilesPerGal +
	                        ", load=" + load +
	                        ", rawIntensity(GJ/million-vkt)=" + rawTechIntensityGJPerMillionVkt +
	                        ", currentCoef(exported)=" + exportedCurrentCoef +
	                        ", outputRatio(target exported)=" + exportedOutputRatio +
	                        ", pMultiplier=" + pMultiplier +
	                        ", priceUnitConversion=" + priceUnitConversion +
	                        ", intensityGap(current-output)=" + intensityGap +
	                        ", techBackConvertedMPG=" + techBackConvertedMPG +
	                        ", targetBackConvertedMPG=" + targetBackConvertedMPG
	                    );

	                    // ---------------------------------------------------------
	                    // Write technology-level row
	                    // ---------------------------------------------------------
	                    contentP1.append(region).append(",")
	                        .append(sector).append(",")
	                        .append(subsector).append(",")
	                        .append(tech).append(",")
	                        .append(modelYearStr).append(",")
	                        .append(policyKey).append(",")
	                        .append(exportedCurrentCoef).append(",")
	                        .append(policyKey).append(",")
	                        .append(exportedOutputRatio).append(",")
	                        .append(pMultiplier).append(",")
	                        .append(priceUnitConversion)
	                        .append(vars.getEol());

	                    hasValidConstraintRow = true;
	                    writtenConstraintRows++;

	                    // ---------------------------------------------------------
	                    // Write one policy activation row per target year
	                    // ---------------------------------------------------------
	                    if (!activationWritten && hasValidConstraintRow && techIndex == 0) {
	                        contentP2.append(region).append(",")
	                            .append(policyKey).append(",")
	                            .append(marketKey).append(",RES,")
	                            .append(targetYearStr).append(",1")
	                            .append(vars.getEol());
	                        activationWritten = true;
	                    }
	                }
	            }
	        }
	    }

	    if (writtenConstraintRows == 0) {
	        utils.warningMessage(
	            "No valid CAFE constraint rows were generated.\n" +
	            "Please verify target table values and transport metadata (load/intensity) for selected years."
	        );
	        return;
	    }

	    if (skippedInvalidRows > 0) {
	        System.out.println("Skipped invalid CAFE rows: " + skippedInvalidRows);
	    }

	    fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();

	    // ---------------------------------------------------------------------
	    // Debug summary
	    // ---------------------------------------------------------------------
	    System.out.println("======================================================");
	    System.out.println("Finished generating MPG target scenario component");
	    System.out.println("Written constraint rows: " + writtenConstraintRows);
	    System.out.println("Skipped invalid rows: " + skippedInvalidRows);

	    System.out.println("--- Rows per target year ---");
	    for (Map.Entry<String, Integer> e : rowsPerTargetYear.entrySet()) {
	        System.out.println("targetYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Rows per model year ---");
	    for (Map.Entry<String, Integer> e : rowsPerModelYear.entrySet()) {
	        System.out.println("modelYear=" + e.getKey() + ", rows=" + e.getValue());
	    }

	    System.out.println("--- Duplicate policy attachment summary ---");
	    for (Map.Entry<String, Set<String>> e : policiesPerTechModelYear.entrySet()) {
	        if (e.getValue().size() > 1) {
	            System.out.println("WARNING: multiple policies attached to same tech/modelYear:"
	                + " key=" + e.getKey()
	                + ", policies=" + e.getValue());
	        }
	    }

	    System.out.println("======================================================");
	}

	
    /**
     * Saves the scenario component for the specified tree of regions.
     * Performs QA checks, generates unique IDs, and builds file content for export.
     *
     * @param tree The TreeView of regions
     */
    private void saveScenarioComponentSaved(TreeView<String> tree) {
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
            .append("region,sector,subsector,tech,year,input,adjusted-coef,policy,output-ratio,pMultiplier,price-unit-conversion").append(vars.getEol());

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

        boolean useMMBTUConversions = vars.getUseTrnMMBTUConversions();  
        useMMBTUConversions = false; //for testing
        
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
                        String coefStr = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, modelYearStr);
                        
                        if (loadStr!=null&&coefStr!=null) {

                        Double loadParsed = safeParseDouble(loadStr);
                        double load = (loadParsed != null) ? loadParsed : 0.0;

                        // Intensity-based CAFE formulation: use UCD transport intensity as the exported coefficient value.

                        Double coefParsed = safeParseDouble(coefStr);// value is GJ/million-vkt                        
                        
                        double coef = (coefParsed != null) ? coefParsed : 0.5;

                        if (!Double.isFinite(load) || load <= 0.0 || !Double.isFinite(coef) || coef <= 0.0) {
                            skippedInvalidRows++;
                            continue;
                        }

                        double outputRatio;
                        double pMultiplier;
                        double priceUnitConversion = 1.0;
                        

                        double target=0.0;
                        
                        if (useMMBTUConversions) {
                        	 // prior to GCAM 8.5
                        	target = 1/targetMilesPerGal/km_per_mile*gj_per_gal*km_per_mln_km/priceUnitConversion; // Convert target from MPG to GJ/vkt, then to price units
    					    outputRatio=target;
    					    pMultiplier=1e9*priceUnitConversion; 
                        } else {
                            // GCAM 8.5+
                        	target = 1/targetMilesPerGal/km_per_mile*gj_per_gal*km_per_bln_km/1000.0; // Convert target from MPG to GJ/bln-vkt
                        	coef*=1000.0; //converts from GJ/million-vkt to GJ/billion-vkt
                            outputRatio = target;
                            pMultiplier = 1.0;
                        }
                        String conversions = (float)outputRatio+","+(float)pMultiplier;
                        
//                        String conversions = utils.getSubsectorConversions(region, sector, subsector, modelYear.intValue());
                        
////					    outputRatio=(float)(1.0/target/1.61*131.76/1e6);
////					    pMultiplier=((float)(load*1e9)); 
//                        
//                        if (!Double.isFinite(outputRatio) || outputRatio <= 0.0 || !Double.isFinite(pMultiplier) || pMultiplier <= 0.0) {
//                            skippedInvalidRows++;
//                            continue;
//                        }
                        
                        contentP1.append(region).append(",").append(sector).append(",").append(subsector).append(",")
                            .append(tech).append(",").append(modelYearStr).append(",").append(io).append(",")
                            .append(coef).append(",").append(io).append(",").append(conversions).append(",").append(priceUnitConversion).append(vars.getEol());
                        
//					    content_p1+=region+","+sector+","+subsector+","+tech+","+yr+","+io+","+coef+","+io+","+outputratio+","+pMultiplier+ vars.getEol();
//					    if (t==0) content_p2+=region+","+io+","+iom+",RES,"+yr+",1"+ vars.getEol();
                        
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
