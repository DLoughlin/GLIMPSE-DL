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
package filter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import chart.LegendUtil;

/**
 * TristateCheckBox is a custom JCheckBox supporting three states:
 * selected, unselected, and half-selected (partial selection).
 * Used for tree structures where a branch can be partially selected.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class TristateCheckBox extends JCheckBox {

     private static final long serialVersionUID = 1L;
      /** Indicates if the checkbox is in the half-selected state. */
      private boolean halfState;
      /** Icon for half-selected state. */
      private static final Icon halfselected = new ImageIcon(LegendUtil.getBufferedImage(Color.white, Color.black, 14));
      /** Icon for unselected state. */
      private static final Icon unselected = new ImageIcon(LegendUtil.getBufferedImage(Color.white, Color.black, 14));

     /**
      * Constructs a TristateCheckBox with fixed size constraints.
      */
     public TristateCheckBox() {
         super();
         // Set consistent sizes to prevent resizing when state changes
         Dimension size = new Dimension(20, 20);
         setPreferredSize(size);
         setMinimumSize(size);
         setMaximumSize(size);
         setSize(size);
     }

    /**
     * Custom paint method to set the icon based on the current state.
     * @param g the Graphics context
     */
    @Override
    public void paint(Graphics g) {
        // If selected, ensure halfState is false
        if (isSelected()) {
            halfState = false;
        }
        // Set icon based on state
        setIcon(halfState ? halfselected : isSelected() ? super.getSelectedIcon() : unselected);
        super.paint(g);
    }

    /**
     * Returns true if the checkbox is half-selected.
     * @return boolean half-selected state
     */
    public boolean isHalfSelected() {
        return halfState;
    }

    /**
     * Sets the half-selected state. If set to true, also sets selected to false and repaints.
     * @param halfState true to set half-selected, false otherwise
     */
    public void setHalfSelected(boolean halfState) {
        this.halfState = halfState;
        if (halfState) {
            setSelected(false);
            repaint();
        }
    }
}