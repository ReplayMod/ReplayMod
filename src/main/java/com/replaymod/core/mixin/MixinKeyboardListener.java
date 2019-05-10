//#if MC>=11300
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

    //#if MC>=11400
    /* FIXME we currently don't use these but they also don't work since their target is wrapped in a lambda,
        see MixinMouseListener for working examples
    @Redirect(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ParentElement;keyPressed(III)Z")
    )
    private boolean keyPressed(ParentElement element, int keyCode, int modifiers, int scanCode) {
        if (KeyboardCallback.EVENT.invoker().keyPressed(keyCode, modifiers, scanCode)) {
            return true;
        } else {
            return element.keyPressed(keyCode, modifiers, scanCode);
        }
    }

    @Redirect(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ParentElement;keyReleased(III)Z")
    )
    private boolean keyReleased(ParentElement element, int keyCode, int modifiers, int scanCode) {
        if (KeyboardCallback.EVENT.invoker().keyReleased(keyCode, modifiers, scanCode)) {
            return true;
        } else {
            return element.keyReleased(keyCode, modifiers, scanCode);
        }
    }

    @Redirect(
            method = "onKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ParentElement;charTyped(CI)Z")
    )
    private boolean charTyped(ParentElement element, char keyChar, int modifiers) {
        if (KeyboardCallback.EVENT.invoker().charTyped(keyChar, modifiers)) {
            return true;
        } else {
            return element.charTyped(keyChar, modifiers);
        }
    }
    */
    //#endif
}
//#endif
