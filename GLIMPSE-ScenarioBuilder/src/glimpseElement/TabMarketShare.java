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
import gui.PaneNewScenarioComponent;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.control.CheckBoxTreeItem.TreeModificationEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TabMarketShare provides the user interface and logic for creating and editing
 * market share policies in the GLIMPSE Scenario Builder.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Allows selection and filtering of technology subsets and supersets for market share policies.</li>
 *   <li>Supports configuration of policy type, application, constraint, and treatment options.</li>
 *   <li>Handles population and validation of policy data tables.</li>
 *   <li>Generates scenario component metadata and writes policy files for GLIMPSE.</li>
 *   <li>Supports loading and saving of scenario component configurations.</li>
 * </ul>
 * <p>
 * <b>UI Elements:</b>
 * <ul>
 *   <li>ComboBoxes for policy type, application, constraint, treatment, and modification type.</li>
 *   <li>CheckComboBoxes for subset and superset technology selection.</li>
 *   <li>TextFields for filtering, policy/market names, and year/value entry.</li>
 *   <li>Buttons for populating, clearing, and deleting table entries.</li>
 *   <li>TreeView for region selection.</li>
 * </ul>
 * <p>
 * <b>Workflow:</b>
 * <ol>
 *   <li>User selects policy type and filters technologies.</li>
 *   <li>User selects subset and superset technologies.</li>
 *   <li>User configures application, constraint, and treatment.</li>
 *   <li>User enters year/value data and populates the table.</li>
 *   <li>User saves the scenario component, generating files and metadata.</li>
 * </ol>
 * <p>
 * <b>Dependencies:</b>
 * <ul>
 *   <li>JavaFX for UI components and threading.</li>
 *   <li>ControlsFX for CheckComboBox controls.</li>
 *   <li>GLIMPSE utility classes for file and data handling.</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> Not thread-safe; use only on JavaFX Application Thread.
 */
public class TabMarketShare extends PolicyTab implements Runnable {
	// === Constants for UI text and options ===
	private static final double MIN_WIDTH = 175;
	private static final String SELECT_ONE = "Select One";
	private static final String SELECT_ONE_OR_MORE = "Select One or More";
	private static final String[] POLICY_TYPE_OPTIONS = { SELECT_ONE, "Renewable Portfolio Standard (RPS)",
			"Clean Energy Standard (CES)", "EV passenger cars and trucks (LDV-EV)", "EV passenger cars trucks and MCs (LDV-EV)",
			"EV freight light truck (HDV-Lt)", "EV freight medium truck (HDV-Med)", "EV freight heavy truck (HDV-Hvy)", 
			"EV freight all trucks (HDV-EV)","LED lights (LED)", "Heat pumps (HP)", "Biofuels (BioF)", "Other (OTH)", 
			"Sector:EGU (EGU)", "Sector:Industry (IND)", "Sector:Industry-fuels (Fuels)","Sector:Buildings (BLD)", 
			"Sector:Trn-Onroad (Onroad)", "Sector:Trn-ALM (ALM)", "Sector:Other (OTH)" };
	private static final String[] APPLIED_TO_OPTIONS = { SELECT_ONE, "All Stock", "Sales" };
	private static final String[] CONSTRAINT_OPTIONS = { SELECT_ONE, "Lower", "Fixed" };
	private static final String[] TREATMENT_OPTIONS = { SELECT_ONE, "Each Selected Region", "Across Selected Regions" };
	private static final String[] MODIFICATION_TYPE_OPTIONS = { "Initial and Final %", "Initial w/% Growth/yr",
			"Initial w/% Growth/pd", "Initial w/Delta/yr", "Initial w/Delta/pd" };

	// === Labels and Controls ===
	private final Label labelSubsetFilter = createLabel("Subset Filter:", LABEL_WIDTH);
	private final TextField textFieldSubsetFilter = createTextField();
	private final Label labelSupersetFilter = createLabel("Superset Filter:", LABEL_WIDTH);
	private final TextField textFieldSupersetFilter = createTextField();
	private final Label labelPolicyType = createLabel("Type?", LABEL_WIDTH);
	private final ComboBox<String> comboBoxPolicyType = createComboBoxString();
	private final Label labelSubset = createLabel("Subset: ", LABEL_WIDTH);
	private final CheckComboBox<String> checkComboBoxSubset = createCheckComboBox();
	private final Label labelSuperset = createLabel("Superset: ", LABEL_WIDTH);
	private final CheckComboBox<String> checkComboBoxSuperset = createCheckComboBox();
	private final Label labelAppliedTo = createLabel("Applied to: ", LABEL_WIDTH);
	private final ComboBox<String> comboBoxAppliedTo = createComboBoxString();
	private final Label labelConstraint = createLabel("Constraint: ", LABEL_WIDTH);
	private final ComboBox<String> comboBoxConstraint = createComboBoxString();
	private final Label labelTreatment = createLabel("Treatment: ", LABEL_WIDTH);
	private final ComboBox<String> comboBoxTreatment = createComboBoxString();

	// === Constants for Metadata ===
	private static final String METADATA_HEADER = "########## Scenario Component Metadata ##########";
	private static final String METADATA_FOOTER = "#################################################";
	private static final String METADATA_SCENARIO_TYPE = "#Scenario component type: Market Share";
	private static final String METADATA_TYPE = "#Type: ";
	private static final String METADATA_SUBSET = "#Subset: ";
	private static final String METADATA_SUPERSET = "#Superset: ";
	private static final String METADATA_APPLIED_TO = "#Applied to: ";
	private static final String METADATA_TREATMENT = "#Treatment: ";
	private static final String METADATA_CONSTRAINT = "#Constraint: ";
	private static final String METADATA_POLICY_NAME = "#Policy name: ";
	private static final String METADATA_MARKET_NAME = "#Market name: ";
	private static final String METADATA_REGIONS = "#Regions: ";
	private static final String METADATA_TABLE_DATA = "#Table data:";

	/**
	 * Constructs a TabMarketShare instance for the given scenario builder tab.
	 * @param title  The tab title.
	 * @param stageX The JavaFX stage.
	 * @param pane   The parent pane.
	 */
	public TabMarketShare(String title, Stage stageX, PaneNewScenarioComponent pane) {
		this.setText(title);
		this.setStyle(styles.getFontStyle());
    	super.setupEventHandlers();
		setupUIControls();
		setupUIComponents();
		setComponentWidths();
		setupUILayout();
		setPolicyAndMarketNames();
		customize();
	}

	/**
	 * Sets up all UI controls, listeners, and default values for the market share policy tab.
	 * Includes ComboBox and CheckComboBox options, widget actions, and event handlers.
	 */
	private void setupUIControls() {
		// Set up initial state for auto-naming and text fields
		checkBoxUseAutoNames.setSelected(true);
		textFieldPolicyName.setDisable(true);
		textFieldMarketName.setDisable(true);
		// Populate ComboBox options
		comboBoxPolicyType.getItems().addAll(POLICY_TYPE_OPTIONS);
		comboBoxPolicyType.getSelectionModel().select(0);
		checkComboBoxSubset.getItems().addAll(SELECT_ONE_OR_MORE);
		checkComboBoxSuperset.getItems().addAll(SELECT_ONE_OR_MORE);
		checkComboBoxSubset.getCheckModel().check(0);
		checkComboBoxSuperset.getCheckModel().check(0);
		checkComboBoxSubset.setPrefWidth(70);
		checkComboBoxSuperset.setPrefWidth(70);
		checkComboBoxSubset.setMaxWidth(70);
		checkComboBoxSuperset.setMaxWidth(70);
		comboBoxAppliedTo.getItems().addAll(APPLIED_TO_OPTIONS);
		comboBoxAppliedTo.getSelectionModel().selectFirst();
		comboBoxConstraint.getItems().addAll(CONSTRAINT_OPTIONS);
		comboBoxConstraint.getSelectionModel().selectFirst();
		comboBoxTreatment.getItems().addAll(TREATMENT_OPTIONS);
		comboBoxTreatment.getSelectionModel().selectFirst();
		comboBoxModificationType.getItems().addAll(MODIFICATION_TYPE_OPTIONS);
		comboBoxModificationType.getSelectionModel().selectFirst();
		// Widget actions and event handlers
		setupWidgetActions();

		// Update policy and market names when region tree changes
		paneForCountryStateTree.getTree().addEventHandler(ActionEvent.ACTION, e -> {
			setPolicyAndMarketNames();
		});
		

	}

	/**
	 * sets default selections for modification type, treatment, and applied to.
	 * Also sets prompt text for subset and superset filter text fields.
	 */
	public void customize() {

		// Set prompt text for subset and superset filter text fields
		textFieldSubsetFilter.setPromptText("Filter techs in numerator");
		textFieldSupersetFilter.setPromptText("Filter techs in denominator");
		
		// Set default selections for modification type, treatment, and applied to
		this.comboBoxModificationType.getSelectionModel().select("Initial and Final %");
		this.comboBoxModificationType.fireEvent(new ActionEvent());
		this.comboBoxTreatment.getSelectionModel().select("Select One");
		this.comboBoxAppliedTo.getSelectionModel().select("Select One");		
	}

	/**
	 * sets up the UI components for the tab, including left, center, and right columns.
	 */
    public void setupUIComponents() {
        setupLeftColumn();
        setupCenterColumn();
        setupRightColumn();   	
    }	
	
	/**
	 * Sets the min and max widths for all ComboBoxes and CheckComboBoxes in the tab.
	 * Ensures consistent sizing for UI elements.
	 */
	private void setComponentWidths() {
		Object[] comboBoxes = { comboBoxPolicyType, checkComboBoxSubset, checkComboBoxSuperset, comboBoxAppliedTo,
				comboBoxModificationType, comboBoxConstraint, comboBoxTreatment };
		for (Object cb : comboBoxes) {
			if (cb instanceof ComboBox) {
				((ComboBox<?>) cb).setMaxWidth(MAX_WIDTH);
				((ComboBox<?>) cb).setMinWidth(MIN_WIDTH);
			} else if (cb instanceof CheckComboBox) {
				((CheckComboBox<?>) cb).setMaxWidth(MAX_WIDTH);
				((CheckComboBox<?>) cb).setMinWidth(MIN_WIDTH);
			}
		}
	}


	/**
	 * Sets the min and max widths for all ComboBoxes.
	 * (Legacy method, not used in current workflow.)
	 */
	private void setComboBoxWidths() {
		Object[] comboBoxes = { comboBoxPolicyType, checkComboBoxSubset, checkComboBoxSuperset, comboBoxAppliedTo,
				comboBoxModificationType, comboBoxConstraint, comboBoxTreatment };
		for (Object cb : comboBoxes) {
			if (cb instanceof ComboBox) {
				((ComboBox<?>) cb).setMaxWidth(MAX_WIDTH);
				((ComboBox<?>) cb).setMinWidth(MIN_WIDTH);
			} else if (cb instanceof CheckComboBox) {
				((CheckComboBox<?>) cb).setMaxWidth(MAX_WIDTH);
				((CheckComboBox<?>) cb).setMinWidth(MIN_WIDTH);
			}
		}
	}

	/**
	 * Sets up the left column of the UI with labels and input controls for policy
	 * specification and population.
	 * Arranges all relevant controls for user input.
	 */
	private void setupLeftColumn() {
		gridPaneLeft.add(utils.createLabel("Specification:"), 0, 0, 2, 1);
		gridPaneLeft.addColumn(0, labelPolicyType, labelSubsetFilter, labelSubset, labelSupersetFilter, labelSuperset,
				labelConstraint, labelAppliedTo, labelTreatment, new Separator(), labelUseAutoNames, labelPolicyName,
				labelMarketName, new Separator(), utils.createLabel("Populate:"), labelModificationType, labelStartYear,
				labelEndYear, labelInitialAmount, labelGrowth);
		gridPaneLeft.addColumn(1, comboBoxPolicyType, textFieldSubsetFilter, checkComboBoxSubset,
				textFieldSupersetFilter, checkComboBoxSuperset, comboBoxConstraint, comboBoxAppliedTo,
				comboBoxTreatment, new Separator(), checkBoxUseAutoNames, textFieldPolicyName, textFieldMarketName,
				new Separator(), new Label(), comboBoxModificationType, textFieldStartYear, textFieldEndYear,
				textFieldInitialAmount, textFieldGrowth);
		gridPaneLeft.setAlignment(Pos.TOP_LEFT);
		gridPaneLeft.setVgap(3.);
		gridPaneLeft.setStyle(styles.getStyle2());
		scrollPaneLeft.setContent(gridPaneLeft);
	}

	/**
	 * Sets up widget actions and event handlers for UI controls.
	 * Handles double-click, selection, and action events for all major controls.
	 */
	private void setupWidgetActions() {
		setOnMouseClicked(labelSubset, e -> {
			if (!checkComboBoxSubset.isDisabled()) {
				boolean isFirstItemChecked = checkComboBoxSubset.getCheckModel().isChecked(0);
				if (e.getClickCount() == 2) {
					if (isFirstItemChecked) {
						checkComboBoxSubset.getCheckModel().clearChecks();
					} else {
						checkComboBoxSubset.getCheckModel().checkAll();
					}
				}
			}
		});
		setOnMouseClicked(labelSuperset, e -> {
			if (!checkComboBoxSuperset.isDisabled()) {
				boolean isFirstItemChecked = checkComboBoxSuperset.getCheckModel().isChecked(0);
				if (e.getClickCount() == 2) {
					if (isFirstItemChecked) {
						checkComboBoxSuperset.getCheckModel().clearChecks();
					} else {
						checkComboBoxSuperset.getCheckModel().checkAll();
					}
				}
			}
		});
		setOnAction(comboBoxPolicyType, e -> {
			String selectedItem = comboBoxPolicyType.getValue();
			if (selectedItem.equals(SELECT_ONE)) {
				checkComboBoxSubset.getCheckModel().clearChecks();
				checkComboBoxSubset.getItems().clear();
				checkComboBoxSuperset.getCheckModel().clearChecks();
				checkComboBoxSuperset.getItems().clear();
				checkComboBoxSubset.getItems().add(SELECT_ONE_OR_MORE);
				checkComboBoxSuperset.getItems().add(SELECT_ONE_OR_MORE);
				checkComboBoxSubset.getCheckModel().check(0);
				checkComboBoxSuperset.getCheckModel().check(0);
				checkComboBoxSubset.setDisable(true);
				checkComboBoxSuperset.setDisable(true);
			} else {
				setupCheckComboBoxes(selectedItem);
				checkComboBoxSubset.setDisable(false);
				checkComboBoxSuperset.setDisable(false);
				if ((selectedItem.contains("RPS")) || (selectedItem.contains("CES"))) {
					//showInfo("For RPS and CES options:\nTechnology selections chosen automatically. Modify as needed.",
					//		"Information");
					utils.warningMessage("For RPS and CES options:\nTechnology selections chosen automatically. Modify as needed");	
					this.comboBoxAppliedTo.getSelectionModel().select("All Stock");
					this.comboBoxTreatment.getSelectionModel().select("Across Selected Regions");
					this.comboBoxModificationType.getSelectionModel().select("Initial and Final %");
					this.comboBoxModificationType.fireEvent(new ActionEvent());
				} else  {
					this.comboBoxAppliedTo.getSelectionModel().select("Sales");
					this.comboBoxTreatment.getSelectionModel().select("Select One");
				}
			}
			setPolicyAndMarketNames();
		});
		setOnAction(textFieldSubsetFilter, e -> setupCheckComboBoxes());
		setOnAction(textFieldSupersetFilter, e -> setupCheckComboBoxes());
		setOnAction(comboBoxAppliedTo, e -> setPolicyAndMarketNames());
		setOnAction(comboBoxTreatment, e -> setPolicyAndMarketNames());
		setOnAction(comboBoxConstraint, e -> setPolicyAndMarketNames());

	}

	/**
	 * Sets up the subset and superset check combo boxes based on the selected
	 * policy type and filters. Calls overloaded method with selected item.
	 */
	private void setupCheckComboBoxes() {
		String item = comboBoxPolicyType.getSelectionModel().getSelectedItem();
		setupCheckComboBoxes(item);
	}

	/**
	 * Sets up the subset and superset check combo boxes for a given policy type.
	 * Filters and populates the available technology options for subset and superset.
	 * @param selectedItem The selected policy type.
	 */
	private void setupCheckComboBoxes(String selectedItem) {
		String[][] techInfo = vars.getTechInfo();
		if (techInfo == null)
			return;
		try {
			List<String> techListSub = new ArrayList<>();
			List<String> techListSup = new ArrayList<>();
			String filterTextSub = textFieldSubsetFilter.getText() != null ? textFieldSubsetFilter.getText().trim()
					: "";
			String filterTextSup = textFieldSupersetFilter.getText() != null ? textFieldSupersetFilter.getText().trim()
					: "";
			boolean useFilterSub = !filterTextSub.isEmpty();
			boolean useFilterSup = !filterTextSup.isEmpty();
			String lastLine = "";
			for (String[] tech : techInfo) {
				String line = tech[0].trim() + " : " + tech[1] + " : " + tech[2];
				if (line.equals(lastLine))
					continue;
				lastLine = line;
				if (tech.length >= 7) {
					line += " : " + tech[6] + " : " + tech[7];
				}
				boolean showSub = !useFilterSub;
				if (useFilterSub) {
					for (String temp : tech) {
						if (temp.contains(filterTextSub)) {
							showSub = true;
							break;
						}
					}
				}
				if (showSub)
					techListSub.add(line.trim());
				boolean showSup = !useFilterSup;
				if (useFilterSup) {
					for (String temp : tech) {
						if (temp.contains(filterTextSup)) {
							showSup = true;
							break;
						}
					}
				}
				if (showSup)
					techListSup.add(line.trim());
			}
			checkComboBoxSubset.getCheckModel().clearChecks();
			checkComboBoxSubset.getItems().clear();
			checkComboBoxSuperset.getCheckModel().clearChecks();
			checkComboBoxSuperset.getItems().clear();
			boolean showEgu = false;
			boolean showLdvCar = false;
			boolean showLdvTruck = false;
			boolean showLdv4w = false;
			boolean showLdvAll = false;
			boolean showHdvAll = false;
			boolean showHdvLight = false;
			boolean showHdvMedium = false;
			boolean showHdvHeavy = false;
			boolean showLighting = false;
			boolean showHeating = false;
			boolean showRefining = false;
			boolean showSectorEgu = false;
			boolean showSectorBuildings = false;
			boolean showSectorIndustry = false;
			boolean showSectorIndustryFuels = false;
			boolean showSectorTrnOnroad = false;
			boolean showSectorTrnAlm = false;
			boolean showSectorOther = false;
			String policyType = comboBoxPolicyType.getValue();
			if (policyType.contains("CES")) {
				showEgu = true;
			}
			if (policyType.contains("RPS")) {
				showEgu = true;
			}
			if (policyType.contains("EV passenger cars and trucks"))
				showLdv4w = true;
			if (policyType.contains("EV passenger cars trucks and MCs"))
				showLdvAll = true;
			if (policyType.contains("EV freight light truck"))
				showHdvLight = true;
			if (policyType.contains("EV freight medium truck"))
				showHdvMedium = true;
			if (policyType.contains("EV freight heavy truck"))
				showHdvHeavy = true;
			if (policyType.contains("EV freight all trucks"))
				showHdvAll = true;
			if (policyType.contains("LED lights"))
				showLighting = true;
			if (policyType.contains("Heat pumps"))
				showHeating = true;
			if (policyType.contains("Biofuels"))
				showRefining = true;
			if (policyType.contains("Sector:EGU"))
				showSectorEgu = true;
			if (policyType.contains("Sector:Buildings"))
				showSectorBuildings = true;
			if (policyType.contains("Sector:Industry"))
				showSectorIndustry = true;
			if (policyType.contains("Sector:Industry-fuels"))
				showSectorIndustryFuels = true;
			if (policyType.contains("Sector:Trn-Onroad"))
				showSectorTrnOnroad = true;
			if (policyType.contains("Sector:Trn-ALM"))
				showSectorTrnAlm = true;
			if (policyType.contains("Sector:Other"))
				showSectorTrnAlm = true;
			for (String techLine : techListSub) {
				boolean showTech = false;
				String techLineLc = techLine.toLowerCase();
				if (showEgu) {
					if (techLineLc.startsWith("electricity")) {
						showTech = true;
					} else if (techLineLc.startsWith("base load")) {
						showTech = true;
					} else if (techLineLc.startsWith("intermediate")) {
						showTech = true;
					} else if (techLineLc.startsWith("peak")) {
						showTech = true;
					} else if (techLineLc.startsWith("subpeak")) {
						showTech = true;
					} else if (techLineLc.startsWith("elec_")) {
						showTech = true;
					} else if (techLineLc.indexOf("cogen") > -1) {
						showTech = true;
					}
				} else if (showLdvTruck) {
					if (techLineLc.contains("large car and truck")) {
						showTech = true;
					}
				} else if (showLdvCar) {
					if (techLineLc.contains(": car :")) {
						showTech = true;
					}
				} else if (showLdv4w) {
					if (techLineLc.indexOf("ldv_4w") > -1) {
						showTech = true;
					}
				} else if (showLdvAll) {
					if (techLineLc.indexOf("ldv") > -1) {
						showTech = true;
					}
				} else if (showHdvLight) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("light"))
							showTech = true;
					}
				} else if (showHdvMedium) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("medium"))
							showTech = true;
					}
				} else if (showHdvHeavy) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("heavy"))
							showTech = true;
					}
				} else if (showHdvAll) {
					if (techLineLc.startsWith("trn_freight_road")) {
						showTech = true;
					}
				} else if (showLighting) {
					if ((techLineLc.startsWith("resid lighting")) || (techLineLc.startsWith("comm lighting"))) {
						showTech = true;
					}
				} else if (showHeating) {
					if ((techLineLc.startsWith("resid heating")) || (techLineLc.startsWith("comm heating"))) {
						showTech = true;
					}
				} else if (showRefining) {
					if ((techLineLc.startsWith("oil refining")) || (techLineLc.startsWith("biomass liquids"))) {
						showTech = true;
					}
				} else if (showSectorEgu) {
					if (techLineLc.endsWith("egu"))
						showTech = true;

				} else if (showSectorIndustry) {
					if (techLineLc.endsWith("industry"))
						showTech = true;

				} else if (showSectorIndustryFuels) {
					if (techLineLc.endsWith("industry-fuels"))
						showTech = true;

				} else if (showSectorBuildings) {
					if (techLineLc.endsWith("buildings"))
						showTech = true;

				} else if (showSectorTrnOnroad) {
					if (techLineLc.endsWith("trn-onroad"))
						showTech = true;

				} else if (showSectorTrnAlm) {
					if ((techLineLc.endsWith("trn-alm")) || (techLineLc.endsWith("trn-nonroad")))
						showTech = true;

				} else if (showSectorOther) {
					showTech = true;

				} else {
					showTech = true;
				}
				if (showTech) {
					checkComboBoxSubset.getItems().add(techLine);
				}
			}
			for (String techLine : techListSup) {
				boolean showTech = false;
				String techLineLc = techLine.toLowerCase();
				if (showEgu) {
					if (techLineLc.startsWith("electricity ")) {
						showTech = true;
					} else if (techLineLc.startsWith("base load")) {
						showTech = true;
					} else if (techLineLc.startsWith("intermediate")) {
						showTech = true;
					} else if (techLineLc.startsWith("peak")) {
						showTech = true;
					} else if (techLineLc.startsWith("subpeak")) {
						showTech = true;
					} else if (techLineLc.startsWith("elec_")) {
						showTech = true;
					} else if (techLineLc.indexOf("cogen") > -1) {
						showTech = true;
					}
				} else if (showLdvTruck) {
					if (techLineLc.contains("large car and truck")) {
						showTech = true;
					}
				} else if (showLdvCar) {
					if (techLineLc.contains(": car :")) {
						showTech = true;
					}
				} else if (showLdv4w) {
					if (techLineLc.indexOf("ldv_4w") > -1) {
						showTech = true;
					}
				} else if (showLdvAll) {
					if (techLineLc.indexOf("ldv") > -1) {
						showTech = true;
					}
				} else if (showHdvLight) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("light"))
							showTech = true;
					}
				} else if (showHdvMedium) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("medium"))
							showTech = true;
					}
				} else if (showHdvHeavy) {
					if (techLineLc.startsWith("trn_freight_road")) {
						if (techLineLc.contains("heavy"))
							showTech = true;
					}
				} else if (showHdvAll) {
					if (techLineLc.startsWith("trn_freight_road")) {
						showTech = true;
					}
				} else if (showLighting) {
					if ((techLineLc.startsWith("resid lighting")) || (techLineLc.startsWith("comm lighting"))) {
						showTech = true;
					}
				} else if (showHeating) {
					if ((techLineLc.startsWith("resid heating")) || (techLineLc.startsWith("comm heating"))) {
						showTech = true;
					}
				} else if (showRefining) {
					if ((techLineLc.startsWith("oil refining")) || (techLineLc.startsWith("biomass liquids"))) {
						showTech = true;
					}
				} else if (showSectorEgu) {
					if (techLineLc.endsWith("egu"))
						showTech = true;

				} else if (showSectorIndustry) {
					if (techLineLc.endsWith("industry"))
						showTech = true;

				} else if (showSectorIndustryFuels) {
					if (techLineLc.endsWith("industry-fuels"))
						showTech = true;

				} else if (showSectorBuildings) {
					if (techLineLc.endsWith("buildings"))
						showTech = true;

				} else if (showSectorTrnOnroad) {
					if (techLineLc.endsWith("trn-onroad"))
						showTech = true;

				} else if (showSectorTrnAlm) {
					if ((techLineLc.endsWith("trn-alm")) || (techLineLc.endsWith("trn-nonroad")))
						showTech = true;

				} else if (showSectorOther) {
					showTech = true;

				} else {
					showTech = true;
				}
				if (showTech) {
					checkComboBoxSuperset.getItems().add(techLine);
				}
			}
			if ((policyType.contains("RPS")) || (policyType.contains("CES"))) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if ((itemText.indexOf("solar") >= 0) || (itemText.indexOf("csp") >= 0)
							|| (itemText.indexOf("pv") >= 0))
						checkComboBoxSubset.getCheckModel().check(i);
					if (itemText.indexOf("wind") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
					if ((itemText.indexOf("hydro") >= 0) && (itemText.indexOf("hydrogen") < 0))
						checkComboBoxSubset.getCheckModel().check(i);
					if (itemText.indexOf("geothermal") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
					if (itemText.indexOf("biomass") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
			if (policyType.contains("CES")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("ccs") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
					if (itemText.indexOf("nuclear") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
			}
			if (policyType.contains("EV")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("bev") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
			if (policyType.startsWith("EV")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("bev") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
			if (policyType.contains("LED")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("solid state") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
			if (policyType.contains("Heat pump")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("heat pump") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
			if (policyType.contains("Biofuel")) {
				for (int i = 0; i < checkComboBoxSubset.getItems().size(); i++) {
					String itemText = checkComboBoxSubset.getItems().get(i).toLowerCase();
					if (itemText.indexOf("bio") >= 0)
						checkComboBoxSubset.getCheckModel().check(i);
				}
				checkComboBoxSuperset.getCheckModel().checkAll();
			}
		} catch (Exception e) {
			utils.warningMessage("Problem reading tech list.");
			System.out.println("Error reading tech list from " + vars.getTchBndListFilename() + ":");
			System.out.println("  ---> " + e);
		}
	}

	/**
	 * Sets the policy and market names automatically based on current selections if
	 * auto-naming is enabled. Uses selected policy type, application, treatment, and regions.
	 */
	protected void setPolicyAndMarketNames() {
		Platform.runLater(() -> {
			if (checkBoxUseAutoNames.isSelected()) {
				String policyType = comboBoxPolicyType.getValue() != null ? comboBoxPolicyType.getValue()
						.replace(" ", "-").replace(":", "") : "--";
				if (policyType.contains("Other"))
					policyType = "Share";
				if (policyType.contains("Select"))
					policyType = "---";
				if ((policyType.contains("("))&&((policyType.contains(")")))) {
					policyType = utils.getTextBetweenParen(policyType);
				}
				
				String toWhich = "--";
				String state = "--";
				String treatment = "--";
				String constraint = "--";
				
				try {
					String s = comboBoxAppliedTo.getValue();
					if (s != null && s.contains("Sales"))
						toWhich = "_sales";
					if (s != null && s.contains("All"))
						toWhich = "_stock";
					
					s = comboBoxTreatment.getValue();
					if (s != null && s.contains("Each"))
						treatment = "_Ea";
					if (s != null && s.contains("Acr"))
						treatment = "_Acr";
					
					s = comboBoxConstraint.getValue();
					if (s != null && s.contains("Lo")) {
						constraint = "_Lo";
					}
					if (s != null && s.contains("Hi")) {
						constraint = "_Hi";
					}
					if (s != null && s.contains("Fix")) {
						constraint = "_Fx";
					}
					
					String[] listOfSelectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
					listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
					String stateStr = utils.returnAppendedString(listOfSelectedLeaves).replace(",", "");
					if (stateStr.length() < 9) {
						state = "_"+stateStr;
					} else {
						state = "_Reg";
					}
					String name = "mktShr_" + policyType + constraint + treatment + toWhich + state ;
					name = name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("___", "__").replaceAll("__", "_");
					textFieldMarketName.setText(name + "_Mkt");
					textFieldPolicyName.setText(name);
				} catch (Exception e) {
					// Ignore auto-naming errors
				}
			}
		});
	}

	/**
	 * Runs background tasks or updates for this tab. Implementation of Runnable
	 * interface. Triggers saveScenarioComponent on the JavaFX Application Thread.
	 */
	@Override
	public void run() {
		Platform.runLater(this::saveScenarioComponent);
	}

	/**
	 * Saves the scenario component using the selected regions from the
	 * country/state tree. Calls overloaded saveScenarioComponent(TreeView).
	 */
	@Override
	public void saveScenarioComponent() {
		saveScenarioComponent(paneForCountryStateTree.getTree());
	}

	/**
	 * Saves the scenario component using the provided tree of selected regions.
	 * Writes all necessary files and metadata for the market share policy.
	 * @param tree The TreeView containing selected regions.
	 */
	private void saveScenarioComponent(TreeView<String> tree) {
		// 1. Early exit on failed QA
		if (!qaInputs()) {
			Thread.currentThread().destroy();
			return;
		}

		// 2. Gather all required values up front
		String which = this.comboBoxConstraint.getValue().toLowerCase();
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
		
		BufferedWriter bw0 = files.initializeBufferedFile(tempDirName, tempFilename0);
		BufferedWriter bw1 = files.initializeBufferedFile(tempDirName, tempFilename1);
		BufferedWriter bw2 = files.initializeBufferedFile(tempDirName, tempFilename2);

		fileContent = "use temp file";
		files.writeToBufferedFile(bw0, getMetaDataContent(tree, market_name, policy_name));
		
		int no_nested = 0;
		int no_non_nested = 0;

		String treatment = comboBoxTreatment.getValue().toLowerCase().trim();
		String[] listOfSelectedLeaves = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
		String states = utils.returnAppendedString(listOfSelectedLeaves);
		
		ObservableList<String> subset_list = checkComboBoxSubset.getCheckModel().getCheckedItems();
		ObservableList<String> superset_list = checkComboBoxSuperset.getCheckModel().getCheckedItems();
		
		String applied_to = comboBoxAppliedTo.getSelectionModel().getSelectedItem().toLowerCase().trim();
		
		ArrayList<String> dataArrayList = this.paneForComponentDetails.getDataYrValsArrayList();
		
		//String[] year_list = new String[dataArrayList.size()];
		String[] year_list = new String[dataArrayList.size()];
		String[] value_list = new String[dataArrayList.size()];
		double[] valuef_list = new double[dataArrayList.size()];
		
		for (int i = 0; i < dataArrayList.size(); i++) {
			String str = dataArrayList.get(i).replaceAll(" ", "").trim();
			year_list[i] = utils.splitString(str, ",")[0];
			value_list[i] = utils.splitString(str, ",")[1];
			valuef_list[i] = Double.parseDouble(value_list[i]);
		}
		
		int start_year = vars.getCalibrationYear();
		
		List<Integer> years=vars.getAllYears();
		
		ArrayList<String> list_of_policy_sector_combos = new ArrayList<>();

		// 3. Write part 1: constraint fraction targets
		files.writeToBufferedFile(bw1, vars.getEol());
		files.writeToBufferedFile(bw1, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw1, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw1, "GLIMPSEPFStdAdjCoef-v2" + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw1,
				"region,sector,subsector,tech,year,policy,adjcoef-year,adjcoef,unit-price-conv" + vars.getEol());
		files.writeToBufferedFile(bw2, vars.getEol());
		files.writeToBufferedFile(bw2, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw2, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw2, "GLIMPSEPFStdAdjCoef-Nest" + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw2,
				"region,sector,nested-subsector,tech,year,policy,adjcoef-year,adjcoef" + vars.getEol());

		for (int s = 0; s < listOfSelectedLeaves.length; s++) {
			String state = listOfSelectedLeaves[s];
			System.out.println("Creating part 1 of 3 of csv file for " + state + " : " + s + " of "
					+ (listOfSelectedLeaves.length - 1));

			for (int t : years) {
				String use_this_market_name = market_name;
				String use_this_policy_name = policy_name;

				if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
					use_this_market_name = state + "_" + market_name;
					use_this_policy_name = state + "_" + policy_name;
				}
				if (applied_to.equals("sales")) {
					use_this_market_name += "-" + t;
					use_this_policy_name += "-" + t;
				}

				String last_subsector = "";
				boolean is_subsector_in_region = true;
				
				for (int i = 0; i < year_list.length; i++) {
					if (((t <= Integer.parseInt(year_list[i])) && (applied_to.equals("all stock")))
							|| ((t == Integer.parseInt(year_list[i])) && (applied_to.equals("sales")))) {
						for (int j = 0; j < superset_list.size(); j++) {
							String temp = superset_list.get(j);
							if (temp.indexOf(":") >= 0) {
								String[] tempi = utils.splitString(temp, ":");
								String sector_name = tempi[0].trim();
								String subsector_name = tempi[1].trim();
								String tech_name = tempi[2].trim();
								if (sector_name.startsWith("trn")) {
									if (last_subsector.equals(subsector_name)) {
										is_subsector_in_region = true;
									} else {
										is_subsector_in_region = utils.isSubsectorInRegion(state, sector_name,
												subsector_name);
									}
								}
								last_subsector = subsector_name;
								String ss = use_this_policy_name + ":" + sector_name;
								list_of_policy_sector_combos = utils
										.addToArrayListIfUnique(list_of_policy_sector_combos, ss);
								BufferedWriter bw = bw1;
								if (tech_name.indexOf("=>") > -1) {
									bw = bw2;
									tech_name = tech_name.replace("=>", ",");
									no_nested++;
								} else {
									no_non_nested++;
								}
								if ((vars.isGcamUSA()) && (state.toLowerCase().equals("usa"))
										&& (listOfSelectedLeaves.length > 1)) {
									// skip
								} else {
									if (is_subsector_in_region) {
										Double val = valuef_list[i];
										String conv = "1.0";
										if (sector_name.startsWith("trn")) {
											conv = "1e-3";
											val *= 1000.;
										}
										String line = state + "," + sector_name + "," + subsector_name + "," + tech_name
												+ "," + t + "," + use_this_policy_name + "," + year_list[i] + "," + val
												+ "," + conv + vars.getEol();
										files.writeToBufferedFile(bw, line);
									}
								}
							}
						}
					}
				}
			}
			updateProgressBar((double) s / (listOfSelectedLeaves.length - 1));
		}

		int max_year = utils.getMaxValFromStringArray(year_list);
		int min_year = utils.getMinValFromStringArray(year_list);

		// 4. Write part 2: secondary output ratio and p-multiplier
		files.writeToBufferedFile(bw1, vars.getEol());
		files.writeToBufferedFile(bw1, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw1, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw1, "GLIMPSEPFStd2ndOut" + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw1,
				"region,sector,subsector,tech,year,policy,output-ratio,pMultiplier" + vars.getEol());
		files.writeToBufferedFile(bw2, vars.getEol());
		files.writeToBufferedFile(bw2, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw2, "Variable ID" + vars.getEol());
		files.writeToBufferedFile(bw2, "GLIMPSEPFStd2ndOut-Nest" + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw2,
				"region,sector,subsector,nested-subsector,tech,year,policy,output-ratio,pMultiplier" + vars.getEol());

		for (int s = 0; s < listOfSelectedLeaves.length; s++) {
			String state = listOfSelectedLeaves[s];
			System.out.println("Creating part 2 of 3 of csv file for " + state + " : " + s + " of "
					+ (listOfSelectedLeaves.length - 1));
			int year_int = -1;
			
			for (int t : years) {
				year_int++;
				
				String use_this_market_name = market_name;
				String use_this_policy_name = policy_name;
				
				if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
					use_this_market_name = state + "_" + market_name;
					use_this_policy_name = state + "_" + policy_name;
				}
				if (applied_to.equals("sales")) {
					use_this_market_name += "-" + t;
					use_this_policy_name += "-" + t;
				}

				if (((t <= max_year) && (applied_to.equals("all stock")) && ( t >= start_year))
						|| ((t <= max_year) && (t >= min_year) && (applied_to.equals("sales")))) {
					
					for (int j = 0; j < subset_list.size(); j++) {
						
						String temp = subset_list.get(j);
						if (temp.indexOf(":") >= 0) {
							String sector_name = utils.splitString(temp, ":")[0].trim();
							String subsector_name = utils.splitString(temp, ":")[1].trim();
							String tech_name = utils.splitString(temp, ":")[2].trim();
							BufferedWriter bw = bw1;
							
							if (tech_name.indexOf("=>") > -1) {
								bw = bw2;
								tech_name = tech_name.replace("=>", ",");
								no_nested++;
							} else {
								no_non_nested++;
							}
							
							double val = 1.0;
							
							String conversions = utils.getSubsectorConversions(val, state, sector_name, subsector_name,
									t);
							
							if ((vars.isGcamUSA()) && (state.toLowerCase().equals("usa"))
									&& (listOfSelectedLeaves.length > 1)) {
								// skip
							} else if (conversions != null) {
								if (conversions.startsWith(","))
									conversions = conversions.substring(1);
								String line = state + "," + sector_name + "," + subsector_name + "," + tech_name + ","
										+ t + "," + use_this_policy_name + "," + conversions + vars.getEol();
								files.writeToBufferedFile(bw, line);
							}
						}
					}
				}
			}
			updateProgressBar((double) s / (listOfSelectedLeaves.length - 1));
		}

		// 5. Write part 3: activate markets
		files.writeToBufferedFile(bw0, "INPUT_TABLE" + vars.getEol());
		files.writeToBufferedFile(bw0, "Variable ID" + vars.getEol());

		String header = "GLIMPSEPFStdActivate";
		String colnames = "region,policy,market,type,year,constrained";
		
		if (which.equals("fixed")) {
			header += "Fx";
			colnames += ",min-price-yr,min-price";
		}
		
		files.writeToBufferedFile(bw0, header + vars.getEol() + vars.getEol());
		files.writeToBufferedFile(bw0, colnames + vars.getEol());
		
		for (int s = 0; s < listOfSelectedLeaves.length; s++) {
			String state = listOfSelectedLeaves[s];
			System.out.println("Creating part 3  of 3 of csv file for " + state + " : " + s + " of "
					+ (listOfSelectedLeaves.length - 1));
			
			for (int i = 0; i < year_list.length; i++) {
				String use_this_market_name = market_name;
				String use_this_policy_name = policy_name;
				
				if (treatment.equals("each selected region") && listOfSelectedLeaves.length >= 2) {
					use_this_market_name = state + "_" + market_name;
					use_this_policy_name = state + "_" + policy_name;
				}
				if (applied_to.equals("sales")) {
					use_this_market_name += "-" + year_list[i];
					use_this_policy_name += "-" + year_list[i];
				}
				if ((vars.isGcamUSA()) && (state.toLowerCase().equals("usa")) && (listOfSelectedLeaves.length > 1)) {
					// skip
				} else {
					String line = "";
					if (which.equals("fixed")) {
						line = state + "," + use_this_policy_name + "," + use_this_market_name + ",RES," + year_list[i]
								+ ",1," + year_list[i] + ",-100" + vars.getEol();
					} else {
						line = state + "," + use_this_policy_name + "," + use_this_market_name + ",RES," + year_list[i]
								+ ",1" + vars.getEol();
					}
					files.writeToBufferedFile(bw0, line);
				}
			}
			updateProgressBar((double) s / (listOfSelectedLeaves.length - 1));
		}

		// 6. Close all writers in finally block for safety
		try {
			files.closeBufferedFile(bw0);
			files.closeBufferedFile(bw1);
			files.closeBufferedFile(bw2);
		} catch (Exception e) {
			System.out.println("Error closing buffered writers: " + e.getMessage());
		}

		// 7. Concatenate temp files
		String temp_file = tempDirName + File.separator + "temp_policy_file.txt";
		files.deleteFile(tempDirName);
		
		String temp_file0 = tempDirName + File.separator + tempFilename0;
		String temp_file1 = tempDirName + File.separator + tempFilename1;
		String temp_file2 = tempDirName + File.separator + tempFilename2;
		
		ArrayList<String> tempfiles = new ArrayList<>();
		tempfiles.add(temp_file0);
		
		if (no_non_nested > 0)
			tempfiles.add(temp_file1);
		if (no_nested > 0)
			tempfiles.add(temp_file2);
		
		files.concatDestSources(temp_file, tempfiles);
		
		System.out.println("Done");
	}

	/**
	 * Generates the metadata content for the scenario component, including selected
	 * options and table data. Used for file output and documentation.
	 * @param tree   The TreeView containing selected regions.
	 * @param market The market name.
	 * @param policy The policy name.
	 * @return The metadata content as a String.
	 */
	public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
		String rtn_str = "";
		StringBuilder rtnStr = new StringBuilder();
		
		ObservableList<String> subset_list = checkComboBoxSubset.getCheckModel().getCheckedItems();
		String subset = utils.getStringFromList(subset_list, ";");
		ObservableList<String> superset_list = checkComboBoxSuperset.getCheckModel().getCheckedItems();
		String superset = utils.getStringFromList(superset_list, ";");

        rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        rtnStr.append("#Type: ").append(this.comboBoxPolicyType.getSelectionModel().getSelectedItem()).append(vars.getEol());
        rtnStr.append("#Subset: ").append(subset).append(vars.getEol());
        rtnStr.append("#Superset: ").append(superset).append(vars.getEol());
        rtnStr.append("#AppliedTo: ").append(this.comboBoxAppliedTo.getSelectionModel().getSelectedItem()).append(vars.getEol());
        rtnStr.append("#Treatment: ").append(this.comboBoxTreatment.getSelectionModel().getSelectedItem()).append(vars.getEol());
        rtnStr.append("#Constraint: ").append(this.comboBoxConstraint.getSelectionModel().getSelectedItem()).append(vars.getEol());
        rtnStr.append("#Policy name: ").append(this.textFieldPolicyName.getText()).append(vars.getEol());
        rtnStr.append("#Market name: ").append(this.textFieldMarketName.getText()).append(vars.getEol());
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
	 * Loads the content of a scenario component from a list of strings, updating the UI accordingly.
	 * Used for loading saved scenario configurations.
	 * @param content The content to load.
	 */
	@Override
	public void loadContent(ArrayList<String> content) {
		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			int pos = line.indexOf(":");
			if (line.startsWith("#") && (pos > -1)) {
				String param = line.substring(1, pos).trim().toLowerCase();
				String value = line.substring(pos + 1).trim();

				if ((param.contains("type")) && (!param.startsWith("Scenario component"))) {
					comboBoxPolicyType.setValue(value);
					comboBoxPolicyType.fireEvent(new ActionEvent());
				} else if (param.equals("applied to")) {
					if (value.contains("sales")) {
						value = "Sales";
					} else {
						value = "All Stock";
					}
					comboBoxAppliedTo.setValue(value);
					comboBoxAppliedTo.fireEvent(new ActionEvent());
				} else if (param.equals("treatment")) {
					comboBoxTreatment.setValue(value);
					comboBoxTreatment.fireEvent(new ActionEvent());
				} else if (param.equals("constraint")) {
					comboBoxConstraint.setValue(value);
					comboBoxConstraint.fireEvent(new ActionEvent());
//				} else if (param.equals("policy name")) {
//					textFieldPolicyName.setText(value);
//					textFieldPolicyName.fireEvent(new ActionEvent());
//				} else if (param.equals("market name")) {
//					textFieldMarketName.setText(value);
//					textFieldMarketName.fireEvent(new ActionEvent());
				} else if (param.equals("subset")) {
					checkComboBoxSubset.getCheckModel().clearChecks();
					String[] set = utils.splitString(value, ";");
					for (int j = 0; j < set.length; j++) {
						String item = set[j].trim();
						// System.out.println("Attempting to check >>"+item+"<<");
						checkComboBoxSubset.getCheckModel().check(item);
					}
					checkComboBoxSubset.fireEvent(new ActionEvent());
				} else if (param.equals("superset")) {
					checkComboBoxSuperset.getCheckModel().clearChecks();
					String[] set = utils.splitString(value, ";");
					for (int j = 0; j < set.length; j++) {
						String item = set[j].trim();
						checkComboBoxSuperset.getCheckModel().check(item);
					}
					checkComboBoxSuperset.fireEvent(new ActionEvent());
				} else if (param.equals("regions")) {
					String[] regions = utils.splitString(value, ",");
					this.paneForCountryStateTree.selectNodes(regions);
				} else if (param.equals("table data")) {
					String[] s = utils.splitString(value, ",");
					this.paneForComponentDetails.data.add(new DataPoint(s[0], s[1]));
				}

			}
		}
		this.setPolicyAndMarketNames();
		this.paneForComponentDetails.updateTable();
	}

	/**
	 * Helper method to validate table data years against allowable policy years.
	 * Ensures that at least one year in the table matches allowable years.
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
	 * Performs QA checks to ensure all required inputs for saving the scenario component are valid.
	 * Checks region selection, table data, subset/superset selections, combo box selections, and unit consistency.
	 * @return true if all required inputs are valid, false otherwise.
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
			if ((checkComboBoxSubset.getCheckModel().getCheckedItems().size() == 0)
					) {
				message += "Subset checkCombox must have at least one selection" + vars.getEol();
				error_count++;
			} else if (checkComboBoxSubset.getCheckModel().getCheckedItems().size() == 0) {
				message += "Subset checkCombox must have at least one selection" + vars.getEol();
				error_count++;
			}
			if ((checkComboBoxSuperset.getCheckModel().getCheckedItems().size() == 0)
					) {
				message += "Superset checkCombox must have at least one selection" + vars.getEol();
				error_count++;
			} else if (checkComboBoxSuperset.getCheckModel().getCheckedItems().size() == 0) {
				message += "Superset checkCombox must have at least one selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxConstraint.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "No selection for Constraint. All comboBoxes must have a selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxAppliedTo.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "No selection for Application. All comboBoxes must have a selection" + vars.getEol();
				error_count++;
			}
			if (comboBoxTreatment.getSelectionModel().getSelectedItem().equals("Select One")) {
				message += "No selection for Treatment. All comboBoxes must have a selection" + vars.getEol();
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
			// check to make sure units match
			if ((checkComboBoxSubset.getCheckModel().getCheckedItems().size() >= 1)
					&& (checkComboBoxSuperset.getCheckModel().getCheckedItems().size() >= 1)) {
				try {
					ObservableList<String> checkBoxSubsetItems = checkComboBoxSubset.getCheckModel().getCheckedItems();
					ObservableList<String> checkBoxSupersetItems = checkComboBoxSuperset.getCheckModel()
							.getCheckedItems();
					String[] items = checkBoxSubsetItems.get(0).split(":");
					String units = null;
					if (items.length == 4)
						units = items[3].trim();
					if (units != null) {
						for (int i = 0; i < checkBoxSubsetItems.size(); i++) {
							items = checkBoxSubsetItems.get(0).split(":");
							if (!items[3].trim().equals(units)) {
								message += "Units of selected items must match: e.g., " + items[3] + "!=" + units;
								error_count++;
							}
						}
						for (int i = 0; i < checkBoxSupersetItems.size(); i++) {
							items = checkBoxSupersetItems.get(0).split(":");
							if (!items[3].trim().equals(units)) {
								message += "Units of selected items must match: e.g., " + items[3] + "!=" + units;
								error_count++;
							}
						}
					}
				} catch (Exception except) {
					System.out.println("Unable to verify that units of selected items match");
				}
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
	 * Returns the suggested filename for the scenario component file.
	 * Used when saving the scenario component to disk.
	 * @return The suggested filename as a String.
	 */
	@Override
	public String getFilenameSuggestion() {
		return filenameSuggestion;
	}

	/**
	 * Resets the filename suggestion to null. Should be called after saving or discarding the scenario component.
	 */
	@Override
	public void resetFilenameSuggestion() {
		filenameSuggestion = null;
	}

	/**
	 * Returns the file content for the scenario component. Used when saving the scenario component to disk.
	 * @return The file content as a String.
	 */
	@Override
	public String getFileContent() {
		return fileContent;
	}

	/**
	 * Resets the file content to null. Should be called after saving or discarding the scenario component.
	 */
	@Override
	public void resetFileContent() {
		fileContent = null;
	}

	/**
	 * Updates the progress bar on the JavaFX Application Thread.
	 * @param progress The progress value to set (between 0.0 and 1.0).
	 */
	private void updateProgressBar(double progress) {
		Platform.runLater(() -> progressBar.setProgress(progress));
	}
}
