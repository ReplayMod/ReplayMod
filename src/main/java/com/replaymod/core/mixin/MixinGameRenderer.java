//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.events.PostRenderWorldCallback;
import com.replaymod.core.events.PreRenderHandCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PostRenderHudCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PostRenderScreenCallback;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;draw(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void postRenderOverlay(float partialTicks, long nanoTime, boolean renderWorld, CallbackInfo ci) {
        PostRenderHudCallback.EVENT.invoker().postRenderHud(partialTicks);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Screen;render(IIF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void postRenderScreen(float partialTicks, long nanoTime, boolean renderWorld, CallbackInfo ci) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(partialTicks);
    }

    @Inject(
            method = "renderCenter",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z"
            )
    )
    private void postRenderWorld(float partialTicks, long nanoTime, CallbackInfo ci) {
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(Camera camera, float partialTicks, CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }
}
//#endif
