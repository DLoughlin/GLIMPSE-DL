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
*/
package ModelInterface.common;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

import ModelInterface.InterfaceMain;

/**
 * This class wraps an AWT FileDialog so that it can easily use
 * it for the FileChooser without changing the code
 * which needs to select files.
 * @author Pralit Patel 
 */
public class AWTFileChooserWrapper implements FileChooser {
	private static final boolean DEBUG_NATIVE_FALLBACK =
			"true".equalsIgnoreCase(System.getProperty("modelinterface.nativeFileDialog.debug", "false"));

	private static void debugLog(String msg) {
		if (DEBUG_NATIVE_FALLBACK) {
			System.out.println("[ModelInterface FileChooser DEBUG][AWT] " + msg);
		}
	}

	/**
	 * Default Constuctor.
	 */
	public AWTFileChooserWrapper() {
		// do nothing
	}

	public File[] doFilePrompt(final Component parent, final String title, 
			final int loadOrSave, final File setFile, final FileFilter fileFilter) {
		return doFilePrompt(parent, title, loadOrSave, setFile, fileFilter, null, null);
	}

	public File[] doFilePrompt(final Component parent, final String title, 
			final int loadOrSave, final File setFile, final FileFilter fileFilter,
			final ActionListener l, final String actionCommand) {
		// should I try to keep this FileDialog cached some how..
		debugLog("Preparing native AWT dialog: op=" + (loadOrSave == FileChooser.SAVE_DIALOG ? "save" : "open")
				+ ", title='" + (title == null ? "" : title) + "'");
		
		FileDialog toWrap = null;
		if(parent instanceof Frame) {
			toWrap = new FileDialog((Frame)parent, title);
		} else if(parent instanceof Dialog) {
			toWrap = new FileDialog((Dialog)parent, title);
		} else {
			// should I throw an exception, use the Main window as the parent or
			// some thing else?
			/*
			throw new UnsupportedOperationException(
					"This FileChooser does not support a parent Component of such a type.");
					*/
			System.out.println("WARNING: This FileChooser does not support a parent Component of such a type.");
			toWrap = new FileDialog(InterfaceMain.getInstance().getFrame(), title);
		}
		
		if(!setFile.isDirectory()) {
			toWrap.setDirectory(setFile.getParent());
			toWrap.setFile(setFile.getName());
		} else {
			toWrap.setDirectory(setFile.getAbsolutePath());
		}
		if(loadOrSave == FileChooser.LOAD_DIALOG) {
			toWrap.setMode(FileDialog.LOAD);
		} else if(loadOrSave == FileChooser.SAVE_DIALOG) {
			toWrap.setMode(FileDialog.SAVE);
		} else {
			System.out.println("Invalid load/save flag");
			assert(false);
		}
		toWrap.setFilenameFilter(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// a null fileFilter means accept all files
				if(fileFilter == null) {
					return true;
				}
				try {
					if(name == null || name.length() == 0) {
						return true;
					}
					File candidate = dir != null ? new File(dir, name) : new File(name);
					return fileFilter.accept(candidate);
				} catch(Throwable t) {
					// Keep native dialog usable even if a custom filter callback is brittle.
					return true;
				}
			}
		});

		// TODO: find a better way as this is a hack
        String canSelectDirectories = "false";
		if(fileFilter != null && fileFilter.getDescription().startsWith("Directory")) {
            canSelectDirectories = "true";
            // Must be set to LOAD to select a directory
			toWrap.setMode(FileDialog.LOAD);
		}
		
        // TODO: this is a marginal hack to work for Macs only
        final String propName = "apple.awt.fileDialogForDirectories";
        System.setProperty(propName, canSelectDirectories);
		debugLog("Showing native AWT dialog now (blocking until close)");
		toWrap.setVisible(true);
        System.setProperty(propName, "false");
		debugLog("Native AWT dialog closed by user");
		String result = toWrap.getFile();
		if(result == null) {
			debugLog("Native AWT result: null (cancel/no selection)");
			return null;
		} else {
			// FileDialog does not seem to support multiple file
			// selection
			File[] ret = new File[1];
			ret[0] = new File(toWrap.getDirectory(), result);
			debugLog("Native AWT result: " + ret[0].getAbsolutePath());
			if(l != null && actionCommand != null) {
				RecentFilesList.getInstance().addFile(ret, l, actionCommand);
			}
			return ret;
		}
	}
}
