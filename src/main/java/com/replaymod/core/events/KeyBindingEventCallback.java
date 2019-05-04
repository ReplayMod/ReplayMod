//#if MC>=11300
package com.replaymod.core.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface KeyBindingEventCallback {
    Event<KeyBindingEventCallback> EVENT = EventFactory.createArrayBacked(
            KeyBindingEventCallback.class,
            (listeners) -> () -> {
                for (KeyBindingEventCallback listener : listeners) {
                    listener.onKeybindingEvent();
                }
            }
    );

    void onKeybindingEvent();
}
//#endif
