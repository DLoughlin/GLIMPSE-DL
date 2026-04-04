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
package listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import chart.Chart;
import chartOptions.SetModifyChanges;
import ModelInterface.InterfaceMain;

/**
 * Handles popup menu for toggling line and shape options on charts.
 * <p>
 * Provides options for displaying lines with or without shapes on supported chart types.
 * </p>
 * <p>
 * Usage: Attach to a JTextField to show popup on mouse events.
 * </p>
 *
 * Author: TWU
 * Date: 1/2/2016
 */
public class LineAndShapePopup implements ActionListener {

    /** Mouse listener for showing the popup menu. */
    private MouseListener mouseListener;
    /** True if both line and shape should be shown. */
    private boolean lineAndShape;
    /** Reference to the chart being modified. */
    private Chart chart;
    /** The popup menu instance. */
    public JPopupMenu popup;

    /**
     * Constructs a LineAndShapePopup and attaches it to the given JTextField.
     *
     * @param jtf   JTextField to attach the popup to
     * @param chart Chart to modify
     */
    public LineAndShapePopup(JTextField jtf, Chart chart) {
        this.chart = chart;
        popup = new JPopupMenu();
        popup.add(createMenuItem("Line and Shape"));
        popup.add(createMenuItem("Line without Shape"));
        mouseListener = new JPopupMenuShower(popup);
        jtf.addMouseListener(mouseListener);
    }

    /**
     * Returns true if both line and shape are selected.
     *
     * @return true if line and shape, false otherwise
     */
    public boolean isLineAndShape() {
        return lineAndShape;
    }

    /**
     * Creates a menu item with the given name and attaches this as its action listener.
     *
     * @param name Name of the menu item
     * @return JMenuItem instance
     */
    private JMenuItem createMenuItem(String name) {
        JMenuItem menuItem = new JMenuItem(name);
        menuItem.addActionListener(this);
        return menuItem;
    }

    /**
     * Handles menu item selection events.
     *
     * @param e ActionEvent triggered by menu item selection
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Check for supported chart type: Category
        if (chart.getChart().getPlot().getPlotType().contains("Category")) {
            if (!(chart.getChart().getCategoryPlot().getRenderer() instanceof LineAndShapeRenderer)) {
                JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Support for Line Chart", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        // Check for supported chart type: XY
        if (chart.getChart().getPlot().getPlotType().contains("XY")) {
            if (!(chart.getChart().getXYPlot().getRenderer() instanceof XYLineAndShapeRenderer)) {
                JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Support for Line Chart", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        // Determine which menu item was selected
        JMenuItem source = (JMenuItem) e.getSource();
        lineAndShape = source.getText().equalsIgnoreCase("Line and Shape");
        // Update chart display and notify changes
        chart.setShowLineAndShape(lineAndShape);
        SetModifyChanges.setLineAndShapeChanges(chart.getChart(), lineAndShape);
    }
}