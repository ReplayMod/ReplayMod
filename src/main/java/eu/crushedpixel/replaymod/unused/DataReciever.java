package eu.crushedpixel.replaymod.unused;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.Packet;

public class DataReciever {

	private byte[] array;
	private int timestamp;
	
	public DataReciever(byte[] array, int timestamp) {
		this.array = array;
		this.timestamp = timestamp;
	}
	
	public byte[] getByteArray() {
		return array;
	}
	public void setByteArray(byte[] array) {
		this.array = array;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	
}
