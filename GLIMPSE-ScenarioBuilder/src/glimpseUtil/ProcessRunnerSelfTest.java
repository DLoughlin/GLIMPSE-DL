package glimpseUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Tiny self-test to validate ProcessRunner captures stdout/stderr and exit code.
 *
 * Run from a Windows cmd shell:
 *   java -cp <your classpath> glimpseUtil.ProcessRunnerSelfTest
 */
public class ProcessRunnerSelfTest {

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException(msg);
        }
    }

    private static void testTokenizer() {
        // Basic quoted-arg behavior
        String s = "-a 1 -b \"C:\\\\Program Files\\\\GCAM\" --name 'hello world' --flag";
        java.util.List<String> tokens = CommandLineTokenizer.tokenize(s);

        assertTrue(tokens.size() == 7, "Unexpected token count: " + tokens);
        assertTrue(tokens.get(0).equals("-a"), "token0 mismatch: " + tokens);
        assertTrue(tokens.get(1).equals("1"), "token1 mismatch: " + tokens);
        assertTrue(tokens.get(2).equals("-b"), "token2 mismatch: " + tokens);
        assertTrue(tokens.get(3).equals("C:\\Program Files\\GCAM"), "token3 mismatch: " + tokens);
        assertTrue(tokens.get(4).equals("--name"), "token4 mismatch: " + tokens);
        assertTrue(tokens.get(5).equals("hello world"), "token5 mismatch: " + tokens);
        assertTrue(tokens.get(6).equals("--flag"), "token6 mismatch: " + tokens);
    }

    private static void testInteractiveSendLine() throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ArrayList<String> cmd = new ArrayList<>();

        if (isWindows) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add("set /p IN=& echo stdin=%IN% & exit /b 0");
        } else {
            cmd.add("sh");
            cmd.add("-c");
            cmd.add("IFS= read IN; echo stdin=$IN; exit 0");
        }

        ProcessRunner.RunningProcess rp = ProcessRunner.start(cmd, new File("."), null, null, null);
        try {
            Thread.sleep(150);
            assertTrue(rp.sendLine(), "Expected sendLine() to succeed while process is waiting for input");
            ProcessResult r = rp.waitForResult(java.time.Duration.ofSeconds(5));
            assertTrue(r.getExitCode() == 0, "Expected interactive process to exit cleanly: " + r.getExitCode());
            assertTrue(r.getStdout().contains("stdin="), "stdout missing echoed stdin marker: " + r.getStdout());
        } finally {
            try {
                rp.stop();
            } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) throws Exception {
        testTokenizer();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ArrayList<String> cmd = new ArrayList<>();

        if (isWindows) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add("echo hello_stdout & echo hello_stderr 1>&2 & exit /b 7");
        } else {
            cmd.add("sh");
            cmd.add("-c");
            cmd.add("echo hello_stdout; echo hello_stderr 1>&2; exit 7");
        }

        ProcessResult r = ProcessRunner.run(cmd, new File("."), null, null);

        System.out.println("exitCode=" + r.getExitCode());
        System.out.println("success=" + r.isSuccess());
        System.out.println("stdout=" + r.getStdout().trim());
        System.out.println("stderr=" + r.getStderr().trim());

        assertTrue(r.getExitCode() == 7, "Expected exitCode=7");
        assertTrue(r.getStdout().contains("hello_stdout"), "stdout missing expected text");
        assertTrue(r.getStderr().contains("hello_stderr"), "stderr missing expected text");

        testInteractiveSendLine();

        System.out.println("OK");
    }
}