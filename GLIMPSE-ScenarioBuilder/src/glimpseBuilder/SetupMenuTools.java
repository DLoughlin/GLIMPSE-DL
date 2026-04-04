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

import glimpseElement.CsvToXmlWidget;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.UtilsDialogs;
import gui.Client;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Manages the setup of the "Tools" menu in the GLIMPSE application.
 */
public final class SetupMenuTools {

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    public void setup(Menu menuTools) {
        Menu menuAdvanced = new Menu("Advanced");
        
        // --- Main Tools ---
        menuTools.getItems().addAll(
            createMenuItem("Check Installation", () -> utils.displayString(vars.examineGLIMPSESetup(), "Analysis of GLIMPSE setup")),
            createMenuItem("Archive Scenario", () -> Client.buttonArchiveScenario.fire()),
            createMenuItem("Fix Lost Handle", () -> {
                utils.fixLostHandle();
                Client.buttonRefreshScenarioStatus.fire();
            }),
            new SeparatorMenuItem(),
            createMenuItem("Browse Trash", () -> files.openFileExplorer(vars.getTrashDir())),
            createMenuItem("Empty Trash", this::emptyTrashAction),
            new SeparatorMenuItem()
        );

        // --- Advanced Submenu ---
        menuAdvanced.getItems().addAll(
            createMenuItem("CSV to XML", () -> new CsvToXmlWidget().createAndShow()),
            createMenuItem("Cleanup Saved Files", this::cleanupSavedFilesAction),
            new SeparatorMenuItem(),
            createMenuItem("Stop ModelInterface Jobs", () -> {
                try {
                    if (Client.modelInterfaceExecutionThread != null) {
                        String summary = Client.modelInterfaceExecutionThread.stopAllSpawnedProcessesAndClearQueue();
                        System.out.println(summary);
                    }
                } catch (Throwable t) {
                    System.err.println("Error stopping ModelInterface jobs: " + t);
                }
            })
        );
        
        menuTools.getItems().add(menuAdvanced);
    }

    private void cleanupSavedFilesAction() {
        if (utils.confirmAction("Move saved debug and solver_log files to trash?")) {
            File[] scenarioSubFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
            if (scenarioSubFolders == null) return;

            Arrays.stream(scenarioSubFolders).forEach(folder -> {
                File[] contents = folder.listFiles(File::isFile);
                if (contents == null) return;
                
                Arrays.stream(contents)
                    .filter(file -> file.getName().contains("debug") || file.getName().contains("solver_log") || file.getName().contains("worst_market_log"))
                    .forEach(files::trash);
            });
        }
    }

    private void emptyTrashAction() {
        if (confirmDeleteTrash()) {
            System.out.println("Attempting to delete files from trash: " + vars.getTrashDir());
            Path trashPath = new File(vars.getTrashDir()).toPath();
            try {
                 Files.walk(trashPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                 System.err.println("Error while deleting trash directory: " + e.getMessage());
            }
        }
    }

    private boolean confirmDeleteTrash() {
        Alert alert = new Alert(AlertType.CONFIRMATION, "This will permanently delete all items from the trash folder.", ButtonType.OK, ButtonType.CANCEL);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Permanently delete all items from trash?");
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private MenuItem createMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}