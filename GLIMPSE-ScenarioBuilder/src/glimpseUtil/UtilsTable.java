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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import glimpseElement.DataPoint;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class UtilsTable {

	public static NumberFormat numberFormatter = NumberFormat.getNumberInstance();
	private static final int MAX_ROWS_AFTER_PASTE = 50;
	private static final Pattern INT_PATTERN = Pattern.compile("-?[0-9]+");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?(([0-9]+)|([0-9]*\\.[0-9]+))");

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

	public static String[][] getDataMatrixFromArrayList(ArrayList<String> data) {
		if (data == null || data.isEmpty())
			return new String[0][0];
		int numRows = data.size();
		String[][] dataMatrix = new String[numRows][];
		for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
			String row = data.get(rowIndex);
			dataMatrix[rowIndex] = row == null ? new String[0] : row.split(",", -1);
		}
		return dataMatrix;
	}

	public static int computeMaxRowLength(String[][] data) {
		if (data == null)
			return 0;
		int maxLength = 0;
		for (String[] row : data) {
			if (row != null && row.length > maxLength) {
				maxLength = row.length;
			}
		}
		return maxLength;
	}

	public static String[] extractColumn(String[][] data, int columnIndex) {
		if (data == null || columnIndex < 0)
			return new String[0];
		String[] column = new String[data.length];
		for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
			if (data[rowIndex] != null && columnIndex < data[rowIndex].length && data[rowIndex][columnIndex] != null) {
				column[rowIndex] = data[rowIndex][columnIndex];
			} else {
				column[rowIndex] = "";
			}
		}
		return column;
	}

	public static Class<?> deduceColumnType(String[] column) {
		if (column == null || column.length <= 1)
			return String.class;
		boolean hasValue = false;
		boolean allIntegers = true;
		boolean allNumeric = true;
		for (int rowIndex = 1; rowIndex < column.length; rowIndex++) {
			String value = column[rowIndex];
			if (value == null)
				continue;
			value = value.trim();
			if (value.isEmpty())
				continue;
			hasValue = true;
			if (!INT_PATTERN.matcher(value).matches()) {
				allIntegers = false;
			}
			if (!DOUBLE_PATTERN.matcher(value).matches()) {
				allNumeric = false;
				break;
			}
		}
		if (!hasValue)
			return String.class;
		if (allIntegers)
			return Integer.class;
		if (allNumeric)
			return Double.class;
		return String.class;
	}

	public static String getColumnHeader(String[][] data, int columnIndex) {
		if (columnIndex < 0)
			return "";
		String fallback = "Column " + (columnIndex + 1);
		if (data == null || data.length == 0 || data[0] == null || columnIndex >= data[0].length)
			return fallback;
		String header = data[0][columnIndex];
		if (header == null)
			return fallback;
		header = header.trim();
		return header.isEmpty() ? fallback : header;
	}

	public static Object getDataAsType(String[] row, Class<?> type, int columnIndex) {
		try {
			if (type == Integer.class) {
				if (row != null && columnIndex < row.length) {
					return Integer.valueOf(row[columnIndex]);
				}
				return Integer.valueOf(0);
			} else if (type == Double.class) {
				if (row != null && columnIndex < row.length) {
					return Double.valueOf(row[columnIndex]);
				}
				return Double.valueOf(0.0);
			}
			if (row != null && columnIndex < row.length) {
				return row[columnIndex];
			}
			return "";
		} catch (Exception e) {
			return "";
		}
	}

	public static TableColumn<List<Object>, String> createColumn(Class<?> type, int index, String name) {
		TableColumn<List<Object>, String> col = new TableColumn<>(name);
		col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(index).toString()));
		return col;
	}

	public static void showPopupTableOfCSVData(String title, ArrayList<String> csvData, int wd, int ht,
			GLIMPSEUtils utils, GLIMPSEVariables vars, GLIMPSEStyles styles, GLIMPSEFiles files) {
		if (styles == null)
			return;
		final String finalTitle = title;
		Runnable popupTask = () -> {
			String usedTitle = finalTitle == null ? GLIMPSEUtils.LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			try {
				javafx.stage.Window owner = UtilsDialogs.getPrimaryOwnerWindow();
				if (owner != null) {
					stage.initOwner(owner);
				}
			} catch (Exception ignored) {}
			stage.setTitle(usedTitle);
			stage.setWidth(wd);
			stage.setHeight(ht);
			BorderPane border = new BorderPane();
			stage.setResizable(true);

			Button closeButton = utils.createButton(GLIMPSEUtils.LABEL_CLOSE, styles.getBigButtonWidth(), null);
			closeButton.setOnAction(e -> stage.close());

			TableView<List<Object>> table = new TableView<>();
			table.setEditable(false);
			table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			table.setMinHeight(0);
			UtilsTable.installCopyPasteHandler(table);
			table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			String[][] rawData = getDataMatrixFromArrayList(csvData);
			int numCols = computeMaxRowLength(rawData);
			int startRowIndex = rawData.length > 0 ? 1 : 0;

			Class<?>[] types = new Class<?>[numCols];
			for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
				String[] column = extractColumn(rawData, columnIndex);
				types[columnIndex] = deduceColumnType(column);
				table.getColumns().add(createColumn(types[columnIndex], columnIndex, getColumnHeader(rawData, columnIndex)));
			}
			for (int rowIndex = startRowIndex; rowIndex < rawData.length; rowIndex++) {
				List<Object> row = new ArrayList<>();
				for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
					row.add(getDataAsType(rawData[rowIndex], types[columnIndex], columnIndex));
				}
				table.getItems().add(row);
			}

			HBox buttonBox = new HBox();
			buttonBox.setPadding(new Insets(4, 4, 4, 4));
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			Button exportButton = utils.createButton("Export", styles.getBigButtonWidth(), null);
			exportButton.setOnAction(ev -> {
				try {
					File initialDir = new File(vars.getGlimpseLogDir());
					FileChooser.ExtensionFilter csvFilter = FileChooserPlus.createExtensionFilter("CSV files (*.csv)", "csv");
					File chosen = FileChooserPlus.showSaveDialog(stage, "Save Scenario Report", initialDir,
							"scenario_report.csv", csvFilter);
					if (chosen != null) {
						files.saveFile(csvData, chosen.getPath());
						utils.showInformationDialog("Information", "Export successful",
								"Saved report to: " + chosen.getPath());
					}
				} catch (Exception ex) {
					utils.showInformationDialog("Information", "Export failed",
							"Could not save report: " + ex.getMessage());
				}
			});

			buttonBox.getChildren().addAll(exportButton, closeButton);
			border.setCenter(table);
			border.setBottom(buttonBox);

			Scene scene = new Scene(border);
			stage.setScene(scene);
			stage.setOnShown(e -> {
				try {
					javafx.stage.Window owner = stage.getOwner();
					if (owner == null || !owner.isShowing()) {
						owner = UtilsDialogs.getPrimaryOwnerWindow();
					}
					if (owner != null && owner.isShowing()) {
						double w = stage.getWidth();
						double h = stage.getHeight();
						if (w > 0 && h > 0) {
							stage.setX(owner.getX() + ((owner.getWidth() - w) / 2.0));
							stage.setY(owner.getY() + ((owner.getHeight() - h) / 2.0));
						}
					}
				} catch (Exception ignored) {}
			});
			stage.show();
		};
		popupTask.run();
	}
}