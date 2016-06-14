package com.replaymod.render.capturer;

import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;

public class StereoscopicPboOpenGlFrameCapturer
        extends MultiFramePboOpenGlFrameCapturer<StereoscopicOpenGlFrame, StereoscopicOpenGlFrameCapturer.Data> {

    public StereoscopicPboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo, StereoscopicOpenGlFrameCapturer.Data.class,
                renderInfo.getFrameSize().getWidth() / 2 * renderInfo.getFrameSize().getHeight());
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    protected StereoscopicOpenGlFrame create(OpenGlFrame[] from) {
        return new StereoscopicOpenGlFrame(from[0], from[1]);
    }
}
