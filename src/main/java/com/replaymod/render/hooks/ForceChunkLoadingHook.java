package com.replaymod.render.hooks;

import net.minecraft.client.render.WorldRenderer;

public class ForceChunkLoadingHook {

    private final WorldRenderer hooked;

    public ForceChunkLoadingHook(WorldRenderer renderGlobal) {
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
