package com.replaymod.core.utils;

import net.minecraft.util.Timer;

public class WrappedTimer extends Timer {
    protected final Timer wrapped;

    public WrappedTimer(Timer wrapped) {
        super(0);
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public void updateTimer() {
        copy(this, wrapped);
        wrapped.updateTimer();
        copy(wrapped, this);
    }

    protected void copy(Timer from, Timer to) {
        to.ticksPerSecond = from.ticksPerSecond;
        to.lastHRTime = from.lastHRTime;
        to.elapsedTicks = from.elapsedTicks;
        to.renderPartialTicks = from.renderPartialTicks;
        to.timerSpeed = from.timerSpeed;
        to.elapsedPartialTicks = from.elapsedPartialTicks;
        to.lastSyncSysClock = from.lastSyncSysClock;
        to.lastSyncHRClock = from.lastSyncHRClock;
        to.counter = from.counter;
        to.timeSyncAdjustment = from.timeSyncAdjustment;
    }
}
