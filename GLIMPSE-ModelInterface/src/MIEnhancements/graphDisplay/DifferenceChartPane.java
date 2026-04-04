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
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import chart.Chart;
import chart.DatasetUtil;
import chart.MyChartFactory;
import chart.MyDataset;
import ModelInterface.InterfaceMain;

/**
 * Handles the difference of two charts by subtracting one chart's data from another's.
 * Missing attributes are assumed to be zero. Displays a chart panel for comparison.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class DifferenceChartPane extends JPanel {
    private static final long serialVersionUID = 1L;
    private Chart charts[];
    private Chart chart;
    private int selectedIndex;
    private int selected[] = { -1, -1 };
    private List<String> rowList = new LinkedList<String>();
    private String[] legend = null;
    private int[] color = null;
    private int[] pColor = null;
    private int[] pattern = null;
    private int[] lineStrokes = null;
    boolean shouldStop = false;

      private java.awt.Component getDialogParent() {
        try {
          return InterfaceMain.getInstance().getFrame();
        } catch (Exception ignored) {
          return null;
        }
      }

    /**
     * Constructs a DifferenceChartPane for comparing two charts.
     * @param charts Array of Chart objects to compare
     * @throws ClassNotFoundException if chart class not found
     */
    public DifferenceChartPane(Chart charts[]) throws ClassNotFoundException {
        selectedIndex = -1;
        this.charts = charts.clone();
        init();
        if (shouldStop) {
            return;
        }
        createDifferenceChart();
    }

    /**
     * Initializes the selection dialogs for choosing charts to compare.
     */
    private void init() {
        setLayout(new BorderLayout());
        JList jc = difference();
        Object options[] = { "Ok", "Cancel" };
        boolean madeValidSelection = false;

        // First chart selection dialog
        while (!madeValidSelection) {
            JOptionPane pane0 = new JOptionPane(new JScrollPane(jc), -1, 0, null, options, options[0]);
                  JDialog dialog = pane0.createDialog(getDialogParent(), "Select first chart to compare");
            dialog.setPreferredSize(new Dimension(600, 200));
            dialog.setLayout(null);
            dialog.setResizable(true);
            dialog.setVisible(true);
            if (pane0.getValue() == null || pane0.getValue().toString().compareToIgnoreCase("cancel") == 0) {
                // User closed window or hit cancel
                shouldStop = true;
                return;
            }
            // Check if a valid selection was made
            if (selectedIndex != -1) {
                selected[0] = selectedIndex;
                dialog.dispose();
                selectedIndex = -1;
                madeValidSelection = true;
                dialog.dispose();
            } else {
                        JOptionPane.showMessageDialog(getDialogParent(), "Please make a selection before pressing OK, or cancel to exit.", "Additional Selection Required", JOptionPane.ERROR_MESSAGE);
            }
        }

        jc.clearSelection();
        madeValidSelection = false;

        // Second chart selection dialog
        while (!madeValidSelection) {
            JOptionPane pane0 = new JOptionPane(new JScrollPane(jc), -1, 0, null, options, options[0]);
                  JDialog dialog = pane0.createDialog(getDialogParent(), "Select second chart to compare");
            dialog.setPreferredSize(new Dimension(600, 200));
            dialog.setLayout(null);
            dialog.setResizable(true);
            dialog.setVisible(true);
            if (pane0.getValue() == null || pane0.getValue().toString().compareToIgnoreCase("cancel") == 0) {
                // User closed window or hit cancel
                shouldStop = true;
                return;
            }
            // Check if a valid selection was made
            if (selectedIndex != -1) {
                selected[1] = selectedIndex;
                dialog.dispose();
                madeValidSelection = true;
                dialog.dispose();
            } else {
                        JOptionPane.showMessageDialog(getDialogParent(), "Please make a selection before pressing OK, or cancel to exit.", "Additional Selection Required", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Creates the difference chart based on selected charts.
     */
    private void createDifferenceChart() {
        String[][] data = null;
        // Error if selections are not made
        if (shouldStop || selected[0] == -1 || selected[1] == -1) {
                  JOptionPane.showMessageDialog(getDialogParent(), "Both dialog windows must have a selection for comparison tool to work", "Additional Selections Required", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String id = charts[selected[0]].getMeta() + "|" + charts[selected[0]].getMetaCol() + ","
                + charts[selected[1]].getMeta() + "|" + charts[selected[1]].getMetaCol();

        if (selected[0] != -1 && selected[1] != -1) {
            try {
                // Only CategoryPlot supported
                if (charts[selected[0]].getChart().getPlot() instanceof CategoryPlot) {
                    DefaultCategoryDataset ds0 = (DefaultCategoryDataset) charts[selected[0]].getChart()
                            .getCategoryPlot().getDataset();
                    DefaultCategoryDataset ds1 = (DefaultCategoryDataset) charts[selected[1]].getChart()
                            .getCategoryPlot().getDataset();

                    // Collect all row keys from both datasets
                    for (int i = 0; i < ds0.getRowCount(); i++)
                        rowList.add(((String) ds0.getRowKey(i)).trim());
                    for (int i = 0; i < ds1.getRowCount(); i++)
                        if (!rowList.contains(((String) ds1.getRowKey(i))) )
                            rowList.add(((String) ds1.getRowKey(i)).trim());

                    String[] ks = rowList.toArray(new String[0]);
                    List<String> l1 = ds0.getRowKeys();
                    List<String> l2 = ds1.getRowKeys();
                    fillLegends(ks, l1.toArray(new String[0]), l2.toArray(new String[0]));

                    // Get difference data
                    data = DatasetUtil.getDiffData(charts[selected[0]].getChart().getCategoryPlot().getDataset(),
                            charts[selected[1]].getChart().getCategoryPlot().getDataset(), rowList);

                    DefaultCategoryDataset dataset = new MyDataset().createCategoryDataset(data, ks,
                            charts[selected[0]].getChartColumn().split(","));

                    String title = id;
                    String subtitle[] = getsubTitle();
                    for (int i = 0; i < subtitle.length; i++) {
                        if (subtitle[i] != null) {
                            subtitle[i] = subtitle[i].replaceAll("\nregion:", "");
                        }
                    }

                    chart = MyChartFactory.createChart(charts[selected[0]].getChartClassName(),
                            charts[selected[0]].getPath(),
                            "Difference: " + charts[selected[0]].getGraphName(),
                            title, subtitle, charts[selected[0]].getAxis_name_unit(),
                            Arrays.toString(legend).replace("[", "").replace("]", ""), color, pColor, pattern,
                            lineStrokes, null, dataset, charts[selected[0]].getRelativeColIndex(),
                            charts[selected[0]].isShowLineAndShape(), "");
                    chart.setUnitsLookup(charts[selected[0]].getUnitsLookup());
                }
                // Set subtitle font and visibility
                for (int j = 0; j < chart.getChart().getSubtitleCount()
                        && !(chart.getChart().getSubtitle(j) instanceof org.jfree.chart.title.LegendTitle); j++) {
                    ((TextTitle) chart.getChart().getSubtitle(j)).setFont(new Font("Arial", 1, 12));
                    chart.getChart().getSubtitle(j).setVisible(true);
                }
            } catch (ClassNotFoundException e) {
                System.out.println("Experiencing ClassNotFoundException in creating DifferencePlot!");
            }
        }
    }

    /**
     * Fills legend, color, pattern, and line stroke arrays for the difference chart.
     * @param ks All row keys
     * @param l1 Row keys from first chart
     * @param l2 Row keys from second chart
     */
    private void fillLegends(String[] ks, String[] l1, String[] l2) {
        legend = new String[ks.length];
        color = new int[ks.length];
        pColor = new int[ks.length];
        pattern = new int[ks.length];
        lineStrokes = new int[ks.length];
        int idx = 0;
        int i1 = 0;
        for (int i = 0; i < ks.length; i++) {
            if (Arrays.asList(l1).contains(ks[i]) && i1 < l1.length) {
                int j = Arrays.asList(l1).indexOf(ks[i].trim());
                legend[idx] = charts[selected[0]].getLegend().split(",")[j];
                color[idx] = charts[selected[0]].getColor()[j];
                pColor[idx] = charts[selected[0]].getpColor()[j];
                pattern[idx] = charts[selected[0]].getPattern()[j];
                lineStrokes[idx] = charts[selected[0]].getLineStrokes()[j];
                i1++;
                idx++;
            } else if (Arrays.asList(l2).contains(ks[i]) && i1 < ks.length) {
                int j = Arrays.asList(l2).indexOf(ks[i]);
                legend[idx] = charts[selected[1]].getLegend().split(",")[j];
                color[idx] = charts[selected[1]].getColor()[j];
                pColor[idx] = charts[selected[1]].getpColor()[j];
                pattern[idx] = charts[selected[1]].getPattern()[j];
                lineStrokes[idx] = charts[selected[1]].getLineStrokes()[j];
                idx++;
            } else {
                System.out.println("error: " + ks[i]);
            }
        }
    }

    /**
     * Creates a JList for chart selection, with a listener to track selection.
     * @return JList for chart selection
     */
    private JList<Object> difference() {
        JList<Object> list = GraphDisplayUtil.metaList(charts);
        list.setSelectionMode(0);
        ListSelectionListener listener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList<?> list = (JList<?>) e.getSource();
                boolean adjust = e.getValueIsAdjusting();
                selectedIndex = -1;
                if (!adjust && list.getSelectedValue() != null)
                    selectedIndex = Integer.valueOf(((String) list.getSelectedValue()).split(",")[1]);
            }
        };
        list.addListSelectionListener(listener);
        return list;
    }

    /**
     * Generates subtitles for the difference chart.
     * @return Array of subtitle strings
     */
    private String[] getsubTitle() {
        String[] st = new String[charts[selected[0]].getTitles().length];
        for (int i = 1; i < charts[selected[0]].getTitles().length; i++) {
            st[i] = charts[selected[0]].getTitles()[i] + " minus " + charts[selected[1]].getTitles()[i];
        }
        return st;
    }

    /**
     * Returns the generated difference chart.
     * @return Chart object
     */
    public Chart getChart() {
        return chart;
    }
}