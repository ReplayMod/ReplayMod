package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.ARGBFrame;
import eu.crushedpixel.replaymod.video.frame.StereoscopicOpenGlFrame;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.nio.ByteBuffer;

import static eu.crushedpixel.replaymod.utils.OpenGLUtils.openGlBytesToARBG;

public class StereoscopicToARGBProcessor extends AbstractFrameProcessor<StereoscopicOpenGlFrame, ARGBFrame> {
    @Override
    public ARGBFrame process(StereoscopicOpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.getLeft().getSize();
        int width = size.getWidth();
        ByteBuffer leftBuffer = rawFrame.getLeft().getByteBuffer();
        ByteBuffer rightBuffer = rawFrame.getRight().getByteBuffer();
        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.getHeight() * 4);
        openGlBytesToARBG(leftBuffer, width, 0, 0, result, width * 2);
        openGlBytesToARBG(rightBuffer, width, size.getWidth(), 0, result, width * 2);
        ByteBufferPool.release(leftBuffer);
        ByteBufferPool.release(rightBuffer);
        return new ARGBFrame(rawFrame.getFrameId(), new Dimension(width * 2, size.getHeight()), result);
    }
}
