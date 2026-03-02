import glimpseUtil.CSVToXMLMain;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Tiny smoke test that runs CSVToXMLMain using a colon-separated preset region list.
 * All input files are written to a temporary directory so this test is
 * environment-independent and can be run by any developer without configuration.
 */
public class SmokeCsvToXmlPresetExpansion {
    public static void main(String[] args) {
        Path tmpDir = null;
        try {
            // Initialize core singletons similarly to the app.
            GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
            GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
            GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
            GLIMPSEFiles files = GLIMPSEFiles.getInstance();
            vars.init(utils, vars, styles, files);

            tmpDir = Files.createTempDirectory("glimpse_smoke_");

            // Write a minimal preset region list (group name: comma-separated sub-regions).
            File preset = tmpDir.resolve("smoke_preset_region_list.txt").toFile();
            Files.write(preset.toPath(),
                    "Northeast: CT, MA, ME, NH, RI, VT\n".getBytes(StandardCharsets.UTF_8));

            // Write a minimal header file (tableId,xmlHeaderPath).
            File header = tmpDir.resolve("smoke_header.txt").toFile();
            Files.write(header.toPath(),
                    "smokeTableId,region,year,value\n".getBytes(StandardCharsets.UTF_8));

            // Write a minimal CSV file using the Northeast preset region token.
            File csv = tmpDir.resolve("smoke.csv").toFile();
            String csvContent =
                    "INPUT_TABLE\n" +
                    "Variable ID\n" +
                    "smokeTableId\n" +
                    "\n" +
                    "region,year,value\n" +
                    "Northeast,2025,42\n";
            Files.write(csv.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));

            File out = tmpDir.resolve("smoke.xml").toFile();

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
        } finally {
            // Clean up the temporary directory and all files created within it.
            if (tmpDir != null) {
                try {
                    Files.walk(tmpDir)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } catch (IOException ignore) {
                    // Best-effort cleanup; temp files will be removed by the OS eventually.
                }
            }
        }
    }
}
