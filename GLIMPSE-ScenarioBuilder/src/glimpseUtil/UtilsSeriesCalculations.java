package glimpseUtil;

/**
 * Series calculation helper methods extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsSeriesCalculations {

	private UtilsSeriesCalculations() {
	}

	public static String[] convertTo1990Dollars(String[] vals, String dollarYear, GLIMPSEFiles files) {
		if (files == null)
			return vals;
		String[] retVals = vals;
		String conversionStr = "1.0";

		try {
			for (int i = 0; i < files.getMonetaryConversionsFileContent().size(); i++) {
				String s[] = files.getMonetaryConversionsFileContent().get(i).split(",");
				if (s[0].equals(dollarYear))
					conversionStr = s[1];
			}

			double conversionDbl = Double.parseDouble(conversionStr);

			for (int i = 0; i < vals.length; i++) {
				double val = Double.parseDouble(vals[i]) * conversionDbl;
				retVals[i] = "" + val;
			}
		} catch (Exception e) {
			System.out.println("Error making dollar year conversion. Returning original values.");
		}

		return retVals;
	}

	public static double[][] calculateValues(String type, int start_year, int end_year, double initial_value,
			double growth, int period_length, double factor) {
		double[][] array = calculateValues(type, start_year, end_year, initial_value, growth, period_length);

		for (int i = 0; i < array[0].length; i++) {
			array[1][i] = array[1][i] * factor;
		}

		return array;
	}

	public static double[][] calculateValues(String type, int start_year, int end_year, double initial_value,
			double growth, int period_length) {
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
					final_value = growth;
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			case "Initial and Final %":
				if (t == 0) {
					initial_value /= 100;
					final_value = growth / 100;
				}
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			}
			returnMatrix[0][t] = year;
			returnMatrix[1][t] = val;
		}

		return returnMatrix;
	}

	public static double[][] calculateValues(String type, boolean isPercent, int start_year, int end_year,
			double initial_value, double growth, int period_length, double factor) {
		double[][] array = calculateValues(type, isPercent, start_year, end_year, initial_value, growth, period_length);

		for (int i = 0; i < array[0].length; i++) {
			array[1][i] = array[1][i] * factor;
		}

		return array;
	}

	public static double[][] calculateValues(String type, boolean isPercent, int start_year, int end_year,
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
					final_value = growth;
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			case "Initial and Final %":
				if (t == 0) {
					initial_value /= 100;
					final_value = growth / 100;
				}
				val = (final_value - initial_value) / (num_periods - 1) * t + initial_value;
				break;
			}
			returnMatrix[0][t] = year;
			returnMatrix[1][t] = val;
		}

		return returnMatrix;
	}
}