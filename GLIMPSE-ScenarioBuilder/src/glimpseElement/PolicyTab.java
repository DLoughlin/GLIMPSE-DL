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

import glimpseUtil.Debug;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;

/**
 * Abstract base class for policy-related tabs in the GLIMPSE Scenario Builder.
 * <p>
 * Provides shared functionality for all scenario component tabs, including:
 * <ul>
 *   <li>Progress tracking for long-running operations (e.g., file generation)</li>
 *   <li>File content and filename suggestion management for scenario component export</li>
 *   <li>Market name uniqueness checking to avoid naming conflicts</li>
 *   <li>Access to shared utility singletons (styles, variables, file and utility helpers)</li>
 * </ul>
 * <p>
 * Subclasses must implement {@link #saveScenarioComponent()} and {@link #loadContent(ArrayList)}
 * to define how scenario components are saved and loaded for each policy type.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * public class TabTechTax extends PolicyTab {
 *     // ... implement abstract methods ...
 * }
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is <b>not</b> thread-safe and should be used only on the JavaFX Application Thread.</p>
 */
public abstract class PolicyTab extends Tab {
    // === Fields for scenario component file management and progress ===
    protected final ProgressBar progressBar = new ProgressBar(0.0); // Progress bar for UI
    protected String filenameSuggestion = null; // Suggested filename for saving
    protected String fileContent = null;        // Content of the file to be saved
    protected List<String> marketList;          // List of unique market names

	// === temp files used for saving large scenario components that would exceed memory limits ===
	protected String tempFilename0 = "temp_policy_file0.txt";
	protected String tempFilename1 = "temp_policy_file1.txt";
	protected String tempFilename2 = "temp_policy_file2.txt";
	protected String temp_file0 = null;
	protected String temp_file1 = null;
	protected String temp_file2 = null;
	protected BufferedWriter bw0 = null;
	protected BufferedWriter bw1 = null;
	protected BufferedWriter bw2 = null;
	protected String temp_file = null;
    
    // === Singleton utility instances for use by subclasses ===
    protected final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    protected final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    protected final GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    protected final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

    // === Constants for UI Texts and Options ===
    // Reduced widths to create a more compact, wrap-friendly layout (option 2)
    protected static final double LABEL_WIDTH = 100; // was 125
    protected static final double MAX_WIDTH = 140;   // was 175
    protected static final double MIN_WIDTH = 90;    // was 105
    protected static final double PREF_WIDTH = 140;  // was 175
    protected static final double AUTO_CHECKBOX_WIDTH = 70;
    protected static final double UNIQUE_CHECKBOX_WIDTH = 86;
    protected static final String NONE = "None";
    protected static final String DEFAULT_START_YEAR = "2025";
    protected static final String DEFAULT_END_YEAR = "2050";
    protected static final String DEFAULT_PERIOD_LENGTH = "5";
    protected static final String LABEL_UNITS_DEFAULT = "1975$s per GJ";
    protected static final String BUTTON_POPULATE = "Populate";
    protected static final String BUTTON_FILL = "Fill";
    protected static final String BUTTON_IMPORT = "Import";
    protected static final String BUTTON_DELETE = "Delete";
    protected static final String BUTTON_CLEAR = "Clear";
    protected static final String TRANSPORT_CONVERSION_NOTICE_TITLE = "Transport Conversion Settings Notice";
    protected static final String METADATA_GCAM_VERSION_PRE8_5 = "#isGCAMVersionPre8.5: ";
    protected static final String METADATA_USE_TRN_MMBTU_CONVERSIONS = "#Transport Use MMBTU Conversions: ";
    protected static final String METADATA_USE_TRN_1990_DOLLAR_CONVERSIONS = "#Transport Use 1990 Dollar Conversions: ";
    protected static final String LABEL_POLICY_NAME = "Policy: ";
    protected static final String LABEL_MARKET_NAME = "Market: ";
    protected static final String LABEL_USE_AUTO_NAMES = "Names: ";
    protected static final String LABEL_MODIFICATION_TYPE = "Type: ";
    protected static final String LABEL_VALUES = "Values: ";
    protected static final String CHECKBOX_AUTO = "Auto";
    protected static final String CHECKBOX_UNIQUE = "Unique";
    private static final String REQUIRED_SELECTION_LISTENER_KEY = "requiredSelectionListenerAttached";
    private static final String REQUIRED_SELECTION_BASE_STYLE_KEY = "requiredSelectionBaseStyle";
    private static final String REQUIRED_SELECTION_SUBTLE_OUTLINE = "-fx-border-color: #AECF88; -fx-border-width: 1.2; -fx-border-radius: 3;";
    private static final String CHECK_COMBOBOX_INNER_TEXT_CLEAR_STYLE = "-fx-border-color: transparent; -fx-border-width: 0; -fx-background-insets: 0; -fx-background-color: transparent;";
    private static final String CHECK_COMBOBOX_INNER_CHROME_CLEAR_STYLE = "-fx-border-color: transparent; -fx-border-width: 0; -fx-background-insets: 0;";
    
    // === Constants for default values and labels ===
    protected static final String[] MODIFICATION_TYPE_OPTIONS = {
            "Initial and Final", "Initial w/% Growth/yr", "Initial w/% Growth/pd",
            "Initial w/Delta/yr", "Initial w/Delta/pd"
    };
    protected static final String MOD_TYPE_INITIAL_FINAL = "Initial and Final";
    protected static final String MOD_TYPE_INITIAL_FINAL_PERCENT = "Initial and Final %";
    protected static final String MOD_TYPE_GROWTH_YR = "Initial w/% Growth/yr";
    protected static final String MOD_TYPE_GROWTH_PD = "Initial w/% Growth/pd";
    protected static final String MOD_TYPE_DELTA_YR = "Initial w/Delta/yr";
    protected static final String MOD_TYPE_DELTA_PD = "Initial w/Delta/pd";
    protected static final String[] CONVERT_FROM_OPTIONS = {
            NONE, "2023$s", "2020$s", "2015$s", "2010$s", "2005$s", "2000$s"
    };
        
    // GUI elements shared across all policy tabs
    protected final Label labelStartYear = utils.createLabel("Start Year: ", LABEL_WIDTH);
    protected final TextField textFieldStartYear = createTextField(MIN_WIDTH);
    protected final Label labelEndYear = utils.createLabel("End Year: ", LABEL_WIDTH);
    protected final TextField textFieldEndYear = createTextField(MIN_WIDTH);
    protected final Label labelInitialAmount = utils.createLabel("Initial Val:   ", LABEL_WIDTH);
    protected final TextField textFieldInitialAmount = utils.createTextField();
    protected final Label labelGrowth = utils.createLabel("Growth (%): ", LABEL_WIDTH);
    protected final TextField textFieldGrowth = utils.createTextField();
    protected final Label labelPeriodLength = utils.createLabel("Period Length: ", LABEL_WIDTH);
    protected final TextField textFieldPeriodLength = createTextField(MIN_WIDTH);
    protected final Label labelConvertFrom = utils.createLabel("Convert $s from: ", LABEL_WIDTH);
    protected final ComboBox<String> comboBoxConvertFrom = utils.createComboBoxString(CONVERT_FROM_OPTIONS);	
    protected final Label labelModificationType = utils.createLabel("Type: ", LABEL_WIDTH);
    protected final ComboBox<String> comboBoxModificationType = utils.createComboBoxString(MODIFICATION_TYPE_OPTIONS);
    protected final Label labelUnits2 = utils.createLabel(LABEL_UNITS_DEFAULT, 160.);

    protected final Button buttonPopulate = createButton(BUTTON_POPULATE, styles.getBigButtonWidth(), null);
	protected final Button buttonFill = createButton(BUTTON_FILL, styles.getBigButtonWidth(), null);
    protected final Button buttonImport = createButton(BUTTON_IMPORT, styles.getBigButtonWidth(), null);
    protected final Button buttonDelete = createButton(BUTTON_DELETE, styles.getBigButtonWidth(), null);
    protected final Button buttonClear = createButton(BUTTON_CLEAR, styles.getBigButtonWidth(), null);
    protected final PaneForComponentDetails paneForComponentDetails = new PaneForComponentDetails();
    protected final HBox hBoxHeaderRight = new HBox();
    protected final VBox vBoxRight = new VBox();
    protected final PaneForCountryStateTree paneForCountryStateTree = new PaneForCountryStateTree();

    // === UI Components ===
    protected final GridPane gridPanePresetModification = new GridPane();
    protected final ScrollPane scrollPaneLeft = new ScrollPane();
    protected final ScrollPane scrollPaneCenter= new ScrollPane();
    protected final ScrollPane scrollPaneRight = new ScrollPane();
    protected final GridPane gridPaneLeft = new GridPane();
    protected final VBox vBoxCenter = new VBox();
    protected final HBox hBoxHeaderCenter = new HBox();
    private boolean modificationTypeSelectionListenerAttached = false;
    private boolean syncingSharedNameOptions = false;
    private static boolean sharedUseAutoNames = true;
    private static boolean sharedUseUniqueNames = true;
    private static final List<PolicyTab> policyTabInstances = new ArrayList<>();

    protected final Label labelPolicyName = createLabel(LABEL_POLICY_NAME, LABEL_WIDTH);
    protected final TextField textFieldPolicyName = createTextField(PREF_WIDTH);
    protected final Label labelMarketName = createLabel(LABEL_MARKET_NAME, LABEL_WIDTH);
    protected final TextField textFieldMarketName = createTextField(PREF_WIDTH);
    //protected final Label labelUseAutoNames = createLabel(LABEL_USE_AUTO_NAMES, LABEL_WIDTH);
    //protected final Label labelUseUniqueNames = createLabel(CHECKBOX_UNIQUE, LABEL_WIDTH);
    protected final CheckBox checkBoxUseAutoNames = createCheckBox(CHECKBOX_AUTO, AUTO_CHECKBOX_WIDTH);
    protected final CheckBox checkBoxUseUniqueNames = createCheckBox(CHECKBOX_UNIQUE, UNIQUE_CHECKBOX_WIDTH);
    protected final Label labelValue = createLabel(LABEL_VALUES);

    private static void registerPolicyTabInstance(PolicyTab tab) {
        if (tab != null && !policyTabInstances.contains(tab)) {
            policyTabInstances.add(tab);
        }
    }

    private void applySharedNameOptionsToThisTab() {
        syncingSharedNameOptions = true;
        try {
            checkBoxUseAutoNames.setSelected(sharedUseAutoNames);
            checkBoxUseUniqueNames.setSelected(sharedUseUniqueNames);
            textFieldPolicyName.setDisable(sharedUseAutoNames);
            textFieldMarketName.setDisable(sharedUseAutoNames);
        } finally {
            syncingSharedNameOptions = false;
        }
    }

    private void applySharedNameOptionsToAllTabs(boolean useAutoNames, boolean useUniqueNames) {
        sharedUseAutoNames = useAutoNames;
        sharedUseUniqueNames = useUniqueNames;
        for (PolicyTab tab : policyTabInstances) {
            if (tab != null) {
                tab.applySharedNameOptionsToThisTab();
                tab.setPolicyAndMarketNames();
            }
        }
    }

    /**
     * Save the scenario component. Implemented by subclasses to define how the component is saved.
     * <p>
     * This method should be implemented by subclasses to handle the logic for saving the scenario component
     * represented by the tab. It may involve writing to files, updating UI, or other operations specific to the policy type.
     * </p>
     */
    public abstract void saveScenarioComponent();

    private TextField createTextField(double width) {
        /**
         * Create a new TextField instance with the specified preferred width.
         * @param width The preferred width for the TextField
         * @return A new TextField with the given width
         */
        TextField textField = utils.createTextField();
        // Use minimum width so widgets in gridPaneLeft occupy their minimum space
        textField.setMinWidth(width);
        return textField;
    }

    private CheckBox createCheckBox(String checkboxName, double width) {
        /**
         * Create a new CheckBox instance with the specified label and preferred width.
         * @param checkbox The label for the CheckBox
         * @param width The preferred width for the CheckBox
         * @return A new CheckBox with the given label and width
         */
        CheckBox checkBox = utils.createCheckBox(checkboxName);
        // Keep checkbox captions visible while still constraining layout width.
        checkBox.setMinWidth(width);
        checkBox.setPrefWidth(width);
        checkBox.setMaxWidth(width);
        checkBox.setStyle("-fx-text-fill: black;");
        return checkBox;
    }

    /**
     * Appends transport conversion settings to scenario metadata for reproducibility.
     *
     * @param metadataBuilder metadata content being assembled
     */
    protected void appendTransportConversionMetadata(StringBuilder metadataBuilder) {
        if (metadataBuilder == null) {
            return;
        }
        metadataBuilder.append(METADATA_GCAM_VERSION_PRE8_5)
                .append(vars.isGcamVersionPre8_5())
                .append(vars.getEol());
    }

    /**
     * Checks one metadata line for transport conversion settings and reports a mismatch warning if found.
     *
     * @param line one metadata line from a saved scenario component
     * @return warning text when saved settings differ from current options, otherwise null
     */
    protected String getTransportConversionMetadataMismatchWarning(String line) {
        if (line == null) {
            return null;
        }

        if (line.startsWith(METADATA_GCAM_VERSION_PRE8_5)) {
            String value = line.substring(METADATA_GCAM_VERSION_PRE8_5.length()).trim();
            Boolean saved = parseBooleanMetadataValue(value);
            if (saved != null && saved.booleanValue() != vars.isGcamVersionPre8_5()) {
                return "GCAM version pre-8.5 transport conventions saved as " + saved
                        + "; current options set this to " + vars.isGcamVersionPre8_5() + ".";
            }
        } else if (line.startsWith(METADATA_USE_TRN_MMBTU_CONVERSIONS)) {
            String value = line.substring(METADATA_USE_TRN_MMBTU_CONVERSIONS.length()).trim();
            Boolean saved = parseBooleanMetadataValue(value);
            if (saved != null && saved.booleanValue() != vars.isGcamVersionPre8_5()) {
                return "Transport MMBTU conversions saved as " + saved
                        + "; current options set this to " + vars.isGcamVersionPre8_5() + ".";
            }
        } else if (line.startsWith(METADATA_USE_TRN_1990_DOLLAR_CONVERSIONS)) {
            String value = line.substring(METADATA_USE_TRN_1990_DOLLAR_CONVERSIONS.length()).trim();
            Boolean saved = parseBooleanMetadataValue(value);
            if (saved != null && saved.booleanValue() != vars.isGcamVersionPre8_5()) {
                return "Transport 1990-dollar conversions saved as " + saved
                        + "; current options set this to " + vars.isGcamVersionPre8_5() + ".";
            }
        }

        return null;
    }

    /**
     * Displays a standardized notice when a loaded component's saved transport settings
     * differ from the current options file.
     *
     * @param warnings unique warning lines collected while loading metadata
     */
    protected void showTransportConversionMetadataWarnings(ArrayList<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }

        String componentName = getText();
        if (componentName == null || componentName.trim().isEmpty()) {
            componentName = "Scenario Component";
        }

        StringBuilder message = new StringBuilder();
        message.append("The saved ").append(componentName)
                .append(" component was created with GCAM transport-version settings that differ from the current options file.")
                .append(vars.getEol()).append(vars.getEol());
        for (String warning : warnings) {
            message.append("- ").append(warning).append(vars.getEol());
        }
        message.append(vars.getEol())
                .append("Loaded component values were not changed. Review the current options if you want future calculations and saves to use matching GCAM transport settings.");

        utils.displayString(message.toString(), TRANSPORT_CONVERSION_NOTICE_TITLE + " - " + componentName);
    }

    private Boolean parseBooleanMetadataValue(String value) {
        if (value == null) {
            return null;
        }
        String lc = value.trim().toLowerCase();
        if ("true".equals(lc) || "yes".equals(lc)) {
            return Boolean.TRUE;
        }
        if ("false".equals(lc) || "no".equals(lc)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private ComboBox createComboBox(String comboBoxName, double width) {
        /**
         * Create a new ComboBox instance with the specified initial item and preferred width.
         * @param comboBox The initial item to add to the ComboBox
         * @param width The preferred width for the ComboBox
         * @return A new ComboBox with the given item and width
         */
        ComboBox comboBox = utils.createComboBox();
        comboBox.getItems().add(comboBoxName);
        // Use minimum width to keep controls compact inside gridPaneLeft
        comboBox.setMinWidth(width);
        return comboBox;
    }

    // Rounded-corner gray border applied to column content containers (gridPaneLeft, vBoxCenter, vBoxRight).
    // Applying to the content VBox/GridPane instead of the ScrollPane gives crisp rounded corners,
    // matching the look of TabTechAvailable's leftPanel.
    protected static final String SCROLLPANE_BORDER_STYLE = "-fx-border-color: #A9A9A9; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;";

    /**
     * Makes a scroll pane fully transparent so the content container's border and background
     * are the only visible column chrome, avoiding the ScrollPane viewport clipping issue.
     */
    private void applyScrollPaneBorder(ScrollPane sp) {
        if (sp == null) return;
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0;");
    }

    /**
     * Returns the full CSS style for a column content pane: standard white background +
     * the rounded-corner gray border. Use this on gridPaneLeft, vBoxCenter, and vBoxRight
     * so all three columns match TabTechAvailable's leftPanel appearance.
     */
    protected String getColumnPanelStyle() {
        return styles.getStyle2() + " " + SCROLLPANE_BORDER_STYLE;
    }

    /**
     * Harmonizes internal insets across the three pane columns so top headers line up.
     * Left/center use shared container padding, while right relies on PaneForCountryStateTree padding.
     */
    private void harmonizeColumnInsets() {
        Insets contentPadding = styles.getDefaultPadding();

        // Keep scroll pane chrome consistent; content containers own the visible insets.
        scrollPaneLeft.setPadding(Insets.EMPTY);
        scrollPaneCenter.setPadding(Insets.EMPTY);
        scrollPaneRight.setPadding(Insets.EMPTY);

        gridPaneLeft.setPadding(contentPadding);
        vBoxCenter.setPadding(contentPadding);

        // Right column: vBoxRight owns the padding; PaneForCountryStateTree has its own
        // padding cleared so all three columns share an identical top inset.
        vBoxRight.setPadding(contentPadding);
    }

    /**
     * Arranges all UI controls in the layout containers for the tab.
     * Organizes controls into left (inputs), center (table), and right (region tree) columns.
     */
    public void setupUILayout() {
       	//System.out.println("Setting up UI layout in PolicyTab");
         gridPanePresetModification.addColumn(0, scrollPaneLeft);
         gridPanePresetModification.addColumn(1, scrollPaneCenter);
         gridPanePresetModification.addColumn(2, scrollPaneRight);
         
         ColumnConstraints col1 = new ColumnConstraints();
         col1.setPercentWidth(33.33);
         ColumnConstraints col2 = new ColumnConstraints();
         col2.setPercentWidth(33.33);
         ColumnConstraints col3 = new ColumnConstraints();
         col3.setPercentWidth(33.33);
         
         gridPanePresetModification.getColumnConstraints().addAll(col1, col2, col3);
         
         scrollPaneLeft.setFitToWidth(true);
         scrollPaneCenter.setFitToWidth(true);
         scrollPaneRight.setFitToWidth(true);

         // Prefer to disable horizontal scrollbars and keep content sized to the viewport
         scrollPaneLeft.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
         scrollPaneCenter.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
         scrollPaneRight.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

         // Bind each scroll pane viewport width to one third of the grid pane width so the
         // content will be sized to the visible area (prevents horizontal scrollbars).
         scrollPaneLeft.prefViewportWidthProperty().bind(gridPanePresetModification.widthProperty().multiply(0.3333));
         scrollPaneCenter.prefViewportWidthProperty().bind(gridPanePresetModification.widthProperty().multiply(0.3333));
         scrollPaneRight.prefViewportWidthProperty().bind(gridPanePresetModification.widthProperty().multiply(0.3333));

         // Bind viewport height to the grid pane height so the viewport matches available vertical space
         scrollPaneLeft.prefViewportHeightProperty().bind(gridPanePresetModification.heightProperty());
         scrollPaneCenter.prefViewportHeightProperty().bind(gridPanePresetModification.heightProperty());
         scrollPaneRight.prefViewportHeightProperty().bind(gridPanePresetModification.heightProperty());

         // Ensure content nodes resize to fit the viewport width (prevents horizontal scrollbars)
         gridPaneLeft.prefWidthProperty().bind(scrollPaneLeft.prefViewportWidthProperty());
         vBoxCenter.prefWidthProperty().bind(scrollPaneCenter.prefViewportWidthProperty());
         vBoxRight.prefWidthProperty().bind(scrollPaneRight.prefViewportWidthProperty());

         // Limit content max height to the viewport height to reduce vertical scrolling at the target size
         gridPaneLeft.maxHeightProperty().bind(scrollPaneLeft.prefViewportHeightProperty());
         vBoxCenter.maxHeightProperty().bind(scrollPaneCenter.prefViewportHeightProperty());
         vBoxRight.maxHeightProperty().bind(scrollPaneRight.prefViewportHeightProperty());

         gridPaneLeft.setStyle(getColumnPanelStyle() + "; -fx-background-color: white;");
        // Ensure the second column (index 1) of gridPaneLeft expands to fill remaining horizontal space.
        // Set up three column constraints: label column (fixed/min), middle column (growable), and optional third column (min).
        ColumnConstraints gc0 = new ColumnConstraints();
        gc0.setMinWidth(LABEL_WIDTH);
        gc0.setHgrow(Priority.NEVER);

        ColumnConstraints gc1 = new ColumnConstraints();
        gc1.setHgrow(Priority.ALWAYS);
        gc1.setFillWidth(true);

        ColumnConstraints gc2 = new ColumnConstraints();
        gc2.setMinWidth(0);
        gc2.setHgrow(Priority.NEVER);

        gridPaneLeft.getColumnConstraints().clear();
        gridPaneLeft.getColumnConstraints().addAll(gc0, gc1, gc2);

        // Ensure any existing or future Region nodes placed in column 1 can expand to fill the available width.
        // Some subclasses actively call setMaxWidth(...) later. To be robust we attach a listener that
        // forces maxWidth back to Double.MAX_VALUE whenever it's changed, and also set Hgrow to ALWAYS.
        java.util.function.Consumer<javafx.scene.Node> enforceGrow = node -> {
            Integer colIndex = GridPane.getColumnIndex(node);
            if (colIndex == null) colIndex = 0;
            if (colIndex == 1 && node instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region r = (javafx.scene.layout.Region) node;
                // Ensure the region can grow without arbitrary caps
                r.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(r, Priority.ALWAYS);
                // If a subclass later calls setMaxWidth, this listener will re-assert our desired value.
                r.maxWidthProperty().addListener((obs, oldV, newV) -> {
                    if (newV == null || newV.doubleValue() != Double.MAX_VALUE) {
                        // Schedule to re-apply to avoid modifying property on the calling stack
                        Platform.runLater(() -> r.setMaxWidth(Double.MAX_VALUE));
                    }
                });
            }
        };

        gridPaneLeft.getChildren().forEach(enforceGrow::accept);

        // Also listen for children added later and apply the same enforcement.
        gridPaneLeft.getChildren().addListener((ListChangeListener<javafx.scene.Node>) change -> {
            while (change.next()) {
                for (javafx.scene.Node added : change.getAddedSubList()) {
                    enforceGrow.accept(added);
                }
            }
        });

        // Final pass after layout completes to override any late size-setting done by subclasses.
        Platform.runLater(() -> gridPaneLeft.getChildren().forEach(enforceGrow::accept));

        // Apply the shared section-header style to any Label in row 0 of gridPaneLeft
        // (the "Specification:" label added by each subclass) so all three column headers
        // use getStyle3() consistently without requiring changes to every subclass.
        Platform.runLater(() -> {
            for (javafx.scene.Node node : gridPaneLeft.getChildren()) {
                Integer row = GridPane.getRowIndex(node);
                if ((row == null || row == 0) && node instanceof Label) {
                    ((Label) node).setStyle(styles.getStyle3());
                }
            }
        });
 		VBox tabLayout = new VBox();
	// Centralized styling: apply default padding and background style so all tabs inherit consistent look
	tabLayout.setPadding(styles.getDefaultPadding());
	tabLayout.setSpacing(6.0);
	tabLayout.setStyle(styles.getStyle2());
	// Let the grid pane grow to fill the available vertical space in the VBox
	tabLayout.getChildren().addAll(gridPanePresetModification);
	VBox.setVgrow(gridPanePresetModification, Priority.ALWAYS);
	// Bind the grid pane height to the tab layout so child columns can size to it
	gridPanePresetModification.prefHeightProperty().bind(tabLayout.heightProperty());
	gridPanePresetModification.prefWidthProperty().bind(tabLayout.widthProperty());

	// Make the scroll panes expand vertically to fill their column
	scrollPaneLeft.setFitToHeight(true);
	scrollPaneCenter.setFitToHeight(true);
	scrollPaneRight.setFitToHeight(true);
	// Ensure scroll panes use centralized style and padding; add subtle border to separate columns
	applyScrollPaneBorder(scrollPaneLeft);
	applyScrollPaneBorder(scrollPaneCenter);
	applyScrollPaneBorder(scrollPaneRight);
        harmonizeColumnInsets();
 		scrollPaneLeft.prefHeightProperty().bind(gridPanePresetModification.heightProperty());
 		scrollPaneCenter.prefHeightProperty().bind(gridPanePresetModification.heightProperty());
 		scrollPaneRight.prefHeightProperty().bind(gridPanePresetModification.heightProperty());
 	 
   	this.setContent(tabLayout);
     }
 	
    /**
     * Creates a new ComboBox<String> with the specified preferred width.
     *
     * @param prefWidth the preferred width for the ComboBox
     * @return a new ComboBox<String> instance with the given preferred width
     */
    protected ComboBox<String> createComboBoxString(double prefWidth) {
		ComboBox<String> comboBox = new ComboBox<>();
		// Use minimum width so combo boxes align to their minimum widths in gridPaneLeft
		comboBox.setMinWidth(prefWidth);
         return comboBox;
     }

  /**
   * Replaces modification-type options while preventing duplicate entries.
   * Use this from tab constructors/setup so repeated initialization does not
   * append a second copy of the same choices.
   */
  protected void setModificationTypeOptions(String... options) {
    comboBoxModificationType.getItems().clear();
    String[] source = (options == null || options.length == 0) ? MODIFICATION_TYPE_OPTIONS : options;
    final String preferredFirst = MOD_TYPE_INITIAL_FINAL;
    for (String option : source) {
      if (option == null) {
        continue;
      }
      String trimmed = option.trim();
      if (preferredFirst.equalsIgnoreCase(trimmed)) {
        comboBoxModificationType.getItems().add(preferredFirst);
        break;
      }
    }
    for (String option : source) {
      if (option == null) {
        continue;
      }
      String trimmed = option.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (preferredFirst.equalsIgnoreCase(trimmed)) {
        continue;
      }
      if (!comboBoxModificationType.getItems().contains(trimmed)) {
        comboBoxModificationType.getItems().add(trimmed);
      }
    }
    if (!comboBoxModificationType.getItems().isEmpty()) {
      comboBoxModificationType.getSelectionModel().selectFirst();
      refreshModificationTypeLabels();
    }
  }

    /** Keeps Initial/Growth label text in sync with the selected modification type. */
    protected void refreshModificationTypeLabels() {
        applyModificationTypeLabels(comboBoxModificationType.getSelectionModel().getSelectedItem());
    }

    private void applyModificationTypeLabels(String selected) {
        if (selected == null) {
            return;
        }
        switch (selected) {
            case MOD_TYPE_GROWTH_YR:
            case MOD_TYPE_GROWTH_PD:
                labelInitialAmount.setText("Initial Val:");
                labelGrowth.setText("Growth (%):");
                break;
            case MOD_TYPE_DELTA_YR:
            case MOD_TYPE_DELTA_PD:
                labelInitialAmount.setText("Initial Val:");
                labelGrowth.setText("Delta:");
                break;
            case MOD_TYPE_INITIAL_FINAL:
                labelInitialAmount.setText("Initial Val:");
                labelGrowth.setText("Final Val:");
                break;
            case MOD_TYPE_INITIAL_FINAL_PERCENT:
                labelInitialAmount.setText("Initial Val (%):");
                labelGrowth.setText("Final Val (%):");
                break;
            default:
                break;
        }
    }
	
	/**
     * Load content into the tab. Implemented by subclasses to define how content is loaded.
     * <p>
     * This method should be implemented by subclasses to handle loading of scenario component data into the tab UI.
     * @param content List of content lines to load (e.g., from a file)
     */
    public abstract void loadContent(ArrayList<String> content);

    /**
     * Set the progress bar value for long-running operations.
     * <p>
     * This method is thread-safe and will update the progress bar on the JavaFX Application Thread.
     * @param progress Progress value between 0.0 and 1.0
     */
    public void setProgress(double progress) {
        if (Platform.isFxApplicationThread()) {
            getProgressBar().setProgress(progress);
        } else {
            Platform.runLater(() -> getProgressBar().setProgress(progress));
        }
    }

    /**
     * Get the suggested filename for saving the scenario component.
     * @return Suggested filename, or null if not set
     */
    public String getFilenameSuggestion() {
        return filenameSuggestion;
    }

    /**
     * Sets up the center column UI controls and layout.
     * <p>
     * Arranges the header buttons and component details pane in the center VBox and sets the style.
     * </p>
     */
    public void setupCenterColumn() {
       	hBoxHeaderCenter.getChildren().clear();
       	hBoxHeaderCenter.getChildren().addAll(buttonPopulate, buttonFill, buttonDelete, buttonClear);
    hBoxHeaderCenter.setSpacing(1.);
    hBoxHeaderCenter.setPadding(new Insets(0, 0, 0, 0));
    hBoxHeaderCenter.setMaxWidth(Double.MAX_VALUE);
    // Counter the center VBox horizontal padding so Populate/Clear sit closer to pane edges.
    Insets centerPadding = vBoxCenter.getPadding() == null ? Insets.EMPTY : vBoxCenter.getPadding();
    VBox.setMargin(hBoxHeaderCenter, new Insets(0, -centerPadding.getRight(), 0, -centerPadding.getLeft()));
    for (Button button : new Button[] { buttonPopulate, buttonFill, buttonDelete, buttonClear }) {
      button.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(button, Priority.ALWAYS);
    }
       	hBoxHeaderCenter.setStyle(styles.getStyle2()); //DHL was 3
       	// Match the section-header style used by "Regions:" and "Specification:" labels
       	labelValue.setStyle(styles.getStyle3());
       	vBoxCenter.getChildren().clear();
       	vBoxCenter.getChildren().addAll(labelValue, hBoxHeaderCenter, paneForComponentDetails);
       	// Ensure the center VBox fills width and can size to the ScrollPane viewport
       	vBoxCenter.setFillWidth(true);
       	// Use the scroll pane's viewportBounds (updated after layout) to get an actual viewport height
       	DoubleBinding viewportHeight = Bindings.createDoubleBinding(() ->
       		(scrollPaneCenter.getViewportBounds() == null) ? 0.0 : scrollPaneCenter.getViewportBounds().getHeight(),
       		scrollPaneCenter.viewportBoundsProperty());

       	// Bind vBoxCenter height to the actual viewport height so it receives a non-zero value when layout completes
       	vBoxCenter.prefHeightProperty().bind(viewportHeight);

       	// Allow the details pane to grow inside the center VBox
       	VBox.setVgrow(paneForComponentDetails, Priority.ALWAYS);
       	paneForComponentDetails.setMaxHeight(Double.MAX_VALUE);
       	paneForComponentDetails.setMinHeight(0);

       	// Bind the details pane preferred height explicitly to the viewport height minus header rows and padding
       	paneForComponentDetails.prefHeightProperty().bind(
       		viewportHeight
       			.subtract(labelValue.heightProperty())
       			.subtract(hBoxHeaderCenter.heightProperty())
       			.subtract(8.0)
       	);
          	// Apply the column panel style (white bg + rounded-corner border) to match TabTechAvailable's leftPanel look
          	vBoxCenter.setStyle(getColumnPanelStyle());
           	scrollPaneCenter.setContent(vBoxCenter);
      }

 	/**
 	 * Sets up the right column of the UI with the country/state tree for region selection.
 	 * <p>
 	 * Adds the region selection tree to the right VBox and applies styling.
 	 * </p>
 	 */
     public void setupRightColumn() {
         vBoxRight.getChildren().clear();
         // Put the region tree pane directly into the right column VBox
         vBoxRight.getChildren().addAll(paneForCountryStateTree);
         // Apply the column panel style (white bg + rounded-corner border) to match TabTechAvailable's leftPanel look
         vBoxRight.setStyle(getColumnPanelStyle());
         // Make the VBox allow its children to grow and fill width
         vBoxRight.setFillWidth(true);
         vBoxRight.setMaxHeight(Double.MAX_VALUE);
         vBoxRight.setMinHeight(0);
         // Place the VBox into the right scroll pane and have the scroll pane size content to its viewport
         scrollPaneRight.setContent(vBoxRight);
         scrollPaneRight.setFitToHeight(true);

         // Bind the vBox height to the scroll pane viewport height so the VBox fills the visible area
         vBoxRight.prefHeightProperty().bind(scrollPaneRight.prefViewportHeightProperty());

         // Allow the country/state tree pane to grow inside the VBox to occupy remaining vertical space
         VBox.setVgrow(paneForCountryStateTree, Priority.ALWAYS);
         paneForCountryStateTree.setMaxHeight(Double.MAX_VALUE);
         paneForCountryStateTree.setMinHeight(0);
         // Let the pane size be governed by VBox layout (no direct binding to scroll pane height)
    	}
    
    /**
     * Get the file content for the scenario component.
     * @return File content string, or null if not set
     */
    public String getFileContent() {
 		if (fileContent == null) {
 			Debug.log("File content is null.");
 			return null;
 		}
 		//System.out.println("Getting file content... length:" + fileContent.length());
 		return fileContent;
 	}

    /**
     * Reset the file content to null after saving or cancelling.
     */
    public void resetFileContent() {
        fileContent = null;
    }

    /**
     * Reset the filename suggestion to null after saving or cancelling.
     */
    public void resetFilenameSuggestion() {
        filenameSuggestion = null;
    }

    /**
     * Reset the progress bar to 0 after an operation completes.
     * <p>
     * This method is thread-safe and will update the progress bar on the JavaFX Application Thread.
     * </p>
     */
    public void resetProgressBar() {
        if (Platform.isFxApplicationThread()) {
            getProgressBar().setProgress(0.0);
        } else {
            Platform.runLater(() -> getProgressBar().setProgress(0.0));
        }
    }

    /**
     * Protected constructor for subclassing. Prevents direct instantiation.
     * <p>
     * Subclasses should call this constructor.
     * </p>
     */
    protected PolicyTab() {
        // No-op constructor for subclassing
        // Ensure left grid pane has consistent padding across all policy tabs
        //gridPaneLeft.setPadding(styles.getDefaultPadding());
        //gridPaneLeft.setStyle(styles.getBackgroundStyle());

        registerPolicyTabInstance(this);
        applySharedNameOptionsToThisTab();

        scrollPaneLeft.setPadding(styles.getDefaultPadding());
        scrollPaneLeft.setStyle(styles.getBackgroundStyle());
        scrollPaneCenter.setPadding(styles.getDefaultPadding());
        scrollPaneCenter.setStyle(styles.getBackgroundStyle());
        scrollPaneRight.setPadding(styles.getDefaultPadding());
        scrollPaneRight.setStyle(styles.getBackgroundStyle());
        
        // Centralize default styling for scroll panes and main center/right containers
        applyScrollPaneBorder(scrollPaneLeft);
        applyScrollPaneBorder(scrollPaneCenter);
        applyScrollPaneBorder(scrollPaneRight);
         // Apply padding to center/right VBoxes so contents align consistently
         vBoxCenter.setPadding(styles.getDefaultPadding());
         vBoxRight.setPadding(styles.getDefaultPadding());
         hBoxHeaderCenter.setPadding(styles.getDefaultPadding());
         harmonizeColumnInsets();
         // Set default text values for fields created via helper so initial values are preserved
        textFieldStartYear.setText(DEFAULT_START_YEAR);
        textFieldEndYear.setText(DEFAULT_END_YEAR);
        textFieldPeriodLength.setText(DEFAULT_PERIOD_LENGTH);
      }


    /**
     * Generate a unique market name if the given name already exists in the market list.
     * <p>
     * This method checks the scenario components directory for existing market names and appends a numeric suffix if needed.
     * @param marketName The original market name
     * @return A unique market name suffix (e.g., "2"), or empty string if not needed
     */
    public String getUniqueMarketName(String marketName) {
        String result = "";
        File folder = new File(vars.getScenarioComponentsDir());
        String[] fileList = folder.list();
        if (fileList == null) {
            return result;
        }
        if (marketList == null) {
            marketList = new ArrayList<>();
            for (String fileName : fileList) {
                String filePath = vars.getScenarioComponentsDir() + File.separator + fileName;
                File file = new File(filePath);
                if (!file.isDirectory()) {
                    ArrayList<String> lines = files.searchForTextInFileA(filePath, "Mkt", "#");
                    for (String line : lines) {
                        String mktName = utils.getTokenWithText(line, "Mkt", ",");
                        if (!utils.getMatch(mktName, marketList)) {
                            marketList.add(mktName);
                        }
                    }
                }
            }
        }
        int id = 0;
        for (String marketFromList : marketList) {
            if (marketFromList != null && marketFromList.startsWith(marketName)) {
                id++;
            }
        }
        if (id != 0) {
            String uniqueName = marketName + id;
            marketList.add(uniqueName);
            result = String.valueOf(id);
        }
        return result;
    }
    
    /**
     * Calculates the values for the policy based on user input and conversion factors.
     * <p>
     * Uses the selected modification type, years, initial value, growth, and conversion factor to compute a 2D array of values.
     * </p>
     * @return a 2D array of calculated values
     */
    protected double[][] calculateValues() {
        String calcType = comboBoxModificationType.getSelectionModel().getSelectedItem();
        int startYear = Integer.parseInt(textFieldStartYear.getText());
        int endYear = Integer.parseInt(textFieldEndYear.getText());
        double initialValue = Double.parseDouble(textFieldInitialAmount.getText());
        double growth = Double.parseDouble(textFieldGrowth.getText());
        int periodLength = vars.getPeriodIncrement();
        double factor = 1.0;
        String convertYear = comboBoxConvertFrom.getValue();
        String tempUnitsVal = labelUnits2.getText();
        String toYear = tempUnitsVal.contains("1990") ? "1990$s" : "1975$s";

        if (!NONE.equals(convertYear)) {
            factor = utils.getConversionFactor(convertYear, toYear);
        }
        return utils.calculateValues(calcType, startYear, endYear, initialValue, growth, periodLength, factor);
    }
    
    /**
     * Display a warning message to the user (centralized for all tabs).
     * <p>
     * Shows a warning dialog with the specified message.
     * </p>
     * @param message The warning message to display
     */
    public void showWarning(String message) {
        utils.warningMessage(message);
    }

    /**
     * Display an informational message to the user (centralized for all tabs).
     * <p>
     * Shows an informational dialog with the specified message and title.
     * </p>
     * @param message The info message to display
     * @param title The title for the message dialog
     */
    public void showInfo(String message, String title) {
        utils.displayString(message, title);
    }

    /**
     * Get the progress bar associated with this tab for UI binding.
     * @return ProgressBar instance for this tab
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Standardized UI component creation methods for tab subclasses.
     * @param text The label text
     * @return A new Label instance
     */
    protected javafx.scene.control.Label createLabel(String text) {
        return utils.createLabel(text);
    }
    /**
     * Create a label with specified text and width.
     * @param text The label text
     * @param width The label width
     * @return A new Label instance
     */
    protected Label createLabel(String text, double width) {
        Label label = utils.createLabel(text, width);
        return label;
    }
    /**
     * Create a new TextField instance.
     * @return A new TextField
     */
    protected TextField createTextField() {
        return utils.createTextField();
    }
    /**
     * Create a ComboBox for String values.
     * @return A new ComboBox<String>
     */
    protected ComboBox<String> createComboBoxString() {
        return utils.createComboBoxString();
    }
    
    /**
     * Create a ComboBox for String values with a seed text and preferred width.
     * @param seedTxt The initial item to add
     * @param width The preferred width
     * @return A new ComboBox<String>
     */
    protected ComboBox<String> createComboBoxString(String seedTxt, double width) {
        ComboBox<String> comboBox = utils.createComboBoxString();
        comboBox.getItems().add(seedTxt);
        comboBox.setMinWidth(width);
        return comboBox;
    }

    /**
     * Configure a ComboBox to use prompt text for its unselected state.
     * Optionally clears any current selection so the prompt is visible.
     *
     * @param comboBox the combo box to configure
     * @param promptText the prompt text to display when nothing is selected
     */
    protected void setComboBoxPrompt(ComboBox<String> comboBox, String promptText) {
        setComboBoxPrompt(comboBox, promptText, true);
    }

    /**
     * Configure a ComboBox prompt text and optionally clear the current selection.
     *
     * @param comboBox the combo box to configure
     * @param promptText the prompt text to display when nothing is selected
     * @param clearSelection whether the current selection should be cleared
     */
    protected void setComboBoxPrompt(ComboBox<String> comboBox, String promptText, boolean clearSelection) {
        if (comboBox == null) {
            return;
        }
        comboBox.setPromptText(promptText);
        if (clearSelection) {
            comboBox.getSelectionModel().clearSelection();
        }
        ensureRequiredSelectionListener(comboBox);
        applyRequiredSelectionOutline(comboBox);
    }

    /**
     * Select the first ComboBox item when one exists.
     *
     * @param comboBox the combo box to update
     */
    protected void selectFirstIfPresent(ComboBox<String> comboBox) {
        if (comboBox != null && !comboBox.getItems().isEmpty()) {
            comboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Clears and repopulates a ComboBox with trimmed, non-empty values.
     *
     * @param comboBox combo box to reset
     * @param items items to load
     */
    protected void resetComboBoxItems(ComboBox<String> comboBox, List<String> items) {
        resetComboBoxItems(comboBox, items, null, false);
    }

    /**
     * Clears and repopulates a ComboBox, with optional default value and selection behavior.
     *
     * @param comboBox combo box to reset
     * @param items items to load
     * @param defaultValue optional default item to append if missing
     * @param selectDefault when true and default exists, select it; otherwise clears selection
     */
    protected void resetComboBoxItems(ComboBox<String> comboBox, List<String> items, String defaultValue, boolean selectDefault) {
        if (comboBox == null) {
            return;
        }

        comboBox.getItems().clear();

        if (items != null) {
            for (String item : items) {
                if (item == null) {
                    continue;
                }
                String trimmed = item.trim();
                if (trimmed.isEmpty() || comboBox.getItems().contains(trimmed)) {
                    continue;
                }
                comboBox.getItems().add(trimmed);
            }
        }

        String normalizedDefault = defaultValue == null ? null : defaultValue.trim();
        if (normalizedDefault != null && !normalizedDefault.isEmpty() && !comboBox.getItems().contains(normalizedDefault)) {
            comboBox.getItems().add(normalizedDefault);
        }

        comboBox.getItems().sort(String::compareToIgnoreCase);
 
        if (selectDefault && normalizedDefault != null && !normalizedDefault.isEmpty()) {
            comboBox.getSelectionModel().select(normalizedDefault);
        } else {
            comboBox.getSelectionModel().clearSelection();
        }
        applyRequiredSelectionOutline(comboBox);
    }

    /**
     * Clears and repopulates a CheckComboBox with trimmed, non-empty values.
     *
     * @param checkComboBox check combo box to reset
     * @param items items to load
     */
    protected void resetCheckComboBoxItems(CheckComboBox<String> checkComboBox, List<String> items) {
        resetCheckComboBoxItems(checkComboBox, items, false);
    }

    /**
     * Clears and repopulates a CheckComboBox, with optional check-all behavior.
     *
     * @param checkComboBox check combo box to reset
     * @param items items to load
     * @param checkAll when true, checks all loaded items
     */
    protected void resetCheckComboBoxItems(CheckComboBox<String> checkComboBox, List<String> items, boolean checkAll) {
        if (checkComboBox == null) {
            return;
        }

        checkComboBox.getCheckModel().clearChecks();
        checkComboBox.getItems().clear();

        if (items != null) {
            for (String item : items) {
                if (item == null) {
                    continue;
                }
                String trimmed = item.trim();
                if (trimmed.isEmpty() || checkComboBox.getItems().contains(trimmed)) {
                    continue;
                }
                checkComboBox.getItems().add(trimmed);
            }
        }

        checkComboBox.getItems().sort(String::compareToIgnoreCase);
 
        if (checkAll && !checkComboBox.getItems().isEmpty()) {
            checkComboBox.getCheckModel().checkAll();
        }
        applyRequiredSelectionOutline(checkComboBox);
    }

    /**
     * Configures a CheckComboBox title so it shows checked counts and switches
     * between an empty-state title and a selected-state title dynamically.
     *
     * @param checkComboBox check combo box to configure
     * @param emptyTitle title to show when no items are selected
     * @param selectedTitle title to show when one or more items are selected
     */
    protected void configureCheckComboBoxSelectionTitle(CheckComboBox<String> checkComboBox, String emptyTitle, String selectedTitle) {
        if (checkComboBox == null) {
            return;
        }

        if (!checkComboBox.getProperties().containsKey(REQUIRED_SELECTION_LISTENER_KEY)) {
            checkComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) change -> {
                int checkedCount = checkComboBox.getCheckModel().getCheckedItems().size();
                checkComboBox.setShowCheckedCount(checkedCount > 0);
                checkComboBox.setTitle(checkedCount > 0 ? selectedTitle : emptyTitle);
                applyRequiredSelectionOutline(checkComboBox);
            });
            checkComboBox.getProperties().put(REQUIRED_SELECTION_LISTENER_KEY, Boolean.TRUE);
        }

        int checkedCount = checkComboBox.getCheckModel().getCheckedItems().size();
        checkComboBox.setShowCheckedCount(checkedCount > 0);
        checkComboBox.setTitle(checkedCount > 0 ? selectedTitle : emptyTitle);
        applyRequiredSelectionOutline(checkComboBox);
    }

    private void ensureRequiredSelectionListener(ComboBox<String> comboBox) {
        if (comboBox.getProperties().containsKey(REQUIRED_SELECTION_LISTENER_KEY)) {
            return;
        }
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                applyRequiredSelectionOutline(comboBox));
        comboBox.getProperties().put(REQUIRED_SELECTION_LISTENER_KEY, Boolean.TRUE);
    }

    private String getBaseStyle(javafx.scene.control.Control control) {
        Object stored = control.getProperties().get(REQUIRED_SELECTION_BASE_STYLE_KEY);
        if (stored instanceof String) {
            return (String) stored;
        }
        String base = control.getStyle();
        if (base == null) {
            base = "";
        }
        control.getProperties().put(REQUIRED_SELECTION_BASE_STYLE_KEY, base);
        return base;
    }

    private void applyRequiredSelectionOutline(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        String base = getBaseStyle(comboBox);
        comboBox.setStyle(isSelectionMissing(comboBox) ? (base + " " + REQUIRED_SELECTION_SUBTLE_OUTLINE).trim() : base);
    }

    private void applyRequiredSelectionOutline(CheckComboBox<String> checkComboBox) {
        if (checkComboBox == null) {
            return;
        }
        boolean missing = isSelectionMissing(checkComboBox);
        String base = getBaseStyle(checkComboBox);
        checkComboBox.setStyle(missing ? (base + " " + REQUIRED_SELECTION_SUBTLE_OUTLINE).trim() : base);
        Platform.runLater(() -> clearCheckComboBoxInnerBorder(checkComboBox));
    }

    private void clearCheckComboBoxInnerBorder(CheckComboBox<String> checkComboBox) {
        if (checkComboBox == null) {
            return;
        }

        // Clear borders only on the inner editor/arrow nodes, NOT .combo-box-base
        // (that node hosts the outer widget border which must remain intact).
        for (javafx.scene.Node node : checkComboBox.lookupAll(".text-input")) {
            node.setStyle(CHECK_COMBOBOX_INNER_TEXT_CLEAR_STYLE);
        }
        for (javafx.scene.Node node : checkComboBox.lookupAll(".text-field")) {
            node.setStyle(CHECK_COMBOBOX_INNER_TEXT_CLEAR_STYLE);
        }
        for (javafx.scene.Node node : checkComboBox.lookupAll(".arrow-button")) {
            node.setStyle(CHECK_COMBOBOX_INNER_CHROME_CLEAR_STYLE);
        }
    }

    /**
     * Returns true when a ComboBox has no selected value.
     *
     * @param comboBox the combo box to inspect
     * @return true when the selected item is null or blank
     */
    protected boolean isSelectionMissing(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return true;
        }
        String selected = comboBox.getSelectionModel().getSelectedItem();
        return selected == null || selected.trim().isEmpty();
    }

    /**
     * Returns true when a CheckComboBox has no checked items.
     *
     * @param checkComboBox the check combo box to inspect
     * @return true when there are no checked items
     */
    protected boolean isSelectionMissing(CheckComboBox<String> checkComboBox) {
        return checkComboBox == null || checkComboBox.getCheckModel().getCheckedItems().isEmpty();
    }

    /**
     * Normalizes a string for use in auto-generated names.
     *
     * @param value raw value
     * @return sanitized name fragment
     */
    protected String normalizeNamePart(String value) {
        if (value == null) {
            return "--";
        }
        String normalized = value.trim()
        	    .replaceAll("[^a-zA-Z0-9_ -]", "_")
                .replaceAll(" ", "_")
                .replaceAll("-", "_");
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        return normalized.isEmpty() ? "--" : normalized;
    }

    /**
     * Removes placeholder selections like "Select One" from a tab's validation logic.
     *
     * @param value selected value
     * @param placeholder placeholder text to treat as missing
     * @return true when the value is missing or equal to the placeholder
     */
    protected boolean isMissingSelection(String value, String placeholder) {
        return value == null || value.trim().isEmpty() || (placeholder != null && placeholder.equals(value.trim()));
    }

    /**
     * Appends a region-selection validation message and returns 1 when invalid.
     */
    protected int validateRegionSelection(TreeView<String> tree, StringBuilder message) {
        if (tree == null || utils.getAllSelectedRegions(tree).length < 1) {
            message.append("Must select at least one region from tree").append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Validates table rows and table-year compatibility in one place.
     */
    protected int validateTableEntries(StringBuilder message, boolean hasRows, boolean yearsMatch) {
        if (!hasRows) {
            message.append("Data table must have at least one entry").append(vars.getEol());
            return 1;
        }
        if (!yearsMatch) {
            message.append("Years specified in table must match allowable policy years (")
                    .append(vars.getAllowablePolicyYears()).append(")")
                    .append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Validates required ComboBox selection with a standardized message.
     */
    protected int validateRequiredSelection(StringBuilder message, ComboBox<String> comboBox, String fieldName) {
        if (isSelectionMissing(comboBox)) {
            message.append(fieldName).append(" comboBox must have a selection").append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Validates required ComboBox selection and treats a placeholder value as missing.
     */
    protected int validateRequiredSelection(StringBuilder message, ComboBox<String> comboBox, String fieldName, String placeholder) {
        int errors = validateRequiredSelection(message, comboBox, fieldName);
        if (errors > 0) {
            return errors;
        }

        String selected = comboBox.getSelectionModel().getSelectedItem();
        if (isMissingSelection(selected, placeholder)) {
            message.append(fieldName).append(" comboBox must have a selection").append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Validates required CheckComboBox selection with a standardized message.
     */
    protected int validateRequiredSelection(StringBuilder message, CheckComboBox<String> checkComboBox, String fieldName) {
        if (isSelectionMissing(checkComboBox)) {
            message.append(fieldName).append(" checkComboBox must have at least one selection").append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Validates required TextField content with a standardized message.
     */
    protected int validateRequiredText(StringBuilder message, TextField textField, String fieldName) {
        if (textField == null || textField.getText() == null || textField.getText().trim().isEmpty()) {
            message.append("A ").append(fieldName).append(" must be provided").append(vars.getEol());
            return 1;
        }
        return 0;
    }

    /**
     * Emits QA warnings/errors and returns true when there are no validation errors.
     */
    protected boolean finalizeQaValidation(int errorCount, StringBuilder message) {
        if (errorCount > 0) {
            if (errorCount == 1) {
                utils.warningMessage(message.toString());
            } else {
                utils.displayString(message.toString(), "Parsing Errors");
            }
        }
        return errorCount == 0;
    }
    
    /**
     * Create a CheckComboBox for String values.
     * @return A new CheckComboBox<String>
     */
    protected CheckComboBox<String> createCheckComboBox() {
        CheckComboBox<String> checkComboBox = utils.createCheckComboBox();
        ensureCheckComboBoxInnerBorderCleanup(checkComboBox);
        return checkComboBox;
    }

    private void ensureCheckComboBoxInnerBorderCleanup(CheckComboBox<String> checkComboBox) {
        if (checkComboBox == null) {
            return;
        }
        checkComboBox.skinProperty().addListener((obs, oldSkin, newSkin) ->
                Platform.runLater(() -> clearCheckComboBoxInnerBorder(checkComboBox)));
        checkComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> clearCheckComboBoxInnerBorder(checkComboBox));
            }
        });
        Platform.runLater(() -> clearCheckComboBoxInnerBorder(checkComboBox));
    }
    /**
     * Create a CheckBox with specified text.
     * @param text The checkbox label
     * @return A new CheckBox
     */
    protected CheckBox createCheckBox(String text) {
        return utils.createCheckBox(text);
    }
    /**
     * Create a Button with specified text, width, and event handler.
     * @param text The button label
     * @param width The button width
     * @param handler The event handler (can be null)
     * @return A new Button
     */
    protected Button createButton(String text, int width, EventHandler<ActionEvent> handler) {
        Button button = utils.createButton(text, width, handler);
        if (handler != null) button.setOnAction(handler);
        return button;
    }
    /**
     * Standardized event handler registration for tab subclasses.
     * @param comboBox The ComboBox to set the handler for
     * @param handler The event handler to assign
     */
    protected void setOnAction(ComboBox<?> comboBox, EventHandler<ActionEvent> handler) {
        comboBox.setOnAction(handler);
    }
    /**
     * Set the action handler for a Button.
     * @param button The Button to set the handler for
     * @param handler The event handler to assign
     */
    protected void setOnAction(Button button, EventHandler<ActionEvent> handler) {
        button.setOnAction(handler);
    }
    /**
     * Set the action handler for a TextField.
     * @param textField The TextField to set the handler for
     * @param handler The event handler to assign
     */
    protected void setOnAction(TextField textField, EventHandler<ActionEvent> handler) {
        textField.setOnAction(handler);
    }
    /**
     * Set the mouse click handler for a Label.
     * @param label The Label to set the handler for
     * @param handler The mouse event handler to assign
     */
    protected void setOnMouseClicked(Label label, EventHandler<javafx.scene.input.MouseEvent> handler) {
        label.setOnMouseClicked(handler);
    }
    /**
     * Set the action handler for a CheckBox.
     * @param checkBox The CheckBox to set the handler for
     * @param handler The event handler to assign
     */
    protected void setOnAction(CheckBox checkBox, EventHandler<ActionEvent> handler) {
        checkBox.setOnAction(handler);
    }
    
    /**
     * Stub for subclasses to override to set policy and market names based on UI state.
     * <p>
     * This method should be overridden by subclasses if they need to update policy/market names dynamically.
     * </p>
     */
    protected void setPolicyAndMarketNames() {
    	//stub to be overridden by subclasses
    	return;
	}
    
    /**
     * Sets up event handlers for UI components in the tab.
     * <p>
     * This includes listeners for combo boxes, checkboxes, buttons, and filter fields.
     * All UI updates are wrapped in Platform.runLater for thread safety.
     * </p>
     */
    protected void setupEventHandlers() {
    	// Add event handler to update policy/market names when region tree changes
    	paneForCountryStateTree.getTree().addEventHandler(ActionEvent.ACTION, e -> {
    		setPolicyAndMarketNames();
    	});

        checkBoxUseAutoNames.setOnAction(e -> Platform.runLater(() -> {
            if (syncingSharedNameOptions) {
                return;
            }
            applySharedNameOptionsToAllTabs(checkBoxUseAutoNames.isSelected(), checkBoxUseUniqueNames.isSelected());
        }));

        checkBoxUseUniqueNames.setOnAction(e -> Platform.runLater(() -> {
            if (syncingSharedNameOptions) {
                return;
            }
            applySharedNameOptionsToAllTabs(checkBoxUseAutoNames.isSelected(), checkBoxUseUniqueNames.isSelected());
        }));
        if (!modificationTypeSelectionListenerAttached) {
            comboBoxModificationType.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                    Platform.runLater(() -> applyModificationTypeLabels(newValue)));
            modificationTypeSelectionListenerAttached = true;
        }
        Platform.runLater(this::refreshModificationTypeLabels);
        buttonClear.setOnAction(e -> Platform.runLater(() -> paneForComponentDetails.clearTable()));
        buttonDelete.setOnAction(e -> Platform.runLater(() -> paneForComponentDetails.deleteItemsFromTable()));
        buttonPopulate.setOnAction(e -> Platform.runLater(() -> {
        	if (qaPopulate()) {
                double[][] values = calculateValues();               
                paneForComponentDetails.setValues(values);
            } else {
				utils.warningMessage("Please fill in fields at bottom of left column to use populate button.");
			}
        }));
        buttonFill.setOnAction(e -> Platform.runLater(() -> {
        		//System.out.println("pressed buttonFill");
        		ArrayList<String> values = paneForComponentDetails.getDataYrValsArrayList();
        		
            	int startYear = Integer.parseInt(textFieldStartYear.getText());
				Integer.parseInt(vars.getStopYear());
				List<Integer> policyYears = vars.getAllowablePolicyYears();
        		
        		// case with empty list - fill with zeros for all policy years: Note: ignores endYear field
                if (values.size()==0) { 

					for (int year : policyYears) {
						if (year >= startYear) {
							values.add(year + ",0.0");
						}
					}
                } else {
					// case with existing values - fill in subsequent years with last value, using list of allowable policy years
                	int index = values.size()-1;
                	int finalYear = values.get(index).split(",").length > 1 ? Integer.parseInt(values.get(index).split(",")[0].trim()) : 0;
					double finalValue = values.get(index).split(",").length > 1 ? Double.parseDouble(values.get(index).split(",")[1].trim()) : 0;
				
					for (int year : policyYears) {
						if (year > finalYear) {
							String formattedValue = formatDisplayValue(finalValue);
							values.add(year + "," + formattedValue);
						}
					}
                }
                paneForComponentDetails.setValues(values);
        }));
    }

    /**
     * Formats numeric values with the shared significant-figures convention.
     */
    protected String formatDisplayValue(double value) {
        return utils.toSignificantFiguresString(value, vars.getValueDisplaySigFigs());
    }

    /**
     * Performs a quick QA check to ensure required fields for populating values are filled.
     * <p>
     * Checks that all required text fields for populating values are not empty.
     * </p>
     * @return true if all required fields are filled, false otherwise
     */
    public boolean qaPopulate() {
        return !(textFieldStartYear.getText().isEmpty() ||
                textFieldEndYear.getText().isEmpty() ||
                textFieldInitialAmount.getText().isEmpty() ||
                textFieldGrowth.getText().isEmpty());
    }

  /**
   * Returns true when the supplied market name contains additional text after the
   * "Mkt" token (for example, "myPolicy_Mkt12345").
   */
  protected boolean hasTextAfterMktToken(String marketName) {
    if (marketName == null) {
      return false;
    }
    int idx = marketName.lastIndexOf("Mkt");
    if (idx < 0) {
      return false;
    }
    String suffix = marketName.substring(idx + 3).trim();
    return !suffix.isEmpty();
  }

  /**
   * Removes a trailing numeric unique suffix that may be appended after the
   * final "Mkt" token when loading existing scenario components for editing.
   */
  protected String stripUniqueSuffixFromLoadedMarketName(String marketName) {
    if (marketName == null) {
      return null;
    }
    String trimmed = marketName.trim();
    int idx = trimmed.lastIndexOf("Mkt");
    if (idx < 0) {
      return trimmed;
    }
    String suffix = trimmed.substring(idx + 3).trim();
    if (suffix.matches("\\d+")) {
      return trimmed.substring(0, idx + 3);
    }
    return trimmed;
  }

  /**
   * Generates a unique suffix only when unique names are enabled and the current
   * market name does not already contain text after "Mkt".
   */
  protected String resolveUniqueSuffix(boolean uniqueNamesEnabled, String marketName) {
    if (!uniqueNamesEnabled) {
      return "";
    }
    if (hasTextAfterMktToken(marketName)) {
      return "";
    }
    return utils.getUniqueString();
  }

  /**
   * Generates a unique suffix for tabs that always use unique IDs unless the
   * current market name already includes a suffix after "Mkt".
   */
  protected String resolveUniqueSuffix(String marketName) {
    if (hasTextAfterMktToken(marketName)) {
      return "";
    }
    return utils.getUniqueString();
  }
    
	protected void initializeTempFiles() {
		/**
		 * Initializes temporary files and writers for saving large scenario components.
		 * <p>
		 * Creates temp directory and files, and initializes BufferedWriter instances for each temp file.
		 * </p>
		 */
		String tempDirName = vars.getGlimpseDir() + File.separator + "GLIMPSE-Data" + File.separator + "temp"; // vars.getGlimpseDir();

		File test = new File(tempDirName);
		if (!test.exists())
			test.mkdir();
		tempFilename0 = "temp_policy_file0.txt";
		tempFilename1 = "temp_policy_file1.txt";
		tempFilename2 = "temp_policy_file2.txt";

		temp_file0 = tempDirName + File.separator + tempFilename0;
		temp_file1 = tempDirName + File.separator + tempFilename1;
		temp_file2 = tempDirName + File.separator + tempFilename2;

		bw0 = files.initializeBufferedFile(tempDirName, tempFilename0);
		bw1 = files.initializeBufferedFile(tempDirName, tempFilename1);
		bw2 = files.initializeBufferedFile(tempDirName, tempFilename2);

		temp_file = tempDirName + File.separator + "temp_policy_file.txt";
		files.deleteFile(temp_file);
		

	}
    
    
}
