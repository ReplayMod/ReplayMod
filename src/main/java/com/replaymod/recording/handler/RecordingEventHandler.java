package com.replaymod.recording.handler;

import com.replaymod.recording.packet.PacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;

//#if MC>=10904
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
//#else
//#if MC>=10800
//$$ import net.minecraft.util.BlockPos;
//#endif
//#endif
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
//#endif

import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Field;

import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static com.replaymod.core.versions.MCVer.*;

public class RecordingEventHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PacketListener packetListener;
    // private final ActionListener actionListener;
    //TODO remove
    private final Logger logger;

    private Double lastX, lastY, lastZ;
    private Float lastPitch, lastYaw;
    private ItemStack[] playerItems     = new ItemStack[6];
    private ItemStack[] playerInventory = new ItemStack[36];
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private int lastHotbar = 0;
    private Integer rotationYawHeadBefore;
    //#if MC>=10904
    private boolean wasHandActive;
    private EnumHand lastActiveHand;
    //#endif

    public RecordingEventHandler(PacketListener packetListener, Logger logger) {
        this.packetListener = packetListener;
        this.logger = logger;
    }

    public void register() {
        FML_BUS.register(this);
        FORGE_BUS.register(this);
        ((RecordingEventSender) mc.renderGlobal).setRecordingEventHandler(this);
    }

    public void unregister() {
        FML_BUS.unregister(this);
        FORGE_BUS.unregister(this);
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.renderGlobal);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    public void onPlayerJoin() {
        try {
            //#if MC>=10904
            packetListener.save(new SPacketSpawnPlayer(player(mc)));
            //#else
            //$$ packetListener.save(new S0CPacketSpawnPlayer(player(mc)));
            //#endif
        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onPlayerRespawn() {
        try {
            //#if MC>=10904
            packetListener.save(new SPacketSpawnPlayer(player(mc)));
            //#else
            //$$ packetListener.save(new S0CPacketSpawnPlayer(player(mc)));
            //#endif
            lastX = lastY = lastZ = null;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //#if MC>=10904
    public void onClientSound(SoundEvent sound, SoundCategory category,
                              double x, double y, double z, float volume, float pitch) {
        try {
            // Send to all other players in ServerWorldEventHandler#playSoundToAllNearExcept
            packetListener.save(new SPacketSoundEffect(sound, category, x, y, z, volume, pitch));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void onClientEffect(int type, BlockPos pos, int data) {
        try {
            // Send to all other players in ServerWorldEventHandler#playEvent
            packetListener.save(new SPacketEffect(type, pos, data, false));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    //#endif

    private void recordActions(EntityPlayer plr) {
        //if( event.phase.equals(Phase.END)){
            if (plr == null) { return;}
            //Record tick time
            
            //Record if gui window is open
            if(mc.currentScreen != null) {
                // GUI is open - mark this and return (no actions possible)
                ByteBuf byteBuf = Unpooled.buffer();
                PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
                packetBuffer.writeVarInt(0);
                packetListener.save(new SPacketCustomPayload("t", packetBuffer));
                return;
            } else {
                // Gui is not open - mark tick and record any actions
                ByteBuf byteBuf = Unpooled.buffer();
                PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
                packetBuffer.writeVarInt(1);
                packetListener.save(new SPacketCustomPayload("t", packetBuffer));
            }

            if(lastPitch == null || lastYaw == null) {
                lastPitch = plr.rotationPitch;
                lastYaw = plr.rotationYawHead;
            } else {
                float diffPitch = lastPitch - plr.rotationPitch;
                float diffYaw = lastYaw - plr.rotationYawHead;
                
                if (Math.abs(diffPitch) + Math.abs(diffYaw) > 0.0001){
                    ByteBuf byteBuf = Unpooled.buffer();
                    PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
                    packetBuffer.writeFloat(lastYaw);
                    packetBuffer.writeFloat(lastPitch);
                    packetListener.save(new SPacketCustomPayload("c", packetBuffer));
                    lastPitch = plr.rotationPitch;
                    lastYaw = plr.rotationYawHead;
                }
                

                // if (Math.abs(diffPitch) + Math.abs(diffYaw) > 0.01) {
                //     //TODO remove after validation of action space
                //     String debugStr = "Turn/Tilt" + " > " + Float.toString(diffYaw) + ' ' + Float.toString(diffPitch);
                //     ITextComponent debugMsg = new TextComponentString(debugStr);
                //     packetListener.save(new SPacketChat(debugMsg, ChatType.CHAT));
                // }

                
            }

            int hotbarIdx = plr.inventory.currentItem;
            for (KeyBinding binding : mc.gameSettings.keyBindings){
                if(binding.isKeyDown()){
                    // //Record that key was down
                    // logger.info(binding.getKeyModifier() +
                    //     " - " + binding.getDisplayName() + 
                    //     " - " + binding.getKeyCode() +  
                    //     " - " + binding.isPressed() + 
                    //     " - " + binding.getKeyDescription());
                    
                    // Hotbar bindings are 2-10 - handle this seperatly to account for scroll wheel
                    if (2 <= binding.getKeyCode() && binding.getKeyCode() <= 10){
                        continue;
                    }

                    ByteBuf byteBuf = Unpooled.buffer();
                    PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
                    packetBuffer.writeVarInt(binding.getKeyCode());
                    packetListener.save(new SPacketCustomPayload("a", packetBuffer));

                    // //TODO remove after validation of action space
                    // String debugStr = binding.getDisplayName() + ":" + binding.getKeyCode() + " > " + binding.getKeyDescription();
                    // ITextComponent debugMsg = new TextComponentString(debugStr);
                    // packetListener.save(new SPacketChat(debugMsg, ChatType.CHAT));
                }
            }
            if (hotbarIdx != lastHotbar){
                lastHotbar = hotbarIdx;
                logger.info("Setting hotbar to " + Integer.toString(hotbarIdx) + " (key:" + Integer.toString(hotbarIdx + 2) + ")");
                ByteBuf byteBuf = Unpooled.buffer();
                PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
                packetBuffer.writeVarInt(hotbarIdx + 2);
                packetListener.save(new SPacketCustomPayload("a", packetBuffer));

                // //TODO remove after validation of action space
                // String debugStr = "Setting hotbar to " + Integer.toString(hotbarIdx) + " (key:" + Integer.toString(hotbarIdx + 2) + ")";
                // ITextComponent debugMsg = new TextComponentString(debugStr);
                // packetListener.save(new SPacketChat(debugMsg, ChatType.CHAT));
            }
        //}
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent e) {
        try {
            if(e.player != player(mc)) return;

            recordActions(e.player);

            boolean force = false;
            if(lastX == null || lastY == null || lastZ == null) {
                force = true;
                lastX = e.player.posX;
                lastY = e.player.posY;
                lastZ = e.player.posZ;
            }

            ticksSinceLastCorrection++;
            if(ticksSinceLastCorrection >= 100) {
                ticksSinceLastCorrection = 0;
                force = true;
            }

            double dx = e.player.posX - lastX;
            double dy = e.player.posY - lastY;
            double dz = e.player.posZ - lastZ;

            lastX = e.player.posX;
            lastY = e.player.posY;
            lastZ = e.player.posZ;

            Packet packet;
            if (force || Math.abs(dx) > 8.0 || Math.abs(dy) > 8.0 || Math.abs(dz) > 8.0) {
                //#if MC>=10904
                packet = new SPacketEntityTeleport(e.player);
                //#else
                //#if MC>=10800
                //$$ packet = new S18PacketEntityTeleport(e.player);
                //#else
                //$$ // In 1.7.10 the client player entity has its posY at eye height
                //$$ // but for all other entities it's at their feet (as it should be).
                //$$ // So, to correctly position the player, we teleport them to their feet (i.a. directly after spawn).
                //$$ // Note: this leaves the lastY value offset by the eye height but because it's only used for relative
                //$$ //       movement, that doesn't matter.
                //$$ S18PacketEntityTeleport teleportPacket = new S18PacketEntityTeleport(e.player);
                //$$ teleportPacket.field_149457_c = floor(e.player.boundingBox.minY * 32);
                //$$ packet = teleportPacket;
                //#endif
                //#endif
            } else {
                byte newYaw = (byte) ((int) (e.player.rotationYaw * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (e.player.rotationPitch * 256.0F / 360.0F));

                //#if MC>=10904
                packet = new SPacketEntity.S17PacketEntityLookMove(e.player.getEntityId(),
                        (short) Math.round(dx * 4096), (short) Math.round(dy * 4096), (short) Math.round(dz * 4096),
                        newYaw, newPitch, e.player.onGround);
                //#else
                //$$ packet = new S14PacketEntity.S17PacketEntityLookMove(e.player.getEntityId(),
                //$$         (byte) Math.round(dx * 32), (byte) Math.round(dy * 32), (byte) Math.round(dz * 32),
                //$$         newYaw, newPitch
                        //#if MC>=10800
                        //$$ , e.player.onGround
                        //#endif
                //$$ );
                //#endif
            }

            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int)(e.player.rotationYawHead * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                //#if MC>=10904
                packetListener.save(new SPacketEntityHeadLook(e.player, (byte) rotationYawHead));
                //#else
                //$$ packetListener.save(new S19PacketEntityHeadLook(e.player, (byte) rotationYawHead));
                //#endif
                rotationYawHeadBefore = rotationYawHead;
            }

            //#if MC>=10904
            packetListener.save(new SPacketEntityVelocity(e.player.getEntityId(), e.player.motionX, e.player.motionY, e.player.motionZ));
            //#else
            //$$ packetListener.save(new S12PacketEntityVelocity(e.player.getEntityId(), e.player.motionX, e.player.motionY, e.player.motionZ));
            //#endif

            //Animation Packets
            //Swing Animation
            if (e.player.isSwingInProgress && e.player.swingProgressInt == -1) {
                //#if MC>=10904
                packetListener.save(new SPacketAnimation(e.player, e.player.swingingHand == EnumHand.MAIN_HAND ? 0 : 3));
                //#else
                //$$ packetListener.save(new S0BPacketAnimation(e.player, 0));
                //#endif
            }

			/*
        //Potion Effect Handling
		List<Integer> found = new ArrayList<Integer>();
		for(PotionEffect pe : (Collection<PotionEffect>)e.player.getActivePotionEffects()) {
			found.add(pe.getPotionID());
			if(lastEffects.contains(found)) continue;
			S1DPacketEntityEffect pee = new S1DPacketEntityEffect(entityID, pe);
			packetListener.save(pee);
		}

		for(int id : lastEffects) {
			if(!found.contains(id)) {
				S1EPacketRemoveEntityEffect pre = new S1EPacketRemoveEntityEffect(entityID, new PotionEffect(id, 0));
				packetListener.save(pre);
			}
		}

		lastEffects = found;
			 */

            //Inventory Handling
            //#if MC>=10904
            if (playerItems[0] != player(mc).getHeldItem(EnumHand.MAIN_HAND)) {
                playerItems[0] = player(mc).getHeldItem(EnumHand.MAIN_HAND);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.MAINHAND, playerItems[0]));
            }

            if (playerItems[1] != player(mc).getHeldItem(EnumHand.OFF_HAND)) {
                playerItems[1] = player(mc).getHeldItem(EnumHand.OFF_HAND);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.OFFHAND, playerItems[1]));
            }

            if (playerItems[2] != player(mc).getItemStackFromSlot(EntityEquipmentSlot.FEET)) {
                playerItems[2] = player(mc).getItemStackFromSlot(EntityEquipmentSlot.FEET);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.FEET, playerItems[2]));
            }

            if (playerItems[3] != player(mc).getItemStackFromSlot(EntityEquipmentSlot.LEGS)) {
                playerItems[3] = player(mc).getItemStackFromSlot(EntityEquipmentSlot.LEGS);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.LEGS, playerItems[3]));
            }

            if (playerItems[4] != player(mc).getItemStackFromSlot(EntityEquipmentSlot.CHEST)) {
                playerItems[4] = player(mc).getItemStackFromSlot(EntityEquipmentSlot.CHEST);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.CHEST, playerItems[4]));
            }

            if (playerItems[5] != player(mc).getItemStackFromSlot(EntityEquipmentSlot.HEAD)) {
                playerItems[5] = player(mc).getItemStackFromSlot(EntityEquipmentSlot.HEAD);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.HEAD, playerItems[5]));
            }

            for (int i = 0; i < playerInventory.length; i++){
                ItemStack itemStack = player(mc).inventory.mainInventory.get(i);
                if (playerInventory[i] != itemStack) {
                    playerInventory[i] = itemStack;
                    packetListener.save(new SPacketSetSlot(0, i, itemStack)); //TODO decide if gui window 0 is appropriate
                }
            }
            //#else1
            //$$ if(playerItems[0] != mc.thePlayer.getHeldItem()) {
            //$$     playerItems[0] = mc.thePlayer.getHeldItem();
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 0, playerItems[0]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[1] != mc.thePlayer.inventory.armorInventory[0]) {
            //$$     playerItems[1] = mc.thePlayer.inventory.armorInventory[0];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 1, playerItems[1]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[2] != mc.thePlayer.inventory.armorInventory[1]) {
            //$$     playerItems[2] = mc.thePlayer.inventory.armorInventory[1];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 2, playerItems[2]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[3] != mc.thePlayer.inventory.armorInventory[2]) {
            //$$     playerItems[3] = mc.thePlayer.inventory.armorInventory[2];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 3, playerItems[3]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[4] != mc.thePlayer.inventory.armorInventory[3]) {
            //$$     playerItems[4] = mc.thePlayer.inventory.armorInventory[3];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 4, playerItems[4]);
            //$$     packetListener.save(pee);
            //$$ }
            //#endif

            //Leaving Ride

            if((!player(mc).isRiding() && lastRiding != -1) ||
                    (player(mc).isRiding() && lastRiding != getRidingEntity(player(mc)).getEntityId())) {
                if(!player(mc).isRiding()) {
                    lastRiding = -1;
                } else {
                    lastRiding = getRidingEntity(player(mc)).getEntityId();
                }
                //#if MC>=10904
                packetListener.save(new SPacketEntityAttach(e.player, getRidingEntity(e.player)));
                //#else
                //$$ packetListener.save(new S1BPacketEntityAttach(0, e.player, getRidingEntity(e.player)));
                //#endif
            }

            //Sleeping
            if(!player(mc).isPlayerSleeping() && wasSleeping) {
                //#if MC>=10904
                packetListener.save(new SPacketAnimation(e.player, 2));
                //#else
                //$$ packetListener.save(new S0BPacketAnimation(e.player, 2));
                //#endif
                wasSleeping = false;
            }

            //#if MC>=10904
            // Active hand (e.g. eating, drinking, blocking)
            if (player(mc).isHandActive() ^ wasHandActive || player(mc).getActiveHand() != lastActiveHand) {
                wasHandActive = player(mc).isHandActive();
                lastActiveHand = player(mc).getActiveHand();
                EntityDataManager dataManager = new EntityDataManager(null);
                int state = (wasHandActive ? 1 : 0) | (lastActiveHand == EnumHand.OFF_HAND ? 2 : 0);
                dataManager.register(EntityLiving.HAND_STATES, (byte) state);
                packetListener.save(new SPacketEntityMetadata(player(mc).getEntityId(), dataManager, true));
            }
            //#endif

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPickupItem(ItemPickupEvent event) {
        try {
            //#if MC>=11100
            //#if MC>=11200
            packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId(),
                    event.pickedUp.getItem().getMaxStackSize()));
            //#else
            //$$ packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId(),
            //$$         event.pickedUp.getEntityItem().getMaxStackSize()));
            //#endif
            //#else
            //#if MC>=10904
            //$$ packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId()));
            //#else
            //$$ packetListener.save(new S0DPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId()));
            //#endif
            //#endif
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onSleep(PlayerSleepInBedEvent event) {
        try {
            //#if MC>=10904
            if (event.getEntityPlayer() != player(mc)) {
                return;
            }

            packetListener.save(new SPacketUseBed(event.getEntityPlayer(), event.getPos()));
            //#else
            //$$ if (event.entityPlayer != player(mc)) {
            //$$     return;
            //$$ }
            //$$
            //$$ packetListener.save(new S0APacketUseBed(event.entityPlayer,
                    //#if MC>=10800
                    //$$ event.pos
                    //#else
                    //$$ event.x, event.y, event.z
                    //#endif
            //$$ ));
            //#endif

            wasSleeping = true;

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void enterMinecart(MinecartInteractEvent event) {
        try {
            //#if MC>=10904
            if(event.getEntity() != player(mc)) {
                return;
            }

            packetListener.save(new SPacketEntityAttach(event.getPlayer(), event.getMinecart()));

            lastRiding = event.getMinecart().getEntityId();
            //#else
            //$$ if(event.entity != player(mc)) {
            //$$     return;
            //$$ }
            //$$
            //$$ packetListener.save(new S1BPacketEntityAttach(0, event.player, event.minecart));
            //$$
            //$$ lastRiding = event.minecart.getEntityId();
            //#endif
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //#if MC>=10800
    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
    //#else
    //$$ public void onBlockBreakAnim(int breakerId, int x, int y, int z, int progress) {
    //#endif
        EntityPlayer thePlayer = player(mc);
        if (thePlayer != null && breakerId == thePlayer.getEntityId()) {
            //#if MC>=10904
            packetListener.save(new SPacketBlockBreakAnim(breakerId, pos, progress));
            //#else
            //$$ packetListener.save(new S25PacketBlockBreakAnim(breakerId,
                    //#if MC>=10800
                    //$$ pos,
                    //#else
                    //$$ x, y, z,
                    //#endif
            //$$         progress));
            //#endif
        }
    }

    @SubscribeEvent
    public void checkForGamePaused(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server =  mc.getIntegratedServer();
            if (server != null && server.isGamePaused) {
                packetListener.setServerWasPaused();
            }
        }
    }

    public interface RecordingEventSender {
        void setRecordingEventHandler(RecordingEventHandler recordingEventHandler);
        RecordingEventHandler getRecordingEventHandler();
    }
}
