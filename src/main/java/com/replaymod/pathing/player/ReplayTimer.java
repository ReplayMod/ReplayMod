package com.replaymod.pathing.player;

import de.johni0702.minecraft.gui.utils.Event;
import net.minecraft.client.render.RenderTickCounter;

/**
 * A timer that does not advance by itself.
 */
public class ReplayTimer extends RenderTickCounter {
    //#if MC>=11600
    public int ticksThisFrame;
    //#endif

    public ReplayTimer() {
        //#if MC>=12003
        //$$ super(0, 0, f -> f);
        //#elseif MC>=11400
        super(0, 0);
        //#else
        //$$ super(0);
        //#endif
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    //#if MC>=11400
    public
    //#if MC>=11600
    int
    //#else
    //$$ void
    //#endif
    beginRenderTick(
    //#else
    //$$ public void updateTimer(
    //#endif
            //#if MC>=11400
            long sysClock
            //#endif
    ) {
        UpdatedCallback.EVENT.invoker().onUpdate();
        //#if MC>=11600
        return ticksThisFrame;
        //#endif
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );
        void onUpdate();
    }
}
