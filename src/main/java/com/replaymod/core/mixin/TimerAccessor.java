package com.replaymod.core.mixin;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=12100
//$$ @Mixin(RenderTickCounter.Dynamic.class)
//#else
@Mixin(RenderTickCounter.class)
//#endif
public interface TimerAccessor {
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
    //#endif
}
