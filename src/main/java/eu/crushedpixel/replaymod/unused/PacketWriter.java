package eu.crushedpixel.replaymod.unused;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.network.PacketBuffer;

public class PacketWriter {

	private boolean active = true;

	private ConcurrentLinkedQueue<PacketReciever> queue = new ConcurrentLinkedQueue<PacketReciever>();

	Thread outputThread = new Thread(new Runnable() {

		@Override
		public void run() {
			
			HashMap<Class, Integer> counts = new HashMap<Class, Integer>();
			
			while(active) {
				PacketReciever packetReciever = queue.poll();
				if(packetReciever != null) {
					ByteBuf bb = Unpooled.buffer();
					PacketBuffer packetBuffer = new PacketBuffer(bb);
					//write the Packet to the given OutputStream

					if(packetReciever.getPacket() != null) {
						try {
							packetReciever.getPacket().writePacketData(packetBuffer);
							
							bb.readerIndex(0);
							byte[] array = new byte[bb.readableBytes()];
							bb.readBytes(array);
							
							stream.writeInt(packetReciever.getTimestamp()); //Timestamp
							stream.writeInt(array.length); //Lenght
							
							if(counts.containsKey(packetReciever.getPacket().getClass())) {
								counts.put(packetReciever.getPacket().getClass(), counts.get(packetReciever.getPacket().getClass())+array.length);
							} else {
								counts.put(packetReciever.getPacket().getClass(), array.length);
							}
							
							stream.writeShort(packetReciever.getPacketID()); //Packet ID
							stream.write(array); //Content
							stream.flush();
						} catch(Exception e) {
							System.out.println(packetReciever.getPacket().getClass());
							e.printStackTrace();
						}
					}

				} else {
					try {
						//let the Thread sleep for 1/2 second and queue up new Packets
						Thread.sleep(500L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				stream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			for(Entry<Class, Integer> entries : counts.entrySet()) {
				System.out.println(entries.getKey()+ "| "+entries.getValue());
			}
			
		}
	});

	public ConcurrentLinkedQueue<PacketReciever> getQueue() {
		return queue;
	}

	public void setQueue(ConcurrentLinkedQueue<PacketReciever> queue) {
		this.queue = queue;
	}

	private DataOutputStream stream;

	public PacketWriter(DataOutputStream stream) {
		this.stream = stream;
		outputThread.start();
	}

	public void requestFinish() {
		active = false;
	}

}
