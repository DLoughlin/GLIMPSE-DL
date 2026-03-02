package ModelInterface.common;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Applies a simple "material-like" theme using UIManager defaults.
 * <p>
 * This avoids introducing new third-party look-and-feel dependencies while still
 * modernizing the UI: cleaner colors, consistent button padding, and readable
 * fonts.
 */
public final class ThemeManager {
	private ThemeManager() {
	}

	/**
	 * Apply a lightweight material-ish theme on top of the current Look & Feel.
	 * <p>
	 * Call this after {@link UIManager#setLookAndFeel(String)} and before creating
	 * Swing components.
	 */
	public static void applyMaterialLikeTheme() {
		final UIDefaults d = UIManager.getLookAndFeelDefaults();

		// Palette (light).
		final Color bg = new Color(0xFAFAFA);
		final Color panelBg = Color.WHITE;
		final Color text = new Color(0x212121);
		final Color disabledText = new Color(0x9E9E9E);
		final Color accent = new Color(0x1976D2); // blue 700
		final Color selectionBg = new Color(0xBBDEFB); // blue 100
		final Color border = new Color(0xE0E0E0);

		// Prefer L&F fonts but bump slightly for readability.
		Font base = d.getFont("defaultFont");
		if (base == null) {
			base = UIManager.getFont("Label.font");
		}
		if (base != null) {
			final Font uiFont = base.deriveFont(Math.max(13f, base.getSize2D()));
			d.put("defaultFont", uiFont);
			UIManager.put("Label.font", uiFont);
			UIManager.put("Button.font", uiFont);
			UIManager.put("ToggleButton.font", uiFont);
			UIManager.put("CheckBox.font", uiFont);
			UIManager.put("RadioButton.font", uiFont);
			UIManager.put("ComboBox.font", uiFont);
			UIManager.put("Menu.font", uiFont);
			UIManager.put("MenuItem.font", uiFont);
			UIManager.put("Table.font", uiFont);
			UIManager.put("Tree.font", uiFont);
			UIManager.put("TextField.font", uiFont);
			UIManager.put("TextArea.font", uiFont);
			UIManager.put("TextPane.font", uiFont);
			UIManager.put("EditorPane.font", uiFont);
		}

		// Backgrounds / foregrounds.
		UIManager.put("control", bg);
		UIManager.put("Panel.background", panelBg);
		UIManager.put("Viewport.background", panelBg);
		UIManager.put("ScrollPane.background", panelBg);
		UIManager.put("List.background", panelBg);
		UIManager.put("Table.background", panelBg);
		UIManager.put("Tree.background", panelBg);

		UIManager.put("Label.foreground", text);
		UIManager.put("Button.foreground", text);
		UIManager.put("Menu.foreground", text);
		UIManager.put("MenuItem.foreground", text);
		UIManager.put("CheckBox.foreground", text);
		UIManager.put("RadioButton.foreground", text);
		UIManager.put("TextField.foreground", text);
		UIManager.put("TextArea.foreground", text);

		UIManager.put("Label.disabledForeground", disabledText);
		UIManager.put("Button.disabledText", disabledText);

		// Focus/selection accents.
		UIManager.put("nimbusFocus", accent);
		UIManager.put("nimbusSelectionBackground", selectionBg);
		UIManager.put("TextField.selectionBackground", selectionBg);
		UIManager.put("TextArea.selectionBackground", selectionBg);
		UIManager.put("Table.selectionBackground", selectionBg);
		UIManager.put("List.selectionBackground", selectionBg);
		UIManager.put("Tree.selectionBackground", selectionBg);

		// Subtle borders (many L&Fs respect these keys).
		UIManager.put("Separator.foreground", border);
		UIManager.put("Separator.background", border);

		// Make buttons feel less cramped.
		SwingButtonSizer.installGlobalListener();
	}
}
