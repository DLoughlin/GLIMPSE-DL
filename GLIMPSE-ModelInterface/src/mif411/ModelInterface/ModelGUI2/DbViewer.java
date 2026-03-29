/** LEGAL NOTICE
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
package ModelInterface.ModelGUI2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.basex.api.dom.BXDoc;
import org.basex.api.dom.BXNode;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.basex.query.value.node.ANode;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.traversal.NodeFilter;

import ModelInterface.BatchRunner;
import ModelInterface.InterfaceMain;
import ModelInterface.MenuAdder;
import ModelInterface.ModelGUI2.QueryTreeModel.QueryGroup;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.queries.SingleQueryExtension;
import ModelInterface.ModelGUI2.tables.BaseTableModel;
import ModelInterface.ModelGUI2.tables.TableSorter;
import ModelInterface.ModelGUI2.tables.TableTransferHandler;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.common.RecentFilesList.RecentFile;
import filter.FilterTreePaneYears;

/**
 * DbViewer is the main class for the database viewing and query interface in
 * the GLIMPSE ModelInterface application.
 * <p>
 * This class provides the graphical user interface (GUI) and logic for
 * interacting with scenario databases, regions, and queries. It supports
 * opening and managing XMLDB databases, viewing and filtering scenarios and
 * regions, running and exporting queries, managing favorite queries, and batch
 * operations. The class integrates with the application's menu system and
 * handles user actions through event listeners. It also provides methods for
 * reading and writing query files, exporting results, and managing UI state.
 * <p>
 * Key features include:
 * <ul>
 * <li>Opening and managing scenario databases</li>
 * <li>Viewing, filtering, and selecting scenarios, regions, and queries</li>
 * <li>Running queries and exporting results to CSV or Excel</li>
 * <li>Managing favorite queries and preset region lists</li>
 * <li>Batch query execution and export</li>
 * <li>Integration with the application's menu system</li>
 * <li>Support for undo/redo and advanced query tree operations</li>
 * </ul>
 * <p>
 * The class is designed for use within a Swing-based desktop application and
 * relies on various helper classes and external libraries for XML database
 * access, file operations, and UI components.
 *
 * @author Battelle Memorial Institute
 * @version 1.1
 * @since 2012
 */
public class DbViewer implements MenuAdder, BatchRunner, ActionListener {
	private JPanel loadingPanel;
	private JLabel loadingLabel;
	private volatile boolean dbViewInitialized = false;
	private javax.swing.SwingWorker<StartupData, Void> startupLoader;
	private enum StartupLifecycleState {
		IDLE,
		PREPARING,
		OPENING_DB,
		LOADING_DATA,
		BUILDING_UI,
		READY,
		FAILED,
		SHUTTING_DOWN
	}
	private volatile StartupLifecycleState startupState = StartupLifecycleState.IDLE;
	private volatile String lastControlOldValue = "(unset)";
	private volatile String lastControlNewValue = "(unset)";

	private boolean isStartupActive() {
		return startupState == StartupLifecycleState.PREPARING
				|| startupState == StartupLifecycleState.OPENING_DB
				|| startupState == StartupLifecycleState.LOADING_DATA
				|| startupState == StartupLifecycleState.BUILDING_UI;
	}

	private boolean isReadyForShutdownCleanup() {
		return startupState == StartupLifecycleState.READY || dbViewInitialized;
	}

	private static String safeControlValue(Object value) {
		return value == null ? "null" : String.valueOf(value);
	}

	private void updateLastControlTransition(Object oldValue, Object newValue) {
		lastControlOldValue = safeControlValue(oldValue);
		lastControlNewValue = safeControlValue(newValue);
		InterfaceMain.logStartupTiming("DbViewer:control old=" + lastControlOldValue + " new=" + lastControlNewValue
				+ " startupState=" + startupState + " dbViewInitialized=" + dbViewInitialized);
	}

	private void setStartupState(StartupLifecycleState newState) {
		StartupLifecycleState previousState = startupState;
		startupState = newState;
		InterfaceMain.logStartupTiming("DbViewer:state " + previousState + " -> " + newState
				+ " [control old=" + lastControlOldValue + ", new=" + lastControlNewValue
				+ ", dbViewInitialized=" + dbViewInitialized + "]");
	}

	private void invalidateQueriesDocument(String reason) {
		queriesDoc = null;
		queries = null;
		InterfaceMain.logStartupTiming("DbViewer:invalidateQueriesDocument reason=" + reason
				+ " [control old=" + lastControlOldValue + ", new=" + lastControlNewValue + "]");
	}

	private static final class StartupData {
		private final Vector<ScenarioListItem> scenarios;
		private final Vector regions;
		private final QueryTreeModel queries;

		private StartupData(Vector<ScenarioListItem> scenarios, Vector regions, QueryTreeModel queries) {
			this.scenarios = scenarios;
			this.regions = regions;
			this.queries = queries;
		}
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	private static void logStartup(String stage, long startNanos) {
		InterfaceMain.logStartupTiming("DbViewer:" + stage + " " + elapsedMillis(startNanos) + " ms");
	}

	private static final String STARTUP_MESSAGE_PREPARING = "Preparing database viewer...";
	private static final String STARTUP_MESSAGE_OPENING_DB = "Opening database...";
	private static final String STARTUP_MESSAGE_LOADING_SCENARIOS = "Loading scenarios...";
	private static final String STARTUP_MESSAGE_LOADING_REGIONS = "Loading regions...";
	private static final String STARTUP_MESSAGE_LOADING_QUERIES = "Loading query definitions...";
	private static final String STARTUP_MESSAGE_BUILDING_LISTS = "Building scenario and region lists...";
	private static final String STARTUP_MESSAGE_BUILDING_TREE = "Building query tree...";
	private static final String STARTUP_MESSAGE_BUILDING_QUERY_PANEL = "Preparing query panel...";
	private static final String STARTUP_MESSAGE_BUILDING_ACTIONS = "Preparing controls...";
	private static final String STARTUP_MESSAGE_LAYOUT = "Laying out interface...";
	private static final String STARTUP_MESSAGE_FINISHING = "Finishing startup...";

	private static final String STARTUP_DIALOG_TITLE = "Database Startup Error";
	private static final String OPEN_DB_DIALOG_TITLE = "Open DB Error";
	private static final int MAX_EXCEPTION_CHAIN_DEPTH = 5;

	private void updateStartupMessage(final String message) {
		final InterfaceMain main = InterfaceMain.getInstance();
		if (main != null) {
			main.updateStartupLoadingMessage(message);
		}
	}

	private void showLoadingShell() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame frame = main.getFrame();
		if (frame == null) {
			return;
		}
		main.showStartupLoadingView(STARTUP_MESSAGE_PREPARING);
	}

	private String formatThrowableSummary(Throwable failure) {
		if (failure == null) {
			return "Unknown error";
		}
		StringBuilder detail = new StringBuilder();
		Throwable current = failure;
		int depth = 0;
		while (current != null && depth < MAX_EXCEPTION_CHAIN_DEPTH) {
			if (depth > 0) {
				detail.append("\nCaused by: ");
			}
			detail.append(current.getClass().getSimpleName());
			String message = current.getMessage();
			if (message != null && !message.trim().isEmpty()) {
				detail.append(": ").append(message.trim());
			}
			current = current.getCause();
			depth++;
		}
		return detail.toString();
	}

	private Throwable unwrapFailure(Throwable failure) {
		Throwable root = failure;
		while (root instanceof java.util.concurrent.ExecutionException && root.getCause() != null) {
			root = root.getCause();
		}
		return root;
	}

	private String buildOpenFailureMessage(Throwable failure, File dbFile) {
		StringBuilder message = new StringBuilder("Could not open the XML database");
		if (dbFile != null) {
			message.append(" at:\n").append(dbFile.getAbsolutePath());
		}
		Throwable root = unwrapFailure(failure);
		if (XMLDB.isSuppressedBaseXResourceException(root)) {
			message.append("\n\nDetails:\nThe database runtime could not resolve bundled BaseX resources in this launch environment.");
			return message.toString();
		}
		String detail = formatThrowableSummary(root);
		if (detail != null && !detail.trim().isEmpty()) {
			message.append("\n\nDetails:\n").append(detail);
		}
		return message.toString();
	}

	private String buildStartupFailureTitle(Throwable failure) {
		Throwable root = unwrapFailure(failure);
		if (root instanceof QueryException) {
			return "Startup Query Error";
		}
		if (root instanceof IllegalStateException || root instanceof NullPointerException) {
			return STARTUP_DIALOG_TITLE;
		}
		return "Database View Initialization Error";
	}

	private String buildStartupFailureMessage(Throwable failure) {
		Throwable root = unwrapFailure(failure);
		if (root instanceof InterruptedException) {
			Thread.currentThread().interrupt();
			return "Database loading was interrupted before initialization completed.";
		}
		if (XMLDB.isSuppressedBaseXResourceException(root)) {
			return "The database runtime could not resolve bundled BaseX resources in this launch environment.";
		}
		String prefix;
		if (root instanceof IllegalStateException || root instanceof NullPointerException) {
			prefix = "The database opened, but required startup data was missing or invalid while building the view.";
		} else if (root instanceof QueryException) {
			prefix = "The database opened, but a query failed while loading startup data.";
		} else {
			prefix = "The database opened, but the interface could not finish initializing the database view.";
		}
		String detail = formatThrowableSummary(root);
		return detail == null || detail.trim().isEmpty() ? prefix : prefix + "\n\nDetails:\n" + detail;
	}

	private StartupData validateStartupData(StartupData data) {
		if (data == null) {
			throw new IllegalStateException("Startup data was not created.");
		}
		if (data.scenarios == null) {
			throw new IllegalStateException("Scenario list could not be loaded.");
		}
		if (data.regions == null) {
			throw new IllegalStateException("Region list could not be loaded.");
		}
		if (data.queries == null) {
			throw new IllegalStateException("Query definitions could not be loaded.");
		}
		return data;
	}

	private void validateQueriesDocument() {
		if (queriesDoc == null) {
			throw new IllegalStateException("Query definitions were not loaded before startup queries were requested.");
		}
		if (queriesDoc.getDocumentElement() == null) {
			throw new IllegalStateException("Query definitions file is missing the root element.");
		}
	}

	private String describeQueryFile(File queryFile) {
		return queryFile == null ? "(none)" : queryFile.getAbsolutePath();
	}

	private File findDefaultStartupQueryFile() {
		String[] candidates = new String[] {
				"config" + File.separator + "Main_queries_GLIMPSE-8.2.xml",
				"config" + File.separator + "Main_queries_GLIMPSE-7p0.xml",
				"config" + File.separator + "Main_queries.xml",
				"Main_queries.xml"
		};
		for (String candidate : candidates) {
			File file = new File(candidate);
			if (file.exists() && file.isFile()) {
				return file.getAbsoluteFile();
			}
		}
		return null;
	}

	private File resolveStartupQueryFile(Properties prop, JFrame parentFrame) {
		if (prop == null) {
			prop = new Properties();
		}
		String queryFileName = prop.getProperty("queryFile", null);
		File queryFile = queryFileName != null && !queryFileName.trim().isEmpty() ? new File(queryFileName) : null;
		if (queryFile == null) {
			System.out.println("No query file specified in properties. Select query file via ModelInterface file menu.");
		}
		if (queryFile == null || !queryFile.exists()) {
			File defaultQueryFile = findDefaultStartupQueryFile();
			if (defaultQueryFile != null) {
				queryFile = defaultQueryFile;
				queryFileName = defaultQueryFile.getAbsolutePath();
				System.out.println("Using default startup query file: " + queryFileName);
			} else {
				FileChooser fc = FileChooserFactory.getFileChooser();
				final FileFilter xmlFilter = new XMLFilter();
				File startDir = new File(prop.getProperty("lastDirectory", "."));
				File[] xmlFiles = fc.doFilePrompt(parentFrame,
						"Could not find query file. Please select one to continue startup.", FileChooser.LOAD_DIALOG,
						startDir, xmlFilter);
				if (xmlFiles != null && xmlFiles.length > 0 && xmlFiles[0] != null) {
					queryFile = xmlFiles[0];
					queryFileName = queryFile.getAbsolutePath();
				}
			}
			if (queryFile == null) {
				throw new IllegalStateException(
						"No startup query file could be resolved. Configure 'queryFile' or place a default query file under 'config'.");
			}
			prop.setProperty("queryFile", queryFileName);
		}
		return queryFile;
	}

	private void ensureQueriesDocumentLoaded(File queryFile) {
		if (queriesDoc != null) {
			return;
		}
		if (queryFile == null) {
			throw new IllegalStateException("No query file is configured for startup.");
		}
		queriesDoc = readQueries(queryFile);
		if (queriesDoc == null) {
			throw new IllegalStateException("The query file could not be parsed: " + describeQueryFile(queryFile));
		}
		if (queriesDoc.getDocumentElement() == null) {
			throw new IllegalStateException("The query file did not contain a document element: " + describeQueryFile(queryFile));
		}
	}

	private File prepareQueryDefinitionsForStartup() {
		InterfaceMain main = InterfaceMain.getInstance();
		if (main == null) {
			throw new IllegalStateException("InterfaceMain is not available while preparing query definitions.");
		}
		File queryFile = resolveStartupQueryFile(main.getProperties(), main.getFrame());
		ensureQueriesDocumentLoaded(queryFile);
		return queryFile;
	}

	private Document queriesDoc;

	private static String controlStr = "DbViewer";

	private DOMImplementationLS implls;

	protected Vector<ScenarioListItem> scns;
	public JList scnList;
	protected JList regionList;
	protected Vector regions;
	protected QueryTreeModel queries;
	private JTabbedPane tablesTabs = new JTabbedPane();
	private JSplitPane scenarioRegionSplit;
	private JSplitPane queriesSplit;
	private JSplitPane tableCreatorSplit;
	private JMenuItem queriesLockMenu;
	private JMenuItem queriesCreateMenu;
	private JMenuItem queriesEditMenu;
	private JMenuItem queriesRemoveMenu;
	private JMenuItem queriesUpdateMenu;
	private JMenuItem significantDigitsMenu;
	private JMenuItem enableUnitConversionsMenu;
	private JMenuItem createFavoritesMenu;// YD Feb-2024
	private JMenuItem loadFavoritesMenu;
	private JMenuItem appendFavoritesMenu;// YD Feb-2024
	private JMenuItem betaMn;
	// New: move Save/Save As under Tools > Queries menu
	private JMenuItem queriesSaveMenu;
	private JMenuItem queriesSaveAsMenu;

	private JMenuItem menuExpPrn;

	private String filteringText;
	// Allow buttons to use a standard Swing height; avoid hard-coding a tiny row height.

	ArrayList<String> region_list = new ArrayList<String>();
	ArrayList<String> subregion_list = new ArrayList<String>();
	ArrayList<String> preset_region_list = new ArrayList<String>();
	private JComboBox<String> comboBoxPresetRegions;
	private String[] preset_choices;
	private JScrollPane listScrollRegions;
	private JScrollPane listScrollQueries;
	public static boolean queryTreeLocked = true;
	public static boolean disableSigDigits = false;
	public static boolean enableUnitConversions = true;

	public static final String SCENARIO_LIST_NAME = "scenario list";
	public static final String REGION_LIST_NAME = "region list";

	final int bottomStripHeight = ModelInterface.common.SwingButtonSizer.STANDARD_BUTTON_HEIGHT + 6;  
	
	private static Map<String, String> selectedYears = null;

	public static ArrayList<java.awt.Window> openWindows = new ArrayList<java.awt.Window>();
	
	private JFrame parentFrame;
	private BatchExecutionController batchExecutionController;
	private FavoriteQueriesManager favoriteQueriesManager;
	
	// Progress is now shown in InterfaceMain's status bar.
	// Keep counters here to compute progress.
	private static int totalQueries = 0;
	private static int completedQueries = 0;
	// Track tab closures so we only count completion once per tab.
	private static final java.util.Set<java.awt.Component> activeQueryTabs = java.util.Collections
			.synchronizedSet(new java.util.HashSet<java.awt.Component>());

	/**
	 * Registers that a new query is starting so the progress UI can track it.
	 * @param tab The component for the tab in which the query will run.
	 */
	public static void registerNewQuery(java.awt.Component tab) {
		activeQueryTabs.add(tab);
		totalQueries++;
		updateProgressUI();
	}

	/**
	 * Registers that a query has completed so the progress UI can track it.
	 * Also removes the given tab component from the in-flight set so that a
	 * subsequent tab-close does not count the same query as completed a second time.
	 */
	static void registerQueryCompleted(java.awt.Component tab) {
		if (activeQueryTabs.remove(tab)) {
			registerQueryCompleted();
		}
	}

	/**
	 * Registers that a query has completed so the progress UI can track it.
	 */
	static void registerQueryCompleted() {
		completedQueries++;
		updateProgressUI();
		if (completedQueries >= totalQueries) {
			finishProgressUI();
		}
	}

	private static void updateProgressUI() {
		InterfaceMain main = InterfaceMain.getInstance();
		if (main == null) {
			return;
		}
		main.updateQueryProgressStatus(completedQueries, totalQueries);
	}

	private static void finishProgressUI() {
		final InterfaceMain main = InterfaceMain.getInstance();
		if (main == null) {
			totalQueries = 0;
			completedQueries = 0;
			return;
		}
		javax.swing.Timer timer = new javax.swing.Timer(800, ev -> {
			main.clearQueryProgressStatus();
			totalQueries = 0;
			completedQueries = 0;
		});
		timer.setRepeats(false);
		timer.start();
	}

	public DbViewer() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		this.parentFrame = parentFrame;
		final DbViewer thisViewer = this;
		this.batchExecutionController = new BatchExecutionController(this);
		

		try {
			DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
			implls = (DOMImplementationLS) reg.getDOMImplementation("XML 3.0");
			if (implls == null) {
				System.out.println("Could not find a DOM3 Load-Save compliant parser.");
				InterfaceMain.getInstance().showMessageDialog("Could not find a DOM3 Load-Save compliant parser.",
						"Initialization Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		} catch (Exception e) {
			System.err.println("Couldn't initialize DOMImplementation: " + e);
			InterfaceMain.getInstance().showMessageDialog("Couldn't initialize DOMImplementation\n" + e,
					"Initialization Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		if (parentFrame == null) {
			// no gui components available such as in batch mode.
			return;

		}

		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("Control")) {
					updateLastControlTransition(evt.getOldValue(), evt.getNewValue());
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						boolean shutdownTriggered = false;
						if (DbViewer.this.isStartupActive()) {
							if (startupLoader != null && !startupLoader.isDone()) {
								startupLoader.cancel(true);
							}
							invalidateQueriesDocument("control-transition-aborted-startup");
							setStartupState(StartupLifecycleState.FAILED);
							// Even if startup never reached READY, the DB may have been opened;
							// ensure we initiate an asynchronous shutdown so resources are released.
							shutdownQueriesAndCloseDatabaseAsync();
							shutdownTriggered = true;
						}
						if (DbViewer.this.isReadyForShutdownCleanup()) {
							setStartupState(StartupLifecycleState.SHUTTING_DOWN);
							// Don't block the EDT during shutdown; cancel queries quickly and close DB asynchronously.
							if (!shutdownTriggered) {
								shutdownQueriesAndCloseDatabaseAsync();
							}
							
							if (queries != null && queries.hasChanges() && InterfaceMain.getInstance().showConfirmDialog(
									"The Queries have been modified.  Do you want to save them?", "Confirm Save Queries",
									JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
									JOptionPane.YES_OPTION) == JOptionPane.YES_OPTION) {
								writeQueries();
							}
							Properties prop = main.getProperties();
							if (prop != null && scenarioRegionSplit != null && queriesSplit != null && tableCreatorSplit != null) {
								prop.setProperty("scenarioRegionSplit",
										String.valueOf(scenarioRegionSplit.getDividerLocation()));
								prop.setProperty("queriesSplit", String.valueOf(queriesSplit.getDividerLocation()));
								prop.setProperty("tableCreatorSplit", String.valueOf(tableCreatorSplit.getDividerLocation()));
							}
						}
						main.getUndoManager().discardAllEdits();
						main.refreshUndoRedo();
						main.getSaveMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().setEnabled(false);
						main.getSaveMenu().setEnabled(false);
						
						// Do not clear the entire content pane; InterfaceMain installs a status bar
						// in BorderLayout.SOUTH that must remain visible.
						Container cp = parentFrame.getContentPane();
						java.awt.Component existingCenter = null;
						if (cp.getLayout() instanceof BorderLayout) {
							existingCenter = ((BorderLayout) cp.getLayout()).getLayoutComponent(BorderLayout.CENTER);
						}
						if (existingCenter != null) {
							cp.remove(existingCenter);
							cp.revalidate();
							cp.repaint();
						}

						// XMLDB.closeDatabase() is handled by shutdownQueriesAndCloseDatabaseAsync
					}
					if (evt.getNewValue().equals(controlStr)) {
						if (DbViewer.this.isStartupActive() || startupState == StartupLifecycleState.READY) {
							InterfaceMain.logStartupTiming("DbViewer:ignoring duplicate control enter for " + controlStr);
							return;
						}
						setStartupState(StartupLifecycleState.PREPARING);
						Properties prop = main.getProperties();
						File queryFile = resolveStartupQueryFile(prop, parentFrame);

						// TODO: move to load preferences
						scenarioRegionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
						scenarioRegionSplit.setResizeWeight(.5);
						queriesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
						// queriesSplit.setLeftComponent(scenarioRegionSplit);
						queriesSplit.setResizeWeight(.5);
						tableCreatorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
						String tempInt;
						try {
							if ((tempInt = prop.getProperty("scenarioRegionSplit")) != null) {
								scenarioRegionSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
							if ((tempInt = prop.getProperty("queriesSplit")) != null) {
								queriesSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
							if ((tempInt = prop.getProperty("tableCreatorSplit")) != null) {
								tableCreatorSplit.setDividerLocation(Integer.valueOf(tempInt));
							}
						} catch (NumberFormatException nfe) {
							System.out.println("Invalid split location preference: " + nfe);
						}
						// Do not attach File menu Save/Save As here; they are managed under Tools > Queries
						// Ensure File menu items remain disabled to avoid duplicates
						main.getSaveMenu().setEnabled(false);
						main.getSaveAsMenu().setEnabled(false);

						ensureQueriesDocumentLoaded(queryFile);

					}
				} else if (evt.getPropertyName().equals("SelectQuery")) {
					FileChooser fc = FileChooserFactory.getFileChooser();
					Properties props = main.getProperties();
					File start = new File(props.getProperty("queryFile",
							props.getProperty("lastDirectory", ".")));
					File[] files = fc.doFilePrompt(parentFrame, "Open Query File", FileChooser.LOAD_DIALOG, start,
							new XMLFilter());
					if (files != null && files.length > 0) {
						File file = files[0];
						invalidateQueriesDocument("select-query-file");
						// update runtime and persist for next launch
						String oldFile = main.getProperties().getProperty("queryFile");
						main.setProperty("queryFile", file.getAbsolutePath());
						main.setProperty("lastDirectory", file.getParent());
						System.out.println("Selected query file: " + file.getAbsolutePath());
						updateQueryTree(file);
						main.fireProperty("QueryFileChanged", oldFile, file.getAbsolutePath());
					}
				}
			}
		});

	}

	/**
	 * Returns the selected years from the properties file or default years if not
	 * found.
	 * 
	 * @return Map of selected years.
	 */
	public static Map<String, String> getSelectedYearListFromPropFile() {

		if (DbViewer.selectedYears == null) {

			DbViewer.selectedYears = new HashMap<>();
			if (InterfaceMain.getInstance() != null) {
				Properties globalProperties = InterfaceMain.getInstance().getProperties();
				if (globalProperties != null) {
					Object rsp = globalProperties.get("selectedYearList");
					if (rsp != null) {
						String defaultYearStr = rsp.toString();
						System.out.println("DbViewer375: Using selectedYearsStr from properties file: " + defaultYearStr);

						String[] yearsArr = InterfaceMain.splitListProperty(defaultYearStr);
						Arrays.sort(yearsArr);
						DbViewer.selectedYears = new HashMap<>();
						if (!(yearsArr.length == 1 && yearsArr[0].equals(""))) {
							for (String year : yearsArr) {
								DbViewer.selectedYears.put(year + "", year + "");
							}
						}
					}
				}
			}

			if (DbViewer.selectedYears.size() == 0) {
				System.out.println(
						"No selected years found in properties file, using 2015-2100 in 5-year increments, except 2021.");
				DbViewer.selectedYears.put("2015", "2015");
				DbViewer.selectedYears.put("2021", "2021");
				DbViewer.selectedYears.put("2025", "2025");
				DbViewer.selectedYears.put("2030", "2030");
				DbViewer.selectedYears.put("2035", "2035");
				DbViewer.selectedYears.put("2040", "2040");
				DbViewer.selectedYears.put("2045", "2045");
				DbViewer.selectedYears.put("2050", "2050");
				DbViewer.selectedYears.put("2055", "2055");
				DbViewer.selectedYears.put("2060", "2060");
				DbViewer.selectedYears.put("2065", "2065");
				DbViewer.selectedYears.put("2070", "2070");
				DbViewer.selectedYears.put("2075", "2075");
				DbViewer.selectedYears.put("2080", "2080");
				DbViewer.selectedYears.put("2085", "2085");
				DbViewer.selectedYears.put("2090", "2090");
				DbViewer.selectedYears.put("2085", "2095");
				DbViewer.selectedYears.put("2100", "2100");
			}
		}
		return DbViewer.selectedYears;

	}

	/**
	 * Forces a refresh of the selected years from the properties file.
	 */
	public static void refreshSelectedYearListFromPropFile() {
		DbViewer.selectedYears = null;
		getSelectedYearListFromPropFile();
	}

	/**
	 * Returns all years from the properties file or default years if not found.
	 * 
	 * @return List of all years.
	 */
	public static List getAllYearListFromPropFile() {

		List<String> allYearsList = new ArrayList<String>();
		if (InterfaceMain.getInstance() != null) {
			Properties globalProperties = InterfaceMain.getInstance().getProperties();
			if (globalProperties != null) {
				String allYearStr = (String) globalProperties.get("allYearsList");
				System.out.println("DbViewer421: Setting allYearStr from properties file: " + allYearStr);

				if (allYearStr != null) {
					String[] yearsArr = InterfaceMain.splitListProperty(allYearStr);
					Arrays.sort(yearsArr);
					allYearsList = new ArrayList<String>(yearsArr.length);
					if (!(yearsArr.length == 1 && yearsArr[0].equals(""))) {
						for (String year : yearsArr) {
							allYearsList.add(year);
						}
					}
				}
			}
		}

		if (allYearsList.size() == 0) {
			System.out.println(
					"No allYearsList found in properties file, using 1990 and 2005-2100 in 5-year increments.");
			allYearsList = new ArrayList<String>();
			allYearsList.add("1990");
			for (int i = 2005; i < 2101; i += 5) {
				allYearsList.add(String.valueOf(i));
			}
			allYearsList.sort(String.CASE_INSENSITIVE_ORDER);
		}

		return allYearsList;
	}

	/**
	 * Creates a JMenuItem with the given title and adds this DbViewer as its
	 * ActionListener.
	 *
	 * @param title The text for the menu item.
	 * @return A configured JMenuItem.
	 */
	private JMenuItem makeMenuItem(String title) {
		JMenuItem ret = new JMenuItem(title);
		ret.addActionListener(this);
		return ret;
	}

	/**
	 * Adds menu items to the application's menu manager.
	 * <p>
	 * This method sets up the file, view, and advanced menu items in the
	 * application's menu bar. It configures their action listeners and
	 * enables/disables them based on the application's state.
	 *
	 * @param menuMan The menu manager to which menu items are added.
	 */
	public void addMenuItems(InterfaceMain.MenuManager menuMan) {
		final long menuStart = System.nanoTime();
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		final ActionListener thisListener = this;
		addFileMenuItems(menuMan, main, parentFrame, thisListener);
		logStartup("addMenuItems:file", menuStart);
		addViewMenuItems(menuMan, main, parentFrame);
		logStartup("addMenuItems:view", menuStart);
		addAdvancedMenuItems(menuMan, main, parentFrame);
		logStartup("addMenuItems:advanced", menuStart);
	}

	/**
	 * Adds file-related menu items to the application's menu manager.
	 *
	 * @param menuMan      The menu manager to which menu items are added.
	 * @param main         The main interface instance.
	 * @param parentFrame  The parent JFrame for dialogs and listeners.
	 * @param thisListener The ActionListener for the menu items.
	 */
	private void addFileMenuItems(InterfaceMain.MenuManager menuMan, InterfaceMain main, JFrame parentFrame,
			ActionListener thisListener) {
		JMenuItem menuItem = new JMenuItem("Open DB");
		menuItem.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuItem, 5);

		final JMenuItem menuManage = makeMenuItem("Manage DB");
		menuManage.setEnabled(false);

		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("Control")) {
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						menuManage.setEnabled(false);
						JMenuItem batchMenu = main.getBatchMenu();
						if(batchMenu != null) {
							batchMenu.removeActionListener(thisListener);
							batchMenu.addActionListener(main);
						}
					}
					if (evt.getNewValue().equals(controlStr)) {
						menuManage.setEnabled(true);
						JMenuItem batchMenu = main.getBatchMenu();
						if(batchMenu != null) {
							batchMenu.removeActionListener(main);
							batchMenu.addActionListener(thisListener);
						}
					}
				}
			}
		});

		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addSeparator(InterfaceMain.FILE_MENU_SEPERATOR);
		menuExpPrn = makeMenuItem("Export Tabs as CSVs");
		menuExpPrn.setEnabled(false);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuExpPrn, 35);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addSeparator(37);

		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			private int numQueries = 0;

			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("Control")) {
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						menuExpPrn.setEnabled(false);
					}
				} else if (evt.getPropertyName().equals("Query") && evt.getOldValue() == null) {
					menuExpPrn.setEnabled(true);
					++numQueries;
				} else if (evt.getPropertyName().equals("Query") && evt.getNewValue() == null) {
					if (--numQueries == 0) {
						menuExpPrn.setEnabled(false);
					}
				}
			}
		});
	}

	/**
	 * Adds view menu items to the application's menu manager.
	 * <p>
	 * This method sets up the view-related menu items such as "Close All Tabs",
	 * "Close All Windows", "Disable Significant Digits", "Disable Unit
	 * Conversions", and "Select Years" in the application's menu bar. It also
	 * configures their action listeners and enables/disables them based on the
	 * application's state.
	 *
	 * @param menuMan     The menu manager to which menu items are added.
	 * @param main        The main interface instance.
	 * @param parentFrame The parent JFrame for dialogs and listeners.
	 */
	private void addViewMenuItems(InterfaceMain.MenuManager menuMan, InterfaceMain main, JFrame parentFrame) {
		// Close All Tabs
		JMenuItem tabCl = new JMenuItem("Close All Tabs");
		tabCl.addActionListener(e -> closeAllTabs());
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(tabCl, 1);

		// Close All Windows
		JMenuItem winCl = new JMenuItem("Close All Windows");
		winCl.addActionListener(e -> closeAllWindows());
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(winCl, 2);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addSeparator(3);

		// Significant Digits Toggle
		significantDigitsMenu = new JMenuItem("Disable Significant Digits");
		significantDigitsMenu.addActionListener(this);
		significantDigitsMenu.setEnabled(true);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(significantDigitsMenu, 10);

		// Unit Conversions Toggle
		enableUnitConversionsMenu = new JMenuItem("Disable Unit Conversions");
		enableUnitConversionsMenu.addActionListener(this);
		enableUnitConversionsMenu.setEnabled(true);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(enableUnitConversionsMenu, 11);

		// Select Years
		JMenuItem yearsMn = new JMenuItem("Select Years to Show");
		yearsMn.addActionListener(e -> new FilterTreePaneYears());
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addSeparator(20);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(yearsMn, 21);
	}

	/**
	 * Adds advanced menu items to the application's menu manager. This includes
	 * menus for query management, such as locking/unlocking the query tree,
	 * saving, creating, editing, and removing queries. It also includes favorites
	 * management.
	 *
	 * @param menuMan     The menu manager to which menu items are added.
	 * @param main        The main interface instance.
	 * @param parentFrame The parent JFrame for dialogs and listeners.
	 */
	private void addAdvancedMenuItems(InterfaceMain.MenuManager menuMan, InterfaceMain main, JFrame parentFrame) {
		// Queries menus live under the global Edit menu.
		InterfaceMain.MenuManager editMM = menuMan.getSubMenuManager(InterfaceMain.EDIT_MENU_POS);
		if (editMM == null) {
			// Should not happen (InterfaceMain builds Edit), but guard anyway.
			JMenu editMenu = new JMenu("Edit");
			menuMan.addMenuItem(editMenu, InterfaceMain.EDIT_MENU_POS);
			editMM = menuMan.getSubMenuManager(InterfaceMain.EDIT_MENU_POS);
		}

		// InterfaceMain is responsible for creating Edit -> Query Tree submenu.
		InterfaceMain.MenuManager queriesMM = editMM.getSubMenuManager(InterfaceMain.TOOLS_SUBMENU1_POS);
		if (queriesMM == null) {
			// Fallback for legacy/partial menu configurations.
			editMM.addMenuItem(new JMenu("Query Tree"), InterfaceMain.TOOLS_SUBMENU1_POS);
			queriesMM = editMM.getSubMenuManager(InterfaceMain.TOOLS_SUBMENU1_POS);
		}

		// Query Tree Lock/Unlock
		queriesLockMenu = makeMenuItem(queryTreeLocked ? "Unlock Query Tree" : "Lock Query Tree");
		queriesLockMenu.addActionListener(this);
		queriesMM.addMenuItem(queriesLockMenu, 1);

		// Save and Save As for Queries
		queriesSaveMenu = makeMenuItem("Save");
		queriesSaveMenu.addActionListener(this);
		queriesSaveMenu.setEnabled(!queryTreeLocked);
		queriesMM.addMenuItem(queriesSaveMenu, 2);

		queriesSaveAsMenu = makeMenuItem("Save As");
		queriesSaveAsMenu.addActionListener(this);
		queriesSaveAsMenu.setEnabled(!queryTreeLocked);
		queriesMM.addMenuItem(queriesSaveAsMenu, 3);

		// Separator, then Undo/Redo
		JMenuItem undoItem = InterfaceMain.getInstance().getUndoMenu();
		JMenuItem redoItem = InterfaceMain.getInstance().getRedoMenu();
		// Explicitly remove Undo/Redo from any existing parent before re-adding to avoid duplicates
		java.awt.Container undoParent = undoItem.getParent();
		if (undoParent != null) {
			if (undoParent instanceof javax.swing.JPopupMenu) {
				((javax.swing.JPopupMenu) undoParent).remove(undoItem);
			} else {
				undoParent.remove(undoItem);
			}
		}
		java.awt.Container redoParent = redoItem.getParent();
		if (redoParent != null) {
			if (redoParent instanceof javax.swing.JPopupMenu) {
				((javax.swing.JPopupMenu) redoParent).remove(redoItem);
			} else {
				redoParent.remove(redoItem);
			}
		}
		// Ensure Undo/Redo are placed after Save, Save As and the first separator
		queriesMM.addSeparator(4);
		queriesMM.addMenuItem(undoItem, 5);
		queriesMM.addMenuItem(redoItem, 6);

		// Separator, then Update/Create/Edit/Remove
		queriesMM.addSeparator(7);
		queriesUpdateMenu = makeMenuItem("Update Single Query");
		queriesUpdateMenu.addActionListener(this);
		queriesUpdateMenu.setEnabled(false);
		queriesMM.addMenuItem(queriesUpdateMenu, 8);

		queriesCreateMenu = makeMenuItem("Create");
		queriesCreateMenu.addActionListener(this);
		queriesCreateMenu.setEnabled(false);
		queriesMM.addMenuItem(queriesCreateMenu, 9);

		queriesEditMenu = makeMenuItem("Edit");
		queriesEditMenu.addActionListener(this);
		queriesEditMenu.setEnabled(false);
		queriesMM.addMenuItem(queriesEditMenu, 10);

		queriesRemoveMenu = makeMenuItem("Remove");
		queriesRemoveMenu.addActionListener(this);
		queriesRemoveMenu.setEnabled(false);
		queriesMM.addMenuItem(queriesRemoveMenu, 11);

		// Favorites submenu is created by InterfaceMain; DbViewer only wires actions.
		InterfaceMain.MenuManager favoritesMM = editMM.getSubMenuManager(InterfaceMain.EDIT_FAVORITES_SUBMENU_POS);
		loadFavoritesMenu = makeMenuItem("Load Favorite Queries File");
		createFavoritesMenu = makeMenuItem("Create Favorite Queries File");
		appendFavoritesMenu = makeMenuItem("Append Favorite Queries File");
		if (favoritesMM != null) {
			favoritesMM.addMenuItem(loadFavoritesMenu, 0);
			favoritesMM.addMenuItem(createFavoritesMenu, 1);
			favoritesMM.addMenuItem(appendFavoritesMenu, 2);
		}
	}	
	
	/**
	 * Handles action events from menu items and buttons.
	 *
	 * @param e The ActionEvent that occurred.
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		switch (command) {
		case "Open DB":
			handleOpenDB(e);
			break;
		case "Manage DB":
			manageDB();
			break;
		case "Enable Beta Features":
			handleEnableBetaFeatures();
			break;
		case "Disable Beta Features":
			handleDisableBetaFeatures();
			break;
		case "Batch Query File":
			handleBatchQueryFile();
			break;
		case "Export Tabs as CSVs":
			exportTabs();
			break;
		case "Lock Query Tree":
			handleLockQueryTree();
			break;
		case "Unlock Query Tree":
			handleUnlockQueryTree();
			break;
		case "Save":
			writeQueries();
			break;
		case "Save As":
			handleSaveAs();
			break;
		case "Disable Significant Digits":
			handleDisableSigDigits();
			break;
		case "Enable Significant Digits":
			handleEnableSigDigits();
			break;
		case "Disable Unit Conversions":
			handleDisableUnitConversions();
			break;
		case "Enable Unit Conversions":
			handleEnableUnitConversions();
			break;
		default:
			// No action
			break;
	}
	}
	/**
	 * Handles the "Open DB" action event. Prompts the user to select a database
	 * directory and opens it.
	 *
	 * @param e The ActionEvent that occurred.
	 */
	private void handleOpenDB(ActionEvent e) {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		File[] dbFiles;
		if (e.getSource() instanceof RecentFile) {
			dbFiles = ((RecentFile) e.getSource()).getFiles();
		} else {
			final FileFilter dbFilter = new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory();
				}

				public String getDescription() {
					return "Directory for a BaseX DB";
				}
			};
			FileChooser fc = FileChooserFactory.getFileChooser();
			dbFiles = fc.doFilePrompt(parentFrame, "Choose BaseX Database", FileChooser.LOAD_DIALOG,
					new File(main.getProperties().getProperty("lastDirectory", ".")), dbFilter, this, "Open DB");
		}
		if (dbFiles != null) {
			main.fireControlChange(controlStr);
			File dbFile = dbFiles[0];
			boolean create = false;
			if (!dbFile.exists()) {
				int response = main.showOptionDialog(
						"The database '" + dbFile.getAbsolutePath() + "' does not exist. Would you like to create it?",
						"Create Database?",
						new Object[] { "Create", "Cancel" },
						JOptionPane.QUESTION_MESSAGE,
						"Create",
						JOptionPane.CANCEL_OPTION);
				if (response == JOptionPane.OK_OPTION) {
					create = true;
				} else {
					// User chose not to create, so abort
					main.fireControlChange("ModelInterface"); // Go back
					return;
				}
			}
			doOpenDB(dbFile, create);
		}
	}

	/**
	 * Exports all open tabs to CSV files.
	 */
	public void exportTabs() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		FileChooser fc = FileChooserFactory.getFileChooser();
		final FileFilter csvFilter = new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
			}

			public String getDescription() {
				return "CSV File (*.csv)";
			}
		};
		File[] csvFiles = fc.doFilePrompt(parentFrame, "Export Tabs as CSVs", FileChooser.SAVE_DIALOG,
				new File(main.getProperties().getProperty("lastDirectory", ".")), csvFilter);
		if (csvFiles == null) {
			return;
		} else {
			main.getProperties().setProperty("lastDirectory", csvFiles[0].getParent());
			for (int i = 0; i < tablesTabs.getTabCount(); ++i) {
				File file = new File(csvFiles[0].getParentFile(),
						tablesTabs.getTitleAt(i).replaceAll("[^a-zA-Z0-9.-]", "_") + ".csv");
				try {
					PrintWriter pw = new PrintWriter(file);
					BaseTableModel btm = getTableModelFromComponent(tablesTabs.getComponentAt(i));
					if (btm != null) {
						btm.print(pw);
					}
					pw.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	/**
	 * Handles the "Enable Beta Features" action. Enables beta features like mapping
	 * and Sankey diagrams and updates the menu item text.
	 */
	private void handleEnableBetaFeatures() {
		betaMn.setText("Disable Beta Features");
		InterfaceMain.enableMapping = true;
		InterfaceMain.enableSankey = true;
		Properties prop = InterfaceMain.getInstance().getProperties();
		prop.setProperty("enableMapping", String.valueOf(InterfaceMain.enableMapping));
		prop.setProperty("enableSankey", String.valueOf(InterfaceMain.enableSankey));
	}

	/**
	 * Handles the "Disable Beta Features" action. Disables beta features and
	 * updates the menu item text.
	 */
	private void handleDisableBetaFeatures() {
		betaMn.setText("Enable Beta Features");
		InterfaceMain.enableMapping = false;
		InterfaceMain.enableSankey = false;
		Properties prop = InterfaceMain.getInstance().getProperties();
		prop.setProperty("enableMapping", String.valueOf(InterfaceMain.enableMapping));
		prop.setProperty("enableSankey", String.valueOf(InterfaceMain.enableSankey));
	}

	/**
	 * Handles the "Batch Query File" action. Prompts the user to select a batch
	 * query file and an output file, then starts the batch query process.
	 */
	private void handleBatchQueryFile() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		FileChooser fc = FileChooserFactory.getFileChooser();
		final FileFilter xmlFilter = new XMLFilter();
		File[] batchFiles = fc.doFilePrompt(parentFrame, "Open batch Query File", FileChooser.LOAD_DIALOG,
				new File(main.getProperties().getProperty("lastDirectory", ".")), xmlFilter);
		if (batchFiles == null) {
			return;
		} else {
			main.getProperties().setProperty("lastDirectory", batchFiles[0].getParent());
			final FileFilter xlsFilter = new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					return f.getName().toLowerCase().endsWith(".xls") || f.getName().toLowerCase().endsWith(".csv")
							|| f.isDirectory();
				}

				public String getDescription() {
					return "Microsoft Excel File(*.xls) or CSV (*.csv)";
				}
			};
			File[] xlsFiles = fc.doFilePrompt(parentFrame, "Select Where to Save Output", FileChooser.SAVE_DIALOG,
					new File(main.getProperties().getProperty("lastDirectory", ".")), xlsFilter);
			if (xlsFiles == null) {
				return;
			} else {
				for (int i = 0; i < xlsFiles.length; ++i) {
					if (!xlsFiles[i].getName().endsWith(".xls") && !xlsFiles[i].getName().endsWith(".csv")) {
						xlsFiles[i] = new File(xlsFiles[i].getAbsolutePath() + ".xls");
					}
				}
				main.getProperties().setProperty("lastDirectory", xlsFiles[0].getParent());
				batchExecutionController.batchQuery(batchFiles[0], xlsFiles[0]);
			}
		}
	}

	/**
	 * Handles the "Lock Query Tree" action. Disables UI components for editing
	 * queries and updates the menu item text.
	 */
	private void handleLockQueryTree() {
		queriesUpdateMenu.setEnabled(false);
		queriesEditMenu.setEnabled(false);
		queriesCreateMenu.setEnabled(false);
		queriesRemoveMenu.setEnabled(false);
		InterfaceMain main = InterfaceMain.getInstance();
		main.getSaveMenu().setEnabled(false);
		main.getSaveAsMenu().setEnabled(false);
		// Disable Queries menu Save items as well
		if (queriesSaveMenu != null) queriesSaveMenu.setEnabled(false);
		if (queriesSaveAsMenu != null) queriesSaveAsMenu.setEnabled(false);
		main.getUndoMenu().setEnabled(false);
		main.getRedoMenu().setEnabled(false);
		queryTreeLocked = true;
		queriesLockMenu.setText("Unlock Query Tree");
	}

	/**
	 * Handles the "Unlock Query Tree" action. Enables UI components for editing
	 * queries and updates the menu item text.
	 */
	private void handleUnlockQueryTree() {
		queriesUpdateMenu.setEnabled(true);
		queriesEditMenu.setEnabled(true);
		queriesCreateMenu.setEnabled(true);
		queriesRemoveMenu.setEnabled(true);
		queryTreeLocked = false;
		queriesLockMenu.setText("Lock Query Tree");
		// Enable Queries menu Save items when unlocked
		if (queriesSaveMenu != null) queriesSaveMenu.setEnabled(true);
		if (queriesSaveAsMenu != null) queriesSaveAsMenu.setEnabled(true);
	}

	/**
	 * Handles the "Save As" action for queries. Prompts the user for a new file
	 * location and saves the current queries to that file.
	 */
	private void handleSaveAs() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		final FileFilter xmlFilter = new XMLFilter();
		FileChooser fc = FileChooserFactory.getFileChooser();
		File[] result = fc.doFilePrompt(parentFrame, null, FileChooser.SAVE_DIALOG,
				new File(main.getProperties().getProperty("queryFile", ".")), xmlFilter);
		if (result != null) {
			File file = result[0];
			if (file.getName().indexOf('.') == -1) {
				if (!(file.getAbsolutePath().endsWith(".xml"))) {
					file = new File(file.getAbsolutePath() + ".xml");
				}
			}
			if (!file.exists() || InterfaceMain.getInstance().showConfirmDialog("Overwrite existing file?",
					"Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					JOptionPane.YES_OPTION) == JOptionPane.YES_OPTION) {
				main.getProperties().setProperty("queryFile", file.getAbsolutePath());
				writeQueries();
			}
		}
	}

	/**
	 * Handles the "Disable Significant Digits" action. Updates the menu item and
	 * sets the flag to disable formatting to specific number of significant digits.
	 */
	private void handleDisableSigDigits() {
		significantDigitsMenu.setText("Enable Significant Digits");
		disableSigDigits = true;
	}

	/**
	 * Handles the "Enable Significant Digits" action. Updates the menu item and
	 * sets the flag to enable formatting to 3 significant digits.
	 */
	private void handleEnableSigDigits() {
		significantDigitsMenu.setText("Disable Significant Digits");
		disableSigDigits = false;
	}

	/**
	 * Handles the "Disable Unit Conversions" action. Updates the menu item and sets
	 * the flag to disable unit conversions.
	 */
	private void handleDisableUnitConversions() {
		enableUnitConversionsMenu.setText("Enable Unit Conversions");
		enableUnitConversions = false;
	}

	/**
	 * Handles the "Enable Unit Conversions" action. Updates the menu item and sets
	 * the flag to enable unit conversions.
	 */
	private void handleEnableUnitConversions() {
		enableUnitConversionsMenu.setText("Enable Unit Conversions");
		enableUnitConversions = true;
		// TODO: add method to enable unit conversions
	}

	/**
	 * Opens the database from the specified file.
	 * 
	 * @param dbFile The database file to open.
	 * @param create Whether to create the database if it doesn't exist.
	 */
	public void doOpenDB(File dbFile, boolean create) {
		final long openStart = System.nanoTime();
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		invalidateQueriesDocument("doOpenDB-begin");
		setStartupState(StartupLifecycleState.OPENING_DB);
		if (dbFile.getParent() != null) {
			main.setProperty("lastDirectory", dbFile.getParent());
		}
		showLoadingShell();
		updateStartupMessage(STARTUP_MESSAGE_OPENING_DB);
		parentFrame.getGlassPane().setVisible(true);
		try {
			prepareQueryDefinitionsForStartup();
			XMLDB.openDatabase(dbFile.getAbsolutePath(), create);
			logStartup("doOpenDB:XMLDB.openDatabase", openStart);
		} catch (Exception e) {
			invalidateQueriesDocument("doOpenDB-open-failed");
			setStartupState(StartupLifecycleState.FAILED);
			if (XMLDB.isSuppressedBaseXResourceException(e)) {
				System.err.println("Suppressing BaseX packaged-resource stack trace during DB open: " + e.getMessage());
			} else {
				e.printStackTrace();
			}
			parentFrame.getGlassPane().setVisible(false);
			main.hideStartupLoadingView();
			InterfaceMain.getInstance().showMessageDialog(buildOpenFailureMessage(e, dbFile), OPEN_DB_DIALOG_TITLE,
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		tablesTabs.setTransferHandler(new TableTransferHandler());
		TabDragListener dragListener = new TabDragListener();
		tablesTabs.addMouseListener(dragListener);
		tablesTabs.addMouseMotionListener(dragListener);
		logStartup("doOpenDB:table tabs initialized", openStart);

		updateStartupMessage(STARTUP_MESSAGE_LOADING_SCENARIOS);
		logStartup("doOpenDB:loadingShell", openStart);
		setStartupState(StartupLifecycleState.LOADING_DATA);
		startBackgroundInitialization(openStart, dbFile);
	}

	private void startBackgroundInitialization(final long openStart, final File dbFile) {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		startupLoader = new javax.swing.SwingWorker<StartupData, Void>() {
			@Override
			protected StartupData doInBackground() {
				final long loadStart = System.nanoTime();
				updateStartupMessage(STARTUP_MESSAGE_LOADING_SCENARIOS);
				Vector<ScenarioListItem> loadedScenarios = getScenarios();
				updateStartupMessage(STARTUP_MESSAGE_LOADING_REGIONS);
				Vector loadedRegions = getRegions();
				updateStartupMessage(STARTUP_MESSAGE_LOADING_QUERIES);
				validateQueriesDocument();
				QueryTreeModel loadedQueries = getQueries();
				InterfaceMain.logStartupTiming("DbViewer:backgroundLoad:modelData " + elapsedMillis(loadStart) + " ms");
				return validateStartupData(new StartupData(loadedScenarios, loadedRegions, loadedQueries));
			}

			@Override
			protected void done() {
				try {
					if (isCancelled() || dbViewInitialized) {
						if (isCancelled()) {
							invalidateQueriesDocument("startupLoader-cancelled");
						}
						setStartupState(isCancelled() ? StartupLifecycleState.FAILED : startupState);
						main.hideStartupLoadingView();
						return;
					}
					setStartupState(StartupLifecycleState.BUILDING_UI);
					updateStartupMessage(STARTUP_MESSAGE_BUILDING_LISTS);
					StartupData data = validateStartupData(get());
					createTableSelector(data);
					logStartup("doOpenDB:createTableSelector", openStart);
					parentFrame.setTitle("GLIMPSE-CE ModelInterface");
					main.setProperty("paramPath", dbFile.getAbsolutePath());
					main.updateActiveDatabaseStatus(dbFile.getAbsolutePath());
					setStartupState(StartupLifecycleState.READY);
					logStartup("doOpenDB:complete", openStart);
				} catch (Exception e) {
					invalidateQueriesDocument("startupLoader-done-failed");
					setStartupState(StartupLifecycleState.FAILED);
					Throwable root = unwrapFailure(e);
					if (XMLDB.isSuppressedBaseXResourceException(root)) {
						System.err.println("Suppressing BaseX packaged-resource stack trace during startup load: " + root.getMessage());
					} else {
						e.printStackTrace();
					}
					parentFrame.getGlassPane().setVisible(false);
					main.hideStartupLoadingView();
					InterfaceMain.getInstance().showMessageDialog(buildStartupFailureMessage(e),
							buildStartupFailureTitle(e), JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		startupLoader.execute();
	}
	/**
	 * Gets the list of scenarios from the database.
	 * 
	 * @return Vector of ScenarioListItem objects.
	 */
	public static Vector<ScenarioListItem> getScenarios() {
		return getScenarios(XMLDB.getInstance());
	}

	/**
	 * Gets the list of scenarios from the specified XMLDB instance.
	 * 
	 * @param xmldb The XMLDB instance.
	 * @return Vector of ScenarioListItem objects.
	 */
	public static Vector<ScenarioListItem> getScenarios(XMLDB xmldb) {
		Vector<ScenarioListItem> ret = new Vector<ScenarioListItem>();
		QueryProcessor queryProc = xmldb.createQuery("/scenario", null, null, null);
		try {
			Iter res = queryProc.iter();
			ANode temp;
			while ((temp = (ANode) res.next()) != null) {
				BXNode tempNode = BXNode.get(temp);
				BXDoc doc = new BXDoc(temp.parent());
				String docName = "";
				try {
					docName = new File(new URI(doc.getDocumentURI())).getName();

				} catch (Exception e) {
					e.printStackTrace();
				}
				Map<String, String> scnAttrMap = XMLDB.getAttrMap(tempNode);
				ret.add(new ScenarioListItem(docName, scnAttrMap.get("name"), scnAttrMap.get("date")));
			}
		} catch (Exception e) {
			System.out.println("Could not load database: " + e.toString());
			JOptionPane.showMessageDialog(null,
					"Could not load selected database, probably due to file corruption. Please review console for futher details.  All loading will stop.");
			return null;
		} finally {
			queryProc.close();
		}
		return ret;
	}

	/**
	 * Resets the scenario list in the UI.
	 */
	public void resetScenarioList() {
		scns = getScenarios();
		scnList.setListData(scns);
	}

	/**
	 * Gets the list of regions from the database.
	 * <p>
	 * This method queries the XMLDB instance for region names using the
	 * regionQueryPortion. It returns a Vector of region names, including "Global"
	 * at the end.
	 *
	 * @return Vector of region names.
	 */
	public Vector getRegions() {

		Vector funcTemp = new Vector<String>(1, 0);
		funcTemp.add("distinct-values");
		Vector ret = new Vector();
		QueryProcessor queryProc = XMLDB.getInstance().createQuery(
				"/scenario/world/" + ModelInterface.ModelGUI2.queries.QueryBuilder.regionQueryPortion + "/@name",
				funcTemp, null, null);
		try {
			Iter res = queryProc.iter();
			Item temp;
			while ((temp = res.next()) != null) {
				ret.add(temp.toJava());
			}
		} catch (QueryException e) {
			System.out.println("Error loading regions: " + e.toString());
			return null;
		} finally {
			queryProc.close();
		}
		ret.add("Global");
		return ret;
	}

	/**
	 * Gets the queries tree model from the loaded queries document.
	 * <p>
	 * This method constructs a QueryTreeModel from the root element of the
	 * queriesDoc Document.
	 *
	 * @return QueryTreeModel object representing the queries tree.
	 */
	protected QueryTreeModel getQueries() {
		validateQueriesDocument();
		try {
			return new QueryTreeModel(queriesDoc.getDocumentElement());
		} catch (RuntimeException rte) {
			throw new IllegalStateException("Query definitions could not be converted into a query tree.", rte);
		}
	}

	JTree queryList = null;

	private JButton queryFilterButton;
	private JButton favoriteQueryButton;
	private JButton runQueryButton;
	private JButton diffQueryButton;
	private JButton listCollapseButton; 

	private JCheckBox doTotalCheckBox;

	/**
	 * Sets up the main UI components for the DbViewer, including scenario/region lists,
	 * the query tree, and result tabs. This method orchestrates the creation of the
	 * entire user interface by calling various setup helper methods.
	 */
	protected void createTableSelector() {
		if (dbViewInitialized) {
			return;
		}
		setupScenarioRegionLists();
		createTableSelector(new StartupData(scns, regions, queries));
	}

	private void createTableSelector(StartupData data) {
		if (dbViewInitialized) {
			return;
		}
		final long selectorStart = System.nanoTime();
		updateStartupMessage(STARTUP_MESSAGE_BUILDING_LISTS);
		applyStartupData(data);
		logStartup("createTableSelector:scenarioRegionLists", selectorStart);
		updateStartupMessage(STARTUP_MESSAGE_BUILDING_TREE);
		setupQueryTree();
		logStartup("createTableSelector:queryTree", selectorStart);
		updateStartupMessage(STARTUP_MESSAGE_LAYOUT);
		setupSplitPanes();
		logStartup("createTableSelector:splitPanes", selectorStart);
		updateStartupMessage(STARTUP_MESSAGE_BUILDING_QUERY_PANEL);
		setupQueryPanel();
		logStartup("createTableSelector:queryPanel", selectorStart);
		updateStartupMessage(STARTUP_MESSAGE_BUILDING_ACTIONS);
		setupButtonPanel();
		logStartup("createTableSelector:buttonPanel", selectorStart);
		installResultsTabCompletionTracking();
		logStartup("createTableSelector:tabTracking", selectorStart);
		setupPresetRegionDropdown();
		logStartup("createTableSelector:presetRegionDropdown", selectorStart);
		favoriteQueriesManager = new FavoriteQueriesManager(queryList, listScrollQueries);
		logStartup("createTableSelector:favoriteQueriesManager", selectorStart);
		setupListeners();
		logStartup("createTableSelector:listeners", selectorStart);
		updateStartupMessage(STARTUP_MESSAGE_FINISHING);
		finalizeUI();
		dbViewInitialized = true;
		logStartup("createTableSelector:finalizeUI", selectorStart);
	}

	private void ensureSplitPanesInitialized() {
		if (scenarioRegionSplit != null && queriesSplit != null && tableCreatorSplit != null) {
			return;
		}
		Properties prop = InterfaceMain.getInstance().getProperties();
		if (scenarioRegionSplit == null) {
			scenarioRegionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			scenarioRegionSplit.setResizeWeight(.5);
		}
		if (queriesSplit == null) {
			queriesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			queriesSplit.setResizeWeight(.5);
		}
		if (tableCreatorSplit == null) {
			tableCreatorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
		}
		if (prop != null) {
			String tempInt;
			try {
				if ((tempInt = prop.getProperty("scenarioRegionSplit")) != null) {
					scenarioRegionSplit.setDividerLocation(Integer.valueOf(tempInt));
				}
				if ((tempInt = prop.getProperty("queriesSplit")) != null) {
					queriesSplit.setDividerLocation(Integer.valueOf(tempInt));
				}
				if ((tempInt = prop.getProperty("tableCreatorSplit")) != null) {
					tableCreatorSplit.setDividerLocation(Integer.valueOf(tempInt));
				}
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid split location preference: " + nfe);
			}
		}
	}
	
	private void applyStartupData(StartupData data) {
		validateStartupData(data);
		scns = data.scenarios;
		regions = data.regions;
		queries = data.queries;
		scnList = new JList(scns);
		scnList.setName(SCENARIO_LIST_NAME);
		regionList = new JList(regions);
		regionList.setName(REGION_LIST_NAME);
	}

	/**
	 * Initializes the scenario and region lists by fetching data from the database
	 * and creating the JList components.
	 */
	private void setupScenarioRegionLists() {
		scns = getScenarios();
		regions = getRegions();
		queries = getQueries();
		scnList = new JList(scns);
		scnList.setName(SCENARIO_LIST_NAME);
		regionList = new JList(regions);
		regionList.setName(REGION_LIST_NAME);
	}

	/**
	 * Initializes the query tree (JTree) component, including its model, transfer
	 * handler for drag-and-drop, selection model, and cell renderer for custom
	 * icons and tooltips.
	 */
	private void setupQueryTree() {
		if (queries == null) {
			throw new IllegalStateException("Query tree model was not initialized before building the query tree.");
		}
		final Icon queryIcon = loadQueryTreeIcon("icons/group-query.png", "query group");
		final Icon singleQueryIcon = loadQueryTreeIcon("icons/single-query.png", "single query");
		queryList = new JTree(queries);
		queryList.setTransferHandler(new QueryTransferHandler(queriesDoc, implls));
		queryList.setDragEnabled(true);
		queryList.getSelectionModel()
				.setSelectionMode(javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		if (queryList.getRowCount() > 0) {
			queryList.setSelectionRow(0);
		}
		for (int i = 0; i < queryList.getRowCount(); ++i) {
			queryList.expandRow(i);
		}
		queryList.setRowHeight(queryList.getFont().getSize() + 5);
		ToolTipManager.sharedInstance().registerComponent(queryList);
		ToolTipManager.sharedInstance().setInitialDelay(1200); // set tooltip delay
		queryList.setCellRenderer(new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof QueryGenerator) {
					setToolTipText(createCommentTooltip(new TreePath(value)));
					setIcon(queryIcon);
				} else if (value instanceof SingleQueryExtension.SingleQueryValue) {
					Object[] tp = new Object[] { "root", ((SingleQueryExtension.SingleQueryValue) value).getParent(),
							value };
					setToolTipText(createCommentTooltip(new TreePath(tp)));
					setIcon(singleQueryIcon);
				}
				return this;
			}
		});
	}

	/**
	 * Sets up the preset region dropdown (JComboBox) and the "Total" checkbox.
	 * It loads the region list from a file and adds the components to the UI.
	 */
	private void setupPresetRegionDropdown() {
		if (scenarioRegionSplit == null || !(scenarioRegionSplit.getRightComponent() instanceof JPanel)) {
			return;
		}
		loadRegionListToDropdown();
		JPanel presetRegionsPanel = new JPanel();
		presetRegionsPanel.setLayout(new BoxLayout(presetRegionsPanel, BoxLayout.X_AXIS));
		presetRegionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		presetRegionsPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		presetRegionsPanel.setPreferredSize(new Dimension(0, bottomStripHeight));
		presetRegionsPanel.setMinimumSize(new Dimension(0, bottomStripHeight));
		presetRegionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bottomStripHeight));

		// Add initial spacing
		presetRegionsPanel.add(Box.createHorizontalStrut(5));
		
		// Move doTotalCheckBox here from setupButtonPanel
		doTotalCheckBox = new JCheckBox("Total  ");
		doTotalCheckBox.setOpaque(true);
		doTotalCheckBox.setAlignmentY(Component.CENTER_ALIGNMENT);
		//doTotalCheckBox.setBackground(Color.WHITE);
		presetRegionsPanel.add(doTotalCheckBox);

		if (preset_choices != null && preset_choices.length > 0) {
			presetRegionsPanel.add(Box.createHorizontalStrut(5));
			JLabel listLabel = new JLabel("Group:");
			listLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
			comboBoxPresetRegions = new JComboBox<String>(preset_choices);
			comboBoxPresetRegions.setVisible(true);
			comboBoxPresetRegions.setMaximumSize(comboBoxPresetRegions.getPreferredSize());
			comboBoxPresetRegions.setAlignmentY(Component.CENTER_ALIGNMENT);

			presetRegionsPanel.add(listLabel);
			presetRegionsPanel.add(Box.createHorizontalStrut(5));
			presetRegionsPanel.add(comboBoxPresetRegions);
			// Add to region panel
			comboBoxPresetRegions.addActionListener(e -> selectPresetRegions());
		}
			// Add the preset panel to BorderLayout.SOUTH of the right wrapper.
		// getRightComponent() returns the rightWrapper (BorderLayout) we set in setupSplitPanes.
		((JPanel) scenarioRegionSplit.getRightComponent()).add(presetRegionsPanel, BorderLayout.SOUTH);

	}

	/**
	 * Sets up the panel containing the main action buttons like "Run Query",
	 * "Diff Query", "Search", and "Favorites". It also includes the query progress
	 * bar.
	 */
	private void setupButtonPanel() {
		if (!(queriesSplit.getRightComponent() instanceof JPanel)) {
			throw new IllegalStateException("Query panel was not initialized before building the button panel.");
		}
		JPanel queryPanel = (JPanel) queriesSplit.getRightComponent();
		JPanel buttonPanel = new JPanel();
		
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		buttonPanel.setPreferredSize(new Dimension(0, bottomStripHeight));
		buttonPanel.setMinimumSize(new Dimension(0, bottomStripHeight));
		buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bottomStripHeight));
		
		// Change to BoxLayout for proper vertical centering
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Add initial spacing
		buttonPanel.add(Box.createHorizontalStrut(2));

		runQueryButton = new JButton("Run Query");
		diffQueryButton = new JButton("Diff Query");
		listCollapseButton = new JButton("View +/-");
		queryFilterButton = new JButton("Search");
		favoriteQueryButton = new JButton("Favorites");
		
		queriesEditMenu.setEnabled(false);
		runQueryButton.setEnabled(false);
		diffQueryButton.setEnabled(false);

		// Let SwingButtonSizer + the Look & Feel determine button size; don't squash.
		JButton[] buttons = new JButton[] { runQueryButton, diffQueryButton, listCollapseButton, queryFilterButton, favoriteQueryButton };
		for (JButton b : buttons) {
			b.setAlignmentY(Component.CENTER_ALIGNMENT);
			buttonPanel.add(b);
			buttonPanel.add(Box.createHorizontalStrut(2));
		}

		// Add a 20px buffer between the Favorites button and the progress bar (if added later)
		// buttonPanel.add(Box.createHorizontalStrut(20)); // FlowLayout handles spacing
		
		// Add to SOUTH so it sticks to the bottom
		queryPanel.add(buttonPanel, BorderLayout.SOUTH);
		this.queryFilterButton = queryFilterButton;
		this.favoriteQueryButton = favoriteQueryButton;
	}

	/**
	 * Sets up the panel that contains the query tree view.
	 */
	private void setupQueryPanel() {
		scenarioRegionSplit.getRightComponent();
		// Use BorderLayout to allow button panel to sit at SOUTH and list in CENTER
		JPanel queryPanel = new JPanel(new BorderLayout());
		
		JLabel queryListLabel = new JLabel("Queries");
		queryListLabel.setHorizontalAlignment(SwingConstants.CENTER);
		queryListLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		queryPanel.add(queryListLabel, BorderLayout.NORTH);
		
		listScrollQueries = new JScrollPane(queryList);
		listScrollQueries.setPreferredSize(new Dimension(150, 100));
		queryPanel.add(listScrollQueries, BorderLayout.CENTER);
		
		queriesSplit.setRightComponent(queryPanel);
	}
	
	/**
	 * Install listeners on the results tabbed pane so when a tab is closed we count
	 * that query as completed for the status bar progress UI.
	 */
	private void installResultsTabCompletionTracking() {
		// This will be called after tablesTabs is created.
		tablesTabs.addContainerListener(new ContainerAdapter() {
			@Override
			public void componentRemoved(ContainerEvent e) {
				java.awt.Component removed = e.getChild();
				// Only track and clean up tabs we registered as query/diff results.
				if (removed != null) {
					// A tab was closed; if it was still active, count it as completed.
					if (activeQueryTabs.remove(removed)) {
						registerQueryCompleted();
					}
					// If all tabs were closed/reset, keep the internal set clean.
					if (tablesTabs.getTabCount() == 0) {
						activeQueryTabs.clear();
					}
				}
			}
		});
	}

	/**
	 * Sets up the split panes that organize the main UI areas: scenarios/regions,
	 * queries, and the results tabs.
	 */
	private void setupSplitPanes() {
		ensureSplitPanesInitialized();
		JFrame currentParentFrame = parentFrame != null ? parentFrame : InterfaceMain.getInstance().getFrame();
		if (currentParentFrame == null) {
			throw new IllegalStateException("Main frame was not available while building split panes.");
		}
		parentFrame = currentParentFrame;
		JPanel scenListPane = new JPanel();
		scenListPane.setLayout(new BoxLayout(scenListPane, BoxLayout.Y_AXIS));
		JLabel scenListLabel = new JLabel("Scenarios");
		scenListLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		scenListLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		scenListPane.add(scenListLabel);
		JScrollPane listScroll = new JScrollPane(scnList);
		scenListPane.add(listScroll);

		// Create the bottom pane separately so it appears below the listPane
		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.X_AXIS));
		bottomPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		// Keep the bottom strip height consistent across the app.
		final int bottomStripHeight = ModelInterface.common.SwingButtonSizer.STANDARD_BUTTON_HEIGHT + 6;
		bottomPane.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		bottomPane.setPreferredSize(new Dimension(0, bottomStripHeight));
		bottomPane.setMinimumSize(new Dimension(0, bottomStripHeight));
		bottomPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, bottomStripHeight));

		final JButton manageDbButton = new JButton("Manage DB");
		manageDbButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		// left-justify the button
		bottomPane.add(Box.createHorizontalStrut(10));
		bottomPane.add(manageDbButton);
		bottomPane.add(Box.createHorizontalGlue());
		manageDbButton.addActionListener(e -> manageDB());
		// Disable button unless DB is open; update on control changes like the menu
		// entry
		manageDbButton.setEnabled(XMLDB.getInstance() != null);
		final JFrame pf = InterfaceMain.getInstance().getFrame();
		pf.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("Control")) {
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						manageDbButton.setEnabled(false);
					}
					if (evt.getNewValue().equals(controlStr)) {
						manageDbButton.setEnabled(true);
					}
				}
			}
		});

		// Wrap the listPane and bottomPane so bottomPane appears below listPane
		JPanel leftWrapper = new JPanel(new BorderLayout());
		leftWrapper.add(scenListPane, BorderLayout.CENTER);
		leftWrapper.add(bottomPane, BorderLayout.SOUTH);

		scenarioRegionSplit.setLeftComponent(leftWrapper);
		scenListPane = new JPanel();
		scenListPane.setLayout(new BoxLayout(scenListPane, BoxLayout.Y_AXIS));
		scenListLabel = new JLabel("Regions");
		scenListLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		scenListLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		scenListPane.add(scenListLabel);
		listScrollRegions = new JScrollPane(regionList);
		scenListPane.add(listScrollRegions);
		// Wrap listPane in a BorderLayout panel (rightWrapper) and put the listPane 
		// in the CENTER so we can put preset panel in SOUTH later.
		JPanel rightWrapper = new JPanel(new BorderLayout());
		rightWrapper.add(scenListPane, BorderLayout.CENTER);
		scenarioRegionSplit.setRightComponent(rightWrapper);
		
		queriesSplit.setLeftComponent(scenarioRegionSplit);
		tableCreatorSplit.setLeftComponent(queriesSplit);
		tableCreatorSplit.setRightComponent(tablesTabs);

		int frameWidth = currentParentFrame.getWidth();
		scenarioRegionSplit.setDividerLocation((int) (frameWidth * 0.2));
		queriesSplit.setDividerLocation((int) (frameWidth * 0.5));

		int frameHeight = currentParentFrame.getHeight();
		tableCreatorSplit.setDividerLocation((int) (frameHeight * 0.4));
	}
	
	/**
	 * Sets up all the event listeners for the UI components, such as buttons and
	 * the query list. This includes actions for running queries, filtering,
	 * managing favorites, and handling tree selections.
	 */
	private void setupListeners() {
		if (queryList == null || scnList == null || regionList == null || runQueryButton == null || diffQueryButton == null
				|| queryFilterButton == null || favoriteQueryButton == null || favoriteQueriesManager == null) {
			return;
		}
		// Move all listeners from createTableSelector here
		// YD edits,2024
		queryFilterButton.addActionListener(new ActionListener() { // YD,2024
			public void actionPerformed(ActionEvent e) {

				JPanel box = new JPanel();
				box.setPreferredSize(new Dimension(400, 50));
				box.setBackground(Color.GRAY);
				box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
				JLabel keyLabel = new JLabel("Please type the filtering text here:");
				keyLabel.setFont(new Font("Arial", Font.BOLD, 16));
				keyLabel.setForeground(Color.white);
				JTextField field = new JTextField(20);
				box.add(keyLabel);
				box.add(field);

				String[] buttons = { "Apply", "Clear", "Cancel" };
				int returnValue = JOptionPane.showOptionDialog(null, box, "Query Filter",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[0]);
				queries = getQueries();
				switch (returnValue) {
				case 0:
					// Process the filter results...
					filteringText = field.getText();
					if (filteringText != null && filteringText.length() > 0) {
						queries = getFilteredQueries(queries, filteringText);

					}

					break;
				case 2:
				case -1:
					return;
				}
				queryList.setModel(queries);
				queryList.setSelectionRow(0);
				for (int i = 0; i < queryList.getRowCount(); ++i) {
					queryList.expandRow(i);
				}
			}
		});

		favoriteQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				favoriteQueriesManager.selectFavoriteQueries();
			}
		});

		createFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				favoriteQueriesManager.createFavoriteQueriesFile();

			}
		});

		loadFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				favoriteQueriesManager.loadFavoriteQueriesFile();

			}
		});

		appendFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				favoriteQueriesManager.appendFavoriteQueries();

			}
		});

		// Add TreeSelectionListener to enable/disable Run Query and Diff Query buttons
		queryList.addTreeSelectionListener(e -> {
			boolean hasQuerySelected = false;
			TreePath[] selectedPaths = queryList.getSelectionPaths();
			if (selectedPaths != null) {
				for (TreePath tp : selectedPaths) {
					if (tp.getLastPathComponent() instanceof QueryGenerator) {
						hasQuerySelected = true;
						break;
					}
				}
			}
			if (runQueryButton != null)
				runQueryButton.setEnabled(hasQuerySelected);
			if (diffQueryButton != null)
				diffQueryButton.setEnabled(hasQuerySelected);
		});

		runQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// read the file everytime, allows for dynamic changing of units.
				int[] scnSel = scnList.getSelectedIndices();
				int[] regionSel = regionList.getSelectedIndices();
				// Checks to make sure at least one scenario, region, and query has been
				// selected
				if (scnSel.length == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select Scenarios to run the query against",
							"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if (regionSel.length == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select Regions to run the query against",
							"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if (queryList.getSelectionCount() == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select a query to run", "Run Query Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					// selection criteria met
					// identifies path in query file to the selected query(ies)
					TreePath[] selPaths = queryList.getSelectionPaths();
					boolean movedTabAlready = false;
					// Force a refresh of the selected years list before running queries
					refreshSelectedYearListFromPropFile();
					// iterates over selected queries by path
					for (int i = 0; i < selPaths.length; ++i) {
						try {
							Object lastComp = selPaths[i].getLastPathComponent();
							// Skip QueryGroups (folders)
							if (lastComp.getClass().getName().contains("QueryGroup")) {
								continue;
							}
							
							QueryGenerator qg = null;
							QueryBinding singleBinding = null;
							if (lastComp instanceof QueryGenerator) {
								qg = (QueryGenerator) lastComp;
							} else {
								singleBinding = ((SingleQueryExtension.SingleQueryValue) lastComp)
										.getAsQueryBinding();
								qg = (QueryGenerator) selPaths[i].getParentPath().getLastPathComponent();
							}
							// add loading icon to QueryResultsPanel
							TabCloseIcon loadingIcon = new TabCloseIcon(tablesTabs);
							// creating new panel for holding the results of the queries
							JComponent ret = new QueryResultsPanel(qg, singleBinding, scnList.getSelectedValues(),
									regionList.getSelectedValues(), loadingIcon, doTotalCheckBox.isSelected());
							// Register that a new query is starting so the progress UI can track it
							registerNewQuery(ret);

							// adds new tab for query results panel
							tablesTabs.addTab(qg.toString(), loadingIcon, ret, createCommentTooltip(selPaths[i]));
							if (!movedTabAlready) {
								tablesTabs.setSelectedIndex(tablesTabs.getTabCount() - 1);
								movedTabAlready = true;
							}

						} catch (ClassCastException cce) {
							System.out.println("Warning: Caught " + cce + " likely a QueryGroup was in the selection");
						}
					}
					// need old value/new value?
					// fire off property or something we did query
				}
			}

		});

		diffQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] scnSel = scnList.getSelectedIndices();
				int[] regionSel = regionList.getSelectedIndices();
				// Checks to make sure at least one scenaro, region, and query has been selected
				if (scnSel.length == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select Scenarios to run the query against",
							"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if (regionSel.length == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select Regions to run the query against",
							"Run Query Error", JOptionPane.ERROR_MESSAGE);
				} else if (queryList.getSelectionCount() == 0) {
					InterfaceMain.getInstance().showMessageDialog("Please select a query to run", "Run Query Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					// selection criteria met
					// identifies path in query file to the selected query(ies)
					TreePath[] selPaths = queryList.getSelectionPaths();
					boolean movedTabAlready = false;
					// Force a refresh of the selected years list before running queries
					refreshSelectedYearListFromPropFile();
					// iterates over selected queries by path
					for (int i = 0; i < selPaths.length; ++i) {
						try {
							Object lastComp = selPaths[i].getLastPathComponent();
							// Skip QueryGroups (folders)
							if (lastComp.getClass().getName().contains("QueryGroup")) {
								continue;
							}

							QueryGenerator qg = null;
							QueryBinding singleBinding = null;
							if (lastComp instanceof QueryGenerator) {
								qg = (QueryGenerator) lastComp;
							} else {
								singleBinding = ((SingleQueryExtension.SingleQueryValue) lastComp).getAsQueryBinding();
								qg = (QueryGenerator) selPaths[i].getParentPath().getLastPathComponent();
							}
							// add loading icon to QueryResultsPanel
							TabCloseIcon loadingIcon = new TabCloseIcon(tablesTabs);
							// creating new panel for holding the results of the queries
							JComponent ret = new DiffResultsPanel(qg, singleBinding, scnList.getSelectedValues(),
									regionList.getSelectedValues(), loadingIcon, doTotalCheckBox.isSelected());
							// Register that a new diff query is starting so the progress UI can track it
							registerNewQuery(ret);
							// adds new tab for query results panel
							tablesTabs.addTab("Diff: " + qg.toString(), loadingIcon, ret,
									createCommentTooltip(selPaths[i]));
							if (!movedTabAlready) {
								tablesTabs.setSelectedIndex(tablesTabs.getTabCount() - 1);
								movedTabAlready = true;
							}

						} catch (ClassCastException cce) {
							System.out.println("Warning: Caught " + cce + " likely a QueryGroup was in the selection");
						}
					}
					// need old value/new value?
					// fire off property or something we did query
				}
			}
		});

		listCollapseButton.addActionListener(new ActionListener() {
            private boolean expanded = true;
            public void actionPerformed(ActionEvent e) {
                if (expanded) {
                    // Collapse all except root
                    for (int i = queryList.getRowCount() - 1; i > 0; i--) {
                        queryList.collapseRow(i);
                    }
                } else {
                    // Expand all
                    for (int i = 0; i < queryList.getRowCount(); i++) {
                        queryList.expandRow(i);
                    }
                }
                expanded = !expanded;
            }
        }); // listener end
		
		
		JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		parentFrame.getContentPane();
		// Use InterfaceMain.setMainView to replace the CENTER component while keeping the
		// global status bar (installed in InterfaceMain.SOUTH) intact.
		InterfaceMain.getInstance().setMainView(tableCreatorSplit);

		// have to get rid of the wait cursor
		parentFrame.getGlassPane().setVisible(false);

		parentFrame.setVisible(true);
	}

	/**
	 * Filters the query tree model to show only the queries and groups that match
	 * the given query string.
	 *
	 * @param model The query tree model to filter.
	 * @param query The text to search for.
	 * @return The filtered query tree model.
	 */
	private QueryTreeModel getFilteredQueries(QueryTreeModel model, String query) {

		QueryGroup root = (QueryGroup) model.getRoot();
		ArrayList query_list = root.getQueryList();

		ArrayList toRemove = new ArrayList<>();

		for (int i = 0; i < query_list.size(); i++) {
			if (query_list.get(i) instanceof QueryGroup) {
				QueryGroup group = (QueryGroup) query_list.get(i);
			 toRemove.addAll(getMatchingNodes(group, query));
			}
		}

		for (int i = 0; i < toRemove.size(); i++) {
			QueryGenerator qg = (QueryGenerator) toRemove.get(i);
			Node myNode = qg.getMyNode();
			qg.getMyNode().getParentNode().removeChild(myNode);
		}

		return model;
	}



	
	/**
	 * Returns a list of matching nodes in the QueryGroup that do not contain the
	 * query string.
	 * <p>
	 * This method traverses the QueryGroup and collects nodes that do not match the
	 * query string.
	 *
	 * @param groupTop The QueryGroup to search.
	 * @param query    The query string to filter by.
	 * @return ArrayList of nodes to remove.
	 */
	private ArrayList getMatchingNodes(QueryGroup groupTop, String query) {
		ArrayList query_list = groupTop.getQueryList();
		ArrayList toRemove = new ArrayList();
		for (int i = 0; i < query_list.size(); i++) {
			if (query_list.get(i) instanceof QueryGroup) {
				QueryGroup group = (QueryGroup) query_list.get(i);
				getMatchingNodes(group, query);
			} else {
				if (!query_list.get(i).toString().toLowerCase().contains(query.toLowerCase())) {
					toRemove.add(query_list.get(i));
				}
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
		 query_list.remove(toRemove.get(i));

		}
		return toRemove;
	}

	/**
	 * Saves the expansion state of a JTree.
	 *
	 * @param tree The JTree whose expansion state is to be saved.
	 * @return An Enumeration of TreePaths representing the expanded nodes.
	 */
	// YD added,2024
	public static Enumeration<TreePath> saveExpansionState(JTree tree) {
		return tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
	}

	/**
	 * Restores the expansion state of a JTree.
	 *
	 * @param enumeration An Enumeration of TreePaths to be expanded.
	 * @param tree        The JTree whose expansion state is to be restored.
	 */
	// YD added,2024
	public void restoreExpansionState(Enumeration enumeration, JTree tree) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
			 TreePath treePath = (TreePath) enumeration.nextElement();
				tree.expandPath(treePath);
			}
		}
	}

	/**
	 * Gets the full TreePath for a query group by its name.
	 *
	 * @param tree      The JTree to search in.
	 * @param groupName The name of the query group.
	 * @return The TreePath for the group, or null if not found.
	 */
	// YD added,2024
	// this method is to get the full tree path for a query group name,YD added
 private TreePath getFullTreePath(JTree tree, String groupName) {
		Enumeration<TreePath> allPath = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		TreePath myTreePath = null;
		if (allPath != null) {
			while (allPath.hasMoreElements()) {
				TreePath treePath = (TreePath) allPath.nextElement();
				String treePathStr = treePath.toString();
				String[] splitsTreePath = treePathStr.replace("[", "").replace("]", "").split(",");
				String currentGroup = splitsTreePath[splitsTreePath.length - 1];
				// handle when "," within groupName such as 'Markets, prices, and costs' first
				boolean groupNameHasComma = groupName.contains(",");
				if (groupNameHasComma && treePath.toString().contains(groupName)) {
					myTreePath = treePath;
				} else if (currentGroup.trim().equalsIgnoreCase(groupName)) {
					myTreePath = treePath;
				}
				if (myTreePath != null) {
					break;
				}
			}
		}
		return (myTreePath);
	}

	/**
	 * Gets all TreePaths for a query group name, considering that the same name can
	 * appear at different levels in the tree.
	 *
	 * @param tree      The JTree to search in.
	 * @param groupName The name of the query group.
	 * @param pathCount The expected path count (depth) of the group.
	 * @return An ArrayList of matching TreePaths.
	 */
	// this method is to get the full tree path for a query group name
	// considering that the same query group name can appear multiple times in the
// same tree
	public static ArrayList<TreePath> getFullTreePath2(JTree tree, String groupName, int pathCount) {
		Enumeration<TreePath> allPath = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		ArrayList<TreePath> myTreePath = new ArrayList<TreePath>();
		if (allPath != null) {
			while (allPath.hasMoreElements()) {
				TreePath treePath = (TreePath) allPath.nextElement();
				String treePathStr = treePath.toString();
				String[] splitsTreePath = treePathStr.replace("[", "").replace("]", "").split(",");
				String currentGroup = splitsTreePath[splitsTreePath.length - 1];
				int checkPathCount = treePath.getPathCount();
				// handle when "," within groupName such as 'Markets, prices, and costs' first
				boolean groupNameHasComma = groupName.contains(",");
				if (groupNameHasComma && treePath.toString().contains(groupName)) {
					myTreePath.add(treePath);
				} else if (checkPathCount == pathCount & currentGroup.trim().equals(groupName)) {
					System.out.println("found for Sankey diagrams group:" + treePathStr);
					myTreePath.add(treePath);
				}
			}
		}
		return (myTreePath);
	}

	/**
	 * Checks if a QueryGroup contains any nested QueryGroups.
	 *
	 * @param mySubGroup The QueryGroup to check.
	 * @return true if the group contains no other groups (only leaves), false
	 *         otherwise.
	 */
	// YD added,2024
	// this method is to check if there are more levels of query group under a query
	// group
	private boolean checkNoMoreQueryGroupInside(QueryGroup mySubGroup) {
		boolean noMoreGroup = true;
		ArrayList group_inside = mySubGroup.getQueryList();
		for (int j = 0; j < group_inside.size(); j++) {
			if (group_inside.get(j) instanceof QueryGroup) {
				noMoreGroup = false;
				break;
			}
		} // for loop end
		return (noMoreGroup);
	}

	/**
	 * Closes all open tabs in the results pane.
	 */
	public void closeAllTabs() {
		// need to disable the export tabs option
		if (menuExpPrn != null) menuExpPrn.setEnabled(false);

		if (tablesTabs.getTabCount() > 0) {
			// Treat all open query tabs as completed when user closes them.
			int toComplete;
			synchronized (activeQueryTabs) {
				toComplete = activeQueryTabs.size();
				activeQueryTabs.clear();
			}
			if (toComplete > 0) {
				completedQueries = Math.min(totalQueries, completedQueries + toComplete);
				updateProgressUI();
				if (completedQueries >= totalQueries) {
					finishProgressUI();
				}
			}
			// iterate over children and close each one
			tablesTabs.removeAll();
		}
	}

	/**
	 * Closes all open secondary windows (JDialog and JFrames) managed by the
	 * application.
	 */
	public void closeAllWindows() {
		// grab the panel
		if (openWindows.isEmpty()) {
			return;
		}
		// iterate over children
		for (Object o : openWindows) {
			// cast it
			if (o instanceof JDialog) {
				JDialog win = (JDialog) o;
				win.dispose();
			}
			if (o instanceof JFrame) {
				JFrame instance = (JFrame) o;
				instance.dispose();
			}
			// close it
		}
		// clear out list
		openWindows.clear();

	}

	/**
	 * Reads lines from a file into an ArrayList, skipping lines starting with the
	 * comment character.
	 * 
	 * @param filename    The file to read.
	 * @param commentChar The character indicating a comment line.
	 * @return ArrayList of lines from the file.
	 * @throws IOException If an I/O error occurs.
	 */
	public ArrayList<String> getStringArrayFromFile(String filename, String commentChar) throws IOException {
		ArrayList<String> arrayList = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filename));
		for (String line; (line = br.readLine()) != null;) {
			line = line.trim();
			if (line.length() > 0) {
				if (commentChar != null && !line.startsWith(commentChar)) {
					arrayList.add(line);
				}
			}
		}
		br.close();

		return arrayList;
	}

	/**
	 * Splits a string into an ArrayList using the specified delimiter.
	 * 
	 * @param line  The string to split.
	 * @param delim The delimiter.
	 * @return ArrayList of split strings.
	 */
	public ArrayList<String> createArrayListFromString(String line, String delim) {
		ArrayList<String> linesList = new ArrayList<String>();
		String[] lines = splitString(line, delim);
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i];// + vars.getEol();
			linesList.add(lines[i]);
		}
		return linesList;
	}

	/**
	 * Splits a string into an array using the specified delimiter.
	 * 
	 * @param str   The string to split.
	 * @param delim The delimiter.
	 * @return Array of split strings.
	 */
	public String[] splitString(String str, String delim) {
		String s[] = str.split(delim);
		return s;
	}

	/**
	 * Selects preset regions in the region list based on the dropdown selection.
	 */
	public void selectPresetRegions() {
		if (comboBoxPresetRegions == null || regionList == null) {
			return;
		}
		boolean verbose = false;
		String selection = (String) comboBoxPresetRegions.getSelectedItem();
		if (selection == null) {
			return;
		}
		int idx = comboBoxPresetRegions.getSelectedIndex();
		if (verbose)
			System.out.println("this is my selection: " + selection);
		if (idx > 0) {// YD added,Apr-2024
			for (int i = 0; i < preset_region_list.size(); i++) {
				String line = preset_region_list.get(i);
				int index = line.indexOf(":");
				if (index > 0) {
					String name = line.substring(0, index).toLowerCase();
					if (selection.toLowerCase().equals(name)) {
						String[] subregions = splitString(line.substring(index + 1), ",");
						if (verbose)
							System.out.println("number of items in this subregion is: " + subregions.length);
						selectItemsFromRegionList(subregions);
					}
				}
			} // for loop end
		} else { // YD added,Apr-2024
			regionList.setSelectedIndex(0);
		} // outer if else loop end
	}// selectPresetRegions method end

	/**
	 * Selects items from the region list based on the provided subregions.
	 * 
	 * @param subregions Array of subregion names to select.
	 */
	public void selectItemsFromRegionList(String[] subregions) {

		ArrayList<Integer> regionIndices = new ArrayList<Integer>();

		for (int i = 0; i < subregions.length; i++) {

			for (int j = 0; j < regions.size(); j++) {

				String st_str = subregions[i].trim();
				String regionName = regions.get(j).toString();
				if (st_str.equals(regionName)) {
					regionIndices.add(j);
				}
			}
		}

		int[] regionIndicesArray = new int[regionIndices.size()];
		for (int n = 0; n < regionIndices.size(); n++) {
			regionIndicesArray[n] = regionIndices.get(n);
		}
		// set selected sub-regions in the regionList
		if (regionIndicesArray.length > 0) {
			regionList.setSelectedIndices(regionIndicesArray);
			// scroll to the selected items
			java.awt.Rectangle bounds = regionList.getCellBounds(regionIndicesArray[0],
					regionIndicesArray[regionIndices.size() - 1]);
			listScrollRegions.getVerticalScrollBar().setValue((int) bounds.getMinY());
		}
	}

	/**
	 * Gets the row number for a leaf under a query group in the tree.
	 * 
	 * @param myTree   The JTree.
	 * @param myPath   The TreePath to the group.
	 * @param leafName The name of the leaf.
	 * @return The row number for the leaf, or -1 if not found.
	 */
	public static int getRowNumberForLeaf(JTree myTree, TreePath myPath, String leafName) {
		int rowNumForLeaf = -1;
		int rowNumForSubgroup = myTree.getRowForPath(myPath);
		QueryGroup myChildGroup = (QueryGroup) myPath.getLastPathComponent();
		ArrayList leaves = myChildGroup.getQueryList();
		for (int m = 0; m < leaves.size(); m++) {
			if (leafName.replace("\"", "").trim().compareToIgnoreCase(leaves.get(m).toString().trim()) == 0) {
				int myIndex = ((TreeModel) myTree.getModel()).getIndexOfChild(myChildGroup, leaves.get(m));
				rowNumForLeaf = rowNumForSubgroup + myIndex + 1;
				break;
			}
		}
		return rowNumForLeaf;
	}

	public void runBatch(Node command) {
		batchExecutionController.runBatch(command);
	}

	/**
	 * Loads the preset region list from a file and populates the dropdown choices.
	 * It reads from "config/preset_region_list.txt", parsing region groups and
	 * their sub-regions.
	 */
	private void loadRegionListToDropdown() {
		String region_list_file = "config/preset_region_list.txt";
		try {
			preset_region_list = getStringArrayFromFile(region_list_file, "#");
			if (preset_region_list.size() > 0) {
				preset_choices = new String[preset_region_list.size() + 1];
				preset_choices[0] = "(optional)";
				for (int i = 0; i < preset_region_list.size(); i++) {
					String line = preset_region_list.get(i);
					int index = line.indexOf(":");
					if (index > 0) {
						String name = line.substring(0, index);
						preset_choices[i + 1] = name;
						String[] subregions = splitString(line.substring(index + 1), ",");
						for (int j = 0; j < subregions.length; j++) {
							subregion_list.add(subregions[j]);
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Not able to read preset_region_list file: " + region_list_file);
		}
	}

	/**
	 * Finalizes the UI setup by adding the main component to the parent frame and
	 * making it visible.
	 */
	private void finalizeUI() {
		JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		if (parentFrame == null)
			return;
		parentFrame.getContentPane();
		InterfaceMain.getInstance().setMainView(tableCreatorSplit);
		InterfaceMain.getInstance().hideStartupLoadingView();
		parentFrame.setLocationRelativeTo(null);
		parentFrame.setVisible(true);
		parentFrame.getGlassPane().setVisible(false);
		if (loadingPanel != null) {
			loadingPanel.setVisible(false);
		}
	}

	/**
	 * Gets the TreePath from a given TreeNode.
	 *
	 * @param treeNode The node to get the path for.
	 * @return The TreePath, or null if the node is null.
	 */
	public TreePath getTreePathFromNode(TreeNode treeNode) {
		List<Object> nodes = new ArrayList<Object>();
		if (treeNode != null) {
			nodes.add(treeNode);
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				nodes.add(0, treeNode);
				treeNode = treeNode.getParent();
			}
		}

		return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
	}

	/**
	 * A class which represents a dirty bit.
	 * 
	 * @author Josh Lurz
	 *
	 */
	private class DirtyBit {
		/**
		 * Whether or not the dirty bit is set.
		 */
		private boolean mIsDirty;

		/**
		 * Constructor which initializes the dirty bit to false.
		 */
		public DirtyBit() {
			mIsDirty = false;
		}

		/**
		 * Set the dirty bit.
		 */
		public void setDirty() {
			mIsDirty = true;
		}

		/**
		 * Get the value of the dirty bit.
		 * 
		 * @return Whether the dirty bit is set.
		 */
		public boolean isDirty() {
			return mIsDirty;
		}
	}

	/**
	 * Opens the database management dialog.
	 */
	private void manageDB() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		new ManageDatabaseDialog(parentFrame, this).setVisible(true);
	}

	/**
	 * Writes a DOM Document to a file.
	 *
	 * @param file The file to write to.
	 * @param doc  The Document to serialize.
	 * @return true if writing was successful, false otherwise.
	 */
	private boolean writeFile(File file, Document doc) {
		LSSerializer serializer = implls.createLSSerializer();
		LSOutput lsOutput = implls.createLSOutput();
		try {
			lsOutput.setByteStream(new FileOutputStream(file));
			serializer.write(doc, lsOutput);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Writes the current queries document to the file specified in properties. If
	 * the file path is not set, shows an error dialog. Returns true if the file was
	 * written successfully, false otherwise.
	 */
	public boolean writeQueries() {
		Properties prop = InterfaceMain.getInstance().getProperties();
		String queryFileName = prop.getProperty("queryFile", null);
		if (queryFileName == null || queryFileName.trim().isEmpty()) {
			InterfaceMain.getInstance().showMessageDialog("No query file specified in properties.",
					"Error Saving Queries", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		File queryFile = new File(queryFileName);
		boolean success = writeFile(queryFile, queriesDoc);
		if (!success) {
			InterfaceMain.getInstance().showMessageDialog(
					"Failed to save queries to file: " + queryFile.getAbsolutePath(), "Error Saving Queries",
					JOptionPane.ERROR_MESSAGE);
		}
		return success;
	}

	/**
	 * Updates the query tree with the queries from the specified file.
	 *
	 * @param queryFile The query file to load.
	 */
	public void updateQueryTree(File queryFile) {
		invalidateQueriesDocument("updateQueryTree");
		queriesDoc = readQueries(queryFile);
		if (queriesDoc != null) {
			queries = getQueries();
			if (queryList != null) {
				queryList.setModel(queries);
				for (int i = 0; i < queryList.getRowCount(); ++i) {
					queryList.expandRow(i);
				}
			}
		}
	}

	/**
	 * Reads queries from the specified file into a Document.
	 * 
	 * @param queryFile The file containing queries.
	 * @return The Document containing queries.
	 */
	public Document readQueries(File queryFile) {
		if (queryFile.exists()) {
			LSInput lsInput = implls.createLSInput();
			try {
				lsInput.setByteStream(new FileInputStream(queryFile));
			} catch (FileNotFoundException e) {
				// is it even possible to get here
				e.printStackTrace();
			}
			LSParser lsParser = implls.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
			lsParser.setFilter(new ParseFilter());
			return lsParser.parse(lsInput);
		} else {
			return ((DOMImplementation) implls).createDocument("", "queries", null);
		}
	}

	/**
	 * Filter and existing DOM subtree with an LSParserFilter. This traverses the
	 * child nodes of the given node recursively removing any rejected nodes in the
	 * same manner that the LSParser would.
	 * 
	 * @param parentNode The current node who's children will be processed
	 *                   recursively.
	 * @param filter     The LSParserFilter to apply.
	 */
	public void filterNodes(Node parentNode, LSParserFilter filter) {
		Node currNode = parentNode.getFirstChild();
		final int whatToShow = filter.getWhatToShow();
		while (currNode != null) {
			Node nextNode = currNode.getNextSibling();
			// First determine if we should even inspect this kind of node.
			boolean showNode;
			switch (currNode.getNodeType()) {
			case Node.ATTRIBUTE_NODE: {
				showNode = (whatToShow & NodeFilter.SHOW_ATTRIBUTE) != 0;
				break;
			}
			case Node.COMMENT_NODE: {
				showNode = (whatToShow & NodeFilter.SHOW_COMMENT) != 0;
				break;
			}
			case Node.ELEMENT_NODE: {
				showNode = (whatToShow & NodeFilter.SHOW_ELEMENT) != 0;
				break;
			}
			case Node.TEXT_NODE: {
				showNode = (whatToShow & NodeFilter.SHOW_TEXT) != 0;
				break;
			}
			default: {
				showNode = (whatToShow & NodeFilter.SHOW_ALL) != 0;
				break;
			}
			}
			if (showNode && filter.acceptNode(currNode) == LSParserFilter.FILTER_REJECT) {
				// the node was rejected so remove it
				parentNode.removeChild(currNode);
			} else {
				// either the node should not be tested or it was accepted so we
				// keep and and recursively process from this node
				filterNodes(currNode, filter);
			}
			currNode = nextNode;
		}
	}

	/**
	 * A filter which will be used by the LSParser to filter out any nodes
	 * which are not necessary for the query file. This includes things like
	 * comments and insignificant whitespace.
	 * 
	 * @author Josh Lurz
	 * 
	 */
	public class ParseFilter implements LSParserFilter {
		/**
		 * A bitmask of what nodes to show. We will be showing all nodes and
		 * making a decision on each one.
		 */
		public int getWhatToShow() {
			return LSParserFilter.FILTER_ACCEPT; //This was ShowAll. Need to verify functionality of change.
		}

		/**
		 * The filter method which will decide which nodes to keep.
		 * 
		 * @param node The node to check.
		 * @return A short which will be either FILTER_ACCEPT, FILTER_REJECT, or
		 *         FILTER_SKIP.
		 */
		public short acceptNode(Node node) {
			// we want to keep all elements
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return LSParserFilter.FILTER_ACCEPT;
			}
			// we want to skip text nodes that are not all whitespace
			else if (node.getNodeType() == Node.TEXT_NODE && node.getTextContent().trim().length() != 0) {
				return LSParserFilter.FILTER_ACCEPT;
			}
			// we will reject everything else
			return LSParserFilter.FILTER_REJECT;
		}

		@Override
		public short startElement(Element element) {
			return LSParserFilter.FILTER_ACCEPT;
		}
	}

	/**
	 * Loads favorite queries file and applies it if requested by the user.
	 */
	/**
	 * Creates a file containing the selected queries as user's favorite queries.
	 * 
	 * @param myTree The JTree containing selected queries.
	 */
	/**
	 * Appends selected queries to the favorite queries file.
	 * 
	 * @param myTree The JTree containing selected queries.
	 */
	// YD added this method to convert the path string into each line with
	// ">",Feb-2024
	public static String convertPathToLine(String pathStr) {
		return Arrays.stream(pathStr.trim().split("\\s*,\\s*")).map(s -> s.isEmpty() ? s : '"' + s + '"')
				.collect(Collectors.joining(">"));
	}

	/**
	 * Counts the number of commas in a TreePath string representation.
	 *
	 * @param pathStr The string representation of the TreePath.
	 * @return The number of commas.
	 */
	// YD added this method to count number of commas in a TreePath,Mar-2024
	public int countCommaInPath(String pathStr) {
		int numCommas = pathStr.length() - pathStr.replace(",", "").length();
		return (numCommas);
	}

	/**
	 * Converts a TreePath that may contain commas in its node names to a
	 * delimited string line.
	 *
	 * @param treePathNow The TreePath to convert.
	 * @return A string representing the path with ">" as a delimiter.
	 */
	// YD added this method to count number of commas in a TreePath,Mar-2024
	public String convertPathWithCommaToLine(TreePath treePathNow) {
		int treePathCount = treePathNow.getPathCount();
		String myStr = "";
		for (int i = 0; i < treePathCount - 1; i++) {
			QueryGroup queryGroupNow = (QueryGroup) treePathNow.getPathComponent(i);
			String strNow = "\"" + queryGroupNow + "\"" + ">";
			myStr = myStr + strNow;
		}
		Object queryName = treePathNow.getPathComponent(treePathCount - 1);
		String lastPart = "\"" + queryName.toString() + "\"";
		String myLine = myStr + lastPart;
		return (myLine);
	}

	// YD edits end

	/**
	 * Extracts the BaseTableModel from a component, which is expected to be a
	 * QueryResultsPanel or a similar structure containing a JTable.
	 *
	 * @param comp The component to extract the model from.
	 * @return The BaseTableModel, or null if not found.
	 */
	public static BaseTableModel getTableModelFromComponent(java.awt.Component comp) {
		Object c;
		try {
			c = ((QueryResultsPanel) comp).getComponent(0);

			if (c instanceof JPanel) {
				return null;
			}
			if (c instanceof JSplitPane) {
				Component leftComponent = ((JSplitPane) c).getLeftComponent();
				JTable table = null;
				if (leftComponent instanceof JScrollPane) {
					table = (JTable) ((JScrollPane) leftComponent).getViewport().getView();
				} else if (leftComponent instanceof JPanel) {
					// Assuming the table is within a JScrollPane which is the second component of the JPanel
					JScrollPane scrollPane = (JScrollPane) ((JPanel) leftComponent).getComponent(1);
					table = (JTable) scrollPane.getViewport().getView();
				}

				if (table != null) {
					TableModel model = table.getModel();
					if (model instanceof TableSorter) {
						return (BaseTableModel) ((TableSorter) model).getTableModel();
					} else if (model instanceof BaseTableModel) {
						return (BaseTableModel) model;
					}
				}
				return null;
			} else {
				JTable table = (JTable) ((JScrollPane) c).getViewport().getView();
				TableModel model = table.getModel();
				if (model instanceof TableSorter) {
					return (BaseTableModel) ((TableSorter) model).getTableModel();
				}
				return (BaseTableModel) model;
			}
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * Extracts the JTable from a component, which is expected to be a
	 * QueryResultsPanel or a similar structure.
	 *
	 * @param comp The component to extract the table from.
	 * @return The JTable, or null if not found.
	 */
	public static JTable getJTableFromComponent(java.awt.Component comp) {
		Object c;
		try {
			QueryResultsPanel qPanel = (QueryResultsPanel) comp;
			c = qPanel.getComponent(0);

			if (c instanceof JPanel) {
				return null;
			}
			if (c instanceof JSplitPane) {
				JSplitPane jsp = (JSplitPane) c;
				Component c1 = jsp.getLeftComponent();

				if (c1 instanceof JScrollPane) {
					return (JTable) ((JScrollPane) (c1)).getViewport().getView();
				} else {
					// if not a JScrollPane, assumes it is a JPanel
					JPanel jp = (JPanel) c1;
					JScrollPane jscp = (JScrollPane) jp.getComponent(1);
					return (JTable) jscp.getViewport().getView();
				}
			} else {
				return (JTable) ((JScrollPane) c).getViewport().getView();
				}
		} catch (ClassCastException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a comment tooltip for a query tree path.
	 * 
	 * @param path The TreePath.
	 * @return The tooltip string in HTML format.
	 */
	public static String createCommentTooltip(TreePath path) {
		QueryGenerator qg;
		if (path.getLastPathComponent() instanceof QueryGenerator) {
			qg = (QueryGenerator) path.getLastPathComponent();
		} else {
			// SingleQueryValue..
			qg = (QueryGenerator) path.getParentPath().getLastPathComponent();
		}
		StringBuilder ret = new StringBuilder("<html><table cellpadding=\"2\"><tr><td>");
		for (int i = 1; i < path.getPathCount() - 1; ++i) {
			ret.append(path.getPathComponent(i)).append(":<br>");
		}
		ret.append(path.getLastPathComponent()).append("<br><br>Comments:<br>").append(qg.getComments())
				.append("</td></tr></table></html>");
		return ret.toString();
	}

	private class TabDragListener implements MouseListener, MouseMotionListener {
		MouseEvent firstMouseEvent = null;
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

		public void mousePressed(MouseEvent e) {
			JComponent c = (JComponent) e.getSource();
			if (tablesTabs.getTabCount() > 0
					&& tablesTabs.getBoundsAt(tablesTabs.getSelectedIndex()).contains(e.getPoint())) {
				if (e.getButton() == 3) {
					// Tell the transfer handler to initiate the copy.
					c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					tablesTabs.getTransferHandler().exportToClipboard(tablesTabs, clip, TransferHandler.COPY);
					c.setCursor(Cursor.getDefaultCursor());
				}
				firstMouseEvent = e;
				e.consume();
			}

		}

		public void mouseDragged(MouseEvent e) {
			// make sure that there was a press first and that that tab has not
			// since been closed
			if (firstMouseEvent != null && tablesTabs.getTabCount() > 0
					&& tablesTabs.getBoundsAt(tablesTabs.getSelectedIndex()).contains(e.getPoint())) {
				e.consume();

				int action = TransferHandler.COPY;

				int dx = Math.abs(e.getX() - firstMouseEvent.getX());
				int dy = Math.abs(e.getY() - firstMouseEvent.getY());
				// Arbitrarily define a 5-pixel shift as the
				// official beginning of a drag.
				if (dx > 5 || dy > 5) {
					JComponent c = (JComponent) e.getSource();
					c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					tablesTabs.getTransferHandler().exportAsDrag(tablesTabs, firstMouseEvent, action);
					firstMouseEvent = null;
					c.setCursor(Cursor.getDefaultCursor());
				}
			}
		}

		/**
		 * Unused mouse event.
		 * @param e The MouseEvent.
		 */
		// all the events we don't care about..
		public void mouseMoved(MouseEvent e) {
		}

		/**
		 * Unused mouse event.
		 * @param e The MouseEvent.
		 */
		public void mouseEntered(MouseEvent e) {
		}

		/**
		 * Unused mouse event.
		 * @param e The MouseEvent.
		 */
		public void mouseExited(MouseEvent e) {
		}

		/**
		 * Unused mouse event.
		 * @param e The MouseEvent.
		 */
		public void mouseClicked(MouseEvent e) {
		}

		/**
		 * Unused mouse event.
		 * @param e The MouseEvent.
		 */
		public void mouseReleased(MouseEvent e) {
		}
	}

	/**
	 * Creates a dialog which will ask for scenarios, regions, and queries to scan
	 * for SingleQueryValues. When finished selecting these values it will do the
	 * scan. This could take some time and the rest of the GUI should be inoperable
	 * while the scan is occuring. A progress bar will be displayed.
	 * 
	 * @param queries   All of the queries in the tree
	 * @param scenarios All of the scenarios.
	 * @param regions   All of the regions.
	 */
	private void createAndShowGetSingleQueries(final List<QueryGenerator> queries,
			final List<ScenarioListItem> scenarios, final List<String> regions) {
		// create the dialog which will block the rest of the gui until it is done
		final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		final JDialog scanDialog = new JDialog(parentFrame, "Update Single Query Cache", true);
		final JTabbedPane selectionTabs = new JTabbedPane();

		// JLists expects these as arrays so create them now
		final ScenarioListItem[] scenariosArr = new ScenarioListItem[scenarios.size()];
		final String[] regionsArr = new String[regions.size()];
		final QueryGenerator[] queriesArr = new QueryGenerator[queries.size()];
		scenarios.toArray(scenariosArr);
		regions.toArray(regionsArr);
		queries.toArray(queriesArr);

		// create the display components
		final JList selectScenarios = new JList(scenariosArr);
		final JList selectRegions = new JList(regionsArr);
		final JList selectQueries = new JList(queriesArr);
		final JButton scanButton = new JButton("Scan");
		final JButton cancelButton = new JButton("Cancel");
		final JPanel all = new JPanel();
		final Component seperator = Box.createRigidArea(new Dimension(20, 10));

		// create the progress bar
		final JProgressBar scanProgress = new JProgressBar(0, queries.size());
		final JLabel progLabel = new JLabel("Label");
		// processing should be done off of the gui thread to ensure responsiveness
		final Thread scanThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// increasing progress should be run on the gui thread so I will create
				// this runnable and use the SwingUtilities.invokeLater to run it on there
				final Runnable incProgress = new Runnable() {
					@Override
					public void run() {
						scanProgress.setValue(scanProgress.getValue() + 1);
					}
				};

				// make lists of the selected values only
				int[] selIndexes = selectScenarios.getSelectedIndices();
				final ScenarioListItem[] selScenarios = new ScenarioListItem[selIndexes.length];
				int pos = 0;
				for (int selIndex : selIndexes) {
					selScenarios[pos++] = scenariosArr[selIndex];
				}

				selIndexes = selectRegions.getSelectedIndices();
				final String[] selRegions = new String[selIndexes.length];
				pos = 0;
				for (int selIndex : selIndexes) {
					selRegions[pos++] = regionsArr[selIndex];
				}

				selIndexes = selectQueries.getSelectedIndices();
				final List<QueryGenerator> selQueries = new ArrayList<QueryGenerator>(selIndexes.length);
				for (int selIndex : selIndexes) {
					selQueries.add(queriesArr[selIndex]);
				}
				scanProgress.setMaximum(selIndexes.length);

				// get the cache document, if there is an exception getting it then it
				// may not exsist so we can try to create it
				XMLDB xmldbInstance = XMLDB.getInstance();
				QueryProcessor queryProc = xmldbInstance.createQuery("/singleQueryListCache", null, null, null);
				ANode doc = null;
				try {
					Iter res = queryProc.iter();
					doc = (ANode) res.next();
					if (doc == null) {
						// Try to create it then get the doc
						xmldbInstance.addFile("cache.xml", "<singleQueryListCache />", 1, 1);
						queryProc = xmldbInstance.createQuery("/singleQueryListCache", null, null, null);
						res = queryProc.iter();
						doc = (ANode) res.next();
					}
				} catch (QueryException e) {
				 // TODO: put error to screen?
					e.printStackTrace();
				} finally {
					queryProc.close();
				}

				// a final check if we were not able to get the doc then do not scan
				boolean wasInterrupted = doc == null;

				// for each query that is enabled have the extension create and cache it's
				// single query list. The cache will be set as metadata on the cache doc
				// if we got interrupted we must stop now
				for (Iterator<QueryGenerator> it = selQueries.iterator(); it.hasNext() && !wasInterrupted;) {
					QueryGenerator currQG = it.next();
					progLabel.setText("Scanning " + currQG.toString());
					SingleQueryExtension se = currQG.getSingleQueryExtension();
					// could be null if the extension is not enabled
					if (se != null) {
						se.createSingleQueryListCache(doc, selScenarios, selRegions);
						}
					SwingUtilities.invokeLater(incProgress);
				 wasInterrupted = Thread.interrupted();
					}

				// clean up and take down the progress bar
				scanDialog.setVisible(false);
			}
		});

		// default is to select all
		selectScenarios.setSelectionInterval(0, scenariosArr.length - 1);
		selectRegions.setSelectionInterval(0, regionsArr.length - 1);
		selectQueries.setSelectionInterval(0, queriesArr.length - 1);

		// create the tabs for the selections
		selectionTabs.addTab("Scenarios", new JScrollPane(selectScenarios));
		selectionTabs.addTab("Regions", new JScrollPane(selectRegions));
		selectionTabs.addTab("Queries", new JScrollPane(selectQueries));
		// have it take as much room as possible
		selectionTabs.setPreferredSize(new Dimension(400, 400));

		// need to make sure the label will align to the left
		final JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.add(progLabel);
		labelPanel.add(Box.createHorizontalGlue());

		// buttons need to be layouted out horizontally
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(scanButton);
		buttonPanel.add(seperator);
		buttonPanel.add(cancelButton);

		// the cancel button will interrupt the can if it is running
		// or just close the dialog if it is not. note that if the
		// users cancels NONE of the scan will be written back to the
		// database
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			 if (scanThread.isAlive()) {
					progLabel.setText("Canceling Scan");
					scanThread.interrupt();
					// this is in effect interrupting all single queries create list
					// queries
					int[] selIndexes = selectQueries.getSelectedIndices();
					for (int selIndex : selIndexes) {
						queriesArr[selIndex].getSingleQueryExtension().interruptGatherThread();
					}
					// will let the scan thread hide the dialog
				} else {
					// has not started yet so just hide it
					scanDialog.setVisible(false);
				}
			}
		});

		// when the scan button is hit we will switch the content of the dialog
		// from the selection lists to a progress bar to let the user know how
		// things are going. The user will still be able to cancel once the scan
		// starts
		scanButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scanButton.setEnabled(false);

				// set up the new content pane
				final JPanel progPanel = new JPanel();
				progPanel.setLayout(new BoxLayout(progPanel, BoxLayout.Y_AXIS));
				progPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				progPanel.add(scanProgress);
				progPanel.add(labelPanel);
				// make sure it is atleast 200 accross
				progPanel.add(Box.createHorizontalStrut(300));
				progPanel.add(Box.createVerticalGlue());
				progPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
				progPanel.add(seperator);
				progPanel.add(buttonPanel);

				// start scanning to cache queries
				scanThread.start();

				// display the new pane and shrink down any unnessary space
				scanDialog.setContentPane(progPanel);
				scanDialog.pack();
			}
		});

		// create the layout which will be tabbed pane on top and buttons on the
		// bottom
		all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
		all.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		all.add(selectionTabs);
		all.add(seperator);
		all.add(new JSeparator(SwingConstants.HORIZONTAL));
		all.add(seperator);
		all.add(buttonPanel);

		// set the content pane for the dialog and show it
		scanDialog.setSize(400, 400);
		scanDialog.setResizable(false);
		scanDialog.setContentPane(all);
		scanDialog.setVisible(true);
	}
	
	/**
	 * Best-effort shutdown of running query tabs and the XML database.
	 * <p>
	 * Cancels running queries immediately, then waits up to a bounded timeout off
	 * the EDT before closing the XMLDB.
	 */
	private void shutdownQueriesAndCloseDatabaseAsync() {
		final int tabCount = tablesTabs == null ? 0 : tablesTabs.getTabCount();

		// 1) Request cancellation quickly (non-blocking).
		for (int tab = 0; tab < tabCount; ++tab) {
			Component comp = tablesTabs.getComponentAt(tab);
			if (comp instanceof QueryResultsPanel) {
				((QueryResultsPanel) comp).killThread();
			} else if (comp instanceof DiffResultsPanel) {
				// DiffResultsPanel extends QueryResultsPanel, but be defensive.
				try {
					((QueryResultsPanel) comp).killThread();
				} catch (ClassCastException cce) {
					// ignore
				}
			}
		}

		// 2) Wait a bounded amount off the EDT, then close DB.
		final long perTabTimeoutMs = 1500L;
		final long maxTotalTimeoutMs = Math.max(perTabTimeoutMs, perTabTimeoutMs * Math.max(1, tabCount));

		Thread shutdownThread = new Thread(() -> {
			final long deadline = System.currentTimeMillis() + maxTotalTimeoutMs;
			for (int tab = 0; tab < tabCount; ++tab) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					break;
				}
				Component comp = tablesTabs.getComponentAt(tab);
				if (comp instanceof QueryResultsPanel) {
					((QueryResultsPanel) comp).killThreadAndWait(remaining);
				}
			}
			try {
				XMLDB.closeDatabase();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}, "DbViewer-Shutdown");
		shutdownThread.setDaemon(true);
		shutdownThread.start();
	}

	private Icon loadQueryTreeIcon(String resourcePath, String label) {
		java.net.URL resource = TabCloseIcon.class.getResource(resourcePath);
		if (resource == null) {
			InterfaceMain.logStartupTiming("DbViewer:missing query tree icon " + resourcePath + " for " + label);
			return javax.swing.UIManager.getIcon("Tree.leafIcon");
		}
		return new ImageIcon(resource);
	}
}

