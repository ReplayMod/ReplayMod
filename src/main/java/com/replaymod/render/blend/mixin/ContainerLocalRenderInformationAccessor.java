//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=11500
//$$ import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
//#else
import net.minecraft.client.render.chunk.ChunkRenderer;
//#endif

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public interface ContainerLocalRenderInformationAccessor {
    //#if MC>=11500
    //$$ @Accessor("chunk")
    //$$ BuiltChunk getRenderChunk();
    //#else
    //#if MC>=11400
    @Accessor("renderer")
    //#else
    //$$ @Accessor
    //#endif
    ChunkRenderer getRenderChunk();
    //#endif
}
//#endif
