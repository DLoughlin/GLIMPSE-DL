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
    private JPanel jp;
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
        // Immediate UI response: set a placeholder panel with inline status and Cancel button
        // Use an empty placeholder panel while thumbnails are generated; hide status/progress UI for now.
        placeholderPanel.setLayout(new BorderLayout());
        // Intentionally do not add a status label, progress bar, or cancel button to keep the UI minimal.
        jp = placeholderPanel;
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
                final Object[] colDataHolder = new Object[1];
                final Object[] data0Holder = new Object[1];
                final Object[] data1Holder = new Object[1];

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

                        colDataHolder[0] = ModelInterfaceUtil.getColDataFromTable(
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
                        colDataHolder[0],
                        col,
                        data0Holder[0],
                        effectiveMeta,
                        ModelInterfaceUtil.getLegend2(effectiveMeta, data1Holder[0]),
                        path,
                        metaCol,
                        unitLookup);
                return chart;
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        LOGGER.log(Level.INFO, "Thumbnail generation cancelled.");
                        jp = new JPanel();
                    } else {
                        Chart[] chart = get();
                        final int idx = ThumbnailUtilNew.getFirstNonNullChart(chart);
                        if (idx != -1 && chart[idx] != null) {
                            // Must update Swing components on EDT
                            SwingUtilities.invokeLater(() -> {
                                JPanel chartPanel = ThumbnailUtilNew.setChartPane(chart, idx, false, true, sp);
                                if (chartPanel != null) jp = chartPanel;
                                // Replace right component of split pane with generated panel
                                Component currentRight = sp.getRightComponent();
                                boolean isExistingGraph = false;
                                int currentWidth = 0;
                                if (currentRight instanceof JComponent) {
                                    Boolean prop = (Boolean) ((JComponent) currentRight).getClientProperty("isGraph");
                                    isExistingGraph = prop != null && prop;
                                    currentWidth = currentRight.getWidth();
                                }
                                if (currentRight != null) sp.remove(currentRight);
                                sp.setRightComponent(jp);
                                if (!isExistingGraph || currentWidth < 50) {
                                    sp.setDividerLocation(0.678);
                                }
                                sp.updateUI();
                            });
                        } else {
                            LOGGER.log(Level.WARNING, "No valid chart found for thumbnail creation.");
                            jp = new JPanel();
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Thumbnail generation failed.", ex);
                    jp = new JPanel();
                } finally {
                    // Reset cursor and log memory; placeholder component stays in the right pane and
                    // will be replaced by the generated panel above when ready.
                    sp.setCursor(defaultCursor);
                    logDebugMemory();
                }
             }
         };
         worker.execute();
         // placeholderPanel already shown by caller; worker is running and placeholder displays progress.
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
     * Returns the JPanel containing the chart thumbnails.
     * @return JPanel with chart thumbnails
     */
    public JPanel getJp() {
        return jp;
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