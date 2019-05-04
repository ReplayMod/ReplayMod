//#if MC>=11300
package com.replaymod.core.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface PostRenderCallback {
    Event<PostRenderCallback> EVENT = EventFactory.createArrayBacked(
            PostRenderCallback.class,
            (listeners) -> () -> {
                for (PostRenderCallback listener : listeners) {
                    listener.postRender();
                }
            }
    );

    void postRender();
}
//#endif
