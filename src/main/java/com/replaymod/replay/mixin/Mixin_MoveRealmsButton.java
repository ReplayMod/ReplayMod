//#if MC>=11600
package com.replaymod.replay.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.Setting;
import com.replaymod.replay.handler.GuiHandler.MainMenuButtonPosition;
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
        String setting = ReplayMod.instance.getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON);
        if (MainMenuButtonPosition.valueOf(setting) == MainMenuButtonPosition.BIG) {
            height -= 24 * 4;
        }
        return height;
    }
}
//#endif
