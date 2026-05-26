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
package glimpseElement;

import java.math.BigDecimal;

import glimpseUtil.GLIMPSEUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DataPoint {
	private GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

	private static final BigDecimal SCI_NOTATION_THRESHOLD = new BigDecimal("0.000001");

	private StringProperty year;
	private StringProperty value;

	public DataPoint(String yr, String val) {
		setYear(yr);
		setValue(val);
	}

	public DataPoint(int yr, double val) {
		String str_yr = "" + yr;
		setYear(str_yr);
		setValue(formatNumericValue(val));
	}

	static String formatNumericValue(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return String.valueOf(value);
		}
		return formatNumericValue(BigDecimal.valueOf(value));
	}

	static String formatNumericValue(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return trimmed;
		}
		try {
			return formatNumericValue(new BigDecimal(trimmed));
		} catch (Exception firstParse) {
			try {
				return formatNumericValue(BigDecimal.valueOf(Double.parseDouble(trimmed)));
			} catch (Exception secondParse) {
				return trimmed;
			}
		}
	}

	private static String formatNumericValue(BigDecimal value) {
		if (value == null) {
			return null;
		}
		if (value.signum() == 0) {
			return "0";
		}
		BigDecimal normalized = value.stripTrailingZeros();
		if (normalized.abs().compareTo(SCI_NOTATION_THRESHOLD) < 0) {
			return normalized.toString();
		}
		return normalized.toPlainString();
	}

	public void setYear(String yr) {
		yearProperty().set(yr);
	}

	public String getYear() {
		return yearProperty().get();
	}

	public StringProperty yearProperty() {
		if (year == null)
			year = new SimpleStringProperty(this, "year");
		return year;
	}

	public void setValue(String val) {
		valueProperty().set(formatNumericValue(val));
	}

	public String getValue() {
		return valueProperty().get();
	}

	public StringProperty valueProperty() {
		if (value == null)
			value = new SimpleStringProperty(this, "value");
		return value;
	}

	public boolean qaDataPoint(boolean isCheckYear) {
		boolean ok = true;
		if (isCheckYear) {
			try {
				int year = Integer.parseInt(getYear());
				// double value = Double.parseDouble(getValue());

				if ((year < 2015) || (year > 2100))
					ok = false;

			} catch (Exception ee) {
				ok = false;
			}
		}
		if (!ok)
			utils.warningMessage("Entry must be for year from 2015 to 2100.");
		return ok;
	}

}
