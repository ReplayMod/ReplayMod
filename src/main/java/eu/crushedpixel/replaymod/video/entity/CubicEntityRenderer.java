package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.capturer.CubicOpenGlFrameCapturer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.lang.reflect.Field;

public class CubicEntityRenderer extends CustomEntityRenderer<CubicOpenGlFrameCapturer.Data> {

    public CubicEntityRenderer(RenderOptions options) {
        super(options);

        try {
            Field hookField = RenderManager.class.getField("hook");
            hookField.set(mc.getRenderManager(), this);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    @Override
    public void close() throws IOException{
        super.close();
        try {
            Field hookField = RenderManager.class.getField("hook");
            hookField.set(mc.getRenderManager(), null);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    @Override
    protected void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        super.gluPerspective(90, 1, zNear, zFar);
    }

    @Override
    public void loadShader(ResourceLocation resourceLocation) {
        if (proxied.theShaderGroup != null) {
            proxied.theShaderGroup.deleteShaderGroup();
            proxied.theShaderGroup = null;
        }
        proxied.useShader = false;
    }

    @Override
    protected void orientCamera(float partialTicks) {
        // Rotate
        switch (data) {
            case FRONT:
                GlStateManager.rotate(0, 0.0F, 1.0F, 0.0F);
                break;
            case RIGHT:
                GlStateManager.rotate(90, 0.0F, 1.0F, 0.0F);
                break;
            case BACK:
                GlStateManager.rotate(180, 0.0F, 1.0F, 0.0F);
                break;
            case LEFT:
                GlStateManager.rotate(-90, 0.0F, 1.0F, 0.0F);
                break;
            case BOTTOM:
                GlStateManager.rotate(90, 1.0F, 0.0F, 0.0F);
                break;
            case TOP:
                GlStateManager.rotate(-90, 1.0F, 0.0F, 0.0F);
                break;
        }

        // Minecraft goes back a little so we have to invert that
        GlStateManager.translate(0.0F, 0.0F, 0.1F);
        super.orientCamera(partialTicks);
    }

    @Override
    protected void renderSpectatorHand(float partialTicks, int renderPass) {
        // No spectator hands during 360° view
    }

    @Override
    protected void renderParticle(EntityFX fx, WorldRenderer worldRenderer, Entity view, float partialTicks, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
        // Align all particles towards the camera
        double dx = fx.prevPosX + (fx.posX - fx.prevPosX) * partialTicks - view.posX;
        double dy = fx.prevPosY + (fx.posY - fx.prevPosY) * partialTicks - view.posY;
        double dz = fx.prevPosZ + (fx.posZ - fx.prevPosZ) * partialTicks - view.posZ;
        double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double yaw = -Math.atan2(dx, dz);

        rotX = (float) Math.cos(yaw);
        rotZ = (float) Math.sin(yaw);
        rotXZ = (float) Math.cos(pitch);

        rotYZ = (float) (-rotZ * Math.sin(pitch));
        rotXY = (float) (rotX * Math.sin(pitch));

        super.renderParticle(fx, worldRenderer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    }

    @SuppressWarnings("unused") // Called by ASM
    public void beforeEntityRender(double dx, double dy, double dz) {
        double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        double yaw = -Math.atan2(dx, dz);
        mc.getRenderManager().playerViewX = (float) Math.toDegrees(pitch);
        mc.getRenderManager().playerViewY = (float) Math.toDegrees(yaw);
    }
}
