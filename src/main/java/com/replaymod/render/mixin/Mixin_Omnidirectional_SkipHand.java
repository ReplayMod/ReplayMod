package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class Mixin_Omnidirectional_SkipHand implements EntityRendererHandler.IEntityRenderer {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void replayModRender_renderSpectatorHand(
            //#if MC>=11500
            MatrixStack matrixStack,
            //#endif
            //#if MC>=11400
            Camera camera,
            //#endif
            float partialTicks,
            //#if MC<11400
            //$$ int renderPass,
            //#endif
            CallbackInfo ci
    ) {
        EntityRendererHandler handler = replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            // No spectator hands during 360° view, we wouldn't even know where to put it
            ci.cancel();
        }
    }
}
