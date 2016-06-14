package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;

public class StereoscopicOpenGlFrameCapturer
        extends OpenGlFrameCapturer<StereoscopicOpenGlFrame, StereoscopicOpenGlFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT_EYE, RIGHT_EYE
    }

    public StereoscopicOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    public StereoscopicOpenGlFrame process() {
        float partialTicks = renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        OpenGlFrame left = renderFrame(frameId, partialTicks, Data.LEFT_EYE);
        OpenGlFrame right = renderFrame(frameId, partialTicks, Data.RIGHT_EYE);
        return new StereoscopicOpenGlFrame(left, right);
    }
}
