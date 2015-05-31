package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.gui.GuiEditKeyframe;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;

public class GuiKeyframeTimeline extends GuiTimeline {
    private static final int KEYFRAME_PLACE_X = 74;
    private static final int KEYFRAME_PLACE_Y = 20;
    private static final int KEYFRAME_TIME_X = 74;
    private static final int KEYFRAME_TIME_Y = 25;

    private Keyframe clickedKeyFrame;
    private long clickTime;
    private boolean dragging;

    public GuiKeyframeTimeline(int positionX, int positionY, int width) {
        super(positionX, positionY, width);
        showMarkers = true;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        long time = getTimeAt(mouseX, mouseY);
        if (time == -1) {
            return;
        }

        int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

        Keyframe closest;
        if (mouseY >= positionY + BORDER_TOP + 5) {
            closest = ReplayHandler.getClosestTimeKeyframeForRealTime((int) time, tolerance);
        } else if (mouseY >= positionY + BORDER_TOP) {
            closest = ReplayHandler.getClosestPlaceKeyframeForRealTime((int) time, tolerance);
        } else {
            closest = null;
        }

        ReplayHandler.selectKeyframe(closest); //can be null, deselects keyframe

        // If we clicked on a key frame, then continue monitoring the mouse for movements
        long currentTime = System.currentTimeMillis();
        if (closest != null) {
            if (currentTime - clickTime < 500) { // if double clicked then open GUI instead
                mc.displayGuiScreen(new GuiEditKeyframe(closest));
                this.clickedKeyFrame = null;
            } else {
                this.clickedKeyFrame = closest;
                this.dragging = false;
            }
        } else { // If we didn't then just update the cursor
            ReplayHandler.setRealTimelineCursor((int) time);
            this.dragging = true;
        }
        this.clickTime = currentTime;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        long time = getTimeAt(mouseX, mouseY);
        if (time != -1) {
            if (clickedKeyFrame != null) {
                int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

                if (dragging || Math.abs(clickedKeyFrame.getRealTimestamp() - time) > tolerance) {
                    clickedKeyFrame.setRealTimestamp((int) time);
                    ReplayHandler.sortKeyframes();
                    dragging = true;
                }
            } else if (dragging) {
                ReplayHandler.setRealTimelineCursor((int) time);
            }
        }
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        mouseDrag(mc, mouseX, mouseY, button);
        clickedKeyFrame = null;
        dragging = false;
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        super.draw(mc, mouseX, mouseY);

        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;

        long leftTime = Math.round(timeStart * timelineLength);
        long rightTime = Math.round((timeStart + zoom) * timelineLength);

        double segmentLength = timelineLength * zoom;

        //Draw Keyframe logos
        for(Keyframe kf : ReplayHandler.getKeyframes()) {
            if (kf.getRealTimestamp() <= rightTime && kf.getRealTimestamp() >= leftTime) {
                int textureX;
                int textureY;
                int y = positionY;
                if (kf instanceof PositionKeyframe) {
                    textureX = KEYFRAME_PLACE_X;
                    textureY = KEYFRAME_PLACE_Y;
                    y += 0;
                } else if (kf instanceof TimeKeyframe) {
                    textureX = KEYFRAME_TIME_X;
                    textureY = KEYFRAME_TIME_Y;
                    y += 5;
                } else {
                    throw new UnsupportedOperationException("Unknown keyframe type: " + kf.getClass());
                }

                if (ReplayHandler.isSelected(kf)) {
                    textureX += 5;
                }

                long positionInSegment = kf.getRealTimestamp() - leftTime;
                double fractionOfSegment = positionInSegment / segmentLength;
                int x = (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);

                rect(x - 2, y + BORDER_TOP, textureX, textureY, 5, 5);
            }
        }
    }
}
