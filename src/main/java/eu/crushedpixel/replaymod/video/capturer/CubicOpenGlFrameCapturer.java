package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;

public class CubicOpenGlFrameCapturer extends OpenGlFrameCapturer<CubicOpenGlFrame, CubicOpenGlFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT, RIGHT, FRONT, BACK, TOP, BOTTOM
    }

    private final int frameSize;
    public CubicOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        super(worldRenderer, renderInfo);
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
    public CubicOpenGlFrame process() {
        float partialTicks = renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        return new CubicOpenGlFrame(renderFrame(frameId, partialTicks, Data.LEFT),
                renderFrame(frameId, partialTicks, Data.RIGHT),
                renderFrame(frameId, partialTicks, Data.FRONT),
                renderFrame(frameId, partialTicks, Data.BACK),
                renderFrame(frameId, partialTicks, Data.TOP),
                renderFrame(frameId, partialTicks, Data.BOTTOM));
    }
}
