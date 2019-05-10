//#if MC>=10800
package com.replaymod.render.hooks;

import com.replaymod.render.mixin.ChunkRenderDispatcherAccessor;
import com.replaymod.render.mixin.WorldRendererAccessor;
import com.replaymod.render.utils.JailingQueue;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBatcher;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderWorker;
import net.minecraft.client.render.chunk.ChunkRenderer;

import java.lang.reflect.Field;
import java.util.Iterator;

//#if MC>=10904
import java.util.concurrent.PriorityBlockingQueue;
//#else
//$$ import java.util.concurrent.BlockingQueue;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class ChunkLoadingRenderGlobal {

    //#if MC>=11300
    private final WorldRenderer hooked;
    //#else
    //$$ private final RenderGlobal hooked;
    //#endif
    private ChunkBatcher renderDispatcher;
    //#if MC>=11300
    private JailingQueue<ChunkRenderTask> workerJailingQueue;
    //#else
    //$$ private JailingQueue<ChunkCompileTaskGenerator> workerJailingQueue;
    //#endif
    private CustomChunkRenderWorker renderWorker;
    private int frame;

    @SuppressWarnings("unchecked")
    public ChunkLoadingRenderGlobal(
            //#if MC>=11300
            WorldRenderer renderGlobal
            //#else
            //$$ RenderGlobal renderGlobal
            //#endif
    ) {
        this.hooked = renderGlobal;

        setup(((WorldRendererAccessor) renderGlobal).getRenderDispatcher());
    }

    public void updateRenderDispatcher(ChunkBatcher renderDispatcher) {
        if (this.renderDispatcher != null) {
            workerJailingQueue.freeAll();
            this.renderDispatcher = null;
        }
        if (renderDispatcher != null) {
            setup(renderDispatcher);
        }
    }

    private void setup(ChunkBatcher renderDispatcher) {
        this.renderDispatcher = renderDispatcher;
        this.renderWorker = new CustomChunkRenderWorker(renderDispatcher, new BlockLayeredBufferBuilder());
        ChunkRenderDispatcherAccessor renderDispatcherAcc = (ChunkRenderDispatcherAccessor) renderDispatcher;

        int workerThreads = renderDispatcherAcc.getListThreadedWorkers().size();
        //#if MC>=10904
        PriorityBlockingQueue<ChunkRenderTask> queueChunkUpdates = renderDispatcherAcc.getQueueChunkUpdates();
        //#else
        //$$ BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = renderDispatcherAcc.getQueueChunkUpdates();
        //#endif
        workerJailingQueue = new JailingQueue<>(queueChunkUpdates);
        renderDispatcherAcc.setQueueChunkUpdates(workerJailingQueue);
        //#if MC>=10904
        ChunkRenderTask element = new ChunkRenderTask(
                null,
                null,
                0
                //#if MC>=11400
                , null
                //#endif
        );
        //#else
        //$$ ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null);
        //#endif
        element.cancel();
        for (int i = 0; i < workerThreads; i++) {
            queueChunkUpdates.add(element);
        }

        // Temporary workaround for dead lock, will be replaced by a new (ShaderMod compatible) mechanism later
        //noinspection StatementWithEmptyBody
        while (renderDispatcher.method_3631(0)) {}

        workerJailingQueue.jail(workerThreads);
        renderDispatcherAcc.setQueueChunkUpdates(queueChunkUpdates);

        try {
            //#if MC>=11300
            Field hookField = WorldRenderer.class.getField("replayModRender_hook");
            //#else
            //$$ Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            //#endif
            hookField.set(hooked, this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void updateChunks() {
        while (renderDispatcher.method_3631(0)) {
            ((WorldRendererAccessor) hooked).setDisplayListEntitiesDirty(true);
        }

        //#if MC>=10904
        PriorityBlockingQueue<ChunkRenderTask> queueChunkUpdates;
        //#else
        //$$ BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates;
        //#endif
        queueChunkUpdates = ((ChunkRenderDispatcherAccessor) renderDispatcher).getQueueChunkUpdates();
        while (!queueChunkUpdates.isEmpty()) {
            try {
                renderWorker.runTask(queueChunkUpdates.poll());
            } catch (InterruptedException ignored) { }
        }

        Iterator<ChunkRenderer> iterator = ((WorldRendererAccessor) hooked).getChunksToUpdate().iterator();
        while (iterator.hasNext()) {
            ChunkRenderer renderchunk = iterator.next();

            renderDispatcher.method_3627(renderchunk);

            //#if MC>=10904
            renderchunk.method_3662();
            //#else
            //$$ renderchunk.setNeedsUpdate(false);
            //#endif
            iterator.remove();
        }
    }

    public void uninstall() {
        workerJailingQueue.freeAll();

        try {
            //#if MC>=11300
            Field hookField = WorldRenderer.class.getField("replayModRender_hook");
            //#else
            //$$ Field hookField = RenderGlobal.class.getField("replayModRender_hook");
            //#endif
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
        public CustomChunkRenderWorker(ChunkBatcher p_i46202_1_, BlockLayeredBufferBuilder p_i46202_2_) {
            super(p_i46202_1_, p_i46202_2_);
        }

        @Override
        protected void runTask(
                //#if MC>=11300
                ChunkRenderTask p_178474_1_
                //#else
                //$$ ChunkCompileTaskGenerator p_178474_1_
                //#endif
        ) throws InterruptedException {
            super.runTask(p_178474_1_);
        }
    }
}
//#endif
