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

//#if MC >= 26.2
//$$ import net.minecraft.client.renderer.GameRenderer;
//#endif

@Mixin(WorldRenderer.class)
public abstract class Mixin_SkipBlockOutlinesDuringRender {
    //#if MC >= 26.2
    //$$ @Shadow @Final private GameRenderer gameRenderer;
    //#else
    @Shadow @Final private MinecraftClient client;
    //#endif

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void replayModRender_drawSelectionBox(CallbackInfo ci) {
        //#if MC >= 26.2
        //$$ EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.gameRenderer).replayModRender_getHandler();
        //#else
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.client.gameRenderer).replayModRender_getHandler();
        //#endif
        if (handler != null) {
            ci.cancel();
        }
    }
}
