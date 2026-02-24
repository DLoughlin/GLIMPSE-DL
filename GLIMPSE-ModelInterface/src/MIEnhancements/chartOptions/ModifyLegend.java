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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.TexturePaint;
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

import chart.Chart;
import chart.LegendUtil;
import graphDisplay.CreateComponent;

/**
 * Dialog for modifying chart legend properties such as color, pattern, and line stroke.
 * Provides UI for legend customization and applies changes to the chart.
 */
public class ModifyLegend extends JDialog {
    private static final long serialVersionUID = 1L;
    private Chart chart;
    private Chart[] charts;
    private String[] legend;
    private int id;
    private JFreeChart jfchart;
    private JTextField jtf;
    /**
     * Event apply type (not currently used)
     */
    public int eventApply;
    public JDialog cancelDialog;
    private JButton jbColor;
    private JTextField jtfChanged;
    private String changeColLegend;
    private HashMap<String, JComboBox> patternLookup = new HashMap<>();
    private HashMap<String, JComboBox> strokeLookup = new HashMap<>();
    TexturePaint[] tpList;
    TexturePaint[] tpStrokeList;
    BasicStroke[] bsStrokeList;
    // Pattern and stroke options
    int[] patternList = { 0, -4162, -4126, 11, 14, 16, 17 };
    int[] strokeList = { 0, 5, 10, 20, 30, 40 };

    /**
     * Constructor for ModifyLegend dialog.
     * @param charts Array of Chart objects
     * @param id Index of the chart to modify
     */
    public ModifyLegend(Chart[] charts, int id) {
        if (charts == null)
            return;
        this.charts = charts;
        this.id = id;
        this.chart = charts[id];
        cancelDialog = this;
        setLegendUI();
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
     * Sets up the legend modification UI.
     */
    private void setLegendUI() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel jp = new JPanel();
        jp.setLayout(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 10, 5, 50);
        c.gridheight = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        setColumnLabel(gridbag, jp);
        legend = chart.getLegend().split(",");

        // Prepare pattern icons and TexturePaints
        ImageIcon[] iiList = new ImageIcon[patternList.length];
        tpList = new TexturePaint[patternList.length];
        for (int a = 0; a < patternList.length; a++) {
            TexturePaint tp = LegendUtil.getTexturePaint(Color.BLACK, Color.GREEN, patternList[a], 1);
            tpList[a] = tp;
            iiList[a] = new ImageIcon(tp.getImage().getScaledInstance(45, 25, Image.SCALE_SMOOTH));
        }

        // Prepare stroke icons and TexturePaints
        ImageIcon[] iiStrokeList = new ImageIcon[strokeList.length];
        tpStrokeList = new TexturePaint[strokeList.length];
        bsStrokeList = new BasicStroke[strokeList.length];
        for (int a = 0; a < strokeList.length; a++) {
            bsStrokeList[a] = LegendUtil.getLineStroke(strokeList[a]);
            TexturePaint tp = LegendUtil.getTexturePaint(Color.BLACK, Color.GREEN, 11, strokeList[a]);
            tpStrokeList[a] = tp;
            iiStrokeList[a] = new ImageIcon(tp.getImage().getScaledInstance(45, 25, Image.SCALE_SMOOTH));
        }

        // Add legend rows
        for (int j = 0; j < legend.length; j++) {
            c = new GridBagConstraints();
            c.fill = 1;
            String name = String.valueOf(j);
            // Legend name field
            jtf = CreateComponent.crtJTextField(name, legend[j], j);
            jtf.setScrollOffset(10);
            jtf.setEditable(false);
            Font font1 = new Font("SansSerif", Font.PLAIN, 14);
            jtf.setFont(font1);
            gridbag.setConstraints(jtf, c);
            jp.add(jtf);
            // Color changer button
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
            // Pattern combo box
            int[] pattern = chart.getPattern();
            JComboBox jcb = new JComboBox(iiList);
            patternLookup.put(legend[j].trim(), jcb);
            gridbag.setConstraints(jcb, c);
            jp.add(jcb);
            // Stroke combo box
            int[] ls = chart.getLineStrokes();
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

        // Set selected index for pattern and stroke combo boxes
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

        // Add control buttons
        String[] options = { "Apply", "Save For Query", "Save Default", "Done" };
        JButton jb1;
        Box box = Box.createHorizontalBox();
        box.add(Box.createVerticalStrut(30));
        for (int i = 0; i < options.length; i++) {
            jb1 = crtJButton(options[i], i);
            gridbag.setConstraints(jb1, c);
            box.add(jb1);
        }
        box.add(Box.createVerticalStrut(30));
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.ipadx = 60;
        gridbag.setConstraints(box, c);
        jp.add(box);

        JScrollPane jsp = new JScrollPane(jp);
        jsp.setBorder(BorderFactory.createEmptyBorder());
        setContentPane(jsp);
        pack();
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
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JButton jb1 = (JButton) e.getSource();
                if (e.getClickCount() > 0)
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
            }
        };
        jb.addMouseListener(ml);
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
        JLabel jl = null;
        for (int j = 0; j < name.length; j++) {
            Box box = Box.createHorizontalBox();
            if (j == 0) {
                jl = CreateComponent.crtJLabel(name[j], name[j], 10, 2, new Dimension(200, 20));
            } else {
                jl = CreateComponent.crtJLabel(name[j], name[j], 10, 2, new Dimension(80, 20));
            }
            jl.setFont(new Font("Verdana", 1, 12));
            box.add(jl);
            gridbag.setConstraints(box, c);
            jp.add(box);
        }
        c.gridwidth = GridBagConstraints.REMAINDER;
        jl = new JLabel("");
        gridbag.setConstraints(jl, c);
        jp.add(jl);
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
            jtfChanged = jtf;
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
