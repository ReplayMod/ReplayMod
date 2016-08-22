package com.replaymod.simplepathing.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.gui.GuiKeyframeRepository;
import com.replaymod.pathing.player.RealtimeTimelinePlayer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.gui.GuiRenderSettings;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replaystudio.pathing.change.*;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.element.advanced.GuiTimelineTime;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Gui plug-in to the GuiReplayOverlay for simple pathing.
 */
public class GuiPathing {
    public static final int TIME_PATH = 0;
    public static final int POSITION_PATH = 1;

    private static final Logger logger = LogManager.getLogger();

    public final GuiTexturedButton playPauseButton = new GuiTexturedButton().setSize(20, 20)
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE);

    public final GuiTexturedButton renderButton = new GuiTexturedButton().onClick(new Runnable() {
        @Override
        public void run() {
            preparePathsForPlayback();
            new GuiRenderSettings(replayHandler, mod.getCurrentTimeline()).display();
        }
    }).setSize(20, 20).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 0);

    public final GuiTexturedButton positionKeyframeButton = new GuiTexturedButton().setSize(20, 20)
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE);

    public final GuiTexturedButton timeKeyframeButton = new GuiTexturedButton().setSize(20, 20)
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE);

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
    }).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 20);

    public final GuiTexturedButton zoomOutButton = new GuiTexturedButton().setSize(9, 9).onClick(new Runnable() {
        @Override
        public void run() {
            zoomTimeline(3d / 2d);
        }
    }).setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setTexturePosH(40, 30);

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

    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;
    private final RealtimeTimelinePlayer player;

    private EntityPositionTracker entityTracker;
    private Consumer<Double> entityTrackerLoadingProgress;
    private SettableFuture<Void> entityTrackerFuture;

    public GuiPathing(final ReplayMod core, final ReplayModSimplePathing mod, final ReplayHandler replayHandler) {
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
                    Timeline timeline = mod.getCurrentTimeline();
                    Path timePath = timeline.getPaths().get(TIME_PATH);

                    preparePathsForPlayback();

                    timePath.setActive(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
                    ListenableFuture<Void> future = player.start(timeline);
                    overlay.setCloseable(false);
                    overlay.setMouseVisible(true);
                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            overlay.setCloseable(true);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                            overlay.setCloseable(true);
                        }
                    });
                }
            }
        });

        positionKeyframeButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                Keyframe keyframe = mod.getSelectedPositionKeyframe();
                return keyframe != null ? 60 : 40;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                updateKeyframe(false);
            }
        });

        timeKeyframeButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                Keyframe keyframe = mod.getSelectedTimeKeyframe();
                return keyframe != null ? 100 : 80;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                updateKeyframe(true);
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

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.keyframerepository", Keyboard.KEY_X, new Runnable() {
            @Override
            public void run() {
                if (!overlay.isVisible()) {
                    return;
                }
                try {
                    GuiKeyframeRepository gui = new GuiKeyframeRepository(
                            mod, replayHandler.getReplayFile(), mod.getCurrentTimeline());
                    Futures.addCallback(gui.getFuture(), new FutureCallback<Timeline>() {
                        @Override
                        public void onSuccess(Timeline result) {
                            if (result != null) {
                                mod.setCurrentTimeline(result);
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
        });

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
            } catch (IOException e) {
                logger.error("Loading entity tracker:", e);
                mod.getCore().runLater(() -> {
                    mod.getCore().printWarningToChat("Error loading entity tracker: %s", e.getLocalizedMessage());
                    entityTrackerFuture.setException(e);
                });
            }
            entityTracker = tracker;
            mod.getCore().runLater(() -> {
                entityTrackerFuture.set(null);
            });
        }).start();
    }

    private void preparePathsForPlayback() {
        Timeline timeline = mod.getCurrentTimeline();
        timeline.getPaths().get(TIME_PATH).updateAll();
        timeline.getPaths().get(POSITION_PATH).updateAll();
    }

    public void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    /**
     * Called when either one of the property buttons is pressed.
     * @param isTime {@code true} for the time property button, {@code false} for the place property button
     */
    private void updateKeyframe(final boolean isTime) {
        if (entityTracker == null) {
            LoadEntityTrackerPopup popup = new LoadEntityTrackerPopup(replayHandler.getOverlay());
            entityTrackerLoadingProgress = p -> popup.progressBar.setProgress(p.floatValue());
            Futures.addCallback(entityTrackerFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                    updateKeyframe(isTime);
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    popup.close();
                }
            });
            return;
        }

        int time = timeline.getCursorPosition();
        Timeline timeline = mod.getCurrentTimeline();
        Path path = timeline.getPaths().get(isTime ? TIME_PATH : POSITION_PATH);

        Keyframe keyframe = isTime ? mod.getSelectedTimeKeyframe() : mod.getSelectedPositionKeyframe();
        Change change;
        if (keyframe == null) {
            change = AddKeyframe.create(path, time);
            change.apply(timeline);
            keyframe = path.getKeyframe(time);
        } else {
            change = RemoveKeyframe.create(path, keyframe);
            change.apply(timeline);
            keyframe = null;
        }

        if (keyframe != null) {
            UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, keyframe);
            if (isTime) {
                builder.setValue(TimestampProperty.PROPERTY, replayHandler.getReplaySender().currentTimeStamp());
            } else {
                CameraEntity camera = replayHandler.getCameraEntity();
                builder.setValue(CameraProperties.POSITION, Triple.of(camera.posX, camera.posY, camera.posZ));
                builder.setValue(CameraProperties.ROTATION, Triple.of(camera.rotationYaw, camera.rotationPitch, camera.roll));
                if (!replayHandler.isCameraView()) {
                    Entity spectated = replayHandler.getOverlay().getMinecraft().getRenderViewEntity();
                    builder.setValue(SpectatorProperty.PROPERTY, spectated.getEntityId());
                }
            }
            UpdateKeyframeProperties updateChange = builder.done();
            updateChange.apply(timeline);
            change = CombinedChange.createFromApplied(change, updateChange);

            // If this new keyframe formed the first segment of the time path
            if (isTime && path.getSegments().size() == 1) {
                PathSegment segment = path.getSegments().iterator().next();
                Interpolator interpolator = new LinearInterpolator();
                interpolator.registerProperty(TimestampProperty.PROPERTY);
                SetInterpolator setInterpolator = SetInterpolator.create(segment, interpolator);
                setInterpolator.apply(timeline);
                change = CombinedChange.createFromApplied(change, setInterpolator);
            }
        }

        // Update interpolators for spectator keyframes
        // while this is overkill, it is far simpler than updating differently for every possible case
        if (!isTime) {
            Change interpolators = updateInterpolators();
            interpolators.apply(timeline);
            change = CombinedChange.createFromApplied(change, interpolators);
        }

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);

        if (isTime) {
            mod.setSelectedTimeKeyframe(keyframe);
        } else {
            mod.setSelectedPositionKeyframe(keyframe);
        }
    }

    public Change updateInterpolators() {
        List<Change> changes = new ArrayList<>();
        Interpolator interpolator = null;
        boolean isSpectatorInterpolator = false;
        for (PathSegment segment : mod.getCurrentTimeline().getPaths().get(POSITION_PATH).getSegments()) {
            if (segment.getStartKeyframe().getValue(SpectatorProperty.PROPERTY).isPresent()
                    && segment.getEndKeyframe().getValue(SpectatorProperty.PROPERTY).isPresent()) {
                // Spectator segment
                if (!isSpectatorInterpolator) {
                    isSpectatorInterpolator = true;
                    interpolator = new LinearInterpolator();
                    interpolator.registerProperty(SpectatorProperty.PROPERTY);
                }
                changes.add(SetInterpolator.create(segment, interpolator));
            } else {
                // Normal segment
                if (isSpectatorInterpolator || interpolator == null) {
                    isSpectatorInterpolator = false;
                    interpolator = new CubicSplineInterpolator();
                    interpolator.registerProperty(CameraProperties.POSITION);
                    interpolator.registerProperty(CameraProperties.ROTATION);
                }
                changes.add(SetInterpolator.create(segment, interpolator));
            }
        }
        return CombinedChange.create(changes.toArray(new Change[changes.size()]));
    }

    public Change updateSpectatorPositions() {
        List<Change> changes = new ArrayList<>();
        Path positionPath = mod.getCurrentTimeline().getPaths().get(POSITION_PATH);
        Path timePath = mod.getCurrentTimeline().getPaths().get(TIME_PATH);
        timePath.updateAll();
        for (Keyframe keyframe : positionPath.getKeyframes()) {
            Optional<Integer> spectator = keyframe.getValue(SpectatorProperty.PROPERTY);
            if (spectator.isPresent()) {
                Optional<Integer> time = timePath.getValue(TimestampProperty.PROPERTY, keyframe.getTime());
                if (!time.isPresent()) {
                    continue; // No time keyframes set at this video time, cannot determine replay time
                }
                Location expected = entityTracker.getEntityPositionAtTimestamp(spectator.get(), time.get());
                if (expected == null) {
                    continue; // We don't have any data on this entity for some reason
                }
                Triple<Double, Double, Double> pos = keyframe.getValue(CameraProperties.POSITION).orElse(Triple.of(0D, 0D, 0D));
                Triple<Float, Float, Float> rot = keyframe.getValue(CameraProperties.ROTATION).orElse(Triple.of(0F, 0F, 0F));
                Location actual = new Location(pos.getLeft(), pos.getMiddle(), pos.getRight(), rot.getLeft(), rot.getRight());
                if (!expected.equals(actual)) {
                    changes.add(UpdateKeyframeProperties.create(positionPath, keyframe)
                            .setValue(CameraProperties.POSITION, Triple.of(expected.getX(), expected.getY(), expected.getZ()))
                            .setValue(CameraProperties.ROTATION, Triple.of(expected.getYaw(), expected.getPitch(), 0f)).done()
                    );
                }
            }
        }
        return CombinedChange.create(changes.toArray(new Change[changes.size()]));
    }

    public Change moveKeyframe(Path path, Keyframe keyframe, long newTime) {
        Timeline timeline = mod.getCurrentTimeline();
        // Interpolator might be required later (only if path is the time path)
        Optional<Interpolator> interpolator =
                path.getSegments().stream().findFirst().map(PathSegment::getInterpolator);

        // First remove the old keyframe
        Change removeChange = RemoveKeyframe.create(path, keyframe);
        removeChange.apply(timeline);

        // and add a new one at the correct time
        Change addChange = AddKeyframe.create(path, newTime);
        addChange.apply(timeline);
        path.getKeyframe(newTime);

        // Then copy over all properties
        UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, path.getKeyframe(newTime));
        for (Property property : keyframe.getProperties()) {
            copyProperty(property, keyframe, builder);
        }
        Change propertyChange = builder.done();
        propertyChange.apply(timeline);

        // Finally set the interpolators
        Change interpolatorChange;
        if (path.getTimeline().getPaths().indexOf(path) == GuiPathing.POSITION_PATH) {
            // Position / Spectator keyframes need special handling
            interpolatorChange = updateInterpolators();
        } else {
            // Time keyframes only need updating when only one segment of them exists
            if (path.getSegments().size() == 1) {
                interpolatorChange = SetInterpolator.create(path.getSegments().iterator().next(), interpolator.get());
            } else {
                interpolatorChange = CombinedChange.create(); // Noop change
            }
        }
        interpolatorChange.apply(timeline);
        // and update spectator positions
        Change spectatorChange = updateSpectatorPositions();
        spectatorChange.apply(timeline);

        return CombinedChange.createFromApplied(removeChange, addChange, propertyChange, interpolatorChange, spectatorChange);
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void copyProperty(Property<T> property, Keyframe from, UpdateKeyframeProperties.Builder to) {
        from.getValue(property).ifPresent(value -> to.setValue(property, value));
    }

    public ReplayModSimplePathing getMod() {
        return mod;
    }

    public EntityPositionTracker getEntityTracker() {
        return entityTracker;
    }

    public void openEditKeyframePopup(Path path, Keyframe keyframe) {
        if (keyframe.getProperties().contains(SpectatorProperty.PROPERTY)) {
            new GuiEditKeyframe.Spectator(this, path, keyframe).open();
        } else if (keyframe.getProperties().contains(CameraProperties.POSITION)) {
            new GuiEditKeyframe.Position(this, path, keyframe).open();
        } else {
            new GuiEditKeyframe.Time(this, path, keyframe).open();
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
