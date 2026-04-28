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

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for parsing clipboard content (rows/columns) for pasting into JavaFX TableView.
 *
 * <p>
 * Goals:
 * <ul>
 *   <li>Accept both tab-delimited and comma-delimited (CSV-ish) formats.</li>
 *   <li>Accept 1-column or 2-column pastes.</li>
 *   <li>Ignore empty/whitespace-only lines.</li>
 * </ul>
 */
public final class ClipboardTableParser {

	private ClipboardTableParser() {
		// utility
	}

	/**
	 * Parse clipboard text into a rectangular-ish grid.
	 *
	 * <p>Rules:
	 * <ul>
	 *   <li>Split rows on \r?\n</li>
	 *   <li>Split columns on tabs when present, otherwise commas, otherwise treat as 1 column</li>
	 *   <li>Trim cells; keep empty cells (""), but drop empty rows</li>
	 * </ul>
	 */
	public static List<List<String>> parseGrid(String clipboardText) {
		List<List<String>> grid = new ArrayList<>();
		if (clipboardText == null) {
			return grid;
		}

		String text = clipboardText.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = text.split("\n", -1);
		for (String rawLine : lines) {
			if (rawLine == null) {
				continue;
			}
			String line = rawLine.trim();
			if (line.isEmpty()) {
				continue;
			}

			List<String> row = splitLine(line);
			// drop fully-empty rows after trim
			boolean anyNonEmpty = false;
			for (String cell : row) {
				if (cell != null && !cell.trim().isEmpty()) {
					anyNonEmpty = true;
					break;
				}
			}
			if (anyNonEmpty) {
				grid.add(row);
			}
		}
		return grid;
	}

	private static List<String> splitLine(String line) {
		// Prefer tabs (Excel/Sheets table copy). If none, try commas.
		final String[] parts;
		if (line.indexOf('\t') >= 0) {
			parts = line.split("\t", -1);
		} else if (line.indexOf(',') >= 0) {
			parts = line.split(",", -1);
		} else {
			parts = new String[] { line };
		}

		List<String> out = new ArrayList<>(parts.length);
		for (String p : parts) {
			out.add(p == null ? "" : p.trim());
		}
		return out;
	}

	/**
	 * Returns the largest column count present in the parsed clipboard grid.
	 *
	 * @param grid parsed clipboard rows
	 * @return maximum number of cells found in any row
	 */
	public static int maxColumns(List<List<String>> grid) {
		int max = 0;
		for (List<String> row : grid) {
			if (row != null) {
				max = Math.max(max, row.size());
			}
		}
		return max;
	}

	/**
	 * Convert a single wide row into a column vector.
	 * Input:  [a, b, c]
	 * Output: [[a], [b], [c]]
	 */
	public static List<List<String>> transposeSingleRowToColumn(List<String> row) {
		List<List<String>> out = new ArrayList<>();
		if (row == null) {
			return out;
		}
		for (String cell : row) {
			List<String> r = new ArrayList<>(1);
			r.add(cell == null ? "" : cell.trim());
			out.add(r);
		}
		return out;
	}

	/**
	 * Pair two wide rows into 2-column row records.
	 * Input rows: years=[y1,y2,...], values=[v1,v2,...]
	 * Output: [[y1,v1], [y2,v2], ...]
	 */
	public static List<List<String>> pairRowsToTwoColumns(List<String> row0, List<String> row1) {
		List<List<String>> out = new ArrayList<>();
		if (row0 == null && row1 == null) {
			return out;
		}
		int n0 = row0 == null ? 0 : row0.size();
		int n1 = row1 == null ? 0 : row1.size();
		int n = Math.max(n0, n1);
		for (int i = 0; i < n; i++) {
			String a = i < n0 ? row0.get(i) : "";
			String b = i < n1 ? row1.get(i) : "";
			List<String> r = new ArrayList<>(2);
			r.add(a == null ? "" : a.trim());
			r.add(b == null ? "" : b.trim());
			out.add(r);
		}
		return out;
	}
}