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
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Manages the setup of the "Help" menu in the GLIMPSE application.
 */
public final class SetupMenuHelp {

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    public void setup(Menu menuHelp) {
        menuHelp.getItems().addAll(
            createWebMenuItem("GCAM Docs (web)", "http://jgcri.github.io/gcam-doc/"),
            createWebMenuItem("GCAM-USA Docs (web)", "http://jgcri.github.io/gcam-doc/gcam-usa.html"),
            new SeparatorMenuItem(),
            createWebMenuItem("GLIMPSE Information (web)", "https://epa.gov/glimpse"),
            createMenuItem("GLIMPSE Document Folder", () -> files.openFileExplorer(vars.getGlimpseDocDir())),
            new SeparatorMenuItem(),
            createMenuItem("About GLIMPSE", this::showAboutDialog),
            new SeparatorMenuItem(),
            new MenuItem(vars.getGLIMPSEVersion()) // Version display item
        );
    }

    private void showAboutDialog() {
        try {
            String filename = Paths.get(vars.getGlimpseResourceDir(), "About-text.txt").toString();
            ArrayList<String> aboutLines = files.getStringArrayFromFile(filename, "#");
            utils.showInformationDialog("About GLIMPSE", "Information about the GLIMPSE prototype", utils.createStringFromArrayList(aboutLines));
        } catch (Exception e) {
            utils.warningMessage("Problem trying to display the About information.");
            System.err.println("Error trying to display About information.");
            e.printStackTrace();
        }
    }

    private MenuItem createWebMenuItem(String title, String url) {
        return createMenuItem(title, () -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                utils.warningMessage("Problem trying to open the web page: " + url);
                System.err.println("Error trying to display web page.");
                e.printStackTrace();
            }
        });
    }

    private MenuItem createMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}
