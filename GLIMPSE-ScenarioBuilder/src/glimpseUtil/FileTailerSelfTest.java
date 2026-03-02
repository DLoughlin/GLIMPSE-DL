/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package glimpseUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple self-test for {@link FileTailer}.
 * <p>
 * Writes a temp file in small bursts and ensures the tailer sees the lines.
 */
public final class FileTailerSelfTest {

    private FileTailerSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempFile("glimpse-filetailer-", ".txt");
        tmp.toFile().deleteOnExit();

        List<String> seen = new ArrayList<>();

        FileTailer.TailHandle h = FileTailer.start(
                tmp,
                StandardCharsets.UTF_8,
                Duration.ofMillis(50),
                Duration.ofSeconds(2),
                line -> {
                    synchronized (seen) {
                        seen.add(line);
                    }
                });

        // Append a couple lines.
        Files.write(tmp, ("a\r\n").getBytes(StandardCharsets.UTF_8));
        Thread.sleep(120);
        Files.write(tmp, ("b\n").getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
        Thread.sleep(120);

        h.stop();
        h.join(Duration.ofSeconds(2));

        // Validate.
        String join;
        synchronized (seen) {
            join = String.join(",", seen);
        }
        if (!join.contains("a") || !join.contains("b")) {
            throw new IllegalStateException("Tailer did not see expected lines. saw=" + join + " file=" + tmp);
        }

        System.out.println("OK FileTailerSelfTest saw: " + join);
    }
}
