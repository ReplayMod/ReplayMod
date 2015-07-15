package eu.crushedpixel.replaymod.video.capturer;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import org.lwjgl.util.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getTotalFrames();

    float updateForNextFrame();

    RenderOptions getRenderOptions();
}
