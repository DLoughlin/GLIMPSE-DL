package chart;

import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Creates a 100% stacked bar chart where each category column sums to 1.0.
 */
public class CategoryStackedPercentBarChart extends CategoryChart {

    public CategoryStackedPercentBarChart(String path, String graphName, String meta,
            String[] titles, String[] axis_name_unit, String legend, int[] color, int[] pColor,
            int[] pattern, int[] lineStrokes, String[][] annotationText,
            DefaultCategoryDataset dataset, int relativeColIndex, boolean ShowLineAndShape, String graphType) {
        super(path, graphName, meta, titles, axis_name_unit, legend, color, pColor,
                pattern, lineStrokes, annotationText, dataset,
                relativeColIndex, ShowLineAndShape, graphType);
        chartClassName = "chart.CategoryStackedPercentBarChart";
        crtChart();
    }

    public CategoryStackedPercentBarChart(String path, String graphName, String meta, String[] titles,
            String[] axis_name_unit, String legend, String column, String[][] annotationText,
            String[][] data, int relativeColIndex) {
        super(path, graphName, meta, titles, axis_name_unit, legend, column, annotationText,
                data, relativeColIndex);
        chartClassName = "chart.CategoryStackedPercentBarChart";
        crtChart();
    }

    private void crtChart() {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        BarRenderer.setDefaultBarPainter(new StandardBarPainter());

        chart = ChartFactory.createStackedBarChart("", verifyAxisName_unit(0),
                verifyAxisName_unit(1), dataset, PlotOrientation.VERTICAL,
                true, true, false);
        plot = (CategoryPlot) chart.getPlot();
        plot.setDataset(0, dataset);

        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        plot.setRenderer(0, renderer);

        setPlotProperty();
        setLegendProperty();
        setAxisProperty();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 1.0);
        rangeAxis.setNumberFormatOverride(NumberFormat.getPercentInstance());

        RendererUtil.setRendererProperty(renderer);
        renderer.setRenderAsPercentages(true);
        renderer.setShadowVisible(false);
        renderer.setDefaultToolTipGenerator(new PercentValueToolTipGenerator());
        setChartProperty();
    }

    private static final class PercentValueToolTipGenerator extends StandardCategoryToolTipGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public String generateToolTip(CategoryDataset ds, int row, int column) {
            Number valueObj = ds.getValue(row, column);
            double value = valueObj == null ? 0.0 : valueObj.doubleValue();
            double columnTotal = 0.0;
            for (int r = 0; r < ds.getRowCount(); r++) {
                Number n = ds.getValue(r, column);
                if (n != null) {
                    columnTotal += n.doubleValue();
                }
            }
            double pct = columnTotal == 0.0 ? 0.0 : value / columnTotal;
            NumberFormat pctFmt = NumberFormat.getPercentInstance();
            NumberFormat valFmt = NumberFormat.getNumberInstance();
            valFmt.setMaximumFractionDigits(6);
            return "(" + ds.getRowKey(row) + ", " + ds.getColumnKey(column) + ") = "
                    + pctFmt.format(pct) + " (val: " + valFmt.format(value) + ")";
        }
    }
}
