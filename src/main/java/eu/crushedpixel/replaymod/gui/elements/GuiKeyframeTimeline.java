package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.GuiEditKeyframe;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.MarkerKeyframe;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;

import java.util.ListIterator;

public class GuiKeyframeTimeline extends GuiTimeline {
    private static final int KEYFRAME_PLACE_X = 74;
    private static final int KEYFRAME_PLACE_Y = 20;
    private static final int KEYFRAME_TIME_X = 74;
    private static final int KEYFRAME_TIME_Y = 25;
    private static final int KEYFRAME_SPEC_X = 74;
    private static final int KEYFRAME_SPEC_Y = 30;
    private static final int KEYFRAME_MARKER_X = 40;
    private static final int KEYFRAME_MARKER_Y = 39;

    private Keyframe clickedKeyFrame;
    private long clickTime;
    private boolean dragging;
    private boolean markerKeyframes;
    private boolean timeKeyframes;
    private boolean placeKeyframes;

    public GuiKeyframeTimeline(int positionX, int positionY, int width, boolean showMarkers,
                               boolean showMarkerKeyframes, boolean showPlaceKeyframes, boolean showTimeKeyframes) {
        super(positionX, positionY, width);
        this.showMarkers = showMarkers;
        this.markerKeyframes = showMarkerKeyframes;
        this.timeKeyframes = showTimeKeyframes;
        this.placeKeyframes = showPlaceKeyframes;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        long time = getTimeAt(mouseX, mouseY);
        if (time == -1) {
            return;
        }

        int tolerance = (int)(2 * Math.round(zoom * timelineLength / width));

        Keyframe closest;
        if (mouseY >= positionY + BORDER_TOP + 5 && timeKeyframes) {
            closest = ReplayHandler.getClosestTimeKeyframeForRealTime((int) time, tolerance);
        } else if (mouseY >= positionY + BORDER_TOP && placeKeyframes) {
            closest = ReplayHandler.getClosestPlaceKeyframeForRealTime((int) time, tolerance);
        } else if (mouseY >= positionY + BORDER_TOP + 10 && markerKeyframes) {
            closest = ReplayHandler.getClosestMarkerForRealTime((int) time, tolerance);
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
        if(clickedKeyFrame instanceof MarkerKeyframe && dragging == false) {
            ReplayMod.replaySender.jumpToTime(clickedKeyFrame.getRealTimestamp());
            ReplayHandler.setLastPosition(((MarkerKeyframe)clickedKeyFrame).getPosition());
        }
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

        //iterate over keyframes to find spectator segments
        ListIterator<Keyframe> iterator = ReplayHandler.getKeyframes().listIterator();
        while(iterator.hasNext()) {
            Keyframe kf = iterator.next();

            if(!(kf instanceof PositionKeyframe) || ((PositionKeyframe)kf).getSpectatedEntityID() == null) continue;

            int i = iterator.nextIndex();
            int nextSpectatorKeyframeRealTime = -1;

            while(iterator.hasNext()) {
                Keyframe kf2 = iterator.next();

                if(kf2 instanceof PositionKeyframe) {
                    if(((PositionKeyframe) kf).getSpectatedEntityID()
                            .equals(((PositionKeyframe) kf2).getSpectatedEntityID())) {

                        nextSpectatorKeyframeRealTime = kf2.getRealTimestamp();
                    }
                    break;
                }
            }

            int i2 = iterator.previousIndex();

            while(i2 >= i) {
                iterator.previous();
                i2--;
            }

            if(nextSpectatorKeyframeRealTime != -1) {
                int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);
                int nextX = getKeyframeX(nextSpectatorKeyframeRealTime, leftTime, bodyWidth, segmentLength);

                drawGradientRect(keyframeX + 2, positionY + BORDER_TOP + 1, nextX - 2, positionY + BORDER_TOP + 4, 0xFF0080FF, 0xFF0080FF);
            }
        }

        drawTimelineCursor(leftTime, rightTime, bodyWidth);


        //Draw Keyframe logos
        iterator = ReplayHandler.getKeyframes().listIterator();
        while(iterator.hasNext()) {
            Keyframe kf = iterator.next();

            if(!kf.equals(ReplayHandler.getSelectedKeyframe()))
                drawKeyframe(kf, bodyWidth, leftTime, rightTime, segmentLength);
        }

        if(ReplayHandler.getSelectedKeyframe() != null) {
            drawKeyframe(ReplayHandler.getSelectedKeyframe(), bodyWidth, leftTime, rightTime, segmentLength);
        }
    }

    private int getKeyframeX(int timestamp, long leftTime, int bodyWidth, double segmentLength) {
        long positionInSegment = timestamp - leftTime;
        double fractionOfSegment = positionInSegment / segmentLength;
        return (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);
    }

    private void drawKeyframe(Keyframe kf, int bodyWidth, long leftTime, long rightTime, double segmentLength) {
        if (kf.getRealTimestamp() <= rightTime && kf.getRealTimestamp() >= leftTime) {
            int textureX;
            int textureY;
            int y = positionY;

            int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);

            if(kf instanceof PositionKeyframe) {
                if(!placeKeyframes) return;
                textureX = KEYFRAME_PLACE_X;
                textureY = KEYFRAME_PLACE_Y;
                y += 0;

                //If Spectator Keyframe, use different texture
                if(((PositionKeyframe) kf).getSpectatedEntityID() != null) {
                    textureX = KEYFRAME_SPEC_X;
                    textureY = KEYFRAME_SPEC_Y;
                }
            } else if(kf instanceof TimeKeyframe) {
                if(!timeKeyframes) return;
                textureX = KEYFRAME_TIME_X;
                textureY = KEYFRAME_TIME_Y;
                y += 5;
            } else if(kf instanceof MarkerKeyframe) {
                if(!markerKeyframes) return;
                textureX = KEYFRAME_MARKER_X;
                textureY = KEYFRAME_MARKER_Y;
                y += 10;
            } else {
                throw new UnsupportedOperationException("Unknown keyframe type: " + kf.getClass());
            }

            if (ReplayHandler.isSelected(kf)) {
                textureX += 5;
            }

            rect(keyframeX - 2, y + BORDER_TOP, textureX, textureY, 5, 5);
        }
    }
}
