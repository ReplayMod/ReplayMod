package com.replaymod.replay.mixin.world_border;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Normally Minecraft's world border texture animation is based off real time;
 * this redirect ensures that it is synced with the time in the Replay instead.
 */
@Mixin(WorldRenderer.class)
public class Mixin_UseReplayTime_ForTexture {
    //#if MC>=11400
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    //#else
    //$$ @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    //#endif
    private long getWorldBorderTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return MCVer.milliTime();
    }
}
