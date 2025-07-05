package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.Frame;
import com.replaymod.render.utils.ByteBufferPool;
import com.replaymod.render.utils.PixelBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

//#if MC>=12105
//#if MC<12106
//$$ import com.mojang.blaze3d.buffers.BufferType;
//$$ import com.mojang.blaze3d.buffers.BufferUsage;
//#endif
//$$ import com.mojang.blaze3d.buffers.GpuBuffer;
//$$ import com.mojang.blaze3d.systems.CommandEncoder;
//$$ import com.mojang.blaze3d.systems.GpuDevice;
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#endif

public abstract class PboOpenGlFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData>
        extends OpenGlFrameCapturer<F, D> {
    private final boolean withDepth;
    private final D[] data;
    //#if MC>=12105
    //$$ private GpuBuffer pbo, otherPBO;
    //#else
    private PixelBufferObject pbo, otherPBO;
    //#endif

    public PboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        withDepth = renderInfo.getRenderSettings().isDepthMap();
        data = type.getEnumConstants();
        int bufferSize = framePixels * (4 /* bgra */ + (withDepth ? 4 /* float */ : 0)) * data.length;
        //#if MC>=12106
        //$$ pbo = RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
        //$$ otherPBO = RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
        //#elseif MC>=12105
        //$$ pbo = RenderSystem.getDevice().createBuffer(null, BufferType.PIXEL_PACK, BufferUsage.STREAM_READ, bufferSize);
        //$$ otherPBO = RenderSystem.getDevice().createBuffer(null, BufferType.PIXEL_PACK, BufferUsage.STREAM_READ, bufferSize);
        //#else
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        otherPBO = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        //#endif
    }

    protected abstract F create(OpenGlFrame[] from);

    private void swapPBOs() {
        //#if MC>=12105
        //$$ GpuBuffer old = pbo;
        //#else
        PixelBufferObject old = pbo;
        //#endif
        pbo = otherPBO;
        otherPBO = old;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames() + 2;
    }

    private F readFromPbo(ByteBuffer pboBuffer, int bytesPerPixel, boolean swapRB) {
        OpenGlFrame[] frames = new OpenGlFrame[data.length];
        int frameBufferSize = getFrameWidth() * getFrameHeight() * bytesPerPixel;
        for (int i = 0; i < frames.length; i++) {
            ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            if (swapRB) {
                for (int j = 0; j < frameBufferSize; j += 4) {
                    byte r = pboBuffer.get();
                    byte g = pboBuffer.get();
                    byte b = pboBuffer.get();
                    byte a = pboBuffer.get();
                    frameBuffer.put(b);
                    frameBuffer.put(g);
                    frameBuffer.put(r);
                    frameBuffer.put(a);
                }
            } else {
                frameBuffer.put(pboBuffer);
            }
            frameBuffer.rewind();
            frames[i] = new OpenGlFrame(framesDone - 2, frameSize, bytesPerPixel, frameBuffer);
        }
        return create(frames);
    }

    @Override
    public Map<Channel, F> process() {
        Map<Channel, F> channels = null;

        if (framesDone > 1) {
            // Read pbo to memory
            //#if MC>=12105
            //#if MC>=12106
            //$$ try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(pbo, true, false)) {
            //#else
            //$$ try (GpuBuffer.ReadView view = RenderSystem.getDevice().createCommandEncoder().readBuffer(pbo)) {
            //#endif
            //$$     channels = new HashMap<>();
            //$$     channels.put(Channel.BRGA, readFromPbo(view.data(), 4, true));
            //$$     if (withDepth) {
            //$$         channels.put(Channel.DEPTH, readFromPbo(view.data(), 4, false));
            //$$     }
            //$$ }
            //#else
            pbo.bind();
            ByteBuffer pboBuffer = pbo.mapReadOnly();

            channels = new HashMap<>();
            channels.put(Channel.BRGA, readFromPbo(pboBuffer, 4, false));
            if (withDepth) {
                channels.put(Channel.DEPTH, readFromPbo(pboBuffer, 4, false));
            }

            pbo.unmap();
            pbo.unbind();
            //#endif
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
        return channels;
    }

    @Override
    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        //#if MC>=12105
        //$$ int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        //$$ CommandEncoder cmd = RenderSystem.getDevice().createCommandEncoder();
        //$$ cmd.copyTextureToBuffer(frameBuffer().getColorAttachment(), pbo, offset, () -> {}, 0);
        //$$ if (withDepth) {
        //$$     offset += data.length * getFrameWidth() * getFrameHeight() * 4;
        //$$     cmd.copyTextureToBuffer(frameBuffer().getDepthAttachment(), pbo, offset, () -> {}, 0);
        //$$ }
        //#else
        pbo.bind();

        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        frameBuffer().beginWrite(true);
        GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, offset);
        if (withDepth) {
            offset += data.length * getFrameWidth() * getFrameHeight() * 4;
            GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, offset);
        }
        frameBuffer().endWrite();

        pbo.unbind();
        //#endif
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pbo.close();
        otherPBO.close();
    }
}
