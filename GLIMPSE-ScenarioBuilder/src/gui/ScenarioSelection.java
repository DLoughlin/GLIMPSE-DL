package gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import javafx.collections.ObservableList;

final class ScenarioSelection {

    private final List<ScenarioRow> rows;
    private final List<String> scenarioNames;

    private ScenarioSelection(List<ScenarioRow> rows, List<String> scenarioNames) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.scenarioNames = Collections.unmodifiableList(new ArrayList<>(scenarioNames));
    }

    static ScenarioSelection capture() {
        List<ScenarioRow> selectedRows = new ArrayList<>();
        List<String> selectedNames = new ArrayList<>();
        if (ScenarioTable.tableScenariosLibrary != null && ScenarioTable.tableScenariosLibrary.getSelectionModel() != null) {
            ObservableList<ScenarioRow> selectedItems = ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedItems();
            if (selectedItems != null) {
                for (ScenarioRow row : selectedItems) {
                    if (row == null) {
                        continue;
                    }
                    selectedRows.add(row);
                    String scenarioName = normalizeScenarioName(row);
                    if (!scenarioName.isEmpty() && !selectedNames.contains(scenarioName)) {
                        selectedNames.add(scenarioName);
                    }
                }
            }
        }
        return new ScenarioSelection(selectedRows, selectedNames);
    }

    static String normalizeScenarioName(ScenarioRow row) {
        if (row == null) {
            return "";
        }
        String scenarioName = row.getScenarioName();
        return scenarioName == null ? "" : scenarioName.trim();
    }

    List<ScenarioRow> getRows() {
        return rows;
    }

    List<String> getScenarioNames() {
        return scenarioNames;
    }

    boolean isEmpty() {
        return rows.isEmpty();
    }

    int size() {
        return rows.size();
    }

    boolean hasSingleSelection() {
        return rows.size() == 1;
    }

    boolean hasTwoSelections() {
        return rows.size() == 2;
    }

    ScenarioRow firstRowOrNull() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    String firstScenarioNameOrEmpty() {
        return scenarioNames.isEmpty() ? "" : scenarioNames.get(0);
    }
}
