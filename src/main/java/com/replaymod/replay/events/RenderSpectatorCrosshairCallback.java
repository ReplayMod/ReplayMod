//#if MC>=11300
package com.replaymod.replay.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;

public interface RenderSpectatorCrosshairCallback {
    Event<RenderSpectatorCrosshairCallback> EVENT = EventFactory.createArrayBacked(
            RenderSpectatorCrosshairCallback.class,
            (listeners) -> () -> {
                for (RenderSpectatorCrosshairCallback listener : listeners) {
                    TriState state = listener.shouldRenderSpectatorCrosshair();
                    if (state != TriState.DEFAULT) {
                        return state;
                    }
                }
                return TriState.DEFAULT;
            }
    );

    TriState shouldRenderSpectatorCrosshair();
}
//#endif
