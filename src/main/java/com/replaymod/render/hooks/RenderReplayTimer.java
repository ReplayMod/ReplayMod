package com.replaymod.render.hooks;

import com.replaymod.core.utils.WrappedTimer;
import net.minecraft.util.Timer;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class RenderReplayTimer extends WrappedTimer {
    private final Timer state = new Timer(0);

    public RenderReplayTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    public void updateTimer() {
        copy(this, state); // Save our current state
        super.updateTimer(); // Update current state
        copy(state, this); // Restore our old state
    }

    public Timer getWrapped() {
        return wrapped;
    }
}
