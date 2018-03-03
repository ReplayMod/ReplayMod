package com.replaymod.render.blend.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ChunkExporter;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {

    @Inject(method = "processTask", at = @At("RETURN"))
    public void afterChunkUpdate(ChunkCompileTaskGenerator task, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            if (task.getStatus() == ChunkCompileTaskGenerator.Status.DONE
                    && task.getType() == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK) {
                for (EnumWorldBlockLayer layer : EnumWorldBlockLayer.values()) {
                    if (task.getCompiledChunk().isLayerStarted(layer)) {
                        blendState.get(ChunkExporter.class).addChunkUpdate(task.getRenderChunk(), layer);
                    }
                }
            }
        }
    }
}
