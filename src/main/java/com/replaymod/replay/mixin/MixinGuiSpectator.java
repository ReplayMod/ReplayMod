package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSpectator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSpectator.class)
public abstract class MixinGuiSpectator {
    @Shadow
    private Minecraft mc;

    @Inject(method = "onHotbarSelected", at = @At("HEAD"), cancellable = true)
    public void isInReplay(int i, CallbackInfo ci) {
        // Prevent spectator gui from opening while in a replay
        if (mc.thePlayer instanceof CameraEntity) {
            ci.cancel();
        }
    }
}
