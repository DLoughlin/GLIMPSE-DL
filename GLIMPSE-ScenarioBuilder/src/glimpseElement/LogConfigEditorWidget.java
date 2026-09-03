/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 *
 * EXPORT CONTROL
 * User agrees that the Software will not be shipped, transferred or
 * exported into any country or used in any manner prohibited by the
 * United States Export Administration Act or any other applicable
 * export laws, restrictions or regulations (collectively the "Export Laws").
 * Export of the Software may require some form of license or other
 * authority from the U.S. Government, and failure to obtain such
 * export control license may result in criminal liability under
 * U.S. laws. In addition, if the Software is identified as export controlled
 * items under the Export Laws, User represents and warrants that User
 * is not a citizen, or otherwise located within, an embargoed nation
 * (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
 *     and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 *
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew
 * Binsted, and Pralit Patel.
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA).
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization
 * Laboratory contract.
 *
 */
package glimpseElement;

import glimpseBuilder.XMLModifier;
import glimpseUtil.FileChooserPlus;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.UtilsDialogs;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A focused editor for GCAM log_conf.xml that supports quick presets and
 * constrained advanced edits while keeping XML serialization safe.
 */
public class LogConfigEditorWidget {

    private static final String[] KNOWN_LOGGER_ORDER = {
        "main_log", "solver_log", "single_market_log", "worst_market_log", "calibration_log",
        "dependency_finder_log", "parallel-grain-log", "solver-data-log", "solver-data-key",
        "climate-log", "target_finder_log"
    };

    private static final String PRESET_MINIMAL = "Minimal";
    private static final String PRESET_NORMAL = "Normal";
    private static final String PRESET_DEBUG = "Debug";

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    private final ObservableList<LoggerModel> allLoggers = FXCollections.observableArrayList();
    private final ObservableList<LoggerModel> filteredLoggers = FXCollections.observableArrayList();
    private final Map<String, LoggerModel> baselineByName = new HashMap<String, LoggerModel>();

    private final TextField searchField = new TextField();
    private final ListView<LoggerModel> loggerListView = new ListView<LoggerModel>(filteredLoggers);

    private final Label loadedFileLabel = new Label("Loaded file: --");
    private final Label savedPathLabel = new Label("Last saved: --");
    private final Label statusLabel = new Label("Status: --");
    private final Label validationLabel = new Label("Validation: --");
    private final Button detailsButton = new Button("Details");

    private final CheckBox enabledCheckBox = new CheckBox("Enable logger");
    private final TextField outputFileField = new TextField();
    private final ComboBox<String> presetCombo = new ComboBox<String>();

    private final TextField advancedNameField = new TextField();
    private final TextField advancedTypeField = new TextField();
    private final ComboBox<String> printLogWarningCombo = new ComboBox<String>();
    private final ComboBox<String> minLogWarningCombo = new ComboBox<String>();
    private final ComboBox<String> minToScreenWarningCombo = new ComboBox<String>();
    private final TextArea headerMessageArea = new TextArea();

    private final TextArea warningsArea = new TextArea();
    private final TextArea previewArea = new TextArea();

    private Stage stage;
    private File currentFile;
    private Document xmlDocument;
    private boolean dirty = false;
    private boolean updatingUi = false;
    private String lastSavedDisplay = "--";

    private static final List<String> WARNING_LEVEL_OPTIONS = Arrays.asList(
            "1 (DEBUG) - Very detailed troubleshooting messages",
            "2 (NOTICE) - General informational messages",
            "3 (WARNING) - Potential problems that should be reviewed",
            "4 (ERROR) - Actual errors",
            "5 (SEVERE) - Critical problems; run may not continue",
            "6 (NONE)");

    private static final Map<String, LoggerDefaults> HARD_DEFAULTS = buildHardDefaults();

    public void createAndShow() {
        stage = new Stage();
        stage.setTitle("Edit Log Configuration");
        stage.initModality(Modality.WINDOW_MODAL);
        try {
            Window owner = UtilsDialogs.getPrimaryOwnerWindow();
            if (owner != null) {
                stage.initOwner(owner);
            }
        } catch (Exception ignored) {
        }

        Scene scene = new Scene(buildRoot(), 1120, 720);
        gui.ScenarioBuilder.applyModernTheme(scene);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            if (!confirmDiscardIfDirty("Close editor and discard unsaved changes?")) {
                e.consume();
            }
        });

        File defaultFile = resolveDefaultLogConfig();
        if (defaultFile != null) {
            loadFromFile(defaultFile);
        } else {
            updateHeaderLabels();
            validationLabel.setText("Validation: log_conf.xml path is not configured.");
            statusLabel.setText("Status: invalid");
            statusLabel.setStyle("-fx-text-fill: #b00020;");
            setDetailsButtonVisible(true);
        }

        stage.show();
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(buildHeader());
        root.setCenter(buildMainArea());
        root.setBottom(buildFooter());

        return root;
    }

    private VBox buildHeader() {
        Button openButton = new Button("Open");
        Button saveButton = new Button("Save");
        Button saveAsButton = new Button("Save As");
        Button revertButton = new Button("Revert");
        Button compareButton = new Button("Compare to Default");
        Button editRawButton = new Button("Open in Text Editor");

        openButton.setOnAction(e -> openConfigFile());
        saveButton.setOnAction(e -> saveCurrentFile());
        saveAsButton.setOnAction(e -> saveAsNewFile());
        revertButton.setOnAction(e -> {
            if (currentFile != null && confirmDiscardIfDirty("Revert to disk version and discard unsaved changes?")) {
                loadFromFile(currentFile);
            }
        });
        compareButton.setOnAction(e -> showDiffFromBaseline());
        editRawButton.setOnAction(e -> {
            if (currentFile != null) {
                files.showFileInTextEditor(currentFile.getAbsolutePath());
            }
        });
        detailsButton.setOnAction(e -> showValidationDetailsDialog());
        setDetailsButtonVisible(false);

        HBox buttonRow = new HBox(8, openButton, saveButton, saveAsButton, revertButton,
                compareButton, editRawButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        HBox statusRow = new HBox(8, statusLabel, validationLabel, detailsButton);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(6, buttonRow, loadedFileLabel, savedPathLabel, statusRow);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private BorderPane buildMainArea() {
        BorderPane pane = new BorderPane();

        pane.setLeft(buildLoggerListPanel());
        pane.setCenter(buildDetailsPanel());

        return pane;
    }

    private VBox buildLoggerListPanel() {
        searchField.setPromptText("Search logger name or output file");

        ChangeListener<Object> refreshFilter = (obs, oldVal, newVal) -> refilterLoggerList();
        searchField.textProperty().addListener(refreshFilter);

        loggerListView.setCellFactory(list -> new ListCell<LoggerModel>() {
            @Override
            protected void updateItem(LoggerModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String onOff = item.enabled ? "on" : "off";
                String output = safe(item.fileName);
                String levelSummary = "P/M/S " + safe(item.printLogWarningLevel) + "/"
                        + safe(item.minLogWarningLevel) + "/" + safe(item.minToScreenWarningLevel);
                setText(item.name + "  [" + onOff + "]\n" + output + "\n" + levelSummary);
                setStyle(item.enabled ? "-fx-text-fill: #1a7f37;" : "-fx-text-fill: #666666;");
            }
        });

        loggerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (oldSel != null) {
                applyUiToModel(oldSel);
            }
            populateDetails(newSel);
        });

        Button debugButton = new Button("Debug");
        Button quietButton = new Button("Quiet");
        Button defaultButton = new Button("Default");

        debugButton.setOnAction(e -> applyQuickDebugPreset());
        quietButton.setOnAction(e -> applyQuietProductionPreset());
        defaultButton.setOnAction(e -> restoreDefaults());

        HBox globalActions = new HBox(8, debugButton, quietButton, defaultButton);
        globalActions.setAlignment(Pos.CENTER_LEFT);

        VBox pane = new VBox(6,
                new Label("Logger list"),
                searchField,
                loggerListView,
                globalActions);
        pane.setPadding(new Insets(0, 10, 0, 0));
        pane.setPrefWidth(320);
        VBox.setVgrow(loggerListView, Priority.ALWAYS);
        return pane;
    }

    private TabPane buildDetailsPanel() {
        TabPane tabs = new TabPane();

        Tab detailsTab = new Tab("Logger Details", buildLoggerDetailsPane());
        Tab previewTab = new Tab("Preview", buildPreviewPane());

        detailsTab.setClosable(false);
        previewTab.setClosable(false);

        tabs.getTabs().addAll(detailsTab, previewTab);
        return tabs;
    }

    private VBox buildLoggerDetailsPane() {
        presetCombo.getItems().addAll(PRESET_MINIMAL, PRESET_NORMAL, PRESET_DEBUG);
        printLogWarningCombo.getItems().setAll(WARNING_LEVEL_OPTIONS);
        minLogWarningCombo.getItems().setAll(WARNING_LEVEL_OPTIONS);
        minToScreenWarningCombo.getItems().setAll(WARNING_LEVEL_OPTIONS);
        printLogWarningCombo.setEditable(false);
        minLogWarningCombo.setEditable(false);
        minToScreenWarningCombo.setEditable(false);

        advancedNameField.setEditable(false);
        advancedTypeField.setEditable(false);
        headerMessageArea.setPrefRowCount(5);

        applyTooltip(presetCombo, "Preset is optional; custom level combinations may leave this blank.");
        applyTooltip(printLogWarningCombo, "Controls what gets written to file.");
        applyTooltip(minLogWarningCombo, "Minimum severity retained by this logger.");
        applyTooltip(minToScreenWarningCombo, "Messages from this logger that appear in the console.");
        applyTooltip(headerMessageArea, "Prefix text attached to log entries.");

        enabledCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingUi) {
                return;
            }
            LoggerModel selected = loggerListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            selected.enabled = newVal.booleanValue();
            if (!selected.enabled) {
                selected.printLogWarningLevel = "6";
                selected.minLogWarningLevel = "6";
                selected.minToScreenWarningLevel = "6";
            } else if ("6".equals(safe(selected.printLogWarningLevel))
                    && "6".equals(safe(selected.minLogWarningLevel))
                    && "6".equals(safe(selected.minToScreenWarningLevel))) {
                applyPresetToModel(selected, PRESET_NORMAL);
            }
            populateDetails(selected);
            markDirtyAndRefresh();
        });

        presetCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingUi || newVal == null) {
                return;
            }
            LoggerModel selected = loggerListView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            applyPresetToModel(selected, newVal);
            populateDetails(selected);
            markDirtyAndRefresh();
        });

        outputFileField.textProperty().addListener((obs, oldVal, newVal) -> markDirtyIfUserEdit());
        printLogWarningCombo.valueProperty().addListener((obs, oldVal, newVal) -> markDirtyIfUserEdit());
        minLogWarningCombo.valueProperty().addListener((obs, oldVal, newVal) -> markDirtyIfUserEdit());
        minToScreenWarningCombo.valueProperty().addListener((obs, oldVal, newVal) -> markDirtyIfUserEdit());
        headerMessageArea.textProperty().addListener((obs, oldVal, newVal) -> markDirtyIfUserEdit());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int r = 0;
        grid.add(enabledCheckBox, 0, r++, 2, 1);
        grid.add(new Label("Verbosity preset:"), 0, r);
        grid.add(presetCombo, 1, r++);
        grid.add(new Label("Name:"), 0, r);
        grid.add(advancedNameField, 1, r++);
        grid.add(new Label("Type:"), 0, r);
        grid.add(advancedTypeField, 1, r++);
        grid.add(new Label("FileName:"), 0, r);
        grid.add(outputFileField, 1, r++);
        grid.add(new Separator(), 0, r++, 2, 1);
        grid.add(new Label("printLogWarningLevel:"), 0, r);
        grid.add(printLogWarningCombo, 1, r++);
        grid.add(new Label("minLogWarningLevel:"), 0, r);
        grid.add(minLogWarningCombo, 1, r++);
        grid.add(new Label("minToScreenWarningLevel:"), 0, r);
        grid.add(minToScreenWarningCombo, 1, r++);
        grid.add(new Separator(), 0, r++, 2, 1);
        grid.add(new Label("headerMessage:"), 0, r);
        grid.add(headerMessageArea, 1, r++);

        VBox box = new VBox(10, grid);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildPreviewPane() {
        warningsArea.setEditable(false);
        warningsArea.setWrapText(true);
        warningsArea.setPrefRowCount(6);

        previewArea.setEditable(false);
        previewArea.setWrapText(false);

        VBox box = new VBox(8,
                new Label("Warnings"), warningsArea,
                new Label("XML preview"), previewArea);
        box.setPadding(new Insets(10));
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        return box;
    }

    private HBox buildFooter() {
        Button closeButton = new Button("Close");
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> {
            if (confirmDiscardIfDirty("Close editor and discard unsaved changes?")) {
                stage.close();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(8, spacer, closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        return footer;
    }

    private void openConfigFile() {
        if (!confirmDiscardIfDirty("Open another file and discard unsaved changes?")) {
            return;
        }
        File initialDir = currentFile != null ? currentFile.getParentFile() : resolveDefaultParentDir();
        File selected = FileChooserPlus.showOpenDialog(
                UtilsDialogs.getPrimaryOwnerWindow(),
                "Open log configuration XML",
                initialDir,
                new javafx.stage.FileChooser.ExtensionFilter("XML Files", "*.xml"));
        if (selected != null) {
            loadFromFile(selected);
        }
    }

    private void saveCurrentFile() {
        if (currentFile == null) {
            saveAsNewFile();
            return;
        }
        persistToFile(currentFile);
    }

    private void saveAsNewFile() {
        File initialDir = currentFile != null ? currentFile.getParentFile() : resolveDefaultParentDir();
        String initialName = currentFile != null ? currentFile.getName() : "log_conf.xml";
        File chosen = FileChooserPlus.showSaveDialog(
                UtilsDialogs.getPrimaryOwnerWindow(),
                "Save log configuration XML",
                initialDir,
                initialName,
                new javafx.stage.FileChooser.ExtensionFilter("XML Files", "*.xml"));
        if (chosen != null) {
            persistToFile(chosen);
        }
    }

    private void loadFromFile(File file) {
        if (file == null) {
            return;
        }
        Document doc = XMLModifier.openXmlDocument(file);
        if (doc == null) {
            showError("Unable to open XML file", "Could not parse: " + file.getAbsolutePath());
            return;
        }

        this.currentFile = file;
        this.xmlDocument = doc;
        allLoggers.clear();
        filteredLoggers.clear();
        baselineByName.clear();

        List<LoggerModel> parsed = parseLoggerModels(doc);
        allLoggers.addAll(sortLoggers(parsed));
        for (LoggerModel model : allLoggers) {
            baselineByName.put(model.name, model.copyWithoutElement());
        }

        dirty = false;
        lastSavedDisplay = "--";
        refilterLoggerList();
        if (!filteredLoggers.isEmpty()) {
            loggerListView.getSelectionModel().select(0);
        } else {
            populateDetails(null);
        }
        updateStatusAndPreview();
        updateHeaderLabels();
    }

    private void persistToFile(File outFile) {
        applyCurrentSelectionToModel();
        List<String> errors = validateModels();
        if (!errors.isEmpty()) {
            showError("Cannot save invalid configuration", String.join("\n", errors));
            return;
        }
        applyModelsToDocument(xmlDocument, allLoggers);
        XMLModifier.writeXmlDocument(xmlDocument, outFile.getAbsolutePath());
        currentFile = outFile;
        dirty = false;
        lastSavedDisplay = outFile.getAbsolutePath() + " @ "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        updateHeaderLabels();
        updateStatusAndPreview();
    }

    private void restoreDefaults() {
        if (allLoggers.isEmpty()) {
            return;
        }
        for (LoggerModel current : allLoggers) {
            LoggerDefaults defaults = HARD_DEFAULTS.get(current.name);
            if (defaults != null) {
                defaults.applyTo(current);
            }
        }
        LoggerModel selected = loggerListView.getSelectionModel().getSelectedItem();
        populateDetails(selected);
        markDirtyAndRefresh();
    }

    private void showDiffFromBaseline() {
        applyCurrentSelectionToModel();
        StringBuilder diff = new StringBuilder();
        for (LoggerModel logger : allLoggers) {
            LoggerDefaults defaults = HARD_DEFAULTS.get(logger.name);
            if (defaults == null) {
                diff.append("No hard default exists for logger: ").append(logger.name).append("\n");
                continue;
            }
            appendFieldDiff(diff, logger.name, "Type", defaults.type, logger.type);
            appendFieldDiff(diff, logger.name, "FileName", defaults.fileName, logger.fileName);
            appendFieldDiff(diff, logger.name, "printLogWarningLevel", defaults.printLogWarningLevel, logger.printLogWarningLevel);
            appendFieldDiff(diff, logger.name, "minLogWarningLevel", defaults.minLogWarningLevel, logger.minLogWarningLevel);
            appendFieldDiff(diff, logger.name, "minToScreenWarningLevel", defaults.minToScreenWarningLevel, logger.minToScreenWarningLevel);
            appendFieldDiff(diff, logger.name, "headerMessage", defaults.headerMessage, logger.headerMessage);
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Compare to Default");
        alert.setHeaderText("Differences from hard-coded defaults");
        alert.getDialogPane().setExpandableContent(new TextArea(diff.length() == 0 ? "No differences." : diff.toString()));
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private void appendFieldDiff(StringBuilder diff, String loggerName, String field, String before, String after) {
        String b = safe(before);
        String a = safe(after);
        if (!b.equals(a)) {
            diff.append(loggerName)
                .append(" :: ")
                .append(field)
                .append(" = '")
                .append(b)
                .append("' -> '")
                .append(a)
                .append("'\n");
        }
    }

    private void applyQuickDebugPreset() {
        applyNamedPreset("solver_log", PRESET_DEBUG, true);
        applyNamedPreset("single_market_log", PRESET_DEBUG, true);
        applyNamedPreset("worst_market_log", PRESET_DEBUG, true);
        markDirtyAndRefresh();
    }

    private void applyQuietProductionPreset() {
        applyNamedPreset("single_market_log", PRESET_MINIMAL, false);
        applyNamedPreset("worst_market_log", PRESET_MINIMAL, false);
        applyNamedPreset("dependency_finder_log", PRESET_MINIMAL, false);
        applyNamedPreset("solver-data-log", PRESET_MINIMAL, false);
        applyNamedPreset("solver-data-key", PRESET_MINIMAL, false);
        applyNamedPreset("main_log", PRESET_NORMAL, true);
        markDirtyAndRefresh();
    }

    private void applyNamedPreset(String loggerName, String preset, boolean enabled) {
        for (LoggerModel model : allLoggers) {
            if (loggerName.equalsIgnoreCase(model.name)) {
                model.enabled = enabled;
                if (enabled) {
                    applyPresetToModel(model, preset);
                } else {
                    model.printLogWarningLevel = "6";
                    model.minLogWarningLevel = "6";
                    model.minToScreenWarningLevel = "6";
                }
                break;
            }
        }
        populateDetails(loggerListView.getSelectionModel().getSelectedItem());
    }

    private void refilterLoggerList() {
        applyCurrentSelectionToModel();
        filteredLoggers.clear();
        for (LoggerModel logger : allLoggers) {
            if (matchesFilter(logger)) {
                filteredLoggers.add(logger);
            }
        }

        LoggerModel selected = loggerListView.getSelectionModel().getSelectedItem();
        if (selected != null && !filteredLoggers.contains(selected) && !filteredLoggers.isEmpty()) {
            loggerListView.getSelectionModel().select(0);
        } else if (selected == null && !filteredLoggers.isEmpty()) {
            loggerListView.getSelectionModel().select(0);
        }
        loggerListView.refresh();
    }

    private boolean matchesFilter(LoggerModel logger) {
        String search = safe(searchField.getText()).toLowerCase(Locale.ROOT);
        if (!search.isEmpty()) {
            String haystack = (safe(logger.name) + " " + safe(logger.fileName)).toLowerCase(Locale.ROOT);
            if (!haystack.contains(search)) {
                return false;
            }
        }
        return true;
    }

    private void populateDetails(LoggerModel model) {
        updatingUi = true;
        try {
            if (model == null) {
                enabledCheckBox.setSelected(false);
                outputFileField.setText("");
                presetCombo.getSelectionModel().clearSelection();
                advancedNameField.setText("");
                advancedTypeField.setText("");
                printLogWarningCombo.getSelectionModel().clearSelection();
                minLogWarningCombo.getSelectionModel().clearSelection();
                minToScreenWarningCombo.getSelectionModel().clearSelection();
                headerMessageArea.setText("");
                return;
            }

            enabledCheckBox.setSelected(model.enabled);
            outputFileField.setText(safe(model.fileName));
            advancedNameField.setText(safe(model.name));
            advancedTypeField.setText(safe(model.type));
            selectLevelValueOrDefault(printLogWarningCombo, model.printLogWarningLevel);
            selectLevelValueOrDefault(minLogWarningCombo, model.minLogWarningLevel);
            selectLevelValueOrDefault(minToScreenWarningCombo, model.minToScreenWarningLevel);
            headerMessageArea.setText(safe(model.headerMessage));
            presetCombo.getSelectionModel().select(detectPreset(model));
        } finally {
            updatingUi = false;
        }
    }

    private void applyCurrentSelectionToModel() {
        LoggerModel selected = loggerListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            applyUiToModel(selected);
        }
    }

    private void applyUiToModel(LoggerModel model) {
        if (updatingUi || model == null) {
            return;
        }
        model.enabled = enabledCheckBox.isSelected();
        model.fileName = safe(outputFileField.getText()).trim();
        model.printLogWarningLevel = optionToLevelValue(printLogWarningCombo.getSelectionModel().getSelectedItem());
        model.minLogWarningLevel = optionToLevelValue(minLogWarningCombo.getSelectionModel().getSelectedItem());
        model.minToScreenWarningLevel = optionToLevelValue(minToScreenWarningCombo.getSelectionModel().getSelectedItem());
        model.headerMessage = safe(headerMessageArea.getText());
    }

    private void markDirtyIfUserEdit() {
        if (updatingUi) {
            return;
        }
        markDirtyAndRefresh();
    }

    private void markDirtyAndRefresh() {
        applyCurrentSelectionToModel();
        dirty = true;
        updateStatusAndPreview();
        loggerListView.refresh();
    }

    private void updateStatusAndPreview() {
        applyCurrentSelectionToModel();
        List<String> warnings = validateModels();
        boolean valid = warnings.isEmpty();

        if (valid) {
            validationLabel.setText("Validation: no blocking issues");
            statusLabel.setText("Status: " + (dirty ? "unsaved changes" : "valid"));
            statusLabel.setStyle(dirty ? "-fx-text-fill: #b8860b;" : "-fx-text-fill: #1a7f37;");
        } else {
            validationLabel.setText("Validation: " + warnings.size() + " issue(s)");
            statusLabel.setText("Status: invalid");
            statusLabel.setStyle("-fx-text-fill: #b00020;");
        }
        setDetailsButtonVisible(!valid);

        warningsArea.setText(warnings.isEmpty() ? "No warnings." : String.join("\n", warnings));
        previewArea.setText(buildXmlPreviewText());
        updateHeaderLabels();
    }

    private void setDetailsButtonVisible(boolean visible) {
        detailsButton.setVisible(visible);
        detailsButton.setManaged(visible);
    }

    private void updateHeaderLabels() {
        loadedFileLabel.setText("Loaded file: " + (currentFile == null ? "--" : currentFile.getAbsolutePath()));
        savedPathLabel.setText("Last saved: " + lastSavedDisplay);
    }

    private List<String> validateModels() {
        List<String> issues = new ArrayList<String>();
        Set<String> usedFiles = new LinkedHashSet<String>();

        if (allLoggers.isEmpty()) {
            issues.add("No logger sections were found in the XML file.");
        }

        for (LoggerModel logger : allLoggers) {
            if (isBlank(logger.name)) {
                issues.add("Logger is missing required field: name");
            }
            if (isBlank(logger.type)) {
                issues.add("Logger '" + safe(logger.name) + "' is missing required field: type");
            }
            if (isBlank(logger.fileName)) {
                issues.add("Logger '" + safe(logger.name) + "' is missing required field: FileName");
            }

            Integer print = parseIntField(logger, "printLogWarningLevel", logger.printLogWarningLevel, issues);
            Integer min = parseIntField(logger, "minLogWarningLevel", logger.minLogWarningLevel, issues);
            Integer screen = parseIntField(logger, "minToScreenWarningLevel", logger.minToScreenWarningLevel, issues);

            if (print != null && !isInRange(print.intValue())) {
                issues.add("Logger '" + safe(logger.name) + "' has printLogWarningLevel outside expected range (1 to 6).");
            }
            if (min != null && !isInRange(min.intValue())) {
                issues.add("Logger '" + safe(logger.name) + "' has minLogWarningLevel outside expected range (1 to 6).");
            }
            if (screen != null && !isInRange(screen.intValue())) {
                issues.add("Logger '" + safe(logger.name) + "' has minToScreenWarningLevel outside expected range (1 to 6).");
            }

            if (screen != null && min != null && screen.intValue() < min.intValue()) {
                issues.add("Logger '" + safe(logger.name)
                        + "' has minToScreenWarningLevel lower than minLogWarningLevel.");
            }

            String key = safe(logger.fileName).trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && usedFiles.contains(key)) {
                issues.add("Two or more loggers write to the same file: " + logger.fileName);
            }
            if (!key.isEmpty()) {
                usedFiles.add(key);
            }
        }

        try {
            buildXmlPreviewText();
        } catch (Exception e) {
            issues.add("XML preview failed: " + e.getMessage());
        }

        return issues;
    }

    private void showValidationDetailsDialog() {
        applyCurrentSelectionToModel();
        List<String> issues = validateModels();

        AlertType type = issues.isEmpty() ? AlertType.INFORMATION : AlertType.WARNING;
        Alert alert = new Alert(type);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Validation Details");
        alert.setHeaderText(issues.isEmpty() ? "No validation issues were found." : "Current validation issues");

        TextArea detailsArea = new TextArea(issues.isEmpty() ? "No issues." : String.join("\n", issues));
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPrefRowCount(12);
        alert.getDialogPane().setExpandableContent(detailsArea);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private boolean isInRange(int value) {
        return value >= 1 && value <= 6;
    }

    private static Map<String, LoggerDefaults> buildHardDefaults() {
        Map<String, LoggerDefaults> defaults = new HashMap<String, LoggerDefaults>();
        putHardDefault(defaults, "main_log", "PlainTextLogger", "logs/main_log.txt", "1", "1", "2", "{date}:{time}");
        putHardDefault(defaults, "worst_market_log", "PlainTextLogger", "logs/worst_market_log.txt", "1", "1", "6", "{date}:{time}");
        putHardDefault(defaults, "single_market_log", "PlainTextLogger", "logs/single_market_log.txt", "1", "1", "4", "{date}:{time}");
        putHardDefault(defaults, "solver_log", "PlainTextLogger", "logs/solver_log.csv", "1", "3", "4", "{date}:{time}");
        putHardDefault(defaults, "calibration_log", "PlainTextLogger", "logs/calibration_log.txt", "1", "1", "4", "{date}:{time}");
        putHardDefault(defaults, "dependency_finder_log", "PlainTextLogger", "logs/dependency_finder_log.txt", "1", "1", "4", "{date}:{time}");
        putHardDefault(defaults, "parallel-grain-log", "PlainTextLogger", "logs/parallel-grain-log.txt", "1", "1", "4", "{date}:{time}");
        putHardDefault(defaults, "solver-data-log", "PlainTextLogger", "logs/solver-data-log.txt", "1", "6", "6", "{date}:{time}");
        putHardDefault(defaults, "solver-data-key", "PlainTextLogger", "logs/solver-data-key.txt", "1", "6", "6", "{date}:{time}");
        putHardDefault(defaults, "climate-log", "PlainTextLogger", "logs/climate-log.txt", "1", "1", "4", "{date}:{time}");
        putHardDefault(defaults, "target_finder_log", "PlainTextLogger", "logs/target_finder_log.txt", "1", "1", "4", "{date}:{time}");
        return Collections.unmodifiableMap(defaults);
    }

    private static void putHardDefault(Map<String, LoggerDefaults> map, String name, String type, String fileName,
            String printLevel, String minLogLevel, String minScreenLevel, String headerMessage) {
        map.put(name, new LoggerDefaults(type, fileName, printLevel, minLogLevel, minScreenLevel, headerMessage));
    }

    private Integer parseIntField(LoggerModel logger, String fieldName, String value, List<String> issues) {
        String trimmed = safe(value).trim();
        if (trimmed.isEmpty()) {
            issues.add("Logger '" + safe(logger.name) + "' is missing required field: " + fieldName);
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(trimmed));
        } catch (NumberFormatException nfe) {
            issues.add("Logger '" + safe(logger.name) + "' has non-integer " + fieldName + ": " + trimmed);
            return null;
        }
    }

    private void selectLevelValueOrDefault(ComboBox<String> comboBox, String value) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            normalized = "6";
        }
        comboBox.getSelectionModel().select(levelValueToOption(normalized));
    }

    private String levelValueToOption(String value) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            normalized = "6";
        }
        for (String option : WARNING_LEVEL_OPTIONS) {
            if (option.startsWith(normalized + " ") || option.equals(normalized)) {
                return option;
            }
        }
        return "6 (NONE)";
    }

    private String optionToLevelValue(String option) {
        String normalized = safe(option).trim();
        if (normalized.isEmpty()) {
            return "6";
        }
        int spaceIdx = normalized.indexOf(' ');
        if (spaceIdx > 0) {
            return normalized.substring(0, spaceIdx).trim();
        }
        return normalized;
    }

    private String buildXmlPreviewText() {
        if (xmlDocument == null) {
            return "";
        }

        Document previewDoc = (Document) xmlDocument.cloneNode(true);
        applyModelsToDocument(previewDoc, allLoggers);

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            t.transform(new DOMSource(previewDoc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception ex) {
            return "Unable to generate XML preview: " + ex.getMessage();
        }
    }

    private void applyModelsToDocument(Document doc, List<LoggerModel> models) {
        List<Element> loggerElements = getLoggerElements(doc);
        int count = Math.min(loggerElements.size(), models.size());
        for (int i = 0; i < count; i++) {
            models.get(i).applyToElement(loggerElements.get(i));
        }
    }

    private List<LoggerModel> parseLoggerModels(Document doc) {
        List<LoggerModel> list = new ArrayList<LoggerModel>();
        List<Element> loggerElements = getLoggerElements(doc);
        for (Element loggerEl : loggerElements) {
            list.add(LoggerModel.fromElement(loggerEl));
        }
        return list;
    }

    private List<Element> getLoggerElements(Document doc) {
        List<Element> out = new ArrayList<Element>();
        if (doc == null) {
            return out;
        }
        NodeList nodeList = doc.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element el = (Element) node;
                if ("logger".equalsIgnoreCase(el.getTagName())) {
                    out.add(el);
                }
            }
        }
        return out;
    }

    private List<LoggerModel> sortLoggers(List<LoggerModel> input) {
        List<LoggerModel> sorted = new ArrayList<LoggerModel>();
        List<LoggerModel> remaining = new ArrayList<LoggerModel>(input);

        for (String known : KNOWN_LOGGER_ORDER) {
            for (int i = 0; i < remaining.size(); i++) {
                if (known.equalsIgnoreCase(remaining.get(i).name)) {
                    sorted.add(remaining.remove(i));
                    break;
                }
            }
        }
        sorted.addAll(remaining);
        return sorted;
    }

    private void applyPresetToModel(LoggerModel model, String preset) {
        if (model == null || preset == null) {
            return;
        }
        if (PRESET_MINIMAL.equals(preset)) {
            model.printLogWarningLevel = "3";
            model.minLogWarningLevel = "3";
            model.minToScreenWarningLevel = "4";
        } else if (PRESET_NORMAL.equals(preset)) {
            model.printLogWarningLevel = "2";
            model.minLogWarningLevel = "2";
            model.minToScreenWarningLevel = "3";
        } else if (PRESET_DEBUG.equals(preset)) {
            model.printLogWarningLevel = "1";
            model.minLogWarningLevel = "1";
            model.minToScreenWarningLevel = "2";
        }
    }

    private String detectPreset(LoggerModel model) {
        String p = safe(model.printLogWarningLevel).trim();
        String m = safe(model.minLogWarningLevel).trim();
        String s = safe(model.minToScreenWarningLevel).trim();
        if ("3".equals(p) && "3".equals(m) && "4".equals(s)) {
            return PRESET_MINIMAL;
        }
        if ("2".equals(p) && "2".equals(m) && "3".equals(s)) {
            return PRESET_NORMAL;
        }
        if ("1".equals(p) && "1".equals(m) && "2".equals(s)) {
            return PRESET_DEBUG;
        }
        return null;
    }

    private boolean confirmDiscardIfDirty(String message) {
        if (!dirty) {
            return true;
        }
        Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
        UtilsDialogs.initDialogOwner(alert);
        alert.setTitle("Log Configuration Editor");
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void applyTooltip(javafx.scene.control.Control node, String text) {
        node.setTooltip(new Tooltip(text));
    }

    private File resolveDefaultLogConfig() {
        if (vars.getgCamExecutableDir() == null || vars.getgCamExecutableDir().trim().isEmpty()) {
            return null;
        }
        return Paths.get(vars.getgCamExecutableDir(), "log_conf.xml").toFile();
    }

    private File resolveDefaultParentDir() {
        File defaultFile = resolveDefaultLogConfig();
        if (defaultFile != null && defaultFile.getParentFile() != null) {
            return defaultFile.getParentFile();
        }
        return new File(System.getProperty("user.home", "."));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static final class LoggerModel {
        private Element element;
        private String name;
        private String type;
        private String fileName;
        private String printLogWarningLevel;
        private String minLogWarningLevel;
        private String minToScreenWarningLevel;
        private String headerMessage;
        private boolean enabled;

        static LoggerModel fromElement(Element element) {
            LoggerModel m = new LoggerModel();
            m.element = element;
            m.name = readAttr(element, "name");
            m.type = readAttr(element, "type");
            m.fileName = readChildText(element, "FileName");
            m.printLogWarningLevel = normalizeLevelValue(readChildText(element, "printLogWarningLevel"));
            m.minLogWarningLevel = normalizeLevelValue(readChildText(element, "minLogWarningLevel"));
            m.minToScreenWarningLevel = normalizeLevelValue(readChildText(element, "minToScreenWarningLevel"));
            m.headerMessage = readChildText(element, "headerMessage");
            m.enabled = !"6".equals(safe(m.printLogWarningLevel).trim())
                    || !"6".equals(safe(m.minLogWarningLevel).trim())
                    || !"6".equals(safe(m.minToScreenWarningLevel).trim());
            return m;
        }

        LoggerModel copyWithoutElement() {
            LoggerModel c = new LoggerModel();
            c.name = name;
            c.type = type;
            c.fileName = fileName;
            c.printLogWarningLevel = printLogWarningLevel;
            c.minLogWarningLevel = minLogWarningLevel;
            c.minToScreenWarningLevel = minToScreenWarningLevel;
            c.headerMessage = headerMessage;
            c.enabled = enabled;
            return c;
        }

        void copyValuesFrom(LoggerModel other) {
            if (other == null) {
                return;
            }
            this.name = other.name;
            this.type = other.type;
            this.fileName = other.fileName;
            this.printLogWarningLevel = other.printLogWarningLevel;
            this.minLogWarningLevel = other.minLogWarningLevel;
            this.minToScreenWarningLevel = other.minToScreenWarningLevel;
            this.headerMessage = other.headerMessage;
            this.enabled = other.enabled;
        }

        void applyToElement(Element loggerElement) {
            if (loggerElement == null) {
                return;
            }
            writeAttrIfPresent(loggerElement, "name", name);
            writeAttrIfPresent(loggerElement, "type", type);
            writeChildText(loggerElement, "FileName", fileName);
            writeChildText(loggerElement, "printLogWarningLevel", printLogWarningLevel);
            writeChildText(loggerElement, "minLogWarningLevel", minLogWarningLevel);
            writeChildText(loggerElement, "minToScreenWarningLevel", minToScreenWarningLevel);
            writeChildText(loggerElement, "headerMessage", headerMessage);
        }

        private static String readAttr(Element element, String key) {
            if (element == null || key == null) {
                return "";
            }
            return element.hasAttribute(key) ? element.getAttribute(key) : "";
        }

        private static void writeAttrIfPresent(Element element, String key, String value) {
            if (element == null || key == null) {
                return;
            }
            if (value != null) {
                element.setAttribute(key, value);
            }
        }

        private static String readChildText(Element parent, String tagName) {
            Element child = getChildByTagIgnoreCase(parent, tagName);
            return child == null ? "" : safe(child.getTextContent());
        }

        private static void writeChildText(Element parent, String tagName, String value) {
            Element child = getChildByTagIgnoreCase(parent, tagName);
            if (child == null) {
                child = parent.getOwnerDocument().createElement(tagName);
                parent.appendChild(child);
            }
            child.setTextContent(safe(value));
        }

        private static Element getChildByTagIgnoreCase(Element parent, String tagName) {
            if (parent == null || tagName == null) {
                return null;
            }
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element el = (Element) child;
                    if (tagName.equalsIgnoreCase(el.getTagName())) {
                        return el;
                    }
                }
            }
            return null;
        }

        private static String safe(String s) {
            return s == null ? "" : s;
        }

        private static String normalizeLevelValue(String value) {
            String trimmed = safe(value).trim();
            if (trimmed.isEmpty()) {
                return "6";
            }
            if ("-1".equals(trimmed) || "0".equals(trimmed)) {
                return "1";
            }
            return trimmed;
        }
    }

    private static final class LoggerDefaults {
        private final String type;
        private final String fileName;
        private final String printLogWarningLevel;
        private final String minLogWarningLevel;
        private final String minToScreenWarningLevel;
        private final String headerMessage;

        private LoggerDefaults(String type, String fileName, String printLogWarningLevel,
                String minLogWarningLevel, String minToScreenWarningLevel, String headerMessage) {
            this.type = type;
            this.fileName = fileName;
            this.printLogWarningLevel = printLogWarningLevel;
            this.minLogWarningLevel = minLogWarningLevel;
            this.minToScreenWarningLevel = minToScreenWarningLevel;
            this.headerMessage = headerMessage;
        }

        private void applyTo(LoggerModel model) {
            if (model == null) {
                return;
            }
            model.type = type;
            model.fileName = fileName;
            model.printLogWarningLevel = printLogWarningLevel;
            model.minLogWarningLevel = minLogWarningLevel;
            model.minToScreenWarningLevel = minToScreenWarningLevel;
            model.headerMessage = headerMessage;
            model.enabled = !"6".equals(printLogWarningLevel)
                    || !"6".equals(minLogWarningLevel)
                    || !"6".equals(minToScreenWarningLevel);
        }
    }
}
