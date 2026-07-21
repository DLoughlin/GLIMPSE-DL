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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import conversionUtil.ArrayConversion;

/**
 * Utility class for file operations, including file selection dialogs,
 * reading, writing, and parsing files.
 *
 * Author: TWU
 * Created: 1/2/2016
 */
public class FileUtil {

    private static File getDefaultChooserFile() {
        File homeDir = FileSystemView.getFileSystemView().getHomeDirectory();
        return homeDir != null ? homeDir : new File(System.getProperty("user.home", "."));
    }

    private static File[] promptForFiles(String title, int mode, File setFile, FileFilter filter) {
        FileChooser chooser = FileChooserFactory.getFileChooser();
        File start = setFile != null ? setFile : getDefaultChooserFile();
        return chooser.doFilePrompt(null, title, mode, start, filter);
    }

    /**
     * Opens a save file dialog and returns the selected file path.
     * @param desc Description for the file filter
     * @param extension File extension for the filter
     * @return Absolute path of the selected file or null if cancelled
     */
    public static String getSaveFilePathFromChooser(String desc, String extension) {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, extension);
        File[] selected = promptForFiles("Select a file location", FileChooser.SAVE_DIALOG,
                getDefaultChooserFile(), filter);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Opens a save file dialog and returns the selected file path.
     * @return Absolute path of the selected file or null if cancelled
     */
    public static String getSaveFilePathFromChooser() {
        File[] selected = promptForFiles("Select a file location", FileChooser.SAVE_DIALOG,
                getDefaultChooserFile(), null);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Opens a file open dialog and returns the selected file path.
     * @return Absolute path of the selected file or null if cancelled
     */
    public static String getOpenFilePathFromChooser() {
        File[] selected = promptForFiles("Select a file location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), null);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Opens a file open dialog and returns the selected files.
     * @return Array of selected files or null if cancelled
     */
    public static File[] getOpenFileFromChooser() {
        return promptForFiles("Select files location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), null);
    }

    /**
     * Opens a directory selection dialog and returns the selected directory path.
     * @return Absolute path of the selected directory or null if cancelled
     */
    public static String getOpenDirectoryPathFromChooser() {
        FileFilter directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return true;
            }

            @Override
            public String getDescription() {
                // AWTFileChooserWrapper checks description prefix for directory mode.
                return "Directory selection";
            }
        };
        File[] selected = promptForFiles("Select a directory location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), directoryFilter);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Lists the files in the specified directory.
     * @param path Directory path
     * @return Array of file names in the directory
     */
    public static String[] getOpenDirectory(String path) {
        return (new File(path)).list();
    }

    /**
     * Opens a file open dialog with a filter and returns the selected file path.
     * @param nameFilter Description for the file filter
     * @param extension File extension for the filter
     * @return Absolute path of the selected file or null if cancelled
     */
    public static String getOpenFilePathFromChooser(String nameFilter, String extension) {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(nameFilter, extension);
        File[] selected = promptForFiles("Select a file location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), filter);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Opens a file open dialog with a filter and returns the selected files.
     * @param nameFilter Description for the file filter
     * @param extension File extension for the filter
     * @return Array of selected files or null if cancelled
     */
    public static File[] getOpenFileFromChooser(String nameFilter, String extension) {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(nameFilter, extension);
        return promptForFiles("Select a file location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), filter);
    }

    /**
     * Opens a file open dialog with multiple extensions and returns the selected file path.
     * @param desc Description for the file filter
     * @param extension Array of file extensions for the filter
     * @return Absolute path of the selected file or null if cancelled
     */
    public static String getOpenFilePathFromChooser(String desc, String[] extension) {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, extension);
        File[] selected = promptForFiles("Select a file location", FileChooser.LOAD_DIALOG,
                getDefaultChooserFile(), filter);
        return (selected != null && selected.length > 0 && selected[0] != null)
                ? selected[0].getAbsolutePath() : null;
    }

    /**
     * Initializes a DataInputStream for the specified file path.
     * @param path File path
     * @return DataInputStream or null if file not found
     */
    public static DataInputStream initInFile(String path) {
        DataInputStream dis = null;
        FileInputStream fis = null;
        try {
            new File(path).createNewFile();
            fis = new FileInputStream(path);
            dis = new DataInputStream(fis);
        } catch (FileNotFoundException fnf) {
            System.out.println(fnf.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dis;
    }

    /**
     * Parses lines from a file into a reversed array.
     * @param reader LineNumberReader for the file
     * @return Reversed array of lines
     */
    public static String[] parseFile2RevArray(LineNumberReader reader) {
        ArrayList<String> as = new ArrayList<>();
        String lineString = null;
        try {
            lineString = reader.readLine();
            while (lineString != null && !lineString.equals("")) {
                as.add(lineString.trim());
                lineString = reader.readLine();
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        // Convert and reverse array dimensions
        return ArrayConversion.array2to1Conversion(
            ArrayConversion.arrayDimReverse(
                ArrayConversion.array1to2Conversion(as.toArray(new String[0]))));
    }

    /**
     * Parses lines from a file into an array, skipping the first two lines.
     * @param reader LineNumberReader for the file
     * @return Array of lines
     */
    public static String[] parseFile2Array(LineNumberReader reader) {
        ArrayList<String> as = new ArrayList<>();
        String lineString = null;
        try {
            // Skip first two lines
            reader.readLine();
            reader.readLine();
            for (lineString = reader.readLine(); lineString != null && !lineString.equals(""); lineString = reader.readLine()) {
                as.add(lineString.trim());
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return as.toArray(new String[0]);
    }

    /**
     * Parses lines from a file into an array, skipping a specified number of lines.
     * @param reader LineNumberReader for the file
     * @param lineSkip Number of lines to skip
     * @return Array of lines
     */
    public static String[] parseFile2Array(LineNumberReader reader, int lineSkip) {
        ArrayList<String> as = new ArrayList<>();
        String lineString = null;
        try {
            for (int i = 0; i < lineSkip; i++)
                reader.readLine();
            lineString = reader.readLine();
            if (lineString != null)
                as.add(lineString.trim());
            reader.readLine();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return as.toArray(new String[0]);
    }

    /**
     * Parses lines from a file into an array, marking a line and reading up to 50 lines.
     * @param reader LineNumberReader for the file
     * @param lineSkip Number of lines to skip (unused)
     * @param markLine Line to mark
     * @return Array of lines
     */
    public static String[] parseFile2Array(LineNumberReader reader, int lineSkip, int markLine) {
        ArrayList<String> as = new ArrayList<>();
        String lineRead = null;
        try {
            reader.mark(markLine);
            for (int k = markLine; (lineRead = reader.readLine()) != null && k >= markLine; k++) {
                if (k >= markLine + 50)
                    break;
                as.add(lineRead.trim());
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return as.toArray(new String[0]);
    }

    /**
     * Marks the current position in the reader.
     * @param reader LineNumberReader
     * @param m Number of characters to read ahead
     * @throws Exception if marking fails
     */
    public static void mark(LineNumberReader reader, int m) throws Exception {
        reader.mark(m);
    }

    /**
     * Parses the header line from a file and returns an array of header values.
     * @param reader LineNumberReader for the file
     * @return Array of header values
     */
    public static String[] parseFileHeader(LineNumberReader reader) {
        ArrayList<String> as = new ArrayList<>();
        String lineString = null;
        try {
            lineString = reader.readLine();
            if (lineString != null) {
                String[] h = lineString.split(",");
                System.out.println(h.length + "  " + lineString);
                for (String value : h) {
                    as.add(value.trim());
                }
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return as.toArray(new String[0]);
    }

    /**
     * Splits a line into an array using the given expression.
     * @param line Input line
     * @param expression Delimiter expression
     * @return Array of split values
     */
    public static String[] parseLine2Array(String line, String expression) {
        return line.split(expression);
    }

    /**
     * Initializes a FileOutputStream for the specified file path.
     * Deletes the file if it exists.
     * @param path File path
     * @return FileOutputStream or null if file not found
     */
    public static FileOutputStream initOutFile(String path) {
        FileOutputStream fos = null;
        try {
            File file = new File(path);
            if (file.exists())
                file.delete();
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException fnf) {
            System.out.println(fnf.getMessage());
        }
        return fos;
    }

    /**
     * Initializes a FileOutputStream for the specified file.
     * Deletes the file if it exists.
     * @param file File object
     * @return FileOutputStream or null if file not found
     */
    public static FileOutputStream initOutFile(File file) {
        FileOutputStream fos = null;
        try {
            if (file.exists())
                file.delete();
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException fnf) {
            System.out.println(fnf.getMessage());
        }
        return fos;
    }

    /**
     * Initializes a FileOutputStream for the specified file path, with append option.
     * Deletes the file if it exists and append is false.
     * @param path File path
     * @param append Whether to append to the file
     * @return FileOutputStream or null if file not found
     */
    public static FileOutputStream initOutFile(String path, boolean append) {
        FileOutputStream fos = null;
        try {
            File file = new File(path);
            if (file.exists() && !append)
                file.delete();
            fos = new FileOutputStream(file, append);
        } catch (FileNotFoundException fnf) {
            System.out.println(fnf.getMessage());
        }
        return fos;
    }

    /**
     * Writes a string to a file and closes the stream.
     * @param fos FileOutputStream
     * @param writestr String to write
     */
    public static void writetofile(FileOutputStream fos, String writestr) {
        try {
            fos.write(writestr.getBytes());
            fos.close();
        } catch (IOException ioe) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reverses the dimensions of a string array and returns the result.
     * @param in Input array
     * @return Reversed array
     */
    public static String[] arrayDimReverseWrite(String[] in) {
        String[][] s1 = ArrayConversion.array1to2Conversion(in);
        String[] out = new String[in[0].split(",").length];
        for (int i = 0; i < s1[0].length; i++) {
            StringBuilder s = new StringBuilder();
            for (int j = 0; j < s1.length; j++) {
                s.append(",").append(s1[j][i]);
            }
            out[i] = s.substring(1);
        }
        return out;
    }

    /**
     * Writes an array of strings to a file, each on a new line, and closes the stream.
     * @param fos FileOutputStream
     * @param writestr Array of strings to write
     */
    public static void writetofile(FileOutputStream fos, String[] writestr) {
        try {
            for (String str : writestr) {
                fos.write(str.getBytes());
                fos.write("\n".getBytes());
            }
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file at the specified path.
     * @param path File path
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        file.delete();
    }

    /**
     * Loads an image icon from a URL string.
     * @param s URL string
     * @return ImageIcon or null if not found
     */
    public static ImageIcon getIcon(String s) {
        ImageIcon icon = null;
        URL imgURL = null;
        try {
            imgURL = new URL(s);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (imgURL != null)
            icon = new ImageIcon(imgURL);
        else
            System.err.println("Couldn't find file: " + s);
        return icon;
    }

}