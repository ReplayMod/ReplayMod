package com.replaymod.render.gui.progress;

import com.replaymod.render.hooks.FramebufferDelegateHolder;
import com.replaymod.render.hooks.WindowDelegateHolder;
import com.replaymod.render.mixin.MainWindowAccessor;
import de.johni0702.minecraft.gui.function.Closeable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC >= 26.2
//$$ import com.mojang.blaze3d.systems.GpuSurface;
//$$ import com.mojang.blaze3d.systems.SurfaceException;
//#endif

//#if MC >= 26.1
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.gl.WindowFramebuffer;
//#endif

public class VirtualWindow implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftClient mc;
    private final Window window;
    private final MainWindowAccessor acc;

    private final Framebuffer guiFramebuffer;
    private boolean isBound;
    private int framebufferWidth, framebufferHeight;

    private int gameWidth, gameHeight;


    public VirtualWindow(MinecraftClient mc) {
        this.mc = mc;
        this.window = mc.getWindow();
        this.acc = (MainWindowAccessor) (Object) this.window;

        framebufferWidth = acc.getFramebufferWidth();
        framebufferHeight = acc.getFramebufferHeight();

        //#if MC>=11700
        //$$ guiFramebuffer = new WindowFramebuffer(framebufferWidth, framebufferHeight);
        //#else
        guiFramebuffer = new Framebuffer(framebufferWidth, framebufferHeight, true
                //#if MC>=11400
                , false
                //#endif
        );
        //#endif

        WindowDelegateHolder.get(mc).setWindowDelegate(this);
    }

    @Override
    public void close() {
        guiFramebuffer.delete();

        WindowDelegateHolder.get(mc).setWindowDelegate(null);
    }

    public void bind() {
        gameWidth = acc.getFramebufferWidth();
        gameHeight = acc.getFramebufferHeight();
        acc.setFramebufferWidth(framebufferWidth);
        acc.setFramebufferHeight(framebufferHeight);
        applyScaleFactor();
        isBound = true;
    }

    public void unbind() {
        acc.setFramebufferWidth(gameWidth);
        acc.setFramebufferHeight(gameHeight);
        applyScaleFactor();
        isBound = false;
    }

    public void beginWrite() {
        FramebufferDelegateHolder.get(mc).setFramebufferDelegate(guiFramebuffer);
        //#if MC<12105
        guiFramebuffer.beginWrite(true);
        //#endif
    }

    public void endWrite() {
        //#if MC<12105
        guiFramebuffer.endWrite();
        //#endif
        FramebufferDelegateHolder.get(mc).setFramebufferDelegate(null);
    }

    //#if MC >= 26.2
    //$$ private boolean surfaceIsInvalid;
    //$$ private boolean windowSurfaceNeedsReconfiguring;
    //$$ public void flip() {
    //$$     if (framebufferWidth == 0 || framebufferHeight == 0) {
    //$$         return;
    //$$     }
    //$$
    //$$     GpuSurface windowSurface = mc.windowSurface();
    //$$
    //$$     if (windowSurfaceNeedsReconfiguring || windowSurface.isSuboptimal() && !surfaceIsInvalid) {
    //$$         GpuSurface.PresentMode presentMode = GpuSurface.PresentMode.getSupportedVsyncMode(windowSurface.supportedPresentModes(), mc.options.enableVsync().get());
    //$$         GpuSurface.Configuration config = new GpuSurface.Configuration(framebufferWidth, framebufferHeight, presentMode);
    //$$         try {
    //$$             windowSurface.configure(config);
    //$$             this.surfaceIsInvalid = false;
    //$$         } catch (SurfaceException e) {
    //$$             LOGGER.warn("Couldn't configure surface to {}: {}", config, e);
    //$$             this.surfaceIsInvalid = true;
    //$$         }
    //$$     }
    //$$
    //$$     if (surfaceIsInvalid || window.isMinimized()) {
    //$$         return;
    //$$     }
    //$$
    //$$     try {
    //$$         windowSurface.acquireNextTexture();
    //$$     } catch (SurfaceException e) {
    //$$         LOGGER.warn("Couldn't acquire next surface texture with config {}: {}", windowSurface.currentConfiguration(), e);
    //$$         this.surfaceIsInvalid = true;
    //$$         this.windowSurfaceNeedsReconfiguring = true;
    //$$         return;
    //$$     }
    //$$
    //$$     windowSurface.blitFromTexture(RenderSystem.getDevice().createCommandEncoder(), guiFramebuffer.getColorTextureView());
    //$$     RenderSystem.getDevice().createCommandEncoder().submit();
    //$$     windowSurface.present();
    //$$ }
    //#else
    public void flip() {
        //#if MC>=12105
        //$$ guiFramebuffer.blitToScreen();
        //#else
        guiFramebuffer.draw(framebufferWidth, framebufferHeight);
        //#endif

        //#if MC >= 26.1
        //$$ RenderSystem.flipFrame(null);
        //#elseif MC>=12102
        //$$ window.swapBuffers(null);
        //#elseif MC>=11500
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
    //#endif

    /**
     * Updates the size of the window's framebuffer. Must only be called while this window is bound.
     */
    public void onResolutionChanged(int newWidth, int newHeight) {
        if (newWidth == 0 || newHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return;
        }

        if (framebufferWidth == newWidth && framebufferHeight == newHeight) {
            return; // size is unchanged, nothing to do
        }

        framebufferWidth = newWidth;
        framebufferHeight = newHeight;
        //#if MC >= 26.2
        //$$ windowSurfaceNeedsReconfiguring = true;
        //#endif

        //#if MC>=11400
        guiFramebuffer.resize(newWidth, newHeight
                //#if MC>=11400 && MC<12102
                , false
                //#endif
        );
        //#else
        //$$ guiFramebuffer.createBindFramebuffer(newWidth, newHeight);
        //#endif

        applyScaleFactor();
        if (mc.currentScreen != null) {
            //#if MC>=12111
            //$$ mc.currentScreen.resize(window.getScaledWidth(), window.getScaledHeight());
            //#else
            mc.currentScreen.resize(mc, window.getScaledWidth(), window.getScaledHeight());
            //#endif
        }
    }

    private void applyScaleFactor() {
        //#if MC>=11400
        window.setScaleFactor(window.calculateScaleFactor(mc.options.guiScale, mc.forcesUnicodeFont()));
        //#else
        //$$ // Nothing to do, ScaledResolution re-computes the scale factor every time it is created
        //#endif
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public boolean isBound() {
        return isBound;
    }
}
