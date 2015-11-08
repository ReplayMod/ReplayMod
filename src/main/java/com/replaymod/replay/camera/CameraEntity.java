package com.replaymod.replay.camera;

import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay {@link Minecraft#thePlayer} should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
public class CameraEntity extends EntityPlayerSP {
    /**
     * Roll of this camera in degrees.
     */
    public float roll;

    @Getter
    @Setter
    private CameraController cameraController;

    private long lastControllerUpdate = System.currentTimeMillis();

    public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatFileWriter statFileWriter) {
        super(mcIn, worldIn, netHandlerPlayClient, statFileWriter);
        FMLCommonHandler.instance().bus().register(this);
        cameraController = ReplayModReplay.instance.createCameraController(this);
    }

    /**
     * Moves the camera by the specified delta.
     * @param x Delta in X direction
     * @param y Delta in Y direction
     * @param z Delta in Z direction
     */
    public void moveCamera(double x, double y, double z) {
        setCameraPosition(posX + x, posY + y, posZ + z);
    }

    /**
     * Set the camera position.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setCameraPosition(double x, double y, double z) {
        this.lastTickPosX = this.prevPosX = this.posX = x;
        this.lastTickPosY = this.prevPosY = this.posY = y;
        this.lastTickPosZ = this.prevPosZ = this.posZ = z;
        updateBoundingBox();
    }

    /**
     * Sets the camera rotation.
     * @param yaw Yaw in degrees
     * @param pitch Pitch in degrees
     * @param roll Roll in degrees
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.roll = roll;
    }

    /**
     * Sets the camera position and rotation to that of the specified AdvancedPosition
     * @param pos The position and rotation to set
     */
    public void setCameraPosRot(AdvancedPosition pos) {
        setCameraRotation((float) pos.getYaw(), (float) pos.getPitch(), (float) pos.getRoll());
        setCameraPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets the camera position and rotation to that of the specified entity.
     * @param to The entity whose position to copy
     */
    public void setCameraPosRot(Entity to) {
        prevPosX = to.prevPosX;
        prevPosY = to.prevPosY;
        prevPosZ = to.prevPosZ;
        prevRotationYaw = to.prevRotationYaw;
        prevRotationPitch = to.prevRotationPitch;
        posX = to.posX;
        posY = to.posY;
        posZ = to.posZ;
        rotationYaw = to.rotationYaw;
        rotationPitch = to.rotationPitch;
        lastTickPosX = to.lastTickPosX;
        lastTickPosY = to.lastTickPosY;
        lastTickPosZ = to.lastTickPosZ;
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        setEntityBoundingBox(new AxisAlignedBB(
                posX - width / 2, posY, posZ - width / 2,
                posX + width / 2, posY + height, posZ + width / 2));
    }

    @Override
    public void onUpdate() {
        Entity view = mc.getRenderViewEntity();
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            if (spectating != null && (view.getUniqueID() != spectating || view.worldObj != worldObj)) {
                view = worldObj.getPlayerEntityByUUID(spectating);
                if (view != null) {
                    mc.setRenderViewEntity(view);
                } else {
                    mc.setRenderViewEntity(this);
                    return;
                }
            }
            // Move cmera to their position so when we exit the first person view
            // we don't jump back to where we entered it
            if (view != this) {
                setCameraPosRot(view);
            }
        }
    }

    @Override
    public void preparePlayerToSpawn() {
        // Make sure our world is up-to-date in case of world changes
        if (mc.theWorld != null) {
            worldObj = mc.theWorld;
        }
        super.preparePlayerToSpawn();
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false; // Make sure no suffocation overlay is rendered
    }

    @Override
    public boolean isInsideOfMaterial(Material materialIn) {
        return false; // Make sure no overlays are rendered
    }

    @Override
    public boolean isInLava() {
        return false; // Make sure no lava overlay is rendered
    }

    @Override
    public boolean isInWater() {
        return false; // Make sure no water overlay is rendered
    }

    @Override
    public boolean canBePushed() {
        return false; // We are in full control of ourselves
    }

    @Override
    protected void createRunningParticles() {
        // We do not produce any particles, we are a camera
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // We are a camera, we cannot collide
    }

    @Override
    public boolean isSpectator() {
        return true; // Make sure we're treated as spectator
    }

    @Override
    public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
        MovingObjectPosition pos = super.rayTrace(p_174822_1_, 1f);

        // Make sure we can never look at blocks (-> no outline)
        if(pos != null && pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            pos.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
        }

        return pos;
    }

    @Override
    public void setDead() {
        super.setDead();
        FMLCommonHandler.instance().bus().unregister(this);
    }

    @SubscribeEvent
    public void onRenderUpdate(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            long now = System.currentTimeMillis();
            long timePassed = now - lastControllerUpdate;
            cameraController.update(timePassed / 50f);
            lastControllerUpdate = now;

            if (mc.gameSettings.keyBindAttack.isPressed() || mc.gameSettings.keyBindUseItem.isPressed()) {
                if (canSpectate(mc.pointedEntity)) {
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(mc.pointedEntity);
                    // Make sure we don't exit right away
                    mc.gameSettings.keyBindSneak.pressTime = 0;
                }
            }
        }
    }

    public boolean canSpectate(Entity e) {
        return e != null && !e.isInvisible()
                && (e instanceof EntityPlayer || e instanceof EntityLiving || e instanceof EntityItemFrame);
    }

    @SubscribeEvent
    public void onSettingsChanged(SettingsChangedEvent event) {
        if (event.getKey() == Setting.CAMERA) {
            cameraController = ReplayModReplay.instance.createCameraController(this);
        }
    }
}
