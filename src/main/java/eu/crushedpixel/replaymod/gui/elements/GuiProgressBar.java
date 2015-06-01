package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;

public class GuiProgressBar extends Gui {

    private static final int BORDER_WIDTH = 2;

    private final Minecraft mc = Minecraft.getMinecraft();

    private int xPosition, yPosition, width, height;

    private float progress = 0;

    private String progressString = null;

    public GuiProgressBar(int xPosition, int yPosition, int width, int height) {
        this(xPosition, yPosition, width, height, 0);
    }

    public GuiProgressBar(int xPosition, int yPosition, int width, int height, float initialProgress) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.progress = initialProgress;
    }

    /**
     * Sets the progress amount of this GuiProgressBar.
     * @param progress A value between 0 and 1
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    public float getProgress() {
        return progress;
    }

    /**
     * Sets position and size of this GuiProgressBar.
     */
    public void setBounds(int xPosition, int yPosition, int width, int height) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
    }

    /**
     * Replaces the default progress String that is being displayed (x%) with a custom String.
     * This has to be called whenever the progress itself changes.
     * Setting <code>progressString</code> to <code>null</code> uses the default progress String.
     * @param progressString The String to display
     */
    public void setProgressString(String progressString) {
        this.progressString = progressString;
    }

    public void drawProgressBar() {
        int progressWidth = Math.round((width - (2*BORDER_WIDTH)) * progress);

        String progressString;
        if(this.progressString == null) {
            progressString = (int) Math.floor(progress * 100) + "%";
        } else {
            progressString = this.progressString;
        }

        // Draws black outline
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, Color.BLACK.getRGB());

        // Draws white background
        drawRect(xPosition + BORDER_WIDTH, yPosition + BORDER_WIDTH,
                xPosition + width - BORDER_WIDTH, yPosition + height - BORDER_WIDTH, Color.WHITE.getRGB());

        // Draws red progress
        drawRect(xPosition + BORDER_WIDTH, yPosition + BORDER_WIDTH,
                xPosition + BORDER_WIDTH + progressWidth, yPosition + height - BORDER_WIDTH, Color.GRAY.getRGB());


        int xMiddle = xPosition + (width/2);
        int yMiddle = yPosition + (height/2);

        int progressStringWidth = mc.fontRendererObj.getStringWidth(progressString);
        int progressStringHeight = mc.fontRendererObj.FONT_HEIGHT-3;

        int progressStringXPosition = xMiddle - (progressStringWidth/2);
        int progressStringYPosition = yMiddle - (progressStringHeight/2);

        mc.fontRendererObj.drawString(progressString, progressStringXPosition, progressStringYPosition, Color.BLACK.getRGB());
    }

}
