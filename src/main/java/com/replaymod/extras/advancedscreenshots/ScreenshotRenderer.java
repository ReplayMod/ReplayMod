package com.replaymod.extras.advancedscreenshots;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import com.replaymod.render.rendering.Pipelines;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

@RequiredArgsConstructor
public class ScreenshotRenderer implements RenderInfo {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final RenderSettings settings;

    public boolean renderScreenshot() throws Throwable {
        try {
            int displayWidthBefore = mc.displayWidth;
            int displayHeightBefore = mc.displayHeight;

            boolean hideGUIBefore = mc.gameSettings.hideGUI;
            mc.gameSettings.hideGUI = true;

            ChunkLoadingRenderGlobal clrg = new ChunkLoadingRenderGlobal(mc.renderGlobal);

            Pipelines.newPipeline(settings.getRenderMethod(),this,
                    new ScreenshotWriter(settings.getOutputFile())).run();

            clrg.uninstall();

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
    public int getTotalFrames() {
        // render 2 frames, because only the second contains all frames fully loaded
        return 2;
    }

    @Override
    public float updateForNextFrame() {
        return mc.timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }
}
