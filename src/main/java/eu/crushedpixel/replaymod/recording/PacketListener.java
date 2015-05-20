package eu.crushedpixel.replaymod.recording;

import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0FPacketSpawnMob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PacketListener extends DataListener {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private ChannelHandlerContext context = null;


    public PacketListener(File file, String name, String worldName, long startTime, boolean singleplayer) throws FileNotFoundException {
        super(file, name, worldName, startTime, singleplayer);
    }

    public void saveOnly(Packet packet) {
        try {
            if(packet instanceof S0CPacketSpawnPlayer) {
                UUID uuid = ((S0CPacketSpawnPlayer) packet).func_179819_c();
                players.add(uuid.toString());
            }

            PacketData pd = getPacketData(context, packet);
            writeData(pd);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(ctx == null) {
            if(context == null) {
                return;
            } else {
                ctx = context;
            }
        }
        this.context = ctx;
        if(!alive) {
            super.channelRead(ctx, msg);
            return;
        }
        if(msg instanceof Packet) {
            try {
                Packet packet = (Packet) msg;

                if(packet instanceof S0DPacketCollectItem) {
                    if(mc.thePlayer != null ||
                            ((S0DPacketCollectItem) packet).func_149353_d() == mc.thePlayer.getEntityId()) {
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                if(packet instanceof S0CPacketSpawnPlayer) {
                    UUID uuid = ((S0CPacketSpawnPlayer) packet).func_179819_c();
                    players.add(uuid.toString());
                }

                PacketData pd = getPacketData(ctx, packet);
                writeData(pd);
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

        super.channelRead(ctx, msg);
    }

    private void writeData(PacketData pd) {
        dataWriter.writeData(pd);
        lastSentPacket = pd.getTimestamp();
    }

    private PacketData getPacketData(ChannelHandlerContext ctx, Packet packet) throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        if(startTime == null) startTime = System.currentTimeMillis();

        int timestamp = (int) (System.currentTimeMillis() - startTime);


        if(packet instanceof S0FPacketSpawnMob) {
            S0FPacketSpawnMob p = (S0FPacketSpawnMob) packet;
            p.field_149043_l = new DataWatcher(null);
            for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>)p.func_149027_c()) {
                p.field_149043_l.addObject(wo.getDataValueId(), wo.getObject());
            }
        }

        if(packet instanceof S0CPacketSpawnPlayer) {
            S0CPacketSpawnPlayer p = (S0CPacketSpawnPlayer) packet;
            p.field_148960_i = new DataWatcher(null);
            for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>)p.func_148944_c()) {
                p.field_148960_i.addObject(wo.getDataValueId(), wo.getObject());
            }
        }


        byte[] array = ReplayFileIO.serializePacket(packet);

        return new PacketData(array, timestamp);
    }

}
