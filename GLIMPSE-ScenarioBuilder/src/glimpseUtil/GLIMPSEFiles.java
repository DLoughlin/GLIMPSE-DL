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
package glimpseUtil;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * Utility class for file operations in GLIMPSE.
 * Handles file copying, moving, reading, writing, zipping, and more.
 *
 * @author US EPA
 */
public class GLIMPSEFiles {
    // Constants
    private static final int BUFFER_SIZE = 1024;
    private static final String COMMENT_CHAR = "#";
    private static final String ERROR_MSG_FILE_NOT_EXIST = "File does not exist:";
    private static final String ERROR_MSG_WRITING_FILE = "Error writing file";
    private static final String ERROR_MSG_DELETING_FILE = "error deleting ";
    private static final String ERROR_MSG_ZIPPING = "Error zipping directory: ";
    // Singleton instance
    public final static GLIMPSEFiles instance = new GLIMPSEFiles();

    // Dependencies
    public GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    public GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

    // File content caches
    public ArrayList<String> optionsFileContent = new ArrayList<>();
    //public ArrayList<String> glimpseCSVColumnsFileContent = new ArrayList<>();
    private ArrayList<String> glimpseXMLHeadersFileContent = new ArrayList<>();
    public ArrayList<String> glimpseTechBoundFileContent = new ArrayList<>();
    private ArrayList<String> gCamConfigurationTemplateFileContent = new ArrayList<>();
    private ArrayList<String> resPolicyTemplateFileContent = new ArrayList<>();
    private ArrayList<String> monetaryConversionsFileContent = new ArrayList<>();

    /**
     * Private constructor for singleton pattern.
     */
    public GLIMPSEFiles() {}

    /**
     * Get the singleton instance.
     * @return GLIMPSEFiles instance
     */
    public static GLIMPSEFiles getInstance() {
        return instance;
    }

    /**
     * Initialize utility and variable dependencies.
     * @param u GLIMPSEUtils
     * @param v GLIMPSEVariables
     * @param s GLIMPSEStyles (unused)
     * @param f GLIMPSEFiles (unused)
     */
    public void init(GLIMPSEUtils u, GLIMPSEVariables v, GLIMPSEStyles s, GLIMPSEFiles f) {
        if (u != null) utils = u;
        if (v != null) vars = v;
    }

    // --- File Move/Copy/Exist/Delete Section ---

    /**
     * Move a file from source to destination.
     * @param source Path to move from
     * @param destination Path to move to
     * @return true if successful
     */
    public boolean moveFile(Path source, Path destination) {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            System.out.println("Error moving file:" + source.getFileName() + " ... " + e);
            return false;
        }
    }

    /**
     * Copy a file from source to destination.
     * @param source Path to copy from
     * @param destination Path to copy to
     * @return true if successful
     */
    public boolean copyFile(Path source, Path destination) {
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a file exists.
     * @param filename File path
     * @return true if file exists
     */
    public boolean doesFileExist(String filename) {
        try {
            File file = new File(filename);
            return file.exists();
        } catch (Exception e) {
            System.out.println("Exception occurred accessing " + filename + ": " + e + ". Attempting to continue.");
            return false;
        }
    }

    /**
     * Delete a file by filename.
     * @param filename File path
     */
    public void deleteFile(String filename) {
        try {
            File file = new File(filename);
            if (file.exists()) deleteFile(file);
        } catch (Exception e) {
            System.out.println(ERROR_MSG_DELETING_FILE + filename);
        }
    }

    /**
     * Delete a file by File object.
     * @param file File object
     */
    public void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    /**
     * Delete all files with a given extension in a directory.
     * @param directoryName Directory path
     * @param extension File extension
     */
    public void deleteFiles(String directoryName, String extension) {
        final File dir = new File(directoryName);
        final String[] allFiles = dir.list();
        if (allFiles != null) {
            for (final String file : allFiles) {
                if (file.endsWith(extension)) {
                    new File(directoryName + File.separator + file).delete();
                }
            }
        }
    }

    /**
     * Delete a directory and all its contents.
     * @param file Directory File object
     */
    public void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    // --- File Reading Section ---

    /**
     * Load lines from a file, skipping lines starting with commentChar.
     * @param filename File path
     * @param commentChar Comment character
     * @return ArrayList of lines
     */
    public ArrayList<String> getStringArrayFromFile(String filename, String commentChar) {
        ArrayList<String> arrayList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && (commentChar == null || !line.startsWith(commentChar))) {
                    arrayList.add(line);
                }
            }
        } catch (Exception e) {
            System.out.println("In getStringArrayFromFile: Error reading file " + filename + ". Attempting to continue." + "   exception:" + e);
            e.printStackTrace();
        }
        return arrayList;
    }
    
	public List<String> getStringListFromFile(String regionListFilename, String commentChar) {
		ArrayList<String> arrayList = getStringArrayFromFile(regionListFilename, commentChar);		
		return arrayList;
	}

    /**
     * Load lines from a file that start with a given prefix.
     * @param filename File path
     * @param prefix Prefix string
     * @return ArrayList of matching lines
     */
    public ArrayList<String> getStringArrayWithPrefix(String filename, String prefix) {
        ArrayList<String> arrayList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.startsWith(prefix)) {
                    arrayList.add(line);
                }
            }
        } catch (Exception e) {
            System.out.println("In getStringArrayWithPrefix (" + prefix + "): Error reading file " + filename + ". Attempting to continue." + "   exception: " + e);
        }
        return arrayList;
    }

    /**
     * Load lines from a file that start with any of the given prefixes.
     * @param filename File path
     * @param prefixes Array of prefix strings
     * @return ArrayList of matching lines
     */
    public ArrayList<String> getStringArrayWithPrefix(String filename, String[] prefixes) {
        ArrayList<String> arrayList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    for (String prefix : prefixes) {
                        if (line.startsWith(prefix)) {
                            arrayList.add(line);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("In getStringArrayWithPrefix (multiple): Error reading file " + filename + ". Attempting to continue." + "   exception:" + e);
        }
        return arrayList;
    }

    /**
     * Load a specific line from a file, skipping comment lines.
     * @param filename File path
     * @param x Line number (1-based)
     * @param comment Comment string
     * @return The line content
     */
    public String getLineXFromFile(String filename, int x, String comment) {
        System.out.println("loading line " + x + " from file: " + filename);
        String s = "";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int lineNum = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(comment)) {
                    lineNum++;
                }
                if (lineNum == x) {
                    s = line;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("In getLineXFromFile (" + x + "): Error reading file " + filename + ". Attempting to continue." + "   exception:" + e);
        }
        return s;
    }

    /**
     * Load a list of files from a file, skipping lines containing typeString.
     * @param filename File path
     * @param typeString String to skip
     * @return ArrayList of file names
     */
    public ArrayList<String> loadFileListFromFile(String filename, String typeString) {
        ArrayList<String> fileList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(typeString) && line.length() > 0) {
                    String fileInList = line.trim();
                    File f = new File(fileInList);
                    if (!f.isDirectory()) {
                        fileList.add(fileInList);
                    }
                }
            }
        } catch (Exception e) {
            utils.warningMessage("Problem reading component file " + filename + " to determine type.");
            System.out.println("Error reading scenario component file to determine typ:" + e);
        }
        return fileList;
    }

    // --- File Writing Section ---

    /**
     * Save content to a file.
     * @param content Content string
     * @param filename File path
     */
    public void saveFile(String content, String filename) {
        saveFile(content, new File(filename));
    }

    /**
     * Save a list of strings to a file.
     * @param orig_content List of strings
     * @param filename File path
     */
    public void saveFile(ArrayList<String> orig_content, String filename) {
        File file = new File(filename);
        try (PrintStream filestream = new PrintStream(file)) {
            for (String str : orig_content) {
                filestream.print(str + "\r\n");
            }
        } catch (Exception e) {
            utils.warningMessage("Problem writing file to disk.");
            System.out.println("Problem writing file " + filename);
            System.out.println("Error:" + e);
            System.out.println("Trying to continue...");
        }
    }

    /**
     * Save content to a file.
     * @param content Content string
     * @param file File object
     */
    public void saveFile(String content, File file) {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(content);
        } catch (IOException ex) {
            utils.warningMessage("Error writing file:" + ex);
            System.out.println(ERROR_MSG_WRITING_FILE);
        }
    }

    // --- File Search Section ---

    /**
     * Search for text in a file, ignoring comment lines.
     * @param f File object
     * @param text Text to search for
     * @param comment Comment string
     * @return true if found
     */
    public boolean searchForTextInFile(File f, String text, String comment) {
        if (f.exists()) {
            ArrayList<String> arrayListTxt = getStringArrayFromFile(f.getPath(), comment);
            for (String str : arrayListTxt) {
                if (str.contains(text)) return true;
            }
        }
        return false;
    }

    /**
     * Search for text at the start of lines in a file.
     * @param f File object
     * @param text Text to search for
     * @param comment Comment string
     * @return true if found
     */
    public boolean searchForTextAtStartOfLinesInFile(File f, String text, String comment) {
        if (f.exists()) {
            ArrayList<String> arrayListTxt = getStringArrayFromFile(f.getPath(), comment);
            for (String str : arrayListTxt) {
                if (str.startsWith(text)) return true;
            }
        }
        return false;
    }

    /**
     * Search for text in a file and return the first matching line.
     * @param f File object
     * @param text Text to search for
     * @param comment Comment string
     * @return The matching line, or empty string
     */
    public String searchForTextInFileS(File f, String text, String comment) {
        if (f.exists()) {
            ArrayList<String> arrayListTxt = getStringArrayFromFile(f.getPath(), comment);
            for (String str : arrayListTxt) {
                if (str.contains(text)) return str;
            }
        }
        return "";
    }

    /**
     * Search for text in a file and return all matching lines.
     * @param filename File path
     * @param text Text to search for
     * @param comment Comment string
     * @return List of matching lines
     */
    public ArrayList<String> searchForTextInFileA(String filename, String text, String comment) {
        ArrayList<String> list = new ArrayList<>();
        File f = new File(filename);
        if (f.exists()) {
            ArrayList<String> arrayListTxt = getStringArrayFromFile(f.getPath(), comment);
            for (String str : arrayListTxt) {
                if (str.contains(text)) list.add(str);
            }
        }
        return list;
    }

    /**
     * Count lines containing text in a file, skipping comment lines.
     * @param f File object
     * @param text Text to search for
     * @param comment Comment string
     * @return Number of matching lines
     */
    public int countLinesWithTextInFile(File f, String text, String comment) {
        int count = 0;
        if (f.exists()) {
            ArrayList<String> arrayListTxt = getStringArrayFromFile(f.getPath(), comment);
            for (String str : arrayListTxt) {
                if (!str.startsWith(comment) && str.contains(text)) count++;
            }
        }
        return count;
    }

    // --- File Zipping Section ---

    /**
     * Zip a directory and its contents.
     * @param dir Directory to zip
     * @param zipDirName Output zip file name
     */
    public void zipDirectory(File dir, String zipDirName) {
        try {
            List<String> filesListInDir = populateFilesList(dir);
            try (FileOutputStream fos = new FileOutputStream(zipDirName);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                for (String filePath : filesListInDir) {
                    System.out.println("Zipping " + filePath);
                    ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1));
                    zos.putNextEntry(ze);
                    try (FileInputStream fis = new FileInputStream(filePath)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            System.out.println(ERROR_MSG_ZIPPING + e);
        }
    }

    /**
     * Populate a list of all files in a directory (recursive).
     * @param dir Directory
     * @return List of file paths
     * @throws IOException
     */
    private List<String> populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        List<String> listInDir = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    listInDir.add(file.getAbsolutePath());
                } else {
                    listInDir.addAll(populateFilesList(file));
                }
            }
        }
        return listInDir;
    }

    // --- Miscellaneous Utilities ---

    /**
     * Get last modified date info for a file.
     * @param filename File path
     * @return Date string or "Unknown"
     */
    public String getLastModifiedInfoForFile(String filename) {
        long long_date = 0;
        try {
            File f = new File(filename);
            if (f.exists()) long_date = f.lastModified();
        } catch (Exception e) {
            System.out.println("Error getting last modified date of file " + filename + vars.getEol() + e);
        }
        if (long_date == 0) {
            return "Unknown";
        } else {
            Date date = new Date(long_date);
            SimpleDateFormat df2 = new SimpleDateFormat("yy.MM.dd");
            return df2.format(date);
        }
    }

    /**
     * Get relative path from base directory.
     * @param base_dir Base directory
     * @param filename File path
     * @return Relative path
     */
    public String getRelativePath(String base_dir, String filename) {
        if (!filename.startsWith("..")) {
            try {
                Path base_path = Paths.get(base_dir);
                Path file_path = Paths.get(filename);
                Path relative_path = base_path.relativize(file_path);
                return relative_path.toString();
            } catch (Exception e) {
                System.out.println("error calculating relative filename. Using full path.");
            }
        }
        return filename;
    }

    /**
     * Get resolved path from base directory.
     * @param base_dir Base directory
     * @param filename File path
     * @return Resolved path
     */
    public String getResolvedPath(String base_dir, String filename) {
        try {
            Path base_path = Paths.get(base_dir);
            Path file_path = Paths.get(filename);
            Path resolved_path = base_path.resolve(file_path).normalize();
            return resolved_path.toString();
        } catch (Exception e) {
            System.out.println("error calculating resolved filename. Using full path.");
            return filename;
        }
    }

    /**
     * Initialize a BufferedWriter for a file in a directory.
     * @param dirName Directory name
     * @param filename File name
     * @return BufferedWriter
     */
    public BufferedWriter initializeBufferedFile(String dirName, String filename) {
        try {
            File dir = new File(dirName);
            if (!dir.exists()) dir.mkdir();
            File file = new File(dirName + File.separator + filename);
            if (!file.exists()) file.createNewFile();
            FileWriter fw = new FileWriter(file);
            return new BufferedWriter(fw);
        } catch (Exception e) {
            System.out.println("difficulty setting up temp file:" + e);
            return null;
        }
    }

    /**
     * Close a BufferedWriter.
     * @param bw BufferedWriter
     */
    public void closeBufferedFile(BufferedWriter bw) {
        if (bw != null) {
            try {
                bw.close();
            } catch (Exception e) {
                System.out.println("Problem closing temp file.");
            }
        }
    }

    /**
     * Write a string to a BufferedWriter.
     * @param bw BufferedWriter
     * @param s String to write
     */
    public void writeToBufferedFile(BufferedWriter bw, String s) {
        if (bw != null) {
            try {
                bw.write(s);
            } catch (Exception e) {
                System.out.println("Error writing to temp policy file:" + e);
            }
        }
    }

    /**
     * Concatenate source files into a destination file.
     * @param dest_filename Destination file
     * @param src_filenames List of source files
     */
    public void concatDestSources(String dest_filename, ArrayList<String> src_filenames) {
        try (FileWriter output = new FileWriter(dest_filename)) {
            for (String src : src_filenames) {
                try (Scanner sc = new Scanner(new File(src))) {
                    while (sc.hasNextLine()) {
                        String str = sc.nextLine();
                        output.append(str + vars.getEol());
                    }
                }
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Concatenate a single source file into a destination file.
     * @param dest_filename Destination file
     * @param src_filename Source file
     */
    public void concatDestSource(String dest_filename, String src_filename) {
        ArrayList<String> src_list = new ArrayList<>();
        src_list.add(src_filename);
        concatDestSources(dest_filename, src_list);
    }

    /**
     * Get matching lines in a file for a list of prefixes.
     * @param filename File path
     * @param array List of prefixes
     * @return List of matching lines
     */
    public ArrayList<String> getMatchingTextArrayInFile(String filename, ArrayList<String> array) {
        ArrayList<String> content = getStringArrayFromFile(filename, COMMENT_CHAR);
        boolean[] b = new boolean[array.size()];
        int count_true = 0;
        for (int j = 0; j < array.size(); j++) {
            for (int i = content.size() - 1; i >= 0; i--) {
                String line = content.get(i);
                if (line.startsWith(array.get(j))) {
                    b[j] = true;
                    count_true++;
                    array.set(j, line);
                    break;
                }
            }
            if (!b[j]) array.set(j, "");
            if (count_true == b.length) break;
        }
        return array;
    }

    /**
     * Delete a directory and all its contents using NIO.
     * @param path Path to directory
     * @throws IOException
     */
    public void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    /**
     * Get the size of a directory in bytes.
     * @param path Path to directory
     * @return Size in bytes
     */
    public long getDirectorySize(Path path) {
        long size = 0;
        try (Stream<Path> walk = Files.walk(path)) {
            size = walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    System.out.printf("Failed to get size of %s%n%s", p, e);
                    return 0L;
                }
            }).sum();
        } catch (IOException e) {
            System.out.println("IO error: " + e);
        }
        return size;
    }

    /**
     * Test if a folder exists.
     * @param filename Folder path
     * @return true if folder exists
     */
    public boolean testFolderExists(String filename) {
        if (filename != null) {
            File f = new File(filename);
            return f.isDirectory() && f.exists();
        }
        return false;
    }

    /**
     * Append text to a file if it exists.
     * @param txt Text to append
     * @param filename File path
     * @return true if successful
     */
    public boolean appendTextToFile(String txt, String filename) {
        if (doesFileExist(filename)) {
            try (FileWriter fr = new FileWriter(new File(filename), true)) {
                fr.write(txt);
                return true;
            } catch (Exception e) {
                System.out.println("Error appending text to file.");
            }
        }
        return false;
    }

    // --- UI/Editor Section ---

    /**
     * Show a file in the configured text editor, or system default if unavailable.
     * @param filename File path
     */
    public void showFileInTextEditor(String filename) {
        GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
        File f = new File(filename);
        if (!f.exists()) {
            String msg = ERROR_MSG_FILE_NOT_EXIST + filename;
            utils.warningMessage(msg);
            return;
        }

        // Preferred: run the configured editor as a proper arg list (handles paths with spaces).
        String editorCmd = vars.getTextEditor();
        if (editorCmd != null) {
            editorCmd = editorCmd.trim();
        }

        try {
            if (editorCmd != null && editorCmd.length() > 0) {
                // Allow options files to specify an editor with args, e.g.:
                //   "C:\\Program Files\\Notepad++\\notepad++.exe" -multiInst
                // Tokenize into args and append the filename as final arg.
                java.util.ArrayList<String> cmdArgs = new java.util.ArrayList<>();
                for (String a : CommandLineTokenizer.tokenize(editorCmd)) {
                    if (a != null && a.trim().length() > 0) {
                        cmdArgs.add(a);
                    }
                }
                cmdArgs.add(f.getAbsolutePath());

                // Fire-and-forget: we only care if it starts successfully.
                ProcessRunner.start(cmdArgs, null, null, null, null);
                return;
            }
        } catch (Exception e) {
            utils.warningMessage("Could not use text editor specified in options file. Using system default.");
            System.out.println("Error trying to open file to view with editor.");
            System.out.println("   file: " + filename);
            System.out.println("   editor: " + vars.getTextEditor());
            System.out.println("Error: " + e);
        }

        // Fallback: system default editor.
        try {
            Desktop.getDesktop().edit(f);
        } catch (Exception e1) {
            utils.warningMessage("Problem trying to open file with system text editor.");
            System.out.println("Error trying to open file to view with system text editor.");
            System.out.println("Error: " + e1);
        }
    }

    /**
     * Show a file in the configured XML editor, or system default if unavailable.
     * @param filename File path
     */
    public void showFileInXmlEditor(String filename) {
        showFileInTextEditor(filename);
    }

    /**
     * Open a file in the system file explorer.
     * @param filename File path
     */
    public void openFileExplorer(String filename) {
        File file = new File(filename);
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception e1) {
            utils.warningMessage("Problem opening system file browser.");
            System.out.println("Error opening system file browser.");
            e1.printStackTrace();
        }
    }

    // --- Key-Value File Section ---

    /**
     * Load key-value pairs from a file.
     * @param filename File path
     * @param delimiter Delimiter string
     * @return List of key-value pairs
     */
    public ArrayList<String[]> loadKeyValuePairsFromFile(String filename, String delimiter) {
        ArrayList<String[]> keywordValuePairs = new ArrayList<>();
        try (Scanner scan = new Scanner(new File(filename))) {
            scan.useDelimiter(delimiter);
            while (scan.hasNext()) {
                String str = scan.nextLine();
                if (!str.startsWith(COMMENT_CHAR) && str.contains(delimiter)) {
                    int n = str.indexOf(delimiter);
                    if (n >= 0) {
                        String[] s = new String[2];
                        s[0] = str.substring(0, n).trim();
                        s[1] = str.substring(n + 1).trim();
                        keywordValuePairs.add(s);
                    }
                }
            }
        } catch (Exception e) {
            utils.warningMessage("Problem reading keyword-value pairs.");
            System.out.println("Error reading keyword-value pairs:" + e);
            utils.exitOnException();
        }
        return keywordValuePairs;
    }

    // --- File Trash Section ---

    /**
     * Move a file or directory to the trash directory.
     * @param toTrash File or directory to trash
     * @return true if successful
     */
    public boolean trash(File toTrash) {
        Path trashPath = new File(vars.getTrashDir()).toPath();
        if (toTrash.isDirectory()) {
            File[] files = toTrash.listFiles();
            if (files != null) {
                for (File file : files) {
                    trash(file);
                }
            }
        } else {
            try {
                Files.move(Paths.get(toTrash.getPath()), trashPath, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Move a file to the trash directory by filename.
     * @param origFilename File path
     */
    public void trashFile(String origFilename) {
        try {
            File origFile = new File(origFilename);
            String name = origFile.getName();
            String trashFilename = vars.getTrashDir() + File.separator + name;
            File newFile = new File(trashFilename);
            if (!newFile.exists()) newFile.createNewFile();
            try (InputStream oInStream = new FileInputStream(origFile);
                 OutputStream oOutStream = new FileOutputStream(newFile)) {
                byte[] oBytes = new byte[BUFFER_SIZE];
                int nLength;
                BufferedInputStream oBuffInputStream = new BufferedInputStream(oInStream);
                while ((nLength = oBuffInputStream.read(oBytes)) > 0) {
                    oOutStream.write(oBytes, 0, nLength);
                }
            }
            origFile.delete();
        } catch (Exception e) {
            System.out.println("Error trying to back up configuration file for scenario.");
            System.out.println("error: " + e);
            utils.exitOnException();
        }
    }

    /**
     * Move a file to the trash directory by File object.
     * @param origFile File object
     */
    public void trashFile(File origFile) {
        if (origFile != null) trashFile(origFile.getAbsolutePath());
    }

    // --- File Copy/Move Section (Manual) ---

    /**
     * Copy a file from origFilename to newFilename.
     * @param origFilename Source file
     * @param newFilename Destination file
     */
    public void copyFile(String origFilename, String newFilename) {
        try {
            File origFile = new File(origFilename);
            File newFile = new File(newFilename);
            if (!newFile.exists()) {
                File parentFile = newFile.getParentFile();
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs();
                }
                newFile.createNewFile();
            }
            try (InputStream oInStream = new FileInputStream(origFile);
                 OutputStream oOutStream = new FileOutputStream(newFile)) {
                byte[] oBytes = new byte[BUFFER_SIZE];
                int nLength;
                BufferedInputStream oBuffInputStream = new BufferedInputStream(oInStream);
                while ((nLength = oBuffInputStream.read(oBytes)) > 0) {
                    oOutStream.write(oBytes, 0, nLength);
                }
            }
        } catch (Exception e) {
            System.out.println("Error trying to copy file from " + origFilename + " to " + newFilename);
            System.out.println("error: " + e);
            utils.exitOnException();
        }
    }

    /**
     * Copy and move a file from origFilename to newFilename.
     * @param origFilename Source file
     * @param newFilename Destination file
     */
    public void copyAndMoveFile(String origFilename, String newFilename) {
        copyFile(origFilename, newFilename);
    }

    // --- File Loading Section ---

    /**
     * Load required files into memory.
     */
    public void loadFiles() {
        try {
            glimpseXMLHeadersFileContent = getStringArrayFromFile(vars.getXmlHeaderFilename(), COMMENT_CHAR);
        } catch (Exception e) {
            System.out.println("\nError opening files needed by GLIMPSE.");
            System.out.println("Exception " + e);
        }
        try {
            glimpseTechBoundFileContent = getStringArrayFromFile(vars.getTchBndListFilename(), COMMENT_CHAR);
        } catch (Exception e) {
            System.out.println("Error opening files needed by GLIMPSE.");
            System.out.println("Exception " + e);
        }
        try {
            gCamConfigurationTemplateFileContent = getStringArrayFromFile(vars.getConfigurationTemplateFilename(), COMMENT_CHAR);
        } catch (Exception e) {
            System.out.println("Error opening files needed by GLIMPSE.");
            System.out.println("Exception " + e);
        }
        try {
            setMonetaryConversionsFileContent(getStringArrayFromFile(vars.getMonetaryConversionsFilename(), COMMENT_CHAR));
        } catch (Exception e) {
            System.out.println("Error opening files needed by GLIMPSE.");
            System.out.println("Exception " + e);
        }
    }

	public ArrayList<String> getMonetaryConversionsFileContent() {
		return monetaryConversionsFileContent;
	}

	public void setMonetaryConversionsFileContent(ArrayList<String> monetaryConversionsFileContent) {
		this.monetaryConversionsFileContent = monetaryConversionsFileContent;
	}

    /**
     * Open a file in the configured text editor (from the options file), falling back to the
     * system default application if no editor is configured or launching the configured editor fails.
     *
     * @param filename Path to the file to open.
     */
    public void openInTextEditorWithFallback(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            utils.warningMessage("No file specified to open.");
            return;
        }

        File file = new File(filename);
        if (!file.exists()) {
            utils.warningMessage(ERROR_MSG_FILE_NOT_EXIST + " " + filename);
            return;
        }

        // 1) Try the user-configured text editor.
        String editor = null;
        try {
            editor = vars.getTextEditor();
        } catch (Exception ignored) {
            // We'll fall back below.
        }

        if (editor != null) {
            editor = editor.trim();
        }

        if (editor != null && !editor.isEmpty()) {
            try {
                // Use ProcessBuilder to avoid cmd quoting issues. If editor includes args, keep it simple: treat as a command.
                // Typical configured values are full paths like C:\\Windows\\System32\\notepad.exe
                new ProcessBuilder(editor, file.getAbsolutePath()).start();
                return;
            } catch (Exception e) {
                System.out.println("Warning: configured text editor failed (" + editor + ") for file: " + filename);
                System.out.println("  ---> " + e);
                // Fall through to system default.
            }
        }

        // 2) Fall back to system default editor.
        try {
            if (!Desktop.isDesktopSupported()) {
                utils.warningMessage("Desktop integration not supported; can't open file: " + filename);
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            utils.warningMessage("Could not open file in system default editor: " + filename + "\n" + e.getMessage());
            System.out.println("Error opening file " + filename + ": " + e);
        }
    }

}
