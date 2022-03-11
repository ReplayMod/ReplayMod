package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.rendering.Pipelines;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.crash.CrashReport;

import static com.replaymod.core.versions.MCVer.resizeMainWindow;

public class ScreenshotRenderer implements RenderInfo {

    private final MinecraftClient mc = MCVer.getMinecraft();

    private final RenderSettings settings;

    private int framesDone;

    public ScreenshotRenderer(RenderSettings settings) {
        this.settings = settings;
    }

    public boolean renderScreenshot() throws Throwable {
        try {
            Window window = mc.getWindow();
            int widthBefore = window.getFramebufferWidth();
            int heightBefore = window.getFramebufferHeight();

            ForceChunkLoadingHook clrg = new ForceChunkLoadingHook(mc.worldRenderer);

            if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
                BlendState.setState(new BlendState(settings.getOutputFile()));
                Pipelines.newBlendPipeline(this).run();
            } else {
                Pipelines.newPipeline(settings.getRenderMethod(), this,
                        new ScreenshotWriter(settings.getOutputFile())).run();
            }

            clrg.uninstall();

            resizeMainWindow(mc, widthBefore, heightBefore);
            return true;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.create(e, "Creating Equirectangular Screenshot");
            MCVer.getMinecraft().setCrashReport(report);
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
        return mc.getTickDelta();
    }

    @Override
    public void updatePostRender(float tickDelta) {
        // nothing to do
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }
}
