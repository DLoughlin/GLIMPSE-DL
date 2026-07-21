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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * A utility class for showing file chooser dialogs using the OS-native picker.
 * <p>
 * On all platforms the primary implementation uses {@link java.awt.FileDialog},
 * which delegates to the real OS-native dialog:
 * <ul>
 *   <li><b>Windows</b> – Win32 {@code GetOpenFileName} / {@code GetSaveFileName}
 *       (the modern Windows Explorer file picker)</li>
 *   <li><b>macOS</b>  – {@code NSOpenPanel} / {@code NSSavePanel}
 *       (the native macOS file chooser)</li>
 *   <li><b>Linux</b>  – GTK {@code GtkFileChooserDialog} when GTK is available</li>
 * </ul>
 * If the native AWT dialog throws an unexpected exception the JavaFX
 * {@link FileChooser} is used as a last-resort fallback.  A user clicking
 * <em>Cancel</em> returns {@code null} immediately without opening any
 * fallback dialog.
 * <p>
 * Kill-switch: set {@code -Dglimpse.nativeFileDialog=false} to force the
 * JavaFX dialog on every platform.<br>
 * Debug tracing: set {@code -Dglimpse.nativeFileDialog.debug=true}.
 */
public final class FileChooserPlus {

    // Set -Dglimpse.nativeFileDialog=false to force JavaFX dialogs only.
    private static final boolean PREFER_NATIVE_FILE_DIALOG =
            !"false".equalsIgnoreCase(System.getProperty("glimpse.nativeFileDialog", "true"));
    private static final boolean DEBUG_NATIVE_FALLBACK =
            "true".equalsIgnoreCase(System.getProperty("glimpse.nativeFileDialog.debug", "false"));
    private static final boolean DEBUG_NATIVE_FLOW = DEBUG_NATIVE_FALLBACK;
    private static final String WINDOWS_CANCEL_TOKEN = "__GLIMPSE_CANCEL__";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FileChooserPlus() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Shows a "Save File" dialog using the OS-native file picker.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param initialFileName  The suggested name for the file.
     * @param filter           The extension filter to apply.
     * @return The selected file, or {@code null} if the user cancelled.
     */
    public static File showSaveDialog(Window ownerWindow, String title, File initialDirectory,
            String initialFileName, FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                if (isWindows()) {
                    return showWindowsNativeSaveDialog(title, initialDirectory, initialFileName, filter);
                }
                File selected = showNativeDialog(ownerWindow, title, initialDirectory, initialFileName, filter, FileDialog.SAVE);
                // null means the user cancelled — stop here, do NOT fall through to JavaFX.
                return selected;
            } catch (Throwable t) {
                logNativeFallback("save", title, t);
                // Native threw an exception — fall through to JavaFX.
            }
        } else {
            logNativeDisabled("save", title);
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, initialFileName, filter);
        return chooser.showSaveDialog(ownerWindow);
    }

    /**
     * Shows an "Open File" dialog using the OS-native file picker.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param filter           The extension filter to apply.
     * @return The selected file, or {@code null} if the user cancelled.
     */
    public static File showOpenDialog(Window ownerWindow, String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                if (isWindows()) {
                    return showWindowsNativeOpenDialog(title, initialDirectory, filter);
                }
                File selected = showNativeDialog(ownerWindow, title, initialDirectory, null, filter, FileDialog.LOAD);
                // null means the user cancelled — stop here, do NOT fall through to JavaFX.
                return selected;
            } catch (Throwable t) {
                logNativeFallback("open", title, t);
                // Native threw an exception — fall through to JavaFX.
            }
        } else {
            logNativeDisabled("open", title);
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, null, filter);
        return chooser.showOpenDialog(ownerWindow);
    }

    /**
     * Shows an "Open File" dialog that allows selecting multiple files using the OS-native file picker.
     *
     * @param ownerWindow      The parent window for the dialog.
     * @param title            The title for the dialog window.
     * @param initialDirectory The directory to open initially.
     * @param filter           The extension filter to apply.
     * @return A list of selected files, or {@code null} if the user cancelled.
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) {
        if (PREFER_NATIVE_FILE_DIALOG) {
            try {
                if (isWindows()) {
                    return showWindowsNativeOpenMultipleDialog(title, initialDirectory, filter);
                }
                List<File> selected = showNativeMultipleDialog(ownerWindow, title, initialDirectory, filter);
                // null means the user cancelled — stop here, do NOT fall through to JavaFX.
                return selected;
            } catch (Throwable t) {
                logNativeFallback("open-multi", title, t);
                // Native threw an exception — fall through to JavaFX.
            }
        } else {
            logNativeDisabled("open-multi", title);
        }
        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, null, filter);
        return chooser.showOpenMultipleDialog(ownerWindow);
    }

    private static File showNativeDialog(Window ownerWindow, String title, File initialDirectory, String initialFileName,
            FileChooser.ExtensionFilter filter, int mode) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return null;
        }

        final AtomicReference<File> selected = new AtomicReference<>();
        final AtomicReference<Exception> thrown = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] ownerCenter = getOwnerCenterOnFxThread(ownerWindow);

        // Run on a dedicated background thread so we can safely call
        // EventQueue.invokeAndWait without risking a deadlock between the
        // JavaFX Application Thread and the AWT Event Dispatch Thread.
        Thread bgThread = new Thread(() -> {
            try {
                EventQueue.invokeAndWait(() -> {
                    FileDialog dialog = null;
                    java.awt.Frame ownerFrame = null;
                    try {
                        ownerFrame = createNativeDialogOwner(ownerCenter);
                        dialog = new FileDialog(ownerFrame, title == null ? "" : title, mode);
                        if (initialDirectory != null && initialDirectory.isDirectory()) {
                            dialog.setDirectory(initialDirectory.getAbsolutePath());
                        }
                        if (initialFileName != null && !initialFileName.trim().isEmpty()) {
                            dialog.setFile(initialFileName.trim());
                        }
                        if (filter != null) {
                            dialog.setFilenameFilter((dir, name) -> matchesExtensionFilter(name, filter));
                        }
                        dialog.setLocationRelativeTo(ownerFrame);
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
                        if (ownerFrame != null) {
                            ownerFrame.dispose();
                        }
                    }
                });
            } catch (Exception ex) {
                thrown.set(ex);
            } finally {
                latch.countDown();
            }
        }, "glimpse-native-filedlg");
        bgThread.setDaemon(true);
        bgThread.start();

        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }

        if (thrown.get() != null) {
            throw thrown.get();
        }
        return selected.get();
    }

    private static List<File> showNativeMultipleDialog(Window ownerWindow, String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return null;
        }

        final AtomicReference<List<File>> selected = new AtomicReference<>();
        final AtomicReference<Exception> thrown = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] ownerCenter = getOwnerCenterOnFxThread(ownerWindow);

        Thread bgThread = new Thread(() -> {
            try {
                EventQueue.invokeAndWait(() -> {
                    FileDialog dialog = null;
                    java.awt.Frame ownerFrame = null;
                    try {
                        ownerFrame = createNativeDialogOwner(ownerCenter);
                        dialog = new FileDialog(ownerFrame, title == null ? "" : title, FileDialog.LOAD);
                        dialog.setMultipleMode(true);
                        if (initialDirectory != null && initialDirectory.isDirectory()) {
                            dialog.setDirectory(initialDirectory.getAbsolutePath());
                        }
                        if (filter != null) {
                            dialog.setFilenameFilter((dir, name) -> matchesExtensionFilter(name, filter));
                        }
                        dialog.setLocationRelativeTo(ownerFrame);
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
                        if (ownerFrame != null) {
                            ownerFrame.dispose();
                        }
                    }
                });
            } catch (Exception ex) {
                thrown.set(ex);
            } finally {
                latch.countDown();
            }
        }, "glimpse-native-filedlg-multi");
        bgThread.setDaemon(true);
        bgThread.start();

        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
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

    private static File showWindowsNativeSaveDialog(String title, File initialDirectory, String initialFileName,
            FileChooser.ExtensionFilter filter) throws IOException, InterruptedException {
        String initialDir = initialDirectory != null ? initialDirectory.getAbsolutePath() : "";
        String safeTitle = title == null ? "" : title;
        String safeFile = initialFileName == null ? "" : initialFileName;
        String filterSpec = buildWindowsFilterSpec(filter);

        String script =
                "$ErrorActionPreference='Stop';"
                + "Add-Type -AssemblyName System.Windows.Forms;"
                + "[System.Windows.Forms.Application]::EnableVisualStyles();"
                + "$owner = New-Object System.Windows.Forms.Form;"
                + "$owner.StartPosition='Manual';$owner.Size=New-Object System.Drawing.Size(1,1);"
                + "$owner.Location=New-Object System.Drawing.Point(-32000,-32000);"
                + "$owner.ShowInTaskbar=$false;$owner.TopMost=$true;$owner.Opacity=0;"
                + "$owner.Show();$owner.Activate();"
                + "$dlg = New-Object System.Windows.Forms.SaveFileDialog;"
                + "$dlg.Title='" + escapePsSingleQuoted(safeTitle) + "';"
                + "$dlg.InitialDirectory='" + escapePsSingleQuoted(initialDir) + "';"
                + "$dlg.FileName='" + escapePsSingleQuoted(safeFile) + "';"
                + "$dlg.OverwritePrompt=$true;"
                + "$dlg.AddExtension=$true;"
                + "$dlg.Filter='" + escapePsSingleQuoted(filterSpec) + "';"
                + "$result = $dlg.ShowDialog($owner);"
                + "if($result -eq [System.Windows.Forms.DialogResult]::OK){[Console]::Out.WriteLine($dlg.FileName)}"
                + "else {[Console]::Out.WriteLine('" + WINDOWS_CANCEL_TOKEN + "')}"
                + "$owner.Close();$owner.Dispose();";

        List<String> lines = runPowerShellChooserScript(script);
        if (lines.isEmpty() || WINDOWS_CANCEL_TOKEN.equals(lines.get(0))) {
            return null;
        }
        return new File(lines.get(0));
    }

    private static File showWindowsNativeOpenDialog(String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) throws IOException, InterruptedException {
        String initialDir = initialDirectory != null ? initialDirectory.getAbsolutePath() : "";
        String safeTitle = title == null ? "" : title;
        String filterSpec = buildWindowsFilterSpec(filter);

        String script =
                "$ErrorActionPreference='Stop';"
                + "Add-Type -AssemblyName System.Windows.Forms;"
                + "[System.Windows.Forms.Application]::EnableVisualStyles();"
                + "$owner = New-Object System.Windows.Forms.Form;"
                + "$owner.StartPosition='Manual';$owner.Size=New-Object System.Drawing.Size(1,1);"
                + "$owner.Location=New-Object System.Drawing.Point(-32000,-32000);"
                + "$owner.ShowInTaskbar=$false;$owner.TopMost=$true;$owner.Opacity=0;"
                + "$owner.Show();$owner.Activate();"
                + "$dlg = New-Object System.Windows.Forms.OpenFileDialog;"
                + "$dlg.Title='" + escapePsSingleQuoted(safeTitle) + "';"
                + "$dlg.InitialDirectory='" + escapePsSingleQuoted(initialDir) + "';"
                + "$dlg.Multiselect=$false;"
                + "$dlg.Filter='" + escapePsSingleQuoted(filterSpec) + "';"
                + "$result = $dlg.ShowDialog($owner);"
                + "if($result -eq [System.Windows.Forms.DialogResult]::OK){[Console]::Out.WriteLine($dlg.FileName)}"
                + "else {[Console]::Out.WriteLine('" + WINDOWS_CANCEL_TOKEN + "')}"
                + "$owner.Close();$owner.Dispose();";

        List<String> lines = runPowerShellChooserScript(script);
        if (lines.isEmpty() || WINDOWS_CANCEL_TOKEN.equals(lines.get(0))) {
            return null;
        }
        return new File(lines.get(0));
    }

    private static List<File> showWindowsNativeOpenMultipleDialog(String title, File initialDirectory,
            FileChooser.ExtensionFilter filter) throws IOException, InterruptedException {
        String initialDir = initialDirectory != null ? initialDirectory.getAbsolutePath() : "";
        String safeTitle = title == null ? "" : title;
        String filterSpec = buildWindowsFilterSpec(filter);

        String script =
                "$ErrorActionPreference='Stop';"
                + "Add-Type -AssemblyName System.Windows.Forms;"
                + "[System.Windows.Forms.Application]::EnableVisualStyles();"
                + "$owner = New-Object System.Windows.Forms.Form;"
                + "$owner.StartPosition='Manual';$owner.Size=New-Object System.Drawing.Size(1,1);"
                + "$owner.Location=New-Object System.Drawing.Point(-32000,-32000);"
                + "$owner.ShowInTaskbar=$false;$owner.TopMost=$true;$owner.Opacity=0;"
                + "$owner.Show();$owner.Activate();"
                + "$dlg = New-Object System.Windows.Forms.OpenFileDialog;"
                + "$dlg.Title='" + escapePsSingleQuoted(safeTitle) + "';"
                + "$dlg.InitialDirectory='" + escapePsSingleQuoted(initialDir) + "';"
                + "$dlg.Multiselect=$true;"
                + "$dlg.Filter='" + escapePsSingleQuoted(filterSpec) + "';"
                + "$result = $dlg.ShowDialog($owner);"
                + "if($result -eq [System.Windows.Forms.DialogResult]::OK){foreach($f in $dlg.FileNames){[Console]::Out.WriteLine($f)}}"
                + "else {[Console]::Out.WriteLine('" + WINDOWS_CANCEL_TOKEN + "')}"
                + "$owner.Close();$owner.Dispose();";

        List<String> lines = runPowerShellChooserScript(script);
        if (lines.isEmpty() || WINDOWS_CANCEL_TOKEN.equals(lines.get(0))) {
            return null;
        }
        List<File> files = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                files.add(new File(line.trim()));
            }
        }
        return files.isEmpty() ? null : files;
    }

    private static String buildWindowsFilterSpec(FileChooser.ExtensionFilter filter) {
        if (filter == null || filter.getExtensions() == null || filter.getExtensions().isEmpty()) {
            return "All Files (*.*)|*.*";
        }
        String description = filter.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "Supported Files";
        }
        List<String> patterns = new ArrayList<>();
        for (String ext : filter.getExtensions()) {
            if (ext == null) {
                continue;
            }
            String p = ext.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (!p.contains("*")) {
                if (p.startsWith(".")) {
                    p = "*" + p;
                } else {
                    p = "*." + p;
                }
            }
            patterns.add(p);
        }
        if (patterns.isEmpty()) {
            return "All Files (*.*)|*.*";
        }
        return description + " (" + String.join(";", patterns) + ")|" + String.join(";", patterns) + "|All Files (*.*)|*.*";
    }

    private static List<String> runPowerShellChooserScript(String script) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("powershell.exe");
        command.add("-NoProfile");
        command.add("-NonInteractive");
        command.add("-STA");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-Command");
        command.add(script);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    outputLines.add(trimmed);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("PowerShell chooser exited with code " + exitCode
                    + (outputLines.isEmpty() ? "" : ": " + outputLines.get(outputLines.size() - 1)));
        }

        return outputLines;
    }

    private static String escapePsSingleQuoted(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replace("'", "''");
    }

    private static void logNativeFallback(String op, String title, Throwable t) {
        String safeTitle = title == null ? "" : title;
        String reason = (t == null) ? "unknown" : (t.getClass().getSimpleName() + ": " + t.getMessage());
        String message = "[FileChooserPlus] Native file dialog failed for " + op
                + " (title='" + safeTitle + "'); falling back to JavaFX chooser. Reason: " + reason;
        System.out.println(message);
        debugLog(message);
        if (DEBUG_NATIVE_FALLBACK && t != null) {
            t.printStackTrace(System.out);
            debugLog("Stacktrace printed to console for: " + t.getClass().getName());
        }
    }

    private static void logNativeDisabled(String op, String title) {
        String safeTitle = title == null ? "" : title;
        String message = "[FileChooserPlus] Native dialogs disabled for " + op
                + " (title='" + safeTitle + "') via -Dglimpse.nativeFileDialog=false; using JavaFX chooser.";
        System.out.println(message);
        debugLog(message);
    }

    private static void debugLog(String message) {
        if (!DEBUG_NATIVE_FLOW || message == null) {
            return;
        }
        try {
            Path logPath = Paths.get(System.getProperty("user.home", "."), "glimpse-filechooser-debug.log");
            String line = "[" + new Date() + "] " + message + System.lineSeparator();
            Files.write(logPath, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging must never interfere with chooser flow.
        }
    }

    private static int[] getOwnerCenterOnFxThread(Window ownerWindow) {
        if (ownerWindow == null) {
            return null;
        }

        try {
            if (Platform.isFxApplicationThread()) {
                return new int[] {
                    (int) Math.round(ownerWindow.getX() + (ownerWindow.getWidth() / 2.0)),
                    (int) Math.round(ownerWindow.getY() + (ownerWindow.getHeight() / 2.0))
                };
            }

            final AtomicReference<int[]> center = new AtomicReference<>();
            final CountDownLatch fxLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    center.set(new int[] {
                        (int) Math.round(ownerWindow.getX() + (ownerWindow.getWidth() / 2.0)),
                        (int) Math.round(ownerWindow.getY() + (ownerWindow.getHeight() / 2.0))
                    });
                } finally {
                    fxLatch.countDown();
                }
            });
            fxLatch.await();
            return center.get();
        } catch (Exception ex) {
            return null;
        }
    }

    private static java.awt.Frame createNativeDialogOwner(int[] ownerCenter) {
        java.awt.Frame frame = new java.awt.Frame();
        frame.setUndecorated(true);
        try {
            frame.setAlwaysOnTop(true);
        } catch (Exception ignored) {
            // Best effort only.
        }
        try {
            frame.setFocusableWindowState(false);
        } catch (Exception ignored) {
            // Best effort only.
        }

        if (ownerCenter != null && ownerCenter.length == 2) {
            frame.setLocation(ownerCenter[0], ownerCenter[1]);
        }

        frame.setSize(1, 1);
        frame.setVisible(true);
        return frame;
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

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return osName.contains("win");
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
