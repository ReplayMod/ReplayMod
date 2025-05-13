package com.replaymod.render.capturer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.WritableDimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.replaymod.core.versions.MCVer.popMatrix;
import static com.replaymod.core.versions.MCVer.pushMatrix;
import static com.replaymod.core.versions.MCVer.resizeMainWindow;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

//#if MC>=12105
//$$ import com.mojang.blaze3d.buffers.BufferType;
//$$ import com.mojang.blaze3d.buffers.BufferUsage;
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.systems.GpuDevice;
//#endif

public abstract class OpenGlFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;
    private Framebuffer frameBuffer;

    protected final MinecraftClient mc = MCVer.getMinecraft();

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
        resizeMainWindow(mc, getFrameWidth(), getFrameHeight());

        pushMatrix();
        //#if MC<12105
        frameBuffer().beginWrite(true);
        //#endif

        //#if MC>=12105
        //$$ RenderSystem.getDevice()
        //$$         .createCommandEncoder()
        //$$         .clearColorAndDepthTextures(mc.getFramebuffer().getColorAttachment(), 0, mc.getFramebuffer().getDepthAttachment(), 1);
        //#else
        GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                //#if MC>=11400 && MC<12102
                , false
                //#endif
        );
        //#endif
        //#if MC<11904
        GlStateManager.enableTexture();
        //#endif

        worldRenderer.renderWorld(partialTicks, captureData);

        //#if MC<12105
        frameBuffer().endWrite();
        //#endif
        popMatrix();

        return captureFrame(frameId, captureData);
    }

    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        ByteBuffer buffer = ByteBufferPool.allocate(getFrameWidth() * getFrameHeight() * 4);
        //#if MC>=12105
        //$$ GpuDevice device = RenderSystem.getDevice();
        //$$ try (GpuBuffer gpuBuffer = device.createBuffer(null, BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, getFrameWidth() * getFrameHeight() * 4)) {
        //$$     device.createCommandEncoder().copyTextureToBuffer(frameBuffer().getColorAttachment(), gpuBuffer, 0, () -> {}, 0);
        //$$     try (GpuBuffer.ReadView view = device.createCommandEncoder().readBuffer(gpuBuffer)) {
        //$$         buffer.put(view.data());
        //$$     }
        //$$ }
        //#else
        frameBuffer().beginWrite(true);
        GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buffer);
        frameBuffer().endWrite();
        //#endif
        buffer.rewind();

        return new OpenGlFrame(frameId, new Dimension(getFrameWidth(), getFrameHeight()), 4, buffer);
    }

    @Override
    public void close() throws IOException {
    }
}
