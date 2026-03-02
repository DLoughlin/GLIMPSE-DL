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
package glimpseBuilder;

import java.util.Date;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import glimpseElement.ComponentLibraryTable;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseElement.ComponentRow;

public class SetupTableComponentLibrary {

	private GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
	private GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
	private GLIMPSEFiles files = GLIMPSEFiles.getInstance();
	private GLIMPSEStyles styles = GLIMPSEStyles.getInstance();

	public SetupTableComponentLibrary() {
		
	}
	
	public void setup() { //TableView<ComponentRow>

		ComponentLibraryTable.tableComponents = new TableView<>(ComponentLibraryTable.getListOfFiles());
		// Make columns fit the available table width (prevents a horizontal scrollbar at normal sizes).
		ComponentLibraryTable.getTableComponents().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		TableColumn<ComponentRow, String> nameCol = ComponentLibraryTable.getFileNameColumn();
		nameCol.prefWidthProperty().bind(ComponentLibraryTable.getTableComponents().widthProperty().multiply(0.65));

		// Not currently shown in table!!!!
		TableColumn<ComponentRow, String> addressCol = ComponentLibraryTable.getAddressColumn();
		addressCol.prefWidthProperty().bind(ComponentLibraryTable.getTableComponents().widthProperty().divide(2.));

		TableColumn<ComponentRow, Date> dateCol = ComponentLibraryTable.getBirthDateColumn();
		dateCol.prefWidthProperty().bind(ComponentLibraryTable.getTableComponents().widthProperty().multiply(0.35));
		ComponentLibraryTable.getTableComponents().getColumns().addAll(nameCol, /*addressCol,*/ dateCol);

		ComponentLibraryTable.getTableComponents().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		showComponentDetailsOnDoubleClick();//ComponentTable.tableComponents);

		addFiltering();//ComponentTable.tableComponents);
	}

	private void showComponentDetailsOnDoubleClick() {//TableView<ComponentRow> tableComponents) {

		ComponentLibraryTable.getTableComponents().setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
					// If the table is double clicked
					ComponentRow mf1 = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItem();
					String filename = mf1.getAddress();
					
					files.showFileInTextEditor(filename);

				}
				if (event.isPrimaryButtonDown()) {
					ComponentRow mf1 = ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedItem();
					
					if (ComponentLibraryTable.getTableComponents().getSelectionModel().getSelectedCells().size()==1) {
						String componentList=mf1.getAddress().replace(";",vars.getEol());
						ComponentLibraryTable.getTableComponents().setTooltip(new Tooltip(componentList));	
					} else {
						ComponentLibraryTable.getTableComponents().setTooltip(null);
					}
				}
			}
		});

	}

	private void addFiltering() {//TableView<ComponentRow> table) {

		// Filter TextField is now created by the UI pane (PaneComponentLibrary) so it can be
		// placed where we want in the layout.
		if (ComponentLibraryTable.getFilterComponentsTextField() == null) {
			throw new IllegalStateException("Filter TextField not initialized. Create it in PaneComponentLibrary and call ComponentLibraryTable.setFilterComponentsTextField(...) before SetupTableComponentLibrary.setup().");
		}

		ComponentLibraryTable.getFilterComponentsTextField().setMinWidth(styles.getBigButtonWidth());

		FilteredList<ComponentRow> filteredComponents = new FilteredList<>(ComponentLibraryTable.getTableComponents().getItems(), p -> true);

		ComponentLibraryTable.getFilterComponentsTextField().textProperty().addListener((observable, oldValue, newValue) -> {
			filteredComponents.setPredicate(myfile1 -> {
				// If user hasn't typed anything into the search bar
				if (newValue == null || newValue.isEmpty()) {
					// Display all items
					return true;
				}

				// Compare items with filter text (case-insensitive)
				String lowerCaseFilter = newValue.toLowerCase();

				return myfile1.getFileName() != null && myfile1.getFileName().toLowerCase().contains(lowerCaseFilter);
			});
		});

		// Adds the ability to sort the list after being filtered
		SortedList<ComponentRow> sortedComponents = new SortedList<>(filteredComponents);
		sortedComponents.comparatorProperty().bind(ComponentLibraryTable.getTableComponents().comparatorProperty());
		ComponentLibraryTable.tableComponents.setItems(sortedComponents);
	}

}