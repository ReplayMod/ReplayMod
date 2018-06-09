//#if MC>=10800
package com.replaymod.replay.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=10904
import net.minecraft.util.math.BlockPos;
//#else
//$$ import net.minecraft.util.BlockPos;
//#endif

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    @Redirect(method = "updateChunkPositions", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(III)V"))
    private void replayModReplay_updatePositionAndMarkForUpdate(RenderChunk renderChunk, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!pos.equals(renderChunk.getPosition())) {
            //#if MC>=10904
            //#if MC>=11100
            renderChunk.setPosition(x, y, z);
            //#else
            //$$ renderChunk.setOrigin(x, y, z);
            //#endif
            renderChunk.setNeedsUpdate(false);
            //#else
            //$$ renderChunk.setPosition(pos);
            //$$ renderChunk.setNeedsUpdate(true);
            //#endif
        }
    }
}
//#endif
