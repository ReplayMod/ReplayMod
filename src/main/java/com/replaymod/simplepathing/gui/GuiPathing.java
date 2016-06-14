package com.replaymod.simplepathing.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.change.*;
import com.replaymod.pathing.gui.GuiKeyframeRepository;
import com.replaymod.pathing.interpolation.AbstractInterpolator;
import com.replaymod.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.pathing.interpolation.LinearInterpolator;
import com.replaymod.pathing.path.Keyframe;
import com.replaymod.pathing.path.Path;
import com.replaymod.pathing.path.PathSegment;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.player.RealtimeTimelinePlayer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.gui.GuiRenderSettings;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiTimelineTime;
import de.johni0702.minecraft.gui.element.advanced.IGuiTimeline;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;
import org.lwjgl.util.WritablePoint;

import java.io.IOException;


/**
 * Gui plug-in to the GuiReplayOverlay for simple pathing.
 */
public class GuiPathing {
    public static final int TIME_PATH = 0;
    public static final int POSITION_PATH = 1;

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
    }.onClick(new IGuiTimeline.OnClick() {
        @Override
        public void run(int time) {
            timeline.setCursorPosition(time);
            // TODO: Keyframe selection
            mod.setSelectedPositionKeyframe(null);
            mod.setSelectedTimeKeyframe(null);
        }
    }).setSize(Integer.MAX_VALUE, 20).setLength(30 * 60 * 1000).setMarkers();

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
                    player.start(timeline);
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
    }

    private void preparePathsForPlayback() {
        Timeline timeline = mod.getCurrentTimeline();
        Path timePath = timeline.getPaths().get(TIME_PATH);
        Path positionPath = timeline.getPaths().get(POSITION_PATH);

        // TODO Change interpolator via Change class
        AbstractInterpolator interpolator = new LinearInterpolator();
        interpolator.registerProperty(TimestampProperty.PROPERTY);
        for (PathSegment segment : timePath.getSegments()) {
            segment.setInterpolator(interpolator);
        }
        interpolator = new CubicSplineInterpolator();
        interpolator.registerProperty(CameraProperties.POSITION);
        interpolator.registerProperty(CameraProperties.ROTATION);
        for (PathSegment segment : positionPath.getSegments()) {
            segment.setInterpolator(interpolator);
        }
        timePath.updateAll();
        positionPath.updateAll();
    }

    public void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    /**
     * Called when either one of the property buttons is pressed.
     * @param isTime {@code true} for the time property button, {@code false} for the place property button
     */
    private void updateKeyframe(final boolean isTime) {
        int time = timeline.getCursorPosition();
        Timeline timeline = mod.getCurrentTimeline();
        Path path = timeline.getPaths().get(isTime ? TIME_PATH : POSITION_PATH);

        Keyframe keyframe = path.getKeyframe(time);
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
            }
            UpdateKeyframeProperties updateChange = builder.done();
            updateChange.apply(timeline);
            timeline.pushChange(CombinedChange.createFromApplied(change, updateChange));
        } else {
            timeline.pushChange(change);
        }

        if (isTime) {
            mod.setSelectedTimeKeyframe(keyframe);
        } else {
            mod.setSelectedPositionKeyframe(keyframe);
        }
    }

    public ReplayModSimplePathing getMod() {
        return mod;
    }
}
