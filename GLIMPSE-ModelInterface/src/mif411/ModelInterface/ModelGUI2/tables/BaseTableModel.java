/*
* LEGAL NOTICE
* This computer software was prepared by Battelle Memorial Institute,
* hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830
* with the Department of Energy (DOE). NEITHER THE GOVERNMENT NOR THE
* CONTRACTOR MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* Copyright 2012 Battelle Memorial Institute.  All Rights Reserved.
* Distributed as open-source under the terms of the Educational Community 
* License version 2.0 (ECL 2.0). http://www.opensource.org/licenses/ecl2.php
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
*/
package ModelInterface.ModelGUI2.tables;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

//import java.sql.Statement;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.basex.util.Prop;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.DOMmodel;
import ModelInterface.ModelGUI2.Documentation;

public abstract class BaseTableModel extends AbstractTableModel {
	protected Vector activeRows;
	protected Document doc;
	protected ArrayList wild;
	protected Map tableFilterMaps;
	protected String title;
	protected Documentation documentation;
	protected String units;
	protected boolean remove1975;
	protected TableSorter sortedTable;
	//private java.util.List<String> defaultYearList;

	// stuff for filtering
	// can i move these somewhere
	protected int currFilter;
	protected String[] currKeys;
	
	protected String tableTypeString;

	/** 
	 * Default Constuctor
	 */
	public BaseTableModel() {
		// TODO: Create a prefrence to control removing 
		// the year1975.  Currently this value is just 
		// taken from the global properties file but it 
		// should be able to change while the Interface is 
		// running perhaps even on a per-query basis.
		if(InterfaceMain.getInstance() != null) {
			Properties globalProperties = InterfaceMain.getInstance().getProperties();
			String remove75Str;
			globalProperties.setProperty("remove1975", 
					remove75Str = globalProperties.getProperty("remove1975", "true"));
			remove1975 = Boolean.parseBoolean(remove75Str);
		} else {
			remove1975 = false;
		}
		sortedTable = new TableSorter(this);
	}

	/**
	 * Constuctor initializes some necessary data members
	 * @param tp Not required in the BaseTableModel, but will be in derived class to create xpath
	 * 	  doc DOM document, necessary so that we can run an Xpath query on it.
	 * 	  tableTypeString used to display which type of table the user is looking at
	 */
	public BaseTableModel(TreePath tp, Document doc, String tableTypeString, Documentation documentationIn) {
		this.doc = doc;
		this.tableTypeString = tableTypeString;
		this.documentation = documentationIn;
		this.title = ((DOMmodel.DOMNodeAdapter)tp.getLastPathComponent()).getNode().getNodeName();
		final BaseTableModel thisTableModel = this;
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();

        if(parentFrame != null ) {
            parentFrame.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    if(e.getPropertyName().equals("Control") || (e.getPropertyName().equals("Table") &&
                            !thisTableModel.equals(e.getNewValue()))) {
                        System.out.println("New Table: "+e.getNewValue());
                        System.out.println("This Table: "+thisTableModel);
                        System.out.println("Stoped listening for filter");
                        parentFrame.removePropertyChangeListener(this);
                    } else if(e.getPropertyName().equals("Filter")) {
                        try {
                            // is there a better way than to do this..
                            if(thisTableModel instanceof NewDataTableModel) {
                                //((NewDataTableModel)thisTableModel).filterData();
                                InterfaceMain.getInstance().showMessageDialog(
                                    "This table does not support filtering",
                                    "Table Filter Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                thisTableModel.filterData();
                            }
                        } catch (UnsupportedOperationException uoe) {
                            InterfaceMain.getInstance().showMessageDialog(
                                "This table does not support filtering",
                                "Table Filter Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
	}

	/**
	 * Get the cell renderer at specified position
	 * @param row renderer for row
	 * @param col renderer for col
	 * @return the renderer required to view the cell
	 */
	public abstract TableCellRenderer getCellRenderer(int row, int pos);
	
	/** 
	 * Creates a chart from the data in the table.
	 * @author jlurz
	 */
	public abstract JFreeChart createChart(int rowAt, int colAt);
	
	/**
	 * abstract the deriving class needs to implement this for flip functionality, params used 
	 * mostly for MultiTable to know which table to pass the call on to
	 * @param row the row over which flip was called
	 * 	  col the col over which flip was called
	 */
	public abstract void flip(int row, int col);

	/**
	 * The deriving class needs to implement this so that it gets the nodes at given rows/cols and gets their annotation from
	 * the documentation
	 * @param row rows positions of the nodes to be annotated
	 * 	  col cols positions of the nodes to be annotated 
	 * 	  documentation the documentation where to look up annotation information
	 */
	public abstract void annotate(int[] rows, int[] cols, Documentation documentation);

	/**
	 * abstract since deriving tables would most likely have diffrent method for building a table depending on what the purpose of the table is
	 * @param xpe the Xpath expression that it will run to get nodes to build the table
	 */
	protected abstract void buildTable(XPathExpression xpe);
	
	/**
	 * this funtion is used to convert a treepath to and xpath expression, a treepath is an
	 * array of nodes which represent the parent path of the desired leaf node
	 * @param tp the TreePath to be converted
	 *        currNode current node the last node (leaf node that was clicked) !! is this really needed
	 *        flag used to chacnge the strictness, 0 won't specify any attrributes, 1 will specify all attrubutes except any wild nodes
	 */
 	protected XPathExpression treePathtoXPath(TreePath tp, Node currNode, int flag) {
	   // attempts to put the tp in form /nodeName[@attributeName=attrubuteValue]/childNode
	   StringBuffer pathStr = new StringBuffer("/");
           Object[] path = tp.getPath();
           Node tempNode;
	   currNode = ((DOMmodel.DOMNodeAdapter)path[path.length-1]).getNode();
           for (int i = 0; i < path.length-1; i++) {
	           tempNode= ((DOMmodel.DOMNodeAdapter)path[i]).getNode();
		   if(flag == 0) {
                   	pathStr.append(tempNode.getNodeName()).append("/");
		   } else if(flag == 1) { 
			   pathStr.append(tempNode.getNodeName()); 
			   Vector attrs = getAttrsNoWild(tempNode);
			   if(attrs.size() > 0) {
				   pathStr.append("[");
			   }
			   for(int j=0; j < attrs.size(); j++) {
				   pathStr.append("(@").append(((Node)attrs.get(j)).getNodeName())
					   .append("='").append(((Node)attrs.get(j)).getNodeValue()).append("')");
				   if(j < attrs.size()-1) {
					   pathStr.append(" and ");
				   } else { 
					   pathStr.append("]");
				   }
			   }
			   pathStr.append("/");
		   }
           }
           //pathStr = "/" + pathStr + currNode.getNodeName();
           pathStr.append(currNode.getNodeName());
           if (flag == 1 &&currNode.hasAttributes() && !getTextData(currNode).equals("")) {
		   if(flag == 0) {
                   	pathStr.append("[@").append(getOneAttrVal(currNode)).append("]");
		   } else if(flag == 1) {
			   Vector attrs = getAttrsNoWild(currNode);
			   for(int j=0; j < attrs.size(); j++) {
				   pathStr.append("[@").append(((Node)attrs.get(j)).getNodeName())
					   .append("='").append(((Node)attrs.get(j)).getNodeValue()).append("']");
			   }
			   pathStr.append("/text()");
		   }
           }
           else if (flag == 1 && currNode.hasAttributes()) {
		   if (flag == 0) {
                   	pathStr.append("[@").append(getOneAttrVal(currNode)).append("]/text()");
		   } else if(flag == 1) {
			   Vector attrs =getAttrsNoWild(currNode);
			   for(int j=0; j < attrs.size(); j++) {
				   pathStr.append("@").append(((Node)attrs.get(j)).getNodeName()).append("='")
					   .append(((Node)attrs.get(j)).getNodeValue()).append("'");
			   }
			   pathStr.append("/text()");
		   }
           }
           else {
                   pathStr.append("/text()");
           }
	   System.out.println(pathStr.toString());
       try {
           return XPathFactory.newInstance().newXPath().compile(pathStr.toString());
       } catch(XPathExpressionException e) {
           e.printStackTrace();
           return null;
       }
	}

  /**
   * Gets the first attrubute name, value pair, and returns it in a string with the format 
   * name=value
   * @param node the node which you want to get the attrubute from
   * @return a string in the format name=value
   */
  public String getOneAttrVal(Node node) {
	  NamedNodeMap nodeMap = node.getAttributes();
	  if(nodeMap.item(0).getNodeName().equals("fillout")) {
		  return getOneAttrVal(node, 1);
	  }
	  return nodeMap.item(0).getNodeName() +"="+ nodeMap.item(0).getNodeValue();
	  //return /*((Element)node).getAttribute(*/nodeMap.item(0).getNodeValue()/*)*/;
  }

  /**
   * Gets the attrubute name, value pair in the requested position and returns it in a string with the format 
   * name=value
   * @param node the node which you want to get the attrubute from
   *        pos indicates which attrubute is wanted
   * @return a string in the format name=value
   */
  public String getOneAttrVal(Node node, int pos) {
	  NamedNodeMap nodeMap = node.getAttributes();
	  //return nodeMap.item(pos).getNodeName() +"="+ nodeMap.item(pos).getNodeValue();
	  return /*((Element)node).getAttribute(*/nodeMap.item(pos).getNodeValue()/*)*/;
  }

  /**
   * Gets all the attrubutes for the node and returns the ones that are no wild in a vector
   * @param node the node which attributes will come form
   * @return the vector off all the attributes
   */
  public Vector getAttrsNoWild(Node node) {
	  if( ((((String)wild.get(0)).matches(".*[Ss]ector") || ((String)wild.get(1)).matches(".*[Ss]ector"))) && node.getNodeName().equals("subsector") ) {
		  return new Vector();
	  }

	  if(node.getNodeName().equals((String)wild.get(0)) || node.getNodeName().equals((String)wild.get(1)) ) { 
		 return new Vector();
	  }
	  NamedNodeMap nodeMap = node.getAttributes();
	  Node tempNode;
	  Vector ret = new Vector();
	  for(int i = 0; i < nodeMap.getLength(); i++) {
		  tempNode = nodeMap.item(i);
		  if(tempNode.getNodeName().indexOf(':') == -1) {
		  	ret.add(tempNode);
		  }
	  }
	  return ret;
  }

  /**
   * Looks to find a text node child of a node, and returns it's contents
   * @param node the node used to look for a text child
   * @return String with the value of the text node if found, else empty string
   */
  public String getTextData(Node node) {
	  NodeList nl = node.getChildNodes();
	  for (int i = 0; i < nl.getLength(); i++) {
		  if (nl.item(i).getNodeType() == Node.TEXT_NODE) {
			  return nl.item(i).getNodeValue();
		  }
	  }
	  return "";
  }

  /**
   * Creates a dialog box so that the user can select the 2 axis for a table, and returns a list of the 2 nodes, which will be used for the wild
   * @param path the path which was selected in the tree, so we can get a list of canidates for the 2 axes.
   * @return an ArrayList of the 2 nodes selected from the dialog
   */
  protected ArrayList chooseTableHeaders( TreePath path ){
	final ArrayList selected = new ArrayList(2);

	final Object[] itemsObjs = path.getPath();
	
	String[] items = new String[ itemsObjs.length ];
	for(int i=0; i<itemsObjs.length; i++){
		Node currNode = ((DOMmodel.DOMNodeAdapter)itemsObjs[i]).getNode();
		if( currNode.hasAttributes() ){ // print out first attribute, just to help
			NamedNodeMap nodeMap = currNode.getAttributes();
			items[i] = currNode.getNodeName() + " (" + nodeMap.item(0).getNodeName() + " = " + nodeMap.item(0).getNodeValue() + ")"; 
		}else{
			items[i] = currNode.getNodeName();
		}
	}
	final JList list = new JList(items);
	list.setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    
	JScrollPane scrollingList = new JScrollPane(list);
    		
    final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
    final JDialog filterDialog = new JDialog(parentFrame, tableTypeString + " for '" + ((DOMmodel.DOMNodeAdapter)itemsObjs[itemsObjs.length-1]).getNode().getNodeName() + "'. Please choose two headers:", true);
    filterDialog.setSize(500,400);
    // Center over the main frame instead of top-left
    filterDialog.setLocationRelativeTo(parentFrame);
    filterDialog.setResizable(false);
	
	final JButton nextButton = new JButton(" Finished With Selection ");
	nextButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int[] selectedIndices = list.getSelectedIndices();
			if( selectedIndices.length == 2 ){
				selected.add( itemsObjs[ selectedIndices[0] ] );
				selected.add( itemsObjs[ selectedIndices[1] ] );
				filterDialog.dispose();
			}else{
				// make user try again ..
				InterfaceMain.getInstance().showMessageDialog("Error: You must choose exactly two (2)!", null, JOptionPane.ERROR_MESSAGE);
			}
		}
	});

	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(nextButton);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));

	final JLabel listLabel = new JLabel();
	listLabel.setHorizontalAlignment(JLabel.LEFT);

	JPanel listPane = new JPanel();
	listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
	listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	listPane.add(Box.createVerticalGlue());
	listPane.add(listLabel);
	listPane.add(Box.createVerticalStrut(10));
	JScrollPane listScroll = new JScrollPane(list);
	listScroll.setPreferredSize(new Dimension(150, 750));
	listPane.add(listScroll);
	listPane.add(Box.createVerticalStrut(10));
	listPane.add(new JSeparator(SwingConstants.HORIZONTAL));

	Container filterContent = filterDialog.getContentPane();
	//filterContent.add(new JSeparator(SwingConstants.HORIZONTAL));
	filterContent.add(listPane, BorderLayout.CENTER);
	filterContent.add(buttonPane, BorderLayout.PAGE_END);
	filterDialog.setContentPane(filterContent);
	//filterDialog.show();
	filterDialog.setVisible(true);
   		
  	return selected; //arraylist with the two selected nodes
  }

        /**
	 * Creates a dialog and lists attrubutes of the nodes which can be filtered, while updating
	 * the filterMaps to keep track of what the user has selected
	 */
	public void filterData() {
		// so i can make oldNumRows final and it won't crash
		if (activeRows == null) {
			activeRows = new Vector();
		}
		// create some copies, so if the use cancels we can reset the old value
		final int oldNumRows = activeRows.size();
		currKeys = new String[0];
		final Map tempFilterMaps = (Map)((LinkedHashMap)tableFilterMaps).clone();

		// a vector of nodeName which can be filtered
		// we go through this list backwards so that nodes apear in the same order they do
		// in the tree
		final Vector possibleKeys = new Vector(tempFilterMaps.keySet());
		currFilter = possibleKeys.size()-1;
		String title = "Filter Table";
		if (possibleKeys.isEmpty()) {
			return;
		}
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
		final JDialog filterDialog = new JDialog(parentFrame, title, true);
		filterDialog.setSize(500,400);
		// Center over the main frame instead of top-left
		filterDialog.setLocationRelativeTo(parentFrame);
		filterDialog.setResizable(false);

		final JList list = new JList();
		final JLabel listLabel = new JLabel();
		listLabel.setHorizontalAlignment(JLabel.LEFT);
		updateList(list, listLabel, (String)possibleKeys.get(currFilter), tempFilterMaps);

		final String cancelTitle = " Cancel ";

		final JButton cancelButton = new JButton(cancelTitle);
		final JButton backButton = new JButton(" < Back ");
		final JButton nextButton = new JButton(" Next > ");

		backButton.setMnemonic(KeyEvent.VK_B);
		nextButton.setMnemonic(KeyEvent.VK_N);
		// adds the listener for the cancel button, and when we have reached the end it will change it's
		// title to finished, which tells use to go ahead and update the filters, and call doFilter
	        cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!cancelButton.getText().equals(cancelTitle)) {
					updateFilters(tempFilterMaps, list, (String)possibleKeys.get(currFilter));
					tableFilterMaps = tempFilterMaps;
					doFilter(possibleKeys);
					if (oldNumRows < activeRows.size()) {
						fireTableRowsInserted(oldNumRows, activeRows.size());
					} else if (oldNumRows > activeRows.size()) {
						fireTableRowsDeleted(0, activeRows.size());
					} else {
						fireTableRowsUpdated(0,activeRows.size());
					}
				}
				//exit this dialog..
				filterDialog.dispose();
				//filterDialog.hide();
			}
		});

            // when we go back, if we can, get the previous nodeName, and load up all its attributes
	    // make the cancel buttons title cancel again if it was on finished
	    backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters(tempFilterMaps, list, (String)possibleKeys.get(currFilter));
				//currFilter--;
				currFilter++;
				updateList(list, listLabel, (String)possibleKeys.get(currFilter), tempFilterMaps);
				if (!nextButton.isEnabled()) {
					nextButton.setEnabled(true);
					cancelButton.setText(cancelTitle);
				}
				if (currFilter == possibleKeys.size()-1) {
					backButton.setEnabled(false);
				}
			}
		});

            // when we go next, if we can, get the next nodeName, and load up all its attributes
	    // make the cancel buttons title finished if we have reached the end of the list of nodeNames 
	    nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters(tempFilterMaps, list, (String)possibleKeys.get(currFilter));
				//currFilter++;
				currFilter--;
				updateList(list, listLabel, (String)possibleKeys.get(currFilter), tempFilterMaps);
				if (!backButton.isEnabled()) {
					backButton.setEnabled(true);
				}
				if (currFilter == 0) {
					nextButton.setEnabled(false);
					cancelButton.setText("Finished");
				}
			}
		});

	        // set display properties
		JPanel buttonPane = new JPanel();
	    	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	    	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    	buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(backButton);
		backButton.setEnabled(false);
		if (possibleKeys.size() == 1) {
			nextButton.setEnabled(false);
			cancelButton.setText("Finished");
		}

	    	buttonPane.add(nextButton);
	    	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	    	buttonPane.add(cancelButton);

		JPanel listPane = new JPanel();
		listPane.setLayout( new BoxLayout(listPane, BoxLayout.Y_AXIS));
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		listPane.add(Box.createVerticalGlue());
		listPane.add(listLabel);
		listPane.add(Box.createVerticalStrut(10));
		JScrollPane listScroll = new JScrollPane(list);
		listScroll.setPreferredSize(new Dimension(150, 750));
		listPane.add(listScroll);
		listPane.add(Box.createVerticalStrut(10));
		listPane.add(new JSeparator(SwingConstants.HORIZONTAL));

		Container filterContent = filterDialog.getContentPane();
		//filterContent.add(new JSeparator(SwingConstants.HORIZONTAL));
		filterContent.add(listPane, BorderLayout.CENTER);
		filterContent.add(buttonPane, BorderLayout.PAGE_END);
		filterDialog.setContentPane(filterContent);
		//filterDialog.show();
		filterDialog.setVisible(true);
	}

	/**
	 * Update the filterMaps of the users selections
	 * @param tempFilterMaps the current set of filterMaps being changed by user selections
	 *        list the list of values the user can select
	 *        key the current key in the filterMaps being edited
	 */
	protected void updateFilters(Map tempFilterMaps, JList list, String key) {
		int[] selectedKeys = list.getSelectedIndices();
		int j = 0;
		// since the list only returns the selected ones makes things complicated
		// need to go through and change boolean values of the maps, kind of like an XOR
		for (int i = 0; i < currKeys.length; i++) {
			// clean this up... maybe
			if (((Boolean)((Map)tempFilterMaps.get(key)).get(currKeys[i])).booleanValue() && (j >= selectedKeys.length || i != selectedKeys[j])) {
				((Map)tempFilterMaps.get(key)).put(currKeys[i], new Boolean(false));
			} else if (!((Boolean)((Map)tempFilterMaps.get(key)).get(currKeys[i])).booleanValue() && (j < selectedKeys.length && i == selectedKeys[j])) {
				((Map)tempFilterMaps.get(key)).put(currKeys[i], new Boolean(true));
			}
			if (j < selectedKeys.length && i == selectedKeys[j]) {
				j++;
			}
		}
	}

	/**
	 * Update the JList which is displaced to the user for selection
	 * @param list the list which will change
	 *        listLable the nodeName that these attributes belong to
	 *        key the current nodeName, used as the current key into the filter map
	 *        tempFilterMaps the set of filterMaps being edited by the user
	 */
	protected void updateList(JList list, JLabel listLabel, String key, Map tempFilterMaps) {
		Map tempMap = (Map)tempFilterMaps.get(key);
		Vector tempVector = new Vector();
		listLabel.setText("Filter "+key);
		currKeys = (String[])tempMap.keySet().toArray(new String[0]);
		list.setListData(currKeys);
		// check the maps to see which ones are true and add it to the list of selected
		for (int i = 0; i < currKeys.length; i++) {
			if (((Boolean)tempMap.get(currKeys[i])).booleanValue()) {
				tempVector.addElement(new Integer(i));
			}
		}
		int[] selected = new int[tempVector.size()];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = ((Integer)tempVector.get(i)).intValue();
		}
		tempMap = null;
		tempVector = null;
		list.setSelectedIndices(selected);
	}

	/**
	 * abstract since most the of the deriving tables will store the data differently
	 * @param possibleFilters the vector nodeNames that had valid attributes for filtering
	 */
	protected abstract void doFilter(Vector possibleFilters);

	public abstract void exportToExcel(HSSFSheet sheet, HSSFWorkbook wb, HSSFPatriarch dp);

	protected TableCellRenderer getDocumentationRenderer() {
		final TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
		return new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus,
					int row, int col) {
				Component ret = defaultRenderer.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, col);
				if(documentation != null && documentation.hasDocumentation(getNodeAt(row, col))) {
					ret.setBackground(isSelected? Color.BLUE : Color.CYAN);
				} else {
					ret.setBackground(isSelected? Color.BLUE : Color.WHITE);
				}
				return ret;
			}
		};
	}
	
	/**
	 * Get the backing node in the cell specified
	 * @param row which row
	 * @param col which col
	 * @return the node desired or null if not given a valid position
	 */
	protected abstract Node getNodeAt(int row, int col);

	/**
	 * Determine a prefered dimension size to create an image from a JFreeChart.  The problem is
	 * when there are too many series the legend gets too big an pushes out the chart itself.
	 * @param chart a JFreeChart with data that would be used to create an image from
	 * @return A dimension that will be able to show all of the chart and legend
	 */
	public Dimension getChartDimensions(JFreeChart chart) {
		//System.out.println("Num legend items: "+chart.getPlot().getLegendItems().getItemCount());
		//int numItems = chart.getPlot().getLegendItems().getItemCount();
		int legendSize = 0;
		for(Iterator it = chart.getPlot().getLegendItems().iterator(); it.hasNext(); ) {
			/*
			String temp = ((org.jfree.chart.LegendItem)it.next()).getLabel();
			System.out.println("Lengend item: "+temp);
			*/
			legendSize += ((org.jfree.chart.LegendItem)it.next()).getLabel().length();
		}
		//System.out.println("Total length: "+legendSize);
		if(legendSize <= 500) {
			//System.out.println("Returning: 350, "+350);
			return new Dimension(350, 350);
		} else {
			//System.out.println("Returning: 350, "+(350+((legendSize-500)/2)));
			return new Dimension(350, 350+((legendSize-500)/2));
		}
	}

	/**
	 * Set the active rows of this table.  This method is essentially setting the filter, and should
	 * usually not be called directly.
	 * @param newActive the new Vector of indices which are active
	 */
	public void setActiveRows(Vector newActive) {
		activeRows = newActive;
		// correct event to fire?
		fireTableStructureChanged();
	}

	/**
	 * Used to export a table to text. There is a newline between rows and
	 * a tab between columns.
     * @param delimiter The text delimiter to use to sperate values.
	 * @return A String which represents this table.
	 */
       public String exportToText(char delimiter) {
	       String lineEnding = System.getProperty("line.separator");
           final int regionColNumber = 1;
           boolean isGlobal = true;
           String value;
	       StringBuilder ret = new StringBuilder();
           value = title;
           if(value.indexOf(delimiter) != -1) {
               value = '"'+value+'"';
           }
	       ret.append(value).append(lineEnding);
	       for(int i = 0; i < getColumnCount() && isGlobal; ++i) {
               value = getColumnName(i);
               if(value.equals("region")) {
                   isGlobal = false;
               }
           }
	       for(int i = 0; i < getColumnCount(); ++i) {
               value = getColumnName(i);
               if(isGlobal && i == regionColNumber) {
                   ret.append("region").append(delimiter);
               }
               if(value.indexOf(delimiter) != -1) {
                   value = '"'+value+'"';
               }
		       ret.append(value).append(delimiter);
	       }
	       ret.append(lineEnding);
	       for(int row = 0; row < getRowCount(); ++row) {
		       for(int col = 0; col < getColumnCount(); ++col) {
                   if(isGlobal && col == regionColNumber) {
                       ret.append("Global").append(delimiter);
                   }
                   value = sortedTable.getValueAt(row, col).toString();
                   if(value.indexOf(delimiter) != -1) {
                       value = '"'+value+'"';
                   }
			       ret.append(value).append(delimiter);
		       }
		       ret.append(lineEnding);
	       }
	       return ret.toString();
       }
       
       public void print(PrintWriter pw) {
    	   pw.print(exportToText(','));
       }

       private String[] getRemainingIdentifiers(JTable table) {

    	    String[] identifiers = new String[table.getColumnCount()];
    	    int k = 0;

    	    for(int i = 0; i < table.getColumnCount(); i++) {
    	            identifiers[i] = table.getColumnName(i);
    	        
    	    }

    	    return identifiers;
    	}

       public JTable getAsSortedTable() {
	       JTable ret = new JTable(sortedTable);
	       String[][] data = new String[ret.getRowCount()][ret.getColumnCount()];

	       for(int i = 0; i < ret.getRowCount(); i++) {
	           for(int j = 0; j < ret.getColumnCount(); j++) {

	                   if(ret.getValueAt(i, j) == null) {
	                	   ret.setValueAt("", i, j);
	                   }
	                   data[i][j] = ret.getValueAt(i, j).toString();
	               
	           }
	       }

	       DefaultTableModel modeltable = new DefaultTableModel(data, getRemainingIdentifiers(ret));
	        //now add units column
	       ret.setModel(modeltable);
	       sortedTable.setTableHeader(ret.getTableHeader());
	       return ret;
       }

       public List<String> getSelectedYearList() {
    	   
    	  List<String> yearArrayList = new ArrayList<String>();
    	  String yearList=null;
    	  
   	   		if(InterfaceMain.getInstance() != null) {
   	   	   		   Properties globalProperties = InterfaceMain.getInstance().getProperties();

					yearList = globalProperties.getProperty("selectedYearList");
					System.out.println("BaseTableModel784: Using selectedYearsList from properties: "+yearList);
			       if(yearList!=null && yearList.length()>0) {
						String[] setYearListArr = InterfaceMain.splitListProperty(yearList);
 				      
					       for(String year : setYearListArr ) {
						       yearArrayList.add(year);
 					       
 				       }
 			       }			       
			       if((yearArrayList==null)||(yearArrayList.size()==0)) {
			    	   yearArrayList=getAllYearList();
			       }
			       		       		       
			       if((yearArrayList==null)||(yearArrayList.size()==0)) {
			    	   yearArrayList.clear();
			    	   yearArrayList.add("2015");
			    	   yearArrayList.add("2021");
			    	   yearArrayList.add("2025");
			    	   yearArrayList.add("2030");
			    	   yearArrayList.add("2035");
			    	   yearArrayList.add("2040");
			    	   yearArrayList.add("2045");
			    	   yearArrayList.add("2050");
			       
			    	   for (String year : yearArrayList) {
			    		   yearList = (yearList == null) ? year : yearList + ";" + year;
			    	   }
			    	   
			    	   if(InterfaceMain.getInstance() != null) {
			    		   InterfaceMain.getInstance().getProperties().setProperty("selectedYearList", yearList);
			    	   }	
			       }
		       }
		       
	       return yearArrayList;
       }
       
       public List<String> getAllYearList() {
    	   
    	  List<String> yearArrayList = new ArrayList<String>();
    	  String yearList=null;
    	  
   	   		if(InterfaceMain.getInstance() != null) {
   	   	   		   Properties globalProperties = InterfaceMain.getInstance().getProperties();

					yearList = globalProperties.getProperty("allYearList");
					//System.out.println("BaseTableModel830: Using allYearList from properties: "+yearList);
			       if(yearList!=null && yearList.length()>0) {
						String[] setYearListArr = InterfaceMain.splitListProperty(yearList);
 				      
					       for(String year : setYearListArr ) {
						       yearArrayList.add(year);
 					       
 				       }
 			       }			       
			       		       		       
			       if((yearArrayList==null)||(yearArrayList.size()==0)) {
			    	   yearArrayList.clear();
			    	   yearArrayList.add("2015");
			    	   yearArrayList.add("2021");
			    	   yearArrayList.add("2025");
			    	   yearArrayList.add("2030");
			    	   yearArrayList.add("2035");
			    	   yearArrayList.add("2040");
			    	   yearArrayList.add("2045");
			    	   yearArrayList.add("2050");
			    	   yearArrayList.add("2055");
			    	   yearArrayList.add("2060");
			    	   yearArrayList.add("2065");
			    	   yearArrayList.add("2070");
			    	   yearArrayList.add("2075");
			    	   yearArrayList.add("2080");
			    	   yearArrayList.add("2085");
			    	   yearArrayList.add("2090");
			    	   yearArrayList.add("2095");
			    	   yearArrayList.add("2100");

			    	   for (String year : yearArrayList) {
			    		   yearList = (yearList == null) ? year : yearList + ";" + year;
			    	   }
			    	   
			    	   if(InterfaceMain.getInstance() != null) {
			    		   InterfaceMain.getInstance().getProperties().setProperty("allYearList", yearList);
			    	   }	
			       }
		       }
		       
	       return yearArrayList;
       }
       
}