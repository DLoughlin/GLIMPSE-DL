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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.JTableHeader;
import java.math.BigDecimal;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import graphDisplay.MapMode;
import graphDisplay.MapModeResolver;
import graphDisplay.Thumbnail;
import graphDisplay.StateMapPanel;
import graphDisplay.SankeyDiagramFromTable;
import graphDisplay.WorldMapPanel;
import graphDisplay.ModelInterfaceUtil;
import chart.LegendUtil;
import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;
import javax.swing.SwingWorker;

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
    /** Query name (chart name) used optionally as the first line when copying to clipboard */
    private String chartName;
	/** List of selected years to display */
    private List<String> selectedYears;
	private static final int MAX_AUTO_CHARTS = 125; // Max number of charts to auto-generate before skipping auto-graphics
    private static final int MAX_AUTO_ROWS = 2000; // Do not auto-generate thumbnails if table has more rows than this
    private JButton graphButton;
    /** Backing query table (unformatted numeric values) used to rebuild display values. */
    private JTable sourceTable;
    /** Current filter selection (if any) used to rebuild the filtered rows. */
    private Map<String, String> currentSelection;
    /** View columns currently shown in this filtered table. */
    private Integer[] visibleColumnIndices;
    /** Column names corresponding to visibleColumnIndices. */
    private String[] visibleColumnNames;

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
        this.chartName = chartName;
        this.sourceTable = jTable;
        this.currentSelection = sel;
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
        visibleColumnIndices = alI.toArray(new Integer[0]);
        visibleColumnNames = al.toArray(new String[0]);
        String[][] tData = getTableData(jTable, visibleColumnIndices);
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
            DefaultTableModel dtm = new DefaultTableModel(newData, visibleColumnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            jtable = new JTable(dtm);
            jtable.setDragEnabled(true);
            // Note: DnD size checks and user warnings are handled centrally by TableTransferHandler.
            // Keep default drag behavior for the JTable and avoid per-table listeners that duplicate that logic.
            jtable.setRowHeight(jtable.getFont().getSize() + 5);
            tableModel = jtable.getModel();
            configureNumericSorters(columnDoubleComparator);
        } catch (Exception e) {
            System.out.println("FilteredTable Caught: ");
            e.printStackTrace();
        }
        // Attach right-click "Collapse" context menu to the column header
        if (jtable != null) {
            addColumnHeaderContextMenu();
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
                // Prevent generating thumbnails if the table is too large.
                int rowCountCheck = (jtable != null) ? jtable.getRowCount() : 0;
                if (rowCountCheck >= MAX_AUTO_ROWS) {
                    System.out.println("Graph suppressed: Result has " + rowCountCheck + " rows (limit is " + MAX_AUTO_ROWS + ") — auto graphics won't be generated.");
                    return;
                }
                 if (debug)
                     System.out.println("FilteredTable: graph press: " + chartName + " " + Arrays.toString(unit) + " " + path + " " + doubleIndex + " " + jtable.getColumnCount() + "  " + jtable.getRowCount());

                 // Always regenerate thumbnails when the user clicks Graph.
                 // Cancel any in-progress thumbnail generation and free previous thumbnail before creating a new one.
                 if (tn != null) {
                     try {
                         tn.cancelGeneration();
                     } catch (Exception ignored) {}
                     tn = null;
                     System.gc();
                 }

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
        // Background export button: builds tab-delimited text off the EDT and places it on clipboard
        JButton exportBgButton = new JButton("Copy");
        exportBgButton.setBackground(LegendUtil.getRGB(-8205574));
        exportBgButton.setToolTipText("Assemble full table in background and copy to clipboard");
        exportBgButton.addActionListener(e -> {
        final Component parent = SwingUtilities.getWindowAncestor(jtable);

        // Snapshot table data on the EDT before handing off to the background thread,
        // since Swing components are not thread-safe.
        final int colCount = jtable.getColumnCount();
        final String[] colNames = new String[colCount];
        for (int colIdx = 0; colIdx < colCount; colIdx++) {
            colNames[colIdx] = jtable.getColumnName(colIdx);
        }
        final int rowCount = jtable.getRowCount();
        final Object[][] rowData = new Object[rowCount][colCount];
        final TableModel model = jtable.getModel();
        // Read cell data from the TableModel (source of truth) under synchronization,
        // using view-to-model index conversion to preserve current sorting/filtering.
        synchronized (model) {
            for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                int modelRow = jtable.convertRowIndexToModel(rowIdx);
                for (int colIdx = 0; colIdx < colCount; colIdx++) {
                    int modelCol = jtable.convertColumnIndexToModel(colIdx);
                    rowData[rowIdx][colIdx] = model.getValueAt(modelRow, modelCol);
                }
            }
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private int rowsExported = 0;
            @Override
            protected Void doInBackground() throws Exception {
                StringBuilder sb = new StringBuilder(Math.min(1024, colCount * 32));
                // Optionally prepend the query name as the first line
                boolean includeQueryName = Boolean.parseBoolean(
                    InterfaceMain.getInstance() != null
                        ? InterfaceMain.getInstance().getProperties().getProperty("copyIncludeQueryName", "false")
                        : "false");
                if (includeQueryName && chartName != null && !chartName.isEmpty()) {
                    sb.append(chartName).append('\n');
                }
                // header
                for (int colIdx = 0; colIdx < colCount; colIdx++) {
                    sb.append(colNames[colIdx]);
                    if (colIdx < colCount - 1) sb.append('\t');
                }
                sb.append('\n');
                // rows
                for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                    for (int colIdx = 0; colIdx < colCount; colIdx++) {
                        Object val = rowData[rowIdx][colIdx];
                        sb.append(val == null ? "" : val.toString());
                        if (colIdx < colCount - 1) sb.append('\t');
                    }
                    sb.append('\n');
                    rowsExported = rowIdx + 1;
                    // occasional yield to keep UI responsive for very large tables
                    if ((rowIdx & 0x3FF) == 0) Thread.yield();
                }
                StringSelection sel = new StringSelection(sb.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(parent, "Exported " + rowsExported + " rows to clipboard.");
                } catch (CancellationException ex) {
                    JOptionPane.showMessageDialog(parent, "Export was cancelled.", "Export Cancelled", JOptionPane.WARNING_MESSAGE);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String msg = (cause != null && cause.getMessage() != null) ? cause.getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(parent, "Export failed: " + msg, "Export Error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(parent, "Export was interrupted.", "Export Interrupted", JOptionPane.WARNING_MESSAGE);
                }
            }
        };

        // Start worker (no progress dialog shown)
        worker.execute();
});
        box.add(exportBgButton);
         box.add(new JLabel(" "));
        // Mapping button
        jb = new JButton("Mapping");
        jb.setBackground(LegendUtil.getRGB(-8205574));
        jb.setToolTipText("Beta: Map regional data");
        jb.setFont(jb.getFont());
        jb.addActionListener(e -> {
                if (debug)
                    System.out.println("FilteredTable: mapping press: " + chartName + " " + Arrays.toString(unit) + " " + path + " " + doubleIndex + " " + jtable.getColumnCount() + "  " + jtable.getRowCount());
                ModelInterfaceUtil.getMetaIndex2(jtable, doubleIndex);
                ModelInterfaceUtil.getUnitDataFromTableByLastNamedCol(jtable);
                        if (MapModeResolver.requiresRowSelection(jtable)) {
                          JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Please select a row in the table first.");
                    return;
                }
                MapMode mapMode = MapModeResolver.resolve(jtable);
                if (mapMode == null) {
                          JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "The query results do not contain mappable state or region data.");
                    return;
                }
                MapMode.createPanel(chartName, jtable, mapMode);
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
                jtable.getSelectionModel().isSelectionEmpty();
                boolean containOtherColumns = checkContainOtherColumns(jtable);
                        if (!containOtherColumns) {
                          JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "the query results cannot generate a flow dataset.");
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
        return MapModeResolver.containsOtherColumns(jtable);
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
        return MapModeResolver.containsAnyState(table);
    }

    /**
     * Checks if "region" column contains any country names.
     * @param table JTable
     * @return true if any country found
     */
    private boolean checkContainAnyCountryRegion(JTable table) {
        return MapModeResolver.containsAnyCountryRegion(table);
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
     * Rebuilds this table's displayed values using the latest significant-digits settings.
     * Uses the original query table so users do not need to rerun queries.
     */
    public void refreshSignificantDigitsDisplay() {
        if (sourceTable == null || jtable == null || visibleColumnIndices == null || visibleColumnNames == null) {
            return;
        }

        String[][] tData = getTableData(sourceTable, visibleColumnIndices);
        if (currentSelection == null || currentSelection.isEmpty()) {
            newData = tData.clone();
        } else {
            String[] qualifier = ModelInterfaceUtil.getColumnFromTable(sourceTable, 5);
            newData = getfilterTableData(tData, getFilterData(qualifier, currentSelection));
        }

        DefaultTableModel refreshedModel = new DefaultTableModel(newData, visibleColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jtable.setModel(refreshedModel);
        tableModel = refreshedModel;
        configureNumericSorters(buildDoubleStringComparator());
        jtable.revalidate();
        jtable.repaint();

        // If a graph is currently shown, regenerate it so displayed numeric labels stay in sync.
        Component currentRight = sp == null ? null : sp.getRightComponent();
        if (currentRight instanceof JComponent) {
            Object isGraph = ((JComponent) currentRight).getClientProperty("isGraph");
            if (Boolean.TRUE.equals(isGraph) && graphButton != null) {
                graphButton.doClick();
            }
        }
    }

    private void configureNumericSorters(Comparator<String> numericComparator) {
        tableModel = jtable.getModel();
        sorter = new TableRowSorter<>(tableModel);
        jtable.setRowSorter(sorter);
        for (int colC = 0; colC < jtable.getColumnCount(); colC++) {
            String clsName = jtable.getColumnName(colC);
            try {
                Double.parseDouble(clsName);
                sorter.setComparator(colC, numericComparator);
            } catch (Exception e) {
                // Ignore non-numeric headers.
            }
        }
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
		if (!InterfaceMain.autoGenerateGraphics) return;

		// If the result set is very large, skip auto-generating thumbnails to avoid heavy UI work.
 		int rowCount = (jtable != null) ? jtable.getRowCount() : 0;
		// If the number of rows has reached or exceeded the configured threshold, skip auto graphics.
		if (rowCount >= MAX_AUTO_ROWS) {
			System.out.println("Auto-graphics skipped: Result has " + rowCount + " rows (limit is " + MAX_AUTO_ROWS + ") — auto graphics won't be generated.");
			return;
		}

		int chartCount = estimateChartCount(jtable);
		if (chartCount < MAX_AUTO_CHARTS) {
			System.out.println("Auto-graphics proceeding: Result has " + chartCount + " charts.");
			clickGraphButton();
		} else {
			System.out.println("Auto-graphics skipped: Result has " + chartCount + " charts (limit is " + MAX_AUTO_CHARTS + ").");
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

    // -------------------------------------------------------------------------
    // Column-collapse feature
    // -------------------------------------------------------------------------

    /**
     * Attaches a right-click popup menu to the table column header.  When the
     * user right-clicks a categorical (label) column – i.e. any column to the
     * left of the first year/numeric column – a "Collapse" item is shown.
     * Selecting it removes that column and sums numeric values for rows that
     * now share the same remaining key, mirroring the (:collapse:) behaviour
     * available in GCAM XPath queries.
     */
    private void addColumnHeaderContextMenu() {
        JTableHeader header = jtable.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) return;
                int viewCol = header.columnAtPoint(e.getPoint());
                if (viewCol < 0 || viewCol >= doubleIndex) return; // only label cols
                JPopupMenu popup = new JPopupMenu();
                String colName = jtable.getColumnName(viewCol);
                JMenuItem item = new JMenuItem("Collapse \u201c" + colName + "\u201d");
                item.addActionListener(ae -> collapseColumn(viewCol));
                popup.add(item);
                popup.show(header, e.getX(), e.getY());
            }
        });
    }

    /**
     * Removes the categorical column at {@code viewColIdx} from the displayed
     * table and rebuilds it so that rows sharing the same remaining label values
     * are merged, with their numeric (year) cells summed.
     *
     * @param viewColIdx The view-space index of the column to collapse.
     */
    private void collapseColumn(int viewColIdx) {
        int modelColIdx = jtable.convertColumnIndexToModel(viewColIdx);

        if (modelColIdx >= doubleIndex) {
            JOptionPane.showMessageDialog(jtable,
                "Only label columns (to the left of the year columns) can be collapsed.");
            return;
        }

        int colCount     = tableModel.getColumnCount();
        int rowCount     = tableModel.getRowCount();
        int unitsColIdx  = colCount - 1;         // last column is always units
        int numYearCols  = colCount - doubleIndex - 1; // numeric year columns
        int newDoubleIdx = doubleIndex - 1;
        int newColCount  = colCount - 1;

        String collapsedColName = tableModel.getColumnName(modelColIdx);

        // Build new column names (omit the collapsed column)
        String[] newColNames = new String[newColCount];
        for (int origCol = 0, newCol = 0; origCol < colCount; origCol++) {
            if (origCol != modelColIdx) newColNames[newCol++] = tableModel.getColumnName(origCol);
        }

        // Group rows by the surviving categorical columns; accumulate numeric sums
        LinkedHashMap<String, double[]> sumsMap = new LinkedHashMap<>();
        LinkedHashMap<String, String[]> catMap  = new LinkedHashMap<>();
        LinkedHashMap<String, String>   unitMap = new LinkedHashMap<>();

        for (int row = 0; row < rowCount; row++) {
            StringBuilder sb = new StringBuilder();
            String[] catVals = new String[newDoubleIdx];
            for (int origCol = 0, newCol = 0; origCol < doubleIndex; origCol++) {
                if (origCol == modelColIdx) continue;
                String val = collapseValueToString(tableModel.getValueAt(row, origCol));
                catVals[newCol] = val;
                sb.append(val).append('\u0000');
                newCol++;
            }
            String key = sb.toString();

            if (!sumsMap.containsKey(key)) {
                sumsMap.put(key, new double[numYearCols]);
                catMap.put(key, catVals);
                unitMap.put(key, collapseValueToString(tableModel.getValueAt(row, unitsColIdx)));
            }

            double[] rowSums = sumsMap.get(key);
            for (int yi = 0; yi < numYearCols; yi++) {
                Object val = tableModel.getValueAt(row, doubleIndex + yi);
                if (val != null) {
                    try { rowSums[yi] += Double.parseDouble(val.toString()); }
                    catch (NumberFormatException ignore) {}
                }
            }
        }

        // Assemble collapsed data
        String[][] collapsed = new String[sumsMap.size()][newColCount];
        int ri = 0;
        for (String key : sumsMap.keySet()) {
            String[] catVals = catMap.get(key);
            double[] rowSums = sumsMap.get(key);
            for (int i = 0; i < newDoubleIdx; i++)          collapsed[ri][i]                  = catVals[i];
            for (int i = 0; i < numYearCols; i++)            collapsed[ri][newDoubleIdx + i]   = toSigFigs(rowSums[i], sigfigs);
            collapsed[ri][newColCount - 1] = unitMap.get(key);
            ri++;
        }

        // Commit
        doubleIndex = newDoubleIdx;
        DefaultTableModel newModel = new DefaultTableModel(collapsed, newColNames) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        jtable.setModel(newModel);
        tableModel = newModel;

        sorter = new TableRowSorter<>(tableModel);
        jtable.setRowSorter(sorter);
        for (int colC = 0; colC < jtable.getColumnCount(); colC++) {
            try {
                Double.parseDouble(jtable.getColumnName(colC));
                sorter.setComparator(colC, buildDoubleStringComparator());
            } catch (NumberFormatException ignore) {}
        }

        jtable.revalidate();
        jtable.repaint();
        System.out.println("Collapsed column \"" + collapsedColName + "\": " + collapsed.length + " rows remain.");
    }

    /** Converts a table cell value to a non-null String. */
    private static String collapseValueToString(Object val) {
        return (val == null) ? "" : val.toString();
    }

    /** Comparator that sorts strings numerically when parseable as doubles. */
    private static Comparator<String> buildDoubleStringComparator() {
        return (v1, v2) -> {
            Double d1 = null, d2 = null;
            try { d1 = Double.parseDouble(v1); } catch (NumberFormatException ignore) {}
            try { d2 = Double.parseDouble(v2); } catch (NumberFormatException ignore) {}
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return Double.compare(d1, d2);
        };
    }
}