package com.replaymod.render.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
//$$ import net.minecraft.client.render.Camera;
//$$ import net.minecraft.world.BlockView;
//#else
import net.minecraft.client.renderer.GameRenderer;
//#endif

//#if MC>=11400
//$$ @Mixin(value = Camera.class)
//#else
@Mixin(value = GameRenderer.class)
//#endif
public abstract class MixinCamera {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().entityRenderer).replayModRender_getHandler();
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    //#if MC>=11400
    //$$ @Inject(method = "update", at = @At("HEAD"))
    //#else
    @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    //#endif
    private void replayModRender_beforeSetupCameraTransform(
            //#if MC>=11400
            //$$ BlockView blockView,
            //$$ Entity entity,
            //$$ boolean thirdPerson,
            //$$ boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
        }
    //#if MC<11400
    }

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
    //#endif
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            RenderSettings settings = getHandler().getSettings();
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

    //#if MC>=11400
    //$$ @Inject(method = "update", at = @At("RETURN"))
    //#else
    @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    //#endif
    private void replayModRender_afterSetupCameraTransform(
            //#if MC>=11400
            //$$ BlockView blockView,
            //$$ Entity entity,
            //$$ boolean thirdPerson,
            //$$ boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11300
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            entity.rotationYaw = orgYaw;
            entity.rotationPitch = orgPitch;
            entity.prevRotationYaw = orgPrevYaw;
            entity.prevRotationPitch = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
        }
    }

    //#if MC>=11400
    //$$ @Inject(method = "update", at = @At("HEAD"))
    //#else
    @Inject(method = "orientCamera", at = @At("HEAD"))
    //#endif
    private void replayModRender_setupCubicFrameRotation(
            //#if MC>=11400
            //$$ BlockView blockView,
            //$$ Entity entity,
            //$$ boolean thirdPerson,
            //$$ boolean inverseView,
            //#endif
            float partialTicks,
            CallbackInfo ci
    ) {
        if (getHandler() != null && getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            switch ((CubicOpenGlFrameCapturer.Data) getHandler().data) {
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
        if (getHandler() != null && getHandler().omnidirectional) {
            // Minecraft goes back a little so we have to revert that
            //#if MC>=11300
            GL11.glTranslatef(0.0F, 0.0F, -0.05F);
            //#else
            //$$ GL11.glTranslatef(0.0F, 0.0F, 0.1F);
            //#endif
        }
    }
}
