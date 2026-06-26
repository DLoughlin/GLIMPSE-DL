package gui;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import glimpseUtil.ProcessResult;
import glimpseUtil.ProcessRunner;

/**
 * Owns mutable GCAM execution/session state for the scenario library UI.
 * Keeps process/future identity, prompt state, and queued/completed scenario tracking
 * behind a small API and exposes immutable snapshots for read-only consumers.
 */
final class GcamRunController {
    interface LineListener {
        void onLine(String scenarioName, String line, boolean stderr);
    }

    interface RunLifecycleListener {
        void onRunStarted(String scenarioName);
        void onRunFinished(String scenarioName, ProcessResult result);
    }

    static final class RunRequest {
        final String scenarioName;
        final List<String> command;
        final File workingDirectory;

        RunRequest(String scenarioName, List<String> command, File workingDirectory) {
            this.scenarioName = normalize(scenarioName);
            this.command = command == null ? Collections.emptyList() : new ArrayList<>(command);
            this.workingDirectory = workingDirectory;
        }

        boolean isValid() {
            return !scenarioName.isEmpty() && command != null && !command.isEmpty();
        }
    }

    static final class ExecutionState {
        final ProcessRunner.RunningProcess currentRun;
        final Future<ProcessResult> currentFuture;
        final String currentScenarioName;
        final boolean promptDialogActive;
        final String activePromptLine;
        final String lastHandledDatabasePrompt;
        final boolean databasePromptAwaitingReset;
        final ProcessRunner.StopResult lastStopResult;
        final String lastStoppedScenarioName;
        final List<String> queuedScenarioNames;
        final List<String> completedScenarioNames;
        final long currentRunStartTimeMillis;

        ExecutionState(ProcessRunner.RunningProcess currentRun,
                Future<ProcessResult> currentFuture,
                String currentScenarioName,
                boolean promptDialogActive,
                String activePromptLine,
                String lastHandledDatabasePrompt,
                boolean databasePromptAwaitingReset,
                ProcessRunner.StopResult lastStopResult,
                String lastStoppedScenarioName,
                List<String> queuedScenarioNames,
                List<String> completedScenarioNames,
                long currentRunStartTimeMillis) {
            this.currentRun = currentRun;
            this.currentFuture = currentFuture;
            this.currentScenarioName = normalize(currentScenarioName);
            this.promptDialogActive = promptDialogActive;
            this.activePromptLine = activePromptLine == null ? null : activePromptLine;
            this.lastHandledDatabasePrompt = normalize(lastHandledDatabasePrompt);
            this.databasePromptAwaitingReset = databasePromptAwaitingReset;
            this.lastStopResult = lastStopResult;
            this.lastStoppedScenarioName = normalize(lastStoppedScenarioName);
            this.queuedScenarioNames = immutableCopy(queuedScenarioNames);
            this.completedScenarioNames = immutableCopy(completedScenarioNames);
            this.currentRunStartTimeMillis = Math.max(0L, currentRunStartTimeMillis);
        }

        boolean hasActiveRun() {
            return (currentRun != null && currentRun.getProcess() != null && currentRun.getProcess().isAlive())
                    || (currentFuture != null && !currentFuture.isDone());
        }

        boolean isStopRequested() {
            return currentRun != null && currentRun.isStopRequested();
        }

        boolean isScenarioActivelyRunning(String scenarioName) {
            String normalized = normalize(scenarioName);
            return !normalized.isEmpty()
                    && normalized.equals(currentScenarioName)
                    && currentRun != null
                    && currentRun.getProcess() != null
                    && currentRun.getProcess().isAlive();
        }
    }

    private final List<String> queuedScenarioNames = new ArrayList<>();
    private final List<String> completedScenarioNames = new ArrayList<>();
    private final StringBuilder recentPromptBuffer = new StringBuilder();

    private volatile ProcessRunner.RunningProcess currentRun;
    private volatile Future<ProcessResult> currentFuture;
    private volatile String currentScenarioName;
    private volatile boolean promptDialogActive;
    private volatile String activePromptLine;
    private volatile String lastHandledDatabasePrompt;
    private volatile boolean databasePromptAwaitingReset;
    private volatile ProcessRunner.StopResult lastStopResult;
    private volatile String lastStoppedScenarioName;
    private volatile long currentRunStartTimeMillis;

    ExecutionState snapshot() {
        synchronized (queuedScenarioNames) {
            synchronized (completedScenarioNames) {
                return new ExecutionState(
                        currentRun,
                        currentFuture,
                        currentScenarioName,
                        promptDialogActive,
                        activePromptLine,
                        lastHandledDatabasePrompt,
                        databasePromptAwaitingReset,
                        lastStopResult,
                        lastStoppedScenarioName,
                        queuedScenarioNames,
                        completedScenarioNames,
                        currentRunStartTimeMillis);
            }
        }
    }

    void setCurrentExecution(ProcessRunner.RunningProcess run, Future<ProcessResult> future, String scenarioName) {
        this.currentRun = run;
        this.currentFuture = future;
        this.currentScenarioName = normalize(scenarioName);
        this.currentRunStartTimeMillis = System.currentTimeMillis();
    }

    void clearCurrentExecution() {
        this.currentRun = null;
        this.currentFuture = null;
        this.currentScenarioName = "";
        this.currentRunStartTimeMillis = 0L;
        this.activePromptLine = null;
        this.promptDialogActive = false;
        this.databasePromptAwaitingReset = false;
        clearRecentPromptBuffer();
    }

    Future<ProcessResult> beginRun(ExecutionThread executionThread,
            RunRequest request,
            LineListener lineListener,
            RunLifecycleListener lifecycleListener) {
        if (executionThread == null) {
            throw new IllegalArgumentException("executionThread cannot be null");
        }
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("request must include scenario name and command");
        }

        ScenarioRunCallable callable = new ScenarioRunCallable(this, request, lineListener, lifecycleListener);
        Future<ProcessResult> future = executionThread.submitCallable(callable);
        this.currentFuture = future;
        return future;
    }

    private void markRunStarted(String scenarioName, ProcessRunner.RunningProcess run) {
        this.currentRun = run;
        this.currentScenarioName = normalize(scenarioName);
        this.currentRunStartTimeMillis = System.currentTimeMillis();
        this.lastStopResult = null;
        this.activePromptLine = null;
        this.promptDialogActive = false;
        this.databasePromptAwaitingReset = false;
        clearRecentPromptBuffer();
    }

    private void markRunFinished(String scenarioName, ProcessResult result) {
        String normalizedScenario = normalize(scenarioName);
        removeQueuedRun(normalizedScenario);
        if (result != null && result.getExitCode() == 0 && !result.isTimedOut()) {
            addCompletedRun(normalizedScenario);
        }
        if (normalizedScenario.equals(getCurrentScenarioName())) {
            clearCurrentExecution();
        }
    }

    private static final class ScenarioRunCallable implements java.util.concurrent.Callable<ProcessResult>, ExecutionThread.DebugDescribable {
        private final GcamRunController controller;
        private final RunRequest request;
        private final LineListener lineListener;
        private final RunLifecycleListener lifecycleListener;

        ScenarioRunCallable(GcamRunController controller,
                RunRequest request,
                LineListener lineListener,
                RunLifecycleListener lifecycleListener) {
            this.controller = controller;
            this.request = request;
            this.lineListener = lineListener;
            this.lifecycleListener = lifecycleListener;
        }

        @Override
        public String getDebugDescription() {
            return "GCAM scenario run: " + request.scenarioName;
        }

        @Override
        public ProcessResult call() throws Exception {
            ProcessRunner.RunningProcess run = null;
            ProcessResult result = null;
            long startMillis = System.currentTimeMillis();
            try {
                // Log that we're about to start this scenario
                appendGcamInfo("[GCAM-RUN] Starting execution for scenario: " + request.scenarioName);
                appendGcamInfo("[GCAM-RUN] Command: " + String.join(" ", request.command));
                appendGcamInfo("[GCAM-RUN] Working directory: " + (request.workingDirectory != null ? request.workingDirectory.getAbsolutePath() : "current"));
                
                // Pre-flight checks
                if (!request.command.isEmpty()) {
                    String executable = request.command.get(0);
                    java.io.File exeFile = new java.io.File(executable);
                    appendGcamInfo("[GCAM-RUN] Executable path: " + exeFile.getAbsolutePath());
                    appendGcamInfo("[GCAM-RUN] Executable exists: " + exeFile.exists());
                    appendGcamInfo("[GCAM-RUN] Executable is file: " + exeFile.isFile());
                    appendGcamInfo("[GCAM-RUN] Executable can read: " + exeFile.canRead());
                    appendGcamInfo("[GCAM-RUN] Executable can execute: " + exeFile.canExecute());
                }
                
                if (request.workingDirectory != null) {
                    appendGcamInfo("[GCAM-RUN] Working dir exists: " + request.workingDirectory.exists());
                    appendGcamInfo("[GCAM-RUN] Working dir is dir: " + request.workingDirectory.isDirectory());
                    appendGcamInfo("[GCAM-RUN] Working dir can read: " + request.workingDirectory.canRead());
                }
                
                // Build environment with CLASSPATH for GCAM
                java.util.Map<String, String> environment = new java.util.HashMap<>(System.getenv());
                String classpath = "." + java.io.File.pathSeparator + ".." + java.io.File.separator + "libs" + java.io.File.separator + "jars" + java.io.File.separator + "*" + java.io.File.pathSeparator + "XMLDBDriver.jar";
                environment.put("CLASSPATH", classpath);
                appendGcamInfo("[GCAM-RUN] Setting CLASSPATH: " + classpath);
                
                run = ProcessRunner.start(
                        request.command,
                        request.workingDirectory,
                        environment,
                        line -> {
                            if (lineListener != null) {
                                lineListener.onLine(request.scenarioName, line, false);
                            }
                        },
                        line -> {
                            if (lineListener != null) {
                                lineListener.onLine(request.scenarioName, line, true);
                            }
                        });
                controller.markRunStarted(request.scenarioName, run);
                appendGcamInfo("[GCAM-RUN] Process started successfully for scenario: " + request.scenarioName);
                if (lifecycleListener != null) {
                    lifecycleListener.onRunStarted(request.scenarioName);
                }
                result = run.waitForResult(null);
                long elapsed = System.currentTimeMillis() - startMillis;
                appendGcamInfo("[GCAM-RUN] Process completed for scenario: " + request.scenarioName + ", exit code: " + result.getExitCode() + ", duration: " + elapsed + "ms");
                if (result.getExitCode() != 0) {
                    appendGcamError("[GCAM-RUN] Exit code " + result.getExitCode() + " (0x" + Integer.toHexString(result.getExitCode() & 0xFFFFFFFF) + ")");
                    if (result.getStderr() != null && !result.getStderr().isEmpty()) {
                        appendGcamError("[GCAM-RUN] Process stderr: " + result.getStderr());
                    }
                }
                return result;
            } catch (Exception ex) {
                long elapsed = Math.max(0L, System.currentTimeMillis() - startMillis);
                String errorText = "GLIMPSE failed to start GCAM for scenario '" + request.scenarioName + "': " + ex;
                appendGcamError("[GCAM-RUN-ERROR] " + errorText);
                appendGcamError("[GCAM-RUN-ERROR] Exception type: " + ex.getClass().getName());
                appendGcamError("[GCAM-RUN-ERROR] Exception message: " + ex.getMessage());
                appendThrowableToGcamConsole(ex);
                if (lineListener != null) {
                    try {
                        lineListener.onLine(request.scenarioName, errorText, true);
                    } catch (Exception ignored) {}
                }
                result = new ProcessResult(-1, "", errorText, false, elapsed);
                return result;
            } finally {
                controller.markRunFinished(request.scenarioName, result);
                if (lifecycleListener != null) {
                    lifecycleListener.onRunFinished(request.scenarioName, result);
                }
            }
        }

        private void appendGcamInfo(String message) {
            try {
                ConsoleManager.appendLine(
                        ConsoleManager.StreamSource.GCAM_STDOUT,
                        ConsoleManager.MessageKind.GLIMPSE_INFO,
                        message == null ? "" : message);
            } catch (Exception ignored) {}
        }

        private void appendGcamError(String message) {
            try {
                ConsoleManager.appendLine(
                        ConsoleManager.StreamSource.GCAM_STDOUT,
                        ConsoleManager.MessageKind.STDERR,
                        message == null ? "" : message);
            } catch (Exception ignored) {}
        }

        private void appendThrowableToGcamConsole(Throwable throwable) {
            if (throwable == null) {
                return;
            }
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                pw.flush();
                String[] lines = sw.toString().split("\\R");
                for (String line : lines) {
                    appendGcamError(line);
                }
            } catch (Exception ignored) {}
        }
    }

    boolean hasActiveRun() {
        return snapshot().hasActiveRun();
    }

    boolean isScenarioActivelyRunning(String scenarioName) {
        return snapshot().isScenarioActivelyRunning(scenarioName);
    }

    void setPromptDialogActive(boolean active) {
        this.promptDialogActive = active;
    }

    boolean isPromptDialogActive() {
        return promptDialogActive;
    }

    void setActivePromptLine(String promptLine) {
        this.activePromptLine = promptLine;
    }

    String getActivePromptLine() {
        return activePromptLine;
    }

    void setLastHandledDatabasePrompt(String prompt) {
        this.lastHandledDatabasePrompt = normalize(prompt);
    }

    String getLastHandledDatabasePrompt() {
        return normalize(lastHandledDatabasePrompt);
    }

    void setDatabasePromptAwaitingReset(boolean awaitingReset) {
        this.databasePromptAwaitingReset = awaitingReset;
    }

    boolean isDatabasePromptAwaitingReset() {
        return databasePromptAwaitingReset;
    }

    String appendAndGetRecentPromptWindow(String line, int maxChars) {
        if (line == null) {
            return "";
        }
        synchronized (recentPromptBuffer) {
            if (recentPromptBuffer.length() > 0) {
                recentPromptBuffer.append(' ');
            }
            recentPromptBuffer.append(line.trim());
            int safeMax = Math.max(1, maxChars);
            if (recentPromptBuffer.length() > safeMax) {
                recentPromptBuffer.delete(0, recentPromptBuffer.length() - safeMax);
            }
            return recentPromptBuffer.toString();
        }
    }

    void clearRecentPromptBuffer() {
        synchronized (recentPromptBuffer) {
            recentPromptBuffer.setLength(0);
        }
    }

    void resetPromptCycle() {
        databasePromptAwaitingReset = false;
        lastHandledDatabasePrompt = "";
    }

    ProcessRunner.StopResult stopCurrentRun() {
        ProcessRunner.RunningProcess run = currentRun;
        if (run == null) {
            lastStopResult = null;
            return null;
        }
        lastStoppedScenarioName = getCurrentScenarioName();
        lastStopResult = run.stop();
        return lastStopResult;
    }

    ProcessRunner.StopResult getLastStopResult() {
        return lastStopResult;
    }

    String getStopRequestedScenarioName() {
        String currentScenario = getCurrentScenarioName();
        ProcessRunner.RunningProcess run = currentRun;
        if (!currentScenario.isEmpty() && run != null && run.isStopRequested()) {
            return currentScenario;
        }
        return normalize(lastStoppedScenarioName);
    }

    void markScenarioStopped(String scenarioName) {
        lastStoppedScenarioName = normalize(scenarioName);
    }

    void clearStoppedScenario(String scenarioName) {
        String normalized = normalize(scenarioName);
        if (normalized.isEmpty()) {
            return;
        }
        String currentScenario = getCurrentScenarioName();
        ProcessRunner.RunningProcess run = currentRun;
        if (normalized.equals(currentScenario) && run != null && run.isStopRequested()) {
            return;
        }
        if (normalized.equals(normalize(lastStoppedScenarioName))) {
            lastStoppedScenarioName = "";
        }
    }

    void clearQueuedRuns() {
        synchronized (queuedScenarioNames) {
            queuedScenarioNames.clear();
        }
    }

    void addQueuedRun(String scenarioName) {
        String normalized = normalize(scenarioName);
        if (normalized.isEmpty()) {
            return;
        }
        synchronized (queuedScenarioNames) {
            queuedScenarioNames.add(normalized);
        }
    }

    void replaceQueuedRuns(List<String> scenarioNames) {
        synchronized (queuedScenarioNames) {
            queuedScenarioNames.clear();
            if (scenarioNames == null) {
                return;
            }
            for (String scenarioName : scenarioNames) {
                String normalized = normalize(scenarioName);
                if (!normalized.isEmpty()) {
                    queuedScenarioNames.add(normalized);
                }
            }
        }
    }

    boolean removeQueuedRun(String scenarioName) {
        String normalized = normalize(scenarioName);
        boolean removed = false;
        synchronized (queuedScenarioNames) {
            while (queuedScenarioNames.remove(normalized)) {
                removed = true;
            }
        }
        return removed;
    }

    List<String> getQueuedRuns() {
        synchronized (queuedScenarioNames) {
            return new ArrayList<>(queuedScenarioNames);
        }
    }

    void addCompletedRun(String scenarioName) {
        String normalized = normalize(scenarioName);
        if (normalized.isEmpty()) {
            return;
        }
        synchronized (completedScenarioNames) {
            completedScenarioNames.add(normalized);
        }
    }

    List<String> getCompletedRuns() {
        synchronized (completedScenarioNames) {
            return new ArrayList<>(completedScenarioNames);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    ProcessRunner.RunningProcess getCurrentRun() {
        return currentRun;
    }

    Future<ProcessResult> getCurrentFuture() {
        return currentFuture;
    }

    String getCurrentScenarioName() {
        return normalize(currentScenarioName);
    }
}