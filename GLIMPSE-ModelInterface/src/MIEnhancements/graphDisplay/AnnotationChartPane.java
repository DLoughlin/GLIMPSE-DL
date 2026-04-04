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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.annotations.CategoryPointerAnnotation;
import org.jfree.chart.annotations.TextAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import chart.Chart;
import chart.ChartUtil;
import chart.DatasetUtil;
import ModelInterface.InterfaceMain;

/**
 * Handles adding and removing annotations on category or XY charts.
 * Supports interactive annotation management via dialog and table UI.
 *
 * Author: TWU
 * Date: 1/2/2016
 */
public class AnnotationChartPane {

    /** Row keys for chart data */
    private String[] rowKeys = null;
    /** Column keys for chart data */
    private String[] columnKeys = null;
    /** Chart values for annotation placement */
    private double[][] value = null;
    /** Chart object to annotate */
    private JFreeChart jfchart;
    /** Chart wrapper */
    private Chart chart;
    /** Dialog for annotation UI */
    private JDialog dialog = null;
    /** Table for annotation text input */
    private JTable table;

    private java.awt.Component getDialogParent() {
        try {
            return InterfaceMain.getInstance().getFrame();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Constructor: prompts user to add or remove annotation, then acts accordingly.
     * @param chart Chart object to annotate
     */
    public AnnotationChartPane(Chart chart) {
        this.chart = chart;
        jfchart = chart.getChart();
        String[] data = { "Add", "Remove" };
        String action = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select an Action",
                JOptionPane.INFORMATION_MESSAGE, null, data, data[0]);

        if ("Add".equals(action))
            addChartAnnotation();
        else
            removeChartAnnotation();
    }

    /**
     * Removes a selected annotation from the chart.
     */
    private void removeChartAnnotation() {
        String[] name = null;
        String selected = null;
        TextAnnotation[] annotation = null;

        if (jfchart.getPlot().getPlotType().contains("Category")) {
            if (JOptionPane.showConfirmDialog(getDialogParent(), "choose one", "Select an Annotation?",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                int n = jfchart.getCategoryPlot().getAnnotations().size();
                annotation = new TextAnnotation[n];
                name = new String[n];

                Iterator<?> it = jfchart.getCategoryPlot().getAnnotations().iterator();
                int i = 0;
                while (it.hasNext()) {
                    annotation[i] = (TextAnnotation) it.next();
                    name[i] = annotation[i].getText();
                    i++;
                }

                selected = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select an Annotation",
                        JOptionPane.INFORMATION_MESSAGE, null, name, name[0]);
            }
            // Remove the selected annotation
            int idx = Arrays.asList(name).indexOf(selected);
            jfchart.getCategoryPlot().removeAnnotation((CategoryAnnotation) annotation[idx]);
        }
    }

    /**
     * Adds annotation(s) to the chart using a table UI for text input.
     */
    private void addChartAnnotation() {
        rowKeys = chart.getChartRow().split(",");
        columnKeys = chart.getChartColumn().split(",");
        // Prepare value matrix for annotation placement
        if (jfchart.getPlot().getPlotType().contains("Category")) {
            DefaultCategoryDataset ds = (DefaultCategoryDataset) jfchart.getCategoryPlot().getDataset();
            value = new double[rowKeys.length][columnKeys.length];
            for (int i = 0; i < rowKeys.length; i++)
                for (int j = 0; j < columnKeys.length; j++)
                    value[i][j] = (double) ds.getValue(i, j);
        } else if (jfchart.getPlot().getPlotType().contains("XY")) {
            XYPlot plot = jfchart.getXYPlot();
            value = DatasetUtil.getYValues(plot);
        }

        // Create annotation table UI
        table = setAnnotationTable(rowKeys, columnKeys);
        table.setMaximumSize(new Dimension(1000, 600));
        table.setMinimumSize(new Dimension(400, 200));
        JScrollPane jsp = new JScrollPane(table);
        JPanel jp = new JPanel(new BorderLayout());
        jp.add(jsp, BorderLayout.CENTER);

        // Add buttons for annotation actions
        String[] options = { "Apply", "Save", "Done" };
        JButton jb;
        Box box = Box.createHorizontalBox();
        box.add(Box.createVerticalStrut(30));
        for (int i = 0; i < options.length; i++) {
            jb = crtJButton(options[i], i);
            box.add(jb);
        }
        box.add(Box.createVerticalStrut(30));
        jp.add(box, BorderLayout.SOUTH);

        // Show dialog for annotation input
        dialog = new JDialog(InterfaceMain.getInstance().getFrame(), "Input Annotation Text", false);
        dialog.setContentPane(jp);
        dialog.setSize(new Dimension(600, 200));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(getDialogParent());
        dialog.setVisible(true);
    }

    /**
     * Creates a JButton for annotation actions and attaches mouse listener.
     * @param name Button name
     * @param i Button index
     * @return Configured JButton
     */
    private JButton crtJButton(String name, int i) {
        JButton jb = new JButton(name);
        jb.setName(name);
        jb.setToolTipText(String.valueOf(i));
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JButton jb1 = (JButton) e.getSource();
                if (e.getClickCount() > 0) {
                    if (jb1.getName().equals("Apply")) {
                        doApply();
                        buildAnnotationText();
                    } else if (jb1.getName().equals("Save")) {
                        JOptionPane.showMessageDialog(getDialogParent(), "Not implement yet", "Information",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (jb1.getName().equals("Done")) {
                        dialog.dispose();
                    }
                }
            }
        };
        jb.addMouseListener(ml);
        return jb;
    }

    /**
     * Applies annotation(s) to the chart based on table input.
     */
    private void doApply() {
        double[] y = null;
        for (int i = 0; i < table.getRowCount(); i++) {
            try {
                if (jfchart.getPlot().getPlotType().contains("Category")) {
                    // Use correct renderer for annotation placement
                    if (jfchart.getCategoryPlot().getRenderer() instanceof StackedAreaRenderer
                            || jfchart.getCategoryPlot().getRenderer() instanceof StackedBarRenderer) {
                        y = ChartUtil.getAnnotationTextTableLocation(value, i);
                    } else {
                        y = ChartUtil.getAnnotationTextLocation(value[i], false);
                    }
                    // Add annotation for each non-empty cell
                    for (int j = 1; j < table.getColumnCount(); j++) {
                        if (!table.getValueAt(i, j).equals("")) {
                            CategoryPointerAnnotation categoryPointerAnnotation = ChartUtil.createAnnotation(
                                    (String) table.getValueAt(i, j), (String) columnKeys[j - 1], y[j - 1]);
                            jfchart.getCategoryPlot().addAnnotation(categoryPointerAnnotation);
                        }
                    }
                } else {
                    y = ChartUtil.getAnnotationTextLocation(value[i], false);
                    for (int j = 1; j < table.getColumnCount(); j++) {
                        if (!table.getValueAt(i, j).equals("")) {
                            XYPointerAnnotation xyPointerAnnotation = ChartUtil
                                    .createAnnotation((String) table.getValueAt(i, j), 
                                    Double.valueOf(columnKeys[j - 1].trim()).doubleValue(), y[j - 1]);
                            jfchart.getXYPlot().addAnnotation(xyPointerAnnotation);
                        }
                    }
                }
            } catch (java.lang.IllegalArgumentException e) {
                System.out.println("Apply Annotation Failed");
            }
        }
    }

    /**
     * Creates the annotation table for user input.
     * @param rowKeys Row keys for table
     * @param columnKeys Column keys for table
     * @return Configured JTable
     */
    private JTable setAnnotationTable(String[] rowKeys, String[] columnKeys) {
        String[][] annotationText = new String[rowKeys.length][columnKeys.length + 1];
        String[] tableCol = new String[columnKeys.length + 1];
        tableCol[0] = "";

        for (int i = 0; i < annotationText.length; i++) {
            annotationText[i][0] = rowKeys[i];
            for (int j = 0; j < columnKeys.length; j++)
                annotationText[i][j + 1] = "";
        }
        for (int i = 0; i < columnKeys.length; i++) {
            tableCol[i + 1] = columnKeys[i];
        }

        JTable table = new JTable(annotationText, tableCol);
        TableColumnModel cmodel = table.getColumnModel();
        cmodel.getColumn(0).setWidth(180);
        for (int i = 0; i < annotationText.length; i++) {
            table.setEditingRow(i);
            for (int j = 0; j < columnKeys.length; j++) {
                table.setEditingColumn(j + 1);
            }
        }
        return table;
    }

    /**
     * Stores annotation text from table into chart object.
     */
    public void buildAnnotationText() {
        String[][] annotationText = new String[table.getRowCount()][table.getColumnCount()];
        for (int i = 0; i < table.getRowCount(); i++)
            for (int j = 0; j < table.getColumnCount(); j++)
                annotationText[i][j] = (String) table.getValueAt(i, j);
        chart.setAnnotationText(annotationText);
    }

    /**
     * Returns the chart object with annotations.
     * @return JFreeChart with annotations
     */
    public JFreeChart getJfchart() {
        return jfchart;
    }
}