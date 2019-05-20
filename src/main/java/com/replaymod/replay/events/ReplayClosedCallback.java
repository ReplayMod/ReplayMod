package com.replaymod.replay.events;

import com.replaymod.replay.ReplayHandler;
import de.johni0702.minecraft.gui.utils.Event;

public interface ReplayClosedCallback {
    Event<ReplayClosedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosedCallback listener : listeners) {
                    listener.replayClosed(replayHandler);
                }
            });

    void replayClosed(ReplayHandler replayHandler);
}
