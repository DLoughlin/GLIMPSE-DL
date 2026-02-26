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

import java.io.File;
import java.util.Date;

import javafx.application.Platform;

import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import glimpseElement.ScenarioTable;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseElement.ScenarioRow;

public class SetupTableScenariosLibrary {

	// initiates the singleton that holds program parameters
	GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
	GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
	GLIMPSEFiles files = GLIMPSEFiles.getInstance();
	
	public SetupTableScenariosLibrary() {
		
	}
	
	public void setup() {//TableView<ScenarioRow>

		//TableView<ScenarioRow> 
		ScenarioTable.tableScenariosLibrary = new TableView<>(ScenarioTable.listOfScenarioRuns);

		TableColumn<ScenarioRow, String> scenNameCol = ScenarioTable.getScenNameColumn();
		scenNameCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(4.));

		TableColumn<ScenarioRow, String> runComponentsCol = ScenarioTable.getComponentsColumn();
		runComponentsCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(2.));

		TableColumn<ScenarioRow, Date> createdCol = ScenarioTable.getCreatedDateColumn();
		createdCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(5.));

		TableColumn<ScenarioRow, Date> startedCol = ScenarioTable.getStartedDateColumn();
		startedCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(5.));
		
		TableColumn<ScenarioRow, Date> completedCol = ScenarioTable.getCompletedDateColumn();
		completedCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(5.));
		
		TableColumn<ScenarioRow, String> statusCol = ScenarioTable.getStatusColumn();
		statusCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(10.));

		TableColumn<ScenarioRow, String> noErrCol = ScenarioTable.getNoErrColumn();
		noErrCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(10.));
		
		TableColumn<ScenarioRow, String> unsolvedMarketsCol = ScenarioTable.getUnsolvedMarketsColumn();
		unsolvedMarketsCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(8.));
		
		TableColumn<ScenarioRow, String> runtimeCol = ScenarioTable.getRuntimeColumn();
		runtimeCol.prefWidthProperty().bind(ScenarioTable.tableScenariosLibrary.widthProperty().divide(9.));
		
		ScenarioTable.tableScenariosLibrary.getColumns().addAll(scenNameCol, createdCol, /*startedCol,*/ completedCol, statusCol,unsolvedMarketsCol,runtimeCol);
		ScenarioTable.tableScenariosLibrary.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// Show tooltip for the row currently being hovered (no selection required).
		// Tooltip text is computed once per ScenarioRow on a background thread to
		// avoid filesystem I/O on the JavaFX UI thread.
		ScenarioTable.tableScenariosLibrary.setRowFactory(tv -> {
			TableRow<ScenarioRow> row = new TableRow<>();
			row.setOnMouseEntered(e -> {
				ScenarioRow item = row.getItem();
				if (item == null) {
					row.setTooltip(null);
					return;
				}

				String cached = item.getCachedTooltipText();
				if (cached != null) {
					// Use pre-computed tooltip text (empty string means no tooltip)
					row.setTooltip(cached.isEmpty() ? null : new Tooltip(cached));
					return;
				}

				// Compute tooltip off the UI thread to avoid stalls from filesystem I/O.
				// markTooltipComputationStarted() uses CAS to ensure only one thread runs per item.
				if (!item.markTooltipComputationStarted()) {
					return;
				}
				Thread bgThread = new Thread(() -> {
					try {
						String databaseName = "";
						try {
							String configFilename = vars.getScenarioDir() + File.separator + item.getScenarioName()
									+ File.separator + "configuration_" + item.getScenarioName() + ".xml";
							File configFile = new File(configFilename);
							if (configFile.exists()) {
								String databaseLine = files.searchForTextInFileS(configFile, "xmldb-location", "#");
								databaseName = utils.getStringBetweenCharSequences(databaseLine, ">", "</");
								if (databaseName == null) databaseName = "";
								databaseName = databaseName.trim();
							}
						} catch (Exception ex) {
							databaseName = "";
						}

						String components = item.getComponents();
						String componentsFormatted = (components == null) ? "" : components.replace(";", vars.getEol()).trim();

						String eol = vars.getEol();
						String sep = "----------------------------------------";
						String tt = "";
						if (!databaseName.isEmpty()) {
							tt += "Database: " + databaseName;
						}
						if (!componentsFormatted.isEmpty()) {
							if (!tt.isEmpty()) {
								tt += eol + sep + eol;
							}
							tt += componentsFormatted;
						}

						final String tooltipText = tt;
						item.setCachedTooltipText(tooltipText);
						Platform.runLater(() -> {
							if (row.getItem() == item) {
								row.setTooltip(tooltipText.isEmpty() ? null : new Tooltip(tooltipText));
							}
						});
					} finally {
						// Only one background thread runs per item at a time (enforced by the
						// markTooltipComputationStarted() CAS above), so this check-then-set
						// is safe: no other thread can have written cachedTooltipText since
						// this thread started.
						// Ensure cachedTooltipText is set (even to "") so future hover events
						// do not attempt recomputation, and reset the flag so a retry is
						// possible if cachedTooltipText was never set due to an error.
						if (item.getCachedTooltipText() == null) {
							item.setCachedTooltipText("");
						}
						item.markTooltipComputationFinished();
					}
				});
				bgThread.setDaemon(true);
				bgThread.start();
			});
			row.setOnMouseExited(e -> row.setTooltip(null));
			return row;
		});

		ScenarioTable.tableScenariosLibrary.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {

					ScenarioRow mfr = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItem();
					String filename = vars.getScenarioDir() + File.separator+ mfr.getScenarioName()
							+ File.separator+"configuration_" + mfr.getScenarioName() + ".xml";

					files.showFileInXmlEditor(filename);
				}
				// Tooltip is handled by row hover (rowFactory). No selection-based tooltip here.
			}
		});

	}

}