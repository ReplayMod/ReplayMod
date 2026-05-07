package com.replaymod.render;

import com.replaymod.render.frame.OpenGlTextureFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.rendering.VideoRenderer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static com.replaymod.render.ReplayModRender.LOGGER;

public class NativeOpenGlEncoder implements FrameConsumer<OpenGlTextureFrame> {
    private static final String LIBRARY_PROPERTY = "replaymod.nativeEncoder.library";
    private static final String ENABLE_PROPERTY = "replaymod.nativeEncoder";
    private static final String REQUIRED_PROPERTY = "replaymod.nativeEncoder.required";
    private static final String ENABLE_ENV = "REPLAYMOD_NATIVE_ENCODER";
    private static final String REQUIRED_ENV = "REPLAYMOD_NATIVE_ENCODER_REQUIRED";
    private static final String LIBRARY_ENV = "REPLAYMOD_NATIVE_ENCODER_LIBRARY";
    private static final String DEFAULT_LIBRARY = "replaymod_native_encoder";

    private static boolean loadAttempted;
    private static boolean loaded;
    private static Throwable loadFailure;

    private final VideoRenderer renderer;
    private final RenderSettings settings;
    private final long handle;
    private final long startedNanos = System.nanoTime();
    private long framesWritten;
    private long encodeNanos;
    private long maxEncodeNanos;
    private boolean closed;

    public static boolean shouldUse(RenderSettings settings) {
        if (settings.getRenderMethod() != RenderSettings.RenderMethod.DEFAULT
                || settings.isDepthMap()
                || settings.getAntiAliasing() != RenderSettings.AntiAliasing.NONE
                || settings.getEncodingPreset() != RenderSettings.EncodingPreset.MP4_HARDWARE) {
            return false;
        }
        if (settings.getExportArguments() != null
                && !settings.getExportArguments().equals(RenderSettings.EncodingPreset.MP4_HARDWARE.getValue())) {
            return false;
        }

        String mode = System.getProperty(ENABLE_PROPERTY);
        if (mode == null || mode.trim().isEmpty()) {
            mode = System.getenv(ENABLE_ENV);
        }
        if (mode != null && !mode.trim().isEmpty()) {
            return Boolean.parseBoolean(mode);
        }
        // On hybrid systems where the OpenGL renderer is not NVIDIA, the native NVENC
        // path will always fail in CUDA<->GL interop. Skip it silently so we go through
        // the FFmpeg path, which can still use VAAPI or NVENC via PCI on some setups.
        if (!isLikelyNvidiaGlRenderer()) {
            LOGGER.info("Native OpenGL/NVENC encoder skipped: OpenGL renderer is not NVIDIA. " +
                    "Set -Dreplaymod.nativeEncoder=true to force the attempt.");
            return false;
        }
        return true;
    }

    private static boolean isLikelyNvidiaGlRenderer() {
        try {
            String renderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
            String vendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
            String combined = ((renderer == null ? "" : renderer) + " " + (vendor == null ? "" : vendor))
                    .toLowerCase(Locale.ROOT);
            if (combined.isEmpty()) {
                // Could not query the renderer (no GL context yet); be permissive and let the native
                // path try. The C++ side will probe cuGLGetDevices and bail out cleanly if needed.
                return true;
            }
            return combined.contains("nvidia") || combined.contains("geforce") || combined.contains("quadro")
                    || combined.contains("rtx") || combined.contains("gtx") || combined.contains("tesla");
        } catch (Throwable t) {
            return true;
        }
    }

    public static NativeOpenGlEncoder createIfAvailable(VideoRenderer renderer) throws UnavailableException {
        RenderSettings settings = renderer.getRenderSettings();
        if (!shouldUse(settings)) {
            return null;
        }
        if (!loadNativeLibrary()) {
            handleUnavailable(settings, "Native OpenGL/NVENC encoder library is unavailable", loadFailure);
            return null;
        }
        try {
            return new NativeOpenGlEncoder(renderer);
        } catch (Throwable t) {
            handleUnavailable(settings, "Failed to initialize native OpenGL/NVENC encoder", t);
            return null;
        }
    }

    private static void handleUnavailable(RenderSettings settings, String message, Throwable cause) throws UnavailableException {
        if (isRequired(settings)) {
            throw new UnavailableException(buildRequiredMessage(settings, message, cause), cause);
        }
        LOGGER.info("{}; falling back to FFmpeg rawvideo stdin path. Cause: {}",
                message, cause == null ? "unknown" : cause.toString());
    }

    private static boolean isRequired(RenderSettings settings) {
        String explicit = System.getProperty(REQUIRED_PROPERTY);
        if (explicit == null || explicit.trim().isEmpty()) {
            explicit = System.getenv(REQUIRED_ENV);
        }
        if (explicit != null && !explicit.trim().isEmpty()) {
            return Boolean.parseBoolean(explicit);
        }
        return false;
    }

    private static String buildRequiredMessage(RenderSettings settings, String message, Throwable cause) {
        return message + ". Native encoding was explicitly required for this render. "
                + "Install replaymod_native_encoder, disable replaymod.nativeEncoder.required, or use the FFmpeg fallback. Cause: "
                + (cause == null ? "unknown" : cause.toString());
    }

    private static long rawInputBytesPerSecond(RenderSettings settings) {
        return (long) settings.getVideoWidth()
                * (long) settings.getVideoHeight()
                * 4L
                * (long) settings.getFramesPerSecond();
    }

    private NativeOpenGlEncoder(VideoRenderer renderer) throws IOException {
        this.renderer = renderer;
        this.settings = renderer.getRenderSettings();

        File outputFolder = settings.getOutputFile().getParentFile();
        if (outputFolder != null && !outputFolder.isDirectory() && !outputFolder.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputFolder);
        }

        handle = nativeOpen(
                settings.getOutputFile().getAbsolutePath(),
                settings.getVideoWidth(),
                settings.getVideoHeight(),
                settings.getFramesPerSecond(),
                settings.getBitRate(),
                true);
        if (handle == 0) {
            throw new IOException("Native encoder returned a null handle.");
        }
        LOGGER.info("Using native OpenGL texture NVENC path: resolution={}x{}, fps={}, targetBitrate={}/s",
                settings.getVideoWidth(), settings.getVideoHeight(), settings.getFramesPerSecond(),
                formatBytes(settings.getBitRate() / 8L));
    }

    private static synchronized boolean loadNativeLibrary() {
        if (loadAttempted) {
            return loaded;
        }
        loadAttempted = true;
        String explicit = System.getProperty(LIBRARY_PROPERTY);
        if (explicit == null || explicit.trim().isEmpty()) {
            explicit = System.getenv(LIBRARY_ENV);
        }
        try {
            if (explicit != null && !explicit.trim().isEmpty()) {
                System.load(explicit);
            } else {
                System.loadLibrary(DEFAULT_LIBRARY);
            }
            loaded = true;
        } catch (Throwable t) {
            loadFailure = t;
            loaded = false;
        }
        return loaded;
    }

    @Override
    public void consume(Map<Channel, OpenGlTextureFrame> channels) {
        OpenGlTextureFrame frame = channels.get(Channel.BRGA);
        if (frame == null || closed) {
            return;
        }
        try {
            long startNanos = System.nanoTime();
            nativeEncode(handle, frame.getFrameId(), frame.getTextureTarget(), frame.getTextureId());
            long elapsed = System.nanoTime() - startNanos;
            framesWritten++;
            encodeNanos += elapsed;
            maxEncodeNanos = Math.max(maxEncodeNanos, elapsed);
        } catch (Throwable t) {
            renderer.setFailure(t);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            nativeClose(handle);
        } finally {
            logFinalBenchmark();
        }
    }

    public void abort() {
        if (!closed) {
            closed = true;
            try {
                nativeAbort(handle);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isParallelCapable() {
        return false;
    }

    private void logFinalBenchmark() {
        if (framesWritten == 0) {
            LOGGER.info("Native OpenGL/NVENC benchmark finished: no frames were encoded.");
            return;
        }
        double wallSeconds = Math.max(1, System.nanoTime() - startedNanos) / 1_000_000_000.0;
        double videoSeconds = framesWritten / (double) settings.getFramesPerSecond();
        LOGGER.info("Native OpenGL/NVENC benchmark finished: frames={}, videoTime={}, wall={}, realtime={}x, avgEncode={}, maxEncode={}",
                framesWritten,
                formatSeconds(videoSeconds),
                formatSeconds(wallSeconds),
                formatDecimal(videoSeconds / wallSeconds),
                formatMillis(encodeNanos / Math.max(1L, framesWritten)),
                formatMillis(maxEncodeNanos));
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.3fms", nanos / 1_000_000.0);
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.3fs", seconds);
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatBytes(long bytes) {
        double value = bytes;
        String[] units = {"B", "KiB", "MiB", "GiB"};
        int unit = 0;
        while (value >= 1024.0 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return String.format(Locale.ROOT, "%.2f%s", value, units[unit]);
    }

    private static native long nativeOpen(String outputFile, int width, int height, int fps, int bitrate, boolean flipVertical) throws IOException;

    private static native void nativeEncode(long handle, int frameId, int textureTarget, int textureId) throws IOException;

    private static native void nativeClose(long handle) throws IOException;

    private static native void nativeAbort(long handle);

    public static class UnavailableException extends IOException {
        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
