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

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void replayModRender_setupStereoscopicProjection(float partialTicks, long frameStartNano, MatrixStack matrixStack, CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                matrixStack.translate(0.1, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                matrixStack.translate(-0.1, 0, 0);
            }
        }
    }
}
