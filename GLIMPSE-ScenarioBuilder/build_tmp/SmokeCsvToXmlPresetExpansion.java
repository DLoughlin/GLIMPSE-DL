import glimpseUtil.CSVToXMLMain;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

import java.io.File;

/**
 * Tiny smoke test that runs CSVToXMLMain using a colon-separated preset region list.
 * Writes output to build_tmp/smoke.xml.
 */
public class SmokeCsvToXmlPresetExpansion {
    public static void main(String[] args) {
        try {
            // Initialize core singletons similarly to the app.
            GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
            GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
            GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
            GLIMPSEFiles files = GLIMPSEFiles.getInstance();
            vars.init(utils, vars, styles, files);

            File baseDir = new File("C:/Users/danlo/git/GLIMPSE-CE/GLIMPSE-ScenarioBuilder/build_tmp");
            File preset = new File(baseDir, "smoke_preset_region_list.txt");
            File header = new File(baseDir, "smoke_header.txt");
            File csv = new File(baseDir, "smoke.csv");
            File out = new File(baseDir, "smoke.xml");

            vars.setPresetRegionListFilename(preset.getAbsolutePath());

            String[] toolArgs = {
                    csv.getAbsolutePath(),
                    header.getAbsolutePath(),
                    out.getAbsolutePath(),
                    "--usePresetRegionList=true"
            };

            CSVToXMLMain.main(toolArgs);

            System.out.println("Smoke test complete. Output: " + out.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Smoke test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
