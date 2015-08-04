package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;

public class SimpleOpenGlFrameCapturer extends OpenGlFrameCapturer<OpenGlFrame, CaptureData> {

    public SimpleOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    public OpenGlFrame process() {
        float partialTicks = renderInfo.updateForNextFrame();
        return renderFrame(framesDone++, partialTicks);
    }
}
