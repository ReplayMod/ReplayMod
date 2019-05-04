package com.replaymod.render.events;

import com.replaymod.render.rendering.VideoRenderer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ReplayRenderCallback {
    interface Pre {
        Event<Pre> EVENT = EventFactory.createArrayBacked(Pre.class,
                (listeners) -> (renderer) -> {
                    for (Pre listener : listeners) {
                        listener.beforeRendering(renderer);
                    }
                });

        void beforeRendering(VideoRenderer renderer);
    }

    interface Post {
        Event<Post> EVENT = EventFactory.createArrayBacked(Post.class,
                (listeners) -> (renderer) -> {
                    for (Post listener : listeners) {
                        listener.afterRendering(renderer);
                    }
                });

        void afterRendering(VideoRenderer renderer);
    }
}
