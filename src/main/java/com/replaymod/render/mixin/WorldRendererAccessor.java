package com.replaymod.render.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

//#if MC>=10800
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRenderer;
//#endif

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    //#if MC<11500
    @Accessor("field_4076")
    void setRenderEntitiesStartupCounter(int value);

    //#if MC>=10800
    @Accessor("chunkBuilder")
    ChunkBuilder getRenderDispatcher();

    @Accessor("needsTerrainUpdate")
    void setDisplayListEntitiesDirty(boolean value);

    @Accessor("chunksToRebuild")
    Set<ChunkRenderer> getChunksToUpdate();
    //#endif
    //#endif
}
