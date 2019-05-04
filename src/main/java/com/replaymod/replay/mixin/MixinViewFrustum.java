//#if MC>=10800
package com.replaymod.replay.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    @Redirect(
            method = "updateChunkPositions",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=10904
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(III)V"
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(Lnet/minecraft/util/BlockPos;)V"
                    //#endif
            )
    )
    private void replayModReplay_updatePositionAndMarkForUpdate(
            RenderChunk renderChunk,
            //#if MC>=10904
            int x, int y, int z
            //#else
            //$$ BlockPos pos
            //#endif
    ) {
        //#if MC>=10904
        BlockPos pos = new BlockPos(x, y, z);
        //#endif
        if (!pos.equals(renderChunk.getPosition())) {
            //#if MC>=10904
            renderChunk.setPosition(x, y, z);
            renderChunk.setNeedsUpdate(false);
            //#else
            //$$ renderChunk.setPosition(pos);
            //$$ renderChunk.setNeedsUpdate(true);
            //#endif
        }
    }
}
//#endif
