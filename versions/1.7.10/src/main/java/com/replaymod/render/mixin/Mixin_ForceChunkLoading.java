package com.replaymod.render.mixin;

import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderGlobal.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    @ModifyVariable(method = "updateRenderers", at = @At("HEAD"), argsOnly = true)
    private boolean replayModRender_updateAllChunks(boolean renderAllChunks) {
        if (replayModRender_hook != null) {
            renderAllChunks = true;
        }
        return renderAllChunks;
    }
}

