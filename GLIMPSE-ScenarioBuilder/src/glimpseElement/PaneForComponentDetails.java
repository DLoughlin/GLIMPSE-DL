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
package glimpseElement;

import java.util.ArrayList;

import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.UtilsTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

/**
 * PaneForComponentDetails is a custom JavaFX VBox that provides a table interface for editing and displaying
 * pairs of data points (typically year and value) for GLIMPSE scenario components. It supports adding, editing,
 * deleting, and reordering rows, as well as copy-paste and drag selection. The table can be configured to enforce
 * year-value pairs, hide columns, and set custom column names and styles. This class is used throughout the
 * scenario builder to allow users to input and manipulate time series or paired data for scenario elements.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Editable TableView for DataPoint objects (year-value pairs)</li>
 *   <li>Customizable column names and styles</li>
 *   <li>Support for adding, deleting, and reordering rows</li>
 *   <li>Copy-paste and drag selection support</li>
 *   <li>Configurable enforcement of year-value pair input</li>
 * </ul>
 * <b>Usage:</b> Instantiate and add to a JavaFX scene. Use provided methods to manipulate table contents.
 *
 * @author Dan Loughlin
 * @version 1.0
 */
public class PaneForComponentDetails extends VBox {
    private static final int POPULATE_SIG_FIGS = 6;

    // Singleton utility and style instances
    private GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

    /**
     * Ensures the table has at least one blank row so users can click/paste into an empty table.
     */
    private void ensureAtLeastOneBlankRow() {
        if (data == null) {
            return;
        }
        if (data.isEmpty()) {
            data.add(new DataPoint("", ""));
        }
    }

    /**
     * The main TableView for displaying DataPoint objects.
     */
    public TableView<DataPoint> table = new TableView<DataPoint>();
    /**
     * The observable list backing the table's data.
     */
    public ObservableList<DataPoint> data = FXCollections.observableArrayList();
    
    // HBox containing input fields and add button
    HBox inputHBox = new HBox();
    // Input fields for year and value
    TextField textFieldYear = utils.createTextField();
    TextField textFieldValue = utils.createTextField();

    // Button to add new data point
    Button buttonAdd = utils.createButton("Add", styles.getBigButtonWidth(), null);
    // Flag to enforce year-value pair input
    boolean enforceYrValPair = true;

    // Table columns for year and value
    TableColumn<DataPoint,String> colYear;
    TableColumn<DataPoint,String> colValue;

    /**
     * Constructs a PaneForComponentDetails with default settings and UI components.
     * Sets up the table, columns, input fields, and event handlers.
     */
    public PaneForComponentDetails() {
        this.setStyle(styles.getFontStyle());

        // Initialize columns
        colYear = new TableColumn<DataPoint, String>("Year");
        colValue = new TableColumn<DataPoint, String>("Value");

        table.getColumns().addAll(colYear, colValue);
        table.setEditable(true);

        // Set up cell value factories and cell factories for editing
        colYear.setCellValueFactory(new PropertyValueFactory<DataPoint, String>("year"));
        colYear.setCellFactory(col -> new glimpseElement.DragSelectionCell());
        colYear.prefWidthProperty().bind(table.widthProperty().divide(8. / 3.)); 
        colYear.setStyle(styles.getStyle5());
        colYear.setEditable(true);

        colValue.setCellValueFactory(new PropertyValueFactory<DataPoint, String>("value"));
        colValue.setCellFactory(col -> new glimpseElement.DragSelectionCell());
        colValue.prefWidthProperty().bind(table.widthProperty().divide(8. / 5.));
        colValue.setStyle(styles.getStyle5());
        colValue.setEditable(true);

        // Commit handlers for editing cells
        colYear.setOnEditCommit(new EventHandler<CellEditEvent<DataPoint, String>>() {
            @Override
            public void handle(CellEditEvent<DataPoint, String> t) {
                // Update year value in DataPoint
                t.getTableView().getItems().get(t.getTablePosition().getRow()).setYear(t.getNewValue());
            }
        });
        colValue.setOnEditCommit(new EventHandler<CellEditEvent<DataPoint, String>>() {
            @Override
            public void handle(CellEditEvent<DataPoint, String> t) {
                // Update value in DataPoint
                t.getTableView().getItems().get(t.getTablePosition().getRow()).setValue(t.getNewValue());
            }
        });

        table.setItems(data);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        UtilsTable.installCopyPasteHandler(table);

        // Bind input field widths to table width
        textFieldYear.prefWidthProperty().bind(table.widthProperty().divide(8. / 2.75));
        textFieldValue.prefWidthProperty().bind(table.widthProperty().divide(8. / 3.75));
        textFieldYear.setPromptText("Year");
        textFieldValue.setPromptText("Value");
        
        inputHBox.getChildren().addAll(textFieldYear, textFieldValue, buttonAdd);
        inputHBox.setSpacing(3.);
        // Use centralized small top padding
        inputHBox.setPadding(styles.getSmallTopPadding());

        // Set initial Add button state based on current field content
        buttonAdd.setDisable(textFieldYear.getText() == null || textFieldYear.getText().trim().isEmpty()
                || textFieldValue.getText() == null || textFieldValue.getText().trim().isEmpty());

        // Enable Add only when both text fields have non-empty trimmed text
        textFieldYear.textProperty().addListener((obs, oldV, newV) -> {
            boolean disable = textFieldYear.getText() == null || textFieldYear.getText().trim().isEmpty()
                    || textFieldValue.getText() == null || textFieldValue.getText().trim().isEmpty();
            buttonAdd.setDisable(disable);
        });
        textFieldValue.textProperty().addListener((obs, oldV, newV) -> {
            boolean disable = textFieldYear.getText() == null || textFieldYear.getText().trim().isEmpty()
                    || textFieldValue.getText() == null || textFieldValue.getText().trim().isEmpty();
            buttonAdd.setDisable(disable);
        });

        // Allow pressing Enter in either field to trigger Add when enabled
        textFieldYear.setOnAction(e -> {
            if (!buttonAdd.isDisabled())
                buttonAdd.fire();
        });
        textFieldValue.setOnAction(e -> {
            if (!buttonAdd.isDisabled())
                buttonAdd.fire();
        });

        // Add button action: add new DataPoint if valid
        buttonAdd.setOnAction(e -> {
            // If either input field is empty do nothing
            if (textFieldYear.getText() == null || textFieldYear.getText().trim().isEmpty()
                    || textFieldValue.getText() == null || textFieldValue.getText().trim().isEmpty()) {
                return;
            }
            // Trim inputs before creating the DataPoint
            String year = textFieldYear.getText().trim();
            String value = textFieldValue.getText().trim();
            DataPoint dp = new DataPoint(year, value);
            if (dp.qaDataPoint(enforceYrValPair)){
                addOrReplacePlaceholderRow(year, value);
                // clear fields and disable Add until new input is provided
                textFieldYear.clear();
                textFieldValue.clear();
                buttonAdd.setDisable(true);
                // move focus back to the Year field
                textFieldYear.requestFocus();
            }
        });

        this.getChildren().addAll(table, inputHBox);

        // Make table grow to fill remaining vertical space inside this VBox
        this.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMaxHeight(Double.MAX_VALUE);
        // Also allow the table to use unlimited preferred height and use constrained column resizing
        table.setPrefHeight(Double.MAX_VALUE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(0);
        this.setMaxHeight(Double.MAX_VALUE);
        this.setMinHeight(0);

        // When this PaneForComponentDetails is added to a parent VBox, allow it to grow vertically
        VBox.setVgrow(this, Priority.ALWAYS);

        // Bind the table's prefHeight to this VBox height minus the inputHBox height so it expands to fill
        table.prefHeightProperty().bind(this.heightProperty().subtract(inputHBox.heightProperty()).subtract(4));

        // Seed with one blank row so pasting works even when the table starts empty.
        ensureAtLeastOneBlankRow();

        // Runtime layout listener to help debugging actual heights while resizing
        // this.heightProperty().addListener((obs, oldH, newH) -> {
        //     System.out.println("[PaneForComponentDetails] VBox height=" + newH +
        //             " table.height=" + table.getHeight() +
        //             " inputHBox.height=" + inputHBox.getHeight());
        // });
        // table.heightProperty().addListener((obs, oldH, newH) -> {
        //     System.out.println("[PaneForComponentDetails] table prefHeight=" + table.getPrefHeight() +
        //             " actual=" + newH);
        // });
        // echoData("end of constructor");
    }

    /**
     * Adds a new DataPoint with only a value (year is null).
     * @param name1 Value for the DataPoint
     */
    public void addItem(String name1) {
        DataPoint dp = new DataPoint(null, name1);
        data.add(dp);
    }

    /**
     * Adds a table row while replacing the seeded blank placeholder when present.
     */
    public void addTableRow(String year, String value) {
        addOrReplacePlaceholderRow(year, value);
    }

    private boolean isBlankText(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Returns the first fully blank row index, or -1 when no blank row exists.
     */
    private int findFirstBlankRowIndex() {
        if (data == null || data.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < data.size(); i++) {
            DataPoint row = data.get(i);
            if (row == null) {
                return i;
            }
            if (isBlankText(row.getYear()) && isBlankText(row.getValue())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds a row of data, reusing the first blank row when available.
     */
    private void addOrReplacePlaceholderRow(String year, String value) {
        DataPoint dp = new DataPoint(year, value);
        int blankRowIndex = findFirstBlankRowIndex();
        if (blankRowIndex >= 0) {
            data.set(blankRowIndex, dp);
        } else {
            data.add(dp);
        }
    }

    /**
     * Adds a new DataPoint with year and value.
     * @param name0 Year for the DataPoint
     * @param name1 Value for the DataPoint
     */
    public void addItem(String name0, String name1) {
        addOrReplacePlaceholderRow(name0, name1);
    }

    /**
     * Sets the style for each column.
     * @param s0 Style string for year column
     * @param s1 Style string for value column
     */
    public void setColumnFormatting(String s0, String s1) {
        colYear.setStyle(s0);
        colValue.setStyle(s1);
    }

    /**
     * Sets the column names and optionally hides columns if one is null.
     * @param name0 Name for year column (null to hide)
     * @param name1 Name for value column (null to hide)
     */
    public void setColumnNames(String name0, String name1) {
        if (name0 != null) {
            colYear.setText(name0);
            if (name1 == null) {
                colYear.prefWidthProperty().bind(table.widthProperty().divide(1.));
                hideValColumn();
            }
        }
        if (name1 != null) {
            colValue.setText(name1);
            if (name0 == null) {
                colValue.prefWidthProperty().bind(table.widthProperty().divide(1.));
                hideYrColumn();
            }
        }
    }

    /**
     * Hides the year column.
     */
    public void hideYrColumn() {
        colYear.setVisible(false);
    }

    /**
     * Hides the value column.
     */
    public void hideValColumn() {
        colValue.setVisible(false);
    }

    /**
     * Sets the visibility of the add item input fields and button.
     * @param b true to show, false to hide
     */
    public void setAddItemVisible(boolean b) {
        inputHBox.setVisible(b);
    }

    /**
     * Sets whether to enforce year-value pair input validation.
     * @param b true to enforce, false to allow any input
     */
    public void setEnforceYrValPair(boolean b) {
        enforceYrValPair = b;
    }

    /**
     * Creates a new DataPoint with the given year and value.
     * @param year Year string
     * @param value Value string
     * @return New DataPoint instance
     */
    public DataPoint createDataPoint(String year, String value) {
        DataPoint dp = new DataPoint(year, value);
        return dp;
    }

    /**
     * Checks if the table is empty.
     * @return true if table has no items, false otherwise
     */
    public boolean isEmpty() {
        boolean empty = true;
        if (table.getItems().size() > 0)
            empty = false;
        return empty;
    }

    /**
     * Deletes selected items from the table after user confirmation.
     */
    public void deleteItemsFromTable() {

        if (!utils.confirmDelete())
            return;

        // Snapshot selection first; getSelectedItems() is a live view that changes as rows are removed.
        ArrayList<DataPoint> selectedDataPoints = new ArrayList<>(table.getSelectionModel().getSelectedItems());
        data.removeAll(selectedDataPoints);
    }

    /**
     * Moves the selected item up in the table, if possible.
     */
    public void moveItemUpInTable() {
        ObservableList<DataPoint> allItems = table.getItems();

        ObservableList<DataPoint> selectedItems = table.getSelectionModel().getSelectedItems();
        if (selectedItems.size() == 1) {
            int n = table.getSelectionModel().getSelectedIndex();
            if (n - 1 >= 0) {
                DataPoint dataA = allItems.get(n);
                DataPoint dataB = allItems.get(n - 1);
                allItems.set(n - 1, dataA);
                allItems.set(n, dataB);
                table.setItems(allItems);
            }
        }
    }

    /**
     * Moves the selected item down in the table, if possible.
     */
    public void moveItemDownInTable() {
        ObservableList<DataPoint> allItems = table.getItems();

        ObservableList<DataPoint> selectedItems = table.getSelectionModel().getSelectedItems();
        if (selectedItems.size() == 1) {
            int n = table.getSelectionModel().getSelectedIndex();
            if (n < allItems.size() - 1) {
                DataPoint dataA = allItems.get(n);
                DataPoint dataB = allItems.get(n + 1);
                allItems.set(n + 1, dataA);
                allItems.set(n, dataB);
                table.setItems(allItems);
            }
        }
    }

    /**
     * Returns a string representation of the table data for debugging or export.
     * @return String with each item as "Item i = year , value"
     */
    public String dataOutput() {
        String str_data = "";

        ObservableList<DataPoint> tableData = table.getItems();

        for (int i = 0; i < tableData.size(); i++) {
            str_data += "Item " + i + " = " + tableData.get(i).getYear() + " , " + tableData.get(i).getValue()
                    + vars.getEol();
        }

        return str_data;
    }
    

    /**
     * Returns the table data as an ArrayList of strings, each formatted as "year , value".
     * @return ArrayList of year-value strings
     */
    public ArrayList<String> getDataYrValsArrayList() {
        String str_data = "";

        ArrayList<String> data = new ArrayList<String>();

        ObservableList<DataPoint> tableData = table.getItems();

        for (int i = 0; i < tableData.size(); i++) {
            String year = tableData.get(i).getYear();
            String value = tableData.get(i).getValue();

            year = (year == null) ? "" : year.trim();
            value = (value == null) ? "" : value.trim();

            // Skip completely blank rows (e.g., the seeded starter row, or trailing blanks from paste).
            if (year.isEmpty() && value.isEmpty()) {
                continue;
            }

            str_data = year + " , " + value;
            data.add(str_data);
        }

        return data;
    }

    /**
     * Returns all values in the value column as an ArrayList.
     * @return ArrayList of value strings
     */
    public ArrayList<String> getValues() {
        ArrayList<String> column = new ArrayList<String>();
        ObservableList<DataPoint> tableData = table.getItems();

        for (int i = 0; i < tableData.size(); i++) {
            String s = tableData.get(i).getValue().trim();

            if (s != null)
                column.add(s);

        }

        return column;
    }

    /**
     * Clears all data from the table.
     */
    public void clearTable() {
        data.clear();
        // Keep one blank row for paste/click usability.
        ensureAtLeastOneBlankRow();
    }

    /**
     * Updates the table view to reflect the current data list.
     */
    public void updateTable() {
        table.setItems(data);

        //echoData("end of updateTable");
    }

    /**
     * Prints the current data to the console for debugging.
     * @param str Message to print before data
     */
    public void echoData(String str) {
        System.out.println(str);
        for (int i = 0; i < data.size(); i++) {
            System.out.println(" i: " + i + " " + data.get(i).getYear() + " " + data.get(i).getValue());
        }
    }

    /**
     * Sets the table data from a 2D array of doubles (first row: years, second row: values).
     * @param values 2D array [2][n] with years and values
     */
    public void setValues(double[][] values) {
        data.clear();

        for (int i = 0; i < values[0].length; i++) {
            int yr = (int) values[0][i];
            double val = values[1][i];
            addOrReplacePlaceholderRow(String.valueOf(yr), formatValueForDisplay(val));
        }
        updateTable();
    }

    /**
     * Formats a numeric value for table display using standard notation unless
     * the magnitude is smaller than 1e-6.
     */
    private String formatValueForDisplay(double value) {
        return utils.toSignificantFiguresString(value, POPULATE_SIG_FIGS);
    }

    /**
     * Format only the rendered text for the Value column; this does not mutate
     * the underlying DataPoint value string.
     */
    private String formatValueCellText(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            double parsed = Double.parseDouble(trimmed);
            return utils.toSignificantFiguresString(parsed, vars.getValueDisplaySigFigs());
        } catch (NumberFormatException nfe) {
            return trimmed;
        }
    }

    /**
     * Sets the table data from an array list of strings in which first row: years, second row: values.
     * @param values ArrayList of strings formatted as "year,value"
     */
	public void setValues(ArrayList<String> values) {
		data.clear();
		for (int i = 0; i < values.size(); i++) {
			String[] parts = values.get(i).split(",");
			if (parts.length == 2) {
				String col0 = parts[0].trim();
				String col1 = parts[1].trim();
				addOrReplacePlaceholderRow(col0, col1);
			}
		}
		updateTable();
	}
    
    /**
     * Sets the table data from a 2D array of strings (first row: years, second row: values).
     * @param values 2D array [2][n] with years and values as strings
     */
    public void setValues(String[][] values) {
        data.clear();
        for (int i = 0; i < values[0].length; i++) {
            String col0 = values[0][i];
            String col1 = values[1][i];
            addOrReplacePlaceholderRow(col0, col1);
        }
        updateTable();
    }

    /**
     * Populates the table from a comma-separated string.
     * @param csv The comma-separated string with year and value.
     */
    public void populateTableFromCSV(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return;
        }
        String[] parts = utils.splitString(csv, ",");
        if (parts.length == 2) {
            String year = parts[0].trim();
            String value = parts[1].trim();
            addOrReplacePlaceholderRow(year, value);
        }
    }

    /**
     * Cell factory for enabling drag selection in the table.
     */
    public class DragSelectionCellFactory implements
            Callback<TableColumn<DataPoint, String>, TableCell<DataPoint, String>> {

        @Override
        public TableCell<DataPoint, String> call(final TableColumn<DataPoint, String> col) {
            return new DragSelectionCell();
        }

    }

    /**
     * Custom TableCell that supports drag selection and editing for DataPoint table cells.
     */
    public class DragSelectionCell extends TextFieldTableCell<DataPoint, String> {

        private TextField textField;

        public DragSelectionCell() {
            // Start full drag and select cell on drag detected
            setOnDragDetected(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    startFullDrag();
                    getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn());
                }
            });
            // Select cell on mouse drag enter
            setOnMouseDragEntered(new EventHandler<MouseDragEvent>() {

                @Override
                public void handle(MouseDragEvent event) {
                    getTableColumn().getTableView().getSelectionModel().select(getIndex(), getTableColumn());
                }

            });

        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                // Create a new text field for editing
                TextField textField=utils.createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    String displayText = getString();
                    if (getTableColumn() == colValue) {
                        displayText = formatValueCellText(displayText);
                    }
                    setText(displayText);
                    setGraphic(null);
                }
            }
        }

        /**
         * Returns the string value of the cell item, or empty string if null.
         * @return String value of cell
         */
        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }


		
}