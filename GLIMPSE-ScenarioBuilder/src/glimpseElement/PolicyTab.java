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

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
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
    protected static final double LABEL_WIDTH = 125;
    protected static final double MAX_WIDTH = 175;
    protected static final double MIN_WIDTH = 105;
    protected static final double PREF_WIDTH = 175;
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
    protected static final String LABEL_POLICY_NAME = "Policy: ";
    protected static final String LABEL_MARKET_NAME = "Market: ";
    protected static final String LABEL_USE_AUTO_NAMES = "Names: ";
    protected static final String LABEL_MODIFICATION_TYPE = "Type: ";
    protected static final String LABEL_VALUES = "Values: ";
    protected static final String CHECKBOX_AUTO = "Auto";
    protected static final String CHECKBOX_UNIQUE = "Unique";
    
    // === Constants for default values and labels ===
    protected static final String[] MODIFICATION_TYPE_OPTIONS = {
            "Initial w/% Growth/yr", "Initial w/% Growth/pd",
            "Initial w/Delta/yr", "Initial w/Delta/pd", "Initial and Final"
    };
    protected static final String[] CONVERT_FROM_OPTIONS = {
            NONE, "2023$s", "2020$s", "2015$s", "2010$s", "2005$s", "2000$s"
    };
        
    // GUI elements shared across all policy tabs
    protected final Label labelStartYear = utils.createLabel("Start Year: ", LABEL_WIDTH);
    protected final TextField textFieldStartYear = new TextField(DEFAULT_START_YEAR);
    protected final Label labelEndYear = utils.createLabel("End Year: ", LABEL_WIDTH);
    protected final TextField textFieldEndYear = new TextField(DEFAULT_END_YEAR);
    protected final Label labelInitialAmount = utils.createLabel("Initial Val:   ", LABEL_WIDTH);
    protected final TextField textFieldInitialAmount = utils.createTextField();
    protected final Label labelGrowth = utils.createLabel("Growth (%): ", LABEL_WIDTH);
    protected final TextField textFieldGrowth = utils.createTextField();
    protected final Label labelPeriodLength = utils.createLabel("Period Length: ", LABEL_WIDTH);
    protected final TextField textFieldPeriodLength = new TextField(DEFAULT_PERIOD_LENGTH);
    protected final Label labelConvertFrom = utils.createLabel("Convert $s from: ", LABEL_WIDTH);
    protected final ComboBox<String> comboBoxConvertFrom = utils.createComboBoxString(CONVERT_FROM_OPTIONS);	
    protected final Label labelModificationType = utils.createLabel("Type: ", LABEL_WIDTH);
    protected final ComboBox<String> comboBoxModificationType = utils.createComboBoxString(MODIFICATION_TYPE_OPTIONS);
    protected final Label labelUnits2 = utils.createLabel(LABEL_UNITS_DEFAULT, 225.);

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
    
    protected final Label labelPolicyName = createLabel(LABEL_POLICY_NAME, LABEL_WIDTH);
    protected final TextField textFieldPolicyName = createTextField(PREF_WIDTH);
    protected final Label labelMarketName = createLabel(LABEL_MARKET_NAME, LABEL_WIDTH);
    protected final TextField textFieldMarketName = createTextField(PREF_WIDTH);
    //protected final Label labelUseAutoNames = createLabel(LABEL_USE_AUTO_NAMES, LABEL_WIDTH);
    //protected final Label labelUseUniqueNames = createLabel(CHECKBOX_UNIQUE, LABEL_WIDTH);
    protected final CheckBox checkBoxUseAutoNames = createCheckBox(CHECKBOX_AUTO);
    protected final CheckBox checkBoxUseUniqueNames = createCheckBox(CHECKBOX_UNIQUE);
    protected final Label labelValue = createLabel(LABEL_VALUES);

    
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
        textField.setPrefWidth(width);
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
        checkBox.setPrefWidth(width);
        return checkBox;
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
        comboBox.setPrefWidth(width);
        return comboBox;
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
        gridPaneLeft.setPrefWidth(325);
        gridPaneLeft.setMinWidth(325);
        vBoxCenter.setPrefWidth(300);
        vBoxRight.setPrefWidth(300);
		VBox tabLayout = new VBox();
		tabLayout.getChildren().addAll(gridPanePresetModification);
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
		comboBox.setPrefWidth(prefWidth);
		return comboBox;
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
        Platform.runLater(() -> getProgressBar().setProgress(progress));
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
    	hBoxHeaderCenter.setSpacing(2.);
    	hBoxHeaderCenter.setStyle(styles.getStyle3());
    	vBoxCenter.getChildren().clear();
    	vBoxCenter.getChildren().addAll(labelValue, hBoxHeaderCenter, paneForComponentDetails);
    	vBoxCenter.setStyle(styles.getStyle2());
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
        vBoxRight.getChildren().addAll(paneForCountryStateTree);
        vBoxRight.setStyle(styles.getStyle2());
        scrollPaneRight.setContent(vBoxRight);
	}
    
    /**
     * Get the file content for the scenario component.
     * @return File content string, or null if not set
     */
    public String getFileContent() {
        if (fileContent == null) {
            System.out.println("File content is null.");
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
        Platform.runLater(() -> getProgressBar().setProgress(0.0));
    }

    /**
     * Protected constructor for subclassing. Prevents direct instantiation.
     * <p>
     * Subclasses should call this constructor.
     * </p>
     */
    protected PolicyTab() {
        // No-op constructor for subclassing
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
        comboBox.setPrefWidth(width);
        return comboBox;
    }
    
    /**
     * Create a CheckComboBox for String values.
     * @return A new CheckComboBox<String>
     */
    protected CheckComboBox<String> createCheckComboBox() {
        return utils.createCheckComboBox();
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
            boolean selected = checkBoxUseAutoNames.isSelected();
            textFieldPolicyName.setDisable(selected);
            textFieldMarketName.setDisable(selected);
        }));
        comboBoxModificationType.setOnAction(e -> Platform.runLater(() -> {
            String selected = comboBoxModificationType.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            switch (selected) {
                case "Initial w/% Growth/yr":
                case "Initial w/% Growth/pd":
                	labelInitialAmount.setText("Initial Val:");
                	labelGrowth.setText("Growth (%):");
                    break;
                case "Initial w/Delta/yr":
                case "Initial w/Delta/pd":
                	labelInitialAmount.setText("Initial Val:");
                	labelGrowth.setText("Delta:");
                    break;
                case "Initial and Final":
                	labelInitialAmount.setText("Initial Val:");
                    labelGrowth.setText("Final Val:");
                    break;
                case "Initial and Final %":
                	labelInitialAmount.setText("Initial Val (%):");
                    labelGrowth.setText("Final Val (%):");
                    break;
            }
        }));
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
				int endYear = Integer.parseInt(vars.getStopYear());
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
							values.add(year + "," + finalValue);
						}
					}										
                }
                paneForComponentDetails.setValues(values);
        }));
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