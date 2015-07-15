package eu.crushedpixel.replaymod.video.rendering;

import java.io.Closeable;

public interface FrameConsumer<P extends Frame> extends Closeable {

    void consume(P frame);

}
