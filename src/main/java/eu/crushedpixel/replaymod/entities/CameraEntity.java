package eu.crushedpixel.replaymod.entities;

import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.replay.LesserDataWatcher;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.Sys;

public class CameraEntity extends EntityPlayer {

    public static final double SPEED_CHANGE = 0.5;
    public static final double LOWER_SPEED = 2;
    public static final double UPPER_SPEED = 20;

    private static double MAX_SPEED = 10;
    private static double THRESHOLD = MAX_SPEED / 20;
    private static double DECAY = MAX_SPEED/3;

    public static void modifyCameraSpeed(boolean increase) {
        setCameraMaximumSpeed(getCameraMaximumSpeed() + (increase ? 1 : -1) * SPEED_CHANGE);
    }

    public static void setCameraMaximumSpeed(double maxSpeed) {
        if(maxSpeed < LOWER_SPEED || maxSpeed > UPPER_SPEED) return;
        MAX_SPEED = maxSpeed;
        THRESHOLD = MAX_SPEED / 20;
        DECAY = 5;
    }

    public static double getCameraMaximumSpeed() {
        return MAX_SPEED;
    }

    private Vec3 direction;
    private Vec3 dirBefore;
    private double motion;

    private final Minecraft mc = Minecraft.getMinecraft();

    private long lastCall = 0;

    private boolean speedup = false;

    public CameraEntity(World worldIn) {
        super(worldIn, Minecraft.getMinecraft().getSession().getProfile());
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if(!ReplayHandler.isInReplay()) return;
            Entity view = Minecraft.getMinecraft().getRenderViewEntity();
            if (view != this && view != null) {
                prevPosX = view.prevPosX;
                prevPosY = view.prevPosY;
                prevPosZ = view.prevPosZ;
                prevRotationYaw = view.prevRotationYaw;
                prevRotationPitch = view.prevRotationPitch;
                posX = view.posX;
                posY = view.posY;
                posZ = view.posZ;
                rotationYaw = view.rotationYaw;
                rotationPitch = view.rotationPitch;
            }
        }
    }

    //frac = time since last tick
    public void updateMovement() {

        long frac = Sys.getTime() - lastCall;

        if(frac == 0) return;

        double decFac = Math.max(0, 1 - (DECAY * (frac / 1000D)));

        if(speedup) {
            if(motion < THRESHOLD) motion = THRESHOLD;
            motion /= decFac;
        } else {
            motion *= decFac;
        }

        motion = Math.min(motion, MAX_SPEED);

        if(ReplayHandler.getCameraEntity() != null && mc.thePlayer != null
                && mc.getRenderViewEntity() != null) {
            //Aligns the particle rotation
            mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = mc.getRenderViewEntity().rotationPitch;
            mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = mc.getRenderViewEntity().rotationYaw;
        }

        lastCall = Sys.getTime();

        if(direction == null || motion < THRESHOLD) {
            return;
        }

        Vec3 movement = direction.normalize();
        double factor = motion * (frac / 1000D);

        moveRelative(movement.xCoord * factor, movement.yCoord * factor, movement.zCoord * factor);
    }

    public void speedUp() {
        speedup = true;
    }

    public void stopSpeedUp() {
        speedup = false;
    }

    public void setMovement(MoveDirection dir) {
        switch(dir) {
            case BACKWARD:
                direction = this.getVectorForRotation(-rotationPitch, rotationYaw - 180);
                break;
            case DOWN:
                direction = this.getVectorForRotation(90, 0);
                break;
            case FORWARD:
                direction = this.getVectorForRotation(rotationPitch, rotationYaw);
                break;
            case LEFT:
                direction = this.getVectorForRotation(0, rotationYaw - 90);
                break;
            case RIGHT:
                direction = this.getVectorForRotation(0, rotationYaw + 90);
                break;
            case UP:
                direction = this.getVectorForRotation(-90, 0);
                break;
        }

        Vec3 dbf = direction;

        if(dirBefore != null) {
            direction = dirBefore.normalize().add(direction);
        }

        dirBefore = dbf;

        updateMovement();
    }

    public void moveAbsolute(AdvancedPosition pos) {
        this.moveAbsolute(pos.getX(), pos.getY(), pos.getZ());
        rotationPitch = prevRotationPitch = (float)pos.getPitch();
        rotationYaw = prevRotationYaw = (float)pos.getYaw();
        updateBoundingBox();
    }

    public void moveAbsolute(double x, double y, double z) {
        if(ReplayHandler.isInPath()) return;
        this.lastTickPosX = this.prevPosX = this.posX = x;
        this.lastTickPosY = this.prevPosY = this.posY = y;
        this.lastTickPosZ = this.prevPosZ = this.posZ = z;
        updateBoundingBox();
    }

    public void moveRelative(double x, double y, double z) {
        if(ReplayHandler.isInPath()) return;
        this.lastTickPosX = this.prevPosX = this.posX = this.posX + x;
        this.lastTickPosY = this.prevPosY = this.posY = this.posY + y;
        this.lastTickPosZ = this.prevPosZ = this.posZ = this.posZ + z;
        updateBoundingBox();
    }

    public void movePath(AdvancedPosition pos) {
        this.prevRotationPitch = this.rotationPitch = (float)pos.getPitch();
        this.prevRotationYaw = this.rotationYaw = (float)pos.getYaw();
        this.lastTickPosX = this.prevPosX = this.posX = pos.getX();
        this.lastTickPosY = this.prevPosY = this.posY = pos.getY();
        this.lastTickPosZ = this.prevPosZ = this.posZ = pos.getZ();
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        this.setEntityBoundingBox(new AxisAlignedBB(posX - width / 2, posY, posZ - width / 2,
                posX + width / 2, posY + height, posZ + width / 2));
    }

    @Override
    protected void entityInit() {
        this.dataWatcher = new LesserDataWatcher(this);
        this.setSize(0.6f, 1.8f);
        updateBoundingBox();
    }

    @Override
    public void setAngles(float yaw, float pitch) {
        this.rotationYaw = (float) ((double) this.rotationYaw + (double) yaw * 0.15D);
        this.rotationPitch = (float) ((double) this.rotationPitch - (double) pitch * 0.15D);
        this.rotationPitch = MathHelper.clamp_float(this.rotationPitch, -90.0F, 90.0F);
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYawHead = this.rotationYawHead = this.prevRotationYaw = this.rotationYaw;
    }

    @Override
    public boolean canBePushed() {
        return true;
    }

    @Override
    protected void createRunningParticles() {}

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    public void setCurrentItemOrArmor(int slotIn, ItemStack stack) {}

    @Override
    public ItemStack[] getInventory() {
        return null;
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    public enum MoveDirection {
        UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD
    }

    @Override
    public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
        MovingObjectPosition pos = super.rayTrace(p_174822_1_, 1f);

        if(pos != null && pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            pos.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
        }

        return pos;
    }
}
