package com.replaymod.replay.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface EntityLivingBaseAccessor {
    //#if MC>=11300
    @Accessor("field_6224")
    double getInterpTargetX();
    @Accessor("field_6245")
    double getInterpTargetY();
    @Accessor("field_6263")
    double getInterpTargetZ();
    @Accessor("field_6284")
    double getInterpTargetYaw();
    @Accessor("field_6221")
    double getInterpTargetPitch();
    //#endif

    //#if MC>=10904
    @Accessor("itemUseTimeLeft")
    int getActiveItemStackUseCount();
    @Accessor("itemUseTimeLeft")
    void setActiveItemStackUseCount(int value);
    //#endif
}
