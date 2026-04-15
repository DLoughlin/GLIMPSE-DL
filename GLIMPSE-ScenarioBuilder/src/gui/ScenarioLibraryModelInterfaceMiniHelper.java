package gui;

import glimpseUtil.ModelInterfaceFontSizeOptions;
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
            problems.add("The configured " + label.toLowerCase() + " was not found: " + f.getAbsolutePath());
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

    // TODO: Re-enable once JavaFX (ScenarioBuilder) and Swing (ModelInterface) font-size scaling
    //       can be reconciled (Windows HiDPI / toolkit mismatch). Call site is in
    //       PaneScenarioLibrary.runModelInterfaceWhich(); the -s option is implemented in
    //       InterfaceMain.main() and validated against ModelInterfaceFontSizeOptions.
    //
    // static void appendModelInterfaceFontSizeArgIfValid(List<String> args, String preferredFontSize) {
    //     if (args == null || preferredFontSize == null) { return; }
    //     String trimmed = preferredFontSize.trim();
    //     if (trimmed.isEmpty()) { return; }
    //     try {
    //         int parsedFontSize = Integer.parseInt(trimmed);
    //         if (ModelInterfaceFontSizeOptions.isAllowedFontSize(parsedFontSize)) {
    //             args.add("-s");
    //             args.add(Integer.toString(parsedFontSize));
    //             return;
    //         }
    //     } catch (NumberFormatException ignored) { }
    //     System.out.println("Skipping ModelInterface -s launch argument due to invalid preferredFontSize: '"
    //             + preferredFontSize + "'.");
    // }

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
