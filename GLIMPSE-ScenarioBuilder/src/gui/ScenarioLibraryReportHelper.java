package gui;

import glimpseUtil.GLIMPSEFiles;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

final class ScenarioLibraryReportHelper {

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
}
