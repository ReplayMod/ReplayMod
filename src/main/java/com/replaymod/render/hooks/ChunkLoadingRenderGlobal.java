package com.replaymod.render.hooks;

import net.minecraft.client.render.WorldRenderer;

public class ChunkLoadingRenderGlobal {

    private final WorldRenderer hooked;

    public ChunkLoadingRenderGlobal(WorldRenderer renderGlobal) {
        this.hooked = renderGlobal;

        IForceChunkLoading.from(renderGlobal).replayModRender_setHook(this);
    }

    public void uninstall() {
        IForceChunkLoading.from(hooked).replayModRender_setHook(null);
    }

    public interface IBlockOnChunkRebuilds {
        boolean uploadEverythingBlocking();
    }
}
