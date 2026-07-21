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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;

import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;

import conversionUtil.ArrayConversion;

/**
 * Utility functions for chart legends, including creation, color, pattern, and
 * stroke handling. Handles legend item collections for various chart types and
 * provides color/pattern utilities.
 *
 * Author: TWU Created: 1/2/2016
 */
public class LegendUtil {
	private static boolean debug = false;
	/** List of supported pattern codes. */
	public static final String[] patternList = { "-4162", "-4126", "11", "14", "16", "17" };
	/** List of supported stroke codes. */
	public static final String[] strokeList = { "0", "5", "10", "20", "30", "40" };

	/**
	 * Creates a LegendItemCollection with color and pattern for each legend entry.
	 * 
	 * @param legend   Legend labels
	 * @param color    Colors for each legend
	 * @param pattern  Pattern codes for each legend
	 * @param pColor   Pattern colors for each legend
	 * @param stroke_i Stroke indices for each legend
	 * @return LegendItemCollection for chart
	 */
	public static LegendItemCollection crtLegenditemcollection(String[] legend, int[] color, int[] pattern,
			int[] pColor, int[] stroke_i) {
		LegendItemCollection legenditemcollection = new LegendItemCollection();
		for (int i = legend.length - 1; i > -1; i--) {
			String key = legend[i].trim();
			LegendItem legenditem;
			java.awt.TexturePaint tp = null;
			// If pattern is specified and not default, use texture paint
			if (pattern != null && pattern[i] != -4105 && pattern[i] != 1) {
				tp = LegendUtil.getTexturePaint(new Color(color[i]), new Color(pColor[i]), pattern[i], stroke_i[i]);
				legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, tp);
			} else {
				legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, new Color(color[i]));
			}
			legenditemcollection.add(legenditem);
		}
		return legenditemcollection;
	}

	/**
	 * Creates a LegendItemCollection with only color for each legend entry.
	 * 
	 * @param legend Legend labels
	 * @param color  Colors for each legend
	 * @return LegendItemCollection for chart
	 */
	public static LegendItemCollection crtLegenditemcollection(String[] legend, int[] color) {
		LegendItemCollection legenditemcollection = new LegendItemCollection();
		for (int i = legend.length - 1; i > -1; i--) {
			String key = legend[i].trim();
			LegendItem legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX,
					new Color(color[i]));
			legenditemcollection.add(legenditem);
		}
		return legenditemcollection;
	}

	/**
	 * Creates a LegendItemCollection using TexturePaint and JComboBox lookup for
	 * each legend entry.
	 * 
	 * @param legend    Legend labels
	 * @param tp        Array of TexturePaints
	 * @param jcbLookup Lookup for JComboBox selection per legend
	 * @return LegendItemCollection for chart
	 */
	public static LegendItemCollection crtLegenditemcollection(String[] legend, TexturePaint[] tp,
			HashMap<String, JComboBox> jcbLookup) {
		LegendItemCollection legenditemcollection = new LegendItemCollection();
		for (int i = legend.length - 1; i > -1; i--) {
			String key = legend[i].trim();
			TexturePaint paintstyle;
			JComboBox jcb = jcbLookup.get(legend[i]);
			int j = jcb.getSelectedIndex();
			try {
				paintstyle = tp[j];
			} catch (Exception e) {
				paintstyle = tp[0];
			}
			LegendItem legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, paintstyle);
			legenditemcollection.add(legenditem);
		}
		return legenditemcollection;
	}

	/**
	 * Creates a LegendItemCollection using Paint array for each legend entry.
	 * 
	 * @param legend Legend labels
	 * @param tp     Array of Paints
	 * @return LegendItemCollection for chart, or null if problem detected
	 */
	public static LegendItemCollection crtLegenditemcollection(String[] legend, Paint[] tp) {

		LegendItemCollection legenditemcollection = new LegendItemCollection();
		boolean problem = false;
		for (int i = legend.length - 1; i > -1; i--) {
			int temp = tp.length;
			int paintIndex = Math.floorMod(i, temp);
			//commented code was used to address issue when there were more legend items than paint styles; Now addressed with Mod function
			if (i >= tp.length) {
				problem = true;
			}
			// if (!problem) {
			String key = legend[i].trim();
			LegendItem legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, tp[paintIndex]);
			legenditemcollection.add(legenditem);
			// }
		}
		if (problem) {
			System.out.println(
					"Note: Difficulty constructing legend for transposed graphic. Check result for suitability.");
		}
		return legenditemcollection;
	}

	/**
	 * Creates a LegendItemCollection for XYPlot using Paint array for visible
	 * series.
	 * 
	 * @param plot   XYPlot
	 * @param legend Legend labels
	 * @param tp     Array of Paints
	 * @return LegendItemCollection for chart
	 */
	public static LegendItemCollection crtLegenditemcollection(XYPlot plot, String[] legend, Paint[] tp) {
		LegendItemCollection legenditemcollection = new LegendItemCollection();
		for (int i = legend.length - 1; i > -1; i--) {
			if (plot.getRenderer().isSeriesVisible(i)) {
				String key = legend[i].trim();
				if (debug)
					System.out.println("crtLegenditemcollection::key: " + key);
				LegendItem legenditem = new LegendItem(key, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX,
						tp[legend.length - 1 - i]);
				legenditemcollection.add(legenditem);
			}
		}
		return legenditemcollection;
	}

	/**
	 * Returns the index of a target string in a result array.
	 * 
	 * @param target Target string
	 * @param result Array to search
	 * @return Index of target, or -1 if not found
	 */
	public static int checkIndex(String target, String[] result) {
		List<String> l = Arrays.asList(result);
		return l.indexOf(target);
	}

	/**
	 * Gets the LegendItemCollection from a JFreeChart.
	 * 
	 * @param chart JFreeChart
	 * @return LegendItemCollection
	 */
	public static LegendItemCollection getLegendItemCollectionFromChart(JFreeChart chart) {
		if (chart.getPlot().getPlotType().contains("Category"))
			return chart.getCategoryPlot().getLegendItems();
		else
			return chart.getXYPlot().getLegendItems();
	}

	/**
	 * Gets a distributed LegendTitle from a chart's fixed legend items.
	 * 
	 * @param chart JFreeChart
	 * @return LegendTitle
	 */
	public static LegendTitle getDistLegendItemCollectionFromChart(JFreeChart chart) {
		JFreeChart chartC = null;
		try {
			chartC = (JFreeChart) chart.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		LegendItemCollection lc;
		if (chart.getPlot().getPlotType().contains("Category"))
			lc = chart.getCategoryPlot().getFixedLegendItems();
		else
			lc = chart.getXYPlot().getFixedLegendItems();
		Map<String, String> lgdMap = new LinkedHashMap<>();
		for (int i = 0; i < lc.getItemCount(); i++)
			lgdMap.put(lc.get(i).getLabel(), String.valueOf(i));
		String[] l = lgdMap.keySet().toArray(new String[0]);
		TexturePaint[] tp = new TexturePaint[lgdMap.size()];
		int[] c = new int[lgdMap.size()];
		for (int i = 0; i < l.length; i++) {
			c[i] = lc.get(Integer.parseInt(lgdMap.get(l[i]))).getFillPaint().hashCode();
			tp[i] = (TexturePaint) lc.get(Integer.parseInt(lgdMap.get(l[i]))).getFillPaint();
		}
		lc = crtLegenditemcollection(l, tp);
		chartC.getCategoryPlot().setFixedLegendItems(lc);
		return new LegendTitle(chartC.getCategoryPlot());
	}

	/**
	 * Gets legend labels from a JFreeChart.
	 * 
	 * @param jfchart JFreeChart
	 * @return Array of legend labels
	 */
	public static String[] getLegendLabels(JFreeChart jfchart) {
		if (jfchart.getPlot().getPlotType().equals("Category Plot"))
			return getLegendLabels(jfchart.getCategoryPlot().getLegendItems());
		else if (jfchart.getPlot().getPlotType().equals("XY Plot"))
			return getLegendLabels(jfchart.getXYPlot().getLegendItems());
		else if (jfchart.getPlot().getPlotType().equals("Pie Plot"))
			return getLegendLabels(((PiePlot) jfchart.getPlot()).getLegendItems());
		else
			return null;
	}

	/**
	 * Gets legend label string from a JFreeChart.
	 * 
	 * @param jfchart JFreeChart
	 * @return Legend label string
	 */
	public static String getLegendLabel(JFreeChart jfchart) {
		if (jfchart.getPlot().getPlotType().equals("Category Plot"))
			return getLegendLabel(jfchart.getCategoryPlot().getFixedLegendItems());
		else
			return getLegendLabel(jfchart.getXYPlot().getLegendItems());
	}

	/**
	 * Gets legend labels from a LegendItemCollection.
	 * 
	 * @param lc LegendItemCollection
	 * @return Array of legend labels
	 */
	public static String[] getLegendLabels(LegendItemCollection lc) {
		String[] ll = new String[lc.getItemCount()];
		for (int i = 0; i < lc.getItemCount(); i++)
			ll[i] = lc.get(i).getLabel();
		return ll;
	}

	/**
	 * Gets legend label string from a LegendItemCollection.
	 * 
	 * @param lc LegendItemCollection
	 * @return Legend label string
	 */
	public static String getLegendLabel(LegendItemCollection lc) {
		return ArrayConversion.array2String(getLegendLabels(lc));
	}

	/**
	 * Gets legend colors from a JFreeChart.
	 * 
	 * @param jfchart JFreeChart
	 * @return Array of legend colors (RGB int)
	 */
	public static int[] getLegendColor(JFreeChart jfchart) {
		if (jfchart.getPlot().getPlotType().equals("Category Plot"))
			return getLegendColor(jfchart.getCategoryPlot().getFixedLegendItems());
		else
			return getLegendColor(jfchart.getXYPlot().getLegendItems());
	}

	/**
	 * Gets legend colors from a LegendItemCollection.
	 * 
	 * @param lc LegendItemCollection
	 * @return Array of legend colors (RGB int)
	 */
	public static int[] getLegendColor(LegendItemCollection lc) {
		int[] tp = new int[lc.getItemCount()];
		for (int i = 0; i < lc.getItemCount(); i++) {
			Color c = (Color) lc.get(i).getFillPaint();
			// Avoid colors too close to white for visibility
			if (c.getRed() == 255 && c.getGreen() == 255 && c.getBlue() > 153) {
				c = new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 153));
			}
			tp[i] = c.getRGB();
		}
		return tp;
	}

	/**
	 * Gets legend Paint array from a JFreeChart.
	 * 
	 * @param jfchart JFreeChart
	 * @return Array of Paints
	 */
	public static Paint[] getLegendPaint(JFreeChart jfchart) {
		if (jfchart.getPlot().getPlotType().equals("Category Plot"))
			return getLegendPaint(jfchart.getCategoryPlot().getLegendItems());
		else if (jfchart.getPlot().getPlotType().contains("Pie"))
			return getLegendPaint(((PiePlot) jfchart.getPlot()).getLegendItems());
		else if (jfchart.getPlot().getPlotType().equals("XY Plot"))
			return getLegendPaint(jfchart.getXYPlot().getLegendItems());
		else
			return null;
	}

	/**
	 * Gets legend Paint array from a LegendItemCollection.
	 * 
	 * @param lc LegendItemCollection
	 * @return Array of Paints
	 */
	public static Paint[] getLegendPaint(LegendItemCollection lc) {
		Paint[] tp = new Paint[lc.getItemCount()];
		for (int i = 0; i < lc.getItemCount(); i++) {
			tp[i] = lc.get(i).getFillPaint();
		}
		return tp;
	}

	/**
	 * Adjusts legend items by index order.
	 * 
	 * @param indexs Array of indices
	 * @param lc     LegendItemCollection
	 * @return Adjusted LegendItemCollection
	 */
	public static LegendItemCollection adjLenend(int[] indexs, LegendItemCollection lc) {
		LegendItemCollection legendItemCollection = new LegendItemCollection();
		Stack<LegendItem> s = new Stack<>();
		for (int i = 0; i < indexs.length; i++) {
			int c = lc.getItemCount() - 1 - indexs[i];
			if (debug)
				System.out.println("LegendUtil::LegendItemCollection:i: " + i + " index: " + indexs[i] + " lc count: "
						+ lc.getItemCount());
			LegendItem legenditem = lc.get(c);
			s.push(legenditem);
		}
		int sc = s.size();
		for (int i = 0; i < sc; i++) {
			LegendItem legenditem = s.pop();
			legendItemCollection.add(legenditem);
		}
		return legendItemCollection;
	}

	/**
	 * Returns a Color from RGB values.
	 * 
	 * @param r Red
	 * @param g Green
	 * @param b Blue
	 * @return Color
	 */
	public static Color getColorbyRGB(int r, int g, int b) {
		return new Color(r, g, b);
	}

	/**
	 * Returns the RGB int value of a Color.
	 * 
	 * @param color Color
	 * @return RGB int value
	 */
	public static int getRGB(Color color) {
		return color.getRGB();
	}

	/**
	 * Returns a Color from an RGB int value.
	 * 
	 * @param c RGB int value
	 * @return Color
	 */
	public static Color getRGB(int c) {
		return new Color(c);
	}

	/**
	 * Creates a TexturePaint from a BufferedImage and Color.
	 * 
	 * @param image BufferedImage
	 * @param color Color
	 * @return TexturePaint
	 */
	public static TexturePaint getTexturePaint(BufferedImage image, Color color) {
		int imW = 10;
		int imH = 10;
		Graphics2D g2im = image.createGraphics();
		g2im.setColor(color);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setStroke(new BasicStroke(1.0F));
		g2im.drawImage(image, null, 0, 0);
		return new TexturePaint(image, new Rectangle(imW, imH));
	}

	/**
	 * Creates a TexturePaint from a Color and pattern code using a file chooser.
	 * 
	 * @param color   Color
	 * @param pattern Pattern code
	 * @return TexturePaint
	 */
	public static TexturePaint getTexturePaint(Color color, int pattern) {
		TexturePaint tp = null;
		FileChooser chooser = FileChooserFactory.getFileChooser();
		File[] selected = chooser.doFilePrompt(null,
				"Select files location",
				FileChooser.LOAD_DIALOG,
				new File(System.getProperty("user.home", ".")),
				null);
		if (selected != null && selected.length > 0 && selected[0] != null) {
			BufferedImage image = null;
			try {
				File input = selected[0];
				image = ImageIO.read(input);
			} catch (IOException ex) {
				System.out.println("error: " + ex.getMessage());
			}
			tp = getTexturePaint(image, color);
		}
		return tp;
	}

	/**
	 * Creates a BufferedImage with base and overlay color and size.
	 * 
	 * @param baseColor Base color
	 * @param color     Overlay color
	 * @param size      Image size
	 * @return BufferedImage
	 */
	public static BufferedImage getBufferedImage(Color baseColor, Color color, int size) {
		int imW = size;
		int imH = size;
		BufferedImage im = new BufferedImage(imW, imH, 1);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(baseColor);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(color);
		g2im.setStroke(new BasicStroke(2.0F));
		g2im.drawLine(0, imH, imW, 0);
		g2im.drawRect(0, 0, imH, imW);
		return im;
	}

	/**
	 * Creates a BufferedImage with base and overlay color.
	 * 
	 * @param baseColor Base color
	 * @param color     Overlay color
	 * @return BufferedImage
	 */
	public static BufferedImage getBufferedImage(Color baseColor, Color color) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, 1);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(baseColor);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(color);
		g2im.setStroke(new BasicStroke(2.0F));
		g2im.drawRect(0, 0, imH, imW);
		return im;
	}

	/**
	 * Sets the color of a TexturePaint.
	 * 
	 * @param paint TexturePaint
	 * @param color Color
	 * @return TexturePaint
	 */
	public static TexturePaint setTexturePaintColor(TexturePaint paint, Color color) {
		Graphics2D g2im = paint.getImage().createGraphics();
		g2im.setColor(color);
		return paint;
	}

	/**
	 * Converts a hex string to RGB int value.
	 * 
	 * @param h Hex string (e.g. "FF00FF")
	 * @return RGB int value
	 */
	public static int Hex2RGB(String h) {
		int r = Integer.parseInt(h.substring(0, 2), 16);
		int g = Integer.parseInt(h.substring(2, 4), 16);
		int b = Integer.parseInt(h.substring(4, 6), 16);
		Color color = new Color(r, g, b);
		return color.getRGB();
	}

	/**
	 * Gets the pattern file name for a given pattern code.
	 * 
	 * @param pattern Pattern code
	 * @return Pattern file name
	 */
	static String getPatternFile(int pattern) {
		String rs = null;
		switch (pattern) {
		case -4162:
			rs = "PatternedFill26.jpg";
			break;
		case 11:
			rs = "PatternedFill13.jpg";
			break;
		case 14:
			rs = "PatternedFill44.jpg";
			break;
		case 16:
			rs = "PatternedFill38.jpg";
			break;
		case 17:
			rs = "PatternedFill37.jpg";
			break;
		}
		return rs;
	}

	/**
	 * Creates a TexturePaint with color, pattern color, pattern code, and stroke
	 * index.
	 * 
	 * @param color    Base color
	 * @param pcolor   Pattern color
	 * @param pattern  Pattern code
	 * @param stroke_i Stroke index
	 * @return TexturePaint
	 */
	public static TexturePaint getTexturePaint(Color color, Color pcolor, int pattern, int stroke_i) {
		TexturePaint tp;
		BasicStroke stroke_w = getLineStroke(stroke_i);
		// Select pattern type
		switch (pattern) {
		case -4126:
			tp = getBufferedImageO(color, pcolor, stroke_w);
			break;
		case -4162:
			tp = getBufferedImageU(color, pcolor, stroke_w);
			break;
		case 11:
			tp = getBufferedImageH(color, pcolor, stroke_w);
			break;
		case 14:
			tp = getBufferedImageV(color, pcolor, stroke_w);
			break;
		case 16:
			tp = getBufferedImageX(color, pcolor, stroke_w);
			break;
		case 17:
			tp = getBufferedImageD(color, pcolor, stroke_w);
			break;
		default:
			tp = getDefaultTP(color, stroke_w);
			break;
		}
		return tp;
	}

	/**
	 * Returns a default TexturePaint with color and stroke.
	 * 
	 * @param paint    Color
	 * @param stroke_w Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getDefaultTP(Color paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setStroke(stroke_w);
		g2im.setColor(paint);
		g2im.fillRect(0, 0, imW, imH);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with U pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageU(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.drawLine(0, imH, imW, 0);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with D pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageD(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.drawLine(0, 0, imW, imH);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with X pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageX(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.drawLine(0, imH, imW, 0);
		g2im.drawLine(0, 0, imW, imH);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with H pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageH(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.drawLine(0, imH / 2, imW, imH / 2);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with V pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageV(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.drawLine(imW / 2, 0, imW / 2, imH);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a TexturePaint with O pattern.
	 * 
	 * @param paint     Base color
	 * @param stk_paint Pattern color
	 * @param stroke_w  Stroke
	 * @return TexturePaint
	 */
	public static TexturePaint getBufferedImageO(Color paint, Color stk_paint, BasicStroke stroke_w) {
		int imW = 10;
		int imH = 10;
		BufferedImage im = new BufferedImage(imW, imH, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2im = im.createGraphics();
		g2im.setColor(paint);
		g2im.setStroke(stroke_w);
		g2im.fillRect(0, 0, imW, imH);
		g2im.setColor(stk_paint);
		g2im.fillOval(imW / 8, imW / 8, 3 * imW / 4, 3 * imH / 4);
		return new TexturePaint(im, new Rectangle(imW, imH));
	}

	/**
	 * Returns a BasicStroke for a given dash index.
	 * 
	 * @param dashIndex Dash index
	 * @return BasicStroke
	 */
	public static BasicStroke getLineStroke(int dashIndex) {
		BasicStroke stroke;
		switch (dashIndex) {
		case 0:
			stroke = new BasicStroke(1.0F);
			break;
		case 5:
			stroke = new BasicStroke(2.0F);
			break;
		case 10:
			stroke = new BasicStroke(3.0F);
			break;
		case 20:
			stroke = new BasicStroke(2.0F, 1, 1, 1.0F, new float[] { 2.0F, 6.0F }, 0.0F);
			break;
		case 30:
			stroke = new BasicStroke(2.0F, 1, 1, 1.0F, new float[] { 4.0F, 6.0F }, 0.0F);
			break;
		case 40:
			stroke = new BasicStroke(2.0F, 1, 1, 1.0F, new float[] { 6.0F, 6.0F }, 0.0F);
			break;
		default:
			stroke = new BasicStroke(2.0F);
		}
		return ChartUtil.scaleStroke(stroke);
	}
}
