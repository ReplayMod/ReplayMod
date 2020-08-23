package com.replaymod.simplepathing;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedCallback;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayClosingCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import com.replaymod.simplepathing.gui.GuiPathing;
import com.replaymod.simplepathing.preview.PathPreview;
import lombok.Getter;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.core.versions.MCVer.Entity_getZ;

public class ReplayModSimplePathing extends EventRegistrations implements Module {
    { instance = this; }
    public static ReplayModSimplePathing instance;

    private ReplayMod core;

    public static Logger LOGGER = LogManager.getLogger();

    private GuiPathing guiPathing;
    private PathPreview pathPreview = new PathPreview(this);

    public ReplayModSimplePathing(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);

        on(SettingsChangedCallback.EVENT, (registry, key) -> {
            if (key == Setting.DEFAULT_INTERPOLATION) {
                if (currentTimeline != null && guiPathing != null) {
                    updateDefaultInterpolatorType();
                }
            }
        });
    }

    @Override
    public void register() {
        super.register();
        pathPreview.register();
    }

    @Override
    public void unregister() {
        super.unregister();
        pathPreview.unregister();
    }

    @Override
    public void initClient() {
        register();
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        pathPreview.registerKeyBindings(registry);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.keyframerepository", Keyboard.KEY_X, () -> {
            if (guiPathing != null) guiPathing.keyframeRepoButtonPressed();
        }, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.clearkeyframes", Keyboard.KEY_C, () -> {
            if (guiPathing != null) guiPathing.clearKeyframesButtonPressed();
        }, true);
        core.getKeyBindingRegistry().registerRepeatedKeyBinding("replaymod.input.synctimeline", Keyboard.KEY_V, () -> {
            if (guiPathing != null) guiPathing.syncTimeButtonPressed();
        }, true);
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_DELETE, () -> {
            if (guiPathing != null) guiPathing.deleteButtonPressed();
        });

        final Runnable INSERT_PK = () -> {
            if (null == guiPathing) return;
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            SPTimeline timeline = this.getCurrentTimeline();
            CameraEntity camera = replayHandler.getCameraEntity();
            int spectatedId = !replayHandler.isCameraView() ? getRenderViewEntity(replayHandler.getOverlay().getMinecraft()).getEntityId() : -1;
            timeline.addPositionKeyframe(guiPathing.timeline.getCursorPosition(), Entity_getX(camera), Entity_getY(camera), Entity_getZ(camera),
                    camera.yaw, camera.pitch, camera.roll, spectatedId);
        };
        final Runnable INSERT_TK = () -> {
            if (null == guiPathing) return;
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            SPTimeline timeline = this.getCurrentTimeline();
            timeline.addTimeKeyframe(guiPathing.timeline.getCursorPosition(), replayHandler.getReplaySender().currentTimeStamp());
        };
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.insertpositionkeyframe", Keyboard.KEY_I, INSERT_PK, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.insertabspositionkeyframe", Keyboard.KEY_U, () -> {
            if (null == guiPathing) return;
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            SPTimeline timeline = this.getCurrentTimeline();
            CameraEntity camera = replayHandler.getCameraEntity();
            timeline.addPositionKeyframe(guiPathing.timeline.getCursorPosition(), Entity_getX(camera), Entity_getY(camera), Entity_getZ(camera),
                    camera.yaw, camera.pitch, camera.roll, -1);
        }, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.inserttimekeyframe", Keyboard.KEY_O, INSERT_TK, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.insertbothkeyframes", 0, () -> {
            INSERT_PK.run();
            INSERT_TK.run();
        }, true);
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpened); }
    private void onReplayOpened(ReplayHandler replayHandler) {
        ReplayFile replayFile = replayHandler.getReplayFile();
        try {
            synchronized (replayFile) {
                Timeline timeline = replayFile.getTimelines(new SPTimeline()).get("");
                if (timeline != null) {
                    setCurrentTimeline(new SPTimeline(timeline));
                } else {
                    setCurrentTimeline(new SPTimeline());
                }
            }
        } catch (IOException e) {
            throw new CrashException(CrashReport.create(e, "Reading timeline"));
        }

        guiPathing = new GuiPathing(core, this, replayHandler);

        saveService = Executors.newSingleThreadExecutor();
        new Runnable() {
            @Override
            public void run() {
                maybeSaveTimeline(replayFile);
                if (guiPathing != null) {
                    core.runLater(this);
                }
            }
        }.run();
    }

    { on(ReplayClosingCallback.EVENT, replayHandler -> onReplayClosing()); }
    private void onReplayClosing() {
        saveService.shutdown();
        try {
            saveService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveService = null;
    }

    { on(ReplayClosedCallback.EVENT, replayHandler -> onReplayClosed()); }
    private void onReplayClosed() {
        currentTimeline = null;
        guiPathing = null;
        selectedPath = null;
    }

    private GuiReplayOverlay getReplayOverlay() {
        return ReplayModReplay.instance.getReplayHandler().getOverlay();
    }

    private SPTimeline currentTimeline;

    private SPPath selectedPath;
    @Getter
    private long selectedTime;

    public SPPath getSelectedPath() {
        if (getReplayOverlay().timeline.getSelectedMarker() != null) {
            selectedPath = null;
            selectedTime = 0;
        }
        return selectedPath;
    }

    public boolean isSelected(Keyframe keyframe) {
        return getSelectedPath() != null && currentTimeline.getKeyframe(selectedPath, selectedTime) == keyframe;
    }

    public void setSelected(SPPath path, long time) {
        selectedPath = path;
        selectedTime = time;
        if (selectedPath != null) {
            getReplayOverlay().timeline.setSelectedMarker(null);
        }
    }

    public void setCurrentTimeline(SPTimeline newTimeline) {
        selectedPath = null;
        currentTimeline = newTimeline;
        updateDefaultInterpolatorType();
    }

    public void clearCurrentTimeline() {
        setCurrentTimeline(new SPTimeline());
    }

    public SPTimeline getCurrentTimeline() {
        return currentTimeline;
    }

    private void updateDefaultInterpolatorType() {
        InterpolatorType newDefaultType =
                InterpolatorType.fromString(core.getSettingsRegistry().get(Setting.DEFAULT_INTERPOLATION));
        currentTimeline.setDefaultInterpolatorType(newDefaultType);
    }

    public ReplayMod getCore() {
        return core;
    }

    public GuiPathing getGuiPathing() {
        return guiPathing;
    }

    private final AtomicInteger lastSaveId = new AtomicInteger();
    private ExecutorService saveService;
    private Change lastChange;
    private void maybeSaveTimeline(ReplayFile replayFile) {
        SPTimeline spTimeline = currentTimeline;
        if (spTimeline == null || saveService == null) {
            lastChange = null;
            return;
        }

        Change latestChange = spTimeline.getTimeline().peekUndoStack();
        if (latestChange == null || latestChange == lastChange) {
            return;
        }
        lastChange = latestChange;

        // Clone the timeline for async saving
        Timeline timeline;
        try {
            TimelineSerialization serialization = new TimelineSerialization(spTimeline, null);
            String serialized = serialization.serialize(Collections.singletonMap("", spTimeline.getTimeline()));
            timeline = serialization.deserialize(serialized).get("");
        } catch (Throwable t) {
            CrashReport report = CrashReport.create(t, "Cloning timeline");
            throw new CrashException(report);
        }

        int id = lastSaveId.incrementAndGet();
        saveService.submit(() -> {
            if (lastSaveId.get() != id) {
                return; // Another job has been scheduled, it will do the hard work.
            }
            try {
                saveTimeline(replayFile, spTimeline, timeline);
            } catch (IOException e) {
                LOGGER.error("Auto-saving timeline:", e);
            }
        });
    }

    private void saveTimeline(ReplayFile replayFile, PathingRegistry pathingRegistry, Timeline timeline) throws IOException {
        synchronized (replayFile) {
            Map<String, Timeline> timelineMap = replayFile.getTimelines(pathingRegistry);
            timelineMap.put("", timeline);
            replayFile.writeTimelines(pathingRegistry, timelineMap);
        }
    }
}
