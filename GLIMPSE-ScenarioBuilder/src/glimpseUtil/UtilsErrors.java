package glimpseUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import gui.ScenarioBuilder;
import gui.ScenarioLibraryReportHelper;
import gui.ScenarioLibraryReportHelper.ErrorTextReport;
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
import javafx.stage.Window;

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

	/**
	 * Returns the shared error-report helper instance.
	 *
	 * @return singleton {@link UtilsErrors} instance
	 */
	public static UtilsErrors getInstance() {
		return INSTANCE;
	}

	/**
	 * Initializes the helper with shared GLIMPSE services.
	 *
	 * @param vars shared variables instance
	 * @param styles shared style definitions
	 * @param files shared file utilities
	 * @param utils shared utility facade
	 */
	public void init(GLIMPSEVariables vars, GLIMPSEStyles styles, GLIMPSEFiles files, GLIMPSEUtils utils) {
		this.vars = vars;
		this.styles = styles;
		this.files = files;
		this.utils = utils;
	}

	/**
	 * Displays a list of lines in a text dialog without line wrapping.
	 *
	 * @param arrayListArg lines to display
	 * @param title dialog title
	 */
	public void displayArrayList(ArrayList<String> arrayListArg, String title) {
		Platform.runLater(() -> displayArrayList(arrayListArg, title, false));
	}

	/**
	 * Displays a list of lines in a text dialog.
	 *
	 * @param arrayListArg lines to display
	 * @param title dialog title
	 * @param doWrap whether line wrapping should be enabled
	 */
	public void displayArrayList(ArrayList<String> arrayListArg, String title, boolean doWrap) {
		if (styles == null || vars == null)
			return;
		final String finalTitle = title;
		Runnable displayTask = () -> {
			BorderPane border = new BorderPane();
			String usedTitle = finalTitle == null ? GLIMPSEUtils.LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			initializeStageOwnerAndCentering(stage);
			stage.setTitle(usedTitle);
			stage.setWidth(900);
			stage.setHeight(800);
			stage.setResizable(true);
			stage.setAlwaysOnTop(true);
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
				ScenarioBuilder.applyModernTheme(scene);
				stage.setScene(scene);
				stage.show();
			}
		};
		displayTask.run();
	}

	/**
	 * Shows the scenario-library error report using the richer text report view.
	 *
	 * @param title popup title
	 * @param csvData CSV rows describing the report
	 * @param wd popup width
	 * @param ht popup height
	 */
	public void showPopupTableOfErrorReport(String title, ArrayList<String> csvData, int wd, int ht) {
		if (styles == null) {
			return;
		}
		if (csvData == null || csvData.isEmpty()) {
			utils.showPopupTableOfCSVData(title, csvData, wd, ht);
			return;
		}
		ScenarioLibraryReportHelper.ErrorTextReport report = buildTextReportFromCsv(title, csvData);
		showTextErrorReport(report, wd, ht);
	}

	/**
	 * Displays a formatted error text report with filtering and export controls.
	 *
	 * @param report report model to display
	 * @param wd popup width
	 * @param ht popup height
	 */
	public void showTextErrorReport(ScenarioLibraryReportHelper.ErrorTextReport report, int wd, int ht) {
		if (styles == null || report == null) {
			return;
		}
		Runnable popupTask = () -> {
			String usedTitle = report.getTitle() == null || report.getTitle().trim().isEmpty()
					? GLIMPSEUtils.LABEL_DISPLAY
					: report.getTitle();
			Stage stage = new Stage();
			initializeStageOwnerAndCentering(stage);
			stage.setTitle(usedTitle);
			stage.setWidth(wd);
			stage.setHeight(ht);
			stage.setResizable(true);

			BorderPane border = new BorderPane();
			TextArea textArea = new TextArea();
			textArea.setEditable(false);
			textArea.setWrapText(false);
			textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			textArea.setMinHeight(0);

			ChoiceBox<String> viewSelector = new ChoiceBox<>(FXCollections.observableArrayList(report.getFilterOptions()));
			if (viewSelector.getItems().isEmpty()) {
				viewSelector.getItems().add("All lines");
			}
			viewSelector.getSelectionModel().select(0);
			viewSelector.setTooltip(new Tooltip("Choose which lines to display"));

			Runnable refreshText = () -> {
				String selectedFilter = viewSelector.getValue();
				String text = report.buildText(selectedFilter);
				if ((text == null || text.trim().isEmpty()) && !report.hasVisibleContent(selectedFilter)) {
					text = "No errors reported" + System.lineSeparator();
				}
				textArea.setText(text == null ? "" : text);
				textArea.positionCaret(0);
				textArea.setScrollTop(0);
				textArea.setScrollLeft(0);
			};
			refreshText.run();
			viewSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshText.run());

			Button saveAsBtn = new Button("Save As...");
			saveAsBtn.setTooltip(new Tooltip("Save the currently visible error report as text"));
			saveAsBtn.setOnAction(ev -> {
				try {
					File initialDir = new File(vars.getGlimpseLogDir());
					FileChooser.ExtensionFilter txtFilter = FileChooserPlus.createExtensionFilter("Text files (*.txt)", "txt");
					String defaultName = report.getDefaultSaveFileName();
					if (defaultName == null || defaultName.trim().isEmpty()) {
						defaultName = "error_report.txt";
					}
					File chosen = FileChooserPlus.showSaveDialog(stage, "Save Error Report", initialDir, defaultName, txtFilter);
					if (chosen != null) {
						ArrayList<String> exportRows = new ArrayList<>();
						String text = report.buildText(viewSelector.getValue());
						if (text != null && !text.isEmpty()) {
							Collections.addAll(exportRows, text.split("\\R", -1));
						}
						files.saveFile(exportRows, chosen.getPath());
						utils.showInformationDialog("Information", "Export successful", "Saved report to: " + chosen.getPath());
					}
				} catch (Exception ex) {
					utils.showInformationDialog("Information", "Export failed", "Could not save report: " + ex.getMessage());
				}
			});

			Button closeButton = utils.createButton(GLIMPSEUtils.LABEL_CLOSE, styles.getBigButtonWidth(), null);
			closeButton.setOnAction(e -> stage.close());

			HBox controls = new HBox(10, new Label("View:"), viewSelector, saveAsBtn);
			controls.setPadding(new Insets(6, 10, 6, 10));
			controls.setAlignment(Pos.CENTER_LEFT);

			HBox buttonBox = new HBox();
			buttonBox.setPadding(new Insets(4, 4, 4, 4));
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			buttonBox.getChildren().addAll(closeButton);

			border.setTop(controls);
			border.setCenter(textArea);
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

	private ScenarioLibraryReportHelper.ErrorTextReport buildTextReportFromCsv(String title, ArrayList<String> csvData) {
		ArrayList<ScenarioLibraryReportHelper.ErrorReportLine> reportLines = new ArrayList<>();
		if (csvData != null && !csvData.isEmpty()) {
			String[][] rawData = utils.getDataMatrixFromArrayList(csvData);
			String[] headerRow = rawData.length > 0 ? rawData[0] : new String[0];
			int classificationIndex = findColumnIndex(headerRow, "Classification");
			for (int rowIndex = 1; rowIndex < rawData.length; rowIndex++) {
				String[] row = rawData[rowIndex];
				if (row == null) {
					continue;
				}
				String classification = classificationIndex >= 0 && classificationIndex < row.length ? row[classificationIndex] : "";
				StringBuilder lineText = new StringBuilder();
				for (int col = 0; col < row.length; col++) {
					if (classificationIndex == col) {
						continue;
					}
					String value = row[col] == null ? "" : row[col].trim();
					if (lineText.length() > 0) {
						lineText.append('\t');
					}
					lineText.append(value);
				}
				reportLines.add(new ScenarioLibraryReportHelper.ErrorReportLine(classification, lineText.toString()));
			}
		}
		return new ScenarioLibraryReportHelper.ErrorTextReport(
				title,
				defaultErrorFilters(),
				reportLines,
				"error_report.txt");
	}

	private ArrayList<String> defaultErrorFilters() {
		ArrayList<String> filters = new ArrayList<>();
		filters.add("All lines");
		filters.add("Major errors");
		filters.add("Moderate errors");
		filters.add("Minor errors");
		return filters;
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

	private void initializeStageOwnerAndCentering(Stage stage) {
		if (stage == null) {
			return;
		}
		try {
			Window owner = UtilsDialogs.getPrimaryOwnerWindow();
			if (owner != null) {
				stage.initOwner(owner);
			}
		} catch (Exception ignored) {
		}
		stage.setOnShown(e -> centerStageOverPrimaryOwner(stage));
	}

	private void centerStageOverPrimaryOwner(Stage stage) {
		if (stage == null) {
			return;
		}
		try {
			Window owner = UtilsDialogs.getPrimaryOwnerWindow();
			if (owner == null || !owner.isShowing()) {
				return;
			}
			double stageWidth = stage.getWidth();
			double stageHeight = stage.getHeight();
			if (stageWidth <= 0 || stageHeight <= 0) {
				return;
			}
			stage.setX(owner.getX() + ((owner.getWidth() - stageWidth) / 2.0));
			stage.setY(owner.getY() + ((owner.getHeight() - stageHeight) / 2.0));
		} catch (Exception ignored) {
		}
	}

	/**
	 * Generates CSV-style error-report rows from a GCAM main log.
	 *
	 * @param mainLogFile path to the main log file
	 * @param scenario scenario name to include in the report rows
	 * @return report rows without a header line
	 */
	public ArrayList<String> generateErrorReport(String mainLogFile, String scenario) {
		if (files == null || vars == null)
			return new ArrayList<>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		formatter.setGroupingUsed(false);
		double minDmd = 0.0001;
		double minRed = 0.01;
		int totalFails = 0;
		int minorFails = 0;
		int smallMarketFails = 0;
		int majorFails = 0;
		int moderateFails = 0;
		String scenarioLabel = (scenario == null || scenario.trim().isEmpty()) ? "exe/main_log.txt" : scenario;
		ArrayList<String> report = new ArrayList<>();
		ArrayList<String[]> tokenRows = new ArrayList<>();
		ArrayList<String> classifications = new ArrayList<>();
		ArrayList<String> smallMarkets = new ArrayList<>();
		int maxTokenCount = 0;
		File mainlogfile = new File(mainLogFile);
		if (mainlogfile.exists()) {
			String[] prefixes = { "ERROR", "SEVERE", "Period" };
			ArrayList<String> errorLines = files.getStringArrayWithPrefix(mainlogfile.getPath(), prefixes);
			for (String errorLine : errorLines) {
				if (errorLine == null)
					continue;
				String normalized = errorLine.replace(":", ",");
				String[] tokens = normalized.split(",");
				maxTokenCount = Math.max(maxTokenCount, tokens.length);
				String classification = "";
				String smallMarket = "";
				try {
					if (tokens.length > 12) {
						double red = Double.parseDouble(tokens[7].trim());
						double dmd = Double.parseDouble(tokens[9].trim());
						tokens[12].trim();
						totalFails++;
						if (red > minRed) {
							if (red > minRed * 5.0) {
								classification = "MAJOR";
								majorFails++;
							} else {
								classification = "MODERATE";
								moderateFails++;
								if (dmd <= minDmd)
									smallMarketFails++;
							}
						} else {
							classification = "MINOR";
							minorFails++;
							if (dmd <= minDmd)
								smallMarketFails++;
						}
						smallMarket = (dmd <= minDmd) ? "true" : "false";
					}
				} catch (Exception e) {
					// Ignore parse errors for robustness
				}
				tokenRows.add(tokens);
				classifications.add(classification);
				smallMarkets.add(smallMarket);
			}
			if (tokenRows.isEmpty()) {
				maxTokenCount = Math.max(maxTokenCount, 1);
				tokenRows.add(new String[] { "No errors reported" });
				classifications.add("");
				smallMarkets.add("");
			}
			if (totalFails > 0) {
				maxTokenCount = Math.max(maxTokenCount, 1);
				String verdict;
				if (totalFails == 0) {
					verdict = "Verdict: Pass (no errors)";
				} else if (totalFails == minorFails) {
					verdict = "Verdict: Pass? (all errors are minor)";
				} else if (totalFails == minorFails + moderateFails) {
					verdict = "Verdict: Pass? (all errors are minor or moderate)";
				} else if (totalFails == smallMarketFails) {
					verdict = "Verdict: Pass? (all fails are in small markets)";
				} else if (totalFails == minorFails + smallMarketFails) {
					verdict = "Verdict: Pass? (all fails are minor or in small markets)";
				} else {
					verdict = "Verdict: Fail? (major, non-small market failures)";
				}
				String summary = "Total errors=" + totalFails + "; Major errors=" + majorFails + "; Moderate errors="
						+ moderateFails + "; Small market errors=" + smallMarketFails + "; "
						+ verdict + " (" + formatter.format(minRed * 100.0) + "-"
						+ formatter.format(minRed * 5.0 * 100.0) + "% thresholds)";
				tokenRows.add(new String[] { "Summary", summary });
				classifications.add("");
				smallMarkets.add("");
			}
		} else {
			maxTokenCount = Math.max(maxTokenCount, 1);
			tokenRows.add(new String[] { "Main log not found" });
			classifications.add("");
			smallMarkets.add("");
		}

		int columnCount = 1 + Math.max(1, maxTokenCount) + 2;
		for (int i = 0; i < tokenRows.size(); i++) {
			ArrayList<String> fields = new ArrayList<>();
			fields.add(sanitizeCsvField(scenarioLabel));
			String[] tokens = tokenRows.get(i);
			for (int t = 0; t < Math.max(1, maxTokenCount); t++) {
				String token = (tokens != null && t < tokens.length) ? tokens[t] : "";
				fields.add(sanitizeCsvField(token));
			}
			fields.add(sanitizeCsvField(classifications.get(i)));
			fields.add(sanitizeCsvField(smallMarkets.get(i)));
			report.add(buildCsvRow(fields, columnCount));
		}
		return report;
	}

	/**
	 * Converts raw report rows into a rectangular CSV table with a header row.
	 *
	 * @param rows raw report rows generated from the log
	 * @return padded table rows suitable for popup display
	 */
	public ArrayList<String> buildErrorReportTable(ArrayList<String> rows) {
		ArrayList<String> table = new ArrayList<>();
		int maxCols = 0;
		if (rows != null) {
			for (String row : rows) {
				if (row == null)
					continue;
				int len = row.split(",", -1).length;
				if (len > maxCols)
					maxCols = len;
			}
		}
		if (maxCols < 4)
			maxCols = 4;
		int tokenCols = Math.max(1, maxCols - 3);
		ArrayList<String> header = new ArrayList<>();
		header.add("Scenario");
		for (int i = 1; i <= tokenCols; i++) {
			header.add("Field" + i);
		}
		header.add("Classification");
		header.add("SmallMarket");
		table.add(buildCsvRow(header, header.size()));

		if (rows == null || rows.isEmpty()) {
			ArrayList<String> noErr = new ArrayList<>();
			noErr.add(" ");
			noErr.add("No errors reported");
			table.add(buildCsvRow(noErr, header.size()));
			return table;
		}
		for (String row : rows) {
			String[] parts = row == null ? new String[0] : row.split(",", -1);
			ArrayList<String> fields = new ArrayList<>();
			Collections.addAll(fields, parts);
			table.add(buildCsvRow(fields, header.size()));
		}
		return table;
	}

	/**
	 * Summarizes error severities from parsed GCAM error lines.
	 *
	 * @param errors error lines to classify
	 * @param minRed minimum relative difference threshold for non-minor errors
	 * @return compact summary string, or an empty string when no errors were found
	 */
	public String processErrors(ArrayList<String> errors, double minRed) {
		if (errors == null)
			return "";
		double minDmd = 0.0001;
		int total = 0;
		int major = 0;
		int moderate = 0;
		int minor = 0;
		int smallMarkets = 0;
		for (String errorLine : errors) {
			if (errorLine == null)
				continue;
			String normalized = errorLine.replace(":", ",");
			String[] tokens = normalized.split(",");
			try {
				if (tokens.length > 12) {
					double red = Double.parseDouble(tokens[7].trim());
					double dmd = Double.parseDouble(tokens[9].trim());
					String mkt = tokens[12].trim();
					total++;
					if (dmd <= minDmd)
						smallMarkets++;
					if ((red > minRed) && (!mkt.contains("water consumption"))) {
						if (red > minRed * 5.0) {
							major++;
						} else {
							moderate++;
						}
					} else {
						minor++;
					}
				}
			} catch (Exception e) {
				// ignore parse errors
			}
		}
		if (total == 0)
			return "";
		return "total=" + total + ";major=" + major + ";moderate=" + moderate + ";minor=" + minor
				+ ";small=" + smallMarkets;
	}
}
