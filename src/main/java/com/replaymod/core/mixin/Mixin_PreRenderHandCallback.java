//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.events.PreRenderHandCallback;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12005
//$$ import org.joml.Matrix4f;
//#else
//#endif

@Mixin(GameRenderer.class)
public class Mixin_PreRenderHandCallback {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(
            //#if MC>=11500 && MC<12005
            MatrixStack matrixStack,
            //#endif
            Camera camera,
            float partialTicks,
            //#if MC>=12005
            //$$ Matrix4f matrixStack,
            //#endif
            CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }
}
//#endif
