package com.replaymod.render.capturer;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.WritableDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;

//#if MC>=10800
import static net.minecraft.client.renderer.GlStateManager.*;
//#else
//$$ import static com.replaymod.core.versions.MCVer.GlStateManager.*;
//#endif

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public abstract class OpenGlFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;
    private Framebuffer frameBuffer;

    private final Minecraft mc = MCVer.getMinecraft();

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
            frameBuffer = mc.getFramebuffer();
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

        pushMatrix();
        frameBuffer().bindFramebuffer(true);

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();

        worldRenderer.renderWorld(partialTicks, captureData);

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

    protected void resize(int width, int height) {
        //#if MC>=11300
        Framebuffer fb = mc.getFramebuffer();
        if (fb.framebufferWidth != width || fb.framebufferHeight != height) {
            fb.createFramebuffer(width, height);
        }
        mc.mainWindow.framebufferWidth = width;
        mc.mainWindow.framebufferHeight = height;
        //#else
        //$$ if (width != mc.displayWidth || height != mc.displayHeight) {
        //$$     mc.resize(width, height);
        //$$ }
        //#endif
    }

    @Override
    public void close() throws IOException {
        worldRenderer.close();
    }
}
