package com.replaymod.render.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.ReadableColor;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10904
import net.minecraft.util.math.RayTraceResult;
//#else
//$$ import net.minecraft.util.MovingObjectPosition;
//#endif

@Mixin(value = EntityRenderer.class)
public abstract class MixinEntityRenderer implements EntityRendererHandler.IEntityRenderer, EntityRendererHandler.GluPerspective {
    @Shadow
    public Minecraft mc;

    private EntityRendererHandler replayModRender_handler;

    @Override
    public void replayModRender_setHandler(EntityRendererHandler handler) {
        this.replayModRender_handler = handler;
    }

    @Override
    public EntityRendererHandler replayModRender_getHandler() {
        return replayModRender_handler;
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void replayModRender_onSetupFog(int fogDistanceFlag, float partialTicks, CallbackInfo ci) {
        if (replayModRender_handler == null) return;
        if (replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
        if (replayModRender_handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            RenderSettings settings = replayModRender_handler.getSettings();
            if (settings.isStabilizeYaw()) {
                entity.prevRotationYaw = entity.rotationYaw = 0;
            }
            if (settings.isStabilizePitch()) {
                entity.prevRotationPitch = entity.rotationPitch = 0;
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    private void replayModRender_beforeSetupCameraTransform(float partialTicks, int renderPass, CallbackInfo ci) {
        if (replayModRender_handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
        }
    }

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void replayModRender_afterSetupCameraTransform(float partialTicks, int renderPass, CallbackInfo ci) {
        if (replayModRender_handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            entity.rotationYaw = orgYaw;
            entity.rotationPitch = orgPitch;
            entity.prevRotationYaw = orgPrevYaw;
            entity.prevRotationPitch = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void replayModRender_renderSpectatorHand(float partialTicks, int renderPass, CallbackInfo ci) {
        if (replayModRender_handler != null) {
            if (replayModRender_handler.omnidirectional) {
                ci.cancel();
                return; // No spectator hands during 360° view, we wouldn't even know where to put it
            }
            Entity currentEntity = Minecraft.getMinecraft().getRenderViewEntity();
            if (currentEntity instanceof EntityPlayer && !(currentEntity instanceof CameraEntity)) {
                if (renderPass == 2) { // Need to update render pass
                    renderPass = replayModRender_handler.data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? 1 : 0;
                    renderHand(partialTicks, renderPass);
                    ci.cancel();
                } // else, render normally
            }
        }
    }

    @Shadow
    public abstract void renderHand(float partialTicks, int renderPass);

    //#if MC>=10904
    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/RayTraceResult;IF)V"))
    private void replayModRender_drawSelectionBox(RenderGlobal instance, EntityPlayer player, RayTraceResult rtr, int alwaysZero, float partialTicks) {
    //#else
    //$$ @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V"))
    //$$ private void replayModRender_drawSelectionBox(RenderGlobal instance, EntityPlayer player, MovingObjectPosition rtr, int alwaysZero, float partialTicks) {
    //#endif
        if (replayModRender_handler == null) {
            instance.drawSelectionBox(player, rtr, alwaysZero, partialTicks);
        }
    }

    private int orgRenderDistanceChunks;

    @Inject(method = "renderWorldPass", at = @At(value = "JUMP", ordinal = 0))
    private void replayModRender_beforeRenderSky(CallbackInfo ci) {
        if (replayModRender_handler != null && replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            orgRenderDistanceChunks = settings.renderDistanceChunks;
            settings.renderDistanceChunks = 5; // Set render distance to 5 so we're always rendering sky when chroma keying
        }
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V"))
    private void replayModRender_renderSky(RenderGlobal instance, float partialTicks, int renderPass) {
        if (replayModRender_handler != null && replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
            Minecraft.getMinecraft().gameSettings.renderDistanceChunks = orgRenderDistanceChunks;
            ReadableColor color = replayModRender_handler.getSettings().getChromaKeyingColor();
            GlStateManager.clearColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        } else {
            instance.renderSky(partialTicks, renderPass);
        }
    }

    /*
     *   Stereoscopic Renderer
     */

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 0))
    private void replayModRender_setupStereoscopicProjection(float partialTicks, int renderPass, CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GlStateManager.translate(0.07, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GlStateManager.translate(-0.07, 0, 0);
            }
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 1))
    private void replayModRender_setupStereoscopicModelView(float partialTicks, int renderPass, CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GlStateManager.translate(0.1, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GlStateManager.translate(-0.1, 0, 0);
            }
        }
    }

    /*
     *   Cubic Renderer
     */

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void replayModRender_gluPerspective$0(float fovY, float aspect, float zNear, float zFar) {
        replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void replayModRender_gluPerspective$1(float fovY, float aspect, float zNear, float zFar) {
        replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void replayModRender_gluPerspective$2(float fovY, float aspect, float zNear, float zFar) {
        replayModRender_gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Override
    public void replayModRender_gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
            fovY = 90;
            aspect = 1;
        }
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_setupCubicFrameRotation(float partialTicks, CallbackInfo ci) {
        if (replayModRender_getHandler() != null && replayModRender_getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            switch ((CubicOpenGlFrameCapturer.Data) replayModRender_getHandler().data) {
                case FRONT:
                    GlStateManager.rotate(0, 0.0F, 1.0F, 0.0F);
                    break;
                case RIGHT:
                    GlStateManager.rotate(90, 0.0F, 1.0F, 0.0F);
                    break;
                case BACK:
                    GlStateManager.rotate(180, 0.0F, 1.0F, 0.0F);
                    break;
                case LEFT:
                    GlStateManager.rotate(-90, 0.0F, 1.0F, 0.0F);
                    break;
                case BOTTOM:
                    GlStateManager.rotate(90, 1.0F, 0.0F, 0.0F);
                    break;
                case TOP:
                    GlStateManager.rotate(-90, 1.0F, 0.0F, 0.0F);
                    break;
            }
        }
        if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
            // Minecraft goes back a little so we have to revert that
            GlStateManager.translate(0.0F, 0.0F, 0.1F);
        }
    }
}
