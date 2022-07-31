package com.replaymod.render.processor;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

import static com.replaymod.render.utils.Utils.openGlBytesToBitmap;

public class OpenGlToBitmapProcessor extends AbstractFrameProcessor<OpenGlFrame, BitmapFrame> {

    @Override
    public BitmapFrame process(OpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        int bpp = rawFrame.getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        openGlBytesToBitmap(rawFrame, 0, 0, result, width);
        ByteBufferPool.release(rawFrame.getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), new Dimension(width, height), bpp, result);
    }
}
