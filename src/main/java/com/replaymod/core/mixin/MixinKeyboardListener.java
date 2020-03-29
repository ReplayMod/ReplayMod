//#if MC>=11400
package com.replaymod.core.mixin;

import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboardListener {
    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;onKeyPressed(Lnet/minecraft/client/util/InputUtil$KeyCode;)V", shift = At.Shift.AFTER))
    private void afterKeyBindingTick(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
        KeyEventCallback.EVENT.invoker().onKeyEvent(key, scanCode, action, modifiers);
    }
}
//#endif
