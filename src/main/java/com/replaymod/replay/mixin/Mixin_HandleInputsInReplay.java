package com.replaymod.replay.mixin;

import com.replaymod.replay.InputReplayTimer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class Mixin_HandleInputsInReplay {
    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=scheduledExecutables"))
    private void updateInReplay(CallbackInfo ci) {
        InputReplayTimer.updateInReplay();
    }
}
