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
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/
package conversionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting between different array and list formats.
 * <p>
 * Author: TWU
 * Date: 1/2/2016
 */
public class ArrayConversion {
    /**
     * Converts a 1D String array where each element is a comma-separated string into a 2D String array.
     * @param in 1D String array
     * @return 2D String array
     */
    public static String[][] array1to2Conversion(String[] in) {
        String[][] out = new String[in.length][in[0].split(",").length];
        for (int i = 0; i < in.length; i++) {
            // Split each string by comma and assign to output
            out[i] = string2Array(in[i], ",");
        }
        return out;
    }

    /**
     * Converts a 2D String array into a 1D String array, joining each row into a single comma-separated string.
     * @param in 2D String array
     * @return 1D String array
     */
    public static String[] array2to1Conversion(String[][] in) {
        String[] out = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            // Join each row into a single string
            out[i] = array2String(in[i]);
        }
        return out;
    }

    /**
     * Reverses the dimensions of a 2D String array (transposes the array).
     * @param in 2D String array
     * @return Transposed 2D String array
     */
    public static String[][] arrayDimReverse(String[][] in) {
        String[][] out = new String[in[0].length][in.length];
        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in[i].length; j++) {
                // Swap rows and columns
                out[j][i] = in[i][j];
            }
        }
        return out;
    }

    /**
     * Splits a string into an array using the specified separator.
     * @param in Input string
     * @param sep Separator (defaults to comma if null)
     * @return Array of strings
     */
    public static String[] string2Array(String in, String sep) {
        return in.split(sep == null ? "," : sep);
    }

    /**
     * Converts an ArrayList to a String array.
     * @param in ArrayList
     * @return String array
     */
    public static String[] arrayList2Array(ArrayList<?> in) {
        return in.toArray(new String[0]);
    }

    /**
     * Converts an ArrayList to an Object array.
     * @param in ArrayList
     * @param isChart Unused parameter (for compatibility)
     * @return Object array
     */
    public static Object[] arrayList2Array(ArrayList<?> in, boolean isChart) {
        return in.toArray(new Object[0]);
    }

    /**
     * Converts a List to a String array.
     * @param in List
     * @return String array
     */
    public static String[] list2Array(List<?> in) {
        return in.toArray(new String[0]);
    }

    /**
     * Joins a String array into a single comma-separated string.
     * @param in String array
     * @return Comma-separated string
     */
    public static String array2String(String[] in) {
        return String.join(",", in);
    }

    /**
     * Joins a List into a single comma-separated string.
     * @param in List
     * @return Comma-separated string
     */
    public static String list2String(List<?> in) {
        return String.join(",", in.toString().replace("[", "").replace("]", ""));
    }
}