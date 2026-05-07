package com.replaymod.replay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReplayExecutors {
    private static final AtomicInteger REPLAY_WORKER_ID = new AtomicInteger();
    private static final AtomicInteger IO_WORKER_ID = new AtomicInteger();

    // THREADING: shared daemon pool for CPU-bound replay work; lives for the mod process.
    public static final ForkJoinPool REPLAY_POOL = new ForkJoinPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
            pool -> {
                ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName("replaymod-worker-" + REPLAY_WORKER_ID.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            null,
            false
    );

    // THREADING: shared daemon pool for IO-bound replay work; lives for the mod process.
    public static final ExecutorService IO_POOL = Executors.newFixedThreadPool(
            Math.min(8, Runtime.getRuntime().availableProcessors()),
            runnable -> {
                Thread thread = new Thread(runnable, "replaymod-io-" + IO_WORKER_ID.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
    );

    private ReplayExecutors() {
    }

    public static void shutdown() {
        REPLAY_POOL.shutdown();
        IO_POOL.shutdown();
    }
}
