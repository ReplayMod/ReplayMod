package com.replaymod.recording.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketPlaceRecipe;
import net.minecraft.network.play.server.SPacketCustomPayload;

import java.io.IOException;

public class CustomActionPacket {
    public interface CustomAction {
        PacketBuffer toPacketBuffer() throws IOException;
        void fromPacketBuffer(PacketBuffer packetBuffer) throws IOException;
    }

    public static CustomAction getActionFromPacket(SPacketCustomPayload packet) {
        String channelName = packet.getChannelName();
        CustomAction action;
        try {
            switch (channelName) {
                case "t":
                    action = new Tick(packet.getBufferData());
                    break;
                case "c":
                    action = new Camera(packet.getBufferData());
                    break;
                case "a":
                    action = new Keypress(packet.getBufferData());
                    break;
                case "r":
                    action = new RecipeClick(packet.getBufferData());
                    break;
                case "b":
                    action = new ButtonClick(packet.getBufferData());
                    break;
                case "s":
                    action = new SlotClick(packet.getBufferData());
                    break;
                case "cw":
                    action = new CloseWindow(packet.getBufferData());
                    break;
                default:
                    return null;
            }
            return action;
        } catch (IOException e) {
            return null;
        }
    }

    public static class Tick implements CustomAction {
        public boolean isGUIOpen;

        public Tick(PacketBuffer packetBuffer) {this.fromPacketBuffer(packetBuffer);}
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

        public Camera(PacketBuffer packetBuffer){this.fromPacketBuffer(packetBuffer);}
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

    public static class Keypress implements CustomAction {
        public int action;

        public Keypress(PacketBuffer packetBuffer){this.fromPacketBuffer(packetBuffer);}
        public Keypress(int action) {
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

    public static class SlotClick implements CustomAction{
        private CPacketClickWindow cPkt = new CPacketClickWindow();

        public SlotClick(PacketBuffer packetBuffer) throws IOException{ this.fromPacketBuffer(packetBuffer);}
        public SlotClick(CPacketClickWindow cPacketClickWindow) {
            this.cPkt = cPacketClickWindow;
        }

        public PacketBuffer toPacketBuffer() throws IOException {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            cPkt.writePacketData(packetBuffer);
            return packetBuffer;
        }

        public void fromPacketBuffer(PacketBuffer buf) throws IOException {
            cPkt.readPacketData(buf);
        }
    }

    public static class ButtonClick implements CustomAction{
        int windowID;
        int buttonID;
        boolean enabled;

        public ButtonClick(PacketBuffer packetBuffer){this.fromPacketBuffer(packetBuffer);}
        public ButtonClick(int windowID, int buttonID, boolean enabled){
            this.windowID = windowID;
            this.buttonID = buttonID;
            this.enabled = enabled;
        }

        public PacketBuffer toPacketBuffer() {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            packetBuffer.writeInt(windowID);
            packetBuffer.writeInt(buttonID);
            packetBuffer.writeBoolean(enabled);
            return packetBuffer;
        }

        public void fromPacketBuffer(PacketBuffer buf) {
            windowID = buf.readInt();
            buttonID = buf.readInt();
            enabled  = buf.readBoolean();
        }
    }

    public static class RecipeClick implements CustomAction{
        CPacketPlaceRecipe cPkt = new CPacketPlaceRecipe();

        public RecipeClick(PacketBuffer packetBuffer) throws IOException{this.fromPacketBuffer(packetBuffer);}
        public RecipeClick(CPacketPlaceRecipe cPacketPlaceRecipe){
            this.cPkt = cPacketPlaceRecipe;
        }

        public PacketBuffer toPacketBuffer() throws IOException {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            cPkt.writePacketData(packetBuffer);
            return packetBuffer;
        }

        public void fromPacketBuffer(PacketBuffer buf) throws IOException {
            cPkt.readPacketData(buf);
        }
    }

    public static class CloseWindow implements CustomAction{
        int windowID;
        ItemStack heldItem;

        public CloseWindow(PacketBuffer packetBuffer) throws IOException{this.fromPacketBuffer(packetBuffer);}
        public CloseWindow(int windowID, ItemStack heldItem){
            this.windowID = windowID;
            this.heldItem = heldItem;
        }

        public PacketBuffer toPacketBuffer() throws IOException {
            ByteBuf byteBuf = Unpooled.buffer();
            PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
            packetBuffer.writeInt(windowID);
            return packetBuffer;
        }

        public void fromPacketBuffer(PacketBuffer packetBuffer) throws IOException {
            windowID = packetBuffer.readInt();
        }
    }
}
