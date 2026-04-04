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
 * Parks and Yadong Xu of ARA through the EPA’s Environmental Modeling and 
 * Visualization Laboratory contract. 
 */
package chartOptions;

import java.awt.Paint;
import java.awt.TexturePaint;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYDataset;

import chart.CategoryPieChart;
import chart.DatasetUtil;
import chart.LegendUtil;
import ModelInterface.InterfaceMain;

/**
 * Utility functions for chart options, including pie chart display and paint management.
 *
 * Referenced classes of package graphDisplay: AChartDisplay, DataPanel
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class ChartOptionsUtil {

  private static java.awt.Component getDialogParent() {
    try {
      return InterfaceMain.getInstance().getFrame();
    } catch (Exception ignored) {
      return null;
    }
  }

    /**
     * Displays a pie chart based on the provided JFreeChart and options.
     *
     * @param path Path for chart output
     * @param graphName Name of the graph
     * @param meta Metadata string
     * @param axis_name_unit Axis names and units
     * @param jfchart Source JFreeChart
     * @return JFreeChart instance for the pie chart
     */
    public static JFreeChart showPieChart(String path, String graphName, String meta, String[] axis_name_unit,
            JFreeChart jfchart) {
        JFreeChart jfreechart = null;
        String[] row = null;
        String[] column = null;
        Paint[] paint = null;
        int selIndex;

        String chartType = chartTypeDialog();
        TableOrder tableOrder = tableOrderDialog();

        // Handle XY plot type
        if (jfchart.getPlot().getPlotType().contains("XY")) {
            XYDataset[] xyds = { jfchart.getXYPlot().getDataset() };
            DefaultCategoryDataset dataset = DatasetUtil.XYDataset2CategoryDataset(xyds[0]);
            row = conversionUtil.ArrayConversion.list2Array(dataset.getRowKeys());
            column = conversionUtil.ArrayConversion.list2Array(dataset.getColumnKeys());
            paint = getPaintArray(LegendUtil.getLegendPaint(jfchart.getXYPlot().getFixedLegendItems()));
            selIndex = selectedDialog(chartType, tableOrder, row, column);
            jfreechart = new CategoryPieChart(path, graphName, meta, jfchart.getTitle().getText(), axis_name_unit, row,
                    paint, tableOrder, selIndex, dataset, chartType).getChart();
        } 
        // Handle Category plot type
        else if (jfchart.getPlot().getPlotType().contains("Category")) {
            DefaultCategoryDataset dataset = (DefaultCategoryDataset) jfchart.getCategoryPlot().getDataset();
            row = conversionUtil.ArrayConversion.list2Array(dataset.getRowKeys());
            column = conversionUtil.ArrayConversion.list2Array(dataset.getColumnKeys());
            paint = getPaintArray(LegendUtil.getLegendPaint(jfchart.getCategoryPlot().getFixedLegendItems()));
            selIndex = selectedDialog(chartType, tableOrder, row, column);
            jfreechart = new CategoryPieChart(path, graphName, meta, jfchart.getTitle().getText(), axis_name_unit, row,
                    paint, tableOrder, selIndex, dataset, chartType).getChart();
        } 
        // Handle Pie plot type
        else if (jfchart.getPlot().getPlotType().contains("Pie")) {
            PieDataset dataset = ((PiePlot) jfchart.getPlot()).getDataset();
            row = conversionUtil.ArrayConversion.list2Array(dataset.getKeys());
            column = row.clone();
            paint = getPaintArray(LegendUtil.getLegendPaint(((PiePlot) jfchart.getPlot()).getLegendItems()));
            selIndex = selectedDialog(chartType, tableOrder, row, column);
            jfreechart = new CategoryPieChart(path, graphName, meta, jfchart.getTitle().getText(), axis_name_unit, row,
                    paint, tableOrder, selIndex, dataset, chartType).getChart();
        }

        return jfreechart;
    }

    /**
     * Dialog for selecting chart type.
     * @return Selected chart type as String
     */
    private static String chartTypeDialog() {
        String[] data = { "Pie Chart", "3D Pie Chart", "Multiple 3D Pie Chart" };
        return (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Pie Chart",
                JOptionPane.INFORMATION_MESSAGE, null, data, data[0]);
    }

    /**
     * Dialog for selecting table order (row/column).
     * @return Selected TableOrder
     */
    private static TableOrder tableOrderDialog() {
        TableOrder extract = TableOrder.BY_COLUMN;
        if (JOptionPane.showConfirmDialog(getDialogParent(), "choose one", "Select a Column?",
                JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.NO_OPTION)
            extract = TableOrder.BY_ROW;
        return extract;
    }

    /**
     * Dialog for selecting category index.
     * @param chartType Chart type
     * @param tableOrder Table order
     * @param row Row keys
     * @param column Column keys
     * @return Selected index
     */
    private static int selectedDialog(String chartType, TableOrder tableOrder, String[] row, String[] column) {
        if (!chartType.equals("Multiple 3D Pie Chart")) {
            if (tableOrder == TableOrder.BY_COLUMN) {
                return Arrays.asList(column).indexOf((String) JOptionPane.showInputDialog(getDialogParent(), "Choose Categories",
                        "Select Categories", JOptionPane.INFORMATION_MESSAGE, null, column, column[0]));
            } else {
                return Arrays.asList(row).indexOf((String) JOptionPane.showInputDialog(getDialogParent(), "Choose Categories",
                        "Select Categories", JOptionPane.INFORMATION_MESSAGE, null, row, row[0]));
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns a reversed copy of the paint array.
     * @param temp Input paint array
     * @return Reversed paint array
     */
    private static Paint[] getPaintArray(Paint[] temp) {
        Paint[] paint = new Paint[temp.length];
        for (int i = paint.length - 1; i > -1; i--)
            paint[i] = temp[paint.length - 1 - i];
        return paint;
    }

    /**
     * Checks if the chart is a 3D chart. Always returns false (3D charts not supported).
     * @param jfchart JFreeChart instance
     * @return false
     */
    public static boolean is3DChart(JFreeChart jfchart) {
        return false;
    }

    /**
     * Attempts to change chart type to 3D. Shows info dialog (3D not supported).
     * @param tp TexturePaint array
     * @param jfchart JFreeChart instance
     * @param stateChange State change indicator
     */
    public static void changeChartType(TexturePaint[] tp, JFreeChart jfchart, int stateChange) {
        JOptionPane.showMessageDialog(getDialogParent(), "No 3D View", "Information", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    /**
     * Repaints the chart series with the provided TexturePaints.
     * @param tp TexturePaint array
     * @param jfchart JFreeChart instance
     */
    public static void repaint(TexturePaint[] tp, JFreeChart jfchart) {
        AbstractRenderer renderer = null;
        // Determine renderer type based on plot
        if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
            renderer = (AbstractRenderer) jfchart.getCategoryPlot().getRenderer();
        } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
            renderer = (AbstractRenderer) jfchart.getXYPlot().getRenderer();
        } else {
            return;
        }

        // Apply texture paints to each series
        for (int i = 0; i < tp.length; i++) {
            renderer.setSeriesPaint(i, tp[i]);
        }
    }
}