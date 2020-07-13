package com.replaymod.pathing.player;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Plays a timeline.
 */
public abstract class AbstractTimelinePlayer extends EventRegistrations {
    private final MinecraftClient mc = getMinecraft();
    private final ReplayHandler replayHandler;
    private Timeline timeline;
    protected long startOffset;
    private long lastTime;
    private long lastTimestamp;
    private ListenableFuture<Void> future;
    private SettableFuture<Void> settableFuture;

    public AbstractTimelinePlayer(ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
    }

    public ListenableFuture<Void> start(Timeline timeline, long from) {
        startOffset = from;
        return start(timeline);
    }

    public ListenableFuture<Void> start(Timeline timeline) {
        this.timeline = timeline;

        Iterator<Keyframe> iter = Iterables.concat(Iterables.transform(timeline.getPaths(),
                new Function<Path, Iterable<Keyframe>>() {
            @Nullable
            @Override
            public Iterable<Keyframe> apply(@Nullable Path input) {
                assert input != null;
                return input.getKeyframes();
            }
        })).iterator();
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
        register();
        lastTime = 0;

        MinecraftAccessor mcA = (MinecraftAccessor) mc;
        ReplayTimer timer = new ReplayTimer(mcA.getTimer());
        mcA.setTimer(timer);

        //noinspection ConstantConditions
        TimerAccessor timerA = (TimerAccessor) timer;
        //#if MC>=11200
        timerA.setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
        timer.tickDelta = timer.ticksThisFrame = 0;
        //#else
        //$$ timer.timerSpeed = 1;
        //$$ timer.elapsedPartialTicks = timer.elapsedTicks = 0;
        //#endif
        return future = settableFuture = SettableFuture.create();
    }

    public ListenableFuture<Void> getFuture() {
        return future;
    }

    public boolean isActive() {
        return future != null && !future.isDone();
    }

    { on(ReplayTimer.UpdatedCallback.EVENT, this::onTick); }
    public void onTick() {
        if (future.isDone()) {
            MinecraftAccessor mcA = (MinecraftAccessor) mc;
            mcA.setTimer(((ReplayTimer) mcA.getTimer()).getWrapped());
            replayHandler.getReplaySender().setReplaySpeed(0);
            replayHandler.getReplaySender().setAsyncMode(true);
            unregister();
            return;
        }
        long time = getTimePassed();
        if (time > lastTimestamp) {
            time = lastTimestamp;
        }

        // Apply to timeline
        timeline.applyToGame(time, replayHandler);
        // Apply a second time in case same of the packets have moved the camera from where it was
        timeline.applyToGame(time, replayHandler);

        // Update minecraft timer
        long replayTime = replayHandler.getReplaySender().currentTimeStamp();
        if (lastTime == 0) {
            // First frame, no change yet
            lastTime = replayTime;
        }
        float timeInTicks = replayTime / 50f;
        float previousTimeInTicks = lastTime / 50f;
        float passedTicks = timeInTicks - previousTimeInTicks;
        RenderTickCounter renderTickCounter = ((MinecraftAccessor) mc).getTimer();
        if (renderTickCounter instanceof ReplayTimer) {
            ReplayTimer timer = (ReplayTimer) renderTickCounter;
            timer.tickDelta += passedTicks;
            timer.ticksThisFrame = (int) timer.tickDelta;
            timer.tickDelta -= timer.ticksThisFrame;
        }

        lastTime = replayTime;

        if (time >= lastTimestamp) {
            settableFuture.set(null);
        }
    }

    public abstract long getTimePassed();
}
