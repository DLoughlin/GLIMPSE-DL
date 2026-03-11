package gui;

import java.util.ArrayList;
import java.util.List;

final class ScenarioStatusRefreshResult {
    private final List<ScenarioStatusSnapshot> snapshots;
    private final boolean noScenarios;
    private final String runningScenario;
    private final List<String> queuedRuns;
    private final List<String> completedRunsToAdd;

    ScenarioStatusRefreshResult(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios, String runningScenario,
            List<String> queuedRuns, List<String> completedRunsToAdd) {
        this.snapshots = snapshots == null ? new ArrayList<>() : new ArrayList<>(snapshots);
        this.noScenarios = noScenarios;
        this.runningScenario = runningScenario == null ? "" : runningScenario;
        this.queuedRuns = queuedRuns == null ? new ArrayList<>() : new ArrayList<>(queuedRuns);
        this.completedRunsToAdd = completedRunsToAdd == null ? new ArrayList<>() : new ArrayList<>(completedRunsToAdd);
    }

    List<ScenarioStatusSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    boolean isNoScenarios() {
        return noScenarios;
    }

    String getRunningScenario() {
        return runningScenario;
    }

    List<String> getQueuedRuns() {
        return new ArrayList<>(queuedRuns);
    }

    List<String> getCompletedRunsToAdd() {
        return new ArrayList<>(completedRunsToAdd);
    }
}
