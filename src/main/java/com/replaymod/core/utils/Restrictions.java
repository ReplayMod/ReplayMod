package com.replaymod.core.utils;

import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.PacketByteBuf;

//#if MC>=12006
//$$ import net.minecraft.network.codec.PacketCodec;
//$$ import net.minecraft.network.packet.CustomPayload;
//#endif

//#if MC>=10904
import net.minecraft.util.Identifier;
//#endif

//#if MC<=10710 || MC>=12002
//$$ import io.netty.buffer.Unpooled;
//#endif

/**
 * Restrictions set by the server,
 * @see <a href="https://gist.github.com/Johni0702/2547c463e51f65f312cb">Replay Restrictions Gist</a>
 */
public class Restrictions {
    //#if MC>=11400
    public static final Identifier PLUGIN_CHANNEL = new Identifier("replaymod", "restrict");
    //#else
    //$$ public static final String PLUGIN_CHANNEL = "Replay|Restrict";
    //#endif
    private boolean noXray;
    private boolean noNoclip;
    private boolean onlyFirstPerson;
    private boolean onlyRecordingPlayer;

    public String handle(CustomPayloadS2CPacket packet) {
        //#if MC>=12006
        //$$ PacketByteBuf buffer = new PacketByteBuf(Unpooled.wrappedBuffer(((Payload) packet.payload()).bytes()));
        //#elseif MC>=12002
        //$$ PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        //$$ packet.write(buffer);
        //#elseif MC>=10800
        PacketByteBuf buffer = packet.getData();
        //#else
        //$$ PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(packet.func_149168_d()));
        //#endif
        while (buffer.isReadable()) {
            String name = buffer.readString(64);
            boolean active = buffer.readBoolean();
//            if ("no_xray".equals(name)) {
//                noXray = active;
//            } else if ("no_noclip".equals(name)) {
//                noNoclip = active;
//            } else if ("only_first_person".equals(name)) {
//                onlyFirstPerson = active;
//            } else if ("only_recording_player".equals(name)) {
//                onlyRecordingPlayer = active;
//            } else {
                return name;
//            }
        }
        return null;
    }

    public boolean isNoXray() {
        return noXray;
    }

    public boolean isNoNoclip() {
        return noNoclip;
    }

    public boolean isOnlyFirstPerson() {
        return onlyFirstPerson;
    }

    public boolean isOnlyRecordingPlayer() {
        return onlyRecordingPlayer;
    }

    //#if MC>=12006
    //$$ public static final CustomPayload.Id<Payload> ID = CustomPayload.id(PLUGIN_CHANNEL.toString());
    //$$ public static final PacketCodec<? super PacketByteBuf, Payload> CODEC = PacketCodec.ofStatic(
    //$$         (buf, payload) -> buf.writeBytes(payload.bytes()),
    //$$         buf -> {
    //$$             byte[] bytes = new byte[buf.readableBytes()];
    //$$             buf.readBytes(bytes);
    //$$             return new Payload(bytes);
    //$$         }
    //$$ );
    //$$ public record Payload(byte[] bytes) implements CustomPayload {
    //$$     @Override
    //$$     public Id<? extends CustomPayload> getId() {
    //$$         return ID;
    //$$     }
    //$$ }
    //#endif
}
