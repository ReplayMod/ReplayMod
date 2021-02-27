package com.replaymod.recording.mixin;

import de.johni0702.minecraft.gui.container.VanillaGuiScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "handleInput", at = @At("HEAD"), cancellable = true)
    private void handleJGuiInput(CallbackInfo ci) {
        ci.cancel();
        if (Mouse.isCreated()) {
            while (Mouse.next()) {
                if (MinecraftForge.EVENT_BUS.post(new VanillaGuiScreen.MouseInputEvent())) {
                    continue;
                }
                handleMouseInput();
            }
        }

        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                if (MinecraftForge.EVENT_BUS.post(new VanillaGuiScreen.KeyboardInputEvent())) {
                    continue;
                }
                handleKeyboardInput();
            }
        }
    }

    @Shadow public abstract void handleMouseInput();

    @Shadow public abstract void handleKeyboardInput();
}
