package com.replaymod.core.events;

import com.replaymod.core.SettingsRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SettingsChangedCallback {
    Event<SettingsChangedCallback> EVENT = EventFactory.createArrayBacked(
            SettingsChangedCallback.class,
            (listeners) -> (registry, key) -> {
                for (SettingsChangedCallback listener : listeners) {
                    listener.onSettingsChanged(registry, key);
                }
            }
    );

    void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key);
}
