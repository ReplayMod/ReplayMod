//#if MC>=11500
package com.replaymod.compat.shaders.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net/optifine/render/ChunkVisibility", remap = false)
public abstract class MixinChunkVisibility {
    @Shadow
    private static int counter;

    /**
     * OF doesn't properly reset the counter when exiting a world.
     * It'll only be reset when getMaxChunkY is called which only happens for
     * SP worlds (i.e. not in a replay or MP).
     * As a result, it may end up in a non-0 state, which will cause isFinished
     * to unconditionally return false, therefore unconditionally setting
     * needsTerrainUpdate to true on each call to WorldRenderer.setupTerrain,
     * therefore unnecessarily consuming resources and live-locking when
     * rendering the shader pass.
     */
    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private static void replayModCompat_fixImproperReset(CallbackInfo ci) {
        MixinChunkVisibility.counter = 0;
    }
}
//#endif
