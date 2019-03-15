package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.rendering.Pipelines;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;

//#if MC>=10800
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
//#endif

@RequiredArgsConstructor
public class ScreenshotRenderer implements RenderInfo {

    private final Minecraft mc = MCVer.getMinecraft();

    private final RenderSettings settings;

    private int framesDone;

    public boolean renderScreenshot() throws Throwable {
        try {
            //#if MC>=11300
            int displayWidthBefore = mc.mainWindow.framebufferWidth;
            int displayHeightBefore = mc.mainWindow.framebufferHeight;
            //#else
            //$$ int displayWidthBefore = mc.displayWidth;
            //$$ int displayHeightBefore = mc.displayHeight;
            //#endif

            boolean hideGUIBefore = mc.gameSettings.hideGUI;
            mc.gameSettings.hideGUI = true;

            //#if MC>=10800
            ChunkLoadingRenderGlobal clrg = new ChunkLoadingRenderGlobal(mc.renderGlobal);
            //#endif

            if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
                BlendState.setState(new BlendState(settings.getOutputFile()));
                Pipelines.newBlendPipeline(this).run();
            } else {
                Pipelines.newPipeline(settings.getRenderMethod(), this,
                        new ScreenshotWriter(settings.getOutputFile())).run();
            }

            //#if MC>=10800
            clrg.uninstall();
            //#endif

            mc.gameSettings.hideGUI = hideGUIBefore;
            //#if MC>=11300
            mc.mainWindow.framebufferWidth = displayWidthBefore;
            mc.mainWindow.framebufferHeight = displayHeightBefore;
            mc.getFramebuffer().createBindFramebuffer(displayWidthBefore, displayHeightBefore);
            //#else
            //$$ mc.resize(displayWidthBefore, displayHeightBefore);
            //#endif
            return true;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.makeCrashReport(e, "Creating Equirectangular Screenshot");
            MCVer.getMinecraft().crashed(report);
        }
        return false;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    @Override
    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public int getTotalFrames() {
        // render 2 frames, because only the second contains all frames fully loaded
        return 2;
    }

    @Override
    public float updateForNextFrame() {
        framesDone++;
        return mc.timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }
}
