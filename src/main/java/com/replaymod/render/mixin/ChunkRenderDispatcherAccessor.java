//#if MC>=10800
package com.replaymod.render.mixin;

import net.minecraft.client.render.chunk.ChunkBatcher;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
//#if MC>=10904
import java.util.concurrent.PriorityBlockingQueue;
//#else
//$$ import java.util.concurrent.BlockingQueue;
//#endif

@Mixin(ChunkBatcher.class)
public interface ChunkRenderDispatcherAccessor {
    @Accessor("workers")
    List<ChunkRenderWorker> getListThreadedWorkers();

    //#if MC>=10904
    @Accessor("pendingChunks")
    PriorityBlockingQueue<ChunkRenderTask> getQueueChunkUpdates();
    @Accessor("pendingChunks")
    void setQueueChunkUpdates(PriorityBlockingQueue<ChunkRenderTask> value);
    //#else
    //$$ @Accessor
    //$$ BlockingQueue<ChunkCompileTaskGenerator> getQueueChunkUpdates();
    //$$ @Accessor
    //$$ void setQueueChunkUpdates(BlockingQueue<ChunkCompileTaskGenerator> value);
    //#endif
}
//#endif
