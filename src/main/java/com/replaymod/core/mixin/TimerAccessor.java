package com.replaymod.core.mixin;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTickCounter.class)
public interface TimerAccessor {
    @Accessor("prevTimeMillis")
    long getLastSyncSysClock();
    @Accessor("prevTimeMillis")
    void setLastSyncSysClock(long value);

    //#if MC>=11200
    @Accessor("tickTime")
    float getTickLength();
    @Accessor("tickTime")
    @Mutable
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
