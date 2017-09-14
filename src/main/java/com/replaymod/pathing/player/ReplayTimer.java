package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.util.Timer;

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
        FMLCommonHandler.instance().bus().post(new UpdatedEvent());
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public static class UpdatedEvent extends Event {
    }
}
