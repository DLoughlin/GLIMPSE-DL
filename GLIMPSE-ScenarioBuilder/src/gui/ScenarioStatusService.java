package gui;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

final class ScenarioStatusService {
    static final String STATUS_IN_QUEUE = "In queue";
    static final String STATUS_SUCCESS = "Success";
    static final String STATUS_DNF = "DNF";
    static final String STATUS_UNSOLVED = "Unsolved mkts";
    static final String STATUS_RUNNING = "Running";
    static final String STATUS_LOST_HANDLE = "Lost handle";
    static final String STATUS_BLOCKED = "Blocked";
    static final String STATUS_WRITING = "Writing";

    private static final long LOST_HANDLE_GRACE_MS = 30_000L;
    private static final String[] STDOUT_SUCCESS_MARKERS = {
            "Model exiting successfully.",
            "Exiting successfully.",
            "Model run completed.",
            "Finished printing output."
    };

    private final GLIMPSEVariables vars;
    private final GLIMPSEFiles files;
    private final GLIMPSEUtils utils;
    private final GcamPromptMonitor gcamPromptMonitor;

    ScenarioStatusService(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
        this.vars = vars;
        this.files = files;
        this.utils = utils;
        this.gcamPromptMonitor = new GcamPromptMonitor(new GcamRunController(), files, null);
    }

    ScenarioStatusRefreshResult refresh(RefreshRequest request) {
        RefreshRequest safeRequest = request == null ? RefreshRequest.empty() : request;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);
        File currentMainLogFile = new File(ScenarioLibraryPathHelper.exeMainLogFile(vars.getgCamExecutableDir()));
        String runningScenario = utils.getRunningScenario(currentMainLogFile);
        List<ScenarioStatusSnapshot> snapshots = new ArrayList<>();
        List<String> updatedQueue = new ArrayList<>(safeRequest.queuedRuns);
        List<String> completedRunsToAdd = new ArrayList<>();
        boolean noScenarios = false;

        try {
            File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
            if (scenarioFolders == null) {
                noScenarios = true;
            } else {
                for (File scenarioFolder : scenarioFolders) {
                    ScenarioStatusSnapshot snapshot = buildScenarioStatusSnapshot(
                            scenarioFolder,
                            currentMainLogFile,
                            runningScenario,
                            safeRequest,
                            updatedQueue,
                            completedRunsToAdd,
                            format);
                    if (snapshot != null) {
                        snapshots.add(snapshot);
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Problem updating scenario table", ex);
        }

        return new ScenarioStatusRefreshResult(snapshots, noScenarios, runningScenario, updatedQueue, completedRunsToAdd);
    }

    private ScenarioStatusSnapshot buildScenarioStatusSnapshot(File scenarioFolder, File currentMainLogFile,
            String runningScenario, RefreshRequest request, List<String> queuedRuns, List<String> completedRunsToAdd,
            DateFormat format) {
        ArrayList<String> searchArray = new ArrayList<>();
        searchArray.add("Model run completed.");
        searchArray.add("Data Readin, Model Run & Write Time:");
        searchArray.add("The following model periods did not solve:");

        long createdDate = 0L;
        long completedDate = 0L;
        String scenarioName = scenarioFolder.getName();
        String configName = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
        File configFile = new File(configName);
        if (!configFile.exists()) {
            return null;
        }

        String components = ScenarioLibraryReportHelper.getComponentsFromConfig(configFile);
        String mainLogName = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
        File mainLogFile = new File(mainLogName);
        boolean mainLogExists = mainLogFile.exists();
        String status = "";
        String runtime = "";
        String unsolved = "";
        createdDate = configFile.lastModified();

        if (mainLogExists) {
            completedDate = mainLogFile.lastModified();
            searchArray = files.getMatchingTextArrayInFile(mainLogName, searchArray);
            if (!searchArray.get(0).isEmpty()) {
                status = STATUS_SUCCESS;
            } else {
                status = STATUS_DNF;
                String runningStatus = utils.getScenarioStatusFromMainLog(mainLogFile);
                if (runningStatus.contains(",ERR")) {
                    String errorStr = runningStatus.substring(runningStatus.indexOf(',') + 4);
                    unsolved = errorStr;
                }
            }
            for (int i = 0; i < queuedRuns.size(); i++) {
                String queuedScenario = queuedRuns.get(i);
                if (queuedScenario.equals(scenarioName)) {
                    status = STATUS_IN_QUEUE;
                    if (mainLogExists) {
                        completedRunsToAdd.add(queuedScenario);
                        queuedRuns.remove(i);
                    }
                    break;
                }
            }
        }

        if (!STATUS_SUCCESS.equals(status) && hasStdoutSuccessMarker(scenarioFolder)) {
            status = STATUS_SUCCESS;
        }
        if (!searchArray.get(1).isEmpty()) {
            runtime = toRuntimeText(searchArray.get(1));
        }
        if (!searchArray.get(2).isEmpty()) {
            try {
                unsolved = searchArray.get(2).split(":")[1].trim();
                status = STATUS_UNSOLVED;
            } catch (Exception e) {
                unsolved = "";
            }
        }

        String createdDateStr = createdDate != 0L ? format.format(createdDate) : "";
        String completedDateStr = completedDate != 0L ? format.format(completedDate) : "";
        if ((!STATUS_SUCCESS.equals(status)) && (!STATUS_UNSOLVED.equals(status)) && (!STATUS_DNF.equals(status))) {
            if (scenarioName.equals(runningScenario)) {
                status = STATUS_RUNNING;
                long lastDate = currentMainLogFile.lastModified();
                boolean isQueued = queuedRuns.contains(scenarioName);
                if (!isQueued && (request.startupTime > 0) && (System.currentTimeMillis() - request.startupTime > LOST_HANDLE_GRACE_MS)
                        && lastDate < request.startupTime) {
                    status = STATUS_LOST_HANDLE;
                } else {
                    String runningStatus = utils.getScenarioStatusFromMainLog(currentMainLogFile);
                    String explicitRunState = getExplicitRunStateLabel(scenarioName, currentMainLogFile, runningStatus, request);
                    if (!explicitRunState.isEmpty()) {
                        status = explicitRunState;
                    } else if (runningStatus.contains(",ERR")) {
                        String temp = runningStatus.substring(0, runningStatus.indexOf(","));
                        status = status + "(" + temp + ")";
                        String errorStr = runningStatus.substring(runningStatus.indexOf(",") + 4);
                        unsolved = errorStr;
                    } else {
                        String temp = runningStatus;
                        if (!temp.isEmpty()) {
                            status = status + "(" + temp + ")";
                        }
                    }
                }
            } else if (queuedRuns.contains(scenarioName)) {
                status = STATUS_IN_QUEUE;
            }
        }
        if ((status.isEmpty() || status.startsWith(STATUS_RUNNING) || STATUS_BLOCKED.equals(status)
                || STATUS_WRITING.equals(status))
                && scenarioName.equals(request.stopRequestedScenarioName) && !request.isScenarioActivelyRunning(scenarioName)) {
            status = STATUS_DNF;
        }

        return new ScenarioStatusSnapshot(scenarioName, components, createdDateStr, completedDateStr, status, runtime, unsolved);
    }

    private String getExplicitRunStateLabel(String scenarioName, File currentMainLogFile, String runningStatus,
            RefreshRequest request) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return "";
        }
        if (scenarioName.equals(request.stopRequestedScenarioName) && !request.isScenarioActivelyRunning(scenarioName)) {
            return STATUS_DNF;
        }
        if (request.currentGcamScenarioName == null || !scenarioName.equals(request.currentGcamScenarioName)) {
            return "";
        }
        if (request.gcamPromptDialogActive || request.databasePromptAwaitingReset) {
            return STATUS_BLOCKED;
        }
        if (isWritingResultsPhase(currentMainLogFile, runningStatus)) {
            return STATUS_WRITING;
        }
        return "";
    }

    private boolean isWritingResultsPhase(File currentMainLogFile, String runningStatus) {
        return gcamPromptMonitor.isWritingResultsPhase(currentMainLogFile, runningStatus);
    }

    private boolean hasStdoutSuccessMarker(File scenarioFolder) {
        if (scenarioFolder == null) {
            return false;
        }
        try {
            File stdoutFile = new File(scenarioFolder, "gcam_stdout.txt");
            if (!stdoutFile.exists()) {
                return false;
            }
            ArrayList<String> lines = files.getStringArrayFromFile(stdoutFile.getAbsolutePath(), "#");
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line == null) {
                    continue;
                }
                for (String marker : STDOUT_SUCCESS_MARKERS) {
                    if (line.contains(marker)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String toRuntimeText(String runtimeLine) {
        String runtime = "";
        try {
            runtime = runtimeLine.split(":")[1].trim();
        } catch (Exception e) {
            runtime = "";
        }
        runtime = runtime.replace("seconds.", "").trim();
        try {
            int totalSecs = (int) Math.round(Float.parseFloat(runtime));
            int hours = (totalSecs - totalSecs % 3600) / 3600;
            int minutes = (totalSecs % 3600 - totalSecs % 3600 % 60) / 60;
            return hours + " hr " + minutes + " min ";
        } catch (Exception e) {
            return runtime;
        }
    }

    static final class RefreshRequest {
        final List<String> queuedRuns;
        final long startupTime;
        final String currentGcamScenarioName;
        final String stopRequestedScenarioName;
        final boolean gcamPromptDialogActive;
        final boolean databasePromptAwaitingReset;
        final boolean scenarioActivelyRunning;

        RefreshRequest(List<String> queuedRuns, long startupTime, String currentGcamScenarioName,
                String stopRequestedScenarioName, boolean gcamPromptDialogActive,
                boolean databasePromptAwaitingReset, boolean scenarioActivelyRunning) {
            this.queuedRuns = queuedRuns == null ? new ArrayList<>() : new ArrayList<>(queuedRuns);
            this.startupTime = startupTime;
            this.currentGcamScenarioName = currentGcamScenarioName == null ? "" : currentGcamScenarioName;
            this.stopRequestedScenarioName = stopRequestedScenarioName == null ? "" : stopRequestedScenarioName;
            this.gcamPromptDialogActive = gcamPromptDialogActive;
            this.databasePromptAwaitingReset = databasePromptAwaitingReset;
            this.scenarioActivelyRunning = scenarioActivelyRunning;
        }

        static RefreshRequest empty() {
            return new RefreshRequest(new ArrayList<>(), 0L, "", "", false, false, false);
        }

        boolean isScenarioActivelyRunning(String scenarioName) {
            return scenarioActivelyRunning
                    && scenarioName != null
                    && !scenarioName.trim().isEmpty()
                    && scenarioName.equals(currentGcamScenarioName);
        }
    }
}
