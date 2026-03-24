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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for scenario and system status lookups.
 */
public class UtilsStatus {

	private static final Pattern RUNNING_PERIOD_PATTERN = Pattern.compile(
			"(?:^|[^A-Za-z])(period|final-calibration period|model period|solving period|time period)\\s*[:=]?\\s*(\\d{1,3})(?:[^0-9]|$)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern RUNNING_PERIOD_WITH_YEAR_PATTERN = Pattern.compile(
			"(?:^|[^A-Za-z])(period|final-calibration period|model period|solving period|time period)\\s+(\\d{1,3})\\s*[:=]\\s*(\\d{4})(?:[^0-9]|$)",
			Pattern.CASE_INSENSITIVE);

	private GLIMPSEVariables vars;
	private GLIMPSEFiles files;

	public void init(GLIMPSEVariables v, GLIMPSEFiles f) {
		vars = v;
		files = f;
	}

	public String getRunningScenario(File mainLogFile) {
		if (files == null || mainLogFile == null || !mainLogFile.exists())
			return "";
		String configLine = files.searchForTextInFileS(mainLogFile, "Configuration file:", "#");
		if (configLine == null || configLine.trim().isEmpty())
			return "";
		String path = configLine.replace("Configuration file:", "").trim();
		if (path.isEmpty())
			return "";
		String name = new File(path).getParent();
		if (name == null)
			return "";
		String scenName = name.substring(name.lastIndexOf(File.separator) + 1);
		return scenName == null ? "" : scenName;
	}

	public String getScenarioStatusFromMainLog(File mainLogFile) {
		if (files == null || mainLogFile == null || !mainLogFile.exists())
			return "";
		String currentPeriod = getLatestRunningPeriod(mainLogFile);
		if (currentPeriod != null && !currentPeriod.isEmpty()) {
			return currentPeriod;
		}
		String unsolved = files.searchForTextInFileS(mainLogFile, "The following model periods did not solve:", "#");
		if (unsolved != null && !unsolved.trim().isEmpty()) {
			String msg = unsolved.replace("The following model periods did not solve:", "").trim();
			return "Unsolved,ERR " + msg;
		}
		String err = files.searchForTextInFileS(mainLogFile, "ERROR", "#");
		if (err != null && !err.trim().isEmpty()) {
			return "ERROR,ERR " + err.trim();
		}
		return "";
	}

	public String getLatestRunningPeriod(File mainLogFile) {
		if (files == null || mainLogFile == null || !mainLogFile.exists())
			return "";
		try {
			ArrayList<String> lines = files.getStringArrayFromFile(mainLogFile.getAbsolutePath(), "#");
			for (int i = lines.size() - 1; i >= 0; i--) {
				String line = lines.get(i);
				if (line == null) {
					continue;
				}
				String trimmed = line.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				Matcher yearMatcher = RUNNING_PERIOD_WITH_YEAR_PATTERN.matcher(trimmed);
				if (yearMatcher.find()) {
					String period = yearMatcher.group(2);
					String year = yearMatcher.group(3);
					String trimmedPeriod = period == null ? "" : period.trim();
					String trimmedYear = year == null ? "" : year.trim();
					if (!trimmedPeriod.isEmpty() && !trimmedYear.isEmpty()) {
						return trimmedPeriod + "," + trimmedYear;
					}
					if (!trimmedPeriod.isEmpty()) {
						return trimmedPeriod;
					}
					if (!trimmedYear.isEmpty()) {
						return trimmedYear;
					}
				}
				Matcher matcher = RUNNING_PERIOD_PATTERN.matcher(trimmed);
				if (matcher.find()) {
					String period = matcher.group(2);
					if (period != null && !period.trim().isEmpty()) {
						return period.trim();
					}
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	public String getComputerStatString() {
		boolean warning = false;
		String status = "";
		try {
			com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
					.getOperatingSystemMXBean();
			double gb = 1073741824d;

			float physicalMemorySize = (float) (os.getTotalPhysicalMemorySize() / gb);
			float physicalMemoryFree = (float) (os.getFreePhysicalMemorySize() / gb);
			float swapSpaceSize = (float) (os.getTotalSwapSpaceSize() / gb);
			float freeSwapSpace = (float) (os.getFreeSwapSpaceSize() / gb);
			File drive = new File("/");
			float totalSpace = (float) (drive.getTotalSpace() / gb);
			float freeSpace = (float) (drive.getFreeSpace() / gb);
			float cpuLoad = (float) os.getSystemCpuLoad();

			String databaseName = vars != null ? vars.getgCamOutputDatabase() : null;
			String databaseShortName = "";
			float databaseSize = 0f;
			if (databaseName != null && !databaseName.trim().isEmpty()) {
				File databaseFolder = new File(databaseName);
				databaseShortName = databaseFolder.getName();
				if (databaseFolder.exists() && files != null) {
					Path databasePath = databaseFolder.toPath();
					databaseSize = (float) (files.getDirectorySize(databasePath) / gb);
				}
			}

			String warningRAM = "";
			String warningDisk = "";
			//String warningSwap = "";
			String warningDb = "";
			float maxDbSize = vars != null ? vars.getMaxDatabaseSize() : 0f;

			if (physicalMemorySize > 0f && physicalMemoryFree / physicalMemorySize < 0.05f)
				warningRAM = "*";
			//if (swapSpaceSize > 0f && freeSwapSpace / swapSpaceSize < 0.05f)
			//	warningSwap = "*";
			if (freeSpace < 40.0f)
				warningDisk = "*";
			if (maxDbSize > 0f && databaseSize > maxDbSize * .8f)
				warningDb = "*";

			if ((physicalMemorySize > 0f && physicalMemoryFree / physicalMemorySize < 0.05f)
					|| (freeSpace < 40.0f)
					|| (maxDbSize > 0f && databaseSize > maxDbSize * .8f)) {
				warning = true;
			}

			String dbFreePct = (maxDbSize > 0f) ? String.format("%,.0f", (1.0f - (databaseSize / maxDbSize)) * 100.0f) : "n/a";
			status = GLIMPSEUtils.LABEL_RESOURCES
					+ "CPU: " + String.format("%,.0f", cpuLoad * 100.0f) + "% | "
					+ "RAM: " + String.format("%,.0f", physicalMemorySize - physicalMemoryFree) + "/"
					+ String.format("%,.0f", physicalMemorySize) + " GB used ("
					+ String.format("%,.0f", physicalMemorySize > 0f ? physicalMemoryFree / physicalMemorySize * 100.0f : 0.0f)
					+ "% Free)" + warningRAM + " | "
					+ "HD: " + String.format("%,.0f", totalSpace - freeSpace) + "/" + String.format("%,.0f", totalSpace)
					+ " GB used (" + String.format("%,.0f", totalSpace > 0f ? freeSpace / totalSpace * 100.0f : 0.0f)
					+ "% Free)" + warningDisk + " | "
					//+ "Swap: " + String.format("%,.0f", swapSpaceSize - freeSwapSpace) + "/"
					//+ String.format("%,.0f", swapSpaceSize) + " GB ("
					//+ String.format("%,.0f", swapSpaceSize > 0f ? freeSwapSpace / swapSpaceSize * 100.0f : 0.0f)
					//+ "% Free)" + warningSwap + " | " 
					+ "DB: " + (databaseShortName.isEmpty() ? "(not set)" : databaseShortName) + " "
					+ String.format("%,.1f", databaseSize) + "/" + (vars != null ? maxDbSize : 0f)
					+ " GB used (" + dbFreePct + "%" + warningDb + " Free)";
		} catch (Exception e) {
			status = "";
		}
		if (warning)
			status = status.trim() + " !!!";
		return status;
	}

	public File getExecutableMainLogFile() {
		if (vars == null)
			return null;
		String exeDir = vars.getgCamExecutableDir();
		if (exeDir == null || exeDir.trim().isEmpty())
			return null;
		return new File(exeDir + File.separator + "logs" + File.separator + "main_log.txt");
	}

	public File getRunningScenarioMainLogTarget(File mainLogFile) {
		if (vars == null)
			return null;
		String scenarioName = getRunningScenario(mainLogFile);
		if (scenarioName == null || scenarioName.trim().isEmpty())
			return null;
		return new File(vars.getScenarioDir() + File.separator + scenarioName + File.separator + "main_log.txt");
	}
}