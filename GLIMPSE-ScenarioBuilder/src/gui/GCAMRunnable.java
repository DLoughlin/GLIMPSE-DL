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
 */
package gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * RunnableCmd is a utility class that implements Runnable to execute system commands
 * in a separate thread. It supports both single command strings and command arrays,
 * and can optionally execute commands in a specified working directory. The class
 * captures and prints the standard output of the executed process to the console.
 * <p>
 * Usage scenarios include launching external applications, scripts, or batch files
 * from within the GLIMPSE Scenario Builder GUI, while keeping the main application
 * responsive. This class is not intended for capturing process output for further
 * programmatic use, but rather for simple command execution and output logging.
 * <p>
 * Example usage:
 * <pre>
 *   RunnableCmd rc = new RunnableCmd();
 *   rc.setCmd("myScript.bat", "C:/my/dir");
 *   new Thread(rc).start();
 * </pre>
 *
 */
class RunnableCmd implements Runnable {

    String cmd = null;
    String[] cmdArray = null;
    File dir = null;
    private Map<String, String> envVars = new HashMap<>();

    /**
     * Sets the command to execute as a single string.
     *
     * @param commandText the command to execute (e.g., "myScript.bat")
     */
    public void setCmd(String commandText) {
        cmd = commandText;
    }
    
    /**
     * Sets the command to execute as an array of strings.
     *
     * @param commandText the command and its arguments (e.g., {"cmd", "/c", "echo Hello"})
     */
    public void setCmd(String[] commandText) {
        cmdArray = commandText;
    }

    /**
     * Sets the command and working directory to execute as a single string.
     *
     * @param commandText the command to execute
     * @param directory the working directory path
     */
    public void setCmd(String commandText, String directory) {
        cmd = commandText;
        dir = new File(directory);
        if (!dir.isDirectory())
            System.out.println("specified directory " + dir + " does not exist.");
    }

    /**
     * Sets the command and working directory to execute as an array of strings.
     *
     * @param commandText the command and its arguments
     * @param directory the working directory path
     */
    public void setCmd(String[] commandText, String directory) {
        cmdArray = commandText;
        dir = new File(directory);
        if (!dir.isDirectory())
            System.out.println("specified directory " + dir + " does not exist.");
    }

    /**
     * Allows setting a custom environment variable for the process.
     * @param key the environment variable name
     * @param value the environment variable value
     */
    public void setEnvVar(String key, String value) {
        envVars.put(key, value);
    }

    /**
     * Executes the configured command in a separate thread. Captures and prints
     * the standard output of the process to the console. If a working directory
     * is specified, the command is executed in that directory. Handles both single
     * string and array command formats. Waits for the process to complete before
     * returning.
     */
    @Override
    public void run() {
        // System.out.println("is dir?" + dir.isDirectory());
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();

        try {
            java.lang.Process p = null;
            // Merge system environment with custom envVars
            Map<String, String> mergedEnv = new HashMap<>(System.getenv());
            mergedEnv.putAll(envVars);
            String[] envArray = mergedEnv.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
            // Determine which command and directory configuration to use
            if (dir == null) {
                // No working directory specified, execute single string command
                if (cmdArray != null) {
                    p = rt.exec(cmdArray, envArray); 
                } else {
                    p = rt.exec(cmd, envArray);
                }
            } else if (cmd == null) {
                // Command array with working directory
                p = rt.exec(cmdArray, envArray, dir);
            } else {
                // Single string command with working directory
                p = rt.exec(cmd, envArray, dir);
            }

            // Read and print the standard output of the process
            String line;
            InputStream stdout = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            while ((line = reader.readLine()) != null) {
                System.out.println("Stdout: " + line);
            }
            // Wait for the process to finish
            p.waitFor();
            // Clean up resources
            p.destroy();
            stdout.close();
            reader.close();
        } catch (Exception e) {
            System.out.println("problem starting \"" + cmd + "\".");
            System.out.println("Error: " + e);
        }
    }

}