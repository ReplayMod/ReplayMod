package com.replaymod.render.capturer;

import java.io.Closeable;

public interface WorldRenderer extends Closeable {
    void renderWorld(float partialTicks, CaptureData data);
    void setOmnidirectional(boolean omnidirectional);
}
