package com.replaymod.simplepathing;

import com.replaymod.core.SettingsRegistry;

import java.util.ArrayList;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> PATH_PREVIEW = make("pathpreview", "pathpreview", true);
    public static final SettingsRegistry.MultipleChoiceSettingKeys<String> DEFAULT_INTERPOLATION =
            new SettingsRegistry.MultipleChoiceSettingKeys<>(
                    "simplepathing", "interpolator", "replaymod.gui.settings.interpolator",
                    "replaymod.gui.editkeyframe.interpolator.cubic.name");

    static {
        DEFAULT_INTERPOLATION.setChoices(new ArrayList<String>() {
            {
                add("replaymod.gui.editkeyframe.interpolator.cubic.name");
                add("replaymod.gui.editkeyframe.interpolator.linear.name");
            }
        });
    }

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("simplepathing", key, "replaymod.gui.settings." + displayString, defaultValue);
    }
}
