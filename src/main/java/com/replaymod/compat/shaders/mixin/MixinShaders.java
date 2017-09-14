package com.replaymod.compat.shaders.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "shadersmod/client/Shaders", remap = false)
public abstract class MixinShaders {

    @Shadow static Minecraft mc;
    @Shadow static long systemTime;
    @Shadow static long lastSystemTime;
    @Shadow static long diffSystemTime;
    @Shadow static int frameCounter;
    @Shadow static float frameTime;
    @Shadow static float frameTimeCounter;

    @Redirect(method = "beginRender", at = @At(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"))
    private static long replayModCompat_currentTimeMillis() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            systemTime = replayHandler.getReplaySender().currentTimeStamp();

            // We need to manipulate all the other previous-frame-time-based variables too

            lastSystemTime = 0; // Draw all frames as if they were the first ones
            // diffSystemTime will be set to 0 by Shaders
            // frameTime will be set to 0 by Shaders
            frameTimeCounter = systemTime / 1000f; // will be %= 3600f by Shaders

            // Set frameCounter only if rendering is in progress
            EntityRendererHandler entityRendererHandler =
                    ((EntityRendererHandler.IEntityRenderer) mc.entityRenderer).replayModRender_getHandler();
            if (entityRendererHandler != null) {
                frameCounter = entityRendererHandler.getRenderInfo().getFramesDone();
            }

            return systemTime;
        }
        return System.currentTimeMillis();
    }

}
