package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.PixelBufferObject;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SimplePboOpenGlFrameCapturer extends OpenGlFrameCapturer<OpenGlFrame, CaptureData> {
    private final int bufferSize;
    private PixelBufferObject pbo, otherPBO;

    public SimplePboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);

        ReadableDimension size = renderInfo.getFrameSize();
        bufferSize = size.getHeight() * size.getWidth() * 3;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        otherPBO = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    private void swapPBOs() {
        PixelBufferObject old = pbo;
        pbo = otherPBO;
        otherPBO = old;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames() + 2;
    }

    @Override
    public OpenGlFrame process() {
        OpenGlFrame frame = null;

        if (framesDone > 1) {
            // Read pbo to memory
            pbo.bind();
            ByteBuffer pboBuffer = pbo.mapReadOnly();
            ByteBuffer buffer = ByteBufferPool.allocate(bufferSize);
            buffer.put(pboBuffer);
            buffer.rewind();
            pbo.unmap();
            pbo.unbind();
            frame = new OpenGlFrame(framesDone - 2, frameSize, buffer);
        }

        if (framesDone < renderInfo.getTotalFrames()) {
            // Then fill it again
            renderFrame(framesDone, renderInfo.updateForNextFrame());
        }

        framesDone++;
        swapPBOs();
        return frame;
    }

    @Override
    protected OpenGlFrame captureFrame(int frameId, CaptureData data) {
        pbo.bind();

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        if (OpenGlHelper.isFramebufferEnabled()) {
            frameBuffer().bindFramebufferTexture();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
            frameBuffer().unbindFramebufferTexture();
        } else {
            GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0);
        }

        pbo.unbind();
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pbo.delete();
        otherPBO.delete();
    }
}
