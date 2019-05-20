package com.replaymod.replay.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface EntityPlayerAccessor extends EntityLivingBaseAccessor {
    //#if MC>=10904
    @Accessor("selectedItem")
    ItemStack getItemStackMainHand();
    @Accessor("selectedItem")
    void setItemStackMainHand(ItemStack value);
    //#else
    //$$ @Accessor
    //$$ ItemStack getItemInUse();
    //$$ @Accessor
    //$$ void setItemInUse(ItemStack value);
    //$$ @Accessor
    //$$ int getItemInUseCount();
    //$$ @Accessor
    //$$ void setItemInUseCount(int value);
    //#endif
}
