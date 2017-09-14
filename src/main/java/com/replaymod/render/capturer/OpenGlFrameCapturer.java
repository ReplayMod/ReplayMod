package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.utils.ByteBufferPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.WritableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public abstract class OpenGlFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;
    private Framebuffer frameBuffer;

    private final Minecraft mc = Minecraft.getMinecraft();

    public OpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        this.worldRenderer = worldRenderer;
        this.renderInfo = renderInfo;
    }

    protected final ReadableDimension frameSize = new ReadableDimension() {
        @Override
        public int getWidth() {
            return getFrameWidth();
        }

        @Override
        public int getHeight() {
            return getFrameHeight();
        }

        @Override
        public void getSize(WritableDimension dest) {
            dest.setSize(getWidth(), getHeight());
        }
    };

    protected int getFrameWidth() {
        return renderInfo.getFrameSize().getWidth();
    }

    protected int getFrameHeight() {
        return renderInfo.getFrameSize().getHeight();
    }

    protected Framebuffer frameBuffer() {
        if (frameBuffer == null) {
            frameBuffer = Minecraft.getMinecraft().getFramebuffer();
        }
        return frameBuffer;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames();
    }

    protected OpenGlFrame renderFrame(int frameId, float partialTicks) {
        return renderFrame(frameId, partialTicks, null);
    }

    protected OpenGlFrame renderFrame(int frameId, float partialTicks, D captureData) {
        resize(getFrameWidth(), getFrameHeight());

        GL11.glPushMatrix();
        frameBuffer().bindFramebuffer(true);

        GL11.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL_TEXTURE_2D);

        worldRenderer.renderWorld(partialTicks, captureData);

        frameBuffer().unbindFramebuffer();
        GL11.glPopMatrix();

        return captureFrame(frameId, captureData);
    }

    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        ByteBuffer buffer = ByteBufferPool.allocate(getFrameWidth() * getFrameHeight() * 3);
        if (OpenGlHelper.isFramebufferEnabled()) {
            frameBuffer().bindFramebufferTexture();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
            frameBuffer().unbindFramebufferTexture();
        } else {
            GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        }
        buffer.rewind();

        return new OpenGlFrame(frameId, new Dimension(getFrameWidth(), getFrameHeight()), buffer);
    }

    protected void resize(int width, int height) {
        if (width != mc.displayWidth || height != mc.displayHeight) {
            setWindowSize(width, height);
        }
    }

    private void setWindowSize(int width, int height) {
        mc.resize(width, height);
    }

    @Override
    public void close() throws IOException {
        worldRenderer.close();
    }
}
