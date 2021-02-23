package com.replaymod.render.mixin;

//#if MC>=10800
import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#else
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC<10904
//$$ import net.minecraft.client.renderer.chunk.RenderChunk;
//$$ import net.minecraft.util.BlockPos;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(WorldRenderer.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    private boolean replayModRender_passThroughSetupTerrain;

    @Shadow
    private boolean needsTerrainUpdate;

    @Shadow
    private ChunkBuilder chunkBuilder;

    @Shadow
    public abstract void setUpTerrain(
            //#if MC>=11400
            Camera viewEntity,
            //#else
            //$$ Entity viewEntity,
            //$$ double partialTicks,
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
            //$$ double partialTicks,
            //#endif
            VisibleRegion camera,
            int frameCount,
            boolean playerSpectator,
            CallbackInfo ci
    ) throws IllegalAccessException {
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
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
            } while (this.needsTerrainUpdate);

            this.needsTerrainUpdate = true;

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
    @Inject(method = "setWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;stop()V"))
    private void stopWorkerThreadsAndChunkLoadingRenderGlobal(CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateRenderDispatcher(null);
        }
    }
    //#endif

    @Inject(method = "reload", at = @At(value = "RETURN"))
    private void setupChunkLoadingRenderGlobal(CallbackInfo ci) {
        if (replayModRender_hook != null) {
            replayModRender_hook.updateRenderDispatcher(this.chunkBuilder);
        }
    }
}
//#endif
