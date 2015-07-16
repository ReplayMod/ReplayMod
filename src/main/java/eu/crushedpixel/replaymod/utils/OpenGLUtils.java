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

    public static void openGlBytesToRBG(ByteBuffer buffer, int bufferWidth, int xOffset, int yOffset, ByteBuffer to, int width) {
        byte[] rowBuf = new byte[bufferWidth * 3];
        // Copy image flipped vertically to target buffer
        int rows = buffer.remaining() / 3 / bufferWidth;
        for (int i = 0; i < rows; i++) {
            buffer.get(rowBuf);
            to.position(((yOffset + rows - i - 1) * width + xOffset) * 3);
            to.put(rowBuf);
        }
        to.rewind();
    }
}
