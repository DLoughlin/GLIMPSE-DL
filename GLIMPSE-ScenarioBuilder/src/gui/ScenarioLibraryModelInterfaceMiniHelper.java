package gui;

import java.io.File;
import java.util.List;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

final class ScenarioLibraryModelInterfaceMiniHelper {

    private ScenarioLibraryModelInterfaceMiniHelper() {
    }

    static void validateOptionalFile(List<String> problems, String label, String filename) {
        if (problems == null || label == null || filename == null || filename.trim().isEmpty()) {
            return;
        }
        File f = new File(filename);
        if (!f.exists()) {
            problems.add(label + " not found: " + f.getAbsolutePath());
        }
    }

    static void appendArgIfPresent(List<String> args, String flag, String value) {
        if (args == null || flag == null || value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        args.add(flag);
        args.add(trimmed);
    }

    static void setDefaultButton(DialogPane dialogPane, ButtonType buttonType) {
        if (dialogPane == null || buttonType == null) {
            return;
        }
        try {
            Object button = dialogPane.lookupButton(buttonType);
            if (button instanceof javafx.scene.control.Button) {
                ((javafx.scene.control.Button) button).setDefaultButton(true);
            }
        } catch (Exception ignored) {
        }
    }

    static ButtonType createOkButton(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
    }

    static ButtonType createCancelCloseButton(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.CANCEL_CLOSE);
    }
}
