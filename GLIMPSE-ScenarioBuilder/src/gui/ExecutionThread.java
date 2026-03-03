/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 *
 * EXPORT CONTROL
 * User agrees that the Software will not be shipped, transferred or
 * exported into any country or used in any manner prohibited by the
 * United States Export Administration Act or any other applicable
 * export laws, restrictions or regulations (collectively the "Export Laws").
 * Export of the Software may require some form of license or other
 * authority from the U.S. Government, and failure to obtain such
 * export control license may result in criminal liability under
 * U.S. laws. In addition, if the Software is identified as export controlled
 * items under the Export Laws, User represents and warrants that User
 * is not a citizen, or otherwise located within, an embargoed nation
 * (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
 *     and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 *
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
 */
package gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import glimpseUtil.ProcessRunner;
import glimpseUtil.ProcessResult;
import glimpseUtil.StatusChecker;

/** 
 * ExecutionThread manages the execution of background tasks (commands, runnables, callables) using an ExecutorService and tracks their status for the GLIMPSE Scenario Builder.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Supports both single-threaded and multi-threaded execution modes for background jobs.</li>
 *   <li>Allows submission of command-line jobs, Runnable, and Callable tasks, with or without working directories.</li>
 *   <li>Tracks all submitted jobs and provides methods to check completion, clean up finished jobs, and retrieve job status.</li>
 *   <li>Integrates with a StatusChecker to monitor and report job progress.</li>
 *   <li>Provides thread-safe methods for job submission, status checking, and shutdown operations.</li>
 *   <li>Implements AutoCloseable for use in try-with-resources blocks.</li>
 *   <li>Includes deprecated methods for backward compatibility with legacy code.</li>
 * </ul>
 * <p>
 * <b>Usage:</b> Instantiate and start the executor using {@link #startUpExecutorSingle()} or {@link #startUpExecutorMulti()}, then submit jobs using the provided methods. Call {@link #shutdown()} or use try-with-resources to ensure proper cleanup.
 * <p>
 * <b>Thread Safety:</b> All public methods are thread-safe unless otherwise noted. The jobs list is synchronized for all access.
 *
 */
public class ExecutionThread implements AutoCloseable {
    private ExecutorService executorService = null;

    /**
     * Optional routing for streaming external process output into the in-app console.
     * If null, output is still captured in {@link glimpseUtil.ProcessResult} but not streamed live.
     */
    private volatile ConsoleManager.StreamSource consoleStreamTarget = null;

    /**
     * Enables live streaming of process output to the given ConsoleManager tab.
     *
     * @param target the console stream target (e.g., MODEL_INTERFACE). If null, streaming is disabled.
     */
    public void setConsoleStreamTarget(ConsoleManager.StreamSource target) {
        this.consoleStreamTarget = target;
    }

    /**
     * List of submitted jobs. All iteration over this list must be synchronized on the list.
     * Each Future represents a submitted background task (Runnable, Callable, or command).
     */
    private final List<Future<?>> jobs = Collections.synchronizedList(new ArrayList<>());
    private final StatusChecker status = new StatusChecker();
    private final AtomicBoolean isCheckingStatus = new AtomicBoolean(false);
    private int numDone = 0;

    /**
     * Best-effort pointer to the task currently running on the executor worker thread
     * (reliable for the common single-thread executor case).
     */
    private volatile Future<?> currentRunningFuture;

    /** Incrementing id for jobs submitted to this ExecutionThread (for easier logging). */
    private final AtomicLong jobIdCounter = new AtomicLong(0);

    /** Best-effort metadata about submitted jobs for logging/debugging. */
    private final Map<Future<?>, String> jobLabels = new ConcurrentHashMap<>();

    /** Optional interface for tasks that can provide a readable description for logs. */
    public interface DebugDescribable {
        /** @return short human-friendly description (no newlines preferred). */
        String getDebugDescription();
    }

    private static String describeCallableForLog(Callable<?> callable) {
        if (callable == null) {
            return "<null>";
        }
        try {
            if (callable instanceof DebugDescribable) {
                String s = ((DebugDescribable) callable).getDebugDescription();
                if (s != null && !s.trim().isEmpty()) {
                    return s.trim();
                }
            }
        } catch (Throwable ignored) {}

        // Fall back to class name + identity hash for stable identification.
        String name;
        try {
            name = callable.getClass().getName();
        } catch (Throwable t) {
            name = "Callable";
        }
        return name + "@" + Integer.toHexString(System.identityHashCode(callable));
    }

    private static String describeRunnableForLog(Runnable runnable) {
        if (runnable == null) {
            return "<null>";
        }
        try {
            if (runnable instanceof DebugDescribable) {
                String s = ((DebugDescribable) runnable).getDebugDescription();
                if (s != null && !s.trim().isEmpty()) {
                    return s.trim();
                }
            }
        } catch (Throwable ignored) {}

        String name;
        try {
            name = runnable.getClass().getName();
        } catch (Throwable t) {
            name = "Runnable";
        }
        return name + "@" + Integer.toHexString(System.identityHashCode(runnable));
    }

    private static String safeOneLine(String s) {
        if (s == null) return "";
        return s.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private String labelAndTrackFuture(Future<?> f, long jobId, String label) {
        String combined = "jobId=" + jobId + ", " + safeOneLine(label);
        try {
            if (f != null) {
                jobLabels.put(f, combined);
            }
        } catch (Throwable ignored) {}
        return combined;
    }

    /**
     * Checks if the number of completed jobs has changed since the last check.
     * <p>
     * Iterates over the jobs list and counts the number of jobs that are done. If the count has changed
     * since the last invocation, updates the internal counter and returns true.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     *
     * @return true if the number of completed jobs has changed, false otherwise.
     */
    public boolean didNumDoneChange() {
        boolean rtnBool = false;
        int localNumDone = 0;
        synchronized (jobs) {
            for (Future<?> job : jobs) {
                if (job.isDone()) localNumDone++;
            }
        }
        if (localNumDone == numDone) {
            rtnBool = false;
        } else {
            rtnBool = true;
            numDone = localNumDone;
        }
        return rtnBool;
    }

    /**
     * Adds an array of command strings as background jobs to the execution queue.
     * <p>
     * Each command string is submitted to the executor as a task executed via {@link glimpseUtil.ProcessRunner}.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     *
     * @param commands Array of command strings to execute as background jobs.
     */
    public void submitCommands(String[] commands) {
        try {
            submitCommandTasks(commands);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while adding commands to execute queue.", e);
        }
    }

    /**
     * Starts a single-threaded executor if not already started.
     * <p>
     * This method should be called during initialization if only one background job should run at a time.
     * </p>
     * <b>Thread safety:</b> This method is not thread-safe and should be called during initialization.
     */
    public void startUpExecutorSingle() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        } else {
            System.err.println("ExecutorService already started.");
        }
    }

    /**
     * Starts a cached thread pool executor if not already started.
     * <p>
     * This method should be called during initialization if multiple background jobs may run concurrently.
     * </p>
     * <b>Thread safety:</b> This method is not thread-safe and should be called during initialization.
     */
    public void startUpExecutorMulti() {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        } else {
            System.err.println("ExecutorService already started.");
        }
    }

    /**
     * Submits an array of Runnable tasks to the executor.
     * <p>
     * Each Runnable is submitted as a separate job. Useful for batch job submission.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     *
     * @param runnables Array of Runnable tasks to execute.
     */
    public void executeRunnables(Runnable[] runnables) {
        for (Runnable runnable : runnables) {
            executeRunnable(runnable);
        }
    }

    /**
     * Submits a single Runnable task to the executor.
     * <p>
     * The Runnable is submitted as a background job and tracked in the jobs list.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     *
     * @param runnable The Runnable task to execute.
     */
    public void executeRunnable(Runnable runnable) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = describeRunnableForLog(runnable);
        System.out.println("Submitting runnable to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<Void> ft = new java.util.concurrent.FutureTask<>(wrapRunnableForTracking(runnable, ref), null);
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        jobs.add(ft);
    }

    /**
     * Submits an array of command strings as background tasks to the executor.
     * <p>
     * Each command string is submitted as a background job executed via the system shell
     * (cmd.exe on Windows, /bin/sh on Unix-like systems) for backward compatibility.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     *
     * @param commands Array of command strings to execute.
     * @throws InterruptedException if interrupted while submitting tasks.
     */
    public void submitCommandTasks(String[] commands) throws InterruptedException {
        for (String command : commands) {
            submitCommand(command);
        }
    }

    /**
     * Submits a single command string as a background task to the executor.
     * <p>
     * This overload runs the command through the platform shell for backward compatibility.
     * Prefer the array-based overloads to avoid quoting/tokenization issues.
     * </p>
     *
     * @param command The command string to execute.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommand(String command) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        // Legacy string commands are inherently platform fragile (quoting/spaces).
        // Keep support for backward compatibility by passing through the system shell.
        Callable<ProcessResult> task = () -> {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            java.util.List<String> cmd = new java.util.ArrayList<>();
            if (isWindows) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(command);
            } else {
                cmd.add("/bin/sh");
                cmd.add("-c");
                cmd.add(command);
            }
            return ProcessRunner.run(cmd, null, null, null);
        };

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = "shell: " + safeOneLine(command);
        System.out.println("Submitting to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<ProcessResult> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(task, ref));
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    /**
     * Submits a single command string as a background task to the executor, specifying a working directory.
     *
     * @param command The command string to execute.
     * @param directory The working directory for the command.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommandWithDirectory(String command, String directory) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        Callable<ProcessResult> task = () -> {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            java.util.List<String> cmd = new java.util.ArrayList<>();
            if (isWindows) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(command);
            } else {
                cmd.add("/bin/sh");
                cmd.add("-c");
                cmd.add(command);
            }

            final ConsoleManager.StreamSource target = consoleStreamTarget;
            ProcessRunner.RunningProcess rp = null;
            try {
                rp = ProcessRunner.start(
                        cmd,
                        directory == null ? null : new File(directory),
                        null,
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.MODEL_STDOUT, line)),
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.STDERR, line)));
                trackProcessHandle(rp);
                return rp.waitForResult(null);
            } finally {
                untrackProcessHandle(rp);
            }
        };

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = "shell: " + safeOneLine(command) + (directory == null || directory.trim().isEmpty() ? "" : " (dir=" + safeOneLine(directory) + ")");
        System.out.println("Submitting to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<ProcessResult> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(task, ref));
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    /**
     * Submits an array of command strings as background tasks to the executor, specifying a working directory.
     *
     * @param commands Array of command strings to execute.
     * @param directory The working directory for all commands.
     * @throws InterruptedException if interrupted while submitting tasks.
     */
    public void submitCommandsWithDirectory(String[] commands, String directory) throws InterruptedException {
        for (String command : commands) {
            submitCommandWithDirectory(command, directory);
        }
    }

    /**
     * Submits a single command as an argument array (command + args) with a working directory.
     * <p>
     * Prefer this overload over the String-based command to avoid quoting/tokenization problems
     * (especially on Windows paths with spaces).
     * </p>
     *
     * @param commandArray The command and its arguments.
     * @param directory The working directory for the command.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommandWithDirectory(String[] commandArray, String directory) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        Callable<ProcessResult> task = () -> {
            final ConsoleManager.StreamSource target = consoleStreamTarget;
            ProcessRunner.RunningProcess rp = null;
            try {
                rp = ProcessRunner.start(
                        java.util.Arrays.asList(commandArray),
                        directory == null ? null : new File(directory),
                        null,
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.MODEL_STDOUT, line)),
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.STDERR, line)));
                trackProcessHandle(rp);
                return rp.waitForResult(null);
            } finally {
                untrackProcessHandle(rp);
            }
        };

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = "cmd: " + safeOneLine(java.util.Arrays.toString(commandArray))
                + (directory == null || directory.trim().isEmpty() ? "" : " (dir=" + safeOneLine(directory) + ")");
        System.out.println("Submitting to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<ProcessResult> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(task, ref));
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    /**
     * Submits a single command as an argument list (command + args) with a working directory.
     *
     * @param commandArgs The command and its arguments.
     * @param directory The working directory for the command.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommandWithDirectory(List<String> commandArgs, String directory) {
        if (commandArgs == null) {
            throw new IllegalArgumentException("commandArgs cannot be null");
        }
        return submitCommandWithDirectory(commandArgs.toArray(new String[0]), directory);
    }

    /**
     * Submits a single command as an argument array (command + args) without specifying a working directory.
     *
     * @param commandArray The command and its arguments.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommand(String[] commandArray) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        Callable<ProcessResult> task = () -> {
            final ConsoleManager.StreamSource target = consoleStreamTarget;
            ProcessRunner.RunningProcess rp = null;
            try {
                rp = ProcessRunner.start(
                        java.util.Arrays.asList(commandArray),
                        null,
                        null,
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.MODEL_STDOUT, line)),
                        target == null ? null : (line -> ConsoleManager.appendLineBuffered(target, ConsoleManager.MessageKind.STDERR, line)));
                trackProcessHandle(rp);
                return rp.waitForResult(null);
            } finally {
                untrackProcessHandle(rp);
            }
        };

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = "cmd: " + safeOneLine(java.util.Arrays.toString(commandArray));
        System.out.println("Submitting to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<ProcessResult> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(task, ref));
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    /**
     * Submits a Callable task to the executor and returns the Future for the result.
     * <p>
     * Prefer this method when the caller needs to wait for completion and/or access
     * the Callable's return value.
     * </p>
     *
     * @param <V> The result type returned by the Callable.
     * @param callable The Callable task to execute.
     * @return Future representing the submitted task.
     */
    public <V> Future<V> submitCallable(Callable<V> callable) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        startStatusCheckerIfNeeded();

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = describeCallableForLog(callable);
        System.out.println("Submitting callable to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<V> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(callable, ref));
        ref.set(ft);
        executorService.execute(ft);

        jobLabels.put(ft, "jobId=" + jobId + ", " + label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    /**
     * Submits a Callable task to the executor.
     * <p>
     * Backward-compatible wrapper around {@link #submitCallable(Callable)}.
     * </p>
     *
     * @param <V> The result type returned by the Callable.
     * @param callable The Callable task to execute.
     */
    public <V> void executeCallableCmd(Callable<V> callable) {
        submitCallable(callable);
    }

    /**
     * Removes completed jobs from the jobs list to prevent memory leaks in long-running applications.
     * <p>
     * Iterates over the jobs list and removes any jobs that are done.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list.
     */
    public void cleanupCompletedJobs() {
        synchronized (jobs) {
            jobs.removeIf(Future::isDone);
        }
    }

    /**
     * Attempts to cancel any queued (not-yet-started) jobs while leaving the currently running job alone.
     * <p>
     * This is best-effort: it cancels all Futures after the first not-done Future in our jobs list, which
     * should correspond to the executor's FIFO queue for the common single-threaded GCAM execution thread.
     * </p>
     *
     * @return number of jobs we attempted to cancel
     */
    public int cancelQueuedJobsKeepRunningCurrent() {
        int cancelled = 0;
        synchronized (jobs) {
            // Prefer an explicit pointer if we have one.
            Future<?> running = currentRunningFuture;
            int runningIdx = -1;
            if (running != null) {
                for (int i = 0; i < jobs.size(); i++) {
                    if (jobs.get(i) == running) {
                        runningIdx = i;
                        break;
                    }
                }
            }

            // Fallback: find the first job that isn't done; that's the one likely running right now.
            if (runningIdx < 0) {
                for (int i = 0; i < jobs.size(); i++) {
                    Future<?> f = jobs.get(i);
                    if (f != null && !f.isDone()) {
                        runningIdx = i;
                        break;
                    }
                }
            }

            if (runningIdx < 0) {
                return 0;
            }

            for (int i = runningIdx + 1; i < jobs.size(); i++) {
                Future<?> f = jobs.get(i);
                if (f == null || f.isDone()) {
                    continue;
                }
                try {
                    f.cancel(true);
                    cancelled++;
                } catch (Exception ignored) {
                    cancelled++;
                }
            }

            jobs.removeIf(Future::isDone);
        }
        return cancelled;
    }

    /**
     * Shuts down the executor service gracefully and terminates the status checker.
     * <p>
     * Waits for all running tasks to complete or times out after 30 seconds. Logs any tasks that did not complete.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list and executor state.
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * Shuts down the executor service gracefully and terminates the status checker.
     * <p>
     * Waits for all running tasks to complete or times out after 30 seconds. Logs any tasks that did not complete.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list and executor state.
     */
    public void shutdown() {
        try {
            status.terminate();
        } finally {
            isCheckingStatus.set(false);
            if (executorService != null) {
                try {
                    executorService.shutdown();
                    if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("ExecutorService did not terminate in the specified time.");
                        List<Runnable> droppedTasks = executorService.shutdownNow();
                        if (!droppedTasks.isEmpty()) {
                            System.err.println("ExecutorService was abruptly shut down. The following tasks will not be executed:");
                            for (Runnable task : droppedTasks) {
                                String label = null;
                                try {
                                    if (task instanceof Future<?>) {
                                        label = jobLabels.get((Future<?>) task);
                                    }
                                } catch (Throwable ignored) {}

                                if (label != null && !label.trim().isEmpty()) {
                                    System.err.println("  " + label);
                                } else {
                                    System.err.println("  " + task);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Shutdown interrupted. Remaining tasks may not have completed.");
                } catch (Exception e) {
                    throw new RuntimeException("Error during executorService shutdown.", e);
                } finally {
                    executorService = null;
                }
            }
        }
    }

    /**
     * Shuts down the executor service immediately and terminates the status checker.
     * <p>
     * Attempts to interrupt running tasks and logs any tasks that did not start.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It synchronizes on the jobs list and executor state.
     */
    public void shutdownNow() {
        System.out.println("Attempting to shut down all model threads.");
        try {
            status.terminate();
        } finally {
            isCheckingStatus.set(false);
            if (executorService != null) {
                try {
                    List<Runnable> notStarted = executorService.shutdownNow();
                    if (!notStarted.isEmpty()) {
                        System.err.println("The following tasks were not started and will not be executed:");
                        for (Runnable task : notStarted) {
                            // ExecutorService.shutdownNow() returns the queued Runnables. In our case,
                            // those are typically FutureTask instances created by ExecutorService.submit(...)
                            // (i.e., also implement Future). Use our stored labels when possible.
                            String label = null;
                            try {
                                if (task instanceof Future<?>) {
                                    label = jobLabels.get((Future<?>) task);
                                }
                            } catch (Throwable ignored) {}

                            if (label != null && !label.trim().isEmpty()) {
                                System.err.println("  " + label);
                            } else {
                                System.err.println("  " + task);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error during executorService shutdownNow.", e);
                } finally {
                    executorService = null;
                }
            }
        }
    }

    /**
     * Checks if the executor service is currently executing tasks.
     * <p>
     * Returns true if the executor is not shut down. Also prints the current execution and termination state.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe for concurrent reads.
     *
     * @return true if executing, false otherwise.
     */
    public boolean isExecuting() {
        if (executorService == null) {
            return false;
        } else {
            boolean isExecuting = !executorService.isShutdown();
            boolean isTerminated = !executorService.isTerminated();
            System.out.println("Is executing: " + isExecuting);
            System.out.println("Is terminated: " + isTerminated);
            return isExecuting;
        }
    }

    /**
     * Returns a string representation of the executor service queue.
     * <p>
     * Useful for debugging and logging the state of the executor.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe for concurrent reads.
     *
     * @return String representation of the executor service.
     */
    public String getQueue() {
        return executorService != null ? executorService.toString() : "ExecutorService not started";
    }

    /**
     * Returns the list of jobs. All iteration over this list must be synchronized on the list.
     * <p>
     * Returns a copy of the jobs list to avoid concurrent modification exceptions.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe. It returns a copy of the jobs list within a synchronized block.
     *
     * @return List of Future jobs.
     */
    public List<Future<?>> getJobs() {
        synchronized (jobs) {
            return new ArrayList<>(jobs);
        }
    }

    /**
     * Returns the status checker instance used to monitor job progress.
     * <p>
     * The StatusChecker is started automatically when jobs are submitted.
     * </p>
     * <b>Thread safety:</b> This method is thread-safe for concurrent reads.
     *
     * @return StatusChecker instance.
     */
    public StatusChecker getStatusChecker() {
        return status;
    }

    /**
     * Helper method to start the status checker if not already started.
     * <p>
     * Uses an AtomicBoolean to ensure the status checker is only started once.
     * </p>
     * This method is thread-safe.
     */
    private void startStatusCheckerIfNeeded() {
        if (isCheckingStatus.compareAndSet(false, true)) {
            status.start();
        }
    }

    /**
     * Submits a command array without starting the StatusChecker.
     * <p>
     * This is intended for small headless utilities/tests where JavaFX isn't on the classpath.
     * The main application should prefer {@link #submitCommand(String[])} which starts job status monitoring.
     * </p>
     *
     * @param commandArray The command and its arguments.
     * @return Future representing the submitted task.
     */
    public Future<?> submitCommandNoStatusChecker(String[] commandArray) {
        if (executorService == null) {
            throw new IllegalStateException("ExecutorService not started.");
        }
        Callable<ProcessResult> task = () -> ProcessRunner.run(
                java.util.Arrays.asList(commandArray),
                null,
                null,
                null);

        final long jobId = jobIdCounter.incrementAndGet();
        final String label = "cmd (no status checker): " + safeOneLine(java.util.Arrays.toString(commandArray));
        System.out.println("Submitting to queue [jobId=" + jobId + "]: " + label);

        java.util.concurrent.atomic.AtomicReference<Future<?>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.FutureTask<ProcessResult> ft = new java.util.concurrent.FutureTask<>(wrapCallableForTracking(task, ref));
        ref.set(ft);
        executorService.execute(ft);

        labelAndTrackFuture(ft, jobId, label);

        synchronized (jobs) {
            jobs.add(ft);
        }
        return ft;
    }

    // Deprecated methods for backward compatibility
    /**
     * @deprecated Use submitCommands instead.
     */
    @Deprecated
    public void addCommandsToQueue(String[] args) {
        submitCommands(args);
    }
    /**
     * @deprecated Use submitCommandTasks instead.
     */
    @Deprecated
    public void executeRunnableCmds(String[] args) throws InterruptedException {
        submitCommandTasks(args);
    }
    /**
     * @deprecated Use submitCommand instead.
     */
    @Deprecated
    public Future<?> executeRunnableCmd(String arg) {
        return submitCommand(arg);
    }

    private <T> Callable<T> wrapCallableForTracking(Callable<T> delegate, java.util.concurrent.atomic.AtomicReference<Future<?>> futureRef) {
        return () -> {
            currentRunningFuture = futureRef.get();
            try {
                return delegate.call();
            } finally {
                // Only clear if we still point at ourselves.
                if (currentRunningFuture == futureRef.get()) {
                    currentRunningFuture = null;
                }
            }
        };
    }

    private Runnable wrapRunnableForTracking(Runnable delegate, java.util.concurrent.atomic.AtomicReference<Future<?>> futureRef) {
        return () -> {
            currentRunningFuture = futureRef.get();
            try {
                delegate.run();
            } finally {
                if (currentRunningFuture == futureRef.get()) {
                    currentRunningFuture = null;
                }
            }
        };
    }

    /**
     * Convenience wrapper to attach a human-friendly label to an arbitrary Callable.
     * <p>
     * This is the easiest way to opt-in to better queue logging while still using lambdas.
     * </p>
     */
    public static <T> Callable<T> namedCallable(String description, Callable<T> delegate) {
        return new LabeledCallable<>(description, delegate);
    }

    /** Simple wrapper that provides a debug label for logging. */
    private static final class LabeledCallable<T> implements Callable<T>, DebugDescribable {
        private final String description;
        private final Callable<T> delegate;

        private LabeledCallable(String description, Callable<T> delegate) {
            this.description = description;
            this.delegate = delegate;
        }

        @Override
        public String getDebugDescription() {
            return description;
        }

        @Override
        public T call() throws Exception {
            return delegate.call();
        }
    }

    /**
     * Best-effort label for the currently running job.
     * <p>
     * This is only as good as the descriptions provided to {@link #submitCallable(Callable)}
     * (ideally via {@link #namedCallable(String, Callable)}).
     * </p>
     */
    public String getCurrentRunningJobLabel() {
        try {
            Future<?> f = currentRunningFuture;
            if (f == null) {
                return "";
            }
            String label = jobLabels.get(f);
            return label == null ? "" : label;
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Attempts to infer the scenario name from the currently running job label.
     * <p>
     * Returns "" if it can't infer anything.
     * </p>
     */
    public String getCurrentRunningScenarioNameBestEffort() {
        String label = getCurrentRunningJobLabel();
        if (label == null || label.trim().isEmpty()) {
            return "";
        }
        return inferScenarioNameFromLabel(label);
    }

    private static String inferScenarioNameFromLabel(String label) {
        try {
            String s = label;
            // Normalize separators and trim.
            s = s.replace('/', '\\');

            // If a configuration file path was included, infer from it.
            int idxCfg = s.toLowerCase().indexOf("configuration_");
            if (idxCfg >= 0) {
                int start = idxCfg + "configuration_".length();
                int end = s.toLowerCase().indexOf(".xml", start);
                if (end > start) {
                    return s.substring(start, end).replace("_archive", "").trim();
                }
            }

            // If the label contains "scenario" wording, try to capture what's after it.
            String low = s.toLowerCase();
            int idx = low.indexOf("scenario");
            if (idx >= 0) {
                String tail = s.substring(idx + "scenario".length());
                // common patterns: "scenario=NAME", "scenario: NAME", "scenario NAME"
                tail = tail.replace("=", " ").replace(":", " ").trim();
                if (!tail.isEmpty()) {
                    String[] toks = tail.split("\\s+");
                    if (toks.length > 0) {
                        String candidate = toks[0].trim();
                        // strip punctuation
                        candidate = candidate.replaceAll("^[\\(\\[]+", "").replaceAll("[\\)\\],;]+$", "");
                        if (!candidate.isEmpty()) {
                            return candidate;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    /**
     * Best-effort set of external processes started by this ExecutionThread.
     * <p>
     * This lets the UI request termination of spawned processes (e.g., ModelInterface).
     * Tasks are expected to remove their handle when they finish.
     * </p>
     */
    private final List<ProcessRunner.RunHandle> activeProcessHandles = new CopyOnWriteArrayList<>();

    /** Add a process handle to the active set (best-effort). */
    private void trackProcessHandle(ProcessRunner.RunHandle handle) {
        if (handle == null) {
            return;
        }
        try {
            activeProcessHandles.add(handle);
        } catch (Throwable ignored) {}
    }

    /** Remove a process handle from the active set (best-effort). */
    private void untrackProcessHandle(ProcessRunner.RunHandle handle) {
        if (handle == null) {
            return;
        }
        try {
            activeProcessHandles.remove(handle);
        } catch (Throwable ignored) {}
    }

    /**
     * Best-effort: stop all currently-running external processes spawned by this ExecutionThread
     * and cancel any queued jobs.
     * <p>
     * This is intentionally aggressive and is meant for "Stop ModelInterface Jobs" style UI actions.
     * </p>
     *
     * @return summary string suitable for logging.
     */
    public String stopAllSpawnedProcessesAndClearQueue() {
        int nHandles = 0;
        int nStopAttempted = 0;
        int nStopped = 0;

        // Snapshot to avoid concurrent modification while tasks finish.
        List<ProcessRunner.RunHandle> snapshot = new ArrayList<>();
        try {
            snapshot.addAll(activeProcessHandles);
        } catch (Throwable ignored) {}

        nHandles = snapshot.size();
        for (ProcessRunner.RunHandle h : snapshot) {
            if (h == null) continue;
            nStopAttempted++;
            try {
                ProcessRunner.StopResult r = h.stop();
                // Count as stopped if we at least attempted and it isn't still alive.
                if (r != null && !r.isAliveAfterStop()) {
                    nStopped++;
                }
            } catch (Throwable ignored) {
                // Still count the attempt.
            } finally {
                untrackProcessHandle(h);
            }
        }

        int cancelled = 0;
        try {
            cancelled = cancelQueuedJobsKeepRunningCurrent();
        } catch (Throwable ignored) {}

        return "stopAllSpawnedProcessesAndClearQueue: handles=" + nHandles
                + ", stopAttempts=" + nStopAttempted
                + ", stopped=" + nStopped
                + ", cancelledQueued=" + cancelled;
    }
}