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
        if(defaultValue != null) {
            if(acceptFloats) {
                setText("" + defaultValue);
            } else {
                setText("" + (int)Math.round(defaultValue));
            }
        }
    }

    public GuiNumberInput(int id, FontRenderer fontRenderer,
                          int xPos, int yPos, int width, int minimum, int maximum, int defaultValue, boolean acceptFloats) {
        this(id, fontRenderer, xPos, yPos, width, new Double(minimum), new Double(maximum), new Double(defaultValue), acceptFloats);
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
            if(minimum != null && val < minimum) {
                setText(acceptFloats ? minimum.toString() : new Integer((int)Math.round(minimum)).toString());
                return;
            }
            if(maximum != null && val > maximum) {
                setText(acceptFloats ? maximum.toString() : new Integer((int)Math.round(maximum)).toString());
                return;
            }
            super.writeText(text);
        } catch(NumberFormatException e) {
        }
    }

    public Double getValue(boolean nullable) {
        try {
            return Double.valueOf(getText());
        } catch(Exception e) {
            return nullable ? null : 0d;
        }
    }

}
