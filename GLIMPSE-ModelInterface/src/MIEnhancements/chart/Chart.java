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
package chart;

import java.awt.Color;
import java.awt.TexturePaint;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;

import chartOptions.FileUtil;
import listener.ChartTitleChangeListener;

/**
 * The base class for all JFreeChart. Subclasses are divided into Category and
 * XY JFreeChart. It holds meta data (selection criteria), legend properties,
 * and basic chart information.
 * 
 * Author		Action				Date		Flag
 * =======================================================================
 * TWU		created				1/2/2016	
 */
public class Chart {
	// Basic chart information
	protected JFreeChart chart;
	protected String chartClassName;
	protected String[] axis_name_unit;
	protected String graphName;
	protected String[] titles;
	protected String chartColumn;
	protected String chartRow;
	// Meta data
	protected String meta;
	protected String metaCol;
	// Legend information
	private String path;
	protected String legend;
	protected TexturePaint[] paint;
	protected int[] color;
	protected int[] pColor;
	protected int[] pattern;
	protected int[] lineStrokes;
	// Function applied
	protected int relativeColIndex;
	protected Map<String, Marker> markerMap = new HashMap<>();
	protected String[][] annotationText;
	protected boolean showLineAndShape;
	protected boolean debug = false;

	public LegendTitle myLegend = null;
	private HashMap<String, String> unitsLookup = null;

	/**
	 * Constructor for empty chart with description.
	 * @param titles Chart titles
	 */
	public Chart(String[] titles) {
		this.titles = titles.clone();
	}

	/**
	 * Set units lookup table.
	 * @param unitsLookup HashMap of units
	 */
	public void setUnitsLookup(HashMap<String, String> unitsLookup) {
		this.unitsLookup = unitsLookup;
	}

	/**
	 * Get units lookup table.
	 * @return HashMap of units
	 */
	public HashMap<String, String> getUnitsLookup() {
		return this.unitsLookup;
	}

	/**
	 * Main chart constructor with all properties.
	 */
	public Chart(String path, String graphName, String meta, String[] titles, String[] axisName_unit, String legend,
			int[] color, int[] pColor, int[] pattern, int[] lineStrokes, String[][] annotationText,
			boolean ShowLineAndShape) {
		this.legend = legend;
		this.color = color == null ? null : color.clone();
		initLegendPattern(pColor, pattern, lineStrokes);
		this.showLineAndShape = ShowLineAndShape;
		init(path, graphName, meta, titles, axisName_unit, annotationText);
	}

	/**
	 * Chart constructor for charts without color/pattern info.
	 */
	public Chart(String path, String graphName, String meta, String[] titles, String[] axisName_unit, String legend,
			String[][] annotationText) {
		this.legend = legend;
		init(path, graphName, meta, titles, axisName_unit, annotationText);
	}

	/**
	 * Initialize legend pattern arrays.
	 */
	protected void initLegendPattern(int[] pColor, int[] pattern, int[] lineStrokes) {
		this.pColor = pColor == null ? this.color.clone() : pColor.clone();
		this.pattern = pattern == null ? null : pattern.clone();
		this.lineStrokes = lineStrokes == null ? null : lineStrokes.clone();

		if (pattern == null) {
			this.pattern = new int[this.pColor.length];
			for (int i = 0; i < this.pattern.length; i++)
				this.pattern[i] = 0;
		}

		if (lineStrokes == null) {
			this.lineStrokes = new int[this.pColor.length];
			for (int i = 0; i < this.lineStrokes.length; i++)
				this.lineStrokes[i] = 5;
		}
	}

	/**
	 * Initialize chart properties.
	 */
	private void init(String path, String graphName, String meta, String[] titles, String[] axisName_unit,
			String[][] annotationText) {
		this.path = path;
		try {
			if (meta.contains("|")) {
				this.meta = meta.split("\\|")[0];
				this.metaCol = meta.split("\\|")[1];
			} else {
				this.meta = meta;
			}
		} catch (Exception e) {
			System.out.println("error processing meta: " + meta + " in init in Chart.java");
		}
		if (debug)
			System.out.println("Chart::init:meta: " + this.meta + " col: " + metaCol + "  " + meta);
		this.graphName = graphName;
		this.titles = titles.clone();
		this.axis_name_unit = (axisName_unit == null ? null : axisName_unit.clone());
		this.annotationText = annotationText == null ? null : annotationText.clone();
		if (debug)
			System.out.println("Chart::init:graphName: " + graphName + " legend: " + legend);
	}

	/**
	 * Get legend information from a property file, then store in Chart object.
	 * @param legends legend label of a JFreeChart (not null)
	 */
	protected void getlegendInfo(String[] legends) {
		if (path == null)
			return;
		Object[] temp = legendInfoFromProperties(path);
		if (temp.length == 0)
			return;
		String[] tempStr = readLegendItemsFromProperties();
		String[] queryStr = readQueryInfoFromProperties();
		String queryNameForChart = "\"" + (chart.getTitle().getText() != null && !chart.getTitle().getText().isEmpty() ? chart.getTitle().getText() : this.graphName) + "\"";
		for (int i = 0; i < legends.length; i++) {
			int idx = Arrays.asList(tempStr).indexOf(legends[i].trim());
			int idx_last = Arrays.asList(tempStr).lastIndexOf(legends[i].trim());
			String[] o = new String[4];
			if (idx > -1 && idx == idx_last) {
				String queryNameInLine = Arrays.asList(queryStr).get(idx);
				if (queryNameInLine.equals(queryNameForChart)) {
					o = ((String) temp[idx]).split("=")[1].split(",");
					color[i] = Integer.valueOf(o[0].trim());
					pColor[i] = Integer.valueOf(o[1].trim());
					pattern[i] = Integer.valueOf(o[2].trim());
					lineStrokes[i] = Integer.valueOf(o[3].trim());
					if (debug)
						System.out.println("Chart::getlegendInfo:legend: " + legends[i] + " color: " + color[i]
							+ " pattern: " + pattern[i] + " lineStrokes: " + lineStrokes[i]);
				} else if (queryNameInLine.equals("*")) {
					o = ((String) temp[idx]).split("=")[1].split(",");
					color[i] = Integer.valueOf(o[0].trim());
					pColor[i] = Integer.valueOf(o[1].trim());
					pattern[i] = Integer.valueOf(o[2].trim());
					lineStrokes[i] = Integer.valueOf(o[3].trim());
				}
			} else if (idx > -1 && idx != idx_last) {
				for (int idxN = idx; idxN <= idx_last; idxN++) {
					String queryNameAtIdx = Arrays.asList(queryStr).get(idxN);
					String legendInFile = Arrays.asList(tempStr).get(idxN);
					if (queryNameAtIdx.equals(queryNameForChart) && legendInFile.equals(legends[i].trim())) {
						o = ((String) temp[idxN]).split("=")[1].split(",");
						color[i] = Integer.valueOf(o[0].trim());
						pColor[i] = Integer.valueOf(o[1].trim());
						pattern[i] = Integer.valueOf(o[2].trim());
						lineStrokes[i] = Integer.valueOf(o[3].trim());
					} else if (queryNameAtIdx.equals("*") && legendInFile.equals(legends[i].trim())) {
						o = ((String) temp[idxN]).split("=")[1].split(",");
						color[i] = Integer.valueOf(o[0].trim());
						pColor[i] = Integer.valueOf(o[1].trim());
						pattern[i] = Integer.valueOf(o[2].trim());
						lineStrokes[i] = Integer.valueOf(o[3].trim());
					}
				}
			}
		}
	}

	/**
	 * Store legend information to a property file.
	 * @param legends legend label of a JFreeChart (not null)
	 * @param color colors of each legend
	 */
	public void storelegendInfo(String[] legends, String[] color) {
		if (path == null)
			return;
		if (debug)
			System.out.println("Chart::storelegendInfo:path: " + path);
		ArrayList<String> tempstr = legendInfoFromProperties(legends, color);
		String[] writestr = tempstr.toArray(new String[0]);
		FileOutputStream fos = FileUtil.initOutFile(path);
		FileUtil.writetofile(fos, writestr);
	}

	/**
	 * Store global legend information to a property file.
	 * @param legends legend label of a JFreeChart (not null)
	 * @param color colors of each legend
	 */
	public void storelegendInfoGlobal(String[] legends, String[] color) {
		if (path == null) {
			System.out.println("Null path to legend color file");
			return;
		}
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			System.out.println("Could not read in any previous color data.");
		}
		ArrayList<Boolean> done = new ArrayList<>();
		for (int i = 0; i < legends.length; i++) {
			done.add(false);
		}
		String firstPart = null;
		String queryNameForChart = "\"" + chart.getTitle().getText() + "\"";
		for (int i = 0; i < lines.size(); i++) {
			if (!lines.get(i).startsWith("*")) {
				if (lines.get(i).contains(":")) {
					continue;
				}
			}
			if (lines.get(i).contains("*")) {
				firstPart = lines.get(i).split(":")[1];
			} else {
				firstPart = lines.get(i);
			}
			firstPart = firstPart.split("=")[0];
			for (int j = 0; j < legends.length; j++) {
				if (firstPart.compareTo(legends[j]) == 0) {
					lines.set(i, "*:" + legends[j].trim() + "=" + color[j]);
					done.set(j, true);
					break;
				}
			}
		}
		for (int i = 0; i < done.size(); i++) {
			if (!done.get(i)) {
				lines.add(queryNameForChart + ":" + legends[i].trim() + "=" + color[i]);
			}
		}
		if (debug)
			System.out.println("Chart::storelegendInfoGlobal:path: " + path);
		String[] writestr = lines.toArray(new String[0]);
		FileOutputStream fos = FileUtil.initOutFile(path, false);
		FileUtil.writetofile(fos, writestr);
	}

	/**
	 * Store local legend information to a property file.
	 * @param legends legend label of a JFreeChart (not null)
	 * @param color colors of each legend
	 */
	public void storelegendInfoLocal(String[] legends, String[] color) {
		if (path == null) {
			System.out.println("Null path to legend color file");
			return;
		}
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			System.out.println("Could not read in any previous color data.");
		}
		ArrayList<Boolean> done = new ArrayList<>();
		for (int i = 0; i < legends.length; i++) {
			done.add(false);
		}
		String firstPart = null;
		String secondPart = null;
		String queryNameForChart = "\"" + chart.getTitle().getText() + "\"";
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith("*")) {
				continue;
			}
			if (!lines.get(i).contains(":")) {
				continue;
			}
			firstPart = lines.get(i).split(":")[0];
			secondPart = lines.get(i).split(":")[1].split("=")[0];
			for (int j = 0; j < legends.length; j++) {
				if (secondPart.compareTo(legends[j]) == 0 && firstPart.compareTo(queryNameForChart) == 0) {
					lines.set(i, queryNameForChart + ":" + legends[j].trim() + "=" + color[j]);
					done.set(j, true);
					break;
				}
			}
		}
		for (int i = 0; i < done.size(); i++) {
			if (!done.get(i)) {
				lines.add(queryNameForChart + ":" + legends[i].trim() + "=" + color[i]);
			}
		}
		if (debug)
			System.out.println("Chart::storelegendInfoLocal:path: " + path);
		String[] writestr = lines.toArray(new String[0]);
		FileOutputStream fos = FileUtil.initOutFile(path, false);
		FileUtil.writetofile(fos, writestr);
	}

	/**
	 * Get legend information from input legend and color.
	 * @param legends legend label of a JFreeChart (not null)
	 * @param color colors of each legend
	 * @return An array of list legend data string
	 */
	public ArrayList<String> legendInfoFromProperties(String[] legends, String[] color) {
		ArrayList<String> tempstrAl = new ArrayList<>();
		Object[] temp = legendInfoFromProperties(path);
		String queryNameForChart = "\"" + chart.getTitle().getText() + "\"";
		if (temp.length == 0) {
			for (int i = 0; i < legends.length; i++)
				tempstrAl.add(legends[i].trim() + "=" + color[i]);
		} else {
			for (int i = 0; i < temp.length; i++) {
				String myLine = (String) temp[i];
				if (myLine.contains(":") && !myLine.contains("*")) {
					String queryNameInFile = ((String) temp[i]).split(":")[0].trim();
					if (queryNameInFile.equals(queryNameForChart)) {
						// Currently, lines with a matching query name and ':' but no '*'
						// are not used to populate legend entries; skip further processing.
						continue;
					}
				} else {
					String firstPart = ((String) temp[i]).split("=")[0];
					String colorPart = ((String) temp[i]).split("=")[1].trim();
					if (firstPart.startsWith("*:")) {
						String keyInLine = firstPart.replace("*:", "");
						int idx = legendResourceExist(legends, keyInLine);
						if (idx > -1) {
							if (!color[idx].trim().equals(colorPart)) {
								tempstrAl.add(legends[idx].trim() + "=" + color[idx]);
							}
						} else {
							tempstrAl.add((String) temp[i]);
						}
					}
				}
			}
			String[] tempStr = readLegendItemsFromProperties();
			for (int i = 0; i < legends.length; i++) {
				String key = legends[i].trim();
				if (!Arrays.asList(tempStr).contains(key))
					tempstrAl.add(key + "=" + color[i]);
			}
		}
		return tempstrAl;
	}

	/**
	 * Read legend items from properties file.
	 * @return Array of legend item strings
	 */
	private String[] readLegendItemsFromProperties() {
		Object[] temp = legendInfoFromProperties(path);
		String[] tempStr = new String[temp.length];
		for (int i = 0; i < temp.length; i++) {
			String myLine = (String) temp[i];
			if (myLine.contains(":") && !myLine.contains("*")) {
				String secondPart = ((String) temp[i]).split(":")[1].trim();
				tempStr[i] = secondPart.split("=")[0].trim();
				if (debug)
					System.out.println("Chart::readLegendItemsFromProperties:read a local line.");
			} else {
				String firstPart = ((String) temp[i]).split("=")[0].trim();
				if (firstPart.startsWith("*:")) {
					tempStr[i] = firstPart.replace("*:", "");
				} else {
					tempStr[i] = firstPart;
				}
			}
		}
		return tempStr;
	}

	/**
	 * Read query info from properties file.
	 * @return Array of query info strings
	 */
	private String[] readQueryInfoFromProperties() {
		Object[] temp = legendInfoFromProperties(path);
		String[] queryStr = new String[temp.length];
		for (int i = 0; i < temp.length; i++) {
			String myLine = (String) temp[i];
			if (myLine.contains(":") && !myLine.contains("*")) {
				String queryNameInFile = ((String) temp[i]).split(":")[0].trim();
				queryStr[i] = queryNameInFile;
			} else {
				String firstPart = ((String) temp[i]).split("=")[0].trim();
				if (firstPart.startsWith("*:")) {
					queryStr[i] = "*";
				} else {
					queryStr[i] = "*";
				}
			}
		}
		return queryStr;
	}

	/**
	 * Get legend information from a property file.
	 * @param path legend data property file location (not null)
	 * @return Array of legend data
	 */
	public Object[] legendInfoFromProperties(String path) {
		LineNumberReader lineReader = null;
		Object[] temp = null;
		try {
			DataInputStream dis = FileUtil.initInFile(path);
			lineReader = new LineNumberReader(new InputStreamReader(dis));
			temp = lineReader.lines().toArray();
			lineReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Legend info not read (FNF): " + e.toString());
		} catch (IOException e) {
			System.out.println("Legend info not read (e): " + e.toString());
		}
		return temp;
	}

	/**
	 * Check if legend resource exists in legends array.
	 * @param legends Array of legend strings
	 * @param key Legend key to check
	 * @return Index of legend if exists, -1 otherwise
	 */
	protected int legendResourceExist(String[] legends, String key) {
		for (int i = 0; i < legends.length; i++)
			legends[i] = legends[i].trim();
		return Arrays.asList(legends).indexOf(key);
	}

	/**
	 * Build texture paint with pattern and stroke information and store in Chart object.
	 */
	protected void buildPaint() {
		if (color == null)
			return;
		paint = new TexturePaint[color.length];
		for (int i = paint.length - 1; i > -1; i--) {
			paint[i] = LegendUtil.getTexturePaint(new Color(color[i]), new Color(pColor[i]), pattern[i], lineStrokes[i]);
			if (debug)
				System.out.println("Chart::buildPaint:pattern: " + pattern[i] + " line: " + lineStrokes[i] + " i: " + i);
		}
	}

	/**
	 * Verify axis name and unit for index i.
	 * @param i Axis index
	 * @return Axis name and unit string
	 */
	protected String verifyAxisName_unit(int i) {
		String axis = "";
		if (axis_name_unit != null && i < axis_name_unit.length)
			axis = axis_name_unit[i];
		return axis;
	}

	/**
	 * Set chart properties and legend position.
	 */
	protected void setChartProperty() {
		if (markerMap != null)
			MarkerUtil.createMarker(chart, markerMap);
		ChartUtil.setSubTitle(chart, titles);
		int lMax = 0;
		String[] temp = legend.split(",");
		for (String s : temp) {
			lMax = Math.max(lMax, s.length());
		}
		myLegend = chart.getLegend();
		chart.getLegend().setPosition(RectangleEdge.RIGHT);
		if (legend.split(",").length < 15 && lMax < 30) {
			// chart.getLegend().setVisible(true);
		} else {
			chart.removeLegend();
		}
		setTitleChangeListener();
		ChartUtils.applyCurrentTheme(chart);
	}

	/**
	 * Set title change listener for chart.
	 */
	protected void setTitleChangeListener() {
		ChartTitleChangeListener mctcl = new ChartTitleChangeListener(this);
		if (chartClassName.contains("Line"))
			chart.getSubtitle(0).addChangeListener(mctcl);
		else
			chart.getTitle().addChangeListener(mctcl);
	}

	// Getters and setters
	public void setLegend(String legend) { this.legend = legend; }
	public void setColor(int[] color) { this.color = color.clone(); }
	public void setColor(int color, int idx) { this.color[idx] = color; }
	public void setPattern(int pattern, int idx) { this.pattern[idx] = pattern; }
	public void setPattern(int[] pattern) { this.pattern = pattern.clone(); }
	public void setLineStrokes(int[] lineStrokes) { this.lineStrokes = lineStrokes.clone(); }
	public JFreeChart getChart() { return chart; }
	public String[] getTitles() { return titles; }
	public String[] getAxis_name_unit() { return axis_name_unit; }
	public String getChartClassName() { return chartClassName; }
	public String getGraphName() { return graphName; }
	public String getMeta() { return meta; }
	public String getLegend() { return legend; }
	public int[] getColor() { return color; }
	public int[] getPattern() { return pattern; }
	public int[] getLineStrokes() { return lineStrokes; }
	public int getRelativeColIndex() { return relativeColIndex; }
	public int[] getpColor() { return pColor; }
	public void setpColor(int[] pColor) { this.pColor = pColor.clone(); }
	public void setChart(JFreeChart chart) { this.chart = chart; }
	public void setTitles(String[] titles) { this.titles = titles.clone(); }
	public void setTitles(String title, int idx) { this.titles[idx] = title; }
	public TexturePaint[] getPaint() { return paint; }
	public void setPaint(TexturePaint[] paint) { this.paint = paint.clone(); }
	public Map<String, Marker> getMarkerMap() { return markerMap; }
	public void setMarkerMap(Map<String, Marker> markerMap) { this.markerMap = markerMap; }
	public String[][] getAnnotationText() { return annotationText; }
	public void setAnnotationText(String[][] annotationText) { this.annotationText = annotationText.clone(); }
	public void setChartClassName(String chartClassName) { this.chartClassName = chartClassName; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public boolean isShowLineAndShape() { return showLineAndShape; }
	public void setShowLineAndShape(boolean showLineAndShape) { this.showLineAndShape = showLineAndShape; }
	public String getMetaCol() { return metaCol; }
	public String getChartColumn() { return chartColumn; }
	public String getChartRow() { return chartRow; }
}
