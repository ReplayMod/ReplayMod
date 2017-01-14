package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandRenderer.class)
public abstract class MixinArmorStandRenderer {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_canRenderInvisibleName(EntityArmorStand entity, CallbackInfoReturnable<Boolean> ci) {
        EntityPlayer thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }
}
