package eu.crushedpixel.replaymod.gui.elements.timelines;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.GuiElement;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;

import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.TEXTURE_SIZE;
import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.replay_gui;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glEnable;

public class GuiTimeline extends Gui implements GuiElement {

    protected static final int TEXTURE_WIDTH = 64;
    protected static final int TEXTURE_HEIGHT = 22;
    protected static final int BORDER_TOP = 4;
    protected static final int BORDER_BOTTOM = 3;

    protected static final int TEXTURE_X = 64;
    protected static final int TEXTURE_Y = 106;

    protected static final int BORDER_LEFT = 4;
    protected static final int BORDER_RIGHT = 4;
    protected static final int BODY_WIDTH = TEXTURE_WIDTH - BORDER_LEFT - BORDER_RIGHT;

    /**
     * Current position of the cursor. Should normally be between 0 and {@link #timelineLength}.
     */
    public int cursorPosition;

    /**
     * Total length of the timeline.
     */
    public int timelineLength;

    /**
     * The time at the left border of this timeline (fraction of the total length).
     */
    public double timeStart;

    /**
     * Zoom of this timeline. 1/10 allows the user to see 1/10 of the total length.
     */
    public double zoom = 1;

    /**
     * The smallest value <code>zoom</code> might have.
     */
    public double minZoom = 0.005;

    /**
     * The step size of zooming in/out
     */
    public double zoomStep = 0.05;

    /**
     * Where the more detailed part of the timeline begins
     */

    public double detailedZoom = 0.05;

    /**
     * The step size for the more detailed part of the timeline
     */
    public double smallZoomStep = 0.005;

    /**
     * Whether to draw markers on the timeline in regular time intervals.
     * This draws the time at big markers above the timeline. Therefore extra space in negative y direction
     * should be kept empty if markers are desired.
     */
    public boolean showMarkers;

    protected final int positionX;
    protected final int positionY;
    protected final int width;
    protected final int height;

    protected boolean enabled = true;

    public GuiTimeline(int positionX, int positionY, int width, int height) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the time which the mouse is at.
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     * @return The time or -1 if the mouse isn't on the timeline
     */
    public long getTimeAt(int mouseX, int mouseY) {
        int left = positionX + BORDER_LEFT;
        int right = positionX + width - BORDER_RIGHT;
        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
        if (mouseX >= left && mouseX <= right && mouseY >= positionY && mouseY <= positionY + height) {
            double segmentLength = timelineLength * zoom;
            double segmentTime =  segmentLength * (mouseX - left) / bodyWidth;
            return Math.round(timeStart * timelineLength + segmentTime);
        } else {
            return -1;
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean hovered) {
        draw(mc, mouseX, mouseY);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        int bodyLeft = positionX + BORDER_LEFT;
        int bodyRight = positionX + width - BORDER_RIGHT;
        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;

        {
            // We have to increase the border size as there is one pixel row which is part of the border while drawing
            // but isn't during position calculations
            int BORDER_LEFT = GuiTimeline.BORDER_LEFT + 1;
            int BODY_WIDTH = GuiTimeline.BODY_WIDTH - 1;

            /*
            // Upper left border
            rect(positionX, positionY, TEXTURE_X, TEXTURE_Y, BORDER_LEFT, BORDER_TOP);

            // Lower left border
            rect(positionX, positionY+height-BORDER_BOTTOM, TEXTURE_X, TEXTURE_Y+TEXTURE_HEIGHT-BORDER_BOTTOM, BORDER_LEFT, BORDER_BOTTOM);
            */

            boolean leftRightDrawn = false;

            for (int i = positionX; i < bodyRight; i += BODY_WIDTH) {
                int textureX = leftRightDrawn ? TEXTURE_X+BORDER_LEFT : TEXTURE_X;

                // Upper border
                rect(i, positionY, textureX, TEXTURE_Y, Math.min(BODY_WIDTH, bodyRight - i), BORDER_TOP);

                int remaining = height-BORDER_TOP-BORDER_BOTTOM;
                int y = positionY + BORDER_TOP;
                while(remaining > 0) {
                    int toDraw = Math.min(remaining, TEXTURE_HEIGHT - BORDER_TOP - BORDER_BOTTOM);
                    rect(i, y, textureX, TEXTURE_Y+BORDER_TOP, Math.min(BODY_WIDTH, bodyRight - i), Math.min(TEXTURE_HEIGHT-BORDER_TOP-BORDER_BOTTOM, toDraw));

                    // Right border
                    if(!leftRightDrawn) {
                        rect(bodyLeft+bodyWidth, y, TEXTURE_X+TEXTURE_WIDTH-BORDER_RIGHT, TEXTURE_Y+BORDER_TOP, BORDER_RIGHT, Math.min(TEXTURE_HEIGHT-BORDER_TOP-BORDER_BOTTOM, toDraw));
                    }

                    y += toDraw;
                    remaining -= toDraw;
                }

                // Lower border
                rect(i, positionY+height-BORDER_BOTTOM, textureX, TEXTURE_Y+TEXTURE_HEIGHT-BORDER_BOTTOM, Math.min(BODY_WIDTH, bodyRight - i), BORDER_BOTTOM);

                leftRightDrawn = true;
            }

            // Upper right corner
            rect(bodyLeft+bodyWidth, positionY, TEXTURE_X+TEXTURE_WIDTH-BORDER_RIGHT, TEXTURE_Y, BORDER_RIGHT, BORDER_TOP);

            // Lower right corner
            rect(bodyLeft+bodyWidth, positionY+height-BORDER_BOTTOM, TEXTURE_X+TEXTURE_WIDTH-BORDER_RIGHT, TEXTURE_Y+TEXTURE_HEIGHT-BORDER_BOTTOM, BORDER_RIGHT, BORDER_BOTTOM);
        }

        long leftTime = Math.round(timeStart * timelineLength);
        long rightTime = Math.round((timeStart + zoom) * timelineLength);

        // Draw markers
        if (showMarkers) {
            int markerY = positionY + height - BORDER_BOTTOM;
            Markers mt = Markers.getMarkers(zoom, timelineLength, bodyWidth);

            // Small markers
            for (int s = 0; s <= timelineLength; s += mt.smallDistance) {
                if (s <= rightTime && s >= leftTime) {
                    long positionInSegment = s - leftTime;
                    double fractionOfSegment = positionInSegment / (zoom * timelineLength);
                    int markerX = (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);

                    drawVerticalLine(markerX, markerY - 3, markerY, 0xffffffff);
                }
            }

            // Big markers
            for (int s = 0; s <= timelineLength; s += mt.distance) {
                if (s <= rightTime && s >= leftTime) {
                    long positionInSegment = s - leftTime;
                    double fractionOfSegment = positionInSegment / (zoom * timelineLength);
                    int markerX = (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);

                    drawVerticalLine(markerX, markerY - 7, markerY, 0xffc0c0c0); // Light gray

                    // Write time above the timeline
                    long sec = Math.round(s / 1000.0);
                    String timestamp = String.format("%02d:%02d", sec / 60, sec % 60);
                    drawCenteredString(mc.fontRendererObj, timestamp, markerX, positionY - 8, 0xffffffff);
                }
            }
        }

        drawTimelineCursor(leftTime, rightTime, bodyWidth);
    }

    protected void drawTimelineCursor(long leftTime, long rightTime, int bodyWidth) {
        // Draw the cursor if it's on the current timeline segment
        if (cursorPosition >= leftTime && cursorPosition <= rightTime) {
            long positionInSegment = cursorPosition - leftTime;
            double fractionOfSegment = positionInSegment / (zoom * timelineLength);
            int cursorX = (int) (positionX + BORDER_LEFT + fractionOfSegment * bodyWidth);

            rect(cursorX - 2, positionY + BORDER_TOP-1, 84, 20, 5, 4);


            int remaining = height-BORDER_TOP-BORDER_BOTTOM-3;
            int y = positionY + BORDER_TOP-1 + 4;
            while(remaining > 0) {
                int toDraw = Math.min(remaining, 11);

                rect(cursorX - 2, y, 84, 24, 5, toDraw);

                y += toDraw;
                remaining -= toDraw;
            }

        }
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        // Draw time under cursor if the mouse is on the timeline
        long mouseTime = getTimeAt(mouseX, mouseY);
        if (mouseTime != -1) {
            long sec = mouseTime / 1000;
            String timestamp = String.format("%02d:%02d", sec / 60, sec % 60);
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, timestamp, null, Color.WHITE);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return getTimeAt(mouseX, mouseY) != -1;
    }

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        return isHovering(mouseX, mouseY);
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {

    }

    @Override
    public void buttonPressed(Minecraft mc, int mouseX, int mouseY, char key, int keyCode) {

    }

    @Override
    public void tick(Minecraft mc) {

    }

    protected void rect(int x, int y, int u, int v, int width, int height) {
        if(!enabled) {
            GlStateManager.color(Color.GRAY.getRed() / 255f, Color.GRAY.getGreen() / 255f, Color.GRAY.getBlue() / 255f, 1f);
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(replay_gui);
        glEnable(GL_BLEND);

        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    public void zoomIn() {
        if(zoom-zoomStep < detailedZoom) {
            zoom = Math.max(minZoom, zoom - smallZoomStep);
            timeStart = Math.min(timeStart, 1f - smallZoomStep);
        } else {
            zoom = Math.max(minZoom, zoom - zoomStep);
            timeStart = Math.min(timeStart, 1f - zoomStep);
        }
    }

    public void zoomOut() {
        if(zoom+smallZoomStep <= detailedZoom) {
            zoom = Math.min(1, zoom+smallZoomStep);
        } else {
            zoom = Math.min(1, zoom+zoomStep);
        }
    }

    @Data
    @AllArgsConstructor
    public static class Markers {

        static final int S = 1000;
        static final int M = 60*1000;

        static final int[] snapNumbers = new int[]{S, 2*S, 5*S, 10*S, 15*S, 20*S, 30*S, M, 2*M,
            5*M, 10*M, 15*M, 30*M};

        static final int MARKER_DISTANCE = 40;

        int smallDistance;
        int distance;

        public static Markers getMarkers(double scale, long totalLength, int pixelWidth) {

            //amount of seconds visible on timeline
            int visible = (int)(scale*totalLength);

            int bigMarkerCount = pixelWidth/MARKER_DISTANCE;

            int bigMarkerDistance = visible / bigMarkerCount;
            int snap = RoundUtils.getClosestInt(bigMarkerDistance, snapNumbers);

            bigMarkerDistance = RoundUtils.roundToMultiple(bigMarkerDistance, snap);

            return new Markers(bigMarkerDistance/4, bigMarkerDistance);
        }
    }

    @Override
    public void setElementEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int xPos() {
        return positionX;
    }

    @Override
    public int yPos() {
        return positionY;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void xPos(int x) {}

    @Override
    public void yPos(int y) {}

    @Override
    public void width(int width) {}

    @Override
    public void height(int height) {}
}
