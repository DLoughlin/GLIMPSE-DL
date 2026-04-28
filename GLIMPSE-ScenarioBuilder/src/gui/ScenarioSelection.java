package gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import javafx.collections.ObservableList;

/**
 * Immutable snapshot of the current Scenario Library table selection.
 * <p>
 * Stores selected rows and normalized names so callers can safely reuse selection
 * context across async operations and UI refreshes.
 */
final class ScenarioSelection {

    private final List<ScenarioRow> rows;
    private final List<String> scenarioNames;

    private ScenarioSelection(List<ScenarioRow> rows, List<String> scenarioNames) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.scenarioNames = Collections.unmodifiableList(new ArrayList<>(scenarioNames));
    }

    /** Captures the currently selected scenario rows and normalized scenario names. */
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

    /** Normalizes a row's scenario name to a non-null trimmed value. */
    static String normalizeScenarioName(ScenarioRow row) {
        if (row == null) {
            return "";
        }
        String scenarioName = row.getScenarioName();
        return scenarioName == null ? "" : scenarioName.trim();
    }

    /** Returns selected scenario rows captured at snapshot time. */
    List<ScenarioRow> getRows() {
        return rows;
    }

    /** Returns normalized scenario names captured at snapshot time. */
    List<String> getScenarioNames() {
        return scenarioNames;
    }

    /** Returns {@code true} when no rows were selected. */
    boolean isEmpty() {
        return rows.isEmpty();
    }

    /** Returns number of selected rows. */
    int size() {
        return rows.size();
    }

    /** Returns {@code true} when exactly one scenario is selected. */
    boolean hasSingleSelection() {
        return rows.size() == 1;
    }

    /** Returns {@code true} when exactly two scenarios are selected. */
    boolean hasTwoSelections() {
        return rows.size() == 2;
    }

    /** Returns the first selected row, or {@code null} if selection is empty. */
    ScenarioRow firstRowOrNull() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Returns the first selected scenario name, or an empty string when none is selected. */
    String firstScenarioNameOrEmpty() {
        return scenarioNames.isEmpty() ? "" : scenarioNames.get(0);
    }
}
