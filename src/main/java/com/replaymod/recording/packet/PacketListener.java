package com.replaymod.recording.packet;

import com.replaymod.core.utils.Restrictions;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11300
import net.minecraft.network.login.server.SPacketLoginSuccess;
//#endif

//#if MC>=10904
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.text.TextComponentString;
//#else
//$$ import net.minecraft.entity.DataWatcher;
//$$ import net.minecraft.util.ChatComponentText;
//$$
//$$ import java.util.List;
//#endif

//#if MC>=10800
import net.minecraft.network.EnumPacketDirection;
// FIXME import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
//#else
//$$ import cpw.mods.fml.common.network.internal.FMLProxyPacket;
//#endif

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.replaymod.core.versions.MCVer.*;

public class PacketListener extends ChannelInboundHandlerAdapter {

    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final ReplayFile replayFile;

    private final ResourcePackRecorder resourcePackRecorder;

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private final DataOutputStream packetOutputStream;

    private ReplayMetaData metaData;

    private ChannelHandlerContext context = null;

    private final long startTime;
    private long lastSentPacket;
    private long timePassedWhilePaused;
    private volatile boolean serverWasPaused;
    //#if MC>=11300
    private EnumConnectionState connectionState = EnumConnectionState.LOGIN;
    //#else
    //$$ private EnumConnectionState connectionState = EnumConnectionState.PLAY;
    //#endif

    /**
     * Used to keep track of the last metadata save job submitted to the save service and
     * as such prevents unnecessary writes.
     */
    private final AtomicInteger lastSaveMetaDataId = new AtomicInteger();

    public PacketListener(ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.replayFile = replayFile;
        this.metaData = metaData;
        this.resourcePackRecorder = new ResourcePackRecorder(replayFile);
        this.packetOutputStream = new DataOutputStream(replayFile.writePacketData());
        this.startTime = metaData.getDate();

        saveMetaData();
    }

    private void saveMetaData() {
        int id = lastSaveMetaDataId.incrementAndGet();
        saveService.submit(() -> {
            if (lastSaveMetaDataId.get() != id) {
                return; // Another job has been scheduled, it will do the hard work.
            }
            try {
                synchronized (replayFile) {
                    replayFile.writeMetaData(metaData);
                }
            } catch (IOException e) {
                logger.error("Writing metadata:", e);
            }
        });
    }

    public void save(Packet packet) {
        try {
            //#if MC>=10904
            if(packet instanceof SPacketSpawnPlayer) {
                UUID uuid = ((SPacketSpawnPlayer) packet).getUniqueId();
            //#else
            //$$ if(packet instanceof S0CPacketSpawnPlayer) {
                //#if MC>=10809
                //$$ UUID uuid = ((S0CPacketSpawnPlayer) packet).getPlayer();
                //#else
                //#if MC>=10800
                //$$ UUID uuid = ((S0CPacketSpawnPlayer) packet).func_179819_c();
                //#else
                //$$ UUID uuid = ((S0CPacketSpawnPlayer) packet).field_148955_b.getId();
                //#endif
                //#endif
            //#endif
                Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
                uuids.add(uuid.toString());
                metaData.setPlayers(uuids.toArray(new String[uuids.size()]));
                saveMetaData();
            }
            //#if MC<10904
            //#if MC>=10800
            //$$ if (packet instanceof S46PacketSetCompressionLevel) {
            //$$     return; // Replay data is never compressed on the packet level
            //$$ }
            //#endif
            //#endif

            byte[] bytes = getPacketData(packet);
            long now = System.currentTimeMillis();
            saveService.submit(() -> {
                if (serverWasPaused) {
                    timePassedWhilePaused = now - startTime - lastSentPacket;
                    serverWasPaused = false;
                }
                int timestamp = (int) (now - startTime - timePassedWhilePaused);
                lastSentPacket = timestamp;
                try {
                    packetOutputStream.writeInt(timestamp);
                    packetOutputStream.writeInt(bytes.length);
                    packetOutputStream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            //#if MC>=11300
            if (packet instanceof SPacketLoginSuccess) {
                connectionState = EnumConnectionState.PLAY;
            }
            //#endif
        } catch(Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metaData.setDuration((int) lastSentPacket);
        saveMetaData();

        saveService.shutdown();
        try {
            saveService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Waiting for save service termination:", e);
        }

        synchronized (replayFile) {
            try {
                replayFile.save();
                replayFile.close();
            } catch (IOException e) {
                logger.error("Saving replay file:", e);
            }
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

        if (msg instanceof Packet) {
            try {
                Packet packet = (Packet) msg;

                //#if MC>=10904
                if(packet instanceof SPacketCollectItem) {
                    if(player(mc) != null ||
                            ((SPacketCollectItem) packet).getEntityID() == player(mc).getEntityId()) {
                //#else
                //$$ if(packet instanceof S0DPacketCollectItem) {
                //$$     if(player(mc) != null ||
                            //#if MC>=10809
                            //$$ ((S0DPacketCollectItem) packet).getEntityID() == player(mc).getEntityId()) {
                            //#else
                            //$$ ((S0DPacketCollectItem) packet).func_149353_d() == player(mc).getEntityId()) {
                            //#endif
                //#endif
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                //#if MC>=10904
                if (packet instanceof SPacketResourcePackSend) {
                    save(resourcePackRecorder.handleResourcePack((SPacketResourcePackSend) packet));
                    return;
                }
                //#else
                //#if MC>=10800
                //$$ if (packet instanceof S48PacketResourcePackSend) {
                //$$     save(resourcePackRecorder.handleResourcePack((S48PacketResourcePackSend) packet));
                //$$     return;
                //$$ }
                //#else
                //$$ if (packet instanceof S3FPacketCustomPayload) {
                //$$     S3FPacketCustomPayload p = (S3FPacketCustomPayload) packet;
                //$$     if ("MC|RPack".equals(p.func_149169_c())) {
                //$$         save(resourcePackRecorder.handleResourcePack(p));
                //$$         return;
                //$$     }
                //$$ }
                //#endif
                //#endif

                /* FIXME
                if (packet instanceof FMLProxyPacket) {
                    // This packet requires special handling
                    //#if MC>=10800
                    ((FMLProxyPacket) packet).toS3FPackets().forEach(this::save);
                    //#else
                    //$$ save(((FMLProxyPacket) packet).toS3FPacket());
                    //#endif
                    super.channelRead(ctx, msg);
                    return;
                }
                */

                save(packet);

                //#if MC>=10904
                if (packet instanceof SPacketCustomPayload) {
                    SPacketCustomPayload p = (SPacketCustomPayload) packet;
                    if (Restrictions.PLUGIN_CHANNEL.equals(p.getChannelName())) {
                        packet = new SPacketDisconnect(new TextComponentString("Please update to view this replay."));
                //#else
                //$$ if (packet instanceof S3FPacketCustomPayload) {
                //$$     S3FPacketCustomPayload p = (S3FPacketCustomPayload) packet;
                    //#if MC>=10800
                    //$$ String channelName = p.getChannelName();
                    //#else
                    //$$ String channelName = p.func_149169_c();
                    //#endif
                //$$     if (Restrictions.PLUGIN_CHANNEL.equals(channelName)) {
                //$$         packet = new S40PacketDisconnect(new ChatComponentText("Please update to view this replay."));
                //#endif
                        save(packet);
                    }
                }
            } catch(Exception e) {
                logger.error("Handling packet for recording:", e);
            }

        }

        super.channelRead(ctx, msg);
    }

    //#if MC>=10904
    private <T> void DataManager_set(EntityDataManager dataManager, EntityDataManager.DataEntry<T> entry) {
        dataManager.register(entry.getKey(), entry.getValue());
    }
    //#endif

    @SuppressWarnings("unchecked")
    private byte[] getPacketData(Packet packet) throws Exception {
        //#if MC>=10904
        if (packet instanceof SPacketSpawnMob) {
            SPacketSpawnMob p = (SPacketSpawnMob) packet;
            if (p.dataManager == null) {
                p.dataManager = new EntityDataManager(null);
                if (p.getDataManagerEntries() != null) {
                    for (EntityDataManager.DataEntry<?> entry : p.getDataManagerEntries()) {
                        DataManager_set(p.dataManager, entry);
                    }
                }
            }
        }

        if (packet instanceof SPacketSpawnPlayer) {
            SPacketSpawnPlayer p = (SPacketSpawnPlayer) packet;
            if (p.watcher == null) {
                p.watcher = new EntityDataManager(null);
                if (p.getDataManagerEntries() != null) {
                    for (EntityDataManager.DataEntry<?> entry : p.getDataManagerEntries()) {
                        DataManager_set(p.watcher, entry);
                    }
                }
            }
        }
        //#else
        //$$ if(packet instanceof S0FPacketSpawnMob) {
        //$$     S0FPacketSpawnMob p = (S0FPacketSpawnMob) packet;
        //$$     if (p.field_149043_l == null) {
        //$$         p.field_149043_l = new DataWatcher(null);
        //$$         if(p.func_149027_c() != null) {
        //$$             for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_149027_c()) {
        //$$                 p.field_149043_l.addObject(wo.getDataValueId(), wo.getObject());
        //$$             }
        //$$         }
        //$$     }
        //$$ }
        //$$
        //$$ if(packet instanceof S0CPacketSpawnPlayer) {
        //$$     S0CPacketSpawnPlayer p = (S0CPacketSpawnPlayer) packet;
            //#if MC>=10809
            //$$ if (p.watcher == null) {
            //$$     p.watcher = new DataWatcher(null);
            //$$     if(p.func_148944_c() != null) {
            //$$         for(DataWatcher.WatchableObject wo : p.func_148944_c()) {
            //$$             p.watcher.addObject(wo.getDataValueId(), wo.getObject());
            //#else
            //$$ if (p.field_148960_i == null) {
            //$$     p.field_148960_i = new DataWatcher(null);
            //$$     if(p.func_148944_c() != null) {
            //$$         for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_148944_c()) {
            //$$             p.field_148960_i.addObject(wo.getDataValueId(), wo.getObject());
            //#endif
        //$$             }
        //$$         }
        //$$     }
        //$$ }
        //#endif

        //#if MC>=10800
        Integer packetId = connectionState.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);
        //#else
        //$$ Integer packetId = (Integer) connectionState.func_150755_b().inverse().get(packet.getClass());
        //#endif
        if (packetId == null) {
            throw new IOException("Unknown packet type:" + packet.getClass());
        }
        ByteBuf byteBuf = Unpooled.buffer();
        PacketBuffer packetBuffer = new PacketBuffer(byteBuf);
        writeVarInt(packetBuffer, packetId);
        packet.writePacketData(packetBuffer);

        byteBuf.readerIndex(0);
        byte[] array = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(array);

        byteBuf.release();
        return array;
    }

    public void addMarker() {
        Entity view = getRenderViewEntity(mc);
        int timestamp = (int) (System.currentTimeMillis() - startTime);

        Marker marker = new Marker();
        marker.setTime(timestamp);
        marker.setX(view.posX);
        marker.setY(view.posY);
        marker.setZ(view.posZ);
        marker.setYaw(view.rotationYaw);
        marker.setPitch(view.rotationPitch);
        // Roll is always 0
        saveService.submit(() -> {
            synchronized (replayFile) {
                try {
                    Set<Marker> markers = replayFile.getMarkers().or(HashSet::new);
                    markers.add(marker);
                    replayFile.writeMarkers(markers);
                } catch (IOException e) {
                    logger.error("Writing markers:", e);
                }
            }
        });
    }

    public void setServerWasPaused() {
        this.serverWasPaused = true;
    }
}
