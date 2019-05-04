package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.io.IOException;

public interface ReplayOpenedCallback {
    Event<ReplayOpenedCallback> EVENT = EventFactory.createArrayBacked(ReplayOpenedCallback.class,
            (listeners) -> (replayHandler) -> {
                for (ReplayOpenedCallback listener : listeners) {
                    listener.replayOpened(replayHandler);
                }
            });

    void replayOpened(ReplayHandler replayHandler) throws IOException;
}
