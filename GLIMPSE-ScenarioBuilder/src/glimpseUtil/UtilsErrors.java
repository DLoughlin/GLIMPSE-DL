package glimpseUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.sun.javafx.tk.Toolkit;

/**
 * Error-report dialog helpers extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsErrors {

	private static final UtilsErrors INSTANCE = new UtilsErrors();

	private final Pattern intPattern = Pattern.compile("-?[0-9]+");
	private final Pattern doublePattern = Pattern.compile("-?(([0-9]+)|([0-9]*\\.[0-9]+))");

	private GLIMPSEVariables vars;
	private GLIMPSEStyles styles;
	private GLIMPSEFiles files;
	private GLIMPSEUtils utils;

	private UtilsErrors() {
	}

	public static UtilsErrors getInstance() {
		return INSTANCE;
	}

	public void init(GLIMPSEVariables vars, GLIMPSEStyles styles, GLIMPSEFiles files, GLIMPSEUtils utils) {
		this.vars = vars;
		this.styles = styles;
		this.files = files;
		this.utils = utils;
	}

	public void displayArrayList(ArrayList<String> arrayListArg, String title) {
		Platform.runLater(() -> displayArrayList(arrayListArg, title, false));
	}

	public void displayArrayList(ArrayList<String> arrayListArg, String title, boolean doWrap) {
		if (styles == null || vars == null)
			return;
		final String finalTitle = title;
		Runnable displayTask = () -> {
			BorderPane border = new BorderPane();
			String usedTitle = finalTitle == null ? GLIMPSEUtils.LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			stage.setTitle(usedTitle);
			stage.setWidth(900);
			stage.setHeight(800);
			stage.setResizable(true);
			TextArea textArea = new TextArea();
			textArea.setEditable(false);
			textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			textArea.setMinHeight(0);
			textArea.setWrapText(doWrap);
			Button closeButton = utils.createButton(GLIMPSEUtils.LABEL_CLOSE, styles.getBigButtonWidth(), null);
			closeButton.setOnAction(e -> stage.close());
			StringBuilder text = new StringBuilder();
			if (arrayListArg != null) {
				for (String str : arrayListArg) {
					if (str.indexOf(vars.getEol()) < 0)
						text.append(str).append(vars.getEol());
					else
						text.append(str);
				}
				textArea.setText(text.toString());
				HBox buttonBox = new HBox();
				buttonBox.setPadding(new Insets(4, 4, 4, 4));
				buttonBox.setSpacing(5);
				buttonBox.setAlignment(Pos.CENTER);
				buttonBox.getChildren().addAll(closeButton);
				border.setCenter(textArea);
				border.setBottom(buttonBox);
				Scene scene = new Scene(border);
				stage.setScene(scene);
				stage.show();
			}
		};
		displayTask.run();
	}

	public void showPopupTableOfErrorReport(String title, ArrayList<String> csvData, int wd, int ht) {
		if (styles == null)
			return;
		if (csvData == null || csvData.isEmpty()) {
			utils.showPopupTableOfCSVData(title, csvData, wd, ht);
			return;
		}
		final String finalTitle = title;
		Runnable popupTask = () -> {
			String usedTitle = finalTitle == null ? GLIMPSEUtils.LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
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

			String[][] rawData = utils.getDataMatrixFromArrayList(csvData);
			int numCols = computeMaxRowLength(rawData);
			String[] headerRow = rawData.length > 0 ? rawData[0] : new String[0];
			int startRowIndex = rawData.length > 0 ? 1 : 0;
			int classCol = findColumnIndex(headerRow, "Classification");

			Class<?>[] types = new Class<?>[numCols];
			for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
				String[] column = extractColumn(rawData, columnIndex);
				types[columnIndex] = deduceColumnType(column);
				TableColumn<List<Object>, String> col = createColumn(types[columnIndex], columnIndex,
						getColumnHeader(rawData, columnIndex));
				installErrorReportOverflowBehavior(col, columnIndex, numCols);
				table.getColumns().add(col);
			}

			ObservableList<List<Object>> master = FXCollections.observableArrayList();
			for (int rowIndex = startRowIndex; rowIndex < rawData.length; rowIndex++) {
				List<Object> row = new ArrayList<>();
				for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
					row.add(getDataAsType(rawData[rowIndex], types[columnIndex], columnIndex));
				}
				master.add(row);
			}

			FilteredList<List<Object>> filtered = new FilteredList<>(master, row -> true);
			table.setItems(filtered);

			ChoiceBox<String> viewSelector = new ChoiceBox<>(FXCollections.observableArrayList(
					"All lines",
					"Major errors",
					"Moderate errors",
					"Minor errors"));
			viewSelector.getSelectionModel().select(0);
			viewSelector.setTooltip(new Tooltip("Choose which rows to display"));

			viewSelector.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
				int idx = newV == null ? 0 : newV.intValue();
				filtered.setPredicate(row -> {
					if (row == null)
						return false;
					if (idx == 0)
						return true;
					String classification = getCellString(row, classCol);
					switch (idx) {
					case 1:
						return "major".equalsIgnoreCase(classification);
					case 2:
						return "moderate".equalsIgnoreCase(classification);
					case 3:
						return "minor".equalsIgnoreCase(classification);
					default:
						return true;
					}
				});
				table.getSelectionModel().clearSelection();
				if (!table.getItems().isEmpty()) {
					table.scrollTo(0);
				}
			});

			Button saveAsBtn = new Button("Save As...");
			saveAsBtn.setTooltip(new Tooltip("Save the currently visible error report as CSV"));
			saveAsBtn.setOnAction(ev -> {
				try {
					File initialDir = new File(vars.getGlimpseLogDir());
					FileChooser.ExtensionFilter csvFilter = FileChooserPlus.createExtensionFilter("CSV files (*.csv)", "csv");
					File chosen = FileChooserPlus.showSaveDialog(stage, "Save Error Report", initialDir, "error_report.csv",
							csvFilter);
					if (chosen != null) {
						ArrayList<String> exportRows = new ArrayList<>();
						ArrayList<String> header = new ArrayList<>();
						for (int col = 0; col < numCols; col++) {
							header.add(sanitizeCsvField(getColumnHeader(rawData, col)));
						}
						exportRows.add(buildCsvRow(header, numCols));
						for (List<Object> row : filtered) {
							ArrayList<String> fields = new ArrayList<>();
							for (int col = 0; col < numCols; col++) {
								fields.add(sanitizeCsvField(getCellString(row, col)));
							}
							exportRows.add(buildCsvRow(fields, numCols));
						}
						files.saveFile(exportRows, chosen.getPath());
						utils.showInformationDialog("Information", "Export successful", "Saved report to: " + chosen.getPath());
					}
				} catch (Exception ex) {
					utils.showInformationDialog("Information", "Export failed", "Could not save report: " + ex.getMessage());
				}
			});

			HBox controls = new HBox(10, new Label("View:"), viewSelector, saveAsBtn);
			controls.setPadding(new Insets(6, 10, 6, 10));
			controls.setAlignment(Pos.CENTER_LEFT);

			HBox buttonBox = new HBox();
			buttonBox.setPadding(new Insets(4, 4, 4, 4));
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			buttonBox.getChildren().addAll(closeButton);

			border.setTop(controls);
			border.setCenter(table);
			border.setBottom(buttonBox);

			Scene scene = new Scene(border);
			try {
				java.net.URL cssUrl = GLIMPSEUtils.class.getResource("/resources/modern.css");
				if (cssUrl != null) {
					scene.getStylesheets().add(cssUrl.toExternalForm());
				}
			} catch (Exception ignored) {
			}
			stage.setScene(scene);
			stage.show();
		};
		popupTask.run();
	}

	private int findColumnIndex(String[] headerRow, String headerName) {
		if (headerRow == null || headerName == null)
			return -1;
		for (int i = 0; i < headerRow.length; i++) {
			String header = headerRow[i] == null ? "" : headerRow[i].trim();
			if (headerName.equalsIgnoreCase(header))
				return i;
		}
		return -1;
	}

	private String getCellString(List<Object> row, int colIndex) {
		if (row == null || colIndex < 0 || colIndex >= row.size())
			return "";
		Object value = row.get(colIndex);
		return value == null ? "" : value.toString();
	}

	private int computeMaxRowLength(String[][] data) {
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

	private String[] extractColumn(String[][] data, int columnIndex) {
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

	private Class<?> deduceColumnType(String[] column) {
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
			if (!intPattern.matcher(value).matches()) {
				allIntegers = false;
			}
			if (!doublePattern.matcher(value).matches()) {
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

	private String getColumnHeader(String[][] data, int columnIndex) {
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

	private Object getDataAsType(String[] row, Class<?> type, int columnIndex) {
		try {
			if (type == Integer.class) {
				if (columnIndex < row.length) {
					return Integer.valueOf(row[columnIndex]);
				}
				return Integer.valueOf(0);
			} else if (type == Double.class) {
				if (columnIndex < row.length) {
					return Double.valueOf(row[columnIndex]);
				}
				return Double.valueOf(0.0);
			} else {
				if (columnIndex < row.length) {
					return row[columnIndex];
				}
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	private TableColumn<List<Object>, String> createColumn(Class<?> type, int index, String name) {
		String text = name;
		TableColumn<List<Object>, String> col = new TableColumn<>(text);
		col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(index).toString()));
		return col;
	}

	private void installErrorReportOverflowBehavior(TableColumn<List<Object>, String> col, int colIndex,
			int totalColumns) {
		col.setCellFactory(column -> new TableCell<List<Object>, String>() {
			private final Text textNode = new Text();
			private final StackPane textPane = new StackPane(textNode);
			private final Rectangle clip = new Rectangle();

			{
				textNode.fontProperty().bind(fontProperty());
				textNode.fillProperty().bind(textFillProperty());
				textPane.setAlignment(Pos.CENTER_LEFT);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				clip.widthProperty().bind(widthProperty());
				clip.heightProperty().bind(heightProperty());
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					setClip(clip);
					toBack();
					return;
				}

				textNode.setText(item);
				setText(null);
				setGraphic(textPane);

				boolean allowOverflow = shouldAllowOverflow(item, colIndex, totalColumns);
				if (allowOverflow) {
					setClip(null);
					toFront();
				} else {
					setClip(clip);
					toBack();
				}
			}

			private boolean shouldAllowOverflow(String text, int columnIndex, int columnCount) {
				if (text == null || text.trim().isEmpty()) {
					return false;
				}
				Object rowObj = getTableRow() == null ? null : getTableRow().getItem();
				if (!(rowObj instanceof List)) {
					return false;
				}
				@SuppressWarnings("unchecked")
				List<Object> rowItem = (List<Object>) rowObj;
				if (!areRightCellsEmpty(rowItem, columnIndex, columnCount)) {
					return false;
				}
				double available = getWidth();
				if (available <= 0) {
					available = col.getWidth();
				}
				if (getInsets() != null) {
					available -= (getInsets().getLeft() + getInsets().getRight());
				}
				if (available <= 0) {
					return false;
				}
				Font font = getFont();
				if (font == null) {
					return false;
				}
				double textWidth = Toolkit.getToolkit().getFontLoader().computeStringWidth(text, font);
				return textWidth > (available + 1.0);
			}
		});
	}

	private boolean areRightCellsEmpty(List<Object> rowItem, int columnIndex, int columnCount) {
		if (rowItem == null) {
			return false;
		}
		for (int i = columnIndex + 1; i < columnCount; i++) {
			String value = i < rowItem.size() && rowItem.get(i) != null ? rowItem.get(i).toString() : "";
			if (!value.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private String sanitizeCsvField(String value) {
		if (value == null)
			return "";
		return value.replace(",", ";").replace("\r", " ").replace("\n", " ").trim();
	}

	private String buildCsvRow(List<String> fields, int columnCount) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < columnCount; i++) {
			if (i > 0)
				sb.append(",");
			String value = i < fields.size() ? fields.get(i) : "";
			sb.append(value == null ? "" : value);
		}
		return sb.toString();
	}
}
