package com.replaymod.render.hooks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface FogStateCallback {
    Event<FogStateCallback> EVENT = EventFactory.createArrayBacked(FogStateCallback.class,
            (enabled) -> {},
            (listeners) -> (enabled) -> {
                for (FogStateCallback listener : listeners) {
                    listener.fogStateChanged(enabled);
                }
            });

    void fogStateChanged(boolean enabled);
}
