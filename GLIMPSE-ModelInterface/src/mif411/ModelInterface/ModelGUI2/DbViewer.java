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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import javax.swing.JFileChooser;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoableEdit;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.basex.api.dom.BXDoc;
import org.basex.api.dom.BXNode;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.basex.query.value.node.ANode;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import ModelInterface.ConfigurationEditor.guihelpers.XMLFileFilter;
import ModelInterface.ModelGUI2.QueryTreeModel.QueryGroup;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.queries.SingleQueryExtension;
import ModelInterface.ModelGUI2.tables.BaseTableModel;
import ModelInterface.ModelGUI2.tables.TableSorter;
import ModelInterface.ModelGUI2.tables.TableTransferHandler;
import ModelInterface.ModelGUI2.undo.RenameScenarioUndoableEdit;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.common.DataPair;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.common.RecentFilesList.RecentFile;

import filter.FilterTreePaneYears;

/***
 * * Author Action Date Flag
 * ======================================================================= TWU
 * Add capability to allow 1/2/2017 @1 to invoke from command line Allow DBView
 * to accept filtered JTable which is in different table model and container Add
 * Copy to Clip board capability to allow large data paste to excel file Add a
 * monitor to large data drag
 * 
 */

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
 * @version 1.0
 * @since 2012
 */
public class DbViewer implements ActionListener, MenuAdder, BatchRunner {
	private Document queriesDoc;

	private static String controlStr = "DbViewer";

	private DOMImplementationLS implls;

	protected Vector<ScenarioListItem> scns;
	protected JList scnList;
	protected JList regionList;
	protected Vector regions;
	protected BaseTableModel bt; // does this still need to be a field?
	protected JScrollPane jsp; // does this still need to be a field?
	protected QueryTreeModel queries;
	private JTabbedPane tablesTabs = new JTabbedPane();
	private JSplitPane scenarioRegionSplit;
	private JSplitPane queriesSplit;
	private JSplitPane tableCreatorSplit;
	private JMenuItem queriesLockMenu; // YD added
	private JMenuItem queriesCreateMenu; // YD added
	private JMenuItem queriesEditMenu; // YD added
	private JMenuItem queriesRemoveMenu; // YD added
	private JMenuItem queriesUpdateMenu; // YD added
	private JMenuItem significantDigitsMenu; // YD added
	private JMenuItem enableUnitConversionsMenu;
	private JMenuItem createFavoritesMenu;// YD Feb-2024
	private JMenuItem loadFavoritesMenu;
	private JMenuItem appendFavoritesMenu;// YD Feb-2024
	private JMenuItem betaMn;
	// New: move Save/Save As under Tools > Queries menu
	private JMenuItem queriesSaveMenu;
	private JMenuItem queriesSaveAsMenu;

	private JMenuItem menuExpPrn;

	private String filteringText; // YD added
	private Enumeration<TreePath> expansionState;// YD added
	private boolean AllCollapsed = false; // YD added
	private static final int BOTTOM_PANE_HEIGHT = 25;
	ArrayList<String> region_list = new ArrayList<String>();// YD added,Feb-2024
	ArrayList<String> subregion_list = new ArrayList<String>();// YD added,Feb-2024
	ArrayList<String> preset_region_list = new ArrayList<String>();// YD added,Feb-2024
	private JComboBox<String> comboBoxPresetRegions; // YD added,Feb-2024
	private String[] preset_choices; // YD added,Feb-2024
	private JScrollPane listScrollRegions; // YD added,Feb-2024
	private JScrollPane listScrollQueries; // YD added,Feb-2024
	public static boolean queryTreeLocked = true; // YD added
	public static boolean disable3Digits = false; // YD added
	public static boolean enableUnitConversions = true;

	public static final String SCENARIO_LIST_NAME = "scenario list";
	public static final String REGION_LIST_NAME = "region list";

	private static Map<String, String> selectedYears = null;

	public static ArrayList openWindows = new ArrayList();

	private JFrame parentFrame = null;

	ArrayList<Object> windowList = new ArrayList<>();

	public DbViewer() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		this.parentFrame = parentFrame;
		final DbViewer thisViewer = this;

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
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						// make sure all queries get killed before we close the database
						for (int tab = 0; tab < tablesTabs.getTabCount(); ++tab) {
							((QueryResultsPanel) tablesTabs.getComponentAt(tab)).killThreadAndWait();
						}

						if (queries.hasChanges() && InterfaceMain.getInstance().showConfirmDialog(
								"The Queries have been modified.  Do you want to save them?", "Confirm Save Queries",
								JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
								JOptionPane.YES_OPTION) == JOptionPane.YES_OPTION) {
							writeQueries();
						}
						Properties prop = main.getProperties();
						prop.setProperty("scenarioRegionSplit",
								String.valueOf(scenarioRegionSplit.getDividerLocation()));
						prop.setProperty("queriesSplit", String.valueOf(queriesSplit.getDividerLocation()));
						prop.setProperty("tableCreatorSplit", String.valueOf(tableCreatorSplit.getDividerLocation()));
						main.getUndoManager().discardAllEdits();
						main.refreshUndoRedo();
						main.getSaveMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().removeActionListener(thisViewer);
						main.getSaveAsMenu().setEnabled(false);
						main.getSaveMenu().setEnabled(false);
						parentFrame.getContentPane().removeAll();

						XMLDB.closeDatabase();
					}
					if (evt.getNewValue().equals(controlStr)) {
						Properties prop = main.getProperties();
						String queryFileName = prop.getProperty("queryFile", null);
						File queryFile = queryFileName != null ? new File(queryFileName) : null;
						if (queryFile == null || !queryFile.exists()) {
							FileChooser fc = FileChooserFactory.getFileChooser();
							final FileFilter xmlFilter = new XMLFilter();
							File[] xmlFiles = fc.doFilePrompt(parentFrame,
									"Could not find query file.  Please select one.", FileChooser.LOAD_DIALOG,
									new File(main.getProperties().getProperty("lastDirectory", ".")), xmlFilter);
							if (xmlFiles == null && xmlFiles.length > 0) {
								// user hit cancel just create a new query file
								queryFileName = "Main_queries.xml";
								queryFile = new File(queryFileName);
							} else {
								queryFile = xmlFiles[0];
								queryFileName = queryFile.getAbsolutePath();
							}
							prop.setProperty("queryFile", queryFileName);
						}

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

						queriesDoc = readQueries(queryFile);

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
	public static Map<String, String> getSelectedYearsFromPropFile() {

		if (DbViewer.selectedYears == null) {

			DbViewer.selectedYears = new HashMap<>();
			if (InterfaceMain.getInstance() != null) {
				InterfaceMain.getInstance().getProperties();
				Properties globalProperties = InterfaceMain.getInstance().getProperties();
				Object rsp = globalProperties.get("selectedYearList");
				if (rsp != null) {
					String defaultYearStr = rsp.toString();
					System.out.println("DbViewer375: Using selectedYearsStr from properties file: " + defaultYearStr);

					String[] yearsArr = defaultYearStr.split(";");
					DbViewer.selectedYears = new HashMap<>();
					if (!(yearsArr.length == 1 && yearsArr[0].equals(""))) {
						for (String year : yearsArr) {
							DbViewer.selectedYears.put(year + "", year + "");
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
	 * Returns all years from the properties file or default years if not found.
	 * 
	 * @return List of all years.
	 */
	public static List getAllYearListFromPropFile() {

		List<String> allYearsList = new ArrayList<String>();
		if (InterfaceMain.getInstance() != null) {
			Properties globalProperties = InterfaceMain.getInstance().getProperties();

			String allYearStr = (String) globalProperties.get("allYearsList");
			System.out.println("DbViewer421: Setting allYearStr from properties file: " + allYearStr);

			if (allYearStr != null) {
				String[] yearsArr = allYearStr.split(";");
				allYearsList = new ArrayList<String>(yearsArr.length);
				if (!(yearsArr.length == 1 && yearsArr[0].equals(""))) {
					for (String year : yearsArr) {
						allYearsList.add(year);
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
		}

		return allYearsList;
	}

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
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		final ActionListener thisListener = this;
		addFileMenuItems(menuMan, main, parentFrame, thisListener);
		addViewMenuItems(menuMan, main, parentFrame);
		addAdvancedMenuItems(menuMan, main, parentFrame);
	}

	private void addFileMenuItems(InterfaceMain.MenuManager menuMan, InterfaceMain main, JFrame parentFrame,
			ActionListener thisListener) {
		JMenuItem menuItem = new JMenuItem("Open DB");
		menuItem.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuItem, 5);

		final JMenuItem menuManage = makeMenuItem("Manage DB");
		// menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).addMenuItem(menuManage, 10);
		menuManage.setEnabled(false);

		parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("Control")) {
					if (evt.getOldValue().equals(controlStr) || evt.getOldValue().equals(controlStr + "Same")) {
						menuManage.setEnabled(false);
						JMenuItem batchMenu = main.getBatchMenu();
						batchMenu.removeActionListener(thisListener);
						batchMenu.addActionListener(main);
					}
					if (evt.getNewValue().equals(controlStr)) {
						menuManage.setEnabled(true);
						JMenuItem batchMenu = main.getBatchMenu();
						batchMenu.removeActionListener(main);
						batchMenu.addActionListener(thisListener);
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
	 * "Close All Windows", "Disable 3 Significant Digits", "Disable Unit
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
		significantDigitsMenu = new JMenuItem("Disable 3 Significant Digits");
		significantDigitsMenu.addActionListener(this);
		significantDigitsMenu.setEnabled(true);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(significantDigitsMenu, 10);

		// Unit Conversions Toggle
		enableUnitConversionsMenu = new JMenuItem("Disable Unit Conversions");
		enableUnitConversionsMenu.addActionListener(this);
		enableUnitConversionsMenu.setEnabled(true);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(enableUnitConversionsMenu, 11);

		// Select Years
		JMenuItem yearsMn = new JMenuItem("Select Years");
		yearsMn.addActionListener(e -> new FilterTreePaneYears());
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addSeparator(20);
		menuMan.getSubMenuManager(InterfaceMain.VIEW_MENU_POS).addMenuItem(yearsMn, 21);
	}

	private void addAdvancedMenuItems(InterfaceMain.MenuManager menuMan, InterfaceMain main, JFrame parentFrame) {
		// Move Queries menus under the global Edit menu instead of Tools
		InterfaceMain.MenuManager editMM = menuMan.getSubMenuManager(InterfaceMain.EDIT_MENU_POS);
		if (editMM == null) {
			JMenu editMenu = new JMenu("Edit");
			menuMan.addMenuItem(editMenu, InterfaceMain.EDIT_MENU_POS);
			editMM = menuMan.getSubMenuManager(InterfaceMain.EDIT_MENU_POS);
		}

		// Ensure a Queries submenu exists under Edit
		InterfaceMain.MenuManager queriesMM = editMM.getSubMenuManager(InterfaceMain.TOOLS_SUBMENU1_POS);
		if (queriesMM == null) {
			editMM.addMenuItem(new JMenu("Queries"), InterfaceMain.TOOLS_SUBMENU1_POS);
			queriesMM = editMM.getSubMenuManager(InterfaceMain.TOOLS_SUBMENU1_POS);
		}

		// Move former Edit submenu items directly under Edit -> Queries
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

		// Favorites submenu now lives directly under Edit (moved from Edit -> Queries)
		JMenu favoritesSubMenu = new JMenu("Favorites List");
		editMM.addMenuItem(favoritesSubMenu, 4);
		// Add a separator between Favorites (4) and Preferences (5)
		editMM.addSeparator(4);
		// queriesMM.addMenuItem(favoritesSubMenu, 25);
		// queriesMM.addSeparator(26);

		// Add Run Batch under Tools (unchanged)
		InterfaceMain.MenuManager toolsMM = menuMan.getSubMenuManager(InterfaceMain.TOOLS_MENU_POS);
		if (toolsMM == null) {
			JMenu toolsMenu = new JMenu("Tools");
			menuMan.addMenuItem(toolsMenu, InterfaceMain.TOOLS_MENU_POS);
			toolsMM = menuMan.getSubMenuManager(InterfaceMain.TOOLS_MENU_POS);
		}
		JMenuItem runBatchMenu = makeMenuItem("Run Batch...");
		runBatchMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	InterfaceMain.getInstance().runBatch();
            }
        });
        toolsMM.addSeparator(51);
        toolsMM.addMenuItem(runBatchMenu, 52);

		// Favorites under Queries
		loadFavoritesMenu = makeMenuItem("Load Favorite Queries File");
		favoritesSubMenu.add(loadFavoritesMenu);

		createFavoritesMenu = makeMenuItem("Create Favorite Queries File");
		favoritesSubMenu.add(createFavoritesMenu);

		appendFavoritesMenu = makeMenuItem("Append Favorite Queries File");
		favoritesSubMenu.add(appendFavoritesMenu);

		/****Hiding menu option for hiding beta features since features are now reasonably stable
		// Separator
		menuMan.getSubMenuManager(InterfaceMain.TOOLS_MENU_POS).addSeparator(99);

		// Beta Features
		String betaText = (InterfaceMain.enableMapping || InterfaceMain.enableSankey) ? "Disable Beta Features"
				: "Enable Beta Features";
		betaMn = makeMenuItem(betaText);
		betaMn.addActionListener(this);
		menuMan.getSubMenuManager(InterfaceMain.TOOLS_MENU_POS).addMenuItem(betaMn, 100);
		*/
	}	
	
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
		case "Disable 3 Significant Digits":
			handleDisable3Digits();
			break;
		case "Enable 3 Significant Digits":
			handleEnable3Digits();
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
			doOpenDB(dbFiles[0]);
		}
	}

	private void handleEnableBetaFeatures() {
		betaMn.setText("Disable Beta Features");
		InterfaceMain.enableMapping = true;
		InterfaceMain.enableSankey = true;
		Properties prop = InterfaceMain.getInstance().getProperties();
		prop.setProperty("enableMapping", String.valueOf(InterfaceMain.enableMapping));
		prop.setProperty("enableSankey", String.valueOf(InterfaceMain.enableSankey));
	}

	private void handleDisableBetaFeatures() {
		betaMn.setText("Enable Beta Features");
		InterfaceMain.enableMapping = false;
		InterfaceMain.enableSankey = false;
		Properties prop = InterfaceMain.getInstance().getProperties();
		prop.setProperty("enableMapping", String.valueOf(InterfaceMain.enableMapping));
		prop.setProperty("enableSankey", String.valueOf(InterfaceMain.enableSankey));
	}

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
				batchQuery(batchFiles[0], xlsFiles[0]);
			}
		}
	}

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

	private void handleDisable3Digits() {
		significantDigitsMenu.setText("Enable 3 Significant Digits");
		disable3Digits = true;
		// TODO: add method to disable using 3 significant digits in the table
	}

	private void handleEnable3Digits() {
		significantDigitsMenu.setText("Disable 3 Significant Digits");
		disable3Digits = false;
		// TODO: add method to enable using 3 significant digits in the table
	}

	private void handleDisableUnitConversions() {
		enableUnitConversionsMenu.setText("Enable Unit Conversions");
		enableUnitConversions = false;
		// TODO: add method to disable unit conversions
	}

	private void handleEnableUnitConversions() {
		enableUnitConversionsMenu.setText("Disable Unit Conversions");
		enableUnitConversions = true;
		// TODO: add method to enable unit conversions
	}

	/**
	 * Opens the database from the specified file.
	 * 
	 * @param dbFile The database file to open.
	 */
	public void doOpenDB(File dbFile) {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		main.getProperties().setProperty("lastDirectory", dbFile.getParent());
		// put up a wait cursor so that the user knows things are happening while the
		// database loads
		parentFrame.getGlassPane().setVisible(true);
		try {
			XMLDB.openDatabase(dbFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			parentFrame.getGlassPane().setVisible(false);
			// tell the user it didn't open.
			InterfaceMain.getInstance().showMessageDialog("Could not open the xml database.", "Open DB Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		tablesTabs.setTransferHandler(new TableTransferHandler());
		TabDragListener dragListener = new TabDragListener();
		tablesTabs.addMouseListener(dragListener);
		tablesTabs.addMouseMotionListener(dragListener);

		createTableSelector();
		parentFrame.setTitle("GLIMPSE-CE ModelInterface [" + dbFile + "]");
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
		return new QueryTreeModel(queriesDoc.getDocumentElement());
	}

	JTree queryList = null;

	private JButton queryFilterButton;
	private JButton favoriteQueryButton;
	private JButton runQueryButton;
	private JButton diffQueryButton;
	private JButton listCollapseButton; 

	private JCheckBox doTotalCheckBox;

	protected void createTableSelector() {
		setupScenarioRegionLists();
		setupQueryTree();
		setupQueryPanel();
		setupButtonPanel();
		setupSplitPanes();
		setupPresetRegionDropdown();
		setupListeners();
		finalizeUI();
	}

	private void setupScenarioRegionLists() {
		scns = getScenarios();
		regions = getRegions();
		queries = getQueries();
		scnList = new JList(scns);
		scnList.setName(SCENARIO_LIST_NAME);
		regionList = new JList(regions);
		regionList.setName(REGION_LIST_NAME);
	}

	private void setupQueryTree() {
		final Icon queryIcon = new ImageIcon(TabCloseIcon.class.getResource("icons/group-query.png"));
		final Icon singleQueryIcon = new ImageIcon(TabCloseIcon.class.getResource("icons/single-query.png"));
		queryList = new JTree(queries);
		queryList.setTransferHandler(new QueryTransferHandler(queriesDoc, implls));
		queryList.setDragEnabled(true);
		queryList.getSelectionModel()
				.setSelectionMode(javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		queryList.setSelectionRow(0);
		for (int i = 0; i < queryList.getRowCount(); ++i) {
			queryList.expandRow(i);
		}
		queryList.setRowHeight(queryList.getFont().getSize() + 5);
		ToolTipManager.sharedInstance().registerComponent(queryList);
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

	private void setupPresetRegionDropdown() {
		loadRegionListToDropdown();
		JPanel presetRegionsPanel = new JPanel();
		presetRegionsPanel.setLayout(new BoxLayout(presetRegionsPanel, BoxLayout.X_AXIS));
		presetRegionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		// Move doTotalCheckBox here from setupButtonPanel
		doTotalCheckBox = new JCheckBox("Total  ");
		presetRegionsPanel.add(doTotalCheckBox);

		if (preset_choices != null && preset_choices.length > 0) {
			JLabel listLabel = new JLabel("Group:");
			comboBoxPresetRegions = new JComboBox<String>(preset_choices);
			comboBoxPresetRegions.setVisible(true);
			comboBoxPresetRegions.setMaximumSize(comboBoxPresetRegions.getPreferredSize());

			presetRegionsPanel.add(listLabel);
			presetRegionsPanel.add(comboBoxPresetRegions);
			// Add to region panel
			comboBoxPresetRegions.addActionListener(e -> selectPresetRegions());
		}
		((JPanel) scenarioRegionSplit.getRightComponent()).add(presetRegionsPanel);

	}

	private void setupButtonPanel() {
		JPanel queryPanel = (JPanel) queriesSplit.getRightComponent();
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		runQueryButton = new JButton("Run Query");
		diffQueryButton = new JButton("Diff Query");
		//final JButton 
		listCollapseButton = new JButton("View +/-");
		//final JButton 
		queryFilterButton = new JButton("Search");
		//final JButton 
		favoriteQueryButton = new JButton("Favorites");
		queriesEditMenu.setEnabled(false);
		runQueryButton.setEnabled(false);
		diffQueryButton.setEnabled(false);
		buttonPanel.add(runQueryButton);
		buttonPanel.add(diffQueryButton);
		buttonPanel.add(listCollapseButton);
		buttonPanel.add(queryFilterButton);
		buttonPanel.add(favoriteQueryButton);
		queryPanel.add(buttonPanel);
		this.queryFilterButton = queryFilterButton;
		this.favoriteQueryButton = favoriteQueryButton;
	}

	private void setupQueryPanel() {
		scenarioRegionSplit.getRightComponent();
		JPanel queryPanel = new JPanel();
		queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.Y_AXIS));
		JLabel listLabel = new JLabel("Queries");
		queryPanel.add(listLabel);
		listScrollQueries = new JScrollPane(queryList);
		listScrollQueries.setPreferredSize(new Dimension(150, 100));
		queryPanel.add(listScrollQueries);
		queriesSplit.setRightComponent(queryPanel);
	}

//	private void setupSplitPanes() {
//		JPanel listPane = new JPanel();
//		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
//		JLabel listLabel = new JLabel("Scenario");
//		listPane.add(listLabel);
//		JScrollPane listScroll = new JScrollPane(scnList);
//		// listScroll.setPreferredSize(new Dimension(150, 150));
//		listPane.add(listScroll);
//		scenarioRegionSplit.setLeftComponent(listPane);
//		listPane = new JPanel();
//		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
//		listLabel = new JLabel("Regions");
//		listPane.add(listLabel);
//		listScrollRegions = new JScrollPane(regionList);
//		// listScrollRegions.setPreferredSize(new Dimension(150, 100));
//		listPane.add(listScrollRegions);
//		scenarioRegionSplit.setRightComponent(listPane);
//		queriesSplit.setLeftComponent(scenarioRegionSplit);
//		tableCreatorSplit.setLeftComponent(queriesSplit);
//		tableCreatorSplit.setRightComponent(tablesTabs);
//
//		int frameWidth = parentFrame.getWidth();
//		scenarioRegionSplit.setDividerLocation((int) (frameWidth * 0.2));
//		queriesSplit.setDividerLocation((int) (frameWidth * 0.5));
//
//		int frameHeight = parentFrame.getHeight();
//		tableCreatorSplit.setDividerLocation((int) (frameHeight * 0.4));
//	}

	private void setupSplitPanes() {
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		JLabel listLabel = new JLabel("Scenario");
		listPane.add(listLabel);
		JScrollPane listScroll = new JScrollPane(scnList);
		// listScroll.setPreferredSize(new Dimension(150, 150));
		listPane.add(listScroll);

		// Create the bottom pane separately so it appears below the listPane
		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.X_AXIS));
		bottomPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottomPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, BOTTOM_PANE_HEIGHT));
		bottomPane.setPreferredSize(new Dimension(100, BOTTOM_PANE_HEIGHT));

		final JButton manageDbButton = new JButton("Manage DB");
		// left-justify the button
		bottomPane.add(Box.createHorizontalStrut(10));
		bottomPane.add(manageDbButton);
		bottomPane.add(Box.createHorizontalGlue());
		manageDbButton.addActionListener(e -> manageDB());
		// make bottom pane size consistent with other bottom panes
		bottomPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, BOTTOM_PANE_HEIGHT));
		bottomPane.setPreferredSize(new Dimension(100, BOTTOM_PANE_HEIGHT));
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
		leftWrapper.add(listPane, BorderLayout.CENTER);
		leftWrapper.add(bottomPane, BorderLayout.SOUTH);

		scenarioRegionSplit.setLeftComponent(leftWrapper);
		listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		listLabel = new JLabel("Regions");
		listPane.add(listLabel);
		listScrollRegions = new JScrollPane(regionList);
		// listScrollRegions.setPreferredSize(new Dimension(150, 100));
		listPane.add(listScrollRegions);
		scenarioRegionSplit.setRightComponent(listPane);
		queriesSplit.setLeftComponent(scenarioRegionSplit);
		tableCreatorSplit.setLeftComponent(queriesSplit);
		tableCreatorSplit.setRightComponent(tablesTabs);

		int frameWidth = parentFrame.getWidth();
		scenarioRegionSplit.setDividerLocation((int) (frameWidth * 0.2));
		queriesSplit.setDividerLocation((int) (frameWidth * 0.5));

		int frameHeight = parentFrame.getHeight();
		tableCreatorSplit.setDividerLocation((int) (frameHeight * 0.4));
	}
	
	private void setupListeners() {
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
				// System.out.println(returnValue);
				queries = getQueries();
				switch (returnValue) {
				case 0:
					// YD added,2024
					// Process the filter results...

					filteringText = field.getText();
					if (filteringText != null && filteringText.length() > 0) {
						queries = getFilteredQueries(queries, filteringText);

					}

					break;
				case 2:
				case -1:
					return;
				// case 'OK_OPTION' end here
				} // switch 'result' end
				queryList.setModel(queries);
				queryList.setSelectionRow(0);
				for (int i = 0; i < queryList.getRowCount(); ++i) {
					queryList.expandRow(i);
				}
			} // actionEvent end
		}); // queryFilterButton listener ends

		// YD added this listener,Feb-2024
		favoriteQueryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectFavoriteQueries(queryList);
			}
		}); // favoriteQueryButton listener ends

		// YD added this listener,Feb-2024
		createFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createFavoriteQueriesFile(queryList);

			}
		});// createFavoritesMenu listener ends

		loadFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFavoriteQueriesFile();

			}
		});// loadFavoritesMenu listener ends

		// YD added this listener,Mar-2024
		appendFavoritesMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				appendFavoriteQueries(queryList);

			}
		});// appendFavoritesMenu listener ends

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
					// iterates over selected queries by path
					for (int i = 0; i < selPaths.length; ++i) {
						try {
							QueryGenerator qg = null;
							QueryBinding singleBinding = null;
							if (selPaths[i].getLastPathComponent() instanceof QueryGenerator) {
								qg = (QueryGenerator) selPaths[i].getLastPathComponent();
							} else {
								singleBinding = ((SingleQueryExtension.SingleQueryValue) selPaths[i]
										.getLastPathComponent()).getAsQueryBinding();
								qg = (QueryGenerator) selPaths[i].getParentPath().getLastPathComponent();
							}
							// add loading icon to QueryResultsPanel
							TabCloseIcon loadingIcon = new TabCloseIcon(tablesTabs);
							// creating new panel for holding the results of the queries
							JComponent ret = new QueryResultsPanel(qg, singleBinding, scnList.getSelectedValues(),
									regionList.getSelectedValues(), loadingIcon, doTotalCheckBox.isSelected());

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
					// iterates over selected queries by path
					for (int i = 0; i < selPaths.length; ++i) {
						try {
							QueryGenerator qg = null;
							QueryBinding singleBinding = null;
							if (selPaths[i].getLastPathComponent() instanceof QueryGenerator) {
								qg = (QueryGenerator) selPaths[i].getLastPathComponent();
							} else {
								singleBinding = ((SingleQueryExtension.SingleQueryValue) selPaths[i]
										.getLastPathComponent()).getAsQueryBinding();
								qg = (QueryGenerator) selPaths[i].getParentPath().getLastPathComponent();
							}
							// add loading icon to QueryResultsPanel
							TabCloseIcon loadingIcon = new TabCloseIcon(tablesTabs);
							// creating new panel for holding the results of the queries
							JComponent ret = new DiffResultsPanel(qg, singleBinding, scnList.getSelectedValues(),
									regionList.getSelectedValues(), loadingIcon, doTotalCheckBox.isSelected());
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
		Container contentPane = parentFrame.getContentPane();
		contentPane.add(tableCreatorSplit);

		// have to get rid of the wait cursor
		parentFrame.getGlassPane().setVisible(false);

		parentFrame.setVisible(true);
	}

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
			// boolean removed=query_list.remove(toRemove.get(i));
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
				if (query_list.get(i).toString().toLowerCase().contains(query.toLowerCase())) {
					// System.out.println("found a match: " + query_list.get(i).toString());
				} else {
					query_list.get(i);
					toRemove.add(query_list.get(i));
				}
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			query_list.remove(toRemove.get(i));

		}
		return toRemove;
	}

	// YD added,2024
	public static Enumeration<TreePath> saveExpansionState(JTree tree) {
		return tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
	}

	// YD added,2024
	public void restoreExpansionState(Enumeration enumeration, JTree tree) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				TreePath treePath = (TreePath) enumeration.nextElement();
				tree.expandPath(treePath);
			}
		}
	}

	// YD added,2024
	private void collapseGroupWhenNotContainKeyWords(JTree myTree, QueryGroup mySubGroup) {
		ArrayList leaf_inside = mySubGroup.getQueryList();
		boolean containKeyWords = leaf_inside.toString().contains(filteringText);
		if (!containKeyWords) {
			getTreePathFromNode(mySubGroup);
			TreePath myFullTreePath = getFullTreePath(myTree, mySubGroup.getName());
			myTree.collapsePath(myFullTreePath);
		} else {
			leaf_inside.toString().indexOf(filteringText);
		} // inner if else loop end
	}

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

	// this method is to get the full tree path for a query group name
	// considering that the same query group name can appear multiple times in the
	// same tree
	// but at different locations,YD added
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

	// YD added, Feb-2024
	// this method is to load the preset region list from the control file to the
	// dropdown menu
	public void loadRegionListToDropdown() {

		if (InterfaceMain.presetRegionListLocation == null
				|| InterfaceMain.presetRegionListLocation.trim().length() == 0) {
			System.out.println("No regions list specified, skipping.");
			return;
		}

		String preset_region_list_filename = InterfaceMain.presetRegionListLocation;

		// preset region list
		try {
			ArrayList<String> contents = getStringArrayFromFile(preset_region_list_filename, "#");

			for (int i = 0; i < contents.size(); i++) {
				String line = contents.get(i);
				if (line.length() > 0) {
					preset_region_list.add(line);
				}
			}
		} catch (Exception e) {

			System.out.println("Unable to load region list: " + e.toString());

		}

		preset_choices = new String[preset_region_list.size() + 1];
		preset_choices[0] = "Select (optional)";
		for (int i = 0; i < preset_region_list.size(); i++) {
			String s = preset_region_list.get(i);
			int index_of_semicolon = s.indexOf(":");
			if (index_of_semicolon > -1) {
				s = s.substring(0, index_of_semicolon);
				preset_choices[i + 1] = s;
			}
		}

	}

	public void closeAllTabs() {
		// need to disable the export tabs option
		if (menuExpPrn != null) menuExpPrn.setEnabled(false);

		// grab the panel
		if (tablesTabs.getTabCount() == 0) {
			// noting to do
			return;
		}
		// iterate over children
		tablesTabs.removeAll();
		// close each one
	}

	public void closeAllWindows() {
		// need to disable the export tabs option
		// menuExpPrn.setEnabled(false);

		// grab the panel
		if (openWindows.size() == 0) {
			// noting to do
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
	 * Selects favorite queries in the query tree based on the favorite queries
	 * file.
	 * 
	 * @param queryList The JTree containing queries.
	 */
	public void selectFavoriteQueries(JTree queryList) {

		ArrayList<String> favoriteQueryLines = new ArrayList<>();
		ArrayList<TreePath> favoriteQueryPaths = new ArrayList<>();
		ArrayList<String> favoriteQueryNames = new ArrayList<>();
		String favoriteQueryFilename = InterfaceMain.favoriteQueriesFileLocation;
		try {
			favoriteQueryLines = getStringArrayFromFile(favoriteQueryFilename, "#");
		} catch (Exception e) {
			System.out.println("Could not read favorite queries file: " + e);
			JOptionPane.showMessageDialog(null, "Unable to load favorites file, please see console for error",
					"Error Saving File", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (String line : favoriteQueryLines) {
			if (!line.isEmpty()) {
				TreePath path = getTreePathForEachLine(queryList, line);
				if (path != null) {
					favoriteQueryPaths.add(path);
				} else {
					System.out.println("Unable to find path for " + line);
				}
				String[] splitLine = line.split(">");
				favoriteQueryNames.add(splitLine[splitLine.length - 1]);
			}
		}
		QueryTreeModel model = (QueryTreeModel) queryList.getModel();
		QueryGroup root = (QueryGroup) model.getRoot();
		int[] rowsToSelect = new int[favoriteQueryPaths.size()];
		ArrayList<String> notFound = new ArrayList<>();
		for (int i = 0; i < favoriteQueryPaths.size(); i++) {
			TreePath treePath = favoriteQueryPaths.get(i);
			String leafName = favoriteQueryNames.get(i);
			int rowNum = getRowNumberForLeaf(queryList, treePath, leafName);
			rowsToSelect[i] = rowNum;
			if (rowNum == -1) {
				notFound.add(leafName);
			}
		}
		if (notFound.size() != rowsToSelect.length) {
			queryList.clearSelection();
			// Only select valid rows
			ArrayList<Integer> validRows = new ArrayList<>();
			for (int row : rowsToSelect) {
				if (row != -1)
					validRows.add(row);
			}
			int[] validRowsArray = validRows.stream().mapToInt(Integer::intValue).toArray();
			queryList.setSelectionRows(validRowsArray);
			// Scroll to last found favorite query
			if (validRowsArray.length > 0) {
				java.awt.Rectangle bounds = queryList.getRowBounds(validRowsArray[validRowsArray.length - 1]);
				listScrollQueries.getVerticalScrollBar().setValue((int) bounds.getMinY());
			}
		}
		if (!notFound.isEmpty()) {
			StringBuilder errorMessage = new StringBuilder("The following queries were not found:\n\n");
			for (String s : notFound) {
				errorMessage.append(s).append("\n");
			}
			JOptionPane.showMessageDialog(null, errorMessage.toString());
		}
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
		boolean verbose = false;
		String selection = (String) comboBoxPresetRegions.getSelectedItem();
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
	} // selectItemsFromRegionList method end

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
			// if (leafName.contains(leaves.get(m).toString())) {
			if (leafName.replace("\"", "").trim().compareToIgnoreCase(leaves.get(m).toString().trim()) == 0) {
				// System.out.println("found my favorite query name here:" +
				// leaves.get(m).toString());
				int myIndex = ((TreeModel) myTree.getModel()).getIndexOfChild(myChildGroup, leaves.get(m));
				rowNumForLeaf = rowNumForSubgroup + myIndex + 1;
				break;
			}
		} // for loop end
		return rowNumForLeaf;
	} // get RowNumberForLeaf method end

	/**
	 * Gets the full tree path for each line in the favorite query list.
	 * 
	 * @param myTree The JTree.
	 * @param myLine The line from the favorite query list.
	 * @return The TreePath corresponding to the line, or null if not found.
	 */
	private TreePath getTreePathForEachLine(JTree myTree, String myLine) {
		TreePath theFullPath = null;
		String[] splitLine = myLine.split(">");
		String[] splitLineForGroups = Arrays.copyOfRange(splitLine, 0, splitLine.length - 1);
		String child_group_to_find = splitLine[splitLine.length - 2]; // get the last child group before the leaf
		String group_to_find_trim = child_group_to_find.substring(1, child_group_to_find.length() - 1); // remove double
																										// quotes
		int pathCount = splitLine.length - 1;
		ArrayList<TreePath> allPaths = getFullTreePath2(myTree, group_to_find_trim, pathCount);
		if (allPaths.size() == 1) {
			theFullPath = allPaths.get(0);
		} else if ((allPaths.size() > 1)) { // when queryGroup appears multiple times in a tree
			for (int i = 0; i < allPaths.size(); i++) {
				TreePath testPath = allPaths.get(i);
				boolean checkMatchAll = matchsAllGroups(testPath, splitLineForGroups);
				if (checkMatchAll) {
					theFullPath = allPaths.get(i);
					// System.out.println("this is the treePath that matches :" + theFullPath);
				}

			}
		}
		return theFullPath;
	} // getTreePathForEachLine method end

	/**
	 * Checks if all elements of a string array are present in the TreePath.
	 * 
	 * @param myPath  The TreePath.
	 * @param allStrs Array of group names.
	 * @return True if all groups match, false otherwise.
	 */
	public boolean matchsAllGroups(TreePath myPath, String[] allStrs) {
		boolean matchsAll = true;
		for (int i = 0; i < allStrs.length; i++) {
			// need to remove the double quotes for each group
			String groupName = allStrs[i];
			String groupQuoteRemoved = groupName.substring(1, groupName.length() - 1);
			String myPathStr = (String) myPath.getPathComponent(i).toString();
			if (!myPathStr.equals(groupQuoteRemoved)) {
				matchsAll = false;
				break;
			}
		}
		return matchsAll;
	}

	// YD edits end

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

	private void manageDB() {
		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		final JDialog filterDialog = new JDialog(parentFrame, "Manage Database", true);
		// Disable UI while DB is closed or rebuilding
		if (XMLDB.getInstance() == null) {
			InterfaceMain.getInstance().showMessageDialog("No database is open. Please open a database first.", "Manage DB", JOptionPane.ERROR_MESSAGE);
			return;
		}
		filterDialog.getGlassPane().addMouseListener(new MouseAdapter() {
		});
		filterDialog.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		final DbViewer thisViewer = this;
		JPanel listPane = new JPanel();
		JPanel buttonPane = new JPanel();
		// Status field shown between the list pane and the buttons
		final javax.swing.JTextField statusField = new javax.swing.JTextField();
		statusField.setEditable(false);
		statusField.setBackground(Color.LIGHT_GRAY);
		statusField.setText("");
		statusField.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusField.getPreferredSize().height));
         final JButton addButton = new JButton("Add");
         final JButton removeButton = new JButton("Remove");
         final JButton renameButton = new JButton("Rename");
         final JButton exportButton = new JButton("Export");
         final JButton rebuildButton = new JButton("Rebuild DB");
          final JButton doneButton = new JButton("Done");
          listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
          Container contentPane = filterDialog.getContentPane();
          removeButton.setEnabled(false);
          renameButton.setEnabled(false);
          exportButton.setEnabled(false);
          rebuildButton.setEnabled(true);

        // Add a small uniform padding around each button in the dialog
        java.awt.Insets btnPadding = new java.awt.Insets(2, 2, 2, 2);
        addButton.setMargin(btnPadding);
        removeButton.setMargin(btnPadding);
        renameButton.setMargin(btnPadding);
        exportButton.setMargin(btnPadding);
        rebuildButton.setMargin(btnPadding);
        doneButton.setMargin(btnPadding);

        // Make Manage DB buttons visually match the Run/Diff buttons below the query list.
        // Use the Run Query button's preferred width and font if available to keep a consistent look.
        int preferredBtnWidth = 90;
        java.awt.Font referenceFont = null;
        if (runQueryButton != null) {
            java.awt.Dimension ref = runQueryButton.getPreferredSize();
            if (ref != null && ref.width > 0) preferredBtnWidth = ref.width;
            referenceFont = runQueryButton.getFont();
        }
        java.awt.Dimension manageBtnDim = new java.awt.Dimension(preferredBtnWidth, BOTTOM_PANE_HEIGHT - 8);
        JButton[] manageButtons = new JButton[] { addButton, removeButton, renameButton, exportButton, 
                rebuildButton, doneButton };
        for (JButton b : manageButtons) {
            b.setPreferredSize(manageBtnDim);
            b.setMaximumSize(new java.awt.Dimension(preferredBtnWidth, BOTTOM_PANE_HEIGHT));
            b.setAlignmentY(Component.CENTER_ALIGNMENT);
            if (referenceFont != null) b.setFont(referenceFont);
        }

        // list of scenarios displayed in the Manage DB dialog
        final JList list = new JList(scns != null ? scns : new Vector<ScenarioListItem>());
        
        // Enable/disable action buttons based on selection in the scenarios list
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean hasSelection = !list.isSelectionEmpty();
                removeButton.setEnabled(hasSelection);
                renameButton.setEnabled(hasSelection);
                exportButton.setEnabled(hasSelection);
            }
        });

         // Map to store previous enabled states so we can restore them after long-running ops
         final java.util.Map<JButton, Boolean> prevStates = new java.util.HashMap();
         // Runnable to disable all dialog buttons on the EDT and remember their previous state
         final Runnable disableAllButtons = new Runnable() {
             @Override
             public void run() {
                 SwingUtilities.invokeLater(new Runnable() {
                     @Override
                     public void run() {
                         prevStates.put(addButton, addButton.isEnabled());
                         prevStates.put(removeButton, removeButton.isEnabled());
                         prevStates.put(renameButton, renameButton.isEnabled());
                         prevStates.put(exportButton, exportButton.isEnabled());
                         prevStates.put(rebuildButton, rebuildButton.isEnabled());
                         prevStates.put(doneButton, doneButton.isEnabled());
                         addButton.setEnabled(false);
                         removeButton.setEnabled(false);
                         renameButton.setEnabled(false);
                         exportButton.setEnabled(false);
                         rebuildButton.setEnabled(false);
                         doneButton.setEnabled(false);
                     }
                 });
             }
         };
         // Runnable to restore buttons' enabled state using saved values
         final Runnable restoreAllButtons = new Runnable() {
             @Override
             public void run() {
                 SwingUtilities.invokeLater(new Runnable() {
                     @Override
                     public void run() {
                         Boolean b;
                         b = prevStates.get(addButton); if (b != null) addButton.setEnabled(b);
                         b = prevStates.get(removeButton); if (b != null) removeButton.setEnabled(b);
                         b = prevStates.get(renameButton); if (b != null) renameButton.setEnabled(b);
                         b = prevStates.get(exportButton); if (b != null) exportButton.setEnabled(b);
                         b = prevStates.get(rebuildButton); if (b != null) rebuildButton.setEnabled(b);
                         b = prevStates.get(doneButton); if (b != null) doneButton.setEnabled(b);
                     }
                 });
             }
         };

        // Dirty bit tracks changes; declare it here so listeners can use it
        final DirtyBit dirtyBit = new DirtyBit();

         addButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {

                 FileChooser fc = FileChooserFactory.getFileChooser();
                 final FileFilter xmlFilter = new XMLFilter();
                 // Dan: parentFrame seems to introduce some dialog blocking issues. Trying to
                 // set parent of filechooser as the filterDialog

                 SwingUtilities.invokeLater(() -> {
                     final File[] xmlFiles = fc.doFilePrompt(/* parentFrame */filterDialog, "Open XML File",
                             FileChooser.LOAD_DIALOG, new File(main.getProperties().getProperty("lastDirectory", ".")),
                             xmlFilter);

                     if (xmlFiles != null) {

                        dirtyBit.setDirty();
                        main.getProperties().setProperty("lastDirectory", xmlFiles[0].getParent());
                        // update status and start background add
                        statusField.setText("Adding files...");
                        // disable dialog buttons while adding
                        //disableAllButtons.run();

                        new Thread(new Runnable() {
                             @Override
                             public void run() {
                                 for (int addFileIndex = 0; addFileIndex < xmlFiles.length; ++addFileIndex) {
                                     if (xmlFiles[addFileIndex] != null) {
                                         // pass the Manage Database dialog as owner so the import
                                         // progress dialog is positioned relative to it and keeps focus
                                         XMLDB.getInstance().addFile("run_" + System.currentTimeMillis() + ".xml",
                                                 xmlFiles[addFileIndex].getAbsolutePath(), addFileIndex, xmlFiles.length,
                                                 filterDialog);
                                     }
                                 }
                                 scns = getScenarios();
                                 // update UI on EDT and restore buttons
                                 SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        list.setListData(scns);
                                        statusField.setText("Add complete");
                                        //restoreAllButtons.run();
                                    }
                                 });
                             }
                         }).start();
                      }
                  });
              }

          });
         removeButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 final Object[] remList = list.getSelectedValues();
                 if (remList == null || remList.length == 0) {
                     return;
                 }
                 
                 int ans = InterfaceMain.getInstance().showConfirmDialog(
                         "Remove scenario? This removes the scenario index but does not decrease database size.\nAfter removing, click on Rebuild DB to reclaim space.\nRebuild DB may take as long as an hour or two.",
                         "Click on Optimize to reclaim space.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                         JOptionPane.NO_OPTION);
                 if (ans != JOptionPane.YES_OPTION) {
                     return;
                 }
                 
                 // Inform user and show busy glass, then perform removal off the EDT
                 SwingUtilities.invokeLater(new Runnable() {
                     @Override
                     public void run() {
                         statusField.setText("Removing selected scenarios...");
                         filterDialog.getGlassPane().setVisible(true);
                     }
                 });

                 // disable dialog buttons while removal runs
                 //disableAllButtons.run();
                 
                 new Thread(new Runnable() {
                     @Override
                     public void run() {
                         try {
                             for (int i = 0; i < remList.length; ++i) {
                                 dirtyBit.setDirty();
                                 XMLDB.getInstance().removeDoc(((ScenarioListItem) remList[i]).getDocName());
                             }
                             scns = getScenarios();
                         } finally {
                             // Update UI on EDT when done and restore buttons
                             SwingUtilities.invokeLater(new Runnable() {
                                 @Override
                                 public void run() {
                                     list.setListData(scns);
                                     filterDialog.getGlassPane().setVisible(false);
                                     statusField.setText("Remove complete");
                                     //this may have been causing some issues
                                     //restoreAllButtons.run();
                                     // After removal completed, ensure these action buttons are disabled
                                     removeButton.setEnabled(false);
                                     renameButton.setEnabled(false);
                                     exportButton.setEnabled(false);
                                 }
                             });
                         }
                     }
                 }).start();
              }
          });
         renameButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 final Object[] renameList = list.getSelectedValues();
                 if (renameList.length == 0) {
                     return;
                 }
                 final JDialog renameScenarioDialog = new JDialog(parentFrame, "Rename Scenarios", true);
                 renameScenarioDialog.setResizable(false);
                 final List<JTextField> renameBoxes = new ArrayList<JTextField>(renameList.length);
                 JPanel renameBoxPanel = new JPanel();
                 renameBoxPanel.setLayout(new BoxLayout(renameBoxPanel, BoxLayout.Y_AXIS));
                 Component verticalSeparator = Box.createVerticalStrut(5);

                 for (int i = 0; i < renameList.length; ++i) {
                     ScenarioListItem currItem = (ScenarioListItem) renameList[i];
                     JPanel currPanel = new JPanel();
                     currPanel.setLayout(new BoxLayout(currPanel, BoxLayout.X_AXIS));
                     JLabel currLabel = new JLabel("<html>Rename <b>" + currItem.getScnName() + "</b> on <b>"
                             + currItem.getScnDate() + "</b> to:</html>");
                     JTextField currTextBox = new JTextField(currItem.getScnName(), 20);
                     currTextBox.setMaximumSize(currTextBox.getPreferredSize());
                     renameBoxes.add(currTextBox);
                     currPanel.add(currLabel);
                     currPanel.add(Box.createHorizontalGlue());
                     renameBoxPanel.add(currPanel);
                     currPanel = new JPanel();
                     currPanel.setLayout(new BoxLayout(currPanel, BoxLayout.X_AXIS));
                     currPanel.add(currTextBox);
                     currPanel.add(Box.createHorizontalGlue());
                     renameBoxPanel.add(currPanel);
                    	renameBoxPanel.add(verticalSeparator);
                 }

                 JPanel renameButtonPanel = new JPanel();
                 final JButton renameOK = new JButton("  OK  ");
                 final JButton renameCancel = new JButton("Cancel");
                 renameButtonPanel.setLayout(new BoxLayout(renameButtonPanel, BoxLayout.X_AXIS));
                 renameButtonPanel.add(Box.createHorizontalGlue());
                 renameButtonPanel.add(renameOK);
                 renameButtonPanel.add(Box.createHorizontalStrut(10));
                 renameButtonPanel.add(renameCancel);
                 ActionListener renameButtonListener = new ActionListener() {
                     public void actionPerformed(ActionEvent renameEvt) {
                         if (renameEvt.getSource() == renameOK) {
                             for (int i = 0; i < renameList.length; ++i) {
                                 ScenarioListItem currItem = (ScenarioListItem) renameList[i];
                                 String currText = renameBoxes.get(i).getText();
                                 // only do it if the name really is different
                                 if (!currItem.getScnName().equals(currText)) {
                                     // the undoable edit will take care of doing
                                     // the rename
                                     UndoableEdit renameEdit = new RenameScenarioUndoableEdit(thisViewer, currItem,
                                             currText);
                                     InterfaceMain.getInstance().getUndoManager().addEdit(renameEdit);
                                     InterfaceMain.getInstance().refreshUndoRedo();
                                 }
                             }
                         }
                         // scns = getScenarios();
                         list.setListData(scns);
                         renameScenarioDialog.dispose();
                     }
                 };
                 renameOK.addActionListener(renameButtonListener);
                 renameCancel.addActionListener(renameButtonListener);

                 renameBoxPanel.add(renameButtonPanel);
                 renameBoxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                 renameScenarioDialog.getContentPane().add(renameBoxPanel);
                 renameScenarioDialog.pack();
                 renameScenarioDialog.setVisible(true);
             }
         });

         exportButton.addActionListener(new ActionListener() {
             /**
              * Method called when the export button is clicked which allows the user to
              * select a location to export the scenario to and exports the scenario.
              * 
              * @param aEvent The event received.
              * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
              */
             public void actionPerformed(ActionEvent aEvent) {
                 final Object[] selectedList = list.getSelectedValues();

                 // !!!Dan: updated this so isSingleSelection is always false
                 final boolean isSingleSelection = false; // selectedList.length == 1;
                 FileFilter fileFilter;
                 String saveDialogTitle;

                 if (isSingleSelection) {
                     fileFilter = new XMLFileFilter();
                     saveDialogTitle = "Save As XML";
                 } else {
                     fileFilter = new FileFilter() {
                         public boolean accept(File f) {
                             return f.isDirectory();
                         }

                         public String getDescription() {
                             return "Directory to export into";
                         }
                     };
                     saveDialogTitle = "Select Export Directory";
                 }
                 FileChooser fc = FileChooserFactory.getFileChooser();

                 final File[] exportLocation = fc.doFilePrompt(null, saveDialogTitle, FileChooser.SAVE_DIALOG,
                         new File(main.getProperties().getProperty("lastDirectory", ".")), fileFilter);
                 if (isSingleSelection && !exportLocation[0].getName().endsWith(".xml")) {
                     exportLocation[0] = new File(exportLocation[0].getParentFile(),
                             exportLocation[0].getName() + ".xml");
                 }
                 if (exportLocation == null) {
                     // user canceled, nothing to do
                     return;
                 }
                 // update status before starting background export
                 statusField.setText("Exporting runs to selected directory...");
                 // disable dialog buttons while exporting
                 //disableAllButtons.run();
                 final JProgressBar progBar = new JProgressBar(0, selectedList.length);
                 final JLabel curLabel = new JLabel("Exporting runs from the database");
                 // Create the export progress dialog and then position it centered on the Manage Database dialog
                 final JDialog jd = XMLDB.createProgressBarGUI(progBar, "Exporting Runs", curLabel);
                 if (jd != null) {
                     jd.setLocationRelativeTo(filterDialog);
                 }
                 final Runnable incProgress = (new Runnable() {
                     @Override
                     public void run() {
                         progBar.setValue(progBar.getValue() + 1);
                     }
                 });
                 if (jd != null) {
                     jd.setVisible(true);
                 }

                 // run the export off the gui thread which ensures progress updates correctly
                 // and keeps the gui responsive
                 new Thread(new Runnable() {
                      @Override
                      public void run() {
                          boolean success = true;
                          for (int i = 0; i < selectedList.length; ++i) {
                             ScenarioListItem currItem = (ScenarioListItem) selectedList[i];
                             File exportFile;

                             if (isSingleSelection) {
                                 exportFile = exportLocation[0];
                             } else {
                                 String exportFileName = currItem.getScnName() + "_"
                                         + currItem.getScnDate().replaceAll(":", "_") + ".xml";
                                 exportFile = new File(exportLocation[0], exportFileName);
                             }
                             success = success && XMLDB.getInstance().exportDoc(currItem.getDocName(), exportFile);
                             SwingUtilities.invokeLater(incProgress);
                          }
                          if (jd != null) {
                             jd.setVisible(false);
                          }
                          final boolean finalSuccess = success;
                          SwingUtilities.invokeLater(new Runnable() {
                             @Override
                             public void run() {
                                 if (finalSuccess) {
                                     InterfaceMain.getInstance().showMessageDialog("Scenario export succeeded.",
                                             "Scenario Export", JOptionPane.INFORMATION_MESSAGE);
                                     statusField.setText("Export complete");
                                 } else {
                                     InterfaceMain.getInstance().showMessageDialog("Scenario export failed.", null,
                                             JOptionPane.ERROR_MESSAGE);
                                     statusField.setText("Export failed");
                                 }
                                 // restore dialog buttons after export finishes
                                 //removed since not necessary
                                 //restoreAllButtons.run();
                             }
                         });
                      }
                  }).start();
              }

          });

         // Rebuild DB button - export current docs, drop DB files, recreate DB and re-import docs
         rebuildButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // verify DB is open
                if (XMLDB.getInstance() == null) {
                    InterfaceMain.getInstance().showMessageDialog("No database is open.", "Rebuild DB",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int ans = InterfaceMain.getInstance().showConfirmDialog(
                        "Rebuild database? This will export all scemarops, create a fresh database and re-import the scenarios.\nRebuilding is expected to require as long as an hour or two.",
                        "Confirm Rebuild", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) {
                    return;
                }

                // Disable UI while rebuilding
                disableAllButtons.run();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setText("Rebuilding database...");
                        filterDialog.getGlassPane().setVisible(true);
                        filterDialog.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File tempDir = null;
                        File backupDir = null;
                        String cont = null;
                        String dbPath = null;
                        AtomicBoolean rebuildSuccess = new AtomicBoolean(false);
                        try {
                            // gather docs to export from the Manage DB dialog list (only what's shown there)
                            java.util.List<ScenarioListItem> currentScns = new java.util.ArrayList<ScenarioListItem>();
                            javax.swing.ListModel lm = list.getModel();
                            for (int i = 0; i < lm.getSize(); i++) {
                                Object el = lm.getElementAt(i);
                                if (el instanceof ScenarioListItem) {
                                    currentScns.add((ScenarioListItem) el);
                                }
                            }
                            if (currentScns.isEmpty()) {
                                throw new Exception("No documents found in Manage Database list to export.");
                            }
                            // create temp dir
                            String tmpBase = System.getProperty("java.io.tmpdir");
                            tempDir = new File(tmpBase, "glimpse_rebuild_" + System.currentTimeMillis());
                            if (!tempDir.mkdirs()) {
                                throw new IOException("Could not create temporary directory: " + tempDir);
                            }
                            // export each doc with detailed logging
                            final AtomicInteger num = new AtomicInteger(0);
                            for (ScenarioListItem s : currentScns) {
                                final int currNum = num.incrementAndGet();
                                File out = new File(tempDir, s.getDocName());
                                System.out.println("Rebuild: exporting " + s.getDocName() + " -> " + out.getAbsolutePath());
                                final String exportName = s.getDocName();
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusField.setText("Exporting " + exportName + " (" + currNum + "/" + currentScns.size() + ")");
                                    }
                                });
                                boolean ok = XMLDB.getInstance().exportDoc(s.getDocName(), out);
                                if (!ok) {
                                    throw new IOException("Failed to export " + s.getDocName());
                                } else {
                                    System.out.println("Rebuild: export succeeded for " + s.getDocName());
                                }
                            }
                            // capture container and db path
                            cont = XMLDB.getInstance().getContainer();
                            dbPath = System.getProperty("org.basex.DBPATH");
                            System.out.println("Rebuild: captured container='" + cont + "' dbPath='" + dbPath + "'");
                            // close database
                            System.out.println("Rebuild: closing database before deleting files...");
                            XMLDB.closeDatabase();
                            // --- BEGIN BACKUP LOGIC ---
                            File dbDir = null;
                            if (dbPath != null && cont != null) {
                                dbDir = new File(dbPath, cont);
                                if (dbDir.exists()) {
                                    // Create backup directory
                                    backupDir = new File(dbDir.getParentFile(), dbDir.getName() + "_backup_" + System.currentTimeMillis());
                                    System.out.println("Rebuild: backing up DB directory to " + backupDir.getAbsolutePath());
                                    copyDirectory(dbDir, backupDir);
                                    System.out.println("Rebuild: backup completed");
                                } else {
                                    System.out.println("Rebuild: DB directory not found: " + dbDir.getAbsolutePath());
                                }
                            }
                            // --- END BACKUP LOGIC ---
                            // delete old DB files
                            if (dbDir != null && dbDir.exists()) {
                                System.out.println("Rebuild: deleting old DB files at " + dbDir.getAbsolutePath());
                                deleteRecursive(dbDir);
                                System.out.println("Rebuild: deletion of old DB files completed");
                            }
                            // create a fresh context and DB, import files
                            org.basex.core.Context tmpCtx = new org.basex.core.Context();
                            final AtomicInteger num1 = new AtomicInteger(0);
                            boolean tmpCtxAdopted = false;
                            try {
                                System.out.println("Rebuild: creating new database '" + cont + "'...");
                                new org.basex.core.cmd.CreateDB(cont).execute(tmpCtx);
                                File[] exportFiles = tempDir.listFiles();
                                if (exportFiles != null) {
                                    for (File f : exportFiles) {
                                        final int currNum1 = num1.incrementAndGet();
                                        System.out.println("Rebuild: importing file " + f.getName() + " -> database '" + cont + "'");
                                        SwingUtilities.invokeLater(new Runnable() {
                                            public void run() {
                                                statusField.setText("Importing " + f.getName() + " (" + currNum1 + "/" + exportFiles.length + ")");
                                            }
                                        });
                                        new org.basex.core.cmd.Add(f.getName(), f.getAbsolutePath()).execute(tmpCtx);
                                        System.out.println("Rebuild: import completed for " + f.getName());
                                    }
                                } else {
                                    System.out.println("Rebuild: no export files found in temp dir: " + tempDir);
                                }
                                // Instead of closing tmpCtx here, set the default collection on tmpCtx
                                // and attempt to adopt tmpCtx into XMLDB so queries will see the default collection.
                                try {
                                    System.out.println("Rebuild: setting default collection '" + cont + "' on tmpCtx...");
                                    new org.basex.core.cmd.Open(cont).execute(tmpCtx);
                                } catch (Exception openEx) {
                                    System.out.println("Rebuild: failed to open collection on tmpCtx: " + openEx);
                                }

                                try {
                                    System.out.println("Rebuild: attempting to open database by adopting tmpCtx...");
                                    XMLDB.openDatabase(tmpCtx);
                                    tmpCtxAdopted = true;
                                    System.out.println("Rebuild: database opened by adopting tmpCtx");
                                } catch (Exception adoptEx) {
                                    System.out.println("Rebuild: adopt tmpCtx open failed: " + adoptEx);
                                    // fallback to previous behavior: create a fresh context and open the desired collection
                                    try {
                                        XMLDB.openDatabase(new org.basex.core.Context());
                                        try {
                                            if (cont != null && XMLDB.getInstance() != null && XMLDB.getInstance().getContext() != null) {
                                               org.basex.core.cmd.Open openCmd = new org.basex.core.cmd.Open(cont);
                                               String openRes = openCmd.execute(XMLDB.getInstance().getContext());
                                               System.out.println("Rebuild: Open command result: " + openRes);
                                            }
                                        } catch (Exception openEx2) {
                                            System.out.println("Rebuild: failed to set default collection via Open(): " + openEx2);
                                        }
                                        System.out.println("Rebuild: database re-opened using fresh context");
                                    } catch (Exception ex2) {
                                        System.out.println("Rebuild: fresh-context open failed, attempting open by path...");
                                        try {
                                            XMLDB.openDatabase(new File(dbPath, cont).getAbsolutePath());
                                            System.out.println("Rebuild: database opened by path");
                                        } catch (Exception ex3) {
                                            System.out.println("Rebuild: failed to re-open database: " + ex3);
                                            SwingUtilities.invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    InterfaceMain.getInstance().showMessageDialog(
                                                            "Failed to re-open database after rebuild. Please restart the application.",
                                                            "Rebuild Error", JOptionPane.ERROR_MESSAGE);
                                                }
                                            });
                                        }
                                    }
                                } finally {
                                    // Only close tmpCtx if it wasn't adopted by XMLDB.openDatabase(tmpCtx)
                                    if (!tmpCtxAdopted) {
                                        try { new org.basex.core.cmd.Close().execute(tmpCtx); } catch (Exception ignore) {}
                                        try { tmpCtx.close(); } catch (Exception ignore) {}
                                    }
                                }
                                rebuildSuccess.set(true);
                            } finally {
                                // nothing to do here
                            }
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            // --- RESTORE ON FAILURE ---
                            if (backupDir != null && backupDir.exists()) {
                                try {
                                    dbPath = System.getProperty("org.basex.DBPATH");
                                    File dbDir = new File(dbPath, cont);
                                    if (dbDir.exists()) {
                                        deleteRecursive(dbDir);
                                    }
                                    copyDirectory(backupDir, dbDir);
                                    System.out.println("Rebuild: Database restore from backup completed.");
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusField.setText("Rebuild failed. Database restored from backup.");
                                            InterfaceMain.getInstance().showMessageDialog(
                                                    "Database rebuild failed. The previous database has been restored.", "Rebuild Error",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                } catch (Exception restoreEx) {
                                    System.out.println("Rebuild: Failed to restore database from backup: " + restoreEx);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusField.setText("Rebuild failed. Restore from backup also failed.");
                                            InterfaceMain.getInstance().showMessageDialog(
                                                    "Database rebuild failed and restore from backup also failed. Manual intervention required.",
                                                    "Rebuild Error", JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            } else {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusField.setText("Rebuild failed. No backup available.");
                                        InterfaceMain.getInstance().showMessageDialog(
                                                "Database rebuild failed and no backup was available.", "Rebuild Error",
                                                JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            }
                        } finally {
                            // cleanup temp dir if present
                            if (tempDir != null && tempDir.exists()) {
                                try { deleteRecursive(tempDir); } catch (Exception ignored) {}
                            }
                            // If rebuild succeeded, delete backup
                            if (rebuildSuccess.get() && backupDir != null && backupDir.exists()) {
                                try { deleteRecursive(backupDir); } catch (Exception ignored) {}
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    filterDialog.getGlassPane().setVisible(false);
                                    filterDialog.getGlassPane().setCursor(Cursor.getDefaultCursor());
                                    // ensure buttons restored in finally as well
                                    restoreAllButtons.run();
                                    if (rebuildSuccess.get()) {
                                        JOptionPane.showMessageDialog(filterDialog, "Database rebuild is complete.", "Rebuild Complete", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        // --- assemble the Manage DB dialog UI and show it ---
        // Configure button pane
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        // Add 3 pixels to the left of the Add button for extra spacing
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(addButton);
        // half the previous spacing (was 6)
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(removeButton);
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(renameButton);
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(exportButton);
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(rebuildButton);
        // Place Done immediately to the right of Rebuild with half spacing
        buttonPane.add(Box.createHorizontalStrut(3));
        buttonPane.add(doneButton);
        // then push remaining space to the right
        buttonPane.add(Box.createHorizontalGlue());
        // Add 3 pixels to the right for extra spacing
        buttonPane.add(Box.createHorizontalStrut(3));

        // Populate list pane (shows scenarios) and status field
        listPane.add(new JScrollPane(list));
        listPane.add(Box.createVerticalStrut(6));
        listPane.add(statusField);

        // Assemble into dialog
        contentPane.setLayout(new BorderLayout());
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        // Done button closes dialog and refreshes scenario list if changes were made
        doneButton.addActionListener(new ActionListener() {

	public void actionPerformed(ActionEvent e) {
		if (dirtyBit.isDirty()) {
			scns = getScenarios();
			try {
				if (scnList != null)
					scnList.setListData(scns);
			} catch (Exception ex) {
				// ignore UI update issues
			}
		}
		filterDialog.dispose();
	}});

	filterDialog.pack();
	// Make the dialog about 50% taller than the packed size so content has more
	// vertical room
	try{

	java.awt.Dimension dlgSize = filterDialog.getSize();
	// Reduce the computed width by 30 pixels as requested
	int newWidth = Math.max(100, (int) (dlgSize.width * 1.2) - 30);
	int newHeight = (int) (dlgSize.height * 1.5);filterDialog.setSize(newWidth,newHeight);}catch(Exception ignore){
	// ignore any unexpected sizing issues
	}filterDialog.setLocationRelativeTo(parentFrame);filterDialog.setVisible(true);}

	// Helper to delete files/directories recursively
	private static void deleteRecursive(File f) throws IOException {
		if (f == null || !f.exists())
			return;
		if (f.isDirectory()) {
			File[] children = f.listFiles();
			if (children != null) {
				for (File c : children) {
					System.out.println("Rebuild: deleting " + c.getAbsolutePath());
					deleteRecursive(c);
				}
			}
		}
		if (!f.delete()) {
			System.out.println("Rebuild: failed to delete " + f.getAbsolutePath());
			throw new IOException("Failed to delete: " + f.getAbsolutePath());
		} else {
			System.out.println("Rebuild: deleted " + f.getAbsolutePath());
		}
	}

	// Helper to copy files/directories recursively
	private static void copyDirectory(File source, File dest) throws IOException {
    if (source == null || !source.exists()) return;
    if (source.isDirectory()) {
        if (!dest.exists()) {
            if (!dest.mkdirs()) throw new IOException("Failed to create directory: " + dest);
        }
        File[] children = source.listFiles();
        if (children != null) {
            for (File c : children) {
                copyDirectory(c, new File(dest, c.getName()));
            }
        }
    } else {
        java.nio.file.Files.copy(source.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
	 * Exports all open tabs as CSV files to the selected directory.
	 */
	public void exportTabs() {
		final InterfaceMain main = InterfaceMain.getInstance();

		if (tablesTabs.getTabCount() == 0) {
			// error?
			return;
		}

		// select export location
		String exportDialogTitle = "Select Export Directory";
		FileFilter fileFilter = new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}

			public String getDescription() {
				return "Directory to export into";
			}
		};

		FileChooser fc = FileChooserFactory.getFileChooser();
		final File[] exportLocation = fc.doFilePrompt(null, exportDialogTitle, FileChooser.SAVE_DIALOG,
				new File(main.getProperties().getProperty("lastDirectory", ".")), fileFilter);

		if (exportLocation == null) {
			// user canceled, nothing to do
			return;
		}

		ArrayList<String> tab_titles = new ArrayList<String>();

		int tab_count = tablesTabs.getTabCount();
		for (int i = 0; i < tab_count; i++) {
			String tab_title = tablesTabs.getTitleAt(i).replaceAll(" ", "_").replaceAll(":", "");

			// System.out.println("Title:" + tab_title);

			int k = 0;
			for (int j = 0; j < tab_titles.size(); j++) {
				if (tab_titles.get(j).equals(tab_title))
					k++;
			}
			tab_titles.add(tab_title);
			if (k > 0)
				tab_title += ("_" + k);

			Component c = tablesTabs.getComponentAt(i);

			JTable jTable = getJTableFromComponent(c);

			TableModel tm = jTable.getModel();

			if (tm != null) {
				String filename = exportLocation[0].getAbsolutePath() + File.separator + tab_title + ".csv";
				writeTableModelToFile(filename, tm);
			}

		}
		JOptionPane.showMessageDialog(null,
				tab_titles.size() + " files exported to " + exportLocation[0].getAbsolutePath());
	}

	protected boolean arrayListIncludes(ArrayList<String> array, String val) {
		for (int i = 0; i < array.size(); i++) {
			if (val.equals(array.get(i)))
				return true;
		}
		return false;
	}

	/**
	 * Writes the specified TableModel to a CSV file.
	 * <p>
	 * This method writes the column names and all rows of the TableModel to the
	 * given filename in CSV format.
	 *
	 * @param filename The file to write to.
	 * @param tm       The TableModel to export.
	 */
	protected void writeTableModelToFile(String filename, TableModel tm) {
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(filename));

			int no_rows = tm.getRowCount();
			int no_cols = tm.getColumnCount();

			String s = "";
			for (int j = 0; j < no_cols; j++) {
				if (s != "")
					s += ",";
				s += tm.getColumnName(j);
			}
			pw.println(s);

			for (int i = 0; i < no_rows; i++) {
				s = "";
				for (int j = 0; j < no_cols; j++) {
					if (s != "")
						s += ",";
					s += tm.getValueAt(i, j);
				}
				pw.println(s);
			}

			pw.close();

		} catch (Exception e) {
			System.out.println("Problem writing tab to CSV file:" + e);
		}

	}

	protected void batchQuery(File queryFile, final File excelFile) {
		final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		final Vector<ScenarioListItem> tempScns = getScenarios();
		final String singleSheetCheckBoxPropName = "batchQueryResultsInDifferentSheets";
		final String includeChartsPropName = "batchQueryIncludeCharts";
		final String splitRunsPropName = "batchQuerySplitRunsInDifferentSheets";
		final String replaceResultsPropName = "batchQueryReplaceResults";
		Properties prop = InterfaceMain.getInstance().getProperties();

		final String coresToUsePropertyName = "coresToUse";
		Runtime.getRuntime().availableProcessors();
		final int defaultNumCoresToUse = Integer.valueOf(prop.getProperty(coresToUsePropertyName, Integer.toString(2)));
		prop.setProperty(coresToUsePropertyName, Integer.toString(defaultNumCoresToUse));

		// Create a Select Scenarios dialog to get which scenarios to run
		final JList scenarioList = new JList(tempScns);
		final JDialog scenarioDialog = new JDialog(parentFrame, "Select Scenarios to Run", true);
		JPanel listPane = new JPanel();
		JPanel buttonPane = new JPanel();
		final JCheckBox singleSheetCheckBox = new JCheckBox("Place all results in different sheets",
				Boolean.parseBoolean(prop.getProperty(singleSheetCheckBoxPropName, "false")));
		final JCheckBox drawPicsCheckBox = new JCheckBox("Include charts with results",
				Boolean.parseBoolean(prop.getProperty(includeChartsPropName, "true")));
		final JCheckBox seperateRunsCheckBox = new JCheckBox("Split runs into different sheets",
				Boolean.parseBoolean(prop.getProperty(splitRunsPropName, "false")));
		final JButton okButton = new JButton("Ok");
		okButton.setEnabled(false);
		JButton cancelButton = new JButton("Cancel");
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		Container contentPane = scenarioDialog.getContentPane();
		final JCheckBox overwriteCheckBox = new JCheckBox("Overwrite selected file if it exists",
				Boolean.parseBoolean(prop.getProperty(replaceResultsPropName, "false")));

		scenarioList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (scenarioList.isSelectionEmpty()) {
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
				}
			}
		});

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scenarioDialog.dispose();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scenarioList.clearSelection();
				scenarioDialog.dispose();
			}
		});

		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(cancelButton);

		JScrollPane sp = new JScrollPane(scenarioList);
		sp.setPreferredSize(new Dimension(300, 300));
		listPane.add(new JLabel("Select Scenarios:"));
		listPane.add(Box.createVerticalStrut(10));
		listPane.add(sp);
		listPane.add(singleSheetCheckBox);
		listPane.add(overwriteCheckBox);
		listPane.add(drawPicsCheckBox);
		listPane.add(seperateRunsCheckBox);
		listPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(listPane, BorderLayout.PAGE_START);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		scenarioDialog.pack();
		scenarioDialog.setVisible(true);

		if (scenarioList.isSelectionEmpty()) {
			return;
		}
		// save the check box options back into the properties
		prop.setProperty(singleSheetCheckBoxPropName, Boolean.toString(singleSheetCheckBox.isSelected()));
		prop.setProperty(includeChartsPropName, Boolean.toString(drawPicsCheckBox.isSelected()));
		prop.setProperty(splitRunsPropName, Boolean.toString(seperateRunsCheckBox.isSelected()));
		prop.setProperty(replaceResultsPropName, Boolean.toString(overwriteCheckBox.isSelected()));

		// read the batch query file
		Document queries = readQueries(queryFile);
		final NodeList res;
		try {
			res = (NodeList) XPathFactory.newInstance().newXPath().evaluate("/queries/node()", queries,
					XPathConstants.NODESET);
		} catch (Exception e) {
			InterfaceMain.getInstance().showMessageDialog("Could not find queries to run in batch file:\n" + queryFile,
					"Batch Query Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		final int numQueries = res.getLength();
		if (numQueries == 0) {
			InterfaceMain.getInstance().showMessageDialog("Could not find queries to run in batch file:\n" + queryFile,
					"Batch Query Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final Vector<Object[]> toRunScns = new Vector<Object[]>();
		if (!seperateRunsCheckBox.isSelected()) {
			toRunScns.add(scenarioList.getSelectedValuesList().toArray());
		} else {
			for (Object scn : scenarioList.getSelectedValuesList().toArray()) {
				Object[] temp = new Object[1];
				temp[0] = scn;
				toRunScns.add(temp);
			}
		}
		// create window

		// Provide the default set of all regions which does not include Global
		Vector<String> allRegions = new Vector<String>(regions);
		allRegions.remove("Global");

		new BatchWindow(excelFile, toRunScns, allRegions, singleSheetCheckBox.isSelected(),
				drawPicsCheckBox.isSelected(), numQueries, res, overwriteCheckBox.isSelected(), defaultNumCoresToUse);
	}

	/**
	 * Writes the specified Document to a file.
	 * 
	 * @param file   The file to write to.
	 * @param theDoc The Document to write.
	 * @return True if successful, false otherwise.
	 */
	public boolean writeFile(File file, Document theDoc) {
		LSSerializer serializer = implls.createLSSerializer();
		DOMConfiguration domConfig = serializer.getDomConfig();
		boolean prettyPrint = Boolean.parseBoolean(System.getProperty("ModelInterface.pretty-print", "true"));
		domConfig.setParameter("format-pretty-print", prettyPrint);
		// create the serializer and have it print the document

		try {
			LSOutput lsOut = implls.createLSOutput();
			lsOut.setByteStream(new FileOutputStream(file));
			serializer.write(theDoc, lsOut);
		} catch (Exception e) {
			System.err.println("Error outputting tree: " + e);
			return false;
		}
		return true;
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
			// DocumentType DOCTYPE = impl.createDocumentType("recent", "", "");
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
	 * Loads favorite queries file and applies it if requested by the user.
	 */
	public void loadFavoriteQueriesFile() {
		String PathToUse = null;
		if (InterfaceMain.favoriteQueriesFileLocation != null) {
			File f = new File(InterfaceMain.favoriteQueriesFileLocation);
			if (f.exists()) {
				PathToUse = f.getParent();
			}
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select a query file to load.");
		if (PathToUse != null) {
			fileChooser.setCurrentDirectory(new File(PathToUse));
		}
		int returnVal = fileChooser.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			InterfaceMain.favoriteQueriesFileLocation = fileChooser.getSelectedFile().getAbsolutePath();
			String messageFileSaved = fileChooser.getSelectedFile().toString()
					+ " has been loaded.\n\n Would you like to apply it now?";
			int answer = JOptionPane.showConfirmDialog(null, messageFileSaved, "Switch?", JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				selectFavoriteQueries(queryList);
			}
		}
	}

	/**
	 * Creates a file containing the selected queries as user's favorite queries.
	 * 
	 * @param myTree The JTree containing selected queries.
	 */
	public void createFavoriteQueriesFile(JTree myTree) {
		TreePath[] selectedTreePath = myTree.getSelectionPaths();
		if (selectedTreePath.length == 0) {
			JOptionPane.showMessageDialog(null, "Please select at least one query to save.");
			return;
		}

		boolean hasLeaf = false;
		for (TreePath tp : selectedTreePath) {
			if (tp.getLastPathComponent() instanceof QueryGenerator)
				hasLeaf = true;
		}
		if (!hasLeaf) {
			JOptionPane.showMessageDialog(null, "Please select at least one query to save.");
			return;
		}

		String PathToUse = null;
		if (InterfaceMain.favoriteQueriesFileLocation != null) {
			File f = new File(InterfaceMain.favoriteQueriesFileLocation);
			if (f.exists()) {
				PathToUse = f.getParent();
			}
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Specify a favorite queries file to save");
		if (PathToUse != null) {
			fileChooser.setCurrentDirectory(new File(PathToUse));
		}
		int userSelection = fileChooser.showSaveDialog(null);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			try {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(fileChooser.getSelectedFile(), false)));
				String convertedLine = "";
				for (int i = 0; i < selectedTreePath.length; i++) {

					TreePath treePathNow = selectedTreePath[i];
					if (treePathNow.getLastPathComponent() instanceof QueryGenerator) {
						int treePathCount = treePathNow.getPathCount();
						String pathStr = selectedTreePath[i].toString();
						String lineStr = pathStr.substring(1, pathStr.length() - 1);// remove the square brackets
						int commaCount = countCommaInPath(lineStr);
						if (commaCount > treePathCount - 1) { // there are commas inside queryGroup
							convertedLine = convertPathWithCommaToLine(treePathNow);
						} else {
							convertedLine = convertPathToLine(lineStr);
						}
					}
					// System.out.println("converted line is:" + convertedLine);
					writer.write(convertedLine);
					writer.newLine();
				}
				writer.close();
				String messageFileSaved = fileChooser.getSelectedFile().toString()
						+ " has been saved.\n\n Would you like to make this the active favorites file?";
				int answer = JOptionPane.showConfirmDialog(null, messageFileSaved, "Switch?",
						JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					InterfaceMain.favoriteQueriesFileLocation = fileChooser.getSelectedFile().getAbsolutePath();
				}
			} catch (IOException e) {
				System.out.println("Could not save file: " + e.toString());
				JOptionPane.showMessageDialog(null, "Unable to save file, please see console for error",
						"Error Saving File", JOptionPane.ERROR_MESSAGE);
			}

		}

	}// createFavoriteQueriesFile method end

	/**
	 * Appends selected queries to the favorite queries file.
	 * 
	 * @param myTree The JTree containing selected queries.
	 */
	public void appendFavoriteQueries(JTree myTree) {
		TreePath[] selectedTreePath = myTree.getSelectionPaths();
		if (selectedTreePath == null || selectedTreePath.length == 0) {
			JOptionPane.showMessageDialog(null, "Please select at least one query to append.");
			return;
		}
		String favoritesFilePath = InterfaceMain.favoriteQueriesFileLocation;
		if (favoritesFilePath == null || favoritesFilePath.trim().isEmpty()) {
			JOptionPane.showMessageDialog(null,
					"No favorite queries file specified. Please load or create a file first.");
			return;
		}
		File favoritesFile = new File(favoritesFilePath);
		if (!favoritesFile.exists()) {
			String messageFileNotFound = "Favorite queries list file " + favoritesFilePath
					+ " could not be found to be appended to.\nPlease load or create a file first.";
			JOptionPane.showMessageDialog(null, messageFileNotFound);
			return;
		}
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(favoritesFile, true)))) {
			for (TreePath treePathNow : selectedTreePath) {
				int treePathCount = treePathNow.getPathCount();
				String pathStr = treePathNow.toString();
				String lineStr = pathStr.substring(1, pathStr.length() - 1); // remove the square brackets
				int commaCount = countCommaInPath(lineStr);
				String convertedLine;
				if (commaCount > treePathCount - 1) { // there are commas inside queryGroup
					convertedLine = convertPathWithCommaToLine(treePathNow);
				} else {
					convertedLine = convertPathToLine(lineStr);
				}
				writer.write(convertedLine);
				writer.newLine();
			}
			writer.flush();
			String messageQueriesAppended = "The selected queries have been appended to " + favoritesFilePath;
			JOptionPane.showMessageDialog(null, messageQueriesAppended);
		} catch (IOException e) {
			System.out.println("Could not append to favorite queries: " + e);
			JOptionPane.showMessageDialog(null, "Unable to append to file, please see console for error",
					"Error Appending File", JOptionPane.ERROR_MESSAGE);
		}
	}// appendFavoriteQueries method end

	// YD added this method to convert the path string into each line with
	// ">",Feb-2024
	public static String convertPathToLine(String pathStr) {
		return Arrays.stream(pathStr.trim().split("\\s*,\\s*")).map(s -> s.isEmpty() ? s : '"' + s + '"')
				.collect(Collectors.joining(">"));
	}

	// YD added this method to count number of commas in a TreePath,Mar-2024
	public int countCommaInPath(String pathStr) {
		int numCommas = pathStr.length() - pathStr.replace(",", "").length();
		return (numCommas);
	}

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

	public static BaseTableModel getTableModelFromComponent(java.awt.Component comp) {
		Object c;
		try {
			c = ((QueryResultsPanel) comp).getComponent(0);

			if (c instanceof JPanel) {
				return null;
			}
			if (c instanceof JSplitPane) {
				// Dan-Debug - Causes error javax.swing.JPanel cannot be cast to
				// javax.swing.JScrollPane
				return (BaseTableModel) ((TableSorter) ((JTable) ((JScrollPane) ((JSplitPane) c).getLeftComponent())
						.getViewport().getView()).getModel()).getTableModel();
			} else {
				return (BaseTableModel) ((JTable) ((JScrollPane) c).getViewport().getView()).getModel();
			}
		} catch (ClassCastException e) {
			return null;
		}
	}

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

		// all the events we don't care about..
		public void mouseMoved(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

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

	public void runBatch(Node command) {
		/**
		 * Executes a batch command for running queries on the XMLDB database.
		 * <p>
		 * This method parses the provided XML Node command, which should contain
		 * instructions for running batch queries. It supports the following actions:
		 * <ul>
		 * <li>Opening the database file</li>
		 * <li>Loading query files or inline queries</li>
		 * <li>Selecting scenarios to run</li>
		 * <li>Configuring batch options (single sheet, charts, split runs, replace
		 * results, cores to use)</li>
		 * <li>Running the batch and exporting results</li>
		 * </ul>
		 * The method validates required files, opens the database if needed, loads
		 * scenarios and queries, and executes the batch run. If any required
		 * information is missing or an error occurs, it prints the stack trace and
		 * closes the database if it was opened.
		 *
		 * @param command The XML Node containing batch command instructions.
		 */
		// Get properties and batch options
		Properties prop = InterfaceMain.getInstance().getProperties();
		final String singleSheetCheckBoxPropName = "batchQueryResultsInDifferentSheets";
		final String includeChartsPropName = "batchQueryIncludeCharts";
		final String splitRunsPropName = "batchQuerySplitRunsInDifferentSheets";
		final String replaceResultsPropName = "batchQueryReplaceResults";
		final String coresToUsePropertyName = "coresToUse";
		Runtime.getRuntime().availableProcessors();
		final int defaultNumCoresToUse = Integer.valueOf(prop.getProperty(coresToUsePropertyName, Integer.toString(2)));
		prop.setProperty(coresToUsePropertyName, Integer.toString(defaultNumCoresToUse));

		NodeList children = command.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node child = children.item(i);
			// Only process element nodes
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			String actionCommand = ((Element) child).getAttribute("name");
			if (actionCommand == null) {
				continue;
			}
			// Only handle XMLDB Batch File commands
			if ("XMLDB Batch File".equals(actionCommand)) {
				File queryFile = null;
				Node queriesNode = null;
				File outFile = null;
				String dbFile = null;
				boolean didOpenDB = false;
				List<DataPair<String, String>> scenariosNames = new ArrayList<>();
				// Batch options
				boolean singleSheet = Boolean.parseBoolean(prop.getProperty(singleSheetCheckBoxPropName, "false"));
				boolean includeCharts = Boolean.parseBoolean(prop.getProperty(includeChartsPropName, "true"));
				boolean splitRuns = Boolean.parseBoolean(prop.getProperty(splitRunsPropName, "false"));
				boolean replaceResults = Boolean.parseBoolean(prop.getProperty(replaceResultsPropName, "false"));
				int numCoresToUse = defaultNumCoresToUse;
				// Parse child nodes for batch file configuration
				NodeList fileNameChildren = child.getChildNodes();
				for (int j = 0; j < fileNameChildren.getLength(); ++j) {
					Node fileNode = fileNameChildren.item(j);
					if (fileNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					String nodeName = fileNode.getNodeName();
					switch (nodeName) {
					case "queryFile":
						queryFile = new File(fileNode.getTextContent());
						break;
					case "queries":
						queriesNode = fileNode;
						break;
					case "outFile":
						outFile = new File(fileNode.getTextContent());
						break;
					case "xmldbLocation":
						dbFile = fileNode.getTextContent();
						break;
					case "scenario":
						scenariosNames.add(new DataPair<>(((Element) fileNode).getAttribute("name"),
								((Element) fileNode).getAttribute("date")));
						break;
					case singleSheetCheckBoxPropName:
						singleSheet = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case includeChartsPropName:
						includeCharts = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case splitRunsPropName:
						splitRuns = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case replaceResultsPropName:
						replaceResults = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case coresToUsePropertyName:
						try {
							numCoresToUse = Integer.parseInt(fileNode.getTextContent());
						} catch (NumberFormatException ex) {
							numCoresToUse = defaultNumCoresToUse;
						}
						break;
					default:
						System.out.println("Unknown tag: " + nodeName);
						break;
					}
				}
				try {
					// Validate required files
					if ((queryFile == null && queriesNode == null) || outFile == null || dbFile == null) {
						throw new Exception("Not enough information provided to run batch query.");
					}
					// Open DB if needed
					if (XMLDB.getInstance() == null) {
						XMLDB.openDatabase(dbFile);
						didOpenDB = true;
					}
					// Get scenarios from DB
					Vector<ScenarioListItem> scenariosInDb = getScenarios();
					Vector<ScenarioListItem> scenariosToRun = new Vector<>();
					if (scenariosNames.isEmpty() && !scenariosInDb.isEmpty()) {
						scenariosToRun.add(scenariosInDb.lastElement());
					} else {
						for (DataPair<String, String> currScn : scenariosNames) {
							String scen = currScn.getKey();
							String date = currScn.getValue();
							if (date.isEmpty()) {
								date = null;
							}
							ScenarioListItem found = ScenarioListItem.findClosestScenario(scenariosInDb, scen, date);
							if (found != null) {
								scenariosToRun.add(found);
							}
						}
					}
					if (scenariosToRun.isEmpty()) {
						throw new Exception("Could not find scenarios to run.");
					}
					// Load queries from file or inline
					if (queryFile != null && queriesNode != null) {
						throw new Exception("Setting both a queryFile and inline queries is not allowed.");
					} else if (queryFile != null) {
						queriesNode = readQueries(queryFile).getDocumentElement();
					} else {
						filterNodes(queriesNode, new ParseFilter());
					}
					// Get queries to run
					final NodeList res = (NodeList) XPathFactory.newInstance().newXPath().evaluate("./aQuery",
							queriesNode, XPathConstants.NODESET);
					final int numQueries = res.getLength();
					if (numQueries == 0) {
						throw new Exception("Could not find queries to run.");
					}
					// Prepare scenarios to run
					final Vector<Object[]> toRunScns = new Vector<>();
					if (!splitRuns) {
						toRunScns.add(scenariosToRun.toArray());
					} else {
						for (ScenarioListItem scn : scenariosToRun) {
							Object[] temp = new Object[1];
							temp[0] = scn;
							toRunScns.add(temp);
						}
					}
					// Get regions (excluding Global)
					Vector<String> allRegions = getRegions();
					allRegions.remove("Global");
					// Run the batch window
					BatchWindow runner = new BatchWindow(outFile, toRunScns, allRegions, singleSheet, includeCharts,
							numQueries, res, replaceResults, numCoresToUse);
					if (runner != null) {
						runner.waitForFinish();
					}
				} catch (Exception e) {
					// Print stack trace for errors
					e.printStackTrace();
				} finally {
					// Close DB if it was opened in this method
					if (didOpenDB) {
						XMLDB.closeDatabase();
					}
				}
			} else {
				// Unknown command type
				System.out.println("Unknown command: " + actionCommand);
			}
		}
	}

	private void finalizeUI() {
		JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		if (parentFrame == null)
			return;
		Container contentPane = parentFrame.getContentPane();
		contentPane.removeAll();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tableCreatorSplit, BorderLayout.CENTER);
		// Remove pack() to avoid overriding setSize from InterfaceMain
		// parentFrame.pack();
		parentFrame.setLocationRelativeTo(null);
		parentFrame.setVisible(true);
		parentFrame.getGlassPane().setVisible(false);
	}
}
