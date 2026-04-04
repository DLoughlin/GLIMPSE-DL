package graphDisplay;

import javax.swing.JTable;

import org.geotools.map.MapContent;
import org.geotools.swing.JMapPane;
import org.geotools.swing.tool.PanTool;

import ModelInterface.InterfaceMain;

/**
 * Encapsulates the boundary source and map-pane behavior for each supported map mode.
 */
public enum MapMode {
    STATE("State map") {
        @Override
        public String getShapeFilePath() {
            return InterfaceMain.stateShapeFileLocation;
        }

        @Override
        public String getMissingBoundaryMessage(String shpFilePath) {
            return "StateMapPanel: No state boundary features available. Did you set the Map Resource Folder? "
                    + "(stateShapeFileLocation=" + shpFilePath + ")";
        }
    },
    WORLD("World map") {
        @Override
        public String getShapeFilePath() {
            return InterfaceMain.gcamReg32ShapeFileLocation;
        }

        @Override
        public void configureMapPane(JMapPane mapPane) {
            PanTool myPanTool = new PanTool();
            myPanTool.onMouseDragged(null);
            mapPane.setCursorTool(myPanTool);
        }

        @Override
        public String getMissingBoundaryMessage(String shpFilePath) {
            return "WorldMapPanel: No boundary features available. Did you set the Map Resource Folder? "
                    + "(shapeFilePath=" + shpFilePath + ")";
        }
    },
    WORLD_WITH_STATES("World map") {
        @Override
        public String getShapeFilePath() {
            return InterfaceMain.gcamReg32US52ShapeFileLocation;
        }

        @Override
        public void configureMapPane(JMapPane mapPane) {
            WORLD.configureMapPane(mapPane);
        }

        @Override
        public String getMissingBoundaryMessage(String shpFilePath) {
            return WORLD.getMissingBoundaryMessage(shpFilePath);
        }
    };

    private final String mapLabel;

    MapMode(String mapLabel) {
        this.mapLabel = mapLabel;
    }

    public String getMapLabel() {
        return mapLabel;
    }

    public abstract String getShapeFilePath();

    public void configureMapPane(JMapPane mapPane) {
        // default no-op
    }

    public void logRegionProperty(String propertyVal) {
        // default no-op
    }

    public abstract String getMissingBoundaryMessage(String shpFilePath);

    public MapContent createBoundaryMap(AbstractMapPanel panel) {
        return panel.buildBoundaryMap(getMapLabel(), getShapeFilePath());
    }

    public static MapMode fromWorldStatesIncluded(boolean statesIncluded) {
        return statesIncluded ? WORLD_WITH_STATES : WORLD;
    }

    public static AbstractMapPanel createPanel(String chartName, JTable jtable, MapMode mode) {
        return ModeAwareMapPanel.open(chartName, jtable, mode);
    }
}