//#if MC>=11300
package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.render.Camera;
//#endif

@Mixin(BackgroundRenderer.class)
public abstract class MixinFogRenderer {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private void replayModRender_onSetupFog(
            //#if MC>=11400
            Camera camera,
            int something,
            //#else
            //$$ int fogDistanceFlag,
            //$$ float partialTicks,
            //#endif
            CallbackInfo ci
    ) {
        EntityRendererHandler handler =
                ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null) return;
        if (handler.getSettings().getChromaKeyingColor() != null) {
            ci.cancel();
        }
    }
}
//#endif
