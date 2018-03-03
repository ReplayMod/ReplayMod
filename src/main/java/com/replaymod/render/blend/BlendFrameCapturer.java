package com.replaymod.render.blend;

import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.utils.ByteBufferPool;
import net.minecraft.client.Minecraft;
import org.lwjgl.util.Dimension;

import java.io.IOException;

public class BlendFrameCapturer implements FrameCapturer<RGBFrame> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;

    public BlendFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        this.worldRenderer = worldRenderer;
        this.renderInfo = renderInfo;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames();
    }

    @Override
    public RGBFrame process() {
        if (framesDone == 0) {
            BlendState.getState().setup();
        }

        renderInfo.updateForNextFrame();

        BlendState.getState().preFrame(framesDone);
        worldRenderer.renderWorld(Minecraft.getMinecraft().timer.renderPartialTicks, null);
        BlendState.getState().postFrame(framesDone);

        return new RGBFrame(framesDone++, new Dimension(0, 0), ByteBufferPool.allocate(0));
    }

    @Override
    public void close() throws IOException {
        BlendState.getState().tearDown();
        BlendState.setState(null);
    }
}
