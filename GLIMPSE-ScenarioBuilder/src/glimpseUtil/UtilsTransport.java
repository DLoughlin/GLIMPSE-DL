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

	public UtilsTransport(GLIMPSEVariables vars, GLIMPSEFiles files, GLIMPSEUtils utils) {
		this.vars = vars;
		this.files = files;
		this.utils = utils;
	}

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

	public String getLoadFactor(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("load", region, sector, subsector, tech, year);
	}

	public String getVehCoefficient(String region, String sector, String subsector, String tech, String year) {
		return getTrnVehInfo("coefficient", region, sector, subsector, tech, year);
	}

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
			System.out.println("Problem finding " + param + " for " + sector + " / " + subsector);
		return val;
	}

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
