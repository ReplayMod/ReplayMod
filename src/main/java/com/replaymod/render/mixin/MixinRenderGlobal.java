package com.replaymod.render.mixin;

import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    public ChunkLoadingRenderGlobal replayModRender_hook;
    private boolean replayModRender_passThroughSetupTerrain;

    @Shadow
    public boolean displayListEntitiesDirty;

    @Shadow
    public ChunkRenderDispatcher renderDispatcher;

    @Shadow
    public abstract void setupTerrain(Entity viewEntity, double partialTicks, ICamera camera,
                                      int frameCount, boolean playerSpectator);

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    public void replayModRender_setupTerrain(Entity viewEntity, double partialTicks, ICamera camera,
                                               int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (replayModRender_hook != null && !replayModRender_passThroughSetupTerrain) {
            replayModRender_passThroughSetupTerrain = true;

            do {
                setupTerrain(viewEntity, partialTicks, camera, replayModRender_hook.nextFrameId(), playerSpectator);

                replayModRender_hook.updateChunks();
            } while (displayListEntitiesDirty);

            displayListEntitiesDirty = true;

            replayModRender_passThroughSetupTerrain = false;
            ci.cancel();
        }
    }

    @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    public void replayModRender_updateChunks(long finishTimeNano, CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateChunks();
            ci.cancel();
        }
    }
}
