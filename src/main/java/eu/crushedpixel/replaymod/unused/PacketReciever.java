package eu.crushedpixel.replaymod.unused;

import net.minecraft.network.Packet;

public class PacketReciever {

	private Packet packet;
	private short packetID;
	private int timestamp;
	
	public PacketReciever(Packet packet, short packetID, int timestamp) {
		this.packet = packet;
		this.packetID = packetID;
		this.timestamp = timestamp;
	}
	
	public Packet getPacket() {
		return packet;
	}
	public void setPacket(Packet packet) {
		this.packet = packet;
	}
	public short getPacketID() {
		return packetID;
	}
	public void setPacketID(short packetID) {
		this.packetID = packetID;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	
}
