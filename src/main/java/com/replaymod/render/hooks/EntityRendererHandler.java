package com.replaymod.render.hooks;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.CaptureData;
import com.replaymod.render.capturer.WorldRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;

import static net.minecraft.client.renderer.GlStateManager.*;

public class EntityRendererHandler implements WorldRenderer {
    public final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    protected final RenderSettings settings;

    public CaptureData data;

    public EntityRendererHandler(RenderSettings settings) {
        this.settings = settings;

        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(this);
    }

    public void withDisplaySize(int displayWidth, int displayHeight, Runnable runnable) {
        final int prevWidth = mc.displayWidth;
        final int prevHeight = mc.displayHeight;
        mc.displayWidth = displayWidth;
        mc.displayHeight = displayHeight;

        runnable.run();

        mc.displayWidth = prevWidth;
        mc.displayHeight = prevHeight;
    }

    @Override
    public void renderWorld(ReadableDimension displaySize, final float partialTicks, CaptureData data) {
        this.data = data;
        withDisplaySize(displaySize.getWidth(), displaySize.getHeight(), new Runnable() {
            @Override
            public void run() {
                renderWorld(partialTicks, 0);
            }
        });
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        mc.entityRenderer.updateLightmap(partialTicks);

        enableDepth();
        enableAlpha();
        alphaFunc(516, 0.5F);

        mc.entityRenderer.renderWorldPass(2, partialTicks, finishTimeNano);
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.entityRenderer).replayModRender_setHandler(null);
    }

    public static final class NoCullingClippingHelper extends ClippingHelper {
        @Override
        public boolean isBoxInFrustum(double p_78553_1_, double p_78553_3_, double p_78553_5_, double p_78553_7_, double p_78553_9_, double p_78553_11_) {
            return true;
        }
    }

    public interface GluPerspective {
        void replayModRender_gluPerspective(float fovY, float aspect, float zNear, float zFar);
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
