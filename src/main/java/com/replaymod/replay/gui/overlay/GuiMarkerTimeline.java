package com.replaymod.replay.gui.overlay;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.util.Location;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import de.johni0702.minecraft.gui.function.Draggable;
import net.minecraft.client.resources.I18n;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import static de.johni0702.minecraft.gui.utils.Utils.clamp;

public class GuiMarkerTimeline extends AbstractGuiTimeline<GuiMarkerTimeline> implements Draggable {
    protected static final int TEXTURE_MARKER_X = 109;
    protected static final int TEXTURE_MARKER_Y = 20;
    protected static final int TEXTURE_MARKER_SELECTED_X = 114;
    protected static final int TEXTURE_MARKER_SELECTED_Y = 20;
    protected static final int MARKER_SIZE = 5;

    private final ReplayHandler replayHandler;

    private ReadableDimension lastSize;

    private Marker selectedMarker;
    private int draggingStartX;
    private int draggingTimeDelta;
    private boolean dragging;
    private long lastClickTime;

    public GuiMarkerTimeline(ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
    }

    @Override
    protected GuiMarkerTimeline getThis() {
        return this;
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        lastSize = size;
        super.draw(renderer, size, renderInfo);

        drawMarkers(renderer, size);
    }

    protected void drawMarkers(GuiRenderer renderer, ReadableDimension size) {
        renderer.bindTexture(ReplayMod.TEXTURE);

        for (Marker marker : replayHandler.getMarkers()) {
            drawMarker(renderer, size, marker);
        }
    }

    protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker) {
        int visibleLength = (int) (getLength() * getZoom());
        int markerPos = clamp(marker.getTime(), getOffset(), getOffset() + visibleLength);
        double positionInVisible = markerPos - getOffset();
        double fractionOfVisible = positionInVisible / visibleLength;
        int markerX = (int) (BORDER_LEFT + fractionOfVisible * (size.getWidth() - BORDER_LEFT - BORDER_RIGHT));

        int textureX, textureY;
        if (marker.equals(selectedMarker)) {
            textureX = TEXTURE_MARKER_SELECTED_X;
            textureY = TEXTURE_MARKER_SELECTED_Y;
        } else {
            textureX = TEXTURE_MARKER_X;
            textureY = TEXTURE_MARKER_Y;
        }
        renderer.drawTexturedRect(markerX - 2, size.getHeight() - BORDER_BOTTOM - MARKER_SIZE,
                textureX, textureY, MARKER_SIZE, MARKER_SIZE);
    }


    /**
     * Returns the marker which the mouse is at.
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     * @return The marker or {@code null} if the mouse isn't on a marker
     */
    protected Marker getMarkerAt(int mouseX, int mouseY) {
        if (lastSize == null) {
            return null;
        }
        Point mouse = new Point(mouseX, mouseY);
        getContainer().convertFor(this, mouse);
        mouseX = mouse.getX();
        mouseY = mouse.getY();

        if (mouseX < 0 || mouseY < lastSize.getHeight() - BORDER_BOTTOM - MARKER_SIZE
                || mouseX > lastSize.getWidth() || mouseY > lastSize.getHeight() - BORDER_BOTTOM) {
            return null;
        }

        int visibleLength = (int) (getLength() * getZoom());
        int contentWidth = lastSize.getWidth() - BORDER_LEFT - BORDER_RIGHT;
        for (Marker marker : replayHandler.getMarkers()) {
            int markerPos = clamp(marker.getTime(), getOffset(), getOffset() + visibleLength);
            double positionInVisible = markerPos - getOffset();
            double fractionOfVisible = positionInVisible / visibleLength;
            int markerX = (int) (BORDER_LEFT + fractionOfVisible * contentWidth);
            if (Math.abs(markerX - mouseX) < 3) {
                return marker;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        Marker marker = getMarkerAt(position.getX(), position.getY());
        if (marker != null) {
            if (button == 0) { // Left click
                long now = System.currentTimeMillis();
                selectedMarker = marker;
                if (Math.abs(lastClickTime - now) > 500) { // Single click
                    draggingStartX = position.getX();
                    draggingTimeDelta = marker.getTime() - getTimeAt(position.getX(), position.getY());
                } else { // Double click
                    new GuiEditMarkerPopup(replayHandler, getContainer(), marker).open();
                }
                lastClickTime = now;
            } else if (button == 1) { // Right click
                selectedMarker = null;
                replayHandler.setTargetPosition(new Location(
                        marker.getX(), marker.getY(), marker.getZ(),
                        marker.getPitch(), marker.getYaw()
                ));
                replayHandler.doJump(marker.getTime(), false);
            }
            return true;
        } else {
            selectedMarker = null;
        }
        return super.mouseClick(position, button);
    }

    @Override
    public boolean mouseDrag(ReadablePoint position, int button, long timeSinceLastCall) {
        if (selectedMarker != null) {
            int diff = position.getX() - draggingStartX;
            if (Math.abs(diff) > MARKER_SIZE) {
                dragging = true;
            }
            if (dragging) {
                int timeAt = getTimeAt(position.getX(), position.getY());
                if (timeAt != -1) {
                    selectedMarker.setTime(draggingTimeDelta + timeAt);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseRelease(ReadablePoint position, int button) {
        if (selectedMarker != null) {
            mouseDrag(position, button, 0);
            if (dragging) {
                dragging = false;
                replayHandler.saveMarkers();
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getTooltipText(RenderInfo renderInfo) {
        Marker marker = getMarkerAt(renderInfo.mouseX, renderInfo.mouseY);
        if (marker != null) {
            return marker.getName() != null ? marker.getName() : I18n.format("replaymod.gui.ingame.unnamedmarker");
        }
        return super.getTooltipText(renderInfo);
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }
}
