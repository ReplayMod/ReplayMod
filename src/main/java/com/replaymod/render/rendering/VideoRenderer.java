package com.replaymod.render.rendering;

import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.pathing.player.AbstractTimelinePlayer;
import com.replaymod.pathing.player.ReplayTimer;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.VideoWriter;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.events.ReplayRenderEvent;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.gui.GuiRenderingDone;
import com.replaymod.render.gui.GuiVideoRenderer;
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import com.replaymod.render.metadata.MetadataInjector;
import com.replaymod.render.utils.SoundHandler;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Timer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

//#if MC>=10904
import net.minecraft.util.SoundCategory;
//#else
//$$ import net.minecraft.client.audio.SoundCategory;
//#endif

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static com.google.common.collect.Iterables.getLast;
import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.ReplayModRender.LOGGER;
import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final VideoWriter videoWriter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    //#if MC>=10904
    private Map<SoundCategory, Float> originalSoundLevels;
    //#else
    //$$ private Map originalSoundLevels;
    //#endif

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    private ChunkLoadingRenderGlobal chunkLoadingRenderGlobal;

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    private Framebuffer guiFramebuffer;
    private int displayWidth, displayHeight;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this,
                videoWriter = new VideoWriter(this) {
            @Override
            public void consume(RGBFrame frame) {
                gui.updatePreview(frame);
                super.consume(frame);
            }
        });
    }

    /**
     * Render this video.
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     */
    public boolean renderVideo() throws Throwable {
        FML_BUS.post(new ReplayRenderEvent.Pre(this));

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        Timer timer = mc.timer;

        // Play up to one second before starting to render
        // This is necessary in order to ensure that all entities have at least two position packets
        // and their first position in the recording is correct.
        // Note that it is impossible to also get the interpolation between their latest position
        // and the one in the recording correct as there's no reliable way to tell when the server ticks
        // or when we should be done with the interpolation of the entity
        Optional<Integer> optionalVideoStartTime = timeline.getValue(TimestampProperty.PROPERTY, 0);
        if (optionalVideoStartTime.isPresent()) {
            int videoStart = optionalVideoStartTime.get();

            if (videoStart > 1000) {
                int replayTime = videoStart - 1000;
                //#if MC>=11200
                timer.renderPartialTicks = 0;
                timer.tickLength = WrappedTimer.DEFAULT_MS_PER_TICK;
                //#else
                //$$ timer.elapsedPartialTicks = timer.renderPartialTicks = 0;
                //$$ timer.timerSpeed = 1;
                //#endif
                while (replayTime < videoStart) {
                    timer.elapsedTicks = 1;
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }

        mc.renderGlobal.renderEntitiesStartupCounter = 0;

        renderingPipeline.run();

        if (settings.isInject360Metadata()) {
            if (settings.getRenderMethod() == RenderSettings.RenderMethod.ODS) {
                MetadataInjector.injectODSMetadata(settings.getOutputFile());
            } else {
                MetadataInjector.inject360Metadata(settings.getOutputFile());
            }
        }

        finish();

        FML_BUS.post(new ReplayRenderEvent.Post(this));

        if (failureCause != null) {
            throw failureCause;
        }

        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        int displayWidthBefore = mc.displayWidth;
        int displayHeightBefore = mc.displayHeight;

        mc.displayWidth = displayWidth;
        mc.displayHeight = displayHeight;

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            drawGui();
        }

        // Updating the timer will cause the timeline player to update the game state
        mc.timer.updateTimer();

        int elapsedTicks = mc.timer.elapsedTicks;
        while (elapsedTicks-- > 0) {
            tick();
        }

        // change Minecraft's display size back
        mc.displayWidth = displayWidthBefore;
        mc.displayHeight = displayHeightBefore;

        framesDone++;
        return mc.timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        if (!OpenGlHelper.isFramebufferEnabled()) {
            Display.setResizable(false);
        }
        if (mc.gameSettings.showDebugInfo) {
            debugInfoWasShown = true;
            mc.gameSettings.showDebugInfo = false;
        }
        if (Mouse.isGrabbed()) {
            mouseWasGrabbed = true;
        }
        Mouse.setGrabbed(false);

        // Mute all sounds except GUI sounds (buttons, etc.)
        Map<SoundCategory, Float> mutedSounds = new EnumMap<>(SoundCategory.class);
        for (SoundCategory category : SoundCategory.values()) {
            mutedSounds.put(category, 0f);
        }
        //#if MC>=10904
        originalSoundLevels = mc.gameSettings.soundLevels;
        mutedSounds.put(SoundCategory.MASTER, originalSoundLevels.get(SoundCategory.MASTER));
        mc.gameSettings.soundLevels = mutedSounds;
        //#else
        //$$ originalSoundLevels = mc.gameSettings.mapSoundLevels;
        //$$ mutedSounds.put(SoundCategory.MASTER, (Float) originalSoundLevels.get(SoundCategory.MASTER));
        //$$ mc.gameSettings.mapSoundLevels = mutedSounds;
        //#endif

        fps = settings.getFramesPerSecond();

        long duration = 0;
        for (Path path : timeline.getPaths()) {
            if (!path.isActive()) continue;

            // Prepare path interpolations
            path.updateAll();
            // Find end time
            Collection<Keyframe> keyframes = path.getKeyframes();
            if (keyframes.size() > 0) {
                duration = Math.max(duration, getLast(keyframes).getTime());
            }
        }

        totalFrames = (int) (duration*fps/1000);

        updateDisplaySize();

        //#if MC<=10809
        //$$ ScaledResolution scaled = newScaledResolution(mc);
        //$$ gui.toMinecraft().setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());
        //#endif

        chunkLoadingRenderGlobal = new ChunkLoadingRenderGlobal(mc.renderGlobal);

        // Set up our own framebuffer to render the GUI to
        guiFramebuffer = new Framebuffer(displayWidth, displayHeight, true);
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick(new ReplayTimer.UpdatedEvent());

        if (!OpenGlHelper.isFramebufferEnabled()) {
            Display.setResizable(true);
        }
        mc.gameSettings.showDebugInfo = debugInfoWasShown;
        if (mouseWasGrabbed) {
            mc.mouseHelper.grabMouseCursor();
        }
        //#if MC>=10904
        mc.gameSettings.soundLevels = originalSoundLevels;
        //#else
        //$$ mc.gameSettings.mapSoundLevels = originalSoundLevels;
        //#endif
        mc.displayGuiScreen(null);
        if (chunkLoadingRenderGlobal != null) {
            chunkLoadingRenderGlobal.uninstall();
        }

        new SoundHandler().playRenderSuccessSound();

        try {
            if (!hasFailed()) {
                new GuiRenderingDone(ReplayModRender.instance, videoWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (VideoWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        mc.resize(displayWidth, displayHeight);
    }

    private void tick() {
        synchronized (mc.scheduledTasks) {
            while (!mc.scheduledTasks.isEmpty()) {
                ((FutureTask) mc.scheduledTasks.poll()).run();
            }
        }

        mc.currentScreen = gui.toMinecraft();
        try {
            mc.runTick();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void drawGui() {
        do {
            // Resize the GUI framebuffer if the display size changed
            if (!settings.isHighPerformance() && displaySizeChanged()) {
                updateDisplaySize();
                guiFramebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
            }

            pushMatrix();
            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            enableTexture2D();
            guiFramebuffer.bindFramebuffer(true);

            mc.entityRenderer.setupOverlayRendering();

            ScaledResolution scaled = newScaledResolution(mc);
            gui.toMinecraft().setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());

            try {
                gui.toMinecraft().handleInput();
            } catch (IOException e) {
                // That's a strange exception from this kind of method O_o
                // It isn't actually thrown here, so we'll deal with it the easy way
                throw new RuntimeException(e);
            }

            int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
            int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;

            gui.toMinecraft().drawScreen(mouseX, mouseY, 0);

            guiFramebuffer.unbindFramebuffer();
            popMatrix();
            pushMatrix();
            guiFramebuffer.framebufferRender(displayWidth, displayHeight);
            popMatrix();

            // if not in high performance mode, update the gui size if screen size changed
            // otherwise just swap the progress gui to screen
            if (settings.isHighPerformance()) {
                Display.update();
            } else {
                mc.updateDisplay();
            }
            if (Mouse.isGrabbed()) {
                Mouse.setGrabbed(false);
            }
            if (paused) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } while (paused && !hasFailed());
    }

    private boolean displaySizeChanged() {
        return displayWidth != Display.getWidth() || displayHeight != Display.getHeight();
    }

    private void updateDisplaySize() {
        displayWidth = Display.getWidth();
        displayHeight = Display.getHeight();
    }

    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getVideoTime() { return framesDone * 1000 / fps; }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        videoWriter.abort();
        this.cancelled = true;
        renderingPipeline.cancel();
    }

    public boolean hasFailed() {
        return failureCause != null;
    }

    public synchronized void setFailure(Throwable cause) {
        if (this.failureCause != null) {
            LOGGER.error("Further failure during failed rendering: ", cause);
        } else {
            LOGGER.error("Failure during rendering: ", cause);
            this.failureCause = cause;
            cancel();
        }
    }

    private class TimelinePlayer extends AbstractTimelinePlayer {
        public TimelinePlayer(ReplayHandler replayHandler) {
            super(replayHandler);
        }

        @Override
        public long getTimePassed() {
            return getVideoTime();
        }
    }
}
