package com.replaymod.core;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> NOTIFICATIONS = make("notifications", "notifications", true);
    public static final Setting<String> RECORDING_PATH = advanced("recordingPath", null, "./replay_recordings/");

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>("core", key, displayName, defaultValue);
    }

    private static <T> Setting<T> advanced(String key, String displayName, T defaultValue) {
        return new Setting<>("advanced", key, displayName, defaultValue);
    }

    public Setting(String category, String key, String displayString, T defaultValue) {
        super(category, key, displayString == null ? null : "replaymod.gui.settings." + displayString, defaultValue);
    }
}
