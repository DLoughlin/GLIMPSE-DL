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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * TabPollutantTaxCap provides the UI and logic to create and edit pollutant
 * tax and cap scenario components in the GLIMPSE Scenario Builder.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Present measure, pollutant, and sector/category controls to the user</li>
 *   <li>Support automatic and manual naming for policies and markets</li>
 *   <li>Collect and validate time-series cap/tax values and selected regions</li>
 *   <li>Generate CSV scenario component files with embedded metadata</li>
 * </ul>
 * </p>
 *
 * <p>
 * Threading: all UI interactions must occur on the JavaFX Application Thread.
 * Methods that update UI components use Platform.runLater where appropriate.
 * </p>
 */
public class TabPollutantTaxCap extends PolicyTab implements Runnable {
	// === Constants for UI Texts and Options ===
	//private static final double MAX_WIDTH = 225;
	//private static final double MIN_WIDTH = 175;
    private static final double MAX_WIDTH = 225;
    private static final double MIN_WIDTH = 175;
	private static final double PREF_WIDTH = LABEL_WIDTH;
	private static final String[] MEASURE_OPTIONS = { "Emission Tax ($/t)", "Emission Cap (Mt)"  };
	//private static final String[] POLLUTANT_OPTIONS = { "Select One", "CO2 (MT C)", "CO2 (MT CO2)", "GHG (MT CO2E)",
	//		"NOx (Tg)", "SO2 (Tg)", "PM2.5 (Tg)", "NMVOC (Tg)", "CO (Tg)", "NH3 (Tg)", "CH4 (Tg)", "N2O (Tg)" };
	private static final String LABEL_MEASURE = "Measure: ";
	private static final String LABEL_CATEGORY = "Category: ";
	private static final String LABEL_POLLUTANT = "Pollutant: ";
	// === Magic String Constants for Logic ===
	private static final String TAX = "Tax";
	private static final String CAP = "Cap";
	private static final String SELECT_ONE = "Select One";
	private static final String SELECT_ONE_OR_MORE = "Select One or More";
	private static final String ALL = "All";
	private static final String INPUT_TABLE = "INPUT_TABLE";
	private static final String VARIABLE_ID = "Variable ID";
	private static final String GLIMPSE_EMISSION_CAP = "GLIMPSEEmissionCap";
	private static final String GLIMPSE_EMISSION_TAX = "GLIMPSEEmissionTax";
	private static final String GLIMPSE_EMISSION_MARKET = "GLIMPSEEmissionMarket";
	private static final String GLIMPSE_ADD_CO2_SUBSPECIES_NEST = "GLIMPSEAddCO2Subspecies-Nest";
	private static final String GLIMPSE_ADD_CO2_SUBSPECIES = "GLIMPSEAddCO2Subspecies";
	private static final String GLIMPSE_GHG_EMISSION_CAP = "GLIMPSEGHGEmissionCap";
	private static final String GLIMPSE_GHG_EMISSION_TAX = "GLIMPSEGHGEmissionTax";
	private static final String GLIMPSE_LINKED_GHG_MARKET_P1 = "GLIMPSELinkedGHGEmissionMarketP1";
	private static final String GLIMPSE_LINKED_GHG_MARKET_P2 = "GLIMPSELinkedGHGEmissionMarketP2";
	private static final String GLIMPSE_EMISSION_CAP_PPS_P1 = "GLIMPSEEmissionCap-PPS-P1";
	private static final String GLIMPSE_EMISSION_CAP_PPS_P2 = "GLIMPSEEmissionCap-PPS-P2";
	private static final String EMISSION_TAX = "Emission Tax";
	private static final String GHG = "GHG";
	private static final String CO2 = "CO2";

	// === State ===
	public static String descriptionText = "";
	public static String runQueueStr = "Queue is empty.";

	// Add flag to prevent recursion in category check listener
	private boolean isAdjustingCategoryChecks = false;

	// === UI Controls ===
	private final Label labelComboBoxMeasure = createLabel(LABEL_MEASURE, LABEL_WIDTH);
	private final ComboBox<String> comboBoxMeasure = createComboBoxString();
	private final Label labelCheckComboBoxCategory = createLabel(LABEL_CATEGORY, LABEL_WIDTH);
	private final CheckComboBox<String> checkComboBoxCategory = createCheckComboBox();
	private final Label labelComboBoxPollutant = createLabel(LABEL_POLLUTANT, LABEL_WIDTH);
	private final ComboBox<String> comboBoxPollutant = createComboBoxString();
	// HBox for Auto and Unique checkboxes
	private final javafx.scene.layout.HBox hBoxAutoUnique = new javafx.scene.layout.HBox(8);

	/**
	 * Construct and initialize the pollutant tax/cap tab.
	 *
	 * The constructor configures default checkbox states (auto/unique naming),
	 * disables manual name fields when auto naming is enabled, registers shared
	 * event handlers from the superclass, and builds the tab UI layout.
	 *
	 * @param title  the tab title displayed in the TabPane
	 * @param stageX JavaFX Stage (kept for compatibility; not used directly)
	 */
	public TabPollutantTaxCap(String title, Stage stageX) {
		this.setText(title);
		this.setStyle(styles.getFontStyle());
		// Checkbox defaults and policy/market field enablement are managed in PolicyTab.
		super.setupEventHandlers();
		setupUIControls();
		setComponentWidths();
		setupUILayout();
		setPolicyAndMarketNames();
		

	}

	/**
	 * Initialize UI controls, populate choice lists, and wire listeners.
	 *
	 * This method prepares the left/center/right column controls, fills the
	 * measure, pollutant, and category lists using data from vars, configures
	 * default selections, and registers listeners that keep derived values
	 * (for example auto-generated names) in sync with user changes.
	 */
	private void setupUIControls() {
		// Set up left, center, and right columns of the UI
		setupLeftColumn();
		setupCenterColumn();
		setupRightColumn();
		// Populate measure, pollutant, category, and conversion options
		for (String option : MEASURE_OPTIONS) {
			comboBoxMeasure.getItems().add(option);
		}

		String[] pollutantOptions = vars.getPollutantList();
		for (String option : pollutantOptions) {
			comboBoxPollutant.getItems().add(option);
		}
		for (String option : vars.getCategoriesFromTechBnd()) {
			checkComboBoxCategory.getItems().add(option);
		}

		//initialize pulldown menus to default values
		setComboBoxPrompt(comboBoxPollutant, SELECT_ONE);
		setComboBoxPrompt(comboBoxMeasure, SELECT_ONE);
		configureCheckComboBoxSelectionTitle(checkComboBoxCategory, SELECT_ONE_OR_MORE, "Selected");
		
		checkComboBoxCategory.getCheckModel().clearChecks();
		checkComboBoxCategory.getCheckModel().check(ALL);
		checkComboBoxCategory.setDisable(true);
		comboBoxModificationType.getSelectionModel().selectFirst();
		comboBoxConvertFrom.getSelectionModel().selectFirst();
		comboBoxPollutant.setDisable(false);
		labelConvertFrom.setVisible(false);
		comboBoxConvertFrom.setVisible(false);

		// Listener for category selection changes
		checkComboBoxCategory.getCheckModel().getCheckedItems()
				.addListener((javafx.collections.ListChangeListener<String>) change -> {
					if (isAdjustingCategoryChecks)
						return;
					if (checkComboBoxCategory.isDisabled())
						return;
					// Only allow one category to be checked at a time, or 'All'
					if (checkComboBoxCategory.getCheckModel().getCheckedItems().size() > 1) {
						isAdjustingCategoryChecks = true;
						String lastChecked = null;
						while (change.next()) {
							if (change.wasAdded() && !change.getAddedSubList().isEmpty()) {
								lastChecked = change.getAddedSubList().get(change.getAddedSubList().size() - 1);
							}
						}
						// Uncheck all except lastChecked
						if ((lastChecked != null) && (lastChecked.equals(ALL))) {
							for (String item : new ArrayList<>(
									checkComboBoxCategory.getCheckModel().getCheckedItems())) {
								if (!item.equals(lastChecked)) {
									checkComboBoxCategory.getCheckModel().clearCheck(item);
								}
							}
						} else {
							checkComboBoxCategory.getCheckModel().clearCheck(ALL);
						}
						isAdjustingCategoryChecks = false;
					}
					// If no category is checked, default to 'All'
					if (checkComboBoxCategory.getCheckModel().getCheckedItems().size() == 0) {
						isAdjustingCategoryChecks = true;
						checkComboBoxCategory.getCheckModel().check(ALL);
						isAdjustingCategoryChecks = false;
					}
					setPolicyAndMarketNames();
				});

		// Listener for measure selection changes
		setOnAction(comboBoxMeasure, e -> {
			applyMeasureSelection();
		});
		// Listener for pollutant selection changes
		setOnAction(comboBoxPollutant, e -> {
			applyPollutantSelection();
		});

	}

	/**
	 * Applies measure-dependent UI state (conversion controls visibility) and
	 * refreshes generated names.
	 */
	private void applyMeasureSelection() {
		String selectedMeasure = comboBoxMeasure.getSelectionModel().getSelectedItem();
		if (selectedMeasure != null && selectedMeasure.startsWith(EMISSION_TAX)) {
			labelConvertFrom.setVisible(true);
			comboBoxConvertFrom.setVisible(true);
		} else {
			labelConvertFrom.setVisible(false);
			comboBoxConvertFrom.setVisible(false);
		}
		setPolicyAndMarketNames();
	}

	/**
	 * Applies pollutant-dependent category enable/disable behavior and refreshes
	 * generated names.
	 */
	private void applyPollutantSelection() {
		String selectedItem = comboBoxPollutant.getSelectionModel().getSelectedItem();
		if (selectedItem != null && !SELECT_ONE.equals(selectedItem)) {
			if (selectedItem.startsWith(CO2)) {
				checkComboBoxCategory.setDisable(false);
			} else {
				checkComboBoxCategory.getCheckModel().clearChecks();
				checkComboBoxCategory.getCheckModel().check(ALL);
				checkComboBoxCategory.setDisable(true);
			}
		}
		setPolicyAndMarketNames();
	}

	/**
	 * Sets up the left column of the UI grid pane, including labels and controls for specification and population.
	 */
	private void setupLeftColumn() {
		gridPaneLeft.add(utils.createLabel("Specification:"), 0, 0, 2, 1);
		hBoxAutoUnique.getChildren().clear();
		hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
		gridPaneLeft.addColumn(0, labelComboBoxMeasure, labelComboBoxPollutant, labelCheckComboBoxCategory, new Label(),
				new Separator(), new Label("Names:"), labelPolicyName, labelMarketName, new Label(), new Separator(),
				utils.createLabel("Populate:"), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount,
				labelGrowth, labelConvertFrom);
		gridPaneLeft.addColumn(1, comboBoxMeasure, comboBoxPollutant, checkComboBoxCategory, new Label(),
				new Separator(), hBoxAutoUnique, textFieldPolicyName, textFieldMarketName, new Label(),
				new Separator(), new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear,
				textFieldInitialAmount, textFieldGrowth, comboBoxConvertFrom);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
//		gridPaneLeft.setVgap(3.);
//		//		gridPaneLeft.setStyle(styles.getStyle2());
//		// Use light background so the left panel matches the dialog's button area
//		// gridPaneLeft.setStyle(styles.getStyle2() + styles.getLightBackgroundStyle());
//		// Use standard background so left panel matches MPG Target left pane
//		gridPaneLeft.setStyle(styles.getStyle2());
//		scrollPaneLeft.setStyle(styles.getStyle2());
		scrollPaneLeft.setContent(gridPaneLeft);
//        // Ensure center and right scroll panes use same background color
//        scrollPaneCenter.setStyle(styles.getStyle2());
//        scrollPaneRight.setStyle(styles.getStyle2());
	}

	/**
	 * Sets the widths for all ComboBox and CheckComboBox components for
	 * consistency. Ensures a uniform look and feel for the UI.
	 */
	private void setComponentWidths() {
		setComboBoxWidths(checkComboBoxCategory);
		setComboBoxWidths(comboBoxMeasure);
		setComboBoxWidths(comboBoxModificationType);
		setComboBoxWidths(comboBoxPollutant);
	}

	/**
	 * Sets the widths for a ComboBox for consistency.
	 *
	 * @param comboBox ComboBox to set widths for
	 */
	private void setComboBoxWidths(ComboBox<String> comboBox) {
		comboBox.setMaxWidth(MAX_WIDTH);
		comboBox.setMinWidth(MIN_WIDTH);
		comboBox.setPrefWidth(PREF_WIDTH);
	}

	/**
	 * Sets the widths for a CheckComboBox for consistency.
	 *
	 * @param comboBox CheckComboBox to set widths for
	 */
	private void setComboBoxWidths(CheckComboBox<String> comboBox) {
		comboBox.setMaxWidth(MAX_WIDTH);
		comboBox.setMinWidth(MIN_WIDTH);
		comboBox.setPrefWidth(PREF_WIDTH);
	}

	/**
	 * Compute and set default policy and market names when auto-naming is enabled.
	 *
	 * The generated name reflects the selected measure, pollutant, category
	 * and the (possibly aggregated) region string. Name components are
	 * sanitized (spaces and dashes replaced) to produce safe identifiers.
	 * UI updates are executed on the JavaFX thread via Platform.runLater.
	 */
	protected void setPolicyAndMarketNames() {
		Platform.runLater(() -> {
			if (checkBoxUseAutoNames.isSelected()) {
				String measure = "--";
				String pollutant = "--";
				String category = "--";
				String state = "--";
				try {
					// Determine measure type (Tax/Cap)
					String s = comboBoxMeasure.getValue();
					if (s != null && s.contains("Tax"))
						measure = "polTax";
					if (s != null && s.contains("Cap"))
						measure = "polCap";
					// Determine category selection
					int cats = checkComboBoxCategory.getCheckModel().getCheckedItems().size();
					if (cats == 0) {
						category = "All";
					} else if (cats == 1) {
						category = checkComboBoxCategory.getCheckModel().getCheckedItems().get(0).replaceAll(" ", "_").replaceAll("-", "_");
					} else {
						category = "Mult";
					}
					// Get pollutant name (first word)
					s = comboBoxPollutant.getValue();
					if (s != null && !s.equals("Select One")) {
						pollutant = utils.splitString(s, " ")[0];
					}
					// Get selected regions and process for name
					String[] listOfSelectedRegions = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
					if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
						listOfSelectedRegions = utils.removeUSADuplicate(listOfSelectedRegions);
						String state_str = utils.returnAppendedString(listOfSelectedRegions).replace(",", "");
						if (state_str.length() < 9) {
							state = state_str;
						} else {
							state = "Reg";
						}
					}
					// Compose name and apply string replacements for validity
					String name = measure + "_" + category + "_" + pollutant + "_" + state;
					name = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("--", "_").replaceAll("_-_", "_").replaceAll("---", "");
					textFieldMarketName.setText(name + "_Mkt");
					textFieldPolicyName.setText(name);
				} catch (Exception e) {
					System.out.println("Error trying to auto-name market");
				}
			}
		});
	}

	/**
	 * Runnable implementation: triggers saving the scenario component. Calls
	 * saveScenarioComponent().
	 * <p>
	 * This method is used to run the save operation on the JavaFX Application Thread.
	 * </p>
	 */
	@Override
	public void run() {
		Platform.runLater(() -> saveScenarioComponent());
	}

	/**
	 * Saves the scenario component by generating metadata and CSV content. Uses
	 * selected regions, pollutant, sector, and cap/tax values.
	 * <p>
	 * This is the main entry point for saving the scenario component. Calls the
	 * overloaded saveScenarioComponent(TreeView) method with the current region
	 * tree.
	 * </p>
	 */
	@Override
	public void saveScenarioComponent() {
		saveScenarioComponent(paneForCountryStateTree.getTree());
	}

	/**
	 * Saves the scenario component using the provided region tree.
	 *
	 * Validates inputs then dispatches to the proper generator based on the
	 * selected pollutant and measure (robust CO2 cap, GHG tax/cap, or
	 * flexible tax/cap). This is the main entry point used by run().
	 *
	 * @param tree TreeView of selected regions
	 */
	private void saveScenarioComponent(TreeView<String> tree) {
		// Validate inputs before proceeding
		if (!qaInputs()) {
			return;
		}

		// Get selected regions and remove duplicates
		String[] listOfSelectedRegions = utils.getAllSelectedRegions(tree);
		listOfSelectedRegions = utils.removeUSADuplicate(listOfSelectedRegions);

        String ID = null;
        if (checkBoxUseUniqueNames.isSelected()) { 
        	ID = utils.getUniqueString();
        } else {
        	ID="";
        }
		String policy_name = textFieldPolicyName.getText() + ID;
		String market_name = textFieldMarketName.getText() + ID;
		filenameSuggestion = textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";

		// Get selected categories
		List<String> cats = checkComboBoxCategory.getCheckModel().getCheckedItems();

		String measure = comboBoxMeasure.getValue();
		measure = measure.contains(CAP) ? CAP : TAX;

		// Get selected pollutant
		String pol_menu = comboBoxPollutant.getSelectionModel().getSelectedItem().trim() + " ";
		String pol = pol_menu.substring(0, pol_menu.indexOf(" ")).trim();

		// Generate metadata for file header
		fileContent = getMetaDataContent(tree, market_name, policy_name);

		// Route to appropriate file generation method based on pollutant and measure
		// Cases:
		// Caps:
		// 1. CO2 cap in MTC on all categories
		// 2. CO2 cap in MTCO2 on all categories
		// 3. GHG cap in MTCO2E on all categories
		// 4. CO2 cap in MTC on specific categories
		// 5. CO2 cap in MTCO2 on specific categories
		// 6. Non-GHG cap in Tg on all categories
		// Taxes:
		// 7. CO2 tax in $/MTC on all categories
		// 8. CO2 tax in $/MTCO2 on all categories
		// 9. GHG tax in $/MTCO2E on all categories
		// 10. Non-GHG tax in $/Tg on all categories

		if (measure.equals(CAP)) {
			if (pol.startsWith("CO2")) {
				if (cats.contains(ALL)) { // cases 1, 2
					saveScenarioComponentRobustCO2Cap(listOfSelectedRegions, pol, pol_menu, market_name, policy_name);
					return;

				} else { // cases 4, 5
					saveScenarioComponentFlexTaxOrCap(listOfSelectedRegions, measure, cats, pol, pol_menu,
							market_name, policy_name, ID);
					return;
				}
			} else if ((pol.startsWith(GHG))||(pol.startsWith("F-"))) { // case 3
				saveScenarioComponentGHGTaxOrCap(listOfSelectedRegions, measure, pol, pol_menu, market_name,
						policy_name);
				return;
			} else { // case 6
				saveScenarioComponentFlexTaxOrCap(listOfSelectedRegions, measure, cats, pol, pol_menu,
						market_name, policy_name, ID);
				return;
			}
		} else if (measure.equals(TAX)) {
			if ((pol.startsWith(GHG))||(pol.startsWith("F-"))) { // case 9
				saveScenarioComponentGHGTaxOrCap(listOfSelectedRegions, measure, pol, pol_menu, market_name,
						policy_name);
				return;
			} else { // cases 7, 8, 10
				saveScenarioComponentFlexTaxOrCap(listOfSelectedRegions, measure, cats, pol, pol_menu,
						market_name, policy_name, ID);
				return;
			}
		} else {
			System.out.println("Cap or tax type not supported!");
		}

	}

	/**
	 * Special implementation for robust CO2 cap policies, generating scenario files
	 * for complex CO2 cap scenarios.
	 * <p>
	 * This method generates two tables: one for the cap policy and one for the linked market scenario.
	 * It also applies a demand adjustment factor if the units are MT CO2.
	 * </p>
	 *
	 * @param listOfSelectedRegions Array of selected region names
	 * @param pol                   Pollutant string
	 * @param pol_menu              Pollutant menu string
	 * @param market_name           Market name
	 * @param policy_name           Policy name
	 */
	private void saveScenarioComponentRobustCO2Cap(String[] listOfSelectedRegions, String pol, String pol_menu,
			String market_name, String policy_name) {

		// Add metadata and table headers for robust CO2 cap scenario
		fileContent += INPUT_TABLE + vars.getEol();
		fileContent += VARIABLE_ID + vars.getEol();
		fileContent += GLIMPSE_EMISSION_CAP_PPS_P1 + vars.getEol() + vars.getEol();
		fileContent += "region,policy,policy-type,min-price,market,year,cap" + vars.getEol();
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
			for (String state : listOfSelectedRegions) {
				ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();
				for (String data_str : data) {
					data_str = data_str.replaceAll(" ", "");
					fileContent += state + "," + policy_name + ",tax,1," + market_name + "," + data_str + vars.getEol();
				}
			}
		}
		// Determine demand adjustment factor based on units
		String dmdAdj = "1";
		if (pol_menu.contains("(MT CO2)"))
			dmdAdj = "3.667";

		// Add additional table for linked market scenario if multiple regions
		if (listOfSelectedRegions != null && listOfSelectedRegions.length >= 1) {
			fileContent += vars.getEol();
			fileContent += INPUT_TABLE + vars.getEol();
			fileContent += VARIABLE_ID + vars.getEol();
			fileContent += GLIMPSE_EMISSION_CAP_PPS_P2 + vars.getEol();
			fileContent += vars.getEol();
			fileContent += "region,linked-ghg-policy,price-adjust0,demand-adjust0,market,linked-policy,price-unit,output-unit,price-adjust1,demandAdjust1"
					+ vars.getEol();
			for (String region : listOfSelectedRegions) {
				fileContent += region + "," + pol + ",0,0," + market_name + "," + policy_name + ",1990$/Tg,Tg,1,"
						+ dmdAdj + vars.getEol();
				double progress = (double) 1 / (listOfSelectedRegions.length - 1);
				updateProgressBar(progress);
			}
		}
	}

	/**
	 * Special implementation for flexible tax or cap policies, generating scenario
	 * files for non-GHG/CO2 pollutants or category-specific policies.
	 * <p>
	 * This method handles writing multiple tables for flexible tax/cap policies, including
	 * subspecies tables for CO2 with specific categories. It also manages temporary files
	 * and concatenates them at the end.
	 * </p>
	 *
	 * @param listOfSelectedRegions Array of selected region names
	 * @param measure               Measure type (Tax/Cap)
	 * @param categories            List of selected categories
	 * @param pol                   Pollutant string
	 * @param pol_menu              Pollutant menu string
	 * @param market_name           Market name
	 * @param policy_name           Policy name
	 * @param ID                    Unique identifier for this policy instance
	 */
	private void saveScenarioComponentFlexTaxOrCap(String[] listOfSelectedRegions, String measure,
			List<String> categories, String pol, String pol_menu, String market_name, String policy_name, String ID) {

		// Initialize temp files for writing scenario data
		initializeTempFiles();
		files.writeToBufferedFile(bw0, fileContent);
		fileContent = "use temp file";

		ArrayList<String> tempfiles = new ArrayList<String>();
		ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();

		// If not all categories are selected, append unique ID to pollutant name for uniqueness
		if (!categories.contains(ALL)) pol = pol + "_" + ID;

		// Write table headers based on measure type
		files.writeToBufferedFile(bw0, INPUT_TABLE + vars.getEol());
		files.writeToBufferedFile(bw0, VARIABLE_ID + vars.getEol());
		if (measure.equals(CAP)) {
			files.writeToBufferedFile(bw0, GLIMPSE_EMISSION_CAP + vars.getEol() + vars.getEol());
			files.writeToBufferedFile(bw0, "region,pollutant,market,year,cap" + vars.getEol());
		} else if (measure.equals(TAX)) {
			files.writeToBufferedFile(bw0, GLIMPSE_EMISSION_TAX + vars.getEol() + vars.getEol());
			files.writeToBufferedFile(bw0, "region,pollutant,market,year,tax" + vars.getEol());
		}

		// Write scenario data for each region
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
			String state = listOfSelectedRegions[0];

			for (String data_str : data) {
				data_str = data_str.replaceAll(" ", "").trim();
				int data_yr = Integer.parseInt(data_str.split(",")[0].trim());
				double data_val = Double.parseDouble(data_str.split(",")[1].trim());
				// Convert units if necessary
				if (pol_menu.contains("(MT CO2)")) {
					data_val = data_val / 3.667;
					data_str = data_yr + "," + formatDisplayValue(data_val);
				}
				files.writeToBufferedFile(bw0, state + "," + pol + "," + market_name + "," + data_str + vars.getEol());
			}
		}
		// If multiple regions, write additional market linkage table
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 1) {
			files.writeToBufferedFile(bw0, vars.getEol());
			files.writeToBufferedFile(bw0, INPUT_TABLE + vars.getEol());
			files.writeToBufferedFile(bw0, VARIABLE_ID + vars.getEol());
			files.writeToBufferedFile(bw0, GLIMPSE_EMISSION_MARKET + vars.getEol());
			files.writeToBufferedFile(bw0, vars.getEol());
			files.writeToBufferedFile(bw0, "region,pollutant,market" + vars.getEol());
			for (int s = 1; s < listOfSelectedRegions.length; s++) {
				files.writeToBufferedFile(bw0,
						listOfSelectedRegions[s] + "," + pol + "," + market_name + vars.getEol());
			}
		}

		int start_year = vars.getCalibrationYear();
		List<Integer> years = vars.getAllYears();
		int num_nonnested = 0;
		int num_nested = 0;

		// If specific categories are selected (CO2 only), write subspecies tables
		if (!categories.contains(ALL)) {

			files.writeToBufferedFile(bw1, vars.getEol());
			files.writeToBufferedFile(bw1, INPUT_TABLE + vars.getEol());
			files.writeToBufferedFile(bw1, VARIABLE_ID + vars.getEol());
			files.writeToBufferedFile(bw1, GLIMPSE_ADD_CO2_SUBSPECIES_NEST + vars.getEol());
			files.writeToBufferedFile(bw1, vars.getEol());
			files.writeToBufferedFile(bw1,
					"region,supplysector,nesting-subsector,subsector,technology,year,pollutant" + vars.getEol());

			files.writeToBufferedFile(bw2, vars.getEol());
			files.writeToBufferedFile(bw2, INPUT_TABLE + vars.getEol());
			files.writeToBufferedFile(bw2, VARIABLE_ID + vars.getEol());
			files.writeToBufferedFile(bw2, GLIMPSE_ADD_CO2_SUBSPECIES + vars.getEol());
			files.writeToBufferedFile(bw2, vars.getEol());
			files.writeToBufferedFile(bw2, "region,supplysector,subsector,technology,year,pollutant" + vars.getEol());

			int max_year = 0;
			for (String d : data) {
				int year = Integer.parseInt(d.split(",")[0].trim());
				if (year > max_year)
					max_year = year;
			}

			int l = 0;
			if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
				l++;
				for (String region : listOfSelectedRegions) { // loop over regions
					String[][] tech_list = vars.getTechInfo();
					int cols = tech_list[0].length;
					int rows = tech_list.length;
					for (int y : years) { // loop over years
						if ((y >= start_year) && (y <= max_year)) {

							for (String cat : categories) { // loop over selected categories

								String cat_lwc = cat.toLowerCase();
								for (int r = 0; r < rows; r++) { // loop over all technologies
									String sector_r = tech_list[r][0];
									String subsector_r = tech_list[r][1];
									String tech_r = tech_list[r][2];
									String cat_r = tech_list[r][cols - 1];

									// Avoid PV and wind technologies that don't have inputs
									if ((!tech_r.contains("PV")) && (!tech_r.contains("wind"))
											&& (!tech_r.contains("CSP"))) {
										String cat_r_lwc = cat_r.toLowerCase();
										if ((cat_lwc.equals(cat_r_lwc))) {
											String line = region + "," + sector_r + "," + subsector_r.replace("=>", ",") + ","
													+ tech_r.replace("=>", ",") + "," + y + "," + pol + vars.getEol();
											if ((subsector_r.contains("=>"))||(tech_r.contains("=>"))) {
												files.writeToBufferedFile(bw1, line);
												num_nested++;
											} else {
												files.writeToBufferedFile(bw2, line);
												num_nonnested++;
											}
										}
									}
								}
							}
						}
						double progress = (double) l / (listOfSelectedRegions.length - 1);
						updateProgressBar(progress);

					}
				}
			}
		}

		files.closeBufferedFile(bw0);
		tempfiles.add(temp_file0);

		files.closeBufferedFile(bw1);
		files.closeBufferedFile(bw2);

		if (num_nested > 0)
			tempfiles.add(temp_file1);
		if (num_nonnested > 0)
			tempfiles.add(temp_file2);

		files.concatDestSources(temp_file, tempfiles);

	}

	/**
	 * Special implementation for GHG tax/cap policies, generating scenario files
	 * for GHG policies.
	 * <p>
	 * This method generates tables for GHG tax/cap policies, including linkage tables for multiple regions
	 * and additional tables for GHGs and F-gases. It uses lists of pollutants and units from vars.
	 * </p>
	 *
	 * @param listOfSelectedRegions Array of selected region names
	 * @param measure               Measure type (Tax/Cap)
	 * @param pol                   Pollutant string
	 * @param pol_menu              Pollutant menu string
	 * @param market_name           Market name
	 * @param policy_name           Policy name
	 */
	private void saveScenarioComponentGHGTaxOrCap(String[] listOfSelectedRegions, String measure, String pol,
			String pol_menu, String market_name, String policy_name) {

		// Add metadata and table headers for GHG tax/cap scenario
		fileContent += INPUT_TABLE + vars.getEol();
		fileContent += VARIABLE_ID + vars.getEol();

		if (CAP.equals(measure)) {
			fileContent += GLIMPSE_GHG_EMISSION_CAP + vars.getEol();
			fileContent += vars.getEol();
			fileContent += "region,GHG-Policy,GHG-Market,year,cap" + vars.getEol();
		} else if (TAX.equals(measure)) {
			fileContent += GLIMPSE_GHG_EMISSION_TAX + vars.getEol();
			fileContent += vars.getEol();
			fileContent += "region,GHG-Policy,GHG-Market,year,tax" + vars.getEol();
		}

		// Write scenario data for each region
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
			String state = listOfSelectedRegions[0];
			ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();
			for (String data_str : data) {
				data_str = data_str.replace(" ", "");
				fileContent += state + "," + policy_name + "," + market_name + "," + data_str + vars.getEol();
			}
		}
		// If multiple regions, write additional market linkage table
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 1) {
			fileContent += vars.getEol();
			fileContent += INPUT_TABLE + vars.getEol();
			fileContent += VARIABLE_ID + vars.getEol();
			fileContent += GLIMPSE_EMISSION_MARKET + vars.getEol();
			fileContent += vars.getEol();
			fileContent += "region,pollutant,market" + vars.getEol();
			for (int s = 1; s < listOfSelectedRegions.length; s++) {
				fileContent += listOfSelectedRegions[s] + "," + policy_name + "," + market_name + vars.getEol();
				double progress = (double) s / (listOfSelectedRegions.length - 1);
				updateProgressBar(progress);
			}
		}
		// Write additional tables for GHGs and F-gases
		String fileContent2 = "";
		fileContent2 += vars.getEol();
		fileContent2 += INPUT_TABLE + vars.getEol();
		fileContent2 += VARIABLE_ID + vars.getEol();
		fileContent2 += GLIMPSE_LINKED_GHG_MARKET_P1 + vars.getEol();
		fileContent2 += vars.getEol();
		fileContent2 += "region,pollutant,GHG-market,GHG-Policy,price-adjust,demand-adjust,price-unit,output-unit"
				+ vars.getEol();

		List<String> pollutantList; 
		List<String> GHGs = vars.getGhgList();;
		List<String> price_adjust = vars.getGhgPriceAdjust();
		List<String> demand_adjust = vars.getGhgDemandAdjust();
		List<String> price_unit = vars.getGhgPriceUnit();
		List<String> output_unit = vars.getGhgOutputUnit();
		
		if (pol.startsWith("F-")) {
			// F-gases only
			pollutantList = vars.getFgasList();
		} else {
			// Non-F-gas GHGs and F-gases
			pollutantList = vars.getGhgList();
		}
		
		// Write GHGs and F-gases data for each region
		if (listOfSelectedRegions != null) {
			for (String state : listOfSelectedRegions) {
				for (int i = 0; i < GHGs.size(); i++) {
					for (String p : pollutantList) {

						if (GHGs.get(i).equals(p)) {
							fileContent2 += state + "," + GHGs.get(i) + "," + market_name + "," + policy_name + ","
								+ price_adjust.get(i) + "," + demand_adjust.get(i) + "," + price_unit.get(i) + "," + output_unit.get(i)
								+ vars.getEol();
						}
				    }
			   }
		    }
		}
		// If multiple regions, write additional linkage table
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 1) {
			fileContent2 += vars.getEol();
			fileContent2 += INPUT_TABLE + vars.getEol();
			fileContent2 += VARIABLE_ID + vars.getEol();
			fileContent2 += GLIMPSE_LINKED_GHG_MARKET_P2 + vars.getEol();
			fileContent2 += vars.getEol();
			fileContent2 += "region,pollutant,GHG-market,GHG-Policy" + vars.getEol();
			for (int s = 1; s < listOfSelectedRegions.length; s++) {
					for (String p : pollutantList) {
							fileContent2 += listOfSelectedRegions[s] + "," + p + "," + market_name + ","
									+ policy_name + vars.getEol();
				    }
				double progress = (double) s / (listOfSelectedRegions.length - 1);
				updateProgressBar(progress);
			}
		}

		if (fileContent2.length() > 0)
			fileContent += fileContent2;
	}

	/**
	 * Returns metadata content for the scenario component file, including measure,
	 * pollutant, sector, regions, and table data.
	 *
	 * @param tree   TreeView of selected regions
	 * @param market Market name
	 * @param policy Policy name
	 * @return Metadata string for file header
	 */
	public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
		StringBuilder rtn_str = new StringBuilder();
		rtn_str.append("########## Scenario Component Metadata ##########").append(vars.getEol());
		rtn_str.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
		rtn_str.append("#Measure: ").append(comboBoxMeasure.getValue()).append(vars.getEol());
		rtn_str.append("#Pollutant: ").append(comboBoxPollutant.getValue()).append(vars.getEol());
		rtn_str.append("#Categories: ")
				.append(utils.getStringFromList(checkComboBoxCategory.getCheckModel().getCheckedItems(), ","))
				.append(vars.getEol());
		if (policy == null)
			market = textFieldPolicyName.getText();
		rtn_str.append("#Policy name: ").append(policy).append(vars.getEol());
		if (market == null)
			market = textFieldMarketName.getText();
		rtn_str.append("#Market name: ").append(market).append(vars.getEol());
		String[] listOfSelectedRegions = utils.getAllSelectedRegions(tree);
		listOfSelectedRegions = utils.removeUSADuplicate(listOfSelectedRegions);
		String states = utils.returnAppendedString(listOfSelectedRegions);
		rtn_str.append("#Regions: ").append(states).append(vars.getEol());
		ArrayList<String> table_content = paneForComponentDetails.getDataYrValsArrayList();
		for (String row : table_content) {
			rtn_str.append("#Table data:").append(row).append(vars.getEol());
		}
		rtn_str.append("#################################################").append(vars.getEol());
		return rtn_str.toString();
	}

	/**
	 * Loads content into the tab from a list of strings (e.g., when editing a
	 * component). Populates measure, pollutant, sector, regions, and table data
	 * from file content.
	 * <p>
	 * This method parses each line of the input content, updating the UI controls and
	 * internal data structures accordingly. It supports loading all relevant fields for
	 * a scenario component.
	 * </p>
	 *
	 * @param content List of file lines to load
	 */
	@Override
	public void loadContent(ArrayList<String> content) {
		for (String line : content) {
			int pos = line.indexOf(":");
			if (line.startsWith("#") && (pos > -1)) {
				String param = line.substring(1, pos).trim().toLowerCase();
				String value = line.substring(pos + 1).trim();
				switch (param) {
				case "measure":
					comboBoxMeasure.setValue(value);
					applyMeasureSelection();
					break;
				case "pollutant":
					comboBoxPollutant.setValue(value);
					applyPollutantSelection();
					break;
				case "categories":
					checkComboBoxCategory.getCheckModel().clearChecks();
					String[] items = utils.splitString(value, ",");
					for (String item : items) {
						if (item.equals(ALL)) {
							checkComboBoxCategory.getCheckModel().check(ALL);
						} else {
							checkComboBoxCategory.getCheckModel().check(item);
						}
					}
					setPolicyAndMarketNames();
					break;
				case "policy name":
					textFieldPolicyName.setText(value);
					break;
				case "market name":
					textFieldMarketName.setText(stripUniqueSuffixFromLoadedMarketName(value));
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
				}
			}
		}
		this.setPolicyAndMarketNames();
		paneForComponentDetails.updateTable();
	}

	/**
	 * Helper method to validate table data years against allowable policy years.
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
			Integer year = Integer.parseInt(dp.getYear().trim());
			if (listOfAllowableYears.contains(year)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Validates all required inputs before saving the scenario component. Checks
	 * for at least one region, at least one table entry, and required selections.
	 * <p>
	 * This method performs a series of checks on the user's input, including region selection,
	 * table data, measure, category, pollutant, and policy name. It displays warnings or errors
	 * if any required input is missing or invalid.
	 * </p>
	 *
	 * @return true if all inputs are valid, false otherwise
	 */
	protected boolean qaInputs() {
		TreeView<String> tree = paneForCountryStateTree.getTree();
		int error_count = 0;
		StringBuilder message = new StringBuilder();
		try {
			error_count += validateRegionSelection(tree, message);
			boolean hasRows = paneForComponentDetails != null && paneForComponentDetails.table.getItems().size() > 0;
			error_count += validateTableEntries(message, hasRows, hasRows && validateTableDataYears());
			error_count += validateRequiredSelection(message, comboBoxMeasure, "Action", SELECT_ONE);
			error_count += validateRequiredSelection(message, checkComboBoxCategory, "Sector");
			error_count += validateRequiredSelection(message, comboBoxPollutant, "Parameter", SELECT_ONE);
			error_count += validateRequiredText(message, textFieldPolicyName, "market name");
		} catch (Exception e1) {
			error_count++;
			message.append("Error in QA of entries").append(vars.getEol());
		}
		return finalizeQaValidation(error_count, message);
	}

	/**
	 * Updates the progress bar in a thread-safe way.
	 *
	 * @param progress Progress value between 0.0 and 1.0
	 */
	private void updateProgressBar(double progress) {
		Platform.runLater(() -> progressBar.setProgress(progress));
	}
}
