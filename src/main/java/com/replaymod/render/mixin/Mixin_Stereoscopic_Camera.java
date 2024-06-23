package com.replaymod.render.mixin;

import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=12005
//$$ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//$$ import org.joml.Matrix4f;
//#endif

@Mixin(GameRenderer.class)
public abstract class Mixin_Stereoscopic_Camera implements EntityRendererHandler.IEntityRenderer {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void replayModRender_setupStereoscopicProjection(CallbackInfoReturnable<Matrix4f> ci) {
        if (replayModRender_getHandler() != null) {
            Matrix4f offset;
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                offset = Matrix4f.translate(0.07f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                offset = Matrix4f.translate(-0.07f, 0, 0);
            } else {
                return;
            }
            offset.multiply(ci.getReturnValue());
            ci.setReturnValue(offset);
        }
    }

    //#if MC>=12005
    //#if MC>=12100
    //$$ @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;"))
    //#else
    //$$ @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;"))
    //#endif
    //$$ private Matrix4f replayModRender_setupStereoscopicProjection(Matrix4f matrix) {
    //#else
    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void replayModRender_setupStereoscopicProjection(float partialTicks, long frameStartNano, MatrixStack matrixStack, CallbackInfo ci) {
    //#endif
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                //#if MC>=12005
                //$$ matrix.translateLocal(0.1f, 0, 0);
                //#else
                matrixStack.translate(0.1f, 0, 0);
                //#endif
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                //#if MC>=12005
                //$$ matrix.translateLocal(-0.1f, 0, 0);
                //#else
                matrixStack.translate(-0.1f, 0, 0);
                //#endif
            }
        }
        //#if MC>=12005
        //$$ return matrix;
        //#endif
    }
}
