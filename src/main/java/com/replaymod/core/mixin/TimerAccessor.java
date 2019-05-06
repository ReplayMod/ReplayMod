package com.replaymod.core.mixin;

import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface TimerAccessor {
    @Accessor
    long getLastSyncSysClock();
    @Accessor
    void setLastSyncSysClock(long value);

    //#if MC>=11200
    @Accessor
    float getTickLength();
    @Accessor
    void setTickLength(float value);
    //#else
    //$$ @Accessor
    //$$ float getTimerSpeed();
    //$$ @Accessor
    //$$ void setTimerSpeed(float value);
    //$$ @Accessor
    //$$ float getTicksPerSecond();
    //$$ @Accessor
    //$$ void setTicksPerSecond(float value);
    //$$ @Accessor
    //$$ double getLastHRTime();
    //$$ @Accessor
    //$$ void setLastHRTime(double value);
    //$$ @Accessor
    //$$ long getLastSyncHRClock();
    //$$ @Accessor
    //$$ void setLastSyncHRClock(long value);
    //$$ @Accessor
    //$$ double getTimeSyncAdjustment();
    //$$ @Accessor
    //$$ void setTimeSyncAdjustment(double value);
    //$$ @Accessor
    //$$ long getCounter();
    //$$ @Accessor
    //$$ void setCounter(long value);
    //#endif
}
