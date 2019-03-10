package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11300
import net.minecraft.client.GameSettings;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.WorldRenderer;
//#else
//$$ import net.minecraft.client.renderer.EntityRenderer;
//$$ import net.minecraft.client.renderer.RenderGlobal;
//$$ import net.minecraft.client.settings.GameSettings;
//$$ import org.lwjgl.util.glu.Project;
//#endif

//#if MC>=10904
import net.minecraft.util.math.RayTraceResult;
//#else
//$$ import net.minecraft.util.MovingObjectPosition;
//#endif

//#if MC>=10800
import net.minecraft.client.renderer.GlStateManager;
//#else
//$$ import net.minecraft.entity.EntityLivingBase;
//$$ import com.replaymod.core.versions.MCVer.GlStateManager;
//#endif

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11300
@Mixin(value = GameRenderer.class)
//#else
//$$ @Mixin(value = EntityRenderer.class)
//#endif
public abstract class MixinEntityRenderer implements EntityRendererHandler.IEntityRenderer {
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
    //#if MC<11300
    //$$ @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModRender_onSetupFog(int fogDistanceFlag, float partialTicks, CallbackInfo ci) {
    //$$     if (replayModRender_handler == null) return;
    //$$     if (replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
        if (replayModRender_handler != null) {
            Entity entity = getRenderViewEntity(MCVer.getMinecraft());
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
    private void replayModRender_beforeSetupCameraTransform(
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_handler != null) {
            Entity entity = getRenderViewEntity(MCVer.getMinecraft());
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
        }
    }

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void replayModRender_afterSetupCameraTransform(
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_handler != null) {
            Entity entity = getRenderViewEntity(MCVer.getMinecraft());
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
    private void replayModRender_renderSpectatorHand(
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_handler != null) {
            if (replayModRender_handler.omnidirectional) {
                // No spectator hands during 360° view, we wouldn't even know where to put it
                ci.cancel();
            //#if MC>=11300
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

    //#if MC<11300
    //$$ @Shadow
    //$$ public abstract void renderHand(float partialTicks, int renderPass);
    //#endif

    //#if MC>=11300
    @Redirect(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/RayTraceResult;IF)V"))
    private void replayModRender_drawSelectionBox(WorldRenderer instance, EntityPlayer player, RayTraceResult rtr, int alwaysZero, float partialTicks) {
    //#else
    //#if MC>=10904
    //$$ @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/RayTraceResult;IF)V"))
    //$$ private void replayModRender_drawSelectionBox(RenderGlobal instance, EntityPlayer player, RayTraceResult rtr, int alwaysZero, float partialTicks) {
    //#else
    //#if MC>=10800
    //$$ @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V"))
    //#else
    //$$ @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V"))
    //#endif
    //$$ private void replayModRender_drawSelectionBox(RenderGlobal instance, EntityPlayer player, MovingObjectPosition rtr, int alwaysZero, float partialTicks) {
    //#endif
    //#endif
        if (replayModRender_handler == null) {
            instance.drawSelectionBox(player, rtr, alwaysZero, partialTicks);
        }
    }

    private int orgRenderDistanceChunks;

    //#if MC>=11300
    @Inject(method = "updateCameraAndRender(FJ)V", at = @At(value = "JUMP", ordinal = 0))
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "renderWorldPass", at = @At(value = "JUMP", ordinal = 0))
    //#else
    //$$ @Inject(method = "renderWorld", at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
    //$$         target = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;getInstance()Lnet/minecraft/client/renderer/culling/ClippingHelper;"))
    //#endif
    //#endif
    private void replayModRender_beforeRenderSky(CallbackInfo ci) {
        if (replayModRender_handler != null && replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
            GameSettings settings = MCVer.getMinecraft().gameSettings;
            orgRenderDistanceChunks = settings.renderDistanceChunks;
            settings.renderDistanceChunks = 5; // Set render distance to 5 so we're always rendering sky when chroma keying
        }
    }

    //#if MC>=11300
    @Redirect(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(F)V"))
    private void replayModRender_renderSky(WorldRenderer instance, float partialTicks) {
    //#else
    //#if MC>=10800
    //$$ @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V"))
    //$$ private void replayModRender_renderSky(RenderGlobal instance, float partialTicks, int renderPass) {
    //#else
    //$$ @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(F)V"))
    //$$ private void replayModRender_renderSky(RenderGlobal instance, float partialTicks) {
    //#endif
    //#endif
        if (replayModRender_handler != null && replayModRender_handler.getSettings().getChromaKeyingColor() != null) {
            MCVer.getMinecraft().gameSettings.renderDistanceChunks = orgRenderDistanceChunks;
            ReadableColor color = replayModRender_handler.getSettings().getChromaKeyingColor();
            GlStateManager.clearColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        } else {
            //#if MC>=11300
            instance.renderSky(partialTicks);
            //#else
            //#if MC>=10800
            //$$ instance.renderSky(partialTicks, renderPass);
            //#else
            //$$ instance.renderSky(partialTicks);
            //#endif
            //#endif
        }
    }

    /*
     *   Stereoscopic Renderer
     */

    //#if MC>=10800
    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 0))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    //#endif
    private void replayModRender_setupStereoscopicProjection(
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GL11.glTranslatef(0.07f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GL11.glTranslatef(-0.07f, 0, 0);
            }
        }
    }

    //#if MC>=10800
    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 1))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLoadIdentity()V", shift = At.Shift.AFTER, ordinal = 1, remap = false))
    //#endif
    private void replayModRender_setupStereoscopicModelView(
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GL11.glTranslatef(0.1f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GL11.glTranslatef(-0.1f, 0, 0);
            }
        }
    }

    /*
     *   Cubic Renderer
     */

    //#if MC>=11300
    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Matrix4f;perspective(DFFF)Lnet/minecraft/client/renderer/Matrix4f;"))
    private Matrix4f replayModRender_perspective$0(double fovY, float aspect, float zNear, float zFar) {
        return replayModRender_perspective((float) fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Matrix4f;perspective(DFFF)Lnet/minecraft/client/renderer/Matrix4f;"))
    private Matrix4f replayModRender_perspective$1(double fovY, float aspect, float zNear, float zFar) {
        return replayModRender_perspective((float) fovY, aspect, zNear, zFar);
    }

    private Matrix4f replayModRender_perspective(float fovY, float aspect, float zNear, float zFar) {
        if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
            fovY = 90;
            aspect = 1;
        }
        return Matrix4f.perspective(fovY, aspect, zNear, zFar);
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

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_setupCubicFrameRotation(float partialTicks, CallbackInfo ci) {
        if (replayModRender_getHandler() != null && replayModRender_getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            switch ((CubicOpenGlFrameCapturer.Data) replayModRender_getHandler().data) {
                case FRONT:
                    GL11.glRotatef(0, 0.0F, 1.0F, 0.0F);
                    break;
                case RIGHT:
                    GL11.glRotatef(90, 0.0F, 1.0F, 0.0F);
                    break;
                case BACK:
                    GL11.glRotatef(180, 0.0F, 1.0F, 0.0F);
                    break;
                case LEFT:
                    GL11.glRotatef(-90, 0.0F, 1.0F, 0.0F);
                    break;
                case BOTTOM:
                    GL11.glRotatef(90, 1.0F, 0.0F, 0.0F);
                    break;
                case TOP:
                    GL11.glRotatef(-90, 1.0F, 0.0F, 0.0F);
                    break;
            }
        }
        if (replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional) {
            // Minecraft goes back a little so we have to revert that
            GL11.glTranslatef(0.0F, 0.0F, 0.1F);
        }
    }
}
