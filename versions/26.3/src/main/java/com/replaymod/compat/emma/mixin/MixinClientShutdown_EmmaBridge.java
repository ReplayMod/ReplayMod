package com.replaymod.compat.emma.mixin;

import com.replaymod.compat.emma.EmmaBridgeShutdownCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinClientShutdown_EmmaBridge {
    @Inject(method = "close", at = @At("RETURN"))
    private void replaymod$stopEmmaWorkers(CallbackInfo ci) {
        EmmaBridgeShutdownCompat.shutdown();
    }
}
