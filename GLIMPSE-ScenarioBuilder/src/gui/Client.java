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
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;
import javafx.scene.Node;
import javafx.scene.Parent;

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
 * <p>
 * <b>Usage:</b> This class is launched as a JavaFX application. It is responsible for the lifecycle of the Scenario Builder GUI.
 * <p>
 * <b>Thread Safety:</b> Most methods must be called on the JavaFX Application Thread. Static accessors are provided for UI integration.
 * <p>
 * <b>Integration:</b>
 * <ul>
 *   <li>Works with {@link ScenarioBuilder} for building and managing the main UI panels.</li>
 *   <li>Uses {@link GLIMPSEVariables}, {@link GLIMPSEFiles}, {@link GLIMPSEStyles}, and {@link glimpseUtil.GLIMPSEUtils} for configuration and utility functions.</li>
 *   <li>Integrates with menu setup classes (e.g., {@link SetupMenuFile}, {@link SetupMenuEdit}, etc.).</li>
 *   <li>Provides access to execution threads for running GCAM and post-processing tasks.</li>
 * </ul>
 */
public class Client extends Application {

	// version
	private static final String VERSION = "GLIMPSE-CE ScenarioBuilder";
	private static final int MIN_RUNTIME_FONT_SIZE = 8;
	private static final int MAX_RUNTIME_FONT_SIZE = 24;
	private static final String STATUS_BAR_BASE_STYLE = " -fx-padding: 6 10 6 10; -fx-border-color: #e0e0e0 transparent transparent transparent; -fx-border-width: 1 0 0 0;";
	private static final String STATUS_BAR_DEFAULT_TEXT_STYLE = "-fx-text-fill: black;";
	private static final String STATUS_BAR_ALERT_TEXT_STYLE = "-fx-text-fill: red;";
	private static final double STATUS_BAR_OPERATION_PROGRESS_WIDTH = 120.0;
	private static final double STATUS_BAR_OPERATION_PROGRESS_HEIGHT = 12.0;
	private static final String RESOURCE_STATUS_PREFIX = "Resources...";
	
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
    private static final String STARTUP_BUILDING_UI_MESSAGE = "Setting up ScenarioBuilder panels...";
    private static final String STARTUP_WINDOW_READY_MESSAGE = "Configuring ScenarioBuilder window...";
    private static final String STARTUP_POST_SHOW_MESSAGE = "Finalizing ScenarioBuilder startup...";
    private static final String STARTUP_SCENARIO_MESSAGE = "Loading scenario status...";
    private static final String STARTUP_COMPONENT_MESSAGE = "Loading scenario components...";
    private static final String STARTUP_DIALOG_HEADING = "GLIMPSE startup:";
    private static final String STARTUP_SHELL_MESSAGE = "Starting ScenarioBuilder...";
    private static final String EARLY_SPLASH_INITIAL_MESSAGE = "Launching ScenarioBuilder...";
    private static final String EARLY_SPLASH_DISABLE_FLAG = "glimpse.disableEarlySplash";
    private static final String STARTUP_LAUNCH_WATCHDOG_FLAG = "glimpse.debugLaunchWatchdog";
    private static final String STARTUP_LAUNCH_THREADS_WATCHDOG_FLAG = "glimpse.debugLaunchThreadsWatchdog";
    private static final String STARTUP_LAUNCH_JAR_URL_DEBUG_FLAG = "glimpse.debugLaunchJarUrls";
    private static final String STARTUP_PREWARM_BEFORE_SHOW_FLAG = "glimpse.startupPrewarmBeforeShow";
    private static final String STARTUP_SHOW_WATCHDOG_FLAG = "glimpse.debugShowWatchdog";
    private static final String STARTUP_DEFER_MAIN_UI_UNTIL_READY_FLAG = "glimpse.startupDeferMainUiUntilReady";
    private static final String STARTUP_WATCHDOG_VERBOSE_STACK_FLAG = "glimpse.debugWatchdogVerboseStack";
    private static final int STARTUP_WATCHDOG_INTERVAL_MS = 2000;
    private static final long DATABASE_REBUILD_WATCH_INTERVAL_MS = 15000L;
    private static final String[] STARTUP_CRITICAL_ICON_PREWARM_KEYS = new String[] {
            "left_arrow", "double_left_arrow", "right_arrow", "up_right_arrow"
    };
    private static final String[] STARTUP_ICON_PREWARM_KEYS = new String[] {
            "left_arrow", "double_left_arrow", "right_arrow", "up_right_arrow",
            "play", "stop", "delete1", "edit1", "refresh1", "compare",
            "results", "results-selected", "log", "log-selected", "errors", "errors-selected",
            "open_folder1", "add", "queue",
            "create", "move_up", "move_down"
    };
    private static final double STARTUP_OVERLAY_MAX_WIDTH = 420.0;
    private static final int STARTUP_TOTAL_STEPS = 5;
    private static final int STARTUP_STEP_WINDOW_LAYOUT = 1;
    private static final int STARTUP_STEP_UI_READY = 2;
    private static final int STARTUP_STEP_FILES_READY = 3;
    private static final int STARTUP_STEP_COMPONENTS_READY = 4;
    private static final int STARTUP_STEP_SCENARIOS_READY = 5;
    private static final double TOP_LEFT_PANEL_RATIO = 4.0;
    private static final double TOP_RIGHT_PANEL_RATIO = 2.5;
    private static final double TOP_PANEL_GAP = 4.0;
    private static final double TOP_ROW_HEIGHT_RATIO = 45.0;
    private static final double BOTTOM_ROW_HEIGHT_RATIO = 55.0;
    // endregion

    // region Static Fields
    public static Stage primaryStage;
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
    private final AtomicBoolean startupCriticalIconPrewarmDone = new AtomicBoolean(false);
    private ProgressBar scenarioOperationProgressBar;
    private ProgressBar startupOverlayProgressBar;
    private final AtomicBoolean startupOverlayVisible = new AtomicBoolean(false);
    private Label startupOverlayLabel;
    private VBox startupOverlayBox;

    /** Startup timing anchor (nanoseconds). */
    private static final long STARTUP_T0_NANOS = System.nanoTime();
    /** Optional early timing toggle before options are loaded. */
    private static volatile boolean bootstrapTimingEnabled = true;
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
    /** Last startup message emitted to stdout so repeated progress updates do not spam logs. */
    private static volatile String lastStartupStatusLogged = "";
    /** True once the main ScenarioBuilder window has been shown. */
    private static volatile boolean mainWindowDisplayed = false;
    /** Keep startup shell visible until full main UI widgets are composed. */
    private static volatile boolean deferMainUiUntilReady = true;
    /** Prepared full scene shown once startup gate conditions are met. */
    private static volatile Scene preparedMainScene;
    private static volatile VBox preparedMainRoot;
    private static volatile GridPane preparedMainGridPane;
    /** Initial library loads that must finish before steady-state resource text is restored. */
    private static volatile boolean initialScenarioLoadPending = true;
    private static volatile boolean initialComponentLoadPending = true;
    private static volatile int startupStepsCompleted = 0;
    private static volatile String lastStartupStatusText = STARTUP_UI_MESSAGE;
    private static volatile boolean startupRelayoutApplied = false;
    private static volatile javax.swing.JWindow earlySplashWindow;
    private static volatile javax.swing.JLabel earlySplashLabel;
    private static volatile javax.swing.JProgressBar earlySplashProgressBar;
    private static final AtomicBoolean earlySplashVisible = new AtomicBoolean(false);
    private static volatile boolean initEntered = false;
    private static volatile boolean startEntered = false;
    private static final AtomicBoolean databaseTimestampWatchStarted = new AtomicBoolean(false);
    private static final AtomicBoolean startupResourcePrewarmStarted = new AtomicBoolean(false);
    private static volatile boolean databaseTimestampWatchStopRequested = false;
    private static volatile String watchedDatabasePath = "";
    private static volatile long watchedDatabaseLastModified = Long.MIN_VALUE;
    /** Log file path captured during init; heavy computer-stat collection is written asynchronously. */
    private static volatile String deferredStartupLogFilename;
    /** Last printed launch thread snapshot signature to avoid repetitive watchdog spam. */
    private static volatile String lastLaunchThreadsSnapshotSignature = "";
    /** Last printed classloader URL diagnostic signature to avoid repetitive watchdog spam. */
    private static volatile String lastLaunchClassLoaderUrlsSignature = "";

    /**
     * Launches the JavaFX application lifecycle for Scenario Builder.
     *
     * @param args Command line arguments passed to the application. Supports an options file via -options flag or as a single argument.
     */
    public static void main(String[] args) {
        logBootstrapCheckpoint("main(): entered");
        if (!Boolean.getBoolean(EARLY_SPLASH_DISABLE_FLAG)) {
            showEarlyStartupSplash(EARLY_SPLASH_INITIAL_MESSAGE);
        } else {
            logBootstrapCheckpoint("main(): early splash disabled by -D" + EARLY_SPLASH_DISABLE_FLAG + "=true");
        }
        logBootstrapCheckpoint("main(): after showEarlyStartupSplash");
        enableLaunchJarUrlDebugIfRequested();
        // Avoid touching JavaFX platform state before launch(); the toolkit is
        // configured in start() once the JavaFX runtime is fully initialized.

        final AtomicBoolean launchWatchdogDone = new AtomicBoolean(false);
        Thread launchWatchdog = null;
        Thread launchThreadsWatchdog = null;
        if (Boolean.getBoolean(STARTUP_LAUNCH_WATCHDOG_FLAG)) {
            launchWatchdog = startStartupWatchdog(
                    "glimpse-launch-watchdog",
                    "launch(args) waiting for init()",
                    Thread.currentThread(),
                    launchWatchdogDone,
                    () -> initEntered || startEntered);
        }
        if (Boolean.getBoolean(STARTUP_LAUNCH_THREADS_WATCHDOG_FLAG)) {
            launchThreadsWatchdog = startLaunchPhaseThreadDumpWatchdog(
                    launchWatchdogDone,
                    () -> initEntered || startEntered);
        }

        try {
            logBootstrapCheckpoint("main(): before launch(args)");
            launch(args);
        } finally {
            launchWatchdogDone.set(true);
            if (launchWatchdog != null) {
                launchWatchdog.interrupt();
            }
            if (launchThreadsWatchdog != null) {
                launchThreadsWatchdog.interrupt();
            }
            logBootstrapCheckpoint("main(): after launch(args)");
            closeEarlyStartupSplash();
        }
    }

    /**
     * Initializes singleton utilities, options, and startup logging before the primary stage is shown.
     *
     * @throws Exception if initialization fails
     */
    @Override
    public void init() throws Exception {
    	
    		// Install console redirection as early as possible so startup prints are captured.
        final long t0 = System.nanoTime();
        initEntered = true;
        logBootstrapCheckpoint("init(): entered");
        ConsoleOutputRedirect.install();
        logBootstrapCheckpoint("init(): after ConsoleOutputRedirect.install");

        updateEarlyStartupSplashMessage("Loading settings and options...");
        System.out.println("Loading settings and initializing.");

        // Initialize utility/variable objects with references to each other
        vars.init(utils, vars, styles, files);
        files.init(utils, vars, styles, files);
        utils.init(utils, vars, styles, files);

        // Parse command-line arguments for options file
        processArgs();

        // Load options into the vars singleton
        vars.loadOptions(optionsFilename);
        deferMainUiUntilReady = Boolean.parseBoolean(System.getProperty(STARTUP_DEFER_MAIN_UI_UNTIL_READY_FLAG, "true"));
        bootstrapTimingEnabled = vars.getDebugStartupTiming();
        logStartupCheckpoint("init(): options loaded", t0);
        updateEarlyStartupSplashMessage("Loading GLIMPSE options...");
        // Defer full setup analysis until after first paint; it performs many
        // filesystem checks and can significantly delay JavaFX launch.

        // Reset startup log immediately; defer expensive computer-stat collection to background.
        String glimpseLogDir = vars.getGlimpseLogDir();
        if (glimpseLogDir != null && glimpseLogDir.trim().length() > 0) {
            String glimpseLogFilename = glimpseLogDir + File.separator + "glimpse_log.txt";
            utils.resetLogFile(glimpseLogFilename);
            deferredStartupLogFilename = glimpseLogFilename;
        } else {
            System.out.println("Warning: glimpseLogDir not set; skipping log reset.");
        }

        // NOTE: Intentionally NOT calling files.loadFiles() here.
        // It can be heavy and would delay first window paint.

        utils.sb = this.sb;
        instanceForStatus = this;

        logStartupCheckpoint("Client.init complete", t0);

        // Wire event-driven database-size refresh: when the DB path changes,
        // request a fresh size calculation.
        vars.setOnGCamOutputDatabaseChanged(() -> Client.requestDatabaseSizeRefresh(true));
    }

    /**
     * Builds and displays the primary Scenario Builder window.
     * <p>
     * Shows a lightweight startup shell immediately, then composes the full UI and
     * starts deferred initialization tasks.
     *
     * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        final long t0 = System.nanoTime();
        startEntered = true;
        System.out.println("Starting GLIMPSE Graphical User Interface...");

        Client.primaryStage = primaryStage;
        logStartupCheckpoint("start(): primaryStage assigned", t0);

        // Ensures JavaFX does not exit implicitly on certain VMs
        // (for example when WM_ENDSESSION is called on Windows).
        Platform.setImplicitExit(false);
        logStartupCheckpoint("start(): after Platform.setImplicitExit(false)", t0);

        // Ensure threads are properly terminated on window close
        primaryStage.setOnCloseRequest(event -> {
            // Don't let exceptions prevent shutdown.
            safeShutdownExecutionThreads();
            Platform.exit();
        });
        logStartupCheckpoint("start(): close handler installed", t0);

        //        //testing to see if I can have this appear early
//        primaryStage.setTitle(VERSION);
//        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
//        primaryStage.setHeight(MIN_WINDOW_HEIGHT);
//        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
//        primaryStage.setWidth(MIN_WINDOW_WIDTH);
//        primaryStage.centerOnScreen();
//        primaryStage.show();
                
        logStartupCheckpoint("start(): before startup shell setup", t0);
        advanceStartupStep(STARTUP_STEP_WINDOW_LAYOUT, STARTUP_SHELL_MESSAGE);
        logStartupCheckpoint("start(): after advanceStartupStep", t0);
        setStartupStatus(STARTUP_SHELL_MESSAGE, -1, true);
        logStartupCheckpoint("start(): after setStartupStatus(shell)", t0);
        setStartupShellWindow();
        logStartupCheckpoint("start(): after setStartupShellWindow", t0);

        final boolean prewarmBeforeShow = Boolean.getBoolean(STARTUP_PREWARM_BEFORE_SHOW_FLAG);
        if (prewarmBeforeShow) {
            startStartupResourcePrewarm();
            logStartupCheckpoint("start(): startup resource prewarm queued (pre-show)", t0);
        }

        final AtomicBoolean showWatchdogDone = new AtomicBoolean(false);
        Thread showWatchdog = null;
        if (Boolean.getBoolean(STARTUP_SHOW_WATCHDOG_FLAG)) {
            showWatchdog = startStartupWatchdog(
                    "glimpse-show-watchdog",
                    "primaryStage.show()",
                    Thread.currentThread(),
                    showWatchdogDone,
                    null);
        }
        logStartupCheckpoint("start(): before primaryStage.show", t0);
        primaryStage.show();
        showWatchdogDone.set(true);
        if (showWatchdog != null) {
            showWatchdog.interrupt();
        }
        logStartupCheckpoint("start(): after primaryStage.show", t0);
        closeEarlyStartupSplash();
        logStartupCheckpoint("start(): after closeEarlyStartupSplash", t0);
        // Computer stats log-write and setup analysis are both deferred to after
        // initial startup loads finish so they don't compete with the critical startup path.
        startDeferredSetupAnalysisLogging();
        logStartupCheckpoint("Startup shell shown", t0);
        if (!prewarmBeforeShow) {
            startStartupResourcePrewarm();
            logStartupCheckpoint("start(): startup resource prewarm queued (post-show)", t0);
        }

        runAfterInitialFxPulse(() -> {
            // Build heavy panes after first paint so startup is perceived as immediate.
            setStartupStatus(STARTUP_BUILDING_UI_MESSAGE, -1, true);
            warmUpFxControlsForStartup();
            waitForCriticalIconPrewarmThenBuild(0);
        });

        logStartupCheckpoint("Client.start complete", t0);
    }

    /**
     * Pre-initializes JavaFX rendering pipeline by creating a minimal off-screen stage
     * and forcing a rendering pulse. This causes glass/prism native libraries to load
     * before the main stage.show() call, avoiding an 18+ second block on native library
     * initialization during the critical first render.
     */
    private void startStartupResourcePrewarm() {
        if (!startupResourcePrewarmStarted.compareAndSet(false, true)) {
            return;
        }
        Thread prewarmThread = new Thread(() -> {
            Client.logStartupBuildCheckpoint("startup prewarm: begin");
            try {
                utils.prewarmButtonIcons(STARTUP_CRITICAL_ICON_PREWARM_KEYS);
            } catch (Throwable ignored) {
                // Non-critical optimization; button creation falls back to normal icon loading.
            } finally {
                startupCriticalIconPrewarmDone.set(true);
                Client.logStartupBuildCheckpoint("startup prewarm: critical icons complete");
            }
            try {
                utils.prewarmButtonIcons(STARTUP_ICON_PREWARM_KEYS);
            } catch (Throwable ignored) {
                // Non-critical optimization; button creation falls back to normal icon loading.
            }

            // Warm CSS URL lookup without forcing full stream copy/decompression work.
            try {
                CSSResourceManager.getModernCssUrl();
            } catch (Exception ignored) {
                // Non-critical optimization.
            }
            Client.logStartupBuildCheckpoint("startup prewarm: complete");
        }, "glimpse-startup-resource-prewarm");
        prewarmThread.setDaemon(true);
        prewarmThread.start();
    }


    private void waitForCriticalIconPrewarmThenBuild(int attempt) {
        if (startupCriticalIconPrewarmDone.get() || attempt >= 120) {
            if (!startupCriticalIconPrewarmDone.get()) {
                logStartupCheckpoint("startup prewarm: critical icon wait timed out", STARTUP_T0_NANOS);
            }
            buildScenarioBuilderAndComposeMainWindow();
            return;
        }

        PauseTransition pause = new PauseTransition(Duration.millis(25));
        pause.setOnFinished(e -> waitForCriticalIconPrewarmThenBuild(attempt + 1));
        pause.play();
    }

    private void buildScenarioBuilderAndComposeMainWindow() {
        logStartupCheckpoint("ScenarioBuilder.build start", STARTUP_T0_NANOS);
        getScenarioBuilder().build();
        logStartupCheckpoint("ScenarioBuilder.build complete", STARTUP_T0_NANOS);
        advanceStartupStep(STARTUP_STEP_UI_READY, STARTUP_WINDOW_READY_MESSAGE);
        setFileDependentUiEnabled(false);
        setStartupStatus(STARTUP_WINDOW_READY_MESSAGE, -1, true);
        logStartupCheckpoint("Main window composition start", STARTUP_T0_NANOS);
        setMainWindow(combineAllElementsIntoOnePane(), createMenuBar(), !deferMainUiUntilReady);
        if (!deferMainUiUntilReady) {
            forceStartupRelayoutAfterSceneSwap();
        } else {
            logStartupCheckpoint("Main window reveal deferred until startup UI is composed", STARTUP_T0_NANOS);
            tryRevealPreparedMainWindow(false);
        }
        logStartupCheckpoint("Main window composition complete", STARTUP_T0_NANOS);
        utils.setModalDialogsReadyAndFlushWarnings();

        // Wire DB-size-done callback so the status bar re-renders with the real value
        // instead of "calculating..." once the async directory scan finishes.
        utils.setOnDatabaseSizeRefreshed(Client::refreshStatusBarComputerStats);
        logStartupCheckpoint("DB size refresh callback wired", STARTUP_T0_NANOS);

        // Kick off the first DB-size scan now that the GUI is visible.
        requestDatabaseSizeRefresh(true);
        startDatabaseTimestampWatcher();

        setStartupStatus(STARTUP_POST_SHOW_MESSAGE, -1, true);
        logStartupCheckpoint("Post-show startup tasks begin", STARTUP_T0_NANOS);
        setupExecutionThreads();

        final String iconFile = "file:" + vars.getGlimpseResourceDir() + File.separator + "GLIMPSE_icon_large.png";
        primaryStage.getIcons().add(new Image(iconFile));

        logStartupCheckpoint("Primary stage shown (first FX pulse after show)", STARTUP_T0_NANOS);
        WindowsRuntimePreflight.ensureMsvcRuntimeAvailableOrWarn(utils, "Startup");
        setStartupStatus(STARTUP_FILES_MESSAGE, -1, true);
        // Start table/data population only after the main panes have had a chance to render once.
        Platform.runLater(this::startDeferredFileLoading);
    }

    private void warmUpFxControlsForStartup() {
        logStartupCheckpoint("startup fx warmup: begin", STARTUP_T0_NANOS);
        try {
            VBox warmupRoot = new VBox();
            warmupRoot.setManaged(false);
            warmupRoot.setVisible(false);

            logStartupCheckpoint("startup fx warmup: createButton start", STARTUP_T0_NANOS);
            Button warmupButton = utils.createButton("Warmup", styles.getBigButtonWidth(), null);
            logStartupCheckpoint("startup fx warmup: createButton complete", STARTUP_T0_NANOS);
            warmupRoot.getChildren().add(warmupButton);

            Scene warmupScene = new Scene(warmupRoot, 1, 1);
            applyModernCss(warmupScene);

            logStartupCheckpoint("startup fx warmup: applyCss start", STARTUP_T0_NANOS);
            warmupRoot.applyCss();
            logStartupCheckpoint("startup fx warmup: applyCss complete", STARTUP_T0_NANOS);

            logStartupCheckpoint("startup fx warmup: layout start", STARTUP_T0_NANOS);
            warmupRoot.layout();
            logStartupCheckpoint("startup fx warmup: layout complete", STARTUP_T0_NANOS);
        } catch (Throwable ignored) {
            // Non-critical optimization; continue with normal startup path.
        }
        logStartupCheckpoint("startup fx warmup: complete", STARTUP_T0_NANOS);
    }

    /**
     * Parses launch arguments and resolves the options filename when provided.
     * <p>
     * Supports both single-argument usage and {@code -options <file>} form.
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
     * Builds the main menu bar and delegates menu population to setup helpers.
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
     * Composes the primary two-row layout containing top editing panes and bottom run pane.
     *
     * @return GridPane containing all main UI elements
     */
    private GridPane combineAllElementsIntoOnePane() {
        final GridPane mainGridPane = new GridPane();
        mainGridPane.setHgap(0);
        mainGridPane.setMinSize(0, 0);
        mainGridPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        javafx.scene.layout.RowConstraints topRow = new javafx.scene.layout.RowConstraints();
        topRow.setVgrow(Priority.ALWAYS);
        topRow.setPercentHeight(TOP_ROW_HEIGHT_RATIO / (TOP_ROW_HEIGHT_RATIO + BOTTOM_ROW_HEIGHT_RATIO) * 100.0);

        javafx.scene.layout.RowConstraints bottomRow = new javafx.scene.layout.RowConstraints();
        bottomRow.setVgrow(Priority.ALWAYS);
        bottomRow.setPercentHeight(BOTTOM_ROW_HEIGHT_RATIO / (TOP_ROW_HEIGHT_RATIO + BOTTOM_ROW_HEIGHT_RATIO) * 100.0);

        mainGridPane.getRowConstraints().setAll(topRow, bottomRow);

        VBox componentLibraryBox = getScenarioBuilder().getvBoxComponentLibrary();
        VBox arrowBox = getScenarioBuilder().getvBoxButton();
        VBox createScenarioBox = getScenarioBuilder().getvBoxCreateScenario();
        VBox runBox = getScenarioBuilder().getvBoxRun();

        // Keep panes hidden until all controls are assembled; reveal together after the frame is shown.
        deferMainPaneDisplayUntilReady(componentLibraryBox);
        deferMainPaneDisplayUntilReady(arrowBox);
        deferMainPaneDisplayUntilReady(createScenarioBox);
        deferMainPaneDisplayUntilReady(runBox);

        // Set max sizes to allow proper resizing behavior
        componentLibraryBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        createScenarioBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        runBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        componentLibraryBox.setMinWidth(0);
        createScenarioBox.setMinWidth(0);
        
        final HBox topRowBox = new HBox(TOP_PANEL_GAP, componentLibraryBox, arrowBox, createScenarioBox);
        topRowBox.setFillHeight(true);
        topRowBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        arrowBox.setMinWidth(Region.USE_PREF_SIZE);
        arrowBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        arrowBox.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(componentLibraryBox, Priority.ALWAYS);
        HBox.setHgrow(createScenarioBox, Priority.ALWAYS);

        final double ratioDenominator = TOP_LEFT_PANEL_RATIO + TOP_RIGHT_PANEL_RATIO;
        componentLibraryBox.prefWidthProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> {
                            double topWidth = topRowBox.getWidth();
                            double arrowWidth = Math.max(arrowBox.getWidth(), arrowBox.prefWidth(-1));
                            double availableWidth = Math.max(0.0, topWidth - arrowWidth - TOP_PANEL_GAP);
                            return availableWidth * (TOP_LEFT_PANEL_RATIO / ratioDenominator);
                        },
                        topRowBox.widthProperty(),
                        arrowBox.widthProperty(),
                        arrowBox.prefWidthProperty()));
        createScenarioBox.prefWidthProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> {
                            double topWidth = topRowBox.getWidth();
                            double arrowWidth = Math.max(arrowBox.getWidth(), arrowBox.prefWidth(-1));
                            double availableWidth = Math.max(0.0, topWidth - arrowWidth - TOP_PANEL_GAP);
                            return availableWidth * (TOP_RIGHT_PANEL_RATIO / ratioDenominator);
                        },
                        topRowBox.widthProperty(),
                        arrowBox.widthProperty(),
                        arrowBox.prefWidthProperty()));

        final HBox bottomRowBox = new HBox(10, runBox);
        bottomRowBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(runBox, Priority.ALWAYS);
        bottomRowBox.setStyle(styles.getStyle1());

        GridPane.setHgrow(topRowBox, Priority.ALWAYS);
        GridPane.setVgrow(topRowBox, Priority.ALWAYS);
        GridPane.setHgrow(bottomRowBox, Priority.ALWAYS);
        GridPane.setVgrow(bottomRowBox, Priority.ALWAYS);
        mainGridPane.add(topRowBox, 0, 0);
        mainGridPane.add(bottomRowBox, 0, 1);

        return mainGridPane;
    }

    private static void deferMainPaneDisplayUntilReady(VBox pane) {
        if (pane == null) {
            return;
        }
        pane.setVisible(false);
        pane.setManaged(false);
    }

    private static void revealMainPane(VBox pane) {
        if (pane == null) {
            return;
        }
        pane.setManaged(true);
        pane.setVisible(true);
    }

    private void revealMainPanesIfReady() {
        VBox componentLibraryBox = getScenarioBuilder().getvBoxComponentLibrary();
        VBox arrowBox = getScenarioBuilder().getvBoxButton();
        VBox createScenarioBox = getScenarioBuilder().getvBoxCreateScenario();
        VBox runBox = getScenarioBuilder().getvBoxRun();

        if (componentLibraryBox == null || arrowBox == null || createScenarioBox == null || runBox == null) {
            return;
        }
        if (componentLibraryBox.getChildren().isEmpty()
                || arrowBox.getChildren().isEmpty()
                || createScenarioBox.getChildren().isEmpty()
                || runBox.getChildren().isEmpty()) {
            return;
        }

        revealMainPane(componentLibraryBox);
        revealMainPane(arrowBox);
        revealMainPane(createScenarioBox);
        revealMainPane(runBox);
    }

    /**
     * Sets up the main application window with the provided layout and menu bar.
     *
     * Configures the root layout, scene, window size, and optionally displays the splash screen.
     *
     * @param mainGridPane The main content pane
     * @param menuBar The menu bar
     */
    private void setMainWindow(GridPane mainGridPane, MenuBar menuBar, boolean showImmediately) {
        // Compose the root layout
        sb.setStyle(buildStatusBarStyleForText("", null));
        configureStatusBarRightItems();
        final StackPane centerStack = new StackPane();
        centerStack.getChildren().add(mainGridPane);
        startupOverlayBox = createStartupOverlay();
        centerStack.getChildren().add(startupOverlayBox);
        StackPane.setAlignment(startupOverlayBox, Pos.CENTER);
        VBox.setVgrow(centerStack, Priority.ALWAYS);
        final VBox root = new VBox(menuBar, centerStack, sb);
        root.setFillWidth(true);
        final Scene scene = new Scene(root, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);

        applyModernCss(scene);

        if (showImmediately) {
            applyMainScene(scene, root, mainGridPane);
        } else {
            preparedMainScene = scene;
            preparedMainRoot = root;
            preparedMainGridPane = mainGridPane;
        }
    }

    private void applyMainScene(Scene scene, VBox root, GridPane mainGridPane) {
        if (scene == null || root == null) {
            return;
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle(VERSION);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setWidth(MIN_WINDOW_WIDTH);
        primaryStage.centerOnScreen();

        applyStartupStatus(sb.getText(), calculateStartupProgress(), startupBusyState);
        Platform.runLater(() -> {
            try {
                revealMainPanesIfReady();
                root.applyCss();
                root.layout();
                mainGridPane.requestLayout();
            } catch (Exception ignored) {
            }
        });
        mainWindowDisplayed = true;

        if (vars.getShowSplash()) {
            loadSplashScreen();
        }
    }

    private static boolean isStartupDataReadyForMainUi() {
        return startupStepsCompleted >= STARTUP_STEP_UI_READY;
    }

    private static void tryRevealPreparedMainWindow(boolean forceReveal) {
        if (!deferMainUiUntilReady || mainWindowDisplayed || preparedMainScene == null) {
            return;
        }
        if (!forceReveal && !isStartupDataReadyForMainUi()) {
            return;
        }
        final Client inst = instanceForStatus;
        if (inst == null) {
            return;
        }
        Runnable reveal = inst::revealPreparedMainWindowNow;
        if (Platform.isFxApplicationThread()) {
            reveal.run();
        } else {
            Platform.runLater(reveal);
        }
    }

    private void revealPreparedMainWindowNow() {
        if (mainWindowDisplayed || preparedMainScene == null || primaryStage == null) {
            return;
        }
        Scene scene = preparedMainScene;
        VBox root = preparedMainRoot;
        GridPane mainGridPane = preparedMainGridPane;
        preparedMainScene = null;
        preparedMainRoot = null;
        preparedMainGridPane = null;

        applyMainScene(scene, root, mainGridPane);
        forceStartupRelayoutAfterSceneSwap();
        logStartupCheckpoint("Main window revealed after startup readiness gate", STARTUP_T0_NANOS);
    }

    /**
     * After swapping from the lightweight startup shell to the full UI scene,
     * force one extra CSS/layout pass and a tiny one-time stage-size nudge so
     * width/height bindings settle immediately without user resize.
     */
    private void forceStartupRelayoutAfterSceneSwap() {
        if (startupRelayoutApplied) {
            return;
        }
        startupRelayoutApplied = true;
        Platform.runLater(() -> {
            try {
                final Stage stage = primaryStage;
                final Scene scene = stage == null ? null : stage.getScene();
                final Parent root = scene == null ? null : scene.getRoot();
                if (stage == null || root == null) {
                    return;
                }

                final double targetW = Math.max(MIN_WINDOW_WIDTH, stage.getWidth());
                final double targetH = Math.max(MIN_WINDOW_HEIGHT, stage.getHeight());

                // First pass: force CSS/layout and request another pulse.
                root.applyCss();
                root.layout();
                root.requestLayout();

                Platform.runLater(() -> {
                    try {
                        // Second pass: re-apply layout after controls have reported final pref sizes.
                        root.applyCss();
                        root.layout();

                        // Let JavaFX compute scene-driven preferred sizing once, then restore
                        // the target startup dimensions to keep the expected window footprint.
                        stage.sizeToScene();
                        stage.setWidth(targetW);
                        stage.setHeight(targetH);

                        // Final one-pixel nudge to guarantee bound regions recompute now.
                        stage.setWidth(targetW + 1);
                        stage.setHeight(targetH + 1);
                        stage.setWidth(targetW);
                        stage.setHeight(targetH);
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Shows a lightweight shell scene so users get immediate visual feedback
     * while the full ScenarioBuilder panes are still being constructed.
     */
    private void setStartupShellWindow() {
        Label heading = new Label(STARTUP_DIALOG_HEADING);
        heading.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #465060;");
        Label message = new Label(STARTUP_SHELL_MESSAGE);
        message.setStyle("-fx-font-size: 13px; -fx-text-fill: #465060;");
        ProgressBar bar = new ProgressBar(ProgressIndicator.INDETERMINATE_PROGRESS);
        bar.setPrefWidth(260);

        VBox center = new VBox(12, heading, message, bar);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(24));
        BorderPane shellRoot = new BorderPane(center);
        // Keep startup shell styling minimal to reduce first-show CSS work.
        shellRoot.setStyle("-fx-background-color: #f7f9fc;");

        Scene scene = new Scene(shellRoot, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle(VERSION);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setWidth(MIN_WINDOW_WIDTH);
        primaryStage.centerOnScreen();
    }

    /**
     * Sets up the execution threads for GCAM and the model interface.
     * GCAM uses a single-threaded executor, while the model interface uses a multi-threaded executor.
     *
     * Initializes and starts the execution queues for both GCAM and post-processor.
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
                //System.out.println("Loading GLIMPSE files (deferred)...");
                setStartupStatus(STARTUP_FILES_MESSAGE, -1, true);
                files.loadFiles();
                filesLoaded = true;
                advanceStartupStep(STARTUP_STEP_FILES_READY, STARTUP_COMPONENT_MESSAGE);
                logStartupCheckpoint("files.loadFiles complete (deferred)", t0);

                Platform.runLater(() -> {
                    try {
                        setStartupStatus(STARTUP_COMPONENT_MESSAGE, -1, true);
                        if (Client.getPaneComponentLibrary() != null) {
                            Client.getPaneComponentLibrary().refreshComponentLibraryTableForStartup();
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
                    tryRevealPreparedMainWindow(false);
                });

            } catch (Throwable ex) {
                System.err.println("Deferred file loading failed: " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    setFileDependentUiEnabled(true);
                    setStartupStatus("Error loading required files (see console)", -1, false);
                    tryRevealPreparedMainWindow(true);
                });
            }
        }, "glimpse-deferred-file-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Writes the startup computer-status snapshot after first paint so it does not block app launch.
     */
    private void startDeferredComputerStatsLogging() {
        final String logFilename = deferredStartupLogFilename;
        if (logFilename == null || logFilename.trim().isEmpty()) {
            return;
        }
        final Thread t = new Thread(() -> {
            final long t0 = System.nanoTime();
            try {
                final String stats = utils.getComputerStatString();
                if (stats != null && !stats.trim().isEmpty()) {
                    files.appendTextToFile(stats + vars.getEol(), logFilename);
                }
                logStartupCheckpoint("Deferred computer stats logged", t0);
            } catch (Throwable ex) {
                System.out.println("Deferred computer stats logging failed: " + ex.getMessage());
            }
        }, "glimpse-startup-computer-stats");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Runs the verbose installation/setup diagnostics after first paint so init()
     * remains focused on critical startup work.
     * <p>
     * When {@code debugStartupTiming} is enabled, individual setup check timings are
     * emitted as {@code [startup-setup]} lines so the slowest sub-checks are visible
     * in the startup log.
     */
    private void startDeferredSetupAnalysisLogging() {
        final Thread t = new Thread(() -> {
            final long t0 = System.nanoTime();
            logStartupCheckpoint("Deferred setup analysis: begin", t0);
            try {
                final String setup = vars.examineGLIMPSESetup();
                if (setup != null && !setup.trim().isEmpty()) {
                    System.out.println(setup);
                }
                logStartupCheckpoint("Deferred setup analysis: complete", t0);
            } catch (Throwable ex) {
                System.out.println("Deferred setup analysis failed: " + ex.getMessage());
            }
        }, "glimpse-startup-setup-analysis");
        t.setDaemon(true);
        t.start();
    }

    /** Updates the status bar text (safe from any thread). */
    public static void setStartupStatus(String text, double progress, boolean busy) {
        final Client inst = instanceForStatus;
        startupBusyState = busy;
        final String safeText = (text == null || text.trim().isEmpty()) ? STARTUP_READY_MESSAGE : text.trim();
        updateEarlyStartupSplashMessage(safeText);
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

    /**
     * Convenience overload for reporting required-file startup progress when file counts are unknown.
     *
     * @param filename file path (or name) currently being loaded
     */
    public static void setStartupRequiredFileStatus(String filename) {
        setStartupRequiredFileStatus(filename, -1, -1);
    }

    /**
     * Reports required-file loading progress in startup status UI.
     *
     * @param filename file path (or name) currently being loaded
     * @param currentFileIndex one-based index of file currently loading; ignored when {@code <= 0}
     * @param totalFiles total number of files in this phase; ignored when {@code <= 0}
     */
    public static void setStartupRequiredFileStatus(String filename, int currentFileIndex, int totalFiles) {
        String safeFilename = (filename == null) ? "" : filename.trim();
        if (safeFilename.isEmpty()) {
            setStartupStatus(STARTUP_FILES_MESSAGE, -1, true);
            return;
        }
        String displayName = new File(safeFilename).getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = safeFilename;
        }
        String progressSuffix = "";
        if (currentFileIndex > 0 && totalFiles > 0) {
            progressSuffix = " (" + currentFileIndex + "/" + totalFiles + ")";
        }
        setStartupStatus("Loading required files... " + displayName + progressSuffix, -1, true);
    }

    private void applyStartupStatus(String text, double progress, boolean busy) {
        String safeText = (text == null || text.trim().isEmpty()) ? STARTUP_READY_MESSAGE : text.trim();
        lastStartupStatusText = safeText;
        logStartupStatusToStdout(safeText, busy);
        sb.setText(safeText);
        sb.setStyle(buildStatusBarStyleForText(safeText, null));
        if (startupOverlayLabel != null) {
            startupOverlayLabel.setText(safeText);
        }
        updateStartupStepForStatus(safeText);
        if (startupOverlayProgressBar != null) {
            startupOverlayProgressBar.setProgress(busy ? ProgressIndicator.INDETERMINATE_PROGRESS : 1.0);
        }
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

    /**
     * Marks initial scenario status loading as complete and advances startup progress state.
     * <p>
     * This method is safe to call from non-JavaFX threads.
     */
    public static void markInitialScenarioLoadComplete() {
        initialScenarioLoadPending = false;
        advanceStartupStep(STARTUP_STEP_SCENARIOS_READY, STARTUP_READY_MESSAGE);
        applyDeferredStatusBarTextIfReady();
        tryRevealPreparedMainWindow(false);
        // Now that all initial loads are done, write the computer stats snapshot to
        // the log file without competing with the startup tasks above.
        final Client inst = instanceForStatus;
        if (inst != null) {
            inst.startDeferredComputerStatsLogging();
        }
    }

    /**
     * Marks initial component library loading as complete and advances startup progress state.
     * <p>
     * This method is safe to call from non-JavaFX threads.
     */
    public static void markInitialComponentLoadComplete() {
        initialComponentLoadPending = false;
        advanceStartupStep(STARTUP_STEP_COMPONENTS_READY, STARTUP_SCENARIO_MESSAGE);
        applyDeferredStatusBarTextIfReady();
        tryRevealPreparedMainWindow(false);
    }

    private VBox createStartupOverlay() {
        Label headingLabel = new Label(STARTUP_DIALOG_HEADING);
        headingLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #465060;");
        headingLabel.setWrapText(true);
        headingLabel.setAlignment(Pos.CENTER);
        headingLabel.setMaxWidth(Double.MAX_VALUE);

        startupOverlayLabel = new Label("Starting...");
        startupOverlayLabel.setWrapText(true);
        startupOverlayLabel.setAlignment(Pos.CENTER);
        startupOverlayLabel.setMaxWidth(Double.MAX_VALUE);
        startupOverlayLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #465060;");

        startupOverlayProgressBar = new ProgressBar(ProgressIndicator.INDETERMINATE_PROGRESS);
        startupOverlayProgressBar.setPrefWidth(240);
        startupOverlayProgressBar.setMaxWidth(240);
        startupOverlayProgressBar.setMinWidth(240);
        startupOverlayProgressBar.setPrefHeight(18);
        startupOverlayProgressBar.setFocusTraversable(false);
        startupOverlayProgressBar.setStyle("-fx-accent: #748ac4;");

        HBox progressRow = new HBox(12, startupOverlayProgressBar);
        progressRow.setAlignment(Pos.CENTER);

        VBox overlay = new VBox(12, headingLabel, startupOverlayLabel, progressRow);
        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(true);
        overlay.setManaged(false);
        overlay.setVisible(false);
        overlay.setMaxWidth(STARTUP_OVERLAY_MAX_WIDTH);
        overlay.setPadding(new Insets(22, 28, 22, 28));
        overlay.setStyle("-fx-background-color: rgba(255,255,255,0.97); -fx-background-radius: 12; -fx-border-color: rgba(220,224,234,0.95); -fx-border-radius: 12;");
        applyStartupVisualState();
        return overlay;
    }

    private static void advanceStartupStep(int stepNumber, String statusText) {
        if (stepNumber > startupStepsCompleted) {
            startupStepsCompleted = Math.min(stepNumber, STARTUP_TOTAL_STEPS);
        }
        if (statusText != null && !statusText.trim().isEmpty()) {
            lastStartupStatusText = statusText.trim();
        }
        updateEarlyStartupSplashMessage(lastStartupStatusText);
        final Client inst = instanceForStatus;
        if (inst == null) {
            return;
        }
        Runnable update = inst::applyStartupVisualState;
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void applyStartupVisualState() {
        if (startupOverlayLabel != null) {
            startupOverlayLabel.setText(lastStartupStatusText == null || lastStartupStatusText.trim().isEmpty() ? STARTUP_UI_MESSAGE : lastStartupStatusText.trim());
        }
        if (startupOverlayProgressBar != null) {
            startupOverlayProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }
    }

    private double normalizeStartupProgress(String safeText, double progress, boolean busy) {
        if (!busy) {
            if (STARTUP_READY_MESSAGE.equalsIgnoreCase(safeText) || SCENARIO_REFRESHED_MESSAGE.equalsIgnoreCase(safeText)) {
                startupStepsCompleted = STARTUP_TOTAL_STEPS;
                return 1.0;
            }
            return calculateStartupProgress();
        }
        if (progress >= 0.0 && progress <= 1.0) {
            return Math.max(progress, calculateStartupProgress());
        }
        return calculateStartupProgress();
    }

    private void updateStartupStepForStatus(String safeText) {
        if (safeText == null) {
            return;
        }
        if (STARTUP_BUILDING_UI_MESSAGE.equalsIgnoreCase(safeText)
                || STARTUP_WINDOW_READY_MESSAGE.equalsIgnoreCase(safeText)
                || STARTUP_POST_SHOW_MESSAGE.equalsIgnoreCase(safeText)
                || safeText.startsWith(STARTUP_FILES_MESSAGE)) {
            startupStepsCompleted = Math.max(startupStepsCompleted, STARTUP_STEP_UI_READY);
        } else if (STARTUP_COMPONENT_MESSAGE.equalsIgnoreCase(safeText)) {
            startupStepsCompleted = Math.max(startupStepsCompleted, STARTUP_STEP_FILES_READY);
        } else if (STARTUP_SCENARIO_MESSAGE.equalsIgnoreCase(safeText)) {
            startupStepsCompleted = Math.max(startupStepsCompleted, STARTUP_STEP_COMPONENTS_READY);
        } else if (STARTUP_READY_MESSAGE.equalsIgnoreCase(safeText) || SCENARIO_REFRESHED_MESSAGE.equalsIgnoreCase(safeText)) {
            startupStepsCompleted = STARTUP_TOTAL_STEPS;
        }
    }

    private double calculateStartupProgress() {
        return Math.max(0.0, Math.min(1.0, startupStepsCompleted / (double) STARTUP_TOTAL_STEPS));
    }

    private String formatStartupPercent(double progress) {
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * 100.0);
        return percent + "%";
    }

  /** Returns the singleton {@link ScenarioBuilder} used by this application instance. */
	public ScenarioBuilder getScenarioBuilder() {
		return scenarioBuilder;
	}

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

    private String buildStatusBarStyleForText(String text, String extraStyle) {
        String normalizedText = text == null ? "" : text.trim();
        String normalizedExtraStyle = extraStyle == null ? "" : extraStyle.trim();
        String textColorStyle = normalizedText.endsWith("!!!") ? STATUS_BAR_ALERT_TEXT_STYLE : STATUS_BAR_DEFAULT_TEXT_STYLE;
        if (normalizedExtraStyle.isEmpty()) {
            return buildStatusBarStyle(textColorStyle);
        }
        return buildStatusBarStyle(textColorStyle + " " + normalizedExtraStyle);
    }

    private boolean isRecurringResourceStatus(String text) {
        if (text == null) {
            return false;
        }
        return text.trim().startsWith(RESOURCE_STATUS_PREFIX);
    }

    private void logStartupStatusToStdout(String text, boolean busy) {
        if (!busy && mainWindowDisplayed && shouldApplyDeferredStatusAfter(text)) {
            return;
        }
        String safeText = (text == null || text.trim().isEmpty()) ? STARTUP_READY_MESSAGE : text.trim();
        String normalized = (busy ? "BUSY|" : "IDLE|") + safeText;
        if (normalized.equals(lastStartupStatusLogged)) {
            return;
        }
        if (isRecurringResourceStatus(safeText)
                && lastStartupStatusLogged != null
                && (lastStartupStatusLogged.startsWith("BUSY|" + RESOURCE_STATUS_PREFIX)
                        || lastStartupStatusLogged.startsWith("IDLE|" + RESOURCE_STATUS_PREFIX))) {
            lastStartupStatusLogged = normalized;
            return;
        }
        lastStartupStatusLogged = normalized;
        System.out.println("[startup-status] " + safeText);
    }

    private boolean shouldApplyDeferredStatusAfter(String text) {
        if (text == null) {
            return false;
        }
        String safeText = text.trim();
        return STARTUP_READY_MESSAGE.equalsIgnoreCase(safeText)
                || SCENARIO_REFRESHED_MESSAGE.equalsIgnoreCase(safeText);
    }

    /**
     * Queues a post-startup status-bar message to apply once startup and initial library loads finish.
     *
     * @param text status message text
     * @param style optional extra CSS fragment for the status bar; may be {@code null}
     */
    public static void setDeferredStatusBarText(String text, String style) {
        final String safeText = (text == null) ? "" : text.trim();
        if (safeText.isEmpty()) {
            return;
        }
        deferredStatusBarText = safeText + "\n" + ((style == null || style.trim().isEmpty()) ? "" : style.trim());
        applyDeferredStatusBarTextIfReady();
    }

    private static boolean areInitialLibraryLoadsPending() {
        return initialScenarioLoadPending || initialComponentLoadPending;
    }

    /** Returns whether startup flow is still considered busy by the status subsystem. */
    public static boolean isStartupBusy() {
        return startupBusyState;
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
            String pendingText = parts[0];
            String pendingStyle = parts.length > 1 ? parts[1] : null;
            inst.sb.setText(pendingText);
            inst.sb.setStyle(inst.buildStatusBarStyleForText(pendingText, pendingStyle));
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void applyModernCss(Scene scene) {
        CSSResourceManager.applyModernTheme(scene);
    }

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

    private static void safeShutdownExecutionThreads() {
        databaseTimestampWatchStopRequested = true;
        try {
            if (Client.gCAMExecutionThread != null) {
                try {
                    if (Client.gCAMExecutionThread.getStatusChecker() != null) {
                        Client.gCAMExecutionThread.getStatusChecker().terminate();
                    }
                } catch (Throwable ignored) {
                }
                try {
                    Client.gCAMExecutionThread.shutdownNow();
                } catch (Throwable ignored) {
                }
            }
        } finally {
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

    /**
     * Watches only the database file/folder timestamp and requests a DB-size refresh
     * when that timestamp changes (for example after external rebuild/delete workflows).
     */
    private static void startDatabaseTimestampWatcher() {
        if (!databaseTimestampWatchStarted.compareAndSet(false, true)) {
            return;
        }
        databaseTimestampWatchStopRequested = false;
        Thread watcher = new Thread(() -> {
            while (!databaseTimestampWatchStopRequested) {
                try {
                    Thread.sleep(DATABASE_REBUILD_WATCH_INTERVAL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!mainWindowDisplayed) {
                    continue;
                }
                Client inst = instanceForStatus;
                if (inst == null) {
                    continue;
                }
                String configuredDatabase = inst.vars.getgCamOutputDatabase();
                if (configuredDatabase == null || configuredDatabase.trim().isEmpty()) {
                    watchedDatabasePath = "";
                    watchedDatabaseLastModified = Long.MIN_VALUE;
                    continue;
                }

                File databaseTarget = new File(configuredDatabase.trim());
                String absolutePath = databaseTarget.getAbsolutePath();
                long currentTimestamp = readDatabaseTimestamp(databaseTarget);

                if (!absolutePath.equals(watchedDatabasePath)) {
                    watchedDatabasePath = absolutePath;
                    watchedDatabaseLastModified = currentTimestamp;
                    continue;
                }

                long previousTimestamp = watchedDatabaseLastModified;
                if (previousTimestamp != Long.MIN_VALUE && currentTimestamp != previousTimestamp) {
                    watchedDatabaseLastModified = currentTimestamp;
                    requestDatabaseSizeRefresh(true);
                    continue;
                }
                watchedDatabaseLastModified = currentTimestamp;
            }
        }, "glimpse-db-timestamp-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private static long readDatabaseTimestamp(File databaseTarget) {
        if (databaseTarget == null || !databaseTarget.exists()) {
            return -1L;
        }
        try {
            return databaseTarget.lastModified();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static void logStartupCheckpoint(String label, long t0Nanos) {
        try {
            if (!GLIMPSEVariables.getInstance().getDebugStartupTiming()) {
                return;
            }
        } catch (Throwable ignored) {
            return;
        }

        final long now = System.nanoTime();
        final long msSinceT0 = (now - t0Nanos) / 1_000_000L;
        final long msSinceProcessStart = (now - STARTUP_T0_NANOS) / 1_000_000L;
        System.out.println("[startup] " + label + " | +" + msSinceT0 + "ms | total=" + msSinceProcessStart + "ms");
    }

    /** Emits early bootstrap timings before options/debug flags are loaded. */
    private static void logBootstrapCheckpoint(String label) {
        if (!bootstrapTimingEnabled) {
            return;
        }
        final long now = System.nanoTime();
        final long msSinceProcessStart = (now - STARTUP_T0_NANOS) / 1_000_000L;
        System.out.println("[startup-bootstrap] " + label + " | total=" + msSinceProcessStart + "ms");
    }

    /**
     * Optional launch diagnostic that asks the JDK classpath loader to print URLs
     * as classes/resources are resolved (including jar URLs opened pre-init()).
     */
    private static void enableLaunchJarUrlDebugIfRequested() {
        if (!Boolean.getBoolean(STARTUP_LAUNCH_JAR_URL_DEBUG_FLAG)) {
            return;
        }
        try {
            System.setProperty("sun.misc.URLClassPath.debug", "true");
            logBootstrapCheckpoint("main(): enabled sun.misc.URLClassPath.debug for jar URL diagnostics");
        } catch (Throwable t) {
            System.out.println("[startup-bootstrap] unable to enable jar URL diagnostics: " + t.getMessage());
        }
    }

    private interface StartupStopCondition {
        boolean shouldStop();
    }

    private static Thread startStartupWatchdog(
            String threadName,
            String phaseLabel,
            Thread watchedThread,
            AtomicBoolean done,
            StartupStopCondition stopCondition) {
        Thread t = new Thread(() -> {
            String lastTopFrame = null;
             while (!done.get()) {
                 try {
                     Thread.sleep(STARTUP_WATCHDOG_INTERVAL_MS);
                 } catch (InterruptedException ie) {
                     return;
                 }
                 if (done.get()) {
                    return;
                 }
                 if (stopCondition != null && stopCondition.shouldStop()) {
                    return;
                 }
                 if (watchedThread == null) {
                     continue;
                 }
                 StackTraceElement[] trace = watchedThread.getStackTrace();
                 String top = (trace != null && trace.length > 0) ? trace[0].toString() : "<no-stack>";
                 System.out.println("[startup-watchdog] " + phaseLabel
                        + " still busy on thread '" + watchedThread.getName() + "' top=" + top);

                // Optional deeper stack emission for startup stalls dominated by class/resource inflation.
                if (Boolean.getBoolean(STARTUP_WATCHDOG_VERBOSE_STACK_FLAG)
                        && trace != null
                        && trace.length > 0
                        && !top.equals(lastTopFrame)
                        && (top.contains("ZipFile") || top.contains("Inflater") || top.contains("ClassLoader"))) {
                    lastTopFrame = top;
                    int depth = Math.min(12, trace.length);
                    StringBuilder sb = new StringBuilder();
                    sb.append("[startup-watchdog] ").append(phaseLabel)
                      .append(" stack snapshot:");
                    for (int i = 0; i < depth; i++) {
                        sb.append("\n    at ").append(trace[i]);
                    }
                    System.out.println(sb.toString());
                    // When launch stalls in URLJarFile/ZipFile, print classloader URLs so we can
                    // identify the exact jar/rsrc URL chain being resolved.
                    logLaunchClassLoaderUrlsForThread(phaseLabel, watchedThread);
                }
             }
         }, threadName);
         t.setDaemon(true);
         t.start();
         return t;
    }

    private static void logLaunchClassLoaderUrlsForThread(String phaseLabel, Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[startup-watchdog] ").append(phaseLabel)
              .append(" classloader URLs:\n");
            appendClassLoaderUrls(sb, "thread-context", thread.getContextClassLoader());
            appendClassLoaderUrls(sb, "system", ClassLoader.getSystemClassLoader());
            String message = sb.toString();
            if (message.equals(lastLaunchClassLoaderUrlsSignature)) {
                return;
            }
            lastLaunchClassLoaderUrlsSignature = message;
            System.out.println(message);
        } catch (Throwable ignored) {
            // Best-effort diagnostics only.
        }
    }

    private static void appendClassLoaderUrls(StringBuilder sb, String label, ClassLoader classLoader) {
        sb.append("    ").append(label).append(": ");
        if (classLoader == null) {
            sb.append("<bootstrap>\n");
            return;
        }
        sb.append(classLoader.getClass().getName()).append("\n");
        if (classLoader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            if (urls == null || urls.length == 0) {
                sb.append("      <no-urls>\n");
            } else {
                for (URL url : urls) {
                    sb.append("      ").append(url).append("\n");
                }
            }
        }

        ClassLoader parent = classLoader.getParent();
        int depth = 0;
        while (parent != null && depth < 5) {
            sb.append("      parent[").append(depth).append("]: ")
              .append(parent.getClass().getName()).append("\n");
            if (parent instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) parent).getURLs();
                if (urls == null || urls.length == 0) {
                    sb.append("        <no-urls>\n");
                } else {
                    for (URL url : urls) {
                        sb.append("        ").append(url).append("\n");
                    }
                }
            }
            parent = parent.getParent();
            depth++;
        }
        if (parent == null) {
            sb.append("      parent[").append(depth).append("]: <bootstrap>\n");
        }
    }

    /**
     * During launch(args), sample JavaFX/runtime threads to expose where startup
     * time is spent before Application.init() is entered.
     */
    private static Thread startLaunchPhaseThreadDumpWatchdog(
            AtomicBoolean done,
            StartupStopCondition stopCondition) {
        Thread t = new Thread(() -> {
            while (!done.get()) {
                try {
                    Thread.sleep(STARTUP_WATCHDOG_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    return;
                }
                if (done.get()) {
                    return;
                }
                if (stopCondition != null && stopCondition.shouldStop()) {
                    return;
                }

                Map<Thread, StackTraceElement[]> allTraces = Thread.getAllStackTraces();
                StringBuilder snapshot = new StringBuilder();
                int tracked = 0;

                for (Map.Entry<Thread, StackTraceElement[]> entry : allTraces.entrySet()) {
                    Thread thread = entry.getKey();
                    if (thread == null || !thread.isAlive()) {
                        continue;
                    }
                    String name = thread.getName();
                    if (!isLaunchDiagnosticThreadName(name)) {
                        continue;
                    }

                    StackTraceElement[] trace = entry.getValue();
                    String top = (trace != null && trace.length > 0) ? trace[0].toString() : "<no-stack>";
                    snapshot.append(name)
                            .append("[")
                            .append(thread.getState())
                            .append("] top=")
                            .append(top)
                            .append(" | ");
                    tracked++;

                    if (Boolean.getBoolean(STARTUP_WATCHDOG_VERBOSE_STACK_FLAG)
                            && trace != null
                            && trace.length > 0
                            && (top.contains("ZipFile") || top.contains("Inflater") || top.contains("ClassLoader"))) {
                        int depth = Math.min(10, trace.length);
                        StringBuilder deep = new StringBuilder();
                        deep.append("[startup-watchdog] launch(args) thread '")
                                .append(name)
                                .append("' stack snapshot:");
                        for (int i = 0; i < depth; i++) {
                            deep.append("\n    at ").append(trace[i]);
                        }
                        System.out.println(deep.toString());
                    }
                }

                if (tracked == 0) {
                    continue;
                }

                String signature = snapshot.toString();
                if (signature.equals(lastLaunchThreadsSnapshotSignature)) {
                    continue;
                }
                lastLaunchThreadsSnapshotSignature = signature;
                System.out.println("[startup-watchdog] launch(args) active runtime threads: " + signature);
            }
        }, "glimpse-launch-threads-watchdog");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static boolean isLaunchDiagnosticThreadName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.startsWith("JavaFX")
                || normalized.contains("Launcher")
                || normalized.contains("Quantum")
                || normalized.contains("Prism")
                || normalized.contains("Glass")
                || normalized.startsWith("AWT-EventQueue");
    }

    /** Package-visible helper so startup builders can emit granular checkpoint logs. */
    static void logStartupBuildCheckpoint(String label) {
        logStartupCheckpoint(label, STARTUP_T0_NANOS);
    }

    /**
     * Gives JavaFX at least one render pulse after stage show before heavy startup work.
     */
    private static void runAfterInitialFxPulse(Runnable task) {
        if (task == null) {
            return;
        }
        Platform.runLater(() -> Platform.runLater(task));
    }

    private static int clampRuntimeFontSize(int requestedFontSize) {
        return Math.max(MIN_RUNTIME_FONT_SIZE, Math.min(MAX_RUNTIME_FONT_SIZE, requestedFontSize));
    }

    private static String mergeFontStyle(String style, int fontSize) {
        String baseStyle = style == null ? "" : style;
        String fontStyle = "-fx-font-size: " + fontSize + "px;";
        if (baseStyle.matches("(?s).*?-fx-font-size\\s*:.*")) {
            return baseStyle.replaceAll("-fx-font-size\\s*:\\s*[-+]?[0-9]*\\.?[0-9]+px\\s*;?", fontStyle);
        }
        return (baseStyle + " " + fontStyle).trim();
    }

    private static void applyFontSizeRecursively(Node node, int fontSize) {
        if (node == null) {
            return;
        }
        try {
            node.setStyle(mergeFontStyle(node.getStyle(), fontSize));
        } catch (Exception ignored) {
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                applyFontSizeRecursively(child, fontSize);
            }
        }
    }

    /**
     * Applies font size immediately across the visible JavaFX window without persisting to disk.
     */
    public static void applyRuntimeFontSize(int requestedFontSize) {
        Runnable applyTask = () -> {
            int fontSize = clampRuntimeFontSize(requestedFontSize);
            GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
            vars.setPreferredFontSize(Integer.toString(fontSize));

            Stage stage = primaryStage;
            if (stage == null) {
                return;
            }
            Scene scene = stage.getScene();
            if (scene == null || scene.getRoot() == null) {
                return;
            }

            applyFontSizeRecursively(scene.getRoot(), fontSize);
            scene.getRoot().requestLayout();
        };

        if (Platform.isFxApplicationThread()) {
            applyTask.run();
        } else {
            Platform.runLater(applyTask);
        }
    }

    /**
     * Returns the currently effective runtime font size used by the UI.
     *
     * @return clamped runtime font size in pixels
     */
    public static int getRuntimeFontSize() {
        try {
            String raw = GLIMPSEVariables.getInstance().getPreferredFontSize();
            return clampRuntimeFontSize(Integer.parseInt(raw));
        } catch (Exception ignored) {
            return clampRuntimeFontSize(GLIMPSEStyles.getInstance().getFontSize());
        }
    }

    /** Returns the minimum allowed runtime font size. */
    public static int getMinRuntimeFontSize() {
        return MIN_RUNTIME_FONT_SIZE;
    }

    /** Returns the maximum allowed runtime font size. */
    public static int getMaxRuntimeFontSize() {
        return MAX_RUNTIME_FONT_SIZE;
    }

    /** Returns the primary JavaFX stage. */
    public static Stage getPrimaryStage() { return primaryStage; }
    /** Returns the resolved options filename (if provided at launch). */
    public static String getOptionsFilename() { return optionsFilename; }
    /** Returns the create-scenario pane instance. */
    public static PaneCreateScenario getPaneCreateScenario() { return paneCreateScenario; }
    /** Returns the scenario-library pane instance. */
    public static PaneScenarioLibrary getPaneScenarioLibrary() { return paneScenarioLibrary; }
    /** Returns the component-library pane instance. */
    public static PaneComponentLibrary getPaneComponentLibrary() { return paneComponentLibrary; }

    /**
     * Triggers a status-bar resource-stats re-render if the scenario pane is available
     * and startup is no longer busy. Safe to call from any thread.
     * <p>
     * Called by {@link glimpseUtil.UtilsStatus} when an async database-size calculation
     * finishes so the status bar can display the real value without waiting for the
     * next full scenario refresh cycle.
     */
    public static void refreshStatusBarComputerStats() {
        if (startupBusyState || areInitialLibraryLoadsPending()) {
            return;
        }
        final PaneScenarioLibrary pane = paneScenarioLibrary;
        if (pane == null) {
            return;
        }
        Runnable task = pane::refreshStatusBarComputerStats;
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Requests a refresh of the database-size portion of the status bar summary.
     *
     * @param force when true, bypass the normal recent-cache reuse behavior
     */
    public static void requestDatabaseSizeRefresh(boolean force) {
        final Client inst = instanceForStatus;
        if (inst == null) {
            return;
        }
        inst.utils.requestDatabaseSizeRefresh(force);
        refreshStatusBarComputerStats();
    }

    /** Requests a refresh of the database-size portion of the status bar summary. */
    public static void requestDatabaseSizeRefresh() {
        requestDatabaseSizeRefresh(false);
    }
    /** Returns the single-right-arrow button. */
    public static Button getButtonRightArrow() { return buttonRightArrow; }
    /** Returns the single-left-arrow button. */
    public static Button getButtonLeftArrow() { return buttonLeftArrow; }
    /** Returns the double-left-arrow button. */
    public static Button getButtonLeftDoubleArrow() { return buttonLeftDoubleArrow; }
    /** Returns the Edit Scenario button. */
    public static Button getButtonEditScenario() { return buttonEditScenario; }
    /** Returns the Delete Component button. */
    public static Button getButtonDeleteComponent() { return buttonDeleteComponent; }
    /** Returns the Refresh Components button. */
    public static Button getButtonRefreshComponents() { return buttonRefreshComponents; }
    /** Returns the New Component button. */
    public static Button getButtonNewComponent() { return buttonNewComponent; }
    /** Returns the Edit Component button. */
    public static Button getButtonEditComponent() { return buttonEditComponent; }
    /** Returns the Browse Component Library button. */
    public static Button getButtonBrowseComponentLibrary() { return buttonBrowseComponentLibrary; }
    /** Returns the Move Component Up button. */
    public static Button getButtonMoveComponentUp() { return buttonMoveComponentUp; }
    /** Returns the Move Component Down button. */
    public static Button getButtonMoveComponentDown() { return buttonMoveComponentDown; }
    /** Returns the Create Scenario Config File button. */
    public static Button getButtonCreateScenarioConfigFile() { return buttonCreateScenarioConfigFile; }
    /** Returns the View Config button. */
    public static Button getButtonViewConfig() { return buttonViewConfig; }
    /** Returns the View Log button. */
    public static Button getButtonViewLog() { return buttonViewLog; }
    /** Returns the View Executable Log button. */
    public static Button getButtonViewExeLog() { return buttonViewExeLog; }
    /** Returns the View Errors button. */
    public static Button getButtonViewErrors() { return buttonViewErrors; }
    /** Returns the View Executable Errors button. */
    public static Button getButtonViewExeErrors() { return buttonViewExeErrors; }
    /** Returns the Browse Scenario Folder button. */
    public static Button getButtonBrowseScenarioFolder() { return buttonBrowseScenarioFolder; }
    /** Returns the Import Scenario button. */
    public static Button getButtonImportScenario() { return buttonImportScenario; }
    /** Returns the Diff Files button. */
    public static Button getButtonDiffFiles() { return buttonDiffFiles; }
    /** Returns the Show Run Queue button. */
    public static Button getButtonShowRunQueue() { return buttonShowRunQueue; }
    /** Returns the Refresh Scenario Status button. */
    public static Button getButtonRefreshScenarioStatus() { return buttonRefreshScenarioStatus; }
    /** Returns the Delete Scenario button. */
    public static Button getButtonDeleteScenario() { return buttonDeleteScenario; }
    /** Returns the Run Scenario button. */
    public static Button getButtonRunScenario() { return buttonRunScenario; }
    /** Returns the Stop Scenario button. */
    public static Button getButtonStopScenario() { return buttonStopScenario; }
    /** Returns the Results button. */
    public static Button getButtonResults() { return buttonResults; }
    /** Returns the Results-for-selected button. */
    public static Button getButtonResultsForSelected() { return buttonResultsForSelected; }
    /** Returns the Archive Scenario button. */
    public static Button getButtonArchiveScenario() { return buttonArchiveScenario; }
    /** Returns the Report button. */
    public static Button getButtonReport() { return buttonReport; }
    /** Returns the Examine Scenario button. */
    public static Button getButtonExamineScenario() { return buttonExamineScenario; }
    /** Returns the GCAM execution-thread wrapper. */
    public static ExecutionThread getgCAMExecutionThread() { return gCAMExecutionThread; }
    /** Returns the ModelInterface execution-thread wrapper. */
    public static ExecutionThread getgCAMPPExecutionThread() { return modelInterfaceExecutionThread; }

    /**
     * Increments active scenario-operation count and shows right-side status progress indicator.
     * <p>
     * Safe to call from any thread.
     */
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

    /**
     * Decrements active scenario-operation count and hides the indicator when count reaches zero.
     * <p>
     * Safe to call from any thread.
     */
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

    private static void showEarlyStartupSplash(String message) {
        if (!earlySplashVisible.compareAndSet(false, true)) {
            updateEarlyStartupSplashMessage(message);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.JWindow window = new javax.swing.JWindow();
                window.setAlwaysOnTop(true);

                javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
                content.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

                javax.swing.JLabel heading = new javax.swing.JLabel("GLIMPSE startup:");
                heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 14f));

                javax.swing.JLabel messageLabel = new javax.swing.JLabel(
                        (message == null || message.trim().isEmpty()) ? EARLY_SPLASH_INITIAL_MESSAGE : message.trim());
                javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
                bar.setIndeterminate(true);
                bar.setStringPainted(false);

                javax.swing.JPanel textPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1, 0, 4));
                textPanel.add(heading);
                textPanel.add(messageLabel);
                content.add(textPanel, java.awt.BorderLayout.CENTER);
                content.add(bar, java.awt.BorderLayout.SOUTH);

                window.setContentPane(content);
                window.setSize(430, 110);
                window.setLocationRelativeTo(null);
                earlySplashWindow = window;
                earlySplashLabel = messageLabel;
                earlySplashProgressBar = bar;
                window.setVisible(true);
            } catch (Throwable t) {
                earlySplashVisible.set(false);
            }
        });
    }

    private static void updateEarlyStartupSplashMessage(String message) {
        if (!earlySplashVisible.get()) {
            return;
        }
        final String safeMessage = (message == null || message.trim().isEmpty())
                ? EARLY_SPLASH_INITIAL_MESSAGE
                : message.trim();
        SwingUtilities.invokeLater(() -> {
            try {
                if (earlySplashLabel != null) {
                    earlySplashLabel.setText(safeMessage);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private static String buildEarlySplashStepText() {
        int completed = Math.max(0, Math.min(STARTUP_TOTAL_STEPS, startupStepsCompleted));
        if (completed <= 0) {
            return "Preparing startup...";
        }
        return "Step " + completed + " of " + STARTUP_TOTAL_STEPS;
    }

    private static String buildEarlySplashProgressText() {
        int completed = Math.max(0, Math.min(STARTUP_TOTAL_STEPS, startupStepsCompleted));
        int percent = (int) Math.round((completed * 100.0) / STARTUP_TOTAL_STEPS);
        return percent + "%";
    }

    private static void closeEarlyStartupSplash() {
        if (!earlySplashVisible.compareAndSet(true, false)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                if (earlySplashWindow != null) {
                    earlySplashWindow.setVisible(false);
                    earlySplashWindow.dispose();
                }
            } catch (Throwable ignored) {
            } finally {
                earlySplashWindow = null;
                earlySplashLabel = null;
                earlySplashProgressBar = null;
            }
        });
    }
}
