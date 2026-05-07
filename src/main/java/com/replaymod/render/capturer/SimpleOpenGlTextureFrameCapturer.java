package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlTextureFrame;
import com.replaymod.render.rendering.Channel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Collections;
import java.util.Map;

public class SimpleOpenGlTextureFrameCapturer
        extends OpenGlFrameCapturer<OpenGlTextureFrame, SimpleOpenGlTextureFrameCapturer.SinglePass> {

    public SimpleOpenGlTextureFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    public Map<Channel, OpenGlTextureFrame> process() {
        float partialTicks = renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        return Collections.singletonMap(Channel.BRGA,
                (OpenGlTextureFrame) renderFrame(frameId, partialTicks, SinglePass.SINGLE_PASS));
    }

    @Override
    protected OpenGlTextureFrame captureFrame(int frameId, SinglePass captureData) {
        //#if MC>=12105
        //$$ throw new UnsupportedOperationException("Native OpenGL texture encoding is not wired for Minecraft 1.21.5+ GPU texture handles yet.");
        //#else
        frameBuffer().beginWrite(true);
        int textureId = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        frameBuffer().endWrite();
        if (textureId == 0) {
            throw new IllegalStateException("Framebuffer has no OpenGL color texture attachment.");
        }
        return new OpenGlTextureFrame(frameId, frameSize, GL11.GL_TEXTURE_2D, textureId);
        //#endif
    }

    public enum SinglePass implements CaptureData {
        SINGLE_PASS
    }
}
