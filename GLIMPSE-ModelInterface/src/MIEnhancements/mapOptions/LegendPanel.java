package mapOptions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.UIManager;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;

/**
 * LegendPanel displays a color legend for map visualizations using JFreeChart.
 * It creates a paint scale and axis based on the provided MapColor and units.
 */
public class LegendPanel extends JComponent {
    private static final long serialVersionUID = -9100503749455967320L;
    private PaintScaleLegend legend;
    private boolean debug = false;
    private static final Font LEGEND_LABEL_FONT = resolveUiFont(Font.PLAIN, 13);
    private static final Font LEGEND_TICK_FONT = resolveUiFont(Font.PLAIN, 12);

    /**
     * Constructs a LegendPanel for the given MapColor and units.
     * @param mapColor the color mapping for the legend
     * @param units the units label for the axis
     */
    public LegendPanel(MapColor mapColor, String units) {
        initLegend(mapColor, units);
    }

    /**
     * Paints the legend component.
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        Graphics2D g2d = (Graphics2D) g.create();
        java.awt.geom.Rectangle2D area = new java.awt.geom.Rectangle2D.Double(0, 0, getWidth(), getHeight());
        legend.draw(g2d, area);
        g2d.dispose();
    }

    /**
     * Returns the preferred size of the legend panel.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150, 145);
    }

    /**
     * Returns the minimum size of the legend panel.
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(130, 130);
    }

    /**
     * Initializes the legend and axis based on the MapColor and units.
     * @param mapColor the color mapping
     * @param units the units label
     */
    public void initLegend(MapColor mapColor, String units) {
        LookupPaintScale scale = null;
        NumberAxis scaleAxis = new NumberAxis(units);
        scaleAxis.setTickMarkPaint(Color.BLACK);
        scaleAxis.setTickMarkStroke(new BasicStroke(1));
        scaleAxis.setTickLabelFont(LEGEND_TICK_FONT);
        scaleAxis.setLabelFont(LEGEND_LABEL_FONT);

        try {
            // Calculate interval, min, and max for the legend
            double interval = mapColor.getIntervalStart(1) - mapColor.getIntervalStart(0);
            double min = mapColor.getIntervalStart(0);
            double max = mapColor.getIntervalStart(mapColor.getColorCount() - 1) + interval;
            scale = createPaintScale(mapColor, min, max);

            // Configure axis ticks based on min/max values
            if (min > 0 || max < 0) {
                TickUnitSource mySource = scaleAxis.createStandardTickUnits();
                scaleAxis.setStandardTickUnits(mySource);
            } else {
                NumberTickUnit myTickUnit = new NumberTickUnit(interval);
                scaleAxis.setAutoRange(false);
                scaleAxis.setTickUnit(myTickUnit);
                scaleAxis.setMinorTickMarksVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create or update the legend
        if (legend == null) {
            legend = new PaintScaleLegend(scale, scaleAxis);
            legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
            legend.setAxisOffset(0);
            legend.setMargin(new RectangleInsets(2, 2, 2, 2));
            legend.setFrame(new BlockBorder(new Color(180, 180, 180)));
            legend.setPadding(new RectangleInsets(6, 6, 6, 6));
            legend.setStripWidth(72);
            legend.setPosition(RectangleEdge.RIGHT);
            legend.setBackgroundPaint(Color.WHITE);
        } else {
            legend.setScale(scale);
            legend.setAxis(scaleAxis);
        }
    }

    /**
     * Returns the PaintScaleLegend instance.
     * @return the legend
     */
    public PaintScaleLegend getLegend() {
        if (debug)
            System.out.println("in LegendPanel.getLegend");
        return legend;
    }

    /**
     * Creates a LookupPaintScale for the legend from the MapColor.
     * @param mapColor the color mapping
     * @param min the minimum value
     * @param max the maximum value
     * @return the paint scale
     */
    protected LookupPaintScale createPaintScale(MapColor mapColor, double min, double max) {
        int colorCount = mapColor.getColorCount();
        LookupPaintScale paintScale = new LookupPaintScale(min, max, Color.GRAY);
        double interval = (max - min) / colorCount;
        if (max == min) {
            paintScale.add(min, mapColor.getColor(0));
        } else {
            // Add each color to the scale at the correct interval
            for (int i = 0; i < colorCount; i++) {
                double scale = min + (i * interval);
                paintScale.add(scale, mapColor.getColor(i));
            }
        }
        return paintScale;
    }

    private static Font resolveUiFont(int style, int fallbackSize) {
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            return new Font("Dialog", style, fallbackSize);
        }
        return baseFont.deriveFont(style, fallbackSize);
    }
}