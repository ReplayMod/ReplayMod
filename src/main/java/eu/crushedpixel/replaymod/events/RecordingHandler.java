package eu.crushedpixel.replaymod.events;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.minecart.MinecartEvent;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;

public class RecordingHandler {

	private Minecraft mc = Minecraft.getMinecraft();

	public static final int entityID = Integer.MIN_VALUE+9001;

	@SubscribeEvent
	public void onPlayerJoin(EntityJoinWorldEvent e) {
		try {
			if(e.entity != mc.thePlayer) return;
			if(!ConnectionEventHandler.isRecording()) return;

			EntityPlayer player = (EntityPlayer)e.entity;

			S38PacketPlayerListItem ppli = new S38PacketPlayerListItem();
			ByteBuf buf = Unpooled.buffer();
			PacketBuffer pbuf = new PacketBuffer(buf);

			pbuf.writeEnumValue(Action.ADD_PLAYER);
			pbuf.writeVarIntToBuffer(1);
			pbuf.writeUuid(e.entity.getUniqueID());

			pbuf.writeString(player.getName());
			pbuf.writeVarIntToBuffer(0);
			pbuf.writeVarIntToBuffer(mc.playerController.getCurrentGameType().getID());
			pbuf.writeVarIntToBuffer(0);

			pbuf.writeBoolean(true);
			pbuf.writeChatComponent(player.getDisplayName());

			ppli.readPacketData(pbuf);
			ConnectionEventHandler.insertPacket(ppli);

			ConnectionEventHandler.insertPacket(spawnPlayer(mc.thePlayer));
		} catch(Exception e1) {
			e1.printStackTrace();
		}
	}

	private S0CPacketSpawnPlayer spawnPlayer(EntityPlayer player) {
		try {
			S0CPacketSpawnPlayer packet = new S0CPacketSpawnPlayer();

			ByteBuf bb = Unpooled.buffer();
			PacketBuffer pb = new PacketBuffer(bb);

			pb.writeVarIntToBuffer(entityID);
			pb.writeUuid(player.getUUID(player.getGameProfile()));

			pb.writeInt(MathHelper.floor_double(player.posX * 32.0D));
			pb.writeInt(MathHelper.floor_double(player.posY * 32.0D));
			pb.writeInt(MathHelper.floor_double(player.posZ * 32.0D));
			pb.writeByte((byte)((int)(player.rotationYaw * 256.0F / 360.0F)));
			pb.writeByte((byte)((int)(player.rotationPitch * 256.0F / 360.0F)));

			ItemStack itemstack = player.inventory.getCurrentItem();
			pb.writeInt(itemstack == null ? 0 : Item.getIdFromItem(itemstack.getItem()));
			player.getDataWatcher().writeTo(pb);

			packet.readPacketData(pb);

			return packet;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Double lastX = null, lastY = null, lastZ = null;
	private List<Integer> lastEffects = new ArrayList<Integer>();

	private ItemStack[] playerItems = new ItemStack[5];

	public void resetVars() {
		lastX = lastY = lastZ = null;
		lastEffects = new ArrayList<Integer>();
		playerItems = new ItemStack[5];
	}

	private int ticksSinceLastCorrection = 0;

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		try {
			if(e.player != mc.thePlayer) return;
			if(!ConnectionEventHandler.isRecording()) return;

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

			Packet packet = null;
			if(force || Math.abs(dx) > 4.0 || Math.abs(dy) > 4.0 || Math.abs(dz) > 4.0) {
				int x = MathHelper.floor_double(e.player.posX * 32.0D);
				int y = MathHelper.floor_double(e.player.posY * 32.0D);
				int z = MathHelper.floor_double(e.player.posZ * 32.0D);
				byte yaw = (byte)((int)(e.player.rotationYaw * 256.0F / 360.0F));
				byte pitch = (byte)((int)(e.player.rotationPitch * 256.0F / 360.0F));
				packet = new S18PacketEntityTeleport(entityID, x, y, z, yaw, pitch, e.player.onGround);
			} else {
				byte oldYaw = (byte)((int)(e.player.prevRotationYaw * 256.0F / 360.0F));
				byte newYaw = (byte)((int)(e.player.rotationYaw * 256.0F / 360.0F));
				byte oldPitch = (byte)((int)(e.player.prevRotationPitch * 256.0F / 360.0F));
				byte newPitch = (byte)((int)(e.player.rotationPitch * 256.0F / 360.0F));

				byte dPitch = (byte)(newPitch-oldPitch);
				byte dYaw = (byte)(newYaw-oldYaw);

				packet = new S17PacketEntityLookMove(entityID, 
						(byte)Math.round(dx*32), (byte)Math.round(dy*32), (byte)Math.round(dz*32), 
						newYaw, newPitch, e.player.onGround);
			}

			ConnectionEventHandler.insertPacket(packet);

			//HEAD POS
			S19PacketEntityHeadLook head = new S19PacketEntityHeadLook();
			ByteBuf bb1 = Unpooled.buffer();
			PacketBuffer pb1 = new PacketBuffer(bb1);

			pb1.writeVarIntToBuffer(entityID);
			pb1.writeByte(((int)(e.player.rotationYawHead * 256.0F / 360.0F)));

			head.readPacketData(pb1);

			ConnectionEventHandler.insertPacket(head);

			S12PacketEntityVelocity vel = new S12PacketEntityVelocity(entityID, e.player.motionX, e.player.motionY, e.player.motionZ);
			ConnectionEventHandler.insertPacket(vel);

			//Animation Packets
			//Swing Animation
			if(e.player.swingProgressInt == 1) {
				S0BPacketAnimation pac = new S0BPacketAnimation();

				ByteBuf bb = Unpooled.buffer();
				PacketBuffer pb = new PacketBuffer(bb);

				pb.writeVarIntToBuffer(entityID);
				pb.writeByte(0);

				pac.readPacketData(pb);

				ConnectionEventHandler.insertPacket(pac);
			}

			/*
		//Potion Effect Handling
		List<Integer> found = new ArrayList<Integer>();
		for(PotionEffect pe : (Collection<PotionEffect>)e.player.getActivePotionEffects()) {
			found.add(pe.getPotionID());
			if(lastEffects.contains(found)) continue;
			S1DPacketEntityEffect pee = new S1DPacketEntityEffect(entityID, pe);
			ConnectionEventHandler.insertPacket(pee);
		}

		for(int id : lastEffects) {
			if(!found.contains(id)) {
				S1EPacketRemoveEntityEffect pre = new S1EPacketRemoveEntityEffect(entityID, new PotionEffect(id, 0));
				ConnectionEventHandler.insertPacket(pre);
			}
		}

		lastEffects = found;
			 */

			//Inventory Handling
			if(playerItems[0] != mc.thePlayer.getHeldItem()) {
				playerItems[0] = mc.thePlayer.getHeldItem();
				S04PacketEntityEquipment pee = new S04PacketEntityEquipment(entityID, 0, playerItems[0]);
				ConnectionEventHandler.insertPacket(pee);
			}

			if(playerItems[1] != mc.thePlayer.inventory.armorInventory[0]) {
				playerItems[1] = mc.thePlayer.inventory.armorInventory[0];
				S04PacketEntityEquipment pee = new S04PacketEntityEquipment(entityID, 1, playerItems[1]);
				ConnectionEventHandler.insertPacket(pee);
			}

			if(playerItems[2] != mc.thePlayer.inventory.armorInventory[1]) {
				playerItems[2] = mc.thePlayer.inventory.armorInventory[1];
				S04PacketEntityEquipment pee = new S04PacketEntityEquipment(entityID, 2, playerItems[2]);
				ConnectionEventHandler.insertPacket(pee);
			}

			if(playerItems[3] != mc.thePlayer.inventory.armorInventory[2]) {
				playerItems[3] = mc.thePlayer.inventory.armorInventory[2];
				S04PacketEntityEquipment pee = new S04PacketEntityEquipment(entityID, 3, playerItems[3]);
				ConnectionEventHandler.insertPacket(pee);
			}

			if(playerItems[4] != mc.thePlayer.inventory.armorInventory[3]) {
				playerItems[4] = mc.thePlayer.inventory.armorInventory[3];
				S04PacketEntityEquipment pee = new S04PacketEntityEquipment(entityID, 4, playerItems[4]);
				ConnectionEventHandler.insertPacket(pee);
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

				pbuf.writeInt(entityID);
				pbuf.writeInt(lastRiding);
				pbuf.writeBoolean(false);

				pea.readPacketData(pbuf);

				ConnectionEventHandler.insertPacket(pea);
			}
			
			//Sleeping
			if(!mc.thePlayer.isPlayerSleeping() && wasSleeping) {
				S0BPacketAnimation pac = new S0BPacketAnimation();

				ByteBuf bb = Unpooled.buffer();
				PacketBuffer pb = new PacketBuffer(bb);

				pb.writeVarIntToBuffer(entityID);
				pb.writeByte(2);

				pac.readPacketData(pb);

				ConnectionEventHandler.insertPacket(pac);
				
				wasSleeping = false;
			}

		} catch(Exception e1) {
			e1.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onPickupItem(ItemPickupEvent event) {
		try {
			ConnectionEventHandler.insertPacket(new S0DPacketCollectItem(event.pickedUp.getEntityId(), entityID));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onRespawn(PlayerRespawnEvent event) {
		try {
			//destroy entity, then respawn
			ConnectionEventHandler.insertPacket(new S13PacketDestroyEntities(entityID));
			ConnectionEventHandler.insertPacket(spawnPlayer(mc.thePlayer));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onHurt(LivingHurtEvent event) {
		try {
			if(event.entity.getEntityId() != mc.thePlayer.getEntityId()) {
				return;
			};

			S19PacketEntityStatus packet = new S19PacketEntityStatus();

			ByteBuf buf = Unpooled.buffer();
			PacketBuffer pbuf = new PacketBuffer(buf);

			pbuf.writeInt(entityID);
			pbuf.writeByte(2);

			packet.readPacketData(pbuf);

			ConnectionEventHandler.insertPacket(packet);

			//Damage Animation
			S0BPacketAnimation pac = new S0BPacketAnimation();

			ByteBuf bb = Unpooled.buffer();
			PacketBuffer pb = new PacketBuffer(bb);

			pb.writeVarIntToBuffer(entityID);
			pb.writeByte(1);

			pac.readPacketData(pb);

			ConnectionEventHandler.insertPacket(pac);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		try {
			if(event.entity.getEntityId() != mc.thePlayer.getEntityId()) {
				return;
			};

			S19PacketEntityStatus packet = new S19PacketEntityStatus();

			ByteBuf buf = Unpooled.buffer();
			PacketBuffer pbuf = new PacketBuffer(buf);

			pbuf.writeInt(entityID);
			pbuf.writeByte(3);

			packet.readPacketData(pbuf);

			ConnectionEventHandler.insertPacket(packet);
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

			pb.writeVarIntToBuffer(entityID);
			pb.writeByte(3);

			packet.readPacketData(pb);

			ConnectionEventHandler.insertPacket(packet);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean wasSleeping = false;

	@SubscribeEvent
	public void onSleep(PlayerSleepInBedEvent event) {
		try {

			if(event.entityPlayer != mc.thePlayer) {
				return;
			};

			System.out.println(event.getResult());
			S0APacketUseBed pub = new S0APacketUseBed();

			ByteBuf buf = Unpooled.buffer();
			PacketBuffer pbuf = new PacketBuffer(buf);

			pbuf.writeVarIntToBuffer(entityID);
			pbuf.writeBlockPos(event.pos);

			pub.readPacketData(pbuf);

			ConnectionEventHandler.insertPacket(pub);

			wasSleeping = true;

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private int lastRiding = -1;

	@SubscribeEvent
	public void enterMinecart(MinecartInteractEvent event) {
		try {
			if(event.player != mc.thePlayer) {
				return;
			};

			S1BPacketEntityAttach pea = new S1BPacketEntityAttach();

			ByteBuf buf = Unpooled.buffer();
			PacketBuffer pbuf = new PacketBuffer(buf);

			pbuf.writeInt(entityID);
			pbuf.writeInt(event.minecart.getEntityId());
			pbuf.writeBoolean(false);

			pea.readPacketData(pbuf);

			ConnectionEventHandler.insertPacket(pea);

			lastRiding = event.minecart.getEntityId();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
