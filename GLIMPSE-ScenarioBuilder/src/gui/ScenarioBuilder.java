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
 * and that User is not otherwise prohibited
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
package gui;

import glimpseBuilder.SetupTableComponentLibrary;
import glimpseBuilder.SetupTableCreateScenario;
import glimpseBuilder.SetupTableScenariosLibrary;
import glimpseElement.ComponentLibraryTable;
import glimpseElement.ComponentRow;
import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * ScenarioBuilder is responsible for constructing the main user interface and core logic for the GLIMPSE Scenario Builder application.
 * <p>
 * This class initializes and manages all primary JavaFX panes, tables, buttons, and event handlers required for scenario creation, editing, and management.
 * It follows the singleton pattern to ensure a single instance throughout the application lifecycle.
 * <p>
 * <b>Main Features:</b>
 * <ul>
 *   <li>Initializes and lays out the component library, scenario creation, and scenario library panes.</li>
 *   <li>Configures and manages all major UI controls, including search fields, tables, and action buttons.</li>
 *   <li>Implements event handlers for adding/removing components, editing scenarios, and updating UI state.</li>
 *   <li>Integrates with GLIMPSE utility classes for consistent styling, file management, and variable access.</li>
 *   <li>Supports filtering and sorting of components and scenarios using JavaFX's observable collections.</li>
 *   <li>Provides utility methods for file type detection and dynamic UI resizing.</li>
 * </ul>
 * <p>
 * <b>Usage:</b> Call {@link #build()} to initialize and display the main panes and controls. Use the provided getters to access the main layout containers for embedding in the application scene.
 * <p>
 * <b>Thread Safety:</b> This class is not thread-safe and should be used only on the JavaFX Application Thread.
 * <p>
 * <b>Integration:</b>
 * <ul>
 *   <li>Works with {@link Client} for main application window and event handling.</li>
 *   <li>Uses {@link GLIMPSEVariables}, {@link GLIMPSEFiles}, {@link GLIMPSEStyles}, and {@link GLIMPSEUtils} for configuration and utility functions.</li>
 *   <li>Integrates with table setup classes (e.g., {@link SetupTableComponentLibrary}, {@link SetupTableCreateScenario}, {@link SetupTableScenariosLibrary}).</li>
 *   <li>Provides access to main UI panes for embedding in the application scene.</li>
 * </ul>
 */
public class ScenarioBuilder {

	// Constants for UI Labels and Tooltips
	private static final String LABEL_COMPONENT_LIBRARY = "Component Library";
	private static final String LABEL_CREATE_SCENARIO = "Create Scenario";
	private static final String LABEL_SCENARIO_LIBRARY = "Scenario Library";
	private static final String LABEL_SEARCH = "Search:";
	private static final String TOOLTIP_FILTER = "Enter text to begin filtering";
	private static final String TOOLTIP_REMOVE_SELECTED_COMPONENTS = "Remove selected component(s) from scenario";
	private static final String TOOLTIP_REMOVE_ALL_COMPONENTS = "Remove all components from scenario";
	private static final String TOOLTIP_ADD_SELECTED_COMPONENTS = "Add selected component(s) to scenario";
	private static final String TOOLTIP_EDIT_SCENARIO = "Edit: Move selected scenario from working list to scenario edit pane";

	// Constants for Internal Logic
	private static final String FILE_TYPE_XML = "xml";
	private static final String FILE_TYPE_PRESET = "preset";
	private static final String FILE_TYPE_INPUT_TABLE = "INPUT_TABLE";
	private static final String EXTERNALLY_CREATED_SCENARIO_PREFIX = "Externally-created scenario";

	// Singleton Instance
	public static final ScenarioBuilder instance = new ScenarioBuilder();

	// UI Panes
	protected VBox vBoxComponentLibrary;
	protected VBox vBoxCreateScenario;
	protected VBox vBoxButton;
	protected VBox vBoxRun;

	// UI Labels
	protected Label labelComponentLibrary;
	protected Label labelSearchComponentLibrary;
	protected Label labelSearchScenarios;
	protected Label labelScenarioLibrary;
	protected Label labelScenarioName;

	// GLIMPSE Utilities
	protected final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
	protected final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
	protected final GLIMPSEFiles files = GLIMPSEFiles.getInstance();
	protected final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

	/** Default CSS resource used for the modern look and feel. */
	private static final String MODERN_CSS_RESOURCE = "/resources/modern.css";

	// --- CSS style class names (for modern.css) ---
	private static final String STYLE_PANEL_CARD = "panel-card";
	private static final String STYLE_TOOLBAR = "toolbar";
	private static final String STYLE_SECTION_TITLE = "section-title";

	/**
	 * Returns the singleton instance of ScenarioBuilder.
	 *
	 * @return the singleton ScenarioBuilder instance
	 */
	public static ScenarioBuilder getInstance() {
		return instance;
	}

	/**
	 * Private constructor for singleton pattern. Prevents external instantiation.
	 */
	public ScenarioBuilder() {
		// Private constructor for singleton pattern
	}

	/**
	 * Initializes and builds all main UI panes, tables, and controls for the Scenario Builder.
	 * This method should be called once during application startup.
	 * Sets up all tables, buttons, panes, and resizes labels for consistent UI.
	 */
	public void build() {
		// Initialization of vars/files/utils is done during Client.init().
		// Avoid repeating it here to reduce startup cost and prevent reinitialization side effects.

		createTables();
		createArrowButtons();
		createComponentLibraryPane();
		createCreateScenarioPane();
		createScenarioLibraryPane();
		resizeLabels();
	}

	/**
	 * Creates and configures the main tables for the component library, scenario creation, and scenario library.
	 * Sets up tooltips and filtering for the component library table.
	 */
	private void createTables() {
		new SetupTableComponentLibrary().setup();
		ComponentLibraryTable.getFilterComponentsTextField().setTooltip(new Tooltip(TOOLTIP_FILTER));

		new SetupTableCreateScenario().setup();
		new SetupTableScenariosLibrary().setup();
	}

	/**
	 * Builds the component library pane, including the label, search field, and action buttons.
	 * Assembles the layout using HBox and VBox containers.
	 * Adds all relevant controls and sets style for the pane.
	 */
	private void createComponentLibraryPane() {
		labelComponentLibrary = utils.createLabel(LABEL_COMPONENT_LIBRARY/*, 1.7 * styles.getBigButtonWidth()*/);
		labelComponentLibrary.getStyleClass().add(STYLE_SECTION_TITLE);
		// Match the Scenario Library "titled border" behavior: give the title a background
		// so it can sit on top of the bordered panel without the border line bleeding through.
		// Use setters for padding/font so it applies regardless of CSS precedence.
		labelComponentLibrary.setStyle(labelComponentLibrary.getStyle() + "; -fx-background-color: -fx-control-inner-background;");
		labelComponentLibrary.setPadding(new Insets(0, 6, 6, 6));
		try {
			Font f = labelComponentLibrary.getFont();
			double size = (f != null) ? f.getSize() : 12.0;
			String family = (f != null) ? f.getFamily() : null;
			labelComponentLibrary.setFont(Font.font(family, FontWeight.BOLD, size));
		} catch (Exception ignored) {
			// If font can't be derived, fall back to CSS (still OK).
			labelComponentLibrary.setStyle(labelComponentLibrary.getStyle() + "; -fx-font-weight: bold;");
		}

		labelSearchComponentLibrary = utils.createLabel(LABEL_SEARCH);
		labelSearchComponentLibrary.setMinWidth(Region.USE_PREF_SIZE);

		HBox paneObjects = new HBox();
		paneObjects.getStyleClass().add(STYLE_TOOLBAR);
		paneObjects.setAlignment(Pos.CENTER_LEFT);
		paneObjects.setSpacing(4);

		Client.paneComponentLibrary = new PaneNewScenarioComponent();

		// Add all relevant controls to the component library pane
		paneObjects.getChildren().addAll(
			labelSearchComponentLibrary, 
			ComponentLibraryTable.getFilterComponentsTextField(),
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonNewComponent,
			Client.buttonEditComponent,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonBrowseComponentLibrary,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonDeleteComponent,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonRefreshComponents
		);

		// Build a bordered "card" and overlay the section title on its top border.
		VBox content = new VBox(4, paneObjects, Client.paneComponentLibrary.getvBox());
		StackPane titledPanel = new StackPane(content, labelComponentLibrary);
		StackPane.setAlignment(labelComponentLibrary, Pos.TOP_LEFT);
		labelComponentLibrary.setTranslateY(-8);
		StackPane.setMargin(labelComponentLibrary, new Insets(0, 0, 0, 10));
		labelComponentLibrary.toFront();

		vBoxComponentLibrary = new VBox(0, titledPanel);
		vBoxComponentLibrary.getStyleClass().add(STYLE_PANEL_CARD);
		vBoxComponentLibrary.setStyle(styles.getStyle1());
	}

	/**
	 * Builds the scenario creation pane, including the scenario name label and scenario creation controls.
	 * Uses VBox for vertical layout and sets style for the pane.
	 */
	private void createCreateScenarioPane() {
		labelScenarioName = utils.createLabel(LABEL_CREATE_SCENARIO, 2 * styles.getBigButtonWidth());
		labelScenarioName.getStyleClass().add(STYLE_SECTION_TITLE);
		Client.paneCreateScenario = new PaneCreateScenario(Client.primaryStage);

		vBoxCreateScenario = new VBox(4, Client.paneCreateScenario.getvBox());
		vBoxCreateScenario.getStyleClass().add(STYLE_PANEL_CARD);
		vBoxCreateScenario.setStyle(styles.getStyle1());
	}

	/**
	 * Builds the scenario library pane, including the scenario library label, search field, and action buttons.
	 * Configures filtering and sorting for the scenario table and sets style for the pane.
	 */
	private void createScenarioLibraryPane() {
		labelScenarioLibrary = utils.createLabel(LABEL_SCENARIO_LIBRARY/*, styles.getBigButtonWidth() * 1.75*/);
		labelScenarioLibrary.getStyleClass().add(STYLE_SECTION_TITLE);
		// Give the title a matching background so it visually "breaks" the border line behind it.
		// (If modern.css provides a background already, this is harmless.)
		labelScenarioLibrary.setStyle(labelScenarioLibrary.getStyle() + "; -fx-background-color: -fx-control-inner-background; -fx-padding: 0 6 0 6;");

		TextField filterScenarioTextField = utils.createTextField();
		filterScenarioTextField.setMinWidth(styles.getBigButtonWidth());
		filterScenarioTextField.setPrefWidth(styles.getBigButtonWidth() * 1.75);
		filterScenarioTextField.setTooltip(new Tooltip(TOOLTIP_FILTER));
		filterScenarioTextField.setPromptText("Filter scenarios...");

		// Set up filtered and sorted lists for scenario table
		ScenarioTable.filteredScenarios = new FilteredList<>(ScenarioTable.tableScenariosLibrary.getItems(), p -> true);

		filterScenarioTextField.textProperty().addListener((observable, oldValue, newValue) ->
			ScenarioTable.filteredScenarios.setPredicate(scenarioRow -> {
				if (newValue == null || newValue.isEmpty()) {
					return true;
				}
				String lowerCaseFilter = newValue.toLowerCase();
				return scenarioRow.getScenarioName().toLowerCase().contains(lowerCaseFilter);
			})
		);

		SortedList<ScenarioRow> sortedScenarios = new SortedList<>(ScenarioTable.filteredScenarios);
		sortedScenarios.comparatorProperty().bind(ScenarioTable.tableScenariosLibrary.comparatorProperty());
		ScenarioTable.tableScenariosLibrary.setItems(sortedScenarios);

		Client.paneScenarioLibrary = new PaneScenarioLibrary(Client.primaryStage);

		// Ensure the inner scenario table doesn't draw its own border when inside a panel.
		try {
			ScenarioTable.tableScenariosLibrary.getStyleClass().add("no-inner-border");
		} catch (Exception ignored) {}

		HBox buttonHBox = new HBox();
		buttonHBox.setAlignment(Pos.CENTER_LEFT);
		buttonHBox.setSpacing(4);
		buttonHBox.getStyleClass().add(STYLE_TOOLBAR);

		labelSearchScenarios = utils.createLabel(LABEL_SEARCH/*, styles.getBigButtonWidth()*/);
		labelSearchScenarios.setTextAlignment(TextAlignment.LEFT);

		// Add all relevant controls to the scenario library pane
		buttonHBox.getChildren().addAll(
			labelSearchScenarios, 
			filterScenarioTextField,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonEditScenario,
			Client.buttonViewConfig,
			Client.buttonBrowseScenarioFolder,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonRunScenario,
			Client.buttonStopScenario,
			Client.buttonDeleteScenario, 
			utils.getSeparator(Orientation.VERTICAL, 3, false),
			Client.buttonResults, 
			Client.buttonResultsForSelected,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonDiffFiles,
			Client.buttonShowRunQueue, 
			utils.getSeparator(Orientation.VERTICAL, 3, false),
			Client.buttonViewExeLog, Client.buttonViewExeErrors, 
			Client.buttonViewLog, 
			Client.buttonViewErrors,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonRefreshScenarioStatus,
			utils.getSeparator(Orientation.VERTICAL, 3, false), 
			Client.buttonConsole
		);

		// Build a bordered "card" and overlay the section title on its top border.
		// This avoids clipping/truncation that can happen when the title is inside the toolbar HBox.
		HBox toolbarRow = new HBox(6, buttonHBox);
		toolbarRow.setAlignment(Pos.CENTER_LEFT);
		toolbarRow.getStyleClass().add(STYLE_TOOLBAR);

		VBox content = new VBox(4, toolbarRow, Client.paneScenarioLibrary.gethBox());
		StackPane titledPanel = new StackPane(content, labelScenarioLibrary);
		StackPane.setAlignment(labelScenarioLibrary, Pos.TOP_LEFT);
		// Nudge the title upward so it sits on the border line.
		labelScenarioLibrary.setTranslateY(-8);
		StackPane.setMargin(labelScenarioLibrary, new Insets(0, 0, 0, 10));

		vBoxRun = new VBox(0, titledPanel);
		vBoxRun.getStyleClass().add(STYLE_PANEL_CARD);
		// Don't use legacy style1 here: it adds the blue border around the whole Scenario Library pane.
		vBoxRun.setStyle("");
	}

	/**
	 * Creates and configures the arrow buttons for moving components between lists and editing scenarios.
	 * Sets up tooltips, disables by default, and assigns event handlers for each button.
	 */
	private void createArrowButtons() {
		Client.buttonLeftArrow = utils.createButton(null, styles.getBigButtonWidth(), TOOLTIP_REMOVE_SELECTED_COMPONENTS, "left_arrow");
		Client.buttonLeftArrow.setDisable(true);
		Client.buttonLeftArrow.setOnAction(this::removeSelectedComponents);

		Client.buttonLeftDoubleArrow = utils.createButton(null, styles.getBigButtonWidth(), TOOLTIP_REMOVE_ALL_COMPONENTS, "double_left_arrow");
		Client.buttonLeftDoubleArrow.setDisable(true);
		Client.buttonLeftDoubleArrow.setOnAction(this::removeAllComponents);

		Client.buttonRightArrow = utils.createButton(null, styles.getBigButtonWidth(), TOOLTIP_ADD_SELECTED_COMPONENTS, "right_arrow");
		Client.buttonRightArrow.setDisable(true);
		Client.buttonRightArrow.setOnAction(this::addSelectedComponents);

		Client.buttonEditScenario = utils.createButton("Edit", styles.getBigButtonWidth(), TOOLTIP_EDIT_SCENARIO, "up_right_arrow");
		Client.buttonEditScenario.setDisable(true);
		Client.buttonEditScenario.setOnAction(this::loadSelectedScenarioForEditing);

		vBoxButton = new VBox(5, Client.buttonRightArrow, Client.buttonLeftArrow, Client.buttonLeftDoubleArrow);
		vBoxButton.setAlignment(Pos.CENTER);
		vBoxButton.prefWidthProperty().bind(Client.primaryStage.widthProperty().multiply(0.5 / 7.0));
	}

	// --- Event Handlers for Buttons ---

	/**
	 * Removes the selected components from the scenario creation list.
	 * Updates the status of arrow and action buttons.
	 *
	 * @param event the ActionEvent triggered by the button
	 */
	private void removeSelectedComponents(ActionEvent event) {
		ObservableList<ComponentRow> selectedItems = ComponentLibraryTable.getTableCreateScenario().getSelectionModel().getSelectedItems();
		ComponentLibraryTable.removeFromListOfFilesCreatePolicyScenario(selectedItems);
		setArrowAndButtonStatus();
	}

	/**
	 * Removes all components from the scenario creation list.
	 * Updates the status of arrow and action buttons.
	 *
	 * @param event the ActionEvent triggered by the button
	 */
	private void removeAllComponents(ActionEvent event) {
		ObservableList<ComponentRow> allItems = ComponentLibraryTable.getTableCreateScenario().getItems();
		ComponentLibraryTable.removeFromListOfFilesCreatePolicyScenario(allItems);
		setArrowAndButtonStatus();
	}

	/**
	 * Adds the selected components from the component library to the scenario creation list.
	 * Updates the status of arrow and action buttons.
	 *
	 * @param event the ActionEvent triggered by the button
	 */
	private void addSelectedComponents(ActionEvent event) {
		ObservableList<ComponentRow> selectedItems = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItems();
		ComponentLibraryTable.addToListOfFilesCreatePolicyScenario(selectedItems);
		setArrowAndButtonStatus();
	}

	/**
	 * Loads the selected scenario from the scenario library for editing in the scenario creation pane.
	 * If the scenario was created externally, editing is not allowed.
	 * Updates the scenario name and component list in the creation pane.
	 *
	 * @param event the ActionEvent triggered by the button
	 */
	private void loadSelectedScenarioForEditing(ActionEvent event) {
		ObservableList<ScenarioRow> selectedScenarios = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
		if (selectedScenarios.size() != 1) {
			return;
		}

		ScenarioRow selectedScenario = selectedScenarios.get(0);
		String scenarioName = selectedScenario.getScenarioName().trim();
		String components = selectedScenario.getComponents().trim();

		if (components.startsWith(EXTERNALLY_CREATED_SCENARIO_PREFIX)) {
			utils.showInformationDialog("Information", "Function not supported.", "Cannot modify scenario components in a scenario created outside of the ScenarioBuilder.");
			return;
		}

		Client.paneCreateScenario.setScenarioName(scenarioName);
		Client.buttonCreateScenarioConfigFile.setDisable(false);

		if (components.endsWith(";")) {
			components = components.substring(0, components.length() - 1);
		}

		ComponentRow[] componentRows = new ComponentRow[0];
		if (!components.isEmpty()) {
			// Split the components string and create ComponentRow objects for each filename
			componentRows = Arrays.stream(components.split(";"))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.map(this::createComponentRowFromFilename)
				.toArray(ComponentRow[]::new);
		}

		try {
			ComponentLibraryTable.createListOfFilesCreatePolicyScenario(componentRows);
		} catch (Exception e) {
			utils.warningMessage("Problem trying to modify list order.");
			System.err.println("Non-fatal error when adding files to list: " + e);
			System.err.println("Attempting to continue...");
		}

		setArrowAndButtonStatus();
	}

	/**
	 * Creates a ComponentRow object from a given filename, using the scenario components directory and file's last modified date.
	 *
	 * @param filename the name of the component file
	 * @return a new ComponentRow representing the file
	 */
	private ComponentRow createComponentRowFromFilename(String filename) {
		String fullFilename = vars.getScenarioComponentsDir() + File.separator + filename;
		File file = new File(fullFilename);
		Date lastModified = new Date(file.lastModified());
		return new ComponentRow(filename, fullFilename, lastModified);
	}

	/**
	 * Resizes all main labels in the UI to fit their content and style requirements.
	 * Uses utility method to resize each label for consistent appearance.
	 */
	private void resizeLabels() {
		labelComponentLibrary = utils.resizeLabelText(labelComponentLibrary);
		labelSearchComponentLibrary = utils.resizeLabelText(labelSearchComponentLibrary);
		labelSearchScenarios = utils.resizeLabelText(labelSearchScenarios);
		labelScenarioLibrary = utils.resizeLabelText(labelScenarioLibrary);
		labelScenarioName = utils.resizeLabelText(labelScenarioName);
		// resizeLabelText() can overwrite font/style, so re-apply any custom tweaks.
		applyComponentLibraryTitleTweaks();
		applyScenarioLibraryTitleTweaks();
		applyCreateScenarioTitleTweaks();
	}

	/**
	 * Re-applies custom styling that must survive calls to utils.resizeLabelText(),
	 * which resets label font and style.
	 */
	private void applyComponentLibraryTitleTweaks() {
		if (labelComponentLibrary == null) {
			return;
		}
		// Ensure the title still has the border-break background patch.
		labelComponentLibrary.setStyle(labelComponentLibrary.getStyle()
				+ "; -fx-background-color: -fx-control-inner-background;"
		);
		// Add breathing room below the title.
		labelComponentLibrary.setPadding(new Insets(0, 6, 6, 6));
		// Force bold without changing the computed size.
		try {
			Font f = labelComponentLibrary.getFont();
			double size = (f != null) ? f.getSize() : 12.0;
			String family = (f != null) ? f.getFamily() : null;
			labelComponentLibrary.setFont(Font.font(family, FontWeight.BOLD, size));
		} catch (Exception ignored) {
			labelComponentLibrary.setStyle(labelComponentLibrary.getStyle() + "; -fx-font-weight: bold;");
		}
	}

	private void applyScenarioLibraryTitleTweaks() {
		if (labelScenarioLibrary == null) {
			return;
		}
		labelScenarioLibrary.setStyle(labelScenarioLibrary.getStyle()
				+ "; -fx-background-color: -fx-control-inner-background;"
		);
		labelScenarioLibrary.setPadding(new Insets(0, 6, 6, 6));
		try {
			Font f = labelScenarioLibrary.getFont();
			double size = (f != null) ? f.getSize() : 12.0;
			String family = (f != null) ? f.getFamily() : null;
			labelScenarioLibrary.setFont(Font.font(family, FontWeight.BOLD, size));
		} catch (Exception ignored) {
			labelScenarioLibrary.setStyle(labelScenarioLibrary.getStyle() + "; -fx-font-weight: bold;");
		}
		labelScenarioLibrary.toFront();
	}

	private void applyCreateScenarioTitleTweaks() {
		if (labelScenarioName == null) {
			return;
		}
		labelScenarioName.setStyle(labelScenarioName.getStyle()
				+ "; -fx-background-color: -fx-control-inner-background;"
		);
		labelScenarioName.setPadding(new Insets(0, 6, 6, 6));
		try {
			Font f = labelScenarioName.getFont();
			double size = (f != null) ? f.getSize() : 12.0;
			String family = (f != null) ? f.getFamily() : null;
			labelScenarioName.setFont(Font.font(family, FontWeight.BOLD, size));
		} catch (Exception ignored) {
			labelScenarioName.setStyle(labelScenarioName.getStyle() + "; -fx-font-weight: bold;");
		}
	}

	/**
	 * Determines the file type of a given filename by checking its extension and content.
	 * If the file ends with .xml, returns "xml". Otherwise, scans the file for a type string or input table marker.
	 * Returns "preset" if no type is found.
	 *
	 * @param filename   the file to check
	 * @param typeString the string to search for in the file
	 * @return the file type as a string (e.g., "xml", "preset", or "INPUT_TABLE")
	 */
	protected String getFileType(String filename, String typeString) {
		if (filename.endsWith(".xml")) {
			return FILE_TYPE_XML;
		}

		try (Stream<String> lines = Files.lines(Paths.get(filename))) {
			Optional<String> componentType = lines.map(String::trim)
				.filter(line -> line.contains(typeString) || line.contains(FILE_TYPE_INPUT_TABLE))
				.map(line -> {
					if (line.contains(typeString)) {
						return line.substring(line.lastIndexOf("=") + 1).trim();
					} else {
						return FILE_TYPE_INPUT_TABLE;
					}
				})
				.findFirst();

			if (componentType.isPresent()) {
				return componentType.get();
			}

		} catch (IOException e) {
			utils.warningMessage("Problem reading component file " + filename + " to determine type.");
			System.err.println("Error reading scenario component file to determine type: " + e);
		}

		System.err.println("File does not include " + typeString + "=. Assuming file of type preset.");
		return FILE_TYPE_PRESET;
	}

	/**
	 * Updates the enabled/disabled status of all major arrow and action buttons based on current selection state.
	 * This method should be called after any change in selection or list contents.
	 * Enables/disables buttons for scenario library, scenario creation, and component library as appropriate.
	 */
	protected void setArrowAndButtonStatus() {
		int numSelectedScenarios = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems().size();
		int numSelectedCreate = ComponentLibraryTable.getTableCreateScenario().getSelectionModel().getSelectedItems().size();
		int numSelectedCandidate = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItems().size();

		// Scenario Library buttons
		boolean hasScenariosSelected = numSelectedScenarios >= 1;
		Client.buttonBrowseScenarioFolder.setDisable(!hasScenariosSelected);
		Client.buttonDeleteScenario.setDisable(!hasScenariosSelected);
		Client.buttonViewConfig.setDisable(!hasScenariosSelected);
		Client.buttonArchiveScenario.setDisable(!hasScenariosSelected);
		Client.buttonViewLog.setDisable(!hasScenariosSelected);
		Client.buttonViewErrors.setDisable(!hasScenariosSelected);
		Client.buttonRunScenario.setDisable(!hasScenariosSelected);
		Client.buttonEditScenario.setDisable(numSelectedScenarios != 1);
		Client.buttonResultsForSelected.setDisable(numSelectedScenarios != 1);
		Client.buttonDiffFiles.setDisable(numSelectedScenarios != 2);

		// Create Scenario (middle) buttons
		Client.buttonLeftDoubleArrow.setDisable(ComponentLibraryTable.getTableCreateScenario().getItems().isEmpty());
		boolean hasCreateItemsSelected = numSelectedCreate >= 1;
		Client.buttonLeftArrow.setDisable(!hasCreateItemsSelected);
		Client.buttonMoveComponentUp.setDisable(!hasCreateItemsSelected);
		Client.buttonMoveComponentDown.setDisable(!hasCreateItemsSelected);
		boolean hasScenarioName = Client.paneCreateScenario.getTextFieldScenarioName().getText().length() > 0;
		Client.buttonCreateScenarioConfigFile.setDisable(!hasScenarioName);

		// Component Library (candidate) buttons
		boolean hasCandidatesSelected = numSelectedCandidate >= 1;
		Client.buttonRightArrow.setDisable(!hasCandidatesSelected);
		Client.buttonDeleteComponent.setDisable(!hasCandidatesSelected);
		Client.buttonEditComponent.setDisable(numSelectedCandidate != 1);
	}


	// --- Getters for main layout panes ---

	/**
	 * Returns the VBox containing the component library pane.
	 *
	 * @return VBox for the component library
	 */
	public VBox getvBoxComponentLibrary() {
		return vBoxComponentLibrary;
	}

	/**
	 * Returns the VBox containing the scenario creation pane.
	 *
	 * @return VBox for scenario creation
	 */
	public VBox getvBoxCreateScenario() {
		return vBoxCreateScenario;
	}

	/**
	 * Returns the VBox containing the arrow buttons for moving components.
	 *
	 * @return VBox for arrow buttons
	 */
	public VBox getvBoxButton() {
		return vBoxButton;
	}

	/**
	 * Returns the VBox containing the scenario library pane and controls.
	 *
	 * @return VBox for scenario library and controls
	 */
	public VBox getvBoxRun() {
		return vBoxRun;
	}
	
	/**
	 * Updates the run status for all scenarios by delegating to paneScenarioLibrary.
	 * Should be called after scenario status changes to refresh the UI.
	 */
	public void updateTables() {
		if (Client.paneScenarioLibrary != null) {
			Client.paneScenarioLibrary.updateRunStatus();
		}
	}

	/**
	 * Applies the application's modern stylesheet to the provided Scene (if available).
	 *
	 * @param scene Scene to style
	 */
	public static void applyModernTheme(Scene scene) {
		if (scene == null)
			return;
		try {
			java.net.URL cssUrl = ScenarioBuilder.class.getResource(getModernCssResource());
			if (cssUrl != null) {
				String css = cssUrl.toExternalForm();
				if (!scene.getStylesheets().contains(css)) {
					scene.getStylesheets().add(css);
				}
			}
		} catch (Exception ex) {
			// ignore stylesheet load failures and fall back to default styles
		}
	}

	/**
	 * Creates and returns a dialog Stage configured with standardized styling and modality.
	 *
	 * @param title  dialog title
	 * @param width  preferred width
	 * @param height preferred height
	 * @return configured Stage
	 */
	protected Stage createDialogStage(String title, int width, int height) {
		Stage stage = new Stage();
		stage.setTitle(title);
		stage.setWidth(width);
		stage.setHeight(height);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setResizable(false);
		stage.setAlwaysOnTop(true);
		return stage;
	}

	/**
	 * Creates a dialog button with the application's standard size and style.
	 *
	 * <p>Subclasses should use this method whenever they need to create buttons inside
	 * dialogs or panels, rather than calling {@code utils.createButton} directly.
	 * This ensures consistent sizing (governed by {@code styles.getBigButtonWidth()})
	 * and appearance across the entire UI. Bypass this method only when a button
	 * genuinely requires non-standard dimensions or styling that cannot be achieved
	 * through the shared style system.
	 *
	 * <p>Subclasses may override this method to apply additional or alternative
	 * styling, but overrides should still delegate to the parent implementation
	 * (via {@code super.createDialogButton(text)}) unless a fundamentally different
	 * button type is required.
	 *
	 * @param text the label to display on the button
	 * @return a {@link javafx.scene.control.Button} styled with the application's
	 *         standard dialog-button dimensions
	 */
	protected javafx.scene.control.Button createDialogButton(String text) {
		return utils.createButton(text, styles.getBigButtonWidth(), null);
	}

	public static String getModernCssResource() {
		return MODERN_CSS_RESOURCE;
	}
}
