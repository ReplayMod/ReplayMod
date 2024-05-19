//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.events.PostRenderWorldCallback;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11500
import com.llamalad7.mixinextras.sugar.Local;
//#endif

@Mixin(WorldRenderer.class)
public class Mixin_PostRenderWorldCalback {
    @Inject(method = "render", at = @At("RETURN"))
    //#if MC>=11500
    private void postRenderWorld(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrixStack) {
    //#else
    //$$ private void postRenderWorld(CallbackInfo ci) {
    //$$     MatrixStack matrixStack = new MatrixStack();
    //#endif
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(matrixStack);
    }
}
//#endif
