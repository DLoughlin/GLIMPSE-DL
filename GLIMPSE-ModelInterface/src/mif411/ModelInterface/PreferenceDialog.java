package ModelInterface;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;

/**
 * Modal Preferences dialog for ModelInterface. All interaction with the host
 * application is performed through {@link PreferenceDialogCallbacks}, keeping
 * this class independent of {@link InterfaceMain}.
 */
final class PreferenceDialog {

	// ---------------------------------------------------------------------------
	// Scrollable helper panel (tracks viewport width)
	// ---------------------------------------------------------------------------
	private static final class ViewportWidthPanel extends javax.swing.JPanel
			implements javax.swing.Scrollable {

		ViewportWidthPanel(java.awt.LayoutManager layout) { super(layout); }

		@Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			if (orientation == javax.swing.SwingConstants.VERTICAL) return Math.max(visibleRect.height - 16, 16);
			return Math.max(visibleRect.width - 16, 16);
		}

		@Override public boolean getScrollableTracksViewportWidth() { return true; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	// ---------------------------------------------------------------------------
	// State
	// ---------------------------------------------------------------------------

	private final PreferenceDialogCallbacks callbacks;

	private javax.swing.JTextField xmlEditorField;
	private javax.swing.JTextField csvEditorField;
	private javax.swing.JTextField txtEditorField;
	private javax.swing.JTextField unitsFileField;
	private javax.swing.JTextField regionsFileField;
	private javax.swing.JTextField mapResourceFolderField;
	private JComboBox<String> sigDigitsCombo;
	private JComboBox<String> fontSizeCombo;
	private javax.swing.JSpinner graphicsTitleFontSizeSpinner;
	private javax.swing.JSpinner graphicsSubtitleFontSizeSpinner;
	private javax.swing.JSpinner graphicsDomainAxisLabelFontSizeSpinner;
	private javax.swing.JSpinner graphicsDomainAxisTickFontSizeSpinner;
	private javax.swing.JSpinner graphicsRangeAxisLabelFontSizeSpinner;
	private javax.swing.JSpinner graphicsRangeAxisTickFontSizeSpinner;
	private javax.swing.JSpinner graphicsLegendFontSizeSpinner;
	private javax.swing.JSpinner graphicsLineWidthScaleSpinner;
	private javax.swing.JSpinner graphicsThumbnailFontSizeSpinner;
	private javax.swing.JSpinner graphicsThumbnailLineWidthSpinner;
	private javax.swing.JCheckBox zipExportedScenariosCheckbox;
	private javax.swing.JCheckBox copyIncludeQueryNameCheckbox;
	private javax.swing.JCheckBox compressTreeCheckbox;
	private javax.swing.JCheckBox autoGenerateGraphicsCheckbox;
	private javax.swing.JCheckBox limitSigDigitsCheckbox;
	private javax.swing.JCheckBox disableUnitConversionsCheckbox;
	private javax.swing.JCheckBox nativeFileDialogCheckbox;
	private javax.swing.JComboBox<String> selectYearsCombo;

	// ---------------------------------------------------------------------------
	// Construction
	// ---------------------------------------------------------------------------

	PreferenceDialog(final PreferenceDialogCallbacks callbacks) {
		this.callbacks = callbacks;
	}

	// ---------------------------------------------------------------------------
	// Entry point
	// ---------------------------------------------------------------------------

	void showDialog() {
		final Properties props = callbacks.getProperties();
		final JDialog dlg = new JDialog(callbacks.getOwnerFrame(), "Preferences", true);
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final int initialFontSize = callbacks.getCurrentFontSize();
		final AtomicBoolean fontSizeSaved = new AtomicBoolean(false);
		final Runnable rollbackFontPreviewIfNeeded = () -> {
			if (!fontSizeSaved.get() && callbacks.getCurrentFontSize() != initialFontSize) {
				callbacks.applyFontSize(initialFontSize);
			}
		};
		dlg.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) {
				rollbackFontPreviewIfNeeded.run();
			}
		});

		javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
		tabs.addTab("General",          buildGeneralScroll(props));
		tabs.addTab("Text and Graphics",         buildGraphicsScroll(props));
		tabs.addTab("Optional Features", buildOptionalScroll(props));
		ensureOptionalTabIsLast(tabs);

		for (int i = 0; i < tabs.getTabCount(); i++) {
			javax.swing.JLabel lbl = new javax.swing.JLabel(tabs.getTitleAt(i));
			lbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));
			tabs.setTabComponentAt(i, lbl);
		}
		installPreferencesTabStyling(tabs);

		// Save / Close buttons
		javax.swing.JPanel bottom = new javax.swing.JPanel(
				new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
		javax.swing.JButton save = new javax.swing.JButton("Save");
		save.addActionListener(ev -> {
			final int selectedFontSize = InterfaceMain.parseFontSizeValue(
					fontSizeCombo == null || fontSizeCombo.getSelectedItem() == null
							? Integer.toString(callbacks.getCurrentFontSize())
							: fontSizeCombo.getSelectedItem().toString(),
					callbacks.getCurrentFontSize());
			final boolean fontSizeChanged = selectedFontSize != initialFontSize;
			callbacks.updateProperties(p -> {
				p.setProperty("xmlEditor", safeTrim(xmlEditorField.getText()));
				p.setProperty("csvEditor", safeTrim(csvEditorField.getText()));
				if (txtEditorField != null) p.setProperty("txtEditor", safeTrim(txtEditorField.getText()));
				if (sigDigitsCombo != null && sigDigitsCombo.getSelectedItem() != null)
					p.setProperty("significantDigits", sigDigitsCombo.getSelectedItem().toString());
				if (zipExportedScenariosCheckbox != null)
					p.setProperty("zipExportedScenarios", Boolean.toString(zipExportedScenariosCheckbox.isSelected()));
				if (copyIncludeQueryNameCheckbox != null)
					p.setProperty("copyIncludeQueryName", Boolean.toString(copyIncludeQueryNameCheckbox.isSelected()));
				if (compressTreeCheckbox != null)
					p.setProperty("compress_tree", Boolean.toString(compressTreeCheckbox.isSelected()));
				if (limitSigDigitsCheckbox != null)
					p.setProperty("limitSigDigits", Boolean.toString(limitSigDigitsCheckbox.isSelected()));
				if (disableUnitConversionsCheckbox != null)
					p.setProperty("disableUnitConversions", Boolean.toString(disableUnitConversionsCheckbox.isSelected()));
				if (selectYearsCombo != null && selectYearsCombo.getSelectedItem() != null)
					p.setProperty("selectYearsToShow", selectYearsCombo.getSelectedItem().toString());
				if (autoGenerateGraphicsCheckbox != null)
					p.setProperty("autoGenerateGraphics", Boolean.toString(autoGenerateGraphicsCheckbox.isSelected()));
				if (nativeFileDialogCheckbox != null) {
					String useNativeChoosers = Boolean.toString(nativeFileDialogCheckbox.isSelected());
					p.setProperty("nativeFileDialog", useNativeChoosers);
					// Keep runtime chooser behavior in sync immediately without restart.
					System.setProperty("modelinterface.nativeFileDialog", useNativeChoosers);
				}
				if (unitsFileField != null) p.setProperty("unitsFile", safeTrim(unitsFileField.getText()));
				if (regionsFileField != null) p.setProperty("presetRegionList", safeTrim(regionsFileField.getText()));
				if (mapResourceFolderField != null) p.setProperty("mapResourceFolder", safeTrim(mapResourceFolderField.getText()));
				saveGraphicsSpinners(p);
				p.setProperty(InterfaceMain.FONT_SIZE_PROPERTY, Integer.toString(selectedFontSize));
			});
			fontSizeSaved.set(true);
			callbacks.applyFontSize(selectedFontSize);
			if (fontSizeChanged) {
				callbacks.showMessageDialog("Font size updated and applied to open views.",
						"Preferences", JOptionPane.INFORMATION_MESSAGE);
			}
			dlg.dispose();
		});
		javax.swing.JButton close = new javax.swing.JButton("Close");
		close.addActionListener(ev -> { rollbackFontPreviewIfNeeded.run(); dlg.dispose(); });
		bottom.add(save);
		bottom.add(close);

		javax.swing.JPanel bottomArea = new javax.swing.JPanel(new java.awt.BorderLayout());
		bottomArea.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), java.awt.BorderLayout.NORTH);
		bottomArea.add(bottom, java.awt.BorderLayout.CENTER);

		javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout());
		content.add(tabs, java.awt.BorderLayout.CENTER);
		content.add(bottomArea, java.awt.BorderLayout.SOUTH);

		dlg.setContentPane(content);
		dlg.pack();
		int defaultWidth = (int) Math.round(dlg.getWidth() * (2.0 / 3.0));
		dlg.setSize(Math.max(560, defaultWidth), (int) Math.round(dlg.getHeight() * 1.2));
		dlg.setLocationRelativeTo(callbacks.getOwnerFrame());
		dlg.setVisible(true);
	}

	// ---------------------------------------------------------------------------
	// Tab builders
	// ---------------------------------------------------------------------------

	private javax.swing.JScrollPane buildGeneralScroll(Properties props) {
		javax.swing.JPanel panel = new ViewportWidthPanel(new java.awt.GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		java.awt.GridBagConstraints gc = defaultGbc();

		// ---- Table options section ----
		javax.swing.JLabel tableOptionsLbl = new javax.swing.JLabel("Table options:");
		tableOptionsLbl.setFont(tableOptionsLbl.getFont().deriveFont(java.awt.Font.BOLD));
		gc.gridwidth = 2; gc.weightx = 1.0;
		panel.add(tableOptionsLbl, gc);
		gc.gridwidth = 1;

		// Limit Significant Digits checkbox
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
		limitSigDigitsCheckbox = new javax.swing.JCheckBox("Limit significant digits");
		limitSigDigitsCheckbox.setSelected(parseBooleanProp(props, "limitSigDigits", false));
		panel.add(limitSigDigitsCheckbox, gc);
		gc.gridwidth = 1;

		// Significant digits combo — compact width (no fill/expand)
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.0;
		panel.add(new javax.swing.JLabel("Significant digits for results:"), gc);
		gc.gridx = 1; gc.weightx = 0.0; gc.gridwidth = 1;
		gc.fill = java.awt.GridBagConstraints.NONE;
		sigDigitsCombo = new JComboBox<>(new String[] { "2", "3", "4", "5" });
		sigDigitsCombo.setSelectedItem(props.getProperty("significantDigits", "3"));
		panel.add(sigDigitsCombo, gc);
		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		// Disable unit conversions checkbox
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
		disableUnitConversionsCheckbox = new javax.swing.JCheckBox("Disable unit conversions");
		disableUnitConversionsCheckbox.setSelected(parseBooleanProp(props, "disableUnitConversions", false));
		panel.add(disableUnitConversionsCheckbox, gc);
		gc.gridwidth = 1;

		// Select years to show combo — compact width (no fill/expand)
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.0;
		panel.add(new javax.swing.JLabel("Select years to show:"), gc);
		gc.gridx = 1; gc.weightx = 0.0; gc.gridwidth = 1;
		gc.fill = java.awt.GridBagConstraints.NONE;
		selectYearsCombo = new JComboBox<>(new String[] { "All", "Model years only", "Custom range" });
		selectYearsCombo.setSelectedItem(props.getProperty("selectYearsToShow", "All"));
		panel.add(selectYearsCombo, gc);
		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		// Separator
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1; gc.weightx = 0.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;

		// ---- File editors section ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
		javax.swing.JLabel fileEditorsLbl = new javax.swing.JLabel("File editors:");
		fileEditorsLbl.setFont(fileEditorsLbl.getFont().deriveFont(java.awt.Font.BOLD));
		panel.add(fileEditorsLbl, gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		// XML / CSV / TXT editor rows
		gc.gridy++; gc.gridx = 0;
		panel.add(new javax.swing.JLabel("XML editor command:"), gc);
		gc.gridx = 1; gc.weightx = 1.0;
		xmlEditorField = new javax.swing.JTextField(props.getProperty("xmlEditor", ""));
		panel.add(xmlEditorField, gc);
		gc.gridx = 2; gc.weightx = 0.0;
		javax.swing.JButton xmlBrowse = new javax.swing.JButton("Browse");
		xmlBrowse.addActionListener(ev -> { File f = promptForExecutable("Select XML editor executable"); if (f != null) xmlEditorField.setText(f.getAbsolutePath()); });
		panel.add(xmlBrowse, gc);

		gc.gridy++; gc.gridx = 0;
		panel.add(new javax.swing.JLabel("CSV editor command:"), gc);
		gc.gridx = 1; gc.weightx = 1.0;
		csvEditorField = new javax.swing.JTextField(props.getProperty("csvEditor", ""));
		panel.add(csvEditorField, gc);
		gc.gridx = 2; gc.weightx = 0.0;
		javax.swing.JButton csvBrowse = new javax.swing.JButton("Browse");
		csvBrowse.addActionListener(ev -> { File f = promptForExecutable("Select CSV editor executable"); if (f != null) csvEditorField.setText(f.getAbsolutePath()); });
		panel.add(csvBrowse, gc);

		gc.gridy++; gc.gridx = 0;
		panel.add(new javax.swing.JLabel("TXT editor command:"), gc);
		gc.gridx = 1; gc.weightx = 1.0;
		txtEditorField = new javax.swing.JTextField(props.getProperty("txtEditor", ""));
		panel.add(txtEditorField, gc);
		gc.gridx = 2; gc.weightx = 0.0;
		javax.swing.JButton txtBrowse = new javax.swing.JButton("Browse");
		txtBrowse.addActionListener(ev -> { File f = promptForExecutable("Select TXT editor executable"); if (f != null) txtEditorField.setText(f.getAbsolutePath()); });
		panel.add(txtBrowse, gc);

		// Hint + separator
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 3; gc.weightx = 1.0;
		panel.add(new javax.swing.JLabel(
				"<html>Tip: leave blank to use the default system editor, or enter a command such as<br>"
				+ "<i>notepad.exe</i>, <i>code</i>, or <i>\"C:\\Program Files\\editor.exe\" --arg</i>.<br>"
				+ "Arguments are supported; quote paths that contain spaces.</html>"), gc);
		gc.gridy++;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1;

		// ---- Other options section ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
		javax.swing.JLabel otherOptionsLbl = new javax.swing.JLabel("Other options:");
		otherOptionsLbl.setFont(otherOptionsLbl.getFont().deriveFont(java.awt.Font.BOLD));
		panel.add(otherOptionsLbl, gc);
		gc.gridwidth = 1;

		// Checkboxes
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0;
		zipExportedScenariosCheckbox = new javax.swing.JCheckBox("Automatically zip scenarios when exported");
		zipExportedScenariosCheckbox.setSelected(parseBooleanProp(props, "zipExportedScenarios", false));
		panel.add(zipExportedScenariosCheckbox, gc);

		gc.gridy++; gc.gridx = 0;
		copyIncludeQueryNameCheckbox = new javax.swing.JCheckBox("Include query name when copying tab data");
		copyIncludeQueryNameCheckbox.setSelected(parseBooleanProp(props, "copyIncludeQueryName", false));
		panel.add(copyIncludeQueryNameCheckbox, gc);

		gc.gridy++; gc.gridx = 0;
		compressTreeCheckbox = new javax.swing.JCheckBox("Compress Query Tree at startup");
		compressTreeCheckbox.setSelected(parseBooleanProp(props, "compress_tree", true));
		panel.add(compressTreeCheckbox, gc);

		gc.gridy++; gc.gridx = 0;
		nativeFileDialogCheckbox = new javax.swing.JCheckBox("Use native file and folder choosers");
		nativeFileDialogCheckbox.setSelected(parseBooleanPropWithLegacy(props,
				"nativeFileDialog", "modelinterface.nativeFileDialog", true));
		nativeFileDialogCheckbox.setToolTipText("Uncheck to force Java chooser dialogs.");
		panel.add(nativeFileDialogCheckbox, gc);

		// Equalize combo widths (for sigDigitsCombo)
		{
			sigDigitsCombo.setPreferredSize(
					new java.awt.Dimension(sigDigitsCombo.getPreferredSize().width, sigDigitsCombo.getPreferredSize().height));
		}

		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		return scrollOf(panel);
	}

	private javax.swing.JScrollPane buildGraphicsScroll(Properties props) {
		javax.swing.JPanel panel = new ViewportWidthPanel(new java.awt.GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		java.awt.GridBagConstraints gc = defaultGbc();

		// ---- Font size combo (at the top) ----
		gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.0;
		gc.fill = java.awt.GridBagConstraints.NONE;
		panel.add(new javax.swing.JLabel("Font size:"), gc);
		gc.gridx = 1; gc.weightx = 0.0; gc.gridwidth = 1;
		fontSizeCombo = new JComboBox<>(InterfaceMain.getGeneralFontSizeOptions());
		fontSizeCombo.setEditable(true);
		fontSizeCombo.setPrototypeDisplayValue("24"); // prevents the editable text field from inflating the width
		fontSizeCombo.setSelectedItem(Integer.toString(InterfaceMain.resolveConfiguredFontSize(props)));
		fontSizeCombo.addActionListener(ev -> {
			Object sel = fontSizeCombo.getSelectedItem();
			int previewSize = InterfaceMain.parseFontSizeValue(sel == null ? null : sel.toString(), callbacks.getCurrentFontSize());
			if (previewSize != callbacks.getCurrentFontSize()) callbacks.applyFontSize(previewSize);
		});
		panel.add(fontSizeCombo, gc);
		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		// ---- Separator ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		// ---- Auto Graphics checkbox (at the top) ----
		gc.gridy++; gc.gridwidth = 2; gc.weightx = 1.0;
		autoGenerateGraphicsCheckbox = new javax.swing.JCheckBox("Enable auto graphics");
		autoGenerateGraphicsCheckbox.setSelected(parseBooleanProp(props, "autoGenerateGraphics", false));
		panel.add(autoGenerateGraphicsCheckbox, gc);

		// ---- Separator ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		// ---- Thumbnail settings (first) ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		javax.swing.JLabel thumbnailHdr = new javax.swing.JLabel("Thumbnail settings");
		thumbnailHdr.setFont(thumbnailHdr.getFont().deriveFont(java.awt.Font.BOLD));
		panel.add(thumbnailHdr, gc);
		gc.gridwidth = 1; gc.weightx = 0.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Thumbnail font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsThumbnailFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE);
		panel.add(graphicsThumbnailFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Thumbnail line width:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsThumbnailLineWidthSpinner = graphicsDoubleSpinner(props, InterfaceMain.GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH, InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE, InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE, 0.1d);
		panel.add(graphicsThumbnailLineWidthSpinner, gc);

		// ---- Separator ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		// ---- Full-size chart settings ----
		gc.gridy++;
		javax.swing.JLabel fullSizeHdr = new javax.swing.JLabel("Full-size chart settings");
		fullSizeHdr.setFont(fullSizeHdr.getFont().deriveFont(java.awt.Font.BOLD));
		gc.gridwidth = 2; gc.weightx = 1.0;
		panel.add(fullSizeHdr, gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Default title font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsTitleFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_TITLE_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_TITLE_FONT_SIZE);
		panel.add(graphicsTitleFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Default subtitle font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsSubtitleFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE);
		panel.add(graphicsSubtitleFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("X-axis label font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsDomainAxisLabelFontSizeSpinner = graphicsFontSpinner(props,
				InterfaceMain.GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY,
				InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
		panel.add(graphicsDomainAxisLabelFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("X-axis tick/value font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsDomainAxisTickFontSizeSpinner = graphicsFontSpinner(props,
				InterfaceMain.GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY,
				InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
		panel.add(graphicsDomainAxisTickFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Y-axis label font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsRangeAxisLabelFontSizeSpinner = graphicsFontSpinner(props,
				InterfaceMain.GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY,
				InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
		panel.add(graphicsRangeAxisLabelFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Y-axis tick/value font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsRangeAxisTickFontSizeSpinner = graphicsFontSpinner(props,
				InterfaceMain.GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY,
				InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
		panel.add(graphicsRangeAxisTickFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Legend label font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsLegendFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_LEGEND_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_LEGEND_FONT_SIZE);
		panel.add(graphicsLegendFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Line width:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsLineWidthScaleSpinner = graphicsDoubleSpinner(props, InterfaceMain.GRAPHICS_LINE_WIDTH_SCALE_PROPERTY,
				InterfaceMain.DEFAULT_GRAPHICS_LINE_WIDTH_SCALE, InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE, InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE, 0.1d);
		panel.add(graphicsLineWidthScaleSpinner, gc);

		// Equalize all spinner widths to the widest natural preferred size.
		{
			javax.swing.JSpinner[] spinners = {
				graphicsThumbnailFontSizeSpinner, graphicsThumbnailLineWidthSpinner,
				graphicsTitleFontSizeSpinner, graphicsSubtitleFontSizeSpinner,
				graphicsDomainAxisLabelFontSizeSpinner, graphicsDomainAxisTickFontSizeSpinner,
				graphicsRangeAxisLabelFontSizeSpinner, graphicsRangeAxisTickFontSizeSpinner,
				graphicsLegendFontSizeSpinner, graphicsLineWidthScaleSpinner
			};
			int maxW = 0;
			for (javax.swing.JSpinner s : spinners) maxW = Math.max(maxW, s.getPreferredSize().width);
			for (javax.swing.JSpinner s : spinners)
				s.setPreferredSize(new java.awt.Dimension(maxW, s.getPreferredSize().height));
		}

		// ---- Separator ----
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 2; gc.weightx = 1.0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL), gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

		// ---- Reset Defaults button (left-justified) ----
		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.NONE;
		gc.anchor = java.awt.GridBagConstraints.WEST;
		javax.swing.JButton resetBtn = new javax.swing.JButton("Reset Defaults");
		resetBtn.addActionListener(ev -> {
			graphicsThumbnailFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE);
			graphicsThumbnailLineWidthSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH);
			graphicsTitleFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_TITLE_FONT_SIZE);
			graphicsSubtitleFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE);
			graphicsDomainAxisLabelFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
			graphicsDomainAxisTickFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
			graphicsRangeAxisLabelFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
			graphicsRangeAxisTickFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
			graphicsLegendFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_LEGEND_FONT_SIZE);
			graphicsLineWidthScaleSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_LINE_WIDTH_SCALE);
		});
		panel.add(resetBtn, gc);

		return scrollOf(panel);
	}

	private javax.swing.JScrollPane buildOptionalScroll(Properties props) {
		javax.swing.JPanel panel = new ViewportWidthPanel(new java.awt.GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		java.awt.GridBagConstraints oc = defaultGbc();

		// Units file row
		panel.add(new javax.swing.JLabel("Convert Units (CSV):"), oc);
		oc.gridx = 1; oc.weightx = 1.0;
		unitsFileField = new javax.swing.JTextField(props.getProperty("unitsFile", ""));
		panel.add(unitsFileField, oc);
		oc.gridx = 2; oc.weightx = 0.0;
		javax.swing.JButton unitsBrowse = new javax.swing.JButton("Browse");
		unitsBrowse.addActionListener(ev -> {
			callbacks.dispatchMenuAction(new ActionEvent(unitsBrowse, ActionEvent.ACTION_PERFORMED, "Select Units File"));
			unitsFileField.setText(callbacks.getProperties().getProperty("unitsFile", ""));
		});
		panel.add(unitsBrowse, oc);
		oc.gridx = 3;
		javax.swing.JButton unitsEdit = new javax.swing.JButton("Edit");
		unitsEdit.addActionListener(ev -> callbacks.openEditorForFile(new File(unitsFileField.getText()), "csv"));
		panel.add(unitsEdit, oc);

		// Regions file row
		oc.gridy++; oc.gridx = 0;
		panel.add(new javax.swing.JLabel("Preset Regions:"), oc);
		oc.gridx = 1; oc.weightx = 1.0;
		regionsFileField = new javax.swing.JTextField(props.getProperty("presetRegionList", ""));
		panel.add(regionsFileField, oc);
		oc.gridx = 2; oc.weightx = 0.0;
		javax.swing.JButton regionsBrowse = new javax.swing.JButton("Browse");
		regionsBrowse.addActionListener(ev -> {
			callbacks.dispatchMenuAction(new ActionEvent(regionsBrowse, ActionEvent.ACTION_PERFORMED, "Select Regions File"));
			regionsFileField.setText(callbacks.getProperties().getProperty("presetRegionList", ""));
		});
		panel.add(regionsBrowse, oc);
		oc.gridx = 3;
		javax.swing.JButton regionsEdit = new javax.swing.JButton("Edit");
		regionsEdit.addActionListener(ev -> callbacks.openEditorForFile(new File(regionsFileField.getText()), "txt"));
		panel.add(regionsEdit, oc);

		// Map resource folder row
		oc.gridy++; oc.gridx = 0;
		panel.add(new javax.swing.JLabel("Mapping Resources:"), oc);
		oc.gridx = 1; oc.weightx = 1.0;
		mapResourceFolderField = new javax.swing.JTextField(props.getProperty("mapResourceFolder", ""));
		panel.add(mapResourceFolderField, oc);
		oc.gridx = 2; oc.weightx = 0.0;
		javax.swing.JButton mapBrowse = new javax.swing.JButton("Browse");
		mapBrowse.addActionListener(ev -> {
			callbacks.dispatchMenuAction(new ActionEvent(mapBrowse, ActionEvent.ACTION_PERFORMED, "Select Map Resource Folder"));
			mapResourceFolderField.setText(callbacks.getProperties().getProperty("mapResourceFolder", ""));
		});
		panel.add(mapBrowse, oc);
		oc.gridx = 3;
		panel.add(new javax.swing.JLabel(""), oc);

		// Spacer
		oc.gridy++; oc.gridx = 0; oc.gridwidth = 4; oc.weightx = 1.0;
		panel.add(new javax.swing.JLabel(""), oc);

		return scrollOf(panel);
	}

	// ---------------------------------------------------------------------------
	// Save helpers
	// ---------------------------------------------------------------------------

	private void saveGraphicsSpinners(Properties p) {
		if (graphicsTitleFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_TITLE_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsTitleFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_TITLE_FONT_SIZE));
		if (graphicsSubtitleFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_SUBTITLE_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsSubtitleFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_SUBTITLE_FONT_SIZE));
		String domainAxisLabelFontSize = graphicsDomainAxisLabelFontSizeSpinner == null
				? null
				: boundedIntStr(graphicsDomainAxisLabelFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
		String rangeAxisLabelFontSize = graphicsRangeAxisLabelFontSizeSpinner == null
				? null
				: boundedIntStr(graphicsRangeAxisLabelFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
		String domainAxisTickFontSize = graphicsDomainAxisTickFontSizeSpinner == null
				? null
				: boundedIntStr(graphicsDomainAxisTickFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
		String rangeAxisTickFontSize = graphicsRangeAxisTickFontSizeSpinner == null
				? null
				: boundedIntStr(graphicsRangeAxisTickFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
		if (domainAxisLabelFontSize != null)
			p.setProperty(InterfaceMain.GRAPHICS_DOMAIN_AXIS_LABEL_FONT_SIZE_PROPERTY, domainAxisLabelFontSize);
		if (rangeAxisLabelFontSize != null)
			p.setProperty(InterfaceMain.GRAPHICS_RANGE_AXIS_LABEL_FONT_SIZE_PROPERTY, rangeAxisLabelFontSize);
		if (domainAxisTickFontSize != null)
			p.setProperty(InterfaceMain.GRAPHICS_DOMAIN_AXIS_TICK_FONT_SIZE_PROPERTY, domainAxisTickFontSize);
		if (rangeAxisTickFontSize != null)
			p.setProperty(InterfaceMain.GRAPHICS_RANGE_AXIS_TICK_FONT_SIZE_PROPERTY, rangeAxisTickFontSize);
		updateLegacyAxisFontProperties(p, domainAxisLabelFontSize, rangeAxisLabelFontSize,
				domainAxisTickFontSize, rangeAxisTickFontSize);
		if (graphicsLegendFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_LEGEND_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsLegendFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_LEGEND_FONT_SIZE));
		if (graphicsLineWidthScaleSpinner != null) {
			double v = ((Number) graphicsLineWidthScaleSpinner.getValue()).doubleValue();
			p.setProperty(InterfaceMain.GRAPHICS_LINE_WIDTH_SCALE_PROPERTY,
					Double.toString(InterfaceMain.parseBoundedDoubleValue(Double.toString(v),
							InterfaceMain.DEFAULT_GRAPHICS_LINE_WIDTH_SCALE,
							InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE, InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE)));
		}
		if (graphicsThumbnailFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_THUMBNAIL_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsThumbnailFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_FONT_SIZE));
		if (graphicsThumbnailLineWidthSpinner != null) {
			double v = ((Number) graphicsThumbnailLineWidthSpinner.getValue()).doubleValue();
			p.setProperty(InterfaceMain.GRAPHICS_THUMBNAIL_LINE_WIDTH_PROPERTY,
					Double.toString(InterfaceMain.parseBoundedDoubleValue(Double.toString(v),
							InterfaceMain.DEFAULT_GRAPHICS_THUMBNAIL_LINE_WIDTH,
							InterfaceMain.MIN_GRAPHICS_LINE_WIDTH_SCALE, InterfaceMain.MAX_GRAPHICS_LINE_WIDTH_SCALE)));
		}
	}

	private String boundedIntStr(javax.swing.JSpinner spinner, int defaultValue) {
		int v = ((Number) spinner.getValue()).intValue();
		return Integer.toString(InterfaceMain.parseBoundedIntValue(Integer.toString(v),
				defaultValue, InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE));
	}

	private void updateLegacyAxisFontProperties(Properties p, String domainAxisLabelFontSize,
			String rangeAxisLabelFontSize, String domainAxisTickFontSize, String rangeAxisTickFontSize) {
		if (domainAxisLabelFontSize != null && domainAxisLabelFontSize.equals(rangeAxisLabelFontSize)) {
			p.setProperty(InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY, domainAxisLabelFontSize);
		}
		if (domainAxisTickFontSize != null && domainAxisTickFontSize.equals(rangeAxisTickFontSize)) {
			p.setProperty(InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY, domainAxisTickFontSize);
		}
	}

	// ---------------------------------------------------------------------------
	// Widget factories
	// ---------------------------------------------------------------------------

	private javax.swing.JSpinner graphicsFontSpinner(Properties props, String key, int defaultValue) {
		int value = InterfaceMain.parseBoundedIntValue(props.getProperty(key), defaultValue,
				InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE);
		return new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(
				value, InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE, 1));
	}

	private javax.swing.JSpinner graphicsFontSpinner(Properties props, String key, String legacyKey, int defaultValue) {
		int value = InterfaceMain.resolveGraphicsFontSize(props, key, legacyKey, defaultValue);
		return new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(
				value, InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE, 1));
	}

	private javax.swing.JSpinner graphicsDoubleSpinner(Properties props, String key,
			double defaultValue, double min, double max, double step) {
		double value = InterfaceMain.parseBoundedDoubleValue(props.getProperty(key), defaultValue, min, max);
		return new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(value, min, max, step));
	}

	// ---------------------------------------------------------------------------
	// Static helpers
	// ---------------------------------------------------------------------------

	private static java.awt.GridBagConstraints defaultGbc() {
		java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0;
		gc.anchor = java.awt.GridBagConstraints.WEST;
		gc.fill   = java.awt.GridBagConstraints.HORIZONTAL;
		gc.weightx = 0.0;
		gc.insets = new java.awt.Insets(6, 6, 6, 6);
		return gc;
	}

	private static javax.swing.JScrollPane scrollOf(javax.swing.JPanel panel) {
		javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(panel,
				javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		return scroll;
	}

	private static void ensureOptionalTabIsLast(javax.swing.JTabbedPane tabs) {
		int idx = tabs.indexOfTab("Optional Features");
		if (idx > -1 && idx != tabs.getTabCount() - 1) {
			java.awt.Component comp = tabs.getComponentAt(idx);
			javax.swing.Icon icon   = tabs.getIconAt(idx);
			String tip              = tabs.getToolTipTextAt(idx);
			tabs.removeTabAt(idx);
			tabs.addTab("Optional Features", icon, comp, tip);
		}
	}

	private static boolean parseBooleanProp(Properties props, String key, boolean fallback) {
		String v = props.getProperty(key);
		if ("true".equalsIgnoreCase(v))  return true;
		if ("false".equalsIgnoreCase(v)) return false;
		return fallback;
	}

	private static boolean parseBooleanPropWithLegacy(Properties props, String key, String legacyKey,
			boolean fallback) {
		String v = props.getProperty(key);
		if ((v == null || v.trim().isEmpty()) && legacyKey != null) {
			v = props.getProperty(legacyKey);
		}
		if ("true".equalsIgnoreCase(v))  return true;
		if ("false".equalsIgnoreCase(v)) return false;
		return fallback;
	}

	private File promptForExecutable(String title) {
		FileChooser chooser = FileChooserFactory.getFileChooser();
		String lastDir = callbacks.getProperties().getProperty("lastDirectory", ".");
		File[] files = chooser.doFilePrompt(callbacks.getOwnerFrame(), title,
				FileChooser.LOAD_DIALOG, new File(lastDir), null);
		if (files != null && files.length > 0 && files[0] != null) {
			File selected = files[0];
			if (selected.getParent() != null) {
				callbacks.updateProperties(p -> p.setProperty("lastDirectory", selected.getParent()));
			}
			return selected;
		}
		return null;
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static int clampColor(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static java.awt.Color shiftColor(java.awt.Color source, int delta) {
		if (source == null) {
			return null;
		}
		return new java.awt.Color(
				clampColor(source.getRed() + delta),
				clampColor(source.getGreen() + delta),
				clampColor(source.getBlue() + delta));
	}

	private static java.awt.Color resolvePreferencesTabBaseColor(javax.swing.JTabbedPane tabs) {
		java.awt.Color base = javax.swing.UIManager.getColor("TabbedPane.unselectedBackground");
		if (base == null) {
			base = javax.swing.UIManager.getColor("TabbedPane.background");
		}
		if (base == null && tabs != null) {
			base = tabs.getBackground();
		}
		if (base == null) {
			base = new java.awt.Color(220, 220, 220);
		}
		return base;
	}

	private static java.awt.Color resolvePreferencesTabSelectedColor() {
		java.awt.Color selected = javax.swing.UIManager.getColor("List.selectionBackground");
		if (selected == null) {
			selected = javax.swing.UIManager.getColor("Tree.selectionBackground");
		}
		if (selected == null) {
			selected = javax.swing.UIManager.getColor("Table.selectionBackground");
		}
		if (selected == null) {
			selected = new java.awt.Color(57, 105, 138);
		}
		return selected;
	}

	private static java.awt.Color resolvePreferencesTabSelectedForeground() {
		java.awt.Color fg = javax.swing.UIManager.getColor("List.selectionForeground");
		if (fg == null) {
			fg = javax.swing.UIManager.getColor("Tree.selectionForeground");
		}
		if (fg == null) {
			fg = javax.swing.UIManager.getColor("Table.selectionForeground");
		}
		if (fg == null) {
			fg = java.awt.Color.WHITE;
		}
		return fg;
	}

	private static final class PreferencesTabsBackgroundUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
		@Override
		protected void paintTabBackground(java.awt.Graphics g, int tabPlacement, int tabIndex,
				int x, int y, int w, int h, boolean isSelected) {
			java.awt.Color tabColor = tabPane.getBackgroundAt(tabIndex);
			if (tabColor == null) {
				tabColor = tabPane.getBackground();
			}
			if (tabColor == null) {
				tabColor = java.awt.Color.LIGHT_GRAY;
			}
			g.setColor(tabColor);
			g.fillRect(x, y, w, h);
		}
	}

	private static void refreshPreferencesTabSelectionStyling(javax.swing.JTabbedPane tabs) {
		if (tabs == null) {
			return;
		}
		int tabCount = tabs.getTabCount();
		if (tabCount <= 0) {
			return;
		}
		int selectedIndex = tabs.getSelectedIndex();
		if (selectedIndex < 0 || selectedIndex >= tabCount) {
			return;
		}
		java.awt.Color baseColor = resolvePreferencesTabBaseColor(tabs);
		java.awt.Color selectedColor = resolvePreferencesTabSelectedColor();
		java.awt.Color unselectedColor = shiftColor(baseColor, 16);
		java.awt.Color selectedForeground = resolvePreferencesTabSelectedForeground();
		java.awt.Color defaultForeground = javax.swing.UIManager.getColor("TabbedPane.foreground");
		if (defaultForeground == null) {
			defaultForeground = tabs.getForeground();
		}
		if (defaultForeground == null) {
			defaultForeground = java.awt.Color.BLACK;
		}
		for (int i = 0; i < tabCount; ++i) {
			boolean isSelected = i == selectedIndex;
			tabs.setBackgroundAt(i, isSelected ? selectedColor : unselectedColor);
			tabs.setForegroundAt(i, isSelected ? selectedForeground : defaultForeground);
		}
		tabs.repaint();
	}

	private static void installPreferencesTabStyling(javax.swing.JTabbedPane tabs) {
		if (tabs == null) {
			return;
		}
		tabs.setUI(new PreferencesTabsBackgroundUI());
		tabs.setOpaque(true);
		tabs.addChangeListener(e -> refreshPreferencesTabSelectionStyling(tabs));
		refreshPreferencesTabSelectionStyling(tabs);
	}
}
