package com.replaymod.compat.shaders.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(EntityRenderer.class)
public abstract class MixinShaderEntityRenderer {

    @Shadow
    public Minecraft mc;

    private static Field replayModCompat_frameTimeCounterField = null;

    static {
        replayModCompat_initFrameTimeCounterField();
    }

    private static void replayModCompat_initFrameTimeCounterField() {
        try {
            replayModCompat_frameTimeCounterField = Class.forName("shadersmod.client.Shaders")
                    .getDeclaredField("frameTimeCounter");
            replayModCompat_frameTimeCounterField.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
            // no shaders mod installed
        } catch (NoSuchFieldException e) {
            // the field wasn't found. Has it been renamed?
            e.printStackTrace();
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableDepth()V"))
    private void replayModCompat_updateShaderFrameTimeCounter(CallbackInfo ignore) {
        if (replayModCompat_frameTimeCounterField != null) {
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            float timestamp = replayHandler.getReplaySender().currentTimeStamp() / 1000f % 3600f;
            try {
                replayModCompat_frameTimeCounterField.set(null, timestamp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
