package eu.crushedpixel.replaymod.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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

    /**
     * Magic init method which has to be called from the OpenGL thread so the variables in this class
     * can be initialized successfully.
     * Does not perform any work on its own.
     */
    public static void init() {
    }

    public static void openGlBytesToARBG(ByteBuffer buffer, int bufferWidth, int xOffset, int yOffset, ByteBuffer to, int width) {
        byte[] pixel = new byte[4];
        pixel[0] = (byte) 0xff;
        int bufferSize = buffer.remaining() / 3;
        // Read the OpenGL image row by row from right to left (flipped horizontally)
        for (int i = bufferSize - 1; i >= 0; i--) {
            // Coordinates in the final image
            int x = xOffset + bufferWidth - i % bufferWidth - 1; // X coord of OpenGL image has to be flipped first
            int y = yOffset + i / bufferWidth;
            // Write to image (row by row, left to right)
            buffer.get(pixel, 1, 3);
            to.position((y * width + x) * 4);
            to.put(pixel);
        }
        to.rewind();
    }
}
