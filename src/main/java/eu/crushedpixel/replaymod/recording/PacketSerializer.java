package eu.crushedpixel.replaymod.recording;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.util.MessageSerializer;

public class PacketSerializer extends MessageSerializer {

	public PacketSerializer(EnumPacketDirection direction) {
		super(direction);
	}

	@Override
	public void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf byteBuf) throws IOException {
		EnumConnectionState state =  ((EnumConnectionState)ctx.channel().attr(NetworkManager.attrKeyConnectionState).get());
		encode(state, packet, byteBuf);
	}
	
	public void encode(EnumConnectionState state, Packet packet, ByteBuf byteBuf) {
		Integer integer = state.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);

        if (integer == null) {
            return;
        } else {
            PacketBuffer packetbuffer = new PacketBuffer(byteBuf);
            packetbuffer.writeVarIntToBuffer(integer.intValue());

            try {
                packet.writePacketData(packetbuffer);
            }
            catch (Throwable throwable) {
               throwable.printStackTrace();
            }
        }
	}

	public static ByteBuf toByteBuf(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteBuf bb;
		bb = Unpooled.buffer(bytes.length);
		bb.writeBytes(bytes);

		return bb;
	}


}
