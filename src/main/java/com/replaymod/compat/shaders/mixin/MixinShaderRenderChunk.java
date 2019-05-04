//#if MC>=10800
package com.replaymod.compat.shaders.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunk.class)
public abstract class MixinShaderRenderChunk {

    private final Minecraft mc = Minecraft.getInstance();

    /**
     *  Changes the RenderChunk#isPlayerUpdate method that Optifine adds
     *  to always return true while rendering so no chunks are being added
     *  to a separate rendering queue
     */
    @Inject(method = "isPlayerUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void replayModCompat_disableIsPlayerUpdate(CallbackInfoReturnable<Boolean> ci) {
        // TODO: Update to 1.12 once optifine is available
        // TODO: We're on 1.13 now and haven't gotten any 1.12 complaints, so 1.12 is probably working but 1.13?
        if (((EntityRendererHandler.IEntityRenderer) mc.entityRenderer).replayModRender_getHandler() == null) return;
        ci.setReturnValue(true);
    }


}
//#endif
