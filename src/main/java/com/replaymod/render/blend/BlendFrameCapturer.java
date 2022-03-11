package com.replaymod.render.blend;

import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class BlendFrameCapturer implements FrameCapturer<BitmapFrame> {
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
    public Map<Channel, BitmapFrame> process() {
        if (framesDone == 0) {
            BlendState.getState().setup();
        }

        float tickDelta = renderInfo.updateForNextFrame();

        BlendState.getState().preFrame(framesDone);
        worldRenderer.renderWorld(MinecraftClient.getInstance().getTickDelta(), null);
        BlendState.getState().postFrame(framesDone);

        renderInfo.updatePostRender(tickDelta);

        BitmapFrame frame = new BitmapFrame(framesDone++, new Dimension(0, 0), 0, ByteBufferPool.allocate(0));
        return Collections.singletonMap(Channel.BRGA, frame);
    }

    @Override
    public void close() throws IOException {
        BlendState.getState().tearDown();
        BlendState.setState(null);
    }
}
