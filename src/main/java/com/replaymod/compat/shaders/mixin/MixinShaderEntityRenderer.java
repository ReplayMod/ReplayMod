//#if MC>=10800
package com.replaymod.compat.shaders.mixin;

import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinShaderEntityRenderer {

    @Inject(method = "renderWorldPass", at = @At("HEAD"))
    private void replayModCompat_updateShaderFrameTimeCounter(CallbackInfo ignore) {
        if (ReplayModReplay.instance.getReplayHandler() == null) return;
        if (ShaderReflection.shaders_frameTimeCounter == null) return;

        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        float timestamp = replayHandler.getReplaySender().currentTimeStamp() / 1000f % 3600f;
        try {
            ShaderReflection.shaders_frameTimeCounter.set(null, timestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
//#endif
