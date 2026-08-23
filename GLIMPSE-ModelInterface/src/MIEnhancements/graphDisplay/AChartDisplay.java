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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
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
	/** Active chart panel instance so preferred chart size can drive dialog sizing. */
	private ChartPanel chartPanel = null;
	/** Container that keeps the chart buttons visible below the chart area. */
	private JPanel chartPaneContainer = null;
	/** Default dialog size X */
	private int smallSizeX = 270;
	/** Default dialog size Y */
	private int smallSizeY = 240;
	/** Small extra height for split-pane/dialog chrome around the table area. */
	private static final int TABLE_CHROME_ALLOWANCE = 12;
	/** Preserved chart viewport size used to keep chart geometry stable across toggles. */
	private Dimension preservedChartViewportSize = null;
	/** Stable chart-options strip height captured once to avoid layout-dependent drift. */
	private int chartOptionsHeight = 36;
	/** True while this class is mutating dialog layout (not a user resize). */
	private boolean updatingLayout = false;
	/** Emits chart/table layout sizing diagnostics for toggle troubleshooting. */
	private static final boolean DEBUG_LAYOUT = false;
	/** Button to show/hide table */
	JButton jb_table = new JButton("Show Table");
	/** Button to show/hide legend */
	JButton jb_legend = new JButton("Show Legend");
	/** Flag for table visibility */
	private boolean tableShowing = false;
	/** Flag for legend visibility */
	private boolean legendShowing = false;

	/**
	 * ChartPanel override that injects the Series (legend) tab in the
	 * "Customize" dialog and renames the right-click "Properties" item.
	 */
	private class SeriesChartPanel extends ChartPanel {
		private static final long serialVersionUID = 1L;

		SeriesChartPanel(JFreeChart chartToDisplay) {
			super(chartToDisplay);
		}

		/** Opens the Customize dialog (also called by the toolbar button). */
		@Override
		public void doEditChartProperties() {
			JFreeChart editableChart = getChart();
			if (editableChart == null) {
				return;
			}
			final ChartUtil.GraphicsPreferences originalGraphicsPreferences = ChartUtil.captureGraphicsPreferences(editableChart);
			ChartUtil.applyGraphicsDefaults(editableChart);
			ChartEditor editor = ChartEditorManager.getChartEditor(editableChart);
			JTabbedPane editorTabs = null;
			if (editor instanceof Component) {
				insertSeriesTab((Component) editor, editableChart);
				applyFriendlyAxisTerminology((Component) editor);
				editorTabs = findTabbedPane((Container) editor);
			}

			// Build a custom dialog so we can control its size (50% taller than default).
			JDialog customizeDialog = new JDialog((Frame) null, "Customize Chart", true);
			customizeDialog.setLayout(new BorderLayout());
			customizeDialog.add((Component) editor, BorderLayout.CENTER);
			final AtomicBoolean chartChangesCommitted = new AtomicBoolean(false);
			final Runnable restoreOriginalChart = new Runnable() {
				@Override
				public void run() {
					if (chartChangesCommitted.get()) {
						return;
					}
					ChartUtil.applyGraphicsPreferences(editableChart, originalGraphicsPreferences);
					repaintDisplayedChart();
				}
			};
			customizeDialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					restoreOriginalChart.run();
				}
			});

			JPanel buttonPanel = new JPanel();
			JButton okBtn = new JButton("OK");
			JButton cancelBtn = new JButton("Cancel");
			okBtn.addActionListener(ae -> {
				editor.updateChart(editableChart);
				ChartUtil.persistGraphicsPreferences(editableChart);
				chartChangesCommitted.set(true);
				refreshDisplayedChart(editableChart);
				customizeDialog.dispose();
			});
			cancelBtn.addActionListener(ae -> {
				restoreOriginalChart.run();
				customizeDialog.dispose();
			});
			buttonPanel.add(okBtn);
			buttonPanel.add(cancelBtn);
			customizeDialog.add(buttonPanel, BorderLayout.SOUTH);

			// Select the Series tab (inserted at index 0) by default.
			if (editorTabs != null) {
				for (int i = 0; i < editorTabs.getTabCount(); i++) {
					if ("Series".equalsIgnoreCase(editorTabs.getTitleAt(i))) {
						editorTabs.setSelectedIndex(i);
						break;
					}
				}
			}

			// Size dialog: width +30%, height -20% relative to previous sizing.
			Dimension editorPref = ((Component) editor).getPreferredSize();
			int dlgW = (int) (Math.max(300, (editorPref.width + 30) / 2) * 1.69);
			int dlgH = (int) ((Math.max(350, editorPref.height) + buttonPanel.getPreferredSize().height + 30) * 1.5 * 0.80);
			customizeDialog.setSize(dlgW, dlgH);
			customizeDialog.setLocationRelativeTo(this);
			customizeDialog.setVisible(true);
		}

		/**
		 * Override the right-click popup to rename "Properties..." → "Customize...".
		 */
		@Override
		public javax.swing.JPopupMenu createPopupMenu(boolean properties, boolean copy,
				boolean save, boolean print, boolean zoom) {
			javax.swing.JPopupMenu menu = super.createPopupMenu(properties, copy, save, print, zoom);
			if (menu != null) {
				for (int i = 0; i < menu.getComponentCount(); i++) {
					Component item = menu.getComponent(i);
					if (item instanceof javax.swing.JMenuItem) {
						javax.swing.JMenuItem mi = (javax.swing.JMenuItem) item;
						if ("Properties...".equalsIgnoreCase(mi.getText())
								|| "Properties".equalsIgnoreCase(mi.getText())) {
							mi.setText("Customize...");
						}
					}
				}
			}
			return menu;
		}
	}

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
			dialog.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					if (updatingLayout) {
						return;
					}
					// User-driven window resizing should be allowed to resize the chart.
					clearPreservedChartViewportSize();
					updateChartScrollBarPolicies();
					// Recalculate and apply chart scaling for new viewport size
					SwingUtilities.invokeLater(() -> fitChartViewToViewport());
				}
			});
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
			// Capture a stable post-layout baseline once so table toggles do not ratchet down
			// by repeatedly preserving a slightly smaller live viewport.
			SwingUtilities.invokeLater(() -> preserveCurrentChartViewportSize());
			DbViewer.openWindows.add(dialog);
		}
	}

	/**
	 * Sets up the split pane for chart and data panels in the dialog.
	 * @param chartPane Chart panel scroll pane
	 * @param dataPane Data panel
	 */
	private void setJSplitPane(JPanel chartPane, DataPanel dataPane) {
		updatingLayout = true;
		try {
			ensurePreservedChartViewportSize();
			applyPreservedChartViewportSize();
			if (dataPane != null) {
				dataPane.applyTableViewportSizing();
			}
			sp = null;
			if (dataPane != null) {
				sp = new JSplitPane();
				sp.setOrientation(JSplitPane.VERTICAL_SPLIT);
				sp.setTopComponent(chartPane);
				sp.setBottomComponent(dataPane);
				sp.setResizeWeight(0.0);
				sp.setDividerSize(5);
				dialog.setContentPane(sp);
			} else {
				dialog.setContentPane(chartPane);
			}
			sizeDialogToFitLayout(dataPane);
			dialog.getContentPane().revalidate();
			dialog.getContentPane().repaint();
			if (sp != null && dataPane != null) {
				setSplitDividerForTable(dataPane);
			}
			logLayoutState("setJSplitPane-afterLayout", dataPane != null);
			updateChartScrollBarPolicies();
			SwingUtilities.invokeLater(() -> {
				if (sp != null && dataPane != null) {
					setSplitDividerForTable(dataPane);
				}
				fitChartViewToViewport();
				logLayoutState("setJSplitPane-postEDT", dataPane != null);
			});
		} finally {
			updatingLayout = false;
		}
		// dialog.setVisible(true); // YD moved to init()
	}

	private void preserveCurrentChartViewportSize() {
		if (chartPaneScroll == null || jp == null || chartPaneContainer == null) {
			return;
		}
		Dimension viewportSize = chartPaneScroll.getViewport().getExtentSize();
		if (viewportSize == null || viewportSize.width <= 0 || viewportSize.height <= 0) {
			return;
		}
		if (preservedChartViewportSize == null) {
			preservedChartViewportSize = new Dimension(viewportSize);
		} else {
			// Keep the largest observed viewport so tiny layout fluctuations do not shrink
			// the preserved size over repeated show/hide toggles.
			preservedChartViewportSize.width = Math.max(preservedChartViewportSize.width, viewportSize.width);
			preservedChartViewportSize.height = Math.max(preservedChartViewportSize.height, viewportSize.height);
		}
		applyPreservedChartViewportSize();
	}

	private void ensurePreservedChartViewportSize() {
		if (preservedChartViewportSize != null) {
			return;
		}
		Dimension viewportSize = null;
		if (chartPaneScroll != null) {
			viewportSize = chartPaneScroll.getViewport().getExtentSize();
		}
		if (viewportSize == null || viewportSize.width <= 0 || viewportSize.height <= 0) {
			if (chartPanel != null) {
				viewportSize = chartPanel.getPreferredSize();
			}
		}
		if (viewportSize == null || viewportSize.width <= 0 || viewportSize.height <= 0) {
			viewportSize = new Dimension(640, 360);
		}
		preservedChartViewportSize = new Dimension(viewportSize);
	}

	private void applyPreservedChartViewportSize() {
		if (preservedChartViewportSize == null) {
			return;
		}
		// Keep only the target size cache; the actual view size is synchronized to the
		// viewport in fitChartViewToViewport() to avoid scrollbars.
	}

	private void clearPreservedChartViewportSize() {
		preservedChartViewportSize = null;
		if (jp != null) {
			jp.setPreferredSize(null);
		}
		if (chartPaneContainer != null) {
			chartPaneContainer.setPreferredSize(null);
		}
		// Allow the chart to expand when user manually resizes the dialog
		if (chartPanel != null) {
			chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
			chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
		}
	}

	private void updateChartScrollBarPolicies() {
		if (chartPaneScroll == null) {
			return;
		}
		chartPaneScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		chartPaneScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		chartPaneScroll.revalidate();
		chartPaneScroll.repaint();
	}

	private void fitChartViewToViewport() {
		if (chartPaneScroll == null || jp == null) {
			return;
		}
		Dimension target;
		if (preservedChartViewportSize != null && sp == null) {
			// Table is hidden: use the stable preserved baseline so that layout-timing
			// jitter (the live viewport may not have updated yet after setSize/revalidate)
			// does not cause a 1-2 px ratchet shrink on each Hide Table toggle.
			target = new Dimension(preservedChartViewportSize);
		} else {
			// Table is showing (sp != null): the chart shares vertical space with the
			// table, so we must fit jp to the actual available top-panel viewport.
			target = chartPaneScroll.getViewport().getExtentSize();
			if (target == null || target.width <= 0 || target.height <= 0) {
				return;
			}
		}
		jp.setPreferredSize(target);
		// Scale the chart to fit the viewport to prevent scrollbars and ensure
		// the X axis and legend labels are fully visible
		if (chartPanel != null) {
			chartPanel.setMaximumDrawWidth(target.width);
			chartPanel.setMaximumDrawHeight(target.height);
		}
		jp.revalidate();
		if (chartPaneContainer != null) {
			chartPaneContainer.revalidate();
		}
	}

	private int getTargetSplitTopHeight() {
		int target = getChartViewportTargetSize().height + getChartOptionHeight();
		return Math.max(220, target);
	}

	private void setSplitDividerForTable(DataPanel dataPane) {
		if (sp == null || dataPane == null) {
			return;
		}
		int splitHeight = sp.getHeight();
		if (splitHeight <= 0) {
			return;
		}
		int minTop = 220;
		int desiredBottom = Math.max(120, getDataPanePreferredHeight(dataPane) + TABLE_CHROME_ALLOWANCE);
		int maxBottom = Math.max(120, splitHeight - sp.getDividerSize() - minTop);
		int bottom = Math.min(desiredBottom, maxBottom);
		int divider = splitHeight - sp.getDividerSize() - bottom;
		divider = Math.max(minTop, divider);
		sp.setDividerLocation(divider);
	}

	private void logLayoutState(String phase, boolean withTable) {
		if (!DEBUG_LAYOUT) {
			return;
		}
		Dimension dialogSize = dialog == null ? null : dialog.getSize();
		Dimension contentSize = dialog == null || dialog.getContentPane() == null ? null : dialog.getContentPane().getSize();
		Dimension viewportSize = chartPaneScroll == null ? null : chartPaneScroll.getViewport().getExtentSize();
		Dimension chartViewPreferred = jp == null ? null : jp.getPreferredSize();
		Dimension chartPanelPreferred = chartPanel == null ? null : chartPanel.getPreferredSize();
		int divider = sp == null ? -1 : sp.getDividerLocation();
		System.out.println("[AChartDisplay] " + phase + " withTable=" + withTable +
				" dialog=" + dialogSize + " content=" + contentSize +
				" viewport=" + viewportSize + " viewPreferred=" + chartViewPreferred +
				" chartPreferred=" + chartPanelPreferred + " divider=" + divider);
	}

	private int getChartPanePreferredHeight(Component chartPane) {
		int height = Math.max(240, chartPane.getPreferredSize().height);
		return height;
	}

	private Dimension getChartViewportTargetSize() {
		if (preservedChartViewportSize != null) {
			return new Dimension(preservedChartViewportSize);
		}
		if (chartPanel != null && chartPanel.getPreferredSize() != null) {
			Dimension preferred = chartPanel.getPreferredSize();
			if (preferred.width > 0 && preferred.height > 0) {
				return preferred;
			}
		}
		return new Dimension(640, 360);
	}

	private int getChartOptionHeight() {
		return Math.max(28, chartOptionsHeight);
	}

	private void sizeDialogToFitLayout(DataPanel dataPane) {
		if (dialog == null) {
			return;
		}
		Dimension chartSize = getChartViewportTargetSize();
		int contentWidth = chartSize.width;
		int contentHeight = chartSize.height + getChartOptionHeight();
		if (dataPane != null) {
			int dataHeight = Math.max(120, getDataPanePreferredHeight(dataPane) + TABLE_CHROME_ALLOWANCE);
			contentHeight += 5 + dataHeight;
			contentWidth = Math.max(contentWidth, dataPane.getPreferredSize().width);
		}
		Insets windowInsets = dialog.getInsets();
		Rectangle screenBounds = getAvailableScreenBounds();
		int desiredWidth = contentWidth + windowInsets.left + windowInsets.right;
		int desiredHeight = contentHeight + windowInsets.top + windowInsets.bottom;
		desiredWidth = Math.min(desiredWidth, Math.max(640, screenBounds.width - 40));
		desiredHeight = Math.min(desiredHeight, Math.max(360, screenBounds.height - 40));

		dialog.setSize(desiredWidth, desiredHeight);
		logLayoutState("sizeDialogToFitLayout", dataPane != null);
	}

	private int getDataPanePreferredHeight(Component dataPane) {
		int preferred = dataPane.getPreferredSize().height;
		if (dataPane instanceof DataPanel) {
			preferred = Math.max(preferred, ((DataPanel) dataPane).getPreferredDisplayHeight());
		}
		return preferred;
	}

	private Rectangle getAvailableScreenBounds() {
		GraphicsConfiguration gc = dialog == null ? null : dialog.getGraphicsConfiguration();
		if (gc == null) {
			gc = InterfaceMain.getInstance().getFrame().getGraphicsConfiguration();
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
	 * Creates the chart panel with options and returns as a container panel.
	 * @param jfreechart Chart to display
	 * @return JPanel containing chart and controls
	 */
	private JPanel setChartPane(JFreeChart jfreechart) {
		if (chartPaneContainer == null) {
			jp = new JPanel(new BorderLayout());
			jp.setMinimumSize(new Dimension(640, 360));
			chartPaneScroll = new JScrollPane(jp);
			chartPaneScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			chartPaneScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			chartPaneContainer = new JPanel(new BorderLayout());
			chartPaneContainer.add(chartPaneScroll, BorderLayout.CENTER);
			Box options = chartOption();
			chartOptionsHeight = Math.max(28, options.getPreferredSize().height);
			chartPaneContainer.add(options, BorderLayout.SOUTH);
			chartPaneContainer.setMinimumSize(new Dimension(640, 390));
		}
		ThumbnailUtilNew.validateChartPane(jp);
		chartPanel = new SeriesChartPanel(jfreechart);
		jp.add(chartPanel, BorderLayout.CENTER);
		jp.revalidate();
		jp.repaint();
		chartPaneContainer.revalidate();
		chartPaneContainer.repaint();
		return chartPaneContainer;
	}

	/**
	 * Creates the data panel for the chart and returns as a scroll pane.
	 * @param jfreechart Chart to display
	 * @param unitLookup Units lookup map
	 * @return DataPanel containing data table and controls
	 */
	private DataPanel setDataPane(JFreeChart jfreechart, HashMap<String, String> unitLookup) {
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
		return dataPane;
	}

	/**
	 * Creates the chart options box with buttons for chart/table/legend options.
	 * @return Box containing chart option buttons
	 */
	private Box chartOption() {
		JButton jbCustomize = new JButton("Customize");
		JButton jbCopyChart = new JButton("Copy Chart");
		JButton jbCopyData = new JButton("Copy Data");
		jbCustomize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (chartPanel != null) {
					chartPanel.doEditChartProperties();
				}
			}
		});
		jbCopyChart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (chartPanel != null) {
					chartPanel.doCopy();
				}
			}
		});
		jbCopyData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DataPanel dataPane = resolveDataPaneForCopy();
				if (dataPane == null || !dataPane.copyTableToClipboard()) {
					JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(),
							"No chart data is available to copy.",
							"Copy Data", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		Box box = Box.createHorizontalBox();
		box.add(jbCustomize);
		box.add(jbCopyChart);
		box.add(jbCopyData);
		// Table show/hide button
		java.awt.event.MouseListener mlTable = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (!tableShowing) {
					preserveCurrentChartViewportSize();
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

	private DataPanel resolveDataPaneForCopy() {
		if (tableShowing && sp != null && sp.getBottomComponent() instanceof DataPanel) {
			return (DataPanel) sp.getBottomComponent();
		}
		return setDataPane(chart.getChart(), chart.getUnitsLookup());
	}

	private void insertSeriesTab(Component editorComponent, JFreeChart editorChart) {
		if (!(editorComponent instanceof Container)) {
			return;
		}
		JTabbedPane tabs = findTabbedPane((Container) editorComponent);
		if (tabs == null) {
			return;
		}
		removeTab(tabs, "Other");
		if (hasSeriesTab(tabs)) {
			return;
		}
		int insertIndex = resolveSeriesInsertIndex(tabs);
		JScrollPane seriesPanel = ModifyLegend.buildEmbeddedPanel(charts, id);
		tabs.insertTab("Series", null, seriesPanel, null, insertIndex);
	}

	private JTabbedPane findTabbedPane(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTabbedPane) {
				return (JTabbedPane) child;
			}
			if (child instanceof Container) {
				JTabbedPane nested = findTabbedPane((Container) child);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private boolean hasSeriesTab(JTabbedPane tabs) {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if ("Series".equalsIgnoreCase(tabs.getTitleAt(i))) {
				return true;
			}
		}
		return false;
	}

	private void removeTab(JTabbedPane tabs, String tabTitle) {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (tabTitle.equalsIgnoreCase(tabs.getTitleAt(i))) {
				tabs.removeTabAt(i);
				return;
			}
		}
	}

	private int resolveSeriesInsertIndex(JTabbedPane tabs) {
		// Place Series first — it is the most commonly used tab.
		return 0;
	}

	private void applyFriendlyAxisTerminology(Component root) {
		if (root == null) {
			return;
		}
		if (root instanceof JTabbedPane) {
			JTabbedPane tabs = (JTabbedPane) root;
			for (int i = 0; i < tabs.getTabCount(); i++) {
				tabs.setTitleAt(i, toFriendlyAxisText(tabs.getTitleAt(i)));
			}
		}
		if (root instanceof JLabel) {
			JLabel label = (JLabel) root;
			label.setText(toFriendlyAxisText(label.getText()));
		}
		if (root instanceof JComponent) {
			JComponent component = (JComponent) root;
			Border border = component.getBorder();
			if (border instanceof TitledBorder) {
				TitledBorder titledBorder = (TitledBorder) border;
				titledBorder.setTitle(toFriendlyAxisText(titledBorder.getTitle()));
			}
		}
		if (root instanceof Container) {
			for (Component child : ((Container) root).getComponents()) {
				applyFriendlyAxisTerminology(child);
			}
		}
	}

	private String toFriendlyAxisText(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		return text
				.replace("Domain Axis", "X-axis")
				.replace("Domain axis", "X-axis")
				.replace("domain axis", "x-axis")
				.replace("Range Axis", "Y-axis")
				.replace("Range axis", "Y-axis")
				.replace("range axis", "y-axis");
	}

	private void refreshDisplayedChart(JFreeChart updatedChart) {
		if (updatedChart == null) {
			return;
		}
		ChartUtils.applyCurrentTheme(updatedChart);
		ChartUtil.applyGraphicsDefaults(updatedChart);
		repaintDisplayedChart(updatedChart);
		syncLegendButton(updatedChart);
	}

	private void repaintDisplayedChart() {
		repaintDisplayedChart(chartPanel == null ? null : chartPanel.getChart());
	}

	private void repaintDisplayedChart(JFreeChart updatedChart) {
		if (chartPanel != null) {
			chartPanel.setChart(updatedChart);
			chartPanel.repaint();
		}
		if (dialog != null) {
			dialog.repaint();
		}
	}

	private void syncLegendButton(JFreeChart activeChart) {
		if (activeChart.getLegend() != null) {
			legendShowing = activeChart.getLegend().isVisible();
		} else {
			legendShowing = false;
		}
		jb_legend.setText(legendShowing ? "Hide Legend" : "Show Legend");
	}

}
