package eu.crushedpixel.replaymod.recording;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;

public class PacketListener extends DataListener {

	public PacketListener(File file, String name, String worldName, long startTime, boolean singleplayer) throws FileNotFoundException {
		super(file, name, worldName, startTime, singleplayer);
	}

	private static final Minecraft mc = Minecraft.getMinecraft();

	private ChannelHandlerContext context = null;

	public void saveOnly(Packet packet) {
		try {
			if(packet instanceof S0CPacketSpawnPlayer) {
				UUID uuid = ((S0CPacketSpawnPlayer)packet).func_179819_c();
				players.add(uuid.toString());
			}
			
			PacketData pd = getPacketData(context, packet);
			writeData(pd);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(ctx == null) {
			if(context == null) {
				return;
			} else {
				ctx = context;
			}
		}
		this.context = ctx;
		if(!alive) {
			super.channelRead(ctx, msg);
			return;
		}
		if(msg instanceof Packet) {
			try {
				Packet packet = (Packet)msg;

				if(packet instanceof S0DPacketCollectItem) {
					if(mc.thePlayer != null || 
							((S0DPacketCollectItem)packet).func_149353_d() == mc.thePlayer.getEntityId()) {
						super.channelRead(ctx, msg);
						return;
					}
				}
				
				if(packet instanceof S0CPacketSpawnPlayer) {
					UUID uuid = ((S0CPacketSpawnPlayer)packet).func_179819_c();
					players.add(uuid.toString());
				}

				PacketData pd = getPacketData(ctx, packet);
				writeData(pd);
			} catch(Exception e) {
				e.printStackTrace();
			}

		}

		super.channelRead(ctx, msg);
	}

	private void writeData(PacketData pd) {
		dataWriter.writeData(pd);
		lastSentPacket = pd.getTimestamp();
	}
	
	private static Field spawnMobDataWatcher, spawnPlayerDataWatcher;
	
	static {
		try {
			spawnMobDataWatcher = S0FPacketSpawnMob.class.getDeclaredField(MCPNames.field("field_149043_l"));
			spawnMobDataWatcher.setAccessible(true);
			
			spawnPlayerDataWatcher = S0CPacketSpawnPlayer.class.getDeclaredField(MCPNames.field("field_148960_i"));
			spawnPlayerDataWatcher.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private PacketData getPacketData(ChannelHandlerContext ctx, Packet packet) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

		if(startTime == null) startTime = System.currentTimeMillis();

		int timestamp = (int)(System.currentTimeMillis() - startTime);
		
		if(packet instanceof S0FPacketSpawnMob) {
			DataWatcher l = (DataWatcher)spawnMobDataWatcher.get(packet);
			DataWatcher dw = new DataWatcher(null);
			if(l == null) {
				spawnMobDataWatcher.set(packet, dw);
			}
		}

		if(packet instanceof S0CPacketSpawnPlayer) {
			DataWatcher l = (DataWatcher)spawnPlayerDataWatcher.get(packet);
			DataWatcher dw = new DataWatcher(null);
			if(l == null) {
				spawnPlayerDataWatcher.set(packet, dw);
			}
		}

		byte[] array = ReplayFileIO.serializePacket(packet);

		return new PacketData(array, timestamp);
	}

}
