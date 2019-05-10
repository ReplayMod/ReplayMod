//#if MC>=10800
package com.replaymod.replay.mixin;

import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinViewFrustum {
    @Redirect(
            method = "updateCameraPosition",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=10904
                    target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;method_3653(III)V"
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(Lnet/minecraft/util/BlockPos;)V"
                    //#endif
            )
    )
    private void replayModReplay_updatePositionAndMarkForUpdate(
            ChunkRenderer renderChunk,
            //#if MC>=10904
            int x, int y, int z
            //#else
            //$$ BlockPos pos
            //#endif
    ) {
        //#if MC>=10904
        BlockPos pos = new BlockPos(x, y, z);
        //#endif
        if (!pos.equals(renderChunk.getOrigin())) {
            //#if MC>=10904
            renderChunk.method_3653(x, y, z);
            renderChunk.scheduleRender(false);
            //#else
            //$$ renderChunk.setPosition(pos);
            //$$ renderChunk.setNeedsUpdate(true);
            //#endif
        }
    }
}
//#endif
