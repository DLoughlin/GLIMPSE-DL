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
 * TabCafeStd is a JavaFX tab for creating and editing CAFE (Corporate Average
 * Fuel Economy) standard policies within the GLIMPSE Scenario Builder. This
 * class provides a user interface for specifying policy parameters, selecting
 * regions, technologies, and units, and managing policy data tables. It
 * supports both manual and automatic naming of policies and markets, and
 * handles the export of scenario component data in a format compatible with
 * GLIMPSE.
 *
 * <b>Main Features:</b>
 * <ul>
 * <li>UI controls for subsector, technology, units, and modification type
 * selection</li>
 * <li>Automatic and manual naming of policy and market</li>
 * <li>Region selection via a tree view</li>
 * <li>Data table for year-value pairs and policy details</li>
 * <li>Validation (QA) of user input and table data</li>
 * <li>Export of scenario component metadata and policy tables</li>
 * <li>Support for loading and saving policy configurations</li>
 * </ul>
 *
 * <b>Thread Safety:</b> This class is not thread-safe. All UI updates must be
 * performed on the JavaFX Application Thread.
 *
 * <b>Usage:</b> Instantiate this class as a tab in the scenario builder UI. The
 * user interacts with the controls to define a CAFE standard policy, and can
 * save or load configurations as needed.
 *
 * <b>Key Methods:</b>
 * <ul>
 * <li>{@link #setupUIComponents()} - Initializes UI components and layout</li>
 * <li>{@link #setupEventHandlers()} - Sets up event handlers for user
 * interaction</li>
 * <li>{@link #setPolicyAndMarketNames()} - Automatically generates policy and
 * market names</li>
 * <li>{@link #saveScenarioComponent()} - Saves the scenario component and
 * exports data</li>
 * <li>{@link #getMetaDataContent(TreeView, String, String)} - Generates
 * metadata for export</li>
 * <li>{@link #loadContent(ArrayList)} - Loads configuration from file</li>
 * <li>{@link #qaInputs()} - Validates user input and table data</li>
 * </ul>
 *
 * <b>Class Structure:</b>
 * <ul>
 * <li>UI component setup and layout methods</li>
 * <li>Event handler setup for user interaction</li>
 * <li>Methods for auto/manual naming, validation, and export</li>
 * <li>Helper methods for data processing and QA</li>
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
	private static final String SELECT_ONE_OR_MORE = "Select One or More";
	private static final String[] APPLICATION_MODE_OPTIONS = { MODE_FLEET_AVERAGE, MODE_NEW_SALES };
	private static final String[] SUBSECTOR_OPTIONS = { "Car", "Large Car and Truck", "Light Truck",
			"Medium Truck", "Heavy Truck" };
	private static final String[] TECH_OPTIONS = { "BEV", "FCEV", "Hybrid Liquids", "Liquids", "NG" };
	private static final String[] UNITS_OPTIONS = { "MPG", "MJ/vkt" };
	private static final String[] MOD_TYPE_OPTIONS = { "Initial and Final", "Initial w/% Growth/yr",
			"Initial w/% Growth/pd", "Initial w/Delta/yr", "Initial w/Delta/pd" };
	// private static final String HEADER_PART1 = "GLIMPSEEffCreditTargets-Part1";
	// private static final String HEADER_PART2 = "GLIMPSEEffCreditTargets-Part2";
	private static final String HEADER_PART1 = "GLIMPSECAFECreditTargets-Part1";
	private static final String HEADER_PART2 = "GLIMPSECAFECreditTargets-Part2";

	private static final String SECONDARY_OUTPUT_PREFIX = "credit-";

	private static final String INPUT_TABLE = "INPUT_TABLE";
	private static final String VARIABLE_ID = "Variable ID";
	private static final String NO_MATCH = "No match";
	private static final String SELECT_ONE = "Select One";

	private static final String REG = "Reg";
	private static final String MARKET_SUFFIX = "_Mkt";
	private static final String METADATA_APPLICATION_MODE = "#Application mode: ";

	// === UI Components ===
	// private final GridPane gridPanePresetModification = new GridPane();
	// private final GridPane gridPaneLeft = new GridPane();
	private final Label labelComboBoxSubsector = utils.createLabel(LABEL_SUBSECTOR, LABEL_WIDTH);
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
	 * Constructs a new TabCafeStd instance and initializes the UI components for
	 * the CAFE Standard tab. Sets up event handlers and populates controls with
	 * available data.
	 *
	 * @param title  The title of the tab
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
	 * Sets up all UI components by delegating to column setup methods. Calls
	 * setupLeftColumn, setupCenterColumn, and setupRightColumn.
	 */
	public void setupUIComponents() {
		setupLeftColumn(); // Left column: labels and controls
		setupCenterColumn(); // Center column: details table
		setupRightColumn(); // Right column: region tree
	}

	/**
	 * Sets up the UI controls (combo boxes, check combo boxes, etc.). Populates
	 * combo boxes and sets initial selections. Delegates to setupLeftColumn,
	 * setupCenterColumn, setupRightColumn.
	 */
	private void setupUIControls() {
		resetComboBoxItems(comboBoxSubsector, java.util.Arrays.asList(SUBSECTOR_OPTIONS));
		setComboBoxPrompt(comboBoxSubsector, SELECT_ONE, false);
		resetCheckComboBoxItems(checkComboBoxTech, java.util.Arrays.asList(TECH_OPTIONS), true);
		configureCheckComboBoxSelectionTitle(checkComboBoxTech, SELECT_ONE_OR_MORE, "Selected");
		checkComboBoxTech.setDisable(true); // Disabled until subsector selected
		resetComboBoxItems(comboBoxWhichUnits, java.util.Arrays.asList(UNITS_OPTIONS));
		comboBoxWhichUnits.getSelectionModel().select("MPG");
		comboBoxWhichUnits.setDisable(true); // Disabled until tech selected
		resetComboBoxItems(comboBoxApplicationMode, java.util.Arrays.asList(APPLICATION_MODE_OPTIONS));
		comboBoxApplicationMode.getSelectionModel().select(MODE_FLEET_AVERAGE);
		setModificationTypeOptions(MOD_TYPE_OPTIONS);
	}

	/**
	 * Sets up the left column of the UI, adding labels and controls to the grid
	 * pane. Arranges labels and controls for specification, subsector,
	 * technologies, units, and other parameters.
	 */
	private void setupLeftColumn() {
		gridPaneLeft.add(utils.createLabel(LABEL_SPECIFICATION), 0, 0, 2, 1);
		// Add checkboxes to HBox for auto/unique naming
		hBoxAutoUnique.getChildren().clear();
		hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
		// Add labels and controls to grid
		gridPaneLeft.addColumn(0, labelComboBoxSubsector, labelCheckComboBoxTech, labelWhichUnits, labelApplicationMode,
				new Separator(), utils.createLabel(LABEL_NAMES), labelPolicyName, labelMarketName, new Label(),
				new Separator(), utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear, labelEndYear,
				labelInitialAmount, labelGrowth);
		gridPaneLeft.addColumn(1, comboBoxSubsector, checkComboBoxTech, comboBoxWhichUnits, comboBoxApplicationMode,
				new Separator(), hBoxAutoUnique, textFieldPolicyName, textFieldMarketName, new Label(), new Separator(),
				new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear, textFieldInitialAmount,
				textFieldGrowth);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
		gridPaneLeft.setVgap(3.);
		// gridPaneLeft.setStyle(styles.getStyle2());
		scrollPaneLeft.setContent(gridPaneLeft);
	}

	/**
	 * Sets preferred, min, and max widths for UI components. Applies sizing to
	 * combo boxes and text fields for consistent layout.
	 */
	private void setComponentWidths() {
		ComboBox<?>[] comboBoxes = { comboBoxSubsector, comboBoxWhichUnits, comboBoxApplicationMode,
				comboBoxModificationType };
		TextField[] textFields = { textFieldStartYear, textFieldEndYear, textFieldInitialAmount, textFieldGrowth,
				textFieldPeriodLength, textFieldPolicyName, textFieldMarketName };
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
	 * Sets up event handlers for UI components. Handles user interactions such as
	 * combo box changes and button clicks. Includes double-click toggling for
	 * technology selection and subsector-based enabling/disabling.
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
			if (!isSelectionMissing(comboBoxSubsector)) {
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
		// Keep policy keys target-year scoped so each target can activate
		// independently.
		return targetYear + "_" + basePolicyName;
	}

	private String getMarketKeyForTargetYear(String baseMarketName, String policyKey, String targetYear,
			boolean newSalesMode) {
		if (!newSalesMode) {
			// Fleet Average follows legacy activate-table market naming based on policy
			// key.
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
	 * Sets the policy and market names automatically based on selected subsector
	 * and regions. If auto-naming is enabled, updates the text fields accordingly.
	 * Uses region, sector, and technology selections to build unique names. Runs on
	 * the JavaFX Application Thread.
	 */
	protected void setPolicyAndMarketNames() {
		Platform.runLater(() -> {
			if (checkBoxUseAutoNames.isSelected()) {
				String policyType = "mpgTgt";

				String sector = "--";
				String state = "--";
				try {
					String s = comboBoxSubsector.getValue();
					if (s != null) {
						s = utils.capitalizeOnlyFirstLetterOfString(normalizeNamePart(s));
						sector = s;
					}
					String[] listOfSelectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
					if (listOfSelectedLeaves.length > 0) {
						listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
						String stateStr = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
						if (stateStr.length() < 9) {
							state = normalizeNamePart(stateStr);
						} else {
							state = REG;
						}
					}
					String name = normalizeNamePart(policyType + "_" + sector + "_" + state);
					textFieldMarketName.setText(name + MARKET_SUFFIX);
					textFieldPolicyName.setText(name);
				} catch (Exception e) {
					System.out.println("Cannot auto-name market. Continuing.");
				}
			}
		});
	}

	/**
	 * Runnable implementation. Triggers saving of the scenario component. Calls
	 * saveScenarioComponent() on the JavaFX Application Thread.
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

	private void saveScenarioComponent(TreeView<String> tree) {

		if (!qaInputs()) {
			Thread.currentThread().destroy();
			return;
		}

		// --- Names and filename ---
		String ID = resolveUniqueSuffix(this.textFieldMarketName.getText());
		String policy_name = this.textFieldPolicyName.getText() + ID;
		String market_name = this.textFieldMarketName.getText() + ID;
		filenameSuggestion = this.textFieldPolicyName.getText().replaceAll("/", "-").replaceAll(" ", "_") + ".csv";

		// secondary-output commodity name: "credit-" + base policy name (no unique ID
		// suffix so it is
		// consistent across all regions and model years within this component)
		String secondaryOutputName = SECONDARY_OUTPUT_PREFIX + this.textFieldPolicyName.getText();

		// --- Metadata header ---
		fileContent = this.getMetaDataContent(tree, market_name, policy_name);

		// --- Selected regions ---
		String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
		listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);

		// --- Table data (year, target efficiency as output/input) ---
		ArrayList<String> dataArrayList = this.paneForComponentDetails.getDataYrValsArrayList();

		// --- Application mode ---
		boolean newSalesMode = isNewSalesMode();

		// --- Subsector / sector mapping (transport-specific for now; extend later) ---
		String subsector = comboBoxSubsector.getValue();
		String sector;
		if (subsector.equals("Light Truck") || subsector.equals("Medium Truck") || subsector.equals("Heavy Truck")) {
			sector = "trn_freight_road";
		} else {
			sector = "trn_pass_road_LDV_4W";
		}

		// --- Parse table rows up front ---
		String[] year_list = new String[dataArrayList.size()];
		double[] valuef_list = new double[dataArrayList.size()];
		for (int i = 0; i < dataArrayList.size(); i++) {
			String str = dataArrayList.get(i).replaceAll(" ", "").trim();
			year_list[i] = utils.splitString(str, ",")[0];
			Double val = safeParseDouble(utils.splitString(str, ",")[1]);
			valuef_list[i] = (val != null) ? val : 0.0;
		}

		// convert MPG target values to GJ/million-vkt
		if ("MPG".equals(comboBoxWhichUnits.getValue())) {
			double gjPerGallon = 0.1203;
			for (int i = 0; i < valuef_list.length; i++) {
				//converted values on order of 1e3
				double targetGJPerMillionKm = (gjPerGallon / (1.61 * valuef_list[i])) * 1e6;
				valuef_list[i] = targetGJPerMillionKm;
			}
		}

		ObservableList<String> tech_list = checkComboBoxTech.getCheckModel().getCheckedItems();

		// --- Part1 header: policy-portfolio-standard activation table ---
		StringBuilder contentP1 = new StringBuilder();
		contentP1.append(INPUT_TABLE).append(vars.getEol());
		contentP1.append(VARIABLE_ID).append(vars.getEol());
		contentP1.append(HEADER_PART1).append(vars.getEol())
				.append(vars.getEol())
				.append("region,policy,market,type,isShare,constraint-year,constrained,minprice-year,min-price,maxprice-year,max-price,price-unit,output-unit")
				.append(vars.getEol());

		// --- Part2 header: secondary-output credit rows ---
		StringBuilder contentP2 = new StringBuilder();
		contentP2.append(INPUT_TABLE).append(vars.getEol());
		contentP2.append(VARIABLE_ID).append(vars.getEol());
		contentP2.append(HEADER_PART2).append(vars.getEol())
				.append(vars.getEol())
				.append("region,sector,subsector,tech,year,secondary-output,output-ratio,pMultiplier")
				.append(vars.getEol());

		// --- Loop: region -> target year -> tech -> model year ---
		for (String region : listOfSelectedLeaves) {

			for (int i = 0; i < year_list.length; i++) {
				String targetYearStr = year_list[i];
				double targetEff = valuef_list[i];
				Integer targetYear = safeParseInt(targetYearStr);
				if (targetYear == null)
					continue;

				// Create year-scoped policy and market keys so each target can activate independently
				String policyKey = getPolicyKeyForTargetYear(policy_name, targetYearStr, newSalesMode);
				String marketKey = getMarketKeyForTargetYear(market_name, policyKey, targetYearStr, newSalesMode);

				// Part1: Policy-portfolio-standard activation row (one per region + target year)
				// Specifies constraint type (RES for credit-generating), binding status, and price bounds
				contentP1.append(region).append(",").append(policyKey).append(",")
						.append(marketKey).append(",")
						.append("RES").append(",") // type = RES: credit-generating constraint
						.append("0").append(",") // isShareBased = 0: not a market-share constraint
						.append(targetYearStr).append(",") // constraint activates in target year
						.append("1").append(",") // constrained = 1: GCAM must satisfy constraint via credit system
						.append(targetYearStr).append(",") // min-price year
						.append("0").append(",") // min-price = 0: no penalty if target not met
						.append(targetYearStr).append(",") // max-price year
						.append("1000000").append(",") // max-price: very high upper bound on credit cost
						.append("1975$/EJ-credit").append(",") // price unit for reporting
						.append("EJ-credit") // output unit for credit constraint
						.append(vars.getEol());

				// Part2: one row per region + tech + model year covered by this target
				for (String tech : tech_list) {

					float pMultiplier = (float) 1.0;
					float techEff = 0.0f;
					float techLoad = 1.0f;
					// Fetch technology-specific efficiency and load factor
					String techEffS = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, targetYearStr,
							"GJ/million-veh-km");
					String techLoadS = utils.getTrnVehInfo("load", region, sector, subsector, tech, targetYearStr);

					if ((techEffS == null) || (techLoadS == null)) {
						break;
					}

					try {
						techEff = (float) Double.parseDouble(techEffS);  
						techLoad = (float) Double.parseDouble(techLoadS);

					} catch (Exception e) {
						System.out.println("Error parsing tech efficiency or load for " + region + ", " + sector + ", "
								+ subsector + ", " + tech + " in year " + targetYearStr);
						break;
					}
					
					// Calculate output_ratio: the efficiency gap (target minus tech baseline)
					float output_ratio = (float) (targetEff - techEff);
					System.out.println(region + "," + tech + "," + targetYearStr + "," + targetEff + "," + techEff + "," + output_ratio);

					// Convert coefficients according to the selected GCAM transport version
					if (vars.isGcamVersionPre8_5()) {
						output_ratio = (float) (output_ratio / 1e6);
						pMultiplier = 1e9f;
					} else {
						pMultiplier = 1.0f;
					}
					String formattedOutputRatio = formatDisplayValue(output_ratio);
					String formattedPMultiplier = formatDisplayValue(pMultiplier);

					List<Integer> allModelYears = vars.getAllowablePolicyYears();
					for (Integer modelYear : allModelYears) {
						if (!shouldApplyTargetToModelYear(newSalesMode, modelYear, targetYear)) {
							continue;
						}

						contentP2.append(region).append(",")
								.append(sector).append(",")
								.append(subsector).append(",")
								.append(tech).append(",")
								.append(modelYear).append(",")
								.append(policyKey).append(",")
								.append(formattedOutputRatio).append(",")
								.append(formattedPMultiplier)
								.append(vars.getEol());
					}
				}
			}
		}

		fileContent += contentP1.toString() + vars.getEol() + contentP2.toString();
		System.out.println("Done");
	}

	private void saveScenarioComponentAlt1(TreeView<String> tree) {

		if (!qaInputs()) {
			Thread.currentThread().destroy();
		} else {

			//// setting up policy name and suggested file name

			String ID = resolveUniqueSuffix(this.textFieldMarketName.getText());
			String policy_name = this.textFieldPolicyName.getText() + ID;
			String market_name = this.textFieldMarketName.getText() + ID;
			filenameSuggestion = this.textFieldPolicyName.getText().replaceAll("/", "-").replaceAll(" ", "_") + ".csv";

			// clearing info to save to file
			fileContent = this.getMetaDataContent(tree, market_name, policy_name);
			String content_p1 = "";
			String content_p2 = "";

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

			// header 1:
			content_p1 += "INPUT_TABLE" + vars.getEol();
			content_p1 += "Variable ID" + vars.getEol();
			content_p1 += header_part1 + vars.getEol() + vars.getEol();
			content_p1 += "region,sector,subsector,tech,year,input,coefficient,policy,output-ratio,pMultiplier"
					+ vars.getEol();

			// header 2:
			content_p2 += "INPUT_TABLE" + vars.getEol();
			content_p2 += "Variable ID" + vars.getEol();
			content_p2 += header_part2 + vars.getEol() + vars.getEol();
			content_p2 += "region,policy,market,type,year,constrained" + vars.getEol();

			///// ----- Constructing data components

			// loop over regions
			for (int r = 0; r < listOfSelectedLeaves.length; r++) {
				String region = listOfSelectedLeaves[r];

				// for each region, sector/subsector, get list of techs
				String subsector = comboBoxSubsector.getValue();
				String sector = "";
				if ((subsector.equals("Light Truck")) || (subsector.equals("Medium Truck"))
						|| (subsector.equals("Heavy Truck"))) {
					sector = "trn_freight_road";
				} else {
					sector = "trn_pass_road_LDV_4W";
				}

				for (int i = 0; i < dataArrayList.size(); i++) {
					String str = dataArrayList.get(i).replaceAll(" ", "").trim();
					year_list[i] = utils.splitString(str, ",")[0];
					value_list[i] = utils.splitString(str, ",")[1];
					valuef_list[i] = Double.parseDouble(value_list[i]);

					String yr = year_list[i];
					double val = valuef_list[i];

					ObservableList<String> tech_list = this.checkComboBoxTech.getCheckModel().getCheckedItems();

					for (int t = 0; t < tech_list.size(); t++) {
						String tech = tech_list.get(t);

						String load_str = utils.getTrnVehInfo("load", region, sector, subsector, tech, yr);
						if (load_str == null) {
							System.out.println("why null?");
						}
						double load = Double.parseDouble(load_str);

						String coef_str = utils.getTrnVehInfo("intensity", region, sector, subsector, tech, yr);
						if (coef_str == null) {
							// hack since NG vehicles are not in the coef list
							coef_str = "5000";
							load = 0.0;
						}
						double coef = Double.parseDouble(coef_str);

						String io = yr + "_" + policy_name;
						String iom = io + "Mkt";

						String outputratio = "";
						String pMultiplier = "";

						// Convert MPG target to GJ/million-km basis for comparison with coef.
						// Energy content: 0.1203 GJ/gallon; distance conversion: 1.61 km/mile
						double gjPerGallon = 0.1203;
						double targetGJPerMillionKm = (gjPerGallon / (1.61 * val)) * 1e6;

									boolean which = vars.isGcamVersionPre8_5();

									if (which) { // for GCAM8.2 and earlier versions
							// coef is already in GJ/million-km; outputratio is target intensity on same
							// basis
							outputratio = formatDisplayValue((float) targetGJPerMillionKm);
							pMultiplier = formatDisplayValue((float) (load * 1e9));
						} else { // for GCAM8.5 and later versions
							// coef is in MJ/km; convert to GJ/million-km for consistency with 8.2 workflow
							coef *= 1000000.0; // convert from GJ/km to GJ/million km
							outputratio = formatDisplayValue((float) targetGJPerMillionKm);
							// pMultiplier=Double.toString((float)(load*1e9));
							pMultiplier = formatDisplayValue(1.0);
						}

						content_p1 += region + "," + sector + "," + subsector + "," + tech + "," + yr + "," + io + ","
								+ coef + "," + io + "," + outputratio + "," + pMultiplier + vars.getEol();
						if (t == 0)
							content_p2 += region + "," + io + "," + iom + ",RES," + yr + ",1" + vars.getEol();
					}
				}

				fileContent += content_p1 + vars.getEol();
				fileContent += content_p2;

				System.out.println("Done");
			}
		}

	}

	/**
	 * Generates the metadata content string for the scenario component, including
	 * selected subsector, technologies, units, policy/market names, and table data.
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
	 * Loads content from a list of strings (typically from a file) and populates
	 * the UI fields accordingly. Parses each line and updates the corresponding UI
	 * control.
	 *
	 * @param content The list of content lines to load
	 */
	@Override
	public void loadContent(ArrayList<String> content) {
		if (content == null) {
			return;
		}
		for (String line : content) {
			int pos = line.indexOf(":");
			if (line.startsWith("#") && (pos > -1)) {
				String param = line.substring(1, pos).trim().toLowerCase();
				String value = line.substring(pos + 1).trim();
				switch (param) {
				case "subsector":
					comboBoxSubsector.setValue(value);
					break;
				case "technologies":
					checkComboBoxTech.getCheckModel().clearChecks();
					String[] set = utils.splitString(value, ";");
					for (String item : set) {
						checkComboBoxTech.getCheckModel().check(item.trim());
					}
					break;
				case "units":
					comboBoxWhichUnits.setValue(value);
					break;
				case "application mode":
					if (APPLICATION_MODE_OPTIONS[0].equals(value) || APPLICATION_MODE_OPTIONS[1].equals(value)) {
						comboBoxApplicationMode.setValue(value);
					}
					break;
				case "policy name":
					textFieldPolicyName.setText(value);
					break;
				case "market name":
					textFieldMarketName.setText(value);
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
	}

	/**
	 * Helper method to validate table data years against allowable policy years.
	 * Checks if at least one year in the table matches allowable years.
	 *
	 * @return true if at least one year matches allowable years, false otherwise
	 */
	private boolean validateTableDataYears() {
		List<Integer> listOfAllowableYears = vars.getAllowablePolicyYears();
		ObservableList<DataPoint> data = paneForComponentDetails != null ? this.paneForComponentDetails.table.getItems()
				: null;
		if (data == null)
			return false;
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
	 * Performs QA checks on the current UI state to ensure all required inputs are
	 * valid. Displays warnings or error messages as needed.
	 *
	 * @return true if all inputs are valid, false otherwise
	 */
	protected boolean qaInputs() {
		TreeView<String> tree = paneForCountryStateTree.getTree();
		int errorCount = 0;
		StringBuilder message = new StringBuilder();
		try {
			errorCount += validateRegionSelection(tree, message);
			boolean hasRows = paneForComponentDetails != null && !paneForComponentDetails.table.getItems().isEmpty();
			boolean yearsMatch = !hasRows || validateTableDataYears();
			errorCount += validateTableEntries(message, hasRows, yearsMatch);
			errorCount += validateRequiredSelection(message, comboBoxSubsector, "Sector");
			errorCount += validateRequiredSelection(message, checkComboBoxTech, "Tech");
			errorCount += validateRequiredSelection(message, comboBoxWhichUnits, "Treatment");
			errorCount += validateRequiredText(message, textFieldMarketName, "market name");
			errorCount += validateRequiredText(message, textFieldPolicyName, "policy name");
		} catch (Exception e1) {
			errorCount++;
			message.append("Error in QA of entries").append(vars.getEol());
		}
		return finalizeQaValidation(errorCount, message);
	}

	/**
	 * Sets the units label based on the selected technologies. If units are
	 * inconsistent, sets a warning label. Optionally updates a UI label if needed.
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
		return unit;
	}
}
