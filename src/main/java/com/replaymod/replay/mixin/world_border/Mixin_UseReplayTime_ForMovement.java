package com.replaymod.replay.mixin.world_border;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Normally Minecraft's world border movement is based off real time;
 * this redirect ensures that it is synced with the time in the Replay instead.
 */
//#if MC>=11400
// FIXME: preprocessor should be able to remap between fabric and forge
//#if FABRIC
@Mixin(targets = "net.minecraft.world.border.WorldBorder.MovingArea")
//#else
//$$ @Mixin(targets = "net.minecraft.world.border.WorldBorder.MovingBorderInfo")
//#endif
//#else
//$$ @Mixin(net.minecraft.world.border.WorldBorder.class)
//#endif
public class Mixin_UseReplayTime_ForMovement {

    //#if MC>=11400
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    //#else
    //$$ @Redirect(method = "*", at = @At(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"))
    //#endif
    private long getWorldBorderTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return MCVer.milliTime();
    }
}
