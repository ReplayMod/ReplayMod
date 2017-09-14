package com.replaymod.render.hooks;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.WorldRenderer;
import cpw.mods.fml.common.FMLCommonHandler;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

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

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorld(partialTicks, finishTimeNano);

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
