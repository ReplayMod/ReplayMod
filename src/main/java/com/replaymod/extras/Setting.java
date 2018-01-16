package com.replaymod.extras;

import com.replaymod.core.SettingsRegistry;

public final class Setting<T> {
    public static final SettingsRegistry.SettingKey<Boolean> SKIP_POST_SCREENSHOT_GUI =
            new SettingsRegistry.SettingKeys<>("advanced", "skipPostScreenshotGui", null, false);
}
