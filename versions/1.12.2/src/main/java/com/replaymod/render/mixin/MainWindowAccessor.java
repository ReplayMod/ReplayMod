package com.replaymod.render.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MainWindowAccessor {
    @Accessor("displayWidth")
    int getFramebufferWidth();
    @Accessor("displayWidth")
    void setFramebufferWidth(int value);
    @Accessor("displayHeight")
    int getFramebufferHeight();
    @Accessor("displayHeight")
    void setFramebufferHeight(int value);
}
