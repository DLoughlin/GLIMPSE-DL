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
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.filechooser.FileFilter;

/**
 * Wrapper which attempts a native chooser first and falls back to the
 * configured Java chooser only when the native chooser fails unexpectedly.
 */
public class NativeFirstFileChooserWrapper implements FileChooser {
	private final FileChooser nativeChooser;
	private final FileChooser fallbackChooser;
	private static final String DEBUG_LOG_FILE = System.getProperty("user.home", ".")
			+ File.separator + "modelinterface-filechooser-debug.log";

	private static boolean isDebugNativeFallbackEnabled() {
		return "true".equalsIgnoreCase(System.getProperty("modelinterface.nativeFileDialog.debug", "false"));
	}

	public NativeFirstFileChooserWrapper(FileChooser nativeChooser, FileChooser fallbackChooser) {
		this.nativeChooser = nativeChooser;
		this.fallbackChooser = fallbackChooser;
	}

	public File[] doFilePrompt(final Component parent, final String title,
			final int loadOrSave, final File setFile, final FileFilter fileFilter) {
		return doFilePrompt(parent, title, loadOrSave, setFile, fileFilter, null, null);
	}

	public File[] doFilePrompt(final Component parent, final String title,
			final int loadOrSave, final File setFile, final FileFilter fileFilter,
			final ActionListener actionListener, final String actionCommand) {
		if (isDebugNativeFallbackEnabled()) {
			debugLog("Request: op=" + (loadOrSave == FileChooser.SAVE_DIALOG ? "save" : "open")
					+ ", title='" + (title == null ? "" : title) + "', seed='"
					+ (setFile == null ? "<null>" : setFile.getAbsolutePath()) + "'", null);
		}
		if (isWindows() && isDirectoryRequest(fileFilter) && loadOrSave == FileChooser.LOAD_DIALOG) {
			try {
				if (isDebugNativeFallbackEnabled()) {
					debugLog("Attempting Windows native directory chooser", null);
				}
				File[] windowsResult = showWindowsNativeDirectoryChooser(title, setFile);
				if (isDebugNativeFallbackEnabled()) {
					debugLog("Windows native directory chooser returned "
							+ (windowsResult == null ? "null (cancel/no selection)" : (windowsResult.length + " file(s)")),
							null);
				}
				if (windowsResult != null && actionListener != null && actionCommand != null
						&& "Open DB".equals(actionCommand)) {
					RecentFilesList.getInstance().addFile(windowsResult, actionListener, actionCommand);
				}
				return windowsResult;
			} catch (Throwable t) {
				if (isDebugNativeFallbackEnabled()) {
					debugLog("Windows native directory chooser failed; falling back to chooser wrappers", t);
				}
			}
		}

		if (nativeChooser != null) {
			try {
				if (isDebugNativeFallbackEnabled()) {
					debugLog("Attempting native chooser", null);
				}
				File[] nativeResult = nativeChooser.doFilePrompt(parent, title, loadOrSave, setFile, fileFilter,
						actionListener, actionCommand);
				if (isDebugNativeFallbackEnabled()) {
					debugLog("Native chooser returned "
							+ (nativeResult == null ? "null (cancel/no selection)" : (nativeResult.length + " file(s)")),
							null);
				}
				return nativeResult;
			} catch (Throwable t) {
				logNativeFallback(loadOrSave, title, t);
			}
		}
		if (fallbackChooser != null) {
			if (isDebugNativeFallbackEnabled()) {
				debugLog("Using fallback Java chooser", null);
			}
			File[] fallbackResult = fallbackChooser.doFilePrompt(parent, title, loadOrSave, setFile, fileFilter,
					actionListener, actionCommand);
			if (isDebugNativeFallbackEnabled()) {
				debugLog("Fallback chooser returned "
						+ (fallbackResult == null ? "null (cancel/no selection)" : (fallbackResult.length + " file(s)")),
						null);
			}
			return fallbackResult;
		}
		if (isDebugNativeFallbackEnabled()) {
			debugLog("No chooser available; returning null", null);
		}
		return null;
	}

	private void logNativeFallback(final int loadOrSave, final String title, final Throwable t) {
		String operation = loadOrSave == FileChooser.SAVE_DIALOG ? "save" : "open";
		String safeTitle = title == null ? "" : title;
		String reason = t == null ? "unknown" : (t.getClass().getSimpleName() + ": " + t.getMessage());
		System.out.println("[ModelInterface FileChooser] Native dialog failed for " + operation
				+ " (title='" + safeTitle + "'); falling back to Java chooser. Reason: " + reason);
		if (isDebugNativeFallbackEnabled() && t != null) {
			t.printStackTrace(System.out);
		}
		if (isDebugNativeFallbackEnabled()) {
			debugLog("Fallback activation: " + reason, t);
		}
	}

	private static synchronized void debugLog(String message, Throwable t) {
		if (!isDebugNativeFallbackEnabled()) {
			return;
		}
		String line = "[" + new Date() + "] " + message;
		System.out.println("[ModelInterface FileChooser DEBUG] " + line);
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(DEBUG_LOG_FILE, true));
			out.println(line);
			if (t != null) {
				t.printStackTrace(out);
			}
		} catch (IOException ignored) {
			// Keep debug logging best-effort only.
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name", "");
		return os.toLowerCase().contains("win");
	}

	private static boolean isDirectoryRequest(FileFilter fileFilter) {
		if (fileFilter == null) {
			return false;
		}
		try {
			String description = fileFilter.getDescription();
			return description != null && description.startsWith("Directory");
		} catch (Throwable t) {
			return false;
		}
	}

	private static File[] showWindowsNativeDirectoryChooser(String title, File seedDir) throws Exception {
		String initialDir = ".";
		if (seedDir != null) {
			if (seedDir.isDirectory()) {
				initialDir = seedDir.getAbsolutePath();
			} else if (seedDir.getParentFile() != null) {
				initialDir = seedDir.getParentFile().getAbsolutePath();
			}
		}

		String safeTitle = escapePsSingleQuoted(title == null ? "Choose Folder" : title);
		String safeInitialDir = escapePsSingleQuoted(initialDir);
		String markerName = "Select Folder";

		String script =
				"Add-Type -AssemblyName System.Windows.Forms; " +
				"$dlg = New-Object System.Windows.Forms.OpenFileDialog; " +
				"$dlg.Title = '" + safeTitle + "'; " +
				"$dlg.InitialDirectory = '" + safeInitialDir + "'; " +
				"$dlg.CheckFileExists = $false; " +
				"$dlg.CheckPathExists = $true; " +
				"$dlg.ValidateNames = $false; " +
				"$dlg.DereferenceLinks = $true; " +
				"$dlg.Multiselect = $false; " +
				"$dlg.FileName = '" + markerName + "'; " +
				"$res = $dlg.ShowDialog(); " +
				"if ($res -eq [System.Windows.Forms.DialogResult]::OK) { " +
				"  $picked = $dlg.FileName; " +
				"  if ([System.IO.File]::Exists($picked)) { $picked = Split-Path -Parent $picked; } " +
				"  if ($picked.EndsWith('\\" + markerName + "')) { $picked = Split-Path -Parent $picked; } " +
				"  Write-Output $picked; " +
				"}";

		ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA", "-Command", script);
		pb.redirectErrorStream(false);
		Process p = pb.start();

		List<String> outLines = new ArrayList<String>();
		List<String> errLines = new ArrayList<String>();
		BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line;
		while ((line = out.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				outLines.add(line.trim());
			}
		}
		while ((line = err.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				errLines.add(line.trim());
			}
		}

		int exit = p.waitFor();
		if (exit != 0) {
			throw new IOException("PowerShell native directory chooser failed. Exit=" + exit + ", stderr=" + errLines);
		}

		if (outLines.isEmpty()) {
			return null;
		}

		File selected = new File(outLines.get(outLines.size() - 1));
		if (!selected.exists()) {
			File parent = selected.getParentFile();
			if (parent != null && parent.exists()) {
				selected = parent;
			}
		}
		return new File[] { selected };
	}

	private static String escapePsSingleQuoted(String text) {
		return text == null ? "" : text.replace("'", "''");
	}
}
