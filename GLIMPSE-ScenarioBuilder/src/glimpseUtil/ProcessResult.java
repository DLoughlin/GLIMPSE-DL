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

    /**
     * Creates an immutable process-result snapshot.
     *
     * @param exitCode process exit code, or a sentinel value when timed out
     * @param stdout captured standard output
     * @param stderr captured standard error
     * @param timedOut whether the process timed out
     * @param durationMillis elapsed execution time in milliseconds
     */
    public ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long durationMillis) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.timedOut = timedOut;
        this.durationMillis = durationMillis;
    }

    /**
     * Returns the process exit code.
     *
     * @return exit code reported by the process
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Returns the captured standard output.
     *
     * @return process standard output text
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Returns the captured standard error.
     *
     * @return process standard error text
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Indicates whether execution ended because of a timeout.
     *
     * @return {@code true} when the process timed out
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Returns the total runtime in milliseconds.
     *
     * @return elapsed execution time in milliseconds
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Indicates whether the process completed successfully.
     *
     * @return {@code true} when the process exited normally with code 0
     */
    public boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }

    @Override
    public String toString() {
        return "ProcessResult{exitCode=" + exitCode + ", timedOut=" + timedOut + ", durationMillis=" + durationMillis + "}";
    }
}
