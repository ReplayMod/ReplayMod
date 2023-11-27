// 1.18 - 1.20.1
package com.replaymod.render.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public interface ChunkInfoAccessor {
    @Accessor
    ChunkBuilder.BuiltChunk getChunk();
}
