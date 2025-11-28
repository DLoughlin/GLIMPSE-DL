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

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import gui.Client;
import java.util.ArrayList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Manages the setup of the "File" menu in the GLIMPSE application.
 */
public final class SetupMenuFile {

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    /**
     * Populates the "File" menu with its items.
     * @param menuFile The menu to populate.
     */
    public void setup(Menu menuFile) {
        menuFile.getItems().addAll(
                createMenuItem("Open Options File", () -> {
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Open Options File");
                    fileChooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("Options Files", "options*.txt")
                    );
                    String glimpseDir = vars.getGlimpseDir();
                    java.io.File initialDir = null;
                    if (glimpseDir != null && !glimpseDir.isEmpty()) {
                        initialDir = new java.io.File(glimpseDir);
                        if (!initialDir.exists() || !initialDir.isDirectory()) {
                            initialDir = new java.io.File(System.getProperty("user.dir"));
                        }
                    } else {
                        initialDir = new java.io.File(System.getProperty("user.dir"));
                    }
                    fileChooser.setInitialDirectory(initialDir);
                    java.io.File selectedFile = fileChooser.showOpenDialog(null);
                    if (selectedFile != null) {
                        vars.setOptionsFilename(selectedFile.getAbsolutePath());
                        vars.loadOptions();
                        // Clear and reload Scenario Components and Scenarios tables
                        Client.getPaneScenarioLibrary().clearAndRefreshScenarioTable();
                        Client.getPaneComponentLibrary().refreshComponentLibraryTable();
                    }
                }),
                new SeparatorMenuItem(),       		
        		createMenuItem("Show Current Options", () -> {
                ArrayList<String> optionsList = vars.getArrayListOfOptions();
                utils.displayArrayList(optionsList, "Options");
            }),
            createMenuItem("Edit Current Options File", () -> files.showFileInTextEditor(vars.getOptionsFilename())),
            createMenuItem("Reload Options File", () -> {
                vars.loadOptions();
                utils.showInformationDialog("Information", "Caution",
                    "Existing scenarios must be re-created (+) for changes in the options file to be reflected in their configuration file.");
            }),
            new SeparatorMenuItem(),
            createMenuItem("Import Scenario", () -> Client.buttonImportScenario.fire()),
            new SeparatorMenuItem(),
            createMenuItem("Exit", () -> System.exit(0))
        );
    }

    /**
     * Helper to create a MenuItem with a title and an action.
     */
    private MenuItem createMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}