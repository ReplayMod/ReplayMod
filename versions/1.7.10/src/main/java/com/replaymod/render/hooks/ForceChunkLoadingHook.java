package com.replaymod.render.hooks;

import net.minecraft.client.renderer.RenderGlobal;

public class ForceChunkLoadingHook {
    private final RenderGlobal hooked;

    public ForceChunkLoadingHook(RenderGlobal renderGlobal) {
        this.hooked = renderGlobal;

        IForceChunkLoading.from(renderGlobal).replayModRender_setHook(this);
    }

    public void uninstall() {
        IForceChunkLoading.from(hooked).replayModRender_setHook(null);
    }
}
