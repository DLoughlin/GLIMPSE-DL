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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import ModelInterface.InterfaceMain;
import ModelInterface.UnitConversionInstance;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.tables.BaseTableModel;
import ModelInterface.ModelGUI2.tables.ComboTableModel;
import ModelInterface.ModelGUI2.tables.CopyPaste;
import ModelInterface.ModelGUI2.tables.MultiTableModel;
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
 * ============================================================ TWU Replace
 * chart label 1/2/2017 @1 to a chart application Add capability to invoke
 * command line query
 */

public class QueryResultsPanel extends JPanel {

	/**
	 * Referring to the thread that is running. Used to track which thread is being
	 * used/closed
	 */
	Thread runThread;

	/** The context for running queries which can be used to cancel it */
	DbProcInterrupt context = null;

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public QueryResultsPanel() {
		;
	}

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
	public QueryResultsPanel(final QueryGenerator qg, final QueryBinding singleBinding,
			final Object[] scenarioListValues, final Object[] regionListValues, final TabCloseIcon icon,
			final boolean generateTotals) {
		initializeWaiting();
		context = new DbProcInterrupt();
		final QueryResultsPanel thisThread = this;
		runThread = new Thread() {
			public void run() {
				JComponent ret = null;
				String errorMessage = null;
				// do computations, return a JComponent
				try {
					if (qg.isGroup() && singleBinding == null) {
						ret = createGroupTableContent(qg, scenarioListValues, regionListValues);
					} else {
						ret = createSingleTableContent(qg, singleBinding, scenarioListValues, regionListValues,
								generateTotals);
					}
				} catch (Exception e) {
					errorMessage = e.getMessage();
					e.printStackTrace();
				} finally {
					// Ensure we always mark this query as completed so the progress UI stays in sync
					DbViewer.registerQueryCompleted();
				}
				// Stop process if the user terminated the process
				if (isInterrupted())
					return;

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
								JTable table = getJTableFromComponent(thisThread);
								if (table != null && table.getRowCount() >= 150) {
									shouldGraph = false;
									System.out.println("Auto-graphics skipped: Result has " + table.getRowCount() + " rows (limit is 30).");
								} else if (table != null) {
									System.out.println("Auto-graphics proceeding: Result has " + table.getRowCount() + " rows.");
								}

								if (shouldGraph && sp.getLeftComponent() instanceof JPanel) {
									JPanel jp = (JPanel) sp.getLeftComponent();
									clickGraphButton(jp);
								}
							}
						}
					}

					private void clickGraphButton(java.awt.Container container) {
						for (java.awt.Component comp : container.getComponents()) {
							if (comp instanceof JButton) {
								JButton button = (JButton) comp;
								if ("Graph".equals(button.getText())) {
									button.doClick();
									return;
								}
							} else if (comp instanceof java.awt.Container) {
								clickGraphButton((java.awt.Container) comp);
							}
						}
					}
				});
			}
		};
		runThread.start();
	}

	/**
	 * Interrupt the query that is inprogress. This is normally run when the tab is
	 * closed before the results are finished. Note that this method is
	 * asycnchronous and does not ensure the query thread has finished running by
	 * the time it returns which is normally not necessary.
	 * 
	 * @see killThreadAndWait
	 */
	public void killThread() {
		context.interrupt();
		runThread.interrupt();
	}

	/**
	 * Kills the running query and waits for it to stop running before returning.
	 * This would be useful to call for instance before we are about to close the
	 * database since it would not be acceptable for the query to take it's time
	 * interrupting and coming to a stop.
	 */
	public void killThreadAndWait() {
		// kill the thread same as usual
		killThread();

		// join with the runThread which ensures the query thread
		// is done interrupting
		try {
			runThread.join();
		} catch (InterruptedException ie) {
			// ignore
		}
	}

	/**
	 * Inserts a text box that will be displayed until the results from the quert
	 * are available.
	 */
	public void initializeWaiting() {
		// write the text-box
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.X_AXIS));
		tempPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		tempPanel.add(new JLabel("Waiting for results. Close to terminate"));
		add(tempPanel);

	}

	/**
	 * Creates the group table content.
	 * 
	 * @param qg               the query generator
	 * @param scnListValues
	 * @param regionListValues
	 * 
	 * @return a JComponent with the results in it as a JScrollingPane
	 * @throws Exception thrown if the multiTableModel returns an invalid result
	 * 
	 */
	private JComponent createGroupTableContent(QueryGenerator qg, final Object[] scnListValues,
			final Object[] regionListValues) throws Exception {
		BaseTableModel bt = new MultiTableModel(qg, scnListValues, regionListValues, context);
		JTable jTable = new JTable(bt);
		jTable.setCellSelectionEnabled(true);
		jTable.getColumnModel().getColumn(0).setCellRenderer(((MultiTableModel) bt).getCellRenderer(0, 0));
		jTable.getColumnModel().getColumn(0).setCellEditor(((MultiTableModel) bt).getCellEditor(0, 0));
		InterfaceMain.getInstance().fireProperty("Query", null, bt);
		JScrollPane tableScrollPane = new JScrollPane(jTable);
		tableScrollPane.getViewport().setBackground(getBackground());
		return tableScrollPane;
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
			jTable = new JTable(convertToSumTable(bt));
		} else {
			jTable = bt.getAsSortedTable();
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

		//Properties props = main.getProperties();// @1
		// for restore legend information
		//String path = System.getProperty("user.dir") + "\\LegendBundle.properties";
		String path = InterfaceMain.legendBundlesLoc;
		// props.getProperty("lastDirectory") + "\\LegendBundle.properties"; //@1

		//need to do this before units table is built
		if(DbViewer.enableUnitConversions) {
			try {
			convertUnits(qg,jTable);
			} catch(Exception e) {
				System.out.println("Could not convert units: "+e.toString());
			}
		}
		
		//String[] unit = getUnit(qg, (String) jTable.getValueAt(0, jTable.getColumnCount() - 1)); // @
		// Dan: to do: be smarter about units. Array... one per line?
		String[][] units = getUnits(qg, jTable); // @
		
		

//		new FilteredTable(null,qg.toString(), //@1
//				unit, path, //@1
//				jTable, sp); //@1
		new FilteredTable(null, qg.toString(), // @1
				units[0], path, // @1
				jTable, sp); // @1

		main.fireProperty("Query", null, bt); // @1
		return sp;
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
				}else if(uciCheck.getQuery().contains("*")) {
					//for wildcards, since we can't rule it out yet
					conversionOpts.add(uciCheck);
				}
				else if(uciCheck.getQuery().compareToIgnoreCase(qg.toString().trim())==0) {
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
				parsedLine=lines.get(curLine).split(",(?=([^\"]|\"[^\"]*\")*$)");
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
			return null;
		}
		return localInfo;
	}

	public String[][] getUnits(QueryGenerator qg, JTable table) {
		int last_col = table.getColumnCount() - 1;
		int num_rows = table.getRowCount();
		String[][] units = new String[num_rows][2];

		if (qg != null) {
			String item0 = qg.getAxis2Name();
			for (int i = 0; i < num_rows; i++) {
				units[i][0] = qg.getAxis2Name();
				String u = (String) table.getValueAt(i, table.getColumnCount() - 1);
				units[i][1] = qg.getVariable() + " (" + u + ")";
			}
		}
		return units;
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

	protected DefaultTableModel convertToSumTable(ComboTableModel table_model) {

		ArrayList<String> headers = getHeaders(table_model);

		DefaultTableModel sum_table_model = constructNewDefaultTableModel(headers);

		int reg_col = getColNumFromHeader("region", headers);

		// gets key columns
		ArrayList<Integer> keys = getKeys(headers);
		ArrayList<Integer> years = getYears(headers);

		ArrayList<String> tableKeys = getTableKeysTotal(keys, table_model, reg_col);

		for (int i = 0; i < table_model.getRowCount(); i++) {
			boolean match = false;
			String key = tableKeys.get(i);
			ArrayList<String> sumTableKeys = getTableKeysTotal(keys, sum_table_model, reg_col);

			if (sum_table_model.getRowCount() > 0) {
				for (int j = 0; j < sum_table_model.getRowCount(); j++) {
					String sumKey = sumTableKeys.get(j);
					if (sumKey.equals(key)) {
						match = true;
						sumRows(years, sum_table_model, j, table_model, i);
					}
				}
			}
			if (!match) {
				sum_table_model.addRow(getRowObjectsTotal(table_model, i, reg_col));
			}
		}
		return sum_table_model;

	}

	private DefaultTableModel constructNewDefaultTableModel(ArrayList<String> headers) {
		int cols = headers.size();
		DefaultTableModel dtm = new DefaultTableModel(0, cols);

		Object[] col_name = new Object[cols];
		for (int i = 0; i < cols; i++) {
			col_name[i] = headers.get(i);
		}

		dtm.setColumnIdentifiers(col_name);

		return dtm;
	}

	private void sumRows(ArrayList<Integer> yrs, DefaultTableModel orig, int orig_row, ComboTableModel toAdd,
			int add_row) {
		for (int i = 0; i < yrs.size(); i++) {
			int y = yrs.get(i).intValue();
			double orig_val = Double.parseDouble("" + orig.getValueAt(orig_row, y));
			double add_val = Double.parseDouble("" + toAdd.getValueAt(add_row, y));
			double new_val = orig_val + add_val;
			orig.setValueAt(new_val, orig_row, y);
		}

	}

	private int getColNumFromHeader(String colName, ArrayList<String> list) {
		int rtn_val = -1;

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(colName))
				rtn_val = i;
		}

		return rtn_val;
	}

	private Object[] getRowObjectsTotal(ComboTableModel tm, int row, int reg_col) {
		Object[] rtn_objs = new Object[tm.getColumnCount()];

		for (int i = 0; i < tm.getColumnCount(); i++) {
			if (i == reg_col) {
				rtn_objs[i] = "Total";
			} else {
				rtn_objs[i] = tm.getValueAt(row, i);
			}
		}

		return rtn_objs;
	}

	private ArrayList<String> getTableKeysTotal(ArrayList<Integer> keys, TableModel tm, int reg_col) {
		ArrayList<String> rtn_list = new ArrayList<String>();

		for (int i = 0; i < tm.getRowCount(); i++) {
			String str = "";

			for (int j = 0; j < keys.size(); j++) {
				int col = keys.get(j).intValue();
				if (col == reg_col) {
					if (col != 0)
						str += ",";
					str += "Total";
				} else {
					if (col != 0)
						str += ",";
					str += tm.getValueAt(i, col);
				}
			}
			rtn_list.add(str);

		}
		return rtn_list;
	}

	private ArrayList<String> getHeaders(TableModel jm) {
		ArrayList<String> header_list = new ArrayList<String>();

		for (int col = 0; col < jm.getColumnCount(); col++) {
			header_list.add("" + jm.getColumnName(col));
		}

		return header_list;
	}

	private ArrayList<Integer> getKeys(ArrayList<String> headers) {
		ArrayList<Integer> rtn_list = new ArrayList<Integer>();

		for (int col = 0; col < headers.size(); col++) {
			String temp = headers.get(col);

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

		return rtn_list;
	}

	private ArrayList<Integer> getYears(ArrayList<String> headers) {
		ArrayList<Integer> rtn_list = new ArrayList<Integer>();

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

	/**
	 * recursive method to find the JTable in the component
	 * 
	 * @param comp
	 * @return
	 */
	public JTable getJTableFromComponent(JComponent comp) {
		if (comp instanceof JTable)
			return (JTable) comp;
		for (int i = 0; i < comp.getComponentCount(); i++) {
			java.awt.Component child = comp.getComponent(i);
			if (child instanceof JComponent) {
				JTable ret = getJTableFromComponent((JComponent) child);
				if (ret != null)
					return ret;
			}
		}
		return null;
	}

}
