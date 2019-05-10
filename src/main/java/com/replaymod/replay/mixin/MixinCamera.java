//#if MC>=11400
package com.replaymod.replay.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class MixinCamera {
    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotatef(FFFF)V",
                    ordinal = 0
            )
    )
    private void applyRoll(BlockView blockView, Entity view, boolean boolean_1, boolean boolean_2, float float_1, CallbackInfo ci) {
        if (view instanceof CameraEntity) {
            GlStateManager.rotatef(((CameraEntity) view).roll, 0.0F, 0.0F, 1.0F);
        }
    }
}
//#endif
