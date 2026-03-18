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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import ModelInterface.InterfaceMain;
import ModelInterface.InterfaceMain.MenuManager;
import ModelInterface.MenuAdder;

/**
 * A singleton class which will handle all aspects of listing recent files.
 * These aspects include collecting a list of recently Open/Save As files, 
 * updating the Recent Files menu, having them opened by the correct 
 * ActionListener, and use the Interface's properties to load/store the
 * recent files list.  An action listener should check the source of the
 * action since if it comes from a RecentFile it should not prompt for a
 * file to open.  Only finite number of recent files will be kept and
 * that number is determined by the RecentFilesLength property.
 *
 * @author Pralit Patel 
 */
public class RecentFilesList implements MenuAdder {
	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	private static void logStartup(String stage, long startNanos) {
		InterfaceMain.logStartupTiming("RecentFilesList:" + stage + " " + elapsedMillis(startNanos) + " ms");
	}

	/**
	 * Private instance of this class.
	 */
	private static final RecentFilesList instance = new RecentFilesList();

	/**
	 * The menu this class will add.  This menu will contain a
	 * menu item for each recent file available.
	 */
	private final JMenu recentFilesMenu = new JMenu("Open Recent DB"); // renamed to DB-specific
	// A persistent "Clear Menu" item at the end of the list
	private final JMenuItem clearMenuItem = new JMenuItem("Clear Menu");

	/**
	 * The number of recent files this list will keep track of.
	 */
	private int recentFilesLength;

	/**
	 * The max length a file name in the list can be.
	 */
	private static final int MAX_TITLE_LENGTH = 40;

	/**
	 * Private constructor.
	 */
	private RecentFilesList() {
		final long initStart = System.nanoTime();
		InterfaceMain.getInstance().getFrame().addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getPropertyName().equals("Control") 
					&& evt.getNewValue().equals("ModelInterface")) {
					doSetProperties();
				}
			}
		});
		logStartup("constructor:listener registered", initStart);
		// setup Clear Menu behavior
		clearMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clearRecentFiles();
			}
		});
		// mnemonic for submenu
		recentFilesMenu.setMnemonic(java.awt.event.KeyEvent.VK_D);
		logStartup("constructor:complete", initStart);
	}

	/**
	 * Get the instance of this class
	 * @return The instance of this class.
	 */
	public static RecentFilesList getInstance() {
		return instance;
	}

	public void addMenuItems(MenuManager menuMan) {
		final long menuStart = System.nanoTime();
		// now is the time create the MenuItems for each recent file.
		Properties prop = InterfaceMain.getInstance().getProperties();
		recentFilesLength = 5;
		try {
			String lengthStr =  prop.getProperty("RecentFilesLength", "5");
			prop.setProperty("RecentFilesLength", lengthStr);
			recentFilesLength = Integer.parseInt(lengthStr);
		} catch(NumberFormatException nfe) {
			System.out.println("Could not parse length of recent files list: " + recentFilesLength + " , setting to "+recentFilesLength);
			
		}
		logStartup("addMenuItems:properties", menuStart);

		// should I add these through the menu manager?
		recentFilesMenu.removeAll();
		for(int i = 1; i <= recentFilesLength; ++i) {
			String filesStr = prop.getProperty("RecentFile"+i);
			String targetStr = prop.getProperty("RecentFileTarget"+i);

			// if we get back null that means that we have < RecentFilesLength
			// in the properties so just go ahead and stop now
			if(filesStr == null || targetStr == null) {
				break;
			}

			// files are seperated by semicolons
			String[] filesSplit = filesStr.split(";");
			// target will be stored targetclass;actionCommand
			String[] targetSplit = targetStr.split(";");
			if(filesSplit.length < 1 || targetSplit.length != 2) {
				continue;
			}
			// Only include DB entries (actionCommand must be "Open DB")
			if(!"Open DB".equals(targetSplit[1])) {
				continue;
			}
			File[] files = new File[filesSplit.length];
			for(int j = 0; j < files.length; ++j) {
				files[j] = new File(filesSplit[j]);
			}

			recentFilesMenu.add(new RecentFile(files, targetSplit[0], targetSplit[1]));
		}
		logStartup("addMenuItems:entries built", menuStart);

		// add Clear Menu with separator at the end
		if(recentFilesMenu.getItemCount() > 0) {
			recentFilesMenu.addSeparator();
		}
		recentFilesMenu.add(clearMenuItem);

		menuMan.getSubMenuManager(InterfaceMain.FILE_MENU_POS).
			addMenuItem(recentFilesMenu, InterfaceMain.FILE_OPEN_SUBMENU_POS);
		logStartup("addMenuItems:complete", menuStart);
	}

	/**
	 * Reset recent list and clear corresponding properties.
	 */
	private void clearRecentFiles() {
		// remove all items then add back the Clear command
		recentFilesMenu.removeAll();
		recentFilesMenu.add(clearMenuItem);
		// wipe properties for recent files using persistent API with batch update
		InterfaceMain.getInstance().updateProperties(prop -> {
			for(int i = 1; i <= recentFilesLength; ++i) {
				prop.remove("RecentFile"+i);
				prop.remove("RecentFileTarget"+i);
			}
		});
	}

	/**
	 * Sets the properties with the latest recent files.
	 */
	private void doSetProperties() {
		InterfaceMain.getInstance().updateProperties(prop -> {
			// first clear existing entries to avoid stale values
			for(int i = 1; i <= recentFilesLength; ++i) {
				prop.remove("RecentFile"+i);
				prop.remove("RecentFileTarget"+i);
			}
			Component[] theFiles = recentFilesMenu.getMenuComponents();
			int idx = 1;
			for(int i = 0; i < theFiles.length; ++i) {
				if(!(theFiles[i] instanceof RecentFile)) continue; // skip separators/clear
				RecentFile f = (RecentFile)theFiles[i];
				prop.setProperty("RecentFile"+idx, f.getFilePaths());
				prop.setProperty("RecentFileTarget"+idx, f.getTargetName()+";"+f.getActionCommand());
				idx++;
			}
		});
	}

	/**
	 * Called to notify that a file was opened or saved that could be re-opened and 
	 * thus will be added to the recent files list.  If the file is already in the
	 * list it will be moved up the list.  If we already have the max allowed
	 * recent files then the least recently opened will be removed.
	 * @param files The files opened.
	 * @param source The ActionListener will be responsible for opening the files.
	 * @param actionCommand The command for the event so it gets opened. 
	 */ 
	public void addFile(File[] files, ActionListener source, String actionCommand) {
		// Only track databases in this menu
		if(!"Open DB".equals(actionCommand)) {
			return;
		}
		JMenuItem temp = new RecentFile(files, source, actionCommand);
		int pos;
		if((pos = doesMenuContain(temp)) != -1) {
			recentFilesMenu.remove(pos);
		} else if(getRecentFileCount() >= recentFilesLength) {
			removeLastRecentFile();
		}
		recentFilesMenu.insert(temp, 0);
		doSetProperties();
	}
	
	/**
	 * Called to notify that a file was opened or saved that could be re-opened and 
	 * thus will be added to the recent files list.  If the file is already in the
	 * list it will be moved up the list.  If we already have the max allowed
	 * recent files then the least recently opened will be removed.
	 * @param files The files opened.
	 * @param source The class will be responsible for opening the files.
	 * @param actionCommand The command for the event so it gets opened. 
	 */ 
	public void addFile(File[] files, String source, String actionCommand) {
		// Only track databases in this menu
		if(!"Open DB".equals(actionCommand)) {
			return;
		}
		JMenuItem temp = new RecentFile(files, source, actionCommand);
		int pos;
		if((pos = doesMenuContain(temp)) != -1) {
			recentFilesMenu.remove(pos);
		} else if(getRecentFileCount() >= recentFilesLength) {
			removeLastRecentFile();
		}
		recentFilesMenu.insert(temp, 0);
		doSetProperties();
	}
	
	/**
	 * Count only RecentFile items in the menu (exclude separators and the Clear item).
	 */
	private int getRecentFileCount() {
		int count = 0;
		for (java.awt.Component c : recentFilesMenu.getMenuComponents()) {
			if (c instanceof RecentFile) count++;
		}
		return count;
	}
	/**
	 * Remove the last (oldest) RecentFile entry, keeping the Clear item/separators intact.
	 */
	private void removeLastRecentFile() {
		java.awt.Component[] comps = recentFilesMenu.getMenuComponents();
		for (int i = comps.length - 1; i >= 0; i--) {
			if (comps[i] instanceof RecentFile) {
				recentFilesMenu.remove(i);
				break;
			}
		}
	}

	/**
	 * Looks at the subelements of the recent files list menu and 
	 * determines if it contains the passed in menu item.
	 * @param item The item which is being search for
	 * @return The position in which it was found, or -1 if not found
	 */
	private int doesMenuContain(JMenuItem item) {
		java.awt.Component[] comps = recentFilesMenu.getMenuComponents();
		for (int i = 0; i < comps.length; ++i) {
			if (!(comps[i] instanceof JMenuItem)) continue;
			JMenuItem mi = (JMenuItem) comps[i];
			// mi may be null for separators when using getItem; using components avoids that,
			// but keep a null guard just in case
			if (mi != null && mi.equals(item)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Displays an error message dialog with a reason for
	 * why it was not able to open the recent file.
	 * @param reason A brief explanation of what went wrong.
	 */
	private static void showNoOpenError(String reason) {
		InterfaceMain.getInstance().showMessageDialog(reason, "Could Not Open File Error",
				JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * A menu item which represents a recent file.  It listen for it's own
	 * actions and pass it on the the appropriate ActionListener so that it
	 * will open the file. A RecentFile is immutable.
	 * @author Pralit Patel
	 */
	public class RecentFile extends JMenuItem implements ActionListener {
		/**
		 * The files that should be opened.
		 */
		private final File[] files;

		/**
		 * The class name of the target which will do
		 * the opening.
		 */
		private final String targetName;

		/**
		 * The action command to give when calling the
		 * actionPerformed
		 */
		private final String actionCommand;

		/**
		 * Constructs a RecentFile.  Used when reading creating from the properties
		 * since the targetName is already determined.
		 * @param files The files to open
		 * @param targetname The class name for that will do the opening.
		 * @param actionCommand The command to give the ActionEvent.
		 */
		public RecentFile(File[] files, String targetName, String actionCommand) {
			super(createTitle(files[0].getAbsolutePath()));
			this.files = files;
			this.targetName = targetName;
			this.actionCommand = actionCommand;
			addActionListener(this);
			setToolTipText(files[0].getAbsolutePath());
		}

		/**
		 * Constructs a RecentFile.  Used when a file was opened/saved.  It will
		 * have to determine the class name to use so that it would be able to 
		 * reopen the file again.
		 * @param files The files to open
		 * @param list The listener that could open this file again.
		 * @param actionCommand The command to give the ActionEvent.
		 */
		public RecentFile(File[] files, ActionListener list, String actionCommand) {
			super(createTitle(files[0].getName()));
			this.files = files;
			this.targetName = list.getClass().getName();
			this.actionCommand = actionCommand;
			addActionListener(this);
			setToolTipText(files[0].getAbsolutePath());
		}

		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// this would be a good time to make sure the files exist
			for(File file : files) {
				if(!file.exists()) {
					System.out.println("Does not exist anymore");
					showNoOpenError("The file no longer exists.");
					return;
				}
			}
			ActionEvent newE = new ActionEvent(this, e.getID(), actionCommand);
			try {
				MenuAdder target = InterfaceMain.getInstance().getMenuAdder(targetName);
				target.getClass().getMethod("actionPerformed", 
						ActionEvent.class).invoke(target, newE);
			} catch(NoSuchMethodException methodE) {
				showNoOpenError("Could not find target to open.");
				methodE.printStackTrace();
			} catch(IllegalAccessException accessE) {
				showNoOpenError("Could not find target to open.");
				accessE.printStackTrace();
			} catch(InvocationTargetException invokeE) {
				showNoOpenError("Could not find target to open.");
				invokeE.printStackTrace();
			} catch(NullPointerException nullE) {
				showNoOpenError("Could not find target to open.");
				nullE.printStackTrace();
			}
		}
		
		/**
		 * Get the files of this recent file.
		 * @return The files.
		 */
		public File[] getFiles() {
			return files;
		}

		/**
		 * Get the files as absolute file paths concatonated together
		 * with semicolons.
		 * @return The file paths ready to added to the properties.
		 */
		public String getFilePaths() {
			StringBuilder ret = new StringBuilder();
			for(File file : files) {
				ret.append(file.getAbsolutePath()).append(";");
			}
			return ret.toString();
		}

		/**
		 * Get the target name.
		 * @return Target class name.
		 */
		public String getTargetName() {
			return targetName;
		}

		/**
		 * Get the action command that will be used.
		 * @return The action command.
		 */
		public String getActionCommand() {
			return actionCommand;
		}

		public boolean equals(Object other) {
			if(other == null || !(other instanceof RecentFile)) {
				return false;
			} else {
				RecentFile o = (RecentFile)other;
				if(files.length != o.files.length) {
					return false;
				}
				for(int i = 0; i < files.length; ++i) {
					if(!files[i].equals(o.files[i])) {
						return false;
					}
				}
				return targetName.equals(o.targetName) && 
					actionCommand.equals(o.actionCommand);
			}
		}

		public int hashCode() {
			return files.hashCode() ^ targetName.hashCode() ^ actionCommand.hashCode();
		}
	}

	/**
	 * Create a title that is no longer that MAX_TITLE_LENGTH.  The rest will
	 * be abbreviated with a .. also the full file path may be viewed through
	 * the tool tip.
	 * @param actionCommand The action command used to open the file.
	 * @param fileName The file name to abbreviate.
	 * @return The title that is no longer than MAX_TITLE_LENGTH
	 */
	private static String createTitle(String fileName) {
		// subtract off the actionCommand length and 4 for ": .."
		int maxFileLen = MAX_TITLE_LENGTH - 3; // 3 for ".. "
		if(fileName.length() <= maxFileLen) {
			return fileName;
		} else {
			return ".."+fileName.substring(
					fileName.length() - maxFileLen -1, fileName.length());
		}
	}
}