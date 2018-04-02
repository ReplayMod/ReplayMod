package com.replaymod.extras.advancedscreenshots;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.rendering.Pipelines;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

//#if MC>=10800
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
//#endif

@RequiredArgsConstructor
public class ScreenshotRenderer implements RenderInfo {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final RenderSettings settings;

    private int framesDone;

    public boolean renderScreenshot() throws Throwable {
        try {
            int displayWidthBefore = mc.displayWidth;
            int displayHeightBefore = mc.displayHeight;

            boolean hideGUIBefore = mc.gameSettings.hideGUI;
            mc.gameSettings.hideGUI = true;

            //#if MC>=10800
            ChunkLoadingRenderGlobal clrg = new ChunkLoadingRenderGlobal(mc.renderGlobal);
            //#endif

            Pipelines.newPipeline(settings.getRenderMethod(),this,
                    new ScreenshotWriter(settings.getOutputFile())).run();

            //#if MC>=10800
            clrg.uninstall();
            //#endif

            mc.gameSettings.hideGUI = hideGUIBefore;
            mc.resize(displayWidthBefore, displayHeightBefore);
            return true;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.makeCrashReport(e, "Creating Equirectangular Screenshot");
            Minecraft.getMinecraft().crashed(report);
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
