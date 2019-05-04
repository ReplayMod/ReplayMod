package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.io.IOException;

public interface ReplayClosingCallback {
    Event<ReplayClosingCallback> EVENT = EventFactory.createArrayBacked(ReplayClosingCallback.class,
            (listeners) -> (replayHandler) -> {
                for (ReplayClosingCallback listener : listeners) {
                    listener.replayClosing(replayHandler);
                }
            });

    void replayClosing(ReplayHandler replayHandler) throws IOException;
}
