package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.resources.I18n;

public class GuiOnOffButton extends GuiToggleButton {

    private static final String[] values = new String[] {I18n.format("options.on"), I18n.format("options.off")};

    public GuiOnOffButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText, values);
    }

    public GuiOnOffButton(int buttonId, int x, int y, int width, int height, String buttonText) {
        super(buttonId, x, y, width, height, buttonText, values);
    }

    public GuiOnOffButton(int buttonId, int x, int y, int width, int height, String buttonText, String onValue, String offValue) {
        super(buttonId, x, y, width, height, buttonText, new String[] {onValue, offValue});
    }

    public boolean isOn() {
        return getValue() == 0;
    }
}
