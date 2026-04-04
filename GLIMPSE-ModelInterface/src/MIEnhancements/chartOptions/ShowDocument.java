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
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/
package chartOptions;

import java.lang.reflect.Method;
import javax.swing.JOptionPane;

import ModelInterface.InterfaceMain;

/**
 * Utility class for displaying a document (URL) in the system's default web browser.
 * <p>
 * Handles Mac OS, Windows, and Linux/Unix systems.
 * </p>
 *
 * <p>
 * Author: TWU
 * Created: 1/2/2016
 * </p>
 */
public class ShowDocument {
    /** Error message shown if browser launch fails. */
    private static final String ERR_MSG = "Error attempting to launch web browser";
    /** Debug flag for logging. */
    private static final boolean DEBUG = false;

    /**
     * Default constructor.
     */
    public ShowDocument() {
        // No initialization required
    }

    /**
     * Opens the specified URL in the system's default web browser.
     *
     * @param url the URL to open
     */
    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        if (DEBUG) {
            System.out.println("ShowDocument:url: " + url);
        }
        try {
            if (osName.startsWith("Mac OS")) {
                // Mac OS: use reflection to call FileManager.openURL
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", String.class);
                openURL.invoke(null, url);
            } else if (osName.startsWith("Windows")) {
                // Windows: use rundll32 to open the URL
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else {
                // Linux/Unix: try common browsers
                String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (String b : browsers) {
                    // Check if browser exists using 'which' command
                    if (Runtime.getRuntime().exec(new String[] { "which", b }).waitFor() == 0) {
                        browser = b;
                        break;
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                }
                Runtime.getRuntime().exec(new String[] { browser, url });
            }
        } catch (Exception e) {
            // Show error dialog if browser launch fails
              JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), ERR_MSG);
        }
    }
}