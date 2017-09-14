package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ReplayCloseEvent extends Event {
    @Getter
    private final ReplayHandler replayHandler;

    public static class Pre extends ReplayCloseEvent {
        public Pre(ReplayHandler replayHandler) {
            super(replayHandler);
        }
    }

    public static class Post extends ReplayCloseEvent {
        public Post(ReplayHandler replayHandler) {
            super(replayHandler);
        }
    }
}
