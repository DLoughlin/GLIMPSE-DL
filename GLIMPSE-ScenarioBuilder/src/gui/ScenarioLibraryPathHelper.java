package gui;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path-construction helpers for Scenario Library folders, configs, and log artifacts.
 * <p>
 * Centralizing these builders keeps filename conventions consistent across UI actions,
 * status refresh logic, and report generation.
 */
final class ScenarioLibraryPathHelper {

    private ScenarioLibraryPathHelper() {
    }

    /** Returns the absolute directory path for a scenario under the scenario root. */
    static String scenarioDir(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioRootDir, scenarioName).toString();
    }

    /** Returns the standard scenario configuration XML path. */
    static String scenarioConfigFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "configuration_" + scenarioName + ".xml").toString();
    }

    /** Returns the archive configuration XML path for a scenario. */
    static String scenarioArchiveConfigFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "configuration_" + scenarioName + "_archive.xml").toString();
    }

    /** Returns the main log path inside a scenario folder. */
    static String scenarioMainLogFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "main_log.txt").toString();
    }

    /** Returns the GCAM stdout path inside a scenario folder. */
    static String scenarioStdoutFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "gcam_stdout.txt").toString();
    }

    /** Returns the scenario error file path inside a scenario folder. */
    static String scenarioErrorFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "main_error.txt").toString();
    }

    /** Returns the session run ledger file (`Runs.txt`) in the GLIMPSE log directory. */
    static String glimpseRunsFile(String glimpseLogDir) {
        return Paths.get(glimpseLogDir, "Runs.txt").toString();
    }

    /** Returns the executable-level `main_log.txt` path under the executable `logs` folder. */
    static String exeMainLogFile(String executableDir) {
        return Paths.get(executableDir, "logs", "main_log.txt").toString();
    }

    /** Returns the executable-level `main_log.txt` path as a {@link Path}. */
    static Path exeMainLogPath(String executableDir) {
        return Paths.get(executableDir, "logs", "main_log.txt");
    }

    /** Returns a scenario `main_log.txt` path as a {@link Path}. */
    static Path scenarioMainLogPath(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioRootDir, scenarioName, "main_log.txt");
    }
}
