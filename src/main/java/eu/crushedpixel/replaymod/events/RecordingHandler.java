package eu.crushedpixel.replaymod.events;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;

public class RecordingHandler {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private int entityID = Integer.MAX_VALUE;
	
	@SubscribeEvent
	public void onPlayerJoin(EntityJoinWorldEvent e) throws IOException {
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
		
		//Packet packet = new S0CPacketSpawnPlayer((EntityPlayer)e.entity);
		Packet packet = new S0CPacketSpawnPlayer();
		
		ByteBuf bb = Unpooled.buffer();
		PacketBuffer pb = new PacketBuffer(bb);
		
		pb.writeVarIntToBuffer(entityID);
		//pb.writeUuid((e.entity).getUniqueID());
		pb.writeUuid(e.entity.getUniqueID());
		
		pb.writeInt(MathHelper.floor_double(e.entity.posX * 32.0D));
		pb.writeInt(MathHelper.floor_double(e.entity.posY * 32.0D));
		pb.writeInt(MathHelper.floor_double(e.entity.posZ * 32.0D));
		pb.writeByte((byte)((int)(((EntityPlayer)e.entity).rotationYaw * 256.0F / 360.0F)));
		pb.writeByte((byte)((int)(((EntityPlayer)e.entity).rotationPitch * 256.0F / 360.0F)));
		
		ItemStack itemstack = ((EntityPlayer)e.entity).inventory.getCurrentItem();
		pb.writeInt(itemstack == null ? 0 : Item.getIdFromItem(itemstack.getItem()));
		((EntityPlayer)e.entity).getDataWatcher().writeTo(pb);
		
		packet.readPacketData(pb);
		
		ConnectionEventHandler.insertPacket(packet);
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		if(e.player != mc.thePlayer) return;
		if(!ConnectionEventHandler.isRecording()) return;
		
		double dx = e.player.prevPosX - e.player.posX;
		double dy = e.player.prevPosY - e.player.posY;
		double dz = e.player.prevPosZ - e.player.posZ;
		
		Packet packet;
		if(Math.abs(dx) > 4.0 || Math.abs(dy) > 4.0 || Math.abs(dz) > 4.0) {
			packet = new S18PacketEntityTeleport(e.player);
		} else {
			byte oldYaw = (byte)((int)(e.player.prevRotationYaw * 256.0F / 360.0F));
			byte newYaw = (byte)((int)(e.player.rotationYaw * 256.0F / 360.0F));
			byte oldPitch = (byte)((int)(e.player.prevRotationPitch * 256.0F / 360.0F));
			byte newPitch = (byte)((int)(e.player.rotationPitch * 256.0F / 360.0F));
			
			byte dPitch = (byte)(newPitch-oldPitch);
			byte dYaw = (byte)(newYaw-oldYaw);
			
			packet = new S17PacketEntityLookMove(entityID, 
					(byte)Math.round(dx*32), (byte)Math.round(dy*32), (byte)Math.round(dz*32), 
					dYaw, dPitch, e.player.onGround);
		}
		
		ConnectionEventHandler.insertPacket(packet);
	}
}
