package gui;

/**
 * Immutable snapshot of one scenario row's derived status fields.
 * <p>
 * Used as a transport object between status computation and table rendering.
 */
final class ScenarioStatusSnapshot {
    final String scenarioName;
    final String components;
    final String createdDate;
    final String completedDate;
    final String status;
    final String runtime;
    final String unsolved;

    /**
     * Creates a snapshot using non-null string values.
     */
    ScenarioStatusSnapshot(String scenarioName, String components, String createdDate, String completedDate,
            String status, String runtime, String unsolved) {
        this.scenarioName = scenarioName == null ? "" : scenarioName;
        this.components = components == null ? "" : components;
        this.createdDate = createdDate == null ? "" : createdDate;
        this.completedDate = completedDate == null ? "" : completedDate;
        this.status = status == null ? "" : status;
        this.runtime = runtime == null ? "" : runtime;
        this.unsolved = unsolved == null ? "" : unsolved;
    }
}
