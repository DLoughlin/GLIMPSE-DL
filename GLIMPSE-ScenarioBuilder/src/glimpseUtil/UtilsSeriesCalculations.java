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
}
