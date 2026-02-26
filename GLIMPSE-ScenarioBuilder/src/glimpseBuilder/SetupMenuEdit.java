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
package glimpseBuilder;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEVariables;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/**
 * Manages the setup of the "Edit" menu in the GLIMPSE application.
 *
 * This class builds the menu items for editing various configuration files
 * used by GCAM, the Scenario Builder, and the ModelInterface.
 */
public class SetupMenuEdit {

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    /**
     * Populates the "Edit" menu with submenus and items for file editing.
     * @param menuEdit The main "Edit" menu to populate.
     */
    public void setup(Menu menuEdit) {
        Menu gcamSetupFiles = new Menu("GCAM Setup Files");
        Menu scenarioBuilderSetupFiles = new Menu("Scenario Builder Files");
        Menu modelInterfaceSetupFiles = new Menu("ModelInterface Files");

        // --- GCAM Setup Files ---
        gcamSetupFiles.getItems().addAll(
            createFileMenuItem("Solver Config File", () -> vars.getgCamSolver()),
            createFileMenuItem("Log Config File", () -> resolvePath(vars.getgCamExecutableDir(), "log_conf.xml"))
        );

        // --- Scenario Builder Files ---
        scenarioBuilderSetupFiles.getItems().addAll(
            createFileMenuItem("Scenario Template", () -> vars.getConfigurationTemplateFilename()),
            createFileMenuItem("Tech List for Bounds", () -> vars.getTchBndListFilename()),
            createFileMenuItem("Transportation Vehicle Data", () -> vars.getTrnVehInfoFilename()),
            createFileMenuItem("Region File", () -> vars.getRegionListFilename()),
            createFileMenuItem("Subregion File", () -> vars.getSubRegionsFilename()),
            createFileMenuItem("Preset Region File", () -> vars.getPresetRegionListFilename()),
            createFileMenuItem("XML Header File", () -> vars.getXmlHeaderFilename()),
            createFileMenuItem("CSV Column File", () -> vars.getCsvColumnFilename())
        );

        modelInterfaceSetupFiles.getItems().addAll(
                createFileMenuItem("Query File", () -> vars.getQueryFilename()),
                createFileMenuItem("Favorite Queries File", () -> vars.getFavoriteQueryFilename()),
                createFileMenuItem("Unit Conversions File", () -> vars.getUnitConversionsFilename()),
                createFileMenuItem("Preset Region File", () -> vars.getPresetRegionListFilename())
        );
        
        menuEdit.getItems().addAll(scenarioBuilderSetupFiles, modelInterfaceSetupFiles, gcamSetupFiles);
    }

    /**
     * Creates a MenuItem that opens a file in a text editor when clicked.
     * @param title The text to display on the menu item.
     * @param filenameSupplier A Supplier that provides the path to the file.
     * @return A configured MenuItem.
     */
    private MenuItem createFileMenuItem(String title, Supplier<String> filenameSupplier) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> files.showFileInTextEditor(filenameSupplier.get()));
        return menuItem;
    }
    
    /**
     * Safely resolves a path from a base directory and path segments.
     * Returns null if the base directory is null.
     * @param base The base directory.
     * @param parts The subsequent path components.
     * @return The resolved path as a string, or null.
     */
    private String resolvePath(String base, String... parts) {
        return Optional.ofNullable(base)
                       .map(b -> Paths.get(b, parts).toString())
                       .orElse(null);
    }
}
