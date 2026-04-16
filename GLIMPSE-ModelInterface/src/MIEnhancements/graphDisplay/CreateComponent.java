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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;

import ModelInterface.InterfaceMain;

/**
 * Utility class for creating Swing components with preset properties.
 * Provides factory methods for dialogs, labels, buttons, lists, combo boxes, radio buttons, and text fields.
 */
public class CreateComponent {
    /** Debug flag for printing component creation events. */
    private static boolean debug = false;

    /**
     * Creates a resizable JDialog with the specified title and listeners for window events.
     *
     * @param name Title of the dialog
     * @return Configured JDialog instance
     */
    public static JDialog crtJDialog(String name) {
    JDialog dialog = new JDialog(InterfaceMain.getInstance().getFrame());
        dialog.setResizable(true);
        dialog.setTitle(name);
        dialog.setFocusable(true);
    dialog.setLocationRelativeTo(InterfaceMain.getInstance().getFrame());

        // Add window listener for open/close events
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JDialog dialog = (JDialog) e.getSource();
                if (debug)
                    System.out.println("CreateComponent::crtJDialog:windowCloseing:name " + dialog.getTitle());
            }

            @Override
            public void windowOpened(WindowEvent e) {
                JDialog dialog = (JDialog) e.getSource();
                if (debug)
                    System.out.println("CreateComponent::crtJDialog:windowOpened:name " + dialog.getTitle());
            }
        });

        // Add window state listener for activation/deactivation
        dialog.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                JDialog dialog = (JDialog) e.getSource();
                if (e.getNewState() == WindowEvent.WINDOW_ACTIVATED)
                    dialog.setVisible(true);
                else if (e.getNewState() == WindowEvent.WINDOW_DEACTIVATED)
                    dialog.setVisible(false);
                if (debug)
                    System.out.println("CreateComponent::crtJDialog:windowStateChanged:name " + dialog.getTitle());
            }
        });

        return dialog;
    }

    /**
     * Creates a JTextField with specified name, text, and column index property.
     *
     * @param name Name of the text field
     * @param text Initial text
     * @param index Column index property
     * @return Configured JTextField instance
     */
    public static JTextField crtJTextField(String name, String text, int index) {
        JTextField Jtf = new JTextField(text);
        Jtf.setName(name);
        Jtf.setFont(resolveConfiguredUIBasedFont("TextField.font", Jtf.getFont()));
        Jtf.getDocument().putProperty("colIndex", Integer.valueOf(index));
        Jtf.setMaximumSize(new Dimension(300, 20));
        Jtf.setMinimumSize(new Dimension(80, 20));
        Jtf.setEditable(true);
        Jtf.setDragEnabled(true);
        return Jtf;
    }

    /**
     * Creates a JLabel with specified name, text, and font size.
     *
     * @param name Name of the label
     * @param text Label text
     * @param fontSize Font size
     * @return Configured JLabel instance
     */
    public static JLabel crtJLabel(String name, String text, int fontSize) {
        JLabel jl = new JLabel(text);
        jl.setName(name);
        int offset = fontSize - InterfaceMain.getConfiguredFontSize();
        jl.setFont(resolveConfiguredUIBasedFont("Label.font", jl.getFont(), Font.PLAIN, offset));
        return jl;
    }

    /**
     * Creates a JLabel with specified name, text, font size, orientation, and size.
     *
     * @param name Name of the label
     * @param text Label text
     * @param fontSize Font size
     * @param orintation Label orientation (SwingConstants)
     * @param labSize Preferred size (not set)
     * @return Configured JLabel instance
     */
    public static JLabel crtJLabel(String name, String text, int fontSize, int orintation, Dimension labSize) {
        JLabel jl = new JLabel(text, orintation);
        jl.setName(name);
        int offset = fontSize - InterfaceMain.getConfiguredFontSize();
        jl.setFont(resolveConfiguredUIBasedFont("Label.font", jl.getFont(), Font.PLAIN, offset));
        // labSize is not used, but could be set with jl.setPreferredSize(labSize);
        return jl;
    }

    /**
     * Creates a JList with specified name, data, selection mode, and size.
     *
     * @param name Name of the list
     * @param data Array of list items
     * @param selectionMode Selection mode (ListSelectionModel)
     * @param listSize Preferred size (not set)
     * @return Configured JList instance
     */
    public static JList<?> dataList(String name, String data[], int selectionMode, Dimension listSize) {
        JList<?> list = new JList<Object>(data);
        list.setName(name);
        list.setFont(resolveConfiguredUIBasedFont("List.font", list.getFont()));
        list.setVisibleRowCount(3);
        list.setSelectionMode(selectionMode);
        // listSize is not used, but could be set with list.setPreferredSize(listSize);
        return list;
    }

    /**
     * Creates a JComboBox with specified name, data, selected index, and size.
     *
     * @param name Name of the combo box
     * @param data Array of combo box items
     * @param selIndex Selected index
     * @param comboSize Size of the combo box
     * @return Configured JComboBox instance
     */
    public static JComboBox<?> dataCombo(String name, String data[], int selIndex, Dimension comboSize) {
        JComboBox<String> cb = new JComboBox<String>(data);
        cb.setName(name);
        cb.setFont(resolveConfiguredUIBasedFont("ComboBox.font", cb.getFont()));
        if (data != null && data.length > 0) {
            cb.setPrototypeDisplayValue(longestValue(data));
        }
        cb.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String fullText = value == null ? "" : value.toString();
                label.setFont(cb.getFont());
                int availableWidth = index < 0
                        ? Math.max(80, cb.getWidth() - 30)
                        : Math.max(80, list.getWidth() - 16);
                String displayText = ellipsizeToWidth(fullText, label.getFontMetrics(label.getFont()), availableWidth);
                label.setText(displayText);
                label.setToolTipText(displayText.equals(fullText) ? null : fullText);
                return label;
            }
        });
        if (comboSize != null) {
            cb.setPreferredSize(comboSize);
            cb.setMinimumSize(comboSize);
            cb.setMaximumSize(comboSize);
            cb.setSize(comboSize);
        }
        cb.setSelectedIndex(selIndex);
        return cb;
    }

    /**
     * Creates a JButton with specified name and preferred size.
     *
     * @param name Button name
     * @param butSize Preferred size
     * @return Configured JButton instance
     */
    public static JButton crtJButton(String name, Dimension butSize) {
        JButton jb = new JButton(name);
        jb.setName(name);
        jb.setPreferredSize(butSize);
        return jb;
    }

    /**
     * Creates a JButton with specified name and background color.
     *
     * @param name Button name
     * @param color Background color
     * @return Configured JButton instance
     */
    public static JButton crtJButton(String name, Color color) {
        JButton jb = new JButton();
        jb.setBackground(color);
        jb.setName(name);
        jb.setPreferredSize(new Dimension(80, 20));
        return jb;
    }

    /**
     * Creates a JButton with specified name and icon.
     *
     * @param name Button name
     * @param icon ImageIcon for the button
     * @return Configured JButton instance
     */
    public static JButton crtJButton(String name, ImageIcon icon) {
        JButton jb = new JButton(icon);
        jb.setName(name);
        return jb;
    }

    /**
     * Creates a JRadioButton with specified name and selection state.
     *
     * @param name Radio button name
     * @param selected Whether the button is selected
     * @return Configured JRadioButton instance
     */
    public static JRadioButton crtJButGroup(String name, boolean selected) {
        JRadioButton radio = new JRadioButton(name);
        radio.setName(name);
        radio.setSelected(selected);
        return radio;
    }

    /**
     * Creates a JDialog containing the specified component and sets its title and preferred size.
     *
     * @param component Component to display in the dialog
     * @param title Dialog title
     * @return Configured JDialog instance
     */
    public static JDialog crtDialog(JComponent component, String title) {
    JDialog dialog = new JDialog(InterfaceMain.getInstance().getFrame());
        dialog.setTitle(title);
        dialog.setContentPane(component);
        dialog.pack();
        dialog.setPreferredSize(new Dimension(400, 300));
    dialog.setLocationRelativeTo(InterfaceMain.getInstance().getFrame());
        dialog.setVisible(true);
        return dialog;
    }

    private static Font resolveConfiguredUIBasedFont(String uiKey, Font fallback) {
        return resolveConfiguredUIBasedFont(uiKey, fallback, Font.PLAIN, 0);
    }

    private static Font resolveConfiguredUIBasedFont(String uiKey, Font fallback, int style, int sizeOffset) {
        Font uiFont = UIManager.getFont(uiKey);
        if (uiFont == null) {
            uiFont = fallback;
        }
        if (uiFont == null) {
            uiFont = new Font(Font.DIALOG, style, InterfaceMain.getConfiguredFontSize());
        }
        int resolvedSize = Math.max(8, InterfaceMain.getConfiguredFontSize() + sizeOffset);
        return uiFont.deriveFont(style, (float) resolvedSize);
    }

    private static String longestValue(String[] values) {
        String longest = "";
        if (values == null) {
            return longest;
        }
        for (String value : values) {
            if (value != null && value.length() > longest.length()) {
                longest = value;
            }
        }
        return longest;
    }

    private static String ellipsizeToWidth(String text, java.awt.FontMetrics metrics, int maxWidth) {
        if (text == null || maxWidth <= 0 || metrics.stringWidth(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String dots = "...";
        int dotsWidth = metrics.stringWidth(dots);
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + dotsWidth > maxWidth) {
            end--;
        }
        if (end <= 0) {
            return dots;
        }
        return text.substring(0, end) + dots;
    }
}