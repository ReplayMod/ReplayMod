// FIXME 1.15
//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ChunkExporter;
import net.minecraft.client.render.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.render.chunk.ChunkRenderTask;
//#else
//$$ import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
//#endif

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {

    @Inject(method = "runTask", at = @At("RETURN"))
    //#if MC>=11400
    public void afterChunkUpdate(ChunkRenderTask task, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            if (task.getStage() == ChunkRenderTask.Stage.DONE
                    && task.getMode() == ChunkRenderTask.Mode.REBUILD_CHUNK) {
                blendState.get(ChunkExporter.class).addChunkUpdate(task.getChunkRenderer(), task.getRenderData());
            }
        }
    }
    //#else
    //$$ public void afterChunkUpdate(ChunkCompileTaskGenerator task, CallbackInfo ci) {
    //$$     BlendState blendState = BlendState.getState();
    //$$     if (blendState != null) {
    //$$         if (task.getStatus() == ChunkCompileTaskGenerator.Status.DONE
    //$$                 && task.getType() == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK) {
    //$$             blendState.get(ChunkExporter.class).addChunkUpdate(task.getRenderChunk(), task.getCompiledChunk());
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
//#endif
