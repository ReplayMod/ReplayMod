package com.replaymod.render.processor;

import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;

import java.nio.ByteBuffer;

import static com.replaymod.render.utils.Utils.openGlBytesToBitmap;

public class CubicToBitmapProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, BitmapFrame> {

    @Override
    public BitmapFrame process(CubicOpenGlFrame rawFrame) {
        int size = rawFrame.getLeft().getSize().getWidth();
        int bpp = rawFrame.getLeft().getBytesPerPixel();
        int width = size * 4;
        int height = size * 3;
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        openGlBytesToBitmap(rawFrame.getLeft(), 0, size, result, width);
        openGlBytesToBitmap(rawFrame.getFront(), size, size, result, width);
        openGlBytesToBitmap(rawFrame.getRight(), size * 2, size, result, width);
        openGlBytesToBitmap(rawFrame.getBack(), size * 3, size, result, width);
        openGlBytesToBitmap(rawFrame.getTop(), size, 0, result, width);
        openGlBytesToBitmap(rawFrame.getBottom(), size, size * 2, result, width);
        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        ByteBufferPool.release(rawFrame.getFront().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBack().getByteBuffer());
        ByteBufferPool.release(rawFrame.getTop().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBottom().getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), new Dimension(width, height), bpp, result);
    }
}
