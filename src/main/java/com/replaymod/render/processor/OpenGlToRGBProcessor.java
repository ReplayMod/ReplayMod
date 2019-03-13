package com.replaymod.render.processor;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

public class OpenGlToRGBProcessor extends AbstractFrameProcessor<OpenGlFrame, RGBFrame> {

    private byte[] row, rowSwap;

    @Override
    public RGBFrame process(OpenGlFrame rawFrame) {
        // Flip whole image in place

        ReadableDimension size = rawFrame.getSize();
        int rowSize = size.getWidth() * 4;
        if (row == null || row.length < rowSize) {
            row = new byte[rowSize];
            rowSwap = new byte[rowSize];
        }
        ByteBuffer buffer = rawFrame.getByteBuffer();
        int rows = size.getHeight();
        byte[] row = this.row;
        byte[] rowSwap = this.rowSwap;
        for (int i = 0; i < rows / 2; i++) {
            int from = rowSize * i;
            int to = rowSize * (rows - i - 1);
            buffer.position(from);
            buffer.get(row);
            buffer.position(to);
            buffer.get(rowSwap);
            buffer.position(to);
            buffer.put(row);
            buffer.position(from);
            buffer.put(rowSwap);
        }
        buffer.rewind();
        return new RGBFrame(rawFrame.getFrameId(), size, buffer);
    }
}
