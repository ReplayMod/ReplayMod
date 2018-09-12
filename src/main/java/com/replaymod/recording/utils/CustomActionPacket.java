package com.replaymod.recording.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;

public class CustomActionPacket {
    public interface CustomAction {
        PacketBuffer toPacketBuffer();
        void fromPacketBuffer(PacketBuffer packetBuffer);
    }

    public static CustomAction getActionFromPacket(SPacketCustomPayload packet) {
        String channelName = packet.getChannelName();
        CustomAction action;
        if (channelName.equals("t")) {
            action = new Tick();
            action.fromPacketBuffer(packet.getBufferData());
        } else if (channelName.equals("c")) {
            action = new Camera();
            action.fromPacketBuffer(packet.getBufferData());
        } else if (channelName.equals("a")) {
            action = new Action();
            action.fromPacketBuffer(packet.getBufferData());
        } else {
            return null;
        }
        return action;
    }

    public static class Tick implements CustomAction {
        public boolean isGUIOpen;
        public Tick() {}
        public Tick(boolean isGUIOpen) {
            this.isGUIOpen = isGUIOpen;
        }
        public PacketBuffer toPacketBuffer() {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            packetBuffer.writeVarInt(this.isGUIOpen ? 0 : 1);
            return packetBuffer;
        }
        public void fromPacketBuffer(PacketBuffer packetBuffer) {
            this.isGUIOpen = packetBuffer.readVarInt() == 0;
        }
    }

    public static class Camera implements CustomAction {
        public float diffYaw;
        public float diffPitch;
        public Camera() {}
        public Camera(float diffYaw, float diffPitch) {
            this.diffYaw = diffYaw;
            this.diffPitch = diffPitch;
        }
        public PacketBuffer toPacketBuffer() {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            packetBuffer.writeFloat(this.diffYaw);
            packetBuffer.writeFloat(this.diffPitch);
            return packetBuffer;
        }
        public void fromPacketBuffer(PacketBuffer packetBuffer) {
            this.diffYaw = packetBuffer.readFloat();
            this.diffPitch = packetBuffer.readFloat();
        }
    }

    public static class Action implements CustomAction {
        public int action;
        public Action() {}
        public Action(int action) {
            this.action = action;
        }
        public PacketBuffer toPacketBuffer() {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            packetBuffer.writeVarInt(this.action);
            return packetBuffer;
        }
        public void fromPacketBuffer(PacketBuffer packetBuffer) {
            this.action = packetBuffer.readVarInt();
        }
    }
}
