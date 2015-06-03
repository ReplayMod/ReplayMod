package eu.crushedpixel.replaymod.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class OpenGLUtils {
    public static final int VIEWPORT_MAX_WIDTH;
    public static final int VIEWPORT_MAX_HEIGHT;

    static {
        IntBuffer buffer = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS, buffer);
        VIEWPORT_MAX_WIDTH = buffer.get();
        VIEWPORT_MAX_HEIGHT = buffer.get();
    }

    public static void openGlBytesToBufferedImage(ByteBuffer buffer, int bufferWidth, BufferedImage image, int offsetX, int offsetY) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int imageWidth = image.getWidth();
        int bufferSize = buffer.remaining() / 3;
        // Read the OpenGL image row by row from right to left (flipped horizontally)
        for (int i = bufferSize - 1; i >= 0; i--) {
            // Coordinates in the final image
            int x = offsetX + bufferWidth - i % bufferWidth - 1; // X coord of OpenGL image has to be flipped first
            int y = offsetY + i / bufferWidth;
            if (x >= imageWidth || y * imageWidth >= pixels.length) {
                buffer.position(buffer.position() + 3); // Pixel would end up outside of image
                continue;
            }
            // Write to image (row by row, left to right)
            pixels[y * imageWidth + x] = 0xff << 24 | (buffer.get() & 0xff) << 16 | (buffer.get() & 0xff) << 8 | buffer.get() & 0xff;
        }
        buffer.rewind();
    }

}
