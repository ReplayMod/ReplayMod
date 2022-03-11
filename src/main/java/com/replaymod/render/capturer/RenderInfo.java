package com.replaymod.render.capturer;

import com.replaymod.render.RenderSettings;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    float updateForNextFrame();

    void updatePostRender(float tickDelta);

    RenderSettings getRenderSettings();
}
