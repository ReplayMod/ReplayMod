package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.glu.Project;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public abstract class CustomEntityRenderer {
    protected final Minecraft mc = Minecraft.getMinecraft();
    protected final EntityRenderer proxied = mc.entityRenderer;
    protected int frameCount;

    protected void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        Project.gluPerspective(fovY, aspect, zNear, zFar);
    }

    public void updateCameraAndRender(float renderPartialTicks) {
        if (mc.theWorld != null) {
            renderWorld(renderPartialTicks, 0);

            if (OpenGlHelper.shadersSupported) {
                mc.renderGlobal.renderEntityOutlineFramebuffer();

                if (proxied.theShaderGroup != null && proxied.useShader) {
                    matrixMode(GL_TEXTURE);
                    pushMatrix();
                    loadIdentity();
                    proxied.theShaderGroup.loadShaderGroup(renderPartialTicks);
                    popMatrix();
                }

                mc.getFramebuffer().bindFramebuffer(true);
            }
        }
    }

    protected void renderWorld(float partialTicks, long finishTimeNano) {
        proxied.updateLightmap(partialTicks);

        enableDepth();
        enableAlpha();
        alphaFunc(516, 0.5F);

        renderWorldPass(partialTicks, finishTimeNano, 2);
    }

    protected void renderWorldPass(float partialTicks, long finishTimeNano, int renderPass) {
        RenderGlobal renderglobal = mc.renderGlobal;
        EffectRenderer effectrenderer = mc.effectRenderer;
        Entity entity = mc.getRenderViewEntity();
        enableCull();
        viewport(0, 0, mc.displayWidth, mc.displayHeight);
        proxied.updateFogColor(partialTicks);
        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        setupCameraTransform(partialTicks);
        ActiveRenderInfo.updateRenderInfo(mc.thePlayer, mc.gameSettings.thirdPersonView == 2);
        ClippingHelperImpl.getInstance();
        ICamera camera = new NoCullingCamera();

        if (this.mc.gameSettings.renderDistanceChunks >= 4) {
            proxied.setupFog(-1, partialTicks);
            matrixMode(GL_PROJECTION);
            loadIdentity();
            gluPerspective(proxied.getFOVModifier(partialTicks, true), (float) mc.displayWidth / mc.displayHeight, 0.05F, proxied.farPlaneDistance * 2.0F);
            matrixMode(GL_MODELVIEW);
            renderglobal.renderSky(partialTicks, renderPass);
            matrixMode(GL_PROJECTION);
            loadIdentity();
            gluPerspective(proxied.getFOVModifier(partialTicks, true), (float) mc.displayWidth / mc.displayHeight, 0.05F, proxied.farPlaneDistance * MathHelper.SQRT_2);
            matrixMode(GL_MODELVIEW);
        }

        proxied.setupFog(0, partialTicks);
        shadeModel(GL_SMOOTH);

        if (entity.posY + entity.getEyeHeight() < 128) {
            renderCloudsCheck(renderglobal, partialTicks, renderPass);
        }

        proxied.setupFog(0, partialTicks);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        renderglobal.setupTerrain(entity, partialTicks, camera, frameCount++, mc.thePlayer.isSpectator());

        renderglobal.updateChunks(finishTimeNano);

        matrixMode(GL_MODELVIEW);
        pushMatrix();
        disableAlpha();
        renderglobal.renderBlockLayer(EnumWorldBlockLayer.SOLID, partialTicks, renderPass, entity);
        enableAlpha();
        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, partialTicks, renderPass, entity);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT, partialTicks, renderPass, entity);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        shadeModel(GL_FLAT);
        alphaFunc(516, 0.1F);

        matrixMode(GL_MODELVIEW);
        popMatrix();
        pushMatrix();
        RenderHelper.enableStandardItemLighting();

        net.minecraftforge.client.ForgeHooksClient.setRenderPass(0);
        renderglobal.renderEntities(entity, camera, partialTicks);
        net.minecraftforge.client.ForgeHooksClient.setRenderPass(0);
        RenderHelper.disableStandardItemLighting();
        proxied.disableLightmap();
        matrixMode(GL_MODELVIEW);
        popMatrix();

        enableBlend();
        tryBlendFuncSeparate(770, 1, 1, 0);
        renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getWorldRenderer(), entity, partialTicks);
        disableBlend();

        proxied.enableLightmap();
        effectrenderer.renderLitParticles(entity, partialTicks);
        RenderHelper.disableStandardItemLighting();
        proxied.setupFog(0, partialTicks);
        effectrenderer.renderParticles(entity, partialTicks);
        proxied.disableLightmap();

        depthMask(false);
        enableCull();
        proxied.renderRainSnow(partialTicks);
        depthMask(true);
        renderglobal.renderWorldBorder(entity, partialTicks);
        disableBlend();
        enableCull();
        tryBlendFuncSeparate(770, 771, 1, 0);
        alphaFunc(516, 0.1F);
        proxied.setupFog(0, partialTicks);
        enableBlend();
        depthMask(false);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        shadeModel(GL_SMOOTH);

        if (mc.gameSettings.fancyGraphics) {
            enableBlend();
            tryBlendFuncSeparate(770, 771, 1, 0);
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partialTicks, renderPass, entity);
            disableBlend();
        } else {
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partialTicks, renderPass, entity);
        }

        RenderHelper.enableStandardItemLighting();
        net.minecraftforge.client.ForgeHooksClient.setRenderPass(1);
        renderglobal.renderEntities(entity, camera, partialTicks);
        net.minecraftforge.client.ForgeHooksClient.setRenderPass(-1);
        RenderHelper.disableStandardItemLighting();

        shadeModel(GL_FLAT);
        depthMask(true);
        enableCull();
        disableBlend();
        disableFog();

        if (entity.posY + entity.getEyeHeight() >= 128) {
            renderCloudsCheck(renderglobal, partialTicks, renderPass);
        }

        net.minecraftforge.client.ForgeHooksClient.dispatchRenderLast(renderglobal, partialTicks);

        if(!ReplayHandler.isCamera() && ReplayHandler.getCurrentEntity() instanceof EntityPlayer)
            ReplayMod.spectatorRenderer.renderSpectatorHand((EntityPlayer)ReplayHandler.getCurrentEntity(), partialTicks, renderPass);
    }

    protected void setupCameraTransform(float partialTicks) {
        proxied.farPlaneDistance = (float)(this.mc.gameSettings.renderDistanceChunks * 16);

        matrixMode(GL_PROJECTION);
        loadIdentity();

        gluPerspective(proxied.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, proxied.farPlaneDistance * MathHelper.SQRT_2);

        matrixMode(GL_MODELVIEW);
        loadIdentity();

        orientCamera(partialTicks);
    }

    protected void orientCamera(float partialTicks) {
        proxied.orientCamera(partialTicks);
    }

    protected void renderCloudsCheck(RenderGlobal renderglobal, float partialTicks, int renderPass) {
        if (this.mc.gameSettings.shouldRenderClouds()) {
            matrixMode(GL_PROJECTION);
            loadIdentity();
            gluPerspective(proxied.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, proxied.farPlaneDistance * 4.0F);
            matrixMode(GL_MODELVIEW);
            pushMatrix();
            proxied.setupFog(0, partialTicks);
            renderglobal.renderClouds(partialTicks, renderPass);
            disableFog();
            popMatrix();
            matrixMode(GL_PROJECTION);
            loadIdentity();
            gluPerspective(proxied.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, proxied.farPlaneDistance * MathHelper.SQRT_2);
            matrixMode(GL_MODELVIEW);
        }
    }

    public static final class NoCullingCamera implements ICamera {

        @Override
        public boolean isBoundingBoxInFrustum(AxisAlignedBB p_78546_1_) {
            return true;
        }

        @Override
        public void setPosition(double p_78547_1_, double p_78547_3_, double p_78547_5_) {

        }
    }
}
