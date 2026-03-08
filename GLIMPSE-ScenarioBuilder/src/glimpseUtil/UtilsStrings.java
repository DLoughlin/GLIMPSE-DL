package glimpseUtil;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;

/**
 * String and collection helper methods extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsStrings {

	private UtilsStrings() {
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
}
