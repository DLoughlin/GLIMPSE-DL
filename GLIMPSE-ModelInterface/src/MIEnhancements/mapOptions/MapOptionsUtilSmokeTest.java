package mapOptions;

import javax.swing.JTable;

/**
 * Tiny smoke test runner for MapOptionsUtil that can be run from the IDE.
 * This avoids bringing up the full UI just to validate null/empty/odd table shapes.
 */
public final class MapOptionsUtilSmokeTest {

    public static void main(String[] args) {
        testNoYearColumns();
        testNullCellsInYearColumn();
        testMissingScenarioColumn();
        System.out.println("MapOptionsUtilSmokeTest: OK");
    }

    private static void testNoYearColumns() {
        Object[][] data = new Object[][] { { "USA", "S1", 1.0 } };
        Object[] cols = new Object[] { "region", "scenario", "value" };
        JTable t = new JTable(data, cols);

        assert MapOptionsUtil.getYearListFromTableData(t).isEmpty();
        assert MapOptionsUtil.getAbsMinMaxFromTable(t, false)[1] == 1.0;
        assert MapOptionsUtil.getScenarioListFromTableData(t).size() == 1;
    }

    private static void testNullCellsInYearColumn() {
        Object[][] data = new Object[][] { { "USA", "S1", null }, { "CAN", "S1", 2.5 } };
        Object[] cols = new Object[] { "region", "scenario", "2020" };
        JTable t = new JTable(data, cols);

        double[] mm = MapOptionsUtil.getAbsMinMaxFromTableColumn(t, "2020", false);
        assert mm[0] <= 2.0 && mm[1] >= 3.0;
    }

    private static void testMissingScenarioColumn() {
        Object[][] data = new Object[][] { { "USA", 1.0 } };
        Object[] cols = new Object[] { "region", "2020" };
        JTable t = new JTable(data, cols);

        assert MapOptionsUtil.getScenarioListFromTableData(t).isEmpty();
    }
}
