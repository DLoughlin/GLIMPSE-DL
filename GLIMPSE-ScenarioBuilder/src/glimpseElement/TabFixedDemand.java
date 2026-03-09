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
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * TabFixedDemand provides the user interface and logic for creating or editing
 * Fixed Demand scenario components in the GLIMPSE Scenario Builder.
 * <p>
 * This tab allows users to specify fixed demand for a sector in selected regions over a time period,
 * using various modification types (e.g., initial/final, growth rates, deltas).
 * Users can populate, edit, and clear demand values, and save the scenario component as a CSV file.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Sector selection with automatic unit display</li>
 *   <li>Region selection via a tree view</li>
 *   <li>Demand value specification by year, initial/final, growth, or delta</li>
 *   <li>Populate, clear, and delete value table</li>
 *   <li>Validation and CSV export of scenario component</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * TabFixedDemand tab = new TabFixedDemand("Fixed Demand", stage);
 * // Add to TabPane, interact via UI
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe and should be used only on the JavaFX Application Thread.</p>
 *
 * <h2>UI Layout</h2>
 * <ul>
 *   <li>Left: Sector and modification type selection, input fields for years and values</li>
 *   <li>Center: Table of demand values, with populate/clear/delete buttons</li>
 *   <li>Right: Region selection tree</li>
 * </ul>
 *
 * <h2>Component Details</h2>
 * <ul>
 *   <li>Supports multiple modification types for demand (initial/final, growth, delta)</li>
 *   <li>Validates user input for sector, region, and year/value data</li>
 *   <li>Exports scenario component as a CSV file with metadata</li>
 *   <li>Supports loading and editing of existing scenario components</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>GLIMPSEVariables, GLIMPSEStyles, GLIMPSEUtils for shared data and utilities</li>
 *   <li>JavaFX controls and layout classes</li>
 * </ul>
 *
 * <h2>Revision History</h2>
 * <ul>
 *   <li>2025-09-16: Updated Javadoc and comments for clarity and completeness.</li>
 *   <li>2026-02-03: Minor updates to Javadoc and removed unused comments.</li>
 * </ul>
 */
public class TabFixedDemand extends PolicyTab implements Runnable {
    // === Constants for UI Strings and Options ===
    
    /** Label text for sector selection. */
    private static final String LABEL_SECTOR = "Sector: ";
    /** Label text for units display. */
    private static final String LABEL_UNITS = "Units: ";
    /** Label text for final value. */
    private static final String LABEL_FINAL = "Final: ";
    /** Label text for values section. */
    private static final String LABEL_VALUES = "Values: ";
    /** Header text for specification section. */
    private static final String LABEL_SPECIFICATION = "Specification:";
    /** Label text for populate button/section. */
    private static final String LABEL_POPULATE = "Populate:";

    /** Standard label width for alignment. */
    private static final double LABEL_WIDTH = 125;
    
    // Modification type strings for demand specification
    /** Modification type: Set initial and final points. */
    private static final String MOD_TYPE_INITIAL_FINAL = "Initial and Final";
    /** Modification type: Initial value with percentage growth per year. */
    private static final String MOD_TYPE_GROWTH_YR = "Initial w/% Growth/yr";
    /** Modification type: Initial value with percentage growth per period. */
    private static final String MOD_TYPE_GROWTH_PD = "Initial w/% Growth/pd";
    /** Modification type: Initial value with constant delta per year. */
    private static final String MOD_TYPE_DELTA_YR = "Initial w/Delta/yr";
    /** Modification type: Initial value with constant delta per period. */
    private static final String MOD_TYPE_DELTA_PD = "Initial w/Delta/pd";
    
    /** Array of all available modification types. */
    private static final String[] MODIFICATION_TYPES = {
            MOD_TYPE_INITIAL_FINAL, MOD_TYPE_GROWTH_YR, MOD_TYPE_GROWTH_PD, MOD_TYPE_DELTA_YR, MOD_TYPE_DELTA_PD
    };
    
    /** Constant for 'Other' sector selection. */
    private static final String SECTOR_OTHER = "Other";
    /** Constant for default select prompt. */
    private static final String SECTOR_SELECT_ONE = "Select One";

    // === Utility singletons ===
    // Shared instances for variables, styles, and utilities
    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

    // === UI controls ===
    // UI controls for sector selection and units display
    private final Label labelSector = createLabel(LABEL_SECTOR, LABEL_WIDTH);
    // Initialize simply here; population and sizing are handled in setup methods
    private final ComboBox<String> comboBoxSector = new ComboBox<>();
    private final Label labelUnits = createLabel(LABEL_UNITS, LABEL_WIDTH);
    private final Label labelUnitsValue = createLabel("", LABEL_WIDTH);
    private final Label labelValue = createLabel(LABEL_VALUES, LABEL_WIDTH);
    
    // === Data ===
    /**
     * Sector information array retrieved from GLIMPSEVariables.
     * Each row contains details about a sector (name, units, etc.).
     */
    private final String[][] sectorInfo;

    /**
     * Description text for the tab (not currently used in UI).
     */
    public static String descriptionText = "";
    /**
     * Run queue string for status display (not currently used in UI).
     */
    public static String runQueueStr = "Queue is empty.";

    /**
     * Constructs a TabFixedDemand for the given title and stage.
     * Initializes UI controls, layout, and event handlers for the Fixed Demand tab.
     *
     * @param title Tab title to display
     * @param stageX JavaFX Stage (not used directly, but may be used for dialogs)
     */
    public TabFixedDemand(String title, Stage stageX) {
        this.setText(title);
        this.setStyle(styles.getFontStyle());
        sectorInfo = vars.getSectorInfo();
        setupUIControls();
        setComponentWidths();
        setupUILayout();
        setupActions();
        
        // Use the shared layout from PolicyTab.setupUILayout().
        // This ensures the tab inherits centralized padding and scroll pane styles.
    }

    /**
     * Initializes the UI structure for the tab.
     * Sets up event handlers and arranges the left, center, and right columns.
     */
    private void setupUIControls() {
    	super.setupEventHandlers();
    	setupLeftColumn();
        setupCenterColumn();
        setupRightColumn();
    }

    /**
     * Configures the content of the left column (Sector selection, inputs).
     * Clears existing children and rebuilds the grid pane with labels and input fields.
     */
    private void setupLeftColumn() {
        gridPaneLeft.getChildren().clear();
        gridPaneLeft.add(utils.createLabel(LABEL_SPECIFICATION), 0, 0, 2, 1);
        gridPaneLeft.addColumn(0, labelSector, new Label(), labelUnits, new Separator(),
                utils.createLabel(LABEL_POPULATE), labelModificationType, labelStartYear, labelEndYear, labelInitialAmount, labelGrowth);
        gridPaneLeft.addColumn(1, comboBoxSector, new Label(), labelUnitsValue, new Separator(), new Label(),
                comboBoxModificationType, textFieldStartYear, textFieldEndYear, textFieldInitialAmount, textFieldGrowth);
        gridPaneLeft.setAlignment(Pos.TOP_LEFT);
        gridPaneLeft.setVgap(3.);
        // Note: Styling is handled via global styles or inherited.
        scrollPaneLeft.setContent(gridPaneLeft);
    }

    /**
     * Sets the widths and sizing for all UI components for layout consistency.
     * Adjusts preferred, minimum, and maximum widths for controls.
     */
    private void setComponentWidths() {
        gridPaneLeft.setPrefWidth(325);
        gridPaneLeft.setMinWidth(325);
        vBoxCenter.setPrefWidth(300);
        vBoxRight.setPrefWidth(300);
        comboBoxSector.setMaxWidth(MAX_WIDTH);
        comboBoxModificationType.setMaxWidth(MAX_WIDTH);
        comboBoxSector.setMinWidth(MIN_WIDTH);
        comboBoxModificationType.setMinWidth(MIN_WIDTH);
        comboBoxSector.setPrefWidth(PREF_WIDTH);
        comboBoxModificationType.setPrefWidth(PREF_WIDTH);
    }

    /**
     * Sets up actions and listeners for UI controls (combo boxes, buttons).
     * Uses Platform.runLater for thread safety if called off JavaFX thread.
     * Handles sector selection, modification type changes, and table actions.
     */
    private void setupActions() {
        setupComboBoxSector();
        comboBoxSector.getSelectionModel().selectFirst();
        comboBoxModificationType.getItems().addAll(MODIFICATION_TYPES);
        comboBoxModificationType.getSelectionModel().selectFirst();
        
        // Sector selection event: update displayed units when a new sector is chosen.
        registerComboBoxEvent(comboBoxSector, e -> {
            String selectedItem = comboBoxSector.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;
            if (SECTOR_OTHER.equals(selectedItem)) {
                // Future feature: set other sector box to visible and enable.
            } else {
                updateSectorOutputAndUnits();
            }
        });
        
        // Initialize state by firing initial event
        comboBoxSector.fireEvent(new ActionEvent());
    }

    /**
     * Updates the label for the growth/final value based on modification type selection.
     * Switches between "Final", "Growth (%)", or "Delta" as appropriate.
     * Uses Platform.runLater for thread safety.
     */
    private void updateGrowthLabel() {
        Platform.runLater(() -> {
            String type = comboBoxModificationType.getSelectionModel().getSelectedItem();
            if (type == null) return;
            switch (type) {
                case MOD_TYPE_GROWTH_YR:
                case MOD_TYPE_GROWTH_PD:
                    labelGrowth.setText("Growth (%):");
                    break;
                case MOD_TYPE_DELTA_YR:
                case MOD_TYPE_DELTA_PD:
                    labelGrowth.setText("Delta:");
                    break;
                case MOD_TYPE_INITIAL_FINAL:
                default:
                    labelGrowth.setText(LABEL_FINAL);
                    break;
            }
        });
    }

    /**
     * Populates the sector combo box with available sectors from sectorInfo.
     * Adds an "Other" option for custom sectors.
     * Handles exceptions and displays a warning if sector list cannot be read.
     */
    private void setupComboBoxSector() {
        try {
            for (String[] sector : sectorInfo) {
                comboBoxSector.getItems().add(sector[0]);
            }
            comboBoxSector.getItems().add(SECTOR_SELECT_ONE);
            comboBoxSector.getSelectionModel().select(SECTOR_SELECT_ONE);
        } catch (Exception e) {
            utils.warningMessage("Problem reading sector list.");
            System.out.println("  ---> " + e);
        }
    }

    /**
     * Updates the units label based on the selected sector.
     * Looks up units in sectorInfo and displays them next to the sector.
     * Uses Platform.runLater for thread safety.
     */
    private void updateSectorOutputAndUnits() {
        Platform.runLater(() -> {
            String selectedSector = comboBoxSector.getValue();
            labelUnitsValue.setText("");
            if (selectedSector == null) return;
            for (String[] sector : sectorInfo) {
                if (selectedSector.equals(sector[0])) {
                    labelUnitsValue.setText(sector[2]);
                }
            }
        });
    }

    /**
     * Runnable implementation: triggers saving the scenario component.
     * Calls saveScenarioComponent() on the JavaFX Application Thread.
     */
    @Override
    public void run() {
        Platform.runLater(this::saveScenarioComponent);
    }

    /**
     * Saves the scenario component to a file.
     * Uses the currently selected regions from the tree view.
     * This is an override of the base class or interface method.
     */
    @Override
    public void saveScenarioComponent() {
        saveScenarioComponent(paneForCountryStateTree.getTree());
    }

	/**
	 * Constructs a suggested filename based on the current sector and selected regions.
	 * 
	 * @return A filename string like "fxDMD_SectorName_RegionList.csv"
	 */
    public String getFilenameSuggestion() {
    	String name="";
    	
    	String sectorName = comboBoxSector.getSelectionModel().getSelectedItem();
		if (sectorName != null) sectorName = sectorName.trim();
		else sectorName = "Unk";

		String[] selectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
		String state = "";
        if (selectedLeaves.length > 0) {
            selectedLeaves = utils.removeUSADuplicate(selectedLeaves);
            String stateStr = utils.returnAppendedString(selectedLeaves).replace(",", "");
            // If the region string is too long, truncate/abbreviate it
            state = stateStr.length() < 9 ? stateStr : "Reg";
        }
        name = ("fxDMD" + "_" + sectorName + "_" + state).replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
		return name;
		
	}
    
    /**
     * Helper method to save the scenario component.
     * Validates input, constructs the CSV content, and prepares the file for saving.
     *
     * @param tree The TreeView containing the selected regions.
     */
    private void saveScenarioComponent(TreeView<String> tree) {
        if (qaInputs()) {
            StringBuilder fileContentBuilder = new StringBuilder();
            
            // 1. Append metadata header
            fileContentBuilder.append(getMetaDataContent(tree, "", ""));
            
            // 2. Define input table structure
            fileContentBuilder.append("INPUT_TABLE").append(vars.getEol())
                .append("Variable ID").append(vars.getEol())
                .append("GLIMPSEFixedDemand").append(vars.getEol()).append(vars.getEol());

            // 3. Prepare data for export
            String[] listOfSelectedLeaves = utils.removeUSADuplicate(utils.getAllSelectedRegions(tree));
            String sectorName = comboBoxSector.getSelectionModel().getSelectedItem();
            if (sectorName != null) sectorName = sectorName.trim();
            filenameSuggestion = getFilenameSuggestion();

            // Extract year and value pairs from the data table
            List<String> dataArrayList = paneForComponentDetails.getDataYrValsArrayList();
            List<String> yearList = new ArrayList<>();
            List<String> valueList = new ArrayList<>();

            for (String str : dataArrayList) {
                String[] split = utils.splitString(str.replace(" ", "").trim(), ",");
                if (split.length > 1) {
                    yearList.add(split[0]);
                    valueList.add(split[1]);
                }
            }

            // 4. Write CSV header and data rows
            fileContentBuilder.append("region,sector,sector,year,value").append(vars.getEol());
            for (String region : listOfSelectedLeaves) {
                for (int i = 0; i < yearList.size(); i++) {
                    fileContentBuilder.append(region).append(",")
                        .append(sectorName).append(",")
                        .append(sectorName).append(",")
                        .append(yearList.get(i)).append(",")
                        .append(valueList.get(i)).append(vars.getEol());
                }
            }

            fileContent = fileContentBuilder.toString();
            System.out.println("CSV file content constructed.");
        }
    }

    /**
     * Generates metadata text for the file header.
     * Includes component type, sector, regions, and raw table data.
     *
     * @param tree The TreeView of selected regions
     * @param market Unused parameter (kept for interface consistency)
     * @param policy Unused parameter (kept for interface consistency)
     * @return A formatted string containing the metadata.
     */
    public String getMetaDataContent(TreeView<String> tree, String market, String policy) {
        StringBuilder rtnStr = new StringBuilder();
        rtnStr.append("########## Scenario Component Metadata ##########").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        rtnStr.append("#Sector:").append(comboBoxSector.getValue()).append(vars.getEol());
        String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
        listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
        String states = utils.returnAppendedString(listOfSelectedLeaves);
        rtnStr.append("#Regions: ").append(states).append(vars.getEol());
        List<String> tableContent = paneForComponentDetails.getDataYrValsArrayList();
        for (String row : tableContent) {
            rtnStr.append("#Table data:").append(row).append(vars.getEol());
        }
        rtnStr.append("#################################################").append(vars.getEol());
        return rtnStr.toString();
    }

    /**
     * Loads a scenario component from a list of strings (file content).
     * Parses metadata to populate the UI fields (sector, regions, table data).
     *
     * @param content ArrayList of strings representing the file lines.
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        for (String line : content) {
            int pos = line.indexOf(":");
            // Check for metadata lines starting with '#'
            if (line.startsWith("#") && (pos > -1)) {
                String param = line.substring(1, pos).trim().toLowerCase();
                String value = line.substring(pos + 1).trim();
                
                if (param.equals("sector")) {
                    comboBoxSector.setValue(value);
                    comboBoxSector.fireEvent(new ActionEvent());
                }
                if (param.equals("regions")) {
                    String[] regions = utils.splitString(value, ",");
                    paneForCountryStateTree.selectNodes(regions);
                }
                if (param.equals("table data")) {
                    String[] s = utils.splitString(value, ",");
                    if (s.length >= 2) {
                         paneForComponentDetails.addTableRow(s[0], s[1]);
                    }
                }
            }
        }
        paneForComponentDetails.updateTable();
    }

    /**
     * Helper method to validate table data years against allowable policy years.
     * Checks that at least one year in the table matches an allowable policy year.
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
     * Validates all required inputs before saving the scenario component.
     * Checks for at least one region, at least one table entry, and sector selection.
     * Also checks that table years match allowable policy years.
     * Displays warnings for any missing or invalid input.
     *
     * @return true if all inputs are valid, false otherwise
     */
    protected boolean qaInputs() {
        TreeView<String> tree = paneForCountryStateTree.getTree();
        int errorCount = 0;
        StringBuilder message = new StringBuilder();
        try {
            // Check if at least one region is selected
			if (utils.getAllSelectedRegions(tree).length < 1) {
				message.append("Must select at least one region from tree").append(vars.getEol());
				errorCount++;
			}
            
            // Check if table has data
			if (paneForComponentDetails == null || paneForComponentDetails.table.getItems().size() == 0) {
				message.append("Data table must have at least one entry").append(vars.getEol());
				errorCount++;
			} else {
                // Validate years in table against allowable policy years
				boolean match = validateTableDataYears();
				if (!match) {
					message.append("Years specified in table must match allowable policy years (")
						.append(vars.getAllowablePolicyYears()).append(")").append(vars.getEol());
					errorCount++;
				}
			}
            
            // Check if sector is selected
            String selected = comboBoxSector.getSelectionModel().getSelectedItem();
            if (selected == null || selected.equals(SECTOR_SELECT_ONE)) {
                message.append("Sector comboBox must have a selection").append(vars.getEol());
                errorCount++;
            }
        } catch (Exception e1) {
            errorCount++;
            message.append("Error in QA of entries").append(vars.getEol());
        }
        
        if (errorCount > 0) {
            if (errorCount == 1) {
                utils.warningMessage(message.toString());
            } else {
                utils.warningMessage("More than one issue with inputs");
                utils.displayString(message.toString(), "Parsing Errors");
            }
        }
        return errorCount == 0;
    }
    
    /**
     * Registers an event handler for a ComboBox's ActionEvent.
     *
     * @param comboBox the ComboBox to register the event for
     * @param handler the event handler
     */
    private void registerComboBoxEvent(ComboBox<String> comboBox, javafx.event.EventHandler<ActionEvent> handler) {
        comboBox.setOnAction(handler);
    }

    /**
     * Registers an event handler for a Button's ActionEvent.
     *
     * @param button The Button to register the event for.
     * @param handler The event handler to execute on action.
     */
    private void registerButtonEvent(Button button, javafx.event.EventHandler<ActionEvent> handler) {
        button.setOnAction(handler);
    }
}
