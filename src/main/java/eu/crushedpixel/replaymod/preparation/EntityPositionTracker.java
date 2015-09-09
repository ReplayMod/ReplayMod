package eu.crushedpixel.replaymod.preparation;

import de.johni0702.replaystudio.PacketData;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.replay.ZipReplayFile;
import de.johni0702.replaystudio.studio.ReplayStudio;
import de.johni0702.replaystudio.util.PacketUtils;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.protocol.data.game.values.entity.player.PositionElement;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityMovementPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.*;
import org.spacehq.packetlib.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An EntityPositionTracker knows every Entity's position at any timestamp for a single Replay File.
 * To do so, it once reads the whole Replay File and stores all Packets that set or change an Entity's position into Memory.<br>
 *     <br>
 * While this significantly increases the Replay loading time, it's the only way to:<br>
 *     1) Properly preview the Camera Path when an Entity is spectated<br>
 *     2) Calculate a smooth Path from an Entity's Shoulder Cam perspective
 */
public class EntityPositionTracker {

    private static final List<Class> MOVEMENT_PACKETS = new ArrayList<Class>() {
        {
            add(ServerPlayerPositionRotationPacket.class);
            add(ServerSpawnPlayerPacket.class);
            add(ServerSpawnObjectPacket.class);
            add(ServerSpawnMobPacket.class);
            add(ServerSpawnPaintingPacket.class);
            add(ServerSpawnExpOrbPacket.class);
            add(ServerEntityMovementPacket.class);
            add(ServerEntityPositionRotationPacket.class);
            add(ServerEntityPositionPacket.class);
            add(ServerEntityTeleportPacket.class);
        }
    };

    private final File replayFile;
    private boolean loaded = false;

    private Map<Integer, List<Pair<Integer, AdvancedPosition>>> entityPositions;

    public EntityPositionTracker(File replayFile) {
        this.replayFile = replayFile;
        this.entityPositions = new HashMap<Integer, List<Pair<Integer, AdvancedPosition>>>();
    }

    public void load() throws IOException {
        ReplayStudio studio = new ReplayStudio();
        studio.setWrappingEnabled(false);

        ZipReplayFile zrf = new ZipReplayFile(studio, replayFile);
        ReplayInputStream rin = zrf.getPacketData();

        PacketData packetData;
        while((packetData = rin.readPacket()) != null) {
            Packet packet = packetData.getPacket();

            if(!MOVEMENT_PACKETS.contains(packet.getClass())) continue;

            Integer entityID = PacketUtils.getEntityId(packet);
            if(entityID == null) continue;

            AdvancedPosition oldPosition = new AdvancedPosition(0, 0, 0, 0, 0);

            List<Pair<Integer, AdvancedPosition>> positions = entityPositions.get(entityID);

            if(positions == null) positions = new ArrayList<Pair<Integer, AdvancedPosition>>();
            if(!positions.isEmpty()) {
                oldPosition = positions.get(positions.size() - 1).getRight();
            }

            AdvancedPosition newPosition = getNewPosition(oldPosition, packet);

            if(newPosition != null) positions.add(Pair.of((int) packetData.getTime(), newPosition));
            entityPositions.put(entityID, positions);
        }

        loaded = true;
    }

    /**
     * @param entityID The Entity's ID
     * @param timestamp The timestamp
     * @return The specified Entity's position at the given timestamp, or null if the entity doesn't exist at that timestamp
     * @throws IllegalStateException if the EntityPositionTracker's load() method wasn't called yet
     */
    public AdvancedPosition getEntityPositionAtTimestamp(int entityID, int timestamp) {
        if(!loaded) throw new IllegalStateException("The EntityPositionTracker hasn't loaded the Replay File yet");

        List<Pair<Integer, AdvancedPosition>> positions = entityPositions.get(entityID);
        if(positions == null || positions.isEmpty()) return null;

        boolean exists = false;

        for(Pair<Integer, AdvancedPosition> pair : positions) {
            if(pair.getKey() <= timestamp) exists = true;
            if(pair.getKey() >= timestamp) {
                if(exists) return pair.getValue();
                return null;
            }
        }

        return null;
    }

    //@johni0702 - you may consider adding a similar method to the ReplayStudio PacketUtils
    private AdvancedPosition getNewPosition(AdvancedPosition previous, Packet packet) {
        if(packet instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket ppl = (ServerPlayerPositionRotationPacket)packet;

            double x = previous.getX() + ppl.getX();
            double y = previous.getY() + ppl.getY();
            double z = previous.getZ() + ppl.getZ();
            double yaw = previous.getYaw() + ppl.getYaw();
            double pitch = previous.getPitch() + ppl.getPitch();

            for(PositionElement relative : ppl.getRelativeElements()) {
                if(relative == PositionElement.X) {
                    x -= previous.getX();
                } else if(relative == PositionElement.Y) {
                    y -= previous.getY();
                } else if(relative == PositionElement.Z) {
                    z -= previous.getZ();
                } else if(relative == PositionElement.YAW) {
                    yaw -= previous.getYaw();
                } else if(relative == PositionElement.PITCH) {
                    pitch -= previous.getPitch();
                }
            }

            return new AdvancedPosition(x, y, z, pitch, yaw);
        }

        if(packet instanceof ServerSpawnPlayerPacket) {
            ServerSpawnPlayerPacket spp = (ServerSpawnPlayerPacket)packet;
            return new AdvancedPosition(spp.getX(), spp.getY(), spp.getZ(), spp.getPitch(), spp.getYaw());
        }

        if(packet instanceof ServerSpawnObjectPacket) {
            ServerSpawnObjectPacket spp = (ServerSpawnObjectPacket)packet;
            return new AdvancedPosition(spp.getX(), spp.getY(), spp.getZ(), spp.getPitch(), spp.getYaw());
        }

        if(packet instanceof ServerSpawnExpOrbPacket) {
            ServerSpawnExpOrbPacket spp = (ServerSpawnExpOrbPacket)packet;
            return new AdvancedPosition(spp.getX(), spp.getY(), spp.getZ(), 0, 0);
        }

        if(packet instanceof ServerSpawnMobPacket) {
            ServerSpawnMobPacket spp = (ServerSpawnMobPacket)packet;
            return new AdvancedPosition(spp.getX(), spp.getY(), spp.getZ(), spp.getPitch(), spp.getYaw());
        }

        if(packet instanceof ServerSpawnPaintingPacket) {
            ServerSpawnPaintingPacket spp = (ServerSpawnPaintingPacket)packet;
            return new AdvancedPosition(spp.getPosition().getX(), spp.getPosition().getY(), spp.getPosition().getZ(), 0, 0);
        }

        if(packet instanceof ServerEntityMovementPacket) {
            ServerEntityMovementPacket ppl = (ServerEntityMovementPacket)packet;
            //why is the rot/pos variable of ServerEntityMovementPacket not accessible?
            boolean rot = ppl instanceof ServerEntityPositionRotationPacket;

            double x = previous.getX() + ppl.getMovementX();
            double y = previous.getY() + ppl.getMovementY();
            double z = previous.getZ() + ppl.getMovementZ();

            double pitch = previous.getPitch();
            double yaw = previous.getYaw();

            if(rot) {
                pitch = ppl.getPitch();
                yaw = ppl.getYaw();
            }

            return new AdvancedPosition(x, y, z, pitch, yaw);
        }

        if(packet instanceof ServerEntityTeleportPacket) {
            ServerEntityTeleportPacket spp = (ServerEntityTeleportPacket)packet;
            return new AdvancedPosition(spp.getX(), spp.getY(), spp.getZ(), spp.getPitch(), spp.getYaw());
        }

        return null;
    }
}