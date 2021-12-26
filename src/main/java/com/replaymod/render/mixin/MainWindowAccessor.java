package com.replaymod.render.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Window.class)
public interface MainWindowAccessor {
    @Accessor
    int getFramebufferWidth();
    @Accessor
    void setFramebufferWidth(int value);
    @Accessor
    int getFramebufferHeight();
    @Accessor
    void setFramebufferHeight(int value);
    // FIXME preprocessor should be able to infer this mapping
    // FIXME preprocessor should be able to remap this one when the mapping is given manually
    //#if MC>=11500
    @Invoker
    //#else
    //$$ @Invoker("method_4483")
    //#endif
    void invokeUpdateFramebufferSize();
}
