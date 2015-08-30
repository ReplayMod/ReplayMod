package eu.crushedpixel.replaymod.studio;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;
import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.filter.StreamFilter;
import de.johni0702.replaystudio.replay.ReplayFile;
import de.johni0702.replaystudio.replay.ReplayMetaData;
import de.johni0702.replaystudio.stream.PacketStream;
import eu.crushedpixel.replaymod.ReplayMod;
import org.apache.commons.io.IOUtils;
import org.spacehq.mc.protocol.packet.ingame.server.ServerResourcePackSendPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.*;
import java.util.*;

// TODO Reset resourcepack when changing from a resourcepack replay to a normal replay
public class ConnectMetadataFilter implements StreamFilter {
    private ReplayFile replayFile;
    private Set<String> players = new HashSet<String>();
    private Set<UUID> invisiblePlayers = new HashSet<UUID>();
    private long duration;
    private Map<String, File> resourcePacks = new HashMap<String, File>();
    private Map<Integer, String> resourcePackIndex = new HashMap<Integer, String>();

    @Override
    public String getName() {
        return "connect_metadata";
    }

    public void setInputReplay(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    @Override
    public void init(Studio studio, JsonObject jsonObject) {

    }

    @Override
    public void onStart(PacketStream packetStream) {
        try {
            Optional<Set<UUID>> invisible = replayFile.getInvisiblePlayers();
            if (invisible.isPresent()) {
                invisiblePlayers.addAll(invisible.get());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onPacket(PacketStream packetStream, PacketData packetData) {
        Packet packet = packetData.getPacket();
        if (packet instanceof ServerSpawnPlayerPacket) {
            players.add(((ServerSpawnPlayerPacket) packet).getUUID().toString());
        }
        if (packet instanceof ServerResourcePackSendPacket) {
            String url = ((ServerResourcePackSendPacket) packet).getUrl();
            if (url.startsWith("replay://")) {
                int id = Integer.parseInt(url.substring("replay://".length()));
                try {
                    Map<Integer, String> oldIndex = replayFile.getResourcePackIndex();
                    if (oldIndex != null) {
                        String hash = oldIndex.get(id);
                        if (hash != null) {
                            // Make sure we have the resource pack
                            File file = resourcePacks.get(hash);
                            if (file == null) {
                                Optional<InputStream> in = replayFile.getResourcePack(hash);
                                if (in.isPresent()) {
                                    try {
                                        file = File.createTempFile("replaymod", "resourcepack");
                                        OutputStream out = new FileOutputStream(file);
                                        try {
                                            IOUtils.copy(in.get(), out);
                                        } finally {
                                            IOUtils.closeQuietly(out);
                                        }
                                    } finally {
                                        IOUtils.closeQuietly(in.get());
                                    }
                                    resourcePacks.put(hash, file);
                                }
                            }

                            // Re-Index the current packet
                            id = resourcePackIndex.size();
                            resourcePackIndex.put(id, hash);
                            url = "replay://" + id;
                            packetStream.insert(packetData.getTime(), new ServerResourcePackSendPacket(url, ""));
                            return false;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    @Override
    public void onEnd(PacketStream packetStream, long l) {
        if (l > duration) {
            duration = l;
        }
    }

    public void writeTo(ReplayFile output) throws IOException {
        ReplayMetaData metaData = new ReplayMetaData();
        metaData.setDuration((int) duration);
        metaData.setSingleplayer(false);
        metaData.setServerName("Multiple worlds");
        metaData.setMcVersion(ReplayMod.getMinecraftVersion());
        metaData.setDate(System.currentTimeMillis());
        metaData.setPlayers(players.toArray(new String[players.size()]));
        output.writeMetaData(metaData);

        if (!invisiblePlayers.isEmpty()) {
            output.writeInvisiblePlayers(invisiblePlayers);
        }

        if (!resourcePackIndex.isEmpty()) {
            output.writeResourcePackIndex(resourcePackIndex);

            // Only store resource packs we really need
            resourcePacks.keySet().retainAll(resourcePackIndex.values());

            for (Map.Entry<String, File> e : resourcePacks.entrySet()) {
                OutputStream out = output.writeResourcePack(e.getKey());
                try {
                    InputStream in = new FileInputStream(e.getValue());
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        }
    }
}
