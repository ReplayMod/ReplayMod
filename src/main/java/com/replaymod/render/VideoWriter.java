package com.replaymod.render;

import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.rendering.FrameConsumer;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.StreamPipe;
import eu.crushedpixel.replaymod.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import org.apache.commons.io.IOUtils;
import org.lwjgl.util.ReadableDimension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.isTrue;

public class VideoWriter implements FrameConsumer<RGBFrame> {

    private final RenderSettings settings;
    private final Process process;
    private final OutputStream outputStream;
    private final WritableByteChannel channel;
    private final String commandArgs;
    private volatile boolean aborted;

    public VideoWriter(final RenderSettings settings) throws IOException {
        this.settings = settings;

        File outputFolder = settings.getOutputFile().getParentFile();
        String fileName = settings.getOutputFile().getName();

        commandArgs = settings.getExportArguments()
                    .replace("%WIDTH%", String.valueOf(settings.getVideoWidth()))
                    .replace("%HEIGHT%", String.valueOf(settings.getVideoHeight()))
                    .replace("%FPS%", String.valueOf(settings.getFramesPerSecond()))
                    .replace("%FILENAME%", fileName)
                    .replace("%BITRATE%", String.valueOf(settings.getBitRate()));

        List<String> command = new ArrayList<>();
        command.add(settings.getExportCommand().isEmpty() ? "ffmpeg" : settings.getExportCommand());
        command.addAll(StringUtils.translateCommandline(commandArgs));
        System.out.println("Starting " + settings.getExportCommand() + " with args: " + commandArgs);
        process = new ProcessBuilder(command).directory(outputFolder).start();
        OutputStream exportLogOut = new FileOutputStream("export.log");
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
}
