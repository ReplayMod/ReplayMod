package com.replaymod.simplepathing.gui;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.pathing.gui.GuiKeyframeRepository;
import com.replaymod.pathing.player.RealtimeTimelinePlayer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.gui.GuiRenderSettings;
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
import cpw.mods.fml.common.Loader;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.AbstractGuiClickable;
import de.johni0702.minecraft.gui.element.AbstractGuiElement;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiHorizontalScrollbar;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTexturedButton;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.IGuiClickable;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.element.advanced.GuiTimelineTime;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import net.minecraft.crash.CrashReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.WritablePoint;

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

    public final GuiTexturedButton playPauseButton = new GuiTexturedButton() {
        @Override
        public GuiElement getTooltip(RenderInfo renderInfo) {
            GuiTooltip tooltip = (GuiTooltip) super.getTooltip(renderInfo);
            if (tooltip != null) {
                if (player.isActive()) {
                    tooltip.setI18nText("replaymod.gui.ingame.menu.pausepath");
                } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    tooltip.setI18nText("replaymod.gui.ingame.menu.playpathfromstart");
                } else {
                    tooltip.setI18nText("replaymod.gui.ingame.menu.playpath");
                }
            }
            return tooltip;
        }
    }.setSize(20, 20).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTooltip(new GuiTooltip());

    public final GuiTexturedButton renderButton = new GuiTexturedButton().onClick(new Runnable() {
        @Override
        public void run() {
            if (!preparePathsForPlayback()) return;

            // Clone the timeline passed to the settings gui as it may be stored for later rendering in a queue
            SPTimeline spTimeline = mod.getCurrentTimeline();
            Timeline timeline;
            try {
                TimelineSerialization serialization = new TimelineSerialization(spTimeline, null);
                String serialized = serialization.serialize(Collections.singletonMap("", spTimeline.getTimeline()));
                timeline = serialization.deserialize(serialized).get("");
            } catch (Throwable t) {
                error(LOGGER, replayHandler.getOverlay(), CrashReport.makeCrashReport(t, "Cloning timeline"), () -> {});
                return;
            }

            new GuiRenderSettings(replayHandler, timeline).display();
        }
    }).setSize(20, 20).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 0)
            .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.ingame.menu.renderpath"));

    public final GuiTexturedButton positionKeyframeButton = new GuiTexturedButton() {
        @Override
        public GuiElement getTooltip(RenderInfo renderInfo) {
            GuiTooltip tooltip = (GuiTooltip) super.getTooltip(renderInfo);
            if (tooltip != null) {
                if (getTextureNormal().getY() == 40) { // Add keyframe
                    if (getTextureNormal().getX() == 0) { // Position
                        tooltip.setI18nText("replaymod.gui.ingame.menu.addposkeyframe");
                    } else { // Spectator
                        tooltip.setI18nText("replaymod.gui.ingame.menu.addspeckeyframe");
                    }
                } else { // Remove keyframe
                    if (getTextureNormal().getX() == 0) { // Position
                        tooltip.setI18nText("replaymod.gui.ingame.menu.removeposkeyframe");
                    } else { // Spectator
                        tooltip.setI18nText("replaymod.gui.ingame.menu.removespeckeyframe");
                    }
                }
            }
            return tooltip;
        }
    }.setSize(20, 20).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTooltip(new GuiTooltip());

    public final GuiTexturedButton timeKeyframeButton = new GuiTexturedButton() {
        @Override
        public GuiElement getTooltip(RenderInfo renderInfo) {
            GuiTooltip tooltip = (GuiTooltip) super.getTooltip(renderInfo);
            if (tooltip != null) {
                if (getTextureNormal().getY() == 80) { // Add time keyframe
                    tooltip.setI18nText("replaymod.gui.ingame.menu.addtimekeyframe");
                } else { // Remove time keyframe
                    tooltip.setI18nText("replaymod.gui.ingame.menu.removetimekeyframe");
                }
            }
            return tooltip;
        }
    }.setSize(20, 20).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTooltip(new GuiTooltip());

    public final GuiKeyframeTimeline timeline = new GuiKeyframeTimeline(this){
        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (player.isActive()) {
                setCursorPosition((int) player.getTimePassed());
            }
            super.draw(renderer, size, renderInfo);
        }
    }.setSize(Integer.MAX_VALUE, 20).setLength(30 * 60 * 1000).setMarkers();

    public final GuiHorizontalScrollbar scrollbar = new GuiHorizontalScrollbar().setSize(Integer.MAX_VALUE, 9);
    {scrollbar.onValueChanged(new Runnable() {
        @Override
        public void run() {
            timeline.setOffset((int) (scrollbar.getPosition() * timeline.getLength()));
            timeline.setZoom(scrollbar.getZoom());
        }
    }).setZoom(0.1);}

    public final GuiTimelineTime<GuiKeyframeTimeline> timelineTime = new GuiTimelineTime<GuiKeyframeTimeline>()
            .setTimeline(timeline);

    public final GuiTexturedButton zoomInButton = new GuiTexturedButton().setSize(9, 9).onClick(new Runnable() {
        @Override
        public void run() {
            zoomTimeline(2d / 3d);
        }
    }).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 20)
            .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.ingame.menu.zoomin"));

    public final GuiTexturedButton zoomOutButton = new GuiTexturedButton().setSize(9, 9).onClick(new Runnable() {
        @Override
        public void run() {
            zoomTimeline(3d / 2d);
        }
    }).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 30)
            .setTooltip(new GuiTooltip().setI18nText("replaymod.gui.ingame.menu.zoomout"));

    public final GuiPanel zoomButtonPanel = new GuiPanel()
            .setLayout(new VerticalLayout(VerticalLayout.Alignment.CENTER).setSpacing(2))
            .addElements(null, zoomInButton, zoomOutButton);

    public final GuiPanel timelinePanel = new GuiPanel().setSize(Integer.MAX_VALUE, 40)
            .setLayout(new CustomLayout<GuiPanel>() {
                @Override
                protected void layout(GuiPanel container, int width, int height) {
                    pos(zoomButtonPanel, width - width(zoomButtonPanel), 10);
                    pos(timelineTime, 0, 2);
                    size(timelineTime, x(zoomButtonPanel), 8);
                    pos(timeline, 0, y(timelineTime) + height(timelineTime));
                    size(timeline, x(zoomButtonPanel) - 2, 20);
                    pos(scrollbar, 0, y(timeline) + height(timeline) + 1);
                    size(scrollbar, x(zoomButtonPanel) - 2, 9);
                }
            }).addElements(null, timelineTime, timeline, scrollbar, zoomButtonPanel);

    public final GuiPanel panel = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5));

    {
        panel.addElements(new HorizontalLayout.Data(0.5), playPauseButton);
        if (Loader.isModLoaded("replaymod-render")) {
            panel.addElements(new HorizontalLayout.Data(0.5), renderButton);
        }
        panel.addElements(new HorizontalLayout.Data(0.5), positionKeyframeButton, timeKeyframeButton, timelinePanel);
    }

    /**
     * IGuiClickable dummy component that is inserted at a high level.
     * During path playback, this catches all click events and forwards them to the
     * abort path playback button.
     * Dragging does not have to be intercepted as every GUI element should only
     * respond to dragging events after it has received and handled a click event.
     */
    private final IGuiClickable clickCatcher = new AbstractGuiClickable() {
        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (player.isActive()) {
                // Make sure the mouse is always visible during path playback
                // even if the game closes the overlay for some reason (e.g. world change)
                replayHandler.getOverlay().setMouseVisible(true);
            }
        }

        @Override
        protected AbstractGuiElement getThis() {
            return this;
        }

        @Override
        protected ReadableDimension calcMinSize() {
            return new Dimension(0, 0);
        }

        @Override
        public boolean mouseClick(ReadablePoint position, int button) {
            if (player.isActive()) {
                playPauseButton.mouseClick(position, button);
                return true;
            }
            return false;
        }

        @Override
        public int getLayer() {
            return player.isActive() ? 10 : 0;
        }
    };

    private final ReplayMod core;
    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;
    private final RealtimeTimelinePlayer player;

    private EntityPositionTracker entityTracker;
    private Consumer<Double> entityTrackerLoadingProgress;
    private SettableFuture<Void> entityTrackerFuture;

    public GuiPathing(final ReplayMod core, final ReplayModSimplePathing mod, final ReplayHandler replayHandler) {
        this.core = core;
        this.mod = mod;
        this.replayHandler = replayHandler;
        this.player = new RealtimeTimelinePlayer(replayHandler);
        final GuiReplayOverlay overlay = replayHandler.getOverlay();

        playPauseButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                return player.isActive() ? 20 : 0;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                if (player.isActive()) {
                    player.getFuture().cancel(false);
                } else {
                    Path timePath = mod.getCurrentTimeline().getTimePath();

                    if (!preparePathsForPlayback()) return;

                    timePath.setActive(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
                    // Start from cursor time unless the control key is pressed (then start from beginning)
                    int startTime = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)? 0 : GuiPathing.this.timeline.getCursorPosition();
                    ListenableFuture<Void> future = player.start(mod.getCurrentTimeline().getTimeline(), startTime);
                    overlay.setCloseable(false);
                    overlay.setMouseVisible(true);
                    core.printInfoToChat("replaymod.chat.pathstarted");
                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            if (future.isCancelled()) {
                                core.printInfoToChat("replaymod.chat.pathinterrupted");
                            } else {
                                core.printInfoToChat("replaymod.chat.pathfinished");
                            }
                            overlay.setCloseable(true);
                            timePath.setActive(true);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                            overlay.setCloseable(true);
                            timePath.setActive(true);
                        }
                    });
                }
            }
        });

        positionKeyframeButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                SPPath keyframePath = mod.getSelectedPath();
                long keyframeTime = mod.getSelectedTime();
                if (keyframePath != SPPath.POSITION) {
                    // No keyframe or wrong path
                    keyframeTime = timeline.getCursorPosition();
                    keyframePath = mod.getCurrentTimeline().isPositionKeyframe(keyframeTime) ? SPPath.POSITION : null;
                }
                if (keyframePath != SPPath.POSITION) {
                    return replayHandler.isCameraView() ? 0 : 40;
                } else {
                    return mod.getCurrentTimeline().isSpectatorKeyframe(keyframeTime) ? 40 : 0;
                }
            }

            @Override
            public int getY() {
                SPPath keyframePath = mod.getSelectedPath();
                if (keyframePath != SPPath.POSITION) {
                    // No keyframe selected but there might be one at exactly the position of the cursor
                    keyframePath = mod.getCurrentTimeline().isPositionKeyframe(timeline.getCursorPosition()) ? SPPath.POSITION : null;
                }
                return keyframePath == SPPath.POSITION ? 60 : 40;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                updateKeyframe(SPPath.POSITION);
            }
        });

        timeKeyframeButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                SPPath keyframePath = mod.getSelectedPath();
                if (keyframePath != SPPath.TIME) {
                    // No keyframe selected but there might be one at exactly the position of the cursor
                    keyframePath = mod.getCurrentTimeline().isTimeKeyframe(timeline.getCursorPosition()) ? SPPath.TIME : null;
                }
                return keyframePath == SPPath.TIME ? 100 : 80;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                updateKeyframe(SPPath.TIME);
            }
        });

        overlay.addElements(null, panel, clickCatcher);
        overlay.setLayout(new CustomLayout<GuiReplayOverlay>(overlay.getLayout()) {
            @Override
            protected void layout(GuiReplayOverlay container, int width, int height) {
                pos(panel, 10, y(overlay.topPanel) + height(overlay.topPanel) + 3);
                size(panel, width - 20, 40);
                size(clickCatcher, 0, 0);
            }
        });

        startLoadingEntityTracker();
    }

    public void keyframeRepoButtonPressed() {
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
        GuiYesNoPopup popup = GuiYesNoPopup.open(replayHandler.getOverlay(),
                new GuiLabel().setI18nText("replaymod.gui.clearcallback.title").setColor(Colors.BLACK)
        ).setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no");
        Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean delete) {
                if (delete) {
                    mod.clearCurrentTimeline();
                    if (entityTracker != null) {
                        mod.getCurrentTimeline().setEntityTracker(entityTracker);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void syncTimeButtonPressed() {
        // Current replay time
        int time = replayHandler.getReplaySender().currentTimeStamp();
        // Position of the cursor
        int cursor = timeline.getCursorPosition();
        // Get the last time keyframe before the cursor
        mod.getCurrentTimeline().getTimePath().getKeyframes().stream()
                .filter(it -> it.getTime() <= cursor).reduce((__, last) -> last).ifPresent(keyframe -> {
            // Cursor position at the keyframe
            int keyframeCursor = (int) keyframe.getTime();
            // Replay time at the keyframe
            // This is a keyframe from the time path, so it _should_ always have a time property
            int keyframeTime = keyframe.getValue(TimestampProperty.PROPERTY).get();
            // Replay time passed
            int timePassed = time - keyframeTime;
            // Speed (set to 1 when shift is held)
            double speed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : replayHandler.getOverlay().getSpeedSliderValue();
            // Cursor time passed
            int cursorPassed = (int) (timePassed / speed);
            // Move cursor to new position
            timeline.setCursorPosition(keyframeCursor + cursorPassed);
            // Deselect keyframe to allow the user to add a new one right away
            mod.setSelected(null, 0);
        });
    }

    public void deleteButtonPressed() {
        if (mod.getSelectedPath() != null) {
            updateKeyframe(mod.getSelectedPath());
        }
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

    private boolean preparePathsForPlayback() {
        SPTimeline timeline = mod.getCurrentTimeline();
        timeline.getTimeline().getPaths().forEach(Path::updateAll);

        // Make sure time keyframes's values are monotonically increasing
        int lastTime = 0;
        for (Keyframe keyframe : timeline.getTimePath().getKeyframes()) {
            int time = keyframe.getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            if (time < lastTime) {
                // We are going backwards in time
                GuiInfoPopup.open(replayHandler.getOverlay(),
                        "replaymod.error.negativetime1",
                        "replaymod.error.negativetime2",
                        "replaymod.error.negativetime3");
                return false;
            }
            lastTime = time;
        }

        // Make sure there are at least two position- and two time-keyframes
        if (timeline.getPositionPath().getSegments().isEmpty()
                || timeline.getTimePath().getSegments().isEmpty()) {
            GuiInfoPopup.open(replayHandler.getOverlay(), "replaymod.chat.morekeyframes");
            return false;
        }

        return true;
    }

    public void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    public boolean ensureEntityTracker(Runnable withDelayedTracker) {
        if (entityTracker == null) {
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
                    withDelayedTracker.run();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    String message = "Failed to load entity tracker, pathing will be unavailable.";
                    GuiReplayOverlay overlay = replayHandler.getOverlay();
                    Utils.error(LOGGER, overlay, CrashReport.makeCrashReport(t, message), popup::close);
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
     */
    private void updateKeyframe(SPPath path) {
        LOGGER.debug("Updating keyframe on path {}" + path);
        if (!ensureEntityTracker(() -> updateKeyframe(path))) return;

        int time = timeline.getCursorPosition();
        SPTimeline timeline = mod.getCurrentTimeline();

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
                    if (!replayHandler.isCameraView()) {
                        spectatedId = replayHandler.getOverlay().getMinecraft().renderViewEntity.getEntityId();
                    }
                    timeline.addPositionKeyframe(time, camera.posX, camera.posY, camera.posZ,
                            camera.rotationYaw, camera.rotationPitch, camera.roll, spectatedId);
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
        if (!ensureEntityTracker(() -> openEditKeyframePopup(path, time))) return;
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
