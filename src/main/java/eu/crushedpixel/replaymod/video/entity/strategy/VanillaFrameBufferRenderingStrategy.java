package eu.crushedpixel.replaymod.video.entity.strategy;

import com.google.gson.JsonSyntaxException;
import eu.crushedpixel.replaymod.utils.OpenGLUtils;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class VanillaFrameBufferRenderingStrategy implements FrameRenderingStrategy, CustomEntityRenderer.LoadShaderHook {

    private static final Logger logger = LogManager.getLogger();

    private final CustomEntityRenderer renderer;
    private final ByteBuffer buffer;
    private Framebuffer frameBuffer;

    public VanillaFrameBufferRenderingStrategy(CustomEntityRenderer renderer) {
        this.renderer = renderer;
        this.buffer = BufferUtils.createByteBuffer(renderer.resultWidth * renderer.resultHeight * 3);
    }

    @Override
    public void loadShader(ResourceLocation resourceLocation) {
        EntityRenderer proxied = renderer.proxied;
        try {
            ShaderGroup theShaderGroup = new ShaderGroup(renderer.mc.getTextureManager(), proxied.resourceManager, frameBuffer(), resourceLocation);
            theShaderGroup.createBindFramebuffers(renderer.resultWidth, renderer.resultHeight);
            proxied.theShaderGroup = theShaderGroup;
            proxied.useShader = true;
        } catch (IOException e) {
            logger.warn("Failed to load shader: " + resourceLocation, e);
            proxied.useShader = false;
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to load shader: " + resourceLocation, e);
            proxied.useShader = false;
        }
    }

    private Framebuffer frameBuffer() {
        if (frameBuffer == null) {
            frameBuffer = new Framebuffer(renderer.resultWidth, renderer.resultHeight, true);

            ShaderGroup theShaderGroup = renderer.proxied.theShaderGroup;
            if (theShaderGroup != null) {
                theShaderGroup.deleteShaderGroup();
                renderer.proxied.theShaderGroup = null;
            }
        }
        return frameBuffer;
    }

    @Override
    public void renderFrame(final float partialTicks, BufferedImage into, int x, int y) {
        pushMatrix();
        frameBuffer().bindFramebuffer(true);

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();

        renderer.withDisplaySize(renderer.resultWidth, renderer.resultHeight, new Runnable() {
            @Override
            public void run() {
                renderer.renderWorld(partialTicks, 0);
                if (OpenGlHelper.shadersSupported) {
                    EntityRenderer proxied = renderer.proxied;
                    if (proxied.theShaderGroup != null && proxied.useShader) {
                        matrixMode(GL_TEXTURE);

                        pushMatrix();
                        loadIdentity();
                        proxied.theShaderGroup.loadShaderGroup(partialTicks);
                        popMatrix();
                    }
                }
            }
        });

        frameBuffer().unbindFramebuffer();
        popMatrix();

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        buffer.clear();

        frameBuffer().bindFramebufferTexture();
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        frameBuffer().unbindFramebufferTexture();

        OpenGLUtils.openGlBytesToBufferedImage(buffer, renderer.resultWidth, into, x, y);
    }

    @Override
    public void cleanup() {
        if (frameBuffer != null) {
            frameBuffer.deleteFramebuffer();
        }
        if (renderer.proxied.theShaderGroup != null) {
            renderer.proxied.theShaderGroup.createBindFramebuffers(renderer.mc.displayWidth, renderer.mc.displayHeight);
        }
    }
}
