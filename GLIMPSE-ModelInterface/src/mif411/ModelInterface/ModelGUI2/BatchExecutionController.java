package ModelInterface.ModelGUI2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.common.DataPair;

public class BatchExecutionController {
    private DbViewer dbViewer;

    public BatchExecutionController(DbViewer dbViewer) {
        this.dbViewer = dbViewer;
    }

    public void runBatch(Node command) {
		/**
		 * Executes a batch command for running queries on the XMLDB database.
		 * <p>
		 * This method parses the provided XML Node command, which should contain
		 * instructions for running batch queries. It supports the following actions:
		 * <ul>
		 * <li>Opening the database file</li>
		 * <li>Loading query files or inline queries</li>
		 * <li>Selecting scenarios to run</li>
		 * <li>Configuring batch options (single sheet, charts, split runs, replace
		 * results, cores to use)</li>
		 * <li>Running the batch and exporting results</li>
		 * </ul>
		 * The method validates required files, opens the database if needed, loads
		 * scenarios and queries, and executes the batch run. If any required
		 * information is missing or an error occurs, it prints the stack trace and
		 * closes the database if it was opened.
		 *
		 * @param command The XML Node containing batch command instructions.
		 */
		// Get properties and batch options
		Properties prop = InterfaceMain.getInstance().getProperties();
		final String singleSheetCheckBoxPropName = "batchQueryResultsInDifferentSheets";
		final String includeChartsPropName = "batchQueryIncludeCharts";
		final String splitRunsPropName = "batchQuerySplitRunsInDifferentSheets";
		final String replaceResultsPropName = "batchQueryReplaceResults";
		final String coresToUsePropertyName = "coresToUse";
		Runtime.getRuntime().availableProcessors();
		final int defaultNumCoresToUse = Integer.valueOf(prop.getProperty(coresToUsePropertyName, Integer.toString(2)));
		prop.setProperty(coresToUsePropertyName, Integer.toString(defaultNumCoresToUse));

		NodeList children = command.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node child = children.item(i);
			// Only process element nodes
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			String actionCommand = ((Element) child).getAttribute("name");
			if (actionCommand == null) {
				continue;
			}
			// Only handle XMLDB Batch File commands
			if ("XMLDB Batch File".equals(actionCommand)) {
				File queryFile = null;
				Node queriesNode = null;
				File outFile = null;
				String dbFile = null;
				boolean didOpenDB = false;
				List<DataPair<String, String>> scenariosNames = new ArrayList<>();
				// Batch options
				boolean singleSheet = Boolean.parseBoolean(prop.getProperty(singleSheetCheckBoxPropName, "false"));
				boolean includeCharts = Boolean.parseBoolean(prop.getProperty(includeChartsPropName, "true"));
				boolean splitRuns = Boolean.parseBoolean(prop.getProperty(splitRunsPropName, "false"));
				boolean replaceResults = Boolean.parseBoolean(prop.getProperty(replaceResultsPropName, "false"));
				int numCoresToUse = defaultNumCoresToUse;
				// Parse child nodes for batch file configuration
				NodeList fileNameChildren = child.getChildNodes();
				for (int j = 0; j < fileNameChildren.getLength(); ++j) {
					Node fileNode = fileNameChildren.item(j);
					if (fileNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					String nodeName = fileNode.getNodeName();
					switch (nodeName) {
					case "queryFile":
						queryFile = new File(fileNode.getTextContent());
						break;
					case "queries":
						queriesNode = fileNode;
						break;
					case "outFile":
						outFile = new File(fileNode.getTextContent());
						break;
					case "xmldbLocation":
						dbFile = fileNode.getTextContent();
						break;
					case "scenario":
						scenariosNames.add(new DataPair<>(((Element) fileNode).getAttribute("name"),
								((Element) fileNode).getAttribute("date")));
						break;
					case singleSheetCheckBoxPropName:
						singleSheet = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case includeChartsPropName:
						includeCharts = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case splitRunsPropName:
						splitRuns = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case replaceResultsPropName:
						replaceResults = Boolean.parseBoolean(fileNode.getTextContent());
						break;
					case coresToUsePropertyName:
						try {
							numCoresToUse = Integer.parseInt(fileNode.getTextContent());
						} catch (NumberFormatException ex) {
							numCoresToUse = defaultNumCoresToUse;
						}
						break;
					default:
						System.out.println("Unknown tag: " + nodeName);
						break;
					}
				}
				try {
					// Validate required files
					if ((queryFile == null && queriesNode == null) || outFile == null || dbFile == null) {
						throw new Exception("Not enough information provided to run batch query.");
					}
					// Open DB if needed
					if (XMLDB.getInstance() == null) {
						XMLDB.openDatabase(dbFile);
						didOpenDB = true;
					}
					// Get scenarios from DB
					Vector<ScenarioListItem> scenariosInDb = DbViewer.getScenarios();
					Vector<ScenarioListItem> scenariosToRun = new Vector<>();
					if (scenariosNames.isEmpty() && !scenariosInDb.isEmpty()) {
						scenariosToRun.add(scenariosInDb.lastElement());
					} else {
						for (DataPair<String, String> currScn : scenariosNames) {
							String scen = currScn.getKey();
							String date = currScn.getValue();
							if (date.isEmpty()) {
								date = null;
							}
							ScenarioListItem found = ScenarioListItem.findClosestScenario(scenariosInDb, scen, date);
							if (found != null) {
								scenariosToRun.add(found);
							}
						}
					}
					if (scenariosToRun.isEmpty()) {
						throw new Exception("Could not find scenarios to run.");
					}
					// Load queries from file or inline
					if (queryFile != null && queriesNode != null) {
						throw new Exception("Setting both a queryFile and inline queries is not allowed.");
					} else if (queryFile != null) {
						queriesNode = dbViewer.readQueries(queryFile).getDocumentElement();
					} else {
						dbViewer.filterNodes(queriesNode, dbViewer.new ParseFilter());
					}
					// Get queries to run
					final NodeList res = (NodeList) XPathFactory.newInstance().newXPath().evaluate("./aQuery",
							queriesNode, XPathConstants.NODESET);
					final int numQueries = res.getLength();
					if (numQueries == 0) {
						throw new Exception("Could not find queries to run.");
					}
					// Prepare scenarios to run
					final Vector<Object[]> toRunScns = new Vector<>();
					if (!splitRuns) {
						toRunScns.add(scenariosToRun.toArray());
					} else {
						for (ScenarioListItem scn : scenariosToRun) {
							Object[] temp = new Object[1];
							temp[0] = scn;
							toRunScns.add(temp);
						}
					}
					// Get regions (excluding Global)
					Vector<String> allRegions = dbViewer.getRegions();
					allRegions.remove("Global");
					// Run the batch window
					// Register total queries to run so the progress UI can track them
					int totalToRegister = numQueries * toRunScns.size();
					for (int qi = 0; qi < totalToRegister; ++qi) {
						DbViewer.registerNewQuery();
					}
					BatchWindow runner = new BatchWindow(outFile, toRunScns, allRegions, singleSheet, includeCharts,
							numQueries, res, replaceResults, numCoresToUse);
					if (runner != null) {
						runner.waitForFinish();
					}
				} catch (Exception e) {
					// Print stack trace for errors
					e.printStackTrace();
				} finally {
					// Close DB if it was opened in this method
					if (didOpenDB) {
						XMLDB.closeDatabase();
					}
				}
			} else {
				// Unknown command type
				System.out.println("Unknown command: " + actionCommand);
			}
		}
	}

    /**
	 * Runs a batch query from a file and saves the output.
	 * 
	 * @param batchFile The batch query file.
	 * @param outFile   The output file.
	 */
	public void batchQuery(File batchFile, File outFile) {
		try {
			final Document doc = dbViewer.readQueries(batchFile);
			final NodeList res = (NodeList) XPathFactory.newInstance().newXPath().evaluate("./aQuery",
					doc.getDocumentElement(), XPathConstants.NODESET);
			final int numQueries = res.getLength();
			if (numQueries == 0) {
				throw new Exception("Could not find queries to run.");
			}
			Vector<Object[]> toRunScns = new Vector<Object[]>();
			toRunScns.add(dbViewer.scnList.getSelectedValues());
			Vector<String> allRegions = dbViewer.getRegions();
			allRegions.remove("Global");
			new BatchWindow(outFile, toRunScns, allRegions, false, true, numQueries, res, false, 2);
		} catch (Exception e) {
		 e.printStackTrace();
		}
	}
}
