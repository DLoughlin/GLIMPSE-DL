package gui;

import java.nio.file.Path;
import java.nio.file.Paths;

final class ScenarioLibraryPathHelper {

    private ScenarioLibraryPathHelper() {
    }

    static String scenarioDir(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioRootDir, scenarioName).toString();
    }

    static String scenarioConfigFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "configuration_" + scenarioName + ".xml").toString();
    }

    static String scenarioArchiveConfigFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "configuration_" + scenarioName + "_archive.xml").toString();
    }

    static String scenarioMainLogFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "main_log.txt").toString();
    }

    static String scenarioStdoutFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "gcam_stdout.txt").toString();
    }

    static String scenarioErrorFile(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioDir(scenarioRootDir, scenarioName), "main_error.txt").toString();
    }

    static String glimpseRunsFile(String glimpseLogDir) {
        return Paths.get(glimpseLogDir, "Runs.txt").toString();
    }

    static String exeMainLogFile(String executableDir) {
        return Paths.get(executableDir, "logs", "main_log.txt").toString();
    }

    static Path exeMainLogPath(String executableDir) {
        return Paths.get(executableDir, "logs", "main_log.txt");
    }

    static Path scenarioMainLogPath(String scenarioRootDir, String scenarioName) {
        return Paths.get(scenarioRootDir, scenarioName, "main_log.txt");
    }
}
