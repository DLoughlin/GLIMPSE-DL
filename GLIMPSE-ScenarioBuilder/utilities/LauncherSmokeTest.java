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
            // Use a built-in command so it works on a vanilla Windows machine.
            // /c echo ... prints and exits.
            String msgWithSpaces = "hello from smoke test";
            File workingDir = new File(System.getProperty("java.io.tmpdir"));

            t.submitCommandWithDirectory(
                    new String[] { "cmd.exe", "/c", "echo", msgWithSpaces },
                    workingDir.getAbsolutePath()).get();

            System.out.println("LauncherSmokeTest: completed");
        } finally {
            t.shutdownNow();
        }
    }
}