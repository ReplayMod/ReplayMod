package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class Mixin_PreserveDepthDuringHandRendering {
    @ModifyArg(
            // FIXME preprocessor bug: 1.8.9 uses method with `(FJ)V` when just name would be enough
            //#if MC>=10809
            method = "renderWorld",
            //#else
            //$$ method = "updateCameraAndRender(F)V",
            //#endif
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"),
            index = 0
    )
    private int replayModRender_skipClearWhenRecordingDepth(int mask) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this).replayModRender_getHandler();
        if (handler != null && handler.getSettings().isDepthMap()) {
            mask = mask & ~GL11.GL_DEPTH_BUFFER_BIT;
        }
        return mask;
    }
}
