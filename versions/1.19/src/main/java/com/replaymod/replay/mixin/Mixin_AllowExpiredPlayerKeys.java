package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.network.encryption.PlayerPublicKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerPublicKey.PublicKeyData.class)
public abstract class Mixin_AllowExpiredPlayerKeys {
    //#if MC>=11902
    //$$ @Inject(method = { "isExpired()Z", "isExpired(Ljava/time/Duration;)Z" }, at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "isExpired", at = @At("HEAD"), cancellable = true)
    //#endif
    private void neverExpireWhenInReplay(CallbackInfoReturnable<Boolean> ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(false);
        }
    }
}
