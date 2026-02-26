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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import chart.Chart;
import conversionUtil.ArrayConversion;
import ModelInterface.InterfaceMain;

/**
 * The class to handle multiple charts displaying with added on functions.
 * Provides thumbnail chart creation and display from table data and metadata.
 * Handles chart pane setup and unit lookup for enhanced chart display.
 *
 * @author TWU
 */
public class Thumbnail {
    private static final Logger LOGGER = Logger.getLogger(Thumbnail.class.getName());
    private static final int DEFAULT_CURSOR_TYPE = Cursor.DEFAULT_CURSOR;
    private static final int WAIT_CURSOR_TYPE = Cursor.WAIT_CURSOR;
    private boolean debug = false;
    private final BorderLayout containerLayout = new BorderLayout();
    private final JPanel containerPanel = new JPanel(containerLayout);
    private final JPanel placeholderPanel = new JPanel();
    private SwingWorker<Chart[], Void> worker;
    private final Cursor waitCursor = new Cursor(WAIT_CURSOR_TYPE);
    private final Cursor defaultCursor = new Cursor(DEFAULT_CURSOR_TYPE);
    private HashMap<String, String> unitLookup;

    /**
     * Constructs a Thumbnail object and creates thumbnail charts from table data.
     * Sets up the chart pane in the provided JSplitPane.
     *
     * @param chartName the name of a JFreeChart (not null)
     * @param unit the unit label of the chart (not null)
     * @param path the legend property file (nullable)
     * @param cnt index for table data extraction
     * @param jtable table data including meta, column, row names, and values
     * @param metaMap map of metadata keys to row indices
     * @param sp JSplitPane for chart pane display
     * @param unitLookup lookup for units
     * @throws IllegalArgumentException if required arguments are null
     */
    public Thumbnail(String chartName, String[] unit, String path, int cnt, JTable jtable,
            Map<String, Integer[]> metaMap, JSplitPane sp, HashMap<String, String> unitLookup) {
        Objects.requireNonNull(chartName, "chartName must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        Objects.requireNonNull(jtable, "jtable must not be null");
        Objects.requireNonNull(sp, "JSplitPane must not be null");

        placeholderPanel.setLayout(new BorderLayout());
        // Add placeholder inside the stable container; containerPanel is the fixed reference
        // returned by getJp() so callers never hold a stale reference.
        containerPanel.add(placeholderPanel, BorderLayout.CENTER);

        // Ensure a stable right component while work is in progress.
        // Some call paths create the Thumbnail and then later update the split pane;
        // installing the placeholder immediately avoids transient UI states.
        final Component currentRight = sp.getRightComponent();
        if (currentRight != containerPanel) {
            SwingUtilities.invokeLater(() -> {
                if (sp.getRightComponent() != containerPanel) {
                    sp.setRightComponent(containerPanel);
                    sp.revalidate();
                    sp.repaint();
                }
            });
        }

        sp.setCursor(waitCursor);
        this.unitLookup = unitLookup;

        // Run chart data creation in background to avoid blocking the EDT.
        final Map<String, Integer[]> metaMapFinal = metaMap; // capture for worker
        worker = new SwingWorker<Chart[], Void>() {
            private String metaCol;
            private String col;

            @Override
            protected Chart[] doInBackground() throws Exception {
                // Snapshot all JTable-dependent data on the EDT to avoid accessing Swing components
                // from the background thread.
                final Map<String, Integer[]>[] effectiveMetaHolder = new Map[1];
                final String[] metaColHolder = new String[1];
                final String[] colHolder = new String[1];
                final String[][][] data1Holder = new String[1][][];
                final String[][][] data0Holder = new String[1][][];
                final String[][] colUnitsHolder = new String[1][];

                // invokeAndWait is intentionally used here (not invokeLater) because the
                // background thread must capture a consistent snapshot of all JTable-derived
                // data before the heavy chart-creation work begins. Using invokeLater would
                // allow the background thread to proceed with stale or partially-updated
                // data. This is safe as long as no EDT thread is blocking on this worker
                // (which would cause a deadlock). Callers must never hold the EDT lock while
                // waiting for this SwingWorker to complete.
                assert !SwingUtilities.isEventDispatchThread()
                        : "Thumbnail SwingWorker must not run on the EDT; "
                        + "invokeAndWait below would deadlock if it did.";
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Integer[]> effectiveMeta = metaMapFinal;
                        if (effectiveMeta == null) {
                            effectiveMeta = ModelInterfaceUtil.getMetaIndex2(jtable, cnt);
                        }
                        effectiveMetaHolder[0] = effectiveMeta;

                        metaColHolder[0] = ArrayConversion.array2String(
                                ModelInterfaceUtil.getColumnFromTable(jtable, cnt, 2));
                        colHolder[0] = ArrayConversion.array2String(
                                ModelInterfaceUtil.getColumnFromTable(jtable, cnt, 0));

                        colUnitsHolder[0] = ModelInterfaceUtil.getColDataFromTable(
                                jtable, jtable.getColumnCount() - 1);
                        data0Holder[0] = ModelInterfaceUtil.getDataFromTable(jtable, cnt, 0);
                        data1Holder[0] = ModelInterfaceUtil.getDataFromTable(jtable, cnt, 1);
                    }
                });

                // Copy snapshot values into instance fields to preserve existing behavior.
                metaCol = metaColHolder[0];
                col = colHolder[0];
                Map<String, Integer[]> effectiveMeta = effectiveMetaHolder[0];

                // Heavy work: create Chart objects using only snapshot data (no Swing access here).
                Chart[] chart = ThumbnailUtilNew.createChart(
                        chartName,
                        unit,
                        colUnitsHolder[0],
                        col,
                        data0Holder[0],
                        effectiveMeta,
                        ModelInterfaceUtil.getLegend2(effectiveMeta, data1Holder[0]),
                        path,
                        metaCol,
                        unitLookup);

                // If the background thread was interrupted during chart creation, propagate cancellation
                // so that done() follows the cancellation path instead of displaying an "aborted" chart.
                if (Thread.currentThread().isInterrupted() || isCancelled()) {
                    LOGGER.log(Level.INFO, "Thumbnail generation interrupted during chart creation; cancelling worker.");
                    cancel(true);
                    return null;
                }
                return chart;
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        LOGGER.log(Level.INFO, "Thumbnail generation cancelled.");
                        // Leave containerPanel as-is (showing placeholder); no jp reassignment needed.
                    } else {
                        Chart[] chart = get();
                        final int idx = ThumbnailUtilNew.getFirstNonNullChart(chart);
                        if (idx != -1 && chart[idx] != null) {
                            // Must update Swing components on EDT
                            SwingUtilities.invokeLater(() -> {
                                JPanel chartPanel = ThumbnailUtilNew.setChartPane(chart, idx, false, true, sp);
                                if (chartPanel != null) {
                                    // Check if container already holds a graph (for divider logic).
                                    // containerPanel is always the split pane's right component, so
                                    // inspect its center child rather than sp.getRightComponent().
                                    Component currentChild = containerLayout.getLayoutComponent(BorderLayout.CENTER);
                                    boolean isExistingGraph = currentChild instanceof JComponent
                                            && Boolean.TRUE.equals(((JComponent) currentChild).getClientProperty("isGraph"));
                                    int currentWidth = containerPanel.getWidth();

                                    // Update contents in-place; containerPanel reference stays stable
                                    containerPanel.removeAll();
                                    containerPanel.add(chartPanel, BorderLayout.CENTER);
                                    // YD, 2/26/2026, align breakout window with the main window
									chartPanel.setLocation(InterfaceMain.getInstance().getFrame().getLocation());
                                    containerPanel.revalidate();
                                    containerPanel.repaint();

                                    // Ensure the split pane still points to our stable container
                                    if (sp.getRightComponent() != containerPanel) {
                                        sp.setRightComponent(containerPanel);
                                    }
                                    if (!isExistingGraph || currentWidth < 50) {
                                        sp.setDividerLocation(0.678);
                                    }
                                    sp.updateUI();
                                }
                            });
                        } else {
                            LOGGER.log(Level.WARNING, "No valid chart found for thumbnail creation.");
                            // Leave containerPanel as-is (showing placeholder).
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Thumbnail generation failed.", ex);
                    // Leave containerPanel as-is (showing placeholder).
                } finally {
                    // Reset cursor and log memory; placeholder component stays in the right pane and
                    // will be replaced by the generated panel above when ready.
                    sp.setCursor(defaultCursor);
                    logDebugMemory();
                }
             }
         };
         worker.execute();
         // containerPanel (with placeholder) is already the right component; worker updates its contents when done.
     }

    /**
     * Cancel thumbnail generation if it's still running.
     */
    public void cancelGeneration() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    /**
     * Returns the stable JPanel container for the chart thumbnails.
     * The returned panel is a fixed reference; its contents are updated in-place
     * when the background worker completes, so callers always hold a valid reference.
     * @return JPanel container with chart thumbnails
     */
    public JPanel getJp() {
        return containerPanel;
    }

    /**
     * Logs debug memory information if debug is enabled.
     */
    private void logDebugMemory() {
        if (debug) {
            LOGGER.log(Level.INFO, String.format("Thumbnail::Thumbnail:max memory %d total: %d free: %d",
                    Runtime.getRuntime().maxMemory(),
                    Runtime.getRuntime().totalMemory(),
                    Runtime.getRuntime().freeMemory()));
        }
    }
}
