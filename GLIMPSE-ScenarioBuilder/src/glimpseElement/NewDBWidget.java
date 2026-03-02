/*
* LEGAL NOTICE
* This computer software was prepared by US EPA.
* THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* EXPORT CONTROL
* User agrees that the Software will not be shipped, transferred or
* exported into any country or used in any manner prohibited by the
* United States Export Administration Act or any other applicable
* export laws, restrictions or regulations (collectively the "Export Laws").
* Export of the Software may require some form of license or other
* authority from the U.S. Government, and failure to obtain such
* export control license may result in criminal liability under
* U.S. laws. In addition, if the Software is identified as export controlled
* items under the Export Laws, User represents and warrants that User
* is not a citizen, or otherwise located within, an embargoed nation
* (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
*
* SUPPORT
* GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. 
* The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
* Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
* Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
* Laboratory contract.
* 
*/
package glimpseElement;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import gui.Client;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NewDBWidget {
	protected GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
	protected GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
	protected GLIMPSEFiles files = GLIMPSEFiles.getInstance();
	protected GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

	TextField newDBNameTextField = utils.createTextField();

	public void newDBWidget() {
		System.out.println("Created new DB Widget");
	}

	public void createAndShow() {
		String title = "Create new GCAM output database";

		Stage stage = new Stage();
		stage.setTitle(title);
		// stage.setWidth(200);
		// stage.setHeight(100);
		Scene scene = new Scene(new Group());
		gui.ScenarioBuilder.applyModernTheme(scene);
		// stage.setResizable(false);

		Label topLabel = utils
				.createLabel("If specified database does not already exist, it will be created in the output folder.");

		Label newDBNameLabel = utils.createLabel("Name:");

		GridPane grid = new GridPane();

		grid.setHgap(10);
		grid.setVgap(10);
		// Use centralized horizontal padding for grids
		grid.setPadding(styles.getHorizontalPadding10());

		grid.add(topLabel, 0, 0);
		grid.add(newDBNameLabel, 0, 1);
		grid.add(newDBNameTextField, 0, 2);

		grid.add(new Separator(Orientation.HORIZONTAL), 0, 3, 3, 1);

		Button createButton = utils.createButton("Create", styles.getBigButtonWidth(), "Creates new DB or opens existing",null);
		Button cancelButton = utils.createButton("Cancel", styles.getBigButtonWidth(), null);

		cancelButton.setOnAction(e -> {
			stage.close();
		});

		createButton.setOnAction(e -> {
			creator();
			stage.close();
		});

		VBox root = new VBox();
		// Use centralized tiny padding for compact roots
		root.setPadding(styles.getTinyPadding());
		root.setSpacing(5);
		root.setAlignment(Pos.TOP_LEFT);

		HBox buttonBox = new HBox();
		// Use centralized button box padding
		buttonBox.setPadding(styles.getButtonBoxPadding());

         buttonBox.setSpacing(5);
         buttonBox.setAlignment(Pos.CENTER);
         buttonBox.getChildren().addAll(createButton, cancelButton);

         root.getChildren().addAll(grid, buttonBox);
         scene.setRoot(root);

         stage.setScene(scene);
         stage.show();

	}

	private void creator() { 
		try {
			String newDbName = newDBNameTextField.getText();
			newDbName = validateDbNameOrNull(newDbName);
			if (newDbName == null) {
				return;
			}

			File miDir = new File(vars.getModelInterfaceJarDir());
			File miJar = new File(miDir, vars.getModelInterfaceJar());

			File outputDir = new File(vars.getgCamExecutableDir(), ".." + File.separator + "output");
			File outputDb = new File(outputDir, newDbName);
			// Canonicalize and verify we didn't escape the intended output directory.
			try {
				File canonicalOutputDir = outputDir.getCanonicalFile();
				File canonicalOutputDb = outputDb.getCanonicalFile();
				String basePath = canonicalOutputDir.getPath();
				String dbPath = canonicalOutputDb.getPath();
				if (!dbPath.equals(basePath) && !dbPath.startsWith(basePath + File.separator)) {
					utils.warningMessage("Invalid database name (must be a simple name under the output folder).");
					return;
				}
			} catch (IOException ioe) {
				utils.warningMessage("Problem validating output path.");
				System.out.println("Error validating output path: " + ioe);
				return;
			}

			String[] cmd = new String[] {
					"java",
					"-jar",
					miJar.getAbsolutePath(),
					"-o",
					outputDb.getAbsolutePath()
			};

			System.out.println("Starting ModelInterface...");
			System.out.println("   dir: " + miDir.getAbsolutePath());
			System.out.println("   cmd: " + java.util.Arrays.toString(cmd));
			try {
				Client.modelInterfaceExecutionThread.submitCommandWithDirectory(cmd, miDir.getAbsolutePath());
			} catch (Exception e) {
				utils.warningMessage("Problem starting up post-processor.");
				System.out.println("Error in trying to start up post-processor:");
				System.out.println(e);
			}

		} catch (Exception e) {
			System.out.println("Error creating new database " + e);
			System.out.println("  " + e);
		}
	}

	/**
	 * Validates a user-supplied DB name to ensure it is a simple directory name (no separators)
	 * and is safe on Windows (no reserved device names / invalid characters).
	 *
	 * @param rawName User input
	 * @return Trimmed valid name, or null if invalid (and shows a warning)
	 */
	private String validateDbNameOrNull(String rawName) {
		if (rawName == null) {
			utils.warningMessage("Please enter a database name.");
			return null;
		}
		String name = rawName.trim();
		if (name.isEmpty()) {
			utils.warningMessage("Please enter a database name.");
			return null;
		}

		// Disallow any path separators or traversal attempts.
		if (name.contains("/") || name.contains("\\") || name.contains(":")) {
			utils.warningMessage("Database name can't contain path separators.");
			return null;
		}
		if (name.equals(".") || name.equals("..") || name.contains("..")) {
			utils.warningMessage("Database name can't contain '..'.");
			return null;
		}

		// Windows filename constraints (safe to enforce cross-platform;
		// reduces portability surprises when projects are shared).
		// Invalid: < > : " / \\ | ? * and control chars.
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c < 32 || c == '<' || c == '>' || c == '"' || c == '|' || c == '?' || c == '*') {
				utils.warningMessage("Database name contains invalid characters.");
				return null;
			}
		}
		if (name.endsWith(".") || name.endsWith(" ")) {
			utils.warningMessage("Database name can't end with a space or period.");
			return null;
		}

		// Reserved device names on Windows, with or without extensions.
		String base = name;
		int dot = base.indexOf('.');
		if (dot >= 0) {
			base = base.substring(0, dot);
		}
		base = base.toUpperCase(Locale.ROOT);
		if (base.equals("CON") || base.equals("PRN") || base.equals("AUX") || base.equals("NUL")
				|| base.matches("COM[1-9]") || base.matches("LPT[1-9]")) {
			utils.warningMessage("Database name is a reserved Windows device name.");
			return null;
		}

		return name;
	}

}
