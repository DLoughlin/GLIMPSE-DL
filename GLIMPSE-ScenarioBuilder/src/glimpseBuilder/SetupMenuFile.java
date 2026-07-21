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
import glimpseUtil.FileChooserPlus;
import glimpseUtil.UtilsDialogs;
import gui.Client;
import java.util.ArrayList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.application.Platform;

/**
 * Manages the setup of the "File" menu in the GLIMPSE application.
 * Provides methods to populate the menu with items for file operations
 * such as opening, editing, reloading options files, importing scenarios, and exiting.
 */
public final class SetupMenuFile {

    // Singleton instances for accessing variables, utilities, and file operations
    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    /**
     * Populates the "File" menu with its items and their associated actions.
     * @param menuFile The Menu object to populate.
     */
    public void setup(Menu menuFile) {
        menuFile.getItems().addAll(
                // Menu item to open an options file and reload configuration
                createMenuItem("Open Options File", () -> {
                    // Set initial directory for file chooser
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
                    java.io.File selectedFile = FileChooserPlus.showOpenDialog(
                        UtilsDialogs.getPrimaryOwnerWindow(),
                        "Open Options File",
                        initialDir,
                        new javafx.stage.FileChooser.ExtensionFilter("Options Files", "options*.txt")
                    );
                    if (selectedFile != null) {
                        vars.setOptionsFilename(selectedFile.getAbsolutePath());
                        reloadOptionsAndRefreshUi(true);
                    }
                }),
                new SeparatorMenuItem(),
                // Menu item to display current options in a dialog
                createMenuItem("Show Current Options", () -> {
                    ArrayList<String> optionsList = vars.getArrayListOfOptions();
                    utils.displayArrayList(optionsList, "Options");
                }),
                // Menu item to edit the current options file in a text editor
                createMenuItem("Edit Current Options File", () -> files.showFileInTextEditor(vars.getOptionsFilename())),
                // Menu item to reload the current options file
                createMenuItem("Reload Options File", () -> {
                    reloadOptionsAndRefreshUi(false);
                    utils.showInformationDialog("Information", "Caution",
                        "Existing scenarios must be re-created (+) for changes in the options file to be reflected in their configuration file.");
                }),
                new SeparatorMenuItem(),
                // Menu item to import a scenario using the import button
                createMenuItem("Import Scenario", () -> Client.buttonImportScenario.fire()),
                new SeparatorMenuItem(),
                // Menu item to exit the application
                createMenuItem("Exit", () -> Platform.exit())
        );
    }

    /**
     * Reload options and update visible UI state that depends on options.
     */
    private void reloadOptionsAndRefreshUi(boolean refreshTables) {
        final int oldEffectiveFontSize = getEffectiveFontSizeFromOptions();
        vars.loadOptions();
        final int newEffectiveFontSize = getEffectiveFontSizeFromOptions();

        // Re-apply runtime font only when the effective size changed.
        if (newEffectiveFontSize != oldEffectiveFontSize) {
            Client.applyRuntimeFontSize(newEffectiveFontSize);
        }

        if (refreshTables) {
            if (Client.getPaneScenarioLibrary() != null) {
                Client.getPaneScenarioLibrary().clearAndRefreshScenarioTable();
            }
            if (Client.getPaneComponentLibrary() != null) {
                Client.getPaneComponentLibrary().refreshComponentLibraryTable();
            }
        }
    }

    /**
     * Returns the effective runtime font size after clamping to supported bounds.
     */
    private int getEffectiveFontSizeFromOptions() {
        try {
            int parsed = Integer.parseInt(vars.getPreferredFontSize());
            return Math.max(Client.getMinRuntimeFontSize(), Math.min(Client.getMaxRuntimeFontSize(), parsed));
        } catch (Exception ignored) {
            return Client.getRuntimeFontSize();
        }
    }

    /**
     * Helper method to create a MenuItem with a title and an action.
     * @param title The display text for the menu item.
     * @param action The action to perform when the menu item is selected.
     * @return Configured MenuItem instance.
     */
    private MenuItem createMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}