package com.replaymod.simplepathing;

import com.replaymod.core.SettingsRegistry;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> PATH_PREVIEW = make("pathpreview", "pathpreview", true);

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("simplepathing", key, "replaymod.gui.settings." + displayString, defaultValue);
    }
}
