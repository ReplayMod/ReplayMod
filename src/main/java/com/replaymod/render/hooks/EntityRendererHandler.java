package com.replaymod.render.hooks;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;

//#if MC>=10800
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
//#else
//$$ import com.replaymod.core.versions.MCVer.GlStateManager;
//$$ import cpw.mods.fml.common.FMLCommonHandler;
//#endif

import java.io.IOException;

public class EntityRendererHandler implements WorldRenderer {
    public final Minecraft mc = Minecraft.getMinecraft();

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
        FMLCommonHandler.instance().onRenderTickStart(partialTicks);

        mc.entityRenderer.updateLightmap(partialTicks);

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.5F);

        //#if MC>=10800
        mc.entityRenderer.renderWorldPass(2, partialTicks, finishTimeNano);
        //#else
        //$$ mc.entityRenderer.renderWorld(partialTicks, finishTimeNano);
        //#endif

        FMLCommonHandler.instance().onRenderTickEnd(partialTicks);
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(null);
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public interface GluPerspective {
        void replayModRender_gluPerspective(float fovY, float aspect, float zNear, float zFar);
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
