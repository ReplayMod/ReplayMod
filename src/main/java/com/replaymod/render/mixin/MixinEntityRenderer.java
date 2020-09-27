package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC>=11400
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.HitResult;
//#endif

//#if MC>=11400
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.client.render.WorldRenderer;
//#else
//$$ import com.replaymod.replay.camera.CameraEntity;
//$$ import net.minecraft.client.renderer.EntityRenderer;
//$$ import net.minecraft.client.renderer.RenderGlobal;
//$$ import net.minecraft.client.settings.GameSettings;
//$$ import net.minecraft.entity.Entity;
//$$ import org.lwjgl.util.glu.Project;
//#endif

//#if MC>=10904
import net.minecraft.world.RaycastContext;
//#else
//$$ import net.minecraft.util.MovingObjectPosition;
//#endif

//#if MC>=10800
import com.mojang.blaze3d.platform.GlStateManager;
//#else
//$$ import net.minecraft.entity.EntityLivingBase;
//$$ import com.replaymod.core.versions.MCVer.GlStateManager;
//#endif

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
@Mixin(value = GameRenderer.class)
//#else
//$$ @Mixin(value = EntityRenderer.class)
//#endif
public abstract class MixinEntityRenderer implements EntityRendererHandler.IEntityRenderer {
    @Shadow
    public MinecraftClient client;

    private EntityRendererHandler replayModRender_handler;

    @Override
    public void replayModRender_setHandler(EntityRendererHandler handler) {
        this.replayModRender_handler = handler;
    }

    @Override
    public EntityRendererHandler replayModRender_getHandler() {
        return replayModRender_handler;
    }

    //#if MC<=10710
    //$$ @Redirect(method = "renderWorld", at = @At(value = "INVOKE",target =
    //$$         "Lnet/minecraft/client/renderer/RenderGlobal;updateRenderers(Lnet/minecraft/entity/EntityLivingBase;Z)Z"))
    //$$ private boolean replayModRender_updateAllChunks(RenderGlobal self, EntityLivingBase view, boolean renderAllChunks) {
    //$$     if (replayModRender_handler != null) {
    //$$         renderAllChunks = true;
    //$$     }
    //$$     return self.updateRenderers(view, renderAllChunks);
    //$$ }
    //#endif

    // Moved to MixinFogRenderer in 1.13
    //#if MC<11400
    //$$ @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModRender_onSetupFog(int fogDistanceFlag, float partialTicks, CallbackInfo ci) {
    //$$     if (replayModRender_handler == null) return;
    //$$     if (replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void replayModRender_renderSpectatorHand(
            //#if MC>=11500
            MatrixStack matrixStack,
            //#endif
            //#if MC>=11400
            Camera camera,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_handler != null) {
            if (replayModRender_handler.omnidirectional) {
                // No spectator hands during 360° view, we wouldn't even know where to put it
                ci.cancel();
            //#if MC>=11400
            }
            //#else
            //$$ } else {
            //$$     Entity currentEntity = getRenderViewEntity(MCVer.getMinecraft());
            //$$     if (currentEntity instanceof EntityPlayer && !(currentEntity instanceof CameraEntity)) {
            //$$         if (renderPass == 2) { // Need to update render pass
            //$$             renderPass = replayModRender_handler.data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? 1 : 0;
            //$$             renderHand(partialTicks, renderPass);
            //$$             ci.cancel();
            //$$         } // else, render normally
            //$$     }
            //$$ }
            //#endif
        }
    }

    //#if MC<11400
    //$$ @Shadow
    //$$ public abstract void renderHand(float partialTicks, int renderPass);
    //#endif

    /*
     *   Stereoscopic Renderer
     */

    //#if MC>=11500
    @Inject(method = "method_22973", at = @At("RETURN"), cancellable = true)
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
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "applyCameraTransformations", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 0))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    //#endif
    //$$ private void replayModRender_setupStereoscopicProjection(
    //$$         float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
    //$$         CallbackInfo ci
    //$$ ) {
    //$$     if (replayModRender_getHandler() != null) {
    //$$         if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
    //$$             GL11.glTranslatef(0.07f, 0, 0);
    //$$         } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
    //$$             GL11.glTranslatef(-0.07f, 0, 0);
    //$$         }
    //$$     }
    //$$ }
    //#endif

    //#if MC>=11500
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
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "applyCameraTransformations", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 1))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 1, remap = false))
    //#endif
    //$$ private void replayModRender_setupStereoscopicModelView(
    //$$         float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
    //$$         CallbackInfo ci
    //$$ ) {
    //$$     if (replayModRender_getHandler() != null) {
    //$$         if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
    //$$             GL11.glTranslatef(0.1f, 0, 0);
    //$$         } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
    //$$             GL11.glTranslatef(-0.1f, 0, 0);
    //$$         }
    //$$     }
    //$$ }
    //#endif

    /*
     *   Cubic Renderer
     */

    //#if MC>=11400
    @Redirect(method = "method_22973", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Matrix4f;viewboxMatrix(DFFF)Lnet/minecraft/util/math/Matrix4f;"))
    private Matrix4f replayModRender_perspective$0(double fovY, float aspect, float zNear, float zFar) {
        return replayModRender_perspective((float) fovY, aspect, zNear, zFar);
    }

    //#if MC<11500
    //#if MC>=11400
    //$$ @Redirect(method = "renderCenter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Matrix4f;method_4929(DFFF)Lnet/minecraft/client/util/math/Matrix4f;"))
    //#else
    //$$ @Redirect(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Matrix4f;perspective(DFFF)Lnet/minecraft/client/renderer/Matrix4f;"))
    //#endif
    //$$ private Matrix4f replayModRender_perspective$1(double fovY, float aspect, float zNear, float zFar) {
    //$$     return replayModRender_perspective((float) fovY, aspect, zNear, zFar);
    //$$ }
    //$$
    //$$ @Redirect(method = "renderAboveClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Matrix4f;method_4929(DFFF)Lnet/minecraft/client/util/math/Matrix4f;"))
    //$$ private Matrix4f replayModRender_perspective$2(double fovY, float aspect, float zNear, float zFar) {
    //$$     return replayModRender_perspective((float) fovY, aspect, zNear, zFar);
    //$$ }
    //#endif

    private Matrix4f replayModRender_perspective(float fovY, float aspect, float zNear, float zFar) {
        if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
            fovY = 90;
            aspect = 1;
        }
        return Matrix4f.viewboxMatrix(fovY, aspect, zNear, zFar);
    }
    //#else
    //$$ @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    //$$ private void replayModRender_gluPerspective$0(float fovY, float aspect, float zNear, float zFar) {
    //$$     replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    //$$ }
    //$$
    //#if MC>=10800
    //$$ @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    //#else
    //$$ @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    //#endif
    //$$ private void replayModRender_gluPerspective$1(float fovY, float aspect, float zNear, float zFar) {
    //$$     replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    //$$ }
    //$$
    //#if MC>=10800
    //$$ @Redirect(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    //$$ private void replayModRender_gluPerspective$2(float fovY, float aspect, float zNear, float zFar) {
    //$$     replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    //$$ }
    //#endif
    //$$
    //$$ private void replayModRender_gluPerspective(float fovY, float aspect, float zNear, float zFar) {
    //$$     if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
    //$$         fovY = 90;
    //$$         aspect = 1;
    //$$     }
    //$$     Project.gluPerspective(fovY, aspect, zNear, zFar);
    //$$ }
    //#endif
}
