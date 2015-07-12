package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class GuiDraggingNumberInput extends GuiNumberInputWithText {

    public GuiDraggingNumberInput(FontRenderer fontRenderer,
                          int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue, boolean acceptFloats) {
        this(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats, "", 0.5f);
    }

    public GuiDraggingNumberInput(FontRenderer fontRenderer,
                                  int xPos, int yPos, int width, Double minimum,
                                  Double maximum, Double defaultValue, boolean acceptFloats, String suffix, double stepSize) {
        super(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats, suffix);

        this.stepSize = stepSize;
    }

    private int prevMouseX;
    private boolean dragging;
    private boolean clicked;

    private double stepSize;

    @Override
    public boolean mouseClick(Minecraft mc, int mouseX, int mouseY, int button) {
        if(MouseUtils.isMouseWithinBounds(xPosition, yPosition, width, height) && isEnabled) {
            dragging = false;
            clicked = true;
            prevMouseX = mouseX;
            return true;
        }
        return false;
    }

    @Override
    public void mouseDrag(Minecraft mc, int mouseX, int mouseY, int button) {
        if(clicked && mouseX != prevMouseX) {
            dragging = true;
            int diff = mouseX - prevMouseX;
            prevMouseX = mouseX;

            double value = getPreciseValue();
            double valueDiff = diff * stepSize;

            this.setValue(RoundUtils.round2Decimals(value + valueDiff));
        }
        super.mouseDrag(mc, mouseX, mouseY, button);
    }

    @Override
    public void mouseRelease(Minecraft mc, int mouseX, int mouseY, int button) {
        clicked = false;
        if(!dragging) {
            super.mouseClick(mc, mouseX, mouseY, button);
        }
        super.mouseRelease(mc, mouseX, mouseY, button);
    }
}
