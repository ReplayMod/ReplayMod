package com.replaymod.core.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//#if MC>=11400
//$$ import net.minecraft.client.gui.widget.AbstractButtonWidget;
//#else
import net.minecraft.client.gui.GuiButton;
//#endif

//#if MC>=11300
import net.minecraft.client.gui.IGuiEventListener;
//#endif

@Mixin(GuiScreen.class)
public interface GuiScreenAccessor {
    @Accessor
    //#if MC>=11400
    //$$ List<AbstractButtonWidget> getButtons();
    //#else
    List<GuiButton> getButtons();
    //#endif

    //#if MC>=11300
    @Accessor
    List<IGuiEventListener> getChildren();
    //#endif
}
