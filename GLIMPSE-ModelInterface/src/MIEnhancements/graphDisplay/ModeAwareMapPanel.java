package graphDisplay;

import javax.swing.JTable;

/**
 * Shared concrete map panel that renders according to the selected {@link MapMode}.
 */
final class ModeAwareMapPanel extends AbstractMapPanel {

    static AbstractMapPanel open(String chartName, JTable jtable, MapMode mapMode) {
        return new ModeAwareMapPanel(chartName, jtable, mapMode);
    }

    private ModeAwareMapPanel(String chartName, JTable jtable, MapMode mapMode) {
        super(chartName, jtable, mapMode == null ? MapMode.STATE : mapMode);
        initialize();
    }
}