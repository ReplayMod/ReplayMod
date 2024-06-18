package com.replaymod.render.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import com.replaymod.render.utils.FlawlessFrames;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
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

@Mixin(WorldRenderer.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    @Shadow private ChunkBuilder field_45614;

    @Shadow @Final private ChunkRenderingDataPreparer field_45615;

    @Shadow protected abstract void setupTerrain(Camera par1, Frustum par2, boolean par3, boolean par4);

    @Shadow private Frustum frustum;

    @Shadow private Frustum capturedFrustum;

    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract void applyFrustum(Frustum par1);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"))
    private void forceAllChunks(CallbackInfo ci, @Local(argsOnly = true) Camera camera) {
        if (replayModRender_hook == null) {
            return;
        }
        if (FlawlessFrames.hasSodium()) {
            return;
        }

        assert this.client.player != null;

        ChunkRenderingDataPreparer renderingData = this.field_45615;
        ChunkRenderingDataPreparerAccessor renderingDataAcc = (ChunkRenderingDataPreparerAccessor) renderingData;
        ChunkRendererRegionBuilder chunkRendererRegionBuilder = new ChunkRendererRegionBuilder();

        do {
            boolean areWeDoneYet = true;

            // Determine which chunks shall be visible
            setupTerrain(camera, this.frustum, this.capturedFrustum != null, this.client.player.isSpectator());

            // Wait for async processing to be complete
            Future<?> fullUpdateFuture = renderingDataAcc.fullUpdateFuture();
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

            // If that async processing did change the chunk graph, we need to re-apply the frustum (otherwise this is
            // only done in the next setupTerrain call, which not happen this frame)
            if (renderingData.method_52836()) {
                this.applyFrustum((new Frustum(frustum)).coverBoxAroundSetPosition(8)); // call based on the one in setupTerrain
            }

            // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
            // all of them anyway and this way we can take advantage of threading)
            for (ChunkBuilder.BuiltChunk builtChunk : renderingDataAcc.builtChunkStorage().chunks) {
                if (!builtChunk.needsRebuild()) {
                    continue;
                }
                // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                if (builtChunk.shouldBuild()) {
                    builtChunk.scheduleRebuild(this.field_45614, chunkRendererRegionBuilder);
                    areWeDoneYet = false;
                }
                builtChunk.cancelRebuild();
            }

            // Upload all chunks
            if (((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.field_45614).uploadEverythingBlocking()) {
                areWeDoneYet = false;
            }

            // Repeat until no more updates are needed
            if (!areWeDoneYet) {
                renderingData.method_52817(); // sets shouldUpdate to true
            }
        } while (renderingDataAcc.shouldUpdate());
    }
}
