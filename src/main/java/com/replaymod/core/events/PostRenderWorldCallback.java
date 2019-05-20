//#if MC>=11400
package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld();
                }
            }
    );

    void postRenderWorld();
}
//#endif
