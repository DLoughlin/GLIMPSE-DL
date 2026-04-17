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
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;
import chart.Chart;
import conversionUtil.ArrayConversion;

/**
 * DataPanel handles displaying tabular data and chart panels, allowing subsetting
 * and selection of data for charting. Supports both category and XY datasets.
 * <p>
 * Author Action Date Flag
 * =======================================================================
 * TWU   created 1/2/2016
 */
public class DataPanel extends JPanel implements ListSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final int MAX_VISIBLE_TABLE_ROWS = 10;
	protected DefaultTableModel tableModel;
	protected TableColumnModel cmodel;
	protected DefaultTableCellRenderer renderer;
	protected JTable table;
	protected String tableCol[];
	protected int id;
	protected JFreeChart[] chart;
	protected JFreeChart[] copyChart;
	protected LegendItemCollection copyLgd;
	// Inherit values
	protected DefaultCategoryDataset[] cds;
	protected XYDataset ds;
	protected String dataValue[][];
	protected boolean addRow = true;
	protected JScrollPane tableScrollPane;
	/** Cached significant-digits preference; -1 means not yet loaded. Thread-safe via AtomicInteger. */
	private final AtomicInteger cachedSignificantDigits = new AtomicInteger(-1);

	public DataPanel(JFreeChart ch) {
		init(ch);
	}

	public DataPanel(Chart[] charts, int id) {
		this.id = id;
		init(charts);
	}

	/**
	 * Initialize DataPanel with multiple charts.
	 * @param charts Array of Chart objects
	 */
	private void init(Chart[] charts) {
		setLayout(new BorderLayout());
		chart = new JFreeChart[charts.length];
		copyChart = new JFreeChart[charts.length];
		for (int i = 0; i < charts.length; i++) {
			try {
				chart[i] = charts[i].getChart();
				if (chart[i] != null)
					copyChart[i] = (JFreeChart) chart[i].clone();
				else
					copyChart = chart;
			} catch (CloneNotSupportedException e) {
				copyChart = chart;
			}
		}
		// Set legend items depending on plot type
		if (copyChart[id].getPlot().getPlotType().contains("XY")) {
			copyLgd = copyChart[id].getXYPlot().getFixedLegendItems();
		} else {
			copyLgd = copyChart[id].getCategoryPlot().getFixedLegendItems();
		}
		crtTable(chart[id]);
	}

	/**
	 * Initialize DataPanel with a single chart.
	 * @param ch JFreeChart object
	 */
	private void init(JFreeChart ch) {
		setLayout(new BorderLayout());
		chart = new JFreeChart[1];
		copyChart = new JFreeChart[1];
		try {
			chart[0] = ch;
			copyChart[0] = (JFreeChart) ch.clone();
		} catch (CloneNotSupportedException e) {
			copyChart = chart;
		}
		crtTable(chart[0]);
	}

	/**
	 * Create and configure the JTable for displaying data.
	 * @param chart JFreeChart to associate with the table
	 */
	private void crtTable(final JFreeChart chart) {
		table = new JTable();
		tableModel = (DefaultTableModel) table.getModel();
		tableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				applyTableViewportSizing();
			}
		});
		cmodel = table.getColumnModel();
		renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(4); // Center alignment
		renderer.setBorder(null);
		Font headerFont = UIManager.getFont("TableHeader.font");
		if (headerFont == null) {
			headerFont = table.getTableHeader().getFont();
		}
		if (headerFont != null) {
			table.getTableHeader().setFont(headerFont.deriveFont(Font.PLAIN,
					(float) Math.max(8, InterfaceMain.getConfiguredFontSize() + 3)));
		}
		table.setAutoCreateRowSorter(false);
		Font tableFont = UIManager.getFont("Table.font");
		if (tableFont == null) {
			tableFont = table.getFont();
		}
		if (tableFont != null) {
			table.setFont(tableFont.deriveFont((float) Math.max(8, InterfaceMain.getConfiguredFontSize() + 2)));
		}
		table.setRowHeight(table.getFont().getSize() + 10);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.setDefaultEditor(Object.class, null); // Make table non-editable
		tableScrollPane = new JScrollPane(table);
		tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tableScrollPane.setPreferredSize(new Dimension(700, 100));
		add(tableScrollPane, "Center");
		applyTableViewportSizing();
	}

	/**
	 * Sizes the table viewport to show up to MAX_VISIBLE_TABLE_ROWS rows,
	 * adding a vertical scrollbar only when there are more rows.
	 */
	protected void applyTableViewportSizing() {
		if (table == null || tableScrollPane == null) {
			return;
		}
		int rowCount = table.getRowCount();
		int visibleRows = Math.min(Math.max(1, rowCount), MAX_VISIBLE_TABLE_ROWS);
		int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
		int rowHeight = Math.max(1, table.getRowHeight());
		int preferredHeight = headerHeight + (visibleRows * rowHeight) + 2;
		int preferredWidth = Math.max(700, tableScrollPane.getPreferredSize().width);

		tableScrollPane.setVerticalScrollBarPolicy(
				rowCount > MAX_VISIBLE_TABLE_ROWS ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		tableScrollPane.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
		tableScrollPane.revalidate();
		revalidate();
	}

	/**
	 * Returns a stable preferred height for displaying this data pane with
	 * up to MAX_VISIBLE_TABLE_ROWS rows visible in the table viewport.
	 */
	public int getPreferredDisplayHeight() {
		int preferred = getPreferredSize().height;
		if (table == null || tableScrollPane == null) {
			return preferred;
		}
		Insets panelInsets = getInsets();
		Insets scrollInsets = tableScrollPane.getInsets();
		int rowCount = table.getRowCount();
		int visibleRows = Math.min(Math.max(1, rowCount), MAX_VISIBLE_TABLE_ROWS);
		int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
		int viewportHeight = headerHeight + (visibleRows * Math.max(1, table.getRowHeight())) + 2;
		int tableHeight = viewportHeight + scrollInsets.top + scrollInsets.bottom;
		int contentHeight = tableHeight + panelInsets.top + panelInsets.bottom;
		return Math.max(preferred, contentHeight);
	}

	/**
	 * Set custom cell renderer and editor for columns except the first.
	 */
	protected void SetColumnModel() {
		for (int j = 1; j < table.getColumnCount(); j++) {
			cmodel.getColumn(j).setCellRenderer(renderer);
			cmodel.getColumn(j).setCellEditor(table.getDefaultEditor(getClass()));
		}
	}

	/**
	 * Read the significant digits preference from properties (Preferences dialog).
	 * Falls back to defaultDigits if missing/invalid.
	 * The result is cached in an AtomicInteger so repeated calls (including from
	 * background threads) avoid repeated calls to InterfaceMain.getInstance().
	 */
	protected int getPreferredSignificantDigits(int defaultDigits) {
		int cached = cachedSignificantDigits.get();
		if (cached >= 0) {
			return cached;
		}
		int ret = defaultDigits;
		try {
			InterfaceMain instance = InterfaceMain.getInstance();
			if (instance != null) {
				String pref = instance.getProperties().getProperty("significantDigits",
						String.valueOf(defaultDigits));
				if (pref != null) {
					pref = pref.trim();
					// Preferences currently supports 2/3/5; be permissive in case properties file is edited.
					int parsed = Integer.parseInt(pref);
					if (parsed > 0) {
						ret = parsed;
					}
				}
			}
		} catch (Exception ignored) {
			// use defaultDigits
		}
		cachedSignificantDigits.set(ret);
		return ret;
	}

	/**
	 * Populate table with category dataset values, rounding as needed.
	 * @param cds Array of DefaultCategoryDataset
	 * @param n Number of digits to round to
	 */
	public void setDigit(DefaultCategoryDataset[] cds, int n) {
		// If caller passes a positive n we will still honor the user preference (keeps UI consistent).
		final int digits = getPreferredSignificantDigits(n > 0 ? n : 3);
		int r = cds[0].getRowCount();
		int c = cds[0].getColumnCount();

		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				if (digits > 0 && !DbViewer.disableSigDigits) {
					dataValue[i][j + 1] = String.valueOf(
							conversionUtil.DataConversion.roundDouble(cds[0].getValue(i, j).doubleValue(), digits));
				} else {
					dataValue[i][j + 1] = String.valueOf(cds[0].getValue(i, j));
				}
			}
		}

		String[][] tranDataValue = ArrayConversion.arrayDimReverse(dataValue);
		for (int k = 1; k < cds.length; k++) {
			for (int i = 0; i < cds[k].getRowCount(); i++) {
				String[] temp = new String[cds[k].getColumnCount()];
				for (int j = 0; j < cds[k].getColumnCount(); j++) {
					if (addRow) {
						if (digits > 0 && !DbViewer.disableSigDigits) {
							dataValue[r + i][j + 1] = String.valueOf(conversionUtil.DataConversion
									.roundDouble(cds[k].getValue(i, j).doubleValue(), digits));
						} else {
							dataValue[r + i][j + 1] = String.valueOf(Math.round((double) cds[k].getValue(i, j)));
						}
					} else {
						if (digits > 0 && !DbViewer.disableSigDigits) {
							temp[j] = String.valueOf(conversionUtil.DataConversion
									.roundDouble(cds[k].getValue(i, j).doubleValue(), digits));
						} else {
							temp[j] = String.valueOf(Math.round((double) cds[k].getValue(i, j)));
						}
					}
				}
				if (!addRow) {
					tranDataValue[i + 1 + c] = temp;
				}
			}

			if (addRow) {
				r += cds[k].getRowCount();
			} else {
				c += cds[k].getRowCount();
			}

		}

		if (!addRow) {
			dataValue = ArrayConversion.arrayDimReverse(tranDataValue);
		}
		tableModel.setDataVector(dataValue, tableCol);
		// Comparator for sorting columns with double values
		Comparator<String> columnDoubleComparator =
			    (String v1, String v2) -> {

			    //cast v1 to double
			    Double val1=Double.parseDouble(v1);
			    //cast v2 to double
			    Double val2=Double.parseDouble(v2);
			    //return result
			   
			    	
			  return Double.compare(val1, val2);

			};

		
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);
		// Add custom sorters to columns that are numbers
		for(int colC=0;colC<table.getColumnCount();colC++) {
			String clsName = table.getColumnName(colC);
			try {
				Double.parseDouble(clsName);
				sorter.setComparator(colC, columnDoubleComparator);
			} catch (Exception e) {
				// Not a numeric column, skip
			}
		}
	}

	/**
	 * Populate table with XY dataset values, rounding as needed.
	 * @param ds XYDataset
	 * @param n Number of digits to round to
	 */
	public void setDigit(XYDataset ds, int n) {
		final int digits = getPreferredSignificantDigits(n > 0 ? n : 3);
		int l = 0;
		for (int k = 0; k < copyChart[id].getXYPlot().getDatasetCount(); k++) {
			ds = copyChart[id].getXYPlot().getDataset(k);
			for (int i = 0; i < ds.getSeriesCount(); i++) {
				for (int j = 0; j < ds.getItemCount(i); j++) {
					if (digits > 0 && !DbViewer.disableSigDigits) {
						dataValue[l + i][j + 1] = String
								.valueOf(conversionUtil.DataConversion.roundDouble(ds.getYValue(i, j), digits));
					} else {
						dataValue[l + i][j + 1] = String.valueOf(ds.getYValue(i, j));
					}
				}

			}
			l += ds.getSeriesCount();
		}
		tableModel.setDataVector(dataValue, tableCol);
	}

	/**
	 * Handles selection changes in the table, updating selected rows/columns.
	 * @param e ListSelectionEvent
	 */
	public void valueChanged(ListSelectionEvent e) {
		boolean adjust = e.getValueIsAdjusting();

		if (!adjust) {
			int selectedC[] = null;
			int selectedR[] = null;

			if (table.getRowSelectionAllowed()) {
				selectedR = table.getSelectedRows();
				for (int i = 0; i < selectedR.length; i++) {
					if (selectedR[i] < table.getRowCount() - 1) {
						selectedR[i] = table.convertRowIndexToModel(selectedR[i]);
					} else {
						selectedR = Arrays.copyOf(selectedR, selectedR.length - 1);
						break;
					}
				}
			}

			if (table.getColumnSelectionAllowed()) {
				selectedC = table.getSelectedColumns();
				for (int i = 0; i < selectedC.length; i++) {
					if (selectedC[i] < table.getColumnCount() - 1) {
						selectedC[i] = table.convertColumnIndexToModel(selectedC[i]) - 1;
					} else {
						selectedC = Arrays.copyOf(selectedC, selectedC.length - 1);
						break;
					}
				}
			}
			/*
			if (selectedC[0] < 0) {
				if (selectedC.length == 1) {
					selectedC = new int[table.getColumnCount() - 2];
					for (int i = 0; i < selectedC.length; i++)
						selectedC[i] = i + 1;
					for (int i = 0; i < selectedC.length; i++)
						selectedC[i] = table.convertColumnIndexToModel(selectedC[i]) - 1;
				} else {
					((javax.swing.DefaultListSelectionModel) e.getSource()).setValueIsAdjusting(true);
					table.clearSelection();
					JOptionPane.showMessageDialog(null, "Select first column only or without the first column",
							"Information", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}*/
			//GraphDisplayUtil.showSelectRow(selectedC, selectedR, chart[id], copyChart[id], copyLgd);
		}

	}

	public String[] getTableCol() {
		return tableCol;
	}

	public String[][] getDataValue() {
		return dataValue;
	}

	public DefaultTableModel getTableModel() {
		return tableModel;
	}

	public TableColumnModel getCmodel() {
		return cmodel;
	}

	public JFreeChart getChart() {
		return chart[id];
	}

	public JFreeChart getCopyChart() {
		return copyChart[id];
	}

	public DefaultCategoryDataset[] getCds() {
		return cds;
	}

	public XYDataset getDs() {
		return ds;
	}

	/**
	 * Copies the full table contents to the system clipboard as tab-delimited text,
	 * including the header row and all visible rows.
	 */
	public boolean copyTableToClipboard() {
		String clipboardText = buildTableClipboardText();
		if (clipboardText == null || clipboardText.isEmpty()) {
			return false;
		}
		try {
			StringSelection selection = new StringSelection(clipboardText);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
			return true;
		} catch (IllegalStateException ex) {
			JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(),
					"Unable to access clipboard. Please try again.",
					"Clipboard Busy", JOptionPane.WARNING_MESSAGE);
			return false;
		}
	}

	private String buildTableClipboardText() {
		if (table == null || table.getColumnCount() == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int col = 0; col < table.getColumnCount(); col++) {
			if (col > 0) {
				builder.append('\t');
			}
			String header = table.getColumnName(col);
			builder.append(header == null ? "" : header);
		}
		builder.append(System.lineSeparator());

		for (int row = 0; row < table.getRowCount(); row++) {
			for (int col = 0; col < table.getColumnCount(); col++) {
				if (col > 0) {
					builder.append('\t');
				}
				Object value = table.getValueAt(row, col);
				builder.append(value == null ? "" : value.toString());
			}
			builder.append(System.lineSeparator());
		}
		return builder.toString();
	}

}
