package graphDisplay;

// Utility imports for chart creation and display
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import chart.CategoryChart;
import chart.Chart;
import chart.DatasetUtil;
import chart.MyChartFactory;
import chart.XYChart;
import conversionUtil.ArrayConversion;
import listener.IconMouseListener;

/**
 * Utility class for creating and displaying thumbnail charts in a grid layout.
 * Provides methods for chart creation, pane setup, and chart display options.
 * Handles both legacy and enhanced unit handling for chart data.
 */
public class ThumbnailUtilNew {

	private static final Logger LOGGER = Logger.getLogger(ThumbnailUtilNew.class.getName());
	private static final int DEFAULT_THUMBNAIL_SIZE = 240; // 320 * 0.75
	private static final int MIN_THUMBNAIL_SIZE = 135; // 180 * 0.75
	private static final int MAX_THUMBNAIL_SIZE = 240; // 320 * 0.75
	private static final int DEFAULT_GRID_WIDTH = 2;
	private static final int DEFAULT_PANEL_MIN_WIDTH = 330;
	private static final Color DEFAULT_PANEL_BG_COLOR = Color.GREEN;
	private static final int SCROLL_BAR_WIDTH = 65;
	private static final Font THUMBNAIL_TITLE_FONT = new Font("Arial", Font.BOLD, 12);
	private static final Font THUMBNAIL_SUBTITLE_FONT = new Font("Arial", Font.BOLD, 11);
	private static final String CATEGORY_LINE_CHART = "chart.CategoryLineChart";
	private static final String XY_CHART = "XY";
	private static final String CATEGORY_CHART = "Category";
	private static final String LEGEND_TITLE_CLASS = "org.jfree.chart.title.LegendTitle";
	private static final String NO_DATA_HTML_STYLE = "<style type='text/css'> p{font-family: Verdana;font-size:10;font-weight: plan;}</style>";

	// --- Chart Creation ---
	/**
	 * Creates an array of Chart objects from table data and metadata, supporting
	 * units and legend. Handles both legacy and enhanced unit handling for chart
	 * data.
	 * 
	 * @param chartName  Name of the chart type
	 * @param unit       Array of units for axes
	 * @param col_units  Array of units for columns
	 * @param column     Column names
	 * @param tableData  Table data for chart
	 * @param metaMap    Map of metadata keys to row indices
	 * @param legendG    Legend entries
	 * @param path       Path for chart resources
	 * @param metaCol    Metadata column name
	 * @param unitLookup Lookup for units
	 * @return Array of Chart objects
	 */
	public static Chart[] createChart(String chartName, String[] unit, String[] col_units, String column,
			String[][] tableData, Map<String, Integer[]> metaMap, String[] legendG, String path, String metaCol,
			HashMap<String, String> unitLookup) {
		// Extract keys from metadata map
		String[] keys = metaMap.keySet().toArray(new String[0]);
		ArrayList<Chart> chartL = new ArrayList<>();
		String[] my_unit = new String[2];
		my_unit[0] = unit[0];
		// Ignore 'year' as a unit for axis
		if (my_unit[0] != null && my_unit[0].toLowerCase().equals("year"))
			my_unit[0] = null;
		// Extract item shown from unit string
		String item_shown = (unit.length > 1 && unit[1].contains("(")) ? unit[1].substring(0, unit[1].indexOf("(") - 1)
				: "";

		for (int i = 0; i < keys.length; i++) {
			try {
				String key = keys[i];
				Integer[] range = metaMap.get(key);
				if (range != null) {
					// Copy relevant rows from table data
					String[][] temp = copyArrayRange(tableData, range);
					// Validate and sanitize numeric data
					for (int row = 0; row < temp.length; row++) {
						for (int colu = 1; colu < temp[row].length; colu++) {
							try {
								Double.parseDouble(temp[row][colu]);
							} catch (NumberFormatException e) {
								temp[row][colu] = "0.0";
							}
						}
					}
					// Prepare data for chart
					String[][] data = new String[temp.length][temp[0].length - 1];
					String[] l = new String[temp.length];
					// Determine unit for columns
					String str_unit = (col_units != null && col_units.length > 0) ? col_units[range[0]] : "";
					if (col_units != null) {
						for (int j = 0; j < range.length; j++) {
							int t = range[j];
							if (!col_units[t].equals(str_unit)) {
								str_unit = "various";
								break;
							}
						}
					}
					// Compose unit string for chart
					my_unit[1] = item_shown + " (" + str_unit + ")";
					// Extract legend and data
					for (int k = 0; k < l.length; k++) {
						l[k] = temp[k][0].trim().replace(",", "-");
						data[k] = Arrays.copyOfRange(temp[k], 1, temp[k].length);
					}
					// Generate subtitle for chart
					String stitle = getSubTitle(keys, keys[i], metaCol);
					// Create chart using factory
					Chart tempC = null;
					try {
						tempC = MyChartFactory.createChart(CATEGORY_LINE_CHART, path, chartName,
								keys[i] + "|" + metaCol, new String[] { chartName, stitle }, my_unit,
								ArrayConversion.array2String(l), column, null, data, -1);
						if (unitLookup != null) {
							HashMap<String, String> normalizedLookup = new HashMap<>();
							for (String unitKey : unitLookup.keySet()) {
								if (unitKey != null) {
									normalizedLookup.put(unitKey.trim().replace(",", "-"), unitLookup.get(unitKey));
								}
							}
							tempC.setUnitsLookup(normalizedLookup);
						}
						chartL.add(tempC);
					} catch (ClassNotFoundException | NullPointerException e) {
						LOGGER.log(Level.WARNING, "Chart creation failed for key: " + key, e);
						chartL.add(new Chart(new String[] { chartName, keys[i] }));
					}
				} else {
					// Fallback for missing range
					chartL.add(new Chart(new String[] { chartName, keys[i] }));
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error in createChart for key: " + keys[i], e);
				chartL.add(new Chart(new String[] { chartName, keys[i] }));
			}
		}
		Chart[] chart = chartL.toArray(new Chart[0]);
		return chart;
	}

	/**
	 * Overload for legacy usage (no col_units/unitLookup).
	 */
	public static Chart[] createChart(String chartName, String[] unit, String column, String[][] tableData,
			Map<String, Integer[]> metaMap, String[] legendG, String path, String metaCol) {
		// Delegate to main createChart method
		return createChart(chartName, unit, null, column, tableData, metaMap, legendG, path, metaCol, null);
	}

	/**
	 * Creates transposed charts from a list of data arrays.
	 * 
	 * @param chartName Chart type name
	 * @param path      Path for chart resources
	 * @param unit      Array of units for axes
	 * @param legend    Legend string
	 * @param column    Column names
	 * @param data      List of table data arrays
	 * @param keys      Keys for each chart
	 * @return Array of transposed Chart objects
	 */
	public static Chart[] createTransposeChart(String queryName, String[] units, String column, String meta,
			String[] newSeriesNames, String[] newGraphNames, ArrayList<String[][]> transposedData) {

		newSeriesNames = removeDateFromScenarios(newSeriesNames); // removes dates from scenario names
		ArrayList<Chart> chartL = new ArrayList<>();
		for (int i = 0; i < transposedData.size(); i++) {
			// Clean series from plot if all values are zero?
			String[][] data = transposedData.get(i);
			try {
				String plotName = newGraphNames[i].trim();
				chartL.add(MyChartFactory.createTransposedChart(queryName, // query name
						plotName, // item being plotted
						newSeriesNames, // series in plot
						meta, // metadata
						column, // column names
						units, // units associated with series
						data)); // data to plot
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		return chartL.toArray(new Chart[0]);
	}

	// --- Utility Methods ---
	/**
	 * Processes legend string to remove duplicate entries and date suffixes.
	 * 
	 * @param legend Legend string
	 * @return Cleaned legend string
	 */
	protected static String[] removeDateFromScenarios(String[] seriesNames) {

		String[] orig_seriesNames = seriesNames.clone();
		boolean duplicates = false;
		// Remove date suffixes from legend entries
		for (int i = 0; i < seriesNames.length; i++) {
			if (seriesNames[i].contains("_date=")) {
				seriesNames[i] = seriesNames[i].split("_date=")[0];
			}
		}
		// Check for duplicate legend entries
		for (int i = 0; i < seriesNames.length; i++) {
			for (int j = 0; j < seriesNames.length; j++) {
				if (i != j && seriesNames[i].equals(seriesNames[j])) {
					duplicates = true;
					break;
				}
			}
		}

		String[] rtn_str_array;
		if (duplicates) {
			rtn_str_array = orig_seriesNames;
		} else {
			rtn_str_array = seriesNames;
		}
		return rtn_str_array;
	}

	/**
	 * Copies a range of rows from a table data array.
	 * 
	 * @param table_data Source table data
	 * @param rows       Indices of rows to copy
	 * @return Sub-array of table data
	 */
	private static String[][] copyArrayRange(String[][] table_data, Integer[] rows) {
		String[][] rtn_str_array = new String[rows.length][table_data[0].length];
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c < table_data[0].length; c++) {
				rtn_str_array[r][c] = table_data[rows[r]][c];
			}
		}
		return rtn_str_array;
	}

	/**
	 * Checks if a legend resource exists in the data array.
	 * 
	 * @param data Array of legend strings
	 * @param key  Key to search for
	 * @return Index of key in data array, or -1 if not found
	 */
	protected static int legendResourceExist(String[] data, String key) {
		return Arrays.asList(data).indexOf(key);
	}

	/**
	 * Checks if all parts of a key exist in the qualifier data array.
	 * 
	 * @param qualifierData Array of qualifier strings
	 * @param key           Key to check
	 * @return True if all parts exist, false otherwise
	 */
	protected static boolean keyExist(String[] qualifierData, String key) {
		String[] keyPart = key.split(" ");
		for (String part : keyPart) {
			if (!Arrays.asList(qualifierData).contains(part.trim())) {
				return false;
			}
		}
		return true;
	}

	// --- Region Chart ---
	/**
	 * Creates region charts from data and metadata.
	 * 
	 * @param chartClassName Chart class name
	 * @param path           Path for chart resources
	 * @param chartName      Chart name
	 * @param meta           Metadata string
	 * @param title          Chart title
	 * @param unit           Array of units
	 * @param legend         Legend string
	 * @param column         Column names
	 * @param data           Table data
	 * @return Array of region Chart objects
	 */
	public static Chart[] createChart(String chartClassName, String path, String chartName, String meta, String title,
			String[] unit, String legend, String column, String[][] data) {
		int d = legend.split(",").length;
		int n = data.length / d;
		Chart[] chart = new Chart[n];
		String[] metaStr = meta.split(";");
		try {
			for (int i = 0; i < chart.length; i++)
				chart[i] = MyChartFactory.createChart(chartClassName, path, chartName, metaStr[i], title.split("\\|"),
						unit, legend, column, null, Arrays.copyOfRange(data, i * d, (i + 1) * d), -1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return chart;
	}

	// --- Option Pane Chart Creation ---
	/**
	 * Creates charts for option pane display, supporting relative index and chart
	 * type.
	 * 
	 * @param cn            Chart class name
	 * @param relativeIndex Relative index for chart
	 * @param chart         Array of Chart objects
	 * @param chartType     Chart type string
	 * @return Array of Chart objects for option pane
	 */
	public static Chart[] createChart(String cn, int relativeIndex, Chart[] chart, String chartType) {
		Chart[] chart1 = new Chart[chart.length];
		for (int i = 0; i < chart.length; i++) {
			String[] axis = chart[i].getAxis_name_unit().clone();
			if (relativeIndex > -1)
				axis[1] = "Relative (" + axis[1] + ")";
			try {
				if (cn.contains("Category")) {
					DefaultCategoryDataset dataset = null;
					// Convert XY dataset to Category dataset if needed
					if (chart[i].getChartClassName().contains("XY"))
						dataset = DatasetUtil.XYDataset2CategoryDataset(((XYChart) chart[i]).getDataset());
					else if (chart[i].getChartClassName().contains("Category"))
						dataset = ((CategoryChart) chart[i]).getDataset();
					chart1[i] = MyChartFactory.createChart(cn, chart[i].getPath(), chart[i].getGraphName(),
							chart[i].getMeta() + "|" + chart[i].getMetaCol(), chart[i].getTitles(), axis,
							chart[i].getLegend(), chart[i].getColor(), chart[i].getpColor(), chart[i].getPattern(),
							chart[i].getLineStrokes(), chart[i].getAnnotationText(), dataset, relativeIndex,
							chart[i].isShowLineAndShape(), chartType);
				} else {
					DefaultXYDataset dataset = null;
					// Convert Category dataset to XY dataset if needed
					if (chart[i].getChartClassName().contains("Category"))
						dataset = DatasetUtil.CategoryDataset2XYDataset(((CategoryChart) chart[i]).getDataset());
					else if (chart[i].getChartClassName().contains("XY"))
						dataset = DatasetUtil.createXYDataset(chart[i].getChart().getXYPlot().getDataset(),
								chart[i].getRelativeColIndex());
					chart1[i] = MyChartFactory.createChart(cn, chart[i].getPath(), chart[i].getGraphName(),
							chart[i].getMeta() + "|" + chart[i].getMetaCol(), chart[i].getTitles(), axis,
							chart[i].getLegend(), chart[i].getColor(), chart[i].getpColor(), chart[i].getPattern(),
							chart[i].getLineStrokes(), chart[i].getAnnotationText(), dataset, relativeIndex,
							chart[i].isShowLineAndShape());
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NullPointerException e1) {
				chart1[i] = chart[i];
			}
		}
		return chart1;
	}

	// --- Chart Pane Display ---
	/**
	 * Sets up a JPanel containing chart thumbnails in a grid layout.
	 * 
	 * @param chart     Array of Chart objects
	 * @param w         Width of each chart thumbnail
	 * @param gridWidth Number of columns in grid
	 * @param sameScale Whether to use same scale for all charts
	 * @param transpose Whether to transpose chart data
	 * @return JPanel containing chart thumbnails
	 */
	public static JPanel setChartPane(Chart[] chart, int w, int gridWidth, boolean sameScale, boolean transpose) {
		// Create grid layout for chart thumbnails
		int padding = SCROLL_BAR_WIDTH / 4;
		GridLayout gl = new GridLayout(0, gridWidth);
		gl.setHgap(0);
		gl.setVgap(0);
		JPanel chartPane = new JPanel(gl);
		chartPane.removeAll(); // defensive: ensure pane starts empty
		chartPane.setBorder(BorderFactory.createEmptyBorder(padding, 0, padding, 0));
		// Calculate max and min values for scaling
		double max = setMax(chart);
		double min = setMin(chart);
		for (int i = 0; i < chart.length; i++) {
			IconMouseListener iconListener = new IconMouseListener(chart, i);
			JButton jb = null;
			try {
				jb = buttonIcon(chart[i], chart.length - 1 - i, w, max, min, sameScale, transpose, iconListener);
			} catch (OutOfMemoryError e2) {
				JOptionPane.showMessageDialog(null, "Too many charts to be created. No enough memory ", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				chartPane.removeAll();
				Runtime.getRuntime().gc();
				return null;
			}
			jb.setMargin(new Insets(0, 0, 0, 0));
			jb.setBackground(Color.lightGray);
			jb.setName(String.valueOf(i));
			jb.setPreferredSize(new Dimension(w, w));
			chartPane.add(jb);
		}
		chartPane.setSize(w * gridWidth, w * gridWidth);
		return chartPane;
	}

	/**
	 * Creates a JButton with chart thumbnail icon and tooltip.
	 * 
	 * @param chart        Chart object
	 * @param idx          Index for button name
	 * @param w            Width of thumbnail
	 * @param max          Maximum value for scaling
	 * @param min          Minimum value for scaling
	 * @param sameScale    Whether to use same scale for all charts
	 * @param transpose    Whether to transpose chart data
	 * @param iconListener Mouse listener for icon
	 * @return JButton with chart thumbnail
	 */
	public static JButton buttonIcon(Chart chart, int idx, int w, double max, double min, boolean sameScale,
			boolean transpose, IconMouseListener iconListener) {
		JButton jb = new JButton();
		JFreeChart freeChart = null;
		boolean category = false;
		try {
			if (chart.getChartClassName().contains("Category"))
				category = true;
			freeChart = chart.getChart();
		} catch (IllegalStateException | NullPointerException e) {
			// ignore
		}
		if (freeChart != null) {
			// Set axis label positions and scaling
			if (category) {
				freeChart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
				freeChart.getCategoryPlot().getDomainAxis().setLabel(null);
			} else {
				freeChart.getXYPlot().getDomainAxis().setLabelAngle(90);
				freeChart.getXYPlot().getDomainAxis().setLabel(null);
			}
			if (sameScale) {
				if (category) {
					freeChart.getCategoryPlot().getRangeAxis().setUpperBound(max);
					freeChart.getCategoryPlot().getRangeAxis().setLowerBound(min);
				} else {
					freeChart.getXYPlot().getRangeAxis().setUpperBound(max);
					freeChart.getXYPlot().getRangeAxis().setLowerBound(min);
				}
			} else {
				if (category)
					freeChart.getCategoryPlot().getRangeAxis().setAutoRange(true);
				else
					freeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			}
			// Hide legend and title for thumbnail
			if (freeChart.getLegend() != null) {
				freeChart.getLegend().visible = false;
			}
			freeChart.getTitle().setFont(THUMBNAIL_TITLE_FONT);
			freeChart.getTitle().setVisible(false);
			// Set subtitle font and visibility
			for (int j = 0; j < freeChart.getSubtitleCount()
					&& !(freeChart.getSubtitle(j) instanceof org.jfree.chart.title.LegendTitle); j++) {
				((TextTitle) freeChart.getSubtitle(j)).setFont(THUMBNAIL_SUBTITLE_FONT);
				freeChart.getSubtitle(j).setVisible(true);
			}
			ChartUtils.applyCurrentTheme(freeChart);
			try {
				// Create thumbnail image
				int padding = SCROLL_BAR_WIDTH / 8;
				int imageSize = Math.max(1, w - (padding * 2));
				BufferedImage thumb1 = freeChart.createBufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB, null);
				ImageIcon image1 = new ImageIcon(thumb1);
				jb.setIcon(image1);
				jb.setName(String.valueOf(idx));
			} catch (IllegalStateException | NullPointerException e) {
				// ignore
			}
			jb.addMouseListener(iconListener);
			jb.setToolTipText(chart.getMeta());
		} else {
			// If chart is null, show description text
			if (!transpose)
				jb.setText(getEmptyChartDesc(chart.getTitles()));
		}
		return jb;
	}

	public static JPanel createFlowChartPane(Chart[] chart, boolean sameScale) {
		int padding = SCROLL_BAR_WIDTH / 4;
		WrappingLayout fl = new WrappingLayout(FlowLayout.LEFT);
		fl.setHgap(0);
		fl.setVgap(0);
		JPanel chartPane = new WrappingPanel(fl);
		chartPane.removeAll(); // defensive: ensure pane starts empty
		chartPane.setBorder(BorderFactory.createEmptyBorder(padding, 0, padding, 0));
		// Calculate max and min values for scaling
		double max = setMax(chart);
		double min = setMin(chart);
		for (int i = 0; i < chart.length; i++) {
			IconMouseListener iconListener = new IconMouseListener(chart, i);
			JButton jb = null;
			try {
				jb = buttonIcon(chart[i], chart.length - 1 - i, DEFAULT_THUMBNAIL_SIZE, max, min, sameScale, false, iconListener);
			} catch (OutOfMemoryError e2) {
				JOptionPane.showMessageDialog(null, "Too many charts to be created. No enough memory ", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				chartPane.removeAll();
				Runtime.getRuntime().gc();
				return null;
			}
			jb.setMargin(new Insets(0, 0, 0, 0));
			jb.setBackground(Color.lightGray);
			jb.setName(String.valueOf(i));
			chartPane.add(jb);
		}
		return chartPane;
	}

	/**
	 * Returns a description for an empty chart.
	 * 
	 * @param titles Chart titles
	 * @return HTML description string
	 */
	public static String getEmptyChartDesc(String[] titles) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>").append(NO_DATA_HTML_STYLE).append("<p>No Data For: </p><p>");
		sb.append(Optional.ofNullable(titles).filter(arr -> arr.length > 0).map(arr -> arr[0]).orElse("N/A"));
		sb.append("</p><p>");
		sb.append(Optional.ofNullable(titles).filter(arr -> arr.length > 1).map(arr -> arr[1]).orElse(""));
		sb.append("</p></html>");
		return sb.toString();
	}

	/**
	 * Sets up a chart pane for display in a split pane.
	 * 
	 * @param chart           Array of Chart objects
	 * @param firstNonNullidx Index of first non-null chart
	 * @param sameScale       Whether to use same scale for all charts
	 * @param transpose       Whether to transpose chart data
	 * @param sp              JSplitPane for display
	 * @return JPanel containing chart pane
	 */
	public static JPanel setChartPane(Chart[] chart, int firstNonNullidx, boolean sameScale, boolean transpose,
			JSplitPane sp) {
		int gridWidth = chart.length > 1 ? DEFAULT_GRID_WIDTH : 1;
		int w = computeFixGridLayoutViewSize(sp.getSize().width, gridWidth);
		JPanel chartPane = setChartPane(chart, w, gridWidth, sameScale, transpose);
		JPanel jp = null;
		if (chartPane != null) {
			jp = new JPanel(new BorderLayout());
			jp.setMinimumSize(new Dimension(DEFAULT_PANEL_MIN_WIDTH, 100));
			jp.setBackground(DEFAULT_PANEL_BG_COLOR);
			jp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jp.setName(chart[firstNonNullidx].getGraphName());
			jp.putClientProperty("isGraph", true);
			new OptionsArea(jp, chart, gridWidth, false, sp);

			JScrollPane scrollPane = new JScrollPane(chartPane);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.getVerticalScrollBar().setUnitIncrement(20);
			jp.add(scrollPane, BorderLayout.CENTER);
			
			final int finalW = w;
			final int finalGridWidth = gridWidth;
			final JSplitPane finalSp = sp; // Make sp final for lambda

			// Check current state before the component is likely replaced by the caller
			Component currentRight = finalSp.getRightComponent();
			boolean isExistingGraph = false;
			if (currentRight instanceof JComponent) {
				Boolean prop = (Boolean) ((JComponent) currentRight).getClientProperty("isGraph");
				isExistingGraph = prop != null && prop;
			}
			
			boolean shouldResize = !isExistingGraph;
			if (isExistingGraph) {
				int currentRightWidth = currentRight.getWidth();
				if (currentRightWidth < SCROLL_BAR_WIDTH || finalSp.getDividerLocation() < 0) {
					shouldResize = true;
				}
			}

			final boolean finalShouldResize = shouldResize;

			SwingUtilities.invokeLater(() -> {
				if (finalSp.getWidth() > 0 && finalShouldResize) {
					int paneWidth = (finalW * finalGridWidth) + SCROLL_BAR_WIDTH;
					finalSp.setDividerLocation(finalSp.getWidth() - paneWidth);
				}
			});
			
			jp.updateUI();
		}
		return jp;
	}

	/**
	 * Validates and updates the chart pane UI.
	 * 
	 * @param jp JPanel to validate
	 */
	public static void validateChartPane(JPanel jp) {
		if (jp.getLayout() instanceof BorderLayout) {
			BorderLayout bl = (BorderLayout) jp.getLayout();
			Component center = bl.getLayoutComponent(BorderLayout.CENTER);
			if (center != null) {
				jp.remove(center);
			}
		}
	}

	// --- Subtitle ---
	/**
	 * Generates a subtitle for a chart based on keys and metadata column.
	 * 
	 * @param keys      Array of key strings
	 * @param keyString Key string for chart
	 * @param mCol      Metadata column name
	 * @return Subtitle string
	 */
	private static String getSubTitle(String[] keys, String keyString, String mCol) {
		String rtn_str="";
		keyString = keyString.replace(",depth=1", "");
		if (keyString.contains("minus")) {
			rtn_str=keyString;
		} else {
		String temp = keyString.split(",")[0];
		int ks = temp.split(" ").length;
		String scen = temp.split(" ")[ks - 1];
		String region = "";
		if (ks >= 2)
			region = temp.split(" ")[ks - 2];
		String sect = "";
		int num = temp.indexOf(region);
		if (num > 0) {
			sect = temp.substring(0, num - 1).trim();
			scen += "\n" + sect;
		}
		String dateStr = "";
		if (keyString.contains(",")) {
			dateStr = keyString.split(",")[1];
		}
		int dateIndex = 0;
		if (!dateStr.isEmpty()) {
			for (String s : Arrays.asList(keys)) {
				s = s.replace(",depth=1", "");
				if (s.contains(temp))
					if (!s.contains(dateStr))
						dateIndex++;
					else
						break;
			}
		}
		rtn_str = scen;
		if (dateIndex > 0)
			rtn_str += dateIndex;
		if (region.length() > 0)
			rtn_str += "\nregion: " + region;
		}
		return rtn_str;
	}

	// --- Range Calculation ---
	/**
	 * Calculates the maximum value across all charts.
	 * 
	 * @param chart Array of Chart objects
	 * @return Maximum value
	 */
	public static double setMax(Chart[] chart) {
		double max = 0;
		for (int i = 0; i < chart.length && chart[i] != null; i++) {
			String s = chart[i].getChartClassName();
			if (s != null)
				if (s.contains("XY"))
					max = Math.max(chart[i].getChart().getXYPlot().getRangeAxis().getUpperBound(), max);
				else
					max = Math.max(chart[i].getChart().getCategoryPlot().getRangeAxis().getUpperBound(), max);
		}
		return max;
	}

	/**
	 * Calculates the minimum value across all charts.
	 * 
	 * @param chart Array of Chart objects
	 * @return Minimum value
	 */
	public static double setMin(Chart[] chart) {
		double min = 0;
		for (int i = 0; i < chart.length && chart[i] != null; i++) {
			String s = chart[i].getChartClassName();
			if (s != null)
				if (s.contains("XY"))
					min = Math.min(chart[i].getChart().getXYPlot().getRangeAxis().getLowerBound(), min);
				else
					min = Math.min(chart[i].getChart().getCategoryPlot().getRangeAxis().getLowerBound(), min);
		}
		return min;
	}

	/**
	 * Gets the preferred chart dimensions based on legend length.
	 * 
	 * @param jfreechart JFreeChart object
	 * @return Preferred Dimension for chart
	 */
	@SuppressWarnings("rawtypes")
	public static Dimension getChartDimensions(JFreeChart jfreechart) {
		int i = 0;
		for (Iterator iterator = jfreechart.getPlot().getLegendItems().iterator(); iterator.hasNext();) // Sum legend
																										// label lengths
			i += ((LegendItem) iterator.next()).getLabel().length();
		if (i <= 500)
			return new Dimension(350, 350);
		else
			return new Dimension(350, 350 + (i - 500) / 2);
	}

	/**
	 * Computes fixed grid layout view size for chart thumbnails.
	 * 
	 * @param x         Width of parent container
	 * @param gridWidth Number of columns in grid
	 * @return Computed width for thumbnails
	 */
	public static int computeFixGridLayoutViewSize(int x, int gridWidth) {
		int w;
		if (x > 0 && gridWidth > 1) {
			w = (x / gridWidth / 2) - 20;
			w = Math.max(MIN_THUMBNAIL_SIZE, Math.min(w, MAX_THUMBNAIL_SIZE));
		} else
			w = DEFAULT_THUMBNAIL_SIZE;
		return w;
	}

	/**
	 * Computes grid layout view size for chart thumbnails.
	 * 
	 * @param x         Width of parent container
	 * @param gridWidth Number of columns in grid
	 * @return Computed width for thumbnails
	 */
	public static int computeGridLayoutViewSize(int x, int gridWidth) {
		int w;
		if (gridWidth < 2)
			w = (int) (x * 0.25 - 20);
		else
			w = (int) ((x * 0.4 - 20) / 2);
		return w;
	}

	/**
	 * Returns the index of the first non-null chart in the array.
	 * 
	 * @param chart Array of Chart objects
	 * @return Index of first non-null chart, or -1 if none found
	 */
	public static int getFirstNonNullChart(Chart[] chart) {
		for (int i = 0; i < chart.length; i++) {
			if (chart[i] != null && chart[i].getChart() != null) {
				return i;
			}
		}
		return -1;
	}
}
