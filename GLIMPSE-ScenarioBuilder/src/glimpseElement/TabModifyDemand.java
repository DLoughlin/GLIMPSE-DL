package glimpseElement;

import javafx.stage.Stage;

/**
 * Preferred class name for the Modify Demand tab.
 * <p>
 * This class currently delegates to {@link TabFixedDemand} to preserve
 * backward compatibility while callers migrate to the new name.
 * </p>
 */
public class TabModifyDemand extends TabFixedDemand {

    public TabModifyDemand(String title, Stage stageX) {
        super(title, stageX);
    }
}
