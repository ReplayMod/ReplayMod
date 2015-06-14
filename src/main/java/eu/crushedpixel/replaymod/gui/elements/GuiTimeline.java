package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;

import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.TEXTURE_SIZE;
import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.replay_gui;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glEnable;

public class GuiTimeline extends Gui implements GuiElement {

    protected static final int TEXTURE_WIDTH = 64;
    protected static final int BORDER_TOP = 4;
    protected static final int BORDER_BOTTOM = 3;

    protected static final int HEIGHT = 22;
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
     * Whether to draw markers on the timeline in regular time intervals.
     * This draws the time at big markers above the timeline. Therefore extra space in negative y direction
     * should be kept empty if markers are desired.
     */
    public boolean showMarkers;

    protected final int positionX;
    protected final int positionY;
    protected final int width;

    public GuiTimeline(int positionX, int positionY, int width) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
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
        if (mouseX >= left && mouseX <= right && mouseY >= positionY && mouseY <= positionY + HEIGHT) {
            double segmentLength = timelineLength * zoom;
            double segmentTime =  segmentLength * (mouseX - left) / bodyWidth;
            return Math.round(timeStart * timelineLength + segmentTime);
        } else {
            return -1;
        }
    }

    public void draw(Minecraft mc, int mouseX, int mouseY) {
        int bodyLeft = positionX + BORDER_LEFT;
        int bodyRight = positionX + width - BORDER_RIGHT;
        int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;

        {
            // We have to increase the border size as there is one pixel row which is part of the border while drawing
            // but isn't during position calculations
            int BORDER_LEFT = GuiTimeline.BORDER_LEFT + 1;
            int BODY_WIDTH = GuiTimeline.BODY_WIDTH - 1;

            // Left border
            rect(positionX, positionY, TEXTURE_X, TEXTURE_Y, BORDER_LEFT, HEIGHT);
            // Body
            for (int i = bodyLeft; i < bodyRight; i += BODY_WIDTH) {
                rect(i, positionY, TEXTURE_X + BORDER_LEFT, TEXTURE_Y, Math.min(BODY_WIDTH, bodyRight - i), HEIGHT);
            }
            // Right border
            rect(bodyRight, positionY, TEXTURE_X + BORDER_LEFT + BODY_WIDTH, TEXTURE_Y, BORDER_RIGHT, HEIGHT);
        }

        long leftTime = Math.round(timeStart * timelineLength);
        long rightTime = Math.round((timeStart + zoom) * timelineLength);

        // Draw markers
        if (showMarkers) {
            int markerY = positionY + HEIGHT - BORDER_BOTTOM;
            MarkerType mt = MarkerType.getMarkerType(zoom, timelineLength);

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
                    String timestamp = String.format("%02d:%02ds", sec / 60, sec % 60);
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

            rect(cursorX - 2, positionY + BORDER_TOP, 84, 20, 5, 16);
        }
    }

    @Override
    public void drawOverlay(Minecraft mc, int mouseX, int mouseY) {
        // Draw time under cursor if the mouse is on the timeline
        long mouseTime = getTimeAt(mouseX, mouseY);
        if (mouseTime != -1) {
            long sec = mouseTime / 1000;
            String timestamp = String.format("%02d:%02ds", sec / 60, sec % 60);
            ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, timestamp, null, Color.WHITE);
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return getTimeAt(mouseX, mouseY) != -1;
    }

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {

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
        Minecraft.getMinecraft().renderEngine.bindTexture(replay_gui);
        glEnable(GL_BLEND);

        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }


    private enum MarkerType {

        ONE_S(1000, 100),
        FIVE_S(5 * 1000, 1000),
        QUARTER_M(15 * 1000, 3 * 1000),
        HALF_M(30 * 1000, 5 * 1000),
        ONE_M(60 * 1000, 10 * 1000),
        FIVE_M(5 * 60 * 1000, 50 * 1000);

        int distance;
        int smallDistance;
        int maximum = 10;

        MarkerType(int minimum, int smallDistance) {
            this.distance = minimum;
            this.smallDistance = smallDistance;
        }

        public static MarkerType getMarkerType(double scale, long totalLength) {
            long seconds = Math.round(totalLength * scale);

            for(MarkerType mt : values()) {
                if(seconds / mt.distance <= 10) {
                    return mt;
                }
            }

            return FIVE_M;
        }
    }
}
