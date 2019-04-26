package com.replaymod.replay.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {
    //#if MC>=11300
    @Accessor
    double getInterpTargetX();
    @Accessor
    double getInterpTargetY();
    @Accessor
    double getInterpTargetZ();
    @Accessor
    double getInterpTargetYaw();
    @Accessor
    double getInterpTargetPitch();
    //#endif

    //#if MC>=10904
    @Accessor
    int getActiveItemStackUseCount();
    @Accessor
    void setActiveItemStackUseCount(int value);
    //#endif
}
