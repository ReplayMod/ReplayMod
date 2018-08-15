package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.*;
import net.minecraft.network.play.server.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager; // RAH
import org.apache.logging.log4j.Logger; // RAH
import com.replaymod.replay.events.ReplayPlayingEvent; // RAH - Alert ReplayModSimplePathing that the video is playing


//#if MC>=11200
import com.replaymod.core.utils.WrappedTimer;
//#endif
//#if MC>=11002
import net.minecraft.world.GameType;
//#else
//$$ import net.minecraft.world.WorldSettings.GameType;
//#endif
//#if MC>=10904
import net.minecraft.util.text.ITextComponent;
//#else
//$$ import net.minecraft.util.IChatComponent;
//#endif

//#if MC>=10800
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#else
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent;
//$$ import org.apache.commons.io.Charsets;
//#endif

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Sends replay packets to netty channels.
 * Even though {@link Sharable}, this should never be added to multiple pipes at once, it may however be re-added when
 * the replay restart from the beginning.
 */
@Sharable
public class ReplaySender extends ChannelDuplexHandler {
    /**
     * These packets are ignored completely during replay.
     */
    private static final List<Class> BAD_PACKETS = Arrays.<Class>asList(
            //#if MC>=11200
            SPacketRecipeBook.class,
            SPacketAdvancementInfo.class,
            SPacketSelectAdvancementsTab.class,
            //#endif
            //#if MC>=10904
            // TODO Update possibly more?
            SPacketUpdateHealth.class,
            SPacketOpenWindow.class,
            SPacketCloseWindow.class,
            //SPacketSetSlot.class,
            SPacketWindowItems.class,
            SPacketSignEditorOpen.class,
            SPacketStatistics.class,
            SPacketSetExperience.class,
            SPacketCamera.class,
            SPacketPlayerAbilities.class,
            SPacketTitle.class
            //#else
            //#if MC>=10800
            //$$ S43PacketCamera.class,
            //$$ S45PacketTitle.class,
            //#endif
            //$$ S06PacketUpdateHealth.class,
            //$$ S2DPacketOpenWindow.class,
            //$$ S2EPacketCloseWindow.class,
            //$$ S2FPacketSetSlot.class,
            //$$ S30PacketWindowItems.class,
            //$$ S36PacketSignEditorOpen.class,
            //$$ S37PacketStatistics.class,
            //$$ S1FPacketSetExperience.class,
            //$$ S39PacketPlayerAbilities.class
            //#endif
    );

	//RAH
	private boolean automationInitialization = false;
	// RAH

    private static int TP_DISTANCE_LIMIT = 128;

    /**
     * The replay handler responsible for the current replay.
     */
    private final ReplayHandler replayHandler;

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
     * @see #currentTimeStamp()
     */
    protected int currentTimeStamp;

    /**
     * The replay file.
     */
    protected ReplayFile replayFile;

    /**
     * The channel handler context used to send packets to minecraft.
     */
    protected ChannelHandlerContext ctx;

    /**
     * The replay input stream from which new packets are read.
     * When accessing this stream make sure to synchronize on {@code this} as it's used from multiple threads.
     */
    protected ReplayInputStream replayIn;

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
    public ReplaySender(ReplayHandler replayHandler, ReplayFile file, boolean asyncMode) throws IOException {
        this.replayHandler = replayHandler;
        this.replayFile = file;
        this.asyncMode = asyncMode;
        this.replayLength = file.getMetaData().getDuration();

        MinecraftForge.EVENT_BUS.register(this);

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

    public boolean isAsyncMode() {
        return asyncMode;
    }

	/**
	* RAH - Added
	*
	*/
	public int getPlayerId()
	{
		return actualID;
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
     * Return a fake {@link Minecraft#getSystemTime()} value that respects slowdown/speedup/pause and works in both,
     * sync and async mode.
     * Note: For sync mode this returns the last value passed to {@link #sendPacketsTill(int)}.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public int currentTimeStamp() {
        if (asyncMode) {
            int timePassed = (int) (System.currentTimeMillis() - lastPacketSent);
            return lastTimeStamp + (int) (timePassed * getReplaySpeed());
        } else {
            return lastTimeStamp;
        }
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
        MinecraftForge.EVENT_BUS.unregister(this);
        try {
            channelInactive(ctx);
            ctx.channel().pipeline().close();
            FileUtils.deleteDirectory(tempResourcePackFolder);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ClientTickEvent event) {
        // Unfortunately the WorldTickEvent doesn't seem to be emitted on the CLIENT side
        if (event.phase != TickEvent.Phase.START) return;

        // Spawning a player into an empty chunk (which we might do with the recording player)
        // prevents it from being moved by teleport packets (it essentially gets stuck) because
        // Entity#addedToChunk is not set and it is therefore not updated every tick.
        // To counteract this, we need to manually update it's position if it hasn't been added
        // to any chunk yet.
        if (world(mc) != null) {
            for (EntityPlayer playerEntity : playerEntities(world(mc))) {
                if (!playerEntity.addedToChunk && playerEntity instanceof EntityOtherPlayerMP) {
                    playerEntity.onLivingUpdate();
                }
            }
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
                Packet p = deserializePacket((byte[]) msg);

                if (p != null) {
                    p = processPacket(p);
                    if (p != null) {
                        super.channelRead(ctx, p);
                    }

                    // If we do not give minecraft time to tick, there will be dead entity artifacts left in the world
                    // Therefore we have to remove all loaded, dead entities manually if we are in sync mode.
                    // We do this after every SpawnX packet and after the destroy entities packet.
                    if (!asyncMode && world(mc) != null) {
                        //#if MC>=10904
                        if (p instanceof SPacketSpawnPlayer
                                || p instanceof SPacketSpawnObject
                                || p instanceof SPacketSpawnMob
                                || p instanceof SPacketSpawnGlobalEntity
                                || p instanceof SPacketSpawnPainting
                                || p instanceof SPacketSpawnExperienceOrb
                                || p instanceof SPacketDestroyEntities) {
                        //#else
                        //$$ if (p instanceof S0CPacketSpawnPlayer
                        //$$         || p instanceof S0EPacketSpawnObject
                        //$$         || p instanceof S0FPacketSpawnMob
                        //$$         || p instanceof S2CPacketSpawnGlobalEntity
                        //$$         || p instanceof S10PacketSpawnPainting
                        //$$         || p instanceof S11PacketSpawnExperienceOrb
                        //$$         || p instanceof S13PacketDestroyEntities) {
                        //#endif
                            World world = world(mc);
                            for (int i = 0; i < world.loadedEntityList.size(); ++i) {
                                Entity entity = loadedEntityList(world).get(i);
                                if (entity.isDead) {
                                    int chunkX = entity.chunkCoordX;
                                    int chunkY = entity.chunkCoordZ;

                                    //#if MC>=10904
                                    if (entity.addedToChunk && world.getChunkProvider().getLoadedChunk(chunkX, chunkY) != null) {
                                    //#else
                                    //$$ if (entity.addedToChunk && world.getChunkProvider().chunkExists(chunkX, chunkY)) {
                                    //#endif
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

    private Packet deserializePacket(byte[] bytes) throws IOException, IllegalAccessException, InstantiationException {
        ByteBuf bb = Unpooled.wrappedBuffer(bytes);
        PacketBuffer pb = new PacketBuffer(bb);

        int i = readVarInt(pb);

        //#if MC>=10800
        Packet p = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, i);
        //#else
        //$$ Packet p = Packet.generatePacket(EnumConnectionState.PLAY.func_150755_b(), i);
        //#endif
        p.readPacketData(pb);

        return p;
    }

    /**
     * Process a packet and return the result.
     * @param p The packet to process
     * @return The processed packet or {@code null} if no packet shall be sent
     */
    protected Packet processPacket(Packet p) throws Exception {
        //#if MC>=10904
        if (p instanceof SPacketCustomPayload) {
            SPacketCustomPayload packet = (SPacketCustomPayload) p;
        //#else
        //$$ if (p instanceof S3FPacketCustomPayload) {
        //$$     S3FPacketCustomPayload packet = (S3FPacketCustomPayload) p;
        //#endif
            //#if MC>=10800
            String channelName = packet.getChannelName();
            //#else
            //$$ String channelName = packet.func_149169_c();
            //#endif
            if (Restrictions.PLUGIN_CHANNEL.equals(channelName)) {
                final String unknown = replayHandler.getRestrictions().handle(packet);
                if (unknown == null) {
                    return null;
                } else {
                    // Failed to parse options, make sure that under no circumstances further packets are parsed
                    terminateReplay();
                    // Then end replay and show error GUI
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                replayHandler.endReplay();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mc.displayGuiScreen(new GuiErrorScreen(
                                    I18n.format("replaymod.error.unknownrestriction1"),
                                    I18n.format("replaymod.error.unknownrestriction2", unknown)
                            ));
                        }
                    });
                }
            }
        }
        //#if MC>=10904
        if (p instanceof SPacketDisconnect) {
            ITextComponent reason = ((SPacketDisconnect) p).getReason();
        //#else
        //$$ if (p instanceof S40PacketDisconnect) {
            //#if MC>=10809
            //$$ IChatComponent reason = ((S40PacketDisconnect) p).getReason();
            //#else
            //$$ IChatComponent reason = ((S40PacketDisconnect) p).func_149165_c();
            //#endif
        //#endif
            if ("Please update to view this replay.".equals(reason.getUnformattedText())) {
                // This version of the mod supports replay restrictions so we are allowed
                // to remove this packet.
                return null;
            }
        }

        if(BAD_PACKETS.contains(p.getClass())) return null;

        //#if MC>=10904
        if (p instanceof SPacketCustomPayload) {
            SPacketCustomPayload packet = (SPacketCustomPayload) p;
        //#else
        //$$ if (p instanceof S3FPacketCustomPayload) {
        //$$     S3FPacketCustomPayload packet = (S3FPacketCustomPayload) p;
        //#endif
            //#if MC>=10800
            String channelName = packet.getChannelName();
            //#else
            //$$ String channelName = packet.func_149169_c();
            //#endif
            if ("MC|BOpen".equals(channelName)) {
                return null;
            }
        //#if MC>=10800
        }

        //#if MC>=10904
        if(p instanceof SPacketResourcePackSend) {
            SPacketResourcePackSend packet = (SPacketResourcePackSend) p;
            String url = packet.getURL();
        //#else
        //$$ if(p instanceof S48PacketResourcePackSend) {
        //$$     S48PacketResourcePackSend packet = (S48PacketResourcePackSend) p;
            //#if MC>=10809
            //$$ String url = packet.getURL();
            //#else
        //$$     String url = packet.func_179783_a();
            //#endif
        //#endif
            if (url.startsWith("replay://")) {
        //#else
        //$$     String url;
        //$$     if ("MC|RPack".equals(channelName) &&
        //$$             (url = new String(packet.func_149168_d(), Charsets.UTF_8)).startsWith("replay://")) {
        //#endif
                int id = Integer.parseInt(url.substring("replay://".length()));
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index != null) {
                    String hash = index.get(id);
                    if (hash != null) {
                        File file = new File(tempResourcePackFolder, hash + ".zip");
                        if (!file.exists()) {
                            IOUtils.copy(replayFile.getResourcePack(hash).get(), new FileOutputStream(file));
                        }
                        setServerResourcePack(mc.getResourcePackRepository(), file);
                    }
                }
                return null;
            }
        }

        //#if MC>=10904
        if(p instanceof SPacketJoinGame) {
            SPacketJoinGame packet = (SPacketJoinGame) p;
            int entId = packet.getPlayerId();
        //#else
        //$$ if(p instanceof S01PacketJoinGame) {
        //$$     S01PacketJoinGame packet = (S01PacketJoinGame) p;
            //#if MC>=10800
            //$$ int entId = packet.getEntityId();
            //#else
            //$$ int entId = packet.func_149197_c();
            //#endif
        //#endif
            allowMovement = true;
            actualID = entId;
            entId = -1789435; // Camera entity id should be negative which is an invalid id and can't be used by servers
            //#if MC>=10800
            int dimension = packet.getDimension();
            EnumDifficulty difficulty = packet.getDifficulty();
            int maxPlayers = packet.getMaxPlayers();
            WorldType worldType = packet.getWorldType();

            //#if MC>=10904
            p = new SPacketJoinGame(entId, GameType.SPECTATOR, false, dimension,
                    difficulty, maxPlayers, worldType, false);
            //#else
            //$$ p = new S01PacketJoinGame(entId, GameType.SPECTATOR, false, dimension,
            //$$         difficulty, maxPlayers, worldType, false);
            //#endif
            //#else
            //$$ int dimension = packet.func_149194_f();
            //$$ EnumDifficulty difficulty = packet.func_149192_g();
            //$$ int maxPlayers = packet.func_149193_h();
            //$$ WorldType worldType = packet.func_149196_i();
            //$$
            //$$ p = new S01PacketJoinGame(entId, GameType.ADVENTURE, false, dimension,
            //$$         difficulty, maxPlayers, worldType);
            //#endif
        }

        //#if MC>=10904
        if(p instanceof SPacketRespawn) {
            SPacketRespawn respawn = (SPacketRespawn) p;
            p = new SPacketRespawn(respawn.getDimensionID(),
                    respawn.getDifficulty(), respawn.getWorldType(), GameType.SPECTATOR);
        //#else
        //$$ if(p instanceof S07PacketRespawn) {
        //$$     S07PacketRespawn respawn = (S07PacketRespawn) p;
            //#if MC>=10809
            //$$ p = new S07PacketRespawn(respawn.getDimensionID(),
            //$$         respawn.getDifficulty(), respawn.getWorldType(), GameType.SPECTATOR);
            //#else
            //$$ p = new S07PacketRespawn(respawn.func_149082_c(),
            //$$         respawn.func_149081_d(), respawn.func_149080_f(),
                    //#if MC>=10800
                    //$$ GameType.SPECTATOR);
                    //#else
                    //$$ GameType.ADVENTURE);
                    //#endif
            //#endif
        //#endif

            allowMovement = true;
        }

        //#if MC>=10904
        if(p instanceof SPacketPlayerPosLook) {
            final SPacketPlayerPosLook ppl = (SPacketPlayerPosLook) p;
        //#else
        //$$ if(p instanceof S08PacketPlayerPosLook) {
        //$$     final S08PacketPlayerPosLook ppl = (S08PacketPlayerPosLook) p;
        //#endif
            if(!hasWorldLoaded) hasWorldLoaded = true;

            if (mc.currentScreen instanceof GuiDownloadTerrain) {
                // Close the world loading screen manually in case we swallow the packet
                mc.displayGuiScreen(null);
            }

            if(replayHandler.shouldSuppressCameraMovements()) return null;

            CameraEntity cent = replayHandler.getCameraEntity();

            //#if MC>=10800
            //#if MC>=10904
            for (SPacketPlayerPosLook.EnumFlags relative : ppl.getFlags()) {
                if (relative == SPacketPlayerPosLook.EnumFlags.X
                        || relative == SPacketPlayerPosLook.EnumFlags.Y
                        || relative == SPacketPlayerPosLook.EnumFlags.Z) {
            //#else
            //$$ for (Object relative : ppl.func_179834_f()) {
            //$$     if (relative == S08PacketPlayerPosLook.EnumFlags.X
            //$$             || relative == S08PacketPlayerPosLook.EnumFlags.Y
            //$$             || relative == S08PacketPlayerPosLook.EnumFlags.Z) {
            //#endif
                    return null; // At least one of the coordinates is relative, so we don't care
                }
            }
            //#endif

            if(cent != null) {
                //#if MC>=10809
                if(!allowMovement && !((Math.abs(cent.posX - ppl.getX()) > TP_DISTANCE_LIMIT) ||
                        (Math.abs(cent.posZ - ppl.getZ()) > TP_DISTANCE_LIMIT))) {
                //#else
                //$$ if(!allowMovement && !((Math.abs(cent.posX - ppl.func_148932_c()) > TP_DISTANCE_LIMIT) ||
                //$$         (Math.abs(cent.posZ - ppl.func_148933_e()) > TP_DISTANCE_LIMIT))) {
                //#endif
                    return null;
                } else {
                    allowMovement = false;
                }
            }

            new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    if (world(mc) == null || !mc.isCallingFromMinecraftThread()) {
                        ReplayMod.instance.runLater(this);
                        return;
                    }

                    CameraEntity cent = replayHandler.getCameraEntity();
                    //#if MC>=10809
                    cent.setCameraPosition(ppl.getX(), ppl.getY(), ppl.getZ());
                    //#else
                    //$$ cent.setCameraPosition(ppl.func_148932_c(), ppl.func_148928_d(), ppl.func_148933_e());
                    //#endif
                }
            }.run();
        }

        //#if MC>=10904
        if(p instanceof SPacketChangeGameState) {
            SPacketChangeGameState pg = (SPacketChangeGameState)p;
            int reason = pg.getGameState();
        //#else
        //$$ if(p instanceof S2BPacketChangeGameState) {
        //$$     S2BPacketChangeGameState pg = (S2BPacketChangeGameState)p;
            //#if MC>=10809
            //$$ int reason = pg.getGameState();
            //#else
            //$$ int reason = pg.func_149138_c();
            //#endif
        //#endif

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

        //#if MC>=10904
        if (p instanceof SPacketChat) {
        //#else
        //$$ if (p instanceof S02PacketChat) {
        //#endif
            if (!ReplayModReplay.instance.getCore().getSettingsRegistry().get(Setting.SHOW_CHAT)) {
                return null;
            }
        }

        return asyncMode ? processPacketAsync(p) : processPacketSync(p);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        //#if MC>=10904
        ctx.channel().attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).set(EnumConnectionState.PLAY);
        //#else
        //#if MC>=10800
        //$$ ctx.attr(NetworkManager.attrKeyConnectionState).set(EnumConnectionState.PLAY);
        //#endif
        //#endif
        super.channelActive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // The embedded channel's event loop will consider every thread to be in it and as such provides no
        // guarantees that only one thread is using the pipeline at any one time.
        // For reading the replay sender (either sync or async) is the only thread ever writing.
        // For writing it may very well happen that multiple threads want to use the pipline at the same time.
        // It's unclear whether the EmbeddedChannel is supposed to be thread-safe (the behavior of the event loop
        // does suggest that). However it seems like it either isn't (likely) or there is a race condition.
        // See: https://www.replaymod.com/forum/thread/1752#post8045 (https://paste.replaymod.com/lotacatuwo)
        // To work around this issue, we just outright drop all write/flush requests (they aren't needed anyway).
        // This still leaves channel handlers upstream with the threading issue but they all seem to cope well with it.
        promise.setSuccess();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // See write method above
    }

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public boolean paused() {
        //#if MC>=11200
        return mc.timer.tickLength == Float.POSITIVE_INFINITY;
        //#else
        //$$ return mc.timer.timerSpeed == 0;
        //#endif
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
        //#if MC>=11200
        mc.timer.tickLength = WrappedTimer.DEFAULT_MS_PER_TICK / (float) d;
        //#else
        //$$ mc.timer.timerSpeed = (float) d;
        //#endif
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
                while (!terminate) {
                    synchronized (ReplaySender.this) {
                        if (replayIn == null) {
                            replayIn = replayFile.getPacketData();
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
                                    nextPacket = new PacketData(replayIn);
                                }

                                int nextTimeStamp = nextPacket.timestamp;

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
                                channelRead(ctx, nextPacket.bytes);
                                nextPacket = null;

                                lastTimeStamp = nextTimeStamp;

                                // In case we finished jumping
                                // We need to check that we aren't planing to restart so we don't accidentally run this
                                // code before we actually restarted
                                if (isHurrying() && lastTimeStamp > desiredTimeStamp && !startFromBeginning) {
                                    desiredTimeStamp = -1;

                                    replayHandler.moveCameraToTargetPosition();

                                    // Pause after jumping
                                    setReplaySpeed(0);
                                }
								// RAH Begin - If replay is playing, everything is setup, so we can launch initKeyFrames and rendering
								// Things are complicated because this thread is not the MC threads, so we have to jump through hoops
								// I need to be able to send a message to replayHandler or guiPathing - somehow!
								if (!automationInitialization && lastTimeStamp >= 2977) {
									LogManager.getLogger().debug("Triggering event");
									automationInitialization = true;
									setReplaySpeed(0);
									/// Events to ReplayModSimplePathing weren't adequate - resulted in No OpenGL context found in thread error
									//FML_BUS.post(new ReplayPlayingEvent.Post(replayHandler)); // Events spawn a new thread, must be MC thread
									//replayHandler.startedReplay();
								}
								// RAH End
                            } catch (EOFException eof) {
                                // Reached end of file
								LogManager.getLogger().debug("End of File");
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
                        replayHandler.restartedReplay();
                        if (replayIn != null) {
                            replayIn.close();
                            replayIn = null;
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
			LogManager.getLogger().debug("RAH: lastTimeStamp " + lastTimeStamp + " startFromBeginning"); 
            startFromBeginning = true;
        }

        desiredTimeStamp = millis;
    }

    protected Packet processPacketAsync(Packet p) {
        //If hurrying, ignore some packets, except for short durations
        if(desiredTimeStamp - lastTimeStamp > 1000) {
            //#if MC>=10904
            if(p instanceof SPacketParticles) return null;

            if(p instanceof SPacketSpawnObject) {
                SPacketSpawnObject pso = (SPacketSpawnObject)p;
                int type = pso.getType();
            //#else
            //$$ if(p instanceof S2APacketParticles) return null;
            //$$
            //$$ if(p instanceof S0EPacketSpawnObject) {
            //$$     S0EPacketSpawnObject pso = (S0EPacketSpawnObject)p;
                //#if MC>=10809
                //$$ int type = pso.getType();
                //#else
                //$$ int type = pso.func_148993_l();
                //#endif
            //#endif
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
                if (timestamp == lastTimeStamp) { // Do nothing if we're already there
                    return;
                }
                if (timestamp < lastTimeStamp) { // Restart the replay if we need to go backwards in time
                    hasWorldLoaded = false;
                    lastTimeStamp = 0;
                    if (replayIn != null) {
                        replayIn.close();
                        replayIn = null;
                    }
                    startFromBeginning = false;
                    nextPacket = null;
                    replayHandler.restartedReplay();
                }

                if (replayIn == null) {
                    replayIn = replayFile.getPacketData();
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
                            pd = new PacketData(replayIn);
                        }

                        int nextTimeStamp = pd.timestamp;
                        if (nextTimeStamp > timestamp) {
                            // We are done sending all packets
                            nextPacket = pd;
                            break;
                        }

                        // Process packet
                        channelRead(ctx, pd.bytes);
                    } catch (EOFException eof) {
                        // Shit! We hit the end before finishing our job! What shall we do now?
                        // well, let's just pretend we're done...
                        replayIn = null;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // This might be required if we change to async mode anytime soon
                lastPacketSent = System.currentTimeMillis();
                lastTimeStamp = timestamp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Packet processPacketSync(Packet p) {
        //#if MC>=10904
        if (p instanceof SPacketUnloadChunk) {
            SPacketUnloadChunk packet = (SPacketUnloadChunk) p;
            int x = packet.getX();
            int z = packet.getZ();
        //#else
        //#if MC>=10809
        //$$ if (p instanceof S21PacketChunkData && ((S21PacketChunkData) p).getExtractedSize() == 0) {
        //$$     S21PacketChunkData packet = (S21PacketChunkData) p;
        //$$     int x = packet.getChunkX();
        //$$     int z = packet.getChunkZ();
        //#else
        //$$ if (p instanceof S21PacketChunkData && ((S21PacketChunkData) p).func_149276_g() == 0) {
        //$$     S21PacketChunkData packet = (S21PacketChunkData) p;
        //$$     int x = packet.func_149273_e();
        //$$     int z = packet.func_149271_f();
        //#endif
        //#endif
            // If the chunk is getting unloaded, we will have to forcefully update the position of all entities
            // within. Otherwise, if there wasn't a game tick recently, there may be entities that have moved
            // out of the chunk by now but are still registered in it. If we do not update those, they will get
            // unloaded even though they shouldn't.
            // Note: This is only half of the truth. Entities may be removed by chunk-unloading, see else-case below.
            // To make things worse, it seems like players were never supposed to be unloaded this way because
            // they will remain glitched in the World#playerEntities list.
            World world = world(mc);
            IChunkProvider chunkProvider = world.getChunkProvider();
            // Get the chunk that will be unloaded
            Chunk chunk = chunkProvider.provideChunk(x, z);
            if (!chunk.isEmpty()) {
                List<Entity> entitiesInChunk = new ArrayList<>();
                // Gather all entities in that chunk
                for (Collection<Entity> entityList : getEntityLists(chunk)) {
                    entitiesInChunk.addAll(entityList);
                }
                for (Entity entity : entitiesInChunk) {
                    // Skip interpolation of position updates coming from server
                    // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
                    // Needs to be called at least 4 times thanks to
                    // EntityOtherPlayerMP#otherPlayerMPPosRotationIncrements (max vanilla value is 3)
                    for (int i = 0; i < 4; i++) {
                        entity.onUpdate();
                    }

                    // Check whether the entity has left the chunk
                    int chunkX = floor(entity.posX / 16);
                    int chunkZ = floor(entity.posZ / 16);
                    if (entity.chunkCoordX != chunkX || entity.chunkCoordZ != chunkZ) {
                        // Entity has left the chunk
                        chunk.removeEntityAtIndex(entity, entity.chunkCoordY);
                        //#if MC>=10904
                        Chunk newChunk = chunkProvider.getLoadedChunk(chunkX, chunkZ);
                        //#else
                        //$$ Chunk newChunk = chunkProvider.chunkExists(chunkX, chunkZ)
                        //$$         ? chunkProvider.provideChunk(chunkX, chunkZ) : null;
                        //#endif
                        if (newChunk != null) {
                            newChunk.addEntity(entity);
                        } else {
                            // Entity has left all loaded chunks
                            entity.addedToChunk = false;
                        }
                    } else {
                        // When entities remain in a chunk that's to be unloaded, they'll only be added to a unload
                        // queue and remain loaded as before until the next tick (which during jumping is way off).
                        // So, if they are re-spawned with the same entity id, MC actually cleans up the old entity and
                        // then adds the new one but leaves the unload queue as is.
                        // Finally, on the next tick the legitimate entity will be unloaded because it's part of the
                        // unload queue (entities .equals based purely on their id). However, the old entity object
                        // is used to determine the chunk the entity is removed from and in this case that'll allow the
                        // legitimate entity to remain registered in a loaded chunk, causing them to still be rendered.
                        //
                        // The usual removal-due-to-chunk-unload process will, without touching the entityList, call
                        // onEntityRemoved. In that method WorldClient checks to see whether the entity is still in the
                        // entityList (which it is) and then adds it to the entitySpawnQueue.
                        // As the final result the entity will remain loaded.
                        // To get the same result without ticking, we just remove the entity from the to-be-unloaded
                        // chunk but keep it loaded otherwise. They won't be rendered because they're not part of any
                        // chunk and will be removed properly if the server decides to re-spawn the entity.
                        chunk.removeEntityAtIndex(entity, entity.chunkCoordY);
                        entity.addedToChunk = false;
                    }
                }
            }
        }
        return p; // During synchronous playback everything is sent normally
    }

    private static final class PacketData {
        private static final ByteBuf byteBuf = Unpooled.buffer();
        private static final ByteBufOutputStream byteBufOut = new ByteBufOutputStream(byteBuf);
        private static final ReplayOutputStream encoder = new ReplayOutputStream(new ReplayStudio(), byteBufOut);
        private final int timestamp;
        private final byte[] bytes;

        public PacketData(ReplayInputStream in) throws IOException {
            com.replaymod.replaystudio.PacketData data = in.readPacket();
            timestamp = (int) data.getTime();
            // We need to re-encode MCProtocolLib packets, so we can later decode them as NMS packets
            // The main reason we aren't reading them as NMS packets is that we want ReplayStudio to be able
            // to apply ViaVersion (and potentially other magic) to it.
            synchronized (encoder) {
                byteBuf.markReaderIndex(); // Mark the current reader and writer index (should be at start)
                byteBuf.markWriterIndex();

                encoder.write(data); // Re-encode packet, data will end up in byteBuf
                encoder.flush();

                byteBuf.skipBytes(8); // Skip packet length & timestamp
                bytes = new byte[byteBuf.readableBytes()]; // Create bytes array of sufficient size
                byteBuf.readBytes(bytes); // Read all data into bytes

                byteBuf.resetReaderIndex(); // Reset reader & writer index for next use
                byteBuf.resetWriterIndex();
            }
        }
    }
}
