package com.replaymod.render.utils;

import java.nio.ByteBuffer;

public class Utils {
    /**
     * Copies the rgb image (flipped vertically) to the specified position in the target buffer
     * @param buffer Source image
     * @param bufferWidth  Source image width
     * @param xOffset X offset in target image
     * @param yOffset Y offset in target image
     * @param to Target image
     * @param width Target image width
     */
    public static void openGlBytesToRBG(ByteBuffer buffer, int bufferWidth, int xOffset, int yOffset, ByteBuffer to, int width) {
        byte[] rowBuf = new byte[bufferWidth * 4];
        // Copy image flipped vertically to target buffer
        int rows = buffer.remaining() / 4 / bufferWidth;
        for (int i = 0; i < rows; i++) {
            buffer.get(rowBuf);
            to.position(((yOffset + rows - i - 1) * width + xOffset) * 4);
            to.put(rowBuf);
        }
        to.rewind();
    }
}
