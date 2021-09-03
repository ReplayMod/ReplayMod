package com.replaymod.simplepathing.gui;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Result;
import com.replaymod.core.utils.Utils;
import com.replaymod.pathing.gui.GuiKeyframeRepository;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.crash.CrashReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import static com.replaymod.core.utils.Utils.error;
import static com.replaymod.simplepathing.ReplayModSimplePathing.LOGGER;


/**
 * Gui plug-in to the GuiReplayOverlay for simple pathing.
 */
public class GuiPathing {
    private static final Logger logger = LogManager.getLogger();

    private final ReplayMod core;
    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;
    public final GuiReplayOverlay overlay;

    public final GuiPathingKt kt;

    // Whether any error which occured during entity tracker loading has already been shown to the user
    private boolean errorShown;
    private EntityPositionTracker entityTracker;
    private Consumer<Double> entityTrackerLoadingProgress;
    private SettableFuture<Void> entityTrackerFuture;

    public GuiPathing(final ReplayMod core, final ReplayModSimplePathing mod, final ReplayHandler replayHandler) {
        this.core = core;
        this.mod = mod;
        this.replayHandler = replayHandler;
        this.overlay = replayHandler.getOverlay();
        this.kt = new GuiPathingKt(this, replayHandler);

        startLoadingEntityTracker();
    }

    public void keyframeRepoButtonPressed() {
        kt.abortPathPlayback();
        try {
            GuiKeyframeRepository gui = new GuiKeyframeRepository(
                    mod.getCurrentTimeline(), replayHandler.getReplayFile(), mod.getCurrentTimeline().getTimeline());
            Futures.addCallback(gui.getFuture(), new FutureCallback<Timeline>() {
                @Override
                public void onSuccess(Timeline result) {
                    if (result != null) {
                        mod.setCurrentTimeline(new SPTimeline(result));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                    core.printWarningToChat("Error loading timeline: " + t.getMessage());
                }
            });
            gui.display();
        } catch (IOException e) {
            e.printStackTrace();
            core.printWarningToChat("Error loading timeline: " + e.getMessage());
        }
    }

    public void clearKeyframesButtonPressed() {
        GuiYesNoPopup.open(replayHandler.getOverlay(),
                new GuiLabel().setI18nText("replaymod.gui.clearcallback.title").setColor(Colors.BLACK)
        ).setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no").onAccept(() -> {
            mod.clearCurrentTimeline();
            if (entityTracker != null) {
                mod.getCurrentTimeline().setEntityTracker(entityTracker);
            }
        });
    }

    public boolean deleteButtonPressed() {
        if (mod.getSelectedPath() != null) {
            toggleKeyframe(mod.getSelectedPath(), false);
            return true;
        }
        return false;
    }

    private void startLoadingEntityTracker() {
        Preconditions.checkState(entityTrackerFuture == null);
        // Start loading entity tracker
        entityTrackerFuture = SettableFuture.create();
        new Thread(() -> {
            EntityPositionTracker tracker = new EntityPositionTracker(replayHandler.getReplayFile());
            try {
                long start = System.currentTimeMillis();
                tracker.load(p -> {
                    if (entityTrackerLoadingProgress != null) {
                        entityTrackerLoadingProgress.accept(p);
                    }
                });
                logger.info("Loaded entity tracker in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Throwable e) {
                logger.error("Loading entity tracker:", e);
                mod.getCore().runLater(() -> {
                    mod.getCore().printWarningToChat("Error loading entity tracker: %s", e.getLocalizedMessage());
                    entityTrackerFuture.setException(e);
                });
                return;
            }
            entityTracker = tracker;
            mod.getCore().runLater(() -> {
                entityTrackerFuture.set(null);
            });
        }).start();
    }

    Result<Timeline, String[]> preparePathsForPlayback(boolean ignoreTimeKeyframes) {
        SPTimeline spTimeline = mod.getCurrentTimeline();

        String[] errors = validatePathsForPlayback(spTimeline, ignoreTimeKeyframes);
        if (errors != null) {
            return Result.err(errors);
        }

        try {
            TimelineSerialization serialization = new TimelineSerialization(spTimeline, null);
            String serialized = serialization.serialize(Collections.singletonMap("", spTimeline.getTimeline()));
            Timeline timeline = serialization.deserialize(serialized).get("");
            timeline.getPaths().forEach(Path::updateAll);
            return Result.ok(timeline);
        } catch (Throwable t) {
            error(LOGGER, replayHandler.getOverlay(), CrashReport.create(t, "Cloning timeline"), () -> {});
            return Result.err(null);
        }
    }

    private String[] validatePathsForPlayback(SPTimeline timeline, boolean ignoreTimeKeyframes) {
        timeline.getTimeline().getPaths().forEach(Path::updateAll);

        // Make sure there are at least two position keyframes
        if (timeline.getPositionPath().getSegments().isEmpty()) {
            return new String[]{ "replaymod.chat.morekeyframes" };
        }

        if (ignoreTimeKeyframes) {
            return null;
        }

        // Make sure time keyframes's values are monotonically increasing
        int lastTime = 0;
        for (Keyframe keyframe : timeline.getTimePath().getKeyframes()) {
            int time = keyframe.getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            if (time < lastTime) {
                // We are going backwards in time
                return new String[]{
                        "replaymod.error.negativetime1",
                        "replaymod.error.negativetime2",
                        "replaymod.error.negativetime3"
                };
            }
            lastTime = time;
        }

        // Make sure there are at least two time keyframes
        if (timeline.getTimePath().getSegments().isEmpty()) {
            return new String[]{ "replaymod.chat.morekeyframes" };
        }

        return null;
    }

    public boolean loadEntityTracker(Runnable thenRun) {
        if (entityTracker == null && !errorShown) {
            LOGGER.debug("Entity tracker not yet loaded, delaying...");
            LoadEntityTrackerPopup popup = new LoadEntityTrackerPopup(replayHandler.getOverlay());
            entityTrackerLoadingProgress = p -> popup.progressBar.setProgress(p.floatValue());
            Futures.addCallback(entityTrackerFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                    if (mod.getCurrentTimeline().getEntityTracker() == null) {
                        mod.getCurrentTimeline().setEntityTracker(entityTracker);
                    }
                    thenRun.run();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    if (!errorShown) {
                        String message = "Failed to load entity tracker, spectator keyframes will be broken.";
                        GuiReplayOverlay overlay = replayHandler.getOverlay();
                        Utils.error(LOGGER, overlay, CrashReport.create(t, message), () -> {
                            popup.close();
                            thenRun.run();
                        });
                        errorShown = true;
                    } else {
                        thenRun.run();
                    }
                }
            });
            return false;
        }
        if (mod.getCurrentTimeline().getEntityTracker() == null) {
            mod.getCurrentTimeline().setEntityTracker(entityTracker);
        }
        return true;
    }

    /**
     * Called when either one of the property buttons is pressed.
     * @param path {@code TIME} for the time property button, {@code POSITION} for the place property button
     * @param neverSpectator when true, will insert a position keyframe even when currently spectating an entity
     */
    public void toggleKeyframe(SPPath path, boolean neverSpectator) {
        LOGGER.debug("Updating keyframe on path {}" + path);
        if (!loadEntityTracker(() -> toggleKeyframe(path, neverSpectator))) return;

        int time = (int) kt.getTimeline().getCursor().getPositionMillis();
        SPTimeline timeline = mod.getCurrentTimeline();

        if (timeline.getPositionPath().getKeyframes().isEmpty() &&
                timeline.getTimePath().getKeyframes().isEmpty() &&
                time > 1000) {
            String text = I18n.translate("replaymod.gui.ingame.first_keyframe_not_at_start_warning");
            GuiInfoPopup.open(overlay, text.split("\\\\n"));
        }

        switch (path) {
            case TIME:
                if (mod.getSelectedPath() == path) {
                    LOGGER.debug("Selected keyframe is time keyframe -> removing keyframe");
                    timeline.removeTimeKeyframe(mod.getSelectedTime());
                    mod.setSelected(null, 0);
                } else if (timeline.isTimeKeyframe(time)) {
                    LOGGER.debug("Keyframe at cursor position is time keyframe -> removing keyframe");
                    timeline.removeTimeKeyframe(time);
                    mod.setSelected(null, 0);
                } else {
                    LOGGER.debug("No time keyframe found -> adding new keyframe");
                    timeline.addTimeKeyframe(time, replayHandler.getReplaySender().currentTimeStamp());
                    mod.setSelected(path, time);
                }
                break;
            case POSITION:
                if (mod.getSelectedPath() == path) {
                    LOGGER.debug("Selected keyframe is position keyframe -> removing keyframe");
                    timeline.removePositionKeyframe(mod.getSelectedTime());
                    mod.setSelected(null, 0);
                } else if (timeline.isPositionKeyframe(time)) {
                    LOGGER.debug("Keyframe at cursor position is position keyframe -> removing keyframe");
                    timeline.removePositionKeyframe(time);
                    mod.setSelected(null, 0);
                } else {
                    LOGGER.debug("No position keyframe found -> adding new keyframe");
                    CameraEntity camera = replayHandler.getCameraEntity();
                    int spectatedId = -1;
                    if (!replayHandler.isCameraView() && !neverSpectator) {
                        spectatedId = replayHandler.getOverlay().getMinecraft().getCameraEntity().getEntityId();
                    }
                    timeline.addPositionKeyframe(time, camera.getX(), camera.getY(), camera.getZ(),
                            camera.yaw, camera.pitch, camera.roll, spectatedId);
                    mod.setSelected(path, time);
                }
                break;
        }
    }

    public ReplayModSimplePathing getMod() {
        return mod;
    }

    public EntityPositionTracker getEntityTracker() {
        return entityTracker;
    }

    public void openEditKeyframePopup(SPPath path, long time) {
        if (!loadEntityTracker(() -> openEditKeyframePopup(path, time))) return;
        Keyframe keyframe = mod.getCurrentTimeline().getKeyframe(path, time);
        if (keyframe.getProperties().contains(SpectatorProperty.PROPERTY)) {
            new GuiEditKeyframe.Spectator(this, path, keyframe.getTime()).open();
        } else if (keyframe.getProperties().contains(CameraProperties.POSITION)) {
            new GuiEditKeyframe.Position(this, path, keyframe.getTime()).open();
        } else {
            new GuiEditKeyframe.Time(this, path, keyframe.getTime()).open();
        }
    }

    private class LoadEntityTrackerPopup extends AbstractGuiPopup<LoadEntityTrackerPopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar(popup).setSize(300, 20)
                .setI18nLabel("replaymod.gui.loadentitytracker");

        public LoadEntityTrackerPopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected LoadEntityTrackerPopup getThis() {
            return this;
        }
    }
}
