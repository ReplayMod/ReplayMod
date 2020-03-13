package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class Mixin_SkipBlockOutlinesDuringRender {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"), cancellable = true)
    private void replayModRender_drawSelectionBox(CallbackInfo ci) {
        if (((EntityRendererHandler.IEntityRenderer) client.gameRenderer).replayModRender_getHandler() != null) {
            ci.cancel();
        }
    }
}
