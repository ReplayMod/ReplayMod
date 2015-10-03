package eu.crushedpixel.replaymod.gui.elements;

import com.replaymod.core.SettingsRegistry;

public class GuiSettingsOnOffButton extends GuiOnOffButton {

    private SettingsRegistry settingsRegistry;
    private SettingsRegistry.SettingKey<Boolean> toChange;

    public GuiSettingsOnOffButton(int buttonId, int x, int y, int width, int height, SettingsRegistry settingsRegistry,
                                  SettingsRegistry.SettingKey<Boolean> toChange) {
        super(buttonId, x, y, width, height, toChange.getDisplayString()+": ");
        this.settingsRegistry = settingsRegistry;
        this.toChange = toChange;
        this.setValue(settingsRegistry.get(toChange) ? 0 : 1);
    }

    public GuiSettingsOnOffButton(int buttonId, int x, int y, int width, int height, SettingsRegistry settingsRegistry,
                                  SettingsRegistry.SettingKey<Boolean> toChange, String onValue, String offValue) {
        super(buttonId, x, y, width, height, toChange.getDisplayString()+": ", onValue, offValue);
        this.settingsRegistry = settingsRegistry;
        this.toChange = toChange;
        this.setValue(settingsRegistry.get(toChange) ? 0 : 1);
    }

    @Override
    public void toggle() {
        super.toggle();
        settingsRegistry.set(toChange, isOn());
        settingsRegistry.save();
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        settingsRegistry.set(toChange, isOn());
        settingsRegistry.save();
    }
}
