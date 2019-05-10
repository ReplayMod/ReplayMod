package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class ReplayTimer extends WrappedTimer {
    //#if MC>=11300
    private final RenderTickCounter state = new RenderTickCounter(0, 0);
    //#else
    //$$ private final Timer state = new Timer(0);
    //#endif

    public ReplayTimer(RenderTickCounter wrapped) {
        super(wrapped);
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    //#if MC>=11400
    public void beginRenderTick(
    //#else
    //$$ public void updateTimer(
    //#endif
            //#if MC>=11300
            long sysClock
            //#endif
    ) {
        copy(this, state); // Save our current state
        wrapped.beginRenderTick(
                //#if MC>=11300
                sysClock
                //#endif
        ); // Update current state
        copy(state, this); // Restore our old state
        UpdatedCallback.EVENT.invoker().onUpdate();
    }

    public RenderTickCounter getWrapped() {
        return wrapped;
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = EventFactory.createArrayBacked(UpdatedCallback.class,
                (listeners) -> () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                });
        void onUpdate();
    }
}
