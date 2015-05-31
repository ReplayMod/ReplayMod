package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.TEXTURE_SIZE;
import static eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay.replay_gui;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glEnable;

public class GuiScrollbar extends Gui {

    protected static final int BORDER_LEFT = 1;
    protected static final int BORDER_RIGHT = 2;
    protected static final int BORDER_TOP = 1;

    protected static final int BACKGROUND_WIDTH = 64;
    protected static final int BACKGROUND_HEIGHT = 9;
    protected static final int BACKGROUND_BODY_WIDTH = BACKGROUND_WIDTH - BORDER_LEFT - BORDER_RIGHT;
    protected static final int BACKGROUND_TEXTURE_X = 64;
    protected static final int BACKGROUND_TEXTURE_Y = 97;

    protected static final int SLIDER_WIDTH = 62;
    protected static final int SLIDER_HEIGHT = 7;
    protected static final int SLIDER_TEXTURE_X = 64;
    protected static final int SLIDER_TEXTURE_Y = 90;
    protected static final int SLIDER_BORDER_LEFT = 1;
    protected static final int SLIDER_BORDER_RIGHT = 2;
    protected static final int SLIDER_BODY_WIDTH = SLIDER_WIDTH - SLIDER_BORDER_LEFT - SLIDER_BORDER_RIGHT;

    /**
     * Current position of the left end of the slider. Must be between 0 and 1 - {@link #size} (inclusive).
     */
    public double sliderPosition;

    /**
     * Size of the slider. Should be between 0 (exclusive) and 1 (inclusive)
     */
    public double size = 1;

    protected final int positionX;
    protected final int positionY;
    protected final int width;

    private int draggingStart = -1;
    private double draggingStartPosition;

    public GuiScrollbar(int positionX, int positionY, int width) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
    }

    private int getSliderOffsetX() {
        return (int) Math.round((width - BORDER_LEFT - BORDER_RIGHT) * sliderPosition) + BORDER_LEFT;
    }

    public boolean startDragging(int mouseX, int mouseY) {
        int offsetX = getSliderOffsetX();
        int minX = positionX + offsetX;
        int maxX = positionX + offsetX + (int) (width * size);
        int minY = positionY + BORDER_TOP;
        int maxY = minY + SLIDER_HEIGHT;
        if (mouseX >= minX && mouseY >= minY && mouseX < maxX && mouseY < maxY) {
            draggingStart = mouseX;
            draggingStartPosition = sliderPosition;
            return true;
        } else {
            return false;
        }
    }

    public void doDragging(int mouseX) {
        if (draggingStart != -1) {
            double delta = (double) (mouseX - draggingStart) / (width - BORDER_LEFT - BORDER_RIGHT);
            sliderPosition = Math.max(0, Math.min(1 - size, draggingStartPosition + delta));
        }
    }

    public void endDragging(int mouseX) {
        doDragging(mouseX);
        draggingStart = -1;
    }

    public void draw(Minecraft mc) {
        GlStateManager.resetColor();
        mc.renderEngine.bindTexture(replay_gui);
        glEnable(GL_BLEND);

        // Background
        {
            // We have to increase the border size as there is one pixel row which is part of the border while drawing
            // but isn't during position calculations
            int BORDER_LEFT = GuiScrollbar.BORDER_LEFT + 1;
            int BACKGROUND_BODY_WIDTH = GuiScrollbar.BACKGROUND_BODY_WIDTH - 1;

            int bodyLeft = positionX + BORDER_LEFT;
            int bodyRight = positionX + width - BORDER_RIGHT;

            // Left border
            rect(positionX, positionY, BACKGROUND_TEXTURE_X, BACKGROUND_TEXTURE_Y, BORDER_LEFT, BACKGROUND_HEIGHT);
            // Body
            for (int i = bodyLeft; i < bodyRight; i += BACKGROUND_BODY_WIDTH) {
                rect(i, positionY, BACKGROUND_TEXTURE_X + BORDER_LEFT, BACKGROUND_TEXTURE_Y,
                        Math.min(BACKGROUND_BODY_WIDTH, bodyRight - i), BACKGROUND_HEIGHT);
            }
            // Right border
            rect(bodyRight, positionY, BACKGROUND_TEXTURE_X + BACKGROUND_WIDTH - BORDER_RIGHT,
                    BACKGROUND_TEXTURE_Y, BORDER_RIGHT, BACKGROUND_HEIGHT);
        }

        // The slider itself
        {
            int positionX = this.positionX + getSliderOffsetX();
            int positionY = this.positionY + BORDER_TOP;

            int backgroundBodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
            int bodyLeft = positionX + SLIDER_BORDER_LEFT;
            int bodyRight = positionX + (int) Math.round(backgroundBodyWidth * size) - SLIDER_BORDER_RIGHT;

            // Left border
            rect(positionX, positionY, SLIDER_TEXTURE_X, SLIDER_TEXTURE_Y, SLIDER_BORDER_LEFT, SLIDER_HEIGHT);
            // Body
            for (int i = bodyLeft; i < bodyRight; i += SLIDER_BODY_WIDTH) {
                rect(i, positionY, SLIDER_TEXTURE_X + SLIDER_BORDER_LEFT, SLIDER_TEXTURE_Y,
                        Math.min(SLIDER_BODY_WIDTH, bodyRight - i), SLIDER_HEIGHT);
            }
            // Right border
            rect(bodyRight, positionY, SLIDER_TEXTURE_X + SLIDER_WIDTH - SLIDER_BORDER_RIGHT,
                    SLIDER_TEXTURE_Y, SLIDER_BORDER_RIGHT, SLIDER_HEIGHT);
        }
    }

    protected void rect(int x, int y, int u, int v, int width, int height) {
        GlStateManager.resetColor();
        Minecraft.getMinecraft().renderEngine.bindTexture(replay_gui);
        glEnable(GL_BLEND);

        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
