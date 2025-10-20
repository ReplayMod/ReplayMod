package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BillboardParticle.class)
public abstract class MixinParticleManager {
    @Shadow public abstract void render(BillboardParticleSubmittable par1, Camera par2, float par3);

    @Inject(method = "render(Lnet/minecraft/client/particle/BillboardParticleSubmittable;Lnet/minecraft/client/render/Camera;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/BillboardParticle$Rotator;setRotation(Lorg/joml/Quaternionf;Lnet/minecraft/client/render/Camera;F)V"))
    private void faceCameraAtParticle(
            CallbackInfo ci,
            @Local(argsOnly = true) float partialTicks,
            @Local(argsOnly = true) Camera camera,
            @Share("orgRotation") LocalRef<Quaternionf> orgRotationRef
    ) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            return;
        }

        Quaternionf rotation = camera.getRotation();
        orgRotationRef.set(new Quaternionf(rotation));

        Vec3d from = new Vec3d(0, 0, -1);
        Vec3d to = MCVer.getPosition((BillboardParticle)(Object) this, partialTicks)
                .subtract(camera.getPos())
                .normalize();
        Vec3d axis = from.crossProduct(to);
        rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dotProduct(to)));
        rotation.normalize();
    }

    @Inject(method = "render(Lnet/minecraft/client/particle/BillboardParticleSubmittable;Lnet/minecraft/client/render/Camera;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/BillboardParticle$Rotator;setRotation(Lorg/joml/Quaternionf;Lnet/minecraft/client/render/Camera;F)V", shift = At.Shift.AFTER))
    private void faceCameraAtParticleCleanup(
            CallbackInfo ci,
            @Local(argsOnly = true) Camera camera,
            @Share("orgRotation") LocalRef<Quaternionf> orgRotationRef
    ) {
        Quaternionf orgRotation = orgRotationRef.get();
        if (orgRotation == null) {
            return;
        }

        camera.getRotation().set(orgRotation.x, orgRotation.y, orgRotation.z, orgRotation.w);
    }
}
