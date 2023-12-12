package com.replaymod.render.mixin;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkRenderingDataPreparer.class)
public interface ChunkRenderingDataPreparerAccessor {
    @Accessor("field_45623")
    BuiltChunkStorage builtChunkStorage();

    @Accessor("field_45621")
    boolean shouldUpdate();

    @Accessor("field_45622")
    Future<?> fullUpdateFuture();
}
