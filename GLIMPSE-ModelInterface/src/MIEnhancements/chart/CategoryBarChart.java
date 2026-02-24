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
package chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * CategoryBarChart creates a JFreeChart bar chart with properties from the parent CategoryChart.
 * <p>
 * Author: TWU
 * Date: 1/2/2016
 */
public class CategoryBarChart extends CategoryChart {
    /** Renderer for customizing bar appearance */
    protected BarRenderer renderer;

    /**
     * Constructs a CategoryBarChart with detailed chart properties.
     *
     * @param path              Path for chart output
     * @param graphName         Chart name
     * @param meta              Metadata
     * @param titles            Chart titles
     * @param axisName_unit     Axis names and units
     * @param legend            Legend text
     * @param color             Bar colors
     * @param pColor            Pattern colors
     * @param pattern           Bar patterns
     * @param lineStrokes       Line strokes
     * @param annotationText    Annotation text
     * @param dataset           Data for chart
     * @param relativeColIndex  Relative column index
     * @param ShowLineAndShape  Show line and shape
     * @param graphType         Type of graph
     */
    public CategoryBarChart(String path, String graphName, String meta, String[] titles,
                           String[] axisName_unit, String legend, int[] color, int[] pColor,
                           int[] pattern, int[] lineStrokes, String[][] annotationText,
                           DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape, String graphType) {
        super(path, graphName, meta, titles, axisName_unit, legend, color, pColor,
                pattern, lineStrokes, annotationText, dataset,
                relativeColIndex, ShowLineAndShape, graphType);
        chartClassName = "chart.CategoryBarChart";
        crtChart();
    }

    /**
     * Constructs a CategoryBarChart with basic chart properties and data.
     *
     * @param path              Path for chart output
     * @param graphName         Chart name
     * @param meta              Metadata
     * @param titles            Chart titles
     * @param axis_name_unit    Axis names and units
     * @param legend            Legend text
     * @param column            Data column
     * @param annotationText    Annotation text
     * @param data              Chart data
     * @param relativeColIndex  Relative column index
     */
    public CategoryBarChart(String path, String graphName, String meta, String[] titles,
                           String[] axis_name_unit, String legend, String column, String[][] annotationText, String[][] data,
                           int relativeColIndex) {
        super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
                data, relativeColIndex);
        chartClassName = "chart.CategoryBarChart";
        crtChart();
    }

    /**
     * Initializes and configures the bar chart and its renderer.
     */
    private void crtChart() {
        // Set legacy chart theme for consistent appearance
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());

        // Create the bar chart
        chart = ChartFactory.createBarChart("", verifyAxisName_unit(0),
                verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
                true, true, false);
        plot = (CategoryPlot) chart.getPlot();
        plot.setDataset(0, dataset);
        renderer = (BarRenderer) plot.getRenderer(0);
        renderer.setMaximumBarWidth(.30); // Limit bar width for aesthetics
        renderer.setBarPainter(new StandardBarPainter()); // Use standard painter
        renderer.setShadowVisible(false); // Remove bar shadow

        // Set chart properties
        setPlotProperty();
        setLegendProperty();
        setAxisProperty();
        RendererUtil.setRendererProperty(renderer);
    }
}