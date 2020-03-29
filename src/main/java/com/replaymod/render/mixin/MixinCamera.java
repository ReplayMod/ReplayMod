package com.replaymod.render.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.*;

//#if MC>=11400
import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
//#else
//$$ import net.minecraft.client.renderer.EntityRenderer;
//#endif

//#if MC>=11400
@Mixin(value = Camera.class)
//#else
//$$ @Mixin(value = EntityRenderer.class)
//#endif
public abstract class MixinCamera {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    // Only relevant on 1.13+ (previously MC always used the non-head yaw) and only for LivingEntity view entities.
    private float orgHeadYaw;
    private float orgPrevHeadYaw;

    //#if MC>=11400
    @Inject(method = "update", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At("HEAD"))
    //#endif
    private void replayModRender_beforeSetupCameraTransform(
            //#if MC>=11400
            BlockView blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            orgYaw = entity.yaw;
            orgPitch = entity.pitch;
            orgPrevYaw = entity.prevYaw;
            orgPrevPitch = entity.prevPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
            if (entity instanceof LivingEntity) {
                orgHeadYaw = ((LivingEntity) entity).headYaw;
                orgPrevHeadYaw = ((LivingEntity) entity).prevHeadYaw;
            }
        }
    //#if MC<11400
    //$$ }
    //$$
    //$$ @Inject(method = "orientCamera", at = @At("HEAD"))
    //$$ private void replayModRender_resetRotationIfNeeded(float partialTicks, CallbackInfo ci) {
    //#endif
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            RenderSettings settings = getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                entity.prevYaw = entity.yaw = 0;
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).prevHeadYaw = ((LivingEntity) entity).headYaw = 0;
                }
            }
            if (settings.isStabilizePitch()) {
                entity.prevPitch = entity.pitch = 0;
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    //#if MC>=11400
    @Inject(method = "update", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "setupCameraTransform", at = @At("RETURN"))
    //#endif
    private void replayModRender_afterSetupCameraTransform(
            //#if MC>=11400
            BlockView blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            //#if MC<11400
            //$$ Entity entity = getRenderViewEntity(getMinecraft());
            //#endif
            entity.yaw = orgYaw;
            entity.pitch = orgPitch;
            entity.prevYaw = orgPrevYaw;
            entity.prevPitch = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).headYaw = orgHeadYaw;
                ((LivingEntity) entity).prevHeadYaw = orgPrevHeadYaw;
            }
        }
    }
}
