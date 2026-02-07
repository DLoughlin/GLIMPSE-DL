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
package ModelInterface.ModelGUI2.xmldb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.MainOptions;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Open;
import org.basex.io.IO;
import org.basex.io.IOFile;
import org.basex.io.out.PrintOutput;
import org.basex.io.serial.Serializer;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.DBNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.queries.QueryGenerator;

public class XMLDB {
    /**
     * The static instance of the XMLDB.
     */
	private static XMLDB xmldbInstance = null;

    /**
     * The database context need to run commands on the DB.
     */
    private Context context = null;
    // single object to serialize write/update operations against the BaseX context
    private final Object writeLock = new Object();
    /**
     * Public accessor for the BaseX context. Other classes may need the
     * Context to execute BaseX commands. Returning the Context allows
     * callers to run commands without exposing the field directly.
     *
     * @return the BaseX Context or null if the DB is not open.
     */
    public Context getContext() {
        return context;
    }

    /**
     * The name of the currently open database.
     */
    private String contName = null;

    /**
     * Was context adopted. If the context was adopted then we will
     * not attempt to close it when it is time to close the DB.
     */
    private boolean wasContextAdopted;

	/**
	 * Gets the instance of the xml database.
	 * @warning If the database is not open it will return null, ideally it
	 * 	would throw an exception.
	 * @return The instance of the xml database.
	 */
	public static XMLDB getInstance() {
		// WARNING: not thread safe
		if(xmldbInstance == null) {
			// should throw an exception..
			System.out.println("The database is not open.");
		}
		return xmldbInstance;
	}
	/**
	 * Opens a new xml database at the given location.
	 * @param dbLocation The location of the database to open.
	 * @throws Exception If a database is already open or there was an error opening the database.
	 */ 
	public static void openDatabase(String dbLocation) throws Exception {
		// WARNING: not thread safe
		if(xmldbInstance != null) {
			throw new Exception("Could not open databse because "+xmldbInstance.contName+
					" is still open");
		}
        xmldbInstance = new XMLDB(dbLocation);
    }
	/**
	 * Opens a new xml database at the given location and potentially an existing context.
	 * @param dbLocation The location of the database to open.
	 * @param contextIn An existing daabase context to use an existing connection and support
     *                  using an in memory database that has been loaded already.  If null a
     *                  new default context will be created to open the data base at location dbLocation
     *                  from disk.
	 * @throws Exception If a database is already open or there was an error opening the database.
	 */ 
	public static void openDatabase(Context contextIn) throws Exception {
		// WARNING: not thread safe
		if(xmldbInstance != null) {
			throw new Exception("Could not open databse because "+xmldbInstance.contName+
					" is still open");
		}
		try {
		xmldbInstance = new XMLDB(contextIn);
		} 
		catch (FileSystemNotFoundException e) {
		    System.err.println("BaseX initialization issue. Likely OK. Continuing...");
		    throw e;
		}
	}

	/**
	 * Closes the database. Note that all errors on close are ignored.
	 */
	public static void closeDatabase() {
		// WARNING: not thread safe
		if(xmldbInstance != null) {
			try {
                // ensure no concurrent writers are active while closing
                synchronized(xmldbInstance.writeLock) {
                    if(!xmldbInstance.wasContextAdopted) {
                        new Close().execute(xmldbInstance.context);
                    }
                }
             } catch (BaseXException e) {
                 e.printStackTrace();
 			} finally {
                xmldbInstance.context = null;
                xmldbInstance.contName = null;
 				xmldbInstance = null;
 			}
 		}
	}

	/**
	 * Private constructor to be used with factories/static inits
	 * @param db the path to the database
	 * @throws Exception If the DB cannot be opened.
	 */
	private XMLDB(String db) throws Exception {
        wasContextAdopted = false;
		if(!openDB(db)) {
			throw new Exception ("Could not open DB");
		}
    }
	
	/**
	 * Public constructor using a context to initialize.
	 * @param contextIn The built context.
	 * @throws Exception Exception if the DB cannot be opened.
	 */
	public XMLDB(Context contextIn) throws Exception {
        wasContextAdopted = true;
        context = contextIn;
	}
	
	/**
	 * Opens the DB given a path.  Checks if the path is valid and sets in the 
	 * preferences file.
	 * 
	 * @param dbPath Path to the DB to open.  Can be absolulte or relative
	 * @return whether or not DB opened sucessfully
	 * @throws Exception Throws from downstream children.
	 */
	private boolean openDB(String dbPath) throws Exception {
        // We need to seperate the path to the DB and the container name (last name in the path)
        File dbLocationFile = new File(dbPath).getAbsoluteFile();
        // The path may be a relative path so we must convert it to absolute here.
        String path = dbLocationFile.getParentFile().getCanonicalPath();
        // BaseX is particular about valid container names and will change them without warning
        // so we check explicitly.
        String containerNameUnmodified = dbLocationFile.getName();
        
        System.out.println("Container name="+containerNameUnmodified);
        
        //workaround
        contName = containerNameUnmodified;
        //Dan: This code seems to be generating false warnings, commenting out for now
        //contName = IO.get( containerNameUnmodified ).dbName();
        if( !containerNameUnmodified.equals( contName ) ) {
            System.out.println( "WARNING: "+containerNameUnmodified+" contains invalid characters, it has been changed to: "+contName );
            System.out.println( "WARNING: container name '"+containerNameUnmodified+
                    "' contains invalid characters, it has been changed to: '"+contName+"'" );
        }

        // The db Context will check the org.basex.DBPATH property when it is created
        // and use it as the base path for finding all collections/containers
        System.setProperty("org.basex.DBPATH", path);

        context = new Context();
        // Set some default behaviors such as no indexing etc
        // TODO: experiment with these
        context.options.set(MainOptions.ATTRINDEX, false);
        context.options.set(MainOptions.FTINDEX, false);
        context.options.set(MainOptions.UPDINDEX, false);
        context.options.set(MainOptions.CHOP, true);
        context.options.set(MainOptions.ADDCACHE, true);
        context.options.set(MainOptions.INTPARSE, true);
       // context.options.set(MainOptions.AUTOOPTIMIZE, true);

        System.out.println("Database path="+path);
        
        
        //first check if directory exists
        boolean DbDirectoryExists = dbLocationFile.exists();
        //next see if any basex files (by checking extension) are in that directory
        boolean oneFileHasBasexExt=false;
        String warningMessage=null;
        if(DbDirectoryExists) {
        	File[] extCheck=dbLocationFile.listFiles();
        	for(int curF=0;curF<extCheck.length;curF++) {
        		if(extCheck[curF].getName().endsWith(".basex")) {
        			oneFileHasBasexExt=true;
        			break;
        		}
        	}
        	warningMessage="Could not open the database.  Attempt to create a new one?\nWARNING doing so will delete all files in the directory.";
        } else {
        	warningMessage="The specified directory does not exist, would you like to initialize a new database?";
        }
      
        
        System.out.println("Database exists? "+DbDirectoryExists);
        
        if (DbDirectoryExists && oneFileHasBasexExt) {
        	try {
               new Open(contName).execute(context);
        	} catch (Exception e) {
        		//show a message
        	 System.out.println("Cannot open database: "+contName);
        	 System.out.println("  error: "+e);
        	 return false;
        		
        	}
        } else {
        	try {
        		int ans = InterfaceMain.getInstance().showConfirmDialog(
        	        	warningMessage,
        				"Open DB Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.NO_OPTION);
        		if (ans == JOptionPane.YES_OPTION) {
        			new CreateDB(contName).execute(context);
        		}
        	} catch (Exception e) {
        		System.out.println("Cannot create database: "+contName);
        	 System.out.println("  error: "+e); 
        	 return false;
        	}
        }
        return true;
        
	}
	public void addFile(String fileName, int myNum, int totalNum) {
        // try to generate a unique name for this file to be added
        SimpleDateFormat format = new SimpleDateFormat("ddMMyy-hhmmss");
        addFile("run_"+format.format(new Date())+".xml", fileName,myNum,totalNum);
	}

	/**
	 * This method adds a new file to the DB.  The progress info method is called
	 * in a seperate thread (and window) to keep track.  Note that
	 * execute method from BaseX blocks, so we use that as an easy gate to know
	 * when to close.
	 * @param docName the docuname to use
	 * @param fileName the file to import
	 */
    public void addFile(String docName, String fileName, int myCount, int total) {
		// Delegate to new overload without an owner (maintain API)
		addFile(docName, fileName, myCount, total, null);
	}

	/**
	 * Overloaded addFile that accepts an owner dialog so the import dialog can be
	 * positioned relative to the owner (e.g. Manage Database dialog).
	 */
	public void addFile(String docName, String fileName, int myCount, int total, final JDialog owner) {
		try {
			final Add myAdd = new Add(docName, fileName);
			final JProgressBar progBarOverall = new JProgressBar(0, total);
			progBarOverall.setValue(myCount);
			final File f = new File(fileName);
			final JLabel overallLabel = new JLabel("Importing " + f.getName());
			final JProgressBar StepOneProgBar = new JProgressBar(0, 100);
			final JLabel StepOneLabel = new JLabel("Copying to Database . . .");
			StepOneProgBar.setIndeterminate(true);

			final JDialog jd = XMLDB.createProgressBarGUIAddScenario("Importing Scenarios into Database",
					overallLabel, progBarOverall,
					StepOneLabel, StepOneProgBar,
					owner);
			jd.setSize(400, 250);

			final Runnable incProgress = new Runnable() {
                @Override
				public void run() {
					int lastVal = -1;
					int curActiveProg = 1;
					// Show the dialog on the EDT. If it's modal, this will present it and
					// run a nested event loop so the dialog receives input until dismissed.
					SwingUtilities.invokeLater(new Runnable() {
					    @Override
						public void run() {
							jd.setModal(true);
							jd.setVisible(true);
							jd.toFront();
							jd.requestFocusInWindow();
						}
					});
					// small initial pause to let UI settle
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					while (myAdd.progressInfo() < 1) {
						StepOneProgBar.setIndeterminate(false);
						int myVal = (int) (myAdd.progressInfo() * 100.0);
						if (curActiveProg == 1) {
							StepOneProgBar.setValue(myVal / 2);
							if (myVal < lastVal) {
								curActiveProg = 2;
							}
						}
						if (curActiveProg == 2) {
							StepOneProgBar.setValue(50 + (myVal / 2));
						}
						lastVal = myVal;
						jd.repaint();
						try {
							Thread.yield();
							Thread.sleep(500);
						} catch (Exception e) {
							System.out.println("Error sleeping on import: " + e.toString());
						}
					}
					StepOneLabel.setText("Writing to disk . . . ");
					StepOneProgBar.setIndeterminate(true);
				}
			};
			Thread progressThread = new Thread(incProgress);
			progressThread.start();

			try {
				// serialize Add execution to avoid concurrent BaseX writes interfering
				synchronized(writeLock) {
					myAdd.execute(context);
				}
 			} catch (Exception e) {
 				System.out.println("Problem running import: " + e.toString());
 				System.out.println("For file " + fileName);
 				JOptionPane.showMessageDialog(jd,
 					"The import for " + fileName + " failed.  Note that this may leave your database in a brokens state.");
 			}

			// Ensure dialog is closed when work completes
			if (jd.isVisible()) {
				SwingUtilities.invokeLater(new Runnable() {
				    @Override
					public void run() {
						jd.setVisible(false);
						jd.dispose();
					}
				});
			} else {
				jd.dispose();
			}

		} catch (Exception e) {
		 e.printStackTrace();
		}
	}
	public void removeDoc(String docName) {
		final int maxAttempts = 4;
		int attempt = 0;
		while (true) {
			attempt++;
			try {
				// synchronize deletes with other writers
				synchronized(writeLock) {
					System.out.println("Removing :"+docName+" attempt "+attempt);
					new Delete(docName).execute(context);
				}
				break; // success
			} catch (BaseXException | RuntimeException e) {
				String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
				// if this looks like the transient lock-file issue, retry a few times
				if (attempt >= maxAttempts || !(msg.contains("lock file does not exist") || msg.contains("lock"))) {
					e.printStackTrace();
					break;
				}
				// short exponential backoff then retry
				try {
					Thread.sleep(200L * attempt);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}
	
	/**
	 * Export a document to a text file.
	 * @param aDocName Name of the document to export.
	 * @param aLocation Location at which to save the document.
	 * @return Whether the scenario was saved successfully.
	 */
	/*wanted type IOFile... changed from File*/
	public boolean exportDoc(final String aDocName, final File aLocation) {
		try {
            final int[] docPre = context.data().resources.docs(aDocName).toArray();
            if(docPre.length != 1) {
            	System.err.println(aDocName+" matched "+docPre.length+" docs; != 1");
            	return false;
            }
            
            IOFile iof = new IOFile(aLocation);
            
            final PrintOutput out = new PrintOutput(iof/*aLocation*//*.getPath()*/);
            final Serializer outputter = Serializer.get(out);
            outputter.serialize(new DBNode(context.data(), docPre[0]));
            outputter.close();
            out.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getContainer() {
		return contName;
	}

	public QueryProcessor createQuery(String query, Vector<String> queryFunctions, 
			Object[] scenarios, Object[] regions) {
		return createQuery(QueryBindingFactory.getQueryBinding(query, queryFunctions, contName),
				scenarios, regions);
	}
	public QueryProcessor createQuery(QueryGenerator qg, Object[] scenarios, Object[] regions) {
        return createQuery(qg, scenarios, regions, null);
    }
	public QueryProcessor createQuery(QueryGenerator qg, Object[] scenarios, Object[] regions, DbProcInterrupt interrupt) {
		return createQuery(QueryBindingFactory.getQueryBinding(qg, contName), scenarios,
				regions, interrupt);
	}
	public QueryProcessor createQuery(QueryBinding queryBinding, Object[] scenarios, Object[] regions) {
        return createQuery(queryBinding, scenarios, regions, null);
    }
	public QueryProcessor createQuery(QueryBinding queryBinding, Object[] scenarios, Object[] regions, DbProcInterrupt interrupt) {
		String queryComplete = queryBinding.bindToQuery(scenarios, regions);
		System.out.println("About to perform query: "+queryComplete);
        QueryProcessor ret = new QueryProcessor(queryComplete, context);
        if(interrupt != null) {
            interrupt.setProc(ret);
        }
        return ret;
	}

    /*
	private static ThreadLocal<Map<Integer, Map<String, String>>> attrCache = 
        new ThreadLocal<Map<Integer, Map<String, String>>>() {
            protected Map<Integer, Map<String, String>> initialValue() {
                final int cacheSize = 500;
                return new LRUCacheMap<Integer, Map<String, String>>(cacheSize);
            }
        };
        */
	//private static Map<String, Map<String, String>> attrCache = new TreeMap<String, Map<String, String>>();
	public static Map<String, String> getAttrMap(Node node) {
		final NamedNodeMap attrs = node.getAttributes();
        final Map<String, String> ret = new TreeMap<String, String>();
        for(int i = 0; i < attrs.getLength(); ++i) {
				Node temp = attrs.item(i);
				ret.put(temp.getNodeName(), temp.getNodeValue());
        }
        return ret;
	}
	public static Map<String, String> getAttrMapWithCache(Node node) {
        /*
		int nodeId = node.getNode().id;
		Map<String, String> ret = attrCache.get().get(nodeId);
		// null means a cahce miss so we will need to go back to the database
		if(ret == null) {
			ret = getAttrMap(node);
			// put the result in the cache for the next time
			attrCache.get().put(nodeId, ret);
		}
		return ret;
        */
        return getAttrMap(node);
	}
	public static String getAllAttr(Map<String, String> attrMap, List<String> showAttrList) {
		String ret;
		if((ret = attrMap.get("name")) != null) {
			ret = "," + ret;
		} else {
			ret = "";
		}
		for(Iterator<Map.Entry<String, String>> it = attrMap.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, String> currAttr = it.next();
			if((!currAttr.getKey().equals("name") &&
					!currAttr.getKey().equals("type") && 
					!currAttr.getKey().equals("unit") && 
					!currAttr.getKey().equals("year")) || (showAttrList != null && showAttrList.contains(currAttr.getKey()))) {
				ret += ","+currAttr.getKey()+"="+currAttr.getValue();
			}
		}
		return !ret.isEmpty() ? ret.substring(1) : ret;
	}
	public String getQueryFunctionAsDistinctNames() {
		return "declare function local:distinct-node-names ($args as node()*) as xs:string* { fn:distinct-values(for $nname in $args return fn:local-name($nname)) }; local:distinct-node-names";
	}
	/**
	 * Sets the value of the given node to the passed in string content back into the
	 * database.
	 * @param val The node for which to set the value.
	 * @param content The new value to set.
	 */
	public void setValue(ANode val, String content) {
		// simple XQuery update query to replace the value of the given val
		final String setValueXQuery = "declare variable $newValue as xs:string external; replace value of node self::node() with $newValue";
        QueryProcessor queryProc = new QueryProcessor(setValueXQuery, context);
        try {
            // ensure updates are serialized with other DB writers
            synchronized(writeLock) {
                // set val as the context so that it knows which node to update
                queryProc.context(val);
                // setting the new value through the variable is safer and allows us to use
                // the same query
                queryProc.bind("newValue", content, "xs:string");
                // not expecting anything to be in the results
                queryProc.iter();
            }
        } catch(QueryException e) {
            e.printStackTrace();
        } finally {
            queryProc.close();
        }
	}
	/**
	 * Inserts a single query cache onto the cache document.
     * @param doc The cache document to add the cache value.
	 * @param hash The hash value to use as an id
	 * @param content The new value to set.
	 */
	public void updateSingleCacheValue(ANode doc, int hash, String content) {
		// simple XQuery update query to replace the value of the given val
		final String setValueXQuery = "declare variable $hashId as xs:integer external; declare variable $newCacheValue as xs:string external; if(exists(self::node()/cache[@id=$hashId])) then replace value of node self::node()/cache[@id=$hashId] with $newCacheValue else insert node element cache{ attribute id { $hashId }, text { $newCacheValue } } into self::node()";
		QueryProcessor queryProc = new QueryProcessor(setValueXQuery, context);
		try {
			// serialize updates to avoid lock-file races
			synchronized(writeLock) {
				// set doc as the context so that it knows which node to update
				queryProc.context(doc);
				// setting the new value through the variable is safer and allows us to use
				// the same query
				queryProc.bind("hashId", hash, "xs:integer");
				queryProc.bind("newCacheValue", content, "xs:string");
				// not expecting anything to be in the results
				queryProc.iter();
			}
		} catch(QueryException e) {
		 e.printStackTrace();
		} finally {
			queryProc.close();
		}
	}
	public void addVarMetaData() {
        /*
		try {
			XmlQueryContext qc = manager.createQueryContext(XmlQueryContext.LiveValues, XmlQueryContext.Eager);
			final XmlResults res = manager.query(
					"collection('"+contName+"')/*[fn:empty(dbxml:metadata('var'))]", qc);
			// instead of having to determine the size of prog bar, should be figured out
			// by the number of query builders..
			final JProgressBar progBar = new JProgressBar(0, res.size()*7);
			final JDialog jd = createProgressBarGUI(progBar, "Getting Variables", "Finding new variables");
			(new Thread(new Runnable() {
				public void run() {
					Runnable incProgress = (new Runnable() {
						public void run() {
							progBar.setValue(progBar.getValue() + 1);
						}
					});
					try {
					XmlResults tempRes;
					XmlResults getNodeRes;
					XmlValue tempVal;
					XmlValue delVal;
					java.util.List<String> getVarFromDoucmentNames = new ArrayList<String>();
					boolean gotVars = false;
					while(res.hasNext()) {
						gotVars = true;
						tempVal = res.next();
						getVarFromDoucmentNames.add(tempVal.getNodeHandle());
					}
					res.delete();
					for(Iterator<String> it = getVarFromDoucmentNames.iterator(); it.hasNext(); ) {
						getNodeRes = myContainer.getNode(it.next());
						tempVal = getNodeRes.next();
						System.out.println("Getting new MetaData");
						String path = "local:distinct-node-names(/scenario/world/*[@type='region']/demographics//*[fn:count(child::text()) = 1])";
						tempRes = getVars(tempVal, path);
						StringBuffer strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString());
							strBuff.append(';');
						}
						XmlDocument docTemp = tempVal.asDocument();
						docTemp.setMetaData("", "demographicsVar", new XmlValue(strBuff.toString()));
						tempRes.delete();
						SwingUtilities.invokeLater(incProgress);

						path = "local:distinct-node-names(/scenario/world/*[@type='region']/*[@type='sector']/*[@type='subsector']/*[@type='technology']/*[fn:count(child::text()) = 1])";
						tempRes = getVars(tempVal, path);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							//System.out.println("adding");
							delVal = tempRes.next();
							strBuff.append(delVal.asString());
							strBuff.append(';');
						}
						tempRes.delete();
						docTemp.setMetaData("", "var", new XmlValue(strBuff.toString()));
						SwingUtilities.invokeLater(incProgress);

						XmlQueryContext qcL = manager.createQueryContext(XmlQueryContext.LiveValues, XmlQueryContext.Lazy);
						XmlQueryExpression qe = manager.prepare("distinct-values(/scenario/world/*[@type='region']/*[@type='sector']/*[@type='subsector']/*[@type='technology']/*[@type='GHG']/@name)", qcL);
						tempRes = qe.execute(tempVal, qcL);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString()).append(';');
						}
						docTemp.setMetaData("", "ghgNames", new XmlValue(strBuff.toString()));
						tempRes.delete();
						SwingUtilities.invokeLater(incProgress);

						qe = manager.prepare("distinct-values(/scenario/world/*[@type='region']/*[@type='sector']/*[@type='subsector']/*[@type='technology']/*[@type='GHG']/emissions/@fuel-name)", qcL);
						tempRes = qe.execute(tempVal, qcL);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString()).append(';');
						}
						docTemp.setMetaData("", "fuelNames", new XmlValue(strBuff.toString()));
						tempRes.delete();
						SwingUtilities.invokeLater(incProgress);

						path = "local:distinct-node-names(/scenario/world/climate-model/*[fn:count(child::text()) = 1])";
						tempRes = getVars(tempVal, path);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString());
							strBuff.append(';');
						}
						docTemp.setMetaData("", "ClimateVar", new XmlValue(strBuff.toString()));
						tempRes.delete();
						SwingUtilities.invokeLater(incProgress);

						path = "local:distinct-node-names(//LandLeaf/*[fn:count(child::text()) = 1])";
						tempRes = getVars(tempVal, path);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString());
							strBuff.append(';');
						}
						docTemp.setMetaData("", "LandAllocationVar", new XmlValue(strBuff.toString()));
						tempRes.delete();
						SwingUtilities.invokeLater(incProgress);

						path = "local:distinct-node-names(/scenario/world/*[@type='region']/GDP/*[fn:count(child::text()) = 1])";
						tempRes = getVars(tempVal, path);
						strBuff = new StringBuffer();
						while(tempRes.hasNext()) {
							delVal = tempRes.next();
							strBuff.append(delVal.asString());
							strBuff.append(';');
						}
						docTemp.setMetaData("", "GDPVar", new XmlValue(strBuff.toString()));
						final XmlUpdateContext uc = manager.createUpdateContext();
						myContainer.updateDocument(docTemp, uc);
						tempRes.delete();
						getNodeRes.delete();
						SwingUtilities.invokeLater(incProgress);
					}
					res.delete();
					getVarMetaData();
					if(jd != null) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run(){
								jd.setVisible(false);
							}
						});
					}
					String message = gotVars ? "Finished getting new variables." : "There were no new variables.";
					InterfaceMain.getInstance().showMessageDialog(message, "Get Variables", 
							JOptionPane.INFORMATION_MESSAGE);
					} catch(XmlException e) {
					 e.printStackTrace();
					}
				}
			})).start();
		} catch(XmlException e) {
		 e.printStackTrace();
			//closeDB();
		}
        */
        // TODO: still necessary?
	}

    /*
	protected XmlResults getVars(XmlValue contextVal, String path) {
		try {
			XmlQueryContext qc = manager.createQueryContext(XmlQueryContext.LiveValues, XmlQueryContext.Lazy);

			/* The xQuery more readable:
			 * declare function local:distinct-node-names ($args as node()*) as xs:string* { 
			 *	fn:distinct-values(for $nname in $args return fn:local-name($nname))
			 * }; 
			 * local:distinct-node-names(/scenario/world/region/supplysector/subsector/technology/*[fn:count(child::text()) = 1])
			 * / 
			String queryStr = "declare function local:distinct-node-names ($args as node()*) as xs:string* { fn:distinct-values(for $nname in $args return fn:local-name($nname)) }; "+path;
			XmlQueryExpression qe = manager.prepare(queryStr, qc);
			return qe.execute(contextVal, qc);
		} catch(XmlException e) {
		 e.printStackTrace();
			return null;
		}
	}
    */
    /*
	protected void getVarMetaData() {
		XmlResults res = createQuery("/*[fn:exists(dbxml:metadata('var'))]", null, null, null);
		// TODO: is is safe to assume if one is null they are all null?
		if(SupplyDemandQueryBuilder.varList == null) {
			SupplyDemandQueryBuilder.varList = new LinkedHashMap<String, Boolean>();
			DemographicsQueryBuilder.varList = new LinkedHashMap<String, Boolean>();
			EmissionsQueryBuilder.ghgList = new LinkedHashMap<String, Boolean>();
			EmissionsQueryBuilder.fuelList = new LinkedHashMap<String, Boolean>();
			GDPQueryBuilder.varList = new LinkedHashMap<String, Boolean>();
			ClimateQueryBuilder.varList = new LinkedHashMap<String, Boolean>();
			LandAllocatorQueryBuilder.varList = new LinkedHashMap<String, Boolean>();
		}
		XmlMetaData md;
		try {
			while(res.hasNext()) {
				XmlValue vt = res.next();
				XmlDocument docTemp = vt.asDocument();
				System.out.println("Gathering metadata from a doc "+docTemp.getName());
				XmlMetaDataIterator it = docTemp.getMetaDataIterator();
				while((md = it.next()) != null) {
					XmlValue mdVal = md.get_value();
					if(mdVal.isNull()) {
						System.out.println("Got null metadata value for "+md.get_name());
					} else if(md.get_name().equals("var")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							SupplyDemandQueryBuilder.varList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("demographicsVar")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							DemographicsQueryBuilder.varList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("ghgNames")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							EmissionsQueryBuilder.ghgList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("fuelNames")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							EmissionsQueryBuilder.fuelList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("GDPVar")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							GDPQueryBuilder.varList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("ClimateVar")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							ClimateQueryBuilder.varList.put(vars[i], new Boolean(false));
						}
					} else if(md.get_name().equals("LandAllocationVar")) {
						String[] vars = mdVal.asString().split(";");
						for(int i = 0; i < vars.length; ++i) {
							LandAllocatorQueryBuilder.varList.put(vars[i], new Boolean(false));
						}
					}
				}
			}
		} catch(XmlException e) {
		 e.printStackTrace();
		}
		res.delete();
		// maybe this should be somewhere else..
		// maybe summable list should be cached..
		Vector<String> funcTemp = new Vector<String>(1,0);
		funcTemp.add("distinct-values");
		res = createQuery("/scenario/output-meta-data/summable/@var", funcTemp, null, null);
		QueryGenerator.sumableList = new Vector<String>();
		QueryGenerator.hasYearList = new Vector<String>();
		XmlValue delValue;
		try {
			while(res.hasNext()) {
				delValue = res.next();
				QueryGenerator.sumableList.add(delValue.asString());
			}
			res.delete();
			res = createQuery("/scenario/output-meta-data/has-year/@var", funcTemp, null, null);
			while(res.hasNext()) {
				delValue = res.next();
				QueryGenerator.hasYearList.add(delValue.asString());
			}
			res.delete();
		} catch(XmlException e) {
		 e.printStackTrace();
		}
	}
    */
	// TODO: this is a util method and should be moved somewhere else
	/**
	 * This method creates a window in a with a progress bar and label.
	 * If put in a thread both the bar and lable (passed in) can be updated
	 * using their respective functions.
	 * 
	 * @param progBar The progressbar to update
	 * @param title The title of the window
	 * @param label The Jlabel that will be added to window.
	 * @return
	 */
	public static JDialog createProgressBarGUI(JProgressBar progBar, String title, JLabel label) {
        final JFrame parentFrame = InterfaceMain.getInstance().getFrame();
        if(progBar.getMaximum() == 0) {
            return null;
        }
        // non-modal dialog so background work can continue
        JDialog filterDialog = new JDialog(parentFrame, title, false);
        filterDialog.setAlwaysOnTop(true);
        filterDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        JPanel all = new JPanel();
        all.setLayout( new BoxLayout(all, BoxLayout.Y_AXIS));
        progBar.setPreferredSize(new Dimension(200, 20));
        //JLabel label = new JLabel(labelStr);
        Container contentPane = filterDialog.getContentPane();
        all.add(label, BorderLayout.PAGE_START);
        all.add(Box.createVerticalStrut(10));
        all.add(progBar);
        all.add(Box.createVerticalStrut(10));

        // Add a dismiss button so the user can hide the dialog while the task continues
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Hide the dialog but do not dispose it so background work can continue
                filterDialog.setVisible(false);
            }
        });
        buttonPanel.add(dismissButton);
        all.add(buttonPanel);
        dismissButton.setVisible(false); // Hide for now, may re-enable later

        all.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPane.add(all, BorderLayout.PAGE_START);
        filterDialog.pack();

        // Center over the ModelInterface main frame
        filterDialog.setLocationRelativeTo(parentFrame);

        // Ensure the dialog gets focus/visibility when shown
        filterDialog.setVisible(true);
        filterDialog.toFront();
        filterDialog.requestFocusInWindow();
        return filterDialog;
    }
	
	public static JDialog createProgressBarGUIAddScenario(String title, JLabel overallLabel, JProgressBar overallProgBar,
				JLabel StepOneLabel, JProgressBar StepOneProgBar) {
		// delegate to new overload without an owner
		return createProgressBarGUIAddScenario(title, overallLabel, overallProgBar, StepOneLabel, StepOneProgBar, null);
	}

	public static JDialog createProgressBarGUIAddScenario(String title, JLabel overallLabel, JProgressBar overallProgBar,
			JLabel StepOneLabel, JProgressBar StepOneProgBar, JDialog owner) {
			final JFrame parentFrame = InterfaceMain.getInstance().getFrame();

			// If an owner dialog is provided, use it as the owner. Otherwise center on main frame.
			JDialog filterDialog = (owner != null) ? new JDialog(owner, title, true) : new JDialog(parentFrame, title, true);
			filterDialog.setAlwaysOnTop(true);
			filterDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			JPanel all = new JPanel();
			all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
			Container contentPane = filterDialog.getContentPane();

			// Only show the overall progress bar when importing multiple scenarios
			if (overallProgBar != null && overallProgBar.getMaximum() > 1) {
				overallProgBar.setPreferredSize(new Dimension(200, 20));
				all.add(overallLabel, BorderLayout.PAGE_START);
				all.add(Box.createVerticalStrut(10));
				all.add(overallProgBar);
				all.add(Box.createVerticalStrut(20));
			}

			// Always show the step/progress for the single-file import operations
			StepOneProgBar.setPreferredSize(new Dimension(200, 20));
			all.add(StepOneLabel);
			all.add(Box.createVerticalStrut(10));
			all.add(StepOneProgBar);
			all.add(Box.createVerticalStrut(20));

			// Add a dismiss button so the user can hide the dialog while the import continues
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JButton dismissButton = new JButton("Dismiss");
			dismissButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Hide the dialog but do not dispose so background work can update it if needed
					filterDialog.setVisible(false);
				}
			});
			buttonPanel.add(dismissButton);
			all.add(buttonPanel);
        	dismissButton.setVisible(false); // Hide for now, may re-enable later
			
			all.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
			contentPane.add(all, BorderLayout.PAGE_START);
			filterDialog.pack();

			// Center relative to owner if provided, otherwise center over main frame
			if (owner != null) {
				filterDialog.setLocationRelativeTo(owner);
			} else {
				filterDialog.setLocationRelativeTo(parentFrame);
			}

			return filterDialog;
		}
 }