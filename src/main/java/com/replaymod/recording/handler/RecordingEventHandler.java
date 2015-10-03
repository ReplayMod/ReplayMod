package com.replaymod.recording.handler;

import com.replaymod.recording.packet.PacketListener;
import eu.crushedpixel.replaymod.utils.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.*;
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class RecordingEventHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PacketListener packetListener;

    private Double lastX, lastY, lastZ;
    private ItemStack[] playerItems = new ItemStack[5];
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    public void register() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        ((RecordingEventSender) mc.renderGlobal).setRecordingEventHandler(this);
    }

    public void unregister() {
        FMLCommonHandler.instance().bus().unregister(this);
        MinecraftForge.EVENT_BUS.unregister(this);
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.renderGlobal);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    public void onPlayerJoin() {
        try {
            packetListener.save(spawnPlayer(mc.thePlayer));
        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onPlayerRespawn() {
        try {
            packetListener.save(spawnPlayer(mc.thePlayer));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private S0CPacketSpawnPlayer spawnPlayer(EntityPlayer player) {
        try {
            S0CPacketSpawnPlayer packet = new S0CPacketSpawnPlayer();

            ByteBuf bb = Unpooled.buffer();
            PacketBuffer pb = new PacketBuffer(bb);

            pb.writeVarIntToBuffer(player.getEntityId());
            pb.writeUuid(EntityPlayer.getUUID(player.getGameProfile()));

            pb.writeInt(MathHelper.floor_double(player.posX * 32.0D));
            pb.writeInt(MathHelper.floor_double(player.posY * 32.0D));
            pb.writeInt(MathHelper.floor_double(player.posZ * 32.0D));
            pb.writeByte((byte) ((int) (player.rotationYaw * 256.0F / 360.0F)));
            pb.writeByte((byte) ((int) (player.rotationPitch * 256.0F / 360.0F)));

            ItemStack itemstack = player.inventory.getCurrentItem();
            pb.writeShort(itemstack == null ? 0 : Item.getIdFromItem(itemstack.getItem()));

            player.getDataWatcher().writeTo(pb);

            packet.readPacketData(pb);

            return packet;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
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
            if(force || Math.abs(dx) > 4.0 || Math.abs(dy) > 4.0 || Math.abs(dz) > 4.0) {
                int x = MathHelper.floor_double(e.player.posX * 32.0D);
                int y = MathHelper.floor_double(e.player.posY * 32.0D);
                int z = MathHelper.floor_double(e.player.posZ * 32.0D);
                byte yaw = (byte) ((int) (e.player.rotationYaw * 256.0F / 360.0F));
                byte pitch = (byte) ((int) (e.player.rotationPitch * 256.0F / 360.0F));
                packet = new S18PacketEntityTeleport(e.player.getEntityId(), x, y, z, yaw, pitch, e.player.onGround);
            } else {
                byte newYaw = (byte) ((int) (e.player.rotationYaw * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (e.player.rotationPitch * 256.0F / 360.0F));

                packet = new S17PacketEntityLookMove(e.player.getEntityId(),
                        (byte) Math.round(dx * 32), (byte) Math.round(dy * 32), (byte) Math.round(dz * 32),
                        newYaw, newPitch, e.player.onGround);
            }

            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int)(e.player.rotationYawHead * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                S19PacketEntityHeadLook head = new S19PacketEntityHeadLook();
                ByteBuf bb1 = Unpooled.buffer();
                PacketBuffer pb1 = new PacketBuffer(bb1);

                pb1.writeVarIntToBuffer(e.player.getEntityId());
                pb1.writeByte(rotationYawHead);

                head.readPacketData(pb1);

                packetListener.save(head);

                rotationYawHeadBefore = rotationYawHead;
            }

            S12PacketEntityVelocity vel = new S12PacketEntityVelocity(e.player.getEntityId(), e.player.motionX, e.player.motionY, e.player.motionZ);
            packetListener.save(vel);

            //Animation Packets
            //Swing Animation
            if(e.player.swingProgressInt == 1) {
                S0BPacketAnimation pac = new S0BPacketAnimation();

                ByteBuf bb = Unpooled.buffer();
                PacketBuffer pb = new PacketBuffer(bb);

                pb.writeVarIntToBuffer(e.player.getEntityId());
                pb.writeByte(0);

                pac.readPacketData(pb);

                packetListener.save(pac);
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
            if(playerItems[0] != mc.thePlayer.getHeldItem()) {
                playerItems[0] = mc.thePlayer.getHeldItem();
                S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 0, playerItems[0]);
                packetListener.save(pee);
            }

            if(playerItems[1] != mc.thePlayer.inventory.armorInventory[0]) {
                playerItems[1] = mc.thePlayer.inventory.armorInventory[0];
                S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 1, playerItems[1]);
                packetListener.save(pee);
            }

            if(playerItems[2] != mc.thePlayer.inventory.armorInventory[1]) {
                playerItems[2] = mc.thePlayer.inventory.armorInventory[1];
                S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 2, playerItems[2]);
                packetListener.save(pee);
            }

            if(playerItems[3] != mc.thePlayer.inventory.armorInventory[2]) {
                playerItems[3] = mc.thePlayer.inventory.armorInventory[2];
                S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 3, playerItems[3]);
                packetListener.save(pee);
            }

            if(playerItems[4] != mc.thePlayer.inventory.armorInventory[3]) {
                playerItems[4] = mc.thePlayer.inventory.armorInventory[3];
                S04PacketEntityEquipment pee = new S04PacketEntityEquipment(e.player.getEntityId(), 4, playerItems[4]);
                packetListener.save(pee);
            }

            //Leaving Ride

            if((!mc.thePlayer.isRiding() && lastRiding != -1) ||
                    (mc.thePlayer.isRiding() && lastRiding != mc.thePlayer.ridingEntity.getEntityId())) {
                if(!mc.thePlayer.isRiding()) {
                    lastRiding = -1;
                } else {
                    lastRiding = mc.thePlayer.ridingEntity.getEntityId();
                }

                S1BPacketEntityAttach pea = new S1BPacketEntityAttach();

                ByteBuf buf = Unpooled.buffer();
                PacketBuffer pbuf = new PacketBuffer(buf);

                pbuf.writeInt(e.player.getEntityId());
                pbuf.writeInt(lastRiding);
                pbuf.writeBoolean(false);

                pea.readPacketData(pbuf);

                packetListener.save(pea);
            }

            //Sleeping
            if(!mc.thePlayer.isPlayerSleeping() && wasSleeping) {
                S0BPacketAnimation pac = new S0BPacketAnimation();

                ByteBuf bb = Unpooled.buffer();
                PacketBuffer pb = new PacketBuffer(bb);

                pb.writeVarIntToBuffer(e.player.getEntityId());
                pb.writeByte(2);

                pac.readPacketData(pb);

                packetListener.save(pac);

                wasSleeping = false;
            }

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPickupItem(ItemPickupEvent event) {
        try {
            packetListener.save(new S0DPacketCollectItem(event.pickedUp.getEntityId(), event.player.getEntityId()));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onStartEating(PlayerUseItemEvent.Start event) {
        try {
            if(!event.entityPlayer.isEating()) return;
            S0BPacketAnimation packet = new S0BPacketAnimation();

            ByteBuf bb = Unpooled.buffer();
            PacketBuffer pb = new PacketBuffer(bb);

            pb.writeVarIntToBuffer(event.entityPlayer.getEntityId());
            pb.writeByte(3);

            packet.readPacketData(pb);

            packetListener.save(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onSleep(PlayerSleepInBedEvent event) {
        try {
            if(event.entityPlayer != mc.thePlayer) {
                return;
            }

            S0APacketUseBed pub = new S0APacketUseBed();

            ByteBuf buf = Unpooled.buffer();
            PacketBuffer pbuf = new PacketBuffer(buf);

            pbuf.writeVarIntToBuffer(event.entityPlayer.getEntityId());
            pbuf.writeBlockPos(event.pos);

            pub.readPacketData(pbuf);

            packetListener.save(pub);

            wasSleeping = true;

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void enterMinecart(MinecartInteractEvent event) {
        try {
            if(event.player != mc.thePlayer) {
                return;
            }

            S1BPacketEntityAttach pea = new S1BPacketEntityAttach();

            ByteBuf buf = Unpooled.buffer();
            PacketBuffer pbuf = new PacketBuffer(buf);

            pbuf.writeInt(event.player.getEntityId());
            pbuf.writeInt(event.minecart.getEntityId());
            pbuf.writeBoolean(false);

            pea.readPacketData(pbuf);

            packetListener.save(pea);

            lastRiding = event.minecart.getEntityId();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        EntityPlayer thePlayer = mc.thePlayer;
        if (thePlayer != null && breakerId == thePlayer.getEntityId()) {
            packetListener.save(new S25PacketBlockBreakAnim(breakerId, pos, progress));
        }
    }

    @SubscribeEvent
    public void checkForGamePaused(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server =  mc.getIntegratedServer();
            if (server != null && server.isGamePaused) {
                packetListener.serverWasPaused = true;
            }
        }
    }

    public interface RecordingEventSender {
        void setRecordingEventHandler(RecordingEventHandler recordingEventHandler);
        RecordingEventHandler getRecordingEventHandler();
    }
}
