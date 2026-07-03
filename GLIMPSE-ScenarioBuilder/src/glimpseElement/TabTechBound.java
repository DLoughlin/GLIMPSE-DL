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

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.CheckComboBox;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * TabTechBound provides the UI and behavior for creating and editing
 * technology-bound policies in the GLIMPSE Scenario Builder.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Build controls for selecting sector/category and filtering/selecting technologies.</li>
 *   <li>Support constraint type selection (upper, lower, fixed bounds).</li>
 *   <li>Enable specification of how policies are applied across regions (each region vs. across regions).</li>
 *   <li>Support applied-to selection (all stock vs. sales).</li>
 *   <li>Validate inputs and serialize scenario components to GLIMPSE-format files.</li>
 *   <li>Handle transport-specific load factor conversions for constraint values.</li>
 * </ul>
 * </p>
 *
 * <p><b>UI Structure:</b>
 * <ul>
 *   <li>Left column: Category, filter, technology selection, constraint type, treatment, applied-to, and naming options.</li>
 *   <li>Center column: Region tree selection and populate controls.</li>
 *   <li>Right column: Year/value data entry table.</li>
 * </ul>
 * </p>
 *
 * <p><b>File Output:</b> Creates temporary files in GLIMPSE-Data/temp containing:
 * <ul>
 *   <li>Metadata (policy description and selections)</li>
 *   <li>Technology mapping tables (for nested and non-nested subsectors)</li>
 *   <li>Constraint value tables with region, policy, and market information</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b> Not thread-safe; use only on the JavaFX Application Thread.</p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Extends PolicyTab and implements Runnable for background thread execution.</li>
 *   <li>Supports both nested (with '=>' markers) and non-nested technology structures.</li>
 *   <li>Auto-generates policy and market names when auto-naming is enabled.</li>
 * </ul>
 * </p>
 */
public class TabTechBound extends PolicyTab implements Runnable {
	// === UI label and option constants ===
	private static final String LABEL_FILTER = "Filter:";
	private static final String LABEL_NAMES = "Names:";
	private static final String LABEL_CATEGORY = "Category: ";
	private static final String LABEL_TECHS = "Tech(s): ";
	private static final String LABEL_CONSTRAINT = "Constraint: ";
	private static final String LABEL_APPLIED_TO = "Applied To: ";
	private static final String LABEL_TREATMENT = "Treatment: ";
	private static final String LABEL_UNITS = "Units: ";
	private static final String LABEL_POPULATE = "Populate:";
	private static final String SELECT_ONE = "Select One";
	private static final String SELECT_ONE_OR_MORE = "Select One or More";
	private static final String ALL = "All";
	private static final String CONSTRAINT_UPPER = "Upper Bound";
	private static final String CONSTRAINT_LOWER = "Lower Bound";
	private static final String CONSTRAINT_FIXED = "Fixed Bound";
	private static final String[] CONSTRAINT_OPTIONS = { CONSTRAINT_LOWER, CONSTRAINT_UPPER, 
			CONSTRAINT_FIXED };
	private static final String[] TREATMENT_OPTIONS = { "Across Selected Regions","Each Selected Region" };
	private static final String[] APPLIED_TO_OPTIONS = { "All Stock", "Sales" };
	private static final String UNITS_DEFAULT = "";
	// === UI Components ===
	private final Label labelFilter = createLabel(LABEL_FILTER, LABEL_WIDTH);
	private final TextField textFieldFilter = createTextField();
	private final Label labelComboBoxCategory = createLabel(LABEL_CATEGORY, LABEL_WIDTH);
	private final ComboBox<String> comboBoxCategory = createComboBoxString();
	private final Label labelCheckComboBoxTech = createLabel(LABEL_TECHS, LABEL_WIDTH);
	private final CheckComboBox<String> checkComboBoxTech = utils.createCheckComboBox();
	private final Label labelComboBoxConstraint = createLabel(LABEL_CONSTRAINT, LABEL_WIDTH);
	private final ComboBox<String> comboBoxConstraint = createComboBoxString();
	private final Label labelTreatment = createLabel(LABEL_TREATMENT, LABEL_WIDTH);
	private final ComboBox<String> comboBoxTreatment = createComboBoxString();
	private final Label labelAppliedTo = createLabel(LABEL_APPLIED_TO, LABEL_WIDTH);
	private final ComboBox<String> comboBoxAppliedTo = createComboBoxString();
	private final Label labelUnits = createLabel(LABEL_UNITS, LABEL_WIDTH);

	// HBox for Auto and Unique checkboxes
    private final javafx.scene.layout.HBox hBoxAutoUnique = new javafx.scene.layout.HBox(8);

	/**
	 * Create a new TabTechBound instance and initialize the UI controls and event handlers.
	 * Populates category and constraint options, sets up the technology list, and prepares
	 * the tab for user interaction. Auto-naming is enabled by default.
	 *
	 * @param title  The tab title to display
	 * @param stageX The JavaFX stage reference (used by parent classes)
	 */
	public TabTechBound(String title, Stage stageX) {
		this.setText(title);
		this.setStyle(styles.getFontStyle());
		setupUIControls();
		setupUIComponents();
		setComponentWidths();
		setupUILayout();
		setupComboBoxCategory();
		setupTechComboBox();
		setupComboBoxOptions();
		setupEventHandlers();
		setPolicyAndMarketNames();
		setUnitsLabel();
	}

	/**
	 * Initialize basic UI control state: default checkbox selections (auto-naming and unique names enabled),
	 * policy/market name fields disabled (enabled only when auto-naming is off).
	 */
	private void setupUIControls() {
		checkBoxUseAutoNames.setSelected(true);
		checkBoxUseUniqueNames.setSelected(true);
		textFieldPolicyName.setDisable(true);
		textFieldMarketName.setDisable(true);
	}

	/**
	 * Build the primary UI columns for this tab.
	 */
	public void setupUIComponents() {
		setupLeftColumn();
		setupCenterColumn();
		setupRightColumn();
	}

	/**
	 * Ensure consistent sizing for comboboxes and text fields used in the layout.
	 */
	private void setComponentWidths() {
		ComboBox<?>[] comboBoxes = { comboBoxCategory, comboBoxModificationType, comboBoxConstraint, comboBoxAppliedTo,
				comboBoxTreatment };
		for (ComboBox<?> cb : comboBoxes) {
			if (cb != null) {
				cb.setMaxWidth(MAX_WIDTH);
				cb.setMinWidth(MIN_WIDTH);
				cb.setPrefWidth(PREF_WIDTH);
				cb.getStyleClass().add("combo-box");
			}
		}
		checkComboBoxTech.setMaxWidth(MAX_WIDTH);
		checkComboBoxTech.setMinWidth(MIN_WIDTH);
		checkComboBoxTech.setPrefWidth(PREF_WIDTH);
		TextField[] textFields = { textFieldStartYear, textFieldEndYear, textFieldInitialAmount, textFieldGrowth,
				textFieldPeriodLength, textFieldPolicyName, textFieldMarketName, textFieldFilter };
		for (TextField tf : textFields) {
			tf.setMaxWidth(MAX_WIDTH);
			tf.setMinWidth(MIN_WIDTH);
			tf.setPrefWidth(PREF_WIDTH);
		}
	}

	/**
	 * Configure the left column layout and add labeled controls for policy specification,
	 * including category, filter, technology selection, constraint type, treatment, applied-to options,
	 * naming controls, and populate/data entry fields. The controls are arranged in gridPaneLeft.
	 */
	private void setupLeftColumn() {

		// Set prompt text to help users filter the tech list
		textFieldFilter.setPromptText("Filter techs");

		gridPaneLeft.getChildren().clear();
		gridPaneLeft.add(utils.createLabel("Specification:"), 0, 0, 2, 1);
		hBoxAutoUnique.getChildren().clear();
		hBoxAutoUnique.getChildren().addAll(checkBoxUseAutoNames, checkBoxUseUniqueNames);
		gridPaneLeft.addColumn(0, labelComboBoxCategory, labelFilter, labelCheckComboBoxTech, labelComboBoxConstraint,
				labelAppliedTo, labelTreatment, new Label(), labelUnits, new Label(), new Separator(),
				utils.createLabel(LABEL_NAMES), labelPolicyName, labelMarketName, new Label(), new Separator(),
				utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear, labelEndYear,
				labelInitialAmount, labelGrowth);
		gridPaneLeft.addColumn(1, comboBoxCategory, textFieldFilter, checkComboBoxTech, comboBoxConstraint,
				comboBoxAppliedTo, comboBoxTreatment, new Label(), labelUnits2, new Label(), new Separator(),
				hBoxAutoUnique, textFieldPolicyName, textFieldMarketName, new Label(), new Separator(),
				new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear, textFieldInitialAmount,
				textFieldGrowth);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
		gridPaneLeft.setVgap(3.);
		// Scroll pane holds the left grid
		scrollPaneLeft.setContent(gridPaneLeft);
	}

	/**
	 * Initialize the technology check-combo with a default entry and disable it
	 * until a category or filter is provided by the user.
	 */
	private void setupTechComboBox() {
		configureCheckComboBoxSelectionTitle(checkComboBoxTech, SELECT_ONE_OR_MORE, "Selected");
		checkComboBoxTech.setDisable(true);
	}

	/**
	 * Populate combo boxes with their available options and select sensible
	 * defaults.
	 */
	private void setupComboBoxOptions() {
		resetComboBoxItems(comboBoxTreatment, java.util.Arrays.asList(TREATMENT_OPTIONS));
		setComboBoxPrompt(comboBoxTreatment, SELECT_ONE);
		resetComboBoxItems(comboBoxConstraint, java.util.Arrays.asList(CONSTRAINT_OPTIONS));
		setComboBoxPrompt(comboBoxConstraint, SELECT_ONE);
		setModificationTypeOptions(MODIFICATION_TYPE_OPTIONS);
		resetComboBoxItems(comboBoxAppliedTo, java.util.Arrays.asList(APPLIED_TO_OPTIONS));
		setComboBoxPrompt(comboBoxAppliedTo, SELECT_ONE);
	}

	private void onCategorySelected() {
		setPolicyAndMarketNames();
	}

	private void onConstraintSelected() {
		setPolicyAndMarketNames();
	}

	private void onTreatmentSelected() {
		setPolicyAndMarketNames();
	}

	private void onAppliedToSelected() {
		setPolicyAndMarketNames();
	}

	/**
	 * Attach listeners and event handlers for controls in this tab. Listeners update UI state,
	 * enable/disable controls appropriately, react to user actions (category/constraint selection),
	 * and propagate changes to dependent fields (policy/market names, units label).
	 * 
	 * All UI updates are scheduled on the JavaFX Application Thread when necessary.
	 */
	protected void setupEventHandlers() {

		super.setupEventHandlers();

		setOnAction(textFieldFilter, e -> {
			updateCheckComboBoxTech();
		});

		labelCheckComboBoxTech.setOnMouseClicked(e -> {
			// Allow double-click to select/deselect all when control is enabled
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
		setOnAction(comboBoxCategory, e -> {
			String selectedItem = comboBoxCategory.getSelectionModel().getSelectedItem();
			if (isSelectionMissing(comboBoxCategory)) {
				// Reset tech list and disable filtering controls
				resetCheckComboBoxItems(checkComboBoxTech, null);
				checkComboBoxTech.setDisable(true);
				labelUnits2.setText(UNITS_DEFAULT);
				textFieldFilter.setText("");
				textFieldFilter.setDisable(true);
			} else {
				// Populate techs for the selected category
				updateCheckComboBoxTech();
				checkComboBoxTech.setDisable(false);
				textFieldFilter.setDisable(false);
			}
			onCategorySelected();
		 setUnitsLabel();
		});
		checkComboBoxTech.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
			while (c.next()) {
				setPolicyAndMarketNames();
				setUnitsLabel();
			}
		});
		setOnAction(comboBoxConstraint, e -> onConstraintSelected());
		setOnAction(comboBoxTreatment, e -> onTreatmentSelected());
		setOnAction(comboBoxAppliedTo, e -> onAppliedToSelected());
	}

	/**
	 * Populate category combo box using tech information supplied by vars.
	 * Ensures categories are unique and non-empty before adding them.
	 */
	private void setupComboBoxCategory() {
		comboBoxCategory.getItems().clear();
		comboBoxCategory.getItems().addAll("All");
		setComboBoxPrompt(comboBoxCategory, SELECT_ONE);
		try {
			String[][] techInfo = vars.getTechInfo();
			if (techInfo == null)
				return;
			ArrayList<String> categoryList = new ArrayList<>();

			for (String[] tech : techInfo) {
				if (tech == null || tech.length == 0)
					continue;
				String text = (tech.length > 7 && tech[7] != null) ? tech[7].trim() : "";
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
				if (cat != null && !cat.isEmpty())
					comboBoxCategory.getItems().add(cat.trim());
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
	 * Update the technology list shown in the check-combobox based on the
	 * selected category and the filter text. Avoids duplicate entries.
	 */
	private void updateCheckComboBoxTech() {
		String cat = comboBoxCategory.getValue();
		if (cat == null)
			return;
		String[][] techInfo = vars.getTechInfo();
		if (techInfo == null)
			return;
		boolean isAllCat = cat.equals(ALL);
		try {
			// Clear previous items and checks to ensure clean state
			resetCheckComboBoxItems(checkComboBoxTech, null);
			if (cat != null) {
				String lastLine = "";
				String filterText = textFieldFilter.getText() != null ? textFieldFilter.getText().trim() : "";
				for (String[] techRow : techInfo) {
					if (techRow == null || techRow.length < 3)
						continue;
					String line = (techRow[0] != null ? techRow[0].trim() : "") + " : "
						+ (techRow[1] != null ? techRow[1] : "") + " : " + (techRow[2] != null ? techRow[2] : "");
					if (filterText.isEmpty() || line.contains(filterText)) {
						if (techRow.length >= 7 && techRow[6] != null)
							line += " : " + techRow[6];
						if (!line.equals(lastLine)) {
							lastLine = line;
							if (isAllCat || (techRow.length > 7 && techRow[7] != null && techRow[7].equals(cat))) {
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
	 * Auto-generate policy and market names when auto-naming is enabled. Uses
	 * current selections (constraint, category, treatment, applied-to and
	 * selected regions) to compose a readable, unique default name.
	 */
	protected void setPolicyAndMarketNames() {
		if (checkBoxUseAutoNames.isSelected()) {
			String policyType = "--";
			String sector = "--";
			String state = "--";
			String treatment = "--";
			String appliedTo = "--";
			try {
				String s = comboBoxConstraint.getValue();
				if (s != null) {
					if (s.contains("Upper"))
						policyType = "_Up";
					if (s.contains("Lower"))
						policyType = "_Lo";
					if (s.contains("Fixed"))
						policyType = "_Fx";
				}
				s = comboBoxCategory.getValue();
				if (!isSelectionMissing(comboBoxCategory)) {
					s = utils.capitalizeOnlyFirstLetterOfString(normalizeNamePart(s));
					sector = s;
				}
				s = comboBoxTreatment.getValue();
				if (s != null) {
					if (s.contains("Each"))
						treatment = "_Ea";
					if (s.contains("Across"))
						treatment = "_Acr";
				}
				s = comboBoxAppliedTo.getValue();
				if (s != null) {
					if (s.contains("Sales")) {
						appliedTo = "_sales";
					} else {
						appliedTo = "_stock";
					}
				}

				String[] listOfSelectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
				if (listOfSelectedLeaves.length > 0) {
					listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
					String stateStr = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
					if (stateStr.length() < 9) {
						state = normalizeNamePart(stateStr);
					} else {
						state = "Reg";
					}
				}

				String name = normalizeNamePart("tchBnd" + policyType + "_" + sector + treatment + appliedTo + "_" + state);
				textFieldMarketName.setText(name + "_Mkt");
				textFieldPolicyName.setText(name);
			} catch (Exception e) {
				System.out.println("Cannot auto-name market. Continuing.");
			}
		}
	}

	/**
	 * Runnable implementation: schedule a save on the JavaFX thread.
	 */
	@Override
	public void run() {
		Platform.runLater(this::saveScenarioComponent);
	}

	/**
	 * Public save entrypoint — use currently selected region tree from the UI.
	 */
	@Override
	public void saveScenarioComponent() {
		saveScenarioComponent(paneForCountryStateTree.getTree());
	}

	/**
	 * Core logic to validate inputs and write the scenario component to a set
	 * of temporary files. The method constructs the appropriate headers based
	 * on the constraint type, handles nested vs non-nested technologies, and
	 * applies transport load-factor conversions when needed.
	 *
	 * @param tree The region selection tree from the UI
	 */
	private void saveScenarioComponent(TreeView<String> tree) {

		System.out.println("isGCAMVersionPre8.5: " + vars.isGcamVersionPre8_5());
		
		if (!qaInputs()) {
			// Abort save if validation fails
			return;
		}

		String bound_type = comboBoxConstraint.getSelectionModel().getSelectedItem().trim().toLowerCase();

		String ID = resolveUniqueSuffix(checkBoxUseUniqueNames.isSelected(), this.textFieldMarketName.getText());
		String policy_name = this.textFieldPolicyName.getText() + ID;
		String market_name = this.textFieldMarketName.getText() + ID;
		filenameSuggestion = this.textFieldPolicyName.getText().replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";

		String tempDirName = vars.getGlimpseDir() + File.separator + "GLIMPSE-Data" + File.separator + "temp";
		File test = new File(tempDirName);
		if (!test.exists())
			test.mkdir();

		String tempFilename0 = "temp_policy_file0.txt";
		String tempFilename1 = "temp_policy_file1.txt";
		String tempFilename2 = "temp_policy_file2.txt";
		String tempFilename3 = "temp_policy_file3.txt";

		BufferedWriter bw0 = files.initializeBufferedFile(tempDirName, tempFilename0);
		BufferedWriter bw1 = files.initializeBufferedFile(tempDirName, tempFilename1);
		BufferedWriter bw2 = files.initializeBufferedFile(tempDirName, tempFilename2);
		BufferedWriter bw3 = files.initializeBufferedFile(tempDirName, tempFilename3);

		fileContent = "use temp file";
		String temp_file = tempDirName + File.separator + "temp_policy_file.txt";
		files.deleteFile(temp_file);

		int no_nested = 0;
		int no_non_nested = 0;

		files.writeToBufferedFile(bw0, getMetaDataContent(tree, market_name, policy_name));

		String treatment = comboBoxTreatment.getValue().toLowerCase();
		String appliedTo = comboBoxAppliedTo.getValue().toLowerCase();

		String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
		listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);

		String header_part1 = "GLIMPSEPFStdTechUpBoundP1";
		String header_part2 = "GLIMPSEPFStdTechUpBoundP2";

		if (bound_type.equals("fixed bound")) {
			header_part2 = "GLIMPSEPFStdTechFxBoundP2";
		} else if (bound_type.equals("lower bound")) {
			header_part1 = "GLIMPSEPFStdTechLoBoundP1";
			header_part2 = "GLIMPSEPFStdTechLoBoundP2";
		} else if (bound_type.equals("upper bound")) {
			header_part1 = "GLIMPSEPFStdTechUpBoundP1";
			header_part2 = "GLIMPSEPFStdTechUpBoundP2";
		}

		ObservableList<String> tech_list = checkComboBoxTech.getCheckModel().getCheckedItems();

		files.writeToBufferedFile(bw1, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw1, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw1, header_part1 + "-Nest" + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw1,
				"region,sector,nested-subsector,subsector,tech,year,policy-name" + vars.getEol());

		files.writeToBufferedFile(bw2, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw2, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw2, header_part1 + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw2, "region,sector,subsector,tech,year,policy-name" + vars.getEol());

		ArrayList<String> dataArrayList = this.paneForComponentDetails.getDataYrValsArrayList();
		String[] year_list = new String[dataArrayList.size()];
		String[] value_list = new String[dataArrayList.size()];
		double[] valuef_list = new double[dataArrayList.size()];

		for (int i = 0; i < dataArrayList.size(); i++) {
			String str = dataArrayList.get(i).replaceAll(" ", "").trim();
			year_list[i] = utils.splitString(str, ",")[0];
			value_list[i] = utils.splitString(str, ",")[1];
			valuef_list[i] = Double.parseDouble(value_list[i]);
		}

		int start_year = Integer.parseInt(year_list[0]);
		int calib_year = vars.getCalibrationYear();
		int last_year = Integer.parseInt(year_list[year_list.length - 1]);

		StringBuilder nestedBuffer = new StringBuilder();
		StringBuilder nonNestedBuffer = new StringBuilder();

		boolean isTransportation = false;
		String prev_subsector = "";
		double loadFactor = 1.0;
		String sector = "";
		String subsector = "";
		String tech = "";

		ArrayList<String> loadFactorList = new ArrayList<>();

		for (String techItem : tech_list) {
			String[] temp = utils.splitString(techItem.trim(), ":");

			sector = temp[0].trim();
			subsector = temp[1].trim();
			tech = temp[2].trim();

			if (prev_subsector.equals("")) {
				prev_subsector = subsector;
			} else if (!subsector.equals(prev_subsector)) {
				prev_subsector = subsector;
			}

			if (sector.toLowerCase().startsWith("trn")) {
				isTransportation = true;
			}

			boolean is_nested = tech.contains("=>");
			if (is_nested) {
				no_nested++;
				tech = tech.replaceAll("=>", ",");
			} else {
				no_non_nested++;
			}

			for (String state : listOfSelectedLeaves) {

				String use_this_policy_name = policy_name;
				if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
					use_this_policy_name = state + "_" + policy_name;
				}

				state = state.trim();

				for (String yr : year_list) {

					int y = Integer.parseInt(yr);

					if ((y >= start_year) && (y <= last_year)) {

						if (sector.toLowerCase().startsWith("trn")) {
							String loadStr = utils.getTrnVehInfo("load", state, sector, subsector, tech, yr);
							loadFactorList.add(state + "," + sector + "," + subsector + "," + yr + "," + loadStr);
						}

						String use_this_policy_name1 = use_this_policy_name + "-" + yr;

						for (int y1 : vars.getAllYears()) {

							if (y >= calib_year) {
								if ((y1 == y) || ((appliedTo.equals("all stock")) && (y1 <= y) && (y1 >= calib_year))) {

									if (is_nested) {
										nestedBuffer.append(state).append(",").append(sector).append(",")
										.append(subsector).append(",").append(tech).append(",").append(y1)
										.append(",").append(use_this_policy_name1).append(vars.getEol());
									} else {
										nonNestedBuffer.append(state).append(",").append(sector).append(",")
										.append(subsector).append(",").append(tech).append(",").append(y1)
										.append(",").append(use_this_policy_name1).append(vars.getEol());
									}

								}
							}
						}
					}
				}
			}
		}

		if (loadFactorList.size() > 0)
			loadFactorList = utils.getUniqueItemsFromStringArrayList(loadFactorList);

		files.writeToBufferedFile(bw1, nestedBuffer.toString());
		files.writeToBufferedFile(bw2, nonNestedBuffer.toString());

		files.writeToBufferedFile(bw3, vars.getEol() + "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw3, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw3, header_part2 + vars.getEol() + vars.getEol());

		if (bound_type.equals("fixed bound"))

		{
			files.writeToBufferedFile(bw3,
						"region,policy-name,market,type,constraint-yr,constraint-val,min-price-yr,min-price-val"
							+ vars.getEol());
		} else {
			files.writeToBufferedFile(bw3,
						"region,policy-name,market,type,constraint-yr,constraint-val" + vars.getEol());
		}

		StringBuilder constraintBuffer = new StringBuilder();


		
		for (String state : listOfSelectedLeaves) {
			String use_this_market_name = market_name;
			String use_this_policy_name = policy_name;
			if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
				use_this_market_name = state + "_" + market_name;
				use_this_policy_name = state + "_" + policy_name;
			}

			for (String data : dataArrayList) {
				String data_str = data.replace(" ", "");
				String year = utils.splitString(data_str, ",")[0];
				String val = utils.splitString(data_str, ",")[1];

				String textStr = state + "," + sector + "," + subsector + "," + year;
				loadFactor = 1.0;

				String use_this_policy_name1 = use_this_policy_name + "-" + year;
				;
				Double valf = Double.parseDouble(val);
				
				if (isTransportation) {
					if (loadFactorList.size() > 0) {
						for (String lfStr : loadFactorList) {
							if (lfStr.startsWith(textStr)) {
								String[] temp = utils.splitString(lfStr, ",");
								try {
									loadFactor = Double.parseDouble(temp[4]);
								} catch (Exception e) {
									loadFactor = 1.0;
								}
								break;
							}
						}
					}

					if (vars.isGcamVersionPre8_5()) {
						valf = valf / (1.0e9 * loadFactor / 1.055);
					} else {
						valf = valf / loadFactor;
					}
				}

				val = formatDisplayValue(valf);

				if (bound_type.equals("fixed bound")) {
					constraintBuffer.append(state).append(",").append(use_this_policy_name1).append(",")
						.append(use_this_market_name).append(",tax,").append(year).append(",").append(val)
						.append(",").append(year).append(",-100").append(vars.getEol());
				} else if (bound_type.equals("upper bound")) {
					constraintBuffer.append(state).append(",").append(use_this_policy_name1).append(",")
						.append(use_this_market_name).append(",tax,").append(year).append(",").append(val)
						.append(vars.getEol());
				} else if (bound_type.equals("lower bound")) {
					constraintBuffer.append(state).append(",").append(use_this_policy_name1).append(",")
						.append(use_this_market_name).append(",subsidy,").append(year).append(",").append(val)
						.append(vars.getEol());
				}
			}
		}

		files.writeToBufferedFile(bw3, constraintBuffer.toString());

		files.closeBufferedFile(bw0);
		files.closeBufferedFile(bw1);
		files.closeBufferedFile(bw2);
		files.closeBufferedFile(bw3);

		String temp_file0 = tempDirName + File.separator + tempFilename0;
		String temp_file1 = tempDirName + File.separator + tempFilename1;
		String temp_file2 = tempDirName + File.separator + tempFilename2;
		String temp_file3 = tempDirName + File.separator + tempFilename3;

		ArrayList<String> tempfiles = new ArrayList<>();
		tempfiles.add(temp_file0);

		if (no_nested > 0) {
			tempfiles.add(temp_file1);
		}
		if (no_non_nested > 0) {
			tempfiles.add(temp_file2);
		}
		tempfiles.add(temp_file3);

		files.concatDestSources(temp_file, tempfiles);

		System.out.println("Done");
	}

	/**
	 * Build a metadata header string describing the selected inputs. This is
	 * written to the temporary metadata file for the scenario component.
	 *
	 * @param tree   Region selection tree
	 * @param market Market name used in the generated files
	 * @param policy Policy name used in the generated files
	 * @return A formatted metadata string
	 */
	public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
		StringBuilder rtnStr = new StringBuilder();
		rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
		rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
		rtnStr.append("#Category: ").append(this.comboBoxCategory.getSelectionModel().getSelectedItem())
			.append(vars.getEol());
		ObservableList<String> techList = checkComboBoxTech.getCheckModel().getCheckedItems();
		String techs = utils.getStringFromList(techList, ";");
		rtnStr.append("#Technologies: ").append(techs).append(vars.getEol());
		rtnStr.append("#Constraint: ").append(comboBoxConstraint.getSelectionModel().getSelectedItem())
			.append(vars.getEol());
		rtnStr.append("#Applied to: ").append(comboBoxAppliedTo.getSelectionModel().getSelectedItem())
			.append(vars.getEol());
		rtnStr.append("#Treatment: ").append(comboBoxTreatment.getSelectionModel().getSelectedItem())
			.append(vars.getEol());
		if (policy == null)
			market = textFieldPolicyName.getText();
		rtnStr.append("#Policy name: ").append(policy).append(vars.getEol());
		if (market == null)
			market = textFieldMarketName.getText();
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
	 * Load a previously saved component (metadata lines) into the UI controls.
	 * Lines are expected to start with a '#' followed by a key:value pair.
	 *
	 * @param content Lines read from a metadata file
	 */
	@Override
	public void loadContent(ArrayList<String> content) {
		for (String line : content) {
			int pos = line.indexOf(":");
			if (line.startsWith("#") && (pos > -1)) {
				String param = line.substring(1, pos).trim().toLowerCase();
				String value = line.substring(pos + 1).trim();

				if (param.equals("category")) {
					comboBoxCategory.setValue(value);
				}
				if (param.equals("technologies")) {
					checkComboBoxTech.getCheckModel().clearChecks();
					String[] set = utils.splitString(value, ";");
					for (String item : set) {
						if (item != null) {
							checkComboBoxTech.getCheckModel().check(item.trim());
						}
					}
				}
				if (param.equals("constraint")) {
					comboBoxConstraint.setValue(value);
				}
				if (param.equals("applied to")) {
					if (value.toLowerCase().contains("sales")) {
						value = "Sales";
					} else {
						value = "All Stock";
					}
					comboBoxAppliedTo.setValue(value);
				}
				if (param.equals("treatment")) {
					comboBoxTreatment.setValue(value);
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
		this.setPolicyAndMarketNames();
		this.setUnitsLabel();
		this.paneForComponentDetails.updateTable();
	}

	/**
	 * Check whether the table contains at least one data point whose year is
	 * among the allowable policy years.
	 *
	 * @return true if a valid year is present; false otherwise
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
	 * Validate that required inputs are present and sensible before attempting
	 * to save the scenario component. Displays warnings when errors are found.
	 *
	 * @return true if inputs pass QA; false otherwise
	 */
	protected boolean qaInputs() {

		TreeView<String> tree = paneForCountryStateTree.getTree();

		int error_count = 0;
		StringBuilder message = new StringBuilder();

		try {
			error_count += validateRegionSelection(tree, message);
			boolean hasRows = paneForComponentDetails != null && !paneForComponentDetails.table.getItems().isEmpty();
			boolean yearsMatch = !hasRows || validateTableDataYears();
			error_count += validateTableEntries(message, hasRows, yearsMatch);
			error_count += validateRequiredSelection(message, comboBoxCategory, "Category");
			error_count += validateRequiredSelection(message, checkComboBoxTech, "Tech");
			error_count += validateRequiredSelection(message, comboBoxConstraint, "Constraint");
			error_count += validateRequiredSelection(message, comboBoxTreatment, "Treatment");
			error_count += validateRequiredSelection(message, comboBoxAppliedTo, "Applied To");
			error_count += validateRequiredText(message, textFieldMarketName, "market name");
			error_count += validateRequiredText(message, textFieldPolicyName, "policy name");

		} catch (Exception e1) {
			error_count++;
			message.append("Error in QA of entries").append(vars.getEol());
		}
		return finalizeQaValidation(error_count, message);
	}

	/**
	 * Update the units label using the first selected technology's units field,
	 * if present in the tech string parts.
	 */
	private void setUnitsLabel() {
		ObservableList<String> selectedTechs = checkComboBoxTech.getCheckModel().getCheckedItems();
		String units = UNITS_DEFAULT;
		if (selectedTechs != null && !selectedTechs.isEmpty()) {
			String firstTech = selectedTechs.get(0);
			String[] parts = firstTech.split(":");
			if (parts.length >= 4 && parts[3] != null) {
				units = parts[3].trim();
			}
		}
		labelUnits2.setText(units);
	}
	
	
}
