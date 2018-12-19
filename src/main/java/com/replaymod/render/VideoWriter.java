package com.replaymod.render;

import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.StreamPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.lwjgl.util.ReadableDimension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

import static com.replaymod.render.ReplayModRender.LOGGER;
import static org.apache.commons.lang3.Validate.isTrue;

public class VideoWriter implements FrameConsumer<RGBFrame> {

    private final VideoRenderer renderer;
    private final RenderSettings settings;
    private final Process process;
    private final OutputStream outputStream;
    private final WritableByteChannel channel;
    private final String commandArgs;
    private volatile boolean aborted;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

    public VideoWriter(final VideoRenderer renderer) throws IOException {
        this.renderer = renderer;
        this.settings = renderer.getRenderSettings();

        File outputFolder = settings.getOutputFile().getParentFile();
        FileUtils.forceMkdir(outputFolder);
        String fileName = settings.getOutputFile().getName();

        commandArgs = settings.getExportArguments()
                    .replace("%WIDTH%", String.valueOf(settings.getVideoWidth()))
                    .replace("%HEIGHT%", String.valueOf(settings.getVideoHeight()))
                    .replace("%FPS%", String.valueOf(settings.getFramesPerSecond()))
                    .replace("%FILENAME%", fileName)
                    .replace("%BITRATE%", String.valueOf(settings.getBitRate()))
                    .replace("%FILTERS%", settings.getVideoFilters());

        String executable = settings.getExportCommandOrDefault();
        LOGGER.info("Starting {} with args: {}", executable, commandArgs);
        String[] cmdline;
        try {
            cmdline = new CommandLine(executable).addArguments(commandArgs).toStrings();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse ffmpeg command line:", e);
            throw new FFmpegStartupException(settings, e.getLocalizedMessage());
        }
        try {
            process = new ProcessBuilder(cmdline).directory(outputFolder).start();
        } catch (IOException e) {
            throw new NoFFmpegException(e);
        }
        File exportLogFile = new File(Minecraft.getMinecraft().mcDataDir, "export.log");
        OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream(exportLogFile), ffmpegLog);
        new StreamPipe(process.getInputStream(), exportLogOut).start();
        new StreamPipe(process.getErrorStream(), exportLogOut).start();
        outputStream = process.getOutputStream();
        channel = Channels.newChannel(outputStream);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(outputStream);

        long startTime = System.nanoTime();
        long rem = TimeUnit.SECONDS.toNanos(30);
        do {
            try {
                process.exitValue();
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

        process.destroy();
    }

    @Override
    public void consume(RGBFrame frame) {
        try {
            checkSize(frame.getSize());
            channel.write(frame.getByteBuffer());
            ByteBufferPool.release(frame.getByteBuffer());
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
                renderer.setFailure(e);
                return;
            }
            CrashReport report = CrashReport.makeCrashReport(t, "Exporting frame");
            CrashReportCategory exportDetails = report.makeCategory("Export details");
            exportDetails.addCrashSection("Export command", settings.getExportCommand());
            exportDetails.addCrashSection("Export args", commandArgs);
            Minecraft.getMinecraft().crashed(report);
        }
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

    public File getVideoFile() throws FFmpegStartupException {
        String log = ffmpegLog.toString();
        for (String line : log.split("\n")) {
            if (line.startsWith("Output #0")) {
                String fileName = line.substring(line.indexOf(", to '") + 6, line.lastIndexOf('\''));
                return new File(settings.getOutputFile().getParentFile(), fileName);
            }
        }
        throw new FFmpegStartupException(settings, log);
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
