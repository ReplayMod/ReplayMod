//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.events.PreRenderHandCallback;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class Mixin_PreRenderHandCallback {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }
}
//#endif
