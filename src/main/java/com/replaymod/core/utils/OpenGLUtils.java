package com.replaymod.core.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;

public class OpenGLUtils {
    public static final int VIEWPORT_MAX_WIDTH;
    public static final int VIEWPORT_MAX_HEIGHT;

    static {
        IntBuffer buffer = BufferUtils.createIntBuffer(16);
        //#if MC>=11300
        // FIXME GL11.glGetIntegerv(GL11.GL_MAX_VIEWPORT_DIMS, buffer);
        buffer.put(0xffff);
        buffer.put(0xffff);
        //#else
        //$$ GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS, buffer);
        //#endif
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
}
