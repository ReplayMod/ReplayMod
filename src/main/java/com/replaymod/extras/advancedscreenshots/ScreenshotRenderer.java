package com.replaymod.extras.advancedscreenshots;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.rendering.Pipelines;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;

//#if MC>=11400
import com.replaymod.render.mixin.MainWindowAccessor;
import static com.replaymod.core.versions.MCVer.getWindow;
//#endif

//#if MC>=10800 && MC<11500
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
//#endif

import static com.replaymod.core.versions.MCVer.getRenderPartialTicks;

@RequiredArgsConstructor
public class ScreenshotRenderer implements RenderInfo {

    private final MinecraftClient mc = MCVer.getMinecraft();

    private final RenderSettings settings;

    private int framesDone;

    public boolean renderScreenshot() throws Throwable {
        try {
            //#if MC>=11400
            int displayWidthBefore = getWindow(mc).getFramebufferWidth();
            int displayHeightBefore = getWindow(mc).getFramebufferHeight();
            //#else
            //$$ int displayWidthBefore = mc.displayWidth;
            //$$ int displayHeightBefore = mc.displayHeight;
            //#endif

            boolean hideGUIBefore = mc.options.hudHidden;
            mc.options.hudHidden = true;

            //#if MC>=10800 && MC<11500
            ChunkLoadingRenderGlobal clrg = new ChunkLoadingRenderGlobal(mc.worldRenderer);
            //#endif

            if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
                BlendState.setState(new BlendState(settings.getOutputFile()));
                Pipelines.newBlendPipeline(this).run();
            } else {
                Pipelines.newPipeline(settings.getRenderMethod(), this,
                        new ScreenshotWriter(settings.getOutputFile())).run();
            }

            //#if MC>=10800 && MC<11500
            clrg.uninstall();
            //#endif

            mc.options.hudHidden = hideGUIBefore;
            //#if MC>=11400
            //noinspection ConstantConditions
            MainWindowAccessor acc = (MainWindowAccessor) (Object) getWindow(mc);
            acc.setFramebufferWidth(displayWidthBefore);
            acc.setFramebufferHeight(displayHeightBefore);
            mc.getFramebuffer().resize(displayWidthBefore, displayHeightBefore
                    //#if MC>=11400
                    , false
                    //#endif
            );
            //#if MC>=11500
            //$$ mc.gameRenderer.onResized(displayWidthBefore, displayHeightBefore);
            //#endif
            //#else
            //$$ mc.resize(displayWidthBefore, displayHeightBefore);
            //#endif
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
        return getRenderPartialTicks();
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }
}
