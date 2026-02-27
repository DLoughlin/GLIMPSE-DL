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

/**
 * Runs an external process and captures stdout/stderr and the exit code.
 * Uses ProcessBuilder and drains both streams concurrently to avoid deadlocks.
 */
public final class ProcessRunner {

    private ProcessRunner() {}

    public static ProcessResult run(List<String> command, File workingDir, Map<String, String> extraEnv, Duration timeout)
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

        Process process = pb.start();

        Charset charset = StandardCharsets.UTF_8;
        ExecutorService ioPool = Executors.newFixedThreadPool(2);
        try {
            Future<String> outFuture = ioPool.submit(streamToString(process.getInputStream(), charset));
            Future<String> errFuture = ioPool.submit(streamToString(process.getErrorStream(), charset));

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
                // Best effort kill.
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
}
