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
package glimpseUtil;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * A utility class for creating and displaying JavaFX FileChooser dialogs.
 * This class provides a simplified and robust way to prompt the user to
 * select a file for opening or saving.
 *
 * This version has been refactored for clarity, correctness, and modern
 * Java 8+ best practices.
 */
public final class FileChooserPlus {

    // Set -Dglimpse.nativeFileDialog=false to force JavaFX dialogs only.
    private static final boolean PREFER_NATIVE_FILE_DIALOG =
            !"false".equalsIgnoreCase(System.getProperty("glimpse.nativeFileDialog", "true"));
    private static final boolean DEBUG_NATIVE_FALLBACK =
            "true".equalsIgnoreCase(System.getProperty("glimpse.nativeFileDialog.debug", "false"));

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FileChooserPlus() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Shows a "Save File" dialog.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param initialFileName  The suggested name for the file.
     * @param filter           The extension filter to apply.
     * @return An Optional containing the selected file, or an empty Optional if canceled.
     */
    public static File showSaveDialog(Window ownerWindow, String title, File initialDirectory, String initialFileName, FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                File nativeSelection = showNativeDialog(title, initialDirectory, initialFileName, filter, FileDialog.SAVE);
                if (nativeSelection != null) {
                    return nativeSelection;
                }
            } catch (Throwable t) {
                logNativeFallback("save", title, t);
                // Fall through to JavaFX chooser.
            }
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, initialFileName, filter);
        return chooser.showSaveDialog(ownerWindow);
    }

    /**
     * Shows an "Open File" dialog.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param filter           The extension filter to apply.
     * @return An Optional containing the selected file, or an empty Optional if canceled.
     */
    public static File showOpenDialog(Window ownerWindow, String title, File initialDirectory, FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                File nativeSelection = showNativeDialog(title, initialDirectory, null, filter, FileDialog.LOAD);
                if (nativeSelection != null) {
                    return nativeSelection;
                }
            } catch (Throwable t) {
                logNativeFallback("open", title, t);
                // Fall through to JavaFX chooser.
            }
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, null, filter);
        return chooser.showOpenDialog(ownerWindow);
    }

    /**
     * Shows an "Open File" dialog that allows selecting multiple files.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param filter           The extension filter to apply.
     * @return A list of selected files, or null if canceled.
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                List<File> nativeSelection = showNativeMultipleDialog(title, initialDirectory, filter);
                if (nativeSelection != null) {
                    return nativeSelection;
                }
            } catch (Throwable t) {
                logNativeFallback("open-multi", title, t);
                // Fall through to JavaFX chooser.
            }
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, null, filter);
        return chooser.showOpenMultipleDialog(ownerWindow);
    }

    private static File showNativeDialog(String title, File initialDirectory, String initialFileName,
            FileChooser.ExtensionFilter filter, int mode) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return null;
        }

        AtomicReference<File> selected = new AtomicReference<>();
        AtomicReference<Exception> thrown = new AtomicReference<>();

        Runnable showDialog = () -> {
            FileDialog dialog = null;
            try {
                dialog = new FileDialog((java.awt.Frame) null, title == null ? "" : title, mode);
                if (initialDirectory != null && initialDirectory.isDirectory()) {
                    dialog.setDirectory(initialDirectory.getAbsolutePath());
                }
                if (initialFileName != null && !initialFileName.trim().isEmpty()) {
                    dialog.setFile(initialFileName.trim());
                }
                if (filter != null) {
                    dialog.setFilenameFilter((dir, name) -> matchesExtensionFilter(name, filter));
                }
                dialog.setVisible(true);

                String fileName = dialog.getFile();
                String directory = dialog.getDirectory();
                if (fileName != null && directory != null) {
                    selected.set(new File(directory, fileName));
                }
            } catch (Exception ex) {
                thrown.set(ex);
            } finally {
                if (dialog != null) {
                    dialog.dispose();
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            showDialog.run();
        } else {
            EventQueue.invokeAndWait(showDialog);
        }

        if (thrown.get() != null) {
            throw thrown.get();
        }
        return selected.get();
    }

    private static List<File> showNativeMultipleDialog(String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return null;
        }

        AtomicReference<List<File>> selected = new AtomicReference<>();
        AtomicReference<Exception> thrown = new AtomicReference<>();

        Runnable showDialog = () -> {
            FileDialog dialog = null;
            try {
                dialog = new FileDialog((java.awt.Frame) null, title == null ? "" : title, FileDialog.LOAD);
                dialog.setMultipleMode(true);
                if (initialDirectory != null && initialDirectory.isDirectory()) {
                    dialog.setDirectory(initialDirectory.getAbsolutePath());
                }
                if (filter != null) {
                    dialog.setFilenameFilter((dir, name) -> matchesExtensionFilter(name, filter));
                }
                dialog.setVisible(true);

                File[] files = dialog.getFiles();
                if (files != null && files.length > 0) {
                    selected.set(new ArrayList<>(Arrays.asList(files)));
                    return;
                }

                String fileName = dialog.getFile();
                String directory = dialog.getDirectory();
                if (fileName != null && directory != null) {
                    selected.set(new ArrayList<>(Arrays.asList(new File(directory, fileName))));
                }
            } catch (Exception ex) {
                thrown.set(ex);
            } finally {
                if (dialog != null) {
                    dialog.dispose();
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            showDialog.run();
        } else {
            EventQueue.invokeAndWait(showDialog);
        }

        if (thrown.get() != null) {
            throw thrown.get();
        }
        return selected.get();
    }

    private static boolean matchesExtensionFilter(String fileName, FileChooser.ExtensionFilter filter) {
        if (fileName == null || filter == null) {
            return true;
        }
        List<String> patterns = filter.getExtensions();
        if (patterns == null || patterns.isEmpty()) {
            return true;
        }
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            String p = pattern.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty() || "*".equals(p) || "*.*".equals(p)) {
                return true;
            }
            if (p.startsWith("*.")) {
                if (lowerFileName.endsWith(p.substring(1))) {
                    return true;
                }
                continue;
            }
            if (p.startsWith(".")) {
                if (lowerFileName.endsWith(p)) {
                    return true;
                }
                continue;
            }
            if (p.indexOf('*') >= 0 || p.indexOf('?') >= 0) {
                if (wildcardMatches(lowerFileName, p)) {
                    return true;
                }
                continue;
            }
            if (lowerFileName.equals(p) || lowerFileName.endsWith("." + p) || lowerFileName.endsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean wildcardMatches(String text, String wildcardPattern) {
        if (text == null || wildcardPattern == null) {
            return false;
        }
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < wildcardPattern.length(); i++) {
            char ch = wildcardPattern.charAt(i);
            if (ch == '*') {
                regex.append(".*");
            } else if (ch == '?') {
                regex.append('.');
            } else {
                if ("\\.^$|()[]{}+".indexOf(ch) >= 0) {
                    regex.append('\\');
                }
                regex.append(ch);
            }
        }
        return text.matches(regex.toString());
    }

    private static void logNativeFallback(String op, String title, Throwable t) {
        String safeTitle = title == null ? "" : title;
        String reason = (t == null) ? "unknown" : (t.getClass().getSimpleName() + ": " + t.getMessage());
        System.out.println("[FileChooserPlus] Native file dialog failed for " + op
                + " (title='" + safeTitle + "'); falling back to JavaFX chooser. Reason: " + reason);
        if (DEBUG_NATIVE_FALLBACK && t != null) {
            t.printStackTrace(System.out);
        }
    }

    /**
     * Private helper method to create and configure a FileChooser instance.
     */
    private static FileChooser createAndConfigureChooser(String title, File initialDirectory, String initialFileName, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        if (initialFileName != null && !initialFileName.isEmpty()) {
            fileChooser.setInitialFileName(initialFileName);
        }

        if (filter != null) {
            fileChooser.getExtensionFilters().add(filter);
        }

        return fileChooser;
    }

    /**
     * Creates an ExtensionFilter for use with a FileChooser.
     *
     * @param description The textual description for the filter (e.g., "Image Files").
     * @param extensions  The file extensions to include, without the dot (e.g., "jpg", "png").
     * @return A configured FileChooser.ExtensionFilter instance.
     */
    public static FileChooser.ExtensionFilter createExtensionFilter(String description, String... extensions) {
        // Prepend "*." to each extension to create the pattern.
        for (int i = 0; i < extensions.length; i++) {
            extensions[i] = "*." + extensions[i];
        }
        return new FileChooser.ExtensionFilter(description, extensions);
    }
}
