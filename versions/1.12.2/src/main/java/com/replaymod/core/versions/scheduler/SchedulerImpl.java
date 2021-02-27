package com.replaymod.core.versions.scheduler;

import com.google.common.util.concurrent.ListenableFutureTask;
import com.replaymod.core.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//#if MC>=10809
import net.minecraftforge.common.MinecraftForge;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif

//#if MC<10800
//$$ import java.util.ArrayDeque;
//#endif

public class SchedulerImpl implements  Scheduler {
    private static final Minecraft mc = Minecraft.getMinecraft();

    //#if MC>=10809
    private static final EventBus FML_BUS = MinecraftForge.EVENT_BUS;
    //#else
    //$$ private static final EventBus FML_BUS = FMLCommonHandler.instance().bus();
    //#endif

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        if (mc.isCallingFromMinecraftThread()) {
            runnable.run();
        } else {
            FutureTask<Void> future = new FutureTask<>(runnable, null);
            runLater(future);
            future.get(30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        runLater(runnable);
    }

    /**
     * Set when the currently running code has been scheduled by runLater.
     * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
     * processed, otherwise a livelock may occur.
     */
    private boolean inRunLater = false;

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        runLater(() -> runLaterWithoutLock(runnable), runnable);
    }

    public void runLater(Runnable runnable) {
        runLater(runnable, () -> runLater(runnable));
    }

    private void runLater(Runnable runnable, Runnable defer) {
        if (mc.isCallingFromMinecraftThread() && inRunLater) {
            //#if MC>=10800
            FML_BUS.register(new Object() {
                @SubscribeEvent
                public void onRenderTick(TickEvent.RenderTickEvent event) {
                    if (event.phase == TickEvent.Phase.START) {
                        FML_BUS.unregister(this);
                        defer.run();
                    }
                }
            });
            //#else
            //$$ FML_BUS.register(new RunLaterHelper(defer));
            //#endif
            return;
        }
        //#if MC>=10800
        Queue<FutureTask<?>> tasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (tasks) {
        //#else
        //$$ Queue<ListenableFutureTask<?>> tasks = scheduledTasks;
        //$$ synchronized (scheduledTasks) {
        //#endif
            tasks.add(ListenableFutureTask.create(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } catch (ReportedException e) {
                    e.printStackTrace();
                    System.err.println(e.getCrashReport().getCompleteReport());
                    mc.crashed(e.getCrashReport());
                } finally {
                    inRunLater = false;
                }
            }, null));
        }
    }

    //#if MC>=10800
    @Override
    public void runTasks() {
    }
    //#else
    //$$ // 1.7.10: Cannot use MC's because it is processed only during ticks (so not at all when replay is paused)
    //$$ private final Queue<ListenableFutureTask<?>> scheduledTasks = new ArrayDeque<>();
    //$$
    //$$ // in 1.7.10 apparently events can't be delivered to anonymous classes
    //$$ public class RunLaterHelper {
    //$$     private final Runnable defer;
    //$$
    //$$     private RunLaterHelper(Runnable defer) {
    //$$         this.defer = defer;
    //$$     }
    //$$
    //$$     @SubscribeEvent
    //$$     public void onRenderTick(TickEvent.RenderTickEvent event) {
    //$$         if (event.phase == TickEvent.Phase.START) {
    //$$             FML_BUS.unregister(this);
    //$$             defer.run();
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public void runTasks() {
    //$$     synchronized (scheduledTasks) {
    //$$         while (!scheduledTasks.isEmpty()) {
    //$$             scheduledTasks.poll().run();
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
