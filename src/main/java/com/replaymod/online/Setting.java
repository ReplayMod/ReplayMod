package com.replaymod.online;

import com.replaymod.core.SettingsRegistry;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> SKIP_LOGIN_PROMPT = make("skipLoginPrompt", null, false);

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("online", key, displayString == null ? null : "replaymod.gui.settings." + displayString, defaultValue);
    }
}
