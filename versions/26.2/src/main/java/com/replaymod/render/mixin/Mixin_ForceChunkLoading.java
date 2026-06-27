package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.utils.FlawlessFrames;
import net.minecraft.client.Camera;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Mixin(LevelExtractor.class)
public abstract class Mixin_ForceChunkLoading {
    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Shadow
    private @Nullable SectionUpdateTracker sectionUpdateTracker;

    @Shadow
    private @Nullable ClientLevel level;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    protected abstract void applyFrustum(Frustum frustum);

    //#if MC >= 26.1
    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections()Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private void forceAllChunks(
            CallbackInfo ci,
            @Local(argsOnly = true) Camera camera,
            @Local(name = "cullFrustum") Frustum cullFrustum,
            @Local(name = "cache") RenderRegionCache cache
    ) {
        if (!ForceChunkLoadingHook.enabled || FlawlessFrames.hasSodium()) {
            return;
        }

        SectionOcclusionGraph sectionOcclusionGraph = this.levelRenderer.sectionOcclusionGraph();
        ChunkRenderingDataPreparerAccessor sectionOcclusionGraphAcc = (ChunkRenderingDataPreparerAccessor) sectionOcclusionGraph;

        while (true) {
            boolean areWeDoneYet = true;

            // Update (or schedule update) of occlusion graph to account for newly loaded (and later also newly compiled) chunks
            // TODO may need to switch to the render thread for this in the future
            sectionOcclusionGraph.update(this.levelRenderState.cameraRenderState, (int) camera.getFov(), this.levelRenderState.chunkLoadingRenderState);

            // Wait for any async occlusion graph updates to be done
            Future<?> fullUpdateFuture = sectionOcclusionGraphAcc.fullUpdateFuture();
            if (fullUpdateFuture != null) {
                try {
                    fullUpdateFuture.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            // Re-compute visible chunks if occlusion graph has changed
            if (sectionOcclusionGraph.consumeFrustumUpdate()) {
                this.applyFrustum(cullFrustum);
            }

            // Schedule changed and newly visible chunks for updating
            for (SectionRenderDispatcher.RenderSection section : this.levelRenderer.visibleSections()) {
                SectionUpdateTracker.SectionDirtyState dirtyState = this.sectionUpdateTracker.getDirtyState(section.getSectionNode());
                if (dirtyState != null
                        && dirtyState.isDirty()
                        && (section.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED || this.sectionUpdateTracker.hasAllNeighbors(this.level, section.getSectionNode()))) {
                    section.compileAsync(cache.createRegion(this.level, section.getSectionNode()));
                    section.setFadeDuration(0);
                    dirtyState.setNotDirty();
                    areWeDoneYet = false;
                }
            }

            // Wait for chunks to be compiled and uploaded to the GPU
            // TODO may need to switch to the render thread for this in the future
            if (((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.levelRenderer.sectionRenderDispatcher()).uploadEverythingBlocking()) {
                areWeDoneYet = false;
            }

            // Repeat until no more updates are needed
            if (areWeDoneYet) {
                break;
            }
        }
    }
}
