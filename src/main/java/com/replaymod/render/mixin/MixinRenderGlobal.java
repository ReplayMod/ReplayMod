//#if MC>=10800
package com.replaymod.render.mixin;

import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import net.minecraft.client.render.chunk.ChunkBatcher;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#endif

//#if MC>=11300
import net.minecraft.client.render.WorldRenderer;
//#else
//$$ import net.minecraft.client.renderer.RenderGlobal;
//#endif

//#if MC<10904
//$$ import net.minecraft.client.renderer.chunk.RenderChunk;
//$$ import net.minecraft.util.BlockPos;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC>=11300
@Mixin(WorldRenderer.class)
//#else
//$$ @Mixin(RenderGlobal.class)
//#endif
public abstract class MixinRenderGlobal {
    public ChunkLoadingRenderGlobal replayModRender_hook;
    private boolean replayModRender_passThroughSetupTerrain;

    @Shadow
    public boolean terrainUpdateNecessary;

    @Shadow
    public ChunkBatcher chunkBatcher;

    @Shadow
    public abstract void setUpTerrain(
            //#if MC>=11400
            Camera viewEntity,
            //#else
            //$$ Entity viewEntity,
            //#if MC>=11300
            //$$ float partialTicks,
            //#else
            //$$ double partialTicks,
            //#endif
            //#endif
            VisibleRegion camera,
            int frameCount,
            boolean playerSpectator
    );

    @Inject(method = "setUpTerrain", at = @At("HEAD"), cancellable = true)
    private void replayModRender_setupTerrain(
            //#if MC>=11400
            Camera viewEntity,
            //#else
            //$$ Entity viewEntity,
            //#if MC>=11300
            //$$ float partialTicks,
            //#else
            //$$ double partialTicks,
            //#endif
            //#endif
            VisibleRegion camera,
            int frameCount,
            boolean playerSpectator,
            CallbackInfo ci
    ) {
        if (replayModRender_hook != null && !replayModRender_passThroughSetupTerrain) {
            replayModRender_passThroughSetupTerrain = true;

            do {
                setUpTerrain(
                        viewEntity,
                        //#if MC<11400
                        //$$ partialTicks,
                        //#endif
                        camera,
                        replayModRender_hook.nextFrameId(),
                        playerSpectator
                );
                replayModRender_hook.updateChunks();
            } while (this.terrainUpdateNecessary);

            this.terrainUpdateNecessary = true;

            replayModRender_passThroughSetupTerrain = false;
            ci.cancel();
        }
    }

    //#if MC<10904
    //$$ @Inject(method = "isPositionInRenderChunk", at = @At("HEAD"), cancellable = true)
    //$$ public void replayModRender_isPositionInRenderChunk(BlockPos pos, RenderChunk chunk, CallbackInfoReturnable<Boolean> ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         ci.setReturnValue(true);
    //$$     }
    //$$ }
    //#endif

    @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    public void replayModRender_updateChunks(long finishTimeNano, CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateChunks();
            ci.cancel();
        }
    }

    // Prior to 1.9.4, MC always uses the same ChunkRenderDispatcher instance
    //#if MC>=10904
    @Inject(method = "setWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBatcher;method_3619()V"))
    private void stopWorkerThreadsAndChunkLoadingRenderGlobal(CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateRenderDispatcher(null);
        }
    }
    //#endif

    @Inject(method = "reload", at = @At(value = "RETURN"))
    private void setupChunkLoadingRenderGlobal(CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateRenderDispatcher(this.chunkBatcher);
        }
    }
}
//#endif
