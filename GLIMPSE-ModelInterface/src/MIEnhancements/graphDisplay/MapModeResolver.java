package graphDisplay;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTable;

import filter.FilteredTable;

/**
 * Resolves which map mode can represent the current table contents.
 */
public final class MapModeResolver {
    private static final String STRING_STATES = "AK,AL,AR,AZ,CA,CO,CT,DC,DE,FL,GA,HI,IA,ID,IL,IN,KS,KY,LA,MA,MD,ME,MI,MN,MO,MS,MT,NC,ND,NE,NH,NJ,NM,NV,NY,OH,OK,OR,PA,RI,SC,SD,TN,TX,UT,VA,VT,WA,WI,WV,WY";
    private static final String[] COUNTRY_REGIONS = {"Africa_Eastern","Africa_Northern","Africa_Southern","Africa_Western","Australia_NZ","Brazil","Canada","Central America and Caribbean","Central Asia","China","EU_12","EU_15","Europe_Eastern","Europe_Non_EU","European Free Trade Association","India","Indonesia","Japan","Mexico","Middle East","Pakistan","Russia","South Africa","South America_Northern","South America_Southern","South Asia","South Korea","Southeast Asia","Taiwan","Argentina","Colombia"};

    private MapModeResolver() {}

    public static MapMode resolve(JTable table) {
        boolean containsStates = containsAnyState(table);
        boolean containsCountries = containsAnyCountryRegion(table);
        if (containsStates && containsCountries) {
            return MapMode.WORLD_WITH_STATES;
        }
        if (containsStates) {
            return MapMode.STATE;
        }
        if (containsCountries) {
            return MapMode.WORLD;
        }
        return null;
    }

    public static boolean requiresRowSelection(JTable table) {
        return table != null && table.getSelectionModel().isSelectionEmpty() && containsOtherColumns(table);
    }

    public static boolean containsOtherColumns(JTable table) {
        if (table == null) {
            return false;
        }
        int regionIdx = FilteredTable.getColumnByName(table, "region");
        if (regionIdx < 0) {
            return false;
        }
        java.util.ArrayList<String> yearList = FilteredTable.getYearListFromTableData(table);
        if (yearList.isEmpty()) {
            return false;
        }
        int firstYearIdx = FilteredTable.getColumnByName(table, yearList.get(0));
        return firstYearIdx >= 0 && (firstYearIdx - regionIdx) != 1;
    }

    public static boolean containsAnyState(JTable table) {
        return stringContainsItemFromArray(STRING_STATES, getUniqueRegions(table));
    }

    public static boolean containsAnyCountryRegion(JTable table) {
        return arrayContainsItemFromArray(COUNTRY_REGIONS, getUniqueRegions(table));
    }

    private static String[] getUniqueRegions(JTable table) {
        if (table == null) {
            return new String[0];
        }
        int regionColIdx = FilteredTable.getColumnByName(table, "region");
        if (regionColIdx < 0) {
            return new String[0];
        }
        Set<String> regionSet = new HashSet<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            Object value = table.getValueAt(row, regionColIdx);
            if (value != null) {
                regionSet.add(value.toString());
            }
        }
        return regionSet.toArray(new String[0]);
    }

    private static boolean stringContainsItemFromArray(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }

    private static boolean arrayContainsItemFromArray(String[] arrayStr, String[] items) {
        Set<String> itemsAsSet = new HashSet<>(Arrays.asList(items));
        for (String str : arrayStr) {
            if (itemsAsSet.contains(str)) {
                return true;
            }
        }
        return false;
    }
}
