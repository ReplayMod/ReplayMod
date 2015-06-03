package eu.crushedpixel.replaymod.video.entity.strategy;

import eu.crushedpixel.replaymod.utils.OpenGLUtils;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VanillaReadPixelsRenderingStrategy implements FrameRenderingStrategy {

    private final CustomEntityRenderer renderer;
    private final ByteBuffer buffer;

    public VanillaReadPixelsRenderingStrategy(CustomEntityRenderer renderer) {
        this.renderer = renderer;
        this.buffer = BufferUtils.createByteBuffer(renderer.resultWidth * renderer.resultHeight * 3);
    }

    @Override
    public void renderFrame(final float partialTicks, BufferedImage into, int x, int y) {
        pushMatrix();

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();

        renderer.withDisplaySize(renderer.resultWidth, renderer.resultHeight, new Runnable() {
            @Override
            public void run() {
                renderer.renderWorld(partialTicks, 0);
            }
        });

        popMatrix();

        buffer.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, renderer.resultWidth, renderer.resultHeight, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        buffer.rewind();

        OpenGLUtils.openGlBytesToBufferedImage(buffer, renderer.resultWidth, into, x, y);
    }

    @Override
    public void cleanup() {

    }
}
