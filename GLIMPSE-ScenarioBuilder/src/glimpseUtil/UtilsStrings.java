package glimpseUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javafx.collections.ObservableList;

/**
 * String and collection helper methods extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsStrings {

    public GLIMPSEVariables vars;
    private long orig_date = 0;
 
 	
	public UtilsStrings() {
	}

	public static boolean getMatch(String str, List<String> marketList) {
		if (str == null || marketList == null)
			return false;
		for (String item : marketList) {
			if (str.equals(item)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<String> addToArrayListIfUnique(ArrayList<String> list, String str) {
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

	public static String getTokenWithText(String line, String txt, String delim) {
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

	public static String getStringFromList(ObservableList<String> ol, String separator) {
		if (ol == null || separator == null)
			return "";
		StringBuilder rtnStr = new StringBuilder();
		for (String o : ol) {
			rtnStr.append(o).append(separator);
		}
		return rtnStr.toString();
	}

	public static String getRidOfTrailingCommasInString(String s) {
		if (s == null)
			return null;
		while (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	public static String[] getRidOfTrailingCommasInStringArray(String[] s) {
		if (s == null)
			return null;
		int i = 0;
		for (String str : s) {
			s[i++] = getRidOfTrailingCommasInString(str);
		}
		return s;
	}

	public static String capitalizeOnlyFirstLetterOfString(String inputString) {
		if (inputString == null || inputString.isEmpty())
			return inputString;
		String outputString = inputString;

		if (inputString.length() == 1) {
			outputString = inputString.toUpperCase();
		}
		if (inputString.length() > 1) {
			outputString = inputString.substring(0, 1).toUpperCase() + inputString.substring(1).toLowerCase();
		}
		return outputString;
	}

	public static String[] splitString(String str, String delim) {
		if (str == null || delim == null)
			return new String[0];
		return str.split(delim);
	}

	public static ArrayList<String> createArrayListFromString(String line, String delim) {
		if (line == null || delim == null)
			return new ArrayList<>();
		ArrayList<String> linesList = new ArrayList<>();
		String[] lines = splitString(line, delim);
		for (String l : lines) {
			linesList.add(l);
		}
		return linesList;
	}

	public static String createStringFromArrayList(ArrayList<String> arrayList, String eol) {
		if (arrayList == null)
			return "";
		StringBuilder result = new StringBuilder();
		for (String s : arrayList) {
			result.append(s).append(eol);
		}
		return result.toString();
	}

	public static String createStringFromArrayList(List<String> filesToSave, String delimiter) {
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

	public static String createStringFromStringArray(String[] strArray) {
		if (strArray == null)
			return "";
		StringBuilder rtnStr = new StringBuilder();
		for (int i = 0; i < strArray.length; i++) {
			if (i > 0)
				rtnStr.append(",");
			rtnStr.append(strArray[i]);
		}
		return rtnStr.toString();
	}

	public static String trimIfExists(String str) {
		if (str != null)
			str = str.trim();
		return str;
	}

	public static boolean hasSpecialCharacter(String str) {
		if (str == null)
			return false;
		return !str.matches("[A-Za-z0-9_.-]+");
	}

	public static String getParentheticString(String s) {
		return getTextBetweenParen(s);
	}

	public static String getTextBetweenParen(String s) {
		if (s == null)
			return "";
		int start = s.indexOf('(');
		int end = s.indexOf(')', start + 1);
		if (start < 0 || end < 0 || end <= start)
			return "";
		return s.substring(start + 1, end);
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
	public String getMatchOld(ArrayList<String> list, String item, String delimiter) {
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
	public String getMatchOld(ArrayList<String> list, String item, String delimiter1, String delimiter2) {
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

	public ArrayList<String> getUniqueItemsFromStringArrayList(ArrayList<String> list) {
		ArrayList<String> resultList = new ArrayList<>();
		if (list == null)
			return resultList;
		for (String str1 : list) {
			if (str1 == null)
				continue;
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
		} else if (resultList.contains("Select One or More")) {
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
		String temp = "" + diff_sec;
		temp.substring(temp.length() - 6);

		return "" + diff_sec;
	}

	public String commentLinesInString(String stringLine, String startComment, String endComment) {
		String[] stringLines = splitEOL(stringLine);
		StringBuilder newStringLine = new StringBuilder();
		for (String line : stringLines) {
			String commented = startComment + line + endComment;
			newStringLine.append(commented);
			if (vars != null && line.indexOf(vars.getEol()) < 0)
				newStringLine.append(vars.getEol());
		}
		return newStringLine.toString();
	}

	public static String[] removeWorldRegion(String[] sOrig) {
		if (sOrig == null)
			return null;
		String[] sReturn;

		int worldLocation = -1;
		int i = 0;
		for (String s : sOrig) {
			if (s != null && s.trim().toLowerCase().equals("world")) {
				worldLocation = i;
			}
			i++;
		}

		if (worldLocation == -1) {
			sReturn = sOrig;
		} else {
			sReturn = new String[sOrig.length - 1];
			int pos = 0;
			for (int j = 0; j < sOrig.length; j++) {
				if (j != worldLocation) {
					sReturn[pos] = sOrig[j];
					pos++;
				}
			}
		}

		return sReturn;
	}

	public static String[] removeUSADuplicate(String[] sOrig) {
		if (sOrig == null)
			return null;
		String[] sReturn;

		int usaCount = 0;
		for (String s : sOrig) {
			if (s != null && s.trim().toLowerCase().equals("usa")) {
				usaCount++;
			}
		}

		if (usaCount < 2) {
			sReturn = sOrig;
		} else {
			sReturn = new String[sOrig.length - 1];
			for (int i = 0; i < sReturn.length; i++) {
				sReturn[i] = sOrig[i];
			}
		}

		return sReturn;
	}

}