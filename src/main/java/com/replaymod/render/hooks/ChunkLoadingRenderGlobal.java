package com.replaymod.render.hooks;

import com.replaymod.render.utils.JailingQueue;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.client.renderer.chunk.RenderChunk;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

public class ChunkLoadingRenderGlobal {

    private final RenderGlobal hooked;
    private final ChunkRenderDispatcher renderDispatcher;
    private final JailingQueue<ChunkCompileTaskGenerator> workerJailingQueue;
    private final CustomChunkRenderWorker renderWorker;
    private int frame;

    @SuppressWarnings("unchecked")
    public ChunkLoadingRenderGlobal(RenderGlobal renderGlobal) {
        this.hooked = renderGlobal;
        this.renderDispatcher = renderGlobal.renderDispatcher;
        this.renderWorker = new CustomChunkRenderWorker(renderDispatcher, new RegionRenderCacheBuilder());

        int workerThreads = renderDispatcher.listThreadedWorkers.size();
        BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = renderDispatcher.queueChunkUpdates;
        workerJailingQueue = new JailingQueue<>(queueChunkUpdates);
        renderDispatcher.queueChunkUpdates = workerJailingQueue;
        ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null);
        element.finish();
        for (int i = 0; i < workerThreads; i++) {
            queueChunkUpdates.add(element);
        }

        // Temporary workaround for dead lock, will be replaced by a new (ShaderMod compatible) mechanism later
        //noinspection StatementWithEmptyBody
        while (renderDispatcher.runChunkUploads(0)) {}

        workerJailingQueue.jail(workerThreads);
        renderDispatcher.queueChunkUpdates = queueChunkUpdates;

        try {
            Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            hookField.set(hooked, this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void updateChunks() {
        while (renderDispatcher.runChunkUploads(0)) {
            hooked.displayListEntitiesDirty = true;
        }

        while (!renderDispatcher.queueChunkUpdates.isEmpty()) {
            try {
                renderWorker.processTask(renderDispatcher.queueChunkUpdates.poll());
            } catch (InterruptedException ignored) { }
        }

        Iterator<RenderChunk> iterator = hooked.chunksToUpdate.iterator();
        while (iterator.hasNext()) {
            RenderChunk renderchunk = iterator.next();

            renderDispatcher.updateChunkNow(renderchunk);

            renderchunk.setNeedsUpdate(false);
            iterator.remove();
        }
    }

    public void uninstall() {
        workerJailingQueue.freeAll();

        try {
            Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            hookField.set(hooked, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public int nextFrameId() {
        return frame++;
    }

    /**
     * Custom ChunkRenderWorker class providing access to the protected processTask method
     */
    private static class CustomChunkRenderWorker extends ChunkRenderWorker {
        public CustomChunkRenderWorker(ChunkRenderDispatcher p_i46202_1_, RegionRenderCacheBuilder p_i46202_2_) {
            super(p_i46202_1_, p_i46202_2_);
        }

        @Override
        protected void processTask(ChunkCompileTaskGenerator p_178474_1_) throws InterruptedException {
            super.processTask(p_178474_1_);
        }
    }
}
