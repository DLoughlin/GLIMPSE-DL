/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package gui;

import com.github.difflib.text.DiffRow;

/**
 * One row in the side-by-side diff view.
 *
 * Stores a single visual line of output from {@link com.github.difflib.text.DiffRowGenerator}.
 */
public class DiffLineRow {

    private final int originalLineNumber;
    private final int newLineNumber;
    private final String originalText;
    private final String newText;
    private final DiffRow.Tag tag;

    public DiffLineRow(int originalLineNumber, int newLineNumber, String originalText, String newText, DiffRow.Tag tag) {
        this.originalLineNumber = originalLineNumber;
        this.newLineNumber = newLineNumber;
        this.originalText = originalText == null ? "" : originalText;
        this.newText = newText == null ? "" : newText;
        this.tag = tag == null ? DiffRow.Tag.EQUAL : tag;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public int getNewLineNumber() {
        return newLineNumber;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getNewText() {
        return newText;
    }

    public DiffRow.Tag getTag() {
        return tag;
    }

    public boolean isDifferent() {
        return tag != DiffRow.Tag.EQUAL;
    }
}
