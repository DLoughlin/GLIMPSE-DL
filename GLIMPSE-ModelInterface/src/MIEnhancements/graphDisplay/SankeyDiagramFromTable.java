package graphDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.flow.FlowPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.flow.DefaultFlowDataset;
import org.jfree.data.flow.NodeKey;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;

import chart.LegendUtil;
import chartOptions.FileUtil;

import filter.FilteredTable;
import mapOptions.MapOptionsUtil;

/**
 * Displays a Sankey Diagram or Stacked Bar Chart constructed from a query result table.
 * Allows user selection of scenario, region, and year to filter data.
 *
 * Author: Yadong
 * Date: September/03/2024
 */
public class SankeyDiagramFromTable extends JFrame implements ComponentListener {
    private static final long serialVersionUID = 1L;
    private String chartName;
    private JTable jtable;
    private JFrame frame;
    private JToolBar toolBar;
    private JPanel scenarioMenuPanel;
    private JPanel regionMenuPanel;
    private JPanel yearMenuPanel;
    private JPanel sankeyPanel;
    private JPanel sankeyLabelPanel;
    private JPanel barChartPanel;
    private JPanel summaryPanel;
    private JButton nextYearButton;
    private JButton prevYearButton;
    private CategoryPlot barPlot;
    private LegendItemCollection barLegendItems = null;
    private String bundlePath = null;
    private StackedBarRenderer barRenderer;
    private FlowPlot myPlot;
    private Set<NodeKey> mySet;

    private JLabel scenarioListLabel;
    private JLabel regionListLabel;
    private JLabel listLabel;
    private JComboBox<String> scenarioListMenu;
    private JComboBox<String> regionListMenu;
    private JComboBox<String> yearListMenu;
    private double defaultNodeWidth = 200.0;
    private double defaultNodeMargin = 0.02;
    private boolean replaceWithBarChart = false;
    protected boolean debug = false;

    /**
     * Constructor for SankeyDiagramFromTable
     * @param chartName Name of the chart
     * @param jtable Table containing query results
     * @throws ClassNotFoundException
     */
    public SankeyDiagramFromTable(String chartName, JTable jtable) throws ClassNotFoundException {
        this.chartName = chartName;
        this.jtable = jtable;
        initialize();
    }

    /**
     * Initializes the frame and UI components
     */
    private void initialize() {
        frame = new JFrame("Sankey for " + chartName);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        bundlePath = InterfaceMain.legendBundlesLoc;
        int regionIdx = FilteredTable.getColumnByName(jtable, "region");
        ArrayList<String> yearList = FilteredTable.getYearListFromTableData(jtable);
        int firstYearIdx = FilteredTable.getColumnByName(jtable, yearList.get(0));
        // If only one column between region and year, use bar chart
        if (firstYearIdx - regionIdx == 2) {
            replaceWithBarChart = true;
        }
        frame.getContentPane().add(createToolBar(), BorderLayout.WEST);
        if (replaceWithBarChart) {
            frame.getContentPane().add(createStackedBarPlot(), BorderLayout.CENTER);
        } else {
            frame.getContentPane().add(createSankeyPlot(), BorderLayout.CENTER);
        }
        frame.getContentPane().add(createSummary(), BorderLayout.EAST);
        frame.validate();
        frame.pack();
        Dimension preferredD = new Dimension(1200, 800);
        frame.setSize(preferredD);
        frame.setMinimumSize(new Dimension(500, 300));
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        DbViewer.openWindows.add(frame);
    }

    /**
     * Creates the left toolbar with scenario, region, and year selectors
     * @return JComponent toolbar
     */
    protected JComponent createToolBar() {
        toolBar = new JToolBar();
        toolBar.setBackground(Color.LIGHT_GRAY);
        toolBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        toolBar.setLayout(new GridLayout(10, 1));
        toolBar.setFloatable(false);

        // Scenario dropdown
        scenarioMenuPanel = new JPanel();
        scenarioMenuPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        scenarioMenuPanel.setLayout(new BoxLayout(scenarioMenuPanel, BoxLayout.Y_AXIS));
        scenarioMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        scenarioListLabel = new JLabel("Scenario:", SwingConstants.LEFT);
        scenarioListLabel.setFont(resolveUiFont("Label.font", Font.BOLD, 4));
        scenarioListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        List<String> scenarioListFromTable = MapOptionsUtil.getScenarioListFromTableData(jtable);
        DefaultComboBoxModel<String> dmlScenario = new DefaultComboBoxModel<String>();
        for (String scenario : scenarioListFromTable) {
            dmlScenario.addElement(scenario);
        }
        scenarioListMenu = new JComboBox<String>();
        scenarioListMenu.setModel(dmlScenario);
        scenarioListMenu.setVisible(true);
        scenarioListMenu.setFont(resolveUiFont("ComboBox.font", Font.BOLD, 2));
        scenarioListMenu.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        scenarioListMenu.setMaximumSize(new Dimension(300, 25));
        scenarioListMenu.addActionListener(new UpdateSankeyOrBarChart());
        scenarioMenuPanel.add(scenarioListLabel);
        scenarioMenuPanel.add(scenarioListMenu);
        toolBar.add(scenarioMenuPanel);

        // Region dropdown
        regionMenuPanel = new JPanel();
        regionMenuPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        regionMenuPanel.setLayout(new BoxLayout(regionMenuPanel, BoxLayout.X_AXIS));
        regionMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        regionListLabel = new JLabel("Region:", SwingConstants.LEFT);
        regionListLabel.setFont(resolveUiFont("Label.font", Font.BOLD, 4));
        regionListLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        List<String> regionListFromTable = MapOptionsUtil.getUniqueRegionsInTable(jtable);
        DefaultComboBoxModel<String> dmlRegion = new DefaultComboBoxModel<String>();
        for (String region : regionListFromTable) {
            dmlRegion.addElement(region);
        }
        regionListMenu = new JComboBox<String>();
        regionListMenu.setModel(dmlRegion);
        regionListMenu.setVisible(true);
        regionListMenu.setFont(resolveUiFont("ComboBox.font", Font.BOLD, 2));
        regionListMenu.setMaximumSize(new Dimension(100, 25));
        regionListMenu.addActionListener(new UpdateSankeyOrBarChart());
        regionMenuPanel.add(regionListLabel);
        regionMenuPanel.add(regionListMenu);
        toolBar.add(regionMenuPanel);

        // Year dropdown
        yearMenuPanel = new JPanel();
        yearMenuPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        yearMenuPanel.setLayout(new BoxLayout(yearMenuPanel, BoxLayout.X_AXIS));
        yearMenuPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        listLabel = new JLabel("Year:", SwingConstants.LEFT);
        listLabel.setFont(resolveUiFont("Label.font", Font.BOLD, 4));
        listLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        ArrayList<String> yearListFromTable = MapOptionsUtil.getYearListFromTableData(jtable);
        DefaultComboBoxModel<String> dml = new DefaultComboBoxModel<String>();
        for (String year : yearListFromTable) {
            dml.addElement(year);
        }
        yearListMenu = new JComboBox<String>();
        yearListMenu.setModel(dml);
        yearListMenu.setVisible(true);
        yearListMenu.setFont(resolveUiFont("ComboBox.font", Font.BOLD, 2));
        yearListMenu.setMaximumSize(new Dimension(150, 25));
        yearListMenu.addActionListener(new UpdateSankeyOrBarChart());

        // Next/Prev year buttons
        nextYearButton = new JButton(">");
        nextYearButton.addActionListener(e -> {
            int y = yearListMenu.getSelectedIndex();
            if (y < yearListMenu.getModel().getSize() - 1) {
                yearListMenu.setSelectedIndex(y + 1);
            }
        });
        nextYearButton.setVisible(true);
        prevYearButton = new JButton("<");
        prevYearButton.addActionListener(e -> {
            int y = yearListMenu.getSelectedIndex();
            if (y > 0) {
                yearListMenu.setSelectedIndex(y - 1);
            }
        });
        prevYearButton.setVisible(true);

        yearMenuPanel.add(listLabel);
        yearMenuPanel.add(prevYearButton);
        yearMenuPanel.add(yearListMenu);
        yearMenuPanel.add(nextYearButton);
        toolBar.add(yearMenuPanel);
        return toolBar;
    }

    /**
     * Creates the Sankey plot panel
     * @return JComponent Sankey panel
     */
    protected JComponent createSankeyPlot() {
        String selectedScenario = (String) scenarioListMenu.getSelectedItem();
        String selectedRegion = (String) regionListMenu.getSelectedItem();
        String selectedYear = (String) yearListMenu.getSelectedItem();
        DefaultFlowDataset myDataset = createFlowDatasetFromTable(jtable, selectedScenario, selectedRegion, selectedYear);
        mySet = myDataset.getAllNodes();

        sankeyPanel = new JPanel();
        sankeyPanel.setLayout(new BoxLayout(sankeyPanel, BoxLayout.Y_AXIS));
        sankeyPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sankeyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        myPlot = new FlowPlot(myDataset);
        myPlot.setNodeLabelOffsetX(-170.0);
        myPlot.setNodeLabelOffsetY(-170.0);
        myPlot.setNodeWidth(defaultNodeWidth);
        myPlot.setNodeMargin(defaultNodeMargin);
        myPlot.setDefaultNodeLabelFont(resolveUiFont("Label.font", Font.BOLD, 4));
        myPlot.setOutlineVisible(true);

        setFlowPlotColorFromBundle();

        String chartTitle = chartName + " for " + selectedRegion + " in " + selectedYear;
        JFreeChart chart = new JFreeChart(chartTitle, myPlot);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.validate();
        chartPanel.setPreferredSize(new Dimension(800, 600));
        sankeyPanel.add(chartPanel, BorderLayout.CENTER);

        // Node labels below chart
        sankeyLabelPanel = new JPanel();
        sankeyLabelPanel.setLayout(new BoxLayout(sankeyLabelPanel, BoxLayout.X_AXIS));
        sankeyLabelPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sankeyLabelPanel.setMaximumSize(new Dimension(10000, 100));
        sankeyLabelPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        int regionIdx = FilteredTable.getColumnByName(jtable, "region");
        ArrayList<String> yearList = FilteredTable.getYearListFromTableData(jtable);
        if (yearList.isEmpty() || regionIdx < 0) {
            sankeyPanel.add(sankeyLabelPanel);
            return sankeyPanel;
        }
        int firstYearIdx = FilteredTable.getColumnByName(jtable, yearList.get(0));
        int totalNodes = firstYearIdx - regionIdx - 1;
        if (totalNodes <= 0) {
            sankeyPanel.add(sankeyLabelPanel);
            return sankeyPanel;
        }
        int labelWidth = (int) Math.round(4 * myPlot.getNodeWidth() / totalNodes);
        if (totalNodes == 3) {
            labelWidth = (int) Math.round(2 * myPlot.getNodeWidth() / totalNodes);
        } else if (totalNodes >= 4) {
            labelWidth = (int) Math.round(myPlot.getNodeWidth() / totalNodes);
        }
        labelWidth = Math.max(labelWidth, 80);
        for (int i = 0; i < totalNodes; i++) {
            String nextNode = jtable.getColumnName(firstYearIdx - i - 1);
            JTextField nextNodeFromColumn = new JTextField(nextNode);
            nextNodeFromColumn.setFont(resolveUiFont("TextField.font", Font.BOLD, 4));
            nextNodeFromColumn.setPreferredSize(new Dimension(labelWidth, 30));
            nextNodeFromColumn.setMaximumSize(new Dimension(labelWidth, 30));
            nextNodeFromColumn.setHorizontalAlignment(JTextField.CENTER);
            nextNodeFromColumn.setEditable(false);
            nextNodeFromColumn.setBackground(Color.GRAY);
            sankeyLabelPanel.add(nextNodeFromColumn);
        }
        sankeyPanel.add(sankeyLabelPanel);
        return sankeyPanel;
    }

    /**
     * Creates the stacked bar chart panel
     * @return JComponent bar chart panel
     */
    protected JComponent createStackedBarPlot() {
        barChartPanel = new JPanel();
        barChartPanel.setLayout(new BoxLayout(barChartPanel, BoxLayout.Y_AXIS));
        barChartPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        barChartPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        String selectedScenario = (String) scenarioListMenu.getSelectedItem();
        String selectedRegion = (String) regionListMenu.getSelectedItem();
        String selectedYear = (String) yearListMenu.getSelectedItem();
        int unitColIdx = FilteredTable.getColumnByName(jtable, "Units");
        String unitForYAxis = (String) jtable.getValueAt(0, unitColIdx);
        DefaultCategoryDataset myDataset = createCategoryDatasetFromTable(jtable, selectedScenario, selectedRegion, selectedYear);
        JFreeChart barChart = ChartFactory.createStackedBarChart("", "", unitForYAxis, myDataset, PlotOrientation.VERTICAL, true, true, false);
        LegendTitle legend = barChart.getLegend();
        legend.setItemFont(resolveUiFont("Label.font", Font.BOLD, 2));

        barPlot = (CategoryPlot) barChart.getPlot();
        barPlot.setDataset(0, myDataset);
        barRenderer = (StackedBarRenderer) barPlot.getRenderer();
        barRenderer.setMaximumBarWidth(0.2);
        barRenderer.setDefaultItemLabelFont(resolveUiFont("Label.font", Font.BOLD, 4));
        barRenderer.setSeriesItemLabelFont(0, resolveUiFont("Label.font", Font.BOLD, 4));
        barRenderer.setBarPainter(new StandardBarPainter()); // Remove shine

        if (bundlePath != null) {
            setLegendColorFromBundle();
        } else {
            barPlot.setRenderer(0, barRenderer);
        }

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.validate();
        chartPanel.setPreferredSize(new Dimension(800, 600));
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        return barChartPanel;
    }

    /**
     * Sets legend colors for bar chart from bundle file
     */
    private void setLegendColorFromBundle() {
        Object[] temp = getLegendInfoFromProperties(bundlePath);
        if (temp == null || temp.length == 0) return;
        String queryNameForChart = '"' + chartName + '"';
        barLegendItems = barPlot.getLegendItems();
        String[] legends = new String[barLegendItems.getItemCount()];
        if (barLegendItems != null) {
            for (int i = 0; i < barLegendItems.getItemCount(); i++) {
                LegendItem item = barLegendItems.get(i);
                legends[i] = item.getLabel();
            }
        }
        String[] tempStr = readLegendItemsFromProperties();
        String[] queryStr = readQueryInfoFromProperties();
        for (int i = 0; i < legends.length; i++) {
            int idx = Arrays.asList(tempStr).indexOf(legends[i].trim());
            int idx_last = Arrays.asList(tempStr).lastIndexOf(legends[i].trim());
            String[] o = new String[4];
            if (idx > -1 & idx == idx_last) {
                String queryNameInLine = Arrays.asList(queryStr).get(idx);
                if (queryNameInLine.equals(queryNameForChart)) {
                    o = ((String) temp[idx]).split("=")[1].split(",");
                    int barFillColor = Integer.valueOf(o[0].trim());
                    Color myColor = LegendUtil.getRGB(barFillColor);
                    barRenderer.setSeriesPaint(i, myColor);
                    if (debug) System.out.println("setLegendColorFromBundle: use color: " + myColor);
                } else if (queryNameInLine.equals("*")) {
                    o = ((String) temp[idx]).split("=")[1].split(",");
                    int barFillColor = Integer.valueOf(o[0].trim());
                    Color myColor = LegendUtil.getRGB(barFillColor);
                    barRenderer.setSeriesPaint(i, myColor);
                }
            } else if (idx > -1 & idx != idx_last) {
                for (int idxN = idx; idxN <= idx_last; idxN++) {
                    String queryNameAtIdx = Arrays.asList(queryStr).get(idxN);
                    String legendInFile = Arrays.asList(tempStr).get(idxN);
                    if (queryNameAtIdx.equals(queryNameForChart) & legendInFile.equals(legends[i].trim())) {
                        o = ((String) temp[idxN]).split("=")[1].split(",");
                        int barFillColor = Integer.valueOf(o[0].trim());
                        Color myColor = LegendUtil.getRGB(barFillColor);
                        barRenderer.setSeriesPaint(i, myColor);
                    } else if (queryNameAtIdx.equals("*") & legendInFile.equals(legends[i].trim())) {
                        o = ((String) temp[idxN]).split("=")[1].split(",");
                        int barFillColor = Integer.valueOf(o[0].trim());
                        Color myColor = LegendUtil.getRGB(barFillColor);
                        barRenderer.setSeriesPaint(i, myColor);
                    }
                }
            }
        }
        barPlot.setRenderer(0, barRenderer);
    }

    /**
     * Sets node colors for Sankey plot from bundle file
     */
    private void setFlowPlotColorFromBundle() {
        Object[] temp = getLegendInfoFromProperties(bundlePath);
        if (temp == null || temp.length == 0 || mySet == null || myPlot == null) return;
        String queryNameForChart = '"' + chartName + '"';
        String[] tempStr = readLegendItemsFromProperties();
        String[] queryStr = readQueryInfoFromProperties();
        Iterator<NodeKey> nodeIterator = mySet.iterator();
        while (nodeIterator.hasNext()) {
            NodeKey myKey = nodeIterator.next();
            int idx = Arrays.asList(tempStr).indexOf(myKey.getNode().toString());
            int idx_last = Arrays.asList(tempStr).lastIndexOf(myKey.getNode().toString());
            String[] o = new String[4];
            if (idx > -1 & idx == idx_last) {
                if (queryStr == null) return;
                String queryNameInLine = Arrays.asList(queryStr).get(idx);
                if (queryNameInLine == null) return;
                if (queryNameInLine.equals(queryNameForChart)) {
                    o = ((String) temp[idx]).split("=")[1].split(",");
                    Color useThisColor = LegendUtil.getRGB(Integer.valueOf(o[0].trim()));
                    myPlot.setNodeFillColor(myKey, useThisColor);
                    if (debug) System.out.println("setFlowPlotColorFromBundle: use color: " + useThisColor);
                } else if (queryNameInLine.equals("*")) {
                    o = ((String) temp[idx]).split("=")[1].split(",");
                    Color useThisColor = LegendUtil.getRGB(Integer.valueOf(o[0].trim()));
                    myPlot.setNodeFillColor(myKey, useThisColor);
                }
            } else if (idx > -1 & idx != idx_last) {
                for (int idxN = idx; idxN <= idx_last; idxN++) {
                    if (queryStr == null) return;
                    String queryNameAtIdx = Arrays.asList(queryStr).get(idxN);
                    if (tempStr == null) return;
                    String legendInFile = Arrays.asList(tempStr).get(idxN);
                    if (legendInFile == null || queryNameAtIdx == null) return;
                    if (queryNameAtIdx.equals(queryNameForChart) & legendInFile.equals(myKey.getNode().toString())) {
                        o = ((String) temp[idxN]).split("=")[1].split(",");
                        Color useThisColor = LegendUtil.getRGB(Integer.valueOf(o[0].trim()));
                        myPlot.setNodeFillColor(myKey, useThisColor);
                    } else if (queryNameAtIdx.equals("*") & legendInFile.equals(myKey.getNode().toString())) {
                        o = ((String) temp[idxN]).split("=")[1].split(",");
                        Color useThisColor = LegendUtil.getRGB(Integer.valueOf(o[0].trim()));
                        myPlot.setNodeFillColor(myKey, useThisColor);
                    }
                }
            }
        }
    }

    /**
     * Reads legend item names from bundle file
     * @return String[] legend item names
     */
    private String[] readLegendItemsFromProperties() {
        Object[] temp = getLegendInfoFromProperties(bundlePath);
        if (temp == null) return new String[0];
        String[] tempStr = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            String myLine = (String) temp[i];
            if (myLine.contains(":") & !myLine.contains("*")) {
                String secondPart = ((String) temp[i]).split(":")[1].trim();
                tempStr[i] = secondPart.split("=")[0].trim();
                if (debug) System.out.println("readLegendItemsFromProperties: local line and queryName matched");
            } else {
                tempStr[i] = ((String) temp[i]).split("=")[0].trim();
                String firstPart = tempStr[i];
                if (firstPart.startsWith("*:")) {
                    tempStr[i] = firstPart.replace("*:", "");
                }
            }
        }
        return tempStr;
    }

    /**
     * Reads query names from bundle file
     * @return String[] query names
     */
    private String[] readQueryInfoFromProperties() {
        Object[] temp = getLegendInfoFromProperties(bundlePath);
        if (temp == null) return new String[0];
        String[] queryStr = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            String myLine = (String) temp[i];
            if (myLine.contains(":") & !myLine.contains("*")) {
                String queryNameInFile = ((String) temp[i]).split(":")[0].trim();
                queryStr[i] = queryNameInFile;
            } else {
                String firstPart = ((String) temp[i]).split("=")[0].trim();
                if (firstPart.startsWith("*:")) {
                    queryStr[i] = "*";
                } else {
                    queryStr[i] = "*";
                }
            }
        }
        return queryStr;
    }

    /**
     * Reads legend info lines from bundle file
     * @param path Path to bundle file
     * @return Object[] legend info lines
     */
    private Object[] getLegendInfoFromProperties(String path) {
        LineNumberReader lineReader = null;
        Object[] temp = new Object[0];
        if (path == null || path.trim().isEmpty()) {
            return temp;
        }
        try {
            DataInputStream dis = FileUtil.initInFile(path);
            lineReader = new LineNumberReader(new InputStreamReader(dis));
            temp = lineReader.lines().toArray();
            lineReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not read legend info (FNF): " + e.toString());
        } catch (IOException e) {
            System.out.println("Could not read legend info (IO): " + e.toString());
        }
        return temp;
    }

    /**
     * Creates summary panel (currently empty)
     * @return JComponent summary panel
     */
    protected JComponent createSummary() {
        summaryPanel = new JPanel();
        summaryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.X_AXIS));
        summaryPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        summaryPanel.add(Box.createHorizontalGlue());
        summaryPanel.add(Box.createHorizontalGlue());
        return summaryPanel;
    }

    /**
     * Handles dropdown changes to redraw chart
     */
    public class UpdateSankeyOrBarChart extends JPanel implements ActionListener {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (replaceWithBarChart) {
                RedrawBarChart();
            } else {
                RedrawSankeyPlot();
            }
        }
    }

    /**
     * Redraws Sankey plot when selection changes
     */
    public void RedrawSankeyPlot() {
        frame.remove(sankeyPanel);
        frame.getContentPane().add(createSankeyPlot(), BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Redraws bar chart when selection changes
     */
    public void RedrawBarChart() {
        frame.remove(barChartPanel);
        frame.getContentPane().add(createStackedBarPlot(), BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Creates flow dataset for Sankey plot from table
     * @param jtable Table
     * @param scenarioStr Scenario
     * @param regionStr Region
     * @param yearStr Year
     * @return DefaultFlowDataset
     */
    private DefaultFlowDataset createFlowDatasetFromTable(JTable jtable, String scenarioStr, String regionStr, String yearStr) {
        int regionIdx = FilteredTable.getColumnByName(jtable, "region");
        int scenarioIdx = FilteredTable.getColumnByName(jtable, "scenario");
        ArrayList<String> yearList = FilteredTable.getYearListFromTableData(jtable);
        if (yearList.isEmpty() || regionIdx < 0 || scenarioIdx < 0 || yearStr == null) {
            return new DefaultFlowDataset();
        }
        int firstYearIdx = FilteredTable.getColumnByName(jtable, yearList.get(0));
        int yearIdx = FilteredTable.getColumnByName(jtable, yearStr);
        if (firstYearIdx < 0 || yearIdx < 0) {
            return new DefaultFlowDataset();
        }
        DefaultFlowDataset dataset = new DefaultFlowDataset();
        int curStage = 0;
        for (int row = 0; row < jtable.getRowCount(); row++) {
            curStage = 0;
            boolean scenario2Keep = String.valueOf(jtable.getValueAt(row, scenarioIdx)).equals(scenarioStr);
            boolean region2Keep = String.valueOf(jtable.getValueAt(row, regionIdx)).equals(regionStr);
            if (scenario2Keep && region2Keep) {
                String flowValue = String.valueOf(jtable.getValueAt(row, yearIdx));
                double flowRate;
                try {
                    flowRate = Double.parseDouble(flowValue);
                } catch (NumberFormatException e) {
                    continue;
                }
                for (int j = firstYearIdx - 1; j > regionIdx + 1; j--) {
                    String fromSource = String.valueOf(jtable.getValueAt(row, j));
                    String toDes = String.valueOf(jtable.getValueAt(row, j - 1));
                    if (flowRate != 0) {
                        dataset.setFlow(curStage, fromSource, toDes, flowRate);
                    }
                    curStage = curStage + 1;
                }
            }
        }
        return dataset;
    }

    /**
     * Creates category dataset for bar chart from table
     * @param jtable Table
     * @param scenarioStr Scenario
     * @param regionStr Region
     * @param yearStr Year
     * @return DefaultCategoryDataset
     */
    private DefaultCategoryDataset createCategoryDatasetFromTable(JTable jtable, String scenarioStr, String regionStr, String yearStr) {
        int yearIdx = FilteredTable.getColumnByName(jtable, yearStr);
        int scenarioIdx = FilteredTable.getColumnByName(jtable, "scenario");
        int regionIdx = FilteredTable.getColumnByName(jtable, "region");
        if (yearIdx < 0 || scenarioIdx < 0 || regionIdx < 0 || regionIdx + 1 >= jtable.getColumnCount()) {
            return new DefaultCategoryDataset();
        }
        String colName = jtable.getColumnName(regionIdx + 1);
        DefaultCategoryDataset myDataset = new DefaultCategoryDataset();
        for (int row = 0; row < jtable.getRowCount(); row++) {
            boolean scenario2Keep = String.valueOf(jtable.getValueAt(row, scenarioIdx)).equals(scenarioStr);
            boolean region2Keep = String.valueOf(jtable.getValueAt(row, regionIdx)).equals(regionStr);
            if (scenario2Keep && region2Keep) {
                String myStr = String.valueOf(jtable.getValueAt(row, regionIdx + 1));
                try {
                    double myNum = Double.parseDouble(String.valueOf(jtable.getValueAt(row, yearIdx)));
                    myDataset.addValue(myNum, myStr, colName);
                } catch (NumberFormatException e) {
                    if (debug) System.out.println("Skipping non-numeric bar value: " + jtable.getValueAt(row, yearIdx));
                }
            }
        }
        return myDataset;
    }

    // ComponentListener methods (not used)
    @Override
    public void componentResized(ComponentEvent e) {}
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void componentHidden(ComponentEvent e) {}

    private Font resolveUiFont(String uiKey, int style, int sizeOffset) {
        Font baseFont = javax.swing.UIManager.getFont(uiKey);
        if (baseFont == null) {
            baseFont = getFont();
        }
        if (baseFont == null) {
            baseFont = new Font(Font.DIALOG, style, InterfaceMain.getConfiguredFontSize());
        }
        int size = Math.max(8, InterfaceMain.getConfiguredFontSize() + sizeOffset);
        return baseFont.deriveFont(style, (float) size);
    }
}