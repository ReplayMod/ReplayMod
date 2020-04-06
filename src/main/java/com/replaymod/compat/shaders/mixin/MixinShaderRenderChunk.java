//#if MC>=10800
package com.replaymod.compat.shaders.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11500
@Mixin(net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk.class)
//#else
//$$ @Mixin(net.minecraft.client.render.chunk.ChunkRenderer.class)
//#endif
public abstract class MixinShaderRenderChunk {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     *  Changes the RenderChunk#isPlayerUpdate method that Optifine adds
     *  to always return true while rendering so no chunks are being added
     *  to a separate rendering queue
     */
    @Inject(method = "isPlayerUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void replayModCompat_disableIsPlayerUpdate(CallbackInfoReturnable<Boolean> ci) {
        if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null) return;
        ci.setReturnValue(true);
    }


}
//#endif
