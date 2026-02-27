/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package gui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * Redirects {@link System#out} and {@link System#err} to {@link ConsoleManager}.
 * <p>
 * This is intentionally lightweight and UI-friendly:
 * <ul>
 *   <li>Buffers data until newline, then appends a line to the console tab.</li>
 *   <li>Also forwards to the original streams so output still shows in the launching terminal.</li>
 *   <li>Safe to call before the JavaFX stage exists; {@link ConsoleManager} will lazily create it.</li>
 * </ul>
 */
final class ConsoleOutputRedirect {

    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static boolean installed;

    private ConsoleOutputRedirect() {}

    static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        originalOut = System.out;
        originalErr = System.err;

        // Use platform default charset to match the default PrintStream behaviour.
        final String csName = Charset.defaultCharset().name();

        try {
            System.setOut(new PrintStream(new TeeOutputStream(originalOut,
                    new LineBufferingFxOutputStream(ConsoleManager.StreamSource.GLIMPSE_STDOUT, csName)), true, csName));

            System.setErr(new PrintStream(new TeeOutputStream(originalErr,
                    new LineBufferingFxOutputStream(ConsoleManager.StreamSource.GLIMPSE_STDERR, csName)), true, csName));
        } catch (java.io.UnsupportedEncodingException e) {
            // Extremely unlikely for the platform default charset, but fall back safely.
            System.setOut(new PrintStream(new TeeOutputStream(originalOut,
                    new LineBufferingFxOutputStream(ConsoleManager.StreamSource.GLIMPSE_STDOUT, null)), true));
            System.setErr(new PrintStream(new TeeOutputStream(originalErr,
                    new LineBufferingFxOutputStream(ConsoleManager.StreamSource.GLIMPSE_STDERR, null)), true));
        }

        ConsoleManager.appendHeader(ConsoleManager.StreamSource.GLIMPSE_STDOUT, "System.out redirected to GLIMPSE Console");
        ConsoleManager.appendHeader(ConsoleManager.StreamSource.GLIMPSE_STDERR, "System.err redirected to GLIMPSE Console");
    }

    static synchronized void uninstall() {
        if (!installed) {
            return;
        }
        installed = false;
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int b1) throws IOException {
            a.write(b1);
            b.write(b1);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }

        @Override
        public void close() throws IOException {
            // Don't close the original streams.
            b.flush();
        }
    }

    private static final class LineBufferingFxOutputStream extends OutputStream {
        private final ConsoleManager.StreamSource source;
        private final String charsetName;
        private final StringBuilder sb = new StringBuilder(256);

        LineBufferingFxOutputStream(ConsoleManager.StreamSource source, String charsetName) {
            this.source = source;
            this.charsetName = charsetName;
        }

        @Override
        public synchronized void write(int b) {
            char c = (char) (b & 0xFF);
            if (c == '\r') {
                return;
            }
            if (c == '\n') {
                flushLine();
                return;
            }
            sb.append(c);
            // Safety valve: if someone writes forever without newlines.
            if (sb.length() >= 4096) {
                flushLine();
            }
        }

        @Override
        public synchronized void write(byte[] buf, int off, int len) {
            if (buf == null || len <= 0) {
                return;
            }
            String s;
            if (charsetName != null) {
                try {
                    s = new String(buf, off, len, charsetName);
                } catch (Exception e) {
                    s = new String(buf, off, len);
                }
            } else {
                s = new String(buf, off, len);
            }
            for (int i = 0; i < s.length(); i++) {
                write(s.charAt(i));
            }
        }

        private void flushLine() {
            if (sb.length() == 0) {
                ConsoleManager.appendLine(source, "");
                return;
            }
            ConsoleManager.appendLine(source, sb.toString());
            sb.setLength(0);
        }

        @Override
        public synchronized void flush() {
            if (sb.length() > 0) {
                flushLine();
            }
        }
    }
}