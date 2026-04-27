/*
* LEGAL NOTICE
* This computer software was prepared by US EPA.
* THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
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
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
* 
*/
package glimpseUtil;

import gui.Client;
import gui.DiffLineRow;
import gui.ScenarioLibraryReportHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.StatusBar;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Utility class for common operations in GLIMPSE ScenarioBuilder.
 * <p>
 * Provides methods for string manipulation, GUI component creation, file operations,
 * error reporting, and transportation vehicle info processing. Many methods are
 * designed to support the application's data handling and user interface needs.
 * </p>
 */
public class GLIMPSEUtils {

    /**
     * Singleton instance of GLIMPSEUtils.
     */
    public static final GLIMPSEUtils instance = new GLIMPSEUtils();

    public GLIMPSEVariables vars;
    public GLIMPSEStyles styles;
    public GLIMPSEFiles files;
    public StatusBar sb;
    private final UtilsStrings stringUtils = new UtilsStrings();
    private UtilsTransport transportUtils;
    private UtilsStatus statusUtils;
    private UtilsDiff diffUtils;
    private UtilsUI uiUtils;

    // Constants for label texts, combo box options, and other hardcoded strings
    public static final String[] STATE_CODES = { "AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "HI",
			"IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE",
			"NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VA", "VT", "WA",
			"WI", "WV", "WY", "AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "HI", "IA", "ID", "IL",
			"IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM",
			"NV", "NY", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VA", "VT", "WA", "WI", "WV",
			"WY" };
    public static final String DATE_FORMAT_STR = "yyyy MMM dd HH:mm";
    public static final String LABEL_WARNING = "Warning";
    public static final String LABEL_CONFIRMATION_DIALOG = "Confirmation Dialog";
    public static final String LABEL_DELETE_SELECTED_ITEMS = "Delete selected items?";
    public static final String LABEL_PLEASE_CONFIRM_DELETION = "Please confirm deletion.";
    public static final String LABEL_CLOSE = "Close";
    public static final String LABEL_OK = "OK";
    public static final String LABEL_DISPLAY = "Display";
    public static final String LABEL_INFORMATION = "Information";
    public static final String LABEL_ARCHIVE_SCENARIO = "Archive selected scenario(s) by copying all files to scenario folder(s)?";
    public static final String LABEL_PLEASE_CONFIRM_ARCHIVE = "Please confirm archive.";
    public static final String LABEL_NOTICE = "Notice";
    public static final String LABEL_CANNOT_BE_EXECUTED = "Cannot be executed.";
    public static final String LABEL_RESOURCES = "  Resources... ";

    public String[] states = STATE_CODES;
    public String dateFormatStr = DATE_FORMAT_STR;

    // Specifies style for GUI tables, such as border width and color

    public GLIMPSEUtils() {
    }

    /**
	 * Returns the singleton instance of GLIMPSEUtils.
	 * 
	 * @return GLIMPSEUtils instance
	 */
	public static GLIMPSEUtils getInstance() {
		return instance;
	}

	/**
	 * Initializes utility class with references to variables, styles, and files.
	 * 
	 * @param u GLIMPSEUtils instance (not used)
	 * @param v GLIMPSEVariables instance
	 * @param s GLIMPSEStyles instance
	 * @param f GLIMPSEFiles instance
	 */
	public void init(GLIMPSEUtils u, GLIMPSEVariables v, GLIMPSEStyles s, GLIMPSEFiles f) {
		vars = v;
		styles = s;
		files = f;
		stringUtils.vars = v;
		transportUtils = new UtilsTransport(v, f, this);
		statusUtils = new UtilsStatus();
		statusUtils.init(v, f);
		diffUtils = new UtilsDiff();
		diffUtils.init(f, diff -> displayArrayList(diff, "Differences"));
		uiUtils = new UtilsUI();
		uiUtils.init(v, s);
		UtilsDialogs.getInstance().init(s);
		UtilsErrors.getInstance().init(v, s, f, this);
	}


	/**
	 * Checks if a string matches any item in the provided list.
	 *
	 * @param str        String to check
	 * @param marketList List of strings
	 * @return true if match found, false otherwise
	 */
	public boolean getMatch(String str, List<String> marketList) {
		return UtilsStrings.getMatch(str, marketList);
	}

	/**
	 * Adds a string to the list if it is not already present.
	 *
	 * @param list List of strings
	 * @param str  String to add
	 * @return Updated list
	 */
	public ArrayList<String> addToArrayListIfUnique(ArrayList<String> list, String str) {
		return UtilsStrings.addToArrayListIfUnique(list, str);
	}

	/**
	 * Returns the first token containing the specified text from a delimited line.
	 *
	 * @param line  Input string
	 * @param txt   Text to search for
	 * @param delim Delimiter
	 * @return Token containing the text, or empty string if not found
	 */
	public String getTokenWithText(String line, String txt, String delim) {
		return UtilsStrings.getTokenWithText(line, txt, delim);
	}

	/**
	 * Concatenates elements of an ObservableList<String> into a single string
	 * separated by the given separator.
	 *
	 * @param ol        ObservableList<String>
	 * @param separator Separator string
	 * @return Concatenated string
	 */
	public String getStringFromList(ObservableList<String> ol, String separator) {
		return UtilsStrings.getStringFromList(ol, separator);
	}

	/**
	 * Returns the current date as a string in yyyy-MM-dd format.
	 *
	 * @return Current date string
	 */
	public String getCurrentTimeStamp() {
		return UtilsDateNumber.getCurrentTimeStamp();
	}

	/**
	 * Parses a date string using the class dateFormatStr.
	 *
	 * @param dateStr Date string
	 * @return Date object, or null if parsing fails
	 */
	public Date getFormattedDate(String dateStr) {
		return UtilsDateNumber.getFormattedDate(dateStr, dateFormatStr);
	}


	/**
	 * Shows a warning message dialog on the JavaFX application thread.
	 *
	 * @param msg Message to display
	 */
	public void warningMessage(String msg) {
		UtilsDialogs.getInstance().warningMessage(msg);
    }

    /**
     * Enables modal dialogs and flushes any queued warning messages.
     * Call this after the primary stage is shown.
     */
    public void setModalDialogsReadyAndFlushWarnings() {
    	UtilsDialogs.getInstance().setModalDialogsReadyAndFlushWarnings();
    }

	/**
	 * Displays a dialog for text input and returns the entered text.
	 *
	 * @param descriptionType Dialog title
	 * @return Entered text
	 */
	public String getTextDialog(String descriptionType) {
		return UtilsDialogs.getInstance().getTextDialog(descriptionType);
	}

	/**
	 * Converts a string to an integer, returns 0 if conversion fails.
	 *
	 * @param s String to convert
	 * @return Integer value
	 */
	public int convertStringToInt(String s) {
		return UtilsDateNumber.convertStringToInt(s);
	}

	/**
	 * Returns the year string for a given period index.
	 *
	 * @param period Period index
	 * @return Year as string
	 */
	public String getYearForPeriod(int period) {
		return UtilsDateNumber.getYearForPeriod(period);
	}

	/**
	 * Returns the period index for a given year string.
	 *
	 * @param year Year as string
	 * @return Period index as string
	 */
	public String getPeriodForYear(String year) {
		return UtilsDateNumber.getPeriodForYear(year);
	}

	/**
	 * Removes trailing commas from a string.
	 *
	 * @param s Input string
	 * @return String without trailing commas
	 */
	public String getRidOfTrailingCommasInString(String s) {
		return UtilsStrings.getRidOfTrailingCommasInString(s);
	}

	/**
	 * Removes trailing commas from each string in an array.
	 *
	 * @param s Array of strings
	 * @return Array with trailing commas removed
	 */
	public String[] getRidOfTrailingCommasInStringArray(String[] s) {
		return UtilsStrings.getRidOfTrailingCommasInStringArray(s);
	}

	/**
	 * Clears the text in a TextArea.
	 *
	 * @param ta TextArea to clear
	 */
	public void clearTextArea(TextArea ta) {
		if (ta == null)
			return;
		ta.setText(null);
	}

	/**
	 * Capitalizes only the first letter of a string.
	 *
	 * @param input_string Input string
	 * @return String with only the first letter capitalized
	 */
	public String capitalizeOnlyFirstLetterOfString(String input_string) {
		return UtilsStrings.capitalizeOnlyFirstLetterOfString(input_string);
	}

	/**
	 * Shows a confirmation dialog for deletion.
	 *
	 * @return true if user confirms, false otherwise
	 */
	public boolean confirmDelete() {
		return UtilsDialogs.getInstance().confirmDelete();
	}

	/**
	 * Exits the application if Client.exit_on_exception is true.
	 */
	public void exitOnException() {
		if (Client.exit_on_exception == true) {
			System.exit(0);
		}
	}

	/**
	 * Splits a string by the given delimiter.
	 *
	 * @param str   Input string
	 * @param delim Delimiter
	 * @return Array of split strings
	 */
	public String[] splitString(String str, String delim) {
		return UtilsStrings.splitString(str, delim);
	}



	/**
	 * Creates an ArrayList from a delimited string.
	 *
	 * @param line  Input string
	 * @param delim Delimiter
	 * @return ArrayList of strings
	 */
	public ArrayList<String> createArrayListFromString(String line, String delim) {
		return UtilsStrings.createArrayListFromString(line, delim);
	}

	public String[] splitEOL(String line) {
		return stringUtils.splitEOL(line);
	}

	public ArrayList<String> createArrayListFromString(String line) {
		return stringUtils.createArrayListFromString(line);
	}

	public String[] createStringArrayFromObservableList(ObservableList<String> arrayStr) {
		return stringUtils.createStringArrayFromObservableList(arrayStr);
	}

	public String[] createStringArrayFromArrayList(ArrayList<String> arrayList) {
		return stringUtils.createStringArrayFromArrayList(arrayList);
	}

	public String[] createStringArrayFromListOfIntegers(List<Integer> list) {
		return stringUtils.createStringArrayFromListOfIntegers(list);
	}

	public ArrayList<String> getStringListFromString(String str, String delim) {
		return stringUtils.getStringListFromString(str, delim);
	}

	public String getKeyValue(ArrayList<String[]> keyValuePairs, String key) {
		return stringUtils.getKeyValue(keyValuePairs, key);
	}

//	public String getMatch(ArrayList<String> list, String item, String delimiter) {
//		return stringUtils.getMatch(list, item, delimiter);
//	}
//
//	public String getMatch(ArrayList<String> list, String item, String delimiter1, String delimiter2) {
//		return stringUtils.getMatch(list, item, delimiter1, delimiter2);
//	}
//
//	public String[] getMatches(ArrayList<String> list, String item, String delimiter1, String delimiter2, String delimiter3) {
//		return stringUtils.getMatches(list, item, delimiter1, delimiter2, delimiter3);
//	}

	public int getMaxValFromStringArray(String[] strArray) {
		return stringUtils.getMaxValFromStringArray(strArray);
	}

	public int getMinValFromStringArray(String[] strArray) {
		return stringUtils.getMinValFromStringArray(strArray);
	}

	public String getStringUpToChar(String str, String ch) {
		return stringUtils.getStringUpToChar(str, ch);
	}

	public String getStringBetweenCharSequences(String str, String startSequence, String endSequence) {
		return stringUtils.getStringBetweenCharSequences(str, startSequence, endSequence);
	}

	/**
	 * Concatenates an ArrayList of strings into a single string with EOL separators.
	 *
	 * @param arrayList ArrayList of strings
	 * @return Concatenated string
	 */
	public String createStringFromArrayList(ArrayList<String> arrayList) {
		return UtilsStrings.createStringFromArrayList(arrayList, vars.getEol());
	}

	/**
	 * Concatenates an ArrayList of strings into a single string with a custom delimiter.
	 *
	 * @param filesToSave ArrayList of strings
	 * @param delimiter   Delimiter string
	 * @return Concatenated string
	 */
	public String createStringFromArrayList(List<String> filesToSave, String delimiter) {
		return UtilsStrings.createStringFromArrayList(filesToSave, delimiter);
	}



	/**
	 * Concatenates a string array into a single comma-separated string.
	 *
	 * @param str_array Array of strings
	 * @return Concatenated string
	 */
	public String createStringFromStringArray(String[] str_array) {
		return UtilsStrings.createStringFromStringArray(str_array);
	}

	public boolean hasSpecialCharacter(String str) {
		return UtilsStrings.hasSpecialCharacter(str);
	}

	public String getParentheticString(String s) {
		return UtilsStrings.getParentheticString(s);
	}

	public String getTextBetweenParen(String s) {
		return UtilsStrings.getTextBetweenParen(s);
	}

	/**
	 * Creates a JavaFX Separator with specified orientation, length, and visibility.
	 *
	 * @param orientation Orientation of the separator
	 * @param length      Minimum width
	 * @param visible     Visibility flag
	 * @return Separator instance
	 */
	public Separator getSeparator(Orientation orientation, int length, boolean visible) {
		if (uiUtils == null)
			return new UtilsUI().getSeparator(orientation, length, visible);
		return uiUtils.getSeparator(orientation, length, visible);
	}




	/**
	 * Creates a JavaFX Label with the specified text and default style.
	 * 
	 * @param txt Label text
	 * @return Label instance
	 */
	public Label createLabel(String txt) {
		if (uiUtils == null)
			return new UtilsUI().createLabel(txt);
		return uiUtils.createLabel(txt);
	}

	/**
	 * Creates a JavaFX Label with the specified text, width, and default style.
	 * 
	 * @param txt        Label text
	 * @param pref_width Preferred width
	 * @return Label instance
	 */
	public Label createLabel(String txt, double pref_width) {
		if (uiUtils == null)
			return new UtilsUI().createLabel(txt, pref_width);
		return uiUtils.createLabel(txt, pref_width);
	}

	/**
	 * Creates a JavaFX TextField with the specified width.
	 * 
	 * @param wid Width
	 * @return TextField instance
	 */
	public TextField createTextField(double wid) {
		if (uiUtils == null)
			return new UtilsUI().createTextField(wid);
		return uiUtils.createTextField(wid);
	}

	public ComboBox<String> createComboBox() {
		if (uiUtils == null)
			return new UtilsUI().createComboBox();
		return uiUtils.createComboBox();
	}

	public ComboBox<String> createComboBoxString(double wid) {
		if (uiUtils == null)
			return new UtilsUI().createComboBoxString(wid);
		return uiUtils.createComboBoxString(wid);
	}

	public ComboBox<String> createComboBoxString(String[] items) {
		if (uiUtils == null)
			return new UtilsUI().createComboBoxString(items);
		return uiUtils.createComboBoxString(items);
	}

	public CheckComboBox<String> createCheckComboBox(double wid) {
		if (uiUtils == null)
			return new UtilsUI().createCheckComboBox(wid);
		return uiUtils.createCheckComboBox(wid);
	}

	/**
	 * Creates a JavaFX TextField with default style.
	 * 
	 * @return TextField instance
	 */
	public TextField createTextField() {
		if (uiUtils == null)
			return new UtilsUI().createTextField();
		return uiUtils.createTextField();
	}

	/**
	 * Creates a JavaFX ComboBox<String> for strings with default style.
	 * 
	 * @return ComboBox<String> instance
	 */
	public ComboBox<String> createComboBoxString() {
		if (uiUtils == null)
			return new UtilsUI().createComboBoxString();
		return uiUtils.createComboBoxString();
	}

	/**
	 * Creates a ControlsFX CheckComboBox<String> for strings with default style.
	 * 
	 * @return CheckComboBox<String> instance
	 */
	public CheckComboBox<String> createCheckComboBox() {
		if (uiUtils == null)
			return new UtilsUI().createCheckComboBox();
		return uiUtils.createCheckComboBox();
	}

	/**
	 * Creates a JavaFX CheckBox with the specified label and default style.
	 * 
	 * @param s Label text
	 * @return CheckBox instance
	 */
	public CheckBox createCheckBox(String s) {
		if (uiUtils == null)
			return new UtilsUI().createCheckBox(s);
		return uiUtils.createCheckBox(s);
	}

	private Button createButtonInternal(String text, int wid, String tt, String imageName) {
		if (uiUtils == null)
			return new UtilsUI().createButton(text, wid, tt, imageName);
		return uiUtils.createButton(text, wid, tt, imageName);
	}

	/**
	 * Creates a JavaFX Button with the specified text, width, tooltip, and icon
	 * image.
	 * 
	 * @param text      Button text
	 * @param wid       Button width
	 * @param tt        Tooltip text
	 * @param imageName Icon image file name (without extension)
	 * @return Button instance
	 */
	public Button createButton(String text, int wid, String tt, String imageName) {
		return createButtonInternal(text, wid, tt, imageName);
	}

	/**
	 * Creates a JavaFX Button with the specified text and default width.
	 * 
	 * @param text Button text
	 * @return Button instance
	 */
	public Button createButton(String text) {
		return createButtonInternal(text, styles != null ? styles.getBigButtonWidth() : -1, null, null);
	}

	/**
	 * Creates a JavaFX Button with the specified text and tooltip.
	 * 
	 * @param text Button text
	 * @param tt   Tooltip text
	 * @return Button instance
	 */
	public Button createButton(String text, String tt) {
		return createButtonInternal(text, -1, tt, null);
	}

	/**
	 * Creates a JavaFX Button with the specified text, width, and event handler.
	 * 
	 * @param text    Button text
	 * @param width   Button width
	 * @param handler Event handler for button action
	 * @return Button instance
	 */
	public Button createButton(String text, int width, EventHandler<ActionEvent> handler) {
		if (uiUtils == null)
			return new UtilsUI().createButton(text, width, handler);
		return uiUtils.createButton(text, width, handler);
	}

	public Button resizeButtonText(Button button) {
		if (uiUtils == null)
			return new UtilsUI().resizeButtonText(button);
		return uiUtils.resizeButtonText(button);
	}

	public Button resizeButtonText(Button button, String text, double size) {
		if (uiUtils == null)
			return new UtilsUI().resizeButtonText(button, text, size);
		return uiUtils.resizeButtonText(button, text, size);
	}

	public Label resizeLabelText(Label label) {
		if (uiUtils == null)
			return new UtilsUI().resizeLabelText(label);
		return uiUtils.resizeLabelText(label);
	}

	public Label resizeLabelText(Label label, String text, double size) {
		if (uiUtils == null)
			return new UtilsUI().resizeLabelText(label, text, size);
		return uiUtils.resizeLabelText(label, text, size);
	}

	public String returnAppendedString(String[] stringArray) {
		return UtilsStrings.createStringFromStringArray(stringArray);
	}

	public void insertLinesIntoFile(String filename, String lines, int startRow) {
		if (files == null)
			return;
		ArrayList<String> linesList = createArrayListFromString(lines);

		ArrayList<String> arraylist = files.getStringArrayFromFile(filename, "#");

		for (int i = linesList.size() - 1; i > -1; i--) {
			String str = linesList.get(i);
			// Todo: Test. This code to add Eol characters was adding extra spaces between
			// metadata lines. Did commenting it out cause other issues?
			// if (str.indexOf(vars.getEol()) < 0)
			// str += vars.getEol();
			arraylist.add(startRow, str);
		}
		// arraylist.addAll(startRow, linesList);

		files.saveFile(arraylist, filename);
	}

	public ArrayList<String> getUniqueItemsFromStringArrayList(ArrayList<String> list) {
		return stringUtils.getUniqueItemsFromStringArrayList(list);
	}

	public String getUniqueString() {
		return stringUtils.getUniqueString();
	}

	public String commentLinesInString(String stringLine, String startComment, String endComment) {
		return stringUtils.commentLinesInString(stringLine, startComment, endComment);
	}

	public String trimIfExists(String str) {
		return UtilsStrings.trimIfExists(str);
	}

	public String toSignificantFiguresString(double val, int significantFigures) {
		return UtilsDateNumber.toSignificantFiguresString(val, significantFigures);
	}

	public String[] convertTo1990Dollars(String[] vals, String dollarYear) {
		return UtilsSeriesCalculations.convertTo1990Dollars(vals, dollarYear, files);
	}

	public double[][] calculateValues(String calcType, int startYear, int endYear, double initialValue, double growth,
			int periodLength, double factor) {
		return UtilsSeriesCalculations.calculateValues(calcType, startYear, endYear, initialValue, growth, periodLength,
				factor);
	}

	public String[] getAllSelectedRegions(TreeView<String> tree) {
		if (uiUtils == null)
			return new UtilsUI().getAllSelectedRegions(tree);
		return uiUtils.getAllSelectedRegions(tree);
	}

	public String[] removeWorldRegion(String[] sOrig) {
		return UtilsStrings.removeWorldRegion(sOrig);
	}

	public String[] removeUSADuplicate(String[] sOrig) {
		return UtilsStrings.removeUSADuplicate(sOrig);
	}

	public ArrayList<CheckBoxTreeItem<String>> returnAllSelectedLeaves(TreeItem<String> rootNode) {
		if (uiUtils == null)
			return new UtilsUI().returnAllSelectedLeaves(rootNode);
		return uiUtils.returnAllSelectedLeaves(rootNode);
	}

	public boolean getAllChildren(TreeItem<String> node, ArrayList<TreeItem<String>> list) {
		if (uiUtils == null)
			return new UtilsUI().getAllChildren(node, list);
		return uiUtils.getAllChildren(node, list);
	}

	public boolean confirmAction(String s) {
		return UtilsDialogs.getInstance().confirmAction(s);
	}

	public boolean showInformationDialog(String title, String header, String content) {
		return UtilsDialogs.getInstance().showInformationDialog(title, header, content);
	}

	public boolean showInformationDialog(String title, String header, String content, double widthScale) {
		return UtilsDialogs.getInstance().showInformationDialog(title, header, content, widthScale);
	}

	public boolean confirmArchiveScenario() {
		if (vars == null)
			return false;
		return UtilsDialogs.getInstance().confirmArchiveScenario();
	}
 
	public boolean showStatusDialog(String title, String header, String content) {
		return UtilsDialogs.getInstance().showStatusDialog(title, header, content);
	}

	public boolean selectYesOrNoDialog(String s) {
		return UtilsDialogs.getInstance().selectYesOrNoDialog(s);
	}

	public void resetLogFile(String filename) {
		if (files == null || filename == null)
			return;
		ArrayList<String> empty = new ArrayList<>();
		files.saveFile(empty, filename);
	}
 
	public boolean diffTwoFiles(String file1, String file2) {
		if (diffUtils == null)
			return false;
		return diffUtils.diffTwoFiles(file1, file2);
	}

	public List<DiffLineRow> generateSideBySideDiffRows(String file1, String file2) {
		if (diffUtils == null)
			return new ArrayList<>();
		return diffUtils.generateSideBySideDiffRows(file1, file2);
	}

	public void displayString(String str, String title) {
		ArrayList<String> str_array = createArrayListFromString(str);
		displayArrayList(str_array, title);
	}

	public void printArrayListToStdout(ArrayList<String> arrayListArg) {
		if (arrayListArg == null)
			return;
		for (String str : arrayListArg) {
			System.out.println("i: " + str + " - " + (str != null ? str.split(":").length : 0));
		}
	}

	public void displayArrayList(ArrayList<String> arrayListArg, String title) {
		UtilsErrors.getInstance().displayArrayList(arrayListArg, title);
	}

	public void displayArrayList(ArrayList<String> arrayListArg, String title, boolean doWrap) {
		UtilsErrors.getInstance().displayArrayList(arrayListArg, title, doWrap);
	}

	// ====================== Some table code for generating a popup to show CSV
	// tables ========================
	// from:
	// https://stackoverflow.com/questions/44956205/javafx-tableview-with-different-cell-types-and-unknown-size

	public String[][] getDataMatrixFromArrayList(ArrayList<String> data) {
		return UtilsTable.getDataMatrixFromArrayList(data);
	}

	public void showPopupTableOfCSVData(String title, ArrayList<String> csvData, int wd, int ht) {
		UtilsTable.showPopupTableOfCSVData(title, csvData, wd, ht, this, vars, styles, files);
	}

	public void showPopupTableOfErrorReport(String title, ArrayList<String> csvData, int wd, int ht) {
		UtilsErrors.getInstance().showPopupTableOfErrorReport(title, csvData, wd, ht);
	}

	public void showTextErrorReport(ScenarioLibraryReportHelper.ErrorTextReport report, int wd, int ht) {
		UtilsErrors.getInstance().showTextErrorReport(report, wd, ht);
	}

	public double getConversionFactor(String fromYear, String toYear) {
		double d = 1.0;

		if ("1990$s".equals(toYear)) {
			if ("2023$s".equals(fromYear)) {
				d = 0.49;
			} else if ("2020$s".equals(fromYear)) {
				d = 0.56;
			} else if ("2015$s".equals(fromYear)) {
				d = 0.61;
					} else if ("2010$s".equals(fromYear)) {
				d = 0.66;
			} else if ("2005$s".equals(fromYear)) {
				d = 0.73;
			} else if ("2000$s".equals(fromYear)) {
				d = 0.82;
			}
		} else { // 1975$s
			if ("2023$s".equals(fromYear)) {
				d = 0.23;
			} else if ("2020$s".equals(fromYear)) {
				d = 0.26;
			} else if ("2015$s".equals(fromYear)) {
				d = 0.29;
			} else if ("2010$s".equals(fromYear)) {
				d = 0.31;
			} else if ("2005$s".equals(fromYear)) {
				d = 0.34;
			} else if ("2000$s".equals(fromYear)) {
				d = 0.38;
			}
		}

		return d;
	}



	/**
	 * Retrieves the load factor for a transportation technology in a specific
	 * region, sector, and year.
	 */
	public String getLoadFactor(String region, String sector, String subsector, String tech, String year) {
		if (transportUtils == null)
			return null;
		return transportUtils.getLoadFactor(region, sector, subsector, tech, year);
	}

	/**
	 * Retrieves the vehicle coefficient for a transportation technology in a
	 * specific region, sector, and year.
	 */
	public String getVehCoefficient(String region, String sector, String subsector, String tech, String year) {
		if (transportUtils == null)
			return null;
		return transportUtils.getVehCoefficient(region, sector, subsector, tech, year);
	}

	/**
	 * Retrieves the technology names for a given subsector in a region.
	 */
	public String[] getTrnTechsInSubsector(String region, String sector, String subsector) {
		if (transportUtils == null)
			return null;
		return transportUtils.getTrnTechsInSubsector(region, sector, subsector);
	}

	/**
	 * Retrieves transportation vehicle information for a given parameter, region,
	 * sector, subsector, technology, and year.
	 */
	public String getTrnVehInfo(String param, String region, String sector, String subsector, String tech,
			String year_str) {
		if (transportUtils == null)
			return null;
		return transportUtils.getTrnVehInfo(param, region, sector, subsector, tech, year_str);
	}

	/**
	 * Loads transportation vehicle information from a file and categorizes it into
	 * different tables based on vehicle type.
	 */
	public void loadTrnVehInfo() {
		if (transportUtils == null)
			return;
		transportUtils.loadTrnVehInfo();
	}

	
	public String getSubsectorConversions(double numf, String region, String sector, String subsector, int year) {

		String val = null;
		double num = 0.0;

	 val = numf + ",1";

		String load = "1";
		if (sector.startsWith("trn"))
			load = getLoadFactor(region, sector, subsector, "any", Integer.toString(year));

		if (load != null) {

			try {
				double valf = Double.parseDouble(load);
				if (sector.startsWith("trn")) {
					boolean useMMBTUConversions = true;
					if (vars != null) {
						useMMBTUConversions = vars.getUseTrnMMBTUConversions();
					}

					if (useMMBTUConversions) {
						num = numf * (1e-6) / valf * 1.055;
						val = "," + num + ",1.0e6";
					} else {
						num = numf * 1.0e-9 / valf * 1000.0;
						val = "," + num + ",1.0e9";
					}
				}
						} catch (Exception e) {
				;
			}

		}

		if ((sector.indexOf("trn_") >= 0) && (load == null))
			val = null;

//		}
		return val;
	}

	/**
	 * Checks if a subsector is present in the specified region and sector.
	 * 
	 * @param region    Region name
	 * @param sector    Sector name
	 * @param subsector Subsector name
	 * @return true if subsector is in region, false otherwise
	 */
	public boolean isSubsectorInRegion(String region, String sector, String subsector) {
		if (transportUtils == null)
			return false;
		return transportUtils.isSubsectorInRegion(region, sector, subsector);
	}


	public boolean isState(String name) {
		boolean return_val = false;
		if (states == null)
			states = STATE_CODES;
		for (String state : states) {
			if (name.equals(state)) {
				return_val = true;
				break;
			}
		}
		return return_val;
	}

	/**
	 * Generates a detailed error report from the main log file as CSV rows.
	 *
	 * @param main_log_file Path to main log file
	 * @param scenario      Scenario name
	 * @return List of CSV rows (no header)
	 */
	public ArrayList<String> generateErrorReport(String main_log_file, String scenario) {
		return UtilsErrors.getInstance().generateErrorReport(main_log_file, scenario);
	}

	/**
	 * Builds a table-ready CSV list for error reports (adds a header and pads rows).
	 *
	 * @param rows CSV rows from generateErrorReport
	 * @return CSV with header and uniform column count
	 */
	public ArrayList<String> buildErrorReportTable(ArrayList<String> rows) {
		return UtilsErrors.getInstance().buildErrorReportTable(rows);
	}

	public String processErrors(ArrayList<String> errors, double min_red) {
		return UtilsErrors.getInstance().processErrors(errors, min_red);
	}

	public String getRunningScenario(File mainLogFile) {
		if (statusUtils == null)
			return "";
		return statusUtils.getRunningScenario(mainLogFile);
	}

	public String getScenarioStatusFromMainLog(File mainLogFile) {
		if (statusUtils == null)
			return "";
		return statusUtils.getScenarioStatusFromMainLog(mainLogFile);
	}

	public String getLatestRunningPeriod(File mainLogFile) {
		if (statusUtils == null)
			return "";
		return statusUtils.getLatestRunningPeriod(mainLogFile);
	}

	public void fixLostHandle() {
		if (vars == null || files == null)
			return;
		String exeDir = vars.getgCamExecutableDir();
		if (exeDir == null || exeDir.trim().isEmpty())
			return;
		File exeMainLog = new File(exeDir + File.separator + "logs" + File.separator + "main_log.txt");
		if (!exeMainLog.exists()) {
			showInformationDialog("Notice", "Fix Lost Handle", "Executable main_log.txt not found.");
			return;
		}
		String scenarioName = getRunningScenario(exeMainLog);
		if (scenarioName == null || scenarioName.trim().isEmpty()) {
			showInformationDialog("Notice", "Fix Lost Handle", "No running scenario detected in main_log.txt.");
			return;
		}
		Path scenarioDir = Paths.get(vars.getScenarioDir(), scenarioName);
		if (!Files.exists(scenarioDir)) {
			showInformationDialog("Notice", "Fix Lost Handle", "Scenario folder not found: " + scenarioDir.toString());
			return;
		}
		Path dest = scenarioDir.resolve("main_log.txt");
		try {
			Files.move(exeMainLog.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
			showInformationDialog("Notice", "Fix Lost Handle", "Moved executable main_log.txt to scenario: " + scenarioName);
		} catch (Exception e) {
			warningMessage("Problem moving main_log.txt: " + e.getMessage());
		}
	}

	public String getComputerStatString() {
		if (statusUtils == null)
			return "";
		return statusUtils.getComputerStatString();
	}

	public String getMatch(String str, String item, String delimiter1, String delimiter2) {
		String rtn_str = "";
		item = item.trim();
		
		try {

			String[] s = str.split(delimiter1);
			for (String temp : s) {
				String left = temp.split(delimiter2)[0].trim();
				String right = temp.split(delimiter2)[1].trim();
				if (left.equals(item)) {
					rtn_str = right;
					break;
				}
			}
		
		} catch (Exception e) {
			System.out.println("Error in getMatch: " + e);
		}
		return rtn_str;
	}
	
	public String[] getMatches(String str, String item, String delimiter1, String delimiter2, String delimiter3) {
		String[] rtn_str = null;
		item = item.trim();
		//for (String str : list) {
			String[] s1 = str.split(delimiter1);
			for (String temp : s1) {
				String[] s2 = temp.split(delimiter2);
				if (s2[0].trim().equals(item)) {
					rtn_str = s2[1].split(delimiter3);
					for (int i = 0; i < rtn_str.length; i++) {
						rtn_str[i] = rtn_str[i].trim();
					}
					break;
				}
			}
		//}
		return rtn_str;
		
		
		
	}
	
	public String getMatch(String str, String item, String delimiter) {
		String rtn_str = "";
		item = item.trim();
		String temp = "";

			String[] s = str.split(delimiter);
			temp = s[0].trim();
			if (temp.equals(item)) {
				rtn_str = s[1].trim();
			}
		
		return rtn_str;
	}
	
	public String getMatch(ArrayList<String> list, String item, String delimiter) {
		String rtn_str = "";
		item = item.trim();
		String temp = "";
		for (String str : list) {
			String[] s = str.split(delimiter);
			temp = s[0].trim();
			if (temp.equals(item)) {
				rtn_str = s[1].trim();
				break;
			}
		}
		return rtn_str;
	}
	
}
