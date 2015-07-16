package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.RGBFrame;
import eu.crushedpixel.replaymod.video.frame.StereoscopicOpenGlFrame;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.nio.ByteBuffer;

import static eu.crushedpixel.replaymod.utils.OpenGLUtils.openGlBytesToRBG;

public class StereoscopicToRGBProcessor extends AbstractFrameProcessor<StereoscopicOpenGlFrame, RGBFrame> {
    @Override
    public RGBFrame process(StereoscopicOpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.getLeft().getSize();
        int width = size.getWidth();
        ByteBuffer leftBuffer = rawFrame.getLeft().getByteBuffer();
        ByteBuffer rightBuffer = rawFrame.getRight().getByteBuffer();
        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.getHeight() * 3);
        openGlBytesToRBG(leftBuffer, width, 0, 0, result, width * 2);
        openGlBytesToRBG(rightBuffer, width, size.getWidth(), 0, result, width * 2);
        ByteBufferPool.release(leftBuffer);
        ByteBufferPool.release(rightBuffer);
        return new RGBFrame(rawFrame.getFrameId(), new Dimension(width * 2, size.getHeight()), result);
    }
}
