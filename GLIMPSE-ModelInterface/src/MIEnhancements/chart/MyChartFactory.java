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

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

/**
 * Factory class for creating Chart instances with various data and
 * configuration options.
 * <p>
 * Author: TWU Date: 1/2/2016
 */
public class MyChartFactory {
	/**
	 * Debug flag for logging chart creation details.
	 */
	private static boolean debug = false;

	/**
	 * Creates a Chart using a DefaultCategoryDataset and various chart properties.
	 *
	 * @param className        the chart class name
	 * @param path             the file path for chart resources
	 * @param graphName        the name of the graph
	 * @param meta             metadata for the chart
	 * @param titles           array of chart titles
	 * @param axis_name_unit   array of axis names and units
	 * @param legend           legend text
	 * @param color            array of colors
	 * @param pColor           array of pattern colors
	 * @param pattern          array of patterns
	 * @param lineStrokes      array of line stroke styles
	 * @param annotationText   annotation text for the chart
	 * @param dataset          the category dataset
	 * @param relativeColIndex relative column index
	 * @param ShowLineAndShape flag to show line and shape
	 * @param graphType        type of graph
	 * @return a Chart instance
	 * @throws ClassNotFoundException if the chart class cannot be found
	 */
	public static Chart createChart(String className, String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, int[] color, int[] pColor, int[] pattern, int[] lineStrokes,
			String[][] annotationText, DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape,
			String graphType) throws ClassNotFoundException {
		// Ensure titles[0] is not null if titles has at least two elements
		if (titles.length >= 2 && titles[0] == null) {
			titles[0] = titles[1];
			titles[1] = "";
		}
		// Prepare constructor arguments
		Object[] o = { path, graphName, meta, titles, axis_name_unit, legend, color, pColor, pattern, lineStrokes,
				annotationText, dataset, relativeColIndex, ShowLineAndShape, graphType };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart1:className: " + t.getName());
		Chart chart = (Chart) ChartUtil.creatNewInstance(t, o);
		System.runFinalization();
		return chart;
	}

	/**
	 * Creates a Chart using a DefaultXYDataset and various chart properties.
	 *
	 * @param className        the chart class name
	 * @param path             the file path for chart resources
	 * @param graphName        the name of the graph
	 * @param meta             metadata for the chart
	 * @param titles           array of chart titles
	 * @param axis_name_unit   array of axis names and units
	 * @param legend           legend text
	 * @param color            array of colors
	 * @param pColor           array of pattern colors
	 * @param pattern          array of patterns
	 * @param lineStrokes      array of line stroke styles
	 * @param annotationText   annotation text for the chart
	 * @param dataset          the XY dataset
	 * @param relativeColIndex relative column index
	 * @param ShowLineAndShape flag to show line and shape
	 * @return a Chart instance
	 * @throws ClassNotFoundException if the chart class cannot be found
	 */
	public static Chart createChart(String className, String path, String graphName, String meta, String[] titles,
			String[] axis_name_unit, String legend, int[] color, int[] pColor, int[] pattern, int[] lineStrokes,
			String[][] annotationText, DefaultXYDataset dataset, int relativeColIndex, boolean ShowLineAndShape)
			throws ClassNotFoundException {
		// Prepare constructor arguments
		Object[] o = { path, graphName, meta, titles, axis_name_unit, legend, color, pColor, pattern, lineStrokes,
				annotationText, dataset, relativeColIndex, ShowLineAndShape };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart2:className: " + t.getName());
		Chart chart = (Chart) ChartUtil.creatNewInstance(t, o);
		return chart;
	}

	/**
	 * Creates a Chart for a single dataset, called from graphDisplayUtil.
	 *
	 * @param className        the chart class name
	 * @param path             the file path for chart resources (may be null for
	 *                         transpose charts)
	 * @param graphName        the name of the graph
	 * @param id               chart identifier
	 * @param titles           array of chart titles
	 * @param axisName_unit    array of axis names and units
	 * @param legend           legend text
	 * @param column           column name
	 * @param annotationText   annotation text for the chart
	 * @param data             chart data
	 * @param relativeColIndex relative column index
	 * @return a Chart instance
	 * @throws ClassNotFoundException if the chart class cannot be found
	 */
	public static Chart createChart(String className, String path, String graphName, String id, String[] titles,
			String[] axisName_unit, String legend, String column, String[][] annotationText, String[][] data,
			int relativeColIndex) throws ClassNotFoundException {
		// Prepare constructor arguments
		Object[] o = { path, graphName, id.trim(), titles, axisName_unit, legend, column, annotationText, data,
				Integer.valueOf(relativeColIndex) };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart3:className: " + t.getName());
		return (Chart) ChartUtil.creatNewInstance(t, o);
	}

	/**
	 * Creates a Chart for Box and Whisker plots with a single dataset.
	 *
	 * @param className     the chart class name
	 * @param path          the file path for chart resources
	 * @param graphName     the name of the graph
	 * @param id            chart identifier
	 * @param titles        array of chart titles
	 * @param axisName_unit array of axis names and units
	 * @param column        column name
	 * @param annotation    annotation text for the chart
	 * @param data          chart data as a nested list
	 * @return a Chart instance
	 * @throws ClassNotFoundException if the chart class cannot be found
	 */
	public static Chart createChart(String className, String path, String graphName, String id, String[] titles,
			String[] axisName_unit, String column, String[][] annotation, ArrayList<List<String[]>> data)
			throws ClassNotFoundException {
		// Prepare constructor arguments
		Object[] o = { path, graphName, id, titles, axisName_unit, column, annotation, data };
		Class<?> t = Class.forName(className);
		if (debug)
			System.out.println("ChartFactory::createChart4:className: " + t.getName());
		return (Chart) ChartUtil.creatNewInstance(t, o);
	}

	/**
	 * Creates a transposed Chart using the provided data and configuration.
	 * <p>
	 * This method is typically used to generate a chart where the data has been
	 * transposed, such as switching rows and columns for display purposes. It
	 * constructs a chart instance using reflection and passes the required
	 * parameters to the chart constructor.
	 *
	 * @param categoryLineChart the chart class name to instantiate
	 * @param nullPath          the file path for chart resources (can be null)
	 * @param chartName         the name of the chart
	 * @param newChartNames     the new chart names (series)
	 * @param titles            array of chart titles
	 * @param unit              array of axis units
	 * @param newSeriesNames    array of new series names
	 * @param column            column name
	 * @param nullString        unused parameter (reserved for future use)
	 * @param transposedData    transposed chart data
	 * @param val               relative column index or value (unused)
	 * @return a Chart instance created with the transposed data
	 */
	public static Chart createTransposedChart(String queryName, String chartName, String[] newSeriesNames, String meta,
			String column, String[] units, String[][] transposedData) {
		// TODO Auto-generated method stub
		String newSeriesString = String.join(",", newSeriesNames);
		String[] axisUnits = new String[] { null, resolveAggregateUnitLabel(units) };

		String[] titles = new String[2];
		titles[0] = queryName;
		titles[1] = chartName;

		Chart chart = (Chart) new CategoryLineChart((String) null, // no path needed for transpose chart
				chartName, // previously was series, now its own graph
				meta, // meta not currently used? Not sure this is needed, but doesn't like null
				titles, // titles need to pass or construct this
				axisUnits, // axis labels [x, y]
				newSeriesString, // list of new series for transposed chart, used as legend
				column, // column not used in transpose chart
				(String[][]) null, // no annotations for transpose chart
				transposedData, // transposed data
				-1); // relativeColIndex not used in transpose chart
		if (units != null && units.length > 0) {
			java.util.HashMap<String, String> unitLookup = new java.util.HashMap<>();
			// In a transposed chart, the 'series' are what was originally the column headers (or similar)
			// 'units' passed here corresponds to the units for the new series.
			// The length of 'units' should match 'newSeriesNames'.
			
			for (int i = 0; i < newSeriesNames.length; i++) {
				if (i < units.length) {
					// Normalize key just like in ThumbnailUtilNew
					unitLookup.put(newSeriesNames[i].trim().replace(",", "-"), normalizeUnitToken(units[i]));
				}
			}
			chart.setUnitsLookup(unitLookup);
		}
		return chart;
	}

	private static String resolveAggregateUnitLabel(String[] units) {
		if (units == null || units.length == 0) {
			return "";
		}
		String chosen = null;
		for (String unit : units) {
			String normalized = normalizeUnitToken(unit);
			if (normalized.isEmpty()) {
				continue;
			}
			if (chosen == null) {
				chosen = normalized;
			} else if (!chosen.equalsIgnoreCase(normalized)) {
				return "various";
			}
		}
		return chosen == null ? "" : chosen;
	}

	private static String normalizeUnitToken(String rawUnit) {
		if (rawUnit == null) {
			return "";
		}
		String unit = rawUnit.trim();
		if (unit.isEmpty()) {
			return "";
		}
		int openParen = unit.lastIndexOf('(');
		int closeParen = unit.endsWith(")") ? unit.length() - 1 : -1;
		if (openParen >= 0 && closeParen > openParen) {
			String token = unit.substring(openParen + 1, closeParen).trim();
			if (!token.isEmpty()) {
				unit = token;
			}
		}
		if ("none".equalsIgnoreCase(unit)) {
			return "unitless";
		}
		if ("none specified".equalsIgnoreCase(unit)) {
			return "None specified";
		}
		return unit;
	}
}