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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ModelInterface.InterfaceMain;

import chart.Chart;
import chart.LegendUtil;
import listener.ThumbnailBoxPopup;

/**
 * The class to handle options functions in display a chart panel.
 * 
 * Author Action Date Flag
 * ======================================================================= TWU
 * created 1/2/2016
 */

public class OptionsArea {
	private JPanel jp;
	private JSplitPane sp;
	private Chart[] chart;
	private int w;
	private int gridWidth;
	private boolean sameScale;
	private boolean hideOptions;
	//private int typeLineChart = 2;
	//private int typeRelativeLineChart = 3;
	public static String LINE_CHART="Line Chart";
	public static String STACKED_BAR_CHART="Stacked Bar Chart";
	public static String STACKED_AREA_CHART="Stacked Area Chart";
	public static String REL_RATIO_LINE ="Relative Ratio (Line)";
	public static String REL_DIFF_LINE="Relative Diff (Line)";
	public static String REL_DIFF_BAR="Relative Diff (Bar)";

	public OptionsArea(JPanel jp, Chart[] chart, int gridWidth, boolean sameScale, JSplitPane sp) {
		this(jp, chart, gridWidth, sameScale, sp, false);
	}

	public OptionsArea(JPanel jp, Chart[] chart, int gridWidth, boolean sameScale, JSplitPane sp, boolean hideOptions) {
		this.jp = jp;
		this.jp.setBackground(Color.green);
		this.jp.setLayout(new BorderLayout());
		this.chart = chart;
		this.gridWidth = gridWidth;
		this.sameScale = sameScale;
		this.sp = sp;
		this.hideOptions = hideOptions;
		setOptionsArea();
	}

	public JPanel getPanel() {
		return jp;
	}

	protected void setOptionsArea() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(5));
		JButton jb = new JButton("Options");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		jp.add(box);

		jb.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Runnable refreshAction = new Runnable() {
					public void run() {
						setChartPane();
					}
				};
				// IMPORTANT: use the latest OptionsArea.this.chart instead of a stale captured reference
				ThumbnailBoxPopup popup = new ThumbnailBoxPopup(OptionsArea.this.chart, w, gridWidth, sameScale, sp,
						refreshAction, hideOptions, (Consumer<Boolean>) (newSameScale) -> {
							sameScale = newSameScale;
						});
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
		box.add(jb);
		box.add(Box.createHorizontalStrut(10));
		/* JLabel jl = new JLabel("Display", 2);
		box.add(jl);
		box.add(Box.createHorizontalStrut(10));
		JScrollPane dspCol = displayCol();
		dspCol.setMaximumSize(new Dimension(90, 30));
		dspCol.setMinimumSize(new Dimension(30, 30));
		box.add(dspCol); */

		GraphOptionPane gPane = new GraphOptionPane();
		gPane.setMaximumSize(new Dimension(150, 30));
		gPane.setMinimumSize(new Dimension(90, 30));
		gPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		gPane.setBackground(Color.green);
		box.add(gPane);

		box.add(Box.createHorizontalStrut(10));

		/*jb = new JButton("Refresh");
		jb.setBackground(LegendUtil.getRGB(-8205574));
		java.awt.event.MouseListener ml = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				setChartPane();
			}
		};
		jb.addMouseListener(ml);
		box.add(jb);*/
		box.add(Box.createHorizontalStrut(10));

		jp.add(box, BorderLayout.NORTH);
	}

	private JScrollPane displayCol() {
		final String c[] = { "1", "2", "3", "4", "5", "6" };
		JList<String> list = new JList<String>(c);
		list.setName("dispCol");
		Font listFont = UIManager.getFont("List.font");
		if (listFont == null) {
			listFont = list.getFont();
		}
		if (listFont != null) {
			list.setFont(listFont.deriveFont((float) InterfaceMain.getConfiguredFontSize()));
		}
		list.setVisibleRowCount(3);
		list.setSelectionMode(0);
		ListSelectionListener lsl = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				JList<?> list = (JList<?>) e.getSource();
				boolean adjust = e.getValueIsAdjusting();
				if (!adjust && !list.isSelectionEmpty()) {
					gridWidth = Integer.valueOf(c[list.getSelectedIndex()]).intValue();
					setChartPane();
					jp.updateUI();
				}
			}
		};
		list.addListSelectionListener(lsl);
		return new JScrollPane(list);
	}

	private void setChartPane() {
		// Remove existing center component and replace it
		ThumbnailUtilNew.validateChartPane(jp);

		JPanel chartPane;
		if (hideOptions) {
			chartPane = ThumbnailUtilNew.createFlowChartPane(chart, sameScale);
		} else {
			w = ThumbnailUtilNew.computeFixGridLayoutViewSize(sp.getSize().width, gridWidth);
			chartPane = ThumbnailUtilNew.setChartPane(chart, w, gridWidth, sameScale, false);
		}

		JScrollPane scrollPane = new JScrollPane(chartPane);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		jp.add(scrollPane, BorderLayout.CENTER);
		jp.revalidate();
		jp.repaint();
	}

	/**
	 * The class to handle JFreeChart type conversion and relative value among
	 * columns of Thumbnail charts
	 *
	 */

	private class GraphOptionPane extends JComboBox<String> implements ActionListener {

		private static final long serialVersionUID = 1L;

		private String graphType[] = {OptionsArea.LINE_CHART, OptionsArea.STACKED_BAR_CHART, OptionsArea.STACKED_AREA_CHART,OptionsArea.REL_RATIO_LINE,OptionsArea.REL_DIFF_LINE,OptionsArea.REL_DIFF_BAR };
		private String graphClassName[] = {"chart.CategoryLineChart","chart.CategoryStackedBarChart","chart.CategoryStackedAreaChart","chart.CategoryLineChart","chart.CategoryLineChart","chart.CategoryStackedBarChart" };

		
		private int idx;
		private int relativeIndex;
		private String cn;
		private Chart[] oChart = null;

		public GraphOptionPane() {
			// Dan: Using modified version (2)
			idx = ThumbnailUtilNew.getFirstNonNullChart(chart);
			if (idx != -1) {
				relativeIndex = chart[idx].getRelativeColIndex();
				cn = chart[idx].getChartClassName();
				oChart = chart.clone();
				setPane();
			}
		}

		private void setPane() {
			int listC = graphType.length;
			for (int i = 0; i < listC; i++)
				addItem(graphType[i]);

			setName("GraphOptionPane");
			// Dan: Using modified version (2)
			setSelectedIndex(getIndex(chart[ThumbnailUtilNew.getFirstNonNullChart(chart)]));
			Font comboFont = resolveComboFont();
			setFont(comboFont);
			setPrototypeDisplayValue(OptionsArea.STACKED_AREA_CHART);
			setMaximumRowCount(graphType.length);
			// Keep popup list items in sync with combo font and apply clipping with tooltip fallback.
			setRenderer(new DefaultListCellRenderer() {
				private static final long serialVersionUID = 1L;
				@Override
				public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					String fullText = value == null ? "" : value.toString();
					label.setFont(GraphOptionPane.this.getFont());
					int availableWidth = index < 0
							? Math.max(80, GraphOptionPane.this.getWidth() - 30)
							: Math.max(80, list.getWidth() - 16);
					String displayText = ellipsizeToWidth(fullText, label.getFontMetrics(label.getFont()), availableWidth);
					label.setText(displayText);
					label.setToolTipText(displayText.equals(fullText) ? null : fullText);
					if (index == getFirstRelativeGraphIndex()) {
						label.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
					} else {
						label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
					}
					return label;
				}
			});
			setPreferredSize(new Dimension(computePreferredComboWidth(comboFont), 30));
			addActionListener(this);
			setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
		}

		private Font resolveComboFont() {
			Font uiComboFont = UIManager.getFont("ComboBox.font");
			if (uiComboFont == null) {
				uiComboFont = getFont();
			}
			if (uiComboFont == null) {
				uiComboFont = new Font(Font.DIALOG, Font.PLAIN, InterfaceMain.getConfiguredFontSize());
			}
			return uiComboFont.deriveFont((float) InterfaceMain.getConfiguredFontSize());
		}

		private int computePreferredComboWidth(Font comboFont) {
			java.awt.FontMetrics metrics = getFontMetrics(comboFont);
			int maxTextWidth = 0;
			for (String option : graphType) {
				maxTextWidth = Math.max(maxTextWidth, metrics.stringWidth(option));
			}
			return Math.max(170, maxTextWidth + 42);
		}

		private int getFirstRelativeGraphIndex() {
			for (int i = 0; i < graphType.length; i++) {
				if (OptionsArea.REL_RATIO_LINE.equals(graphType[i])) {
					return i;
				}
			}
			return -1;
		}

		private String ellipsizeToWidth(String text, java.awt.FontMetrics metrics, int maxWidth) {
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

		private int getIndex(Chart ch) {
			int sIndex = 0;
			for (int i = 0; i < graphType.length; i++) {
				if (!ch.getChartClassName().equalsIgnoreCase(graphClassName[i].trim()))
					continue;
				sIndex = i;
				break;
			}
			return sIndex;
		}

		public void actionPerformed(ActionEvent e1) {

			JList<String> listYrsForRelChart = new JList<String>(
					("back to original chart," + chart[idx].getChartColumn()).split(","));
			listYrsForRelChart.setSelectionMode(0);
			listYrsForRelChart.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {

					JList<?> list = (JList<?>) e.getSource();
					boolean adjust = e.getValueIsAdjusting();
					if (!adjust) {
						relativeIndex = list.getSelectedValue().equals("back to original chart") ? -1
								: list.getSelectedIndex() - 1;
						if (relativeIndex == -1) {
							resetChart();
							//setSelectedIndex(typeLineChart);
						}
					}
				}
			});

			JComboBox<?> source = (JComboBox<?>) e1.getSource();

			int selectedChartIndex = source.getSelectedIndex();
			String selectedValue=source.getSelectedItem().toString();
			//if (selectedChartIndex == typeRelativeLineChart) {
			if(selectedValue.compareTo(OptionsArea.REL_DIFF_BAR)==0
				|| selectedValue.compareTo(OptionsArea.REL_DIFF_LINE)==0
				|| selectedValue.compareTo(OptionsArea.REL_RATIO_LINE)==0) {
				resetChart();

				
				//setSelectedIndex(sele);
				listYrsForRelChart.setSelectedIndex(1);

				String options[] = { "ok" };
				JOptionPane pane0 = new JOptionPane(new JScrollPane(listYrsForRelChart), -1, 0, null, options,
						options[0]);
				JDialog dialog = pane0.createDialog("Please select a relative data value");
				dialog.setLayout(null);
				dialog.setResizable(true);
				dialog.setVisible(true);

			} else if (relativeIndex > -1) {

				resetChart();
			 setSelectedIndex(selectedChartIndex);
				listYrsForRelChart.setSelectedIndex(selectedChartIndex);

			}

			// trying this
			int index = selectedChartIndex;
			//if (selectedChartIndex == typeRelativeLineChart)
			//	index = typeLineChart;
			cn = graphClassName[index];
			// Create the new chart array and make sure the enclosing OptionsArea uses it
			Chart[] newCharts = ThumbnailUtilNew.createChart(cn, relativeIndex, chart, selectedValue);
			chart = newCharts;
			OptionsArea.this.chart = newCharts;

			// If we're inside a Breakout (Transpose) dialog, update that dialog so it replaces its single pane
			java.awt.Container top = javax.swing.SwingUtilities.getWindowAncestor(OptionsArea.this.jp);
			if (top instanceof Breakout) {
				((Breakout) top).updateChartPaneWithCharts(newCharts, sameScale);
				return;
			}

			setChartPane();
		}

		public void resetChart() {

			cn = "chart.CategoryLineChart";
			relativeIndex = -1;

			chart = oChart;
			OptionsArea.this.chart = oChart;

			java.awt.Container top = javax.swing.SwingUtilities.getWindowAncestor(OptionsArea.this.jp);
			if (top instanceof Breakout) {
				((Breakout) top).updateChartPaneWithCharts(oChart, sameScale);
				return;
			}

			setChartPane();

			// isLine = true;
			// setSelectedIndex(typeLineChart);

		}

	}

}