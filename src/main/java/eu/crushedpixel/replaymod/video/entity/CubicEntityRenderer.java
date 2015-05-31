package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

public class CubicEntityRenderer extends CustomEntityRenderer {

    public static enum Direction {
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

    public CubicEntityRenderer(boolean stable) {
        this.stable = stable;
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
}
