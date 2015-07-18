package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;
import eu.crushedpixel.replaymod.video.rendering.Frame;
import eu.crushedpixel.replaymod.video.rendering.FrameCapturer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.WritableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public abstract class OpenGlFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer<D> worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;
    private Framebuffer frameBuffer;

    public OpenGlFrameCapturer(WorldRenderer<D> worldRenderer, RenderInfo renderInfo) {
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
            frameBuffer = new Framebuffer(getFrameWidth(), getFrameHeight(), true);
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
        pushMatrix();
        frameBuffer().bindFramebuffer(true);

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();

        worldRenderer.renderWorld(frameSize, partialTicks, captureData);

        frameBuffer().unbindFramebuffer();
        popMatrix();

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

    @Override
    public void close() throws IOException {
        worldRenderer.close();
        if (frameBuffer != null) {
            frameBuffer.deleteFramebuffer();
        }
    }
}
