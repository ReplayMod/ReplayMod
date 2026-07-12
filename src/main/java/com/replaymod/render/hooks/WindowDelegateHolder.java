package com.replaymod.render.hooks;

import com.replaymod.render.gui.progress.VirtualWindow;
import net.minecraft.client.MinecraftClient;

public interface WindowDelegateHolder {
    void setWindowDelegate(VirtualWindow window);

    static WindowDelegateHolder get(MinecraftClient mc) {
        return (WindowDelegateHolder) mc;
    }
}
