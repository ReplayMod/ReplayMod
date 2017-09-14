package com.replaymod.core.utils;

import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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

    public static void drawRotatedRectWithCustomSizedTexture(int x, int y, float rotation, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        GL11.glPushMatrix();

        float f4 = 1.0F / textureWidth;
        float f5 = 1.0F / textureHeight;

        Tessellator tessellator = Tessellator.instance;
        GL11.glTranslatef(x+(width/2), y+(width/2), 0);
        GL11.glRotatef(rotation, 0, 0, 1);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-width / 2, height / 2, 0.0D, (double) (u * f4), (double) ((v + (float) height) * f5));
        tessellator.addVertexWithUV(width/2, height/2, 0.0D, (double)((u + (float)width) * f4), (double)((v + (float)height) * f5));
        tessellator.addVertexWithUV(width/2, -height/2, 0.0D, (double)((u + (float)width) * f4), (double)(v * f5));
        tessellator.addVertexWithUV(-width/2, -height/2, 0.0D, (double)(u * f4), (double)(v * f5));
        tessellator.draw();

        GL11.glPopMatrix();
    }
}
