package com.replaymod.simplepathing;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.SettingsChangedEvent;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayCloseEvent;
import com.replaymod.replay.events.ReplayOpenEvent;
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
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

//#if MC>=10800
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//#endif

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.replaymod.core.versions.MCVer.*;

@Mod(modid = ReplayModSimplePathing.MOD_ID,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        //#if MC>=10800
        clientSideOnly = true,
        //#endif
        useMetadata = true)
public class ReplayModSimplePathing {
    public static final String MOD_ID = "replaymod-simplepathing";

    @Mod.Instance(MOD_ID)
    public static ReplayModSimplePathing instance;

    private ReplayMod core;

    public static Logger LOGGER;

    private GuiPathing guiPathing;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        core = ReplayMod.instance;

        core.getSettingsRegistry().register(Setting.class);

        FML_BUS.register(this);

        PathPreview pathPreview = new PathPreview(this);
        pathPreview.register();

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.keyframerepository", Keyboard.KEY_X, () -> {
            if (guiPathing != null) guiPathing.keyframeRepoButtonPressed();
        });
        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.clearkeyframes", Keyboard.KEY_C, () -> {
            if (guiPathing != null) guiPathing.clearKeyframesButtonPressed();
        });
        core.getKeyBindingRegistry().registerRepeatedKeyBinding("replaymod.input.synctimeline", Keyboard.KEY_V, () -> {
            if (guiPathing != null) guiPathing.syncTimeButtonPressed();
        });
        core.getKeyBindingRegistry().registerRaw(Keyboard.KEY_DELETE, () -> {
            if (guiPathing != null) guiPathing.deleteButtonPressed();
        });
    }

    @SubscribeEvent
    public void postReplayOpen(ReplayOpenEvent.Post event) {
        ReplayFile replayFile = event.getReplayHandler().getReplayFile();
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
            throw new ReportedException(CrashReport.makeCrashReport(e, "Reading timeline"));
        }

        guiPathing = new GuiPathing(core, this, event.getReplayHandler());

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

    @SubscribeEvent
    public void preReplayClose(ReplayCloseEvent.Pre event) {
        saveService.shutdown();
        try {
            saveService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveService = null;
    }

    @SubscribeEvent
    public void onReplayClose(ReplayCloseEvent.Post event) {
        currentTimeline = null;
        guiPathing = null;
        selectedPath = null;
    }

    @SubscribeEvent
    public void onSettingsChanged(SettingsChangedEvent event) {
        if (event.getKey() == Setting.DEFAULT_INTERPOLATION) {
            if (currentTimeline != null && guiPathing != null) {
                updateDefaultInterpolatorType();
            }
        }
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
            CrashReport report = CrashReport.makeCrashReport(t, "Cloning timeline");
            throw new ReportedException(report);
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
