package gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import glimpseElement.ScenarioRow;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

/**
 * Service for scenario file-system operations triggered by Scenario Library actions.
 * <p>
 * This class encapsulates path resolution, open/view helpers, import behavior, archiving,
 * and delete-to-trash operations so UI code can stay focused on interaction flow.
 */
final class ScenarioFileActionService {

    private final GLIMPSEVariables vars;
    private final GLIMPSEFiles files;
    private final GLIMPSEUtils utils;

    ScenarioFileActionService(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
        this.vars = vars;
        this.files = files;
        this.utils = utils;
    }

    /** Returns config XML paths for the selected scenarios. */
    List<String> getScenarioConfigPaths(ScenarioSelection selection) {
        List<String> paths = new ArrayList<>();
        if (selection == null) {
            return paths;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            if (!scenarioName.isEmpty()) {
                paths.add(ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName));
            }
        }
        return paths;
    }

    /** Returns main log paths for the selected scenarios. */
    List<String> getScenarioMainLogPaths(ScenarioSelection selection) {
        List<String> paths = new ArrayList<>();
        if (selection == null) {
            return paths;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            if (!scenarioName.isEmpty()) {
                paths.add(ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName));
            }
        }
        return paths;
    }

    /** Returns folder paths for the selected scenarios. */
    List<String> getScenarioFolderPaths(ScenarioSelection selection) {
        List<String> paths = new ArrayList<>();
        if (selection == null) {
            return paths;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            if (!scenarioName.isEmpty()) {
                paths.add(ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName));
            }
        }
        return paths;
    }

    /** Returns the executable-level main log path. */
    String getExeMainLogPath() {
        return ScenarioLibraryPathHelper.exeMainLogFile(vars.getgCamExecutableDir());
    }

    /** Returns true if any selected scenario main log is missing (and opens the missing path). */
    boolean warnIfAnyScenarioMainLogMissing(ScenarioSelection selection) {
        for (String logPath : getScenarioMainLogPaths(selection)) {
            if (!files.doesFileExist(logPath)) {
                files.showFileInTextEditor(logPath);
                return true;
            }
        }
        return false;
    }

    /** Opens selected scenario folders in the system file explorer. */
    void openScenarioFolders(ScenarioSelection selection) {
        for (String folderPath : getScenarioFolderPaths(selection)) {
            files.openFileExplorer(folderPath);
        }
    }

    /** Opens selected scenario config files in the configured text editor. */
    void openScenarioConfigs(ScenarioSelection selection) {
        for (String configPath : getScenarioConfigPaths(selection)) {
            files.showFileInTextEditor(configPath);
        }
    }

    /** Opens selected scenario main logs in the configured text editor. */
    void openScenarioLogs(ScenarioSelection selection) {
        for (String logPath : getScenarioMainLogPaths(selection)) {
            files.showFileInTextEditor(logPath);
        }
    }

    /** Opens the executable-level main log in the configured text editor. */
    void openExeLog() {
        files.showFileInTextEditor(getExeMainLogPath());
    }

    /** Imports an external scenario config into the scenario library area. */
    ImportResult importScenarioConfig(File newConfigFile) {
        if (newConfigFile == null) {
            return ImportResult.cancelled();
        }
        String str = files.searchForTextInFileS(newConfigFile, "scenarioName", "<!--");
        String scenarioName = utils.getStringBetweenCharSequences(str, ">", "</");
        String workingScenarioLog = ScenarioLibraryPathHelper.glimpseRunsFile(vars.getGlimpseLogDir());
        File workingScenariosFile = new File(workingScenarioLog);
        boolean scenarioExists = files.searchForTextAtStartOfLinesInFile(workingScenariosFile, scenarioName + ",", "#");
        String newScenarioFolder = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName);
        String newScenarioConfig = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
        File scenarioFolder = new File(newScenarioFolder);
        if (!scenarioFolder.exists()) {
            scenarioFolder.mkdir();
        }
        files.copyFile(newConfigFile.getAbsolutePath(), newScenarioConfig);

        ScenarioRow row = new ScenarioRow(scenarioName);
        row.setComponents("Externally-created scenario");
        row.setCreatedDate(new Date());
        row.setStatus("No");
        return new ImportResult(row, scenarioName, scenarioExists);
    }

    /** Creates or refreshes archive artifacts for selected scenarios. */
    void archiveScenarios(ScenarioSelection selection) {
        if (selection == null) {
            return;
        }
        for (String scenarioName : selection.getScenarioNames()) {
            if (scenarioName.isEmpty()) {
                continue;
            }
            String workingDir = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName);
            String configFilename = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
            String archiveConfigFilename = ScenarioLibraryPathHelper.scenarioArchiveConfigFile(vars.getScenarioDir(), scenarioName);
            archiveScenario(vars.getgCamExecutableDir(), workingDir, archiveConfigFilename, configFilename, scenarioName);
        }
    }

    /** Moves selected scenarios into the trash area and returns deleted rows. */
    List<ScenarioRow> deleteScenarios(ScenarioSelection selection) throws IOException {
        List<ScenarioRow> deletedRows = new ArrayList<>();
        if (selection == null) {
            return deletedRows;
        }
        for (ScenarioRow row : selection.getRows()) {
            if (row == null) {
                continue;
            }
            String scenarioName = ScenarioSelection.normalizeScenarioName(row);
            if (scenarioName.isEmpty()) {
                continue;
            }
            String xmlDir = ScenarioLibraryPathHelper.scenarioDir(vars.getScenarioDir(), scenarioName);
            String trashDirFolder = ScenarioLibraryPathHelper.scenarioDir(vars.getTrashDir(), scenarioName);
            File trashDir = new File(trashDirFolder);
            if (trashDir.exists()) {
                files.deleteDirectoryStream(trashDir.toPath());
            }
            if (!trashDir.exists()) {
                trashDir.mkdirs();
            }
            Files.move(Paths.get(xmlDir), Paths.get(trashDirFolder), StandardCopyOption.REPLACE_EXISTING);
            deletedRows.add(row);
        }
        return deletedRows;
    }

    private void archiveScenario(String exeDir, String workingDir, String archiveConfigFilename, String configFilename, String scenarioName) {
        ArrayList<String> configContent = files.getStringArrayFromFile(configFilename, "#");
        ArrayList<String> updatedConfigContent = new ArrayList<>();
        boolean inScenarioComponents = false;
        String archiveFolderName = workingDir + File.separator + "archive";
        File archiveFolder = new File(archiveFolderName);
        if (archiveFolder.exists()) {
            String msg = "Archive already exists. Replace?";
            if (!utils.selectYesOrNoDialog(msg)) {
                return;
            }
            File[] archiveFiles = archiveFolder.listFiles();
            if (archiveFiles != null) {
                for (File file : archiveFiles) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            }
        }
        for (String line : configContent) {
            if (line.indexOf("<ScenarioComponents>") >= 0) {
                inScenarioComponents = true;
            }
            if (line.indexOf("</ScenarioComponents>") >= 0) {
                inScenarioComponents = false;
            }
            if (inScenarioComponents && line.indexOf("Value") >= 0) {
                int startIndex = line.indexOf('>') + 1;
                int endIndex = line.lastIndexOf('<');
                String originalPath = line.substring(startIndex, endIndex);
                Path sourcePath = Paths.get(exeDir).resolve(Paths.get(originalPath)).normalize();
                File destFile = new File(workingDir + File.separator + "archive" + File.separator + sourcePath.getFileName());
                line = line.replace(originalPath, destFile.getAbsolutePath());
                if (destFile.exists()) {
                    utils.warningMessage("Multiple files named " + sourcePath.getFileName() + ". Keeping last.");
                    destFile.delete();
                }
                File parentFile = destFile.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                try {
                    Files.copy(sourcePath, destFile.toPath());
                } catch (IOException e) {
                    System.out.println("Error during archiving:");
                    e.printStackTrace();
                }
            }
            updatedConfigContent.add(line);
        }
        files.saveFile(updatedConfigContent, archiveConfigFilename);
        files.saveFile(updatedConfigContent, archiveFolder + File.separator + "configuration_" + scenarioName + "_archive.xml");
        File zipDir = new File(workingDir + File.separator + "archive");
        String zipFilename = workingDir + File.separator + "archive" + utils.getCurrentTimeStamp() + ".zip";
        File zipFile = new File(zipFilename);
        if (zipFile.exists()) {
            files.deleteDirectory(zipFile);
        }
        files.zipDirectory(zipDir, zipFilename);
        System.out.println("Done archiving.");
    }

    /**
     * Immutable result describing the outcome of a scenario-config import.
     */
    static final class ImportResult {
        private final ScenarioRow scenarioRow;
        private final String scenarioName;
        private final boolean overwroteExisting;

        private ImportResult(ScenarioRow scenarioRow, String scenarioName, boolean overwroteExisting) {
            this.scenarioRow = scenarioRow;
            this.scenarioName = scenarioName == null ? "" : scenarioName;
            this.overwroteExisting = overwroteExisting;
        }

        /** Returns a cancelled import result sentinel. */
        static ImportResult cancelled() {
            return new ImportResult(null, "", false);
        }

        /** Returns the imported scenario row, or {@code null} when import did not occur. */
        ScenarioRow getScenarioRow() {
            return scenarioRow;
        }

        /** Returns the imported scenario name (empty when not imported). */
        String getScenarioName() {
            return scenarioName;
        }

        /** Returns whether the import replaced an existing scenario entry. */
        boolean overwroteExisting() {
            return overwroteExisting;
        }

        /** Returns whether a scenario was actually imported. */
        boolean wasImported() {
            return scenarioRow != null && !scenarioName.isEmpty();
        }
    }
}