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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;

import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.InterfaceMain;
import chart.Chart;
import chart.ChartMarker;
import chart.ChartUtil;
import chartOptions.ChartOptionsUtil;
import chartOptions.ExportExcel;
import chartOptions.ModifyLegend;
import chartOptions.SelectDecimalFormat;

/**
 * AChartDisplay handles displaying a chart and its options in a dialog window.
 * Supports showing/hiding legend, table, and chart options for a given chart or chart array.
 * <p>
 * Author: TWU
 * Date: 1/2/2016
 */
public class AChartDisplay {

	/** Array of charts to display (from IconMouseListener) */
	private Chart[] charts;
	/** Index of the chart to display (from IconMouseListener) */
	private int id;
	/** The chart to display (from IconMouseListener) */
	private Chart chart;
	/** The current chart being displayed */
	private JFreeChart curchart;
	/** Main split pane for chart and data */
	private JSplitPane sp;
	/** Main panel for chart display */
	private JPanel jp;
	/** Dialog window for chart display */
	private JDialog dialog;
	/** Scroll pane for chart panel */
	private JScrollPane chartPaneScroll = null;
	/** Default dialog size X */
	private int smallSizeX = 540;
	/** Default dialog size Y */
	private int smallSizeY = 480;
	/** Button to show/hide table */
	JButton jb_table = new JButton("Show Table");
	/** Button to show/hide legend */
	JButton jb_legend = new JButton("Show Legend");
	/** Chart options dialog */
	ChartOptions myOpts = null;
	/** Flag for table visibility */
	private boolean tableShowing = false;
	/** Flag for legend visibility */
	private boolean legendShowing = false;

	/**
	 * Constructor for displaying a chart from an array of charts.
	 * @param charts Array of Chart objects
	 * @param id Index of chart to display
	 */
	public AChartDisplay(Chart[] charts, final int id) {
		super();
		this.charts = charts;
		this.id = id;
		this.chart = charts[id];
		init();
	}

	/**
	 * Constructor for displaying a single chart.
	 * @param chart Chart object to display
	 */
	public AChartDisplay(Chart chart) {
		super();
		this.charts = new Chart[1];
		charts[0] = chart;
		this.chart = chart;
		this.id = 0;
		init();
	}

	/**
	 * Initializes the chart display dialog and sets up chart panel and options.
	 */
	private void init() {
		curchart = null;
		if (chart == null) {
			return;
		}
		JFreeChart jf = chart.getChart();
		if (jf != null) {
			ChartUtil.applyGraphicsDefaults(jf);
			for (int j = 0; j < jf.getSubtitleCount(); j++) {
				jf.getSubtitle(j).setVisible(true);
			}
			if (jf.getTitle() != null)
				jf.getTitle().setVisible(true); // Ensure title is visible
			dialog = CreateComponent.crtJDialog(chart.getGraphName());
			dialog.setSize(new Dimension(smallSizeX, smallSizeY));
			setJSplitPane(setChartPane(jf), null);
			this.legendShowing = false;
			if (jf.getLegend() != null) {
				this.legendShowing = jf.getLegend().visible;
			}
			jb_legend.setText(legendShowing ? "Hide Legend" : "Show Legend");
			smallSizeX = dialog.getWidth();
			smallSizeY = dialog.getHeight();
			dialog.setLocation(InterfaceMain.getInstance().getFrame().getLocation());
			dialog.setVisible(true);
			DbViewer.openWindows.add(dialog);
		}
	}

	/**
	 * Sets up the split pane for chart and data panels in the dialog.
	 * @param chartPane Chart panel scroll pane
	 * @param dataPane Data panel scroll pane
	 */
	private void setJSplitPane(JScrollPane chartPane, JScrollPane dataPane) {
		sp = null;
		if (dataPane != null) {
			sp = new JSplitPane();
			sp.setOrientation(JSplitPane.VERTICAL_SPLIT);
			sp.setTopComponent(chartPane);
			sp.setBottomComponent(dataPane);
			sp.setDividerLocation(0.9);
			sp.setDividerSize(5);
			dialog.setContentPane(sp);
		} else {
			dialog.setContentPane(chartPane);
		}
		dialog.pack();
		// dialog.setVisible(true); // YD moved to init()
	}

	/**
	 * Creates the chart panel with options and returns as a scroll pane.
	 * @param jfreechart Chart to display
	 * @return JScrollPane containing chart panel
	 */
	private JScrollPane setChartPane(JFreeChart jfreechart) {
		if (chartPaneScroll == null) {
			ChartPanel chartPanel = new ChartPanel(jfreechart);
			JPanel chartPane = new JPanel(new BorderLayout());
			chartPane.add(chartPanel, BorderLayout.CENTER);
			chartPane.add(chartOption(), BorderLayout.SOUTH);
			chartPane.setMinimumSize(new Dimension(640, 360));
			chartPane.updateUI();
			chartPaneScroll = new JScrollPane(chartPane);
		}
		return chartPaneScroll;
	}

	/**
	 * Creates the data panel for the chart and returns as a scroll pane.
	 * @param jfreechart Chart to display
	 * @param unitLookup Units lookup map
	 * @return JScrollPane containing data panel
	 */
	private JScrollPane setDataPane(JFreeChart jfreechart, HashMap<String, String> unitLookup) {
		DataPanel dataPane = null;
		try {
			if (jfreechart.getPlot().getPlotType().contains("Category")) {
				if (jfreechart.getCategoryPlot().getDataset() instanceof DefaultBoxAndWhiskerCategoryDataset) {
					dataPane = new BoxAndWhiskerDataPane(jfreechart);
				} else {
					// Always use the units-aware constructor when we have a Chart instance,
					// regardless of whether this display was opened from a thumbnail array.
					// The prior logic only passed unitLookup when charts != null, which caused
					// the units column to be blank in the expanded view in some cases.
					if (unitLookup != null) {
						dataPane = new CategoryDatasetDataPane(charts, id, unitLookup);
					} else {
						dataPane = new CategoryDatasetDataPane(jfreechart);
					}
				}
			} else if (jfreechart.getPlot().getPlotType().contains("XY")) {
				if (charts == null)
					dataPane = new XYDatasetDataPane(jfreechart);
				else
					dataPane = new XYDatasetDataPane(charts, id);
			}
		} catch (CloneNotSupportedException e1) {
			// ignore
		}
		return new JScrollPane(dataPane);
	}

	/**
	 * Creates the chart options box with buttons for chart/table/legend options.
	 * @return Box containing chart option buttons
	 */
	private Box chartOption() {
		JButton jb = new JButton("Chart Options");
		jb.setName("ChartOptions");
		java.awt.event.MouseListener ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (myOpts != null) {
					myOpts.dispose();
					myOpts = null;
				}
				myOpts = new ChartOptions(sp, e.getXOnScreen(), e.getYOnScreen());
			}
		};
		jb.addMouseListener(ml);
		Box box = Box.createHorizontalBox();
		box.add(jb);
		jb.setName("ChartOptions");
		// Table show/hide button
		java.awt.event.MouseListener mlTable = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (!tableShowing) {
					jb_table.setText("Hide Table");
					setJSplitPane(setChartPane(chart.getChart()), setDataPane(chart.getChart(), chart.getUnitsLookup()));
				} else {
					jb_table.setText("Show Table");
					setJSplitPane(setChartPane(chart.getChart()), null);
				}
				tableShowing = !tableShowing;
			}
		};
		jb_table.addMouseListener(mlTable);
		box.add(jb_table);
		// Legend show/hide button
		java.awt.event.MouseListener mlLegend = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JFreeChart jf = chart.getChart();
				if (!legendShowing) {
					jf.addLegend(chart.myLegend);
					jb_legend.setText("Hide Legend");
				} else {
					jf.removeLegend();
					jb_legend.setText("Show Legend");
				}
				dialog.repaint();
				legendShowing = !legendShowing;
			}
		};
		jb_legend.addMouseListener(mlLegend);
		box.add(jb_legend);
		return box;
	}

	private class ChartOptions extends JDialog implements ActionListener, ItemListener {

		private static final long serialVersionUID = 1L;
		private final String options[] = { "Original Chart Type",
				// Chart is modified in thumb nail life cycle
				"Modify Legend", "Make a Marker", "Add/Remove Annotation",
				// Chart is modified in single chart life cycle
				"Show Legend", "Show As 3D",
				// Chart is for representation only
				"Show As PieChart", // "Show As HistogramChart",
				"Select a Decimal Format", "Export Data to Excel" };
		// "Generate Report" };
		private Box box;
		// Chart data manipulation
		private DataPanel datapane;
		private JFreeChart jfreechart;

		public ChartOptions(JSplitPane sp, int x, int y) {
			super((Frame) null, false);
			new ModifyLegend(charts, id);
		}

		private void createButtonItem(int start, int end) {
			box.add(Box.createVerticalStrut(5));
			for (int i = start; i < end; i++) {
				JButton rbMenuItem = new JButton(options[i]);
				rbMenuItem.setPreferredSize(new Dimension(120, 20));
				rbMenuItem.setActionCommand(String.valueOf(i));
				rbMenuItem.addActionListener(this);
				box.add(rbMenuItem);
				box.add(Box.createVerticalStrut(5));
			}
		}

		private void createCheckBoxItem() {
			for (int i = 4; i < 6; i++) {
				JCheckBox cbMenuItem = new JCheckBox(options[i]);
				cbMenuItem.setActionCommand(String.valueOf(i));
				if (i == 4) {
					cbMenuItem.setSelected(jfreechart.getLegend().visible);
				} else if (i == 5) {
					// No 3D for pie and histogram chart
					if (!(jfreechart.getPlot() instanceof PiePlot) && !(jfreechart.getPlot() instanceof XYPlot
							&& jfreechart.getXYPlot().getDataset() instanceof IntervalXYDataset))
						cbMenuItem.setSelected(ChartOptionsUtil.is3DChart(jfreechart));
				}
				cbMenuItem.addItemListener(this);
				box.add(cbMenuItem);
			}
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof JButton)
				processRadioButtonMenuItem(e.getActionCommand());
			dispose();
		}

		private void processRadioButtonMenuItem(String action) {
			try {
				switch (Integer.valueOf(action.trim()).intValue()) {
				case 0:
					curchart = null;
					refreshChart(chart.getChart());//jfreechart);
					break;
				case 1: // Modify Legend
					if (curchart != null && curchart.getPlot() instanceof PiePlot
							|| (jfreechart.getPlot().getPlotType().contains("XY")
									&& jfreechart.getXYPlot().getDataset() instanceof IntervalXYDataset)) {
						JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Not support for this Chart", "Information",
								JOptionPane.INFORMATION_MESSAGE);
						return;
					} else
						new ModifyLegend(charts, id);
					break;
				case 2: // Modify Marker; curchart only use for chart type is a pie chart
					if (curchart != null && curchart.getPlot() instanceof PiePlot
							|| (jfreechart.getPlot().getPlotType().contains("XY")
									&& jfreechart.getXYPlot().getDataset() instanceof IntervalXYDataset)) {
						JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Not support for this Chart", "Information",
								JOptionPane.INFORMATION_MESSAGE);
						return;
					} else {
						ChartMarker cm = new ChartMarker(chart, dialog);
						jfreechart = cm.getJfchart();
					}
					break;
				case 3: // Modify Annotation
					if (curchart != null && curchart.getPlot() instanceof PiePlot
							|| (jfreechart.getPlot().getPlotType().contains("XY")
									&& jfreechart.getXYPlot().getDataset() instanceof IntervalXYDataset)) {
						JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Not support for this Chart", "Information",
								JOptionPane.INFORMATION_MESSAGE);
						return;
					} else
						jfreechart = new AnnotationChartPane(chart).getJfchart();
					break;
				case 6: // pie chart
					curchart = ChartOptionsUtil.showPieChart(chart.getPath(), chart.getGraphName(),
							chart.getMeta() + "|" + chart.getMetaCol(), chart.getAxis_name_unit(), chart.getChart());
					refreshChart(curchart);
					break;
				case 7: // Show different decimal point
					new SelectDecimalFormat(datapane);
					break;
				case 8: // Export Data
					if (JOptionPane.showConfirmDialog(InterfaceMain.getInstance().getFrame(), "Meta Data Also?", "choose one",
							JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION)
						new ExportExcel("", datapane.getTableCol(), datapane.getDataValue(), chart.getTitles()[1],
							chart.getMetaCol(), chart.getMeta(), chart.getAxis_name_unit()[1]);
					else
						new ExportExcel("", datapane.getTableCol(), datapane.getDataValue());
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				this.dispose();
				return;
			}
		}

		private void refreshChart(JFreeChart jf) {
			ChartUtils.applyCurrentTheme(jf);
			ChartUtil.applyGraphicsDefaults(jf);
			ThumbnailUtilNew.validateChartPane(jp);
			jp.add(new ChartPanel(jf), BorderLayout.CENTER);
			jp.updateUI();
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			JCheckBox cbMenuItem = (JCheckBox) e.getSource();
			String action = cbMenuItem.getActionCommand();
			try {
				switch (Integer.valueOf(action.trim()).intValue()) {
				case 4:
					if (jfreechart.getPlot().getPlotType().contains("Pie"))
						JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "Show/Hide Legend Not Apply for Pie Chart", "Information",
								JOptionPane.INFORMATION_MESSAGE);
					else {
						jfreechart.getLegend().visible = !jfreechart.getLegend().isVisible();
						cbMenuItem.setSelected(jfreechart.getLegend().visible);
						cbMenuItem.revalidate();
					}
					break;
				case 5:
					ChartOptionsUtil.changeChartType(chart.getPaint(), jfreechart, e.getStateChange());
					break;
				}
			} catch (Exception e1) {
				e1.printStackTrace();
				this.dispose();
			}
			ChartUtils.applyCurrentTheme(jfreechart);
			ChartUtil.applyGraphicsDefaults(jfreechart);
			ThumbnailUtilNew.validateChartPane(jp);
			jp.add(new ChartPanel(jfreechart), BorderLayout.CENTER);
			jp.updateUI();
			this.dispose();
		}
	}
}