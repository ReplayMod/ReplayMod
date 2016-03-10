package com.replaymod.pathing.player;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.replay.ReplayHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Plays a timeline.
 */
public abstract class AbstractTimelinePlayer {
    private final ReplayHandler replayHandler;
    private Timeline timeline;
    private long lastTimestamp;
    private ListenableFuture<Void> future;
    private SettableFuture<Void> settableFuture;

    public AbstractTimelinePlayer(ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
    }

    public ListenableFuture<Void> start(Timeline timeline) {
        this.timeline = timeline;

        Iterator<Keyframe> iter = FluentIterable.from(timeline.getPaths())
                .transformAndConcat(new Function<Path, Iterable<Keyframe>>() {
            @Nullable
            @Override
            public Iterable<Keyframe> apply(@Nullable Path input) {
                assert input != null;
                return input.getKeyframes();
            }
        }).iterator();
        if (!iter.hasNext()) {
            lastTimestamp = 0;
        } else {
            lastTimestamp = new Ordering<Keyframe>() {
                @Override
                public int compare(@Nullable Keyframe left, @Nullable Keyframe right) {
                    assert left != null;
                    assert right != null;
                    return Longs.compare(left.getTime(), right.getTime());
                }
            }.max(iter).getTime();
        }

        replayHandler.getReplaySender().setSyncModeAndWait();
        FMLCommonHandler.instance().bus().register(this);
        return future = settableFuture = SettableFuture.create();
    }

    public ListenableFuture<Void> getFuture() {
        return future;
    }

    public boolean isActive() {
        return future != null && !future.isDone();
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        if (future.isDone()) {
            replayHandler.getReplaySender().setAsyncMode(true);
            FMLCommonHandler.instance().bus().unregister(this);
            return;
        }
        long time = getTimePassed();
        if (time > lastTimestamp) {
            time = lastTimestamp;
        }
        timeline.applyToGame(time, replayHandler);
        if (time == lastTimestamp) {
            settableFuture.set(null);
        }
    }

    public abstract long getTimePassed();
}
