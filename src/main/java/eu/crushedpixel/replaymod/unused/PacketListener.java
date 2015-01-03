package eu.crushedpixel.replaymod.unused;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class PacketListener extends ChannelInboundHandlerAdapter {

	private PacketWriter packetWriter;
	private long startTime;
	
	public PacketListener(PacketWriter packetWriter, long startTime) {
		this.packetWriter = packetWriter;
		this.startTime = startTime;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		super.channelRead(ctx, msg);
		
		if(msg instanceof Packet) {
			Packet packet = (Packet)msg;
			int packetID = ((EnumConnectionState)ctx.channel()
					.attr(NetworkManager.attrKeyConnectionState).get()).getPacketId(EnumPacketDirection.CLIENTBOUND, packet);
			int timestamp = (int) (System.currentTimeMillis() - startTime);
			PacketReciever reciever = new PacketReciever(packet, (short)packetID, timestamp);
			packetWriter.getQueue().add(reciever);
		}
	}
	

}
