package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.PixelBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class MultiFramePboOpenGlFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData>
        extends OpenGlFrameCapturer<F, D> {
    private final D[] data;
    private PixelBufferObject pbo, otherPBO;

    public MultiFramePboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        data = type.getEnumConstants();
        int bufferSize = framePixels * 4 * data.length;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        otherPBO = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    protected abstract F create(OpenGlFrame[] from);

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
    public F process() {
        F frame = null;

        if (framesDone > 1) {
            // Read pbo to memory
            pbo.bind();
            ByteBuffer pboBuffer = pbo.mapReadOnly();

            OpenGlFrame[] frames = new OpenGlFrame[data.length];
            int frameBufferSize = getFrameWidth() * getFrameHeight() * 4;
            for (int i = 0; i < frames.length; i++) {
                ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
                pboBuffer.limit(pboBuffer.position() + frameBufferSize);
                frameBuffer.put(pboBuffer);
                frameBuffer.rewind();
                frames[i] = new OpenGlFrame(framesDone - 2, frameSize, 4, frameBuffer);
            }

            pbo.unmap();
            pbo.unbind();

            frame = create(frames);
        }

        if (framesDone < renderInfo.getTotalFrames()) {
            float partialTicks = renderInfo.updateForNextFrame();
            // Then fill it again
            for (D data : this.data) {
                renderFrame(framesDone, partialTicks, data);
            }
        }

        framesDone++;
        swapPBOs();
        return frame;
    }

    @Override
    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        pbo.bind();

        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        frameBuffer().beginWrite(true);
        GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, offset);
        frameBuffer().endWrite();

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
