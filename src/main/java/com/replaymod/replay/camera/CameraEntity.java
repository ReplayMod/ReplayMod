package com.replaymod.replay.camera;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.core.utils.Utils;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replay.events.ReplayChatMessageEvent;
import com.replaymod.replaystudio.util.Location;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay {@link Minecraft#thePlayer} should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
public class CameraEntity extends EntityClientPlayerMP {
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

    public CameraEntity(Minecraft mcIn, World worldIn, Session session, NetHandlerPlayClient netHandlerPlayClient, StatFileWriter statFileWriter) {
        super(mcIn, worldIn, session, netHandlerPlayClient, statFileWriter);
        entityUniqueID = UUID.randomUUID(); // Need to not have the same UUID as the player who recorded the replay
        FMLCommonHandler.instance().bus().register(eventHandler);
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
        if (to == this) return;
        float yOffset = 1.62f; // Magic value (eye height) from EntityRenderer#orientCamera
        prevPosX = to.prevPosX;
        prevPosY = to.prevPosY + yOffset;
        prevPosZ = to.prevPosZ;
        prevRotationYaw = to.prevRotationYaw;
        prevRotationPitch = to.prevRotationPitch;
        posX = to.posX;
        posY = to.posY + yOffset;
        posZ = to.posZ;
        rotationYaw = to.rotationYaw;
        rotationPitch = to.rotationPitch;
        lastTickPosX = to.lastTickPosX;
        lastTickPosY = to.lastTickPosY + yOffset;
        lastTickPosZ = to.lastTickPosZ;
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        this.boundingBox.setBounds(
                posX - width / 2, posY, posZ - width / 2,
                posX + width / 2, posY + height, posZ + width / 2);
    }

    @Override
    public void onUpdate() {
        EntityLivingBase view = mc.renderViewEntity;
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            if (spectating != null && (view.getUniqueID() != spectating
                    || view.worldObj != worldObj)
                    || worldObj.getEntityByID(view.getEntityId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = worldObj.getPlayerEntityByUUID(spectating);
                if (view != null) {
                    mc.renderViewEntity = view;
                } else {
                    mc.renderViewEntity = this;
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
        if (mc.renderViewEntity == this) {
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
    public boolean handleLavaMovement() {
        return falseUnlessSpectating(Entity::handleLavaMovement); // Make sure no lava overlay is rendered
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
        Entity view = mc.renderViewEntity;
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
    public boolean canBeCollidedWith() {
        return false; // We are a camera, we cannot collide
    }

    /* TODO 1.7.10: isSpectator has been added in 1.8, this is probably going to require lots of manual changes
    @Override
    public boolean isSpectator() {
        return ReplayModReplay.instance.getReplayHandler().isCameraView(); // Make sure we're treated as spectator
    }
    */

    @Override
    public boolean shouldRenderInPass(int pass) {
        // Never render the camera
        // This is necessary to hide the player head in third person mode and to not
        // cause any unwanted shadows when rendering with shaders.
        return false;
    }

    @Override
    public boolean isInvisible() {
        Entity view = mc.renderViewEntity;
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public ResourceLocation getLocationSkin() {
        Entity view = mc.renderViewEntity;
        if (view != this && view instanceof EntityPlayer) {
            return Utils.getResourceLocationForPlayerUUID(view.getUniqueID());
        }
        return super.getLocationSkin();
    }

    @Override
    public float getSwingProgress(float renderPartialTicks) {
        Entity view = mc.renderViewEntity;
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getSwingProgress(renderPartialTicks);
        }
        return 0;
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
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
        // Do not open any block GUIs for the camera entities
        // Note: Vanilla GUIs are filtered out on a packet level, this only applies to mod GUIs
    }

    @Override
    public void setDead() {
        super.setDead();
        FMLCommonHandler.instance().bus().unregister(eventHandler);
        MinecraftForge.EVENT_BUS.unregister(eventHandler);
    }

    private void update() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        if (mc.gameSettings.keyBindAttack.isPressed() || mc.gameSettings.keyBindUseItem.isPressed()) {
            if (canSpectate(mc.pointedEntity)) {
                ReplayModReplay.instance.getReplayHandler().spectateEntity((EntityLivingBase) mc.pointedEntity);
                // Make sure we don't exit right away
                mc.gameSettings.keyBindSneak.pressTime = 0;
            }
        }

        Map<String, KeyBinding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getKeyBindings();
        if (keyBindings.get("replaymod.input.rollclockwise").getIsKeyPressed()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("replaymod.input.rollcounterclockwise").getIsKeyPressed()) {
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
                && e instanceof EntityPlayer; // 1.7.10 has no general concept of eye height
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
        super.addChatMessage(message);
    }

    // All event handlers need to be public in 1.7.10
    public class EventHandler {
        private EventHandler() {}

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
            if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                event.setCanceled(!canSpectate(mc.pointedEntity));
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
            if (mc.renderViewEntity == CameraEntity.this || !(mc.renderViewEntity instanceof EntityPlayer)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderHandMonitor(RenderHandEvent event) {
            Entity view = mc.renderViewEntity;
            if (view instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    mc.entityRenderer.itemRenderer.prevEquippedProgress = 1;
                    mc.entityRenderer.itemRenderer.equippedProgress = 1;
                    mc.entityRenderer.itemRenderer.itemToRender = player.inventory.getCurrentItem();
                    mc.entityRenderer.itemRenderer.equippedItemSlot = player.inventory.currentItem;

                    mc.thePlayer.renderArmYaw = mc.thePlayer.prevRenderArmYaw = player.rotationYaw;
                    mc.thePlayer.renderArmPitch = mc.thePlayer.prevRenderArmPitch = player.rotationPitch;
                }
            }
        }

        /* TODO 1.7.10: This event has been added in 1.8, has to be replaced with mixin
        @SubscribeEvent
        public void onEntityViewRenderEvent(EntityViewRenderEvent.CameraSetup event) {
            if (mc.getRenderViewEntity() == CameraEntity.this) {
                event.roll = roll;
            }
        }
        */

        private boolean heldItemTooltipsWasTrue;

        @SubscribeEvent
        public void preRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
            switch (event.type) {
                case ALL:
                    heldItemTooltipsWasTrue = mc.gameSettings.heldItemTooltips;
                    mc.gameSettings.heldItemTooltips = false;
                    break;
                case ARMOR:
                case HEALTH:
                case FOOD:
                case AIR:
                case HOTBAR:
                case EXPERIENCE:
                case HEALTHMOUNT:
                case JUMPBAR:
                    event.setCanceled(true);
                    break;
                case HELMET:
                case PORTAL:
                case CROSSHAIRS:
                case BOSSHEALTH:
                case TEXT:
                case CHAT:
                case PLAYER_LIST:
                case DEBUG:
                    break;
            }
        }

        @SubscribeEvent
        public void postRenderGameOverlay(RenderGameOverlayEvent.Post event) {
            if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
            mc.gameSettings.heldItemTooltips = heldItemTooltipsWasTrue;
        }
    }
}
