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
package glimpseBuilder;

import java.util.ArrayList;
import java.util.Arrays;

import glimpseUtil.GLIMPSEFiles;
import glimpseUtil.GLIMPSEStyles;
import glimpseUtil.GLIMPSEUtils;
import glimpseUtil.GLIMPSEVariables;

/**
 * CsvFileWriter is responsible for generating CSV file content based on provided column and data lists.
 * It uses GLIMPSE utility classes to process and format the data for output.
 *
 * @author US EPA, GLIMPSE contributors
 */
public class CsvFileWriter {
    /** Singleton instances */
    protected GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
    protected GLIMPSEStyles styles = GLIMPSEStyles.getInstance();
    protected GLIMPSEFiles files = GLIMPSEFiles.getInstance();
    protected GLIMPSEUtils utils = GLIMPSEUtils.getInstance();
    private static CsvFileWriter instance = null;

    ArrayList<String> csvColumnList = null;
    ArrayList<String> dataList = null;
    ArrayList<String> csvList = null;

    /**
     * Main method for testing CsvFileWriter functionality.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        CsvFileWriter writer = new CsvFileWriter();
        //writer.test();
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private CsvFileWriter() {
    }

    /**
     * Returns the singleton instance of CsvFileWriter.
     * @return CsvFileWriter instance
     */
    public static CsvFileWriter getInstance() {
        if (instance == null) {
            instance = new CsvFileWriter();
        }
        return instance;
    }

    /**
     * Creates the content for a CSV file based on the provided column and data lists.
     * Handles nested headers, region/year/value mapping, and formatting for GLIMPSE scenario data.
     *
     * @param colList List of column definitions
     * @param dataList List of data values and keys
     * @return ArrayList<String> representing lines of the CSV file
     */
    public ArrayList<String> createCsvContent(ArrayList<String> colList, ArrayList<String> dataList) {
        ArrayList<String> fileContentList = new ArrayList<String>();

        boolean isNested = false; // Flag for nested header structure

        // Extract key scenario values from dataList using utility methods
        String sectorText = utils.getMatch(dataList, "sector", ";", ":");
        String subsector1Text = utils.getMatch(dataList, "subsector", ";", ":");
        String subsector2Text = "";
        String technologyText = utils.getMatch(dataList, "technology", ";", ":");
        String inputText = utils.getStringUpToChar(utils.getMatch(dataList, "input", ";", ":"), ")");
        String outputText = utils.getStringUpToChar(utils.getMatch(dataList, "output", ";", ":"), ")");
        String paramText = utils.getMatch(dataList, "param", ";", ":");
        String param2Text = utils.getMatch(dataList, "param2", ";", ":");
        String[] yearsText = utils.getMatches(dataList, "year", ";", ":", ",");
        String[] valuesText = utils.getMatches(dataList, "value", ";", ":", ",");
        String[] regionsText = utils.getMatches(dataList, "region", ";", ":", ",");
        String dollarYearText = utils.getMatch(dataList, "dollarYear", ",");

        // Convert values to 1990 dollars if required
        if ((paramText.equals("Capital Cost"))||(paramText.equals("Levelized Non-Energy Cost"))) {
            valuesText = utils.convertTo1990Dollars(valuesText, dollarYearText);
        }
        
        // Compose header key for column lookup
        String comboText = sectorText + "/" + paramText;

        // Special handling for certain sector types
        if ((sectorText.equals("base load generation") || sectorText.equals("peaking generation")
                || sectorText.equals("intermediate generation"))) {
            comboText = "egu/" + paramText;
        } else if (sectorText.startsWith("trn")) {
			comboText = "trn/" + paramText;
		} else {
			comboText = "all/" + paramText;
		}

        int no_years = yearsText.length;
        int no_regions = regionsText.length;

        // Find header text from column list
        String headerText = utils.getMatch(colList, comboText, ";");
        System.out.println("Echo header text: " + headerText);

        // Handle nested header structure if present
        if (headerText.contains("=>")) {
            isNested = true;
            headerText = headerText.replace("=>", ",");

            // Split technology text if nested
            if (technologyText.contains("=>")) {
                String[] str = technologyText.split("=>");
                subsector2Text = str[0].trim();
                technologyText = str[1].trim();
            }
        }

        // Split header into name and columns
        String headerName = (headerText.split(":")[0]).trim();
        String header = (headerText.split(":")[1]).trim();

        // Add initial lines to CSV content
        fileContentList.add("INPUT_TABLE");
        fileContentList.add("Variable ID");
        fileContentList.add(headerName);
        fileContentList.add("");
        fileContentList.add(header);

        // Prepare lists for iteration
        ArrayList<String> headerAndColNames = utils.createArrayListFromString(header, ",");
        ArrayList<String> yearsList = new ArrayList<>(Arrays.asList(yearsText));
        ArrayList<String> valuesList = new ArrayList<>(Arrays.asList(valuesText));
        ArrayList<String> regionsList = new ArrayList<>(Arrays.asList(regionsText));

        // Iterate over regions and years to build CSV rows
        for (String region : regionsList) {
            for (String year : yearsList) {
                String line = "";
                for (String colName : headerAndColNames) {
                    // Map column names to scenario values
                    if (colName.equals("region"))
                        line += (line.isEmpty() ? "" : ",") + region;
                    else if (colName.equals("year"))
                        line += (line.isEmpty() ? "" : ",") + year;
                    else if (colName.equals("from-to")) {
                        int yearInt = 0;
                        int year2Int = 0;
                        try {
                            yearInt = Integer.parseInt(year) - 5;
                            year2Int = yearInt + 5;
                        } catch (Exception e2) {
                            System.out.println("Error translating year in CSV file. Attempting to continue.");
                        }
                        line += (line.isEmpty() ? "" : ",") + yearInt + "," + year2Int;
                    }
                    else if (colName.equals("data")) {
                        int valueIndex = yearsList.indexOf(year);
                        if (valueIndex >= 0 && valueIndex < valuesList.size()) {
                            line += (line.isEmpty() ? "" : ",") + valuesList.get(valueIndex);
                        } else {
                            line += (line.isEmpty() ? "" : ",") + "N/A"; // or some default value
                        }
                    }
                    else if (colName.indexOf("/") > 0)
                        ;//line += (line.isEmpty() ? "" : ",") + colName.substring(colName.indexOf("/") + 1);
                    else if (colName.equals("sector"))
                        line += (line.isEmpty() ? "" : ",") + sectorText;
                    else if ((colName.equals("subsector")) || (colName.equals("subsector1")))
                        line += (line.isEmpty() ? "" : ",") + subsector1Text;
                    else if (colName.equals("subsector2"))
                        line += (line.isEmpty() ? "" : ",") + subsector2Text;
                    else if (colName.equals("technology"))
                        line += (line.isEmpty() ? "" : ",") + technologyText;
                    else if (colName.equals("fuel"))
                        line += (line.isEmpty() ? "" : ",") + inputText;
                    else if (colName.equals("market"))
                        line += (line.isEmpty() ? "" : ",") + region;
                    else if (colName.equals("species"))
                        line += (line.isEmpty() ? "" : ",") + param2Text;
                }
                fileContentList.add(line);
            }
        }

        return fileContentList;
    }

}