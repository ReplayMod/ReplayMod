package eu.crushedpixel.replaymod.video;

import com.google.common.base.Preconditions;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import org.monte.media.*;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.math.Rational;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoWriter {

    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat(DATE_FORMAT);

    private static final String VIDEO_EXTENSION = ".avi";

    private final File file;
    private final MovieWriter out;
    private final Buffer buf;
    private final int track;

    private volatile boolean active = true;
    private volatile boolean cancelled = false;

    private int queueLimit;
    private final Queue<BufferedImage> toWrite;
    private final Thread writerThread;

    private final Lock lock = new ReentrantLock();
    private final Condition emptyCondition = lock.newCondition();
    private final Condition noLongerEmptyCondition = lock.newCondition();
    private final Condition noLongerFullCondition = lock.newCondition();

    public VideoWriter(int width, int height, int fps, float quality) throws IOException {
        this(width, height, fps, quality, Integer.MAX_VALUE);
    }

    public VideoWriter(int width, int height, int fps, float quality, int queueLimit) throws IOException {
        this.queueLimit = queueLimit;
        this.toWrite = new LinkedList<BufferedImage>();

        File folder = ReplayFileIO.getRenderFolder();
        String fileName = FILE_FORMAT.format(Calendar.getInstance().getTime());
        file = new File(folder, fileName + VIDEO_EXTENSION);
        Files.createFile(file.toPath());

        out = Registry.getInstance().getWriter(file);
        Format format = new Format(FormatKeys.MediaTypeKey, MediaType.VIDEO,
                FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG,
                FormatKeys.FrameRateKey, new Rational(fps, 1),
                VideoFormatKeys.WidthKey, width,
                VideoFormatKeys.HeightKey, height,
                VideoFormatKeys.DepthKey, 24,
                VideoFormatKeys.QualityKey, quality);

        track = out.addTrack(format);

        buf = new Buffer();
        buf.format = new Format(VideoFormatKeys.DataClassKey, BufferedImage.class);
        buf.sampleDuration = out.getFormat(track).get(VideoFormatKeys.FrameRateKey).inverse();

        writerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(!cancelled && (active || !toWrite.isEmpty())) {
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
                        buf.data = img;
                        try {
                            out.write(track, buf);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    toWrite.clear();
                    out.close();
                    if (cancelled) {
                        Files.delete(file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        writerThread.start();
    }

    public void setQueueLimit(int limit) {
        this.queueLimit = limit;
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
