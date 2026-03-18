package gui;

import java.io.File;
import java.util.ArrayList;

import glimpseUtil.GLIMPSEFiles;

final class GcamPromptMonitor {
    static final int DEFAULT_PROMPT_BUFFER_MAX_CHARS = 512;

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

    String appendAndGetRecentPromptWindow(String line) {
        return runController.appendAndGetRecentPromptWindow(line, promptBufferMaxChars);
    }

    void clearRecentPromptBuffer() {
        runController.clearRecentPromptBuffer();
    }

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
