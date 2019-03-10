package com.replaymod.render.hooks;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;

//#if MC>=10800
//#if MC>=11300
import net.minecraftforge.fml.hooks.BasicEventHooks;
//#else
//$$ import net.minecraft.client.renderer.GlStateManager;
//$$ import net.minecraftforge.fml.common.FMLCommonHandler;
//#endif
//#else
//$$ import com.replaymod.core.versions.MCVer.GlStateManager;
//$$ import cpw.mods.fml.common.FMLCommonHandler;
//#endif

import java.io.IOException;

public class EntityRendererHandler implements WorldRenderer {
    public final Minecraft mc = MCVer.getMinecraft();

    @Getter
    protected final RenderSettings settings;

    @Getter
    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(this);
    }

    @Override
    public void renderWorld(final float partialTicks, CaptureData data) {
        this.data = data;
        renderWorld(partialTicks, 0);
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        //#if MC>=11300
        BasicEventHooks.onRenderTickStart(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickStart(partialTicks);
        //#endif

        //#if MC>=11300
        mc.entityRenderer.renderWorld(partialTicks, finishTimeNano);
        //#else
        //$$ mc.entityRenderer.updateLightmap(partialTicks);
        //$$
        //$$ GlStateManager.enableDepth();
        //$$ GlStateManager.enableAlpha();
        //$$ GlStateManager.alphaFunc(516, 0.5F);
        //$$
        //#if MC>=10800
        //$$ mc.entityRenderer.renderWorldPass(2, partialTicks, finishTimeNano);
        //#else
        //$$ mc.entityRenderer.renderWorld(partialTicks, finishTimeNano);
        //#endif
        //#endif

        //#if MC>=11300
        BasicEventHooks.onRenderTickEnd(partialTicks);
        //#else
        //$$ FMLCommonHandler.instance().onRenderTickEnd(partialTicks);
        //#endif
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(null);
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
