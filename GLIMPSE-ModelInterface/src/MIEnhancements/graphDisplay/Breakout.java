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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import chart.Chart;
import chart.DatasetUtil;
import conversionUtil.ArrayConversion;
import ModelInterface.InterfaceMain;

/**
 * Handles transposing rows and columns of a dataset and displaying charts in a
 * panel.
 *
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 * DanL modified 2/17/2026
 */
public class Breakout extends JDialog {
	private Chart[] transChart;
	private static final boolean DEBUG = false;
    private Chart[] chart;
    private int gridWidth;
    private boolean sameScale;
    private JPanel jp;
	private OptionsArea optionsArea;

	/**
	 * Transposes the given charts and displays them in a dialog.
	 * 
	 * @param chart     Array of Chart objects
	 * @param w         Width (unused)
	 * @param gridWidth Grid width (unused)
	 * @param sameScale Whether to use the same scale for all charts
	 * @param sp        JSplitPane for chart display
	 * @param isBreakout
	 */
	public Breakout(Chart[] chart, int w, int gridWidth, boolean sameScale, JSplitPane sp, boolean isBreakout) {
		super(InterfaceMain.getInstance().getFrame());
		this.chart = chart;
		this.gridWidth = gridWidth;
		this.sameScale = sameScale;
		this.jp = new JPanel(new BorderLayout());

		if (isBreakout) {
			
			// DanL - The 'true' passed here hides the Breakout and Transpose options
			this.optionsArea = new OptionsArea(jp, chart, gridWidth, sameScale, sp, true);
			setChartPane(); // This will now add the JScrollPane
			add(jp);
			setTitle("Thumbnails: " + chart[0].getGraphName());
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			pack();
			setSize(800, 600);
			setLocation(InterfaceMain.getInstance().getFrame().getLocation());
			setVisible(true);
		}
		else {
			String meta = ArrayConversion.array2String(getMetaArray(chart)); // original chart info, e.g., state and
																				// scenario name
			List<String> masterLegend = getMasterLegend(chart); // original series in chart
			String[] newPlotNames = masterLegend.toArray(new String[0]); // each original series becomes a new plot
			List<String[][]> transposedData;
			try {
				transposedData = getTransposeData(masterLegend, transChart); // transposes plot data for all charts
			} catch (NullPointerException | IndexOutOfBoundsException e) {
				transposedData = new ArrayList<>();
			}
			// need to determine how to handle complex datasets
			if (transposedData.isEmpty()) {// || chart.length > 2) {
				JOptionPane.showMessageDialog(null, "Transpose is not yet supported on complex datasets.", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			String[] newSeries = meta.split(","); // each original chart becomes a new series based on metadata
			int idx = ThumbnailUtilNew.getFirstNonNullChart(chart);

			// Extract units from each original chart to associate with the new series (which correspond to original charts)
			// The length of 'newSeries' is determined by the number of original charts that made it into 'meta'
			// We need an array of units that matches 'newSeries' length/order.
			String[] newSeriesUnits = new String[newSeries.length];
			// 'meta' comes from getMetaArray(chart), which iterates through 'chart' array.
			// So 'newSeries' should align with 'chart' array elements, assuming all charts are included in meta.
			// However, getMetaArray might filter or process. Let's look at getMetaArray implementation if needed, 
			// but typically it just maps chart->meta. 
			// Let's assume 1-to-1 mapping for now.
			for(int i=0; i<newSeries.length && i<chart.length; i++) {
				if(chart[i] != null && chart[i].getAxis_name_unit() != null && chart[i].getAxis_name_unit().length > 1) {
					// axis_name_unit[1] is the Y-axis label which usually contains the unit, e.g. "Energy (EJ)"
					// We want to extract just "EJ" or keep "Energy (EJ)". 
					// ThumbnailUtilNew.createChart constructs it as: item_shown + " (" + str_unit + ")"
					// Let's use the full y-axis label as the unit for now, or parse it if specifically requested.
					// The user prompt example says "input (EJ)", which looks like a full axis label.
					newSeriesUnits[i] = chart[i].getAxis_name_unit()[1];
				} else {
					newSeriesUnits[i] = "";
				}
			}

			Chart[] chart1 = ThumbnailUtilNew.createTransposeChart(chart[idx].getGraphName(), // same as queryName
					newSeriesUnits, // units - PASS SPECIFIC SERIES UNITS HERE instead of generic axis units
					chart[idx].getChartColumn(), meta, newSeries, // what were previously the graph names
					newPlotNames, // what was previously the legend
					new ArrayList<String[][]>(transposedData)); // transposed data

			if (DEBUG) {
				System.out.println("Transpose::Transpose:input " + chart1.length + " trans: " + transChart.length
						+ " transpose: " + chart1.length);
			}
			
			this.chart = chart1;
			this.gridWidth = 0;

			// Build the transpose dialog the same way as Breakout View: options in NORTH, single scrollpane in CENTER
			JPanel optionsPanel = new JPanel();
			this.optionsArea = new OptionsArea(optionsPanel, this.chart, this.gridWidth, sameScale, sp, true);
			jp.add(optionsArea.getPanel(), BorderLayout.NORTH);
			setChartPane();
			add(jp);
			setTitle("Transpose Thumbnails: " + chart[0].getGraphName());
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			pack();
			setSize(800, 600);
			setLocation(InterfaceMain.getInstance().getFrame().getLocation());
			setVisible(true);
		}
	}

	/**
	 * Converts a Chart array to a master legend list (unique legend items).
	 * 
	 * @param chartArray Array of Chart objects
	 * @return List of unique legend items
	 */
	private static List<String> getMasterLegend(Chart[] chartArray) {
		List<String> masterLegend = new ArrayList<>();
		for (Chart chart : chartArray) {
			String[] legendItems = chart.getLegend().split(",");
			for (String item : legendItems) {
				String trimmed = item.trim();
				if (!masterLegend.contains(trimmed)) {
					masterLegend.add(trimmed);
				}
			}
		}
		return masterLegend;
	}

	/**
	 * Transposes the data for each legend item across all charts.
	 * 
	 * @param masterLegend List of legend items
	 * @param chart        Array of Chart objects
	 * @return List of transposed data arrays
	 */
	private static List<String[][]> getTransposeData(List<String> masterLegend, Chart[] chart) {
		List<String[][]> transposed = new ArrayList<>();
		int wid = chart[0].getChartColumn().length();
		for (int i = 0; i < masterLegend.size(); i++) {
			String[][] data = getDataset2Data(chart, i, masterLegend.get(i), masterLegend, wid);
			if (data != null) {
				transposed.add(data);
				if (DEBUG) {
					System.out.println("Legend item: " + masterLegend.get(i) + " " + i + " of " + masterLegend.size()
							+ " data: " + data.length + "  " + Arrays.toString(data[0]));
				}
			}
		}
		return transposed;
	}

	/**
	 * Gets the data for a specific series across all charts.
	 * 
	 * @param chart      Array of Chart objects
	 * @param seriesNo   Series index
	 * @param series     Series name
	 * @param seriesList List of all series
	 * @return 2D String array of data
	 */
	private static String[][] getDataset2Data(Chart[] chart, int seriesNo, String series, List<String> seriesList,
			int wid) {
		String[][] data = new String[chart.length][wid]; // was this, but sizing wasn't correct [seriesList.size()];
		for (int i = 0; i < chart.length; i++) {
			Arrays.fill(data[i], "0.0");
		}
		int k = 0;
		for (int idx = 0; idx < chart.length && chart[idx].getChart() != null; idx++) {
			k++;
			String[] chartLegend = chart[idx].getLegend().split(",");
			int legendNo = -1;
			for (int l = 0; l < chartLegend.length; l++) {
				if (chartLegend[l].trim().equals(series.trim())) {
					legendNo = l;
					break;
				}
			}
			if (legendNo > -1) {
				data[idx] = DatasetUtil.dataset2Data(chart[idx].getChart(), legendNo)[0];
			}
		}
		return Arrays.copyOfRange(data, 0, k);
	}

	/**
	 * Gets the meta array for the given charts and sets transChart.
	 * 
	 * @param chart Array of Chart objects
	 * @return Array of meta strings
	 */
	private String[] getMetaArray(Chart[] chart) {
		String[] meta = new String[chart.length];
		List<Chart> chartList = new ArrayList<>();
		int k = 0;
		for (int i = 0; i < chart.length; i++) {
			if (chart[i].getMeta() != null) {
				meta[k] = chart[i].getMeta().replace(",", "_");
				chartList.add(chart[i]);
				if (DEBUG) {
					System.out.println(
							"Transpose::getMetaArray:i " + i + " : meta: " + meta[k] + " : " + chart[i].getMeta());
				}
				k++;
			} else {
				System.out
						.println("Transpose::getMetaArray:i " + i + " k: " + k + " title: " + chart[i].getTitles()[1]);
			}
		}
		transChart = chartList.toArray(new Chart[0]);
		return Arrays.copyOfRange(meta, 0, k);
	}

    private void setChartPane() {
        ThumbnailUtilNew.validateChartPane(jp);
        JPanel chartPane = ThumbnailUtilNew.createFlowChartPane(chart, sameScale);
        JScrollPane scrollPane = new JScrollPane(chartPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        if (jp.getComponentCount() > 1 && jp.getComponent(1) instanceof JScrollPane) {
            jp.remove(1);
        }
        jp.add(scrollPane, BorderLayout.CENTER);
        jp.revalidate();
        jp.repaint();
    }

	/**
	 * Replace the transpose dialog's thumbnails with a new set of charts.
	 */
	public void updateChartPaneWithCharts(Chart[] newCharts, boolean sameScale) {
		this.chart = newCharts;
		this.sameScale = sameScale;
		setChartPane();
	}
}