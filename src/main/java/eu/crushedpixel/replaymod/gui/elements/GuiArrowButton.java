package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import java.awt.*;

public class GuiArrowButton extends GuiButton {

    private boolean upwards = false;

    public GuiArrowButton(int buttonId, int x, int y, String buttonText, boolean upwards) {
        super(buttonId, x, y, buttonText);

        this.upwards = upwards;
        width = 20;
        height = 20;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        try {
            super.drawButton(mc, mouseX, mouseY);
            if(upwards) {
                for(int i = 0; i <= Math.ceil(height / 2) - 5; i++) {
                    drawHorizontalLine(xPosition + width - height + i + 4, xPosition + width - i - 6, yPosition + height - ((height / 3) + i + 2), Color.BLACK.getRGB());
                }
            } else {
                for(int i = 0; i <= Math.ceil(height / 2) - 5; i++) {
                    drawHorizontalLine(xPosition + width - height + i + 4, xPosition + width - i - 6, yPosition + (height / 3) + i + 2, Color.BLACK.getRGB());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
