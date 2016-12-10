package com.replaymod.render.events;

import com.replaymod.render.rendering.VideoRenderer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.common.eventhandler.Event;

@RequiredArgsConstructor
public abstract class ReplayRenderEvent extends Event {
    @Getter
    private final VideoRenderer videoRenderer;

    public static class Pre extends ReplayRenderEvent {
        public Pre(VideoRenderer videoRenderer) {
            super(videoRenderer);
        }
    }

    public static class Post extends ReplayRenderEvent {
        public Post(VideoRenderer videoRenderer) {
            super(videoRenderer);
        }
    }
}
