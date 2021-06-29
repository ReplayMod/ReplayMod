package com.replaymod.render.hooks;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PreRenderHandCallback;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.Setting;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

//#if MC>=11400
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import net.minecraft.util.Util;
//#else
//#if MC>=11400
//$$ import net.minecraftforge.fml.hooks.BasicEventHooks;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif
//#endif

import java.io.IOException;

public class EntityRendererHandler extends EventRegistrations implements WorldRenderer {
    public final MinecraftClient mc = MCVer.getMinecraft();

    protected final RenderSettings settings;

    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    private final long startTime;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        //#if MC>=11400
        this.startTime = Util.getMeasuringTimeNano();
        //#else
        //$$ this.startTime = System.nanoTime();
        //#endif

        on(PreRenderHandCallback.EVENT, () -> omnidirectional);

        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(this);
        register();
    }

    @Override
    public void renderWorld(final float partialTicks, CaptureData data) {
        this.data = data;
        long offsetMillis;
        if (ReplayMod.instance.getSettingsRegistry().get(Setting.FRAME_TIME_FROM_WORLD_TIME)) {
            offsetMillis = ReplayModReplay.instance.getReplayHandler().getReplaySender().currentTimeStamp();
        } else {
            offsetMillis = renderInfo.getFramesDone() * 1_000L / settings.getFramesPerSecond();
        }
        long frameStartTimeNano = startTime + offsetMillis * 1_000_000L;
        renderWorld(partialTicks, frameStartTimeNano);
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        //#if MC>=11400
        PreRenderCallback.EVENT.invoker().preRender();
        //#else
        //#if MC>=11400
        //$$ BasicEventHooks.onRenderTickStart(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickStart(partialTicks);
        //#endif
        //#endif

        if (mc.world != null && mc.player != null) {
            Screen orgScreen = mc.currentScreen;
            boolean orgPauseOnLostFocus = mc.options.pauseOnLostFocus;
            try {
                mc.currentScreen = null; // do not want to render the current screen (that'd just be the progress gui)
                mc.options.pauseOnLostFocus = false; // do not want the pause menu to open if the window is unfocused

                //#if MC>=11400
                mc.gameRenderer.render(partialTicks, finishTimeNano, true);
                //#else
                //$$ mc.setIngameNotInFocus(); // this should already be the case but it somehow still sometimes is not
                //#if MC>=10809
                //$$ mc.entityRenderer.updateCameraAndRender(partialTicks, finishTimeNano);
                //#else
                //$$ mc.entityRenderer.updateCameraAndRender(partialTicks);
                //#endif
                //#endif
            } finally {
                mc.currentScreen = orgScreen;
                mc.options.pauseOnLostFocus = orgPauseOnLostFocus;
            }
        }

        //#if MC>=11400
        PostRenderCallback.EVENT.invoker().postRender();
        //#else
        //#if MC>=11400
        //$$ BasicEventHooks.onRenderTickEnd(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickEnd(partialTicks);
        //#endif
        //#endif
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(null);
        unregister();
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public RenderSettings getSettings() {
        return this.settings;
    }

    public RenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
