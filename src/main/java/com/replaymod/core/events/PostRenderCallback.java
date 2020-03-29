//#if MC>=11400
package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;

public interface PostRenderCallback {
    Event<PostRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PostRenderCallback listener : listeners) {
                    listener.postRender();
                }
            }
    );

    void postRender();
}
//#endif
