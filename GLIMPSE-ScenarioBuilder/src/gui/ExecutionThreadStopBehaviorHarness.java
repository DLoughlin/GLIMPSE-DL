package gui;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tiny harness to sanity-check ExecutionThread cancellation semantics.
 *
 * This is NOT a JUnit test; it’s a self-contained main() you can run in Eclipse.
 *
 * Expected behavior:
 *  - Cancel queued jobs should keep the first running job alive.
 *  - Canceling the "current" future is dangerous because it interrupts the executor thread and can prevent queued tasks from running.
 */
public class ExecutionThreadStopBehaviorHarness {

    public static void main(String[] args) throws Exception {
        ExecutionThread et = new ExecutionThread();
        et.startUpExecutorSingle();

        final CountDownLatch job1Started = new CountDownLatch(1);
        final CountDownLatch allowJob1ToFinish = new CountDownLatch(1);
        final CountDownLatch job2Ran = new CountDownLatch(1);

        Future<String> job1 = et.submitCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                job1Started.countDown();
                // Simulate long-ish work.
                allowJob1ToFinish.await(5, TimeUnit.SECONDS);
                return "job1";
            }
        });

        Future<String> job2 = et.submitCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                job2Ran.countDown();
                return "job2";
            }
        });

        // Wait until job1 is running.
        job1Started.await(2, TimeUnit.SECONDS);

        // This should cancel job2 but keep job1 alive.
        int cancelled = et.cancelQueuedJobsKeepRunningCurrent();
        System.out.println("cancelQueuedJobsKeepRunningCurrent cancelled=" + cancelled);
        System.out.println("job1: done=" + job1.isDone() + ", cancelled=" + job1.isCancelled());
        System.out.println("job2: done=" + job2.isDone() + ", cancelled=" + job2.isCancelled());

        // Let job1 finish.
        allowJob1ToFinish.countDown();
        System.out.println("job1 result=" + job1.get(2, TimeUnit.SECONDS));

        // job2 should NOT have run.
        System.out.println("job2Ran=" + (job2Ran.getCount() == 0));

        et.shutdown();
        System.out.println("DONE");
    }
}
