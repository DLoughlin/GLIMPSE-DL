/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 */
package glimpseUtil;

/**
 * Simple immutable value object containing the result of running an external process.
 * Captures stdout/stderr, exit code, and a few timing flags.
 */
public final class ProcessResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final boolean timedOut;
    private final long durationMillis;

    public ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long durationMillis) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.timedOut = timedOut;
        this.durationMillis = durationMillis;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }

    @Override
    public String toString() {
        return "ProcessResult{exitCode=" + exitCode + ", timedOut=" + timedOut + ", durationMillis=" + durationMillis + "}";
    }
}
