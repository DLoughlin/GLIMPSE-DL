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
import java.util.List;

import javax.swing.JOptionPane;

import chart.CategoryBoxAndWhiskerChart;
import chart.Chart;
import chart.DatasetUtil;
import ModelInterface.InterfaceMain;

/**
 * Handles the creation and management of a box-and-whisker statistics chart pane.
 * Supports displaying statistics for a subset of series or data range.
 *
 * <p>Author: TWU<br>
 * Created: 1/2/2016
 * </p>
 */
public class BoxAndWhiskerChartPane {

    /**
     * The chart instance managed by this pane.
     */
    private Chart chart;

    /**
     * Constructs a BoxAndWhiskerChartPane using the provided array of charts.
     *
     * @param charts Array of Chart objects to be included in the box-and-whisker chart.
     * @throws ClassNotFoundException if a required class for chart creation is not found.
     */
    public BoxAndWhiskerChartPane(Chart[] charts) throws ClassNotFoundException {
        // Retrieve statistics data for the provided charts
        ArrayList<List<String[]>> statisticsData = new ArrayList<>();
        try {
            statisticsData = DatasetUtil.getStatisticsData(charts);
        } catch (java.lang.IndexOutOfBoundsException e1) {
            statisticsData.clear();
        }
        // If no data is available, show an information dialog and exit
        if (statisticsData.isEmpty()) {
            JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(),
                "No Support for different number of technologies for each chart",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Build chart identifier string from chart metadata
        StringBuilder idBuilder = new StringBuilder();
        for (Chart chart : charts) {
            idBuilder.append(",").append(chart.getMeta()).append("|").append(chart.getMetaCol());
        }
        String id = idBuilder.toString();
        // Create the CategoryBoxAndWhiskerChart using the first chart's properties and the statistics data
        chart = new CategoryBoxAndWhiskerChart(
            charts[0].getPath(),
            "BoxAndWhisker_" + charts[0].getGraphName(),
            id,
            charts[0].getTitles(),
            charts[0].getAxis_name_unit(),
            charts[0].getChartColumn(),
            charts[0].getChartRow(),
            null,
            statisticsData
        );
    }

    /**
     * Returns the chart managed by this pane.
     *
     * @return the Chart instance
     */
    public Chart getChart() {
        return chart;
    }
}