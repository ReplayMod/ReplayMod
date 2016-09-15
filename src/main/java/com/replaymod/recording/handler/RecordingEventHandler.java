package com.replaymod.recording.handler;

import com.replaymod.recording.packet.PacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

import java.util.Objects;

public class RecordingEventHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PacketListener packetListener;

    private Double lastX, lastY, lastZ;
    private ItemStack[] playerItems = new ItemStack[6];
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;
    private boolean wasHandActive;
    private EnumHand lastActiveHand;

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        ((RecordingEventSender) mc.renderGlobal).setRecordingEventHandler(this);
    }

    public void unregister() {
        MinecraftForge.EVENT_BUS.unregister(this);
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.renderGlobal);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    public void onPlayerJoin() {
        try {
            packetListener.save(new SPacketSpawnPlayer(mc.thePlayer));
        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onPlayerRespawn() {
        try {
            packetListener.save(new SPacketSpawnPlayer(mc.thePlayer));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent e) {
        try {
            if(e.player != mc.thePlayer) return;

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
                packet = new SPacketEntityTeleport(e.player);
            } else {
                byte newYaw = (byte) ((int) (e.player.rotationYaw * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (e.player.rotationPitch * 256.0F / 360.0F));

                packet = new SPacketEntity.S17PacketEntityLookMove(e.player.getEntityId(),
                        (short) Math.round(dx * 4096), (short) Math.round(dy * 4096), (short) Math.round(dz * 4096),
                        newYaw, newPitch, e.player.onGround);
            }

            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int)(e.player.rotationYawHead * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                packetListener.save(new SPacketEntityHeadLook(e.player, (byte) rotationYawHead));
                rotationYawHeadBefore = rotationYawHead;
            }

            packetListener.save(new SPacketEntityVelocity(e.player.getEntityId(), e.player.motionX, e.player.motionY, e.player.motionZ));

            //Animation Packets
            //Swing Animation
            if (e.player.isSwingInProgress && e.player.swingProgressInt == -1) {
                packetListener.save(new SPacketAnimation(e.player, e.player.swingingHand == EnumHand.MAIN_HAND ? 0 : 3));
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
            if (playerItems[0] != mc.thePlayer.getHeldItem(EnumHand.MAIN_HAND)) {
                playerItems[0] = mc.thePlayer.getHeldItem(EnumHand.MAIN_HAND);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.MAINHAND, playerItems[0]));
            }

            if (playerItems[1] != mc.thePlayer.getHeldItem(EnumHand.OFF_HAND)) {
                playerItems[1] = mc.thePlayer.getHeldItem(EnumHand.OFF_HAND);
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.OFFHAND, playerItems[1]));
            }

            if (playerItems[2] != mc.thePlayer.inventory.armorInventory[0]) {
                playerItems[2] = mc.thePlayer.inventory.armorInventory[0];
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.FEET, playerItems[2]));
            }

            if (playerItems[3] != mc.thePlayer.inventory.armorInventory[1]) {
                playerItems[3] = mc.thePlayer.inventory.armorInventory[1];
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.LEGS, playerItems[3]));
            }

            if (playerItems[4] != mc.thePlayer.inventory.armorInventory[2]) {
                playerItems[4] = mc.thePlayer.inventory.armorInventory[2];
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.CHEST, playerItems[4]));
            }

            if (playerItems[5] != mc.thePlayer.inventory.armorInventory[3]) {
                playerItems[5] = mc.thePlayer.inventory.armorInventory[3];
                packetListener.save(new SPacketEntityEquipment(e.player.getEntityId(), EntityEquipmentSlot.HEAD, playerItems[5]));
            }

            //Leaving Ride

            if((!mc.thePlayer.isRiding() && lastRiding != -1) ||
                    (mc.thePlayer.isRiding() && lastRiding != mc.thePlayer.getRidingEntity().getEntityId())) {
                if(!mc.thePlayer.isRiding()) {
                    lastRiding = -1;
                } else {
                    lastRiding = mc.thePlayer.getRidingEntity().getEntityId();
                }
                packetListener.save(new SPacketEntityAttach(e.player, e.player.getRidingEntity()));
            }

            //Sleeping
            if(!mc.thePlayer.isPlayerSleeping() && wasSleeping) {
                packetListener.save(new SPacketAnimation(e.player, 2));
                wasSleeping = false;
            }

            // Active hand (e.g. eating, drinking, blocking)
            if (mc.thePlayer.isHandActive() ^ wasHandActive || mc.thePlayer.getActiveHand() != lastActiveHand) {
                wasHandActive = mc.thePlayer.isHandActive();
                lastActiveHand = mc.thePlayer.getActiveHand();
                EntityDataManager dataManager = new EntityDataManager(null);
                int state = (wasHandActive ? 1 : 0) | (lastActiveHand == EnumHand.OFF_HAND ? 2 : 0);
                dataManager.register(EntityLiving.HAND_STATES, (byte) state);
                packetListener.save(new SPacketEntityMetadata(mc.thePlayer.getEntityId(), dataManager, true));
            }

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPickupItem(ItemPickupEvent event) {
        try {
            packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId()));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onSleep(PlayerSleepInBedEvent event) {
        try {
            if (event.getEntityPlayer() != mc.thePlayer) {
                return;
            }

            packetListener.save(new SPacketUseBed(event.getEntityPlayer(), event.getPos()));

            wasSleeping = true;

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void enterMinecart(MinecartInteractEvent event) {
        try {
            if(event.getEntity() != mc.thePlayer) {
                return;
            }

            packetListener.save(new SPacketEntityAttach(event.getPlayer(), event.getMinecart()));

            lastRiding = event.getMinecart().getEntityId();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        EntityPlayer thePlayer = mc.thePlayer;
        if (thePlayer != null && breakerId == thePlayer.getEntityId()) {
            packetListener.save(new SPacketBlockBreakAnim(breakerId, pos, progress));
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
