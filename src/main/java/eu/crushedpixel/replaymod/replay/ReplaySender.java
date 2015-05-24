package eu.crushedpixel.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFutureTask;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.events.RecordingHandler;
import eu.crushedpixel.replaymod.holders.PacketData;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import org.apache.commons.io.FileUtils;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
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
            S28PacketEffect.class,
            S2BPacketChangeGameState.class,
            S06PacketUpdateHealth.class,
            S2DPacketOpenWindow.class,
            S2EPacketCloseWindow.class,
            S2FPacketSetSlot.class,
            S30PacketWindowItems.class,
            S36PacketSignEditorOpen.class,
            S37PacketStatistics.class,
            S1FPacketSetExperience.class,
            S43PacketCamera.class,
            S39PacketPlayerAbilities.class);

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
     * Whether the replay has been restarted.
     * It might be required to advance some ticks in order for rendering to keep up.
     */
    protected boolean hasRestarted;

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
            new Thread(asyncSender).start();
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
            new Thread(asyncSender).start();
        } else {
            this.terminate = true;
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
    protected Packet processPacket(Packet p) {
        if(BAD_PACKETS.contains(p.getClass())) return null;

        if(p instanceof S29PacketSoundEffect && ReplayProcess.isVideoRecording()) {
            return null;
        }

        if(p instanceof S03PacketTimeUpdate) {
            p = TimeHandler.getTimePacket((S03PacketTimeUpdate) p);
        }

        if(p instanceof S48PacketResourcePackSend) {
            S48PacketResourcePackSend packet = (S48PacketResourcePackSend) p;
            new ResourcePackCheck(packet.func_179783_a(), packet.func_179784_b()).start();
            return null;
        }

        if(p instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) p;
            if(packet.field_149379_a == actualID) {
                packet.field_149379_a = RecordingHandler.entityID;
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

                    if (cent == null){
                        cent = new CameraEntity(mc.theWorld);
                    }
                    cent.moveAbsolute(ppl.func_148932_c(), ppl.func_148928_d(), ppl.func_148933_e());
                    ReplayHandler.setCameraEntity(cent);
                    return null;
                }
            }.call();
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
        super.channelInactive(ctx);
    }

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public boolean paused() {
        return MCTimerHandler.getTimerSpeed() == 0;
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
        MCTimerHandler.setTimerSpeed((float) d);
    }

    /**
     * Checks for stored resource packs and loads them or downloads a new one.
     */
    private static class ResourcePackCheck extends Thread {

        private static Minecraft mc = Minecraft.getMinecraft();
        private static ResourcePackRepository repo = mc.getResourcePackRepository();

        private String url, hash;

        public ResourcePackCheck(String url, String hash) {
            this.url = url;
            this.hash = hash;
        }

        /**
         * Return the location of the stored resource pack.
         * @param url The original url of the resource pack
         * @param hash The hash code of the resource pack
         * @return File location of the resource pack
         */
        private File getServerResourcePackLocation(String url, String hash) {

            String filename;

            if(hash.matches("^[a-f0-9]{40}$")) {
                filename = hash;
            } else {
                filename = url.substring(url.lastIndexOf("/") + 1);

                if(filename.contains("?")) {
                    filename = filename.substring(0, filename.indexOf("?"));
                }

                if(!filename.endsWith(".zip")) {
                    return null;
                }

                filename = "legacy_" + filename.replaceAll("\\W", "");
            }

            return new File(repo.dirServerResourcepacks, filename);
        }

        /**
         * Download a resource pack from a specified URL.
         * @param url The URL to download from
         * @param file The target file location
         * @return {@code true} if the download was successful, {@code false} if an I/O-error occured
         */
        private boolean downloadServerResourcePack(String url, File file) {
            try {
                FileUtils.copyURLToFile(new URL(url), file);
                return true;
            } catch(Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * Add the resource pack to the loaded resource packs if resource packs are enabled in the config.
         * If there is no local copy of the resource pack, this loads the resource pack from the specified url.
         */
        @Override
        public void run() {
            try {
                boolean use = ReplayMod.replaySettings.getUseResourcePacks();
                if(!use) return;

                System.out.println("Looking for downloaded Resource Pack...");
                File rp = getServerResourcePackLocation(url, hash);
                if(rp == null) {
                    System.out.println("Invalid Resource Pack provided");
                    return;
                }
                if(rp.exists()) {
                    System.out.println("Resource Pack found!");
                    repo.func_177319_a(rp);

                } else {
                    System.out.println("No Resource Pack found.");
                    System.out.println("Attempting to download Resource Pack...");
                    boolean success = downloadServerResourcePack(url, rp);
                    System.out.println(success ? "Resource pack was successfully downloaded!" : "Resource Pack download failed.");
                    if(success) {
                        repo.func_177319_a(rp);
                    }
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
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

                                    // Give the render engine a reason to get going
                                    MCTimerHandler.advanceRenderPartialTicks(5);
                                    MCTimerHandler.advancePartialTicks(5);
                                    MCTimerHandler.advanceTicks(5);

                                    Position pos = ReplayHandler.getLastPosition();
                                    CameraEntity cam = ReplayHandler.getCameraEntity();
                                    if (cam != null && pos != null) {
                                        // Move camera back in case we have been respawned
                                        if (Math.abs(pos.getX() - cam.posX) < ReplayMod.TP_DISTANCE_LIMIT && Math.abs(pos.getZ() - cam.posZ) < ReplayMod.TP_DISTANCE_LIMIT) {
                                            cam.moveAbsolute(pos.getX(), pos.getY(), pos.getZ());
                                            cam.rotationPitch = pos.getPitch();
                                            cam.rotationYaw = pos.getYaw();
                                        }
                                    }

                                    // Pause after jumping
                                    setReplaySpeed(0);
                                }
                            } catch (EOFException eof) {
                                // Reached end of file
                                // Pause the replay which will cause it to freeze before getting restarted
                                setReplaySpeed(0);
                                // Then wait until the user tells us to continue
                                while (paused() && hasWorldLoaded && desiredTimeStamp == -1) {
                                    Thread.sleep(10);
                                }
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Restart the replay.
                        hasRestarted = true;
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
                    hasRestarted = true;
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

                // In case we have restarted the replay we have to give the render engine a reason to get going
                if (hasRestarted) {
                    MCTimerHandler.advanceRenderPartialTicks(5);
                    MCTimerHandler.advancePartialTicks(5);
                    MCTimerHandler.advanceTicks(5);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Packet processPacketSync(Packet p) {
        return p; // During synchronous playback everything is sent normally
    }

}
