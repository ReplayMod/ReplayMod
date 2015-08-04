package eu.crushedpixel.replaymod.video.capturer;

import org.lwjgl.util.ReadableDimension;

import java.io.Closeable;

public interface WorldRenderer extends Closeable {
    void renderWorld(ReadableDimension displaySize, float partialTicks, CaptureData data);
}
