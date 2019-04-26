package com.replaymod.render.mixin;

import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

//#if MC>=10800
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
//#endif

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor
    void setRenderEntitiesStartupCounter(int value);

    //#if MC>=10800
    @Accessor
    ChunkRenderDispatcher getRenderDispatcher();

    @Accessor
    void setDisplayListEntitiesDirty(boolean value);

    @Accessor
    Set<RenderChunk> getChunksToUpdate();
    //#endif
}
