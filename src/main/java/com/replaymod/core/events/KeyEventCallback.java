//#if MC>=11300
package com.replaymod.core.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = EventFactory.createArrayBacked(
            KeyEventCallback.class,
            (listeners) -> (key, scanCode, action, modifiers) -> {
                for (KeyEventCallback listener : listeners) {
                    listener.onKeyEvent(key, scanCode, action, modifiers);
                }
            }
    );

    void onKeyEvent(int key, int scanCode, int action, int modifiers);
}
//#endif
