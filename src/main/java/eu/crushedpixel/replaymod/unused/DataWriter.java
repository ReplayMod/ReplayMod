package eu.crushedpixel.replaymod.unused;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.network.PacketBuffer;

public class DataWriter {

	private boolean active = true;

	private ConcurrentLinkedQueue<DataReciever> queue = new ConcurrentLinkedQueue<DataReciever>();

	Thread outputThread = new Thread(new Runnable() {

		@Override
		public void run() {
			
			HashMap<Class, Integer> counts = new HashMap<Class, Integer>();
			
			while(active) {
				DataReciever dataReciever = queue.poll();
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

	public ConcurrentLinkedQueue<DataReciever> getQueue() {
		return queue;
	}

	public void setQueue(ConcurrentLinkedQueue<DataReciever> queue) {
		this.queue = queue;
	}

	private DataOutputStream stream;

	public DataWriter(DataOutputStream stream) {
		this.stream = stream;
		outputThread.start();
	}

	public void requestFinish() {
		active = false;
	}

}
