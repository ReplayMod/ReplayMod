package com.replaymod.render.hooks;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

//#if MC>=11500
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11400
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
//#else
//#if MC>=11300
//$$ import net.minecraftforge.fml.hooks.BasicEventHooks;
//#else
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif
//#endif

import java.io.IOException;

public class EntityRendererHandler implements WorldRenderer {
    public final MinecraftClient mc = MCVer.getMinecraft();

    @Getter
    protected final RenderSettings settings;

    @Getter
    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(this);
    }

    @Override
    public void renderWorld(final float partialTicks, CaptureData data) {
        this.data = data;
        renderWorld(partialTicks, 0);
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        //#if MC>=11400
        PreRenderCallback.EVENT.invoker().preRender();
        //#else
        //#if MC>=11300
        //$$ BasicEventHooks.onRenderTickStart(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickStart(partialTicks);
        //#endif
        //#endif

        if (mc.world != null && mc.player != null) {
            mc.gameRenderer.renderWorld(
                    partialTicks,
                    finishTimeNano
                    //#if MC>=11500
                    //$$ , new MatrixStack()
                    //#endif
            );
        }

        //#if MC>=11400
        PostRenderCallback.EVENT.invoker().postRender();
        //#else
        //#if MC>=11300
        //$$ BasicEventHooks.onRenderTickEnd(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickEnd(partialTicks);
        //#endif
        //#endif
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(null);
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
