package eu.crushedpixel.replaymod.recording;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;

import net.minecraft.entity.DataWatcher;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.reflection.MCPNames;

public class PacketListener extends DataListener {

	public PacketListener(File file, String name, String worldName, long startTime, int maxSize, boolean singleplayer) throws FileNotFoundException {
		super(file, name, worldName, startTime, maxSize, singleplayer);
	}
	
	private ChannelHandlerContext context = null;

	private static final PacketSerializer packetSerializer = new PacketSerializer(EnumPacketDirection.CLIENTBOUND);
	
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
				
				if(startTime == null) startTime = System.currentTimeMillis();
				
				int timestamp = (int)(System.currentTimeMillis() - startTime);

				//Converts the packet back to a ByteBuffer for correct saving

				ByteBuf bb = Unpooled.buffer();
				if(packet instanceof S0FPacketSpawnMob) {
					Field field_149043_l = S0FPacketSpawnMob.class.getDeclaredField(MCPNames.field("field_149043_l"));
					field_149043_l.setAccessible(true);
					DataWatcher l = (DataWatcher)field_149043_l.get(packet);
					DataWatcher dw = new DataWatcher(null);
					if(l == null) {
						field_149043_l.set(packet, dw);
					}
				}

				if(packet instanceof S0CPacketSpawnPlayer) {
					Field field_149043_l = S0CPacketSpawnPlayer.class.getDeclaredField(MCPNames.field("field_148960_i"));
					field_149043_l.setAccessible(true);
					DataWatcher l = (DataWatcher)field_149043_l.get(packet);
					DataWatcher dw = new DataWatcher(null);
					if(l == null) {
						field_149043_l.set(packet, dw);
					}
				}
				
				packetSerializer.encode(ctx, packet, bb);

				bb.readerIndex(0);
				byte[] array = new byte[bb.readableBytes()];
				bb.readBytes(array);

				totalBytes += (array.length + (2*4)); //two Integer values and the Packet size
				if(totalBytes >= maxSize && maxSize > 0) {
					ChatMessageRequests.addChatMessage("Maximum file size exceeded", ChatMessageType.WARNING);
					ChatMessageRequests.addChatMessage("The Recording has been stopped", ChatMessageType.WARNING);
					alive = false;
				}

				bb.readerIndex(0);

				dataWriter.writeData(new PacketData(array, timestamp));

				lastSentPacket = timestamp;
			} catch(Exception e) {
				e.printStackTrace();
			}

		}

		super.channelRead(ctx, msg);
	}

}
