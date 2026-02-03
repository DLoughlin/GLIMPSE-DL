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

import glimpseUtil.FileChooserPlus;
import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;
import javafx.geometry.Insets;
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

public class CsvToXmlWidget {
	protected GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
	protected GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
	protected GLIMPSEFiles files = GLIMPSEFiles.getInstance();
	protected GLIMPSEUtils utils = GLIMPSEUtils.getInstance();

	TextField csvFileTextField=utils.createTextField();
	TextField headerFileTextField=utils.createTextField();
	String csvFilename="";
	String headersFilename=vars.getXmlHeaderFilename();
	String xmlFilename="";
	
	public void csvToXmlWidget() {
		System.out.println("Created CSV to XML Widget");
	}
	
	public void createAndShow() {
		String title="CSV to XML Converter";

		Stage stage = new Stage();
		stage.setTitle(title);
		//stage.setWidth(200);
		//stage.setHeight(100);
		Scene scene = new Scene(new Group());
		//stage.setResizable(false);
		
		Label topLabel=utils.createLabel("Input files:");
		
		Label csvFileLabel=utils.createLabel("CSV file");
		//TextField csvFileTextField=utils.createTextField();
		Button browseForCsvFile=utils.createButton("Browse", styles.getBigButtonWidth(),"Locate CSV file",null);
		Label headerFileLabel=utils.createLabel("Header file");
		//TextField headerFileTextField=utils.createTextField();
		Button browseForHeaderFile=utils.createButton("Browse", styles.getBigButtonWidth(),"Locate header file",null);

		headerFileTextField.setText(headersFilename);

		browseForCsvFile.setOnAction(e -> {
			csvFilename=browseForFile("CSV file (*.csv)","*.csv",vars.getGlimpseDir(),"Open");	
			csvFileTextField.setText(csvFilename);

			System.out.println("Updated "+csvFilename);
			System.out.println(csvFileTextField.getText());			
		});
		
		browseForHeaderFile.setOnAction(e -> {
			File headerFile=new File(headersFilename);
			String temp=browseForFile("TXT file (*.txt)","*.txt",headerFile.getParent(),"Open");	
			headersFilename=temp;
			headerFileTextField.setText(temp);
		});	
				
		
		GridPane grid = new GridPane();

		
		grid.setHgap(10);
		grid.setVgap(10);
		// Use centralized horizontal padding for grids
		grid.setPadding(styles.getHorizontalPadding10());
		
		grid.add(topLabel, 0, 0);
		grid.add(csvFileLabel, 0, 1);
		grid.add(headerFileLabel, 0, 2);
		//grid.add(xmlFileLabel, 0, 3);
		
		grid.add(csvFileTextField, 1, 1);
		grid.add(headerFileTextField, 1, 2);
		//grid.add(xmlFileTextField, 1, 3);
		
		grid.add(browseForCsvFile, 2, 1);
		grid.add(browseForHeaderFile, 2, 2);
		//grid.add(browseForXmlFile, 2, 3);		
				
		grid.add(new Separator(Orientation.HORIZONTAL), 0, 3, 3,1);
		
		Button convertButton = utils.createButton("Convert", styles.getBigButtonWidth(),"Converts CSV to XML using header",null);
		Button closeButton = utils.createButton("Close", styles.getBigButtonWidth(),null);

		closeButton.setOnAction(e -> {
			stage.close();
			});
		
		convertButton.setOnAction(e -> {
			converter();
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
         buttonBox.getChildren().addAll(convertButton,closeButton);


		
 
 	root.getChildren().addAll(grid,buttonBox);
		scene.setRoot(root);

		stage.setScene(scene);
	
		
		stage.show();
		
		
    }

//    /**
//     * Shows an "Open File" dialog.
//     *
//     * @param ownerWindow      The parent window for the dialog.
//     * @param title            The title for the dialog window.
//     * @param initialDirectory The directory to open initially.
//     * @param filter           The extension filter to apply.
//     * @return An Optional containing the selected file, or an empty Optional if canceled.
//     */
//    public static File showOpenDialog(Window ownerWindow, String title, File initialDirectory, FileChooser.ExtensionFilter filter) {
//        FileChooser chooser = createAndConfigureChooser(title, initialDirectory, null, filter);
//        File result = chooser.showOpenDialog(ownerWindow);
//        return result;
//    }
	
	private String browseForFile(String filter1,String filter2,String path,String which) {
		String tmp="";
			try {
				File file = FileChooserPlus.showOpenDialog(null, which,new File(path),FileChooserPlus.createExtensionFilter(filter1,filter2));
				tmp=file.getPath();
			} catch(Exception ex) {
				System.out.println("Error in "+which+" "+filter2+" file:");
				System.out.println("  "+ex);
			}
			return tmp;
	}

	
	private void converter() {
		try {
			csvFilename=csvFileTextField.getText();
			headersFilename=headerFileTextField.getText();
			
			File csvFile=new File(csvFilename);
			xmlFilename=browseForFile("XML file (*.xml)","*.xml",csvFile.getParent(),"Save");
			String[] s = { csvFilename, headersFilename, xmlFilename };
			System.out.println("csv to xml conversion commencing:");
			System.out.println("    csv file: " + csvFilename);
			System.out.println("    header file: " + headersFilename);
			System.out.println("    xml file: " + xmlFilename);
			glimpseUtil.CSVToXMLMain.main(s);

			files.showFileInTextEditor(xmlFilename);
		} catch (Exception e) {
			System.out.println("Error saving or opening xml file "+xmlFilename);
			System.out.println("  "+e);
		}
	}
	
}