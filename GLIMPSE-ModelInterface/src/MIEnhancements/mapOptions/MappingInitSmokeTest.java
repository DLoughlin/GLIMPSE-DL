package mapOptions;

import java.io.File;

import ModelInterface.InterfaceMain;

/**
 * Smoke test for mapping initialization.
 *
 * Run this from the IDE (or command line) to verify that providing a map resource
 * folder will populate the three key shapefile paths.
 */
public final class MappingInitSmokeTest {

    public static void main(String[] args) {
        // Create a relative default consistent with InterfaceMain's fallback.
        File rel = new File("map_resources");
        if (!rel.exists()) {
            // When launched from repo root, map_resources is under GLIMPSE-ModelInterface.
            rel = new File("GLIMPSE-ModelInterface" + File.separator + "map_resources");
        }

        String folder = rel.getAbsolutePath();
        boolean ok = InterfaceMain.initializeMappingFromFolder(folder);

        System.out.println("initializeMappingFromFolder(" + folder + ") => " + ok);
        System.out.println(" enableMapping=" + InterfaceMain.enableMapping);
        System.out.println(" shapeFileLocationPrefix=" + InterfaceMain.shapeFileLocationPrefix);
        System.out.println(" stateShapeFileLocation=" + InterfaceMain.stateShapeFileLocation);
        System.out.println(" gcamReg32ShapeFileLocation=" + InterfaceMain.gcamReg32ShapeFileLocation);
        System.out.println(" gcamReg32US52ShapeFileLocation=" + InterfaceMain.gcamReg32US52ShapeFileLocation);

        // Assertions (run with -ea to enforce)
        assert InterfaceMain.shapeFileLocationPrefix != null && !InterfaceMain.shapeFileLocationPrefix.trim().isEmpty();
        assert InterfaceMain.stateShapeFileLocation != null && new File(InterfaceMain.stateShapeFileLocation).exists();
        assert InterfaceMain.gcamReg32ShapeFileLocation != null && new File(InterfaceMain.gcamReg32ShapeFileLocation).exists();

        System.out.println("MappingInitSmokeTest: OK");
    }
}