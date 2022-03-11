package com.replaymod.render.capturer;

import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.rendering.Channel;

import java.util.Collections;
import java.util.Map;

public class CubicOpenGlFrameCapturer extends OpenGlFrameCapturer<CubicOpenGlFrame, CubicOpenGlFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT, RIGHT, FRONT, BACK, TOP, BOTTOM
    }

    private final int frameSize;
    public CubicOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        super(worldRenderer, renderInfo);
        this.frameSize = frameSize;
        worldRenderer.setOmnidirectional(true);
    }

    @Override
    protected int getFrameWidth() {
        return frameSize;
    }

    @Override
    protected int getFrameHeight() {
        return frameSize;
    }

    @Override
    public Map<Channel, CubicOpenGlFrame> process() {
        float partialTicks = renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        CubicOpenGlFrame frame =  new CubicOpenGlFrame(renderFrame(frameId, partialTicks, Data.LEFT),
                renderFrame(frameId, partialTicks, Data.RIGHT),
                renderFrame(frameId, partialTicks, Data.FRONT),
                renderFrame(frameId, partialTicks, Data.BACK),
                renderFrame(frameId, partialTicks, Data.TOP),
                renderFrame(frameId, partialTicks, Data.BOTTOM));
        renderInfo.updatePostRender(partialTicks);
        return Collections.singletonMap(Channel.BRGA, frame);
    }
}
