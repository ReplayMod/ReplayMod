package eu.crushedpixel.replaymod.recording;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.gui.GuiReplaySaving;
import eu.crushedpixel.replaymod.holders.PacketData;

public abstract class DataListener extends ChannelInboundHandlerAdapter {

	protected File file;
	protected Long startTime = null;
	protected String name;
	protected String worldName;
	
	private boolean singleplayer;
	
	protected long lastSentPacket = 0;

	protected boolean alive = true;
	
	protected DataWriter dataWriter;

	private Gson gson = new Gson();
	
	protected Set<String> players = new HashSet<String>();

	public void setWorldName(String worldName) {
		this.worldName = worldName;
		System.out.println(worldName);
	}

	public DataListener(File file, String name, String worldName, long startTime, boolean singleplayer) throws FileNotFoundException {
		this.file = file;
		this.startTime = startTime;
		this.name = name;
		this.worldName = worldName;
		this.singleplayer = singleplayer;

		System.out.println(worldName);

		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		DataOutputStream out = new DataOutputStream(bos);
		dataWriter = new DataWriter(out);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		dataWriter.requestFinish(players);
	}
	
	public class DataWriter {

		private boolean active = true;

		private ConcurrentLinkedQueue<PacketData> queue = new ConcurrentLinkedQueue<PacketData>();

		public void writeData(PacketData data) {
			queue.add(data);
		}
		
		Thread outputThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				HashMap<Class, Integer> counts = new HashMap<Class, Integer>();
				
				while(active) {
					PacketData dataReciever = queue.poll();
					if(dataReciever != null) {
						//write the ByteBuf to the given OutputStream

						byte[] array = dataReciever.getByteArray();
						
						if(array != null) {
							try {
								stream.writeInt(dataReciever.getTimestamp()); //Timestamp
								stream.writeInt(array.length); //Lenght

								stream.write(array); //Content
								stream.flush();
							} catch(Exception e) {
								e.printStackTrace();
							}
						}

					} else {
						try {
							//let the Thread sleep for 1/4 second and queue up new Packets
							Thread.sleep(250L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				try {
					stream.flush();
					stream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				for(Entry<Class, Integer> entries : counts.entrySet()) {
					System.out.println(entries.getKey()+ "| "+entries.getValue());
				}
				
			}
		});

		private DataOutputStream stream;

		public DataWriter(DataOutputStream stream) {
			this.stream = stream;
			outputThread.start();
		}

		public void requestFinish(Set<String> players) {
			active = false;
			byte[] buffer = new byte[1024];

			try {
				GuiReplaySaving.replaySaving = true;

				String mcversion = Minecraft.getMinecraft().getVersion();
				String[] split = mcversion.split("-");
				if(split.length > 0) {
					mcversion = split[0];
				}
				
				String[] pl = players.toArray(new String[players.size()]);
				
				ReplayMetaData metaData = new ReplayMetaData(singleplayer, worldName, (int) lastSentPacket, startTime, pl, mcversion);
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
				
				GuiReplaySaving.replaySaving = false;
			} catch(Exception e) {
				e.printStackTrace();
				GuiReplaySaving.replaySaving = false;
			}
		}

	}

}
