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

import java.util.Date;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ScenarioTable {
	
	public static TableView<ScenarioRow> tableScenariosLibrary;
	public static FilteredList<ScenarioRow> filteredScenarios;
	public static ObservableList<ScenarioRow> listOfScenarioRuns = FXCollections.observableArrayList();
	

	public static void addToListOfRunFiles(ScenarioRow[] fileArray) {
		for (ScenarioRow rowi : fileArray) {
			if (!listOfScenarioRuns.contains(rowi)) {
				listOfScenarioRuns.add(rowi);
			}
		}
	}

	public static void removeFromListOfRunFiles(ObservableList<ScenarioRow> fileArray) {
		// System.out.println("number of files to delete..." +
		// fileArray.size());
		ObservableList<ScenarioRow> copy = FXCollections.observableArrayList();
		for (ScenarioRow i : fileArray) {
			copy.add(i);
		}
		for (ScenarioRow i : copy) {
			listOfScenarioRuns.remove(i);
		}
	}

	/* Returns First Name TableColumn */
	public static TableColumn<ScenarioRow, String> getScenNameColumn() {
		TableColumn<ScenarioRow, String> scenNameCol = new TableColumn<>("Scenario Name");
		scenNameCol.setCellValueFactory(new PropertyValueFactory<>("scenName"));
		return scenNameCol;
	}

	/* Returns Last Name TableColumn */
	public static TableColumn<ScenarioRow, String> getComponentsColumn() {
		TableColumn<ScenarioRow, String> componentsCol = new TableColumn<>("Components");
		componentsCol.setCellValueFactory(new PropertyValueFactory<>("components"));
		//componentsCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		return componentsCol;
	}

	/* Returns Birth Date TableColumn */
	public static TableColumn<ScenarioRow, String> getCreatedDateColumn() {
		TableColumn<ScenarioRow, String> createdCol = new TableColumn<>("Created");
		createdCol.setStyle("-fx-alignment: CENTER;");
		createdCol.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
		return createdCol;
	}

	/* Returns Birth Date TableColumn */
	public static TableColumn<ScenarioRow, String> getStartedDateColumn() {
		TableColumn<ScenarioRow, String> startedCol = new TableColumn<>("Started");
		startedCol.setStyle("-fx-alignment: CENTER;");
		startedCol.setCellValueFactory(new PropertyValueFactory<>("startedDate"));
		return startedCol;
	}
	
	/* Returns Birth Date TableColumn */
	public static TableColumn<ScenarioRow, String> getCompletedDateColumn() {
		TableColumn<ScenarioRow, String> completedCol = new TableColumn<>("Completed");
		completedCol.setCellValueFactory(new PropertyValueFactory<>("completedDate"));
		completedCol.setStyle("-fx-alignment: CENTER;");
		return completedCol;
	}
	
	public static TableColumn<ScenarioRow, String> getStatusColumn() {
		TableColumn<ScenarioRow, String> statusCol = new TableColumn<>("Status");
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
		statusCol.setStyle("-fx-alignment: CENTER;");
		return statusCol;
	}
	
	public static TableColumn<ScenarioRow, String> getRuntimeColumn() {
		TableColumn<ScenarioRow, String> runtimeCol = new TableColumn<>("Runtime");
		runtimeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		runtimeCol.setCellValueFactory(new PropertyValueFactory<>("runtime"));
		return runtimeCol;
	}
	
	public static TableColumn<ScenarioRow, String> getUnsolvedMarketsColumn() {
		TableColumn<ScenarioRow, String> unsolvedMarketsColumn = new TableColumn<>("ProbMkts");
		unsolvedMarketsColumn.setStyle("-fx-alignment: CENTER-LEFT;");
		unsolvedMarketsColumn.setCellValueFactory(new PropertyValueFactory<>("unsolvedMarkets"));
		return unsolvedMarketsColumn;
	}

	public static TableColumn<ScenarioRow, String> getNoErrColumn() {
		TableColumn<ScenarioRow, String> noErrCol = new TableColumn<>("#Err");
		//noErrCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		noErrCol.setCellValueFactory(new PropertyValueFactory<>("noErr"));
		return noErrCol;
	}
	
	public static void clear() {
		ObservableList<ScenarioRow> copy = FXCollections.observableArrayList();
		for (ScenarioRow i : listOfScenarioRuns) {
			copy.add(i);
		}

		for (ScenarioRow i : copy) {

			listOfScenarioRuns.remove(i);
		}
	}
	
}