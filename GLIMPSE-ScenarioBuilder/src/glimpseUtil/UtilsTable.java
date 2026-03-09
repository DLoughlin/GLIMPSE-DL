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
*  * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
* 
*/
package glimpseUtil;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import glimpseElement.DataPoint;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class UtilsTable {

	public static NumberFormat numberFormatter = NumberFormat.getNumberInstance();
	private static final int MAX_ROWS_AFTER_PASTE = 50;

	/**
	 * Install the keyboard handler: + CTRL + C = copy to clipboard + CTRL + V =
	 * paste to clipboard
	 * 
	 * @param table
	 */
	public static void installCopyPasteHandler(TableView<?> table) {

		// install copy/paste keyboard handler
		table.setOnKeyPressed(new TableKeyEventHandler());

	}

	/**
	 * Copy/Paste keyboard event handler. The handler uses the keyEvent's source
	 * for the clipboard data. The source must be of type TableView.
	 */
	public static class TableKeyEventHandler implements EventHandler<KeyEvent> {

		KeyCodeCombination copyKeyCodeCompination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
		KeyCodeCombination pasteKeyCodeCompination = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_ANY);

		@Override
		public void handle(final KeyEvent keyEvent) {

			if (copyKeyCodeCompination.match(keyEvent)) {

				if (keyEvent.getSource() instanceof TableView) {

					// copy to clipboard
					copySelectionToClipboard((TableView<?>) keyEvent.getSource());

					// event is handled, consume it
					keyEvent.consume();

				}

			} else if (pasteKeyCodeCompination.match(keyEvent)) {

				if (keyEvent.getSource() instanceof TableView) {

					// copy to clipboard
					pasteFromClipboard((TableView<?>) keyEvent.getSource());

					// event is handled, consume it
					keyEvent.consume();

				}

			}

		}

	}

	/**
	 * Get table selection and copy it to the clipboard.
	 * 
	 * @param table
	 */
	public static void copySelectionToClipboard(TableView<?> table) {

		StringBuilder clipboardString = new StringBuilder();

		ObservableList<Integer> positionList = table.getSelectionModel().getSelectedIndices();

		int item_no = 0;

		for (Integer position : positionList) {

			int row = position.intValue();

			for (int col = 0; col < table.getColumns().size(); col++) {

				if (col == 0) {
					if (item_no != 0)
						clipboardString.append('\n');
					item_no++;
				} else {
					clipboardString.append('\t');
				}

				// create string from cell
				String text = "";

				Object observableValue = table.getColumns().get(col).getCellObservableValue(row);

				// null-check: provide empty string for nulls
				if (observableValue == null) {
					text = "";
				} else if (observableValue instanceof DoubleProperty) {

					text = numberFormatter.format(((DoubleProperty) observableValue).get());

				} else if (observableValue instanceof IntegerProperty) {

					text = numberFormatter.format(((IntegerProperty) observableValue).get());

				} else if (observableValue instanceof StringProperty) {

					text = ((StringProperty) observableValue).get();

				} else {
					Debug.log("Unsupported observable value: " + observableValue);
				}

				// add new item to clipboard
				clipboardString.append(text);
			}
		}

		final ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(clipboardString.toString());
		Clipboard.getSystemClipboard().setContent(clipboardContent);

	}

	public static void pasteFromClipboard(TableView<?> table) {
		int table_size = table.getItems().size();
		Debug.log("Table size: " + table_size);

		if (table.getSelectionModel().getSelectedCells().size() == 0) {
			return;
		}

		TablePosition<?, ?> pos = table.getSelectionModel().getSelectedCells().get(0);
		int startRow = pos.getRow();
		int startCol = pos.getColumn();

		Debug.log("Pasting starting at row " + startRow + " col " + startCol);

		String pasteString = Clipboard.getSystemClipboard().getString();
		if (pasteString == null || pasteString.trim().isEmpty()) {
			return;
		}

		List<List<String>> grid = ClipboardTableParser.parseGrid(pasteString);
		int maxCols = ClipboardTableParser.maxColumns(grid);
		if (grid.isEmpty()) {
			return;
		}

		if (maxCols > 3 && grid.size() < 3) {
			if (grid.size() == 1) {
				grid = ClipboardTableParser.transposeSingleRowToColumn(grid.get(0));
			} else if (grid.size() == 2) {
				grid = ClipboardTableParser.pairRowsToTwoColumns(grid.get(0), grid.get(1));
				maxCols = 2;
			}
			maxCols = ClipboardTableParser.maxColumns(grid);
		}

		int desiredRowCount = startRow + grid.size();
		ensureRowCapacityForPaste(table, desiredRowCount);

		for (int r = 0; r < grid.size(); r++) {
			int rowTable = startRow + r;
			if (rowTable >= table.getItems().size()) {
				int extra = rowTable - table.getItems().size();
				Debug.log("More rows being pasted than in table: " + extra + ".");
				continue;
			}

			List<String> rowCells = grid.get(r);
			int colsToPaste = Math.min(rowCells.size(), table.getColumns().size() - startCol);
			if (maxCols == 1) {
				colsToPaste = Math.min(1, table.getColumns().size() - startCol);
			}

			for (int c = 0; c < colsToPaste; c++) {
				int colTable = startCol + c;
				if (colTable >= table.getColumns().size()) {
					Debug.log("More columns being pasted than in table.");
					continue;
				}

				String clipboardCellContent = rowCells.size() > c ? rowCells.get(c) : "";
				clipboardCellContent = clipboardCellContent == null ? "" : clipboardCellContent.trim();

				TableColumn tableColumn = table.getColumns().get(colTable);
				ObservableValue observableValue = tableColumn.getCellObservableValue(rowTable);

				if (observableValue instanceof DoubleProperty) {
					try {
						double value = numberFormatter.parse(clipboardCellContent).doubleValue();
						((DoubleProperty) observableValue).set(value);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (observableValue instanceof IntegerProperty) {
					try {
						int value = NumberFormat.getInstance().parse(clipboardCellContent).intValue();
						((IntegerProperty) observableValue).set(value);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				} else if (observableValue instanceof StringProperty) {
					((StringProperty) observableValue).set(clipboardCellContent);
				} else {
					Debug.log("Unsupported observable value: " + observableValue);
				}

				Debug.log(rowTable + File.separator + colTable);
			}
		}

	}

	private static void ensureRowCapacityForPaste(TableView<?> table, int desiredRowCount) {
		desiredRowCount = Math.min(desiredRowCount, MAX_ROWS_AFTER_PASTE);

		if (desiredRowCount <= table.getItems().size()) {
			return;
		}

		ObservableList<?> items = table.getItems();
		if (items == null || items.isEmpty()) {
			return;
		}

		Object firstItem = items.get(0);
		if (firstItem instanceof DataPoint) {
			@SuppressWarnings("unchecked")
			ObservableList<DataPoint> dpItems = (ObservableList<DataPoint>) items;
			while (dpItems.size() < desiredRowCount) {
				dpItems.add(new DataPoint("", ""));
			}
		}
	}
}
