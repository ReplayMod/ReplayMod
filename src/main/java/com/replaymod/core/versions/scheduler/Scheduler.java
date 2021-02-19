package com.replaymod.core.versions.scheduler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Scheduler {
    /**
     * Execute the given runnable on the main client thread, returning only after it has been run (or after 30 seconds).
     */
    void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Execute the given runnable after game has started (once the overlay has been closed).
     * Most importantly, it will run after resources (including language keys!) have been loaded.
     * Below 1.14, this is equivalent to {@link #runLater(Runnable)}.
     */
    void runPostStartup(Runnable runnable);

    /**
     * Pre-1.14 MC would hold the lock on the scheduledTasks queue while executing its tasks
     * such that no new tasks could be submitted while one of them was running.
     * This would cause issues with long-running tasks (e.g. video rendering) as it would
     * block all async tasks (e.g. skin loading).
     */
    void runLaterWithoutLock(Runnable runnable);

    void runLater(Runnable runnable);

    void runTasks();
}
