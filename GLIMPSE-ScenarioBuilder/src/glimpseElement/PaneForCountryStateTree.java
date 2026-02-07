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

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

/**
 * PaneForCountryStateTree is a JavaFX VBox component that displays a tree view of regions and subregions (such as countries and states),
 * with support for selecting regions, applying preset region groups, and handling region/subregion data from files or fallbacks.
 * <p>
 * This class is used in the GLIMPSE Scenario Builder to allow users to select regions for scenario configuration.
 * It supports dynamic loading of region and subregion data, preset region groupings, and provides methods for interacting with the tree view.
 * </p>
 *
 * @author US EPA, GLIMPSE contributors
 */
public class PaneForCountryStateTree extends VBox {
    private GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    private GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    private GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    private GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private TreeView<String> treeView;
    private Label labelAppliedTo;
    private Label labelPresetRegions;
    private ComboBox<String> comboBoxPresetRegions;

    private ArrayList<String> regionList = new ArrayList<>();
    private ArrayList<String> subregionList = new ArrayList<>();
    private ArrayList<String> presetRegionList = new ArrayList<>();

    private static final String SELECT_OPTIONAL = "Select (optional)";
    private static final String WORLD = "world";
    private static final String DELIMITER_COLON = ":";
    private static final String COMMENT_CHAR = "#";

    /**
     * Constructs the PaneForCountryStateTree, loading region and subregion data and setting up the UI.
     */
    public PaneForCountryStateTree() {
        loadRegionAndSubregionData(); // Load region and subregion data from files or fallback
        setupUI(); // Set up the user interface
    }

    /**
     * Sets up the user interface components, including the tree view and preset region ComboBox.
     */
    private void setupUI() {
        this.setStyle(styles.getFontStyle());
        treeView = createTreeViewWithRegions(); // Create tree with regions
        if (vars.isGcamUSA()) treeView = addSubregionsToTreeView(); // Add subregions if GCAM-USA mode

        labelAppliedTo = utils.createLabel("Select region(s):");
        labelPresetRegions = utils.createLabel("Presets:");
        labelPresetRegions.setMinWidth(50);

        TreeItem<String> rootItem = treeView.getRoot();
        if (rootItem != null) {
            rootItem.setExpanded(true); // Expand root node
        }
        StackPane treePane = new StackPane(treeView);
        // Ensure this VBox and the internal tree pane expand to fill available space
        this.setFillWidth(true);
        this.setMaxHeight(Double.MAX_VALUE);
        this.setMaxWidth(Double.MAX_VALUE);
        treePane.setMaxHeight(Double.MAX_VALUE);
        treePane.setMinHeight(0);
        treePane.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(treePane, Priority.ALWAYS);
        // Let the TreeView itself be resizeable inside the pane
        treeView.setMaxHeight(Double.MAX_VALUE);
        treeView.setMinHeight(0);
        treeView.setMaxWidth(Double.MAX_VALUE);
        labelAppliedTo.setStyle(styles.getStyle3());

        comboBoxPresetRegions = utils.createComboBoxString();
        comboBoxPresetRegions.getItems().add(SELECT_OPTIONAL);
        comboBoxPresetRegions.getSelectionModel().select(0);

        // Set action for preset region selection
        comboBoxPresetRegions.setOnAction(e -> {
            checkPresetRegions();
        });

        populateComboBoxPresetRegions(); // Populate preset region ComboBox

        HBox presetRegionHBox = new HBox();
        presetRegionHBox.setSpacing(5.);
        // Use centralized default padding from styles
        presetRegionHBox.setPadding(styles.getDefaultPadding());
        presetRegionHBox.getChildren().addAll(labelPresetRegions, comboBoxPresetRegions);
        presetRegionHBox.setStyle(styles.getStyle2());

        this.getChildren().addAll(labelAppliedTo, treePane, presetRegionHBox);
        this.setStyle(styles.getStyle2());
        // Apply the centralized default padding to this pane as well
        this.setPadding(styles.getDefaultPadding());
        this.setAlignment(javafx.geometry.Pos.TOP_LEFT);
    }

    /**
     * Checks the selected preset region and selects the corresponding nodes in the tree view.
     */
    public void checkPresetRegions() {
        String selection = comboBoxPresetRegions.getSelectionModel().getSelectedItem();
        for (String line : presetRegionList) {
            int index = line.indexOf(DELIMITER_COLON);
            if (index > 0) {
                String name = line.substring(0, index).toLowerCase();
                if (selection != null && selection.equalsIgnoreCase(name)) {
                    String[] subregions = utils.splitString(line.substring(index + 1), ",");
                    selectNodes(subregions); // Select nodes matching preset
                }
            }
        }
    }

    /**
     * Populates the ComboBox with available preset region group names.
     */
    public void populateComboBoxPresetRegions() {
        comboBoxPresetRegions.getItems().clear();
        comboBoxPresetRegions.getItems().add(SELECT_OPTIONAL);
        comboBoxPresetRegions.getSelectionModel().select(0);

        for (String preset : presetRegionList) {
            int indexOfColon = preset.indexOf(DELIMITER_COLON);
            if (indexOfColon > -1) {
                String presetName = preset.substring(0, indexOfColon);
                comboBoxPresetRegions.getItems().add(presetName); // Add preset name to ComboBox
            }
        }
    }

    /**
     * Loads region, subregion, and preset region data from files or uses fallback values if files are unavailable.
     */
    public void loadRegionAndSubregionData() {
        String stateListFilename = vars.getSubRegionsFilename();
        String regionListFilename = vars.getRegionListFilename();
        String presetRegionListFilename = vars.getPresetRegionListFilename();

        // Load subregion (state) list
        try {
            ArrayList<String> contents = files.getStringArrayFromFile(stateListFilename, COMMENT_CHAR);
            for (String line : contents) {
                if (line.indexOf(DELIMITER_COLON) > 0) subregionList.add(line);
            }
        } catch (Exception e) {
            // Fallback if file not found
            String fallback = "USA:AL,AK,AZ,AR,CA,CO,CT,DE,DC,FL,GA,HI,ID,IL,IN,IA,KS,KY,LA,ME,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,OH,OK,OR,PA,RI,SC,SD,TN,TX,UT,VT,VA,WA,WV,WI,WY";
            subregionList.add(fallback);
        }

        // Load region list
        try {
            ArrayList<String> contents = files.getStringArrayFromFile(regionListFilename, COMMENT_CHAR);
            for (String line : contents) {
                if (line.length() > 0) {
                    ArrayList<String> tempList = utils.createArrayListFromString(line, ",");
                    for (String region : tempList) {
                        regionList.add(region);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback if file not found
            String[] fallback = {
                "USA", "Canada", "EU-15", "Europe_Non_EU", "European Free Trade Association", "Japan",
                "Australia_NZ", "Central Asia", "Russia", "China", "Middle East", "Africa_Eastern",
                "Africa_Northern", "Africa_Southern", "Africa_Western", "South Africa", "Brazil",
                "Central America and Caribbean", "Mexico", "South America_Northern", "South America_Southern",
                "Argentina", "Colombia", "Indonesia", "Pakistan", "South Asia", "Southeast Asia", "Taiwan",
                "Europe_Eastern", "EU-12", "South Korea", "India"
            };
            for (String region : fallback) regionList.add(region);
        }

        // Load preset region list
        try {
            ArrayList<String> contents = files.getStringArrayFromFile(presetRegionListFilename, COMMENT_CHAR);
            for (String line : contents) {
                if (line.length() > 0) {
                    presetRegionList.add(line);
                }
            }
        } catch (Exception e) {
            // Fallback if file not found
            String[] fallback = {
                "North America:USA,Canada,Mexico,Central America and Caribbean",
                "South America:Argentina,Brazil,Colombia,South America_Northern,South America_Southern",
                "Africa:Africa_Northern,Africa_Southern,Africa_Eastern,Africa_Western",
                "EU:EU-15,EU-12",
                "Europe:EU-15,EU-12,Europe_Eastern,European Free Trade Association,Europe_Non_EGU",
                "Asia:Japan,Central Asia,Russia,China,Middle East,Indonesia,Pakistan,South Asia,Southeast Asia,Taiwan,South Korea,India",
                "East Asia:Japan,China,Taiwan,South Korea",
                "Southeast Asia:Indonesia,Southeast Asia",
                "South Asia:Pakistan,India,South Asia",
                "West Asia:Middle East"
            };
            for (String preset : fallback) presetRegionList.add(preset);
        }
    }

    /**
     * Selects nodes in the tree view that match the provided node names.
     * @param nodes Array of node names to select.
     */
    public void selectNodes(String[] nodes) {
        TreeItem<String> root = treeView.getRoot();
        if (root == null) return;
        root.setExpanded(true); // Expand root
        ObservableList<TreeItem<String>> regionNodes = root.getChildren();
        for (TreeItem<String> regionNode : regionNodes) {
            regionNode.setExpanded(true); // Expand all region nodes
        }
        int numOfLeaves = treeView.getExpandedItemCount();
        for (String nodeName : nodes) {
            String trimmedNodeName = nodeName.trim();
            for (int j = 0; j < numOfLeaves; j++) {
                CheckBoxTreeItem<String> treeItem = (CheckBoxTreeItem<String>) treeView.getTreeItem(j);
                if (treeItem != null) {
                    String treeItemName = treeItem.getValue();
                    if (trimmedNodeName.equals(treeItemName)) {
                        treeItem.setSelected(true); // Select the node
                        expandTreeView(treeItem); // Expand to make visible
                    }
                }
            }
        }
    }

    /**
     * Recursively expands the tree view to make the selected item visible.
     * @param selectedItem The tree item to expand.
     */
    public void expandTreeView(TreeItem<String> selectedItem) {
        if (selectedItem != null) {
            expandTreeView(selectedItem.getParent()); // Recursively expand parent
            if (!selectedItem.isLeaf()) {
                selectedItem.setExpanded(true); // Expand this node if not a leaf
            }
        }
    }

    /**
     * Returns the TreeView component for external access.
     * @return The TreeView of regions and subregions.
     */
    public TreeView<String> getTree() {
        return treeView;
    }

    /**
     * Adds subregions to the tree view if GCAM-USA mode is enabled.
     * @return The updated TreeView with subregions.
     */
    public TreeView<String> addSubregionsToTreeView() {
        TreeItem<String> rootItem = treeView.getRoot();
        if (rootItem == null) return treeView;
        ObservableList<TreeItem<String>> regionNodes = rootItem.getChildren();
        for (String line : subregionList) {
            int index = line.indexOf(DELIMITER_COLON);
            if (index > 0) {
                String region = line.substring(0, index);
                String[] subregions = utils.splitString(line.substring(index + 1), ",");
                CheckBoxTreeItem<String> regionNode = null;
                // Find the region node to add subregions to
                for (TreeItem<String> node : regionNodes) {
                    if (node.getValue().equals(region)) {
                        regionNode = (CheckBoxTreeItem<String>) node;
                        break;
                    }
                }
                if (regionNode != null) {
                    for (String subregionName : subregions) {
                        CheckBoxTreeItem<String> subregionItem = new CheckBoxTreeItem<>(subregionName);
                        regionNode.getChildren().add(subregionItem); // Add subregion as child
                    }
                }
            }
        }
        return treeView;
    }

    /**
     * Creates a TreeView with region nodes as children of the root node.
     * @return The constructed TreeView.
     */
    public TreeView<String> createTreeViewWithRegions() {
        CheckBoxTreeItem<String> world = new CheckBoxTreeItem<>(WORLD); // Root node
        for (String region : regionList) {
            world.getChildren().add(new CheckBoxTreeItem<>(region)); // Add each region as child
        }
        TreeView<String> treeView = new TreeView<>(world);
        treeView.setCellFactory(CheckBoxTreeCell.<String>forTreeView()); // Use CheckBox cells
        return treeView;
    }

    /**
     * Returns a list of selected nodes in the tree view.
     * @return ArrayList of selected node names.
     */
    public ArrayList<String> getSelectedNodes() {
        ArrayList<String> selectedNodes = new ArrayList<>();
        TreeView<String> tv = getTree();
        if (tv == null) return selectedNodes;
        ObservableList<TreeItem<String>> selectedItems = tv.getSelectionModel().getSelectedItems();
        for (TreeItem<String> item : selectedItems) {
            if (item != null) {
                selectedNodes.add(item.toString()); // Add selected node name
            }
        }
        return selectedNodes;
    }

    /**
     * Adds an event handler to all leaf nodes in the tree view.
     * @param ev The event handler to add.
     */
    public void addEventHandlerToAllLeafs(EventHandler ev) {
        TreeView<String> tv = getTree();
        if (tv != null) {
            addEventHandlerRecursively(tv.getRoot(), ev); // Add handler recursively
        }
    }

    /**
     * Recursively adds an event handler to all children of the given node.
     * @param node The parent node.
     * @param ev The event handler to add.
     */
    protected void addEventHandlerRecursively(TreeItem<?> node, EventHandler ev) {
        if (node == null) return;
        for (TreeItem<?> child : node.getChildren()) {
            child.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), ev); // Add handler
            addEventHandlerRecursively(child, ev); // Recurse
        }
    }

    /**
     * Recursively counts the number of items in the tree starting from the given node.
     * @param node The root node to start counting from.
     * @return The total number of items in the tree.
     */
    protected int countItemsInTree(TreeItem<?> node) {
        if (node == null) return 0;
        int count = 1; // Count this node
        for (TreeItem<?> child : node.getChildren()) {
            count += countItemsInTree(child); // Add children recursively
        }
        return count;
    }
}
