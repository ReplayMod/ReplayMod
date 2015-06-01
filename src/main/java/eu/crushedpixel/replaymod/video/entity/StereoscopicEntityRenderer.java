package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

import static net.minecraft.client.renderer.GlStateManager.loadIdentity;
import static net.minecraft.client.renderer.GlStateManager.matrixMode;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

public class StereoscopicEntityRenderer extends CustomEntityRenderer {

    private boolean leftEye;

    public StereoscopicEntityRenderer(RenderOptions options) {
        super(options);
    }

    public void setEye(boolean leftEye) {
        this.leftEye = leftEye;
    }

    @Override
    public void updateCameraAndRender(float renderPartialTicks) {
        boolean wasAnaglyph = mc.gameSettings.anaglyph;
        mc.gameSettings.anaglyph = true;
        super.updateCameraAndRender(renderPartialTicks);
        mc.gameSettings.anaglyph = wasAnaglyph;
    }

    protected void translateStereoscopic() {
        GlStateManager.translate(leftEye ? 0.07 : -0.07, 0, 0);
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
        super.renderSpectatorHand(partialTicks, leftEye ? 1 : 0);
    }
}
