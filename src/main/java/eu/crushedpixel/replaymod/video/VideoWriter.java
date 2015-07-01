package eu.crushedpixel.replaymod.video;

import com.google.common.base.Preconditions;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.StreamPipe;
import eu.crushedpixel.replaymod.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.commons.lang3.Validate.isTrue;

public class VideoWriter {

    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    private final RenderOptions options;
    private final Process process;
    private final OutputStream outputStream;
    private final WritableByteChannel channel;

    private volatile boolean active = true;
    private volatile boolean cancelled = false;

    private int queueLimit;
    private final Queue<BufferedImage> toWrite;
    private final Thread writerThread;

    private final Lock lock = new ReentrantLock();
    private final Condition emptyCondition = lock.newCondition();
    private final Condition noLongerEmptyCondition = lock.newCondition();
    private final Condition noLongerFullCondition = lock.newCondition();

    public VideoWriter(final VideoRenderer renderer, final RenderOptions options) throws IOException {
        this.options = options.copy();
        this.queueLimit = options.getWriterQueueSize();
        this.toWrite = new LinkedList<BufferedImage>();

        File folder = ReplayFileIO.getRenderFolder();
        String fileName = FILE_FORMAT.format(Calendar.getInstance().getTime());

        final String args;

        if(options.getCommandLineArguments() != null) {
            args = options.getCommandLineArguments();
        } else {
            args = options.getExportCommandArgs()
                    .replace("%WIDTH%", String.valueOf(options.getWidth()))
                    .replace("%HEIGHT%", String.valueOf(options.getHeight()))
                    .replace("%FPS%", String.valueOf(options.getFps()))
                    .replace("%FILENAME%", fileName)
                    .replace("%BITRATE%", options.getBitrate());
        }

        List<String> command = new ArrayList<String>();
        command.add(options.getExportCommand());
        command.addAll(StringUtils.translateCommandline(args));
        System.out.println("Starting " + options.getExportCommand() + " with args: " + args);
        process = new ProcessBuilder(command).directory(folder).start();
        OutputStream exportLogOut = new FileOutputStream("export.log");
        new StreamPipe(process.getInputStream(), exportLogOut).start();
        new StreamPipe(process.getErrorStream(), exportLogOut).start();
        outputStream = process.getOutputStream();
        channel = Channels.newChannel(outputStream);

        writerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (!cancelled && (active || !toWrite.isEmpty())) {
                        try {
                            lock.lockInterruptibly();
                            BufferedImage img;
                            try {
                                img = toWrite.poll();
                                if (img == null) {
                                    noLongerEmptyCondition.await();
                                    img = toWrite.poll();
                                }
                                noLongerFullCondition.signal();
                                if (toWrite.isEmpty()) {
                                    emptyCondition.signalAll();
                                }
                            } finally {
                                lock.unlock();
                            }

                            DataBuffer imgBuffer = img.getRaster().getDataBuffer();
                            if (imgBuffer instanceof DataBufferByte) {
                                outputStream.write(((DataBufferByte) imgBuffer).getData());
                            } else if (imgBuffer instanceof DataBufferInt) {
                                ByteBuffer byteBuffer = ByteBuffer.allocate(img.getWidth() * img.getHeight() * 4);
                                byteBuffer.asIntBuffer().put(((DataBufferInt) imgBuffer).getData());
                                channel.write(byteBuffer);
                            } else {
                                throw new RuntimeException("DataBuffer type not supported: " + imgBuffer.getClass());
                            }
                        } catch (InterruptedException ignored) {
                        }
                    }
                    toWrite.clear();
                    IOUtils.closeQuietly(outputStream);
                } catch (IOException e) {
                    if (active) {
                        CrashReport report = new CrashReport("Exporting frame", e);
                        CrashReportCategory exportDetails = report.makeCategory("Export details");
                        exportDetails.addCrashSection("Export command", options.getExportCommand());
                        exportDetails.addCrashSection("Export args", args);
                        Minecraft.getMinecraft().crashed(report);
                        renderer.cancel();
                    }
                } finally {
                    process.destroy();
                }
            }
        }, "replaymod-video-writer");
        writerThread.start();
    }

    /**
     * Add the image to the writer queue.
     * @param image The image
     * @param waitIfFull Whether to wait if the queue is full or to return immediately
     * @return {@code} true if the image was added, {@code false} if it could not be added due to the queue being full
     *         and {@code waitIfFull} being {@code false}
     */
    public boolean writeImage(BufferedImage image, boolean waitIfFull) {
        Preconditions.checkState(active, "This VideoWriter has already been closed.");
        isTrue(image.getWidth() == options.getWidth(), "Width has to be " + options.getWidth() + " but was " + image.getWidth());
        isTrue(image.getHeight() == options.getHeight(), "Height has to be " + options.getHeight() + " but was " + image.getHeight());

        lock.lock();
        try {
            while (toWrite.size() >= queueLimit) {
                if (waitIfFull) {
                    noLongerFullCondition.await();
                } else {
                    return false;
                }
            }
            toWrite.offer(image);
            noLongerEmptyCondition.signal();
            return true;
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
        return false;
    }

    public void endRecording() {
        active = false;
        writerThread.interrupt();
    }

    public void abortRecording() {
        cancelled = true;
        active = false;
        writerThread.interrupt();
    }

    public void waitTillQueueEmpty() {
        lock.lock();
        try {
            if (!toWrite.isEmpty()) {
                emptyCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void waitForFinish() throws InterruptedException {
        Preconditions.checkState(!active, "Video writer still active.");
        writerThread.join();
    }
}
