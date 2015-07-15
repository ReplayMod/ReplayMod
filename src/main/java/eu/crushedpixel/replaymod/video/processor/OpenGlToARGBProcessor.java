package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.ARGBFrame;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;
import org.lwjgl.util.ReadableDimension;

import java.nio.ByteBuffer;

import static eu.crushedpixel.replaymod.utils.OpenGLUtils.openGlBytesToARBG;

public class OpenGlToARGBProcessor extends AbstractFrameProcessor<OpenGlFrame, ARGBFrame> {
    @Override
    public ARGBFrame process(OpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.getSize();
        ByteBuffer buffer = rawFrame.getByteBuffer();
        ByteBuffer argb = ByteBufferPool.allocate(size.getWidth() * size.getHeight() * 4);
        openGlBytesToARBG(buffer, size.getWidth(), 0, 0, argb, size.getWidth());
        ByteBufferPool.release(buffer);
        return new ARGBFrame(rawFrame.getFrameId(), size, argb);
    }
}
