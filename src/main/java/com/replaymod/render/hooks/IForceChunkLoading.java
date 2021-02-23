package com.replaymod.render.hooks;

import net.minecraft.client.render.WorldRenderer;

public interface IForceChunkLoading {
    void replayModRender_setHook(ChunkLoadingRenderGlobal hook);

    static IForceChunkLoading from(WorldRenderer worldRenderer) {
        return (IForceChunkLoading) worldRenderer;
    }
}
