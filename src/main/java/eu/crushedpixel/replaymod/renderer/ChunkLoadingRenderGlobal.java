package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.utils.JailingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

public class ChunkLoadingRenderGlobal extends RenderGlobal {
    public static void install(Minecraft mc) {
        RenderGlobal org = mc.renderGlobal;
        if (org instanceof ChunkLoadingRenderGlobal) {
            ((ChunkLoadingRenderGlobal) org).uninstall();
        }
        mc.renderGlobal = new ChunkLoadingRenderGlobal(mc);
    }

    private final RenderGlobal originalRenderGlobal;
    private final JailingQueue<?> workerJailingQueue;
    private final CustomChunkRenderWorker renderWorker;
    private int frame;

    @SuppressWarnings("unchecked")
    private ChunkLoadingRenderGlobal(Minecraft mc) {
        super(mc);
        this.originalRenderGlobal = mc.renderGlobal;
        this.renderWorker = new CustomChunkRenderWorker(renderDispatcher, new RegionRenderCacheBuilder());
        if (mc.theWorld != null) {
            setWorldAndLoadRenderers(mc.theWorld);
        }

        int workerThreads = renderDispatcher.listThreadedWorkers.size();
        BlockingQueue<Object> queueChunkUpdates = renderDispatcher.queueChunkUpdates;
        workerJailingQueue = new JailingQueue<Object>(queueChunkUpdates);
        renderDispatcher.queueChunkUpdates = workerJailingQueue;
        ChunkCompileTaskGenerator element = new ChunkCompileTaskGenerator(null, null);
        element.finish();
        for (int i = 0; i < workerThreads; i++) {
            queueChunkUpdates.add(element);
        }
        workerJailingQueue.jail(workerThreads);
        renderDispatcher.queueChunkUpdates = queueChunkUpdates;
    }

    @Override
    public void setupTerrain(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator) {
        // There has to be a better way to force minecraft into queueing all chunks at once
        // Someone should probably find and implement it!
        do {
            super.setupTerrain(viewEntity, partialTicks, camera, frame++, playerSpectator);
        } while (displayListEntitiesDirty);
    }

    @Override
    public boolean isPositionInRenderChunk(BlockPos p_174983_1_, RenderChunk p_174983_2_) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateChunks(long finishTimeNano) {
        while (renderDispatcher.runChunkUploads(0)) {
            displayListEntitiesDirty = true;
        }

        while (!renderDispatcher.queueChunkUpdates.isEmpty()) {
            try {
                renderWorker.processTask((ChunkCompileTaskGenerator) renderDispatcher.queueChunkUpdates.poll());
            } catch (InterruptedException ignored) { }
        }

        Iterator<RenderChunk> iterator = chunksToUpdate.iterator();
        while (iterator.hasNext()) {
            RenderChunk renderchunk = iterator.next();

            renderDispatcher.updateChunkNow(renderchunk);

            renderchunk.setNeedsUpdate(false);
            iterator.remove();
        }
    }

    public void uninstall() {
        workerJailingQueue.freeAll();
        Minecraft.getMinecraft().renderGlobal = originalRenderGlobal;
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
