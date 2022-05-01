package com.replaymod.render.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class Mixin_ChromaKeyDisableFog {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private
    //#if MC>=11500
    static
    //#endif
    void replayModRender_onSetupFog(CallbackInfo ci) {
        EntityRendererHandler handler =
                ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null) return;
        if (handler.getSettings().getChromaKeyingColor() != null) {
            // Starting with 1.15, fog is no longer enabled in this method but is instead managed by the RenderLayer
            // system (and with 1.17, they are enabled permanently / depend only on the shader). Therefore, cancelling
            // this method is no longer sufficient, and we additionally also need to set the start value to get rid of
            // fog (this doesn't hurt on 1.14 either).
            // Note: This only becomes noticeable with Sodium because Vanilla would already set the start to max for
            //       unrelated reasons. But Sodium does some math which gives wrong results if end isn't greater than
            //       start, as would be the case in these cases. Sodium doing math is also the reason we don't set start
            //       equal to end (that'll result in undefined behavior because it sticks those into a smoothstep on old
            //       versions), and we don't set it to MAX_VALUE because that also gives wrong results.
            GlStateManager.fogStart(1E10F);
            GlStateManager.fogEnd(2E10F);
            ci.cancel();
        }
    }
}
