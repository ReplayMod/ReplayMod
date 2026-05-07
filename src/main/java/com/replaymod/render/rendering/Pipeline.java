package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
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
import java.util.concurrent.atomic.AtomicLong;

import static com.replaymod.core.versions.MCVer.getMinecraft;
import static com.replaymod.render.ReplayModRender.LOGGER;

public class Pipeline<R extends Frame, P extends Frame> implements RenderPipeline {

    private final RenderSettings settings;
    private final WorldRenderer worldRenderer;
    private final FrameCapturer<R> capturer;
    private final FrameProcessor<R, P> processor;
    private final GlToAbsoluteDepthProcessor depthProcessor;
    private final FrameConsumer<P> consumer;

    private volatile boolean abort;
    private final AtomicLong captureCalls = new AtomicLong();
    private final AtomicLong capturedFrames = new AtomicLong();
    private final AtomicLong captureNanos = new AtomicLong();
    private final AtomicLong processedFrames = new AtomicLong();
    private final AtomicLong processNanos = new AtomicLong();
    private final AtomicLong consumeNanos = new AtomicLong();

    public Pipeline(RenderSettings settings, WorldRenderer worldRenderer, FrameCapturer<R> capturer, FrameProcessor<R, P> processor, FrameConsumer<P> consumer) {
        this.settings = settings;
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
        long pipelineStartNanos = System.nanoTime();
        int processThreads = settings.getRenderWorkerThreadCount();
        ExecutorService processService = new ThreadPoolExecutor(processThreads, processThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(Math.max(2, processThreads)) {
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
            long captureStartNanos = System.nanoTime();
            Map<Channel, R> rawFrame = capturer.process();
            captureCalls.incrementAndGet();
            captureNanos.addAndGet(System.nanoTime() - captureStartNanos);
            if (rawFrame != null) {
                capturedFrames.incrementAndGet();
                processService.submit(new ProcessTask(rawFrame));
            }
        }

        processService.shutdown();
        try {
            processService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logPipelineBenchmark(pipelineStartNanos, System.nanoTime(), processThreads);

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

    private void logPipelineBenchmark(long pipelineStartNanos, long pipelineEndNanos, int processThreads) {
        long captures = capturedFrames.get();
        long processed = processedFrames.get();
        double wallSeconds = nanosToSeconds(Math.max(1, pipelineEndNanos - pipelineStartNanos));
        double videoSeconds = processed / (double) settings.getFramesPerSecond();
        long totalCaptureNanos = captureNanos.get();
        long totalProcessNanos = processNanos.get();
        long totalConsumeNanos = consumeNanos.get();
        LOGGER.info("Render pipeline benchmark: frames={}, captureCalls={}, workers={}, wall={}, videoTime={}, realtime={}x, captureAvg={}, processAvg={}, consumeAvg={}, captureTotal={}, processWorkerTotal={}, consumeWorkerTotal={}",
                processed,
                captureCalls.get(),
                processThreads,
                formatSeconds(wallSeconds),
                formatSeconds(videoSeconds),
                formatDecimal(videoSeconds / wallSeconds),
                formatMillis(totalCaptureNanos / Math.max(1L, captureCalls.get())),
                formatMillis(totalProcessNanos / Math.max(1L, captures)),
                formatMillis(totalConsumeNanos / Math.max(1L, processed)),
                formatSeconds(nanosToSeconds(totalCaptureNanos)),
                formatSeconds(nanosToSeconds(totalProcessNanos)),
                formatSeconds(nanosToSeconds(totalConsumeNanos)));
    }

    private static double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000.0;
    }

    private static String formatMillis(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3fms", nanos / 1_000_000.0);
    }

    private static String formatSeconds(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.3fs", seconds);
    }

    private static String formatDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
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
                long processStartNanos = System.nanoTime();
                for (Map.Entry<Channel, R> entry : rawChannels.entrySet()) {
                    P processedFrame = processor.process(entry.getValue());
                    if (entry.getKey() == Channel.DEPTH && processedFrame instanceof BitmapFrame) {
                        depthProcessor.process((BitmapFrame) processedFrame);
                    }
                    processedChannels.put(entry.getKey(), processedFrame);
                }
                processNanos.addAndGet(System.nanoTime() - processStartNanos);
                if (processedChannels.isEmpty()) {
                    return;
                }
                long consumeStartNanos = System.nanoTime();
                consumer.consume(processedChannels);
                consumeNanos.addAndGet(System.nanoTime() - consumeStartNanos);
                processedFrames.incrementAndGet();
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
