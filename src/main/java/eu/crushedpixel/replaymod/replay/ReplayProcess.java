package eu.crushedpixel.replaymod.replay;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler.ChatMessageType;
import eu.crushedpixel.replaymod.gui.GuiCancelRender;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.holders.TimeKeyframe;
import eu.crushedpixel.replaymod.interpolation.LinearPoint;
import eu.crushedpixel.replaymod.interpolation.LinearTimestamp;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
import eu.crushedpixel.replaymod.timer.EnchantmentTimer;
import eu.crushedpixel.replaymod.timer.MCTimerHandler;
import eu.crushedpixel.replaymod.video.ScreenCapture;
import eu.crushedpixel.replaymod.video.VideoWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;

import java.awt.image.BufferedImage;

public class ReplayProcess {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static long startRealTime;
    private static int lastRealReplayTime;
    private static long lastRealTime = 0;

    private static boolean linear = false;

    private static Position lastPosition = null;
    private static int lastTimestamp = -1;

    private static SplinePoint motionSpline = null;
    private static LinearPoint motionLinear = null;
    private static LinearTimestamp timeLinear = null;

    private static double lastSpeed = 1f;

    private static double previousReplaySpeed = 0;

    private static boolean calculated = false;

    private static boolean isVideoRecording = false;
    private static boolean blocked = false;
    private static boolean deepBlock = false;
    private static boolean requestFinish = false;
    private static float lastPartialTicks, lastRenderPartialTicks;
    private static int lastTicks;
    private static boolean resetTimer = false;
    private static boolean firstTime = false;

    public static boolean isVideoRecording() {
        return isVideoRecording;
    }

    private static void resetProcess() {
        firstTime = true;

        lastPosition = null;
        motionSpline = null;
        motionLinear = null;
        timeLinear = null;
        calculated = false;
        requestFinish = false;

        blocked = deepBlock = false;

        startRealTime = System.currentTimeMillis();
        lastRealTime = startRealTime;
        lastRealReplayTime = 0;
        lastTimestamp = -1;
        lastSpeed = 1f;
        linear = ReplayMod.replaySettings.isLinearMovement();

        previousReplaySpeed = ReplayMod.replaySender.getReplaySpeed();

        EnchantmentTimer.resetRecordingTime();
    }

    public static void startReplayProcess(boolean record) {
        ReplayHandler.selectKeyframe(null);
        resetProcess();

        isVideoRecording = record;

        ReplayMod.chatMessageHandler.initialize();

        //if not enough keyframes, abort and leave chat message
        if(ReplayHandler.getPosKeyframeCount() < 2 && ReplayHandler.getTimeKeyframeCount() < 2) {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.notenoughkeyframes", ChatMessageType.WARNING);
            return;
        }

        ReplayHandler.sortKeyframes();
        ReplayHandler.setInPath(true);
        ReplayMod.replaySender.setAsyncMode(false);

        //gets first Timestamp and sets Replay Time to it
        TimeKeyframe tf = ReplayHandler.getFirstTimeKeyframe();
        if(tf != null) {
            int ts = tf.getTimestamp();
            if(ts < ReplayMod.replaySender.currentTimeStamp()) {
                mc.displayGuiScreen(null);
            }
            ReplayMod.replaySender.sendPacketsTill(ts);
        }

        ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathstarted", ChatMessageType.INFORMATION);

        //if video is recording, the Replay Process takes control over the Minecraft Timer
        if(isVideoRecording()) {
            MCTimerHandler.setTimerSpeed(1f);
            MCTimerHandler.setPassiveTimer();
        } else {
            MCTimerHandler.setTimerSpeed(1f);
        }
    }

    public static void stopReplayProcess(boolean finished) {
        if(!ReplayHandler.isInPath()) return;

        //if canceled, display a different chat message
        if(finished) ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathfinished", ChatMessageType.INFORMATION);
        else {
            ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.pathinterrupted", ChatMessageType.INFORMATION);
            if(isVideoRecording()) {
                VideoWriter.abortRecording();
            }
        }

        ReplayHandler.setInPath(false);
        ReplayMod.replaySender.setAsyncMode(true);

        ReplayMod.replaySender.stopHurrying();

        MCTimerHandler.setActiveTimer();
        ReplayMod.replaySender.setReplaySpeed(previousReplaySpeed);
        ReplayMod.replaySender.setReplaySpeed(0);
    }

    public static void unblockAndTick(boolean justCheck) {
        if(!deepBlock) blocked = false;
        if(!blocked || !isVideoRecording())
            pathTick(isVideoRecording(), justCheck);
    }

    //if justCheck is true, no Screenshot will be taken, it will only be checked
    //whether all chunks have been rendered. This is necessary because no Render ticks
    //are called if the Timer speed is set to 0, leading to this method never being
    //called from the RenderWorldLastEvent handlers.
    private static void pathTick(boolean recording, boolean justCheck) {
        if(ReplayMod.replaySender.isHurrying()) {
            lastRealTime = System.currentTimeMillis();
            return;
        }

        if(firstTime) {
            firstTime = false;
            lastPartialTicks = 100;
            lastRenderPartialTicks = 100;
            lastTicks = 100;
            MCTimerHandler.setRenderPartialTicks(100);
            MCTimerHandler.setPartialTicks(100);
            MCTimerHandler.setTicks(100);
        }

        if(recording && ((ReplayMod.replaySettings.getWaitForChunks() && RenderChunk.renderChunksUpdated != 0) || mc.currentScreen instanceof GuiCancelRender)) {
            if(!firstTime) {
                MCTimerHandler.setTimerSpeed(0f);
                MCTimerHandler.setPartialTicks(0f);
                MCTimerHandler.setRenderPartialTicks(0f);
                MCTimerHandler.setTicks(0);
                resetTimer = true;
            }
            return;
        } else if(recording && ReplayMod.replaySettings.getWaitForChunks()) {
            MCTimerHandler.setTimerSpeed((float) lastSpeed);
            //MCTimerHandler.setRenderPartialTicks(lastRenderPartialTicks);
            if(resetTimer) {
                MCTimerHandler.setPartialTicks(lastPartialTicks);
                MCTimerHandler.setRenderPartialTicks(lastRenderPartialTicks);
                MCTimerHandler.setTicks(lastTicks);
                resetTimer = false;
            }
        }

        if(justCheck) return;

        if(recording) {
            if(blocked) return;

            deepBlock = true;
            blocked = true;
        }

        int posCount = ReplayHandler.getPosKeyframeCount();
        int timeCount = ReplayHandler.getTimeKeyframeCount();

        if(!linear && motionSpline == null) {
            //set up spline path
            motionSpline = new SplinePoint();
            for(Keyframe kf : ReplayHandler.getKeyframes()) {
                if(kf instanceof PositionKeyframe) {
                    PositionKeyframe pkf = (PositionKeyframe) kf;
                    Position pos = pkf.getPosition();
                    motionSpline.addPoint(pos);
                }
            }
        }

        if(linear && motionLinear == null) {
            //set up linear path
            motionLinear = new LinearPoint();
            for(Keyframe kf : ReplayHandler.getKeyframes()) {
                if(kf instanceof PositionKeyframe) {
                    PositionKeyframe pkf = (PositionKeyframe) kf;
                    Position pos = pkf.getPosition();
                    motionLinear.addPoint(pos);
                }
            }
        }
        if(timeLinear == null) {
            timeLinear = new LinearTimestamp();
            for(Keyframe kf : ReplayHandler.getKeyframes()) {
                if(kf instanceof TimeKeyframe) {
                    timeLinear.addPoint(((TimeKeyframe) kf).getTimestamp());
                }
            }
        }

        if(!calculated) {
            calculated = true;
            if(posCount > 1 && motionSpline != null)
                motionSpline.calcSpline();
        }

        long curTime = System.currentTimeMillis();
        long timeStep;
        if(recording) {
            timeStep = 1000 / ReplayMod.replaySettings.getVideoFramerate();
        } else {
            timeStep = curTime - lastRealTime;
        }

        int curRealReplayTime = (int) (lastRealReplayTime + timeStep);

        PositionKeyframe lastPos = ReplayHandler.getPreviousPositionKeyframe(curRealReplayTime);
        PositionKeyframe nextPos = ReplayHandler.getNextPositionKeyframe(curRealReplayTime);

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

        TimeKeyframe lastTime = ReplayHandler.getPreviousTimeKeyframe(curRealReplayTime);
        TimeKeyframe nextTime = ReplayHandler.getNextTimeKeyframe(curRealReplayTime);

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
                    if(lastTimeStamp == nextTimeStamp) curSpeed = 0f;
                    else
                        curSpeed = ((double) ((nextTime.getTimestamp() - lastTime.getTimestamp()))) / ((double) ((nextTimeStamp - lastTimeStamp)));
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

        float splinePos = ((float) ReplayHandler.getKeyframeIndex(lastPos) + currentPosStepPerc) / (float) (posCount - 1);
        float timePos = ((float) ReplayHandler.getKeyframeIndex(lastTime) + currentTimeStepPerc) / (float) (timeCount - 1);

        Position pos = null;
        if(posCount > 1) {
            if(!linear) {
                pos = motionSpline.getPoint(Math.max(0, Math.min(1, splinePos)));
            } else {
                pos = motionLinear.getPoint(Math.max(0, Math.min(1, splinePos)));
            }
        } else {
            if(posCount == 1) {
                pos = ReplayHandler.getFirstPositionKeyframe().getPosition();
            }
        }

        ReplayHandler.setCameraTilt(pos.getRoll());

        Integer curTimestamp = null;
        if(timeLinear != null && timeCount > 1) {
            curTimestamp = timeLinear.getPoint(Math.max(0, Math.min(1, timePos)));
        }

        if(pos != null) {
            ReplayHandler.getCameraEntity().movePath(pos);
        }

        if(!isVideoRecording()) ReplayMod.replaySender.setReplaySpeed(curSpeed);
        //if(curSpeed > 0)
        lastSpeed = curSpeed;

        if(recording) {
            MCTimerHandler.updateTimer((1f / ReplayMod.replaySettings.getVideoFramerate()));
            EnchantmentTimer.increaseRecordingTime((1000 / ReplayMod.replaySettings.getVideoFramerate()));
        }

        lastPartialTicks = MCTimerHandler.getPartialTicks();
        lastRenderPartialTicks = MCTimerHandler.getRenderTicks();
        lastTicks = MCTimerHandler.getTicks();

        if(curTimestamp != null)
            ReplayMod.replaySender.sendPacketsTill(curTimestamp);

        //splinePos = (index of last entry + add) / total entries

        lastRealReplayTime = curRealReplayTime;
        lastRealTime = curTime;

        if(isVideoRecording()) {
            try {
                if(!VideoWriter.isRecording() && ReplayHandler.isInPath()) {
                    VideoWriter.startRecording(mc.displayWidth, mc.displayHeight);
                } else {
                    final BufferedImage screen = ScreenCapture.captureScreen();
                    VideoWriter.writeImage(screen);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        if(requestFinish) {
            stopReplayProcess(true);
            requestFinish = false;
            if(recording) {
                VideoWriter.endRecording();
            }
        }

        if((splinePos >= 1 || posCount <= 1) && (timePos >= 1 || timeCount <= 1)) {
            requestFinish = true;
        }

        if(recording) {
            deepBlock = false;
        }
    }
}
