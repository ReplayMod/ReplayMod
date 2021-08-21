package com.replaymod.simplepathing;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.SettingsChangedCallback;
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
import net.minecraft.client.gui.screen.Screen;
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

public class ReplayModSimplePathing extends EventRegistrations implements Module {
    { instance = this; }
    public static ReplayModSimplePathing instance;

    private ReplayMod core;
    public KeyBindingRegistry.Binding keyPositionKeyframe;
    public KeyBindingRegistry.Binding keyTimeKeyframe;
    public KeyBindingRegistry.Binding keySyncTime;

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
        keySyncTime = core.getKeyBindingRegistry().registerRepeatedKeyBinding("replaymod.input.synctimeline", Keyboard.KEY_V, () -> {
            if (guiPathing != null) guiPathing.syncTimeButtonPressed();
        }, true);
        SettingsRegistry settingsRegistry = core.getSettingsRegistry();
        keySyncTime.registerAutoActivationSupport(settingsRegistry.get(Setting.AUTO_SYNC), active -> {
            settingsRegistry.set(Setting.AUTO_SYNC, active);
            settingsRegistry.save();
        });
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_DELETE, () ->
                guiPathing != null && guiPathing.deleteButtonPressed());
        keyPositionKeyframe = core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.positionkeyframe", Keyboard.KEY_I, () -> {
            if (guiPathing != null) guiPathing.toggleKeyframe(SPPath.POSITION, false);
        }, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.positiononlykeyframe", 0, () -> {
            if (guiPathing != null) guiPathing.toggleKeyframe(SPPath.POSITION, true);
        }, true);
        keyTimeKeyframe = core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.timekeyframe", Keyboard.KEY_O, () -> {
            if (guiPathing != null) guiPathing.toggleKeyframe(SPPath.TIME, false);
        }, true);
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.bothkeyframes", 0, () -> {
            if (guiPathing != null) {
                guiPathing.toggleKeyframe(SPPath.TIME, false);
                guiPathing.toggleKeyframe(SPPath.POSITION, false);
            }
        }, true);
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_Z, () -> {
            if (Screen.hasControlDown() && currentTimeline != null) {
                Timeline timeline = currentTimeline.getTimeline();
                if (Screen.hasShiftDown()) {
                    if (timeline.peekRedoStack() != null) {
                        timeline.redoLastChange();
                    }
                } else {
                    if (timeline.peekUndoStack() != null) {
                        timeline.undoLastChange();
                    }
                }
                return true;
            }
            return false;
        });
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_Y, () -> {
            if (Screen.hasControlDown() && currentTimeline != null) {
                Timeline timeline = currentTimeline.getTimeline();
                if (timeline.peekRedoStack() != null) {
                    timeline.redoLastChange();
                }
                return true;
            }
            return false;
        });
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpened); }
    private void onReplayOpened(ReplayHandler replayHandler) {
        ReplayFile replayFile = replayHandler.getReplayFile();
        try {
            synchronized (replayFile) {
                Timeline timeline = replayFile.getTimelines(new SPTimeline()).get("");
                if (timeline != null) {
                    setCurrentTimeline(new SPTimeline(timeline), false);
                } else {
                    setCurrentTimeline(new SPTimeline(), false);
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
    private long selectedTime;

    public SPPath getSelectedPath() {
        if (getReplayOverlay().timeline.getSelectedMarker() != null) {
            selectedPath = null;
            selectedTime = 0;
        }
        return selectedPath;
    }

    public long getSelectedTime() {
        return selectedTime;
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
        setCurrentTimeline(newTimeline, true);
    }

    private void setCurrentTimeline(SPTimeline newTimeline, boolean save) {
        selectedPath = null;
        currentTimeline = newTimeline;
        if (!save) {
            lastTimeline = newTimeline;
            lastChange = newTimeline.getTimeline().peekUndoStack();
        }
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
    private SPTimeline lastTimeline;
    private Change lastChange;
    private void maybeSaveTimeline(ReplayFile replayFile) {
        SPTimeline spTimeline = currentTimeline;
        if (spTimeline == null || saveService == null) {
            lastTimeline = null;
            lastChange = null;
            return;
        }

        Change latestChange = spTimeline.getTimeline().peekUndoStack();
        if (spTimeline == lastTimeline && latestChange == lastChange) {
            return;
        }
        lastTimeline = spTimeline;
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
