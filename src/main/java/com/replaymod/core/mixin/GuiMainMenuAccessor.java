package com.replaymod.core.mixin;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiMainMenu.class)
public interface GuiMainMenuAccessor {
    //#if MC>=10904
    @Accessor
    GuiScreen getRealmsNotification();
    @Accessor
    void setRealmsNotification(GuiScreen value);
    //#endif
}
