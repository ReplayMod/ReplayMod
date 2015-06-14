package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;

public class GuiToggleButton extends GuiAdvancedButton {

    private String baseText;
    private String[] values;
    private int value;

    public GuiToggleButton(int buttonId, int x, int y, String buttonText, String[] values) {
        super(buttonId, x, y, buttonText);
        this.values = values;
        this.baseText = buttonText;
        this.value = 0;
        if(values.length <= 1) {
            throw new RuntimeException("At least two elements need to be added to a GuiToggleButton");
        }

        this.displayString = baseText+values[value];
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean success = super.mousePressed(mc, mouseX, mouseY);
        if (success) {
            toggle();
        }
        return success;
    }

    public void toggle() {
        value++;
        if(value >= values.length) value = 0;
        this.displayString = baseText+values[value];
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


}
