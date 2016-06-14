package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
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
        GlStateManager.pushMatrix();

        float f4 = 1.0F / textureWidth;
        float f5 = 1.0F / textureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.translate(x+(width/2), y+(width/2), 0);
        GlStateManager.rotate(rotation, 0, 0, 1);
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertexWithUV(-width / 2, height / 2, 0.0D, (double) (u * f4), (double) ((v + (float) height) * f5));
        worldrenderer.addVertexWithUV(width/2, height/2, 0.0D, (double)((u + (float)width) * f4), (double)((v + (float)height) * f5));
        worldrenderer.addVertexWithUV(width/2, -height/2, 0.0D, (double)((u + (float)width) * f4), (double)(v * f5));
        worldrenderer.addVertexWithUV(-width/2, -height/2, 0.0D, (double)(u * f4), (double)(v * f5));
        tessellator.draw();

        GlStateManager.popMatrix();
    }
}
