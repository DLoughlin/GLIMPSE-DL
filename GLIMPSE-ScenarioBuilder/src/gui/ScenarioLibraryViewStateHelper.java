package gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import glimpseElement.ScenarioRow;
import glimpseElement.ScenarioTable;
import glimpseUtil.GLIMPSEUtils;
import javafx.collections.ObservableList;

final class ScenarioLibraryViewStateHelper {

    private final GLIMPSEUtils utils;

    ScenarioLibraryViewStateHelper(GLIMPSEUtils utils) {
        this.utils = utils;
    }

    RefreshViewState capture() {
        if (ScenarioTable.tableScenariosLibrary == null) {
            return RefreshViewState.empty();
        }
        List<String> selectedScenarioNames = getCurrentSelectedScenarioNames();
        String focusedScenarioName = "";
        int focusedIndex = -1;
        if (ScenarioTable.tableScenariosLibrary.getFocusModel() != null) {
            focusedIndex = ScenarioTable.tableScenariosLibrary.getFocusModel().getFocusedIndex();
            if (focusedIndex >= 0 && focusedIndex < ScenarioTable.tableScenariosLibrary.getItems().size()) {
                focusedScenarioName = normalizeScenarioName(ScenarioTable.tableScenariosLibrary.getItems().get(focusedIndex));
            }
        }

        int anchorIndex = focusedIndex;
        if (anchorIndex < 0 && !selectedScenarioNames.isEmpty()) {
            for (int i = 0; i < ScenarioTable.tableScenariosLibrary.getItems().size(); i++) {
                ScenarioRow row = ScenarioTable.tableScenariosLibrary.getItems().get(i);
                if (selectedScenarioNames.contains(normalizeScenarioName(row))) {
                    anchorIndex = i;
                    break;
                }
            }
        }
        if (anchorIndex < 0 && !ScenarioTable.tableScenariosLibrary.getItems().isEmpty()) {
            anchorIndex = Math.max(0, ScenarioTable.tableScenariosLibrary.getSelectionModel().getSelectedIndex());
        }
        String anchorScenarioName = (anchorIndex >= 0 && anchorIndex < ScenarioTable.tableScenariosLibrary.getItems().size())
                ? normalizeScenarioName(ScenarioTable.tableScenariosLibrary.getItems().get(anchorIndex))
                : "";
        return new RefreshViewState(selectedScenarioNames, focusedScenarioName, anchorScenarioName, anchorIndex);
    }

    void applySnapshots(List<ScenarioStatusSnapshot> snapshots, boolean noScenarios, RefreshViewState pendingViewState,
            String noScenariosMessage, String readyMessage) {
        if (ScenarioTable.tableScenariosLibrary == null) {
            return;
        }

        preserveSelectionAndRefresh(() -> {
            RefreshViewState viewState = pendingViewState == null ? RefreshViewState.empty() : pendingViewState;
            Map<String, ScenarioRow> existingRowsByName = new LinkedHashMap<>();
            for (ScenarioRow row : ScenarioTable.listOfScenarioRuns) {
                if (row == null) {
                    continue;
                }
                String scenarioName = normalizeScenarioName(row);
                if (!scenarioName.isEmpty()) {
                    existingRowsByName.put(scenarioName, row);
                }
            }

            ObservableList<ScenarioRow> rows = ScenarioTable.listOfScenarioRuns;
            List<ScenarioStatusSnapshot> safeSnapshots = snapshots == null ? new ArrayList<>() : snapshots;
            List<String> snapshotNames = new ArrayList<>();
            for (ScenarioStatusSnapshot snapshot : safeSnapshots) {
                if (snapshot != null && snapshot.scenarioName != null && !snapshot.scenarioName.trim().isEmpty()) {
                    snapshotNames.add(snapshot.scenarioName.trim());
                }
            }

            if (hasSameScenarioOrdering(rows, snapshotNames)) {
                for (int i = 0; i < safeSnapshots.size(); i++) {
                    ScenarioStatusSnapshot snapshot = safeSnapshots.get(i);
                    if (snapshot == null) {
                        continue;
                    }
                    ScenarioRow row = rows.get(i);
                    applySnapshotToRow(row, snapshot);
                }
            } else {
                List<ScenarioRow> rebuiltRows = new ArrayList<>();
                for (ScenarioStatusSnapshot snapshot : safeSnapshots) {
                    if (snapshot == null) {
                        continue;
                    }
                    ScenarioRow row = existingRowsByName.get(snapshot.scenarioName);
                    if (row == null) {
                        row = new ScenarioRow(snapshot.scenarioName);
                    }
                    applySnapshotToRow(row, snapshot);
                    rebuiltRows.add(row);
                }
                rows.setAll(rebuiltRows);
            }

            ScenarioTable.tableScenariosLibrary.setPlaceholder(
                    utils.createLabel(noScenarios || safeSnapshots.isEmpty() ? noScenariosMessage : readyMessage));
            restore(viewState, getCurrentSelectedScenarioNames());
        });
    }

    private void applySnapshotToRow(ScenarioRow row, ScenarioStatusSnapshot snapshot) {
        if (row == null || snapshot == null) {
            return;
        }
        row.setComponents(snapshot.components);
        row.setCreatedDate(snapshot.createdDate);
        row.setCompletedDate(snapshot.completedDate);
        row.setStatus(snapshot.status);
        row.setRuntime(snapshot.runtime);
        row.setUnsolvedMarkets(snapshot.unsolved);
    }

    private boolean hasSameScenarioOrdering(List<ScenarioRow> rows, List<String> snapshotNames) {
        if (rows == null || snapshotNames == null || rows.size() != snapshotNames.size()) {
            return false;
        }
        for (int i = 0; i < rows.size(); i++) {
            ScenarioRow row = rows.get(i);
            String expected = snapshotNames.get(i);
            if (!normalizeScenarioName(row).equals(expected)) {
                return false;
            }
        }
        return true;
    }

    void preserveSelectionAndRefresh(Runnable updateAction) {
        if (ScenarioTable.tableScenariosLibrary == null) {
            if (updateAction != null) {
                updateAction.run();
            }
            return;
        }
        RefreshViewState viewState = capture();
        List<String> selectionBeforeUpdate = getCurrentSelectedScenarioNames();
        if (updateAction != null) {
            updateAction.run();
        }
        restore(viewState, selectionBeforeUpdate);
        ScenarioTable.tableScenariosLibrary.refresh();
    }

    private void restore(RefreshViewState viewState, List<String> selectionBeforeRefresh) {
        if (ScenarioTable.tableScenariosLibrary == null) {
            return;
        }
        RefreshViewState safeViewState = viewState == null ? RefreshViewState.empty() : viewState;
        List<String> currentSelection = getCurrentSelectedScenarioNames();
        List<String> restoredSelection = currentSelection;
        if (shouldRestoreSelection(safeViewState.selectedScenarioNames, selectionBeforeRefresh, currentSelection)) {
            restoredSelection = restoreSelectedScenarioNames(safeViewState.selectedScenarioNames, safeViewState.anchorScenarioName);
        }

        int anchorIndex = resolveAnchorIndex(safeViewState, restoredSelection);
        int focusedIndex = resolveFocusedIndex(safeViewState, restoredSelection, anchorIndex);

        if (anchorIndex >= 0 && anchorIndex < ScenarioTable.tableScenariosLibrary.getItems().size()) {
            ScenarioTable.tableScenariosLibrary.scrollTo(anchorIndex);
        }
        if (focusedIndex >= 0 && focusedIndex < ScenarioTable.tableScenariosLibrary.getItems().size()
                && ScenarioTable.tableScenariosLibrary.getFocusModel() != null) {
            ScenarioTable.tableScenariosLibrary.getFocusModel().focus(focusedIndex);
        }
    }

    private boolean shouldRestoreSelection(List<String> capturedSelection, List<String> selectionBeforeRefresh, List<String> currentSelection) {
        List<String> safeCapturedSelection = sanitizeScenarioNames(capturedSelection);
        List<String> safeBeforeRefresh = sanitizeScenarioNames(selectionBeforeRefresh);
        List<String> safeCurrentSelection = sanitizeScenarioNames(currentSelection);
        if (safeCapturedSelection.isEmpty()) {
            return safeCurrentSelection.isEmpty();
        }
        if (safeCurrentSelection.isEmpty()) {
            return true;
        }
        return safeCurrentSelection.equals(safeCapturedSelection) || safeCurrentSelection.equals(safeBeforeRefresh);
    }

    private List<String> restoreSelectedScenarioNames(List<String> scenarioNamesToRestore, String preferredLeadScenarioName) {
        List<String> restoredSelection = new ArrayList<>();
        if (ScenarioTable.tableScenariosLibrary == null || ScenarioTable.tableScenariosLibrary.getSelectionModel() == null) {
            return restoredSelection;
        }
        javafx.scene.control.TableView.TableViewSelectionModel<ScenarioRow> selectionModel = ScenarioTable.tableScenariosLibrary.getSelectionModel();
        selectionModel.clearSelection();
        List<String> sanitizedScenarioNames = sanitizeScenarioNames(scenarioNamesToRestore);
        if (sanitizedScenarioNames.isEmpty()) {
            return restoredSelection;
        }

        List<Integer> rowIndexesToSelect = new ArrayList<>();
        List<String> orderedScenarioNames = new ArrayList<>();
        for (int i = 0; i < ScenarioTable.tableScenariosLibrary.getItems().size(); i++) {
            ScenarioRow row = ScenarioTable.tableScenariosLibrary.getItems().get(i);
            String scenarioName = normalizeScenarioName(row);
            if (sanitizedScenarioNames.contains(scenarioName)) {
                rowIndexesToSelect.add(i);
                orderedScenarioNames.add(scenarioName);
            }
        }
        for (Integer rowIndex : rowIndexesToSelect) {
            selectionModel.select(rowIndex);
        }
        restoredSelection.addAll(orderedScenarioNames);

        int leadIndex = findScenarioRowIndexByName(preferredLeadScenarioName);
        if (leadIndex < 0 && !orderedScenarioNames.isEmpty()) {
            leadIndex = findScenarioRowIndexByName(orderedScenarioNames.get(0));
        }
        if (leadIndex >= 0) {
            selectionModel.select(leadIndex);
        }
        return restoredSelection;
    }

    private int resolveAnchorIndex(RefreshViewState viewState, List<String> restoredSelection) {
        int anchorIndex = findScenarioRowIndexByName(viewState.anchorScenarioName);
        if (anchorIndex >= 0) {
            return anchorIndex;
        }
        if (restoredSelection != null && !restoredSelection.isEmpty()) {
            int restoredLeadIndex = findScenarioRowIndexByName(restoredSelection.get(0));
            if (restoredLeadIndex >= 0) {
                return restoredLeadIndex;
            }
        }
        if (!ScenarioTable.tableScenariosLibrary.getItems().isEmpty()) {
            return Math.min(Math.max(viewState.anchorIndex, 0), ScenarioTable.tableScenariosLibrary.getItems().size() - 1);
        }
        return -1;
    }

    private int resolveFocusedIndex(RefreshViewState viewState, List<String> restoredSelection, int anchorIndex) {
        int focusedIndex = findScenarioRowIndexByName(viewState.focusedScenarioName);
        if (focusedIndex >= 0) {
            return focusedIndex;
        }
        if (restoredSelection != null && !restoredSelection.isEmpty()) {
            for (String scenarioName : restoredSelection) {
                int restoredIndex = findScenarioRowIndexByName(scenarioName);
                if (restoredIndex >= 0) {
                    return restoredIndex;
                }
            }
        }
        return anchorIndex;
    }

    private List<String> getCurrentSelectedScenarioNames() {
        return sanitizeScenarioNames(ScenarioSelection.capture().getScenarioNames());
    }

    private List<String> sanitizeScenarioNames(List<String> scenarioNames) {
        List<String> sanitized = new ArrayList<>();
        if (scenarioNames == null) {
            return sanitized;
        }
        for (String scenarioName : scenarioNames) {
            if (scenarioName == null) {
                continue;
            }
            String trimmed = scenarioName.trim();
            if (!trimmed.isEmpty() && !sanitized.contains(trimmed)) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    private int findScenarioRowIndexByName(String scenarioName) {
        if (ScenarioTable.tableScenariosLibrary == null || scenarioName == null || scenarioName.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < ScenarioTable.tableScenariosLibrary.getItems().size(); i++) {
            ScenarioRow row = ScenarioTable.tableScenariosLibrary.getItems().get(i);
            if (scenarioName.equals(normalizeScenarioName(row))) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeScenarioName(ScenarioRow row) {
        return ScenarioSelection.normalizeScenarioName(row);
    }

    static final class RefreshViewState {
        final List<String> selectedScenarioNames;
        final String focusedScenarioName;
        final String anchorScenarioName;
        final int anchorIndex;

        RefreshViewState(List<String> selectedScenarioNames, String focusedScenarioName, String anchorScenarioName, int anchorIndex) {
            this.selectedScenarioNames = selectedScenarioNames == null ? new ArrayList<>() : new ArrayList<>(selectedScenarioNames);
            this.focusedScenarioName = focusedScenarioName == null ? "" : focusedScenarioName;
            this.anchorScenarioName = anchorScenarioName == null ? "" : anchorScenarioName;
            this.anchorIndex = anchorIndex;
        }

        static RefreshViewState empty() {
            return new RefreshViewState(new ArrayList<>(), "", "", -1);
        }
    }
}