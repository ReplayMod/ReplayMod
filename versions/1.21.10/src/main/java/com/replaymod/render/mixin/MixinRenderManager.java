package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public abstract class MixinRenderManager {
    @Inject(method = "render", at = @At("HEAD"))
    private void replayModRender_reorientForCubicRendering(
            CallbackInfo ci,
            @Local(argsOnly = true, ordinal = 0) double dx,
            @Local(argsOnly = true, ordinal = 1) double dy,
            @Local(argsOnly = true, ordinal = 2) double dz,
            @Local(argsOnly = true) LocalRef<CameraRenderState> cameraRenderStateRef
    ) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            return;
        }

        CameraRenderState org = cameraRenderStateRef.get();
        CameraRenderState copy = new CameraRenderState();
        copy.blockPos = org.blockPos;
        copy.pos = org.pos;
        copy.initialized = org.initialized;
        copy.entityPos = org.entityPos;
        copy.orientation.lookAlong((float) dx, (float) dy, (float) dz, 0f, 1f, 0f);
        cameraRenderStateRef.set(copy);
    }
}
