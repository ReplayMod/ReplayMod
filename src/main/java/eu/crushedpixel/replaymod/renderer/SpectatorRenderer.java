package eu.crushedpixel.replaymod.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.util.glu.Project;

/**
 * Created by mariusmetzger on 24/05/15.
 */
public class SpectatorRenderer extends SafeEntityRenderer {

    private final Minecraft mc;

    public SpectatorRenderer(Minecraft mcIn, EntityRenderer renderer) {
        super(mcIn, renderer);
        this.mc = mcIn;
    }

    @Override
    protected void renderHand(float p_78476_1_, int p_78476_2_) {
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            float f1 = 0.07F;

            if (this.mc.gameSettings.anaglyph)
            {
                GlStateManager.translate((float)(-(p_78476_2_ * 2 - 1)) * f1, 0.0F, 0.0F);
            }

            Project.gluPerspective(this.getFOVModifier(p_78476_1_, false), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();

            if (this.mc.gameSettings.anaglyph)
            {
                GlStateManager.translate((float)(p_78476_2_ * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }

            GlStateManager.pushMatrix();
            this.hurtCameraEffect(p_78476_1_);

            if (this.mc.gameSettings.viewBobbing)
            {
                this.setupViewBobbing(p_78476_1_);
            }

            boolean flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();

            if (this.mc.gameSettings.thirdPersonView == 0 && !flag && !this.mc.gameSettings.hideGUI && !this.mc.playerController.isSpectator())
            {
                this.enableLightmap();
                this.itemRenderer.renderItemInFirstPerson(p_78476_1_);
                this.disableLightmap();
            }

            GlStateManager.popMatrix();

            if (this.mc.gameSettings.thirdPersonView == 0 && !flag)
            {
                this.itemRenderer.renderOverlays(p_78476_1_);
                this.hurtCameraEffect(p_78476_1_);
            }

            if (this.mc.gameSettings.viewBobbing)
            {
                this.setupViewBobbing(p_78476_1_);
            }

    }
}
