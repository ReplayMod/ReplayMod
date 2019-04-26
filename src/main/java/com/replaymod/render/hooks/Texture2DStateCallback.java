package com.replaymod.render.hooks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface Texture2DStateCallback {
    Event<Texture2DStateCallback> EVENT = EventFactory.createArrayBacked(Texture2DStateCallback.class,
            (slot, enabled) -> {},
            (listeners) -> (slot, enabled) -> {
                for (Texture2DStateCallback listener : listeners) {
                    listener.texture2DStateChanged(slot, enabled);
                }
            });

    void texture2DStateChanged(int slot, boolean enabled);
}
