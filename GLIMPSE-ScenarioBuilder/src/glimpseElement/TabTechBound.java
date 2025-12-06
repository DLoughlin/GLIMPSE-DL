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
import javafx.scene.control.CheckBoxTreeItem.TreeModificationEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TabTechBound provides the user interface and logic for creating and editing
 * technology bound policies in the GLIMPSE Scenario Builder.
 *
 * <p>
 * <b>Main responsibilities:</b>
 * <ul>
 *   <li>Allow users to select sector/category, filter and select technologies, and specify constraint type</li>
 *   <li>Configure policy and market names (auto/manual) and treatment (per region or across regions)</li>
 *   <li>Specify and populate constraint values over time</li>
 *   <li>Validate, import, and export scenario component data as CSV</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Support for filtering and selecting multiple technologies</li>
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
 * TabTechBound tab = new TabTechBound("Tech Bound", stage);
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
 *   <li>Handles UI setup, event listeners, and scenario file generation for technology bound policies.</li>
 *   <li>Supports upper, lower, and fixed bounds, and flexible treatment across/within regions.</li>
 *   <li>Provides methods for loading, validating, and saving scenario component data.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Key Methods:</b>
 * <ul>
 *   <li>{@link #TabTechBound(String, Stage)} - Constructor, sets up UI and listeners.</li>
 *   <li>{@link #setupUIControls()} - Initializes UI controls and listeners.</li>
 *   <li>{@link #saveScenarioComponent()} - Main entry for saving scenario data.</li>
 *   <li>{@link #saveScenarioComponent(TreeView)} - Handles file generation for tech bound policies.</li>
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
public class TabTechBound extends PolicyTab implements Runnable {
	// === UI label and option constants ===
	private static final String LABEL_FILTER = "Filter:";
	private static final String LABEL_CATEGORY = "Category: ";
	private static final String LABEL_TECHS = "Tech(s): ";
	private static final String LABEL_CONSTRAINT = "Constraint: ";
	private static final String LABEL_APPLIED_TO = "Applied To: ";
	private static final String LABEL_TREATMENT = "Treatment: ";
	private static final String LABEL_UNITS = "Units: ";
	private static final String LABEL_FINAL_VAL = "Final Val: ";
	private static final String LABEL_POPULATE = "Populate:";
	private static final String SELECT_ONE = "Select One";
	private static final String SELECT_ONE_OR_MORE = "Select One or More";
	private static final String ALL = "All";
	private static final String CONSTRAINT_UPPER = "Upper Bound";
	private static final String CONSTRAINT_LOWER = "Lower Bound";
	private static final String CONSTRAINT_FIXED = "Fixed Bound";
	private static final String[] CONSTRAINT_OPTIONS = { SELECT_ONE, CONSTRAINT_UPPER, CONSTRAINT_LOWER,
			CONSTRAINT_FIXED };
	private static final String[] TREATMENT_OPTIONS = { SELECT_ONE, "Each Selected Region", "Across Selected Regions" };
	private static final String[] APPLIED_TO_OPTIONS = { SELECT_ONE, "All Stock", "Sales" };
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

	/**
	 * Constructs a new TabTechBound instance and initializes the UI components for
	 * the technology bound tab. Sets up event handlers and populates controls with
	 * available data.
	 *
	 * @param title  The title of the tab
	 * @param stageX The JavaFX stage (not used directly)
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
	 * Sets up UI controls with options and default values. Initializes checkboxes,
	 * disables manual name fields if auto-naming is enabled.
	 */
	private void setupUIControls() {
		checkBoxUseAutoNames.setSelected(true);
		textFieldPolicyName.setDisable(true);
		textFieldMarketName.setDisable(true);
	}

	/**
	 * Sets up the main UI components for the left, center, and right columns. Calls
	 * setup methods for each column.
	 */
	public void setupUIComponents() {
		setupLeftColumn();
		setupCenterColumn();
		setupRightColumn();
	}

	/**
	 * Sets preferred, min, and max widths for UI components for consistent layout.
	 * Iterates over ComboBoxes and TextFields to set their widths.
	 */
	private void setComponentWidths() {
		ComboBox<?>[] comboBoxes = { comboBoxCategory, comboBoxModificationType, comboBoxConstraint, comboBoxAppliedTo,
				comboBoxTreatment };
		for (ComboBox<?> cb : comboBoxes) {
			cb.setMaxWidth(MAX_WIDTH);
			cb.setMinWidth(MIN_WIDTH);
			cb.setPrefWidth(PREF_WIDTH);
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
	 * Sets up the left column UI controls and layout. Adds labels and controls to
	 * the gridPaneLeft.
	 */
	private void setupLeftColumn() {

		// Filter TextField initial text
		textFieldFilter.setPromptText("Filter techs");

		gridPaneLeft.getChildren().clear();
		gridPaneLeft.add(utils.createLabel("Specification:"), 0, 0, 2, 1);
		gridPaneLeft.addColumn(0, labelComboBoxCategory, labelFilter, labelCheckComboBoxTech, labelComboBoxConstraint,
				labelAppliedTo, labelTreatment, new Label(), labelUnits, new Label(), new Separator(),
				labelUseAutoNames, labelPolicyName, labelMarketName, new Label(), new Separator(),
				utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear, labelEndYear,
				labelInitialAmount, labelGrowth);
		gridPaneLeft.addColumn(1, comboBoxCategory, textFieldFilter, checkComboBoxTech, comboBoxConstraint,
				comboBoxAppliedTo, comboBoxTreatment, new Label(), labelUnits2, new Label(), new Separator(),
				checkBoxUseAutoNames, textFieldPolicyName, textFieldMarketName, new Label(), new Separator(),
				new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear, textFieldInitialAmount,
				textFieldGrowth);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
		gridPaneLeft.setVgap(3.);
		gridPaneLeft.setStyle(styles.getStyle2());
		scrollPaneLeft.setContent(gridPaneLeft);
	}

	/**
	 * Sets up the technology combo box with default values and disables it until a
	 * filter or category is selected.
	 */
	private void setupTechComboBox() {
		checkComboBoxTech.getItems().add(SELECT_ONE_OR_MORE);
		checkComboBoxTech.getCheckModel().checkAll();
		checkComboBoxTech.setDisable(true);
	}

	/**
	 * Sets up combo box options for treatment, constraint, and modification type.
	 * Populates combo boxes with available options and selects defaults.
	 */
	private void setupComboBoxOptions() {
		comboBoxTreatment.getItems().addAll(TREATMENT_OPTIONS);
		comboBoxTreatment.getSelectionModel().selectFirst();
		comboBoxConstraint.getItems().addAll(CONSTRAINT_OPTIONS);
		comboBoxModificationType.getItems().addAll(MODIFICATION_TYPE_OPTIONS);
		comboBoxCategory.getSelectionModel().selectFirst();
		comboBoxConstraint.getSelectionModel().selectFirst();
		comboBoxModificationType.getSelectionModel().selectFirst();
		comboBoxAppliedTo.getItems().addAll(APPLIED_TO_OPTIONS);
		comboBoxAppliedTo.getSelectionModel().selectFirst();
	}

	/**
	 * Called when the category ComboBox selection changes. Updates policy/market names.
	 */
	private void onCategorySelected() {
		setPolicyAndMarketNames();
	}

	/**
	 * Called when the constraint ComboBox selection changes. Updates policy/market names.
	 */
	private void onConstraintSelected() {
		setPolicyAndMarketNames();
	}

	/**
	 * Called when the treatment ComboBox selection changes. Updates policy/market names.
	 */
	private void onTreatmentSelected() {
		setPolicyAndMarketNames();
	}

	/**
	 * Called when the appliedTo ComboBox selection changes. Updates policy/market names.
	 */
	private void onAppliedToSelected() {
		setPolicyAndMarketNames();
	}

	/**
	 * Sets up event handlers for UI components, including listeners for filter,
	 * category, technology, and other controls.
	 * Handles dynamic UI changes and triggers auto-naming and validation as needed.
	 */
	protected void setupEventHandlers() {

		super.setupEventHandlers();

		setEventHandler(textFieldFilter, e -> {
			updateCheckComboBoxTech();
//			}
		});

		setEventHandler(checkComboBoxTech, e -> setPolicyAndMarketNames());

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
		setEventHandler(comboBoxCategory, e -> {
			String selectedItem = comboBoxCategory.getSelectionModel().getSelectedItem();
			if (selectedItem == null)
				return;
			if (selectedItem.equals(SELECT_ONE)) {
				checkComboBoxTech.getCheckModel().clearChecks();
				checkComboBoxTech.getItems().clear();
				checkComboBoxTech.getItems().add(SELECT_ONE_OR_MORE);
				checkComboBoxTech.getCheckModel().check(0);
				checkComboBoxTech.setDisable(true);
				labelUnits2.setText(UNITS_DEFAULT);
				textFieldFilter.setText("");
				textFieldFilter.setDisable(true);
			} else {
				updateCheckComboBoxTech();
				checkComboBoxTech.setDisable(false);
				textFieldFilter.setDisable(false);
			}
			onCategorySelected();
		 setUnitsLabel();
		});
		checkComboBoxTech.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
			while (c.next()) {
				setUnitsLabel();
			}
		});
		setEventHandler(comboBoxConstraint, e -> onConstraintSelected());
		setEventHandler(comboBoxTreatment, e -> onTreatmentSelected());
		setEventHandler(comboBoxAppliedTo, e -> onAppliedToSelected());
	}

	/**
	 * Populates the sector combo box based on the technology info and filter text.
	 * Handles filtering and ensures no duplicate sectors are added.
	 */
	private void setupComboBoxCategory() {
		comboBoxCategory.getItems().clear();
		comboBoxCategory.getItems().addAll("Select One", "All");
		comboBoxCategory.getSelectionModel().selectFirst();
		try {
			String[][] techInfo = vars.getTechInfo();
			if (techInfo == null)
				return;
			ArrayList<String> categoryList = new ArrayList<>();

			for (String[] tech : techInfo) {
				if (tech == null || tech.length == 0)
					continue;
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
				if (cat != null)
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
	 * Updates the technology check combo box based on the selected sector and
	 * filter text. Only technologies matching the filter and sector are shown.
	 * Called when the filter or category changes.
	 */
	private void updateCheckComboBoxTech() {
		// Platform.runLater(() -> {
		String cat = comboBoxCategory.getValue();
		if (cat == null)
			return;
		String[][] techInfo = vars.getTechInfo();
		if (techInfo == null)
			return;
		boolean isAllCat = cat.equals(ALL);
		try {
			if (!checkComboBoxTech.getItems().isEmpty()) {
				checkComboBoxTech.getCheckModel().clearChecks();
				checkComboBoxTech.getItems().clear();
			}
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
		// };);
	}

	/**
	 * Automatically sets the policy and market names based on the current
	 * selections and auto-naming rules.
	 * Uses selected constraint, category, treatment, and region to generate unique
	 * names. Handles edge cases for multiple regions.
	 */
	protected void setPolicyAndMarketNames() {
		// Platform.runLater(() -> {
		if (checkBoxUseAutoNames.isSelected()) {
			String policyType = "--";
			String technology = "Tech";
			String sector = "--";
			String state = "--";
			String treatment = "--";
			String appliedTo = "--";
			try {
				String s = comboBoxConstraint.getValue();
				if (s.contains("Upper"))
					policyType = "_Up";
				if (s.contains("Lower"))
					policyType = "_Lo";
				if (s.contains("Fixed"))
					policyType = "_Fx";
				s = comboBoxCategory.getValue();
				if (!s.equals(SELECT_ONE)) {
					s = s.replace(" ", "_");
					s = utils.capitalizeOnlyFirstLetterOfString(s);
					sector = s;
				}
				s = comboBoxTreatment.getValue();
				if (s.contains("Each"))
					treatment = "_Ea";
				if (s.contains("Across"))
					treatment = "_Acr";
				
				s = comboBoxAppliedTo.getValue();
				if (s.contains("Sales")) {
					appliedTo = "_sales";
				} else {
					appliedTo = "_stock";
				}
				
				String[] listOfSelectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
				if (listOfSelectedLeaves.length > 0) {
					listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
					String stateStr = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
					if (stateStr.length() < 9) {
						state = stateStr;
					} else {
						state = "Reg";
					}
				}

				String name = "tchBnd" + policyType + "_" + sector + treatment + appliedTo + " " + state;
				name = name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("___", "__").replaceAll("__", "_");
				textFieldMarketName.setText(name + "_Mkt");
				textFieldPolicyName.setText(name);
			} catch (Exception e) {
				System.out.println("Cannot auto-name market. Continuing.");
			}
		}
		// });
	}

	/**
	 * Runnable implementation: triggers saving the scenario component. Calls
	 * saveScenarioComponent().
	 */
	@Override
	public void run() {
		Platform.runLater(this::saveScenarioComponent);
	}

	/**
	 * Saves the scenario component using the current region tree.
	 * Main entry point for saving the scenario component. Calls the overloaded
	 * saveScenarioComponent(TreeView) method.
	 */
	@Override
	public void saveScenarioComponent() {
		saveScenarioComponent(paneForCountryStateTree.getTree());
	}

	/**
	 * Saves the scenario component using the provided region tree. Writes metadata
	 * and constraint tables to temporary files. Handles nested/non-nested techs and
	 * transportation load factors.
	 *
	 * @param tree The region selection tree
	 */
	private void saveScenarioComponent(TreeView<String> tree) {

		if (!qaInputs()) {
			Thread.currentThread().destroy();
			return;
		}

		String bound_type = comboBoxConstraint.getSelectionModel().getSelectedItem().trim().toLowerCase();

		String ID = utils.getUniqueString();
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
		boolean isMultiSubsector = false;
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
			} else if (subsector != prev_subsector) {
				prev_subsector = subsector;
				isMultiSubsector = true;
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
				String use_this_market_name = market_name;
				if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
					use_this_policy_name = state + "_" + policy_name;
					use_this_market_name = state + "_" + market_name;
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

//						
//						if (appliedTo.equals("all stock")) {
//							use_this_market_name += "-"+yr;
//						}
//						if (sector.toLowerCase().startsWith("trn")) {
//							String loadStr = utils.getTrnVehInfo("load", state, sector, subsector, tech, yr);
//							loadFactorList.add(state + "," + sector + "," + subsector + "," + yr + "," + loadStr);
//						}
//
//						int y = Integer.parseInt(yr);
//						if (((y > calib_year) && (y >= start_year) && (y <= last_year))) { // only apply bound to legit
//																							// years
//
//							if (is_nested) {
//								nestedBuffer.append(state).append(",").append(sector).append(",").append(subsector)
//										.append(",").append(tech).append(",").append(y).append(",")
//										.append(use_this_policy_name).append(vars.getEol());
//							} else {
//								nonNestedBuffer.append(state).append(",").append(sector).append(",").append(subsector)
//										.append(",").append(tech).append(",").append(y).append(",")
//										.append(use_this_policy_name).append(vars.getEol());
//							}
//						}

		if (loadFactorList.size() > 0)
			loadFactorList = utils.removeDuplicateStringsFromArrayList(loadFactorList);

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

					Double valf = Double.parseDouble(val);
					valf = valf / (1.0e9 * loadFactor); // convert from $/quads to $/EJ
					val = "" + valf;
				}

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
	 * Returns a string containing the metadata content for the scenario component.
	 * Includes category, technologies, constraint, applied to, treatment, policy/market names, regions, and table data.
	 *
	 * @param tree   The region selection tree
	 * @param market The market name
	 * @param policy The policy name
	 * @return Metadata content as a string
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
	 * Loads content into the tab from the provided list of strings. Populates
	 * category, technologies, constraint, treatment, policy/market names, regions,
	 * and table data.
	 *
	 * @param content List of content lines to load
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
					comboBoxCategory.fireEvent(new ActionEvent());
				}
				if (param.equals("technologies")) {
					checkComboBoxTech.getCheckModel().clearChecks();
					String[] set = utils.splitString(value, ";");
					// Enhanced for-loop for set
					for (String item : set) {
						if (item != null) {
							checkComboBoxTech.getCheckModel().check(item.trim());
							checkComboBoxTech.fireEvent(new ActionEvent());
						}
					}
				}
				if (param.equals("constraint")) {
					comboBoxConstraint.setValue(value);
					comboBoxConstraint.fireEvent(new ActionEvent());
				}
				if (param.equals("applied to")) {
					if (value.toLowerCase().contains("sales")) {
						value = "Sales";
					} else {
						value = "All Stock";
					}
					comboBoxAppliedTo.setValue(value);
					comboBoxAppliedTo.fireEvent(new ActionEvent());
				}
				if (param.equals("treatment")) {
					comboBoxTreatment.setValue(value);
					comboBoxTreatment.fireEvent(new ActionEvent());
				}
//				if (param.equals("policy name")) {
//					textFieldPolicyName.setText(value);
//					textFieldPolicyName.fireEvent(new ActionEvent());
//				}
//				if (param.equals("market name")) {
//					textFieldMarketName.setText(value);
//					textFieldMarketName.fireEvent(new ActionEvent());
//				}
				if (param.equals("regions")) {
					String[] regions = utils.splitString(value, ",");
					this.paneForCountryStateTree.selectNodes(regions);
				}
				if (param.equals("table data")) {
					String[] s = utils.splitString(value, ",");
					if (s.length >= 2) {
						this.paneForComponentDetails.data.add(new DataPoint(s[0], s[1]));
					}
				}
			}
		}
		this.setPolicyAndMarketNames();
		this.setUnitsLabel();
		this.paneForComponentDetails.updateTable();
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
	 * Validates that all required inputs for saving the scenario component are
	 * present and correct. Checks for at least one region, at least one table
	 * entry, and required selections.
	 *
	 * @return true if all required inputs are valid, false otherwise
	 */
	protected boolean qaInputs() {

		TreeView<String> tree = paneForCountryStateTree.getTree();

		int error_count = 0;
		String message = "";

		try {

			if (utils.getAllSelectedRegions(tree).length < 1) {
				message += "Must select at least one region from tree" + vars.getEol();
				error_count++;
			}
			if (paneForComponentDetails.table.getItems().size() == 0) {
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
			if (comboBoxCategory.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "Category comboBox must have a selection" + vars.getEol();
				error_count++;
			}
			if (checkComboBoxTech.getCheckModel().getCheckedItems().size() == 0) {
				message += "Tech checkComboBox must have a selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxConstraint.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "Constraint comboBox must have a selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxTreatment.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "Treatment comboBox must have a selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxAppliedTo.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "Applied To comboBox must have a selection" + vars.getEol();
				error_count++;
			}
			if (textFieldMarketName.getText().equals("")) {
				message += "A market name must be provided" + vars.getEol();
				error_count++;
			}
			if (textFieldPolicyName.getText().equals("")) {
				message += "A policy name must be provided" + vars.getEol();
				error_count++;
			}

		} catch (Exception e1) {
			error_count++;
			message += "Error in QA of entries" + vars.getEol();
		}
		if (error_count > 0) {
			if (error_count == 1) {
				utils.warningMessage(message);
			} else if (error_count > 1) {
				utils.displayString(message, "Parsing Errors");
			}
		}

		boolean is_correct;
		if (error_count == 0) {
			is_correct = true;
		} else {
			is_correct = false;
		}
		return is_correct;
	}

	/**
	 * Utility method to set an event handler for a control (Button, ComboBox,
	 * CheckBox, TextField).
	 *
	 * @param control The control to set the event handler for
	 * @param handler The event handler to assign
	 * @param <T>     The type of the control
	 */
	private <T extends javafx.scene.Node> void setEventHandler(T control, EventHandler<ActionEvent> handler) {
		if (control instanceof Button) {
			((Button) control).setOnAction(handler);
		} else if (control instanceof ComboBox) {
			((ComboBox<?>) control).setOnAction(handler);
		} else if (control instanceof CheckBox) {
			((CheckBox) control).setOnAction(handler);
		} else if (control instanceof TextField) {
			((TextField) control).setOnAction(handler);
		}
	}

	/**
	 * Updates the units label based on the selected technologies. If available,
	 * extracts units from the selected technology string.
	 */
	private void setUnitsLabel() {
		ObservableList<String> selectedTechs = checkComboBoxTech.getCheckModel().getCheckedItems();
		String units = UNITS_DEFAULT;
		if (selectedTechs != null && !selectedTechs.isEmpty()) {
			// Example: extract units from selected techs if available
			// This logic should be replaced with actual units extraction as needed
			String firstTech = selectedTechs.get(0);
			String[] parts = firstTech.split(":");
			if (parts.length >= 4) {
				units = parts[3].trim();
			}
		}
		labelUnits2.setText(units);
	}
	
	
}
