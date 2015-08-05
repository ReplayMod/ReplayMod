package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.renderer.SpectatorRenderer;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.EntityRendererHandler;
import eu.crushedpixel.replaymod.video.capturer.CubicOpenGlFrameCapturer;
import eu.crushedpixel.replaymod.video.capturer.StereoscopicOpenGlFrameCapturer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements EntityRendererHandler.IEntityRenderer, EntityRendererHandler.GluPerspective {
    private EntityRendererHandler handler;
    private SpectatorRenderer spectatorRenderer;

    @Override
    public void setHandler(EntityRendererHandler handler) {
        this.handler = handler;
        if (spectatorRenderer != null) {
            spectatorRenderer.cleanup();
            spectatorRenderer = null;
        }
        if (handler != null) {
            spectatorRenderer = new SpectatorRenderer();
        }
    }

    @Override
    public EntityRendererHandler getHandler() {
        return handler;
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void onSetupFog(int fogDistanceFlag, float partialTicks, CallbackInfo ci) {
        if (handler == null) return;
        if (!handler.getOptions().isDefaultSky()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;setPosition(DDD)V"))
    public void createNoCullingFrustum(Frustum frustum, double x, double y, double z) {
        if (handler != null) {
            frustum.clippingHelper = new EntityRendererHandler.NoCullingClippingHelper();
        }
        frustum.setPosition(x, y, z);
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
        if (handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            RenderOptions options = handler.getOptions();
            if (options.getIgnoreCameraRotation()[0]) {
                entity.prevRotationYaw = entity.rotationYaw = 0;
            }
            if (options.getIgnoreCameraRotation()[1]) {
                entity.prevRotationPitch = entity.rotationPitch = 0;
            }
            if (options.getIgnoreCameraRotation()[2]) {
                ReplayHandler.setCameraTilt(0);
            }
        }
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    private void beforeSetupCameraTransform(float partialTicks, int renderPass, CallbackInfo ci) {
        if (handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = ReplayHandler.getCameraTilt();
        }
    }

    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    private void afterSetupCameraTransform(float partialTicks, int renderPass, CallbackInfo ci) {
        if (handler != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            entity.rotationYaw = orgYaw;
            entity.rotationPitch = orgPitch;
            entity.prevRotationYaw = orgPrevYaw;
            entity.prevRotationPitch = orgPrevPitch;
            ReplayHandler.setCameraTilt(orgRoll);
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderSpectatorHand(float partialTicks, int renderPass, CallbackInfo ci) {
        if (handler != null) {
            if (handler.data instanceof CubicOpenGlFrameCapturer.Data) {
                return; // No spectator hands during 360° view, we wouldn't even know where to put it
            }
            Entity currentEntity = ReplayHandler.getCurrentEntity();
            if (!ReplayHandler.isCamera() && currentEntity instanceof EntityPlayer) {
                renderPass = handler.data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? 1 : 0;
                spectatorRenderer.renderSpectatorHand((EntityPlayer) currentEntity, partialTicks, renderPass);
            }
        }
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawSelectionBox(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V"))
    private void drawSelectionBox(RenderGlobal instance, EntityPlayer player, MovingObjectPosition mop, int alwaysZero, float partialTicks) {
        if (handler == null) {
            instance.drawSelectionBox(player, mop, alwaysZero, partialTicks);
        }
    }

    private int orgRenderDistanceChunks;

    @Inject(method = "renderWorldPass", at = @At(value = "JUMP", ordinal = 0))
    private void beforeRenderSky(CallbackInfo ci) {
        if (handler != null && !handler.getOptions().isDefaultSky()) {
            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            orgRenderDistanceChunks = settings.renderDistanceChunks;
            settings.renderDistanceChunks = 5; // Set render distance to 5 so we're always rendering sky when chroma keying
        }
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(FI)V"))
    private void renderSky(RenderGlobal instance, float partialTicks, int renderPass) {
        if (handler != null && !handler.getOptions().isDefaultSky()) {
            Minecraft.getMinecraft().gameSettings.renderDistanceChunks = orgRenderDistanceChunks;
            int c = handler.getOptions().getSkyColor();
            GlStateManager.clearColor((c >> 16 & 0xff) / (float) 0xff, (c >> 8 & 0xff) / (float) 0xff, (c & 0xff) / (float) 0xff, 1);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        } else {
            instance.renderSky(partialTicks, renderPass);
        }
    }

    /*
     *   Cubic Renderer
     */

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 0))
    private void setupStereoscopicProjection(float partialTicks, int renderPass, CallbackInfo ci) {
        if (getHandler() != null) {
            if (getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GlStateManager.translate(0.07, 0, 0);
            } else if (getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GlStateManager.translate(-0.07, 0, 0);
            }
        }
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;loadIdentity()V", shift = At.Shift.AFTER, ordinal = 1))
    private void setupStereoscopicModelView(float partialTicks, int renderPass, CallbackInfo ci) {
        if (getHandler() != null) {
            if (getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                GlStateManager.translate(0.1, 0, 0);
            } else if (getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                GlStateManager.translate(-0.1, 0, 0);
            }
        }
    }

    /*
     *   Cubic Renderer
     */

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$0(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$1(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Redirect(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", remap = false))
    private void gluPerspective$2(float fovY, float aspect, float zNear, float zFar) {
        gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Override
    public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        if (getHandler() != null && getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            fovY = 90;
            aspect = 1;
        }
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void setupCubicFrameRotation(float partialTicks, CallbackInfo ci) {
        if (getHandler() != null && getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            switch ((CubicOpenGlFrameCapturer.Data) getHandler().data) {
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

            // Minecraft goes back a little so we have to invert that
            GlStateManager.translate(0.0F, 0.0F, 0.1F);
        }
    }


    /*
     *   Misc
     */


    @Inject(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", shift = At.Shift.AFTER, ordinal = 3))
    private void setupCameraRoll(float partialTicks, CallbackInfo ci) {
        if (ReplayHandler.isInReplay()) {
            GL11.glRotated(ReplayHandler.getCameraTilt(), 0D, 0D, 1D);
        }
    }
}
