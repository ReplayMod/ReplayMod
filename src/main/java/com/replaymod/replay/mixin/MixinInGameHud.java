//#if MC>=11400
package com.replaymod.replay.mixin;

import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Inject(method = "shouldRenderSpectatorCrosshair", at = @At("HEAD"), cancellable = true)
    private void shouldRenderSpectatorCrosshair(CallbackInfoReturnable<Boolean> ci) {
        TriState state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
        if (state != TriState.DEFAULT) {
            ci.setReturnValue(state.get());
        }
    }
}
//#endif
