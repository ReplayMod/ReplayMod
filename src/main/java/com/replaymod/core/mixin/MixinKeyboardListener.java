package com.replaymod.core.mixin;

import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
import net.minecraft.client.Keyboard;
//#else
//$$ import net.minecraft.client.settings.KeyBinding;
//$$ import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=11400
@Mixin(Keyboard.class)
public class MixinKeyboardListener {
    private static final String ON_KEY_PRESSED = "Lnet/minecraft/client/options/KeyBinding;onKeyPressed(Lnet/minecraft/client/util/InputUtil$Key;)V";

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = ON_KEY_PRESSED), cancellable = true)
    private void beforeKeyBindingTick(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (KeyEventCallback.EVENT.invoker().onKeyEvent(key, scanCode, action, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = ON_KEY_PRESSED, shift = At.Shift.AFTER))
    private void afterKeyBindingTick(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }
}
//#else
//$$ @Mixin(KeyBinding.class)
//$$ public class MixinKeyboardListener {
//$$     @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
//$$     private static void beforeKeyBindingTick(CallbackInfo ci) {
//$$         int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
//$$         int action = Keyboard.getEventKeyState() ? KeyEventCallback.ACTION_PRESS : KeyEventCallback.ACTION_RELEASE;
//$$         if (KeyEventCallback.EVENT.invoker().onKeyEvent(keyCode, 0, action, 0)) {
//$$             ci.cancel();
//$$         }
//$$     }
//$$
//$$     @Inject(method = "onTick", at = @At("RETURN"))
//$$     private static void afterKeyBindingTick(CallbackInfo ci) {
//$$         KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
//$$     }
//$$ }
//#endif
