import gui.ScenarioLibraryReportHelper;
import gui.ScenarioLibraryReportHelper.ErrorTextReport;
import glimpseUtil.GLIMPSEUtils;
import java.util.ArrayList;

public class ScenarioLibraryReportHelperSmoke {
    static class FakeUtils extends GLIMPSEUtils {
        @Override
        public ArrayList<String> generateErrorReport(String mainLogFile, String scenario) {
            ArrayList<String> rows = new ArrayList<>();
            rows.add(csv(scenario, "Period", "0", "1975", "", ""));
            rows.add(csv(scenario, "Period", "1", "1990", "", ""));
            rows.add(csv(scenario, "Period", "2", "2005", "", ""));
            rows.add(csv(scenario, "Period", "3", "2010", "", ""));
            rows.add(csv(scenario, "Period", "4", "2015", "", ""));
            rows.add(csv(scenario, "Period", "5", "2021", "", ""));
            rows.add(csv(scenario, "Period", "6", "2025", "", ""));
            rows.add(csv(scenario, "Period", "7", "2030", "", ""));
            rows.add(csv(scenario, "ERROR", "Model did not solve period 7 within set iteration 10001", "", "", ""));
            rows.add(csv(scenario, "ERROR", "Currently Unsolved Markets", "", "", ""));
            rows.add(csv(scenario, "ERROR", "Unsolved Part 1", "Solvable Markets", "", ""));
            rows.add(csv(scenario, "ERROR", "X", "XL", "XR", "ED", "EDL", "EDR", "RED", "brk", "Supply", "Demand", "Mrk Type", "Market", "", ""));
            rows.add(csv(scenario, "ERROR", "Unsolved Part 2", "Unsolvable Markets Not Cleared", "", ""));
            rows.add(csv(scenario, "ERROR", "X", "XL", "XR", "ED", "EDL", "EDR", "RED", "brk", "Supply", "Demand", "Mrk Type", "Market", "", ""));
            rows.add(csv(scenario, "ERROR", "0", "0", "0", "0.0200568", "0.0200568", "0.0200568", "1", "0", "0", "0.0200568", "Normal", "Chinaresid heating coal_d1", "MAJOR", "false"));
            rows.add(csv(scenario, "ERROR", "0", "0", "0", "0.0200568", "0.0200568", "0.0200568", ".02", "0", "0", "0.0200568", "Normal", "Chinaresid heating coal_d3", "MODERATE", "false"));
            rows.add(csv(scenario, "Summary", "Total errors=100; Major errors=99; Moderate errors=1; Small market errors=0; Verdict: Fail? (major errors) (1-5% thresholds)", "", "", ""));
            return rows;
        }

        private String csv(String scenario, String... values) {
            StringBuilder builder = new StringBuilder(scenario);
            for (String value : values) {
                builder.append(',').append(value == null ? "" : value);
            }
            return builder.toString();
        }
    }

    public static void main(String[] args) {
        ErrorTextReport report = ScenarioLibraryReportHelper.createMainLogErrorTextReport(
                new FakeUtils(),
                "C:/fake/main_log.txt",
                "GLIMPSE-8.2-Ref");
        System.out.println(report.buildText("All lines"));
        System.out.println("--- MAJOR ONLY ---");
        System.out.println(report.buildText("Major errors"));
    }
}
