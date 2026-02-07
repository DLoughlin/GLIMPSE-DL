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
package filter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import ModelInterface.ModelGUI2.DbViewer;
import conversionUtil.ArrayConversion;
import graphDisplay.GraphDisplayUtil;
import graphDisplay.ModelInterfaceUtil;

/**
 * Handles the filter tree pane to build, manage, and display filter tree.
 * <p>
 * Author: TWU
 * Date: 1/2/2016
 */
public class FilterTreePane {
    private static final boolean playWithLineStyle = false;
    private static final String lineStyle = "Horizontal";
    private JTree tree;
    private Map<String, String> selOptions;
    private boolean existSel = false;
    private String chartName;
    private String[] unit;
    private String path;
    private JTable jtable;
    private JSplitPane sp;
    public JDialog dialog;
    private boolean debug = false;
    private DefaultMutableTreeNode originalRootNode; // Store the original root node

    /**
     * Constructs a FilterTreePane and displays the filter dialog.
     * @param chartName Chart name
     * @param unit Units array
     * @param path Path string
     * @param jtable JTable reference
     * @param sel Selected options map
     * @param sp JSplitPane reference
     */
    public FilterTreePane(String chartName, String[] unit, String path, JTable jtable, Map<String, String> sel, JSplitPane sp) {
        if (debug)
            System.out.println("jtable: " + jtable.getRowCount());
        try {
            init(chartName, unit, path, jtable, sel, sp);
            showFilter();
        } catch (Exception e) {
            System.out.println("Error launching filter panel:" + e.toString());
            dialog.dispose();
        }
    }

    /**
     * Initializes member variables.
     */
    private void init(String chartName, String[] unit, String path, JTable jtable, Map<String, String> sel, JSplitPane sp) {
        this.chartName = chartName;
        this.unit = unit.clone();
        this.path = path;
        this.jtable = jtable;
        this.sp = sp;
        if (sel != null) {
            this.selOptions = sel;
            existSel = true;
        } else {
            this.selOptions = new LinkedHashMap<String, String>();
        }
    }

    /**
     * Builds tree node names from table data.
     * @param jtable JTable reference
     * @return Array of node names
     */
    private String[] buildTreeName(JTable jtable) {
        String[] qualifier = ModelInterfaceUtil.getColumnFromTable(jtable, 5);
        String[][] listData = ArrayConversion.arrayDimReverse(ModelInterfaceUtil.getDataFromTable(jtable, 5));
        ArrayList<String[]> tableColumnUniqueValues = GraphDisplayUtil.getUniqQualifierData(qualifier, listData);
        ArrayList<String> al = buildNodesName(qualifier, tableColumnUniqueValues);
        String[] cn = al.toArray(new String[0]);
        if (debug)
            System.out.println("cn: " + Arrays.toString(cn));
        return cn;
    }

    /**
     * Builds node names for the filter tree.
     */
    private ArrayList<String> buildNodesName(String[] qualifier, ArrayList<String[]> tableColumnUniqueValues) {
        ArrayList<String> al = new ArrayList<String>();
        int cnt = ModelInterfaceUtil.getDoubleTypeColIndex(jtable);
        for (int n = cnt; n < jtable.getColumnCount() - 1; n++)
            al.add("Year|" + jtable.getColumnName(n));
        for (int k = 0; k < 1; k++) {
            for (int i = 0; i < qualifier.length; i++) {
                String q = qualifier[i].trim();
                String[] temp = tableColumnUniqueValues.get(i);
                for (int j = 0; j < temp.length; j++) {
                    String v = q + "|" + temp[j].trim();
                    al.add(v);
                    if (debug)
                        System.out.println("buildNodesName::al: " + Arrays.toString(al.toArray()));
                }
            }
        }
        return al;
    }

    /**
     * Builds the filter tree UI component.
     * @param cn Node names
     * @return JScrollPane containing the tree
     */
    private JScrollPane buildTree(String cn[]) {
        try {
            DefaultMutableTreeNode top = createNode("Filter All", "Root", null);
            tree = new JTree(top);
            createNodes(top, cn);
            // Store a deep copy of the original root node for restoring
            originalRootNode = deepCopyTree(top);
            if (existSel)
                setSelBoolean();
            tree.getSelectionModel().setSelectionMode(4); // Multiple interval selection
            // Mouse listener for node selection
            MouseListener ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selPath != null) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                        if (e.getClickCount() > 0) {
                            if (((TrNode) selectedNode.getUserObject()).isSelected) {
                                setNodeBoolean(null, false, selectedNode);
                            } else {
                                setNodeBoolean(null, true, selectedNode);
                            }
                        }
                    } else {
                        tree.addSelectionPath(selPath);
                        tree.expandPath(selPath);
                    }
                }
            };
            tree.addMouseListener(ml);
            tree.setCellRenderer(new TreeSelCellRenderer());
            tree.setLargeModel(true);
            tree.setExpandsSelectedPaths(true);
            // Set font and row height for better scaling
            tree.setRowHeight(tree.getFont().getSize() + 13);
            TreeNode root = (TreeNode) tree.getModel().getRoot();
            TreePath tPath = new TreePath(root);
            tree.expandPath(tPath);
            tree.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            tree.setAutoscrolls(true);
            tree.setScrollsOnExpand(true);
            tree.setMaximumSize(new Dimension(800, 1000));
        } catch (Exception e) {
            // Silent catch
        }
        JScrollPane jsp = new JScrollPane(tree);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (playWithLineStyle)
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        return jsp;
    }

    /**
     * Creates the filter input box and the Ok/Cancel button box for the dialog.
     * @return Box containing filter input and buttons
     */
    private Box crtBottomPanel() {
        Box vbox = Box.createVerticalBox();
        // Add 5px padding above filter input row
        vbox.add(Box.createVerticalStrut(5));
        // Filter input row
        Box filterBox = Box.createHorizontalBox();
        filterBox.add(Box.createHorizontalStrut(5)); // 5px padding left
        filterBox.add(Box.createHorizontalStrut(10)); // increased left padding before Filter label
        JLabel filterLabel = new JLabel("Filter:");
        JTextField filterField = new JTextField(20);
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        // Add filter action
        applyButton.addActionListener(e -> {
            String filterText = filterField.getText().trim().toLowerCase();
            if (filterText.isEmpty()) {
                // Show all nodes if filter is empty
                expandAll(tree, true);
            } else {
                filterTree(tree, filterText);
            }
        });
        clearButton.addActionListener(e -> {
            filterField.setText("");
            // Restore the original tree model
            if (originalRootNode != null) {
                tree.setModel(new javax.swing.tree.DefaultTreeModel(deepCopyTree(originalRootNode)));
                collapseToLevel(tree, 1);
            }
        });
        filterBox.add(filterLabel);
        filterBox.add(Box.createHorizontalStrut(5));
        filterBox.add(filterField);
        filterBox.add(Box.createHorizontalStrut(5));
        filterBox.add(applyButton);
        filterBox.add(Box.createHorizontalStrut(5));
        filterBox.add(clearButton);
        filterBox.add(Box.createHorizontalStrut(5)); // 5px padding right
        vbox.add(filterBox);
        // Add 5px padding below filter input row
        vbox.add(Box.createVerticalStrut(5));
        vbox.add(new javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL)); // horizontal separator
        // Add 5px padding above button row
        vbox.add(Box.createVerticalStrut(5));
        // Ok/Cancel button row
        vbox.add(crtButton());
        // Add 5px padding below button row
        vbox.add(Box.createVerticalStrut(5));
        return vbox;
    }

    /**
     * Creates the Ok/Cancel button box for the dialog.
     * @return Box containing buttons
     */
    private Box crtButton() {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue()); // add glue before buttons for centering
        JButton jb = new JButton("Ok");
        jb.setName("Ok");
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JButton but = (JButton) e.getSource();
                if (but.getName().trim().equals("Ok")) {
                    // If filter all unchecked give warning message
                    if (selOptions.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Select filter", "Warning", JOptionPane.WARNING_MESSAGE);
                    } else {
                        // Apply filter and close dialog
                        new FilteredTable(selOptions, chartName, unit, path, jtable, sp);
                        dialog.dispose();
                    }
                } else {
                    dialog.dispose();
                }
            }
        };
        jb.addMouseListener(ml);
        box.add(jb);
        box.add(Box.createHorizontalStrut(10)); // space between buttons
        jb = new JButton("Cancel");
        jb.setName("Cancel");
        jb.addMouseListener(ml);
        box.add(jb);
        box.add(Box.createHorizontalGlue()); // add glue after buttons for centering
        return box;
    }

    /**
     * Creates nodes for the filter tree.
     */
    private void createNodes(DefaultMutableTreeNode top, String cn[]) {
        for (int i = 0; i < cn.length; i++) {
            String temp[] = cn[i].split("\\|");
            String nodePath[] = new String[temp.length + 1];
            nodePath[0] = "Filter All";
            for (int k = 0; k < temp.length; k++)
                nodePath[k + 1] = temp[k];
            TreePath p = TreeUtil.findByName(tree, Arrays.copyOfRange(nodePath, 0, 2));
            DefaultMutableTreeNode node = null;
            if (p == null)
                node = createNode(nodePath[1], "filter", top);
            else
                node = (DefaultMutableTreeNode) p.getLastPathComponent();
            createSubNode(temp, 1, "", node);
        }
    }

    /**
     * Recursively creates subnodes for the filter tree.
     */
    private void createSubNode(String nodename[], int level, String type, DefaultMutableTreeNode top) {
        for (int j = level; j < nodename.length; j++) {
            String temp[] = Arrays.copyOfRange(nodename, 0, j + 1);
            TreePath p = TreeUtil.findByName(tree, temp);
            DefaultMutableTreeNode node = null;
            if (p == null) {
                if (j == nodename.length - 1) {
                    node = createNode(nodename[j], "Value", top);
                    if (debug)
                        System.out.println("createSubNode:sel: " + Arrays.toString(selOptions.values().toArray()));
                } else {
                    node = createNode(nodename[j], "column", top);
                }
                top = node;
            } else {
                node = (DefaultMutableTreeNode) p.getLastPathComponent();
                top = node;
            }
        }
    }

    /**
     * Creates a tree node and adds it to the parent.
     */
    private DefaultMutableTreeNode createNode(String nodename, String type, DefaultMutableTreeNode top) {
        DefaultMutableTreeNode category = null;
        if (nodename != null) {
            if (!existSel)
                category = new DefaultMutableTreeNode(new TrNode(nodename, type, true, top));
            else
                category = new DefaultMutableTreeNode(new TrNode(nodename, type, false, top));
            String k = ((TrNode) category.getUserObject()).keyStr;
            if (type.equals("Value") && !existSel)
                selOptions.put(k, k);
            if (top != null) {
                top.add(category);
                if (top.toString().contains("Year") && !Arrays.asList(Var.sectionYRange).contains(nodename.trim())) {
                    selOptions.remove(k, k);
                    ((TrNode) category.getUserObject()).isSelected = false;
                }
            }
        }
        if (debug)
            System.out.println("nodeName: " + category.toString());
        return category;
    }

    /**
     * Sets selection state for a node and its children.
     */
    private void setNodeBoolean(String[] leaf, boolean selected, DefaultMutableTreeNode node) {
        TreeNode tNode = node;
        String keyStr = ((TrNode) node.getUserObject()).keyStr;
        if (!node.isRoot()) {
            ((TrNode) node.getUserObject()).setSelected(selected);
            TreePath tpath = new TreePath(tNode);
            tree.expandPath(new TreePath(tNode));
            if (!tNode.isLeaf()) {
                DefaultMutableTreeNode pn = ((DefaultMutableTreeNode) tpath.getLastPathComponent());
                for (Enumeration<?> e = tNode.children(); e.hasMoreElements();) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
                    if (n.isLeaf()) {
                        keyStr = ((TrNode) n.getUserObject()).keyStr;
                        if (leaf != null)
                            if (Arrays.asList(leaf).contains(keyStr.trim())) {
                                selected = true;
                            } else {
                                selected = false;
                            }
                        if (selected)
                            selOptions.put(keyStr, keyStr);
                        else
                            selOptions.remove(keyStr, keyStr);
                        ((TrNode) n.getUserObject()).setSelected(selected);
                    } else {
                        setNodeBoolean(leaf, selected, n);
                    }
                }
                checkPartial(pn, ((TrNode) pn.getUserObject()).isSelected);
            } else if (tNode.isLeaf()) {
                if (leaf != null) {
                    if (Arrays.asList(leaf).contains(keyStr.trim())) {
                        selected = true;
                    } else {
                        selected = false;
                    }
                }
                if (selected)
                    selOptions.put(keyStr, keyStr);
                else
                    selOptions.remove(keyStr, keyStr);
                DefaultMutableTreeNode pn = (DefaultMutableTreeNode) node.getParent();
                checkPartial(pn, ((TrNode) pn.getUserObject()).isSelected);
            }
            if (debug)
                for (String key : selOptions.keySet())
                    System.out.println("setNodeBoolean:sel: " + selOptions.get(key));
        } else {
            selectAllBox(selected);
        }
        tree.updateUI();
    }

    /**
     * Selects or deselects all nodes in the tree.
     */
    private void selectAllBox(boolean selected) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        TreePath tPath = new TreePath(root);
        tree.expandPath(tPath);
        setPartialParentNode(false, (DefaultMutableTreeNode) root, selected);
        for (Enumeration<?> e = root.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (selected)
                setNodeBoolean(null, true, node);
            else {
                setNodeBoolean(null, false, node);
                tree.clearSelection();
            }
        }
    }

    /**
     * Checks if parent node is partially selected and updates its state.
     */
    private void checkPartial(DefaultMutableTreeNode pNode, boolean selected) {
        boolean isPartial = false;
        TreePath tPath = new TreePath(pNode);
        tree.expandPath(tPath);
        for (Enumeration<?> e = pNode.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (selected) {
                if (!((TrNode) node.getUserObject()).isSelected) {
                    isPartial = true;
                    break;
                }
            } else {
                if (((TrNode) node.getUserObject()).isSelected) {
                    isPartial = true;
                    break;
                }
            }
        }
        setPartialParentNode(isPartial, pNode, selected);
        if (pNode.getParent() != null)
            checkPartial((DefaultMutableTreeNode) pNode.getParent(), selected);
        tree.setSelectionPath(tPath);
        tree.updateUI();
    }

    /**
     * Sets partial selection state for parent node.
     */
    private void setPartialParentNode(boolean isPartial, DefaultMutableTreeNode pNode, boolean selected) {
        ((TrNode) pNode.getUserObject()).setSelected(selected);
        if (!pNode.isLeaf()) {
            if (isPartial) {
                ((TrNode) pNode.getUserObject()).setPartialSelectedForParent(true);
                ((TrNode) pNode.getUserObject()).setSelected(false);
            } else if (selected) {
                ((TrNode) pNode.getUserObject()).setPartialSelectedForParent(false);
                ((TrNode) pNode.getUserObject()).setSelected(true);
            } else {
                ((TrNode) pNode.getUserObject()).setPartialSelectedForParent(false);
                ((TrNode) pNode.getUserObject()).setSelected(false);
            }
        }
    }

    /**
     * Sets selection state for nodes based on selOptions.
     */
    public void setSelBoolean() {
        String[] leaf = selOptions.keySet().toArray(new String[0]);
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        TreePath tPath = new TreePath(root);
        tree.expandPath(tPath);
        for (Enumeration<?> e = root.children(); e.hasMoreElements();) {
            setNodeBoolean(leaf, true, (DefaultMutableTreeNode) e.nextElement());
        }
    }

    /**
     * Displays the filter dialog.
     */
    public void showFilter() {
        dialog = new JDialog();
        dialog.setTitle(chartName + " Filter");
        dialog.setSize(500, 500); // increased height by 50 pixels (was 450)
        dialog.setLocationRelativeTo(jtable);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(buildTree(buildTreeName(jtable)), BorderLayout.CENTER);
        dialog.getContentPane().add(crtBottomPanel(), BorderLayout.SOUTH);
        dialog.setVisible(true);
        DbViewer.openWindows.add(dialog);
    }

    /**
     * Returns the filter tree.
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Returns the filter dialog.
     */
    public JDialog getDialog() {
        return dialog;
    }

    // Helper method to filter tree leaves by text
    private void filterTree(JTree tree, String filterText) {
        DefaultMutableTreeNode originalRoot = (DefaultMutableTreeNode) tree.getModel().getRoot();
        DefaultMutableTreeNode filteredRoot = filterTreeBuildFilteredRoot(originalRoot, filterText);
        if (filteredRoot == null) {
            // No matches found - restore original tree and notify user
            if (originalRootNode != null) {
                tree.setModel(new javax.swing.tree.DefaultTreeModel(deepCopyTree(originalRootNode)));
            }
            JOptionPane.showMessageDialog(dialog, "No matches found for: " + filterText, "Filter Results", JOptionPane.INFORMATION_MESSAGE);
        } else {
            tree.setModel(new javax.swing.tree.DefaultTreeModel(filteredRoot));
            collapseAll(tree);
            expandAllMatching(tree, filteredRoot, new TreePath(filteredRoot));
        }
    }

    // Recursively build a filtered tree containing only matching leaves and their parent branches
    private DefaultMutableTreeNode filterTreeBuildFilteredRoot(DefaultMutableTreeNode node, String filterText) {
        DefaultMutableTreeNode filteredNode = new DefaultMutableTreeNode(node.getUserObject());
        boolean hasMatchingChild = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode filteredChild = filterTreeBuildFilteredRoot(child, filterText);
            if (filteredChild != null) {
                filteredNode.add(filteredChild);
                hasMatchingChild = true;
            }
        }
        if (node.isLeaf()) {
            String nodeStr = node.toString().toLowerCase();
            if (nodeStr.contains(filterText)) {
                return filteredNode;
            } else {
                return null;
            }
        } else {
            return hasMatchingChild ? filteredNode : null;
        }
    }

    // Collapse all nodes in the tree
    private void collapseAll(JTree tree) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root!=null) collapseAllRecursive(tree, new TreePath(root));
    }
    private void collapseAllRecursive(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            collapseAllRecursive(tree, path);
        }
        tree.collapsePath(parent);
    }

    // Expand all paths to matching leaves
    private void expandAllMatching(JTree tree, DefaultMutableTreeNode node, TreePath path) {
        if (node.isLeaf()) {
            tree.expandPath(path.getParentPath());
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                expandAllMatching(tree, child, path.pathByAddingChild(child));
            }
        }
    }

    // Helper to expand/collapse all nodes
    private void expandAll(JTree tree, boolean expand) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        expandAllRecursive(tree, new TreePath(root), expand);
    }
    private void expandAllRecursive(JTree tree, TreePath parent, boolean expand) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            expandAllRecursive(tree, path, expand);
        }
        if (expand) tree.expandPath(parent); else tree.collapsePath(parent);
    }

    // Helper: Deep copy a tree node and its children
    private DefaultMutableTreeNode deepCopyTree(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            copy.add(deepCopyTree(child));
        }
        return copy;
    }

    // Collapse tree to a specific level (root is level 0)
    private void collapseToLevel(JTree tree, int level) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        collapseToLevelRecursive(tree, new TreePath(root), 0, level);
    }
    private void collapseToLevelRecursive(JTree tree, TreePath parent, int currentLevel, int maxLevel) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        if (currentLevel < maxLevel) {
            tree.expandPath(parent);
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(child);
                collapseToLevelRecursive(tree, path, currentLevel + 1, maxLevel);
            }
        } else {
            tree.collapsePath(parent);
        }
    }
}
