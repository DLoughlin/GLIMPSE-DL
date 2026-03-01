/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package gui;

import java.io.File;
import java.util.List;

import com.github.difflib.text.DiffRow;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

// For Save As...
import javafx.stage.FileChooser;

// Export scope UI
import javafx.scene.control.ChoiceBox;

/**
 * Side-by-side diff window for comparing two text files (typically XML configs).
 *
 * Uses {@link com.github.difflib.text.DiffRowGenerator} output rendered into a JavaFX {@link TableView}.
 */
public class DiffWindow {

    /** Last size used for this window in the current session. */
    private static double lastWidth = 1200;
    private static double lastHeight = 720;

    /** Shows a modal diff window. */
    public static void show(String file1, String file2, List<DiffLineRow> rows) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(file1, file2, rows));
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Diff: " + shortName(file1) + " \u001f " + shortName(file2));

        ObservableList<DiffLineRow> data = FXCollections.observableArrayList(rows);

        // Backing filtered view so the scope pulldown can update the table immediately.
        FilteredList<DiffLineRow> filtered = new FilteredList<>(data, r -> true);

        TableView<DiffLineRow> table = new TableView<>(filtered);
        table.getStyleClass().add("diff-table");
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DiffLineRow, String> colOrigLine = new TableColumn<>("#");
        colOrigLine.setPrefWidth(80);
        colOrigLine.setMinWidth(80);
        colOrigLine.setMaxWidth(80);
        colOrigLine.setResizable(false);
        colOrigLine.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getOriginalLineNumber() > 0 ? String.valueOf(c.getValue().getOriginalLineNumber()) : ""));
        colOrigLine.setSortable(false);

        TableColumn<DiffLineRow, String> colOrig = new TableColumn<>("Original");
        colOrig.setCellValueFactory(new PropertyValueFactory<>("originalText"));
        colOrig.setSortable(false);

        TableColumn<DiffLineRow, String> colNewLine = new TableColumn<>("#");
        colNewLine.setPrefWidth(80);
        colNewLine.setMinWidth(80);
        colNewLine.setMaxWidth(80);
        colNewLine.setResizable(false);
        colNewLine.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNewLineNumber() > 0 ? String.valueOf(c.getValue().getNewLineNumber()) : ""));
        colNewLine.setSortable(false);

        TableColumn<DiffLineRow, String> colNew = new TableColumn<>("New");
        colNew.setCellValueFactory(new PropertyValueFactory<>("newText"));
        colNew.setSortable(false);

        installCellFactory(colOrigLine, true, true);
        installCellFactory(colOrig, true, false);
        installCellFactory(colNewLine, false, true);
        installCellFactory(colNew, false, false);

        table.getColumns().addAll(colOrigLine, colOrig, colNewLine, colNew);

        table.setRowFactory(tv -> new TableRow<DiffLineRow>() {
            @Override
            protected void updateItem(DiffLineRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("diff-row-different");
                if (!empty && item != null && item.isDifferent()) {
                    getStyleClass().add("diff-row-different");
                }
            }
        });

        Label header = new Label(shortName(file1) + "  vs  " + shortName(file2));
        header.setPadding(new Insets(6, 10, 6, 10));
        header.setFont(Font.font(header.getFont().getFamily(), 13));

        ChoiceBox<String> exportScope = new ChoiceBox<>(FXCollections.observableArrayList(
                "All rows",
                "Changed rows only"
        ));
        exportScope.getSelectionModel().select(0);
        exportScope.setTooltip(new Tooltip("Choose whether to show all rows or only changed rows"));

        // Re-filter the table whenever view scope changes.
        exportScope.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            boolean changedOnly = newV != null && newV.intValue() == 1;
            filtered.setPredicate(r -> {
                if (r == null) return false;
                return !changedOnly || r.isDifferent();
            });
            // Keep UI feeling responsive (selection and scroll position can become invalid after filtering).
            table.getSelectionModel().clearSelection();
            if (!table.getItems().isEmpty()) {
                table.scrollTo(0);
            }
        });

        Button saveAsBtn = new Button("Save As...");
        saveAsBtn.setTooltip(new Tooltip("Save the currently visible diff table as a CSV file"));
        saveAsBtn.setOnAction(e -> saveTableAsCsv(stage, table, file1, file2, exportScope.getSelectionModel().getSelectedIndex() == 1));

        Button nextDiffBtn = new Button("Next diff");
        nextDiffBtn.setOnAction(e -> jumpToNextDiff(table));

        HBox controls = new HBox(10, new Label("View:"), exportScope, saveAsBtn, nextDiffBtn);
        controls.setPadding(new Insets(6, 10, 6, 10));

        BorderPane root = new BorderPane();
        root.setTop(new VBox2(header, controls));
        root.setCenter(table);

        Scene scene = new Scene(root, lastWidth, lastHeight);
        // Prefer the app's shared modern.css for consistent styling.
        try {
            java.net.URL cssUrl = DiffWindow.class.getResource("/resources/modern.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        stage.setScene(scene);

        // Remember last size within this session.
        stage.widthProperty().addListener((obs, oldV, newV) -> {
            try {
                double w = newV == null ? -1 : newV.doubleValue();
                if (w > 300) {
                    lastWidth = w;
                }
            } catch (Exception ignored) {}
        });
        stage.heightProperty().addListener((obs, oldV, newV) -> {
            try {
                double h = newV == null ? -1 : newV.doubleValue();
                if (h > 300) {
                    lastHeight = h;
                }
            } catch (Exception ignored) {}
        });

        // Restore last size (best-effort) when shown.
        try {
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
        } catch (Exception ignored) {}

        stage.show();
    }

    private static void installCellFactory(TableColumn<DiffLineRow, String> col, boolean isOriginalColumn, boolean isLineNumberColumn) {
        col.setCellFactory(tc -> new TableCell<DiffLineRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                String display = empty ? null : maybeUnescapeEntities(item);
                setText(display);

                getStyleClass().removeAll("diff-cell", "diff-equal", "diff-insert", "diff-delete", "diff-change", "diff-empty-side");
                getStyleClass().add("diff-cell");

                if (empty) {
                    return;
                }

                DiffLineRow row = getTableRow() == null ? null : (DiffLineRow) getTableRow().getItem();
                if (row == null) {
                    return;
                }

                DiffRow.Tag tag = row.getTag();
                switch (tag) {
                case INSERT:
                    // New side shows green, original side is empty.
                    if (isOriginalColumn) {
                        getStyleClass().add("diff-empty-side");
                    } else {
                        getStyleClass().add("diff-insert");
                    }
                    break;
                case DELETE:
                    // Original side shows red, new side is empty.
                    if (isOriginalColumn) {
                        getStyleClass().add("diff-delete");
                    } else {
                        getStyleClass().add("diff-empty-side");
                    }
                    break;
                case CHANGE:
                    getStyleClass().add("diff-change");
                    break;
                case EQUAL:
                default:
                    getStyleClass().add("diff-equal");
                    break;
                }

                if (isLineNumberColumn) {
                    getStyleClass().add("diff-line-no");
                }
            }
        });
    }

    /**
     * Unescape common XML/HTML character entities for display.
     *
     * Handles: &amp;lt; &amp;gt; &amp;amp; &amp;quot; &amp;apos; plus numeric references (e.g. &#60; or &#x3C;).
     *
     * Note: This is for display only (the underlying data remains unchanged).
     */
    private static String maybeUnescapeEntities(String s) {
        if (s == null || s.indexOf('&') < 0) {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (ch != '&') {
                out.append(ch);
                continue;
            }

            int semi = s.indexOf(';', i + 1);
            if (semi < 0) {
                out.append(ch);
                continue;
            }

            String entity = s.substring(i + 1, semi); // without & and ;
            String repl = null;

            // Named entities commonly seen in escaped XML.
            if ("lt".equals(entity)) repl = "<";
            else if ("gt".equals(entity)) repl = ">";
            else if ("amp".equals(entity)) repl = "&";
            else if ("quot".equals(entity)) repl = "\"";
            else if ("apos".equals(entity)) repl = "'";
            else if (entity.startsWith("#")) {
                // Numeric references: &#DEC; or &#xHEX;
                try {
                    int codePoint;
                    if (entity.startsWith("#x") || entity.startsWith("#X")) {
                        codePoint = Integer.parseInt(entity.substring(2), 16);
                    } else {
                        codePoint = Integer.parseInt(entity.substring(1), 10);
                    }

                    if (codePoint >= 0 && Character.isValidCodePoint(codePoint)) {
                        repl = new String(Character.toChars(codePoint));
                    }
                } catch (Exception ignored) {
                    // leave as-is
                }
            }

            if (repl != null) {
                out.append(repl);
                i = semi; // skip past ';'
            } else {
                // Unknown; keep original
                out.append('&').append(entity).append(';');
                i = semi;
            }
        }
        return out.toString();
    }

    private static void jumpToNextDiff(TableView<DiffLineRow> table) {
        int start = Math.max(0, table.getSelectionModel().getSelectedIndex());
        if (table.getItems().isEmpty()) {
            return;
        }
        for (int i = start + 1; i < table.getItems().size(); i++) {
            if (table.getItems().get(i) != null && table.getItems().get(i).isDifferent()) {
                table.getSelectionModel().clearAndSelect(i, table.getColumns().get(1));
                table.scrollTo(Math.max(0, i - 3));
                return;
            }
        }
        // Wrap
        for (int i = 0; i <= start; i++) {
            if (table.getItems().get(i) != null && table.getItems().get(i).isDifferent()) {
                table.getSelectionModel().clearAndSelect(i, table.getColumns().get(1));
                table.scrollTo(Math.max(0, i - 3));
                return;
            }
        }
    }

    private static String shortName(String path) {
        try {
            if (path == null) return "";
            return new File(path).getName();
        } catch (Exception e) {
            return path == null ? "" : path;
        }
    }

    private static void saveTableAsCsv(Stage owner, TableView<DiffLineRow> table, String file1, String file2, boolean changedRowsOnly) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Diff as CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));

            String base = "diff";
            try {
                String a = shortName(file1);
                String b = shortName(file2);
                if (a != null && !a.trim().isEmpty() && b != null && !b.trim().isEmpty()) {
                    base = "diff_" + a + "_vs_" + b;
                }
            } catch (Exception ignored) {
            }
            // Note: Save As exports whatever is currently visible (table.getItems()). We still use
            // the scope to provide a sensible default filename.
            if (changedRowsOnly) {
                base = base + "_changed";
            }
            chooser.setInitialFileName(sanitizeFileName(base) + ".csv");

            File outFile = chooser.showSaveDialog(owner);
            if (outFile == null) {
                return;
            }

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(outFile), java.nio.charset.StandardCharsets.UTF_8))) {
                pw.println("OriginalLine,Original,NewLine,New,Tag");

                // Export exactly what the user is currently viewing.
                for (DiffLineRow r : table.getItems()) {
                    if (r == null) {
                        continue;
                    }

                    String origLine = r.getOriginalLineNumber() > 0 ? String.valueOf(r.getOriginalLineNumber()) : "";
                    String newLine = r.getNewLineNumber() > 0 ? String.valueOf(r.getNewLineNumber()) : "";

                    String orig = maybeUnescapeEntities(r.getOriginalText());
                    String neu = maybeUnescapeEntities(r.getNewText());

                    String tag = r.getTag() == null ? "" : r.getTag().name();

                    pw.print(csv(origLine));
                    pw.print(',');
                    pw.print(csv(orig));
                    pw.print(',');
                    pw.print(csv(newLine));
                    pw.print(',');
                    pw.print(csv(neu));
                    pw.print(',');
                    pw.print(csv(tag));
                    pw.println();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        boolean needsQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',' || c == '"' || c == '\n' || c == '\r' || c == '\t') {
                needsQuotes = true;
                break;
            }
        }
        if (!needsQuotes) {
            return s;
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    private static String sanitizeFileName(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "diff";
        }
        // Replace characters that are not allowed in Windows filenames.
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    /**
     * Minimal helper VBox so we don't pull in extra files; avoids javafx.scene.layout.VBox import name clash
     * with existing project conventions.
     */
    private static class VBox2 extends javafx.scene.layout.VBox {
        VBox2(javafx.scene.Node... children) {
            super(0, children);
        }
    }

    /**
     * Captures selected cell positions, in selection order.
     */
    private static class TablePosition2 {
        final int row;
        final int colId;

        private TablePosition2(int row, int colId) {
            this.row = row;
            this.colId = colId;
        }

        static java.util.List<TablePosition2> from(TableView<DiffLineRow> tv) {
            java.util.List<TablePosition2> out = new java.util.ArrayList<>();
            try {
                for (javafx.scene.control.TablePosition<?, ?> p : tv.getSelectionModel().getSelectedCells()) {
                    out.add(new TablePosition2(p.getRow(), p.getColumn()));
                }
            } catch (Exception ignored) {
            }
            if (out.isEmpty()) {
                int idx = tv.getSelectionModel().getSelectedIndex();
                if (idx >= 0) {
                    out.add(new TablePosition2(idx, 1));
                }
            }
            return out;
        }
    }
}
