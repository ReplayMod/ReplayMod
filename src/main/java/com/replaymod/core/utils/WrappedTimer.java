package com.replaymod.core.utils;

import com.replaymod.core.mixin.TimerAccessor;
import net.minecraft.client.render.RenderTickCounter;

public class WrappedTimer extends RenderTickCounter {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

    protected final RenderTickCounter wrapped;

    public WrappedTimer(RenderTickCounter wrapped) {
        //#if MC>=11400
        super(0, 0);
        //#else
        //$$ super(0);
        //#endif
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public void beginRenderTick(
            //#if MC>=11400
            long sysClock
            //#endif
    ) {
        copy(this, wrapped);
        wrapped.beginRenderTick(
                //#if MC>=11400
                sysClock
                //#endif
        );
        copy(wrapped, this);
    }

    protected void copy(RenderTickCounter from, RenderTickCounter to) {
        TimerAccessor fromA = (TimerAccessor) from;
        TimerAccessor toA = (TimerAccessor) to;

        to.ticksThisFrame = from.ticksThisFrame;
        to.tickDelta = from.tickDelta;
        toA.setLastSyncSysClock(fromA.getLastSyncSysClock());
        to.lastFrameDuration = from.lastFrameDuration;
        //#if MC>=11200
        toA.setTickLength(fromA.getTickLength());
        //#else
        //$$ toA.setTicksPerSecond(fromA.getTicksPerSecond());
        //$$ toA.setLastHRTime(fromA.getLastHRTime());
        //$$ toA.setTimerSpeed(fromA.getTimerSpeed());
        //$$ toA.setLastSyncHRClock(fromA.getLastSyncHRClock());
        //$$ toA.setCounter(fromA.getCounter());
        //$$ toA.setTimeSyncAdjustment(fromA.getTimeSyncAdjustment());
        //#endif
    }
}
