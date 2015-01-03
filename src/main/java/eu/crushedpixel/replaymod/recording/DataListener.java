package eu.crushedpixel.replaymod.recording;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.network.Packet;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.chat.ChatMessageRequests;
import eu.crushedpixel.replaymod.chat.ChatMessageRequests.ChatMessageType;

public class DataListener extends ChannelInboundHandlerAdapter {

	protected File file;
	protected long startTime;
	protected long maxSize;
	protected long totalBytes = 0;
	protected DataOutputStream out;
	protected String name;
	protected String worldName;
	
	protected long lastSentPacket = 0;

	protected boolean alive = true;
	protected boolean isPacketListener = false;

	private boolean writeJoinPacket = false;

	private Gson gson = new Gson();

	public void setWorldName(String worldName) {
		this.worldName = worldName;
		System.out.println(worldName);
	}
	
	public void insertPacket(Packet p) {
		
	}

	public DataListener(File file, String name, String worldName, long startTime, int maxSize) throws FileNotFoundException {
		this.file = file;
		this.startTime = startTime;
		this.maxSize = maxSize*1024*1024;
		this.name = name;
		this.worldName = worldName;

		System.out.println(worldName);

		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		out = new DataOutputStream(bos);

		writeJoinPacket = true;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(!alive || isPacketListener) {
			lastSentPacket = System.currentTimeMillis();
			super.channelRead(ctx, msg);
			return;
		}

		/*
		if(writeJoinPacket) {
			writeJoinPacket = false;
			S01PacketJoinGame p = new S01PacketJoinGame();

			int timestamp = 0;

			PacketSerializer ps = new PacketSerializer(EnumPacketDirection.CLIENTBOUND);
			ByteBuf bb = Unpooled.buffer();
			
			PacketBuffer pb = new PacketBuffer(bb);
			pb.writeInt(new Random().nextInt(1000));
			pb.writeByte(3);
			pb.writeByte(-1);
			pb.writeByte(0);
			pb.writeByte(100);
			pb.writeString("default");
			pb.writeBoolean(false);
			
			p.readPacketData(pb);
			
			ByteBuf b2 = Unpooled.buffer();
			ps.encode(ctx, p, b2);

			b2.readerIndex(0);
			
			byte[] array = new byte[b2.readableBytes()];
			b2.readBytes(array);

			b2.readerIndex(0);
			
			out.writeInt(timestamp);
			out.writeInt(array.length);
			out.write(array);
			out.flush();
		}
		*/

		int timestamp = (int)(System.currentTimeMillis() - startTime);

		ByteBuf byteBuf = (ByteBuf)msg;

		byteBuf.readerIndex(0);
		byte[] array = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(array);

		totalBytes += (array.length + (2*4)); //two Integer values and the Packet size
		if(totalBytes >= maxSize && maxSize > 0) {
			ChatMessageRequests.addChatMessage("Maximum file size exceeded", ChatMessageType.WARNING);
			ChatMessageRequests.addChatMessage("The Recording has been stopped", ChatMessageType.WARNING);
			alive = false;
		}

		byteBuf.readerIndex(0);

		out.writeInt(timestamp);
		out.writeInt(array.length);
		out.write(array);
		out.flush();

		super.channelRead(ctx, msg);
	}


	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		out.flush();
		out.close();

		byte[] buffer = new byte[1024];

		try {
			ReplayMetaData metaData = new ReplayMetaData(isPacketListener, worldName, (int) (lastSentPacket - startTime), startTime);
			String json = gson.toJson(metaData);

			File folder = new File("./replay_recordings/");
			folder.mkdirs();

			File archive = new File(folder, name+ConnectionEventHandler.ZIP_FILE_EXTENSION);
			archive.createNewFile();

			FileOutputStream fos = new FileOutputStream(archive);
			ZipOutputStream zos = new ZipOutputStream(fos);

			zos.putNextEntry(new ZipEntry("metaData.json"));
			PrintWriter pw = new PrintWriter(zos);
			pw.write(json);
			pw.flush();
			zos.closeEntry();

			zos.putNextEntry(new ZipEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION));
			FileInputStream fis = new FileInputStream(file);
			int len;
			while((len = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}

			fis.close();
			zos.closeEntry();

			zos.close();

			file.delete();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
