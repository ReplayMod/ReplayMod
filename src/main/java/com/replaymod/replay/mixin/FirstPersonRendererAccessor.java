package com.replaymod.replay.mixin;

import net.minecraft.client.render.FirstPersonRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FirstPersonRenderer.class)
public interface FirstPersonRendererAccessor {
    //#if MC>=10904
    @Accessor("mainHand")
    void setItemStackMainHand(ItemStack value);
    @Accessor("offHand")
    void setItemStackOffHand(ItemStack value);
    @Accessor("equipProgressMainHand")
    void setEquippedProgressMainHand(float value);
    @Accessor("prevEquipProgressMainHand")
    void setPrevEquippedProgressMainHand(float value);
    @Accessor("equipProgressOffHand")
    void setEquippedProgressOffHand(float value);
    @Accessor("prevEquipProgressOffHand")
    void setPrevEquippedProgressOffHand(float value);
    //#else
    //$$ @Accessor
    //$$ void setItemToRender(ItemStack value);
    //$$ @Accessor
    //$$ void setEquippedItemSlot(int value);
    //$$ @Accessor
    //$$ void setEquippedProgress(float value);
    //$$ @Accessor
    //$$ void setPrevEquippedProgress(float value);
    //#endif
}
