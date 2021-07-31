package com.replaymod.core.versions;

import com.replaymod.render.mixin.MainWindowAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.Display;

public class Window implements MainWindowAccessor {

    private final Minecraft mc;
    private ScaledResolution scaledResolution;

    public Window(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public int getFramebufferWidth() {
        return mc.displayWidth;
    }

    @Override
    public void setFramebufferWidth(int value) {
        mc.displayWidth = value;
    }

    @Override
    public int getFramebufferHeight() {
        return mc.displayHeight;
    }

    @Override
    public void setFramebufferHeight(int value) {
        mc.displayHeight = value;
    }

    public long getHandle() {
        return 0;
    }

    public int getWidth() {
        return Display.getWidth();
    }

    public int getHeight() {
        return Display.getHeight();
    }

    private ScaledResolution scaledResolution() {
        ScaledResolution scaledResolution = this.scaledResolution;
        if (scaledResolution == null) {
            //#if MC>=10809
            scaledResolution = new ScaledResolution(mc);
            //#else
            //$$ scaledResolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            //#endif
            this.scaledResolution = scaledResolution;
        }
        return scaledResolution;
    }

    public int getScaledWidth() {
        return scaledResolution().getScaledWidth();
    }

    public int getScaledHeight() {
        return scaledResolution().getScaledHeight();
    }
}
