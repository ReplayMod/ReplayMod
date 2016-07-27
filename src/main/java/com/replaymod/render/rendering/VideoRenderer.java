package com.replaymod.render.rendering;

import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.VideoWriter;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.gui.GuiVideoRenderer;
import com.replaymod.render.hooks.ChunkLoadingRenderGlobal;
import com.replaymod.render.hooks.RenderReplayTimer;
import com.replaymod.render.metadata.MetadataInjector;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.Timer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.FutureTask;

import static com.google.common.collect.Iterables.getLast;
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
    private Map originalSoundLevels;

    private ChunkLoadingRenderGlobal chunkLoadingRenderGlobal;

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this,
                videoWriter = new VideoWriter(settings) {
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
    public boolean renderVideo() {
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
                timer.elapsedPartialTicks = timer.renderPartialTicks = 0;
                timer.timerSpeed = 1;
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
            MetadataInjector.inject360Metadata(settings.getOutputFile());
        }

        finish();
        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            drawGui();
        }

        timeline.applyToGame(getVideoTime(), replayHandler);

        int elapsedTicks = mc.timer.elapsedTicks;
        while (elapsedTicks-- > 0) {
            tick();
        }

        framesDone++;
        return mc.timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        replayHandler.getReplaySender().setSyncModeAndWait();

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
        mc.timer = new RenderReplayTimer(mc.timer);
        mc.timer.timerSpeed = 1;

        // Mute all sounds except GUI sounds (buttons, etc.)
        Map<SoundCategory, Float> mutedSounds = new EnumMap<>(SoundCategory.class);
        for (SoundCategory category : SoundCategory.values()) {
            mutedSounds.put(category, 0f);
        }
        originalSoundLevels = mc.gameSettings.mapSoundLevels;
        mutedSounds.put(SoundCategory.MASTER, (Float) originalSoundLevels.get(SoundCategory.MASTER));
        mc.gameSettings.mapSoundLevels = mutedSounds;

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

        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        gui.toMinecraft().setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());

        chunkLoadingRenderGlobal = new ChunkLoadingRenderGlobal(mc.renderGlobal);
    }

    private void finish() {
        replayHandler.getReplaySender().setAsyncMode(true);

        if (!OpenGlHelper.isFramebufferEnabled()) {
            Display.setResizable(true);
        }
        mc.gameSettings.showDebugInfo = debugInfoWasShown;
        if (mouseWasGrabbed) {
            mc.mouseHelper.grabMouseCursor();
        }
        mc.timer = ((RenderReplayTimer) mc.timer).getWrapped();
        mc.gameSettings.mapSoundLevels = originalSoundLevels;
        mc.displayGuiScreen(null);
        if (chunkLoadingRenderGlobal != null) {
            chunkLoadingRenderGlobal.uninstall();
        }

        ReplayMod.soundHandler.playRenderSuccessSound();

        mc.displayGuiScreen(null);
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
            pushMatrix();
            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            enableTexture2D();
            mc.getFramebuffer().bindFramebuffer(true);
            mc.entityRenderer.setupOverlayRendering();

            try {
                gui.toMinecraft().handleInput();
            } catch (IOException e) {
                // That's a strange exception from this kind of method O_o
                // It isn't actually thrown here, so we'll deal with it the easy way
                throw new RuntimeException(e);
            }

            ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
            int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;
            gui.toMinecraft().drawScreen(mouseX, mouseY, 0);

            mc.getFramebuffer().unbindFramebuffer();
            popMatrix();
            pushMatrix();
            mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
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
        } while (paused);
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
}
