package gui;

import java.util.Locale;

final class ScenarioLibraryPromptHelper {

    static final String GCAM_DB_PROMPT_TOKEN_CLOSE_MI = "close the modelinterface";
    static final String GCAM_DB_PROMPT_TOKEN_CLOSE_IT = "please close it";
    static final String GCAM_DB_PROMPT_TOKEN_APPEARS_OPEN = "appears to be open";
    static final String GCAM_DB_PROMPT_TOKEN_PRESS_ENTER = "press enter";
    static final String GCAM_DB_PROMPT_TOKEN_HIT_ENTER = "hit enter";
    static final String GCAM_DB_PROMPT_TOKEN_PRESS_RETURN = "press return";
    static final String GCAM_DB_PROMPT_TOKEN_CONTINUE = "continue";
    static final String GCAM_DB_PROMPT_TOKEN_DATABASE = "database";
    static final String GCAM_DB_PROMPT_TOKEN_DB = " db";
    static final String GCAM_DB_PROMPT_EXACT_TRIGGER = "please close it and press return";

    static final String GCAM_WRITE_TOKEN_PRINTING_OUTPUT = "printing output";
    static final String GCAM_WRITE_TOKEN_XML_DATABASE = "starting output to xml database";
    static final String GCAM_WRITE_TOKEN_WRITE_TIME = "write time:";

    private ScenarioLibraryPromptHelper() {
    }

    static boolean looksLikeDatabaseSavePrompt(String line) {
        if (line == null) {
            return false;
        }
        String normalized = normalizeDatabasePromptText(line);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.contains(GCAM_DB_PROMPT_EXACT_TRIGGER)) {
            return true;
        }
        boolean mentionsDatabase = normalized.contains(GCAM_DB_PROMPT_TOKEN_DATABASE)
                || normalized.contains(GCAM_DB_PROMPT_TOKEN_DB);
        boolean mentionsBlockedState = normalized.contains(GCAM_DB_PROMPT_TOKEN_CLOSE_MI)
                || normalized.contains(GCAM_DB_PROMPT_TOKEN_CLOSE_IT)
                || normalized.contains(GCAM_DB_PROMPT_TOKEN_APPEARS_OPEN);
        boolean mentionsResumeInput = normalized.contains(GCAM_DB_PROMPT_TOKEN_PRESS_ENTER)
                || normalized.contains(GCAM_DB_PROMPT_TOKEN_HIT_ENTER)
                || normalized.contains(GCAM_DB_PROMPT_TOKEN_PRESS_RETURN);
        boolean mentionsContinue = normalized.contains(GCAM_DB_PROMPT_TOKEN_CONTINUE);
        return mentionsDatabase && mentionsBlockedState && (mentionsResumeInput || mentionsContinue);
    }

    static String normalizeDatabasePromptText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
    }

    static boolean containsWritingPhrase(String text) {
        if (text == null) {
            return false;
        }
        String normalized = normalizeDatabasePromptText(text);
        return normalized.contains(GCAM_WRITE_TOKEN_PRINTING_OUTPUT)
                || normalized.contains(GCAM_WRITE_TOKEN_XML_DATABASE)
                || normalized.contains(GCAM_WRITE_TOKEN_WRITE_TIME);
    }
}
