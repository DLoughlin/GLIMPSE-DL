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

/**
 * Tiny debug helper to silence ad-hoc diagnostics by default.
 *
 * Enable by passing a JVM system property:
 * <pre>
 *   -Dglimpse.debug=true
 * </pre>
 */
public final class Debug {

	/** System property name used to enable debug prints. */
	public static final String PROP_NAME = "glimpse.debug";

	/**
	 * Global debug toggle.
	 *
	 * Defaults to false. Can be enabled by setting {@code -Dglimpse.debug=true}.
	 */
	public static final boolean ENABLED = Boolean.parseBoolean(System.getProperty(PROP_NAME, "false"));

	private Debug() {
		// no instances
	}

	public static void log(String message) {
		if (!ENABLED) return;
		System.out.println(message);
	}

	public static void log(String message, Throwable t) {
		if (!ENABLED) return;
		System.out.println(message);
		if (t != null) {
			t.printStackTrace(System.out);
		}
	}
}
