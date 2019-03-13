//#if MC>=11300
package com.replaymod.recording.mixin;

import com.replaymod.replay.InputReplayTimer;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHelper.class)
public abstract class MixinMouseHelper {
    //#ifdef DEV_ENV
    @Shadow
    private boolean mouseGrabbed;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void noGrab(CallbackInfo ci) {
        // Used to be provided by Forge for 1.12.2 and below
        if (Boolean.valueOf(System.getProperty("fml.noGrab", "false"))) {
            mouseGrabbed = true;
            ci.cancel();
        }
    }
    //#endif

    @Inject(method = "scrollCallback",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSpectator()Z"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void handleReplayModScroll(long _p0, double _p1, double _p2, CallbackInfo ci, double _l1, double yOffsetAccumulated) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) yOffsetAccumulated);
            ci.cancel();
        }
    }
}
//#endif
