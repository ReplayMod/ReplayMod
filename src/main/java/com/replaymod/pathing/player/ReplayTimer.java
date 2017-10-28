package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import net.minecraft.util.Timer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class ReplayTimer extends WrappedTimer {
    private final Timer state = new Timer(0);

    public ReplayTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    public void updateTimer() {
        copy(this, state); // Save our current state
        super.updateTimer(); // Update current state
        copy(state, this); // Restore our old state
        MinecraftForge.EVENT_BUS.post(new UpdatedEvent());
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public static class UpdatedEvent extends Event {
    }
}
