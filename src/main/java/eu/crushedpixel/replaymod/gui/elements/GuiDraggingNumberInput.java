package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class GuiDraggingNumberInput extends GuiNumberInput {

    public GuiDraggingNumberInput(FontRenderer fontRenderer,
                          int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue, boolean acceptFloats) {
        super(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats);
    }

    private int prevMouseX;
    private boolean dragging;

    @Override
    public void mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        dragging = false;
        prevMouseX = mouseX;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        if(mouseX != prevMouseX) {
            dragging = true;
            int diff = mouseX - prevMouseX;
            prevMouseX = mouseX;

            int bounds = 100;

            if(minimum != null && maximum != null) {
                bounds = (int)(maximum-minimum);
            }

            double value = getPreciseValue();
            double valueDiff = (diff/200) * bounds;

            this.setValue(value+valueDiff);
        }
        super.mouseDrag(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        if(!dragging) {
            super.mouseClick(mc, mouseX, mouseY, button);
        }
        super.mouseRelease(mc, mouseX, mouseY, button);
    }
}
