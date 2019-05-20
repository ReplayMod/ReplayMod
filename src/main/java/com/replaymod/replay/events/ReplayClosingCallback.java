package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import de.johni0702.minecraft.gui.utils.Event;

import java.io.IOException;

public interface ReplayClosingCallback {
    Event<ReplayClosingCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosingCallback listener : listeners) {
                    listener.replayClosing(replayHandler);
                }
            });

    void replayClosing(ReplayHandler replayHandler) throws IOException;
}
