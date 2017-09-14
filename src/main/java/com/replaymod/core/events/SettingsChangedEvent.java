package com.replaymod.core.events;

import com.replaymod.core.SettingsRegistry;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettingsChangedEvent extends Event {
    @Getter
    private final SettingsRegistry settingsRegistry;

    @Getter
    private final SettingsRegistry.SettingKey<?> key;
}
