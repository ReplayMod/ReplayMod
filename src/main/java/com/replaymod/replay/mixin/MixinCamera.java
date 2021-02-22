package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinCamera {
    @Shadow @Final private MinecraftClient client;
    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;getPitch()F"
            )
    )
    private void applyRoll(float float_1, long long_1, MatrixStack matrixStack, CallbackInfo ci) {
        Entity entity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
        if (entity instanceof CameraEntity) {
            matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(((CameraEntity) entity).roll));
        }
    }
}
