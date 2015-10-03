package com.replaymod.replay.gui.overlay;

import de.johni0702.replaystudio.data.Marker;
import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.timelines.GuiTimeline;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import com.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Point;

import java.awt.*;

public class GuiMarkerTimeline extends GuiTimeline {
    private static final int KEYFRAME_MARKER_X = 109;
    private static final int KEYFRAME_MARKER_Y = 20;

    private final ReplayHandler replayHandler;
    private Marker selectedMarker;
    private Marker clickedMarker;
    private long clickTime;
    private boolean dragging;

    public GuiMarkerTimeline(ReplayHandler replayHandler, int positionX, int positionY, int width, int height) {
        super(positionX, positionY, width, height);
        this.replayHandler = replayHandler;
        this.showMarkers = false;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if(!enabled) return false;

        long time = getTimeAt(mouseX, mouseY);
        if(time == -1) {
            return false;
        }

        int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

        Marker closest = null;
        if(mouseY >= positionY + BORDER_TOP + 10) {
            long distance = tolerance;
            for (Marker m : replayHandler.getMarkers()) {
                long d = Math.abs(m.getTime() - time);
                if (d <= distance) {
                    closest = m;
                    distance = d;
                }
            }
        }

        //left mouse button
        if(button == 0) {
            if(closest == null) { //if no keyframe clicked, jump in time
                replayHandler.doJump((int) getTimeAt(mouseX, mouseY), true);
            } else {
                selectedMarker = closest;
                // If we clicked on a key frame, then continue monitoring the mouse for movements
                long currentTime = System.currentTimeMillis();
                if(currentTime - clickTime < 500) { // if double clicked then open GUI instead
//                    mc.displayGuiScreen(GuiEditKeyframe.create(closest));
                    // TODO Edit Gui
                    this.clickedMarker = null;
                } else {
                    this.clickedMarker = closest;
                    this.dragging = false;
                }
                this.clickTime = currentTime;
            }

        } else if(button == 1) {
            if(closest != null) {
                //Jump to clicked Marker Keyframe (explicitly force to jump to this position)
                replayHandler.setTargetPosition(new AdvancedPosition(
                        closest.getX(), closest.getY(), closest.getZ(),
                        closest.getPitch(), closest.getYaw(), closest.getRoll()
                ));

                //perform the jump, telling the Overlay not to override the last position value
                replayHandler.doJump(closest.getTime(), false);
            }
        }

        return isHovering(mouseX, mouseY);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        if(!enabled) return;
        long time = getTimeAt(mouseX, mouseY);
        if (time != -1) {
            if (clickedMarker != null) {
                int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));

                if (dragging || Math.abs(clickedMarker.getTime() - time) > tolerance) {
                    clickedMarker.setTime((int) time);
                    dragging = true;
                }
            }
        }
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        mouseDrag(mc, mouseX, mouseY, button);
        clickedMarker = null;
        if (dragging) {
            replayHandler.saveMarkers();
            dragging = false;
        }
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
        for (Marker marker : replayHandler.getMarkers()) {
            drawMarker(marker, bodyWidth, leftTime, rightTime, segmentLength);
        }
    }

    private int getMarkerX(int timestamp, long leftTime, int bodyWidth, double segmentLength) {
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

        for (Marker marker : replayHandler.getMarkers()) {
            int markerX = getMarkerX(marker.getTime(), leftTime, bodyWidth, segmentLength);

            if(MouseUtils.isMouseWithinBounds(markerX - 2, this.positionY + BORDER_TOP + 10 + 1, 5, 5)) {
                Point mouse = MouseUtils.getMousePos();
                String markerName = marker.getName();
                if(markerName == null || markerName.isEmpty()) markerName = I18n.format("replaymod.gui.ingame.unnamedmarker");
                ReplayMod.tooltipRenderer.drawTooltip(mouse.getX(), mouse.getY(), markerName, null, Color.WHITE);

                drawn = true;
            }
        }

        if(!drawn) {
            super.drawOverlay(mc, mouseX, mouseY);
        }
    }

    private void drawMarker(Marker marker, int bodyWidth, long leftTime, long rightTime, double segmentLength) {
        if (marker.getTime() <= rightTime && marker.getTime() >= leftTime) {
            int textureX = KEYFRAME_MARKER_X;
            int textureY = KEYFRAME_MARKER_Y;
            int y = positionY+10;

            int keyframeX = getMarkerX(marker.getTime(), leftTime, bodyWidth, segmentLength);

            if (selectedMarker == marker) {
                textureX += 5;
            }

            rect(keyframeX - 2, y + BORDER_TOP, textureX, textureY, 5, 5);
        }
    }
}