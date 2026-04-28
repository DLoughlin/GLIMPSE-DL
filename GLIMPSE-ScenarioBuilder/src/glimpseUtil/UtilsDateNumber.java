package glimpseUtil;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Date and numeric helper methods extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsDateNumber {

	private UtilsDateNumber() {
	}

	/**
	 * Returns the current date formatted as {@code yyyy-MM-dd}.
	 *
	 * @return current date string
	 */
	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		Date now = new Date();
		return sdfDate.format(now);
	}

	/**
	 * Parses a date string using the supplied format pattern.
	 *
	 * @param dateStr date string to parse
	 * @param dateFormatStr format pattern to apply
	 * @return parsed date, or {@code null} when parsing fails
	 */
	public static Date getFormattedDate(String dateStr, String dateFormatStr) {
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
	 * Converts a string to an integer after removing simple quote characters.
	 *
	 * @param s string to convert
	 * @return parsed integer, or {@code 0} when conversion fails
	 */
	public static int convertStringToInt(String s) {
		if (s == null)
			return 0;
		s = s.replaceAll("\"", "").replaceAll("'", "");
		int rtnVal = 0;
		try {
			rtnVal = Integer.parseInt(s);
		} catch (Exception e) {
			System.out.println("problem converting " + s + " to int: " + e);
		}
		return rtnVal;
	}

	/**
	 * Converts a model period index to its corresponding year string.
	 *
	 * @param period model period index
	 * @return year label for the period
	 */
	public static String getYearForPeriod(int period) {
		String rtnStr = "";

		if (period == -1) {
			rtnStr = "2100";
		} else if (period == 0) {
			rtnStr = "1975";
		} else if (period == 1) {
			rtnStr = "1990";
		} else if (period == 2) {
			rtnStr = "2005";
		} else {
			rtnStr = 2005 + 5 * (period - 2) + "";
		}
		return rtnStr;
	}

	/**
	 * Converts a year string to the associated model period index.
	 *
	 * @param year year string to convert
	 * @return period index as a string, or an empty string when invalid
	 */
	public static String getPeriodForYear(String year) {
		if (year == null)
			return "";
		double yearD = 0;
		try {
			yearD = Double.parseDouble(year);
		} catch (NumberFormatException e) {
			return "";
		}
		double increment = (yearD - 2005.) / 5. + 2;
		int incrementInt = (int) increment;
		return "" + incrementInt;
	}

	/**
	 * Formats a floating-point value to the requested number of significant figures.
	 *
	 * @param val value to format
	 * @param significantFigures number of significant figures to keep
	 * @return formatted numeric string
	 */
	public static String toSignificantFiguresString(double val, int significantFigures) {
		BigDecimal bd = new BigDecimal(val);
		String test = String.format("%." + significantFigures + "G", bd);
		if (test.contains("E+")) {
			test = String.format(Locale.US, "%.0f",
					Double.valueOf(String.format("%." + significantFigures + "G", bd)));
		}
		return test;
	}
}
