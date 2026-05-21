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
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for scenario and system status lookups.
 */
public class UtilsStatus {
	private static final double BYTES_PER_GB = 1073741824d;
	private static final long DATABASE_SIZE_CACHE_MILLIS = 30000L;

	private static final Pattern RUNNING_PERIOD_PATTERN = Pattern.compile(
			"(?:^|[^A-Za-z])(period|final-calibration period|model period|solving period|time period)\\s*[:=]?\\s*(\\d{1,3})(?:[^0-9]|$)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern RUNNING_PERIOD_WITH_YEAR_PATTERN = Pattern.compile(
			"(?:^|[^A-Za-z])(period|final-calibration period|model period|solving period|time period)\\s+(\\d{1,3})\\s*[:=]\\s*(\\d{4})(?:[^0-9]|$)",
			Pattern.CASE_INSENSITIVE);

	private GLIMPSEVariables vars;
	private GLIMPSEFiles files;
	private volatile boolean nativeOsMetricsAvailable = true;
	private volatile boolean nativeOsMetricsWarningLogged = false;
	private volatile int nativeOsMetricsFailureCount = 0;
	private volatile boolean nativeDiskSpaceMetricsAvailable = true;
	private volatile boolean nativeDiskSpaceWarningLogged = false;
	private volatile int nativeDiskSpaceFailureCount = 0;
	private volatile String cachedDatabaseSizePath = "";
	private volatile long cachedDatabaseSizeTimestamp = 0L;
	private volatile float cachedDatabaseSizeGb = 0f;
	private volatile String databaseSizePendingPath = "";
	private volatile boolean databaseSizeComputeInProgress = false;
	private volatile Runnable onDatabaseSizeRefreshed = null;
	private volatile long databaseSizeRequestGeneration = 0L;

	/**
	 * Registers a callback to be invoked on a background thread whenever an async
	 * database-size calculation completes. Typically wired to
	 * {@code Client.refreshStatusBarComputerStats()} so the status bar re-renders
	 * with the real value instead of showing "calculating...".
	 *
	 * @param callback runnable to call when DB size refresh finishes; may be {@code null} to clear
	 */
	public void setOnDatabaseSizeRefreshed(Runnable callback) {
		onDatabaseSizeRefreshed = callback;
	}

	/**
	 * Initializes the status helper with shared variables and file utilities.
	 *
	 * @param v shared variables instance
	 * @param f shared file helper
	 */
	public void init(GLIMPSEVariables v, GLIMPSEFiles f) {
		vars = v;
		files = f;
	}

	/**
	 * Extracts the currently running scenario name from a GCAM main log.
	 *
	 * @param mainLogFile main log file to inspect
	 * @return scenario name, or an empty string when unavailable
	 */
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

	/**
	 * Returns a concise scenario status summary derived from a GCAM main log.
	 *
	 * @param mainLogFile main log file to inspect
	 * @return status text, or an empty string when no status can be determined
	 */
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

	/**
	 * Finds the most recent model period mentioned in a GCAM main log.
	 *
	 * @param mainLogFile main log file to inspect
	 * @return latest period or period/year pair, or an empty string when none is found
	 */
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

	/**
	 * Builds a compact resource-usage status string for the current machine and
	 * configured output database.
	 *
	 * @return computer resource status summary
	 */
	public String getComputerStatString() {
		boolean warning = false;
		String status = "";
		try {
			com.sun.management.OperatingSystemMXBean os = getOperatingSystemMxBeanOrNull();

			float physicalMemorySize = readOperatingSystemMetricGb(os, "getTotalPhysicalMemorySize", bean -> bean.getTotalPhysicalMemorySize());
			float physicalMemoryFree = readOperatingSystemMetricGb(os, "getFreePhysicalMemorySize", bean -> bean.getFreePhysicalMemorySize());
			File drive = new File("/");
			float totalSpace = readDiskSpaceGb(drive, true);
			float freeSpace = readDiskSpaceGb(drive, false);
			float cpuLoad = (float) readOperatingSystemMetric(os, "getSystemCpuLoad", bean -> bean.getSystemCpuLoad());

			String databaseName = vars != null ? vars.getgCamOutputDatabase() : null;
			String databaseShortName = "";
			float databaseSize = 0f;
			boolean databaseSizePending = false;
			if (databaseName != null && !databaseName.trim().isEmpty()) {
				File databaseFolder = new File(databaseName);
				databaseShortName = databaseFolder.getName();
				databaseSize = getCachedDatabaseSizeGb(databaseFolder);
				databaseSizePending = isDatabaseSizePending(databaseFolder);
			}

			String warningRAM = "";
			String warningDisk = "";
			String warningDb = "";
			float maxDbSize = vars != null ? vars.getMaxDatabaseSize() : 0f;

			if (Float.isFinite(physicalMemorySize) && physicalMemorySize > 0f
					&& Float.isFinite(physicalMemoryFree) && physicalMemoryFree / physicalMemorySize < 0.05f)
				warningRAM = "*";
			if (Float.isFinite(freeSpace) && freeSpace < 40.0f)
				warningDisk = "*";
			if (maxDbSize > 0f && databaseSize > maxDbSize * .8f)
				warningDb = "*";

			if ((Float.isFinite(physicalMemorySize) && physicalMemorySize > 0f
					&& Float.isFinite(physicalMemoryFree) && physicalMemoryFree / physicalMemorySize < 0.05f)
					|| (Float.isFinite(freeSpace) && freeSpace < 40.0f)
					|| (maxDbSize > 0f && databaseSize > maxDbSize * .8f)) {
				warning = true;
			}

			String dbFreePct = (maxDbSize > 0f) ? String.format("%,.0f", (1.0f - (databaseSize / maxDbSize)) * 100.0f) : "n/a";
			String dbUsageText = databaseSizePending
					? "calculating..."
					: String.format("%,.0f", databaseSize) + "/" + (vars != null ? String.format("%,.0f", maxDbSize) : 0f)
							+ " GB used (" + dbFreePct + "%" + warningDb + " Free)";
			status = GLIMPSEUtils.LABEL_RESOURCES
					+ "CPU: " + formatCpuLoad(cpuLoad) + " | "
					+ "RAM: " + formatRamUsage(physicalMemorySize, physicalMemoryFree, warningRAM) + " | "
					+ "HD: " + formatDiskUsage(totalSpace, freeSpace, warningDisk) + " | "
					+ "DB: " + (databaseShortName.isEmpty() ? "(not set)" : databaseShortName) + " "
					+ dbUsageText;
		} catch (Throwable e) {
			status = "";
		}
		if (warning)
			status = status.trim() + " !!!";
		return status;
	}

	private com.sun.management.OperatingSystemMXBean getOperatingSystemMxBeanOrNull() {
		if (!nativeOsMetricsAvailable) {
			return null;
		}
		try {
			java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
			if (bean instanceof com.sun.management.OperatingSystemMXBean) {
				return (com.sun.management.OperatingSystemMXBean) bean;
			}
			disableNativeOsMetrics("OperatingSystemMXBean cast", null);
		} catch (Throwable t) {
			disableNativeOsMetrics("ManagementFactory.getOperatingSystemMXBean", t);
		}
		return null;
	}

	private float readOperatingSystemMetricGb(com.sun.management.OperatingSystemMXBean os,
			String metricName, ToDoubleFunction<com.sun.management.OperatingSystemMXBean> extractor) {
		double value = readOperatingSystemMetric(os, metricName, extractor);
		return Double.isFinite(value) ? (float) (value / BYTES_PER_GB) : Float.NaN;
	}

	private double readOperatingSystemMetric(com.sun.management.OperatingSystemMXBean os,
			String metricName, ToDoubleFunction<com.sun.management.OperatingSystemMXBean> extractor) {
		if (os == null || !nativeOsMetricsAvailable) {
			return Double.NaN;
		}
		try {
			return extractor.applyAsDouble(os);
		} catch (Throwable t) {
			disableNativeOsMetrics(metricName, t);
			return Double.NaN;
		}
	}

	private void disableNativeOsMetrics(String metricName, Throwable t) {
		nativeOsMetricsFailureCount++;
		nativeOsMetricsAvailable = false;
		if (nativeOsMetricsWarningLogged) {
			return;
		}
		nativeOsMetricsWarningLogged = true;
		String where = (metricName == null || metricName.trim().isEmpty())
				? ""
				: " (failed on: " + metricName.trim() + ")";
		String suffix = (t == null || t.getMessage() == null || t.getMessage().trim().isEmpty())
				? ""
				: ": " + t.getMessage().trim();
		System.out.println("OS/JMX resource metrics unavailable; falling back to reduced status reporting"
				+ where + " [failure-count=" + nativeOsMetricsFailureCount
				+ "; additional failures suppressed]" + suffix);
	}

	private float getCachedDatabaseSizeGb(File databaseFolder) {
		if (databaseFolder == null || !databaseFolder.exists() || files == null) {
			return 0f;
		}
		String folderPath = databaseFolder.getAbsolutePath();
		return folderPath.equals(cachedDatabaseSizePath) ? cachedDatabaseSizeGb : 0f;
	}

	private boolean isDatabaseSizePending(File databaseFolder) {
		if (databaseFolder == null) {
			return false;
		}
		String folderPath = databaseFolder.getAbsolutePath();
		return databaseSizeComputeInProgress && folderPath.equals(databaseSizePendingPath)
				&& !folderPath.equals(cachedDatabaseSizePath);
	}

	/**
	 * Requests an asynchronous refresh of the currently configured output database
	 * size. Call this only on meaningful events (startup, database path changes,
	 * scenario status refreshes, ModelInterface close, etc.).
	 *
	 * @param force when {@code true}, bypass the normal recent-cache short circuit
	 */
	public void requestDatabaseSizeRefresh(boolean force) {
		if (vars == null || files == null) {
			return;
		}
		String databaseName = vars.getgCamOutputDatabase();
		if (databaseName == null || databaseName.trim().isEmpty()) {
			return;
		}
		final File databaseFolder = new File(databaseName.trim());
		final String folderPath = databaseFolder.getAbsolutePath();
		if (!databaseFolder.exists()) {
			// No folder yet; clear stale state and allow the status bar to stop showing
			// a previous database value.
			cachedDatabaseSizePath = folderPath;
			cachedDatabaseSizeGb = 0f;
			cachedDatabaseSizeTimestamp = System.currentTimeMillis();
			databaseSizePendingPath = folderPath;
			databaseSizeComputeInProgress = false;
			invokeDatabaseSizeRefreshCallback();
			return;
		}

		final long requestGeneration;
		final long now = System.currentTimeMillis();
		synchronized (this) {
			if (!force && folderPath.equals(cachedDatabaseSizePath)
					&& now - cachedDatabaseSizeTimestamp < DATABASE_SIZE_CACHE_MILLIS
					&& !databaseSizeComputeInProgress) {
				return;
			}
			if (databaseSizeComputeInProgress && folderPath.equals(databaseSizePendingPath)) {
				return;
			}
			databaseSizeComputeInProgress = true;
			databaseSizePendingPath = folderPath;
			databaseSizeRequestGeneration++;
			requestGeneration = databaseSizeRequestGeneration;
		}

		Thread t = new Thread(() -> {
			float sizeGb = 0f;
			boolean success = false;
			try {
				Path databasePath = databaseFolder.toPath();
				sizeGb = (float) (files.getDirectorySize(databasePath) / BYTES_PER_GB);
				success = true;
			} catch (Throwable ignored) {
			} finally {
				boolean shouldInvokeCallback = false;
				synchronized (UtilsStatus.this) {
					if (requestGeneration == databaseSizeRequestGeneration
							&& folderPath.equals(databaseSizePendingPath)) {
						cachedDatabaseSizeGb = success ? sizeGb : 0f;
						cachedDatabaseSizePath = folderPath;
						cachedDatabaseSizeTimestamp = System.currentTimeMillis();
						databaseSizeComputeInProgress = false;
						shouldInvokeCallback = true;
					}
				}
				if (shouldInvokeCallback) {
					invokeDatabaseSizeRefreshCallback();
				}
			}
		}, "glimpse-db-size-refresh");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Requests an asynchronous refresh of the currently configured output database size.
	 */
	public void requestDatabaseSizeRefresh() {
		requestDatabaseSizeRefresh(false);
	}

	private void invokeDatabaseSizeRefreshCallback() {
		final Runnable callback = onDatabaseSizeRefreshed;
		if (callback != null) {
			try {
				callback.run();
			} catch (Throwable ignored) {
			}
		}
	}

	private float readDiskSpaceGb(File drive, boolean total) {
		if (!nativeDiskSpaceMetricsAvailable || drive == null) {
			return Float.NaN;
		}
		try {
			long bytes = total ? drive.getTotalSpace() : drive.getFreeSpace();
			return (float) (bytes / BYTES_PER_GB);
		} catch (Throwable t) {
			disableNativeDiskSpaceMetrics(total ? "File.getTotalSpace" : "File.getFreeSpace", t);
			return Float.NaN;
		}
	}

	private void disableNativeDiskSpaceMetrics(String metricName, Throwable t) {
		nativeDiskSpaceFailureCount++;
		nativeDiskSpaceMetricsAvailable = false;
		if (nativeDiskSpaceWarningLogged) {
			return;
		}
		nativeDiskSpaceWarningLogged = true;
		String where = (metricName == null || metricName.trim().isEmpty())
				? ""
				: " (failed on: " + metricName.trim() + ")";
		String suffix = (t == null || t.getMessage() == null || t.getMessage().trim().isEmpty())
				? ""
				: ": " + t.getMessage().trim();
		System.out.println("Disk space metrics unavailable; falling back to reduced status reporting"
				+ where + " [failure-count=" + nativeDiskSpaceFailureCount
				+ "; additional failures suppressed]" + suffix);
	}

	private String formatCpuLoad(float cpuLoad) {
		if (!Float.isFinite(cpuLoad) || cpuLoad < 0f) {
			return "n/a";
		}
		return String.format("%,.0f", cpuLoad * 100.0f) + "%";
	}

	private String formatDiskUsage(float totalSpace, float freeSpace, String warningDisk) {
		if (!Float.isFinite(totalSpace) || totalSpace <= 0f || !Float.isFinite(freeSpace)) {
			return "n/a";
		}
		return String.format("%,.0f", totalSpace - freeSpace) + "/" + String.format("%,.0f", totalSpace)
				+ " GB used (" + String.format("%,.0f", freeSpace / totalSpace * 100.0f)
				+ "% Free)" + warningDisk;
	}

	private String formatRamUsage(float physicalMemorySize, float physicalMemoryFree, String warningRAM) {
		if (!Float.isFinite(physicalMemorySize) || physicalMemorySize <= 0f || !Float.isFinite(physicalMemoryFree)) {
			return "n/a";
		}
		return String.format("%,.0f", physicalMemorySize - physicalMemoryFree) + "/"
				+ String.format("%,.0f", physicalMemorySize) + " GB used ("
				+ String.format("%,.0f", physicalMemoryFree / physicalMemorySize * 100.0f)
				+ "% Free)" + warningRAM;
	}
	/**
	 * Returns the executable-side GCAM `main_log.txt` file path.
	 *
	 * @return executable main log file, or {@code null} when unavailable
	 */
	public File getExecutableMainLogFile() {
		if (vars == null)
			return null;
		String exeDir = vars.getgCamExecutableDir();
		if (exeDir == null || exeDir.trim().isEmpty())
			return null;
		return new File(exeDir + File.separator + "logs" + File.separator + "main_log.txt");
	}

	/**
	 * Returns the scenario-side target path where the executable main log should be
	 * copied or moved.
	 *
	 * @param mainLogFile executable main log file used to determine the scenario name
	 * @return target main log file in the active scenario folder, or {@code null}
	 */
	public File getRunningScenarioMainLogTarget(File mainLogFile) {
		if (vars == null)
			return null;
		String scenarioName = getRunningScenario(mainLogFile);
		if (scenarioName == null || scenarioName.trim().isEmpty())
			return null;
		return new File(vars.getScenarioDir() + File.separator + scenarioName + File.separator + "main_log.txt");
	}
}
