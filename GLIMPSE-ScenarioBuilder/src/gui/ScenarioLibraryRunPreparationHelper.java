package gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import glimpseElement.ScenarioRow;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

final class ScenarioLibraryRunPreparationHelper {

    private final GLIMPSEVariables vars;
    private final GLIMPSEFiles files;
    private final GLIMPSEUtils utils;

    ScenarioLibraryRunPreparationHelper(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
        this.vars = vars;
        this.files = files;
        this.utils = utils;
    }

    RunPreparationResult prepareSelectedRuns(ScenarioSelection selection, RunPreparationCallbacks callbacks) {
        List<String> configFiles = new ArrayList<>();
        if (selection == null || callbacks == null) {
            return new RunPreparationResult(configFiles);
        }
        for (ScenarioRow row : selection.getRows()) {
            PreparedRun preparedRun = prepareRun(row, callbacks);
            if (preparedRun.shouldLaunch()) {
                configFiles.add(preparedRun.getConfigFile());
            }
        }
        return new RunPreparationResult(configFiles);
    }

    private PreparedRun prepareRun(ScenarioRow row, RunPreparationCallbacks callbacks) {
        if (row == null) {
            return PreparedRun.skip();
        }
        String scenarioName = ScenarioSelection.normalizeScenarioName(row);
        if (scenarioName.isEmpty()) {
            return PreparedRun.skip();
        }

        row.setCreatedDate(new Date());
        if (!confirmRunWithExistingMainLog(scenarioName)) {
            return PreparedRun.skip();
        }

        callbacks.clearScenarioRunStatusFields(scenarioName);
        cleanupPriorRunLogs(scenarioName);

        String configFile = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
        callbacks.markQueued(scenarioName);
        row.setStatus("In queue");

        String launchConfig = maybeUseArchivedConfig(row, scenarioName, configFile);
        return PreparedRun.launch(scenarioName, launchConfig);
    }

    private boolean confirmRunWithExistingMainLog(String scenarioName) {
        String mainLogFile = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
        if (!files.doesFileExist(mainLogFile)) {
            return true;
        }
        return utils.selectYesOrNoDialog("main_log.txt exists for " + scenarioName + ". Run anyway?");
    }

    private void cleanupPriorRunLogs(String scenarioName) {
        String mainLogFile = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
        files.deleteFile(mainLogFile);
        try {
            files.deleteFile(ScenarioLibraryPathHelper.scenarioStdoutFile(vars.getScenarioDir(), scenarioName));
            files.deleteFile(ScenarioLibraryPathHelper.scenarioErrorFile(vars.getScenarioDir(), scenarioName));
        } catch (Exception ignored) {
        }
    }

    private String maybeUseArchivedConfig(ScenarioRow row, String scenarioName, String defaultConfigFile) {
        try {
            String archiveConfigFilename = ScenarioLibraryPathHelper.scenarioArchiveConfigFile(vars.getScenarioDir(), scenarioName);
            File archiveConfigFile = new File(archiveConfigFilename);
            if (archiveConfigFile.exists() && utils.selectYesOrNoDialog("Run " + scenarioName + " from archive?")) {
                row.setCreatedDate(new Date(archiveConfigFile.lastModified()));
                return archiveConfigFilename;
            }
        } catch (Exception e) {
            System.out.println("Problem checking on existence of archive. Attempting to continue from non-archived files.");
        }
        return defaultConfigFile;
    }

    interface RunPreparationCallbacks {
        void clearScenarioRunStatusFields(String scenarioName);
        void markQueued(String scenarioName);
    }

    static final class RunPreparationResult {
        private final List<String> configFiles;

        private RunPreparationResult(List<String> configFiles) {
            this.configFiles = configFiles == null ? new ArrayList<>() : new ArrayList<>(configFiles);
        }

        String[] getConfigFiles() {
            return configFiles.toArray(new String[0]);
        }

        boolean hasLaunchableRuns() {
            return !configFiles.isEmpty();
        }
    }

    private static final class PreparedRun {
        private final String scenarioName;
        private final String configFile;

        private PreparedRun(String scenarioName, String configFile) {
            this.scenarioName = scenarioName == null ? "" : scenarioName;
            this.configFile = configFile == null ? "" : configFile;
        }

        static PreparedRun skip() {
            return new PreparedRun("", "");
        }

        static PreparedRun launch(String scenarioName, String configFile) {
            return new PreparedRun(scenarioName, configFile);
        }

        boolean shouldLaunch() {
            return !scenarioName.trim().isEmpty() && !configFile.trim().isEmpty();
        }

        String getConfigFile() {
            return configFile;
        }
    }
}