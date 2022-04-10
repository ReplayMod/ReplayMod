package com.replaymod.render.gui.progress;

import com.replaymod.render.mixin.MainWindowAccessor;
import de.johni0702.minecraft.gui.function.Closeable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;

//#if MC>=11700
//$$ import net.minecraft.client.gl.WindowFramebuffer;
//#endif

public class VirtualWindow implements Closeable {
    private final MinecraftClient mc;
    private final Window window;
    private final MainWindowAccessor acc;

    private final Framebuffer guiFramebuffer;
    private int framebufferWidth, framebufferHeight;

    private int gameWidth, gameHeight;


    public VirtualWindow(MinecraftClient mc) {
        this.mc = mc;
        this.window = mc.getWindow();
        this.acc = (MainWindowAccessor) (Object) this.window;

        updateFramebufferSize();

        //#if MC>=11700
        //$$ guiFramebuffer = new WindowFramebuffer(framebufferWidth, framebufferHeight);
        //#else
        guiFramebuffer = new Framebuffer(framebufferWidth, framebufferHeight, true
                //#if MC>=11400
                , false
                //#endif
        );
        //#endif
    }

    @Override
    public void close() {
        guiFramebuffer.delete();
    }

    public void bind() {
        gameWidth = acc.getFramebufferWidth();
        gameHeight = acc.getFramebufferHeight();
        acc.setFramebufferWidth(framebufferWidth);
        acc.setFramebufferHeight(framebufferHeight);
    }

    public void unbind() {
        acc.setFramebufferWidth(gameWidth);
        acc.setFramebufferHeight(gameHeight);
    }

    public void beginWrite() {
        guiFramebuffer.beginWrite(true);
    }

    public void endWrite() {
        guiFramebuffer.endWrite();
    }

    public void flip() {
        guiFramebuffer.draw(framebufferWidth, framebufferHeight);

        //#if MC>=11500
        window.swapBuffers();
        //#else
        //#if MC>=11400
        //$$ window.setFullscreen(false);
        //#else
        //#if MC>=10800
        //$$ mc.updateDisplay();
        //#else
        //$$ mc.resetSize();
        //#endif
        //#endif
        //#endif
    }

    public void updateSize() {
        // Resize the GUI framebuffer if the display size changed
        if (framebufferSizeChanged()) {
            updateFramebufferSize();
            //#if MC>=11400
            guiFramebuffer.resize(framebufferWidth, framebufferHeight
                    //#if MC>=11400
                    , false
                    //#endif
            );
            //#else
            //$$ guiFramebuffer.createBindFramebuffer(framebufferWidth, framebufferHeight);
            //#endif
        }
    }

    private boolean framebufferSizeChanged() {
        int realWidth = mc.getWindow().getFramebufferWidth();
        int realHeight = mc.getWindow().getFramebufferHeight();
        if (realWidth == 0 || realHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return false;
        }
        return framebufferWidth != realWidth || framebufferHeight != realHeight;
    }

    private void updateFramebufferSize() {
        framebufferWidth = mc.getWindow().getFramebufferWidth();
        framebufferHeight = mc.getWindow().getFramebufferHeight();
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }
}
