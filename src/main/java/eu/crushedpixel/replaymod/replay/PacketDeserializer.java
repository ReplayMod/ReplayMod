package eu.crushedpixel.replaymod.replay;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.MessageDeserializer;

import com.google.common.collect.BiMap;

import eu.crushedpixel.replaymod.reflection.MCPNames;

public class PacketDeserializer extends MessageDeserializer {

	private final EnumPacketDirection direction;
	private Field directionMaps;
	private EnumConnectionState state;
	
	public PacketDeserializer(EnumPacketDirection direction) {
		super(direction);
		this.direction = direction;
		try {
			directionMaps = EnumConnectionState.class.getDeclaredField(MCPNames.field("field_179247_h"));
			directionMaps.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public Packet getPacket(EnumPacketDirection direction, int packetId) throws InstantiationException, IllegalAccessException
    {
		Map map = ((Map)directionMaps.get(state));
		BiMap biMap = ((BiMap)map.get(direction));
		if(biMap == null) {
			System.out.println("BiMap is null!");
		}
        Class oclass = (Class)biMap.get(Integer.valueOf(packetId));
        return oclass == null ? null : (Packet)oclass.newInstance();
    }

	public void setEnumConnectionState(EnumConnectionState state) {
		this.state = state;
	}
	
	@Override
	public void decode(ChannelHandlerContext p_decode_1_,
			ByteBuf p_decode_2_, List p_decode_3_) throws IOException,
			InstantiationException, IllegalAccessException {

		 if (p_decode_2_.readableBytes() != 0)
	        {
	            PacketBuffer packetbuffer = new PacketBuffer(p_decode_2_);
	            int i = packetbuffer.readVarIntFromBuffer();
	            
	            Field state_by_id = null;
	            try {
	            	state_by_id = EnumConnectionState.class.getDeclaredField(MCPNames.field("field_150764_e"));
	            	state_by_id.setAccessible(true);
	            	state = (EnumConnectionState)((TIntObjectMap)state_by_id.get(null)).get(i);
	            	TIntObjectMap map = (TIntObjectMap)state_by_id.get(null);
	            	TIntObjectIterator it = map.iterator();
	            	while(it.hasNext()) {
	            		it.advance();
	            		System.out.println(it.key() +" | "+it.value().getClass());
	            	}
	            } catch(Exception e) {
	            	e.printStackTrace();
	            }
	            
	            if(state == null) {
	            	System.out.println("state is null");
	            }
	            //Packet packet = getPacket(this.direction, i);
	            Packet packet = state.getPacket(EnumPacketDirection.CLIENTBOUND, i);

	            if (packet == null)
	            {
	                throw new IOException("Bad packet id " + i);
	            }
	            else
	            {
	                packet.readPacketData(packetbuffer);

	                if (packetbuffer.readableBytes() > 0)
	                {
	                    throw new IOException("Packet " + ((EnumConnectionState)p_decode_1_.channel().attr(NetworkManager.attrKeyConnectionState).get()).getId() + "/" + i + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + packetbuffer.readableBytes() + " bytes extra whilst reading packet " + i);
	                }
	                else
	                {
	                    p_decode_3_.add(packet);
	                }
	            }
	        }
	}

}
