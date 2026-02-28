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
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

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

    public String[][] trn_veh_info_table = null;
    public String[][] ldv2W_table = null;
    public String[][] ldv4W_table = null;
    public String[][] hdv_table = null;
    public String[][] oth_table = null;

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
	}

	/**
	 * Applies the ScenarioBuilder modern theme to a JavaFX Alert/Dialog.
	 */
	private void applyModernThemeToDialog(javafx.scene.control.Dialog<?> dialog) {
		if (dialog == null)
			return;
		try {
			javafx.scene.control.DialogPane pane = dialog.getDialogPane();
			if (pane == null)
				return;
			java.net.URL cssUrl = gui.ScenarioBuilder.class.getResource(gui.ScenarioBuilder.getModernCssResource());
			if (cssUrl != null) {
				String css = cssUrl.toExternalForm();
				if (!pane.getStylesheets().contains(css)) {
					pane.getStylesheets().add(css);
				}
			}
		} catch (Exception e) {
			// ignore theme failures; fall back to default Alert styling
		}
	}

	/**
	 * Checks if a string matches any item in the provided list.
	 *
	 * @param str        String to check
	 * @param marketList List of strings
	 * @return true if match found, false otherwise
	 */
	public boolean getMatch(String str, List<String> marketList) {
		if (str == null || marketList == null)
			return false;
		for (String item : marketList) {
			if (str.equals(item)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a string to the list if it is not already present.
	 *
	 * @param list List of strings
	 * @param str  String to add
	 * @return Updated list
	 */
	public ArrayList<String> addToArrayListIfUnique(ArrayList<String> list, String str) {
		if (list == null || str == null)
			return list;
		for (String item : list) {
			if (item.compareTo(str) == 0) {
				return list;
			}
		}
		list.add(str);
		return list;
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
		if (line == null || txt == null || delim == null)
			return "";
		String[] tokens = line.split(delim);
		for (String token : tokens) {
			if (token.indexOf(txt) >= 0) {
				return token;
			}
		}
		return "";
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
		if (ol == null || separator == null || vars == null)
			return "";
		StringBuilder rtn_str = new StringBuilder();
		for (String o : ol) {
			rtn_str.append(o).append(separator);
		}
		return rtn_str.toString();
	}

	/**
	 * Returns the current date as a string in yyyy-MM-dd format.
	 *
	 * @return Current date string
	 */
	public String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");// dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	/**
	 * Parses a date string using the class dateFormatStr.
	 *
	 * @param dateStr Date string
	 * @return Date object, or null if parsing fails
	 */
	public Date getFormattedDate(String dateStr) {
		if (dateStr == null)
			return null;
		DateFormat format = new SimpleDateFormat(dateFormatStr, Locale.ENGLISH);
		Date formattedDate = null;
		try {
			formattedDate = format.parse(dateStr);
		} catch (Exception e) {
			System.out.println("Error formatting " + dateStr);
		}
		return formattedDate;
	}


	/**
	 * Shows a warning message dialog on the JavaFX application thread.
	 *
	 * @param msg Message to display
	 */
	public void warningMessage(String msg) {
		if (msg == null)
			return;
		if (Platform.isFxApplicationThread()) {
			Alert alert = new Alert(AlertType.WARNING);
			applyModernThemeToDialog(alert);
			alert.setTitle(LABEL_WARNING);
			alert.setHeaderText(LABEL_WARNING);
			alert.setContentText(msg);
			alert.showAndWait();
		} else {
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.WARNING);
				applyModernThemeToDialog(alert);
				alert.setTitle(LABEL_WARNING);
				alert.setHeaderText(LABEL_WARNING);
				alert.setContentText(msg);
				alert.showAndWait();
			});
		}
		return;
	}
	
	/**
	 * Displays a dialog for text input and returns the entered text.
	 *
	 * @param descriptionType Dialog title
	 * @return Entered text
	 */
	public String getTextDialog(String descriptionType) {
		if (descriptionType == null)
			descriptionType = "";
		String title = descriptionType;
		TextArea textArea = new TextArea();
		textArea.setEditable(true);
		// Allow the dialog text area to grow with its container instead of using a fixed preferred size
		textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		textArea.setMinHeight(0);

		try {
			Stage stage = new Stage();

			stage.setTitle(title);
			stage.setWidth(400);
			stage.setHeight(400);
			Scene scene = new Scene(new Group());
			gui.ScenarioBuilder.applyModernTheme(scene);
			stage.setResizable(false);
			stage.setAlwaysOnTop(true);

			Button okButton = createButton("OK", styles.getBigButtonWidth(), null);

			okButton.setOnAction(e -> {
				stage.close();
			});

			VBox root = new VBox();
			// Use centralized small padding for dialog roots
			root.setPadding(styles.getSmallPadding());
			root.setSpacing(5);
			root.setAlignment(Pos.TOP_LEFT);

			String text = "";

			textArea.setText(text);

			HBox buttonBox = new HBox();
			// Use centralized small padding for dialog button areas
			buttonBox.setPadding(styles.getSmallPadding());
			buttonBox.setSpacing(5);
			buttonBox.setAlignment(Pos.CENTER);
			buttonBox.getChildren().addAll(okButton);

			root.getChildren().addAll(textArea, buttonBox);
			scene.setRoot(root);

			stage.setScene(scene);
			stage.showAndWait();
		} catch (Exception e) {
			System.out.println("Exception on textArea dialog:" + e);
		}

		return textArea.getText();
	}

	/**
	 * Converts a string to an integer, returns 0 if conversion fails.
	 *
	 * @param s String to convert
	 * @return Integer value
	 */
	public int convertStringToInt(String s) {
		s = s.replaceAll("\"", "").replaceAll("'", ""); // Remove quotations
		if (s == null)
			return 0;
		int rtn_val = 0;
		try {
			rtn_val = Integer.parseInt(s);
		} catch (Exception e) {
			System.out.println("problem converting " + s + " to int: " + e);
		}
		return rtn_val;
	}

	/**
	 * Returns the year string for a given period index.
	 *
	 * @param period Period index
	 * @return Year as string
	 */
	public String getYearForPeriod(int period) {
		String rtn_str = "";

		if (period == -1) {
			rtn_str = "2100";
		} else if (period == 0) {
			rtn_str = "1975";
		} else if (period == 1) {
			rtn_str = "1990";
		} else if (period == 2) {
			rtn_str = "2005";
		} else {
			rtn_str = 2005 + 5 * (period - 2) + "";
		}
		return rtn_str;
	}

	/**
	 * Returns the period index for a given year string.
	 *
	 * @param year Year as string
	 * @return Period index as string
	 */
	public String getPeriodForYear(String year) {
		if (year == null)
			return "";
		double year_d = 0;
		try {
			year_d = Double.parseDouble(year);
		} catch (NumberFormatException e) {
			return "";
		}
		double increment = (year_d - 2005.) / 5. + 2;
		int increment_int = (int) increment;
		return "" + increment_int;
	}

	/**
	 * Removes trailing commas from a string.
	 *
	 * @param s Input string
	 * @return String without trailing commas
	 */
	public String getRidOfTrailingCommasInString(String s) {
		if (s == null)
			return null;
		while (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	/**
	 * Removes trailing commas from each string in an array.
	 *
	 * @param s Array of strings
	 * @return Array with trailing commas removed
	 */
	public String[] getRidOfTrailingCommasInStringArray(String[] s) {
		if (s == null)
			return null;
		int i = 0;
		for (String str : s) {
			s[i++] = getRidOfTrailingCommasInString(str);
		}
		return s;
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
		if (input_string == null || input_string.isEmpty())
			return input_string;
		String output_string = input_string;

		if (input_string.length() == 1) {
			output_string = input_string.toUpperCase();
		}
		if (input_string.length() > 1) {
			output_string = input_string.substring(0, 1).toUpperCase() + input_string.substring(1).toLowerCase();
		}
		return output_string;
	}

	/**
	 * Shows a confirmation dialog for deletion.
	 *
	 * @return true if user confirms, false otherwise
	 */
	public boolean confirmDelete() {
		boolean continueWithDelete = true;
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyModernThemeToDialog(alert);
		alert.setTitle(LABEL_CONFIRMATION_DIALOG);
		alert.setHeaderText(LABEL_DELETE_SELECTED_ITEMS);
		alert.setContentText(LABEL_PLEASE_CONFIRM_DELETION);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.CANCEL) {
			continueWithDelete = false;
		}
		return continueWithDelete;
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
		if (str == null || delim == null)
			return new String[0];
		String s[] = str.split(delim);
		return s;
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
		if (line == null || delim == null)
			return new ArrayList<>();
		ArrayList<String> linesList = new ArrayList<>();
		String[] lines = splitString(line, delim);
		for (String l : lines) {
			linesList.add(l);
		}
		return linesList;
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
		if (arrayList == null)
			return "";
		StringBuilder result = new StringBuilder();
		for (String s : arrayList) {
			result.append(s).append(vars.getEol());
		}
		return result.toString();
	}

	/**
	 * Concatenates an ArrayList of strings into a single string with a custom delimiter.
	 *
	 * @param filesToSave ArrayList of strings
	 * @param delimiter   Delimiter string
	 * @return Concatenated string
	 */
	public String createStringFromArrayList(List<String> filesToSave, String delimiter) {
		if (filesToSave == null || delimiter == null)
			return "";
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < filesToSave.size(); i++) {
			result.append(filesToSave.get(i));
			if (i < filesToSave.size() - 1) {
				result.append(delimiter);
			}
		}
		return result.toString();
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
		if (str_array == null)
			return "";
		StringBuilder rtn_str = new StringBuilder();
		for (int i = 0; i < str_array.length; i++) {
			if (i > 0)
				rtn_str.append(",");
			rtn_str.append(str_array[i]);
		}
		return rtn_str.toString();
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
		String rtn_str = temp.substring(temp.length() - 6);

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
		if (str != null)
			str = str.trim();
		return str;
	}

	public String toSignificantFiguresString(double val, int significantFigures) {
		BigDecimal bd = new BigDecimal(val);
		String test = String.format("%." + significantFigures + "G", bd);
		if (test.contains("E+")) {
			test = String.format(Locale.US, "%.0f", Double.valueOf(String.format("%." + significantFigures + "G", bd)));
		}
		return test;
	}

	public String[] convertTo1990Dollars(String[] vals, String dollarYear) {
		if (files == null)
			return vals;
		String[] ret_vals = vals;
		String conversion_str = "1.0";

		try {
			for (int i = 0; i < files.getMonetaryConversionsFileContent().size(); i++) {
				String s[] = files.getMonetaryConversionsFileContent().get(i).split(",");
				if (s[0].equals(dollarYear))
					conversion_str = s[1];
			}

			double conversion_dbl = Double.parseDouble(conversion_str);

			for (int i = 0; i < vals.length; i++) {
				double val = Double.parseDouble(vals[i]) * conversion_dbl;
				ret_vals[i] = "" + val;
			}
		} catch (Exception e) {
			System.out.println("Error making dollar year conversion. Returning original values.");
		}

		return ret_vals;
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
		// confirmation of delete
		boolean continueAction = true;
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyModernThemeToDialog(alert);
		alert.setTitle("Confirmation Dialog");
		alert.setHeaderText(s);
		alert.setContentText("Please confirm.");
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.CANCEL) {
			continueAction = false;
		}
		return continueAction;
	}

	public boolean showInformationDialog(String title, String header, String content) {
		if (title == null || header == null || content == null)
			return false;
		Alert alert = new Alert(AlertType.INFORMATION);
		applyModernThemeToDialog(alert);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
		return true;
	}

	public boolean confirmArchiveScenario() {
		if (vars == null)
			return false;
		Alert alert = new Alert(AlertType.CONFIRMATION);
		applyModernThemeToDialog(alert);
		alert.setTitle(LABEL_CONFIRMATION_DIALOG);
		alert.setHeaderText(LABEL_ARCHIVE_SCENARIO);
		alert.setContentText(LABEL_PLEASE_CONFIRM_ARCHIVE);

		ButtonType yes = new ButtonType("Yes");
		ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(yes, no);
		// Make the safer choice the default.
		try {
			Button noBtn = (Button) alert.getDialogPane().lookupButton(no);
			if (noBtn != null) {
				noBtn.setDefaultButton(true);
				noBtn.setCancelButton(true);
			}
		} catch (Exception e) {
			// ignore; default behavior will still treat close as NO
		}

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == yes;
	}
 
	public boolean showStatusDialog(String title, String header, String content) {
		if (title == null || header == null || content == null)
			return false;
		Alert alert = new Alert(AlertType.INFORMATION);
		applyModernThemeToDialog(alert);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		Optional<ButtonType> result = alert.showAndWait();
		return !(result.isPresent() && result.get() == ButtonType.CANCEL);
	}

	public boolean selectYesOrNoDialog(String s) {
		boolean b = false;

		JFrame jf = new JFrame();
		jf.setAlwaysOnTop(true);

		int dialogButton = JOptionPane.YES_NO_OPTION;
		int dialogResult = JOptionPane.showConfirmDialog(jf, s, "Confirmation required", dialogButton);
		b = (dialogResult == 0);
		return b;
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
		int num_cols = data.get(0).toString().split(",").length;
		int num_rows = data.size();
		String[][] dataMatrix = new String[num_rows][num_cols];
		for (int r = 0; r < num_rows; r++) {
			dataMatrix[r] = data.get(r).toString().split(",");
		}
		return dataMatrix;
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
			TableUtils.installCopyPasteHandler(table);
			table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			String[][] rawData = getDataMatrixFromArrayList(csvData);
			int numCols = computeMaxRowLength(rawData);

			Class<?>[] types = new Class<?>[numCols];

			for (int columnIndex = 0; columnIndex < numCols; columnIndex++) {
				String[] column = extractColumn(rawData, columnIndex);
				types[columnIndex] = deduceColumnType(column);
				table.getColumns().add(createColumn(types[columnIndex], columnIndex, rawData[0][columnIndex]));
			}
			;
			for (int rowIndex = 1; rowIndex < rawData.length; rowIndex++) {
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

	private Class<?> deduceColumnType(String[] column) {
		/**
		 * Deduces the data type of a column based on its values.
		 * 
		 * @param column The column as a String array
		 * @return The deduced Class type (Integer.class, Double.class, or String.class)
		 */
		boolean allInts = true;
		boolean allDoubles = true;
		try {
			for (int i = 1; i < column.length; i++) {
				String str = column[i];
				if ((str != null) && (!str.equals(""))) {
					allInts = allInts && intPattern.matcher(str).matches();
					allDoubles = allDoubles && doublePattern.matcher(str).matches();
				}
			}
			if (allInts) {
				return Integer.class;
			}
			if (allDoubles) {
				return Double.class;
			}
		} catch (Exception e) {
			;
		}
		return String.class;
	}

	private int computeMaxRowLength(Object[][] array) {
		/**
		 * Computes the maximum row length in a 2D array.
		 * 
		 * @param array The 2D array
		 * @return The maximum row length
		 */
		int maxLength = Integer.MIN_VALUE;
		for (int i = 0; i < array.length; i++) {
			if (array[i].length > maxLength) {
				maxLength = array[i].length;
			}
		}
		return maxLength;
	}

	private String[] extractColumn(String[][] data, int columnIndex) {
		/**
		 * Extracts a column from a 2D String array.
		 * 
		 * @param data        The 2D String array
		 * @param columnIndex The index of the column to extract
		 * @return The extracted column as a String array
		 */
		String[] column = new String[data.length];
		for (int i = 0; i < data.length; i++) {
			if (columnIndex < data[i].length) {
				column[i] = data[i][columnIndex];
			} else {
				column[i] = "";
			}
		}
		return column;
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
		boolean b = false;

//		if (isState(region))
//			region = "USA";

		String[][] data = getTrnDataForProcessing(sector);

		int match_row = -1;

		boolean old_format = isOldFormatTrnVehInfo(data);

		int param_col = 0;
		if (old_format)
			param_col = -1;
		int region_col = param_col + 1;
		int sector_col = region_col + 1;
		int subsector_col = sector_col + 1;
		for (int j = 0; j < data.length; j++) {
			String data_region = data[j][region_col];
			String data_subsector = data[j][subsector_col];
			if ((region.equals(data_region)) && (subsector.equals(data_subsector))) {
				match_row = j;
				break;
			}
		}

		if (match_row > -1)
			b = true;
		return b;
	}

	/**
	 * Retrieves the load factor for a transportation technology in a specific
	 * region, sector, and year.
	 * 
	 * @param region    Region name
	 * @param sector    Sector name
	 * @param subsector Subsector name
	 * @param tech      Technology name
	 * @param year      Year as string
	 * @return Load factor as string
	 */
	public String getLoadFactor(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("load", region, sector, subsector, tech, year);
	}

	/**
	 * Retrieves the vehicle coefficient for a transportation technology in a
	 * specific region, sector, and year.
	 * 
	 * @param region    Region name
	 * @param sector    Sector name
	 * @param subsector Subsector name
	 * @param tech      Technology name
	 * @param year      Year as string
	 * @return Vehicle coefficient as string
	 */
	public String getVehCoefficient(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("coefficient", region, sector, subsector, tech, year);
	}

	private String[][] getTrnDataForProcessing(String sector) {

		String[][] data;

		if (ldv4W_table == null)
			loadTrnVehInfo();

		if (sector.indexOf("4W") >= 0) {
			data = ldv4W_table;
		} else if (sector.indexOf("LDV") >= 0) {
			data = ldv2W_table;
		} else if (sector.indexOf("freight_road") >= 0) {
			data = hdv_table;
		} else {
			data = oth_table;
		}
		return data;
	}

	/**
	 * Retrieves the technology names for a given subsector in a region.
	 * 
	 * @param region    Region name
	 * @param sector    Sector name
	 * @param subsector Subsector name
	 * @return Array of technology names
	 */
	public String[] getTrnTechsInSubsector(String region, String sector, String subsector) {

		region = region.toLowerCase();
		sector = sector.toLowerCase();
		subsector = subsector.toLowerCase();

		String[][] data = getTrnDataForProcessing(sector);

		boolean old_format = isOldFormatTrnVehInfo(data);

		if (old_format) {
			System.out.println("TrnVehInfoData file is not in correct format to support CAFE.");
			return null;
		}

		int param_col = 0;
		int region_col = param_col + 1;
		int sector_col = region_col + 1;
		int subsector_col = sector_col + 1;
		int tech_col = subsector_col + 1;

		ArrayList<String> list = new ArrayList<String>();

		for (int j = 1; j < data.length; j++) {
			if (data[j][region_col].toLowerCase().equals(region)) {
				if (data[j][sector_col].toLowerCase().equals(sector)) {
					if (data[j][subsector_col].toLowerCase().equals(subsector)) {
						if ((data[j][param_col]).toLowerCase().trim().startsWith("load")) {
							list = addToArrayListIfUnique(list, data[j][tech_col]);
						}
					}
				}
			}
		}

		String[] tech_list = createStringArrayFromArrayList(list);

		return tech_list;
	}

	/**
	 * Retrieves transportation vehicle information for a given parameter, region,
	 * sector, subsector, technology, and year.
	 * 
	 * @param param     Parameter name
	 * @param region    Region name
	 * @param sector    Sector name
	 * @param subsector Subsector name
	 * @param tech      Technology name
	 * @param year_str  Year as string
	 * @return Parameter value as string
	 */
	public String getTrnVehInfo(String param, String region, String sector, String subsector, String tech,
			String year_str) {
		String val = null;

		String[][] data = getTrnDataForProcessing(sector);

		param = param.toLowerCase();

		try {
			// test to see if file is in old format (e.g., no variable as first column)
			boolean old_format = isOldFormatTrnVehInfo(data);

			int param_col = 0;
			if (old_format)
				param_col = -1;
			int region_col = param_col + 1;
			int sector_col = region_col + 1;
			int subsector_col = sector_col + 1;
			int tech_col = subsector_col + 1;

			int year_col = -1;
			for (int i = 0; i < data[0].length; i++) {
				String cmp_str = data[0][i].trim();
				if (year_str.equals(cmp_str)) {
					year_col = i;
					break;
				}
			}

			int match_row = -1;

			if (year_col > -1) {
				for (int j = 1; j < data.length; j++) {
					String temp = data[j][0];
					if (((old_format) && ("load".equals(param))) || (temp.toLowerCase().trim().startsWith(param))) {
						String data_region = data[j][region_col].trim();
						String data_subsector = data[j][subsector_col].trim();
						String data_tech = data[j][tech_col].trim();
						if ((region.equals(data_region)) && (subsector.equals(data_subsector))) {
							if (("load".equals(param)) || ((!"load".equals(param)) && (tech.equals(data_tech)))) {

								match_row = j;
								break;
							}
						}
					}
				}

				if (match_row > -1) {
					val = data[match_row][year_col];
				}
			}
		} catch (Exception e) {
			System.out.println("Error reading transportation input file. Please check format. Exception: " + e);
			val = null;
		}
		if (val == null)
			System.out.println("Problem finding " + param + " for " + sector + " / " + subsector);
		return val;
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
	 * Loads transportation vehicle information from a file and categorizes it into
	 * different tables based on vehicle type.
	 */
	public void loadTrnVehInfo() {
		if (vars == null || files == null)
			return;
		String filename = vars.getTrnVehInfoFilename();
		System.out.println("Loading transportation info from " + filename);

		try {

			ArrayList<String> contents = files.getStringArrayFromFile(filename, "#");
			ArrayList<String> ldv2w = new ArrayList<String>();
			ArrayList<String> ldv4w = new ArrayList<String>();
			ArrayList<String> hdv = new ArrayList<String>();
			ArrayList<String> other = new ArrayList<String>();
			ldv2w.add(contents.get(0));
			ldv4w.add(contents.get(0));
			hdv.add(contents.get(0));
			other.add(contents.get(0));

			for (int i = 1; i < contents.size(); i++) {
				String str = contents.get(i);
				if (str.indexOf("4W") >= 0) {
					ldv4w.add(str);
				} else if (str.indexOf("2W") >= 0) {
					ldv2w.add(str);
				} else if (str.indexOf("trn_freight_road") >= 0) {
					hdv.add(str);
				} else {
					other.add(str);
				}
			}

			ldv4W_table = getDataMatrixFromArrayList(ldv4w);
			ldv2W_table = getDataMatrixFromArrayList(ldv2w);
			hdv_table = getDataMatrixFromArrayList(hdv);
			oth_table = getDataMatrixFromArrayList(other);
		} catch (Exception e) {
			System.out.println("Problem reading transportation technology load data from " + filename + ": " + e);
		}
	}

	/**
	 * Generates a detailed error report from the main log file.
	 * 
	 * @param main_log_file Path to main log file
	 * @param scenario      Scenario name
	 * @return List of error report lines
	 */
	public ArrayList<String> generateErrorReport(String main_log_file, String scenario) {
		if (files == null || vars == null)
			return new ArrayList<>();
		DecimalFormat formatter = new DecimalFormat("#,###.0");
		double min_dmd = 0.0001;
		double min_red = 0.01;
		int total_fails = 0, minor_fails = 0, min_smallmkt_fails = 0, major_fails = 0, moderate_fails = 0,
				maj_smallmkt_fails = 0;
		if (scenario == null)
			scenario = "exe/main_log.txt";
		ArrayList<String> report = new ArrayList<>();
		File mainlogfile = new File(main_log_file);
		if (mainlogfile.exists()) {
			String[] str = { "ERROR", "SEVERE", "Period" };
			ArrayList<String> error_lines = files.getStringArrayWithPrefix(mainlogfile.getPath(), str);
			for (String errorLine : error_lines) {
				String line = scenario + ":" + errorLine.replace(":", ",");
				try {
					String right = null;
					if (line.contains(":")) {
						right = line.substring(line.indexOf(":") + 1);
						String[] tokens = right.split(",");
						if (tokens.length > 9) {
							double red = Double.parseDouble(tokens[7].trim());
							String mkt = tokens[12].trim();
							String mktyp = tokens[11].trim();
							total_fails++;
							if ((red > min_red) && (!mkt.contains("water consumption"))) {
								String state = mkt.trim().substring(0, 2);
								if (isState(state) || "US".equals(state)) {
								}
								if (mkt.toLowerCase().contains("grid")) {
								}
								if (mkt.toLowerCase().contains("water")) {
								}
								if (mkt.toLowerCase().contains("trial")) {
								}
								if (mktyp.equals("RES")) {
								}
								double dmd = Double.parseDouble(tokens[9].trim());
								if (dmd <= min_dmd)
									maj_smallmkt_fails++;
								if (red > min_red * 5.0) {
									line += " *** MAJOR (" + formatter.format(red * 100.) + "%>"
											+ formatter.format(min_red * 5.0 * 100.) + "%) ***";
									major_fails++;
								} else {
									line += " *** MODERATE (" + formatter.format(red * 100.) + "% is >" + min_red * 100.
											+ " and <" + formatter.format(min_red * 5.0 * 100.) + "%) ***";
									moderate_fails++;
								}
							} else {
								minor_fails++;
								String state = mkt.trim().substring(0, 2);
								if (isState(state) || "US".equals(state)) {
								}
								if (mkt.toLowerCase().contains("water")) {
								}
								if (mkt.toLowerCase().contains("trial")) {
								}
								if (mktyp.equals("RES")) {
								}
								double dmd = Double.parseDouble(tokens[9].trim());
								if (dmd <= min_dmd)
									min_smallmkt_fails++;
							}
						}
					}
				} catch (Exception e) {
					// Ignore parse errors for robustness
				}
				report.add(line);
			}
			if (total_fails > 0) {
				report.add("------------------------------");
				String rtn_str = "Evaluation:" + vars.getEol() + "Total errors=" + total_fails + vars.getEol()
						+ "Major errors (>" + formatter.format(min_red * 5.0 * 100.0) + "%)=" + major_fails
						+ vars.getEol() + "Moderate errors (>" + formatter.format(min_red * 100.0) + "%)="
						+ moderate_fails + vars.getEol() + "Small market errors (DMD<" + min_dmd + ")="
						+ (maj_smallmkt_fails + min_smallmkt_fails) + vars.getEol() + ">>>";
				if (total_fails == 0) {
					rtn_str += "Verdict: Pass (no errors)";
				} else if (total_fails == minor_fails) {
					rtn_str += "Verdict: Pass? (all errors are minor)";
				} else if (total_fails == minor_fails + moderate_fails) {
					rtn_str += "Verdict: Pass? (all errors are minor or moderate)";
				} else {
					if (total_fails == maj_smallmkt_fails + min_smallmkt_fails) {
						rtn_str += "Verdict: Pass? (all fails are in small markets)";
					} else if (total_fails == maj_smallmkt_fails + minor_fails) {
						rtn_str += "Verdict: Pass? (all fails are minor or in small markets)";
					} else {
						rtn_str += "Verdict: Fail? (major, non-small market failures)";
					}
				}
				rtn_str += vars.getEol();
				report.add(rtn_str);
				report.add("------------------------------");
			}
		}
		return report;
	}

	/**
	 * Removes interior quotes from a string and wraps with quotes.
	 * 
	 * @param orig Input string
	 * @return Modified string
	 */
	public String correctInteriorQuotes(String orig) {
		if (orig == null)
			return null;
		return '"' + orig.replaceAll("\"", "") + '"';
	}

	/**
	 * Processes error lines and summarizes error types.
	 * 
	 * @param errors  List of error lines
	 * @param min_red Minimum RED value
	 * @return Summary string
	 */
	public String processErrors(ArrayList<String> errors, double min_red) {
		if (vars == null)
			return "";
		if (errors == null)
			return "";
		String rtn_str = "";
		double min_dmd = 0.0001;
		int total_fails = 0, minor_fails = 0, min_us_fails = 0, min_smallmkt_fails = 0, min_res_type_fails = 0,
				major_fails = 0, maj_us_fails = 0, maj_smallmkt_fails = 0, maj_res_type_fails = 0;
		for (String line : errors) {
			try {
				String right = null;
				if (line.contains(":")) {
					right = line.substring(line.indexOf(":") + 1);
					String[] tokens = right.split(",");
					if (tokens.length > 9) {
						double red = Double.parseDouble(tokens[7].trim());
						String mkt = tokens[12].trim();
						String mktyp = tokens[11].trim();
						total_fails++;
						if ((red > min_red) && (!mkt.contains("water consumption"))) {
							major_fails++;
							String state = mkt.trim().substring(0, 2);
							if (isState(state) || "US".equals(state))
								maj_us_fails++;
							if (mkt.toLowerCase().contains("grid")) {
							}
							if (mkt.toLowerCase().contains("water")) {
							}
							if (mkt.toLowerCase().contains("trial")) {
							}
							if (mktyp.equals("RES"))
								maj_res_type_fails++;
							double dmd = Double.parseDouble(tokens[9].trim());
							if (dmd <= min_dmd)
								maj_smallmkt_fails++;
						} else {
							minor_fails++;
							String state = mkt.trim().substring(0, 2);
							if (isState(state) || "US".equals(state))
								min_us_fails++;
							if (mkt.toLowerCase().contains("water")) {
							}
							if (mkt.toLowerCase().contains("trial")) {
							}
							if (mktyp.equals("RES"))
								min_res_type_fails++;
							double dmd = Double.parseDouble(tokens[9].trim());
							if (dmd <= min_dmd)
								min_smallmkt_fails++;
						}
					}
				}
			} catch (Exception e) {
				// Ignore parse errors for robustness
			}
		}
		if (total_fails > 0) {
			if (total_fails == minor_fails) {
				rtn_str = "Pass (all minor: RED<" + min_red + ")";
			} else if (total_fails == maj_smallmkt_fails + min_smallmkt_fails) {
				rtn_str = "Pass (all major are small market: DMD<" + min_dmd + ")";
			} else if (total_fails == maj_smallmkt_fails + minor_fails) {
				rtn_str = "Pass (all are major+small market or minor)";
			} else if (total_fails == maj_res_type_fails + min_res_type_fails) {
				rtn_str = "All failures are of type RES";
			} else {
				rtn_str = "Fail";
			}
			rtn_str += "... Major (RED>" + min_red + "): " + major_fails + ", of which " + maj_res_type_fails
					+ " are type RES, " + maj_us_fails + " are in US, " + maj_smallmkt_fails + " are small mkt; Minor: "
					+ minor_fails + ", of which " + min_res_type_fails + " are type RES, " + min_us_fails
					+ " are in US, " + min_smallmkt_fails + " are small mkt." + vars.getEol();
		}
		return rtn_str;
	}

	/**
	 * Retrieves the scenario name from the main log file.
	 * 
	 * @param current_main_log_file Main log file
	 * @return Scenario name as string
	 */
	public String getRunningScenario(File current_main_log_file) {
		if (current_main_log_file == null)
			return "";
		String rtn_str = "";

		if (current_main_log_file.exists()) {
			rtn_str = this.getScenarioNameFromMainLog(current_main_log_file);
		}

		return rtn_str;
	}

	/**
	 * Extracts the scenario name from the main log file.
	 * 
	 * @param file Main log file
	 * @return Scenario name as string
	 */
	public String getScenarioNameFromMainLog(File file) {
		if (file == null)
			return "";
		String rtn_str = "";
		try (Scanner fileScanner = new Scanner(file)) {
			boolean stop_recording = false;
			while ((fileScanner.hasNext()) && !stop_recording) {
				String line = fileScanner.nextLine().trim();
				if (line.startsWith("Configuration file: ")) {
					try {
						line = line.substring(line.indexOf(":") + 1).trim();
						File f = new File(line);
						if (f.exists())
							rtn_str = f.getParentFile().getName();
					} catch (Exception e) {
						rtn_str = "";
					}
					stop_recording = true;
				}
			}
		} catch (Exception e) {
			System.out.println("Problem reading components from " + file.getName() + ": " + e);
		}
		return rtn_str;
	}

	/**
	 * Retrieves the status of a scenario from the main log file.
	 * 
	 * @param file Main log file
	 * @return Status string
	 */
	public String getScenarioStatusFromMainLog(File file) {
		if (file == null)
			return "";
		boolean has_err = false;
		String status = "";
		String errors = "";
		String current_period = "";
		boolean new_period = true;
		try (Scanner fileScanner = new Scanner(file)) {
			boolean stop_recording = false;
			while ((fileScanner.hasNext()) && !stop_recording) {
				String line = fileScanner.nextLine().trim();
				if (line.startsWith("Period ")) {
					try {
						current_period = line.substring(7, line.indexOf(":"));
						status = current_period;
						new_period = true;
					} catch (Exception e) {
						status = "?";
					}
				}
				if (line.startsWith("ERROR:X")) {
					has_err = true;
					if (new_period) {
						if (!errors.isEmpty())
							errors += ",";
						errors += current_period;
						new_period = false;
					}
				}

				if (line.startsWith("Model run completed")) {
					status = "Finishing";
				}
			}
			if (has_err)
				status += ",ERR" + errors;
		} catch (Exception e) {
			System.out.println("Problem reading components from " + file.getName() + ": " + e);
		}

		return status;
	}

	public void fixLostHandle() {
		if (vars == null || files == null)
			return;
		String main_log_filename = vars.getgCamExecutableDir() + File.separator + "logs" + File.separator
				+ "main_log.txt";
		String err_msg = main_log_filename + " does not exist.";
		String scenario_name = "";
		String scenario_main_log_name = "";
		File main_log_file = new File(main_log_filename);
		if (main_log_file.exists()) {
			scenario_name = this.getScenarioNameFromMainLog(main_log_file);
			if (scenario_name != "") {
				scenario_main_log_name = vars.getScenarioDir() + File.separator + scenario_name + File.separator
						+ "main_log.txt";
				files.copyFile(main_log_filename, scenario_main_log_name);
			}
		} else {
			this.showInformationDialog(LABEL_NOTICE, LABEL_CANNOT_BE_EXECUTED, err_msg);
		}
	}

	public String getComputerStatString() {

		boolean warning = false;

		String status = "";
		try {

			com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
					.getOperatingSystemMXBean();

			double gb = 1073741824;

			float physicalMemorySize = (float) (os.getTotalPhysicalMemorySize() / gb);
			float physicalMemoryFree = (float) (os.getFreePhysicalMemorySize() / gb);
			float swapSpaceSize = (float) (os.getTotalSwapSpaceSize() / gb);
			float freeSwapSpace = (float) (os.getFreeSwapSpaceSize() / gb);
			File drive = new File("/");
			float total_space = (float) (drive.getTotalSpace() / gb);
			float free_space = (float) (drive.getFreeSpace() / gb);
			float cpu_load = (float) (os.getSystemCpuLoad());

			String database_name = vars.getgCamOutputDatabase();
			String database_short_name = database_name.substring(database_name.lastIndexOf(File.separator) + 1);
			File database_folder = new File(database_name);
			Path database_path = database_folder.toPath();
			float database_size = (float) (files.getDirectorySize(database_path) / gb);
			String warning_RAM = "";
			String warning_disk = "";
			String warning_swap = "";
			String warning_db = "";

			if (physicalMemoryFree / physicalMemorySize < 0.05)
				warning_RAM = "*";
			if (freeSwapSpace / swapSpaceSize < 0.05)
				warning_swap = "*";
			if (free_space < 40.0)
				warning_disk = "*";
			if (database_size > vars.getMaxDatabaseSize() * .8)
				warning_db = "*";

			if ((physicalMemoryFree / physicalMemorySize < 0.05) || (freeSwapSpace / swapSpaceSize < 0.05)
					|| (free_space < 40.0) || (database_size > vars.getMaxDatabaseSize() * .8)) {
				warning = true;
			}

			status = "  Resources... " + "CPU: " + String.format("%,.0f", cpu_load * 100.0) + "% | " + "RAM: "
					+ String.format("%,.0f", physicalMemorySize) + "GB Free:"
					+ String.format("%,.0f", physicalMemoryFree / physicalMemorySize * 100.) + "%" + warning_RAM + " | "
					+ "HD: " + String.format("%,.0f", free_space) + "GB Free:"
					+ String.format("%,.0f", free_space / total_space * 100.) + "%" + warning_disk + " | " + "Swap: "
					+ String.format("%,.0f", swapSpaceSize) + "GB Free:"
					+ String.format("%,.0f", freeSwapSpace / swapSpaceSize * 100.0) + "%" + warning_swap + " | "
					+ "DB: " + database_short_name + " " + String.format("%,.1f", database_size) + "GB Free:"
					+ String.format("%,.0f", (1.0 - (database_size / vars.getMaxDatabaseSize())) * 100.0) + "%"
					+ warning_db;
		} catch (Exception e) {
			status = "";
		}
		if (warning)
			status = status.trim() + " !!!";

		return status;
	}

	public void resetLogFile() {
		if (vars == null || files == null)
			return;
		// replace glimpse log file with empty file
		String glimpse_log_filename = vars.getGlimpseLogDir() + File.separator + "glimpse_log.txt";
		files.deleteFile(glimpse_log_filename);
		File f = new File(glimpse_log_filename);
		files.saveFile("", f);
	}

	public void resetLogFile(String s) {
		if (vars == null || files == null)
			return;
		// replace glimpse log file with empty file
		String glimpse_log_filename = vars.getGlimpseLogDir() + File.separator + "glimpse_log.txt";
		String glimpse_log_prior_filename = vars.getGlimpseLogDir() + File.separator + "glimpse_log_prior.txt";
		files.deleteFile(glimpse_log_prior_filename);
		files.copyFile(glimpse_log_filename, glimpse_log_prior_filename);
		files.deleteFile(glimpse_log_filename);

		File f = new File(glimpse_log_filename);
		files.saveFile("", f);

		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		String time = formatter.format(date);

		String msg = "Information at startup " + time + ":" + vars.getEol() + s.trim() + vars.getEol() + vars.getEol()
				+ "--- Resource warnings during current session follow: ---" + vars.getEol();
		files.appendTextToFile(msg, glimpse_log_filename);
	}

	public String getParentheticString(String orig) {
		String rtn_str = orig;

		try {
			String s = orig.substring((orig.indexOf("(") + 1), orig.indexOf(")"));
			rtn_str = s;
		} catch (Exception e) {
			;
		}
		return rtn_str;
	}

	public boolean hasSpecialCharacter(String s) {
		if (s == null || s.trim().isEmpty()) {
			System.out.println("Incorrect format of string");
			return false;
		}
		Pattern special = Pattern.compile("[!@#$%&*()+=|<>?{}\\[\\]~]");
		Matcher m = special.matcher(s);
		boolean b = m.find();
		return b;
	}

	/**
	 * Checks if the transportation vehicle info data is in old format.
	 * 
	 * @param data Transportation vehicle info data matrix
	 * @return true if old format, false otherwise
	 */
	private boolean isOldFormatTrnVehInfo(String[][] data) {
		/**
		 * Checks if the transportation vehicle info data is in old format.
		 * 
		 * @param data Transportation vehicle info data matrix
		 * @return true if old format, false otherwise
		 */
		boolean old_format = false;
		if ((data[0][0] != null) && (data[0][0].toLowerCase().trim().startsWith("parameter"))) {
			old_format = false;
		}
		return old_format;
	}

	/**
	 * Helper method to check if a string is null or empty.
	 * 
	 * @param s String to check
	 * @return true if null or empty, false otherwise
	 */
	private boolean isNullOrEmpty(String s) {
		/**
		 * Helper method to check if a string is null or empty.
		 * 
		 * @param s String to check
		 * @return true if null or empty, false otherwise
		 */
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Creates a JavaFX ComboBox<String> and populates it with the provided options.
	 * 
	 * @param convertFromOptions Array of string options to add to the ComboBox
	 * @return ComboBox<String> instance with the given options
	 */
	public ComboBox<String> createComboBoxString(String[] convertFromOptions) {
		ComboBox<String> comboBox = new ComboBox<>();
		if (convertFromOptions != null) {
			for (String option : convertFromOptions) {
				if (!isNullOrEmpty(option)) {
					comboBox.getItems().add(option);
				}
			}
		}
		return comboBox;
	}

	/**
	 * Concatenates elements of an ArrayList<String> into a single string separated
	 * by the given separator.
	 * 
	 * @param ol        ArrayList<String> to concatenate
	 * @param separator Separator string
	 * @return Concatenated string
	 */
	public String getStringFromList(ArrayList<String> ol, String separator) {
		if (ol == null || separator == null || vars == null)
			return "";
		StringBuilder rtn_str = new StringBuilder();
		for (String o : ol) {
			rtn_str.append(o).append(separator);
		}
		return rtn_str.toString();
	}

	/**
	 * Creates a new JavaFX ComboBox<String> instance.
	 * 
	 * @return ComboBox<String> instance
	 */
	public ComboBox<String> createComboBox() {
		ComboBox<String> comboBox = new ComboBox<>();
		return comboBox;
	}

	/**
	 * Converts a list of integers to a string array.
	 * 
	 * @param allowablePolicyYears List of integers to convert
	 * @return Array of strings representing the integers
	 */
	public String[] createStringArrayFromListOfIntegers(List<Integer> allowablePolicyYears) {
		String[] str_array = new String[allowablePolicyYears.size()];
		for (int i = 0; i < allowablePolicyYears.size(); i++) {
			str_array[i] = Integer.toString(allowablePolicyYears.get(i));
		}
		return str_array;
	}

	/**
	 * Splits the given string using the specified delimiter and returns the result
	 * as a List<String>.
	 *
	 * @param val       the input string to split
	 * @param delimeter the delimiter to use for splitting the string
	 * @return a List<String> containing the split elements; empty list if input or
	 *         delimiter is null
	 */
	public List<String> getStringListFromString(String val, String delimeter) {
		List<String> rtn_list = new ArrayList<>();
		if (val == null || delimeter == null)
			return rtn_list;
		String[] parts = val.split(delimeter);
		for (String part : parts) {
			rtn_list.add(part);
		}
		return rtn_list;
	}

	/**
	 * Creates a JavaFX ComboBox<String> with the specified preferred width.
	 *
	 * @param prefWidth the preferred width of the ComboBox
	 * @return a ComboBox<String> instance with the given preferred width
	 */
	public ComboBox<String> createComboBoxString(double prefWidth) {
		ComboBox<String> comboBox = new ComboBox<>();
		comboBox.setPrefWidth(prefWidth);
		return comboBox;
	}

	/**
	 * Creates a ControlsFX CheckComboBox<String> with the specified preferred
	 * width.
	 *
	 * @param prefWidth the preferred width of the CheckComboBox
	 * @return a CheckComboBox<String> instance with the given preferred width
	 */
	public CheckComboBox<String> createCheckComboBox(double prefWidth) {
		CheckComboBox<String> checkComboBox = new CheckComboBox<>();
		checkComboBox.setPrefWidth(prefWidth);
		return checkComboBox;
	}

	/**
	 * Creates a JavaFX Label with the specified preferred width.
	 *
	 * @param labelWidth the preferred width of the Label
	 * @return a Label instance with the given preferred width
	 */
	public Label createLabel(double labelWidth) {
		Label label = new Label();
		label.setPrefWidth(labelWidth);
		return label;
	}

	/**
	 * Extracts and returns the text between parentheses in the given string.
	 *
	 * @param policyType the input string containing parentheses
	 * @return the text found between the first '(' and ')', or the original string
	 *         if not found
	 */
	public String getTextBetweenParen(String policyType) {
		if (policyType == null)
			return null;
		if ((policyType.indexOf("(") >= 0) || (policyType.indexOf(")") >= 0)) {
			int start = policyType.indexOf("(");
			int end = policyType.indexOf(")");
			if (end > start) {
				policyType = policyType.substring(start + 1, end).trim();
			}
		}
		return policyType;
	}

	/**
	 * Generates a side-by-side diff (line aligned) using java-diff-utils {@link DiffRowGenerator}.
	 *
	 * <p>Intended for UI display (TableView) rather than unified diff text output.</p>
	 *
	 * @param file1 original file
	 * @param file2 new file
	 * @return list of DiffLineRow objects suitable for rendering
	 */
	public java.util.List<DiffLineRow> generateSideBySideDiffRows(String file1, String file2) {
		java.util.List<DiffLineRow> out = new java.util.ArrayList<>();
		if (files == null) {
			return out;
		}

		java.util.ArrayList<String> file1Content = files.getStringArrayFromFile(file1, "#");
		java.util.ArrayList<String> file2Content = files.getStringArrayFromFile(file2, "#");
		if (file1Content == null) file1Content = new java.util.ArrayList<>();
		if (file2Content == null) file2Content = new java.util.ArrayList<>();

		try {
			DiffRowGenerator generator = DiffRowGenerator.create()
					.showInlineDiffs(true)
					.inlineDiffByWord(true)
					.oldTag(f -> "")
					.newTag(f -> "")
					.build();

			java.util.List<DiffRow> rows = generator.generateDiffRows(file1Content, file2Content);

			int oldLine = 0;
			int newLine = 0;
			for (DiffRow r : rows) {
				if (r == null) continue;
				DiffRow.Tag tag = r.getTag();

				String oldText = r.getOldLine();
				String newText = r.getNewLine();

				int oldNum = 0;
				int newNum = 0;
				switch (tag) {
				case INSERT:
					newNum = ++newLine;
					break;
				case DELETE:
					oldNum = ++oldLine;
					break;
				case CHANGE:
					oldNum = ++oldLine;
					newNum = ++newLine;
					break;
				case EQUAL:
				default:
					oldNum = ++oldLine;
					newNum = ++newLine;
					break;
				}

				out.add(new DiffLineRow(oldNum, newNum, oldText, newText, tag));
			}

		} catch (Exception e) {
			// Best-effort: return a minimal message row.
			out.clear();
			out.add(new DiffLineRow(0, 0, "Diff failed:", String.valueOf(e), com.github.difflib.text.DiffRow.Tag.CHANGE));
		}

		return out;
	}

}
