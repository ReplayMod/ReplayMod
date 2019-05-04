package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ReplayClosedCallback {
    Event<ReplayClosedCallback> EVENT = EventFactory.createArrayBacked(ReplayClosedCallback.class,
            (listeners) -> (replayHandler) -> {
                for (ReplayClosedCallback listener : listeners) {
                    listener.replayClosed(replayHandler);
                }
            });

    void replayClosed(ReplayHandler replayHandler);
}
