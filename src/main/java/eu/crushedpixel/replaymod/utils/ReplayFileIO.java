package eu.crushedpixel.replaymod.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;

import akka.japi.Pair;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.PacketSerializer;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.replay.PacketDeserializer;

@SuppressWarnings("resource") //Gets handled by finalizer
public class ReplayFileIO {

	public static List<File> getAllReplayFiles() {
		File folder = new File("./replay_recordings/");
		folder.mkdirs();
		List<File> files = new ArrayList<File>();
		for(File file : folder.listFiles()) {
			if(("."+FilenameUtils.getExtension(file.getAbsolutePath())).equals(
					ConnectionEventHandler.ZIP_FILE_EXTENSION)) {
				files.add(file);
			}
		}
		return files;
	}
	
	private static DataInputStream getMetaDataInputStream(File replayFile) throws Exception {
		ZipFile archive = null;

		try {
			archive = new ZipFile(replayFile);
			ZipArchiveEntry tmcpr = archive.getEntry("metaData"+
					ConnectionEventHandler.JSON_FILE_EXTENSION);

			return new DataInputStream(archive.getInputStream(tmcpr));
		} catch(Exception e) {
			throw e;
		}
	}

	public static void writeReplayFile(File replayFile, File tempFile, ReplayMetaData metaData) throws IOException {
		byte[] buffer = new byte[1024];

		if(!replayFile.exists()) {
			replayFile.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(replayFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		String json = new Gson().toJson(metaData);

		zos.putNextEntry(new ZipEntry("metaData.json"));
		PrintWriter pw = new PrintWriter(zos);
		pw.write(json);
		pw.flush();
		zos.closeEntry();

		zos.putNextEntry(new ZipEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION));
		FileInputStream fis = new FileInputStream(tempFile);
		int len;
		while((len = fis.read(buffer)) > 0) {
			zos.write(buffer, 0, len);
		}

		fis.close();
		zos.closeEntry();

		zos.close();
	}

	private static Pair<Long, DataInputStream> getTempFileInputStream(File replayFile) throws Exception {
		ZipFile archive = null;

		try {
			archive = new ZipFile(replayFile);
			ZipArchiveEntry tmcpr = archive.getEntry("recording"+
					ConnectionEventHandler.TEMP_FILE_EXTENSION);
			long size = tmcpr.getSize();

			return new Pair<Long, DataInputStream>(size, new DataInputStream(archive.getInputStream(tmcpr)));
		} catch(Exception e) {
			throw e;
		}
	}

	public static ReplayMetaData getMetaData(File replayFile) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(getMetaDataInputStream(replayFile)));
		String json = br.readLine();

		return new Gson().fromJson(json, ReplayMetaData.class);
	}

	/**
	 * @param replayFile
	 * @return Whether the given Replay File contains a {@link S01PacketJoinGame}.
	 */
	public static boolean containsJoinPacket(File replayFile) {
		DataInputStream dis = null;
		if(replayFile == lastReplayFile) {
			return lastContainsJoinPacket;
		}

		lastReplayFile = replayFile;
		lastContainsJoinPacket = false;
		try {
			Pair<Long, DataInputStream> pair = getTempFileInputStream(replayFile);
			dis = pair.second();
			PacketData pd = readPacketData(dis);
			while(dis.available() > 0) {
				Packet p = deserializePacket(pd.getByteArray());
				if(p instanceof S01PacketJoinGame) {
					lastContainsJoinPacket = true;
					return lastContainsJoinPacket;
				} if(p instanceof S08PacketPlayerPosLook) {
					lastContainsJoinPacket = false;
					return lastContainsJoinPacket;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(dis != null) {
					dis.close();
				}
			} catch(Exception e) {}
		}

		return false;
	}

	public static PacketData readPacketData(DataInputStream dis) throws IOException {
		int timestamp = dis.readInt();
		int bytes = dis.readInt();
		byte[] bb = new byte[bytes];
		dis.readFully(bb);

		return new PacketData(bb, timestamp);
	}

	public static Packet deserializePacket(byte[] bytes) throws InstantiationException, IllegalAccessException, IOException {
		ByteBuf bb = Unpooled.wrappedBuffer(bytes);
		PacketBuffer pb = new PacketBuffer(bb);

		int i = pb.readVarIntFromBuffer();

		Packet p = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, i);
		p.readPacketData(pb);

		return p;
	}

	public static byte[] serializePacket(Packet packet) throws IOException {
		ByteBuf bb = Unpooled.buffer();
		packetSerializer.encode(EnumConnectionState.PLAY, packet, bb);
		bb.readerIndex(0);
		byte[] array = new byte[bb.readableBytes()];
		bb.readBytes(array);

		bb.readerIndex(0);

		return array;
	}

	public static void writePacket(PacketData pd, DataOutput out) throws IOException {
		out.writeInt(pd.getTimestamp());
		out.writeInt(pd.getByteArray().length);
		out.write(pd.getByteArray());
	}
	
	public static int getWrittenByteSize(PacketData pd) {
		return (2*4)+pd.getByteArray().length;
	}

	public static void writePackets(Collection<PacketData> p, DataOutput out) throws IOException {
		for(PacketData pd : p) {
			writePacket(pd, out);
		}
	}

	private static final PacketSerializer packetSerializer = new PacketSerializer(EnumPacketDirection.CLIENTBOUND);
	private static final PacketDeserializer deserializer = new PacketDeserializer(EnumPacketDirection.SERVERBOUND);

	private static File lastReplayFile = null;
	private static boolean lastContainsJoinPacket = false;

	/**
	 * 
	 * @param replayFile The File to reverse
	 * @param outputFile The File to save the reversed Packets in
	 * @param seekJoinPacket Whether a {@link S01PacketJoinGame} should be seeked in the Replay File. If containsJoinPacket is being
	 * called with the same File directly afterwards, this will reduce the amount of calculations. Only use if needed,
	 * because this consumes a significant amount of time!
	 * @param boundaries Two timestamps which make a boundary for excluded Packets
	 * @return Whether the action was successful
	 */
	public static boolean reversePackets(File replayFile, File outputFile, boolean seekJoinPacket, int... boundaries) {
		lastReplayFile = replayFile;
		lastContainsJoinPacket = false;

		RandomAccessFile raf = null;
		DataInputStream dis = null;

		boolean bounds = false;
		int lower = 0, upper = 0;
		if(boundaries.length >= 2) {
			if(boundaries[0] > boundaries[1]) {
				upper = boundaries[0];
				lower = boundaries[1];
			} else {
				upper = boundaries[1];
				lower = boundaries[0];
			}
		}
		try {
			if(!outputFile.exists()) {
				outputFile.createNewFile();
			}
			raf = new RandomAccessFile(outputFile, "rw");

			Pair<Long, DataInputStream> pair = getTempFileInputStream(replayFile);
			dis = pair.second();
			long fileLength = pair.first();
			
			raf.setLength(fileLength);
			
			long pointerBefore = fileLength;

			while(dis.available() > 0) {
				try {
					PacketData pd = readPacketData(dis);

					boolean write = true;
					if(bounds) {
						if(pd.getTimestamp() < lower || pd.getTimestamp() > upper) {
							write = false;
						}
					}

					if(write) {
						if(seekJoinPacket && !lastContainsJoinPacket) {
							Packet p = deserializePacket(pd.getByteArray());
							if(p instanceof S01PacketJoinGame) lastContainsJoinPacket = true;
						}
						pointerBefore = pointerBefore - getWrittenByteSize(pd);
						raf.seek(pointerBefore);
						writePacket(pd, raf);
					}
				} catch(EOFException e) {
					e.printStackTrace();
					break;
				}
			}
			return true;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(raf != null) {
					raf.close();
				}
				if(dis != null) {
					dis.close();
				}
			} catch(Exception e) {}
		}

		return false;
	}
}
