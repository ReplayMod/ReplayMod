package com.replaymod.render.processor;

import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ODSToBitmapProcessor extends AbstractFrameProcessor<ODSOpenGlFrame, BitmapFrame> {
    private final EquirectangularToBitmapProcessor processor;

    public ODSToBitmapProcessor(int outputWidth, int outputHeight, int sphericalFovX) {
        processor = new EquirectangularToBitmapProcessor(outputWidth, outputHeight / 2, sphericalFovX);
    }

    @Override
    public BitmapFrame process(ODSOpenGlFrame rawFrame) {
        BitmapFrame leftFrame = processor.process(rawFrame.getLeft());
        BitmapFrame rightFrame = processor.process(rawFrame.getRight());
        ReadableDimension size = new Dimension(leftFrame.getSize().getWidth(), leftFrame.getSize().getHeight() * 2);
        int bpp = rawFrame.getLeft().getLeft().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(size.getWidth() * size.getHeight() * bpp);
        result.put(leftFrame.getByteBuffer());
        result.put(rightFrame.getByteBuffer());
        result.rewind();
        ByteBufferPool.release(leftFrame.getByteBuffer());
        ByteBufferPool.release(rightFrame.getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), size, bpp, result);
    }

    @Override
    public void close() throws IOException {
        processor.close();
    }

    public int getFrameSize() {
        return processor.getFrameSize();
    }
}
