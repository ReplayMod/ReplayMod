package eu.crushedpixel.replaymod.video.processor;

import eu.crushedpixel.replaymod.video.rendering.Frame;
import eu.crushedpixel.replaymod.video.rendering.FrameProcessor;

import java.io.IOException;

public abstract class AbstractFrameProcessor<R extends Frame, P extends Frame> implements FrameProcessor<R, P> {
    @Override
    public void close() throws IOException {
    }
}
