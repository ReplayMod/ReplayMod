package com.replaymod.core.versions.scheduler;

import com.replaymod.core.mixin.MinecraftAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.thread.ReentrantThreadExecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//#if MC>=12100
//$$ import net.minecraft.util.crash.ReportType;
//#endif

public class SchedulerImpl implements  Scheduler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        if (mc.isOnThread()) {
            runnable.run();
        } else {
            executor.submit(() -> {
                runnable.run();
                return null;
            }).get(30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        runLater(new Runnable() {
            @Override
            public void run() {
                if (mc.getOverlay() != null) {
                    // delay until after resources have been loaded
                    runLater(this);
                    return;
                }
                runnable.run();
            }
        });
    }

    /**
     * Set when the currently running code has been scheduled by runLater.
     * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
     * processed, otherwise a livelock may occur.
     */
    private boolean inRunLater = false;
    private boolean inRenderTaskQueue = false;
    // Starting 1.14 MC clears the queue of scheduled tasks on disconnect.
    // This works fine for MC since it uses the queue only for packet handling but breaks our assumption that
    // stuff submitted via runLater is actually always run (e.g. recording might not be fully stopped because parts
    // of that are run via runLater and stopping the recording happens right around the time MC clears the queue).
    // Luckily, that's also the version where MC pulled out the executor implementation, so we can just spin up our own.
    public static class ReplayModExecutor extends ReentrantThreadExecutor<Runnable> {
        private final Thread mcThread = Thread.currentThread();

        private ReplayModExecutor(String string_1) {
            super(string_1);
        }

        @Override public Runnable createTask(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean canExecute(Runnable runnable) {
            return true;
        }

        @Override
        protected Thread getThread() {
            return mcThread;
        }

        @Override
        public void runTasks() {
            super.runTasks();
        }
    }
    public final ReplayModExecutor executor = new ReplayModExecutor("Client/ReplayMod");

    @Override
    public void runTasks() {
        executor.runTasks();
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        // MC 1.14+ no longer synchronizes on the queue while running its tasks
        runLater(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        runLater(runnable, () -> runLater(runnable));
    }

    private void runLater(Runnable runnable, Runnable defer) {
        if (mc.isOnThread() && inRunLater && !inRenderTaskQueue) {
            ((MinecraftAccessor) mc).getRenderTaskQueue().offer(() -> {
                inRenderTaskQueue = true;
                try {
                    defer.run();
                } finally {
                    inRenderTaskQueue = false;
                }
            });
        } else {
            executor.send(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } catch (CrashException e) {
                    e.printStackTrace();
                    //#if MC>=12100
                    //$$ System.err.println(e.getReport().asString(ReportType.MINECRAFT_CRASH_REPORT));
                    //#else
                    System.err.println(e.getReport().asString());
                    //#endif
                    mc.setCrashReport(e.getReport());
                } finally {
                    inRunLater = false;
                }
            });
        }
    }
}
