package com.replaymod.render.hooks;

import com.replaymod.render.utils.FlawlessFrames;
import net.minecraft.client.render.WorldRenderer;

public class ForceChunkLoadingHook {

    private final WorldRenderer hooked;

    public ForceChunkLoadingHook(WorldRenderer renderGlobal) {
        this.hooked = renderGlobal;

        FlawlessFrames.setEnabled(true);
        IForceChunkLoading.from(renderGlobal).replayModRender_setHook(this);
    }

    public void uninstall() {
        IForceChunkLoading.from(hooked).replayModRender_setHook(null);
        FlawlessFrames.setEnabled(false);
    }

    public interface IBlockOnChunkRebuilds {
        boolean uploadEverythingBlocking();
    }
}
