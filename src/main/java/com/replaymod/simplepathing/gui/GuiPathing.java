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
import com.replaymod.render.gui.noGuiRenderSettings;
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

//#if MC>=10800
import net.minecraftforge.fml.common.Loader;
//#else
//$$ import cpw.mods.fml.common.Loader;
//#endif

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import static com.replaymod.core.utils.Utils.error;
import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.simplepathing.ReplayModSimplePathing.LOGGER;

// RAH
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.extras.Extra;
import java.util.*;
import java.util.stream.Collectors;
import com.google.common.base.Predicate;
import com.replaymod.extras.playeroverview.PlayerOverview;
import net.minecraft.init.MobEffects;
import com.replaymod.replay.events.ReplayPlayingEvent; // RAH
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent; //RAH
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent; // RAH


/**
 * Gui plug-in to the GuiReplayOverlay for simple pathing.
 */
public class GuiPathing {
    private static final Logger logger = LogManager.getLogger();

	// RAH Added event - unfortunately sending events to a separate thread doesnt appear to work, but this seems silly
	// Either way, the event is throwing by replaySender, however it is not received here
	@SubscribeEvent
	public void postReplayPlaying(ReplayPlayingEvent.Post event) {
		LogManager.getLogger().debug("**************************** Video is playing per replaySender ");
		renderButton.onClick();
	}

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

			initKeyFrames(); // RAH - before doing render, set start/stop keyframes

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

			// RAH removed - GuiRenderSettings renderSettings = new GuiRenderSettings(replayHandler, timeline); 
			// RAH removed - renderSettings.display();

			// RAH Added - begin
			noGuiRenderSettings renderSettings = new noGuiRenderSettings(replayHandler, timeline); 
			renderSettings.doRender(renderStartTime_ms,renderEndTime_ms); // Since our rendering is not static, need render start/end relative to the whole 'file' or 'session'
			// RAH Added - end

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
	private int renderStartTime_ms = 0; // RAH
	private int renderEndTime_ms = 0; // RAH

    public GuiPathing(final ReplayMod core, final ReplayModSimplePathing mod, final ReplayHandler replayHandler) {
        this.core = core;
        this.mod = mod;
        this.replayHandler = replayHandler;
        this.player = new RealtimeTimelinePlayer(replayHandler);
        final GuiReplayOverlay overlay = replayHandler.getOverlay();

		replayHandler.setGuiPathing(this);

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


	/** RAH - all new
	* This is for automation, set keyframes (time and position) so this file can be rendered
	* It is called from renderButton.run() - at that point in the code, the framework is loaded and available to query
	*
	**/
	public void initKeyFrames() {

		// Trying to set keyframes for automation.
		// Steps:
		//        1.) Spectate the first player that comes up
		//        2.) Set currentTimeStamp to beginning of the video
		//        3.) updateKeyframe for time and Position 
		//        repeat 2 and 3 for end of file

		int startTime_ms = 0; // This is set below
		int endTime_ms = replayHandler.getReplaySender().replayLength(); 
		int spectatedId = -1; // This is set below

		// Step 1
		// - This code foolishly takes the first player
        List<EntityPlayer> players = world(replayHandler.getOverlay().getMinecraft()).getPlayers(EntityPlayer.class, new Predicate() {
            @Override
            public boolean apply(Object input) {
                return !(input instanceof CameraEntity); // Exclude the camera entity
            }
        });
		//Collections.sort(players, new PlayerComparator()); // Sort by name, spectators last
		for (final EntityPlayer p : players) {
			LOGGER.debug("Players");
			if (spectatedId < 0) 
			{
				replayHandler.spectateEntity(p);
				//if (!replayHandler.isCameraView()) {
				//	spectatedId = getRenderViewEntity(replayHandler.getOverlay().getMinecraft()).getEntityId();
				//}
				spectatedId = p.getEntityId();
				LOGGER.debug("\tSpectatedId-> " + spectatedId);
			}
		}
		
		// Step 2
		// There were problems with doJump, so we simply take the 'current' state and use this as the 'starting' point and position the end
		//timeline.setCursorPosition(startTime_ms);
		//replayHandler.doJump(startTime_ms,false);

		// Set the start of the render at the current position - we will need to reserve this time somewhere so we can use it in the filename
		replayHandler.getReplaySender().setReplaySpeed(0);
		startTime_ms = replayHandler.getReplaySender().currentTimeStamp(); 
		timeline.setCursorPosition(0);
		renderStartTime_ms = startTime_ms;

		// Step 3 - update Key frames - uses replaySender.currentTimeStamp()
		myUpdateKeyframe(SPPath.TIME,startTime_ms); // Yuk - I hate having to change the function name - myUpdateKeyframe used startTime_ms instead of replaySender.currentTimeSamp()
		updateKeyframe(SPPath.POSITION,spectatedId);

		
		// Position cursor at end of playback so we can get camera parameters there
		timeline.setCursorPosition(endTime_ms);

		// BAH's proposal is that we should be able to get rid of the doJump - it seems to work - so we don't need this jump
		//replayHandler.doJump(endTime_ms,false);
		replayHandler.getReplaySender().setReplaySpeed(1); // video was paused, we need to let it play a small amount so we can get new camera parameters to make the system happy
		// Sleep a bit so the engine and play and update variables.
		try {
				Thread.sleep(000);
			} catch (InterruptedException e) {
				logger.debug(e);
				return;
		}
		//renderEndTime_ms = replayHandler.getReplaySender().currentTimeStamp();
		renderEndTime_ms = endTime_ms - startTime_ms; // !!!!!! - I am not sure this logic is coorect
		replayHandler.getReplaySender().setReplaySpeed(0);
		myUpdateKeyframe(SPPath.TIME,renderEndTime_ms);  
		updateKeyframe(SPPath.POSITION,spectatedId); 
	}

	// RAH, brought in from another package
	private static boolean isSpectator(EntityPlayer e) {
        //#if MC>=10904
        return e.isInvisible() && e.getActivePotionEffect(MobEffects.INVISIBILITY) == null;
        //#else
        //$$ return e.isInvisible() && e.getActivePotionEffect(Potion.invisibility) == null;
        //#endif
    }
	private static final class PlayerComparator implements Comparator<EntityPlayer> {
        @Override
        public int compare(EntityPlayer o1, EntityPlayer o2) {
            if (isSpectator(o1) && !isSpectator(o2)) return 1;
            if (isSpectator(o2) && !isSpectator(o1)) return -1;
            //#if MC>=10800
            return o1.getName().compareToIgnoreCase(o2.getName());
            //#else
            //$$ return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
            //#endif
        }
    }
	//RAH end brought in

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
        if (timeline.getPositionPath().getSegments().isEmpty() || timeline.getTimePath().getSegments().isEmpty()) {
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
        if (!ensureEntityTracker(() -> updateKeyframe(path))) return;

        int time = timeline.getCursorPosition();
        SPTimeline timeline = mod.getCurrentTimeline();
		LOGGER.debug("Updating keyframe on path {}" + path + "@ " + time);

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
                        spectatedId = getRenderViewEntity(replayHandler.getOverlay().getMinecraft()).getEntityId();
                    }
                    timeline.addPositionKeyframe(time, camera.posX, camera.posY, camera.posZ,
                            camera.rotationYaw, camera.rotationPitch, camera.roll, spectatedId);
                    mod.setSelected(path, time);
                }
                break;
        }
    }

	// RAH - Adding a endTimeStamp instead of using replayHandler.getReplaySender().currentTimeStamp() -- this allows us to avoid a doJump because it is expensive
    private void myUpdateKeyframe(SPPath path, int timeStamp_ms) {
        if (!ensureEntityTracker(() -> updateKeyframe(path))) return;

        int time = timeline.getCursorPosition();
        SPTimeline timeline = mod.getCurrentTimeline();
		LOGGER.debug("Updating keyframe on path {}" + path + "@ " + time);

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
                    //timeline.addTimeKeyframe(time, replayHandler.getReplaySender().currentTimeStamp());
					timeline.addTimeKeyframe(time, timeStamp_ms);
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
                        spectatedId = getRenderViewEntity(replayHandler.getOverlay().getMinecraft()).getEntityId();
                    }
                    timeline.addPositionKeyframe(time, camera.posX, camera.posY, camera.posZ,
                            camera.rotationYaw, camera.rotationPitch, camera.roll, spectatedId);
                    mod.setSelected(path, time);
                }
                break;
        }
    }

	private void updateKeyframe(SPPath path, int spectatedId) {
        if (!ensureEntityTracker(() -> updateKeyframe(path))) return;

        int time = timeline.getCursorPosition();
        SPTimeline timeline = mod.getCurrentTimeline();
		LOGGER.debug("Updating keyframe on path {}" + path + "@ " + time);

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
                    timeline.addTimeKeyframe(time, replayHandler.getReplaySender().currentTimeStamp()	);
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
					/*
                    int spectatedId = -1;
                    if (!replayHandler.isCameraView()) {
                        spectatedId = getRenderViewEntity(replayHandler.getOverlay().getMinecraft()).getEntityId();
                    }
					*/
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
