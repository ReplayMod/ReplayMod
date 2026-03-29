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
    //#if MC >= 26.1
    //$$ private static final String REALMS_INIT = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;init(II)V";
    //#elseif MC>=11901
    //$$ private static final String REALMS_INIT = "Lnet/minecraft/client/realms/gui/screen/RealmsNotificationsScreen;init(Lnet/minecraft/client/MinecraftClient;II)V";
    //#else
    private static final String REALMS_INIT = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/MinecraftClient;II)V";
    //#endif

    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = REALMS_INIT),
            //#if MC >= 26.1
            //$$ index = 1
            //#else
            index = 2
            //#endif
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
