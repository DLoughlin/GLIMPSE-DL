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
package glimpseBuilder;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import glimpseUtil.ModelInterfaceFontSizeOptions;
import gui.Client;
import gui.ScenarioLibraryReportHelper;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Manages the setup of the "View" menu in the GLIMPSE application.
 */
public final class SetupMenuView {

    private static final List<Integer> ALLOWED_FONT_SIZES = ModelInterfaceFontSizeOptions.getAllowedFontSizes();

    private final GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private final GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private final GLIMPSEFiles files = GLIMPSEFiles.getInstance();

    public void setup(Menu menuView) {
        Menu menuResourceLogs = new Menu("Resource Logs");
        Menu menuBrowseFolders = new Menu("Browse Folder");

        // --- Main Log Viewing Items ---
        String mainLogPath = Paths.get(vars.getgCamExecutableDir(), "logs", "main_log.txt").toString();
        menuView.getItems().addAll(
            createMenuItem("Current Main Log", () -> files.showFileInTextEditor(mainLogPath)),
            createMenuItem("Errors in Main Log", () -> {
                utils.showTextErrorReport(
                    ScenarioLibraryReportHelper.createMainLogErrorTextReport(utils, mainLogPath, null),
                    910,
                    600);
            }),
            createMenuItem("Current Solver Log", () -> files.showFileInTextEditor(Paths.get(vars.getgCamExecutableDir(), "logs", "solver_log.csv").toString())),
            createMenuItem("Current Worst Market Log", () -> files.showFileInTextEditor(Paths.get(vars.getgCamExecutableDir(), "logs", "worst_market_log.txt").toString())),
            createMenuItem("Current Calibration Log", () -> files.showFileInTextEditor(Paths.get(vars.getgCamExecutableDir(), "logs", "calibration_log.txt").toString())),
            createMenuItem("Debug File", this::showDebugFile),
            new SeparatorMenuItem(),
            createMenuItem("Font Size...", this::showFontSizeDialog),
            new SeparatorMenuItem()
        );

        // --- Resource Logs Submenu ---
        menuResourceLogs.getItems().addAll(
            createMenuItem("Current Session", () -> files.showFileInTextEditor(Paths.get(vars.getGlimpseLogDir(), "glimpse_log.txt").toString())),
            createMenuItem("Prior Session", () -> files.showFileInTextEditor(Paths.get(vars.getGlimpseLogDir(), "glimpse_log_prior.txt").toString()))
        );

        // --- Browse Folders Submenu ---
        menuBrowseFolders.getItems().addAll(
            createMenuItem("GLIMPSE Folder", () -> files.openFileExplorer(vars.getGlimpseDir())),
            createMenuItem("GLIMPSE Scenario Folder", () -> files.openFileExplorer(vars.getScenarioDir())),
            createMenuItem("GLIMPSE Scenario Component Folder", () -> files.openFileExplorer(vars.getScenarioComponentsDir())),
            createMenuItem("GLIMPSE Contrib Folder", () -> files.openFileExplorer(Paths.get(vars.getGlimpseDir(), "Contrib").toString())),
            createMenuItem("GLIMPSE Trash Folder", () -> files.openFileExplorer(vars.getTrashDir())),
            new SeparatorMenuItem(),
            createMenuItem("GCAM exe Folder", () -> files.openFileExplorer(vars.getgCamExecutableDir())),
            createMenuItem("GCAM log Folder", () -> files.openFileExplorer(Paths.get(vars.getgCamExecutableDir(), "logs").toString())),
            createMenuItem("GCAM input Folder", () -> files.openFileExplorer(new File(vars.getgCamExecutableDir()).getParentFile().toPath().resolve("input").toString())),
            createMenuItem("GCAM output Folder", () -> files.openFileExplorer(new File(vars.getgCamExecutableDir()).getParentFile().toPath().resolve("output").toString()))
        );
        
        menuView.getItems().addAll(menuResourceLogs, new SeparatorMenuItem(), menuBrowseFolders);
    }

    private void showDebugFile() {
        String debugFileName = ("1".equals(vars.getDebugRename()) || "true".equalsIgnoreCase(vars.getDebugRename()) || "yes".equalsIgnoreCase(vars.getDebugRename()))
            ? "debug" + vars.getDebugRegion().trim() + ".xml"
            : "debug.xml";
        String fullPath = Paths.get(vars.getgCamExecutableDir(), debugFileName).toString();
        files.showFileInTextEditor(fullPath);
    }

    private void showFontSizeDialog() {
        final int initialSize = getCurrentRuntimeFontSize();
        final boolean[] committed = new boolean[] { false };

        Stage dialog = new Stage();
        dialog.setTitle("Font Size");
        dialog.initModality(Modality.WINDOW_MODAL);
        try {
            javafx.stage.Window owner = glimpseUtil.UtilsDialogs.getPrimaryOwnerWindow();
            if (owner != null) {
                dialog.initOwner(owner);
            }
        } catch (Exception ignored) {
        }

        Label currentLabel = new Label("Initial size from options: " + initialSize + " | Preview size: " + initialSize);
        Label chooseLabel = new Label("Choose size:");

        ComboBox<Integer> sizeCombo = new ComboBox<>();
        sizeCombo.getItems().addAll(ALLOWED_FONT_SIZES);
        sizeCombo.setEditable(false);
        if (!ALLOWED_FONT_SIZES.contains(initialSize)) {
            sizeCombo.getItems().add(initialSize);
        }
        sizeCombo.getSelectionModel().select(Integer.valueOf(initialSize));
        sizeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentLabel.setText("Initial size from options: " + initialSize + " | Preview size: " + newVal);
                Client.applyRuntimeFontSize(newVal);
            }
        });

        Button minusButton = new Button("-");
        minusButton.setMinWidth(36);
        Button plusButton = new Button("+");
        plusButton.setMinWidth(36);

        minusButton.setOnAction(e -> stepFontSize(sizeCombo, -1));
        plusButton.setOnAction(e -> stepFontSize(sizeCombo, 1));

        HBox chooserRow = new HBox(8, chooseLabel, minusButton, sizeCombo, plusButton);
        chooserRow.setAlignment(Pos.CENTER_LEFT);

        Button okButton = new Button("Select");
        okButton.setDefaultButton(true);
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);

        okButton.setOnAction(e -> {
            Integer selectedSize = sizeCombo.getSelectionModel().getSelectedItem();
            if (selectedSize != null) {
                Client.applyRuntimeFontSize(selectedSize);
            }
            committed[0] = true;
            dialog.close();
        });
        cancelButton.setOnAction(e -> {
            Client.applyRuntimeFontSize(initialSize);
            dialog.close();
        });

        dialog.setOnCloseRequest(e -> {
            if (!committed[0]) {
                Client.applyRuntimeFontSize(initialSize);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(8, spacer, okButton, cancelButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, currentLabel, chooserRow, buttonRow);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 350, 140);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private int getCurrentRuntimeFontSize() {
        try {
            int parsed = Integer.parseInt(vars.getPreferredFontSize());
            return Math.max(Client.getMinRuntimeFontSize(), Math.min(Client.getMaxRuntimeFontSize(), parsed));
        } catch (Exception ignored) {
            return Client.getRuntimeFontSize();
        }
    }

    private void stepFontSize(ComboBox<Integer> sizeCombo, int delta) {
        Integer selected = sizeCombo.getSelectionModel().getSelectedItem();
        int current = (selected == null) ? getCurrentRuntimeFontSize() : selected;
        int idx = ALLOWED_FONT_SIZES.indexOf(current);
        if (idx < 0) {
            idx = 0;
        }
        int nextIdx = Math.max(0, Math.min(ALLOWED_FONT_SIZES.size() - 1, idx + delta));
        sizeCombo.getSelectionModel().select(nextIdx);
    }

    private MenuItem createMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}