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
package gui;

import static javafx.stage.Modality.APPLICATION_MODAL;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import glimpseElement.PolicyTab;
import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseElement.TabCafeStd;
import glimpseElement.ComponentRow;
import glimpseElement.TabMarketShare;
import glimpseElement.TabFuelPriceAdj;
import glimpseElement.ComponentLibraryTable;
import glimpseElement.TabXMLList;
import glimpseElement.TabPollutantTaxCap;
import glimpseElement.TabTechTax;
import glimpseUtil.FileChooserPlus;
import glimpseElement.TabTechAvailable;
import glimpseElement.TabTechBound;
import glimpseElement.TabTechBound;
import glimpseElement.TabTechParam;
import glimpseElement.TabFixedDemand;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * PaneNewScenarioComponent generates the top-left pane for scenario component management in the GLIMPSE Scenario Builder.
 * <p>
 * This class provides the user interface and logic for creating, editing, deleting, and saving scenario components.
 * It manages the component library table, handles user actions, and coordinates the display and persistence of scenario component files.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Displays a table of available scenario components, allowing users to browse, refresh, and manage components.</li>
 *   <li>Supports creation of new scenario components via a dialog with multiple specialized tabs (e.g., Market Share, Tech Bound, XML List, etc.).</li>
 *   <li>Allows editing of existing components, with logic to determine the correct editor based on file type and content.</li>
 *   <li>Enables deletion of components, with safeguards to prevent deletion if a component is in use by a scenario.</li>
 *   <li>Handles file operations for saving, moving, and loading scenario component files.</li>
 *   <li>Integrates with the GLIMPSE Scenario Builder's main application window and utilities.</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <ul>
 *   <li>Instantiate this class to provide the scenario component management pane in the Scenario Builder UI.</li>
 *   <li>Interact with the provided VBox via {@link #getvBox()} to embed in the main application layout.</li>
 * </ul>
 * <p>
 * <b>Dependencies:</b>
 * <ul>
 *   <li>Relies on various GLIMPSE custom classes for tabs, file utilities, and data models (e.g., {@link ComponentLibraryTable}, {@link PolicyTab}, {@link TabTechAvailable}).</li>
 *   <li>Uses JavaFX for UI components and event handling.</li>
 * </ul>
 *
 */
public class PaneNewScenarioComponent extends gui.ScenarioBuilder {

	// === Constants ===
	private static final String TAB_MARKET_SHARE = "Market Share";
	private static final String TAB_FLEX_SHARE = "Flex Share";
	private static final String TAB_MPG_TARGET = "MPG Target";
	//private static final String TAB_TECH_BOUND = "Tech Bound";
	private static final String TAB_TECH_BOUND2 = "Tech Bound";
	private static final String TAB_TECH_AVAIL = "Tech Avail";
	private static final String TAB_TECH_PARAM = "Tech Param";
	private static final String TAB_TECH_TAX = "Tech Tax/Subsidy";
	private static final String TAB_XML_LIST = "XML List";
	private static final String TAB_POLLUTANT_TAX_CAP = "Pollutant Tax/Cap";
	private static final String TAB_FUEL_PRICE_ADJ = "Fuel Price Adj";
	private static final String TAB_FIXED_DEMAND = "Fixed Demand";
	private static final String BUTTON_LABEL_NEW = "New";
	private static final String BUTTON_LABEL_EDIT = "Edit";
	private static final String BUTTON_LABEL_BROWSE = "Browse";
	private static final String BUTTON_LABEL_DELETE = "Delete";
	private static final String BUTTON_LABEL_REFRESH = "Refresh";
	private static final String BUTTON_LABEL_SAVE = "Save";
	private static final String BUTTON_LABEL_CLOSE = "Close";
	private static final String DIALOG_TITLE_NEW_COMPONENT = "New Scenario Component Creator";
	private static final String FILE_FILTER_TXT = "TXT files (*.txt)";
	private static final String FILE_FILTER_CSV = "CSV files (*.csv)";
	private static final String FILE_EXT_TXT = "txt";
	private static final String FILE_EXT_CSV = "csv";
	private static final String XML_LIST_KEYWORD = "xmllist";
	private static final String TEMP_POLICY_FILENAME = "temp_policy_file.txt";
	private static final String ERROR_LOADING_COMPONENTS = "Problem loading scenario component files.";
	private static final String ERROR_MOVING_FILE = "Problem moving file ";
	private static final String ERROR_CREATING_POLICY_FILE = "Error creating policy file: ";
	private static final String INFO_EDIT_REQUIRES_ONE = "Editing requires exactly one scenario component to be selected.";
	private static final String INFO_UNSUPPORTED_ACTION = "Unsupported action";
	private static final String WARNING_COMPONENT_USED = "Cannot delete selected scenario component since it is used in a scenario.";
	private static final String WARNING_UNKNOWN_TAB = "Unknown tab selected: ";
	private static final String WARNING_PROCESS_CANCELLED = "Process of building scenario component cancelled.";
	private static final String WARNING_PROCESS_FAILED = "Process of building scenario component failed.";

	// === UI Components ===
	private final VBox mainVBox = createMainVBox();
	private final HBox buttonHBox = createButtonHBox();
	private ProgressBar progressBar;
	private Button buttonSaveComponent;
	private Button buttonClose;
	private HBox hBoxButtons;
	private HBox hBoxProgress;

	// === Tab Components ===
	private TabPollutantTaxCap pollTaxCapTab;
	private TabMarketShare techMarketShareTab;
	private TabTechBound techBoundTab;
	private TabTechBound techBound2Tab;
	private TabTechAvailable techAvailTab;
	private TabFixedDemand fixedDemandTab;
	private TabTechParam techParamTab;
	private TabTechTax techTaxTab;
	private TabFuelPriceAdj fuelPriceAdjTab;
	private TabCafeStd cafeStdTab;
	private TabXMLList xmlListTab;
	private PolicyTab currentTab;

	// === State and Task Management ===
	private Task<?> saveTask;
	private Thread saveThread;
	private Stage stageWithTabs;
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);

	/**
	 * Constructs a new PaneNewScenarioComponent.
	 * <p>
	 * Initializes the main UI components, sets up event handlers, and loads the component library table.
	 * The pane is styled and sized to fit within the Scenario Builder's main window.
	 */
	public PaneNewScenarioComponent() {
		initializeButtons();
		initializeComponentLibraryTable();
		setupEventHandlers();
		mainVBox.getChildren().addAll(ComponentLibraryTable.getTableComponents());
		mainVBox.prefWidthProperty().bind(Client.primaryStage.widthProperty().multiply(4.0 / 7.0));
	}

	// === Initialization Methods ===
	/**
	 * Initializes the main action buttons for component management (New, Edit, Browse, Delete, Refresh).
	 * Sets tooltips, icons, and disables buttons as appropriate.
	 */
	private void initializeButtons() {
		Client.buttonNewComponent = utils.createButton(BUTTON_LABEL_NEW, styles.getBigButtonWidth(),
				"New: Open dialog to create new scenario component", "add");
		Client.buttonEditComponent = utils.createButton(BUTTON_LABEL_EDIT, styles.getBigButtonWidth(),
				"Edit: Edit selected scenario component", "edit");
		Client.buttonEditComponent.setDisable(true);
		Client.buttonBrowseComponentLibrary = utils.createButton(BUTTON_LABEL_BROWSE, styles.getBigButtonWidth(),
				"Browse: Open scenario component library folder", "open_folder");
		Client.buttonDeleteComponent = utils.createButton(BUTTON_LABEL_DELETE, styles.getBigButtonWidth(),
				"Delete: Remove selected scenario component", "delete");
		Client.buttonDeleteComponent.setDisable(true);
		Client.buttonRefreshComponents = utils.createButton(BUTTON_LABEL_REFRESH, styles.getBigButtonWidth(),
				"Refresh: Reload list of candidate scenario components", "refresh");
	}

	/**
	 * Loads and refreshes the component library table from the scenario components directory.
	 * Handles exceptions and displays warnings if loading fails.
	 */
	private void initializeComponentLibraryTable() {
		try {
			refreshComponentLibraryTable();
		} catch (Exception exception) {
			utils.warningMessage(ERROR_LOADING_COMPONENTS);
			System.out.println("Error loading scenario component files from:");
			System.out.println("    " + vars.getScenarioComponentsDir());
			System.out.println("Error: " + exception);
			utils.exitOnException();
		}
	}

	/**
	 * Sets up event handlers for table selection and button actions (New, Edit, Browse, Delete, Refresh).
	 * Ensures correct enabling/disabling of buttons based on selection state.
	 */
	private void setupEventHandlers() {
		ComponentLibraryTable.getTableComponents().setOnMouseClicked(event -> setArrowAndButtonStatus());
		Client.buttonDeleteComponent.setDisable(true);
		Client.buttonNewComponent.setOnAction(event -> showComponentDialog(null, null, Client.primaryStage, null, null));
		Client.buttonEditComponent.setOnAction(event -> handleEditComponent());
		Client.buttonRefreshComponents.setOnAction(event -> refreshComponentLibraryTable());
		Client.buttonBrowseComponentLibrary.setOnAction(event -> handleBrowseComponentLibrary());
		Client.buttonDeleteComponent.setOnAction(event -> handleDeleteComponent());
	}

	// === Event Handlers ===
	/**
	 * Handles the logic for editing a scenario component.
	 * <p>
	 * - Only allows editing if exactly one component is selected.
	 * - If the selected file is an XML, it opens in the XML editor.
	 * - Otherwise, attempts to determine the component type from the file's contents
	 *   and opens the appropriate dialog for editing.
	 */
	private void handleEditComponent() {
		ObservableList<ComponentRow> selectedComponentRows = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItems();
		if (selectedComponentRows == null || selectedComponentRows.size() != 1) {
			utils.showInformationDialog("Information", INFO_UNSUPPORTED_ACTION, INFO_EDIT_REQUIRES_ONE);
			return;
		}
		String componentFilePath = selectedComponentRows.get(0).getAddress();
		System.out.println("Editing component " + componentFilePath);
		if (componentFilePath.toLowerCase().endsWith(".xml")) {
			String xmlFilePath = vars.getScenarioComponentsDir() + File.separator + componentFilePath;
			files.showFileInXmlEditor(xmlFilePath);
		} else {
			String tabType = null;
			ArrayList<String> fileContents = files.getStringArrayFromFile(componentFilePath, null);
			if (fileContents != null && fileContents.size() > 1) {
				String firstLine = fileContents.get(0);
				if (firstLine.contains(XML_LIST_KEYWORD)) {
					tabType = TAB_XML_LIST;
				}
			}
			if (tabType == null && fileContents != null) {
				for (String line : fileContents) {
					if (line.startsWith("#Scenario component type:")) {
						tabType = line.substring(line.indexOf(":") + 1).trim();
					}
				}
			}
			showComponentDialog(null, null, Client.primaryStage, tabType, fileContents);
		}
	}

	/**
	 * Handles the action of browsing the scenario component library folder in the system file explorer.
	 * Displays errors and exits on exception.
	 */
	private void handleBrowseComponentLibrary() {
		try {
			String scenarioComponentsDirectory = vars.getScenarioComponentsDir();
			files.openFileExplorer(scenarioComponentsDirectory);
		} catch (Exception exception) {
			exception.printStackTrace();
			utils.exitOnException();
		}
	}

	/**
	 * Handles the deletion of selected scenario components.
	 * <p>
	 * - Checks if any selected component is used in a scenario and prevents deletion if so.
	 * - Moves deleted files to the trash directory.
	 * - Updates the component library table after deletion.
	 */
	private void handleDeleteComponent() {
		ObservableList<ComponentRow> selectedComponentRows = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItems();
		if (selectedComponentRows == null) return;
		if (checkIfComponentsAreUsed(selectedComponentRows)) {
			utils.warningMessage(WARNING_COMPONENT_USED);
			return;
		}
		if (!utils.confirmDelete())
			return;
		List<ComponentRow> componentsToRemove = new ArrayList<>();
		for (ComponentRow componentRow : selectedComponentRows) {
			String componentFilePath = componentRow.getAddress();
			String trashFileName = componentRow.getFileName();
			if (trashFileName.contains(File.separator))
				trashFileName = trashFileName.substring(trashFileName.lastIndexOf(File.separator) + 1);
			String trashFilePath = vars.getTrashDir() + File.separator + trashFileName;
			try {
				Files.move(Paths.get(componentFilePath), Paths.get(trashFilePath),
						StandardCopyOption.REPLACE_EXISTING);
				componentsToRemove.add(componentRow);
			} catch (Exception exception) {
				utils.warningMessage(ERROR_MOVING_FILE + componentFilePath + " to trash");
				System.out.println("error:" + exception);
				utils.exitOnException();
			}
		}
		ComponentLibraryTable.removeFromListOfFiles(javafx.collections.FXCollections.observableArrayList(componentsToRemove));
	}

	/**
	 * Checks if any of the selected scenario components are used in existing scenarios.
	 *
	 * @param selectedFiles List of selected ComponentRow objects
	 * @return true if any component is used, false otherwise
	 */
	private boolean checkIfComponentsAreUsed(ObservableList<ComponentRow> selectedFiles) {
		if (selectedFiles == null) return false;
		ObservableList<ScenarioRow> scenarioLibrary = ScenarioTable.listOfScenarioRuns;
		for (ScenarioRow scenario : scenarioLibrary) {
			String components = scenario.getComponents();
			if (components.length() > 0) {
				for (ComponentRow selectedFile : selectedFiles) {
					String fileToDelete = selectedFile.getFileName();
					if (components.contains(fileToDelete))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Shows the dialog for creating or editing a scenario component.
	 * <p>
	 * - Sets up the dialog UI, initializes all tab panes, and binds event handlers for save and close actions.
	 * - Handles the logic for saving the component file and updating the component library table.
	 * - Manages the dialog's lifecycle and ensures proper cleanup on close or cancel.
	 *
	 * @param name Name of the component (optional)
	 * @param filename Filename of the component (optional)
	 * @param mainStage The main application stage
	 * @param whichTab The tab to select (optional)
	 * @param contentToLoad Content to load into the tab (optional)
	 */
	private void showComponentDialog(String name, String filename, Stage mainStage, String whichTab,
			ArrayList<String> contentToLoad) {
		stageWithTabs = new Stage();
		double dialogWidth = 1150;
		double dialogHeight = 720;

		hBoxButtons = createButtonHBox();
		buttonSaveComponent = createDialogButton(BUTTON_LABEL_SAVE);
		buttonClose = createDialogButton(BUTTON_LABEL_CLOSE);
		hBoxButtons.getChildren().addAll(buttonSaveComponent, buttonClose);
		hBoxButtons.setSpacing(15.);
		hBoxButtons.setAlignment(javafx.geometry.Pos.CENTER);
		hBoxButtons.setPadding(new javafx.geometry.Insets(0, 10, 10, 10));

		progressBar = createProgressBar(dialogWidth - 50);
		hBoxProgress = createProgressHBox(progressBar);
		hBoxProgress.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));

		xmlListTab = new TabXMLList(TAB_XML_LIST, stageWithTabs, ComponentLibraryTable.getTableComponents());
		xmlListTab.setClosable(false);
		pollTaxCapTab = new TabPollutantTaxCap(TAB_POLLUTANT_TAX_CAP, stageWithTabs);
		pollTaxCapTab.setClosable(false);
		fuelPriceAdjTab = new TabFuelPriceAdj(TAB_FUEL_PRICE_ADJ, stageWithTabs);
		fuelPriceAdjTab.setClosable(false);
		techMarketShareTab = new TabMarketShare(TAB_MARKET_SHARE, stageWithTabs, this);
		techMarketShareTab.setClosable(false);
		//techBoundTab = new TabTechBound(TAB_TECH_BOUND, stageWithTabs);
		//techBoundTab.setClosable(false);
		techBound2Tab = new TabTechBound(TAB_TECH_BOUND2, stageWithTabs);
		techBound2Tab.setClosable(false);
		cafeStdTab = new TabCafeStd(TAB_MPG_TARGET, stageWithTabs);
		cafeStdTab.setClosable(false);
		techAvailTab = new TabTechAvailable(TAB_TECH_AVAIL, stageWithTabs);
		techAvailTab.setClosable(false);
		techParamTab = new TabTechParam(TAB_TECH_PARAM, stageWithTabs);
		techParamTab.setClosable(false);
		techTaxTab = new TabTechTax(TAB_TECH_TAX, stageWithTabs);
		techTaxTab.setClosable(false);
		fixedDemandTab = new TabFixedDemand(TAB_FIXED_DEMAND, stageWithTabs);
		fixedDemandTab.setClosable(false);

		TabPane addComponentTabPane = new TabPane();
		addComponentTabPane.setStyle(styles.getStyle2());
		addComponentTabPane.getTabs().addAll(pollTaxCapTab, techMarketShareTab, /*techBoundTab,*/ techBound2Tab,
				techTaxTab, cafeStdTab, techParamTab, fuelPriceAdjTab, fixedDemandTab, techAvailTab, xmlListTab);
		javafx.scene.layout.VBox.setVgrow(addComponentTabPane, javafx.scene.layout.Priority.ALWAYS);
		// Allow the TabPane to expand fully and bind its height to the dialog so tab content fills available space
		addComponentTabPane.setMaxHeight(Double.MAX_VALUE);
		addComponentTabPane.setPrefHeight(Double.MAX_VALUE);
		// When dialogPane is created below we bind the tab pane height to the dialog height minus the progress and button bars

		VBox dialogPane = new VBox();
		dialogPane.getChildren().addAll(addComponentTabPane, hBoxProgress, hBoxButtons);
		dialogPane.setSpacing(5);
		// Ensure the dialog and tab pane can grow; bind tab pane height to remaining space in dialog
		dialogPane.setMaxHeight(Double.MAX_VALUE);
		dialogPane.setMinHeight(0);
		addComponentTabPane.prefHeightProperty().bind(dialogPane.heightProperty().subtract(hBoxProgress.heightProperty()).subtract(hBoxButtons.heightProperty()).subtract(10));

		stageWithTabs.initOwner(mainStage);
		stageWithTabs.initModality(APPLICATION_MODAL);
		Scene scene = new Scene(dialogPane, dialogWidth, dialogHeight);
		try {
			if (getClass().getResource("/resources/modern.css") != null) {
				scene.getStylesheets().add(getClass().getResource("/resources/modern.css").toExternalForm());
			}
		} catch (Exception e) {
			System.out.println("Error loading modern.css: " + e);
		}
		stageWithTabs.setScene(scene);
		stageWithTabs.setTitle(DIALOG_TITLE_NEW_COMPONENT);

		stageWithTabs.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				if (saveTask != null) {
					saveTask.cancel();
				}
				if (saveThread != null && saveThread.isAlive()) {
					saveThread.interrupt();
				}
				if (stageWithTabs != null) {
					stageWithTabs.hide();
					stageWithTabs.setOnCloseRequest(null);
					stageWithTabs = null;
				}
			}
		});

		buttonClose.setOnAction(e -> {
			if (stageWithTabs != null) {
				stageWithTabs.close();
				stageWithTabs.setOnCloseRequest(null);
				stageWithTabs = null;
			}
		});

		buttonSaveComponent.setOnAction(e -> {
			String which = addComponentTabPane.getSelectionModel().getSelectedItem().getText();
			currentTab = getTabByName(which);
			if (currentTab == null) {
				utils.warningMessage(WARNING_UNKNOWN_TAB + which);
				return;
			}
			progressBar.progressProperty().bind(currentTab.getProgressBar().progressProperty());

			saveTask = new Task<Integer>() {
				@Override
				public Integer call() throws Exception {
					currentTab.saveScenarioComponent();
					return 1;
				}
				@Override
				protected void succeeded() {
					super.succeeded();
					Platform.runLater(() -> {
						//System.out.println("Done!");
						saveComponentFile(currentTab);
					});
				}
				@Override
				protected void cancelled() {
					super.cancelled();
					Platform.runLater(() -> {
						System.out.println("Cancelled!");
						utils.warningMessage(WARNING_PROCESS_CANCELLED);
						enableButtons();
						currentTab.resetFileContent();
						currentTab.resetFilenameSuggestion();
						currentTab.resetProgressBar();
					});
				}
				@Override
				protected void failed() {
					super.failed();
					Platform.runLater(() -> {
						System.out.println("Failed!");
					 utils.warningMessage(WARNING_PROCESS_FAILED);
						enableButtons();
						currentTab.resetFileContent();
						currentTab.resetFilenameSuggestion();
						currentTab.resetProgressBar();
					});
				}
			};
			saveThread = new Thread(saveTask);
			saveThread.setDaemon(true);
			saveThread.start();
			disableButtons();
		});

		if (whichTab != null) {
			currentTab = getTabByName(whichTab);
			if (currentTab != null) {
				selectTabAndLoadContent(whichTab, contentToLoad, addComponentTabPane);
			}
		}

		// Allow dialog to be resized so tab content (tables, panes) can expand vertically
		stageWithTabs.setResizable(true);
		stageWithTabs.show();
	}

	/**
	 * Returns the PolicyTab instance corresponding to the given tab name.
	 *
	 * @param tabName The name of the tab
	 * @return The PolicyTab instance, or null if not found
	 */
	private PolicyTab getTabByName(String tabName) {
		switch (tabName) {
			case TAB_MARKET_SHARE:
			case TAB_FLEX_SHARE:
				return techMarketShareTab;
			case TAB_MPG_TARGET:
				return cafeStdTab;
			//case TAB_TECH_BOUND:
			//	return techBoundTab;
			case TAB_TECH_BOUND2:
				return techBound2Tab;
			case TAB_TECH_AVAIL:
				return techAvailTab;
			case TAB_TECH_PARAM:
				return techParamTab;
			case TAB_TECH_TAX:
				return techTaxTab;
			case TAB_XML_LIST:
				return xmlListTab;
			case TAB_POLLUTANT_TAX_CAP:
				return pollTaxCapTab;
			case TAB_FUEL_PRICE_ADJ:
				return fuelPriceAdjTab;
			case TAB_FIXED_DEMAND:
				return fixedDemandTab;
			default:
				return null;
		}
	}

	/**
	 * Loads content into the specified PolicyTab and selects it in the TabPane.
	 *
	 * @param whichTab The tab name
	 * @param contentToLoad The content to load
	 * @param tp The TabPane containing the tabs
	 */
	private void selectTabAndLoadContent(String whichTab, ArrayList<String> contentToLoad, TabPane tp) {
		if (whichTab == null) return;
		for (Tab t : tp.getTabs()) {
			if (t instanceof PolicyTab && t.getText().equals(whichTab)) {
				PolicyTab policyTab = (PolicyTab) t;
				policyTab.loadContent(contentToLoad);
				tp.getSelectionModel().select(policyTab);
				break;
			}
		}
	}

	/**
	 * Saves the scenario component file using the provided PolicyTab's content and filename suggestion.
	 * <p>
	 * - Determines the correct file extension and filter based on content.
	 * - Handles saving to a temporary file if required.
	 * - Updates the component library table after saving.
	 *
	 * @param tab The PolicyTab containing the file content and filename suggestion
	 */
	public void saveComponentFile(PolicyTab tab) {
		if (tab == null) return;
		String filenameSuggestion = tab.getFilenameSuggestion();
		String fileContent = tab.getFileContent();
		if (fileContent == null) return;
		boolean useTempFile = false;
		if (fileContent.equals("use temp file")) {
			useTempFile = true;
		}
		if ((filenameSuggestion != null) && (!filenameSuggestion.isEmpty())) {
			enableButtons();
			tab.resetFileContent();
			tab.resetFilenameSuggestion();
			tab.resetProgressBar();
			String filter1 = "";
			String filter2 = "";
			if (fileContent.contains(XML_LIST_KEYWORD)) {
				filter1 = FILE_FILTER_TXT;
				filter2 = FILE_EXT_TXT;
				if ((!filenameSuggestion.endsWith(".txt")) && (!filenameSuggestion.endsWith(".TXT")))
					filenameSuggestion += ".txt";
			} else {
				filter1 = FILE_FILTER_CSV;
				filter2 = FILE_EXT_CSV;
				if ((!filenameSuggestion.endsWith(".csv")) && (!filenameSuggestion.endsWith(".CSV")))
					filenameSuggestion += ".csv";
			}
			File file = FileChooserPlus.showSaveDialog(stageWithTabs, "Save Scenario Component",
					new File(vars.getScenarioComponentsDir()), filenameSuggestion,
					FileChooserPlus.createExtensionFilter(filter1, filter2));
			if (file == null)
				return;
			if (!useTempFile) {
				files.saveFile(fileContent, file);
			} else {
				String tempPolicyFilename = vars.getGlimpseDir() + File.separator + "GLIMPSE-Data" + File.separator
						+ "temp" + File.separator + TEMP_POLICY_FILENAME;
				try {
					Files.move(Paths.get(tempPolicyFilename), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					System.out.println(ERROR_CREATING_POLICY_FILE + e);
				}
			}
			ComponentRow p1 = new ComponentRow(file.getName(), file.getPath(), new Date());
			ComponentRow[] fileArr = { p1 };
			ComponentLibraryTable.addOrUpdateFiles(fileArr);
		}
		refreshComponentLibraryTable();
	}

	/**
	 * Disables the Save and Close buttons in the component dialog.
	 * Used to prevent user interaction during save operations.
	 */
	public void disableButtons() {
		if (buttonSaveComponent != null) buttonSaveComponent.setDisable(true);
		if (buttonClose != null) buttonClose.setDisable(true);
	}

	/**
	 * Enables the Save and Close buttons in the component dialog.
	 * Used to restore user interaction after save operations.
	 */
	public void enableButtons() {
		if (buttonSaveComponent != null) buttonSaveComponent.setDisable(false);
		if (buttonClose != null) buttonClose.setDisable(false);
	}

	/**
	 * Refreshes the scenario component library table by scanning the components directory.
	 * Updates the table with the latest files found.
	 */
	public void refreshComponentLibraryTable() {
		File folder = new File(vars.getScenarioComponentsDir());
		ArrayList<File> fileList = buildFileList(folder.toPath());
		ComponentRow[] fileArr = new ComponentRow[fileList.size()];
		int k = 0;
		for (File file : fileList) {
			String relativeName = files.getRelativePath(folder.toString(), file.getAbsolutePath());
			ComponentRow p1 = new ComponentRow(relativeName, file.getPath(), new Date(file.lastModified()));
			fileArr[k] = p1;
			k++;
		}
		ComponentLibraryTable.createListOfFiles(fileArr);
	}

	/**
	 * Recursively builds a list of files from the given directory path.
	 *
	 * @param path The root directory path
	 * @return ArrayList of File objects found in the directory and subdirectories
	 */
	public ArrayList<File> buildFileList(Path path) {
		ArrayList<File> rtnArray = new ArrayList<>();
		File root = path.toFile();
		File[] list = root.listFiles();
		if (list == null) return rtnArray;
		for (File f : list) {
			if (f.isDirectory()) {
				rtnArray.addAll(buildFileList(f.toPath()));
			} else {
				rtnArray.add(f);
			}
		}
		return rtnArray;
	}

	/**
	 * Loads the given list of files into the component library table.
	 *
	 * @param file List of File objects to load
	 */
	public void loadFile(List<File> file) {
		if (file == null) return;
		int k = 0;
		ComponentRow[] fileArr = new ComponentRow[file.size()];
		for (File i : file) {
			ComponentRow p1 = new ComponentRow(i.getName(), i.getPath(), new Date(i.lastModified()));
			fileArr[k] = p1;
			k++;
		}
		ComponentLibraryTable.addToListOfFiles(fileArr);
	}

	/**
	 * Returns the VBox containing the component library table.
	 *
	 * @return VBox with the component library table
	 */
	public VBox getvBox() {
		return mainVBox;
	}

	/**
	 * Creates and returns a styled VBox for the main pane.
	 *
	 * @return A new VBox instance
	 */
	private VBox createMainVBox() {
		VBox vbox = new VBox(1);
		return vbox;
	}

	/**
	 * Creates and returns a new HBox for button layout.
	 *
	 * @return A new HBox instance
	 */
	private HBox createButtonHBox() {
		return new HBox(1);
	}

	/**
	 * Creates and returns a ProgressBar with the specified width.
	 *
	 * @param width The preferred width of the progress bar
	 * @return A new ProgressBar instance
	 */
	private ProgressBar createProgressBar(double width) {
		ProgressBar bar = new ProgressBar(0.0);
		bar.setPrefWidth(width);
		return bar;
	}

	/**
	 * Creates and returns an HBox containing the given ProgressBar, centered.
	 *
	 * @param bar The ProgressBar to add
	 * @return A new HBox instance
	 */
	private HBox createProgressHBox(ProgressBar bar) {
		HBox hbox = new HBox();
		hbox.setAlignment(javafx.geometry.Pos.CENTER);
		hbox.getChildren().add(bar);
		return hbox;
	}

	/**
	 * Creates and returns a dialog button with the specified text and default style.
	 *
	 * @param text The button label
	 * @return A new Button instance
	 */
	private Button createDialogButton(String text) {
		return utils.createButton(text, styles.getBigButtonWidth(), null);
	}
}
