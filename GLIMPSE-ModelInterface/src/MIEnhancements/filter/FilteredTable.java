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
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/

//TODO: merge FilteredTable and FilterTable_orig to reduce code redundancy
//TODO: improve/fix selection of which years put in filter popup and which to check  

package filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;
import chart.LegendUtil;
import graphDisplay.StateMapPanel;
import graphDisplay.ModelInterfaceUtil;
import graphDisplay.SankeyDiagramFromTable;
import graphDisplay.Thumbnail;
import graphDisplay.WorldMapPanel;

/**
 * Handles a JTable filtered by meta data of another JTable, then displays on a split pane.
 * Provides filtering, mapping, graphing, and Sankey diagram features for tabular data.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class FilteredTable {
    /** Table model for filtered data */
    private TableModel tableModel;
    /** Row sorter for table */
    private TableRowSorter<TableModel> sorter;
    /** Index of first double column */
    private int doubleIndex;
    /** Filtered table data */
    private String[][] newData;
    /** JTable instance */
    private JTable jtable;
    /** Split pane for UI */
    private JSplitPane sp;
    /** Table column names */
    private String[] tableColumnData;
    /** Thumbnail graph panel */
    private Thumbnail tn;
    /** State map panel */
    private StateMapPanel mp;
    /** Sankey diagram panel */
    private SankeyDiagramFromTable sankeyP;
    /** World map panel */
    private WorldMapPanel worldMap;
    /** Debug flag */
    private boolean debug = false;
    /** Significant figures for numeric display */
    private int sigfigs = 3;
	/** List of selected years to display */
    private List<String> selectedYears;
	private static final int MAX_AUTO_CHARTS = 125; // Max number of charts to auto-generate before skipping auto-graphics
	private JButton graphButton;

    /**
     * Constructs a FilteredTable and sets up the UI and filtering logic.
     * @param sel Selection map for filtering
     * @param chartName Chart name for graphing
     * @param unit Units for display
     * @param path Data path
     * @param jTable Source JTable
     * @param sp Split pane for UI
     */
    public FilteredTable(Map<String, String> sel, String chartName, String[] unit, String path, final JTable jTable, JSplitPane sp) {
        this(sel, chartName, unit, path, jTable, sp, new ArrayList<>());
    }

    /**
     * Constructs a FilteredTable and sets up the UI and filtering logic.
     * @param sel Selection map for filtering
     * @param chartName Chart name for graphing
     * @param unit Units for display
     * @param path Data path
     * @param jTable Source JTable
     * @param sp Split pane for UI
     * @param selectedYears List of selected years to display
     */
	public FilteredTable(Map<String, String> sel, String chartName, String[] unit, String path, final JTable jTable,
			JSplitPane sp, List<String> selectedYears) {
        this.sp = sp;
        this.selectedYears = selectedYears;
        JPanel jp = new JPanel(new BorderLayout());
        Component c = sp.getRightComponent();
        if (c != null) sp.remove(c);

        if (sel == null)
            Var.origYRange = ModelInterfaceUtil.getColumnFromTable(jTable, 0);

        tableColumnData = ModelInterfaceUtil.getColumnFromTable(jTable, 4);
        String[] cls = new String[tableColumnData.length];
        for (int j = 0; j < tableColumnData.length; j++) {
            cls[j] = jTable.getColumnName(j);
        }
        doubleIndex = ModelInterfaceUtil.getDoubleTypeColIndex(cls);
        String[] qualifier = ModelInterfaceUtil.getColumnFromTable(jTable, 5);
        ArrayList<String> al = new ArrayList<>();
        ArrayList<Integer> alI = new ArrayList<>();
        Integer[] tableColumnIndex = getTableColumnIndex(sel);
        if (debug)
            System.out.println("FilteredTable: colidx: " + Arrays.toString(tableColumnIndex));
        for (int i = 0; i < doubleIndex; i++) {
            al.add(tableColumnData[i]);
            alI.add(i);
        }
        for (int i = 0; i < tableColumnIndex.length; i++) {
            al.add(tableColumnData[tableColumnIndex[i]]);
            alI.add(tableColumnIndex[i]);
        }
        al.add(tableColumnData[tableColumnData.length - 1]);
        alI.add(tableColumnData.length - 1);
        if (debug) {
            System.out.println("FilteredTable: col: " + Arrays.toString(tableColumnData));
            System.out.println("FilteredTable: colidx: " + Arrays.toString(alI.toArray(new Integer[0])));
        }
        String[][] tData = getTableData(jTable, alI.toArray(new Integer[0]));
        Comparator<String> columnDoubleComparator = (String v1, String v2) -> {
            Double val1 = null;
            try { val1 = Double.parseDouble(v1); } catch (NumberFormatException e) {}
            Double val2 = null;
            try { val2 = Double.parseDouble(v2); } catch (NumberFormatException e) {}
            if (val1 == null && val2 == null) return 0;
            else if (val1 == null) return 1;
            else if (val2 == null) return -1;
            else return Double.compare(val1, val2);
        };
        if (sel == null || sel.isEmpty())
            newData = tData.clone();
        else
            newData = getfilterTableData(tData, getFilterData(qualifier, sel));
        try {
            DefaultTableModel dtm = new DefaultTableModel(newData, al.toArray(new String[0])) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            jtable = new JTable(dtm);
            jtable.setDragEnabled(true);
            jtable.setRowHeight(jtable.getFont().getSize() + 5);
            tableModel = jtable.getModel();
            sorter = new TableRowSorter<>(tableModel);
            jtable.setRowSorter(sorter);
            // Add custom sorters to columns that are numbers
            for (int colC = 0; colC < jtable.getColumnCount(); colC++) {
                String clsName = jtable.getColumnName(colC);
                try {
                    Double.parseDouble(clsName);
                    sorter.setComparator(colC, columnDoubleComparator);
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("FilteredTable Caught: ");
            e.printStackTrace();
        }
        Box box = Box.createHorizontalBox();
        // Filter button
        JButton jb = new JButton("Filter");
        jb.setBackground(LegendUtil.getRGB(-8205574));
        jb.addActionListener(e -> new FilterTreePane(chartName, unit, path, jTable, sel, sp));
        box.add(jb);
        // Graph button
        graphButton = new JButton("Graph");
        graphButton.setBackground(LegendUtil.getRGB(-8205574));
        graphButton.addActionListener(e -> {
                if (debug)
                    System.out.println("FilteredTable: graph press: " + chartName + " " + Arrays.toString(unit) + " " + path + " " + doubleIndex + " " + jtable.getColumnCount() + "  " + jtable.getRowCount());

                // Always regenerate thumbnails when the user clicks Graph.
                // Previously, thumbnails were cached in 'tn' and repeat clicks were a no-op.
                tn = null;
                System.gc();

                Map<String, Integer[]> metaMap = ModelInterfaceUtil.getMetaIndex2(jtable, doubleIndex);
                HashMap<String, String> unitsMap = ModelInterfaceUtil.getUnitDataFromTableByLastNamedCol(jTable);
                tn = new Thumbnail(chartName, unit, path, doubleIndex, jtable, metaMap, sp, unitsMap);

                JPanel graphPanel = tn.getJp();
                if (graphPanel != null)
                    setRightComponent(graphPanel);
                else {
                    tn = null;
                    System.gc();
                }
        });
        box.add(graphButton);
        box.add(new JLabel(" "));
        // Mapping button
        jb = new JButton("Mapping");
        jb.setBackground(LegendUtil.getRGB(-8205574));
        jb.setToolTipText("Beta: Map regional data");
        jb.setFont(jb.getFont());
        jb.addActionListener(e -> {
                if (debug)
                    System.out.println("FilteredTable: mapping press: " + chartName + " " + Arrays.toString(unit) + " " + path + " " + doubleIndex + " " + jtable.getColumnCount() + "  " + jtable.getRowCount());
                Map<String, Integer[]> metaMap = ModelInterfaceUtil.getMetaIndex2(jtable, doubleIndex);
                HashMap<String, String> unitsMap = ModelInterfaceUtil.getUnitDataFromTableByLastNamedCol(jtable);
                boolean checkStates = checkContainAnyState(jtable);
                boolean checkCountries = checkContainAnyCountryRegion(jtable);
                boolean noRowSelected = jtable.getSelectionModel().isSelectionEmpty();
                boolean containOtherColumns = checkContainOtherColumns(jtable);
                if (checkStates & !checkCountries) {
                    if (noRowSelected & containOtherColumns) {
                        JOptionPane.showMessageDialog(null, "Please select a row in the table first.");
                        return;
                    } else {
                        mp = new StateMapPanel(chartName, jtable);
                    }
                } else if (checkCountries & !checkStates) {
                    if (noRowSelected & containOtherColumns) {
                        JOptionPane.showMessageDialog(null, "Please select a row in the table first.");
                        return;
                    } else {
                        boolean statesIncluded = false;
                        worldMap = new WorldMapPanel(chartName, jtable, statesIncluded);
                    }
                } else if (checkCountries & checkStates) {
                    if (noRowSelected & containOtherColumns) {
                        JOptionPane.showMessageDialog(null, "Please select a row in the table first.");
                        return;
                    } else {
                        boolean statesIncluded = true;
                        worldMap = new WorldMapPanel(chartName, jtable, statesIncluded);
                    }
                }
        });
        if (InterfaceMain.enableMapping) {
            box.add(jb);
        }
        // Sankey button
        jb = new JButton("Sankey");
        jb.setBackground(LegendUtil.getRGB(-8205574));
        jb.setToolTipText("Beta: Plot data to Sankey Diagram");
        jb.setFont(jb.getFont());
        jb.addActionListener(e -> {
                boolean noRowSelected = jtable.getSelectionModel().isSelectionEmpty();
                boolean containOtherColumns = checkContainOtherColumns(jtable);
                if (!containOtherColumns) {
                    JOptionPane.showMessageDialog(null, "the query results cannot generate a flow dataset.");
                    return;
                } else {
                    try {
                        sankeyP = new SankeyDiagramFromTable(chartName, jtable);
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
        });
        if (InterfaceMain.enableSankey) {
            box.add(jb);
        }
        box.setSize(new Dimension(80, 20));
        jp.add(box, BorderLayout.NORTH);
        jp.add(new JScrollPane(jtable), BorderLayout.CENTER);
        jp.updateUI();
        c = sp.getLeftComponent();
        if (c != null) sp.remove(c);
        sp.setLeftComponent(jp);
        if (debug)
            System.out.println("FilteredTable::FilteredTable:max memory " + Runtime.getRuntime().maxMemory() + " total: " + Runtime.getRuntime().totalMemory() + " free: " + Runtime.getRuntime().freeMemory());
    }

    /**
     * Get column indices for filtering based on selection map.
     * @param sel Selection map
     * @return Array of column indices
     */
    private Integer[] getTableColumnIndex(Map<String, String> sel) {
        Integer[] tableColumnIndex = null;
        Map<String, Integer> tableColumnDataIndex = new LinkedHashMap<>();
        if (sel != null && !sel.isEmpty()) {
            String[] keys = sel.keySet().toArray(new String[0]);
            for (String key : keys) {
                String[] temp = key.split("\\|");
                if (temp[0].contains("Year")) {
                    tableColumnDataIndex.put(temp[1], Arrays.asList(tableColumnData).indexOf(temp[1].trim()));
                    if (debug)
                        System.out.println("FilteredTable::getTableColumnIndex:col " + temp[0] + "  " + temp[1] + "  " + Arrays.toString(tableColumnData));
                }
            }
            String[] k = tableColumnDataIndex.keySet().toArray(new String[0]);
            Var.sectionYRange = k.clone();
        }
        if (!tableColumnDataIndex.isEmpty())
            tableColumnIndex = tableColumnDataIndex.values().toArray(new Integer[0]);
        else {
            if (Var.sectionYRange == null)
                Var.sectionYRange = Var.defaultYRange.clone();
            ArrayList<Integer> temp = new ArrayList<>();
            for (String section : Var.sectionYRange) {
                int i = Arrays.asList(Var.origYRange).indexOf(section);
                if (i > -1)
                    temp.add(i);
            }
            tableColumnIndex = new Integer[temp.size()];
            for (int k = 0; k < tableColumnIndex.length; k++)
                tableColumnIndex[k] = doubleIndex + temp.get(k);
        }
        Arrays.sort(tableColumnIndex);
        if (debug)
            System.out.println("FilteredTable::getTableColumnIndex::col" + Arrays.toString(tableColumnIndex) + " sec: " + Arrays.toString(Var.sectionYRange));
        
        if (selectedYears != null && !selectedYears.isEmpty()) {
            ArrayList<Integer> yearIndices = new ArrayList<>();
            for (String year : selectedYears) {
                int colIdx = -1;
                for (int i = 0; i < tableColumnData.length; i++) {
                    if (tableColumnData[i].equals(year)) {
                        colIdx = i;
                        break;
                    }
                }
                if (colIdx != -1) {
                    yearIndices.add(colIdx);
                }
            }
            
            ArrayList<Integer> currentIndices = new ArrayList<>();
            for (Integer index : tableColumnIndex) {
                currentIndices.add(index);
            }
            
            currentIndices.retainAll(yearIndices);
            tableColumnIndex = currentIndices.toArray(new Integer[0]);
        }

        return tableColumnIndex;
    }

    /**
     * Checks if there are columns between "region" and first year column.
     * @param jtable JTable
     * @return true if other columns exist, false otherwise
     */
    private boolean checkContainOtherColumns(JTable jtable) {
        int regionIdx = getColumnByName(jtable, "region");
        ArrayList<String> yearList = getYearListFromTableData(jtable);
        int firstYearIdx = FilteredTable.getColumnByName(jtable, yearList.get(0));
        int idxDiff = firstYearIdx - regionIdx;
        return idxDiff != 1;
    }

    /**
     * Gets year list from table column names.
     * @param jtable JTable
     * @return List of year strings
     */
    public static ArrayList<String> getYearListFromTableData(JTable jtable) {
        int nCols = jtable.getColumnCount();
        ArrayList<String> yearList = new ArrayList<>();
        for (int j = 0; j < nCols; j++) {
            String cls = jtable.getColumnName(j);
            try {
                Double myYear = Double.parseDouble(cls);
                yearList.add(String.valueOf(myYear.intValue()));
            } catch (Exception e) {}
        }
        return yearList;
    }

    /**
     * Checks if "region" column contains any US state codes.
     * @param table JTable
     * @return true if any state code found
     */
    private boolean checkContainAnyState(JTable table) {
        String stringStates = "AK,AL,AR,AZ,CA,CO,CT,DC,DE,FL,GA,HI,IA,ID,IL,IN,KS,KY,LA,MA,MD,ME,MI,MN,MO,MS,MT,NC,ND,NE,NH,NJ,NM,NV,NY,OH,OK,OR,PA,RI,SC,SD,TN,TX,UT,VA,VT,WA,WI,WV,WY";
        int regionColIdx = getColumnByName(table, "region");
        String[][] regionColData = getTableData(table, new Integer[]{regionColIdx});
        String[] regions = Arrays.stream(regionColData).map(row -> row[0]).toArray(String[]::new);
        Set<String> regionSet = new HashSet<>(Arrays.asList(regions));
        String[] uniqueRegions = regionSet.toArray(new String[0]);
        return stringContainsItemFromArray(stringStates, uniqueRegions);
    }

    /**
     * Checks if "region" column contains any country names.
     * @param table JTable
     * @return true if any country found
     */
    private boolean checkContainAnyCountryRegion(JTable table) {
        String[] stringCountries = {"Africa_Eastern","Africa_Northern","Africa_Southern","Africa_Western","Australia_NZ","Brazil","Canada","Central America and Caribbean","Central Asia","China","EU_12","EU_15","Europe_Eastern","Europe_Non_EU","European Free Trade Association","India","Indonesia","Japan","Mexico","Middle East","Pakistan","Russia","South Africa","South America_Northern","South America_Southern","South Asia","South Korea","Southeast Asia","Taiwan","Argentina","Colombia"};
        int regionColIdx = getColumnByName(table, "region");
        String[][] regionColData = getTableData(table, new Integer[]{regionColIdx});
        String[] regions = Arrays.stream(regionColData).map(row -> row[0]).toArray(String[]::new);
        Set<String> regionSet = new HashSet<>(Arrays.asList(regions));
        String[] uniqueRegions = regionSet.toArray(new String[0]);
        return arrayContainsItemFromArray(stringCountries, uniqueRegions);
    }

    /**
     * Gets column index by name.
     * @param table JTable
     * @param name Column name
     * @return Index or -1 if not found
     */
    public static int getColumnByName(JTable table, String name) {
        for (int i = 0; i < table.getColumnCount(); ++i)
            if (table.getColumnName(i).equals(name))
                return i;
        return -1;
    }

    /**
     * Checks if input string contains any item from array.
     * @param inputStr Input string
     * @param items Array of items
     * @return true if any item found
     */
    private boolean stringContainsItemFromArray(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }

    /**
     * Checks if any item in items is present in arrayStr.
     * @param arrayStr Array of strings
     * @param items Items to check
     * @return true if any item found
     */
    private boolean arrayContainsItemFromArray(String[] arrayStr, String[] items) {
        List<String> itemsAsList = Arrays.asList(items);
        for (String str : arrayStr) {
            if (itemsAsList.contains(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets table data for specified columns, formatted to significant figures for numeric columns.
     * @param jtable JTable
     * @param col Array of column indices
     * @return 2D array of table data
     */
    private String[][] getTableData(JTable jtable, Integer[] col) {
        String[][] tData = new String[jtable.getRowCount()][col.length];
        for (int i = 0; i < jtable.getRowCount(); i++) {
            for (int j = 0; j < col.length; j++) {
                String cls = jtable.getColumnName(col[j]);
                boolean isDouble = false;
                try {
                    Double.parseDouble(cls);
                    isDouble = true;
                } catch (Exception e) {}
                if (isDouble) {
                    Double val = null;
                    try {
                        val = Double.parseDouble(jtable.getValueAt(i, col[j]).toString());
                    } catch (NumberFormatException e) {
                        val = null;
                    }
                    if (val == null || val.isInfinite() || val.isNaN()) {
                        tData[i][j] = "N/A";
                    } else {
                        tData[i][j] = toSigFigs(val, sigfigs);
                    }
                } else {
                    String s = (String) jtable.getValueAt(i, col[j]);
                    if (s == null || s.trim().isEmpty())
                        tData[i][j] = "NA";
                    else
                        tData[i][j] = s;
                }
            }
        }
        return tData;
    }

    /**
     * Formats a double value to a string with specified significant digits.
     * @param value Value to format
     * @param significantDigits Number of significant digits
     * @return Formatted string
     */
    public static String toSigFigs(double value, int significantDigits) {
        if (significantDigits < 0) throw new IllegalArgumentException();
        // If the global toggle disables significant digits, show raw values.
        if (DbViewer.disableSigDigits) {
            return Double.toString(value);
        }

        // Honor user preference from Preferences dialog when available.
        // The sig-fig argument is kept for backward compatibility (callers currently pass 3).
        int sigDigitsToUse = significantDigits;
        try {
            if (InterfaceMain.getInstance() != null) {
                String pref = InterfaceMain.getInstance().getProperties().getProperty("significantDigits", "3");
                if (pref != null) {
                    pref = pref.trim();
                    if (pref.equals("2") || pref.equals("3") || pref.equals("5")) {
                        sigDigitsToUse = Integer.parseInt(pref);
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore and use the provided significantDigits.
        }

        BigDecimal bd;
        try {
            bd = new BigDecimal(value, MathContext.DECIMAL64);
        } catch (Exception e) {
            bd = new BigDecimal(0.0);
            System.out.println("Could not create Decimal: " + e.toString());
        }
        bd = bd.round(new MathContext(sigDigitsToUse, RoundingMode.HALF_UP));
        final int precision = bd.precision();
        if (precision < sigDigitsToUse)
            bd = bd.setScale(bd.scale() + (sigDigitsToUse - precision));
        return bd.toPlainString();
    }

    /**
     * Sets the right component of the split pane to the given panel.
     * @param jpc JPanel to set
     */
    public void setRightComponent(JPanel jpc) {
        Component currentRight = sp.getRightComponent();
        boolean isExistingGraph = false;
        int currentWidth = 0;

        if (currentRight instanceof JComponent) {
            Boolean prop = (Boolean) ((JComponent) currentRight).getClientProperty("isGraph");
            isExistingGraph = prop != null && prop;
            currentWidth = currentRight.getWidth();
        }

        if (currentRight != null)
            sp.remove(currentRight);
        sp.setRightComponent(jpc);
        
        if (!isExistingGraph || currentWidth < 50) {
            sp.setDividerLocation(0.678);
        }
        sp.updateUI();
    }

    /**
     * Filters table data based on filter criteria.
     * @param source Source data
     * @param filter Filter criteria
     * @return Filtered data
     */
    private String[][] getfilterTableData(String[][] source, ArrayList<String[]> filter) {
        ArrayList<String[]> al = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            boolean found = true;
            for (int j = 0; j < filter.size(); j++) {
                if (filter.get(j).length == 0) continue;
                
                boolean match = false;
                for (int k = 0; k < filter.get(j).length; k++) {
                    if (j == filter.size() - 1) {
                        if (source[i][source[0].length - 1].trim().equalsIgnoreCase(filter.get(j)[k].trim())) {
                            match = true;
                            break;
                        }
                    } else if (source[i][j].trim().equalsIgnoreCase(filter.get(j)[k].trim())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    found = false;
                    break;
                }
            }
            if (found) {
                al.add(source[i]);
                if (debug)
                    System.out.println("getfilterTableData: " + i + "  " + Arrays.toString(source[i]));
            }
        }
        return al.toArray(new String[0][0]);
    }

    /**
     * Gets filter data from qualifier and selection map.
     * @param qualifier Qualifier array
     * @param sel Selection map
     * @return Filter data
     */
    private ArrayList<String[]> getFilterData(String[] qualifier, Map<String, String> sel) {
        ArrayList<String[]> filter = new ArrayList<>();
        String[] s = sel.values().toArray(new String[0]);
        for (int j = 0; j < qualifier.length; j++) {
            String key = qualifier[j].trim();
            ArrayList<String> uni = new ArrayList<>();
            for (String value : s) {
                String[] temp = value.split("\\|");
                String q = temp[0].trim();
                if (debug)
                    System.out.println("FilteredTable::getfilterData:QualiferIndex: " + j + " key: " + key + " sel: " + Arrays.toString(temp));
                if (q.equals(key) && !uni.contains(temp[1].trim()))
                    uni.add(temp[1].trim());
            }
            if (debug)
                System.out.println("FilteredTable::getfilterData:RowIndex: " + j + "  " + Arrays.toString(uni.toArray(new String[0])));
            filter.add(j, uni.toArray(new String[0]));
        }
        return filter;
    }

    /**
     * Auto-generates graphics if enabled and conditions are met.
     */
	public void autoGraph() {
		if (InterfaceMain.autoGenerateGraphics) {
			int chartCount = estimateChartCount(jtable);
			if (chartCount < MAX_AUTO_CHARTS) {
				System.out.println("Auto-graphics proceeding: Result has " + chartCount + " charts.");
				clickGraphButton();
			} else {
				System.out.println("Auto-graphics skipped: Result has " + chartCount + " charts (limit is " + MAX_AUTO_CHARTS + ").");
			}
		}
	}

	private void clickGraphButton() {
		if (graphButton != null) {
			graphButton.doClick();
		}
	}

	/**
	 * Estimates the number of charts by counting unique values in the first column.
	 * @param table JTable
	 * @return Estimated number of charts
	 */
	private int estimateChartCount(JTable table) {
		if (table == null || table.getRowCount() == 0) {
			return 0;
		}

		TableModel model = table.getModel();
		int scenarioColumn = -1;
		int regionColumn = -1;

		for (int i = 0; i < model.getColumnCount(); i++) {
			String columnName = model.getColumnName(i);
			if ("scenario".equalsIgnoreCase(columnName)) {
				scenarioColumn = i;
			}
			if ("region".equalsIgnoreCase(columnName)) {
				regionColumn = i;
			}
		}

		if (scenarioColumn == -1) {
			// Fallback to old method if scenario column is not found
			System.out.println("Warning: 'scenario' column not found. Falling back to old chart count estimation.");
			HashSet<Object> uniqueValues = new HashSet<>();
			for (int i = 0; i < table.getRowCount(); i++) {
				if (table.getRowSorter() != null) {
					int modelRow = table.getRowSorter().convertRowIndexToModel(i);
					uniqueValues.add(model.getValueAt(modelRow, 0));
				} else {
					uniqueValues.add(table.getValueAt(i, 0));
				}
			}
			int count = uniqueValues.size();
			System.out.println("Estimated chart count based on unique values in first column: " + count);
			return count;
		}

		HashSet<Object> uniqueScenarios = new HashSet<>();
		for (int i = 0; i < table.getRowCount(); i++) {
			int modelRow;
			if (table.getRowSorter() != null) {
				modelRow = table.getRowSorter().convertRowIndexToModel(i);
			} else {
				modelRow = i;
			}
			uniqueScenarios.add(model.getValueAt(modelRow, scenarioColumn));
		}

		int scenarioCount = uniqueScenarios.size();
		int regionCount = 1; // Default to 1 if region column is not found

		if (regionColumn != -1) {
			HashSet<Object> uniqueRegions = new HashSet<>();
			for (int i = 0; i < table.getRowCount(); i++) {
				int modelRow;
				if (table.getRowSorter() != null) {
					modelRow = table.getRowSorter().convertRowIndexToModel(i);
				} else {
					modelRow = i;
				}
				uniqueRegions.add(model.getValueAt(modelRow, regionColumn));
			}
			regionCount = uniqueRegions.size();
		}
		
		int chartCount = scenarioCount * regionCount;

		System.out.println("Estimated chart count based on " + scenarioCount + " scenarios and " + regionCount + " regions: " + chartCount);
		return chartCount;
	}
}
