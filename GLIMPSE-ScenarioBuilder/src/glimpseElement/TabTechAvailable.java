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
 * TabTechAvailable provides the user interface and logic for managing technology availability
 * in the GLIMPSE Scenario Builder. This tab allows users to filter, select, and configure
 * technology bounds and availability for scenario components.
 *
 * <p>
 * <b>Usage:</b> Instantiated as a tab in the scenario builder. Extends {@link PolicyTab} and implements {@link Runnable}.
 * </p>
 *
 * <p>
 * <b>Main Features:</b>
 * <ul>
 *   <li>Displays a table of available technologies with options to set bounds and years.</li>
 *   <li>Allows filtering by technology type and text search.</li>
 *   <li>Supports selection of all or a range of technologies for scenario constraints.</li>
 *   <li>Handles both nested and non-nested technology structures for scenario export.</li>
 *   <li>Integrates with a region/country selection tree for scenario targeting.</li>
 *   <li>Provides methods for saving scenario components and shareweights to file.</li>
 *   <li>Supports loading of scenario component content from file.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Dependencies:</b>
 * <ul>
 *   <li>JavaFX for UI components</li>
 *   <li>GLIMPSE utility classes for file, style, and variable management</li>
 *   <li>{@link TechBound} for technology data representation</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Thread Safety:</b> Not thread-safe. Should be used on the JavaFX Application Thread.
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
     * Constructor for TabTechAvailable. Sets up the UI, event handlers, and initializes the technology bounds table.
     *
     * @param title  the title of the tab
     * @param stageX the JavaFX Stage
     */
    public TabTechAvailable(String title, Stage stageX) {
        this.setStyle(styles.getFontStyle());
        setupUIControls();
        setComponentWidths();
        setupUILayout(title);
    }

    /**
     * Sets up UI controls and event handlers for the tab.
     * Includes filter fields, year fields, selection buttons, and table columns.
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
     * Sets up the layout of the tab using HBox and VBox.
     * @param title The title of the tab
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
     * Sets up a boolean column (Never? or Range?) for the table.
     * @param column The TableColumn to set up
     * @param isAll  True if this is the 'Never?' column, false for 'Range?'
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
     * Sets up a year column (First or Last) for the table.
     * @param column The TableColumn to set up
     * @param isFirst True if this is the 'First' year column, false for 'Last'
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
     * Sets up the combo box for type filtering.
     */
    private void setupComboBoxType() {
        comboBoxCategoryFilter.getItems().addAll(vars.getCategoriesFromTechBnd());
        comboBoxCategoryFilter.getSelectionModel().selectFirst();
    }

    /**
     * Hides nested subsector from the technology list for display.
     * @return ObservableList<TechBound> with modified tech names
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
     * Gets the matching line from the original technology list for a given line.
     * Used for mapping displayed tech names to original tech names for export.
     * @param line The line to match
     * @return The matching line from the original list
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
     * Updates the first and last years for all visible technology bounds.
     * @param firstYr The first year
     * @param lastYr The last year
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
     * Selects all visible items in the table.
     * Sets the 'Never?' bound for all visible technologies.
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
     * Selects a range of visible items in the table.
     * Sets the 'Range?' bound for all visible technologies.
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
     * Adds filtering and sorting to the technology bounds table.
     * Filters by category and text, and sorts results.
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
     * Runs background tasks or updates for this tab. Implementation of Runnable interface.
     * Triggers scenario component save on the JavaFX thread.
     */
    @Override
    public void run() {
        Platform.runLater(this::saveScenarioComponent);
    }

    /**
     * Generates a suggested filename for the scenario component based on sector and regions.
     * Format: [Sector]_fxDMD_[Regions].csv, with special handling for long region lists.
     *
     * @return Suggested filename string
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
     * Saves the current scenario component to file, including both nested and non-nested technology bounds.
     * Builds file content and metadata for export.
     */
    @Override
    public void saveScenarioComponent() {
        if (qaInputs()) {
            TreeView<String> tree = this.paneForCountryStateTree.getTree();
            String[] listOfSelectedLeaves = utils.getAllSelectedRegions(tree);
            listOfSelectedLeaves = utils.removeUSADuplicate(listOfSelectedLeaves);
            String states = utils.returnAppendedString(listOfSelectedLeaves);

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
                        firstYear = "3000";
                        lastYear = "3005";
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
     * Generates metadata content for the scenario component, including technology bounds and selected regions.
     *
     * @param tree the TreeView containing region selections
     * @return a String containing the metadata content
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
     * Loads scenario component data from file and updates the UI accordingly.
     * Parses content lines and updates technology bounds and region selections.
     *
     * @param content the list of content lines to load
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
     * Gets the list of technology bounds from the variables.
     * Reads technology info and builds the initial list for the table.
     * @return ObservableList<TechBound>
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
     * Saves the current scenario component shareweight data to file.
     * This method should be implemented to export shareweight constraints for technologies.
     * (Not implemented in this version)
     */
    public void saveScenarioComponentShareweight() {
        // Implementation for saving shareweight scenario data
        // (Not implemented in this version)
    }

    /**
     * Validates user input before saving the scenario component. Checks for at least one region and one technology bound.
     * Displays warning messages if validation fails.
     *
     * @return true if all required fields are valid, false otherwise
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
