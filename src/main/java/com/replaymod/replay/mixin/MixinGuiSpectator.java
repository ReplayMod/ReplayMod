//#if MC>=10800
package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.gui.hud.SpectatorHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.*;

@Mixin(SpectatorHud.class)
public abstract class MixinGuiSpectator {
    //#if MC>=10904
    @Inject(method = "cycleSlot", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "func_175260_a", at = @At("HEAD"), cancellable = true)
    //#endif
    public void isInReplay(
            //#if MC>=11400
            double i,
            //#else
            //$$ int i,
            //#endif
            CallbackInfo ci
    ) {
        // Prevent spectator gui from opening while in a replay
        if (getMinecraft().player instanceof CameraEntity) {
            ci.cancel();
        }
    }
}
//#endif
