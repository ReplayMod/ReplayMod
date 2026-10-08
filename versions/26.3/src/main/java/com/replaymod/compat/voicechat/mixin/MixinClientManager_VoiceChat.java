package com.replaymod.compat.voicechat.mixin;

import com.replaymod.replay.ReplayModReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recorded voice authentication packets belong to the original server connection. */
@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.voice.client.ClientManager", remap = false)
public abstract class MixinClientManager_VoiceChat {
    @Inject(method = "authenticate(Lde/maxhenkel/voicechat/net/SecretPacket;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaymod$skipRecordedAuthentication(@Coerce Object packet, CallbackInfo ci) {
        if (ReplayModReplay.instance != null && ReplayModReplay.instance.getReplayHandler() != null) {
            ci.cancel();
        }
    }
}
