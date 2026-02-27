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

import glimpseBuilder.SetupMenuEdit;
import glimpseBuilder.SetupMenuFile;
import glimpseBuilder.SetupMenuHelp;
import glimpseBuilder.SetupMenuTools;
import glimpseBuilder.SetupMenuView;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEVariables;
import java.io.File;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

/**
 * The main entry point and controller for the GLIMPSE Scenario Builder GUI application.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Initializes and launches the JavaFX-based Scenario Builder application.</li>
 *   <li>Handles application startup, shutdown, and splash screen display.</li>
 *   <li>Manages the main window, menu bar, and layout of all major GUI panels.</li>
 *   <li>Initializes and provides access to all major scenario and component panes, buttons, and execution threads.</li>
 *   <li>Processes command-line arguments and loads user options.</li>
 *   <li>Coordinates the setup of execution threads for GCAM and post-processing.</li>
 *   <li>Provides static accessors for key UI elements and threads for use throughout the application.</li>
 * </ul>
 *
 * <b>Usage:</b> This class is launched as a JavaFX application. It is responsible for the lifecycle of the Scenario Builder GUI.
 *
 * <b>Thread Safety:</b> Most methods must be called on the JavaFX Application Thread. Static accessors are provided for UI integration.
 *
 * <b>Integration:</b>
 * <ul>
 *   <li>Works with {@link ScenarioBuilder} for building and managing the main UI panels.</li>
 *   <li>Uses {@link GLIMPSEVariables}, {@link GLIMPSEFiles}, {@link GLIMPSEStyles}, and {@link glimpseUtil.GLIMPSEUtils} for configuration and utility functions.</li>
 *   <li>Integrates with menu setup classes (e.g., {@link SetupMenuFile}, {@link SetupMenuEdit}, etc.).</li>
 *   <li>Provides access to execution threads for running GCAM and post-processing tasks.</li>
 * </ul>
 *
 * <b>Example:</b>
 * <pre>
 * // Launch the Scenario Builder application
 * Client.main(new String[] {"-options", "options_GCAM-global-8.2.txt"});
 * </pre>
 *
 */
public class Client extends Application {

	// version
	private static final String VERSION = "GLIMPSE-CE ScenarioBuilder";
	
    // region Constants
    private static final double MIN_WINDOW_HEIGHT = 850;
    private static final double MIN_WINDOW_WIDTH = 1100;
    private static final double SPLASH_WIDTH = 383.0;
    private static final double SPLASH_HEIGHT = 384.0;
    private static final String OPTIONS_ARG_FLAG = "-options";
    // endregion

    // region Static Fields
    static Stage primaryStage;
    private static String optionsFilename = null;
    public static boolean exit_on_exception = false; // Retained public for potential external access
    // endregion

    // region GUI Panels
    static PaneCreateScenario paneCreateScenario;
    static PaneScenarioLibrary paneScenarioLibrary;
    static PaneNewScenarioComponent paneComponentLibrary;
    // endregion

    // region GUI Buttons
    // Arrow buttons between the top right/left pane
    static Button buttonRightArrow;
    static Button buttonLeftArrow;
    static Button buttonLeftDoubleArrow;
    static Button buttonEditScenario;

    // Buttons on the top left pane
    static Button buttonDeleteComponent;
    static Button buttonRefreshComponents;
    static Button buttonNewComponent;
    static Button buttonEditComponent;
    static Button buttonBrowseComponentLibrary;

    // Buttons on the top right pane
    static Button buttonMoveComponentUp;
    static Button buttonMoveComponentDown;
    static Button buttonCreateScenarioConfigFile;

    // Buttons on the bottom pane
    static Button buttonViewConfig;
    static Button buttonViewLog;
    static Button buttonViewExeLog;
    static Button buttonViewErrors;
    static Button buttonViewExeErrors;
    static Button buttonBrowseScenarioFolder;
    public static Button buttonImportScenario;
    static Button buttonDiffFiles;
    static Button buttonShowRunQueue;
    public static Button buttonRefreshScenarioStatus;
    static Button buttonConsole;
    static Button buttonDeleteScenario;
    static Button buttonRunScenario;
    static Button buttonResults;
    static Button buttonResultsForSelected;
    public static Button buttonArchiveScenario;
    public static Button buttonReport;
    public static Button buttonExamineScenario;
    // endregion

    // region GCAM Threads
    public static ExecutionThread gCAMExecutionThread;
    public static ExecutionThread modelInterfaceExecutionThread;
    // endregion

    // region Instance Variables
    private final ScenarioBuilder scenarioBuilder = ScenarioBuilder.getInstance();
    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    private final glimpseUtil.GLIMPSEUtils utils = glimpseUtil.GLIMPSEUtils.getInstance();
    private final StatusBar sb = new StatusBar();
    // endregion

    /**
     * The entry point of the application. Sets up JavaFX and launches the GUI.
     *
     * Handles command-line arguments and starts the JavaFX application lifecycle.
     *
     * @param args Command line arguments passed to the application. Supports an options file via -options flag or as a single argument.
     */
    public static void main(String[] args) {
        // Ensures JavaFX does not exit implicitly on certain VMs (e.g., when WM_ENDSESSION is called).
        Platform.setImplicitExit(false);
        launch(args);
    }

    /**
     * Initializes the application. Loads settings, options, and data files, and sets up utility references.
     *
     * Initializes all singleton utility classes and loads user options and data files. Also logs system information.
     *
     * @throws Exception if initialization fails
     */
    @Override
    public void init() throws Exception {
        // Install console redirection as early as possible so startup prints are captured.
        ConsoleOutputRedirect.install();

        System.out.println("Loading settings and initializing.");
        // Initialize utility/variable objects with references to each other
        vars.init(utils, vars, styles, files);
        files.init(utils, vars, styles, files);
        utils.init(utils, vars, styles, files);
        // Parse command-line arguments for options file
        processArgs();
        // Load options into the vars singleton
        vars.loadOptions(optionsFilename);
        final String setup = vars.examineGLIMPSESetup();
        if (setup.length() > 0) {
            System.out.println(setup);
        }
        // Reset log file and log computer stats
        utils.resetLogFile(utils.getComputerStatString());
        // Load data files into files singleton
        files.loadFiles();
        utils.sb = this.sb;
    }

    /**
     * Starts the JavaFX application and sets up the main window and GUI components.
     *
     * Initializes the main window, sets up event handlers for shutdown, builds all panels, and configures the application icon.
     *
     * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting GLIMPSE Graphical User Interface...");
        Client.primaryStage = primaryStage;
        // Ensure threads are properly terminated on window close
        primaryStage.setOnCloseRequest(event -> {
            // Terminate status checkers and shutdown execution threads
            // If any UI updates are needed here in the future, use Platform.runLater
            Client.gCAMExecutionThread.getStatusChecker().terminate();
            Client.modelInterfaceExecutionThread.getStatusChecker().terminate();
            Client.gCAMExecutionThread.shutdownNow();
            Client.modelInterfaceExecutionThread.shutdownNow();
            Platform.exit();
        });
        // Build GUI panels and layout
        getScenarioBuilder().build();
        // Set up the main window with menu and content
        setMainWindow(combineAllElementsIntoOnePane(), createMenuBar());
        // Set up execution threads for GCAM and post-processor
        setupExecutionThreads();
        // Set application icon
        final String iconFile = "file:" + vars.getGlimpseResourceDir() + File.separator + "GLIMPSE_icon_large.png";
        primaryStage.getIcons().add(new Image(iconFile));
    }

    /**
     * Processes command-line arguments to extract the options filename if provided.
     * Supports both single argument and -options flag.
     *
     * Sets the static optionsFilename field if an options file is specified.
     */
    private void processArgs() {
        final Parameters params = getParameters();
        final List<String> paramList = params.getRaw();
        if (paramList.isEmpty()) {
            return;
        }
        if (paramList.size() == 1) {
            optionsFilename = paramList.get(0);
        } else {
            for (int i = 0; i < paramList.size(); i++) {
                if (OPTIONS_ARG_FLAG.equalsIgnoreCase(paramList.get(i)) && i + 1 < paramList.size()) {
                    optionsFilename = paramList.get(i + 1);
                    break;
                }
            }
        }
    }

    /**
     * Creates the main menu bar for the application, including File, Edit, Tools, View, and Help menus.
     *
     * Each menu is set up using its corresponding setup class.
     *
     * @return MenuBar the constructed menu bar
     */
    private MenuBar createMenuBar() {
        final MenuBar menuBar = new MenuBar();
        // File menu
        final Menu menuFile = new Menu("File");
        new SetupMenuFile().setup(menuFile);
        // Edit menu
        final Menu menuEdit = new Menu("Edit");
        new SetupMenuEdit().setup(menuEdit);
        // Tools menu
        final Menu menuTools = new Menu("Tools");
        new SetupMenuTools().setup(menuTools);
        // View menu
        final Menu menuView = new Menu("View");
        new SetupMenuView().setup(menuView);
        // Help menu
        final Menu menuHelp = new Menu("Help");
        new SetupMenuHelp().setup(menuHelp);
        // Add all menus to the menu bar
        menuBar.getMenus().addAll(menuFile, menuEdit, menuView, menuTools, menuHelp);
        return menuBar;
    }

    /**
     * Combines all main GUI elements into a single GridPane for the main window layout.
     *
     * Adds the component library, button panel, create scenario panel, and run panel to the main layout.
     *
     * @return GridPane containing all main UI elements
     */
    private GridPane combineAllElementsIntoOnePane() {
        final GridPane mainGridPane = new GridPane();
        // Add component library, button panel, and create scenario panel
        mainGridPane.add(getScenarioBuilder().getvBoxComponentLibrary(), 0, 0);
        mainGridPane.add(getScenarioBuilder().getvBoxButton(), 1, 0);
        mainGridPane.add(getScenarioBuilder().getvBoxCreateScenario(), 3, 0);

        // Add run panel at the bottom, spanning all columns
        final HBox stack = new HBox(20);
        stack.getChildren().addAll(getScenarioBuilder().getvBoxRun());
        stack.prefWidthProperty().bind(primaryStage.widthProperty());
        stack.setStyle(styles.getStyle1());
        mainGridPane.add(stack, 0, 1, 4, 1);

        return mainGridPane;
    }

    /**
     * Sets up the main application window with the provided layout and menu bar.
     *
     * Configures the root layout, scene, window size, and optionally displays the splash screen.
     *
     * @param mainGridPane The main content pane
     * @param menuBar The menu bar
     */
    private void setMainWindow(GridPane mainGridPane, MenuBar menuBar) {
        // Compose the root layout
        sb.setStyle(styles.getBackgroundStyle() + " -fx-padding: 5; -fx-border-color: #B0B0B0; -fx-border-width: 1 0 0 0;");
        final VBox root = new VBox(menuBar, mainGridPane, sb);
        final Scene scene = new Scene(root, vars.DEFAULT_SCENARIO_BUILDER_WIDTH, vars.DEFAULT_SCENARIO_BUILDER_HEIGHT);

        // Apply Modern CSS
        try {
            java.net.URL cssUrl = getClass().getResource("/resources/modern.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Could not find modern.css resource.");
            }
        } catch (Exception e) {
            System.err.println("Error loading modern.css: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle(VERSION);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setWidth(MIN_WINDOW_WIDTH);
        primaryStage.show();

        // Optionally show splash screen on startup
        if (vars.getShowSplash()) {
            loadSplashScreen();
        }
    }

    /**
     * Sets up the execution threads for GCAM and the model interface.
     * GCAM uses a single-threaded executor, while the model interface uses a multi-threaded executor.
     *
     * Initializes and starts the execution queues for both GCAM and post-processing.
     */
    private void setupExecutionThreads() {
        // Starting separate execution queues for GCAM and post-processor.
        gCAMExecutionThread = new ExecutionThread();
        modelInterfaceExecutionThread = new ExecutionThread();

        gCAMExecutionThread.startUpExecutorSingle();
        modelInterfaceExecutionThread.startUpExecutorMulti();
    }

    /**
     * Loads and displays the splash screen with fade-in and fade-out effects.
     *
     * Shows a splash image on startup if enabled in user options. Handles errors gracefully if the image is missing.
     *
     * @return true if splash screen loaded successfully, false otherwise
     */
    private boolean loadSplashScreen() {
        try {
            final Stage splashStage = new Stage();
            final VBox splashRoot = new VBox();
            final Scene splashScene = new Scene(splashRoot, SPLASH_WIDTH, SPLASH_HEIGHT, Color.TRANSPARENT);

            splashStage.setScene(splashScene);
            splashStage.centerOnScreen();
            splashStage.initOwner(primaryStage);
            splashStage.initModality(Modality.WINDOW_MODAL);
            splashStage.initStyle(StageStyle.TRANSPARENT);
            splashStage.setOpacity(0.9);

            final GridPane pane = new GridPane();
            // Use lowercase 'file:' for cross-platform compatibility
            final String imagePath = "file:" + vars.getGlimpseDir() + File.separator + "resources" + File.separator + "glimpse-splash.png";
            final Image image = new Image(imagePath);

            if (image.isError()) {
                System.err.println("Could not find splash graphic. Continuing without splash screen.");
                return false;
            }

            pane.getChildren().add(new ImageView(image));
            splashRoot.getChildren().add(pane);
            splashRoot.setStyle("-fx-background-color: transparent;");
            splashStage.show();

            // Fade in effect
            final FadeTransition fadeIn = new FadeTransition(Duration.seconds(3), pane);
            fadeIn.setFromValue(0.1);
            fadeIn.setToValue(1);

            // Fade out effect
            final FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), pane);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            // After fade in, start fade out
            fadeIn.setOnFinished(e -> fadeOut.play());

            // After fade out, hide the splash screen
            fadeOut.setOnFinished(e -> splashStage.hide());

            fadeIn.play();

        } catch (Exception ex) {
            System.err.println("An error occurred while loading the splash screen.");
            ex.printStackTrace();
        }
        return true;
    }

    /**
     * Sets the text of the status bar at the bottom of the application window.
     * If called from a background thread, wraps the update in Platform.runLater.
     *
     * Used throughout the application to provide user feedback and status updates.
     *
     * @param text The text to display in the status bar
     */
    protected void setStatusBarText(String text) {
        if (Platform.isFxApplicationThread()) {
            sb.setText(text);
        } else {
            Platform.runLater(() -> sb.setText(text));
        }
    }
    
    // region Getters for private static fields
    /**
     * Gets the primary application stage.
     * @return Stage the primary stage
     */
    public static Stage getPrimaryStage() { return primaryStage; }
    /**
     * Gets the options filename provided via command-line arguments.
     * @return String the options filename
     */
    public static String getOptionsFilename() { return optionsFilename; }
    /**
     * Gets the pane for creating scenarios.
     * @return PaneCreateScenario the create scenario pane
     */
    public static PaneCreateScenario getPaneCreateScenario() { return paneCreateScenario; }
    /**
     * Gets the pane for working scenarios.
     * @return PaneScenarioLibrary the working scenarios pane
     */
    public static PaneScenarioLibrary getPaneScenarioLibrary() { return paneScenarioLibrary; }
    /**
     * Gets the pane for candidate scenario components.
     * @return PaneNewScenarioComponent the candidate components pane
     */
    public static PaneNewScenarioComponent getPaneComponentLibrary() { return paneComponentLibrary; }
    /**
     * Gets the right arrow button.
     * @return Button the right arrow button
     */
    public static Button getButtonRightArrow() { return buttonRightArrow; }
    /**
     * Gets the left arrow button.
     * @return Button the left arrow button
     */
    public static Button getButtonLeftArrow() { return buttonLeftArrow; }
    /**
     * Gets the left double arrow button.
     * @return Button the left double arrow button
     */
    public static Button getButtonLeftDoubleArrow() { return buttonLeftDoubleArrow; }
    /**
     * Gets the edit scenario button.
     * @return Button the edit scenario button
     */
    public static Button getButtonEditScenario() { return buttonEditScenario; }
    /**
     * Gets the delete component button.
     * @return Button the delete component button
     */
    public static Button getButtonDeleteComponent() { return buttonDeleteComponent; }
    /**
     * Gets the refresh components button.
     * @return Button the refresh components button
     */
    public static Button getButtonRefreshComponents() { return buttonRefreshComponents; }
    /**
     * Gets the new component button.
     * @return Button the new component button
     */
    public static Button getButtonNewComponent() { return buttonNewComponent; }
    /**
     * Gets the edit component button.
     * @return Button the edit component button
     */
    public static Button getButtonEditComponent() { return buttonEditComponent; }
    /**
     * Gets the browse component library button.
     * @return Button the browse component library button
     */
    public static Button getButtonBrowseComponentLibrary() { return buttonBrowseComponentLibrary; }
    /**
     * Gets the move component up button.
     * @return Button the move component up button
     */
    public static Button getButtonMoveComponentUp() { return buttonMoveComponentUp; }
    /**
     * Gets the move component down button.
     * @return Button the move component down button
     */
    public static Button getButtonMoveComponentDown() { return buttonMoveComponentDown; }
    /**
     * Gets the create scenario config file button.
     * @return Button the create scenario config file button
     */
    public static Button getButtonCreateScenarioConfigFile() { return buttonCreateScenarioConfigFile; }
    /**
     * Gets the view config button.
     * @return Button the view config button
     */
    public static Button getButtonViewConfig() { return buttonViewConfig; }
    /**
     * Gets the view log button.
     * @return Button the view log button
     */
    public static Button getButtonViewLog() { return buttonViewLog; }
    /**
     * Gets the view exe log button.
     * @return Button the view exe log button
     */
    public static Button getButtonViewExeLog() { return buttonViewExeLog; }
    /**
     * Gets the view errors button.
     * @return Button the view errors button
     */
    public static Button getButtonViewErrors() { return buttonViewErrors; }
    /**
     * Gets the view exe errors button.
     * @return Button the view exe errors button
     */
    public static Button getButtonViewExeErrors() { return buttonViewExeErrors; }
    /**
     * Gets the browse scenario folder button.
     * @return Button the browse scenario folder button
     */
    public static Button getButtonBrowseScenarioFolder() { return buttonBrowseScenarioFolder; }
    /**
     * Gets the import scenario button.
     * @return Button the import scenario button
     */
    public static Button getButtonImportScenario() { return buttonImportScenario; }
    /**
     * Gets the diff files button.
     * @return Button the diff files button
     */
    public static Button getButtonDiffFiles() { return buttonDiffFiles; }
    /**
     * Gets the show run queue button.
     * @return Button the show run queue button
     */
    public static Button getButtonShowRunQueue() { return buttonShowRunQueue; }
    /**
     * Gets the refresh scenario status button.
     * @return Button the refresh scenario status button
     */
    public static Button getButtonRefreshScenarioStatus() { return buttonRefreshScenarioStatus; }
    /**
     * Gets the delete scenario button.
     * @return Button the delete scenario button
     */
    public static Button getButtonDeleteScenario() { return buttonDeleteScenario; }
    /**
     * Gets the run scenario button.
     * @return Button the run scenario button
     */
    public static Button getButtonRunScenario() { return buttonRunScenario; }
    /**
     * Gets the results button.
     * @return Button the results button
     */
    public static Button getButtonResults() { return buttonResults; }
    /**
     * Gets the results for selected button.
     * @return Button the results for selected button
     */
    public static Button getButtonResultsForSelected() { return buttonResultsForSelected; }
    /**
     * Gets the archive scenario button.
     * @return Button the archive scenario button
     */
    public static Button getButtonArchiveScenario() { return buttonArchiveScenario; }
    /**
     * Gets the report button.
     * @return Button the report button
     */
    public static Button getButtonReport() { return buttonReport; }
    /**
     * Gets the examine scenario button.
     * @return Button the examine scenario button
     */
    public static Button getButtonExamineScenario() { return buttonExamineScenario; }
    /**
     * Gets the GCAM execution thread.
     * @return ExecutionThread the GCAM execution thread
     */
    public static ExecutionThread getgCAMExecutionThread() { return gCAMExecutionThread; }
    /**
     * Gets the model interface execution thread.
     * @return ExecutionThread the model interface execution thread
     */
    public static ExecutionThread getgCAMPPExecutionThread() { return modelInterfaceExecutionThread; }
    // endregion

	public ScenarioBuilder getScenarioBuilder() {
		return scenarioBuilder;
	}


}