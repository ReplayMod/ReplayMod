package com.replaymod.render.mixin;

//#if MC>=10800 && MC<11500

import com.replaymod.core.versions.MCVer;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker implements MCVer.ChunkRenderWorkerAccessor {
    @Shadow abstract void runTask(ChunkRenderTask task) throws InterruptedException;

    public void doRunTask(ChunkRenderTask task) throws InterruptedException {
        runTask(task);
    }
}
//#endif
