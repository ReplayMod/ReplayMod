package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.ARGBFrame;
import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;
import org.lwjgl.util.Dimension;

import java.nio.ByteBuffer;

import static eu.crushedpixel.replaymod.utils.OpenGLUtils.openGlBytesToARBG;

public class CubicToARGBProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, ARGBFrame> {
    @Override
    public ARGBFrame process(CubicOpenGlFrame rawFrame) {
        int size = rawFrame.getLeft().getSize().getWidth();
        int width = size * 4;
        int height = size * 3;
        ByteBuffer result = ByteBufferPool.allocate(width * height * 4);
        openGlBytesToARBG(rawFrame.getLeft().getByteBuffer(), size, 0, size, result, width);
        openGlBytesToARBG(rawFrame.getFront().getByteBuffer(), size, size, size, result, width);
        openGlBytesToARBG(rawFrame.getRight().getByteBuffer(), size, size * 2, size, result, width);
        openGlBytesToARBG(rawFrame.getBack().getByteBuffer(), size, size * 3, size, result, width);
        openGlBytesToARBG(rawFrame.getTop().getByteBuffer(), size, size, 0, result, width);
        openGlBytesToARBG(rawFrame.getBottom().getByteBuffer(), size, size, size * 2, result, width);
        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        ByteBufferPool.release(rawFrame.getFront().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBack().getByteBuffer());
        ByteBufferPool.release(rawFrame.getTop().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBottom().getByteBuffer());
        return new ARGBFrame(rawFrame.getFrameId(), new Dimension(width, height), result);
    }
}
