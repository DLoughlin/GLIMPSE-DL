package gui;

import java.util.ArrayList;
import java.util.List;

final class ScenarioStatusRefreshResult {
    private final List<ScenarioStatusSnapshot> snapshots;
    private final boolean noScenarios;
    private final String runningScenario;
    private final List<String> queuedRuns;

    ScenarioStatusRefreshResult(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios, String runningScenario,
            List<String> queuedRuns) {
        this.snapshots = snapshots == null ? new ArrayList<>() : new ArrayList<>(snapshots);
        this.noScenarios = noScenarios;
        this.runningScenario = runningScenario == null ? "" : runningScenario;
        this.queuedRuns = queuedRuns == null ? new ArrayList<>() : new ArrayList<>(queuedRuns);
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
}