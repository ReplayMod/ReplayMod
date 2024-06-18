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

//#if MC>=12005
//$$ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//$$ import org.joml.Matrix4f;
//#endif

@Mixin(GameRenderer.class)
public class MixinCamera {
    @Shadow @Final private MinecraftClient client;
    //#if MC>=12005
    //$$ @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;"))
    //$$ private Matrix4f applyRoll(Matrix4f matrix) {
    //#else
    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;getPitch()F"
            )
    )
    private void applyRoll(float float_1, long long_1, MatrixStack matrixStack, CallbackInfo ci) {
    //#endif
        Entity entity = this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity();
        if (entity instanceof CameraEntity) {
            //#if MC>=12005
            //$$ matrix.rotateLocal(((CameraEntity) entity).roll * (float) Math.PI / 180f, 0f, 0f, 1f);
            //#else
            matrixStack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(((CameraEntity) entity).roll));
            //#endif
        }
        //#if MC>=12005
        //$$ return matrix;
        //#endif
    }
}
