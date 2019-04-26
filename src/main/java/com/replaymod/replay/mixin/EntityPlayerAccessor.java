package com.replaymod.replay.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayer.class)
public interface EntityPlayerAccessor extends EntityLivingBaseAccessor {
    //#if MC>=10904
    @Accessor
    ItemStack getItemStackMainHand();
    @Accessor
    void setItemStackMainHand(ItemStack value);
    //#endif
}
