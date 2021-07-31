package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class Mixin_ChromaKeyDisableFog implements EntityRendererHandler.IEntityRenderer {
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void replayModRender_onSetupFog(int fogDistanceFlag, float partialTicks, CallbackInfo ci) {
        EntityRendererHandler handler = replayModRender_getHandler();
        if (handler != null && handler.getSettings().getChromaKeyingColor() != null) {
            ci.cancel();
        }
    }
}
