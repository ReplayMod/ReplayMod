package com.replaymod.core.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//#if MC>=11400
import net.minecraft.client.gui.widget.AbstractButtonWidget;
//#else
//$$ import net.minecraft.client.gui.GuiButton;
//#endif

//#if MC>=11400
import net.minecraft.client.gui.Element;
//#endif

@Mixin(Screen.class)
public interface GuiScreenAccessor {
    //#if MC>=11400
    @Accessor
    List<AbstractButtonWidget> getButtons();
    //#else
    //$$ @Accessor("buttonList")
    //$$ List<GuiButton> getButtons();
    //#endif

    //#if MC>=11400
    @Accessor
    List<Element> getChildren();
    //#endif
}
