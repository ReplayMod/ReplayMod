//#if MC>=11600
package com.replaymod.replay.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TitleScreen.class)
public abstract class Mixin_MoveRealmsButton {
    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/MinecraftClient;II)V"),
            index = 2
    )
    private int adjustRealmsButton(int height) {
        return height - (24 - 10) * 4;
    }
}
//#endif
