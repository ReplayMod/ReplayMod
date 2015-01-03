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
	public void encode(ChannelHandlerContext p_encode_1_, Packet p_encode_2_, ByteBuf p_encode_3_) throws IOException {
		//super.encode(p_encode_1_, p_encode_2_, p_encode_3_);
		
		Integer integer = ((EnumConnectionState)p_encode_1_.channel().attr(NetworkManager.attrKeyConnectionState).get()).getPacketId(EnumPacketDirection.CLIENTBOUND, p_encode_2_);

        if (integer == null)
        {
            return;
        }
        else
        {
            PacketBuffer packetbuffer = new PacketBuffer(p_encode_3_);
            packetbuffer.writeVarIntToBuffer(integer.intValue());

            try
            {
                if (p_encode_2_ instanceof S0CPacketSpawnPlayer)
                {
                    //p_encode_2_ = p_encode_2_; FML: Kill warning
                }

                p_encode_2_.writePacketData(packetbuffer);
            }
            catch (Throwable throwable)
            {
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
