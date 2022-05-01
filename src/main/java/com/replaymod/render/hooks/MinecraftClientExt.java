package com.replaymod.render.hooks;

import com.replaymod.render.gui.progress.VirtualWindow;
import net.minecraft.client.MinecraftClient;

public interface MinecraftClientExt {
    void setWindowDelegate(VirtualWindow window);

    static MinecraftClientExt get(MinecraftClient mc) {
        return (MinecraftClientExt) mc;
    }
}
