package gui;

import java.io.File;
import java.util.ArrayList;

import glimpseUtil.GLIMPSEFiles;

/**
 * Utility for detecting interactive GCAM prompt windows and output-writing phases.
 * <p>
 * It buffers recent output lines, identifies database-save prompts, and reports prompt
 * events to callbacks without coupling prompt parsing logic to run-control orchestration.
 */
final class GcamPromptMonitor {
    static final int DEFAULT_PROMPT_BUFFER_MAX_CHARS = 512;

    /** Callback contract for prompt detections emitted by this monitor. */
    interface PromptCallbacks {
        void onPromptDetected(String promptWindow);
    }

    private final GcamRunController runController;
    private final GLIMPSEFiles files;
    private final PromptCallbacks callbacks;
    private final int promptBufferMaxChars;

    GcamPromptMonitor(GcamRunController runController, GLIMPSEFiles files, PromptCallbacks callbacks) {
        this(runController, files, callbacks, DEFAULT_PROMPT_BUFFER_MAX_CHARS);
    }

    GcamPromptMonitor(GcamRunController runController, GLIMPSEFiles files, PromptCallbacks callbacks, int promptBufferMaxChars) {
        this.runController = runController;
        this.files = files;
        this.callbacks = callbacks;
        this.promptBufferMaxChars = Math.max(1, promptBufferMaxChars);
    }

    /** Handles a new output line and triggers prompt callbacks when a DB prompt is recognized. */
    void handlePotentialInteractivePrompt(String line) {
        try {
            String promptWindow = appendAndGetRecentPromptWindow(line);
            String normalizedPromptWindow = ScenarioLibraryPromptHelper.normalizeDatabasePromptText(promptWindow);
            if (!ScenarioLibraryPromptHelper.looksLikeDatabaseSavePrompt(normalizedPromptWindow)) {
                if (runController.isDatabasePromptAwaitingReset()) {
                    runController.resetPromptCycle();
                }
                return;
            }
            maybeNotifyPromptDetected(promptWindow);
        } catch (Exception ignored) {}
    }

    /** Appends text to the rolling prompt buffer and returns the recent normalized window. */
    String appendAndGetRecentPromptWindow(String line) {
        return runController.appendAndGetRecentPromptWindow(line, promptBufferMaxChars);
    }

    /** Clears any cached prompt text accumulated so far. */
    void clearRecentPromptBuffer() {
        runController.clearRecentPromptBuffer();
    }

    /** Returns whether the run appears to be in the output-writing phase. */
    boolean isWritingResultsPhase(File currentMainLogFile, String runningStatus) {
        if (runningStatus != null && runningStatus.contains(",ERR")) {
            return false;
        }
        if (containsWritingPhrase(runningStatus)) {
            return true;
        }
        if (currentMainLogFile == null || !currentMainLogFile.exists() || files == null) {
            return false;
        }
        try {
            ArrayList<String> lines = files.getStringArrayFromFile(currentMainLogFile.getAbsolutePath(), "#");
            int start = Math.max(0, lines.size() - 40);
            for (int i = lines.size() - 1; i >= start; i--) {
                String line = lines.get(i);
                if (line == null) {
                    continue;
                }
                String normalized = ScenarioLibraryPromptHelper.normalizeDatabasePromptText(line);
                if (normalized.isEmpty()) {
                    continue;
                }
                if (normalized.contains("model run completed")) {
                    return false;
                }
                if (containsWritingPhrase(normalized)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Delegates phrase detection for write-phase markers to prompt helper logic. */
    boolean containsWritingPhrase(String text) {
        return ScenarioLibraryPromptHelper.containsWritingPhrase(text);
    }

    private void maybeNotifyPromptDetected(String promptWindow) {
        if (promptWindow == null || promptWindow.trim().isEmpty()) {
            return;
        }
        String normalizedPrompt = ScenarioLibraryPromptHelper.normalizeDatabasePromptText(promptWindow);
        if (normalizedPrompt.isEmpty()) {
            return;
        }
        if (runController.isPromptDialogActive()) {
            return;
        }
        if (runController.isDatabasePromptAwaitingReset()
                && normalizedPrompt.equals(runController.getLastHandledDatabasePrompt())) {
            return;
        }
        if (callbacks != null) {
            callbacks.onPromptDetected(promptWindow);
        }
    }
}
