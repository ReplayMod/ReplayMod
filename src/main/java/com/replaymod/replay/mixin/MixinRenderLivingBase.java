package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBase {
    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_canRenderInvisibleName(EntityLivingBase entity, CallbackInfoReturnable<Boolean> ci) {
        EntityPlayer thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean replayModReplay_shouldInvisibleNotBeRendered(EntityLivingBase entity, EntityPlayer thePlayer) {
        return thePlayer instanceof CameraEntity || entity.isInvisibleToPlayer(thePlayer);
    }
}
