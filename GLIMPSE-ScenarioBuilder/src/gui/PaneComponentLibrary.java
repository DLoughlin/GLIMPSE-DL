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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import glimpseElement.ComponentLibraryTable;
import glimpseElement.ComponentRow;
import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Builds and manages the Component Library pane (top-left panel) in Scenario Builder.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Creates and wires all component-library actions (new, edit, browse, delete, refresh).</li>
 *   <li>Owns the component table population lifecycle, including asynchronous refresh and startup load.</li>
 *   <li>Coordinates with {@link ScenarioComponentCreatorDialog} to create or edit component definitions.</li>
 *   <li>Protects scenario integrity by preventing deletion of components referenced by scenarios.</li>
 *   <li>Maintains default table presentation behavior (latest first, scroll positioning, placeholders).</li>
 * </ul>
 * <p>
 * <b>Usage:</b> Construct once during {@link ScenarioBuilder#build()}, then embed {@link #getvBox()} in
 * the main layout and trigger {@link #refreshComponentLibraryTableForStartup()} during initial load.
 * <p>
 * <b>Thread Safety:</b> UI mutations are performed on the JavaFX Application Thread. File-system scanning
 * for refresh runs on a background daemon thread and marshals table updates back to JavaFX.
 * <p>
 * <b>Integration:</b>
 * <ul>
 *   <li>Works with {@link ComponentLibraryTable} for table model/list operations.</li>
 *   <li>Uses {@link Client} button slots and startup notifications for coordinated app state.</li>
 *   <li>Delegates create/edit workflows to {@link ScenarioComponentCreatorDialog}.</li>
 * </ul>
 */
public class PaneComponentLibrary extends gui.ScenarioBuilder {

	// === Constants ===
	private static final String BUTTON_LABEL_NEW = "New";
	private static final String BUTTON_LABEL_EDIT = "Edit";
	private static final String BUTTON_LABEL_BROWSE = "Browse";
	private static final String BUTTON_LABEL_DELETE = "Delete";
	private static final String BUTTON_LABEL_REFRESH = "Refresh";

	private static final String XML_LIST_KEYWORD = "xmllist";
	private static final String ERROR_LOADING_COMPONENTS = "Problem loading scenario component files.";
	private static final String ERROR_MOVING_FILE = "Problem moving file ";
	private static final String INFO_EDIT_REQUIRES_ONE = "Editing requires exactly one scenario component to be selected.";
	private static final String INFO_UNSUPPORTED_ACTION = "Unsupported action";
	private static final String WARNING_COMPONENT_USED = "Cannot delete selected scenario component since it is used in a scenario.";
	private static final String PROMPT_FILTER_COMPONENTS = "Filter components...";
	private static final String LOADING_COMPONENTS_MESSAGE = "Loading components...";
	private static final String NO_COMPONENTS_MESSAGE = "No scenario components found.";

	// === UI Components ===
	private final VBox mainVBox = new VBox(1);

	// Dialog
	private final ScenarioComponentCreatorDialog creatorDialog;

	private final AtomicBoolean componentRefreshInProgress = new AtomicBoolean(false);

	/**
	 * Creates the pane, initializes controls, and binds action handlers.
	 */
	public PaneComponentLibrary() {
		this.creatorDialog = new ScenarioComponentCreatorDialog(this::refreshComponentLibraryTable);
		initializeButtons();
		initializeFilterField();
		initializeComponentLibraryTable();
		setupEventHandlers();
		mainVBox.getChildren().add(ComponentLibraryTable.getTableComponents());
		mainVBox.setFillWidth(true);
		mainVBox.setMaxWidth(Double.MAX_VALUE);
	}

	private void initializeFilterField() {
		// ScenarioBuilder.createTables() initializes this early so table setup can wire listeners.
		// This pane owns layout placement + prompt text.
		TextField filter = ComponentLibraryTable.getFilterComponentsTextField();
		if (filter == null) {
			filter = utils.createTextField();
			ComponentLibraryTable.setFilterComponentsTextField(filter);
		}
		filter.setPromptText(PROMPT_FILTER_COMPONENTS);
	}

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

	private void initializeComponentLibraryTable() {
		try {
			if (ComponentLibraryTable.getTableComponents() != null) {
				ComponentLibraryTable.getTableComponents().setPlaceholder(utils.createLabel(LOADING_COMPONENTS_MESSAGE));
				applyDefaultCreatedSortAndScrollToTop();
			}
		} catch (Exception exception) {
			utils.warningMessage(ERROR_LOADING_COMPONENTS);
			System.out.println("Error loading scenario component files from:");
			System.out.println("    " + vars.getScenarioComponentsDir());
			System.out.println("Error: " + exception);
			utils.exitOnException();
		}
	}

	private void setupEventHandlers() {
		ComponentLibraryTable.getTableComponents().setOnMouseClicked(event -> setArrowAndButtonStatus());
		Client.buttonDeleteComponent.setDisable(true);

		Client.buttonNewComponent.setOnAction(event -> creatorDialog.showNew(Client.primaryStage));
		Client.buttonEditComponent.setOnAction(event -> handleEditComponent());
		Client.buttonRefreshComponents.setOnAction(event -> refreshComponentLibraryTable());
		Client.buttonBrowseComponentLibrary.setOnAction(event -> handleBrowseComponentLibrary());
		Client.buttonDeleteComponent.setOnAction(event -> handleDeleteComponent());
	}

	private void handleEditComponent() {
		ObservableList<ComponentRow> selectedComponentRows = ComponentLibraryTable.getTableComponents().getSelectionModel()
				.getSelectedItems();
		if (selectedComponentRows == null || selectedComponentRows.size() != 1) {
			utils.showInformationDialog("Information", INFO_UNSUPPORTED_ACTION, INFO_EDIT_REQUIRES_ONE);
			return;
		}
		String componentFilePath = selectedComponentRows.get(0).getAddress();
		System.out.println("Editing component " + componentFilePath);
		if (componentFilePath.toLowerCase().endsWith(".xml")) {
			files.showFileInXmlEditor(componentFilePath);
		} else {
			String tabType = null;
			ArrayList<String> fileContents = files.getStringArrayFromFile(componentFilePath, null);
			if (fileContents != null && fileContents.size() > 1) {
				String firstLine = fileContents.get(0);
				if (firstLine.contains(XML_LIST_KEYWORD)) {
					tabType = ScenarioComponentCreatorDialog.TAB_XML_LIST;
				}
			}
			if (tabType == null && fileContents != null) {
				for (String line : fileContents) {
					if (line.startsWith("#Scenario component type:")) {
						tabType = line.substring(line.indexOf(":") + 1).trim();
					}
				}
			}
			creatorDialog.showEdit(Client.primaryStage, tabType, fileContents);
		}
	}

	private void handleBrowseComponentLibrary() {
		try {
			String scenarioComponentsDirectory = vars.getScenarioComponentsDir();
			files.openFileExplorer(scenarioComponentsDirectory);
		} catch (Exception exception) {
			exception.printStackTrace();
			utils.exitOnException();
		}
	}

	private void handleDeleteComponent() {
		ObservableList<ComponentRow> selectedComponentRows = ComponentLibraryTable.getTableComponents().getSelectionModel()
				.getSelectedItems();
		if (selectedComponentRows == null)
			return;
		List<ComponentRow> selectedComponentRowsSnapshot = new ArrayList<>(selectedComponentRows);
		if (selectedComponentRowsSnapshot.isEmpty()) {
			return;
		}
		if (checkIfComponentsAreUsed(selectedComponentRowsSnapshot)) {
			utils.warningMessage(WARNING_COMPONENT_USED);
			return;
		}
		if (!utils.confirmDelete())
			return;
		List<ComponentRow> componentsToRemove = new ArrayList<>();
		for (ComponentRow componentRow : selectedComponentRowsSnapshot) {
			String componentFilePath = componentRow.getAddress();
			String trashFileName = componentRow.getFileName();
			if (trashFileName.contains(File.separator))
				trashFileName = trashFileName.substring(trashFileName.lastIndexOf(File.separator) + 1);
			String trashFilePath = vars.getTrashDir() + File.separator + trashFileName;
			try {
				Files.move(Paths.get(componentFilePath), Paths.get(trashFilePath), StandardCopyOption.REPLACE_EXISTING);
				componentsToRemove.add(componentRow);
			} catch (Exception exception) {
				utils.warningMessage(ERROR_MOVING_FILE + componentFilePath + " to trash");
				System.out.println("error:" + exception);
				utils.exitOnException();
			}
		}
		ComponentLibraryTable.removeFromListOfFiles(
				javafx.collections.FXCollections.observableArrayList(componentsToRemove));
	}

	private boolean checkIfComponentsAreUsed(List<ComponentRow> selectedFiles) {
		if (selectedFiles == null)
			return false;
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
	 * Refreshes component table contents for user-initiated refresh actions.
	 */
	public void refreshComponentLibraryTable() {
		refreshComponentLibraryTableAsync(true);
	}

	/**
	 * Refreshes component table contents during startup bootstrap flow.
	 * <p>
	 * This path marks startup component load completion once refresh work has finished.
	 */
	public void refreshComponentLibraryTableForStartup() {
		refreshComponentLibraryTableAsync(false);
	}

	private void refreshComponentLibraryTableAsync(boolean userInitiated) {
		if (!componentRefreshInProgress.compareAndSet(false, true)) {
			return;
		}
		if (ComponentLibraryTable.getTableComponents() != null) {
			ComponentLibraryTable.getTableComponents().setPlaceholder(utils.createLabel(LOADING_COMPONENTS_MESSAGE));
		}
		//System.out.println("Loading scenario components...");
		Thread thread = new Thread(() -> {
			try {
				File folder = new File(vars.getScenarioComponentsDir());
				ArrayList<File> fileList = buildFileList(folder.toPath());
				fileList.sort((left, right) -> {
					long leftModified = left == null ? Long.MIN_VALUE : left.lastModified();
					long rightModified = right == null ? Long.MIN_VALUE : right.lastModified();
					int byCreatedDesc = Long.compare(rightModified, leftModified);
					if (byCreatedDesc != 0) {
						return byCreatedDesc;
					}
					String leftName = left == null ? "" : left.getName();
					String rightName = right == null ? "" : right.getName();
					return leftName.compareToIgnoreCase(rightName);
				});
				ComponentRow[] fileArr = new ComponentRow[fileList.size()];
				int k = 0;
				for (File file : fileList) {
					String relativeName = files.getRelativePath(folder.toString(), file.getAbsolutePath());
					ComponentRow p1 = new ComponentRow(relativeName, file.getPath(), new Date(file.lastModified()));
					fileArr[k] = p1;
					k++;
				}
				javafx.application.Platform.runLater(() -> {
					ComponentLibraryTable.createListOfFiles(fileArr);
					applyDefaultCreatedSortAndScrollToTop();
					if (ComponentLibraryTable.getTableComponents() != null && fileArr.length == 0) {
						ComponentLibraryTable.getTableComponents().setPlaceholder(utils.createLabel(NO_COMPONENTS_MESSAGE));
					}
					if (!userInitiated) {
						Client.markInitialComponentLoadComplete();
					}
				});
			} catch (Exception exception) {
				javafx.application.Platform.runLater(() -> {
					utils.warningMessage(ERROR_LOADING_COMPONENTS);
					System.out.println("Error loading scenario component files from:");
					System.out.println("    " + vars.getScenarioComponentsDir());
					System.out.println("Error: " + exception);
					if (ComponentLibraryTable.getTableComponents() != null) {
						ComponentLibraryTable.getTableComponents().setPlaceholder(utils.createLabel(ERROR_LOADING_COMPONENTS));
					}
					System.out.println("Problem loading scenario components.");
					if (!userInitiated) {
						Client.markInitialComponentLoadComplete();
					}
				});
			} finally {
				componentRefreshInProgress.set(false);
			}
		}, "component-library-refresh");
		thread.setDaemon(true);
		thread.start();
	}

	private void applyDefaultCreatedSortAndScrollToTop() {
		if (Platform.isFxApplicationThread()) {
			applyDefaultCreatedSortAndScrollToTopNow();
			return;
		}
		Platform.runLater(this::applyDefaultCreatedSortAndScrollToTopNow);
	}

	private void applyDefaultCreatedSortAndScrollToTopNow() {
		TableView<ComponentRow> table = ComponentLibraryTable.getTableComponents();
		if (table == null) {
			return;
		}
		if (table.getItems() != null && !table.getItems().isEmpty()) {
			table.scrollTo(0);
		}
	}

	/**
	 * Recursively builds a flat list of files rooted at the provided path.
	 *
	 * @param path root directory to traverse
	 * @return flattened list of files discovered under {@code path}
	 */
	public ArrayList<File> buildFileList(Path path) {
		ArrayList<File> rtnArray = new ArrayList<>();
		File root = path.toFile();
		File[] list = root.listFiles();
		if (list == null)
			return rtnArray;
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
	 * Appends files to the component library table model.
	 *
	 * @param file files to append as component rows
	 */
	public void loadFile(List<File> file) {
		if (file == null)
			return;
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
	 * Returns the root JavaFX container for this pane.
	 *
	 * @return pane root container
	 */
	public VBox getvBox() {
		return mainVBox;
	}
}