package com.replaymod.core.events;

import com.replaymod.core.SettingsRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.Event;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.Event;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif

@RequiredArgsConstructor
public class SettingsChangedEvent extends Event {
    @Getter
    private final SettingsRegistry settingsRegistry;

    @Getter
    private final SettingsRegistry.SettingKey<?> key;
}
