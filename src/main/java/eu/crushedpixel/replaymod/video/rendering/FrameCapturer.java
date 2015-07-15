package eu.crushedpixel.replaymod.video.rendering;

import java.io.Closeable;

public interface FrameCapturer<R extends Frame> extends Closeable {

    boolean isDone();

    R process();

}
