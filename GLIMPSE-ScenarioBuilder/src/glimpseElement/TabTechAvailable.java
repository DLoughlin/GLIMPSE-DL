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

import java.util.ArrayList;
import glimpseBuilder.TechBound;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TabTechAvailable is a UI component used by the GLIMPSE Scenario Builder to
 * present and manage technology availability constraints. It provides an editable table
 * of technologies with support for filtering, bulk selection actions, and export to
 * GLIMPSE-format scenario component files.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Render an editable table of TechBound entries displaying first/last years and bound flags.</li>
 *   <li>Support filtering by category and free-form text search across all visible items.</li>
 *   <li>Provide bulk selection actions: set all visible to 'Never' (bound all), set to 'Range', or set years.</li>
 *   <li>Serialize selected constraints into CSV-style input tables (nested and non-nested technology structures).</li>
 *   <li>Generate metadata headers describing policy selections and constraints.</li>
 *   <li>Load previously saved constraints from scenario component files.</li>
 * </ul>
 * </p>
 *
 * <p><b>UI Structure:</b>
 * <ul>
 *   <li>Left panel: Filter controls, editable technology bounds table, and year/selection buttons.</li>
 *   <li>Right panel: Region tree selection pane for specifying where constraints apply.</li>
 * </ul>
 * </p>
 *
 * <p><b>Table Columns:</b>
 * <ul>
 *   <li>'Never?' - CheckBox column for bounding all years.</li>
 *   <li>'Range?' - CheckBox column for bounding specific year ranges.</li>
 *   <li>'First' - Editable first year value.</li>
 *   <li>'Last' - Editable last year value.</li>
 *   <li>Technology description with sector, subsector, and units.</li>
 * </ul>
 * </p>
 *
 * <p><b>Special Handling:</b>
 * <ul>
 *   <li>Nested subsector markers ('=>') are hidden in display but preserved in exports.</li>
 *   <li>Category filtering works in combination with free-text filtering.</li>
 *   <li>Bulk operations apply to all currently visible items after filtering.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread Safety:</b> Not thread-safe; use only on the JavaFX Application Thread.</p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Extends PolicyTab and implements Runnable for background thread execution.</li>
 *   <li>Interacts with application-level utilities (vars, utils, styles) for data and UI support.</li>
 * </ul>
 * </p>
 */
public class TabTechAvailable extends PolicyTab implements Runnable {
    // === Constants for UI Texts and Options ===
    private static final String LABEL_FILTER_BY_CATEGORY = "Filter by Category: ";
    private static final String LABEL_TEXT = " Text: ";
    private static final String LABEL_FIRST_YEAR = " First yr: ";
    private static final String LABEL_LAST_YEAR = " Last yr: ";
    private static final String LABEL_SELECT = "Select: ";
    private static final String BUTTON_SET_YEARS = "Set Years";
    private static final String BUTTON_SELECT_ALL = "Never";
    private static final String BUTTON_SELECT_RANGE = "Range";
    private static final String LABEL_TECH_SELECT = "Select technologies and specify all, first, or last years to constrain new purchases:";
    private static final String COMBOBOX_FILTER_CATEGORY_DEFAULT = "Filter by Category?";
    private static final String COMBOBOX_FILTER_CATEGORY_ALL = "All";
    private static final String DEFAULT_FIRST_YEAR = "1975";
    private static final String DEFAULT_LAST_YEAR = "2021";

    // === Table and Layout Components ===
    public final TableView<TechBound> tableTechBounds = new TableView<>();
     
     // === Data Lists ===
     private ObservableList<TechBound> origList;
     private ObservableList<TechBound> tableList;

    // === Filter and Control UI ===
    private final Label filterByCategoryLabel = createLabel(LABEL_FILTER_BY_CATEGORY);
    private final ComboBox<String> comboBoxCategoryFilter = createComboBoxString();
    private final Label filterByTextLabel = createLabel(LABEL_TEXT);
    private final TextField filterTextField = createTextField();
    private final Label firstYrLabel = createLabel(LABEL_FIRST_YEAR);
    private final TextField firstYrTextField = createTextField();
    private final Label lastYrLabel = createLabel(LABEL_LAST_YEAR);
    private final TextField lastYrTextField = createTextField();
    private final Button setFirstLastYrsButton = createButton(BUTTON_SET_YEARS, styles.getBigButtonWidth(), null);
    private final Label selectLabel = createLabel(LABEL_SELECT);
    private final Button selectAllButton = createButton(BUTTON_SELECT_ALL, styles.getBigButtonWidth(), null);
    private final Button selectRangeButton = createButton(BUTTON_SELECT_RANGE, styles.getBigButtonWidth(), null);

    /**
     * Constructor for TabTechAvailable. Sets up the UI, initializes the technology bounds table,
     * configures event handlers for filtering and bulk actions, and prepares the regions tree pane.
     *
     * @param title  The title of the tab to display
     * @param stageX The JavaFX Stage (provided by caller)
     */
    public TabTechAvailable(String title, Stage stageX) {
        this.setStyle(styles.getFontStyle());
        setupUIControls();
        setComponentWidths();
        setupUILayout(title);
    }

    /**
     * Sets up UI controls and their event handlers. Creates the table columns with appropriate
     * cell factories for boolean checkboxes and editable year fields, binds TechBound properties,
     * initializes default year values, wires up selection/year buttons, prepares category filter,
     * and loads the initial technology bounds list from application variables.
     *
     * Important: This method does not attach controls into the tab layout — that is handled by setupUILayout().
     */
    private void setupUIControls() {
        
        // Set up the filter text field to update the sector combo box
        filterTextField.setPromptText("Filter techs");
        
        firstYrTextField.setText(DEFAULT_FIRST_YEAR);
        lastYrTextField.setText(DEFAULT_LAST_YEAR);

        setOnAction(setFirstLastYrsButton, e -> updateFirstAndLastYears(firstYrTextField.getText(), lastYrTextField.getText()));
        setOnAction(selectAllButton, e -> selectAllVisibleItems());
        setOnAction(selectRangeButton, e -> selectRangeVisibleItems());

        TableColumn<TechBound, Boolean> isBoundAll = new TableColumn<>(BUTTON_SELECT_ALL + "?");
        TableColumn<TechBound, Boolean> isBoundRange = new TableColumn<>(BUTTON_SELECT_RANGE + "?");
        TableColumn<TechBound, String> techNameCol = new TableColumn<>("Sector : Subsector : Technology : Units // Category");
        TableColumn<TechBound, String> firstYearCol = new TableColumn<>("First");
        TableColumn<TechBound, String> lastYearCol = new TableColumn<>("Last");

        tableTechBounds.getColumns().clear();
        tableTechBounds.getColumns().addAll(isBoundAll, isBoundRange, firstYearCol, lastYearCol, techNameCol);

        setupBooleanColumn(isBoundAll, true);
        setupBooleanColumn(isBoundRange, false);
        tableTechBounds.setEditable(true);
        isBoundAll.setEditable(true);

        techNameCol.setCellValueFactory(new PropertyValueFactory<>("techName"));
        techNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        techNameCol.setEditable(false);

        setupYearColumn(firstYearCol, true);
        setupYearColumn(lastYearCol, false);

        if (origList == null) origList = getBoundList();
        ObservableList<TechBound> list2 = hideNestedSubsectorFromTechList();
        tableTechBounds.setItems(list2);
        tableList = tableTechBounds.getItems();
        addFiltering();
        setupComboBoxType();
    }

    /**
     * Sets preferred widths for UI components.
     */
    private void setComponentWidths() {
        firstYrTextField.setPrefWidth(styles.getBigButtonWidth());
        lastYrTextField.setPrefWidth(styles.getBigButtonWidth());
        tableTechBounds.setMinWidth(500);
        //tableTechBounds.setPrefWidth(700);
        paneForCountryStateTree.setMinWidth(275.);
    }

    /**
     * Sets up the layout of the tab using HBox and VBox containers. Arranges the table and
     * filter controls on the left panel, and the region tree pane on the right. Configures
     * resizing behavior so the table expands to fill available space while controls remain
     * at the bottom. Sets this layout as the tab's content.
     * 
     * @param title The title of the tab to display
     */
    private void setupUILayout(String title) {
        this.setText(title);
        HBox tabLayout = new HBox();
        tabLayout.autosize();
        // Apply centralized default padding, spacing and background style so the tab
        // visually matches other PolicyTab-based tabs
        tabLayout.setPadding(styles.getDefaultPadding());
        tabLayout.setSpacing(6.0);
        //tabLayout.setStyle(styles.getStyle2());
         VBox leftPanel = new VBox();
        // apply CSS-based border to match the requested format
        leftPanel.setStyle("-fx-border-color: #A9A9A9; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        leftPanel.setPadding(styles.getDefaultPadding());
          // Title at top
          Label titleLabel = utils.createLabel(LABEL_TECH_SELECT);
          leftPanel.getChildren().add(titleLabel);
        // Ensure left panel uses the same light background and padding as other panels
        //leftPanel.setStyle(styles.getStyle2());
        //  leftPanel.setStyle(styles.getStyle2());
        //leftPanel.setPadding(styles.getDefaultPadding());
         
         // Filter layout immediately under the title (anchored to top)
         HBox filterHBox = new HBox();
         filterHBox.setPadding(styles.getDefaultPadding());
         filterHBox.getChildren().addAll(filterByCategoryLabel, comboBoxCategoryFilter, filterByTextLabel, filterTextField);
         leftPanel.getChildren().add(filterHBox);

        // Main table that should expand vertically
        leftPanel.getChildren().add(tableTechBounds);
        VBox.setVgrow(tableTechBounds, Priority.ALWAYS);

        // Place the reset/year controls at the bottom
        HBox resetYrLayout = new HBox();
         // Use centralized medium padding for grouped control areas, add 5px extra top padding
         Insets medPad = styles.getMediumPadding();
         resetYrLayout.setPadding(new Insets(medPad.getTop() + 5, medPad.getRight(), medPad.getBottom(), medPad.getLeft()));
         resetYrLayout.setSpacing(5.);
         resetYrLayout.getChildren().addAll(selectLabel, selectAllButton, selectRangeButton, firstYrLabel, firstYrTextField, lastYrLabel, lastYrTextField, setFirstLastYrsButton);
         leftPanel.getChildren().add(resetYrLayout);

        // Allow the left panel to expand vertically with the tab
        leftPanel.prefHeightProperty().bind(tabLayout.heightProperty());
        
        // Bind the table bottom to the top of resetYrLayout so the table resizes exactly up to the controls
        // Subtract header and filter heights and a small padding to avoid overlap
        tableTechBounds.prefHeightProperty().bind(leftPanel.heightProperty()
                .subtract(resetYrLayout.heightProperty())
                .subtract(filterHBox.heightProperty())
                .subtract(titleLabel.heightProperty())
                .subtract(20));
 
         // Make the tree pane 1/3 of the width, and anchor it to the right side
         // Use PolicyTab helper to set up the right column (adds paneForCountryStateTree into vBoxRight/scrollPaneRight)
         setupRightColumn();
         // Configure right scroll pane so the tree pane can expand to the right edge
         // Let the scroll pane fit its content width and bind the tree pane width to the scroll pane
         scrollPaneRight.setFitToWidth(true);
         scrollPaneRight.setFitToHeight(true);
         // Align children of the right VBox to the top-right so the tree sits against the right edge when smaller
         vBoxRight.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
         // Allow the pane to grow to the scroll pane width (minus small padding)
         paneForCountryStateTree.prefWidthProperty().bind(scrollPaneRight.widthProperty().subtract(8));
         paneForCountryStateTree.setMaxWidth(Double.MAX_VALUE);
         // Bind the scrollPaneRight to occupy ~1/3 of the total tab width
         scrollPaneRight.prefWidthProperty().bind(tabLayout.widthProperty().multiply(0.33));
         HBox.setHgrow(leftPanel, Priority.ALWAYS);
         
         // Ensure table also resizes with the left panel
         tableTechBounds.prefWidthProperty().bind(leftPanel.widthProperty().subtract(20));
         // Apply default style/padding to the right scroll pane so the tree matches other tabs
         //scrollPaneRight.setStyle(styles.getStyle2());
         scrollPaneRight.setPadding(styles.getDefaultPadding());
 
         tabLayout.getChildren().addAll(leftPanel, scrollPaneRight);
         this.setContent(tabLayout);
     }

    /**
     * Configure a boolean column to bind to the corresponding TechBound flag.
     * The column displays a centered CheckBox and will update the underlying
     * TechBound when the user toggles the cell.
     *
     * @param column the TableColumn to configure
     * @param isAll true to bind the column to the 'isBoundAll' flag, false for 'isBoundRange'
     */
    private void setupBooleanColumn(TableColumn<TechBound, Boolean> column, boolean isAll) {
        column.setCellValueFactory(param -> {
            TechBound tb = param.getValue();
            SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(isAll ? tb.isBoundAll() : tb.isBoundRange());
            booleanProp.addListener((observable, oldValue, newValue) -> {
                if (isAll) tb.setIsBoundAll(newValue);
                else tb.setIsBoundRange(newValue);
            });
            return booleanProp;
        });
        column.setCellFactory(p -> {
            CheckBoxTableCell<TechBound, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
    }

    /**
     * Configure a year column (editable text) bound to a TechBound's first or
     * last year. Changes entered by the user are immediately propagated to the
     * model object.
     *
     * @param column the TableColumn to configure
     * @param isFirst true to bind to the first year property, false for last year
     */
    private void setupYearColumn(TableColumn<TechBound, String> column, boolean isFirst) {
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setEditable(true);
        column.setCellValueFactory(param -> {
            TechBound tb = param.getValue();
            SimpleStringProperty strProp = new SimpleStringProperty(isFirst ? tb.getFirstYear() : tb.getLastYear());
            strProp.addListener((observable, oldValue, newValue) -> {
                if (isFirst) tb.setFirstYear(newValue);
                else tb.setLastYear(newValue);
            });
            return strProp;
        });
    }

    /**
     * Populate the category filter combo box with available categories derived
     * from technology metadata and select the first (default) entry.
     */
    private void setupComboBoxType() {
        comboBoxCategoryFilter.getItems().addAll(vars.getCategoriesFromTechBnd());
        comboBoxCategoryFilter.getSelectionModel().selectFirst();
    }

    /**
     * Build a display list of TechBound objects where nested subsector markers
     * (expressed as '=>' in original names) are simplified for readability.
     *
     * The returned list contains new TechBound instances with the same year and
     * bound flags as the originals but with an altered techName used only for
     * display and user selection. The original list (origList) is used later
     * to map back to the full original names when exporting.
     *
     * @return an ObservableList of TechBound prepared for table display
     */
    private ObservableList<TechBound> hideNestedSubsectorFromTechList() {
        ObservableList<TechBound> rtnList = FXCollections.observableArrayList();
        for (TechBound tb0 : origList) {
            TechBound tb = new TechBound(tb0.getFirstYear(), tb0.getLastYear(), tb0.getTechName(), tb0.isBoundAll(), tb0.isBoundRange());
            String name = tb.getTechName();
            String[] component = name.split(":");
            StringBuilder modifiedName = new StringBuilder();
            for (int j = 0; j < component.length; j++) {
                if (component[j].contains("=>")) {
                    component[j] = component[j].split("=>")[1];
                }
                modifiedName.append(component[j]);
                if (j != component.length - 1) modifiedName.append(" : ");
            }
            tb.setTechName(modifiedName.toString());
            rtnList.add(tb);
        }
        return rtnList;
    }

    /**
     * Given a compact display line (with nested '=>' parts removed), find the
     * corresponding original technology line from the stored origList. This is
     * used when exporting to ensure the exported constraint references the
     * original nested structure when present.
     *
     * @param line the compacted displayed tech name
     * @return the matching original tech line, or an empty string if not found
     */
    private String getMatchingLineFromTechList(String line) {
        String matchingLine = "";
        String[] words = line.split(":");
        for (int i = 0; i < words.length; i++) words[i] = words[i].trim();
        boolean match = false;
        for (TechBound origTb : origList) {
            String origLine = origTb.getTechName();
            if (!match) {
                for (int j = 0; j < words.length; j++) {
                    match = true;
                    String txt = null;
                    String txt1 = null;
                    if (j == 0) {
                        txt = txt1 = words[j].trim() + " :";
                    } else {
                        txt = ": " + words[j].trim();
                        txt1 = ">" + words[j].trim();
                    }
                    if ((origLine.indexOf(txt) == -1) && (origLine.indexOf(txt1) == -1)) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                matchingLine = origLine;
                break;
            }
        }
        if (!match) System.out.println("Error adding constraint to " + line);
        return matchingLine;
    }

    /**
     * Apply the provided first and last years to every currently visible row in
     * the table. Executed on the JavaFX thread via Platform.runLater to ensure
     * safe UI/model updates.
     */
    private void updateFirstAndLastYears(String firstYr, String lastYr) {
        Platform.runLater(() -> {
            FilteredList<TechBound> visibleComponents = new FilteredList<>(tableTechBounds.getItems(), p -> true);
            for (TechBound tb : visibleComponents) {
                tb.setFirstYear(firstYr);
                tb.setLastYear(lastYr);
            }
            String text = filterTextField.getText();
            filterTextField.setText("Resetting...");
            filterTextField.setText(text);
        });
    }

    /**
     * Toggle the 'Never?' bound flag for all visible technologies. If any
     * visible row is currently unbound the action will set all to bound; if
     * all are already bound the action clears them (acts as a toggle).
     */
    private void selectAllVisibleItems() {
        Platform.runLater(() -> {
            ObservableList<TechBound> visibleComponents = tableTechBounds.getItems();
            // If any visible item is not currently bound, set all to bound; otherwise clear all
            boolean anyUnbound = false;
            for (TechBound tb : visibleComponents) {
                if (!tb.isBoundAll()) { anyUnbound = true; break; }
            }
            for (TechBound tb : visibleComponents) {
                tb.setIsBoundAll(anyUnbound);
            }
            String text = filterTextField.getText();
            filterTextField.setText("Resetting...");
            filterTextField.setText(text);
        });
    }

    /**
     * Toggle the 'Range?' bound flag for all visible technologies. Behavior is
     * analogous to selectAllVisibleItems but operates on the range-flag.
     */
    private void selectRangeVisibleItems() {
        Platform.runLater(() -> {
            ObservableList<TechBound> visibleComponents = tableTechBounds.getItems();
            // If any visible item is not currently range-bound, set all to range; otherwise clear all
            boolean anyUnbound = false;
            for (TechBound tb : visibleComponents) {
                if (!tb.isBoundRange()) { anyUnbound = true; break; }
            }
            for (TechBound tb : visibleComponents) {
                tb.setIsBoundRange(anyUnbound);
            }
            String text = filterTextField.getText();
            filterTextField.setText("Resetting...");
            filterTextField.setText(text);
        });
    }

    /**
     * Add text and category filtering to the table. The filter combines the
     * selected category (from comboBoxCategoryFilter) and the text from
     * filterTextField to restrict which rows are shown. The filtered results
     * are wrapped in a SortedList and bound to the table so column sorting
     * continues to work.
     */
    private void addFiltering() {
        FilteredList<TechBound> filteredComponents = new FilteredList<>(tableTechBounds.getItems(), p -> true);
        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredComponents.setPredicate(techBound -> {
                String type = comboBoxCategoryFilter.getSelectionModel().getSelectedItem().toLowerCase().trim();
                if (type.equals("") || type.equals(COMBOBOX_FILTER_CATEGORY_ALL.toLowerCase()) || type.equals(COMBOBOX_FILTER_CATEGORY_DEFAULT.toLowerCase())) {
                    // pass
                } else if (!techBound.getTechName().toLowerCase().trim().endsWith(type)) {
                    return false;
                }
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase().trim();
                return techBound.getTechName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        comboBoxCategoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            filteredComponents.setPredicate(techBound -> {
                if (newValue.equals(COMBOBOX_FILTER_CATEGORY_DEFAULT) || newValue.equals(COMBOBOX_FILTER_CATEGORY_ALL)) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return techBound.getTechName().toLowerCase().trim().endsWith(lowerCaseFilter);
            });
            String filterText = filterTextField.getText();
            filterTextField.setText("");
            if (!newValue.equals(COMBOBOX_FILTER_CATEGORY_DEFAULT)) {
                filterTextField.setText(filterText);
            }
        });
        SortedList<TechBound> sortedComponents = new SortedList<>(filteredComponents);
        sortedComponents.comparatorProperty().bind(tableTechBounds.comparatorProperty());
        tableTechBounds.setItems(sortedComponents);
    }

    /**
     * Runnable implementation: invoked when background work should commit
     * changes. This implementation schedules saving the scenario component on
     * the JavaFX thread.
     */
    @Override
    public void run() {
        Platform.runLater(this::saveScenarioComponent);
    }

    /**
     * Build a suggested filename for exported scenario component files based on
     * the selected regions. Uses a shortened token if many regions are selected
     * to keep filenames readable. The returned filename is sanitized to only
     * include alphanumerics and underscores.
     *
     * @return a filename suggestion (including .csv extension)
     */
    public String getFilenameSuggestion() {
        String[] selectedLeaves = utils.getAllSelectedRegions(paneForCountryStateTree.getTree());
        String state = "";
        if (selectedLeaves.length > 0) {
            selectedLeaves = utils.removeUSADuplicate(selectedLeaves);
            String stateStr = utils.returnAppendedString(selectedLeaves).replace(",", "");
            state = stateStr.length() < 9 ? stateStr : "Reg";
        }
        return ("tchAvl_" + state).replaceAll("[^a-zA-Z0-9_]", "_") + ".csv";
    }

    /**
     * Collect the selected constraints from the UI and synthesize the
     * corresponding scenario component file content. The method produces one
     * or two input table bodies: one for non-nested technologies and another
     * for nested (nesting-subsector) technologies. No file I/O is performed
     * here; the constructed content is stored in the fileContent field.
     *
     * The method requires qaInputs() to pass before building content.
     */
    @Override
    public void saveScenarioComponent() {
        if (qaInputs()) {
            TreeView<String> tree = this.paneForCountryStateTree.getTree();
            String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
            listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
            utils.returnAppendedString(listOfSelectedLeaves);

            filenameSuggestion = getFilenameSuggestion();

            StringBuilder fileContentBuilder = new StringBuilder();
            fileContentBuilder.append(getMetaDataContent(tree));

            StringBuilder fileContent1Builder = new StringBuilder();
            StringBuilder fileContent2Builder = new StringBuilder();

            String header1 = "GLIMPSETechAvailBnd";
            String header2 = "GLIMPSETechAvailBnd-Nest";

            fileContent1Builder.append("INPUT_TABLE").append(vars.getEol())
                .append("Variable ID").append(vars.getEol())
                .append(header1).append(vars.getEol()).append(vars.getEol())
                .append("region,sector,subsector,tech,init-year,final-year").append(vars.getEol());

            fileContent2Builder.append("INPUT_TABLE").append(vars.getEol())
                .append("Variable ID").append(vars.getEol())
                .append(header2).append(vars.getEol()).append(vars.getEol())
                .append("region,sector,nesting-subsector,subsector,tech,init-year,final-year").append(vars.getEol());

            int numNonNest = 0;
            int numNest = 0;

            for (TechBound techBound : tableList) {
                if (techBound.isBoundAll() || techBound.isBoundRange()) {
                    String tblName = techBound.getTechName();
                    String name = getMatchingLineFromTechList(tblName);
                    boolean isNested = name.contains("=>");

                    String firstYear = techBound.getFirstYear();
                    String lastYear = techBound.getLastYear();
                    String[] info = name.split(":");
                    name = info[0].trim() + "," + info[1].trim() + "," + info[2].trim();

                    if (techBound.isBoundAll()) {
                        firstYear=""+vars.getAllYears().get(0);
                        lastYear=""+vars.getCalibrationYear();
                    }

                    if (isNested) {
                        name = name.replace("=>", ",").trim();
                    }

                    for (String state : listOfSelectedLeaves) {
                        String line = state + "," + name + "," + firstYear + "," + lastYear + vars.getEol();
                        if (!isNested) {
                            numNonNest++;
                            fileContent1Builder.append(line);
                        } else {
                            numNest++;
                            fileContent2Builder.append(line);
                        }
                    }
                }
            }

            if (numNonNest > 0 && numNest > 0) {
                fileContent2Builder.insert(0, vars.getEol());
            }

            if (numNonNest > 0) {
                fileContentBuilder.append(fileContent1Builder);
            }

            if (numNest > 0) {
                fileContentBuilder.append(fileContent2Builder);
            }

            fileContent = fileContentBuilder.toString();
        }
    }

    /**
     * Produce a header block containing metadata about the current scenario
     * component: the tab title, the list of active bounds (with flags and
     * years), and the selected regions. This metadata is prefixed to the
     * exported file content to aid human readers and downstream parsing.
     *
     * @param tree the TreeView containing the current region selection
     * @return formatted metadata string (already terminated with EOL)
     */
    public String getMetaDataContent(TreeView<String> tree) {
        StringBuilder rtnStr = new StringBuilder();
        rtnStr.append("############ Scenario Component Meta-Data ############").append(vars.getEol());
        rtnStr.append("#Scenario component type: ").append(this.getText()).append(vars.getEol());
        for (TechBound bnd : tableList) {
            if (bnd.isBoundAll() || bnd.isBoundRange()) {
                rtnStr.append("#Bound:Never>").append(bnd.isBoundAll())
                        .append(",Range>").append(bnd.isBoundRange())
                        .append(",First>").append(bnd.getFirstYear())
                        .append(",Last>").append(bnd.getLastYear())
                        .append(",Tech>").append(bnd.getTechName())
                        .append(vars.getEol());
            }
        }
        String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
        listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
        String states = utils.returnAppendedString(listOfSelectedLeaves);
        rtnStr.append("#Regions: ").append(states).append(vars.getEol());
        rtnStr.append("######################################################").append(vars.getEol());
        return rtnStr.toString();
    }

    /**
     * Load state from a list of lines (as read from an exported scenario
     * component). Recognized metadata entries (lines starting with '#') are
     * parsed and used to restore bound flags, year fields, and region
     * selections in the UI.
     *
     * @param content lines of the file to load
     */
    @Override
    public void loadContent(ArrayList<String> content) {
        ObservableList<TechBound> techList = tableTechBounds.getItems();
        for (String line : content) {
            int pos = line.indexOf(":");
            if (line.startsWith("#") && (pos > -1)) {
                String param = line.substring(1, pos).trim().toLowerCase();
                String value = line.substring(pos + 1).trim();
                if (param.equals("bound")) {
                    String[] attributes = utils.splitString(value, ",");
                    String never = "";
                    String range = "";
                    String first = "";
                    String last = "";
                    String tech = "";
                    for (String str : attributes) {
                        int pos2 = str.indexOf(">");
                        String att = str.substring(0, pos2).trim().toLowerCase();
                        String val = str.substring(pos2 + 1).trim();
                        if (att.equals("never")) {
                            never = val;
                        } else if (att.equals("range")) {
                            range = val;
                        } else if (att.equals("first")) {
                            first = val;
                        } else if (att.equals("last")) {
                            last = val;
                        } else if (att.equals("tech")) {
                            tech = val.toLowerCase().trim();
                        }
                    }
                    for (TechBound tb : techList) {
                        if (tb.getTechName().toLowerCase().equals(tech)) {
                            if (never.equals("true")) tb.setIsBoundAll(true);
                            if (range.equals("true")) tb.setIsBoundRange(true);
                            if (!first.equals("")) tb.setFirstYear(first);
                            if (!last.equals("")) tb.setLastYear(last);
                            break;
                        }
                    }
                }
                if (param.equals("regions")) {
                    String[] regions = utils.splitString(value, ",");
                    this.paneForCountryStateTree.selectNodes(regions);
                }
            }
        }
    }

    /**
     * Read technology definition data from the application-level vars and
     * compose the initial list of TechBound objects used to populate the
     * table. The method attempts to avoid duplicate adjacent entries.
     *
     * @return an ObservableList of TechBound initialized with default years
     */
    private ObservableList<TechBound> getBoundList() {
        ObservableList<TechBound> list = FXCollections.observableArrayList();
        try {
            String[][] techInfo = vars.getTechInfo();
            String lastLine = "";
            for (String[] tech : techInfo) {
                String line = tech[0].trim() + " : " + tech[1] + " : " + tech[2];
                if (!line.equals(lastLine)) {
                    lastLine = line;
                    if (tech.length >= 7) line += " : " + tech[6];
                    if (tech.length >= 8) line += " // " + tech[7];
                    if (line.length() > 0) {
                        list.add(new TechBound(DEFAULT_FIRST_YEAR, DEFAULT_LAST_YEAR, line, Boolean.FALSE, Boolean.FALSE));
                    }
                }
            }
        } catch (Exception e) {
            utils.warningMessage("Problem reading tech list. Attempting to use defaults.");
            System.out.println("Error reading tech list from " + vars.getTchBndListFilename() + ":");
            System.out.println("  ---> " + e);
        }
        return list;
    }


    /**
     * Validate that required inputs are present before attempting to save. The
     * method ensures at least one region is selected and at least one
     * technology has a bounding flag or range set. On failure it displays a
     * warning or error dialog using application utilities.
     *
     * @return true if inputs pass validation, false otherwise
     */
    protected boolean qaInputs() {
        TreeView<String> tree = paneForCountryStateTree.getTree();
        int errorCount = 0;
        StringBuilder message = new StringBuilder();
        try {
            if (utils.getAllSelectedRegions(tree).length < 1) {
                message.append("Must select at least one region from tree").append(vars.getEol());
                errorCount++;
            }
            ObservableList<TechBound> techList = tableTechBounds.getItems();
            boolean atLeastOneActive = false;
            for (TechBound techBound : techList) {
                if (techBound.isBound()) {
                    atLeastOneActive = true;
                    break;
                }
            }
            if (!atLeastOneActive) {
                message.append("At least one technology must be bound").append(vars.getEol());
                errorCount++;
            }
        } catch (Exception e1) {
            errorCount++;
            message.append("Error in QA of entries").append(vars.getEol());
        }
        if (errorCount > 0) {
            if (errorCount == 1) {
                utils.warningMessage(message.toString());
            } else if (errorCount > 1) {
                utils.displayString(message.toString(), "Parsing Errors");
            }
        }
        return errorCount == 0;
    }
}
