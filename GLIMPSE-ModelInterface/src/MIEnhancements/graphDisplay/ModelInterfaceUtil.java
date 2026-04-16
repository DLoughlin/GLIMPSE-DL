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
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/
package graphDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import conversionUtil.ArrayConversion;

/**
 * Utility functions for GraphDisplay package related to data from ModelInterface package.
 * <p>
 * Author: TWU
 * Created: 1/2/2016
 */
public class ModelInterfaceUtil {
    private static boolean debug = false;

    /**
     * Returns column names from JTable based on function type.
     * @param jtable JTable to extract columns from
     * @param func Function type (0: plot, 1: legend, 2: qualifier, 3: no unit, 4: all, 5: all but years)
     * @return Array of column names
     */
    public static String[] getColumnFromTable(JTable jtable, int func) {
        int cnt = getDoubleTypeColIndex(jtable);
        return getColumnFromTable(jtable, cnt, func);
    }

    /**
     * Returns column names from JTable based on function type and column index.
     * @param jtable JTable to extract columns from
     * @param cnt Index for double type column
     * @param func Function type
     * @return Array of column names
     */
    public static String[] getColumnFromTable(JTable jtable, int cnt, int func) {
        if (debug)
            System.out.println("ModelInterfaceUtil:getColumnFromTable:cnt: " + cnt + " func: " + func + "  colcnt: "
                    + jtable.getColumnCount());

        String[] data = null;
        switch (func) {
        case 0: // plot data columns
            data = getDataColumn(cnt, jtable.getColumnCount() - 1, jtable);
            break;
        case 1: // legend column
            data = getDataColumn(cnt - 1, cnt, jtable);
            break;
        case 2: // qualifier columns
            data = getDataColumn(0, cnt - 1, jtable);
            break;
        case 3: // columns without unit
            data = getDataColumn(0, cnt, jtable);
            break;
        case 4: // all columns
            data = getDataColumn(0, jtable.getColumnCount(), jtable);
            break;
        case 5: // all columns BUT years
            String[] dataNoUnits = getDataColumn(0, cnt, jtable);
            data = new String[dataNoUnits.length + 1];
            for (int i = 0; i < dataNoUnits.length; i++) {
                data[i] = dataNoUnits[i];
            }
            data[data.length - 1] = "Units";
            break;
        }
        return data;
    }

    /**
     * Returns column names in a specified range from JTable.
     * @param start Start index
     * @param end End index
     * @param jtable JTable
     * @return Array of column names
     */
    public static String[] getDataColumn(int start, int end, JTable jtable) {
        String[] col = new String[end - start];
        for (int i = start; i < end; i++) {
            col[i - start] = jtable.getColumnName(i);
        }
        return col;
    }

    /**
     * Returns table data from JTable based on function type.
     * @param jtable JTable
     * @param func Function type
     * @return 2D array of table data
     */
    public static String[][] getDataFromTable(JTable jtable, int func) {
        int cnt = getDoubleTypeColIndex(jtable);
        return getDataFromTable(jtable, cnt, func);
    }

    /**
     * Returns data from a specific column in JTable.
     * @param jtable JTable
     * @param col Column index
     * @return Array of column data
     */
    public static String[] getColDataFromTable(JTable jtable, int col) {
        String[] rtn_str_array = new String[jtable.getRowCount()];
        for (int i = 0; i < jtable.getRowCount(); i++) {
            rtn_str_array[i] = (String) jtable.getValueAt(i, col);
        }
        return rtn_str_array;
    }

    /**
     * Returns a map of unit data from JTable using the last named column.
     * @param jtable JTable
     * @return HashMap of key to unit
     */
    public static HashMap<String, String> getUnitDataFromTableByLastNamedCol(JTable jtable) {
        HashMap<String, String> toReturn = new HashMap<>();
        int colToGrab = 0;
        // Find the last column without a number for name
        for (int i = 0; i < jtable.getColumnCount(); i++) {
            try {
                Integer.parseInt(jtable.getColumnName(i));
                colToGrab = i - 1;
                break;
            } catch (Exception e) {
                continue;
            }
        }
        for (int i = 0; i < jtable.getRowCount(); i++) {
            Object keyObj = jtable.getValueAt(i, colToGrab);
            Object unitObj = jtable.getValueAt(i, jtable.getColumnCount() - 1); // Last column is units
            String key = (keyObj == null) ? "" : keyObj.toString();
            String unit = (unitObj == null) ? "NA" : unitObj.toString();
            if (unit.trim().isEmpty()) unit = "NA";
            // Preserve the original key and also index by normalized key for resilient downstream lookups.
            toReturn.put(key, unit);
            toReturn.put(normalizeUnitLookupKey(key), unit);
        }
        return toReturn;
    }

    private static String normalizeUnitLookupKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().replace(',', '-').replaceAll("\\s+", " ");
    }

    /**
     * Returns table data from JTable based on function type and column index.
     * @param jtable JTable
     * @param cnt Index for double type column
     * @param func Function type
     * @return 2D array of table data
     */
    public static String[][] getDataFromTable(JTable jtable, int cnt, int func) {
        if (debug)
            System.out.println("ModelInterfaceUtil:getDataFromTable:cnt: " + cnt + " func: " + func);

        String[][] data = null;
        switch (func) {
        case 0: // plot data
            data = getTableData(jtable, cnt - 1, jtable.getColumnCount() - 1);
            if (debug)
                System.out.println("ModelInterfaceUtil:getDataFromTable:data:0 " + Arrays.toString(data[0]) + "  " + data.length);
            break;
        case 1: // legend
            data = getTableData(jtable, cnt - 1, cnt);
            if (debug)
                System.out.println("ModelInterfaceUtil:getDataFromTable:data:1 " + Arrays.toString(data[0])
                        + data.length + " " + data[0].length);
            break;
        case 2: // qualifier info
            data = getTableData(jtable, 0, cnt - 1);
            break;
        case 3: //
            data = getTableData(jtable, 0, cnt);
            break;
        case 4: // all columns
            data = getTableData(jtable, 0, jtable.getColumnCount());
            break;
        case 5: // exclude values
            data = getTableNonData(jtable, 0, cnt);
            break;
        }
        return data;
    }

    /**
     * Returns table data in a specified range from JTable.
     * @param jtable JTable
     * @param start Start column index
     * @param end End column index
     * @return 2D array of table data
     */
    public static String[][] getTableData(JTable jtable, int start, int end) {
        String[][] data = new String[jtable.getRowCount()][end - start];
        if (debug)
            System.out.println(
                    "ModelInterfaceUtil:getTableData:start: " + start + " end: " + end + " row: " + data.length);
        for (int i = 0; i < jtable.getRowCount(); i++) {
            for (int j = start; j < end; j++) {
                String cls = jtable.getColumnClass(j).getName();
                if (cls.equals("java.lang.Double")) {
                    double d = ((Double) jtable.getValueAt(i, j)).doubleValue();
                    data[i][j - start] = String.valueOf(d);
                } else {
                    data[i][j - start] = ((String) jtable.getValueAt(i, j));
                }
            }
            if (debug)
                System.out.println("ModelInterfaceUtil:getTableData:data: " + Arrays.toString(data[i]));
        }
        return data;
    }

    /**
     * Returns non-double table data and appends units at the end of each row.
     * @param jtable JTable
     * @param start Start column index
     * @param end End column index
     * @return 2D array of table data
     */
    public static String[][] getTableNonData(JTable jtable, int start, int end) {
        String[][] data = new String[jtable.getRowCount()][end - start + 1];
        if (debug)
            System.out.println(
                    "ModelInterfaceUtil:getTableNonData:start: " + start + " end: " + end + " row: " + data.length);
        for (int i = 0; i < jtable.getRowCount(); i++) {
            for (int j = start; j < end; j++) {
                String cls = jtable.getColumnClass(j).getName();
                if (!cls.equals("java.lang.Double")) {
                    data[i][j - start] = ((String) jtable.getValueAt(i, j));
                }
            }
            // Append units at end of each row
            String units = (String) jtable.getValueAt(i, jtable.getColumnCount() - 1);
            if (units == null || units.trim().isEmpty()) {
                units = "NA";
            }
            data[i][end] = units;
            if (debug)
                System.out.println("ModelInterfaceUtil:getTableData:data: " + Arrays.toString(data[i]));
        }
        return data;
    }

    /**
     * Builds a chart data value offset with a chart key.
     * @param jtable JTable
     * @param col Array of column names
     * @return Map of key to offset indices
     */
    public static Map<String, Integer[]> getMetaIndex(JTable jtable, String[] col) {
        String[][] data = getDataFromTable(jtable, 3);
        Map<String, Integer[]> metaMap = new LinkedHashMap<String, Integer[]>();
        for (String s : col) {
            String tempKey = "";
            // Build the first compare key up to data columns
            for (int j = 0; j < data[0].length - 1; j++)
                tempKey = tempKey + " " + data[0][j].replace(" ", "_");
            tempKey = tempKey + " " + s;
            ArrayList<Integer> offsetIndex = new ArrayList<Integer>();
            for (int i = 0; i < data.length; i++) {
                String keyStr = "";
                for (int j = 0; j < data[i].length - 1; j++)
                    keyStr = keyStr + " " + data[i][j].replace(" ", "_");
                if (!tempKey.trim().equals(keyStr.trim()))
                    tempKey = keyStr;
                // Get match qualifier's data value offset (location in JTable)
                if ((tempKey.trim() + " " + s)
                        .equals((keyStr.trim()) + " " + data[i][data[i].length - 1].replace(" ", "_")))
                    offsetIndex.add(i);
            }
            Integer[] offset = new Integer[offsetIndex.size()];
            for (int i = 0; i < offsetIndex.size(); i++)
                offset[i] = offsetIndex.get(i);
            metaMap.put(s, offset);
            if (debug)
                System.out.println("getMetaIndex:key: " + s + " offset: " + offsetIndex.toString());
        }
        return metaMap;
    }

    /**
     * Builds a chart data value range with a qualified chart key.
     * @param jtable JTable
     * @param cnt Index for double type column
     * @return Map of key to begin/end indices
     */
    public static Map<String, Integer[]> getMetaIndex(JTable jtable, int cnt) {
        String[][] data = getDataFromTable(jtable, cnt, 2);
        Map<String, Integer[]> metaMap = new LinkedHashMap<String, Integer[]>();
        Map<String, Integer> metaMap0 = new LinkedHashMap<String, Integer>();
        Map<String, Integer> metaMap1 = new LinkedHashMap<String, Integer>();
        Integer[] beginEnd = { 0, 0 };
        ArrayList<Integer> matchingRows = new ArrayList<Integer>();
        String tempKey = "";
        for (int j = data[0].length - 1; j >= 0; j--)
            tempKey = tempKey + " " + data[0][j];
        for (int i = 0; i < data.length; i++) {
            String keyStr = "";
            for (int j = data[0].length - 1; j >= 0; j--)
                keyStr = keyStr + " " + data[i][j];
            if (!tempKey.trim().equals(keyStr.trim())) {
                metaMap0.put(tempKey.trim(), beginEnd[0]);
                metaMap1.put(tempKey.trim(), beginEnd[1]);
                beginEnd[0] = beginEnd[1] + 1;
                tempKey = keyStr;
            } else {
                matchingRows.add(new Integer(i));
            }
            beginEnd[1] = i;
            if (i == data.length - 1) {
                metaMap0.put(tempKey.trim(), beginEnd[0]);
                metaMap1.put(tempKey.trim(), beginEnd[1]);
                break;
            }
        }
        for (String s : metaMap0.keySet()) {
            Integer b = metaMap0.get(s);
            Integer e = metaMap1.get(s);
            Integer[] intv = { b, e };
            metaMap.put(s, intv);
            Integer[] intv1 = metaMap.get(s);
            if (debug)
                System.out.println(
                        "getMetaIndex:key: " + s + "  " + intv1[0] + "  " + intv1[1] + " b: " + b + " e: " + e);
        }
        return metaMap;
    }

    /**
     * Builds a chart data value range with a qualified chart key (alternative version).
     * @param jtable JTable
     * @param cnt Index for double type column
     * @return Map of key to matching row indices
     */
    public static Map<String, Integer[]> getMetaIndex2(JTable jtable, int cnt) {
        Map<String, Integer[]> metaMap = new LinkedHashMap<String, Integer[]>();
        String[][] data = getDataFromTable(jtable, cnt, 2);
        boolean[] matched = new boolean[data.length];
        for (int i = 0; i < data.length; i++) {
            matched[i] = false;
        }
        // Loop over rows
        for (int row = 0; row < data.length; row++) {
            if (!matched[row]) {
                ArrayList<Integer> matchingRows = new ArrayList<Integer>();
                // Get key for this row
                String rowKey = "";
                for (int col = data[0].length - 1; col >= 0; col--) {
                    rowKey = rowKey + " " + data[row][col];
                }
                rowKey = rowKey.trim();
                // Loop over lower rows
                for (int row2 = row; row2 < data.length; row2++) {
                    if (!matched[row2]) {
                        String keyStr = "";
                        for (int col = data[0].length - 1; col >= 0; col--) {
                            keyStr = keyStr + " " + data[row2][col];
                        }
                        keyStr = keyStr.trim();
                        if (rowKey.equals(keyStr)) {
                            matchingRows.add(new Integer(row2));
                            matched[row2] = true;
                        }
                    }
                }
                if (matchingRows != null && matchingRows.size() > 0) {
                    Integer[] matching_rows = new Integer[matchingRows.size()];
                    for (int i = 0; i < matchingRows.size(); i++) {
                        matching_rows[i] = matchingRows.get(i);
                    }
                    metaMap.put(rowKey, matching_rows);
                }
            }
        }
        return metaMap;
    }

    /**
     * Gets the common legends (attributes) with a given qualifier.
     * @param metaMap Map of key to indices
     * @param data Table data
     * @return Array of legend strings
     */
    public static String[] getLegend(Map<String, Integer[]> metaMap, String[][] data) {
        if (debug)
            System.out.println("getLegend:len: " + data.length + "  " + data[0].length);
        Map<String, String> legendMap = new LinkedHashMap<String, String>();
        Iterator<String> it = metaMap.keySet().iterator();
        ArrayList<String> lgdData = new ArrayList<String>();
        boolean q = true;
        while (it.hasNext()) {
            String key = it.next().replace(",depth=1", "");
            if (key.split(" ").length > 2) {
                Integer[] beginEnd = metaMap.get(key);
                if (debug)
                    System.out.println("getLegend:key: " + key + " b: " + beginEnd[0].intValue() + " e: "
                            + beginEnd[1].intValue());
                try {
                    if (beginEnd.length == 1) {
                        lgdData.add(data[0][0]);
                    } else {
                        for (int i = beginEnd[0].intValue(); i <= beginEnd[1].intValue(); i++)
                            lgdData.add(data[i][0]);
                    }
                } catch (Exception e) {
                    q = false;
                    break;
                }
            } else {
                q = false;
                break;
            }
        }
        String[] lgd = null;
        if (q)
            lgd = lgdData.toArray(new String[0]);
        else
            lgd = ArrayConversion.arrayDimReverse(data)[0];
        for (int i = 0; i < lgd.length; i++) {
            String keyStr = lgd[i];
            legendMap.put(keyStr.trim(), keyStr.trim());
        }
        return legendMap.values().toArray(new String[0]);
    }

    /**
     * Gets the common legends (attributes) with a given qualifier (alternative version).
     * @param metaMap Map of key to indices
     * @param data Table data
     * @return Array of legend strings
     */
    public static String[] getLegend2(Map<String, Integer[]> metaMap, String[][] data) {
        if (debug)
            System.out.println("getLegend:len: " + data.length + "  " + data[0].length);
        Map<String, String> legendMap = new LinkedHashMap<String, String>();
        Iterator<String> it = metaMap.keySet().iterator();
        ArrayList<String> lgdData = new ArrayList<String>();
        boolean q = true;
        while (it.hasNext()) {
            String key = it.next().replace(",depth=1", "");
            if (key.split(" ").length > 2) {
                Integer[] matches = metaMap.get(key);
                try {
                    for (int i = 0; i < matches.length; i++) {
                        lgdData.add(data[matches[i]][0]);
                    }
                } catch (NullPointerException e) {
                    q = false;
                    break;
                }
            } else {
                q = false;
                break;
            }
        }
        String[] lgd = null;
        if (q)
            lgd = lgdData.toArray(new String[0]);
        else
            lgd = ArrayConversion.arrayDimReverse(data)[0];
        for (int i = 0; i < lgd.length; i++) {
            String keyStr = lgd[i];
            legendMap.put(keyStr.trim(), keyStr.trim());
        }
        return legendMap.values().toArray(new String[0]);
    }

    /**
     * Returns index of first double type column in JTable.
     * @param jtable JTable
     * @return Index of double type column
     */
    public static int getDoubleTypeColIndex(JTable jtable) {
        int idx = 0;
        for (int j = 0; j < jtable.getColumnCount(); j++) {
            if (!jtable.getColumnClass(j).getName().equals("java.lang.Double"))
                idx++;
            else
                break;
        }
        // Fallback: find first column with year in range
        if (idx >= jtable.getColumnCount()) {
            for (int j = 0; j < jtable.getColumnCount(); j++) {
                String str = jtable.getColumnName(j);
                try {
                    int year = Integer.parseInt(str);
                    if ((year > 1975) && (year < 2105)) {
                        idx = j;
                        break;
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        return idx;
    }

    /**
     * Returns index of first double type column in TableModel.
     * @param jtable TableModel
     * @return Index of double type column
     */
    public static int getDoubleTypeColIndex(TableModel jtable) {
        int idx = 0;
        for (int j = 0; j < jtable.getColumnCount(); j++)
            if (!jtable.getColumnClass(j).getName().equals("java.lang.Double"))
                idx++;
            else
                break;
        if (idx >= jtable.getColumnCount()) {
            for (int j = 0; j < jtable.getColumnCount(); j++) {
                String str = jtable.getColumnName(j);
                try {
                    int year = Integer.parseInt(str);
                    if ((year > 1975) && (year < 2105)) {
                        idx = j;
                        break;
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        return idx;
    }

    /**
     * Returns index of first double type column in array of class names.
     * @param cls Array of class names
     * @return Index of double type column
     */
    public static int getDoubleTypeColIndex(String[] cls) {
        int idx = 0;
        for (int j = 0; j < cls.length; j++)
            if (!cls[j].equals("java.lang.Double"))
                idx++;
            else
                break;
        if (idx >= cls.length) {
            for (int j = 0; j < cls.length; j++) {
                String str = cls[j];
                try {
                    int year = Integer.parseInt(str);
                    if ((year > 1975) && (year < 2105)) {
                        idx = j;
                        break;
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        return idx;
    }

}