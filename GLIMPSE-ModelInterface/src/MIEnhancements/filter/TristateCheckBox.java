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
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.UIManager;

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
      /** Cache icons by size so partial state matches active LAF sizing. */
      private static final Map<Integer, Icon> halfselectedIcons = new HashMap<Integer, Icon>();

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
        // Half-selected takes precedence and is always mutually exclusive with checked.
        if (halfState) {
            setSelected(false);
            setIcon(getHalfSelectedIcon(resolveNativeIconSize()));
        } else {
            // Use LAF icons for the checked/unchecked appearance (e.g., blue check).
            setIcon(null);
        }
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
        }
        repaint();
    }

    private Icon getHalfSelectedIcon(int iconSize) {
        synchronized (halfselectedIcons) {
            Icon icon = halfselectedIcons.get(iconSize);
            if (icon == null) {
                icon = createHalfSelectedIcon(iconSize);
                halfselectedIcons.put(iconSize, icon);
            }
            return icon;
        }
    }

    private int resolveNativeIconSize() {
        Icon lafIcon = UIManager.getIcon("CheckBox.icon");
        if (lafIcon != null) {
            int width = lafIcon.getIconWidth();
            int height = lafIcon.getIconHeight();
            if (width > 0 && height > 0) {
                return Math.max(12, Math.min(width, height));
            }
        }
        int fallback = Math.min(getWidth(), getHeight());
        return fallback > 0 ? Math.max(12, fallback - 6) : 14;
    }

    private static Icon createHalfSelectedIcon(int iconSize) {
        BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.white);
        g2.fillRect(1, 1, iconSize - 2, iconSize - 2);
        g2.setColor(Color.black);
        g2.drawRect(0, 0, iconSize - 1, iconSize - 1);
        g2.setStroke(new BasicStroke(Math.max(2f, iconSize / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(2, iconSize - 3, iconSize - 3, 2);
        g2.dispose();
        return new ImageIcon(image);
    }
}
