package com.replaymod.render.mixin;

import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
            } while (displayListEntitiesDirty);

            replayModRender_passThroughSetupTerrain = false;
            ci.cancel();
        }
    }

    @Inject(method = "isPositionInRenderChunk", at = @At("HEAD"), cancellable = true)
    public void replayModRender_isPositionInRenderChunk(BlockPos pos, RenderChunk chunk, CallbackInfoReturnable<Boolean> ci) {
        if (replayModRender_hook != null) {
            ci.setReturnValue(true);
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
