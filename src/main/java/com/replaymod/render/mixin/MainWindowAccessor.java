//#if MC>=11400
package com.replaymod.render.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface MainWindowAccessor {
    @Accessor
    void setFramebufferWidth(int value);
    @Accessor
    void setFramebufferHeight(int value);
}
//#endif
