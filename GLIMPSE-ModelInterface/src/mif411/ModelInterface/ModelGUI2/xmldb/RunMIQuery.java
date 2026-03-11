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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.basex.api.dom.BXNode;
import org.basex.query.QueryException;
import org.basex.query.QueryModule;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.Value;
import org.basex.query.value.ValueBuilder;
import org.basex.query.value.item.Str;
import org.basex.query.value.node.ANode;
import org.basex.query.value.node.FElem;
import org.basex.query.value.seq.Empty;
import org.basex.query.value.seq.StrSeq;

import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.ScenarioListItem;
import ModelInterface.ModelGUI2.queries.QueryGenerator;

/**
 * A BaseX Java bound QueryModule to run a Model Interface query.
 * @author Pralit Patel.
 */ 
public class RunMIQuery extends QueryModule {
    @Requires(Permission.READ)
    @Deterministic
    @ContextDependent
    public Value runMIQuery(ANode aMIQury, Value aScnNames, Value aRegionNames) throws QueryException {
        // We need to put any logging on STDERR so we can assume anything on STDOUT is results
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        // If a user wants to suppress all output we must redirect STDERR as well
        if(Boolean.parseBoolean(System.getProperty("ModelInterface.SUPPRESS_OUTPUT", "false"))) {
            System.setErr(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    // ignore all
                }
            }));
        }
        System.setOut(System.err);
        XMLDB xmldb = null;
        try {
            String[] scenarioNames = null;
            String[] regions = null;
            if(aScnNames instanceof Empty) {
                scenarioNames = new String[0];
            } else if(aScnNames instanceof Str) {
                scenarioNames = new String[] { ((Str)aScnNames).toJava() };
            } else if(aScnNames instanceof StrSeq) {
                scenarioNames = ((StrSeq)aScnNames).toJava();
            } else {
                throw new Exception("Scenario argument of unexpected type: "+aScnNames.toString());
            }
            if(aRegionNames instanceof Empty) {
                regions = new String[0];
            } else if(aRegionNames instanceof Str) {
                regions = new String[] { ((Str)aRegionNames).toJava() };
            } else if(aRegionNames instanceof StrSeq) {
                regions = ((StrSeq)aRegionNames).toJava();
            } else {
                throw new Exception("Regions argument of unexpected type: "+aRegionNames.toString());
            }
            QueryGenerator qg = new QueryGenerator(aMIQury.toJava());
            xmldb = new XMLDB(queryContext.context);
            Vector<ScenarioListItem> scenariosInDb = DbViewer.getScenarios(xmldb);
            Vector<ScenarioListItem> scenariosToRun = new Vector<ScenarioListItem>();
            if(scenarioNames.length == 0 && !scenariosInDb.isEmpty()) {
                scenariosToRun.add(scenariosInDb.lastElement());
            } else {
                for(String currScn : scenarioNames) {
                    String[] scnSplit = currScn.split(" (?=[^ ]+$)");
                    String scnName = scnSplit.length > 0 ? scnSplit[0] : null;
                    String scnDate = scnSplit.length > 1 ? scnSplit[1] : null;
                    ScenarioListItem found = ScenarioListItem.findClosestScenario(scenariosInDb, scnName, scnDate);
                    if(found != null) {
                        scenariosToRun.add(found);
                    }
                }
            }
            if(scenariosToRun.isEmpty()) {
                throw new Exception("Could not find scenarios to run.");
            }
            QueryProcessor queryProc = xmldb.createQuery(qg, scenariosToRun.toArray(), regions);
            ValueBuilder vb = new ValueBuilder(queryContext); //Dan: added argument, was blank
            FElem elem = new FElem("csv");
            vb.add(elem);
            buildTable(queryProc, qg, elem);
            return vb.value();
            //return res.value();
        } catch(Exception e) {
            e.printStackTrace();
            throw new QueryException(e);
        } finally {
            // note no need to close xmldb since we adopted the context
        	// from the already running BaseX instance
        	
            // reset output streams
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }
    private void buildTable(QueryProcessor queryProc, QueryGenerator qg, FElem outputDoc) throws Exception {
        boolean isGlobal = false;

        Iter res = queryProc.iter();
        ANode tempNode;
        final LinkedList<QueryRow>parentPath = new LinkedList<QueryRow>();
        while((tempNode = (ANode)res.next()) != null) {
            BXNode domNode = BXNode.get(tempNode);
            qg.defaultAddToDataTree(tempNode.parent(), parentPath.listIterator(0), isGlobal);

            if(!parentPath.peekLast().key.equals("Units")) {
                QueryRow unitsRow = new QueryRow(null);
                parentPath.offerLast(unitsRow);
                unitsRow.key = "Units";
            }
            String currUnits = XMLDB.getAttrMap(BXNode.get(tempNode.parent())).get("unit");
            parentPath.peekLast().value = currUnits == null ? "None Specified" : currUnits;

            FElem row = new FElem("record");
            boolean skip = false;
            for(Iterator<QueryRow> it = parentPath.descendingIterator(); it.hasNext(); ) {
                QueryRow currRow = it.next();
                if(!(currRow.key == null && currRow.value == null )) {
                    FElem col = new FElem(currRow.key);
                    col.add(currRow.value);
                    row.add(col);
                    // skip this data since a rewrite list set the value
                    // to an empty string indicating the user wanted to
                    // delete it
                    skip = skip || currRow.value.isEmpty();
                }
            }
            if(!skip) {
                FElem valueCol = new FElem("value");
                valueCol.add(domNode.getNodeValue());
                row.add(valueCol);
                outputDoc.add(row);
            }
        }
        System.out.println("After Function: "+System.currentTimeMillis());
    }
}