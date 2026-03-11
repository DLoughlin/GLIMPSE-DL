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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import gui.Client;
import gui.DiffLineRow;

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
    private UtilsTransport transportUtils;

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

    private long orig_date = 0;

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
		transportUtils = new UtilsTransport(v, f, this);
		UtilsDialogs.getInstance().init(s);
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
	 * Splits a string by end-of-line characters.
	 *
	 * @param line Input string
	 * @return Array of lines
	 */
	public String[] splitEOL(String line) {
		if (line == null) {
			line = "";
		}
		String[] lines = line.split(vars.getEol());
		if (lines.length == 1)
			lines = line.split("\r");
		if (lines.length == 1)
			lines = line.split("\n");
		if (lines.length == 1)
			lines = line.split("\r\n");
		return lines;
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

	/**
	 * Creates an ArrayList from a string split by end-of-line.
	 *
	 * @param line Input string
	 * @return ArrayList of strings
	 */
	public ArrayList<String> createArrayListFromString(String line) {
		if (line == null)
			return new ArrayList<>();
		ArrayList<String> linesList = new ArrayList<>();
		String[] lines = splitEOL(line);
		for (String l : lines) {
			linesList.add(l);
		}
		return linesList;
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
	 * Converts an ObservableList<String> of strings to a string array, appending EOL to each.
	 *
	 * @param array_str ObservableList<String> of strings
	 * @return Array of strings
	 */
	public String[] createStringArrayFromObservableList(ObservableList<String> array_str) {
		if (array_str == null || vars == null)
			return new String[0];
		String[] rtn_str = new String[array_str.size()];
		int i = 0;
		for (String s : array_str) {
			rtn_str[i++] = s + vars.getEol();
		}
		return rtn_str;
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

	/**
	 * Converts an ArrayList of strings to a string array, appending EOL to each.
	 *
	 * @param arrayList ArrayList of strings
	 * @return Array of strings
	 */
	public String[] createStringArrayFromArrayList(ArrayList<String> arrayList) {
		if (arrayList == null || vars == null)
			return new String[0];
		String[] result = new String[arrayList.size()];
		int i = 0;
		for (String s : arrayList) {
			result[i++] = s + vars.getEol();
		}
		return result;
	}

	public String[] createStringArrayFromListOfIntegers(List<Integer> list) {
		if (list == null)
			return new String[0];
		String[] result = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			Integer val = list.get(i);
			result[i] = val == null ? "" : String.valueOf(val);
		}
		return result;
	}

	public ArrayList<String> getStringListFromString(String str, String delim) {
		ArrayList<String> list = new ArrayList<>();
		if (str == null || delim == null)
			return list;
		String[] parts = str.split(delim);
		for (String part : parts) {
			if (part == null)
				continue;
			String trimmed = part.trim();
			if (!trimmed.isEmpty())
				list.add(trimmed);
		}
		return list;
	}

	public boolean hasSpecialCharacter(String str) {
		if (str == null)
			return false;
		return !str.matches("[A-Za-z0-9_.-]+");
	}

	public String getParentheticString(String s) {
		return getTextBetweenParen(s);
	}

	public String getTextBetweenParen(String s) {
		if (s == null)
			return "";
		int start = s.indexOf('(');
		int end = s.indexOf(')', start + 1);
		if (start < 0 || end < 0 || end <= start)
			return "";
		return s.substring(start + 1, end);
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

		Separator separator = new Separator(orientation);
		separator.setMinWidth(length);
		separator.setVisible(visible);

		return separator;
	}

	/**
	 * Retrieves the value for a given key from a list of key-value pairs.
	 *
	 * @param keyValuePairs List of key-value string arrays
	 * @param key           Key to search for
	 * @return Value string, or null if not found
	 */
	public String getKeyValue(ArrayList<String[]> keyValuePairs, String key) {
		String value = null;
		key = key.trim().toLowerCase();
		for (String[] s : keyValuePairs) {
			if (s[0].trim().toLowerCase().equals(key)) {
				value = s[1];
			}
		}
		if (value != null)
			value = value.trim();
		return value;
	}

	/**
	 * Finds a match for an item in a list of delimited strings and returns the
	 * associated value.
	 * 
	 * @param list      List of delimited strings
	 * @param item      Item to match
	 * @param delimiter Delimiter
	 * @return Associated value, or empty string if not found
	 */
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

	/**
	 * Finds a match for an item in a list of delimited strings and returns the
	 * associated value.
	 * 
	 * @param list      List of delimited strings
	 * @param item      Item to match
	 * @param delimiter Delimiter
	 * @return Associated value, or empty string if not found
	 */
	public String getMatch(ArrayList<String> list, String item, String delimiter1, String delimiter2) {
		String rtn_str = "";
		item = item.trim();

		try {
		for (String str : list) {
			String[] s = str.split(delimiter1);
			for (String temp : s) {
				String left = temp.split(delimiter2)[0].trim();
				String right = temp.split(delimiter2)[1].trim();
				if (left.equals(item)) {
					rtn_str = right;
					break;
				}
			}
		}
		} catch (Exception e) {
			System.out.println("Error in getMatch: " + e);
		}
		return rtn_str;
	}

	/**
	 * Finds matches for an item in a list of delimited strings and returns
	 * associated values as an array.
	 * 
	 * @param list       List of delimited strings
	 * @param item       Item to match
	 * @param delimiter1 First delimiter
	 * @param delimiter2 Second delimiter
	 * @return Array of associated values
	 */
	public String[] getMatches(ArrayList<String> list, String item, String delimiter1, String delimiter2, String delimiter3) {
		String[] rtn_str = null;
		item = item.trim();
		for (String str : list) {
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
		}
		return rtn_str;
	}

	/**
	 * Creates a JavaFX Label with the specified text and default style.
	 * 
	 * @param txt Label text
	 * @return Label instance
	 */
	public Label createLabel(String txt) {
		if (styles == null)
			return new Label(txt); // null check for styles
		Label label = new Label(txt);
		// Use centralized micro padding for labels
		label.setPadding(styles.getMicroPadding());
		return label;
	}

	/**
	 * Creates a JavaFX Label with the specified text, width, and default style.
	 * 
	 * @param txt        Label text
	 * @param pref_width Preferred width
	 * @return Label instance
	 */
	public Label createLabel(String txt, double pref_width) {
		Label label = createLabel(txt);
		label.setPrefWidth(pref_width);
		label.setMaxWidth(pref_width);
		label.setMinWidth(pref_width);
		if (styles == null)
			return label; // null check for styles
		label = resizeLabelText(label);
		return label;
	}

	/**
	 * Creates a JavaFX TextField with the specified width.
	 * 
	 * @param wid Width
	 * @return TextField instance
	 */
	public TextField createTextField(double wid) {
		TextField tf = new TextField();
		tf.setPrefWidth(wid);
		tf.setMinWidth(wid);
		tf.setMaxWidth(wid);
		return tf;
	}

	public ComboBox<String> createComboBox() {
		return createComboBoxString();
	}

	public ComboBox<String> createComboBoxString(double wid) {
		ComboBox<String> comboBox = new ComboBox<>();
		comboBox.setPrefWidth(wid);
		comboBox.setMinWidth(wid);
		comboBox.setMaxWidth(wid);
		return comboBox;
	}

	public ComboBox<String> createComboBoxString(String[] items) {
		ComboBox<String> comboBox = createComboBoxString();
		if (items != null) {
			for (String item : items) {
				comboBox.getItems().add(item);
			}
		}
		return comboBox;
	}

	public CheckComboBox<String> createCheckComboBox(double wid) {
		CheckComboBox<String> checkComboBox = new CheckComboBox<>();
		checkComboBox.setPrefWidth(wid);
		checkComboBox.setMinWidth(wid);
		checkComboBox.setMaxWidth(wid);
		return checkComboBox;
	}

	/**
	 * Creates a JavaFX TextField with default style.
	 * 
	 * @return TextField instance
	 */
	public TextField createTextField() {
		TextField tf = new TextField();
		return tf;
	}

	/**
	 * Creates a JavaFX ComboBox<String> for strings with default style.
	 * 
	 * @return ComboBox<String> instance
	 */
	public ComboBox<String> createComboBoxString() {
		ComboBox<String> comboBox = new ComboBox<>();
		return comboBox;
	}

	/**
	 * Creates a ControlsFX CheckComboBox<String> for strings with default style.
	 * 
	 * @return CheckComboBox<String> instance
	 */
	public CheckComboBox<String> createCheckComboBox() {
		CheckComboBox<String> checkComboBox = new CheckComboBox<>();
		checkComboBox.setPrefWidth(Double.MAX_VALUE);
		return checkComboBox;
	}

	/**
	 * Creates a JavaFX CheckBox with the specified label and default style.
	 * 
	 * @param s Label text
	 * @return CheckBox instance
	 */
	public CheckBox createCheckBox(String s) {
		CheckBox checkBox = new CheckBox(s);
		return checkBox;
	}

	private Button createButtonInternal(String text, int wid, String tt, String imageName) {
		Button button = new Button();
		// Use centralized micro padding for buttons
		if (styles != null) {
			button.setPadding(styles.getMicroPadding());
		} else {
			button.setPadding(new Insets(2, 2, 2, 2));
		}
		if (tt != null && styles != null) {
			Tooltip tooltip = new Tooltip(tt);
			tooltip.setFont(Font.font(styles.getFontStyle()));
			button.setTooltip(tooltip);
		}
		if (text != null) {
			button.setText(text);
		}
		if (imageName != null && vars != null && styles != null
				&& (vars.getUseIcons().toLowerCase().equals("true") || text == null)) {
			try {
				double size = styles.getSmallButtonWidth();
				String imagePath = "file:" + vars.getResourceDir() + File.separator + imageName + ".png";
				Image image = new Image(imagePath, size, size, false, true);
				ImageView imageView = new ImageView(image);
				imageView.autosize();
				button.setGraphic(imageView);
				button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				button.setPrefSize(size, size);
				button.setMaxSize(size, size);
				button.setMinSize(size, size);
				// No padding when using icon-only small buttons
				button.setPadding(styles.getNoPadding());
				// Prevent JavaFX default hover/pressed background from showing through transparent icon pixels
				button.setStyle("-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0;");
			} catch (Exception e) {
				System.out.println("Could not create button images.");
			}
		} else if (wid > 0 && styles != null) {
			double height = styles.getSmallButtonWidth();
			button.setPrefSize(wid, height);
			button.setMaxSize(wid, height);
			button.setMinSize(wid, height);
		}
		if (styles != null)
			button = resizeButtonText(button);
		return button;
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
		return createButtonInternal(text, styles.getBigButtonWidth(), null, null);
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
		Button button = createButtonInternal(text, width, null, null);
		if (handler != null) {
			button.setOnAction(handler);
		}
		return button;
	}

	public Button resizeButtonText(Button button) {
		if (styles == null)
			return button;
		String text = button.getText();
		resizeButtonText(button, text, styles.getFontSize());
		return button;
	}

	public Button resizeButtonText(Button button, String text, double size) {
		if (styles == null)
			return button;
		FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
		button.setFont(Font.font(size));
		double font = button.getFont().getSize();
		// Preserve any existing style properties and replace/add only the font-size rule
		String existingStyle = button.getStyle();
		if (existingStyle == null)
			existingStyle = "";
		if (existingStyle.contains("-fx-font-size")) {
			existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + (font) + "px;");
		} else {
			if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
				existingStyle += ";";
			existingStyle += "-fx-font-size:" + (font) + "px;";
		}
		button.setStyle(existingStyle);
		button.applyCss();
		button.layout();
		button.setText(text);

		double prefWidth = button.getPrefWidth();
		double estimatedWidth = fontLoader.computeStringWidth(text, button.getFont());
		double prefHeight = button.getPrefHeight();

		if ((size > 0) && ((estimatedWidth > prefWidth - 5) || (size > prefHeight - 5))) {
			return resizeButtonText(button, text, size - 0.5);
		} else {

			return button;
		}
	}

	public Label resizeLabelText(Label label) {
		if (styles == null)
			return label;
		return resizeLabelText(label, label.getText(), styles.getFontSize());
	}

	public Label resizeLabelText(Label label, String text, double size) {
		if (styles == null)
			return label;
		FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
		// Preserve existing font family/weight and preserve any existing inline style rules.
		Font existingFont = label.getFont();
		String family = existingFont != null ? existingFont.getFamily() : null;
		// Keep whatever weight/posture the label already has; just change the size.
		label.setFont(Font.font(family, size));
		double font = label.getFont().getSize();
		String existingStyle = label.getStyle();
		if (existingStyle == null)
			existingStyle = "";
		if (existingStyle.contains("-fx-font-size")) {
			existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + (font) + "px;");
		} else {
			if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
				existingStyle += ";";
			existingStyle += "-fx-font-size:" + (font) + "px;";
		}
		label.setStyle(existingStyle);
		label.applyCss();
		label.layout();
		label.setText(text);
		double prefWidth = label.getPrefWidth();
		double predictedWidth = fontLoader.computeStringWidth(label.getText(), label.getFont());

		if ((prefWidth > 0) && (predictedWidth > prefWidth - 10)) {
			return resizeLabelText(label, text, size - 0.5);
		} else
			return label;
	}

	public String returnAppendedString(String[] stringArray) {
		if (stringArray == null || stringArray.length == 0)
			return "";
		StringBuilder result = new StringBuilder(stringArray[0]);
		for (int i = 1; i < stringArray.length; i++) {
			result.append(",").append(stringArray[i]);
		}
		return result.toString();
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
		ArrayList<String> resultList = new ArrayList<>();
		for (String str1 : list) {
			boolean match = false;
			for (String str2 : resultList) {
				if (str1.trim().equals(str2.trim())) {
					match = true;
					break;
				}
			}
			if (!match)
				resultList.add(str1.trim());
		}
		if (resultList.contains("Select One")) {
			resultList.remove("Select One");
			Collections.sort(resultList);
			resultList.add(0, "Select One");
		} else if (resultList.contains("Select One")) {
			resultList.remove("Select One or More");
			Collections.sort(resultList);
			resultList.add(0, "Select One or More");
		} else if (resultList.contains("All")) {
			resultList.remove("All");
			Collections.sort(resultList);
			resultList.add(0, "All");
		}
		return resultList;
	}

	public String getUniqueString() {

		if (orig_date == 0) {
			Calendar cal = Calendar.getInstance();
			cal.set(2025, Calendar.JULY, 15);
			orig_date = cal.getTime().getTime();
		}

		Date d = new Date();
		long now = d.getTime();
		int diff_sec = (int) Math.floor((now - orig_date) / 1000.);
		String temp="" + diff_sec;
		temp.substring(temp.length() - 6);

		return "" + diff_sec;
	}

	public String commentLinesInString(String stringLine, String startComment, String endComment) {
		String[] stringLines = splitEOL(stringLine);
		StringBuilder newStringLine = new StringBuilder();
		for (String line : stringLines) {
			String commented = startComment + line + endComment;
			newStringLine.append(commented);
			if (line.indexOf(vars.getEol()) < 0)
				newStringLine.append(vars.getEol());
		}
		return newStringLine.toString();
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

	public String[] getAllSelectedRegions(TreeView<String> tree) {

		ArrayList<CheckBoxTreeItem<String>> selectedLeaves = returnAllSelectedLeaves(tree.getRoot());
		int n = selectedLeaves.size();
		String[] list = new String[n];
		for (int i = 0; i < selectedLeaves.size(); i++) {
			list[i] = selectedLeaves.get(i).getValue();
		}
		list = removeUSADuplicate(list);
		list = removeWorldRegion(list);

		return list;
	}

	public String[] removeWorldRegion(String[] s_orig) {
		String[] s_return;

		int world_location = -1;
		int i = 0;
		for (String s : s_orig) {
			if (s.trim().toLowerCase().equals("world")) {
				world_location = i;
			}
			i++;
		}

		if (world_location == -1) {
			s_return = s_orig;
		} else {
			s_return = new String[s_orig.length - 1];
			int pos = 0;
			for (int j = 0; j < s_orig.length; j++) {
				if (j != world_location) {
					s_return[pos] = s_orig[j];
					pos++;
				}
			}
		}

		return s_return;
	}

	public String[] removeUSADuplicate(String[] s_orig) {
		String[] s_return;

		int usa_count = 0;
		for (String s : s_orig) {
			if (s.trim().toLowerCase().equals("usa")) {
				usa_count++;
			}
		}

		if (usa_count < 2) {
			s_return = s_orig;
		} else {
			s_return = new String[s_orig.length - 1];
			for (int i = 0; i < s_return.length; i++) {
				s_return[i] = s_orig[i];
			}
		}

		return s_return;
	}

	public ArrayList<CheckBoxTreeItem<String>> returnAllSelectedLeaves(TreeItem<String> rootNode) {
		ArrayList<TreeItem<String>> leaves = new ArrayList<>();
		ArrayList<CheckBoxTreeItem<String>> selectedLeaves = new ArrayList<>();
		getAllChildren(rootNode, leaves);
		for (TreeItem<String> leaf : leaves) {
			if (leaf instanceof CheckBoxTreeItem) {
				CheckBoxTreeItem<String> temp = (CheckBoxTreeItem<String>) leaf;
				if (temp.isSelected()) {
					selectedLeaves.add(temp);
				}
			}
		}
		return selectedLeaves;
	}

	public boolean getAllChildren(TreeItem<String> node, ArrayList<TreeItem<String>> list) {
		ObservableList<TreeItem<String>> childrenNodes = node.getChildren();
		boolean areAllChildrenSelected = true;

		if (!childrenNodes.isEmpty()) {

			for (TreeItem<String> item : childrenNodes) {
				if (!getAllChildren(item, list))
					areAllChildrenSelected = false;
			}
			// If all of the children are selected, the node itself is also
			// added
			// this may be problematic if GCAM-USA accepts taxes or policies at
			// the USA level
			if (areAllChildrenSelected)
				list.add(node);

		} else {
			list.add(node);
		}

		return areAllChildrenSelected;
	}

	public boolean confirmAction(String s) {
		return UtilsDialogs.getInstance().confirmAction(s);
	}

	public boolean showInformationDialog(String title, String header, String content) {
		return UtilsDialogs.getInstance().showInformationDialog(title, header, content);
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
		if (files == null)
			return false;
		boolean b = false;

		ArrayList<String> file1Content = files.getStringArrayFromFile(file1, "#");
		ArrayList<String> file2Content = files.getStringArrayFromFile(file2, "#");

		Patch<String> patch = null;

		try {
			patch = DiffUtils.diff(file1Content, file2Content);
			b = true;
		} catch (DiffException e) {
			e.printStackTrace();
		}

		ArrayList<String> diff = new ArrayList<String>();
		if (patch == null) {
			diff.add("Diff failed: could not compute differences.");
			displayArrayList(diff, "Differences");
			return false;
		}

		// Unified diff style report for readability.
		diff.add("--- " + safeFileLabel(file1));
		diff.add("+++ " + safeFileLabel(file2));

		int inserts = 0;
		int deletes = 0;
		int changes = 0;

		for (AbstractDelta<String> delta : patch.getDeltas()) {
			if (delta == null)
				continue;
			switch (delta.getType()) {
			case INSERT:
				inserts++;
				break;
			case DELETE:
				deletes++;
				break;
			case CHANGE:
				changes++;
				break;
			default:
				break;
			}
		}

		diff.add("Hunks: " + patch.getDeltas().size() + "  (" + "insert=" + inserts + ", delete=" + deletes + ", change="
				+ changes + ")");
		diff.add("---");

		// Show a little context around changes.
		final int context = 2;

		for (AbstractDelta<String> delta : patch.getDeltas()) {
			if (delta == null)
				continue;
			appendUnifiedHunk(diff, delta, file1Content, file2Content, context, true);
		}

		if (patch.getDeltas().isEmpty()) {
			diff.add("(No differences)");
		}

		displayArrayList(diff, "Differences");

		return b;
	}

	public List<DiffLineRow> generateSideBySideDiffRows(String file1, String file2) {
		if (files == null)
			return new ArrayList<>();
		ArrayList<String> file1Content = files.getStringArrayFromFile(file1, "#");
		ArrayList<String> file2Content = files.getStringArrayFromFile(file2, "#");

		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> "")
				.newTag(f -> "")
				.build();

		List<DiffLineRow> rows = new ArrayList<>();
		List<DiffRow> diffRows;
		try {
			diffRows = generator.generateDiffRows(file1Content, file2Content);
		} catch (DiffException e) {
			diffRows = new ArrayList<>();
			diffRows.add(new DiffRow(DiffRow.Tag.CHANGE, "Diff failed: " + e.getMessage(), ""));
		}
		int oldLine = 1;
		int newLine = 1;
		for (DiffRow row : diffRows) {
			if (row == null)
				continue;
			DiffRow.Tag tag = row.getTag();
			int oldNum = 0;
			int newNum = 0;
			switch (tag) {
			case INSERT:
				newNum = newLine++;
				break;
			case DELETE:
				oldNum = oldLine++;
				break;
			case CHANGE:
				oldNum = oldLine++;
				newNum = newLine++;
				break;
			case EQUAL:
			default:
				oldNum = oldLine++;
				newNum = newLine++;
				break;
			}
			rows.add(new DiffLineRow(oldNum, newNum, row.getOldLine(), row.getNewLine(), tag));
		}
		return rows;
	}

	/** Returns a readable short label for a file path (basename when available). */
	private String safeFileLabel(String filePath) {
		try {
			if (filePath == null)
				return "";
			File f = new File(filePath);
			String name = f.getName();
			return (name != null && !name.isEmpty()) ? name : filePath;
		} catch (Throwable t) {
			return filePath == null ? "" : filePath;
		}
	}

	/**
	 * Safe read of a line from a list; returns empty string when out of bounds.
	 */
	private String safeGetLine(List<String> lines, int index0) {
		if (lines == null)
			return "";
		if (index0 < 0 || index0 >= lines.size())
			return "";
		String s = lines.get(index0);
		return s == null ? "" : s;
	}

	/**
	 * Removes trailing whitespace for readability and to reduce noise.
	 * (We still show the original content aside from trailing whitespace.)
	 */
	private String rstrip(String s) {
		if (s == null)
			return "";
		int end = s.length();
		while (end > 0) {
			char c = s.charAt(end - 1);
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
				end--;
			} else {
				break;
			}
		}
		return s.substring(0, end);
	}

	private void appendUnifiedHunk(ArrayList<String> out, AbstractDelta<String> delta, List<String> oldLines,
			List<String> newLines, int context, boolean normalizeTrailingWhitespace) {
		if (out == null || delta == null)
			return;

		int oldPos = 0;
		int oldSize = 0;
		int newPos = 0;
		int newSize = 0;
		List<String> srcLines = null;
		List<String> tgtLines = null;
		try {
			if (delta.getSource() != null) {
				oldPos = delta.getSource().getPosition();
				srcLines = delta.getSource().getLines();
				oldSize = (srcLines == null) ? 0 : srcLines.size();
			}
			if (delta.getTarget() != null) {
				newPos = delta.getTarget().getPosition();
				tgtLines = delta.getTarget().getLines();
				newSize = (tgtLines == null) ? 0 : tgtLines.size();
			}
		} catch (Throwable t) {
			// If anything goes wrong, fall back to the raw delta representation.
			out.add(String.valueOf(delta));
			out.add("---");
			return;
		}

		int oldStart = Math.max(0, oldPos - context);
		int oldEnd = Math.min(oldLines == null ? 0 : oldLines.size(), oldPos + Math.max(oldSize, 1) + context);

		int newStart = Math.max(0, newPos - context);
		int newEnd = Math.min(newLines == null ? 0 : newLines.size(), newPos + Math.max(newSize, 1) + context);

		// Hunk header (1-based line numbers)
		out.add("@@ -" + (oldStart + 1) + "," + (oldEnd - oldStart) + " +" + (newStart + 1) + "," + (newEnd - newStart)
				+ " @@  (" + delta.getType() + ")");

		// Old context up to change
		for (int i = oldStart; i < oldPos; i++) {
			String s = safeGetLine(oldLines, i);
			out.add(" " + (normalizeTrailingWhitespace ? rstrip(s) : s));
		}

		// Removed/changed old lines
		if (srcLines != null) {
			for (String s : srcLines) {
				out.add("-" + (normalizeTrailingWhitespace ? rstrip(s) : (s == null ? "" : s)));
			}
		}

		// Added/changed new lines
		if (tgtLines != null) {
			for (String s : tgtLines) {
				out.add("+" + (normalizeTrailingWhitespace ? rstrip(s) : (s == null ? "" : s)));
			}
		}

		// New context after change
		int oldResume = oldPos + oldSize;
		for (int i = oldResume; i < oldEnd; i++) {
			String s = safeGetLine(oldLines, i);
			out.add(" " + (normalizeTrailingWhitespace ? rstrip(s) : s));
		}

		out.add("---");
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
		Platform.runLater(() -> displayArrayList(arrayListArg, title, false));
	}

	public void displayArrayList(ArrayList<String> arrayListArg, String title, boolean doWrap) {
		if (styles == null)
			return;
		final String finalTitle = title;
		Runnable displayTask = () -> {
			BorderPane border = new BorderPane();
			String usedTitle = finalTitle == null ? LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			stage.setTitle(usedTitle);
			stage.setWidth(900);
			stage.setHeight(800);
			stage.setResizable(true);
			TextArea textArea = new TextArea();
			textArea.setEditable(false);
			// Use flexible sizing so the display dialog can resize naturally
			textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			textArea.setMinHeight(0);
			textArea.setWrapText(doWrap);
			Button closeButton = createButton(LABEL_CLOSE, styles.getBigButtonWidth(), null);
			closeButton.setOnAction(e -> stage.close());
			StringBuilder text = new StringBuilder();
			if (arrayListArg != null) {
				for (String str : arrayListArg) {
					if (str.indexOf(vars.getEol()) < 0)
						text.append(str).append(vars.getEol());
					else
						text.append(str);
				}
				textArea.setText(text.toString());
				HBox buttonBox = new HBox();
				buttonBox.setPadding(new Insets(4, 4, 4, 4));
				buttonBox.setSpacing(5);
				buttonBox.setAlignment(Pos.CENTER);
				buttonBox.getChildren().addAll(closeButton);
				border.setCenter(textArea);
				border.setBottom(buttonBox);
				Scene scene = new Scene(border);
				stage.setScene(scene);
				stage.show();
			}
		};
		displayTask.run();
	}

	// ====================== Some table code for generating a popup to show CSV
	// tables ========================
	// from:
	// https://stackoverflow.com/questions/44956205/javafx-tableview-with-different-cell-types-and-unknown-size

	private final Pattern intPattern = Pattern.compile("-?[0-9]+");
	// this could probably be improved: demo purposes only
	private final Pattern doublePattern = Pattern.compile("-?(([0-9]+)|([0-9]*\\.[0-9]+))");

	public String[][] getDataMatrixFromArrayList(ArrayList<String> data) {
		if (data == null || data.isEmpty())
			return new String[0][0];
		int num_rows = data.size();
		String[][] dataMatrix = new String[num_rows][];
		for (int r = 0; r < num_rows; r++) {
			String row = data.get(r);
			if (row == null) {
				dataMatrix[r] = new String[0];
			} else {
				dataMatrix[r] = row.split(",", -1);
			}
		}
		return dataMatrix;
	}

	private int computeMaxRowLength(String[][] data) {
		if (data == null)
			return 0;
		int maxLength = 0;
		for (String[] row : data) {
			if (row != null && row.length > maxLength) {
				maxLength = row.length;
			}
		}
		return maxLength;
	}

	private String[] extractColumn(String[][] data, int columnIndex) {
		if (data == null || columnIndex < 0)
			return new String[0];
		String[] column = new String[data.length];
		for (int rowIndex = 0; rowIndex < data.length; rowIndex++) {
			if (data[rowIndex] != null && columnIndex < data[rowIndex].length && data[rowIndex][columnIndex] != null) {
				column[rowIndex] = data[rowIndex][columnIndex];
			} else {
				column[rowIndex] = "";
			}
		}
		return column;
	}

	private Class<?> deduceColumnType(String[] column) {
		if (column == null || column.length <= 1)
			return String.class;
		boolean hasValue = false;
		boolean allIntegers = true;
		boolean allNumeric = true;
		for (int rowIndex = 1; rowIndex < column.length; rowIndex++) {
			String value = column[rowIndex];
			if (value == null)
				continue;
			value = value.trim();
			if (value.isEmpty())
				continue;
			hasValue = true;
			if (!intPattern.matcher(value).matches()) {
				allIntegers = false;
			}
			if (!doublePattern.matcher(value).matches()) {
				allNumeric = false;
				break;
			}
		}
		if (!hasValue)
			return String.class;
		if (allIntegers)
			return Integer.class;
		if (allNumeric)
			return Double.class;
		return String.class;
	}

	private String getColumnHeader(String[][] data, int columnIndex) {
		if (columnIndex < 0)
			return "";
		String fallback = "Column " + (columnIndex + 1);
		if (data == null || data.length == 0 || data[0] == null || columnIndex >= data[0].length)
			return fallback;
		String header = data[0][columnIndex];
		if (header == null)
			return fallback;
		header = header.trim();
		return header.isEmpty() ? fallback : header;
	}

	public void showPopupTableOfCSVData(String title, ArrayList<String> csvData, int wd, int ht) {
		if (styles == null)
			return;
		final String finalTitle = title;
		Runnable popupTask = () -> {
			String usedTitle = finalTitle == null ? LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			stage.setTitle(usedTitle);
			stage.setWidth(wd);
			stage.setHeight(ht);
			BorderPane border = new BorderPane();
			stage.setResizable(true);

			Button closeButton = createButton(LABEL_CLOSE, styles.getBigButtonWidth(), null);

			closeButton.setOnAction(e -> {
				stage.close();
			});

			TableView<List<Object>> table = new TableView<>();
			table.setEditable(false);
			// Avoid fixed preferred size; allow table to grow and shrink with its container
			table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			table.setMinHeight(0);
			UtilsTable.installCopyPasteHandler(table);
			table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			String[][] rawData = getDataMatrixFromArrayList(csvData);
			int numCols = computeMaxRowLength(rawData);
			int startRowIndex = rawData.length > 0 ? 1 : 0;

			Class<?>[] types = new Class<?>[numCols];

			for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
				String[] column = extractColumn(rawData, columnIndex);
				types[columnIndex] = deduceColumnType(column);
				table.getColumns().add(createColumn(types[columnIndex], columnIndex, getColumnHeader(rawData, columnIndex)));
			}
			for (int rowIndex = startRowIndex; rowIndex < rawData.length; rowIndex++) {
				List<Object> row = new ArrayList<>();
				for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
					row.add(getDataAsType(rawData[rowIndex], types[columnIndex], columnIndex));
				}
				table.getItems().add(row);
			}

			HBox buttonBox = new HBox();
			buttonBox.setPadding(new Insets(4, 4, 4, 4));
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			// Export button added before Close
            Button exportButton = createButton("Export", styles.getBigButtonWidth(), null);
            exportButton.setOnAction(ev -> {
                try {
                    File initialDir = new File(vars.getGlimpseLogDir());
                    FileChooser.ExtensionFilter csvFilter = FileChooserPlus.createExtensionFilter("CSV files (*.csv)", "csv");
                    File chosen = FileChooserPlus.showSaveDialog(stage, "Save Scenario Report", initialDir, "scenario_report.csv", csvFilter);
                    if (chosen != null) {
                        files.saveFile(csvData, chosen.getPath());
                        showInformationDialog("Information", "Export successful", "Saved report to: " + chosen.getPath());
                    }
                } catch (Exception ex) {
                    showInformationDialog("Information", "Export failed", "Could not save report: " + ex.getMessage());
                }
            });

            buttonBox.getChildren().addAll(exportButton, closeButton);

			// root.getChildren().addAll(table, buttonBox);
			border.setCenter(table);
			border.setBottom(buttonBox);

			Scene scene = new Scene(border);
			// scene.setRoot(root);

			stage.setScene(scene);
			stage.show();
		};
		popupTask.run();
	}

	public void showPopupTableOfErrorReport(String title, ArrayList<String> csvData, int wd, int ht) {
		if (styles == null)
			return;
		if (csvData == null || csvData.isEmpty()) {
			showPopupTableOfCSVData(title, csvData, wd, ht);
			return;
		}
		final String finalTitle = title;
		Runnable popupTask = () -> {
			String usedTitle = finalTitle == null ? LABEL_DISPLAY : finalTitle;
			Stage stage = new Stage();
			stage.setTitle(usedTitle);
			stage.setWidth(wd);
			stage.setHeight(ht);
			BorderPane border = new BorderPane();
			stage.setResizable(true);

			Button closeButton = createButton(LABEL_CLOSE, styles.getBigButtonWidth(), null);
			closeButton.setOnAction(e -> stage.close());

			TableView<List<Object>> table = new TableView<>();
			table.setEditable(false);
			table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			table.setMinHeight(0);
			UtilsTable.installCopyPasteHandler(table);
			table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			String[][] rawData = getDataMatrixFromArrayList(csvData);
			int numCols = computeMaxRowLength(rawData);
			String[] headerRow = rawData.length > 0 ? rawData[0] : new String[0];
			int startRowIndex = rawData.length > 0 ? 1 : 0;
			int classCol = findColumnIndex(headerRow, "Classification");

			Class<?>[] types = new Class<?>[numCols];
			for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
				String[] column = extractColumn(rawData, columnIndex);
				types[columnIndex] = deduceColumnType(column);
				TableColumn<List<Object>, String> col = createColumn(types[columnIndex], columnIndex, getColumnHeader(rawData, columnIndex));
				installErrorReportOverflowBehavior(table, col, columnIndex, numCols);
				table.getColumns().add(col);
			}

			ObservableList<List<Object>> master = FXCollections.observableArrayList();
			for (int rowIndex = startRowIndex; rowIndex < rawData.length; rowIndex++) {
				List<Object> row = new ArrayList<>();
				for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
					row.add(getDataAsType(rawData[rowIndex], types[columnIndex], columnIndex));
				}
				master.add(row);
			}

			FilteredList<List<Object>> filtered = new FilteredList<>(master, row -> true);
			table.setItems(filtered);

			ChoiceBox<String> viewSelector = new ChoiceBox<>(FXCollections.observableArrayList(
					"All lines",
					"Major errors",
					"Moderate errors",
					"Minor errors"));
			viewSelector.getSelectionModel().select(0);
			viewSelector.setTooltip(new Tooltip("Choose which rows to display"));

			viewSelector.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
				int idx = newV == null ? 0 : newV.intValue();
				filtered.setPredicate(row -> {
					if (row == null)
						return false;
					if (idx == 0)
						return true;
					String classification = getCellString(row, classCol);
					switch (idx) {
					case 1:
						return "major".equalsIgnoreCase(classification);
					case 2:
						return "moderate".equalsIgnoreCase(classification);
					case 3:
						return "minor".equalsIgnoreCase(classification);
					default:
						return true;
					}
				});
				table.getSelectionModel().clearSelection();
				if (!table.getItems().isEmpty()) {
					table.scrollTo(0);
				}
			});

			Button saveAsBtn = new Button("Save As...");
			saveAsBtn.setTooltip(new Tooltip("Save the currently visible error report as CSV"));
			saveAsBtn.setOnAction(ev -> {
				try {
					File initialDir = new File(vars.getGlimpseLogDir());
					FileChooser.ExtensionFilter csvFilter = FileChooserPlus.createExtensionFilter("CSV files (*.csv)", "csv");
					File chosen = FileChooserPlus.showSaveDialog(stage, "Save Error Report", initialDir, "error_report.csv", csvFilter);
					if (chosen != null) {
						ArrayList<String> exportRows = new ArrayList<>();
						ArrayList<String> header = new ArrayList<>();
						for (int col = 0; col < numCols; col++) {
							header.add(sanitizeCsvField(getColumnHeader(rawData, col)));
						}
						exportRows.add(buildCsvRow(header, numCols));
						for (List<Object> row : filtered) {
							ArrayList<String> fields = new ArrayList<>();
							for (int col = 0; col < numCols; col++) {
								fields.add(sanitizeCsvField(getCellString(row, col)));
							}
							exportRows.add(buildCsvRow(fields, numCols));
						}
						files.saveFile(exportRows, chosen.getPath());
						showInformationDialog("Information", "Export successful", "Saved report to: " + chosen.getPath());
					}
				} catch (Exception ex) {
					showInformationDialog("Information", "Export failed", "Could not save report: " + ex.getMessage());
				}
			});

			HBox controls = new HBox(10, new Label("View:"), viewSelector, saveAsBtn);
			controls.setPadding(new Insets(6, 10, 6, 10));
			controls.setAlignment(Pos.CENTER_LEFT);

			HBox buttonBox = new HBox();
			buttonBox.setPadding(new Insets(4, 4, 4, 4));
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			buttonBox.getChildren().addAll(closeButton);

			border.setTop(controls);
			border.setCenter(table);
			border.setBottom(buttonBox);

			Scene scene = new Scene(border);
			try {
				java.net.URL cssUrl = GLIMPSEUtils.class.getResource("/resources/modern.css");
				if (cssUrl != null) {
					scene.getStylesheets().add(cssUrl.toExternalForm());
				}
			} catch (Exception ignored) {
			}
		 stage.setScene(scene);
			stage.show();
		};
		popupTask.run();
	}

	private int findColumnIndex(String[] headerRow, String headerName) {
		if (headerRow == null || headerName == null)
			return -1;
		for (int i = 0; i < headerRow.length; i++) {
			String header = headerRow[i] == null ? "" : headerRow[i].trim();
			if (headerName.equalsIgnoreCase(header))
				return i;
		}
		return -1;
	}

	private String getCellString(List<Object> row, int colIndex) {
		if (row == null || colIndex < 0 || colIndex >= row.size())
			return "";
		Object value = row.get(colIndex);
		return value == null ? "" : value.toString();
	}

	private Object getDataAsType(String[] row, Class<?> type, int columnIndex) {
		/**
		 * Converts a value from a row to the specified type for a given column index.
		 * 
		 * @param row         The row of data as a String array
		 * @param type        The target type (Integer, Double, or String)
		 * @param columnIndex The index of the column
		 * @return The value converted to the specified type, or a default value if
		 *         conversion fails
		 */
		try {
			if (type == Integer.class) {
				if (columnIndex < row.length) {
					return Integer.valueOf(row[columnIndex]);
				} else {
					return new Integer(0);
				}
			} else if (type == Double.class) {
				if (columnIndex < row.length) {
				 return Double.valueOf(row[columnIndex]);
				} else {
					return new Double(0.0);
				}
			} else {
				if (columnIndex < row.length) {
					return row[columnIndex];
				} else {
					return "";
				}
			}
		} catch (Exception e) {
			return "";
		}
	}

	private TableColumn<List<Object>, String> createColumn(Class<?> type, int index, String name) {
		/**
		 * Creates a TableColumn for a TableView with the specified type, index, and
		 * name.
		 * 
		 * @param type  The data type of the column
		 * @param index The index of the column
		 * @param name  The name of the column
		 * @return TableColumn instance
		 */
		String text = name;
		TableColumn<List<Object>, String> col = new TableColumn<>(text);
		col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(index).toString()));
		return col;
	}

	private void installErrorReportOverflowBehavior(TableView<List<Object>> table, TableColumn<List<Object>, String> col,
			int colIndex, int totalColumns) {
		col.setCellFactory(column -> new TableCell<List<Object>, String>() {
			private final Text textNode = new Text();
			private final StackPane textPane = new StackPane(textNode);
			private final Rectangle clip = new Rectangle();

			{
				textNode.fontProperty().bind(fontProperty());
				textNode.fillProperty().bind(textFillProperty());
				textPane.setAlignment(Pos.CENTER_LEFT);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				clip.widthProperty().bind(widthProperty());
				clip.heightProperty().bind(heightProperty());
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					setClip(clip);
					toBack();
					return;
				}

				textNode.setText(item);
				setText(null);
				setGraphic(textPane);

				boolean allowOverflow = shouldAllowOverflow(item, colIndex, totalColumns);
				if (allowOverflow) {
					setClip(null);
					toFront();
				} else {
					setClip(clip);
					toBack();
				}
			}

			private boolean shouldAllowOverflow(String text, int columnIndex, int columnCount) {
				if (text == null || text.trim().isEmpty()) {
					return false;
				}
				Object rowObj = getTableRow() == null ? null : getTableRow().getItem();
				if (!(rowObj instanceof List)) {
					return false;
				}
				@SuppressWarnings("unchecked")
				List<Object> rowItem = (List<Object>) rowObj;
				if (!areRightCellsEmpty(rowItem, columnIndex, columnCount)) {
					return false;
				}
				double available = getWidth();
				if (available <= 0) {
					available = col.getWidth();
				}
				if (getInsets() != null) {
					available -= (getInsets().getLeft() + getInsets().getRight());
				}
				if (available <= 0) {
					return false;
				}
				Font font = getFont();
				if (font == null) {
					return false;
				}
				double textWidth = Toolkit.getToolkit().getFontLoader().computeStringWidth(text, font);
				return textWidth > (available + 1.0);
			}
		});
	}

	private boolean areRightCellsEmpty(List<Object> rowItem, int columnIndex, int columnCount) {
		if (rowItem == null) {
			return false;
		}
		for (int i = columnIndex + 1; i < columnCount; i++) {
			String value = i < rowItem.size() && rowItem.get(i) != null ? rowItem.get(i).toString() : "";
			if (!value.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public double[][] calculateValues(String type, int start_year, int end_year, double initial_value, double growth,
			int period_length, double factor) {

		double[][] array = calculateValues(type, start_year, end_year, initial_value, growth, period_length);

		for (int i = 0; i < array[0].length; i++) {
			array[1][i] = array[1][i] * factor;
		}

		return array;
	}

	public double[][] calculateValues(String type, int start_year, int end_year, double initial_value, double growth,
			int period_length) {

		double[][] returnMatrix;

		double final_value = 0.0;

		int num_periods = (end_year - start_year) / period_length + 1;
		int init_year = start_year;
		double val = 0.0;
		int year = 0;
		returnMatrix = new double[2][num_periods];

		for (int t = 0; t < num_periods; t++) {
			year = init_year + t * period_length;

			switch (type) {
			case "Initial w/% Growth/yr":
				val = initial_value * Math.pow(1 + growth / 100, t * 5);
				break;
			case "Initial w/% Growth/pd":
				val = initial_value * Math.pow(1 + growth / 100, t);
			break;
			case "Initial w/Delta/yr":
				val = (initial_value) + growth * 5 * t;
				break;
			case "Initial w/Delta/pd":
				val = (initial_value) + growth * t;
				break;
			case "Initial and Final":
				if (t == 0)
					final_value = growth; // final value was passed in as "growth
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			case "Initial and Final %":
				if (t == 0) {
					initial_value /= 100;
					final_value = growth / 100;
					; // final value was passed in as "growth
				}
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			}
			returnMatrix[0][t] = year;
			returnMatrix[1][t] = val;

		}

		return returnMatrix;
	}

	public double[][] calculateValues(String type, boolean isPercent, int start_year, int end_year,
			double initial_value, double growth, int period_length, double factor) {
		double[][] array = calculateValues(type, isPercent, start_year, end_year, initial_value, growth, period_length);

		for (int i = 0; i < array[0].length; i++) {
			array[1][i] = array[1][i] * factor;
		}

		return array;
	}

	public double[][] calculateValues(String type, boolean isPercent, int start_year, int end_year,
			double initial_value, double growth, int period_length) {

		double[][] returnMatrix;

		double final_value = 0.0;

		int num_periods = (end_year - start_year) / period_length + 1;
		int init_year = start_year;
		double val = 0.0;
		int year = 0;
	 returnMatrix = new double[2][num_periods];

		if (isPercent) {
			initial_value /= 100.0;
			if (type.startsWith("Initial and Final"))
				growth /= 100.;
		}

		for (int t = 0; t < num_periods; t++) {
			year = init_year + t * period_length;

			switch (type) {
			case "Initial w/% Growth/yr":
				val = initial_value * Math.pow(1 + growth / 100, t * 5);
				break;
			case "Initial w/% Growth/pd":
				val = initial_value * Math.pow(1 + growth / 100, t);
				break;
			case "Initial w/Delta/yr":
				val = (initial_value) + growth * 5 * t;
				break;
			case "Initial w/Delta/pd":
				val = (initial_value) + growth * t;
				break;
			case "Initial and Final":
				if (t == 0)
					final_value = growth; // final value was passed in as "growth
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			case "Initial and Final %":
				if (t == 0) {
					initial_value /= 100;
					final_value = growth / 100;
					; // final value was passed in as "growth
				}
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			}
			returnMatrix[0][t] = year;
			returnMatrix[1][t] = val;

		}

		return returnMatrix;
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

	public String getStringUpToChar(String str, String ch) {
		String rtn_str = str;

		try {
			rtn_str = str.substring(0, str.indexOf(ch));
		} catch (Exception e) {
			;
		}

		return rtn_str.trim();
	}

	public String getStringBetweenCharSequences(String str, String start_sequence, String end_sequence) {
		String rtn_str = "";

		try {
			rtn_str = str.substring(0, str.indexOf(end_sequence));
		} catch (Exception e) {
			;
		}

		try {
			rtn_str = rtn_str.substring(rtn_str.indexOf(start_sequence) + 1);
		} catch (Exception e) {
			;
		}

		return rtn_str.trim();
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
					num = numf * (1e-6) / valf * 1.055;

					val = "," + num + ",1.0e6";
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

	public String getSubsectorConversionsOld(double numf, String region, String sector, String subsector, int year) {

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
					num = numf * (1e-9) / valf * 1.055;

					val = "," + num + ",1.0e9";
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

	public int getMaxValFromStringArray(String[] str_array) {
		int max_int = 0;
		for (String s : str_array) {
			int val = Integer.parseInt(s);
			if (val > max_int)
				max_int = val;
		}
		return max_int;
	}

	public int getMinValFromStringArray(String[] str_array) {
		int min_int = Integer.MAX_VALUE;
		for (String s : str_array) {
			int val = Integer.parseInt(s);
			if (val < min_int)
				min_int = val;
		}
		return min_int;
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
		if (files == null || vars == null)
			return new ArrayList<>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		formatter.setGroupingUsed(false);
		double min_dmd = 0.0001;
		double min_red = 0.01;
		int total_fails = 0, minor_fails = 0, min_smallmkt_fails = 0, major_fails = 0, moderate_fails = 0;
		String scenarioLabel = (scenario == null || scenario.trim().isEmpty()) ? "exe/main_log.txt" : scenario;
		ArrayList<String> report = new ArrayList<>();
		ArrayList<String[]> tokenRows = new ArrayList<>();
		ArrayList<String> classifications = new ArrayList<>();
		ArrayList<String> smallMarkets = new ArrayList<>();
		int maxTokenCount = 0;
		File mainlogfile = new File(main_log_file);
		if (mainlogfile.exists()) {
			String[] str = { "ERROR", "SEVERE", "Period" };
			ArrayList<String> error_lines = files.getStringArrayWithPrefix(mainlogfile.getPath(), str);
			for (String errorLine : error_lines) {
				if (errorLine == null)
					continue;
				String normalized = errorLine.replace(":", ",");
				String[] tokens = normalized.split(",");
				maxTokenCount = Math.max(maxTokenCount, tokens.length);
				String classification = "";
				String smallMarket = "";
				try {
					if (tokens.length > 12) {
						double red = Double.parseDouble(tokens[7].trim());
						double dmd = Double.parseDouble(tokens[9].trim());
						tokens[12].trim();
						total_fails++;
						if (red > min_red) {
							if (red > min_red * 5.0) {
								classification = "MAJOR";
								major_fails++;
								if (dmd <= min_dmd) {
								}
							} else {
								classification = "MODERATE";
								moderate_fails++;
								if (dmd <= min_dmd)
									min_smallmkt_fails++;
							}
						} else {
							classification = "MINOR";
							minor_fails++;
							if (dmd <= min_dmd)
								min_smallmkt_fails++;
						}
						smallMarket = (dmd <= min_dmd) ? "true" : "false";
					}
				} catch (Exception e) {
					// Ignore parse errors for robustness
				}
				tokenRows.add(tokens);
				classifications.add(classification);
				smallMarkets.add(smallMarket);
			}
			if (tokenRows.isEmpty()) {
				maxTokenCount = Math.max(maxTokenCount, 1);
				tokenRows.add(new String[] { "No errors reported" });
				classifications.add("");
				smallMarkets.add("");
			}
			if (total_fails > 0) {
				maxTokenCount = Math.max(maxTokenCount, 1);
				String verdict;
				if (total_fails == 0) {
					verdict = "Verdict: Pass (no errors)";
				} else if (total_fails == minor_fails) {
					verdict = "Verdict: Pass? (all errors are minor)";
				} else if (total_fails == minor_fails + moderate_fails) {
					verdict = "Verdict: Pass? (all errors are minor or moderate)";
				} else if (total_fails == min_smallmkt_fails) {
					verdict = "Verdict: Pass? (all fails are in small markets)";
				} else if (total_fails == minor_fails + min_smallmkt_fails) {
					verdict = "Verdict: Pass? (all fails are minor or in small markets)";
				} else {
					verdict = "Verdict: Fail? (major, non-small market failures)";
				}
				String summary = "Total errors=" + total_fails + "; Major errors=" + major_fails + "; Moderate errors="
						+ moderate_fails + "; Small market errors=" + min_smallmkt_fails + "; "
						+ verdict + " (" + formatter.format(min_red * 100.0) + "-" + formatter.format(min_red * 5.0 * 100.0)
						+ "% thresholds)";
				tokenRows.add(new String[] { "Summary", summary });
				classifications.add("");
				smallMarkets.add("");
			}
		} else {
			maxTokenCount = Math.max(maxTokenCount, 1);
			tokenRows.add(new String[] { "Main log not found" });
			classifications.add("");
			smallMarkets.add("");
		}

		int columnCount = 1 + Math.max(1, maxTokenCount) + 2;
		for (int i = 0; i < tokenRows.size(); i++) {
			ArrayList<String> fields = new ArrayList<>();
			fields.add(sanitizeCsvField(scenarioLabel));
			String[] tokens = tokenRows.get(i);
			for (int t = 0; t < Math.max(1, maxTokenCount); t++) {
				String token = (tokens != null && t < tokens.length) ? tokens[t] : "";
				fields.add(sanitizeCsvField(token));
			}
			fields.add(sanitizeCsvField(classifications.get(i)));
			fields.add(sanitizeCsvField(smallMarkets.get(i)));
			report.add(buildCsvRow(fields, columnCount));
		}
		return report;
	}

	/**
	 * Builds a table-ready CSV list for error reports (adds a header and pads rows).
	 *
	 * @param rows CSV rows from generateErrorReport
	 * @return CSV with header and uniform column count
	 */
	public ArrayList<String> buildErrorReportTable(ArrayList<String> rows) {
		ArrayList<String> table = new ArrayList<>();
		int maxCols = 0;
		if (rows != null) {
			for (String row : rows) {
				if (row == null)
					continue;
				int len = row.split(",", -1).length;
				if (len > maxCols)
					maxCols = len;
			}
		}
		if (maxCols < 4)
			maxCols = 4; // Scenario + at least 1 field + Classification + SmallMarket
		int tokenCols = Math.max(1, maxCols - 3);
		ArrayList<String> header = new ArrayList<>();
		header.add("Scenario");
		for (int i = 1; i <= tokenCols; i++) {
			header.add("Field" + i);
		}
		header.add("Classification");
		header.add("SmallMarket");
		table.add(buildCsvRow(header, header.size()));

		if (rows == null || rows.isEmpty()) {
			ArrayList<String> noErr = new ArrayList<>();
			noErr.add(" ");
			noErr.add("No errors reported");
			table.add(buildCsvRow(noErr, header.size()));
			return table;
		}
		for (String row : rows) {
			String[] parts = row == null ? new String[0] : row.split(",", -1);
			ArrayList<String> fields = new ArrayList<>();
			Collections.addAll(fields, parts);
			table.add(buildCsvRow(fields, header.size()));
		}
		return table;
	}

	public String processErrors(ArrayList<String> errors, double min_red) {
		if (errors == null || vars == null)
			return "";
		double min_dmd = 0.0001;
		int total = 0;
		int major = 0;
		int moderate = 0;
		int minor = 0;
		int smallMarkets = 0;
		for (String errorLine : errors) {
			if (errorLine == null)
				continue;
			String normalized = errorLine.replace(":", ",");
			String[] tokens = normalized.split(",");
			try {
				if (tokens.length > 12) {
					double red = Double.parseDouble(tokens[7].trim());
					double dmd = Double.parseDouble(tokens[9].trim());
					String mkt = tokens[12].trim();
					total++;
					if (dmd <= min_dmd)
						smallMarkets++;
					if ((red > min_red) && (!mkt.contains("water consumption"))) {
						if (red > min_red * 5.0) {
							major++;
						} else {
							moderate++;
						}
					} else {
						minor++;
					}
				}
			} catch (Exception e) {
				// ignore parse errors
			}
		}
		if (total == 0)
			return "";
		return "total=" + total + ";major=" + major + ";moderate=" + moderate + ";minor=" + minor
				+ ";small=" + smallMarkets;
	}

	public String getRunningScenario(File mainLogFile) {
		if (files == null || mainLogFile == null || !mainLogFile.exists())
			return "";
		String configLine = files.searchForTextInFileS(mainLogFile, "Configuration file:", "#");
		if (configLine == null || configLine.trim().isEmpty())
			return "";
		String path = configLine.replace("Configuration file:", "").trim();
		if (path.isEmpty())
			return "";
		String name = new File(path).getParent();
		if (name == null)
			return "";
		String scenName = name.substring(name.lastIndexOf(File.separator) + 1);
		return scenName == null ? "" : scenName;
	}

	public String getScenarioStatusFromMainLog(File mainLogFile) {
		if (files == null || mainLogFile == null || !mainLogFile.exists())
			return "";
		String currentPeriod = getLatestRunningPeriod(mainLogFile);
		if (currentPeriod != null && !currentPeriod.isEmpty()) {
			return currentPeriod;
		}
		String unsolved = files.searchForTextInFileS(mainLogFile, "The following model periods did not solve:", "#");
		if (unsolved != null && !unsolved.trim().isEmpty()) {
			String msg = unsolved.replace("The following model periods did not solve:", "").trim();
			return "Unsolved,ERR " + msg;
		}
		String err = files.searchForTextInFileS(mainLogFile, "ERROR", "#");
		if (err != null && !err.trim().isEmpty()) {
			return "ERROR,ERR " + err.trim();
		}
		return "";
	}

	private static final Pattern RUNNING_PERIOD_PATTERN = Pattern.compile(
			"(?:^|[^A-Za-z])(period|final-calibration period|model period|solving period|time period)\\s*[:=]?\\s*(\\d{1,3})(?:[^0-9]|$)",
			Pattern.CASE_INSENSITIVE);

	private String getLatestRunningPeriod(File mainLogFile) {
		try {
			ArrayList<String> lines = files.getStringArrayFromFile(mainLogFile.getAbsolutePath(), "#");
			for (int i = lines.size() - 1; i >= 0; i--) {
				String line = lines.get(i);
				if (line == null) {
					continue;
				}
				String trimmed = line.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				Matcher matcher = RUNNING_PERIOD_PATTERN.matcher(trimmed);
				if (matcher.find()) {
					String period = matcher.group(2);
					if (period != null && !period.trim().isEmpty()) {
						return period.trim();
					}
				}
			}
		} catch (Exception ignored) {}
		return "";
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
		boolean warning = false;
		String status = "";
		try {
			com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
					.getOperatingSystemMXBean();
			double gb = 1073741824d;

			float physicalMemorySize = (float) (os.getTotalPhysicalMemorySize() / gb);
			float physicalMemoryFree = (float) (os.getFreePhysicalMemorySize() / gb);
			float swapSpaceSize = (float) (os.getTotalSwapSpaceSize() / gb);
			float freeSwapSpace = (float) (os.getFreeSwapSpaceSize() / gb);
			File drive = new File("/");
			float totalSpace = (float) (drive.getTotalSpace() / gb);
			float freeSpace = (float) (drive.getFreeSpace() / gb);
			float cpuLoad = (float) os.getSystemCpuLoad();

			String databaseName = vars != null ? vars.getgCamOutputDatabase() : null;
			String databaseShortName = "";
			float databaseSize = 0f;
			if (databaseName != null && !databaseName.trim().isEmpty()) {
				File databaseFolder = new File(databaseName);
				databaseShortName = databaseFolder.getName();
				if (databaseFolder.exists()) {
					Path databasePath = databaseFolder.toPath();
					databaseSize = (float) (files.getDirectorySize(databasePath) / gb);
				}
			}

			String warningRAM = "";
			String warningDisk = "";
			String warningSwap = "";
			String warningDb = "";
			float maxDbSize = vars != null ? vars.getMaxDatabaseSize() : 0f;

			if (physicalMemorySize > 0f && physicalMemoryFree / physicalMemorySize < 0.05f)
				warningRAM = "*";
			if (swapSpaceSize > 0f && freeSwapSpace / swapSpaceSize < 0.05f)
				warningSwap = "*";
			if (freeSpace < 40.0f)
				warningDisk = "*";
			if (maxDbSize > 0f && databaseSize > maxDbSize * .8f)
				warningDb = "*";

			if ((physicalMemorySize > 0f && physicalMemoryFree / physicalMemorySize < 0.05f)
					|| (swapSpaceSize > 0f && freeSwapSpace / swapSpaceSize < 0.05f) || (freeSpace < 40.0f)
					|| (maxDbSize > 0f && databaseSize > maxDbSize * .8f)) {
				warning = true;
			}

			String dbFreePct = (maxDbSize > 0f) ? String.format("%,.0f", (1.0f - (databaseSize / maxDbSize)) * 100.0f) : "n/a";
			status = "  Resources... " + "CPU: " + String.format("%,.0f", cpuLoad * 100.0f) + "% | " + "RAM: "
					+ String.format("%,.0f", physicalMemorySize) + "GB Free:"
					+ String.format("%,.0f", physicalMemorySize > 0f ? physicalMemoryFree / physicalMemorySize * 100.0f : 0.0f)
					+ "%" + warningRAM + " | " + "HD: " + String.format("%,.0f", freeSpace) + "GB Free:"
					+ String.format("%,.0f", totalSpace > 0f ? freeSpace / totalSpace * 100.0f : 0.0f) + "%" + warningDisk
					+ " | " + "Swap: " + String.format("%,.0f", swapSpaceSize) + "GB Free:"
					+ String.format("%,.0f", swapSpaceSize > 0f ? freeSwapSpace / swapSpaceSize * 100.0f : 0.0f) + "%"
					+ warningSwap + " | " + "DB: " + (databaseShortName.isEmpty() ? "(not set)" : databaseShortName) + " "
					+ String.format("%,.1f", databaseSize) + "GB Free:" + dbFreePct + "%" + warningDb;
		} catch (Exception e) {
			status = "";
		}
		if (warning)
			status = status.trim() + " !!!";
		return status;
	}

	private String sanitizeCsvField(String value) {
		if (value == null)
			return "";
		return value.replace(",", ";").replace("\r", " ").replace("\n", " ").trim();
	}

	private String buildCsvRow(List<String> fields, int columnCount) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < columnCount; i++) {
			if (i > 0)
				sb.append(",");

			String value = i < fields.size() ? fields.get(i) : "";
			sb.append(value == null ? "" : value);
		}
		return sb.toString();
	}
}
