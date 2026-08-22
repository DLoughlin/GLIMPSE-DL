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
package chartOptions;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jfree.chart.JFreeChart;

import ModelInterface.InterfaceMain;
import chart.Chart;
import chart.LegendUtil;
import graphDisplay.CreateComponent;

/**
 * Dialog for modifying chart legend properties such as color, pattern, and line stroke.
 * Provides UI for legend customization and applies changes to the chart.
 */
public class ModifyLegend extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final int MAX_VISIBLE_LEGEND_ROWS = 20;
    private static final int DIALOG_GAP = 12;
    private static final int DIALOG_SCREEN_PADDING = 20;
    private Chart chart;
    private Chart[] charts;
    private String[] legend;
    private JFreeChart jfchart;
    private JTextField jtf;
    /**
     * Event apply type (not currently used)
     */
    public int eventApply;
    public JDialog cancelDialog;
    private JButton jbColor;
    private String changeColLegend;
    private HashMap<String, JComboBox> patternLookup = new HashMap<>();
    private HashMap<String, JComboBox> strokeLookup = new HashMap<>();
    TexturePaint[] tpList;
    TexturePaint[] tpStrokeList;
    BasicStroke[] bsStrokeList;
    // Pattern and stroke options
    int[] patternList = { 0, -4162, -4126, 11, 14, 16, 17 };
    int[] strokeList = { 0, 5, 10, 20, 30, 40 };
    private JDialog anchorDialog;

    /**
     * Constructor for ModifyLegend dialog.
     * @param charts Array of Chart objects
     * @param id Index of the chart to modify
     */
    public ModifyLegend(Chart[] charts, int id) {
        this(charts, id, null);
    }

    /**
     * Constructor for ModifyLegend dialog with an anchor chart dialog for placement.
     * @param charts Array of Chart objects
     * @param id Index of the chart to modify
     * @param anchorDialog Chart dialog used to position this dialog
     */
    public ModifyLegend(Chart[] charts, int id, JDialog anchorDialog) {
        if (charts == null)
            return;
        this.charts = charts;
        this.chart = charts[id];
        this.anchorDialog = anchorDialog;
        cancelDialog = this;
        setLegendUI();
    }

    /** Private no-arg constructor used by the embedded-panel factory only. */
    private ModifyLegend() {
        super((java.awt.Frame) null, false);
    }

    /**
     * Builds the legend-editing content as a self-contained {@link JScrollPane} that
     * can be embedded directly in a tab rather than shown in its own dialog window.
     * The "Done" button is omitted; Apply / Save For Query / Save Default remain.
     *
     * @param charts Array of Chart objects
     * @param id     Index of the chart to modify
     * @return A JScrollPane containing the legend editor panel
     */
    public static JScrollPane buildEmbeddedPanel(Chart[] charts, int id) {
        if (charts == null || id < 0 || id >= charts.length) {
            return new JScrollPane();
        }
        ModifyLegend ml = new ModifyLegend();
        ml.charts = charts;
        ml.chart  = charts[id];
        return ml.buildEmbeddedScrollPane();
    }

    /**
     * Builds the legend UI panel content and wraps it in a JScrollPane.
     * Used both by {@link #setLegendUI()} and {@link #buildEmbeddedPanel}.
     *
     * @param includeCloseButton whether to include the "Done" close button
     * @return JScrollPane containing the legend grid panel
     */
    private JScrollPane buildContentScrollPane(boolean includeCloseButton) {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel jp = new JPanel();
        jp.setLayout(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(1, 10, 5, 50);
        c.gridheight = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        setColumnLabel(gridbag, jp);
        legend = chart.getLegend().split(",");
        int rowHeight = 24;

        ImageIcon[] iiList = new ImageIcon[patternList.length];
        tpList = new TexturePaint[patternList.length];
        for (int a = 0; a < patternList.length; a++) {
            TexturePaint tp = LegendUtil.getTexturePaint(Color.BLACK, Color.GREEN, patternList[a], 1);
            tpList[a] = tp;
            iiList[a] = new ImageIcon(tp.getImage().getScaledInstance(45, 25, Image.SCALE_SMOOTH));
        }

        ImageIcon[] iiStrokeList = new ImageIcon[strokeList.length];
        tpStrokeList = new TexturePaint[strokeList.length];
        bsStrokeList = new BasicStroke[strokeList.length];
        for (int a = 0; a < strokeList.length; a++) {
            bsStrokeList[a] = LegendUtil.getLineStroke(strokeList[a]);
            TexturePaint tp = LegendUtil.getTexturePaint(Color.BLACK, Color.GREEN, 11, strokeList[a]);
            tpStrokeList[a] = tp;
            iiStrokeList[a] = new ImageIcon(tp.getImage().getScaledInstance(45, 25, Image.SCALE_SMOOTH));
        }

        for (int j = 0; j < legend.length; j++) {
            c = new GridBagConstraints();
            c.fill = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            String name = String.valueOf(j);
            jtf = CreateComponent.crtJTextField(name, legend[j], j);
            jtf.setScrollOffset(10);
            jtf.setEditable(false);
            rowHeight = Math.max(rowHeight, jtf.getPreferredSize().height + 6);
            gridbag.setConstraints(jtf, c);
            jp.add(jtf);
            ImageIcon icon = new ImageIcon();
            TexturePaint tpCur = LegendUtil.getTexturePaint(new Color(chart.getColor()[j]), new Color(chart.getColor()[j]), 0, 0);
            icon.setImage(tpCur.getImage());
            Image image = icon.getImage();
            image = image.getScaledInstance(80, 20, Image.SCALE_SMOOTH);
            icon.setImage(image);
            JButton jb = CreateComponent.crtJButton(name, (ImageIcon) null);
            jb.setIcon(icon);
            ColorModifyActionListener mbl = new ColorModifyActionListener();
            jb.setFocusable(true);
            jb.addActionListener(mbl);
            gridbag.setConstraints(jb, c);
            jp.add(jb);
            chart.getPattern();
            JComboBox jcb = new JComboBox(iiList);
            patternLookup.put(legend[j].trim(), jcb);
            gridbag.setConstraints(jcb, c);
            jp.add(jcb);
            chart.getLineStrokes();
            jcb = new JComboBox(iiStrokeList);
            strokeLookup.put(legend[j].trim(), jcb);
            gridbag.setConstraints(jcb, c);
            jp.add(jcb);
            c.gridwidth = GridBagConstraints.REMAINDER;
            JLabel jl = new JLabel("");
            gridbag.setConstraints(jl, c);
            jp.add(jl);
            c.gridwidth = 0;
            c.weightx = 0.0;
        }

        for (int j = 0; j < legend.length; j++) {
            int pattern = chart.getPattern()[j];
            for (int curIDX = 0; curIDX < patternList.length; curIDX++) {
                if (pattern == patternList[curIDX]) {
                    patternLookup.get(legend[j].trim()).setSelectedIndex(curIDX);
                }
            }
            int stroke = chart.getLineStrokes()[j];
            for (int curIDX = 0; curIDX < strokeList.length; curIDX++) {
                if (stroke == strokeList[curIDX]) {
                    strokeLookup.get(legend[j].trim()).setSelectedIndex(curIDX);
                }
            }
        }

        String[] buttonNames = includeCloseButton
                ? new String[]{ "Apply", "Save For Query", "Save Default", "Done" }
                : new String[]{ "Apply", "Save For Query", "Save Default" };
        JButton jb1;
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(3, 0, 1, 0));
        for (int i = 0; i < buttonNames.length; i++) {
            jb1 = crtJButton(buttonNames[i], i);
            c = new GridBagConstraints();
            gridbag.setConstraints(jb1, c);
            box.add(jb1);
        }
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.ipadx = 60;
        gridbag.setConstraints(box, c);
        jp.add(box);

        c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        gridbag.setConstraints(spacer, c);
        jp.add(spacer);

        JScrollPane jsp = new JScrollPane(jp);
        jsp.setBorder(BorderFactory.createEmptyBorder());

        Dimension panelPref = jp.getPreferredSize();
        int visibleLegendRows = Math.min(legend.length, MAX_VISIBLE_LEGEND_ROWS);
        int estimatedRows = 1 + visibleLegendRows + 1;
        int desiredHeight = estimatedRows * Math.max(24, rowHeight) + 24;
        int desiredWidth = panelPref.width + 24;
        if (legend.length > MAX_VISIBLE_LEGEND_ROWS) {
            jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            desiredWidth += jsp.getVerticalScrollBar().getPreferredSize().width;
        } else {
            jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        Rectangle availableScreen = getAvailableScreenBounds(anchorDialog != null ? anchorDialog : this);
        desiredWidth = Math.min(desiredWidth, Math.max(300, availableScreen.width - DIALOG_SCREEN_PADDING));
        desiredHeight = Math.min(desiredHeight, Math.max(260, availableScreen.height - DIALOG_SCREEN_PADDING));
        jsp.setPreferredSize(new Dimension(desiredWidth, desiredHeight));
        return jsp;
    }

    /** Builds the embedded (tab) scroll pane without the Done button. */
    private JScrollPane buildEmbeddedScrollPane() {
        cancelDialog = this; // never actually disposed in embedded mode
        return buildContentScrollPane(false);
    }

    /**
     * Custom renderer for combo boxes with images.
     */
    class ImageComboBoxRenderer extends JPanel implements ListCellRenderer<String> {
        private JLabel label;
        private BufferedImage image;
        public ImageComboBoxRenderer(BufferedImage image) {
            this.image = image;
            this.label = new JLabel();
            setLayout(new BorderLayout());
            add(label, BorderLayout.CENTER);
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            label.setText(value);
            return this;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    /**
     * Sets up the legend modification UI (dialog mode – includes Done button).
     */
    private void setLegendUI() {
        JScrollPane jsp = buildContentScrollPane(true);
        setContentPane(jsp);
        setTitle("Chart Options");
        pack();
        placeDialogToRightOfChart();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Creates a JButton with mouse listener for legend actions.
     * @param name Button name
     * @param i Tooltip index
     * @return JButton
     */
    private JButton crtJButton(String name, int i) {
        JButton jb = new JButton(name);
        jb.setName(name);
        jb.setToolTipText(String.valueOf(i));
        jb.addActionListener(e -> {
          JButton jb1 = (JButton) e.getSource();
          jbColor = jb1;
          if (jb1.getName().equals("Apply")) {
            doApply();
          } else if (jb1.getName().equals("Save")) {
            chart.storelegendInfo(chart.getLegend().split(","), getLegendInfoStr());
          } else if (jb1.getName().equals("Save Default")) {
            chart.storelegendInfoGlobal(chart.getLegend().split(","), getLegendInfoStr());
          } else if (jb1.getName().equals("Save For Query")) {
            chart.storelegendInfoLocal(chart.getLegend().split(","), getLegendInfoStr());
          } else if (jb1.getName().equals("Done")) {
            cancelDialog.dispose();
          }
        });
        return jb;
    }

    /**
     * Applies legend changes to the chart.
     */
    private void doApply() {
        SetModifyChanges.setColorChanges(charts);
        SetModifyChanges.setPatternChanges(charts, patternLookup, patternList);
        SetModifyChanges.setStrokeChanges(charts, strokeLookup, bsStrokeList, strokeList);
    }

    /**
     * Gets legend info as a string array for saving.
     * @return Array of legend info strings
     */
    private String[] getLegendInfoStr() {
        String[] s = new String[chart.getColor().length];
        for (int i = 0; i < s.length; i++) {
            s[i] = chart.getColor()[i] + "," + chart.getpColor()[i] + "," + chart.getPattern()[i] + "," + chart.getLineStrokes()[i];
        }
        return s;
    }

    /**
     * Sets column labels for the legend UI.
     * @param gridbag GridBagLayout
     * @param jp Panel
     */
    private void setColumnLabel(GridBagLayout gridbag, JPanel jp) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        String[] name = { "Legend", "Color", "Pattern", "Line Stroke" };
        int configuredSize = InterfaceMain.getConfiguredFontSize();
        JLabel jl = null;
        for (int j = 0; j < name.length; j++) {
            Box box = Box.createHorizontalBox();
            if (j == 0) {
                jl = CreateComponent.crtJLabel(name[j], name[j], configuredSize, 2, new Dimension(200, 20));
            } else {
                jl = CreateComponent.crtJLabel(name[j], name[j], configuredSize, 2, new Dimension(80, 20));
            }
            box.add(jl);
            gridbag.setConstraints(box, c);
            jp.add(box);
        }
        c.gridwidth = GridBagConstraints.REMAINDER;
        jl = new JLabel("");
        gridbag.setConstraints(jl, c);
        jp.add(jl);
    }

    private void placeDialogToRightOfChart() {
        if (anchorDialog == null) {
            setLocationRelativeTo(getOwner());
            return;
        }
        Rectangle available = getAvailableScreenBounds(anchorDialog);
        Rectangle anchorBounds = anchorDialog.getBounds();

        int x = anchorBounds.x + anchorBounds.width + DIALOG_GAP;
        if (x + getWidth() > available.x + available.width) {
            x = anchorBounds.x - getWidth() - DIALOG_GAP;
        }
        x = Math.max(available.x, Math.min(x, available.x + available.width - getWidth()));

        int y = Math.max(available.y, Math.min(anchorBounds.y, available.y + available.height - getHeight()));
        setLocation(x, y);
    }

    private Rectangle getAvailableScreenBounds(Window anchor) {
        GraphicsConfiguration gc = anchor == null ? null : anchor.getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        Rectangle bounds = new Rectangle(gc.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);
        return bounds;
    }

    /**
     * ActionListener for color modification button.
     */
    public class ColorModifyActionListener implements ActionListener {
        public ColorModifyActionListener() {}
        @Override
        public void actionPerformed(ActionEvent e) {
            jbColor = (JButton) e.getSource();
            ColorChooser4DynamicModifyColor cc = new ColorChooser4DynamicModifyColor(chart, jbColor);
            chart.getPaint()[Integer.valueOf(jbColor.getName().trim())] = cc.getPaint();
            chart.setColor(cc.getColor(), Integer.valueOf(jbColor.getName().trim()));
        }
    }

    /**
     * DocumentListener for legend text fields (not currently used).
     */
    class MyDocumentListener implements DocumentListener {
        JTextField jtf;
        JButton jb;
        public MyDocumentListener(JTextField jtf, JButton jb) {
            this.jtf = jtf;
            this.jb = jb;
        }
        public void changedUpdate(DocumentEvent e) {
            setFldValue(e);
        }
        public void insertUpdate(DocumentEvent e) {
            setFldValue(e);
            eventApply = Integer.valueOf(jtf.getToolTipText().trim());
            jbColor = jb;
        }
        public void removeUpdate(DocumentEvent e) {
            setFldValue(e);
        }
        private void setFldValue(DocumentEvent e) {
            try {
                Document doc = e.getDocument();
                int vStrLen = doc.getLength();
                doc.getText(0, vStrLen);
                changeColLegend = legend[Integer.valueOf(jb.getName().trim())];
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Gets the JFreeChart object.
     * @return JFreeChart
     */
    public JFreeChart getJfchart() {
        return jfchart;
    }
}