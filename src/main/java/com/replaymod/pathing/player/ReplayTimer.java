package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import net.minecraft.util.Timer;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.eventbus.api.Event;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.Event;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.Event;
//#endif

import static com.replaymod.core.versions.MCVer.*;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class ReplayTimer extends WrappedTimer {
    //#if MC>=11300
    private final Timer state = new Timer(0, 0);
    //#else
    //$$ private final Timer state = new Timer(0);
    //#endif

    public ReplayTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    public void updateTimer(
            //#if MC>=11300
            long sysClock
            //#endif
    ) {
        copy(this, state); // Save our current state
        wrapped.updateTimer(
                //#if MC>=11300
                sysClock
                //#endif
        ); // Update current state
        copy(state, this); // Restore our old state
        FML_BUS.post(new UpdatedEvent());
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public static class UpdatedEvent extends Event {
    }
}
