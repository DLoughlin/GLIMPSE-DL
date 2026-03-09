/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package glimpseUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs an external process and captures stdout/stderr and the exit code.
 * Uses ProcessBuilder and drains both streams concurrently to avoid deadlocks.
 */
public final class ProcessRunner {

    private ProcessRunner() {}

    /**
     * Result of a stop request (best-effort).
     * This reports what we attempted and what we observed synchronously during the stop() call.
     */
    public static final class StopResult {
        public enum Outcome {
            /** stop() was already called previously on this handle. */
            ALREADY_REQUESTED,
            /** The process had already exited before we tried to stop it. */
            ALREADY_EXITED,
            /** destroy() was sufficient and the process exited within the graceful timeout. */
            GRACEFUL,
            /** destroyForcibly() was used and the process exited within the force timeout. */
            FORCED,
            /** We tried to stop it, but it still appeared alive after timeouts. */
            FAILED,
            /** stop() was interrupted while waiting. */
            INTERRUPTED,
            /** No underlying process exists. */
            NO_PROCESS
        }

        private final Outcome outcome;
        private final Long pid;
        private final Integer exitCode;
        private final boolean aliveAfterStop;
        private final long waitedMillis;
        private final String error;

        public StopResult(Outcome outcome, Long pid, Integer exitCode, boolean aliveAfterStop, long waitedMillis, String error) {
            this.outcome = outcome;
            this.pid = pid;
            this.exitCode = exitCode;
            this.aliveAfterStop = aliveAfterStop;
            this.waitedMillis = waitedMillis;
            this.error = error;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public Long getPid() {
            return pid;
        }

        public Integer getExitCode() {
            return exitCode;
        }

        public boolean isAliveAfterStop() {
            return aliveAfterStop;
        }

        public long getWaitedMillis() {
            return waitedMillis;
        }

        public String getError() {
            return error;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(outcome);
            // pid may be null on older Java runtimes.
            if (pid != null) {
                sb.append(", pid=").append(pid);
            }
            if (exitCode != null) {
                sb.append(", exitCode=").append(exitCode);
            }
            if (waitedMillis > 0) {
                sb.append(", waited=").append(waitedMillis).append("ms");
            }
            if (aliveAfterStop) {
                sb.append(", stillAlive=true");
            }
            if (error != null && !error.trim().isEmpty()) {
                sb.append(", error=").append(error);
            }
            return sb.toString();
        }
    }

    /**
     * A handle representing a running external process started by {@link ProcessRunner#start(List, File, Map, LineConsumer, LineConsumer)}.
     * <p>
     * This allows the caller (UI) to request termination of the running process.
     */
    public interface RunHandle {
        /**
         * Requests that the process be terminated.
         * <p>
         * Implementation is best-effort: it will try a graceful destroy(), then destroyForcibly().
         *
         * @return structured information about the stop attempt.
         */
        StopResult stop();

        /** @return true if stop() has been requested. */
        boolean isStopRequested();

        /** @return the underlying Process (may be null if start failed). */
        Process getProcess();

        /**
         * Sends raw text to the child process stdin using UTF-8.
         *
         * @return true if the text was written and flushed; false if stdin was unavailable or the process had already exited.
         */
        boolean sendInput(String text);

        /**
         * Sends a single platform newline to the child process stdin.
         *
         * @return true if the newline was written and flushed.
         */
        boolean sendLine();
    }

    /**
     * Starts a process and streams stdout/stderr lines to the provided consumers while also capturing full output.
     * The returned handle can be used to stop the process while {@link RunningProcess#waitForResult()} is waiting.
     */
    public static RunningProcess start(List<String> command,
                                      File workingDir,
                                      Map<String, String> extraEnv,
                                      LineConsumer stdoutLineConsumer,
                                      LineConsumer stderrLineConsumer) throws IOException {
        Objects.requireNonNull(command, "command");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }

        applyJavaEnvAndEcho(pb);

        Process process = pb.start();
        return new RunningProcess(process, stdoutLineConsumer, stderrLineConsumer);
    }

    /**
     * Wrapper that owns the I/O drain threads and provides a stop() hook.
     */
    public static final class RunningProcess implements RunHandle {
        private final Process process;
        private final ExecutorService ioPool;
        private final Future<String> outFuture;
        private final Future<String> errFuture;
        private final Writer stdinWriter;
        private final Object stdinLock = new Object();
        private final long startMillis;
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);

        private RunningProcess(Process process, LineConsumer stdoutLineConsumer, LineConsumer stderrLineConsumer) {
            this.process = process;
            this.startMillis = System.currentTimeMillis();
            Charset charset = StandardCharsets.UTF_8;
            this.stdinWriter = new OutputStreamWriter(process.getOutputStream(), charset);
            this.ioPool = Executors.newFixedThreadPool(2);
            this.outFuture = ioPool.submit(streamToString(process.getInputStream(), charset, stdoutLineConsumer));
            this.errFuture = ioPool.submit(streamToString(process.getErrorStream(), charset, stderrLineConsumer));
        }

        @Override
        public StopResult stop() {
            if (!stopRequested.compareAndSet(false, true)) {
                return new StopResult(StopResult.Outcome.ALREADY_REQUESTED, null, safeExit(process), isAlive(process), 0, null);
            }
            if (process == null) {
                return new StopResult(StopResult.Outcome.NO_PROCESS, null, null, false, 0, null);
            }

            // If it already exited, report it.
            if (!isAlive(process)) {
                return new StopResult(StopResult.Outcome.ALREADY_EXITED, null, safeExit(process), false, 0, null);
            }

            long startWait = System.currentTimeMillis();
            long waited = 0;
            try {
                try {
                    process.destroy();
                } catch (Exception ignored) {}

                boolean exited = false;
                try {
                    exited = process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    waited = System.currentTimeMillis() - startWait;
                    return new StopResult(StopResult.Outcome.INTERRUPTED, null, safeExit(process), isAlive(process), waited, ie.toString());
                }

                if (exited) {
                    waited = System.currentTimeMillis() - startWait;
                    return new StopResult(StopResult.Outcome.GRACEFUL, null, safeExit(process), false, waited, null);
                }

                // Escalate.
                try {
                    process.destroyForcibly();
                } catch (Exception ignored) {}

                try {
                    exited = process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    waited = System.currentTimeMillis() - startWait;
                    return new StopResult(StopResult.Outcome.INTERRUPTED, null, safeExit(process), isAlive(process), waited, ie.toString());
                }

                waited = System.currentTimeMillis() - startWait;
                if (exited || !isAlive(process)) {
                    return new StopResult(StopResult.Outcome.FORCED, null, safeExit(process), false, waited, null);
                }
                return new StopResult(StopResult.Outcome.FAILED, null, safeExit(process), true, waited, null);
            } catch (Exception e) {
                waited = System.currentTimeMillis() - startWait;
                return new StopResult(StopResult.Outcome.FAILED, null, safeExit(process), isAlive(process), waited, e.toString());
            }
        }

        @Override
        public boolean isStopRequested() {
            return stopRequested.get();
        }

        @Override
        public Process getProcess() {
            return process;
        }

        @Override
        public boolean sendInput(String text) {
            if (text == null) {
                return false;
            }
            if (process == null || !isAlive(process)) {
                return false;
            }
            synchronized (stdinLock) {
                try {
                    stdinWriter.write(text);
                    stdinWriter.flush();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        @Override
        public boolean sendLine() {
            return sendInput(System.lineSeparator());
        }

        /**
         * Waits for the process to finish (or timeout) and returns the collected outputs.
         */
        public ProcessResult waitForResult(Duration timeout) throws InterruptedException {
            boolean finished;
            if (timeout == null) {
                process.waitFor();
                finished = true;
            } else {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            boolean timedOut = !finished;
            int exitCode;
            if (timedOut) {
                stop();
                exitCode = -1;
            } else {
                exitCode = process.exitValue();
            }

            String stdout = safeGet(outFuture);
            String stderr = safeGet(errFuture);
            long duration = System.currentTimeMillis() - startMillis;

            // Ensure the pool is stopped.
            try {
                synchronized (stdinLock) {
                    try {
                        stdinWriter.close();
                    } catch (Exception ignored) {}
                }
            } finally {
                ioPool.shutdownNow();
            }
            return new ProcessResult(exitCode, stdout, stderr, timedOut, duration);
        }

        /** Convenience for no timeout. */
        public ProcessResult waitForResult() throws InterruptedException {
            return waitForResult(null);
        }
    }

    public interface LineConsumer {
        void accept(String line);
    }

    public static ProcessResult run(List<String> command,
                                    File workingDir,
                                    Map<String, String> extraEnv,
                                    Duration timeout,
                                    LineConsumer stdoutLineConsumer,
                                    LineConsumer stderrLineConsumer)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        long start = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        if (extraEnv != null && !extraEnv.isEmpty()) {
            pb.environment().putAll(extraEnv);
        }

        applyJavaEnvAndEcho(pb);

        Process process = pb.start();

        Charset charset = StandardCharsets.UTF_8;
        ExecutorService ioPool = Executors.newFixedThreadPool(2);
        try {
            Future<String> outFuture = ioPool.submit(streamToString(process.getInputStream(), charset, stdoutLineConsumer));
            Future<String> errFuture = ioPool.submit(streamToString(process.getErrorStream(), charset, stderrLineConsumer));

            boolean finished;
            if (timeout == null) {
                process.waitFor();
                finished = true;
            } else {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            boolean timedOut = !finished;
            int exitCode;
            if (timedOut) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
                exitCode = -1;
            } else {
                exitCode = process.exitValue();
            }

            String stdout = safeGet(outFuture);
            String stderr = safeGet(errFuture);
            long duration = System.currentTimeMillis() - start;
            return new ProcessResult(exitCode, stdout, stderr, timedOut, duration);
        } finally {
            ioPool.shutdownNow();
        }
    }

    public static ProcessResult run(List<String> command, File workingDir, Map<String, String> extraEnv, Duration timeout)
            throws IOException, InterruptedException {
        return run(command, workingDir, extraEnv, timeout, null, null);
    }

    private static Callable<String> streamToString(InputStream in, Charset charset, LineConsumer onLine) {
        return () -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, charset))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (onLine != null) {
                        try {
                            onLine.accept(line);
                        } catch (Exception ignored) {
                            // Ignore callback failures; do not break process drain.
                        }
                    }
                    sb.append(line).append(System.lineSeparator());
                }
            }
            return sb.toString();
        };
    }

    private static Callable<String> streamToString(InputStream in, Charset charset) {
        return streamToString(in, charset, null);
    }

    private static String safeGet(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException e) {
            return "";
        }
    }

    private static boolean isAlive(Process p) {
        try {
            return p != null && p.isAlive();
        } catch (Throwable t) {
            return false;
        }
    }

    private static Integer safeExit(Process p) {
        try {
            if (p == null || p.isAlive()) {
                return null;
            }
            return p.exitValue();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void applyJavaEnvAndEcho(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        String javaHome = env.get("JAVA_HOME");
        if (javaHome == null || javaHome.trim().isEmpty()) {
            String sysJavaHome = System.getenv("JAVA_HOME");
            if (sysJavaHome != null && !sysJavaHome.trim().isEmpty()) {
                javaHome = sysJavaHome;
                env.put("JAVA_HOME", javaHome);
            }
        }

        String pathKey = getPathKey(env);
        if (pathKey == null) {
            pathKey = "PATH";
        }

        String pathValue = env.get(pathKey);
        if (pathValue == null || pathValue.trim().isEmpty()) {
            String sysPath = System.getenv("PATH");
            if (sysPath != null && !sysPath.trim().isEmpty()) {
                pathValue = sysPath;
            }
        }

        if (javaHome != null && !javaHome.trim().isEmpty()) {
            String javaBin = javaHome + File.separator + "bin";
            if (pathValue == null || pathValue.trim().isEmpty()) {
                pathValue = javaBin;
            } else if (!pathValue.startsWith(javaBin + File.pathSeparator)) {
                pathValue = javaBin + File.pathSeparator + pathValue;
            }
        }

        if (pathValue != null && !pathValue.trim().isEmpty()) {
            env.put(pathKey, pathValue);
        }

        System.out.println("JAVA_HOME=" + env.get("JAVA_HOME"));
        System.out.println("PATH=" + env.get(pathKey));
    }

    private static String getPathKey(Map<String, String> env) {
        for (String key : env.keySet()) {
            if ("PATH".equalsIgnoreCase(key)) {
                return key;
            }
        }
        return null;
    }
}