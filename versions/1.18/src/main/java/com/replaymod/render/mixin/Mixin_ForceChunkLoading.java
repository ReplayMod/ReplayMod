package com.replaymod.render.mixin;

import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import com.replaymod.render.utils.FlawlessFrames;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.BlockingQueue;
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

    @Shadow private ChunkBuilder chunkBuilder;

    @Shadow protected abstract void setupTerrain(Camera par1, Frustum par2, boolean par3, boolean par4);

    @Shadow private Frustum frustum;

    @Shadow private Frustum capturedFrustum;

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private ObjectArrayList<ChunkInfoAccessor> chunkInfos;

    @Shadow private boolean field_34810;

    @Shadow @Final private BlockingQueue<ChunkBuilder.BuiltChunk> builtChunks;

    @Shadow private Future<?> field_34808;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"))
    private void forceAllChunks(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (replayModRender_hook == null) {
            return;
        }
        if (FlawlessFrames.hasSodium()) {
            return;
        }

        assert this.client.player != null;

        ChunkRendererRegionBuilder chunkRendererRegionBuilder = new ChunkRendererRegionBuilder();

        do {
            // Determine which chunks shall be visible
            setupTerrain(camera, this.frustum, this.capturedFrustum != null, this.client.player.isSpectator());

            // Wait for async processing to be complete
            if (this.field_34808 != null) {
                try {
                    this.field_34808.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
            // all of them anyway and this way we can take advantage of threading)
            for (ChunkInfoAccessor chunkInfo : this.chunkInfos) {
                ChunkBuilder.BuiltChunk builtChunk = chunkInfo.getChunk();
                if (!builtChunk.needsRebuild()) {
                    continue;
                }
                // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                if (builtChunk.shouldBuild()) {
                    builtChunk.scheduleRebuild(this.chunkBuilder, chunkRendererRegionBuilder);
                }
                builtChunk.cancelRebuild();
            }

            // Upload all chunks
            this.field_34810 |= ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkBuilder).uploadEverythingBlocking();

            // Repeat until no more updates are needed
        } while (this.field_34810 || !this.builtChunks.isEmpty());
    }
}
