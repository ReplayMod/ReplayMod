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
            //#if MC>=11400
            method = "renderWorld",
            //#else
            //$$ method = "renderWorldPass",
            //#endif
            //#if MC>=11400
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"),
            //#else
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V", ordinal = 1),
            //#endif
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
