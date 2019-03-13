package com.replaymod.render.processor;

import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

import static com.replaymod.render.utils.Utils.openGlBytesToRBG;

public class StereoscopicToRGBProcessor extends AbstractFrameProcessor<StereoscopicOpenGlFrame, RGBFrame> {
    @Override
    public RGBFrame process(StereoscopicOpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.getLeft().getSize();
        int width = size.getWidth();
        ByteBuffer leftBuffer = rawFrame.getLeft().getByteBuffer();
        ByteBuffer rightBuffer = rawFrame.getRight().getByteBuffer();
        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.getHeight() * 4);
        openGlBytesToRBG(leftBuffer, width, 0, 0, result, width * 2);
        openGlBytesToRBG(rightBuffer, width, size.getWidth(), 0, result, width * 2);
        ByteBufferPool.release(leftBuffer);
        ByteBufferPool.release(rightBuffer);
        return new RGBFrame(rawFrame.getFrameId(), new Dimension(width * 2, size.getHeight()), result);
    }
}
