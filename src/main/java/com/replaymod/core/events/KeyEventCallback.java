//#if MC>=11300
package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (key, scanCode, action, modifiers) -> {
                for (KeyEventCallback listener : listeners) {
                    listener.onKeyEvent(key, scanCode, action, modifiers);
                }
            }
    );

    void onKeyEvent(int key, int scanCode, int action, int modifiers);
}
//#endif
