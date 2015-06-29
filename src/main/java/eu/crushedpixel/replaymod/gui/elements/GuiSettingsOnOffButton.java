package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.settings.ReplaySettings;

public class GuiSettingsOnOffButton extends GuiOnOffButton {

    private ReplaySettings.ValueEnum toChange;

    public GuiSettingsOnOffButton(int buttonId, int x, int y, int width, int height, ReplaySettings.ValueEnum toChange) {
        super(buttonId, x, y, width, height, toChange.getName()+": ");
        this.toChange = toChange;
        if(toChange.getValue() instanceof Boolean) {
            this.setValue((Boolean) toChange.getValue() ? 0 : 1);
        }
    }

    public GuiSettingsOnOffButton(int buttonId, int x, int y, int width, int height, ReplaySettings.ValueEnum toChange, String onValue, String offValue) {
        super(buttonId, x, y, width, height, toChange.getName()+": ", onValue, offValue);
        this.toChange = toChange;
        if(toChange.getValue() instanceof Boolean) {
            this.setValue((Boolean) toChange.getValue() ? 0 : 1);
        }
    }

    @Override
    public void toggle() {
        super.toggle();
        toChange.setValue(isOn());
        ReplayMod.replaySettings.rewriteSettings();
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        toChange.setValue(isOn());
        ReplayMod.replaySettings.rewriteSettings();
    }
}
