//#if MC>=10904
package com.replaymod.replay;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.chunk.BlockStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.mc.protocol.util.NetUtil;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.Utils;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.replaystudio.util.PacketUtils;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.NetworkState;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;

//#if MC>=11400
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
//#else
//$$ import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//$$
//$$ import static com.replaymod.core.versions.MCVer.FML_BUS;
//#endif

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

//#if MC>=11200
import com.replaymod.core.utils.WrappedTimer;
//#endif

import static com.replaymod.core.versions.MCVer.getMinecraft;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

/**
 * Sends only chunk updates and entity position updates but tries to do so as quickly as possible.
 * To do so, it performs an initial analysis of the replay, scanning all of its packets and storing entity positions
 * and chunk states while doing so.
 * This allows it to later jump to any time by doing a diff from the current time (including backwards jumping).
 */
@ChannelHandler.Sharable
public class QuickReplaySender extends ChannelHandlerAdapter implements ReplaySender {
    private static final String CACHE_ENTRY = "quickModeCache.bin";
    private static final String CACHE_INDEX_ENTRY = "quickModeCacheIndex.bin";
    private static final int CACHE_VERSION = 0;

    private final MinecraftClient mc = getMinecraft();

    private final ReplayModReplay mod;
    private final ReplayFile replayFile;
    private final EventHandler eventHandler = new EventHandler();
    private ChannelHandlerContext ctx;

    private int currentTimeStamp;
    private double replaySpeed = 1;

    /**
     * Whether async mode is enabled.
     * Async mode is emulated by registering an event handler on client tick.
     */
    private boolean asyncMode;
    private long lastAsyncUpdateTime;

    private ListenableFuture<Void> initPromise;

    private com.github.steveice10.netty.buffer.ByteBuf buf;
    private NetInput bufInput;
    private TreeMap<Integer, Collection<BakedTrackedThing>> thingSpawnsT = new TreeMap<>();
    private ListMultimap<Integer, BakedTrackedThing> thingSpawns = Multimaps.newListMultimap(thingSpawnsT, ArrayList::new);
    private TreeMap<Integer, Collection<BakedTrackedThing>> thingDespawnsT = new TreeMap<>();
    private ListMultimap<Integer, BakedTrackedThing> thingDespawns = Multimaps.newListMultimap(thingDespawnsT, ArrayList::new);
    private List<BakedTrackedThing> activeThings = new LinkedList<>();
    private TreeMap<Integer, Packet<?>> worldTimes = new TreeMap<>();
    private TreeMap<Integer, Packet<?>> thunderStrengths = new TreeMap<>(); // For some reason, this isn't tied to Weather

    public QuickReplaySender(ReplayModReplay mod, ReplayFile replayFile) {
        this.mod = mod;
        this.replayFile = replayFile;
    }

    public void register() {
        eventHandler.register();
    }

    public void unregister() {
        eventHandler.unregister();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public ListenableFuture<Void> getInitializationPromise() {
        return initPromise;
    }

    public ListenableFuture<Void> initialize(Consumer<Double> progress) {
        if (initPromise != null) {
            return initPromise;
        }
        SettableFuture<Void> promise = SettableFuture.create();
        initPromise = promise;
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                if (!tryLoadFromCache(progress)) {
                    double progressSplit = 0.9; // 90% of progress time for analysing, 10% for loading
                    analyseReplay(replayFile, d -> progress.accept(d * progressSplit));
                    tryLoadFromCache(d -> progress.accept(d * (1 - progressSplit) + progressSplit));
                }
                LOGGER.info("Initialized quick replay sender in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Throwable e) {
                LOGGER.error("Initializing quick replay sender:", e);
                mod.getCore().runLater(() -> {
                    mod.getCore().printWarningToChat("Error initializing quick replay sender: %s", e.getLocalizedMessage());
                    promise.setException(e);
                });
                return;
            }
            mod.getCore().runLater(() -> promise.set(null));
        }).start();
        return promise;
    }

    private void ensureInitialized(Runnable body) {
        if (initPromise == null) {
            LOGGER.warn("QuickReplaySender used without prior initialization!", new Throwable());
            initialize(progress -> {});
        }
        Futures.addCallback(initPromise, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                body.run();
            }

            @Override
            public void onFailure(Throwable t) {
                // Error already printed by initialize method
            }
        });
    }

    public void restart() {
        activeThings.clear();
        currentTimeStamp = 0;
        ctx.fireChannelRead(toMC(new ServerRespawnPacket(
                0,
                //#if MC<11400
                //$$ Difficulty.NORMAL,
                //#endif
                GameMode.SPECTATOR,
                WorldType.DEFAULT
        )));
        ctx.fireChannelRead(toMC(new ServerPlayerPositionRotationPacket(0, 0, 0, 0, 0, 0)));
    }

    @Override
    public int currentTimeStamp() {
        return currentTimeStamp;
    }

    @Override
    public void setReplaySpeed(double factor) {
        if (factor != 0) {
            if (paused() && asyncMode) {
                lastAsyncUpdateTime = System.currentTimeMillis();
            }
            this.replaySpeed = factor;
        }
        TimerAccessor timer = (TimerAccessor) ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11200
        timer.setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK / (float) factor);
        //#else
        //$$ timer.setTimerSpeed((float) factor);
        //#endif
    }

    @Override
    public double getReplaySpeed() {
        return replaySpeed;
    }

    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }

    @Override
    public void setAsyncMode(boolean async) {
        if (this.asyncMode == async) return;
        ensureInitialized(() -> {
            this.asyncMode = async;
            if (async) {
                lastAsyncUpdateTime = System.currentTimeMillis();
            }
        });
    }

    @Override
    public void setSyncModeAndWait() {
        setAsyncMode(false);
        // No waiting required, we emulated async mode via tick events
    }

    @Override
    public void jumpToTime(int value) {
        sendPacketsTill(value);
    }

    private class EventHandler extends EventRegistrations {
        //#if MC>=11400
        { on(PreTickCallback.EVENT, this::onTick); }
        private void onTick() {
        //#else
        //$$ @SubscribeEvent
        //$$ public void onTick(TickEvent.ClientTickEvent event) {
        //$$     if (event.phase != TickEvent.Phase.START) return;
        //#endif
            if (!asyncMode) return;

            long now = System.currentTimeMillis();
            long realTimePassed = now - lastAsyncUpdateTime;
            lastAsyncUpdateTime = now;
            int replayTimePassed = (int) (realTimePassed * replaySpeed);
            sendPacketsTill(currentTimeStamp + replayTimePassed);
        }
    }

    private boolean tryLoadFromCache(Consumer<Double> progress) throws IOException {
        boolean success = false;

        Optional<InputStream> cacheIndexOpt = replayFile.getCache(CACHE_INDEX_ENTRY);
        if (!cacheIndexOpt.isPresent()) return false;
        try (InputStream indexIn = cacheIndexOpt.get()) {
            Optional<InputStream> cacheOpt = replayFile.getCache(CACHE_ENTRY);
            if (!cacheOpt.isPresent()) return false;
            try (InputStream cacheIn = cacheOpt.get()) {
                success = loadFromCache(cacheIn, indexIn, progress);
            }
        } finally {
            if (!success) {
                buf = null;
                bufInput = null;
                thingSpawnsT.clear();
                thingDespawnsT.clear();
                worldTimes.clear();
                thunderStrengths.clear();
            }
        }

        return success;
    }

    private boolean loadFromCache(InputStream cacheIn, InputStream rawIndexIn, Consumer<Double> progress) throws IOException {
        long sysTimeStart = System.currentTimeMillis();

        NetInput in = new StreamNetInput(rawIndexIn);
        if (in.readVarInt() != CACHE_VERSION) return false; // Incompatible cache version
        if (new StreamNetInput(cacheIn).readVarInt() != CACHE_VERSION) return false; // Incompatible cache version

        things: while (true) {
            BakedTrackedThing trackedThing;
            switch (in.readVarInt()) {
                case 0: break things;
                case 1: trackedThing = new BakedEntity(in); break;
                case 2: trackedThing = new BakedChunk(in); break;
                case 3: trackedThing = new BakedWeather(in); break;
                default: return false;
            }
            thingSpawns.put(trackedThing.spawnTime, trackedThing);
            thingDespawns.put(trackedThing.despawnTime, trackedThing);
        }

        readFromCache(in, worldTimes);
        readFromCache(in, thunderStrengths);
        int size = in.readVarInt();

        LOGGER.info("Creating quick mode buffer of size: {}KB", size / 1024);
        buf = com.github.steveice10.netty.buffer.Unpooled.buffer(size);
        int read = 0;
        while (true) {
            int len = buf.writeBytes(cacheIn, Math.min(size - read, 4096));
            if (len <= 0) break;
            read += len;
            progress.accept((double) read / size);
        }
        bufInput = new ByteBufNetInput(buf);

        LOGGER.info("Loaded quick replay from cache in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
        return true;
    }

    private static void analyseReplay(ReplayFile replayFile, Consumer<Double> progress) throws IOException {
        ReplayStudio studio = new ReplayStudio();
        PacketUtils.registerAllMovementRelated(studio);
        studio.setParsing(ServerSpawnMobPacket.class, true);
        studio.setParsing(ServerSpawnObjectPacket.class, true);
        studio.setParsing(ServerSpawnPaintingPacket.class, true);
        studio.setParsing(ServerSpawnPlayerPacket.class, true);
        studio.setParsing(ServerEntityDestroyPacket.class, true);
        studio.setParsing(ServerChunkDataPacket.class, true);
        studio.setParsing(ServerUnloadChunkPacket.class, true);
        studio.setParsing(ServerBlockChangePacket.class, true);
        studio.setParsing(ServerMultiBlockChangePacket.class, true);
        studio.setParsing(ServerPlayerListEntryPacket.class, true);
        studio.setParsing(ServerUpdateTimePacket.class, true);
        studio.setParsing(ServerNotifyClientPacket.class, true);

        TreeMap<Integer, com.github.steveice10.packetlib.packet.Packet> worldTimes = new TreeMap<>();
        TreeMap<Integer, com.github.steveice10.packetlib.packet.Packet> thunderStrengths = new TreeMap<>();
        Map<UUID, PlayerListEntry> playerListEntries = new HashMap<>();
        Map<Integer, Entity> activeEntities = new HashMap<>();
        Map<Long, Chunk> activeChunks = new HashMap<>();
        Weather activeWeather = null;

        double sysTimeStart = System.currentTimeMillis();
        double duration;
        try (ReplayInputStream in = replayFile.getPacketData(studio, false);
             OutputStream cacheOut = replayFile.writeCache(CACHE_ENTRY);
             OutputStream cacheIndexOut = replayFile.writeCache(CACHE_INDEX_ENTRY)) {
            NetOutput out = new StreamNetOutput(cacheOut);
            out.writeVarInt(CACHE_VERSION);
            NetOutput indexOut = new StreamNetOutput(cacheIndexOut);
            indexOut.writeVarInt(CACHE_VERSION);

            int index = 0;
            int time = 0;
            duration = replayFile.getMetaData().getDuration();
            PacketData packetData;
            while ((packetData = in.readPacket()) != null) {
                com.github.steveice10.packetlib.packet.Packet packet = packetData.getPacket();
                time = (int) packetData.getTime();
                progress.accept(time / duration);
                Integer entityId = PacketUtils.getEntityId(packet);
                if (packet instanceof ServerSpawnMobPacket
                        || packet instanceof ServerSpawnObjectPacket
                        || packet instanceof ServerSpawnPaintingPacket) {
                    Entity entity = new Entity(entityId, Collections.singletonList(packet));
                    entity.spawnTime = time;
                    Entity prev = activeEntities.put(entityId, entity);
                    if (prev != null) {
                        index = prev.writeToCache(indexOut, out, time, index);
                    }
                } else if (packet instanceof ServerSpawnPlayerPacket) {
                    ServerPlayerListEntryPacket listEntryPacket = new ServerPlayerListEntryPacket(
                            PlayerListEntryAction.ADD_PLAYER,
                            new PlayerListEntry[]{
                                    playerListEntries.get(((ServerSpawnPlayerPacket) packet).getUUID())
                            }
                    );
                    Entity entity = new Entity(entityId, Arrays.asList(listEntryPacket, packet));
                    entity.spawnTime = time;
                    Entity prev = activeEntities.put(entityId, entity);
                    if (prev != null) {
                        index = prev.writeToCache(indexOut, out, time, index);
                    }
                } else if (packet instanceof ServerEntityDestroyPacket) {
                    for (int id : ((ServerEntityDestroyPacket) packet).getEntityIds()) {
                        Entity entity = activeEntities.remove(id);
                        if (entity != null) {
                            index = entity.writeToCache(indexOut, out, time, index);
                        }
                    }
                } else if (packet instanceof ServerChunkDataPacket) {
                    Column column = ((ServerChunkDataPacket) packet).getColumn();
                    if (column.hasBiomeData()) {
                        Chunk chunk = new Chunk(column);
                        chunk.spawnTime = time;
                        Chunk prev = activeChunks.put(coordToLong(column.getX(), column.getZ()), chunk);
                        if (prev != null) {
                            index = prev.writeToCache(indexOut, out, time, index);
                        }
                    } else {
                        Chunk chunk = activeChunks.get(coordToLong(column.getX(), column.getZ()));
                        if (chunk != null) {
                            int sectionY = 0;
                            for (com.github.steveice10.mc.protocol.data.game.chunk.Chunk section : column.getChunks()) {
                                if (section == null) {
                                    sectionY++;
                                    continue;
                                }
                                BlockStorage toBlocks = section.getBlocks();
                                BlockStorage fromBlocks = chunk.currentBlockState[sectionY];
                                for (int y = 0; y < 16; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int x = 0; x < 16; x++) {
                                            BlockState fromState = fromBlocks.get(x, y, z);
                                            BlockState toState = toBlocks.get(x, y, z);
                                            if (!fromState.equals(toState)) {
                                                Position pos = new Position(column.getX() << 4 | x, sectionY << 4 | y, column.getZ() << 4 | z);
                                                chunk.blocks.put(time, new BlockChange(pos, fromState, toState));
                                            }
                                        }
                                    }
                                }
                                chunk.currentBlockState[sectionY] = toBlocks;
                                sectionY++;
                            }
                        }
                    }
                } else if (packet instanceof ServerUnloadChunkPacket) {
                    ServerUnloadChunkPacket p = (ServerUnloadChunkPacket) packet;
                    Chunk prev = activeChunks.remove(coordToLong(p.getX(), p.getZ()));
                    if (prev != null) {
                        index = prev.writeToCache(indexOut, out, time, index);
                    }
                } else if (packet instanceof ServerBlockChangePacket || packet instanceof ServerMultiBlockChangePacket) {
                    for (BlockChangeRecord record :
                            packet instanceof ServerBlockChangePacket
                                    ? new BlockChangeRecord[]{ ((ServerBlockChangePacket) packet).getRecord() }
                                    : ((ServerMultiBlockChangePacket) packet).getRecords()) {
                        Position pos = record.getPosition();
                        Chunk chunk = activeChunks.get(coordToLong(pos.getX() >> 4, pos.getZ() >> 4));
                        if (chunk != null) {
                            BlockStorage blockStorage = chunk.currentBlockState[pos.getY() >> 4];
                            int x = pos.getX() & 15, y = pos.getY() & 15, z = pos.getZ() & 15;
                            BlockState prevState = blockStorage.get(x, y, z);
                            BlockState newState = record.getBlock();
                            blockStorage.set(x, y, z, newState);
                            chunk.blocks.put(time, new BlockChange(pos, prevState, newState));
                        }
                    }
                } else if (packet instanceof ServerPlayerListEntryPacket) {
                    ServerPlayerListEntryPacket p = (ServerPlayerListEntryPacket) packet;
                    if (p.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                        for (PlayerListEntry entry : p.getEntries()) {
                            playerListEntries.put(entry.getProfile().getId(), entry);
                        }
                    }
                } else if (packet instanceof ServerRespawnPacket) {
                    for (Entity entity : activeEntities.values()) {
                        index = entity.writeToCache(indexOut, out, time, index);
                    }
                    activeEntities.clear();
                    for (Chunk chunk : activeChunks.values()) {
                        index = chunk.writeToCache(indexOut, out, time, index);
                    }
                    activeChunks.clear();
                    if (activeWeather != null) {
                        index = activeWeather.writeToCache(indexOut, out, time, index);
                    }
                    activeWeather = null;
                } else if (packet instanceof ServerUpdateTimePacket) {
                    worldTimes.put(time, packet);
                } else if (packet instanceof ServerNotifyClientPacket) {
                    ServerNotifyClientPacket p = (ServerNotifyClientPacket) packet;
                    switch (p.getNotification()) {
                        case START_RAIN:
                            if (activeWeather != null) {
                                index = activeWeather.writeToCache(indexOut, out, time, index);
                            }
                            activeWeather = new Weather();
                            activeWeather.spawnTime = time;
                            break;
                        case STOP_RAIN:
                            if (activeWeather != null) {
                                index = activeWeather.writeToCache(indexOut, out, time, index);
                                activeWeather = null;
                            }
                            break;
                        case RAIN_STRENGTH:
                            if (activeWeather != null) {
                                activeWeather.rainStrengths.put(time, packet);
                            }
                            break;
                        case THUNDER_STRENGTH:
                            thunderStrengths.put(time, packet);
                            break;
                        default: break;
                    }
                }
                if (entityId != null) {
                    Entity entity = activeEntities.get(entityId);
                    if (entity != null) {
                        Location current = entity.locations.isEmpty() ? null : entity.locations.lastEntry().getValue();
                        Location updated = PacketUtils.updateLocation(current, packet);
                        if (updated != null) {
                            entity.locations.put(time, updated);
                        }
                    }
                }
            }

            for (Entity entity : activeEntities.values()) {
                index = entity.writeToCache(indexOut, out, time, index);
            }
            for (Chunk chunk : activeChunks.values()) {
                index = chunk.writeToCache(indexOut, out, time, index);
            }
            if (activeWeather != null) {
                index = activeWeather.writeToCache(indexOut, out, time, index);
            }

            indexOut.writeByte(0);
            writeToCache(indexOut, worldTimes);
            writeToCache(indexOut, thunderStrengths);

            indexOut.writeVarInt(index);
        }
        LOGGER.info("Analysed replay in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
    }

    @Override
    public void sendPacketsTill(int replayTime) {
        ensureInitialized(() -> {
            if (replayTime > currentTimeStamp) {
                activeThings.removeIf(thing -> {
                    if (thing.despawnTime <= replayTime) {
                        try {
                            thing.despawn(this, ctx::fireChannelRead);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
                thingSpawnsT.subMap(currentTimeStamp, false, replayTime, true).values()
                        .forEach(things -> things.forEach(thing -> {
                            if (thing.despawnTime > replayTime) {
                                try {
                                    thing.spawn(this, ctx::fireChannelRead);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                activeThings.add(thing);
                            }
                        }));
                activeThings.forEach(thing -> thing.play(currentTimeStamp, replayTime, ctx::fireChannelRead));
                playMap(worldTimes, currentTimeStamp, replayTime, ctx::fireChannelRead);
                playMap(thunderStrengths, currentTimeStamp, replayTime, ctx::fireChannelRead);
            } else {
                activeThings.removeIf(thing -> {
                    if (thing.spawnTime > replayTime) {
                        try {
                            thing.despawn(this, ctx::fireChannelRead);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
                thingDespawnsT.subMap(replayTime, false, currentTimeStamp, true).values()
                        .forEach(things -> things.forEach(thing -> {
                            if (thing.spawnTime <= replayTime) {
                                try {
                                    thing.spawn(this, ctx::fireChannelRead);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                activeThings.add(thing);
                            }
                        }));
                activeThings.forEach(thing -> thing.rewind(currentTimeStamp, replayTime, ctx::fireChannelRead));
                rewindMap(worldTimes, currentTimeStamp, replayTime, ctx::fireChannelRead);
                rewindMap(thunderStrengths, currentTimeStamp, replayTime, ctx::fireChannelRead);
            }
            currentTimeStamp = replayTime;
        });
    }

    private static final com.github.steveice10.netty.buffer.ByteBuf mcplByteBuf = com.github.steveice10.netty.buffer.Unpooled.buffer();
    private static final ByteBufNetInput byteBufNetInput = new ByteBufNetInput(mcplByteBuf);
    private static final ByteBufNetOutput byteBufNetOutput = new ByteBufNetOutput(mcplByteBuf);

    private static BlockStorage copy(BlockStorage of) {
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            of.write(byteBufNetOutput);
            return new BlockStorage(byteBufNetInput);
        } catch (Exception e) {
            Utils.throwIfUnchecked(e);
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
        }
    }

    private static final ByteBuf byteBuf = Unpooled.buffer();
    private static final ByteBufOutputStream byteBufOut = new ByteBufOutputStream(byteBuf);
    private static final PacketByteBuf packetBuf = new PacketByteBuf(byteBuf);
    private static final ReplayOutputStream encoder = new ReplayOutputStream(new ReplayStudio(), byteBufOut);
    private static final Inflater inflater = new Inflater();
    private static final Deflater deflater = new Deflater();

    private static Packet<?> toMC(com.github.steveice10.packetlib.packet.Packet packet) {
        return toMC(packet, NetworkState.field_11690);
    }

    private static Packet<?> toMC(com.github.steveice10.packetlib.packet.Packet packet, NetworkState state) {
        // We need to re-encode MCProtocolLib packets, so we can then decode them as NMS packets
        // The main reason we aren't reading them as NMS packets is that we want ReplayStudio to be able
        // to apply ViaVersion (and potentially other magic) to it.
        synchronized (encoder) {
            int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
            int writerIndex = byteBuf.writerIndex();
            try {
                encoder.write(0, packet); // Re-encode packet, data will end up in byteBuf
                encoder.flush();

                byteBuf.skipBytes(8); // Skip packet length & timestamp

                int packetId = packetBuf.readVarInt();
                Packet<?> mcPacket = state.getPacketHandler(NetworkSide.CLIENTBOUND, packetId);
                mcPacket.read(packetBuf);
                return mcPacket;
            } catch (Exception e) {
                Utils.throwIfUnchecked(e);
                throw new RuntimeException(e);
            } finally {
                byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
                byteBuf.writerIndex(writerIndex);
            }
        }
    }

    private static Packet<?> readPacketFromCache(NetInput in) throws IOException {
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            int prefix = in.readVarInt();
            int len = prefix >> 1;
            if ((prefix & 1) == 1) {
                int fullLen = in.readVarInt();
                byteBuf.writeBytes(in.readBytes(len));
                byteBuf.capacity(byteBuf.writerIndex() + fullLen);

                inflater.setInput(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), len);
                inflater.inflate(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.writerIndex(), fullLen);

                byteBuf.readerIndex(byteBuf.readerIndex() + len);
                byteBuf.writerIndex(byteBuf.writerIndex() + fullLen);
            } else {
                byteBuf.writeBytes(in.readBytes(len));
            }

            int packetId = packetBuf.readVarInt();
            Packet<?> mcPacket = NetworkState.field_11690.getPacketHandler(NetworkSide.CLIENTBOUND, packetId);
            mcPacket.read(packetBuf);
            return mcPacket;
        } catch (Exception e) {
            Utils.throwIfInstanceOf(e, IOException.class);
            Utils.throwIfUnchecked(e);
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
            inflater.reset();
        }
    }

    private static List<Packet<?>> readPacketsFromCache(NetInput in) throws IOException {
        int size = in.readVarInt();
        List<Packet<?>> packets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            packets.add(readPacketFromCache(in));
        }
        return packets;
    }

    private static void readFromCache(NetInput in, SortedMap<Integer, Packet<?>> packets) throws IOException {
        int time = 0;
        for (int i = in.readVarInt(); i > 0; i--) {
            time += in.readVarInt();
            packets.put(time, readPacketFromCache(in));
        }
    }

    private static int writeToCache(NetOutput out, com.github.steveice10.packetlib.packet.Packet packet) throws IOException {
        int readerIndex = byteBuf.readerIndex(); // Mark the current reader and writer index (should be at start)
        int writerIndex = byteBuf.writerIndex();
        try {
            encoder.write(0, packet); // Re-encode packet, data will end up in byteBuf
            encoder.flush();

            byteBuf.skipBytes(8); // Skip packet length & timestamp

            int rawIndex = byteBuf.readerIndex();
            int size = byteBuf.readableBytes();

            byteBuf.ensureWritable(size);
            deflater.setInput(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), size);
            deflater.finish();
            int compressedSize = 0;
            while (!deflater.finished() && compressedSize < size) {
                compressedSize += deflater.deflate(
                        byteBuf.array(),
                        byteBuf.arrayOffset() + byteBuf.writerIndex() + compressedSize,
                        size - compressedSize
                );
            }

            int len = 0;
            if (compressedSize < size) {
                byteBuf.readerIndex(rawIndex + size);
                byteBuf.writerIndex(rawIndex + size + compressedSize);
                len += writeVarInt(out, compressedSize << 1 | 1);
                len += writeVarInt(out, size);
            } else {
                byteBuf.readerIndex(rawIndex);
                byteBuf.writerIndex(rawIndex + size);
                len += writeVarInt(out, size << 1);
            }
            while (byteBuf.isReadable()) {
                out.writeByte(byteBuf.readByte());
                len += 1;
            }
            return len;
        } catch (Exception e) {
            Utils.throwIfInstanceOf(e, IOException.class);
            Utils.throwIfUnchecked(e);
            throw new RuntimeException(e);
        } finally {
            byteBuf.readerIndex(readerIndex); // Reset reader & writer index for next use
            byteBuf.writerIndex(writerIndex);
            deflater.reset();
        }
    }

    private static int writeToCache(NetOutput out, Collection<com.github.steveice10.packetlib.packet.Packet> packets) throws IOException {
        int len = writeVarInt(out, packets.size());
        for (com.github.steveice10.packetlib.packet.Packet packet : packets) {
            len += writeToCache(out, packet);
        }
        return len;
    }

    private static int writeToCache(NetOutput out, SortedMap<Integer, com.github.steveice10.packetlib.packet.Packet> packets) throws IOException {
        int len = 0;
        len += writeVarInt(out, packets.size());
        int lastTime = 0;
        for (Map.Entry<Integer, com.github.steveice10.packetlib.packet.Packet> entry : packets.entrySet()) {
            int time = entry.getKey();
            len += writeVarInt(out, time - lastTime);
            lastTime = time;

            len += writeToCache(out, entry.getValue());
        }
        return len;
    }

    private static int writeVarInt(NetOutput out, int i) throws IOException {
        int len = 1;
        while ((i & -128) != 0) {
            out.writeByte(i & 127 | 128);
            i >>>= 7;
            len++;
        }
        out.writeByte(i);
        return len;
    }

    private static long coordToLong(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    private static <V> void playMap(NavigableMap<Integer, V> updates, int currentTimeStamp, int replayTime, Consumer<V> update) {
        Map.Entry<Integer, V> lastUpdate = updates.floorEntry(replayTime);
        if (lastUpdate != null && lastUpdate.getKey() > currentTimeStamp) {
            update.accept(lastUpdate.getValue());
        }
    }

    private static <V> void rewindMap(NavigableMap<Integer, V> updates, int currentTimeStamp, int replayTime, Consumer<V> update) {
        Map.Entry<Integer, V> lastUpdate = updates.floorEntry(replayTime);
        if (lastUpdate != null && !lastUpdate.getKey().equals(updates.floorKey(currentTimeStamp))) {
            update.accept(lastUpdate.getValue());
        }
    }

    private static abstract class TrackedThing {
        List<com.github.steveice10.packetlib.packet.Packet> spawnPackets;
        List<com.github.steveice10.packetlib.packet.Packet> despawnPackets;
        int spawnTime;

        private TrackedThing(List<com.github.steveice10.packetlib.packet.Packet> spawnPackets,
                             List<com.github.steveice10.packetlib.packet.Packet> despawnPackets) {
            this.spawnPackets = spawnPackets;
            this.despawnPackets = despawnPackets;
        }

        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeVarInt(spawnTime);
            indexOut.writeVarInt(despawnTime);
            indexOut.writeVarInt(index);
            index += QuickReplaySender.writeToCache(cacheOut, spawnPackets);
            indexOut.writeVarInt(index);
            index += QuickReplaySender.writeToCache(cacheOut, despawnPackets);
            return index;
        }
    }

    // For memory efficiency we store raw packets (we might even want to consider compression or memory mapped files).
    // During analysis we use TrackedThing which we then serialize (we'd have to do that anyway for caching)
    // and afterwards deserialize in BakedTrackedThing as MC packets on demand for replaying.
    private static abstract class BakedTrackedThing {
        int indexSpawnPackets;
        int indexDespawnPackets;
        int spawnTime;
        int despawnTime;

        private BakedTrackedThing(NetInput in) throws IOException {
            spawnTime = in.readVarInt();
            despawnTime = in.readVarInt();
            indexSpawnPackets = in.readVarInt();
            indexDespawnPackets = in.readVarInt();
        }

        public void spawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            sender.buf.readerIndex(indexSpawnPackets);
            readPacketsFromCache(sender.bufInput).forEach(send);
        }

        public void despawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            sender.buf.readerIndex(indexDespawnPackets);
            readPacketsFromCache(sender.bufInput).forEach(send);
        }

        public abstract void play(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send);
        public abstract void rewind(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send);
    }

    private static class Entity extends TrackedThing {
        private int id;
        private NavigableMap<Integer, Location> locations = new TreeMap<>();

        private Entity(int entityId, List<com.github.steveice10.packetlib.packet.Packet> spawnPackets) {
            super(spawnPackets, Collections.singletonList(new ServerEntityDestroyPacket(entityId)));
            this.id = entityId;
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(1);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(id);
            indexOut.writeVarInt(index);
            index += writeVarInt(cacheOut, locations.size());
            int lastTime = 0;
            for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
                int time = entry.getKey();
                Location loc = entry.getValue();
                index += writeVarInt(cacheOut, time - lastTime);
                lastTime = time;
                cacheOut.writeDouble(loc.getX());
                cacheOut.writeDouble(loc.getY());
                cacheOut.writeDouble(loc.getZ());
                cacheOut.writeFloat(loc.getYaw());
                cacheOut.writeFloat(loc.getPitch());
                index += 32;
            }

            return index;
        }
    }

    private static class BakedEntity extends BakedTrackedThing {
        private int id;
        private int index;
        private NavigableMap<Integer, Location> locations;

        private BakedEntity(NetInput in) throws IOException {
            super(in);

            id = in.readVarInt();
            index = in.readVarInt();
        }

        @Override
        public void spawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.spawn(sender, send);

            sender.buf.readerIndex(index);
            NetInput in = sender.bufInput;

            locations = new TreeMap<>();

            int time = 0;
            for (int i = in.readVarInt(); i > 0; i--) {
                time += in.readVarInt();
                locations.put(time, new Location(in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat()));
            }
        }

        @Override
        public void despawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.despawn(sender, send);
            locations = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            playMap(locations, currentTimeStamp, replayTime, l -> {
                send.accept(toMC(new ServerEntityTeleportPacket(id, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), false)));
                send.accept(toMC(new ServerEntityHeadLookPacket(id, l.getYaw())));
            });
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            rewindMap(locations, currentTimeStamp, replayTime, l -> {
                send.accept(toMC(new ServerEntityTeleportPacket(id, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), false)));
                send.accept(toMC(new ServerEntityHeadLookPacket(id, l.getYaw())));
            });
        }
    }

    private static class Chunk extends TrackedThing {
        private TreeMap<Integer, Collection<BlockChange>> blocksT = new TreeMap<>();
        private ListMultimap<Integer, BlockChange> blocks = Multimaps.newListMultimap(blocksT, LinkedList::new); // LinkedList to allow .descendingIterator
        private BlockStorage[] currentBlockState = new BlockStorage[16];

        private Chunk(Column column) {
            super(Collections.singletonList(new ServerChunkDataPacket(column)),
                    Collections.singletonList(new ServerUnloadChunkPacket(column.getX(), column.getZ())));
            com.github.steveice10.mc.protocol.data.game.chunk.Chunk[] chunks = column.getChunks();
            for (int i = 0; i < currentBlockState.length; i++) {
                currentBlockState[i] = chunks[i] == null ? new BlockStorage() : copy(chunks[i].getBlocks());
            }
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(2);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(index);
            index += writeVarInt(cacheOut, blocksT.size());
            int lastTime = 0;
            for (Map.Entry<Integer, Collection<BlockChange>> entry : blocksT.entrySet()) {
                int time = entry.getKey();
                index += writeVarInt(cacheOut, time - lastTime);
                lastTime = time;

                Collection<BlockChange> blockChanges = entry.getValue();
                index += writeVarInt(cacheOut, blockChanges.size());
                for (BlockChange blockChange : blockChanges) {
                    NetUtil.writePosition(cacheOut, blockChange.pos);
                    index += 8;
                    //#if MC>=11300
                    index += writeVarInt(cacheOut, blockChange.from.getId());
                    index += writeVarInt(cacheOut, blockChange.to.getId());
                    //#else
                    //$$ index += writeVarInt(cacheOut, blockChange.from.getId() << 4 | blockChange.from.getData() & 15);
                    //$$ index += writeVarInt(cacheOut, blockChange.to.getId() << 4 | blockChange.to.getData() & 15);
                    //#endif
                }
            }

            return index;
        }
    }

    private static class BakedChunk extends BakedTrackedThing {
        private int index;
        private TreeMap<Integer, Collection<BlockChange>> blocksT;

        private BakedChunk(NetInput in) throws IOException {
            super(in);

            index = in.readVarInt();
        }

        @Override
        public void spawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.spawn(sender, send);

            sender.buf.readerIndex(index);
            NetInput in = sender.bufInput;

            blocksT = new TreeMap<>();
            ListMultimap<Integer, BlockChange> blocks = Multimaps.newListMultimap(blocksT, LinkedList::new); // LinkedList to allow .descendingIterator

            int time = 0;
            for (int i = in.readVarInt(); i > 0; i--) {
                time += in.readVarInt();

                for (int j = in.readVarInt(); j > 0; j--) {
                    blocks.put(time, new BlockChange(
                            NetUtil.readPosition(in),
                            NetUtil.readBlockState(in),
                            NetUtil.readBlockState(in)
                    ));
                }
            }
        }

        @Override
        public void despawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.despawn(sender, send);

            blocksT = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            blocksT.subMap(currentTimeStamp, false, replayTime, true).values()
                    .forEach(updates -> updates.forEach(update -> {
                        send.accept(toMC(new ServerBlockChangePacket(new BlockChangeRecord(update.pos, update.to))));
                    }));
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            if (currentTimeStamp >= despawnTime) {
                play(spawnTime, replayTime, send);
                return;
            }
            blocksT.subMap(replayTime, false, currentTimeStamp, true).descendingMap().values()
                    .forEach(updates ->
                            ((LinkedList<BlockChange>) updates).descendingIterator().forEachRemaining(update ->
                                    send.accept(toMC(new ServerBlockChangePacket(new BlockChangeRecord(update.pos, update.from))))));
        }
    }

    private static class BlockChange {
        private Position pos;
        private BlockState from;
        private BlockState to;

        private BlockChange(Position pos, BlockState from, BlockState to) {
            this.pos = pos;
            this.from = from;
            this.to = to;
        }
    }

    private static class Weather extends TrackedThing {
        private TreeMap<Integer, com.github.steveice10.packetlib.packet.Packet> rainStrengths = new TreeMap<>();

        private Weather() {
            super(Collections.singletonList(new ServerNotifyClientPacket(ClientNotification.START_RAIN, null)),
                    Collections.singletonList(new ServerNotifyClientPacket(ClientNotification.STOP_RAIN, null)));
        }

        @Override
        public int writeToCache(NetOutput indexOut, NetOutput cacheOut, int despawnTime, int index) throws IOException {
            indexOut.writeByte(3);
            index = super.writeToCache(indexOut, cacheOut, despawnTime, index);

            indexOut.writeVarInt(index);
            index += QuickReplaySender.writeToCache(cacheOut, rainStrengths);

            return index;
        }
    }

    private static class BakedWeather extends BakedTrackedThing {
        private int index;
        private TreeMap<Integer, Packet<?>> rainStrengths;

        private BakedWeather(NetInput in) throws IOException {
            super(in);

            index = in.readVarInt();
        }

        @Override
        public void spawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.spawn(sender, send);

            sender.buf.readerIndex(index);

            rainStrengths = new TreeMap<>();

            readFromCache(sender.bufInput, rainStrengths);
        }

        @Override
        public void despawn(QuickReplaySender sender, Consumer<Packet<?>> send) throws IOException {
            super.despawn(sender, send);

            rainStrengths = null;
        }

        @Override
        public void play(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            playMap(rainStrengths, currentTimeStamp, replayTime, send);
        }

        @Override
        public void rewind(int currentTimeStamp, int replayTime, Consumer<Packet<?>> send) {
            rewindMap(rainStrengths, currentTimeStamp, replayTime, send);
        }
    }
}
//#endif
