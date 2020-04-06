//#if MC>=11400
package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//#else
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//$$ import net.minecraft.world.BlockView;
//#endif

//#if MC>=11500
@Mixin(net.minecraft.client.render.GameRenderer.class)
//#else
//$$ @Mixin(net.minecraft.client.render.Camera.class)
//#endif
public class MixinCamera {
    //#if MC>=11500
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
    //#else
    //$$ @Inject(
    //$$         method = "update",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotatef(FFFF)V",
    //$$                 ordinal = 0
    //$$         )
    //$$ )
    //$$ private void applyRoll(BlockView blockView, Entity view, boolean boolean_1, boolean boolean_2, float float_1, CallbackInfo ci) {
    //$$     if (view instanceof CameraEntity) {
    //$$         GlStateManager.rotatef(((CameraEntity) view).roll, 0.0F, 0.0F, 1.0F);
    //$$     }
    //$$ }
    //#endif
}
//#endif
