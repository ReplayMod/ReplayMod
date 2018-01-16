package com.replaymod.restrictions;

import com.replaymod.core.SettingsRegistry;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> NO_XRAY = make("no_xray", false);
    public static final Setting<Boolean> NO_NOCLIP = make("no_noclip", false);
    public static final Setting<Boolean> ONLY_FIRST_PERSON = make("only_first_person", false);
    public static final Setting<Boolean> ONLY_RECORDING_PLAYER = make("only_recording_player", false);
    public static final Setting<Boolean> HIDE_COORDINATES = make("hide_coordinates", false);

    private static <T> Setting<T> make(String key, T defaultValue) {
        return new Setting<>("restrictions", key, defaultValue);
    }

    public Setting(String category, String key, T defaultValue) {
        super(category, key, null, defaultValue);
    }
}
