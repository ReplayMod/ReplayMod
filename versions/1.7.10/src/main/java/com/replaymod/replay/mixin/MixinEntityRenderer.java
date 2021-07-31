package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Shadow private float prevCamRoll;
    @Shadow private float camRoll;
    @Shadow private Minecraft mc;

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void applyRoll(float partialTicks, CallbackInfo ci) {
        EntityLivingBase view = mc.renderViewEntity;
        if (view instanceof CameraEntity) {
            prevCamRoll = camRoll = ((CameraEntity) view).roll;
        }
    }
}
