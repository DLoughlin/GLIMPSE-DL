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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DbViewer;

/**
 * Handles the filter tree pane to build, manage, and display year filters.
 * <p>
 * Author: TWU<br>
 * Created: 1/2/2016
 */
public class FilterTreePaneYears {
    private static final boolean playWithLineStyle = false;
    private static final String lineStyle = "Horizontal";
    private JTree tree;
    private boolean existSel = false;
    private JTable jtable;
    public JDialog dialog;
    private boolean debug = false;
    final InterfaceMain main = InterfaceMain.getInstance();
    private List<String> filterSelectedYears;

    /**
     * Constructs the filter tree pane and displays the filter dialog.
     */
    public FilterTreePaneYears() {
        try {
            showFilter();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (dialog != null) {
                dialog.dispose();
            }
        }
    }

    /**
     * Builds the tree UI for year filtering.
     * @return JScrollPane containing the tree
     */
    private JScrollPane buildTree() {
        try {
            filterSelectedYears = new ArrayList<>(Arrays.asList(InterfaceMain.splitListProperty(
					main.getProperties().getProperty("selectedYearList", "") )));
            DefaultMutableTreeNode top = createNode("Filter All", "Root", null);
            tree = new JTree(top);
            createNodes(top);
            // Mouse listener for node selection
            MouseListener ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selPath != null) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                        if (e.getClickCount() > 0) {
                            TrNode trNode = (TrNode) selectedNode.getUserObject();
                            if (trNode.isSelected) {
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
            tree.setRowHeight(20);
            tree.setLargeModel(true);
            tree.setExpandsSelectedPaths(true);
            TreeNode root = (TreeNode) tree.getModel().getRoot();
            TreePath tPath = new TreePath(root);
            tree.expandPath(tPath);
            tree.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            tree.setAutoscrolls(true);
            tree.setScrollsOnExpand(true);
            tree.setMaximumSize(new Dimension(800, 1100));
        } catch (Exception e) {
            System.out.println("Error getting data for selected year list: " + e.toString());
        }
        JScrollPane jsp = new JScrollPane(tree);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (playWithLineStyle)
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        return jsp;
    }

    /**
     * Creates the OK and Cancel buttons for the dialog.
     * @return Box containing the buttons
     */
    private Box crtButton() {
        Box box = Box.createHorizontalBox();
        JButton jb = new JButton("Ok");
        jb.setName("Ok");
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JButton but = (JButton) e.getSource();
                if (but.getName().trim().equals("Ok")) {
                    filterSelectedYears.sort(null); // Sort numerically
                    main.setProperty("selectedYearList", String.join(";", filterSelectedYears));
                }
                dialog.dispose();
            }
        };
        jb.addMouseListener(ml);
        box.add(jb);
        jb = new JButton("Cancel");
        jb.setName("Cancel");
        jb.addMouseListener(ml);
        box.add(jb);
        return box;
    }

    /**
     * Creates year nodes under the root node.
     * @param top Root node
     */
    private void createNodes(DefaultMutableTreeNode top) {
        List<String> years = new ArrayList<>(Arrays.asList(InterfaceMain.splitListProperty(
				main.getProperties().getProperty("allYearList", "") )));
		for (String year : years) {
            createNode(year, "filter", top);
        }
    }

    /**
     * Creates a tree node for a given year or root.
     * @param nodename Node name
     * @param type Node type
     * @param top Parent node
     * @return Created node
     */
    private DefaultMutableTreeNode createNode(String nodename, String type, DefaultMutableTreeNode top) {
        DefaultMutableTreeNode category = null;
        if (nodename != null) {
            boolean selected = "Root".equals(type) || filterSelectedYears.contains(nodename);
            category = new DefaultMutableTreeNode(new TrNode(nodename, type, selected, top));
            if (top != null) {
                top.add(category);
            }
        }
        if (debug && category != null)
            System.out.println("nodeName: " + category.toString());
        return category;
    }

    /**
     * Sets the selection state for a node and its children.
     * @param leaf Array of leaf node names to select
     * @param selected Selection state
     * @param node Node to update
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
                        if (leaf != null) {
                            selected = Arrays.asList(leaf).contains(keyStr.trim());
                        }
                        if (selected) {
                            if (!filterSelectedYears.contains(keyStr)) filterSelectedYears.add(keyStr);
                        } else {
                            filterSelectedYears.remove(keyStr);
                        }
                        ((TrNode) n.getUserObject()).setSelected(selected);
                    } else {
                        setNodeBoolean(leaf, selected, n);
                    }
                }
                checkPartial(pn, ((TrNode) pn.getUserObject()).isSelected);
            } else if (tNode.isLeaf()) {
                if (leaf != null) {
                    selected = Arrays.asList(leaf).contains(keyStr.trim());
                }
                if (selected) {
                    if (!filterSelectedYears.contains(keyStr)) filterSelectedYears.add(keyStr);
                } else {
                    filterSelectedYears.remove(keyStr);
                }
                DefaultMutableTreeNode pn = (DefaultMutableTreeNode) node.getParent();
                checkPartial(pn, ((TrNode) pn.getUserObject()).isSelected);
            }
            if (debug) {
                System.out.println("setNodeBoolean:sel: " + String.join(";", filterSelectedYears));
            }
        } else {
            selectAllBox(selected);
        }
        tree.updateUI();
    }

    /**
     * Selects or deselects all nodes.
     * @param selected Selection state
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
     * Checks if a parent node is partially selected and updates its state.
     * @param pNode Parent node
     * @param selected Selection state
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
     * Sets the partial selection state for a parent node.
     * @param isPartial Is partially selected
     * @param pNode Parent node
     * @param selected Selection state
     */
    private void setPartialParentNode(boolean isPartial, DefaultMutableTreeNode pNode, boolean selected) {
        TrNode trNode = (TrNode) pNode.getUserObject();
        trNode.setSelected(selected);
        if (!pNode.isLeaf()) {
            if (isPartial) {
                trNode.setPartialSelectedForParent(true);
                trNode.setSelected(false);
            } else {
                trNode.setPartialSelectedForParent(false);
                trNode.setSelected(selected);
            }
        }
    }

    /**
     * Sets selection state for all nodes based on selected years.
     */
    public void setSelBoolean() {
        String[] leaf = filterSelectedYears.toArray(new String[0]);
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        TreePath tPath = new TreePath(root);
        tree.expandPath(tPath);
        for (Enumeration<?> e = root.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            String keyStr = ((TrNode) node.getUserObject()).keyStr;
            boolean isSelected = Arrays.asList(leaf).contains(keyStr.trim());
            if (isSelected) {
                setNodeBoolean(leaf, true, node);
            }
        }
    }

    /**
     * Shows the filter dialog for year selection.
     */
    public void showFilter() {
        filterSelectedYears = new ArrayList<>(Arrays.asList(InterfaceMain.splitListProperty(
				main.getProperties().getProperty("selectedYearList", "") )));
        dialog = new JDialog();
        dialog.setTitle("Year Filter");
        dialog.setSize(300, 500);
        dialog.setLocationRelativeTo(jtable);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(buildTree(), BorderLayout.CENTER);
        dialog.getContentPane().add(crtButton(), BorderLayout.SOUTH);
        setSelBoolean();
        dialog.setVisible(true);
        DbViewer.openWindows.add(dialog);
    }

    /**
     * Gets the filter tree.
     * @return JTree
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Gets the filter dialog.
     * @return JDialog
     */
    public JDialog getDialog() {
        return dialog;
    }
}
