package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;

public class CubicEntityRenderer extends CustomEntityRenderer {

    public enum Direction {
        TOP(1), BOTTOM(9), LEFT(4), FRONT(5), RIGHT(6), BACK(7);

        private final int frame;

        Direction(int frame) {
            this.frame = frame;
        }

        public static Direction forFrame(int frame) {
            for (Direction d : values()) {
                if (d.frame == frame) {
                    return d;
                }
            }
            return null;
        }

        public int getCubicFrame() {
            return frame;
        }
    }

    private final boolean stable;
    private Direction direction;

    public CubicEntityRenderer(RenderOptions options, int frameSize, boolean stable) {
        super(options, frameSize, frameSize);
        this.stable = stable;

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
    public void cleanup() {
        super.cleanup();
        try {
            Field hookField = RenderManager.class.getField("hook");
            hookField.set(mc.getRenderManager(), null);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void setFrame(int id) {
        setDirection(Direction.forFrame(id));
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
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
    protected void setupCameraTransform(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();
        float orgYaw = entity.rotationYaw;
        float orgPitch = entity.rotationPitch;
        float orgPrevYaw = entity.prevRotationYaw;
        float orgPrevPitch = entity.prevRotationPitch;
        float orgRoll = ReplayHandler.getCameraTilt();

        super.setupCameraTransform(partialTicks);

        entity.rotationYaw = orgYaw;
        entity.rotationPitch = orgPitch;
        entity.prevRotationYaw = orgPrevYaw;
        entity.prevRotationPitch = orgPrevPitch;
        if (stable) {
            ReplayHandler.setCameraTilt(orgRoll);
        }
    }

    @Override
    protected void orientCamera(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();

        // Rotate
        switch(direction) {
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

        if (stable) {
            // Stop the minecraft code from doing any rotation
            entity.prevRotationPitch = entity.rotationPitch = 0;
            entity.prevRotationYaw = entity.rotationYaw = 0;
            ReplayHandler.setCameraTilt(0);
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
