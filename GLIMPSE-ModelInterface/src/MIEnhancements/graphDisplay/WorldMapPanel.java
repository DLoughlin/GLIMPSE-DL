package graphDisplay;

import javax.swing.JTable;

/**
 * Compatibility wrapper for world-oriented map modes.
 */
public class WorldMapPanel extends AbstractMapPanel {

    public WorldMapPanel(String chartName, JTable jtable, boolean statesIncluded) {
        super(chartName, jtable, MapMode.fromWorldStatesIncluded(statesIncluded));
        initialize();
    }
}