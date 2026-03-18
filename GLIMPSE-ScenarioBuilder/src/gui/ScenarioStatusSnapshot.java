package gui;

final class ScenarioStatusSnapshot {
    final String scenarioName;
    final String components;
    final String createdDate;
    final String completedDate;
    final String status;
    final String runtime;
    final String unsolved;

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
