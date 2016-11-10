package com.replaymod.render;

import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.rendering.FrameConsumer;
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

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.isTrue;

public class VideoWriter implements FrameConsumer<RGBFrame> {

    private final RenderSettings settings;
    private final Process process;
    private final OutputStream outputStream;
    private final WritableByteChannel channel;
    private final String commandArgs;
    private volatile boolean aborted;

    private ByteArrayOutputStream ffmpegLog = new ByteArrayOutputStream(4096);

    public VideoWriter(final RenderSettings settings) throws IOException {
        this.settings = settings;

        File outputFolder = settings.getOutputFile().getParentFile();
        FileUtils.forceMkdir(outputFolder);
        String fileName = settings.getOutputFile().getName();

        commandArgs = settings.getExportArguments()
                    .replace("%WIDTH%", String.valueOf(settings.getVideoWidth()))
                    .replace("%HEIGHT%", String.valueOf(settings.getVideoHeight()))
                    .replace("%FPS%", String.valueOf(settings.getFramesPerSecond()))
                    .replace("%FILENAME%", fileName)
                    .replace("%BITRATE%", String.valueOf(settings.getBitRate()));

        String executable = settings.getExportCommand().isEmpty() ? "ffmpeg" : settings.getExportCommand();
        System.out.println("Starting " + settings.getExportCommand() + " with args: " + commandArgs);
        String[] cmdline = new CommandLine(executable).addArguments(commandArgs).toStrings();
        process = new ProcessBuilder(cmdline).directory(outputFolder).start();
        OutputStream exportLogOut = new TeeOutputStream(new FileOutputStream("export.log"), ffmpegLog);
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

    public File getVideoFile() {
        String log = ffmpegLog.toString();
        for (String line : log.split("\n")) {
            if (line.startsWith("Output #0")) {
                String fileName = line.substring(line.indexOf(", to '") + 6, line.lastIndexOf('\''));
                return new File(settings.getOutputFile().getParentFile(), fileName);
            }
        }
        throw new IllegalStateException("No output file found.");
    }
}
