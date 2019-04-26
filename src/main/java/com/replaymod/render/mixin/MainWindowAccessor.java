//#if MC>=11300
package com.replaymod.render.mixin;

import net.minecraft.client.MainWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MainWindow.class)
public interface MainWindowAccessor {
    @Accessor
    void setFramebufferWidth(int value);
    @Accessor
    void setFramebufferHeight(int value);
}
//#endif
