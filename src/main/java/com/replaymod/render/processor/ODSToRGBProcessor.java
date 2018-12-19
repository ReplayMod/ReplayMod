package com.replaymod.render.processor;

import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.utils.ByteBufferPool;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ODSToRGBProcessor extends AbstractFrameProcessor<ODSOpenGlFrame, RGBFrame> {
    private final EquirectangularToRGBProcessor processor;

    public ODSToRGBProcessor(int outputWidth, int outputHeight, int sphericalFovX) {
        processor = new EquirectangularToRGBProcessor(outputWidth, outputHeight / 2, sphericalFovX);
    }

    @Override
    public RGBFrame process(ODSOpenGlFrame rawFrame) {
        RGBFrame leftFrame = processor.process(rawFrame.getLeft());
        RGBFrame rightFrame = processor.process(rawFrame.getRight());
        ReadableDimension size = new Dimension(leftFrame.getSize().getWidth(), leftFrame.getSize().getHeight() * 2);
        ByteBuffer result = ByteBufferPool.allocate(size.getWidth() * size.getHeight() * 3);
        result.put(leftFrame.getByteBuffer());
        result.put(rightFrame.getByteBuffer());
        result.rewind();
        ByteBufferPool.release(leftFrame.getByteBuffer());
        ByteBufferPool.release(rightFrame.getByteBuffer());
        return new RGBFrame(rawFrame.getFrameId(), size, result);
    }

    @Override
    public void close() throws IOException {
        processor.close();
    }

    public int getFrameSize() {
        return processor.getFrameSize();
    }
}
