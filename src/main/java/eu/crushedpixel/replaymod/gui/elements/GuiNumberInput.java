package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.gui.elements.listeners.NumberValueChangeListener;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class GuiNumberInput extends GuiAdvancedTextField {

    private List<NumberValueChangeListener> valueChangeListeners = new ArrayList<NumberValueChangeListener>();

    protected Double minimum, maximum;

    protected boolean acceptFloats = false;

    public GuiNumberInput(FontRenderer fontRenderer,
                          int xPos, int yPos, int width, Double minimum, Double maximum, Double defaultValue, boolean acceptFloats) {
        super(fontRenderer, xPos, yPos, width, 20);
        this.minimum = minimum;
        this.maximum = maximum;
        this.acceptFloats = acceptFloats;
        setCursorPositionZero();
        if(defaultValue != null) {
            setValue(defaultValue);
        }
    }

    public GuiNumberInput(FontRenderer fontRenderer,
                          int xPos, int yPos, int width, int minimum, int maximum, int defaultValue, boolean acceptFloats) {
        this(fontRenderer, xPos, yPos, width, (double) minimum, (double) maximum, (double) defaultValue, acceptFloats);
    }

    /**
     * Sets this GuiNumberInput's value without notifying the listeners.
     * @param value The value to set
     */
    public void setValueQuietly(double value) {
        if(minimum != null && value < minimum) {
            setText(acceptFloats ? minimum.toString() : Integer.toString((int) Math.round(minimum)));
        } else if(maximum != null && value > maximum) {
            setText(acceptFloats ? maximum.toString() : Integer.toString((int) Math.round(maximum)));
        } else {
            if(acceptFloats) {
                setText("" + value);
            } else {
                setText("" + (int) Math.round(value));
            }
        }
        setCursorPositionZero();
    }

    public void setValue(double value) {
        setValueQuietly(value);
        fireValueChangeEvent();
    }

    public void addValueChangeListener(NumberValueChangeListener listener) {
        this.valueChangeListeners.add(listener);
    }

    private void fireValueChangeEvent() {
        double val = acceptFloats ? getPreciseValue() : getIntValue();
        for(NumberValueChangeListener listener : valueChangeListeners) {
            listener.onValueChange(val);
        }
    }

    @Override
    public void writeText(String text) {
        String textBefore = getText();
        int cursorPositionBefore = getCursorPosition();
        try {
            super.writeText(text);

            double val;
            if(acceptFloats) {
                val = Double.valueOf(getText());
            } else {
                val = Integer.valueOf(getText());
            }

            if(minimum != null && val < minimum) {
                setText(acceptFloats ? minimum.toString() : Integer.toString((int) Math.round(minimum)));
            } else if(maximum != null && val > maximum) {
                setText(acceptFloats ? maximum.toString() : Integer.toString((int) Math.round(maximum)));
            }

            fireValueChangeEvent();
        } catch(NumberFormatException e) {
            setText(textBefore);
            setCursorPosition(cursorPositionBefore);
        }
    }

    @Override
    public void deleteFromCursor(int p_146175_1_) {
        super.deleteFromCursor(p_146175_1_);
        fireValueChangeEvent();
    }

    @Override
    public void deleteWords(int p_146177_1_) {
        super.deleteWords(p_146177_1_);
        fireValueChangeEvent();
    }

    public int getIntValue() {
        try {
            return Integer.valueOf(getText());
        } catch(Exception e) {
            return 0;
        }
    }

    public Integer getIntValueNullable() {
        try {
            return Integer.valueOf(getText());
        } catch(Exception e) {
            return null;
        }
    }

    public double getPreciseValue() {
        try {
            return Double.valueOf(getText());
        } catch(Exception e) {
            return 0d;
        }
    }

}
