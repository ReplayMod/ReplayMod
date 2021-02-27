package com.replaymod.render.mixin;

import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class Mixin_Stereoscopic_Camera implements EntityRendererHandler.IEntityRenderer {
    //#if MC>=10800
    @Inject(method = "applyCameraTransformations", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 0))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    //#endif
    private void replayModRender_setupStereoscopicProjection(CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GL11.glTranslatef(0.07f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GL11.glTranslatef(-0.07f, 0, 0);
            }
        }
    }

    //#if MC>=10800
    @Inject(method = "applyCameraTransformations", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 1))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 1, remap = false))
    //#endif
    private void replayModRender_setupStereoscopicModelView(CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GL11.glTranslatef(0.1f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GL11.glTranslatef(-0.1f, 0, 0);
            }
        }
    }
}
