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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;

import listener.ListMouseListener;
import ModelInterface.InterfaceMain;

/**
 * Handles creation, editing, and removal of Markers for JFreeChart.
 * Provides UI dialogs for marker operations and manages marker state.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class ChartMarker {
    // Marker position options
    private final String[] pos = { "Bottom", "Bottom-Left", "Bottom-Right", "Center", "Left", "Right", "Top",
            "Top-Left", "Top-Right" };
    // Marker text position options
    private final String[] textPos = { "Baseline", "Baseline-Left", "Baseline-Right", "Bottom", "Bottom-Left",
            "Bottom-Right", "Half-Ascent-Center", "Half-Ascent-Left", "Half-Ascent-Right", "Center", "Center-Left",
            "Center-Right", "Top-Center", "Top-Left", "Top-Right" };
    // Marker operation types
    private final String markerType[] = { "X-Axis Marker", "Y-Axis Marker", "Interval Marker", "Edit Marker",
            "Remove Marker" };
    // Value options for marker value selection
    private final String valueOpt[] = { "", "Max", "Min", "Averge" };
    private String selectedMarkerType;
    private Paint selectedMarkerColor = null;
    private double selectedMarkerValue = -99999;
    private Chart chart;
    private JFreeChart jfchart;
    private Map<String, Marker> markerMap;
    private JDialog dialog; // Parent dialog
    private JDialog dialog1;

    /**
     * Constructs a ChartMarker for the given chart and parent dialog.
     * @param chart Chart object to operate on
     * @param dialog Parent dialog for UI
     */
    public ChartMarker(Chart chart, JDialog dialog) {
        this.dialog = dialog;
        this.chart = chart;
        this.jfchart = chart.getChart();
        this.markerMap = chart.getMarkerMap();
        selectMarkerType();
    }

    private Component getDialogParent() {
        if (dialog != null) {
            return dialog;
        }
        try {
            return InterfaceMain.getInstance().getFrame();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Displays dialog to select marker operation type.
     */
    protected void selectMarkerType() {
        JList<String> list = new JList<>(markerType);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    selectedMarkerType = list.getSelectedValue();
                }
            }
        });
        list.addMouseListener(new ListMouseListener(list));
        list.setSelectedIndex(0);
        JScrollPane jsp = new JScrollPane(list);
        JPanel jp = new JPanel(new BorderLayout());
        jp.add(jsp, BorderLayout.CENTER);

        String[] options = { "Apply", "Save", "Done" };
        Box box = Box.createHorizontalBox();
        box.add(Box.createVerticalStrut(30));
        for (int i = 0; i < options.length; i++) {
            box.add(crtJButton(options[i], i));
        }
        box.add(Box.createVerticalStrut(30));
        jp.add(box, BorderLayout.SOUTH);

        dialog1 = new JDialog(dialog);
        dialog1.setTitle("Perform Marker Operation");
        dialog1.setContentPane(jp);
        dialog1.setSize(new Dimension(200, 160));
        dialog1.setResizable(true);
        dialog1.setVisible(true);
    }

    /**
     * Creates a JButton for marker operation dialog.
     * @param name Button name
     * @param i Button index
     * @return JButton instance
     */
    private JButton crtJButton(String name, int i) {
        JButton jb = new JButton(name);
        jb.setName(name);
        jb.setToolTipText(String.valueOf(i));
        jb.addActionListener(e -> {
          JButton jb1 = (JButton) e.getSource();
          if (jb1.getName().equals("Apply")) {
            doApply();
            chart.setMarkerMap(markerMap);
          } else if (jb1.getName().equals("Save")) {
            JOptionPane.showMessageDialog(getDialogParent(), "Not implemented yet", "Information",
                JOptionPane.INFORMATION_MESSAGE);
          } else if (jb1.getName().equals("Done")) {
            dialog1.dispose();
          }
        });
        return jb;
    }

    /**
     * Executes the selected marker operation.
     */
    private void doApply() {
        if (selectedMarkerType != null) {
            switch (selectedMarkerType) {
                case "X-Axis Marker":
                    createCategoryMarker();
                    break;
                case "Y-Axis Marker":
                    createValueMarker();
                    break;
                case "Interval Marker":
                    createIntervalMarker();
                    break;
                case "Edit Marker":
                    editMarker();
                    break;
                case "Remove Marker":
                    removeMarker();
                    break;
            }
        }
    }

    /**
     * Creates a category marker (X-Axis or value marker for XY plot).
     */
    private void createCategoryMarker() {
        String selectedCategory = selectCategoryList();
        Object o = null;
        if (selectedCategory != null)
            o = selectMarkerColor();

        if (o != null && o.equals("Ok") && selectedMarkerColor != null) {
            try {
                if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
                    CategoryMarker categoryMarker = new CategoryMarker(selectedCategory, selectedMarkerColor, new BasicStroke(2.0F));
                    int optionSelected = getMarkerLabel(categoryMarker);
                    if (optionSelected == 0) {
                        categoryMarker.setDrawAsLine(true);
                        categoryMarker.setLabelAnchor(RectangleAnchor.CENTER);
                        categoryMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                        jfchart.getCategoryPlot().addDomainMarker(categoryMarker);
                    }
                    markerMap.put(selectedMarkerType + "_" + categoryMarker.getLabel(), categoryMarker);
                } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
                    ValueMarker valueMarker = new ValueMarker(
                            Double.valueOf(selectedCategory.substring(0, selectedCategory.lastIndexOf('.'))),
                            selectedMarkerColor, new BasicStroke(2.0F));
                    int optionSelected = getMarkerLabel(valueMarker);
                    if (optionSelected == 0) {
                        valueMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                        valueMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                        jfchart.getXYPlot().addDomainMarker(valueMarker);
                    }
                    markerMap.put(selectedMarkerType + "_" + valueMarker.getLabel(), valueMarker);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Apply Chart Marker Failed");
            }
        }
    }

    /**
     * Creates an interval marker.
     */
    private void createIntervalMarker() {
        Object o = selectMarkerColor();
        if (o == null || !o.equals("Ok") || selectedMarkerColor == null)
            return;
        o = selectMarkerValue("Input a Start value");
        double start = -99999;
        if (o != null && o.equals("Ok")) {
            start = selectedMarkerValue;
            if (Double.valueOf(start) != -99999)
                selectMarkerValue("Input a End value");
            else {
                JOptionPane.showMessageDialog(getDialogParent(), "Invalid data Inputted", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        double end = -99999;
        if (o != null && o.equals("Ok")) {
            end = selectedMarkerValue;
            if (Double.valueOf(start) != -99999) {
                IntervalMarker intervalMarker = new IntervalMarker(start, end, selectedMarkerColor,
                        new BasicStroke(1.0F), selectedMarkerColor, new BasicStroke(1.0F), 0.5f);
                int optionSelected = getMarkerLabel(intervalMarker);
                if (optionSelected == 0) {
                    intervalMarker.setLabelAnchor(RectangleAnchor.CENTER);
                    intervalMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                    if (jfchart.getPlot().getPlotType().contains("Category"))
                        jfchart.getCategoryPlot().addRangeMarker(intervalMarker);
                    else if (jfchart.getPlot().getPlotType().contains("XY"))
                        jfchart.getXYPlot().addRangeMarker(intervalMarker);
                }
                markerMap.put(selectedMarkerType + "_" + intervalMarker.getLabel(), intervalMarker);
            } else {
                JOptionPane.showMessageDialog(getDialogParent(), "Invalid data Inputted", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Creates a value marker (Y-Axis marker).
     */
    private void createValueMarker() {
        Object o = selectMarkerColor();
        if (o == null || !o.equals("Ok") || selectedMarkerColor == null)
            return;
        o = selectMarkerValue("Input a Start value");
        if (o != null && o.equals("Ok")) {
            if (Double.valueOf(selectedMarkerValue) != -99999) {
                ValueMarker valueMarker = new ValueMarker(selectedMarkerValue, selectedMarkerColor,
                        new BasicStroke(2.0F));
                int optionSelected = getMarkerLabel(valueMarker);
                if (optionSelected == 0) {
                    valueMarker.setLabelAnchor(RectangleAnchor.CENTER);
                    valueMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                    if (jfchart.getPlot().getPlotType().contains("Category"))
                        jfchart.getCategoryPlot().addRangeMarker(valueMarker);
                    else if (jfchart.getPlot().getPlotType().contains("XY"))
                        jfchart.getXYPlot().addRangeMarker(valueMarker);
                }
                markerMap.put(selectedMarkerType + "_" + valueMarker.getLabel(), valueMarker);
            } else {
                JOptionPane.showMessageDialog(getDialogParent(), "Invalid data Inputted", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Edits an existing marker's label and position.
     */
    private void editMarker() {
        if (markerMap == null || markerMap.isEmpty()) {
            JOptionPane.showMessageDialog(getDialogParent(), "No Marker to Edit", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] keys = markerMap.keySet().toArray(new String[0]);
        String selectedMarker = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Category",
                JOptionPane.INFORMATION_MESSAGE, null, keys, keys[0]);
        Marker m = markerMap.get(selectedMarker);
        if (JOptionPane.showConfirmDialog(getDialogParent(), "choose one", "Modify Label?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            getMarkerLabel(m);
        if (JOptionPane.showConfirmDialog(getDialogParent(), "choose one", "Modify Label Position?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            getMarkerLabelPosition(m);
        if (JOptionPane.showConfirmDialog(getDialogParent(), "choose one", "Modify Text Label Position?",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            getMarkerTextLabelPosition(m);
    }

    /**
     * Removes a marker from the chart and marker map.
     */
    private void removeMarker() {
        if (markerMap == null || markerMap.isEmpty()) {
            JOptionPane.showMessageDialog(getDialogParent(), "No Marker to Remove", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] keys = markerMap.keySet().toArray(new String[0]);
        String selectedMarker = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Category",
                JOptionPane.INFORMATION_MESSAGE, null, keys, keys[0]);
        Marker m = markerMap.get(selectedMarker);
        String plotType = jfchart.getPlot().getPlotType();
        if (plotType.equalsIgnoreCase("Category Plot")) {
            if (selectedMarker.contains("X-Axis") || selectedMarker.contains("Interval") || selectedMarker.contains("Y-Axis"))
                jfchart.getCategoryPlot().removeRangeMarker(m);
        } else if (plotType.equalsIgnoreCase("XY Plot")) {
            if (selectedMarker.contains("X-Axis"))
                jfchart.getXYPlot().removeDomainMarker(m);
            else if (selectedMarker.contains("Y-Axis") || selectedMarker.contains("Interval"))
                jfchart.getXYPlot().removeRangeMarker(m);
        }
        markerMap.remove(selectedMarker);
    }

    /**
     * Displays dialog to select a category from the chart dataset.
     * @return Selected category as String
     */
    protected String selectCategoryList() {
        List<?> l = null;
        if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot")) {
            CategoryPlot plot = jfchart.getCategoryPlot();
            l = plot.getDataset().getColumnKeys();
        } else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot")) {
            XYPlot plot = jfchart.getXYPlot();
            String[] x = new String[plot.getDataset().getItemCount(0)];
            for (int i = 0; i < plot.getDataset().getItemCount(0); i++) {
                x[i] = String.valueOf(plot.getDataset().getX(0, i));
            }
            l = Arrays.asList(x);
        }
        String[] data = conversionUtil.ArrayConversion.list2Array(l);
        return (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Category, Then Click OK",
                JOptionPane.INFORMATION_MESSAGE, null, data, data[0]);
    }

    /**
     * Displays color chooser dialog for marker color selection.
     * @return Selected option ("Ok" or "Cancel")
     */
    protected Object selectMarkerColor() {
        final JColorChooser tcc = new JColorChooser();
        tcc.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                selectedMarkerColor = tcc.getColor();
            }
        });
        tcc.setBorder(BorderFactory.createTitledBorder("Choose Marker Color"));
        Object[] options = { "Ok", "Cancel" };
        JOptionPane pane = new JOptionPane(tcc, -1, 0, null, options, options[0]);
        JDialog dialog = pane.createDialog(getDialogParent(), "Select a Marker Paint, Then Click OK");
        dialog.setLayout(null);
        dialog.setResizable(true);
        dialog.setVisible(true);
        return pane.getValue();
    }

    /**
     * Displays dialog to select or input marker value.
     * @param name Dialog title
     * @return Selected option ("Ok" or "Cancel")
     */
    protected Object selectMarkerValue(String name) {
        JComboBox<String> valueList = new JComboBox<>(valueOpt);
        valueList.setEditable(true);
        valueList.setSelectedIndex(0);
        valueList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = valueList.getSelectedIndex();
                String s = (String) valueList.getSelectedItem();
                if (selected == -1) {
                    if (s.trim().equals("") || Double.valueOf(s.trim()).isNaN()) {
                        JOptionPane.showMessageDialog(getDialogParent(), "Invalid data Inputted", "Information",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        selectedMarkerValue = Double.valueOf(s).doubleValue();
                    }
                } else {
                    selectedMarkerValue = Double.valueOf(getTheValue(selected)).doubleValue();
                }
            }
        });
        Object[] options = { "Ok", "Cancel" };
        JOptionPane pane = new JOptionPane(valueList, -1, 0, null, options, options[0]);
        JDialog dialog = pane.createDialog(getDialogParent(), name);
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> valueList.requestFocusInWindow());
            }
        });
        dialog.setFocusable(true);
        dialog.setLayout(null);
        dialog.setResizable(true);
        dialog.setVisible(true);
        return pane.getValue();
    }

    /**
     * Gets the value for marker based on selected index.
     * @param idx Index in valueOpt
     * @return Value as int
     */
    private int getTheValue(int idx) {
        int value = 0;
        switch (idx) {
            case 1: // Max
                if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot"))
                    value = (int) jfchart.getCategoryPlot().getRangeAxis().getUpperBound();
                else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot"))
                    value = (int) jfchart.getXYPlot().getRangeAxis().getUpperBound();
                break;
            case 2: // Min
                if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot"))
                    value = (int) jfchart.getCategoryPlot().getRangeAxis().getLowerBound();
                else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot"))
                    value = (int) jfchart.getXYPlot().getRangeAxis().getLowerBound();
                break;
            case 3: // Average
                if (jfchart.getPlot().getPlotType().equalsIgnoreCase("Category Plot"))
                    value = ((int) jfchart.getCategoryPlot().getRangeAxis().getUpperBound()
                            - (int) jfchart.getCategoryPlot().getRangeAxis().getLowerBound()) / 2;
                else if (jfchart.getPlot().getPlotType().equalsIgnoreCase("XY Plot"))
                    value = ((int) jfchart.getXYPlot().getRangeAxis().getUpperBound()
                            - (int) jfchart.getXYPlot().getRangeAxis().getLowerBound()) / 2;
                break;
        }
        return value;
    }

    /**
     * Displays dialog to input marker label and sets it on the marker.
     * @param m Marker to label
     * @return JOptionPane option selected
     */
    protected int getMarkerLabel(final Marker m) {
        JOptionPane pane = new JOptionPane("Please Enter Label String", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        pane.setWantsInput(true);
        pane.setInitialSelectionValue("");
        JDialog dialog = pane.createDialog(getDialogParent(), "Input Marker Label");
        dialog.setVisible(true);
        String markerLabel = ((String) pane.getInputValue()).trim();
        if ((int) pane.getValue() == JOptionPane.CANCEL_OPTION)
            return JOptionPane.CANCEL_OPTION;
        else if ((int) pane.getValue() == JOptionPane.OK_OPTION) {
            if (markerLabel.equals("")) {
                JOptionPane.showMessageDialog(getDialogParent(), "No Label Inputted", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                return JOptionPane.CANCEL_OPTION;
            } else {
                m.setLabel(markerLabel);
                m.setLabelFont(new Font("Verdana", Font.BOLD, 12));
                return JOptionPane.OK_OPTION;
            }
        } else
            return JOptionPane.CANCEL_OPTION;
    }

    /**
     * Returns the marker map.
     * @return Map of marker names to Marker objects
     */
    public Map<String, Marker> getMarkerMap() {
        return markerMap;
    }

    /**
     * Displays dialog to select marker label position.
     * @param m Marker to update
     */
    protected void getMarkerLabelPosition(final Marker m) {
        String selectedPos = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Category",
                JOptionPane.INFORMATION_MESSAGE, null, pos, pos[0]);
        m.setLabelOffset(new RectangleInsets(0, 16, 0, 16));
        m.setLabelAnchor(MarkerUtil.getMarkerLabelPosition(selectedPos));
    }

    /**
     * Displays dialog to select marker text label position.
     * @param m Marker to update
     */
    protected void getMarkerTextLabelPosition(final Marker m) {
        String selectedTextPos = (String) JOptionPane.showInputDialog(getDialogParent(), "Choose one", "Select a Category",
                JOptionPane.INFORMATION_MESSAGE, null, textPos, textPos[0]);
        m.setLabelTextAnchor(MarkerUtil.getMarkerTextLabelPosition(selectedTextPos));
    }

    /**
     * Returns the JFreeChart instance.
     * @return JFreeChart
     */
    public JFreeChart getJfchart() {
        return jfchart;
    }
}
