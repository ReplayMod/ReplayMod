package com.replaymod.core.mixin;

import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import de.johni0702.minecraft.gui.function.KeyInput;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class MixinKeyboardListener {
    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private static void beforeKeyBindingTick(CallbackInfo ci) {
        int keyCode = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        int action = Keyboard.getEventKeyState() ? KeyEventCallback.ACTION_PRESS : KeyEventCallback.ACTION_RELEASE;
        if (KeyEventCallback.EVENT.invoker().onKeyEvent(new KeyInput(keyCode), action)) {
            ci.cancel();
        }
    }

    @Inject(method = "onTick", at = @At("RETURN"))
    private static void afterKeyBindingTick(CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }
}
