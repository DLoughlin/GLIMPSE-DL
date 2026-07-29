package gui;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

/**
 * Computes scenario status snapshots from scenario folders, run logs, and run-controller state.
 * <p>
 * The service parses scenario/executable logs, derives run status labels, reconciles queue state,
 * and emits immutable snapshot rows for Scenario Library rendering.
 */
final class ScenarioStatusService {
    static final String STATUS_IN_QUEUE = "In queue";
    static final String STATUS_SUCCESS = "Success";
    static final String STATUS_DNF = "DNF";
    static final String STATUS_STOPPED = "Stopped";
    static final String STATUS_UNSOLVED = "Unsolved mkts";
    static final String STATUS_RUNNING = "Running";
    static final String STATUS_LOST_HANDLE = "Lost handle";
    static final String STATUS_BLOCKED = "Blocked";
    static final String STATUS_WRITING = "Writing";

    private static final long LOST_HANDLE_GRACE_MS = 30_000L;
    private static final int LOG_TAIL_BYTES = 128 * 1024;
    private static final String CONFIG_FILE_PREFIX = "Configuration file:";
    private static final String RUNTIME_PREFIX = "Data Readin, Model Run & Write Time:";
    private static final String UNSOLVED_PREFIX = "The following model periods did not solve:";
    private static final String ERROR_PREFIX = "ERROR";
    private static final java.util.regex.Pattern RUNNING_PERIOD_WITH_YEAR_PATTERN = java.util.regex.Pattern.compile(
            "^period\\s+(\\d{1,3})\\s*:\\s*(\\d{4})(?:\\D|$)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern UNSOLVED_PERIOD_ERROR_PATTERN = java.util.regex.Pattern.compile(
            "did\\s+not\\s+solve\\s+periods?\\s*[:=]?\\s*([0-9]{1,3}(?:\\s*(?:,|and|&)\\s*[0-9]{1,3})*)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern UNSOLVED_PERIOD_NUMBER_PATTERN = java.util.regex.Pattern.compile("\\d{1,3}");
    private static final String COMPONENTS_HEADER = "Components:";
    private static final String METADATA_HEADER = "##################### Scenario Meta Data #####################";
    private static final String METADATA_FOOTER = "###############################################################";
    private static final String FILES_HEADER = "<Files>";
    private static final String EXTERNALLY_CREATED_SCENARIO = "Externally-created scenario";
    private static final String STOPPED_MARKER = "GLIMPSE scenario status: Stopped";
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
    private final Set<String> writingPhaseScenarios = new HashSet<>();
    private final Map<String, CachedConfigMetadata> configMetadataCache = new HashMap<>();
    private final Map<String, CachedLogAnalysis> logAnalysisCache = new HashMap<>();

    /** Creates a status service bound to the shared GLIMPSE utility singletons. */
    ScenarioStatusService(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
        this.vars = vars;
        this.files = files;
        this.utils = utils;
        this.gcamPromptMonitor = new GcamPromptMonitor(new GcamRunController(), files, null);
    }

    /**
     * Rebuilds status rows for all scenario folders and returns the refresh output.
     *
     * @param request contextual run-state input captured from the UI/controller
     * @return refreshed snapshots plus derived running/queue metadata
     */
    ScenarioStatusRefreshResult refresh(RefreshRequest request) {
        RefreshRequest safeRequest = request == null ? RefreshRequest.empty() : request;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd: HH:mm", Locale.ENGLISH);
        File currentMainLogFile = new File(ScenarioLibraryPathHelper.exeMainLogFile(vars.getgCamExecutableDir()));
        LogAnalysis currentMainLogAnalysis = analyzeCurrentMainLogFile(currentMainLogFile, safeRequest);
        String runningScenario = resolveRunningScenario(currentMainLogAnalysis, safeRequest);
        List<ScenarioStatusSnapshot> snapshots = new ArrayList<>();
        List<String> updatedQueue = new ArrayList<>(safeRequest.queuedRuns);
        removeScenarioFromQueue(updatedQueue, runningScenario);
        boolean noScenarios = false;

        try {
            File[] scenarioFolders = new File(vars.getScenarioDir()).listFiles(File::isDirectory);
            if (scenarioFolders == null) {
                noScenarios = true;
            } else {
                Arrays.sort(scenarioFolders, Comparator.comparingLong(this::getScenarioConfigCreatedTime)
                        .thenComparing(folder -> folder == null ? "" : folder.getName(), String.CASE_INSENSITIVE_ORDER)
                        .reversed());
                for (File scenarioFolder : scenarioFolders) {
                    ScenarioStatusSnapshot snapshot = buildScenarioStatusSnapshot(
                            scenarioFolder,
                            currentMainLogFile,
                            currentMainLogAnalysis,
                            runningScenario,
                            safeRequest,
                            updatedQueue,
                            format);
                    if (snapshot != null) {
                        snapshots.add(snapshot);
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Problem updating scenario table", ex);
        }

        return new ScenarioStatusRefreshResult(snapshots, noScenarios, runningScenario, updatedQueue);
    }

    private long getScenarioConfigCreatedTime(File scenarioFolder) {
        if (scenarioFolder == null) {
            return Long.MAX_VALUE;
        }
        try {
            String scenarioName = scenarioFolder.getName();
            String configName = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
            File configFile = new File(configName);
            if (configFile.exists()) {
                return configFile.lastModified();
            }
        } catch (Exception ignored) {
        }
        return Long.MAX_VALUE;
    }

    private ScenarioStatusSnapshot buildScenarioStatusSnapshot(File scenarioFolder, File currentMainLogFile,
            LogAnalysis currentMainLogAnalysis, String runningScenario, RefreshRequest request, List<String> queuedRuns,
            DateFormat format) {
        long createdDate = 0L;
        long completedDate = 0L;
        String scenarioName = scenarioFolder.getName();
        String configName = ScenarioLibraryPathHelper.scenarioConfigFile(vars.getScenarioDir(), scenarioName);
        File configFile = new File(configName);
        if (!configFile.exists()) {
            return null;
        }

        String components = getComponentsFromConfigCached(configFile);
        String mainLogName = ScenarioLibraryPathHelper.scenarioMainLogFile(vars.getScenarioDir(), scenarioName);
        File mainLogFile = new File(mainLogName);
        boolean mainLogExists = mainLogFile.exists();
        boolean activeScenario = scenarioName.equals(runningScenario);
        boolean queuedScenario = queuedRuns.contains(scenarioName);
        String status = "";
        String runtime = "";
        String unsolved = "";
        createdDate = configFile.lastModified();

        LogAnalysis scenarioLogAnalysis = mainLogExists ? analyzeLogFile(mainLogFile) : LogAnalysis.empty();
        LogAnalysis scenarioStdoutAnalysis = analyzeScenarioStdoutFile(scenarioFolder);

        // If no process handle is active and this scenario has no run artifacts yet,
        // don't mark it as Running solely from the executable main_log's last config line.
        if (activeScenario && !request.isScenarioActivelyRunning(scenarioName) && !mainLogExists
                && scenarioStdoutAnalysis.runtimeLine.isEmpty()
                && scenarioStdoutAnalysis.unsolvedLine.isEmpty()
                && scenarioStdoutAnalysis.statusText.isEmpty()
                && !scenarioStdoutAnalysis.successMarkerFound
                && !queuedScenario) {
            activeScenario = false;
        }
        if (mainLogExists) {
            completedDate = mainLogFile.lastModified();
            if (scenarioLogAnalysis.successMarkerFound) {
                status = STATUS_SUCCESS;
            } else if (scenarioLogAnalysis.stoppedMarkerFound) {
                status = STATUS_STOPPED;
            } else {
                status = STATUS_DNF;
                if (scenarioLogAnalysis.statusText.contains(",ERR")) {
                    String errorStr = scenarioLogAnalysis.statusText.substring(scenarioLogAnalysis.statusText.indexOf(',') + 4);
                    unsolved = errorStr;
                }
            }
        }

        if (!STATUS_SUCCESS.equals(status) && scenarioStdoutAnalysis.successMarkerFound) {
            status = STATUS_SUCCESS;
        }
        if (!scenarioLogAnalysis.runtimeLine.isEmpty()) {
            runtime = toRuntimeText(scenarioLogAnalysis.runtimeLine);
        } else if (!scenarioStdoutAnalysis.runtimeLine.isEmpty()) {
            runtime = toRuntimeText(scenarioStdoutAnalysis.runtimeLine);
        }
        if (!scenarioLogAnalysis.unsolvedLine.isEmpty()) {
            try {
                unsolved = scenarioLogAnalysis.unsolvedLine.split(":", 2)[1].trim();
                status = STATUS_UNSOLVED;
            } catch (Exception e) {
                unsolved = "";
            }
        } else if (!scenarioStdoutAnalysis.unsolvedLine.isEmpty()) {
            try {
                unsolved = scenarioStdoutAnalysis.unsolvedLine.split(":", 2)[1].trim();
                status = STATUS_UNSOLVED;
            } catch (Exception e) {
                unsolved = "";
            }
        }

        if (queuedScenario && !activeScenario) {
            status = STATUS_IN_QUEUE;
            runtime = "";
            unsolved = "";
            completedDate = 0L;
        }

        String createdDateStr = createdDate != 0L ? format.format(createdDate) : "";
        String completedDateStr = completedDate != 0L ? format.format(completedDate) : "";
        if ((!STATUS_SUCCESS.equals(status)) && (!STATUS_UNSOLVED.equals(status)) && (!STATUS_DNF.equals(status)) && (!STATUS_STOPPED.equals(status))) {
            if (activeScenario) {
                status = STATUS_RUNNING;
                long lastDate = currentMainLogFile.lastModified();
                boolean isQueued = queuedScenario;
                if (!isQueued && (request.startupTime > 0) && (System.currentTimeMillis() - request.startupTime > LOST_HANDLE_GRACE_MS)
                        && lastDate < request.startupTime) {
                    status = STATUS_LOST_HANDLE;
                } else {
                    String runningStatus = resolveRunningStatusText(scenarioName, currentMainLogAnalysis, scenarioStdoutAnalysis, request);
                    String explicitRunState = getExplicitRunStateLabel(scenarioName, currentMainLogFile, runningStatus, request);
                    if (!explicitRunState.isEmpty()) {
                        status = explicitRunState;
                        if (STATUS_WRITING.equals(explicitRunState)) {
                            writingPhaseScenarios.add(scenarioName);
                        }
                    } else if (writingPhaseScenarios.contains(scenarioName)
                            && !runningStatus.contains(",ERR")
                            && !scenarioLogAnalysis.successMarkerFound
                            && !scenarioStdoutAnalysis.successMarkerFound) {
                        // Keep Writing sticky for the active run until terminal completion/error markers appear.
                        status = STATUS_WRITING;
                    } else if (runningStatus.contains("ERROR:")) {
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
            } else if (queuedScenario) {
                status = STATUS_IN_QUEUE;
            }
        }

        if (!activeScenario || STATUS_SUCCESS.equals(status) || STATUS_UNSOLVED.equals(status)
                || STATUS_DNF.equals(status) || STATUS_STOPPED.equals(status)) {
            writingPhaseScenarios.remove(scenarioName);
        }
        if (!queuedScenario && !activeScenario
                && (STATUS_SUCCESS.equals(status) || STATUS_UNSOLVED.equals(status)
                        || STATUS_DNF.equals(status) || STATUS_STOPPED.equals(status))) {
        }

        if ((status.isEmpty() || status.startsWith(STATUS_RUNNING) || STATUS_BLOCKED.equals(status)
                || STATUS_WRITING.equals(status))
                && scenarioName.equals(request.stopRequestedScenarioName) && !request.isScenarioActivelyRunning(scenarioName)) {
            status = STATUS_STOPPED;
        }

        return new ScenarioStatusSnapshot(scenarioName, components, createdDateStr, completedDateStr, status, runtime, unsolved);
    }

    private String getComponentsFromConfigCached(File configFile) {
        if (configFile == null) {
            return "";
        }
        String cacheKey = configFile.getAbsolutePath();
        long lastModified = configFile.lastModified();
        long fileLength = configFile.length();
        CachedConfigMetadata cached = configMetadataCache.get(cacheKey);
        if (cached != null && cached.matches(lastModified, fileLength)) {
            return cached.components;
        }

        String components = "";
        boolean hasMetaData = false;
        boolean startRecording = false;
        int count = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.equals(METADATA_HEADER)) {
                    hasMetaData = true;
                }
                if (trimmed.equals(METADATA_FOOTER) || trimmed.equals(FILES_HEADER)) {
                    break;
                }
                if (startRecording && !trimmed.isEmpty()) {
                    if (count++ == 0) {
                        components = trimmed;
                    } else {
                        components += " ; " + trimmed;
                    }
                }
                if (trimmed.equals(COMPONENTS_HEADER)) {
                    startRecording = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Problem reading components from " + configFile.getName() + ": " + e);
        }
        if (!hasMetaData) {
            components = EXTERNALLY_CREATED_SCENARIO;
        }
        configMetadataCache.put(cacheKey, new CachedConfigMetadata(lastModified, fileLength, components));
        return components;
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
            return analyzeLogFile(stdoutFile).stdoutSuccessFound;
        } catch (Exception ignored) {}
        return false;
    }

    private LogAnalysis analyzeScenarioStdoutFile(File scenarioFolder) {
        if (scenarioFolder == null) {
            return LogAnalysis.empty();
        }
        try {
            File stdoutFile = new File(scenarioFolder, "gcam_stdout.txt");
            if (!stdoutFile.exists()) {
                return LogAnalysis.empty();
            }
            return analyzeLogFile(stdoutFile);
        } catch (Exception ignored) {}
        return LogAnalysis.empty();
    }

    private String resolveRunningScenario(LogAnalysis currentMainLogAnalysis, RefreshRequest request) {
        String logScenario = currentMainLogAnalysis == null ? "" : safeTrim(currentMainLogAnalysis.runningScenario);
        String currentScenario = request == null ? "" : safeTrim(request.currentGcamScenarioName);
        if (!currentScenario.isEmpty() && request.isScenarioActivelyRunning(currentScenario)) {
            return currentScenario;
        }
        return logScenario;
    }

    private void removeScenarioFromQueue(List<String> queuedRuns, String scenarioName) {
        String normalizedScenario = safeTrim(scenarioName);
        if (queuedRuns == null || normalizedScenario.isEmpty()) {
            return;
        }
        queuedRuns.removeIf(normalizedScenario::equals);
    }

    private String resolveRunningStatusText(String scenarioName, LogAnalysis currentMainLogAnalysis,
            LogAnalysis scenarioStdoutAnalysis, RefreshRequest request) {
        String currentScenario = request == null ? "" : safeTrim(request.currentGcamScenarioName);
        if (!safeTrim(scenarioName).equals(currentScenario)) {
            return currentMainLogAnalysis == null ? "" : safeTrim(currentMainLogAnalysis.statusText);
        }

        String currentMainLogStatus = currentMainLogAnalysis == null ? "" : safeTrim(currentMainLogAnalysis.statusText);
        if (!currentMainLogStatus.isEmpty()) {
            return currentMainLogStatus;
        }
        String scenarioStdoutStatus = scenarioStdoutAnalysis == null ? "" : safeTrim(scenarioStdoutAnalysis.statusText);
        if (!scenarioStdoutStatus.isEmpty()) {
            return scenarioStdoutStatus;
        }
        return "";
    }

    private LogAnalysis analyzeLogFile(File file) {
        if (file == null || !file.exists()) {
            return LogAnalysis.empty();
        }
        String cacheKey = file.getAbsolutePath();
        long lastModified = file.lastModified();
        long fileLength = file.length();
        CachedLogAnalysis cached = logAnalysisCache.get(cacheKey);
        if (cached != null && cached.matches(lastModified, fileLength)) {
            return cached.analysis;
        }

        LogAnalysis analysis = analyzeLogFileUncached(file, fileLength);
        logAnalysisCache.put(cacheKey, new CachedLogAnalysis(lastModified, fileLength, analysis));
        return analysis;
    }

    private LogAnalysis analyzeCurrentMainLogFile(File currentMainLogFile, RefreshRequest request) {
        if (currentMainLogFile == null || !currentMainLogFile.exists()) {
            return LogAnalysis.empty();
        }
        if (request != null && request.currentGcamScenarioName != null && !request.currentGcamScenarioName.trim().isEmpty()) {
            return analyzeLogFileUncached(currentMainLogFile, currentMainLogFile.length());
        }
        return analyzeLogFile(currentMainLogFile);
    }

    private LogAnalysis analyzeLogFileUncached(File file, long fileLength) {
        String successLine = "";
        String runtimeLine = "";
        String unsolvedLine = "";
        String errorLine = "";
        String runningPeriod = "";
        String runningScenario = "";
        boolean stdoutSuccessFound = false;
        boolean stoppedMarkerFound = false;

        List<String> tailLines = readTailLines(file, LOG_TAIL_BYTES);
        for (int i = tailLines.size() - 1; i >= 0; i--) {
            String line = safeTrim(tailLines.get(i));
            if (line.isEmpty()) {
                continue;
            }
            if (!stoppedMarkerFound && line.contains(STOPPED_MARKER)) {
                stoppedMarkerFound = true;
            }
            if (successLine.isEmpty() && containsAny(line, STDOUT_SUCCESS_MARKERS)) {
                successLine = line;
                stdoutSuccessFound = true;
            }
            if (runtimeLine.isEmpty() && line.contains(RUNTIME_PREFIX)) {
                runtimeLine = line;
            }
            if (unsolvedLine.isEmpty() && line.contains(UNSOLVED_PREFIX)) {
                unsolvedLine = line;
            }
            if (errorLine.isEmpty() && line.contains(ERROR_PREFIX)) {
                errorLine = line;
            }
            if (runningScenario.isEmpty() && line.contains(CONFIG_FILE_PREFIX)) {
                runningScenario = scenarioNameFromConfigLine(line);
            }
            if (runningPeriod.isEmpty() && extractUnsolvedPeriodsFromErrorLine(line).isEmpty()) {
                String period = extractRunningPeriod(line);
                if (!period.isEmpty()) {
                    runningPeriod = period;
                }
            }
            if (!runtimeLine.isEmpty() && !unsolvedLine.isEmpty() && !errorLine.isEmpty()
                    && !runningScenario.isEmpty() && !runningPeriod.isEmpty() && !successLine.isEmpty() && stoppedMarkerFound) {
                break;
            }
        }

        boolean requiresFallback = (!successLine.isEmpty() || !runtimeLine.isEmpty() || !unsolvedLine.isEmpty()
                || !errorLine.isEmpty() || !runningScenario.isEmpty() || !runningPeriod.isEmpty() || stoppedMarkerFound) ? false : fileLength > LOG_TAIL_BYTES;
        if (requiresFallback) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = safeTrim(line);
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (!stoppedMarkerFound && trimmed.contains(STOPPED_MARKER)) {
                        stoppedMarkerFound = true;
                    }
                    if (runningScenario.isEmpty() && trimmed.contains(CONFIG_FILE_PREFIX)) {
                        runningScenario = scenarioNameFromConfigLine(trimmed);
                    }
                    if (successLine.isEmpty() && containsAny(trimmed, STDOUT_SUCCESS_MARKERS)) {
                        successLine = trimmed;
                        stdoutSuccessFound = true;
                    }
                    if (runtimeLine.isEmpty() && trimmed.contains(RUNTIME_PREFIX)) {
                        runtimeLine = trimmed;
                    }
                    if (unsolvedLine.isEmpty() && trimmed.contains(UNSOLVED_PREFIX)) {
                        unsolvedLine = trimmed;
                    }
                    if (errorLine.isEmpty() && trimmed.contains(ERROR_PREFIX)) {
                        errorLine = trimmed;
                    }
                    if (runningPeriod.isEmpty() && extractUnsolvedPeriodsFromErrorLine(trimmed).isEmpty()) {
                        String period = extractRunningPeriod(trimmed);
                        if (!period.isEmpty()) {
                            runningPeriod = period;
                        }
                    }
                    if (!runtimeLine.isEmpty() && !unsolvedLine.isEmpty() && !errorLine.isEmpty()
                            && !runningScenario.isEmpty() && !runningPeriod.isEmpty() && !successLine.isEmpty() && stoppedMarkerFound) {
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        String statusText = "";
        if (!runningPeriod.isEmpty()) {
            statusText = runningPeriod;
        } else if (!unsolvedLine.isEmpty()) {
            String msg = unsolvedLine.replace(UNSOLVED_PREFIX, "").trim();
            statusText = "Unsolved,ERR " + msg;
        } else {
            String unsolvedFromError = extractUnsolvedPeriodsFromErrorLine(errorLine);
            if (!unsolvedFromError.isEmpty()) {
                statusText = "Unsolved,ERR " + unsolvedFromError;
            } else if (!errorLine.isEmpty()) {
                statusText = "ERROR,ERR " + errorLine.trim();
            }
        }
        return new LogAnalysis(successLine, runtimeLine, unsolvedLine, statusText, runningScenario, stdoutSuccessFound, stoppedMarkerFound);
    }

    private List<String> readTailLines(File file, int tailBytes) {
        ArrayList<String> lines = new ArrayList<>();
        if (file == null || !file.exists()) {
            return lines;
        }
        long start = 0L;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            start = Math.max(0L, length - Math.max(1024, tailBytes));
            raf.seek(start);
            if (start > 0) {
                raf.readLine();
            }
            byte[] bytes = new byte[(int) Math.max(0L, length - raf.getFilePointer())];
            raf.readFully(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            for (String line : text.split("\\R")) {
                String trimmed = safeTrim(line);
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        } catch (Exception ignored) {}
        return lines;
    }

    private String scenarioNameFromConfigLine(String configLine) {
        if (configLine == null || !configLine.contains(CONFIG_FILE_PREFIX)) {
            return "";
        }
        String path = configLine.replace(CONFIG_FILE_PREFIX, "").trim();
        if (path.isEmpty()) {
            return "";
        }
        String name = new File(path).getParent();
        if (name == null) {
            return "";
        }
        int separatorIndex = Math.max(name.lastIndexOf(File.separatorChar), name.lastIndexOf('/'));
        return separatorIndex >= 0 ? name.substring(separatorIndex + 1) : name;
    }

    private String extractRunningPeriod(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher yearMatcher = RUNNING_PERIOD_WITH_YEAR_PATTERN.matcher(line);
        if (yearMatcher.find()) {
            String period = yearMatcher.group(1);
            String year = yearMatcher.group(2);
            String trimmedPeriod = period == null ? "" : period.trim();
            String trimmedYear = year == null ? "" : year.trim();
            if (!trimmedPeriod.isEmpty() && !trimmedYear.isEmpty()) {
                if (isValidPeriodAndYear(trimmedPeriod, trimmedYear)) {
                    return trimmedPeriod + "," + trimmedYear;
                }
            }
        }
        return "";
    }

    /**
     * Validates that a period number is within a reasonable range (0–150 periods typical for GCAM).
     * Filters out spurious matches like error codes or years.
     */
    private boolean isValidPeriod(String period) {
        if (period == null || period.isEmpty()) {
            return false;
        }
        try {
            int p = Integer.parseInt(period);
            // GCAM typically runs 0–32 periods for standard runs, up to ~150 for extended scenarios
            // Filter out unlikely values (years like 1701-2999, error codes, etc.)
            return p >= 0 && p <= 200;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Validates that period and year are both reasonable.
     * Filters out spurious matches like error text or stray numbers.
     */
    private boolean isValidPeriodAndYear(String period, String year) {
        if (!isValidPeriod(period)) {
            return false;
        }
        if (year == null || year.isEmpty()) {
            return false;
        }
        try {
            int y = Integer.parseInt(year);
            // Years should be in a reasonable range: 1950–2200
            return y >= 1950 && y <= 2200;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String extractUnsolvedPeriodsFromErrorLine(String line) {
        String trimmed = safeTrim(line);
        if (trimmed.isEmpty() || !trimmed.toLowerCase(Locale.ENGLISH).contains("did not solve period")) {
            return "";
        }
        java.util.regex.Matcher matcher = UNSOLVED_PERIOD_ERROR_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return "";
        }
        String rawPeriods = matcher.group(1);
        if (rawPeriods == null || rawPeriods.trim().isEmpty()) {
            return "";
        }
        LinkedHashSet<String> periods = new LinkedHashSet<>();
        java.util.regex.Matcher numberMatcher = UNSOLVED_PERIOD_NUMBER_PATTERN.matcher(rawPeriods);
        while (numberMatcher.find()) {
            String period = safeTrim(numberMatcher.group());
            if (!period.isEmpty()) {
                periods.add(period);
            }
        }
        if (periods.isEmpty()) {
            return "";
        }
        return String.join(", ", periods);
    }

    private boolean containsAny(String line, String[] markers) {
        if (line == null || markers == null) {
            return false;
        }
        for (String marker : markers) {
            if (marker != null && !marker.isEmpty() && line.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String getExplicitRunStateLabel(String scenarioName, File currentMainLogFile, String runningStatus,
            RefreshRequest request) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            return "";
        }
        if (scenarioName.equals(request.stopRequestedScenarioName) && !request.isScenarioActivelyRunning(scenarioName)) {
            return STATUS_STOPPED;
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

    private static final class CachedConfigMetadata {
        final long lastModified;
        final long fileLength;
        final String components;

        CachedConfigMetadata(long lastModified, long fileLength, String components) {
            this.lastModified = lastModified;
            this.fileLength = fileLength;
            this.components = components == null ? "" : components;
        }

        boolean matches(long candidateLastModified, long candidateFileLength) {
            return lastModified == candidateLastModified && fileLength == candidateFileLength;
        }
    }

    private static final class CachedLogAnalysis {
        final long lastModified;
        final long fileLength;
        final LogAnalysis analysis;

        CachedLogAnalysis(long lastModified, long fileLength, LogAnalysis analysis) {
            this.lastModified = lastModified;
            this.fileLength = fileLength;
            this.analysis = analysis == null ? LogAnalysis.empty() : analysis;
        }

        boolean matches(long candidateLastModified, long candidateFileLength) {
            return lastModified == candidateLastModified && fileLength == candidateFileLength;
        }
    }

    private static final class LogAnalysis {
        final String successLine;
        final String runtimeLine;
        final String unsolvedLine;
        final String statusText;
        final String runningScenario;
        final boolean stdoutSuccessFound;
        final boolean successMarkerFound;
        final boolean stoppedMarkerFound;

        LogAnalysis(String successLine, String runtimeLine, String unsolvedLine, String statusText,
                String runningScenario, boolean stdoutSuccessFound, boolean stoppedMarkerFound) {
            this.successLine = successLine == null ? "" : successLine;
            this.runtimeLine = runtimeLine == null ? "" : runtimeLine;
            this.unsolvedLine = unsolvedLine == null ? "" : unsolvedLine;
            this.statusText = statusText == null ? "" : statusText;
            this.runningScenario = runningScenario == null ? "" : runningScenario;
            this.stdoutSuccessFound = stdoutSuccessFound;
            this.successMarkerFound = !this.successLine.isEmpty();
            this.stoppedMarkerFound = stoppedMarkerFound;
        }

        static LogAnalysis empty() {
            return new LogAnalysis("", "", "", "", "", false, false);
        }
    }

    /** Input context used by {@link #refresh(RefreshRequest)} to reconcile run state. */
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

        /** Returns an empty request with default values. */
        static RefreshRequest empty() {
            return new RefreshRequest(new ArrayList<>(), 0L, "", "", false, false, false);
        }

        /** Returns whether the provided scenario is still the actively running scenario. */
        boolean isScenarioActivelyRunning(String scenarioName) {
            return scenarioActivelyRunning
                    && scenarioName != null
                    && !scenarioName.trim().isEmpty()
                    && scenarioName.equals(currentGcamScenarioName);
        }
    }
}