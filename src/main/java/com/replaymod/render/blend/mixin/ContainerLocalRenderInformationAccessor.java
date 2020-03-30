//#if MC>=10800
package com.replaymod.render.blend.mixin;

import net.minecraft.client.render.chunk.ChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public interface ContainerLocalRenderInformationAccessor {
    //#if MC>=11500
    //$$ @Accessor("chunk")
    //#else
    //#if MC>=11400
    @Accessor("renderer")
    //#else
    //$$ @Accessor
    //#endif
    //#endif
    ChunkRenderer getRenderChunk();
}
//#endif
