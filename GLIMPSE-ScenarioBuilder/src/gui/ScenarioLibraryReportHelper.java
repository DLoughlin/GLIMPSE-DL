package gui;

import glimpseUtil.GLIMPSEFiles;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import glimpseElement.ScenarioRow;

final class ScenarioLibraryReportHelper {

    private static final String RUNTIME_PREFIX = "Data Readin, Model Run & Write Time:";

    private ScenarioLibraryReportHelper() {
    }

    static ArrayList<String> createSimpleQueueReport(List<String> runsQueuedList, List<String> runsCompletedList) {
        ArrayList<String> rtnArray = new ArrayList<>();
        rtnArray.add("Note: Includes only runs added to the queue since the start of this session.");
        if (runsQueuedList != null && !runsQueuedList.isEmpty()) {
            rtnArray.add("---");
            rtnArray.add("In queue:");
            rtnArray.addAll(runsQueuedList);
        }
        if (runsCompletedList != null && !runsCompletedList.isEmpty()) {
            rtnArray.add("---");
            rtnArray.add("Completed:");
            rtnArray.addAll(runsCompletedList);
        }
        return rtnArray;
    }

    static String getComponentsFromConfig(File file) {
        String rtnStr = "";
        try (Scanner fileScanner = new Scanner(file)) {
            boolean startRecording = false;
            boolean stopRecording = false;
            boolean hasMetaData = false;
            int count = 0;
            while (fileScanner.hasNext() && !stopRecording) {
                String line = fileScanner.nextLine().trim();
                if (line.equals("##################### Scenario Meta Data #####################")) {
                    hasMetaData = true;
                }
                if (line.equals("###############################################################")) {
                    stopRecording = true;
                }
                if (startRecording && (line.length() > 0) && !stopRecording) {
                    if (count == 0) {
                        count++;
                        rtnStr += line;
                    } else {
                        rtnStr += " ; " + line;
                    }
                }
                if (line.equals("Components:")) {
                    startRecording = true;
                }
                if (line.equals("<Files>")) {
                    stopRecording = true;
                }
            }
            if (!hasMetaData) {
                rtnStr = "Externally-created scenario";
            }
        } catch (Exception e) {
            System.out.println("Problem reading components from " + file.getName() + ": " + e);
        }
        return rtnStr;
    }

    static int countLogMatches(GLIMPSEFiles files, File mainLog, String token) {
        if (files == null || mainLog == null || token == null || token.trim().isEmpty() || !mainLog.exists()) {
            return 0;
        }
        int count = 0;
        try {
            ArrayList<String> lines = files.getStringArrayFromFile(mainLog.getAbsolutePath(), "#");
            for (String line : lines) {
                if (line != null && line.contains(token)) {
                    count++;
                }
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    static String extractModelVersion(GLIMPSEFiles files, File mainLog) {
        if (files == null || mainLog == null) {
            return "";
        }
        String version = files.searchForTextInFileS(mainLog, "version", "#");
        return version == null ? "" : version.trim().replace(',', ';');
    }

    static String extractUnsolvedMarkets(GLIMPSEFiles files, File mainLog) {
        if (files == null || mainLog == null) {
            return "";
        }
        String unsolved = files.searchForTextInFileS(mainLog, "The following model periods did not solve:", "#");
        if (unsolved == null) {
            return "";
        }
        return unsolved.replace("The following model periods did not solve:", "").trim().replace(',', ';');
    }

    static String extractLastLogValue(GLIMPSEFiles files, File mainLog, String prefix) {
        if (files == null || mainLog == null || prefix == null || prefix.trim().isEmpty() || !mainLog.exists()) {
            return "";
        }
        try {
            ArrayList<String> lines = files.getStringArrayFromFile(mainLog.getAbsolutePath(), "#");
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line == null) {
                    continue;
                }
                if (line.contains(prefix)) {
                    return line.substring(line.indexOf(prefix) + prefix.length()).trim().replace(',', ';');
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    static ArrayList<String> createScenarioExecutionReport(GLIMPSEFiles files, String scenarioDir,
            List<ScenarioRow> scenarioRows, List<String> selectedScenarioNames, List<String> queuedRuns,
            List<String> completedRuns, String stopRequestedScenarioName) {
        ArrayList<String> reportLines = new ArrayList<>();
        reportLines.add("Scenario Execution Report");
        reportLines.add("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        reportLines.add("Queued this session: " + safeSize(queuedRuns));
        reportLines.add("Completed this session: " + safeSize(completedRuns));
        reportLines.add("---");

        boolean filterToSelection = selectedScenarioNames != null && !selectedScenarioNames.isEmpty();
        if (scenarioRows != null) {
            for (ScenarioRow row : scenarioRows) {
                if (row == null) {
                    continue;
                }
                String scenarioName = safeText(row.getScenarioName());
                if (scenarioName.isEmpty()) {
                    continue;
                }
                if (filterToSelection && !selectedScenarioNames.contains(scenarioName)) {
                    continue;
                }
                reportLines.add("Scenario: " + scenarioName);
                reportLines.add("  Status: " + safeText(row.getStatus()));
                reportLines.add("  Components: " + safeText(row.getComponents()));
                reportLines.add("  Created: " + safeText(row.getCreatedDate()));
                reportLines.add("  Completed: " + safeText(row.getCompletedDate()));
                reportLines.add("  Runtime: " + safeText(row.getRuntime()));
                reportLines.add("  Unsolved markets: " + safeText(row.getUnsolvedMarkets()));
                reportLines.add("  Stop requested: " + (scenarioName.equals(safeText(stopRequestedScenarioName)) ? "yes" : "no"));

                File mainLog = ScenarioLibraryPathHelper.scenarioMainLogPath(scenarioDir, scenarioName).toFile();
                if (mainLog.exists()) {
                    reportLines.add("  Main log: " + mainLog.getAbsolutePath());
                    String version = extractModelVersion(files, mainLog);
                    if (!version.isEmpty()) {
                        reportLines.add("  Version: " + version);
                    }
                    String unsolved = extractUnsolvedMarkets(files, mainLog);
                    if (!unsolved.isEmpty()) {
                        reportLines.add("  Log unsolved: " + unsolved);
                    }
                    String runtimeLine = extractLastLogValue(files, mainLog, RUNTIME_PREFIX);
                    if (!runtimeLine.isEmpty()) {
                        reportLines.add("  Runtime raw: " + runtimeLine);
                    }
                }
                reportLines.add("---");
            }
        }
        return reportLines;
    }

    static ArrayList<String> createExecutableErrorReport(GLIMPSEFiles files, File mainLog) {
        ArrayList<String> reportLines = new ArrayList<>();
        reportLines.add("Executable Errors");
        reportLines.add("Log: " + (mainLog == null ? "" : mainLog.getAbsolutePath()));
        reportLines.add("---");
        appendMatchingLogLines(files, reportLines, mainLog, "ERROR");
        appendMatchingLogLines(files, reportLines, mainLog, "Exception");
        appendMatchingLogLines(files, reportLines, mainLog, ",ERR");
        return reportLines;
    }

    static ArrayList<String> createScenarioErrorReport(GLIMPSEFiles files, String scenarioDir, List<ScenarioRow> rows) {
        ArrayList<String> reportLines = new ArrayList<>();
        reportLines.add("Scenario Errors");
        reportLines.add("---");
        if (rows != null) {
            for (ScenarioRow row : rows) {
                if (row == null) {
                    continue;
                }
                String scenarioName = safeText(row.getScenarioName());
                if (scenarioName.isEmpty()) {
                    continue;
                }
                File mainLog = ScenarioLibraryPathHelper.scenarioMainLogPath(scenarioDir, scenarioName).toFile();
                reportLines.add("Scenario: " + scenarioName);
                reportLines.add("Log: " + mainLog.getAbsolutePath());
                appendMatchingLogLines(files, reportLines, mainLog, "ERROR");
                appendMatchingLogLines(files, reportLines, mainLog, "Exception");
                appendMatchingLogLines(files, reportLines, mainLog, ",ERR");
                reportLines.add("---");
            }
        }
        return reportLines;
    }

    private static void appendMatchingLogLines(GLIMPSEFiles files, List<String> linesOut, File logFile, String token) {
        if (files == null || linesOut == null || logFile == null || token == null || token.trim().isEmpty() || !logFile.exists()) {
            return;
        }
        try {
            ArrayList<String> lines = files.getStringArrayFromFile(logFile.getAbsolutePath(), "#");
            boolean addedHeader = false;
            for (String line : lines) {
                if (line == null || !line.contains(token)) {
                    continue;
                }
                if (!addedHeader) {
                    linesOut.add("Matches for '" + token + "':");
                    addedHeader = true;
                }
                linesOut.add("  " + line.trim());
            }
        } catch (Exception ignored) {
        }
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String safeText(String text) {
        return text == null || text.trim().isEmpty() ? "" : text.trim();
    }
}