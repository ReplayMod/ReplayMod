//#if MC>=11300
package com.replaymod.core.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface PreRenderHandCallback {
    Event<PreRenderHandCallback> EVENT = EventFactory.createArrayBacked(
            PreRenderHandCallback.class,
            (listeners) -> () -> {
                for (PreRenderHandCallback listener : listeners) {
                    if (listener.preRenderHand()) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean preRenderHand();
}
//#endif
