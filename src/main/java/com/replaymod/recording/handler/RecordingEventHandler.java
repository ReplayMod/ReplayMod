package com.replaymod.recording.handler;

import com.replaymod.recording.mixin.IntegratedServerAccessor;
import com.replaymod.recording.packet.PacketListener;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.server.integrated.IntegratedServer;
// FIXME not (yet?) 1.13 import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;

//#if FABRIC>=1
import com.replaymod.core.events.PreRenderCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
//#else
//$$ import net.minecraft.network.play.server.SCollectItemPacket;
//$$ import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
//$$ import net.minecraftforge.event.TickEvent;
//#endif

//#if MC>=11600
//$$ import com.mojang.datafixers.util.Pair;
//$$ import java.util.Collections;
//#endif

//#if MC>=11400
//#else
//$$ import net.minecraft.network.play.server.SPacketUseBed;
//#endif

//#if MC>=10904
import com.replaymod.recording.mixin.EntityLivingBaseAccessor;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.util.Hand;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
//#endif

//#if MC>=10800
import net.minecraft.util.math.BlockPos;
//#else
//$$ import net.minecraft.util.MathHelper;
//#endif

import java.util.Objects;

import static com.replaymod.core.versions.MCVer.*;

public class RecordingEventHandler extends EventRegistrations {

    private final MinecraftClient mc = getMinecraft();
    private final PacketListener packetListener;

    private Double lastX, lastY, lastZ;
    private ItemStack[] playerItems = new ItemStack[6];
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;
    //#if MC>=10904
    private boolean wasHandActive;
    private Hand lastActiveHand;
    //#endif

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    @Override
    public void register() {
        super.register();
        ((RecordingEventSender) mc.worldRenderer).setRecordingEventHandler(this);
    }

    @Override
    public void unregister() {
        super.unregister();
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.worldRenderer);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    //#if MC>=11400
    public void onPacket(Packet<?> packet) {
        packetListener.save(packet);
    }
    //#endif

    public void spawnRecordingPlayer() {
        try {
            ClientPlayerEntity player = mc.player;
            assert player != null;
            packetListener.save(new PlayerSpawnS2CPacket(player));
            //#if MC>=11500
            packetListener.save(new EntityTrackerUpdateS2CPacket(player.getEntityId(), player.getDataTracker(), true));
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
            packetListener.save(new PlaySoundS2CPacket(sound, category, x, y, z, volume, pitch));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void onClientEffect(int type, BlockPos pos, int data) {
        try {
            // Send to all other players in ServerWorldEventHandler#playEvent
            packetListener.save(new WorldEventS2CPacket(type, pos, data, false));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    //#endif

    //#if FABRIC>=1
    { on(PreTickCallback.EVENT, this::onPlayerTick); }
    private void onPlayerTick() {
        if (mc.player == null) return;
    //#else
    //$$ @SubscribeEvent
    //$$ public void onPlayerTick(TickEvent.ClientTickEvent e) {
    //$$     if(e.phase != TickEvent.Phase.START || mc.player == null) return;
    //#endif
        ClientPlayerEntity player = mc.player;
        try {

            boolean force = false;
            if(lastX == null || lastY == null || lastZ == null) {
                force = true;
                lastX = Entity_getX(player);
                lastY = Entity_getY(player);
                lastZ = Entity_getZ(player);
            }

            ticksSinceLastCorrection++;
            if(ticksSinceLastCorrection >= 100) {
                ticksSinceLastCorrection = 0;
                force = true;
            }

            double dx = Entity_getX(player) - lastX;
            double dy = Entity_getY(player) - lastY;
            double dz = Entity_getZ(player) - lastZ;

            lastX = Entity_getX(player);
            lastY = Entity_getY(player);
            lastZ = Entity_getZ(player);

            Packet packet;
            if (force || Math.abs(dx) > 8.0 || Math.abs(dy) > 8.0 || Math.abs(dz) > 8.0) {
                //#if MC>=10800
                packet = new EntityPositionS2CPacket(player);
                //#else
                //$$ // In 1.7.10 the client player entity has its posY at eye height
                //$$ // but for all other entities it's at their feet (as it should be).
                //$$ // So, to correctly position the player, we teleport them to their feet (i.a. directly after spawn).
                //$$ // Note: this leaves the lastY value offset by the eye height but because it's only used for relative
                //$$ //       movement, that doesn't matter.
                //$$ S18PacketEntityTeleport teleportPacket = new S18PacketEntityTeleport(player);
                //$$ packet = new S18PacketEntityTeleport(
                //$$         teleportPacket.func_149451_c(),
                //$$         teleportPacket.func_149449_d(),
                //$$         MathHelper.floor_double(player.boundingBox.minY * 32),
                //$$         teleportPacket.func_149446_f(),
                //$$         teleportPacket.func_149450_g(),
                //$$         teleportPacket.func_149447_h()
                //$$ );
                //#endif
            } else {
                byte newYaw = (byte) ((int) (player.yaw * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (player.pitch * 256.0F / 360.0F));

                //#if MC>=11400
                packet = new EntityS2CPacket.RotateAndMoveRelative(
                //#else
                //$$ packet = new SPacketEntity.S17PacketEntityLookMove(
                //#endif
                        player.getEntityId(),
                        //#if MC>=10904
                        (short) Math.round(dx * 4096), (short) Math.round(dy * 4096), (short) Math.round(dz * 4096),
                        //#else
                        //$$ (byte) Math.round(dx * 32), (byte) Math.round(dy * 32), (byte) Math.round(dz * 32),
                        //#endif
                        newYaw, newPitch
                        //#if MC>=11600
                        //$$ , player.isOnGround()
                        //#else
                        //#if MC>=10800
                        , player.onGround
                        //#endif
                        //#endif
                );
            }

            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int)(player.headYaw * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                packetListener.save(new EntitySetHeadYawS2CPacket(player, (byte) rotationYawHead));
                rotationYawHeadBefore = rotationYawHead;
            }

            packetListener.save(new EntityVelocityUpdateS2CPacket(player.getEntityId(),
                    //#if MC>=11400
                    player.getVelocity()
                    //#else
                    //$$ player.motionX, player.motionY, player.motionZ
                    //#endif
            ));

            //Animation Packets
            //Swing Animation
            if (player.isHandSwinging && player.handSwingTicks == 0) {
                packetListener.save(new EntityAnimationS2CPacket(
                        player,
                        //#if MC>=10904
                        player.preferredHand == Hand.MAIN_HAND ? 0 : 3
                        //#else
                        //$$ 0
                        //#endif
                ));
            }

			/*
        //Potion Effect Handling
		List<Integer> found = new ArrayList<Integer>();
		for(PotionEffect pe : (Collection<PotionEffect>)player.getActivePotionEffects()) {
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
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getEquippedStack(slot);
                if (playerItems[slot.ordinal()] != stack) {
                    playerItems[slot.ordinal()] = stack;
                    //#if MC>=11600
                    //$$ packetListener.save(new EntityEquipmentUpdateS2CPacket(player.getEntityId(), Collections.singletonList(Pair.of(slot, stack))));
                    //#else
                    packetListener.save(new EntityEquipmentUpdateS2CPacket(player.getEntityId(), slot, stack));
                    //#endif
                }
            }
            //#else
            //$$ if(playerItems[0] != mc.thePlayer.getHeldItem()) {
            //$$     playerItems[0] = mc.thePlayer.getHeldItem();
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(player.getEntityId(), 0, playerItems[0]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[1] != mc.thePlayer.inventory.armorInventory[0]) {
            //$$     playerItems[1] = mc.thePlayer.inventory.armorInventory[0];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(player.getEntityId(), 1, playerItems[1]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[2] != mc.thePlayer.inventory.armorInventory[1]) {
            //$$     playerItems[2] = mc.thePlayer.inventory.armorInventory[1];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(player.getEntityId(), 2, playerItems[2]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[3] != mc.thePlayer.inventory.armorInventory[2]) {
            //$$     playerItems[3] = mc.thePlayer.inventory.armorInventory[2];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(player.getEntityId(), 3, playerItems[3]);
            //$$     packetListener.save(pee);
            //$$ }
            //$$
            //$$ if(playerItems[4] != mc.thePlayer.inventory.armorInventory[3]) {
            //$$     playerItems[4] = mc.thePlayer.inventory.armorInventory[3];
            //$$     S04PacketEntityEquipment pee = new S04PacketEntityEquipment(player.getEntityId(), 4, playerItems[4]);
            //$$     packetListener.save(pee);
            //$$ }
            //#endif

            //Leaving Ride

            if((!player.isRiding() && lastRiding != -1) ||
                    (player.isRiding() && lastRiding != getRiddenEntity(player).getEntityId())) {
                if(!player.isRiding()) {
                    lastRiding = -1;
                } else {
                    lastRiding = getRiddenEntity(player).getEntityId();
                }
                packetListener.save(new EntityAttachS2CPacket(
                        //#if MC<10904
                        //$$ 0,
                        //#endif
                        player, getRiddenEntity(player)
                ));
            }

            //Sleeping
            if(!player.isSleeping() && wasSleeping) {
                packetListener.save(new EntityAnimationS2CPacket(player, 2));
                wasSleeping = false;
            }

            //#if MC>=10904
            // Active hand (e.g. eating, drinking, blocking)
            if (player.isUsingItem() ^ wasHandActive || player.getActiveHand() != lastActiveHand) {
                wasHandActive = player.isUsingItem();
                lastActiveHand = player.getActiveHand();
                DataTracker dataManager = new DataTracker(null);
                int state = (wasHandActive ? 1 : 0) | (lastActiveHand == Hand.OFF_HAND ? 2 : 0);
                dataManager.startTracking(EntityLivingBaseAccessor.getLivingFlags(), (byte) state);
                packetListener.save(new EntityTrackerUpdateS2CPacket(player.getEntityId(), dataManager, true));
            }
            //#endif

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    //#if FABRIC>=1
    // FIXME fabric
    //#else
    //$$ @SubscribeEvent
    //$$ public void onPickupItem(ItemPickupEvent event) {
    //$$     try {
            //#if MC>=11100
            //#if MC>=11200
            //#if MC>=11400
            //$$ ItemStack stack = event.getStack();
            //$$ packetListener.save(new SCollectItemPacket(
            //$$         event.getOriginalEntity().getEntityId(),
            //$$         event.getPlayer().getEntityId(),
            //$$         event.getStack().getCount()
            //$$ ));
            //#else
            //$$ packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId(),
            //$$         event.pickedUp.getItem().getMaxStackSize()));
            //#endif
            //#else
            //$$ packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId(),
            //$$         event.pickedUp.getEntityItem().getMaxStackSize()));
            //#endif
            //#else
            //$$ packetListener.save(new SPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId()));
            //#endif
    //$$     } catch(Exception e) {
    //$$         e.printStackTrace();
    //$$     }
    //$$ }
    //#endif

    //#if MC>=11400
    // FIXME fabric
    //#else
    //$$ @SubscribeEvent
    //$$ public void onSleep(PlayerSleepInBedEvent event) {
    //$$     try {
            //#if MC>=10904
            //$$ if (event.getEntityPlayer() != mc.player) {
            //$$     return;
            //$$ }
            //$$
            //$$ packetListener.save(new SPacketUseBed(event.getEntityPlayer(), event.getPos()));
            //#else
            //$$ if (event.entityPlayer != mc.thePlayer) {
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
    //$$
    //$$         wasSleeping = true;
    //$$
    //$$     } catch(Exception e) {
    //$$         e.printStackTrace();
    //$$     }
    //$$ }
    //#endif

    /* FIXME event not (yet?) on 1.13
    @SubscribeEvent
    public void enterMinecart(MinecartInteractEvent event) {
        try {
            //#if MC>=10904
            if(event.getEntity() != mc.player) {
                return;
            }

            packetListener.save(new SPacketEntityAttach(event.getPlayer(), event.getMinecart()));

            lastRiding = event.getMinecart().getEntityId();
            //#else
            //$$ if(event.entity != mc.thePlayer) {
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
    */

    //#if MC>=10800
    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
    //#else
    //$$ public void onBlockBreakAnim(int breakerId, int x, int y, int z, int progress) {
    //#endif
        PlayerEntity thePlayer = mc.player;
        if (thePlayer != null && breakerId == thePlayer.getEntityId()) {
            packetListener.save(new BlockBreakingProgressS2CPacket(breakerId,
                    //#if MC>=10800
                    pos,
                    //#else
                    //$$ x, y, z,
                    //#endif
                    progress));
        }
    }

    //#if FABRIC>=1
    { on(PreRenderCallback.EVENT, this::checkForGamePaused); }
    private void checkForGamePaused() {
    //#else
    //$$ @SubscribeEvent
    //$$ public void checkForGamePaused(TickEvent.RenderTickEvent event) {
    //$$     if (event.phase != TickEvent.Phase.START) return;
    //#endif
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server =  mc.getServer();
            if (server != null && ((IntegratedServerAccessor) server).isGamePaused()) {
                packetListener.setServerWasPaused();
            }
        }
    }

    public interface RecordingEventSender {
        void setRecordingEventHandler(RecordingEventHandler recordingEventHandler);
        RecordingEventHandler getRecordingEventHandler();
    }
}
