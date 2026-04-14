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
	private javax.swing.JSpinner graphicsAxisLabelFontSizeSpinner;
	private javax.swing.JSpinner graphicsAxisTickFontSizeSpinner;
	private javax.swing.JSpinner graphicsLegendFontSizeSpinner;
	private javax.swing.JSpinner graphicsLineWidthScaleSpinner;
	private javax.swing.JSpinner graphicsThumbnailFontSizeSpinner;
	private javax.swing.JSpinner graphicsThumbnailLineWidthSpinner;
	private javax.swing.JCheckBox zipExportedScenariosCheckbox;
	private javax.swing.JCheckBox copyIncludeQueryNameCheckbox;
	private javax.swing.JCheckBox compressTreeCheckbox;

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
		tabs.addTab("Graphics",         buildGraphicsScroll(props));
		tabs.addTab("Optional Features", buildOptionalScroll(props));
		ensureOptionalTabIsLast(tabs);

		for (int i = 0; i < tabs.getTabCount(); i++) {
			javax.swing.JLabel lbl = new javax.swing.JLabel(tabs.getTitleAt(i));
			lbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));
			tabs.setTabComponentAt(i, lbl);
		}

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
		dlg.setSize(Math.max(560, defaultWidth), dlg.getHeight() + 20);
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

		// Section header
		javax.swing.JLabel fileEditorsLbl = new javax.swing.JLabel("File editors:");
		fileEditorsLbl.setFont(fileEditorsLbl.getFont().deriveFont(java.awt.Font.BOLD));
		gc.gridwidth = 3; gc.weightx = 1.0;
		panel.add(fileEditorsLbl, gc);
		gc.gridwidth = 1;

		// XML / CSV / TXT editor rows
		gc.gridy++; gc.weightx = 0.0;
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

		// Significant digits combo — compact width (no fill/expand)
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.0;
		panel.add(new javax.swing.JLabel("Significant digits for results:"), gc);
		gc.gridx = 1; gc.weightx = 0.0; gc.gridwidth = 1;
		gc.fill = java.awt.GridBagConstraints.NONE;
		sigDigitsCombo = new JComboBox<>(new String[] { "2", "3", "4", "5" });
		sigDigitsCombo.setSelectedItem(props.getProperty("significantDigits", "3"));
		panel.add(sigDigitsCombo, gc);
		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		// Font size combo — compact width (no fill/expand)
		gc.gridy++; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.0;
		panel.add(new javax.swing.JLabel("Font size:"), gc);
		gc.gridx = 1; gc.weightx = 0.0; gc.gridwidth = 1;
		gc.fill = java.awt.GridBagConstraints.NONE;
		fontSizeCombo = new JComboBox<>(new String[] { "8","9","10","11","12","13","14","15","16","18","20","22","24" });
		fontSizeCombo.setEditable(true);
		fontSizeCombo.setPrototypeDisplayValue("24"); // prevents the editable text field from inflating the width
		fontSizeCombo.setSelectedItem(Integer.toString(InterfaceMain.resolveConfiguredFontSize(props)));
		fontSizeCombo.addActionListener(ev -> {
			Object sel = fontSizeCombo.getSelectedItem();
			int previewSize = InterfaceMain.parseFontSizeValue(sel == null ? null : sel.toString(), callbacks.getCurrentFontSize());
			if (previewSize != callbacks.getCurrentFontSize()) callbacks.applyFontSize(previewSize);
		});
		panel.add(fontSizeCombo, gc);

		// Both combos get the same preferred width — the larger of their natural sizes.
		{
			int w = Math.max(sigDigitsCombo.getPreferredSize().width,
			                 fontSizeCombo.getPreferredSize().width);
			sigDigitsCombo.setPreferredSize(
					new java.awt.Dimension(w, sigDigitsCombo.getPreferredSize().height));
			fontSizeCombo.setPreferredSize(
					new java.awt.Dimension(w, fontSizeCombo.getPreferredSize().height));
		}

		gc.fill = java.awt.GridBagConstraints.HORIZONTAL; // restore for subsequent rows

		return scrollOf(panel);
	}

	private javax.swing.JScrollPane buildGraphicsScroll(Properties props) {
		javax.swing.JPanel panel = new ViewportWidthPanel(new java.awt.GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
		java.awt.GridBagConstraints gc = defaultGbc();

		// ---- Thumbnail settings (first) ----
		javax.swing.JLabel thumbnailHdr = new javax.swing.JLabel("Thumbnail settings");
		thumbnailHdr.setFont(thumbnailHdr.getFont().deriveFont(java.awt.Font.BOLD));
		gc.gridwidth = 2; gc.weightx = 1.0;
		panel.add(thumbnailHdr, gc);
		gc.gridwidth = 1; gc.weightx = 0.0;

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
		panel.add(new javax.swing.JLabel("Axis label font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsAxisLabelFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
		panel.add(graphicsAxisLabelFontSizeSpinner, gc);

		gc.gridy++; gc.gridx = 0; gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		panel.add(new javax.swing.JLabel("Axis tick/value font size:"), gc);
		gc.gridx = 1; gc.fill = java.awt.GridBagConstraints.NONE;
		graphicsAxisTickFontSizeSpinner = graphicsFontSpinner(props, InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY, InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
		panel.add(graphicsAxisTickFontSizeSpinner, gc);

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
				graphicsAxisLabelFontSizeSpinner, graphicsAxisTickFontSizeSpinner,
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
			graphicsAxisLabelFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE);
			graphicsAxisTickFontSizeSpinner.setValue(InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE);
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
		if (graphicsAxisLabelFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_AXIS_LABEL_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsAxisLabelFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_LABEL_FONT_SIZE));
		if (graphicsAxisTickFontSizeSpinner != null)
			p.setProperty(InterfaceMain.GRAPHICS_AXIS_TICK_FONT_SIZE_PROPERTY,
					boundedIntStr(graphicsAxisTickFontSizeSpinner, InterfaceMain.DEFAULT_GRAPHICS_AXIS_TICK_FONT_SIZE));
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

	// ---------------------------------------------------------------------------
	// Widget factories
	// ---------------------------------------------------------------------------

	private javax.swing.JSpinner graphicsFontSpinner(Properties props, String key, int defaultValue) {
		int value = InterfaceMain.parseBoundedIntValue(props.getProperty(key), defaultValue,
				InterfaceMain.MIN_GRAPHICS_FONT_SIZE, InterfaceMain.MAX_GRAPHICS_FONT_SIZE);
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
}
