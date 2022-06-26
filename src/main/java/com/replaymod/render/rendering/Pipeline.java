package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.processor.GlToAbsoluteDepthProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class Pipeline<R extends Frame, P extends Frame> implements Runnable {

    private final WorldRenderer worldRenderer;
    private final FrameCapturer<R> capturer;
    private final FrameProcessor<R, P> processor;
    private final GlToAbsoluteDepthProcessor depthProcessor;
    private final FrameConsumer<P> consumer;

    private volatile boolean abort;

    public Pipeline(WorldRenderer worldRenderer, FrameCapturer<R> capturer, FrameProcessor<R, P> processor, FrameConsumer<P> consumer) {
        this.worldRenderer = worldRenderer;
        this.capturer = capturer;
        this.processor = processor;
        this.consumer = new ParallelSafeConsumer<>(consumer);

        float near = 0.05f;
        float far = getMinecraft().options.viewDistance * 16 * 4;
        this.depthProcessor = new GlToAbsoluteDepthProcessor(near, far);
    }

    @Override
    public synchronized void run() {
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
        while (!capturer.isDone() && !abort) {
            if (GLFW.glfwWindowShouldClose(mc.getWindow().getHandle()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
                processService.shutdown();
                return;
            }
            Map<Channel, R> rawFrame = capturer.process();
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
            worldRenderer.close();
            capturer.close();
            processor.close();
            consumer.close();
        } catch (Throwable t) {
            CrashReport crashReport = CrashReport.create(t, "Cleaning up rendering pipeline");
            throw new CrashException(crashReport);
        }
    }

    public void cancel() {
        abort = true;
    }

    private class ProcessTask implements Runnable {
        private final Map<Channel, R> rawChannels;

        public ProcessTask(Map<Channel, R> rawChannels) {
            this.rawChannels = rawChannels;
        }

        @Override
        public void run() {
            try {
                Map<Channel, P> processedChannels = new HashMap<>();
                for (Map.Entry<Channel, R> entry : rawChannels.entrySet()) {
                    P processedFrame = processor.process(entry.getValue());
                    if (entry.getKey() == Channel.DEPTH && processedFrame instanceof BitmapFrame) {
                        depthProcessor.process((BitmapFrame) processedFrame);
                    }
                    processedChannels.put(entry.getKey(), processedFrame);
                }
                if (processedChannels.isEmpty()) {
                    return;
                }
                consumer.consume(processedChannels);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.create(t, "Processing frame");
                MCVer.getMinecraft().setCrashReport(crashReport);
            }
        }
    }

    private static class ParallelSafeConsumer<P extends Frame> implements FrameConsumer<P> {
        private final FrameConsumer<P> inner;

        private int nextFrame;
        private final Object lock = new Object();

        private ParallelSafeConsumer(FrameConsumer<P> inner) {
            this.inner = inner;
        }

        @Override
        public void consume(Map<Channel, P> channels) {
            if (inner.isParallelCapable()) {
                inner.consume(channels);
            } else {
                int frameId = channels.values().iterator().next().getFrameId();
                synchronized (lock) {
                    while (nextFrame != frameId) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    inner.consume(channels);
                    nextFrame++;
                    lock.notifyAll();
                }
            }
        }

        @Override
        public boolean isParallelCapable() {
            return true;
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }
}
