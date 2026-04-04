package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.geotools.swing.JMapPane;
import org.geotools.swing.tool.PanTool;
import org.geotools.swing.tool.ZoomInTool;
import org.geotools.swing.tool.ZoomOutTool;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;
import filter.FilteredTable;
import mapOptions.LegendPanel;
import mapOptions.MapColor;
import mapOptions.MapColorPalette;
import mapOptions.MapOptionsUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Shared Swing and map-redraw behavior for state/world map panels.
 * Concrete subclasses supply the map-specific MapContent creation and optional map setup.
 */
public abstract class AbstractMapPanel extends JFrame implements ComponentListener {
    protected String chartName;
    protected JFrame frame;
    protected JMapPane jmap;
    protected MapContent stateMap;
    protected JToolBar toolBar;
    protected JPanel scenarioMenuPanel;
    protected JPanel yearMenuPanel;
    protected JPanel colorSchemePanel;
    protected JPanel colorChoicePanel;
    protected JPanel changeNumberPanel;
    protected JPanel refreshMapPanel;
    protected JPanel reverseColorPanel;
    protected JPanel colorConfigPanel;
    protected JPanel exportMapPanel;
    protected JPanel navigationPanel;
    protected JPanel scaleStatusPanel;
    protected JPopupMenu mapContextMenu;
    protected ButtonGroup choiceGroup;
    protected ButtonGroup navigationGroup;
    protected int numColorChoice;
    protected int numColorClass;
    protected String paletteChoice;
    protected JPanel addMapPanel;
    protected JPanel addLegendPanel;
    protected JPanel sectorDisplayPanel;
    protected HashMap<String, String> unitLookup = null;
    protected JTable jtable;
    protected JComboBox<String> scenarioListMenu;
    protected JComboBox<String> yearListMenu;
    protected JButton nextYearButton;
    protected JButton prevYearButton;
    protected JLabel scenarioListLabel;
    protected JLabel listLabel;
    protected JLabel legendLabel;
    protected JLabel scaleStatusLabel;
    protected JTextArea sectorText;
    protected JFormattedTextField minField;
    protected JFormattedTextField maxField;
    protected double previousMin;
    protected double previousMax;
    protected double[] minMaxFromTable = new double[2];
    protected MapColorPalette usePalette;
    protected MapColor useMapColor;
    protected JComboBox<String> comboBoxPalette;
    protected JComboBox<Integer> comboBoxNumClasses;
    protected JComboBox<PaletteChoiceOption> comboBoxPaletteChoice;
    protected static final String[] paletteType = {"SEQUENTIAL", "DIVERGING"};
    protected static final Integer[] numClasses = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    protected IntervalType intervalType = IntervalType.AUTOMATIC;
    protected boolean reverseColors;
    protected boolean normalizeScale;
    protected final MapMode mapMode;

    private static final Logger LOGGER = Logger.getLogger(AbstractMapPanel.class.getName());
    private static final int LEGEND_PANEL_WIDTH = 160;
    private static final int DIVERGING_SWATCH_HEIGHT = 110;
    private static final int SEQUENTIAL_SWATCH_HEIGHT = 72;
    private static final Font MAP_LABEL_FONT = resolveUiFont(Font.PLAIN, 15);
    private static final Font MAP_FIELD_FONT = resolveUiFont(Font.PLAIN, 14);
    private static final Font MAP_BUTTON_FONT = resolveUiFont(Font.PLAIN, 14);
    private static final int PALETTE_PREVIEW_WIDTH = 76;
    private static final int PALETTE_PREVIEW_HEIGHT = 14;
    private static final int PALETTE_COMBO_WIDTH = 104;

    public enum IntervalType {
        CUSTOM, AUTOMATIC
    }

    public static IntervalType getIntervalType(String type) {
        for (IntervalType iType : IntervalType.values()) {
            if (iType.toString().equalsIgnoreCase(type)) {
                return iType;
            }
        }
        return null;
    }

    protected AbstractMapPanel(String chartName, JTable jtable) {
        this(chartName, jtable, MapMode.STATE);
    }

    protected AbstractMapPanel(String chartName, JTable jtable, MapMode mapMode) {
        this.chartName = chartName;
        this.jtable = jtable;
        this.frame = this;
        this.mapMode = mapMode == null ? MapMode.STATE : mapMode;
    }

    protected void initialize() {
        frame.setTitle("Map for " + chartName);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().removeAll();
        frame.getContentPane().add(createToolBar(), BorderLayout.WEST);
        normalizeScale = true;
        minMaxFromTable = getInitialMinMax(normalizeScale);
        reverseColors = false;
        usePalette = MapColorPalette.getMapColorPalette("DIVERGING", 4, 10, reverseColors);
        useMapColor = new MapColor(usePalette, minMaxFromTable[0], minMaxFromTable[1]);
        frame.getContentPane().add(createMapContent(), BorderLayout.CENTER);
        frame.getContentPane().add(createFooter(), BorderLayout.PAGE_END);
        frame.getContentPane().add(addLegendPanel(), BorderLayout.EAST);
        frame.pack();
        frame.setSize(new Dimension(1200, 800));
        frame.setMinimumSize(new Dimension(500, 300));
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        if (!DbViewer.openWindows.contains(frame)) {
            DbViewer.openWindows.add(frame);
        }
    }

    protected JComponent createToolBar() {
        toolBar = new JToolBar(JToolBar.VERTICAL);
        toolBar.setBackground(Color.LIGHT_GRAY);
        toolBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        toolBar.setPreferredSize(new Dimension(340, 800));
        toolBar.setLayout(new BorderLayout());
        toolBar.setFloatable(false);

        WrappingPanel toolContent = new WrappingPanel(new BorderLayout());
        JPanel controlStack = new JPanel();
        controlStack.setOpaque(false);
        controlStack.setLayout(new BoxLayout(controlStack, BoxLayout.Y_AXIS));
        controlStack.setBorder(new EmptyBorder(0, 0, 0, 4));
        toolContent.add(controlStack, BorderLayout.NORTH);

        scenarioMenuPanel = createControlSectionPanel();
        scenarioListLabel = new JLabel("Scenario:", SwingConstants.LEFT);
        scenarioListLabel.setFont(MAP_LABEL_FONT);
        scenarioListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        List<String> scenarioListFromTable = MapOptionsUtil.getScenarioListFromTableData(jtable);
        DefaultComboBoxModel<String> dmlScenario = new DefaultComboBoxModel<>();
        for (String scenario : scenarioListFromTable) {
            dmlScenario.addElement(scenario);
        }
        scenarioListMenu = new JComboBox<>();
        scenarioListMenu.setModel(dmlScenario);
        if (dmlScenario.getSize() > 0) {
            scenarioListMenu.setSelectedIndex(0);
        }
        scenarioListMenu.setVisible(true);
        scenarioListMenu.setFont(MAP_FIELD_FONT);
        scenarioListMenu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        scenarioListMenu.addActionListener(new UpdateMap());
        scenarioMenuPanel.add(scenarioListLabel);
        scenarioMenuPanel.add(Box.createVerticalStrut(4));
        scenarioMenuPanel.add(scenarioListMenu);
        controlStack.add(scenarioMenuPanel);
        controlStack.add(Box.createVerticalStrut(8));

        yearMenuPanel = createControlSectionPanel();
        listLabel = new JLabel("Year:", SwingConstants.LEFT);
        listLabel.setFont(MAP_LABEL_FONT);
        listLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        ArrayList<String> yearListFromTable = MapOptionsUtil.getYearListFromTableData(jtable);
        DefaultComboBoxModel<String> dml = new DefaultComboBoxModel<>();
        for (String year : yearListFromTable) {
            dml.addElement(year);
        }
        if (!yearListFromTable.isEmpty()) {
            dml.setSelectedItem(yearListFromTable.get(0));
        }
        yearListMenu = new JComboBox<>();
        yearListMenu.setModel(dml);
        if (dml.getSize() > 0) {
            yearListMenu.setSelectedIndex(0);
        }
        yearListMenu.setVisible(true);
        yearListMenu.setFont(MAP_FIELD_FONT);
        yearListMenu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        yearListMenu.addActionListener(new UpdateMap());
        nextYearButton = new JButton(">");
        styleActionButton(nextYearButton);
        nextYearButton.setMargin(new Insets(2, 8, 2, 8));
        nextYearButton.addActionListener(e -> {
            int y = yearListMenu.getSelectedIndex();
            if (y < yearListMenu.getModel().getSize() - 1) {
                yearListMenu.setSelectedIndex(y + 1);
            }
        });
        prevYearButton = new JButton("<");
        styleActionButton(prevYearButton);
        prevYearButton.setMargin(new Insets(2, 8, 2, 8));
        prevYearButton.addActionListener(e -> {
            int y = yearListMenu.getSelectedIndex();
            if (y > 0) {
                yearListMenu.setSelectedIndex(y - 1);
            }
        });
        JPanel yearControls = new JPanel();
        yearControls.setOpaque(false);
        yearControls.setLayout(new BoxLayout(yearControls, BoxLayout.X_AXIS));
        yearControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        yearControls.add(prevYearButton);
        yearControls.add(Box.createHorizontalStrut(6));
        yearControls.add(yearListMenu);
        yearControls.add(Box.createHorizontalStrut(6));
        yearControls.add(nextYearButton);
        yearMenuPanel.add(listLabel);
        yearMenuPanel.add(Box.createVerticalStrut(4));
        yearMenuPanel.add(yearControls);
        controlStack.add(yearMenuPanel);
        controlStack.add(Box.createVerticalStrut(8));

        JLabel selectColorLabel = new JLabel("Palette type:", SwingConstants.LEFT);
        selectColorLabel.setFont(MAP_LABEL_FONT);
        colorChoicePanel = createControlSectionPanel();
        colorChoicePanel.setBorder(new EmptyBorder(4, 0, 0, 0));
        colorChoicePanel.setLayout(new BoxLayout(colorChoicePanel, BoxLayout.Y_AXIS));
        colorChoicePanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        colorSchemePanel = createControlSectionPanel();
        comboBoxPalette = new JComboBox<>(paletteType);
        comboBoxPalette.setFont(MAP_FIELD_FONT);
        comboBoxPalette.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        comboBoxPalette.setSelectedIndex(1);
        colorSchemePanel.add(selectColorLabel);
        colorSchemePanel.add(Box.createVerticalStrut(4));
        colorSchemePanel.add(comboBoxPalette);
        controlStack.add(colorSchemePanel);
        comboBoxPalette.addActionListener(e -> {
            limitNumClassesAndChangeChoices();
            redrawMap();
        });
        addDivergingColorChoices();
        controlStack.add(colorChoicePanel);
        controlStack.add(Box.createVerticalStrut(8));

        JLabel changeNumberLabel = new JLabel("Number of color classes:", SwingConstants.LEFT);
        changeNumberLabel.setFont(MAP_LABEL_FONT);
        changeNumberPanel = createControlSectionPanel();
        changeNumberPanel.add(changeNumberLabel);
        changeNumberPanel.add(Box.createVerticalStrut(4));
        comboBoxNumClasses = new JComboBox<>(numClasses);
        comboBoxNumClasses.setFont(MAP_FIELD_FONT);
        comboBoxNumClasses.setMaximumSize(new Dimension(90, 28));
        comboBoxNumClasses.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBoxNumClasses.setSelectedIndex(chooseNumCombos() - 1);
        comboBoxNumClasses.addActionListener(e -> redrawMap());
        changeNumberPanel.add(comboBoxNumClasses);
        controlStack.add(changeNumberPanel);
        controlStack.add(Box.createVerticalStrut(8));

        reverseColorPanel = createControlSectionPanel();
        JButton reverseBtn = new JButton("Reverse Colors");
        styleActionButton(reverseBtn);
        reverseBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        reverseBtn.addActionListener(e -> {
            reverseColors = !reverseColors;
            redrawMap();
        });
        reverseColorPanel.add(reverseBtn);
        controlStack.add(reverseColorPanel);
        controlStack.add(Box.createVerticalStrut(8));

        colorConfigPanel = createControlSectionPanel();
        JButton configBtn = new JButton("Modify Color Scale");
        styleActionButton(configBtn);
        configBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        configBtn.addActionListener(e -> colorScaleOptions());
        colorConfigPanel.add(configBtn);
        controlStack.add(colorConfigPanel);
        controlStack.add(Box.createVerticalStrut(8));

        scaleStatusPanel = createControlSectionPanel();
        scaleStatusLabel = new JLabel();
        scaleStatusLabel.setFont(MAP_FIELD_FONT);
        scaleStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel scaleModeLabel = new JLabel("Scale mode:", SwingConstants.LEFT);
        scaleModeLabel.setFont(MAP_LABEL_FONT);
        scaleStatusPanel.add(scaleModeLabel);
        scaleStatusPanel.add(Box.createVerticalStrut(4));
        scaleStatusPanel.add(scaleStatusLabel);
        controlStack.add(scaleStatusPanel);
        controlStack.add(Box.createVerticalStrut(8));

        navigationPanel = createControlSectionPanel();
        JLabel mapToolsLabel = new JLabel("Map tools:", SwingConstants.LEFT);
        mapToolsLabel.setFont(MAP_LABEL_FONT);
        navigationPanel.add(mapToolsLabel);
        navigationPanel.add(Box.createVerticalStrut(4));
        navigationPanel.add(createNavigationButtons());
        controlStack.add(navigationPanel);
        controlStack.add(Box.createVerticalStrut(8));

        refreshMapPanel = createControlSectionPanel();
        JButton refreshBtn = new JButton("Refresh Map");
        styleActionButton(refreshBtn);
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> redrawMap());
        refreshMapPanel.add(refreshBtn);
        controlStack.add(refreshMapPanel);
        controlStack.add(Box.createVerticalStrut(8));

        exportMapPanel = createControlSectionPanel();
        JPanel exportButtons = new JPanel();
        exportButtons.setOpaque(false);
        exportButtons.setLayout(new BoxLayout(exportButtons, BoxLayout.X_AXIS));
        exportButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton copyBtn = new JButton("Copy");
        styleActionButton(copyBtn);
        copyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyBtn.addActionListener(e -> copyMapImageToClipboard());

        JButton saveBtn = new JButton("Export (PNG)");
        styleActionButton(saveBtn);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> saveMap());

        exportButtons.add(copyBtn);
        exportButtons.add(Box.createHorizontalStrut(6));
        exportButtons.add(saveBtn);
        exportMapPanel.add(exportButtons);
        controlStack.add(exportMapPanel);
        controlStack.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(toolContent);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        toolBar.add(scrollPane, BorderLayout.CENTER);
        updateScaleStatusLabel();
        return toolBar;
    }

    protected JComponent createMapContent() {
        addMapPanel = new JPanel();
        addMapPanel.setLayout(new BoxLayout(addMapPanel, BoxLayout.X_AXIS));
        addMapPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        addMapPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (stateMap != null) {
            stateMap.dispose();
        }
        stateMap = createBoundaryMapLayer();
        if (stateMap == null) {
            stateMap = new MapContent();
        }
        jmap = new JMapPane(stateMap);
        jmap.setBorder(new EmptyBorder(10, 10, 10, 10));
        installMapContextMenu(jmap);
        configureMapPane(jmap);
        addMapPanel.add(jmap);
        return addMapPanel;
    }

    protected int chooseNumCombos() {
        int intToReturn = 10;
        if (minMaxFromTable[0] < 0 && minMaxFromTable[1] > 0) {
            intToReturn = 11;
        }
        return intToReturn;
    }

    protected JComponent addLegendPanel() {
        addLegendPanel = new JPanel();
        addLegendPanel.setLayout(new BoxLayout(addLegendPanel, BoxLayout.Y_AXIS));
        addLegendPanel.setBorder(new EmptyBorder(5, 2, 5, 4));
        addLegendPanel.setPreferredSize(new Dimension(LEGEND_PANEL_WIDTH, 150));
        addLegendPanel.setMinimumSize(new Dimension(LEGEND_PANEL_WIDTH, 120));
        addLegendPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        legendLabel = new JLabel("Legend");
        legendLabel.setFont(MAP_LABEL_FONT);
        legendLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        addLegendPanel.add(legendLabel);
        addLegendPanel.add(Box.createVerticalStrut(4));
        addLegendPanel.add(new LegendPanel(useMapColor, getUnitForLegend()));
        return addLegendPanel;
    }

    protected JComponent createFooter() {
        sectorDisplayPanel = new JPanel(new BorderLayout());
        sectorDisplayPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        String mapSectorInfo = MapOptionsUtil.getSectorPlusInfo(jtable);
        sectorText = new JTextArea(buildFooterText(mapSectorInfo));
        sectorText.setVisible(mapSectorInfo.length() > 0);
        sectorText.setFont(MAP_FIELD_FONT);
        sectorText.setLineWrap(true);
        sectorText.setWrapStyleWord(true);
        sectorText.setEditable(false);
        sectorText.setFocusable(false);
        sectorText.setOpaque(false);
        sectorText.setBorder(null);
        sectorText.setRows(2);
        sectorDisplayPanel.add(sectorText, BorderLayout.CENTER);
        return sectorDisplayPanel;
    }

    public JMapPane getJmap() {
        return jmap;
    }

    protected void redrawMapLayout() {
        if (frame == null) {
            return;
        }
        Container contentPane = frame.getContentPane();
        if (sectorDisplayPanel != null) {
            contentPane.remove(sectorDisplayPanel);
        }
        if (addLegendPanel != null) {
            contentPane.remove(addLegendPanel);
        }
        if (addMapPanel != null) {
            contentPane.remove(addMapPanel);
        }
        if (stateMap != null) {
            stateMap.dispose();
            stateMap = null;
        }
        contentPane.add(createMapContent(), BorderLayout.CENTER);
        contentPane.add(createFooter(), BorderLayout.PAGE_END);
        contentPane.add(addLegendPanel(), BorderLayout.EAST);
        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }

    protected void redrawMapInternal() {
        if (comboBoxNumClasses == null || comboBoxNumClasses.getSelectedItem() == null || choiceGroup == null) {
            return;
        }
        if (yearListMenu == null || scenarioListMenu == null
                || yearListMenu.getSelectedItem() == null || scenarioListMenu.getSelectedItem() == null) {
            return;
        }
        String selectedChoice = MapOptionsUtil.getSelectedButton(choiceGroup);
        if (selectedChoice == null && comboBoxPaletteChoice != null && comboBoxPaletteChoice.getSelectedItem() instanceof PaletteChoiceOption) {
            selectedChoice = String.valueOf(((PaletteChoiceOption) comboBoxPaletteChoice.getSelectedItem()).getChoiceNumber());
        }
        if (selectedChoice == null || comboBoxPalette == null || comboBoxPalette.getSelectedItem() == null) {
            return;
        }
        numColorClass = (int) comboBoxNumClasses.getSelectedItem();
        numColorChoice = Integer.parseInt(selectedChoice) - 1;
        paletteChoice = (String) comboBoxPalette.getSelectedItem();
        usePalette = MapColorPalette.getMapColorPalette(paletteChoice, numColorChoice, numColorClass, reverseColors);
        if (intervalType == IntervalType.AUTOMATIC) {
            minMaxFromTable = getCurrentMinMax(normalizeScale);
        }
        if (intervalType == IntervalType.CUSTOM && minField != null && maxField != null
                && minField.getValue() instanceof Number && maxField.getValue() instanceof Number) {
            double minCustom = ((Number) minField.getValue()).doubleValue();
            double maxCustom = ((Number) maxField.getValue()).doubleValue();
            useMapColor = new MapColor(usePalette, minCustom, maxCustom);
        } else {
            useMapColor = new MapColor(usePalette, minMaxFromTable[0], minMaxFromTable[1]);
        }

        if (stateMap != null) {
            stateMap.dispose();
        }
        stateMap = createBoundaryMapLayer();
        if (stateMap == null) {
            stateMap = new MapContent();
        }
        if (jmap != null) {
            jmap.setMapContent(stateMap);
            configureMapPane(jmap);
            jmap.repaint();
        }
        refreshLegend();
        refreshFooter();
    }

    private void refreshLegend() {
        if (addLegendPanel != null) {
            addLegendPanel.removeAll();
            if (legendLabel == null) {
                legendLabel = new JLabel("Legend");
                legendLabel.setFont(MAP_LABEL_FONT);
                legendLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }
            addLegendPanel.add(legendLabel);
            addLegendPanel.add(Box.createVerticalStrut(4));
            addLegendPanel.add(new LegendPanel(useMapColor, getUnitForLegend()));
            addLegendPanel.revalidate();
            addLegendPanel.repaint();
        }
    }

    private void refreshFooter() {
        if (sectorText != null) {
            String mapSectorInfo = MapOptionsUtil.getSectorPlusInfo(jtable);
            sectorText.setText(buildFooterText(mapSectorInfo));
            sectorText.setVisible(mapSectorInfo.length() > 0);
            sectorText.setCaretPosition(0);
        }
        updateScaleStatusLabel();
    }

    private String getUnitForLegend() {
        int unitColIdx = FilteredTable.getColumnByName(jtable, "Units");
        if (unitColIdx < 0 || jtable.getRowCount() == 0) {
            return "";
        }
        Object unit = jtable.getValueAt(0, unitColIdx);
        return unit == null ? "" : unit.toString();
    }

    private void saveMap() {
        if (frame == null) {
            return;
        }
        BufferedImage image = captureFrameImageWithoutToolbar();
        if (image == null) {
            JOptionPane.showMessageDialog(frame, "Unable to capture the map image.", "Export Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file name to save the map");
        fileChooser.setFileFilter(new FileNameExtensionFilter("*.png", "png"));
        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile == null) {
                return;
            }
            if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
            }
            try {
                ImageIO.write(image, "png", selectedFile);
                String myString = "map is saved to " + selectedFile.getAbsolutePath();
                JOptionPane.showMessageDialog(frame, myString, "map is saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                System.out.println("Could not save map: " + e.toString());
                JOptionPane.showMessageDialog(frame, "Unable to save map, please see console for error", "Error Saving Map", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limitNumClassesAndChangeChoices() {
        paletteChoice = (String) comboBoxPalette.getSelectedItem();
        Integer[] newChoices = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        if (paletteChoice.equalsIgnoreCase("SEQUENTIAL")) {
            if (colorChoicePanel.getComponentCount() > 1) {
                colorChoicePanel.removeAll();
            }
            addSequentialColorChoices();
            comboBoxNumClasses.removeAllItems();
            for (Integer newChoice : newChoices) {
                comboBoxNumClasses.addItem(newChoice);
            }
            comboBoxNumClasses.setSelectedIndex(8);
        } else if (paletteChoice.equalsIgnoreCase("DIVERGING")) {
            if (colorChoicePanel.getComponentCount() > 1) {
                colorChoicePanel.removeAll();
            }
            addDivergingColorChoices();
            comboBoxNumClasses.removeAllItems();
            for (Integer numClass : numClasses) {
                comboBoxNumClasses.addItem(numClass);
            }
            comboBoxNumClasses.setSelectedIndex(chooseNumCombos() - 1);
        }
        colorChoicePanel.revalidate();
        colorChoicePanel.repaint();
    }

    private void addDivergingColorChoices() {
        choiceGroup = new ButtonGroup();
        colorChoicePanel.removeAll();
        JLabel paletteChoiceLabel = new JLabel("Palette:", SwingConstants.LEFT);
        paletteChoiceLabel.setFont(MAP_LABEL_FONT);
        paletteChoiceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorChoicePanel.add(paletteChoiceLabel);
        colorChoicePanel.add(Box.createVerticalStrut(4));
        comboBoxPaletteChoice = createPaletteChoiceComboBox(9, 4, DIVERGING_SWATCH_HEIGHT);
        colorChoicePanel.add(comboBoxPaletteChoice);
    }

    private void addSequentialColorChoices() {
        choiceGroup = new ButtonGroup();
        colorChoicePanel.removeAll();
        JLabel paletteChoiceLabel = new JLabel("Palette:", SwingConstants.LEFT);
        paletteChoiceLabel.setFont(MAP_LABEL_FONT);
        paletteChoiceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorChoicePanel.add(paletteChoiceLabel);
        colorChoicePanel.add(Box.createVerticalStrut(4));
        comboBoxPaletteChoice = createPaletteChoiceComboBox(8, 1, SEQUENTIAL_SWATCH_HEIGHT);
        colorChoicePanel.add(comboBoxPaletteChoice);
    }

    public void colorScaleOptions() {
        final JDialog colorDialog = new JDialog(frame, "Change map color scale", true);
        colorDialog.setSize(new Dimension(650, 300));
        colorDialog.getGlassPane().addMouseListener(new MouseAdapter() {});
        colorDialog.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Container contentPane = colorDialog.getContentPane();

        JPanel scalePane = new JPanel();
        scalePane.setBorder(new EmptyBorder(10, 10, 10, 10));
        scalePane.setLayout(new BoxLayout(scalePane, BoxLayout.Y_AXIS));
        JPanel minScalePane = new JPanel();
        minScalePane.setBorder(new EmptyBorder(5, 5, 5, 5));
        minScalePane.setLayout(new BoxLayout(minScalePane, BoxLayout.X_AXIS));
        JLabel minLabel = new JLabel("Min:", SwingConstants.LEFT);
        minLabel.setFont(MAP_LABEL_FONT);
        NumberFormat numClassFormat = NumberFormat.getNumberInstance();
        numClassFormat.setMaximumFractionDigits(5);
        minField = new JFormattedTextField(numClassFormat);
        minField.setFont(MAP_FIELD_FONT);
        minField.setColumns(7);
        minScalePane.add(minLabel);
        minScalePane.add(minField);
        JPanel maxScalePane = new JPanel();
        maxScalePane.setBorder(new EmptyBorder(5, 5, 5, 5));
        maxScalePane.setLayout(new BoxLayout(maxScalePane, BoxLayout.X_AXIS));
        JLabel maxLabel = new JLabel("Max:", SwingConstants.LEFT);
        maxLabel.setFont(MAP_LABEL_FONT);
        maxField = new JFormattedTextField(numClassFormat);
        maxField.setFont(MAP_FIELD_FONT);
        maxField.setColumns(7);
        maxScalePane.add(maxLabel);
        maxScalePane.add(maxField);
        if (intervalType == IntervalType.AUTOMATIC) {
            minField.setValue(minMaxFromTable[0]);
            maxField.setValue(minMaxFromTable[1]);
        } else if (intervalType == IntervalType.CUSTOM) {
            minField.setValue(previousMin);
            maxField.setValue(previousMax);
        }
        scalePane.add(maxScalePane, BorderLayout.PAGE_START);
        scalePane.add(minScalePane, BorderLayout.PAGE_END);

        JPanel buttonPane = new JPanel();
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(MAP_BUTTON_FONT);
        cancelButton.addActionListener(e -> {
            updateScaleStatusLabel();
            colorDialog.setVisible(false);
        });
        final JButton confirmButton = new JButton("Confirm");
        confirmButton.setFont(MAP_BUTTON_FONT);
        confirmButton.addActionListener(e -> {
            if (!(minField.getValue() instanceof Number) || !(maxField.getValue() instanceof Number)) {
                JOptionPane.showMessageDialog(colorDialog, "Please enter numeric min and max values.", "Invalid Scale", JOptionPane.WARNING_MESSAGE);
                return;
            }
            previousMin = ((Number) minField.getValue()).doubleValue();
            previousMax = ((Number) maxField.getValue()).doubleValue();
            if (previousMin > previousMax) {
                JOptionPane.showMessageDialog(colorDialog, "Min must be less than or equal to max.", "Invalid Scale", JOptionPane.WARNING_MESSAGE);
                return;
            }
            intervalType = IntervalType.CUSTOM;
            updateScaleStatusLabel();
            colorDialog.setVisible(false);
            redrawMap();
        });
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPane.add(confirmButton);
        buttonPane.add(Box.createHorizontalStrut(20));
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createHorizontalGlue());

        JPanel choiceHolderPane = new JPanel();
        choiceHolderPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        choiceHolderPane.setLayout(new BoxLayout(choiceHolderPane, BoxLayout.Y_AXIS));
        JPanel selPane = new JPanel();
        JLabel selLabel = new JLabel("Select:", SwingConstants.LEFT);
        selLabel.setFont(MAP_LABEL_FONT);
        selPane.add(selLabel);
        maxLabel.setFont(MAP_LABEL_FONT);
        JPanel choicePane = new JPanel();
        final JButton useLocalButton = new JButton("Within-year min/max");
        useLocalButton.setFont(MAP_BUTTON_FONT);
        useLocalButton.addActionListener(e -> {
            minMaxFromTable = getAbsMinMaxFromLocal(normalizeScale);
            minField.setValue(minMaxFromTable[0]);
            maxField.setValue(minMaxFromTable[1]);
        });
        final JButton useAllYearButton = new JButton("Across-year min/max");
        useAllYearButton.setFont(MAP_BUTTON_FONT);
        useAllYearButton.addActionListener(e -> {
            minMaxFromTable = getAbsMinMaxForAllYears(normalizeScale);
            minField.setValue(minMaxFromTable[0]);
            maxField.setValue(minMaxFromTable[1]);
        });
        final JButton useGlobalButton = new JButton("Global min/max");
        useGlobalButton.setFont(MAP_BUTTON_FONT);
        useGlobalButton.addActionListener(e -> {
            minMaxFromTable = MapOptionsUtil.getAbsMinMaxFromTable(jtable, normalizeScale);
            minField.setValue(minMaxFromTable[0]);
            maxField.setValue(minMaxFromTable[1]);
        });
        choicePane.setLayout(new BoxLayout(choicePane, BoxLayout.X_AXIS));
        choicePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        choicePane.add(useLocalButton);
        choicePane.add(Box.createHorizontalStrut(5));
        choicePane.add(useAllYearButton);
        choicePane.add(Box.createHorizontalStrut(5));
        choicePane.add(useGlobalButton);
        choicePane.add(Box.createHorizontalGlue());
        JPanel normPane = new JPanel();
        normPane.setLayout(new BoxLayout(normPane, BoxLayout.X_AXIS));
        normPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        final JCheckBox checkNormalizeScaleRatio = new JCheckBox("Normalize Scale Ratio");
        checkNormalizeScaleRatio.setSelected(normalizeScale);
        checkNormalizeScaleRatio.setFont(MAP_FIELD_FONT);
        checkNormalizeScaleRatio.addActionListener(e -> {
            normalizeScale = checkNormalizeScaleRatio.isSelected();
            updateScaleStatusLabel();
        });
        normPane.add(Box.createHorizontalStrut(5));
        normPane.add(checkNormalizeScaleRatio);
        normPane.add(Box.createHorizontalGlue());
        choiceHolderPane.add(selPane);
        choicePane.add(Box.createVerticalStrut(5));
        choiceHolderPane.add(choicePane);
        choicePane.add(Box.createVerticalStrut(5));
        choiceHolderPane.add(normPane);
        choicePane.add(Box.createVerticalGlue());
        contentPane.add(scalePane, BorderLayout.PAGE_START);
        contentPane.add(choiceHolderPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        colorDialog.setVisible(true);
    }

    public double[] getAbsMinMaxForAllYears(boolean normalizeScale) {
        if (yearListMenu == null || scenarioListMenu == null || scenarioListMenu.getSelectedItem() == null) {
            return defaultMinMax();
        }
        HashMap<String, Double> dataForState;
        double minD = Double.MAX_VALUE;
        double maxD = Double.MAX_VALUE * -1.0;
        for (int i = 0; i < yearListMenu.getItemCount(); i++) {
            Object yearItem = yearListMenu.getItemAt(i);
            if (yearItem == null) {
                continue;
            }
            String curItem = yearItem.toString();
            dataForState = MapOptionsUtil.getTableDataForStateOrCountry(jtable, curItem, (String) scenarioListMenu.getSelectedItem());
            if (dataForState == null || dataForState.isEmpty()) {
                continue;
            }
            double newMin = Collections.min(dataForState.values());
            double newMax = Collections.max(dataForState.values());
            if (newMin < minD) {
                minD = newMin;
            }
            if (newMax > maxD) {
                maxD = newMax;
            }
        }
        return minD == Double.MAX_VALUE ? defaultMinMax() : normalizeMinMax(minD, maxD, normalizeScale);
    }

    public double[] getAbsMinMaxFromLocal(boolean normalizeScale) {
        if (yearListMenu == null || scenarioListMenu == null
                || yearListMenu.getSelectedItem() == null || scenarioListMenu.getSelectedItem() == null) {
            return defaultMinMax();
        }
        HashMap<String, Double> dataForState = MapOptionsUtil.getTableDataForStateOrCountry(jtable, (String) yearListMenu.getSelectedItem(), (String) scenarioListMenu.getSelectedItem());
        if (dataForState == null || dataForState.isEmpty()) {
            return defaultMinMax();
        }
        double minD = Collections.min(dataForState.values());
        double maxD = Collections.max(dataForState.values());
        return normalizeMinMax(minD, maxD, normalizeScale);
    }

    private double[] normalizeMinMax(double min, double max, boolean normalizeScale) {
        double[] minMax = new double[2];
        if (min == max) {
            minMax[0] = min;
            minMax[1] = max + Math.max(0.1, 0.1 * min);
        } else if (max > 0 && min < 0 && normalizeScale) {
            if (Math.abs(min) >= max) {
                max = Math.abs(min);
            } else {
                min = -max;
            }
            minMax[0] = min;
            minMax[1] = max;
        } else {
            minMax[0] = min;
            minMax[1] = max;
        }
        return minMax;
    }

    private double[] getInitialMinMax(boolean normalizeScale) {
        if (jtable == null || yearListMenu == null || scenarioListMenu == null
                || yearListMenu.getSelectedItem() == null || scenarioListMenu.getSelectedItem() == null) {
            return defaultMinMax();
        }
        return getCurrentMinMax(normalizeScale);
    }

    private double[] getCurrentMinMax(boolean normalizeScale) {
        if (jtable == null || yearListMenu == null || scenarioListMenu == null
                || yearListMenu.getSelectedItem() == null || scenarioListMenu.getSelectedItem() == null) {
            return defaultMinMax();
        }
        boolean noRowSelected = jtable.getSelectionModel().isSelectionEmpty();
        if (noRowSelected) {
            double[] columnMinMax = MapOptionsUtil.getAbsMinMaxFromTableColumn(jtable, (String) yearListMenu.getSelectedItem(), normalizeScale);
            return isValidMinMax(columnMinMax) ? columnMinMax : defaultMinMax();
        }
        double[] allYearMinMax = getAbsMinMaxForAllYears(normalizeScale);
        return isValidMinMax(allYearMinMax) ? allYearMinMax : defaultMinMax();
    }

    private boolean isValidMinMax(double[] minMax) {
        return minMax != null && minMax.length >= 2 && !Double.isNaN(minMax[0]) && !Double.isNaN(minMax[1])
                && !Double.isInfinite(minMax[0]) && !Double.isInfinite(minMax[1]);
    }

    private double[] defaultMinMax() {
        return new double[] {0.0, 1.0};
    }

    @Override
    public void componentResized(ComponentEvent e) {}

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}

    public class UpdateMap extends JPanel implements ActionListener {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            redrawMap();
        }
    }

    public class RowSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() || frame == null || jtable == null) {
                return;
            }
            if (jtable.getSelectedRow() > -1 && frame.isDisplayable()) {
                redrawMap();
            } else if (jtable.getSelectedRow() > -1 && !frame.isDisplayable()) {
                initialize();
            }
        }
    }

    protected void configureMapPane(JMapPane mapPane) {
        mapMode.configureMapPane(mapPane);
        applyNavigationToolSelection();
    }

    protected MapContent createBoundaryMapLayer() {
        return mapMode.createBoundaryMap(this);
    }

    protected void redrawMap() {
        redrawMapInternal();
    }

    protected MapContent buildBoundaryMap(String mapLabel, String shpFilePath) {
        MapContent map = new MapContent();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = loadBoundaryFeatures(mapLabel, shpFilePath);
        if (featureCollection == null || featureCollection.size() == 0) {
            return map;
        }

        HashMap<String, Double> selectedTableData = getSelectedTableData();
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
        FeatureIterator<SimpleFeature> iterator = featureCollection.features();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                addFeatureLayer(map, featureCollection, feature, selectedTableData, filterFactory);
            }
        } finally {
            iterator.close();
        }
        return map;
    }

    protected FeatureCollection<SimpleFeatureType, SimpleFeature> loadBoundaryFeatures(String mapLabel, String shpFilePath) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = MapOptionsUtil.getCollectionFromShape(shpFilePath);
        if (featureCollection == null || featureCollection.size() == 0) {
            MapOptionsUtil.showOneTimeMappingWarning(mapLabel, shpFilePath);
            System.err.println(getMissingBoundaryMessage(shpFilePath));
            return null;
        }
        return featureCollection;
    }

    protected HashMap<String, Double> getSelectedTableData() {
        if (jtable == null || yearListMenu == null || scenarioListMenu == null
                || yearListMenu.getSelectedItem() == null || scenarioListMenu.getSelectedItem() == null) {
            return new HashMap<>();
        }
        try {
            return MapOptionsUtil.getTableDataForStateOrCountry(jtable, (String) yearListMenu.getSelectedItem(), (String) scenarioListMenu.getSelectedItem());
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    protected void addFeatureLayer(MapContent map,
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
            SimpleFeature feature,
            HashMap<String, Double> selectedTableData,
            FilterFactory filterFactory) {
        String regionPropertyValue = getRegionPropertyValue(feature);
        if (regionPropertyValue == null) {
            return;
        }
        logRegionProperty(regionPropertyValue);
        Color fillerColor = resolveFillColor(selectedTableData, regionPropertyValue);
        SimpleFeatureType type = feature.getType();
        FeatureLayer boundaryLayer = new FeatureLayer(featureCollection,
                SLD.createPolygonStyle(new Color(1, 1, 1), fillerColor, 0.9f));
        Filter filter = filterFactory.equals(filterFactory.property(getRegionPropertyName()),
                filterFactory.literal(feature.getProperty(getRegionPropertyName()).getValue()));
        boundaryLayer.setQuery(new Query(type.getName().getLocalPart(), filter));
        boundaryLayer.setVisible(true);
        map.addLayer(boundaryLayer);
    }

    protected String getRegionPropertyValue(SimpleFeature feature) {
        Collection<Property> properties = feature.getProperties();
        for (Property property : properties) {
            String propName = property.getName().getLocalPart();
            if (getRegionPropertyName().equals(propName) && property.getValue() != null) {
                return property.getValue().toString();
            }
        }
        return null;
    }

    protected Color resolveFillColor(HashMap<String, Double> selectedTableData, String propertyVal) {
        if (selectedTableData == null) {
            return null;
        }
        Double value = selectedTableData.get(propertyVal);
        return value == null ? null : MapOptionsUtil.findStateColorFromMapColor(useMapColor, value);
    }

    protected String getRegionPropertyName() {
        return "subRegn";
    }

    protected void logRegionProperty(String propertyVal) {
        mapMode.logRegionProperty(propertyVal);
    }

    protected String getMissingBoundaryMessage(String shpFilePath) {
        return mapMode.getMissingBoundaryMessage(shpFilePath);
    }

    private JPanel createControlSectionPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JComboBox<PaletteChoiceOption> createPaletteChoiceComboBox(int paletteCount, int defaultIndex, int swatchHeight) {
        DefaultComboBoxModel<PaletteChoiceOption> paletteModel = new DefaultComboBoxModel<>();
        String paletteTypeName = comboBoxPalette != null && comboBoxPalette.getSelectedItem() != null
                ? comboBoxPalette.getSelectedItem().toString()
                : "DIVERGING";
        for (int i = 0; i < paletteCount; i++) {
            PaletteChoiceOption option = createPaletteChoiceOption(paletteTypeName, i);
            paletteModel.addElement(option);
            JRadioButton radioButton = new JRadioButton(String.valueOf(i + 1));
            if (i == defaultIndex) {
                radioButton.setSelected(true);
            }
            choiceGroup.add(radioButton);
        }
        JComboBox<PaletteChoiceOption> paletteChoiceBox = new JComboBox<>(paletteModel);
        paletteChoiceBox.setFont(MAP_FIELD_FONT);
        paletteChoiceBox.setRenderer(new PaletteChoiceRenderer());
        paletteChoiceBox.setMaximumSize(new Dimension(PALETTE_COMBO_WIDTH, 28));
        paletteChoiceBox.setPreferredSize(new Dimension(PALETTE_COMBO_WIDTH, 28));
        paletteChoiceBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        paletteChoiceBox.setSelectedIndex(defaultIndex);
        paletteChoiceBox.setToolTipText("Choose a color ramp preview.");
        paletteChoiceBox.addActionListener(e -> {
            Object selectedItem = paletteChoiceBox.getSelectedItem();
            if (!(selectedItem instanceof PaletteChoiceOption)) {
                return;
            }
            PaletteChoiceOption option = (PaletteChoiceOption) selectedItem;
            setSelectedChoiceButton(option.getPaletteIndex());
            redrawMap();
        });
        setSelectedChoiceButton(defaultIndex);
        return paletteChoiceBox;
    }

    private PaletteChoiceOption createPaletteChoiceOption(String paletteTypeName, int paletteIndex) {
        boolean sequential = "SEQUENTIAL".equalsIgnoreCase(paletteTypeName);
        int previewClassCount = sequential ? 8 : 10;
        MapColorPalette palette = MapColorPalette.getMapColorPalette(paletteTypeName, paletteIndex, previewClassCount, false);
        String paletteKind = sequential ? "Sequential" : "Diverging";
        String description = palette.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = paletteKind + " palette";
        }
        return new PaletteChoiceOption(paletteIndex, description, palette.getColors());
    }

    private void setSelectedChoiceButton(int selectedIndex) {
        if (choiceGroup == null || selectedIndex < 0) {
            return;
        }
        int idx = 0;
        for (java.util.Enumeration<javax.swing.AbstractButton> buttons = choiceGroup.getElements(); buttons.hasMoreElements(); idx++) {
            javax.swing.AbstractButton button = buttons.nextElement();
            button.setSelected(idx == selectedIndex);
        }
        MapOptionsUtil.resetColorForNonSelectedButtons(choiceGroup);
    }

    private static Font resolveUiFont(int style, int fallbackSize) {
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            return new Font("Dialog", style, fallbackSize);
        }
        return baseFont.deriveFont(style, fallbackSize);
    }

    private void styleActionButton(JButton button) {
        if (button == null) {
            return;
        }
        button.setFont(MAP_BUTTON_FONT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFocusPainted(false);
    }

    private JComponent createNavigationButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navigationGroup = new ButtonGroup();

        JToggleButton panButton = createNavigationToggle("Pan", true, () -> {
            if (jmap != null) {
                jmap.setCursorTool(new PanTool());
            }
        });
        JToggleButton zoomInButton = createNavigationToggle("Zoom In", false, () -> {
            if (jmap != null) {
                jmap.setCursorTool(new ZoomInTool());
            }
        });
        JToggleButton zoomOutButton = createNavigationToggle("Zoom Out", false, () -> {
            if (jmap != null) {
                jmap.setCursorTool(new ZoomOutTool());
            }
        });

        buttonPanel.add(panButton);
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(zoomInButton);
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(zoomOutButton);
        return buttonPanel;
    }

    private JToggleButton createNavigationToggle(String label, boolean selected, Runnable onSelect) {
        JToggleButton button = new JToggleButton(label);
        button.setFont(MAP_BUTTON_FONT);
        button.setFocusPainted(false);
        button.setSelected(selected);
        button.addActionListener(e -> {
            if (button.isSelected()) {
                onSelect.run();
            }
        });
        navigationGroup.add(button);
        return button;
    }

    private void applyNavigationToolSelection() {
        if (navigationGroup == null || jmap == null) {
            return;
        }
        for (java.util.Enumeration<javax.swing.AbstractButton> buttons = navigationGroup.getElements(); buttons.hasMoreElements();) {
            javax.swing.AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                button.doClick(0);
                return;
            }
        }
    }

    private void resetMapView() {
        if (jmap == null || stateMap == null || stateMap.getViewport() == null) {
            return;
        }
        try {
            ReferencedEnvelope bounds = stateMap.getMaxBounds();
            if (bounds != null) {
                stateMap.getViewport().setBounds(bounds);
                jmap.repaint();
            }
        } catch (Exception ignored) {
            // keep reset low-risk and best-effort
        }
    }

    private String buildFooterText(String mapSectorInfo) {
        return "Displayed in this map: " + mapSectorInfo;
    }

    private void updateScaleStatusLabel() {
        if (scaleStatusLabel == null) {
            return;
        }
        String intervalLabel = intervalType == IntervalType.CUSTOM ? "Custom range" : "Automatic range";
        String normalizeLabel = normalizeScale ? "normalized" : "raw";
        scaleStatusLabel.setText(intervalLabel + " (" + normalizeLabel + ")");
        scaleStatusLabel.setToolTipText("Automatic uses the current data range. Custom uses the values set in Modify Color Scale.");
    }

    private void installMapContextMenu(JMapPane mapPane) {
        if (mapPane == null) {
            return;
        }
        mapContextMenu = new JPopupMenu();
        JMenuItem copyMapItem = new JMenuItem("Copy");
        copyMapItem.addActionListener(e -> copyMapImageToClipboard());
        mapContextMenu.add(copyMapItem);
        JMenuItem exportMapItem = new JMenuItem("Export");
        exportMapItem.addActionListener(e -> saveMap());
        mapContextMenu.add(exportMapItem);
        mapContextMenu.addSeparator();
        JMenuItem resetMapItem = new JMenuItem("Reset");
        resetMapItem.addActionListener(e -> resetMapView());
        mapContextMenu.add(resetMapItem);
        mapPane.setComponentPopupMenu(mapContextMenu);
    }

    private void copyMapImageToClipboard() {
        if (frame == null) {
            return;
        }
        BufferedImage image = makeClipboardSafeImage(captureMapPaneImage());
        if (image == null) {
            JOptionPane.showMessageDialog(frame, "Unable to capture the map image.", "Copy Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            clipboard.setContents(new ImageSelection(image), null);
            JOptionPane.showMessageDialog(frame, "Map image copied to clipboard.", "Copy", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException ex) {
            logClipboardUnavailable("Direct clipboard image copy unavailable", image, ex);
            JOptionPane.showMessageDialog(frame, "Clipboard is currently unavailable. Please try again.", "Copy Error", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Direct clipboard image copy failed for map image {0}; attempting PNG fallback. Cause: {1}: {2}",
                    new Object[] { describeImage(image), ex.getClass().getName(), ex.getMessage() });
            if (!copyPngFallbackToClipboard(clipboard, image, ex)) {
                throw ex;
            }
        }
    }

    private boolean copyPngFallbackToClipboard(Clipboard clipboard, BufferedImage image, RuntimeException originalException) {
        if (PNG_INPUT_STREAM_FLAVOR == null) {
            LOGGER.log(Level.WARNING,
                    "PNG clipboard fallback is unavailable for map image {0} after direct copy failure.",
                    describeImage(image));
            return false;
        }
        try {
            clipboard.setContents(new PngClipboardSelection(image), null);
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO,
                        "Recovered clipboard copy using PNG fallback for map image {0} after {1}: {2}",
                        new Object[] { describeImage(image), originalException.getClass().getName(), originalException.getMessage() });
            }
            JOptionPane.showMessageDialog(frame,
                    "Map image copied to clipboard. If direct paste fails in one app, try a PNG-aware target such as Paint or Office.",
                    "Copy",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IllegalStateException ex) {
            logClipboardUnavailable("PNG clipboard fallback unavailable", image, ex);
            JOptionPane.showMessageDialog(frame, "Clipboard is currently unavailable. Please try again.", "Copy Error", JOptionPane.WARNING_MESSAGE);
            return true;
        } catch (IOException ex) {
            originalException.addSuppressed(ex);
            LOGGER.log(Level.WARNING,
                    "PNG clipboard fallback failed for map image {0} after direct copy failure.",
                    describeImage(image));
            LOGGER.log(Level.FINE, "Direct clipboard failure cause", originalException);
            LOGGER.log(Level.WARNING, "PNG fallback I/O failure", ex);
            return false;
        } catch (RuntimeException ex) {
            originalException.addSuppressed(ex);
            LOGGER.log(Level.WARNING,
                    "PNG clipboard fallback failed for map image {0} after direct copy failure.",
                    describeImage(image));
            LOGGER.log(Level.FINE, "Direct clipboard failure cause", originalException);
            LOGGER.log(Level.WARNING, "PNG fallback runtime failure", ex);
            return false;
        }
    }

    private void logClipboardUnavailable(String message, BufferedImage image, IllegalStateException ex) {
        LOGGER.log(Level.WARNING,
                "{0} for map image {1}: {2}",
                new Object[] { message, describeImage(image), ex.getMessage() });
    }

    private String describeImage(BufferedImage image) {
        if (image == null) {
            return "<null>";
        }
        return image.getWidth() + "x" + image.getHeight() + " type=" + image.getType();
    }

    private BufferedImage makeClipboardSafeImage(BufferedImage sourceImage) {
        if (sourceImage == null) {
            return null;
        }
        BufferedImage bgrImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = bgrImage.createGraphics();
        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bgrImage.getWidth(), bgrImage.getHeight());
            g2d.drawImage(sourceImage, 0, 0, null);
            return bgrImage;
        } finally {
            g2d.dispose();
        }
    }

    private BufferedImage captureMapPaneImage() {
        if (jmap == null) {
            return null;
        }
        return captureComponentImage(jmap);
    }

    private BufferedImage captureFrameImageWithoutToolbar() {
        if (frame == null) {
            return null;
        }
        boolean toolBarWasAttached = toolBar != null && toolBar.getParent() == frame.getContentPane();
        try {
            if (toolBarWasAttached) {
                frame.getContentPane().remove(toolBar);
                frame.revalidate();
                frame.repaint();
            }
            return captureComponentImage(frame);
        } finally {
            if (toolBarWasAttached) {
                frame.getContentPane().add(toolBar, BorderLayout.WEST);
                frame.revalidate();
                frame.repaint();
            }
        }
    }

    private BufferedImage captureComponentImage(Component component) {
        if (component == null) {
            return null;
        }
        int width = Math.max(component.getWidth(), 1);
        int height = Math.max(component.getHeight(), 1);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            component.printAll(g2d);
            return image;
        } finally {
            g2d.dispose();
        }
    }

    private static DataFlavor createPngInputStreamFlavor() {
        return new DataFlavor("image/png;class=java.io.InputStream", "PNG Image");
    }

    private static final DataFlavor PNG_INPUT_STREAM_FLAVOR = createPngInputStreamFlavor();

    private static final class ImageSelection implements Transferable {
        private static final DataFlavor[] FLAVORS = PNG_INPUT_STREAM_FLAVOR == null
                ? new DataFlavor[] { DataFlavor.imageFlavor }
                : new DataFlavor[] { PNG_INPUT_STREAM_FLAVOR, DataFlavor.imageFlavor };

        private final BufferedImage image;
        private byte[] pngBytes;

        private ImageSelection(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (DataFlavor.imageFlavor.equals(flavor)) {
                return true;
            }
            return PNG_INPUT_STREAM_FLAVOR != null && PNG_INPUT_STREAM_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (DataFlavor.imageFlavor.equals(flavor)) {
                return image;
            }
            if (PNG_INPUT_STREAM_FLAVOR != null && PNG_INPUT_STREAM_FLAVOR.equals(flavor)) {
                return new ByteArrayInputStream(getPngBytes());
            }
            throw new UnsupportedFlavorException(flavor);
        }

        protected final byte[] getPngBytes() throws IOException {
            if (pngBytes == null) {
                ByteArrayOutputStream pngBuffer = new ByteArrayOutputStream();
                ImageIO.write(image, "png", pngBuffer);
                pngBytes = pngBuffer.toByteArray();
            }
            return pngBytes.clone();
        }
    }

    private static final class PngClipboardSelection implements Transferable, ClipboardOwner {
        private static final DataFlavor[] FLAVORS = new DataFlavor[] { PNG_INPUT_STREAM_FLAVOR };

        private final byte[] pngBytes;

        private PngClipboardSelection(BufferedImage image) throws IOException {
            ByteArrayOutputStream pngBuffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", pngBuffer);
            pngBytes = pngBuffer.toByteArray();
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return PNG_INPUT_STREAM_FLAVOR != null && PNG_INPUT_STREAM_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return new ByteArrayInputStream(pngBytes);
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // no-op
        }
    }

    private static final class PaletteChoiceOption {
        private final int paletteIndex;
        private final String description;
        private final Color[] previewColors;

        private PaletteChoiceOption(int paletteIndex, String description, Color[] previewColors) {
            this.paletteIndex = paletteIndex;
            this.description = description;
            this.previewColors = previewColors == null ? new Color[0] : previewColors.clone();
        }

        private int getPaletteIndex() {
            return paletteIndex;
        }

        private int getChoiceNumber() {
            return paletteIndex + 1;
        }

        private String getDescription() {
            return description;
        }

        private Color[] getPreviewColors() {
            return previewColors.clone();
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static final class PalettePreviewIcon implements Icon {
        private final Color[] colors;
        private final int width;
        private final int height;

        private PalettePreviewIcon(Color[] colors, int width, int height) {
            this.colors = colors == null ? new Color[0] : colors.clone();
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x, y, width, height, 6, 6);
                if (colors.length > 0) {
                    int segmentWidth = Math.max(1, width / colors.length);
                    for (int i = 0; i < colors.length; i++) {
                        int swatchX = x + (i * segmentWidth);
                        int swatchWidth = (i == colors.length - 1) ? (x + width - swatchX) : segmentWidth;
                        g2d.setColor(colors[i]);
                        g2d.fillRect(swatchX, y + 1, swatchWidth, Math.max(1, height - 2));
                    }
                }
                g2d.setColor(new Color(150, 150, 150));
                g2d.drawRoundRect(x, y, width - 1, height - 1, 6, 6);
            } finally {
                g2d.dispose();
            }
        }
    }

    private static final class PaletteChoiceRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PaletteChoiceOption) {
                PaletteChoiceOption option = (PaletteChoiceOption) value;
                label.setText(" ");
                label.setIcon(new PalettePreviewIcon(option.getPreviewColors(), PALETTE_PREVIEW_WIDTH, PALETTE_PREVIEW_HEIGHT));
                label.setFont(MAP_FIELD_FONT);
                label.setIconTextGap(0);
            }
            return label;
        }
    }
}
