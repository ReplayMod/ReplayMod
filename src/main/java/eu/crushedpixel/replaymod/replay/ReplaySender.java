package eu.crushedpixel.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFutureTask;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.events.handlers.RecordingHandler;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Sends replay packets to netty channels.
 * Even though {@link Sharable}, this should never be added to multiple pipes at once, it may however be re-added when
 * the replay restart from the beginning.
 */
@Sharable
public class ReplaySender extends ChannelInboundHandlerAdapter {

    /**
     * These packets are ignored completely during replay.
     */
    private static final List<Class> BAD_PACKETS = Arrays.<Class>asList(
            S06PacketUpdateHealth.class,
            S2DPacketOpenWindow.class,
            S2EPacketCloseWindow.class,
            S2FPacketSetSlot.class,
            S30PacketWindowItems.class,
            S36PacketSignEditorOpen.class,
            S37PacketStatistics.class,
            S1FPacketSetExperience.class,
            S43PacketCamera.class,
            S39PacketPlayerAbilities.class,
            S02PacketChat.class,
            S45PacketTitle.class);

    /**
     * Whether to work in async mode.
     *
     * When in async mode, a separate thread send packets and waits according to their delays.
     * This is default in normal playback mode.
     *
     * When in sync mode, no packets will be sent until {@link #sendPacketsTill(int)} is called.
     * This is used during path playback and video rendering.
     */
    protected boolean asyncMode;

    /**
     * Timestamp of the last packet sent in milliseconds since the start.
     */
    protected int lastTimeStamp;

    /**
     * The replay file.
     */
    protected ReplayFile replayFile;

    /**
     * The channel handler context used to send packets to minecraft.
     */
    protected ChannelHandlerContext ctx;

    /**
     * The data input stream from which new packets are read.
     * When accessing this stream make sure to synchronize on {@code this} as it's used from multiple threads.
     */
    protected DataInputStream dis;

    /**
     * The next packet that should be sent.
     * This is required as some actions such as jumping to a specified timestamp have to peek at the next packet.
     */
    protected PacketData nextPacket;

    /**
     * Whether we need to restart the current replay. E.g. when jumping backwards in time
     */
    protected boolean startFromBeginning = true;

    /**
     * Whether to terminate the replay. This only has an effect on the async mode and is {@code true} during sync mode.
     */
    protected boolean terminate;

    /**
     * The speed of the replay. 1 is normal, 2 is twice as fast, 0.5 is half speed and 0 is frozen
     */
    protected double replaySpeed = 1f;

    /**
     * Whether the world has been loaded and the dirt-screen should go away.
     */
    protected boolean hasWorldLoaded;

    /**
     * The minecraft instance.
     */
    protected Minecraft mc = Minecraft.getMinecraft();

    /**
     * The total length of this replay in milliseconds.
     */
    protected final int replayLength;

    /**
     * Our actual entity id that the server gave to us.
     */
    protected int actualID = -1;

    /**
     * Whether to allow (process) the next player movement packet.
     */
    protected boolean allowMovement;

    /**
     * Directory to which resource packs are extracted.
     */
    private final File tempResourcePackFolder = Files.createTempDir();

    /**
     * Create a new replay sender.
     * @param file The replay file
     * @param asyncMode {@code true} for async mode, {@code false} otherwise
     * @see #asyncMode
     */
    public ReplaySender(ReplayFile file, boolean asyncMode) {
        this.replayFile = file;
        this.asyncMode = asyncMode;
        this.replayLength = file.metadata().get().getDuration();

        if (asyncMode) {
            new Thread(asyncSender, "replaymod-async-sender").start();
        }
    }

    /**
     * Set whether this replay sender operates in async mode.
     * When in async mode, it will send packets timed from a separate thread.
     * When not in async mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * @param asyncMode {@code true} to enable async mode
     */
    public void setAsyncMode(boolean asyncMode) {
        if (this.asyncMode == asyncMode) return;
        this.asyncMode = asyncMode;
        if (asyncMode) {
            this.terminate = false;
            new Thread(asyncSender, "replaymod-async-sender").start();
        } else {
            this.terminate = true;
        }
    }

    /**
     * Set whether this replay sender  to operate in sync mode.
     * When in sync mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * This call will block until the async worker thread has stopped.
     */
    public void setSyncModeAndWait() {
        if (!this.asyncMode) return;
        this.asyncMode = false;
        this.terminate = true;
        synchronized (this) {
            // This will wait for the worker thread to leave the synchronized code part
        }
    }

    /**
     * Return the timestamp of the last packet sent.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public int currentTimeStamp() {
        return lastTimeStamp;
    }

    /**
     * Return the total length of the replay played.
     * @return Total length in milliseconds
     */
    public int replayLength() {
        return replayLength;
    }

    /**
     * Terminate this replay sender.
     */
    public void terminateReplay() {
        terminate = true;
        try {
            channelInactive(ctx);
            ctx.channel().pipeline().close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // When in async mode and the replay sender shut down, then don't send packets
        if(terminate && asyncMode) {
            return;
        }

        // When a packet is sent directly, perform no filtering
        if(msg instanceof Packet) {
            super.channelRead(ctx, msg);
        }

        if (msg instanceof byte[]) {
            try {
                Packet p = ReplayFileIO.deserializePacket((byte[]) msg);

                if (p != null) {
                    p = processPacket(p);
                    if (p != null) {
                        super.channelRead(ctx, p);
                    }

                    // If we do not give minecraft time to tick, there will be dead entity artifacts left in the world
                    // Therefore we have to remove all loaded, dead entities manually if we are in sync mode.
                    // We do this after every SpawnX packet and after the destroy entities packet.
                    if (!asyncMode && mc.theWorld != null) {
                        if (p instanceof S0CPacketSpawnPlayer
                                || p instanceof S0EPacketSpawnObject
                                || p instanceof S0FPacketSpawnMob
                                || p instanceof S2CPacketSpawnGlobalEntity
                                || p instanceof S10PacketSpawnPainting
                                || p instanceof S11PacketSpawnExperienceOrb
                                || p instanceof S13PacketDestroyEntities) {
                            World world = mc.theWorld;
                            for (int i = 0; i < world.loadedEntityList.size(); ++i) {
                                Entity entity = (Entity) world.loadedEntityList.get(i);
                                if (entity.isDead) {
                                    int chunkX = entity.chunkCoordX;
                                    int chunkY = entity.chunkCoordZ;

                                    if (entity.addedToChunk && world.getChunkProvider().chunkExists(chunkX, chunkY)) {
                                        world.getChunkFromChunkCoords(chunkX, chunkY).removeEntity(entity);
                                    }

                                    world.loadedEntityList.remove(i--);
                                    world.onEntityRemoved(entity);
                                }

                            }
                        }
                    }
                }
            } catch (Exception e) {
                // We'd rather not have a failure parsing one packet screw up the whole replay process
                e.printStackTrace();
            }
        }

    }

    /**
     * Process a packet and return the result.
     * @param p The packet to process
     * @return The processed packet or {@code null} if no packet shall be sent
     */
    protected Packet processPacket(Packet p) throws Exception {
        if(BAD_PACKETS.contains(p.getClass())) return null;

        if(ReplayProcess.isVideoRecording() && ReplayHandler.isInPath()) {
            if(p instanceof S29PacketSoundEffect) {
                return null;
            }
        }

        if(p instanceof S48PacketResourcePackSend) {
            S48PacketResourcePackSend packet = (S48PacketResourcePackSend) p;
            String url = packet.func_179783_a();
            if (url.startsWith("replay://")) {
                int id = Integer.parseInt(url.substring("replay://".length()));
                Map<Integer, String> index = replayFile.resourcePackIndex().get();
                if (index != null) {
                    String hash = index.get(id);
                    if (hash != null) {
                        File file = new File(tempResourcePackFolder, hash + ".zip");
                        if (!file.exists()) {
                            IOUtils.copy(replayFile.resourcePack(hash).get(), new FileOutputStream(file));
                        }
                        mc.getResourcePackRepository().func_177319_a(file);
                    }
                }
                return null;
            }
        }

        if(p instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) p;
            if(packet.field_149379_a == actualID) {
                packet.field_149379_a = RecordingHandler.entityID;
            }
        }

        if(p instanceof S1BPacketEntityAttach) {
            S1BPacketEntityAttach packet = (S1BPacketEntityAttach) p;
            if(packet.func_149403_d() == actualID) {
                return null;
            }
        }

        if(p instanceof S01PacketJoinGame) {
            S01PacketJoinGame packet = (S01PacketJoinGame) p;
            allowMovement = true;
            int entId = packet.getEntityId();
            actualID = entId;
            entId = Integer.MIN_VALUE + 9002;
            int dimension = packet.getDimension();
            EnumDifficulty difficulty = packet.getDifficulty();
            int maxPlayers = packet.getMaxPlayers();
            WorldType worldType = packet.getWorldType();

            p = new S01PacketJoinGame(entId, GameType.SPECTATOR, false, dimension,
                    difficulty, maxPlayers, worldType, false);
        }

        if(p instanceof S07PacketRespawn) {
            S07PacketRespawn respawn = (S07PacketRespawn) p;
            p = new S07PacketRespawn(respawn.func_149082_c(),
                    respawn.func_149081_d(), respawn.func_149080_f(), GameType.SPECTATOR);

            allowMovement = true;
        }

        if(p instanceof S08PacketPlayerPosLook) {
            if(!hasWorldLoaded) hasWorldLoaded = true;
            final S08PacketPlayerPosLook ppl = (S08PacketPlayerPosLook) p;

            if (mc.currentScreen instanceof GuiDownloadTerrain) {
                // Close the world loading screen manually in case we swallow the packet
                mc.displayGuiScreen(null);
            }

            if(ReplayHandler.isInPath()) return null;

            CameraEntity cent = ReplayHandler.getCameraEntity();

            for (Object relative : ppl.func_179834_f()) {
                if (relative == S08PacketPlayerPosLook.EnumFlags.X
                        || relative == S08PacketPlayerPosLook.EnumFlags.Y
                        || relative == S08PacketPlayerPosLook.EnumFlags.Z) {
                    return null; // At least one of the coordinates is relative, so we don't care
                }
            }

            if(cent != null) {
                if(!allowMovement && !((Math.abs(cent.posX - ppl.func_148932_c()) > ReplayMod.TP_DISTANCE_LIMIT) ||
                        (Math.abs(cent.posZ - ppl.func_148933_e()) > ReplayMod.TP_DISTANCE_LIMIT))) {
                    return null;
                } else {
                    allowMovement = false;
                }
            }

            new Callable<Void>() {
                @Override
                @SuppressWarnings("unchecked")
                public Void call() {
                    if (mc.theWorld == null || !mc.isCallingFromMinecraftThread()) {
                        synchronized(mc.scheduledTasks) {
                            mc.scheduledTasks.add(ListenableFutureTask.create(this));
                        }
                        return null;
                    }

                    CameraEntity cent = ReplayHandler.getCameraEntity();
                    cent.moveAbsolute(ppl.func_148932_c(), ppl.func_148928_d(), ppl.func_148933_e());
                    
                    ReplayHandler.spectateCamera();
                    return null;
                }
            }.call();
        }

        if(p instanceof S2BPacketChangeGameState) {
            S2BPacketChangeGameState pg = (S2BPacketChangeGameState)p;
            int reason = pg.func_149138_c();

            // only allow the following packets:
            // 1 - End raining
            // 2 - Begin raining
            //
            // The following values are to control sky color (e.g. if thunderstorm)
            // 7 - Fade value
            // 8 - Fade time
            if(!(reason == 1 || reason == 2 || reason == 7 || reason == 8)) {
                return null;
            }
        }

        return asyncMode ? processPacketAsync(p) : processPacketSync(p);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        ctx.attr(NetworkManager.attrKeyConnectionState).set(EnumConnectionState.PLAY);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        replayFile.close();
        FileUtils.deleteDirectory(tempResourcePackFolder);
        super.channelInactive(ctx);
    }

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public boolean paused() {
        return mc.timer.timerSpeed == 0;
    }

    /**
     * Returns the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * If 0 is returned, the replay is paused.
     * @return speed multiplier
     */
    public double getReplaySpeed() {
        if(!paused()) return replaySpeed;
        else return 0;
    }

    /**
     * Set the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * The speed may not be set to 0 nor to negative values.
     * @param d Speed multiplier
     */
    public void setReplaySpeed(final double d) {
        if(d != 0) this.replaySpeed = d;
        mc.timer.timerSpeed = (float) d;
    }

    /////////////////////////////////////////////////////////
    //       Asynchronous packet processing                //
    /////////////////////////////////////////////////////////

    /**
     * The real time at which the last packet was sent in milliseconds.
     */
    private long lastPacketSent;

    /**
     * There is no waiting performed until a packet with at least this timestamp is reached (but not yet sent).
     * If this is -1, then timing is normal.
     */
    private long desiredTimeStamp = -1;

    /**
     * Runnable which performs timed dispatching of packets from the input stream.
     */
    private Runnable asyncSender = new Runnable() {
        public void run() {
            try {
                while (ctx == null && !terminate) {
                    Thread.sleep(10);
                }
                REPLAY_LOOP:
                while (true) {
                    synchronized (ReplaySender.this) {
                        if (dis == null) {
                            dis = new DataInputStream(replayFile.recording().get());
                        }
                        // Packet loop
                        while (true) {
                            try {
                                // When playback is paused and the world has loaded (we don't want any dirt-screens) we sleep
                                while (paused() && hasWorldLoaded) {
                                    // Unless we are going to terminate, restart or jump
                                    if (terminate || startFromBeginning || desiredTimeStamp != -1) {
                                        break;
                                    }
                                    Thread.sleep(10);
                                }

                                if (terminate) {
                                    break REPLAY_LOOP;
                                }

                                if (startFromBeginning) {
                                    // In case we need to restart from the beginning
                                    // break out of the loop sending all packets which will
                                    // cause the replay to be restarted by the outer loop
                                    break;
                                }

                                // Read the next packet if we don't already have one
                                if (nextPacket == null) {
                                    nextPacket = ReplayFileIO.readPacketData(dis);
                                }

                                int nextTimeStamp = nextPacket.getTimestamp();

                                // If we aren't jumping and the world has already been loaded (no dirt-screens) then wait
                                // the required amount to get proper packet timing
                                if (!isHurrying() && hasWorldLoaded) {
                                    // How much time should have passed
                                    int timeWait = (int) Math.round((nextTimeStamp - lastTimeStamp) / replaySpeed);
                                    // How much time did pass
                                    long timeDiff = System.currentTimeMillis() - lastPacketSent;
                                    // How much time we need to wait to make up for the difference
                                    long timeToSleep = Math.max(0, timeWait - timeDiff);

                                    Thread.sleep(timeToSleep);
                                    lastPacketSent = System.currentTimeMillis();
                                }

                                // Process packet
                                channelRead(ctx, nextPacket.getByteArray());
                                nextPacket = null;

                                lastTimeStamp = nextTimeStamp;

                                // In case we finished jumping
                                // We need to check that we aren't planing to restart so we don't accidentally run this
                                // code before we actually restarted
                                if (isHurrying() && lastTimeStamp > desiredTimeStamp && !startFromBeginning) {
                                    desiredTimeStamp = -1;

                                    ReplayHandler.moveCameraToLastPosition();

                                    // Pause after jumping
                                    setReplaySpeed(0);
                                }
                            } catch (EOFException eof) {
                                // Reached end of file
                                // Pause the replay which will cause it to freeze before getting restarted
                                setReplaySpeed(0);
                                // Then wait until the user tells us to continue
                                while (paused() && hasWorldLoaded && desiredTimeStamp == -1 && !terminate) {
                                    Thread.sleep(10);
                                }
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Restart the replay.
                        hasWorldLoaded = false;
                        lastTimeStamp = 0;
                        startFromBeginning = false;
                        nextPacket = null;
                        lastPacketSent = System.currentTimeMillis();
                        ReplayHandler.restartReplay();
                        if (dis != null) {
                            dis.close();
                            dis = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Return whether this replay sender is currently rushing. When rushing, all packets are sent without waiting until
     * a specified timestamp is passed.
     * @return {@code true} if currently rushing, {@code false} otherwise
     */
    public boolean isHurrying() {
        return desiredTimeStamp != -1;
    }

    /**
     * Cancels the hurrying.
     */
    public void stopHurrying() {
        desiredTimeStamp = -1;
    }

    /**
     * Return the timestamp to which this replay sender is currently rushing. All packets with an lower or equal
     * timestamp will be sent out without any sleeping.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public long getDesiredTimestamp() {
        return desiredTimeStamp;
    }

    /**
     * Jumps to the specified timestamp when in async mode by rushing all packets until one with a timestamp greater
     * than the specified timestamp is found.
     * If the timestamp has already passed, this causes the replay to restart and then rush all packets.
     * @param millis Timestamp in milliseconds since the start of the replay
     */
    public void jumpToTime(int millis) {
        Preconditions.checkState(asyncMode, "Can only jump in async mode. Use sendPacketsTill(int) instead.");
        if(millis < lastTimeStamp && !isHurrying()) {
            startFromBeginning = true;
        }

        desiredTimeStamp = millis;
    }

    protected Packet processPacketAsync(Packet p) {
        //If hurrying, ignore some packets, unless during Replay Path and *not* in short hurries
        if(!ReplayHandler.isInPath() && desiredTimeStamp - lastTimeStamp > 1000) {
            if(p instanceof S2APacketParticles) return null;

            if(p instanceof S0EPacketSpawnObject) {
                S0EPacketSpawnObject pso = (S0EPacketSpawnObject)p;
                int type = pso.func_148993_l();
                if(type == 76) { // Firework rocket
                    return null;
                }
            }
        }
        return p;
    }

    /////////////////////////////////////////////////////////
    //        Synchronous packet processing                //
    /////////////////////////////////////////////////////////

    /**
     * Sends all packets until the specified timestamp is reached (inclusive).
     * If the timestamp is smaller than the last packet sent, the replay is restarted from the beginning.
     * @param timestamp The timestamp in milliseconds since the beginning of this replay
     */
    public void sendPacketsTill(int timestamp) {
        Preconditions.checkState(!asyncMode, "This method cannot be used in async mode. Use jumpToTime(int) instead.");
        try {
            while (ctx == null && !terminate) { // Make sure channel is ready
                Thread.sleep(10);
            }

            synchronized (this) {
                if (timestamp < lastTimeStamp) { // Restart the replay if we need to go backwards in time
                    hasWorldLoaded = false;
                    lastTimeStamp = 0;
                    if (dis != null) {
                        dis.close();
                        dis = null;
                    }
                    startFromBeginning = false;
                    nextPacket = null;
                    ReplayHandler.restartReplay();
                }

                if (dis == null) {
                    dis = new DataInputStream(replayFile.recording().get());
                }

                while (true) { // Send packets
                    try {
                        PacketData pd;
                        if (nextPacket != null) {
                            // If there is still a packet left from before, use it first
                            pd = nextPacket;
                            nextPacket = null;
                        } else {
                            // Otherwise read one from the input stream
                            pd = ReplayFileIO.readPacketData(dis);
                        }

                        int nextTimeStamp = pd.getTimestamp();
                        if (nextTimeStamp > timestamp) {
                            // We are done sending all packets
                            nextPacket = pd;
                            break;
                        }

                        // Process packet
                        channelRead(ctx, pd.getByteArray());

                        // Store last timestamp
                        lastTimeStamp = nextTimeStamp;
                    } catch (EOFException eof) {
                        // Shit! We hit the end before finishing our job! What shall we do now?
                        // well, let's just pretend we're done...
                        dis = null;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // This might be required if we change to async mode anytime soon
                lastPacketSent = System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Packet processPacketSync(Packet p) {
        return p; // During synchronous playback everything is sent normally
    }

}
