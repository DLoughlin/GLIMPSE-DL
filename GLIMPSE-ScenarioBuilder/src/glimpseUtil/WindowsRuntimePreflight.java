package glimpseUtil;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Windows-only runtime checks for required native dependencies.
 */
public final class WindowsRuntimePreflight {
    private static final String[] REQUIRED_DLLS = {"vcruntime140.dll", "msvcp140.dll"};
    private static final String[] OPTIONAL_DLLS = {"vcruntime140_1.dll"};
    private static final String VC_REDIST_URL = "https://aka.ms/vs/17/release/vc_redist.x64.exe";

    private static volatile boolean checked = false;
    private static volatile boolean available = true;
    private static volatile boolean warningShown = false;
    private static volatile boolean proceedDespiteMissing = false;
    private static volatile boolean declinedProceed = false;
    private static volatile String lastMissingDlls = "";

    private WindowsRuntimePreflight() {
        // Utility class
    }

    /**
     * Indicates whether the current JVM is running on Windows.
     *
     * @return {@code true} when the host operating system is Windows
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().startsWith("windows");
    }

    /**
     * Verifies that the required Microsoft Visual C++ runtime is available and,
     * when missing, optionally warns the user before allowing execution to continue.
     *
     * @param utils utility facade used to show warning UI when needed
     * @param contextLabel short label describing the action being attempted
     * @return {@code true} when execution may proceed
     */
    public static boolean ensureMsvcRuntimeAvailableOrWarn(GLIMPSEUtils utils, String contextLabel) {
        if (!isWindows()) {
            return true;
        }
        if (checkMsvcRuntimeAvailable()) {
            return true;
        }
        if (proceedDespiteMissing) {
            return true;
        }
        if (declinedProceed) {
            return false;
        }
        if (!warningShown && utils != null) {
            warningShown = true;
            boolean allowProceed = promptForMissingRuntime(utils, contextLabel);
            if (allowProceed) {
                proceedDespiteMissing = true;
            } else {
                declinedProceed = true;
            }
        }
        return proceedDespiteMissing;
    }

    /**
     * Performs a one-time check for the required Microsoft Visual C++ runtime DLLs.
     *
     * @return {@code true} when all required runtime files were found
     */
    public static boolean checkMsvcRuntimeAvailable() {
        if (!isWindows()) {
            return true;
        }
        if (checked) {
            return available;
        }
        available = detectMsvcRuntime();
        checked = true;
        return available;
    }

    /**
     * Builds the explanatory message shown when the Windows VC++ runtime appears
     * to be missing.
     *
     * @param contextLabel optional label describing the operation being blocked
     * @return multi-line warning message for the user
     */
    public static String buildMissingRuntimeMessage(String contextLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append("Microsoft Visual C++ Runtime appears to be missing on Windows.");
        if (contextLabel != null && contextLabel.trim().length() > 0) {
            sb.append(" Context: ").append(contextLabel).append(".");
        }
        sb.append("\n\nGCAM may fail to start and the console window can close immediately.");
        if (lastMissingDlls != null && lastMissingDlls.trim().length() > 0) {
            sb.append("\n\nMissing DLL(s): ").append(lastMissingDlls);
        }
        sb.append("\nSearched: %WINDIR%\\System32, %WINDIR%\\SysWOW64, and PATH.");
        sb.append("\n\nInstall: Microsoft Visual C++ 2015-2022 Redistributable (x64).");
        sb.append("\n").append(VC_REDIST_URL);
        return sb.toString();
    }

    private static boolean promptForMissingRuntime(GLIMPSEUtils utils, String contextLabel) {
        final String message = buildMissingRuntimeMessage(contextLabel);
        if (Platform.isFxApplicationThread()) {
            return showPromptDialog(utils, message);
        }
        final boolean[] resultHolder = new boolean[] { false };
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                resultHolder[0] = showPromptDialog(utils, message);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return resultHolder[0];
    }

    private static boolean showPromptDialog(GLIMPSEUtils utils, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Missing Microsoft Visual C++ Runtime");
        alert.setHeaderText("Required Windows runtime not found");
        alert.setContentText(message);

        ButtonType installBtn = new ButtonType("Get VC++ Runtime", ButtonBar.ButtonData.OTHER);
        ButtonType proceedBtn = new ButtonType("Proceed anyway", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(installBtn, proceedBtn, cancelBtn);

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == installBtn) {
            openUrlBestEffort(VC_REDIST_URL, utils);
            return false;
        }
        if (response.isPresent() && response.get() == proceedBtn) {
            return true;
        }
        return false;
    }

    private static void openUrlBestEffort(String url, GLIMPSEUtils utils) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            if (utils != null) {
                utils.warningMessage("Problem trying to open the web page: " + url);
            }
            System.err.println("Error trying to open web page: " + e);
        }
    }

    private static boolean detectMsvcRuntime() {
        List<Path> searchDirs = getSearchDirs();
        List<String> missing = new ArrayList<>();
        for (String dll : REQUIRED_DLLS) {
            if (!dllPresentInDirs(dll, searchDirs)) {
                missing.add(dll);
            }
        }

        if (!missing.isEmpty()) {
            lastMissingDlls = String.join(", ", missing);
            return false;
        }

        // Optional DLLs are checked for extra diagnostics but do not block startup/run.
        List<String> optionalMissing = new ArrayList<>();
        for (String dll : OPTIONAL_DLLS) {
            if (!dllPresentInDirs(dll, searchDirs)) {
                optionalMissing.add(dll);
            }
        }
        if (!optionalMissing.isEmpty()) {
            lastMissingDlls = String.join(", ", optionalMissing);
        }

        return true;
    }

    private static List<Path> getSearchDirs() {
        List<Path> dirs = new ArrayList<>();
        String windir = System.getenv("WINDIR");
        if (windir != null && windir.trim().length() > 0) {
            dirs.add(Paths.get(windir, "System32"));
            dirs.add(Paths.get(windir, "SysWOW64"));
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && pathEnv.trim().length() > 0) {
            String[] parts = pathEnv.split(";");
            for (String part : parts) {
                if (part != null && part.trim().length() > 0) {
                    dirs.add(Paths.get(part.trim()));
                }
            }
        }
        return dirs;
    }

    private static boolean dllPresentInDirs(String dllName, List<Path> dirs) {
        if (dllName == null || dllName.trim().length() == 0) {
            return false;
        }
        for (Path dir : dirs) {
            try {
                Path candidate = dir.resolve(dllName);
                if (Files.isRegularFile(candidate)) {
                    return true;
                }
            } catch (Exception ignored) {
                // Ignore invalid paths
            }
        }
        return false;
    }

    /**
     * Returns the runtime DLL names that are required for startup checks.
     *
     * @return list of required DLL file names
     */
    public static List<String> getRequiredDlls() {
        return Arrays.asList(REQUIRED_DLLS);
    }
}