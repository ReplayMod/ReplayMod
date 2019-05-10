package com.replaymod.core.mixin;

import net.minecraft.client.gui.MainMenuScreen;
import net.minecraft.client.gui.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MainMenuScreen.class)
public interface GuiMainMenuAccessor {
    //#if MC>=10904
    @Accessor("realmsNotificationGui")
    Screen getRealmsNotification();
    @Accessor("realmsNotificationGui")
    void setRealmsNotification(Screen value);
    //#endif
}
