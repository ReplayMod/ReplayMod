package eu.crushedpixel.replaymod.replay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.GenericLinearInterpolation;
import eu.crushedpixel.replaymod.interpolation.GenericSplineInterpolation;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.ReplayTimer;
import eu.crushedpixel.replaymod.utils.CameraPathValidator;
import eu.crushedpixel.replaymod.video.VideoRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.resources.I18n;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;

import java.io.IOException;

public class ReplayProcess {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static int lastRealReplayTime;
    private static long lastRealTime = 0;

    private static boolean linear = false;

    private static GenericSplineInterpolation<AdvancedPosition> motionSpline = null;
    private static GenericLinearInterpolation<AdvancedPosition> motionLinear = null;
    private static GenericLinearInterpolation<TimestampValue> timeLinear = null;

    private static double previousReplaySpeed = 0;

    private static boolean calculated = false;

    @Getter
    private static VideoRenderer videoRenderer = null;

    private static boolean isVideoRecording = false;
    private static boolean requestFinish = false;
    private static boolean firstTime = false;

    public static boolean isVideoRecording() {
        return isVideoRecording;
    }

    private static void resetProcess() {
        firstTime = true;

        motionSpline = null;
        motionLinear = null;
        timeLinear = null;
        calculated = false;
        requestFinish = false;

        lastRealTime = System.currentTimeMillis();
        lastRealReplayTime = 0;
        linear = ReplayMod.replaySettings.isLinearMovement();

        previousReplaySpeed = ReplayMod.replaySender.getReplaySpeed();

        EnchantmentTimer.resetRecordingTime();
    }

    public static void startReplayProcess(RenderOptions renderOptions) {
        mc.displayGuiScreen(null);

        ReplayHandler.selectKeyframe(null);
        resetProcess();

        isVideoRecording = renderOptions != null;

        ReplayMod.chatMessageHandler.initialize();

        try {
            CameraPathValidator.validateCameraPath(ReplayHandler.getPositionKeyframes(), ReplayHandler.getTimeKeyframes());
        } catch(CameraPathValidator.InvalidCameraPathException e) {
            e.printToChat();
            return;
        }

        ReplayHandler.setInPath(true);
        ReplayMod.replaySender.setAsyncMode(false);

        if (renderOptions == null) {
            //gets first Value and sets Replay Time to it
            Keyframe<TimestampValue> tf = ReplayHandler.getTimeKeyframes().first();
            if (tf != null) {
                int ts = (int)tf.getValue().value;
                if (ts < ReplayMod.replaySender.currentTimeStamp()) {
                    mc.displayGuiScreen(null);
                }
                ReplayMod.replaySender.sendPacketsTill(ts);
            }

            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathstarted", ChatMessageType.INFORMATION);
            mc.timer.timerSpeed = 1;
        } else {
            boolean success = false;
            try {
                isVideoRecording = true;
                videoRenderer = new VideoRenderer(renderOptions);
                success = videoRenderer.renderVideo();
            } catch (IOException e) {
                e.printStackTrace();

                GuiErrorScreen errorScreen = new GuiErrorScreen(I18n.format("replaymod.gui.rendering.error.title"),
                        I18n.format("replaymod.gui.rendering.error.message"));
                mc.displayGuiScreen(errorScreen);
            } catch (Throwable t) {
                CrashReport crashReport = CrashReport.makeCrashReport(t, "Rendering video");
                throw new ReportedException(crashReport);
            } finally {
                isVideoRecording = false;
                stopReplayProcess(success);
            }
        }
    }

    public static void stopReplayProcess(boolean finished) {
        if(!ReplayHandler.isInPath()) return;

        //if canceled, display a different chat message
        if(finished) ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathfinished", ChatMessageType.INFORMATION);
        else {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathinterrupted", ChatMessageType.INFORMATION);
        }

        ReplayHandler.setInPath(false);
        ReplayMod.replaySender.setAsyncMode(true);

        ReplayMod.replaySender.stopHurrying();

        ReplayTimer.get(mc).passive = false;
        ReplayMod.replaySender.setReplaySpeed(previousReplaySpeed);
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    //if justCheck is true, no Screenshot will be taken, it will only be checked
    //whether all chunks have been rendered. This is necessary because no Render ticks
    //are called if the Timer speed is set to 0, leading to this method never being
    //called from the RenderWorldLastEvent handlers.
    public static void tickReplay(boolean justCheck) {
        if (isVideoRecording) {
            return;
        }
        if(ReplayMod.replaySender.isHurrying()) {
            lastRealTime = System.currentTimeMillis();
            return;
        }

        if(firstTime) {
            if(RenderChunk.renderChunksUpdated != 0 || mc.currentScreen != null) {
                return;
            }

            lastRealTime = System.currentTimeMillis();
            lastRealReplayTime = 0;

            firstTime = false;
            mc.timer.renderPartialTicks = 100;
            mc.timer.elapsedPartialTicks = 100;
            mc.timer.elapsedTicks = 100;
        }

        if(justCheck) return;

        int posCount = ReplayHandler.getPositionKeyframes().size();
        int timeCount = ReplayHandler.getTimeKeyframes().size();

        if(!linear && motionSpline == null) {
            //set up spline path
            motionSpline = new GenericSplineInterpolation<AdvancedPosition>();
            for(Keyframe<AdvancedPosition> kf : ReplayHandler.getPositionKeyframes()) {
                motionSpline.addPoint(kf.getValue());
            }
        }

        if(linear && motionLinear == null) {
            //set up linear path
            motionLinear = new GenericLinearInterpolation<AdvancedPosition>();
            for(Keyframe<AdvancedPosition> kf : ReplayHandler.getPositionKeyframes()) {
                motionLinear.addPoint(kf.getValue());
            }
        }
        if(timeLinear == null) {
            timeLinear = new GenericLinearInterpolation<TimestampValue>();
            for(Keyframe<TimestampValue> kf : ReplayHandler.getTimeKeyframes()) {
                timeLinear.addPoint(kf.getValue());
            }
        }

        if(!calculated) {
            calculated = true;
            if(posCount > 1 && motionSpline != null)
                motionSpline.prepare();
        }

        long curTime = System.currentTimeMillis();
        long timeStep = curTime - lastRealTime;

        int curRealReplayTime = (int) (lastRealReplayTime + timeStep);

        Keyframe<AdvancedPosition> lastPos = ReplayHandler.getPositionKeyframes().getPreviousKeyframe(curRealReplayTime, true);
        Keyframe<AdvancedPosition> nextPos = ReplayHandler.getPositionKeyframes().getNextKeyframe(curRealReplayTime, true);

        boolean spectating = false;

        //if it's between two spectator keyframes sharing the same entity, spectate this entity
        if(lastPos != null && nextPos != null) {
            if(lastPos.getValue().getSpectatedEntityID() != null && nextPos.getValue().getSpectatedEntityID() != null) {
                if(lastPos.getValue().getSpectatedEntityID().equals(nextPos.getValue().getSpectatedEntityID())) {
                    spectating = true;
                }
            }
        }

        ReplayHandler.setRealTimelineCursor(curRealReplayTime);

        int lastPosStamp = 0;
        int nextPosStamp = 0;

        if(nextPos != null || lastPos != null) {
            if(nextPos != null) {
                nextPosStamp = nextPos.getRealTimestamp();
            } else {
                nextPosStamp = lastPos.getRealTimestamp();
            }

            if(lastPos != null) {
                lastPosStamp = lastPos.getRealTimestamp();
            } else {
                lastPosStamp = nextPos.getRealTimestamp();
            }
        }

        Keyframe<TimestampValue> lastTime = ReplayHandler.getTimeKeyframes().getPreviousKeyframe(curRealReplayTime, true);
        Keyframe<TimestampValue> nextTime = ReplayHandler.getTimeKeyframes().getNextKeyframe(curRealReplayTime, true);

        int lastTimeStamp = 0;
        int nextTimeStamp = 0;

        double curSpeed = 0;

        if(timeCount > 1 && (nextTime != null || lastTime != null)) {

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
                    curSpeed = ((double) (((int)nextTime.getValue().value - (int)lastTime.getValue().value))) / ((double) ((nextTimeStamp - lastTimeStamp)));
                }

                if(lastTimeStamp == nextTimeStamp) {
                    curSpeed = 0f;
                }
            }
        }

        int currentPosDiff = nextPosStamp - lastPosStamp;
        int currentPos = curRealReplayTime - lastPosStamp;

        float currentPosStepPerc = (float) currentPos / (float) currentPosDiff; //The percentage of the travelled path between the current positions
        if(Float.isInfinite(currentPosStepPerc)) currentPosStepPerc = 0;

        int currentTimeDiff = nextTimeStamp - lastTimeStamp;
        int currentTime = curRealReplayTime - lastTimeStamp;

        float currentTimeStepPerc = (float) currentTime / (float) currentTimeDiff; //The percentage of the travelled path between the current timestamps
        if(Float.isInfinite(currentTimeStepPerc)) currentTimeStepPerc = 0;

        float splinePos = ((float) ReplayHandler.getPositionKeyframes().indexOf(lastPos) + currentPosStepPerc) / (float) (posCount - 1);
        float timePos = ((float) ReplayHandler.getTimeKeyframes().indexOf(lastTime) + currentTimeStepPerc) / (float) (timeCount - 1);

        if(!spectating) {
            ReplayHandler.spectateCamera();
            AdvancedPosition pos = new AdvancedPosition();
            if(posCount > 1) {
                if(!linear) {
                    motionSpline.applyPoint(Math.max(0, Math.min(1, splinePos)), pos);
                } else {
                    motionLinear.applyPoint(Math.max(0, Math.min(1, splinePos)), pos);
                }
            } else {
                if(posCount == 1) {
                    Keyframe<AdvancedPosition> keyframe = ReplayHandler.getPositionKeyframes().first();
                    assert keyframe != null;
                    pos = keyframe.getValue();
                }
            }

            if(pos != null) {
                ReplayHandler.setCameraTilt((float)pos.getRoll());
                ReplayHandler.getCameraEntity().movePath(pos);
            }
        } else {
            ReplayHandler.spectateEntity(mc.theWorld.getEntityByID(lastPos.getValue().getSpectatedEntityID()));
        }

        Integer curTimestamp = null;
        if(timeLinear != null && timeCount > 1) {
            TimestampValue timestampValue = new TimestampValue();
            timeLinear.applyPoint(Math.max(0, Math.min(1, timePos)), timestampValue);
            curTimestamp = (int) timestampValue.value;
        }

        if(!isVideoRecording()) ReplayMod.replaySender.setReplaySpeed(curSpeed);

        if(curTimestamp != null)
            ReplayMod.replaySender.sendPacketsTill(curTimestamp);

        lastRealReplayTime = curRealReplayTime;
        lastRealTime = curTime;

        if(requestFinish) {
            stopReplayProcess(true);
            requestFinish = false;
        }

        if((splinePos >= 1 || posCount <= 1) && (timePos >= 1 || timeCount <= 1)) {
            requestFinish = true;
        }
    }
}
