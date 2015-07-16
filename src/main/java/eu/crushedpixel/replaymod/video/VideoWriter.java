package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.utils.StreamPipe;
import eu.crushedpixel.replaymod.utils.StringUtils;
import eu.crushedpixel.replaymod.video.frame.RGBFrame;
import eu.crushedpixel.replaymod.video.rendering.FrameConsumer;
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

    private final RenderOptions options;
    private final Process process;
    private final OutputStream outputStream;
    private final WritableByteChannel channel;
    private final String commandArgs;

    public VideoWriter(final RenderOptions options) throws IOException {
        this.options = options.copy();

        File outputFolder = options.getOutputFile().getParentFile();
        String fileName = options.getOutputFile().getName();

        commandArgs = options.getExportCommandArgs()
                    .replace("%WIDTH%", String.valueOf(options.getWidth()))
                    .replace("%HEIGHT%", String.valueOf(options.getHeight()))
                    .replace("%FPS%", String.valueOf(options.getFps()))
                    .replace("%FILENAME%", fileName)
                    .replace("%BITRATE%", options.getBitrate());

        List<String> command = new ArrayList<String>();
        command.add(options.getExportCommand());
        command.addAll(StringUtils.translateCommandline(commandArgs));
        System.out.println("Starting " + options.getExportCommand() + " with args: " + commandArgs);
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
            CrashReport report = CrashReport.makeCrashReport(t, "Exporting frame");
            CrashReportCategory exportDetails = report.makeCategory("Export details");
            exportDetails.addCrashSection("Export command", options.getExportCommand());
            exportDetails.addCrashSection("Export args", commandArgs);
            Minecraft.getMinecraft().crashed(report);
        }
    }

    private void checkSize(ReadableDimension size) {
        checkSize(size.getWidth(), size.getHeight());
    }

    private void checkSize(int width, int height) {
        isTrue(width == options.getWidth(), "Width has to be %d but was %d", options.getWidth(), width);
        isTrue(height == options.getHeight(), "Height has to be %d but was %d", options.getHeight(), height);
    }
}
