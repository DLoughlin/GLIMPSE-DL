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

import java.io.BufferedWriter;
import java.io.File;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TabPollutantTaxCap provides the user interface and logic for creating and
 * editing pollutant tax/cap policies in the GLIMPSE Scenario Builder.
 *
 * <p>
 * <b>Main responsibilities:</b>
 * <ul>
 *   <li>Allow users to select measure type (tax or cap), pollutant, and sector/category</li>
 *   <li>Configure policy and market names (auto/manual)</li>
 *   <li>Specify and populate cap/tax values over time</li>
 *   <li>Validate, import, and export scenario component data as CSV</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Support for multiple pollutants (CO2, GHG, NOx, SO2, etc.)</li>
 *   <li>Automatic and manual naming for policy and market</li>
 *   <li>Dynamic enabling/disabling of UI controls based on selections</li>
 *   <li>Validation of user input and units</li>
 *   <li>Progress tracking for file generation</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Usage:</b>
 * <pre>
 * TabPollutantTaxCap tab = new TabPollutantTaxCap("Pollutant Tax/Cap", stage);
 * // Add to TabPane, interact via UI
 * </pre>
 * </p>
 *
 * <p>
 * <b>Thread Safety:</b> This class is not thread-safe and should be used only
 * on the JavaFX Application Thread.
 * </p>
 *
 * <p>
 * <b>Class Details:</b>
 * <ul>
 *   <li>Extends {@link PolicyTab} and implements {@link Runnable}.</li>
 *   <li>Handles UI setup, event listeners, and scenario file generation for pollutant tax/cap policies.</li>
 *   <li>Supports robust CO2 cap, GHG tax/cap, and flexible tax/cap for other pollutants.</li>
 *   <li>Provides methods for loading, validating, and saving scenario component data.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Key Methods:</b>
 * <ul>
 *   <li>{@link #TabPollutantTaxCap(String, Stage)} - Constructor, sets up UI and listeners.</li>
 *   <li>{@link #setupUIControls()} - Initializes UI controls and listeners.</li>
 *   <li>{@link #saveScenarioComponent()} - Main entry for saving scenario data.</li>
 *   <li>{@link #saveScenarioComponentFlexTaxOrCap(String[], String, String, List, String, String, String, String, String)} - Handles flexible tax/cap file generation.</li>
 *   <li>{@link #saveScenarioComponentGHGTaxOrCap(String[], String, String, String, String, String)} - Handles GHG tax/cap file generation.</li>
 *   <li>{@link #saveScenarioComponentRobustCO2Cap(String[], String, String, String, String)} - Handles robust CO2 cap file generation.</li>
 *   <li>{@link #getMetaDataContent(TreeView, String, String)} - Generates metadata for scenario files.</li>
 *   <li>{@link #loadContent(ArrayList)} - Loads scenario data from file.</li>
 *   <li>{@link #qaInputs()} - Validates user input before saving.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>See Also:</b>
 * <ul>
 *   <li>{@link PolicyTab}</li>
 *   <li>{@link DataPoint}</li>
 *   <li>{@link PaneForComponentDetails}</li>
 *   <li>{@link Utils}</li>
 * </ul>
 * </p>
 */
public class TabPollutantTaxCap extends PolicyTab implements Runnable {
	// === Constants for UI Texts and Options ===
	private static final double MAX_WIDTH = 225;
	private static final double MIN_WIDTH = 175;
	private static final double PREF_WIDTH = LABEL_WIDTH;
	private static final String[] MEASURE_OPTIONS = { "Select One", "Emission Cap (Mt)", "Emission Tax ($/t)" };
	//private static final String[] POLLUTANT_OPTIONS = { "Select One", "CO2 (MT C)", "CO2 (MT CO2)", "GHG (MT CO2E)",
	//		"NOx (Tg)", "SO2 (Tg)", "PM2.5 (Tg)", "NMVOC (Tg)", "CO (Tg)", "NH3 (Tg)", "CH4 (Tg)", "N2O (Tg)" };
	private static final String LABEL_MEASURE = "Measure: ";
	private static final String LABEL_CATEGORY = "Category: ";
	private static final String LABEL_POLLUTANT = "Pollutant: ";
	// === Magic String Constants for Logic ===
	private static final String TAX = "Tax";
	private static final String CAP = "Cap";
	private static final String SELECT_ONE = "Select One";
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

	/**
	 * Constructs a TabPollutantTaxCap for the given title and stage. Sets up all UI
	 * controls, listeners, and default values for the pollutant tax/cap policy tab.
	 *
	 * @param title  the tab title
	 * @param stageX JavaFX Stage (not used directly)
	 */
	public TabPollutantTaxCap(String title, Stage stageX) {
		this.setText(title);
		this.setStyle(styles.getFontStyle());
		checkBoxUseAutoNames.setSelected(true);
		textFieldPolicyName.setDisable(true);
		textFieldMarketName.setDisable(true);
		super.setupEventHandlers();
		setupUIControls();
		setComponentWidths();
		setupUILayout();
		setPolicyAndMarketNames();
	}

	/**
	 * Initializes and configures all UI controls, populates combo boxes, and sets
	 * up listeners for user interaction.
	 */
	private void setupUIControls() {
		setupLeftColumn();
		setupCenterColumn();
		setupRightColumn();
		// Populate measure, pollutant, category, and conversion options
		for (String option : MEASURE_OPTIONS) {
			comboBoxMeasure.getItems().add(option);
		}
		comboBoxPollutant.getItems().add("Select One");
		String[] pollutantOptions = vars.getPollutantList();
		for (String option : pollutantOptions) {
			comboBoxPollutant.getItems().add(option);
		}
		for (String option : vars.getCategoriesFromTechBnd()) {
			checkComboBoxCategory.getItems().add(option);
		}

		comboBoxMeasure.getSelectionModel().selectFirst();
		comboBoxPollutant.getSelectionModel().selectFirst();
		checkComboBoxCategory.getCheckModel().clearChecks();
		checkComboBoxCategory.getCheckModel().check(ALL);
		checkComboBoxCategory.setDisable(true);
		comboBoxModificationType.getSelectionModel().selectFirst();
		comboBoxConvertFrom.getSelectionModel().selectFirst();
		comboBoxPollutant.setDisable(false);
		labelConvertFrom.setVisible(false);
		comboBoxConvertFrom.setVisible(false);

		// Listeners and actions
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

		setOnAction(comboBoxMeasure, e -> {
			// Show/hide conversion controls based on measure type
			if (comboBoxMeasure.getSelectionModel().getSelectedItem().startsWith(EMISSION_TAX)) {
				labelConvertFrom.setVisible(true);
				comboBoxConvertFrom.setVisible(true);
			} else {
				labelConvertFrom.setVisible(false);
				comboBoxConvertFrom.setVisible(false);
			}
			setPolicyAndMarketNames();
		});
		setOnAction(comboBoxPollutant, e -> {
			String selectedItem = comboBoxPollutant.getSelectionModel().getSelectedItem();
			if (!SELECT_ONE.equals(selectedItem)) {
				if (selectedItem.startsWith(CO2)) {
					checkComboBoxCategory.setDisable(false);
				} else {
					checkComboBoxCategory.getCheckModel().clearChecks();
					checkComboBoxCategory.getCheckModel().check(ALL);
					checkComboBoxCategory.setDisable(true);
				}
			}
			setPolicyAndMarketNames();
		});

	}

	/**
	 * Sets up the left column of the UI grid pane, including labels and controls for specification and population.
	 */
	private void setupLeftColumn() {
		gridPaneLeft.add(utils.createLabel("Specification:"), 0, 0, 2, 1);
		gridPaneLeft.addColumn(0, labelComboBoxMeasure, labelComboBoxPollutant, labelCheckComboBoxCategory, new Label(),
				new Separator(), labelUseAutoNames, labelPolicyName, labelMarketName, new Label(), new Separator(),
				utils.createLabel("Populate:"), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount,
				labelGrowth, labelConvertFrom);
		gridPaneLeft.addColumn(1, comboBoxMeasure, comboBoxPollutant, checkComboBoxCategory, new Label(),
				new Separator(), checkBoxUseAutoNames, textFieldPolicyName, textFieldMarketName, new Label(),
				new Separator(), new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear,
				textFieldInitialAmount, textFieldGrowth, comboBoxConvertFrom);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
		gridPaneLeft.setVgap(3.);
		gridPaneLeft.setStyle(styles.getStyle2());
		scrollPaneLeft.setContent(gridPaneLeft);
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
	 * Automatically sets the policy and market names based on current selections
	 * and options. If auto-naming is enabled, updates the text fields accordingly.
	 * Uses selected measure, pollutant, category, and region to generate unique
	 * names. Handles edge cases for multiple categories or regions.
	 */
	protected void setPolicyAndMarketNames() {
		Platform.runLater(() -> {
			if (checkBoxUseAutoNames.isSelected()) {
				String measure = "--";
				String pollutant = "--";
				String category = "--";
				String state = "--";
				try {
					String s = comboBoxMeasure.getValue();
					if (s != null && s.contains("Tax"))
						measure = "polTax";
					if (s != null && s.contains("Cap"))
						measure = "polCap";
					int cats = checkComboBoxCategory.getCheckModel().getCheckedItems().size();
					if (cats == 0) {
						category = "All";
					} else if (cats == 1) {
						category = checkComboBoxCategory.getCheckModel().getCheckedItems().get(0).replaceAll(" ", "_").replaceAll("-", "_");
					} else {
						category = "Mult";
					}
					s = comboBoxPollutant.getValue();
					if (s != null && !s.equals("Select One")) {
						pollutant = utils.splitString(s, " ")[0];
					}
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
	 * Saves the scenario component using the provided region tree. Validates
	 * inputs, generates metadata and CSV, and sets fileContent/filenameSuggestion.
	 *
	 * @param tree TreeView of selected regions
	 */
	private void saveScenarioComponent(TreeView<String> tree) {
		if (!qaInputs()) {
			Thread.currentThread().destroy();
			return;
		}

		String[] listOfSelectedRegions = utils.getAllSelectedRegions(tree);
		listOfSelectedRegions = utils.removeUSADuplicate(listOfSelectedRegions);

		String ID = utils.getUniqueString();
		String policy_name = textFieldPolicyName.getText() + ID;
		String market_name = textFieldMarketName.getText() + ID;
		filenameSuggestion = textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";

		//String category = null;
		List<String> cats = checkComboBoxCategory.getCheckModel().getCheckedItems();
//		if (cats.size() == 0) {
//			category = ALL;
//		} else if (cats.size() == 1) {
//			category = checkComboBoxCategory.getCheckModel().getCheckedItems().get(0);
//		}

		String measure = comboBoxMeasure.getValue();
		measure = measure.contains(CAP) ? CAP : TAX;

		String pol_menu = comboBoxPollutant.getSelectionModel().getSelectedItem().trim() + " ";
		String pol = pol_menu.substring(0, pol_menu.indexOf(" ")).trim();

		fileContent = getMetaDataContent(tree, market_name, policy_name);

		// Route to appropriate file generation method based on pollutant and measure

		// cases:
		// Caps:
		// 1. CO2 cap in MTC on all categories x
		// 2. CO2 cap in MTCO2 on all categories x
		// 3. GHG cap in MTCO2E on all categories x
		// 4. CO2 cap in MTC on specific categories
		// 5. CO2 cap in MTCO2 on specific categories
		// 6. Non-GHG cap in Tg on all categories
		// Taxes:
		// 7. CO2 tax in $/MTC on all categories
		// 8. CO2 tax in $/MTCO2 on all categories
		// 9. GHG tax in $/MTCO2E on all categories x
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
	 *
	 * @param listOfSelectedRegions Array of selected region names
	 * @param pol                   Pollutant string
	 * @param pol_menu              Pollutant menu string
	 * @param market_name           Market name
	 * @param policy_name           Policy name
	 */
	private void saveScenarioComponentRobustCO2Cap(String[] listOfSelectedRegions, String pol, String pol_menu,
			String market_name, String policy_name) {

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
		String dmdAdj = "1";
		if (pol_menu.contains("(MT CO2)"))
			dmdAdj = "3.667";

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

		initializeTempFiles();
		files.writeToBufferedFile(bw0, fileContent);
		fileContent = "use temp file";

		ArrayList<String> tempfiles = new ArrayList<String>();
		ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();

		// If not all categories are selected, append unique ID to pollutant name for uniqueness
		if (!categories.contains(ALL)) pol = pol + "_" + ID;

		files.writeToBufferedFile(bw0, INPUT_TABLE + vars.getEol());
		files.writeToBufferedFile(bw0, VARIABLE_ID + vars.getEol());
		if (measure.equals(CAP)) {
			files.writeToBufferedFile(bw0, GLIMPSE_EMISSION_CAP + vars.getEol() + vars.getEol());
			files.writeToBufferedFile(bw0, "region,pollutant,market,year,cap" + vars.getEol());
		} else if (measure.equals(TAX)) {
			files.writeToBufferedFile(bw0, GLIMPSE_EMISSION_TAX + vars.getEol() + vars.getEol());
			files.writeToBufferedFile(bw0, "region,pollutant,market,year,tax" + vars.getEol());
		}

		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
			String state = listOfSelectedRegions[0];

			for (String data_str : data) {
				data_str = data_str.replaceAll(" ", "").trim();
				int data_yr = Integer.parseInt(data_str.split(",")[0].trim());
				double data_val = Double.parseDouble(data_str.split(",")[1].trim());
				if (pol_menu.contains("(MT CO2)")) {
					data_val = data_val / 3.667;
					data_str = data_yr + "," + data_val;
				}
				files.writeToBufferedFile(bw0, state + "," + pol + "," + market_name + "," + data_str + vars.getEol());
			}
		}
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
				double progress = (double) s / (listOfSelectedRegions.length - 1);
				updateProgressBar(progress);
			}
		}

		int start_year = vars.getCalibrationYear();
		List<Integer> years = vars.getAllYears();
		int nonest_count = 0;
		int nest_count = 0;

		if (!categories.contains(ALL)) { // case where specific categories are selected (CO2 only)

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

									// DHL: added PV and wind check to avoid issue where these didn't have inputs,
									// resulting in severe error
									if ((!tech_r.contains("PV")) && (!tech_r.contains("wind"))
											&& (!tech_r.contains("CSP"))) {
										String cat_r_lwc = cat_r.toLowerCase();
										if ((cat_lwc.equals(cat_r_lwc))) {
											String line = region + "," + sector_r + "," + subsector_r + ","
													+ tech_r.replace("=>", ",") + "," + y + "," + pol + vars.getEol();
											if (tech_r.contains("=>")) {
												files.writeToBufferedFile(bw1, line);
												nest_count++;
											} else {
												files.writeToBufferedFile(bw2, line);
												nonest_count++;
											}
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

		files.closeBufferedFile(bw0);
		tempfiles.add(temp_file0);

		files.closeBufferedFile(bw1);
		files.closeBufferedFile(bw2);

		if (nest_count > 0)
			tempfiles.add(temp_file1);
		if (nonest_count > 0)
			tempfiles.add(temp_file2);

		files.concatDestSources(temp_file, tempfiles);

	}

	/**
	 * Special implementation for GHG tax/cap policies, generating scenario files
	 * for GHG policies.
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

		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 0) {
			String state = listOfSelectedRegions[0];
			ArrayList<String> data = paneForComponentDetails.getDataYrValsArrayList();
			for (String data_str : data) {
				data_str = data_str.replace(" ", "");
				fileContent += state + "," + policy_name + "," + market_name + "," + data_str + vars.getEol();
			}
		}
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
		String fileContent2 = "";
		fileContent2 += vars.getEol();
		fileContent2 += INPUT_TABLE + vars.getEol();
		fileContent2 += VARIABLE_ID + vars.getEol();
		fileContent2 += GLIMPSE_LINKED_GHG_MARKET_P1 + vars.getEol();
		fileContent2 += vars.getEol();
		fileContent2 += "region,pollutant,GHG-market,GHG-Policy,price-adjust,demand-adjust,price-unit,output-unit"
				+ vars.getEol();
		String[] GHGs;// = { "CO2","CH4","CH4_AWB","CH4_AGR","N2O","N2O_AWB","N2O_AGR","C2F6","CF4","SF6","HFC23","HFC32","HFC125","HFC134a","HFC143a","HFC152a","HFC227ea","HFC43","HFC236fa","HFC365mfc","HFC245fa" };
		String[] price_adjust;// = { "1.0","6.818","6.818","6.818","81.265","81.265","81.265","3.327","2.015","6.218","4.036","0.184","0.954","0.39","1.219","0.034","0.873","0.447","2.675","0.217","0.281" };
		String[] demand_adjust;// = { "3.667","25","25","25","298","298","298","12.2","7.39","22.8","14.8","0.675","3.5","1.43","4.47","0.124","3.2","1.64","9.81","0.794","1.03" };
		String[] price_unit;// = { "1990$/tC","1990$/GgCH4","1990$/GgCH4","1990$/GgCH4","1990$/GgN2O","1990$/GgN2O","1990$/GgN2O","1990$/MgC2F6","1990$/MgCF4","1990$/MgSF6","1990$/MgHFC23","1990$/MgHFC32","1990$/MgHFC125","1990$/MgHFC134a","1990$/MgHFC143a","1990$/MgHFC152a","1990$/MgHFC227ea","1990$/MgHFC43","1990$/MgHFC236fa","1990$/MgHFC365mfc","1990$/MgHFC245fa" };
		String[] output_unit;// = { "MtC","TgCH4","TgCH4","TgCH4","TgN2O","TgN2O","TgN2O","GgC2F6","GgCF4","GgSF6","GgHFC23","GgHFC32","GgHFC125","GgHFC134a","GgHFC143a","GgHFC152a","GgHFC227ea","GgHFC43","GgHFC236fa","GgHFC365mfc","GgHFC245faO" };

		if (pol.startsWith("F-")) {
			//F-gases only
			GHGs = new String[] { "C2F6","CF4","SF6","HFC23","HFC32","HFC125","HFC134a","HFC143a","HFC152a","HFC227ea","HFC43","HFC236fa","HFC365mfc","HFC245fa" };
			price_adjust = new String[] { "3.327","2.015","6.218","4.036","0.184","0.954","0.39","1.219","0.034","0.873","0.447","2.675","0.217","0.281" };
			demand_adjust = new String[] { "12.2","7.39","22.8","14.8","0.675","3.5","1.43","4.47","0.124","3.2","1.64","9.81","0.794","1.03" };
			price_unit = new String[] { "1990$/MgC2F6","1990$/MgCF4","1990$/MgSF6","1990$/MgHFC23","1990$/MgHFC32","1990$/MgHFC125","1990$/MgHFC134a","1990$/MgHFC143a","1990$/MgHFC152a","1990$/MgHFC227ea","1990$/MgHFC43","1990$/MgHFC236fa","1990$/MgHFC365mfc","1990$/MgHFC245fa" };
			output_unit = new String[] { "GgC2F6","GgCF4","GgSF6","GgHFC23","GgHFC32","GgHFC125","GgHFC134a","GgHFC143a","GgHFC152a","GgHFC227ea","GgHFC43","GgHFC236fa","GgHFC365mfc","GgHFC245faO" };
		} else { 
			//GHGs only
			GHGs = new String[] { "CO2","CH4","CH4_AWB","CH4_AGR","N2O","N2O_AWB","N2O_AGR" };
			price_adjust = new String[] { "1.0","6.818","6.818","6.818","81.265","81.265","81.265"};
			demand_adjust = new String[] { "3.667","25","25","25","298","298","298"};
			price_unit = new String[] { "1990$/tC","1990$/GgCH4","1990$/GgCH4","1990$/GgCH4","1990$/GgN2O","1990$/GgN2O","1990$/GgN2O"};
			output_unit = new String[] { "MtC","TgCH4","TgCH4","TgCH4","TgN2O","TgN2O","TgN2O"};
			//includes both GHGs and F-gases
//			GHGs = new String[] { "CO2","CH4","CH4_AWB","CH4_AGR","N2O","N2O_AWB","N2O_AGR","C2F6","CF4","SF6","HFC23","HFC32","HFC125","HFC134a","HFC143a","HFC152a","HFC227ea","HFC43","HFC236fa","HFC365mfc","HFC245fa" };
//			price_adjust = new String[] { "1.0","6.818","6.818","6.818","81.265","81.265","81.265","3.327","2.015","6.218","4.036","0.184","0.954","0.39","1.219","0.034","0.873","0.447","2.675","0.217","0.281" };
//			demand_adjust = new String[] { "3.667","25","25","25","298","298","298","12.2","7.39","22.8","14.8","0.675","3.5","1.43","4.47","0.124","3.2","1.64","9.81","0.794","1.03" };
//			price_unit = new String[] { "1990$/tC","1990$/GgCH4","1990$/GgCH4","1990$/GgCH4","1990$/GgN2O","1990$/GgN2O","1990$/GgN2O","1990$/MgC2F6","1990$/MgCF4","1990$/MgSF6","1990$/MgHFC23","1990$/MgHFC32","1990$/MgHFC125","1990$/MgHFC134a","1990$/MgHFC143a","1990$/MgHFC152a","1990$/MgHFC227ea","1990$/MgHFC43","1990$/MgHFC236fa","1990$/MgHFC365mfc","1990$/MgHFC245fa" };
//			output_unit = new String[] { "MtC","TgCH4","TgCH4","TgCH4","TgN2O","TgN2O","TgN2O","GgC2F6","GgCF4","GgSF6","GgHFC23","GgHFC32","GgHFC125","GgHFC134a","GgHFC143a","GgHFC152a","GgHFC227ea","GgHFC43","GgHFC236fa","GgHFC365mfc","GgHFC245faO" };

		}
		
		if (listOfSelectedRegions != null) {
			for (String state : listOfSelectedRegions) {
				for (int i = 0; i < GHGs.length; i++) {
					if ((pol.equals("GHG") 
							|| (pol.equals("F-gases")&&(GHGs[i].equals("C2F6")||GHGs[i].equals("CF4")||GHGs[i].equals("HFC125")||GHGs[i].equals("HFC134a")||GHGs[i].equals("HRC245fa")||GHGs[i].equals("SF6")))
							|| ((pol.equals("CO2")) && (GHGs[i].equals("CO2"))))) {
						fileContent2 += state + "," + GHGs[i] + "," + market_name + "," + policy_name + ","
								+ price_adjust[i] + "," + demand_adjust[i] + "," + price_unit[i] + "," + output_unit[i]
								+ vars.getEol();
					}
				}
			}
		}
		if (listOfSelectedRegions != null && listOfSelectedRegions.length > 1) {
			fileContent2 += vars.getEol();
			fileContent2 += INPUT_TABLE + vars.getEol();
			fileContent2 += VARIABLE_ID + vars.getEol();
			fileContent2 += GLIMPSE_LINKED_GHG_MARKET_P2 + vars.getEol();
			fileContent2 += vars.getEol();
			fileContent2 += "region,pollutant,GHG-market,GHG-Policy" + vars.getEol();
			for (int s = 1; s < listOfSelectedRegions.length; s++) {
				for (int i = 0; i < GHGs.length; i++) {
					if ((pol.equals("GHG")) || ((pol.equals("CO2")) && (GHGs[i].equals("CO2")))) {
						String state = listOfSelectedRegions[s];
						fileContent2 += state + "," + GHGs[i] + "," + market_name + "," + policy_name + vars.getEol();
					}
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
					comboBoxMeasure.fireEvent(new ActionEvent());
					break;
				case "pollutant":
					comboBoxPollutant.setValue(value);
					comboBoxPollutant.fireEvent(new ActionEvent());
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
					checkComboBoxCategory.fireEvent(new ActionEvent());
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
					paneForComponentDetails.data.add(new DataPoint(s[0], s[1]));
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
	 *
	 * @return true if all inputs are valid, false otherwise
	 */
	protected boolean qaInputs() {
		TreeView<String> tree = paneForCountryStateTree.getTree();
		int error_count = 0;
		StringBuilder message = new StringBuilder();
		try {

			if (utils.getAllSelectedRegions(tree).length < 1) {
				message.append("Must select at least one region from tree").append(vars.getEol());
				error_count++;
			}
			if (paneForComponentDetails == null || paneForComponentDetails.table.getItems().size() == 0) {
				message.append("Data table must have at least one entry").append(vars.getEol());
				error_count++;
			} else {
				boolean match = validateTableDataYears();
				if (!match) {
					message.append("Years specified in table must match allowable policy years (")
							.append(vars.getAllowablePolicyYears()).append(")").append(vars.getEol());
					error_count++;
				}
			}
			if (comboBoxMeasure.getSelectionModel().getSelectedItem().equals(SELECT_ONE)) {
				message.append("Action comboBox must have a selection").append(vars.getEol());
				error_count++;
			}
			if (checkComboBoxCategory.getCheckModel().getCheckedItems().size() == 0) {
				message.append("Sector comboBox must have a selection").append(vars.getEol());
				error_count++;
			}
			if (comboBoxPollutant.getSelectionModel().getSelectedItem().equals(SELECT_ONE)) {
				message.append("Parameter comboBox must have a selection").append(vars.getEol());
				error_count++;
			}
			if (textFieldPolicyName.getText().isEmpty()) {
				message.append("A market name must be provided").append(vars.getEol());
				error_count++;
			}
		} catch (Exception e1) {
			error_count++;
			message.append("Error in QA of entries").append(vars.getEol());
		}
		if (error_count > 0) {
			if (error_count == 1) {
				utils.warningMessage(message.toString());
			} else if (error_count > 1) {
				utils.displayString(message.toString(), "Errors while developing component");
			}
		}
		return error_count == 0;
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
