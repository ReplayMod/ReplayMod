package com.replaymod.core.utils;

import net.minecraft.util.Timer;

public class WrappedTimer extends Timer {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

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
        to.elapsedTicks = from.elapsedTicks;
        to.field_194147_b = from.field_194147_b; // elapsedPartialTicks
        to.lastSyncSysClock = from.lastSyncSysClock;
        to.field_194148_c = from.field_194148_c; // lastTickDiff
        to.field_194149_e = from.field_194149_e; // msPerTick
    }
}
