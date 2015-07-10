package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class GuiDraggingNumberInput extends GuiNumberInputWithText {

    public GuiDraggingNumberInput(FontRenderer fontRenderer,
                          int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue, boolean acceptFloats) {
        this(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats, "");
    }

    public GuiDraggingNumberInput(FontRenderer fontRenderer,
                                  int xPos, int yPos, int width, Double minimum,
                                  Double maximum, Double defaultValue, boolean acceptFloats, String suffix) {
        super(fontRenderer, xPos, yPos, width, minimum, maximum, defaultValue, acceptFloats, suffix);
    }

    private int prevMouseX;
    private boolean dragging;
    private boolean clicked;

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

            int bounds = 100;

            if(minimum != null && maximum != null) {
                bounds = (int)(maximum-minimum);
            }

            double value = getPreciseValue();
            double valueDiff = RoundUtils.round2Decimals((diff / 200f) * bounds);

            this.setValue(value+valueDiff);
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
