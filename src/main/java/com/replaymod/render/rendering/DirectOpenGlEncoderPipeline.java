package com.replaymod.render.rendering;

import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.SimpleOpenGlTextureFrameCapturer;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.OpenGlTextureFrame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.replaymod.render.ReplayModRender.LOGGER;

public class DirectOpenGlEncoderPipeline implements RenderPipeline {
    private final RenderSettings settings;
    private final WorldRenderer worldRenderer;
    private final SimpleOpenGlTextureFrameCapturer capturer;
    private final FrameConsumer<OpenGlTextureFrame> consumer;
    private volatile boolean abort;
    private final AtomicLong captureCalls = new AtomicLong();
    private final AtomicLong capturedFrames = new AtomicLong();
    private final AtomicLong captureNanos = new AtomicLong();
    private final AtomicLong consumeNanos = new AtomicLong();

    public DirectOpenGlEncoderPipeline(
            RenderSettings settings,
            WorldRenderer worldRenderer,
            SimpleOpenGlTextureFrameCapturer capturer,
            FrameConsumer<OpenGlTextureFrame> consumer
    ) {
        this.settings = settings;
        this.worldRenderer = worldRenderer;
        this.capturer = capturer;
        this.consumer = consumer;
    }

    @Override
    public synchronized void run() {
        long pipelineStartNanos = System.nanoTime();
        MinecraftClient mc = MCVer.getMinecraft();
        try {
            while (!capturer.isDone() && !abort) {
                if (GLFW.glfwWindowShouldClose(mc.getWindow().getHandle())
                        || ((MinecraftAccessor) mc).getCrashReporter() != null) {
                    return;
                }
                long captureStartNanos = System.nanoTime();
                Map<Channel, OpenGlTextureFrame> rawFrame = capturer.process();
                captureCalls.incrementAndGet();
                captureNanos.addAndGet(System.nanoTime() - captureStartNanos);
                if (abort) {
                    break;
                }
                if (rawFrame != null) {
                    capturedFrames.incrementAndGet();
                    long consumeStartNanos = System.nanoTime();
                    consumer.consume(rawFrame);
                    consumeNanos.addAndGet(System.nanoTime() - consumeStartNanos);
                }
            }

            logPipelineBenchmark(pipelineStartNanos, System.nanoTime());
            worldRenderer.close();
            capturer.close();
            consumer.close();
        } catch (Throwable t) {
            CrashReport crashReport = CrashReport.create(t, "Running native OpenGL texture rendering pipeline");
            throw new CrashException(crashReport);
        }
    }

    private void logPipelineBenchmark(long pipelineStartNanos, long pipelineEndNanos) {
        long frames = capturedFrames.get();
        double wallSeconds = nanosToSeconds(Math.max(1, pipelineEndNanos - pipelineStartNanos));
        double videoSeconds = frames / (double) settings.getFramesPerSecond();
        LOGGER.info("Native OpenGL texture pipeline benchmark: frames={}, captureCalls={}, wall={}, videoTime={}, realtime={}x, captureAvg={}, consumeAvg={}, captureTotal={}, consumeTotal={}",
                frames,
                captureCalls.get(),
                formatSeconds(wallSeconds),
                formatSeconds(videoSeconds),
                formatDecimal(videoSeconds / wallSeconds),
                formatMillis(captureNanos.get() / Math.max(1L, captureCalls.get())),
                formatMillis(consumeNanos.get() / Math.max(1L, frames)),
                formatSeconds(nanosToSeconds(captureNanos.get())),
                formatSeconds(nanosToSeconds(consumeNanos.get())));
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

    @Override
    public void cancel() {
        abort = true;
    }
}
