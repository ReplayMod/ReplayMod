package com.replaymod.render.hooks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public interface FramebufferDelegateHolder {
    void setFramebufferDelegate(Framebuffer framebuffer);

    static FramebufferDelegateHolder get(MinecraftClient mc) {
        //#if MC >= 26.2
        //$$ return (FramebufferDelegateHolder) mc.gameRenderer;
        //#else
        return (FramebufferDelegateHolder) mc;
        //#endif
    }
}
