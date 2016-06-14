package com.replaymod.render.rendering;

import java.io.Closeable;

public interface FrameConsumer<P extends Frame> extends Closeable {

    void consume(P frame);

}
