package glimpseUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple command-line tokenizer.
 *
 * Contract:
 * - Splits on whitespace outside of quotes.
 * - Supports double quotes (") and single quotes (') to group tokens.
 * - Supports backslash escaping (\\) for quotes and backslash itself.
 * - Keeps behavior close to a typical shell tokenizer, but it intentionally stays small and dependency-free.
 */
public final class CommandLineTokenizer {

    private CommandLineTokenizer() {}

    public static List<String> tokenize(String commandLine) {
        ArrayList<String> tokens = new ArrayList<>();
        if (commandLine == null) {
            return tokens;
        }

        String s = commandLine.trim();
        if (s.isEmpty()) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\\') {
                char next = (i + 1) < s.length() ? s.charAt(i + 1) : '\0';
                if (next == '\\' || next == '"' || next == '\'') {
                    current.append(next);
                    i++;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (!inDoubleQuotes && c == '\'') {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }
            if (!inSingleQuotes && c == '"') {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (!inSingleQuotes && !inDoubleQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}