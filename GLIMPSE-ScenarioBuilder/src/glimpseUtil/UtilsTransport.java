package glimpseUtil;

import java.util.ArrayList;

/**
 * Transportation table loading, caching, and lookup logic extracted from {@link GLIMPSEUtils}.
 * <p>
 * This helper intentionally preserves the current lookup behavior and failure contracts so that
 * {@link GLIMPSEUtils} can remain a stable facade for existing callers.
 * </p>
 */
public final class UtilsTransport {

	private final GLIMPSEVariables vars;
	private final GLIMPSEFiles files;
	private final GLIMPSEUtils utils;

	private String[][] ldv2WTable;
	private String[][] ldv4WTable;
	private String[][] hdvTable;
	private String[][] otherTable;

	/**
	 * Creates a transport helper backed by shared GLIMPSE services.
	 *
	 * @param vars shared variables and configuration access
	 * @param files shared file utilities for loading transport data
	 * @param utils shared general-purpose utility facade
	 */
	public UtilsTransport(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
		this.vars = vars;
		this.files = files;
		this.utils = utils;
	}

	/**
	 * Tests whether a transportation subsector is defined for a region within the
	 * loaded transport vehicle table.
	 *
	 * @param region region name to match
	 * @param sector transport sector whose table should be searched
	 * @param subsector subsector name to locate
	 * @return {@code true} when the subsector exists in the selected region
	 */
	public boolean isSubsectorInRegion(String region, String sector, String subsector) {
		boolean b = false;
		String[][] data = getTrnDataForProcessing(sector);
		if (data == null || data.length == 0)
			return false;

		int matchRow = -1;
		boolean oldFormat = isOldFormatTrnVehInfo(data);

		int paramCol = 0;
		if (oldFormat)
			paramCol = -1;
		int regionCol = paramCol + 1;
		int subsectorCol = regionCol + 2;

		for (int j = 0; j < data.length; j++) {
			if (data[j] == null || data[j].length <= subsectorCol)
				continue;
			String dataRegion = data[j][regionCol];
			String dataSubsector = data[j][subsectorCol];
			if ((region.equals(dataRegion)) && (subsector.equals(dataSubsector))) {
				matchRow = j;
				break;
			}
		}

		if (matchRow > -1)
			b = true;
		return b;
	}

	/**
	 * Returns the load-factor value for a transportation technology record.
	 *
	 * @param region region name
	 * @param sector sector name
	 * @param subsector subsector name
	 * @param tech technology name
	 * @param year year column to read
	 * @return matching load-factor value, or {@code null} when unavailable
	 */
	public String getLoadFactor(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("load", region, sector, subsector, tech, year);
	}

	/**
	 * Returns the vehicle coefficient for a transportation technology record.
	 *
	 * @param region region name
	 * @param sector sector name
	 * @param subsector subsector name
	 * @param tech technology name
	 * @param year year column to read
	 * @return matching coefficient value, or {@code null} when unavailable
	 */
	public String getVehCoefficient(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("coefficient", region, sector, subsector, tech, year);
	}

	/**
	 * Lists the transportation technologies defined for a subsector.
	 *
	 * @param region region name
	 * @param sector sector name
	 * @param subsector subsector name
	 * @return technology names for the subsector, or {@code null} when unsupported
	 */
	public String[] getTrnTechsInSubsector(String region, String sector, String subsector) {
		if (region == null || sector == null || subsector == null)
			return null;

		region = region.toLowerCase();
		sector = sector.toLowerCase();
		subsector = subsector.toLowerCase();

		String[][] data = getTrnDataForProcessing(sector);
		if (data == null || data.length == 0)
			return null;

		boolean oldFormat = isOldFormatTrnVehInfo(data);
		if (oldFormat) {
			System.out.println("TrnVehInfoData file is not in correct format to support CAFE.");
			return null;
		}

		int paramCol = 0;
		int regionCol = paramCol + 1;
		int sectorCol = regionCol + 1;
		int subsectorCol = sectorCol + 1;
		int techCol = subsectorCol + 1;

		ArrayList<String> list = new ArrayList<String>();
		for (int j = 1; j < data.length; j++) {
			if (data[j] == null || data[j].length <= techCol)
				continue;
			if (data[j][regionCol].toLowerCase().equals(region)) {
				if (data[j][sectorCol].toLowerCase().equals(sector)) {
					if (data[j][subsectorCol].toLowerCase().equals(subsector)) {
						if ((data[j][paramCol]).toLowerCase().trim().startsWith("load")) {
							list = utils.addToArrayListIfUnique(list, data[j][techCol]);
						}
					}
				}
			}
		}

		return utils.createStringArrayFromArrayList(list);
	}

	/**
	 * Looks up a transportation input-table value for the requested parameter and
	 * year.
	 *
	 * @param param parameter name such as {@code load} or {@code coefficient}
	 * @param region region name
	 * @param sector sector name
	 * @param subsector subsector name
	 * @param tech technology name
	 * @param yearStr target year header
	 * @return table value, or {@code null} when no matching record is found
	 */
	public String getTrnVehInfo(String param, String region, String sector, String subsector, String tech, String yearStr) {
		String val = null;
		String[][] data = getTrnDataForProcessing(sector);
		if (data == null || data.length == 0 || data[0] == null)
			return null;

		param = param == null ? "" : param.toLowerCase();

		try {
			boolean oldFormat = isOldFormatTrnVehInfo(data);

			int paramCol = 0;
			if (oldFormat)
				paramCol = -1;
			int regionCol = paramCol + 1;
			int subsectorCol = regionCol + 2;
			int techCol = subsectorCol + 1;

			int yearCol = -1;
			for (int i = 0; i < data[0].length; i++) {
				String cmpStr = data[0][i].trim();
				if (yearStr.equals(cmpStr)) {
					yearCol = i;
					break;
				}
			}

			int matchRow = -1;
			if (yearCol > -1) {
				for (int j = 1; j < data.length; j++) {
					if (data[j] == null || data[j].length <= Math.max(techCol, yearCol))
						continue;
					String temp = data[j][0];
					if (((oldFormat) && ("load".equals(param))) || (temp.toLowerCase().trim().startsWith(param))) {
						String dataRegion = data[j][regionCol].trim();
						String dataSubsector = data[j][subsectorCol].trim();
						String dataTech = data[j][techCol].trim();
						if ((region.equals(dataRegion)) && (subsector.equals(dataSubsector))) {
							if (("load".equals(param)) || ((!"load".equals(param)) && (tech.equals(dataTech)))) {
								matchRow = j;
								break;
							}
						}
					}
				}

				if (matchRow > -1) {
					val = data[matchRow][yearCol];
				}
			}
		} catch (Exception e) {
			System.out.println("Error reading transportation input file. Please check format. Exception: " + e);
			val = null;
		}
		if (val == null)
			System.out.println("Problem finding " + param + " for " + sector + " / " + subsector + " / " + tech + " in " + region + " for year " + yearStr);
		return val;
	}

	/**
	 * Looks up a transportation input-table value for the requested parameter and
	 * year.
	 *
	 * @param param parameter name such as {@code load} or {@code coefficient}
	 * @param region region name
	 * @param sector sector name
	 * @param subsector subsector name
	 * @param tech technology name
	 * @param yearStr target year header
	 * @param units ... if can, converts result to this unit   
	 * @return table value, or {@code null} when no matching record is found
	 */
	public String getTrnVehInfo(String param, String region, String sector, String subsector, String tech, String yearStr, String reqdUnits) {
		String val = null;
		String[][] data = getTrnDataForProcessing(sector);
		if (data == null || data.length == 0 || data[0] == null)
			return null;

		param = param == null ? "" : param.toLowerCase();

		try {
			boolean oldFormat = isOldFormatTrnVehInfo(data);

			int paramCol = 0;
			if (oldFormat)
				paramCol = -1;
			int regionCol = paramCol + 1;
			int subsectorCol = regionCol + 2;
			int techCol = subsectorCol + 1;

			int yearCol = -1;
			for (int i = 0; i < data[0].length; i++) {
				String cmpStr = data[0][i].trim();
				if (yearStr.equals(cmpStr)) {
					yearCol = i;
					break;
				}
			}

			int matchRow = -1;
			if (yearCol > -1) {
				for (int j = 1; j < data.length; j++) {
					if (data[j] == null || data[j].length <= Math.max(techCol, yearCol))
						continue;
					String temp = data[j][0];
					if (((oldFormat) && ("load".equals(param))) || (temp.toLowerCase().trim().startsWith(param))) {
						String dataRegion = data[j][regionCol].trim();
						String dataSubsector = data[j][subsectorCol].trim();
						String dataTech = data[j][techCol].trim();
						if ((region.equals(dataRegion)) && (subsector.equals(dataSubsector))) {
							if (("load".equals(param)) || ((!"load".equals(param)) && (tech.equals(dataTech)))) {
								matchRow = j;
								break;
							}
						}
					}
				}

				if (matchRow > -1) {
					val = data[matchRow][yearCol];
					double valf = Double.parseDouble(val);
					String units = data[matchRow][data[matchRow].length - 1].trim();
					Double convertedVal = convertTransportUnits(valf, units, reqdUnits);
					if (convertedVal != null) {
						val = "" + convertedVal;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error reading transportation input file. Please check format. Exception: " + e);
			val = null;
		}
		if (val == null)
			System.out.println("Problem finding " + param + " for " + sector + " / " + subsector + " / " + tech + " in " + region + " for year " + yearStr);
		return val;
	}

	/**
	 * Converts transportation values between metric-style units such as
	 * {@code kJ/service-km}, {@code GJ/million-service-km}, and
	 * {@code EJ/billion-service-km}.
	 * <p>
	 * The method also supports common variants (for example, {@code bln} and
	 * {@code billion}), and denominator labels such as {@code service-km} and
	 * {@code veh-km}. Conversion is based on energy magnitude and denominator scale.
	 * </p>
	 *
	 * @param value value in {@code fromUnits}
	 * @param fromUnits source units string
	 * @param toUnits target units string
	 * @return converted value in {@code toUnits}, or {@code null} when conversion is
	 *         not possible
	 */
	public Double convertTransportUnits(double value, String fromUnits, String toUnits) {
		UnitScale from = parseUnitScale(fromUnits);
		UnitScale to = parseUnitScale(toUnits);

		if (from == null || to == null) {
			return null;
		}

		if (from.unitless && to.unitless) {
			return value;
		}
		if (from.unitless || to.unitless) {
			return null;
		}

		double baseValueJPerM = value * from.energyToJoule / from.denominatorToMeter;
		double convertedValue = baseValueJPerM * to.denominatorToMeter / to.energyToJoule;
		return convertedValue;
	}

	private UnitScale parseUnitScale(String units) {
		if (units == null) {
			return null;
		}

		String normalized = units.trim().toLowerCase().replace(" ", "");
		if (normalized.isEmpty()) {
			return null;
		}
		if ("unitless".equals(normalized)) {
			return UnitScale.unitless();
		}

		String[] split = normalized.split("/");
		if (split.length != 2) {
			return null;
		}

		double energyToJoule = getEnergyToJouleFactor(split[0]);
		double denominatorToMeter = getDenominatorToMeterFactor(split[1]);
		if (!Double.isFinite(energyToJoule) || !Double.isFinite(denominatorToMeter) || energyToJoule <= 0.0
				|| denominatorToMeter <= 0.0) {
			return null;
		}

		return new UnitScale(energyToJoule, denominatorToMeter, false);
	}

	private double getEnergyToJouleFactor(String token) {
		if (token == null) {
			return Double.NaN;
		}
		String t = token.trim().toLowerCase();
		switch (t) {
		case "j":
			return 1.0;
		case "kj":
			return 1.0e3;
		case "mj":
			return 1.0e6;
		case "gj":
			return 1.0e9;
		case "tj":
			return 1.0e12;
		case "pj":
			return 1.0e15;
		case "ej":
			return 1.0e18;
		default:
			return Double.NaN;
		}
	}

	private double getDenominatorToMeterFactor(String token) {
		if (token == null) {
			return Double.NaN;
		}
		String t = token.trim().toLowerCase();
		double scale = 1.0;

		if (t.startsWith("thousand-")) {
			scale = 1.0e3;
			t = t.substring("thousand-".length());
		} else if (t.startsWith("million-")) {
			scale = 1.0e6;
			t = t.substring("million-".length());
		} else if (t.startsWith("billion-")) {
			scale = 1.0e9;
			t = t.substring("billion-".length());
		} else if (t.startsWith("bln-")) {
			scale = 1.0e9;
			t = t.substring("bln-".length());
		} else if (t.startsWith("trillion-")) {
			scale = 1.0e12;
			t = t.substring("trillion-".length());
		}

		double distanceToMeter = getDistanceToMeterFactor(t);
		if (!Double.isFinite(distanceToMeter) || distanceToMeter <= 0.0) {
			return Double.NaN;
		}

		return scale * distanceToMeter;
	}

	private double getDistanceToMeterFactor(String token) {
		if (token == null || token.isEmpty()) {
			return Double.NaN;
		}

		String[] pieces = token.split("-");
		String base = pieces[pieces.length - 1];

		if ("km".equals(base)) {
			return 1.0e3;
		}
		if ("m".equals(base)) {
			return 1.0;
		}
		return Double.NaN;
	}

	private static final class UnitScale {
		private final double energyToJoule;
		private final double denominatorToMeter;
		private final boolean unitless;

		private UnitScale(double energyToJoule, double denominatorToMeter, boolean unitless) {
			this.energyToJoule = energyToJoule;
			this.denominatorToMeter = denominatorToMeter;
			this.unitless = unitless;
		}

		private static UnitScale unitless() {
			return new UnitScale(1.0, 1.0, true);
		}
	}

	/**
	 * Loads and partitions transportation vehicle information into the cached
	 * sector-specific tables used by this helper.
	 */
	public void loadTrnVehInfo() {
		if (vars == null || files == null || utils == null)
			return;
		String filename = vars.getTrnVehInfoFilename();
		System.out.println("Loading transportation info from " + filename);

		try {
			ArrayList<String> contents = files.getStringArrayFromFile(filename, "#");
			if (contents == null || contents.isEmpty()) {
				ldv4WTable = new String[0][0];
				ldv2WTable = new String[0][0];
				hdvTable = new String[0][0];
				otherTable = new String[0][0];
				return;
			}

			ArrayList<String> ldv2w = new ArrayList<String>();
			ArrayList<String> ldv4w = new ArrayList<String>();
			ArrayList<String> hdv = new ArrayList<String>();
			ArrayList<String> other = new ArrayList<String>();
			ldv2w.add(contents.get(0));
			ldv4w.add(contents.get(0));
			hdv.add(contents.get(0));
			other.add(contents.get(0));

			for (int i = 1; i < contents.size(); i++) {
				String str = contents.get(i);
				if (str == null)
					continue;
				if (str.indexOf("4W") >= 0) {
					ldv4w.add(str);
				} else if (str.indexOf("2W") >= 0) {
					ldv2w.add(str);
				} else if (str.indexOf("trn_freight_road") >= 0) {
					hdv.add(str);
				} else {
					other.add(str);
				}
			}

			ldv4WTable = utils.getDataMatrixFromArrayList(ldv4w);
			ldv2WTable = utils.getDataMatrixFromArrayList(ldv2w);
			hdvTable = utils.getDataMatrixFromArrayList(hdv);
			otherTable = utils.getDataMatrixFromArrayList(other);
		} catch (Exception e) {
			System.out.println("Problem reading transportation technology load data from " + filename + ": " + e);
		}
	}

	private String[][] getTrnDataForProcessing(String sector) {
		if (ldv4WTable == null)
			loadTrnVehInfo();

		if (sector == null)
			return otherTable;
		if (sector.indexOf("4W") >= 0) {
			return ldv4WTable;
		} else if (sector.indexOf("LDV") >= 0) {
			return ldv2WTable;
		} else if (sector.indexOf("freight_road") >= 0) {
			return hdvTable;
		}
		return otherTable;
	}

	private boolean isOldFormatTrnVehInfo(String[][] data) {
		if (data == null || data.length == 0 || data[0] == null || data[0].length == 0)
			return true;
		String firstHeader = data[0][0];
		if (firstHeader == null)
			return true;
		String header = firstHeader.trim().toLowerCase();
		return !(header.contains("param") || header.contains("variable"));
	}
}
