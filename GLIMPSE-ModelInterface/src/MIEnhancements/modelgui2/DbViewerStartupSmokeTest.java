package modelgui2;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import ModelInterface.ModelGUI2.DbViewer;

/**
 * Tiny smoke test for DbViewer startup lifecycle helpers.
 *
 * This runner is intentionally headless-safe and avoids opening the full UI.
 * It focuses on the regression-prone pieces we hardened:
 * - query file fallback resolution
 * - explicit queriesDoc invalidation
 * - repeat lifecycle transitions that mimic open/cancel/open style flows
 */
public final class DbViewerStartupSmokeTest {

    public static void main(String[] args) throws Exception {
        DbViewer viewer = new DbViewer();
        testQueryFallbackResolution(viewer);
        testQueriesDocInvalidation(viewer);
        testLifecycleTransitions(viewer);
        testDbViewInitializedReset(viewer);
        testBackgroundStartupLoaderFailsCleanly(viewer);
        testDbFileWithoutParentDoesNotCrash(viewer);
        testButtonPanelRequiresQueryPanel(viewer);
        testMissingIconFallsBack(viewer);
        testMalformedFavoritePathReturnsNull();
        testMalformedAxisLevelFailsCleanly();
        testSingleQueryExtensionIgnoresUnexpectedTreeModel();
        System.out.println("DbViewerStartupSmokeTest: OK");
    }

    private static void testQueryFallbackResolution(DbViewer viewer) throws Exception {
        Method resolveStartupQueryFile = DbViewer.class.getDeclaredMethod("resolveStartupQueryFile", Properties.class,
                javax.swing.JFrame.class);
        resolveStartupQueryFile.setAccessible(true);

        Properties props = new Properties();
        props.setProperty("lastDirectory", new File(".").getAbsolutePath());
        File resolved = (File) resolveStartupQueryFile.invoke(viewer, props, null);

        assert resolved != null : "Expected a startup query file to be resolved";
        assert resolved.exists() : "Resolved query file must exist";
        assert props.getProperty("queryFile") != null && !props.getProperty("queryFile").trim().isEmpty();
    }

    private static void testQueriesDocInvalidation(DbViewer viewer) throws Exception {
        Method invalidateQueriesDocument = DbViewer.class.getDeclaredMethod("invalidateQueriesDocument", String.class);
        invalidateQueriesDocument.setAccessible(true);

        Field queriesDocField = DbViewer.class.getDeclaredField("queriesDoc");
        queriesDocField.setAccessible(true);
        Field queriesField = DbViewer.class.getDeclaredField("queries");
        queriesField.setAccessible(true);

        queriesDocField.set(viewer, org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance()
                .getDOMImplementation("XML 3.0").createDocument(null, "queries", null));
        queriesField.set(viewer, new Object());

        invalidateQueriesDocument.invoke(viewer, "smoke-test");

        assert queriesDocField.get(viewer) == null : "queriesDoc should be cleared";
        assert queriesField.get(viewer) == null : "queries model should be cleared";
    }

    private static void testLifecycleTransitions(DbViewer viewer) throws Exception {
        Class<?> stateClass = Class.forName("ModelInterface.ModelGUI2.DbViewer$StartupLifecycleState");
        Method setStartupState = DbViewer.class.getDeclaredMethod("setStartupState", stateClass);
        setStartupState.setAccessible(true);
        Field startupStateField = DbViewer.class.getDeclaredField("startupState");
        startupStateField.setAccessible(true);

        Object preparing = Enum.valueOf((Class<Enum>) stateClass.asSubclass(Enum.class), "PREPARING");
        Object failed = Enum.valueOf((Class<Enum>) stateClass.asSubclass(Enum.class), "FAILED");
        Object ready = Enum.valueOf((Class<Enum>) stateClass.asSubclass(Enum.class), "READY");
        Object shuttingDown = Enum.valueOf((Class<Enum>) stateClass.asSubclass(Enum.class), "SHUTTING_DOWN");

        setStartupState.invoke(viewer, preparing);
        assert startupStateField.get(viewer) == preparing;

        setStartupState.invoke(viewer, failed);
        assert startupStateField.get(viewer) == failed;

        setStartupState.invoke(viewer, preparing);
        setStartupState.invoke(viewer, ready);
        assert startupStateField.get(viewer) == ready;

        setStartupState.invoke(viewer, shuttingDown);
        assert startupStateField.get(viewer) == shuttingDown;
    }

    private static void testDbFileWithoutParentDoesNotCrash(DbViewer viewer) throws Exception {
        File dbFile = new File("startup-smoke-db");
        assert dbFile.getParent() == null : "Expected smoke db file to have no parent path";

        Field startupStateField = DbViewer.class.getDeclaredField("startupState");
        startupStateField.setAccessible(true);
        Class<?> stateClass = Class.forName("ModelInterface.ModelGUI2.DbViewer$StartupLifecycleState");
        Object failed = Enum.valueOf((Class<Enum>) stateClass.asSubclass(Enum.class), "FAILED");

        try {
            viewer.doOpenDB(dbFile, false);
        } catch (NullPointerException npe) {
            throw new AssertionError("doOpenDB should not throw NullPointerException when dbFile.getParent() is null", npe);
        } catch (Exception expectedInHeadlessOrNoDbEnv) {
            // Accept other failures here; this smoke test is specifically guarding against the null-parent crash.
        }

        Object state = startupStateField.get(viewer);
        assert state != null;
        assert state == failed || state.toString().equals("OPENING_DB") || state.toString().equals("LOADING_DATA");
    }

    private static void testButtonPanelRequiresQueryPanel(DbViewer viewer) throws Exception {
        Method setupButtonPanel = DbViewer.class.getDeclaredMethod("setupButtonPanel");
        setupButtonPanel.setAccessible(true);

        Field queriesSplitField = DbViewer.class.getDeclaredField("queriesSplit");
        queriesSplitField.setAccessible(true);
        queriesSplitField.set(viewer, new javax.swing.JSplitPane());

        try {
            setupButtonPanel.invoke(viewer);
            throw new AssertionError("Expected setupButtonPanel to reject a missing query panel");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assert cause instanceof IllegalStateException : "Expected IllegalStateException but got " + cause;
        }
    }

    private static void testMissingIconFallsBack(DbViewer viewer) throws Exception {
        Method loadQueryTreeIcon = DbViewer.class.getDeclaredMethod("loadQueryTreeIcon", String.class, String.class);
        loadQueryTreeIcon.setAccessible(true);
        Object icon = loadQueryTreeIcon.invoke(viewer, "icons/does-not-exist.png", "smoke-test");
        assert icon != null : "Expected a fallback icon when the resource is missing";
    }

    private static void testMalformedFavoritePathReturnsNull() throws Exception {
        javax.swing.JTree tree = new javax.swing.JTree();
        ModelInterface.ModelGUI2.FavoriteQueriesManager manager = new ModelInterface.ModelGUI2.FavoriteQueriesManager(tree,
                null);
        Method getTreePathForEachLine = ModelInterface.ModelGUI2.FavoriteQueriesManager.class
                .getDeclaredMethod("getTreePathForEachLine", javax.swing.JTree.class, String.class);
        getTreePathForEachLine.setAccessible(true);

        Object result = getTreePathForEachLine.invoke(manager, tree, "malformed-line-without-delimiter");
        assert result == null : "Malformed favorite query paths should be ignored safely";
    }

    private static void testMalformedAxisLevelFailsCleanly() throws Exception {
        org.w3c.dom.Document doc = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance()
                .getDOMImplementation("XML 3.0").createDocument(null, "queries", null);
        org.w3c.dom.Element query = doc.createElement("supplyDemandQuery");
        query.setAttribute("title", "Malformed Axis Test");

        org.w3c.dom.Element axis1 = doc.createElement("axis1");
        axis1.setAttribute("name", "Y");
        axis1.setTextContent("sector[@]");
        query.appendChild(axis1);

        org.w3c.dom.Element axis2 = doc.createElement("axis2");
        axis2.setAttribute("name", "X");
        axis2.setTextContent("year");
        query.appendChild(axis2);

        org.w3c.dom.Element xPath = doc.createElement("xPath");
        xPath.setAttribute("dataName", "value");
        xPath.setTextContent("supplysector/output/node()");
        query.appendChild(xPath);

        try {
            new ModelInterface.ModelGUI2.queries.QueryGenerator(query);
            throw new AssertionError("Expected malformed axis syntax to fail cleanly");
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage() != null && expected.getMessage().contains("Malformed axis level");
        }
    }

    private static void testSingleQueryExtensionIgnoresUnexpectedTreeModel() throws Exception {
        org.w3c.dom.Document doc = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance()
                .getDOMImplementation("XML 3.0").createDocument(null, "queries", null);
        org.w3c.dom.Element query = doc.createElement("supplyDemandQuery");
        query.setAttribute("title", "SingleQuery Smoke");

        org.w3c.dom.Element axis1 = doc.createElement("axis1");
        axis1.setAttribute("name", "Y");
        axis1.setTextContent("sector");
        query.appendChild(axis1);

        org.w3c.dom.Element axis2 = doc.createElement("axis2");
        axis2.setAttribute("name", "X");
        axis2.setTextContent("year");
        query.appendChild(axis2);

        org.w3c.dom.Element xPath = doc.createElement("xPath");
        xPath.setAttribute("dataName", "value");
        xPath.setTextContent("supplysector/output/node()");
        query.appendChild(xPath);

        ModelInterface.ModelGUI2.queries.QueryGenerator qg = new ModelInterface.ModelGUI2.queries.QueryGenerator(query);
        ModelInterface.ModelGUI2.queries.SingleQueryExtension ext = qg.getSingleQueryExtension();
        javax.swing.JTree wrongTree = new javax.swing.JTree();
        javax.swing.event.TreeSelectionEvent event = new javax.swing.event.TreeSelectionEvent(wrongTree,
                new javax.swing.tree.TreePath[] { new javax.swing.tree.TreePath(wrongTree.getModel().getRoot()) },
                new boolean[] { true }, null, new javax.swing.tree.TreePath(wrongTree.getModel().getRoot()));

        ext.valueChanged(event);
    }

    private static void testBackgroundStartupLoaderFailsCleanly(DbViewer viewer) throws Exception {
        Method loadStartupDataInBackground = DbViewer.class.getDeclaredMethod("loadStartupDataInBackground", File.class,
                boolean.class);
        loadStartupDataInBackground.setAccessible(true);

        File invalidDb = new File("build_tmp", "missing-db-for-startup-smoke");
        if (invalidDb.exists()) {
            throw new AssertionError("Expected smoke-test DB path to be absent: " + invalidDb.getAbsolutePath());
        }

        try {
            loadStartupDataInBackground.invoke(viewer, invalidDb, false);
            throw new AssertionError("Expected background startup loader to fail for a missing database path");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assert cause instanceof IllegalStateException : "Expected IllegalStateException but got " + cause;
            assert cause.getMessage() != null && cause.getMessage().contains("Could not open or initialize")
                    : "Unexpected background loader failure message: " + cause.getMessage();
        }
    }

    private static void testDbViewInitializedReset(DbViewer viewer) throws Exception {
        Field dbViewInitializedField = DbViewer.class.getDeclaredField("dbViewInitialized");
        dbViewInitializedField.setAccessible(true);
        Method resetDbViewInitialized = DbViewer.class.getDeclaredMethod("resetDbViewInitialized", String.class);
        resetDbViewInitialized.setAccessible(true);

        dbViewInitializedField.setBoolean(viewer, true);
        resetDbViewInitialized.invoke(viewer, "smoke-test");
        assert !dbViewInitializedField.getBoolean(viewer) : "dbViewInitialized should reset to false";

        dbViewInitializedField.setBoolean(viewer, true);
        try {
            viewer.doOpenDB(new File("startup-smoke-db"), false);
        } catch (Exception expectedInHeadlessOrNoDbEnv) {
            // The smoke environment may fail before startup completes; we only care that the stale init flag is reset.
        }
        assert !dbViewInitializedField.getBoolean(viewer)
                : "Fresh doOpenDB should clear stale dbViewInitialized before startup begins";
    }
}