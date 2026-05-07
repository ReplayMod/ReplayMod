package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.StreamPipe;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.replaymod.render.ReplayModRender.LOGGER;
import static org.apache.commons.lang3.Validate.isTrue;

public class FFmpegWriter implements FrameConsumer<BitmapFrame> {
    private static final long BENCHMARK_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final String LEGACY_MP4_CUSTOM_ARGS =
            "-y -f rawvideo -pix_fmt bgra -s %WIDTH%x%HEIGHT% -r %FPS% -i - %FILTERS%-an -c:v libx264 -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"";
    private static final String LEGACY_WEBM_CUSTOM_ARGS =
            "-y -f rawvideo -pix_fmt bgra -s %WIDTH%x%HEIGHT% -r %FPS% -i - %FILTERS%-an -c:v libvpx -b:v %BITRATE% -pix_fmt yuv420p \"%FILENAME%\"";

    private final VideoRenderer renderer;
    private final RenderSettings settings;
    private final Process process;
    private final OutputStream outputStream;
    private final String commandArgs;
    private final boolean inputNeedsVerticalFlip;
    private final int maxQueuedFrames;
    private final Object queueLock = new Object();
    private final TreeMap<Integer, Map<Channel, BitmapFrame>> queuedFrames = new TreeMap<>();
    private final Thread writerThread;
    private byte[] writeBuffer;
    private volatile boolean aborted;
    private volatile Throwable writerFailure;
    private boolean inputClosed;
    private int nextFrameToWrite;
    private final long startedNanos = System.nanoTime();
    private long firstFrameNanos = -1;
    private long lastFrameNanos = -1;
    private long framesWritten;
    private long bytesWritten;
    private long writeNanos;
    private long maxFrameWriteNanos;
    private long writeCalls;
    private long lastBenchmarkLogNanos;
    private long enqueueWaitNanos;
    private int maxQueueDepth;
    private boolean loggedBufferMode;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

    public FFmpegWriter(final VideoRenderer renderer) throws IOException {
        this(renderer, false);
    }

    public FFmpegWriter(final VideoRenderer renderer, boolean inputNeedsVerticalFlip) throws IOException {
        this.renderer = renderer;
        this.settings = renderer.getRenderSettings();
        this.inputNeedsVerticalFlip = inputNeedsVerticalFlip;

        File outputFolder = settings.getOutputFile().getParentFile();
        FileUtils.forceMkdir(outputFolder);
        String fileName = settings.getOutputFile().getName();

        String executable = settings.getExportCommandOrDefault();
        commandArgs = buildCommandArgs(executable, fileName);
        LOGGER.info("Starting {} with args: {}", executable, commandArgs);
        String[] cmdline;
        try {
            cmdline = new CommandLine(executable).addArguments(commandArgs, false).toStrings();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse ffmpeg command line:", e);
            throw new FFmpegStartupException(settings, e.getLocalizedMessage());
        }
        try {
            process = new ProcessBuilder(cmdline).directory(outputFolder).start();
        } catch (IOException e) {
            throw new NoFFmpegException(e);
        }
        File exportLogFile = new File(MCVer.getMinecraft().runDirectory, "export.log");
        OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream(exportLogFile), ffmpegLog);
        new StreamPipe(process.getInputStream(), exportLogOut).start();
        new StreamPipe(process.getErrorStream(), exportLogOut).start();
        outputStream = process.getOutputStream();
        maxQueuedFrames = Math.max(2, Math.min(8, settings.getRenderWorkerThreadCount()));
        writerThread = new Thread(this::runWriter, "replaymod-ffmpeg-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        long rawBytesPerSecond = (long) settings.getVideoWidth()
                * (long) settings.getVideoHeight()
                * 4L
                * (long) settings.getFramesPerSecond();
        LOGGER.info("FFmpeg benchmark started: resolution={}x{}, fps={}, rawInputRate={}/s, targetBitrate={}/s",
                settings.getVideoWidth(), settings.getVideoHeight(), settings.getFramesPerSecond(),
                formatBytes(rawBytesPerSecond), formatBytes(settings.getBitRate() / 8L));
    }

    @Override
    public void close() throws IOException {
        synchronized (queueLock) {
            inputClosed = true;
            queueLock.notifyAll();
        }
        try {
            writerThread.join(TimeUnit.SECONDS.toMillis(60));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (writerThread.isAlive()) {
            aborted = true;
            IOUtils.closeQuietly(outputStream);
            writerThread.interrupt();
            try {
                writerThread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("FFmpeg writer thread did not finish cleanly; forcing FFmpeg stdin closed.");
        }
        IOUtils.closeQuietly(outputStream);

        long startTime = System.nanoTime();
        long rem = TimeUnit.SECONDS.toNanos(30);
        boolean exited = false;
        int exitCode = Integer.MIN_VALUE;
        do {
            try {
                exitCode = process.exitValue();
                exited = true;
                break;
            } catch(IllegalThreadStateException ex) {
                if (rem > 0) {
                    try {
                        Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            rem = TimeUnit.SECONDS.toNanos(30) - (System.nanoTime() - startTime);
        } while (rem > 0);

        if (!exited) {
            process.destroy();
        }
        logFinalBenchmark(System.nanoTime(), System.nanoTime() - startTime, exited, exitCode);
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame frame = channels.get(Channel.BRGA);
        try {
            checkSize(frame.getSize());
            enqueueFrame(frame.getFrameId(), channels);
        } catch (Throwable t) {
            if (aborted) {
                return;
            }
            try {
                // Check whether this is a failure right at the beginning of the rendering process
                // or at some later point (ffmpeg won't print the output file until the first frame
                // has been written to stdin, so we can't already check for invalid args in <init>).
                getVideoFile();
            } catch (FFmpegStartupException e) {
                // Possibly invalid ffmpeg arguments
                LOGGER.error("FFmpeg failed to start or rejected the export arguments:\n{}", e.getLog());
                renderer.setFailure(e);
                return;
            }
            renderer.setFailure(t);
            releaseFrames(channels);
        }
    }

    private void enqueueFrame(int frameId, Map<Channel, BitmapFrame> channels) throws InterruptedException, IOException {
        long waitStartNanos = 0;
        synchronized (queueLock) {
            while (!aborted && writerFailure == null && queuedFrames.size() >= maxQueuedFrames) {
                if (waitStartNanos == 0) {
                    waitStartNanos = System.nanoTime();
                }
                queueLock.wait();
            }
            if (waitStartNanos != 0) {
                enqueueWaitNanos += System.nanoTime() - waitStartNanos;
            }
            if (writerFailure != null) {
                throw new RuntimeException(writerFailure);
            }
            if (aborted || inputClosed) {
                throw new IOException("FFmpeg writer is closed.");
            }
            Map<Channel, BitmapFrame> previous = queuedFrames.put(frameId, channels);
            if (previous != null) {
                releaseFrames(previous);
                LOGGER.warn("Replacing duplicate rendered frame {} in FFmpeg queue.", frameId);
            }
            maxQueueDepth = Math.max(maxQueueDepth, queuedFrames.size());
            queueLock.notifyAll();
        }
    }

    private void runWriter() {
        try {
            while (true) {
                Map<Channel, BitmapFrame> channels = takeNextFrame();
                if (channels == null) {
                    return;
                }
                writeFrame(channels);
            }
        } catch (Throwable t) {
            if (!aborted) {
                writerFailure = t;
                renderer.setFailure(t);
            }
        } finally {
            synchronized (queueLock) {
                for (Map<Channel, BitmapFrame> channels : queuedFrames.values()) {
                    releaseFrames(channels);
                }
                queuedFrames.clear();
                queueLock.notifyAll();
            }
        }
    }

    private Map<Channel, BitmapFrame> takeNextFrame() throws InterruptedException {
        synchronized (queueLock) {
            while (true) {
                Map<Channel, BitmapFrame> channels = queuedFrames.remove(nextFrameToWrite);
                if (channels != null) {
                    nextFrameToWrite++;
                    queueLock.notifyAll();
                    return channels;
                }
                if (inputClosed && !queuedFrames.isEmpty()) {
                    int firstFrameId = queuedFrames.firstKey();
                    LOGGER.warn("FFmpeg writer expected frame {} but only frame {}+ is queued; continuing with queued frame.",
                            nextFrameToWrite, firstFrameId);
                    nextFrameToWrite = firstFrameId;
                    continue;
                }
                if ((inputClosed || aborted) && queuedFrames.isEmpty()) {
                    queueLock.notifyAll();
                    return null;
                }
                queueLock.wait();
            }
        }
    }

    private void writeFrame(Map<Channel, BitmapFrame> channels) throws IOException {
        try {
            BitmapFrame frame = channels.get(Channel.BRGA);
            ByteBuffer buffer = frame.getByteBuffer();
            int frameBytes = buffer.remaining();
            long writeStartNanos = System.nanoTime();
            if (firstFrameNanos < 0) {
                firstFrameNanos = writeStartNanos;
            }
            writeBufferToFFmpeg(buffer);
            long now = System.nanoTime();
            long frameWriteNanos = now - writeStartNanos;
            framesWritten++;
            bytesWritten += frameBytes;
            writeNanos += frameWriteNanos;
            maxFrameWriteNanos = Math.max(maxFrameWriteNanos, frameWriteNanos);
            lastFrameNanos = now;
            logProgressBenchmark(now, false);
        } finally {
            releaseFrames(channels);
        }
    }

    private void releaseFrames(Map<Channel, BitmapFrame> channels) {
        for (BitmapFrame value : channels.values()) {
            ByteBufferPool.release(value.getByteBuffer());
        }
    }

    private void writeBufferToFFmpeg(ByteBuffer buffer) throws IOException {
        int remaining = buffer.remaining();
        if (!loggedBufferMode) {
            loggedBufferMode = true;
            LOGGER.info("FFmpeg stdin write path: directBuffer={}, hasArray={}, copyBuffer={}",
                    buffer.isDirect(), buffer.hasArray(), buffer.hasArray() ? "none" : formatBytes(remaining));
        }
        if (buffer.hasArray()) {
            int position = buffer.position();
            outputStream.write(buffer.array(), buffer.arrayOffset() + position, remaining);
            buffer.position(position + remaining);
            writeCalls++;
            return;
        }
        ensureWriteBufferCapacity(remaining);
        buffer.get(writeBuffer, 0, remaining);
        outputStream.write(writeBuffer, 0, remaining);
        writeCalls++;
    }

    private void ensureWriteBufferCapacity(int size) {
        if (writeBuffer == null || writeBuffer.length < size) {
            writeBuffer = new byte[size];
        }
    }

    private void logProgressBenchmark(long nowNanos, boolean force) {
        if (framesWritten == 0) {
            return;
        }
        if (!force && nowNanos - lastBenchmarkLogNanos < BENCHMARK_LOG_INTERVAL_NANOS) {
            return;
        }
        lastBenchmarkLogNanos = nowNanos;
        long frameSpanNanos = Math.max(1, nowNanos - firstFrameNanos);
        double elapsedSeconds = nanosToSeconds(Math.max(1, nowNanos - startedNanos));
        double streamSeconds = nanosToSeconds(frameSpanNanos);
        double writeSeconds = nanosToSeconds(Math.max(1, writeNanos));
        double encodedVideoSeconds = framesWritten / (double) settings.getFramesPerSecond();
        LOGGER.info("FFmpeg benchmark: frames={}, videoTime={}, wall={}, streamFps={}, realtime={}x, rawWritten={}, avgWrite={}, maxWrite={}, writeThroughput={}/s, writeCalls={}, avgChunk={}, queueMax={}, enqueueWait={}",
                framesWritten,
                formatSeconds(encodedVideoSeconds),
                formatSeconds(elapsedSeconds),
                formatDecimal(framesWritten / streamSeconds),
                formatDecimal(encodedVideoSeconds / elapsedSeconds),
                formatBytes(bytesWritten),
                formatMillis(writeNanos / Math.max(1L, framesWritten)),
                formatMillis(maxFrameWriteNanos),
                formatBytes((long) (bytesWritten / writeSeconds)),
                writeCalls,
                formatBytes(bytesWritten / Math.max(1L, writeCalls)),
                maxQueueDepth,
                formatSeconds(nanosToSeconds(enqueueWaitNanos)));
    }

    private void logFinalBenchmark(long nowNanos, long ffmpegWaitNanos, boolean exited, int exitCode) {
        logProgressBenchmark(nowNanos, true);
        if (framesWritten == 0) {
            LOGGER.info("FFmpeg benchmark finished: no frames were written, exited={}, exitCode={}",
                    exited, exited ? String.valueOf(exitCode) : "still running after timeout");
            return;
        }
        double totalSeconds = nanosToSeconds(Math.max(1, nowNanos - startedNanos));
        double writeSeconds = nanosToSeconds(Math.max(1, writeNanos));
        double encodedVideoSeconds = framesWritten / (double) settings.getFramesPerSecond();
        LOGGER.info("FFmpeg benchmark finished: frames={}, videoTime={}, wall={}, realtime={}x, rawWritten={}, writeTime={}, writeDuty={}%, writeCalls={}, avgChunk={}, queueMax={}, enqueueWait={}, waitForFFmpeg={}, exitCode={}",
                framesWritten,
                formatSeconds(encodedVideoSeconds),
                formatSeconds(totalSeconds),
                formatDecimal(encodedVideoSeconds / totalSeconds),
                formatBytes(bytesWritten),
                formatSeconds(writeSeconds),
                formatDecimal(writeSeconds * 100.0 / totalSeconds),
                writeCalls,
                formatBytes(bytesWritten / Math.max(1L, writeCalls)),
                maxQueueDepth,
                formatSeconds(nanosToSeconds(enqueueWaitNanos)),
                formatSeconds(nanosToSeconds(ffmpegWaitNanos)),
                exited ? String.valueOf(exitCode) : "still running after timeout");
    }

    private static double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000.0;
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

    @Override
    public boolean isParallelCapable() {
        return true;
    }

    private void checkSize(ReadableDimension size) {
        checkSize(size.getWidth(), size.getHeight());
    }

    private void checkSize(int width, int height) {
        isTrue(width == settings.getVideoWidth(), "Width has to be %d but was %d", settings.getVideoWidth(), width);
        isTrue(height == settings.getVideoHeight(), "Height has to be %d but was %d", settings.getVideoHeight(), height);
    }

    public void abort() {
        aborted = true;
    }

    private String buildCommandArgs(String executable, String fileName) {
        String args = upgradeLegacyPresetArguments(settings.getExportArguments());
        String videoFilters = getEffectiveVideoFilters();
        HardwareH264Encoder hardwareEncoder = null;
        if (args.contains("%HARDWARE_H264%")) {
            hardwareEncoder = selectHardwareH264Encoder(executable, videoFilters);
            args = args.replace("%HARDWARE_H264%", hardwareEncoder.args);
        }
        args = addThreadLimitForCpuEncoders(args);
        return args
                .replace("%WIDTH%", String.valueOf(settings.getVideoWidth()))
                .replace("%HEIGHT%", String.valueOf(settings.getVideoHeight()))
                .replace("%FPS%", String.valueOf(settings.getFramesPerSecond()))
                .replace("%FILENAME%", fileName)
                .replace("%BITRATE%", String.valueOf(settings.getBitRate()))
                .replace("%THREADS%", String.valueOf(Math.max(1, settings.getEncoderThreadCount())))
                .replace("%FILTERS%", hardwareEncoder != null && hardwareEncoder.consumesFilters
                        ? ""
                        : videoFilters);
    }

    private String getEffectiveVideoFilters() {
        if (!inputNeedsVerticalFlip) {
            return settings.getVideoFilters();
        }
        String filters = appendVideoFilter(settings.getVideoFilters(), "vflip");
        LOGGER.info("Using FFmpeg vertical flip filter for raw OpenGL frame fast path: {}", filters.trim());
        return filters;
    }

    private String appendVideoFilter(String filters, String filter) {
        String filterChain = extractFilterChain(filters);
        if (filterChain.isEmpty()) {
            filterChain = filter;
        } else {
            filterChain += "," + filter;
        }
        return "-filter:v " + filterChain + " ";
    }

    private String upgradeLegacyPresetArguments(String args) {
        if (LEGACY_MP4_CUSTOM_ARGS.equals(args)) {
            return RenderSettings.EncodingPreset.MP4_HARDWARE.getValue();
        }
        if (LEGACY_WEBM_CUSTOM_ARGS.equals(args)) {
            return RenderSettings.EncodingPreset.WEBM_CUSTOM.getValue();
        }
        return args;
    }

    private String addThreadLimitForCpuEncoders(String args) {
        String lowerArgs = args.toLowerCase(Locale.ROOT);
        if (lowerArgs.contains("-threads")
                || (!lowerArgs.contains("libx264") && !lowerArgs.contains("libvpx"))) {
            return args;
        }
        int outputIndex = args.lastIndexOf("\"%FILENAME%\"");
        if (outputIndex < 0) {
            outputIndex = args.lastIndexOf("%FILENAME%");
        }
        String threads = "-threads %THREADS% ";
        if (outputIndex < 0) {
            return args + " " + threads;
        }
        return args.substring(0, outputIndex) + threads + args.substring(outputIndex);
    }

    private HardwareH264Encoder selectHardwareH264Encoder(String executable, String filters) {
        List<String> hwaccels = queryFFmpegFeatures(executable, "-hwaccels",
                new String[]{"cuda", "d3d11va", "dxva2", "qsv", "vaapi", "vulkan", "opencl", "videotoolbox"});
        List<String> encoders = queryFFmpegFeatures(executable, "-encoders",
                new String[]{"h264_nvenc", "h264_vaapi", "h264_qsv", "h264_amf", "h264_videotoolbox"});
        List<String> gpuFilters = queryFFmpegFeatures(executable, "-filters",
                new String[]{"hwupload", "hwupload_cuda", "hwmap", "scale_cuda", "scale_vaapi", "scale_qsv"});
        LOGGER.info("FFmpeg hardware acceleration methods: {}", hwaccels.isEmpty() ? "none detected" : hwaccels);
        LOGGER.info("FFmpeg hardware H.264 encoders: {}", encoders.isEmpty() ? "none detected" : encoders);
        LOGGER.info("FFmpeg GPU upload/map filters: {}", gpuFilters.isEmpty() ? "none detected" : gpuFilters);
        LOGGER.info("ReplayMod currently feeds FFmpeg via rawvideo stdin, so hardware encoders still require a CPU-to-GPU upload. Zero-copy OpenGL texture handoff is not available through this FFmpeg CLI path.");

        String encoder = null;
        boolean consumesFilters = false;
        if (encoders.contains("h264_nvenc")) {
            String filterChain = extractFilterChain(filters);
            if (encoderSupportsPixelFormat(executable, "h264_nvenc", "bgra")) {
                if (filterChain.isEmpty()) {
                    LOGGER.info("Using NVENC direct BGRA input path; skipping CPU BGRA-to-NV12 filter before GPU upload.");
                    encoder = "-c:v h264_nvenc -preset p1 -pix_fmt bgra";
                } else {
                    LOGGER.info("Using NVENC BGRA input path with software filter chain: {}", filterChain);
                    encoder = "-filter:v " + filterChain + " -c:v h264_nvenc -preset p1 -pix_fmt bgra";
                    consumesFilters = true;
                }
            } else {
                encoder = buildCudaUploadFilter(filters) + "-c:v h264_nvenc -preset p1";
                consumesFilters = true;
            }
        } else if (encoders.contains("h264_vaapi")) {
            encoder = "-vaapi_device /dev/dri/renderD128 " + buildVaapiUploadFilter(filters) + "-c:v h264_vaapi -qp 23";
            consumesFilters = true;
        } else if (encoders.contains("h264_qsv")) {
            encoder = "h264_qsv -preset veryfast";
        } else if (encoders.contains("h264_amf")) {
            encoder = "h264_amf -quality speed";
        } else if (encoders.contains("h264_videotoolbox")) {
            encoder = "h264_videotoolbox";
        }
        if (encoder == null) {
            LOGGER.warn("No supported hardware H.264 encoder found; falling back to libx264.");
            return new HardwareH264Encoder("-c:v libx264 -preset veryfast -threads %THREADS% -b:v %BITRATE% -pix_fmt yuv420p", false);
        }
        LOGGER.info("Using FFmpeg hardware encoder: {}", encoder);
        return new HardwareH264Encoder(encoder + " -b:v %BITRATE%", consumesFilters);
    }

    private String buildCudaUploadFilter(String filters) {
        String filterChain = extractFilterChain(filters);
        if (filterChain.isEmpty()) {
            filterChain = "format=nv12,hwupload_cuda";
        } else {
            filterChain += ",format=nv12,hwupload_cuda";
        }
        LOGGER.info("Using CUDA upload/filter chain for NVENC: {}", filterChain);
        return "-filter:v " + filterChain + " ";
    }

    private String buildVaapiUploadFilter(String filters) {
        String filterChain = extractFilterChain(filters);
        if (filterChain.isEmpty()) {
            filterChain = "format=nv12,hwupload";
        } else {
            filterChain += ",format=nv12,hwupload";
        }
        LOGGER.info("Using VAAPI upload/filter chain: {}", filterChain);
        return "-filter:v " + filterChain + " ";
    }

    private String extractFilterChain(String filters) {
        String trimmed = filters == null ? "" : filters.trim();
        if (trimmed.startsWith("-filter:v ")) {
            return trimmed.substring("-filter:v ".length()).trim();
        }
        if (trimmed.startsWith("-vf ")) {
            return trimmed.substring("-vf ".length()).trim();
        }
        return "";
    }

    private static class HardwareH264Encoder {
        private final String args;
        private final boolean consumesFilters;

        private HardwareH264Encoder(String args, boolean consumesFilters) {
            this.args = args;
            this.consumesFilters = consumesFilters;
        }
    }

    private List<String> queryFFmpegFeatures(String executable, String argument, String[] candidates) {
        List<String> features = new ArrayList<>();
        Process ffmpegProcess = null;
        try {
            ffmpegProcess = new ProcessBuilder(executable, "-hide_banner", argument)
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
            IOUtils.copy(ffmpegProcess.getInputStream(), output);
            if (!ffmpegProcess.waitFor(3, TimeUnit.SECONDS)) {
                ffmpegProcess.destroy();
                return features;
            }
            String text = output.toString("UTF-8").toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (text.contains(candidate)) {
                    features.add(candidate);
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to query FFmpeg {}:", argument, t);
        } finally {
            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
            }
        }
        return features;
    }

    private boolean encoderSupportsPixelFormat(String executable, String encoder, String pixelFormat) {
        Process ffmpegProcess = null;
        try {
            ffmpegProcess = new ProcessBuilder(executable, "-hide_banner", "-h", "encoder=" + encoder)
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
            IOUtils.copy(ffmpegProcess.getInputStream(), output);
            if (!ffmpegProcess.waitFor(3, TimeUnit.SECONDS)) {
                ffmpegProcess.destroy();
                return false;
            }
            String text = output.toString("UTF-8").toLowerCase(Locale.ROOT);
            return text.contains(pixelFormat.toLowerCase(Locale.ROOT));
        } catch (Throwable t) {
            LOGGER.debug("Failed to query FFmpeg encoder {} pixel formats:", encoder, t);
            return false;
        } finally {
            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
            }
        }
    }

    public File getVideoFile() throws FFmpegStartupException {
        waitForFFmpegLog();
        String log = ffmpegLog.toString();
        for (String line : log.split("\n")) {
            if (line.startsWith("Output #0")) {
                String fileName = line.substring(line.indexOf(", to '") + 6, line.lastIndexOf('\''));
                return new File(settings.getOutputFile().getParentFile(), fileName);
            }
        }
        throw new FFmpegStartupException(settings, log);
    }

    private void waitForFFmpegLog() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        int previousSize = -1;
        while (System.nanoTime() < deadline) {
            int size = ffmpegLog.size();
            if (size > 0 && size == previousSize) {
                return;
            }
            previousSize = size;
            try {
                process.exitValue();
                if (size > 0) {
                    return;
                }
            } catch (IllegalThreadStateException ignored) {
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static class NoFFmpegException extends IOException {
        public NoFFmpegException(Throwable cause) {
            super(cause);
        }
    }

    public static class FFmpegStartupException extends IOException {
        private final RenderSettings settings;
        private final String log;

        public FFmpegStartupException(RenderSettings settings, String log) {
            super(log);
            this.settings = settings;
            this.log = log;
        }

        public RenderSettings getSettings() {
            return settings;
        }

        public String getLog() {
            return log;
        }
    }
}
