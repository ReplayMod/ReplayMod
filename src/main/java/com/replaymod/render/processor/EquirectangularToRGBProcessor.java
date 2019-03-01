package com.replaymod.render.processor;

import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

import static java.lang.Math.PI;

public class EquirectangularToRGBProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, RGBFrame> {
    private static final byte IMAGE_BACK = 0;
    private static final byte IMAGE_FRONT = 1;
    private static final byte IMAGE_LEFT = 2;
    private static final byte IMAGE_RIGHT = 3;
    private static final byte IMAGE_TOP = 4;
    private static final byte IMAGE_BOTTOM = 5;

    @Getter
    private final int frameSize;
    private final int width;
    private final int height;

    private final byte[][] image;
    private final int[][] imageX;
    private final int[][] imageY;

    public EquirectangularToRGBProcessor(int outputWidth, int outputHeight, int sphericalFovX) {
        // calculate the dimensions of the original equirectangular projection
        // (before cropping according to FOV)
        width = outputWidth;
        height = outputHeight;

        int fullWidth;
        if (sphericalFovX < 360) {
            fullWidth = Math.round(width * 360 / (float) sphericalFovX);
        } else {
            fullWidth = width;
        }

        int fullHeight = fullWidth / 2;

        frameSize = fullWidth / 4;

        image = new byte[height][width];
        imageX = new int[height][width];
        imageY = new int[height][width];

        int xOffset = (fullWidth - width) / 2;
        int yOffset = (fullHeight - height) / 2;

        for (int x = 0; x < width; x++) {
            // get x position relative to the full projection
            int i = xOffset + x;

            double yaw = PI * 2 * i / fullWidth;
            int piQuarter = 8 * i / fullWidth - 4;
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
            double fYaw = (yaw + PI / 4) % (PI / 2) - PI / 4;
            double d = 1 / Math.cos(fYaw);
            double gcXN = (Math.tan(fYaw) + 1) / 2;
            for (int y = 0; y < height; y++) {
                // get y position relative to the full projection
                int j = yOffset + y;

                double cXN = gcXN;
                byte pt = target;
                double pitch = PI * j / fullHeight - PI / 2;
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
                image[y][x] = pt;
                imageX[y][x] = imgX;
                imageY[y][x] = frameSize - imgY - 1; // The OpenGl buffer contains data flipped vertically
            }
        }
    }

    @Override
    public RGBFrame process(CubicOpenGlFrame rawFrame) {
        Validate.isTrue(rawFrame.getLeft().getSize().getWidth() == frameSize, "Frame size must be %d but was %d",
                frameSize, rawFrame.getLeft().getSize().getWidth());
        ByteBuffer result = ByteBufferPool.allocate(width * height * 3);
        ByteBuffer[] images = {
                rawFrame.getBack().getByteBuffer(), rawFrame.getFront().getByteBuffer(),
                rawFrame.getLeft().getByteBuffer(), rawFrame.getRight().getByteBuffer(),
                rawFrame.getTop().getByteBuffer(), rawFrame.getBottom().getByteBuffer()
        };
        byte[] pixel = new byte[3];
        byte[] image;
        int[] imageX, imageY;
        for (int y = 0; y < height; y++) {
            image = this.image[y];
            imageX = this.imageX[y];
            imageY = this.imageY[y];
            for (int x = 0; x < width; x++) {
                ByteBuffer source = images[image[x]];
                source.position((imageX[x] + imageY[x] * frameSize) * 3);
                source.get(pixel);
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
        return new RGBFrame(rawFrame.getFrameId(), new Dimension(width, height), result);
    }
}
