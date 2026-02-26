/*
* LEGAL NOTICE
* This computer software was prepared by Battelle Memorial Institute,
* hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
* with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
* CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
* Distributed as open-source under the terms of the Educational Community 
* License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
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
*/
package ModelInterface.ModelGUI2.tables;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.QueryResultsPanel;
import filter.FilteredTable;

/**
 *   Author			Action							Date		Flag
 *  ================================================================== 			
 *	TWU				Add capability to accept 		1/2/2017	@1
 *					filtered Jtable data transfer
 *					Add capability to export
 *					data to clip board
 */

public class TableTransferHandler extends TransferHandler {
    private static final int MAX_DND_ROWS = 3000; // threshold to block DnD and show warning

	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return false;
	}

	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}

	public boolean importData(JComponent comp, Transferable t) {
		return false;
	}

	protected Transferable createTransferable(JComponent comp) {
		TransferableTable tt;
		try {
			tt = new TransferableTable(comp);
		} catch (UnsupportedOperationException e) {
			return null;
		}
		int estimatedRows = tt.getEstimatedRowCount();
		if (estimatedRows > MAX_DND_ROWS) {
			Component parent = SwingUtilities.getWindowAncestor(comp);
			JOptionPane.showMessageDialog(parent,
					"For large tables (>"+MAX_DND_ROWS+" rows), use Copy button or Ctrl+C / Ctrl+V on selected rows",
					"Large table - use clipboard or CSV export", JOptionPane.INFORMATION_MESSAGE);
			System.out.println("DnD blocked: table has " + estimatedRows + " rows (limit " + MAX_DND_ROWS + ")");
			return null; // cancel the drag
		}
		return tt;
	}
	// @1
	protected void exportDone(JComponent c, Transferable data, int action) {
        // Guard: createTransferable may have returned null to cancel the DnD.
        if (data == null) {
            // Nothing exported because we cancelled the drag - avoid NPE.
            return;
        }
        if (data instanceof TransferableTable) {
            System.out.println("Exported row count: " + ((TransferableTable) data).getCurRowCount());
        } else {
            // Unknown transferable type; do not attempt to inspect it.
            System.out.println("Export completed with non-table transferable.");
        }
    }// @1

	public class TransferableTable implements Transferable {
		private BaseTableModel bt;
		private JTable jtable;
		private DataFlavor[] transFlavors;
		private String fs;// --tai add
		private int curRowCount = 0;//@1
		private int estimatedRowCount = 0;

		public TransferableTable(JComponent comp) {

			if (comp instanceof JTabbedPane) {

				Component c = ((QueryResultsPanel) ((JTabbedPane) comp).getSelectedComponent()).getComponent(0);
				// If a JPanel is returned, QueryResultsPanel returned a Panel
				// with text,
				// so no table can be extracted

				if (c instanceof JSplitPane) {
					Component c1 = ((JSplitPane) c).getLeftComponent();
					try {
					if (c1 instanceof JScrollPane)
						bt = DbViewer.getTableModelFromComponent(((JTabbedPane) comp).getSelectedComponent());
					// @1
					else {
						JPanel jp = (JPanel) c1;
						BorderLayout bl = (BorderLayout) jp.getLayout();
						jtable = (JTable) ((JScrollPane) bl.getLayoutComponent("Center")).getViewport().getView();
					}//@1
					} catch(Exception e) {
						System.out.println("Attempting to continue after error: "+e);
					}
				}
			} else if (comp instanceof JTable) {
				bt = (BaseTableModel) ((JTable) comp).getModel(); // bt
																	// (BaseTableModel)
			} else {
				throw new UnsupportedOperationException("Can't transfer this component");
			}
			// Estimate row count quickly without building the full transfer string
			if (bt != null) {
				estimatedRowCount = bt.getRowCount();
			} else if (jtable != null) {
				estimatedRowCount = jtable.getRowCount();
			} else {
				estimatedRowCount = 0;
			}

			transFlavors = new DataFlavor[1];
             transFlavors[0] = DataFlavor.stringFlavor;

         }

		public int getEstimatedRowCount() {
			return estimatedRowCount;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return transFlavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for (DataFlavor tranFlavor : transFlavors) {
				if (tranFlavor.equals(flavor)) {
					return true;
				}
			}
			return false;
		}

		public String getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			// only string for now..
			if (bt != null)
				return bt.exportToText('\t');
			// @1
			else {
				if (fs != null)
					return fs;
				else
					return packString(jtable);

			}// @1
		}
		
		// @1
		protected String packString(JTable jtable) {
			fs = "";
			for (int i = 0; i < jtable.getColumnCount(); i++)
				fs = fs + jtable.getColumnName(i) + "\t";
			fs = fs + "\n";

			for (int i = 0; i < jtable.getRowCount(); i++) {
				for (int j = 0; j < jtable.getColumnCount(); j++) {
					String cls = jtable.getColumnClass(j).getName();
					if (cls.equals("java.lang.Double")) {
						fs = fs + (String.valueOf(jtable.getValueAt(i, j))) + "\t";
					} else
						fs = fs + (String) jtable.getValueAt(i, j) + "\t";
				}
				fs = fs + "\n";
				curRowCount = i + 1; // record exported rows correctly
			}
			return fs;
		}

		public int getCurRowCount() {
			return curRowCount;
		} //@1


	}

}