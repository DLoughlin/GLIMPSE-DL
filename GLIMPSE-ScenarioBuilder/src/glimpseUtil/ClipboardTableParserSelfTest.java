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
 * and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 *
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 */
package glimpseUtil;

import java.util.List;

/**
 * Lightweight self-test (no JUnit dependency) for ClipboardTableParser.
 *
 * Run manually if needed; build scripts typically compile it anyway.
 */
public class ClipboardTableParserSelfTest {

	public static void main(String[] args) {
		test("2020\t1.0\n2025\t2.0", 2, 2);
		test("2020,1.0\n2025,2.0", 2, 2);
		test("1.0\n2.0\n3.0", 3, 1);
		test("2020\n2025\n2030", 3, 1);
		test("\n\n  \n2020\t1\n\n", 1, 2);

		// row-selection style (wide, short)
		testTransposeSingleRow();
		testPairTwoRows();

		System.out.println("ClipboardTableParserSelfTest: OK");
	}

	private static void test(String input, int expectedRows, int expectedMaxCols) {
		List<List<String>> grid = ClipboardTableParser.parseGrid(input);
		int maxCols = ClipboardTableParser.maxColumns(grid);
		if (grid.size() != expectedRows || maxCols != expectedMaxCols) {
			throw new RuntimeException("Expected rows=" + expectedRows + " maxCols=" + expectedMaxCols +
					" but got rows=" + grid.size() + " maxCols=" + maxCols + " for input: " + input);
		}
	}

	private static void testTransposeSingleRow() {
		List<java.util.List<String>> out = ClipboardTableParser.transposeSingleRowToColumn(
				java.util.Arrays.asList("a", "b", "c"));
		if (out.size() != 3 || out.get(0).size() != 1 || !"b".equals(out.get(1).get(0))) {
			throw new RuntimeException("transposeSingleRowToColumn failed: " + out);
		}
	}

	private static void testPairTwoRows() {
		List<java.util.List<String>> out = ClipboardTableParser.pairRowsToTwoColumns(
				java.util.Arrays.asList("2020", "2025"),
				java.util.Arrays.asList("1.0", "2.0"));
		if (out.size() != 2 || out.get(0).size() != 2 || !"2025".equals(out.get(1).get(0)) || !"2.0".equals(out.get(1).get(1))) {
			throw new RuntimeException("pairRowsToTwoColumns failed: " + out);
		}
	}
}