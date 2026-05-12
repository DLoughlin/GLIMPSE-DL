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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import glimpseElement.ComponentLibraryTable;
import glimpseElement.ComponentRow;
import glimpseElement.PolicyTab;
import glimpseElement.TabCafeStd;
import glimpseElement.TabFixedDemand;
import glimpseElement.TabFuelPriceAdj;
import glimpseElement.TabMarketShare;
import glimpseElement.TabPollutantTaxCap;
import glimpseElement.TabTechAvailable;
import glimpseElement.TabTechBound;
import glimpseElement.TabTechParam;
import glimpseElement.TabTechTax;
import glimpseElement.TabXMLList;
import glimpseUtil.FileChooserPlus;
import glimpseUtil.UtilsDialogs;
import javafx.application.Platform;
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
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * ScenarioComponentCreatorDialog owns the "New Scenario Component Creator" modal dialog.
 *
 * It encapsulates the Stage lifecycle, tab creation, and save workflow. Callers can
 * optionally pre-select a tab and load file content to support editing.
 */
public class ScenarioComponentCreatorDialog extends gui.ScenarioBuilder {

	// === Constants (kept consistent with prior PaneNewScenarioComponent) ===
	public static final String TAB_MARKET_SHARE = "Market Share";
	public static final String TAB_FLEX_SHARE = "Flex Share";
	public static final String TAB_MPG_TARGET = "MPG Target";
	public static final String TAB_TECH_BOUND2 = "Tech Bound";
	public static final String TAB_TECH_AVAIL = "Tech Avail";
	public static final String TAB_TECH_PARAM = "Tech Param";
	public static final String TAB_TECH_TAX = "Tech Tax/Subsidy";
	public static final String TAB_XML_LIST = "XML List";
	public static final String TAB_POLLUTANT_TAX_CAP = "Pollutant Tax/Cap";
	public static final String TAB_FUEL_PRICE_ADJ = "Fuel Price Adj";
	public static final String TAB_FIXED_DEMAND = "Fixed Demand";

	private static final String BUTTON_LABEL_SAVE = "Save";
	private static final String BUTTON_LABEL_CLOSE = "Close";
	private static final String DIALOG_TITLE_NEW_COMPONENT = "New Scenario Component Creator";
	private static final String FILE_FILTER_TXT = "TXT files (*.txt)";
	private static final String FILE_FILTER_CSV = "CSV files (*.csv)";
	private static final String FILE_EXT_TXT = "txt";
	private static final String FILE_EXT_CSV = "csv";
	private static final String XML_LIST_KEYWORD = "xmllist";
	private static final String TEMP_POLICY_FILENAME = "temp_policy_file.txt";

	private static final String ERROR_CREATING_POLICY_FILE = "Error creating policy file: ";
	private static final String WARNING_UNKNOWN_TAB = "Unknown tab selected: ";
	private static final String WARNING_PROCESS_CANCELLED = "Process of building scenario component cancelled.";
	private static final String WARNING_PROCESS_FAILED = "Process of building scenario component failed.";

	// === Owned UI/state ===
	private ProgressBar progressBar;
	private Button buttonSaveComponent;
	private Button buttonClose;
	private HBox hBoxButtons;
	private HBox hBoxProgress;

	private PolicyTab currentTab;
	private Task<?> saveTask;
	private Thread saveThread;
	private final AtomicBoolean saveInProgress = new AtomicBoolean(false);
	private Stage stageWithTabs;

	// === Tab Components ===
	private TabPollutantTaxCap pollTaxCapTab;
	private TabMarketShare techMarketShareTab;
	private TabTechBound techBound2Tab;
	private TabTechAvailable techAvailTab;
	private TabFixedDemand fixedDemandTab;
	private TabTechParam techParamTab;
	private TabTechTax techTaxTab;
	private TabFuelPriceAdj fuelPriceAdjTab;
	private TabCafeStd cafeStdTab;
	private TabXMLList xmlListTab;

	// Callback fired only after a successful component save.
	private final Runnable onSaveSuccess;

	/** Last size used for this dialog in the current session. */
	private static double lastDialogWidth = 1190;
	private static double lastDialogHeight = 750;

	public ScenarioComponentCreatorDialog(Runnable onSaveSuccess) {
		this.onSaveSuccess = (onSaveSuccess != null) ? onSaveSuccess : () -> {};
	}

	public void showNew(Stage ownerStage) {
		show(ownerStage, null, null);
	}

	public void showEdit(Stage ownerStage, String whichTab, ArrayList<String> contentToLoad) {
		show(ownerStage, whichTab, contentToLoad);
	}

	/**
	 * Shows the modal component creator.
	 */
	public void show(Stage mainStage, String whichTab, ArrayList<String> contentToLoad) {
		stageWithTabs = new Stage();
		double dialogWidth = lastDialogWidth;
		double dialogHeight = lastDialogHeight;

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

		// Tabs
		xmlListTab = new TabXMLList(TAB_XML_LIST, stageWithTabs, ComponentLibraryTable.getTableComponents());
		xmlListTab.setClosable(false);
		pollTaxCapTab = new TabPollutantTaxCap(TAB_POLLUTANT_TAX_CAP, stageWithTabs);
		pollTaxCapTab.setClosable(false);
		fuelPriceAdjTab = new TabFuelPriceAdj(TAB_FUEL_PRICE_ADJ, stageWithTabs);
		fuelPriceAdjTab.setClosable(false);
		techMarketShareTab = new TabMarketShare(TAB_MARKET_SHARE, stageWithTabs);
		techMarketShareTab.setClosable(false);
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
		addComponentTabPane.getTabs().addAll(pollTaxCapTab, techMarketShareTab, techBound2Tab, techTaxTab, cafeStdTab,
				techParamTab, fuelPriceAdjTab, fixedDemandTab, techAvailTab, xmlListTab);
		VBox.setVgrow(addComponentTabPane, javafx.scene.layout.Priority.ALWAYS);
		addComponentTabPane.setMaxHeight(Double.MAX_VALUE);
		addComponentTabPane.setPrefHeight(Double.MAX_VALUE);

		VBox dialogPane = new VBox();
		dialogPane.getChildren().addAll(addComponentTabPane, hBoxProgress, hBoxButtons);
		dialogPane.setSpacing(5);
		dialogPane.setMaxHeight(Double.MAX_VALUE);
		dialogPane.setMinHeight(0);
		addComponentTabPane.prefHeightProperty().bind(dialogPane.heightProperty().subtract(hBoxProgress.heightProperty())
				.subtract(hBoxButtons.heightProperty()).subtract(10));

		Window dialogOwner = mainStage;
		if (dialogOwner == null) {
			dialogOwner = UtilsDialogs.getPrimaryOwnerWindow();
		}
		if (dialogOwner != null) {
			try {
				stageWithTabs.initOwner(dialogOwner);
			} catch (Exception ignored) {}
		}
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

		// Remember last size within this session.
		stageWithTabs.widthProperty().addListener((obs, oldV, newV) -> {
			try {
				double w = newV == null ? -1 : newV.doubleValue();
				if (w > 500) {
					lastDialogWidth = w;
				}
			} catch (Exception ignored) {}
		});
		stageWithTabs.heightProperty().addListener((obs, oldV, newV) -> {
			try {
				double h = newV == null ? -1 : newV.doubleValue();
				if (h > 400) {
					lastDialogHeight = h;
				}
			} catch (Exception ignored) {}
		});

		// Restore last size (best-effort) when showing.
		try {
			stageWithTabs.setWidth(dialogWidth);
			stageWithTabs.setHeight(dialogHeight);
		} catch (Exception ignored) {}

		stageWithTabs.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				cleanupOnClose();
			}
		});

		buttonClose.setOnAction(e -> cleanupOnClose());

		buttonSaveComponent.setOnAction(e -> {
			if (saveThread != null && saveThread.isAlive()) {
				return;
			}
			if (!saveInProgress.compareAndSet(false, true)) {
				return;
			}
			setSaveButtonDisabled(true);
			String which = addComponentTabPane.getSelectionModel().getSelectedItem().getText();
			currentTab = getTabByName(which);
			if (currentTab == null) {
				saveInProgress.set(false);
				setSaveButtonDisabled(false);
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
					saveInProgress.set(false);
					Platform.runLater(() -> saveComponentFile(currentTab));
				}

				@Override
				protected void cancelled() {
					super.cancelled();
					saveInProgress.set(false);
					Platform.runLater(() -> {
						setSaveButtonDisabled(false);
						System.out.println("Cancelled!");
						utils.warningMessage(WARNING_PROCESS_CANCELLED);
						currentTab.resetFileContent();
						currentTab.resetFilenameSuggestion();
						currentTab.resetProgressBar();
					});
				}

				@Override
				protected void failed() {
					super.failed();
					saveInProgress.set(false);
					Platform.runLater(() -> {
						setSaveButtonDisabled(false);
						Throwable cause = getException();
						System.out.println("Failed!");
						if (cause != null) {
							System.out.println("Save failure cause: " + cause.getMessage());
							cause.printStackTrace();
							utils.warningMessage(WARNING_PROCESS_FAILED + "\n" + cause);
						} else {
							utils.warningMessage(WARNING_PROCESS_FAILED);
						}
						currentTab.resetFileContent();
						currentTab.resetFilenameSuggestion();
						currentTab.resetProgressBar();
					});
				}
			};

			saveThread = new Thread(saveTask);
			saveThread.setDaemon(true);
			saveThread.start();
		});

		if (whichTab != null) {
			currentTab = getTabByName(whichTab);
			if (currentTab != null) {
				selectTabAndLoadContent(whichTab, contentToLoad, addComponentTabPane);
			}
		}

		stageWithTabs.setResizable(true);
		stageWithTabs.setOnShown(e -> {
			try {
				Window owner = stageWithTabs.getOwner();
				if (owner == null || !owner.isShowing()) {
					owner = UtilsDialogs.getPrimaryOwnerWindow();
				}
				if (owner != null && owner.isShowing()) {
					double w = stageWithTabs.getWidth();
					double h = stageWithTabs.getHeight();
					if (w > 0 && h > 0) {
						stageWithTabs.setX(owner.getX() + ((owner.getWidth() - w) / 2.0));
						stageWithTabs.setY(owner.getY() + ((owner.getHeight() - h) / 2.0));
					}
				}
			} catch (Exception ignored) {}
		});
		stageWithTabs.show();
	}

	private void cleanupOnClose() {
		saveInProgress.set(false);
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

	private PolicyTab getTabByName(String tabName) {
		switch (tabName) {
			case TAB_MARKET_SHARE:
			case TAB_FLEX_SHARE:
				return techMarketShareTab;
			case TAB_MPG_TARGET:
				return cafeStdTab;
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

	private void selectTabAndLoadContent(String whichTab, ArrayList<String> contentToLoad, TabPane tp) {
		if (whichTab == null) return;
		for (Tab t : tp.getTabs()) {
			if (t instanceof PolicyTab && t.getText().equals(whichTab)) {
				PolicyTab policyTab = (PolicyTab) t;
				// Suppress new-component notices while restoring an existing Market Share/Flex Share component.
				if (policyTab instanceof TabMarketShare) {
					((TabMarketShare) policyTab).setEditing(true);
				}
				policyTab.loadContent(contentToLoad);
				tp.getSelectionModel().select(policyTab);
				break;
			}
		}
	}

	public void saveComponentFile(PolicyTab tab) {
		try {
			if (tab == null) {
				setSaveButtonDisabled(false);
				return;
			}
			String filenameSuggestion = tab.getFilenameSuggestion();
			String fileContent = tab.getFileContent();
			if (fileContent == null) {
				setSaveButtonDisabled(false);
				return;
			}
			boolean useTempFile = false;
			if (fileContent.equals("use temp file")) {
				useTempFile = true;
			}
			if ((filenameSuggestion != null) && (!filenameSuggestion.isEmpty())) {
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
				if (file == null) {
					setSaveButtonDisabled(false);
					return;
				}
				boolean saved = true;
				if (!useTempFile) {
					files.saveFile(fileContent, file);
				} else {
					String tempPolicyFilename = vars.getGlimpseDir() + File.separator + "GLIMPSE-Data" + File.separator
							+ "temp" + File.separator + TEMP_POLICY_FILENAME;
					try {
						Files.move(Paths.get(tempPolicyFilename), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (Exception e) {
						System.out.println(ERROR_CREATING_POLICY_FILE + e);
						saved = false;
					}
				}
				if (saved) {
					tab.resetFileContent();
					tab.resetFilenameSuggestion();
					tab.resetProgressBar();
					ComponentRow p1 = new ComponentRow(file.getName(), file.getPath(), new Date());
					ComponentRow[] fileArr = { p1 };
					ComponentLibraryTable.addOrUpdateFiles(fileArr);
					onSaveSuccess.run();
				}
			}
		} catch (Exception e) {
			System.out.println("saveComponentFile failed: " + e.getMessage());
			e.printStackTrace();
			utils.warningMessage(WARNING_PROCESS_FAILED + "\n" + e);
		} finally {
			setSaveButtonDisabled(false);
		}
	}

	private void setSaveButtonDisabled(boolean disabled) {
		if (buttonSaveComponent != null) {
			buttonSaveComponent.setDisable(disabled);
		}
	}

	private HBox createButtonHBox() {
		return new HBox(1);
	}

	private ProgressBar createProgressBar(double width) {
		ProgressBar bar = new ProgressBar(0.0);
		bar.setPrefWidth(width);
		return bar;
	}

	private HBox createProgressHBox(ProgressBar bar) {
		HBox hbox = new HBox();
		hbox.setAlignment(javafx.geometry.Pos.CENTER);
		hbox.getChildren().add(bar);
		return hbox;
	}
}
