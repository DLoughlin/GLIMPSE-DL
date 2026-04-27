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
 * and that User is not otherwise prohibited
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

import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.event.Event;

public class DragSelectionCell extends TextFieldTableCell<DataPoint, String> {

	private TextField textField;

	/**
	 * Constructs a new DragSelectionCell, attaching mouse listeners
	 * to enable drag-selection behavior within a TableView.
	 */
	public DragSelectionCell() {
		// Use a lambda expression for the drag-detected event handler.
		setOnDragDetected(event -> {
			startFullDrag();
			getTableView().getSelectionModel().select(getIndex(), getTableColumn());
			event.consume(); // Consume the event to prevent other handlers from running
		});

		// Use a lambda expression for the mouse-drag-entered event handler.
		setOnMouseDragEntered(event -> {
			getTableView().getSelectionModel().select(getIndex(), getTableColumn());
			event.consume();
		});
	}

	@Override
	public void startEdit() {
		if (isEmpty()) {
			return;
		}
		super.startEdit();
		createTextFieldIfNeeded();
		textField.setText(getString());
		setText(null);
		setGraphic(textField);
		textField.selectAll();
		textField.requestFocus();
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();
		// Revert the cell to its non-editing state.
		setText(getString());
		setGraphic(null);
	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		if (empty || item == null) {
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
				setText(getString());
				setGraphic(null);
			}
		}
	}

	/**
	 * Returns the string representation of the item, or an empty string if the item is null.
	 *
	 * @return The string value of the cell's item.
	 */
	private String getString() {
		return getItem() == null ? "" : getItem();
	}

	private void createTextFieldIfNeeded() {
		if (textField != null) {
			return;
		}
		textField = new TextField(getString());
		textField.setOnAction(event -> commitFromEditor());
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
				event.consume();
			}
		});
		// Commit on focus loss so edits are retained when clicking other cells/controls.
		textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
			if (!isFocused) {
				commitFromEditor();
			}
		});
	}

	private void commitFromEditor() {
		if (textField == null) {
			return;
		}
		commitEdit(textField.getText());
	}

	@Override
	public void commitEdit(String newValue) {
		// JavaFX may end editing before focus-loss listeners run; fire commit manually in that case.
		if (!isEditing() && (newValue != null ? !newValue.equals(getItem()) : getItem() != null)) {
			TableView<DataPoint> table = getTableView();
			if (table != null) {
				TableColumn<DataPoint, String> column = getTableColumn();
				TableColumn.CellEditEvent<DataPoint, String> event =
						new TableColumn.CellEditEvent<>(
								table,
								new TablePosition<>(table, getIndex(), column),
								TableColumn.editCommitEvent(),
								newValue);
				Event.fireEvent(column, event);
			}
		}
		super.commitEdit(newValue);
		setGraphic(null);
		setText(getString());
	}
}