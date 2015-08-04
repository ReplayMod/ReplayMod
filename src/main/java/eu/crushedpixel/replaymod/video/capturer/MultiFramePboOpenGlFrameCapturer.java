package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.opengl.PixelBufferObject;
import eu.crushedpixel.replaymod.utils.ByteBufferPool;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;
import eu.crushedpixel.replaymod.video.rendering.Frame;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class MultiFramePboOpenGlFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData>
        extends OpenGlFrameCapturer<F, D> {
    private final D[] data;
    private PixelBufferObject pbo, otherPBO;

    public MultiFramePboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        data = type.getEnumConstants();
        int bufferSize = framePixels * 3 * data.length;
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
            int frameBufferSize = getFrameWidth() * getFrameHeight() * 3;
            for (int i = 0; i < frames.length; i++) {
                ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
                pboBuffer.limit(pboBuffer.position() + frameBufferSize);
                frameBuffer.put(pboBuffer);
                frameBuffer.rewind();
                frames[i] = new OpenGlFrame(framesDone - 2, frameSize, frameBuffer);
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

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 3;
        if (OpenGlHelper.isFramebufferEnabled()) {
            frameBuffer().bindFramebufferTexture();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, offset);
            frameBuffer().unbindFramebufferTexture();
        } else {
            GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, offset);
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
