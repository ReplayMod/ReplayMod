package com.replaymod.replay.camera;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.core.utils.Utils;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replaystudio.util.Location;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

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

    /**
     * The entity whose hand was the last one rendered.
     */
    private Entity lastHandRendered = null;

    /**
     * The hashCode and equals methods of Entity are not stable.
     * Therefore we cannot register any event handlers directly in the CameraEntity class and
     * instead have this inner class.
     */
    private final EventHandler eventHandler = new EventHandler();

    public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager) {
        super(mcIn, worldIn, netHandlerPlayClient, statisticsManager);
        MinecraftForge.EVENT_BUS.register(eventHandler);
        if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
            cameraController = ReplayModReplay.instance.createCameraController(this);
        } else {
            cameraController = new SpectatorCameraController(this);
        }
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
    public void setCameraPosRot(Location pos) {
        setCameraRotation(pos.getYaw(), pos.getPitch(), roll);
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
            if (spectating != null && (view.getUniqueID() != spectating
                    || view.worldObj != worldObj)
                    || worldObj.getEntityByID(view.getEntityId()) != view) {
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
    public void setAngles(float yaw, float pitch) {
        if (mc.getRenderViewEntity() == this) {
            // Only update camera rotation when the camera is the view
            super.setAngles(yaw, pitch);
        }
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return falseUnlessSpectating(Entity::isEntityInsideOpaqueBlock); // Make sure no suffocation overlay is rendered
    }

    @Override
    public boolean isInsideOfMaterial(Material materialIn) {
        return falseUnlessSpectating(e -> e.isInsideOfMaterial(materialIn)); // Make sure no overlays are rendered
    }

    @Override
    public boolean isInLava() {
        return falseUnlessSpectating(Entity::isInLava); // Make sure no lava overlay is rendered
    }

    @Override
    public boolean isInWater() {
        return falseUnlessSpectating(Entity::isInWater); // Make sure no water overlay is rendered
    }

    @Override
    public boolean isBurning() {
        return falseUnlessSpectating(Entity::isBurning); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = mc.getRenderViewEntity();
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
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
        return ReplayModReplay.instance.getReplayHandler().isCameraView(); // Make sure we're treated as spectator
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        // Never render the camera
        // This is necessary to hide the player head in third person mode and to not
        // cause any unwanted shadows when rendering with shaders.
        return false;
    }

    @Override
    public boolean isInvisible() {
        Entity view = mc.getRenderViewEntity();
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public ResourceLocation getLocationSkin() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return Utils.getResourceLocationForPlayerUUID(view.getUniqueID());
        }
        return super.getLocationSkin();
    }

    @Override
    public String getSkinType() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getSkinType();
        }
        return super.getSkinType();
    }

    @Override
    public float getSwingProgress(float renderPartialTicks) {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getSwingProgress(renderPartialTicks);
        }
        return 0;
    }

    @Override
    public float getCooldownPeriod() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getCooldownPeriod();
        }
        return 1;
    }

    @Override
    public float getCooledAttackStrength(float adjustTicks) {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getCooledAttackStrength(adjustTicks);
        }
        // Default to 1 as to not render the cooldown indicator (renders for < 1)
        return 1;
    }

    @Override
    public EnumHand getActiveHand() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getActiveHand();
        }
        return super.getActiveHand();
    }

    @Override
    public boolean isHandActive() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).isHandActive();
        }
        return super.isHandActive();
    }

    @Override
    public RayTraceResult rayTrace(double p_174822_1_, float p_174822_3_) {
        RayTraceResult pos = super.rayTrace(p_174822_1_, 1f);

        // Make sure we can never look at blocks (-> no outline)
        if(pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
            pos.typeOfHit = RayTraceResult.Type.MISS;
        }

        return pos;
    }

    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
        // Do not open any block GUIs for the camera entities
        // Note: Vanilla GUIs are filtered out on a packet level, this only applies to mod GUIs
    }

    @Override
    public void setDead() {
        super.setDead();
        MinecraftForge.EVENT_BUS.unregister(eventHandler);
    }

    private void update() {
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

        Map<String, KeyBinding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getKeyBindings();
        if (keyBindings.get("replaymod.input.rollclockwise").isKeyDown()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("replaymod.input.rollcounterclockwise").isKeyDown()) {
            roll -= Utils.isCtrlDown() ? 0.2 : 1;
        }
    }

    private void updateArmYawAndPitch() {
        prevRenderArmYaw = renderArmYaw;
        prevRenderArmPitch = renderArmPitch;
        renderArmPitch = renderArmPitch +  (rotationPitch - renderArmPitch) * 0.5f;
        renderArmYaw = renderArmYaw +  (rotationYaw - renderArmYaw) * 0.5f;
    }

    public boolean canSpectate(Entity e) {
        return e != null && !e.isInvisible()
                && (e instanceof EntityPlayer || e instanceof EntityLiving || e instanceof EntityItemFrame);
    }

    private class EventHandler {
        @SubscribeEvent
        public void onPreClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                update();
                updateArmYawAndPitch();
            }
        }

        @SubscribeEvent
        public void onRenderUpdate(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                update();
            }
        }

        @SubscribeEvent
        public void preCrosshairRender(RenderGameOverlayEvent.Pre event) {
            // The crosshair should only render if targeted entity can actually be spectated
            if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                event.setCanceled(!canSpectate(mc.pointedEntity));
            }
            // Hotbar should never be rendered
            if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onSettingsChanged(SettingsChangedEvent event) {
            if (event.getKey() == Setting.CAMERA) {
                if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
                    cameraController = ReplayModReplay.instance.createCameraController(CameraEntity.this);
                } else {
                    cameraController = new SpectatorCameraController(CameraEntity.this);
                }
            }
        }

        @SubscribeEvent
        public void onRenderHand(RenderHandEvent event) {
            // Unless we are spectating another player, don't render our hand
            if (mc.getRenderViewEntity() == CameraEntity.this || !(mc.getRenderViewEntity() instanceof EntityPlayer)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderHandMonitor(RenderHandEvent event) {
            Entity view = mc.getRenderViewEntity();
            if (view instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    mc.entityRenderer.itemRenderer.prevEquippedProgressMainHand = 1;
                    mc.entityRenderer.itemRenderer.prevEquippedProgressOffHand = 1;
                    mc.entityRenderer.itemRenderer.equippedProgressMainHand = 1;
                    mc.entityRenderer.itemRenderer.equippedProgressOffHand = 1;
                    mc.entityRenderer.itemRenderer.itemStackMainHand = player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                    mc.entityRenderer.itemRenderer.itemStackOffHand = player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);


                    mc.thePlayer.renderArmYaw = mc.thePlayer.prevRenderArmYaw = player.rotationYaw;
                    mc.thePlayer.renderArmPitch = mc.thePlayer.prevRenderArmPitch = player.rotationPitch;
                }
            }
        }

        @SubscribeEvent
        public void onEntityViewRenderEvent(EntityViewRenderEvent.CameraSetup event) {
            if (mc.getRenderViewEntity() == CameraEntity.this) {
                event.setRoll(roll);
            }
        }
    }
}
