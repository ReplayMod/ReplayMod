//#if MC>=11300
package com.replaymod.core.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface PreRenderCallback {
    Event<PreRenderCallback> EVENT = EventFactory.createArrayBacked(
            PreRenderCallback.class,
            (listeners) -> () -> {
                for (PreRenderCallback listener : listeners) {
                    listener.preRender();
                }
            }
    );

    void preRender();
}
//#endif
