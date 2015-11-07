package com.replaymod.core.events;

import com.replaymod.core.SettingsRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.common.eventhandler.Event;

@RequiredArgsConstructor
public class SettingsChangedEvent extends Event {
    @Getter
    private final SettingsRegistry settingsRegistry;

    @Getter
    private final SettingsRegistry.SettingKey<?> key;
}
