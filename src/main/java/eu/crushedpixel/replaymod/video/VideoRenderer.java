package eu.crushedpixel.replaymod.video;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiVideoRenderer;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.GenericLinearInterpolation;
import eu.crushedpixel.replaymod.interpolation.GenericSplineInterpolation;
import eu.crushedpixel.replaymod.interpolation.Interpolation;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.renderer.ChunkLoadingRenderGlobal;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplaySender;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.ReplayTimer;
import eu.crushedpixel.replaymod.video.capturer.RenderInfo;
import eu.crushedpixel.replaymod.video.frame.RGBFrame;
import eu.crushedpixel.replaymod.video.rendering.Pipeline;
import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Timer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;
import java.util.concurrent.FutureTask;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplaySender replaySender;
    private final RenderOptions options;

    private int fps;
    private boolean mouseWasGrabbed;

    private ChunkLoadingRenderGlobal chunkLoadingRenderGlobal;
    private Interpolation<AdvancedPosition> movement;
    private Interpolation<TimestampValue> time;

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;

    private final Pipeline renderingPipeline;

    public VideoRenderer(RenderOptions options) throws IOException {
        this.gui = new GuiVideoRenderer(this);
        this.replaySender = ReplayMod.replaySender;
        this.options = options;
        this.renderingPipeline = Pipelines.newPipeline(options.getMode(), this, new VideoWriter(options) {
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
     * @throws IOException {@link Minecraft#runTick()} can apparently throw these, don't question it!
     */
    public boolean renderVideo() throws IOException {
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
        TimestampValue timestampValue = new TimestampValue();
        time.applyPoint(0, timestampValue);

        int videoStart = (int) timestampValue.value;

        if (videoStart > 1000) {
            int replayTime = videoStart - 1000;
            timer.elapsedPartialTicks = timer.renderPartialTicks = 0;
            timer.timerSpeed = 1;
            while (replayTime < videoStart) {
                timer.elapsedTicks = 1;
                replayTime += 50;
                replaySender.sendPacketsTill(replayTime);
                tick();
            }
        }

        mc.renderGlobal.renderEntitiesStartupCounter = 0;

        renderingPipeline.run();

        finish();
        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        if (!options.isHighPerformance() || framesDone % fps == 0) {
            drawGui();
        }

        updateTime(mc.timer, framesDone);

        int elapsedTicks = mc.timer.elapsedTicks;
        while (elapsedTicks-- > 0) {
            tick();
        }

        updateCam();
        framesDone++;
        return mc.timer.renderPartialTicks;
    }

    @Override
    public RenderOptions getRenderOptions() {
        return options;
    }

    private void setup() {
        if (Mouse.isGrabbed()) {
            mouseWasGrabbed = true;
        }
        Mouse.setGrabbed(false);
        ReplayTimer.get(mc).passive = true;
        mc.timer.timerSpeed = 1;

        fps = options.getFps();
        if (options.isLinearMovement()) {
            movement = new GenericLinearInterpolation<AdvancedPosition>();
        } else {
            movement = new GenericSplineInterpolation<AdvancedPosition>();
        }
        time = new GenericLinearInterpolation<TimestampValue>();

        int duration = 0;
        int posKeyframes = 0;

        for(Keyframe<AdvancedPosition> keyframe : ReplayHandler.getPositionKeyframes()) {
            if (keyframe.getRealTimestamp() > duration) {
                duration = keyframe.getRealTimestamp();
            }

            movement.addPoint(keyframe.getValue());
            posKeyframes++;
        }

        for(Keyframe<TimestampValue> keyframe : ReplayHandler.getTimeKeyframes()) {
            if (keyframe.getRealTimestamp() > duration) {
                duration = keyframe.getRealTimestamp();
            }

            int timestamp = (int)keyframe.getValue().value;
            time.addPoint(new TimestampValue(timestamp));
        }

        totalFrames = duration*fps/1000;

        if (posKeyframes >= 2) {
            movement.prepare();
        } else {
            movement = null; // No movement occurring
        }
        time.prepare();


        ScaledResolution scaled = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        gui.setWorldAndResolution(mc, scaled.getScaledWidth(), scaled.getScaledHeight());

        if (options.isWaitForChunks()) {
            chunkLoadingRenderGlobal = new ChunkLoadingRenderGlobal(mc.renderGlobal);
        }
    }

    private void finish() {
        if (mouseWasGrabbed) {
            mc.mouseHelper.grabMouseCursor();
        }
        ReplayTimer.get(mc).passive = false;
        mc.displayGuiScreen(null);
        if (chunkLoadingRenderGlobal != null) {
            chunkLoadingRenderGlobal.uninstall();
        }

        ReplayMod.soundHandler.playRenderSuccessSound();

        mc.displayGuiScreen(null);
    }

    private void updateCam() {
        KeyframeList<AdvancedPosition> positionKeyframes = ReplayHandler.getPositionKeyframes();

        if (ReplayHandler.getCameraEntity() == null) {
            if (mc.theWorld == null) {
                return; // World hasn't been sent yet
            }
            ReplayHandler.setCameraEntity(new CameraEntity(mc.theWorld));
        }
        int videoTime = framesDone * 1000 / fps;
        int posCount = ReplayHandler.getPositionKeyframes().size();

        AdvancedPosition pos = new AdvancedPosition();
        Keyframe<AdvancedPosition> lastPos = positionKeyframes.getPreviousKeyframe(videoTime, true);
        Keyframe<AdvancedPosition> nextPos = null;
        if (movement == null || lastPos == null) {
            // Stay at one position, no movement
            Keyframe<AdvancedPosition> keyframe = positionKeyframes.getNextKeyframe(-1, true);
            assert keyframe != null;
            pos = keyframe.getValue();
        } else {
            // Position interpolation
            nextPos = positionKeyframes.getNextKeyframe(videoTime, true);

            int lastPosStamp = lastPos.getRealTimestamp();
            int nextPosStamp = (nextPos == null ? lastPos : nextPos).getRealTimestamp();

            int diffLength = nextPosStamp - lastPosStamp;
            float diffPct = (float) (videoTime - lastPosStamp) / diffLength;
            if(Float.isInfinite(diffPct) || Float.isNaN(diffPct)) diffPct = 0;

            float totalPct = (positionKeyframes.indexOf(lastPos) + diffPct) / (posCount-1);
            movement.applyPoint(Math.max(0, Math.min(1, totalPct)), pos);
        }

        boolean spectating = false;

        //if it's between two spectator keyframes sharing the same entity, spectate this entity
        if(lastPos != null && nextPos != null) {
            if(lastPos.getValue().getSpectatedEntityID() != null && nextPos.getValue().getSpectatedEntityID() != null) {
                if(lastPos.getValue().getSpectatedEntityID().equals(nextPos.getValue().getSpectatedEntityID())) {
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
                ReplayHandler.setCameraTilt((float)pos.getRoll());
                ReplayHandler.getCameraEntity().movePath(pos);
                mc.entityRenderer.fovModifierHand = mc.entityRenderer.fovModifierHandPrev = 1;
            }
        } else {
            ReplayHandler.spectateEntity(mc.theWorld.getEntityByID(lastPos.getValue().getSpectatedEntityID()));
        }

    }

    private void updateTime(Timer timer, int framesDone) {
        KeyframeList<TimestampValue> timeKeyframes = ReplayHandler.getTimeKeyframes();

        int videoTime = framesDone * 1000 / fps;
        int timeCount = timeKeyframes.size();

        // WARNING: The rest of this method contains some magic for which Marius is responsible
        // Time interpolation
        Keyframe<TimestampValue> lastTime = timeKeyframes.getPreviousKeyframe(videoTime, false);
        Keyframe<TimestampValue> nextTime = timeKeyframes.getNextKeyframe(videoTime, false);

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
                    curSpeed = ((double)(((int)nextTime.getValue().value-(int)lastTime.getValue().value)))/((double)((nextTimeStamp-lastTimeStamp)));
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

        float timePos = (timeKeyframes.indexOf(lastTime) + currentTimeStepPerc) / (timeCount-1f);

        TimestampValue timestampValue = new TimestampValue();
        time.applyPoint(Math.max(0, Math.min(1, timePos)), timestampValue);
        Integer replayTime = (int) timestampValue.value;
        replaySender.sendPacketsTill(replayTime);

        if (curSpeed >= 0) {
            replaySender.setReplaySpeed(curSpeed);
        }

        // Update Timer
        EnchantmentTimer.increaseRecordingTime(1000 / fps);

        timer.elapsedPartialTicks += timer.timerSpeed * 20 / fps;
        timer.elapsedTicks = (int) timer.elapsedPartialTicks;
        timer.elapsedPartialTicks -= timer.elapsedTicks;
        timer.renderPartialTicks = timer.elapsedPartialTicks;
    }

    private void tick() {
        synchronized (mc.scheduledTasks) {
            while (!mc.scheduledTasks.isEmpty()) {
                ((FutureTask) mc.scheduledTasks.poll()).run();
            }
        }

        mc.currentScreen = gui;
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
        return new Dimension(options.getWidth(), options.getHeight());
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
        this.cancelled = true;
        renderingPipeline.cancel();
    }
}
