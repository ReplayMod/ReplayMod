package eu.crushedpixel.replaymod.holders;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.Packet;

public class PacketData {

	private byte[] array;
	private int timestamp;
	
	public PacketData(byte[] array, int timestamp) {
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
