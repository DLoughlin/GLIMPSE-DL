package gui;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class ScenarioLibraryStopHelper {

    enum StopMode {
        CONTINUE,
        STOP_CURRENT,
        STOP_ALL
    }

    private ScenarioLibraryStopHelper() {
    }

    static StopMode promptForStopMode() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Stop GCAM run");
        alert.setHeaderText("Stop GCAM?");

        Label msg = new Label(
                "Stopping GCAM may leave partial output files in the scenario folder.\n"
              + "\n"
              + "Choose:\n"
              + " - Stop: stop the currently running scenario only\n"
              + " - Stop All: stop the running scenario and cancel queued scenarios\n"
              + " - Continue: keep running\n");
        msg.setWrapText(true);

        VBox content = new VBox(10);
        content.getChildren().addAll(msg);
        alert.getDialogPane().setContent(content);

        ButtonType stopBtn = ScenarioLibraryModelInterfaceMiniHelper.createOkButton("Stop");
        ButtonType stopAllBtn = new ButtonType("Stop All", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType continueBtn = ScenarioLibraryModelInterfaceMiniHelper.createCancelCloseButton("Continue");
        alert.getButtonTypes().setAll(stopBtn, stopAllBtn, continueBtn);

        ScenarioLibraryModelInterfaceMiniHelper.setDefaultButton(alert.getDialogPane(), continueBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() == continueBtn) {
            return StopMode.CONTINUE;
        }
        if (result.get() == stopAllBtn) {
            return StopMode.STOP_ALL;
        }
        return StopMode.STOP_CURRENT;
    }
}
