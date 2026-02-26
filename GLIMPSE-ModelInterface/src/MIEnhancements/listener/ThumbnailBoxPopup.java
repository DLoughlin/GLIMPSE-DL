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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import chart.Chart;
import graphDisplay.AChartDisplay;
import graphDisplay.DifferenceChartPane;
import graphDisplay.Breakout;
import java.util.function.Consumer;
import java.awt.Component;
import javax.swing.JPanel;

/**
 * Handles Thumbnail Box Popup events for chart panels.
 * Provides menu options for chart operations such as Difference and Transpose.
 * Each menu item triggers the corresponding chart operation and displays the result.
 *
 * Author Action Date Flag
 * ======================================================================= 
 * TWU    created 1/2/2016
 */
public class ThumbnailBoxPopup extends JPopupMenu implements ActionListener {

    private static final long serialVersionUID = 1L;
    /** Menu options for chart operations */
    private final String[] menuOptions = { "Difference", "Transpose", "Breakout" };
    private Chart[] charts;
    private int thumbnailWidth;
    private int gridWidth;
    private boolean useSameScale;
    private JSplitPane splitPane;
    private Runnable refreshAction;
    private JCheckBoxMenuItem sameScaleMenuItem;
    private boolean hideOptions;
    private Consumer<Boolean> onSameScaleChange;

    /**
     * Constructs the popup menu for thumbnail chart operations.
     * @param charts Array of Chart objects
     * @param thumbnailWidth Width of chart thumbnails
     * @param gridWidth Number of columns in grid
     * @param useSameScale Whether to use same scale for all charts
     * @param splitPane JSplitPane containing chart panel
     */
    public ThumbnailBoxPopup(Chart[] charts, int thumbnailWidth, int gridWidth, boolean useSameScale, JSplitPane splitPane) {
        this(charts, thumbnailWidth, gridWidth, useSameScale, splitPane, null, false, null);
    }

    /**
     * Constructs the popup menu for thumbnail chart operations, with refresh capability.
     * @param charts Array of Chart objects
     * @param thumbnailWidth Width of chart thumbnails
     * @param gridWidth Number of columns in grid
     * @param useSameScale Whether to use same scale for all charts
     * @param splitPane JSplitPane containing chart panel
     * @param refreshAction Action to run when Refresh is clicked
     */
    public ThumbnailBoxPopup(Chart[] charts, int thumbnailWidth, int gridWidth, boolean useSameScale, JSplitPane splitPane, Runnable refreshAction) {
        this(charts, thumbnailWidth, gridWidth, useSameScale, splitPane, refreshAction, false, null);
    }

    public ThumbnailBoxPopup(Chart[] charts, int thumbnailWidth, int gridWidth, boolean useSameScale, JSplitPane splitPane, Runnable refreshAction, boolean hideOptions) {
        this(charts, thumbnailWidth, gridWidth, useSameScale, splitPane, refreshAction, hideOptions, null);
    }

    public ThumbnailBoxPopup(Chart[] charts, int thumbnailWidth, int gridWidth, boolean useSameScale, JSplitPane splitPane, Runnable refreshAction, boolean hideOptions, Consumer<Boolean> onSameScaleChange) {
        this.charts = charts;
        this.thumbnailWidth = thumbnailWidth;
        this.gridWidth = gridWidth;
        this.useSameScale = useSameScale;
        this.splitPane = splitPane;
        this.refreshAction = refreshAction;
        this.hideOptions = hideOptions;
        this.onSameScaleChange = onSameScaleChange;
        createMenuItems();
    }

    /**
     * Creates menu items for each chart operation and adds listeners.
     */
    private void createMenuItems() {
        for (String option : menuOptions) {
            if (hideOptions && (option.equals("Transpose") || option.equals("Breakout"))) {
                continue;
            }
            JMenuItem menuItem = new JMenuItem(option);
            menuItem.addActionListener(this);
            this.add(menuItem);
        }
        
        sameScaleMenuItem = new JCheckBoxMenuItem("Same Scale");
        sameScaleMenuItem.setSelected(useSameScale);
        sameScaleMenuItem.addActionListener(this);
        this.add(sameScaleMenuItem);
        
        if (refreshAction != null) {
            JMenuItem refreshItem = new JMenuItem("Refresh");
            refreshItem.addActionListener(this);
            this.add(refreshItem);
        }
    }

    /**
     * Handles menu item selection and triggers the corresponding chart operation.
     * Displays the result in a new chart panel or window.
     * @param e ActionEvent triggered by menu item selection
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if ("Refresh".equals(e.getActionCommand())) {
            if (refreshAction != null) {
                refreshAction.run();
            }
        } else if (e.getSource() == sameScaleMenuItem) {
            useSameScale = sameScaleMenuItem.isSelected();
            if (onSameScaleChange != null) {
                onSameScaleChange.accept(useSameScale);
            }
            if (refreshAction != null) {
                refreshAction.run();
            }
        } else {
            JMenuItem source = (JMenuItem) e.getSource();
            try {
                if (charts != null) {
                    String selected = source.getText();
                    if (selected.equalsIgnoreCase("Difference")) {
                        // Show difference chart
                        new AChartDisplay(new DifferenceChartPane(charts).getChart());
                    } else if (selected.equalsIgnoreCase("Transpose")) {
                        // Show transposed chart
                        new Breakout(charts.clone(), thumbnailWidth, gridWidth, useSameScale, splitPane, false);
                    } else if (selected.equalsIgnoreCase("Breakout")) {
                        // Show breakout chart
                        new Breakout(charts.clone(), thumbnailWidth, gridWidth, useSameScale, splitPane, true);
                        // After showing the breakout dialog, clear thumbnails from the query tab's right component
                        try {
                            if (splitPane != null) {
                                // Remove the right component entirely so the left component can fill the area
                                Component currentRight = splitPane.getRightComponent();
                                if (currentRight != null) {
                                    try {
                                        splitPane.remove(currentRight);
                                    } catch (Exception ignore) {
                                    }
                                }
                                // Ensure no right component remains
                                splitPane.setRightComponent(null);
                                // Move divider fully to the right so left component occupies full width
                                try {
                                    splitPane.setDividerLocation(1.0);
                                } catch (Exception ignore) {
                                    // Fallback: set to max integer
                                    splitPane.setDividerLocation(splitPane.getWidth());
                                }
                                splitPane.revalidate();
                                splitPane.repaint();
                            }
                        } catch (Exception ex) {
                            // Non-fatal: log or ignore to avoid breaking the popup action
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (ClassNotFoundException | NullPointerException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                System.out.println("Other error!");
                ex.printStackTrace();
            }
        }
        this.setVisible(false); // Hide popup after action
    }

}