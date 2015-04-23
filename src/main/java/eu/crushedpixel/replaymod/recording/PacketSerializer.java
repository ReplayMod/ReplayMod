package eu.crushedpixel.replaymod.recording;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import net.minecraft.util.MessageSerializer;

import java.io.IOException;

public class PacketSerializer extends MessageSerializer {

    public PacketSerializer(EnumPacketDirection direction) {
        super(direction);
    }

    public static ByteBuf toByteBuf(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteBuf bb;
        bb = Unpooled.buffer(bytes.length);
        bb.writeBytes(bytes);

        return bb;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf byteBuf) throws IOException {
        EnumConnectionState state = ((EnumConnectionState) ctx.channel().attr(NetworkManager.attrKeyConnectionState).get());
        encode(state, packet, byteBuf);
    }

    public void encode(EnumConnectionState state, Packet packet, ByteBuf byteBuf) {
        Integer integer = state.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);

        if(integer == null) {
            return;
        } else {
            PacketBuffer packetbuffer = new PacketBuffer(byteBuf);
            packetbuffer.writeVarIntToBuffer(integer.intValue());

            try {
                packet.writePacketData(packetbuffer);
            } catch(Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }


}
