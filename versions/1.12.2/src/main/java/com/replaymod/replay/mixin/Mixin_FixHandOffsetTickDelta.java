package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * In 1.12.2 and below, Vanilla does not respect the tickDelta value when getting the yaw/pitch of the player for
 * computing the hand offset.
 * This causes the hand movement to be jittery when spectating another player in a replay.
 */
@Mixin(ItemRenderer.class)
public abstract class Mixin_FixHandOffsetTickDelta {
    @Redirect(method = "rotateArm", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;rotationYaw:F"))
    private float getYaw(
            EntityPlayerSP instance,
            //#if MC<10900
            //$$ EntityPlayerSP arg,
            //#endif
            float tickDelta
    ) {
        if (instance instanceof CameraEntity) {
            return instance.prevRotationYaw + (instance.rotationYaw - instance.prevRotationYaw) * tickDelta;
        } else {
            return instance.rotationYaw;
        }
    }

    @Redirect(method = "rotateArm", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;rotationPitch:F"))
    private float getPitch(
            EntityPlayerSP instance,
            //#if MC<10900
            //$$ EntityPlayerSP arg,
            //#endif
            float tickDelta
    ) {
        if (instance instanceof CameraEntity) {
            return instance.prevRotationPitch + (instance.rotationPitch - instance.prevRotationPitch) * tickDelta;
        } else {
            return instance.rotationPitch;
        }
    }
}
