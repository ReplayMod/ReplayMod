package eu.crushedpixel.replaymod.mixin;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.gui.GuiSpectator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSpectator.class)
public abstract class MixinGuiSpectator {
    @Inject(method = "func_175260_a", at = @At("HEAD"), cancellable = true)
    public void isInReplay(int i, CallbackInfo ci) {
        if (ReplayHandler.isInReplay()) {
            ci.cancel();
        }
    }
}
