package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashException;

//#if MC>=11300
import org.lwjgl.glfw.GLFW;
//#else
//$$ import org.lwjgl.opengl.Display;
//#endif

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Pipeline<R extends Frame, P extends Frame> implements Runnable {

    private final FrameCapturer<R> capturer;
    private final FrameProcessor<R, P> processor;
    private int consumerNextFrame;
    private final Object consumerLock = new Object();
    private final FrameConsumer<P> consumer;

    private Thread runningThread;

    public Pipeline(FrameCapturer<R> capturer, FrameProcessor<R, P> processor, FrameConsumer<P> consumer) {
        this.capturer = capturer;
        this.processor = processor;
        this.consumer = consumer;
    }

    @Override
    public synchronized void run() {
        runningThread = Thread.currentThread();
        consumerNextFrame = 0;
        int processors = Runtime.getRuntime().availableProcessors();
        int processThreads = Math.max(1, processors - 2); // One processor for the main thread and one for ffmpeg, sorry OS :(
        ExecutorService processService = new ThreadPoolExecutor(processThreads, processThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(2) {
                    @Override
                    public boolean offer(Runnable runnable) {
                        try {
                            put(runnable);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        return true;
                    }
                }, new ThreadPoolExecutor.DiscardPolicy());

        MinecraftClient mc = MCVer.getMinecraft();
        while (!capturer.isDone() && !Thread.currentThread().isInterrupted()) {
            //#if MC>=11300
            if (GLFW.glfwWindowShouldClose(mc.window.getHandle()) || ((MinecraftAccessor) mc).hasCrashed()) {
            //#else
            //$$ if (Display.isCloseRequested() || ((MinecraftAccessor) mc).hasCrashed()) {
            //#endif
                Thread.currentThread().interrupt();
                return;
            }
            R rawFrame = capturer.process();
            if (rawFrame != null) {
                processService.submit(new ProcessTask(rawFrame));
            }
        }

        processService.shutdown();
        try {
            processService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            capturer.close();
            processor.close();
            consumer.close();
        } catch (Throwable t) {
            CrashReport crashReport = CrashReport.create(t, "Cleaning up rendering pipeline");
            throw new CrashException(crashReport);
        }
    }

    public void cancel() {
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }

    @RequiredArgsConstructor
    private class ProcessTask implements Runnable {
        private final R rawFrame;

        @Override
        public void run() {
            try {
                P processedFrame = processor.process(rawFrame);
                synchronized (consumerLock) {
                    while (consumerNextFrame != processedFrame.getFrameId()) {
                        try {
                            consumerLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    consumer.consume(processedFrame);
                    consumerNextFrame++;
                    consumerLock.notifyAll();
                }
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.create(t, "Processing frame");
                MCVer.getMinecraft().setCrashReport(crashReport);
            }
        }
    }
}
