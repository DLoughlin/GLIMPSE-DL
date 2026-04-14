/*
 * LEGAL NOTICE
 * This computer software was prepared by Battelle Memorial Institute,
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
 * with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
 * CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 *
 * Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
 * Distributed as open-source under the terms of the Educational Community
 * License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
 */
package ModelInterface;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.JFrame;

/**
 * Callback interface used by {@link PreferenceDialog} to interact with the
 * host application. Decouples the dialog implementation from
 * {@link InterfaceMain} so the dialog can be tested or reused independently.
 */
interface PreferenceDialogCallbacks {

	/** The Swing frame to use as dialog owner and for relative positioning. */
	JFrame getOwnerFrame();

	/**
	 * Returns a snapshot of the current saved application properties.
	 * The returned object may be a defensive copy.
	 */
	Properties getProperties();

	/**
	 * Apply a batch of property changes and persist them to disk.
	 *
	 * @param updates consumer that modifies the live {@link Properties} object
	 */
	void updateProperties(Consumer<Properties> updates);

	/** Show a modal message dialog rooted at the owner frame. */
	void showMessageDialog(Object message, String title, int messageType);

	/**
	 * Apply a new UI font size immediately to all open windows and persist the
	 * change.
	 */
	void applyFontSize(int newSize);

	/** Return the currently active configured UI font size. */
	int getCurrentFontSize();

	/**
	 * Open {@code file} in the configured editor for the given file type.
	 *
	 * @param file the file to open (must not be {@code null})
	 * @param type editor key, e.g. {@code "xml"}, {@code "csv"}, {@code "txt"}
	 */
	void openEditorForFile(File file, String type);

	/**
	 * Dispatch an {@link ActionEvent} to the main action handler so the dialog
	 * can trigger file-chooser actions (e.g. "Select Units File") that live in
	 * the host.
	 */
	void dispatchMenuAction(ActionEvent e);
}
