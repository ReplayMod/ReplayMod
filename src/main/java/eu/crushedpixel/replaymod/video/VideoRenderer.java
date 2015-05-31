package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiVideoRenderer;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.interpolation.Interpolation;
import eu.crushedpixel.replaymod.interpolation.LinearPoint;
import eu.crushedpixel.replaymod.interpolation.LinearTimestamp;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
import eu.crushedpixel.replaymod.renderer.ChunkLoadingRenderGlobal;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplaySender;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.video.frame.FrameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Timer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.FutureTask;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final FrameRenderer frameRenderer;
    private final VideoWriter videoWriter;
    private final ReplaySender replaySender;
    private final RenderOptions options;

    private int fps;
    private boolean mouseWasGrabbed;

    private ChunkLoadingRenderGlobal chunkLoadingRenderGlobal;
    private Interpolation<Position> movement;
    private Interpolation<Integer> time;

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean frameRendererUpdatesGui;
    private boolean paused;
    private boolean cancelled;

    public VideoRenderer(RenderOptions options) throws IOException {
        this.frameRenderer = options.getRenderer();
        this.videoWriter = new VideoWriter(frameRenderer.getVideoWidth(), frameRenderer.getVideoHeight(),
                options.getFps(), options.getQuality(), 10);
        this.gui = new GuiVideoRenderer(this);
        this.replaySender = ReplayMod.replaySender;
        this.options = options;
    }

    /**
     * Render this video.
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     * @throws IOException {@link Minecraft#runTick()} can apparently throw these, don't question it!
     */
    public boolean renderVideo() throws IOException {
        setup();

        Timer timer = MCTimerHandler.getTimer();

        replaySender.sendPacketsTill(time.getPoint(0));
        // Pre-tick twice, once to process all the packets
        tick();
        // the second time to prevent interpolation of the player position
        tick();

        mc.renderGlobal.renderEntitiesStartupCounter = 0;

        framesDone = 0;
        while (framesDone < totalFrames && !cancelled) {
            pauseIfNeeded();

            if (Display.isActive() && Display.isCloseRequested()) {
                mc.shutdown();
                return false;
            }

            updateTime(timer, framesDone);

            int elapsedTicks = timer.elapsedTicks;
            while (elapsedTicks-- > 0) {
                tick();
            }

            frame(timer);
            framesDone++;

            if (!frameRendererUpdatesGui) {
                drawGui();
            }
            System.gc();
        }

        finish();
        return !cancelled;
    }

    private void setup() {
        if (Mouse.isGrabbed()) {
            mouseWasGrabbed = true;
        }
        Mouse.setGrabbed(false);
        MCTimerHandler.setTimerSpeed(1f);
        MCTimerHandler.setPassiveTimer();

        fps = options.getFps();
        if (options.isLinearMovement()) {
            movement = new LinearPoint();
        } else {
            movement = new SplinePoint();
        }
        time = new LinearTimestamp();

        int duration = 0;
        int posKeyframes = 0;
        for (Keyframe keyframe : ReplayHandler.getKeyframes()) {
            if (keyframe.getRealTimestamp() > duration) {
                duration = keyframe.getRealTimestamp();
            }
            if(keyframe instanceof PositionKeyframe) {
                movement.addPoint(((PositionKeyframe) keyframe).getPosition());
                posKeyframes++;
            }
            if (keyframe instanceof TimeKeyframe) {
                time.addPoint(((TimeKeyframe) keyframe).getTimestamp());
            }
        }
        totalFrames = duration*fps/1000;

        if (posKeyframes >= 2) {
            movement.prepare();
        } else {
            movement = null; // No movement occurring
        }
        time.prepare();

        frameRendererUpdatesGui = frameRenderer.setRenderPreviewCallback(new Runnable() {
            @Override
            public void run() {
                drawGui();
            }
        });
        frameRenderer.setup();

        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        gui.setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());

        if (options.isWaitForChunks()) {
            chunkLoadingRenderGlobal = new ChunkLoadingRenderGlobal(mc.renderGlobal);
        }
    }

    private void finish() {
        if (cancelled) {
            videoWriter.abortRecording();
        } else {
            videoWriter.endRecording();
        }
        try {
            videoWriter.waitForFinish();
        } catch (InterruptedException ignored) { }

        frameRenderer.tearDown();
        if (mouseWasGrabbed) {
            Mouse.setGrabbed(true);
        }
        MCTimerHandler.setActiveTimer();
        mc.displayGuiScreen(null);
        if (chunkLoadingRenderGlobal != null) {
            chunkLoadingRenderGlobal.uninstall();
        }
    }

    private void updateCam() {
        if (ReplayHandler.getCameraEntity() == null) {
            if (mc.theWorld == null) {
                return; // World hasn't been sent yet
            }
            ReplayHandler.setCameraEntity(new CameraEntity(mc.theWorld));
        }
        int videoTime = framesDone * 1000 / fps;
        int posCount = ReplayHandler.getPosKeyframeCount();

        Position pos;
        PositionKeyframe lastPos = ReplayHandler.getPreviousPositionKeyframe(videoTime);
        PositionKeyframe nextPos = null;
        if (movement == null || lastPos == null) {
            // Stay at one position, no movement
            pos = ReplayHandler.getNextPositionKeyframe(-1).getPosition();
        } else {
            // Position interpolation
            nextPos = ReplayHandler.getNextPositionKeyframe(videoTime);

            int lastPosStamp = lastPos.getRealTimestamp();
            int nextPosStamp = (nextPos == null ? lastPos : nextPos).getRealTimestamp();

            int diffLength = nextPosStamp - lastPosStamp;
            float diffPct = (float) (videoTime - lastPosStamp) / diffLength;
            if(Float.isInfinite(diffPct) || Float.isNaN(diffPct)) diffPct = 0;

            float totalPct = (ReplayHandler.getKeyframeIndex(lastPos) + diffPct) / (posCount-1);
            pos = movement.getPoint(Math.max(0, Math.min(1, totalPct)));
        }

        boolean spectating = false;

        //if it's between two spectator keyframes sharing the same entity, spectate this entity
        if(lastPos != null && nextPos != null) {
            if(lastPos.getSpectatedEntityID() != null && nextPos.getSpectatedEntityID() != null) {
                if(lastPos.getSpectatedEntityID().equals(nextPos.getSpectatedEntityID())) {
                    spectating = true;
                }
            }
        }

        if(!spectating) {
            // Make sure we're spectating the camera entity
            // We do not use .spectateCamera() as that method sets the position of the camera to the previous
            // entity, overriding our calculations
            ReplayHandler.spectateEntity(ReplayHandler.getCameraEntity());
            
            if(pos != null) {
                ReplayHandler.setCameraTilt(pos.getRoll());
                ReplayHandler.getCameraEntity().movePath(pos);
            }
        } else {
            ReplayHandler.spectateEntity(mc.theWorld.getEntityByID(lastPos.getSpectatedEntityID()));
        }

    }

    private void updateTime(Timer timer, int framesDone) {
        int videoTime = framesDone * 1000 / fps;
        int timeCount = ReplayHandler.getTimeKeyframeCount();

        // WARNING: The rest of this method contains some magic for which Marius is responsible
        // Time interpolation
        TimeKeyframe lastTime = ReplayHandler.getPreviousTimeKeyframe(videoTime);
        TimeKeyframe nextTime = ReplayHandler.getNextTimeKeyframe(videoTime);

        int lastTimeStamp = 0;
        int nextTimeStamp = 0;

        double curSpeed = 0;

        if(nextTime != null || lastTime != null) {
            if(nextTime != null && lastTime != null && nextTime.getRealTimestamp() == lastTime.getRealTimestamp()) {
                curSpeed = 0;
            } else {
                if(nextTime != null) {
                    nextTimeStamp = nextTime.getRealTimestamp();
                } else {
                    nextTimeStamp = lastTime.getRealTimestamp();
                }

                if(lastTime != null) {
                    lastTimeStamp = lastTime.getRealTimestamp();
                } else {
                    lastTimeStamp = nextTime.getRealTimestamp();
                }

                if(!(nextTime == null || lastTime == null)) {
                    if(lastTimeStamp == nextTimeStamp) curSpeed = 0f;
                    else curSpeed = ((double)((nextTime.getTimestamp()-lastTime.getTimestamp())))/((double)((nextTimeStamp-lastTimeStamp)));
                }

                if(lastTimeStamp == nextTimeStamp) {
                    curSpeed = 0f;
                }
            }
        }

        int currentTimeDiff = nextTimeStamp - lastTimeStamp;
        int currentTime = videoTime - lastTimeStamp;

        float currentTimeStepPerc = (float)currentTime/(float)currentTimeDiff; //The percentage of the travelled path between the current timestamps
        if(Float.isInfinite(currentTimeStepPerc) || Float.isNaN(currentTimeStepPerc)) currentTimeStepPerc = 0;

        float timePos = (ReplayHandler.getKeyframeIndex(lastTime) + currentTimeStepPerc) / (timeCount-1f);

        Integer replayTime = time.getPoint(Math.max(0, Math.min(1, timePos)));
        if(replayTime != null) {
            replaySender.sendPacketsTill(replayTime);
        }

        if(curSpeed > 0) {
            replaySender.setReplaySpeed(curSpeed);
        }

        // Update Timer
        EnchantmentTimer.increaseRecordingTime(1000 / fps);

        timer.elapsedPartialTicks += timer.timerSpeed * 20 / fps;
        timer.elapsedTicks = (int) timer.elapsedPartialTicks;
        timer.elapsedPartialTicks -= timer.elapsedTicks;
        timer.renderPartialTicks = timer.elapsedPartialTicks;
    }

    private void tick() throws IOException {
        synchronized (mc.scheduledTasks) {
            while (!mc.scheduledTasks.isEmpty()) {
                ((FutureTask) mc.scheduledTasks.poll()).run();
            }
        }

        mc.currentScreen = gui;
        mc.runTick();
    }

    private void frame(Timer timer) {
        updateCam();

        BufferedImage frame = null;
        while (frame == null) {
            try {
                frame = frameRenderer.captureFrame(timer);
            } catch (OutOfMemoryError e) {
                System.out.println("Caught oom error-> calling garbage collector, decreasing queue size and waiting for video writer");
                System.gc();
                videoWriter.waitTillQueueEmpty();
            }
        }
        while (!videoWriter.writeImage(frame, false)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
            drawGui();
        }
    }

    private void pauseIfNeeded() {
        while (paused && !cancelled && !Display.isCloseRequested()) {
            drawGui();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void drawGui() {
        pushMatrix();
        clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        enableTexture2D();
        mc.getFramebuffer().bindFramebuffer(true);
        mc.entityRenderer.setupOverlayRendering();

        try {
            gui.handleInput();
        } catch (IOException e) {
            // That's a strange exception from this kind of method O_o
            // It isn't actually thrown here, so we'll deal with it the easy way
            throw new RuntimeException(e);
        }

        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
        int mouseY = scaled.getScaledHeight() - Mouse.getY() * scaled.getScaledHeight() / mc.displayHeight - 1;
        gui.drawScreen(mouseX, mouseY, 0);

        mc.getFramebuffer().unbindFramebuffer();
        popMatrix();
        pushMatrix();
        mc.getFramebuffer().framebufferRender(mc.displayWidth, mc.displayHeight);
        popMatrix();

        Display.update();
        if (Mouse.isGrabbed()) {
            Mouse.setGrabbed(false);
        }
    }

    public int getFramesDone() {
        return framesDone;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public FrameRenderer getFrameRenderer() {
        return frameRenderer;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
