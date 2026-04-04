package graphDisplay;

import javax.swing.JTable;

/**
 * Compatibility wrapper for the state map mode.
 */
public class StateMapPanel extends AbstractMapPanel {

    public StateMapPanel(String chartName, JTable jtable) {
        super(chartName, jtable, MapMode.STATE);
        initialize();
    }
}