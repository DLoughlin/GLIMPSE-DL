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
        boolean escaping = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaping) {
                // Keep escaped char literally.
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                // Escape next character (including quotes/space).
                escaping = true;
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
                // Skip repeated whitespace.
                continue;
            }

            current.append(c);
        }

        if (escaping) {
            // Trailing backslash: treat it as literal backslash.
            current.append('\\');
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}
