package glimpseUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import gui.DiffLineRow;

/**
 * Diff generation and formatting helpers extracted from {@link GLIMPSEUtils}.
 */
public final class UtilsDiff {

	private GLIMPSEFiles files;
	private Consumer<ArrayList<String>> differencesDisplayer;

	public void init(GLIMPSEFiles files, Consumer<ArrayList<String>> differencesDisplayer) {
		this.files = files;
		this.differencesDisplayer = differencesDisplayer;
	}

	public boolean diffTwoFiles(String file1, String file2) {
		if (files == null)
			return false;
		ArrayList<String> diff = buildDiffReport(file1, file2);
		displayDifferences(diff);
		return !isDiffFailure(diff);
	}

	public ArrayList<String> buildDiffReport(String file1, String file2) {
		if (files == null)
			return buildDiffFailureReport("Diff failed: file access is not initialized.");
		ArrayList<String> file1Content = files.getStringArrayFromFile(file1, "#");
		ArrayList<String> file2Content = files.getStringArrayFromFile(file2, "#");
		return buildDiffReport(file1, file2, file1Content, file2Content);
	}

	public ArrayList<String> buildDiffReport(String file1Label, String file2Label, List<String> file1Content,
			List<String> file2Content) {
		Patch<String> patch = null;
		try {
			patch = DiffUtils.diff(file1Content == null ? new ArrayList<>() : file1Content,
					file2Content == null ? new ArrayList<>() : file2Content);
		} catch (DiffException e) {
			e.printStackTrace();
			return buildDiffFailureReport("Diff failed: could not compute differences.");
		}

		if (patch == null)
			return buildDiffFailureReport("Diff failed: could not compute differences.");

		ArrayList<String> diff = new ArrayList<>();
		diff.add("--- " + safeFileLabel(file1Label));
		diff.add("+++ " + safeFileLabel(file2Label));

		int inserts = 0;
		int deletes = 0;
		int changes = 0;
		for (AbstractDelta<String> delta : patch.getDeltas()) {
			if (delta == null)
				continue;
			switch (delta.getType()) {
			case INSERT:
				inserts++;
				break;
			case DELETE:
				deletes++;
				break;
			case CHANGE:
				changes++;
				break;
			default:
				break;
			}
		}

		diff.add("Hunks: " + patch.getDeltas().size() + "  (" + "insert=" + inserts + ", delete=" + deletes
				+ ", change=" + changes + ")");
		diff.add("---");

		final int context = 2;
		for (AbstractDelta<String> delta : patch.getDeltas()) {
			if (delta == null)
				continue;
			appendUnifiedHunk(diff, delta, file1Content, file2Content, context, true);
		}

		if (patch.getDeltas().isEmpty()) {
			diff.add("(No differences)");
		}
		return diff;
	}

	public List<DiffLineRow> generateSideBySideDiffRows(String file1, String file2) {
		if (files == null)
			return new ArrayList<>();
		ArrayList<String> file1Content = files.getStringArrayFromFile(file1, "#");
		ArrayList<String> file2Content = files.getStringArrayFromFile(file2, "#");

		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> "")
				.newTag(f -> "")
				.build();

		List<DiffLineRow> rows = new ArrayList<>();
		List<DiffRow> diffRows;
		try {
			diffRows = generator.generateDiffRows(file1Content, file2Content);
		} catch (DiffException e) {
			diffRows = new ArrayList<>();
			diffRows.add(new DiffRow(DiffRow.Tag.CHANGE, "Diff failed: " + e.getMessage(), ""));
		}
		int oldLine = 1;
		int newLine = 1;
		for (DiffRow row : diffRows) {
			if (row == null)
				continue;
			DiffRow.Tag tag = row.getTag();
			int oldNum = 0;
			int newNum = 0;
			switch (tag) {
			case INSERT:
				newNum = newLine++;
				break;
			case DELETE:
				oldNum = oldLine++;
				break;
			case CHANGE:
				oldNum = oldLine++;
				newNum = newLine++;
				break;
			case EQUAL:
			default:
				oldNum = oldLine++;
				newNum = newLine++;
				break;
			}
			rows.add(new DiffLineRow(oldNum, newNum, row.getOldLine(), row.getNewLine(), tag));
		}
		return rows;
	}

	private ArrayList<String> buildDiffFailureReport(String message) {
		ArrayList<String> diff = new ArrayList<>();
		diff.add(message == null ? "Diff failed." : message);
		return diff;
	}

	private boolean isDiffFailure(List<String> diff) {
		if (diff == null || diff.isEmpty())
			return true;
		String firstLine = diff.get(0);
		return firstLine != null && firstLine.startsWith("Diff failed:");
	}

	private void displayDifferences(ArrayList<String> diff) {
		if (differencesDisplayer != null) {
			differencesDisplayer.accept(diff);
		}
	}

	private String safeFileLabel(String filePath) {
		try {
			if (filePath == null)
				return "";
			File f = new File(filePath);
			String name = f.getName();
			return (name != null && !name.isEmpty()) ? name : filePath;
		} catch (Throwable t) {
			return filePath == null ? "" : filePath;
		}
	}

	private String safeGetLine(List<String> lines, int index0) {
		if (lines == null)
			return "";
		if (index0 < 0 || index0 >= lines.size())
			return "";
		String s = lines.get(index0);
		return s == null ? "" : s;
	}

	private String rstrip(String s) {
		if (s == null)
			return "";
		int end = s.length();
		while (end > 0) {
			char c = s.charAt(end - 1);
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
				end--;
			} else {
				break;
			}
		}
		return s.substring(0, end);
	}

	private void appendUnifiedHunk(ArrayList<String> out, AbstractDelta<String> delta, List<String> oldLines,
			List<String> newLines, int context, boolean normalizeTrailingWhitespace) {
		if (out == null || delta == null)
			return;

		int oldPos = 0;
		int oldSize = 0;
		int newPos = 0;
		int newSize = 0;
		List<String> srcLines = null;
		List<String> tgtLines = null;
		try {
			if (delta.getSource() != null) {
				oldPos = delta.getSource().getPosition();
				srcLines = delta.getSource().getLines();
				oldSize = (srcLines == null) ? 0 : srcLines.size();
			}
			if (delta.getTarget() != null) {
				newPos = delta.getTarget().getPosition();
				tgtLines = delta.getTarget().getLines();
				newSize = (tgtLines == null) ? 0 : tgtLines.size();
			}
		} catch (Throwable t) {
			out.add(String.valueOf(delta));
			out.add("---");
			return;
		}

		int oldStart = Math.max(0, oldPos - context);
		int oldEnd = Math.min(oldLines == null ? 0 : oldLines.size(), oldPos + Math.max(oldSize, 1) + context);
		int newStart = Math.max(0, newPos - context);
		int newEnd = Math.min(newLines == null ? 0 : newLines.size(), newPos + Math.max(newSize, 1) + context);

		out.add("@@ -" + (oldStart + 1) + "," + (oldEnd - oldStart) + " +" + (newStart + 1) + ","
				+ (newEnd - newStart) + " @@  (" + delta.getType() + ")");

		for (int i = oldStart; i < oldPos; i++) {
			String s = safeGetLine(oldLines, i);
			out.add(" " + (normalizeTrailingWhitespace ? rstrip(s) : s));
		}

		if (srcLines != null) {
			for (String s : srcLines) {
				out.add("-" + (normalizeTrailingWhitespace ? rstrip(s) : (s == null ? "" : s)));
			}
		}

		if (tgtLines != null) {
			for (String s : tgtLines) {
				out.add("+" + (normalizeTrailingWhitespace ? rstrip(s) : (s == null ? "" : s)));
			}
		}

		int oldResume = oldPos + oldSize;
		for (int i = oldResume; i < oldEnd; i++) {
			String s = safeGetLine(oldLines, i);
			out.add(" " + (normalizeTrailingWhitespace ? rstrip(s) : s));
		}

		out.add("---");
	}
}