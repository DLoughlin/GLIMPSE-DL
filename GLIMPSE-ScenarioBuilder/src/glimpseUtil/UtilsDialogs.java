package glimpseUtil;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import gui.Client;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Dialog helper methods extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsDialogs {

	private static final UtilsDialogs INSTANCE = new UtilsDialogs();

	private final ConcurrentLinkedQueue<String> deferredWarningMessages = new ConcurrentLinkedQueue<>();
	private volatile boolean modalDialogsReady = false;

	private GLIMPSEStyles styles;

	private UtilsDialogs() {
	}

	/**
	 * Returns the shared dialog helper instance.
	 *
	 * @return singleton dialog helper
	 */
	public static UtilsDialogs getInstance() {
		return INSTANCE;
	}

	/**
	 * Initializes the dialog helper with shared style settings.
	 *
	 * @param styles shared styles used to theme dialogs
	 */
	public void init(GLIMPSEStyles styles) {
		this.styles = styles;
	}

	private void applyModernThemeToDialog(javafx.scene.control.Dialog<?> dialog) {
		if (dialog == null)
			return;
		try {
			javafx.scene.control.DialogPane pane = dialog.getDialogPane();
			if (pane == null)
				return;
			java.net.URL cssUrl = gui.ScenarioBuilder.class.getResource(gui.ScenarioBuilder.getModernCssResource());
			if (cssUrl != null) {
				String css = cssUrl.toExternalForm();
				if (!pane.getStylesheets().contains(css)) {
					pane.getStylesheets().add(css);
				}
			}
			if (styles != null) {
				pane.setStyle(styles.getFontStyle());
			}
		} catch (Exception e) {
			// ignore theme failures; fall back to default Alert styling
		}
	}

	/**
	 * Assigns the best available owner window to a JavaFX dialog.
	 *
	 * @param dialog dialog to initialize
	 */
	public static void initDialogOwner(javafx.scene.control.Dialog<?> dialog) {
		INSTANCE.applyDialogOwner(dialog);
	}

	/**
	 * Assigns the best available owner window to a stage and centers it on show.
	 *
	 * @param stage stage to initialize
	 */
	public static void initStageOwner(Stage stage) {
		INSTANCE.applyStageOwner(stage);
	}

	/**
	 * Returns the currently visible primary owner window when one is available.
	 *
	 * @return visible primary owner window, or {@code null}
	 */
	public static Window getPrimaryOwnerWindow() {
		return INSTANCE.getVisiblePrimaryWindow();
	}

	private void applyDialogOwner(javafx.scene.control.Dialog<?> dialog) {
		if (dialog == null) {
			return;
		}
		try {
			Window owner = getVisiblePrimaryWindow();
			if (owner != null) {
				dialog.initOwner(owner);
			}
		} catch (Exception ignored) {
		}
	}

	private void applyStageOwner(Stage stage) {
		if (stage == null) {
			return;
		}
		try {
			Window owner = getVisiblePrimaryWindow();
			if (owner != null) {
				stage.initOwner(owner);
				stage.initModality(Modality.WINDOW_MODAL);
				stage.setOnShown(e -> centerStageOverOwner(stage, owner));
			}
		} catch (Exception ignored) {
		}
	}

	private void centerStageOverOwner(Stage stage, Window owner) {
		if (stage == null || owner == null || !owner.isShowing()) {
			return;
		}
		try {
			double w = stage.getWidth();
			double h = stage.getHeight();
			if (w <= 0 || h <= 0) {
				return;
			}
			stage.setX(owner.getX() + ((owner.getWidth() - w) / 2.0));
			stage.setY(owner.getY() + ((owner.getHeight() - h) / 2.0));
		} catch (Exception ignored) {}
	}

	private Window getVisiblePrimaryWindow() {
		try {
			Stage owner = Client.primaryStage;
			if (owner != null && owner.isShowing()) {
				return owner;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * Shows a warning dialog, or queues the warning until modal dialogs are ready.
	 *
	 * @param msg warning text to display
	 */
	public void warningMessage(String msg) {
		if (msg == null)
			return;
		if (!modalDialogsReady) {
			deferredWarningMessages.add(msg);
			return;
		}
		Runnable showAlert = () -> {
			Alert alert = new Alert(AlertType.WARNING);
			applyDialogOwner(alert);
			applyModernThemeToDialog(alert);
			alert.setTitle(GLIMPSEUtils.LABEL_WARNING);
			alert.setHeaderText(GLIMPSEUtils.LABEL_WARNING);
			alert.setContentText(msg);
			alert.showAndWait();
		};
		if (Platform.isFxApplicationThread()) {
			showAlert.run();
		} else {
			Platform.runLater(showAlert);
		}
	}

	/**
	 * Enables modal dialogs and flushes any warnings queued before the primary UI
	 * became ready.
	 */
	public void setModalDialogsReadyAndFlushWarnings() {
		modalDialogsReady = true;
		if (deferredWarningMessages.isEmpty()) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			flushQueuedWarnings();
		} else {
			Platform.runLater(this::flushQueuedWarnings);
		}
	}

	private void flushQueuedWarnings() {
		if (!modalDialogsReady) {
			return;
		}
		String msg;
		while ((msg = deferredWarningMessages.poll()) != null) {
			if (msg.trim().isEmpty()) {
				continue;
			}
			Alert alert = new Alert(AlertType.WARNING);
			applyDialogOwner(alert);
			applyModernThemeToDialog(alert);
			alert.setTitle(GLIMPSEUtils.LABEL_WARNING);
			alert.setHeaderText(GLIMPSEUtils.LABEL_WARNING);
			alert.setContentText(msg);
			alert.showAndWait();
		}
	}

	/**
	 * Shows a modal text-entry dialog and returns the entered text.
	 *
	 * @param descriptionType title describing the requested input
	 * @return entered text, or an empty string when none was provided
	 */
	public String getTextDialog(String descriptionType) {
		if (descriptionType == null)
			descriptionType = "";
		String title = descriptionType;
		TextArea textArea = new TextArea();
		textArea.setEditable(true);
		textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		textArea.setMinHeight(0);

		try {
			Stage stage = new Stage();
			applyStageOwner(stage);
			stage.setTitle(title);
			stage.setWidth(400);
			stage.setHeight(400);
			Scene scene = new Scene(new Group());
			gui.ScenarioBuilder.applyModernTheme(scene);
			stage.setResizable(false);
			stage.setAlwaysOnTop(true);

			Button okButton = new Button(GLIMPSEUtils.LABEL_OK);
			if (styles != null) {
				okButton.setPrefWidth(styles.getBigButtonWidth());
				okButton.setMinWidth(styles.getBigButtonWidth());
				okButton.setMaxWidth(styles.getBigButtonWidth());
			}
			okButton.setOnAction(e -> stage.close());

			VBox root = new VBox();
			if (styles != null) {
				root.setPadding(styles.getSmallPadding());
			}
			root.setSpacing(5);
			root.setAlignment(Pos.TOP_LEFT);

			textArea.setText("");

			HBox buttonBox = new HBox();
			if (styles != null) {
				buttonBox.setPadding(styles.getSmallPadding());
			}
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			buttonBox.getChildren().addAll(okButton);

			root.getChildren().addAll(textArea, buttonBox);
			scene.setRoot(root);

			stage.setScene(scene);
			stage.showAndWait();
		} catch (Exception e) {
			System.out.println("Exception on textArea dialog:" + e);
		}

		return textArea.getText();
	}

	/**
	 * Prompts the user to confirm deletion of selected items.
	 *
	 * @return {@code true} when deletion should continue
	 */
	public boolean confirmDelete() {
		boolean continueWithDelete = true;
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle(GLIMPSEUtils.LABEL_CONFIRMATION_DIALOG);
		alert.setHeaderText(GLIMPSEUtils.LABEL_DELETE_SELECTED_ITEMS);
		alert.setContentText(GLIMPSEUtils.LABEL_PLEASE_CONFIRM_DELETION);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.CANCEL) {
			continueWithDelete = false;
		}
		return continueWithDelete;
	}

	/**
	 * Prompts the user to confirm an arbitrary action.
	 *
	 * @param s action description shown in the dialog header
	 * @return {@code true} when the user confirms the action
	 */
	public boolean confirmAction(String s) {
		boolean continueAction = true;
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle(GLIMPSEUtils.LABEL_CONFIRMATION_DIALOG);
		alert.setHeaderText(s);
		alert.setContentText("Please confirm.");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.CANCEL) {
			continueAction = false;
		}
		return continueAction;
	}

	/**
	 * Shows an informational dialog using default width scaling.
	 *
	 * @param title dialog title
	 * @param header dialog header text
	 * @param content dialog body text
	 * @return {@code true} when the dialog was shown
	 */
	public boolean showInformationDialog(String title, String header, String content) {
		return showInformationDialog(title, header, content, 1.0);
	}

	/**
	 * Shows an informational dialog with optional width scaling.
	 *
	 * @param title dialog title
	 * @param header dialog header text
	 * @param content dialog body text
	 * @param widthScale width scaling factor applied to the dialog pane
	 * @return {@code true} when the dialog was shown
	 */
	public boolean showInformationDialog(String title, String header, String content, double widthScale) {
		if (title == null || header == null || content == null)
			return false;
		Alert alert = new Alert(AlertType.INFORMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		if (widthScale > 1.0) {
			applyDialogWidthScale(alert, widthScale);
		}
		alert.showAndWait();
		return true;
	}

	private void applyDialogWidthScale(Alert alert, double widthScale) {
		try {
			javafx.scene.control.DialogPane pane = alert.getDialogPane();
			if (pane == null) {
				return;
			}
			pane.applyCss();
			double baseWidth = pane.prefWidth(-1);
			if (baseWidth <= 0) {
				baseWidth = pane.getPrefWidth();
			}
			if (baseWidth <= 0) {
				baseWidth = 420.0;
			}
			pane.setPrefWidth(baseWidth * widthScale);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Prompts the user to confirm scenario archival.
	 *
	 * @return {@code true} when the user chooses to archive the scenario
	 */
	public boolean confirmArchiveScenario() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle(GLIMPSEUtils.LABEL_CONFIRMATION_DIALOG);
		alert.setHeaderText(GLIMPSEUtils.LABEL_ARCHIVE_SCENARIO);
		alert.setContentText(GLIMPSEUtils.LABEL_PLEASE_CONFIRM_ARCHIVE);

		ButtonType yes = new ButtonType("Yes");
		ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(yes, no);
		try {
			Button noBtn = (Button) alert.getDialogPane().lookupButton(no);
			if (noBtn != null) {
				noBtn.setDefaultButton(true);
				noBtn.setCancelButton(true);
			}
		} catch (Exception e) {
			// ignore; default behavior will still treat close as NO
		}

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == yes;
	}

	/**
	 * Shows a status dialog containing informational text.
	 *
	 * @param title dialog title
	 * @param header dialog header text
	 * @param content dialog body text
	 * @return {@code true} unless the dialog was explicitly canceled
	 */
	public boolean showStatusDialog(String title, String header, String content) {
		if (title == null || header == null || content == null)
			return false;
		Alert alert = new Alert(AlertType.INFORMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		Optional<ButtonType> result = alert.showAndWait();
		return !(result.isPresent() && result.get() == ButtonType.CANCEL);
	}

	/**
	 * Prompts the user with a yes/no confirmation dialog.
	 *
	 * @param s message to show
	 * @return {@code true} when the user selects Yes
	 */
	public boolean selectYesOrNoDialog(String s) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyDialogOwner(alert);
		applyModernThemeToDialog(alert);
		alert.setTitle("Confirmation required");
		alert.setHeaderText(null);

		Label msg = new Label(s == null ? "" : s);
		msg.setWrapText(true);
		if (styles != null) {
			msg.setStyle(styles.getFontStyle());
		}
		msg.setMaxWidth(420);

		VBox content = new VBox(10, msg);
		content.setFillWidth(true);
		content.setMaxWidth(440);
		if (styles != null) {
			content.setStyle(styles.getFontStyle());
		}
		alert.getDialogPane().setContent(content);
		alert.getDialogPane().setPrefWidth(480);
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

		try {
			Button noBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
			if (noBtn != null) {
				noBtn.setDefaultButton(true);
				noBtn.setCancelButton(true);
			}
		} catch (Exception ignored) {}

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.YES;
	}
}