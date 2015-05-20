package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class GuiNumberInput extends GuiTextField {

    private Double minimum, maximum;

    private boolean acceptFloats = false;

    public GuiNumberInput(int id, FontRenderer fontRenderer,
                          int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue, boolean acceptFloats) {
        super(id, fontRenderer, xPos, yPos, width, 20);
        this.minimum = minimum;
        this.maximum = maximum;
        this.acceptFloats = acceptFloats;
        if(defaultValue != null) setText(""+defaultValue);
    }

    @Override
    public void writeText(String text) {
        try {
            double val;
            if(acceptFloats) {
                val = Double.valueOf(getText() + text);
            } else {
                val = Integer.valueOf(getText() + text);
            }
            if(minimum != null && val < minimum) return;
            if(maximum != null && val > maximum) return;
            super.writeText(text);
        } catch(NumberFormatException e) {
        }
    }

}
