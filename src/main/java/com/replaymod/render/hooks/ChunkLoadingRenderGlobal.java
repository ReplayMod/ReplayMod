//#if MC>=10800
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

//#if MC>=10904
import java.util.concurrent.PriorityBlockingQueue;
//#else
//$$ import java.util.concurrent.BlockingQueue;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class ChunkLoadingRenderGlobal {

    private final RenderGlobal hooked;
    private ChunkRenderDispatcher renderDispatcher;
    private JailingQueue<ChunkCompileTaskGenerator> workerJailingQueue;
    private CustomChunkRenderWorker renderWorker;
    private int frame;

    @SuppressWarnings("unchecked")
    public ChunkLoadingRenderGlobal(RenderGlobal renderGlobal) {
        this.hooked = renderGlobal;

        setup(renderGlobal.renderDispatcher);
    }

    public void updateRenderDispatcher(ChunkRenderDispatcher renderDispatcher) {
        if (this.renderDispatcher != null) {
            workerJailingQueue.freeAll();
            this.renderDispatcher = null;
        }
        if (renderDispatcher != null) {
            setup(renderDispatcher);
        }
    }

    private void setup(ChunkRenderDispatcher renderDispatcher) {
        this.renderDispatcher = renderDispatcher;
        this.renderWorker = new CustomChunkRenderWorker(renderDispatcher, new RegionRenderCacheBuilder());

        int workerThreads = renderDispatcher.listThreadedWorkers.size();
        //#if MC>=10904
        PriorityBlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = renderDispatcher.queueChunkUpdates;
        //#else
        //$$ BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = renderDispatcher.queueChunkUpdates;
        //#endif
        workerJailingQueue = new JailingQueue<>(queueChunkUpdates);
        renderDispatcher.queueChunkUpdates = workerJailingQueue;
        //#if MC>=10904
        ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null, 0);
        //#else
        //$$ ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null);
        //#endif
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
                renderWorker.processTask(
                        //#if MC<10904
                        //$$ (ChunkCompileTaskGenerator)
                        //#endif
                                renderDispatcher.queueChunkUpdates.poll());
            } catch (InterruptedException ignored) { }
        }

        //#if MC<10904
        //$$ @SuppressWarnings("unchecked")
        //#endif
        Iterator<RenderChunk> iterator = hooked.chunksToUpdate.iterator();
        while (iterator.hasNext()) {
            RenderChunk renderchunk = iterator.next();

            renderDispatcher.updateChunkNow(renderchunk);

            //#if MC>=10904
            renderchunk.clearNeedsUpdate();
            //#else
            //$$ renderchunk.setNeedsUpdate(false);
            //#endif
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
//#endif
