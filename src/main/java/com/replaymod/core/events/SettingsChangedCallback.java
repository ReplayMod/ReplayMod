package com.replaymod.core.events;

import com.replaymod.core.SettingsRegistry;
import de.johni0702.minecraft.gui.utils.Event;

public interface SettingsChangedCallback {
    Event<SettingsChangedCallback> EVENT = Event.create((listeners) ->
            (registry, key) -> {
                for (SettingsChangedCallback listener : listeners) {
                    listener.onSettingsChanged(registry, key);
                }
            }
    );

    void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key);
}
