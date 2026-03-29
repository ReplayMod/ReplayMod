package com.replaymod.replay.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mixin(Camera.class)
public class MixinCamera {
    @Shadow
    private @Nullable Entity entity;

    @ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
    private float applyRoll(float rotZ) {
        Entity entity = this.entity;
        if (!(entity instanceof CameraEntity)) return rotZ;

        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.getSettings().isStabilizeRoll()) return rotZ;

        return rotZ - ((CameraEntity) entity).roll * (float) Math.PI / 180f;
    }
}
