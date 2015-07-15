package eu.crushedpixel.replaymod.video.capturer;

import org.lwjgl.util.ReadableDimension;

import java.io.Closeable;

public interface WorldRenderer<D extends CaptureData> extends Closeable {
    void renderWorld(ReadableDimension displaySize, float partialTicks, D data);
}
