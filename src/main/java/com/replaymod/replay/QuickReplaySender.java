//#if MC>=10904
package com.replaymod.replay;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetInput;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.util.RandomAccessReplay;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.NetworkState;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;

//#if FABRIC>=1
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
//#else
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.event.TickEvent;
//$$
//$$ import static com.replaymod.core.versions.MCVer.FML_BUS;
//#endif

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

//#if MC>=11602
//$$ import net.minecraft.util.registry.DynamicRegistryManager;
//$$ import net.minecraft.util.registry.Registry;
//#endif

//#if MC>=11600
//$$ import net.minecraft.world.World;
//#else
import net.minecraft.world.level.LevelGeneratorType;
//#endif
//#if MC>=11400
import net.minecraft.world.dimension.DimensionType;
//#else
//$$ import net.minecraft.world.EnumDifficulty;
//#endif

//#if MC>=11200
import com.replaymod.core.utils.WrappedTimer;
//#endif

//#if MC>=11002
import net.minecraft.world.GameMode;
//#else
//$$ import net.minecraft.world.WorldSettings.GameType;
//#endif

import static com.replaymod.core.versions.MCVer.getMinecraft;
import static com.replaymod.core.versions.MCVer.getPacketTypeRegistry;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

/**
 * Sends only chunk updates and entity position updates but tries to do so as quickly as possible.
 * To do so, it performs an initial analysis of the replay, scanning all of its packets and storing entity positions
 * and chunk states while doing so.
 * This allows it to later jump to any time by doing a diff from the current time (including backwards jumping).
 */
@ChannelHandler.Sharable
public class QuickReplaySender extends ChannelHandlerAdapter implements ReplaySender {
    private final MinecraftClient mc = getMinecraft();

    private final ReplayModReplay mod;
    private final RandomAccessReplay<Packet<?>> replay;
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

    public QuickReplaySender(ReplayModReplay mod, ReplayFile replayFile) {
        this.mod = mod;
        this.replay = new RandomAccessReplay<Packet<?>>(replayFile, getPacketTypeRegistry(false)) {
            private byte[] buf = new byte[0];

            @Override
            protected Packet<?> decode(com.github.steveice10.netty.buffer.ByteBuf byteBuf) throws IOException {
                int packetId = new ByteBufNetInput(byteBuf).readVarInt();
                //#if MC>=11500
                Packet<?> mcPacket = NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, packetId);
                //#else
                //$$ Packet<?> mcPacket;
                //$$ try {
                //$$     mcPacket = NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, packetId);
                //$$ } catch (IllegalAccessException | InstantiationException e) {
                //$$     throw new IOException(e);
                //$$ }
                //#endif
                if (mcPacket != null) {
                    int size = byteBuf.readableBytes();
                    if (buf.length < size) {
                        buf = new byte[size];
                    }
                    byteBuf.readBytes(buf, 0, size);
                    ByteBuf wrappedBuf = Unpooled.wrappedBuffer(buf);
                    wrappedBuf.writerIndex(size);
                    mcPacket.read(new PacketByteBuf(wrappedBuf));
                }
                return mcPacket;
            }

            @Override
            protected void dispatch(Packet<?> packet) {
                ctx.fireChannelRead(packet);
            }
        };
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
                replay.load(progress);
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
        replay.reset();
        ctx.fireChannelRead(new PlayerRespawnS2CPacket(
                //#if MC>=11600
                //#if MC>=11602
                //$$ DimensionType.addRegistryDefaults(new DynamicRegistryManager.Impl()).get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.OVERWORLD_REGISTRY_KEY),
                //#else
                //$$ DimensionType.OVERWORLD_REGISTRY_KEY,
                //#endif
                //$$ World.OVERWORLD,
                //$$ 0,
                //$$ GameMode.SPECTATOR,
                //$$ GameMode.SPECTATOR,
                //$$ false,
                //$$ false,
                //$$ false
                //#else
                //#if MC>=11400
                DimensionType.OVERWORLD,
                //#else
                //$$ 0,
                //#endif
                //#if MC>=11500
                0,
                //#endif
                //#if MC<11400
                //$$ EnumDifficulty.NORMAL,
                //#endif
                LevelGeneratorType.DEFAULT,
                GameMode.SPECTATOR
                //#endif
        ));
        ctx.fireChannelRead(new PlayerPositionLookS2CPacket(0, 0, 0, 0, 0, Collections.emptySet(), 0));
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
        //#if FABRIC>=1
        { on(PreTickCallback.EVENT, this::onTick); }
        private void onTick() {
        //#else
        //$$ @SubscribeEvent
        //$$ public void onTick(TickEvent.ClientTickEvent event) {
        //$$     if (event.phase != TickEvent.Phase.START) return;
        //#endif
            if (!asyncMode || paused()) return;

            long now = System.currentTimeMillis();
            long realTimePassed = now - lastAsyncUpdateTime;
            lastAsyncUpdateTime = now;
            int replayTimePassed = (int) (realTimePassed * replaySpeed);
            sendPacketsTill(currentTimeStamp + replayTimePassed);
        }
    }

    @Override
    public void sendPacketsTill(int replayTime) {
        ensureInitialized(() -> {
            try {
                replay.seek(replayTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentTimeStamp = replayTime;
        });
    }
}
//#endif
