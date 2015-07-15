package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.capturer.StereoscopicOpenGlFrameCapturer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import static net.minecraft.client.renderer.GlStateManager.loadIdentity;
import static net.minecraft.client.renderer.GlStateManager.matrixMode;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

public class StereoscopicEntityRenderer extends CustomEntityRenderer<StereoscopicOpenGlFrameCapturer.Data> {

    public StereoscopicEntityRenderer(RenderOptions options) {
        super(options);
    }

    @Override
    public void loadShader(ResourceLocation resourceLocation) {
        if (proxied.theShaderGroup != null) {
            proxied.theShaderGroup.deleteShaderGroup();
            proxied.theShaderGroup = null;
        }
        proxied.useShader = false;
    }

    protected void translateStereoscopic() {
        GlStateManager.translate(data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? 0.07 : -0.07, 0, 0);
    }

    @Override
    protected void setupCameraTransform(float partialTicks) {
        proxied.farPlaneDistance = (float)(this.mc.gameSettings.renderDistanceChunks * 16);

        matrixMode(GL_PROJECTION);
        loadIdentity();
        translateStereoscopic();

        gluPerspective(proxied.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, proxied.farPlaneDistance * MathHelper.SQRT_2);

        matrixMode(GL_MODELVIEW);
        loadIdentity();
        translateStereoscopic();

        orientCamera(partialTicks);
    }

    @Override
    protected void renderSpectatorHand(float partialTicks, int renderPass) {
        super.renderSpectatorHand(partialTicks, data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE ? 1 : 0);
    }
}
