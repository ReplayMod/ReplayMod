package com.replaymod.render.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.core.versions.MCVer;
import com.replaymod.pathing.player.AbstractTimelinePlayer;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.CameraPathExporter;
import com.replaymod.render.EXRWriter;
import com.replaymod.render.PNGWriter;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.events.ReplayRenderCallback;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.gui.GuiRenderingDone;
import com.replaymod.render.gui.GuiVideoRenderer;
import com.replaymod.render.gui.progress.VirtualWindow;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.metadata.MetadataInjector;
import com.replaymod.render.mixin.WorldRendererAccessor;
import com.replaymod.render.utils.FlawlessFrames;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;

//#if MC>=11700
//$$ import net.minecraft.client.render.DiffuseLighting;
//$$ import net.minecraft.util.math.Matrix4f;
//#endif

//#if MC>=11600
import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC>=11500
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
//#endif

//#if MC>=11400
import net.minecraft.client.gui.screen.Screen;
import java.util.concurrent.CompletableFuture;
//#else
//$$ import org.lwjgl.input.Mouse;
//$$ import org.lwjgl.opengl.Display;
//#endif

//#if MC>=10800
//#else
//$$ import com.replaymod.replay.gui.screen.GuiOpeningReplay;
//#endif

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.ReplayModRender.LOGGER;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private static final Identifier SOUND_RENDER_SUCCESS = new Identifier("replaymod", "render_success");
    private final MinecraftClient mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final FFmpegWriter ffmpegWriter;
    private final CameraPathExporter cameraPathExporter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    private Map<SoundCategory, Float> originalSoundLevels;

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    private ForceChunkLoadingHook forceChunkLoadingHook;
    //#if MC<10800
    //$$ private GuiOpeningReplay guiOpeningReplay;
    //#endif

    private int framesDone;
    private int totalFrames;

    private final VirtualWindow guiWindow = new VirtualWindow(mc);
    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
            BlendState.setState(new BlendState(settings.getOutputFile()));

            this.renderingPipeline = Pipelines.newBlendPipeline(this);
            this.ffmpegWriter = null;
        } else {
            FrameConsumer<BitmapFrame> frameConsumer;
            if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.EXR) {
                frameConsumer = EXRWriter.create(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
            } else if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.PNG) {
                frameConsumer = new PNGWriter(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
            } else {
                frameConsumer = new FFmpegWriter(this);
            }
            ffmpegWriter = frameConsumer instanceof FFmpegWriter ? (FFmpegWriter) frameConsumer : null;
            FrameConsumer<BitmapFrame> previewingFrameConsumer = new FrameConsumer<BitmapFrame>() {
                @Override
                public void consume(Map<Channel, BitmapFrame> channels) {
                    BitmapFrame bgra = channels.get(Channel.BRGA);
                    if (bgra != null) {
                        gui.updatePreview(bgra.getByteBuffer(), bgra.getSize());
                    }
                    frameConsumer.consume(channels);
                }

                @Override
                public void close() throws IOException {
                    frameConsumer.close();
                }
            };
            this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this, previewingFrameConsumer);
        }

        if (settings.isCameraPathExport()) {
            this.cameraPathExporter = new CameraPathExporter(settings);
        } else {
            this.cameraPathExporter = null;
        }
    }

    /**
     * Render this video.
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     */
    public boolean renderVideo() throws Throwable {
        ReplayRenderCallback.Pre.EVENT.invoker().beforeRendering(this);

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        RenderTickCounter timer = ((MinecraftAccessor) mc).getTimer();

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
                timer.tickDelta = 0;
                ((TimerAccessor) timer).setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
                //#else
                //$$ timer.elapsedPartialTicks = timer.renderPartialTicks = 0;
                //$$ timer.timerSpeed = 1;
                //#endif
                while (replayTime < videoStart) {
                    //#if MC<11600
                    //$$ timer.ticksThisFrame = 1;
                    //#endif
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }

        //#if MC<11500
        //$$ ((WorldRendererAccessor) mc.worldRenderer).setRenderEntitiesStartupCounter(0);
        //#endif

        renderingPipeline.run();

        if (((MinecraftAccessor) mc).getCrashReporter() != null) {
            throw new CrashException(((MinecraftAccessor) mc).getCrashReporter());
        }

        if (settings.isInjectSphericalMetadata()) {
            MetadataInjector.injectMetadata(settings.getRenderMethod(), settings.getOutputFile(),
                    settings.getTargetVideoWidth(), settings.getTargetVideoHeight(),
                    settings.getSphericalFovX(), settings.getSphericalFovY());
        }

        finish();

        ReplayRenderCallback.Post.EVENT.invoker().afterRendering(this);

        if (failureCause != null) {
            throw failureCause;
        }

        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        guiWindow.bind();

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            while (drawGui() && paused) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Updating the timer will cause the timeline player to update the game state
        RenderTickCounter timer = ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11600
        int elapsedTicks =
        //#endif
        timer.beginRenderTick(
                //#if MC>=11400
                MCVer.milliTime()
                //#endif
        );
        //#if MC<11600
        //$$ int elapsedTicks = timer.ticksThisFrame;
        //#endif

        executeTaskQueue();

        //#if MC<10800
        //$$ if (guiOpeningReplay != null) {
        //$$     guiOpeningReplay.handleInput();
        //$$ }
        //#endif

        while (elapsedTicks-- > 0) {
            tick();
        }

        // change Minecraft's display size back
        guiWindow.unbind();

        if (cameraPathExporter != null) {
            cameraPathExporter.recordFrame(timer.tickDelta);
        }

        framesDone++;
        return timer.tickDelta;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        // FBOs are always used in 1.14+
        //#if MC<11400
        //$$ if (!OpenGlHelper.isFramebufferEnabled()) {
        //$$     Display.setResizable(false);
        //$$ }
        //#endif
        if (mc.options.debugEnabled) {
            debugInfoWasShown = true;
            mc.options.debugEnabled = false;
        }
        //#if MC>=11400
        if (mc.mouse.isCursorLocked()) {
            mouseWasGrabbed = true;
        }
        mc.mouse.unlockCursor();
        //#else
        //$$ if (Mouse.isGrabbed()) {
        //$$     mouseWasGrabbed = true;
        //$$ }
        //$$ Mouse.setGrabbed(false);
        //#endif

        // Mute all sounds except GUI sounds (buttons, etc.)
        originalSoundLevels = new EnumMap<>(SoundCategory.class);
        for (SoundCategory category : SoundCategory.values()) {
            if (category != SoundCategory.MASTER) {
                originalSoundLevels.put(category, mc.options.getSoundVolume(category));
                mc.options.setSoundVolume(category, 0);
            }
        }

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

        if (cameraPathExporter != null) {
            cameraPathExporter.setup(totalFrames);
        }

        gui.toMinecraft().init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());

        forceChunkLoadingHook = new ForceChunkLoadingHook(mc.worldRenderer);
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick();

        guiWindow.close();

        // FBOs are always used in 1.14+
        //#if MC<11400
        //$$ if (!OpenGlHelper.isFramebufferEnabled()) {
        //$$     Display.setResizable(true);
        //$$ }
        //#endif
        mc.options.debugEnabled = debugInfoWasShown;
        if (mouseWasGrabbed) {
            //#if MC>=11400
            mc.mouse.lockCursor();
            //#else
            //$$ mc.mouseHelper.grabMouseCursor();
            //#endif
        }
        for (Map.Entry<SoundCategory, Float> entry : originalSoundLevels.entrySet()) {
            mc.options.setSoundVolume(entry.getKey(), entry.getValue());
        }
        mc.openScreen(null);
        forceChunkLoadingHook.uninstall();

        if (!hasFailed() && cameraPathExporter != null) {
            try {
                cameraPathExporter.finish();
            } catch (IOException e) {
                setFailure(e);
            }
        }

        mc.getSoundManager().play(PositionedSoundInstance.master(new SoundEvent(SOUND_RENDER_SUCCESS), 1));

        try {
            if (!hasFailed() && ffmpegWriter != null) {
                new GuiRenderingDone(ReplayModRender.instance, ffmpegWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (FFmpegWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        resizeMainWindow(mc, guiWindow.getFramebufferWidth(), guiWindow.getFramebufferHeight());
    }

    private void executeTaskQueue() {
        //#if MC>=11400
        while (true) {
            while (mc.getOverlay() != null) {
                drawGui();
                ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
            }

            CompletableFuture<Void> resourceReloadFuture = ((MinecraftAccessor) mc).getResourceReloadFuture();
            if (resourceReloadFuture != null) {
                ((MinecraftAccessor) mc).setResourceReloadFuture(null);
                mc.reloadResources().thenRun(() -> resourceReloadFuture.complete(null));
                continue;
            }
            break;
        }
        ((MCVer.MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
        //#else
        //$$ Queue<FutureTask<?>> scheduledTasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //$$ //noinspection SynchronizationOnLocalVariableOrMethodParameter
        //$$ synchronized (scheduledTasks) {
        //$$     while (!scheduledTasks.isEmpty()) {
        //$$         scheduledTasks.poll().run();
        //$$     }
        //$$ }
        //#endif

        //#if MC<10800
        //$$ if (mc.currentScreen instanceof GuiOpeningReplay) {
        //$$     guiOpeningReplay = (GuiOpeningReplay) mc.currentScreen;
        //$$ }
        //#endif

        mc.currentScreen = gui.toMinecraft();
    }

    private void tick() {
        //#if MC>=10800 && MC<11400
        //$$ try {
        //$$     mc.runTick();
        //$$ } catch (IOException e) {
        //$$     throw new RuntimeException(e);
        //$$ }
        //#else
        mc.tick();
        //#endif
    }

    public boolean drawGui() {
        Window window = mc.getWindow();
        do {
            if (GLFW.glfwWindowShouldClose(window.getHandle()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
                return false;
            }

            pushMatrix();
            GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    //#if MC>=11400
                    , false
                    //#endif
            );
            GlStateManager.enableTexture();
            guiWindow.beginWrite();

            //#if MC>=11500
            RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            //#if MC>=11700
            //$$ RenderSystem.setProjectionMatrix(Matrix4f.projectionMatrix(
            //$$         0,
            //$$         (float) (window.getFramebufferWidth() / window.getScaleFactor()),
            //$$         0,
            //$$         (float) (window.getFramebufferHeight() / window.getScaleFactor()),
            //$$         1000,
            //$$         3000
            //$$ ));
            //$$ MatrixStack matrixStack = RenderSystem.getModelViewStack();
            //$$ matrixStack.loadIdentity();
            //$$ matrixStack.translate(0, 0, -2000);
            //$$ RenderSystem.applyModelViewMatrix();
            //$$ DiffuseLighting.enableGuiDepthLighting();
            //#else
            RenderSystem.matrixMode(GL11.GL_PROJECTION);
            RenderSystem.loadIdentity();
            RenderSystem.ortho(0, window.getFramebufferWidth() / window.getScaleFactor(), window.getFramebufferHeight() / window.getScaleFactor(), 0, 1000, 3000);
            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.loadIdentity();
            RenderSystem.translatef(0, 0, -2000);
            //#endif
            //#else
            //#if MC>=11400
            //$$ window.method_4493(
                    //#if MC>=11400
                    //$$ false
                    //#endif
            //$$ );
            //#else
            //$$ mc.entityRenderer.setupOverlayRendering();
            //#endif
            //#endif

            gui.toMinecraft().init(mc, window.getScaledWidth(), window.getScaledHeight());

            // Events are polled on 1.13+ in mainWindow.update which is called later
            //#if MC<11400
            //#if MC>=10800
            //$$ try {
            //$$     gui.toMinecraft().handleInput();
            //$$ } catch (IOException e) {
            //$$     // That's a strange exception from this kind of method O_o
            //$$     // It isn't actually thrown here, so we'll deal with it the easy way
            //$$     throw new RuntimeException(e);
            //$$ }
            //#else
            //$$ gui.toMinecraft().handleInput();
            //#endif
            //#endif

            //#if MC>=11400
            int mouseX = (int) mc.mouse.getX() * window.getScaledWidth() / Math.max(window.getWidth(), 1);
            int mouseY = (int) mc.mouse.getY() * window.getScaledHeight() / Math.max(window.getHeight(), 1);

            if (mc.getOverlay() != null) {
                Screen orgScreen = mc.currentScreen;
                try {
                    mc.currentScreen = gui.toMinecraft();
                    mc.getOverlay().render(
                            //#if MC>=11600
                            new MatrixStack(),
                            //#endif
                            mouseX, mouseY, 0);
                } finally {
                    mc.currentScreen = orgScreen;
                }
            } else {
                gui.toMinecraft().tick();
                gui.toMinecraft().render(
                        //#if MC>=11600
                        new MatrixStack(),
                        //#endif
                        mouseX, mouseY, 0);
            }
            //#else
            //$$ int mouseX = Mouse.getX() * window.getScaledWidth() / mc.displayWidth;
            //$$ int mouseY = window.getScaledHeight() - Mouse.getY() * window.getScaledHeight() / mc.displayHeight - 1;
            //$$
            //$$ gui.toMinecraft().updateScreen();
            //$$ gui.toMinecraft().drawScreen(mouseX, mouseY, 0);
            //#endif

            guiWindow.endWrite();
            popMatrix();
            pushMatrix();
            guiWindow.flip();
            popMatrix();

            //#if MC>=11400
            if (mc.mouse.isCursorLocked()) {
                mc.mouse.unlockCursor();
            }
            //#else
            //$$ if (Mouse.isGrabbed()) {
            //$$     Mouse.setGrabbed(false);
            //$$ }
            //#endif

            return !hasFailed() && !cancelled;
        } while (true);
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
        if (ffmpegWriter != null) {
            ffmpegWriter.abort();
        }
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

    public static String[] checkCompat(Stream<RenderSettings> settings) {
        return settings.map(VideoRenderer::checkCompat).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static String[] checkCompat(RenderSettings settings) {
        //#if FABRIC>=1
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium") && !FlawlessFrames.hasSodium()) {
            return new String[] {
                    "Rendering is not supported with your Sodium version.",
                    "It is missing support for the FREX Flawless Frames API.",
                    "Either update to the latest version or uninstall Sodium before rendering!",
            };
        }
        //#if MC>=11700
        //$$ if (settings.getRenderMethod() == RenderSettings.RenderMethod.ODS
        //$$         && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")) {
        //$$     return new String[] {
        //$$             "ODS export requires Iris to be installed for Minecraft 1.17 and above.",
        //$$             "Note that it is nevertheless incompatible with other shaders and will simply replace them.",
        //$$             "Get it from: https://irisshaders.net/",
        //$$     };
        //$$ }
        //#endif
        //#endif
        return null;
    }
}
