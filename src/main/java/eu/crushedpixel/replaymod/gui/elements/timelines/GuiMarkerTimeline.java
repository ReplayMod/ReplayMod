package eu.crushedpixel.replaymod.gui.elements.timelines;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.holders.MarkerKeyframe;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Point;

import java.awt.*;

public class GuiMarkerTimeline extends GuiTimeline {
    private static final int KEYFRAME_MARKER_X = 109;
    private static final int KEYFRAME_MARKER_Y = 20;

    private MarkerKeyframe clickedKeyFrame;
    private long clickTime;
    private boolean dragging;

    public GuiMarkerTimeline(int positionX, int positionY, int width, boolean showMarkers) {
        super(positionX, positionY, width);
        this.showMarkers = showMarkers;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if(!enabled) return;

        //left mouse button
        if(button == 0) {
            long time = getTimeAt(mouseX, mouseY);
            if(time == -1) {
                return;
            }

            int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

            MarkerKeyframe closest = null;
            if(mouseY >= positionY + BORDER_TOP + 10) {
                closest = ReplayHandler.getClosestMarkerForRealTime((int) time, tolerance);
            }

            ReplayHandler.selectMarkerKeyframe(closest);

            if(closest == null) { //if no keyframe clicked, jump in time
                ReplayMod.overlay.performJump(getTimeAt(mouseX, mouseY));
            } else {
                // If we clicked on a key frame, then continue monitoring the mouse for movements
                long currentTime = System.currentTimeMillis();
                if(closest != null) {
                    if(currentTime - clickTime < 500) { // if double clicked then open GUI instead
                        //TODO: Make Marker Name editable
                        this.clickedKeyFrame = null;
                    } else {
                        this.clickedKeyFrame = closest;
                        this.dragging = false;
                    }
                } else { // If we didn't then just update the cursor
                    this.dragging = true;
                }
                this.clickTime = currentTime;
            }

        } else if(button == 1) {

            long time = getTimeAt(mouseX, mouseY);
            if(time == -1) {
                return;
            }

            int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

            MarkerKeyframe closest = null;
            if(mouseY >= positionY + BORDER_TOP + 10) {
                closest = ReplayHandler.getClosestMarkerForRealTime((int) time, tolerance);
            }

            if(closest != null) {
                //Jump to clicked Marker Keyframe
                ReplayHandler.setLastPosition(closest.getPosition());
                ReplayMod.replaySender.jumpToTime(closest.getRealTimestamp());
            }
        }
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        if(!enabled) return;
        long time = getTimeAt(mouseX, mouseY);
        if (time != -1) {
            if (clickedKeyFrame != null) {
                int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

                if (dragging || Math.abs(clickedKeyFrame.getRealTimestamp() - time) > tolerance) {
                    clickedKeyFrame.setRealTimestamp((int) time);
                    dragging = true;
                }
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

        drawTimelineCursor(leftTime, rightTime, bodyWidth);

        //Draw Keyframe logos
        for (MarkerKeyframe kf : ReplayHandler.getMarkers()) {
            if (kf != null && !kf.equals(ReplayHandler.getSelectedMarkerKeyframe()))
                drawKeyframe(kf, bodyWidth, leftTime, rightTime, segmentLength);
        }

        if(ReplayHandler.getSelectedMarkerKeyframe() != null) {
            drawKeyframe(ReplayHandler.getSelectedMarkerKeyframe(), bodyWidth, leftTime, rightTime, segmentLength);
        }
    }

    private int getKeyframeX(int timestamp, long leftTime, int bodyWidth, double segmentLength) {
        long positionInSegment = timestamp - leftTime;
        double fractionOfSegment = positionInSegment / segmentLength;
        return (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        boolean drawn = false;

        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;

        long leftTime = Math.round(timeStart * timelineLength);
        double segmentLength = timelineLength * zoom;

        for(MarkerKeyframe marker : ReplayHandler.getMarkers()) {
            int keyframeX = getKeyframeX(marker.getRealTimestamp(), leftTime, bodyWidth, segmentLength);

            if(MouseUtils.isMouseWithinBounds(keyframeX - 2, this.positionY + BORDER_TOP + 10 + 1, 5, 5)) {
                Point mouse = MouseUtils.getMousePos();
                String markerName = marker.getName();
                if(markerName == null) markerName = I18n.format("replaymod.gui.ingame.unnamedmarker");
                ReplayMod.tooltipRenderer.drawTooltip(mouse.getX(), mouse.getY(), markerName, null, Color.WHITE);

                drawn = true;
            }
        }

        if(!drawn) {
            super.drawOverlay(mc, mouseX, mouseY);
        }
    }

    private void drawKeyframe(MarkerKeyframe kf, int bodyWidth, long leftTime, long rightTime, double segmentLength) {
        if (kf.getRealTimestamp() <= rightTime && kf.getRealTimestamp() >= leftTime) {
            int textureX = KEYFRAME_MARKER_X;
            int textureY = KEYFRAME_MARKER_Y;
            int y = positionY+10;

            int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);

            if (ReplayHandler.isSelected(kf)) {
                textureX += 5;
            }

            rect(keyframeX - 2, y + BORDER_TOP, textureX, textureY, 5, 5);
        }
    }
}