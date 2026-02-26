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
package chartOptions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import chart.Chart;
import chart.LegendUtil;
import conversionUtil.ArrayConversion;

/**
 * Utility class for modifying chart legend, color, pattern, stroke, and shape options.
 * Provides static methods to update chart appearance and legend items.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class SetModifyChanges {
    protected static boolean debug = false;

    /**
     * Updates legend items for each chart and applies current theme.
     * @param chart Array of Chart objects
     * @param id Chart index to apply theme
     * @param tf JTextField containing legend info
     */
    public static void setLegendChanges(Chart[] chart, int id, JTextField tf) {
        for (int i = 0; i < chart.length; i++) {
            String[] legend = chart[i].getLegend().split(",");
            chart[i].setLegend(ArrayConversion.array2String(legend));
            for (int j = 0; j < legend.length; j++) {
                setLegenditemcollection(chart[i], legend, chart[i].getPaint());
                setModifyChanges(chart[i], chart[i].getPaint());
                if (debug)
                    System.out.println("SetModifyChanges::setLegendChanges:legend " + chart[i].getLegend());
                ChartUtils.applyCurrentTheme(chart[id].getChart());
            }
        }
    }

    /**
     * Applies color changes to all charts and updates legend items.
     * @param chart Array of Chart objects
     */
    public static void setColorChanges(Chart[] chart) {
        for (int i = 0; i < chart.length; i++) {
            String[] legend = chart[i].getLegend().split(",");
            TexturePaint[] tp_color = chart[i].getPaint();
            setLegenditemcollection(chart[i], legend, tp_color);
            setModifyChanges(chart[i], tp_color);
            if (debug)
                System.out.println("SetModifyChanges::setLegendChanges:color " + Arrays.toString(chart[i].getColor()));
        }
    }

    /**
     * Applies pattern changes to chart legend items using combo box selections.
     * @param chart Array of Chart objects
     * @param comboLookup Map of legend names to JComboBox for pattern selection
     * @param patternNums Array of pattern numbers
     */
    public static void setPatternChanges(Chart[] chart, HashMap<String, JComboBox> comboLookup, int[] patternNums) {
        for (int i = 0; i < chart.length; i++) {
            String[] legend = chart[i].getLegend().split(",");
            TexturePaint[] tp_color = chart[i].getPaint();
            for (int j = 0; j < legend.length; j++) {
                int sel = 0;
                if (comboLookup.get(legend[j].trim()) != null) {
                    sel = comboLookup.get(legend[j].trim()).getSelectedIndex();
                }
                Color c = new Color(chart[i].getColor()[j]);
                Color contrast = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
                TexturePaint tpMerge = LegendUtil.getTexturePaint(new Color(chart[i].getColor()[j]), contrast, patternNums[sel], 1);
                tp_color[j] = tpMerge;
                chart[i].setPattern(patternNums[sel], j);
                setLegenditemcollection(chart[i], legend, tp_color);
                setModifyChanges(chart[i], tp_color);
            }
        }
    }

    /**
     * Sets the series paint for each legend item in the chart using TexturePaint array.
     * @param chart Chart object
     * @param tp Array of TexturePaint objects
     * @return Modified JFreeChart object
     */
    public static JFreeChart setModifyChanges(Chart chart, TexturePaint[] tp) {
        AbstractRenderer renderer = null;
        JFreeChart jfchart = chart.getChart();
        if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
            CategoryPlot plot = jfchart.getCategoryPlot();
            renderer = (AbstractRenderer) plot.getRenderer();
        } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
            XYPlot plot = (XYPlot) jfchart.getPlot();
            renderer = (AbstractRenderer) plot.getRenderer();
        }
        for (int idx = 0; idx < tp.length; idx++) {
            renderer.setSeriesPaint(idx, tp[idx]);
        }
        ChartUtils.applyCurrentTheme(jfchart);
        return jfchart;
    }

    /**
     * Updates the legend item collection for the chart using legend names and colors.
     * @param chart Chart object
     * @param legend Array of legend names
     * @param color Array of TexturePaint objects
     */
    public static void setLegenditemcollection(Chart chart, String[] legend, TexturePaint[] color) {
        if (chart.getChartClassName().contains("Category")) {
            chart.getChart().getCategoryPlot().setFixedLegendItems(LegendUtil.crtLegenditemcollection(legend, color));
        } else if (chart.getChartClassName().contains("XY")) {
            chart.getChart().getXYPlot().setFixedLegendItems(LegendUtil.crtLegenditemcollection(legend, color));
        }
    }

    /**
     * Applies stroke changes to line charts using combo box selections.
     * @param chart Array of Chart objects
     * @param strokeLookup Map of legend names to JComboBox for stroke selection
     * @param stroke Array of BasicStroke objects
     * @param stokeVals Array of stroke values
     */
    public static void setStrokeChanges(Chart[] chart, HashMap<String, JComboBox> strokeLookup, BasicStroke[] stroke, int[] stokeVals) {
        for (int j = 0; j < chart.length; j++) {
            if (!chart[j].getChartClassName().contains("Line"))
                continue;
            String[] legend = chart[j].getLegend().split(",");
            int[] ls = chart[j].getLineStrokes();
            JFreeChart jfchart = chart[j].getChart();
            for (int i = 0; i < legend.length; i++) {
                int idx = strokeLookup.get(legend[i].trim()).getSelectedIndex();
                if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
                    CategoryPlot plot = jfchart.getCategoryPlot();
                    LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
                    renderer.setSeriesStroke(i, stroke[idx]);
                    ls[i] = stokeVals[idx];
                } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
                    XYPlot plot = jfchart.getXYPlot();
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
                    renderer.setSeriesStroke(i, stroke[idx]);
                    ls[i] = stokeVals[idx];
                }
                ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
                ChartUtils.applyCurrentTheme(jfchart);
            }
        }
    }

    /**
     * Updates the icon of a JButton to display the given TexturePaint.
     * @param jb JButton to update
     * @param paint Paint object (should be TexturePaint)
     */
    public static void updateButton(JButton jb, Paint paint) {
        ImageIcon icon = new ImageIcon(((TexturePaint) paint).getImage());
        Image image = icon.getImage();
        image = image.getScaledInstance(80, 20, Image.SCALE_SMOOTH);
        icon.setImage(image);
        jb.setIcon(icon);
    }

    /**
     * Sets whether shapes are visible for line and shape renderers in the chart.
     * @param jfchart JFreeChart object
     * @param lineAndShape true to show shapes, false to hide
     */
    public static void setLineAndShapeChanges(JFreeChart jfchart, boolean lineAndShape) {
        if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
            CategoryPlot plot = jfchart.getCategoryPlot();
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            for (int i = 0; i < plot.getLegendItems().getItemCount(); i++)
                renderer.setSeriesShapesVisible(i, lineAndShape);
        } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
            XYPlot plot = jfchart.getXYPlot();
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            for (int i = 0; i < plot.getLegendItems().getItemCount(); i++)
                renderer.setSeriesShapesVisible(i, lineAndShape);
        }
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        ChartUtils.applyCurrentTheme(jfchart);
    }
}