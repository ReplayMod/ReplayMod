package com.replaymod.simplepathing.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import org.lwjgl.util.ReadableDimension;

public class GuiKeyframeTimeline extends AbstractGuiTimeline<GuiKeyframeTimeline> {
    protected static final int KEYFRAME_SIZE = 5;
    protected static final int KEYFRAME_TEXTURE_X = 74;
    protected static final int KEYFRAME_TEXTURE_Y = 20;

    private final GuiPathing gui;

    public GuiKeyframeTimeline(GuiPathing gui) {
        this.gui = gui;
    }

    @Override
    protected void drawTimelineCursor(GuiRenderer renderer, ReadableDimension size) {
        ReplayModSimplePathing mod = gui.getMod();

        int width = size.getWidth();
        int visibleWidth = width - BORDER_LEFT - BORDER_RIGHT;
        int startTime = getOffset();
        int visibleTime = (int) (getZoom() * getLength());
        int endTime = getOffset() + visibleTime;

        renderer.bindTexture(ReplayMod.TEXTURE);

        for (Keyframe keyframe : mod.getCurrentTimeline().getPaths().get(0).getKeyframes()) {
            if (keyframe.getTime() >= startTime && keyframe.getTime() <= endTime) {
                double relativeTime = keyframe.getTime() - startTime;
                int positonX = BORDER_LEFT + (int) (relativeTime / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2;
                int u = KEYFRAME_TEXTURE_X + (mod.getSelectedKeyframe() == keyframe ? KEYFRAME_SIZE : 0);
                int v = KEYFRAME_TEXTURE_Y;
                if (keyframe.getValue(CameraProperties.POSITION).isPresent()) {
                    renderer.drawTexturedRect(positonX, BORDER_TOP, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
                if (keyframe.getValue(TimestampProperty.PROPERTY).isPresent()) {
                    v += KEYFRAME_SIZE;
                    renderer.drawTexturedRect(positonX, BORDER_TOP + KEYFRAME_SIZE, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
            }
        }

        super.drawTimelineCursor(renderer, size);
    }

    @Override
    protected GuiKeyframeTimeline getThis() {
        return this;
    }
}
