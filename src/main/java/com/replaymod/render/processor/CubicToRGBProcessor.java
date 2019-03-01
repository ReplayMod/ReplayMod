package com.replaymod.render.processor;

import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;

import java.nio.ByteBuffer;

import static com.replaymod.render.utils.Utils.openGlBytesToRBG;

public class CubicToRGBProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, RGBFrame> {
    @Override
    public RGBFrame process(CubicOpenGlFrame rawFrame) {
        int size = rawFrame.getLeft().getSize().getWidth();
        int width = size * 4;
        int height = size * 3;
        ByteBuffer result = ByteBufferPool.allocate(width * height * 3);
        openGlBytesToRBG(rawFrame.getLeft().getByteBuffer(), size, 0, size, result, width);
        openGlBytesToRBG(rawFrame.getFront().getByteBuffer(), size, size, size, result, width);
        openGlBytesToRBG(rawFrame.getRight().getByteBuffer(), size, size * 2, size, result, width);
        openGlBytesToRBG(rawFrame.getBack().getByteBuffer(), size, size * 3, size, result, width);
        openGlBytesToRBG(rawFrame.getTop().getByteBuffer(), size, size, 0, result, width);
        openGlBytesToRBG(rawFrame.getBottom().getByteBuffer(), size, size, size * 2, result, width);
        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        ByteBufferPool.release(rawFrame.getFront().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBack().getByteBuffer());
        ByteBufferPool.release(rawFrame.getTop().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBottom().getByteBuffer());
        return new RGBFrame(rawFrame.getFrameId(), new Dimension(width, height), result);
    }
}
