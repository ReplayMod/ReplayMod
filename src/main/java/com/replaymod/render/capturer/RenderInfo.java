package com.replaymod.render.capturer;

import com.replaymod.render.RenderSettings;
import org.lwjgl.util.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getTotalFrames();

    float updateForNextFrame();

    RenderSettings getRenderSettings();
}
