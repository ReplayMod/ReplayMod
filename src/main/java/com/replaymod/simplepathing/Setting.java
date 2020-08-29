package com.replaymod.simplepathing;

import com.replaymod.core.SettingsRegistry;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> PATH_PREVIEW = make("pathpreview", "pathpreview", true);
    public static final Setting<Integer> TIMELINE_LENGTH = make("timelineLength", null, 30 * 60);
    public static final SettingsRegistry.MultipleChoiceSettingKeys<String> DEFAULT_INTERPOLATION;

    static {
        String format = "replaymod.gui.editkeyframe.interpolator.%s.name";
        DEFAULT_INTERPOLATION = new SettingsRegistry.MultipleChoiceSettingKeys<>(
                "simplepathing", "interpolator", "replaymod.gui.settings.interpolator",
                String.format(format, InterpolatorType.fromString("invalid returns default").getLocalizationKey())
        );
        DEFAULT_INTERPOLATION.setChoices(
                Arrays.stream(InterpolatorType.values()).filter(i -> i != InterpolatorType.DEFAULT)
                        .map(i -> String.format(format, i.getLocalizationKey()))
                        .collect(Collectors.toList())
        );
    }

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("simplepathing", key, displayString == null ? null : "replaymod.gui.settings." + displayString, defaultValue);
    }
}
