package com.replaymod.render.capturer;

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

//#if MC>=11400
import com.replaymod.render.mixin.MainWindowAccessor;
import static com.replaymod.core.versions.MCVer.getWindow;
//#endif

//#if MC>=10800
import static com.mojang.blaze3d.platform.GlStateManager.*;
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

    private final MinecraftClient mc = MCVer.getMinecraft();

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
        frameBuffer().beginWrite(true);

        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                //#if MC>=11400
                , false
                //#endif
        );
        enableTexture();

        worldRenderer.renderWorld(partialTicks, captureData);

        frameBuffer().endWrite();
        popMatrix();

        return captureFrame(frameId, captureData);
    }

    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        ByteBuffer buffer = ByteBufferPool.allocate(getFrameWidth() * getFrameHeight() * 4);
        frameBuffer().beginWrite(true);
        GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buffer);
        frameBuffer().endWrite();
        buffer.rewind();

        return new OpenGlFrame(frameId, new Dimension(getFrameWidth(), getFrameHeight()), 4, buffer);
    }

    protected void resize(int width, int height) {
        //#if MC>=11400
        Framebuffer fb = mc.getFramebuffer();
        if (fb.viewportWidth != width || fb.viewportHeight != height) {
            fb.resize(width, height
                    //#if MC>=11400
                    , false
                    //#endif
            );
        }
        //noinspection ConstantConditions
        MainWindowAccessor mainWindow = (MainWindowAccessor) (Object) getWindow(mc);
        mainWindow.setFramebufferWidth(width);
        mainWindow.setFramebufferHeight(height);
        //#if MC>=11500
        mc.gameRenderer.onResized(width, height);
        //#endif
        //#else
        //$$ if (width != mc.displayWidth || height != mc.displayHeight) {
        //$$     mc.resize(width, height);
        //$$ }
        //#endif
    }

    @Override
    public void close() throws IOException {
    }
}
