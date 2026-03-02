/*
 * Minimal smoke test for the refactored command launcher.
 *
 * This is intentionally dependency-free (no JavaFX) so it can be compiled/run from a terminal.
 * It verifies:
 *  - args-based process launching via ExecutionThread.submitCommand(String[])
 *  - merged stdout/stderr output (java -version writes to stderr on many JDKs)
 *
 * Note: This doesn't launch ModelInterface itself.
 */
package utilities;

import java.io.File;

import gui.ExecutionThread;

public class LauncherSmokeTest {
    public static void main(String[] args) throws Exception {
        ExecutionThread t = new ExecutionThread();
        t.startUpExecutorMulti();
        try {
            // 1) Validate stderr->stdout merge: java -version typically writes to stderr.
            t.submitCommandNoStatusChecker(new String[] { "java", "-version" }).get();

            // 2) Validate working directory + args containing spaces.
            // Use a built-in shell command appropriate for the current OS.
            String msgWithSpaces = "hello from smoke test";
            File workingDir = new File(System.getProperty("java.io.tmpdir"));
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
            String[] echoCmd = isWindows
                    ? new String[] { "cmd.exe", "/c", "echo", msgWithSpaces }
                    : new String[] { "sh", "-c", "echo \"$1\"", "--", msgWithSpaces };

            t.submitCommandWithDirectory(echoCmd, workingDir.getAbsolutePath()).get();

            System.out.println("LauncherSmokeTest: completed");
        } finally {
            t.shutdownNow();
        }
    }
}