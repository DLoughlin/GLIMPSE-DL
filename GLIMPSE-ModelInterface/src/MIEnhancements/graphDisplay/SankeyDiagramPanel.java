package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreePath;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.flow.FlowPlot;
import org.jfree.data.flow.DefaultFlowDataset;
import org.jfree.data.flow.NodeKey;
import org.jfree.data.flow.FlowKey;

import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.QueryResultsPanel;
import ModelInterface.ModelGUI2.tables.ComboTableModel;
import ModelInterface.ModelGUI2.TabCloseIcon;
import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.queries.SingleQueryExtension;
import ModelInterface.ModelGUI2.xmldb.QueryBinding;
import ModelInterface.ModelGUI2.xmldb.DbProcInterrupt;

import filter.FilteredTable;



/**
 * SankeyDiagramPanel displays a Sankey Diagram by constructing a flow dataset from
 * user-selected input and output data flows.
 *
 * Author: Yadong
 * Created: August/08/2024
 */
public class SankeyDiagramPanel extends JFrame implements ComponentListener {
	private static final long serialVersionUID = 1L;
	private String diagramName;
	private JTabbedPane tablesTabs;
	private String[] endUseEnergyList;
	private JTree queryList;
	protected JList scnList;
	protected JList regionList;
	private JFrame frame;
	private JToolBar toolBar;
	private JPanel scenarioMenuPanel;
	private JPanel regionMenuPanel;
	private JPanel yearMenuPanel;
	private JPanel endUseEnergyPanel;
	private JPanel sankeyPanel;
	private JLabel scenarioListLabel;
	private JLabel regionListLabel;
	private	JLabel listLabel;
	private JLabel endUseListLabel;
	private JComboBox<String> scenarioListMenu;
	private	JComboBox<String> regionListMenu;
	private	JComboBox<String> yearListMenu;
	private JComboBox<String> endUseEnergyMenu;
	private String selectedScenario;
	private String selectedRegion;
	private String selectedYear;
	private String[] sankey_query_names = new String[5];
	private HashMap<String,DefaultFlowDataset> gatheredFlowDatasets = new HashMap<>();
	DbProcInterrupt context = null;

	/**
	 * Constructor for SankeyDiagramPanel.
	 * @param diagramName Name of the diagram.
	 * @param scnList List of scenarios.
	 * @param regionList List of regions.
	 * @param queryList JTree of queries.
	 * @param tablesTabs Tabbed pane for tables.
	 * @throws ClassNotFoundException if required classes are not found.
	 */
	public SankeyDiagramPanel(String diagramName,JList scnList, JList regionList,JTree queryList,JTabbedPane tablesTabs) throws ClassNotFoundException {
		this.diagramName = diagramName;
		this.scnList = scnList;
		this.regionList = regionList;
		this.queryList = queryList;
		this.tablesTabs = tablesTabs;
		initialize();
	}
	
	/**
	 * Initializes the Sankey diagram panel and its components.
	 */
	private void initialize() {
		sankey_query_names[0]= "End-use energy consumption (aggregated)";
		sankey_query_names[1]= "End-use energy consumption (detail)";
		sankey_query_names[2]= "Electricity generation input";
		sankey_query_names[3]= "Refined liquid production input";
		sankey_query_names[4]= "Hydrogen production input";
		endUseEnergyList = Arrays.copyOfRange(sankey_query_names,0,2);
		frame = new JFrame("Sankey for "+diagramName);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.getContentPane().add(createToolBar(),BorderLayout.WEST);
		selectQueriesForSankey(queryList);
		System.out.println("Check gatheredFlowDatasets size: "+gatheredFlowDatasets.size());
		selectedScenario = (String)scenarioListMenu.getSelectedItem();
	    selectedRegion = (String)regionListMenu.getSelectedItem();
	    selectedYear =(String)yearListMenu.getSelectedItem();
		DefaultFlowDataset combinedDataset = createFlowDatasetFromAllTables(queryList,selectedScenario,selectedRegion,selectedYear);
		if (combinedDataset.getAllNodes().size()>=1) {
		frame.getContentPane().add(createSankeyPlot(combinedDataset),BorderLayout.CENTER);
		}
		frame.validate();
		frame.pack();
		Dimension preferredD = new Dimension(1200,800);
		frame.setSize(preferredD);
		frame.setMinimumSize(new Dimension(500,300));
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	/**
	 * Creates the left-side toolbar with dropdown menus for user selections.
	 * @return JComponent toolbar.
	 */
	protected JComponent createToolBar() {
		//create a toolBar on the left
		toolBar = new JToolBar();
		toolBar.setBackground(Color.LIGHT_GRAY);
		toolBar.setBorder(new EmptyBorder(5,5,5,5));
		toolBar.setLayout(new GridLayout(10,1));
		toolBar.setFloatable(false);
		//add end-use energy consumption dropdown menu inside the JToolBar 
		endUseEnergyPanel = new JPanel();
		endUseEnergyPanel.setBorder(new EmptyBorder(10,10,10,10));
		endUseEnergyPanel.setLayout(new BoxLayout(endUseEnergyPanel,BoxLayout.Y_AXIS));
		endUseEnergyPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		endUseListLabel = new JLabel("End-use energy consumption:",SwingConstants.LEFT);
		endUseListLabel.setFont(new Font("Arial",Font.BOLD,16));
		endUseListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		DefaultComboBoxModel<String> dmlForEndUse = new DefaultComboBoxModel<String>();
		for (int i=0;i< endUseEnergyList.length;i++) {
			dmlForEndUse.addElement(endUseEnergyList[i]);	
		}
	    endUseEnergyMenu = new JComboBox<String>();
	    endUseEnergyMenu.setModel(dmlForEndUse);
	    endUseEnergyMenu.setVisible(true);
	    endUseEnergyMenu.setFont(new Font("Arial",Font.BOLD,14));
		endUseEnergyMenu.setMaximumSize(new Dimension(300,25));
		endUseEnergyMenu.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		endUseEnergyMenu.addActionListener(new UpdateSelectedQueries());
		//yearListMenu.addActionListener();
		endUseEnergyPanel.add(endUseListLabel);
	    endUseEnergyPanel.add(endUseEnergyMenu);
		toolBar.add(endUseEnergyPanel);
		
		//add scenario dropdown menu inside the JToolBar 
		scenarioMenuPanel = new JPanel();
		scenarioMenuPanel.setBorder(new EmptyBorder(5,5,5,5));
		scenarioMenuPanel.setLayout(new BoxLayout(scenarioMenuPanel,BoxLayout.Y_AXIS));
		scenarioMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		scenarioListLabel = new JLabel("Scenario:",SwingConstants.LEFT);
		scenarioListLabel.setFont(new Font("Arial",Font.BOLD,16));
		scenarioListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		List selectedScenarios = scnList.getSelectedValuesList();
		DefaultComboBoxModel<String> dmlScenario = new DefaultComboBoxModel<String>();
				for (int i=0;i< selectedScenarios.size();i++) {
					dmlScenario.addElement(selectedScenarios.get(i).toString());	
				}
		scenarioListMenu = new JComboBox<String>();
		scenarioListMenu.setModel(dmlScenario);
		scenarioListMenu.setVisible(true);
		scenarioListMenu.setFont(new Font("Arial",Font.BOLD,14));
		scenarioListMenu.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		scenarioListMenu.setMaximumSize(new Dimension(300,25));
		//scenarioListMenu.addActionListener();
		scenarioMenuPanel.add(scenarioListLabel);
		scenarioMenuPanel.add(scenarioListMenu);
		toolBar.add(scenarioMenuPanel);
		
		//add region dropdown menu inside the JToolBar 
		regionMenuPanel = new JPanel();
		regionMenuPanel.setBorder(new EmptyBorder(5,5,5,5));
		regionMenuPanel.setLayout(new BoxLayout(regionMenuPanel,BoxLayout.X_AXIS));
		regionMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		regionListLabel = new JLabel("Region:",SwingConstants.LEFT);
		regionListLabel.setFont(new Font("Arial",Font.BOLD,16));
		regionListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		DefaultComboBoxModel<String> dmlRegion = new DefaultComboBoxModel<String>();
		List selectedRegions = regionList.getSelectedValuesList();
				for (int i=0;i< selectedRegions.size();i++) {
					dmlRegion.addElement(selectedRegions.get(i).toString());	
				}
		regionListMenu = new JComboBox<String>();
		regionListMenu.setModel(dmlRegion);
		regionListMenu.setVisible(true);
		regionListMenu.setFont(new Font("Arial",Font.BOLD,14));
		regionListMenu.setMaximumSize(new Dimension(200,25));
		regionListMenu.addActionListener(new UpdateSankeyChart());
		regionMenuPanel.add(regionListLabel);
		regionMenuPanel.add(regionListMenu);
		toolBar.add(regionMenuPanel);		
		//add year dropdown menu inside the JToolBar 
		yearMenuPanel = new JPanel();
		yearMenuPanel.setBorder(new EmptyBorder(10,10,10,10));
		yearMenuPanel.setLayout(new BoxLayout(yearMenuPanel,BoxLayout.X_AXIS));
		yearMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		listLabel = new JLabel("Year:",SwingConstants.LEFT);
		listLabel.setFont(new Font("Arial",Font.BOLD,16));
		listLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		String[] yearList = {"2015","2020","2025","2030","2035","2040","2045","2050","aaaa"};
		
		DefaultComboBoxModel<String> dml = new DefaultComboBoxModel<String>();
			for (int i=0;i< yearList.length;i++) {
				dml.addElement(yearList[i]);	
			}
		yearListMenu = new JComboBox<String>();
		yearListMenu.setModel(dml);
		yearListMenu.setVisible(true);
		//yearListMenu.setMaximumSize(yearListMenu.getPreferredSize());
		yearListMenu.setFont(new Font("Arial",Font.BOLD,14));
		yearListMenu.setMaximumSize(new Dimension(150,25));
		yearListMenu.addActionListener(new UpdateSankeyChart());
		yearMenuPanel.add(listLabel);
		yearMenuPanel.add(yearListMenu);
		toolBar.add(yearMenuPanel);
		return toolBar;
	};
	
	/**
	 * Creates the Sankey plot panel using the provided flow dataset.
	 * @param myDataset The flow dataset to visualize.
	 * @return JComponent Sankey plot panel.
	 */
	protected JComponent createSankeyPlot(DefaultFlowDataset myDataset) {
		
		
		sankeyPanel = new JPanel();
		sankeyPanel.setLayout(new BoxLayout(sankeyPanel,BoxLayout.X_AXIS));
		sankeyPanel.setBorder(new EmptyBorder(10,10,10,10));
		sankeyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		//DefaultFlowDataset myDataset = getExampleFlowDataset(3);
		
		Set<NodeKey> mySet = myDataset.getAllNodes();
		Iterator<NodeKey> nodeIterator = mySet.iterator();
		Set<FlowKey> myFlowSet = myDataset.getAllFlows();
		Iterator<FlowKey> flowKeyIterator = myFlowSet.iterator();
		while (flowKeyIterator.hasNext()) {
			FlowKey myFlowKey = flowKeyIterator.next();
			System.out.println("Check each of the FlowKey: "+myFlowKey.toString());
		}
		
		FlowPlot myPlot = new FlowPlot(myDataset);
		while (nodeIterator.hasNext()) {
			NodeKey myKey = nodeIterator.next();
			
		    System.out.println("Check each of the NodeKey: "+myKey.toString());
		    System.out.println("Check getNode of the NodeKey: "+myKey.getNode());
		    if (myKey.getNode().toString().equalsIgnoreCase("Electricity")) {
		    	myPlot.setNodeFillColor(myKey, Color.ORANGE);
		    }else if (myKey.getNode().toString().equalsIgnoreCase("Natural gas")) {
		    	myPlot.setNodeFillColor(myKey, Color.BLUE);
		    }else if (myKey.getNode().toString().equalsIgnoreCase("Biomass")) {
		    	myPlot.setNodeFillColor(myKey, Color.GREEN);
		    }
		}
		
		
		//System.out.println("Check DefaultNodeLabelPaint: "+myPlot.getDefaultNodeLabelPaint());
		
		myPlot.setNodeLabelOffsetX(-170.0);
		myPlot.setNodeLabelOffsetY(-170.0);
		myPlot.setNodeWidth(200.0);
		myPlot.setNodeMargin(0.015);
		myPlot.setDefaultNodeLabelFont(new Font("Arial",Font.BOLD,16));
		myPlot.setOutlineVisible(true);
		myPlot.setOutlinePaint(new Color(0,0,0));
		String chartTitle = "Energy Flow for "+ selectedRegion +" in year "+selectedYear;
		JFreeChart chart = new JFreeChart(chartTitle,myPlot);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.validate();
		//chartPanel.setPreferredSize(new java.awt.Dimension(sankeyPanel.getWidth(),sankeyPanel.getHeight()));
		sankeyPanel.add(chartPanel,BorderLayout.CENTER);
		return sankeyPanel;
	}
	
	// YD added this method for Sankey Diagrams,Sep-2024
	/**
	 * Selects queries relevant for the Sankey diagram and adds their results to the tabbed pane.
	 * @param queryList JTree containing available queries.
	 */
	public void selectQueriesForSankey(JTree queryList) {
			
			//look for the queries under "for Sankey diagrams" group
			String[] sankey_query_names_to_select = new String[4];
			String selectedEndUse = (String)endUseEnergyMenu.getSelectedItem();
			sankey_query_names_to_select[0] = selectedEndUse;
			sankey_query_names_to_select[1] = sankey_query_names[2];
			sankey_query_names_to_select[2] = sankey_query_names[3];
			sankey_query_names_to_select[3] = sankey_query_names[4];
			ArrayList<TreePath> sankey_query_treePath = new ArrayList<TreePath>();
			sankey_query_treePath = getFullTreePathSankey(queryList,"Sankey diagrams");
			if(sankey_query_treePath.size()==0) {
				String errorMessage=" No group containing 'Sankey diagrams' is found in the query list:\n";
				JOptionPane.showMessageDialog(null, errorMessage);
				
			}else {
				int[] rowsToSelect = new int[sankey_query_names_to_select.length];
				for (int n = 0; n < sankey_query_names_to_select.length; n++) {	
					String leaf_name_to_find = sankey_query_names_to_select[n];
					int rowNum = DbViewer.getRowNumberForLeaf(queryList, sankey_query_treePath.get(0), leaf_name_to_find);
					rowsToSelect[n] = rowNum;
				}
				System.out.println("check how many rows were found for Sankey: "+rowsToSelect.length);
					
				queryList.clearSelection();
				queryList.setSelectionRows(rowsToSelect);
				TreePath[] selPaths = queryList.getSelectionPaths();
				// iterates over selected queries by path
				for (int i = 0; i < selPaths.length; ++i) {
					try {
						QueryGenerator qg = null;
						QueryBinding singleBinding = null;
						if (selPaths[i].getLastPathComponent() instanceof QueryGenerator) {
							qg = (QueryGenerator) selPaths[i].getLastPathComponent();
							System.out.println("check if QueryGenerator is a group " + qg.isGroup());	
						} else {
							singleBinding = ((SingleQueryExtension.SingleQueryValue) selPaths[i]
										.getLastPathComponent()).getAsQueryBinding();
							qg = (QueryGenerator) selPaths[i].getParentPath().getLastPathComponent();	
						}
						// BaseTableModel
							
						// add loading icon to QueryResultsPanel
						TabCloseIcon loadingIcon = new TabCloseIcon(tablesTabs);
						// creating new panel for holding the results of the queries
						JComponent ret = new QueryResultsPanel(qg, singleBinding,scnList.getSelectedValues(),regionList.getSelectedValues(), loadingIcon, false); 			
						// adds new tab for query results panel
						// tablesTabs.addTab(qg.toString(), loadingIcon, ret, DbViewer.createCommentTooltip(selPaths[i]));
						// create a simple tooltip from the selected tree path (DbViewer.createCommentTooltip is not static/visible)
						String tooltip = null;
						if (selPaths[i] != null && selPaths[i].getLastPathComponent() != null) {
							tooltip = selPaths[i].getLastPathComponent().toString();
						}
						tablesTabs.addTab(qg.toString(), loadingIcon, ret, tooltip);
						System.out.println("check tablesTabs number: " + tablesTabs.getComponentCount());
					} catch (ClassCastException cce) {
						System.out.println("Warning: Caught " + cce + " likely a QueryGroup was in the selection");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}//inner for loop end
			}
	    }// selectQueriesForSankey method end
	
	/**
	 * Creates a combined flow dataset from all selected tables based on scenario, region, and year.
	 * @param queryList JTree of queries.
	 * @param scenarioStr Selected scenario string.
	 * @param regionStr Selected region string.
	 * @param yearStr Selected year string.
	 * @return DefaultFlowDataset combined dataset.
	 */
	private DefaultFlowDataset createFlowDatasetFromAllTables(JTree queryList, String scenarioStr, String regionStr, String yearStr) {
		DefaultFlowDataset combinedFlowDataset = new DefaultFlowDataset();
		ComboTableModel bt = null;
		TreePath[] selPaths = queryList.getSelectionPaths();
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
				// BaseTableModel
				int stageNForTable = -1;
				bt = new ComboTableModel(qg, scnList.getSelectedValues(), regionList.getSelectedValues(), singleBinding, context);
				if (bt != null) {
				//gather all flowDatasets into a HashMap first
				JTable jTable = null;
				JSplitPane sp = new JSplitPane();
				jTable = bt.getAsSortedTable();
				String datasetName = selPaths[i].getLastPathComponent().toString();
				//the scenario string in the tables are slightly different from the ones in the scenario list
				String selectedScenarioPart1 = scenarioStr.substring(0,scenarioStr.indexOf(' '));
				String selectedScenarioPart2 = scenarioStr.substring(scenarioStr.indexOf(' ')+1);
				String selectedScenarioInTable = selectedScenarioPart1+",date="+selectedScenarioPart2;
				System.out.println("check selectedScenario: " + selectedScenarioInTable);
				System.out.println("check datasetName: " + datasetName);
				if (datasetName.contains("End-use energy consumption")) {
					stageNForTable = 1;
				}else if (datasetName.contains("Electricity generation input")) {
					stageNForTable =0;
				}else if(datasetName.contains("Refined liquid production input")) {
					stageNForTable =0;
				}
				int regionIdx = FilteredTable.getColumnByName(jTable, "region");
				int scenarioIdx = FilteredTable.getColumnByName(jTable, "scenario");
				ArrayList<String> yearList = FilteredTable.getYearListFromTableData(jTable);
				int firstYearIdx = FilteredTable.getColumnByName(jTable, yearList.get(0));
				//System.out.println("inside createFlowDatasetFromTable method,firstYearIdx is: "+firstYearIdx);
				int yearIdx = FilteredTable.getColumnByName(jTable, yearStr);
				System.out.println("inside createFlowDatasetFromAllTables method,YearIdx is: "+yearIdx);
				//int nodeN = firstYearIdx-regionIdx-1;
				for(int row=0;row<jTable.getRowCount();row++) {
				//check if the scenario and region match the selected
					boolean scenario2Keep = ((String)jTable.getValueAt(row, scenarioIdx)).equals(selectedScenarioInTable);
					boolean region2Keep = ((String)jTable.getValueAt(row, regionIdx)).equals(regionStr);
					if (scenario2Keep && region2Keep) {
						for (int j = regionIdx+1; j < firstYearIdx-1; j++) {
						String toDes = (String)jTable.getValueAt(row, j);
						String fromSource = (String)jTable.getValueAt(row, j+1);
						double flowRate = Double.parseDouble((String)jTable.getValueAt(row, yearIdx));
						System.out.println("createFlowDatasetFromAllTables::to this destination: "+toDes);
						if (flowRate !=0) {
						combinedFlowDataset.setFlow(stageNForTable,fromSource,toDes,flowRate);
						}
						}
					}
				}//inner for loop end
			}
					
			} catch (ClassCastException cce) {
				System.out.println("Warning: Caught " + cce + " likely a QueryGroup was in the selection");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}//outer for loop end
		return combinedFlowDataset;
	}
	
	/**
	 * Creates a flow dataset from a single table, filtering by scenario, region, and year.
	 * @param jtable JTable containing data.
	 * @param scenarioStr Scenario string.
	 * @param regionStr Region string.
	 * @param yearStr Year string.
	 * @param stageN Stage number for the flow.
	 * @return DefaultFlowDataset dataset.
	 */
	private DefaultFlowDataset createFlowDatasetFromTable(JTable jtable, String scenarioStr, String regionStr, String yearStr, int stageN) {			
			//remove those rows not matching scenarioStr and regionStr from jtable first
			int regionIdx = FilteredTable.getColumnByName(jtable, "region");
			int scenarioIdx = FilteredTable.getColumnByName(jtable, "scenario");
			ArrayList<String> yearList = FilteredTable.getYearListFromTableData(jtable);
			int firstYearIdx = FilteredTable.getColumnByName(jtable, yearList.get(0));
			//System.out.println("inside createFlowDatasetFromTable method,firstYearIdx is: "+firstYearIdx);
			int yearIdx = FilteredTable.getColumnByName(jtable, yearStr);
			System.out.println("inside createFlowDatasetFromTable method,YearIdx is: "+yearIdx);
			//int nodeN = firstYearIdx-regionIdx-1;
			DefaultFlowDataset dataset = new DefaultFlowDataset(); 
			for(int row=0;row<jtable.getRowCount();row++) {
			//check if the scenario and region match the selected
				boolean scenario2Keep = ((String)jtable.getValueAt(row, scenarioIdx)).equals(scenarioStr);
				boolean region2Keep = ((String)jtable.getValueAt(row, regionIdx)).equals(regionStr);
				if (scenario2Keep && region2Keep) {
					for (int j = regionIdx+1; j < firstYearIdx-1; j++) {
					String toDes = (String)jtable.getValueAt(row, j);
					String fromSource = (String)jtable.getValueAt(row, j+1);
					double flowRate = Double.parseDouble((String)jtable.getValueAt(row, yearIdx));
					System.out.println("createFlowDatasetFromTable::to this destination: "+toDes);
					if (flowRate !=0) {
					dataset.setFlow(stageN,fromSource,toDes,flowRate);
					}
					 }
				}
			}
			return dataset;
		}
	/**
	 * Returns an example flow dataset for demonstration purposes.
	 * @param choice Integer to select example data.
	 * @return DefaultFlowDataset example dataset.
	 */
	private DefaultFlowDataset getExampleFlowDataset(int choice) {
		// these data are from the query results "Inputs by tech" under "Inputs and outputs" 
	    // NC "Region" for "Year" 2015
		DefaultFlowDataset dataset = new DefaultFlowDataset(); 
        if (choice ==1) {
		dataset.setFlow(0,"comm cooking","electricity",0.000048);
		dataset.setFlow(0, "comm cooking", "electricity", 0.00002);
		dataset.setFlow(0,"comm cooking","gas",0.000138);
		dataset.setFlow(0, "comm cooking", "gas", 0.000092);
		dataset.setFlow(0,"comm cooling","electricity",0.000395);
		dataset.setFlow(0, "comm cooling", "electricity", 0.000049);
		dataset.setFlow(0,"comm cooling","gas",0.000072);
		dataset.setFlow(0,"comm heating","biomass",0.00441);
		dataset.setFlow(0,"comm heating","electricity",0.00103);
		dataset.setFlow(0, "comm heating", "electricity", 0.000183);
		dataset.setFlow(0,"comm heating","gas",0.00657);
		dataset.setFlow(0,"comm heating","refined liquids",0.00798);
		
		dataset.setFlow(1,"electricity","electric range",0.000048);
		dataset.setFlow(1,"electricity","electric range hi-eff", 0.00002);
		dataset.setFlow(1,"gas","gas range",0.000138);
		dataset.setFlow(1,"gas","gas range hi-eff", 0.000092);
		dataset.setFlow(1,"electricity","air conditioning",0.000395);
		dataset.setFlow(1,"electricity","air conditioning hi-eff", 0.000049);
		dataset.setFlow(1,"gas","gas cooling",0.000072);
		dataset.setFlow(1,"biomass","wood furnace",0.00441);
		dataset.setFlow(1,"electricity","electric furnace",0.00103);
		dataset.setFlow(1,"electricity","electric heatpump", 0.000183);
		dataset.setFlow(1,"gas","gas furnace",0.00657);
		dataset.setFlow(1,"refined liquids","fuel furnace",0.00798);
		// these data are from "End-use energy consumption" and "Energy consumption"
		// "Energy inputs to refining activities" NC "Region" for "Year" 2030
        }else if (choice ==2) { 
        dataset.setFlow(0,"Biomass","Electricity",0.0043);
    	dataset.setFlow(0, "Coal", "Electricity", 0.206);
    	dataset.setFlow(0,"Natural gas","Electricity",0.354);
    	dataset.setFlow(0, "Refined liquids","Electricity",0.000283);
    	dataset.setFlow(0,"Uranium","Electricity",0.484);
    	dataset.setFlow(0,"Solar","Electricity",0.05814);
    	dataset.setFlow(0,"Wind","Electricity",0.02784);
    	dataset.setFlow(0,"Biomass","Refining",0.0011991);
    	dataset.setFlow(0, "Natural gas", "Refining", 0.0000841);
    	dataset.setFlow(1, "Electricity", "Refining", 0.00000173);
    	dataset.setFlow(0,"Biomass","Buildings",0.00819);
    	dataset.setFlow(0, "Coal", "Buildings", 0.00175);
    	dataset.setFlow(1,"Electricity","Buildings",0.433);
    	dataset.setFlow(0,"Natural gas","Buildings",0.0943);
    	dataset.setFlow(0,"Refined liquids","Transportation",0.531);
    	dataset.setFlow(1,"Electricity","Transportation",0.0299);
    	dataset.setFlow(0,"Natural gas","Transportation",0.00332);
    	dataset.setFlow(0,"Biomass","Industry",0.082);
    	dataset.setFlow(0, "Coal", "Industry", 0.0201);
    	dataset.setFlow(1,"Electricity","Industry",0.123);
    	dataset.setFlow(0,"Natural gas","Industry",0.125);
    	dataset.setFlow(0,"Refined liquids","Industry",0.11);	
        }else {
        	tablesTabs.getSelectedIndex();
        }
		return dataset;
	}
	
	//YD added,Sep-2024,this method is to find the full treePath containing a group name string
	/**
	 * Finds the full tree path containing a group name string in a JTree.
	 * @param tree JTree to search.
	 * @param groupName Name of the group to find.
	 * @return ArrayList<TreePath> of matching paths.
	 */
	private ArrayList<TreePath> getFullTreePathSankey(JTree tree, String groupName) {
		Enumeration<TreePath> allPath = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		ArrayList<TreePath> myTreePath = new ArrayList<TreePath>();
		if (allPath != null) {
			while (allPath.hasMoreElements()) {
				TreePath treePath = (TreePath) allPath.nextElement();
				String treePathStr = treePath.toString();
				String[] splitsTreePath = treePathStr.replace("[", "").replace("]", "").split(",");
				String currentGroup = splitsTreePath[splitsTreePath.length - 1];
				// handle when "," within groupName such as 'Markets, prices, and costs' first
				boolean groupNameHasComma = groupName.contains(",");
				if (groupNameHasComma && treePath.toString().contains(groupName)) {
					myTreePath.add(treePath);
				} else if (currentGroup.trim().contains(groupName)) {
					myTreePath.add(treePath);
				}
			}
		}
		return (myTreePath);
	}
	
	/**
	 * ActionListener to update the Sankey chart when region or year selection changes.
	 */
	public class UpdateSankeyChart extends JPanel implements ActionListener {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Remove previous Sankey panel and update with new selection
			frame.remove(sankeyPanel);
			selectedScenario = (String)scenarioListMenu.getSelectedItem();
		    selectedRegion = (String)regionListMenu.getSelectedItem();
		    selectedYear =(String)yearListMenu.getSelectedItem();
			DefaultFlowDataset combinedDataset = createFlowDatasetFromAllTables(queryList,selectedScenario,selectedRegion,selectedYear);
			if (combinedDataset.getAllNodes().size()>=1) {
			frame.getContentPane().add(createSankeyPlot(combinedDataset),BorderLayout.CENTER);
			}
			frame.revalidate();
			frame.repaint();
			
		}
	}
	/**
	 * ActionListener to update selected queries when end-use energy selection changes.
	 */
	public class UpdateSelectedQueries extends JPanel implements ActionListener {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Remove all tabs and re-select queries for Sankey
			tablesTabs.removeAll();
			selectQueriesForSankey(queryList);
		}
	}
	
	// ComponentListener methods (empty implementations)
	@Override
	public void componentResized(ComponentEvent e) {}
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentHidden(ComponentEvent e) {}
	
	
}