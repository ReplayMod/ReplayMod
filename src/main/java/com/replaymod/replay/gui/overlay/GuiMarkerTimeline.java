package com.replaymod.replay.gui.overlay;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.util.Location;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import de.johni0702.minecraft.gui.function.Draggable;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.resource.language.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static de.johni0702.minecraft.gui.utils.Utils.clamp;

public class GuiMarkerTimeline extends AbstractGuiTimeline<GuiMarkerTimeline> implements Draggable, Typeable {
    protected static final int TEXTURE_MARKER_X = 109;
    protected static final int TEXTURE_MARKER_Y = 20;
    protected static final int TEXTURE_MARKER_SELECTED_X = 114;
    protected static final int TEXTURE_MARKER_SELECTED_Y = 20;
    protected static final int MARKER_SIZE = 5;

    @Nullable
    private final ReplayHandler replayHandler;
    private final Consumer<Set<Marker>> saveMarkers;
    protected Set<Marker> markers;

    private ReadableDimension lastSize;

    private Marker selectedMarker;
    private int draggingStartX;
    private int draggingTimeDelta;
    private boolean dragging;
    private long lastClickTime;

    public GuiMarkerTimeline(@Nonnull ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
        try {
            this.markers = replayHandler.getReplayFile().getMarkers().or(HashSet::new);
        } catch (IOException e) {
            ReplayModReplay.LOGGER.error("Failed to get markers from replay", e);
            this.markers = new HashSet<>();
        }
        this.saveMarkers = (markers) -> {
            try {
                replayHandler.getReplayFile().writeMarkers(markers);
            } catch (IOException e) {
                ReplayModReplay.LOGGER.error("Failed to save markers to replay", e);
            }
        };
    }

    public GuiMarkerTimeline(Set<Marker> markers, Consumer<Set<Marker>> saveMarkers) {
        this.replayHandler = null;
        this.markers = markers;
        this.saveMarkers = saveMarkers;
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

        for (Marker marker : markers) {
            drawMarker(renderer, size, marker);
        }
    }

    protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker) {
        int visibleLength = (int) (getLength() * getZoom());
        int markerPos = clamp(marker.getTime(), getOffset(), getOffset() + visibleLength);
        double positionInVisible = markerPos - getOffset();
        double fractionOfVisible = positionInVisible / visibleLength;
        int markerX = (int) (BORDER_LEFT + fractionOfVisible * (size.getWidth() - BORDER_LEFT - BORDER_RIGHT));
        drawMarker(renderer, size, marker, markerX);
    }

    protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker, int markerX) {
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
        for (Marker marker : markers) {
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
                    new GuiEditMarkerPopup(getContainer(), marker, (updatedMarker) -> {
                        markers.remove(marker);
                        markers.add(updatedMarker);
                        saveMarkers.accept(markers);
                    }).open();
                }
                lastClickTime = now;
            } else if (button == 1) { // Right click
                selectedMarker = null;
                if (replayHandler != null) {
                    CameraEntity cameraEntity = replayHandler.getCameraEntity();
                    if (cameraEntity != null) {
                        cameraEntity.setCameraPosRot(new Location(
                                marker.getX(), marker.getY(), marker.getZ(),
                                marker.getYaw(), marker.getPitch()
                        ));
                    }
                    replayHandler.doJump(marker.getTime(), true);
                } else {
                    setCursorPosition(marker.getTime());
                }
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
                saveMarkers.accept(markers);
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getTooltipText(RenderInfo renderInfo) {
        Marker marker = getMarkerAt(renderInfo.mouseX, renderInfo.mouseY);
        if (marker != null) {
            return marker.getName() != null ? marker.getName() : I18n.translate("replaymod.gui.ingame.unnamedmarker");
        }
        return super.getTooltipText(renderInfo);
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == Keyboard.KEY_DELETE && selectedMarker != null) {
            markers.remove(selectedMarker);
            saveMarkers.accept(markers);
            return true;
        }
        return false;
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
        saveMarkers.accept(markers);
    }
}
