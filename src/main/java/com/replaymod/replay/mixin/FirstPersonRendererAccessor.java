package com.replaymod.replay.mixin;

import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FirstPersonRenderer.class)
public interface FirstPersonRendererAccessor {
    //#if MC>=10904
    @Accessor
    void setItemStackMainHand(ItemStack value);
    @Accessor
    void setItemStackOffHand(ItemStack value);
    @Accessor
    void setEquippedProgressMainHand(float value);
    @Accessor
    void setPrevEquippedProgressMainHand(float value);
    @Accessor
    void setEquippedProgressOffHand(float value);
    @Accessor
    void setPrevEquippedProgressOffHand(float value);
    //#endif
}
