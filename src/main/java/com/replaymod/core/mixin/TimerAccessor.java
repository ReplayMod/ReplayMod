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
    //#endif
}
