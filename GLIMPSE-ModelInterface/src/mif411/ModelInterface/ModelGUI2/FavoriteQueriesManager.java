package ModelInterface.ModelGUI2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;

import ModelInterface.InterfaceMain;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import ModelInterface.ModelGUI2.QueryTreeModel.QueryGroup;
import ModelInterface.ModelGUI2.queries.QueryGenerator;

public class FavoriteQueriesManager {

    private final JTree queryList;
    private final JScrollPane listScrollQueries;

    public FavoriteQueriesManager(JTree queryList, JScrollPane listScrollQueries) {
        this.queryList = queryList;
        this.listScrollQueries = listScrollQueries;
    }

	private Component getDialogParent() {
		Component owner = null;
		try {
			owner = InterfaceMain.getInstance().getFrame();
		} catch (Exception ignored) {
		}
		if (owner == null) {
			owner = listScrollQueries != null ? listScrollQueries : queryList;
		}
		return owner;
	}

    /**
	 * Selects favorite queries in the query tree based on the favorite queries
	 * file.
	 */
	public void selectFavoriteQueries() {

		if (queryList == null || queryList.getModel() == null) {
			JOptionPane.showMessageDialog(getDialogParent(),
					"Query tree is not ready yet. Please wait for startup to finish before applying favorites.",
					"Favorite Queries", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		expandAllRows(queryList);
		ArrayList<String> favoriteQueryLines = new ArrayList<>();
		ArrayList<TreePath> favoriteQueryPaths = new ArrayList<>();
		ArrayList<String> favoriteQueryNames = new ArrayList<>();
		String favoriteQueryFilename = InterfaceMain.favoriteQueriesFileLocation;
		if (favoriteQueryFilename == null || favoriteQueryFilename.trim().isEmpty()) {
			JOptionPane.showMessageDialog(getDialogParent(),
					"No favorite queries file is configured. Please load or create one first.",
					"Favorite Queries", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		File favoritesFile = new File(favoriteQueryFilename);
		if (!favoritesFile.exists()) {
			JOptionPane.showMessageDialog(getDialogParent(),
					"Favorite queries file was not found:\n" + favoriteQueryFilename,
					"Favorite Queries", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			favoriteQueryLines = getStringArrayFromFile(favoriteQueryFilename, "#");
		} catch (Exception e) {
			System.out.println("Could not read favorite queries file: " + e);
			JOptionPane.showMessageDialog(getDialogParent(), "Unable to load favorites file, please see console for error",
					"Error Saving File", JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (String line : favoriteQueryLines) {
			if (!line.isEmpty()) {
				TreePath path = getTreePathForEachLine(queryList, line);
				if (path != null) {
					favoriteQueryPaths.add(path);
				} else {
					System.out.println("Unable to find path for " + line);
				}
				String[] splitLine = line.split(">");
				favoriteQueryNames.add(splitLine[splitLine.length - 1]);
			}
		}
		int[] rowsToSelect = new int[favoriteQueryPaths.size()];
		ArrayList<String> notFound = new ArrayList<>();
		for (int i = 0; i < favoriteQueryPaths.size(); i++) {
			TreePath treePath = favoriteQueryPaths.get(i);
			String leafName = favoriteQueryNames.get(i);
			int rowNum = getRowNumberForLeaf(queryList, treePath, leafName);
			rowsToSelect[i] = rowNum;
			if (rowNum == -1) {
				notFound.add(leafName);
			}
		}
		if (notFound.size() != rowsToSelect.length) {
			queryList.clearSelection();
			// Only select valid rows
			ArrayList<Integer> validRows = new ArrayList<>();
			for (int row : rowsToSelect) {
				if (row != -1)
					validRows.add(row);
			}
			int[] validRowsArray = validRows.stream().mapToInt(Integer::intValue).toArray();
			queryList.setSelectionRows(validRowsArray);
			// Scroll to last found favorite query
			if (validRowsArray.length > 0) {
				java.awt.Rectangle bounds = queryList.getRowBounds(validRowsArray[validRowsArray.length - 1]);
				if (bounds != null && listScrollQueries != null) {
					listScrollQueries.getVerticalScrollBar().setValue((int) bounds.getMinY());
				}
			}
		}
		if (!notFound.isEmpty()) {
			StringBuilder errorMessage = new StringBuilder("The following queries were not found:\n\n");
			for (String s : notFound) {
				errorMessage.append(s).append("\n");
			}
			JOptionPane.showMessageDialog(getDialogParent(), errorMessage.toString());
		}
	}

    /**
	 * Creates a file containing the selected queries as user's favorite queries.
	 */
	public void createFavoriteQueriesFile() {
		TreePath[] selectedTreePath = queryList.getSelectionPaths();
		if (selectedTreePath == null || selectedTreePath.length == 0) {
			JOptionPane.showMessageDialog(getDialogParent(), "Please select at least one query to save.");
			return;
		}

		boolean hasLeaf = false;
		for (TreePath tp : selectedTreePath) {
			if (tp.getLastPathComponent() instanceof QueryGenerator)
				hasLeaf = true;
		}
		if (!hasLeaf) {
			JOptionPane.showMessageDialog(getDialogParent(), "Please select at least one query to save.");
			return;
		}

		String PathToUse = null;
		if (InterfaceMain.favoriteQueriesFileLocation != null) {
			File f = new File(InterfaceMain.favoriteQueriesFileLocation);
			if (f.exists()) {
				PathToUse = f.getParent();
			}
		}

		FileChooser fileChooser = FileChooserFactory.getFileChooser();
		File seedFile = PathToUse != null ? new File(PathToUse) : new File(System.getProperty("user.home", "."));
		File[] selected = fileChooser.doFilePrompt(getDialogParent(),
				"Specify a favorite queries file to save",
				FileChooser.SAVE_DIALOG,
				seedFile,
				null);

		if (selected != null && selected.length > 0 && selected[0] != null) {
			File selectedFile = selected[0];
			try {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(selectedFile, false)));
				String convertedLine = "";
				for (int i = 0; i < selectedTreePath.length; i++) {

					TreePath treePathNow = selectedTreePath[i];
					if (treePathNow.getLastPathComponent() instanceof QueryGenerator) {
						int treePathCount = treePathNow.getPathCount();
						String pathStr = selectedTreePath[i].toString();
						String lineStr = pathStr.substring(1, pathStr.length() - 1);// remove the square brackets
						int commaCount = countCommaInPath(lineStr);
						if (commaCount > treePathCount - 1) { // there are commas inside queryGroup
							convertedLine = convertPathWithCommaToLine(treePathNow);
						} else {
							convertedLine = convertPathToLine(lineStr);
						}
					}
					// System.out.println("converted line is:" + convertedLine);
					writer.write(convertedLine);
					writer.newLine();
				}
				writer.close();
				String messageFileSaved = selectedFile.toString()
						+ " has been saved.\n\n Would you like to make this the active favorites file?";
				int answer = JOptionPane.showConfirmDialog(getDialogParent(), messageFileSaved, "Switch?",
						JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					InterfaceMain.favoriteQueriesFileLocation = selectedFile.getAbsolutePath();
				}
			} catch (IOException e) {
				System.out.println("Could not save file: " + e.toString());
				JOptionPane.showMessageDialog(getDialogParent(), "Unable to save file, please see console for error",
						"Error Saving File", JOptionPane.ERROR_MESSAGE);
			}

		}

	}

    /**
	 * Appends selected queries to the favorite queries file.
	 */
	public void appendFavoriteQueries() {
		TreePath[] selectedTreePath = queryList.getSelectionPaths();
		if (selectedTreePath == null || selectedTreePath.length == 0) {
			JOptionPane.showMessageDialog(getDialogParent(), "Please select at least one query to append.");
			return;
		}
		String favoritesFilePath = InterfaceMain.favoriteQueriesFileLocation;
		if (favoritesFilePath == null || favoritesFilePath.trim().isEmpty()) {
			JOptionPane.showMessageDialog(getDialogParent(),
					"No favorite queries file specified. Please load or create a file first.");
			return;
		}
		File favoritesFile = new File(favoritesFilePath);
		if (!favoritesFile.exists()) {
			String messageFileNotFound = "Favorite queries list file " + favoritesFilePath
					+ " could not be found to be appended to.\nPlease load or create a file first.";
			JOptionPane.showMessageDialog(getDialogParent(), messageFileNotFound);
			return;
		}
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(favoritesFile, true)))) {
			for (TreePath treePathNow : selectedTreePath) {
				int treePathCount = treePathNow.getPathCount();
				String pathStr = treePathNow.toString();
				String lineStr = pathStr.substring(1, pathStr.length() - 1); // remove the square brackets
				int commaCount = countCommaInPath(lineStr);
				String convertedLine;
				if (commaCount > treePathCount - 1) { // there are commas inside queryGroup
					convertedLine = convertPathWithCommaToLine(treePathNow);
				} else {
					convertedLine = convertPathToLine(lineStr);
				}
				writer.write(convertedLine);
				writer.newLine();
			}
			writer.flush();
			String messageQueriesAppended = "The selected queries have been appended to " + favoritesFilePath;
			JOptionPane.showMessageDialog(getDialogParent(), messageQueriesAppended);
		} catch (IOException e) {
			System.out.println("Could not append to favorite queries: " + e);
			JOptionPane.showMessageDialog(getDialogParent(), "Unable to append to file, please see console for error",
					"Error Appending File", JOptionPane.ERROR_MESSAGE);
		}
	}

    /**
	 * Loads favorite queries file and applies it if requested by the user.
	 */
	public void loadFavoriteQueriesFile() {
		String PathToUse = null;
		if (InterfaceMain.favoriteQueriesFileLocation != null) {
			File f = new File(InterfaceMain.favoriteQueriesFileLocation);
			if (f.exists()) {
				PathToUse = f.getParent();
			}
		}

		FileChooser fileChooser = FileChooserFactory.getFileChooser();
		File seedFile = PathToUse != null ? new File(PathToUse) : new File(System.getProperty("user.home", "."));
		File[] selected = fileChooser.doFilePrompt(getDialogParent(),
				"Select a query file to load.",
				FileChooser.LOAD_DIALOG,
				seedFile,
				null);

		if (selected != null && selected.length > 0 && selected[0] != null) {
			File selectedFile = selected[0];
			InterfaceMain.favoriteQueriesFileLocation = selectedFile.getAbsolutePath();
			String messageFileSaved = selectedFile.toString()
					+ " has been loaded.\n\n Would you like to apply it now?";
			int answer = JOptionPane.showConfirmDialog(getDialogParent(), messageFileSaved, "Switch?", JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				selectFavoriteQueries();
			}
		}
	}

    /**
	 * Reads lines from a file into an ArrayList, skipping lines starting with the
	 * comment character.
	 * 
	 * @param filename    The file to read.
	 * @param commentChar The character indicating a comment line.
	 * @return ArrayList of lines from the file.
	 * @throws IOException If an I/O error occurs.
	 */
	public ArrayList<String> getStringArrayFromFile(String filename, String commentChar) throws IOException {
		ArrayList<String> arrayList = new ArrayList<String>();
		if (filename == null || filename.trim().isEmpty()) {
			throw new IOException("No filename provided.");
		}

		BufferedReader br = new BufferedReader(new FileReader(filename));
		for (String line; (line = br.readLine()) != null;) {
			line = line.trim();
			if (line.length() > 0) {
				if (commentChar != null && !line.startsWith(commentChar)) {
					arrayList.add(line);
				}
			}
		}
		br.close();

		return arrayList;
	}

    /**
	 * Gets the full tree path for each line in the favorite query list.
	 * 
	 * @param myTree The JTree.
	 * @param myLine The line from the favorite query list.
	 * @return The TreePath corresponding to the line, or null if not found.
	 */
	private TreePath getTreePathForEachLine(JTree myTree, String myLine) {
		if (myTree == null || myTree.getModel() == null || myLine == null || myLine.trim().isEmpty()) {
			return null;
		}
		TreePath theFullPath = null;
		String[] splitLine = myLine.split(">");
		if (splitLine.length < 2) {
			System.out.println("Skipping malformed favorite query path: " + myLine);
			return null;
		}
		String[] splitLineForGroups = Arrays.copyOfRange(splitLine, 0, splitLine.length - 1);
		String child_group_to_find = splitLine[splitLine.length - 2]; // get the last child group before the leaf
		String group_to_find_trim = child_group_to_find.substring(1, child_group_to_find.length() - 1); // remove double
																										// quotes
		int pathCount = splitLine.length - 1;
		ArrayList<TreePath> allPaths = getFullTreePath2(myTree, group_to_find_trim, pathCount);
		if (allPaths.size() == 1) {
			theFullPath = allPaths.get(0);
		} else if ((allPaths.size() > 1)) { // when queryGroup appears multiple times in a tree
			for (int i = 0; i < allPaths.size(); i++) {
				TreePath testPath = allPaths.get(i);
				boolean checkMatchAll = matchsAllGroups(testPath, splitLineForGroups);
				if (checkMatchAll) {
					theFullPath = allPaths.get(i);
					// System.out.println("this is the treePath that matches :" + theFullPath);
				}

			}
		}
		return theFullPath;
	}

    /**
	 * Gets the row number for a leaf under a query group in the tree.
	 * 
	 * @param myTree   The JTree.
	 * @param myPath   The TreePath to the group.
	 * @param leafName The name of the leaf.
	 * @return The row number for the leaf, or -1 if not found.
	 */
	public static int getRowNumberForLeaf(JTree myTree, TreePath myPath, String leafName) {
		int rowNumForLeaf = -1;
		int rowNumForSubgroup = myTree.getRowForPath(myPath);
		QueryGroup myChildGroup = (QueryGroup) myPath.getLastPathComponent();
		ArrayList leaves = myChildGroup.getQueryList();
		for (int m = 0; m < leaves.size(); m++) {
			// if (leafName.contains(leaves.get(m).toString())) {
			if (leafName.replace("\"", "").trim().compareToIgnoreCase(leaves.get(m).toString().trim()) == 0) {
				// System.out.println("found my favorite query name here:" +
				// leaves.get(m).toString());
				int myIndex = ((TreeModel) myTree.getModel()).getIndexOfChild(myChildGroup, leaves.get(m));
				rowNumForLeaf = rowNumForSubgroup + myIndex + 1;
				break;
			}
		} // for loop end
		return rowNumForLeaf;
	} // get RowNumberForLeaf method end

    // this method is to get the full tree path for a query group name
	// considering that the same query group name can appear multiple times in the
    // same tree
	// but at different locations,YD added
	public static ArrayList<TreePath> getFullTreePath2(JTree tree, String groupName, int pathCount) {
		Enumeration<TreePath> allPath = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
		ArrayList<TreePath> myTreePath = new ArrayList<TreePath>();
		if (allPath != null) {
			while (allPath.hasMoreElements()) {
				TreePath treePath = (TreePath) allPath.nextElement();
				String treePathStr = treePath.toString();
				String[] splitsTreePath = treePathStr.replace("[", "").replace("]", "").split(",");
				String currentGroup = splitsTreePath[splitsTreePath.length - 1];
				int checkPathCount = treePath.getPathCount();
				// handle when "," within groupName such as 'Markets, prices, and costs' first
				boolean groupNameHasComma = groupName.contains(",");
				if (groupNameHasComma && treePath.toString().contains(groupName)) {
					myTreePath.add(treePath);
				} else if (checkPathCount == pathCount & currentGroup.trim().equals(groupName)) {
					System.out.println("found for Sankey diagrams group:" + treePathStr);
					myTreePath.add(treePath);
				}
			}
		}
		return (myTreePath);
	}

    /**
	 * Checks if all elements of a string array are present in the TreePath.
	 * 
	 * @param myPath  The TreePath.
	 * @param allStrs Array of group names.
	 * @return True if all groups match, false otherwise.
	 */
	public boolean matchsAllGroups(TreePath myPath, String[] allStrs) {
		boolean matchsAll = true;
		for (int i = 0; i < allStrs.length; i++) {
			// need to remove the double quotes for each group
			String groupName = allStrs[i];
			String groupQuoteRemoved = groupName.substring(1, groupName.length() - 1);
			String myPathStr = (String) myPath.getPathComponent(i).toString();
			if (!myPathStr.equals(groupQuoteRemoved)) {
				matchsAll = false;
				break;
			}
		}
		return matchsAll;
	}

    // YD added this method to convert the path string into each line with
	// ">",Feb-2024
	public static String convertPathToLine(String pathStr) {
		return Arrays.stream(pathStr.trim().split("\\s*,\\s*")).map(s -> s.isEmpty() ? s : '"' + s + '"')
				.collect(Collectors.joining(">"));
	}

	// YD added this method to count number of commas in a TreePath,Mar-2024
	public int countCommaInPath(String pathStr) {
		int numCommas = pathStr.length() - pathStr.replace(",", "").length();
		return (numCommas);
	}

	// YD added this method to count number of commas in a TreePath,Mar-2024
	public String convertPathWithCommaToLine(TreePath treePathNow) {
		int treePathCount = treePathNow.getPathCount();
		String myStr = "";
		for (int i = 0; i < treePathCount - 1; i++) {
			QueryGroup queryGroupNow = (QueryGroup) treePathNow.getPathComponent(i);
			String strNow = "\"" + queryGroupNow + "\"" + ">";
			myStr = myStr + strNow;
		}
		Object queryName = treePathNow.getPathComponent(treePathCount - 1);
		String lastPart = "\"" + queryName.toString() + "\"";
		String myLine = myStr + lastPart;
		return (myLine);
	}

	private void expandAllRows(JTree tree) {
		if (tree == null) {
			return;
		}
		for (int row = 0; row < tree.getRowCount(); row++) {
			tree.expandRow(row);
		}
	}
}
