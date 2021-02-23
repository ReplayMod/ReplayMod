package com.replaymod.render.mixin;

//#if MC>=10800
import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import java.util.Set;
//#else
//$$ import net.minecraft.client.render.VisibleRegion;
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC>=11400 && MC<11500
//$$ import net.minecraft.client.render.Camera;
//#endif

//#if MC<10904
//$$ import net.minecraft.client.renderer.chunk.RenderChunk;
//$$ import net.minecraft.util.BlockPos;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(WorldRenderer.class)
public abstract class Mixin_ForceChunkLoading {
    public ChunkLoadingRenderGlobal replayModRender_hook;

    //#if MC>=11500
    @Shadow private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;

    @Shadow private ChunkBuilder chunkBuilder;

    @Shadow private boolean needsTerrainUpdate;

    @Shadow public abstract void scheduleTerrainUpdate();

    @Shadow protected abstract void setupTerrain(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2);

    @Shadow private int frame;

    private boolean passThrough;
    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    private void forceAllChunks(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2, CallbackInfo ci) throws IllegalAccessException {
        if (replayModRender_hook == null) {
            return;
        }
        if (passThrough) {
            return;
        }
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
        ci.cancel();

        passThrough = true;
        try {
            do {
                // Determine which chunks shall be visible
                setupTerrain(camera_1, frustum_1, boolean_1, frame++, boolean_2);

                // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
                // all of them anyway and this way we can take advantage of threading)
                for (ChunkBuilder.BuiltChunk builtChunk : this.chunksToRebuild) {
                    // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                    if (builtChunk.shouldBuild()) {
                        builtChunk.scheduleRebuild(this.chunkBuilder);
                    }
                    builtChunk.cancelRebuild();
                }
                this.chunksToRebuild.clear();

                // Upload all chunks
                this.needsTerrainUpdate |= ((ChunkLoadingRenderGlobal.IBlockOnChunkRebuilds) this.chunkBuilder).uploadEverythingBlocking();

                // Repeat until no more updates are needed
            } while (this.needsTerrainUpdate);
        } finally {
            passThrough = false;
        }
    }
    //#else
    //$$ private boolean replayModRender_passThroughSetupTerrain;
    //$$
    //$$ @Shadow
    //$$ public boolean needsTerrainUpdate;
    //$$
    //$$ @Shadow
    //$$ public ChunkBuilder chunkBuilder;
    //$$
    //$$ @Shadow
    //$$ public abstract void setUpTerrain(
            //#if MC>=11400
            //$$ Camera viewEntity,
            //#else
            //$$ Entity viewEntity,
            //#if MC>=11400
            //$$ float partialTicks,
            //#else
            //$$ double partialTicks,
            //#endif
            //#endif
    //$$         VisibleRegion camera,
    //$$         int frameCount,
    //$$         boolean playerSpectator
    //$$ );
    //$$
    //$$ @Inject(method = "setUpTerrain", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModRender_setupTerrain(
            //#if MC>=11400
            //$$ Camera viewEntity,
            //#else
            //$$ Entity viewEntity,
            //#if MC>=11400
            //$$ float partialTicks,
            //#else
            //$$ double partialTicks,
            //#endif
            //#endif
    //$$         VisibleRegion camera,
    //$$         int frameCount,
    //$$         boolean playerSpectator,
    //$$         CallbackInfo ci
    //$$ ) throws IllegalAccessException {
    //$$     if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
    //$$         return;
    //$$     }
    //$$     if (replayModRender_hook != null && !replayModRender_passThroughSetupTerrain) {
    //$$         replayModRender_passThroughSetupTerrain = true;
    //$$
    //$$         do {
    //$$             setUpTerrain(
    //$$                     viewEntity,
                        //#if MC<11400
                        //$$ partialTicks,
                        //#endif
    //$$                     camera,
    //$$                     replayModRender_hook.nextFrameId(),
    //$$                     playerSpectator
    //$$             );
    //$$             replayModRender_hook.updateChunks();
    //$$         } while (this.needsTerrainUpdate);
    //$$
    //$$         this.needsTerrainUpdate = true;
    //$$
    //$$         replayModRender_passThroughSetupTerrain = false;
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //#if MC<10904
    //$$ @Inject(method = "isPositionInRenderChunk", at = @At("HEAD"), cancellable = true)
    //$$ public void replayModRender_isPositionInRenderChunk(BlockPos pos, RenderChunk chunk, CallbackInfoReturnable<Boolean> ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         ci.setReturnValue(true);
    //$$     }
    //$$ }
    //#endif
    //$$
    //$$ @Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true)
    //$$ public void replayModRender_updateChunks(long finishTimeNano, CallbackInfo ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         replayModRender_hook.updateChunks();
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ // Prior to 1.9.4, MC always uses the same ChunkRenderDispatcher instance
    //#if MC>=10904
    //$$ @Inject(method = "setWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;stop()V"))
    //$$ private void stopWorkerThreadsAndChunkLoadingRenderGlobal(CallbackInfo ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         replayModRender_hook.updateRenderDispatcher(null);
    //$$     }
    //$$ }
    //#endif
    //$$
    //$$ @Inject(method = "reload", at = @At(value = "RETURN"))
    //$$ private void setupChunkLoadingRenderGlobal(CallbackInfo ci) {
    //$$     if (replayModRender_hook != null) {
    //$$         replayModRender_hook.updateRenderDispatcher(this.chunkBuilder);
    //$$     }
    //$$ }
    //#endif
}
//#endif
