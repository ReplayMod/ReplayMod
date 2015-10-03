package eu.crushedpixel.replaymod.gui.elements.timelines;

import eu.crushedpixel.replaymod.holders.Keyframe;

public class GuiKeyframeTimeline extends GuiTimeline {
    private static final int KEYFRAME_PLACE_X = 74;
    private static final int KEYFRAME_PLACE_Y = 20;
    private static final int KEYFRAME_TIME_X = 74;
    private static final int KEYFRAME_TIME_Y = 25;
    private static final int KEYFRAME_SPEC_X = 74;
    private static final int KEYFRAME_SPEC_Y = 30;

    private Keyframe clickedKeyFrame;
    private long clickTime;
    private boolean dragging;
    private boolean timeKeyframes;
    private boolean placeKeyframes;

    public GuiKeyframeTimeline(int positionX, int positionY, int width, int height, boolean showMarkers,
                               boolean showPlaceKeyframes, boolean showTimeKeyframes) {
        super(positionX, positionY, width, height);
        this.showMarkers = showMarkers;
        this.timeKeyframes = showTimeKeyframes;
        this.placeKeyframes = showPlaceKeyframes;
    }

    // TODO
//    @Override
//    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
//        if(!enabled) return false;
//
//        long time = getTimeAt(mouseX, mouseY);
//        if(time == -1) {
//            return false;
//        }
//
//        int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));
//
//        Keyframe closest;
//        if(mouseY >= positionY + BORDER_TOP + 5 && timeKeyframes) {
//            closest = ReplayHandler.getTimeKeyframes().getClosestKeyframeForTimestamp((int) time, tolerance);
//        } else if(mouseY >= positionY + BORDER_TOP && placeKeyframes) {
//            closest = ReplayHandler.getPositionKeyframes().getClosestKeyframeForTimestamp((int) time, tolerance);
//        } else {
//            closest = null;
//        }
//
//        //left mouse button
//        if(button == 0) {
//            ReplayHandler.selectKeyframe(closest); //can be null, deselects keyframe
//
//            // If we clicked on a key frame, then continue monitoring the mouse for movements
//            long currentTime = System.currentTimeMillis();
//            if(closest != null) {
//                if(currentTime - clickTime < 500) { // if double clicked then open GUI instead
//                    mc.displayGuiScreen(GuiEditKeyframe.create(closest));
//                    this.clickedKeyFrame = null;
//                } else {
//                    this.clickedKeyFrame = closest;
//                    this.dragging = false;
//                }
//            } else { // If we didn't then just update the cursor
//                ReplayHandler.setRealTimelineCursor((int) time);
//                this.dragging = true;
//            }
//            this.clickTime = currentTime;
//
//        } else if(button == 1) {
//            if(closest != null) {
//                if(closest.getValue() instanceof AdvancedPosition) {
//                    AdvancedPosition pos = (AdvancedPosition)closest.getValue();
//                    ReplayHandler.getCameraEntity().movePath(pos);
//                } else if(closest.getValue() instanceof TimestampValue) {
//                    ReplayMod.overlay.performJump(((TimestampValue)closest.getValue()).asInt());
//                }
//            }
//        }
//
//        return isHovering(mouseX, mouseY);
//    }
//
//    @Override
//    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
//        if(!enabled) return;
//        long time = getTimeAt(mouseX, mouseY);
//        if (time != -1) {
//            if (clickedKeyFrame != null) {
//                int tolerance = (int) (2 * Math.round(zoom * timelineLength / width));
//
//                if (dragging || Math.abs(clickedKeyFrame.getRealTimestamp() - time) > tolerance) {
//                    clickedKeyFrame.setRealTimestamp((int) time);
//                    ReplayHandler.getPositionKeyframes().sort();
//                    ReplayHandler.getTimeKeyframes().sort();
//                    dragging = true;
//                }
//            } else if (dragging) {
//                ReplayHandler.setRealTimelineCursor((int) time);
//            }
//        }
//    }
//
//    @Override
//    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
//        mouseDrag(mc, mouseX, mouseY, button);
//        clickedKeyFrame = null;
//        dragging = false;
//    }
//
//    @Override
//    public void draw(Minecraft mc, int mouseX, int mouseY) {
//        super.draw(mc, mouseX, mouseY);
//
//        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
//
//        long leftTime = Math.round(timeStart * timelineLength);
//        long rightTime = Math.round((timeStart + zoom) * timelineLength);
//
//        double segmentLength = timelineLength * zoom;
//
//        //iterate over keyframes to find spectator segments
//        if(placeKeyframes) {
//            ListIterator<Keyframe<AdvancedPosition>> iterator = ReplayHandler.getPositionKeyframes().listIterator();
//            while(iterator.hasNext()) {
//                Keyframe<AdvancedPosition> kf = iterator.next();
//
//                if(!(kf.getValue() instanceof SpectatorData))
//                    continue;
//
//                int i = iterator.nextIndex();
//                int nextSpectatorKeyframeRealTime = -1;
//
//                if (iterator.hasNext()) {
//                    Keyframe<AdvancedPosition> kf2 = iterator.next();
//
//                    if(kf2.getValue() instanceof SpectatorData) {
//                        SpectatorData spectatorData1 = (SpectatorData)kf.getValue();
//                        SpectatorData spectatorData2 = (SpectatorData)kf2.getValue();
//
//                        if(spectatorData1.getSpectatedEntityID() == spectatorData2.getSpectatedEntityID()) {
//                            nextSpectatorKeyframeRealTime = kf2.getRealTimestamp();
//                        }
//                    }
//                }
//
//                int i2 = iterator.previousIndex();
//
//                while(i2 >= i) {
//                    iterator.previous();
//                    i2--;
//                }
//
//                if(nextSpectatorKeyframeRealTime != -1) {
//                    int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);
//                    int nextX = getKeyframeX(nextSpectatorKeyframeRealTime, leftTime, bodyWidth, segmentLength);
//
//                    int color = ((SpectatorData)kf.getValue()).getSpectatingMethod().getColor();
//
//                    drawRect(Math.max(keyframeX + 2, positionX+BORDER_LEFT), positionY + BORDER_TOP + 1, Math.min(nextX - 2, positionX+width-BORDER_RIGHT+1),
//                            positionY + BORDER_TOP + 4, color);
//
//                    GlStateManager.color(1, 1, 1, 1);
//                }
//            }
//        }
//
//        //iterate over time keyframes to find negative replay speeds
//        if(timeKeyframes) {
//            ListIterator<Keyframe<TimestampValue>> iterator = ReplayHandler.getTimeKeyframes().listIterator();
//            while(iterator.hasNext()) {
//                Keyframe<TimestampValue> kf = iterator.next();
//
//                int i = iterator.nextIndex();
//                int nextTimeKeyframeRealTime = -1;
//
//                if (iterator.hasNext()) {
//                    Keyframe<TimestampValue> kf2 = iterator.next();
//                    if(kf.getValue().asInt() > kf2.getValue().asInt()) {
//                        nextTimeKeyframeRealTime = kf2.getRealTimestamp();
//                    }
//                }
//
//                int i2 = iterator.previousIndex();
//
//                while(i2 >= i) {
//                    iterator.previous();
//                    i2--;
//                }
//
//                if(nextTimeKeyframeRealTime != -1) {
//                    int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);
//                    int nextX = getKeyframeX(nextTimeKeyframeRealTime, leftTime, bodyWidth, segmentLength);
//
//                    drawGradientRect(Math.max(keyframeX + 2, positionX+BORDER_LEFT), positionY + BORDER_TOP + 6, Math.min(nextX - 2, positionX+width-BORDER_RIGHT+1),
//                            positionY + BORDER_TOP + 9, 0xFFFF0000, 0xFFFF0000);
//                }
//            }
//        }
//
//        drawTimelineCursor(leftTime, rightTime, bodyWidth);
//
//
//        //Draw Keyframe logos
//        for (Keyframe kf : ReplayHandler.getAllKeyframes()) {
//            if (kf != null && !kf.equals(ReplayHandler.getSelectedKeyframe()))
//                drawKeyframe(kf, bodyWidth, leftTime, rightTime, segmentLength);
//        }
//
//        if(ReplayHandler.getSelectedKeyframe() != null) {
//            drawKeyframe(ReplayHandler.getSelectedKeyframe(), bodyWidth, leftTime, rightTime, segmentLength);
//        }
//    }
//
//    private int getKeyframeX(int timestamp, long leftTime, int bodyWidth, double segmentLength) {
//        long positionInSegment = timestamp - leftTime;
//        double fractionOfSegment = positionInSegment / segmentLength;
//        return (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);
//    }
//
//    private void drawKeyframe(Keyframe kf, int bodyWidth, long leftTime, long rightTime, double segmentLength) {
//        if (kf.getRealTimestamp() <= rightTime && kf.getRealTimestamp() >= leftTime) {
//            int textureX;
//            int textureY;
//            int y = positionY;
//
//            int keyframeX = getKeyframeX(kf.getRealTimestamp(), leftTime, bodyWidth, segmentLength);
//
//            if(kf.getValue() instanceof AdvancedPosition) {
//                if(!placeKeyframes) return;
//                textureX = KEYFRAME_PLACE_X;
//                textureY = KEYFRAME_PLACE_Y;
//                y += 0;
//
//                //If Spectator Keyframe, use different texture
//                if(kf.getValue() instanceof SpectatorData) {
//                    textureX = KEYFRAME_SPEC_X;
//                    textureY = KEYFRAME_SPEC_Y;
//                }
//            } else if(kf.getValue() instanceof TimestampValue) {
//                if(!timeKeyframes) return;
//                textureX = KEYFRAME_TIME_X;
//                textureY = KEYFRAME_TIME_Y;
//                y += 5;
//            } else {
//                throw new UnsupportedOperationException("Unknown keyframe type: " + kf.getClass());
//            }
//
//            if (ReplayHandler.isSelected(kf)) {
//                textureX += 5;
//            }
//
//            rect(keyframeX - 2, y + BORDER_TOP, textureX, textureY, 5, 5);
//        }
//    }
}
