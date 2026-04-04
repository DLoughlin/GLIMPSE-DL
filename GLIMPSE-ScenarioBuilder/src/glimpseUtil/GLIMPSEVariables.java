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
package glimpseUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Holds global variables and configuration for GLIMPSE.
 * Provides accessors and mutators for all configuration options.
 *
 * <p>This class implements the Singleton pattern to ensure a single instance
 * of global variables and configuration settings is used throughout the application.</p>
 *
 * @author US EPA
 */
public class GLIMPSEVariables {

    // --- Singleton Pattern ---
    protected static final GLIMPSEVariables INSTANCE = new GLIMPSEVariables();

    private GLIMPSEUtils utils;
    private GLIMPSEFiles files;
    private GLIMPSEStyles styles;

    // --- Constants ---
    private final String DEFAULT_GLIMPSE_VERSION = "GLIMPSE-CE v2.0";
    public final int DEFAULT_SCENARIO_BUILDER_WIDTH = 1600;
    public final int DEFAULT_SCENARIO_BUILDER_HEIGHT = 900;
    private final float DEFAULT_MAX_DATABASE_SIZE_GB = 40f;
    private final List<Integer> DEFAULT_ALLOWABLE_POLICY_YEARS_LIST = new ArrayList<>(Arrays.asList(2025,2030,2035,2040,2045,2050,2055,2060,2065,2070,2075,2080,2085,2090,2095,2100));
    private final List<Integer> DEFAULT_ALL_YEARS_LIST = new ArrayList<>(Arrays.asList(1990,2005,2010,2015,2021,2025,2030,2035,2040,2045,2050,2055,2060,2065,2070,2075,2080,2085,2090,2095,2100));
    private final Integer DEFAULT_CALIBRATION_YEAR = new Integer(2021);
    private final String DEFAULT_USE_ICONS = "false";
    private final String DEFAULT_PREFERRED_FONT_SIZE = "12";
    private final String DEFAULT_DEBUG_REGION = "USA";
    private final boolean DEFAULT_SHOW_SPLASH = true;
    private final boolean DEFAULT_USE_ALL_AVAILABLE_PROCESSORS = true;
    private final int DEFAULT_PERIOD_INCREMENT = 5; // Default period increment for GCAM
    private final List<String> DEFAULT_REGION_LIST = new ArrayList<>(Arrays.asList("USA", "Canada", "EU-15", "Europe_Non_EU", "European Free Trade Association", "Japan", "Australia_NZ", "Central Asia", "Russia", "China", "Middle East", "Africa_Eastern", "Africa_Northern", "Africa_Southern", "Africa_Western", "South Africa", "Brazil", "Central America and Caribbean", "Mexico", "South America_Northern", "South America_Southern", "Argentina", "Colombia", "Indonesia", "Pakistan", "South Asia", "Southeast Asia", "Taiwan", "Europe_Eastern", "EU-12", "South Korea", "India", "Ukraine"));
    private final List<String> DEFAULT_SUBREGION_LIST = new ArrayList<>(Arrays.asList("AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"));
    private final List<String> DEFAULT_POLLUTANT_LIST = new ArrayList<>(Arrays.asList("CO2 (MTC)", "CO2 (MT CO2)","GHG (MT CO2E)", "NOx (Tg)", "SO2 (Tg)", "PM2.5 (Tg)", "CO (Tg)", "NMVOC (Tg)", "NH3 (Tg)", "CH4 (Tg)", "N2O (Tg)", "BC (Tg)", "OC (Tg)","F-gases (MT CO2E)"));
    
    private final int DEFAULT_SIMULATION_YEAR_INCREMENT = 5;

    // GHG Defaults
	private final List<String> DEFAULT_GHG_LIST = new ArrayList<>(Arrays.asList("CO2","CH4","CH4_AWB","CH4_AGR","N2O","N2O_AWB","N2O_AGR","C2F6","CF4","SF6","HFC23","HFC32","HFC125","HFC134a","HFC143a","HFC152a","HFC227ea","HFC43","HFC236fa","HFC365mfc","HFC245fa"));
	private final List<String> DEFAULT_GHG_PRICE_ADJUST = new ArrayList<>(Arrays.asList("1.0","6.818","6.818","6.818","81.265","81.265","81.265","3.327","2.015","6.218","4.036","0.184","0.954","0.39","1.219","0.034","0.873","0.447","2.675","0.217","0.281"));
	private final List<String> DEFAULT_GHG_DEMAND_ADJUST = new ArrayList<>(Arrays.asList("3.667","25","25","25","298","298","298","12.2","7.39","22.8","14.8","0.675","3.5","1.43","4.47","0.124","3.2","1.64","9.81","0.794","1.03"));
	private final List<String> DEFAULT_GHG_PRICE_UNIT = new ArrayList<>(Arrays.asList("1990$/tC","1990$/GgCH4","1990$/GgCH4","1990$/GgCH4","1990$/GgN2O","1990$/GgN2O","1990$/GgN2O","1990$/MgC2F6","1990$/MgCF4","1990$/MgSF6","1990$/MgHFC23","1990$/MgHFC32","1990$/MgHFC125","1990$/MgHFC134a","1990$/MgHFC143a","1990$/MgHFC152a","1990$/MgHFC227ea","1990$/MgHFC43","1990$/MgHFC236fa","1990$/MgHFC365mfc","1990$/MgHFC245fa"));
	private final List<String> DEFAULT_GHG_OUTPUT_UNIT = new ArrayList<>(Arrays.asList("MtC","TgCH4","TgCH4","TgCH4","TgN2O","TgN2O","TgN2O","GgC2F6","GgCF4","GgSF6","GgHFC23","GgHFC32","GgHFC125","GgHFC134a","GgHFC143a","GgHFC152a","GgHFC227ea","GgHFC43","GgHFC236fa","GgHFC365mfc","GgHFC245fa"));

    // F-gas Defaults
	private final List<String> DEFAULT_FGAS_LIST = new ArrayList<>(Arrays.asList("C2F6","CF4","SF6","HFC23","HFC32","HFC125","HFC134a","HFC143a","HFC152a","HFC227ea","HFC43","HFC236fa","HFC365mfc","HFC245fa"));
	
    // --- Fields ---
    private String glimpseVersion = DEFAULT_GLIMPSE_VERSION;
    private int scenarioBuilderWidth = DEFAULT_SCENARIO_BUILDER_WIDTH;
    private int scenarioBuilderHeight = DEFAULT_SCENARIO_BUILDER_HEIGHT;
    private float maxDatabaseSizeGB = DEFAULT_MAX_DATABASE_SIZE_GB;
    private List<Integer> allowablePolicyYears = DEFAULT_ALLOWABLE_POLICY_YEARS_LIST;
    private List<Integer> allYears = DEFAULT_ALL_YEARS_LIST;
    private List<String> pollutantList = DEFAULT_POLLUTANT_LIST;
    
    private List<String> ghgList = DEFAULT_GHG_LIST;
    private List<String> fGasList = DEFAULT_FGAS_LIST;
    private List<String> ghgPriceAdjust = DEFAULT_GHG_PRICE_ADJUST;
    private List<String> ghgDemandAdjust = DEFAULT_GHG_DEMAND_ADJUST;
    private List<String> ghgPriceUnit = DEFAULT_GHG_PRICE_UNIT;
    private List<String> ghgOutputUnit = DEFAULT_GHG_OUTPUT_UNIT;
    
    private Integer calibrationYear = DEFAULT_CALIBRATION_YEAR;
    private int periodIncrement = DEFAULT_PERIOD_INCREMENT;
    private String preferredFontSize = DEFAULT_PREFERRED_FONT_SIZE;
    private String useIcons = DEFAULT_USE_ICONS;
    private String debugRegion = DEFAULT_DEBUG_REGION;
    private int simulationYearIncrement = DEFAULT_SIMULATION_YEAR_INCREMENT;
    private boolean showSplash = DEFAULT_SHOW_SPLASH;
    private boolean useAllAvailableProcessors = DEFAULT_USE_ALL_AVAILABLE_PROCESSORS;
    private String executeCmdShort = "cmd /C ";
    private String executeCmd = "cmd /C start ";
    private String buildInfo = glimpseVersion;
    private String runQueueStr = "Queue is empty.";
    private String eol = "\n";
    private String debugCreate = "0";
    private String debugRename = "0";
    private String startYearForShare = "2010";
    private String[][] techInfo = null;
   
    private boolean isGcamUSA = false;
    private String glimpseDir = null;
    private String glimpseResourceDir = null;
    private String glimpseDocDir = null;
    private String gCamHomeDir = null;
    private String gCamSolver = null;
    private String scenarioBuilderDir = null;
    private String scenarioBuilderJar = null;
    private String scenarioBuilderJarDir = null;
    private String scenarioBuilderConfigDir = null;
    private String gCamExecutable = null;
    private String gCamExecutableArgs = " -C ";
    private String gCamExecutableDir = null;
    private String modelInterfaceJar = null;
    private String modelInterfaceJarDir = null;
    private String modelInterfaceDir = null;
    private String modelInterfaceConfigDir = null;
    private String filesToSave = null;
    private String scenarioComponentsDir = null;
    private String scenarioDir = null;
    private String glimpseLogDir = null;
    private String resourceDir = null;
    private String trashDir = null;
    private String gCamDataDir = null;
    private String gCamOutputDatabase = null;
    private String optionsFilename = null;
    private String xmlLibrary = null;
    private String textEditor = null;
    private String xmlEditor = null;
    private String descriptionText = "";
    private String stopPeriod = null;
    private String stopYear = null;
    private String configurationTemplateFilename = null;
    private String queryFilename = null;
    private String favoriteQueryFilename = null;
    private String tchBndListFilename = null;
    private String trnVehInfoFilename = null;
    private String regionListFilename = null;
    private String subRegionListFilename = null;
    private String presetRegionListFilename = null;
    private String csvColumnFilename = null;
    private String xmlHeaderFilename = null;
    private String unitConversionsFilename = null;
    private String monetaryConversionsFilename = null;
    private String aboutTextFilename = null;
    //TODO: override region lists if files are present
    private List<String> regionList = null;
    private List<String> subRegionList = null;

    /**
     * Private constructor for singleton pattern.
     */
    private GLIMPSEVariables() {}

    /**
     * Returns the singleton instance of GLIMPSEVariables.
     *
     * @return The singleton instance of GLIMPSEVariables.
     */
    public static GLIMPSEVariables getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the list of pollutants as an array of strings.
     *
     * @return An array of pollutant names.
     */
    public String[] getPollutantList() {
		String[] pollutants = new String[pollutantList.size()];
		for (int i = 0; i < pollutantList.size(); i++) {
			pollutants[i] = pollutantList.get(i);
		}
		return pollutants;
	}

    /**
     * Sets the list of pollutants from a comma-separated string.
     *
     * @param pollutantListStr A comma-separated list of pollutant names.
     */
    public void setPollutantList(String pollutantListStr) {
		this.pollutantList = new ArrayList<>();
		String[] pollutants = pollutantListStr.split(",");
		for (String pollutant : pollutants) {
			this.pollutantList.add(pollutant.trim());
		}
	}
    
    /**
     * Initializes utility, style, and file dependencies.
     *
     * @param u The GLIMPSEUtils instance.
     * @param v The GLIMPSEVariables instance.
     * @param s The GLIMPSEStyles instance.
     * @param f The GLIMPSEFiles instance.
     */
    public void init(GLIMPSEUtils u, GLIMPSEVariables v, GLIMPSEStyles s, GLIMPSEFiles f) {
        this.utils = u;
        this.styles = s;
        this.files = f;
        this.eol = System.lineSeparator();
    }

    /**
     * Returns the GLIMPSE version string.
     *
     * @return The GLIMPSE version string.
     */
    public String getGLIMPSEVersion() {
        return glimpseVersion;
    }

    /**
     * Returns the scenario builder configuration directory.
     *
     * @return the scenario builder configuration directory
     */
    public String getScenarioBuilderConfigDir() {
        return scenarioBuilderConfigDir;
    }

    /**
     * Sets the scenario builder configuration directory.
     *
     * @param s the scenario builder configuration directory
     */
    public void setScenarioBuilderConfigDir(String s) {
        this.scenarioBuilderConfigDir = s;
    }

    /**
     * Returns the model interface configuration directory.
     *
     * @return the model interface configuration directory
     */
    public String getModelInterfaceConfigDir() {
        return modelInterfaceConfigDir;
    }

    /**
     * Sets the model interface configuration directory.
     *
     * @param s the model interface configuration directory
     */
    public void setModelInterfaceConfigDir(String s) {
        this.modelInterfaceConfigDir = s;
    }

    /**
     * Returns the filename for the "About" text.
     *
     * @return The filename for the "About" text.
     */
    public String getAboutTextFilename() {
        return aboutTextFilename;
    }

    /**
     * Sets the filename for the "About" text.
     *
     * @param s The filename for the "About" text.
     */
    public void setAboutTextFilename(String s) {
        aboutTextFilename = s;
    }

    /**
     * Returns the filename for the preset region list.
     *
     * @return The filename for the preset region list.
     */
    public String getPresetRegionListFilename() {
        return presetRegionListFilename;
    }

    /**
     * Sets the filename for the preset region list.
     *
     * @param s The filename for the preset region list.
     */
    public void setPresetRegionListFilename(String s) {
        presetRegionListFilename = s;
    }

    /**
     * Returns the filename for the subregion list.
     *
     * @return The filename for the subregion list.
     */
    public String getSubRegionsFilename() {
        return subRegionListFilename;
    }

    /**
     * Sets the filename for the subregion list.
     *
     * @param s The filename for the subregion list.
     */
    public void setSubRegionsFilename(String s) {
        subRegionListFilename = s;
    }

    /**
     * Returns the filename for the region list.
     *
     * @return The filename for the region list.
     */
    public String getRegionListFilename() {
        return regionListFilename;
    }

    /**
     * Sets the filename for the region list.
     *
     * @param s The filename for the region list.
     */
    public void setRegionListFilename(String s) {
        regionListFilename = s;
    }

    /**
     * Returns the calibration year.
     *
     * @return The calibration year.
     */
    public Integer getCalibrationYear() {
        return calibrationYear;
    }

    /**
     * Sets the calibration year.
     *
     * @param s The calibration year as a string.
     */
    public void setCalibrationYear(String s) {
        calibrationYear = utils.convertStringToInt(s.trim());
    }

    /**
     * Returns the list of allowable policy years.
     *
     * @return A list of allowable policy years.
     */
    public List<Integer> getAllowablePolicyYears() {
        return allowablePolicyYears;
    }

    /**
     * Sets the list of allowable policy years.
     *
     * @param year_list A comma-separated list of years.
     */
    public void setAllowablePolicyYears(String year_list) {
        String[] yearArray = year_list.split(",");
        List<Integer> tempYearList = new ArrayList<>();
        for (String year : yearArray) {
            tempYearList.add(utils.convertStringToInt(year.trim()));
        }
        this.allowablePolicyYears = tempYearList;
    }

    /**
     * Returns whether to use all available processors.
     *
     * @return True if using all available processors, false otherwise.
     */
    public boolean getUseAllAvailableProcessors() {
        return useAllAvailableProcessors;
    }

    /**
     * Sets whether to use all available processors.
     *
     * @param b True to use all available processors, false otherwise.
     */
    public void setUseAllAvailableProcessors(boolean b) {
        useAllAvailableProcessors = b;
    }

    /**
     * Sets whether to use all available processors from a string.
     *
     * @param str "true" or "yes" to use all available processors, "false" otherwise.
     */
    public void setUseAllAvailableProcessors(String str) {
        boolean b = false;
        if ((str.toLowerCase().equals("true")) || (str.toLowerCase().equals("yes"))) b = true;
        useAllAvailableProcessors = b;
    }

    /**
     * Returns whether to show the splash screen.
     *
     * @return True to show the splash screen, false otherwise.
     */
    public boolean getShowSplash() {
        return showSplash;
    }

    /**
     * Sets whether to show the splash screen.
     *
     * @param b True to show the splash screen, false otherwise.
     */
    public void setShowSplash(boolean b) {
        showSplash = b;
    }

    /**
     * Sets whether to show the splash screen from a string.
     *
     * @param str "true" or "yes" to show the splash screen, "false" otherwise.
     */
    public void setShowSplash(String str) {
        boolean b = false;
        if ((str.toLowerCase().equals("true")) || (str.toLowerCase().equals("yes"))) b = true;
        showSplash = b;
    }

    /**
     * Returns the debug region.
     *
     * @return The debug region.
     */
    public String getDebugRegion() {
        return debugRegion;
    }

    /**
     * Sets the debug region.
     *
     * @param s The debug region.
     */
    public void setDebugRegion(String s) {
        this.debugRegion = s;
    }

    /**
     * Returns the start year for sharing.
     *
     * @return The start year for sharing.
     */
    public String getStartYearForShare() {
        return startYearForShare;
    }

    /**
     * Sets the start year for sharing.
     *
     * @param s The start year for sharing.
     */
    public void setStartYearForShare(String s) {
        startYearForShare = s;
    }

    /**
     * Returns the debug create flag.
     *
     * @return The debug create flag.
     */
    public String getDebugCreate() {
        return debugCreate;
    }

    /**
     * Sets the debug create flag.
     *
     * @param s The debug create flag.
     */
    public void setDebugCreate(String s) {
        if (s.toLowerCase().equals("true")) {
            s = "1";
        } else if (s.toLowerCase().equals("false")) {
            s = "0";
        }
        this.debugCreate = s;
    }

    /**
     * Returns the debug rename flag.
     *
     * @return The debug rename flag.
     */
    public String getDebugRename() {
        return debugRename;
    }

    /**
     * Sets the debug rename flag.
     *
     * @param s The debug rename flag.
     */
    public void setDebugRename(String s) {
        this.debugRename = s;
    }

    /**
     * Returns whether debug mode is enabled.
     * <p>
     * This is a convenience boolean used to gate noisy debug-only logging.
     * Historically, GLIMPSE used string debug flags; we interpret any non-zero
     * debug flag as enabling debug.
     */
    public boolean getDebug() {
        return !"0".equals(debugCreate) || !"0".equals(debugRename);
    }

    /**
     * Returns the build information string.
     *
     * @return The build information string.
     */
    public String getBuildInfo() {
        return buildInfo;
    }

    /**
     * Sets the build information string.
     *
     * @param s The build information string.
     */
    public void setBuildInfo(String s) {
        this.buildInfo = s;
    }

    /**
     * Returns whether GCAM USA mode is enabled.
     *
     * @return True if GCAM USA mode is enabled, false otherwise.
     */
    public boolean isGcamUSA() {
        return isGcamUSA;
    }

    /**
     * Sets whether GCAM USA mode is enabled.
     *
     * @param b True to enable GCAM USA mode, false otherwise.
     */
    public void setIsGcamUSA(boolean b) {
        this.isGcamUSA = b;
    }

    /**
     * Returns the short execute command.
     * @return Short execute command
     */
    public String getExecuteCmdShort() {
        return executeCmdShort;
    }

    /**
     * Sets the short execute command.
     * @param s Short execute command
     */
    public void setExecuteCmdShort(String s) {
        this.executeCmdShort = s;
    }

    /**
     * Returns the execute command.
     * @return Execute command
     */
    public String getExecuteCmd() {
        return executeCmd;
    }

    /**
     * Sets the execute command.
     * @param s Execute command
     */
    public void setExecuteCmd(String s) {
        this.executeCmd = s;
    }

    /**
     * Returns the GLIMPSE directory.
     * @return GLIMPSE directory
     */
    public String getGlimpseDir() {
        return glimpseDir;
    }

    /**
     * Sets the GLIMPSE directory.
     * @param s GLIMPSE directory
     */
    public void setGlimpseDir(String s) {
        this.glimpseDir = s;
    }

    /**
     * Returns the GLIMPSE resource directory.
     * @return GLIMPSE resource directory
     */
    public String getGlimpseResourceDir() {
        return glimpseResourceDir;
    }

    /**
     * Sets the GLIMPSE resource directory.
     * @param s GLIMPSE resource directory
     */
    public void setGlimpseResourceDir(String s) {
        this.glimpseResourceDir = s;
    }
    
    /**
     * Returns the GLIMPSE document directory.
     * @return GLIMPSE document directory
     */
    public String getGlimpseDocDir() {
        return glimpseDocDir;
    }

    /**
     * Sets the GLIMPSE document directory.
     * @param s GLIMPSE document directory
     */
    public void setGlimpseDocDir(String s) {
        this.glimpseDocDir = s;
    }
    
    /**
     * Returns the GCAM home directory.
     * @return GCAM home directory
     */
    public String getgCamHomeDir() {
        return gCamHomeDir;
    }

    /**
     * Sets the GCAM home directory.
     * @param s GCAM home directory
     */
    public void setgCamHomeDir(String s) {
        this.gCamHomeDir = s;
    }

    /**
     * Returns the GCAM solver.
     * @return GCAM solver
     */
    public String getgCamSolver() {
        return gCamSolver;
    }

    /**
     * Sets the GCAM solver.
     * @param s GCAM solver
     */
    public void setgCamSolver(String s) {
        this.gCamSolver = s;
    }

    /**
     * Returns the scenario builder directory.
     * @return Scenario builder directory
     */
    public String scenarioBuilderDir() {
        return scenarioBuilderDir;
    }

    /**
     * Sets the scenario builder directory.
     * @param s Scenario builder directory
     */
    public void setScenarioBuilderDir(String s) {
        this.scenarioBuilderDir = s;
    }

    /**
     * Returns the scenario builder JAR file.
     * @return Scenario builder JAR file
     */
    public String getScenarioBuilderJar() {
        return scenarioBuilderJar;
    }

    /**
     * Sets the scenario builder JAR file.
     * @param s Scenario builder JAR file
     */
    public void setScenarioBuilderJar(String s) {
        this.scenarioBuilderJar = s;
    }
    
    /**
     * Returns the GCAM executable.
     * @return GCAM executable
     */
    public String getgCamExecutable() {
        return gCamExecutable;
    }

    /**
     * Sets the GCAM executable.
     * @param s GCAM executable
     */
    public void setgCamExecutable(String s) {
        this.gCamExecutable = s;
    }

    /**
     * Returns the GCAM executable arguments.
     * @return GCAM executable arguments
     */
    public String getgCamExecutableArgs() {
        return gCamExecutableArgs;
    }

    /**
     * Sets the GCAM executable arguments.
     * @param s GCAM executable arguments
     */
    public void setgCamExecutableArgs(String s) {
        this.gCamExecutableArgs = s;
    }
    
    /**
     * Returns the GCAM executable directory.
     * @return GCAM executable directory
     */
    public String getgCamExecutableDir() {
        return gCamExecutableDir;
    }

    /**
     * Sets the GCAM executable directory.
     * @param s GCAM executable directory
     */
    public void setgCamExecutableDir(String s) {
        this.gCamExecutableDir = s;
    }

    /**
     * Returns the model interface JAR file.
     * @return Model interface JAR file
     */
    public String getModelInterfaceJar() {
        return modelInterfaceJar;
    }

    /**
     * Sets the model interface JAR file.
     * @param s Model interface JAR file
     */
    public void setModelInterfaceJar(String s) {
        this.modelInterfaceJar = s;
    }

    /**
     * Returns the model interface directory.
     * @return Model interface directory
     */
    public String getModelInterfaceDir() {
        return modelInterfaceDir;
    }

    /**
     * Sets the model interface directory.
     * @param s Model interface directory
     */
    public void setModelInterfaceDir(String s) {
        this.modelInterfaceDir = s;
    }

    /**
     * Returns the model interface JAR directory.
     * @return Model interface JAR directory
     */
    public String getModelInterfaceJarDir() {
        return modelInterfaceJarDir;
    }

    /**
     * Sets the model interface JAR directory.
     * @param s Model interface JAR directory
     */
    public void setModelInterfaceJarDir(String s) {
        this.modelInterfaceJarDir = s;
    }
    
    /**
     * Returns the files to save.
     * @return Files to save
     */
    public String getFilesToSave() {
        return filesToSave;
    }

    /**
     * Sets the files to save.
     * @param s Files to save
     */
    public void setFilesToSave(String s) {
        this.filesToSave = s;
    }

    /**
     * Returns the scenario components directory.
     * @return Scenario components directory
     */
    public String getScenarioComponentsDir() {
        return scenarioComponentsDir;
    }

    /**
     * Sets the scenario components directory.
     * @param s Scenario components directory
     */
    public void setScenarioComponentsDir(String s) {
        this.scenarioComponentsDir = s;
    }

    /**
     * Returns the scenario directory.
     * @return Scenario directory
     */
    public String getScenarioDir() {
        return scenarioDir;
    }

    /**
     * Sets the scenario directory.
     * @param s Scenario directory
     */
    public void setScenarioDir(String s) {
        this.scenarioDir = s;
    }

    /**
     * Returns the GLIMPSE log directory.
     * @return GLIMPSE log directory
     */
    public String getGlimpseLogDir() {
        return glimpseLogDir;
    }

    /**
     * Sets the GLIMPSE log directory.
     * @param s GLIMPSE log directory
     */
    public void setGlimpseLogDir(String s) {
        glimpseLogDir=s;
    }
    
    /**
     * Returns the query filename.
     * @return Query filename
     */
    public String getQueryFilename() {
        return queryFilename;
    }

    /**
     * Returns the favorite query filename.
     * @return Favorite query filename
     */
    public String getFavoriteQueryFilename() {
        return favoriteQueryFilename;
    }
    
    /**
     * Sets the query filename.
     * @param s Query filename
     */
    public void setQueryFilename(String s) {
        this.queryFilename=s;
    }
    
    /**
     * Sets the unit conversions filename.
     * @param s Unit conversions filename
     */
    public void setUnitConversionsFilename(String s) {
        this.unitConversionsFilename=s;
    }

    /**
     * Returns the unit conversions filename.
     * @return Unit conversions filename
     */
    public String getUnitConversionsFilename() {
        return unitConversionsFilename;
    }
    
    /**
     * Returns the resource directory.
     * @return Resource directory
     */
    public String getResourceDir() {
        return resourceDir;
    }

    /**
     * Sets the resource directory.
     * @param s Resource directory
     */
    public void setResourceDir(String s) {
        this.resourceDir = s;
    }

    /**
     * Returns the trash directory.
     * @return Trash directory
     */
    public String getTrashDir() {
        return trashDir;
    }

    /**
     * Sets the trash directory.
     * @param s Trash directory
     */
    public void setTrashDir(String s) {
        this.trashDir = s;
    }

    /**
     * Returns the configuration template filename.
     * @return Configuration template filename
     */
    public String getConfigurationTemplateFilename() {
        return configurationTemplateFilename;
    }

    /**
     * Sets the configuration template filename.
     * @param s Configuration template filename
     */
    public void setConfigurationTemplateFilename(String s) {
        this.configurationTemplateFilename = s;
    }

    /**
     * Returns the transportation vehicle information filename.
     * @return Transportation vehicle information filename
     */
    public String getTrnVehInfoFilename() {
        return trnVehInfoFilename;
    }

    /**
     * Sets the transportation vehicle information filename.
     * @param s Transportation vehicle information filename
     */
    public void setTrnVehInfoFilename(String s) {
        this.trnVehInfoFilename = s;
    }

    /**
     * Returns the technology boundary list filename.
     * @return Technology boundary list filename
     */
    public String getTchBndListFilename() {
        return tchBndListFilename;
    }

    /**
     * Sets the technology boundary list filename.
     * @param s Technology boundary list filename
     */
    public void setTchBndListFilename(String s) {
        this.tchBndListFilename = s;
    }

    /**
     * Returns the GCAM data directory.
     * @return GCAM data directory
     */
    public String getgCamDataDir() {
        return gCamDataDir;
    }

    /**
     * Sets the GCAM data directory.
     * @param s GCAM data directory
     */
    public void setgCamDataDir(String s) {
        this.gCamDataDir = s;
    }

    /**
     * Returns the GCAM output database.
     * @return GCAM output database
     */
    public String getgCamOutputDatabase() {
        return gCamOutputDatabase;
    }

    /**
     * Sets the GCAM output database.
     * @param s GCAM output database
     */
    public void setgCamOutputDatabase(String s) {
        this.gCamOutputDatabase = s;
    }
    

    /**
     * Returns the maximum database size in GB.
     * @return Maximum database size in GB
     */
    public float getMaxDatabaseSize() {
        return getMaxDatabaseSizeGB();
    }

    /**
     * Sets the maximum database size in GB.
     * @param f Maximum database size in GB
     */
    public void setMaxDatabaseSizeGB(float f) {
        this.maxDatabaseSizeGB = f;
    }
    
    /**
     * Returns the options filename.
     * @return Options filename
     */
    public String getOptionsFilename() {
        return optionsFilename;
    }

    /**
     * Sets the options filename.
     * @param s Options filename
     */
    public void setOptionsFilename(String s) {
        this.optionsFilename = s;
    }

    /**
     * Returns the XML library.
     * @return XML library
     */
    public String getXmlLibrary() {
        return xmlLibrary;
    }

    /**
     * Sets the XML library.
     * @param s XML library
     */
    public void setXmlLibrary(String s) {
        this.xmlLibrary = s;
    }

    /**
     * Returns the text editor.
     * @return Text editor
     */
    public String getTextEditor() {
        return textEditor;
    }

    /**
     * Sets the text editor.
     * @param s Text editor
     */
    public void setTextEditor(String s) {
        this.textEditor = s;
    }

    /**
     * Returns the XML editor.
     * @return XML editor
     */
    public String getXmlEditor() {
        return xmlEditor;
    }

    /**
     * Sets the XML editor.
     * @param s XML editor
     */
    public void setXmlEditor(String s) {
        this.xmlEditor = s;
    }

    /**
     * Returns the monetary conversions filename.
     * @return Monetary conversions filename
     */
    public String getMonetaryConversionsFilename() {
        return this.monetaryConversionsFilename;
    }

    /**
     * Sets the monetary conversions filename.
     * @param s Monetary conversions filename
     */
    public void setMonetaryConversionsFilename(String s) {
        this.monetaryConversionsFilename = s;
    }

    /**
     * Returns the description text.
     * @return Description text
     */
    public String getDescriptionText() {
        return descriptionText;
    }

    /**
     * Sets the description text.
     * @param s Description text
     */
    public void setDescriptionText(String s) {
        this.descriptionText = s;
    }

    /**
     * Returns the stop period.
     * @return Stop period
     */
    public String getStopPeriod() {
        return stopPeriod;
    }

    /**
     * Sets the stop period.
     * @param s Stop period
     */
    public void setStopPeriod(String s) {
    	this.stopPeriod = s;
    	int yearInt = Integer.parseInt(s);
    	this.stopYear = utils.getYearForPeriod(yearInt);
    }
    

    /**
     * Returns the stop year.
     * @return Stop year
     */
    public String getStopYear() {
        return stopYear;
    }

    /**
     * Sets the stop year.
     * @param s Stop year
     */
    public void setStopYear(String s) {
        this.stopYear = s;
        this.stopPeriod = utils.getPeriodForYear(s);
    }
    
    /**
     * Returns the run queue string.
     * @return Run queue string
     */
    public String getRunQueueStr() {
        return runQueueStr;
    }

    /**
     * Sets the run queue string.
     * @param s Run queue string
     */
    public void setRunQueueStr(String s) {
        this.runQueueStr = s;
    }

    /**
     * Returns the end-of-line string.
     * @return End-of-line string
     */
    public String getEol() {
        return eol;
    }

    /**
     * Sets the end-of-line string.
     * @param s End-of-line string
     */
    public void setEol(String s) {
        this.eol = s;
    }

    /**
     * Returns the CSV column filename.
     * @return CSV column filename
     */
    public String getCsvColumnFilename() {
        return csvColumnFilename;
    }

    /**
     * Sets the CSV column filename.
     * @param s CSV column filename
     */
    public void setCsvColumnFilename(String s) {
        csvColumnFilename = s;
    }

    /**
     * Returns whether to use icons.
     * @return "true" if using icons, "false" otherwise
     */
    public String getUseIcons() {
        return useIcons;
    }

    /**
     * Sets whether to use icons.
     * @param str "true" or "yes" to use icons, "false" otherwise
     */
    public void setUseIcons(String str) {
        if ((str.toLowerCase().equals("yes")) || (str.toLowerCase().equals("true")) || (str.toLowerCase().equals("1"))) {
            useIcons = "true";
        } else {
            useIcons = "false";
        }
    }

    /**
     * Returns the XML header filename.
     * @return XML header filename
     */
    public String getXmlHeaderFilename() {
        return xmlHeaderFilename;
    }

    /**
     * Sets the XML header filename.
     * @param s XML header filename
     */
    public void setXmlHeaderFilename(String s) {
        xmlHeaderFilename = s;
    }

    /**
     * Returns the preferred font size.
     * @return Preferred font size
     */
    public String getPreferredFontSize() {
        return preferredFontSize;
    }

    /**
     * Sets the preferred font size.
     * @param s Preferred font size
     */
    public void setPreferredFontSize(String s) {
        try {
            int size = Integer.parseInt(s);
            preferredFontSize = s;
            styles.setFontSize(size);
        } catch (Exception e) {
            System.out.println("Could not convert font size string " + s + " to double.");
        }
    }

    /**
     * Gets the value of a parameter as a string.
     * @param param Parameter name
     * @return Parameter value
     */
    private String get(String param) {
        String returnVal = "";

        param = param.toLowerCase().trim();

        switch (param) {
        case "allowablepolicyyears":
            returnVal = ""+allowablePolicyYears;
            break;
        case "allyears":
            returnVal = ""+allYears;
            break;
        case "useallavailableprocessors":
            returnVal = ""+useAllAvailableProcessors;
            break;
        case "showsplash":
            returnVal = ""+showSplash;
            break;
        case "buildinfo":
            returnVal = buildInfo;
            break;
        case "executecmdshort":
            returnVal = executeCmdShort;
            break;
        case "executecmd":
            returnVal = executeCmd;
            break;
        case "glimpsedir":
            returnVal = glimpseDir;
            break;
        case "glimpseresourcedir":
            returnVal = glimpseResourceDir;
            break;
        case "glimpsedocdir":
            returnVal = glimpseDocDir;
            break;
        case "gcamhomedir":
            returnVal = gCamHomeDir;
            break;
        case "solver":
            returnVal = gCamSolver;
            break;
        case "gcamsolver":
            returnVal = gCamSolver;
            break;
        case "scenariobuilderdir":
            returnVal = scenarioBuilderDir;
            break;
        case "gcamexecutable":
            returnVal = gCamExecutable;
            break;
        case "gcamexecutableargs":
            returnVal = gCamExecutableArgs;
            break;
        case "gcamexecutabledir":
            returnVal = gCamExecutableDir;
            break;
        case "modelinterfacejar":
            returnVal = modelInterfaceJar;
            break;
        case "modelinterfacedir":
            returnVal = modelInterfaceDir;
            break;
        case "modelinterfacejardir":
            returnVal = modelInterfaceJarDir;
            break;
        case "filestosave":
            returnVal = filesToSave;
            break;
        case "gcamoutputtosave":
            returnVal = filesToSave;
            break;
        case "scenariocomponentsdir":
            returnVal = scenarioComponentsDir;
            break;
        case "scenarioxmldir":
            returnVal = scenarioDir;
            break;
        case "scenariodir":
            returnVal = scenarioDir;
            break;
        case "glimpselogdir":
            returnVal = glimpseLogDir;
            break;
        case "queryfilename":
            returnVal = queryFilename;
            break;
        case "favoritequeryfilename":
            returnVal = queryFilename;
            break;
        case "scenariobuilderjardir":
            returnVal = scenarioBuilderJarDir;
            break;
        case "scenariobuilderjar":
            returnVal = scenarioBuilderJar;
            break;
        case "resourcedir":
            returnVal = resourceDir;
            break;
        case "presetregionlistfilename":
            returnVal = presetRegionListFilename;
            break;
        case "regionlistfilename":
            returnVal = regionListFilename;
            break;
        case "subregionlistfilename":
            returnVal = subRegionListFilename;
            break;
        case "trashdir":
            returnVal = trashDir;
            break;
        case "configurationtemplatefilename":
            returnVal = configurationTemplateFilename;
            break;
        case "tranloadfactorsfilename":
            returnVal = trnVehInfoFilename;
            break;
        case "trnvehinfofilename":
            returnVal = trnVehInfoFilename;
            break;
        case "tchbndlistfilename":
            returnVal = tchBndListFilename;
            break;
        case "gcamdatadir":
            returnVal = gCamDataDir;
            break;
        case "gcamoutputdatabase":
            returnVal = gCamOutputDatabase;
            break;
        case "maxdatabasesizegb":
            returnVal = ""+getMaxDatabaseSizeGB();
            break;
        case "optionsfilename":
            returnVal = optionsFilename;
            break;
        case "xmllibrary":
            returnVal = xmlLibrary;
            break;
        case "texteditor":
            returnVal = textEditor;
            break;
        case "xmleditor":
            returnVal = xmlEditor;
            break;
        case "descriptiontext":
            returnVal = descriptionText;
            break;
        case "stopperiod":
        case "stop-period":
            returnVal = stopPeriod;
            break;
        case "stopyear":
            returnVal = stopYear;
            break;
        case "stop-year":
            returnVal = stopYear;
            break;
        case "runqueuestr":
            returnVal = runQueueStr;
            break;
        case "eol":
            returnVal = eol;
            break;
        case "isgcamusa":
            returnVal = String.valueOf(isGcamUSA);
            break;
        case "csvcolumnfilename":
            returnVal = csvColumnFilename;
            break;
        case "xmlheaderfilename":
            returnVal = xmlHeaderFilename;
            break;
        case "preferredfontsize":
            returnVal = preferredFontSize;
            break;
        case "useicons":
            returnVal = useIcons;
            break;
        case "use_icons":
            returnVal = useIcons;
            break;
        case "unitconversionsfilename":
            returnVal = unitConversionsFilename;
            break;
        case "monetaryconversionsfilename":
            returnVal = monetaryConversionsFilename;
            break;
        case "debugregion":
            returnVal = debugRegion;
            break;
        case "debugcreate":
            returnVal = debugCreate;
            break;
        case "startyearforshare":
            returnVal = startYearForShare;
            break;
        case "debugrename":
            returnVal = debugRename;
            break;
        }
        if ((returnVal==null)||(returnVal.equals(""))) System.out.println("No match for "+param);
        return returnVal;
    }

    /**
     * Sets the value of a parameter.
     * @param param Parameter name
     * @param val Parameter value
     */
    private void set(String param, String val) {

	if ((val==null)||(param==null)) return;
	if (val.trim().isEmpty() || param.trim().isEmpty()) return;
	
    param = param.toLowerCase().trim();
    if (val.indexOf("#") > -1){
        val = fixDir(val);
    }


        switch (param) {

        case "glimpsedir":
            String current_dir=System.getProperty("user.dir");
            glimpseDir = fixDir(val);
            if (glimpseDir.startsWith(".")) glimpseDir=current_dir;

            break;
        case "gcamguidir":
        case "glimpseguidir":
        case "scenariobuilderdir":
        	scenarioBuilderDir = fixDir(val);
            break;  
        case "scenariobuilderjardir":
            scenarioBuilderJarDir = fixDir(val);
            break;
        case "scenariobuilderconfigdir":
            scenarioBuilderConfigDir = fixDir(val);
            break;
        case "modelinterfaceconfigdir":
            modelInterfaceConfigDir = fixDir(val);
            break;
        case "scenariobuilderjar":
            scenarioBuilderJar = val;
            break;
        case "gcamhomedir":
            gCamHomeDir = fixDir(val);
            break;        
        case "useallavailableprocessors":
            setUseAllAvailableProcessors(val);
            break;
        case "showsplash":
            setShowSplash(val);
            break;
        case "buildinfo":
            buildInfo = val;
            break;
        case "executecmdshort":
            executeCmdShort = val;
            break;
        case "executecmd":
            executeCmd = val;
            break;

        case "glimpsedocdir":
            glimpseDocDir = fixDir(val);
            break;
        case "glimpseresourcedir":
            glimpseResourceDir = fixDir(val);
            break;
        case "solver":
            gCamSolver = fixDir(val);
            break;
        case "gcamsolver":
            gCamSolver = fixDir(val);
            break;
        case "gcamexecutable":
            gCamExecutable = fixDir(val);
            break;
        case "gcamexecutableargs":
            gCamExecutableArgs = val;
            break;
        case "gcamexecutabledir":
            gCamExecutableDir = fixDir(val);
            break;
        case "modelinterfacejar":
            modelInterfaceJar = fixDir(val);
            break;
        case "modelinterfacedir":
            modelInterfaceDir = fixDir(val);
            break;
        case "modelinterfacejardir":
            modelInterfaceJarDir = fixDir(val);
            break;
        case "gcamoutputtosave":
            filesToSave = fixDir(val);
            break;
        case "filestosave":
            filesToSave = fixDir(val);
            break;
        case "scenariocomponentsdir":
            scenarioComponentsDir = fixDir(val);
            break;
        case "scenarioxmldir":
            scenarioDir = fixDir(val);
            break;
        case "scenariodir":
            scenarioDir = fixDir(val);
            break;
        case "glimpselogdir":
            glimpseLogDir = fixDir(val);
            break;
        case "queryfilename":
            queryFilename = fixDir(val);
            break;
        case "favoritequeryfilename":
            favoriteQueryFilename = fixDir(val);
            break;
        case "unitconversionsfilename":
            unitConversionsFilename = fixDir(val);
            break;
        case "resourcedir":
            resourceDir = fixDir(val);
            break;
        case "presetregionlistfilename":
            presetRegionListFilename = fixDir(val);
            break;
        case "regionlistfilename":
            regionListFilename = fixDir(val);
            break;
        case "subregionlistfilename":
            subRegionListFilename = fixDir(val);
            break;            
        case "trashdir":
            trashDir = fixDir(val);
            break;
        case "configurationtemplatefilename":
            configurationTemplateFilename = fixDir(val);
            break;
        case "tranloadfactorsfilename":
            trnVehInfoFilename = fixDir(val);
            break;
        case "trnvehinfofilename":
            trnVehInfoFilename = fixDir(val);
            break;
        case "tchbndlistfilename":
            tchBndListFilename = fixDir(val);
            break;
        case "gcamdatadir":
            gCamDataDir = fixDir(val);
            break;
        case "gcamoutputdatabase":
            gCamOutputDatabase = fixDir(val);
            break;
        case "maxdatabasesizegb":
            setMaxDatabaseSizeGB(Float.parseFloat(val));
            break;
        case "optionsfilename":
            optionsFilename = fixDir(val);
            break;
        case "xmllibrary":
            xmlLibrary = fixDir(val);
            break;
        case "texteditor":
            textEditor = fixDir(val);
            break;
        case "xmleditor":
            xmlEditor = fixDir(val);
            break;
        case "descriptiontext":
            descriptionText = fixDir(val);
            break;
        case "stopperiod":
        case "stop-period":
        	this.setStopPeriod(val);
            break;
        case "stopyear":
        case "stop-year":
            this.setStopYear(val);
            break;
        case "allowablepolicyyears":
            setAllowablePolicyYears(val);
            break;
        case "allowablepolicyyearlist":
            setAllowablePolicyYears(val);
            break;
        case "startyearforshare":
            setStartYearForShare(val);
            break;    
        case "allyears":
            setAllYears(val);
            break;      
        case "allyearslist":
            setAllYears(val);
            break; 
        case "allyear":
            setAllYears(val);
            break;      
        case "allyearlist":
            setAllYears(val);
            break; 
        case "runqueuestr":
            runQueueStr = fixDir(val);
            break;
        case "eol":
            eol = val;
            break;
        case "isgcamusa":
            isGcamUSA = false;
            if (val.toLowerCase().trim().equals("true"))
                isGcamUSA = true;
            break;
        case "csvcolumnfilename":
            csvColumnFilename = fixDir(val);
            break;
        case "xmlheaderfilename":
            xmlHeaderFilename = fixDir(val);
            break;
        case "preferredfontsize":
            setPreferredFontSize(val);
            break;
        case "useicons":
            setUseIcons(val);
            break;
        case "use_icons":
            setUseIcons(val);
            break;
        case "monetaryconversionsfilename":
            setMonetaryConversionsFilename(fixDir(val));
            break;
        case "debugregion":
            setDebugRegion(val);
            break;
        case "debugcreate":
            setDebugCreate(val);
            break;
        case "debugrename":
            setDebugRename(val);
            break;    
        case "calibrationyear":
            setCalibrationYear(val);
            break;
        case "regionlist":
			setRegionList(val);
			break;
        case "subregionlist":
        	setSubRegionList(val);
        	break;
        case "ghglist":
			setGhgListStr(val);
			break;
        case "ghggwplist":
			setGhgGWPListStr(val);
			break;
        case "ghgunitlist":
        	setGhgUnitListStr(val);
        	break;
        case "pollutantlist":
			setPollutantList(val);
			break;
        default:
			System.out.println("No match for " + param+ getEol() +"   ->param:" + param + " val:" + val);
			break;
        }

        if (param.indexOf("dir") > 0) {
            testDirExists(val);
        }

        return;
    }

    private void setRegionList(String val) {
        /**
         * Sets the region list from a comma-separated string.
         * @param val Comma-separated region list
         */
        regionList = utils.getStringListFromString(val, ",");
    }

    private void setSubRegionList(String val) {
        /**
         * Sets the subregion list from a comma-separated string.
         * @param val Comma-separated subregion list
         */
        subRegionList = utils.getStringListFromString(val, ",");
    }
    
	/**
     * Fixes directory paths in the options file for backward compatibility.
     * @param filename Original filename
     * @return Fixed filename
     */
    private String fixDir(String filename) {
        /**
         * Fixes directory paths in the options file for backward compatibility.
         * @param filename Original filename
         * @return Fixed filename
         */

        //for backwards compatibility on options file... wildcards surrounded by #
        if (filename.indexOf("#glimpseDir#") > -1) {
            filename = filename.replace("#glimpseDir#", glimpseDir);
        }
        if (filename.indexOf("#gCamHomeDir#") > -1) {
            filename = filename.replace("#gCamHomeDir#", gCamHomeDir);
        }
        if (filename.indexOf("#scenarioBuilderDir#") > -1) {
            filename = filename.replace("#scenarioBuilderDir#", scenarioBuilderDir);
        }
        if (filename.indexOf("#modelInterfaceDir#") > -1) {
            filename = filename.replace("#modelInterfaceDir#", modelInterfaceDir);
        }
        
        //for new convention with wildcards surrounded by $s
        if (filename.indexOf("$glimpseDir$") > -1) {
            filename = filename.replace("$glimpseDir$", glimpseDir);
        }
        if (filename.indexOf("$gCamHomeDir$") > -1) {
            filename = filename.replace("$gCamHomeDir$", gCamHomeDir);
        }
        if (filename.indexOf("$scenarioBuilderDir$") > -1) {
            filename = filename.replace("$scenarioBuilderDir$", scenarioBuilderDir);
        }
        if (filename.indexOf("$modelInterfaceDir$") > -1) {
            filename = filename.replace("$modelInterfaceDir$", modelInterfaceDir);
        }

        filename = filename.replace("/", File.separator).replace("\\", File.separator).replace("\\\\", File.separator);

        return filename;
    }

    /**
     * Tests if a directory exists.
     * @param pathName Directory path
     * @return True if the directory exists, false otherwise
     */
    private boolean testDirExists(String pathName) {
        /**
         * Tests if a directory exists.
         * @param pathName Directory path
         * @return True if the directory exists, false otherwise
         */
        return testDirExists(pathName, false);
    }

    /**
     * Tests if a directory exists, with optional fatal error handling.
     * @param pathName Directory path
     * @param isFatal True for fatal error, false otherwise
     * @return True if the directory exists, false otherwise
     */
    private boolean testDirExists(String pathName, boolean isFatal) {
        /**
         * Tests if a directory exists, with optional fatal error handling.
         * @param pathName Directory path
         * @param isFatal True for fatal error, false otherwise
         * @return True if the directory exists, false otherwise
         */

        boolean b = true;

        try {
            File f = new File(pathName);
            b = f.isDirectory();
        } catch (Exception E) {
            System.out.println("error openning: " + pathName);
            b = false;
            if (isFatal) {
                System.out.println("exiting");
                System.exit(0);
            }
        }

        return b;
    }
    
    /**
     * Tests if a file exists.
     * @param pathName File path
     * @return True if the file exists, false otherwise
     */
    private boolean testFileExists(String pathName) {
        /**
         * Tests if a file exists.
         * @param pathName File path
         * @return True if the file exists, false otherwise
         */

        boolean b = true;

        try {
            File f = new File(pathName);
            b = f.exists();
        } catch (Exception E) {
            System.out.println("error openning: " + pathName);
            b = false;
        }

        return b;
    }

    /**
     * Loads options from a specified file.
     * @param filename Options file name
     */
    public void loadOptions(String filename) {
        set("optionsFilename", filename);
        loadOptions();
    }

    /**
     * Loads options from the options file.
     */
    public void loadOptions() {
        ArrayList<String[]> keyValuePairs = null;
        File optionsFile = new File(getOptionsFilename());
        
        System.out.println("Loading options from " + getOptionsFilename());

        if (!optionsFile.exists()) {
            System.out.println("Specified options file does not exist.");
            //utils.warningMessage("Specified options file does not exist. Please check command-line argument.");
            return;
        }

        files.optionsFileContent = files.getStringArrayFromFile(getOptionsFilename(), "#");

        String current_line="";
        
        try {

            keyValuePairs = files.loadKeyValuePairsFromFile(getOptionsFilename(), "=");

            for (int i = 0; i < keyValuePairs.size(); i++) {

                current_line=keyValuePairs.get(i)[0];
                String[] s = keyValuePairs.get(i);
                String s0 = s[0].trim();
                String s1 = s[1].trim();
                
                if (!s0.startsWith("#")) { // pound is the symbol for a comment
                    set(s0, s1);
                }
            }
        } catch (Exception e) {
            //utils.warningMessage("Problem reading options file.");
            System.out.println("Error reading options file: " + optionsFile);
            System.out.println("Line: "+current_line);
            System.out.println("Error: " + e);
            //utils.exitOnException();
        }
        return;
    }

    /**
     * Returns an ArrayList of options in the format "key = value".
     * @return ArrayList of options
     */
    public ArrayList<String> getArrayListOfOptions() {

        files.optionsFileContent=files.getStringArrayFromFile(optionsFilename, "#");
        
        ArrayList<String> optionsFileContent = files.optionsFileContent;
        ArrayList<String> completed_arrayList = new ArrayList<>();

        try {
            for (int i = 0; i < optionsFileContent.size(); i++) {
                String line = optionsFileContent.get(i);
                int loc_of_equals = line.indexOf("=");
                if (loc_of_equals > 0) {
                    String s1 = line.substring(0, loc_of_equals - 1).trim();
                    String s2 = line.substring(loc_of_equals + 1).trim();
                    String val = this.get(s1);
                    if (val != null) {
                        //System.out.println("s1:"+s1+":"+s2+":"+val+":");
                        completed_arrayList.add(s1 + " = " + val);
                    } else {
                        System.out.println("No translation for "+s1);
                    }
                }
            }
        } catch (Exception e) {
            utils.warningMessage("Difficulty reading options file.");
            System.out.println("Difficulty reading options file. Attempting to continue.");
            completed_arrayList.add("");
        }

        return completed_arrayList;
    }
    
    /**
     * Returns the technology information as a matrix.
     * @return Technology information matrix
     */
    public String[][] getTechInfo(){
        
        if (techInfo==null) {
            techInfo=getTechInfoAsMatrix();
        }
        
        return techInfo != null ? techInfo : new String[0][0];
    }
    
    /**
     * Returns the sector information derived from technology information.
     * @return Sector information
     */
    public String[][] getSectorInfo(){
        
        if (techInfo==null) {
            techInfo=getTechInfoAsMatrix();
        }
        if (techInfo == null) return new String[0][0];
        
        ArrayList<String> sector_list=new ArrayList<>(); 
        ArrayList<String> output_list=new ArrayList<>(); 
        ArrayList<String> units_list=new ArrayList<>(); 
        for (int i=0;i<techInfo.length;i++) {
            String sect_i=techInfo[i][0].trim();
            String output_i=techInfo[i][5].trim();
            String units_i=techInfo[i][6].trim();
            
            int current_len=sector_list.size();
            boolean match=false;
            for (int j=0;j<current_len;j++) {
                if (sector_list.get(j).equals(sect_i)) {
                    match=true;
                    break;
                }
            }
            if (!match) {
                sector_list.add(sect_i);
                output_list.add(output_i);
                units_list.add(units_i);
            }
        }
        
        String[][] sector_info=new String[sector_list.size()][3];
        for (int i=0;i<sector_list.size();i++) {
            sector_info[i][0]=sector_list.get(i);
            sector_info[i][1]=output_list.get(i);
            sector_info[i][2]=units_list.get(i);
        }
        
        return sector_info;
    }
    
    /**
     * Returns the technology information as a matrix.
     * @return Technology information matrix
     */
    private String[][] getTechInfoAsMatrix() {
        /**
         * Returns the technology information as a matrix from the file content.
         * @return Technology information matrix
         */

        String[][] returnStringMatrix = null;
        String text="";
        
        try {

            ArrayList<String> arrayList = files.glimpseTechBoundFileContent;

            if ((arrayList == null) || (arrayList.size() == 0))
                throw (new Exception("arrayList not read from file."));
            
            int size_j=0;
            
            text=arrayList.get(0).trim();
            String[] textSplit = null;
            String delim=null;
            
            if (text.contains(":")){ 
                delim=":";
            } else {
                delim=",";
            }
            textSplit=text.split(delim);
            size_j=textSplit.length;

            returnStringMatrix = new String[arrayList.size()][size_j];

            //only reads in first 5 fields
            for (int i = 0; i < arrayList.size(); i++) {
                text = arrayList.get(i).trim();
                if (text.length()>0) {
                textSplit = text.split(delim);
            
                  for (int j = 0; j < size_j ; j++) {
                    returnStringMatrix[i][j] = textSplit[j].trim();
                  }
                }
            }

        } catch (Exception e) {
            utils.warningMessage("Problem reading tech list: "+text);
            System.out.println("Error reading tech list from " + get("tchBndListFile") + ":");
            System.out.println("  ---> " + e);
        }
        return returnStringMatrix;
    }
    
    /**
     * Examines the GLIMPSE setup for potential issues.
     * @return Analysis report
     */
    public String examineGLIMPSESetup() {

        String rtn_str="";
        
        //testing to make sure key parameters have been defined
        String[] params= {
                "glimpseDir","glimpseResourceDir","glimpseDocDir","gCamHomeDir","solver","gCamSolver",
                "scenarioBuilderJar","scenarioBuilderDir",
                "gCamExecutable","gCamExecutableArgs","gCamExecutableDir",
                "modelInterfaceJar","modelInterfaceJarDir",
                "scenarioComponentsDir","scenarioXMLDir","scenarioDir",
                "configurationTemplateFilename","queryFilename","favoriteQueryFilename",
                "tchBndListFilename","tranLoadFactorsFilename",
                "regionListFilename","subRegionListFilename","presetRegionListFilename",
                "monetaryConversionsFilename","xmlHeaderFilename",
                "scenarioBuilderJarDir","scenarioBuilderJar",
                "resourceDir","trashDir","gCamDataDir","gCamOutputDatabase",
                "maxDatabaseSizeGB","optionsFilename","xmlLibrary","textEditor","xmlEditor",
                "stopPeriod","runQueueStr",
                "isGcamUSA","preferredFontSize","useIcons","use_icons","debugRegion",
                "debugCreate","startYearForShare","debugRename","filesToSave"};
        
        ArrayList<String> report=new ArrayList<String>();
        
        report.add(" ");
        report.add("------ Analysis of GLIMPSE setup --------");
        
        boolean params_correct=true;
        
        for (int i=0;i<params.length;i++) {
            String str=params[i];
            String val=get(str);
            if (val==null) {
                params_correct=false; 
                String s="*** Parameter "+str+" is undefined. ***";
                report.add(s);
            } else {
                if (str.indexOf("Dir")>0) {
                    String dir_name=val;
                    if (!this.testDirExists(dir_name)) {
                        params_correct=false;
                        String s="*** Specified folder for "+str+" does not exist: "+val+" ***";
                        System.out.println("warning: "+s);
                        report.add(s);
                    }
                    
                } 
            }            
        }
        if (params_correct) report.add("No problems found with parameters or folders.");


        //checks to see if there are spaces in path
        boolean no_spaces_in_path=true;
        String good_glimpse_folder = this.getGlimpseDir();
        
        if (good_glimpse_folder.contains(" ")) {
            no_spaces_in_path=false;
            String s="*** Potentially problematic installation location: The path to your GLIMPSE root folder includes at least one space character. This can cause problems with GLIMPSE operation. Please move GLIMPSE to a folder that does not contain the space character.";		    
            report.add(s);
        } else {
            report.add("No problem was found with spaces in the path to the GLIMPSE root folder.");
        }
        
        //checks to see if folders are nested
        boolean no_nesting=true;
        
        String bad_glimpse_path = good_glimpse_folder + File.separator
                + good_glimpse_folder.substring(good_glimpse_folder.lastIndexOf(File.separator) + 1);
        if (this.testDirExists(bad_glimpse_path)) {
            no_nesting = false;
            String s = "*** Potentially problematic installation location: Found nesting of GLIMPSE folders. This usually occurs during unzipping or when a GLIMPSE update is placed within the " + good_glimpse_folder
                    + " as opposed to on top of it. Nesting is not neccesarily an issue, but may result in confusion in the future and, in some instances, may result in file pathnames exceeding the length that can be handled by the operating system. Please check your installation. The Installation Guide in the GLIMPSE documentation provides some instructions to address this issue. ***";
            report.add(s);
        } else {
            report.add("No problem was found with nesting of GLIMPSE folders.");
        }
        
        //checks to make sure the full path is not super long
        boolean path_len_ok=true;
        int path_len=this.getGlimpseDir().length();
        if (path_len>150) {
            path_len_ok=false; 
            String s = "*** Full path length to the GLIMPSE root folder is "+path_len+" characters. This exceeds the recommended length of 150 and, in some circumstances, may result in file access problems on operating systems that limit path length to 256 total characters. Please consider re-locating your GLIMPSE root folder such that it has a shorter path.";  
            report.add(s);
        } else {
            report.add("No problem was found with path length.");
        }
        
        //checks to see if java_home is defined 
        boolean found_java=true;
        
        String java_home_folder=System.getenv("JAVA_HOME");
        if (!files.testFolderExists(java_home_folder)) {
            found_java=false;
            String s="*** Your JAVA_HOME is set to "+java_home_folder+", but that folder does not exist. Please update the JAVA_HOME setting in the run_GCAM*.bat file you used to start GLIMPSE. If using the standard version of Java, it is typically C:/Program Files/Java/jre1.8.0_XXX, where you will need to update XXX with the version on your computer. ***";
            report.add(s);
        } else {
            String s="Your JAVA_HOME folder, "+java_home_folder+", was successfully found.";
            report.add(s); 
        }
        
        if ((found_java)&&(no_nesting)&&(params_correct)&&(path_len_ok)&&(no_spaces_in_path)) {
            report.add("Installation at location "+this.getGlimpseDir()+" appears to be succesful.");
        } else {
            report.add("*** One or more problems found with GLIMPSE installation. ***");
        }
        
        report.add(" ");
        report.add("------ Check to verify that key files exist as specified --------");
        String filename=this.getXmlHeaderFilename();
        String s="XML header file: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);
        filename=this.getCsvColumnFilename();
        s="CSV column file for Tech Param: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);
        filename=this.getTchBndListFilename();
        s="Tech Bound file: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);
        filename=this.getConfigurationTemplateFilename();
        s="Configuration template file: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);		
        filename=this.getQueryFilename();
        s="Query file: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);	
        filename=this.getgCamExecutableDir()+File.separator+this.getgCamExecutable();
        s="GCAM executable: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);	
        filename=this.getModelInterfaceJarDir()+File.separator+this.getModelInterfaceJar();
        s="ModelInterface executable: "+filename+" - "+files.doesFileExist(filename);
        report.add(s);			
        
        try {
            report.add(" ");
            report.add("------ Computer Information --------");
            double gb=1073741824;
            com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                     java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            report.add("-- Memory analysis -- ");
            try {

                float physicalMemorySize = (float) (os.getTotalPhysicalMemorySize()/gb);
                float physicalMemoryFree = (float) (os.getFreePhysicalMemorySize()/gb);

                report.add(String.format("Total physical memory: %.1f GB",physicalMemorySize));	
                report.add(String.format("Free physical memory: %.1f GB",physicalMemoryFree));
                
                if ((physicalMemorySize<12.0)&&(!this.isGcamUSA)) report.add("*** At least 12 GB of RAM are recommended for GCAM. The model may stop unexpectedly if RAM is exhausted. ***");
                if ((physicalMemorySize<14.0)&&(this.isGcamUSA)) report.add("*** At least 14 GB of RAM are recommended for GCAM-USA, although 16 or more may be required when using complex policies such as RPS or CES. The model may stop unexpectedly if RAM is exhausted. ***");				
                report.add("");		
            } catch(Exception e1) {
                report.add("Java version does not support assessing physical memory.");
                report.add("");
            }
                
            Runtime rt = Runtime.getRuntime();

            report.add("-- Disk space analysis -- ");			
            File drive = new File("/");
            
            double total_space=(double)(drive.getTotalSpace() /gb);
            double free_space=(double)(drive.getFreeSpace() /gb);
            
            report.add(String.format("Total space: %.1f GB",total_space));
            report.add(String.format("Free space: %.1f GB",free_space));
            if (free_space<100) report.add("*** Warning: Free space is limited. At least 100 GB is advised. ***");

            try {

                float swapSpaceSize = (float) (os.getTotalSwapSpaceSize()/gb);
                if ((swapSpaceSize<25.0)&&(this.isGcamUSA)) report.add("*** At least 25 GB of swap space are recommended. The model may stop unexpectedly if swap space is exhausted. ***");

                float freeSwapSpace = (float) (os.getFreeSwapSpaceSize()/gb);

                report.add(String.format("Total swap space: %.1f GB",swapSpaceSize));	
                report.add(String.format("Free swap space: %.1f GB",freeSwapSpace));
                report.add("");		
            } catch(Exception e1) {
                report.add("Java version does not support assessing swap space size.");
                report.add("");
            }			
                    
            report.add("-- Processor analysis -- ");
            int available_processors = rt.availableProcessors();
            report.add("Available processor cores: "+available_processors);
            float cpu_load=(float)(os.getSystemCpuLoad());
            report.add(String.format("Current usage: %.1f", (float)cpu_load*100.)+"%");
            report.add("");
                        
        } catch(Exception e) {
            System.out.println("Problem checking computer attributes (e.g., RAM)");
        }
        
        //utils.displayArrayList(report,"Result of Check Installation",true);
        
        rtn_str=utils.createStringFromArrayList(report);
        
        return rtn_str;
    }

    /**
     * Returns a list of unique technology types from the technology boundary file.
     * @return List of technology types
     */
    public ArrayList<String> getCategoriesFromTechBnd(){
        ArrayList<String> result=new ArrayList<>();
        
        String[][] tech_info = getTechInfo();
        
        result.add("All");
        
        for (int i=0;i<tech_info.length;i++) {
            utils.addToArrayListIfUnique(result,tech_info[i][tech_info[0].length-1]);
        }
        
        result=utils.getUniqueItemsFromStringArrayList(result);
        return result;
    }

	public float getMaxDatabaseSizeGB() {
		return maxDatabaseSizeGB;
	}
    
	public int getPeriodIncrement() { return this.periodIncrement; }
	
    // Optional-returning getters for nullable fields
    public Optional<String> getAboutTextFilenameOptional() { return Optional.ofNullable(aboutTextFilename); }
    public Optional<String> getPresetRegionListFilenameOptional() { return Optional.ofNullable(presetRegionListFilename); }
    public Optional<String> getSubRegionsFilenameOptional() { return Optional.ofNullable(subRegionListFilename); }
    public Optional<String> getRegionListFilenameOptional() { return Optional.ofNullable(regionListFilename); }
    public Optional<String> getGlimpseDirOptional() { return Optional.ofNullable(glimpseDir); }
    public Optional<String> getGlimpseResourceDirOptional() { return Optional.ofNullable(glimpseResourceDir); }
    public Optional<String> getGlimpseDocDirOptional() { return Optional.ofNullable(glimpseDocDir); }
    public Optional<String> getgCamHomeDirOptional() { return Optional.ofNullable(gCamHomeDir); }
    public Optional<String> getScenarioBuilderDirOptional() { return Optional.ofNullable(scenarioBuilderDir); }
    public Optional<String> getScenarioBuilderJarOptional() { return Optional.ofNullable(scenarioBuilderJar); }
    public Optional<String> getScenarioBuilderJarDirOptional() { return Optional.ofNullable(scenarioBuilderJarDir); }
    public Optional<String> getgCamExecutableOptional() { return Optional.ofNullable(gCamExecutable); }
    public Optional<String> getgCamExecutableDirOptional() { return Optional.ofNullable(gCamExecutableDir); }
    public Optional<String> getModelInterfaceJarOptional() { return Optional.ofNullable(modelInterfaceJar); }
    public Optional<String> getModelInterfaceJarDirOptional() { return Optional.ofNullable(modelInterfaceJarDir); }
    public Optional<String> getModelInterfaceDirOptional() { return Optional.ofNullable(modelInterfaceDir); }
    public Optional<String> getFilesToSaveOptional() { return Optional.ofNullable(filesToSave); }
    public Optional<String> getScenarioComponentsDirOptional() { return Optional.ofNullable(scenarioComponentsDir); }
    public Optional<String> getScenarioDirOptional() { return Optional.ofNullable(scenarioDir); }
    public Optional<String> getGlimpseLogDirOptional() { return Optional.ofNullable(glimpseLogDir); }
    public Optional<String> getResourceDirOptional() { return Optional.ofNullable(resourceDir); }
    public Optional<String> getTrashDirOptional() { return Optional.ofNullable(trashDir); }
    public Optional<String> getgCamDataDirOptional() { return Optional.ofNullable(gCamDataDir); }
    public Optional<String> getgCamOutputDatabaseOptional() { return Optional.ofNullable(gCamOutputDatabase); }
    public Optional<String> getOptionsFilenameOptional() { return Optional.ofNullable(optionsFilename); }
    public Optional<String> getXmlLibraryOptional() { return Optional.ofNullable(xmlLibrary); }
    public Optional<String> getTextEditorOptional() { return Optional.ofNullable(textEditor); }
    public Optional<String> getXmlEditorOptional() { return Optional.ofNullable(xmlEditor); }
    public Optional<String> getDescriptionTextOptional() { return Optional.ofNullable(descriptionText); }
    public Optional<String> getStopPeriodOptional() { return Optional.ofNullable(stopPeriod); }
    public Optional<String> getStopYearOptional() { return Optional.ofNullable(stopYear); }
    public Optional<String> getConfigurationTemplateFilenameOptional() { return Optional.ofNullable(configurationTemplateFilename); }
    public Optional<String> getQueryFilenameOptional() { return Optional.ofNullable(queryFilename); }
    public Optional<String> getFavoriteQueryFilenameOptional() { return Optional.ofNullable(favoriteQueryFilename); }
    public Optional<String> getTchBndListFilenameOptional() { return Optional.ofNullable(tchBndListFilename); }
    public Optional<String> getTrnVehInfoFilenameOptional() { return Optional.ofNullable(trnVehInfoFilename); }
    public Optional<String> getCsvColumnFilenameOptional() { return Optional.ofNullable(csvColumnFilename); }
    public Optional<String> getXmlHeaderFilenameOptional() { return Optional.ofNullable(xmlHeaderFilename); }
    public Optional<String> getUnitConversionsFilenameOptional() { return Optional.ofNullable(unitConversionsFilename); }
    public Optional<String> getMonetaryConversionsFilenameOptional() { return Optional.ofNullable(monetaryConversionsFilename); }

	public int getScenarioBuilderWidth() {
		return scenarioBuilderWidth;
	}

	public void setScenarioBuilderWidth(int scenarioBuilderWidth) {
		this.scenarioBuilderWidth = scenarioBuilderWidth;
	}

	public int getScenarioBuilderHeight() {
		return scenarioBuilderHeight;
	}

	public void setScenarioBuilderHeight(int scenarioBuilderHeight) {
		this.scenarioBuilderHeight = scenarioBuilderHeight;
	}

	public List<Integer> getAllYears() {
		if (allYears==null) {
				allYears=new ArrayList<>(DEFAULT_ALL_YEARS_LIST);
		}
		return allYears;
	}

	public void setAllYears(List<Integer> allYears) {
		this.allYears = allYears;
	}
	
	public void setAllYears(String allYearsStr) {
		this.allYears = new ArrayList<>();
		try {
			String[] s=allYearsStr.split(",");
			for (int i=0;i<s.length;i++) {
				this.allYears.add(Integer.parseInt(s[i].trim()));
			}
		} catch(Exception e) {
			utils.warningMessage("Problem parsing allYears string: "+allYearsStr);
		}
	}

	public int getSimulationYearIncrement() {
		return simulationYearIncrement;
	}

	public void setSimulationYearIncrement(int simulationYearIncrement) {
		this.simulationYearIncrement = simulationYearIncrement;
	}

	public List<String> getRegionList() {
		if (regionList==null) {
			regionList=new ArrayList<>();
			List<String> list = files.getStringListFromFile(getRegionListFilename(), "#");
			for (int i=0;i<list.size();i++) {
				String[] s=list.get(i).trim().split(",");
				for (int j=0;j<s.length;j++) {
					String s2=s[j].trim();
					if (s2.contains(":")) {
						String[] s3=s2.split(":");
						s2=s3[1].trim();
					} 
					if (s2.length()>0) regionList.add(s2);
				}
			}
		}
		return regionList;
	}

	public void setRegionList(List<String> regionList) {
		this.regionList = regionList;
	}

	public List<String> getSubRegionList() {
		if (subRegionList==null) {
			subRegionList=new ArrayList<>();
			List<String> list = files.getStringListFromFile(getSubRegionsFilename(), "#");
			for (int i=0;i<list.size();i++) {
				String[] s=list.get(i).trim().split(",");
				for (int j=0;j<s.length;j++) {
					String s2=s[j].trim();
					if (s2.contains(":")) {
						String[] s3=s2.split(":");
						s2=s3[1].trim();
					} 
					if (s2.length()>0) subRegionList.add(s2);
				}
			}
		}
		return subRegionList;
	}

	public void setSubRegionList(List<String> stateList) {
		this.subRegionList = stateList;
	}

    public List<String> getGhgList() {
        return ghgList;
    }
    
	public List<String> getFgasList() {
		return fGasList;
	}  

    public List<String> getGhgPriceAdjust() {
        return ghgPriceAdjust;
    }
 
    public List<String> getGhgDemandAdjust() {
        return ghgDemandAdjust;
    }
    
    public List<String> getGhgPriceUnit() {
        return ghgPriceUnit;
    }

    public List<String> getGhgOutputUnit() {
        return ghgOutputUnit;
    }
  
    public void setGhgListStr(String emisListStr) {
		this.ghgList = utils.getStringListFromString(emisListStr, ",");
	}
    
    public void setGhgList(List<String> emisList) {
        this.ghgList = emisList;
    }

    public void setFgasListStr(String emisListStr) {
		this.fGasList = utils.getStringListFromString(emisListStr, ",");
	}
    
    public void setFgasList(List<String> emisList) {
    	this.fGasList = emisList;
    }
    
    public void setGhgGWPListStr(String ghgGWPListStr) {
    	setGhgDemandAdjustStr(ghgGWPListStr);
    	String[] ghgGWPs=ghgGWPListStr.split(",");
    	//Double[] ghgGWPValues=new Double[ghgGWPs.length];
    	String priceAdjustStr="";
    	for (int i=0;i<ghgGWPs.length;i++) {
    		double priceAdjVal=1.0;
    		try {
				priceAdjVal=Double.parseDouble(ghgGWPs[i].trim())/3.667;
			} catch(Exception e) {
				priceAdjVal=1.0;
			}
			if (i>0) priceAdjustStr+=",";
			priceAdjustStr+=String.format("%.4f",priceAdjVal);
		}
    	setGhgPriceAdjustStr(priceAdjustStr);
    }
    
    public void setGhgUnitListStr(String ghgUnitListStr) {
		
    	this.setGhgOutputUnitStr(ghgUnitListStr);
    	String[] ghgPriceUnits=ghgUnitListStr.split(",");

    	String priceUnitStr="";
		for (int i=0;i<ghgPriceUnits.length;i++) {
			if (i>0) priceUnitStr+=",";
			priceUnitStr+="1990$s/"+ghgPriceUnits[i].trim();
		}
		this.setGhgPriceUnitStr(priceUnitStr);
    }
    
    public void setGhgPriceAdjustStr(String ghgPriceAdjustStr) {
    	this.ghgPriceAdjust = utils.getStringListFromString(ghgPriceAdjustStr, ",");
    }
    
    public void setGhgPriceAdjust(List<String> ghgPriceAdjust) {
        this.ghgPriceAdjust = ghgPriceAdjust;
    }
    
    public void setGhgDemandAdjustStr(String ghgDemandAdjustStr) {
		this.ghgDemandAdjust = utils.getStringListFromString(ghgDemandAdjustStr, ",");
	}

    public void setGhgDemandAdjust(List<String> ghgDemandAdjust) {
        this.ghgDemandAdjust = ghgDemandAdjust;
    }
	
    public void setGhgPriceUnitStr(String ghgPriceUnitStr) {
    	this.ghgPriceUnit = utils.getStringListFromString(ghgPriceUnitStr, ",");
    }
    
    public void setGhgPriceUnit(List<String> ghgPriceUnit) {
        this.ghgPriceUnit = ghgPriceUnit;
    }
    
    public void setGhgOutputUnitStr(String ghgOutputUnitStr) {
		this.ghgOutputUnit = utils.getStringListFromString(ghgOutputUnitStr, ",");
	}
    
    public void setGhgOutputUnit(List<String> ghgOutputUnit) {
        this.ghgOutputUnit = ghgOutputUnit;
    }
}


