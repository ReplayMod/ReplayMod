package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;

public class CubicPboOpenGlFrameCapturer extends
        MultiFramePboOpenGlFrameCapturer<CubicOpenGlFrame, CubicOpenGlFrameCapturer.Data> {

    private final int frameSize;
    public CubicPboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        super(worldRenderer, renderInfo, CubicOpenGlFrameCapturer.Data.class, frameSize * frameSize);
        this.frameSize = frameSize;
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
    protected CubicOpenGlFrame create(OpenGlFrame[] from) {
        return new CubicOpenGlFrame(from[0], from[1], from[2], from[3], from[4], from[5]);
    }
}
