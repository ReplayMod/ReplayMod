package com.replaymod.render.rendering;

import java.io.Closeable;

public interface FrameCapturer<R extends Frame> extends Closeable {

    boolean isDone();

    R process();

}
