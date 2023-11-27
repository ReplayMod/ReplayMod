//#if MC>=10800
package com.replaymod.replay;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.rar.RandomAccessReplay;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.NetworkState;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;

//#if MC>=11200
import com.replaymod.core.utils.WrappedTimer;
//#endif

import static com.replaymod.core.versions.MCVer.asMc;
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
    private final RandomAccessReplay replay;
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

    public QuickReplaySender(ReplayModReplay mod, ReplayFile replayFile) {
        this.mod = mod;
        this.replay = new RandomAccessReplay(replayFile, getPacketTypeRegistry(State.PLAY)) {
            private byte[] buf = new byte[0];

            @Override
            protected void dispatch(com.replaymod.replaystudio.protocol.Packet packet) {
                com.github.steveice10.netty.buffer.ByteBuf byteBuf = packet.getBuf();
                int size = byteBuf.readableBytes();
                if (buf.length < size) {
                    buf = new byte[size];
                }
                byteBuf.getBytes(byteBuf.readerIndex(), buf, 0, size);
                ByteBuf wrappedBuf = Unpooled.wrappedBuffer(buf);
                wrappedBuf.writerIndex(size);
                PacketByteBuf packetByteBuf = new PacketByteBuf(wrappedBuf);

                NetworkState state = asMc(packet.getRegistry().getState());
                //#if MC>=10809
                Packet<?> mcPacket;
                //#else
                //$$ Packet mcPacket;
                //#endif
                //#if MC>=12002
                //$$ mcPacket = state.getHandler(NetworkSide.CLIENTBOUND).createPacket(packet.getId(), packetByteBuf);
                //#elseif MC>=11700
                //$$ mcPacket = state.getPacketHandler(NetworkSide.CLIENTBOUND, packet.getId(), packetByteBuf);
                //#elseif MC>=11500
                mcPacket = state.getPacketHandler(NetworkSide.CLIENTBOUND, packet.getId());
                //#else
                //$$ try {
                //$$     mcPacket = state.getPacketHandler(NetworkSide.CLIENTBOUND, packet.getId());
                //$$ } catch (IllegalAccessException | InstantiationException e) {
                //$$     e.printStackTrace();
                //$$     return;
                //$$ }
                //#endif
                if (mcPacket != null) {
                    //#if MC<11700
                    try {
                        mcPacket.read(packetByteBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    //#endif

                    ctx.fireChannelRead(mcPacket);
                }
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
                mod.getCore().runLaterWithoutLock(() -> {
                    mod.getCore().printWarningToChat("Error initializing quick replay sender: %s", e.getLocalizedMessage());
                    promise.setException(e);
                });
                return;
            }
            mod.getCore().runLaterWithoutLock(() -> promise.set(null));
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
        { on(PreTickCallback.EVENT, this::onTick); }
        private void onTick() {
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
