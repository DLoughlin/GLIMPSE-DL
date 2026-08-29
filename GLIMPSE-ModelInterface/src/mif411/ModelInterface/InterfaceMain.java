/*
* LEGAL NOTICE
* This computer software was prepared by Battelle Memorial Institute,
* hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
* with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
* CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
* Distributed as open-source under the terms of the Educational Community 
* License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
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
*/
package ModelInterface;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ModelInterface.ConfigurationEditor.configurationeditor.ConfigurationEditor;
import ModelInterface.ConfigurationEditor.utils.DOMUtils;
import ModelInterface.ConfigurationEditor.utils.FileUtils;
import ModelInterface.ModelGUI2.CSVFilter;
//Dan: commented this out
//import ModelInterface.DMsource.DMViewer;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.InputViewer;
import ModelInterface.ModelGUI2.XMLFilter;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.PPsource.PPViewer;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.common.RecentFilesList;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import com.sun.media.imageioimpl.common.PackageUtil;
import java.lang.reflect.Field;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class InterfaceMain implements ActionListener, PreferenceDialogCallbacks {
	private enum StatusBarProgressMode {
		NONE,
		QUERY,
		SHUTDOWN
	}

	private enum LoadingViewMode {
		STARTUP,
		SHUTDOWN
	}
	private static final boolean DEBUG = false;
	private static final int STARTUP_MESSAGE_LINGER_MS = 200;
	private static final int STARTUP_PROGRESS_MAX = 100;
	private static final int STARTUP_TOTAL_STEPS = 5;
	private static final int SHUTDOWN_TOTAL_STEPS = 4;
	private static final int SHUTDOWN_STEP_LINGER_MS = 120;
	private static final String STARTUP_MESSAGE_WITH_DB = "Starting ModelInterface...";
	private static final String STARTUP_MESSAGE_WITHOUT_DB = "Starting ModelInterface... waiting for database selection.";
	private static final String STARTUP_MESSAGE_INITIALIZING = "Loading interface...";
	private static final String STARTUP_MESSAGE_STATUS_BAR = "Preparing workspace...";
	private static final String STARTUP_MESSAGE_DB_VIEW = "Preparing database view...";
	private static final String STARTUP_MESSAGE_DB_PROMPT = "Waiting for database choice...";
	private static final String STARTUP_MESSAGE_OPENING_DB = "Opening database...";
	private static final String STARTUP_MESSAGE_READY = "Ready.";
	private static final String SHUTDOWN_MESSAGE_STARTING = "Shutting down...";
	private static final String SHUTDOWN_MESSAGE_SAVING_WINDOW = "Shutting down... saving window state.";
	private static final String SHUTDOWN_MESSAGE_SAVING_SETTINGS = "Shutting down... saving settings.";
	private static final String SHUTDOWN_MESSAGE_CLOSING_DB = "Shutting down... closing database.";
	private static final String SHUTDOWN_MESSAGE_EXITING = "Shutting down... closing application.";
	private static javax.swing.Timer pendingStartupDbViewerTimer;
	// Restore missing shutdown guard for orderly exit.
	private final java.util.concurrent.atomic.AtomicBoolean shuttingDown = new java.util.concurrent.atomic.AtomicBoolean(false);
	private static final String LAZY_OPEN_INPUT_VIEWER = "LazyOpen:InputViewer";
	private static final String LAZY_OPEN_PP_VIEWER = "LazyOpen:PPViewer";
	private static final String LAZY_OPEN_CONFIGURATION_EDITOR = "LazyOpen:ConfigurationEditor";
	private static final long STARTUP_NANOS = System.nanoTime();

	// Use platform Look & Feel defaults for fonts (do not force a unified size).
	private static final Color UNIFIED_BG = new Color(245, 245, 250); // Soft background
	private static final Color UNIFIED_PANEL_BG = new Color(255, 255, 255); // Panel background
	private static final Color UNIFIED_BTN_BG = new Color(230, 235, 245); // Button background
	private static final Color UNIFIED_BTN_FG = new Color(30, 30, 60); // Button foreground
	private static final Color UNIFIED_BORDER = new Color(200, 200, 220); // Border color

	private JPanel startupLoadingView;
	private JLabel startupLoadingLabel;
	private JProgressBar startupLoadingBar;
	private int startupStepsCompleted;
	private int startupTotalSteps = STARTUP_TOTAL_STEPS;
	private String startupDefaultMessage = STARTUP_MESSAGE_WITH_DB;
	private int shutdownStepsCompleted;
	private int shutdownTotalSteps = SHUTDOWN_TOTAL_STEPS;
	private String shutdownDefaultMessage = SHUTDOWN_MESSAGE_STARTING;
	private LoadingViewMode loadingViewMode = LoadingViewMode.STARTUP;

	/**
	 * Split a delimited list property (e.g., year lists) supporting either ';' or ','
	 * as separators, and trimming optional whitespace.
	 * <p>
	 * Examples accepted: "2015;2020;2025", "2015,2020,2025", "2015; 2020, 2025".
	 */
	public static String[] splitListProperty(final String value) {
		if (value == null) {
			return new String[0];
		}
		final String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return new String[0];
		}
		// split on ';' or ',' with optional surrounding whitespace
		return trimmed.split("\\s*[;,]\\s*");
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	private static final String STARTUP_TIMING_PROPERTY = "do_output_timings";
	public static final String FONT_SIZE_PROPERTY = "fontSize";
	public static final String GRAPHICS_TITLE_FONT_SIZE_PROPERTY = "graphicsTitleFontSize";
	public static final String GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY = "graphicsSubtitleFontSize";
	public static final String GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY = "graphicsAxisLabelFontSize";
	public static final String GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY = "graphicsAxisTickFontSize";
	public static final String GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY = "graphicsDomainAxisLabelFontSize";
	public static final String GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY = "graphicsDomainAxisTickFontSize";
	public static final String GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY = "graphicsRangeAxisLabelFontSize";
	public static final String GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY = "graphicsRangeAxisTickFontSize";
	public static final String GRAPHICS_LEGEND_FONT_SIZE_PROPERTY = "graphicsLegendFontSize";
	public static final String GRAPHICS_LINE_WIDTH_SCALE_PROPERTY = "graphicsLineWidthScale";
	public static final String GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY = "graphicsThumbnailFontSize";
	public static final String GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY = "graphicsThumbnailLineWidth";
	private static final int DEFAULT_FONT_SIZE = 12;
	private static final int MIN_FONT_SIZE = 8;
	private static final int MAX_FONT_SIZE = 32;
	private static final int[] GENERAL_FONT_SIZE_OPTIONS =
			new int[] { 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24 };
	public static final int DEFAULT_GRAPHICS_TITLE_FONT_SIZE = 17;
	public static final int DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE = 14;
	public static final int DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE = 17;
	public static final int DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE = 17;
	public static final int DEFAULT_GRAPHICS_LEGEND_FONT_SIZE = 17;
	public static final double DEFAULT_GRAPHICS_LINE_WIDTH_SCALE = 1.0d;
	public static final int DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE = 11;
	public static final double DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH = 1.5d;
	public static final int MIN_GRAPHICS_FONT_SIZE = 8;
	public static final int MAX_GRAPHICS_FONT_SIZE = 48;
	public static final double MIN_GRAPHICS_LINE_WIDTH_SCALE = 0.25d;
	public static final double MAX_GRAPHICS_LINE_WIDTH_SCALE = 5.0d;
	private static volatile boolean outputStartupTimings = false;
	private static volatile int configuredFontSize = DEFAULT_FONT_SIZE;

	private static int clampFontSize(final int fontSize) {
		return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
	}

	public static String[] getGeneralFontSizeOptions() {
		String[] options = new String[GENERAL_FONT_SIZE_OPTIONS.length];
		for (int i = 0; i < GENERAL_FONT_SIZE_OPTIONS.length; i++) {
			options[i] = Integer.toString(GENERAL_FONT_SIZE_OPTIONS[i]);
		}
		return options;
	}

	private static boolean isGeneralFontSizeOption(final int fontSize) {
		for (int option : GENERAL_FONT_SIZE_OPTIONS) {
			if (option == fontSize) {
				return true;
			}
		}
		return false;
	}

	public static Integer parseGeneralFontSizeOption(final String rawValue) {
		if (rawValue == null || rawValue.trim().isEmpty()) {
			return null;
		}
		try {
			int parsed = Integer.parseInt(rawValue.trim());
			return isGeneralFontSizeOption(parsed) ? Integer.valueOf(parsed) : null;
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	private static String getGeneralFontSizeOptionsForLog() {
		return Arrays.toString(getGeneralFontSizeOptions());
	}

	public static int parseFontSizeValue(final String rawValue, final int fallback) {
		if (rawValue == null || rawValue.trim().isEmpty()) {
			return clampFontSize(fallback);
		}
		try {
			return clampFontSize(Integer.parseInt(rawValue.trim()));
		} catch (NumberFormatException nfe) {
			return clampFontSize(fallback);
		}
	}

	public static int parseBoundedIntValue(final String rawValue, final int fallback, final int min, final int max) {
		int boundedFallback = Math.max(min, Math.min(max, fallback));
		if (rawValue == null || rawValue.trim().isEmpty()) {
			return boundedFallback;
		}
		try {
			int parsed = Integer.parseInt(rawValue.trim());
			return Math.max(min, Math.min(max, parsed));
		} catch (NumberFormatException nfe) {
			return boundedFallback;
		}
	}

	public static double parseBoundedDoubleValue(final String rawValue, final double fallback, final double min,
			final double max) {
		double boundedFallback = Math.max(min, Math.min(max, fallback));
		if (rawValue == null || rawValue.trim().isEmpty()) {
			return boundedFallback;
		}
		try {
			double parsed = Double.parseDouble(rawValue.trim());
			return Math.max(min, Math.min(max, parsed));
		} catch (NumberFormatException nfe) {
			return boundedFallback;
		}
	}

	public static int resolveGraphicsFontSize(final Properties props, final String propertyKey,
			final String legacyPropertyKey, final int defaultValue) {
		String rawValue = null;
		if (props != null) {
			rawValue = props.getProperty(propertyKey);
			if ((rawValue == null || rawValue.trim().isEmpty()) && legacyPropertyKey != null) {
				rawValue = props.getProperty(legacyPropertyKey);
			}
		}
		return parseBoundedIntValue(rawValue, defaultValue, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE);
	}

	public static int resolveConfiguredFontSize(final Properties props) {
		if (props == null) {
			return clampFontSize(configuredFontSize);
		}
		return parseFontSizeValue(props.getProperty(FONT_SIZE_PROPERTY), DEFAULT_FONT_SIZE);
	}

	public static int getConfiguredFontSize() {
		return configuredFontSize;
	}

	private static void applyConfiguredUIFontDefaults() {
		java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource) {
				java.awt.Font baseFont = (java.awt.Font) value;
				if (baseFont.getSize() != configuredFontSize) {
					UIManager.put(key, new javax.swing.plaf.FontUIResource(
							baseFont.getName(), baseFont.getStyle(), configuredFontSize));
				}
			}
		}
	}

	private static void refreshFontSensitiveComponentMetrics(java.awt.Component comp) {
		if (comp == null) {
			return;
		}
		if (comp instanceof javax.swing.JTable) {
			javax.swing.JTable table = (javax.swing.JTable) comp;
			java.awt.Font tableFont = table.getFont();
			if (tableFont != null) {
				table.setRowHeight(Math.max(table.getRowHeight(), tableFont.getSize() + 8));
			}
		}
		if (comp instanceof javax.swing.JTree) {
			javax.swing.JTree tree = (javax.swing.JTree) comp;
			java.awt.Font treeFont = tree.getFont();
			if (treeFont != null) {
				tree.setRowHeight(treeFont.getSize() + 5);
			}
		}
		if (comp instanceof java.awt.Container) {
			for (java.awt.Component child : ((java.awt.Container) comp).getComponents()) {
				refreshFontSensitiveComponentMetrics(child);
			}
		}
	}

	public void applyConfiguredFontSizeNow(final int newFontSize) {
		configuredFontSize = clampFontSize(newFontSize);
		applyConfiguredUIFontDefaults();
		Runnable applyTask = new Runnable() {
			@Override
			public void run() {
				for (java.awt.Window window : java.awt.Window.getWindows()) {
					if (!window.isDisplayable()) {
						continue;
					}
					javax.swing.SwingUtilities.updateComponentTreeUI(window);
					refreshFontSensitiveComponentMetrics(window);
					window.invalidate();
					window.validate();
					window.repaint();
				}
				if (startupLoadingLabel != null) {
					startupLoadingLabel.setFont(startupLoadingLabel.getFont().deriveFont(java.awt.Font.BOLD,
							(float) (configuredFontSize + 7)));
				}
			};
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			applyTask.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(applyTask);
		}
	}

	public static boolean shouldOutputStartupTimings() {
		return outputStartupTimings;
	}

	public static void configureStartupTimingOutput(Properties props) {
		String value = System.getProperty(STARTUP_TIMING_PROPERTY);
		if (value == null && props != null) {
			value = props.getProperty(STARTUP_TIMING_PROPERTY);
		}
		outputStartupTimings = value != null && Boolean.parseBoolean(value.trim());
	}

	public static void logStartupTiming(String message) {
		if (!shouldOutputStartupTimings()) {
			return;
		}
		System.out.println("[startup] " + message);
	}

	private static void logStartup(String stage) {
		logStartupTiming(stage + " @ " + elapsedMillis(STARTUP_NANOS) + " ms");
	}

	public static final int FILE_MENU_POS = 0;
	public static final int EDIT_MENU_POS = 1;
	public static final int VIEW_MENU_POS = 2;
	
	public static final int TOOLS_MENU_POS = 90; // YD added
	// Legacy name used by DbViewer to locate the Edit -> Query Tree submenu.
	public static final int TOOLS_SUBMENU1_POS = 2; // alias of EDIT_QUERY_SUBMENU_POS
	public static final int TOOLS_SUBMENU15_POS = 2; // YD added
	public static final int TOOLS_SUBMENU2_POS = 5; // YD added
	public static final int QUERIES_UNDO_MENUITEM_POS = 25; // YD added
	public static final int QUERIES_REDO_MENUITEM_POS = 30; // YD added
	public static final int HELP_MENU_POS = 100;
	public static final int FILE_NEW_MENUITEM_POS = 0;
	public static final int FILE_OPEN_SUBMENU_POS = 5;
	public static final int FILE_TABS_SUBMENU_POS = 20;
	public static final int FILE_MENU_SEPERATOR = 30;
	public static final int FILE_QUIT_MENUITEM_POS = 50;
	public static final int QUERIES_SAVE_MENUITEM_POS = 35; // YD changed
	public static final int QUERIES_SAVEAS_MENUITEM_POS = 40; // YD changed

	public static final int EDIT_QUERY_SUBMENU_POS = 2; // Edit -> Query Tree submenu position
	// Favorites submenu under Edit (used by DbViewer to register favorite query actions)
	public static final int EDIT_FAVORITES_SUBMENU_POS = 3;
	public static final int EDIT_COPY_MENUITEM_POS = 10;
	public static final int EDIT_PASTE_MENUITEM_POS = 11;
	
	public static final int TOOLS_CSV_MENUITEM_POS = 20; // YD added
	public static final int TOOLS_UNIT_MENUITEM_POS = 2; // YD added
	public static final int TOOLS_SANKEY_MENUITEM_POS = 3;// YD added
	public static final int SANKEY_LOAD_MENUITEM_POS = 60; // YD added
	public static final int SANKEY_DISPLAY_MENUITEM_POS = 70; // YD added
	public static final String REGION_LIST_NAME = "region list"; // YD added

	// New menu position for Config (between Tools and Help)
	public static final int CONFIG_MENU_POS = 95;

	private static File propertiesFile;
	private static String oldControl;
	private static InterfaceMain main;
	private JMenuItem saveMenu;
	private JMenuItem saveAsMenu;
	private JMenuItem quitMenu;
	private JMenuItem undoMenu;
	private JMenuItem redoMenu;
	private JMenuItem batchMenu;
	private JMenuItem selectQueryMenu;
	private JMenuItem editQueryFileMenu; // Open current query file in XML editor

	private Properties savedProperties;
	private final Object propertiesLock = new Object();
	private UndoManager undoManager;

	// New: Help menu primary item
	private JMenuItem helpItem;

	private MenuAdder dbView = null;

	private List<MenuAdder> menuAdders;
	private MenuAdder inputView = null;
	private MenuAdder ppView = null;
	private MenuAdder confEditor = null;
	static String path = null;
	static String queryFilename = null;

	ArrayList<String> energyNameList = null; // YD added

	public static String unitFileLocation = null;
	public static String presetRegionListLocation = null; // YD added,Feb-2024
	public static String favoriteQueriesFileLocation = null; // YD added,Feb-2024
	public static String stateShapeFileLocation = null; // YD added,May-2024
	public static String gcamReg32ShapeFileLocation = null; // YD added,July-2024
	public static String gcamReg32US52ShapeFileLocation = null; // YD added,July-2024
	public static boolean enableMapping = false; // YD added,August-2024
	public static boolean enableSankey = false; // YD added,August-2024
	public static boolean autoGenerateGraphics = false; // DPL added, Feb-2026
	/**
	 * When true (default), the query tree is shown collapsed to the second group
	 * level on startup (e.g., queries -&gt; GLIMPSE queries -&gt; 1.Primary and final
	 * energy). When false, the full tree is expanded. Controlled by the
	 * "compress_tree" property in model_interface.properties.
	 */
	public static boolean compressTree = true; // DPL added, April-2026
	public static String shapeFileLocationPrefix = null;
	public static String legendBundlesLoc = null;

	/**
	 * Initialize shapefile locations from the configured map resource folder.
	 *
	 * This is needed because some map panels read the specific shapefile locations
	 * (stateShapeFileLocation, gcamReg32ShapeFileLocation, gcamReg32US52ShapeFileLocation)
	 * directly, and those were historically only set via the "Select Map Resource Folder"
	 * menu action.
	 *
	 * @param folderPath map resources folder (may be null/blank)
	 * @return true if initialization succeeded and mapping is enabled
	 */
	public static boolean initializeMappingFromFolder(String folderPath) {
		// Clear stale values first (avoid partial success leaving old paths around)
		stateShapeFileLocation = null;
		gcamReg32ShapeFileLocation = null;
		gcamReg32US52ShapeFileLocation = null;

		if (folderPath == null || folderPath.trim().isEmpty()) {
			enableMapping = false;
			return false;
		}
		File dir = new File(folderPath);
		if (!dir.exists() || !dir.isDirectory()) {
			enableMapping = false;
			return false;
		}

		shapeFileLocationPrefix = dir.getAbsolutePath();

		File state = new File(dir, "mapUS52Compact_from_rmap.shp");
		File reg32 = new File(dir, "mapGCAMReg32_from_rmap.shp");
		File reg32us52 = new File(dir, "mapGCAMReg32US52_from_rmap.shp");

		boolean ok = true;
		if (state.exists() && state.isFile()) {
			stateShapeFileLocation = state.getAbsolutePath();
		} else {
			ok = false;
		}
		if (reg32.exists() && reg32.isFile()) {
			gcamReg32ShapeFileLocation = reg32.getAbsolutePath();
		} else {
			ok = false;
		}
		if (reg32us52.exists() && reg32us52.isFile()) {
			gcamReg32US52ShapeFileLocation = reg32us52.getAbsolutePath();
		}
		// Note: reg32us52 is optional depending on use-case; don't fail mapping solely because it's absent.

		enableMapping = ok;
		return enableMapping;
	}

	/**
	 * The main GUI the rest of the GUI components of the ModelInterface will rely
	 * on.
	 */
	private JFrame mainFrame;

	/**
	 * Status bar (bottom of main frame)
	 */
	private JPanel statusBar;
	// New: stable root container so status bar is never lost.
	private JPanel rootContent;
	// Guard to avoid re-entrant contentPane handling
	private boolean suppressContentPaneListener = false;

	// Status bar widgets.
	private JLabel activeDbStatusLabel;
	private JLabel queryProgressLabel;
	private javax.swing.JProgressBar queryProgressBar;
	private StatusBarProgressMode statusBarProgressMode = StatusBarProgressMode.NONE;
	private final AtomicInteger activeDbStatusVersion = new AtomicInteger(0);
	private static final long DB_SIZE_KB = 1024L;
	private static final long DB_SIZE_MB = DB_SIZE_KB * 1024L;
	private static final long DB_SIZE_GB = DB_SIZE_MB * 1024L;
	private static final long DB_SIZE_TB = DB_SIZE_GB * 1024L;
	// Databases >= 0.1 GB are displayed in GB (avoids "432 MB" for a fraction-of-a-GB DB).
	private static final long DB_SIZE_GB_THRESHOLD = DB_SIZE_GB / 10L;

	/**
	 * Main function, creates a new thread for the gui and runs it.
	 */
	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++) {
			System.out.println("arg " + i + ": " + args[i]);
		}

		// we want this to always be in root of run environment
		propertiesFile = new File("model_interface.properties");
		if (!propertiesFile.exists()) {
			try {
				if (propertiesFile.createNewFile()) {
					System.out.println("Created new properties file: " + propertiesFile.getAbsolutePath());
					// Write a default empty properties XML to the new file
					try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
						new Properties().storeToXML(fos, "ModelInterface properties");
					}
				}
			} catch (IOException e) {
				System.err.println("Could not create properties file. Proceeding with defaults.");
				e.printStackTrace();
			}
		}
		System.out.println("Getting model properties from " + propertiesFile.getAbsolutePath());

		// Load properties early so we can apply precedence (CLI > properties > defaults)
		Properties bootProps = new Properties();
		if (propertiesFile.exists()) {
			try (FileInputStream fis = new FileInputStream(propertiesFile)) {
				bootProps.loadFromXML(fis);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		configureStartupTimingOutput(bootProps);
		configuredFontSize = resolveConfiguredFontSize(bootProps);
		bootProps.setProperty(FONT_SIZE_PROPERTY, Integer.toString(configuredFontSize));

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				if (XMLDB.isSuppressedBaseXResourceException(e)) {
					try {
						System.err.println("Suppressing BaseX packaged-resource stack trace in uncaught handler"
								+ (t == null ? "" : " for thread '" + t.getName() + "'"));
					} catch (Throwable ignored) {}
					return;
				}
				// IMPORTANT: Do not show any UI popup for uncaught exceptions.
				// These can be noisy (and sometimes misleading) during mapping and other background work.
				// Instead, always log to stderr so ScenarioBuilder / callers can capture it.
				try {
					System.err.println("Uncaught exception" + (t == null ? "" : " in thread '" + t.getName() + "'") + ": " + e);
				} catch (Throwable ignored) {}
				try {
					e.printStackTrace(System.err);
				} catch (Throwable ignored) {
					// Last resort.
					e.printStackTrace();
				}
			}
		});

		// -b <batch file> -l <log file> -o <dbpath>
		OptionParser parser = new OptionParser();
		parser.accepts("help", "print usage information").forHelp();
		parser.accepts("b", "XML batch file to process").withRequiredArg();
		parser.accepts("l", "log file into which to redirect ModelInterface output").withRequiredArg();
		parser.accepts("o", "path to XML DB").withRequiredArg();
		parser.accepts("q", "path to query file").withRequiredArg();
		parser.accepts("u", "Path to CSV file for unit conversions").withOptionalArg();
		parser.accepts("p", "Path to preset region list").withOptionalArg(); // YD added,Feb-2024
		parser.accepts("f", "Path to favorite queries file").withOptionalArg(); // YD added,Feb-2024
		parser.accepts("m", "Path to mapping directory").withOptionalArg();// YD added,May-2024
		parser.accepts("s", "General UI font size (allowed values: " + getGeneralFontSizeOptionsForLog() + ")")
				.withRequiredArg();
		parser.accepts("legend_bundle", "Path to the LegendBundle.properties file").withOptionalArg();
		parser.accepts("auto-generate-graphics", "Automatically generate graphics when a scenario is run.");
		parser.accepts("init-db", "Initialize a new XML database at the path given by -o and exit.");

		OptionSet opts = null;
		try {
			opts = parser.parse(args);
		} catch (OptionException e) {
			System.out.println("Unable to parse all options: " + e.toString());
			String[] buttons = { "Exit Program", "Lauch with no arguments" };
			int returnValue = JOptionPane.showOptionDialog(null,
					"Invalid Launch Argument found: " + e.toString().split(":")[1], "Bad Launch Argument",
					JOptionPane.YES_NO_CANCEL_OPTION, 0, null, buttons, buttons[0]);
			if (returnValue == 0) {
				System.exit(1);
			}
		}

		// this is to cover launching with a bad argument to blank screen.
		// specifically to ensure some of the default ones can be tried.
		if (opts == null) {
			String[] argsEmpty = new String[0];
			opts = parser.parse(argsEmpty);
		}

		if (opts.has("help")) {
			try {
				System.out.println("Usage: java -jar ModelInterface.jar -b <batch file> -l <log file>");
				parser.printHelpOn(System.out);
			} catch (Exception e) {
				System.err.println("Failed to write usage message");
				System.exit(1);
			}
			System.exit(1);
		}

		if (opts.has("s")) {
			String rawFontSize = String.valueOf(opts.valueOf("s"));
			Integer parsedCliFontSize = parseGeneralFontSizeOption(rawFontSize);
			if (parsedCliFontSize != null) {
				configuredFontSize = parsedCliFontSize.intValue();
				bootProps.setProperty(FONT_SIZE_PROPERTY, Integer.toString(configuredFontSize));
				System.out.println("InterfaceMain: font size set from command line (-s): " + configuredFontSize);
			} else {
				System.out.println("InterfaceMain: ignoring invalid -s font size '" + rawFontSize
						+ "'. Allowed values are " + getGeneralFontSizeOptionsForLog()
						+ ". Keeping font size " + configuredFontSize + ".");
			}
		}

		// if the -l option is set then we will redirect standard output to the
		// specified log file
		PrintStream stdout = System.out;
		if (opts.has("l")) {
			String logFile = (String) opts.valueOf("l");
			stdout.println("InterfaceMain: Directing stdout to " + logFile);
			try {
				FileOutputStream log = new FileOutputStream(logFile);
				System.setOut(new PrintStream(log));
			} catch (Exception e) {
				// If there was an error opening the log file we will post a message indicating
				// as
				// much but continue on with out the redirect.
				System.err.println("Failed to open log file '" + logFile + "' for writing: " + e);
			}
		}

		if (opts.has("o")) {
			path = (String) opts.valueOf("o");
			System.out.println("InterfaceMain: DB Path: " + path + " exists: " + new File(path).exists());
			// Persist last DB path so it is available on next launch
			bootProps.setProperty("paramPath", path);
		} else {
			// use value from properties if available
			String propPath = bootProps.getProperty("paramPath", null);
			if (propPath != null) {
				path = propPath;
				System.out.println("InterfaceMain: DB Path (from properties): " + path + " exists: " + new File(path).exists());
			}
		}

		if (opts.has("init-db")) {
			if (path == null || path.trim().isEmpty()) {
				System.err.println("init-db requested but no database path provided. Use -o <dbPath>.");
				System.exit(1);
			}
			try {
				System.out.println("Initializing database at: " + path);
				XMLDB.openDatabase(path, true);
				XMLDB.closeDatabase();
				System.out.println("Database initialized successfully.");
				System.exit(0);
			} catch (Exception e) {
				System.err.println("Failed to initialize database at " + path + ": " + e);
				e.printStackTrace(System.err);
				System.exit(2);
			}
		}

		// added by Dan to allow query file to be specified as runtime argument
		if (opts.has("q")) {
			queryFilename = (String) opts.valueOf("q");
			System.out.println("InterfaceMain: Query File Path: " + queryFilename + " exists: "
					+ new File(queryFilename).exists());
			bootProps.setProperty("queryFile", queryFilename);
		} else {
			// use value from properties if available
			String propQuery = bootProps.getProperty("queryFile", null);
			if (propQuery != null) {
				queryFilename = propQuery;
				System.out.println("InterfaceMain: Query File Path (from properties): " + queryFilename + " exists: "
						+ new File(queryFilename).exists());
			}
		}

		if (opts.has("b")) {
			String filename = (String) opts.valueOf("b");
			System.out.println("InterfaceMain: batchFile: " + filename + " exists: " + new File(filename).exists());

			System.setProperty("java.awt.headless", "true");
			System.out.println("Running headless? " + GraphicsEnvironment.isHeadless());
			Document batchDoc = filename.equals("-") ? DOMUtils.parseInputStream(System.in)
					: FileUtils.loadDocument(new File(filename), null);
			main = new InterfaceMain();

			// Construct the subset of menu adders that are also BatchRunner while
			// avoiding creating any GUI components
			// TODO: avoid code duplication
			final MenuAdder dbView = new DbViewer();
			final MenuAdder inputView = new InputViewer();
			main.menuAdders = new ArrayList<MenuAdder>(2);
			main.menuAdders.add(dbView);
			main.menuAdders.add(inputView);

			// Run the batch file
			if (batchDoc != null) {
				main.runBatch(batchDoc.getDocumentElement());
			} else {
				System.out.println("Skipping batch " + filename + " due to parsing errors.");
			}
			System.setOut(stdout);
			return;
		}

		// Units file precedence
		if (opts.has("u")) {
			unitFileLocation = (String) opts.valueOf("u");
			System.out.println("InterfaceMain: unitsFile: " + unitFileLocation + " exists: "
					+ new File(unitFileLocation).exists());
			bootProps.setProperty("unitsFile", unitFileLocation);
		} else {
			String propUnits = bootProps.getProperty("unitsFile", null);
			if (propUnits != null) {
				unitFileLocation = propUnits;
				System.out.println("InterfaceMain: unitsFile (from properties): " + unitFileLocation + " exists: "
						+ new File(unitFileLocation).exists());
			} else {
				// also look in the current directory
				File f = new File("config" + File.separator + "units_rules.csv");
				if (f.exists()) {
					unitFileLocation = f.getAbsolutePath();
				}
				System.out.println("    --> attempting to use default unitsFile: " + unitFileLocation + " exists: "
						+ (unitFileLocation != null && new File(unitFileLocation).exists()));
			}
		}

		// Preset regions precedence (-p)
		if (opts.has("p")) {
			presetRegionListLocation = (String) opts.valueOf("p");
			System.out.println("InterfaceMain: presetRegionListLocation: " + presetRegionListLocation + " exists: "
					+ new File(presetRegionListLocation).exists());
			bootProps.setProperty("presetRegionList", presetRegionListLocation);
		} else {
			String propPreset = bootProps.getProperty("presetRegionList", null);
			if (propPreset != null) {
				presetRegionListLocation = propPreset;
				System.out.println("InterfaceMain: presetRegionListLocation (from properties): "
						+ presetRegionListLocation + " exists: " + new File(presetRegionListLocation).exists());
			} else {
				// also look in the current config directory
				File f_preset = new File("config" + File.separator + "preset_region_list.txt");
				if (f_preset.exists()) {
					presetRegionListLocation = f_preset.getAbsolutePath();
				}
				System.out.println(
						"    --> attempting to use default presetRegionListLocation: " + presetRegionListLocation
								+ " exists: " + (presetRegionListLocation != null && new File(presetRegionListLocation).exists()));
			}
		}

		// Favorite queries precedence (-f)
		if (opts.has("f")) {
			favoriteQueriesFileLocation = (String) opts.valueOf("f");
			System.out.println("InterfaceMain: favoriteQueriesFileLocation: " + favoriteQueriesFileLocation
					+ " exists: " + new File(favoriteQueriesFileLocation).exists());
			bootProps.setProperty("favoriteQueriesFile", favoriteQueriesFileLocation);
		} else {
			String propFav = bootProps.getProperty("favoriteQueriesFile", null);
			if (propFav != null) {
				favoriteQueriesFileLocation = propFav;
				System.out.println("InterfaceMain: favoriteQueriesFileLocation (from properties): "
						+ favoriteQueriesFileLocation + " exists: "
						+ new File(favoriteQueriesFileLocation).exists());
			} else {
				File f_favorite = new File("config" + File.separator + "favorite_queries_list.txt");
				if (f_favorite.exists()) {
					favoriteQueriesFileLocation = f_favorite.getAbsolutePath();
				}
				System.out.println("    --> attempting to use default favoriteQueriesFileLocation: "
						+ favoriteQueriesFileLocation + " exists: "
						+ (favoriteQueriesFileLocation != null && new File(favoriteQueriesFileLocation).exists()));
			}
		}

		// auto-generate-graphics precedence
		if (opts.has("auto-generate-graphics")) {
			autoGenerateGraphics = true;
			System.out.println("InterfaceMain: auto-generate-graphics is set to true from command line");
			bootProps.setProperty("autoGenerateGraphics", "true");
		} else {
			String propAutoGraphics = bootProps.getProperty("autoGenerateGraphics", "false");
			if (propAutoGraphics.equalsIgnoreCase("true")) {
				autoGenerateGraphics = true;
				System.out.println("InterfaceMain: auto-generate-graphics is set to true from properties");
			} else {
				autoGenerateGraphics = false;
			}
		}

		// compress_tree precedence (no CLI flag; properties file only)
		{
			String propCompressTree = bootProps.getProperty("compress_tree", "true").trim();
			compressTree = !"false".equalsIgnoreCase(propCompressTree);
			System.out.println("InterfaceMain: compress_tree = " + compressTree);
		}

		// Mapping folder precedence (-m)
		if (opts.has("m")) {
			shapeFileLocationPrefix = (String) opts.valueOf("m");
			System.out.println("InterfaceMain: shapeFileLocationPrefix: " + shapeFileLocationPrefix + " exists: "
					+ new File(shapeFileLocationPrefix).exists());
			enableMapping = true;
			bootProps.setProperty("mapResourceFolder", shapeFileLocationPrefix);
		} else {
			String propMap = bootProps.getProperty("mapResourceFolder", null);
			if (propMap != null) {
				shapeFileLocationPrefix = propMap;
				enableMapping = true;
				System.out.println("InterfaceMain: shapeFileLocationPrefix (from properties): "
						+ shapeFileLocationPrefix + " exists: " + new File(shapeFileLocationPrefix).exists());
			} else {
				// now check the path
				File loc = new File("map_resources");
				if (loc.exists() && loc.isDirectory()) {
					System.out.println("Found absolute map path at " + loc.getAbsolutePath());
					shapeFileLocationPrefix = loc.getAbsolutePath();
					enableMapping = true;
				} else {
					System.out.println("Could not find any maps, disabling mapping option");
					enableMapping = false;
				}
				System.out.println(
						"    --> attempting to use default shapeFileLocationPrefix: " + shapeFileLocationPrefix
								+ " exists: " + (shapeFileLocationPrefix != null && new File(shapeFileLocationPrefix).exists()));
			}
		}

		// Derive the actual shapefile paths now (so maps don't require the menu action).
		initializeMappingFromFolder(shapeFileLocationPrefix);

		// Legend bundle precedence (legend_bundle)
		if (opts.has("legend_bundle")) {
			legendBundlesLoc = (String) opts.valueOf("legend_bundle");
			System.out.println("InterfaceMain: legendBundlesLoc: " + legendBundlesLoc + " exists: "
					+ new File(legendBundlesLoc).exists());
			bootProps.setProperty("legend_bundle", legendBundlesLoc);
		}
		File legendBundleFile = null;
		if (legendBundlesLoc == null) {
			String propLegend = bootProps.getProperty("legend_bundle", null);
			if (propLegend != null) {
				legendBundlesLoc = propLegend;
			}
		}
		if (legendBundlesLoc != null) {
			legendBundleFile = new File(legendBundlesLoc);
		}
		if (legendBundlesLoc == null || !legendBundleFile.exists()) {
			legendBundlesLoc = "config/LegendBundle.properties";
			legendBundleFile = new File(legendBundlesLoc);
			if (!legendBundleFile.exists()) {
				legendBundlesLoc = "LegendBundle.properties";
			}
		}

		// Persist any updated properties from CLI back to the properties file
		try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
			bootProps.storeToXML(fos, "ModelInterface properties (boot updated)");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		logStartup("boot properties resolved");

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			applyConfiguredUIFontDefaults();
		} catch (Exception e) {
			// warn the user... should be ok to keep going
			System.out.println("Error setting look and feel: " + e);
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final long guiStart = System.nanoTime();
				createGUI();
				logStartupTiming("createGUI finished in " + elapsedMillis(guiStart) + " ms");
				if (path != null) {
					File dbFile = new File(path);
					if (!dbFile.exists()) {
						int response = main.showOptionDialog(
								"The database '" + path + "' does not exist. \nWould you like to create it?",
								"Create Database?",
								new Object[] { "Create", "Cancel" },
								JOptionPane.QUESTION_MESSAGE,
								"Create",
								JOptionPane.CANCEL_OPTION);
						if (response != JOptionPane.OK_OPTION) {
							main.completeStartupStep(STARTUP_MESSAGE_READY);
							showGUI();
							return;
						}
						// If create is chosen, doOpenDB will create it.
					}
					main.updateStartupLoadingMessage(STARTUP_MESSAGE_OPENING_DB);
					DbViewer db = (DbViewer) main.dbView;
					final long dbOpenStart = System.nanoTime();
					try {
						db.doOpenDB(dbFile, !dbFile.exists());
						logStartupTiming("DbViewer.doOpenDB finished in " + elapsedMillis(dbOpenStart) + " ms");
					} catch (Exception e) {
						logStartupTiming("DbViewer.doOpenDB failed after " + elapsedMillis(dbOpenStart) + " ms");
						if (ModelInterface.ModelGUI2.xmldb.XMLDB.isSuppressedBaseXResourceException(e)) {
							System.err.println("Suppressing BaseX packaged-resource stack trace during initial DB open: " + e.getMessage());
						} else {
						 e.printStackTrace();
						}
					}
					// DbViewer continues startup asynchronously via SwingWorker; keep the
					// loading view in its current stage until DbViewer reports completion or failure.
					File f = new File(path);
					File[] files = new File[1];
					files[0] = f;
					RecentFilesList.getInstance().addFile(files, "ModelInterface.ModelGUI2.DbViewer", "Open DB");

				}
				else {
					String[] options = { "Choose Database", "Open without Database", "Quit" };
					int response = JOptionPane.showOptionDialog(main.mainFrame,
							"No database specified. What would you like to do?", "Database not specified",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					switch (response) {
					case 0:
						((ActionListener)main.dbView).actionPerformed(new ActionEvent(main.mainFrame, ActionEvent.ACTION_PERFORMED, "Open DB"));
						break;
					case 1:
						break;
					case 2:
						System.exit(0);
						break;
					default:
						System.exit(0);
						break;
					}
					main.completeStartupStep(STARTUP_MESSAGE_READY);
				}
				showGUI();
				logStartup("main window shown");
			}
		});

		// This bit of code makes JAI happy as it must have a vendor argument.
		try {
			InterfaceMain.afVenderNames(PackageUtil.class, "GLIMPSE", "GLIMPSE", "GLIMPSE");
		} catch (NoSuchFieldException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
		 e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
		 e1.printStackTrace();
		} catch (IllegalAccessException e1) {
		 e1.printStackTrace();
		}

	}

	// the static setup of JAI vendor names
	public static void afVenderNames(Class<?> PackageUtil, String vendor, String version, String specTitle)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field vendorField = PackageUtil.getDeclaredField("vendor");
		vendorField.setAccessible(true);
		vendorField.set(null, vendor);

		Field versionField = PackageUtil.getDeclaredField("version");
		versionField.setAccessible(true);
		versionField.set(null, version);

		Field specTitleField = PackageUtil.getDeclaredField("specTitle");
		specTitleField.setAccessible(true);
		specTitleField.set(null, specTitle);
	}

	/**
	 * Create a new instance of this class and makes it visible
	 */
	private static void createGUI() {
		final long createGuiStart = System.nanoTime();
		main = null;
		main = new InterfaceMain();
		Properties startupProps = main.getProperties();
		main.resetStartupProgress(path != null ? STARTUP_MESSAGE_WITH_DB : STARTUP_MESSAGE_WITHOUT_DB);
		main.mainFrame = new JFrame("Model Interface");
		String image_str = Paths.get(".", "config", "results.png").toString();
		main.mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(image_str));
		main.mainFrame.getContentPane().setBackground(UNIFIED_BG);
		// Do not override default fonts; let the platform Look & Feel decide.
		main.mainFrame.getRootPane().setBorder(javax.swing.BorderFactory.createLineBorder(UNIFIED_BORDER, 1));
		if (Boolean.parseBoolean(startupProps.getProperty("isMaximized", "false"))) {
			main.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		String lastHeight = startupProps.getProperty("lastHeight", "600");
		String lastWidth = startupProps.getProperty("lastWidth", "800");

		System.out.println("Using size " + lastWidth + " width x " + lastHeight + " height");

		String enableMapping = startupProps.getProperty("enableMapping", "true");
		if (enableMapping != null) {
			try {
				boolean enableMaps = Boolean.parseBoolean(enableMapping);
				InterfaceMain.enableMapping = enableMaps;
			} catch (Exception e) {
				System.out.println("Couldn't parse enableMaps: " + enableMapping);
			}
		}
		String enableSankey = startupProps.getProperty("enableSankey", "true");
		if (enableSankey != null) {
			try {
				boolean enableSankeys = Boolean.parseBoolean(enableSankey);
				InterfaceMain.enableSankey = enableSankeys;
			} catch (Exception e) {
				System.out.println("Couldn't parse enableSankey: " + enableMapping);
			}
		}
		main.mainFrame.setSize(Integer.parseInt(lastWidth), Integer.parseInt(lastHeight));
		main.mainFrame.setLayout(new BorderLayout());
		main.mainFrame.setLocationRelativeTo(null);
		main.mainFrame.setVisible(true);
		main.showStartupLoadingView(path != null ? STARTUP_MESSAGE_WITH_DB : STARTUP_MESSAGE_WITHOUT_DB);

		main.initialize();
		main.completeStartupStep(STARTUP_MESSAGE_INITIALIZING);
		logStartupTiming("initialize() finished in " + elapsedMillis(createGuiStart) + " ms");
		main.initStatusBar();
		main.completeStartupStep(STARTUP_MESSAGE_STATUS_BAR);
		main.showStartupLoadingView(path != null ? STARTUP_MESSAGE_WITH_DB : STARTUP_MESSAGE_WITHOUT_DB);
		cancelPendingStartupDbViewerTimer();
		if (path != null) {
			final javax.swing.Timer startupDelayTimer = new javax.swing.Timer(STARTUP_MESSAGE_LINGER_MS,
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							pendingStartupDbViewerTimer = null;
							main.completeStartupStep(STARTUP_MESSAGE_DB_VIEW);
							main.fireControlChange("DbViewer");
						}
					});
			startupDelayTimer.setRepeats(false);
			pendingStartupDbViewerTimer = startupDelayTimer;
			startupDelayTimer.start();
		} else {
			main.completeStartupStep(STARTUP_MESSAGE_DB_PROMPT);
		}
		logStartupTiming("createGUI total " + elapsedMillis(createGuiStart) + " ms");
	}

	private static void cancelPendingStartupDbViewerTimer() {
		if (pendingStartupDbViewerTimer != null) {
			pendingStartupDbViewerTimer.stop();
			pendingStartupDbViewerTimer = null;
		}
	}

	private void initStatusBar() {
		if (statusBar != null) {
			return;
		}

		// Ensure a stable root container is installed exactly once.
		if (rootContent == null) {
			rootContent = new JPanel(new BorderLayout());
		}
		if (mainFrame.getContentPane() != rootContent) {
			// Preserve existing CENTER if someone set a different content pane already.
			java.awt.Container old = mainFrame.getContentPane();
			java.awt.Component oldCenter = null;
			if (old != null && old.getLayout() instanceof BorderLayout) {
				oldCenter = ((BorderLayout) old.getLayout()).getLayoutComponent(BorderLayout.CENTER);
			}
			mainFrame.setContentPane(rootContent);
			if (oldCenter != null) {
				rootContent.add(oldCenter, BorderLayout.CENTER);
			}
		}

		statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UNIFIED_BORDER));
		statusBar.setBackground(UNIFIED_BG);

		activeDbStatusLabel = new JLabel();
		activeDbStatusLabel.setOpaque(false);
		activeDbStatusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		statusBar.add(activeDbStatusLabel, BorderLayout.WEST);

		// Right side: query progress (hidden unless multiple queries are running)
		JPanel rightPanel = new JPanel();
		rightPanel.setOpaque(false);
		rightPanel.setLayout(new javax.swing.BoxLayout(rightPanel, javax.swing.BoxLayout.X_AXIS));
		rightPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

		queryProgressLabel = new JLabel();
		queryProgressLabel.setOpaque(false);
		queryProgressLabel.setVisible(false);

		queryProgressBar = new javax.swing.JProgressBar(0, 100);
		queryProgressBar.setStringPainted(false);
		queryProgressBar.setVisible(false);
		queryProgressBar.setBorderPainted(true);
		queryProgressBar.setPreferredSize(new java.awt.Dimension(140, 14));
		queryProgressBar.setMaximumSize(new java.awt.Dimension(180, 14));

		rightPanel.add(javax.swing.Box.createHorizontalGlue());
		rightPanel.add(queryProgressLabel);
		rightPanel.add(javax.swing.Box.createHorizontalStrut(8));
		rightPanel.add(queryProgressBar);
		statusBar.add(rightPanel, BorderLayout.EAST);

		// Mount into the stable root container.
		rootContent.add(statusBar, BorderLayout.SOUTH);
		updateActiveDatabaseStatus(path);
		resetQueryProgressUI();

		// Listen for any code that replaces the frame content pane and restore our
		// stable rootContent with the status bar. This prevents transient UI code
		// from accidentally removing the status bar when running queries or
		// switching views.
		mainFrame.addPropertyChangeListener("contentPane", new java.beans.PropertyChangeListener() {
			@Override
			public void propertyChange(java.beans.PropertyChangeEvent evt) {
				if (suppressContentPaneListener) return;
				Runnable restoreTask = new Runnable() {
					@Override
					public void run() {
						try {
							suppressContentPaneListener = true;
							java.awt.Container newPane = mainFrame.getContentPane();
							// If someone replaced the content pane, take that new pane and mount
							// it into our rootContent center so the status bar remains visible.
							if (newPane != rootContent) {
								// Detach the newPane from the frame and install under rootContent.
								mainFrame.setContentPane(rootContent);
								setMainView(newPane);
							}
						} finally {
							suppressContentPaneListener = false;
						}
					}
				};
				if (javax.swing.SwingUtilities.isEventDispatchThread()) {
					restoreTask.run();
				} else {
					try {
						javax.swing.SwingUtilities.invokeAndWait(restoreTask);
					} catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
						// Restore interrupted status and log; avoid silently swallowing failures.
						Thread.currentThread().interrupt();
						System.err.println("Failed to restore content pane with status bar: " + e);
					}
				}
			}
		});

	}

	private JPanel createStartupLoadingView() {
		JPanel panel = new JPanel(new java.awt.GridBagLayout());
		panel.setOpaque(true);
		panel.setBackground(UNIFIED_BG);

		JPanel content = new JPanel();
		content.setOpaque(true);
		content.setBackground(UNIFIED_PANEL_BG);
		content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(new Color(220, 224, 234), 1, true),
						BorderFactory.createEmptyBorder(22, 28, 22, 28)),
				BorderFactory.createEmptyBorder(0, 0, 0, 0)));

		startupLoadingLabel = new JLabel("Loading...", SwingConstants.CENTER);
		startupLoadingLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
		startupLoadingLabel.setForeground(new Color(70, 76, 96));
		startupLoadingLabel.setFont(startupLoadingLabel.getFont().deriveFont(java.awt.Font.BOLD,
				(float) (getConfiguredFontSize() + 7)));
		startupLoadingLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		startupLoadingBar = new JProgressBar(0, STARTUP_PROGRESS_MAX);
		startupLoadingBar.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
		startupLoadingBar.setIndeterminate(false);
		startupLoadingBar.setValue(0);
		startupLoadingBar.setStringPainted(true);
		startupLoadingBar.setString("0%");
		startupLoadingBar.setBorderPainted(false);
		startupLoadingBar.setBackground(new Color(236, 239, 247));
		startupLoadingBar.setForeground(new Color(116, 138, 196));
		startupLoadingBar.setPreferredSize(new java.awt.Dimension(240, 24));
		startupLoadingBar.setMaximumSize(new java.awt.Dimension(240, 24));

		content.add(startupLoadingLabel);
		content.add(startupLoadingBar);
		applyCurrentLoadingProgressState();
		panel.add(content);
		return panel;
	}

	private JPanel getStartupLoadingView() {
		if (startupLoadingView == null) {
			startupLoadingView = createStartupLoadingView();
		}
		return startupLoadingView;
	}

	private void showShutdownLoadingView(final String message) {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				ensureStatusBarInstalled();
				loadingViewMode = LoadingViewMode.SHUTDOWN;
				getStartupLoadingView();
				if (message != null && !message.trim().isEmpty()) {
					shutdownDefaultMessage = message;
					startupLoadingLabel.setText(message);
				}
				applyCurrentLoadingProgressState();
				setMainView(getStartupLoadingView());
				if (mainFrame != null) {
					mainFrame.getGlassPane().setVisible(false);
					if (mainFrame.getJMenuBar() != null) {
						mainFrame.getJMenuBar().setVisible(false);
					}
					if (statusBar != null) {
						statusBar.setVisible(false);
					}
					mainFrame.setVisible(true);
					rootContent.revalidate();
					rootContent.repaint();
					java.awt.Component center = ((BorderLayout) rootContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
					if (center != null) {
						center.repaint();
						if (center instanceof javax.swing.JComponent) {
							((javax.swing.JComponent) center).paintImmediately(((javax.swing.JComponent) center).getBounds());
						}
					}
					rootContent.paintImmediately(rootContent.getBounds());
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				javax.swing.SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (java.lang.reflect.InvocationTargetException e) {
				System.err.println("Failed to show shutdown loading view: " + e);
			}
		}
	}

	private void allowShutdownProgressPaint() {
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			return;
		}
		try {
			Thread.sleep(SHUTDOWN_STEP_LINGER_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void updateStartupLoadingMessage(final String message) {
		if (message == null || message.trim().isEmpty()) {
			return;
		}
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				getStartupLoadingView();
				startupDefaultMessage = message;
				startupLoadingLabel.setText(message);
				if (mainFrame != null && mainFrame.isVisible()) {
					startupLoadingLabel.repaint();
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	public void showStartupLoadingView(final String message) {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				ensureStatusBarInstalled();
				loadingViewMode = LoadingViewMode.STARTUP;
				getStartupLoadingView();
				if (message != null && !message.trim().isEmpty()) {
					startupDefaultMessage = message;
					startupLoadingLabel.setText(message);
				}
				applyCurrentLoadingProgressState();
				setMainView(getStartupLoadingView());
				if (mainFrame != null) {
					mainFrame.setLocationRelativeTo(null);
					mainFrame.setVisible(true);
					mainFrame.getGlassPane().setVisible(false);
					rootContent.paintImmediately(rootContent.getBounds());
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	public void hideStartupLoadingView() {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				startupStepsCompleted = 0;
				startupTotalSteps = STARTUP_TOTAL_STEPS;
				startupDefaultMessage = STARTUP_MESSAGE_WITH_DB;
				shutdownStepsCompleted = 0;
				shutdownTotalSteps = SHUTDOWN_TOTAL_STEPS;
				shutdownDefaultMessage = SHUTDOWN_MESSAGE_STARTING;
				loadingViewMode = LoadingViewMode.STARTUP;
				if (startupLoadingLabel != null) {
					startupLoadingLabel.setText(startupDefaultMessage);
				}
				if (startupLoadingBar != null) {
					startupLoadingBar.setIndeterminate(false);
					startupLoadingBar.setValue(0);
					startupLoadingBar.setString("0%");
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	private void resetStartupProgress(final String initialMessage) {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				startupTotalSteps = STARTUP_TOTAL_STEPS;
				startupStepsCompleted = 0;
				startupDefaultMessage = (initialMessage == null || initialMessage.trim().isEmpty()) ? STARTUP_MESSAGE_WITH_DB : initialMessage;
				if (startupLoadingLabel != null) {
					startupLoadingLabel.setText(startupDefaultMessage);
				}
				applyStartupProgressState();
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	private void completeStartupStep(final String message) {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				startupStepsCompleted = Math.min(startupStepsCompleted + 1, Math.max(1, startupTotalSteps));
				if (startupLoadingLabel != null) {
					if (message != null && !message.trim().isEmpty()) {
						startupLoadingLabel.setText(message);
					} else {
						startupLoadingLabel.setText(startupDefaultMessage);
					}
				}
				applyStartupProgressState();
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	private void applyStartupProgressState() {
		int totalSteps = Math.max(1, startupTotalSteps);
		int completedSteps = Math.max(0, Math.min(startupStepsCompleted, totalSteps));
		int progressValue = (int)Math.round((completedSteps * (double)STARTUP_PROGRESS_MAX) / totalSteps);
		if (startupLoadingBar != null) {
			startupLoadingBar.setIndeterminate(false);
			startupLoadingBar.setMinimum(0);
			startupLoadingBar.setMaximum(STARTUP_PROGRESS_MAX);
			startupLoadingBar.setValue(progressValue);
			startupLoadingBar.setString(progressValue + "%");
		}
		if (startupLoadingLabel != null && (startupLoadingLabel.getText() == null || startupLoadingLabel.getText().trim().isEmpty())) {
			startupLoadingLabel.setText(startupDefaultMessage);
		}
	}

	private void applyShutdownProgressState() {
		int totalSteps = Math.max(1, shutdownTotalSteps);
		int completedSteps = Math.max(0, Math.min(shutdownStepsCompleted, totalSteps));
		int progressValue = (int)Math.round((completedSteps * (double)STARTUP_PROGRESS_MAX) / totalSteps);
		if (startupLoadingBar != null) {
			startupLoadingBar.setIndeterminate(false);
			startupLoadingBar.setMinimum(0);
			startupLoadingBar.setMaximum(STARTUP_PROGRESS_MAX);
			startupLoadingBar.setValue(progressValue);
			startupLoadingBar.setString(progressValue + "%");
		}
		if (startupLoadingLabel != null) {
			startupLoadingLabel.setText((shutdownDefaultMessage == null || shutdownDefaultMessage.trim().isEmpty())
					? SHUTDOWN_MESSAGE_STARTING : shutdownDefaultMessage);
		}
	}

	private void applyCurrentLoadingProgressState() {
		if (loadingViewMode == LoadingViewMode.SHUTDOWN) {
			applyShutdownProgressState();
		} else {
			applyStartupProgressState();
		}
	}

	/**
	 * Ensure the status bar is present even if another view replaced the frame content pane.
	 */
	private void ensureStatusBarInstalled() {
		if (mainFrame == null) {
			return;
		}
		if (statusBar == null) {
			initStatusBar();
			return;
		}
		if (rootContent == null) {
			rootContent = new JPanel(new BorderLayout());
		}
		if (mainFrame.getContentPane() != rootContent) {
			mainFrame.setContentPane(rootContent);
		}
		if (statusBar.getParent() != rootContent) {
			rootContent.add(statusBar, BorderLayout.SOUTH);
		}
	}

	/**
	 * Replace the main view in the CENTER while keeping the global status bar visible.
	 */
	public void setMainView(java.awt.Component view) {
		if (DEBUG) System.out.println("InterfaceMain.setMainView: entered. view="
				+ (view == null ? "null" : view.getClass().getName())
				+ " thread=" + Thread.currentThread().getName());
		ensureStatusBarInstalled();
		if (DEBUG) System.out.println("InterfaceMain.setMainView: ensureStatusBarInstalled returned.");
		java.awt.Component existing = ((BorderLayout) rootContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		if (DEBUG) System.out.println("InterfaceMain.setMainView: existing center="
				+ (existing == null ? "null" : existing.getClass().getName()));
		if (existing != null) {
			rootContent.remove(existing);
			if (DEBUG) System.out.println("InterfaceMain.setMainView: removed existing center.");
		}
		if (view != null) {
			rootContent.add(view, BorderLayout.CENTER);
			if (DEBUG) System.out.println("InterfaceMain.setMainView: added new center component.");
		}
		if (DEBUG) System.out.println("InterfaceMain.setMainView: calling rootContent.revalidate()...");
		rootContent.revalidate();
		if (DEBUG) System.out.println("InterfaceMain.setMainView: rootContent.revalidate() returned.");
		if (DEBUG) System.out.println("InterfaceMain.setMainView: calling rootContent.repaint()...");
		rootContent.repaint();
		if (DEBUG) System.out.println("InterfaceMain.setMainView: rootContent.repaint() returned.");
	}

	private void resetQueryProgressUI() {
		if (queryProgressBar == null || queryProgressLabel == null) {
			return;
		}
		if (statusBarProgressMode == StatusBarProgressMode.SHUTDOWN) {
			return;
		}
		statusBarProgressMode = StatusBarProgressMode.NONE;
		queryProgressBar.setValue(0);
		queryProgressBar.setVisible(false);
		queryProgressLabel.setText("");
		queryProgressLabel.setVisible(false);
	}

	private void resetShutdownProgressUI() {
		if (queryProgressBar == null || queryProgressLabel == null) {
			return;
		}
		if (statusBarProgressMode != StatusBarProgressMode.SHUTDOWN) {
			return;
		}
		statusBarProgressMode = StatusBarProgressMode.NONE;
		queryProgressBar.setValue(0);
		queryProgressBar.setVisible(false);
		queryProgressLabel.setText("");
		queryProgressLabel.setVisible(false);
	}

	private void updateShutdownProgressStatus(final int completed, final int total, final String message) {
		ensureStatusBarInstalled();
		if (queryProgressBar == null || queryProgressLabel == null) {
			return;
		}
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				statusBarProgressMode = StatusBarProgressMode.SHUTDOWN;
				shutdownTotalSteps = Math.max(1, total);
				shutdownStepsCompleted = Math.max(0, Math.min(completed, shutdownTotalSteps));
				int percent = (int) Math.round((shutdownStepsCompleted * 100.0) / shutdownTotalSteps);
				String displayMessage = (message == null || message.trim().isEmpty()) ? SHUTDOWN_MESSAGE_STARTING : message;
				shutdownDefaultMessage = displayMessage;
				queryProgressBar.setVisible(true);
				queryProgressLabel.setVisible(true);
				queryProgressBar.setMinimum(0);
				queryProgressBar.setMaximum(100);
				queryProgressBar.setValue(percent);
				queryProgressLabel.setText(displayMessage);
				if (loadingViewMode == LoadingViewMode.SHUTDOWN) {
					applyShutdownProgressState();
				}
				if (mainFrame != null) {
					if (mainFrame.getJMenuBar() != null) {
						mainFrame.getJMenuBar().repaint();
					}
					java.awt.Component center = rootContent == null ? null
							: ((BorderLayout) rootContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
					if (center != null) {
						center.revalidate();
						center.repaint();
						if (center instanceof javax.swing.JComponent) {
							((javax.swing.JComponent) center).paintImmediately(((javax.swing.JComponent) center).getBounds());
						}
					}
					rootContent.paintImmediately(rootContent.getBounds());
				}
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				javax.swing.SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (java.lang.reflect.InvocationTargetException e) {
				System.err.println("Failed to update shutdown progress status: " + e);
				return;
			}
			allowShutdownProgressPaint();
		}
	}

	public void updateQueryProgressStatus(final int completed, final int total) {
		ensureStatusBarInstalled();
		if (queryProgressBar == null || queryProgressLabel == null) {
			return;
		}
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				if (statusBarProgressMode == StatusBarProgressMode.SHUTDOWN) {
					return;
				}
				if (total <= 1) {
					resetQueryProgressUI();
					return;
				}
				statusBarProgressMode = StatusBarProgressMode.QUERY;
				queryProgressBar.setVisible(true);
				queryProgressLabel.setVisible(true);
				int safeCompleted = Math.max(0, Math.min(completed, total));
				int percent = (int) Math.round((safeCompleted * 100.0) / total);
				queryProgressBar.setValue(percent);
				queryProgressLabel.setText("Queries: " + safeCompleted + "/" + total);
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			javax.swing.SwingUtilities.invokeLater(r);
		}
	}

	/** Hide and reset query progress in the status bar. */
	public void clearQueryProgressStatus() {
		ensureStatusBarInstalled();
		if (queryProgressBar == null || queryProgressLabel == null) {
			return;
		}
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			resetQueryProgressUI();
		} else {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					resetQueryProgressUI();
				}
			});
		}
	}

	/**
	 * Update the status bar text showing the active database.
	 * @param dbPath database path or null for none
	 */
	public void updateActiveDatabaseStatus(final String dbPath) {
		ensureStatusBarInstalled();
		final String normalizedPath = dbPath == null ? null : dbPath.trim();
		final String text = (normalizedPath == null || normalizedPath.isEmpty())
				? "Database: (none)"
				: "Database: " + normalizedPath;
		final int requestVersion = activeDbStatusVersion.incrementAndGet();
		setActiveDatabaseStatusText(text);
		if (normalizedPath == null || normalizedPath.isEmpty()) {
			return;
		}
		final File dbDirectory = new File(normalizedPath);
		if (!dbDirectory.exists() || !dbDirectory.isDirectory()) {
			return;
		}
		Thread sizeLookupThread = new Thread(new Runnable() {
			@Override
			public void run() {
				String sizeSuffix = buildDatabaseSizeSuffix(dbDirectory);
				if (sizeSuffix == null || requestVersion != activeDbStatusVersion.get()) {
					return;
				}
				setActiveDatabaseStatusText(text + sizeSuffix);
			}
		}, "ModelInterface-DbStatusSize");
		sizeLookupThread.setDaemon(true);
		sizeLookupThread.start();
	}

	public void refreshActiveDatabaseStatus() {
		updateActiveDatabaseStatus(path);
	}

	private void setActiveDatabaseStatusText(final String text) {
		if (activeDbStatusLabel == null) {
			return;
		}
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			activeDbStatusLabel.setText(text);
				} else {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					activeDbStatusLabel.setText(text);
				}
			});
		}
	}

	private static String buildDatabaseSizeSuffix(File dbDirectory) {
		long sizeBytes = calculateSizeBytes(dbDirectory);
		if (sizeBytes < 0L) {
			return null;
		}
		return " | Size: " + formatDatabaseSize(sizeBytes);
	}

	private static String formatDatabaseSize(long sizeBytes) {
		if (sizeBytes <= 0L) {
			return "0 bytes";
		}
		// ≥ 1 TB → TB
		if (sizeBytes >= DB_SIZE_TB) {
			double tb = sizeBytes / (double) DB_SIZE_TB;
			return String.format(tb < 10d ? "%,.1f" : "%,.0f", tb) + " TB";
		}
		// ≥ 0.1 GB (GB_THRESHOLD) → GB  — keeps "0.4 GB" instead of "432 MB"
		if (sizeBytes >= DB_SIZE_GB_THRESHOLD) {
			double gb = sizeBytes / (double) DB_SIZE_GB;
			return String.format(gb < 10d ? "%,.1f" : "%,.0f", gb) + " GB";
		}
		// ≥ 1 MB → MB
		if (sizeBytes >= DB_SIZE_MB) {
			double mb = sizeBytes / (double) DB_SIZE_MB;
			return String.format(mb < 10d ? "%,.1f" : "%,.0f", mb) + " MB";
		}
		// ≥ 1 KB → KB
		if (sizeBytes >= DB_SIZE_KB) {
			double kb = sizeBytes / (double) DB_SIZE_KB;
			return String.format(kb < 10d ? "%,.1f" : "%,.0f", kb) + " KB";
		}
		return sizeBytes + " bytes";
	}

	private static long calculateSizeBytes(File file) {
		if (file == null || !file.exists()) {
			return -1L;
		}
		if (file.isFile()) {
			return file.length();
		}
		long total = 0L;
		File[] children = file.listFiles();
		if (children == null) {
			return total;
		}
		for (File child : children) {
			long childSize = calculateSizeBytes(child);
			if (childSize > 0L) {
				total += childSize;
			}
		}
		return total;
	}

	private static void showGUI() {
		if (main != null && main.mainFrame != null) {
			main.mainFrame.setVisible(true);
			main.mainFrame.toFront();
		}
	}

	private InterfaceMain() {
		mainFrame = null;
		savedProperties = new Properties();
		synchronized (propertiesLock) {
			if (propertiesFile.exists()) {
				System.out.println("Props: " + propertiesFile.getAbsolutePath());

				try {
					savedProperties.loadFromXML(new FileInputStream(propertiesFile));
					String prettyPrintProperty = savedProperties.getProperty("pretty-print", null);
					if (System.getProperty("ModelInterface.pretty-print", null) == null && prettyPrintProperty != null) {
						System.getProperties().setProperty("ModelInterface.pretty-print", prettyPrintProperty);
					}
				} catch (FileNotFoundException notFound) {
					// well I checked if it existed before so..
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}

			// Ensure required properties exist
			if (!savedProperties.containsKey("allYearList")) {
				String allYears = "1990;2005;2010;2015;2020;2021;2025;2030;2035;2040;2045;2050;2055;2060;2065;2070;2075;2080;2085;2090;2095;2100";
				List<String> yearList = new ArrayList<>(Arrays.asList(allYears.split(";")));
				yearList.sort(Comparator.naturalOrder());
				savedProperties.setProperty("allYearList", String.join(";", yearList));
			}
			if (!savedProperties.containsKey("selectedYearList")) {
				String selectedYears = "2015;2020;2021;2025;2030;2035;2040;2045;2050;2055;2060;2065;2070;2075;2080;2085;2090;2095;2100";
				List<String> yearList = new ArrayList<>(Arrays.asList(selectedYears.split(";")));
				yearList.sort(Comparator.naturalOrder());
				savedProperties.setProperty("selectedYearList", String.join(";", yearList));
			}
			if (!savedProperties.containsKey("lastWidth")) {
				savedProperties.setProperty("lastWidth", "1600");
			}
			if (!savedProperties.containsKey("lastHeight")) {
				savedProperties.setProperty("lastHeight", "900");
			}
			if (!savedProperties.containsKey("scenarioRegionsSplit")) {
				savedProperties.setProperty("scenarioRegionsSplit", "275");
			}
			if (!savedProperties.containsKey("remove1975")) {
				savedProperties.setProperty("remove1975", "true");
			}
			if (!savedProperties.containsKey("tableCreatorSplit")) {
				savedProperties.setProperty("tableCreatorSplit", "500");
			}
			if (!savedProperties.containsKey("queriesSplit")) {
				savedProperties.setProperty("queriesSplit", "700");
			}
			if (!savedProperties.containsKey("enableMapping")) {
				savedProperties.setProperty("enableMapping", "true");
			}
			if (!savedProperties.containsKey("favoriteQueriesFile")) {
				savedProperties.setProperty("favoriteQueriesFile", ".\\config\\favorite_queries_list.txt");
			}
			if (!savedProperties.containsKey("presetRegionsFile")) {
				savedProperties.setProperty("presetRegionsFile", ".\\config\\preset_region_list.txt");
			}
			if (!savedProperties.containsKey("unitsFile")) {
				savedProperties.setProperty("unitsFile", ".\\config\\units_rules.csv");
			}
			if (!savedProperties.containsKey("mapResourceFolder")) {
				savedProperties.setProperty("mapResourceFolder", ".\\map_resources");
			}
			if (!savedProperties.containsKey("RecentFilesLength")) {
				savedProperties.setProperty("RecentFilesLength", "5");
			}
			if (!savedProperties.containsKey("suppressStartupWarning")) {
				savedProperties.setProperty("suppressStartupWarning", "false");
			}
			if (!savedProperties.containsKey("isMaximized")) {
				savedProperties.setProperty("isMaximized", "false");
			}
			if (!savedProperties.containsKey("zipExportedScenarios")) {
				savedProperties.setProperty("zipExportedScenarios", "false");
			}
		if (!savedProperties.containsKey("copyIncludeQueryName")) {
			savedProperties.setProperty("copyIncludeQueryName", "false");
		}
		if (!savedProperties.containsKey("presetRegionList")) {
			savedProperties.setProperty("presetRegionList", ".\\config\\preset_region_list.txt");
		}
		if (!savedProperties.containsKey("shapeFileLocationPrefix")) {
			savedProperties.setProperty("shapeFileLocationPrefix", ".\\map_resources");
		}
		if (!savedProperties.containsKey("legendBundlesLoc")) {
			savedProperties.setProperty("legendBundlesLoc", "config/LegendBundle.properties");
		}
		if (!savedProperties.containsKey("compress_tree")) {
			savedProperties.setProperty("compress_tree", "true");
		}
		if (!savedProperties.containsKey("limitSigDigits")) {
			savedProperties.setProperty("limitSigDigits", "false");
		}
		if (!savedProperties.containsKey(FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(FONT_SIZE_PROPERTY, Integer.toString(configuredFontSize));
		}
		if (!savedProperties.containsKey(GRAPHICS_TITLE_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_TITLE_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_TITLE_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
					savedProperties.getProperty(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
							Integer.toString(DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE)));
		}
		if (!savedProperties.containsKey(GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
					savedProperties.getProperty(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
							Integer.toString(DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE)));
		}
		if (!savedProperties.containsKey(GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
					savedProperties.getProperty(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
							Integer.toString(DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE)));
		}
		if (!savedProperties.containsKey(GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
					savedProperties.getProperty(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
							Integer.toString(DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE)));
		}
		if (!savedProperties.containsKey(GRAPHICS_LEGEND_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_LEGEND_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_LEGEND_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_LINE_WIDTH_SCALE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_LINE_WIDTH_SCALE_PROPERTY,
					Double.toString(DEFAULT_GRAPHICS_LINE_WIDTH_SCALE));
		}
		if (!savedProperties.containsKey(GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY,
					Integer.toString(DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE));
		}
		if (!savedProperties.containsKey(GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY)) {
			savedProperties.setProperty(GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY,
					Double.toString(DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH));
		}
		configuredFontSize = resolveConfiguredFontSize(savedProperties);
		savedProperties.setProperty(FONT_SIZE_PROPERTY, Integer.toString(configuredFontSize));
		savedProperties.setProperty(GRAPHICS_TITLE_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_TITLE_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_TITLE_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
				Integer.toString(resolveGraphicsFontSize(savedProperties,
						GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
						GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
						DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
				Integer.toString(resolveGraphicsFontSize(savedProperties,
						GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
						GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
						DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
				Integer.toString(resolveGraphicsFontSize(savedProperties,
						GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
						GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
						DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
				Integer.toString(resolveGraphicsFontSize(savedProperties,
						GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
						GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
						DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_LEGEND_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_LEGEND_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_LEGEND_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_LINE_WIDTH_SCALE_PROPERTY,
				Double.toString(parseBoundedDoubleValue(savedProperties.getProperty(GRAPHICS_LINE_WIDTH_SCALE_PROPERTY),
						DEFAULT_GRAPHICS_LINE_WIDTH_SCALE, MIN_GRAPHICS_LINE_WIDTH_SCALE,
						MAX_GRAPHICS_LINE_WIDTH_SCALE)));
		savedProperties.setProperty(GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY,
				Integer.toString(parseBoundedIntValue(savedProperties.getProperty(GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY),
						DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE, MIN_GRAPHICS_FONT_SIZE, MAX_GRAPHICS_FONT_SIZE)));
		savedProperties.setProperty(GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY,
				Double.toString(parseBoundedDoubleValue(savedProperties.getProperty(GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY),
						DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH, MIN_GRAPHICS_LINE_WIDTH_SCALE,
						MAX_GRAPHICS_LINE_WIDTH_SCALE)));
			// Persist if any defaults were added
			persistProperties();

			oldControl = "ModelInterface";
			if (path != null)
				savedProperties.setProperty("paramPath", path);
			else
				savedProperties.remove("paramPath");
			// added by Dan to allow query file to be specified as runtime argument
			if (queryFilename != null)
				savedProperties.setProperty("queryFile", queryFilename);
			
			// Initialize DbViewer.disableSigDigits based on limitSigDigits property
			String propLimitSigDigits = savedProperties.getProperty("limitSigDigits", "false");
			boolean limitSigDigits = "true".equalsIgnoreCase(propLimitSigDigits);
			DbViewer.disableSigDigits = !limitSigDigits;  // Inverse: if limiting is enabled, disabling is off
		}

	}

	private void initialize() {
		MenuManager menuMan = new MenuManager(null);
		final long initStart = System.nanoTime();
		addWindowAdapters();
		logStartupTiming("initialize:addWindowAdapters " + elapsedMillis(initStart) + " ms");
		addMenuItems(menuMan);
		logStartupTiming("initialize:addMenuItems " + elapsedMillis(initStart) + " ms");
		addMenuAdderMenuItems(menuMan);
		logStartupTiming("initialize:addMenuAdderMenuItems " + elapsedMillis(initStart) + " ms");
		finalizeMenu(menuMan);
		logStartupTiming("initialize:finalizeMenu " + elapsedMillis(initStart) + " ms");
		// Do not force fonts for the menu bar/items; use platform Look & Feel defaults.
		// if path to DB was provided, dispatch to DBViewer to open database
//		  if (path != null) fireControlChange("DbViewer");		 
	}

	private JMenuItem makeMenuItem(String text) {
		JMenuItem item = new JMenuItem(text);
		item.setActionCommand(text);
		item.addActionListener(this);
		return item;
	}

	private void finalizeMenu(MenuManager menuMan) {
		JMenuBar mb = menuMan.createMenu();
		mainFrame.setJMenuBar(mb);
		refreshQueryFileMenuEnabled();
	}

	private void addWindowAdapters() {
		WindowAdapter myWindowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				shutdownAndExit();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				refreshQueryFileMenuEnabled();
			}
		};
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.addWindowListener(myWindowAdapter);
	}

	private String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private File promptForExecutable(String title) {
		FileChooser chooser = FileChooserFactory.getFileChooser();
		String lastDir = getProperties().getProperty("lastDirectory", ".");
		File start = new File(lastDir);
		File[] files = chooser.doFilePrompt(mainFrame, title, FileChooser.LOAD_DIALOG, start, null);
		if (files != null && files.length > 0 && files[0] != null) {
			File selected = files[0];
			if (selected.getParent() != null) {
				synchronized (propertiesLock) {
					savedProperties.setProperty("lastDirectory", selected.getParent());
					persistProperties();
				}
			}
			return selected;
		}
		return null;
	}

	private void shutdownAndExit() {
		if (!shuttingDown.compareAndSet(false, true)) {
			return;
		}
		cancelPendingStartupDbViewerTimer();
		try {
			fireControlChange("ModelInterfaceShutdown");
		} catch (Exception ex) {
			System.err.println("Error clearing active view during shutdown: " + ex);
		}
		shutdownStepsCompleted = 0;
		shutdownTotalSteps = SHUTDOWN_TOTAL_STEPS;
		shutdownDefaultMessage = SHUTDOWN_MESSAGE_STARTING;
		showShutdownLoadingView(SHUTDOWN_MESSAGE_STARTING);
		updateShutdownProgressStatus(0, SHUTDOWN_TOTAL_STEPS, SHUTDOWN_MESSAGE_STARTING);
		try {
			if (mainFrame != null) {
				updateShutdownProgressStatus(1, SHUTDOWN_TOTAL_STEPS, SHUTDOWN_MESSAGE_SAVING_WINDOW);
				synchronized (propertiesLock) {
					savedProperties.setProperty("isMaximized",
							Boolean.toString((mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH));
					if ((mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
						savedProperties.setProperty("lastWidth", Integer.toString(mainFrame.getWidth()));
						savedProperties.setProperty("lastHeight", Integer.toString(mainFrame.getHeight()));
					}
				}
			}
			updateShutdownProgressStatus(2, SHUTDOWN_TOTAL_STEPS, SHUTDOWN_MESSAGE_SAVING_SETTINGS);
			persistProperties();
		} finally {
			try {
				updateShutdownProgressStatus(3, SHUTDOWN_TOTAL_STEPS, SHUTDOWN_MESSAGE_CLOSING_DB);
				XMLDB.closeDatabase();
			} catch (Exception ex) {
				System.err.println("Error closing XML database during shutdown: " + ex);
			} finally {
				updateShutdownProgressStatus(4, SHUTDOWN_TOTAL_STEPS, SHUTDOWN_MESSAGE_EXITING);
				if (mainFrame != null) {
					mainFrame.dispose();
				}
				resetShutdownProgressUI();
				System.exit(0);
			}
		}
	}

	public JFrame getFrame() {
		return mainFrame;
	}

	// ---- PreferenceDialogCallbacks implementation ----

	@Override
	public JFrame getOwnerFrame() { return mainFrame; }

	@Override
	public void applyFontSize(int newSize) { applyConfiguredFontSizeNow(newSize); }

	@Override
	public int getCurrentFontSize() { return configuredFontSize; }

	@Override
	public void dispatchMenuAction(java.awt.event.ActionEvent e) { actionPerformed(e); }

	private void addMenuItems(MenuManager menuMan) {
		addFileMenu(menuMan);
		addEditMenu(menuMan);
		addViewMenu(menuMan);
		addToolsMenu(menuMan);
		addHelpMenu(menuMan);
		setupUndo(menuMan);
	}

	private void addFileMenu(MenuManager menuMan) {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		menuMan.addMenuItem(fileMenu, FILE_MENU_POS);
		MenuManager fileMM = menuMan.getSubMenuManager(FILE_MENU_POS);

		// Initialize actions (some are managed by other viewers)
		saveMenu = new JMenuItem("Save");
		saveMenu.setEnabled(false);
		saveAsMenu = new JMenuItem("Save As…");
		saveAsMenu.setEnabled(false);

		selectQueryMenu = makeMenuItem("Select Query File");
		fileMM.addSeparator(25);
		fileMM.addMenuItem(selectQueryMenu, 26);
		fileMM.addSeparator(27);

		quitMenu = makeMenuItem("Quit");
		quitMenu.setMnemonic(KeyEvent.VK_Q);
		fileMM.addMenuItem(quitMenu, FILE_QUIT_MENUITEM_POS);
	}

	private void addEditMenu(MenuManager menuMan) {
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		menuMan.addMenuItem(editMenu, EDIT_MENU_POS);
		MenuManager editMM = menuMan.getSubMenuManager(EDIT_MENU_POS);

		// 1) Query File
		editQueryFileMenu = makeMenuItem("Query File");
		editQueryFileMenu.setActionCommand("Edit Queries File");
		editMM.addMenuItem(editQueryFileMenu, 0);

		// 2) Separator
		editMM.addSeparator(1);

		// 3) Query Tree submenu (DbViewer will populate it)
		if (editMM.getSubMenuManager(EDIT_QUERY_SUBMENU_POS) == null) {
			editMM.addMenuItem(new JMenu("Query Tree"), EDIT_QUERY_SUBMENU_POS);
		}

		// 4) Favorites List submenu (DbViewer will populate it)
		if (editMM.getSubMenuManager(EDIT_FAVORITES_SUBMENU_POS) == null) {
			editMM.addMenuItem(new JMenu("Favorites List"), EDIT_FAVORITES_SUBMENU_POS);
		}

		//		// 5) Separator
		editMM.addSeparator(5);

		// 6) Preferences...
		JMenuItem setPreferences = new JMenuItem("Preferences...");
		setPreferences.setMnemonic(KeyEvent.VK_P);
		setPreferences.addActionListener(this);
		editMM.addMenuItem(setPreferences, 6);
	}

	private void addViewMenu(MenuManager menuMan) {
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		menuMan.addMenuItem(viewMenu, VIEW_MENU_POS);
	}

	private void addToolsMenu(MenuManager menuMan) {
		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.setMnemonic(KeyEvent.VK_T);
		menuMan.addMenuItem(toolsMenu, TOOLS_MENU_POS);
		MenuManager toolsMM = menuMan.getSubMenuManager(TOOLS_MENU_POS);
		if (toolsMM != null && toolsMM.getSubMenuManager(TOOLS_SUBMENU2_POS) == null) {
			toolsMM.addMenuItem(new JMenu("Open Files"), TOOLS_SUBMENU2_POS);
		}
		// CSV to XML widget — available at all times (no InputViewer required)
		if (toolsMM != null) {
			toolsMM.addSeparator(TOOLS_CSV_MENUITEM_POS - 1);
			JMenuItem csvToXmlItem = new JMenuItem("CSV to XML...");
			csvToXmlItem.setToolTipText("Convert a CSV file to GCAM XML format using a header file");
			csvToXmlItem.setActionCommand("CSV to XML Widget");
			csvToXmlItem.addActionListener(this);
			toolsMM.addMenuItem(csvToXmlItem, TOOLS_CSV_MENUITEM_POS);
		}
	}

	private void addHelpMenu(MenuManager menuMan) {
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		menuMan.addMenuItem(helpMenu, HELP_MENU_POS);
		MenuManager helpMM = menuMan.getSubMenuManager(HELP_MENU_POS);

		helpItem = new JMenuItem("Help");
		helpItem.setMnemonic(KeyEvent.VK_H);
		helpItem.addActionListener(this);
		helpMM.addMenuItem(helpItem, 0);
	}

    // second round YD edited the following lines to move "Undo" and "Redo" to be
    // under "Advanced" >> "Queries"

    private void setupUndo(MenuManager menuMan) {
        undoManager = new UndoManager();
        undoManager.setLimit(10);

        // Create the Undo/Redo menu items but DO NOT register them with the global
        // MenuManager here. DbViewer will place these items into the Edit -> Queries
        // submenu to avoid duplicates and control ordering.
        undoMenu = new JMenuItem("Undo");
        redoMenu = new JMenuItem("Redo");
        // Add standard accelerators - removed to disable shortcuts
        // undoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, getMenuShortcutMask()));
        if (isMac()) {
            // Removed accelerator for mac redo: redoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, getMenuShortcutMask() | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        } else {
            // Removed accelerator for non-mac redo: redoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, getMenuShortcutMask()));
        }

        undoMenu.setEnabled(false);
        redoMenu.setEnabled(false);

        ActionListener undoListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if (cmd.startsWith("Undo")) {
                    try {
                        undoManager.undo();
                        refreshUndoRedo();
                    } catch (CannotUndoException cue) {
                        cue.printStackTrace();
                    }
                } else if (cmd.startsWith("Redo")) {
                    try {
                        undoManager.redo();
                        refreshUndoRedo();
                    } catch (CannotRedoException cre) {
                        cre.printStackTrace();
                    }
                } else {
                    System.out.println("Didn't recognize: " + cmd);
                }
            }
        };

        undoMenu.addActionListener(undoListener);
        redoMenu.addActionListener(undoListener);
    }

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public void refreshUndoRedo() {
		undoMenu.setText(undoManager.getUndoPresentationName());
		undoMenu.setEnabled(undoManager.canUndo());
		redoMenu.setText(undoManager.getRedoPresentationName());
		redoMenu.setEnabled(undoManager.canRedo());
	}

	private void addMenuAdderMenuItems(MenuManager menuMan) {
		final long menuAdderStart = System.nanoTime();
		dbView = new DbViewer();
		logStartupTiming("addMenuAdder:new DbViewer " + elapsedMillis(menuAdderStart) + " ms");
		dbView.addMenuItems(menuMan);
		logStartupTiming("addMenuAdder:DbViewer.addMenuItems " + elapsedMillis(menuAdderStart) + " ms");
		// "XML file" (InputViewer) hidden — not working correctly
		// addLazyMenuItem(menuMan, TOOLS_MENU_POS, TOOLS_SUBMENU2_POS, new JMenuItem("XML file"), LAZY_OPEN_INPUT_VIEWER, 0);
		addLazyMenuItem(menuMan, TOOLS_MENU_POS, TOOLS_SUBMENU2_POS, new JMenuItem("Preprocessor file"), LAZY_OPEN_PP_VIEWER, 20);
		logStartupTiming("addMenuAdder:add lazy open-file items " + elapsedMillis(menuAdderStart) + " ms");
		final MenuAdder recentFilesList = RecentFilesList.getInstance();
		logStartupTiming("addMenuAdder:RecentFilesList.getInstance " + elapsedMillis(menuAdderStart) + " ms");
		recentFilesList.addMenuItems(menuMan);
		logStartupTiming("addMenuAdder:RecentFilesList.addMenuItems " + elapsedMillis(menuAdderStart) + " ms");
		final MenuAdder aboutDialog = new AboutDialog();
		logStartupTiming("addMenuAdder:new AboutDialog " + elapsedMillis(menuAdderStart) + " ms");
		aboutDialog.addMenuItems(menuMan);
		logStartupTiming("addMenuAdder:AboutDialog.addMenuItems " + elapsedMillis(menuAdderStart) + " ms");
		addLazyMenuItem(menuMan, TOOLS_MENU_POS, null, new JMenuItem("Configuration..."), LAZY_OPEN_CONFIGURATION_EDITOR, 19);
		logStartupTiming("addMenuAdder:add lazy configuration item " + elapsedMillis(menuAdderStart) + " ms");

		menuAdders = new ArrayList<MenuAdder>(6);
		menuAdders.add(dbView);
		// menuAdders.add(DMView);
		menuAdders.add(recentFilesList);
		menuAdders.add(aboutDialog);
		logStartupTiming("addMenuAdder:complete " + elapsedMillis(menuAdderStart) + " ms");
	}

	private void addLazyMenuItem(MenuManager menuMan, int topLevelPos, Integer subMenuPos, JMenuItem menuItem,
			String actionCommand, int itemPos) {
		menuItem.setActionCommand(actionCommand);
		menuItem.addActionListener(this);
		MenuManager target = menuMan.getSubMenuManager(topLevelPos);
		if (target == null) {
			return;
		}
		if (subMenuPos != null) {
			MenuManager nested = target.getSubMenuManager(subMenuPos);
			if (nested != null) {
				target = nested;
			}
		}
		target.addMenuItem(menuItem, itemPos);
	}

	private MenuAdder ensureInputView() {
		if (inputView == null) {
			final long start = System.nanoTime();
			inputView = new InputViewer();
			if (menuAdders != null) {
				menuAdders.add(inputView);
			}
			if (shouldOutputStartupTimings()) {
				System.out.println("[startup] lazy init InputViewer in " + elapsedMillis(start) + " ms");
			}
		}
		return inputView;
	}

	private MenuAdder ensurePPView() {
		if (ppView == null) {
			final long start = System.nanoTime();
			ppView = new PPViewer();
			if (menuAdders != null) {
				menuAdders.add(ppView);
			}
			if (shouldOutputStartupTimings()) {
				System.out.println("[startup] lazy init PPViewer in " + elapsedMillis(start) + " ms");
			}
		}
		return ppView;
	}

	private MenuAdder ensureConfigurationEditor() {
		if (confEditor == null) {
			final long start = System.nanoTime();
			confEditor = new ConfigurationEditor();
			if (menuAdders != null) {
				menuAdders.add(confEditor);
			}
			if (shouldOutputStartupTimings()) {
				System.out.println("[startup] lazy init ConfigurationEditor in " + elapsedMillis(start) + " ms");
			}
		}
		return confEditor;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null) return;
		// Minimal routing: keep existing behavior elsewhere in file; the menu items
		// created via makeMenuItem rely on this.
		switch (cmd) {
		case LAZY_OPEN_INPUT_VIEWER:
			((ActionListener) ensureInputView()).actionPerformed(
					new ActionEvent(e.getSource(), e.getID(), "XML file", e.getWhen(), e.getModifiers()));
			break;
		case LAZY_OPEN_PP_VIEWER:
			((ActionListener) ensurePPView()).actionPerformed(
					new ActionEvent(e.getSource(), e.getID(), "Preprocessor file", e.getWhen(), e.getModifiers()));
			break;
		case LAZY_OPEN_CONFIGURATION_EDITOR:
			ConfigurationEditor editor = (ConfigurationEditor) ensureConfigurationEditor();
			editor.setVisible(true);
			editor.toFront();
			break;
		case "Quit":
			shutdownAndExit();
			break;
		case "Select Query File":
			// Reuse existing property-driven selection behavior.
			fireProperty("SelectQuery", null, null);
			break;
		case "Help":
			try {
				Desktop.getDesktop().browse(new URI("https://github.com/DLoughlin/GLIMPSE-CE"));
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(mainFrame, "Unable to open help page. Please visit: https://github.com/DLoughlin/GLIMPSE-CE", "Help", JOptionPane.INFORMATION_MESSAGE);
			}
			break;
		case "Preferences...":
			showPreferencesDialog();
			break;
		case "Edit Queries File":
			openConfiguredQueryFileInXmlEditor();
			break;
		case "Select Units File": {
			FileChooser fc = FileChooserFactory.getFileChooser();
			File start = InterfaceMain.unitFileLocation != null ? new File(InterfaceMain.unitFileLocation)
					: new File(getProperties().getProperty("lastDirectory", "."));
			File[] files = fc.doFilePrompt(mainFrame, "Select Units File", FileChooser.LOAD_DIALOG, start,
					new CSVFilter());
			if (files != null && files.length > 0) {
				File file = files[0];
				String oldUnits = InterfaceMain.unitFileLocation;
				InterfaceMain.unitFileLocation = file.getAbsolutePath();
				synchronized (propertiesLock) {
					savedProperties.setProperty("unitsFile", InterfaceMain.unitFileLocation);
					savedProperties.setProperty("lastDirectory", file.getParent());
				}
				System.out.println("Selected units file: " + file.getAbsolutePath());
				ModelInterface.ModelGUI2.DbViewer.enableUnitConversions = true;
				persistProperties();
				fireProperty("UnitsFileChanged", oldUnits, InterfaceMain.unitFileLocation);
			}
			break;
		}
		case "Select Regions File": {
			FileChooser fc = FileChooserFactory.getFileChooser();
			File start = InterfaceMain.presetRegionListLocation != null
					? new File(InterfaceMain.presetRegionListLocation)
					: new File(getProperties().getProperty("lastDirectory", "."));
			File[] files = fc.doFilePrompt(mainFrame, "Select Regions File", FileChooser.LOAD_DIALOG, start, null);
			if (files != null && files.length > 0) {
				File file = files[0];
				InterfaceMain.presetRegionListLocation = file.getAbsolutePath();
				synchronized (propertiesLock) {
					savedProperties.setProperty("presetRegionList", InterfaceMain.presetRegionListLocation);
					savedProperties.setProperty("lastDirectory", file.getParent());
				}
				System.out.println("Selected regions file: " + file.getAbsolutePath());
				persistProperties();
			}
			break;
		}
		case "Select Map Resource Folder": {
			FileChooser fc = FileChooserFactory.getFileChooser();
			File start = InterfaceMain.shapeFileLocationPrefix != null
					? new File(InterfaceMain.shapeFileLocationPrefix)
					: new File(getProperties().getProperty("lastDirectory", "."));
			javax.swing.filechooser.FileFilter dirFilter = new javax.swing.filechooser.FileFilter() {
				@Override public boolean accept(File f) { return f.isDirectory(); }
				@Override public String getDescription() { return "Directory (select folder)"; }
			};
			File[] dirs = fc.doFilePrompt(mainFrame, "Select Map Resource Folder", FileChooser.LOAD_DIALOG, start,
					dirFilter);
			if (dirs != null && dirs.length > 0) {
				File dir = dirs[0];
				InterfaceMain.shapeFileLocationPrefix = dir.getAbsolutePath();
				InterfaceMain.enableMapping = true;
				System.out.println("Selected map resources folder: " + dir.getAbsolutePath());
				File preset_shapefile = new File(dir, "mapUS52Compact_from_rmap.shp");
				if (preset_shapefile.exists()) {
					stateShapeFileLocation = preset_shapefile.getAbsolutePath();
				} else {
					InterfaceMain.enableMapping = false;
				}
				File preset_reg32_shapefile = new File(dir, "mapGCAMReg32_from_rmap.shp");
				if (preset_reg32_shapefile.exists()) {
					gcamReg32ShapeFileLocation = preset_reg32_shapefile.getAbsolutePath();
					} else {
					InterfaceMain.enableMapping = false;
				}
				File preset_reg32US52_shapefile = new File(dir, "mapGCAMReg32US52_from_rmap.shp");
				if (preset_reg32US52_shapefile.exists()) {
					gcamReg32US52ShapeFileLocation = preset_reg32US52_shapefile.getAbsolutePath();
				}
				synchronized (propertiesLock) {
					savedProperties.setProperty("mapResourceFolder", InterfaceMain.shapeFileLocationPrefix);
					savedProperties.setProperty("lastDirectory", dir.getAbsolutePath());
					savedProperties.setProperty("enableMapping", Boolean.toString(InterfaceMain.enableMapping));
				}
				persistProperties();
			}
			break;
		}
		case "CSV to XML Widget":
			ModelInterface.ModelGUI2.CsvToXmlDialog.showDialog(mainFrame);
			break;
		default:
			// fall through: other menu items may be handled by menu adders.
			break;
		}
	}

	public void openEditorForFile(File file, String type) {
		if (file == null || !file.exists()) {
			showMessageDialog("File does not exist: " + (file != null ? file.getAbsolutePath() : "(null)"),
					"Open Editor", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String editorCmd = getProperties().getProperty(type + "Editor", "").trim();
		try {
			if (!editorCmd.isEmpty()) {
				java.util.List<String> cmdTokens = parseCommandTokens(editorCmd);
				cmdTokens.add(file.getAbsolutePath());
				new ProcessBuilder(cmdTokens).start();
			} else {
				// Use default system editor
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().edit(file);
				} else {
					showMessageDialog("Cannot open editor. Please configure an editor command in Preferences.",
							"Open Editor", JOptionPane.WARNING_MESSAGE);
				}
			}
		} catch (IOException ex) {
			showMessageDialog("Error opening editor for " + file.getName() + ":\n" + ex.getMessage(),
					"Open Editor", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showPreferencesDialog() {
		final String previousSigDigits = getProperties().getProperty("significantDigits", "3");
		final boolean previousDisableSigDigits = DbViewer.disableSigDigits;
		new PreferenceDialog(this).showDialog();
		// Reload auto graphics setting from properties after dialog closes
		synchronized (propertiesLock) {
			if (savedProperties != null) {
				String propAutoGraphics = savedProperties.getProperty("autoGenerateGraphics", "false");
				autoGenerateGraphics = "true".equalsIgnoreCase(propAutoGraphics);
				// Reload limit significant digits setting and apply to DbViewer
				String propLimitSigDigits = savedProperties.getProperty("limitSigDigits", "false");
				boolean limitSigDigits = "true".equalsIgnoreCase(propLimitSigDigits);
				DbViewer.disableSigDigits = !limitSigDigits;  // Inverse: if limiting is enabled, disabling is off
			}
		}
		final String currentSigDigits = getProperties().getProperty("significantDigits", "3");
		if (previousDisableSigDigits != DbViewer.disableSigDigits || !previousSigDigits.equals(currentSigDigits)) {
			if (dbView instanceof DbViewer) {
				((DbViewer) dbView).refreshOpenResultsSignificantDigits();
			}
		}
	}

	/**
	 * Splits a shell-style command string into a list of tokens suitable for
	 * passing to {@link ProcessBuilder}. Handles double- and single-quoted
	 * substrings so that paths containing spaces can be quoted (e.g.,
	 * {@code "C:\Program Files\editor.exe" --flag} → two tokens). Note:
	 * escaped quotes inside a quoted string (e.g., {@code "say \"hi\""}) are
	 * not supported.
	 */
	private java.util.List<String> parseCommandTokens(String cmd) {
		// Match: "…" (group 1) | '…' (group 2) | non-whitespace run (group 3)
		final java.util.regex.Pattern TOKEN_PATTERN =
				java.util.regex.Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
		java.util.List<String> tokens = new java.util.ArrayList<>();
		java.util.regex.Matcher m = TOKEN_PATTERN.matcher(cmd.trim());
		while (m.find()) {
			if (m.group(1) != null) {
				tokens.add(m.group(1));
			} else if (m.group(2) != null) {
				tokens.add(m.group(2));
			} else {
				tokens.add(m.group(3));
			}
		}
		return tokens;
	}

	private boolean hasQueryFileConfigured() {
		String qFile = (queryFilename != null && !queryFilename.trim().isEmpty())
				? queryFilename
				: getProperties().getProperty("queryFile", "");
		return qFile != null && !qFile.trim().isEmpty() && new File(qFile).exists();
	}

	private void refreshQueryFileMenuEnabled() {
		if (editQueryFileMenu != null) {
			editQueryFileMenu.setEnabled(hasQueryFileConfigured());
		}
	}

	private void openConfiguredQueryFileInXmlEditor() {
		String qFile = (queryFilename != null && !queryFilename.trim().isEmpty())
				? queryFilename
				: getProperties().getProperty("queryFile", "");
		if (qFile == null || qFile.trim().isEmpty()) {
			showMessageDialog("No query file is configured. Use File > Select Query File first.",
					"Query File", JOptionPane.INFORMATION_MESSAGE);
			refreshQueryFileMenuEnabled();
			return;
		}
		File f = new File(qFile);
		if (!f.exists()) {
			showMessageDialog("Query file does not exist: " + f.getAbsolutePath(), "Query File",
					JOptionPane.ERROR_MESSAGE);
			refreshQueryFileMenuEnabled();
			return;
		}
		openEditorForFile(f, "xml");
	}

	// Re-add previously existing methods and inner classes removed by accident
	public static InterfaceMain getInstance() { return main; }

	// YD commented lines 526-536 out because we removed "New","Save","Save As" from "File" dropdown menu
	// public JMenuItem getNewMenu() { return newMenu; }

	public JMenuItem getSaveMenu() { return saveMenu; }
	public JMenuItem getSaveAsMenu() { return saveAsMenu; }
	public JMenuItem getQuitMenu() { return quitMenu; }
	// public JMenuItem getCopyMenu() { return copyMenu; }
	// public JMenuItem getPasteMenu() { return pasteMenu; }
	public JMenuItem getUndoMenu() { return undoMenu; }
	public JMenuItem getRedoMenu() { return redoMenu; }
	public JMenuItem getBatchMenu() { return batchMenu; }

	public void fireControlChange(String newValue) {
		cancelPendingStartupDbViewerTimer();
		if (newValue.equals(oldControl)) { oldControl += "Same"; }
		logStartupTiming("InterfaceMain:control old=" + oldControl + " new=" + newValue);
		fireProperty("Control", oldControl, newValue);
	 oldControl = newValue;
	}
	public void fireProperty(String propertyName, Object oldValue, Object newValue) {
		final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
		for (PropertyChangeListener listener : mainFrame.getPropertyChangeListeners()) {
			listener.propertyChange(event);
		}
	}

	/**
	 * Manages a hierarchical menu structure by wrapping {@link JMenuItem} and
	 * {@link JMenu} instances in a tree of {@code MenuManager} nodes.
	 *
	 * <p><b>Automatic JMenuItem-to-JMenu promotion:</b> If a node was originally
	 * created with a leaf {@code JMenuItem} (i.e. not a {@code JMenu}) and
	 * {@link #addMenuItem} is subsequently called to attach children to it, the
	 * node is <em>automatically promoted</em> to a {@code JMenu}. During
	 * promotion the following properties are copied from the original item to the
	 * new menu: text, mnemonic, enabled state, tool-tip text, icon, accelerator
	 * key, action command, and all registered {@code ActionListener}s.
	 *
	 * <p>Callers that require an immutable menu structure (i.e. a node should
	 * never change type after creation) should ensure that any item intended to
	 * have sub-items is registered as a {@code JMenu} from the outset, rather
	 * than relying on this implicit promotion.
	 */
	public class MenuManager {
		private JMenuItem menuValue;
		private Map<Integer, MenuManager> subItems;
		private SortedSet<Integer> sepList;
		MenuManager(JMenuItem menuValue) {
			this.menuValue = menuValue;
			sepList = null;
			if (menuValue == null || menuValue instanceof JMenu) {
				subItems = new TreeMap<Integer, MenuManager>();
			} else { subItems = null; }
		}
		public void addSeparator(int where) {
			if (sepList == null) { sepList = new TreeSet<Integer>(); }
			sepList.add(where);
		}
		public int addMenuItem(JMenuItem menu, int where) {
			// If this node currently represents a leaf JMenuItem but callers are trying
			// to add children, promote it to a JMenu.
			if (subItems == null && menuValue instanceof JMenuItem && !(menuValue instanceof JMenu)) {
				final JMenuItem old = menuValue;
				final JMenu promoted = new JMenu(old.getText());
				promoted.setMnemonic(old.getMnemonic());
				promoted.setEnabled(old.isEnabled());
				promoted.setToolTipText(old.getToolTipText());
				promoted.setIcon(old.getIcon());
				promoted.setAccelerator(old.getAccelerator());

				// Preserve listeners and action command.
				promoted.setActionCommand(old.getActionCommand());
				for (java.awt.event.ActionListener l : old.getActionListeners()) {
					promoted.addActionListener(l);
				}

				menuValue = promoted;
				subItems = new TreeMap<Integer, MenuManager>();
			}

			if (subItems == null) {
				if (menuValue == null || menuValue instanceof JMenu) {
					subItems = new TreeMap<Integer, MenuManager>();
				} else {
					throw new IllegalStateException("Cannot add menu item to non-menu parent: " + menuValue);
				}
			}
			if (subItems.containsKey(where)) { return addMenuItem(menu, where + 1); }
			subItems.put(where, new MenuManager(menu));
			return where;
		}
		public MenuManager getSubMenuManager(int where) {
			if (!subItems.containsKey(where)) { return null; }
			return subItems.get(where);
		}
		JMenuBar createMenu() {
			JMenuBar ret = new JMenuBar();
			Object[] keys = subItems.keySet().toArray();
			for (int i = 0; i < keys.length; ++i) {
				JMenuItem menu = subItems.get(keys[i]).createSubMenu();
				// Add extra padding to main menu items
				menu.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
				ret.add(menu);
			}
			return ret;
		}
		private JMenuItem createSubMenu() {
			if (subItems == null) { return menuValue; }
			Object[] keys = subItems.keySet().toArray();
			for (int i = 0; i < keys.length; ++i) {
				if (sepList != null && !sepList.isEmpty() && ((Integer) keys[i]).intValue() > ((Integer) sepList.first()).intValue()) {
					((JMenu) menuValue).addSeparator();
					sepList.remove(sepList.first());
				}
				menuValue.add(subItems.get(keys[i]).createSubMenu());
			}
			return menuValue;
		}
	}

	public Properties getProperties() {
		Properties copy = new Properties();
		synchronized (propertiesLock) {
			if (savedProperties != null) {
				copy.putAll(savedProperties);
			}
		}
		return copy;
	}

	public void setProperty(String key, String value) {
		synchronized (propertiesLock) {
			if (savedProperties != null) {
				if ("paramPath".equals(key)) {
					path = value;
				}
				savedProperties.setProperty(key, value);
				persistProperties();
			}
		}
	}

	public void removeProperty(String key) {
		synchronized (propertiesLock) {
			if (savedProperties != null) {
				if ("paramPath".equals(key)) {
					path = null;
				}
				savedProperties.remove(key);
				persistProperties();
			}
		}
	}

	public void updateProperties(java.util.function.Consumer<Properties> updates) {
		synchronized (propertiesLock) {
			if (savedProperties != null) {
				updates.accept(savedProperties);
				persistProperties();
			}
		}
	}

	protected Vector getRegions() {
		Vector funcTemp = new Vector<String>(1, 0);
		funcTemp.add("distinct-values");
		Vector ret = new Vector();
		QueryProcessor queryProc = XMLDB.getInstance().createQuery(
				"/scenario/world/" + ModelInterface.ModelGUI2.queries.QueryBuilder.regionQueryPortion + "/@name",
				funcTemp, null, null);
		try {
			Iter res = queryProc.iter();
			Item temp;
			while ((temp = res.next()) != null) { ret.add(temp.toJava()); }
		} catch (QueryException e) { e.printStackTrace(); }
		finally { queryProc.close(); }
		ret.add("Global");
		return ret;
	}

	public MenuAdder getMenuAdder(String classname) {
		for (Iterator<MenuAdder> it = menuAdders.iterator(); it.hasNext();) {
			MenuAdder curr = it.next();
			if (curr.getClass().getName().equals(classname)) { return curr; }
		}
		return null;
	}

	public void runBatch() {
		FileChooser fc = FileChooserFactory.getFileChooser();
		final File[] result = fc.doFilePrompt(mainFrame, "Open Batch File", FileChooser.LOAD_DIALOG,
				new File(getProperties().getProperty("lastDirectory", ".")), new XMLFilter());
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (result != null) {
					for (File file : result) {
						Document doc = FileUtils.loadDocument(file, null);
						if (doc != null) {
							runBatch(doc.getDocumentElement());
						}
					}
				}
			}
		}).start();
	}

	private void runBatch(Node doc) {
		if (doc.getNodeName().equals("queries")) {
			System.out.println("Batch queries are not yet merged with this functionality.");
			System.out.println("Please open a database then run the batch file.");
			return;
		}
		NodeList commands = doc.getChildNodes();
		for (int i = 0; i < commands.getLength(); ++i) {
			if (commands.item(i).getNodeName().equals("class")) {
				Element currClass = (Element) commands.item(i);
				String className = currClass.getAttribute("name");
				MenuAdder runner = getMenuAdder(className);
				if (runner != null && runner instanceof BatchRunner) {
					((BatchRunner) runner).runBatch(currClass);
				} else {
					showMessageDialog("Could not find batch runner for class " + className, "Batch File Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		showMessageDialog("Finished running batch file", "Batch File Complete", JOptionPane.INFORMATION_MESSAGE);
	}

	public void showMessageDialog(Object message, String title, int messageType) {
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println(message);
			return;
		}
		JOptionPane.showMessageDialog(mainFrame, message, title, messageType);
	}

	/**
	 * Wrapper around JOptionPane.showConfirmDialog with a consistent signature used
	 * throughout the application.
	 */
	public int showConfirmDialog(Object message, String title, int optionType, int messageType) {
		return showConfirmDialog(message, title, optionType, messageType, JOptionPane.YES_OPTION);
	}

	/**
	 * Wrapper around JOptionPane.showConfirmDialog with a default option.
	 */
	public int showConfirmDialog(Object message, String title, int optionType, int messageType, int defaultOption) {
		if (GraphicsEnvironment.isHeadless()) {
			// In headless mode, just return the default to keep batch runs deterministic.
			return defaultOption;
		}
		try {
			Object[] options;
			switch (optionType) {
			case JOptionPane.YES_NO_OPTION:
				options = new Object[] { "Yes", "No" };
				break;
			case JOptionPane.OK_CANCEL_OPTION:
				options = new Object[] { "OK", "Cancel" };
				break;
			case JOptionPane.YES_NO_CANCEL_OPTION:
				options = new Object[] { "Yes", "No", "Cancel" };
				break;
			default:
				// Fallback to JOptionPane defaults when we don't recognize the option type.
				return JOptionPane.showConfirmDialog(mainFrame, message, title, optionType, messageType);
			}

			int initialIndex = 0;
			if (optionType == JOptionPane.OK_CANCEL_OPTION) {
				initialIndex = (defaultOption == JOptionPane.CANCEL_OPTION) ? 1 : 0;
			} else if (optionType == JOptionPane.YES_NO_OPTION) {
				initialIndex = (defaultOption == JOptionPane.NO_OPTION) ? 1 : 0;
			} else if (optionType == JOptionPane.YES_NO_CANCEL_OPTION) {
				initialIndex = (defaultOption == JOptionPane.NO_OPTION) ? 1 : (defaultOption == JOptionPane.CANCEL_OPTION ? 2 : 0);
			}

			Object initialValue = options[Math.max(0, Math.min(initialIndex, options.length - 1))];
			return showOptionDialog(message, title, options, messageType, initialValue, defaultOption);
		} catch (Exception ex) {
			// Fallback to standard confirm dialog if any of the mapping logic fails.
			return JOptionPane.showConfirmDialog(mainFrame, message, title, optionType, messageType);
		}
	}

	public int showOptionDialog(Object message, String title, Object[] options, int messageType, Object initialValue,
			int defaultClosedResult) {
		if (GraphicsEnvironment.isHeadless()) {
			return defaultClosedResult;
		}
		if (options == null || options.length == 0) {
			return JOptionPane.CLOSED_OPTION;
		}
		int result = JOptionPane.showOptionDialog(mainFrame, message, title, JOptionPane.DEFAULT_OPTION, messageType, null,
				options, initialValue);
		return result >= 0 ? result : defaultClosedResult;
	}

	private void persistProperties() {
		synchronized (propertiesLock) {
			try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
				Properties sortedProps = new Properties() {
					@Override
					public java.util.Enumeration<Object> keys() {
						return java.util.Collections.enumeration(new java.util.TreeSet<Object>(super.keySet()));
					}
				};
				sortedProps.putAll(savedProperties);
				sortedProps.storeToXML(fos, "ModelInterface properties");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private static boolean isMac() {
		String os = System.getProperty("os.name", "").toLowerCase();
		return os.contains("mac");
	}

	private int getMenuShortcutMask() {
		try {
			return (Integer) Toolkit.class.getMethod("getMenuShortcutKeyMaskEx").invoke(Toolkit.getDefaultToolkit());
		} catch (Exception ex) {
			try {
				return (Integer) Toolkit.class.getMethod("getMenuShortcutKeyMask").invoke(Toolkit.getDefaultToolkit());
			} catch (Exception ex2) {
				return java.awt.event.InputEvent.CTRL_DOWN_MASK;
			}
		}
	}
}

