package com.replaymod.replay.camera;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replay.events.ReplayChatMessageEvent;
import com.replaymod.replaystudio.util.Location;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;

//#if MC>=11300
import net.minecraft.util.math.RayTraceFluidMode;
//#endif

//#if MC>=10904
import net.minecraft.inventory.EntityEquipmentSlot;
//#if MC>=11200
//#if MC>=11300
import net.minecraft.client.util.RecipeBookClient;
//#else
//$$ import net.minecraft.stats.RecipeBook;
//#endif
//#endif
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
//#else
//$$ import net.minecraft.stats.StatFileWriter;
//$$ import net.minecraft.util.AxisAlignedBB;
//$$ import net.minecraft.util.IChatComponent;
//$$ import net.minecraft.util.MovingObjectPosition;
//#endif

//#if MC>=10800
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#if MC>=11300
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.EventPriority;
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
//#else
//$$ import cpw.mods.fml.common.eventhandler.EventPriority;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent;
//$$ import net.minecraft.client.entity.EntityClientPlayerMP;
//$$ import net.minecraft.util.Session;
//#endif

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.replaymod.core.versions.MCVer.*;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay the player should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
public class CameraEntity
        //#if MC>=10800
        extends EntityPlayerSP
        //#else
        //$$ extends EntityClientPlayerMP
        //#endif
{
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

    //#if MC>=11300
    public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager, RecipeBookClient recipeBookClient) {
        super(mcIn, worldIn, netHandlerPlayClient, statisticsManager, recipeBookClient);
    //#else
    //#if MC>=11200
    //$$ public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager, RecipeBook recipeBook) {
    //$$     super(mcIn, worldIn, netHandlerPlayClient, statisticsManager, recipeBook);
    //#else
    //#if MC>=10904
    //$$ public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager) {
    //$$     super(mcIn, worldIn, netHandlerPlayClient, statisticsManager);
    //#else
    //#if MC>=10800
    //$$ public CameraEntity(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandlerPlayClient, StatFileWriter statFileWriter) {
    //$$     super(mcIn, worldIn, netHandlerPlayClient, statFileWriter);
    //#else
    //$$ public CameraEntity(Minecraft mcIn, World worldIn, Session session, NetHandlerPlayClient netHandlerPlayClient, StatFileWriter statFileWriter) {
    //$$     super(mcIn, worldIn, session, netHandlerPlayClient, statFileWriter);
    //#endif
    //#endif
    //#endif
    //#endif
        FORGE_BUS.register(eventHandler);
        FML_BUS.register(eventHandler);
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
        //#if MC>=10800
        float yOffset = 0;
        //#else
        //$$ float yOffset = 1.62f; // Magic value (eye height) from EntityRenderer#orientCamera
        //#endif
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
        //#if MC>=10800
        //#if MC>=11300
        setBoundingBox(new AxisAlignedBB(
        //#else
        //$$ setEntityBoundingBox(new AxisAlignedBB(
        //#endif
        //#else
        //$$ this.boundingBox.setBB(AxisAlignedBB.getBoundingBox(
        //#endif
                posX - width / 2, posY, posZ - width / 2,
                posX + width / 2, posY + height, posZ + width / 2));
    }

    @Override
    //#if MC>=11300
    public void tick() {
    //#else
    //$$ public void onUpdate() {
    //#endif
        //#if MC>=10800
        Entity view = getRenderViewEntity(mc);
        //#else
        //$$ EntityLivingBase view = getRenderViewEntity(mc);
        //#endif
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            if (spectating != null && (view.getUniqueID() != spectating
                    || world(view) != world(this))
                    || world(this).getEntityByID(view.getEntityId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = world(this).getPlayerEntityByUUID(spectating);
                if (view != null) {
                    setRenderViewEntity(mc, view);
                } else {
                    setRenderViewEntity(mc, this);
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
        if (world(mc) != null) {
            world(this, world(mc));
        }
        super.preparePlayerToSpawn();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        if (getRenderViewEntity(mc) == this) {
            // Only update camera rotation when the camera is the view
            super.setRotation(yaw, pitch);
        }
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return falseUnlessSpectating(Entity::isEntityInsideOpaqueBlock); // Make sure no suffocation overlay is rendered
    }

    /* FIXME
    @Override
    public boolean isInsideOfMaterial(Material materialIn) {
        return falseUnlessSpectating(e -> e.isInsideOfMaterial(materialIn)); // Make sure no overlays are rendered
    }
    */

    //#if MC>=10800
    @Override
    public boolean isInLava() {
        return falseUnlessSpectating(Entity::isInLava); // Make sure no lava overlay is rendered
    }
    //#else
    //$$ @Override
    //$$ public boolean handleLavaMovement() {
    //$$     return falseUnlessSpectating(Entity::handleLavaMovement); // Make sure no lava overlay is rendered
    //$$ }
    //#endif

    @Override
    public boolean isInWater() {
        return falseUnlessSpectating(Entity::isInWater); // Make sure no water overlay is rendered
    }

    @Override
    public boolean isBurning() {
        return falseUnlessSpectating(Entity::isBurning); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = getRenderViewEntity(mc);
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false; // We are in full control of ourselves
    }

    //#if MC>=10800
    @Override
    protected void createRunningParticles() {
        // We do not produce any particles, we are a camera
    }
    //#endif

    @Override
    public boolean canBeCollidedWith() {
        return false; // We are a camera, we cannot collide
    }

    //#if MC>=10800
    @Override
    public boolean isSpectator() {
        return ReplayModReplay.instance.getReplayHandler().isCameraView(); // Make sure we're treated as spectator
    }
    //#endif

    @Override
    public boolean shouldRenderInPass(int pass) {
        // Never render the camera
        // This is necessary to hide the player head in third person mode and to not
        // cause any unwanted shadows when rendering with shaders.
        return false;
    }

    @Override
    public boolean isInvisible() {
        Entity view = getRenderViewEntity(mc);
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public ResourceLocation getLocationSkin() {
        Entity view = getRenderViewEntity(mc);
        if (view != this && view instanceof EntityPlayer) {
            return Utils.getResourceLocationForPlayerUUID(view.getUniqueID());
        }
        return super.getLocationSkin();
    }

    //#if MC>=10800
    @Override
    public String getSkinType() {
        Entity view = mc.getRenderViewEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getSkinType();
        }
        return super.getSkinType();
    }
    //#endif

    @Override
    public float getSwingProgress(float renderPartialTicks) {
        Entity view = getRenderViewEntity(mc);
        if (view != this && view instanceof EntityPlayer) {
            return ((EntityPlayer) view).getSwingProgress(renderPartialTicks);
        }
        return 0;
    }

    //#if MC>=10904
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

    //#if MC>=11300
    @Override
    public RayTraceResult rayTrace(double blockReachDistance, float partialTicks, RayTraceFluidMode p_174822_4_) {
        RayTraceResult pos = super.rayTrace(blockReachDistance, partialTicks, p_174822_4_);

        // Make sure we can never look at blocks (-> no outline)
        if(pos != null && pos.type == RayTraceResult.Type.BLOCK) {
            pos.type = RayTraceResult.Type.MISS;
        }

        return pos;
    }
    //#else
    //$$ @Override
    //$$ public RayTraceResult rayTrace(double p_174822_1_, float p_174822_3_) {
    //$$     RayTraceResult pos = super.rayTrace(p_174822_1_, 1f);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
    //$$         pos.typeOfHit = RayTraceResult.Type.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#endif
    //#else
    //$$ @Override
    //$$ public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
    //$$     MovingObjectPosition pos = super.rayTrace(p_174822_1_, 1f);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
    //$$         pos.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#endif

    /* FIXME
    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
        // Do not open any block GUIs for the camera entities
        // Note: Vanilla GUIs are filtered out on a packet level, this only applies to mod GUIs
    }
    */

    @Override
    //#if MC>=11300
    public void remove() {
        super.remove();
    //#else
    //$$ public void setDead() {
    //$$     super.setDead();
    //#endif
        FORGE_BUS.unregister(eventHandler);
        FML_BUS.unregister(eventHandler);
    }

    private void update() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        if (mc.gameSettings.keyBindAttack.isPressed() || mc.gameSettings.keyBindUseItem.isPressed()) {
            if (canSpectate(mc.pointedEntity)) {
                ReplayModReplay.instance.getReplayHandler().spectateEntity(
                        //#if MC<=10710
                        //$$ (EntityLivingBase)
                        //#endif
                        mc.pointedEntity);
                // Make sure we don't exit right away
                mc.gameSettings.keyBindSneak.pressTime = 0;
            }
        }

        Map<String, KeyBinding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getKeyBindings();
        if (isKeyDown(keyBindings.get("replaymod.input.rollclockwise"))) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (isKeyDown(keyBindings.get("replaymod.input.rollcounterclockwise"))) {
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
                //#if MC>=10800
                && (e instanceof EntityPlayer || e instanceof EntityLiving || e instanceof EntityItemFrame);
                //#else
                //$$ && e instanceof EntityPlayer; // cannot be more generic since 1.7.10 has no concept of eye height
                //#endif
    }

    //#if MC>=11102
    @Override
    public void sendMessage(ITextComponent message) {
        if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
        super.sendMessage(message);
    }
    //#else
    //$$ @Override
    //#if MC>=10904
    //$$ public void addChatMessage(ITextComponent message) {
    //#else
    //$$ public void addChatMessage(IChatComponent message) {
    //#endif
    //$$     if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
    //$$     super.addChatMessage(message);
    //$$ }
    //#endif

    //#if MC>=10800
    private
    //#else
    //$$ public // All event handlers need to be public in 1.7.10
    //#endif
    class EventHandler {
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
            if (MCVer.getType(event) == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                event.setCanceled(!canSpectate(mc.pointedEntity));
            }
            // Hotbar should never be rendered
            if (MCVer.getType(event) == RenderGameOverlayEvent.ElementType.HOTBAR) {
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
            if (getRenderViewEntity(mc) == CameraEntity.this || !(getRenderViewEntity(mc) instanceof EntityPlayer)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderHandMonitor(RenderHandEvent event) {
            Entity view = getRenderViewEntity(mc);
            if (view instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    //#if MC>=10904
                    /* FIXME needs AT
                    mc.entityRenderer.itemRenderer.prevEquippedProgressMainHand = 1;
                    mc.entityRenderer.itemRenderer.prevEquippedProgressOffHand = 1;
                    mc.entityRenderer.itemRenderer.equippedProgressMainHand = 1;
                    mc.entityRenderer.itemRenderer.equippedProgressOffHand = 1;
                    mc.entityRenderer.itemRenderer.itemStackMainHand = player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                    mc.entityRenderer.itemRenderer.itemStackOffHand = player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
                    */
                    //#else
                    //$$ mc.entityRenderer.itemRenderer.prevEquippedProgress = 1;
                    //$$ mc.entityRenderer.itemRenderer.equippedProgress = 1;
                    //$$ mc.entityRenderer.itemRenderer.itemToRender = player.inventory.getCurrentItem();
                    //$$ mc.entityRenderer.itemRenderer.equippedItemSlot = player.inventory.currentItem;
                    //#endif


                    player(mc).renderArmYaw = player(mc).prevRenderArmYaw = player.rotationYaw;
                    player(mc).renderArmPitch = player(mc).prevRenderArmPitch = player.rotationPitch;
                }
            }
        }

        //#if MC>=10800
        @SubscribeEvent
        public void onEntityViewRenderEvent(EntityViewRenderEvent.CameraSetup event) {
            if (mc.getRenderViewEntity() == CameraEntity.this) {
                //#if MC>=10904
                event.setRoll(roll);
                //#else
                //$$ event.roll = roll;
                //#endif
            }
        }
        //#endif

        private boolean heldItemTooltipsWasTrue;

        @SubscribeEvent
        public void preRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
            switch (MCVer.getType(event)) {
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
                //#if MC>=10904
                case POTION_ICONS:
                //#endif
                    event.setCanceled(true);
                    break;
                case HELMET:
                case PORTAL:
                case CROSSHAIRS:
                case BOSSHEALTH:
                //#if MC>=10904
                case BOSSINFO:
                case SUBTITLES:
                //#endif
                case TEXT:
                case CHAT:
                case PLAYER_LIST:
                case DEBUG:
                    break;
            }
        }

        @SubscribeEvent
        public void postRenderGameOverlay(RenderGameOverlayEvent.Post event) {
            if (MCVer.getType(event) != RenderGameOverlayEvent.ElementType.ALL) return;
            mc.gameSettings.heldItemTooltips = heldItemTooltipsWasTrue;
        }
    }
}
