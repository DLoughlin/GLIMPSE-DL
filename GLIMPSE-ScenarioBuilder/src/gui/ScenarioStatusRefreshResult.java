package gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable container for one scenario-status refresh pass.
 * <p>
 * Includes refreshed row snapshots plus queue/running metadata needed by the UI.
 */
final class ScenarioStatusRefreshResult {
    private final List<ScenarioStatusSnapshot> snapshots;
    private final boolean noScenarios;
    private final String runningScenario;
    private final List<String> queuedRuns;

    /** Creates a result object with defensive copies of refresh outputs. */
    ScenarioStatusRefreshResult(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios, String runningScenario,
            List<String> queuedRuns) {
        this.snapshots = snapshots == null ? new ArrayList<>() : new ArrayList<>(snapshots);
        this.noScenarios = noScenarios;
        this.runningScenario = runningScenario == null ? "" : runningScenario;
        this.queuedRuns = queuedRuns == null ? new ArrayList<>() : new ArrayList<>(queuedRuns);
    }

    /** Returns refreshed scenario snapshots. */
    List<ScenarioStatusSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    /** Returns whether the scenario directory currently has no scenario folders. */
    boolean isNoScenarios() {
        return noScenarios;
    }

    /** Returns the currently inferred running scenario name, or empty when none. */
    String getRunningScenario() {
        return runningScenario;
    }

    /** Returns the queue list after refresh reconciliation. */
    List<String> getQueuedRuns() {
        return new ArrayList<>(queuedRuns);
    }
}