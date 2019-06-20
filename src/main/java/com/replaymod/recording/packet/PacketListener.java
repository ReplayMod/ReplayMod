package com.replaymod.recording.packet;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.MCVer;
import com.replaymod.editor.gui.MarkerProcessor;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.Setting;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.recording.mixin.SPacketSpawnMobAccessor;
import com.replaymod.recording.mixin.SPacketSpawnPlayerAccessor;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.utils.Colors;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.client.network.packet.*;
import net.minecraft.network.chat.TextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11300
import net.minecraft.client.network.packet.LoginSuccessS2CPacket;
//#else
//$$ import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
//#endif

//#if MC>=10904
//#else
//$$ import java.util.List;
//#endif

//#if MC>=10800
import net.minecraft.network.NetworkSide;
//#endif

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
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

    private static final MinecraftClient mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final ReplayMod core;
    private final Path outputPath;
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
    private NetworkState connectionState = NetworkState.LOGIN;
    //#else
    //$$ private EnumConnectionState connectionState = EnumConnectionState.PLAY;
    //#endif

    /**
     * Used to keep track of the last metadata save job submitted to the save service and
     * as such prevents unnecessary writes.
     */
    private final AtomicInteger lastSaveMetaDataId = new AtomicInteger();

    public PacketListener(ReplayMod core, Path outputPath, ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.core = core;
        this.outputPath = outputPath;
        this.replayFile = replayFile;
        this.metaData = metaData;
        this.resourcePackRecorder = new ResourcePackRecorder(replayFile);
        // Note: doesn't actually always include the login phase, see `connectionState` field instead.
        this.packetOutputStream = new DataOutputStream(replayFile.writePacketData(true));
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
                    if (ReplayMod.isMinimalMode()) {
                        metaData.setFileFormat("MCPR");
                        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
                        metaData.setProtocolVersion(MCVer.getProtocolVersion());
                        metaData.setGenerator("ReplayMod in Minimal Mode");

                        try (OutputStream out = replayFile.write("metaData.json")) {
                            String json = (new Gson()).toJson(metaData);
                            out.write(json.getBytes());
                        }
                    } else {
                        replayFile.writeMetaData(metaData);
                    }
                }
            } catch (IOException e) {
                logger.error("Writing metadata:", e);
            }
        });
    }

    public void save(Packet packet) {
        try {
            if(packet instanceof PlayerSpawnS2CPacket) {
                //#if MC>=10800
                UUID uuid = ((PlayerSpawnS2CPacket) packet).getPlayerUuid();
                //#else
                //$$ UUID uuid = ((S0CPacketSpawnPlayer) packet).func_148948_e().getId();
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
            if (packet instanceof LoginSuccessS2CPacket) {
                connectionState = NetworkState.PLAY;
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

        core.runLater(() -> {
            ConnectionEventHandler connectionEventHandler = ReplayModRecording.instance.getConnectionEventHandler();
            if (connectionEventHandler.getPacketListener() == this) {
                connectionEventHandler.reset();
            }
        });

        GuiLabel savingLabel = new GuiLabel().setI18nText("replaymod.gui.replaysaving.title").setColor(Colors.BLACK);
        new Thread(() -> {
            core.runLater(() -> core.getBackgroundProcesses().addProcess(savingLabel));

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

                    if (core.getSettingsRegistry().get(Setting.AUTO_POST_PROCESS) && !ReplayMod.isMinimalMode()) {
                        MarkerProcessor.apply(outputPath, progress -> {});
                    }
                } catch (IOException e) {
                    logger.error("Saving replay file:", e);
                }
            }

            core.runLater(() -> core.getBackgroundProcesses().removeProcess(savingLabel));
        }).start();
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
                if(packet instanceof ItemPickupAnimationS2CPacket) {
                    if(mc.player != null ||
                            ((ItemPickupAnimationS2CPacket) packet).getEntityId() == mc.player.getEntityId()) {
                //#else
                //$$ if(packet instanceof S0DPacketCollectItem) {
                //$$     if(mc.thePlayer != null || ((S0DPacketCollectItem) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                //#endif
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                //#if MC>=10800
                if (packet instanceof ResourcePackSendS2CPacket) {
                    save(resourcePackRecorder.handleResourcePack((ResourcePackSendS2CPacket) packet));
                    return;
                }
                //#else
                //$$ if (packet instanceof S3FPacketCustomPayload) {
                //$$     S3FPacketCustomPayload p = (S3FPacketCustomPayload) packet;
                //$$     if ("MC|RPack".equals(p.func_149169_c())) {
                //$$         save(resourcePackRecorder.handleResourcePack(p));
                //$$         return;
                //$$     }
                //$$ }
                //#endif

                //#if MC<11300
                //$$ if (packet instanceof FMLProxyPacket) {
                //$$     // This packet requires special handling
                    //#if MC>=10800
                    //$$ ((FMLProxyPacket) packet).toS3FPackets().forEach(this::save);
                    //#else
                    //$$ save(((FMLProxyPacket) packet).toS3FPacket());
                    //#endif
                //$$     super.channelRead(ctx, msg);
                //$$     return;
                //$$ }
                //#endif

                save(packet);

                if (packet instanceof CustomPayloadS2CPacket) {
                    CustomPayloadS2CPacket p = (CustomPayloadS2CPacket) packet;
                    if (Restrictions.PLUGIN_CHANNEL.equals(p.getChannel())) {
                        packet = new DisconnectS2CPacket(new TextComponent("Please update to view this replay."));
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
    private <T> void DataManager_set(DataTracker dataManager, DataTracker.Entry<T> entry) {
        dataManager.startTracking(entry.getData(), entry.get());
    }
    //#endif

    @SuppressWarnings("unchecked")
    private byte[] getPacketData(Packet packet) throws Exception {
        if (packet instanceof MobSpawnS2CPacket) {
            MobSpawnS2CPacket p = (MobSpawnS2CPacket) packet;
            SPacketSpawnMobAccessor pa = (SPacketSpawnMobAccessor) p;
            if (pa.getDataManager() == null) {
                pa.setDataManager(new DataTracker(null));
                if (p.getTrackedValues() != null) {
                    //#if MC>=10904
                    for (DataTracker.Entry<?> entry : p.getTrackedValues()) {
                        DataManager_set(pa.getDataManager(), entry);
                    }
                    //#else
                    //$$ for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_149027_c()) {
                    //$$     pa.getDataManager().addObject(wo.getDataValueId(), wo.getObject());
                    //$$ }
                    //#endif
                }
            }
        }

        if (packet instanceof PlayerSpawnS2CPacket) {
            PlayerSpawnS2CPacket p = (PlayerSpawnS2CPacket) packet;
            SPacketSpawnPlayerAccessor pa = (SPacketSpawnPlayerAccessor) p;
            if (pa.getDataManager() == null) {
                pa.setDataManager(new DataTracker(null));
                if (p.getTrackedValues() != null) {
                    //#if MC>=10904
                    for (DataTracker.Entry<?> entry : p.getTrackedValues()) {
                        DataManager_set(pa.getDataManager(), entry);
                    }
                    //#else
                    //$$ for(DataWatcher.WatchableObject wo : (List<DataWatcher.WatchableObject>) p.func_148944_c()) {
                    //$$     pa.getDataManager().addObject(wo.getDataValueId(), wo.getObject());
                    //$$ }
                    //#endif
                }
            }
        }

        //#if MC>=10800
        Integer packetId = connectionState.getPacketId(NetworkSide.CLIENTBOUND, packet);
        //#else
        //$$ Integer packetId = (Integer) connectionState.func_150755_b().inverse().get(packet.getClass());
        //#endif
        if (packetId == null) {
            throw new IOException("Unknown packet type:" + packet.getClass());
        }
        ByteBuf byteBuf = Unpooled.buffer();
        PacketByteBuf packetBuffer = new PacketByteBuf(byteBuf);
        packetBuffer.writeVarInt(packetId);
        packet.write(packetBuffer);

        byteBuf.readerIndex(0);
        byte[] array = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(array);

        byteBuf.release();
        return array;
    }

    public void addMarker(String name) {
        addMarker(name, (int) getCurrentDuration());
    }

    public void addMarker(String name, int timestamp) {
        Entity view = getRenderViewEntity(mc);

        Marker marker = new Marker();
        marker.setName(name);
        marker.setTime(timestamp);
        marker.setX(view.x);
        marker.setY(view.y);
        marker.setZ(view.z);
        marker.setYaw(view.yaw);
        marker.setPitch(view.pitch);
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

    public long getCurrentDuration() {
        return lastSentPacket;
    }

    public void setServerWasPaused() {
        this.serverWasPaused = true;
    }
}
