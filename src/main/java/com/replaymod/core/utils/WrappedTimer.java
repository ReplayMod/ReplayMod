package com.replaymod.core.utils;

import net.minecraft.util.Timer;

public class WrappedTimer extends Timer {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

    protected final Timer wrapped;

    public WrappedTimer(Timer wrapped) {
        //#if MC>=11300
        super(0, 0);
        //#else
        //$$ super(0);
        //#endif
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public void updateTimer(
            //#if MC>=11300
            long sysClock
            //#endif
    ) {
        copy(this, wrapped);
        wrapped.updateTimer(
                //#if MC>=11300
                sysClock
                //#endif
        );
        copy(wrapped, this);
    }

    protected void copy(Timer from, Timer to) {
        to.elapsedTicks = from.elapsedTicks;
        to.renderPartialTicks = from.renderPartialTicks;
        to.lastSyncSysClock = from.lastSyncSysClock;
        to.elapsedPartialTicks = from.elapsedPartialTicks;
        //#if MC>=11200
        to.tickLength = from.tickLength;
        //#else
        //$$ to.ticksPerSecond = from.ticksPerSecond;
        //$$ to.lastHRTime = from.lastHRTime;
        //$$ to.timerSpeed = from.timerSpeed;
        //$$ to.lastSyncHRClock = from.lastSyncHRClock;
        //#if MC>=10809
        //$$ to.counter = from.counter;
        //#else
        //$$ to.field_74285_i = from.field_74285_i;
        //#endif
        //$$ to.timeSyncAdjustment = from.timeSyncAdjustment;
        //#endif
    }
}
