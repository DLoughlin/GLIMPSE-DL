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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JList;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.nfunk.jep.JEP;

import chart.Chart;

/**
 * Utility functions for the GraphDisplay package.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class GraphDisplayUtil {
    private static boolean debug = false;

    /**
     * Build qualifiers for base selector - it is a multiple-level selection.
     *
     * @param qualifier array of qualifier names (not null)
     * @param data values of qualifiers (not null)
     * @return list of unique values for each qualifier
     */
    public static ArrayList<String[]> getUniqQualifierData(String[] qualifier, String[][] data) {
        ArrayList<String[]> al = new ArrayList<String[]>();
        for (int i = 0; i < qualifier.length; i++) {
            ArrayList<String> al1 = new ArrayList<String>();
            String[] temp = data[i];
            Arrays.sort(temp, null); // Sort the data for uniqueness
            String t = temp[0].trim();
            al1.add(t);
            for (int j = 1; j < temp.length; j++) {
                if (!t.equals(temp[j].trim())) {
                    al1.add(temp[j]);
                    t = temp[j].trim();
                }
            }
            al.add(i, al1.toArray(new String[0]));
        }
        return al;
    }

    /**
     * Display JFreeChart instance of selected rows with subset of selected columns.
     *
     * @param row indexes of selected rows (not null)
     * @param chart chart which rows and columns selected upon (not null)
     */
    public static void showSelectRow(int[] row, JFreeChart chart) {
        if (debug)
            System.out.println("GraphDisplayUtil::showSelectRow:row: " + Arrays.toString(row));
        if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
            if (row != null) {
                // Hide all series first
                for (int i = 0; i < chart.getCategoryPlot().getDataset().getRowCount(); i++) {
                    renderer.setSeriesVisible(i, Boolean.valueOf(false));
                    renderer.setSeriesVisibleInLegend(i, Boolean.valueOf(false));
                }
                // Show only selected series
                for (int i = 0; i < row.length; i++) {
                    renderer.setSeriesVisible(row[i], Boolean.valueOf(true));
                    renderer.setSeriesVisibleInLegend(row[i], Boolean.valueOf(true));
                }
            }
        } else {
            if (row != null) {
                XYItemRenderer renderer = chart.getXYPlot().getRenderer();
                // Hide all series first
                for (int i = 0; i < chart.getXYPlot().getDataset().getSeriesCount(); i++)
                    renderer.setSeriesVisible(i, Boolean.valueOf(false));
                // Show only selected series
                for (int i = 0; i < row.length; i++)
                    renderer.setSeriesVisible(row[i], Boolean.valueOf(true));
            }
        }
    }

    /**
     * Display JFreeChart instance of selected rows with subset of selected columns.
     *
     * @param row names of selected rows (not null)
     * @param chart chart which rows and columns selected upon (not null)
     */
    public static void showSelectRow(String[] row, JFreeChart chart) {
        // Hide all rows then only show selected rows
        if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
            if (row != null) {
                for (int i = 0; i < chart.getCategoryPlot().getDataset().getRowCount(); i++) {
                    renderer.setSeriesVisible(i, Boolean.valueOf(false));
                    renderer.setSeriesVisibleInLegend(i, Boolean.valueOf(false));
                }
                for (int i = 0; i < row.length; i++) {
                    int idx = chart.getCategoryPlot().getDataset().getRowIndex(row[i]);
                    renderer.setSeriesVisible(idx, Boolean.valueOf(true));
                    renderer.setSeriesVisibleInLegend(idx, Boolean.valueOf(true));
                }
            }
        } else {
            if (row != null) {
                XYItemRenderer renderer = chart.getXYPlot().getRenderer();
                for (int i = 0; i < chart.getXYPlot().getDataset().getSeriesCount(); i++)
                    renderer.setSeriesVisible(i, Boolean.valueOf(false));
                for (int i = 0; i < row.length; i++) {
                    int idx = chart.getXYPlot().getDataset().indexOf(row[i]);
                    renderer.setSeriesVisible(idx, Boolean.valueOf(true));
                }
            }
        }
        ChartUtils.applyCurrentTheme(chart);
    }

    /**
     * Show all columns/series in the chart (used in BoxAndWhisker plots).
     *
     * @param chart JFreeChart instance
     */
    public static void showSelectColumn(JFreeChart chart) {
        if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer();
            for (int i = 0; i < chart.getCategoryPlot().getDataset().getRowCount(); i++)
                renderer.setSeriesVisible(i, Boolean.valueOf(true));
        } else {
            XYItemRenderer renderer = chart.getXYPlot().getRenderer();
            for (int i = 0; i < chart.getXYPlot().getDataset().getSeriesCount(); i++)
                renderer.setSeriesVisible(i, Boolean.valueOf(true));
        }
    }

    /**
     * Convert a date string (MM/dd/yyyy) to a long value representing milliseconds since epoch.
     *
     * @param s date string
     * @return milliseconds since epoch
     */
    public static long getDayLong(String s) {
        java.util.Date d = null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        try {
            if (s != null && !s.equals(""))
                d = sdf.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d.getTime();
    }

    /**
     * Check if a value exists in an array.
     *
     * @param computeFC array of values
     * @param k value to check
     * @return true if k exists in computeFC
     */
    public static boolean computIt(int[] computeFC, int k) {
        for (int i = 0; i < computeFC.length; i++) {
            if (k == computeFC[i])
                return true;
        }
        return false;
    }

    /**
     * Stub for computing function column (not implemented).
     *
     * @param expression mathematical expression
     * @return null
     */
    public static String[][] computeFunctionColumn(String expression) {
        JEP myParser = new JEP();
        myParser.parseExpression(expression);
        // TODO: Implement computation logic
        return null;
    }

    /**
     * Create a JList containing chart metadata.
     *
     * @param charts array of Chart objects
     * @return JList of metadata strings
     */
    public static JList<Object> metaList(Chart[] charts) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < charts.length; i++) {
            // Replace commas in metadata to avoid issues
            String meta = charts[i].getMeta().replace(",", "_") + "," + String.valueOf(i);
            map.put(meta, meta);
        }
        Object selOption[] = map.values().toArray();
        JList<Object> list = new JList<Object>(selOption);
        return list;
    }
}