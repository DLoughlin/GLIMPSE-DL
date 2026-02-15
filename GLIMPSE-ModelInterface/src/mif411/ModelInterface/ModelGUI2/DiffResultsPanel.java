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
package ModelInterface.ModelGUI2;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import ModelInterface.InterfaceMain;
import ModelInterface.UnitConversionInstance;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.tables.ComboTableModel;
import ModelInterface.ModelGUI2.tables.CopyPaste;
import ModelInterface.ModelGUI2.xmldb.DbProcInterrupt;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import filter.FilteredTable;

/**
 * Adds capability of running many queries parallel and will display the results
 * after the queries are run as well as change the icon to indicate to the user
 * that it is done running.
 * 
 * Adds capability of running many queries parallel and will display the results
 * after the queries are run as well as change the icon to indicate to the user
 * that it is done running.
 * 
 * Author Action Date Flag
 * ============================================================ DHL creates diff
 * panel 1/2/2021 @1
 */

public class DiffResultsPanel extends QueryResultsPanel {

	// Not used currently
	protected double min_val = 0.001;
	protected double min_pct = 0.1;
	protected boolean use_val_filter = true;
	protected boolean use_pct_filter = true;
	protected String base_scenario = "";
	protected boolean show_pct_diff = false;

	/**
	 * Referring to the thread that is running. Used to track which thread is being
	 * used/closed
	 */
	Thread runThread;

	/** The context for running queries which can be used to cancel it */
	DbProcInterrupt context = null;

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new query results panel.
	 * 
	 * @param qg                 the query generator
	 * @param singleBinding      The single query binding which will filter the
	 *                           results from qg, or null if the user did not select
	 *                           a single query.
	 * @param scenarioListValues list of selected scenarios
	 * @param regionListValues   Regions to be used.
	 * @param icon               The icon that will be changing.
	 */
	public DiffResultsPanel(final QueryGenerator qg, final QueryBinding singleBinding,
			final Object[] scenarioListValues, final Object[] regionListValues, final TabCloseIcon icon,
			final boolean generateTotals) {
		super();
		initializeWaiting();
		context = new DbProcInterrupt();
		final DiffResultsPanel thisThread = this;
		runThread = new Thread() {
			public void run() {
				try {
					JComponent ret = null;
					String errorMessage = null;
					// do computations, return a JComponent
					try {
						ret = createSingleTableContent(qg, singleBinding, scenarioListValues, regionListValues,
							generateTotals);
					} catch (Exception e) {
						System.out.println("Error creating diff table:" + e);
						errorMessage = e.getMessage();
					}
					// Stop process if the user terminated the process
					if (isInterrupted()) {
						return;
					}

					// clear the text box in preparation of adding the new component
					removeAll();

					// icon is changed to the finished state
					icon.finishedLoading();

					// error message displayed
					if (ret == null) {
						JPanel tempPanel = new JPanel();
						tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));
						tempPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
						tempPanel.add(new JLabel(errorMessage));
						add(tempPanel);
					}
					// the new JPanel is added where the text box was
					else {
						setLayout(new BoxLayout(thisThread, BoxLayout.X_AXIS));
						add(ret);
					}
					// the panel is refreshed to show the changes
					revalidate();
					
					final JComponent finalRet = ret;
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (InterfaceMain.autoGenerateGraphics) {
								if (finalRet instanceof JSplitPane) {
									JSplitPane sp = (JSplitPane) finalRet;
									// Check if we should graph based on row count
									boolean shouldGraph = true;
									
									// Try to inspect the table to get row count
									JTable table = thisThread.getJTableFromComponent(thisThread);
									if (table != null && table.getRowCount() >= 150) {
										shouldGraph = false;
										System.out.println("Auto-graphics skipped: Result has " + table.getRowCount() + " rows (limit is 30).");
									} else if (table != null) {
										System.out.println("Auto-graphics proceeding: Result has " + table.getRowCount() + " rows.");
									}

									if (shouldGraph && sp.getLeftComponent() instanceof JPanel) {
										JPanel jp = (JPanel) sp.getLeftComponent();
										clickGraphButton(jp);
									} else {
										//System.out.println("Auto-graphics: Left component is not JPanel or shoudGraph is false.");
									}
								} else {
									//System.out.println("Auto-graphics: result is not JSplitPane");
								}
							}
						}
					});
				} finally {
					// Notify DbViewer that this query has completed regardless of outcome
					DbViewer.registerQueryCompleted();
				}
			}

			private void clickGraphButton(java.awt.Container container) {
				for (java.awt.Component comp : container.getComponents()) {
					if (comp instanceof JButton) {
						JButton button = (JButton) comp;
						if ("Graph".equals(button.getText())) {
							System.out.println("Auto-graphics: Found Graph button, clicking...");
							button.doClick();
							return;
						}
					} else if (comp instanceof java.awt.Container) {
						clickGraphButton((java.awt.Container) comp);
					}
				}
			}
		};
		runThread.start();
	}

	/**
	 * Creates the single table content.
	 * 
	 * @param qg                 the qg
	 * @param singleBinding
	 * @param scenarioListValues
	 * @param regionListValues
	 * 
	 * 
	 * @return a JComponent with the results in it as a JScrollingPane
	 * @throws Exception thrown if the multiTableModel returns an invalid result
	 * 
	 */
	private JComponent createSingleTableContent(QueryGenerator qg, QueryBinding singleBinding,
			final Object[] scenarioListValues, final Object[] regionListValues, final boolean generateTotals)
			throws Exception {
		// aseTableModel bt = new ComboTableModel(qg, scenarioListValues,
		// regionListValues, singleBinding, context);

		final InterfaceMain main = InterfaceMain.getInstance(); // @1
		JSplitPane sp = new JSplitPane();
		// BaseTableModel
		ComboTableModel bt = new ComboTableModel(qg, scenarioListValues, regionListValues, singleBinding, context);

		JTable jTable = null;

		if (generateTotals) {
			jTable = new JTable(convertToDiffTable(convertToSumTable(bt)));
		} else {
			// jTable = bt.getAsSortedTable();
			jTable = new JTable(convertToDiffTable(bt)); // Dan: Attempt to fix recent bug 20220607
		}

		new CopyPaste(jTable);
		jTable.setCellSelectionEnabled(true);

		javax.swing.table.TableColumn col;
		int j = 0;
		while (j < jTable.getColumnCount()) {
			col = jTable.getColumnModel().getColumn(j);
			if (jTable.getColumnName(j).equals("")) {
				col.setPreferredWidth(75);
			} else {
				col.setPreferredWidth(jTable.getColumnName(j).length() * 5 + 30);
			}
			j++;
		}

		Properties props = main.getProperties();// @1
		// for restore legend information
		//String path = System.getProperty("user.dir") + "\\LegendBundle.properties";
		String path=InterfaceMain.legendBundlesLoc;
		// props.getProperty("lastDirectory") + "\\LegendBundle.properties"; //@1
		// String[] units = getUnit(qg, (String) jTable.getValueAt(0,
		// jTable.getColumnCount() - 1)); //@
		
		if(DbViewer.enableUnitConversions) {
			convertUnits(qg,jTable);
		}
		
		String[][] units = getUnits(qg, jTable);
		new FilteredTable(null, qg.toString(), // @1 //Dan changed from FilteredTable and added getUnits
				units[0], path, // @1
				jTable, sp); // @1

		main.fireProperty("Query", null, bt); // @1
		return sp;

	}

	private HashMap<String, ArrayList<UnitConversionInstance>> getUnitInformation() {
		//no need to run if there is no file
		if(InterfaceMain.unitFileLocation==null || InterfaceMain.unitFileLocation.length()==0) {
			return null;
		}
	
		HashMap<String, ArrayList<UnitConversionInstance>> localInfo=new HashMap<>();
		String[] parsedLine=null;
		//expected items in string
		String toUnit=null;
		String fromUnit=null;
		Double conversionFactor=null;
		String query=null;
		String headingOne=null;
		String valueOne=null;
		String headingTwo=null;
		String valueTwo=null;
		String headingThree=null;
		String valueThree=null;
		String headingFour=null;
		String valueFour=null;
		String unitsFileName = InterfaceMain.unitFileLocation;
		Path myPath = null;
		InputStream unitsStream = null;
		try {
			File unitsFile = new File(unitsFileName);
			if (unitsFile.exists()) {
				myPath = unitsFile.toPath();
			} else {
				unitsStream = getClass().getClassLoader().getResourceAsStream(unitsFileName);
				if (unitsStream == null) {
					throw new FileNotFoundException("Units file not found: " + unitsFileName);
				}
			}
			List<String> lines;
			if (myPath != null) {
				lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);
			} else {
				BufferedReader reader = new BufferedReader(new InputStreamReader(unitsStream, StandardCharsets.UTF_8));
				lines = reader.lines().collect(Collectors.toList());
			}
			//first line is header
			for(int curLine=1;curLine<lines.size();curLine++) {
				if(lines.get(curLine).startsWith("#")) {
					continue;
				}
				parsedLine=lines.get(curLine).split(",");
				for(int i=0;i<parsedLine.length;i++) {
					parsedLine[i]=parsedLine[i].replace("\"", "").trim();
				}
				if(!localInfo.containsKey(parsedLine[0])) {
					localInfo.put(parsedLine[0], new ArrayList<UnitConversionInstance>());
				}
				//assume each line has a toUnit and Conversion
				if(parsedLine.length>=3) {
					fromUnit=parsedLine[0];
					toUnit=parsedLine[1];
					conversionFactor=Double.parseDouble(parsedLine[2]);
				}
				//check for entry in query field, set it null if blank
				if(parsedLine.length>=4 && parsedLine[3].length()>0) {
					query=parsedLine[3];
				}else {
					query=null;
				}
				
				//check to make sure this query has match in system
				
				
				//check for heading 1 and value, setting them both to null if there is no value for either
				if(parsedLine.length>=6 && parsedLine[4].length()>0&& parsedLine[5].length()>0) {
					headingOne=parsedLine[4];
					valueOne=parsedLine[5];
				}else {
					headingOne=null;
					valueOne=null;
				}
				//check for heading 2 and value, setting them both to null if there is no value for either
				if(parsedLine.length>=8 && parsedLine[6].length()>0&& parsedLine[7].length()>0) {
					headingTwo=parsedLine[6];
					valueTwo=parsedLine[7];
				}else {
					headingTwo=null;
					valueTwo=null;
				}
				//check for heading 3 and value, setting them both to null if there is no value for either
				if(parsedLine.length>=10 && parsedLine[8].length()>0&& parsedLine[9].length()>0) {
					headingThree=parsedLine[8];
					valueThree=parsedLine[9];
				}else {
					headingThree=null;
					valueThree=null;
				}
				//check for heading 4 and value, setting them both to null if there is no value for either
				if(parsedLine.length>=12 && parsedLine[10].length()>0&& parsedLine[11].length()>0) {
					headingFour=parsedLine[10];
					valueFour=parsedLine[11];
				}else {
					headingFour=null;
					valueFour=null;
				}
				
				localInfo.get(parsedLine[0]).add(new UnitConversionInstance(fromUnit,toUnit,conversionFactor,query,
						headingOne,valueOne,headingTwo,valueTwo,headingThree,valueThree,headingFour,valueFour));
			}
		}catch(Exception e) {
			System.out.println("Could not read units file: "+e.toString());
		}
		return localInfo;
	}

	
	private void convertUnits(QueryGenerator qg, JTable table) {
		HashMap<String,ArrayList<UnitConversionInstance>>unitLookup=getUnitInformation();
		//only run if there is something to convert.
		if(unitLookup==null) {
			return;
		}
		ArrayList<UnitConversionInstance> possibleConversions=null;
		UnitConversionInstance uci=null;
		int num_rows = table.getRowCount();
		int num_cols = table.getColumnCount();
		Double tableVal=null;
		UnitConversionInstance bestUCIMatch=null;
		HashMap<String,Integer> headerLookup=new HashMap<>();
		TableModel tm=table.getModel();
		for (int i = 0; i < table.getColumnCount(); i++) {
           headerLookup.put(tm.getColumnName(i),i);
        }
		String headerComp=null;
		String valComp=null;
		//get all the unique units in this table
		HashSet<String> uniqueUnits=new HashSet<String>();
		for(int i=0;i<num_rows;i++) {
			uniqueUnits.add( (String) table.getValueAt(i, num_cols - 1));
		}
		//make a list of possible conversion objects:
		ArrayList<UnitConversionInstance> conversionOpts=new ArrayList<UnitConversionInstance>();
		for(String unit:uniqueUnits) {
			possibleConversions=unitLookup.get(unit);
			if(possibleConversions==null) {
				continue;
			}
			//to narrow down list, see if this query is our query
			for(UnitConversionInstance uciCheck:possibleConversions) {
				//not all conversions have a query specified
				if(uciCheck.getQuery()==null || uciCheck.getQuery().trim().length()==0) {
					conversionOpts.add(uciCheck);
				}else if(uciCheck.getQuery().compareToIgnoreCase(qg.toString().trim())==0) {
					conversionOpts.add(uciCheck);
				}
			}
		}
		//from this short list, make a specificity level
		HashMap<Integer,ArrayList<UnitConversionInstance>> convertBySpec=new HashMap<>();
		int biggestRank=-1;
		int myRank=0;
		for(UnitConversionInstance toSort:conversionOpts) {
			myRank=0;
			//we want queries to have elevated importance, so give them two points
			if(toSort.getQuery()!=null && toSort.getQuery().trim().length()>0) {
				myRank++;
				myRank++;
			}
			if(toSort.getHeadingOne()!=null && toSort.getHeadingOne().trim().length()>0) {
				myRank++;
			}
			if(toSort.getHeadingTwo()!=null && toSort.getHeadingTwo().trim().length()>0) {
				myRank++;
			}
			if(toSort.getHeadingThree()!=null && toSort.getHeadingThree().trim().length()>0) {
				myRank++;
			}
			if(toSort.getHeadingFour()!=null && toSort.getHeadingFour().trim().length()>0) {
				myRank++;
			}
			if(myRank>biggestRank) {
				biggestRank=myRank;
			}
			if(!convertBySpec.containsKey(myRank)) {
				convertBySpec.put(myRank, new ArrayList<UnitConversionInstance>());
			}
			convertBySpec.get(myRank).add(toSort);
		}
		
		
		if(qg!=null) {
			for (int i = 0; i < num_rows; i++) {
				bestUCIMatch=null;
				//look from most specific to least specific.  Only perfect matches count
				for(int rank=biggestRank;rank>=0;rank--) {
					if(convertBySpec.containsKey(rank)) {
						for(UnitConversionInstance isPer:convertBySpec.get(rank)) {
							if(isPerfectMatchUnitConvOpt(isPer,i,table,headerLookup)) {
								bestUCIMatch=isPer;
							}
							if(bestUCIMatch!=null) {
								break;
							}
						}
						if(bestUCIMatch!=null) {
							break;
						}
					}
					
					
				}
				
				
				if(bestUCIMatch!=null) {
					table.setValueAt(bestUCIMatch.getToUnit()+"", i, num_cols-1);
					Double toMult=bestUCIMatch.getConversionFactor();
					if(toMult != null) {
						//assume first two columns never have results
						for(int curCol=0;curCol<num_cols;curCol++) {
							try {
								tableVal=Double.parseDouble(table.getValueAt(i, curCol).toString());
							}catch(Exception e) {
								//only care it's not a number
								tableVal=null;
							}
							if(tableVal!=null) {
								table.setValueAt((tableVal*toMult), i, curCol);
								//table.setValueAt(Math.random(), i, curCol);
							}
							
						}
					}
					
					
					
					
				}
			}
		}
		
	}

	private boolean isPerfectMatchUnitConvOpt(UnitConversionInstance uci, int i, JTable table, HashMap<String, Integer> headerLookup) {
		//note that we don't test query here because it is selected against when culling options above
		
		//do need to check unit, as different rows can have different units
		if(uci.getFromUnit().compareToIgnoreCase(table.getValueAt(i, table.getColumnCount()-1).toString())!=0){
			return false;
		}
		
		
		//assume this is a regex in all comparisons below
		 //now heading one
		String valComp=null;
		int curHdrIdx=-1;
		String regEx=null;
		if(uci.getHeadingOne()!=null && uci.getHeadingOne().trim().length()>0) {
			curHdrIdx=getIdxGivenKey(uci.getHeadingOne(),headerLookup);
			if(curHdrIdx>=0) {
				valComp=table.getValueAt(i, curHdrIdx).toString();
				regEx="\\Q" + uci.getValueOne().toLowerCase().replace("*", "\\E.*?\\Q") + "\\E";
				if(!Pattern.matches(regEx, valComp.toLowerCase()))
				{
					return false;
				}
			}
			
			
		}
		
		//now heading two
		if(uci.getHeadingTwo()!=null && uci.getHeadingTwo().trim().length()>0) {
			curHdrIdx=-1;
			curHdrIdx=getIdxGivenKey(uci.getHeadingTwo(),headerLookup);
			if(curHdrIdx>=0) {
				valComp=table.getValueAt(i, curHdrIdx).toString();
				regEx="\\Q" + uci.getValueTwo().toLowerCase().replace("*", "\\E.*?\\Q") + "\\E";
				if(!Pattern.matches(regEx, valComp.toLowerCase()))
				{
					return false;
				}
			}
		}
		//now heading three
		if(uci.getHeadingThree()!=null && uci.getHeadingThree().trim().length()>0) {
			curHdrIdx=-1;
			curHdrIdx=getIdxGivenKey(uci.getHeadingThree(),headerLookup);
			if(curHdrIdx>=0) {
				
				valComp=table.getValueAt(i, curHdrIdx).toString();
				regEx="\\Q" + uci.getValueThree().toLowerCase().replace("*", "\\E.*?\\Q") + "\\E";
				
				if(!Pattern.matches(regEx, valComp.toLowerCase()))
				{
					return false;
				}
			}
		}
		//now four
		if(uci.getHeadingFour()!=null && uci.getHeadingFour().trim().length()>0) {
			curHdrIdx=-1;
			curHdrIdx=getIdxGivenKey(uci.getHeadingFour(),headerLookup);
			if(curHdrIdx>=0) {
				valComp=table.getValueAt(i, curHdrIdx).toString();
				regEx="\\Q" + uci.getValueFour().toLowerCase().replace("*", "\\E.*?\\Q") + "\\E";
				
				if(!Pattern.matches(regEx, valComp.toLowerCase()))
				{
					return false;
				}
			}
		}
		return true;
	}


	private int getIdxGivenKey(String UCIVal, HashMap<String, Integer> headerLookup) {
		for(String key:headerLookup.keySet()) {
			if(Pattern.matches(UCIVal, key)) {
				return headerLookup.get(key);
			}
		}
		return -1;
	}

	//YD edited, Sep-2024, use the public staic method in "QueryResultsPanel" instead
//	private String[][] getUnits(QueryGenerator qg, JTable table) {
//		int last_col = table.getColumnCount() - 1;
//		int num_rows = table.getRowCount();
//		String[][] units = new String[num_rows][2];
//
//		if (qg != null) {
//			String item0 = qg.getAxis2Name();
//			for (int i = 0; i < num_rows; i++) {
//				units[i][0] = qg.getAxis2Name();
//				String u = (String) table.getValueAt(i, table.getColumnCount() - 1);
//				// if (this.show_pct_diff) u="pct";
//				units[i][1] = qg.getVariable() + " (" + u + ")";
//			}
//		}
//		return units;
//	}

	private DefaultTableModel convertToDiffTable(TableModel tm) {

		// gets current headers
		ArrayList<String> headers = getHeaders(tm);

		DefaultTableModel dtm = new DefaultTableModel();
		Object[] obj = new Object[tm.getColumnCount()];

		for (int i = 0; i < tm.getColumnCount(); i++) {
			dtm.addColumn(tm.getColumnName(i));
		}

		// gets scenario names
		ArrayList<String> scenarios = getListFromCol(0, tm);
		base_scenario = scenarios.get(0);
		showDiffOptions(scenarios);
		if (base_scenario == null)
			return null;

		// gets region names
		ArrayList<String> regions = getListFromCol(1, tm);

		// gets key columns
		ArrayList<Integer> keys = getKeys(headers);
		ArrayList<Integer> years = getYears(headers);

		// returns unique combos of keys
		ArrayList<String> uniquevals = getListFromCols(keys, tm);
		ArrayList<String> tablekeys = getTableKeys(keys, tm);

		for (int s = 0; s < scenarios.size(); s++) {
			String scenario = scenarios.get(s);
			if (!scenario.equals(base_scenario)) {
				for (int r = 0; r < regions.size(); r++) {
					String region = regions.get(r);
					for (int u = 0; u < uniquevals.size(); u++) {

						int match_base = -1;
						int match_alt = -1;
						int match_back = -1;

						String uniqueval = uniquevals.get(u);
						String base_key_string = base_scenario + "," + region + "," + uniqueval;
						String alt_key_string = scenario + "," + region + "," + uniqueval;
						// String backstop_key_string=region+","+uniqueval;

						for (int i = 0; i < tablekeys.size(); i++) {
							if (base_key_string.equals(tablekeys.get(i)))
								match_base = i;
							if (alt_key_string.equals(tablekeys.get(i)))
								match_alt = i;
							// if (tablekeys.get(i).indexOf(backstop_key_string)>=1) match_back=i;
						}

//						if (match_base==-1) {
//							System.out.println("no match on base!"+base_key_string);
//						}
//						if (match_alt==-1) {
//							System.out.println("no match on alt!"+alt_key_string);
//						}						
						double[] base_data = getTableRowData(match_base, tm, years);
						double[] alt_data = getTableRowData(match_alt, tm, years);
						double[] diff_data = getDiffData(alt_data, base_data);

//						if (match_base==match_alt) diff_data=base_data;
//						
//						if ((match_base==-1)&&(match_alt==-1)) {
//							match_alt=match_back;
//						}
//						//Dan: added this last catch so it won't show Base results in query

						Object[] row_obj = constructRow(scenario, tm, match_alt, match_base, base_data, alt_data,
								diff_data, years);

						if (row_obj != null)
							dtm.addRow(row_obj);

					}
				}
			}
		}

		return dtm;
	}

	private Object[] constructRow(String scenario, TableModel tm, int index_alt, int index_base, double[] base_data,
			double[] alt_data, double[] diff_data, ArrayList<Integer> yr) {
		Object[] obj = new Object[tm.getColumnCount()];

		int index = index_base;

		if ((index_base == -1) && (index_alt == -1))
			return null;

		if (index_base == -1)
			index = index_alt;

		obj[0] = scenario.substring(0, scenario.indexOf(",")) + " minus "
				+ base_scenario.substring(0, base_scenario.indexOf(","));
		for (int i = 1; i < tm.getColumnCount(); i++) {
			obj[i] = tm.getValueAt(index, i);
			if (i == tm.getColumnCount() - 1) {
				if (this.show_pct_diff)
					obj[i] = "pct (" + obj[i] + ")";
			}
		}

		for (int i = 0; i < diff_data.length; i++) {
			int loc = yr.get(i).intValue();
			obj[loc] = new Double(diff_data[i]);
		}

		if ((use_val_filter) || (use_pct_filter)) {
			if (!okToUse(base_data, alt_data, diff_data))
				obj = null;
		}
		return obj;
	}

	private boolean okToUse(double[] base_data,double[] alt_data,double[] diff_data) {
		boolean ok_to_use=true;
		boolean ok_to_use_val=true;
		boolean ok_to_use_pct=true;

		//Dan: This section is a bit messy and could easily be cleaned up and/or combined with getDiffData
		
			if (use_val_filter) {
				ok_to_use_val=false;
				for (int i=0;i<diff_data.length;i++) {
					double val=0.0;
					if (this.show_pct_diff) {
						val=Math.abs(alt_data[i]-base_data[i]);
					} else {
						val=Math.abs(diff_data[i]);
					}
					if (val>min_val) { 
						ok_to_use_val=true;
						break;
					}
				}
				ok_to_use=ok_to_use_val;
			}
			
			if ((ok_to_use)&&(use_pct_filter)) {
				ok_to_use_pct=false;
				for (int i=0;i<base_data.length;i++) {
					if ((base_data[i]==0.0)&&(alt_data[i]!=0.0)) {
						ok_to_use_pct=true;
						break;
					} else {
						if ((base_data[i]!=0.0)&&(alt_data[i]!=0.0)){
							double val=0.0;
							if (this.show_pct_diff) { 
								val=Math.abs(diff_data[i]);
							} else {
								val=Math.abs(diff_data[i]/base_data[i])*100.0;
							}

							if (val>min_pct) { 
								ok_to_use_pct=true;
								break;
							}
						}
					}
				}
				ok_to_use=ok_to_use_pct;
			}
		
		return ok_to_use;

	}

	private double[] getDiffData(double[] alt, double[] base) {
		double[] rtn_vals = new double[alt.length];

		if (this.show_pct_diff) {
			for (int i = 0; i < rtn_vals.length; i++) {
				// Dan: need to check what happens if divide by zero
				if (base[i] == 0.0) {
					rtn_vals[i] = Double.NaN;//explicilty handle case, other code does display
				} else {
					rtn_vals[i] = (alt[i] - base[i]) / base[i] * 100.0;
				}
			}
		} else {
			for (int i = 0; i < rtn_vals.length; i++) {
				rtn_vals[i] = alt[i] - base[i];
			}
		}

		return rtn_vals;
	}

	private double[] getTableRowData(int row, TableModel tm, ArrayList<Integer> years) {
		double[] rtn_array = new double[years.size()];

		for (int i = 0; i < rtn_array.length; i++) {
			double val = 0;
			if (row == -1) {
				val = 0.0;
			} else {
				String temp = "" + tm.getValueAt(row, years.get(i).intValue());
				try {
					val = Double.parseDouble(temp);
				} catch (Exception e) {
					val = -9;
				}
			}
			rtn_array[i] = val;
		}

		return rtn_array;
	}

	private double[][] getTableData(ArrayList<Integer> years, TableModel tm) {
		double[][] rtn_matrix = new double[tm.getRowCount()][years.size()];

		for (int row = 0; row < tm.getRowCount(); row++) {
			for (int coli = 0; coli < years.size(); coli++) {
				int col = years.get(coli);
				String sval = "" + tm.getValueAt(row, col);
				rtn_matrix[row][coli] = Double.valueOf(sval);
			}
		}

		return rtn_matrix;
	}

	private ArrayList<String> getTableKeys(ArrayList<Integer> keys, TableModel tm) {
		ArrayList<String> rtn_list = new ArrayList<String>();

		for (int i = 0; i < tm.getRowCount(); i++) {
			String str = tm.getValueAt(i, 0) + "," + tm.getValueAt(i, 1);

			for (int j = 0; j < keys.size(); j++) {
				str += "," + tm.getValueAt(i, keys.get(j).intValue());
			}
			rtn_list.add(str);
		}
		return rtn_list;
	}

	private ArrayList<String> getListFromCol(int col, TableModel tm) {
		ArrayList<String> rtn_list = new ArrayList<String>();

		for (int row = 0; row < tm.getRowCount(); row++) {
			String temp = "" + tm.getValueAt(row, col);
			boolean match = false;

			for (int i = 0; i < rtn_list.size(); i++) {
				if (rtn_list.get(i).equals(temp)) {
					match = true;
					break;
				}
			}
			if (!match) {
				rtn_list.add(temp);
			}
		}

		return rtn_list;
	}

	private ArrayList<String> getListFromCols(ArrayList<Integer> keys, TableModel tm) {
		ArrayList<String> combos = new ArrayList<String>();

		for (int row = 0; row < tm.getRowCount(); row++) {
			String item = "";
			for (int coli = 0; coli < keys.size(); coli++) {
				int col = keys.get(coli).intValue();
				if (coli != 0) {
					item += ",";
				}
				item += tm.getValueAt(row, col);
			}

			boolean match = false;

			for (int i = 0; i < combos.size(); i++) {
				if (item.equals(combos.get(i))) {
					match = true;
					break;
				}
			}
			if (!match) {
				combos.add(item);
			}
		}

		return combos;
	}

	private ArrayList<String> getHeaders(TableModel jm) {
		ArrayList<String> header_list = new ArrayList<String>();

		for (int col = 0; col < jm.getColumnCount(); col++) {
			header_list.add("" + jm.getColumnName(col));
		}

		return header_list;
	}

	private String getBaseScen(TableModel tm) {
		String rtn_str = null;

		rtn_str = "" + tm.getValueAt(0, 0);

		return rtn_str;
	}

	private ArrayList<Integer> getKeys(ArrayList<String> headers) {
		ArrayList<Integer> rtn_list = new ArrayList<Integer>();

		// gets list of non-number (e.g., year) columns
		for (int col = 0; col < headers.size(); col++) {
			String temp = headers.get(col);
			if ((!temp.equals("scenario")) && (!temp.equals("region"))) {
				boolean isnum = false;
				try {
					double d = Double.parseDouble(temp);
					isnum = true;
				} catch (Exception e) {
					isnum = false;
				}
				if (!isnum) {
					rtn_list.add(col);
				}
			}
		}

		return rtn_list;
	}

	private ArrayList<Integer> getYears(ArrayList<String> headers) {
		ArrayList<Integer> rtn_list = new ArrayList<Integer>();

		// gets list of years in table
		for (int col = 0; col < headers.size(); col++) {
			String temp = headers.get(col);
			if ((!temp.equals("scenario")) && (!temp.equals("region"))) {
				boolean isnum = false;
				try {
					double d = Double.parseDouble(temp);
					isnum = true;
				} catch (Exception e) {
					isnum = false;
				}
				if (isnum) {
					rtn_list.add(col);
				}
			}
		}

		return rtn_list;
	}

	// @1
	private String[] getUnit(QueryGenerator qg, String axis) {
		String u = " (" + axis + ")";
		String[] unit = new String[2];
		if (qg != null) {
			unit[0] = qg.getAxis2Name();
			unit[1] = qg.getVariable() + u;
		}
		return unit;
	}

	private String showDiffOptions(ArrayList<String> scenarios) {
		String rtn_string = scenarios.get(0);

		final InterfaceMain main = InterfaceMain.getInstance();
		final JFrame parentFrame = main.getFrame();
		final JDialog filterDialog = new JDialog(parentFrame, "Difference Options", true);

		final String[] minValChoiceOptions = { "None",  "100.0", "10.0","0.1", "0.01", "0.001", "0.0001", "0.00001", "0.000001" };
		final JLabel minValChoiceLabel = new JLabel("Minimum value: ");
		final JComboBox minValChoice = new JComboBox(minValChoiceOptions);
		final String[] minPctChoiceOptions = { "None", "1", "2.5", "5", "10", "15", "20", "30", "50" };
		final JLabel minPctChoiceLabel = new JLabel("Minimum percent: ");
		final JComboBox minPctChoice = new JComboBox(minPctChoiceOptions);

		final String[] showDiffAsOptions = { "Value", "Percent" };
		final JLabel showDiffAsLabel = new JLabel("Show differences as: ");
		final JComboBox showDiffAsChoice = new JComboBox(showDiffAsOptions);

		filterDialog.getGlassPane().addMouseListener(new MouseAdapter() {
		});
		filterDialog.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JPanel listPane = new JPanel();
		JPanel optionsPane = new JPanel();
		JPanel buttonPane = new JPanel();
		final JButton okButton = new JButton("OK");
		final JButton cancelButton = new JButton("cancel");

		listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
		Container contentPane = filterDialog.getContentPane();

		optionsPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		optionsPane.add(minValChoiceLabel, c);
		c.gridx = 1;
		optionsPane.add(minValChoice, c);
		c.gridx = 0;
		c.gridy = 1;
		optionsPane.add(minPctChoiceLabel, c);
		c.gridx = 1;
		c.gridy = 1;
		optionsPane.add(minPctChoice, c);
		c.gridx = 0;
		c.gridy = 2;
		optionsPane.add(showDiffAsLabel, c);
		c.gridx = 1;
		c.gridy = 2;
		optionsPane.add(showDiffAsChoice, c);

		String[] scns = new String[scenarios.size()];
		for (int i = 0; i < scns.length; i++) {
			scns[i] = scenarios.get(i);
		}

		final JList list = new JList(scns);
		list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (scns.length > 0)
			list.setSelectedIndex(0);

		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (list.getSelectedIndex() == -1) {
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
				}
			}
		});

		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				base_scenario = (String) list.getSelectedValue();
				filterDialog.setVisible(false);
				String min_val_str = minValChoice.getSelectedItem().toString();
				String min_pct_str = minPctChoice.getSelectedItem().toString();
				String show_diff_str = showDiffAsChoice.getSelectedItem().toString();
				if (min_val_str.equals("None")) {
					use_val_filter = false;
				} else {
					use_val_filter = true;
					min_val = Double.parseDouble(min_val_str);
				}
				if (min_pct_str.equals("None")) {
					use_pct_filter = false;
				} else {
					use_pct_filter = true;
					min_pct = Double.parseDouble(min_pct_str);
				}
				if (show_diff_str.equals("Value")) {
					show_pct_diff = false;
				} else {
					show_pct_diff = true;
				}
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				base_scenario = null;
				filterDialog.setVisible(false);
			}
		});

		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonPane.add(okButton);
		buttonPane.add(Box.createHorizontalStrut(10));
		buttonPane.add(cancelButton);
		buttonPane.add(Box.createHorizontalStrut(10));

		JScrollPane sp = new JScrollPane(list);
		sp.setPreferredSize(new Dimension(300, 300));
		listPane.add(new JLabel("Choose Reference Scenario for Calculations"));
		listPane.add(Box.createVerticalStrut(10));
		listPane.add(sp);
		listPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(listPane, BorderLayout.PAGE_START);
		contentPane.add(optionsPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		filterDialog.pack();
		filterDialog.setVisible(true);

		return rtn_string;
	}

}
