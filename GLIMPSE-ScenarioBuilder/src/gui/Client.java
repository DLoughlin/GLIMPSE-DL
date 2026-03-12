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
import glimpseUtil.WindowsRuntimePreflight;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
	private static final String STATUS_BAR_BASE_STYLE = " -fx-padding: 6 10 6 10; -fx-border-color: #e0e0e0 transparent transparent transparent; -fx-border-width: 1 0 0 0;";
	private static final double STATUS_BAR_OPERATION_PROGRESS_WIDTH = 120.0;
	private static final double STATUS_BAR_OPERATION_PROGRESS_HEIGHT = 12.0;
	
    // region Constants
    private static final double MIN_WINDOW_HEIGHT = 850;
    private static final double MIN_WINDOW_WIDTH = 1100;
    private static final double SPLASH_WIDTH = 383.0;
    private static final double SPLASH_HEIGHT = 384.0;
    private static final String OPTIONS_ARG_FLAG = "-options";
    private static final String STARTUP_READY_MESSAGE = "Ready";
    private static final String SCENARIO_REFRESHED_MESSAGE = "Scenario status refreshed.";
    private static final String STARTUP_FILES_MESSAGE = "Loading required files...";
    private static final String STARTUP_UI_MESSAGE = "Building window layout...";
    private static final String STARTUP_SCENARIO_MESSAGE = "Loading scenario status...";
    private static final String STARTUP_COMPONENT_MESSAGE = "Loading scenario components...";
    private static final String STARTUP_DIALOG_TITLE = "GLIMPSE startup";
    private static final String STARTUP_DIALOG_HEADING = "GLIMPSE startup:";
    private static final double STARTUP_DIALOG_ICON_SIZE = 28.0;
    // endregion

    // region Static Fields
    static Stage primaryStage;
    private static String optionsFilename = null;
    public static boolean exit_on_exception = false; // Retained public for potential external access
    // endregion

    // region GUI Panels
    static PaneCreateScenario paneCreateScenario;
    static PaneScenarioLibrary paneScenarioLibrary;
    static PaneComponentLibrary paneComponentLibrary;
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
    static Button buttonStopScenario;
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
    private final AtomicInteger activeScenarioOperationCount = new AtomicInteger(0);
    private ProgressBar scenarioOperationProgressBar;

    /** Startup timing anchor (nanoseconds). */
    private static final long STARTUP_T0_NANOS = System.nanoTime();
    // endregion

    /** True once GLIMPSEFiles.loadFiles() has completed successfully (or at least attempted). */
    private static volatile boolean filesLoaded = false;

    /** Returns whether required GLIMPSEFiles content has been loaded. */
    public static boolean isFilesLoaded() {
        return filesLoaded;
    }

    /** Keep a global handle so deferred/background tasks can update the status bar safely. */
    private static Client instanceForStatus;

    /** Explicit startup completion state. */
    private static volatile boolean startupBusyState = true;
    /** Cached post-startup status message. */
    private static volatile String deferredStatusBarText;
    /** Allows the lightweight startup dialog to stay dismissed for the rest of the session once the user closes it. */
    private static volatile boolean startupDialogDismissed = false;
    /** Initial library loads that must finish before steady-state resource text is restored. */
    private static volatile boolean initialScenarioLoadPending = true;
    private static volatile boolean initialComponentLoadPending = true;

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

        final long t0 = System.nanoTime();
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
        String glimpseLogDir = vars.getGlimpseLogDir();
        if (glimpseLogDir != null && glimpseLogDir.trim().length() > 0) {
            String glimpseLogFilename = glimpseLogDir + File.separator + "glimpse_log.txt";
            utils.resetLogFile(glimpseLogFilename);
            files.appendTextToFile(utils.getComputerStatString() + vars.getEol(), glimpseLogFilename);
        } else {
            System.out.println("Warning: glimpseLogDir not set; skipping log reset.");
        }

        // NOTE: Intentionally NOT calling files.loadFiles() here.
        // It can be heavy and would delay first window paint.

        utils.sb = this.sb;
        instanceForStatus = this;

        logStartupCheckpoint("Client.init complete", t0);
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
        final long t0 = System.nanoTime();
        System.out.println("Starting GLIMPSE Graphical User Interface...");
        Client.primaryStage = primaryStage;

        // Ensure threads are properly terminated on window close
        primaryStage.setOnCloseRequest(event -> {
            // Don't let exceptions prevent shutdown.
            safeShutdownExecutionThreads();
            Platform.exit();
        });

        setStartupStatus(STARTUP_UI_MESSAGE, -1, true);
        getScenarioBuilder().build();

        setFileDependentUiEnabled(false);
        setStartupStatus(STARTUP_FILES_MESSAGE, -1, true);

        // Set up the main window with menu and content (this calls primaryStage.show())
        setMainWindow(combineAllElementsIntoOnePane(), createMenuBar());
        hideStartupDialog();

        // Set up execution threads for GCAM and post-processor
        setupExecutionThreads();

        // Set application icon
        final String iconFile = "file:" + vars.getGlimpseResourceDir() + File.separator + "GLIMPSE_icon_large.png";
        primaryStage.getIcons().add(new Image(iconFile));

        // Log time-to-window and schedule post-show work.
        Platform.runLater(() -> {
            logStartupCheckpoint("Primary stage shown (first FX pulse after show)", STARTUP_T0_NANOS);
            WindowsRuntimePreflight.ensureMsvcRuntimeAvailableOrWarn(utils, "Startup");
            setStartupStatus(STARTUP_SCENARIO_MESSAGE, -1, true);
            startDeferredFileLoading();
        });

        logStartupCheckpoint("Client.start complete", t0);
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
		// No horizontal whitespace between the left pane, arrow buttons, and create-scenario pane.
		mainGridPane.setHgap(0);

		// Layout columns so the arrow buttons stay centered between the left and right panes.
		ColumnConstraints leftCol = new ColumnConstraints();
		leftCol.setHgrow(Priority.ALWAYS);

		ColumnConstraints arrowCol = new ColumnConstraints();
		arrowCol.setHgrow(Priority.NEVER);
		arrowCol.setFillWidth(true);
		arrowCol.setHalignment(javafx.geometry.HPos.CENTER);

		// Column 2 is intentionally empty (a spacer) so the right pane can be pushed further right.
		ColumnConstraints spacerCol = new ColumnConstraints();
		spacerCol.setHgrow(Priority.ALWAYS);
		spacerCol.setMinWidth(0);

		ColumnConstraints rightCol = new ColumnConstraints();
		rightCol.setHgrow(Priority.ALWAYS);

		mainGridPane.getColumnConstraints().setAll(leftCol, arrowCol, spacerCol, rightCol);

        // Give the bottom (scenario library) row more vertical space so the divider sits lower.
        javafx.scene.layout.RowConstraints topRow = new javafx.scene.layout.RowConstraints();
        topRow.setVgrow(Priority.ALWAYS);
        topRow.setPercentHeight(45);

        javafx.scene.layout.RowConstraints bottomRow = new javafx.scene.layout.RowConstraints();
        bottomRow.setVgrow(Priority.ALWAYS);
        bottomRow.setPercentHeight(55);

        mainGridPane.getRowConstraints().setAll(topRow, bottomRow);

        // Add component library, button panel, and create scenario panel
        mainGridPane.add(getScenarioBuilder().getvBoxComponentLibrary(), 0, 0);
        VBox arrowBox = getScenarioBuilder().getvBoxButton();
        mainGridPane.add(arrowBox, 1, 0);
		// Add an empty spacer at column 2 so the grid recognizes the 4-column layout.
		Region spacer = new Region();
		mainGridPane.add(spacer, 2, 0);
        mainGridPane.add(getScenarioBuilder().getvBoxCreateScenario(), 3, 0);

        // Add run panel at the bottom, spanning all columns
        final HBox stack = new HBox(10);
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
        sb.setStyle(buildStatusBarStyle("-fx-text-fill: black;"));
        configureStatusBarRightItems();
        final StackPane centerStack = new StackPane();
        centerStack.getChildren().add(mainGridPane);
        startupOverlayBox = createStartupOverlay();
        centerStack.getChildren().add(startupOverlayBox);
        StackPane.setAlignment(startupOverlayBox, Pos.TOP_LEFT);
        StackPane.setMargin(startupOverlayBox, new Insets(12, 12, 0, 12));
        final VBox root = new VBox(menuBar, centerStack, sb);
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
        utils.setModalDialogsReadyAndFlushWarnings();

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

        // Stream GCAM process output live to the GCAM console tab.
        // (The GCAM stderr tab was removed; ConsoleManager routes GCAM_STDERR to GCAM_STDOUT internally.)
        gCAMExecutionThread.setConsoleStreamTarget(ConsoleManager.StreamSource.GCAM_STDOUT);

        // Stream ModelInterface process output live to the in-app console tab.
        modelInterfaceExecutionThread.setConsoleStreamTarget(ConsoleManager.StreamSource.MODEL_INTERFACE);
    }

    /**
     * Loads GLIMPSEFiles on a background thread so UI can show quickly.
     * This is safe as long as GLIMPSEFiles.loadFiles() does not touch JavaFX objects.
     */
    private void startDeferredFileLoading() {
        final Thread t = new Thread(() -> {
            final long t0 = System.nanoTime();
            try {
                System.out.println("Loading GLIMPSE files (deferred)...");
                setStartupStatus(STARTUP_FILES_MESSAGE, -1, true);
                files.loadFiles();
                filesLoaded = true;
                logStartupCheckpoint("files.loadFiles complete (deferred)", t0);

                Platform.runLater(() -> {
                    try {
                        setStartupStatus(STARTUP_COMPONENT_MESSAGE, -1, true);
                        if (Client.getPaneComponentLibrary() != null) {
                            Client.getPaneComponentLibrary().refreshComponentLibraryTable();
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        setStartupStatus(STARTUP_SCENARIO_MESSAGE, -1, true);
                        if (Client.getPaneScenarioLibrary() != null) {
                            Client.getPaneScenarioLibrary().refreshScenarioStatusAsync(false);
                        }
                    } catch (Throwable ignored) {
                    }
                    setFileDependentUiEnabled(true);
                });

            } catch (Throwable ex) {
                System.err.println("Deferred file loading failed: " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    setFileDependentUiEnabled(true);
                    setStartupStatus("Error loading required files (see console)", -1, false);
                });
            }
        }, "glimpse-deferred-file-loader");
        t.setDaemon(true);
        t.start();
    }

    /** Updates the status bar text (safe from any thread). */
    public static void setStartupStatus(String text, double progress, boolean busy) {
        final Client inst = instanceForStatus;
        startupBusyState = busy;
        final String safeText = (text == null || text.trim().isEmpty()) ? STARTUP_READY_MESSAGE : text.trim();
        if (inst == null) {
            return;
        }
        Runnable update = () -> inst.applyStartupStatus(safeText, progress, busy);
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void applyStartupStatus(String text, double progress, boolean busy) {
        String safeText = (text == null || text.trim().isEmpty()) ? STARTUP_READY_MESSAGE : text.trim();
        sb.setText(safeText);
        if (startupOverlayLabel != null) {
            startupOverlayLabel.setText(safeText);
        }
        if (startupOverlayIndicator != null) {
            if (progress >= 0.0 && progress <= 1.0) {
                startupOverlayIndicator.setProgress(progress);
            } else {
                startupOverlayIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            }
        }
        updateStartupDialogStatus(safeText, progress, busy);
        boolean showOverlay = busy && !STARTUP_READY_MESSAGE.equalsIgnoreCase(safeText);
        startupOverlayVisible.set(showOverlay);
        if (startupOverlayBox != null) {
            startupOverlayBox.setVisible(showOverlay);
            startupOverlayBox.toFront();
        }
        if (!busy && shouldApplyDeferredStatusAfter(safeText)) {
            applyDeferredStatusBarTextIfReady();
        }
    }

    private boolean shouldApplyDeferredStatusAfter(String text) {
        if (text == null) {
            return false;
        }
        String safeText = text.trim();
        return STARTUP_READY_MESSAGE.equalsIgnoreCase(safeText)
                || SCENARIO_REFRESHED_MESSAGE.equalsIgnoreCase(safeText);
    }

    public static void markInitialScenarioLoadComplete() {
        initialScenarioLoadPending = false;
        applyDeferredStatusBarTextIfReady();
    }

    public static void markInitialComponentLoadComplete() {
        initialComponentLoadPending = false;
        applyDeferredStatusBarTextIfReady();
    }

    private static boolean areInitialLibraryLoadsPending() {
        return initialScenarioLoadPending || initialComponentLoadPending;
    }

    public static boolean isStartupBusy() {
        return startupBusyState;
    }

    public static void setDeferredStatusBarText(String text, String style) {
        final String safeText = (text == null) ? "" : text.trim();
        if (safeText.isEmpty()) {
            return;
        }
        deferredStatusBarText = safeText + "\n" + ((style == null || style.trim().isEmpty()) ? "-fx-text-fill: black;" : style.trim());
        applyDeferredStatusBarTextIfReady();
    }

    private static void applyDeferredStatusBarTextIfReady() {
        if (startupBusyState || areInitialLibraryLoadsPending()) {
            return;
        }
        final Client inst = instanceForStatus;
        final String pending = deferredStatusBarText;
        if (inst == null || pending == null || pending.trim().isEmpty()) {
            return;
        }
        Runnable update = () -> {
            String[] parts = pending.split("\\n", 2);
            inst.sb.setText(parts[0]);
            inst.sb.setStyle(inst.buildStatusBarStyle(parts.length > 1 ? parts[1] : "-fx-text-fill: black;"));
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    /**
     * Creates the overlay that shows startup progress and messages.
     *
     * @return VBox the overlay container
     */
    private VBox createStartupOverlay() {
        startupOverlayLabel = new Label("Starting...");
        startupOverlayLabel.setWrapText(true);
        startupOverlayIndicator = new ProgressIndicator();
        startupOverlayIndicator.setMaxSize(28, 28);
        startupOverlayIndicator.setPrefSize(28, 28);
        HBox row = new HBox(10, startupOverlayIndicator, startupOverlayLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox overlay = new VBox(row);
        overlay.setMouseTransparent(true);
        overlay.setManaged(false);
        overlay.setVisible(false);
        overlay.setMaxWidth(420);
        overlay.setStyle("-fx-background-color: rgba(255,255,255,0.94); -fx-background-radius: 8; -fx-padding: 10 14 10 14; -fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 8;");
        return overlay;
    }

    private void ensureStartupDialog() {
        if (startupDialogStage != null) {
            return;
        }
        startupDialogStage = new Stage(StageStyle.UTILITY);
        startupDialogStage.setTitle(STARTUP_DIALOG_TITLE);
        startupDialogStage.initModality(Modality.NONE);
        startupDialogStage.setResizable(false);
        startupDialogStage.setAlwaysOnTop(true);
        try {
            final String iconFile = "file:" + vars.getGlimpseResourceDir() + File.separator + "GLIMPSE_icon_large.png";
            Image stageIcon = new Image(iconFile);
            if (!stageIcon.isError()) {
                startupDialogStage.getIcons().add(stageIcon);
            }
        } catch (Exception ignored) {}
        if (primaryStage != null) {
            startupDialogStage.initOwner(primaryStage);
        }

        Label heading = new Label(STARTUP_DIALOG_HEADING);
        heading.setStyle("-fx-font-weight: bold;");
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        ImageView startupIconView = createStartupDialogIconView();
        if (startupIconView != null) {
            headerRow.getChildren().add(startupIconView);
        }
        headerRow.getChildren().add(heading);

        startupDialogStatusLabel = new Label(STARTUP_READY_MESSAGE);
        startupDialogStatusLabel.setWrapText(true);
        startupDialogStatusLabel.setMinWidth(280);
        startupDialogStatusLabel.setPrefWidth(320);
        startupDialogStatusLabel.setMaxWidth(360);

        startupDialogIndicator = new ProgressIndicator();
        startupDialogIndicator.setMaxSize(22, 22);
        startupDialogIndicator.setPrefSize(22, 22);
        startupDialogIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        HBox statusRow = new HBox(10, startupDialogIndicator, startupDialogStatusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button dismissButton = new Button("Dismiss");
        dismissButton.setDefaultButton(false);
        dismissButton.setCancelButton(true);
        dismissButton.setOnAction(event -> {
            startupDialogDismissed = true;
            hideStartupDialog();
        });

        HBox buttonRow = new HBox(dismissButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, headerRow, statusRow, new Separator(), buttonRow);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: white;");

        Scene dialogScene = new Scene(root);
        startupDialogStage.setScene(dialogScene);
        startupDialogStage.sizeToScene();
        startupDialogStage.setOnCloseRequest(event -> startupDialogDismissed = true);
        startupDialogStage.setOnHidden(event -> {
            if (!startupBusyState) {
                startupDialogDismissed = false;
            }
        });
    }

    private ImageView createStartupDialogIconView() {
        try {
            final String iconFile = "file:" + vars.getGlimpseResourceDir() + File.separator + "GLIMPSE_icon_large.png";
            Image image = new Image(iconFile);
            if (image.isError()) {
                return null;
            }
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(STARTUP_DIALOG_ICON_SIZE);
            imageView.setFitHeight(STARTUP_DIALOG_ICON_SIZE);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            return imageView;
        } catch (Exception ignored) {}
        return null;
    }

    private void updateStartupDialogStatus(String text, double progress, boolean busy) {
        if (startupDialogDismissed && busy) {
            return;
        }
        ensureStartupDialog();
        if (startupDialogStatusLabel != null) {
            startupDialogStatusLabel.setText(text);
        }
        if (startupDialogIndicator != null) {
            if (progress >= 0.0 && progress <= 1.0) {
                startupDialogIndicator.setProgress(progress);
            } else {
                startupDialogIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            }
        }
        if (!busy) {
            hideStartupDialog();
            return;
        }
        if (startupDialogDismissed) {
            return;
        }
        showStartupDialog();
    }

    private void showStartupDialog() {
        if (startupDialogStage == null || startupDialogDismissed) {
            return;
        }
        if (!startupDialogStage.isShowing()) {
            startupDialogStage.show();
        }
        startupDialogStage.toFront();
    }

    private void hideStartupDialog() {
        if (startupDialogStage != null && startupDialogStage.isShowing()) {
            startupDialogStage.hide();
        }
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

            final FadeTransition fadeIn = new FadeTransition(Duration.seconds(3), pane);
            fadeIn.setFromValue(0.1);
            fadeIn.setToValue(1);

            final FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), pane);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeOut.setOnFinished(e -> splashStage.hide());
            fadeIn.play();
        } catch (Exception ex) {
            System.err.println("An error occurred while loading the splash screen.");
            ex.printStackTrace();
        }
        return true;
    }

    /** Safely terminates/interrupts execution threads if they exist. */
    private static void safeShutdownExecutionThreads() {
        try {
            if (Client.gCAMExecutionThread != null) {
                try {
                    if (Client.gCAMExecutionThread.getStatusChecker() != null) {
                        Client.gCAMExecutionThread.getStatusChecker().terminate();
                    }
                } catch (Throwable ignored) {
                    // Best-effort; continue shutdown.
                }
                try {
                    Client.gCAMExecutionThread.shutdownNow();
                } catch (Throwable ignored) {
                }
            }
        } finally {
            // Continue with model interface thread regardless.
            if (Client.modelInterfaceExecutionThread != null) {
                try {
                    if (Client.modelInterfaceExecutionThread.getStatusChecker() != null) {
                        Client.modelInterfaceExecutionThread.getStatusChecker().terminate();
                    }
                } catch (Throwable ignored) {
                }
                try {
                    Client.modelInterfaceExecutionThread.shutdownNow();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** Prints a consistent elapsed-time marker for startup profiling. */
    private static void logStartupCheckpoint(String label, long t0Nanos) {
        // Only emit noisy startup timing info when debug mode is enabled.
        try {
            if (!GLIMPSEVariables.getInstance().getDebug()) {
                return;
            }
        } catch (Throwable ignored) {
            // If vars isn't initialized yet for some reason, default to no logging.
            return;
        }

        final long now = System.nanoTime();
        final long msSinceT0 = (now - t0Nanos) / 1_000_000L;
        final long msSinceProcessStart = (now - STARTUP_T0_NANOS) / 1_000_000L;
        System.out.println("[startup] " + label + " | +" + msSinceT0 + "ms | total=" + msSinceProcessStart + "ms");
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
     * @return PaneComponentLibrary the candidate components pane
     */
    public static PaneComponentLibrary getPaneComponentLibrary() { return paneComponentLibrary; }
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
     * Gets the stop scenario button.
     * @return Button the stop scenario button
     */
    public static Button getButtonStopScenario() { return buttonStopScenario; }
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

    private final AtomicBoolean startupOverlayVisible = new AtomicBoolean(false);
    private Label startupOverlayLabel;
    private ProgressIndicator startupOverlayIndicator;
    private VBox startupOverlayBox;
    private Stage startupDialogStage;
    private Label startupDialogStatusLabel;
    private ProgressIndicator startupDialogIndicator;

    private static void setFileDependentUiEnabled(boolean enabled) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setFileDependentUiEnabled(enabled));
            return;
        }
        if (Client.buttonNewComponent != null) {
            Client.buttonNewComponent.setDisable(!enabled);
        }
        if (Client.buttonEditComponent != null) {
            Client.buttonEditComponent.setDisable(!enabled);
        }
        if (Client.buttonRefreshComponents != null) {
            Client.buttonRefreshComponents.setDisable(!enabled);
        }
    }

    private String buildStatusBarStyle(String extraStyle) {
        String normalizedExtraStyle = (extraStyle == null) ? "" : extraStyle.trim();
        if (!normalizedExtraStyle.isEmpty() && !normalizedExtraStyle.endsWith(";")) {
            normalizedExtraStyle = normalizedExtraStyle + ";";
        }
        return styles.getBackgroundStyle() + STATUS_BAR_BASE_STYLE + (normalizedExtraStyle.isEmpty() ? "" : " " + normalizedExtraStyle);
    }

    public static void beginScenarioOperationProgress() {
        final Client inst = instanceForStatus;
        if (inst == null) {
            return;
        }
        Runnable update = inst::incrementScenarioOperationProgress;
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    public static void endScenarioOperationProgress() {
        final Client inst = instanceForStatus;
        if (inst == null) {
            return;
        }
        Runnable update = inst::decrementScenarioOperationProgress;
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void configureStatusBarRightItems() {
        if (scenarioOperationProgressBar != null) {
            return;
        }
        scenarioOperationProgressBar = new ProgressBar();
        scenarioOperationProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        scenarioOperationProgressBar.setPrefWidth(STATUS_BAR_OPERATION_PROGRESS_WIDTH);
        scenarioOperationProgressBar.setMinWidth(STATUS_BAR_OPERATION_PROGRESS_WIDTH);
        scenarioOperationProgressBar.setMaxWidth(STATUS_BAR_OPERATION_PROGRESS_WIDTH);
        scenarioOperationProgressBar.setPrefHeight(STATUS_BAR_OPERATION_PROGRESS_HEIGHT);
        scenarioOperationProgressBar.setMaxHeight(STATUS_BAR_OPERATION_PROGRESS_HEIGHT);
        scenarioOperationProgressBar.setVisible(false);
        scenarioOperationProgressBar.setManaged(false);
        scenarioOperationProgressBar.setFocusTraversable(false);
        sb.getRightItems().setAll(scenarioOperationProgressBar);
    }

    private void incrementScenarioOperationProgress() {
        configureStatusBarRightItems();
        if (activeScenarioOperationCount.incrementAndGet() > 0) {
            scenarioOperationProgressBar.setVisible(true);
            scenarioOperationProgressBar.setManaged(true);
            scenarioOperationProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }
    }

    private void decrementScenarioOperationProgress() {
        configureStatusBarRightItems();
        int remaining = activeScenarioOperationCount.decrementAndGet();
        if (remaining <= 0) {
            activeScenarioOperationCount.set(0);
            scenarioOperationProgressBar.setVisible(false);
            scenarioOperationProgressBar.setManaged(false);
        }
    }
}
