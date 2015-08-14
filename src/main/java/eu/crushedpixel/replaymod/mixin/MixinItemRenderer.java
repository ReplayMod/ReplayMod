package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Inject(method = "renderOverlays", at = @At("HEAD"), cancellable = true)
    public void shouldRenderOverlay(float partialTick, CallbackInfo ci) {
        if (ReplayHandler.getCameraEntity() != null) {
            ci.cancel();
        }
    }
}
