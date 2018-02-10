package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=10904
import net.minecraft.util.math.BlockPos;
//#else
//$$ import net.minecraft.util.BlockPos;
//#endif

import static com.replaymod.core.versions.MCVer.*;

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    private IRenderChunkFactory renderChunkFactory;

    @Shadow protected RenderGlobal renderGlobal;
    @Shadow protected World world;
    @Shadow protected int countChunksY;
    @Shadow protected int countChunksX;
    @Shadow protected int countChunksZ;
    @Shadow public RenderChunk[] renderChunks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setRenderChunkFactory(World a1, int a2, RenderGlobal a3, IRenderChunkFactory rcf, CallbackInfo ci) {
        this.renderChunkFactory = rcf;
    }

    /**
     * Instead of updating its position, we recreate the render chunk
     * which seems to solve the problem that chunks are invisible when you leave an area and return
     * to it.
     * Any better fixes are welcome.
     * Note: Most of this code is copied from {@link ViewFrustum#updateChunkPositions(double, double)}
     */
    @Inject(method = "updateChunkPositions", at = @At("HEAD"), cancellable = true)
    public void fixedUpdateChunkPositions(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        if (ReplayModReplay.instance.getReplayHandler() == null) {
            return;
        }

        int i = floor(viewEntityX) - 8;
        int j = floor(viewEntityZ) - 8;
        int k = this.countChunksX * 16;

        for (int l = 0; l < this.countChunksX; ++l) {
            int i1 = this.getBaseCoordinate(i, k, l);
            for (int j1 = 0; j1 < this.countChunksZ; ++j1) {
                int k1 = this.getBaseCoordinate(j, k, j1);
                for (int l1 = 0; l1 < this.countChunksY; ++l1) {
                    int i2 = l1 * 16;
                    RenderChunk renderchunk = this.renderChunks[(j1 * this.countChunksY + l1) * this.countChunksX + l];
                    BlockPos blockpos = new BlockPos(i1, i2, k1);
                    if (!blockpos.equals(renderchunk.getPosition())) {
                        // Recreate render chunk instead of setting its position
                        //#if MC>=10904
                        (renderChunks[(j1 * this.countChunksY + l1) * this.countChunksX + l] =
                                renderChunkFactory.create(world, renderGlobal, 0)
                        //#if MC>= 11100
                        ).setPosition(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                        //#else
                        //$$ ).setOrigin(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                        //#endif
                        //#else
                        //$$ renderChunks[(j1 * this.countChunksY + l1) * this.countChunksX + l] =
                        //$$         renderChunkFactory.makeRenderChunk(world, renderGlobal, blockpos, 0);
                        //#endif
                    }
                }
            }
        }
        ci.cancel();
    }

    //#if MC>=10904
    @Shadow abstract int getBaseCoordinate(int p_178157_1_, int p_178157_2_, int p_178157_3_);
    //#else
    //$$ private int getBaseCoordinate(int p_178157_1_, int p_178157_2_, int p_178157_3_) {
    //$$     return func_178157_a(p_178157_1_, p_178157_2_, p_178157_3_);
    //$$ }
    //$$ @Shadow abstract int func_178157_a(int p_178157_1_, int p_178157_2_, int p_178157_3_);
    //#endif
}
