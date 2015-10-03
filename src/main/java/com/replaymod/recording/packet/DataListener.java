package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import de.johni0702.replaystudio.data.Marker;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class DataListener extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger();

    private final ReplayFile replayFile;
    protected final DataWriter dataWriter;

    protected Long startTime = null;
    protected String name;
    protected String worldName;
    public boolean serverWasPaused;
    protected long lastSentPacket = 0;
    protected boolean alive = true;
    protected Set<String> players = new HashSet<>();
    private boolean singleplayer;

    private final Set<Marker> markers = new HashSet<>();
    private final Map<Integer, String> requestToHash = new ConcurrentHashMap<>();

    public DataListener(ReplayFile replayFile, String name, String worldName, long startTime, boolean singleplayer) throws IOException {
        this.replayFile = replayFile;
        this.startTime = startTime;
        this.name = name;
        this.worldName = worldName;
        this.singleplayer = singleplayer;

        dataWriter = new DataWriter();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        dataWriter.requestFinish();
    }

    protected void recordResourcePack(File file, int requestId) {
        try {
            String hash = Hashing.sha1().hashBytes(Files.toByteArray(file)).toString();
            synchronized (dataWriter) {
                if (!requestToHash.containsValue(hash)) {
                    try (OutputStream out = replayFile.writeResourcePack(hash)) {
                        FileUtils.copyFile(file, out);
                    }
                }
            }
            requestToHash.put(requestId, hash);
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    public void addMarker() {
        AdvancedPosition pos = new AdvancedPosition(Minecraft.getMinecraft().getRenderViewEntity());
        int timestamp = (int) (System.currentTimeMillis() - startTime);

        Marker marker = new Marker();
        marker.setTime(timestamp);
        marker.setX(pos.getX());
        marker.setY(pos.getY());
        marker.setZ(pos.getZ());
        marker.setYaw((float) pos.getYaw());
        marker.setPitch((float) pos.getPitch());
        marker.setRoll((float) pos.getRoll());
        markers.add(marker);

        ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.addedmarker", ChatMessageHandler.ChatMessageType.INFORMATION);
    }

    public class DataWriter {
        private long paused;

        private DataOutputStream stream;
        private ExecutorService saveService = Executors.newSingleThreadExecutor();

        private final Thread shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!saveService.isShutdown()) {
                    requestFinish();
                }
            }
        }, "shutdown-hook-data-listener");

        public DataWriter() throws IOException {
            stream = new DataOutputStream(replayFile.writePacketData());
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        public synchronized void writePacket(final byte[] bytes) {
            long now = System.currentTimeMillis();
            if(startTime == null) {
                startTime = now;
            }

            if (serverWasPaused) {
                paused = now - startTime - lastSentPacket;
                serverWasPaused = false;
            }
            final int timestamp = (int) (now - startTime - paused);
            lastSentPacket = timestamp;
            saveService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        stream.writeInt(timestamp);
                        stream.writeInt(bytes.length);
                        stream.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        public synchronized void requestFinish() {
            if (saveService.isShutdown()) return;
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                saveService.shutdown();
                saveService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                stream.close();

                // TODO: Get rid of replay file appender
                ReplayMod.replayFileAppender.startNewReplayFileWriting();

                String mcversion = ReplayMod.getMinecraftVersion();

                String[] pl = players.toArray(new String[players.size()]);

                String generator = "ReplayMod v" + ReplayMod.getContainer().getVersion();

                ReplayMetaData metaData = new ReplayMetaData();
                metaData.setSingleplayer(singleplayer);
                metaData.setServerName(worldName);
                metaData.setGenerator(generator);
                metaData.setDuration((int) lastSentPacket);
                metaData.setDate(startTime);
                metaData.setPlayers(pl);
                metaData.setMcVersion(mcversion);

                replayFile.writeMetaData(metaData);
                replayFile.writeMarkers(markers);
                replayFile.writeResourcePackIndex(requestToHash);
                replayFile.save();
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                ReplayMod.replayFileAppender.replayFileWritingFinished();
            }
        }

    }

}
