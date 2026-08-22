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
import java.awt.Font;
import java.awt.Paint;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.CategoryPointerAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import ModelInterface.InterfaceMain;

/**
 * Utility functions for JFreeChart operations, including dataset/series lookup, annotation creation,
 * legend handling, and series painting. Designed for use in GLIMPSE Model Interface enhancements.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class ChartUtil {
    private static boolean debug = false;

    public static final class GraphicsPreferences {
        public final int titleFontSize;
        public final int subtitleFontSize;
        public final int domainAxisLabelFontSize;
        public final int domainAxisTickFontSize;
        public final int rangeAxisLabelFontSize;
        public final int rangeAxisTickFontSize;
        public final int legendFontSize;
        public final float lineWidthScale;

        private GraphicsPreferences(int titleFontSize, int subtitleFontSize, int domainAxisLabelFontSize,
                int domainAxisTickFontSize, int rangeAxisLabelFontSize, int rangeAxisTickFontSize,
                int legendFontSize, float lineWidthScale) {
            this.titleFontSize = titleFontSize;
            this.subtitleFontSize = subtitleFontSize;
            this.domainAxisLabelFontSize = domainAxisLabelFontSize;
            this.domainAxisTickFontSize = domainAxisTickFontSize;
            this.rangeAxisLabelFontSize = rangeAxisLabelFontSize;
            this.rangeAxisTickFontSize = rangeAxisTickFontSize;
            this.legendFontSize = legendFontSize;
            this.lineWidthScale = lineWidthScale;
        }
    }

    public static final class ThumbnailGraphicsPreferences {
        public final int fontSize;
        public final float lineWidth;

        private ThumbnailGraphicsPreferences(int fontSize, float lineWidth) {
            this.fontSize = fontSize;
            this.lineWidth = lineWidth;
        }
    }

    private static int parseBoundedInt(String rawValue, int fallback, int min, int max) {
        int boundedFallback = Math.max(min, Math.min(max, fallback));
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return boundedFallback;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException nfe) {
            return boundedFallback;
        }
    }

    private static float parseBoundedFloat(String rawValue, double fallback, double min, double max) {
        double boundedFallback = Math.max(min, Math.min(max, fallback));
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return (float) boundedFallback;
        }
        try {
            double parsed = Double.parseDouble(rawValue.trim());
            return (float) Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException nfe) {
            return (float) boundedFallback;
        }
    }

    public static GraphicsPreferences getGraphicsPreferences() {
        java.util.Properties props = null;
        try {
            InterfaceMain instance = InterfaceMain.getInstance();
            if (instance != null) {
                props = instance.getProperties();
            }
        } catch (Throwable ignored) {
            // Preserve chart rendering in headless or early-start contexts.
        }
        return new GraphicsPreferences(
                parseBoundedInt(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_TITLE_FONT_SIZE_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_TITLE_FONT_SIZE,
                        InterfaceMain.MIN_GRAPHICS_FONT_SIZE,
                        InterfaceMain.MAX_GRAPHICS_FONT_SIZE),
                parseBoundedInt(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE,
                        InterfaceMain.MIN_GRAPHICS_FONT_SIZE,
                        InterfaceMain.MAX_GRAPHICS_FONT_SIZE),
                InterfaceMain.resolveGraphicsFontSize(props,
                        InterfaceMain.GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
                        InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
                        InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE),
                InterfaceMain.resolveGraphicsFontSize(props,
                        InterfaceMain.GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
                        InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
                        InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE),
                InterfaceMain.resolveGraphicsFontSize(props,
                        InterfaceMain.GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
                        InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
                        InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE),
                InterfaceMain.resolveGraphicsFontSize(props,
                        InterfaceMain.GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
                        InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
                        InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE),
                parseBoundedInt(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_LEGEND_FONT_SIZE_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_LEGEND_FONT_SIZE,
                        InterfaceMain.MIN_GRAPHICS_FONT_SIZE,
                        InterfaceMain.MAX_GRAPHICS_FONT_SIZE),
                parseBoundedFloat(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_LINE_WIDTH_SCALE_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_LINE_WIDTH_SCALE,
                        InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE,
                        InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE));
    }

    public static float getLineWidthScale() {
        return getGraphicsPreferences().lineWidthScale;
    }

    private static int boundedFontSize(Font font, int fallback) {
        if (font == null) {
            return fallback;
        }
        return InterfaceMain.parseBoundedIntValue(Integer.toString(font.getSize()), fallback,
                InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE);
    }

    private static int resolveAxisLabelFontSize(Axis axis, int fallback) {
        if (axis != null && axis.getLabelFont() != null) {
            return boundedFontSize(axis.getLabelFont(), fallback);
        }
        return fallback;
    }

    private static int resolveAxisTickFontSize(Axis axis, int fallback) {
        if (axis != null && axis.getTickLabelFont() != null) {
            return boundedFontSize(axis.getTickLabelFont(), fallback);
        }
        return fallback;
    }

    public static GraphicsPreferences captureGraphicsPreferences(JFreeChart chart) {
        GraphicsPreferences prefs = getGraphicsPreferences();
        if (chart == null) {
            return prefs;
        }
        int titleFontSize = prefs.titleFontSize;
        int subtitleFontSize = prefs.subtitleFontSize;
        int domainAxisLabelFontSize = prefs.domainAxisLabelFontSize;
        int domainAxisTickFontSize = prefs.domainAxisTickFontSize;
        int rangeAxisLabelFontSize = prefs.rangeAxisLabelFontSize;
        int rangeAxisTickFontSize = prefs.rangeAxisTickFontSize;
        int legendFontSize = prefs.legendFontSize;
        if (chart.getTitle() != null) {
            titleFontSize = boundedFontSize(chart.getTitle().getFont(), titleFontSize);
        }
        for (Object subtitleObj : chart.getSubtitles()) {
            if (subtitleObj instanceof TextTitle) {
                subtitleFontSize = boundedFontSize(((TextTitle) subtitleObj).getFont(), subtitleFontSize);
                break;
            }
        }
        if (chart.getLegend() != null) {
            legendFontSize = boundedFontSize(chart.getLegend().getItemFont(), legendFontSize);
        }
        Plot plot = chart.getPlot();
        if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            domainAxisLabelFontSize = resolveAxisLabelFontSize(categoryPlot.getDomainAxis(), domainAxisLabelFontSize);
            domainAxisTickFontSize = resolveAxisTickFontSize(categoryPlot.getDomainAxis(), domainAxisTickFontSize);
            rangeAxisLabelFontSize = resolveAxisLabelFontSize(categoryPlot.getRangeAxis(), rangeAxisLabelFontSize);
            rangeAxisTickFontSize = resolveAxisTickFontSize(categoryPlot.getRangeAxis(), rangeAxisTickFontSize);
        } else if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            domainAxisLabelFontSize = resolveAxisLabelFontSize(xyPlot.getDomainAxis(), domainAxisLabelFontSize);
            domainAxisTickFontSize = resolveAxisTickFontSize(xyPlot.getDomainAxis(), domainAxisTickFontSize);
            rangeAxisLabelFontSize = resolveAxisLabelFontSize(xyPlot.getRangeAxis(), rangeAxisLabelFontSize);
            rangeAxisTickFontSize = resolveAxisTickFontSize(xyPlot.getRangeAxis(), rangeAxisTickFontSize);
        } else if (plot instanceof PiePlot) {
            PiePlot piePlot = (PiePlot) plot;
            int pieLabelFontSize = boundedFontSize(piePlot.getLabelFont(), domainAxisLabelFontSize);
            domainAxisLabelFontSize = pieLabelFontSize;
            rangeAxisLabelFontSize = pieLabelFontSize;
        }
        return new GraphicsPreferences(titleFontSize, subtitleFontSize, domainAxisLabelFontSize,
                domainAxisTickFontSize, rangeAxisLabelFontSize, rangeAxisTickFontSize,
                legendFontSize, prefs.lineWidthScale);
    }

    public static ThumbnailGraphicsPreferences getThumbnailGraphicsPreferences() {
        java.util.Properties props = null;
        try {
            InterfaceMain instance = InterfaceMain.getInstance();
            if (instance != null) {
                props = instance.getProperties();
            }
        } catch (Throwable ignored) {
            // Keep thumbnail rendering resilient in startup/headless contexts.
        }
        return new ThumbnailGraphicsPreferences(
                parseBoundedInt(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE,
                        InterfaceMain.MIN_GRAPHICS_FONT_SIZE,
                        InterfaceMain.MAX_GRAPHICS_FONT_SIZE),
                parseBoundedFloat(props == null ? null : props.getProperty(InterfaceMain.GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY),
                        InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH,
                        InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE,
                        InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE));
    }

    public static BasicStroke scaleStroke(BasicStroke stroke) {
        return scaleStroke(stroke, getLineWidthScale());
    }

    public static BasicStroke scaleStroke(BasicStroke stroke, float scale) {
        if (stroke == null) {
            return null;
        }
        if (Math.abs(scale - 1.0f) < 0.0001f) {
            return stroke;
        }
        float width = Math.max(0.1f, stroke.getLineWidth() * scale);
        float[] dash = stroke.getDashArray();
        float[] scaledDash = null;
        if (dash != null) {
            scaledDash = new float[dash.length];
            for (int i = 0; i < dash.length; i++) {
                scaledDash[i] = dash[i] * scale;
            }
        }
        return new BasicStroke(width, stroke.getEndCap(), stroke.getLineJoin(),
                stroke.getMiterLimit(), scaledDash, stroke.getDashPhase() * scale);
    }

    private static Font deriveFont(Font base, int style, int size) {
        Font template = base == null ? new Font("SansSerif", style, size) : base;
        return template.deriveFont(style, (float) size);
    }

    private static void applyAxisDefaults(Axis axis, int labelFontSize, int tickFontSize) {
        if (axis == null) {
            return;
        }
        axis.setLabelFont(deriveFont(axis.getLabelFont(), Font.PLAIN, labelFontSize));
        axis.setTickLabelFont(deriveFont(axis.getTickLabelFont(), Font.PLAIN, tickFontSize));
    }

    public static void applyGraphicsPreferences(JFreeChart chart, GraphicsPreferences prefs) {
        if (chart == null || prefs == null) {
            return;
        }
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(deriveFont(chart.getTitle().getFont(), Font.BOLD, prefs.titleFontSize));
            chart.getTitle().setVisible(true);
        }
        for (Object subtitleObj : chart.getSubtitles()) {
            if (subtitleObj instanceof TextTitle) {
                TextTitle textTitle = (TextTitle) subtitleObj;
                textTitle.setFont(deriveFont(textTitle.getFont(), Font.BOLD, prefs.subtitleFontSize));
                textTitle.setVisible(true);
            }
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(deriveFont(chart.getLegend().getItemFont(), Font.PLAIN, prefs.legendFontSize));
        }
        Plot plot = chart.getPlot();
        if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            applyAxisDefaults(categoryPlot.getDomainAxis(), prefs.domainAxisLabelFontSize, prefs.domainAxisTickFontSize);
            applyAxisDefaults(categoryPlot.getRangeAxis(), prefs.rangeAxisLabelFontSize, prefs.rangeAxisTickFontSize);
        } else if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            applyAxisDefaults(xyPlot.getDomainAxis(), prefs.domainAxisLabelFontSize, prefs.domainAxisTickFontSize);
            applyAxisDefaults(xyPlot.getRangeAxis(), prefs.rangeAxisLabelFontSize, prefs.rangeAxisTickFontSize);
        } else if (plot instanceof PiePlot) {
            PiePlot piePlot = (PiePlot) plot;
            piePlot.setLabelFont(deriveFont(piePlot.getLabelFont(), Font.PLAIN, prefs.domainAxisLabelFontSize));
        }
    }

    public static void applyGraphicsDefaults(JFreeChart chart) {
        applyGraphicsPreferences(chart, getGraphicsPreferences());
    }

    public static void persistGraphicsPreferences(JFreeChart chart) {
        InterfaceMain instance = null;
        try {
            instance = InterfaceMain.getInstance();
        } catch (Throwable ignored) {
            // Preserve chart customization in headless or early-start contexts.
        }
        if (instance == null) {
            return;
        }
        GraphicsPreferences prefs = captureGraphicsPreferences(chart);
        instance.updateProperties(properties -> {
            properties.setProperty(InterfaceMain.GRAPHICS_TITLE_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.titleFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.subtitleFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.domainAxisLabelFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.domainAxisTickFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.rangeAxisLabelFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.rangeAxisTickFontSize));
            if (prefs.domainAxisLabelFontSize == prefs.rangeAxisLabelFontSize) {
                properties.setProperty(InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
                        Integer.toString(prefs.domainAxisLabelFontSize));
            }
            if (prefs.domainAxisTickFontSize == prefs.rangeAxisTickFontSize) {
                properties.setProperty(InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
                        Integer.toString(prefs.domainAxisTickFontSize));
            }
            properties.setProperty(InterfaceMain.GRAPHICS_LEGEND_FONT_SIZE_PROPERTY,
                    Integer.toString(prefs.legendFontSize));
            properties.setProperty(InterfaceMain.GRAPHICS_LINE_WIDTH_SCALE_PROPERTY,
                    Float.toString(prefs.lineWidthScale));
        });
    }

    /**
     * Creates a DefaultDrawingSupplier with custom colors and line strokes.
     * @param className Chart class name to determine supplier type
     * @param color Array of color values
     * @param ls Array of line stroke types
     * @return DefaultDrawingSupplier instance
     */
    public static DefaultDrawingSupplier setDrawingSupplier(String className, int[] color, int[] ls) {
        DefaultDrawingSupplier supplier;
        BasicStroke[] strokes = new BasicStroke[ls.length];
        Paint[] paints = new Paint[color.length];
        for (int i = 0; i < paints.length; i++) {
            paints[i] = new Color(color[i]);
        }
        for (int i = 0; i < strokes.length; i++) {
            strokes[i] = LegendUtil.getLineStroke(ls[i]);
        }
        if (className.contains("LineChart")) {
            supplier = new DefaultDrawingSupplier(
                paints,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                strokes,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
            );
        } else {
            supplier = new DefaultDrawingSupplier(
                paints,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                strokes,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
            );
        }
        return supplier;
    }

    /**
     * Creates a new instance of a class using reflection and constructor parameters.
     * @param t Class type
     * @param param Constructor parameters
     * @return New instance or null if instantiation fails
     */
    public static Object creatNewInstance(Class<?> t, Object[] param) {
        Object instance = null;
        Constructor<?>[] constructors = t.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 0) {
                try {
                    instance = constructor.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (paramTypes.length == param.length) {
                try {
                    instance = constructor.newInstance(param);
                } catch (InstantiationException | IllegalAccessException e) {
                    System.out.println("ChartUtil::creatNewInstance:" + e.getClass().getSimpleName() + ":" + e.getMessage());
                } catch (InvocationTargetException e) {
                    System.out.println("ChartUtil::creatNewInstance:InvocationTargetException:" + e.getTargetException().getMessage());
                }
                break;
            }
        }
        return instance;
    }

    /**
     * Finds the dataset and series index for a given key in a JFreeChart.
     * @param chart JFreeChart instance
     * @param key Series or row key
     * @return Array [datasetIndex, seriesIndex] or [-1, -1] if not found
     */
    public static int[] findDataset(JFreeChart chart, String key) {
        int[] datasetSeries = { -1, -1 };
        if (chart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    if (dataset.indexOf(key) != -1) {
                        datasetSeries[0] = i;
                        datasetSeries[1] = dataset.indexOf(key);
                        break;
                    }
                }
            }
        } else if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryPlot plot = chart.getCategoryPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                CategoryDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getRowCount(); j++) {
                    if ((datasetSeries[1] = dataset.getRowIndex(key.trim())) != -1) {
                        datasetSeries[0] = i;
                        break;
                    }
                }
            }
        }
        return datasetSeries;
    }

    /**
     * Finds the dataset and series index for a key in an array of XYDatasets.
     */
    public static int[] findDataset(XYDataset[] dataset, String key) {
        int[] datasetSeries = { -1, -1 };
        for (int i = 0; i < dataset.length; i++) {
            for (int j = 0; j < dataset[i].getSeriesCount(); j++) {
                if ((datasetSeries[1] = dataset[i].indexOf(key)) != -1) {
                    datasetSeries[0] = i;
                    break;
                }
            }
        }
        return datasetSeries;
    }

    /**
     * Finds the dataset and series index for a key in an array of CategoryDatasets.
     */
    public static int[] findDataset(CategoryDataset[] dataset, String key) {
        int[] datasetSeries = { -1, -1 };
        for (int i = 0; i < dataset.length; i++) {
            for (int j = 0; j < dataset[i].getRowCount(); j++) {
                if ((datasetSeries[1] = dataset[i].getRowIndex(key.trim())) != -1) {
                    datasetSeries[0] = i;
                    break;
                }
            }
        }
        return datasetSeries;
    }

    /**
     * Finds the series index for a key in an XYDataset.
     */
    public static int findSeries(XYDataset dataset, String key) {
        int series = 0;
        for (int j = 0; j < dataset.getSeriesCount(); j++) {
            if (dataset.indexOf(key) != -1) {
                series = dataset.indexOf(key);
                break;
            }
        }
        return series;
    }

    /**
     * Finds the series index for a key in a CategoryDataset.
     */
    public static int findSeries(CategoryDataset dataset, String key) {
        int series = 0;
        for (int j = 0; j < dataset.getRowCount(); j++) {
            if (dataset.getRowIndex(key) != -1) {
                series = dataset.getRowIndex(key);
                break;
            }
        }
        return series;
    }

    /**
     * Gets the indices of visible dataset series in a chart.
     * @param chart JFreeChart instance
     * @return Array of visible series indices
     */
    public static Integer[] getVisibleDatasetSeries(JFreeChart chart) {
        ArrayList<Integer> visibleSeries = new ArrayList<>();
        if (chart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    XYItemRenderer renderer = chart.getXYPlot().getRenderer(i);
                    if (renderer.getItemVisible(j, 0))
                        visibleSeries.add(j);
                }
            }
        } else if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryPlot plot = chart.getCategoryPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                CategoryDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getRowCount(); j++) {
                    CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer(i);
                    if (renderer.getItemVisible(j, 0))
                        visibleSeries.add(j);
                }
            }
        }
        return visibleSeries.toArray(new Integer[0]);
    }

    /**
     * Sets all visible dataset series in a chart to be visible in the legend.
     */
    public static void setVisibleDatasetSeries(JFreeChart chart) {
        if (chart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    XYItemRenderer renderer = chart.getXYPlot().getRenderer(i);
                    if (renderer.getSeriesVisible(j)) {
                        renderer.setSeriesVisible(j, true);
                        renderer.setSeriesVisibleInLegend(j, true);
                    }
                }
            }
        } else if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryPlot plot = chart.getCategoryPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                CategoryDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getRowCount(); j++) {
                    CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer(i);
                    if (renderer.getSeriesVisible(j)) {
                        renderer.setSeriesVisible(j, true);
                        renderer.setSeriesVisibleInLegend(j, true);
                    }
                }
            }
        }
    }

    /**
     * Sets all dataset series in a chart to be visible in the legend.
     */
    public static void setDatasetSeriesVisible(JFreeChart chart) {
        if (chart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    XYItemRenderer renderer = chart.getXYPlot().getRenderer(i);
                    renderer.setSeriesVisible(j, true);
                    renderer.setSeriesVisibleInLegend(j, true);
                }
            }
        } else if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryPlot plot = chart.getCategoryPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                CategoryDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getRowCount(); j++) {
                    CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer(i);
                    renderer.setSeriesVisible(j, true);
                    renderer.setSeriesVisibleInLegend(j, true);
                }
            }
        }
    }

    /**
     * Checks if any dataset series in a chart are visible.
     * @param chart JFreeChart instance
     * @return true if any series is visible, false otherwise
     */
    public static boolean isDatasetSeriesInvisible(JFreeChart chart) {
        boolean visible = false;
        if (chart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                XYDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getSeriesCount(); j++) {
                    XYItemRenderer renderer = chart.getXYPlot().getRenderer(i);
                    if (renderer.isSeriesVisible(j)) {
                        visible = true;
                        break;
                    }
                }
            }
        } else if (chart.getPlot().getPlotType().contains("Category")) {
            CategoryPlot plot = chart.getCategoryPlot();
            for (int i = 0; i < plot.getDatasetCount(); i++) {
                CategoryDataset dataset = plot.getDataset(i);
                for (int j = 0; j < dataset.getRowCount(); j++) {
                    CategoryItemRenderer renderer = chart.getCategoryPlot().getRenderer(i);
                    if (renderer.isSeriesVisible(j)) {
                        visible = true;
                        break;
                    }
                }
            }
        }
        return visible;
    }

    /**
     * Sets the chart title and subtitles.
     * @param chart JFreeChart instance
     * @param subTitle Array of subtitle strings
     */
    public static void setSubTitle(JFreeChart chart, String[] subTitle) {
        if (subTitle[0] != null)
            chart.setTitle(subTitle[0]);
        GraphicsPreferences prefs = getGraphicsPreferences();
        for (int i = 1; subTitle != null && i < subTitle.length; i++) {
            TextTitle title = new TextTitle(subTitle[i], new Font("SansSerif", Font.BOLD, prefs.subtitleFontSize));
            title.visible = true;
            chart.addSubtitle(i - 1, title);
        }
    }

    /**
     * Removes the chart title and subtitles.
     * @param chart JFreeChart instance
     * @param subTitle Array of subtitle strings
     */
    public static void removeSubTitle(JFreeChart chart, String[] subTitle) {
        if (subTitle[0] != null)
            chart.setTitle("");
        for (int i = 1; subTitle != null && i < subTitle.length; i++) {
            TextTitle title = new TextTitle(subTitle[i], new Font("SansSerif", Font.BOLD, 14));
            chart.removeSubtitle(title);
        }
    }

    /**
     * Gets the index of a subtitle in the chart.
     */
    public static int getSubTitleIndex(JFreeChart chart, Title subTitle) {
        return chart.getSubtitles().indexOf(subTitle);
    }

    /**
     * Creates a CategoryPointerAnnotation for a category chart.
     * @param text Annotation text
     * @param column Category column
     * @param yValue Y value for annotation
     * @return CategoryPointerAnnotation instance
     */
    public static CategoryPointerAnnotation createAnnotation(String text, String column, double yValue) {
        CategoryPointerAnnotation annotation = null;
        try {
            annotation = new CategoryPointerAnnotation(text, column.trim(), yValue, -2.356194490192345);
            annotation.setFont(new Font("SansSerif", Font.PLAIN, 10));
            annotation.setTextAnchor(TextAnchor.BOTTOM_LEFT);
        } catch (IllegalArgumentException e) {
            System.out.println("ChartUtil::CategoryPointerAnnotation:Apply Annotation Failed");
        }
        return annotation;
    }

    /**
     * Creates an XYPointerAnnotation for an XY chart.
     * @param text Annotation text
     * @param xValue X value
     * @param yValue Y value
     * @return XYPointerAnnotation instance
     */
    public static XYPointerAnnotation createAnnotation(String text, double xValue, double yValue) {
        if (debug)
            System.out.println("x: " + xValue + " y: " + yValue + " text: " + text);
        XYPointerAnnotation annotation = null;
        try {
            annotation = new XYPointerAnnotation(text, xValue, yValue, -0.78539816339744828D);
            annotation.setFont(new Font("SansSerif", Font.PLAIN, 10));
            annotation.setTextAnchor(TextAnchor.BOTTOM_LEFT);
        } catch (IllegalArgumentException e) {
            System.out.println("ChartUtil::CategoryPointerAnnotation:Apply Annotation Failed");
        }
        return annotation;
    }

    /**
     * Creates CategoryPointerAnnotations for all series in a category chart.
     */
    public static CategoryPointerAnnotation[] createAnnotation(String[] text, String[] column, String chartClassName, double[][] data) {
        CategoryPointerAnnotation[] annotations = new CategoryPointerAnnotation[text.length];
        double[] y;
        try {
            if (chartClassName.contains("Stacked"))
                y = getAnnotationTextTableLocation(data);
            else if (chartClassName.contains("Line"))
                y = getAnnotationTextMaxLocation(data);
            else {
                if (chartClassName.contains("3D"))
                    y = getAnnotationTextLocation(data[0], true);
                else
                    y = getAnnotationTextLocation(data[0], false);
            }
            for (int i = 0; i < text.length; i++) {
                Font font = new Font("SansSerif", Font.PLAIN, 9);
                annotations[i] = new CategoryPointerAnnotation(text[i], column[i].trim(), y[i], -2.356194490192345);
                annotations[i].setFont(font);
                annotations[i].setTextAnchor(TextAnchor.TOP_LEFT);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ChartUtil::CategoryPointerAnnotation:Apply Annotation Failed");
        }
        return annotations;
    }

    /**
     * Creates XYPointerAnnotations for all series in an XY chart.
     */
    public static XYPointerAnnotation[] createAnnotation(String[] text, String[] x, String[] column, String chartClassName, double[][] data) {
        XYPointerAnnotation[] annotations = new XYPointerAnnotation[text.length];
        double[] y = getAnnotationTextMaxLocation(data);
        try {
            for (int i = 0; i < text.length; i++) {
                Font font = new Font("SansSerif", Font.PLAIN, 9);
                if (text[i] == null)
                    text[i] = "";
                double temp = Double.parseDouble(x[i].trim());
                if (debug)
                    System.out.println("x: " + temp + " y: " + y[i] + " text: " + text);
                annotations[i] = new XYPointerAnnotation(column[i] + " " + text[i], temp, y[i], -2.356194490192345);
                annotations[i].setFont(font);
                annotations[i].setTextAnchor(TextAnchor.TOP_LEFT);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ChartUtil::CategoryPointerAnnotation:Apply Annotation Failed");
        }
        return annotations;
    }

    /**
     * Gets annotation locations for a data array (optionally for 3D line charts).
     */
    public static double[] getAnnotationTextLocation(double[] data, boolean line3D) {
        double[] annotationLoc = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            annotationLoc[i] = line3D ? data[i] * 1.1 : data[i];
        }
        return annotationLoc;
    }

    /**
     * Gets max value locations for annotation text in a 2D data array.
     */
    public static double[] getAnnotationTextMaxLocation(double[][] data) {
        double[] annotationLoc = new double[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double temp = 0.0;
            for (int j = 0; j < data.length; j++)
                temp = Math.max(temp, data[j][i]);
            annotationLoc[i] = temp;
        }
        return annotationLoc;
    }

    /**
     * Gets sum value locations for annotation text in a 2D data array.
     */
    public static double[] getAnnotationTextTableLocation(double[][] data) {
        double[] annotationLoc = new double[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double temp = 0.0;
            for (int j = 0; j < data.length; j++)
                temp += data[j][i];
            annotationLoc[i] = temp;
        }
        return annotationLoc;
    }

    /**
     * Gets sum value locations for annotation text in a 2D data array up to a given level.
     */
    public static double[] getAnnotationTextTableLocation(double[][] data, int level) {
        double[] annotationLoc = new double[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            double temp = 0.0;
            for (int j = 0; j <= level; j++)
                temp += data[j][i];
            annotationLoc[i] = temp;
        }
        return annotationLoc;
    }

    /**
     * Paints all series in a CategoryPlot with the given Paint array.
     */
    public static void paintSeries(CategoryPlot plot, Paint[] paint) {
        if (paint != null) {
            int k = 0;
            for (int j = plot.getRendererCount() - 1; j >= 0; j--) {
                AbstractRenderer renderer = (AbstractRenderer) plot.getRenderer(j);
                renderer.setAutoPopulateSeriesPaint(false);
                renderer.setAutoPopulateSeriesFillPaint(false);
                int count = plot.getDataset(j).getRowCount();
                for (int i = 0; i < count; i++)
                    renderer.setSeriesPaint(count - 1 - i, paint[k + i]);
                k += count;
            }
        }
    }

    /**
     * Paints a single series in a renderer.
     */
    public static void paintaSeries(AbstractRenderer renderer, int series, Paint paint) {
        if (paint != null)
            renderer.setSeriesPaint(series, paint);
    }

    /**
     * Paints all series in an XYPlot with the given Paint array.
     */
    public static void paintSeries(XYPlot plot, Paint[] paint) {
        if (paint != null) {
            int k = 0;
            for (int j = plot.getRendererCount() - 1; j >= 0; j--) {
                AbstractRenderer renderer = (AbstractRenderer) plot.getRenderer(j);
                renderer.setAutoPopulateSeriesPaint(false);
                renderer.setAutoPopulateSeriesFillPaint(false);
                int count = plot.getDataset(j).getSeriesCount();
                for (int i = 0; i < count; i++)
                    renderer.setSeriesPaint(count - 1 - i, paint[k + i]);
                k += count;
            }
        }
    }

    /**
     * Gets legend items from a chart.
     * @param chart JFreeChart instance
     * @return LegendItemCollection
     */
    public static LegendItemCollection getLegendItemsFromChart(JFreeChart chart) {
        if (chart.getPlot().getPlotType().contains("XY"))
            return chart.getXYPlot().getFixedLegendItems();
        else
            return chart.getCategoryPlot().getFixedLegendItems();
    }

    /**
     * Gets legend values from a LegendItemCollection.
     * @param graphName Name of the graph
     * @param legenditemcollection LegendItemCollection
     * @return ArrayList of legend values
     */
    public static ArrayList<Object[]> getLegendValueFromChart(String graphName, LegendItemCollection legenditemcollection) {
        ArrayList<Object[]> legendValue = new ArrayList<>();
        if (legenditemcollection != null) {
            Paint[] paint = new Paint[legenditemcollection.getItemCount()];
            String[] legend = new String[legenditemcollection.getItemCount()];
            Integer[] pattern = new Integer[legenditemcollection.getItemCount()];
            Iterator<?> k = legenditemcollection.iterator();
            int i = 0;
            while (k.hasNext()) {
                LegendItem l = (LegendItem) k.next();
                legend[i] = l.getLabel();
                paint[i] = l.getFillPaint();
                i++;
            }
            int[] temp = { 1, 1 };
            for (int j = 0; j < temp.length; j++)
                pattern[j] = temp[j];
            legendValue.add(legend);
            legendValue.add(paint);
            legendValue.add(pattern);
        }
        return legendValue;
    }

    /**
     * Finds the maximum value for chart range.
     * @param yAxis ValueAxis
     * @param curMax Current max value
     * @return Maximum value
     */
    public static double findRangeMaxForCharts(ValueAxis yAxis, double curMax) {
        return Math.max(yAxis.getUpperBound(), curMax);
    }

    /**
     * Finds the minimum value for chart range.
     * @param yAxis ValueAxis
     * @param curMin Current min value
     * @return Minimum value
     */
    public static double findRangeMinForCharts(ValueAxis yAxis, double curMin) {
        return Math.min(yAxis.getUpperBound(), curMin);
    }
}