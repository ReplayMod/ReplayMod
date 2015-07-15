package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.ARGBFrame;
import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;
import org.apache.commons.lang3.Validate;
import org.lwjgl.util.Dimension;

import java.nio.ByteBuffer;

import static java.lang.Math.PI;

public class EquirectangularToARGBProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, ARGBFrame> {
    private static final byte IMAGE_BACK = 0;
    private static final byte IMAGE_FRONT = 1;
    private static final byte IMAGE_LEFT = 2;
    private static final byte IMAGE_RIGHT = 3;
    private static final byte IMAGE_TOP = 4;
    private static final byte IMAGE_BOTTOM = 5;

    private final int frameSize;
    private final int width;
    private final int height;

    private final byte[][] image;
    private final int[][] imageX;
    private final int[][] imageY;


    public EquirectangularToARGBProcessor(int frameSize) {
        this.frameSize = frameSize;

        width = frameSize * 4;
        height = frameSize * 2;
        image = new byte[height][width];
        imageX = new int[height][width];
        imageY = new int[height][width];
        for (int i = 0; i < width; i++) {
            double yaw = PI * 2 * i / width;
            int piQuarter = 8 * i / width - 4;
            byte target;
            if (piQuarter < -3) {
                target = IMAGE_BACK;
            } else if (piQuarter < -1) {
                target = IMAGE_LEFT;
            } else if (piQuarter < 1) {
                target = IMAGE_FRONT;
            } else if (piQuarter < 3) {
                target = IMAGE_RIGHT;
            } else {
                target = IMAGE_BACK;
            }
            double fYaw = (yaw + PI/4) % (PI / 2) - PI/4;
            double d = 1 / Math.cos(fYaw);
            double gcXN = (Math.tan(fYaw) + 1) / 2;
            for (int j = 0; j < height; j++) {
                double cXN = gcXN;
                byte pt = target;
                double pitch = PI * j / height - PI / 2;
                double cYN = (Math.tan(pitch) * d + 1) / 2;

                if (cYN >= 1) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (-Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = IMAGE_BOTTOM;
                }
                if (cYN < 0) {
                    double pd = Math.tan(PI/2 - pitch);
                    cXN = (Math.sin(yaw) * pd + 1) / 2;
                    cYN = (Math.cos(yaw) * pd + 1) / 2;
                    pt = IMAGE_TOP;
                }

                int imgX = (int) Math.min(frameSize - 1, (cXN * frameSize));
                int imgY = (int) Math.min(frameSize - 1, (cYN * frameSize));
                image[j][i] = pt;
                imageX[j][i] = imgX;
                imageY[j][i] = frameSize - imgY - 1; // The OpenGl buffer contains data flipped vertically
            }
        }
    }

    @Override
    public ARGBFrame process(CubicOpenGlFrame rawFrame) {
        Validate.isTrue(rawFrame.getLeft().getSize().getWidth() == frameSize, "Frame size must be %d but was %d",
                frameSize, rawFrame.getLeft().getSize().getWidth());
        ByteBuffer result = ByteBufferPool.allocate(width * height * 4);
        ByteBuffer[] images = {
                rawFrame.getBack().getByteBuffer(), rawFrame.getFront().getByteBuffer(),
                rawFrame.getLeft().getByteBuffer(), rawFrame.getRight().getByteBuffer(),
                rawFrame.getTop().getByteBuffer(), rawFrame.getBottom().getByteBuffer()
        };
        byte[] pixel = new byte[4];
        pixel[0] = (byte) 0xff;
        byte[] image;
        int[] imageX, imageY;
        for (int y = 0; y < height; y++) {
            image = this.image[y];
            imageX = this.imageX[y];
            imageY = this.imageY[y];
            for (int x = 0; x < width; x++) {
                ByteBuffer source = images[image[x]];
                source.position((imageX[x] + imageY[x] * frameSize) * 3);
                source.get(pixel, 1, 3);
                result.put(pixel);
            }
        }
        result.rewind();

        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        ByteBufferPool.release(rawFrame.getFront().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBack().getByteBuffer());
        ByteBufferPool.release(rawFrame.getTop().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBottom().getByteBuffer());
        return new ARGBFrame(rawFrame.getFrameId(), new Dimension(width, height), result);
    }
}
