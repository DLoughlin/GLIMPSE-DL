/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package glimpseUtil;

import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A tiny "tail -f" helper.
 * <p>
 * This repeatedly polls a text file for appended bytes and forwards new lines
 * to a callback.
 * <p>
 * Notes/constraints:
 * <ul>
 *   <li>Intended for logs that are appended to (like GCAM's logs/main_log.txt).</li>
 *   <li>Best-effort: if the file is truncated/rotated, it will restart from 0.</li>
 *   <li>Line parsing is based on '\n'. CRLF is normalized.</li>
 * </ul>
 */
public final class FileTailer {

    private FileTailer() {}

    public interface LineConsumer {
        void accept(String line);
    }

    /** A handle to stop the tailer thread. */
    public interface TailHandle {
        /** Request stop (best-effort). */
        void stop();

        /** @return true if stop() was requested. */
        boolean isStopRequested();

        /** Wait for the thread to finish. */
        void join(Duration timeout) throws InterruptedException;
    }

    public static TailHandle start(Path file,
                                  Charset charset,
                                  Duration pollInterval,
                                  Duration initialWaitForFile,
                                  LineConsumer onLine) {
        Objects.requireNonNull(file, "file");

        final Charset useCharset = (charset == null) ? StandardCharsets.UTF_8 : charset;
        final Duration usePollInterval;
        if (pollInterval == null || pollInterval.isZero() || pollInterval.isNegative()) {
            usePollInterval = Duration.ofMillis(200);
        } else {
            usePollInterval = pollInterval;
        }

        final Duration useInitialWait;
        if (initialWaitForFile == null || initialWaitForFile.isNegative()) {
            useInitialWait = Duration.ofSeconds(0);
        } else {
            useInitialWait = initialWaitForFile;
        }

        AtomicBoolean stop = new AtomicBoolean(false);

        Thread t = new Thread(() -> runLoop(file, useCharset, usePollInterval, useInitialWait, onLine, stop),
                "FileTailer-" + safeThreadName(file));
        t.setDaemon(true);
        t.start();

        return new TailHandle() {
            @Override
            public void stop() {
                stop.set(true);
                try {
                    t.interrupt();
                } catch (Exception ignored) {}
            }

            @Override
            public boolean isStopRequested() {
                return stop.get();
            }

            @Override
            public void join(Duration timeout) throws InterruptedException {
                if (timeout == null) {
                    t.join();
                } else {
                    t.join(Math.max(0, timeout.toMillis()));
                }
            }

            @Override
            public String toString() {
                return "FileTailer{" + file + ", charset=" + useCharset + ", poll=" + usePollInterval + ", initialWait=" + useInitialWait + "}";
            }
        };
    }

    private static void runLoop(Path file,
                               Charset charset,
                               Duration pollInterval,
                               Duration initialWaitForFile,
                               LineConsumer onLine,
                               AtomicBoolean stop) {

        long startWait = System.currentTimeMillis();
        while (!stop.get() && !Files.exists(file)) {
            long waited = System.currentTimeMillis() - startWait;
            if (initialWaitForFile.toMillis() <= 0 || waited >= initialWaitForFile.toMillis()) {
                // Give up quietly.
                return;
            }
            sleepQuiet(pollInterval);
        }

        long pos = 0;
        StringBuilder partial = new StringBuilder();

        while (!stop.get()) {
            try {
                if (!Files.exists(file)) {
                    // File disappeared; wait a bit.
                    sleepQuiet(pollInterval);
                    continue;
                }

                long len;
                try {
                    len = Files.size(file);
                } catch (Exception e) {
                    sleepQuiet(pollInterval);
                    continue;
                }

                if (len < pos) {
                    // Truncated/rotated.
                    pos = 0;
                    partial.setLength(0);
                }

                if (len == pos) {
                    sleepQuiet(pollInterval);
                    continue;
                }

                try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                    raf.seek(pos);
                    byte[] buf = new byte[8192];
                    int read;
                    while (!stop.get() && (read = raf.read(buf)) > 0) {
                        String chunk = new String(buf, 0, read, charset);
                        partial.append(chunk);
                        drainLines(partial, onLine);
                        pos = raf.getFilePointer();
                    }
                }

            } catch (Exception ignored) {
                // Best-effort tailing; don't crash.
            }

            sleepQuiet(pollInterval);
        }

        // On stop, flush any remaining partial content as a final line.
        try {
            String last = partial.toString().replace("\r", "");
            if (!last.isEmpty() && onLine != null) {
                onLine.accept(last);
            }
        } catch (Exception ignored) {}
    }

    private static void drainLines(StringBuilder partial, LineConsumer onLine) {
        if (onLine == null) {
            // Still keep partial bounded by dropping already-terminated lines.
            int lastNl = partial.lastIndexOf("\n");
            if (lastNl >= 0) {
                partial.delete(0, lastNl + 1);
            }
            return;
        }

        int idx;
        while ((idx = partial.indexOf("\n")) >= 0) {
            String line = partial.substring(0, idx);
            // Normalize CRLF.
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            try {
                onLine.accept(line);
            } catch (Exception ignored) {}
            partial.delete(0, idx + 1);
        }

        // Keep runaway partial lines bounded.
        if (partial.length() > 1024 * 1024) {
            partial.delete(0, partial.length() - (256 * 1024));
        }
    }

    private static String safeThreadName(Path file) {
        try {
            Path name = file.getFileName();
            if (name != null) {
                return name.toString();
            }
        } catch (Exception ignored) {}
        return "log";
    }

    private static void sleepQuiet(Duration d) {
        try {
            Thread.sleep(Math.max(10L, d.toMillis()));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {}
    }
}
