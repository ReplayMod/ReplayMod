package eu.crushedpixel.replaymod.replay;

import net.minecraft.network.EnumConnectionState;

public class PacketInfo {

	private byte[] bytes;
	private EnumConnectionState connectionState;
	private int packetID;
	
	public PacketInfo(byte[] bytes, EnumConnectionState connectionState,
			int packetID) {
		super();
		this.bytes = bytes;
		this.connectionState = connectionState;
		this.packetID = packetID;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	public EnumConnectionState getConnectionState() {
		return connectionState;
	}
	public void setConnectionState(EnumConnectionState connectionState) {
		this.connectionState = connectionState;
	}
	public int getPacketID() {
		return packetID;
	}
	public void setPacketID(int packetID) {
		this.packetID = packetID;
	}
	
	
}
