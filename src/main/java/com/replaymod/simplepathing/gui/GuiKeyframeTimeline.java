package com.replaymod.simplepathing.gui;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import org.lwjgl.util.ReadableDimension;

import javax.annotation.Nullable;

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

        for (Keyframe keyframe : Iterables.concat(Iterables.transform(mod.getCurrentTimeline().getPaths(), new Function<Path, Iterable<Keyframe>>() {
            @Nullable
            @Override
            public Iterable<Keyframe> apply(@Nullable Path input) {
                assert input != null;
                return input.getKeyframes();
            }
        }))) {
            if (keyframe.getTime() >= startTime && keyframe.getTime() <= endTime) {
                double relativeTime = keyframe.getTime() - startTime;
                int positonX = BORDER_LEFT + (int) (relativeTime / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2;
                int u = KEYFRAME_TEXTURE_X +
                        (mod.getSelectedTimeKeyframe() == keyframe || mod.getSelectedPositionKeyframe() == keyframe
                                ? KEYFRAME_SIZE : 0);
                int v = KEYFRAME_TEXTURE_Y;
                if (keyframe.getValue(CameraProperties.POSITION).isPresent()) {
                    if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
                        v += 2 * KEYFRAME_SIZE;
                    }
                    renderer.drawTexturedRect(positonX, BORDER_TOP, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
                if (keyframe.getValue(TimestampProperty.PROPERTY).isPresent()) {
                    v += KEYFRAME_SIZE;
                    renderer.drawTexturedRect(positonX, BORDER_TOP + KEYFRAME_SIZE, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
            }
        }

        // Draw colored quads on spectator path segments
        for (PathSegment segment : mod.getCurrentTimeline().getPaths().get(GuiPathing.POSITION_PATH).getSegments()) {
            if (segment.getInterpolator() == null
                    || !segment.getInterpolator().getKeyframeProperties().contains(SpectatorProperty.PROPERTY)) {
                continue; // Not a spectator segment
            }
            long startFrameTime = segment.getStartKeyframe().getTime();
            long endFrameTime = segment.getEndKeyframe().getTime();
            if (startFrameTime >= endTime || endFrameTime <= startTime) {
                continue; // Segment out of display range
            }

            double relativeStart = startFrameTime - startTime;
            double relativeEnd = endFrameTime - startTime;
            int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + KEYFRAME_SIZE / 2 + 1);
            int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2);
            if (startX < endX) {
                renderer.drawRect(startX + 1, BORDER_TOP + 1, endX - startX - 2, KEYFRAME_SIZE - 2, 0xFF0088FF);
            }
        }

        super.drawTimelineCursor(renderer, size);
    }

    @Override
    protected GuiKeyframeTimeline getThis() {
        return this;
    }
}
