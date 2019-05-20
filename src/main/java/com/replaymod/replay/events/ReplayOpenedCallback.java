package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import de.johni0702.minecraft.gui.utils.Event;

import java.io.IOException;

public interface ReplayOpenedCallback {
    Event<ReplayOpenedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayOpenedCallback listener : listeners) {
                    listener.replayOpened(replayHandler);
                }
            });

    void replayOpened(ReplayHandler replayHandler) throws IOException;
}
