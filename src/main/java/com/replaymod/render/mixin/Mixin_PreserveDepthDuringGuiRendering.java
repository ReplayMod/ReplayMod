package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//#if MC>=11500
@Mixin(GameRenderer.class)
//#elseif MC>=11400
//$$ @Mixin(net.minecraft.client.util.Window.class)
//#else
//$$ @Mixin(EntityRenderer.class)
//#endif
public abstract class Mixin_PreserveDepthDuringGuiRendering {
    @ModifyArg(
            //#if MC>=11500
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"), index = 0
            //#elseif MC>=11400
            //$$ method = "method_4493",
            //$$ at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(IZ)V"), index = 0
            //#else
            //$$ method = "setupOverlayRendering",
            //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V"), index = 0
            //#endif
    )
    private int replayModRender_skipClearWhenRecordingDepth(int mask) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MinecraftClient.getInstance().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.getSettings().isDepthMap()) {
            mask = mask & ~GL11.GL_DEPTH_BUFFER_BIT;
        }
        return mask;
    }
}
